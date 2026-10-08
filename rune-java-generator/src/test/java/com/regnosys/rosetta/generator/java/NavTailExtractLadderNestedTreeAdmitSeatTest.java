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
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * SEAT 33, law F.B -- facet {@code navTailExtractLadderNestedTreeAdmit} (F12, sig B042):
 * the {@code nestedNavExtractLadderAdmit} (PR #379) NAV-TAIL transparency of
 * {@code CollectionHandler.isCleanLadderContext}'s {@code RExtractExpr} arm relaxes its
 * "the ladder's own subtree must be then/switch-free" scoping conjunct to the SAME
 * NESTED-TREE class the RULE-path bound-then arm below already admits (seat 32 law C3,
 * {@code condLadderThenConfinedToNestedTreeDrainableArms}) -- a rule-path ladder TREE
 * carrying a nested rung whose then/switch-carrying leaf arms are ALL plain arm-ROOT
 * drain-admissible chains hoists each arm chain INTO its owning rung (the #383 law), so
 * the depth-2 block renders IN PLACE inside the nav-tail wrapper. ONE predicate, TWO
 * consumers (LAW 69). The rule-scope guard is written EXPLICITLY at the new consult (the
 * sibling {@code fnArgTransparent} at the same arm writes it the same way): the
 * {@code RExtractExpr} arm is reached on BOTH the FUNCTION and the RULE path, while
 * {@code compileLadderConditionalBlock}'s own leaf check declines a nested tree whose
 * leaf arms carry a then unless {@code findEnclosingRule(cond) != null} -- the #341/#381
 * lockstep, and the reason the #382 catch cannot recur here.
 *
 * <p><b>GOLDEN &larr; FORK.</b> Golden drr 5.61.0 jfsa {@code NotionalLeg2Rule}:
 * {@code thenArg1.mapSingleToItem(trade -> MapperS.of(productForTrade.evaluate(trade.get()))
 * .<ContractualProduct>map(...).<EconomicTerms>map(...) .mapSingleToItem(item -> { final
 * Boolean boolean0 = ...; if ((boolean0 == null ? false : boolean0)) { return ...; } ...
 * final MapperC<FieldWithMetaNonNegativeQuantitySchedule> _thenArg0 = ...; final
 * MapperC<BigDecimal> _thenArg1 = _thenArg0.mapItem(...); return
 * MapperS.of(_thenArg1.get()); } ... return MapperS.<BigDecimal>ofNull(); }))} -- a 6-rung
 * block ladder rendered IN PLACE at depth 2, each hoist inside its owning rung. The fork
 * renders ONE inline {@code getOrDefault(false) ? ... : ... : MapperC.of()} ternary with
 * the basis arm left as a runtime {@code .then(_item -> ...)} chain and the
 * {@code bigInteger} / {@code ReferenceWithMeta*} locals floated to the lambda top.
 *
 * <p><b>CARRIER (drr 5.61.0 POJO):</b>
 * {@code drr/regulation/jfsa/rewrite/trade/reports/NotionalLeg2Rule.java} (B042, 35 diff
 * lines, ONE hunk). <b>WHOLE ceiling 1.</b>
 *
 * <p><b>THE A/B TWIN THAT MAKES THIS LAW NEARLY CERTAIN.</b> jfsa {@code NotionalLeg1Rule}
 * (same file, {@code regulation-jfsa-rewrite-trade-rule.rosetta:1466-1564}) is GREEN today
 * and is {@code NotionalLeg2}'s shape ({@code :1566-1626}) MINUS the {@code then extract
 * trade [ ProductForTrade(trade) -> contractualProduct -> economicTerms extract <ladder> ]}
 * wrapper -- one enclosing extract instead of two. Its golden carries EVERY consequence
 * {@code NotionalLeg2Rule}'s golden needs (per-rung {@code final Boolean booleanN}, the
 * in-arm {@code final BigInteger bigInteger}, the in-rung {@code MapperC} arm-chain pair,
 * the typed-empty terminal) and the fork emits all of it TODAY. This law has to move
 * exactly one {@code extractCount}. {@code e3} is that twin at fixture grain.
 *
 * <p><b>GREEN BLAST RADIUS -- MEASURED, BOTH ROUTES</b> (verdicts33-F section 2, the
 * {@code [P33-LADDEREXT]} anchor at the arm): {@code wouldSkip=true navTail=false} is
 * <b>2 rows / ONE {@code where=} ({@code rule:NotionalLeg2})</b> corpus-wide, OFF and ON
 * identical; the four GREEN drr 7.x {@code ExecutionAgent*} rules read
 * {@code confinedNested=false} over all 62 of their rows, so
 * {@code ladderHasNestedRung} inside the consulted predicate holds them out at this arm
 * exactly as it does at C3's; scout A's {@code fn:GetBasketConstituents} carrier cannot
 * move ({@code bodyKind=RConditionalExpr}/{@code RConstructorExpr}, {@code wouldSkip=false}
 * -- it fails {@code fin.body() instanceof RExtractExpr}, a conjunct this law does not
 * touch). Positive control for the arm: {@code navTail=true} = 16 rows (the #379
 * population -- Direction2Leg1 7 / FirstExerciseDate 5 / DirectionOfLeg1 3 /
 * CDEFirstExerciseDate 1), so a zero would have meant a mis-sited probe.
 * Golden-side, whole corpus: {@code getOrDefault(false) ?} occurs <b>0</b> times in all
 * 174,141 goldens (the #281 law, re-derived over all 25 cells).
 *
 * <p><b>LAW 74 (MEASURED, not analytic).</b> {@code javac32-report.md} section 7.4 row
 * <b>C22 {@code NotionalLeg2Rule} STANDING 3</b>: {@code cannot find symbol: method
 * getOrDefault(boolean)} on {@code class Boolean} (the ternary cond form the block hoists
 * into a {@code final Boolean} + {@code (x == null ? false : x)}), {@code cannot find
 * symbol: method then(...)} on {@code MapperC<FieldWithMetaNonNegativeQuantitySchedule>}
 * (the runtime {@code .then(} the block hoists into {@code _thenArg0}/{@code _thenArg1}),
 * and {@code cannot infer type-variable(s) F,T#2} (the {@code MapperC.of(MapperS.of(...))
 * : MapperC.of()} poles the block replaces with per-arm returns). All three are
 * consequences of the ternary and all three go with the block. <b>PRE 3 -&gt; POST 0.</b>
 * That row's LABEL in section 7.5.1 ("C.3-residue + B.3 + G.4") is a MIS-ATTRIBUTION
 * corrected by this seat: seat 33's {@code B3} anchor never prints
 * {@code parentKind=RThenExpr} (0 of 6,089 rows corpus-wide) and {@code rule:NotionalLeg2}'s
 * two rows there read {@code firstFail=valueSuppressed parentKind=RConditionalExpr}; G.4's
 * carrier cannot move (above). ONE law.
 *
 * <p><b>LAW 77 -- INHERITS.</b> {@code rune-ir-java} contains no re-implementation of
 * {@code isCleanLadderContext} / {@code walkLadderLevel} / {@code LadderLevel}
 * ({@code IRCollectionHandler extends CollectionHandler} with exactly ONE
 * {@code @Override}, {@code thenArgBaseName}); seat 32 measured the same walk at 302 rows
 * / 39 identical groups on BOTH routes, and the seat-33 probe reproduced every
 * {@code [P33-LADDEREXT]} group OFF and ON (2,062 rows each). {@code corpus_control2}
 * measures the carrier against GOLDEN on the IR route anyway.
 *
 * <p><b>CLAIMED RED at the seat base {@code fa49da010} (both routes) --- MEASURED BY THE
 * CHAIN:</b> {@code a1}, {@code a2}, {@code corpus_c1}, {@code corpus_control1} (+
 * {@code corpus_control2} on {@code -Pir-on}). {@code e1}, {@code e2} and {@code e3} are
 * GREEN at RED and must stay GREEN -- {@code e3} is this suite's own POSITIVE CONTROL: if
 * it fails at RED the fixture harness is wrong, not the law, and the fixture is RESHAPED
 * (never the assert weakened). <b>CLAIMED GREEN at the law head:</b> 8/0F/1skip default,
 * 8/0F/0skip {@code -Pir-on}.
 *
 * <p><b>MUTATION LANES (LAW 66/76) -- CLAIMED; the chain MEASURES them (LAW 82) and this
 * javadoc is rewritten FROM the lane logs before the commit:</b>
 * <ul>
 *   <li><b>m-lawFB-disjunct</b> (the whole new disjunct severed): {@code a1}, {@code a2},
 *       {@code corpus_c1}, {@code corpus_control1} (+ {@code corpus_control2}) fail;
 *       {@code e1}, {@code e2}, {@code e3} stay GREEN. NOT empty -- 2 measured rows.</li>
 *   <li><b>m-lawFB-nestedrung</b> ({@code ladderHasNestedRung} dropped from the CONSULTED
 *       predicate): {@code e1} fails, and so does
 *       {@code RuleThenArmLadderNestedTreeAdmitSeatTest.e1}. Seat 32 MEASURED
 *       {@code corpus_control3} (the sixteen drr 7.0.0 {@code ExecutionAgent*Rule.java})
 *       UNMOVED under this exact severance -- the arm's own {@code extractCount <= 1}
 *       return decides those four files -- so it is NOT re-claimed here.</li>
 *   <li><b>m-lawFB-rulescope</b> (the explicit {@code findEnclosingRule(cond) != null}
 *       conjunct dropped): {@code e2} is its intended witness. <b>DECLARED EXPECTATION:
 *       EMPTY.</b> At corpus grain the probe cannot score it -- both {@code wouldSkip=true
 *       navTail=false} rows read {@code encRule=true}, so no corpus row is held by the
 *       guard ALONE; and at fixture grain {@code compileLadderConditionalBlock}'s OWN
 *       {@code findEnclosingRule} gate declines the same tree independently, so the bytes
 *       may not move even in {@code e2}. It ships as an adjudicated-empty LOCKSTEP guard
 *       (LAW 82) -- honestly un-witnessed, never re-scored as load-bearing without a
 *       row.</li>
 *   <li><b>m-lawFB-leftcf</b> (the PRE-EXISTING {@code !subtreeHasControlFlow(tailExt
 *       .left())} conjunct dropped -- a conjunct this law does not author):
 *       {@code corpus_control1} must MOVE. All four carrier rows read {@code leftCF=false};
 *       score from the chain's log, not from this draft.</li>
 * </ul>
 *
 * <p><b>LAW 81 tripwires this heal fires in OTHER suites</b> -- EIGHT suites pin a
 * {@code NotionalLeg2Rule} residue row today, and every one of them moves when the file
 * goes whole. Re-pin each FROM ITS OWN PRINT, in this law's commit:
 * <ul>
 *   <li>{@code RuleThenArmLadderNestedTreeAdmitSeatTest:640-642} --
 *       {@code KNOWN_RESIDUE_DRR561} goes to {@code List.of()} (the carrier was that
 *       suite's SINGLE named residue and the pin of its MEASURED "NotionalLeg2Rule is NOT
 *       a C3 carrier" verdict); its {@code corpus_control2} gains the carrier and its
 *       class javadoc's "NOT a carrier / stays in corpus_control1's residue" paragraphs
 *       are corrected. <b>This suite is F.B's OWN assigned edit</b> (charter row 3).</li>
 *   <li>{@code BareFnCondHoistEveryContextSeatTest:433} --
 *       {@code fork=[1, 1, 0] golden=[4, 4, 0]} (row REMOVED).</li>
 *   <li>{@code BlockLambdaSingleItemChainStampSeatTest:841} --
 *       {@code fork=[2, 3, 1] golden=[2, 3, 2]} (row REMOVED).</li>
 *   <li>{@code ExplicitClosureParamNameBurnSeatTest:528} --
 *       {@code fork=[26, 2, 0] golden=[24, 2, 0]} (row REMOVED); its {@code :518} javadoc
 *       names C22 as "laws C.3 / B.3 / G.4" -- correct it to F.B.</li>
 *   <li>{@code InLambdaThenChainCtlAdmitSeatTest:309} -- the bare path in
 *       {@code KNOWN_THEN_CARRIERS_561} (row REMOVED); its {@code :301} comment files the
 *       carrier under F8/S27.</li>
 *   <li>{@code SetTerminalRuleMultiSeatTest:504} --
 *       {@code fork=[0, 1, 0] golden=[0, 6, 0]} (row REMOVED).</li>
 *   <li>{@code ThenWrappedDefaultSeatTest:493} --
 *       {@code fork=[18, 0, 6] golden=[18, 0, 5]} (row REMOVED).</li>
 *   <li>{@code WildcardLocalDeclSeatTest:631} --
 *       {@code fork=[0, 3, 0] golden=[0, 5, 0]} (row REMOVED).</li>
 * </ul>
 *
 * <p><b>LAW 82 - MEASURED by the receipts chain, run 1 at ce1a06292 (final33.status; every
 * figure below is transcribed from the chain's own logs, never from this file's earlier
 * CLAIMED paragraphs, which it supersedes).</b> GREEN 8/0F/1skip default (f33-green-default.log) /
 * 8/0F/0skip {@code -Pir-on} (f33-green-on.log); RED at the pre-seat base {@code fa49da010}: default
 * 4F = a1, a2, corpus_c1, corpus_control1; {@code -Pir-on} 5F = a1, a2, corpus_c1, corpus_control1, corpus_control2 (f33-red-{default,on}.log).
 * Mutation lanes ({@code mut33.py}, the default profile; {@code lanes33.py --summary}):
 * <ul>
 *   <li><b>{@code m-lawFB-disjunct}</b> ({@code CH_PAIRS_MUT_DISJUNCT}): MEASURED 19/5F/2skip = a1, a2, corpus_c1, corpus_control1, RuleThenArmLadderNestedTreeAdmitSeatTest.corpus_control1 - MATCH (a1, a2, c1, control1 + RuleThenArmLadderNestedTreeAdmit.corpus_control1).</li>
 *   <li><b>{@code m-lawFB-nestedrung}</b> ({@code CH_PAIRS_MUT_NESTEDRUNG}): MEASURED 19/2F/2skip = e1, RuleThenArmLadderNestedTreeAdmitSeatTest.e1 - MATCH (e1 + RuleThenArmLadderNestedTreeAdmit.e1; corpus_control3 held as seat 32 measured).</li>
 *   <li><b>{@code m-lawFB-rulescope}</b> ({@code CH_PAIRS_MUT_RULESCOPE}): MEASURED 8/1F/1skip = e2 - MATCH - e2 alone; the corpus half EMPTY as declared.</li>
 *   <li><b>{@code m-lawFB-leftcf}</b> ({@code CH_PAIRS_MUT_LEFTCF}): MEASURED 8/0F/1skip = (none) - RE-SCORED: EMPTY - the claim said corpus_control1 must MOVE; severing the pre-existing `!subtreeHasControlFlow(tailExt.left())` conjunct admits NO file at this corpus (every navTail row already reads leftCF=false; no green row is held by it alone) - a conjunct this law does not author, now recorded as defence-in-depth with its measured zero.</li>
 * </ul>
 */
class NavTailExtractLadderNestedTreeAdmitSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    static boolean irProviderOnClasspath() {
        try {
            Class.forName("com.regnosys.rosetta.generator.java.ir.IRGenerationProviderImpl");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /** The carrier (drr 5.61.0 POJO, jfsa). */
    private static final String NL2 =
            "drr/regulation/jfsa/rewrite/trade/reports/NotionalLeg2Rule.java";

    /** Cell A = drr 5.61.0 -- the carrier + the whole-cell UNION control. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A);
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    // =========================================================================
    // Fixtures -- reduced from the carrier's REAL source
    // =========================================================================

    /**
     * Reduced from {@code test-corpus/drr/drr-5.61.0/rosetta-source/src/main/rosetta/
     * regulation-jfsa-rewrite-trade-rule.rosetta}: {@code reporting rule NotionalLeg2}
     * ({@code :1566-1626}) and its GREEN twin {@code reporting rule NotionalLeg1}
     * ({@code :1466-1564}).
     *
     * <p><b>A1</b> = {@code NotionalLeg2}'s shape: {@code filter <p> then extract <bare-fn
     * conditional> then extract prod [ <invocation + nav spine> extract <ladder> ] then
     * <fn>} -- the SECOND enclosing extract whose lambda body ROOT is the inner
     * {@code RExtractExpr} is what makes {@code extractCount} 2 today. The ladder is the
     * NESTED-TREE class verbatim from the seat-32 {@code RuleThenArmLadderNestedTreeAdmit}
     * fixture (rung 1's arm is itself a conditional; the deepest leaf arm is the
     * drain-admissible chain {@code <nav> filter <pred> then extract <feature> then
     * only-element}), so the tree is the shape the consulted predicate admits and TWO bare
     * boolean FUNCTION-call conditions ({@code IsBig}, {@code IsSmall}) put the bool-hoist
     * numbering on {@code boolean0}/{@code boolean1} rather than {@code _boolean}.
     *
     * <p><b>E1</b> = the SAME wrapper with a FLAT ladder (no nested rung) whose arm IS
     * drain-admissible -- the wrapped form of the four GREEN drr 7.x
     * {@code ExecutionAgent*} rules. {@code ladderHasNestedRung} must leave it alone.
     *
     * <p><b>E2</b> = the SAME wrapped nested tree hosted in a FUNCTION. The explicit
     * rule-scope guard must keep it declined.
     *
     * <p><b>E3</b> = the UNWRAPPED twin ({@code NotionalLeg1}): ONE enclosing extract, so
     * the pre-law walk already returns {@code extractCount <= 1} and the block renders
     * TODAY. This suite's positive control -- the law ADDS an admit, it never removes one.
     */
    private static final String MODEL = """
            namespace census.seat33fb
            version "1.0.0"

            type Leg:
                qty number (0..1)
                unit string (0..1)

            type Terms:
                legs Leg (0..*)
                strike number (0..1)
                avg number (0..1)

            type Econ:
                terms Terms (0..1)

            type Prod:
                econ Econ (0..1)

            type Instr:
                before Prod (0..1)
                trade Prod (0..1)
                term boolean (0..1)

            func IsTerm:
                inputs:
                    i Instr (0..1)
                output:
                    r boolean (1..1)
                set r:
                    i -> term = True

            func BeforeFor:
                inputs:
                    i Instr (0..1)
                output:
                    p Prod (0..1)
                set p:
                    i -> before

            func TradeFor:
                inputs:
                    i Instr (0..1)
                output:
                    p Prod (0..1)
                set p:
                    i -> trade

            func EconFor:
                inputs:
                    p Prod (0..1)
                output:
                    e Econ (0..1)
                set e:
                    p -> econ

            func IsBig:
                inputs:
                    t Terms (0..1)
                output:
                    r boolean (1..1)
                set r:
                    t -> strike exists

            func IsSmall:
                inputs:
                    t Terms (0..1)
                output:
                    r boolean (1..1)
                set r:
                    t -> avg exists

            func TakeAmt:
                inputs:
                    t Terms (0..1)
                output:
                    r number (1..1)
                set r:
                    t -> strike

            func Spot:
                inputs:
                    t Terms (0..1)
                output:
                    r number (0..1)
                set r:
                    t -> avg

            func FormatNum:
                inputs:
                    n number (0..1)
                output:
                    r number (0..1)
                set r:
                    n

            func FnWrappedNestedTree:
                inputs:
                    i Instr (0..1)
                output:
                    r number (0..1)
                set r:
                    i extract instr [
                        EconFor(TradeFor(instr)) -> terms
                            extract
                                if IsBig(item)
                                then (if item -> strike exists
                                    then TakeAmt(item)
                                    else if item -> avg exists
                                    then (if Spot(item) exists
                                        then Spot(item)
                                        else (item -> legs
                                            filter unit exists
                                            then extract qty
                                            then only-element)))
                                else if IsSmall(item)
                                then TakeAmt(item)
                    ]

            reporting rule A1Wrapped from Instr: <"a1/a2 - THE drr 5.61.0 jfsa NotionalLeg2 SHAPE: a nested-tree rule-path ladder wrapped in a SECOND extract whose lambda body root is the inner extract (a pure nav-spine tail), which the pre-law nav-tail transparency declines because the ladder's own subtree carries a then">
                filter IsTerm
                then extract
                    if IsTerm
                    then BeforeFor
                    else TradeFor
                then extract prod [
                    EconFor(prod) -> terms
                        extract
                            if IsBig(item)
                            then (if item -> strike exists
                                then TakeAmt(item)
                                else if item -> avg exists
                                then (if Spot(item) exists
                                    then Spot(item)
                                    else (item -> legs
                                        filter unit exists
                                        then extract qty
                                        then only-element)))
                            else if IsSmall(item)
                            then TakeAmt(item)
                ]
                then FormatNum
                as "a1"

            reporting rule E1WrappedFlat from Instr: <"e1 - the WRAPPED form of the GREEN drr 7.x ExecutionAgent* FLAT class: the same second extract, a FLAT ladder with a drain-admissible arm and NO nested rung - ladderHasNestedRung must leave today's bytes alone">
                filter IsTerm
                then extract
                    if IsTerm
                    then BeforeFor
                    else TradeFor
                then extract prod [
                    EconFor(prod) -> terms
                        extract
                            if IsBig(item)
                            then (item -> legs
                                filter unit exists
                                then extract qty
                                then only-element)
                            else if IsSmall(item)
                            then TakeAmt(item)
                ]
                then FormatNum
                as "e1"

            reporting rule E3UnwrappedTwin from Instr: <"e3 - the GREEN jfsa NotionalLeg1 twin: the SAME nested tree with ONE enclosing extract instead of two, which the pre-law walk already admits - the positive control (GREEN at RED, GREEN after)">
                filter IsTerm
                then extract
                    if IsTerm
                    then BeforeFor
                    else TradeFor
                then extract EconFor -> terms
                then extract
                    if IsBig(item)
                    then (if item -> strike exists
                        then TakeAmt(item)
                        else if item -> avg exists
                        then (if Spot(item) exists
                            then Spot(item)
                            else (item -> legs
                                filter unit exists
                                then extract qty
                                then only-element)))
                    else if IsSmall(item)
                    then TakeAmt(item)
                then FormatNum
                as "e3"
            """;

    /**
     * a1 -- THE FLIP. The nav-tail-wrapped nested-tree rule-path ladder converts to
     * golden's block form: the inline ternary and every runtime {@code .then(} step are
     * GONE, and the per-rung bool hoist + the typed-empty terminal APPEAR.
     *
     * <p>PIN AT RED: the assert must fail on the LAW's token. {@code getOrDefault(false) ?}
     * is present iff the ladder stayed the inline ternary, which is exactly what the
     * declined {@code extractCount == 2} produces. If instead the fixture renders neither
     * form (no ladder at all, or a generation error), RESHAPE the fixture -- do NOT weaken
     * the assert. {@code e3} is the harness's own control for that failure mode.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_navTailWrappedNestedTreeLadderConvertsToTheBlockForm() throws IOException {
        String out = fixtureRule("A1WrappedRule");
        assertTrue(!out.contains("getOrDefault(false) ?"),
                "a1 must not keep the inline ladder ternary:\n" + out);
        assertContains(out, "final Boolean boolean0 = isBig.evaluate(");
        assertContains(out, "if ((boolean0 == null ? false : boolean0)) {");
        assertContains(out, "return MapperS.<BigDecimal>ofNull();");
    }

    /**
     * a2 -- the DEPTH-2 IN-PLACE half. The transparency's whole point is that the block
     * renders INSIDE the nav-tail wrapper's lambda, with each arm chain hoisted into its
     * OWNING rung rather than left as a runtime {@code .then(} step floated to the lambda
     * top (golden {@code NotionalLeg2Rule:206-211}, the {@code boolean2} rung).
     *
     * <p>The decl assertions are typed, not named: the in-rung pair escapes to
     * {@code _thenArg0}/{@code _thenArg1} here (the outer chain already owns
     * {@code thenArg0..2}) exactly as it does in the carrier, and the law's claim is about
     * PLACEMENT, so the indent comparison is the measurement.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_theWrappedLaddersArmChainHoistsIntoItsOwningRung() throws IOException {
        String out = fixtureRule("A1WrappedRule");
        assertTrue(!codeOnly(out).contains(".then("),
                "a2 must not keep the runtime `.then(` arm chain (golden hoists it in-rung):\n" + out);
        int rungIndent = indentOf(out, "final Boolean boolean0 = ");
        int armIndent = indentOf(out, "final MapperC<Leg> ");
        assertTrue(armIndent > rungIndent,
                "the arm chain must hoist INSIDE the owning rung (arm indent " + armIndent
                        + " must exceed the rung's bool-hoist indent " + rungIndent + "):\n" + out);
        assertContains(out, "final MapperC<BigDecimal> ");
    }

    /**
     * e1 -- THE DECLINE LOCK for {@code ladderHasNestedRung} at the NEW arm. A FLAT ladder
     * (no nested rung) whose arm IS drain-admissible walks to a perfectly valid
     * {@code LadderLevel}, so a nested-rung-free relax would have admitted it. It must keep
     * today's bytes: the inline ternary, the runtime {@code .then(} arm chain, and no
     * ladder bool-hoist numbering. The byte-grain twin is
     * {@code RuleThenArmLadderNestedTreeAdmitSeatTest.corpus_control3} (the sixteen drr
     * 7.0.0 {@code ExecutionAgent*Rule.java}), which this law's commit re-runs.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_wrappedFlatLadderWithADrainableArmKeepsTodaysBytes() throws IOException {
        String out = fixtureRule("E1WrappedFlatRule");
        assertContains(out, "getOrDefault(false) ?");
        assertTrue(codeOnly(out).contains(".then("),
                "e1 must keep the runtime `.then(` arm chain (today's bytes):\n" + out);
        assertTrue(!out.contains("final Boolean boolean0 = "),
                "e1 must NOT gain the ladder block's per-rung bool hoists:\n" + out);
    }

    /**
     * e2 -- THE DECLINE LOCK for the explicit rule-scope guard. The SAME wrapped nested
     * tree hosted in a FUNCTION must keep today's bytes. Without the guard the context arm
     * would admit it; the renderer's own {@code findEnclosingRule} gate would then decline
     * the block and the ladder would fall through to a DIFFERENT producer -- the #382
     * catch this guard exists to prevent.
     *
     * <p><b>DISCLOSED (LAW 82):</b> because the renderer declines independently, this test
     * may PASS under {@code m-lawFB-rulescope} -- i.e. the lane may measure EMPTY at
     * fixture grain as well as at corpus grain. The guard ships as an adjudicated-empty
     * LOCKSTEP guard; it is not re-scored as load-bearing without a row.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e2_functionHostedWrappedNestedTreeStaysDeclined() throws IOException {
        String out = fixtureFunction("FnWrappedNestedTree");
        assertContains(out, "getOrDefault(false) ?");
        assertTrue(!out.contains("final Boolean boolean0 = "),
                "e2 (FUNCTION host) must NOT gain the ladder block's per-rung bool hoists:\n" + out);
    }

    /**
     * e3 -- THE POSITIVE CONTROL and the A/B twin. The UNWRAPPED nested tree (ONE enclosing
     * extract -- the GREEN jfsa {@code NotionalLeg1} shape) already renders the block form
     * at the seat base and must keep rendering it: this law ADDS an admit at a second arm,
     * it never removes one. GREEN at RED. If it is RED at the base, the fixture harness is
     * wrong and the fixture is reshaped -- the assert is not weakened.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e3_unwrappedNestedTreeTwinKeepsItsBlockForm() throws IOException {
        String out = fixtureRule("E3UnwrappedTwinRule");
        assertTrue(!out.contains("getOrDefault(false) ?"),
                "e3 is the positive control: the SINGLE-extract nested tree already blocks at"
                        + " the seat base and must keep doing so:\n" + out);
        assertContains(out, "final Boolean boolean0 = isBig.evaluate(");
        assertContains(out, "if ((boolean0 == null ? false : boolean0)) {");
    }

    /** corpus_c1 -- {@code NotionalLeg2Rule} whole (B042-only, 35 diff lines, ONE hunk). */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_notionalLeg2RuleIsByteIdenticalToGolden() throws IOException {
        assertCarrierWhole(NL2, "B042-only");
    }

    /**
     * corpus_control1 -- LAW 79, the whole-cell UNION scan on drr 5.61.0. The domain is
     * every EMITTED file whose GOLDEN <b>or</b> FORK text carries a ladder-form token; per
     * file the (inline-ternary chains, block rung headers, bool hoists, thenArg decls,
     * typed-empty terminals) tuple must equal golden's, with NO named residue. This is the
     * control that catches the relax flipping any OTHER file in the cell -- over-fire (a
     * green file gains a block) and under-fire (the carrier keeps its ternary) both land
     * here. The tuple is the seat-32 law C3 tuple verbatim (LAW 69: one instrument, two
     * laws at the same seat), which is why its domain pin is transcribable.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr561WholeCellLadderFormEqualsGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 5.61.0 reported a generation error - the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR561, DOMAIN_DRR561);
    }

    /**
     * corpus_control2 -- LAW 77, the IR route vs GOLDEN. {@code isCleanLadderContext} has
     * no re-implementation in {@code rune-ir-java} and the seat-33 probe reproduced every
     * {@code [P33-LADDEREXT]} group on both routes, so the heal INHERITS; this measures it
     * rather than arguing it. Skips unless the IR provider is on the classpath
     * ({@code -Pir-on}).
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForTheCarrier() throws IOException {
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_A_ROOT),
                new ArrayList<>());
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(NL2))),
                normalize(irOut.get(NL2)), "IR route vs GOLDEN: " + NL2);
    }

    ///PIN: TRANSCRIBED (print-first, LAW 73) from
    ///PIN: RuleThenArmLadderNestedTreeAdmitSeatTest.corpus_control1's OWN measured print at
    ///PIN: seat 32 (C3-green1.log: "the union domain must equal the emitted token-bearing
    ///PIN: files (2932) ==> expected: <2932> but was: <1470>"). THIS control's scan tuple,
    ///PIN: cell and emitted-set restriction are that control's VERBATIM, so the domain is
    ///PIN: the same 1470. A read-only walk of the golden tree with the same five tokens
    ///PIN: (target/seat33-instruments/drafts33/FB/domain-derive.py) gives 2932 GOLDEN-side
    ///PIN: token bearers - the emitted-set retain is what halves it, which is exactly why
    ///PIN: the derived number is NOT the pin. This law moves NO file into or out of the
    ///PIN: domain (NotionalLeg2Rule's fork tuple [6, 0, 1, 3, 0] and golden tuple
    ///PIN: [0, 3, 4, 3, 1] are both non-zero, before and after). RE-PIN FROM THIS
    ///PIN: CONTROL'S OWN PRINT if it differs.
    private static final int DOMAIN_DRR561 = 1470;

    ///PIN: EMPTY BY THE LAW. At the seat base ONE drr 5.61.0 file differs in this tuple -
    ///PIN: the carrier, pinned as RuleThenArmLadderNestedTreeAdmitSeatTest's
    ///PIN: KNOWN_RESIDUE_DRR561 row ("...NotionalLeg2Rule.java fork=[6, 0, 1, 3, 0]
    ///PIN: golden=[0, 3, 4, 3, 1]"). This law flips it, so the residue is EMPTY here and
    ///PIN: that suite's row is deleted in the SAME commit. Every mutation lane MOVES this
    ///PIN: list; that is how the lanes are scored.
    private static final List<String> KNOWN_RESIDUE_DRR561 = List.of();

    // =========================================================================
    // Assertion helpers
    // =========================================================================

    private static void assertCarrierWhole(String path, String sigs) throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + path + ": " + own);
        String gen = drrAOutput.get(path);
        assertNotNull(gen, "not generated: " + path);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(path))), normalize(gen),
                path + " must be byte-identical to golden (" + sigs
                        + ") - seat 33 law F.B: the nav-tail extract admits the nested tree");
    }

    /** The leading-whitespace width of the first line containing {@code token}. */
    private static int indentOf(String out, String token) {
        for (String line : normalize(out).split("\n")) {
            if (line.contains(token)) {
                int i = 0;
                while (i < line.length()
                        && (line.charAt(i) == '\t' || line.charAt(i) == ' ')) {
                    i++;
                }
                return i;
            }
        }
        throw new AssertionError("token not rendered, cannot measure its indent: " + token
                + "\n" + out);
    }

    /**
     * (T1..T5) per file: inline ladder ternary chains ({@code getOrDefault(false) ?} -- the
     * #281 corpus law says ZERO goldens carry it), block rung headers
     * ({@code .getOrDefault(false)) {}), bool hoists ({@code final Boolean } decls),
     * {@code thenArg} decls, and typed-empty terminals ({@code ofNull();}). Exactly the
     * five counts a ladder inline&rarr;block restructure moves, and the five a green file
     * must not move at all. Verbatim from seat 32 law C3's control (LAW 69).
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String text = normalize(e.getValue());
            int[] t = new int[5];
            t[0] = count(text, "getOrDefault(false) ?");
            t[1] = count(text, ".getOrDefault(false)) {");
            t[4] = count(text, "ofNull();");
            for (String line : text.split("\n")) {
                String s = line.trim();
                if (s.startsWith("final Boolean ")) {
                    t[2]++;
                }
                if (s.startsWith("final Mapper") && s.contains(" thenArg")) {
                    t[3]++;
                }
            }
            if (t[0] + t[1] + t[2] + t[3] + t[4] > 0) {
                out.put(e.getKey(), t);
            }
        }
        return out;
    }

    private static int count(String s, String needle) {
        int n = 0;
        int from = 0;
        while ((from = s.indexOf(needle, from)) >= 0) {
            n++;
            from += needle.length();
        }
        return n;
    }

    private static Map<String, String> readGoldenTree(Path root) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        try (var stream = Files.walk(root)) {
            stream.filter(p -> p.toString().endsWith(".java")).sorted().forEach(p -> {
                try {
                    out.put(root.relativize(p).toString().replace('\\', '/'), Files.readString(p));
                } catch (IOException e) {
                    throw new AssertionError("golden read failed: " + p, e);
                }
            });
        }
        return out;
    }

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        List<String> mismatched = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        universe.retainAll(emittedA);
        int[] zero = new int[5];
        for (String key : universe) {
            int[] ac = a.getOrDefault(key, zero);
            int[] bc = b.getOrDefault(key, zero);
            if (!java.util.Arrays.equals(ac, bc)) {
                mismatched.add(key + " " + aName + "=" + java.util.Arrays.toString(ac)
                        + " " + bName + "=" + java.util.Arrays.toString(bc));
            }
        }
        // The domain-pin flow (LAW 73): the sentinel failure PRINTS the measured values so
        // the pin is transcribed from this assert's own output, never invented.
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - transcribe from this"
                        + " print: MEASURED domain=" + universe.size()
                        + " residue=" + mismatched);
        assertEquals(knownResidue, mismatched,
                "the ladder-form tuple differs beyond the named residue in " + mismatched.size()
                        + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted token-bearing files ("
                        + expectedDomain + ")");
    }

    /** String literals and comments stripped, so a `.then(` inside a doc string cannot lie. */
    private static String codeOnly(String java) {
        StringBuilder sb = new StringBuilder(java.length());
        int i = 0;
        int n = java.length();
        while (i < n) {
            char ch = java.charAt(i);
            if (ch == '"') {
                int j = i + 1;
                while (j < n && java.charAt(j) != '"') {
                    if (java.charAt(j) == '\\') {
                        j++;
                    }
                    j++;
                }
                i = j + 1;
            } else if (ch == '/' && i + 1 < n && java.charAt(i + 1) == '/') {
                while (i < n && java.charAt(i) != '\n') {
                    i++;
                }
            } else if (ch == '/' && i + 1 < n && java.charAt(i + 1) == '*') {
                int e = java.indexOf("*/", i + 2);
                i = e < 0 ? n : e + 2;
            } else {
                sb.append(ch);
                i++;
            }
        }
        return sb.toString();
    }

    // =========================================================================
    // Fixture harness (the RuleThenArmLadderNestedTreeAdmitSeatTest renderer, verbatim)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat33fb.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat33fb".equals(m.namespace()));
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
        RuleGenerator ruleGen = new RuleGenerator(gm, tt, fg);
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        ruleGen.generateClasses(main, "1.0", out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        rendered = new Render(out, errors);
        return rendered;
    }

    private static String fixtureRule(String ruleName) throws IOException {
        return fixtureFile(ruleName + ".java");
    }

    private static String fixtureFunction(String funcName) throws IOException {
        return fixtureFile(funcName + ".java");
    }

    private static String fixtureFile(String path) throws IOException {
        Render r = render();
        List<String> own = r.errors().stream().filter(e -> e.contains(path)).toList();
        assertTrue(own.isEmpty(), "the generator reported errors for " + path + ": " + own);
        String out = r.output().entrySet().stream()
                .filter(e -> e.getKey().endsWith(path))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
        assertNotNull(out, "not generated: " + path + " (have: " + r.output().keySet() + ")");
        return out;
    }

    private static void assertContains(String out, String token) {
        assertTrue(out.contains(token), "expected token missing:\n  " + token + "\nin:\n" + out);
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
                        failures.add(p + " - " + e);
                    }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[NavTailExtractLadderNestedTreeAdmitSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // Corpus harness (the ReceiverRenderTypingSeatTest cell generator, verbatim)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellAAvailable()) {
            List<String> errs = new ArrayList<>();
            drrAOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell,
            List<String> errors) throws IOException {
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
                collect(errors, ruleGen.generateClasses(model, version, output));
                collect(errors, reportGen.generateClasses(model, version, output));
            }
        }
        collect(errors, funcGen.generateWithErrors(output));
        return output;
    }

    private static Map<String, String> generateCellOnIrRoute(D11CorpusRegressionTest.CellSpec cell,
            List<String> errors) throws IOException {
        String previous = System.getProperty(IRGeneration.PROPERTY);
        System.setProperty(IRGeneration.PROPERTY, "true");
        try {
            assertNotNull(IRGeneration.providerOrNull(),
                    "the IR provider must be resolvable under -Pir-on");
            var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            var typeUtil = new JavaTypeUtil();
            var typeTranslator = new JavaTypeTranslator(typeUtil);
            FunctionGenerator funcGen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            assertTrue(!funcGen.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator");
            var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
            var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
            Map<String, String> output = new LinkedHashMap<>();
            for (RModel model : corpus.workspace().files()) {
                if (gm.shouldGenerate(model)) {
                    String version = gm.version(model);
                    collect(errors, ruleGen.generateClasses(model, version, output));
                    collect(errors, reportGen.generateClasses(model, version, output));
                }
            }
            collect(errors, funcGen.generateWithErrors(output));
            return output;
        } finally {
            if (previous == null) {
                System.clearProperty(IRGeneration.PROPERTY);
            } else {
                System.setProperty(IRGeneration.PROPERTY, previous);
            }
        }
    }

    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " - " + e));
        }
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

}
