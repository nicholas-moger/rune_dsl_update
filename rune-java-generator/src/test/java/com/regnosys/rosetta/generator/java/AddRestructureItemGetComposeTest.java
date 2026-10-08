package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

/**
 * PR #346 — the five-facet compose: 18 byte flips (9 cdm6 + 1 cdm5 + 8 drr FUNCTION).
 *
 * <p><b>F1 — {@code itemGetMetaDerefBlock}</b>: the N7 implicit-item wrapper hoist at BOTH
 * invocation paths via the sentinel LAMBDA channel. An explicit {@code item} argument (or the
 * bare-fn implicit arg) of a call that IS a drainable map/extract lambda body, whose piped
 * element is a meta wrapper and whose callee param is meta-FREE, hoists
 * {@code final <Wrapper> <name> = item.get();} into the block lambda and passes the
 * null-guarded {@code .getValue()} deref. The decl renders the DEFERRED token
 * ({@code ReferenceHandler.ItemGetMetaDerefHoist}) — an eagerly-rendered decl object would
 * {@code getActualName()}-close the whole ancestor scope chain at the mid-statement
 * {@code compileLambda} drain and poison later same-statement hoists (the #340 blocker's
 * root cause, reproduced at cp1 as the Qualify_AssetClass_Equity whole-body TODO
 * degradation). The #340 statement-direct belt became a ROUTE selector: statement-direct
 * keeps the proven JavaBlockBuilder (BarrierFromTriggerEvent byte-identical), the operand
 * class takes the lambda channel.
 *
 * <p><b>F2 — {@code addRestructure}</b>: the whole-add distribution family at the ADD seat.
 * A1: a whole-output ADD whose value is a MULTI {@code default} renders golden's isEmpty
 * if/else with the LEFT repeated ({@code renderAddDefaultDistributionOrNull}). A2+B: the
 * #327 conditional-distribution admissibility widens to MAPPER-CHAIN arms (nav / list-op /
 * extract / conversion / filter / to-string / disguised REnumValueRef nav — each rendering
 * {@code .addAll([toBuilder(]<chain>.getMulti()[)])} regardless of own cardinality, with
 * the per-arm {@code coerceAddValueMetaItem} deref) and to a REAL terminal else. The
 * corpus law is iron: zero of 34,686 goldens carry {@code addAll(…getOrDefault…)},
 * {@code addAll(ifThenElseResult)}, or an addAll ternary.
 *
 * <p><b>F3 — {@code aliasSigDeepChoiceReceiver}</b>: the #210 choice-aware deep recovery
 * mirrored into {@code inferReceiverRType} + {@code inferRTypeFromExpr}'s RDeepFeatureCall
 * arms — a mid-chain deep step through a CHOICE receiver no longer nulls the alias
 * signature walk back to the function-output fallback.
 *
 * <p><b>F4 — {@code flattenLoLThenArgElement}</b>: the #341 arm ported to the deep-then
 * hoist seat ({@code CollectionHandler.tryDeepThenHoist}) — a bare-item FLATTEN level
 * whose receiver is a MapperListOfLists carries its LoL decl's element (invariant:
 * {@code flattenList()} is {@code MapperListOfLists<T> -> MapperC<T>}).
 *
 * <p><b>F5 — {@code builtinTypeBoundItemNav}</b>: the RSegmentDef mis-binding joins the
 * #204 re-root admit set — a bare implicit-item feature whose name collides with a
 * same-named root element (drr {@code segment date}) re-roots on the item.
 *
 * <p>Whole-file byte comparisons run through the REAL D11 FUNCTION generation path and
 * revert RED without the facets (compile-split MEASURED: 1 COMPILES —
 * GetFpmlCashSettlementCurrency — + 17 NON_COMPILING per the per-carrier
 * compile-gate.json verdicts).
 */
class AddRestructureItemGetComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // F1 itemGetMetaDerefBlock flip carriers (all cdm6).
    private static final String OBSERVABLE_QUALIFICATION =
            "cdm/product/qualification/functions/ObservableQualification.java";
    private static final String QUALIFY_IR_OPTION_DEBT_OPTION =
            "cdm/product/qualification/functions/Qualify_InterestRate_Option_DebtOption.java";
    private static final String QUALIFY_ASSET_CLASS_EQUITY =
            "cdm/product/qualification/functions/Qualify_AssetClass_Equity.java";
    private static final String QUALIFY_UNDERLIER_OBSERVABLE_EQUITY =
            "cdm/product/qualification/functions/Qualify_UnderlierObservable_Equity.java";
    // F2 addRestructure flip carriers.
    private static final String FILTER_CHANGE_PRICE_QUANTITY =
            "cdm/product/common/settlement/functions/FilterChangePriceQuantity.java";
    private static final String MAP_LEGAL_AGREEMENT_LIST =
            "cdm/ingest/fpml/confirmation/legal/functions/MapLegalAgreementList.java";
    private static final String GET_FPML_CASH_SETTLEMENT_CURRENCY =
            "cdm/ingest/fpml/confirmation/settlement/functions/GetFpmlCashSettlementCurrency.java";
    private static final String ADJUSTABLE_DATES_RESOLUTION_CDM6 =
            "cdm/margin/schedule/functions/AdjustableDatesResolution.java";
    private static final String ADJUSTABLE_DATES_RESOLUTION_DRR =
            "drr/regulation/common/functions/AdjustableDatesResolution.java";
    private static final String GET_UNDERLIER_ID_FOR_BASKET_CSA =
            "drr/regulation/csa/rewrite/functions/GetUnderlierIDForBasketCSA.java";
    private static final String GET_UNDERLIER_ID_FOR_INDEX_CSA =
            "drr/regulation/csa/rewrite/functions/GetUnderlierIDForIndexCSA.java";
    private static final String CONTRACT_PRICE =
            "drr/standards/iosco/cde/base/price/functions/Contract_Price.java";
    private static final String FILTER_RESET_HISTORY_BY_LEG =
            "drr/regulation/common/functions/FilterResetHistoryByLeg.java";
    private static final String PARTY_IDENTIFIER_TYPE =
            "drr/regulation/common/functions/PartyIdentifierType.java";
    // F3 aliasSigDeepChoiceReceiver flip carrier (cdm6; the cdm5 twin is byte-GREEN and
    // locked by FunctionLambdaNamingFoundationTest — its chain has no deep step).
    private static final String CREATE_ASSET_PAYOUT_TRADE_STATE =
            "cdm/observable/event/functions/Create_AssetPayoutTradeStateWithObservations.java";
    // F4 flattenLoLThenArgElement flip carrier (drr).
    private static final String BEFORE_TRADE_FOR_EVENT =
            "drr/regulation/common/functions/BeforeTradeForEvent.java";
    // F5 builtinTypeBoundItemNav flip carrier (drr esma; asic/fca/jfsa/mas are
    // ite-hoist-co-occupied and healed their date line content-TOWARD).
    private static final String GET_OTHR_PMT_ESMA =
            "drr/projection/iso20022/esma/emir/refit/trade/functions/GetOthrPmt.java";

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
        if (cdm6CellAvailable()) {
            cdm6FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
        if (cdm5CellAvailable()) {
            cdm5FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
        }
        if (drrCellAvailable()) {
            drrFnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
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

    // ==== F1 itemGetMetaDerefBlock flip locks (revert-RED) ====

    @Test
    @EnabledIf("cdm6CellAvailable")
    void observableQualification_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, OBSERVABLE_QUALIFICATION);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyInterestRateOptionDebtOption_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, QUALIFY_IR_OPTION_DEBT_OPTION);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyAssetClassEquity_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, QUALIFY_ASSET_CLASS_EQUITY);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyUnderlierObservableEquity_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR,
                QUALIFY_UNDERLIER_OBSERVABLE_EQUITY);
    }

    /**
     * Positive + negative content lock (F1, explicit-args W1): the item wrapper hoists into
     * the block lambda and the callee takes the null-guarded deref; the flat
     * {@code evaluate(item.get(), securityType} splice is a token the flip REMOVES
     * (count 0 in the frozen golden; positive control 1 hoist decl).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void observableQualification_cdm6_itemHoistsIntoBlockLambda() {
        assertNotNull(cdm6FnOutput);
        String gen = cdm6FnOutput.get(OBSERVABLE_QUALIFICATION);
        assertNotNull(gen, "ObservableQualification not generated");
        assertTrue(gen.contains(
                "final FieldWithMetaBasketConstituent fieldWithMetaBasketConstituent = item.get();"),
                "the item wrapper must hoist into the block lambda");
        assertFalse(gen.contains("evaluate(item.get(), securityType"),
                "the pre-fix flat wrapper splice must be gone");
    }

    // ==== F2 addRestructure flip locks (revert-RED) ====

    @Test
    @EnabledIf("cdm6CellAvailable")
    void filterChangePriceQuantity_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, FILTER_CHANGE_PRICE_QUANTITY);
    }

    @Test
    @EnabledIf("cdm5CellAvailable")
    void filterChangePriceQuantity_cdm5_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, FILTER_CHANGE_PRICE_QUANTITY);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapLegalAgreementList_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_LEGAL_AGREEMENT_LIST);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void getFpmlCashSettlementCurrency_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, GET_FPML_CASH_SETTLEMENT_CURRENCY);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void adjustableDatesResolution_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, ADJUSTABLE_DATES_RESOLUTION_CDM6);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void adjustableDatesResolution_drr_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, ADJUSTABLE_DATES_RESOLUTION_DRR);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void getUnderlierIdForBasketCsa_drr_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, GET_UNDERLIER_ID_FOR_BASKET_CSA);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void getUnderlierIdForIndexCsa_drr_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, GET_UNDERLIER_ID_FOR_INDEX_CSA);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void contractPrice_drr_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, CONTRACT_PRICE);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void filterResetHistoryByLeg_drr_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, FILTER_RESET_HISTORY_BY_LEG);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void partyIdentifierType_drr_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, PARTY_IDENTIFIER_TYPE);
    }

    /**
     * Positive + negative content lock (F2-A1, the default distribution): the MULTI default
     * distributes into golden's isEmpty if/else with the LEFT repeated; the flat
     * {@code .getOrDefault(change)} arg is a token the flip REMOVES (count 0 in the frozen
     * golden — the corpus law is iron: zero goldens call getOrDefault inside addAll).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void filterChangePriceQuantity_cdm6_defaultDistributes() {
        assertNotNull(cdm6FnOutput);
        String gen = cdm6FnOutput.get(FILTER_CHANGE_PRICE_QUANTITY);
        assertNotNull(gen, "FilterChangePriceQuantity not generated");
        assertTrue(gen.contains(
                "if (changeWithMatchingObservable(priceQuantity, change).getMulti().isEmpty()) {"),
                "the MULTI default must distribute into the isEmpty if/else");
        assertFalse(gen.contains(".getOrDefault(change)"),
                "the pre-fix flat list-default must be gone");
    }

    /**
     * Positive + negative content lock (F2-B, the un-hoist with per-arm meta deref): the
     * scalar {@code ifThenElseResult} hoist consumed by addAll is a token the flip REMOVES
     * (count 0 in EVERY golden — 0 of 34,686), and the else arm derefs the FieldWithMetaDate
     * element bare-unguarded before {@code .getMulti()}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void adjustableDatesResolution_drr_armsDistributeWithMetaDeref() {
        assertNotNull(drrFnOutput);
        String gen = drrFnOutput.get(ADJUSTABLE_DATES_RESOLUTION_DRR);
        assertNotNull(gen, "AdjustableDatesResolution not generated");
        assertFalse(gen.contains("final Date ifThenElseResult"),
                "the scalar ite hoist must be gone");
        assertFalse(gen.contains("date.addAll(ifThenElseResult)"),
                "the addAll(scalar) consumer must be gone");
        assertTrue(gen.contains(
                "fieldWithMetaDate -> fieldWithMetaDate.getValue()).getMulti());"),
                "the else arm must deref the wrapper element-wise before getMulti");
    }

    /**
     * Positive content lock (F2-B, the SINGLE mapper-chain arm): golden consumes a SINGLE
     * to-string arm via {@code .addAll(<chain>.getMulti())} — the cardinality-agnostic
     * mapper-chain arm render.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void partyIdentifierType_drr_singleToStringArmTakesGetMulti() {
        assertNotNull(drrFnOutput);
        String gen = drrFnOutput.get(PARTY_IDENTIFIER_TYPE);
        assertNotNull(gen, "PartyIdentifierType not generated");
        assertTrue(gen.contains(
                "partyIdentifierFormat.addAll(MapperS.of(PersonIdentifierTypeEnum.NPID)"
                + ".map(\"to-string\", PersonIdentifierTypeEnum::toDisplayString).getMulti());"),
                "a SINGLE to-string arm must render addAll(<chain>.getMulti())");
    }

    // ==== F3 aliasSigDeepChoiceReceiver flip lock (revert-RED) ====

    @Test
    @EnabledIf("cdm6CellAvailable")
    void createAssetPayoutTradeStateWithObservations_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, CREATE_ASSET_PAYOUT_TRADE_STATE);
    }

    /**
     * Positive + negative content lock (F3): the alias signature types the deep-chain LEAF;
     * the function-output fallback signature is a token the flip REMOVES (count 0 in the
     * frozen golden; positive control 2 — abstract + impl).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void createAssetPayoutTradeState_cdm6_aliasSignatureTypesLeaf() {
        assertNotNull(cdm6FnOutput);
        String gen = cdm6FnOutput.get(CREATE_ASSET_PAYOUT_TRADE_STATE);
        assertNotNull(gen, "Create_AssetPayoutTradeStateWithObservations not generated");
        assertTrue(gen.contains("MapperS<? extends AssetPayout> assetPayout("),
                "the alias signature must type the deep-chain leaf");
        assertFalse(gen.contains("MapperS<? extends TradeState> assetPayout("),
                "the function-output fallback signature must be gone");
    }

    // ==== F4 flattenLoLThenArgElement flip lock (revert-RED) ====

    @Test
    @EnabledIf("drrCellAvailable")
    void beforeTradeForEvent_drr_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, BEFORE_TRADE_FOR_EVENT);
    }

    /**
     * Positive + negative content lock (F4): the flatten decl carries the LoL element
     * (the wrapper); the meta-blind {@code MapperC<String>} decl is a token the flip
     * REMOVES (count 0 in the frozen golden; positive control 1).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void beforeTradeForEvent_drr_flattenDeclKeepsWrapperElement() {
        assertNotNull(drrFnOutput);
        String gen = drrFnOutput.get(BEFORE_TRADE_FOR_EVENT);
        assertNotNull(gen, "BeforeTradeForEvent not generated");
        assertTrue(gen.contains("final MapperC<FieldWithMetaString> thenArg2"),
                "the flatten decl must carry the LoL wrapper element");
        assertFalse(gen.contains("final MapperC<String> thenArg2"),
                "the meta-blind value-typed decl must be gone");
    }

    // ==== F5 builtinTypeBoundItemNav flip lock (revert-RED) ====

    @Test
    @EnabledIf("drrCellAvailable")
    void getOthrPmt_esma_drr_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, GET_OTHR_PMT_ESMA);
    }

    /**
     * Positive + negative content lock (F5): the builtin-colliding bare {@code date}
     * re-roots on the implicit item; the bare {@code .setPmtDt(date)} splice is a token
     * the flip REMOVES (count 0 in the frozen golden; positive control 1).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getOthrPmt_esma_drr_bareDateReRoots() {
        assertNotNull(drrFnOutput);
        String gen = drrFnOutput.get(GET_OTHR_PMT_ESMA);
        assertNotNull(gen, "GetOthrPmt (esma) not generated");
        assertTrue(gen.contains(
                ".setPmtDt(item.<Date>map(\"getDate\", otherPayment -> otherPayment.getDate()).get())"),
                "the bare date must re-root on the implicit item");
        assertFalse(gen.contains(".setPmtDt(date)"),
                "the pre-fix bare-symbol splice must be gone");
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
                + path + " (PR #346 the five-facet compose).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
