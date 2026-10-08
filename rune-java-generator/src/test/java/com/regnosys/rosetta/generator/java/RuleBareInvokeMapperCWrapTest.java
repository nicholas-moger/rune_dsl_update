package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.utils.DeepFeatureCallUtil;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.ast.model.RModel;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #298 — facet bareInvokeMapperCWrap: ONE green-safe GENERATOR mechanism, 3 drr POJO Rule byte
 * flips (the cardinality-aware extension of #280's bareSymInvoke nav-receiver wrap).
 *
 * <p><b>The mechanism.</b> A bare FUNCTION invocation used as a navigation RECEIVER whose callee
 * OUTPUT is MULTI wraps {@code MapperC.<X>of(<fn>.evaluate(...))} not {@code MapperS.of(...)}. #280
 * made a disguised bare-FUNCTION nav receiver fire its invocation, but always wrapped it in
 * {@code MapperS.of(...)}; for a MULTI-output callee (e.g. {@code contract_Price_Monetary} returning
 * {@code PriceSchedule (0..*)}, navigated {@code -> priceType}) golden keeps the list cardinality:
 * {@code MapperC.<PriceSchedule>of(contract_Price_Monetary.evaluate(item.get())).<PriceTypeEnum>map(…)}.
 * The fork's {@code MapperS<List<PriceSchedule>>} then navigates a {@code List} (the feature does not
 * exist on {@code List}) — which never compiled, so every carrier was already a waivered mismatch.
 *
 * <p><b>Where it fires.</b> {@code ReferenceHandler.renderImplicitFunctionInvocation}, the
 * {@code navReceiver} branch. Cardinality-gated on {@code gm.isMulti(callee.output())}: a SINGLE-output
 * function nav stays {@code MapperS.of} (the #280 form unchanged — every #280 single-output carrier is
 * byte-identical). A META-annotated output declines (its emitted witness is the meta wrapper, not the
 * bare X — a separate corpus-unverified shape kept on the old {@code MapperS.of}). The element type is
 * the callee output's {@code resolveTypeCall}, so the wrap + the downstream navigation are typed from
 * one resolved attribute.
 *
 * <p><b>Green-safety (regscan: 3 flipped-out / 0 within-waiver regressions / 14 toward-golden / 7
 * neutral).</b> {@code MapperS.of} over a multi list value is a non-compiling type mismatch, so only an
 * already-waivered file is ever touched; a single-output baresym nav-receiver keeps {@code MapperS.of}
 * by the cardinality gate. byte-oracle / stash-baseline measured exactly 3 flips (drr POJO 402 → 399;
 * clean source = 12 stale waivers, {@code comm -23} = exactly the 3 PriceUnitOfMeasure/StrikePriceNoFormat
 * carriers); ALL FUNCTION cells (cdm5 81 / cdm6 236 / drr 207) + cdm/iso/fpml POJO byte-IDENTICAL.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr Rule output against the frozen
 * goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED for the flip locks.
 */
class RuleBareInvokeMapperCWrapTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> drrOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            drrOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        var labelProviderGen = new LabelProviderGenerator(
                gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                assertNoGenerationErrors(pojoGen.generateClasses(model, version, output));
                assertNoGenerationErrors(choiceGen.generateClasses(model, version, output));
                assertNoGenerationErrors(ruleGen.generateClasses(model, version, output));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
                assertNoGenerationErrors(labelProviderGen.generateClasses(model, version, output));
            }
        }
        funcGen.generate(output);
        return output;
    }

    // ==== Flip locks (revert-RED): a MULTI-output bare FUNCTION nav-receiver wraps MapperC.of. ====

    /**
     * Flip: {@code PriceUnitOfMeasure} (asic) navigates the bare FUNCTION
     * {@code contract_Price_Monetary} (output {@code PriceSchedule (0..*)}) as a nav receiver
     * ({@code -> priceType} / {@code -> priceExpression}). Golden renders
     * {@code MapperC.<PriceSchedule>of(contract_Price_Monetary.evaluate(item.get())).<PriceTypeEnum>map(…)};
     * the fork wrapped {@code MapperS.of(…)} (a {@code List} mis-typed single) before the fix.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void priceUnitOfMeasureAsic_multiOutputBareInvokeNav_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/asic/rewrite/trade/reports/PriceUnitOfMeasureRule.java");
    }

    /** Flip: {@code PriceUnitOfMeasure} (mas) — the same MULTI-output {@code contract_Price_Monetary}
     *  nav-receiver shape in the mas regime. */
    @Test
    @EnabledIf("drrCellAvailable")
    void priceUnitOfMeasureMas_multiOutputBareInvokeNav_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/mas/rewrite/trade/reports/PriceUnitOfMeasureRule.java");
    }

    /**
     * Flip: {@code StrikePriceNoFormat} (iosco cde version1) navigates the bare FUNCTION
     * {@code contract_StrikePrice} (output {@code PriceSchedule (0..*)}) at the WHOLE-OUTPUT seat:
     * {@code output = MapperC.<PriceSchedule>of(contract_StrikePrice.evaluate(input)).<BigDecimal>map("getValue", …).get();}
     * (the {@code .get()} collapse is downstream of the MapperC wrap).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void strikePriceNoFormatIosco_multiOutputBareInvokeNav_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version1/price/reports/StrikePriceNoFormatRule.java");
    }

    // ==== Green-safety / cardinality-gate locks: a SINGLE-output bare invoke nav-receiver keeps
    //      MapperS.of (the #280 form), so the GREEN carrier stays byte-identical. ====

    /**
     * Green-safety / cardinality-gate lock — a SINGLE-output bare FUNCTION nav-receiver keeps
     * {@code MapperS.of}. {@code UpiPreEnrichmentData} (GREEN) navigates
     * {@code create_AnnaDsbUpiRequestFromReportableEvent} (a SINGLE-output function) as a nav receiver
     * ({@code MapperS.of(create_AnnaDsbUpiRequestFromReportableEvent.evaluate(item.get())).<AnnaDsbUpiRequestTypeEnum>map(…)}).
     * The cardinality gate ({@code gm.isMulti(callee.output())} false) declines the MapperC wrap, so the
     * #280 {@code MapperS.of} form is preserved and the GREEN file stays byte-identical to golden.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void upiPreEnrichmentData_singleOutputBareInvokeNav_keepsMapperSofAndByteMatches() throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String gen = drrOutput.get("drr/enrichment/common/reports/UpiPreEnrichmentDataRule.java");
        assertNotNull(gen, "UpiPreEnrichmentDataRule not generated");
        assertTrue(gen.contains(
                        "MapperS.of(create_AnnaDsbUpiRequestFromReportableEvent.evaluate(item.get()))"
                        + ".<AnnaDsbUpiRequestTypeEnum>map"),
                "a SINGLE-output bare FUNCTION nav-receiver must keep MapperS.of — the cardinality gate "
                + "declines the MapperC wrap for a single-output callee");
        assertByteMatchesGolden("drr/enrichment/common/reports/UpiPreEnrichmentDataRule.java");
    }

    /**
     * Green-safety lock — a second GREEN single-output bare FUNCTION nav-receiver carrier
     * ({@code CollateralPortfolioCodeInitialMargin}, asic) stays byte-identical to golden after the
     * cardinality-gated change.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void collateralPortfolioCodeInitialMargin_singleOutputBareInvokeNav_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/asic/rewrite/trade/reports/CollateralPortfolioCodeInitialMarginRule.java");
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String generated = drrOutput.get(path);
        assertNotNull(generated, "Class not generated: " + path
                + " (emission failed or the path differs)");
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated drr output must byte-match the golden (newline-normalized) for "
                + path + " (PR #298 bareInvokeMapperCWrap).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
