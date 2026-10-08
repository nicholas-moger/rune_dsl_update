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
 * SEAT 33, law D.3 -- facet {@code fnDeepCondBaseConfinedArmChainAdmit}: <b>a FUNCTION-path
 * deep then-chain whose BASE is a conditional ladder with control flow confined to admissible
 * arm chains restructures at the deep seat whatever its cardinality -- and the function-level
 * all-or-nothing guard must let the same chain through, or the admit delivers nothing.</b>
 *
 * <p><b>The carrier</b> -- {@code drr/standards/iosco/cde/version1/quantity/functions/
 * TotalNotionalQuantity.java} x drr 7.0.0/7.1.0/7.2.0/7.3.0 (sigs B021 B029 B031 B033 B034
 * B035 B036; 50 diff lines / 7 hunks per cell). Source: drr 7.x
 * {@code standards-iosco-cde-version1-quantity-func.rosetta:54-84}:
 * <pre>
 * set totalTotalNotionalQuantity:
 *     payout
 *         then extract
 *             ((if commodityPayout -&gt; priceQuantity -&gt; quantitySchedule exists
 *              then commodityPayout -&gt; priceQuantity -&gt; quantitySchedule
 *              ... five more meta-reference rungs ...
 *              else if tradeLot -&gt; priceQuantity -&gt; quantity -&gt; unit -&gt; financialUnit exists
 *              then (tradeLot -&gt; priceQuantity -&gt; quantity
 *                      then filter unit -&gt; financialUnit exists
 *                      then only-element))
 *              then extract (if datedValue exists then datedValue -&gt; value sum
 *                            else if multiplier exists then value * multiplier -&gt; value
 *                            else value)
 *             ) default defaultValue
 *         then FormatToShortFraction5DecimalNumber
 * </pre>
 *
 * <p><b>Golden vs fork (drr 7.0.0, inside the {@code thenArg1} lambda -- the outer scaffold
 * {@code thenArg0}/{@code thenArg1} is already byte-identical):</b>
 * <pre>
 * golden: final MapperS&lt;NonNegativeQuantitySchedule&gt; thenArg2;
 *         ... thenArg2 = item.&lt;CommodityPayout&gt;map(...).&lt;NonNegativeQuantitySchedule&gt;map(
 *                 "Type coercion", referenceWithMetaNonNegativeQuantitySchedule0 -&gt; ...);
 *         ... final MapperC&lt;FieldWithMetaNonNegativeQuantitySchedule&gt; _thenArg0 = item...;
 *             final MapperC&lt;FieldWithMetaNonNegativeQuantitySchedule&gt; _thenArg1 = _thenArg0
 *                 .filterItemNullSafe(_item -&gt; ...);
 *         } else { thenArg2 = MapperS.&lt;NonNegativeQuantitySchedule&gt;ofNull(); }
 *         return MapperS.of(thenArg2.mapSingleToItem(_item -&gt; { if (...) { return ...
 *                 .sumBigDecimal(); } ... }).getOrDefault(defaultValue));
 * fork:   final NonNegativeQuantitySchedule ifThenElseResult;
 *         ... final ReferenceWithMetaNonNegativeQuantitySchedule
 *                 referenceWithMetaNonNegativeQuantitySchedule0 = item...;
 *             ifThenElseResult = referenceWithMetaNonNegativeQuantitySchedule0 == null
 *                 ? null : referenceWithMetaNonNegativeQuantitySchedule0.getValue();
 *         ... ifThenElseResult = item...mapC("getQuantity", ...).then(_item -&gt; _item
 *                 .filterItemNullSafe(...)).then(_item -&gt; _item.get()).get();
 *         } else { ifThenElseResult = null; }
 *         return MapperS.of(ifThenElseResult.then(_item -&gt; _item.mapSingleToItem(_item -&gt;
 *                 ... ? ....sum() : ...).getOrDefault(defaultValue)));
 * </pre>
 *
 * <p><b>The defect, and why it is SEVEN rungs in ONE commit</b> (four drafted + three cut
 * in-seat from probe verdicts - the rung 2b/3b/5 entries below). Everything golden needs is
 * ALREADY implemented behind the deep-hoist handshake -- the Mapper-form blank final, the
 * in-chain {@code "Type coercion"} arm derefs ({@code ControlFlowHandler
 * .derefDeepThenIteArmOrKeep}), the typed {@code MapperS.<X>ofNull()} terminal
 * ({@code emptyElseValue}), the in-branch {@code _thenArg0}/{@code _thenArg1} drain, the
 * {@code MapperC} import, and the disappearance of the runtime {@code .then(}. The carrier
 * simply never arrives:
 * <ul>
 *   <li><b>rung 1 (the admit + its LOCKSTEP).</b> {@code tryDeepThenHoist}'s control-flow
 *       gate calls the 1-arg {@code thenChainHasUnhandledControlFlow}, whose conditional-BASE
 *       arm requires {@code MULTI || chainProvesMulti} before it will admit a FUNCTION-path
 *       confined ladder (#392) -- and this ladder is SINGLE. {@code [P33-B3]} measured
 *       {@code firstFail=ctlUnhandled ... baseCondCtl=true baseConfined=true baseCard=SINGLE
 *       baseProvesMulti=false ctlWithCompiler=true} on all 8 carrier rows (4 cells x 2
 *       renders), BOTH routes. Opening that gate ALONE delivers ZERO bytes: the same rows
 *       read {@code fnCtlValueThen=true}, i.e. the function-level all-or-nothing guard
 *       immediately behind it is also TRUE, so the chain moves from one decline to the next.
 *       Rung 1 is therefore TWO edits behind ONE shared predicate
 *       ({@code deepSeatCondBaseAdmitNewlyAdmits}, the #341 law), whose population is BY
 *       CONSTRUCTION the set of chains the gate newly admits.</li>
 *   <li><b>rung 2 (the name).</b> Golden's local is {@code thenArg2}, the THIRD member of the
 *       METHOD session group. The #381 two-channel naming discriminator declines on THREE
 *       conjuncts, all measured: {@code n=1}, {@code parentKind=RDefaultExpr bodyRootFn=false},
 *       and {@code methodThenArgGrp=1}. Without rung 2 the decl renders the right TYPE with
 *       the wrong NAME and the file is not whole.</li>
 *   <li><b>rung 3 (assert-only as DRAFTED - REFUTED, see rung 3b).</b> The in-branch
 *       {@code _thenArg0}/{@code _thenArg1} pair was claimed to fall out of rungs 1+2;
 *       measured ({@code [P33-D3Y]}), the ctl-free arm-interior chain never reached the deep
 *       seat - it died at the function-level all-or-nothing guard, because nothing pushed a
 *       restructure window on this route. Asserted in {@code a2}.</li>
 *   <li><b>rung 2b (in-seat) - {@code StatementHoistSession.registerAfterNext}.</b> The
 *       interior hoist registers MID-COMPILE of the enclosing SET level's value, so a plain
 *       register numbered it {@code thenArg1} and pushed the outer to {@code thenArg2} -
 *       golden is the reverse (upstream creates the outer declaration's identifier before
 *       compiling its initializer). The token attaches to the group immediately after the
 *       level's own registration; a still-pending token is snapshot/restore-covered and
 *       flushes at the tail if the level never registers (DISCLOSED in the session's javadoc:
 *       a pending consumed by an intervening register cannot be rolled back, and a tail flush
 *       into a singleton group renumbers its survivor - neither shape at this corpus).</li>
 *   <li><b>rung 3b (in-seat) - the together-restructure window.</b> The newly-admitted
 *       base compiles under {@code pushDeepThenRestructureChainTop}, exactly as the k==0
 *       plain-base compile pushes it - the #366 exemption then lets the arm-interior
 *       chains hoist and drain in-branch (golden's {@code _thenArg0}/{@code _thenArg1}).</li>
 *   <li><b>rung 5 (in-seat) - the text-order coercion WINDOW.</b> The handshake's arm
 *       coercions register DURING the base compile, the inner-ladder consumers AFTER, and
 *       golden numbers the whole {@code "Type coercion"} param group in emission order
 *       ({@code referenceWithMetaNonNegativeQuantitySchedule0..7}); the window
 *       ({@code JavaStatementScope.pushTextOrderCoercionWindow}) marks the registrations
 *       for law A.1's scoped text-order sort - one shared mechanism, extended.</li>
 *   <li><b>rung 4 (the inner block).</b> The inner ladder has TWO {@link
 *       com.regnosys.rosetta.ast.expressions.unary.RExtractExpr} ancestors, so
 *       {@code isCleanLadderContext} returns {@code extractCount <= 1} = false and the ladder
 *       renders the inline ternary. Rung 4 adds a THIRD one-shot transparency, gated by the
 *       SAME #350 {@code thenArgRefFor} render-truth read the arm beside it uses, so it can
 *       never reach a ladder that is not being restructured. {@code .sum()} ->
 *       {@code .sumBigDecimal()} is a CONSEQUENCE of rung 4 (the #383 block-arm statement
 *       seat) -- {@code [P33-SUM]} measured {@code typed=false} on 8 rows corpus-wide, ALL of
 *       them this carrier -- and the inner {@code _item} shadow disappears with rung 1's
 *       {@code .then(} lambda.</li>
 * </ul>
 *
 * <p><b>CLAIMED RED at the base head {@code fa49da010}</b> (measured by the chain, both
 * routes): {@code a1}, {@code a2}, {@code corpus_c1}, {@code corpus_c2},
 * {@code corpus_control1}, {@code corpus_control2}. <b>CLAIMED GREEN after</b>: all of them.
 * {@code e1} and {@code e2} were CLAIMED GREEN in BOTH states (decline locks) -- REFUTED for
 * {@code e2} by the receipts chain (the LAW-82 block below; {@code f33-red-default.log}): e2
 * fails at the base since its re-pin at B.24's census, because law B.1's window moved its
 * bytes and the re-pinned lock now asserts the per-scope forms POSITIVELY. e1 holds as claimed.
 *
 * <p><b>The mutation lanes</b> ({@code apply33.py D3 --mut CH_PAIRS_MUT_&lt;NAME&gt;}); LAW 76
 * requires each to move {@code corpus_control1}, and LAW 82 makes every failing set below a
 * CLAIM until the chain measures it:
 * <ul>
 *   <li>{@code m-lawD3-multi} (MULTI) -- restore the #392 cardinality requirement at the deep
 *       seat. CLAIMED: a1, a2, corpus_c1, corpus_c2, corpus_control1, corpus_control2.</li>
 *   <li>{@code m-lawD3-compiler} (COMPILER) -- ask the deep seat for the NARROW verdict again.
 *       CLAIMED IDENTICAL to {@code m-lawD3-multi} (a second, independent severance of the
 *       same admit) -- declared as a SUBSET lane, not an independent claim.</li>
 *   <li>{@code m-lawD3-lockstep} (LOCKSTEP) -- the #341 lane: widen the seat gate but NOT the
 *       guard. The chain must decline again at {@code allOrNothing} and the law must deliver
 *       nothing. CLAIMED: a1, a2, corpus_c1, corpus_c2, corpus_control1, corpus_control2.
 *       This lane must fail LOUDLY; a silent pass means the guard exemption was never
 *       load-bearing and the law's own rationale is wrong.</li>
 *   <li>{@code m-lawD3-name} (NAME) -- restore the unconditional {@code >= 2} method-group
 *       size. CLAIMED: a1, corpus_c1, corpus_c2, corpus_control1, corpus_control2.
 *       {@code a2} is NOT claimed (rungs 3/4 do not read the method group).</li>
 *   <li>{@code m-lawD3-afternext} ({@code CH2_PAIRS_MUT_AFTERNEXT}) -- rung 2b severed
 *       (plain register): CLAIMED a1 (the {@code thenArg2} token), corpus_c1, corpus_c2;
 *       corpus_control1 GREEN DECLARED (name-blind tuple scan - the A.1 textorder
 *       precedent).</li>
 *   <li>{@code m-lawD3-window} ({@code CH2_PAIRS_MUT_WINDOW}) -- rung 3b severed: CLAIMED
 *       a1 (zero-{@code .then(}), a2 (the in-branch decls), corpus_c1, corpus_c2,
 *       corpus_control1 (TNQ's tuple returns to the pre-law shape).</li>
 *   <li>{@code m-lawD3-textwindow} ({@code CH2_PAIRS_MUT_TEXTWINDOW}) -- rung 5 severed:
 *       CLAIMED corpus_c1, corpus_c2 (the coercion-param numbering); a1, a2, e1, e2 GREEN;
 *       corpus_control1 GREEN DECLARED (name-blind).</li>
 *   <li>{@code m-lawD3-extract} (EXTRACT) -- sever rung 4's transparency: the inner ladder
 *       returns to the inline ternary and {@code .sumBigDecimal()} reverts to {@code .sum()}
 *       (one lane proving rung 4 and the [P33-SUM] consequence together). CLAIMED: a2,
 *       corpus_c1, corpus_c2, corpus_control1, corpus_control2.</li>
 * </ul>
 *
 * <p><b>LAW 77 -- INHERITS, no IR twin.</b> {@code tryDeepThenHoist},
 * {@code thenChainHasUnhandledControlFlow} and {@code isCleanLadderContext} are all legacy
 * handler; {@code IRCollectionHandler} overrides only {@code thenArgBaseName} and
 * {@code IRControlFlowHandler} only {@code ifThenElseResultBaseName}, both asserted equal to
 * the legacy constants, and {@code [P33-B3]} printed the carrier's 8 rows identically on both
 * routes. {@code corpus_control2} re-proves it on the real IR seam.
 *
 * <p><b>LAW 74.</b> Repairs {@code javac33/javac32-report.md} 7.4 row <b>C10</b>. The row's
 * count of <b>1 is an UNDER-REPORT</b> and no claim here cites it: the carrier carries THREE
 * distinct non-compiling defects -- {@code .then(...)} invoked on a bare
 * {@code NonNegativeQuantitySchedule} (there is no {@code Mapper.then(Function)} member),
 * {@code .sum()} on a {@code MapperC<BigDecimal>} whose only runtime member is
 * {@code sumBigDecimal()}, and an inner {@code _item} lambda parameter shadowing the outer
 * {@code _item} (JLS 6.4) -- javac reported one because the first error kills the enclosing
 * expression. All three are healed by rungs 1 and 4.
 *
 * <p><b>LAW-81 tripwires this law fires in OTHER suites</b> (named here, edited there in the
 * SAME commit, transcribed from THEIR prints):
 * {@code DefaultRightNestedThenHoistSeatTest.KNOWN_RESIDUE_DRR7} loses its
 * {@code TotalNotionalQuantity} row ({@code fork=[3, 10, 0] golden=[0, 10, 2]});
 * {@code IteChainCtorArmMapperWrapSeatTest.KNOWN_DRR7_RESIDUE} loses its
 * {@code TotalNotionalQuantity} row; and the eighteen further TNQ residue rows listed in
 * NOTES.md. Nothing is edited from here.
 *
 * <p><b>LAW 82 - MEASURED by the receipts chain, run 1 at ce1a06292 (final33.status; every
 * figure below is transcribed from the chain's own logs, never from this file's earlier
 * CLAIMED paragraphs, which it supersedes).</b> GREEN 9/0F/1skip default (f33-green-default.log) /
 * 9/0F/0skip {@code -Pir-on} (f33-green-on.log); RED at the pre-seat base {@code fa49da010}: default
 * 6F = a1, a2, corpus_c1, corpus_c2, corpus_control1, e2; {@code -Pir-on} 7F = a1, a2, corpus_c1, corpus_c2, corpus_control1, corpus_control2, e2 (f33-red-{default,on}.log).
 * Mutation lanes ({@code mut33.py}, the default profile; {@code lanes33.py --summary}):
 * <ul>
 *   <li><b>{@code m-lawD3-multi}</b> ({@code CH_PAIRS_MUT_MULTI}): MEASURED 9/6F/1skip = a1, a2, corpus_c1, corpus_c2, corpus_control1, e2 - RE-SCORED (superset): e2 fell as well as the claimed a1, a2, c1, c2, control1 - e2 was re-pinned at B.24's census to POSITIVE per-scope tokens, so rung 1's admit is now one of its witnesses too.</li>
 *   <li><b>{@code m-lawD3-compiler}</b> ({@code CH_PAIRS_MUT_COMPILER}): MEASURED 9/6F/1skip = a1, a2, corpus_c1, corpus_c2, corpus_control1, e2 - RE-SCORED (superset, identical to multi as declared): a1, a2, c1, c2, control1, e2.</li>
 *   <li><b>{@code m-lawD3-lockstep}</b> ({@code CH_PAIRS_MUT_LOCKSTEP}): MEASURED 9/0F/1skip = (none) - RE-SCORED: EMPTY - the claim said this must fail LOUDLY; it does not, because law B.1 (landed one commit later) wraps every k>0 body compile in the #356 restructure window and the guard's #366 window exemption then covers the carriers whether or not D.3's own exemption conjunct stands. The conjunct is now defence-in-depth (measured); the #341 one-shared-predicate law holds by construction. The e2 bisect told the same story.</li>
 *   <li><b>{@code m-lawD3-name}</b> ({@code CH_PAIRS_MUT_NAME}): MEASURED 9/3F/1skip = a1, corpus_c1, corpus_c2 - RE-SCORED: a1, c1, c2 - control1 HELD (the tuple scan is name-blind, the reason the afternext and textwindow lanes declare).</li>
 *   <li><b>{@code m-lawD3-extract}</b> ({@code CH_PAIRS_MUT_EXTRACT}): MEASURED 9/3F/1skip = a2, corpus_c1, corpus_c2 - RE-SCORED: a2, c1, c2 - control1 HELD: rung 4's transparency changes the `.sum()` and decl text, not the scanned tuple.</li>
 *   <li><b>{@code m-lawD3-afternext}</b> ({@code CH2_PAIRS_MUT_AFTERNEXT}): MEASURED 9/3F/1skip = a1, corpus_c1, corpus_c2 - MATCH (a1, c1, c2; control1 held as declared).</li>
 *   <li><b>{@code m-lawD3-window}</b> ({@code CH2_PAIRS_MUT_WINDOW}): MEASURED 9/0F/1skip = (none) - RE-SCORED: EMPTY - rung 3's window push is subsumed by law B.1's k>0 window: with B.1 in place the arm-interior chains reach the deep seat through B.1's window whether or not rung 3 pushes its own, so a1, a2, c1, c2, control1 all held. Redundant defence-in-depth at this head (the same finding as lockstep, from the producer side).</li>
 *   <li><b>{@code m-lawD3-textwindow}</b> ({@code CH2_PAIRS_MUT_TEXTWINDOW}): MEASURED 9/2F/1skip = corpus_c1, corpus_c2 - MATCH (c1, c2; a1, a2, e1, e2 held; control1 held as declared).</li>
 * </ul>
 */
class FnDeepCondBaseConfinedArmChainAdmitSeatTest {

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

    /** The carrier: 7 sigs, 50 diff lines, 7 hunks, four cells. */
    private static final String TNQ =
            "drr/standards/iosco/cde/version1/quantity/functions/TotalNotionalQuantity.java";

    /**
     * corpus_c3 -- the GREEN no-move witness, and the reason rung 1's radius is STRUCTURAL
     * rather than merely unobserved. {@code [P33-B3]}'s whole
     * {@code rulePath=false baseCondCtl=true baseConfined=true} population is 18 rows over
     * THREE {@code where=}: the carrier (8) and {@code fn:Notional} / {@code fn:NotionalLeg}
     * (5 each). Both of the latter already read {@code ctlUnhandled=false} -- they
     * short-circuit TRUE at the #398 {@code condLadderCtlConfinedToNestedCondArms} disjunct
     * -- and {@code fn:Notional} is emitted into THIS cell. Its golden carries the very
     * machinery rung 1 opens ({@code final MapperC<? extends MeasureBase> thenArg0;} +
     * {@code thenArg0 = MapperC.<MeasureBase>ofNull();} at golden:191/209), so a widening
     * that reached one row too far would move it. Byte-identical in drr 7.0.0 and 7.3.0.
     */
    private static final String NOTIONAL =
            "drr/standards/iosco/cde/version1/quantity/functions/Notional.java";

    /** Cell A = drr 7.0.0 -- the carrier, the green lock, and the whole-cell control. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /** Cell B = drr 7.3.0 -- the law must hold across the whole 7.x family, not one cell. */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.3.0");
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
    // Fixtures.
    //
    // A1 is reduced from test-corpus/drr/drr-7.0.0/rosetta-source/src/main/rosetta/
    // standards-iosco-cde-version1-quantity-func.rosetta:54 `func TotalNotionalQuantity`.
    // TWO RESHAPES AT RED (the drafted reduction was refuted by measurement, never the
    // asserts weakened): (1) `schedRef` is [metadata address "pointsTo"=PqQ->sched] with a
    // located PqQ.sched - the drafted [metadata reference] made ALL arms share one meta so
    // the ladder JOIN kept the wrapper and the decl typed MapperS<ReferenceWithMetaSched>
    // (the carrier's quantitySchedule is address-meta, and its TradeLot arm navigates the
    // LOCATION-meta quantity, so the mixed-meta join strips to the bare type and the arms
    // coerce); (2) the last arm navigates `lots -> pq -> q -> sched` (the located
    // attribute), mirroring the carrier's `tradeLot -> priceQuantity -> quantity`.
    // It KEEPS every construct the four rungs read, at the same cardinalities:
    //   * the whole-output SET whose value is a then-chain with n == 2 (base + one extract
    //     body + the trailing bare-function rung) -- this is what makes the METHOD thenArg
    //     group {thenArg0, thenArg1} and golden's deep hoist its THIRD member;
    //   * the outer `then extract` whose lambda body is `(<chain>) default <input>` -- the
    //     measured `parentKind=RDefaultExpr bodyRootFn=false` seat rung 2 relaxes for;
    //   * an ELSELESS conditional LADDER base with SINGLE arms (the measured
    //     `baseCard=SINGLE baseProvesMulti=false`), whose LAST arm is a MULTI
    //     nav-then-filter-then-only-element chain (the confined arm chain -- the ctl the
    //     #392 disjunct declines on cardinality, and the source of golden's in-branch
    //     `_thenArg0`/`_thenArg1` pair);
    //   * `[metadata reference]` leaves on the ladder's navigations, so the arms need the
    //     in-chain `"Type coercion"` deref the handshake's arm pass produces;
    //   * the INNER ctl-free 3-rung ladder behind a SECOND extract, with a `sum` arm -- the
    //     two-extract-ancestor shape rung 4 is about and the [P33-SUM] `typed=false` seat.
    // It DROPS: four of the seven ladder rungs (they are the same construct as the two kept),
    // the report/rule scaffolding, and the real model's type hierarchy.
    //
    // E1 is A1 with a THEN-CHAIN moved into a rung CONDITION. That makes
    // `condLadderCtlConfinedToAdmissibleArmChains` return false at its first conjunct
    // (`subtreeHasControlFlow(cur.condition())`), so rung 1 must NOT admit and every byte
    // must be the pre-law render.
    //
    // E2 is A1 with the `default` operand REMOVED, so the chain is the extract lambda's body
    // ROOT. Rung 1 still admits it (rung 1 does not read the parent), but rung 2's
    // `fnDeepCondBaseSeat` and rung 4's transparency both require the `default` seat, so the
    // decl must take the PER-SCOPE name and the inner ladder must stay an inline ternary.
    // E2 is a SCOPING lock, not a byte-frozen one -- its bytes DO move with rung 1.
    // =========================================================================

    private static final String MODEL = """
            namespace census.seat33d3
            version "1.0.0"

            type Unit:
                kind string (0..1)

            type Dated:
                value number (0..1)

            type Meas:
                value number (0..1)

            type Sched:
                dated Dated (0..*)
                mult Meas (0..1)
                value number (0..1)
                unit Unit (0..1)

            type PqQ:
                sched Sched (0..*)
                    [metadata location]

            type Pq:
                q PqQ (0..1)
                schedRef Sched (0..1)
                    [metadata address "pointsTo"=PqQ->sched]

            type Lot:
                pq Pq (0..*)

            type Leg:
                aPay Pq (0..1)
                bPay Pq (0..1)
                lots Lot (0..*)

            func D3Fmt: <"the trailing then rung - it is what keeps the SET chain at n == 2, so the METHOD thenArg group is {thenArg0, thenArg1} when the deep seat runs">
                inputs:
                    v number (0..1)
                output:
                    f number (0..1)
                set f:
                    v

            func A1FnDeepCondBaseConfinedArmChain: <"a1 - reduced from drr 7.x standards-iosco-cde-version1-quantity-func.rosetta:54 func TotalNotionalQuantity">
                inputs:
                    leg Leg (0..1)
                    fallback number (1..1)
                output:
                    total number (0..1)
                set total:
                    leg
                        then extract
                            ((if aPay -> schedRef exists
                            then aPay -> schedRef
                            else if bPay -> schedRef -> unit -> kind exists
                            then bPay -> schedRef
                            else if lots -> pq -> q -> sched -> unit -> kind exists
                            then (lots -> pq -> q -> sched
                                then filter unit -> kind exists
                                then only-element))
                                then extract
                                    (if dated exists
                                    then dated -> value sum
                                    else if mult exists
                                    then value * mult -> value
                                    else value)
                                ) default fallback
                        then D3Fmt

            func E1CondCtlNotConfined: <"e1 - DECLINE: control flow in a rung CONDITION is not a confined arm chain, so the deep seat must keep declining">
                inputs:
                    leg Leg (0..1)
                    fallback number (1..1)
                output:
                    total number (0..1)
                set total:
                    leg
                        then extract
                            ((if (lots -> pq -> q -> sched
                                    then filter unit -> kind exists
                                    then only-element) exists
                            then aPay -> schedRef
                            else if bPay -> schedRef -> unit -> kind exists
                            then bPay -> schedRef)
                                then extract
                                    (if dated exists
                                    then dated -> value sum
                                    else if mult exists
                                    then value * mult -> value
                                    else value)
                                ) default fallback
                        then D3Fmt

            func E2NoDefaultSeat: <"e2 - SCOPING: the same chain as the lambda body ROOT (no default operand) - rung 1 admits it, rungs 2 and 4 must NOT fire">
                inputs:
                    leg Leg (0..1)
                output:
                    total number (0..1)
                set total:
                    leg
                        then extract
                            ((if aPay -> schedRef exists
                            then aPay -> schedRef
                            else if bPay -> schedRef -> unit -> kind exists
                            then bPay -> schedRef
                            else if lots -> pq -> q -> sched -> unit -> kind exists
                            then (lots -> pq -> q -> sched
                                then filter unit -> kind exists
                                then only-element))
                                then extract
                                    (if dated exists
                                    then dated -> value sum
                                    else if mult exists
                                    then value * mult -> value
                                    else value))
                        then D3Fmt
            """;

    // =========================================================================
    // Part A -- the seats (CLAIMED RED at the base).
    // =========================================================================

    /**
     * a1 -- rungs 1 and 2 together: the deep hoist happens AND it is numbered in the method
     * session group, exactly as golden's {@code thenArg2}.
     *
     * <p>PIN AT RED. The first four asserts are FIXTURE-REACH premises that are GREEN in both
     * states; if any of them fails the fixture is not reaching {@code renderThenExtractSetImpl}
     * / the deep seat at all, and the fixture must be RESHAPED -- never the assert weakened
     * (the seat-32 E.3 non-witness lesson). The law's own token is
     * {@code final MapperS<Sched> thenArg2;}: the pre-law render fails it because the fork
     * emits the POJO ladder local {@code final Sched ifThenElseResult;} instead, which is the
     * mechanism's reason and not a count that could fail for another cause.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_deepCondBaseSingleChainHoistsAndTakesTheMethodGroupName() throws IOException {
        String out = fixtureFunction("A1FnDeepCondBaseConfinedArmChain");
        assertContains(out, "final MapperS<Leg> thenArg0 = MapperS.of(leg);");
        assertContains(out, "thenArg1");
        assertContains(out, ".mapSingleToItem(item -> {");
        assertContains(out, ".getOrDefault(fallback)");

        assertContains(out, "final MapperS<Sched> thenArg2;");
        assertContains(out, "thenArg2 = MapperS.<Sched>ofNull();");
        assertTrue(!out.contains("ifThenElseResult"),
                "the POJO ladder local must be GONE -- golden hoists the Mapper-form blank"
                + " final instead:\n" + out);
        assertEquals(0, count(codeOnly(out), ".then("),
                "the runtime .then( form must be gone (zero of the 34,686 goldens carry"
                + " it):\n" + out);
    }

    /**
     * a2 -- rungs 3 and 4 on the SAME fixture: the confined arm chain's decls drain INSIDE
     * the owning branch as the escaped {@code _thenArg0}/{@code _thenArg1} pair (rung 3, no
     * code -- it must fall out of rungs 1+2), and the inner ladder block-converts, taking the
     * #383 typed sum with it (rung 4 + its measured consequence).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_armChainDrainsInBranchAndTheInnerLadderBlockConverts() throws IOException {
        String out = fixtureFunction("A1FnDeepCondBaseConfinedArmChain");
        assertContains(out, "_thenArg0");
        assertContains(out, "_thenArg1");
        assertTrue(count(out, "final MapperC<") >= 2,
                "rung 3: the arm chain's two levels must declare MapperC locals inside the"
                + " branch:\n" + out);
        assertContains(out, ".mapSingleToItem(_item -> {");
        assertContains(out, ".sumBigDecimal()");
        assertEquals(0, count(out, ".sum()"),
                "rung 4's consequence: a BLOCK-rendered ladder arm's sum renders at a return"
                + " statement seat and takes the typed sumBigDecimal (the #383 law;"
                + " [P33-SUM] typed=false is 8 rows corpus-wide, ALL this carrier):\n" + out);
    }

    // =========================================================================
    // Part B -- the decline locks.
    // =========================================================================

    /**
     * e1 -- the DISCRIMINATOR lock. Control flow in a rung CONDITION makes
     * {@code condLadderCtlConfinedToAdmissibleArmChains} false at its first conjunct, so the
     * newly-admitting disjunct never opens and BOTH halves of rung 1 stay shut. Bytes
     * unchanged in both states: the POJO ladder local and the runtime {@code .then(} survive.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_ctlInARungConditionIsNotAConfinedArmChain() throws IOException {
        String out = fixtureFunction("E1CondCtlNotConfined");
        assertContains(out, ".getOrDefault(fallback)");
        assertContains(out, "ifThenElseResult");
        assertTrue(!out.contains("thenArg2"),
                "e1 must NOT reach the method-group name -- its ladder is not confined:\n"
                + out);
    }

    /**
     * e2 -- the SCOPING lock for rungs 2 and 4. The same chain as the extract lambda's body
     * ROOT (no {@code default} operand): rung 1 admits it, but {@code fnDeepCondBaseSeat}
     * requires the {@code RDefaultExpr} parent whose {@code rawLeft()} is the chain, and
     * rung 4's transparency requires the enclosing extract's body root to be that same
     * {@code default}. So the decl must take the PER-SCOPE name and the inner ladder must
     * keep the inline ternary and its untyped {@code .sum()}.
     *
     * <p>This lock is NOT byte-frozen -- e2's bytes move with rung 1 (the hoist happens) --
     * and that is stated so a reviewer does not read it as one.
     *
     * <p><b>RE-SCORED at law B.24's live-row census (seat 33, B24-trip3.log).</b> The
     * original assert was {@code !out.contains("thenArg2")} -- rung 2's method-group token,
     * chosen when the third arm's confined chain ({@code lots -> pq -> q -> sched then filter
     * ... then only-element}) still rendered the runtime {@code .then(} form and the ladder
     * decl was the bare per-scope {@code thenArg} (the #386 singleton escape). Law B.1 (the
     * k > 0 restructure window, {@code 5c65810cc}) bisected as the mover: under the window the
     * arm-interior chain hoists INTO its branch exactly as rung 3's TNQ precedent describes
     * (golden drains arm-interior decls into the owning branch), and the lambda's per-scope
     * group becomes {@code _thenArg0} (the ladder decl), {@code _thenArg1} (the chain base)
     * and the BARE {@code thenArg2} (the filter step -- the #383 escape rule prefixes only a
     * name the enclosing scope already holds, and the method group ends at {@code thenArg1}).
     * So the token collided with a legitimately bare per-scope name. The lock now asserts
     * what it always meant: rung 2's declared-unassigned method-group form
     * {@code final MapperS<Sched> thenArg2;} is ABSENT and the per-scope numbered forms are
     * PRESENT; the in-branch hoist is pinned so any future move is a tripwire, not a drift.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e2_bodyRootChainWithoutADefaultSeatKeepsThePerScopeNameAndTheTernary()
            throws IOException {
        String out = fixtureFunction("E2NoDefaultSeat");
        assertTrue(!out.contains("final MapperS<Sched> thenArg2;"),
                "e2 must NOT continue the method thenArg group -- there is no default seat:\n"
                + out);
        assertContains(out, "final MapperS<Sched> _thenArg0;");
        assertContains(out, "final MapperC<FieldWithMetaSched> _thenArg1 = item.");
        assertContains(out, "final MapperC<FieldWithMetaSched> thenArg2 = _thenArg1");
        assertEquals(0, count(codeOnly(out), ".then("),
                "the arm-interior chain hoists in-branch under law B.1's k > 0 window - the"
                + " runtime .then( form (ZERO goldens carry it) must be gone:\n" + out);
        assertTrue(!out.contains(".mapSingleToItem(_item -> {"),
                "e2's inner ladder must keep the inline ternary -- rung 4's transparency"
                + " requires the default-tail shape:\n" + out);
        assertContains(out, ".sum()");
    }

    // =========================================================================
    // Part C -- the corpus (4 whole-file rows; cells A and B shown, 7.1/7.2 identical).
    // =========================================================================

    /** corpus_c1 -- the whole-file heal in cell A. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr700TotalNotionalQuantityMatchesGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(TNQ)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + TNQ + ": " + own);
        String gen = drrAOutput.get(TNQ);
        assertNotNull(gen, "not generated: " + TNQ);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(TNQ))), normalize(gen),
                "TotalNotionalQuantity must byte-match golden - seat 33 law D.3: the"
                + " FUNCTION-path SINGLE confined-arm-chain conditional base restructures at"
                + " the deep seat, the all-or-nothing guard lets the same chain through, the"
                + " hoist takes the method group's thenArg2, and the inner ladder"
                + " block-converts (7 sigs, 7 hunks, 50 diff lines)");
    }

    /** corpus_c2 -- the same whole-file heal in the LAST cell of the 7.x family. */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_drr730TotalNotionalQuantityMatchesGolden() throws IOException {
        assertNotNull(drrBOutput, "drr 7.3.0 generation did not run");
        String gen = drrBOutput.get(TNQ);
        assertNotNull(gen, "not generated: " + TNQ);
        assertEquals(normalize(Files.readString(GOLDEN_B.resolve(TNQ))), normalize(gen),
                "seat 33 law D.3 must hold across the whole drr 7.x family, not one cell");
    }

    /**
     * corpus_c3 -- LAW 69 / the measured green set. See {@link #NOTIONAL}: the ONE emitted
     * member of the widened predicate's own {@code baseConfined=true} population in this
     * cell, byte-frozen in both cells. It is asserted to carry the handshake machinery FIRST,
     * so the lock cannot pass vacuously if golden ever changes shape.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c3_drr700NotionalDeepHoistHandshakeIsByteFrozen() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        String golden = Files.readString(GOLDEN_A.resolve(NOTIONAL));
        assertContains(golden, "final MapperC<? extends MeasureBase> thenArg0;");
        assertContains(golden, "thenArg0 = MapperC.<MeasureBase>ofNull();");
        String gen = drrAOutput.get(NOTIONAL);
        assertNotNull(gen, "not generated: " + NOTIONAL);
        assertEquals(normalize(golden), normalize(gen),
                "the #398 nested-cond-arm green carrier must be byte-unchanged - it already"
                + " short-circuits TRUE at its own disjunct, so a new disjunct cannot reach"
                + " it (seat 33 law D.3, rung 1's structural radius argument)");
        if (drrBOutput != null) {
            assertEquals(normalize(Files.readString(GOLDEN_B.resolve(NOTIONAL))),
                    normalize(drrBOutput.get(NOTIONAL)),
                    "the green lock must hold in cell B too");
        }
    }

    // =========================================================================
    // corpus_control1 -- LAW 79, the UNION whole-cell scan.
    //
    // INSTRUMENT: the (T1, T2, T3) triple of DefaultRightNestedThenHoistSeatTest, reused
    // VERBATIM (the one-instrument law) because this law lives in the same F7 `.then(`
    // family and that suite's scan is already the family's census:
    //   T1  `.then(`          -- the REMOVED inline fallback. Golden's count is ZERO in
    //                            EVERY file of this cell (DERIVED: a read-only os.walk over
    //                            test-corpus/drr/drr-7.0.0/rosetta-source/src/generated/java
    //                            at head fa49da010 with a python transcription of `scan`
    //                            below -- 7,808 files, 3,607 token-bearing, T1 total 0), so
    //                            this column alone is a corpus-wide hoist census.
    //   T2  `.getOrDefault(`  -- the `default` seat's total. This law does not change it, so
    //                            a MOVED T2 is an over-fire at a seat it does not name.
    //   T3  `final MapperC<`  -- the ADDED hoist decls. The carrier moves T1 3 -> 0 and
    //                            T3 0 -> 2 (the in-branch _thenArg0/_thenArg1 pair).
    //
    // DOMAIN_DRR7 is the same universe DefaultRightNestedThenHoistSeatTest pins for the same
    // cell with the same instrument and the same harness (its DOMAIN_DRR7 = 1261, MEASURED at
    // the seat-30 head and green through seats 31 and 32). It is a PRINT-FIRST pin here too:
    // if it has moved, transcribe from this assert's own "MEASURED domain=" output.
    //
    // KNOWN_RESIDUE_DRR7 is DERIVED, file for file, from the seat-33 probe-round OFF band
    // dumps at fa49da010 (the six drr 7.0.0 band files; every other file in the cell is
    // byte-identical to golden by definition of the band). At the base head the residue is
    // THREE rows -- Price, QuantityUnitOfMeasure and TotalNotionalQuantity -- reproducing
    // DefaultRightNestedThenHoistSeatTest's own pinned list exactly. This law removes the
    // TotalNotionalQuantity row; the other two are named below with the seat-33 law that
    // closes each. That removal is control1's RED.
    // =========================================================================

    private static final int DOMAIN_DRR7 = 1261;

    /**
     * MEASURED-at-the-base minus this law's own row. {@code QuantityUnitOfMeasure} closes at
     * seat-33 laws B.1/B.3/B.24 and {@code Price} at C.1/C.2 -- both AFTER this commit, so
     * both rows stand here. PRINT-FIRST: transcribe any change from control1's own failing
     * print, never delete a row to make the assert pass.
     */
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of(
            // the Price row (fork=[0, 8, 1] golden=[0, 7, 1]) LEFT this list: law C.2 (heteroMetaDefaultJoinDeref + iteArmMultiDefaultTernary) took Price WHOLE in all four drr 7.x cells;
            // this suite was outside that law's LAW-81 batch list and the row was caught by the seat's
            // live-row CENSUS at B.24 (B24-trip3/4.log: `but was: <[]>`), a pure row removal.
            // the QuantityUnitOfMeasure row (fork=[3, 7, 1] golden=[0, 7, 4]) LEFT this list: law B.24 (defaultJoinHeteroMetaDerefBoth + iteArmMetaCollapseDerefSinkChannel + three in-seat rungs) took QUOM WHOLE in all four drr 7.x cells - the band's last four files;
            // this suite was outside that law's LAW-81 batch list and the row was caught by the seat's
            // live-row CENSUS at B.24 (B24-trip3/4.log: `but was: <[]>`), a pure row removal.
            );

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_drr700WholeCellThenHoistShapesEqualGoldenFileByFile()
            throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error - the scan is incomplete");
        assertTrue(drrAOutput.containsKey(TNQ) && drrAOutput.containsKey(NOTIONAL),
                "the carrier AND the green lock must be INSIDE this scan's domain, else"
                + " control1 proves nothing about either (LAW: a control scans the domain it"
                + " claims)");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR7, DOMAIN_DRR7);
    }

    /**
     * corpus_control2 -- LAW 77, per FILE, on the real IR seam. All three edited seats are
     * legacy-handler and the IR subclasses override neither, so the IR route must render the
     * carrier and the green lock golden-identical too. Skips unless the IR provider is on the
     * classpath ({@code -Pir-on}).
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForTheCarrierAndTheGreenLock() throws IOException {
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT),
                new ArrayList<>());
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(TNQ))),
                normalize(irOut.get(TNQ)), "IR route vs GOLDEN: " + TNQ);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(NOTIONAL))),
                normalize(irOut.get(NOTIONAL)), "IR route vs GOLDEN (the c3 lock): " + NOTIONAL);
    }

    // =========================================================================
    // The scan and the union assert -- DefaultRightNestedThenHoistSeatTest's, verbatim.
    // Character walks, no regex (the project's no-regex-on-structured-content rule).
    // =========================================================================

    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String flat = collapse(codeOnly(e.getValue()));
            int t1 = count(flat, ".then(");
            int t2 = count(flat, ".getOrDefault(");
            int t3 = count(flat, "final MapperC<");
            if (t1 + t2 + t3 > 0) {
                out.put(e.getKey(), new int[] {t1, t2, t3});
            }
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
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - transcribe from this"
                        + " print: MEASURED domain=" + universe.size()
                        + " residue=" + mismatched);
        assertEquals(knownResidue, mismatched,
                "(T1, T2, T3) differ beyond the named residue in " + mismatched.size()
                        + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted token-bearing files ("
                        + expectedDomain + ")");
    }

    /** OCCURRENCE count (the witness-uniqueness law -- never line counts). */
    private static int count(String haystack, String needle) {
        int n = 0;
        int from = 0;
        while ((from = haystack.indexOf(needle, from)) >= 0) {
            n++;
            from += needle.length();
        }
        return n;
    }

    private static Map<String, String> readGoldenTree(Path dir) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        try (var stream = Files.walk(dir)) {
            for (Path p : (Iterable<Path>) stream
                    .filter(q -> q.toString().endsWith(".java"))::iterator) {
                out.put(dir.relativize(p).toString().replace('\\', '/'), Files.readString(p));
            }
        }
        return out;
    }

    private static String collapse(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        boolean inWs = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
                inWs = true;
                continue;
            }
            if (inWs && sb.length() > 0) {
                sb.append(' ');
            }
            inWs = false;
            sb.append(ch);
        }
        return sb.toString();
    }

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

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

    // =========================================================================
    // Fixture harness (the IteChainCtorArmMapperWrapSeatTest function renderer, verbatim).
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat33d3.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat33d3".equals(m.namespace()));
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
            throw new AssertionError("[FnDeepCondBaseConfinedArmChainAdmitSeatTest] builtins"
                    + " parse failures: " + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // Corpus harness (the ReceiverRenderTypingSeatTest cell generator, verbatim).
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors = new ArrayList<>();
    private static Map<String, String> drrBOutput;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellAAvailable()) {
            List<String> errs = new ArrayList<>();
            drrAOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
        }
        if (cellBAvailable()) {
            drrBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.3.0", CELL_B_ROOT),
                    new ArrayList<>());
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

}
