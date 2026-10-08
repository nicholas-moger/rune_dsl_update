package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #361 — the rule runtime-then hoist + the fn ite-hoist seats + the singletonList
 * coerce ladder + the meta-collapse deref hoist + the ctor numeric-narrow chain + the
 * singles: 26 byte flips (13 drr POJO Rules + 4 drr FUNCTION + 3 cdm5 + 6 cdm6
 * FUNCTION) + 0 new / 0 away (regscan361 — 19 TOWARD movers all content-verified at
 * checkpoints).
 *
 * <p><b>ruleRuntimeThenHoist</b> (CollectionHandler): the RULE-path single-conditional
 * {@code then extract [cond]} consumer admit at {@code thenChainHasUnhandledControlFlow}
 * + the effective-else bare-get-collapse arm wrap in
 * {@code compileEffectiveElseConditionalBlock} (the GROUP-H nine: DTCC_TradeParty1/2-
 * ExecutionAgentIDType cftc/csa/common ×5 + DTCC_SDMSPIndicatorCounterparty1/2 cftc ×2
 * + SmallScaleBuySideEntityIndicator asic + DTCC_PhysicalCommodityContractIndicator
 * cftc).
 *
 * <p><b>fnIteHoistSeats</b> (ControlFlowHandler + ArithmeticHandler + CollectionHandler
 * + FunctionExpressionRenderer): {@code thenItemJavaClass} gains the BINARY-arithmetic
 * arm ({@code ArithmeticHandler.binaryResultItemJavaClass} — the LOCKSTEP mirror of the
 * render's witness-selection ladder, the #178 same-walk law) and the COLLAPSING
 * list-op arm (first/last/only-element recurse to the argument item; both arms carry
 * the then-chain-BASE seat discipline); a LAST over a PROVEN-single DISGUISED 2-name
 * chain wraps {@code MapperC.of(…)} (the same-walk both-hops-single proof); the
 * arith-operand conditional takes the #357 Mapper-form slot ({@code mapperFormSlot}
 * admits RArithmeticExpr parents — upstream compiles arithmetic operands against
 * MAPPER.wrapExtends); the alias sink admits the arith-top single-conditional-operand
 * body (MapPrincipalPayment cdm6 + Create_ContractType14__1 esma/fca +
 * GenerateObservationDates cdm5/cdm6).
 *
 * <p><b>singletonListCoerce</b> (FunctionExpressionRenderer + ControlFlowHandler): the
 * hoist-name collision DECLINE converts to the upstream {@code _}-prefix escape
 * (golden {@code _tradeState}); {@code hoistAsListLocalOrNull} extends from the single
 * if/else to the else-if LADDER — per-level source-order compiles, per-arm value
 * locals, terminal emptyList else (Create_Return + Create_Reset cdm5/cdm6 + rider
 * MapSwapTransactionSupplementDividendReturnTerms cdm6).
 *
 * <p><b>metaValueDerefHoist</b> (HandlerHelper + ConversionHandler + LiteralHandler +
 * FunctionExpressionRenderer): the SHARED meta-collapse deref-hoist emission
 * ({@code metaCollapseDerefHoistOrNull}) at the to-string source + list-literal
 * element seats — hoist the collapsed wrapper to a type-named local, re-present the
 * VALUE guarded {@code (x == null ? MapperS.<V>ofNull() : MapperS.of(x.getValue()))};
 * the wrapper resolves through the #264 walker plus a RECOVERY-LOCAL
 * element-preserving alias-body descent (InterestRateLeg1/2ReturnSwap csa).
 *
 * <p><b>ctorSetterNumericNarrowChain</b> (ConstructionHandler + JavaStatementScope):
 * the multi-line chain admit (the "single-line only" claim falsified by golden) + the
 * #327 hoist-reorder law extended to the LAMBDA channel
 * ({@code insertPendingLambdaHoists} + the per-pair watermark), interleaving each
 * numeric value local right after its own pair's thenArg statements
 * (PeriodicPaymentRule iosco v1/v2/v3).
 *
 * <p><b>singles</b>: blockArmComparisonAsMapper — a COMPARISON-RESULT ladder
 * rung/terminal arm coerces {@code .asMapper()} at the Mapper-returning return seat
 * (CustomBasketIndicator csa); aliasExtractItemBinding — the FunctionAliasHelper
 * EXTRACT arm binds the receiver ELEMENT as the implicit item around the body walk
 * (MapCommodityOptionToObservationTerms cdm6).
 *
 * <p>Whole-file byte comparisons run through the REAL D11 generation paths and revert
 * RED without the facets. The negative witnesses are load-bearing per the
 * witness-uniqueness law (OCCURRENCE counts, never line counts): every token below was
 * occurrence-counted in its PRE gen (f-probe-360post; counts noted per witness) and 0
 * in its golden — the flips REMOVE them.
 */
class FnIteHoistSeatsComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    /** The 13 drr POJO Rule flip carriers (GROUP H ×9 + GROUP K ×3 + the asMapper single). */
    private static final String[] DRR_POJO_RULES = {
            "drr/regulation/cftc/rewrite/valuation/reports/DTCC_TradeParty1ExecutionAgentIDTypeRule.java",
            "drr/regulation/cftc/rewrite/valuation/reports/DTCC_TradeParty2ExecutionAgentIDTypeRule.java",
            "drr/regulation/csa/rewrite/valuation/reports/DTCC_TradeParty1ExecutionAgentIDTypeRule.java",
            "drr/regulation/csa/rewrite/valuation/reports/DTCC_TradeParty2ExecutionAgentIDTypeRule.java",
            "drr/regulation/common/dtcc/reports/DTCC_TradeParty1ExecutionAgentIDTypeRule.java",
            "drr/regulation/cftc/rewrite/dtcc/reports/DTCC_SDMSPIndicatorCounterparty1Rule.java",
            "drr/regulation/cftc/rewrite/dtcc/reports/DTCC_SDMSPIndicatorCounterparty2Rule.java",
            "drr/regulation/asic/rewrite/valuation/reports/SmallScaleBuySideEntityIndicatorRule.java",
            "drr/regulation/cftc/rewrite/dtcc/reports/DTCC_PhysicalCommodityContractIndicatorRule.java",
            "drr/standards/iosco/cde/version1/payment/reports/PeriodicPaymentRule.java",
            "drr/standards/iosco/cde/version2/payment/reports/PeriodicPaymentRule.java",
            "drr/standards/iosco/cde/version3/payment/reports/PeriodicPaymentRule.java",
            "drr/regulation/csa/rewrite/trade/reports/CustomBasketIndicatorRule.java",
    };

    /** The 4 drr FUNCTION flip carriers. */
    private static final String[] DRR_FUNCTIONS = {
            "drr/projection/iso20022/esma/emir/refit/trade/functions/Create_ContractType14__1.java",
            "drr/projection/iso20022/fca/ukemir/refit/trade/functions/Create_ContractType14__1.java",
            "drr/regulation/csa/rewrite/trade/functions/InterestRateLeg1ReturnSwap.java",
            "drr/regulation/csa/rewrite/trade/functions/InterestRateLeg2ReturnSwap.java",
    };

    /** The 3 cdm5 FUNCTION flip carriers. */
    private static final String[] CDM5_FUNCTIONS = {
            "cdm/observable/asset/calculatedrate/functions/GenerateObservationDates.java",
            "cdm/event/common/functions/Create_Return.java",
            "cdm/event/common/functions/Create_Reset.java",
    };

    /** The 6 cdm6 FUNCTION flip carriers. */
    private static final String[] CDM6_FUNCTIONS = {
            "cdm/observable/asset/calculatedrate/functions/GenerateObservationDates.java",
            "cdm/event/common/functions/Create_Return.java",
            "cdm/event/common/functions/Create_Reset.java",
            "cdm/ingest/fpml/confirmation/payment/functions/MapPrincipalPayment.java",
            "cdm/ingest/fpml/confirmation/product/dividendswaptransactionsupplement/functions/MapSwapTransactionSupplementDividendReturnTerms.java",
            "cdm/ingest/fpml/confirmation/product/commodityoption/functions/MapCommodityOptionToObservationTerms.java",
    };

    private static Map<String, String> drrFnOutput;
    private static Map<String, String> drrCellOutput;
    private static Map<String, String> cdm5FnOutput;
    private static Map<String, String> cdm6FnOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            var drrCell = new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT);
            drrFnOutput = generateFunctions(drrCell);
            drrCellOutput = generateCell(drrCell);
        }
        if (cdm5CellAvailable()) {
            cdm5FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
        }
        if (cdm6CellAvailable()) {
            cdm6FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
    }

    /** The REAL D11 FUNCTION-kind generation path (function_comparison). */
    private static Map<String, String> generateFunctions(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var funcGen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        var errors = funcGen.generateWithErrors(output);
        assertTrue(errors.isEmpty(),
                () -> "Function generation reported " + errors.size() + " error(s): " + errors);
        return output;
    }

    /** The REAL D11 full-cell path (the POJO/Rule kinds ride RuleGenerator/ReportGenerator). */
    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell),
                D11CorpusRegressionTest.readDoNotPrune(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var enumGen = new EnumGenerator(gm);
        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                assertNoGenerationErrors(enumGen.generateClasses(model, version, output));
                assertNoGenerationErrors(pojoGen.generateClasses(model, version, output));
                assertNoGenerationErrors(choiceGen.generateClasses(model, version, output));
                assertNoGenerationErrors(ruleGen.generateClasses(model, version, output));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
            }
        }
        return output;
    }

    // -------------------------------------------- drr POJO Rule byte locks (13)

    @Test
    @EnabledIf("drrCellAvailable")
    void drrPojoRules_byteMatchGolden() throws IOException {
        for (String path : DRR_POJO_RULES) {
            assertCellByteMatchesGolden(path);
        }
    }

    // -------------------------------------------- drr FUNCTION byte locks (4)

    @Test
    @EnabledIf("drrCellAvailable")
    void drrFunctions_byteMatchGolden() throws IOException {
        for (String path : DRR_FUNCTIONS) {
            assertBytes(path, fn(drrFnOutput, path), DRR_GOLDEN_DIR);
        }
    }

    // -------------------------------------------- cdm FUNCTION byte locks (3 + 6)

    @Test
    @EnabledIf("cdm5CellAvailable")
    void cdm5Functions_byteMatchGolden() throws IOException {
        for (String path : CDM5_FUNCTIONS) {
            assertBytes(path, fn(cdm5FnOutput, path), CDM5_GOLDEN_DIR);
        }
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void cdm6Functions_byteMatchGolden() throws IOException {
        for (String path : CDM6_FUNCTIONS) {
            assertBytes(path, fn(cdm6FnOutput, path), CDM6_GOLDEN_DIR);
        }
    }

    // ------------------------------------------------------- negative witnesses

    /**
     * The GROUP-H witness (asic-valuation SmallScaleBuySide): the runtime
     * {@code .then(item -> item} chain links counted EXACTLY 5 occurrences in the PRE
     * gen (f-probe-360post) and 0 in the golden — the RULE-path then-extract consumer
     * admit hoists the pipe into thenArg statements.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void smallScaleBuySide_runtimeThenGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[7]), ".then(item -> item"),
                "The runtime .then( chain is gone (count 0 in golden)");
    }

    /**
     * The fnIteHoistSeats arith witness (MapPrincipalPayment cdm6): the inline ternary
     * {@code .getOrDefault(false) ? MapperMaths.} counted EXACTLY 1 occurrence in the
     * PRE gen and 0 in the golden — the item-form hoist replaces it.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapPrincipalPayment_inlineTernaryGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[3]),
                        ".getOrDefault(false) ? MapperMaths."),
                "The arith-then inline ternary is gone (count 0 in golden)");
    }

    /**
     * The fnIteHoistSeats list-op witness (esma Create_ContractType14__1): the ternary
     * else {@code .last() : MapperC.of().get())} counted EXACTLY 1 occurrence in the
     * PRE gen and 0 in the golden — the initializer-form hoist + the MapperC.of
     * receiver wrap replace it.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void contractType14_ternaryLastGone() {
        assertEquals(0, count(fn(drrFnOutput, DRR_FUNCTIONS[0]),
                        ".last() : MapperC.of().get())"),
                "The list-op-then inline ternary is gone (count 0 in golden)");
    }

    /**
     * The fnIteHoistSeats alias-seat witness (GenerateObservationDates cdm6): the
     * arith-operand ternary {@code exists(MapperS.of(lockoutDays)).getOrDefault(false) ? }
     * counted EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the Mapper-form
     * slot + the alias-sink admission replace it.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void generateObservationDates_operandTernaryGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[0]),
                        "exists(MapperS.of(lockoutDays)).getOrDefault(false) ? "),
                "The arith-operand ternary is gone (count 0 in golden)");
    }

    /**
     * The singletonListCoerce escape witness (Create_Return cdm6): the BARE inline
     * value {@code .addAfter(create_QuantityChange.evaluate(} counted EXACTLY 1
     * occurrence in the PRE gen and 0 in the golden — the {@code _tradeState} escape
     * hoist + the null-guard singletonList replace it.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void createReturn_bareAddAfterGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[1]),
                        ".addAfter(create_QuantityChange.evaluate("),
                "The bare addAfter value is gone (count 0 in golden)");
    }

    /**
     * The singletonListCoerce ladder witness (Create_Reset cdm6): the ITEM-form arm
     * assignment {@code ifThenElseResult = resolvePerformanceReset.evaluate(} counted
     * EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the LIST-form ladder's
     * per-arm value local + singletonList coercion replace it.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void createReset_itemFormArmGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[2]),
                        "ifThenElseResult = resolvePerformanceReset.evaluate("),
                "The item-form ladder arm is gone (count 0 in golden)");
    }

    /**
     * The metaValueDerefHoist witness (InterestRateLeg1ReturnSwap csa): the broken
     * item-receiver to-string {@code .get().map("to-string", Object::toString)}
     * counted EXACTLY 4 occurrences in the PRE gen and 0 in the golden — the deref
     * hoist + the guarded re-present + the enum toDisplayString replace them.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void interestRateLeg1_itemToStringGone() {
        assertEquals(0, count(fn(drrFnOutput, DRR_FUNCTIONS[2]),
                        ".get().map(\"to-string\", Object::toString)"),
                "The item-receiver to-string is gone (count 0 in golden)");
    }

    /**
     * The ctorSetterNumericNarrowChain witness (iosco v1 PeriodicPaymentRule): the
     * BARE multi-line chain setter value
     * {@code .setFixedRatePaymentFrequencyPeriodMultiplier(thenArg4} counted EXACTLY
     * 1 occurrence in the PRE gen and 0 in the golden — the interleaved bigDecimal0
     * hoist + the null-guard intValueExact narrow replace it.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void periodicPayment_bareChainSetterGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[9]),
                        ".setFixedRatePaymentFrequencyPeriodMultiplier(thenArg4"),
                "The bare chain setter value is gone (count 0 in golden)");
    }

    /**
     * The blockArmComparisonAsMapper witness (csa CustomBasketIndicator): the bare
     * ComparisonResult return tail {@code .getBasket()));} counted EXACTLY 2
     * occurrences in the PRE gen and 0 in the golden — every arm now returns
     * {@code …getBasket())).asMapper();}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void customBasketIndicator_bareComparisonReturnGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[12]), ".getBasket()));"),
                "The bare ComparisonResult return is gone (count 0 in golden)");
    }

    /**
     * The aliasExtractItemBinding witness (MapCommodityOptionToObservationTerms cdm6):
     * the fn-OUTPUT signature leak
     * {@code MapperS<? extends ObservationTerms> fpmlCalculationPeriodsSchedule}
     * counted EXACTLY 2 occurrences in the PRE gen (abstract + impl) and 0 in the
     * golden — the extract item binding types the alias from its body.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapCommodityOption_outputLeakGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[5]),
                        "MapperS<? extends ObservationTerms> fpmlCalculationPeriodsSchedule"),
                "The fn-output signature leak is gone (count 0 in golden)");
    }

    // ----------------------------------------------------------------- helpers

    private static void assertCellByteMatchesGolden(String path) throws IOException {
        assertBytes(path, cell(path), DRR_GOLDEN_DIR);
    }

    private static String cell(String path) {
        assertNotNull(drrCellOutput, "drr cell generation did not run — corpus unavailable?");
        String generated = drrCellOutput.get(path);
        assertNotNull(generated, "Class not generated: " + path);
        return generated;
    }

    private static String fn(Map<String, String> output, String path) {
        assertNotNull(output, "function generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "Class not generated: " + path);
        return generated;
    }

    private static void assertBytes(String path, String generated, Path goldenDir)
            throws IOException {
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " (PR #361).");
    }

    private static int count(String text, String token) {
        int n = 0;
        int i = text.indexOf(token);
        while (i >= 0) {
            n++;
            i = text.indexOf(token, i + token.length());
        }
        return n;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
