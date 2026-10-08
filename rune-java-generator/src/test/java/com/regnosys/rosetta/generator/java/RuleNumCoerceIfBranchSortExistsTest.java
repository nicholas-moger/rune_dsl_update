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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #301 — THREE disjoint green-safe GENERATOR mechanisms (a clean thin-tail compose, the #300
 * pattern), 6 drr POJO Rule + 1 drr FUNCTION byte flips. All green-safe by construction (each fires
 * only on a shape that did not compile / byte-match before). 4 source files (CollectionHandler +
 * ReferenceHandler + NavigationHandler + HandlerHelper/ExistenceHandler); parser UNTOUCHED.
 *
 * <p><b>(A) numericCoercionIfBranchDrain (3 drr POJO).</b> The #300-deferred M1 conditional-block-
 * lambda case. A NULLABLE Integer nav-chain into a {@code BigDecimal} callee param inside a
 * {@code mapSingleToItem(item -> if <c> then …)} ELSELESS conditional block-lambda
 * ({@code if (<c>.getOrDefault(false)) { return …; } return MapperS.<T>ofNull();}). #300 DECLINED
 * the recovery there ({@code numCoerceArgInConditionalLambdaBody}) because the LAMBDA_CHANNEL drain
 * at the lambda TOP re-rendered the conditional as the pre-#223 inline ternary. PR #301 refines the
 * gate to ALLOW the elseless form and drains the pending lambda hoist INSIDE the if-branch
 * ({@code CollectionHandler.compileElselessConditionalBlock} — the #276 drain-inside-the-branch
 * analogue): {@code final Integer integer = <chain>.get();} lands before
 * {@code return MapperS.of(adjustPeriodMultiplier.evaluate(…, (integer == null ? null :
 * BigDecimal.valueOf(integer))));}. Carriers: FloatingRateResetFrequency{MultiplierOfLeg2,
 * PeriodOfLeg1, PeriodOfLeg2} (esma).
 *
 * <p><b>(B) sortLambdaItemTyping (2 drr POJO).</b> A {@code .sort(item -> item.<Date>map(
 * "getEffectiveDate", X -> X.getEffectiveDate()))} comparator-key lambda binds its implicit
 * {@code item} to the sorted list's ELEMENT type (NotionalPeriod) exactly like max/min, but
 * {@code NavigationHandler.implicitItemArgument} declined a sort lambda ("sort/reduce keep their own
 * conventions"), so the nested-map lambda var was named from the rule from-type
 * ({@code transactionReportInstruction}) not the element type ({@code notionalPeriod}). Adding the
 * {@code RSortExpr} arm names it from the element (the #176/#255 lambda-naming law at the sort seat).
 * COMPILES-divergent (the param name is cosmetic). Carriers: NotionalQuantityScheduleLeg1/2 (iosco cde v1).
 *
 * <p><b>(C) existsOperandMapperCWrap (1 drr POJO + 1 drr FUNCTION).</b> The #298 bareInvokeMapperCWrap
 * cardinality law at the bare-OPERAND seat (vs #298's nav-receiver seat). A bare MULTI-output function
 * as an {@code exists(...)} operand wraps {@code exists(MapperC.<X>of(<fn>.evaluate(...)))} not
 * {@code MapperS.of(...)} — {@code HandlerHelper.wrapBareInvocationOperand} (called by ExistenceHandler)
 * is now cardinality-aware. A single-output function keeps {@code MapperS.of} (the #256 form). SHARED
 * rule+function seat: it flipped UnderlyingIdentificationType (asic, POJO) AND IsUnderlierForIndex
 * (csa, FUNCTION — {@code exists(MapperS.of(getUnderlierIDForIndexCSA.evaluate(item.get())))} &rarr;
 * {@code exists(MapperC.<String>of(...))}), 0 regressions (regscan).
 *
 * <p><b>Gates.</b> byte-oracle / stash-baseline 6 drr POJO ({@code comm -23} = exactly the 6 carriers);
 * regscan (same-session PRE clean vs POST, all 8 cells) 0 within-waiver regressions / 0 new mismatches /
 * 7 flipped-out (6 POJO + 1 FUNCTION) / 11 toward-golden + 14 neutral; ALL POJO cdm/iso/fpml +
 * cdm5/cdm6 FUNCTION mismatch counts byte-stable (cdm6 FUNCTION 2 changed toward-golden/neutral — the
 * C shared seat). Anchors are WHOLE-FILE byte comparisons of the generated drr output against the
 * frozen goldens (newline-normalized), through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED for the 7 flip locks +
 * the positive-content lock; the C single-output green-safety lock passes on both clean and fixed source.
 */
class RuleNumCoerceIfBranchSortExistsTest {

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

    // ==== (A) numericCoercionIfBranchDrain flip locks (revert-RED) ====

    /** Flip: {@code FloatingRateResetFrequencyMultiplierOfLeg2} (esma) — the periodMultiplier Integer
     *  hoist lands INSIDE the if-branch of the elseless conditional block-lambda (the #300-deferred M1). */
    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRateResetFrequencyMultiplierOfLeg2Esma_ifBranchDrain_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/esma/emir/refit/trade/reports/FloatingRateResetFrequencyMultiplierOfLeg2Rule.java");
    }

    /** Flip: {@code FloatingRateResetFrequencyPeriodOfLeg1} (esma) — same if-branch Integer hoist. */
    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRateResetFrequencyPeriodOfLeg1Esma_ifBranchDrain_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/esma/emir/refit/trade/reports/FloatingRateResetFrequencyPeriodOfLeg1Rule.java");
    }

    /** Flip: {@code FloatingRateResetFrequencyPeriodOfLeg2} (esma) — same if-branch Integer hoist. */
    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRateResetFrequencyPeriodOfLeg2Esma_ifBranchDrain_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/esma/emir/refit/trade/reports/FloatingRateResetFrequencyPeriodOfLeg2Rule.java");
    }

    // ==== (B) sortLambdaItemTyping flip locks (revert-RED) ====

    /** Flip: {@code NotionalQuantityScheduleLeg1} (iosco cde v1) — the {@code .sort(item -> item.<Date>map(
     *  "getEffectiveDate", …))} nested-map lambda var named from the sorted element type
     *  ({@code notionalPeriod}) not the rule from-type ({@code transactionReportInstruction}). */
    @Test
    @EnabledIf("drrCellAvailable")
    void notionalQuantityScheduleLeg1Iosco_sortLambdaItemTyping_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version1/quantity/reports/NotionalQuantityScheduleLeg1Rule.java");
    }

    /** Flip: {@code NotionalQuantityScheduleLeg2} (iosco cde v1) — same sort-comparator lambda-naming. */
    @Test
    @EnabledIf("drrCellAvailable")
    void notionalQuantityScheduleLeg2Iosco_sortLambdaItemTyping_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version1/quantity/reports/NotionalQuantityScheduleLeg2Rule.java");
    }

    // ==== (C) existsOperandMapperCWrap flip locks (revert-RED) ====

    /** Flip: {@code UnderlyingIdentificationType} (asic, POJO) — a MULTI-output bare function as an
     *  {@code exists(...)} operand wraps {@code exists(MapperC.<ProductIdentifier>of(...))}. */
    @Test
    @EnabledIf("drrCellAvailable")
    void underlyingIdentificationTypeAsic_existsOperandMapperCWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/asic/rewrite/trade/reports/UnderlyingIdentificationTypeRule.java");
    }

    /** Flip: {@code IsUnderlierForIndex} (csa, FUNCTION) — the SAME cardinality-aware exists-operand wrap
     *  fires on a FUNCTION (the shared ExistenceHandler seat): {@code exists(MapperC.<String>of(
     *  getUnderlierIDForIndexCSA.evaluate(item.get())))}. */
    @Test
    @EnabledIf("drrCellAvailable")
    void isUnderlierForIndexCsa_existsOperandMapperCWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/csa/rewrite/functions/IsUnderlierForIndex.java");
    }

    // ==== Positive-content + green-safety locks ====

    /**
     * Positive-content lock (revert-RED) — the three distinct render shapes: A's if-branch Integer
     * hoist + coercion (MultiplierOfLeg2), B's element-typed sort lambda param (NotionalQuantityScheduleLeg1),
     * C's cardinality-aware {@code MapperC.<X>of} exists operand (UnderlyingIdentificationType). Fails RED
     * on clean source.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void pr301RenderShapesPresent() throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");

        String multiplierLeg2 = drrOutput.get(
                "drr/regulation/esma/emir/refit/trade/reports/FloatingRateResetFrequencyMultiplierOfLeg2Rule.java");
        assertNotNull(multiplierLeg2, "FloatingRateResetFrequencyMultiplierOfLeg2 not generated");
        assertTrue(multiplierLeg2.contains(
                "final Integer integer = item.<ResetDates>map(\"getResetDates\""),
                "A: MultiplierOfLeg2 must hoist `final Integer integer = …` inside the if-branch");
        assertTrue(multiplierLeg2.contains("(integer == null ? null : BigDecimal.valueOf(integer))"),
                "A: MultiplierOfLeg2 must render the null-safe Integer->BigDecimal coercion");

        String notionalLeg1 = drrOutput.get(
                "drr/standards/iosco/cde/version1/quantity/reports/NotionalQuantityScheduleLeg1Rule.java");
        assertNotNull(notionalLeg1, "NotionalQuantityScheduleLeg1 not generated");
        assertTrue(notionalLeg1.contains("notionalPeriod -> notionalPeriod.getEffectiveDate()"),
                "B: NotionalQuantityScheduleLeg1 must name the sort nested-map lambda from the element type");
        assertFalse(notionalLeg1.contains(
                "transactionReportInstruction -> transactionReportInstruction.getEffectiveDate()"),
                "B: the rule from-type name must NOT be used for the sorted-element nav lambda");

        String underlyingIdType = drrOutput.get(
                "drr/regulation/asic/rewrite/trade/reports/UnderlyingIdentificationTypeRule.java");
        assertNotNull(underlyingIdType, "UnderlyingIdentificationType not generated");
        assertTrue(underlyingIdType.contains(
                "exists(MapperC.<ProductIdentifier>of(getUnderlierProductIdentifier.evaluate(item.get())))"),
                "C: UnderlyingIdentificationType must wrap the multi-output exists operand MapperC.<X>of");
    }

    /**
     * Green-safety lock (C single-output discriminator) — {@code DTCC_OriginalSwapSDRIDType} (cftc) is a
     * GREEN rule whose {@code exists} operand is a SINGLE-output bare function
     * ({@code originalSwapSDRIdentifierRule}); the cardinality-aware wrap must keep {@code MapperS.of}
     * (NOT {@code MapperC.<…>of}). Passes on BOTH clean and fixed source (a single-output operand was
     * already {@code MapperS.of} and must stay so — the C over-fire guard). This rule is not waivered.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccOriginalSwapSDRIDTypeCftc_singleOutputExists_keepsMapperSOf() throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String gen = drrOutput.get(
                "drr/regulation/cftc/rewrite/dtcc/reports/DTCC_OriginalSwapSDRIDTypeRule.java");
        assertNotNull(gen, "DTCC_OriginalSwapSDRIDType not generated");
        assertTrue(gen.contains("exists(MapperS.of(originalSwapSDRIdentifierRule.evaluate(item.get())))"),
                "C: a single-output bare-function exists operand must keep MapperS.of");
        assertFalse(gen.contains("MapperC.<String>of(originalSwapSDRIdentifierRule.evaluate(item.get()))"),
                "C: a single-output exists operand must NOT be wrapped MapperC.<X>of (over-fire guard)");
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
                + path + " (PR #301).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
