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
 * <b>SEAT 33, LAW D.12</b> -- facets {@code iteProbeSessionSnapshotRestore} (D.1, sig B003)
 * and {@code iteDeclWildcardFromDisagreeingArms} (D.2, sig B014): <b>TWO INDEPENDENT
 * PREDICATES IN ONE COMMIT</b>, because {@code StrikePrice.java} goes WHOLE only with both.
 *
 * <p><b>THE CARRIER.</b> {@code drr/standards/iosco/cde/version1/price/functions/StrikePrice.java}
 * x drr 7.0.0 / 7.1.0 / 7.2.0 / 7.3.0 FUNCTION -- 22 diff lines / 6 hunks per cell at the seat-33
 * base {@code fa49da010}. Reduced from
 * {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/main/rosetta/standards-iosco-cde-version1-price-func.rosetta}
 * lines 134-168: {@code func StrikePrice}, whose {@code set prices:} is a NINE-rung
 * {@code if / else if} ladder with NO terminal else, the whole ladder being the BASE of a
 * {@code then only-element}. Whole-file ceiling: <b>0 for either predicate alone</b> (D.1 alone
 * leaves the 2-line B014 decl hunk; D.2 alone leaves the five B003 hunks = 20 lines),
 * <b>4 together</b>.
 *
 * <p><b>D.1 {@code iteProbeSessionSnapshotRestore} -- the BYTE half.</b> The #372 F-beta k==0
 * quadrant of {@code FunctionExpressionRenderer.renderThenExtractSetImpl} renders the base ladder
 * TWICE: once as a PROBE into a throwaway {@code StringBuilder} on a throwaway scope (to learn
 * whether the real render will succeed), then for real. The scope is isolated but the per-method
 * {@code StatementHoistSession} is a renderer FIELD, and {@code createScope(baseCondF)} carries no
 * statement-hoist sink, so the #179/#399-R3 bare-fn-condition boolHoist inside
 * {@code appendIteHoistChainCore} falls back to that field and REGISTERS its BOOLEAN names during
 * the discarded render. StrikePrice's five bare-function-call rungs therefore number from 5:
 * golden {@code final Boolean boolean0 = isCap.evaluate(...)}, fork {@code boolean5}, five hunks
 * of four lines. The fix snapshots BEFORE the probe and restores immediately after it -- the shape
 * the #399 FS pair one arm up already has, and whose own comment names this defect (LAW 69).
 * MEASURED, both routes, all four cells: {@code [P33-E4] where=fn:StrikePrice probeName=thenArg
 * boolPre=0 boolPost=5 thenArgPre=0 thenArgPost=0} -- and the tag's other three {@code where=}
 * values ({@code fn:ExtractReferenceEntity}, {@code fn:GetUniqueSwapIdentifier},
 * {@code fn:GetUniqueTransactionIdentifier}) all read {@code boolPost == boolPre == 0}, so the
 * GREEN SET AT THIS SEAT IS EMPTY BY MEASUREMENT. The restore can only ever REMOVE registrations
 * a discarded render made.
 *
 * <p><b>D.2 {@code iteDeclWildcardFromDisagreeingArms} -- the COMPILE half.</b> The ite-hoist DECL
 * widens to {@code Mapper*<? extends J>} when the ladder's real arms DISAGREE and fold to a PROPER
 * supertype J that IS the decl element: golden {@code final MapperC<? extends PriceSchedule>
 * thenArg;} over nine arms of {@code {PriceSchedule x6, Price x3}} with
 * {@code type Price extends PriceSchedule}. Java generics are invariant, so the fork's
 * {@code MapperC<PriceSchedule>} is an incompatible-types assignment at the two
 * {@code <Price>map(...)} arms. The fold is {@code ControlFlowHandler}'s own #398 deep-then-ladder
 * fold, EXTRACTED to {@code FunctionAliasHelper} so the two seats consult ONE walk (LAW 69) -- the
 * law is written verbatim in that seat's javadoc and this seat never consulted it.
 * {@code resultType} is deliberately NOT widened, so the typed empties keep golden's own bare form
 * ({@code thenArg = MapperC.<PriceSchedule>ofNull();}, golden StrikePrice line 139).
 * MEASURED with the ACTUAL RType fold the law ships ({@code [P33-F6B]}, both routes, over every
 * {@code appendIteHoistChainCore} decl = 1,573 rows): {@code fires=true} is <b>40 rows, ALL
 * {@code where=fn:StrikePrice}</b>. The whole disagreement population is 56 rows over THREE
 * {@code where=} values (StrikePrice 40 + {@code fn:FXLeg1} 8 + {@code fn:FXLeg2} 8), and the
 * FXLeg pair reads {@code aliasWildcard=true} -- the existing alias rung already widened them, so
 * the {@code declResultType.equals(resultType)} guard skips them. <b>GREEN SET EMPTY.</b>
 * {@code itemTypeOverride == null} is NOT a conjunct of this law and must never become one:
 * 36 of the 40 firing rows read {@code override=true}.
 *
 * <p><b>CLAIMED RED at the seat-33 base {@code fa49da010}</b> (CLAIMED -- measured by the chain;
 * default route): {@code a1}, {@code a2}, {@code corpus_c1}, {@code corpus_c2},
 * {@code corpus_control1}, {@code corpus_control3} (+ {@code corpus_control2} under
 * {@code -Pir-on}). <b>CLAIMED GREEN at the law head:</b> the whole suite.
 * {@code corpus_control0}, {@code e1} and {@code e2} are GREEN at BOTH states by design --
 * {@code control0} is the golden-side oracle (prove the instrument can fail), {@code e1} is D.2's
 * decline lock and {@code e2} the whole-file decline lock.
 *
 * <p><b>MUTATIONS (LAW 66/76) -- CLAIMED; the chain measures them and this javadoc is rewritten
 * from its lane logs (LAW 82).</b>
 * <ul>
 *   <li><b>m-d1-snapshot</b> ({@code FER_PAIRS_MUT_SNAPSHOT} -- the post-probe restore deleted,
 *       the snapshot left hoisted but never applied): CLAIMED failing set {@code a1},
 *       {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1}, {@code corpus_control3}
 *       (+ {@code corpus_control2} on {@code -Pir-on}). {@code a2} and {@code e1} stay GREEN --
 *       the lane that proves the two predicates are INDEPENDENT and that D.1 alone is not the
 *       whole heal.</li>
 *   <li><b>m-d1-site</b> ({@code FER_PAIRS_MUT_SITE} -- the snapshot moved back BELOW the probe,
 *       the restore call KEPT): the same failing set. This is the POSITION lane: it proves the fix
 *       is the ORDER of the snapshot, not the presence of a snapshot/restore pair.</li>
 *   <li><b>m-d2-wildcard</b> ({@code FER_PAIRS_MUT_WILDCARD} -- the widening conjunct severed):
 *       CLAIMED failing set {@code a2}, {@code corpus_c1}, {@code corpus_c2},
 *       {@code corpus_control1}, {@code corpus_control3} (+ {@code corpus_control2}), with
 *       {@code a1} and {@code e2} GREEN -- and the carrier back to NON-COMPILING (javac C9).</li>
 *   <li><b>m-d2-join</b> ({@code FER_PAIRS_MUT_JOIN} -- only the {@code join == decl element}
 *       conjunct severed): <b>CLAIMED EMPTY at this corpus</b> and declared as such. All 40
 *       {@code [P33-F6B]} firing rows read {@code join == item}, so the conjunct is
 *       defence-in-depth here; it ships because a fold that lands ABOVE the decl element would
 *       make the decl accept values the arms never produce.</li>
 *   <li><b>m-d2-terminal</b> ({@code FAH_PAIRS_MUT_TERMINAL} -- the empty/list-literal terminal
 *       INCLUDED in the ladder walk): CLAIMED failing set {@code a2}, {@code corpus_c1},
 *       {@code corpus_c2}, {@code corpus_control1}, {@code corpus_control3}. The synthetic
 *       empty-else folds as an unresolvable arm, the fold aborts, and the wildcard is lost -- the
 *       lane that proves the EXCLUSION is load-bearing.</li>
 * </ul>
 *
 * <p><b>LAW 77.</b> Both seats are legacy-handler / renderer owned and the IR route INHERITS.
 * {@code appendIteHoistChainCore} is {@code FunctionExpressionRenderer}-only; {@code rune-ir-java}
 * re-implements only the NAME seams ({@code IRFunctionExpressionRenderer.ifThenElseResultBaseName},
 * {@code thenArgBaseName}, {@code booleanHoistBaseName}, each asserted equal to the legacy
 * constant), and {@code IRControlFlowHandler} overrides only {@code ifThenElseResultBaseName} --
 * beneath which the fold extraction is a pure refactor. Measured at this head: all four
 * StrikePrice cells are byte-IDENTICAL between the OFF and ON dumps ({@code cmp}, 4/4 SAME), and
 * every {@code [P33-E4]} / {@code [P33-F6B]} histogram is byte-identical between {@code p33off}
 * and {@code p33on}. <b>Analytic is not measured:</b> {@code corpus_control2} compares the IR
 * route against GOLDEN and against the legacy route for both cells.
 *
 * <p><b>LAW 74.</b> D.2 is a COMPILE repair: {@code target/seat32-instruments/javac32/javac32-report.md}
 * section 7.4, row <b>C9 StrikePrice, STANDING 2 -> 0</b> ({@code Seat32Standing.java:571, 573:
 * incompatible types: MapperC<Price> cannot be converted to MapperC<PriceSchedule>}; the
 * element-typed arm is the control and does not error). <b>D.1 is a BYTE law -- PRE 0.</b>
 * Renaming a local changes no type, and section 7.4 attributes both of StrikePrice's standing
 * errors to the decl. <b>No commit message may say "cannot compile" of D.1.</b>
 *
 * <p><b>LAW-81 tripwires this heal WILL fire in OTHER suites</b> (not edited here; re-pinned from
 * their own prints, in this same commit). Both are driven by D.2's decl alone -- D.1's rename
 * moves NO pinned row anywhere in the tree (swept: every one of the 85 test sources that touches a
 * drr 7.x cell, every string literal in each, counted against the pre- and post-law StrikePrice
 * text).
 * <ul>
 *   <li>{@code AliasSigElementMetaSeatTest.KNOWN_RESIDUE_DRR7} -- the row
 *       {@code "drr/standards/iosco/cde/version1/price/functions/StrikePrice.java fork=[0, 0, 1, 0]
 *       golden=[0, 1, 1, 0]"} LEAVES (its T2, the non-meta {@code ? extends} decl count, rises
 *       0 -> 1 = golden's). {@code DOMAIN_DRR7 = 1585} is UNMOVED -- the file keeps a non-zero
 *       triple on both sides and stays inside the union domain.</li>
 *   <li>{@code WildcardLocalDeclSeatTest.KNOWN_RESIDUE_7} -- the row
 *       {@code "drr/standards/iosco/cde/version1/price/functions/StrikePrice.java fork=[0, 1, 4]
 *       golden=[1, 0, 4]"} LEAVES (T1 0 -> 1, T2 1 -> 0). {@code DOMAIN_DRR7 = 1862} UNMOVED.</li>
 * </ul>
 * The committed band lists ({@code ladder-census/band-off.tsv} / {@code band-on.tsv}) carry four
 * StrikePrice rows each and are NOT touched: {@code LadderBindingCensusTest.enforceBandGate} fails
 * only on an out-of-band candidate, so a list that stays a SUPERSET is inert. Their re-extraction
 * to the true residue is the seat's stage-exit commit, not this law's.
 *
 * <p><b>LAW 82 - MEASURED by the receipts chain, run 1 at ce1a06292 (final33.status; every
 * figure below is transcribed from the chain's own logs, never from this file's earlier
 * CLAIMED paragraphs, which it supersedes).</b> GREEN 10/0F/1skip default (f33-green-default.log) /
 * 10/0F/0skip {@code -Pir-on} (f33-green-on.log); RED at the pre-seat base {@code fa49da010}: default
 * 6F = a1, a2, corpus_c1, corpus_c2, corpus_control1, corpus_control3; {@code -Pir-on} 7F = a1, a2, corpus_c1, corpus_c2, corpus_control1, corpus_control2, corpus_control3 (f33-red-{default,on}.log).
 * Mutation lanes ({@code mut33.py}, the default profile; {@code lanes33.py --summary}):
 * <ul>
 *   <li><b>{@code m-lawD12-snapshot}</b> ({@code FER_PAIRS_MUT_SNAPSHOT}): MEASURED 10/5F/1skip = a1, corpus_c1, corpus_c2, corpus_control1, corpus_control3 - MATCH (a1, c1, c2, control1, control3; a2, e1, e2 held).</li>
 *   <li><b>{@code m-lawD12-site}</b> ({@code FER_PAIRS_MUT_SITE}): MEASURED 10/5F/1skip = a1, corpus_c1, corpus_c2, corpus_control1, corpus_control3 - MATCH - the same 5 as snapshot: the fix is the ORDER, as claimed.</li>
 *   <li><b>{@code m-lawD12-wildcard}</b> ({@code FER_PAIRS_MUT_WILDCARD}): MEASURED 10/5F/1skip = a2, corpus_c1, corpus_c2, corpus_control1, corpus_control3 - MATCH (a2, c1, c2, control1, control3; a1, e2 held).</li>
 *   <li><b>{@code m-lawD12-join}</b> ({@code FER_PAIRS_MUT_JOIN}): MEASURED 10/0F/1skip = (none) - EMPTY-as-declared (every firing row reads join == item).</li>
 *   <li><b>{@code m-lawD12-terminal}</b> ({@code FAH_PAIRS_MUT_TERMINAL}): MEASURED 10/5F/1skip = a2, corpus_c1, corpus_c2, corpus_control1, corpus_control3 - MATCH (a2, c1, c2, control1, control3).</li>
 * </ul>
 */
class StrikePriceIteLadderWholeSeatTest {

    static boolean irProviderOnClasspath() {
        try {
            Class.forName("com.regnosys.rosetta.generator.java.ir.IRGenerationProviderImpl");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static final String STRIKE_PRICE =
            "drr/standards/iosco/cde/version1/price/functions/StrikePrice.java";

    /** Cell A = drr 7.0.0. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");

    /**
     * Cell B = drr 7.3.0 -- a genuinely independent render (a different transitive CDM and
     * rune-fpml), not a copy of cell A's run.
     */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.3.0");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");

    /** The D.2 line, transcribed from golden drr 7.0.0 StrikePrice line 108. */
    private static final String GOLDEN_DECL = "final MapperC<? extends PriceSchedule> thenArg;";

    /** The fork-only line D.2 removes -- the invariant decl that does not compile. */
    private static final String FORK_DECL = "final MapperC<PriceSchedule> thenArg;";

    /**
     * The witness that {@code resultType} is NOT widened: golden's typed empty terminal keeps the
     * BARE element under the wildcard decl (golden drr 7.0.0 StrikePrice line 139).
     */
    private static final String GOLDEN_OFNULL = "thenArg = MapperC.<PriceSchedule>ofNull();";

    /** The first of D.1's five renamed hoists, transcribed from golden line 118. */
    private static final String GOLDEN_BOOL0 =
            "final Boolean boolean0 = isCap.evaluate(product(reportableEvent).get());";

    /** The fork's text for the same line at the seat-33 base (dump {@code p33-off}). */
    private static final String FORK_BOOL5 =
            "final Boolean boolean5 = isCap.evaluate(product(reportableEvent).get());";

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A);
    }

    static boolean cellBAvailable() {
        return Files.isDirectory(GOLDEN_B);
    }

    static boolean cellsAndIrProviderAvailable() {
        return cellAAvailable() && cellBAvailable() && irProviderOnClasspath();
    }

    // =========================================================================
    // The reduced fixture -- StrikePrice's shape, minus everything the law does
    // not need. A FUNCTION (never a rule: the #372 quadrant gates on
    // findEnclosingRule == null), a SET seat (never `add`: the quadrant gates on
    // !isAdd), a ladder with NO terminal else whose LAST rung's arm is MULTI (so
    // computeRuleBody(base) == MULTI and the FUNCTION+SET+MULTI quadrant is the
    // one that fires), TWO bare-function-call conditions (so the #179 boolHoist
    // registers TWO BOOLEAN names per render -- the minimum for the NUMBERED
    // form; a singleton group resolves to the escaped `_boolean`), and arms whose
    // elements are a proper SUBTYPE and its SUPERTYPE (`type Px extends Sched`,
    // the `Price extends PriceSchedule` shape). The ladder is the BASE of a
    // `then only-element`, exactly as in the carrier.
    // =========================================================================

    private static final String MODEL = """
            namespace census.seat33d12
            version "1.0.0"

            type Sched:
                ccy string (0..1)

            type Px extends Sched:
                factor number (0..1)

            type Leg:
                sched Sched (0..1)
                alt Sched (0..1)
                px Px (0..1)

            type Instr:
                leg Leg (0..1)
                legs Leg (0..*)
                flag boolean (0..1)

            func IsCapLike:
                inputs:
                    i Instr (1..1)
                output:
                    r boolean (1..1)
                set r:
                    i -> flag exists

            func IsFloorLike:
                inputs:
                    i Instr (1..1)
                output:
                    r boolean (1..1)
                set r:
                    i -> leg exists

            func A1D12StrikeLadder: <"a1/a2 - THE StrikePrice SHAPE: a FUNCTION whose `set` value is an elseless if/else-if ladder with TWO bare-function-call rungs and DISAGREEING arm elements (Sched, Px extends Sched), the ladder being the BASE of a `then only-element`">
                inputs:
                    instr Instr (1..1)
                output:
                    picked Sched (0..1)
                set picked:
                    (if IsCapLike(instr)
                    then instr -> leg -> sched
                    else if IsFloorLike(instr)
                    then instr -> legs -> px)
                    then only-element

            func E1D12AgreeingArms: <"e1 - D.2's DECLINE lock: the SAME ladder, the SAME two bare-fn rungs, but the arms AGREE (both Sched) so the fold reports no disagreement and the decl must stay INVARIANT">
                inputs:
                    instr Instr (1..1)
                output:
                    picked Sched (0..1)
                set picked:
                    (if IsCapLike(instr)
                    then instr -> leg -> sched
                    else if IsFloorLike(instr)
                    then instr -> legs -> alt)
                    then only-element

            func E2D12PlainCondAgreeingArms: <"e2 - the WHOLE-FILE decline lock: neither condition is a bare function call (so nothing registers on the session and the snapshot/restore is inert) and the arms agree (so the fold declines) - this render must be byte-identical either side of the law">
                inputs:
                    instr Instr (1..1)
                output:
                    picked Sched (0..1)
                set picked:
                    (if instr -> leg exists
                    then instr -> leg -> sched
                    else if instr -> flag exists
                    then instr -> legs -> alt)
                    then only-element
            """;

    // =========================================================================
    // a1 / a2 -- the reduced fixture, one render, one assertion each
    // =========================================================================

    /**
     * a1 -- <b>D.1's witness.</b> The ladder registers TWO BOOLEAN names per render, so the
     * discarded probe's leak is directly visible in the resolved names: pre-law the group holds
     * FOUR members and the kept pair resolves to {@code boolean2}/{@code boolean3}; post-law it
     * holds TWO and resolves to {@code boolean0}/{@code boolean1}, golden's law.
     *
     * <p><b>PIN AT RED.</b> The first two asserts hold at BOTH heads and exist so that a failure
     * is diagnosable: if the fixture ever stops reaching the #372 F-beta quadrant they fail FIRST
     * and the right response is to RESHAPE the fixture (the seat-32 E.3 lesson -- a reduction that
     * renders green before the law is a NON-WITNESS), never to weaken the name assert below them.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_hoistedBooleansNumberFromThePreProbeState() throws IOException {
        String out = fixtureFunction("A1D12StrikeLadder");
        assertContains(out, "thenArg = MapperC.<Sched>ofNull();");
        assertEquals(2, count(out, "final Boolean "),
                "the fixture must reach the #372 F-beta quadrant and hoist EXACTLY two Booleans"
                        + " (two bare-fn-call rungs = the minimum for the NUMBERED form):\n" + out);
        assertContains(out, "final Boolean boolean0 = isCapLike.evaluate(");
        assertContains(out, "final Boolean boolean1 = isFloorLike.evaluate(");
        assertTrue(!out.contains("boolean2"),
                "law D.1: the DISCARDED probe render's BOOLEAN registrations must be rolled back,"
                        + " so the real render numbers from 0 - not from the probe's leak:\n" + out);
    }

    /**
     * a2 -- <b>D.2's witness.</b> The two arms are {@code Sched} and {@code Px}
     * ({@code type Px extends Sched}), so they fold to the PROPER supertype {@code Sched}, which
     * IS the decl element; the local must therefore accept {@code MapperC<Px>} and be declared
     * {@code ? extends}. The {@code ofNull} assert is the {@code resultType}-untouched witness and
     * holds at BOTH heads.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_declWidensToTheDisagreeingArmsProperSupertypeJoin() throws IOException {
        String out = fixtureFunction("A1D12StrikeLadder");
        assertContains(out, "thenArg = MapperC.<Sched>ofNull();");
        assertContains(out, "final MapperC<? extends Sched> thenArg;");
        assertTrue(!out.contains("final MapperC<Sched> thenArg;"),
                "law D.2: an invariant decl cannot hold the Px arm - Java generics are invariant"
                        + " and the fork's form does not compile:\n" + out);
    }

    // =========================================================================
    // e1 / e2 -- the decline locks
    // =========================================================================

    /**
     * e1 -- <b>D.2's decline lock.</b> The same ladder, the same two bare-fn rungs, but both arms
     * are {@code Sched}: the fold reports {@code anyDisagree == false}, returns {@code null}, and
     * the decl must stay INVARIANT.
     *
     * <p>DELIBERATELY SCOPED: this render's BOOLEAN names DO move with D.1 (it has the same two
     * bare-fn rungs), so this test asserts only the DECL tokens -- the ones D.2's discriminator
     * owns. {@code e2} is the byte-unchanged whole-file lock.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_agreeingArmsKeepTheInvariantDecl() throws IOException {
        String out = fixtureFunction("E1D12AgreeingArms");
        assertContains(out, "final MapperC<Sched> thenArg;");
        assertTrue(!out.contains("MapperC<? extends "),
                "law D.2 must not widen a ladder whose arms AGREE - anyDisagree is the"
                        + " discriminator, not 'the fold resolved':\n" + out);
    }

    /**
     * e2 -- <b>the whole-file decline lock (bytes unchanged either side of the law).</b> Neither
     * condition is a bare function call, so {@code HandlerHelper.isBareFunctionCallCondition}
     * declines, nothing registers on the session during the probe, and D.1's restore is inert; and
     * the arms agree, so D.2's fold declines. The three asserts pin the seat as REACHED (the
     * typed-empty terminal and the Mapper-form decl) so a silently-declining quadrant cannot make
     * this lock vacuous.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e2_nonBareConditionsRegisterNothingAndTheDeclStaysInvariant() throws IOException {
        String out = fixtureFunction("E2D12PlainCondAgreeingArms");
        assertContains(out, "thenArg = MapperC.<Sched>ofNull();");
        assertContains(out, "final MapperC<Sched> thenArg;");
        assertEquals(0, count(out, "final Boolean "),
                "a ladder with no bare-fn-call condition registers no BOOLEAN name, so the"
                        + " snapshot/restore has nothing to roll back - and adds no line:\n" + out);
    }

    // =========================================================================
    // control0 -- golden is the oracle (prove the instrument can fail)
    // =========================================================================

    /**
     * control0 -- every token this suite asserts against the corpus is read back off GOLDEN first,
     * so a broken scan cannot pass quietly. Golden StrikePrice carries the wildcard decl, the BARE
     * typed empty under it, five hoisted Booleans numbered {@code boolean0..boolean4}, and carries
     * the fork's two band lines nowhere. The last two asserts pin the exact tuple
     * {@code corpus_control1} scans, on golden, in both cells.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenIsTheOracle() throws IOException {
        String g = normalize(Files.readString(GOLDEN_A.resolve(STRIKE_PRICE)));
        assertTrue(g.contains(GOLDEN_DECL), "golden must carry the wildcard decl");
        assertTrue(g.contains(GOLDEN_OFNULL),
                "golden must carry the BARE typed empty under the wildcard decl (resultType is"
                        + " NOT widened by this law)");
        assertTrue(g.contains(GOLDEN_BOOL0), "golden must carry boolean0 verbatim");
        assertTrue(!g.contains(FORK_DECL), "golden must NOT carry the fork's invariant decl");
        assertTrue(!g.contains(FORK_BOOL5), "golden must NOT carry the fork's boolean5");
        assertEquals(5, count(g, "final Boolean "), "golden hoists five Booleans");
        assertEquals(List.of(5, 5, 1), boxed(tuple(g)),
                "golden's (bool decls, max bool index + 1, wildcard decls) tuple - the pin"
                        + " corpus_control1 compares the fork against");
        if (cellBAvailable()) {
            assertEquals(List.of(5, 5, 1),
                    boxed(tuple(normalize(Files.readString(GOLDEN_B.resolve(STRIKE_PRICE))))),
                    "drr 7.3.0 golden carries the same tuple");
        }
    }

    // =========================================================================
    // The carriers -- whole-file byte compares in TWO cells
    // =========================================================================

    /**
     * c1 -- {@code StrikePrice} at drr 7.0.0, WHOLE. The four token asserts run AHEAD of the byte
     * compare so a D.1 regression reports as D.1 and a D.2 regression as D.2, rather than as a
     * 22-line diff.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr700StrikePriceMatchesGolden() throws IOException {
        assertCarrierWhole(drrAOutput, drrAGenErrors, GOLDEN_A, "drr 7.0.0");
    }

    /**
     * c2 -- the SECOND cell. drr 7.3.0 resolves against a different transitive CDM and rune-fpml
     * than 7.0.0, so this is an independent render of the same defect (7.1.0 and 7.2.0 carry the
     * same row; the sig is a 4-file sig).
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_drr730StrikePriceMatchesGolden() throws IOException {
        assertCarrierWhole(drrBOutput, drrBGenErrors, GOLDEN_B, "drr 7.3.0");
    }

    private static void assertCarrierWhole(Map<String, String> output, List<String> genErrors,
            Path goldenRoot, String cell) throws IOException {
        assertNotNull(output, cell + " generation did not run");
        List<String> own = genErrors.stream().filter(e -> e.contains(STRIKE_PRICE)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + STRIKE_PRICE + ": " + own);
        String gen = output.get(STRIKE_PRICE);
        assertNotNull(gen, "not generated: " + STRIKE_PRICE);
        gen = normalize(gen);
        assertTrue(gen.contains(GOLDEN_BOOL0),
                "law D.1 (" + cell + "): the hoisted Booleans must number from the PRE-PROBE"
                        + " state");
        assertTrue(!gen.contains(FORK_BOOL5),
                "law D.1 (" + cell + "): the discarded probe's five registrations must be gone");
        assertTrue(gen.contains(GOLDEN_DECL),
                "law D.2 (" + cell + "): the decl must widen to the disagreeing arms' join");
        assertTrue(!gen.contains(FORK_DECL),
                "law D.2 (" + cell + "): the invariant decl does not compile (javac C9)");
        assertTrue(gen.contains(GOLDEN_OFNULL),
                "law D.2 (" + cell + "): resultType is NOT widened - the typed empty keeps the"
                        + " bare element");
        assertEquals(normalize(Files.readString(goldenRoot.resolve(STRIKE_PRICE))), gen,
                "StrikePrice must byte-match golden at " + cell + " - seat 33 law D.12 closes the"
                        + " file's only two sigs (B003 + B014)");
    }

    // =========================================================================
    // The whole-cell UNION controls (LAW 79)
    // =========================================================================

    /**
     * control1 -- LAW 79, the whole-cell UNION scan on drr 7.0.0. Domain = every emitted file
     * whose GOLDEN <b>or</b> FORK text carries a hoisted Boolean or a wildcard local; per file the
     * TRIPLE must equal golden's beyond the NAMED residue.
     *
     * <p>The triple is chosen so that ONE control sees BOTH predicates:
     * <ul>
     *   <li><b>T1</b> -- {@code final Boolean } declarations (the population D.1 renames; a count
     *       alone cannot see a rename, which is what T2 is for);</li>
     *   <li><b>T2</b> -- the HIGHEST {@code final Boolean booleanK = } index in the file, plus one
     *       (0 when the file has none, and 0 for the escaped singleton {@code _boolean}). This is
     *       D.1's direct observable: golden's numbering is DENSE FROM ZERO, so a leaked probe
     *       registration shows up as T2 > T1. The fork's StrikePrice reads {@code [5, 10, 0]}
     *       against golden's {@code [5, 5, 1]}.</li>
     *   <li><b>T3</b> -- LOCAL declarations carrying {@code MapperS<? extends } /
     *       {@code MapperC<? extends } (D.2's direct observable, and the over-fire net for any
     *       decl this law widens that golden leaves invariant).</li>
     * </ul>
     * The control fails when the law OVER-fires (any green file's numbering or decl form moves)
     * and when it UNDER-fires (StrikePrice's row stays in the residue).
     *
     * <p>The GOLDEN-side domain is pinned exactly and is derivable WITHOUT the generator: a
     * read-only walk of {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/generated/java}
     * (7,808 goldens) finds <b>647</b> carrying a hoisted Boolean or a wildcard local
     * ({@code target/seat33-instruments/drafts33/D12/derive-domain.py}). The UNION domain also
     * depends on which of those the fork emits, so it carries the print-first SENTINEL until the
     * chain prints it.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr700WholeCellBoolNumberingAndWildcardDeclsEqualGolden()
            throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error - the scan is incomplete");
        Map<String, int[]> golden = scan(readGoldenTree(GOLDEN_A));
        assertEquals(GOLDEN_DOMAIN_DRR700, golden.size(),
                "the GOLDEN tree this control scans must be the frozen one (read-only walk: 647"
                        + " of 7808 goldens carry a hoisted Boolean or a wildcard local)");
        assertUnionEqual(scan(drrAOutput), golden, drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR700, UNION_DOMAIN_DRR700);
    }

    /**
     * control3 -- LAW 79 in the SECOND cell (drr 7.3.0). Same triple, same derivation: <b>647</b>
     * of 7,808 goldens. Its residue is EMPTY for the same reason cell A's is -- StrikePrice was
     * the only file in the cell whose triple differed at the base.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkDrr730WholeCellBoolNumberingAndWildcardDeclsEqualGolden()
            throws IOException {
        assertNotNull(drrBOutput, "drr 7.3.0 generation did not run");
        assertEquals(List.of(), drrBGenErrors,
                "drr 7.3.0 reported a generation error - the scan is incomplete");
        Map<String, int[]> golden = scan(readGoldenTree(GOLDEN_B));
        assertEquals(GOLDEN_DOMAIN_DRR730, golden.size(),
                "the GOLDEN tree this control scans must be the frozen one (read-only walk: 647"
                        + " of 7808 goldens carry a hoisted Boolean or a wildcard local)");
        assertUnionEqual(scan(drrBOutput), golden, drrBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR730, UNION_DOMAIN_DRR730);
    }

    /**
     * control2 -- LAW 77, BOTH-ROUTES-vs-GOLDEN. Both seats are renderer/handler owned and the IR
     * route delegates, so the ON route must reach the SAME bytes; the carrier is additionally
     * compared route-to-route so a shared-wrong render still reports as a route fact rather than
     * hiding inside a golden mismatch.
     */
    @Test
    @EnabledIf("cellsAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForBothCells() throws IOException {
        Map<String, String> irA = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT),
                new ArrayList<>());
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(STRIKE_PRICE))),
                normalize(irA.get(STRIKE_PRICE)),
                "IR route vs GOLDEN at drr 7.0.0: " + STRIKE_PRICE + " - if this fails while"
                        + " corpus_c1 passes, one of the two seats has an IR twin (LAW 77)");
        assertEquals(drrAOutput == null ? null : normalize(drrAOutput.get(STRIKE_PRICE)),
                normalize(irA.get(STRIKE_PRICE)), "route divergence at drr 7.0.0");
        Map<String, String> irB = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.3.0", CELL_B_ROOT),
                new ArrayList<>());
        assertEquals(normalize(Files.readString(GOLDEN_B.resolve(STRIKE_PRICE))),
                normalize(irB.get(STRIKE_PRICE)),
                "IR route vs GOLDEN at drr 7.3.0: " + STRIKE_PRICE);
    }

    // =========================================================================
    // Pins
    // =========================================================================

    /**
     * DERIVED by a read-only walk of the FROZEN golden tree at the seat-33 base -- the count of
     * drr 7.0.0 goldens whose triple is non-zero (647 of 7,808). Not a generator measurement: it
     * locks the corpus side of the control, so a corpus drift or a silently-empty golden read
     * fails loudly instead of making the union vacuous.
     */
    private static final int GOLDEN_DOMAIN_DRR700 = 647;

    /** DERIVED the same way for drr 7.3.0: 647 of 7,808. */
    private static final int GOLDEN_DOMAIN_DRR730 = 647;

    ///PIN: SENTINEL (-1) -- the print-first domain-pin discipline. The UNION domain is
    ///PIN: (golden-scan UNION fork-scan) INTERSECT emitted, so it cannot be derived without
    ///PIN: running the generator. The sentinel assert FAILS and PRINTS "MEASURED domain=N
    ///PIN: residue=[...]"; the lead transcribes N here, in the same commit, from that print.
    ///PIN: MEASURED 128 at the RED run (D12-red1.log, corpus_control1) - transcribed.
    private static final int UNION_DOMAIN_DRR700 = 128;

    ///PIN: SENTINEL (-1) -- as above, for drr 7.3.0.
    ///PIN: MEASURED 128 at the RED run (D12-red1.log, corpus_control3) - transcribed.
    private static final int UNION_DOMAIN_DRR730 = 128;

    /**
     * DERIVED at the seat-33 base from the OFF-route band dump: every NON-band file in the cell is
     * byte-identical to golden and therefore cannot contribute a row, so the residue is exactly
     * the band files whose TRIPLE differs. Over drr 7.0.0's six band files that set is
     * <b>StrikePrice alone</b> ({@code fork=[5, 10, 0] golden=[5, 5, 1]}) -- the other five
     * (GetBasketConstituents, Enrich_TransactionReportInstructionTestPackDefault, Price,
     * QuantityUnitOfMeasure, TotalNotionalQuantity) carry triples equal to their goldens' -- and
     * law D.12 removes it. <b>EMPTY, and this law must keep it so.</b>
     *
     * <p>TRANSCRIBE from the control's own print at the first chain run if it disagrees -- a
     * disagreement is a MEASUREMENT, not a licence to weaken the list.
     */
    private static final List<String> KNOWN_RESIDUE_DRR700 = List.of();

    /** DERIVED the same way for drr 7.3.0, band-for-band identical to cell A's. EMPTY. */
    private static final List<String> KNOWN_RESIDUE_DRR730 = List.of();

    // =========================================================================
    // The scan + the union assert (LAW 73: pin the SET, not the count)
    // =========================================================================

    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            int[] t = tuple(normalize(e.getValue()));
            if (t[0] + t[1] + t[2] > 0) {
                out.put(e.getKey(), t);
            }
        }
        return out;
    }

    /** (bool decls, max {@code booleanK} index + 1, wildcard local decls) -- see control1. */
    private static int[] tuple(String code) {
        int t1 = 0;
        int t2 = 0;
        int t3 = 0;
        for (String line : normalize(code).split("\n")) {
            String s = line.trim();
            if (s.startsWith("final Boolean ")) {
                t1++;
                int idx = numberedBooleanIndex(s);
                if (idx + 1 > t2) {
                    t2 = idx + 1;
                }
            }
            if (s.startsWith("final ")
                    && (s.contains("MapperS<? extends ") || s.contains("MapperC<? extends "))) {
                t3++;
            }
        }
        return new int[] {t1, t2, t3};
    }

    /**
     * The numeric suffix of a {@code final Boolean booleanK = } declaration, or {@code -1} for the
     * escaped singleton form ({@code final Boolean _boolean = }, which a one-member group
     * resolves to because {@code boolean} is a Java keyword) and for anything else.
     */
    private static int numberedBooleanIndex(String declLine) {
        String rest = declLine.substring("final Boolean ".length());
        if (!rest.startsWith("boolean")) {
            return -1;
        }
        int i = "boolean".length();
        int n = 0;
        boolean any = false;
        while (i < rest.length() && Character.isDigit(rest.charAt(i))) {
            n = n * 10 + (rest.charAt(i) - '0');
            i++;
            any = true;
        }
        return any && rest.startsWith(" = ", i) ? n : -1;
    }

    private static List<Integer> boxed(int[] t) {
        List<Integer> out = new ArrayList<>();
        for (int v : t) {
            out.add(v);
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
        int[] zero = new int[3];
        for (String key : universe) {
            int[] ac = a.getOrDefault(key, zero);
            int[] bc = b.getOrDefault(key, zero);
            if (!java.util.Arrays.equals(ac, bc)) {
                mismatched.add(key + " " + aName + "=" + java.util.Arrays.toString(ac)
                        + " " + bName + "=" + java.util.Arrays.toString(bc));
            }
        }
        // The domain-pin flow (LAW 73): the sentinel failure PRINTS the measured values.
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - transcribe from this"
                        + " print: MEASURED domain=" + universe.size()
                        + " residue=" + mismatched);
        assertEquals(knownResidue, mismatched,
                "the triple differs beyond the named residue in " + mismatched.size() + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted token-bearing files (" + expectedDomain
                        + ")");
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        int i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }

    // =========================================================================
    // Harness (the seat-26..32 suite shape verbatim)
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
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
        }
        if (cellBAvailable()) {
            List<String> errs = new ArrayList<>();
            drrBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.3.0", CELL_B_ROOT), errs);
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

    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " - " + e));
        }
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

    // =========================================================================
    // Fixture harness (the IteChainCtorArmMapperWrapSeatTest function renderer)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat33d12.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat33d12".equals(m.namespace()));
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        rendered = new Render(out, errors);
        return rendered;
    }

    private static String fixtureFunction(String fnName) throws IOException {
        Render r = render();
        String path = fnName + ".java";
        List<String> own = r.errors().stream().filter(e -> e.contains(path)).toList();
        assertTrue(own.isEmpty(), "the generator reported errors for " + path + ": " + own);
        String out = r.output().entrySet().stream()
                .filter(e -> e.getKey().endsWith(path))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
        assertNotNull(out, "not generated: " + path + " (have: " + r.output().keySet() + ")");
        return normalize(out);
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
            throw new AssertionError("[StrikePriceIteLadderWholeSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

}
