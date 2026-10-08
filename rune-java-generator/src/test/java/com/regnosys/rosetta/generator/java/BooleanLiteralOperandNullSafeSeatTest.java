package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;

/**
 * SEAT 19 — facet {@code booleanLiteralOperandNullSafe}: <b>a boolean LITERAL operand of a logical
 * {@code and}/{@code or} is coerced to a {@code ComparisonResult} —
 * {@code ComparisonResult.ofNullSafe(MapperS.of(true|false))} — exactly like every other
 * non-{@code ComparisonResult} operand, in every context.</b>
 *
 * <p><b>The defect.</b> {@code LogicalHandler.handle} renders each operand through
 * {@code wrapBooleanFunctionOperand}, a SHAPE-by-SHAPE ladder of "this operand is a Mapper, not a
 * {@code ComparisonResult}; add the missing layer" arms (collapse-over-invokable, boolean nav,
 * disguised nav, type-condition bound attribute, then-chain, extract, alias, boolean parameter,
 * default, function/rule). An {@code RBooleanLiteral} matched NONE of them and fell through
 * {@code return rendered;} — and {@code LiteralHandler} renders a boolean literal as
 * {@code MapperS.of(true)}, a {@code MapperS<Boolean>}. {@code andNullSafe}/{@code orNullSafe} are
 * instance methods of {@code ComparisonResult} taking ONLY a {@code ComparisonResult}
 * ({@code rune-runtime ComparisonResult.java:97/150}), so the fork wrote
 * {@code MapperS.of(true).andNullSafe(MapperS.of(true))} where golden writes
 * {@code ComparisonResult.ofNullSafe(MapperS.of(true)).andNullSafe(ComparisonResult.ofNullSafe(MapperS.of(true)))}
 * — 13 drr 5.61.0 {@code DATA_RULE} files ({@code condition …: True and True}, the CFTC Part 43/45
 * "not modelled until UPI is available" stubs), 2 diff-lines each.
 *
 * <p><b>Upstream has no shape ladder here at all</b> ({@code ExpressionGenerator.xtend:406-411}):
 * both operands of {@code and}/{@code or} compile under
 * {@code context.withExpected(COMPARISON_RESULT)}, and {@code TypeCoercionService}'s item →
 * ComparisonResult conversion ({@code :594-596}) writes
 * {@code ComparisonResult.ofNullSafe(MapperS.of(<expr>))}. That is why the wrap has NO context
 * gate in this seat either: a FUNCTION-seat literal operand takes it too ({@code a5}).
 *
 * <p><b>LAW 69 — the other half was already recorded, one handler over.</b>
 * {@code ControlFlowHandler}'s hoist-arm renderer (the A2 {@code ifThenElseResult} hoist of a
 * CONDITIONAL operand) had carried
 * {@code if (arm instanceof RBooleanLiteral) return "ComparisonResult.ofNullSafe(" + rendered + ")";}
 * locally since the facet landed, and THEN delegated every other arm to the shared wrapper. The
 * conditional-ARM half knew the law; the logical-OPERAND half — the shared wrapper itself — never
 * did. This seat moves the arm INTO the shared wrapper ({@code wrapBooleanFunctionOperandShapeArms},
 * reached by BOTH call-forms: the 5-arg compiler-aware overload {@code LogicalHandler} uses and the
 * 4-arg overload {@code ControlFlowHandler} uses) and replaces the local check with the delegation
 * it already made on the next line — byte-neutral for the arm half by construction (the same
 * string), which {@code b1} pins against the fixture and the full-matrix receipt pins against
 * every cell. The IR route reaches the same wrapper by construction — {@code adaptLogical} declines
 * a literal operand ({@code operandBooleanLiteral}), and {@code DataRuleGenerator} is legacy on both
 * routes — and {@code a7} is the ON-route fixture receipt (Copilot round 1).
 *
 * <p><b>LAW 75 — the whole-matrix census BEFORE the seat, not the band after it.</b> Golden side:
 * 4,321 golden files across every cell carry {@code ofNullSafe(MapperS.of(true|false))}; classified
 * by POSITION, <b>exactly 13 carry it in OPERAND position</b> (a receiver or argument of
 * {@code and/orNullSafe}) — the 13 band rows, all drr 5.61.0 {@code DATA_RULE}, one on each side;
 * the other 4,308 are the branch/return-position form the {@code ControlFlowHandler} half already
 * emits byte-identically. Source side: of 652 {@code .rosetta} lines matching a literal beside
 * {@code and}/{@code or}, the DIRECT-operand shape ({@code True and True}) is 13 lines, all
 * drr-5.61.0 — the other 639 are NON-operand matches: 399 {@code x = True and …} EQUALITY operands
 * ({@code b3}) and 240 doc-string prose lines ("…True or False value…"), the #591 review's R-3
 * correction of a first draft that called all 639 equality operands.
 * Fork side, over the WHOLE current band dump: exactly 13 files carry an un-coerced Mapper
 * {@code and/or} operand, all 13 the literal form; golden 0. And the runtime probe over all 275
 * rows ({@code SEAT19_PROBE}, at the logical seat): the literal operands reaching the wrapper are
 * exactly the carriers' — see {@code target/seat19-charter.md} §3b for the receipt.
 *
 * <p><b>LAW 74 — javac was run against the shipped {@code rune-runtime} 9.83.0.</b> The pre-seat
 * form: {@code error: cannot find symbol — method andNullSafe(MapperS<Boolean>), location: class
 * MapperS<Boolean>}. The half-wrapped form
 * {@code ComparisonResult.ofNullSafe(MapperS.of(true)).andNullSafe(MapperS.of(true))}:
 * {@code error: incompatible types: no instance(s) of type variable(s) T exist so that MapperS<T>
 * conforms to ComparisonResult}. The post-seat form compiles. Neither a Mapper RECEIVER nor a
 * Mapper ARGUMENT compiles, so the un-coerced form is in no green file (green-safe by
 * construction) and this seat repairs <b>13 currently non-compiling corpus files</b>.
 *
 * <p><b>LAW 66 — the positive control is a mutation that was actually run.</b> Deleting the new
 * arm from the shared wrapper turns {@code a1–a6}, {@code corpus_c1–c3} and {@code corpus_control1}
 * RED (the failing-first receipt at the pre-seat blob is that same state, 16 run / 10 F); {@code b1} stays GREEN under that mutation
 * ONLY if {@code ControlFlowHandler}'s local check is also restored — with the delegation in place
 * and the arm deleted, {@code b1} goes RED too, which is what makes {@code b1} a pin on the
 * delegation rather than a vacuous pass. The whole-cell {@code control0} reads the oracle off
 * GOLDEN and pins measured counts, so the two emitter controls cannot pass by finding nothing.
 */
class BooleanLiteralOperandNullSafeSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            // THIS repo's own builtins, LAST so the roots above keep their precedence — a checkout
            // without the external corpus RUNS these tests instead of skipping them (#586: a test
            // that skips is not a lock).
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    /**
     * One type, every operand shape the seat must tell apart, each as its own {@code condition}
     * (a DATA_RULE — the carriers' kind; the corpus shape is a type condition whose whole body is
     * {@code True and True}), plus one FUNCTION for the no-context-gate pin.
     */
    private static final String MODEL = """
            namespace census.seat19
            version "1.0.0"

            type Probe:
                flag boolean (0..1)
                other boolean (0..1)
                n int (0..1)

                condition A1BothLiterals: <"a1 - THE CARRIER SHAPE: the drr 5.61.0 CFTC stub">
                    True and True

                condition A2OrWithFalse: <"a2 - the OR method and the false literal">
                    True or False

                condition A3LiteralLeftComparisonRight: <"a3 - literal LEFT, comparison RIGHT untouched">
                    True and n = 1

                condition A4ExistsLeftLiteralRight: <"a4 - existence LEFT, literal RIGHT">
                    flag exists and False

                condition A6NestedChainThenLiteral: <"a6 - the nested sub-chain is already a ComparisonResult">
                    (True and True) or False

                condition B1ConditionalOperandArms: <"b1 - literal ARMS of a conditional OPERAND take the hoist path">
                    (if flag exists then True else False) and other exists

                condition B2DefaultOperand: <"b2 - a literal INSIDE a default operand is not the operand">
                    (flag default True) and other exists

                condition B3EqualityOperand: <"b3 - a literal as an EQUALITY operand is not a logical operand">
                    flag = True and other exists

            func A5FunctionContext: <"a5 - no context gate: a FUNCTION seat wraps the operands too">
                inputs:
                    flag boolean (0..1)
                output:
                    out boolean (1..1)
                set out:
                    True and True
            """;

    /** Golden's exact line for the carrier shape ({@code True and True}). */
    private static final String CARRIER_LINE =
            "return ComparisonResult.ofNullSafe(MapperS.of(true))"
            + ".andNullSafe(ComparisonResult.ofNullSafe(MapperS.of(true)));";

    /** The pre-seat form. Does not compile (LAW 74). */
    private static final String PRE_SEAT_LINE =
            "return MapperS.of(true).andNullSafe(MapperS.of(true));";

    // =========================================================================
    // Part A — the seat (RED at the pre-seat blob)
    // =========================================================================

    /** a1 — the carrier shape, golden's whole {@code return} line byte-for-byte. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_bothLiteralOperandsAreWrapped() throws IOException {
        String out = datarule("ProbeA1BothLiterals.java");
        assertContains(out, CARRIER_LINE);
        assertNotContains(out, PRE_SEAT_LINE);
    }

    /** a2 — the {@code or} method and the {@code false} literal take the same wrap. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_theOrMethodAndTheFalseLiteral() throws IOException {
        String out = datarule("ProbeA2OrWithFalse.java");
        assertContains(out, "return ComparisonResult.ofNullSafe(MapperS.of(true))"
                + ".orNullSafe(ComparisonResult.ofNullSafe(MapperS.of(false)));");
    }

    /**
     * a3 — a literal LEFT beside a comparison RIGHT: the literal wraps, the comparison (already a
     * {@code ComparisonResult}) is untouched, and the comparison's OWN literal {@code MapperS.of(1)}
     * is an {@code areEqual} argument, not a logical operand — it must not wrap.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_literalLeftComparisonRight() throws IOException {
        String out = datarule("ProbeA3LiteralLeftComparisonRight.java");
        assertContains(out, "return ComparisonResult.ofNullSafe(MapperS.of(true)).andNullSafe(areEqual(");
        assertContains(out, "MapperS.of(1), CardinalityOperator.All));");
        assertNotContains(out, "ofNullSafe(MapperS.of(1))");
        assertNotContains(out, "ofNullSafe(areEqual(");
    }

    /** a4 — the RIGHT-only literal: an existence LEFT is a {@code ComparisonResult} already. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_existsLeftLiteralRight() throws IOException {
        String out = datarule("ProbeA4ExistsLeftLiteralRight.java");
        assertContains(out, ".andNullSafe(ComparisonResult.ofNullSafe(MapperS.of(false)));");
        assertContains(out, "return exists(MapperS.of(probe)");
        assertNotContains(out, "ofNullSafe(exists(");
    }

    /**
     * a5 — <b>NO CONTEXT GATE.</b> Upstream compiles every {@code and}/{@code or} operand under the
     * {@code COMPARISON_RESULT} expectation regardless of kind, so a FUNCTION-seat literal operand
     * wraps too. The census finds no corpus carrier for this shape (13 of 13 are DATA_RULE), so this
     * fixture is the only witness — it pins the LAW, not a golden file.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a5_aFunctionSeatLiteralOperandWrapsToo() throws IOException {
        String out = function("A5FunctionContext.java");
        assertContains(out, "ComparisonResult.ofNullSafe(MapperS.of(true))"
                + ".andNullSafe(ComparisonResult.ofNullSafe(MapperS.of(true)))");
        assertNotContains(out, "MapperS.of(true).andNullSafe(");
    }

    /**
     * a6 — a NESTED sub-chain is already a {@code ComparisonResult} (an {@code RLogicalExpr} is not
     * a literal), so only the three LEAF literals wrap — never the sub-chain.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a6_nestedChainWrapsOnlyTheLeafLiterals() throws IOException {
        String out = datarule("ProbeA6NestedChainThenLiteral.java");
        assertEquals(3, countOccurrences(out, "ComparisonResult.ofNullSafe(MapperS.of("),
                "exactly the three leaf literals wrap");
        assertContains(out, ".orNullSafe(ComparisonResult.ofNullSafe(MapperS.of(false)));");
        assertNotContains(out, "ofNullSafe(ComparisonResult.ofNullSafe(");
        assertNotContains(out, "MapperS.of(true).andNullSafe(");
    }

    /**
     * a7 — <b>THE ON-ROUTE REGRESSION (Copilot round 1).</b> Copilot read the IR route's
     * {@code isCoercedBooleanOperand} list, saw no {@code IRLiteral} in it, and concluded a
     * literal-only logical root "remains eligible for native IR emission" where
     * {@code IRJavaLeafEmitter.emitLogical} would compose the two {@code MapperS.of(…)} directly.
     * That is FALSE one layer earlier: the adapter's {@code adaptLogical} declines to lower ANY
     * logical whose operand fails {@code producesComparisonResult}, and its own
     * {@code booleanOperandToken} names the boolean literal as that decline face
     * ({@code operandBooleanLiteral}) — so {@code tryEmitFromIR} is empty, {@code visitLogical}
     * falls to {@code super.visitLogical}, and the IR route reaches this seat's wrapper. The DATA_RULE
     * carriers cannot differ by route at all: {@code DataRuleGenerator} is constructed directly on
     * both routes (it is not an {@code IREmittableGenerator}). The ON ring is the corpus receipt
     * (drr/5.61.0 DATA_RULE 1648/0 on both routes, 0 of 275 rows differing); this test is the
     * FIXTURE receipt for the one shape that does take the IR route — the FUNCTION seat — rendered
     * through the real {@code IRGeneration.functionGenerator} seam with the provider on the
     * classpath. It runs only under {@code -Pir-on} (the same gate the D11 ON ring itself has), and
     * it asserts the provider is present so it cannot pass vacuously on the legacy path.
     *
     * <p><b>Measured:</b> {@code -Pir-on} 17 run / 0 F; the default profile 17 run / 0 F / 1 skipped
     * (this test); and the LAW-66 mutation under {@code -Pir-on} (the arm deleted, the delegation
     * kept) 17 run / 12 F = the eleven of the default-profile mutation <b>plus this test</b> — which
     * is also the experiment that settles the route question: were the IR route emitting the
     * logical natively, this test would be RED in BOTH states.
     */
    @Test
    @EnabledIf("irProviderOnClasspath")
    void a7_theFunctionSeatWrapsOnTheIrRouteToo() throws IOException {
        String out = lookup(renderOnIrRoute(), "functions/A5FunctionContext.java");
        assertContains(out, "ComparisonResult.ofNullSafe(MapperS.of(true))"
                + ".andNullSafe(ComparisonResult.ofNullSafe(MapperS.of(true)))");
        assertNotContains(out, "MapperS.of(true).andNullSafe(");
    }

    // =========================================================================
    // Part B — placement pins (GREEN in BOTH states: they pin what must NOT move)
    // =========================================================================

    /**
     * b1 — <b>THE LAW-69 TWIN.</b> The literal ARMS of a CONDITIONAL operand take
     * {@code ControlFlowHandler}'s hoist path, whose local literal check this seat replaces with the
     * delegation to the shared wrapper. The rendered bytes must be what they were: each arm
     * {@code ifThenElseResult = ComparisonResult.ofNullSafe(MapperS.of(<lit>));} EXACTLY ONCE, never
     * double-wrapped. GREEN before and after — and RED if the new arm is deleted while the
     * delegation stays, which is what makes it a pin on the delegation.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_conditionalOperandArmsStillWrapExactlyOnce() throws IOException {
        String out = datarule("ProbeB1ConditionalOperandArms.java");
        assertEquals(1, countOccurrences(out,
                "ifThenElseResult = ComparisonResult.ofNullSafe(MapperS.of(true));"),
                "the THEN arm wraps exactly once");
        assertEquals(1, countOccurrences(out,
                "ifThenElseResult = ComparisonResult.ofNullSafe(MapperS.of(false));"),
                "the ELSE arm wraps exactly once");
        assertNotContains(out, "ofNullSafe(ComparisonResult.ofNullSafe(");
        assertContains(out, "return ifThenElseResult.andNullSafe(exists(");
    }

    /**
     * b2 — a literal INSIDE a {@code default} operand is the default's value, not the logical
     * operand; the operand is the {@code RDefaultExpr}, which the #332 arm wraps ONCE around the
     * {@code getOrDefault} form. No double wrap, and the bare {@code true} stays the
     * {@code getOrDefault} argument.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_aLiteralInsideADefaultOperandIsNotTheOperand() throws IOException {
        String out = datarule("ProbeB2DefaultOperand.java");
        assertContains(out, ".getOrDefault(true))).andNullSafe(exists(");
        assertEquals(1, countOccurrences(out, "ComparisonResult.ofNullSafe("),
                "the default operand wraps exactly once; the literal inside it does not wrap");
        assertNotContains(out, "ofNullSafe(MapperS.of(true))");
    }

    /**
     * b3 — a literal as an EQUALITY operand ({@code flag = True}) is not a logical operand: the
     * logical operand is the equality, already a {@code ComparisonResult}. This is the dominant
     * corpus CODE shape (399 of the 652 source matches; 240 more are doc-string prose), and it must
     * stay exactly as it is.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_anEqualityOperandLiteralIsNotWrapped() throws IOException {
        String out = datarule("ProbeB3EqualityOperand.java");
        assertContains(out, "MapperS.of(true), CardinalityOperator.All).andNullSafe(exists(");
        assertNotContains(out, "ComparisonResult.ofNullSafe(");
    }

    /**
     * b4 — the wrap adds {@code ComparisonResult} to the operand refs; every DATA_RULE file already
     * imports it (the {@code executeDataRule} signature), so the import set is unchanged: exactly
     * one import line, no duplicate.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_theImportSetIsUnchanged() throws IOException {
        String out = datarule("ProbeA1BothLiterals.java");
        assertEquals(1, countOccurrences(out,
                "import com.rosetta.model.lib.expression.ComparisonResult;"));
        assertEquals(1, countOccurrences(out, "import com.rosetta.model.lib.mapper.MapperS;"));
    }

    // =========================================================================
    // Part C — corpus locks + the LAW-72 whole-cell controls (drr 5.61.0, the carriers' cell)
    // =========================================================================

    private static final Path DRR561_CELL_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path DRR561_GOLDEN_DIR =
            DRR561_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr561Available() {
        return Files.isDirectory(DRR561_CELL_ROOT) && Files.isDirectory(DRR561_GOLDEN_DIR);
    }

    private static final String DATARULE_DIR = "drr/regulation/cftc/rewrite/validation/datarule/";

    /** The 13 carriers — every operand-position site in the whole corpus (LAW 75 census). */
    private static final List<String> CARRIERS = List.of(
            "CFTCPart43TransactionReportFloatingRateResetFrequencyPeriodCond",
            "CFTCPart43TransactionReportIndexFactorCondition",
            "CFTCPart43TransactionReportNotionalAmountScheduleCondition",
            "CFTCPart43TransactionReportPaymentFrequencyPeriodCondition",
            "CFTCPart43TransactionReportSettlementCurrencyCondition",
            "CFTCPart45TransactionReportCDSIndexAttachmentPointCondition",
            "CFTCPart45TransactionReportCDSIndexDetachmentPointCondition",
            "CFTCPart45TransactionReportFixingDateCondition",
            "CFTCPart45TransactionReportFloatingRateResetFrequencyPeriodCond",
            "CFTCPart45TransactionReportIndexFactorCondition",
            "CFTCPart45TransactionReportNotionalAmountScheduleCondition",
            "CFTCPart45TransactionReportPaymentFrequencyPeriodCondition",
            "CFTCPart45TransactionReportSettlementCurrencyCondition");

    /** c1 — a WHOLE-FILE heal: both of its diff-lines were this seat's. */
    @Test
    @EnabledIf("drr561Available")
    void corpus_c1_part43IndexFactorCondition() throws IOException {
        lockDrr561(DATARULE_DIR + "CFTCPart43TransactionReportIndexFactorCondition.java");
    }

    /** c2 — the Part 45 twin. */
    @Test
    @EnabledIf("drr561Available")
    void corpus_c2_part45FixingDateCondition() throws IOException {
        lockDrr561(DATARULE_DIR + "CFTCPart45TransactionReportFixingDateCondition.java");
    }

    /** c3 — ALL 13 carriers, whole-file, in one test: the complete operand-position population. */
    @Test
    @EnabledIf("drr561Available")
    void corpus_c3_allThirteenCarriersAreWholeFileHeals() throws IOException {
        for (String carrier : CARRIERS) {
            lockDrr561(DATARULE_DIR + carrier + ".java");
        }
    }

    /**
     * control 0 — <b>THE ORACLE, read off GOLDEN.</b> Scanning the frozen drr 5.61.0 golden tree
     * with the very same scan the fork is held to, over every {@code .andNullSafe(…)} /
     * {@code .orNullSafe(…)} call site:
     * <ul>
     *   <li>ZERO Mapper-headed arguments and ZERO literal-Mapper receivers — golden never hands a
     *       raw Mapper to either method (it does not compile);</li>
     *   <li>the carrier form {@code ofNullSafe(MapperS.of(true)).andNullSafe(ComparisonResult
     *       .ofNullSafe(MapperS.of(true)))} appears in exactly 13 files;</li>
     *   <li>the call-site population is large (pinned as a floor) — the scan's own positive control
     *       (LAW 66): if the character walk ever stopped finding sites, this collapses first.</li>
     * </ul>
     */
    @Test
    @EnabledIf("drr561Available")
    void corpus_control0_goldenIsTheOracle() throws IOException {
        LogicalSeatScan golden = scan(readGoldenCell());
        assertEquals(List.of(), golden.mapperHeadedArguments,
                "golden was expected to hand NO raw Mapper argument to and/orNullSafe");
        assertEquals(List.of(), golden.literalMapperReceivers,
                "golden was expected to chain and/orNullSafe on NO raw literal Mapper");
        Set<String> expected = new LinkedHashSet<>();
        CARRIERS.forEach(c -> expected.add(DATARULE_DIR + c + ".java"));
        assertEquals(expected, golden.carrierFormFiles,
                "the frozen drr 5.61.0 golden tree has the carrier form in exactly the 13 CARRIERS —"
                + " the SET, not a count (the #591 review's SF-3): " + golden.carrierFormFiles);
        assertTrue(golden.callSites >= 1000,
                "the scan must see the and/orNullSafe population (golden drr 5.61.0 has thousands);"
                + " saw " + golden.callSites);
    }

    /**
     * control 1 — <b>LAW 72: the control's domain is the MECHANISM's reach.</b> Over the WHOLE
     * generated drr 5.61.0 cell — every kind, not the 13 carriers — no {@code and/orNullSafe}
     * argument may be Mapper-headed and no receiver may be a literal Mapper (golden's law, measured
     * in {@code control0}); and the 13 carriers must be REACHED in the carrier form, else "none
     * un-coerced" is vacuous. The fork's TOTAL call-site count is not pinned to golden's: this cell
     * still carries unrelated band mismatches.
     */
    @Test
    @EnabledIf("drr561Available")
    void corpus_control1_noUncoercedMapperOperandAcrossTheWholeCell() {
        assertNotNull(drr561Output, "drr 5.61.0 generation did not run — corpus unavailable?");
        LogicalSeatScan fork = scan(drr561Output);
        assertEquals(List.of(), fork.mapperHeadedArguments,
                "an and/orNullSafe ARGUMENT was emitted Mapper-headed. Both methods take ONLY a"
                + " ComparisonResult, so this does not compile.");
        assertEquals(List.of(), fork.literalMapperReceivers,
                "and/orNullSafe was chained on a raw literal Mapper. MapperS has no such method,"
                + " so this does not compile.");
        Set<String> expected = new LinkedHashSet<>();
        CARRIERS.forEach(c -> expected.add(DATARULE_DIR + c + ".java"));
        assertEquals(expected, fork.carrierFormFiles,
                "the carrier form must be reached in exactly the 13 carriers");
        assertTrue(fork.callSites >= 1000,
                "the scan must see the and/orNullSafe population; saw " + fork.callSites);
    }

    /**
     * control 2 — the neighbouring seat-15 heals in this same cell stay healed
     * ({@code ArgumentPositionRuleWrapSeatTest}'s four drr 5.61.0 whole-file heals, byte-identical
     * at the merged head; their bodies carry logical chains on the same operand-wrapping path).
     */
    @Test
    @EnabledIf("drr561Available")
    void corpus_control2_theNeighbouringSeatHealsStayHealed() throws IOException {
        lockDrr561("drr/regulation/esma/emir/refit/trade/functions/EUEMIRNotionalAmountPeriodLeg1.java");
        lockDrr561("drr/regulation/fca/ukemir/refit/trade/functions/UKEMIRNotionalAmountPeriodLeg2.java");
    }

    // =========================================================================
    // The whole-cell scan (a character walk over a code-only view — no regex)
    // =========================================================================

    /**
     * The logical-seat census of one tree: every {@code .andNullSafe(…)}/{@code .orNullSafe(…)}
     * call site; the two violation lists (a Mapper-headed ARGUMENT; a literal-Mapper RECEIVER
     * {@code MapperS.of(true|false).xxNullSafe}); and the files carrying the carrier form.
     */
    private static final class LogicalSeatScan {
        private int callSites;
        private final List<String> mapperHeadedArguments = new ArrayList<>();
        private final List<String> literalMapperReceivers = new ArrayList<>();
        private final Set<String> carrierFormFiles = new LinkedHashSet<>();
    }

    private static final String CARRIER_FORM =
            "ComparisonResult.ofNullSafe(MapperS.of(true))"
            + ".andNullSafe(ComparisonResult.ofNullSafe(MapperS.of(true)))";

    private static final List<String> LITERAL_RECEIVERS = List.of(
            "MapperS.of(true).andNullSafe(", "MapperS.of(false).andNullSafe(",
            "MapperS.of(true).orNullSafe(", "MapperS.of(false).orNullSafe(");

    /** Run the census over a path &rarr; Java-source map (fork output or golden tree). */
    private static LogicalSeatScan scan(Map<String, String> tree) {
        LogicalSeatScan r = new LogicalSeatScan();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(normalize(e.getValue()));
            for (String method : List.of(".andNullSafe(", ".orNullSafe(")) {
                int i = 0;
                while (true) {
                    i = code.indexOf(method, i);
                    if (i < 0) {
                        break;
                    }
                    int open = i + method.length() - 1;
                    int close = matchParen(code, open);
                    if (close < 0) {
                        break;
                    }
                    r.callSites++;
                    String arg = collapse(code.substring(open + 1, close));
                    if (arg.startsWith("MapperS.") || arg.startsWith("MapperC.")) {
                        r.mapperHeadedArguments.add(e.getKey() + " — " + arg);
                    }
                    i = open + 1;
                }
            }
            // A line-wrapped chain collapses to `MapperS.of(true) .andNullSafe(` — join the
            // member-access dot back onto its receiver so the receiver tokens below cannot be
            // dodged by a continuation line (the #591 review's SF-2). A literal replace, no regex.
            String collapsed = collapse(code).replace(" .", ".");
            for (String receiver : LITERAL_RECEIVERS) {
                if (collapsed.contains(receiver)) {
                    r.literalMapperReceivers.add(e.getKey() + " — " + receiver);
                }
            }
            if (collapsed.contains(CARRIER_FORM)) {
                r.carrierFormFiles.add(e.getKey());
            }
        }
        r.mapperHeadedArguments.sort(String::compareTo);
        r.literalMapperReceivers.sort(String::compareTo);
        return r;
    }

    /** The frozen drr 5.61.0 golden tree as a cell-relative path &rarr; source map. */
    private static Map<String, String> readGoldenCell() throws IOException {
        Map<String, String> tree = new LinkedHashMap<>();
        try (var stream = Files.walk(DRR561_GOLDEN_DIR)) {
            for (Path p : stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java")).toList()) {
                tree.put(DRR561_GOLDEN_DIR.relativize(p).toString().replace('\\', '/'),
                        Files.readString(p));
            }
        }
        assertTrue(tree.size() > 1000,
                "the frozen drr 5.61.0 golden tree looks truncated: " + tree.size() + " files");
        return tree;
    }

    /** Collapse every run of whitespace to one space — a character walk, not a pattern match. */
    private static String collapse(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        boolean pending = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                pending = sb.length() > 0;
            } else {
                if (pending) {
                    sb.append(' ');
                    pending = false;
                }
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * A CODE-ONLY view of generated Java: string literals, char literals and both comment forms
     * blanked to spaces, newlines preserved. A character walk, not a pattern match, and it performs
     * no structural analysis of the language content — only a literal-token scan of the fork's own
     * just-emitted output.
     */
    private static String codeOnly(String s) {
        char[] out = s.toCharArray();
        for (int i = 0; i < out.length; i++) {
            char c = out[i];
            if (c == '"' || c == '\'') {
                char quote = c;
                int j = i + 1;
                while (j < out.length && out[j] != quote) {
                    j += out[j] == '\\' ? 2 : 1;
                }
                for (int k = i; k <= Math.min(j, out.length - 1); k++) {
                    if (out[k] != '\n') {
                        out[k] = ' ';
                    }
                }
                i = j;
            } else if (c == '/' && i + 1 < out.length && out[i + 1] == '/') {
                int j = i;
                while (j < out.length && out[j] != '\n') {
                    out[j++] = ' ';
                }
                i = j;
            } else if (c == '/' && i + 1 < out.length && out[i + 1] == '*') {
                int j = i;
                while (j + 1 < out.length && !(out[j] == '*' && out[j + 1] == '/')) {
                    if (out[j] != '\n') {
                        out[j] = ' ';
                    }
                    j++;
                }
                if (j < out.length) {
                    out[j] = ' ';
                }
                if (j + 1 < out.length) {
                    out[j + 1] = ' ';
                }
                i = j + 1;
            }
        }
        return new String(out);
    }

    /** Index of the {@code ')'} matching the {@code '('} at {@code open}, or -1. */
    private static int matchParen(String s, int open) {
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')' && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

    private static int countOccurrences(String haystack, String needle) {
        int n = 0;
        int i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }

    // =========================================================================
    // Corpus harness
    // =========================================================================

    private static Map<String, String> drr561Output;
    private static List<String> drr561GenErrors = new ArrayList<>();

    @BeforeAll
    static void generateCell() throws IOException {
        if (drr561Available()) {
            List<String> errs = new ArrayList<>();
            drr561Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", DRR561_CELL_ROOT), errs);
            drr561GenErrors = errs;
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell,
            List<String> errors) throws IOException {
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
        var dataRuleGen = new DataRuleGenerator(gm, typeTranslator, typeUtil);
        var labelProviderGen = new LabelProviderGenerator(
                gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        Map<String, String> output = new LinkedHashMap<>();
        // #588/#589 (Copilot): every generator leg's per-model errors are aggregated, not
        // discarded — dropping them lets a byte-lock pass while unrelated work in the same cell
        // failed to generate, which is green for the wrong reason.
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                collect(errors, pojoGen.generateClasses(model, version, output));
                collect(errors, choiceGen.generateClasses(model, version, output));
                collect(errors, ruleGen.generateClasses(model, version, output));
                collect(errors, reportGen.generateClasses(model, version, output));
                collect(errors, dataRuleGen.generateClasses(model, version, output));
                collect(errors, labelProviderGen.generateClasses(model, version, output));
            }
        }
        collect(errors, funcGen.generateWithErrors(output));
        return output;
    }

    /** Aggregate one generator leg's reported errors; surfaced per locked file by {@code lock}. */
    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " — " + e));
        }
    }

    private static void lockDrr561(String path) throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 generation did not run — corpus unavailable?");
        List<String> lockedErrors = drr561GenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drr561Output.get(path);
        assertNotNull(generated, "not generated in drr 5.61.0: " + path);
        Path goldenPath = DRR561_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 5.61.0 output must byte-match golden (newline-normalized) for "
                + path + " — seat 19: a boolean literal operand of a logical and/or is coerced to a"
                + " ComparisonResult.");
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat19.rosetta");
            main.setVersion("0.0.0.test");
            List<RModel> models = new ArrayList<>();
            models.add(main);
            models.addAll(loadBuiltinsOnly());
            linking = RWorkspace.build(models);
            mainModel = main;
        }
    }

    private static Map<String, String> render(Predicate<RModel> filter) throws IOException {
        link();
        GeneratorModel gm = new GeneratorModel(linking.workspace(), filter);
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
        DataRuleGenerator dataRuleGen = new DataRuleGenerator(gm, tt, typeUtil);
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        dataRuleGen.generateClasses(mainModel, "1.0", out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        if (!errors.isEmpty()) {
            throw new AssertionError("fixture generation errors (a broken fixture"
                    + " must fail loudly, not skip): " + errors);
        }
        return out;
    }

    private static Map<String, String> fixture() throws IOException {
        if (fixtureOut == null) {
            fixtureOut = render(m -> "census.seat19".equals(m.namespace()));
        }
        return fixtureOut;
    }

    /** True only when the IR route's provider is on the test classpath (the {@code -Pir-on} profile). */
    static boolean irProviderOnClasspath() {
        try {
            Class.forName("com.regnosys.rosetta.generator.java.ir.IRGenerationProviderImpl");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * The fixture's FUNCTIONS rendered on the IR route: the flag set for the duration of the render
     * (restored after — the flag is re-read per call), the generator obtained through the REAL
     * {@code IRGeneration.functionGenerator} seam, and the provider asserted present so the render
     * cannot silently take the legacy path.
     */
    private static Map<String, String> renderOnIrRoute() throws IOException {
        link();
        String previous = System.getProperty(IRGeneration.PROPERTY);
        System.setProperty(IRGeneration.PROPERTY, "true");
        try {
            assertNotNull(IRGeneration.providerOrNull(),
                    "the IR provider must be resolvable under -Pir-on, else this is not an ON-route"
                    + " render");
            GeneratorModel gm = new GeneratorModel(linking.workspace(),
                    m -> "census.seat19".equals(m.namespace()));
            JavaTypeUtil typeUtil = new JavaTypeUtil();
            JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
            FunctionGenerator fg = IRGeneration.functionGenerator(gm, tt, typeUtil);
            assertTrue(!fg.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator, got " + fg.getClass());
            Map<String, String> out = new LinkedHashMap<>();
            List<String> errors = new ArrayList<>();
            fg.generateWithErrors(out)
                    .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
            if (!errors.isEmpty()) {
                throw new AssertionError("IR-route fixture generation errors: " + errors);
            }
            return out;
        } finally {
            if (previous == null) {
                System.clearProperty(IRGeneration.PROPERTY);
            } else {
                System.setProperty(IRGeneration.PROPERTY, previous);
            }
        }
    }

    private static String datarule(String fileName) throws IOException {
        return lookup(fixture(), "validation/datarule/" + fileName);
    }

    private static String function(String fileName) throws IOException {
        return lookup(fixture(), "functions/" + fileName);
    }

    private static String lookup(Map<String, String> output, String suffix) {
        return output.entrySet().stream()
                .filter(e -> e.getKey().endsWith("/" + suffix))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "'" + suffix + "' was not generated; keys=" + output.keySet()));
    }

    private static List<RModel> loadBuiltinsOnly() throws IOException {
        Map<String, Path> resolved = new LinkedHashMap<>();
        for (Path root : BUILTINS_SEARCH_ROOTS) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (var stream = Files.walk(root)) {
                stream.filter(p -> p.toString().endsWith(".rosetta"))
                      .forEach(p -> resolved.putIfAbsent(p.getFileName().toString(), p));
            }
        }
        List<String> failures = new ArrayList<>();
        List<RModel> models = new ArrayList<>();
        resolved.values().stream()
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .forEach(p -> {
                    try {
                        models.add(AstBuilder.buildFromFile(p));
                    } catch (Exception e) {
                        failures.add(p + " — " + e);
                    }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[BooleanLiteralOperandNullSafeSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static void assertContains(String out, String needle) {
        assertTrue(out.contains(needle),
                "expected needle missing:\n" + needle + "\n--- in output:\n" + out);
    }

    private static void assertNotContains(String out, String token) {
        assertTrue(!out.contains(token),
                "forbidden token present: " + token + "\n--- in output:\n" + out);
    }
}
