package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * PR #351 — the ctl-then restructure foundation + the render-truth walk: 10 byte flips
 * (cdm5 1 + cdm6 6 + drr FUNCTION 2 + drr POJO 1) + 32 TOWARD movers / 0 AWAY.
 *
 * <p><b>F-import — {@code condHoistImportTruth}</b>: the #225 condition-hoist block seat
 * no longer refs {@code ComparisonResult} unconditionally — a #249/#349 erased-type
 * meta-deref recovery hoist at that seat carries no CR token, and the former blanket ref
 * leaked a stale import into the otherwise-golden Create_CashflowFromSettlementPayout.
 * A2 carriers keep the import via the consumer expression's own refs.
 *
 * <p><b>F-neg — {@code bareNegLiteral}</b>: a unary-negated BARE int literal renders as
 * the negated literal per the LiteralHandler context law ({@code MapperS.of(-1)} at an
 * Integer seat — AddBusinessDays cdm5/cdm6; {@code MapperS.of(BigDecimal.valueOf(-1))}
 * at a number seat; raw {@code -1} at an evaluate-arg seat via the wrappedInMapperSOf
 * unwrap contract — GenerateDateList). The #206 rewrite arm keeps {@code -(lit * expr)};
 * the general multiply-by--1 desugar keeps every non-literal operand (Abs stays green).
 *
 * <p><b>F-alias — {@code aliasThenSigRenderTruth}</b>: the alias then-chain signature
 * types from the body render truth — a bare {@code then filter/sort} body pipes the
 * receiver through (the parser hangs a SYNTHETIC implicit input on {@code left()});
 * a body rooted at a FEATURE of the receiver element resolves by name against the
 * element's model type; a per-item single body over a MULTI receiver is elementwise
 * MULTI (attribute-rooted only — pipe-consuming bodies keep their own cardinality).
 * New-path results are guarded by the sanitizeArm lowercase law (a junk symbol-echo
 * receiver must not replace the legacy fallback — the cp3c MapIntent catch).
 * GetRegimeSpecificIdentifiers ({@code MapperC<? extends string>} →
 * {@code MapperC<? extends TradeIdentifier>}) + Get_OptionPremiumOnEventDate
 * ({@code MapperS} → {@code MapperC}) flip.
 *
 * <p><b>F3-i1/i2/i3 — {@code deepThenCtlRestructure}</b>: the deep seat's #250 blanket
 * control-flow decline narrows to UNHANDLED control flow — a then-body whose ROOT is a
 * conditional with ctl-free arms restructures as golden's Mapper-typed
 * {@code final Mapper*<X> ifThenElseResult; if/else[-if]} block via the
 * node-identity handshake ({@code JavaStatementScope.DeepThenIteHoist}, the
 * #327/#339/#340 single-slot pattern) to
 * {@code ControlFlowHandler.hoistAsDeepThenMapperLocalOrNull} (FIRST in the arm chain —
 * the cp4 Franken catch). Conditional levels emit NO thenArg decl (the next level
 * re-roots on the bare sentinel — the SET-seat #257 continue pattern); arms compile
 * INTERIOR and keep their Mapper form (wraps only for INVOKABLE arms — the cp6b
 * double-wrap catch); item types recover render-true from the compiled arms then the
 * #238 AST walk (the snapshot reports MISSING/Object for the StandardizedSchedule*
 * conditionals). The alias-then admission + the function all-or-nothing guard moved to
 * the narrowed predicate in LOCKSTEP (the #341 identical-predicate law). The
 * FunctionAliasHelper walk gained the THEN-ITEM BINDING: implicit-item receivers,
 * unresolved bare symbols, and the {@code root -> feature} enum mis-parse resolve as
 * features of the bound element — through a META WRAPPER's VALUE type
 * (render-truth: arms deref via "Type coercion" then nav the value). The
 * StandardizedSchedule{CommodityForward,CommoditySwapFixedFloat,EquityForward,Option}
 * NotionalAmount quartet flips (body restructure + the {@code MapperS<BigDecimal>}
 * signature); Counterparty2FinancialEntityIndicatorRule flips (drr POJO — the
 * deep-seat consumer ite hoist at a rule sink).
 *
 * <p>Whole-file byte comparisons run through the REAL D11 generation paths and revert
 * RED without the facets. StandardizedScheduleDuration keeps its #353
 * conversion-ready decline lock (arms carry control flow);
 * GetUnderlierProductIdentifierLeg1's lock was converted at #357 (the chain
 * restructure) and the carrier FLIPPED byte-identical at #373 (facet
 * blockLambdaCardinalityJoin — the MapperC arm-join lift).
 */
class CtlThenRestructureFoundationComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // F-neg flip carriers (both cdm cells) + the raw-arg TOWARD content lock.
    private static final String ADD_BUSINESS_DAYS =
            "cdm/base/datetime/functions/AddBusinessDays.java";
    private static final String GENERATE_DATE_LIST =
            "cdm/base/datetime/functions/GenerateDateList.java";
    // F-import flip carrier.
    private static final String CREATE_CASHFLOW_FROM_SETTLEMENT_PAYOUT =
            "cdm/product/template/functions/Create_CashflowFromSettlementPayout.java";
    // F3-i2 flip carriers (two of the quartet lock the class; the D11 20/20 locks all).
    private static final String STANDARDIZED_SCHEDULE_COMMODITY_FORWARD =
            "cdm/margin/schedule/functions/StandardizedScheduleCommodityForwardNotionalAmount.java";
    private static final String STANDARDIZED_SCHEDULE_OPTION =
            "cdm/margin/schedule/functions/StandardizedScheduleOptionNotionalAmount.java";
    // F-alias flip carriers (drr FUNCTION).
    private static final String GET_REGIME_SPECIFIC_IDENTIFIERS =
            "drr/regulation/common/functions/GetRegimeSpecificIdentifiers.java";
    private static final String GET_OPTION_PREMIUM_ON_EVENT_DATE =
            "drr/regulation/common/functions/Get_OptionPremiumOnEventDate.java";
    // F3-i1 flip carrier (drr POJO Rule — the deep-seat consumer ite hoist).
    private static final String COUNTERPARTY_2_FINANCIAL_ENTITY_INDICATOR_RULE =
            "drr/regulation/cftc/rewrite/trade/reports/Counterparty2FinancialEntityIndicatorRule.java";
    // #353 DEFERRED carriers (decline locks — the in-lambda if/return increment converts).
    private static final String STANDARDIZED_SCHEDULE_DURATION =
            "cdm/margin/schedule/functions/StandardizedScheduleDuration.java";
    private static final String GET_UNDERLIER_PRODUCT_IDENTIFIER_LEG_1 =
            "drr/regulation/common/functions/GetUnderlierProductIdentifierLeg1.java";

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

    /** FUNCTION + Rule generation for the drr cell (the F3-i1 lock is a POJO Rule). */
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
        var errors = funcGen.generateWithErrors(output);
        assertTrue(errors.isEmpty(),
                () -> "Function generation reported " + errors.size() + " error(s): " + errors);
        return output;
    }

    // ------------------------------------------------------------- F-neg byte locks

    @Test
    @EnabledIf("cdm6CellAvailable")
    void addBusinessDays_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, ADD_BUSINESS_DAYS);
    }

    @Test
    @EnabledIf("cdm5CellAvailable")
    void addBusinessDays_cdm5_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, ADD_BUSINESS_DAYS);
    }

    /**
     * The bare-negLiteral EVALUATE-ARG form (TOWARD content lock — GenerateDateList
     * still composes with the conditional-ADD restructure): the raw {@code -1} arg via
     * the wrappedInMapperSOf unwrap contract, golden-verbatim. The negative witness:
     * the multiply-by--1 desugar of the bare literal is GONE (count 0 in golden).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void generateDateList_rendersRawNegativeLiteralArg() {
        String gen = gen(cdm6FnOutput, GENERATE_DATE_LIST);
        assertTrue(gen.contains("evaluate(endDate, -1, businessCenters)"),
                "The evaluate arg renders the raw negated literal");
        assertEquals(0, count(gen, "multiply(MapperS.of(BigDecimal.valueOf(-1)), MapperS.of(1))"),
                "The bare-literal multiply desugar is gone (count 0 in golden)");
    }

    // ----------------------------------------------------------- F-import byte lock

    @Test
    @EnabledIf("cdm6CellAvailable")
    void createCashflowFromSettlementPayout_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR,
                CREATE_CASHFLOW_FROM_SETTLEMENT_PAYOUT);
    }

    // ------------------------------------------------------------- F3-i2 byte locks

    @Test
    @EnabledIf("cdm6CellAvailable")
    void standardizedScheduleCommodityForward_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR,
                STANDARDIZED_SCHEDULE_COMMODITY_FORWARD);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void standardizedScheduleOption_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, STANDARDIZED_SCHEDULE_OPTION);
    }

    // ------------------------------------------------------------ F-alias byte locks

    @Test
    @EnabledIf("drrCellAvailable")
    void getRegimeSpecificIdentifiers_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, GET_REGIME_SPECIFIC_IDENTIFIERS);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void getOptionPremiumOnEventDate_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, GET_OPTION_PREMIUM_ON_EVENT_DATE);
    }

    // -------------------------------------------------------------- F3-i1 byte lock

    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty2FinancialEntityIndicatorRule_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR,
                COUNTERPARTY_2_FINANCIAL_ENTITY_INDICATOR_RULE);
    }

    // ------------------------------------------- #353 conversion-ready decline locks

    /**
     * FLIPPED at PR #379 (facet aliasLadderNestedArmAdmit, B — the x353 deferred-carrier
     * lock converts): the alias-seat nested-cond consumer admission generalized from the
     * #377-M1 single-rung shape to the full elseless spine (each rung arm ctl-free or a
     * ctl-free nested conditional ladder, ≥1 nested arm), so the {@code optionExpiry}
     * chain hoists its invocation-bearing base as {@code final MapperS<ExerciseTerms>
     * thenArg} and the consumer renders golden's {@code .mapSingleToItem(item -> { … })}
     * block ladder — the nested-tree multi-line-arm render decline lifted WITH it (the
     * #356 note: the two land together), the European {@code .max(…).mapSingleToItem(…)}
     * continuation arms render via the #333 re-anchor, and the Bermuda nested rung
     * renders one level in with the typed {@code MapperS.<Date>ofNull()} fall-throughs.
     * The whole file is now BYTE-IDENTICAL to golden.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void x353_standardizedScheduleDuration_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, STANDARDIZED_SCHEDULE_DURATION);
    }

    /**
     * FLIPPED at PR #373 (facet blockLambdaCardinalityJoin — the second conversion of
     * this carrier's lock, after the #357 chain restructure): the conditional arm-JOIN
     * goes to upstream's OR (any MULTI arm makes the block MULTI), so the base ITE
     * decl lifts to golden's {@code final MapperC<Product> thenArg0;} with the
     * single-arm {@code MapperC.of(Collections.singletonList(…))} coercion and the
     * {@code MapperC.<Product>ofNull()} empty — the whole file is now BYTE-IDENTICAL
     * to golden (the flip supersedes the #357 partial-restructure witnesses).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void x373_getUnderlierProductIdentifierLeg1_flipsByteIdentical() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR,
                GET_UNDERLIER_PRODUCT_IDENTIFIER_LEG_1);
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
        String gen = gen(output, path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath).replace("\r", "");
        assertEquals(golden, gen.replace("\r", ""),
                "Generated bytes must match the golden for " + path);
    }
}
