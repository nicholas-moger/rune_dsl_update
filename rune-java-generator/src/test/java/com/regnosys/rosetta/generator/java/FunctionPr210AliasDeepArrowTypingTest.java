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
 * PR #210 — facet alias_signature_deep_arrow_typing (10 cdm6 FUNCTION flips), whole-file byte
 * anchors through the REAL D11 loader. Each anchor reverts RED if the fix is removed; the law is
 * green-safe by construction (a deep-call alias is waivered by construction — there are zero green
 * deep-call files — and the stash-baseline confirmed 0 now-matching pristine).
 *
 * <p><b>The law.</b> An alias whose body is a deep-arrow ({@code ->>}) navigation through a
 * one-of/choice receiver — e.g. {@code underlierEconomicTerms: economicTerms -> payout ->
 * OptionPayout -> underlier -> Product ->> economicTerms only-element} — lost its return TYPE in
 * the generated method signature. The parser leaves {@code RDeepFeatureCall.resolvedFeature()}
 * empty when the receiver navigates through a choice ({@code Product}), and
 * {@code FunctionAliasHelper.findAttributeDeep} only recurses for an {@code RDataTypeRef} receiver,
 * so the deep feature ({@code economicTerms}, several levels below the {@code Product} choice's
 * options) was not found and the signature fell back to the function OUTPUT type — these
 * qualification functions output {@code is_product boolean}, so the alias rendered
 * {@code MapperS<? extends boolean>} where golden has {@code MapperS<? extends EconomicTerms>}.
 * The BODY already rendered correctly (the deep_path_util_resolution #205 NavigationHandler path).
 *
 * <p><b>The fix.</b> {@code FunctionAliasHelper.inferDeepFeatureCallType} gains a choice-aware deep
 * search ({@code findDeepFeatureAttrChoiceAware}, the alias-signature sibling of the body-side
 * {@code NavigationHandler.findDeepFeatureAttr} / upstream
 * {@code DeepFeatureCallUtil.findDeepFeaturePaths}): when {@code findAttributeDeep} returns empty,
 * project the receiver RType to its data form (a choice narrows via {@code asRDataType()}) and
 * recurse into each attribute's data type, so {@code economicTerms} resolves to {@code EconomicTerms}
 * and the signature matches golden. All 10 carriers are cdm6 {@code Qualify_EquityOption_ParameterReturn*}.
 */
class FunctionPr210AliasDeepArrowTypingTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR = CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;

    static boolean cdm6Available() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cdm6Available()) {
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

    /** Correlation_Basket — the `Product ->> economicTerms` deep arrow types `MapperS<? extends EconomicTerms>`. */
    @Test
    @EnabledIf("cdm6Available")
    void qualifyEquityOptionParameterReturnCorrelationBasket_aliasDeepArrow_byteMatchesGolden() throws IOException {
        assertByteMatches("cdm/product/qualification/functions/Qualify_EquityOption_ParameterReturnCorrelation_Basket.java",
                "the alias signature `MapperS<? extends EconomicTerms> underlierEconomicTerms(...)` reverts to "
                + "`MapperS<? extends boolean>` — the choice-aware deep search was removed");
    }

    /** Variance_Basket — the exemplar carrier instrumented during characterization. */
    @Test
    @EnabledIf("cdm6Available")
    void qualifyEquityOptionParameterReturnVarianceBasket_aliasDeepArrow_byteMatchesGolden() throws IOException {
        assertByteMatches("cdm/product/qualification/functions/Qualify_EquityOption_ParameterReturnVariance_Basket.java",
                "the alias signature `MapperS<? extends EconomicTerms> underlierEconomicTerms(...)` reverts to "
                + "`MapperS<? extends boolean>`");
    }

    /** Dividend_SingleName — a different ParameterReturn family + the SingleName variant. */
    @Test
    @EnabledIf("cdm6Available")
    void qualifyEquityOptionParameterReturnDividendSingleName_aliasDeepArrow_byteMatchesGolden() throws IOException {
        assertByteMatches("cdm/product/qualification/functions/Qualify_EquityOption_ParameterReturnDividend_SingleName.java",
                "the alias signature `MapperS<? extends EconomicTerms> underlierEconomicTerms(...)` reverts to "
                + "`MapperS<? extends boolean>`");
    }

    /** Volatility_Index — a third family + the Index variant (broadest coverage of the 10-carrier set). */
    @Test
    @EnabledIf("cdm6Available")
    void qualifyEquityOptionParameterReturnVolatilityIndex_aliasDeepArrow_byteMatchesGolden() throws IOException {
        assertByteMatches("cdm/product/qualification/functions/Qualify_EquityOption_ParameterReturnVolatility_Index.java",
                "the alias signature `MapperS<? extends EconomicTerms> underlierEconomicTerms(...)` reverts to "
                + "`MapperS<? extends boolean>`");
    }

    private static void assertByteMatches(String path, String revertHint) throws IOException {
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
