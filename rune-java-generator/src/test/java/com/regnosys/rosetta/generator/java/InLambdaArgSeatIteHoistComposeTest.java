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
 * PR #355 — the in-lambda arg-seat ite-hoist + the fn-output ladder enum + the
 * condition then-exists hoist: 38 byte flips (33 drr FUNCTION + 5 drr POJO Rules) +
 * 14 TOWARD movers / 0 new / 0 AWAY (regscan355).
 *
 * <p><b>F-1 — {@code inLambdaArgSeatIteHoist}</b> (ControlFlowHandler +
 * StatementHoistSession + JavaStatementScope + JavaRawStatement + HandlerHelper): the
 * LAMBDA_CHANNEL twin of the #181 arg-seat ite-hoist. A no-else / effective-else
 * single-cardinality conditional consumed at an arg seat (ctor pair value, evaluate
 * arg) INSIDE a drainable map/extract lambda hoists golden's item-typed local through
 * the pending-lambda-hoist channel (registerPendingLambdaHoist → compileLambda's
 * expr→block drain, the #312 pattern) instead of the zero-golden inline ternary.
 * Naming: per-LAMBDA session sub-groups ({@code registerLambdaScoped}) — singleton
 * bare / {@code 0..n-1} in consumption order (inner-before-outer via the arm-interior
 * relocation window) / per-name {@code _}-escape against the method group's resolved
 * names (mas Create_ContractType15__1's {@code _ifThenElseResult0/1}). Declines:
 * suppression (the P352A law), body-root conditionals + body-root-conditional lambdas
 * (the block-lambda family's territory), unrestructured runtime {@code .then(}
 * lambdas (the #350 thenArgRefFor render-truth read). Riders: the Integer/Long +
 * BigDecimal-else numeric JOIN and the inner-RULE output typing (asic/mas PriceRule's
 * {@code final BigDecimal ifThenElseResult = BigDecimal.valueOf(99999999999l)}), the
 * in-lambda boolHoist ({@code final Boolean _boolean = isDefaultPrice.evaluate(…)}),
 * the BigDecimal-expected int-literal arm compile.
 *
 * <p><b>F-2 — {@code fnOutputLadderArmEnum} + the SET-operation-root ladder
 * transparency</b> (CollectionHandler): a bare UNRESOLVED ladder-arm symbol qualifies
 * against the enclosing FUNCTION's declared OUTPUT enumeration (value-name match —
 * {@code MapperS.of(TradingCapacity7Code.AGEN)}), the same enum supplying the
 * typed-empty item the blinded inference could not; and a SET-operation-root
 * conditional ancestor is TRANSPARENT in {@code isCleanLadderContext} (a statement
 * seat BY CONSTRUCTION — the P355D disjoint-scope-chain probe showed scope
 * registration cannot serve this seat).
 *
 * <p><b>F-3 — {@code condThenExistsHoist}</b> (CollectionHandler): an elseless
 * map-body conditional whose condition is {@code <chain> then exists} (a NULL
 * exists-argument — the parser leaves the implied piped item unmaterialized) hoists
 * the chain to a per-lambda {@code thenArg} local and guards
 * {@code exists(thenArg).asMapper().getOrDefault(false)} (cftc/csa
 * ExecutionVenueTypeRule).
 *
 * <p>Whole-file byte comparisons run through the REAL D11 generation paths and revert
 * RED without the facets. The negative witnesses are load-bearing per the
 * witness-uniqueness law (OCCURRENCE counts, never line counts): every token below
 * was occurrence-counted in its PRE gen (counts noted per witness) and 0 in its
 * golden — the flips REMOVE them.
 */
class InLambdaArgSeatIteHoistComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String ASIC = "drr/projection/iso20022/asic/rewrite/trade/functions/";
    private static final String ESMA = "drr/projection/iso20022/esma/emir/refit/trade/functions/";
    private static final String FCA = "drr/projection/iso20022/fca/ukemir/refit/trade/functions/";
    private static final String HKMA_DTCC =
            "drr/projection/iso20022/hkma/rewrite/trade/dtcc/functions/";
    private static final String HKMA_TR =
            "drr/projection/iso20022/hkma/rewrite/trade/tr/functions/";
    private static final String JFSA = "drr/projection/iso20022/jfsa/rewrite/trade/functions/";
    private static final String MAS = "drr/projection/iso20022/mas/rewrite/trade/functions/";

    /**
     * The 30 F-1 drr FUNCTION flip carriers (projection families); the 31st,
     * {@link #MAS_GET_TX_PRIC}, is listed separately below.
     */
    private static final String[] F1_FUNCTION_CARRIERS = {
            ASIC + "Create_ContractType15__1.java",
            ASIC + "Create_OptionBarrierLevel1Choice__1.java",
            ASIC + "GetNtnlAmt.java",
            ASIC + "GetOptn.java",
            ASIC + "GetOthrPmt.java",
            ASIC + "GetTxPric.java",
            ESMA + "GetOptn.java",
            ESMA + "GetTxPric.java",
            FCA + "GetOptn.java",
            FCA + "GetOthrPmt.java",
            FCA + "GetTxPric.java",
            HKMA_DTCC + "Create_OptionBarrierLevel1Choice__1.java",
            HKMA_DTCC + "GetNtnlAmt.java",
            HKMA_DTCC + "GetOptn.java",
            HKMA_DTCC + "GetOthrPmt.java",
            HKMA_DTCC + "GetTxPric.java",
            HKMA_TR + "Create_OptionBarrierLevel1Choice__1.java",
            HKMA_TR + "GetNtnlAmt.java",
            HKMA_TR + "GetOptn.java",
            HKMA_TR + "GetOthrPmt.java",
            HKMA_TR + "GetTxPric.java",
            JFSA + "GetOptn1.java",
            JFSA + "GetOthrPmt.java",
            JFSA + "GetPric.java",
            JFSA + "GetStrkPric.java",
            JFSA + "GetTxPric1.java",
            JFSA + "GetUndrlygInstrm.java",
            MAS + "Create_ContractType15__1.java",
            MAS + "GetOptn.java",
            MAS + "GetOthrPmt.java",
    };

    /**
     * The 31st F-1 drr FUNCTION flip carrier (mas GetTxPric), byte-locked in its
     * own test below. NOTE: the hkma-dtcc/hkma-tr Create_ContractType15__1 pair was
     * deliberately ABSENT from every #355 byte lock (a residual to-string-over-collapse
     * rider kept it divergent, dl=2 each); PR #356's toStringOverCollapse facet flipped
     * the pair, and {@code NestedLadderRungComposeTest} now byte-locks it.
     */
    private static final String MAS_GET_TX_PRIC = MAS + "GetTxPric.java";

    /** The F-2 fn-output-ladder flip carriers. */
    private static final String EXTRACT_TRADING_CAPACITY =
            "drr/regulation/common/trade/execution/functions/Extract_TradingCapacity.java";
    private static final String CLEARING_OBLIGATION =
            "drr/regulation/common/trade/execution/functions/ClearingObligation.java";

    /** The 5 drr POJO Rule flip carriers (F-1 riders + F-3). */
    private static final String PRICE_RULE_ASIC =
            "drr/regulation/asic/rewrite/trade/reports/PriceRule.java";
    private static final String PRICE_RULE_MAS =
            "drr/regulation/mas/rewrite/trade/reports/PriceRule.java";
    private static final String PACKAGE_TX_PRICE_ASIC =
            "drr/regulation/asic/rewrite/trade/reports/PackageTransactionPriceRule.java";
    private static final String EVT_CFTC =
            "drr/regulation/cftc/rewrite/trade/reports/ExecutionVenueTypeRule.java";
    private static final String EVT_CSA =
            "drr/regulation/csa/rewrite/trade/reports/ExecutionVenueTypeRule.java";

    private static Map<String, String> drrFnOutput;
    private static Map<String, String> drrCellOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            var drrCell = new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT);
            drrFnOutput = generateFunctions(drrCell);
            drrCellOutput = generateCell(drrCell);
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

    // ------------------------------------------------- F-1 FUNCTION byte locks (33)

    @Test
    @EnabledIf("drrCellAvailable")
    void f1ProjectionCarriers_byteMatchGolden() throws IOException {
        for (String path : F1_FUNCTION_CARRIERS) {
            assertFnByteMatchesGolden(path);
        }
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void masGetTxPric_byteMatchesGolden() throws IOException {
        assertFnByteMatchesGolden(MAS_GET_TX_PRIC);
    }

    // ------------------------------------------------- F-2 FUNCTION byte locks (2)

    @Test
    @EnabledIf("drrCellAvailable")
    void extractTradingCapacity_byteMatchesGolden() throws IOException {
        assertFnByteMatchesGolden(EXTRACT_TRADING_CAPACITY);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void clearingObligation_byteMatchesGolden() throws IOException {
        assertFnByteMatchesGolden(CLEARING_OBLIGATION);
    }

    // ------------------------------------------------- POJO Rule byte locks (5)

    @Test
    @EnabledIf("drrCellAvailable")
    void priceRuleAsic_byteMatchesGolden() throws IOException {
        assertCellByteMatchesGolden(PRICE_RULE_ASIC);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void priceRuleMas_byteMatchesGolden() throws IOException {
        assertCellByteMatchesGolden(PRICE_RULE_MAS);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void packageTransactionPriceAsic_byteMatchesGolden() throws IOException {
        assertCellByteMatchesGolden(PACKAGE_TX_PRICE_ASIC);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void executionVenueTypeCftc_byteMatchesGolden() throws IOException {
        assertCellByteMatchesGolden(EVT_CFTC);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void executionVenueTypeCsa_byteMatchesGolden() throws IOException {
        assertCellByteMatchesGolden(EVT_CSA);
    }

    // ------------------------------------------------- negative witnesses

    /**
     * The F-1 elseless-slot negative witness: the mistyped inline else-tail
     * {@code : MapperC.of().get())} counted EXACTLY 2 occurrences in each PRE gen
     * (the two conditional setter slots) and 0 in each golden — the per-slot
     * {@code ifThenElseResultN = null;} hoists REMOVE it.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void f1Carriers_inlineElseTailGone() {
        assertEquals(0, count(fn(ASIC + "GetOthrPmt.java"), ": MapperC.of().get())"),
                "The inline else-tail is gone (count 0 in golden): asic GetOthrPmt");
        assertEquals(0, count(fn(MAS + "Create_ContractType15__1.java"), ": MapperC.of().get())"),
                "The inline else-tail is gone (count 0 in golden): mas Create_ContractType15__1");
    }

    /**
     * The F-1 numeric-JOIN negative witness: the Mapper-wrapped beyond-int literal
     * {@code MapperS.of(99999999999L)} counted EXACTLY 1 occurrence in the PRE gen
     * and 0 in the golden — the BigDecimal-expected arm compile renders
     * {@code BigDecimal.valueOf(99999999999l)} inside the hoist.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void priceRuleAsic_wrappedLongLiteralGone() {
        assertEquals(0, count(cell(PRICE_RULE_ASIC), "MapperS.of(99999999999L)"),
                "The Mapper-wrapped long literal is gone (count 0 in golden)");
    }

    /**
     * The F-2 negative witnesses: the unresolved bare-enum echoes
     * ({@code MapperS.of(AGEN)} / {@code MapperS.of(UKWN)}) counted EXACTLY 1
     * occurrence each in their PRE gens and 0 in the goldens — the fn-output-enum
     * recovery qualifies them ({@code MapperS.of(TradingCapacity7Code.AGEN)} /
     * {@code MapperS.of(ClearingObligationEnum.UKWN)}).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void fnOutputEnumCarriers_bareEchoGone() {
        assertEquals(0, count(fn(EXTRACT_TRADING_CAPACITY), "MapperS.of(AGEN)"),
                "The bare AGEN echo is gone (count 0 in golden)");
        assertEquals(0, count(fn(CLEARING_OBLIGATION), "MapperS.of(UKWN)"),
                "The bare UKWN echo is gone (count 0 in golden)");
    }

    /**
     * The F-3 negative witness: the runtime condition render
     * {@code .then(_item -> exists(_item))} counted EXACTLY 1 occurrence in the PRE
     * gen and 0 in the golden — the condition-position hoist reads
     * {@code exists(thenArg).asMapper()} over the hoisted chain instead.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void executionVenueTypeCftc_runtimeThenConditionGone() {
        assertEquals(0, count(cell(EVT_CFTC), ".then(_item -> exists(_item))"),
                "The runtime .then(exists) condition is gone (count 0 in golden)");
    }

    // ---------------------------------------------------------------- helpers

    private static int count(String haystack, String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
    }

    private static String fn(String path) {
        assertNotNull(drrFnOutput, "function generation did not run — corpus unavailable?");
        String g = drrFnOutput.get(path);
        assertNotNull(g, "Function not generated: " + path);
        return g;
    }

    private static String cell(String path) {
        assertNotNull(drrCellOutput, "cell generation did not run — corpus unavailable?");
        String g = drrCellOutput.get(path);
        assertNotNull(g, "Class not generated: " + path);
        return g;
    }

    private static void assertFnByteMatchesGolden(String path) throws IOException {
        assertBytes(path, fn(path));
    }

    private static void assertCellByteMatchesGolden(String path) throws IOException {
        assertBytes(path, cell(path));
    }

    private static void assertBytes(String path, String gen) throws IOException {
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath).replace("\r", "");
        assertEquals(golden, gen.replace("\r", ""),
                "Generated bytes must match the golden for " + path);
    }
}
