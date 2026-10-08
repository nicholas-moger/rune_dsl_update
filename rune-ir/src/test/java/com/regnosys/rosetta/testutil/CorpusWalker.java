package com.regnosys.rosetta.testutil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared test-scope helper for walking the parseable .rosetta corpus.
 *
 * <p>Universe: the {@code .rosetta} files of the 9.83.0-pinned CATALOGUE cells +
 * {@code rune-dsl/rune-runtime/src/main/resources/model/} (2 rune-runtime builtins
 * when the upstream rune-dsl checkout is present), as returned by
 * {@link #allRosettaFiles()} once the working tree has been refreshed via the
 * project's corpus-refresh workflow.
 *
 * <p><b>These counts are NOT constants.</b> From PR #85 (2026-05-28) to
 * 2026-08-14 the corpus was frozen at 5 cells, and figures like "589" were quoted
 * as if fixed. The 2026-08-14 band expansion adds every remaining rune-dsl-9.83.0
 * release, and the corpus will keep changing — so quote counts with the corpus
 * state they describe. At the 9-cell state: 1,041 catalogue sources + 2 builtins
 * mirrored under {@code test-corpus/} = 1,043.
 *
 * <p><b>Two exclusions apply</b>, and both were once a source of miscounting:
 * <ul>
 *   <li>transitive-DEPENDENCY cells (see {@link #TRANSITIVE_DEP_CELL_DIRS}, e.g.
 *       {@code rune-fpml-1.5.3} = +42 files) are present on disk but are another
 *       cell's dependency closure, not catalogue inputs (PR #185);</li>
 *   <li>build-output COPIES under {@code target/} (see {@link #isBuildOutputPath},
 *       e.g. ISO-20022's 47 duplicated sources). The pre-expansion "589" figure
 *       silently INCLUDED these, which is why it exceeded the CATALOGUE's own 540
 *       by exactly 47 + 2.</li>
 * </ul>
 * Both are pinned for drift by the frozen-baseline manifest, not by the
 * catalogue census.
 *
 * <p>Both paths are <b>cloned/refreshed on demand</b> — {@code test-corpus/}
 * subdirectories and {@code rune-dsl/} are gitignored. In a fresh clone of
 * this repo, {@code corpusExists()} will return {@code false} and the tests
 * that depend on this helper will short-circuit (see {@link #corpusExists()}).
 *
 * <p>Lives in {@code com.regnosys.rosetta.testutil} (NOT under
 * {@code com.regnosys.rosetta.ast}) so it does not pollute the
 * {@code SchemaLockGenerator} AST-package scan.
 *
 * <p>Path resolution: {@link #CORPUS_DIR}, {@link #BUILTINS_DIR}, and
 * {@link #repoRoot()} are derived from this class's classfile location
 * (under {@code target/test-classes/}) — independent of JVM working
 * directory. Correct under Surefire {@code forkCount=0} (in-process
 * tests) and when {@code mvn -f rune-ir/pom.xml test} is invoked
 * from the repo root, where {@code Path.of("../...")} would otherwise
 * resolve outside the repo.
 *
 * <p>This is the rune-ir module's copy of the parser-side helper of the
 * same name (byte-identical semantics; only the module-root assertion and
 * this doc differ) — rune-parser publishes no test-jar, so the IR corpus
 * tests carry their own walker. Used by:
 * <ul>
 *   <li>{@code com.regnosys.rosetta.ir.contract.RRootElementToIRKindMappingTest}
 *       — IRKind mapping completeness
 *   <li>the corpus-walking adapter/census suites under
 *       {@code com.regnosys.rosetta.ir.**}
 * </ul>
 */
public final class CorpusWalker {

    private static final Path TEST_CLASSES_ROOT = resolveTestClassesRoot();
    /** {@code .../rune-ir/} */
    private static final Path MODULE_ROOT = assertModuleRoot(
            TEST_CLASSES_ROOT.getParent().getParent());
    /** {@code .../} (the repo root, parent of {@link #MODULE_ROOT}). */
    private static final Path REPO_ROOT = MODULE_ROOT.getParent();

    /**
     * Defensive sanity-check on the {@code testClasses → module} positional
     * walk. If a future Surefire / Maven layout change moves
     * {@code target/test-classes/} elsewhere, this assertion fails loud at
     * class-init rather than silently resolving paths from a wrong root.
     */
    private static Path assertModuleRoot(Path candidate) {
        String name = candidate.getFileName() == null ? "" : candidate.getFileName().toString();
        if (!"rune-ir".equals(name)) {
            throw new IllegalStateException(
                    "Module root expected to be 'rune-ir' but resolved to: " + candidate +
                            " — Surefire/Maven layout assumption violated; review " +
                            "CorpusWalker.resolveTestClassesRoot()");
        }
        return candidate;
    }

    public static final Path CORPUS_DIR = REPO_ROOT.resolve("test-corpus");
    public static final Path BUILTINS_DIR = REPO_ROOT
            .resolve("rune-dsl/rune-runtime/src/main/resources/model");

    /**
     * Transitive-DEPENDENCY cell directories under {@link #CORPUS_DIR} that are NOT
     * CATALOGUE cells: loaded only as another cell's dependency closure for type
     * RESOLUTION, never byte-compared as a cell of their own. PR #185: cdm-6.20.6 pins
     * rune-fpml 1.5.3 (distinct from the rune-fpml-2.0.0 CATALOGUE cell), so
     * {@code rune-fpml-1.5.3} is loaded by D11 as cdm6's transitive dependency but is NOT
     * one of the 5 D11 {@code ALL_CELLS} / structural-baseline CATALOGUE cells. Excluded
     * from the catalogue census ({@link #allRosettaFiles()}, {@code MatrixCellsProvider}
     * discovery, the structural baselines, the RRootElement audit) so those stay
     * catalogue-scoped at 5 cells — but STILL pinned by the frozen-baseline manifest
     * ({@code Corpus983BaselineManifestTest}) for drift protection (the manifest locks
     * bytes; the census characterises the tested catalogue — different purposes).
     *
     * <p>Note: {@code AstCorpusRegressionTest} runs its OWN {@code Files.walk(CORPUS_DIR)}
     * parse/AST-well-formedness net (a floor-count regression check, not a catalogue
     * census), so it DELIBERATELY still lexes+parses the transitive-dep sources — desirable
     * parse coverage. Only the catalogue-CENSUS surfaces listed above apply this exclusion.
     */
    // MUST equal the active resolution-only rows of test-corpus/corpus-cells.tsv;
    // CorpusCatalogueTest#transitiveDepDirsMatchSot fails if they drift apart.
    public static final java.util.Set<String> TRANSITIVE_DEP_CELL_DIRS =
            java.util.Set.of("rune-fpml-1.5.3",     // pinned by cdm 6.20.2-6.20.6
                             "cdm-5.36.0",          // pinned by drr 5.61.0
                             "iso20022-1.18.0",     // pinned by drr 5.61.0
                             "iso20022-1.37.0");    // pinned by drr 6.35.0-6.38.0

    /**
     * True if {@code p} lies under a {@link #TRANSITIVE_DEP_CELL_DIRS} cell directory.
     * Expects a {@code .rosetta} FILE path (matches {@code /<dir>/} as an interior
     * segment); it is not intended for a bare directory path.
     */
    public static boolean isTransitiveDepPath(Path p) {
        String s = p.toString().replace('\\', '/');
        for (String dir : TRANSITIVE_DEP_CELL_DIRS) {
            if (s.contains("/" + dir + "/")) return true;
        }
        return false;
    }

    private CorpusWalker() {
        // utility class — no instances
    }

    /**
     * Module root ({@code rune-ir/}), resolved once per JVM from this
     * class's classfile location ({@code target/test-classes/}).
     * Independent of JVM working directory; safe to use from any test under
     * Surefire {@code forkCount=0/≥1} or under
     * {@code mvn -f rune-ir/pom.xml test} from the repo root.
     */
    public static Path moduleRoot() {
        return MODULE_ROOT;
    }

    /**
     * Repo root (parent of {@link #moduleRoot()}), resolved once per JVM.
     *
     * <p>Cached so per-file callers across the corpus don't re-parse the
     * ProtectionDomain URI on every call.
     */
    public static Path repoRoot() {
        return REPO_ROOT;
    }

    private static Path resolveTestClassesRoot() {
        try {
            return Paths.get(
                    CorpusWalker.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI());
        } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException("failed to resolve test-classes root", e);
        }
    }

    /**
     * True if the CATALOGUE corpus directory exists with at least one CATALOGUE-cell
     * .rosetta file. PR #185 (Copilot R1): excludes {@link #TRANSITIVE_DEP_CELL_DIRS}
     * sources so this gate stays consistent with {@link #allRosettaFiles()} (which also
     * excludes them) — a hypothetical checkout containing ONLY transitive-dep cells (e.g.
     * just {@code rune-fpml-1.5.3}) must report the catalogue ABSENT so the audits that
     * gate on {@code corpusExists()} then iterate {@code allRosettaFiles()} short-circuit
     * cleanly instead of running on an empty set.
     */
    public static boolean corpusExists() {
        if (!Files.exists(CORPUS_DIR)) {
            return false;
        }
        try (var walk = Files.walk(CORPUS_DIR, 10)) {
            return walk.anyMatch(p -> p.toString().endsWith(".rosetta")
                    && !isTransitiveDepPath(p) && !isBuildOutputPath(p));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * True if {@code p} lies under a build-output directory ({@code target/}) — i.e. a
     * COPY of a source file produced by the cell's own Maven build, not a corpus input.
     *
     * <p>ISO-20022's build copies all 47 of its {@code .rosetta} sources into
     * {@code rosetta-source/target/classes/iso20022/rosetta}. Counting those copies
     * double-counts every ISO element, which is exactly the defect that made the
     * structural-baseline dumper emit 94 files / 4,816 elements where its consumer
     * ({@code StructuralComparisonTest}, which has always pruned {@code target/})
     * expected 47 / 2,408. It stayed latent from PR #85 until the 2026-08-14 band
     * expansion only because the corpus was frozen and nothing re-ran the dumper.
     *
     * <p>NOTE this is about {@code .rosetta} SOURCE copies only. ISO's generated Java
     * goldens legitimately live under {@code target/classes/generated/java} (D25, see
     * {@code D11CorpusRegressionTest#resolveGoldensDir}) and are manifest-tracked there,
     * so the directory itself must stay on disk — it cannot simply be deleted.
     */
    public static boolean isBuildOutputPath(Path p) {
        String s = p.toString().replace('\\', '/');
        return s.contains("/target/");
    }

    /**
     * Returns all .rosetta files in {@link #CORPUS_DIR} + {@link #BUILTINS_DIR}
     * (if present), sorted by path, excluding transitive-dependency cells and
     * build-output copies.
     *
     * <p>Returns 1,043 paths at the 2026-08-14 9-cell state (1,041 CATALOGUE-cell
     * sources + the 2 builtins mirrored under {@code test-corpus/}). Before the band
     * expansion this returned 591, of which 47 were ISO-20022 {@code target/} COPIES
     * counted twice — see {@link #isBuildOutputPath}. Treat this figure as a
     * per-corpus-state fact, not a constant: the corpus is no longer static.
     *
     * <p>If {@link #CORPUS_DIR} is absent (cloned-on-demand corpus not yet
     * refreshed), this throws {@link java.nio.file.NoSuchFileException}.
     * Callers should gate with {@link #corpusExists()}.
     */
    public static List<Path> allRosettaFiles() throws IOException {
        List<Path> all = new ArrayList<>();
        try (var walk = Files.walk(CORPUS_DIR)) {
            walk.filter(p -> p.toString().endsWith(".rosetta"))
                .filter(p -> !isTransitiveDepPath(p))   // PR #185: skip transitive-dep cells (rune-fpml-1.5.3)
                .filter(p -> !isBuildOutputPath(p))     // 2026-08-14: skip target/ source COPIES
                .forEach(all::add);
        }
        if (Files.exists(BUILTINS_DIR)) {
            try (var walk = Files.walk(BUILTINS_DIR)) {
                walk.filter(p -> p.toString().endsWith(".rosetta")).forEach(all::add);
            }
        }
        all.sort(Path::compareTo);
        return all;
    }

    /**
     * Returns a display name suitable for JUnit dynamic-test labels.
     * Relativises against {@link #CORPUS_DIR} or {@link #BUILTINS_DIR};
     * falls back to filename for any other path.
     */
    public static String displayName(Path file) {
        if (file.startsWith(CORPUS_DIR)) {
            return CORPUS_DIR.relativize(file).toString();
        }
        if (file.startsWith(BUILTINS_DIR)) {
            return BUILTINS_DIR.relativize(file).toString();
        }
        return file.getFileName().toString();
    }
}
