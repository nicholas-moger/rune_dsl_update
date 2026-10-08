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
 * PR #345 anchor — the four-facet compose (12 FUNCTION flips: 9 cdm6 + 1 cdm5 + 2 drr).
 *
 * <p><b>F1 — {@code onlyElementMapperSRoundTrip}</b> (+5: GetEventDate,
 * MapGenericProductEconomicTerms, Qualify_Repurchase cdm6+cdm5, PartyLei drr): a THEN-CHAIN
 * tail {@code only-element} collapse over a PROVEN-MapperC pipe re-presents as golden's
 * identity round-trip {@code MapperS.of(<mc>.get())} before the seat's own deref — at the
 * evaluate-arg seat ({@code evaluate(MapperS.of(thenArg.get()).get())}), the then-output
 * decl ({@code = MapperS.of(distinct(…).get()).get();}), the ctor-setter arg (killing the
 * non-compiling {@code .get().get()}) and the extract-RECEIVER seat
 * ({@code MapperS.of(<mc>.get())\n\t.mapSingleToItem(…)}), plus the twins'
 * {@code ComparisonResult.ofNullSafe} extract-operand co-wrap and the ctor bare-fn verbatim
 * splice. The THEN-TAIL gate is load-bearing: a DIRECT nav-chain collapse stays bare in
 * golden (QuantityIncreased + 9 green siblings — the cp1 over-fire catch), and the monotone
 * {@code chainProvesMulti} under-mirror keeps the 859 green MapperS-pipe
 * {@code evaluate(thenArg.get())} forms declined.
 *
 * <p><b>F2 — {@code disguisedAliasRootCardinality}</b>: {@code chainProvesMulti}'s
 * REnumValueRef arm recurses into the disguised ALIAS root on a resolved-single leaf —
 * Qualify_InterestRate_Option_DebtOption's extract now renders golden's {@code mapItem}
 * (its then-remaining N7 residual landed at PR #346's itemGetMetaDerefBlock — the file
 * is byte-identical since).
 *
 * <p><b>F3 — five seat singles</b> (+5): {@code listLiteralAliasMetaWitness}
 * (MapLegalEntity — {@code MapperC.<FieldWithMetaString>of(<alias>(…), …)}),
 * {@code multiDefaultTernary} (MapCreditIndex — the alias-body MULTI default renders
 * {@code <A>.getMulti().isEmpty() ? <B> : <A>}), {@code sortKeyDefaultArgCollapse}
 * (MapPrincipalPaymentSchedule — the re-rooted default RIGHT collapses {@code .get()} and
 * the #234 wrap restores the sort-key Mapper form), {@code functionThenTerminalMetaMulti}
 * (FilterInvalidFloatingRateIndexTradeDate — the multi-output function then-terminal derefs
 * the wrapper element-wise and collapses {@code .getMulti()}), {@code nullResultArmElision}
 * (FinancialUnitToISO20022UnitOfMeasure — a TRAILING run of {@code then empty} rungs folds
 * into the final {@code } else { result = null; }}).
 *
 * <p><b>F4 — {@code witnessDupImportCrossBody}</b> (+2: MapEquityOptionPayout,
 * MapCommodityAmericanExerciseTerms): {@code ImportCollisionResolver.resolve} carries ONE
 * claim map across the ordered bodies (file order: operations → aliases → conditions), and
 * the thenArg decl seats claim their element via the resolver sentinel — a collision SPLIT
 * across bodies (the cdm claim in {@code assignOutput} vs the fpml witness in a later alias
 * body) now renders the later different-canonical witness FQN-inline + suppresses its
 * duplicate import, exactly the #227 first-claim law at whole-file scope.
 *
 * <p>Whole-file byte comparisons run through the REAL D11 FUNCTION generation path and
 * revert RED without the facets (compile-split MEASURED: 3 COMPILES — GetEventDate,
 * PartyLei, FinancialUnitToISO20022UnitOfMeasure — + 9 NON_COMPILING per the per-carrier
 * compile-gate.json verdicts).
 */
class OnlyElementRoundTripComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // F1 onlyElementMapperSRoundTrip flip carriers.
    private static final String GET_EVENT_DATE =
            "cdm/ingest/fpml/confirmation/workflowstep/functions/GetEventDate.java";
    private static final String MAP_GENERIC_PRODUCT_ECONOMIC_TERMS =
            "cdm/ingest/fpml/confirmation/product/genericproduct/functions/MapGenericProductEconomicTerms.java";
    private static final String QUALIFY_REPURCHASE =
            "cdm/event/common/functions/Qualify_Repurchase.java";
    private static final String PARTY_LEI =
            "drr/regulation/common/functions/PartyLei.java";
    // F3 seat-single flip carriers.
    private static final String MAP_LEGAL_ENTITY =
            "cdm/ingest/fpml/confirmation/party/functions/MapLegalEntity.java";
    private static final String MAP_CREDIT_INDEX =
            "cdm/ingest/fpml/confirmation/product/creditdefaultswap/functions/MapCreditIndex.java";
    private static final String MAP_PRINCIPAL_PAYMENT_SCHEDULE =
            "cdm/ingest/fpml/confirmation/payment/functions/MapPrincipalPaymentSchedule.java";
    private static final String FILTER_INVALID_FRO_TRADE_DATE =
            "cdm/observable/asset/fro/functions/FilterInvalidFloatingRateIndexTradeDate.java";
    private static final String FINANCIAL_UNIT_TO_ISO20022 =
            "drr/regulation/common/functions/FinancialUnitToISO20022UnitOfMeasure.java";
    // F4 witnessDupImportCrossBody flip carriers.
    private static final String MAP_EQUITY_OPTION_PAYOUT =
            "cdm/ingest/fpml/confirmation/product/equityoption/functions/MapEquityOptionPayout.java";
    private static final String MAP_COMMODITY_AMERICAN_EXERCISE_TERMS =
            "cdm/ingest/fpml/confirmation/common/functions/MapCommodityAmericanExerciseTerms.java";

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

    // ==== F1 onlyElementMapperSRoundTrip flip locks (revert-RED) ====

    @Test
    @EnabledIf("cdm6CellAvailable")
    void getEventDate_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, GET_EVENT_DATE);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapGenericProductEconomicTerms_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_GENERIC_PRODUCT_ECONOMIC_TERMS);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyRepurchase_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, QUALIFY_REPURCHASE);
    }

    @Test
    @EnabledIf("cdm5CellAvailable")
    void qualifyRepurchase_cdm5_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, QUALIFY_REPURCHASE);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void partyLei_drr_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, PARTY_LEI);
    }

    /**
     * Positive + negative content lock (F1, evaluate-arg seat): the then-tail collapse
     * re-presents as the identity round-trip; the bare splice {@code evaluate(thenArg.get())}
     * is a token the flip REMOVES (count 0 in golden — the #344-verified corpus law's 1
     * wrapped-on-MapperC carrier IS this file).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void getEventDate_cdm6_evaluateArgIsRoundTripped() {
        assertNotNull(cdm6FnOutput);
        String gen = cdm6FnOutput.get(GET_EVENT_DATE);
        assertNotNull(gen, "GetEventDate not generated");
        assertTrue(gen.contains("evaluate(MapperS.of(thenArg.get()).get())"),
                "the then-tail only-element collapse must re-present as MapperS.of(<mc>.get())");
        assertFalse(gen.contains("evaluate(thenArg.get())"),
                "the pre-fix bare collapse splice must be gone");
    }

    /**
     * Negative content lock (F1, ctor-arg seat): the doubled deref
     * {@code .get().get()} is a token the flip REMOVES (zero goldens carry a doubled
     * {@code .get()} anywhere in the corpus).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapGenericProductEconomicTerms_cdm6_noDoubledGet() {
        assertNotNull(cdm6FnOutput);
        String gen = cdm6FnOutput.get(MAP_GENERIC_PRODUCT_ECONOMIC_TERMS);
        assertNotNull(gen, "MapGenericProductEconomicTerms not generated");
        assertTrue(gen.contains(".setNonStandardisedTerms(MapperS.of(distinct(thenArg1).get()).get())"),
                "the ctor-arg collapse must render the identity round-trip");
        assertFalse(gen.contains(".get().get()"),
                "the pre-fix doubled deref must be gone");
    }

    /**
     * Positive content lock (F3, multiDefaultTernary): the alias-body MULTI default renders
     * the list-form ternary with the LEFT repeated; the Mapper-arg
     * {@code getOrDefault(} over a MapperC operand is the flip-removed shape.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapCreditIndex_cdm6_multiDefaultRendersTernary() {
        assertNotNull(cdm6FnOutput);
        String gen = cdm6FnOutput.get(MAP_CREDIT_INDEX);
        assertNotNull(gen, "MapCreditIndex not generated");
        assertTrue(gen.contains(".getMulti().isEmpty() ? "),
                "the alias-body MULTI default must render the isEmpty ternary");
    }

    /**
     * Negative content lock (F4, witnessDupImportCrossBody): the duplicate fpml import is a
     * token the flip REMOVES (golden carries the cdm import only; the fpml witness renders
     * FQN-inline ×3).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapCommodityAmericanExerciseTerms_cdm6_noDuplicateImport() {
        assertNotNull(cdm6FnOutput);
        String gen = cdm6FnOutput.get(MAP_COMMODITY_AMERICAN_EXERCISE_TERMS);
        assertNotNull(gen, "MapCommodityAmericanExerciseTerms not generated");
        assertFalse(gen.contains("import fpml.consolidated.shared.AdjustableOrRelativeDate;"),
                "the duplicate same-simple-name fpml import must be suppressed");
        assertTrue(gen.contains("<fpml.consolidated.shared.AdjustableOrRelativeDate>map"),
                "the losing fpml witness must render FQN-inline");
    }

    // ==== F3 seat-single flip locks (revert-RED) ====

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapLegalEntity_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_LEGAL_ENTITY);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapCreditIndex_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_CREDIT_INDEX);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapPrincipalPaymentSchedule_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_PRINCIPAL_PAYMENT_SCHEDULE);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void filterInvalidFloatingRateIndexTradeDate_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, FILTER_INVALID_FRO_TRADE_DATE);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void financialUnitToIso20022UnitOfMeasure_drr_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, FINANCIAL_UNIT_TO_ISO20022);
    }

    // ==== F4 witnessDupImportCrossBody flip locks (revert-RED) ====

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapEquityOptionPayout_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_EQUITY_OPTION_PAYOUT);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapCommodityAmericanExerciseTerms_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_COMMODITY_AMERICAN_EXERCISE_TERMS);
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
                + path + " (PR #345 the four-facet compose).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
