package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;

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
 * PR #329 anchors — the lambda-naming foundation: facets {@code methodWideNaming}
 * (M1) + {@code aliasOutputSeed} (M2) + {@code deferredLambdaParams} (M3/M4) +
 * {@code filterReceiverNaming} (M5).
 *
 * <ul>
 *   <li><b>M1 (method-wide numbering):</b> registered identifiers (guarded
 *       Type-coercion params, #237 hoist locals) group and number {@code 0..n-1}
 *       over the WHOLE assignOutput method — upstream keeps ONE method body scope,
 *       so statement 3's guarded param numbers {@code referenceWithMetaPayout3}
 *       after statement 1's {@code 0..2} (golden CalculateTransfer) and a #237
 *       hoist local groups with a LATER statement's param
 *       ({@code referenceWithMetaNonNegativeQuantitySchedule0/1}, golden
 *       GetNotionalAmount). Implemented WITHOUT sharing scopes (the #170
 *       discarded-attempt isolation depends on fresh per-statement roots):
 *       {@code FunctionGenerator.compileOperations} brackets the loop with
 *       {@code beginMethodNamingGroup}, and
 *       {@code JavaStatementScope.resolveUnifiedDeferredNames} REPLAYS the kept
 *       registrations (filtered by sentinel-presence in kept text, ordered by the
 *       global sentinel counter) into one unified virtual method scope.</li>
 *   <li><b>M2 (alias output seed):</b> an ALIAS method's scope carries only its
 *       own params — the output name is seeded ONLY when the alias reads it, so a
 *       nav lambda param named like the output stays PLAIN in an alias method
 *       (golden Create_TermsChange {@code tradeState -> tradeState.getTrade()};
 *       the context-blind seed minted the spurious {@code _tradeState}).</li>
 *   <li><b>M3/M4 (deferred lambda params):</b> nav-step and bare-deref lambda
 *       params register as lambda-child identifiers with deferred sentinels, so
 *       the ESCAPE resolves at finalization with the complete method picture —
 *       a #198 ctor-hoist local declared AROUND the lambda forces golden's
 *       {@code _dateTimeList}/{@code _cashSettlementTerms}, and the #290
 *       whole-output deref hoist declared statements LATER forces golden
 *       PriorUSIRule's {@code _fieldWithMetaString} (the session's resolved hoist
 *       names seed the unified scope).</li>
 *   <li><b>M5 (filter receiver naming):</b> a step navigating FROM a FILTER names
 *       its lambda var from the filtered ELEMENT type (golden
 *       {@code varianceLeg -> varianceLeg.getTerminationDate()}), not the
 *       navigated-feature underscore fallback ({@code _terminationDate}).</li>
 * </ul>
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of generated output against the frozen
 * 9.83.0 goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 *
 * <p><b>Deliberate coverage gap (the #325 S1 convention):</b> the cdm5 twins of the
 * cdm6-locked M1 carriers (CalculateTransfer, GetNotionalAmount,
 * GetRateScheduleStepValues) have no second lock — byte-identical rosetta, verified
 * by the byte-oracle + probe dumps; the cdm5 cell loads here for the two M2 shapes
 * it alone carries clean.
 */
class FunctionLambdaNamingFoundationTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // M2 flip carrier (cdm5-clean): three alias methods whose nav param desires the
    // OUTPUT name `tradeState` — plain in an alias method (the output is not a param
    // there), `_tradeState` pre-#329.
    private static final String CREATE_TERMS_CHANGE =
            "cdm/event/common/functions/Create_TermsChange.java";
    // M2 flip carrier (cdm5-clean): the single-alias variant of the same shape.
    private static final String CREATE_ASSET_PAYOUT_TRADE_STATE =
            "cdm/observable/event/functions/Create_AssetPayoutTradeStateWithObservations.java";
    // M1 flip carrier (cdm6; cdm5 twin unlocked): guarded params in statements 2/3
    // continue the method group opened by statement 1 (`referenceWithMetaPayout3/4`
    // after `0..2`).
    private static final String CALCULATE_TRANSFER =
            "cdm/event/common/functions/CalculateTransfer.java";
    // M1 flip carrier (cdm6; cdm5 twin unlocked): the #237-documented CROSS-SEAT
    // group — a hoist local (statement A) numbers `0` and a guarded param
    // (statement B) numbers `1` in ONE method-wide group.
    private static final String GET_NOTIONAL_AMOUNT =
            "cdm/product/asset/calculation/functions/GetNotionalAmount.java";
    // M1 flip carrier (cdm6; cdm5 twin unlocked): two single-member statements'
    // guarded params number 0/1 method-wide.
    private static final String GET_RATE_SCHEDULE_STEP_VALUES =
            "cdm/product/asset/floatingrate/functions/GetRateScheduleStepValues.java";
    // M3 flip carrier (cdm6): the nav param inside a #198/#212 hoisted ctor value
    // escapes against the hoist local declared AROUND it (`_dateTimeList`).
    private static final String MAP_FPML_DATE_TIME_LIST =
            "cdm/ingest/fpml/confirmation/datetime/functions/MapFpmlDateTimeListToDateTimeList.java";
    // M3 flip carrier (cdm6): same law at the ctor-pair singletonList hoist — the
    // pre-#329 bare `cashSettlementTerms` lambda param SHADOWED the local it
    // initializes (never compiled).
    private static final String MAP_CREDIT_DEFAULT_SWAP_CHOICE =
            "cdm/ingest/fpml/confirmation/settlement/functions/MapCreditDefaultSwapChoiceToSettlementTerms.java";
    // M5 flip carrier (cdm6): the post-filter nav param names from the filtered
    // element type (`varianceLeg`), not `_terminationDate`.
    private static final String MAP_VARIANCE_SWAP_ECONOMIC_TERMS =
            "cdm/ingest/fpml/confirmation/product/varianceswap/functions/MapVarianceSwapEconomicTerms.java";
    // Foundation bonus flip carrier (drr FUNCTION): the deferred-param finalization
    // healed its last naming divergence.
    private static final String GET_UNDERLYING_IDENTIFICATION_TYPE =
            "drr/regulation/common/functions/GetUnderlyingIdentificationType.java";
    // M4 flip carrier (drr POJO): the bare MapperC deref params escape
    // (`_fieldWithMetaString`) against the #290 whole-output deref hoist local
    // declared STATEMENTS LATER — the backward dependency only the deferred
    // finalization can see.
    private static final String PRIOR_USI_RULE =
            "drr/regulation/common/trade/link/reports/PriorUSIRule.java";
    // Green-safety pin (drr POJO, GREEN): the #310 item-rooted GUARDED deref carrier
    // — the deferred-param + unified-naming machinery must keep its bytes.
    private static final String PORTFOLIO_CONTAINING_GREEN =
            "drr/regulation/asic/rewrite/margin/reports/PortfolioContainingNonReportedComponentIndicatorRule.java";
    // Green-safety pin (cdm6, GREEN since #328): the F2a hoist local `_tradeState`
    // (an assignOutput seat — the output SEED stays there, M2 removes it from ALIAS
    // methods only) + in-method lambda naming must keep their bytes.
    private static final String CREATE_BILLING_RECORD_GREEN =
            "cdm/event/common/functions/Create_BillingRecord.java";

    private static Map<String, String> cdm5Output;
    private static Map<String, String> cdm6Output;
    private static Map<String, String> drrOutput;

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cdm5CellAvailable()) {
            cdm5Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
        }
        if (cdm6CellAvailable()) {
            cdm6Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
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
        var enumGen = new EnumGenerator(gm);
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
                assertNoGenerationErrors(enumGen.generateClasses(model, version, output));
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

    // ==== Flip locks (revert-RED): the carriers now byte-match golden. ====

    @Test
    @EnabledIf("cdm5CellAvailable")
    void createTermsChange_aliasOutputSeedRemoved_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5Output, CDM5_GOLDEN_DIR, CREATE_TERMS_CHANGE);
    }

    @Test
    @EnabledIf("cdm5CellAvailable")
    void createAssetPayoutTradeState_aliasOutputSeedRemoved_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm5Output, CDM5_GOLDEN_DIR, CREATE_ASSET_PAYOUT_TRADE_STATE);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void calculateTransfer_methodWideGuardedParamNumbering_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, CALCULATE_TRANSFER);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void getNotionalAmount_crossSeatHoistParamGroup_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, GET_NOTIONAL_AMOUNT);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void getRateScheduleStepValues_crossStatementNumbering_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, GET_RATE_SCHEDULE_STEP_VALUES);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapFpmlDateTimeList_navParamEscapesCtorHoistLocal_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, MAP_FPML_DATE_TIME_LIST);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapCreditDefaultSwapChoice_navParamEscapesPairHoistLocal_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, MAP_CREDIT_DEFAULT_SWAP_CHOICE);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapVarianceSwapEconomicTerms_filterReceiverNaming_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, MAP_VARIANCE_SWAP_ECONOMIC_TERMS);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void getUnderlyingIdentificationType_deferredNaming_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, GET_UNDERLYING_IDENTIFICATION_TYPE);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void priorUsiRule_bareDerefEscapesLaterHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, PRIOR_USI_RULE);
    }

    // ==== Positive-content locks (revert-RED). ====

    @Test
    @EnabledIf("cdm6CellAvailable")
    void getRateScheduleStepValues_numbersGuardedParamsMethodWide() {
        String gen = gen(cdm6Output, GET_RATE_SCHEDULE_STEP_VALUES);
        assertTrue(gen.contains("referenceWithMetaPriceSchedule0 -> referenceWithMetaPriceSchedule0"),
                "statement 1's guarded param must number 0 in the method-wide group");
        assertTrue(gen.contains("referenceWithMetaPriceSchedule1 -> referenceWithMetaPriceSchedule1"),
                "statement 2's guarded param must number 1 in the SAME method-wide group");
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapCreditDefaultSwapChoice_hoistLocalWinsPlainName_lambdaEscapes() {
        String gen = gen(cdm6Output, MAP_CREDIT_DEFAULT_SWAP_CHOICE);
        assertTrue(gen.contains(
                "final cdm.product.common.settlement.CashSettlementTerms cashSettlementTerms ="),
                "the ctor-pair hoist local must keep the PLAIN name (a method-level identifier)");
        assertTrue(gen.contains("_cashSettlementTerms -> _cashSettlementTerms.getFixedRecoveryModel()"),
                "the nav param inside the hoisted value must escape against the hoist local");
        assertFalse(gen.contains("cashSettlementTerms -> cashSettlementTerms.getFixedRecoveryModel()"),
                "the pre-#329 bare param shadowed the local it initializes (never compiled)");
    }

    @Test
    @EnabledIf("cdm5CellAvailable")
    void createTermsChange_outputSeedIsSeatKeyed() {
        String gen = gen(cdm5Output, CREATE_TERMS_CHANGE);
        // The SAME desired name resolves differently per SEAT — golden's two-sided law:
        // an ALIAS method's nav param named like the OUTPUT stays PLAIN (the output is
        // not a param of a non-usesOutput alias method)…
        assertTrue(gen.contains("\"getTrade\", tradeState -> tradeState.getTrade()"),
                "an alias method's nav param named like the OUTPUT must stay plain");
        // …while the assignOutput seat KEEPS the escape (the output local `tradeState`
        // IS in scope there — the M2 gate must not remove the seed from that seat).
        assertTrue(gen.contains("\"getTrade\", _tradeState -> _tradeState.getTrade()"),
                "the assignOutput seat's nav param must still escape against the output");
    }

    // ==== Green-safety pins. ====

    @Test
    @EnabledIf("drrCellAvailable")
    void portfolioContaining_greenItemRootedGuardedDeref_staysByteIdentical()
            throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, PORTFOLIO_CONTAINING_GREEN);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void createBillingRecord_greenAssignOutputEscapes_stayByteIdentical() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, CREATE_BILLING_RECORD_GREEN);
    }

    private static String gen(Map<String, String> output, String path) {
        assertNotNull(output, "generation did not run for the cell of " + path);
        String gen = output.get(path);
        assertNotNull(gen, "missing generated file: " + path);
        return gen;
    }

    private void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String path) throws IOException {
        String gen = gen(output, path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "missing golden: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(gen),
                path + " must byte-match the frozen 9.83.0 golden");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
