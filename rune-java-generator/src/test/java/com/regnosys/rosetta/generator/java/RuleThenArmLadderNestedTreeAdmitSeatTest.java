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
 * SEAT 32, law C3 -- facet {@code ruleThenArmLadderNestedTreeAdmit} (F12/F7): the RULE-path
 * bound-then arm of {@code CollectionHandler.isCleanLadderContext} (facet
 * {@code inputFormThenHoist}, PR #357) relaxes its "the ladder's own subtree must be
 * then/switch-free" scoping conjunct to the NESTED-TREE class the block renderer already
 * admits at its {@code :11183} leaf check -- a rule-path ladder tree that carries a nested
 * rung AND whose then/switch-carrying leaf arms are all plain arm-ROOT drain-admissible
 * chains. Golden hoists each such arm chain INTO its OWNING rung (the #383 law), so
 * nothing splices between rungs and the bound seat stays a statement seat.
 *
 * <p><b>GOLDEN &larr; FORK.</b> Golden drr 5.61.0 cftc {@code NotionalAmountLeg1Rule}:
 * {@code _thenArg.mapSingleToItem(item -> { final Boolean boolean0 = …; if ((boolean0 ==
 * null ? false : boolean0)) { … final MapperC<…> thenArg0 = …; final MapperC<BigDecimal>
 * thenArg1 = thenArg0.mapItem(…); return MapperS.of(thenArg1.get()); } … return
 * MapperS.<BigDecimal>ofNull(); })} -- a 13-rung block ladder with per-rung bool hoists and
 * the arm chains hoisted in-rung. The fork renders ONE inline
 * {@code getOrDefault(false) ? … : …} ternary chain with every arm chain left as a runtime
 * {@code .then(} step and every meta hoist floated to the lambda top.
 *
 * <p><b>CARRIERS (drr 5.61.0 POJO, all four cftc):</b> {@code NotionalAmountLeg1Rule}
 * (B078, 110 diff lines), {@code NotionalAmountLeg2Rule} (B085, 44),
 * {@code NotionalCurrencyLeg2Rule} (B084, 40) and {@code NotionalCurrencyLeg1Rule}
 * (B001 B009 B076 B079, 108). <b>WHOLE ceiling 4.</b> The scout charter put
 * {@code NotionalCurrencyLeg1Rule}'s B009 on scout D's law; scout D's own probe REFUTED
 * that ({@code DEF} has ZERO rows for {@code rule:NotionalCurrencyLeg1} -- the rule carries
 * no {@code default}) and handed it back here. Reading golden settles it: the block form's
 * per-return coercion (line 167 {@code return MapperS.of(thenArg1.get()).<String>map("Type
 * coercion", …)}) is what types the outer decl {@code final MapperS<String> thenArg}
 * (B079) and what lets the consumer read {@code item.get()} plainly (B009, line 248), and
 * the in-rung meta-deref hoist is what mints {@code MapperS.<PriceSchedule>ofNull()} and so
 * the {@code import cdm.observable.asset.PriceSchedule} (B001). <b>All four sigs are
 * consequences of THIS admission -- the whole-file compare {@code corpus_c3} is the
 * measurement, and if it fails on B009/B079 alone the file is IMPROVED-not-whole and must
 * be disclosed as such, not re-scoped silently.</b> It DID fail, on the block arms'
 * WRAPPER-ITEM HOPS, and was disclosed as IMPROVED-not-whole (108 -&gt; 10 diff lines);
 * seat 33 law F.A (facet {@code blockArmWrapperHopDeref}) landed those hops and
 * {@code corpus_c3} is now the WHOLE-file compare.
 *
 * <p><b>{@code NotionalLeg2Rule} (jfsa 5.61.0, B077/B042) is NOT a carrier OF THIS LAW --
 * MEASURED.</b> Its {@code [P32-C3CTX]} arm row is {@code bound=true ownThenOrSwitch=true
 * confinedFlat=true extractCount=2}: even with this law's disjunct the early return is
 * {@code extractCount <= 1} = false, because a SECOND enclosing extract is counted -- the
 * {@code then extract trade [ ProductForTrade(trade) -> contractualProduct ->
 * economicTerms extract <ladder> ] } nav-tail wrapper. Seat 33 law F.B (facet
 * {@code navTailExtractLadderNestedTreeAdmit}) removes exactly that one count by
 * consulting the SAME predicate at the {@code RExtractExpr} nav-tail arm, and the file
 * goes WHOLE there -- so it has LEFT {@code corpus_control1}'s residue and
 * {@code KNOWN_RESIDUE_DRR561} is now empty. See
 * {@code NavTailExtractLadderNestedTreeAdmitSeatTest}.
 *
 * <p><b>THE SEAT-32 REQUIRED CONJUNCT.</b> The scout drafted the disjunct as
 * {@code walkLadderLevel(cond, 2) != null && ladderThenArmsAllPlainDrainableChains(lvl)}.
 * {@code walkLadderLevel} returns null ONLY when a nested THEN exceeds the budget, so a
 * FLAT ladder walks to a valid level -- and the probe measured the whole flat decline
 * population at this walk to be GREEN: four drr 7.x {@code ExecutionAgent*} rules, 31 of 31
 * rows {@code nestedTree=false allPlainDrainable=true rungs=2}, reduced from
 * {@code regulation-common-trade-party-rule.rosetta:24-43}. The drafted predicate would
 * have fired on all four. {@code ladderHasNestedRung(lvl)} is therefore a CONJUNCT of the
 * new predicate, {@code e1} is its fixture-grain decline lock and {@code corpus_control3}
 * its byte-grain one.
 *
 * <p><b>THE MEASUREMENT GAP, DISCLOSED.</b> The carriers have ZERO {@code [P32-C3NEST]} and
 * ZERO {@code [P32-C3BLOCK]} rows (the block renderer is never reached for them --
 * {@code isLadderConditional} declines first), so their OWN {@code nestedTree} /
 * {@code allPlainDrainable} are UNMEASURED by the probe. The law rests on them being true;
 * the whole-file compares below are the measurement. Structurally the source
 * ({@code regulation-cftc-rewrite-rule.rosetta:830-…}) shows both: rung 1's then-arm is
 * itself a conditional (nested), and the only then-carrying leaf arm is
 * {@code (<nav> filter <pred> then extract value then only-element)}.
 *
 * <p><b>LAW 74 (MEASURED, not analytic).</b> {@code javac32-report.md} section C22: the
 * fork's text for these files does NOT compile -- 3 errors, {@code cannot find symbol:
 * method getOrDefault(boolean)} on {@code class Boolean}, {@code cannot find symbol: method
 * then(…)} on {@code MapperC<FieldWithMetaNonNegativeQuantitySchedule>}, and
 * {@code cannot infer type-variable(s) F,T#2} at the {@code mapSingleToItem} seat. POST
 * (the golden block form) compiles clean. <b>That print is the MECHANISM-FAMILY witness, not
 * this law's own carrier</b> ({@code javac32-report.md} section 7.5.1): (a) section 7.3 scores
 * C.3 NOT WITNESSED -- none of its three carriers ({@code NotionalAmountLeg1Rule},
 * {@code NotionalAmountLeg2Rule}, {@code NotionalCurrencyLeg2Rule}) was transcribed into the
 * harness; (b) C22 {@code NotionalLeg2Rule} is NOT a carrier of this law (see the class javadoc
 * and the {@code KNOWN_RESIDUE_DRR561} pin) and sat in STANDING 3 at the seat-32 head -- its
 * fork text is UNMOVED by this law, and it is REPAIRED by seat 33 law F.B (C22 PRE 3 ->
 * POST 0; that seat also corrects section 7.5.1's "C.3-residue + B.3 + G.4" row LABEL, a
 * mis-attribution: seat 33's B3 anchor never prints {@code parentKind=RThenExpr} anywhere
 * in the corpus, and G.4's carrier fails a conjunct F.B does not touch);
 * (c) the whole-file claim rests on LAW 80 and
 * {@code corpus_c1}..{@code c4} / {@code control1}..{@code control3}, not on javac.
 *
 * <p><b>LAW 77 -- INHERITS.</b> {@code rune-ir-java} contains no re-implementation of
 * {@code isCleanLadderContext} / {@code isLadderConditional} /
 * {@code compileLadderConditionalBlock} / {@code walkLadderLevel} / {@code LadderLevel}
 * (grep over {@code rune-ir-java/src/main/java} = 0 hits); {@code IRCollectionHandler
 * extends CollectionHandler} with exactly ONE {@code @Override},
 * {@code thenArgBaseName(RThenExpr, GeneratorModel)}. {@code corpus_control2} measures the
 * four carriers against GOLDEN on the IR route.
 *
 * <p><b>CLAIMED RED at the seat base (both routes) --- MEASURED BY THE CHAIN:</b>
 * {@code a1}, {@code a2}, {@code corpus_c1}, {@code corpus_c2}, {@code corpus_c3},
 * {@code corpus_c4}, {@code corpus_control1} (+ {@code corpus_control2} on {@code -Pir-on}).
 * {@code e1}, {@code e2} and {@code corpus_control3} are GREEN at RED and must stay GREEN.
 * <b>CLAIMED GREEN at the law head:</b> 11/0F/1skip default, 11/0F/0skip {@code -Pir-on}.
 *
 * <p><b>MUTATION LANES (LAW 66/76) -- MEASURED (LAW 82) by the seat-32 chain, run 1 at
 * {@code d99ded920} ({@code f32-mut-m-lawC3-*.log}):</b>
 * <ul>
 *   <li><b>m-lawC3-nested</b> (the whole disjunct severed at the arm): MEASURED <b>11/7F/1S</b>
 *       = {@code a1}, {@code a2}, {@code corpus_c1..c4}, {@code corpus_control1} == the RED set,
 *       exactly as claimed; {@code e1}, {@code e2}, {@code corpus_control3} GREEN.</li>
 *   <li><b>m-lawC3-nestedrung</b> ({@code ladderHasNestedRung} dropped -- the seat-32
 *       conjunct): MEASURED <b>11/1F/1S = {@code e1}</b>; {@code corpus_control3} -- the sixteen
 *       {@code ExecutionAgent*Rule.java} of drr 7.0.0 -- stayed BYTE-IDENTICAL. <b>Re-scored:</b>
 *       the four flat green ladders the probe named reach this arm with {@code extractCount=2}
 *       (the probe's own {@code [P32-C3ANCHOR]} rows), and the arm returns
 *       {@code extractCount <= 1} = false for them whether or not the disjunct admits them --
 *       so the conjunct never decided those files; the commit message's "keeps the FLAT drr 7.x
 *       ExecutionAgent* ladders byte-frozen" attributed to the conjunct what the arm's own
 *       return does. The conjunct IS load-bearing at fixture grain: {@code e1}, a flat
 *       single-extract ladder with drainable arms, WOULD be admitted and re-rendered without it.
 *       For the corpus four it is defence-in-depth; the 31/31 {@code nestedTree=false} probe
 *       rows remain its reason to exist.</li>
 *   <li><b>m-lawC3-depth</b> ({@code walkLadderLevel(cond, 2)} -> {@code 1}): MEASURED
 *       <b>11/5F/1S</b> = {@code a1}, {@code a2}, {@code corpus_c1}, {@code corpus_c3},
 *       {@code corpus_control1}; {@code corpus_c2} ({@code NotionalAmountLeg2Rule}) and
 *       {@code corpus_c4} ({@code NotionalCurrencyLeg2Rule}) stayed WHOLE. <b>Re-scored:</b> the
 *       claim "the carriers nest TWO inner levels" is true of the Leg1 pair and of
 *       {@code NotionalCurrencyLeg1Rule} only; the Leg2 pair's ladders nest ONE inner level and
 *       heal at depth 1. The depth budget is load-bearing for three of the four, MEASURED.</li>
 *   <li><b>m-lawC3-depth3</b> ({@code 2} -> {@code 3}): MEASURED <b>11/1F/1S = {@code e2}</b> --
 *       the claim exactly, the asymmetry as designed.</li>
 *   <li><b>m-lawC3-plain</b> ({@code ladderThenArmsAllPlainDrainableChains} dropped): MEASURED
 *       <b>11/0F/1S -- EMPTY</b>: the adjudicated value this javadoc named in advance. The
 *       conjunct ships as a strictly-narrowing lockstep guard, honestly un-witnessed -- not
 *       re-scored as load-bearing without a row.</li>
 *   <li><b>m-lawC3-bound</b> (the {@code thenArgRefFor} conjunct dropped): MEASURED
 *       <b>11/0F/1S -- EMPTY</b>: the same adjudication -- an unbound rule-path then in this
 *       cell renders no differently under the lane; the conjunct is a lockstep guard,
 *       un-witnessed here.</li>
 * </ul>
 * RED at the chain's base {@code ddcdd151b}: {@code a1}, {@code a2}, {@code corpus_c1},
 * {@code corpus_c2}, {@code corpus_c3}, {@code corpus_c4}, {@code corpus_control1} (+
 * {@code corpus_control2} on {@code -Pir-on}); GREEN at the head 11/0F/1skip default,
 * 11/0F/0skip {@code -Pir-on}.
 *
 * <p><b>LAW 81 tripwires this heal WILL fire in OTHER suites</b> (each carrier row leaves
 * its pinned residue when the file goes whole; re-pin from each control's own print, in
 * this law's commit): {@code BareFnCondHoistEveryContextSeatTest} (4 rows),
 * {@code BlockLambdaSingleItemChainStampSeatTest} (4), {@code SetTerminalRuleMultiSeatTest}
 * (4), {@code ThenArgDeclKindFromCompiledSeatTest} (4),
 * {@code WildcardLocalDeclSeatTest} (4), {@code ThenWrappedDefaultSeatTest} (3),
 * {@code InLambdaThenChainCtlAdmitSeatTest} (4 of the 5 {@code KNOWN_THEN_CARRIERS_561}
 * entries at seat 32 -- jfsa {@code NotionalLeg2Rule} stayed; seat 33 law F.B removes the
 * fifth), {@code ChainMapperCRootRungsSeatTest}
 * (1), {@code DispatchBaseInputDeclineSeatTest} (1),
 * {@code FilterPredicateMetaDerefSeatTest} (1),
 * {@code IteElseArmRecoveredMetaDerefSeatTest} (1),
 * {@code RuleCallArmMetaWrapSeatTest} (1) and
 * {@code WrapperLadderKeepsCollapseSeatTest} (1).
 */
class RuleThenArmLadderNestedTreeAdmitSeatTest {

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

    // The four carriers (drr 5.61.0 POJO, cftc).
    private static final String AMT1 =
            "drr/regulation/cftc/rewrite/reports/NotionalAmountLeg1Rule.java";
    private static final String AMT2 =
            "drr/regulation/cftc/rewrite/reports/NotionalAmountLeg2Rule.java";
    private static final String CCY1 =
            "drr/regulation/cftc/rewrite/reports/NotionalCurrencyLeg1Rule.java";
    private static final String CCY2 =
            "drr/regulation/cftc/rewrite/reports/NotionalCurrencyLeg2Rule.java";

    /**
     * Seat 33 law F.B's carrier ({@code navTailExtractLadderNestedTreeAdmit}) -- NOT a
     * carrier of this law, but it goes WHOLE in the same cell, so this suite's IR-route
     * control measures it too (LAW 77, one cell generation already paid for). Its own
     * fixture-grain and byte-grain locks live in
     * {@code NavTailExtractLadderNestedTreeAdmitSeatTest}.
     */
    private static final String NL2 =
            "drr/regulation/jfsa/rewrite/trade/reports/NotionalLeg2Rule.java";

    /** Cell A = drr 5.61.0 -- all four carriers + the whole-cell UNION control. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");

    /** Cell B = drr 7.0.0 -- the GREEN {@code ExecutionAgent*} flat-ladder decline lock. */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A);
    }

    static boolean cellBAvailable() {
        return Files.isDirectory(GOLDEN_B);
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    // =========================================================================
    // Fixtures -- reduced from the carriers' REAL source
    // =========================================================================

    /**
     * A1/A2 = the drr 5.61.0 {@code regulation-cftc-rewrite-rule.rosetta:830-…}
     * {@code NotionalAmountLeg1} shape, reduced to two outer rungs: a bracket extract whose
     * base is a bare-fn conditional then-chain ({@code if IsActionTypeTERM then
     * BeforeTradeForEvent else TradeForEvent}) feeding {@code then extract <ladder>}, where
     * rung 1's arm is itself a conditional (the nested tree) whose deepest else is the
     * drain-admissible chain {@code <nav> filter <pred> then extract <feature> then
     * only-element} -- the exact arm golden hoists as {@code thenArg0}/{@code thenArg1}
     * inside the rung.
     *
     * <p>E1 = the SAME shape with the nested-conditional arm removed, i.e. the GREEN drr
     * 7.x {@code ExecutionAgent*} class of
     * {@code regulation-common-trade-party-rule.rosetta:24-43}: two rungs, one
     * drain-admissible then-chain arm, NO nested rung. The added
     * {@code ladderHasNestedRung} conjunct must leave it alone.
     *
     * <p>E2 = a nested tree ONE level beyond the evidence-capped budget (three inner
     * levels): {@code walkLadderLevel(cond, 2)} returns null, so the disjunct declines and
     * the lockstep with the renderer's own budget holds.
     */
    private static final String MODEL = """
            namespace census.seat32c3
            version "1.0.0"

            type Leg:
                qty number (0..1)
                unit string (0..1)

            type Prod:
                legs Leg (0..*)
                strike number (0..1)
                avg number (0..1)

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

            func IsBig:
                inputs:
                    p Prod (0..1)
                output:
                    r boolean (1..1)
                set r:
                    p -> strike exists

            func IsSmall:
                inputs:
                    p Prod (0..1)
                output:
                    r boolean (1..1)
                set r:
                    p -> avg exists

            func TakeAmt:
                inputs:
                    p Prod (0..1)
                output:
                    r number (1..1)
                set r:
                    p -> strike

            func Spot:
                inputs:
                    p Prod (0..1)
                output:
                    r number (0..1)
                set r:
                    p -> avg

            reporting rule A1NestedTree from Instr: <"a1 - THE drr 5.61.0 cftc NotionalAmountLeg1 SHAPE: a bracket extract whose bare-fn conditional then-chain feeds a NESTED-TREE else-if ladder whose deepest arm is a drain-admissible chain">
                extract instr [
                    if IsTerm
                    then BeforeFor
                    else TradeFor
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
                ]
                as "a1"

            reporting rule E1FlatLadder from Instr: <"e1 - the GREEN drr 7.x ExecutionAgent* FLAT class: two rungs, a drain-admissible then-chain arm, NO nested rung - the ladderHasNestedRung conjunct must leave today's bytes alone">
                extract instr [
                    if IsTerm
                    then BeforeFor
                    else TradeFor
                        then extract
                            if IsBig(item)
                            then (item -> legs
                                filter unit exists
                                then extract qty
                                then only-element)
                            else if IsSmall(item)
                            then TakeAmt(item)
                ]
                as "e1"

            reporting rule E2DeepTree from Instr: <"e2 - THREE inner levels, one beyond the evidence-capped budget: walkLadderLevel returns null and the disjunct declines (the lockstep with the renderer's own walk)">
                extract instr [
                    if IsTerm
                    then BeforeFor
                    else TradeFor
                        then extract
                            if IsBig(item)
                            then (if item -> strike exists
                                then (if item -> avg exists
                                    then (if Spot(item) exists
                                        then Spot(item)
                                        else (item -> legs
                                            filter unit exists
                                            then extract qty
                                            then only-element))
                                    else TakeAmt(item))
                                else TakeAmt(item))
                            else if IsSmall(item)
                            then TakeAmt(item)
                ]
                as "e2"
            """;

    /**
     * a1 -- the flip: the nested-tree rule-path ladder converts to golden's block form. The
     * inline ternary and every runtime {@code .then(} step must be GONE, and the per-rung
     * bool hoist + the typed-empty terminal must APPEAR.
     *
     * <p>PIN AT RED: if the fixture does not render {@code boolean0} (the pre-scan numbers
     * {@code _boolean} when a tree carries exactly ONE bare boolean FUNCTION-call
     * condition, {@code boolean0..n-1} at two or more -- this fixture has two,
     * {@code IsBig} and {@code IsSmall}), reshape the fixture. Do NOT weaken the assert.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_nestedTreeRuleThenLadderConvertsToTheBlockForm() throws IOException {
        String out = fixtureRule("A1NestedTreeRule");
        assertTrue(!out.contains("getOrDefault(false) ?"),
                "a1 must not keep the inline ladder ternary:\n" + out);
        assertTrue(!codeOnly(out).contains(".then("),
                "a1 must not keep the runtime `.then(` arm chain:\n" + out);
        assertContains(out, "final Boolean boolean0 = isBig.evaluate(");
        assertContains(out, "if ((boolean0 == null ? false : boolean0)) {");
        assertContains(out, "return MapperS.<BigDecimal>ofNull();");
    }

    /**
     * a2 -- the #383 half of the same flip: the drain-admissible arm chain hoists INTO its
     * OWNING rung, not to the lambda top. The structural half (the decl is indented deeper
     * than the rung's own bool hoist) is what the byte compares would otherwise only see at
     * whole grain.
     *
     * <p>PIN AT RED on the {@code thenArg0}/{@code thenArg1} numbering: rule-path hoist
     * naming is per-scope literal, so the in-rung pair starts at 0 exactly as golden
     * {@code NotionalCurrencyLeg1Rule} lines 163-167 do.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_nestedTreeLadderArmChainHoistsIntoItsOwningRung() throws IOException {
        String out = fixtureRule("A1NestedTreeRule");
        assertContains(out, "final MapperC<Leg> thenArg0 = ");
        assertContains(out, "final MapperC<BigDecimal> thenArg1 = ");
        assertContains(out, "return MapperS.of(thenArg1.get());");
        int rungIndent = indentOf(out, "final Boolean boolean0 = ");
        int armIndent = indentOf(out, "final MapperC<Leg> thenArg0 = ");
        assertTrue(armIndent > rungIndent,
                "the arm chain must hoist INSIDE the owning rung (arm indent " + armIndent
                        + " must exceed the rung's bool-hoist indent " + rungIndent + "):\n" + out);
    }

    /**
     * e1 -- THE DECLINE LOCK for the seat-32 required conjunct. A FLAT ladder (no nested
     * rung) whose arm IS drain-admissible walks to a perfectly valid {@code LadderLevel},
     * so the scout's drafted predicate would have admitted it. It must keep today's bytes:
     * the inline ternary, the runtime {@code .then(} arm chain, and NO ladder bool-hoist
     * numbering.
     *
     * <p>The byte-grain twin of this lock is {@code corpus_control3}: the four GREEN drr
     * 7.x {@code ExecutionAgent*} rules the probe actually measured (31/31 rows
     * {@code nestedTree=false}) must stay byte-identical to golden.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_flatLadderWithADrainableArmKeepsTodaysBytes() throws IOException {
        String out = fixtureRule("E1FlatLadderRule");
        assertContains(out, "getOrDefault(false) ?");
        assertTrue(codeOnly(out).contains(".then("),
                "e1 must keep the runtime `.then(` arm chain (today's bytes):\n" + out);
        assertTrue(!out.contains("final Boolean boolean0 = "),
                "e1 must NOT gain the ladder block's per-rung bool hoists:\n" + out);
    }

    /**
     * e2 -- the depth-cap lock. Three inner levels exceed the evidence-capped budget the
     * block renderer itself walks at, so {@code walkLadderLevel(cond, 2)} returns null and
     * the disjunct declines. Widening the predicate's budget past the renderer's would
     * break the lockstep and hand the tree to the nested-then gate instead.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e2_treeBeyondTheDepthCapStaysDeclined() throws IOException {
        String out = fixtureRule("E2DeepTreeRule");
        assertContains(out, "getOrDefault(false) ?");
        assertTrue(!out.contains("final Boolean boolean0 = "),
                "e2 must NOT gain the ladder block's per-rung bool hoists:\n" + out);
    }

    /** corpus_c1 -- {@code NotionalAmountLeg1Rule} whole (B078-only, 110 diff lines). */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_notionalAmountLeg1RuleIsByteIdenticalToGolden() throws IOException {
        assertCarrierWhole(AMT1, "B078-only");
    }

    /** corpus_c2 -- {@code NotionalAmountLeg2Rule} whole (B085-only, 44 diff lines). */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c2_notionalAmountLeg2RuleIsByteIdenticalToGolden() throws IOException {
        assertCarrierWhole(AMT2, "B085-only");
    }

    /**
     * corpus_c3 -- {@code NotionalCurrencyLeg1Rule} WHOLE. Seat 32 landed the ladder BLOCK
     * here (108 -> 10 diff lines) and DISCLOSED the exact residue as IMPROVED-not-whole
     * (LAW 80, not re-scoped): the block arms' WRAPPER-ITEM HOPS -- 7 golden-only lines (the
     * {@code PriceSchedule} import + three hoist/deref pairs) and 3 fork-only lines, every
     * fork-only line navigating through {@code ReferenceWithMetaPriceSchedule}. Its javadoc
     * said "when the hop law lands, THIS TEST FAILS and becomes
     * {@code assertCarrierWhole(CCY1, ...)} in that commit" -- <b>seat 33 law F.A (facet
     * {@code blockArmWrapperHopDeref}) is that law and this is that commit.</b> Its two rungs
     * ({@code collapsedMetaDerefArmAdmit} at the collapse hop, {@code
     * ruleCalleeExplicitArgMetaDeref} at the two explicit-args rule calls) land TOGETHER,
     * because golden numbers all three hoists {@code referenceWithMetaPriceSchedule0/1/2} in
     * ONE deferred-coercion name group.
     *
     * <p><b>The seat-32 disclosure's attribution was WRONG and is corrected here.</b> It filed
     * the residue under "the null-typed receiver family (law A.2's binder / S33)"; A.2's landed
     * rung types a LAMBDA ITEM receiver ({@code CollectionHandler:948-954}) and NEITHER hop has
     * one -- the first is an {@code only-element} collapse receiver, the other two are explicit
     * call arguments. Seat 32's own {@code javac32-report.md} section 7.5.2 already said so
     * ("that is C20's shape letter for letter"), so the two seat-32 surfaces disagreed with
     * each other and the javac harness was the one that was right. Seat 33 measured the
     * producers directly: {@code NavigationHandler:756-759} ({@code verdict=decline:noDrain},
     * 4 rows / 2 wheres, both routes) and {@code ReferenceHandler:907} ({@code calleeFn=-},
     * 8 rows / 2 wheres, both routes). Fixture-grain and mutation-grain locks live in
     * {@code BlockArmWrapperHopDerefSeatTest}.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c3_notionalCurrencyLeg1RuleIsByteIdenticalToGolden() throws IOException {
        assertCarrierWhole(CCY1, "B001 B009 B076 B079 + the seat-33 law F.A wrapper hops");
    }

    /** corpus_c4 -- {@code NotionalCurrencyLeg2Rule} whole (B084-only, 40 diff lines). */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c4_notionalCurrencyLeg2RuleIsByteIdenticalToGolden() throws IOException {
        assertCarrierWhole(CCY2, "B084-only");
    }

    /**
     * corpus_control1 -- LAW 79, the whole-cell UNION scan on drr 5.61.0. The domain is
     * every file whose GOLDEN <b>or</b> FORK text carries a ladder-form token; per file the
     * (inline-ternary chains, block rung headers, bool hoists, thenArg decls, typed-empty
     * terminals) tuple must equal golden's beyond the NAMED residue. This is the control
     * that catches the relax flipping any OTHER file in the cell -- over-fire (a green file
     * gains a block) and under-fire (a carrier keeps its ternary) both land here.
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
     * corpus_control2 -- LAW 77, BOTH-ROUTES-vs-GOLDEN. The IR route inherits the
     * {@code CollectionHandler} ladder seats (no re-implementation in
     * {@code rune-ir-java}); all four carriers must render golden-identical there too.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForTheFourCarriers() throws IOException {
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_A_ROOT),
                new ArrayList<>());
        for (String carrier : List.of(AMT1, AMT2, CCY2, NL2)) {
            assertEquals(normalize(Files.readString(GOLDEN_A.resolve(carrier))),
                    normalize(irOut.get(carrier)), "IR route vs GOLDEN: " + carrier);
        }
        // CCY1 goes WHOLE at seat 33 law F.A (blockArmWrapperHopDeref), so the IR route must
        // now render GOLDEN for it too, not merely agree with the legacy route. LAW 77
        // measured INHERITS for both of that law's rungs ([P33-IRCOLL] feature=unit
        // onlyElem=true = 0 of 1,033 rows, against a live positive control of onlyElem=true
        // = 3,515 over 44 groups; and [P33-COLLDEREF] prints the carrier rows on the ON
        // route field-for-field), so no IR twin ships and this is its byte-grain check.
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(CCY1))),
                normalize(irOut.get(CCY1)), "IR route vs GOLDEN: " + CCY1);
    }

    /**
     * corpus_control3 -- THE LOAD-BEARING DECLINE LOCK (LAW 79, a cell the law must not
     * touch at all). Every {@code ExecutionAgent*Rule.java} the drr 7.0.0 cell emits must
     * stay byte-identical to golden. Four of these rules are the probe's measured flat
     * decline population at this exact walk ({@code ExecutionAgentOfCounterparty1DTCC} 11
     * rows, {@code ExecutionAgentOfTheCounterparty1DTCC} 10,
     * {@code ExecutionAgentCounterparty1} 9, {@code ExecutionAgentOfCounterparty1} 1 -- all
     * {@code extractCount=2 crossedRuleThen=true nestedTree=false allPlainDrainable=true});
     * the probe's {@code where=rule:<name>} does not disambiguate the namespaces those
     * names live in, so the lock is taken over the WHOLE basename family rather than four
     * guessed paths.
     *
     * <p>{@code EXECUTION_AGENT_FILES} is MEASURED by a read-only walk of the golden tree
     * ({@code target/seat32-instruments/drafts32/C3/domain-derive.py}) and pinned so the
     * domain cannot silently shrink to zero and read as a pass.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_drr700ExecutionAgentFlatLaddersAreUnmoved() throws IOException {
        assertNotNull(drrBOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrBGenErrors.stream()
                        .filter(e -> e.contains("ExecutionAgent")).toList(),
                "drr 7.0.0 reported a generation error on the decline-lock family");
        List<String> checked = new ArrayList<>();
        for (Map.Entry<String, String> e : drrBOutput.entrySet()) {
            String key = e.getKey();
            String base = key.substring(key.lastIndexOf('/') + 1);
            if (!base.startsWith("ExecutionAgent") || !base.endsWith("Rule.java")) {
                continue;
            }
            checked.add(key);
            assertEquals(normalize(Files.readString(GOLDEN_B.resolve(key))),
                    normalize(e.getValue()),
                    "the GREEN flat-ladder decline moved: " + key
                            + " - the ladderHasNestedRung conjunct is not holding");
        }
        assertEquals(EXECUTION_AGENT_FILES, checked.size(),
                "the decline-lock domain must be the pinned " + EXECUTION_AGENT_FILES
                        + " ExecutionAgent*Rule.java files, measured " + checked.size()
                        + " (transcribe from this print if the corpus moved): " + checked);
    }

    ///PIN: MEASURED by the read-only walk
    ///PIN: `python target/seat32-instruments/drafts32/C3/domain-derive.py` over
    ///PIN: test-corpus/drr/drr-7.0.0/rosetta-source/src/generated/java at ddcdd151b.
    private static final int EXECUTION_AGENT_FILES = 16;

    ///PIN: DERIVED (print-first, LAW 73): the drr 5.61.0 golden files carrying at least one
    ///PIN: of the five tuple tokens, restricted to the namespaces the cell's
    ///PIN: rosetta-config.yml declares (drr.*, com.rosetta.model) == the emissionFilter
    ///PIN: accept-list. Measured 2932 by domain-derive.py at ddcdd151b. The union also
    ///PIN: admits FORK-only token bearers; none exist in this cell at this head (every
    ///PIN: differing band file already carries a non-zero GOLDEN tuple), so the derived
    ///PIN: number IS the expected union. RE-PIN FROM THIS CONTROL'S OWN PRINT if it differs.
    ///PIN: MEASURED at C3-green1.log ("the union domain must equal the emitted token-bearing files
    ///PIN: (2932) ==> expected: <2932> but was: <1470>"): the drafter's domain-derive.py walked the
    ///PIN: golden tree with a wider token set than this control's scan; the control's own print is
    ///PIN: the authority (print-first) - transcribed.
    private static final int DOMAIN_DRR561 = 1470;

    ///PIN: MEASURED from the seat-31 final OFF dump (gen vs golden, every drr 5.61.0 band
    ///PIN: file) by domain-derive.py --residue. At the seat-32 base FIVE files differed:
    ///PIN: the four carriers plus jfsa NotionalLeg2Rule. The four carriers LEFT this list
    ///PIN: when seat 32 law C.3 landed; NotionalLeg2Rule stayed, and that was the pin of
    ///PIN: the MEASURED verdict that it is NOT a C.3 carrier (its arm row is
    ///PIN: extractCount=2, so the early return is false with or without C.3's disjunct -
    ///PIN: byte-neutral either way).
    ///PIN: RE-PINNED TO EMPTY at seat 33 law F.B (navTailExtractLadderNestedTreeAdmit),
    ///PIN: which removes the SECOND extract's count at the nav-tail arm: the file goes
    ///PIN: WHOLE and its row ("drr/.../NotionalLeg2Rule.java fork=[6, 0, 1, 3, 0]
    ///PIN: golden=[0, 3, 4, 3, 1]") leaves this list. DOMAIN_DRR561 is UNMOVED - both
    ///PIN: tuples are non-zero, so no file enters or leaves the union.
    ///PIN: Every mutation lane MOVES this list; that is how the lanes are scored.
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
                        + ") - seat 32 law C3: the nested-tree rule-then ladder admits");
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
     * #281 corpus law says ZERO goldens carry it, independently re-derived at this head by
     * domain-derive.py), block rung headers ({@code .getOrDefault(false)) {}), bool hoists
     * ({@code final Boolean } decls), {@code thenArg} decls, and typed-empty terminals
     * ({@code ofNull();}). Exactly the five counts a ladder inline&rarr;block restructure
     * moves, and the five a green file must not move at all.
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
    // Fixture harness (the InLambdaBoolHoistShadowSeatTest rule renderer, verbatim)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat32c3.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat32c3".equals(m.namespace()));
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
        Render r = render();
        String path = ruleName + ".java";
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
            throw new AssertionError("[RuleThenArmLadderNestedTreeAdmitSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // Corpus harness (the ReceiverRenderTypingSeatTest cell generator, verbatim)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> drrBOutput;
    private static List<String> drrBGenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellAAvailable()) {
            List<String> errs = new ArrayList<>();
            drrAOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
        }
        if (cellBAvailable()) {
            List<String> errs = new ArrayList<>();
            drrBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_B_ROOT), errs);
            drrBGenErrors = errs;
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
