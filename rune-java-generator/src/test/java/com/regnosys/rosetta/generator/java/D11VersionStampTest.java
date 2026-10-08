package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.D11CorpusRegressionTest.CellSpec;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * N4 (v3.1 phase C, C0 item 4) — the harness must emulate Maven resource filtering,
 * not a per-corpus constant.
 *
 * <p><b>The defect.</b> {@code D11CorpusRegressionTest} stamped
 * {@link CellSpec#versionStamp()} onto EVERY model of a cell. Upstream's golden build
 * substituted {@code ${project.version}} through Maven resource filtering, which can
 * only rewrite a version that is actually DECLARED in the {@code .rosetta} source. A
 * model declaring no version never saw a substitution and took the generator default
 * ({@code 0.0.0}) into its output.
 *
 * <p><b>The witness.</b> Band-wide (25 cells, 3,980 model files) exactly TWO files
 * declare no version — {@code regulation-techsprint-g20-mas-rule.rosetta} and
 * {@code -type.rosetta} in drr 5.61.0 — and exactly the four goldens generated from
 * them carry {@code @version 0.0.0} while the cell's other 2,107 carry
 * {@code 0.0.0.master-SNAPSHOT}:
 * {@code MASSFATransactionReport}, {@code Trader} (POJO) and their two {@code *Meta}
 * registries (XMETA). The unconditional stamp made all four diverge on the version
 * line — the root-cause audit's N4.
 *
 * <p><b>Scope of the fix.</b> Only models declaring NO version change behaviour: the
 * 4,013 files declaring {@code "${project.version}"} and the 47 iso20022 files
 * declaring a literal are still stamped exactly as before, so no gated cell can move
 * (all five originally-gated cells declare a version in every file).
 *
 * <p>Skips when the corpus is absent (CI / fresh clone), like every corpus-backed test.
 */
class D11VersionStampTest {

    private static final Path DRR_561_ROSETTA = Path.of(
            "..", "test-corpus", "drr", "drr-5.61.0", "rosetta-source", "src", "main", "rosetta");

    private static CellSpec drr561() {
        return new CellSpec("drr", "5.61.0",
                Path.of("..", "test-corpus", "drr", "drr-5.61.0"));
    }

    /**
     * The two version-less models must survive the harness untouched, so the generator
     * default reaches the output — as the golden proves upstream's did.
     */
    @Test
    void undeclaredVersionIsNotStamped() throws Exception {
        assumeTrue(Files.isDirectory(DRR_561_ROSETTA), "drr 5.61.0 corpus cell absent");

        for (String name : new String[] {
                "regulation-techsprint-g20-mas-rule.rosetta",
                "regulation-techsprint-g20-mas-type.rosetta" }) {
            Path source = DRR_561_ROSETTA.resolve(name);
            assumeTrue(Files.isRegularFile(source), "corpus file absent: " + name);

            RModel model = AstBuilder.buildFromFile(source);
            assertTrue(model.version().isEmpty(),
                    name + " is expected to declare NO version — the N4 witness. If this "
                            + "fails the corpus changed; re-derive N4 before touching the stamp.");

            D11CorpusRegressionTest.applyVersionStamp(model, drr561());

            assertTrue(model.version().isEmpty(),
                    name + ": the harness stamped a version onto a model that declares none. "
                            + "Maven resource filtering cannot substitute an absent declaration, "
                            + "so the golden carries the generator default (@version 0.0.0) and "
                            + "the stamp manufactures a byte mismatch (root-cause audit N4).");
        }
    }

    /**
     * The chaos cell's build ran NO Maven filtering (pin-chaos-goldens.sh invokes the
     * mojo directly), so declared versions reach the goldens VERBATIM — and they are
     * split (measured at the first D11 chaos probe, 2026-09-02: 562 sources declare
     * {@code "1.0.0"}, 22 declare {@code "1.0.0-SNAPSHOT"}; the goldens carry 1,912 vs
     * 82 version annotations). A constant stamp manufactured 82 version-line
     * mismatches. The harness must leave BOTH declaration classes untouched; the
     * witness below is a {@code -SNAPSHOT}-declaring seed variant, the class the
     * constant stamp clobbered.
     */
    @Test
    void chaosDeclaredVersionsPassThroughVerbatim() throws Exception {
        // The cell's location and version from the corpus SOT through ONE helper (v3.2 seat 10,
        // D49 — the chaos-1.0.0 -> chaos-1.1.0 cut; the next cut is a TSV edit).
        Path chaosRosetta = com.regnosys.rosetta.testutil.ChaosCell.sources();
        assumeTrue(Files.isDirectory(chaosRosetta), "chaos corpus cell absent");
        CellSpec chaos = new CellSpec("chaos", com.regnosys.rosetta.testutil.ChaosCell.row().version(),
                com.regnosys.rosetta.testutil.ChaosCell.root());

        // The § 4b refusal files are dropped by the SAME shared, typed filter the D11
        // loader uses (the cq review's SF-6: the previous catch-and-continue swallowed
        // ANY build failure — a parser regression refusing a hundred chaos files would
        // have left this test green); every remaining file must BUILD.
        Path snapshotDeclaring = null;
        Path plainDeclaring = null;
        java.util.List<Path> walk;
        try (var stream = Files.walk(chaosRosetta)) {
            walk = stream.filter(f -> f.toString().endsWith(".rosetta")).sorted().toList();
        }
        for (Path p : D11CorpusRegressionTest.dropChaosExpectedRefusals(chaos, walk)) {
            RModel m = AstBuilder.buildFromFile(p);
            if (m.version().isPresent()) {
                if ("1.0.0-SNAPSHOT".equals(m.version().get()) && snapshotDeclaring == null) {
                    snapshotDeclaring = p;
                } else if ("1.0.0".equals(m.version().get()) && plainDeclaring == null) {
                    plainDeclaring = p;
                }
            }
            if (snapshotDeclaring != null && plainDeclaring != null) break;
        }
        // ASSERTED, not assumed (the split IS the pinned fact this test locks): a
        // vanished declaration class means the corpus moved — re-derive, never skip.
        assertNotNull(snapshotDeclaring,
                "no 1.0.0-SNAPSHOT-declaring chaos model found — the measured 562/22 split"
                        + " has moved; re-derive it before touching the stamp");
        assertNotNull(plainDeclaring, "no 1.0.0-declaring chaos model found — the measured"
                + " 562/22 split has moved; re-derive it before touching the stamp");

        RModel snap = AstBuilder.buildFromFile(snapshotDeclaring);
        D11CorpusRegressionTest.applyVersionStamp(snap, chaos);
        assertEquals("1.0.0-SNAPSHOT", snap.version().orElse(null),
                snapshotDeclaring.getFileName() + ": an unfiltered build's declared version"
                        + " must pass through verbatim — a rewrite manufactures the 82"
                        + " version-line mismatches the first probe found.");

        RModel plain = AstBuilder.buildFromFile(plainDeclaring);
        D11CorpusRegressionTest.applyVersionStamp(plain, chaos);
        assertEquals("1.0.0", plain.version().orElse(null),
                plainDeclaring.getFileName() + ": the majority declaration class must also"
                        + " pass through verbatim.");
    }

    /** The 82 declaring models in the same cell must still receive the cell stamp. */
    @Test
    void declaredVersionIsStamped() throws Exception {
        assumeTrue(Files.isDirectory(DRR_561_ROSETTA), "drr 5.61.0 corpus cell absent");

        Path source;
        try (var stream = Files.walk(DRR_561_ROSETTA)) {
            source = stream.filter(p -> p.toString().endsWith(".rosetta"))
                    .sorted()
                    .filter(p -> {
                        try {
                            return AstBuilder.buildFromFile(p).version().isPresent();
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .findFirst()
                    .orElse(null);
        }
        assumeTrue(source != null, "no version-declaring model found in drr 5.61.0");

        RModel model = AstBuilder.buildFromFile(source);
        D11CorpusRegressionTest.applyVersionStamp(model, drr561());

        assertEquals("0.0.0.master-SNAPSHOT", model.version().orElse(null),
                source.getFileName() + ": a declaring model must still take the cell's "
                        + "substituted stamp — that is what Maven filtering did.");
    }
}
