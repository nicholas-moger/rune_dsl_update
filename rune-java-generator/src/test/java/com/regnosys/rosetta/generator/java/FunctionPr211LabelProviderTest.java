package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PR #211 facet labelProviderImplicit (6 drr FUNCTION flips), whole-file byte anchors through
 * the REAL D11 loader. Each anchor reverts RED if the fix is removed; the law is green-safe by
 * construction (the fork emitted NO {@code @RuneLabelProvider} for any transform function while
 * every transform function golden carries it, so every carrier was already waivered — the
 * stash-baseline confirmed 0 now-matching pristine, and {@code function_comparison} stays green).
 *
 * <p><b>The law.</b> A normal function carrying a transform annotation
 * ({@code [ingest]} / {@code [enrich]} / {@code [projection]}) gets an auto-generated
 * {@code <Func>LabelProvider} class (emitted by {@code LabelProviderGenerator} on the same
 * {@code shouldGenerateLabelProvider} gate) AND a class-level
 * {@code @RuneLabelProvider(labelProvider=<Func>LabelProvider.class)} annotation — mirroring
 * upstream {@code FunctionGenerator.generateClass:122-125}. The fork's {@code detectLabelProvider}
 * only recovered the EXPLICIT {@code [labelProvider X]} source annotation (absent everywhere in
 * the corpus), so the implicit transform case was never surfaced: the projection functions
 * emitted neither the annotation nor its two supporting imports
 * ({@code com.rosetta.model.lib.annotations.RuneLabelProvider} + the generated
 * {@code <ns>.labels.<Func>LabelProvider}). Fix: {@code FunctionGenerator.buildStandardModel}
 * falls back to {@code labelProviderUtil.shouldGenerateLabelProvider(func)} and projects the
 * label-provider class via {@code typeTranslator.toLabelProviderJavaClass} when the explicit
 * detection is null; the template ({@code java-function.stg:26-27}) already emits the annotation
 * whenever {@code labelProviderClassName} is set. Exemplars: the clean uniform sub-family of 6
 * {@code Project_*ToDtccRdsHarmonized} projection functions (the heavier {@code *ToIso20022}
 * carriers co-occupy a separate cardinality-coercion divergence and stay waivered).
 */
class FunctionPr211LabelProviderTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR = DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> drrFunctionOutput;

    static boolean drrAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrAvailable()) {
            drrFunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell) throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(), D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = gen.generateWithErrors(output);
        assertTrue(genErrors.isEmpty(), cell + " FUNCTION generation reported errors: " + genErrors);
        return output;
    }

    private static final String REVERT =
            "the @RuneLabelProvider(labelProvider=<Func>LabelProvider.class) annotation + its two "
            + "supporting imports revert to absent — the implicit shouldGenerateLabelProvider "
            + "fallback in buildStandardModel was removed";

    /** cftc part43 trade — the [projection] transform function gains @RuneLabelProvider. */
    @Test
    @EnabledIf("drrAvailable")
    void cftcPart43Trade_labelProvider_byteMatchesGolden() throws IOException {
        assertByteMatches("drr/projection/dtcc/rds/harmonized/cftc/rewrite/trade/functions/Project_CftcPart43TradeReportToDtccRdsHarmonized.java", REVERT);
    }

    /** cftc part45 trade — sibling regime. */
    @Test
    @EnabledIf("drrAvailable")
    void cftcPart45Trade_labelProvider_byteMatchesGolden() throws IOException {
        assertByteMatches("drr/projection/dtcc/rds/harmonized/cftc/rewrite/trade/functions/Project_CftcPart45TradeReportToDtccRdsHarmonized.java", REVERT);
    }

    /** cftc valuation — the valuation projection variant. */
    @Test
    @EnabledIf("drrAvailable")
    void cftcValuation_labelProvider_byteMatchesGolden() throws IOException {
        assertByteMatches("drr/projection/dtcc/rds/harmonized/cftc/rewrite/valuation/functions/Project_CFTCValuationReportToDtccRdsHarmonized.java", REVERT);
    }

    /** csa trade — a different regulation namespace, same law. */
    @Test
    @EnabledIf("drrAvailable")
    void csaTrade_labelProvider_byteMatchesGolden() throws IOException {
        assertByteMatches("drr/projection/dtcc/rds/harmonized/csa/rewrite/trade/functions/Project_CSATradeReportToDtccRdsHarmonized.java", REVERT);
    }

    /** csa ppd — the partial-physical-delivery report projection. */
    @Test
    @EnabledIf("drrAvailable")
    void csaPpd_labelProvider_byteMatchesGolden() throws IOException {
        assertByteMatches("drr/projection/dtcc/rds/harmonized/csa/rewrite/trade/functions/Project_CsaPpdReportToDtccRdsHarmonized.java", REVERT);
    }

    /** csa valuation — closes the clean DtccRdsHarmonized sub-family of 6. */
    @Test
    @EnabledIf("drrAvailable")
    void csaValuation_labelProvider_byteMatchesGolden() throws IOException {
        assertByteMatches("drr/projection/dtcc/rds/harmonized/csa/rewrite/valuation/functions/Project_CSAValuationReportToDtccRdsHarmonized.java", REVERT);
    }

    private static void assertByteMatches(String path, String revertHint) throws IOException {
        assertNotNull(drrFunctionOutput, "Function generation did not run — corpus unavailable?");
        String generated = drrFunctionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — " + revertHint + ".");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
