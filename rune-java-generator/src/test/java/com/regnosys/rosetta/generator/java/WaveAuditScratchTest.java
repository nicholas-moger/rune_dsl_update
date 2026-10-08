package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.D11CorpusRegressionTest.CellSpec;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Wave Step-0 AUDIT (scratch diagnostic — never trust dormant code, the #403 law):
 * runs the current wave's DORMANT family generators over the cdm-6.20.6 +
 * iso20022-1.38.0 cells AS THEY ARE and byte-diffs every output against the frozen
 * goldens. Prints the drift-class catalogue; makes no assertion beyond corpus
 * presence (diagnostic only — the D11 kind extension is the gate that will enforce
 * byte-equality). Re-pointed at each wave's families per the burn-down design doc
 * §2 Step 0 (wave A audited OnlyExists/Cardinality/package-info at PR #405; wave B
 * audited TypeFormat validators + XMeta registries at PR #407; wave C audited the
 * NEW deep-path util generator at PR #408; NOW pointed at wave D — datarule, the
 * LAST family. No dormant generator existed: the audit measures the NEW
 * family-owned DataRuleGenerator against the goldens, and its
 * goldens=/emitted=/missing=/noGolden= sets ARE the datarule-count
 * reconciliation — open question #4 of the #403 analysis — run through the
 * parser).
 *
 * <p>The two audit cells are deliberate: cdm6 = the largest/most-shape-diverse cell;
 * iso20022 = the {@code ${project.version}} literal + the target/classes golden tree
 * (the iso-tree grep law) + the {@code Error} java.lang-collision witness.
 *
 * <p>OPT-IN ONLY (Copilot #405 R1): the default suite skips this class — it duplicates
 * populations {@code D11CorpusRegressionTest} now gates strictly, and its output is
 * diagnostic, not an assertion. Run it manually when auditing a NEW wave's dormant
 * generators (the burn-down design doc's Step 0):
 * (Renamed from {@code WaveAAuditScratchTest} at PR #407 Copilot R1 — the class is
 * wave-neutral and re-points per wave.)
 * {@code mvn -f rune-java-generator/pom.xml test -Dtest=WaveAuditScratchTest -Dwave.audit=true}
 */
@EnabledIfSystemProperty(named = "wave.audit", matches = "true",
        disabledReason = "Step-0 wave-audit diagnostic — opt-in via -Dwave.audit=true; the D11 gate covers these populations by default")
class WaveAuditScratchTest {

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);
    private static final int SAMPLE_LIMIT = 10;

    @Test
    void auditWaveFamilies() throws IOException {
        var d11 = new D11CorpusRegressionTest();
        // Wave D widened the audit to ALL FIVE cells (the drr/fpml null-expression
        // gen-error triage + the cdm5 tail — the per-path GENERROR print is the
        // diagnostic the D11 gate's aggregated assert doesn't give).
        List<CellSpec> auditCells = D11CorpusRegressionTest.activeCells().toList();
        Assumptions.assumeTrue(auditCells.stream().allMatch(D11CorpusRegressionTest::cellGoldensExist),
                "Audit cells absent — corpus not present");

        for (CellSpec cell : auditCells) {
            var corpus = d11.loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell),
                    D11CorpusRegressionTest.readDoNotPrune(cell));

            // Family: data rules (validation/datarule/<Type><Cond>.java — the tiling:
            // 3,853 goldens corpus-wide with an immediate datarule/ parent; per-cell
            // 448/555/2,140/246/464). The goldens-vs-emitted set diff IS the
            // datarule-count reconciliation (#403 open question #4).
            Map<String, String> dataRuleOut = new LinkedHashMap<>();
            List<GenerationException> dataRuleErrs = new ArrayList<>();
            var dataRuleGen = new DataRuleGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
            for (RModel model : corpus.workspace().files()) {
                if (gm.shouldGenerate(model)) {
                    dataRuleErrs.addAll(dataRuleGen.generateClasses(model, gm.version(model), dataRuleOut));
                }
            }
            audit(cell, "DATARULE", dataRuleOut, dataRuleErrs, p ->
                    "datarule".equals(parentName(p)));
            // Optional generated-output dump for drift triage:
            // -Dwave.audit.dump=<abs dir> writes every emitted file (single-use dirs).
            String dumpDir = System.getProperty("wave.audit.dump");
            if (dumpDir != null && !dumpDir.isBlank()) {
                Path cellDump = Path.of(dumpDir, cell.corpus() + "-" + cell.version());
                for (var e : dataRuleOut.entrySet()) {
                    Path target = cellDump.resolve(e.getKey());
                    Files.createDirectories(target.getParent());
                    Files.writeString(target, e.getValue());
                }
                System.out.println("=== DUMPED " + dataRuleOut.size() + " files to " + cellDump + " ===");
            }
        }
    }

    private static String parentName(Path p) {
        Path parent = p.getParent();
        return (parent != null && parent.getFileName() != null) ? parent.getFileName().toString() : "";
    }

    /** Byte-diff output vs the goldens matching the family predicate; print the drift catalogue. */
    private void audit(CellSpec cell, String family, Map<String, String> output,
                       List<GenerationException> genErrors, Predicate<Path> goldenFamilyFilter)
            throws IOException {
        Path goldensDir = D11CorpusRegressionTest.resolveGoldensDir(cell);
        Map<String, Path> familyGoldens = new LinkedHashMap<>();
        try (var stream = Files.walk(goldensDir)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> p.toString().endsWith(".java"))
                  .filter(goldenFamilyFilter)
                  .sorted()
                  .forEach(p -> familyGoldens.put(
                          goldensDir.relativize(p).toString().replace('\\', '/'), p));
        }

        List<String> identical = new ArrayList<>();
        List<String[]> mismatches = new ArrayList<>(); // {path, firstDiffLine, goldenLine, genLine}
        List<String> missing = new ArrayList<>();
        List<String> noGolden = new ArrayList<>();

        for (var e : familyGoldens.entrySet()) {
            String genContent = output.get(e.getKey());
            if (genContent == null) {
                missing.add(e.getKey());
                continue;
            }
            String golden = normalize(Files.readString(e.getValue()));
            String generated = normalize(genContent);
            if (golden.equals(generated)) {
                identical.add(e.getKey());
            } else {
                String[] gl = golden.split("\n", -1);
                String[] ge = generated.split("\n", -1);
                int line = firstDiffLine(gl, ge);
                mismatches.add(new String[]{e.getKey(), String.valueOf(line),
                        line - 1 < gl.length ? gl[line - 1] : "<EOF>",
                        line - 1 < ge.length ? ge[line - 1] : "<EOF>"});
            }
        }
        for (String key : output.keySet()) {
            if (!familyGoldens.containsKey(key)) {
                noGolden.add(key);
            }
        }

        System.out.println("=== AUDIT " + cell + " " + family
                + ": goldens=" + familyGoldens.size()
                + " emitted=" + output.size()
                + " identical=" + identical.size()
                + " mismatches=" + mismatches.size()
                + " missing=" + missing.size()
                + " noGolden=" + noGolden.size()
                + " genErrors=" + genErrors.size() + " ===");
        mismatches.stream().limit(SAMPLE_LIMIT).forEach(m -> {
            System.out.println("  MISMATCH " + m[0] + " @line " + m[1]);
            System.out.println("    GOLDEN: " + visible(m[2]));
            System.out.println("    GEN   : " + visible(m[3]));
        });
        missing.stream().limit(SAMPLE_LIMIT).forEach(p -> System.out.println("  MISSING " + p));
        noGolden.stream().limit(SAMPLE_LIMIT).forEach(p -> System.out.println("  NOGOLDEN " + p));
        genErrors.stream().limit(SAMPLE_LIMIT).forEach(e ->
                System.out.println("  GENERROR " + e.getTargetPath() + " : " + e.getMessage()));
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static int firstDiffLine(String[] gl, String[] ge) {
        int max = Math.max(gl.length, ge.length);
        for (int i = 0; i < max; i++) {
            String g = i < gl.length ? gl[i] : "<EOF>";
            String e = i < ge.length ? ge[i] : "<EOF>";
            if (!g.equals(e)) return i + 1;
        }
        return 0;
    }

    /** Make tabs/trailing spaces visible in the printed diff lines. */
    private static String visible(String s) {
        return s.replace("\t", "\\t") + "|";
    }
}
