package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Leg-S swap-recon dump driver (PR #439, the drop-in parity program):
 * materialises the fork's FULL generated tree for one D11 cell to a directory
 * ({@code -Dswap.out=...}), reusing {@link D11CorpusRegressionTest}'s own cell
 * loader and the exact generator wiring of its 11 kind passes. The output keys
 * are golden-relative paths, so the dump directory mirrors the cell's
 * {@code rosetta-source/src/generated/java} layout 1:1 — ready to swap into a
 * real corpus project's build in place of the released plugin's output (the
 * community-build layer of the Four-Layer gate; evidence:
 * the development audit "2026-07-19-leg-s-swap-recon-ledger").
 *
 * <p><b>Deliberately named {@code *Probe}, not {@code *Test}:</b> surefire's
 * default includes never discover it, so it adds ZERO entries to the default
 * suite runs (the gensuite shape carries). It compiles on every test-compile
 * (cannot rot silently) and runs only when invoked explicitly:
 *
 * <pre>
 * mvn -f rune-java-generator/pom.xml test -Dtest=SwapTreeDumpProbe
 *   -Dswap.out=C:/work/swap-recon/fork-generated
 *   -Dd11.corpus=cdm -Dd11.version=6.20.6
 *   -Dmaven.build.cache.enabled=false
 * </pre>
 *
 * <p>The cell is selected via {@code -Dswap.cell} (default {@code cdm/6.20.6});
 * {@code -Dd11.corpus}/{@code -Dd11.version} additionally scope the
 * {@code activeCells()} stream exactly as the D11 gate does. Without
 * {@code -Dswap.out} the test skips via a JUnit assumption, so a stray explicit
 * invocation never fails. A SET but invalid {@code swap.out} — a relative path,
 * or a non-empty directory — fails loudly instead (see the guard in the method).
 */
class SwapTreeDumpProbe {

    @Test
    void dumpCellTree() throws IOException {
        String outProp = System.getProperty("swap.out", "").trim();
        Assumptions.assumeTrue(!outProp.isEmpty(), "swap.out not set — probe skipped");
        Path outDir = Path.of(outProp);
        // A set-but-invalid swap.out is a user error, not a skip condition: fail loud.
        // Absolute only (a relative path would resolve against the surefire working dir
        // and write into an unexpected location); empty-or-absent only (stale files from
        // a prior dump into a shrinking population would make later swap runs ambiguous).
        if (!outDir.isAbsolute()) {
            throw new AssertionError("[SWAP-DUMP] swap.out must be an absolute path: " + outProp);
        }
        if (Files.isDirectory(outDir)) {
            try (var entries = Files.list(outDir)) {
                if (entries.findFirst().isPresent()) {
                    throw new AssertionError("[SWAP-DUMP] swap.out must be an empty (or not yet "
                            + "existing) directory — stale files would make later swap runs "
                            + "ambiguous: " + outDir);
                }
            }
        } else if (Files.exists(outDir)) {
            throw new AssertionError("[SWAP-DUMP] swap.out exists and is not a directory: " + outDir);
        }

        String cellName = System.getProperty("swap.cell", "cdm/6.20.6");
        D11CorpusRegressionTest.CellSpec cell = D11CorpusRegressionTest.activeCells()
                .filter(c -> c.toString().equals(cellName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("cell not active: " + cellName));
        Assumptions.assumeTrue(D11CorpusRegressionTest.cellGoldensExist(cell),
                "corpus absent for " + cell);

        var d11 = new D11CorpusRegressionTest();
        var corpus = d11.loadCellCorpusCached(cell);
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var emission = D11CorpusRegressionTest.emissionFilter(cell);

        Map<String, String> all = new LinkedHashMap<>();
        List<String> kindLog = new ArrayList<>();

        // ENUM — mirrors enum_comparison
        {
            var gm = new GeneratorModel(corpus.workspace(), emission);
            // The D43 IR seams (as the D11 harness): flag off ⇒ exactly the legacy calls,
            // flag on (-Pir-on) ⇒ the IR route — the probe is the A/B ring's dump vehicle.
            var gen = IRGeneration.enumGenerator(gm);
            Map<String, String> out = new LinkedHashMap<>();
            List<GenerationException> errs = new ArrayList<>();
            for (RModel model : corpus.workspace().files()) {
                if (gm.shouldGenerate(model)) {
                    errs.addAll(IRGeneration.generateClasses(gen, model, gm.version(model), out));
                }
            }
            failOnErrors("ENUM", errs);
            merge(all, out, "ENUM", kindLog);
        }

        // POJO + choice + rule + report + labelProvider — mirrors pojo_comparison
        {
            var gm = new GeneratorModel(corpus.workspace(), emission,
                    D11CorpusRegressionTest.readDoNotPrune(cell));
            var pojoGen = IRGeneration.modelObjectGenerator(gm, typeTranslator, typeUtil);
            var choiceGen = IRGeneration.choiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
            var funcGen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
            var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
            var labelProviderGen = new LabelProviderGenerator(
                    gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                    new LabelProviderGeneratorUtil());
            Map<String, String> out = new LinkedHashMap<>();
            List<GenerationException> errs = new ArrayList<>();
            for (RModel model : corpus.workspace().files()) {
                if (gm.shouldGenerate(model)) {
                    String version = gm.version(model);
                    errs.addAll(IRGeneration.generateClasses(pojoGen, model, version, out));
                    errs.addAll(IRGeneration.generateClasses(choiceGen, model, version, out));
                    errs.addAll(ruleGen.generateClasses(model, version, out));
                    errs.addAll(reportGen.generateClasses(model, version, out));
                    errs.addAll(labelProviderGen.generateClasses(model, version, out));
                }
            }
            failOnErrors("POJO", errs);
            merge(all, out, "POJO", kindLog);
        }

        // METAFIELD — mirrors metafield_comparison
        {
            var gm = new GeneratorModel(corpus.workspace(), emission);
            var gen = IRGeneration.metaFieldGenerator(gm, typeTranslator);
            Map<String, String> out = new LinkedHashMap<>();
            IRGeneration.generateMeta(gen, out);
            merge(all, out, "METAFIELD", kindLog);
        }

        // FUNCTION — mirrors function_comparison
        {
            var gm = new GeneratorModel(corpus.workspace(), emission);
            var gen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            Map<String, String> out = new LinkedHashMap<>();
            failOnErrors("FUNCTION", gen.generateWithErrors(out));
            merge(all, out, "FUNCTION", kindLog);
        }

        // ONLY_EXISTS / CARDINALITY / TYPE_FORMAT / XMETA / DEEP_PATH / DATA_RULE — per-model generators
        dumpPerModelKind(all, kindLog, corpus, cell, "ONLY_EXISTS",
                gm -> new com.regnosys.rosetta.generator.java.object.validators.OnlyExistsValidatorGenerator(
                        gm, typeTranslator, typeUtil));
        dumpPerModelKind(all, kindLog, corpus, cell, "CARDINALITY",
                gm -> new com.regnosys.rosetta.generator.java.object.validators.CardinalityValidatorGenerator(
                        gm, typeTranslator, typeUtil));
        dumpPerModelKind(all, kindLog, corpus, cell, "TYPE_FORMAT",
                gm -> new com.regnosys.rosetta.generator.java.object.validators.TypeFormatValidatorGenerator(
                        gm, typeTranslator, typeUtil));
        dumpPerModelKind(all, kindLog, corpus, cell, "XMETA",
                gm -> new com.regnosys.rosetta.generator.java.object.ModelMetaGenerator(gm, typeTranslator));
        dumpPerModelKind(all, kindLog, corpus, cell, "DEEP_PATH",
                gm -> new com.regnosys.rosetta.generator.java.object.deeppath.DeepPathUtilGenerator(
                        gm, typeTranslator, typeUtil));
        dumpPerModelKind(all, kindLog, corpus, cell, "DATA_RULE",
                gm -> new com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator(
                        gm, typeTranslator, typeUtil));

        // PACKAGE_INFO — mirrors packageinfo_comparison
        {
            var gm = new GeneratorModel(corpus.workspace(), emission);
            var gen = new com.regnosys.rosetta.generator.java.object.JavaPackageInfoGenerator(gm);
            Map<String, String> out = new LinkedHashMap<>();
            gen.generatePackageInfoClasses(out);
            merge(all, out, "PACKAGE_INFO", kindLog);
        }

        // Write the tree
        int written = 0;
        for (var e : all.entrySet()) {
            Path target = outDir.resolve(e.getKey());
            Files.createDirectories(target.getParent());
            Files.writeString(target, e.getValue());
            written++;
        }
        for (String line : kindLog) {
            System.out.println("[SWAP-DUMP] " + line);
        }
        System.out.println("[SWAP-DUMP] " + cell + " TOTAL files=" + all.size()
                + " written=" + written + " -> " + outDir);
    }

    private interface PerModelGen {
        JavaClassGenerator<?, ?> make(GeneratorModel gm);
    }

    private void dumpPerModelKind(Map<String, String> all, List<String> kindLog,
            com.regnosys.rosetta.symbols.RLinkingResult corpus,
            D11CorpusRegressionTest.CellSpec cell, String label, PerModelGen maker) {
        var gm = new GeneratorModel(corpus.workspace(), D11CorpusRegressionTest.emissionFilter(cell));
        var gen = maker.make(gm);
        Map<String, String> out = new LinkedHashMap<>();
        List<GenerationException> errs = new ArrayList<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                errs.addAll(gen.generateClasses(model, gm.version(model), out));
            }
        }
        failOnErrors(label, errs);
        merge(all, out, label, kindLog);
    }

    private static void failOnErrors(String label, List<GenerationException> errs) {
        if (!errs.isEmpty()) {
            throw new AssertionError("[SWAP-DUMP] " + label + ": " + errs.size()
                    + " generation error(s); first: " + errs.get(0).getMessage());
        }
    }

    private static void merge(Map<String, String> all, Map<String, String> out,
            String label, List<String> kindLog) {
        int collisions = 0;
        for (var e : out.entrySet()) {
            String prev = all.putIfAbsent(e.getKey(), e.getValue());
            if (prev != null) {
                collisions++;
                if (!prev.equals(e.getValue())) {
                    throw new AssertionError("[SWAP-DUMP] " + label
                            + ": conflicting content for key " + e.getKey());
                }
            }
        }
        kindLog.add(label + " files=" + out.size()
                + (collisions > 0 ? " (identical-collisions=" + collisions + ")" : ""));
    }
}
