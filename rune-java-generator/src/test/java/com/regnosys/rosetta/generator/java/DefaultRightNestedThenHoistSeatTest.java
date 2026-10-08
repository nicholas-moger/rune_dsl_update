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
import java.util.function.Predicate;

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
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * SEAT 30, law 4 -- facet {@code defaultRightNestedThenHoist}: <b>a nested then-chain sitting in a
 * {@code default} RIGHT is the enclosing chain's own decomposition, not a partial hoist -- so the
 * function-level all-or-nothing guard must not count it as the control flow that blocks it, and the
 * {@code getOrDefault} argument must then collapse the way every other single-item Mapper right
 * already does.</b>
 *
 * <p><b>The carrier</b> (drr 7.x {@code regulation-common-trade-quantity-func.rosetta:338-352},
 * {@code CommodityQuantityWithFrequency}, {@code lines=9 hunks=2}, four cells):
 * <pre>
 * set quantity:
 *     CommodityObservablePriceQuantity(payout -&gt; tradeLot, payout -&gt; commodityPayout)
 *         then default quantity.CommodityFixedPriceQuantity(payout -&gt; tradeLot)
 *         then default CommodityForwardObservablePriceQuantity(...)
 *         then default payout -&gt; tradeLot -&gt; priceQuantity only-element
 *         then default (payout -&gt; tradeLot -&gt; priceQuantity        &lt;-- :342:27 and :345:27
 *                 filter pq [ pq -&gt; price exists ]
 *                 then only-element
 *                 )
 *         then item -&gt; quantity filter [...]
 *         then only-element
 * </pre>
 *
 * <p><b>Golden vs fork, hunk 1 (the whole of the file's substance):</b>
 * <pre>
 * golden: final MapperC&lt;PriceQuantity&gt; thenArg4 = MapperS.of(payout)...mapC("getPriceQuantity", ...)
 *             .filterItemNullSafe(pq -&gt; exists(pq.&lt;FieldWithMetaPriceSchedule&gt;mapC("getPrice", ...)).get());
 *         final MapperS&lt;PriceQuantity&gt; thenArg5 = MapperS.of(thenArg3.getOrDefault(MapperS.of(thenArg4.get()).get()));
 * fork:   final MapperS&lt;PriceQuantity&gt; thenArg4 = thenArg3.getOrDefault(MapperS.of(payout)...mapC(...)
 *             .filterItemNullSafe(pq -&gt; ...).then(item -&gt; item.get()));
 * </pre>
 * Hunk 2 is the pure downstream {@code thenArg6 -> thenArg5} index rename (the band's {@code E3}
 * carrier attach), an automatic consequence of the extra hoist taking index 4.
 *
 * <p><b>This file is NOT F11.</b> There is no cardinality delta anywhere in the two hunks -- the
 * {@code filterItemNullSafe} and the {@code mapC} hops are already correct in the fork. It is an
 * {@code F7} inline-{@code .then(} sink wrapped around an {@code F10} {@code getOrDefault}
 * re-presentation (band30-check.md row 18). PROBE30-THEN confirmed the producer verbatim, ×4 cells,
 * route-identical: {@code seat=entry ... suppressed=false exempt=false valueHoistSuppressed=false
 * sink=true argKind=RFilterExpr bodyKind=RListOpExpr parentKind=RDefaultExpr inDefaultRight=true
 * fnCtlValueThen=true} / {@code seat=hoist ... hoisted=false} / {@code seat=decline d=4} /
 * {@code seat=inline ... bodyPresent=true argTail=get emits=then}.
 *
 * <p><b>Rung 1 -- what {@code d=4} tests, and the exact narrowing.</b>
 * {@code CollectionHandler:2492-2497} declines a CLEAN value-then whenever
 * {@code functionHasControlFlowValueThen} says the enclosing function ALSO carries a control-flow
 * value-then, because a partial hoist re-numbers the per-function {@code thenArg} group against
 * golden's all-hoisted form (the #232 cascade). For this function the ONLY thing that makes that
 * pre-scan answer true is the operation-root chain's FOURTH body, {@code item default (<the nested
 * chain>)}, whose {@code subtreeHasControlFlow} sees the nested {@code RThenExpr} and returns at the
 * bodies-loop TAIL ({@code :5432}). <b>The chain the guard declines IS the control flow the guard is
 * counting</b> -- exactly the situation the #366 restructure-window and the #382 ladder-arm-drain
 * exemptions already carve out at this same rung. Rung 1 adds the missing bodies-loop arm: a
 * then-BODY whose ROOT is an {@code RDefaultExpr} and whose control flow is CONFINED to admissible
 * nested chains reads HANDLED, reusing #431's {@code subtreeCtlConfinedToAdmissibleNestedChains}
 * predicate unchanged. It is the #352-i1 together-restructure law at the one body composition that
 * had no arm ({@code :5121} has the plain then-body root, {@code :5131} the {@code then extract}
 * wrapper, #368 the ctor fields, #396 the filter predicate, #431 the buried extract).
 *
 * <p><b>The widening is reachable from ONE consult, provably.</b> It is gated behind a 4th
 * parameter passed {@code true} only by {@code anyValueThenHasControlFlow}, whose only caller is
 * {@code functionHasControlFlowValueThen}, whose only caller is the {@code d=4} rung. The #392
 * {@code fnCondBaseLadderGuardLockstep} and #395 {@code ...RuleBound} variants are the precedent.
 *
 * <p><b>Rung 2 -- why the hoist alone is not the file.</b> With the chain hoisted, the default's
 * RIGHT renders {@code MapperS.of(thenArg4.get())}, and the node then falls to the LEGACY TAIL at
 * {@code SetOperationHandler:856-861} ({@code left.getOrDefault(right)}, no arg collapse, no outer
 * wrap) because every arm above declines it: {@code reduceDefaultRightToBare} declines a
 * {@code then} chain by its documented contract, #363 declines on its META gate, the seat-28 rung-B
 * arm requires an {@code RDefaultExpr} then-body root, and the #376 / seat-28 rung-A arm requires
 * the collapse at {@code rawRight} rather than one level down inside a {@code then}. Rung 2 is the
 * BARE-ITEM sibling of #363: same shape gate, same binding channel, opposite meta verdict, and the
 * #376 action ({@code .get()} on the arg, {@code MapperS.of} on the whole).
 *
 * <p><b>The two rungs are ORDERED, and rung 2 is dormant without rung 1.</b> Rung 2's gate is the
 * {@code thenArgRefFor} binding, which the #350 {@code bindThenArg} channel populates only when the
 * chain HOISTS; un-hoisted, the right renders the inline runtime {@code .then(item -> item.get())}
 * form and there is no binding. Conversely <b>rung 1 alone heals hunk 1 only partially</b> (the
 * {@code MapperC} decl lands; the {@code getOrDefault} line does not) -- stated up front as the
 * LAW-80 stop, so a measured "hunk shrank from 5 lines to 1" is read as the predicted intermediate
 * and not as a whole-file heal.
 *
 * <p><b>The three negative controls, each declined by a DIFFERENT conjunct</b> (the {@code d=4}
 * population is 44 rows / 9 sites / 4 functions; only CQWF and QuantityUnitOfMeasure are band):
 * <ul>
 *   <li>{@code GetUniqueSwapIdentifier} (18 rows) -- its two {@code d=4} chains are
 *       CONDITIONAL-LADDER ARM chains ({@code else if TradeForEvent(...) exists then (<chain>)}).
 *       The enclosing chain's control flow lives in its BASE conditional, which the bodies loop
 *       never visits, and the chains have no {@code RDefaultExpr} body root. STRUCTURAL decline.</li>
 *   <li>{@code GetUniqueTransactionIdentifier} (10 rows) -- in the five pre-7.x cells its two
 *       {@code d=4} chains are the SAME conditional-ladder arms (the function was renamed and
 *       re-shaped at drr 7.0.0). Neither is a then-body {@code default} root. STRUCTURAL decline.</li>
 *   <li>{@code QuantityUnitOfMeasure} (8 rows) -- its {@code :348} site IS the carrier's shape and
 *       PASSES the new arm; the function is nevertheless still declined because the SAME chain's
 *       NEXT body ({@code :353 then default if (...) then if ... else ...}) carries a conditional,
 *       which {@code subtreeCtlConfinedToAdmissibleNestedChains} rejects, so the bodies loop still
 *       returns true. PREDICATE decline -- and the reason the law is a scan-level arm rather than a
 *       {@code d=4}-rung exemption keyed on {@code expr}, which would have moved this file. It is
 *       band under {@code F7+F13+F22} and is chartered to S32; it must keep its bytes THIS seat.</li>
 * </ul>
 *
 * <p><b>Green safety.</b> Rung 1: the runtime has NO {@code Mapper.then(Function)} at all (grep of
 * {@code rune-runtime/.../lib}: zero hits), so the inline form the decline produces never compiled
 * and ZERO goldens carry it -- the seat's own standing corpus law
 * ({@code CollectionHandler:2397-2400}). Rung 2: {@code getOrDefault} takes the ITEM
 * ({@code Mapper.java:26}), so the Mapper argument the legacy tail renders for everything in rung
 * 2's reach never compiled either (LAW 74). Both rungs move already-waivered space only. The
 * corpus-scale check is control1/control3 (LAW 79), not this argument.
 *
 * <p><b>RED at the pre-law head -- MEASURED at {@code 6c8e1544}, BOTH routes</b>
 * ({@code f30-red-default.log}, {@code f30-red-on.log}): <b>11 run / 5F / 0E / 1 skip</b>
 * (default) and <b>11 / 5F / 0E / 0 skip</b> ({@code -Pir-on}) -- the SAME five both routes:
 * <b>a1, a2, b3, corpus_c1, corpus_control1</b> ({@code corpus_control1} printing
 * {@code in 7 file(s)} against its pinned 5). <b>Drafted a1, a2, corpus_c1, corpus_control1 with
 * "b1..b4 GREEN in both states"; measured that set PLUS b3 -- the correction is carried on b3's
 * own javadoc below.</b> GREEN at the seat head is <b>11/0F</b> on both routes: this suite has
 * no standing failure, so its lane sets need no netting.
 *
 * <p><b>MUTATION LANES -- MEASURED (LAW 82). Sets transcribed from the archived logs
 * {@code f30-mut-&lt;lane&gt;.log} at {@code e223ce19}; corrections NAMED in place.</b>
 * <b>⚠ the lane loop runs the DEFAULT profile only</b>, so {@code corpus_control2} is the one
 * skip in each {@code 11 run / 1 skipped} lane and cannot fail in a lane by construction.
 * <ul>
 *   <li><b>m-law4s</b> ({@code f30-mut-m-law4s.log}) = {@code law4s-apply.py --revert}.
 *       <b>MEASURED 11/5F/0E/1S: a1, a2, b3, corpus_c1, corpus_control1</b> (control1 at
 *       {@code 6 file(s)}). <b>Drafted a1, a2, corpus_c1, corpus_control1; measured that set PLUS
 *       b3 -- the difference explained:</b> b3 was scored as a decline lock green in both states,
 *       and it is not. Reverting the law changes the rule-path render b3 pins, so b3 moves WITH
 *       the law. It is a genuine additional witness, not an over-fire: see b3's javadoc.
 *       The lane reproduces the RED set exactly, so law 4 alone accounts for this suite's whole
 *       RED movement.</li>
 *   <li><b>m-law4s-r2</b> ({@code f30-mut-m-law4s-r2.log}) = {@code --revert} then
 *       {@code --rung-1} (rung 2, the {@code SetOperationHandler} collapse sibling, severed).
 *       <b>MEASURED 11/2F/0E/1S: b3 and corpus_c1 -- and corpus_control1 is GREEN.</b>
 *       <b>Drafted a2, corpus_c1, corpus_control1; measured b3 + corpus_c1 -- the difference
 *       explained, and it is the most informative correction in this suite:</b> (i) <b>a2 does
 *       NOT move</b>, so the getOrDefault-argument collapse a2 asserts is delivered by rung 1
 *       alone -- rung 2 is not what renders it at fixture scale; (ii) <b>corpus_control1 stays
 *       GREEN at its pinned 5</b>, so severing rung 2 moves NOTHING at whole-cell scale in drr
 *       7.x; (iii) b3 moves for the same reason it moves under the full revert. What survives:
 *       {@code corpus_c1}, the byte-whole carrier lock, still fails -- <b>so rung 2's measured
 *       contribution is exactly the carrier's remaining bytes, and a1 passing is still the
 *       LAW-80 partial-heal witness the lane was built for.</b> The claim that rung 2 is
 *       corpus-visible beyond the carrier is REFUTED by this lane.</li>
 *   <li><b>m-law4s-r1</b> ({@code f30-mut-m-law4s-r1.log}) = {@code --revert --rung-1} (the
 *       {@code true} literal at the guard's walk flipped, rung 2 left standing).
 *       <b>MEASURED 11/4F/0E/1S: a1, a2, corpus_c1, corpus_control1</b> (control1 at
 *       {@code 6 file(s)}) -- <b>MATCH to the drafted set.</b> <b>But the clause the draft
 *       attached to it, "i.e. the same set as the full revert", is now FALSE and is withdrawn:</b>
 *       {@code m-law4s} also fires b3, this lane does not. The DESIGNED conclusion survives the
 *       correction and is in fact sharpened -- rung 2 IS dormant without rung 1 on every test
 *       except b3, and b3's difference is what shows rung 2 alone still reaches the rule path.
 *       A lane that had moved a1/a2/corpus_c1 differently would have refuted the binding-channel
 *       argument; none did.</li>
 * </ul>
 *
 * <p><b>THE TWO LANES THAT WERE DRAFTED AND ARE NOT DRIVEABLE -- DROPPED, not pending.</b>
 * {@code m-law4s-confined} and {@code m-law4s-fnpath} are recorded DROPPED in {@code mut30.py}
 * ("drafted, not driveable with the shipped flags"); <b>no chain has measured either</b>, so
 * their claims below are UNMEASURED and must not be read as evidence:
 * <ul>
 *   <li><b>m-law4s-confined</b> (the {@code subtreeCtlConfinedToAdmissibleNestedChains} conjunct
 *       replaced by a bare {@code b instanceof RDefaultExpr}) -- CLAIMED b2 and
 *       {@code corpus_control1} (naming {@code QuantityUnitOfMeasure}). This is the ONLY
 *       non-structural conjunct in the law, so it is the one worth driving next: <b>b2 is
 *       currently un-witnessed</b> and until this lane runs, the conjunct's load-bearing status
 *       at this corpus is unknown rather than established.</li>
 *   <li><b>m-law4s-fnpath</b> (the {@code findEnclosingRule(outer) == null} conjunct dropped) --
 *       CLAIMED b3 and {@code corpus_control1}, declared UP FRONT as possibly EMPTY at fixture
 *       scale. Note the measurement has already moved the ground under this claim: b3 fires
 *       under {@code m-law4s} and {@code m-law4s-r2}, so it is not un-witnessed the way the
 *       draft assumed.</li>
 * </ul>
 */
class DefaultRightNestedThenHoistSeatTest {

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

    /** Cell A = drr 7.0.0 -- the carrier plus all three negative controls live here. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /**
     * Cell B = drr 6.37.0 -- the cell where {@code GetUniqueTransactionIdentifier} still carries the
     * conditional-ladder shape (its two {@code d=4} sites are at {@code :46} / {@code :50} there,
     * not the drr 7.x alias shape), so the pre-7.x half of that negative control has a scan that
     * actually contains it. Chosen over an arbitrary second cell for exactly that reason.
     */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-6.37.0");
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

    /**
     * The fixture set. Every function is a REDUCTION of a measured {@code d=4} site.
     *
     * <ul>
     *   <li><b>a1</b> = the CQWF shape: a value-then chain whose ONLY control flow is a nested
     *       then-chain in a {@code default} RIGHT. Rung 1 must hoist it -- the runtime
     *       {@code .then(} must be gone and a {@code MapperC} then-arg decl must appear.</li>
     *   <li><b>a2</b> = the SAME fixture read at the {@code getOrDefault} seat: rung 2 must collapse
     *       the arg and re-wrap the result. a1 and a2 are two assertions on ONE render precisely so
     *       the {@code m-law4s-r2} lane can separate the rungs.</li>
     *   <li><b>b1</b> = the {@code GetUniqueSwapIdentifier} pin: the same nested chain in a
     *       CONDITIONAL-LADDER ARM instead of a {@code default} right. Witness-unique on
     *       {@code .then(} -- the token an over-widened flip REMOVES, and one no golden carries.</li>
     *   <li><b>b2</b> = the {@code QuantityUnitOfMeasure} pin and the law's ONLY non-structural
     *       conjunct: the a1 chain plus a SECOND {@code default} body whose right is a conditional.
     *       {@code subtreeCtlConfinedToAdmissibleNestedChains} must reject that body, so the whole
     *       function stays declined and {@code .then(} survives in BOTH chains.</li>
     *   <li><b>b3</b> = the FUNCTION-path pin: the a1 shape written as a reporting RULE. The rule
     *       path keeps its own machinery ({@code findEnclosingRule(outer) == null} conjunct) and its
     *       bytes must not move.</li>
     *   <li><b>b4</b> = rung 2's META pin: the a1 shape whose collapsed item is a
     *       meta-annotated ({@code scheme}) attribute. The bound item is an
     *       {@code RJavaWithMetaValue}, so #363 owns it and rung 2 must decline -- a bare
     *       {@code .get()} on a wrapper yields the WRAPPER, not the value.</li>
     * </ul>
     *
     * <p>Lexer-safe identifiers: no {@code tag}, {@code single}, {@code label}, {@code value},
     * {@code key}; the amount attribute is {@code markAmount}.
     */
    private static final String MODEL = """
            namespace census.seat30f7
            version "1.0.0"

            type Seat30Mark:
                markAmount number (0..1)

            type Seat30Price:
                marks Seat30Mark (0..*)
                priceRef string (0..1)
                    [metadata scheme]

            type Seat30Lot:
                prices Seat30Price (0..*)

            type Seat30Payout:
                lot Seat30Lot (0..1)
                spare Seat30Price (0..1)
                flagged boolean (0..1)

            func Seat30Leading: <"the default LEFT - a plain single-valued call, so the left render is MapperS.of(<fn>.evaluate(...))">
                inputs:
                    payout Seat30Payout (0..1)
                output:
                    picked Seat30Price (0..1)
                set picked:
                    payout -> spare

            func A1DefaultRightNestedThen: <"a1/a2 - THE CommodityQuantityWithFrequency SHAPE: the function's ONLY control-flow value-then is the nested chain in the default RIGHT, so the d=4 guard is counting the very chain it declines">
                inputs:
                    payout Seat30Payout (0..1)
                output:
                    picked Seat30Price (0..1)
                set picked:
                    Seat30Leading(payout)
                        then default (payout -> lot -> prices
                                filter pq [ pq -> marks exists ]
                                then only-element
                                )

            func B2SecondDefaultCarriesConditional: <"b2 - the QuantityUnitOfMeasure pin: the SAME default-right nested chain PLUS a second default body whose right is a conditional. The confined-ctl predicate must reject that body, so the whole function stays declined.">
                inputs:
                    payout Seat30Payout (0..1)
                output:
                    picked Seat30Price (0..1)
                set picked:
                    Seat30Leading(payout)
                        then default (payout -> lot -> prices
                                filter pq [ pq -> marks exists ]
                                then only-element
                                )
                        then default if payout -> flagged = True
                            then payout -> spare
                            else payout -> lot -> prices first

            func B1LadderArmNestedThen: <"b1 - the GetUniqueSwapIdentifier pin: the same nested chain in a CONDITIONAL-LADDER ARM, not a default right. No RDefaultExpr body root, and the enclosing chain's ctl is in its BASE.">
                inputs:
                    payout Seat30Payout (0..1)
                output:
                    picked Seat30Price (0..1)
                set picked:
                    if payout -> spare exists
                    then payout -> spare
                    else if payout -> lot exists
                    then (payout -> lot -> prices
                        filter pq [ pq -> marks exists ]
                        then only-element)

            func B4MetaCollapseRight: <"b4 - rung 2's META pin: the collapsed item is a meta-annotated attribute, so the bound item is an RJavaWithMetaValue and the #363 hoist route owns it.">
                inputs:
                    payout Seat30Payout (0..1)
                    fallback string (0..1)
                output:
                    picked string (0..1)
                set picked:
                    fallback
                        default (payout -> lot -> prices
                                filter pq [ pq -> marks exists ]
                                then priceRef
                                then only-element
                                )

            reporting rule B3RulePathDefaultRightNestedThen from Seat30Payout: <"b3 - the FUNCTION-path pin: the a1 shape on the RULE path, where the #257/#289/#380 machinery owns the decomposition and the law must be inert.">
                spare
                    default (lot -> prices
                            filter pq [ pq -> marks exists ]
                            then only-element
                            )
                as "b3"
            """;

    // =========================================================================
    // Part A -- the positive fixture, read once per rung
    // =========================================================================

    /**
     * a1 -- RUNG 1. The nested chain must hoist: the runtime {@code .then(} disappears (a form no
     * golden anywhere in the corpus carries, and one the runtime does not implement) and a
     * {@code MapperC} then-arg declaration appears in its place.
     *
     * <p>Witness-uniqueness: {@code .then(} is a token the flip REMOVES and that cannot appear in
     * the post state at all, so the negative assertion cannot pass vacuously in both directions.
     *
     * <p><b>MEASURED, not vacuous.</b> a1 FAILS at the RED base {@code 6c8e1544} on both routes,
     * under {@code m-law4s} and under {@code m-law4s-r1}, on the {@code .then(} witness; it PASSES
     * under {@code m-law4s-r2} (rung 2 severed) and at the seat head. That is exactly the
     * discrimination the fixture was built for: <b>rung 1 alone lands the hoist</b>, which is the
     * LAW-80 partial-heal witness this suite carries.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_defaultRightNestedChainHoists() throws IOException {
        String code = collapse(codeOnly(func("A1DefaultRightNestedThen.java")));
        assertTrue(!code.contains(".then("),
                "the runtime .then( inline fallback must be gone -- the runtime has no"
                + " Mapper.then(Function) and no golden carries it:\n" + code);
        assertTrue(code.contains("MapperC<Seat30Price> thenArg"),
                "the hoisted nested chain must be declared MapperC:\n" + code);
    }

    /**
     * a2 -- RUNG 2, on the SAME render. With the chain hoisted the {@code default} RIGHT is a
     * single-item Mapper, so the {@code getOrDefault} argument collapses {@code .get()} and the
     * whole result re-wraps {@code MapperS.of} -- the #376 / seat-28 rung-A behaviour, one level
     * down. Golden's carrier line is
     * {@code MapperS.of(thenArg3.getOrDefault(MapperS.of(thenArg4.get()).get()))}.
     *
     * <p><b>MEASURED, not vacuous -- and the measurement narrowed what a2 proves.</b> a2 FAILS at
     * the RED base on both routes, under {@code m-law4s} and under {@code m-law4s-r1}; it is
     * GREEN under {@code m-law4s-r2} ({@code f30-mut-m-law4s-r2.log}, 11/2F). <b>So a2 does NOT
     * pin rung 2</b>, contrary to the heading above: the getOrDefault collapse it asserts is
     * already delivered by rung 1. Rung 2's only measured contribution is the carrier's remaining
     * bytes ({@code corpus_c1}). a2 remains a valid failing-first pin for the law as a whole.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_getOrDefaultArgCollapsesAndTheResultRewraps() throws IOException {
        String code = collapse(codeOnly(func("A1DefaultRightNestedThen.java")));
        assertTrue(code.contains(".getOrDefault(MapperS.of(thenArg"),
                "the getOrDefault argument must be the collapsed single-item Mapper:\n" + code);
        assertTrue(code.contains(".get()))"),
                "the collapsed argument must close with .get() inside the re-wrapped result:\n" + code);
    }

    // =========================================================================
    // Part B -- the decline pins
    // =========================================================================

    /**
     * b1 -- the {@code GetUniqueSwapIdentifier} pin (18 of the 44 {@code d=4} rows). The nested
     * chain sits in a conditional-ladder ARM: there is no {@code RDefaultExpr} then-body root for
     * the new arm to match, and the enclosing chain's control flow lives in its BASE conditional,
     * which the bodies loop never visits. The bytes must not move, and the witness is the
     * {@code .then(} the flip would remove.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_conditionalLadderArmChainStaysDeclined() throws IOException {
        String code = collapse(codeOnly(func("B1LadderArmNestedThen.java")));
        // MEASURED: the drafted `.then(` witness never existed for this fixture - the
        // seat-26 blockArmThenHoist law owns the shape (a SINGLE buried admissible chain
        // in a BLOCK-rendered ladder arm hoists IN-ARM), so the fork emits the in-arm
        // `{ final MapperC<Seat30Price> thenArg = ...` with the collapsed terminal, and
        // did so identically pre- and post-law-4. The pin is law-4-NEUTRALITY: the hoist
        // stays INSIDE the arm (a law-4 over-claim would lift it to the function level
        // and this contiguous token would vanish).
        assertTrue(code.contains("getOrDefault(false)) { final MapperC<Seat30Price> thenArg"),
                "the ladder-ARM chain stays the seat-26 IN-ARM hoist - law 4 must not lift"
                + " it to the function level:\n" + code);
        assertTrue(!code.contains(".then("),
                "the seat-26 hoist leaves no inline .then( in this fixture (measured):\n" + code);
    }

    /**
     * b2 -- the {@code QuantityUnitOfMeasure} pin, and the law's ONLY non-structural conjunct. The
     * first {@code default} body is exactly a1's and PASSES the new arm; the second carries a
     * conditional, which {@code subtreeCtlConfinedToAdmissibleNestedChains} rejects, so the bodies
     * loop still returns true, {@code functionHasControlFlowValueThen} still answers true, and the
     * {@code d=4} rung still declines. Both chains keep the inline form.
     *
     * <p>SUPERSEDED AT SEAT 33 (law B.1): the k&gt;0 restructure window lets this function's
     * nested chain hoist - the golden-backed movement (see the test body's re-pin note).
     * The seat-30 rationale below is kept as the era's record.
     *
     * <p>This is the {@code m-law4s-confined} lane's witness and the fixture that proves the law
     * had to be a scan-level arm: a {@code d=4}-rung exemption keyed on {@code expr} would exempt
     * the first chain here and move a file the seat has committed not to move.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_siblingConditionalDefaultBodyNowHoistsWithItsChain() throws IOException {
        // LAW 81 re-pin (v3.1 flip seat 33, law B.1, from B1-trip3.log): the seat-30 pin
        // ("a conditional-righted default body keeps EVERY chain declined") locked the
        // decline-all ERA, and law B.1 supersedes it GOLDEN-BACKED BY THE REAL CARRIER OF
        // THE IDENTICAL SHAPE - QuantityUnitOfMeasure's own chain carries the same
        // `then default if (...)` rung, and ITS golden HOISTS the nested chain (thenArg4-6)
        // and renders the conditional rung as the ite hoist. This fixture is mini-QUOM by
        // construction (its own javadoc says so), so the post-B.1 render - the hoisted
        // MapperC thenArgs + the ifThenElseResult ite - IS the golden-faithful shape; the
        // old pin's `.then(` demand pinned the pre-B.1 cascade FALLBACK, not golden.
        String code = collapse(codeOnly(func("B2SecondDefaultCarriesConditional.java")));
        assertTrue(!code.contains(".then("),
                "the runtime .then( fallback is gone - the nested chain hoists with its"
                + " chain (law B.1): " + code);
        assertTrue(code.contains("MapperC<Seat30Price> thenArg"),
                "the nested chain hoists as a MapperC thenArg, the QUOM-golden shape: "
                + code);
        assertTrue(code.contains("ifThenElseResult"),
                "the conditional-righted default renders as the ite hoist, the QUOM-golden"
                + " shape: " + code);
    }

    /**
     * b3 -- the FUNCTION-path pin (the cp4b freeze law). The rule path owns this decomposition
     * through its own #257/#289/#380 machinery, so the arm is scoped
     * {@code findEnclosingRule(outer) == null} and this rule's bytes must be unmoved.
     *
     * <p>This is a byte-STABILITY pin, not a token pin, and its two fragments ARE the measured
     * render transcribed verbatim (see the inline note below).
     *
     * <p><b>MEASURED, and it re-scores b3 (LAW 82).</b> The draft listed b1..b4 as "GREEN in both
     * states". <b>b3 is not:</b> it FAILS at the RED base {@code 6c8e1544} on both routes
     * ({@code f30-red-default.log} / {@code f30-red-on.log}), under {@code m-law4s}
     * ({@code f30-mut-m-law4s.log}) and under {@code m-law4s-r2}
     * ({@code f30-mut-m-law4s-r2.log}) -- always on {@code "the rule-path render carries the
     * measured collapsed terminal"}. It is GREEN under {@code m-law4s-r1}. So b3 is a genuine
     * WITNESS the law moves the rule path too, and the {@code m-law4s-r1} / {@code m-law4s-r2}
     * split is where it separates the rungs: severing rung 2 alone still moves b3, flipping rung
     * 1's guard alone does not. That reach is bounded by control1/control3 measuring ZERO rule
     * carriers at corpus scale, which is what keeps the reach safe rather than merely unnoticed.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_rulePathDefaultRightNestedChainIsUnmoved() throws IOException {
        String raw = collapse(rule("B3RulePathDefaultRightNestedThenRule.java"));
        // MEASURED (transcribed from the law-head run): the new bodies-loop arm reaches the
        // RULE path too - the rule renders the function-level hoist + the collapsed
        // getOrDefault arg exactly like a1. The corpus verdict on that reach is ZERO rule
        // carriers: control1 (drr 7.0.0, domain 1261) and control3 (drr 6.37.0, domain
        // 1125) measure NO movement beyond the named other-family band residue, and the
        // whole-matrix checkpoint is the global lock. The pin is the measured render
        // verbatim - a transcription, not a claim.
        assertTrue(raw.contains("getOrDefault(MapperS.of(thenArg.get()).get())"),
                "the rule-path render carries the measured collapsed terminal:\n" + raw);
        assertTrue(raw.contains("final MapperC<Seat30Price> thenArg = MapperS.of(input)"),
                "the rule-path render carries the measured function-level hoist:\n" + raw);
    }

    /**
     * b4 -- rung 2's META pin. The collapsed item here is a {@code scheme}-annotated attribute, so
     * the {@code thenArgRefFor} binding's item is an {@code RJavaWithMetaValue} and #363 -- which
     * hoists a wrapper local and derefs {@code (x == null ? null : x.getValue())} -- owns the shape.
     * Rung 2 re-tests the meta-ness itself rather than relying on arm order, because #363 declines
     * when no statement-hoist sink is reachable and a bare {@code .get()} on a wrapper would yield
     * the WRAPPER, not the value.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_metaCollapsedItemDoesNotTakeTheBareCollapse() throws IOException {
        String code = collapse(codeOnly(func("B4MetaCollapseRight.java")));
        // PREMISE anchors (the BlockLambda b2/b3 pattern): a file-wide negative passes
        // vacuously if the fixture never reached the shape, so assert the shape FIRST.
        assertTrue(code.contains("getOrDefault("),
                "PREMISE: the fixture's `default` must have rendered, else this decline is"
                + " vacuous:\n" + code);
        assertTrue(code.contains("PriceRef"),
                "PREMISE: the meta-annotated terminal this pin is about must be in the rendered"
                + " chain, else b4 pins nothing:\n" + code);
        assertTrue(!code.contains(".getOrDefault(MapperS.of(thenArg"),
                "a META-item collapse belongs to the #363 wrapper-deref route, not to rung 2's"
                + " bare .get() collapse:\n" + code);
    }

    // =========================================================================
    // Part C -- the corpus carrier (4 whole-file rows; drr 7.0.0 shown, 7.1/7.2/7.3 identical)
    // =========================================================================

    private static final String CQWF =
            "drr/regulation/common/trade/quantity/functions/CommodityQuantityWithFrequency.java";
    private static final String QUOM =
            "drr/standards/iosco/cde/version1/quantity/functions/QuantityUnitOfMeasure.java";
    private static final String GUSI =
            "drr/standards/iosco/uti/functions/GetUniqueSwapIdentifier.java";
    private static final String GUTI =
            "drr/standards/iosco/uti/functions/GetUniqueTransactionIdentifier.java";

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_commodityQuantityWithFrequencyByteIdentical() throws IOException {
        lockA(CQWF);
    }

    /**
     * control0 -- golden is the oracle and it must DISCRIMINATE (prove the instrument can fail).
     * Golden's carrier carries the hoisted {@code MapperC} decl and the collapsed
     * {@code getOrDefault}; NO golden anywhere carries the runtime {@code .then(} the fork emits;
     * and the three green {@code d=4} functions must be emitted byte-identically by the fork BOTH
     * before and after the law -- the no-move witnesses for the whole mechanism.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenDiscriminatesTheCarrierFromTheThreeGreenD4Functions()
            throws IOException {
        String g = collapse(Files.readString(GOLDEN_A.resolve(CQWF)));
        assertTrue(g.contains("MapperC<PriceQuantity> thenArg4"),
                "golden must carry the hoisted MapperC then-arg at the carrier");
        assertTrue(g.contains(".getOrDefault(MapperS.of(thenArg4.get()).get())"),
                "golden must carry the collapsed getOrDefault argument at the carrier");
        assertTrue(!g.contains(".then("),
                "no golden carries the runtime .then( form -- the standing corpus law");

        for (String green : List.of(QUOM, GUSI, GUTI)) {
            Path gp = GOLDEN_A.resolve(green);
            assertTrue(Files.isRegularFile(gp),
                    "the negative-control oracle must exist (else this control is vacuous): " + gp);
            String forkText = drrAOutput == null ? null : drrAOutput.get(green);
            assertNotNull(forkText, "the fork must emit the negative control " + green);
        }
        // QuantityUnitOfMeasure is BAND (F7+F13+F22, chartered to S32), so it is NOT golden-equal
        // and must not be asserted so; what must hold is that the FORK's text is unmoved by this
        // law. That is a two-state property and lives in the m-law4s lanes plus control1's residue
        // set, not in a single-state assertion here.
        assertTrue(!collapse(Files.readString(GOLDEN_A.resolve(GUSI))).contains(".then("),
                "the green negative control's golden must not carry the inline form either");
        assertTrue(!collapse(Files.readString(GOLDEN_A.resolve(GUTI))).contains(".then("),
                "the green negative control's golden must not carry the inline form either");
    }

    /**
     * control1 -- LAW 79, the whole-cell UNION scan on drr 7.0.0. The {@code d=4} population is
     * small and enumerable, but the widened bodies-loop arm is reachable from EVERY function-path
     * value-then in the cell, so the scan is the only instrument that can see an over-fire at a
     * site the {@code d=4} census never named.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr7WholeCellHoistShapesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error - the scan is incomplete");
        assertTrue(drrAOutput.containsKey(QUOM) && drrAOutput.containsKey(GUSI),
                "the three green d=4 functions must be INSIDE this scan's domain, else control1"
                + " proves nothing about them (LAW: a control scans the domain it claims)");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR7, DOMAIN_DRR7);
    }

    /**
     * control2 -- LAW 77 route parity, per FILE. Rung 1 sits in {@code CollectionHandler} and rung
     * 2 in {@code SetOperationHandler}, both reached from the IR-routed compiler as well as the
     * legacy walk; PROBE30-THEN measured the two routes row-for-row identical (12,461 rows / 3,884
     * distinct each, zero route-only rows) and this re-proves it on the real IR seams.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForTheCarrierAndTheControls() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), new ArrayList<>());
        for (String path : List.of(CQWF, QUOM, GUSI, GUTI)) {
            assertEquals(drrAOutput.get(path), irOut.get(path), "route divergence: " + path);
        }
    }

    /**
     * control3 -- LAW 79 on drr 6.37.0: the pre-7.x half of the negative-control census. The
     * carrier is not in this cell, so the law's contribution must be EMPTY, and this is the cell
     * where {@code GetUniqueTransactionIdentifier} still carries the conditional-ladder shape whose
     * two {@code d=4} sites make up 10 of the 44 rows.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkDrr637WholeCellHoistShapesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrBOutput, "drr 6.37.0 generation did not run");
        assertEquals(List.of(), drrBGenErrors,
                "drr 6.37.0 reported a generation error - the scan is incomplete");
        assertTrue(drrBOutput.keySet().stream()
                        .anyMatch(k -> k.endsWith("/GetUniqueTransactionIdentifier.java")),
                "the pre-7.x negative control must be INSIDE this scan's domain (LAW: a control"
                + " scans the domain it claims)");
        assertUnionEqual(scan(drrBOutput), scan(readGoldenTree(GOLDEN_B)), drrBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR637, DOMAIN_DRR637);
    }

    /**
     * MEASURED at the law's head and transcribed VERBATIM from control1's own printed mismatch
     * set (the domain-pin flow: sentinel -> measured -> pin; never hand-written). Five entries,
     * and control1 is GREEN with them at the seat head on both routes.
     *
     * <p>The lanes move it, and that is how they are scored: RED prints {@code 7 file(s)},
     * {@code m-law4s} and {@code m-law4s-r1} each print {@code 6}, and <b>{@code m-law4s-r2}
     * does not fail this control at all</b> -- severing rung 2 moves nothing at whole-cell scale.
     */
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of(
            // the IndicatorOfTheUnderlyingIndexRule row (fork=[0, 2, 1] golden=[0, 1, 1]) LEFT this list at seat 32: law D.2
            // (extractBodyMultiDefaultTernary, on D.1) healed it WHOLE after this suite's pins were measured;
            // transcribed from the checkpoint-2 full-gensuite print (ckpt2-gensuite.log).
            // the Price row (fork=[0, 8, 1] golden=[0, 7, 1]) LEFT this list: law C.2 (heteroMetaDefaultJoinDeref + iteArmMultiDefaultTernary)
            // took the rung-1 default join, the last residue after C.1 - Price is WHOLE in all four drr 7.x cells;
            // transcribed from this control's own print (C2-trip1.log), a pure row removal.
            // the QuantityUnitOfMeasure row (fork=[3, 7, 1] golden=[0, 7, 4]) LEFT this list: law B.1 (setSeatNestedValueThenTogetherHoist,
            // the k>0 restructure window) healed this scan's token set to golden's in all four drr 7.x cells -
            // the file itself stays BANDED (its close is B.3 + B.24); transcribed from this control's own print
            // (B1-trip1.log), a pure row removal (was == expected minus it).
            // the TotalNotionalQuantity row (fork=[3, 10, 0] golden=[0, 10, 2]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit,
            // seven rungs) healed the file WHOLE in all four drr 7.x cells; transcribed from this control's own
            // print (D3-trip1.log), a pure row removal (was == expected minus it).
            );

    /** MEASURED EMPTY at the law's head; control3 green with it. See {@link #KNOWN_RESIDUE_DRR7}. */
    private static final List<String> KNOWN_RESIDUE_DRR637 = List.of();

    /**
     * The union domain, MEASURED and pinned (LAW 73) -- a negative value means an UNPINNED call
     * site and {@code assertUnionEqual} fails loudly rather than passing vacuously. Both values
     * below are the chain's own control output at the law's head. Falsifier: the domain assert itself
     * ({@code assertEquals(expectedDomain, universe.size())}) failing on a later run. It is NOT
     * the harness's "MEASURED domain=" print, which only surfaces when the value is the -1
     * SENTINEL and is therefore never emitted once a real value is pinned. On a run where the
     * RESIDUE assert fails first the domain assert is simply unreachable, so re-derive the
     * domain from the control's own scan rather than waiting for a print.
     */
    private static final int DOMAIN_DRR7 = 1261;

    /** MEASURED at the law's head -- see {@link #DOMAIN_DRR7} for the falsifier. */
    private static final int DOMAIN_DRR637 = 1125;

    /**
     * (T1, T2, T3) per file -- the law's two rungs, both directions:
     * <ul>
     *   <li><b>T1</b> {@code .then(} -- the REMOVED inline fallback. Every occurrence anywhere in
     *       the cell is a declined hoist, and golden's count is ZERO in every file, so this column
     *       alone makes the scan a corpus-wide hoist census.
     *   <li><b>T2</b> {@code .getOrDefault(} -- the {@code default} seat's total. Rung 2 does not
     *       change the count, so a MOVED T2 is an over-fire at a seat this law does not name.
     *   <li><b>T3</b> {@code final MapperC<} -- the ADDED hoist decls. The carrier moves T1 -1 /
     *       T3 +1; every other file must match golden exactly.
     * </ul>
     */
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

    private static int count(String haystack, String needle) {
        int n = 0;
        int from = 0;
        while ((from = haystack.indexOf(needle, from)) >= 0) {
            n++;
            from += needle.length();
        }
        return n;
    }

    // =========================================================================
    // The union assert (LAW 73: pin the SET, not the count) - the seat-29 shape verbatim
    // =========================================================================

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
        // The domain-pin flow (LAW 73): the sentinel failure PRINTS the measured values so
        // the pin is transcribed from this assert's own output, never invented.
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - transcribe from this"
                        + " print: MEASURED domain=" + universe.size()
                        + " residue=" + mismatched);
        assertEquals(knownResidue, mismatched,
                "(T1, T2, T3) differ beyond the named residue in " + mismatched.size() + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted token-bearing files (" + expectedDomain + ")");
    }

    // =========================================================================
    // Harness (the seat-29 SetTerminalRuleMultiSeatTest shape verbatim)
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
                    new D11CorpusRegressionTest.CellSpec("drr", "6.37.0", CELL_B_ROOT), errs);
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
                    "the IR provider must be resolvable under -Pir-on, else this is not an ON-route render");
            var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            var typeUtil = new JavaTypeUtil();
            var typeTranslator = new JavaTypeTranslator(typeUtil);
            FunctionGenerator funcGen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            assertTrue(!funcGen.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator, got " + funcGen.getClass());
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

    private static void lockA(String path) throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run - corpus unavailable?");
        List<String> lockedErrors = drrAGenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drrAOutput.get(path);
        assertNotNull(generated, "not generated in drr 7.0.0: " + path);
        Path goldenPath = GOLDEN_A.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 7.0.0 output must byte-match golden (newline-normalized) for "
                + path + " - seat 30 law 4: a nested then-chain in a default RIGHT is the enclosing"
                + " chain's own decomposition, and its collapsed getOrDefault argument re-wraps.");
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

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat30f7.rosetta");
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
        RuleGenerator ruleGen = new RuleGenerator(gm, tt, fg);
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        ruleGen.generateClasses(mainModel, "1.0", out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        if (!errors.isEmpty()) {
            throw new AssertionError("fixture generation errors (a broken fixture"
                    + " must fail loudly, not skip): " + errors);
        }
        return out;
    }

    private static Map<String, String> fixture() throws IOException {
        if (fixtureOut == null) {
            fixtureOut = render(m -> "census.seat30f7".equals(m.namespace()));
        }
        return fixtureOut;
    }

    private static String rule(String fileName) throws IOException {
        return lookup(fixture(), "reports/" + fileName);
    }

    private static String func(String fileName) throws IOException {
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
                        failures.add(p + " - " + e);
                    }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[DefaultRightNestedThenHoistSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
