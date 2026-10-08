package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
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
 * PR #349 — the five-facet compose: 16 byte flips (5 cdm5 FUNCTION + 8 cdm6 FUNCTION +
 * 2 drr FUNCTION + 1 drr POJO Rule). The F1 lock (the #348 deferred cdm6 twin,
 * ResolveInterestRateObservationIdentifiers) lives in
 * {@link MetaShortFormRefRewrapComposeTest} — its #348 decline lock CONVERTED to the
 * flip lock per its own javadoc promise; this class locks the other 15.
 *
 * <p><b>F2 — {@code derefHoistSeats}</b>: upstream {@code convertNullSafe} at
 * non-Mapper consumption seats. S4: the conditional-arm
 * {@code renderMetaValueDerefOrNull} call sites thread the arm AST so the #249
 * erased-type recovery fires through the only-element #290 erasure
 * (ResolvePerformancePeriodStartPrice cdm6). S5: a DIRECT fn-call arithmetic operand
 * with a single Integer output at a BigDecimal seat hoists {@code final Integer
 * integer = <call>;} + the ternary-ofNull guard (DateDifferenceYears cdm6). S2: a
 * MULTI callee param whose element is the arg item-wrapper's VALUE (equal or MODEL
 * subtype — the #323 flat-class law walked by name) derefs ELEMENTWISE with the BARE
 * MapperC lambda, witness = the PARAM element (FxMarkToMarket ×2). S1+S3
 * {@code aliasBodySinkHoists}: the alias-body compile runs inside a statement-hoist
 * sink (the renderAliasThenHoistOrNull template) so the #237 evaluate-arg meta-deref
 * route and the #335 collapsed-meta rewrap fire inside alias methods; item-typed tops
 * and conditional-carrying bodies DECLINE (the cp2
 * MapCommodityOptionToStrikePriceDatedValues AWAY catch).
 *
 * <p><b>F3 — {@code zonedDateTimeRecordCtor}</b>: the whole-output SET
 * record-constructor arm ports upstream {@code RecordJavaUtil.recordConstructor}'s
 * RZonedDateTimeType dispatch + ifAllNotNull — feature-named hoist locals (an
 * identifier value hoists NOTHING, the declareAsVariable synonym law), the
 * all-features null-guard if/else, {@code ZonedDateTime.of(date.toLocalDate(), time,
 * ZoneId.of(timezone))}. ToDateTime ×2 — the sole generated carrier corpus-wide.
 *
 * <p><b>F4 — {@code aliasLadderJoin}</b>: the alias-signature walk joins
 * conditional/default/arithmetic arms per upstream {@code joinMetaAnnotatedTypes}
 * (null-arm substitution kills the function-output fallback leak; cardinality OR;
 * numeric widen; wrapper-vs-value meta INTERSECT; the nearest-common-Rosetta-ancestor
 * LUB, canonical-disambiguated across models — the cdm-vs-fpml {@code Bond} namespace
 * trap; junk unresolved-symbol arm names sanitize to untypable — the cp4b catch;
 * DIVISION always BigDecimal). The ladder render gains the signature-typed EMPTY
 * then-arm ({@code MapperC.<Valuation>ofNull()}) and the wrapper-rung deref under a
 * value-joined signature. Create_Valuation ×2, ResolveSecurityFinanceBillingAmount
 * ×2, MapReferenceObligation cdm6 (Bond/ConvertibleBond/Loan/Mortgage →
 * UnderlyingAsset), Create_Exercise cdm5 (the GREEN cdm6 twin's
 * MetaInputAliasSigComposeTest byte-lock held — its arms are already value-typed).
 *
 * <p><b>F5 — {@code blockLadder}</b>: F2a — {@code isCleanLadderContext}'s #281
 * RFunction exclusion narrows to a refined admit (ONE then-step on a PURE-NAV base;
 * conditional/invocation/then-in-base and then-of-then DECLINE — four cp iterations
 * pinned the decline set; the 13-file naive-admit AWAY class stays deferred to the
 * runtime-then PR): Extract_HKMASchemeName (5-rung) + Extract_HKMATransactionSchemeName.
 * F2b — the MAPPER_C_EXPECTING elseless sibling (rule-scoped + chainProvesMulti; the
 * eligibility twin drops the resolution-blind CARDINALITY re-check — the P349D probe;
 * {@code MapperC.<T>ofNull()} terminal): ClearingReceiptTimestampRule drr POJO
 * (+ ConfirmationTimestampRule healed 9→2 TOWARD).
 *
 * <p>Whole-file byte comparisons run through the REAL D11 generation paths and revert
 * RED without the facets. Compile-split (the FRESH 2026-07-06 gate, per-carrier): 14
 * NON_COMPILING + 2 COMPILES_DIVERGENT (ResolvePerformancePeriodStartPrice +
 * ClearingReceiptTimestampRule — byte-only flips, CD 32 → 30).
 */
class DerefHoistLadderJoinComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // F2 flip carriers.
    private static final String FX_MARK_TO_MARKET =
            "cdm/event/position/functions/FxMarkToMarket.java";
    private static final String DATE_DIFFERENCE_YEARS =
            "cdm/margin/schedule/functions/DateDifferenceYears.java";
    private static final String RESOLVE_PERFORMANCE_PERIOD_START_PRICE =
            "cdm/product/asset/functions/ResolvePerformancePeriodStartPrice.java";
    // F2 TOWARD-heal carrier (still divergent — the pre-existing stale
    // ComparisonResult import, the #336-documented residual, dl=1).
    private static final String CREATE_CASHFLOW_FROM_SETTLEMENT_PAYOUT =
            "cdm/product/template/functions/Create_CashflowFromSettlementPayout.java";
    // F3 flip carrier (both cdm cells).
    private static final String TO_DATE_TIME =
            "cdm/base/datetime/functions/ToDateTime.java";
    // F4 flip carriers.
    private static final String CREATE_VALUATION =
            "cdm/event/common/functions/Create_Valuation.java";
    private static final String RESOLVE_SECURITY_FINANCE_BILLING_AMOUNT =
            "cdm/event/common/functions/ResolveSecurityFinanceBillingAmount.java";
    private static final String MAP_REFERENCE_OBLIGATION =
            "cdm/ingest/fpml/confirmation/product/creditdefaultswap/functions/MapReferenceObligation.java";
    private static final String CREATE_EXERCISE =
            "cdm/event/common/functions/Create_Exercise.java";
    // F5 flip carriers (drr).
    private static final String EXTRACT_HKMA_SCHEME_NAME =
            "drr/regulation/hkma/rewrite/trade/functions/Extract_HKMASchemeName.java";
    private static final String EXTRACT_HKMA_TRANSACTION_SCHEME_NAME =
            "drr/regulation/hkma/rewrite/valuation/functions/Extract_HKMATransactionSchemeName.java";
    private static final String CLEARING_RECEIPT_TIMESTAMP_RULE =
            "drr/regulation/common/trade/datetime/reports/ClearingReceiptTimestampRule.java";
    // F5 DEFERRED carriers (decline locks — the runtime-then PR converts them).
    private static final String GET_NEXT_FLOATING_REFERENCE_RESET_DATE =
            "drr/regulation/common/functions/GetNextFloatingReferenceResetDate.java";
    private static final String EXTRACT_REFERENCE_ENTITY_FORMAT =
            "drr/regulation/hkma/rewrite/trade/functions/Extract_ReferenceEntityFormat.java";

    private static Map<String, String> cdm6FnOutput;
    private static Map<String, String> cdm5FnOutput;
    private static Map<String, String> drrOutput;

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
            drrOutput = generateFunctionsAndRules(
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

    /**
     * FUNCTION + Rule generation for the drr cell (the F5 carriers span both kinds —
     * the ClearingReceiptTimestampRule POJO lock needs the RuleGenerator path, the
     * {@link RuleLadderConditionalBlockTest} harness).
     */
    private static Map<String, String> generateFunctionsAndRules(
            D11CorpusRegressionTest.CellSpec cell) throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                assertNoGenerationErrors(ruleGen.generateClasses(model, version, output));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
            }
        }
        funcGen.generate(output);
        return output;
    }

    // ---------------------------------------------------------------- F2 locks

    @Test
    @EnabledIf("cdm6CellAvailable")
    void f2_fxMarkToMarket_cdm6_argDerefs_byteLock() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, FX_MARK_TO_MARKET);
        String gen = gen(cdm6FnOutput, FX_MARK_TO_MARKET);
        // S1 (hoist + guarded deref into the meta-free String param) — the hoisted
        // wrapper local appears twice (quoted/base), the raw `.get())` arg never.
        assertEquals(2, count(gen, "final FieldWithMetaString fieldWithMetaString = "),
                "Both currency args hoist the wrapper local (S1)");
        // Negative witness (count 0 in golden): the un-coerced elementwise arg.
        assertFalse(gen.contains(".getMulti(), quotedCurrency(trade).get())"),
                "The raw wrapper arg form must not survive (S1+S2)");
        // S2 (elementwise BARE MapperC deref, witness = the PARAM element).
        assertEquals(2, count(gen, ".<QuantitySchedule>map(\"Type coercion\", "
                + "fieldWithMetaNonNegativeQuantitySchedule -> "
                + "fieldWithMetaNonNegativeQuantitySchedule.getValue()).getMulti()"),
                "Both quantities args deref elementwise with the param-element witness (S2)");
    }

    @Test
    @EnabledIf("cdm5CellAvailable")
    void f2_fxMarkToMarket_cdm5_twin_byteLock() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, FX_MARK_TO_MARKET);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void f2_dateDifferenceYears_operandNumericHoist_byteLock() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, DATE_DIFFERENCE_YEARS);
        String gen = gen(cdm6FnOutput, DATE_DIFFERENCE_YEARS);
        assertTrue(gen.contains("final Integer integer = dateDifference.evaluate(firstDate, secondDate);"),
                "The fn-call operand hoists the Integer local (S5)");
        // Negative witness (count 0 in golden): the raw MapperS.of(<call>) operand.
        assertFalse(gen.contains("divide(MapperS.of(dateDifference.evaluate("),
                "The un-coerced invocation operand must not survive (S5)");
        assertTrue(gen.contains(
                "(integer == null ? MapperS.<BigDecimal>ofNull() : MapperS.of(BigDecimal.valueOf(integer)))"),
                "The ternary-ofNull guard renders (upstream item->wrapper convertNullSafe)");
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void f2_resolvePerformancePeriodStartPrice_condArmDeref_byteLock() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR,
                RESOLVE_PERFORMANCE_PERIOD_START_PRICE);
        String gen = gen(cdm6FnOutput, RESOLVE_PERFORMANCE_PERIOD_START_PRICE);
        assertTrue(gen.contains("final ReferenceWithMetaPriceSchedule referenceWithMetaPriceSchedule = "),
                "The conditional then-arm hoists the wrapper local (S4 — the #249 "
                + "erased-type recovery through the only-element #290 erasure)");
        assertTrue(gen.contains("startPrice = toBuilder(referenceWithMetaPriceSchedule.getValue());"),
                "The else-leg of the null-distributed deref re-wraps toBuilder");
    }

    /**
     * F2 TOWARD-heal content lock: both alias-method S3 re-root seats hoist
     * (dl 7 → 1); the file stays divergent ONLY on the pre-existing stale
     * {@code ComparisonResult} import (the #336-documented residual — its producer is
     * a discarded-attempt refs union at the condition seat, not yet located). Converts
     * to a byte lock when that import producer is fixed.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void f2_createCashflowFromSettlementPayout_aliasRerootHoists_contentLock() {
        String gen = gen(cdm6FnOutput, CREATE_CASHFLOW_FROM_SETTLEMENT_PAYOUT);
        assertEquals(3, count(gen, "final ReferenceWithMetaPriceSchedule referenceWithMetaPriceSchedule = "),
                "All three collapse seats hoist (the two alias-method S3 seats now fire "
                + "under the aliasBodySinkHoists sink)");
        assertTrue(gen.contains("(referenceWithMetaPriceSchedule == null ? "
                + "MapperS.<PriceSchedule>ofNull() : MapperS.of(referenceWithMetaPriceSchedule.getValue()))"),
                "The re-rooted guarded deref renders in the alias bodies");
    }

    // ---------------------------------------------------------------- F3 locks

    @Test
    @EnabledIf("cdm6CellAvailable")
    void f3_toDateTime_cdm6_recordCtor_byteLock() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, TO_DATE_TIME);
        String gen = gen(cdm6FnOutput, TO_DATE_TIME);
        // Negative witness (count 0 in golden): the bogus POJO-builder record form.
        assertFalse(gen.contains("zonedDateTime.builder()"),
                "The builtin record must not render as a POJO builder");
        assertTrue(gen.contains("final LocalTime time = toTime.evaluate(0, 0, 0);"),
                "The non-identifier feature values hoist under their FEATURE names");
        assertFalse(gen.contains("final Date date = "),
                "The identifier value hoists NOTHING (the declareAsVariable synonym law)");
        assertTrue(gen.contains(
                "zonedDateTime = ZonedDateTime.of(date.toLocalDate(), time, ZoneId.of(timezone));"),
                "The record constructor renders with the all-features null-guard");
    }

    @Test
    @EnabledIf("cdm5CellAvailable")
    void f3_toDateTime_cdm5_twin_byteLock() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, TO_DATE_TIME);
    }

    // ---------------------------------------------------------------- F4 locks

    @Test
    @EnabledIf("cdm6CellAvailable")
    void f4_createValuation_cdm6_cardinalityJoinTypedEmpty_byteLock() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, CREATE_VALUATION);
        String gen = gen(cdm6FnOutput, CREATE_VALUATION);
        assertTrue(gen.contains("protected abstract MapperC<? extends Valuation> beforeValuationHistory("),
                "The signature joins to the else-arm's MapperC element (cardinality OR)");
        // Negative witness (count 0 in ANY golden — the #269 typed-empty law).
        assertFalse(gen.contains("return MapperC.of();"),
                "The empty then-arm renders the signature-typed ofNull");
        assertTrue(gen.contains("return MapperC.<Valuation>ofNull();"),
                "The typed empty renders from the joined signature");
    }

    @Test
    @EnabledIf("cdm5CellAvailable")
    void f4_createValuation_cdm5_twin_byteLock() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, CREATE_VALUATION);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void f4_resolveSecurityFinanceBillingAmount_divideJoin_byteLock() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR,
                RESOLVE_SECURITY_FINANCE_BILLING_AMOUNT);
    }

    @Test
    @EnabledIf("cdm5CellAvailable")
    void f4_resolveSecurityFinanceBillingAmount_cdm5_twin_byteLock() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR,
                RESOLVE_SECURITY_FINANCE_BILLING_AMOUNT);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void f4_mapReferenceObligation_ancestorLub_byteLock() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_REFERENCE_OBLIGATION);
        String gen = gen(cdm6FnOutput, MAP_REFERENCE_OBLIGATION);
        // Negative witness (count 0 in golden): the first-arm-typed signature.
        assertFalse(gen.contains("MapperS<? extends Bond> debtAsset("),
                "The ladder signature must join to the common ancestor, not the first arm");
        assertTrue(gen.contains("MapperS<? extends UnderlyingAsset> debtAsset("),
                "The LUB (Bond/ConvertibleBond/Loan/Mortgage -> UnderlyingAsset) renders "
                + "— canonical-disambiguated to the FPML model (the cdm-vs-fpml Bond trap)");
        assertTrue(gen.contains("return MapperS.<UnderlyingAsset>ofNull();"),
                "The terminal typed empty takes the joined type");
    }

    @Test
    @EnabledIf("cdm5CellAvailable")
    void f4_createExercise_metaIntersectArmDeref_byteLock() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, CREATE_EXERCISE);
        String gen = gen(cdm5FnOutput, CREATE_EXERCISE);
        assertTrue(gen.contains("protected abstract MapperS<? extends OptionPayout> optionPayout("),
                "The wrapper-vs-value arms join to the VALUE (the meta intersect)");
        assertTrue(gen.contains(".<OptionPayout>map(\"Type coercion\", referenceWithMetaOptionPayout -> "
                + "referenceWithMetaOptionPayout == null ? null : referenceWithMetaOptionPayout.getValue());"),
                "The wrapper arm derefs at its return under the value-joined signature (Part B)");
    }

    // ---------------------------------------------------------------- F5 locks

    @Test
    @EnabledIf("drrCellAvailable")
    void f5_extractHkmaSchemeName_fiveRungLadder_byteLock() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, EXTRACT_HKMA_SCHEME_NAME);
        String gen = gen(drrOutput, EXTRACT_HKMA_SCHEME_NAME);
        // Negative witness (count 0 corpus-wide — the #281 golden law): the inline
        // getOrDefault ternary chain.
        assertFalse(gen.contains(".getOrDefault(false) ?"),
                "The 5-rung inline ternary must restructure to the block ladder (F2a)");
        assertTrue(gen.contains("return MapperS.<HKTRPartyScheme>ofNull();"),
                "The block ladder's typed-empty terminal renders");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void f5_extractHkmaTransactionSchemeName_byteLock() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, EXTRACT_HKMA_TRANSACTION_SCHEME_NAME);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void f5_clearingReceiptTimestampRule_mapperCElseless_byteLock() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, CLEARING_RECEIPT_TIMESTAMP_RULE);
        String gen = gen(drrOutput, CLEARING_RECEIPT_TIMESTAMP_RULE);
        assertTrue(gen.contains("return MapperC.<EventTimestamp>ofNull();"),
                "The MULTI elseless block renders the MapperC typed-empty terminal (F2b)");
        assertFalse(gen.contains(".getOrDefault(false) ?"),
                "The inline ternary must not survive at the mapSingleToList seat");
    }

    /**
     * F5 DEFERRED-carrier lock CONVERTED to its flip lock (PR #350, F1
     * ladderAdmitHandshake — per this lock's own #349 javadoc promise): the chain's
     * thenArg0..3 hoists were already rendered at the SET seat; the ladder admit now
     * keys on the hoisted-chain BINDING (render-truth) instead of the AST base shape,
     * so the last step's ternary renders as golden's block if/return ladder with the
     * typed-empty terminal.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void f5_getNextFloatingReferenceResetDate_flips() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR,
                GET_NEXT_FLOATING_REFERENCE_RESET_DATE);
        String gen = gen(drrOutput, GET_NEXT_FLOATING_REFERENCE_RESET_DATE);
        assertTrue(gen.contains("return MapperS.<Date>ofNull();"),
                "The block-ladder typed-empty terminal renders (the #350-F1 witness)");
        assertFalse(gen.contains(".getOrDefault(false) ?"),
                "The inline ternary must be gone (the #350-F1 negative witness)");
    }

    /**
     * F5 DEFERRED-carrier decline lock CONVERTED to its flip lock (PR #391 — per
     * this lock's own javadoc promise, "Converts with the runtime-then PR"): the
     * alias-body then-chains decompose through the deep route once the
     * collapse-step decl element recovers from prevRef (the Object-decl discard
     * un-blocks), the mapSingleToItem ladder renders golden's if/return block
     * with the typed-empty terminal, and the mis-bound bare LEI/CountryCode
     * comparands requalify through the hierarchy-aware sibling rungs
     * (EntityIdentifierTypeEnum extends PartyIdentifierTypeEnum).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void f5_extractReferenceEntityFormat_flips() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, EXTRACT_REFERENCE_ENTITY_FORMAT);
        String gen = gen(drrOutput, EXTRACT_REFERENCE_ENTITY_FORMAT);
        assertTrue(gen.contains("return MapperS.<ReferenceEntityFormatEnum>ofNull();"),
                "The block-ladder typed-empty terminal renders (the #391 witness)");
        assertTrue(gen.contains("MapperS.of(EntityIdentifierTypeEnum.LEI)"),
                "The mis-bound bare LEI requalifies through the sibling-enum hierarchy"
                + " (the #391 bareEnumComparandHierarchy witness)");
        assertFalse(gen.contains(".getOrDefault(false) ?"),
                "The inline ternary must be gone (the converted decline lock's"
                + " negative witness)");
    }

    // ---------------------------------------------------------------- helpers

    private static int count(String haystack, String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
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
                + path + " (PR #349 the five-facet compose).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
