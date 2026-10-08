package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * PR #377 — the alias-window octet: 8 byte flips (all drr FUNCTIONs) + 0 new / 0 away
 * (regscan377: 8/0/0; 6 TOWARD movers content-verified).
 *
 * <p><b>L aliasElsefulCondStepAdmit</b> (CollectionHandler + FunctionExpressionRenderer +
 * HandlerHelper): {@code renderAliasThenHoistOrNull} pushes a single-slot node-identity
 * window (spine-admitting — the all-or-nothing guard's descendant walk reaches nested
 * prefix chains) around the sink compile of a bare-item-cardinality-LAST value chain;
 * {@code thenChainHasUnhandledControlFlow}'s extract-conditional STEP arm admits ctl-free
 * elseful conditionals under it (the #361 rule-path arm's alias-seat sibling; the guard
 * cache is bypassed both directions during the window), the step's decl element recovers
 * via the gm-aware IDENTICAL arm join, and a bare boolean FUNCTION-INPUT condition
 * renders golden's raw null-safe guard: flips the ForRegime trio
 * (Create_Collateral/Transaction/ValuationReportInstructionForRegime).
 *
 * <p><b>M aliasNestedCondConsumerAdmit + fnCallRecordReceiver</b>: the window's
 * ELSELESS-nested-then consumer variant — the venueMic shape renders FREE through the
 * #294 nested-tree block form (mapSingleToItem + nested if/return + the typed
 * {@code MapperS.<String>ofNull()} fall-through) — and the record-feature nav fires over
 * a zonedDateTime-returning call ({@code resolveReceiverRType}'s callee-OUTPUT arm:
 * {@code .<Date>map("Date", zdt -> Date.of(zdt.toLocalDate()))}): flips EMIR_ISIN +
 * UKEMIR_ISIN.
 *
 * <p><b>V aliasLadderNestedThenArm</b> (FunctionExpressionRenderer):
 * {@code returnLadderEligible}/{@code appendReturnLadder} recurse a ladder-eligible
 * nested-THEN arm as golden's inner if/return block one depth in (the shared alias
 * session keeps boolHoist numbering per-method): flips EMIR_Venue + UKEMIR_Venue;
 * CallQuantity/PutQuantity ride dl 59→20 and the AnnaDsb monster 747→568.
 *
 * <p><b>P condBaseBareInvokableThenArg</b> (CollectionHandler):
 * {@code isHoistableThenChainLocal} admits a FUNCTION-path bare-invokable body over a
 * CONDITIONAL base — the #351-i3 blank-final thenArg hoist + the #339 consumer collapse
 * {@code MapperS.of(fmtFn.evaluate(thenArgN.get()))}: flips PriceFormatFromNotation.
 *
 * <p><b>U iteArmBareInvokableMetaDeref</b> (ControlFlowHandler): a bare-INVOKABLE ITE arm
 * whose callee's declared OUTPUT is a META wrapper assigned to a VALUE-typed local hoists
 * the wrapper local INSIDE the owning branch + assigns the guarded deref: cdm6
 * MapUnitTypeWithScheme's 3 meta hunks land byte-exact (dl 22→10 TOWARD; the file's
 * residual is the getOrDefault-arg composite — a different mechanism).
 *
 * <p>Whole-file byte locks run through the REAL D11 FUNCTION generation path and revert
 * RED without the facets; every witness token is occurrence-counted (python
 * {@code str.count} semantics — the #352 law) and PRE-counted against f-probe-376post
 * (each "old" token PRE 1 / golden 0; each "golden" token PRE 0 / golden 1).
 */
class AliasWindowOctetComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String COLLATERAL_FOR_REGIME =
            "drr/enrichment/common/margin/functions/Create_CollateralReportInstructionForRegime.java";
    private static final String TRANSACTION_FOR_REGIME =
            "drr/enrichment/common/trade/functions/Create_TransactionReportInstructionForRegime.java";
    private static final String VALUATION_FOR_REGIME =
            "drr/enrichment/common/valuation/functions/Create_ValuationReportInstructionForRegime.java";
    private static final String EMIR_ISIN =
            "drr/regulation/esma/emir/refit/trade/functions/EMIR_ISIN.java";
    private static final String UKEMIR_ISIN =
            "drr/regulation/fca/ukemir/refit/trade/functions/UKEMIR_ISIN.java";
    private static final String EMIR_VENUE =
            "drr/regulation/esma/emir/refit/trade/functions/EMIR_Venue.java";
    private static final String UKEMIR_VENUE =
            "drr/regulation/fca/ukemir/refit/trade/functions/UKEMIR_Venue.java";
    private static final String PRICE_FORMAT_FROM_NOTATION =
            "drr/standards/iosco/cde/base/price/functions/PriceFormatFromNotation.java";
    private static final String MAP_UNIT_TYPE_WITH_SCHEME =
            "cdm/ingest/fpml/confirmation/common/functions/MapUnitTypeWithScheme.java";

    private static Map<String, String> drrFnOutput;
    private static Map<String, String> cdm6FnOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
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

    // ==== byte locks (all 8 flips through the REAL D11 FUNCTION route) ====

    /** L + M + P: the window-admitted consumer flips (6 files). */
    @Test
    @EnabledIf("drrCellAvailable")
    void aliasWindowConsumerFlips_byteMatchGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, COLLATERAL_FOR_REGIME);
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, TRANSACTION_FOR_REGIME);
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, VALUATION_FOR_REGIME);
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, EMIR_ISIN);
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, UKEMIR_ISIN);
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, PRICE_FORMAT_FROM_NOTATION);
    }

    /** V: the nested-then-arm return-ladder flips (the Venue twins). */
    @Test
    @EnabledIf("drrCellAvailable")
    void venueLadderFlips_byteMatchGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, EMIR_VENUE);
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, UKEMIR_VENUE);
    }

    // ==== occurrence-counted witnesses (tokens PRE-counted vs f-probe-376post) ====

    /**
     * L3: the bare boolean FUNCTION-INPUT condition renders golden's raw null-safe
     * guard (PRE 0 / golden 1); the MapperS.of wrap it replaces is gone (PRE 1 /
     * golden 0 — zero goldens carry the wrap form at any if seat, corpus-verified).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void forRegime_rawBoolInputGuard() {
        String gen = generated(drrFnOutput, COLLATERAL_FOR_REGIME);
        assertEquals(1, count(gen, "if ((delegatedReporting == null ? false : delegatedReporting))"),
                "golden's raw null-safe input guard must land (PRE 0, golden 1)");
        assertEquals(0, count(gen, "if (MapperS.of(delegatedReporting).getOrDefault(false))"),
                "the MapperS.of-wrapped input condition must be gone (PRE 1, golden 0)");
    }

    /**
     * M: the venueMic consumer re-roots on the hoisted thenArg (the runtime
     * {@code .then(} form PRE 1 / golden 0), the nested block's typed empty lands
     * (PRE 0 / golden 1), and the M3 record nav replaces the non-compiling getter
     * form (getter PRE 1 / golden 0; record PRE 0 / golden 1).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void emirIsin_venueMicRestructured() {
        String gen = generated(drrFnOutput, EMIR_ISIN);
        assertEquals(0, count(gen, "return micDataForFacility(reportableEvent).then(item -> item"),
                "the runtime .then( venueMic form must be gone (PRE 1, golden 0)");
        assertEquals(1, count(gen, "return MapperS.<String>ofNull();"),
                "the nested block's typed-empty fall-through must land (PRE 0, golden 1)");
        assertEquals(0, count(gen, "getExecutionTimestamp -> getExecutionTimestamp.getDate()"),
                "the non-compiling record-getter form must be gone (PRE 1, golden 0)");
        assertEquals(1, count(gen, "zdt -> Date.of(zdt.toLocalDate())"),
                "the record-feature nav must land (PRE 0, golden 1)");
    }

    /**
     * V: the transactionValidMic nested-then ladder hoists the bool-fn-call rung
     * (PRE 0 / golden 1) and the inline nested ternary's empty tail is gone
     * (PRE 1 / golden 0).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void emirVenue_nestedLadderBoolHoist() {
        String gen = generated(drrFnOutput, EMIR_VENUE);
        assertEquals(1, count(gen, "final Boolean boolean0 = isEmirTradingVenue.evaluate("),
                "the nested rung's bool hoist must land (PRE 0, golden 1)");
        assertEquals(0, count(gen, " : MapperC.of();"),
                "the inline nested ternary's untyped-empty tail must be gone (PRE 1, golden 0)");
    }

    /**
     * P: the ctor-setter arg collapses to golden's consumer form (PRE 0 / golden 1);
     * the raw-local runtime {@code .then(} form is gone (PRE 1 / golden 0).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void priceFormat_condBaseThenArgCollapsed() {
        String gen = generated(drrFnOutput, PRICE_FORMAT_FROM_NOTATION);
        assertEquals(1, count(gen, "MapperS.of(formatToBaseOne18Rate.evaluate(thenArg0.get())).get()"),
                "golden's collapsed bare-invokable consumer must land (PRE 0, golden 1)");
        assertEquals(0, count(gen,
                "ifThenElseResult0.then(item -> formatToBaseOne18Rate.evaluate(item.get())).get()"),
                "the raw-local runtime .then( arg form must be gone (PRE 1, golden 0)");
    }

    /**
     * U (TOWARD witness — the file stays divergent on the getOrDefault-arg composite):
     * the in-branch wrapper hoist + guarded deref land byte-exact (PRE 0 / golden 1).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapUnitType_metaDerefInsideBranch() {
        String gen = generated(cdm6FnOutput, MAP_UNIT_TYPE_WITH_SCHEME);
        assertEquals(1, count(gen, "final FieldWithMetaCapacityUnitEnum fieldWithMetaCapacityUnitEnum"
                + " = mapCapacityUnitWithScheme.evaluate(value, scheme);"),
                "the in-branch wrapper hoist must land (PRE 0, golden 1)");
        assertEquals(1, count(gen, "ifThenElseResult0 = fieldWithMetaCapacityUnitEnum == null"
                + " ? null : fieldWithMetaCapacityUnitEnum.getValue();"),
                "the guarded deref assignment must land (PRE 0, golden 1)");
    }

    // ==== helpers ====

    private String generated(Map<String, String> output, String path) {
        assertNotNull(output, "cell output not generated");
        String gen = output.get(path);
        assertNotNull(gen, "missing generated output: " + path);
        return gen;
    }

    /** Occurrence count (python str.count semantics — the #352 law, NOT line count). */
    private static int count(String haystack, String needle) {
        int n = 0;
        for (int at = haystack.indexOf(needle); at >= 0; at = haystack.indexOf(needle, at + needle.length())) {
            n++;
        }
        return n;
    }

    private void assertByteMatchesGolden(Map<String, String> output, Path goldenDir, String path)
            throws IOException {
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "missing golden: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated(output, path)),
                path + " must byte-match the frozen 9.83.0 golden");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
