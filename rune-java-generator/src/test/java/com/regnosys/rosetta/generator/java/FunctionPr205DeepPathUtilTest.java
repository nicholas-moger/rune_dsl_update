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
 * PR #205 — facet deep_path_util_resolution (4 FUNCTION flips, all cdm6 ingest~product
 * qualification), whole-file byte anchors through the REAL D11 loader (transitive rune-fpml dep
 * per PR #184). Each anchor reverts RED if the fix is removed; green-safe by construction — every
 * deep-arrow ({@code ->>}) call was a dormant {@code /* TODO(M7b-4): wire DeepPathUtil *}{@code /}
 * stub (0 deep-call references in any gen file), so the change only ADDS (stash-baseline confirmed
 * 0 now-matching pristine).
 *
 * <p><b>The mechanism.</b> A {@code ->>} deep feature call (e.g. {@code product ->> economicTerms})
 * was rendered as the non-compiling placeholder
 * {@code .map("chooseEconomicTerms", _economicTerms -> /* TODO *}{@code / chooseEconomicTerms(_economicTerms))}
 * because (1) the parser leaves {@code RDeepFeatureCall.resolvedFeature()} EMPTY for a receiver that
 * navigates through a one-of/choice type (its {@code findAttributeDeep} does not traverse choice
 * options), so the {@code <Type>} witness + map/mapC cardinality were lost; and (2)
 * {@code resolveReceiverTypeName} was {@code resolvedFeature()}-only, so the lambda var +
 * {@code <type>DeepPathUtil} field + {@code @Inject} import never resolved. The golden is
 * {@code .<EconomicTerms>map("chooseEconomicTerms", product -> productDeepPathUtil.chooseEconomicTerms(product))}
 * + {@code import cdm.product.template.util.ProductDeepPathUtil} + the {@code @Inject} field.
 *
 * <p><b>The fix (generator-only, the deep-arrow sibling of the single-arrow fallbackResolveFeature).</b>
 * {@code NavigationHandler.resolveDeepFeature} recovers the deep feature attribute by a gm-aware,
 * choice-aware deep search ({@code findDeepFeatureAttr}, mirroring upstream
 * {@code DeepFeatureCallUtil.findDeepFeaturePaths}) → the {@code <Type>} witness + cardinality;
 * {@code resolveDeepReceiverTypeName} / {@code resolveDeepReceiverSymbolId} resolve the receiver type
 * via {@code toJavaReferenceType(resolveReceiverRType(..))} (whose JavaClass carries BOTH the simple
 * name — lambda var / field — and the package — the {@code @Inject} import namespace, unambiguous
 * across the cdm/fpml same-named {@code Product}); and {@code FunctionDependencyCollector} (now given
 * an {@code ExpressionCompiler}) emits the {@code @Inject} field from the SAME resolution.
 */
class FunctionPr205DeepPathUtilTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR = CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;

    static boolean cellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellAvailable()) {
            cdm6FunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
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

    /** {@code product ->> economicTerms} through a one-of/choice Product (TWO deep calls in one line). */
    @Test
    @EnabledIf("cellAvailable")
    void qualifyCommoditySwaption_deepEconomicTerms_byteMatchesGolden() throws IOException {
        assertCdm6("cdm/product/qualification/functions/Qualify_Commodity_Swaption.java",
                "the resolved `.<EconomicTerms>map(\"chooseEconomicTerms\", product -> "
                + "productDeepPathUtil.chooseEconomicTerms(product))` + ProductDeepPathUtil import/@Inject "
                + "revert to the `/* TODO(M7b-4) */ chooseEconomicTerms(_economicTerms)` stub");
    }

    /** A second economicTerms-through-Product carrier (single deep call). */
    @Test
    @EnabledIf("cellAvailable")
    void qualifyCreditDefaultSwaption_deepEconomicTerms_byteMatchesGolden() throws IOException {
        assertCdm6("cdm/product/qualification/functions/Qualify_CreditDefaultSwaption.java",
                "the resolved DeepPathUtil delegation + import/@Inject revert to the TODO stub");
    }

    /** A third economicTerms-through-Product carrier. */
    @Test
    @EnabledIf("cellAvailable")
    void qualifyInterestRateOptionSwaption_deepEconomicTerms_byteMatchesGolden() throws IOException {
        assertCdm6("cdm/product/qualification/functions/Qualify_InterestRate_Option_Swaption.java",
                "the resolved DeepPathUtil delegation + import/@Inject revert to the TODO stub");
    }

    /**
     * A DIFFERENT DeepPathUtil type + feature ({@code index ->> assetClass} →
     * {@code IndexDeepPathUtil.chooseAssetClass(index)}) — proves the {@code <Type>} witness +
     * {@code @Inject} namespace come from the RESOLVED receiver (a DISTINCT package
     * {@code cdm.observable.asset.util}, not {@code cdm.product.template.util}), not a name-only lookup.
     */
    @Test
    @EnabledIf("cellAvailable")
    void qualifyTotalReturnSwapIndex_deepAssetClass_byteMatchesGolden() throws IOException {
        assertCdm6("cdm/product/qualification/functions/Qualify_TotalReturnSwap_Index.java",
                "the resolved IndexDeepPathUtil.chooseAssetClass delegation + import/@Inject revert to the TODO stub");
    }

    private static void assertCdm6(String path, String revertHint) throws IOException {
        assertNotNull(cdm6FunctionOutput, "Function generation did not run — corpus unavailable?");
        String generated = cdm6FunctionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = CDM6_GOLDEN_DIR.resolve(path);
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
