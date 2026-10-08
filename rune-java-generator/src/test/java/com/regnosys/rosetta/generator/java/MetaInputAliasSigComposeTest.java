package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
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
 * PR #342 anchor — the seven-facet compose (27 FUNCTION flips: 8 cdm5 + 13 cdm6 + 6 drr;
 * the two CommodityBasisLeg flip locks live in {@link FunctionCollapsedMetaDerefSinkTest},
 * whose A3 decline lock the seventh facet CONVERTED — this class carries the other 25).
 *
 * <p><b>F1 — {@code metaInputParam}</b> (+5: Create_Cashflow cdm5 +
 * Update{Price,Quantity}AmountForEachMatchingQuantity ×2 cdm cells): a {@code [metadata …]}-
 * annotated FUNCTION INPUT renders the concrete meta wrapper — the #186 metaWit javadoc's
 * own "distinct, separately-tracked facet". Signature seats via
 * {@code FunctionGenerator.resolveParam} (the isOutput gate dropped); the body reads via
 * {@code NavigationHandler.metaInputParamWrapper} (identity-gated to the enclosing
 * function's declared single-card inputs): the typed MapperS.of wrap (ctor values strip to
 * the bare wrapper name — the #327-B2 render-truth proof selects the PLAIN setter,
 * golden {@code .setCurrency(currency)}), the nav-receiver null-guarded ofNull deref
 * {@code (price == null ? MapperS.<PriceSchedule>ofNull() : MapperS.of(price.getValue()))},
 * the evaluate-arg value deref {@code (price == null ? null : price.getValue())}, and the
 * output-root {@code .getOrCreateValue()} SET deref (the #341 next-segment-must-RESOLVE
 * belt at the ROOT seat). Population CORPUS-CLOSED: exactly 5 goldens carry meta-typed
 * evaluate params (all 5 were divergent carriers) — zero green risk by construction.
 *
 * <p><b>F2 — {@code aliasSigReturnTypeLeak}</b> (+8: CashPriceQuantityNoOfUnitsTriangulation
 * ×2 + EquityCashSettlementAmount + MapOtherAgreements + Create_Exercise + MapParty cdm6 +
 * PrimeBrokerageTransactionIndicatorFunc + ExtractCommodityClassification drr): four
 * P342A-probe-verified arms — DISTINCT joins the #252 then-body cardinality op set
 * (multi-preserving) in {@code FunctionAliasHelper.inferExpressionType}; {@code to-int}
 * types Integer; the #228 shortcut-to-RType walker gains filter/conditional/then arms so
 * disguised alias-hop chains resolve BEFORE the evr token fallback (the {@code
 * MapperS<payout>} / {@code MapperS<commodityUnderlier>} leaks); and
 * {@code chainProvesMulti}'s disguised-evr arm consults the COMPILER-CARRYING
 * {@code resolveDisguisedFeature} — the SAME walk the body witness renders from
 * (render-truth lockstep: MapParty + the ExtractCommodityClassification
 * filterItemNullSafe arity).
 *
 * <p><b>F3 — {@code aliasDistinctThenHoist}</b> (+4: Create_TerminationInstruction +
 * ResolveRepurchaseTransferInstruction ×2 cdm cells): DISTINCT joins
 * {@code isBareItemCardinalityThenBody} (the render admission — the RImplicitVariable
 * guard keeps the MapBuyerSellerToAccountPartyReference {@code distinct(item).get()}
 * decline lock in {@code FunctionAliasThenSignatureTypingTest} UNMOVED), and the
 * deep-then decl's #144/#330 wrapper upgrade DECLINES on a ctor-bodied extract level
 * (a ctor produces the bare type — golden
 * {@code final MapperC<NonNegativeQuantitySchedule> thenArg}).
 *
 * <p><b>F4 — {@code closureParamDirectNav}</b> (+4: CompareQuantityByUnitOfAmount ×2 cdm
 * cells + InterestRateLeg1/2CrossCurrency drr): an EXPLICIT closure param
 * ({@code filter leg [ … ]}, {@code extract q1 [ … ]}) navigates BARE — three lockstep
 * arms off ONE resolution ({@code resolveReceiverDataType}'s closure arm: the owner
 * list-op argument's element; then-owners decline, the #180/#331 gate): witness +
 * map/mapC arity + the type-derived lambda var ({@code interestRatePayout ->}, not the
 * shadow-escape {@code _leg}), and the nav-receiver render replaces the never-compiling
 * {@code MapperS.of(param)} double-wrap with the bare Mapper-typed param — typed the
 * concrete META WRAPPER when the owner argument's terminal attribute is meta-annotated,
 * so {@code coerceNavigationReceiver} auto-inserts golden's Type-coercion deref (the
 * SortIdentifiers partial-heal lock below).
 *
 * <p><b>F5 — {@code multiSetterGetVsGetMulti}</b> (+2: Create_QuantityChange +
 * Create_TermsChange cdm6): {@code extractValueProvesMulti} gains proven-multi
 * ALIAS-invocation ({@code aliasReceiverProvesMulti}) + NAV/disguised-evr
 * ({@code chainProvesMulti}) value shapes at the multi-leaf SET seat → {@code .getMulti()}
 * (a List-typed leaf set with the scalar {@code .get()} never compiled; a single value
 * keeps the #212 singletonList coercion ownership).
 *
 * <p><b>F6 — {@code coercionWitnessFollowsCalleeParam}</b> (+2: Create_StockSplit ×2 cdm
 * cells): the evaluate-arg B2 arm passes the callee parameter's resolved element to the
 * new {@code coerceNavigationReceiver} 3-arg overload — upstream compiles every arg
 * against the parameter's expected item type, so the Type-coercion witness follows the
 * PARAM ({@code <QuantitySchedule>} over a FieldWithMetaNonNegativeQuantitySchedule
 * chain).
 *
 * <p>Every pre-fix form was NON_COMPILING except Create_Cashflow cdm5 (COMPILES-divergent
 * — the {@code String} param + {@code setCurrencyValue} is legal Java; golden's wrapper
 * form differs byte-wise): a wrapper passed where a value/List is expected, an invalid
 * signature type-arg ({@code MapperS<? extends boolean>} / an unknown token class), a
 * MapperS double-wrap navigated by value getters, a scalar {@code .get()} into a List
 * setter. Whole-file byte comparisons run through the REAL D11 FUNCTION generation path
 * and revert RED without the facets.
 */
class MetaInputAliasSigComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // F1 flip carriers (the corpus-closed meta-input population).
    private static final String CREATE_CASHFLOW =
            "cdm/event/common/functions/Create_Cashflow.java";
    private static final String UPDATE_PRICE_AMOUNT =
            "cdm/product/common/settlement/functions/UpdatePriceAmountForEachMatchingQuantity.java";
    private static final String UPDATE_QUANTITY_AMOUNT =
            "cdm/product/common/settlement/functions/UpdateQuantityAmountForEachMatchingQuantity.java";
    // F2 flip carriers.
    private static final String CASH_PRICE_QUANTITY_TRIANGULATION =
            "cdm/observable/common/functions/CashPriceQuantityNoOfUnitsTriangulation.java";
    private static final String EQUITY_CASH_SETTLEMENT_AMOUNT =
            "cdm/event/common/functions/EquityCashSettlementAmount.java";
    private static final String MAP_OTHER_AGREEMENTS =
            "cdm/ingest/fpml/confirmation/legal/functions/MapOtherAgreements.java";
    private static final String CREATE_EXERCISE =
            "cdm/event/common/functions/Create_Exercise.java";
    private static final String MAP_PARTY =
            "cdm/ingest/fpml/confirmation/party/functions/MapParty.java";
    private static final String PRIME_BROKERAGE_INDICATOR =
            "drr/regulation/cftc/rewrite/functions/PrimeBrokerageTransactionIndicatorFunc.java";
    private static final String EXTRACT_COMMODITY_CLASSIFICATION =
            "drr/regulation/common/functions/ExtractCommodityClassification.java";
    // F3 flip carriers.
    private static final String CREATE_TERMINATION_INSTRUCTION =
            "cdm/event/common/functions/Create_TerminationInstruction.java";
    private static final String RESOLVE_REPURCHASE_TRANSFER =
            "cdm/event/common/functions/ResolveRepurchaseTransferInstruction.java";
    // F4 flip carriers.
    private static final String COMPARE_QUANTITY_BY_UNIT =
            "cdm/base/math/functions/CompareQuantityByUnitOfAmount.java";
    private static final String INTEREST_RATE_LEG1_CROSS_CURRENCY =
            "drr/regulation/csa/rewrite/trade/functions/InterestRateLeg1CrossCurrency.java";
    private static final String INTEREST_RATE_LEG2_CROSS_CURRENCY =
            "drr/regulation/csa/rewrite/trade/functions/InterestRateLeg2CrossCurrency.java";
    // F5 flip carriers.
    private static final String CREATE_QUANTITY_CHANGE =
            "cdm/event/common/functions/Create_QuantityChange.java";
    private static final String CREATE_TERMS_CHANGE =
            "cdm/event/common/functions/Create_TermsChange.java";
    // F6 flip carriers.
    private static final String CREATE_STOCK_SPLIT =
            "cdm/event/common/functions/Create_StockSplit.java";
    // F4 partial-heal carrier: the meta-element closure param deref landed (48 -> 28
    // TOWARD) but the file stays divergent on the separate runtime-then class.
    private static final String SORT_IDENTIFIERS =
            "drr/regulation/common/trade/link/functions/SortIdentifiers.java";
    // Stays-deferred breadcrumb: the double-underscore collision-escape depth
    // (lambdaParamUnderscoreEscape — golden `__interestRatePayout` where a second
    // same-desired-name collision escalates) is a NAMING-LAW facet deliberately
    // excluded from this PR's scope. Gen keeps the single-underscore form.
    private static final String INTEREST_CASH_SETTLEMENT_AMOUNT =
            "cdm/event/common/functions/InterestCashSettlementAmount.java";

    private static Map<String, String> drrFnOutput;
    private static Map<String, String> cdm6FnOutput;
    private static Map<String, String> cdm5FnOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            drrFnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
        if (cdm6CellAvailable()) {
            cdm6FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
        if (cdm5CellAvailable()) {
            cdm5FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
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
        assertNoGenerationErrors(funcGen.generateWithErrors(output));
        return output;
    }

    // ==== F1 flip locks (revert-RED) ====

    @Test
    @EnabledIf("cdm5CellAvailable")
    void createCashflow_cdm5_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, CREATE_CASHFLOW);
    }

    @Test
    @EnabledIf("cdm5CellAvailable")
    void updatePriceAmount_cdm5_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, UPDATE_PRICE_AMOUNT);
    }

    @Test
    @EnabledIf("cdm5CellAvailable")
    void updateQuantityAmount_cdm5_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, UPDATE_QUANTITY_AMOUNT);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void updatePriceAmount_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, UPDATE_PRICE_AMOUNT);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void updateQuantityAmount_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, UPDATE_QUANTITY_AMOUNT);
    }

    /**
     * Positive-content lock (revert-RED): the four F1 body access forms land in one
     * carrier — the wrapper-typed signature, the output-root {@code getOrCreateValue}
     * SET deref, the null-guarded ofNull nav receiver, and the inline evaluate-arg
     * value deref (golden's exact fragments).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void updatePriceAmount_cdm6_metaInputAccessForms() {
        String gen = gen(cdm6FnOutput, UPDATE_PRICE_AMOUNT);
        assertTrue(gen.contains(
                "evaluate(FieldWithMetaPriceSchedule price, List<? extends PriceSchedule> change,"),
                "The [metadata location] input must render the concrete wrapper param");
        assertTrue(gen.contains(".getOrCreateValue().setValue("),
                "The meta-wrapped OUTPUT root must deref getOrCreateValue before the value setter");
        assertTrue(gen.contains(
                "(price == null ? MapperS.<PriceSchedule>ofNull() : MapperS.of(price.getValue()))"),
                "A nav from the meta param must use the null-guarded ofNull deref receiver");
        assertTrue(gen.contains("priceUnitEquals.evaluate(item.get(), (price == null ? null : price.getValue()))"),
                "A bare meta param into a value-typed callee must deref inline");
    }

    /**
     * Positive-content lock (revert-RED): the wrapper param passes BARE to the
     * wrapper-typed setter — the #327-B2 render-truth proof selects the PLAIN setter
     * (pre-facet: {@code String currency} + {@code setCurrencyValue}, legal Java but
     * byte-divergent — the ONE COMPILES-divergent carrier of this compose).
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void createCashflow_cdm5_plainSetterBareWrapperArg() {
        String gen = gen(cdm5FnOutput, CREATE_CASHFLOW);
        assertTrue(gen.contains("FieldWithMetaString currency"),
                "The [metadata scheme] string input must render FieldWithMetaString");
        assertTrue(gen.contains(".setCurrency(currency)"),
                "The wrapper arg must select the PLAIN setter, passed bare");
        assertFalse(gen.contains(".setCurrencyValue(currency)"),
                "The pre-facet Value-setter form must be gone");
    }

    // ==== F2 flip locks (revert-RED) ====

    @Test
    @EnabledIf("cdm5CellAvailable")
    void cashPriceQuantityTriangulation_cdm5_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, CASH_PRICE_QUANTITY_TRIANGULATION);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void cashPriceQuantityTriangulation_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, CASH_PRICE_QUANTITY_TRIANGULATION);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void equityCashSettlementAmount_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, EQUITY_CASH_SETTLEMENT_AMOUNT);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapOtherAgreements_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_OTHER_AGREEMENTS);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void createExercise_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, CREATE_EXERCISE);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapParty_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_PARTY);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void primeBrokerageIndicator_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, PRIME_BROKERAGE_INDICATOR);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void extractCommodityClassification_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, EXTRACT_COMMODITY_CLASSIFICATION);
    }

    /**
     * Positive-content lock (revert-RED): the alias signature types from the walked
     * chain, not the output-type fallback — golden {@code MapperS<BigDecimal>} where
     * the pre-facet leak was the invalid {@code MapperS<? extends boolean>} (the
     * function's boolean output through the ?-extends model formatting — never
     * compiled).
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void cashPriceQuantityTriangulation_cdm5_signatureTypesFromChain() {
        String gen = gen(cdm5FnOutput, CASH_PRICE_QUANTITY_TRIANGULATION);
        assertTrue(gen.contains("MapperS<BigDecimal> notional("),
                "The distinct-chain alias must type MapperS<BigDecimal>");
        assertFalse(gen.contains("MapperS<? extends boolean>"),
                "The output-type fallback leak must be gone");
    }

    // ==== F3 flip locks (revert-RED) ====

    @Test
    @EnabledIf("cdm5CellAvailable")
    void createTerminationInstruction_cdm5_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, CREATE_TERMINATION_INSTRUCTION);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void createTerminationInstruction_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, CREATE_TERMINATION_INSTRUCTION);
    }

    @Test
    @EnabledIf("cdm5CellAvailable")
    void resolveRepurchaseTransfer_cdm5_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, RESOLVE_REPURCHASE_TRANSFER);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void resolveRepurchaseTransfer_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, RESOLVE_REPURCHASE_TRANSFER);
    }

    /**
     * Positive-content lock (revert-RED): the DISTINCT then-tail hoists golden's
     * BARE-element decl (the ctor-extract level keeps the ctor type — NOT the
     * mid-chain meta wrapper) and returns {@code distinct(thenArg)}; the inline
     * runtime {@code .then(} counts 0 in every golden.
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void createTerminationInstruction_cdm5_distinctHoistBareElement() {
        String gen = gen(cdm5FnOutput, CREATE_TERMINATION_INSTRUCTION);
        assertTrue(gen.contains("final MapperC<NonNegativeQuantitySchedule> thenArg ="),
                "The thenArg decl must keep the ctor-extract level's BARE element");
        assertTrue(gen.contains("return distinct(thenArg);"),
                "The DISTINCT tail must consume the hoisted thenArg");
        assertFalse(gen.contains(".then(item -> distinct(item))"),
                "The inline runtime .then( form must be gone");
    }

    // ==== F4 flip locks (revert-RED) ====

    @Test
    @EnabledIf("cdm5CellAvailable")
    void compareQuantityByUnit_cdm5_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, COMPARE_QUANTITY_BY_UNIT);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void compareQuantityByUnit_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, COMPARE_QUANTITY_BY_UNIT);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void interestRateLeg1CrossCurrency_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, INTEREST_RATE_LEG1_CROSS_CURRENCY);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void interestRateLeg2CrossCurrency_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, INTEREST_RATE_LEG2_CROSS_CURRENCY);
    }

    /**
     * Positive-content lock (revert-RED): the closure param navigates BARE with the
     * receiver-type-derived step var (golden's exact fragment) — the self-shadowing
     * {@code MapperS.of(leg).map(…, leg -> …)} double-wrap (declared-variable
     * collision, never compiled) is gone.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void interestRateLeg1_bareParamReceiverTypedVar() {
        String gen = gen(drrFnOutput, INTEREST_RATE_LEG1_CROSS_CURRENCY);
        assertTrue(gen.contains(
                "leg.<RateSpecification>map(\"getRateSpecification\", interestRatePayout -> interestRatePayout.getRateSpecification())"),
                "The closure param must navigate bare with the type-derived step var");
        assertFalse(gen.contains("MapperS.of(leg)"),
                "The self-shadowing double-wrap must be gone");
    }

    /**
     * F4 lock CONVERTED to the whole-file flip lock at PR #362 (the deferred-carrier law —
     * the pre-#362 lock's own javadoc named the conversion: "this lock converts to a
     * whole-file flip lock when that facet lands"). The residual pair landed as the
     * fnAliasListLiteralJoinSanitize + fnBooleanParamCondition facets: the alias-signature
     * list-literal arm sanitizes the lowercase symbol-echo through the parser-side element
     * JOIN ({@code MapperC<indexIdentifierFiltered>} → {@code MapperC<String>}, abstract +
     * impl), and the bare single Boolean INPUT-param condition renders upstream's raw
     * null-guard ternary ({@code (isMin == null ? false : isMin)}) with no hoist and no
     * Mapper wrap. Byte-equality locks the whole composition (the #361 metaValueDerefHoist
     * element hoists + the F4 closure-param deref + both #362 pieces) revert-RED.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void sortIdentifiers_aliasSigJoinAndBoolParamCondition_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, SORT_IDENTIFIERS);
    }

    // ==== F5 flip locks (revert-RED) ====

    @Test
    @EnabledIf("cdm6CellAvailable")
    void createQuantityChange_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, CREATE_QUANTITY_CHANGE);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void createTermsChange_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, CREATE_TERMS_CHANGE);
    }

    // ==== F6 flip locks (revert-RED) ====

    @Test
    @EnabledIf("cdm5CellAvailable")
    void createStockSplit_cdm5_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, CREATE_STOCK_SPLIT);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void createStockSplit_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, CREATE_STOCK_SPLIT);
    }

    // ==== the #342 stays-deferred breadcrumb, CONVERTED at PR #363 ====

    /**
     * facet n2DoubleUnderscore (PR #363): the #342 stays-deferred lock's own javadoc
     * named the conversion ("a legit flip ADDS the double-underscore token") — the
     * deferred-carrier law's SIXTH consecutive PR. The as-key raw-input NAME BURN
     * (upstream declareAsVariable's synonym re-registration) burns
     * {@code _interestRatePayout} in the assignOutput body scope, so the currency
     * chain's lambda param escalates to golden's {@code __interestRatePayout} and the
     * file byte-matches.
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void interestCashSettlement_cdm5_n2DoubleUnderscoreBurn_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, INTEREST_CASH_SETTLEMENT_AMOUNT);
    }

    private static String gen(Map<String, String> output, String path) {
        assertNotNull(output, "generation did not run — corpus unavailable?");
        String g = output.get(path);
        assertNotNull(g, "Class not generated: " + path);
        return g;
    }

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String path) throws IOException {
        assertNotNull(output, "generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "Class not generated: " + path
                + " (emission failed or the path differs)");
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " (PR #342 the seven-facet compose).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
