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
 * SEAT 33, law F.A -- facet {@code blockArmWrapperHopDeref} (F13, sigs B002 B039 B040
 * B041): the RULE-path block-ladder ARM's wrapper hops. TWO rungs, ONE commit.
 *
 * <ul>
 *   <li><b>rung (a) {@code collapsedMetaDerefArmAdmit}</b> --
 *       {@code NavigationHandler.collapsedMetaDerefRewrapOrNull} (the #317/#335/#364/#398
 *       producer) DECLINES at its fourth conjunct, the drain-seat test: the arm's node sits
 *       under a conditional, so {@code HandlerHelper.isInsideDrainableMapLambda} refuses it
 *       (the #312 exclusion, cut for {@code compileLambda}'s UNDRAINED conditional-block
 *       form) and no statement sink is reachable inside the lambda. But a rule-path LADDER
 *       block DRAINS its arms per rung, and that is exactly where golden puts the decl. A
 *       NEW narrowly-scoped window flag on {@code JavaStatementScope}, pushed by
 *       {@code CollectionHandler.compileLadderArmWithDeepThenDrain} around ONE arm compile
 *       and consulted ONLY at that conjunct, admits it. {@code findArmDerefHoistSink()} is
 *       deliberately NOT widened -- it has SIX consumers and a radius the seat-33 probe
 *       does not cover.</li>
 *   <li><b>rung (b) {@code ruleCalleeExplicitArgMetaDeref}</b> --
 *       {@code ReferenceHandler:907}'s {@code calleeFn} is null for an RRule symbol, so
 *       every {@code tryMetaDerefArg} arm is gated shut for an EXPLICIT-ARGS reporting-rule
 *       invocation. A SEPARATE {@code metaDerefCallee} local, built with
 *       {@code RFunction.fromRule} + the {@code setTypeCall} restoration exactly as the
 *       IMPLICIT no-args sibling {@code renderImplicitRuleInvocation:3896-3897} already
 *       does (LAW 69), is fed into the meta-deref arms ALONE. {@code calleeFn} itself is
 *       never reassigned, so {@code tryBareEnumArg}, {@code evaluateParamIsMulti} and the
 *       call-output attribute reads keep seeing null across all 21,772 RRule call sites.</li>
 * </ul>
 *
 * <p><b>NEITHER RUNG HEALS A FILE ALONE -- this is why they are ONE commit.</b> Golden
 * numbers the three hoists {@code referenceWithMetaPriceSchedule0} / {@code 1} / {@code 2}
 * in ONE deferred-coercion name group ({@code ReferenceHandler:2659-2663}: "a single-member
 * group resolves to the IDENTICAL escaped-iff-taken name the pre-#170 disambiguate
 * produced"). Rung (a) alone mints a SINGLETON, which resolves to the BARE
 * {@code referenceWithMetaPriceSchedule} -- not golden's {@code ...0}. Rung (b) alone mints
 * a TWO-member group {@code ...0}/{@code ...1} -- not golden's {@code ...1}/{@code ...2}.
 * Only both together produce {@code 0/1/2}. Corpus-verified three ways in the drr 5.61.0
 * goldens (ONE hoist -> bare: {@code iosco/cde/functions/CDEEquityForwardNotional:57},
 * {@code jfsa NotionalLeg2Rule:130}; TWO hoists -> {@code 0}/{@code 1}:
 * {@code cftc NotionalAmountLeg1Rule:213,218}). <b>{@code a3} is the numbering test and it
 * is mandatory.</b>
 *
 * <p><b>GOLDEN &larr; FORK</b> (both carriers, letter for letter -- the same three shapes
 * in the same order):
 * <ul>
 *   <li>B039 (rung a) golden {@code final ReferenceWithMetaPriceSchedule
 *       referenceWithMetaPriceSchedule0 = <chain>.get();} + {@code return
 *       (referenceWithMetaPriceSchedule0 == null ? MapperS.<PriceSchedule>ofNull() :
 *       MapperS.of(referenceWithMetaPriceSchedule0.getValue())).<UnitType>map("getUnit",
 *       ...)} &larr; fork {@code return MapperS.of(<chain>.get()).<UnitType>map("getUnit",
 *       priceSchedule -> priceSchedule.getUnit())...}</li>
 *   <li>B040/B041 (rung b, x2 per file) golden {@code final ReferenceWithMetaPriceSchedule
 *       referenceWithMetaPriceSchedule1 = <chain>.get();} + {@code
 *       cDECommodityNotionalCurrencyRule.evaluate((referenceWithMetaPriceSchedule1 == null ?
 *       null : referenceWithMetaPriceSchedule1.getValue()))} &larr; fork passes the wrapper
 *       chain raw</li>
 *   <li>B002 {@code import cdm.observable.asset.PriceSchedule;} -- a CONSEQUENCE of rung
 *       (a)'s {@code MapperS.<PriceSchedule>ofNull()} witness (the bare {@code PriceSchedule}
 *       token occurs at exactly one line per golden file, the rung-(a) hoist line)</li>
 * </ul>
 * Source: {@code regulation-jfsa-rewrite-trade-rule.rosetta:1715-1717} (rung a: the ELSE
 * arm of the parenthesised nested ite inside the {@code IsEquityForward} rung -- {@code ...
 * -> priceQuantity -> priceSchedule only-element -> unit -> currency}) and {@code :1732-1734}
 * / {@code :1738-1740} (rung b: {@code CDECommodityNotionalCurrency( FixedPriceLeg1(...) ->
 * fixedPrice -> price )}, where {@code price} is {@code [metadata reference]} and the rule's
 * {@code from} is the bare {@code PriceSchedule}). The cftc twin
 * ({@code regulation-cftc-rewrite-rule.rosetta}) carries the same three shapes.
 *
 * <p><b>CARRIERS (drr 5.61.0 POJO, 10 diff lines / 7 golden-only / 3 fork-only each):</b>
 * {@code drr/regulation/cftc/rewrite/reports/NotionalCurrencyLeg1Rule.java} and
 * {@code drr/regulation/jfsa/rewrite/trade/reports/NotionalCurrencyOfLeg1Rule.java}.
 * <b>WHOLE ceiling 2</b> (0 for either rung alone). The charter filed
 * {@code NotionalCurrencyLeg1Rule}'s residue under seat-32 law A.2's binder family; that is
 * a MIS-ATTRIBUTION, corrected here and in
 * {@code RuleThenArmLadderNestedTreeAdmitSeatTest:494}. A.2's landed rung types a LAMBDA
 * ITEM receiver ({@code CollectionHandler:948-954}); neither carrier hunk has one (hunk 1's
 * receiver is an only-element collapse, hunks 2/3 are explicit call arguments). Seat 32's
 * own {@code javac32-report.md} section 7.5.2 already said so ("that is C20's shape letter
 * for letter").
 *
 * <p><b>GREEN BLAST RADIUS -- MEASURED, BOTH ROUTES</b> (verdicts33-F sections 1.1, 1.2):
 * <ul>
 *   <li>rung (a): {@code [P33-COLLDEREF] verdict=decline:noDrain} is <b>4 rows / 2 distinct
 *       {@code where=}</b> ({@code rule:NotionalCurrencyLeg1},
 *       {@code rule:NotionalCurrencyOfLeg1}) -- the ENTIRE population that reaches conjunct
 *       4 -- identical OFF and ON. The other decline classes cannot join it:
 *       {@code decline:hostGate} (577 OFF / 152 ON) is <b>100% {@code wrap=-}</b>, so a
 *       drain seat would still decline them at the wrapper conjunct. Positive control:
 *       {@code verdict=fire} = 98 rows over 11 distinct {@code where=}, 98 on ON too, so a
 *       zero would have been mis-siting rather than inertness.</li>
 *   <li>rung (b): {@code [P33-RARG] symKind=RRule} = 21,772 -> {@code route=LAMBDA_CHANNEL}
 *       401 -> <b>{@code isWrapper=true} 8</b>, and those 8 ARE the two carriers' four calls
 *       each ({@code name=CDECommodityNotionalCurrency argIdx=0
 *       compiledItem=ReferenceWithMetaPriceSchedule valueType=PriceSchedule
 *       fromType=PriceSchedule}), on BOTH routes. <b>ZERO green.</b> The unnarrowed
 *       {@code symKind=RRule isWrapper=true} set is ALSO 8.</li>
 *   <li>Static, over all <b>174,141</b> goldens in all 25 cells (os.walk, because rg does
 *       not follow the corpus junctions): rung (a)'s pre-fix shape occurs <b>0</b> times and
 *       rung (b)'s pre-fix shape occurs <b>0</b> times, while rung (b)'s GOLDEN form
 *       ({@code Rule.evaluate((x == null ? null : x.getValue()))}) stands in <b>19 files</b>
 *       -- 17 of them GREEN today, produced by the #336 IMPLICIT path this law consults.
 *       Since every non-band file's fork text IS its golden text at this head, those zeros
 *       double as fork-side proof: no green file can regress, on either route, for either
 *       rung.</li>
 * </ul>
 *
 * <p><b>THE EXECUTION HAZARD, and why it does not bite (verdicts33-F section 6.1).</b> The
 * carriers' seats are entered TWICE per site -- {@code [P33-COLLDEREF]} prints 2 rows for
 * the ONE rung-(a) source site and {@code [P33-CALLEE33]}/{@code [P33-RARG]} print 4 for the
 * TWO rung-(b) sites -- because the block-rendered ARM subtree is re-compiled by the #375-C2
 * join-bare REPLAY pass. A naive registration on both visits would mint SIX group members
 * and golden's {@code 0/1/2} would become {@code 0..5}. The replay is ALREADY gated on
 * {@code !ladderTreeHasHoists(ladder)} ({@code CollectionHandler:11723}) with that exact
 * reason recorded ("a drained hoist may carry an EAGER identifier whose re-compile would
 * double-register the scope group"), so once this law drains hoists on the tree the replay
 * is skipped and the verdict pass's three registrations are the only ones.
 * <b>{@code a3} is the guard on that, and it asserts the exact three names.</b>
 *
 * <p><b>LAW 77 -- INHERITS, proven twice, no IR twin.</b> {@code IRJavaLeafEmitter
 * .emitFieldAccess} re-implements the only-element re-wrap ({@code :1115-1123}, facet
 * {@code nav_after_get_rewrap}) and its javadoc disposes of the #317 sibling BY ARGUMENT.
 * The probe measured it instead: {@code [P33-IRCOLL] feature=unit onlyElem=true} = <b>0 of
 * 1,033</b> {@code feature=unit} rows (all read {@code onlyElem=false}), against a positive
 * control of {@code onlyElem=true} = 3,515 over 44 groups -- so the arm IS reached and the
 * zero is trusted; and the positive twin, {@code [P33-COLLDEREF]}, prints the two carrier
 * {@code decline:noDrain} rows ON THE ON ROUTE, field-for-field identical. For rung (b),
 * {@code rune-ir-java} carries no render re-implementation of {@code tryMetaDerefArg} /
 * {@code metaDerefHoistRoute} (only {@code IRExpressionCompiler.legacyArgCoercionVerdict},
 * which restates legacy's ladder to decide whether the IR route may claim a native flat
 * render). <b>ONE caution handed forward:</b> {@code feature=value onlyElem=true} IS
 * non-zero (45 rows) -- a future rung aimed at a {@code -> value} hop gets a DIFFERENT
 * LAW-77 answer. {@code corpus_control2} measures both carriers on the IR route anyway.
 *
 * <p><b>CLAIMED RED at the seat base (both routes) --- MEASURED BY THE CHAIN:</b>
 * {@code a1}, {@code a2}, {@code a3}, {@code corpus_c1}, {@code corpus_c2},
 * {@code corpus_control1} (+ {@code corpus_control2} on {@code -Pir-on}). {@code e1} and
 * {@code e2} are GREEN at RED and must stay GREEN. {@code corpus_control1}'s RED prints
 * EXACTLY these two rows (measured from the seat-32 final dumps):
 * {@code .../NotionalCurrencyLeg1Rule.java fork=[0, 0, 0, 14, 0] golden=[3, 1, 2, 13, 0]}
 * and {@code .../NotionalCurrencyOfLeg1Rule.java fork=[0, 0, 0, 9, 0] golden=[3, 1, 2, 8, 0]}.
 * <b>CLAIMED GREEN at the law head:</b> 9/0F/1skip default, 9/0F/0skip {@code -Pir-on}.
 *
 * <p><b>MUTATION LANES (LAW 66/76) -- CLAIMED; the chain MEASURES them (LAW 82) and this
 * javadoc is rewritten FROM the lane logs before the commit:</b>
 * <ul>
 *   <li><b>m-lawFA-rungA</b> (the conjunct-4 consult severed): {@code a1}, {@code a3},
 *       {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} (+
 *       {@code corpus_control2}) fail -- and {@code a3} fails on the NAMES
 *       ({@code ...0}/{@code ...1} instead of {@code ...1}/{@code ...2}), which is the lane
 *       that PROVES the two rungs are coupled. NOT empty -- 4 measured rows.</li>
 *   <li><b>m-lawFA-armwindow</b> (the window never pushed): the same set. Kept separate
 *       because it severs the PRODUCER of the signal rather than its consumer.</li>
 *   <li><b>m-lawFA-rungB</b> (the callee mint severed): {@code a2}, {@code a3},
 *       {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} (+
 *       {@code corpus_control2}) fail -- {@code a3} on the BARE singleton name. The coupling
 *       proof from the other side. NOT empty -- 8 measured rows.</li>
 *   <li><b>m-lawFA-typecall</b> (the {@code setTypeCall} restoration dropped): the rung-(b)
 *       deref must VANISH ({@code tryMetaDerefArg}'s param-type gate declines on the MISSING
 *       type) -- {@code a2}, {@code a3}, {@code corpus_c1}, {@code corpus_c2},
 *       {@code corpus_control1}. The lane that proves the #398 {@code deepCopy} note is live
 *       code, not folklore.</li>
 *   <li><b>m-lawFA-lambdachannel</b> (the route belt dropped): <b>DECLARED EMPTY AT THE
 *       MEASURED GRAIN</b> -- all 8 {@code isWrapper=true} RRule rows are already
 *       LAMBDA_CHANNEL, so no measured wrapper row moves. The belt is <b>NOT inert</b>: it
 *       holds 21,371 further rows (STATEMENT_SINK 20,899 + BLOCK 472) out of
 *       {@code tryMetaDerefArg}'s five null-compiled-type RECOVERY channels
 *       ({@code :2438}/{@code :2452}/{@code :2467}/{@code :2477}/{@code :2512}), which the
 *       probe's naive compiled-type read cannot see -- 393 of the 401 LAMBDA_CHANNEL rows
 *       read {@code compiledItem=-}. One risk the recovery ladder CANNOT carry, measured:
 *       its numeric arms both require {@code isBigDecimal(paramJavaType)} and all 21,772
 *       RRule rows' 21 distinct {@code fromType} values are POJO data types. Ship the belt;
 *       do NOT call it inert.</li>
 *   <li><b>m-lawFA-sentinel</b> (the new channel's decl routed through the PLAIN declaration
 *       object instead of the sentinel): <b>UNSCORED</b>. The traced hazard is the #346/#398
 *       closed-scope law -- the plain render calls {@code getActualName()} and closes the
 *       ancestor scope chain, poisoning rung (b)'s two LATER identifier creations in the
 *       same statement scope. Whether that materialises as a byte move, a rendered stub, or
 *       nothing at this corpus is a MEASUREMENT the chain must make; no set is claimed for
 *       it in advance.</li>
 * </ul>
 * The {@code !expr.args().isEmpty()} conjunct is <b>ADJUDICATED-EMPTY</b> (LAW 82):
 * {@code args=0} occurs 0 times in 125,932 {@code [P33-CALLEE33]} rows (16 distinct values,
 * minimum 1). It ships as structural defence, honestly un-witnessed.
 *
 * <p><b>LAW 74 (MEASURED, not analytic).</b> {@code javac32-report.md} section 7.4 row
 * <b>C20 {@code NotionalCurrencyOfLeg1Rule} STANDING 3</b> (bodies at
 * {@code javac32/Seat32Standing.java:707-715}): line 709 {@code cannot find symbol: method
 * getUnit()}, location {@code variable priceSchedule of type ReferenceWithMetaPriceSchedule}
 * -- <b>rung (a)</b>; lines 712, 714 {@code ReferenceWithMetaPriceSchedule cannot be
 * converted to PriceSchedule} -- <b>rung (b)</b> x2. <b>PRE 3 -&gt; POST 0.</b> Section
 * 7.5.2 licenses citing the cftc twin from the same row ("that is C20's shape letter for
 * letter"). The commit message MAY say "the fork's text does not compile" for these
 * carriers, with the row cited.
 *
 * <p><b>LAW 81 tripwires this heal fires in OTHER suites</b> -- SEVEN suites pin BOTH
 * carriers, and {@code RuleThenArmLadderNestedTreeAdmitSeatTest.corpus_c3} is the
 * IMPROVED-not-whole disclosure that this law promotes. Re-pin each FROM ITS OWN PRINT, in
 * this law's commit: {@code AsKeySetComplexValueDerefSeatTest:525,527};
 * {@code BlockLambdaSingleItemChainStampSeatTest:839,840};
 * {@code DepFieldTypeCollisionSeedSeatTest:533,534};
 * {@code DispatchBaseInputDeclineSeatTest:466,467};
 * {@code FilterPredicateMetaDerefSeatTest:669,670};
 * {@code ThenWrappedDefaultSeatTest:491,492};
 * {@code WrapperLadderKeepsCollapseSeatTest:391,392}.
 *
 * <p><b>LAW 82 - MEASURED by the receipts chain, run 1 at ce1a06292 (final33.status; every
 * figure below is transcribed from the chain's own logs, never from this file's earlier
 * CLAIMED paragraphs, which it supersedes).</b> GREEN 9/0F/1skip default (f33-green-default.log) /
 * 9/0F/0skip {@code -Pir-on} (f33-green-on.log); RED at the pre-seat base {@code fa49da010}: default
 * 6F = a1, a2, a3, corpus_c1, corpus_c2, corpus_control1; {@code -Pir-on} 7F = a1, a2, a3, corpus_c1, corpus_c2, corpus_control1, corpus_control2 (f33-red-{default,on}.log).
 * Mutation lanes ({@code mut33.py}, the default profile; {@code lanes33.py --summary}):
 * <ul>
 *   <li><b>{@code m-lawFA-runga}</b> ({@code NH_PAIRS_MUT_RUNGA}): MEASURED 9/6F/1skip = a1, a2, a3, corpus_c1, corpus_c2, corpus_control1 - RE-SCORED (superset): a2 fell as well as the claimed a1, a3, c1, c2, control1 - the three hoists are ONE numbered name group, so removing rung (a)'s member renumbers the names a2 asserts; the rungs are coupled through the group, which the claim under-stated.</li>
 *   <li><b>{@code m-lawFA-armwindow}</b> ({@code CH_PAIRS_MUT_ARMWINDOW}): MEASURED 9/6F/1skip = a1, a2, a3, corpus_c1, corpus_c2, corpus_control1 - RE-SCORED (superset, identical to runga): a1, a2, a3, c1, c2, control1 - the producer and the consumer of the window signal fail identically, as the lane was designed to prove.</li>
 *   <li><b>{@code m-lawFA-rungb}</b> ({@code RH_PAIRS_MUT_RUNGB}): MEASURED 9/6F/1skip = a1, a2, a3, corpus_c1, corpus_c2, corpus_control1 - RE-SCORED (superset): a1 fell as well as the claimed a2, a3, c1, c2, control1 - the same one-name-group coupling seen from rung (b)'s side (the claim's own 'coupling proof').</li>
 *   <li><b>{@code m-lawFA-typecall}</b> ({@code RH_PAIRS_MUT_TYPECALL}): MEASURED 9/3F/1skip = corpus_c1, corpus_c2, corpus_control1 - RE-SCORED: c1, c2, control1 only - a2 and a3 HELD: the fixture's rule call resolves its parameter type without the `setTypeCall` restoration, so the deref survives there; the restoration is load-bearing at the CORPUS grain (both carriers + the whole-cell scan), not at the fixture's.</li>
 *   <li><b>{@code m-lawFA-lambdachannel}</b> ({@code RH_PAIRS_MUT_LAMBDACHANNEL}): MEASURED 9/0F/1skip = (none) - EMPTY-as-declared (all 8 wrapper RRule rows already LAMBDA_CHANNEL); the belt stands.</li>
 *   <li><b>{@code m-lawFA-sentinel}</b> ({@code NH_PAIRS_MUT_SENTINEL}): MEASURED 9/0F/1skip = (none) - UNSCORED at draft (no claim either way), MEASURED EMPTY: the sentinel-vs-plain-decl hazard produced neither a byte move nor a stub at this corpus or fixture; the sentinel stays as defence-in-depth.</li>
 * </ul>
 */
class BlockArmWrapperHopDerefSeatTest {

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

    // The two carriers (drr 5.61.0 POJO). NOTE: `where=rule:NotionalCurrencyLeg1` is
    // AMBIGUOUS in every seat-33 probe tag -- drr 5.61.0 declares TWO rules of that simple
    // name (asic regulation-asic-rewrite-trade-rule.rosetta:657 and cftc
    // regulation-cftc-rewrite-rule.rosetta:1050) and findEnclosingRule().name() is the
    // SIMPLE name. The asic body is `Notional -> unit -> currency` with no priceSchedule
    // leaf and no CDECommodityNotionalCurrency call, so it is not in the measured 8; every
    // compare here is anchored on the FILE PATH in the cell, never the rule name.
    private static final String CCY1 =
            "drr/regulation/cftc/rewrite/reports/NotionalCurrencyLeg1Rule.java";
    private static final String CCY_OF_1 =
            "drr/regulation/jfsa/rewrite/trade/reports/NotionalCurrencyOfLeg1Rule.java";

    /** Cell A = drr 5.61.0 -- both carriers + the whole-cell UNION control. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A);
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    // =========================================================================
    // Fixtures -- reduced from the carriers' REAL source
    // =========================================================================

    /**
     * Reduced from {@code test-corpus/drr/drr-5.61.0/rosetta-source/src/main/rosetta/
     * regulation-jfsa-rewrite-trade-rule.rosetta}, {@code reporting rule
     * NotionalCurrencyOfLeg1} ({@code :1670-1762}).
     *
     * <p><b>A1</b> keeps every load-bearing dimension of the carrier: the outer chain is
     * {@code filter <p> then extract <bare-fn conditional> then extract <ladder>} so the
     * ladder is a bound rule-then body (a real statement seat whose arm hoists drain
     * in-rung); rung 2's arm is itself a conditional whose ELSE is the
     * {@code -> <meta MULTI> only-element -> <bare feature>} collapse (rung (a)'s seat --
     * the carrier's {@code IsEquityForward} rung, an INNER-level TERMINAL arm); rungs 3 and
     * 4 are EXPLICIT-ARGS RULE calls whose argument is a {@code [metadata reference]}
     * SINGLE nav and whose callee's {@code from} is the BARE value type (rung (b)'s seat --
     * the carrier's two commodity rungs, TOP-level rung arms). Both cardinalities are the
     * carrier's: {@code priceSchedule} is {@code (0..*)} + {@code [metadata reference]} (so
     * the receiver is a {@code mapC} collapsed by {@code only-element}, exactly the shape
     * whose plain re-wrap does not compile) and {@code price} is {@code (0..1)} +
     * {@code [metadata reference]}.
     *
     * <p><b>E1</b> is rung (b)'s decline lock: the same block-ladder arm shape with an
     * explicit-args RULE call whose argument is a PLAIN (non-meta) navigation.
     * {@code tryMetaDerefArg}'s {@code actualItemType instanceof RJavaWithMetaValue} gate
     * must decline it -- no hoist, no deref, bytes unchanged.
     *
     * <p><b>E2</b> is rung (a)'s decline lock: the SAME collapse hop in a rung CONDITION.
     * Conditions compile OUTSIDE the arm window ({@code compileLadderLevelArms} compiles
     * the condition before pushing the arm window), so a law that marked the LADDER's scope
     * instead of each ARM would fire here and golden does not.
     */
    private static final String MODEL = """
            namespace census.seat33fa
            version "1.0.0"

            type UT:
                currency string (0..1)

            type PSched:
                unit UT (0..1)

            type QS:
                unit UT (0..1)

            type RPQ:
                qty QS (0..1)
                priceSchedule PSched (0..*)
                    [metadata reference]

            type FP:
                price PSched (0..1)
                    [metadata reference]

            type Prod:
                rpq RPQ (0..1)
                fp FP (0..1)
                fp2 FP (0..1)
                big boolean (0..1)
                fwd boolean (0..1)
                fix boolean (0..1)
                swn boolean (0..1)

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
                    p -> big = True

            func IsFwd:
                inputs:
                    p Prod (0..1)
                output:
                    r boolean (1..1)
                set r:
                    p -> fwd = True

            func IsFix:
                inputs:
                    p Prod (0..1)
                output:
                    r boolean (1..1)
                set r:
                    p -> fix = True

            func IsSwn:
                inputs:
                    p Prod (0..1)
                output:
                    r boolean (1..1)
                set r:
                    p -> swn = True

            reporting rule CdeCcy from PSched: <"the BARE-`from` rule callee - the CDECommodityNotionalCurrency analogue: its generated evaluate() takes the bare PSched, so a ReferenceWithMetaPSched argument must be hoisted and null-guard-dereffed">
                extract unit -> currency
                as "cdeccy"

            reporting rule CdeQs from QS: <"e1's non-wrapper callee twin: the same rule shape with a PLAIN (non-meta) `from` and a plain argument">
                extract unit -> currency
                as "cdeqs"

            reporting rule A1BlockArmWrapperHop from Instr: <"a1/a2/a3 - THE drr 5.61.0 jfsa NotionalCurrencyOfLeg1 / cftc NotionalCurrencyLeg1 SHAPE: a bound rule-then block ladder whose INNER-level terminal arm carries a `<meta multi> only-element -> <bare feature>` collapse hop and whose two later rung arms are explicit-args RULE calls with a meta-wrapper argument - ONE deferred-coercion name group, 0/1/2">
                filter IsTerm
                then extract
                    if IsTerm
                    then BeforeFor
                    else TradeFor
                then extract
                    if IsBig(item)
                    then item -> rpq -> qty -> unit -> currency
                    else if IsFwd(item)
                    then (if item -> rpq -> qty -> unit -> currency exists
                        then item -> rpq -> qty -> unit -> currency
                        else item -> rpq -> priceSchedule only-element -> unit -> currency)
                    else if IsFix(item)
                    then CdeCcy(item -> fp -> price)
                    else if IsSwn(item)
                    then CdeCcy(item -> fp2 -> price)
                as "a1"

            reporting rule E1PlainArgRuleCall from Instr: <"e1 - the DECLINE LOCK for rung (b)'s wrapper gate: the same block-ladder arm shape, but the explicit-args RULE call's argument is a PLAIN (non-meta) navigation, so tryMetaDerefArg must decline at its RJavaWithMetaValue gate">
                filter IsTerm
                then extract
                    if IsTerm
                    then BeforeFor
                    else TradeFor
                then extract
                    if IsBig(item)
                    then item -> rpq -> qty -> unit -> currency
                    else if IsFix(item)
                    then CdeQs(item -> rpq -> qty)
                    else if IsSwn(item)
                    then CdeQs(item -> rpq -> qty)
                as "e1"

            reporting rule E2CondSeatWrapperHop from Instr: <"e2 - the DECLINE LOCK for rung (a)'s ARM-SCOPED window: the SAME collapse hop in a rung CONDITION, which compiles BEFORE the arm window is pushed - the plain re-wrap must survive and no wrapper local may appear">
                filter IsTerm
                then extract
                    if IsTerm
                    then BeforeFor
                    else TradeFor
                then extract
                    if item -> rpq -> priceSchedule only-element -> unit -> currency exists
                    then item -> rpq -> qty -> unit -> currency
                    else if IsSwn(item)
                    then item -> rpq -> qty -> unit -> currency
                as "e2"
            """;

    /**
     * a1 -- RUNG (a). The collapse hop inside the ladder's inner-level terminal arm hoists
     * the wrapper into a local and null-guard-RECONSTRUCTS a {@code MapperS<PSched>} instead
     * of navigating {@code getUnit} on the wrapper.
     *
     * <p>PIN AT RED: the ABSENT token {@code .get()).<UT>map("getUnit"} is the fork's exact
     * non-compiling form (the plain {@code MapperS.of(<chain>.get())} re-wrap followed by a
     * value-getter nav), so the pre-law render fails this assert for the MECHANISM's reason.
     * If the fixture renders neither form, RESHAPE it -- do not weaken the assert.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_theCollapsedMetaHopInABlockLadderArmHoistsAndDerefs() throws IOException {
        String out = fixtureRule("A1BlockArmWrapperHopRule");
        assertContains(out, "final ReferenceWithMetaPSched referenceWithMetaPSched0 = ");
        assertContains(out, "(referenceWithMetaPSched0 == null ? MapperS.<PSched>ofNull() :"
                + " MapperS.of(referenceWithMetaPSched0.getValue())).<UT>map(\"getUnit\"");
        assertTrue(!out.contains(".get()).<UT>map(\"getUnit\""),
                "a1 must not keep the plain only-element re-wrap + value-getter nav on the"
                        + " WRAPPER (the fork's non-compiling form):\n" + out);
    }

    /**
     * a2 -- RUNG (b). Both explicit-args RULE calls hoist their meta-wrapper argument and
     * pass the null-guarded deref, exactly as the #336 IMPLICIT path already does for the
     * 17 green {@code Rule.evaluate((x == null ? null : x.getValue()))} files.
     *
     * <p>The receiver field name is deliberately NOT asserted (it is minted by
     * {@code FunctionDependencyCollector.ruleDependencyFieldName}, a different law); the
     * law's own tokens are the deref operands.
     *
     * <p>PIN AT RED (measured, FA-red1.log): the draft's ABSENT-half token was the generic
     * {@code .get()));}, which read <b>5</b> at RED, not 2 -- lines 87/91 of the render ARE
     * the two raw-wrapper rule arguments, but lines 63/68/70 are the GREEN
     * {@code isTerm/beforeFor/tradeFor.evaluate(item.get())} FUNCTION-call sites
     * ({@code calleeFn != null}, a {@code MapperS<Instr>} receiver, no wrapper) which this
     * law must NOT touch, so that assert could never go green. The absent half is therefore
     * anchored on the wrapper's own tail {@code fP.getPrice()).get()))} (RED 2, the two
     * rule-call sites; GREEN 0 -- the hoist line ends {@code .get();}), and the three
     * function-call sites are pinned as a NO-MOVE witness. The cause was re-read before
     * the assert was touched: the fixture DOES render the fork's raw-wrapper form.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_theExplicitArgsRuleCallsDerefTheirWrapperArgument() throws IOException {
        String out = fixtureRule("A1BlockArmWrapperHopRule");
        assertContains(out, ".evaluate((referenceWithMetaPSched1 == null ? null :"
                + " referenceWithMetaPSched1.getValue()))");
        assertContains(out, ".evaluate((referenceWithMetaPSched2 == null ? null :"
                + " referenceWithMetaPSched2.getValue()))");
        assertEquals(2, count(out, "== null ? null : referenceWithMetaPSched"),
                "rung (b) must deref BOTH rule-call arguments (golden's two commodity"
                        + " rungs), not one:\n" + out);
        assertEquals(0, count(out, "fP.getPrice()).get()))"),
                "no rule call may still receive the RAW wrapper as its argument (the fork's"
                        + " non-compiling form, javac32 C20:712/714):\n" + out);
        assertEquals(3, count(out, "evaluate(item.get()));"),
                "the three FUNCTION-call sites (isTerm / beforeFor / tradeFor, a non-wrapper"
                        + " MapperS<Instr> receiver) must be UNMOVED by the rule-callee mint:\n" + out);
    }

    /**
     * a3 -- THE NUMBERING TEST, and it is MANDATORY (verdicts33-F section 0.2 + section
     * 6.1). All three hoists share the base {@code referenceWithMetaPSched} and therefore
     * ONE deferred-coercion group: a singleton resolves BARE, a pair resolves
     * {@code 0}/{@code 1}, and only the full triple resolves {@code 0}/{@code 1}/{@code 2}.
     * This is what makes the two rungs one commit, AND it is the guard against the
     * double-registration hazard (the arm subtree is re-compiled by the #375-C2 replay
     * unless the tree already drained a hoist -- {@code CollectionHandler:11723}). Six
     * members, or a bare name, or a {@code ...3}, all fail here.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_theThreeHoistsFormOneNameGroupNumberedZeroOneTwo() throws IOException {
        String out = fixtureRule("A1BlockArmWrapperHopRule");
        assertEquals(3, count(out, "final ReferenceWithMetaPSched referenceWithMetaPSched"),
                "exactly THREE wrapper hoists (one from rung (a), two from rung (b)):\n" + out);
        assertContains(out, "final ReferenceWithMetaPSched referenceWithMetaPSched0 = ");
        assertContains(out, "final ReferenceWithMetaPSched referenceWithMetaPSched1 = ");
        assertContains(out, "final ReferenceWithMetaPSched referenceWithMetaPSched2 = ");
        assertTrue(!out.contains("referenceWithMetaPSched3"),
                "the seat is entered TWICE per site (the #375-C2 replay); a second"
                        + " registration would mint six group members:\n" + out);
        assertTrue(!out.contains("final ReferenceWithMetaPSched referenceWithMetaPSched = "),
                "a BARE name means the group has ONE member - i.e. only one rung landed:\n"
                        + out);
    }

    /**
     * e1 -- THE DECLINE LOCK for rung (b). An explicit-args RULE call whose argument is a
     * PLAIN (non-meta) navigation gets the new callee too, and {@code tryMetaDerefArg} must
     * still decline it at {@code actualItemType instanceof RJavaWithMetaValue}. Bytes
     * unchanged: no local, no deref.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_ruleCallWithANonWrapperArgumentIsUnchanged() throws IOException {
        String out = fixtureRule("E1PlainArgRuleCallRule");
        assertTrue(out.contains(".evaluate("),
                "PREMISE: e1's explicit-args rule call must render an invocation, else the lock"
                        + " is vacuous:\n" + out);
        assertTrue(!out.contains("final QS "),
                "e1 must not hoist a local for a non-meta argument:\n" + out);
        assertTrue(!out.contains("== null ? null : qS"),
                "e1 must not deref a non-meta argument:\n" + out);
    }

    /**
     * e2 -- THE DECLINE LOCK for rung (a)'s ARM-SCOPED window. Rung conditions compile
     * BEFORE the arm window is pushed ({@code compileLadderLevelArms} compiles
     * {@code rung.condition()} at its own seat, then pushes around
     * {@code compileLadderArmWithDeepThenDrain}), so the same collapse hop in a CONDITION
     * must keep the plain re-wrap. A law that marked the LADDER's scope instead of each ARM
     * would fire here, and golden does not.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e2_theRungConditionSeatIsNotTheArmWindow() throws IOException {
        String out = fixtureRule("E2CondSeatWrapperHopRule");
        assertTrue(!out.contains("final ReferenceWithMetaPSched "),
                "e2 must not hoist a wrapper local from a rung CONDITION - the window is"
                        + " ARM-scoped:\n" + out);
        assertContains(out, ".get()).<UT>map(\"getUnit\"");
    }

    /** corpus_c1 -- {@code NotionalCurrencyOfLeg1Rule} whole (B002 B039 B040 B041). */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_notionalCurrencyOfLeg1RuleIsByteIdenticalToGolden() throws IOException {
        assertCarrierWhole(CCY_OF_1, "B002 B039 B040 B041");
    }

    /** corpus_c2 -- {@code NotionalCurrencyLeg1Rule} (cftc) whole (B002 B039 B040 B041). */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c2_notionalCurrencyLeg1RuleIsByteIdenticalToGolden() throws IOException {
        assertCarrierWhole(CCY1, "B002 B039 B040 B041");
    }

    /**
     * corpus_control1 -- LAW 79, the whole-cell UNION scan on drr 5.61.0. Per EMITTED file
     * the (wrapper-hoist decls, guarded meta re-wraps, evaluate-arg derefs, only-element
     * re-wrap navs, field-wrapper hoist decls) tuple must equal golden's, with NO residue.
     * The tuple is chosen so BOTH rungs move it and an over-fire anywhere in the cell lands
     * here: {@code t[0]} and {@code t[4]} count the two wrapper-hoist decl families
     * (t[4] is the pure over-fire detector -- this law must not mint a
     * {@code FieldWithMeta*} local), {@code t[1]} the rung-(a) guarded operand,
     * {@code t[2]} the rung-(b) deref, {@code t[3]} the plain only-element re-wrap the law
     * REMOVES one of per carrier.
     *
     * <p>MEASURED from the seat-32 final OFF dump: at the seat base exactly TWO drr 5.61.0
     * files differ in this tuple, and they are the two carriers
     * ({@code NotionalCurrencyLeg1Rule fork=[0, 0, 0, 14, 0] golden=[3, 1, 2, 13, 0]},
     * {@code NotionalCurrencyOfLeg1Rule fork=[0, 0, 0, 9, 0] golden=[3, 1, 2, 8, 0]}). The
     * other four band files in the cell -- jfsa {@code NotionalLeg2Rule} and the mas
     * {@code FixedFloatRateLeg1/2} / {@code InterestRatePrice} trio -- are already tuple
     * IDENTICAL, so neither law F.B nor the banked E.1 interacts with this control.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr561WholeCellWrapperHopsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 5.61.0 reported a generation error - the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR561, DOMAIN_DRR561);
    }

    /**
     * corpus_control2 -- LAW 77, the IR route vs GOLDEN for BOTH carriers. The probe
     * measured INHERITS ({@code [P33-IRCOLL] feature=unit onlyElem=true} = 0 of 1,033,
     * against a live positive control of 3,515; and {@code [P33-COLLDEREF]} prints the two
     * carrier rows on the ON route), so no IR twin ships -- this measures that rather than
     * arguing it, and it is MANDATORY even so: the inheritance is measured for
     * {@code feature=unit} only, and {@code feature=value onlyElem=true} is non-zero
     * (45 rows). Skips unless the IR provider is on the classpath ({@code -Pir-on}).
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForBothCarriers() throws IOException {
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_A_ROOT),
                new ArrayList<>());
        for (String carrier : List.of(CCY_OF_1, CCY1)) {
            assertEquals(normalize(Files.readString(GOLDEN_A.resolve(carrier))),
                    normalize(irOut.get(carrier)), "IR route vs GOLDEN: " + carrier);
        }
    }

    ///PIN: MEASURED 469 - transcribed from this control's OWN print at the law head
    ///PIN: (FA-green1.log: "expected: <473> but was: <469>", residue EMPTY - the domain
    ///PIN: assert fired alone). The draft pinned the DERIVED upper bound below:
    ///PIN: `python target/seat33-instruments/drafts33/FA/domain-derive.py` walks the drr
    ///PIN: 5.61.0 GOLDEN tree (os.walk followlinks=True; rg does not follow the corpus
    ///PIN: junctions) and finds 473 of 5,249 golden files carrying a non-zero tuple. That
    ///PIN: is the GOLDEN half only: the control's universe is additionally
    ///PIN: retainAll(emitted), which the seat-32 twin's own print measured to roughly HALVE
    ///PIN: the analogous number (a domain-derive of 2932 printed as 1470). So 473 is an
    ///PIN: UPPER BOUND, deliberately pinned high so the assert PRINTS the true value on its
    ///PIN: first run - transcribe it (LAW 73, print-first). The residue assert above runs
    ///PIN: FIRST in assertUnionEqual, so a wrong domain cannot mask the residue verdict.
    private static final int DOMAIN_DRR561 = 469;

    ///PIN: EMPTY BY THE LAW. MEASURED from the seat-32 final OFF dump over all six drr
    ///PIN: 5.61.0 band files: exactly TWO differ in this tuple and they are this law's two
    ///PIN: carriers (NotionalCurrencyLeg1Rule fork=[0, 0, 0, 14, 0] golden=[3, 1, 2, 13, 0];
    ///PIN: NotionalCurrencyOfLeg1Rule fork=[0, 0, 0, 9, 0] golden=[3, 1, 2, 8, 0]). Those
    ///PIN: two rows ARE the CLAIMED RED print. Every mutation lane MOVES this list; that is
    ///PIN: how the lanes are scored.
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
                        + ") - seat 33 law F.A: the block arms' wrapper hops deref");
    }

    /**
     * (T1..T5) per file: wrapper-hoist decls ({@code final ReferenceWithMeta...}), the
     * guarded meta re-wrap operand ({@code == null ? MapperS.<}), the evaluate-arg deref
     * ({@code .evaluate((}), the plain only-element re-wrap followed by a typed nav
     * ({@code .get()).<}), and the sibling field-wrapper hoist family
     * ({@code final FieldWithMeta...}) as a pure over-fire detector.
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String text = normalize(e.getValue());
            int[] t = new int[5];
            t[1] = count(text, "== null ? MapperS.<");
            t[2] = count(text, ".evaluate((");
            t[3] = count(text, ".get()).<");
            for (String line : text.split("\n")) {
                String s = line.trim();
                if (s.startsWith("final ReferenceWithMeta")) {
                    t[0]++;
                }
                if (s.startsWith("final FieldWithMeta")) {
                    t[4]++;
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
        // The domain-pin flow (LAW 73): the residue verdict is asserted BEFORE the domain,
        // so a not-yet-transcribed domain pin cannot mask an over-fire or an under-fire.
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - transcribe from this"
                        + " print: MEASURED domain=" + universe.size()
                        + " residue=" + mismatched);
        assertEquals(knownResidue, mismatched,
                "the wrapper-hop tuple differs beyond the named residue in " + mismatched.size()
                        + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted token-bearing files ("
                        + expectedDomain + ")");
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
        RModel main = AstBuilder.buildFromString(MODEL, "seat33fa.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat33fa".equals(m.namespace()));
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
            throw new AssertionError("[BlockArmWrapperHopDerefSeatTest] builtins parse"
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
