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
 * SEAT 33, law B.1 -- facet {@code setSeatNestedValueThenTogetherHoist} (census family F7):
 * <b>the SET seat's restructure window is pushed for the chain's BASE but not for its step
 * BODIES, so a nested clean value-then inside a {@code k > 0} body is judged a partial hoist
 * by the function-level all-or-nothing guard even though the chain it sits in IS the
 * restructure in progress.</b> An ~8-line evening-out of the {@code k} fork, not a new
 * predicate.
 *
 * <p><b>THIS LAW IS AN ENABLER: whole ceiling ALONE is ZERO.</b>
 * {@code QuantityUnitOfMeasure} goes whole only with the seat's later laws B.3
 * ({@code bareRuleRefFunctionHost}) and B.24 (B.2 + B.4), and B.1 is a HARD PRECONDITION for
 * B.2, whose right operand IS B.1's hoisted collapse ({@code thenBoundCollapseItem},
 * {@code SetOperationHandler:1496-1498}). The exact residue this law leaves is pinned in
 * {@link #KNOWN_RESIDUE_QUOM}, each row named to the law that flips it.
 *
 * <p><b>The carrier</b> -- {@code drr/standards/iosco/cde/version1/quantity/functions/
 * QuantityUnitOfMeasure.java} x drr 7.0.0/7.1.0/7.2.0/7.3.0 (sigs B018 part, B028, B022 name;
 * golden is byte-identical across the four cells). Source: drr 7.x
 * {@code standards-iosco-cde-version1-quantity-func.rosetta:338-360} -- a SET-value then-chain
 * of six {@code default} rungs whose FIFTH rung's right operand is a nested chain:
 * <pre>
 * then default (payout -&gt; tradeLot -&gt; priceQuantity -&gt; quantity
 *         then filter unit -&gt; financialUnit exists
 *         then filter quantity.UnitOfMeasureFromQuantity exists
 *         then only-element
 *         )
 * then default if (... then distinct count = 1) then if ... then ... else ...
 * </pre>
 *
 * <p><b>Golden vs fork (drr 7.0.0):</b>
 * <pre>
 * golden: final MapperC&lt;FieldWithMetaNonNegativeQuantitySchedule&gt; thenArg4 = MapperS.of(payout)...
 *         final MapperC&lt;FieldWithMetaNonNegativeQuantitySchedule&gt; thenArg5 = thenArg4
 *             .filterItemNullSafe(item -&gt; ...);
 *         final MapperC&lt;FieldWithMetaNonNegativeQuantitySchedule&gt; thenArg6 = thenArg5
 *             .filterItemNullSafe(item -&gt; ...);
 * fork:   final MapperS&lt;FieldWithMetaNonNegativeQuantitySchedule&gt; thenArg4 =
 *             thenArg3.getOrDefault(MapperS.of(payout)....filterItemNullSafe(...)
 *                 .filterItemNullSafe(...)).then(item -&gt; item.get()));
 * </pre>
 *
 * <p><b>The decline, MEASURED</b> ({@code [P33-QF7]}, probe round 2, BOTH routes, row for row,
 * 8 carrier rows = 4 cells x 2 passes): {@code verdict=DECLINE-ctlGuard ctlValueThen=true
 * hasSink=true nameTaken=false restructureTop=false n=3 baseKind=RFeatureCall
 * lastBodyKind=RListOpExpr parentKind=RDefaultExpr thenArgGroup=4}. The row excludes both
 * rival gates counterfactually -- {@code hasSink=true} kills the {@code :2808}
 * {@code !hasSink && !lambdaChannel} decline and {@code nameTaken=false} kills the
 * {@code :2823} name-taken decline -- and {@code thenArgGroup=4} is exactly where golden's
 * {@code thenArg4} starts. The guard's only OTHER declining files are
 * {@link #GUSI} (18 rows) and {@link #GUTI} (10 rows), both {@code hasSink=false
 * inLambda=false grp=-1 n=1 REnumValueRef} conditional-ladder ARM chains at a {@code k == 0}
 * position -- outside this window.
 *
 * <p><b>CLAIMED RED at the pre-law head</b> (= the seat's head after law D.3;
 * {@code fa49da010} + the D.3 commit): {@code a1}, {@code corpus_c1}, {@code corpus_c2},
 * {@code corpus_control1}, {@code corpus_control2}. <b>CLAIMED GREEN after</b>: all seven
 * tests. {@code e1} and {@code corpus_c3} are CLAIMED GREEN in BOTH states.
 *
 * <p><b>The mutation lanes</b> ({@code apply33.py B1 --mut FER_PAIRS_MUT_&lt;NAME&gt;}):
 * <ul>
 *   <li>{@code m-lawB1-window} (WINDOW) -- remove the {@code k > 0} push (the pre-law form,
 *       verbatim). CLAIMED: a1, corpus_c1, corpus_c2, corpus_control1, corpus_control2.
 *       {@code e1} and {@code corpus_c3} must stay GREEN -- the lane's whole point is that
 *       the window is the ONLY thing that moved.</li>
 *   <li>{@code m-lawB1-fnpath} (FNPATH) -- drop the explicit FUNCTION-path scoping so the
 *       window is pushed on the RULE path too (the over-fire lane for the #383 per-scope
 *       literal naming). CLAIMED: corpus_control1's residue grows. <b>DECLARED HONESTLY: this
 *       lane MAY BE EMPTY at this corpus</b> -- no probe sized the rule-path population of
 *       {@code k > 0} bodies carrying a nested clean value-then. If the chain measures it
 *       empty, say so (LAW 82) and KEEP the conjunct: it is scoping by construction, not by
 *       census.</li>
 * </ul>
 *
 * <p><b>LAW 77 -- INHERITS.</b> {@code IRFunctionExpressionRenderer} overrides exactly four
 * methods ({@code ifThenElseResultBaseName}, {@code thenArgBaseName},
 * {@code booleanHoistBaseName}, {@code renderSetAssignmentStatement}) and NOT
 * {@code renderThenExtractSetImpl}; {@code [P33-QF7]} printed the carrier's 8 rows identically
 * on both routes. {@code corpus_control2} re-proves it per FILE on the real IR seam.
 *
 * <p><b>LAW 74.</b> Repairs {@code javac33/javac32-report.md} 7.4 row <b>C7 line 470</b> --
 * {@code cannot find symbol: method then(...)}, location {@code class
 * MapperC<FieldWithMetaNonNegativeQuantitySchedule>} ({@code standing-javac.txt:15}), x4
 * cells. That re-attribution away from seat-32's law B.3 is CONFIRMED by the transcript: the
 * receiver is a nav chain in a {@code default} operand, not a hoisted
 * {@code ifThenElseResult}. C7's other seven errors belong to B.3 (472, 474, 487 x2), B.4
 * (479, 482) and B.2 (485) and are NOT claimed here.
 *
 * <p><b>DISCLOSED GAP.</b> {@code [P33-QF7]} sized the GUARD, not the WINDOW.
 * {@code findDeepThenRestructureChainTop} has five other consumers, FOUR of them reachable by
 * this push and UNSIZED ({@code CollectionHandler:6935} / {@code :11026} / {@code :11113},
 * {@code ReferenceHandler:6643}; {@code ReferenceHandler:6765} is rule-path-gated and safe).
 * {@code corpus_control1} and the whole-matrix checkpoint are the only instruments that can
 * see them -- no claim of "radius zero" is made anywhere in this suite.
 *
 * <p><b>The no-move witnesses are NOT duplicated here.</b>
 * {@code DefaultRightNestedThenHoistSeatTest.corpus_control0} (at its {@code :577}) already
 * pins {@code QuantityUnitOfMeasure} + {@link #GUSI} + {@link #GUTI} as "the three green d=4
 * functions ... the no-move witnesses for the whole mechanism", and its {@code corpus_control1}
 * is the same LAW-79 union scan this suite reuses. That lock is named, not copied;
 * {@code corpus_control1} below asserts all three are INSIDE its domain so the reuse is real.
 *
 * <p><b>LAW 82 - MEASURED by the receipts chain, run 1 at ce1a06292 (final33.status; every
 * figure below is transcribed from the chain's own logs, never from this file's earlier
 * CLAIMED paragraphs, which it supersedes).</b> GREEN 7/0F/1skip default (f33-green-default.log) /
 * 7/0F/0skip {@code -Pir-on} (f33-green-on.log); RED at the pre-seat base {@code fa49da010}: default
 * 5F = a1, corpus_c1, corpus_c2, corpus_c3, corpus_control1; {@code -Pir-on} 5F = a1, corpus_c1, corpus_c2, corpus_c3, corpus_control1 (f33-red-{default,on}.log).
 * Mutation lanes ({@code mut33.py}, the default profile; {@code lanes33.py --summary}):
 * <ul>
 *   <li><b>{@code m-lawB1-window}</b> ({@code FER_PAIRS_MUT_WINDOW}): MEASURED 7/4F/1skip = a1, corpus_c1, corpus_c2, corpus_control1 - MATCH (a1, c1, c2, control1; e1 and c3 held - the discriminating lane).</li>
 *   <li><b>{@code m-lawB1-fnpath}</b> ({@code FER_PAIRS_MUT_FNPATH}): MEASURED 7/0F/1skip = (none) - EMPTY-as-declared (may-be-empty at draft; measured 0F - no rule-path carrier at this corpus).</li>
 * </ul>
 */
class SetSeatNestedValueThenTogetherHoistSeatTest {

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

    /** The carrier -- IMPROVED, not whole (see {@link #KNOWN_RESIDUE_QUOM}). */
    private static final String QUOM =
            "drr/standards/iosco/cde/version1/quantity/functions/QuantityUnitOfMeasure.java";

    /**
     * The seat's own OTHER F7 carrier, taken WHOLE by law D.3 immediately before this commit.
     * The charter requires it in this law's whole-cell control: D.3's chain is compiled inside
     * the very {@code k > 0} body this window now wraps, so after B.1 it is exempt from the
     * all-or-nothing guard TWICE (by D.3's own conjunct and by the window). That redundancy is
     * harmless, but the window's four unsized consumers could still move the file -- so it is
     * byte-locked here.
     */
    private static final String TNQ =
            "drr/standards/iosco/cde/version1/quantity/functions/TotalNotionalQuantity.java";

    /** The guard's other declining file (18 rows), {@code hasSink=false} -- outside the window. */
    private static final String GUSI =
            "drr/standards/iosco/uti/functions/GetUniqueSwapIdentifier.java";
    /** The guard's other declining file (10 rows), {@code hasSink=false} -- outside the window. */
    private static final String GUTI =
            "drr/standards/iosco/uti/functions/GetUniqueTransactionIdentifier.java";

    /** Cell A = drr 7.0.0 -- the carrier, the witnesses and the whole-cell control. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /** Cell B = drr 7.3.0 -- golden QuantityUnitOfMeasure is byte-identical to cell A's. */
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
    // standards-iosco-cde-version1-quantity-func.rosetta:338 `func QuantityUnitOfMeasure`.
    // It KEEPS every construct the decline reads, at the same cardinalities:
    //   * a FUNCTION whose whole-output SET value is a then-chain, so the render seat is
    //     renderThenExtractSetImpl and the per-method hoistSession is live;
    //   * a `k > 0` BODY that is `item default (<nested chain>)` -- the measured
    //     `parentKind=RDefaultExpr hasSink=true` seat, at the STATEMENT level (NOT inside a
    //     lambda), which is what makes hasSink true and puts the hoist on the METHOD session;
    //   * that nested chain is a MULTI nav base + TWO filters + only-element (n = 3,
    //     baseKind=RFeatureCall, lastBodyKind=RListOpExpr) -- the carrier's shape exactly;
    //   * a LATER rung whose `default` right is a CONDITIONAL. This is what makes
    //     functionHasControlFlowValueThen TRUE and therefore what makes the guard fire at
    //     all: DefaultRightNestedThenHoistSeatTest's own javadoc records that QUOM survives
    //     that suite's seat-30 arm and is still declined because "the SAME chain's NEXT body
    //     carries a conditional, which subtreeCtlConfinedToAdmissibleNestedChains rejects".
    //     WITHOUT this rung the fixture is a NON-WITNESS -- the guard would never fire and
    //     the nested chain would already hoist (the seat-32 E.3 lesson).
    // It DROPS: three of the six `default` rungs (same construct), the bare rule invocation
    // (that is law B.3's token, and it must NOT be part of this law's RED), and the real
    // model's type hierarchy.
    //
    // E1 is A1 with the conditional rung REMOVED, so the function has NO ctl value-then and
    // the all-or-nothing guard's FIRST conjunct is already false. The nested chain therefore
    // hoists both before and after this law: bytes unchanged, and the window is proved not to
    // be doing the work where the guard was never the blocker.
    // =========================================================================

    private static final String MODEL = """
            namespace census.seat33b1
            version "1.0.0"

            type Unit:
                kind string (0..1)

            type Sched:
                unit Unit (0..1)
                value number (0..1)

            type Pq:
                schedRef Sched (0..1)
                    [metadata reference]

            type Lot:
                pq Pq (0..*)

            type Leg:
                aPay Pq (0..1)
                bPay Pq (0..1)
                lots Lot (0..*)

            func A1SetSeatNestedValueThen: <"a1 - reduced from drr 7.x standards-iosco-cde-version1-quantity-func.rosetta:338 func QuantityUnitOfMeasure">
                inputs:
                    leg Leg (0..1)
                output:
                    result string (0..1)
                set result:
                    leg -> aPay -> schedRef
                        then default leg -> bPay -> schedRef
                        then default (leg -> lots -> pq -> schedRef
                                then filter unit -> kind exists
                                then filter unit exists
                                then only-element
                                )
                        then default if (leg -> lots -> pq -> schedRef
                                    extract unit -> kind
                                    then distinct count = 1)
                                then leg -> lots -> pq -> schedRef first
                        then extract unit -> kind

            func E1NoCtlValueThen: <"e1 - DECLINE: no ctl value-then anywhere in the function, so the all-or-nothing guard never fired and the nested chain already hoisted - bytes unchanged">
                inputs:
                    leg Leg (0..1)
                output:
                    result string (0..1)
                set result:
                    leg -> aPay -> schedRef
                        then default leg -> bPay -> schedRef
                        then default (leg -> lots -> pq -> schedRef
                                then filter unit -> kind exists
                                then filter unit exists
                                then only-element
                                )
                        then extract unit -> kind
            """;

    // =========================================================================
    // Part A -- the seat (CLAIMED RED at the pre-law head).
    // =========================================================================

    /**
     * a1 -- the nested value-then in the {@code k > 0} body hoists TOGETHER with the chain it
     * sits in, instead of inlining the runtime {@code .then(} form.
     *
     * <p>PIN AT RED. The first three asserts are FIXTURE-REACH premises, GREEN in both states.
     * {@code thenArg0} proves the SET chain reached {@code renderThenExtractSetImpl};
     * {@code getOrDefault(} proves the {@code default} rungs rendered; and
     * {@code ifThenElseResult} proves the function really does carry a ctl value-then, i.e.
     * that the all-or-nothing guard is LIVE for this fixture. If that third premise fails the
     * fixture is a NON-WITNESS (the guard would never have fired) -- RESHAPE the fixture,
     * never weaken the assert.
     *
     * <p>The law's own token is the runtime {@code .then(} count, which the pre-law render
     * fails for the mechanism's reason: the nested chain declines at the guard and falls to
     * {@code CollectionHandler:2445}'s inline fallback. ZERO of the 34,686 goldens carry that
     * form anywhere.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_nestedValueThenInAStepBodyHoistsWithItsChain() throws IOException {
        String out = fixtureFunction("A1SetSeatNestedValueThen");
        assertContains(out, "thenArg0");
        assertContains(out, ".getOrDefault(");
        assertContains(out, "ifThenElseResult");

        assertEquals(0, count(codeOnly(out), ".then("),
                "the nested chain must hoist, not fall to the runtime .then( fallback:\n" + out);
        assertTrue(count(out, "final MapperC<") >= 3,
                "the nested chain's three levels (the MULTI nav base and its two filter steps)"
                + " must each take a MapperC hoist decl:\n" + out);
        assertContains(out, "filterItemNullSafe");
    }

    /**
     * e1 -- the DECLINE lock, byte-frozen in both states. The same nested chain in a function
     * with NO ctl value-then: the guard's first conjunct
     * ({@code functionHasControlFlowValueThen}) is already false, so the chain hoisted before
     * this law and must hoist identically after. This is what makes {@code m-lawB1-window} a
     * discriminating lane rather than a blanket one -- the lane must leave e1 GREEN.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_functionWithoutACtlValueThenIsUnaffectedByTheWindow() throws IOException {
        String out = fixtureFunction("E1NoCtlValueThen");
        assertTrue(!out.contains("ifThenElseResult"),
                "PREMISE: e1 must have NO ctl value-then, else it is not the near miss:\n" + out);
        assertEquals(0, count(codeOnly(out), ".then("),
                "e1's nested chain hoists with or without this law - the guard was never its"
                + " blocker:\n" + out);
        assertContains(out, "filterItemNullSafe");
    }

    // =========================================================================
    // Part B -- the corpus. The carrier is IMPROVED by this law alone (an ENABLER); it went WHOLE
    // at law B.24 later in the same seat, and corpus_c1/c2 EVOLVED into byte compares then.
    // =========================================================================

    /**
     * The EXACT residue this ENABLER leaves at the carrier, each row named to the seat-33 law
     * that flips it. Asserted ABSENT in {@code corpus_c1} while the file was not whole, so its
     * not-whole state was a PINNED fact rather than a silence, and so a later law's landing
     * turned this suite RED (LAW 81) instead of drifting - which it did: B.3 landed the first
     * token and B.24 the other three, and at B.24's LAW-81 batch (B24-trip1.log) this suite
     * fired on the first token. The list is now asserted PRESENT in both golden and the fork
     * (the tokens are golden's), and {@code corpus_c1} is the whole-file byte compare.
     */
    private static final List<String> KNOWN_RESIDUE_QUOM = List.of(
            // law B.3 (bareRuleRefFunctionHost): golden invokes the rule with an explicit,
            // deref'd argument; the fork still emits the bare `quantity.UnitOfMeasureFromQuantity`.
            "unitOfMeasureFromQuantityRule.evaluate(",
            // law B.24 rung B.2 (defaultJoinHeteroMetaDerefBoth): golden's method-level
            // wrapper-item hoist, the FIRST member of the numbering group.
            "fieldWithMetaNonNegativeQuantitySchedule0 = MapperS.of(thenArg6.get()).get();",
            // law B.24 rung B.4 (iteArmMetaCollapseDerefSinkChannel): the two ite-arm members
            // of the same group.
            "fieldWithMetaNonNegativeQuantitySchedule1",
            "fieldWithMetaNonNegativeQuantitySchedule2");

    /**
     * corpus_c1 -- the carrier IMPROVED in cell A by this law and WHOLE since law B.24.
     * Golden's three hoisted decl heads must appear with golden's own names and types, the
     * runtime {@code .then(} must be gone, the once-pinned residue tokens must now be PRESENT
     * (they are golden's), and the file must be byte-identical to golden.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr700QuantityUnitOfMeasureHoistsTheNestedChain() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(QUOM)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + QUOM + ": " + own);
        String gen = drrAOutput.get(QUOM);
        assertNotNull(gen, "not generated: " + QUOM);
        String golden = Files.readString(GOLDEN_A.resolve(QUOM));

        assertContains(golden, "final MapperC<FieldWithMetaNonNegativeQuantitySchedule> thenArg4 =");
        assertContains(gen,
                "final MapperC<FieldWithMetaNonNegativeQuantitySchedule> thenArg4 = MapperS.of(payout)");
        assertContains(gen,
                "final MapperC<FieldWithMetaNonNegativeQuantitySchedule> thenArg5 = thenArg4");
        assertContains(gen,
                "final MapperC<FieldWithMetaNonNegativeQuantitySchedule> thenArg6 = thenArg5");
        assertEquals(0, count(codeOnly(gen), ".then("),
                "the carrier's three runtime .then( sites must all be gone - they are the"
                + " nested chain's, and the chain now hoists (javac C7 line 470)");

        for (String residue : KNOWN_RESIDUE_QUOM) {
            assertTrue(golden.contains(residue),
                    "the pinned residue must be a GOLDEN token, else it names nothing: "
                    + residue);
            assertTrue(gen.contains(residue),
                    "this token was law B.3 / B.24's residue while B.1 stood alone; both landed"
                    + " later in seat 33 and the fork now carries it (LAW 81 re-pin at B.24's"
                    + " batch, B24-trip1.log - the ENABLER assert EVOLVED, never weakened): "
                    + residue + "\n" + gen);
        }
        assertEquals(normalize(golden), normalize(gen),
                "QuantityUnitOfMeasure is WHOLE in drr 7.0.0 since law B.24 (the band's last"
                + " four files) - this compare is the LAW-81 evolution of B.1's 'not whole yet'"
                + " assert, re-pinned in the same commit as the law that moved it");
    }

    /** corpus_c2 -- the same improvement in the LAST cell of the 7.x family. */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_drr730QuantityUnitOfMeasureHoistsTheNestedChain() throws IOException {
        assertNotNull(drrBOutput, "drr 7.3.0 generation did not run");
        String gen = drrBOutput.get(QUOM);
        assertNotNull(gen, "not generated: " + QUOM);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(QUOM))),
                normalize(Files.readString(GOLDEN_B.resolve(QUOM))),
                "PREMISE: golden QuantityUnitOfMeasure is byte-identical in cells A and B - if"
                + " that stops being true this cross-cell assert means something else");
        assertContains(gen,
                "final MapperC<FieldWithMetaNonNegativeQuantitySchedule> thenArg5 = thenArg4");
        assertEquals(0, count(codeOnly(gen), ".then("),
                "seat 33 law B.1 must hold across the whole drr 7.x family, not one cell");
        assertEquals(normalize(Files.readString(GOLDEN_B.resolve(QUOM))), normalize(gen),
                "WHOLE in drr 7.3.0 too since law B.24 (the LAW-81 evolution, B24-trip1.log)");
    }

    /**
     * corpus_c3 -- the NO-MOVE witnesses this law's window could reach. {@link #TNQ} was taken
     * WHOLE by law D.3 in the commit immediately before this one AND its chain is compiled
     * inside the very {@code k > 0} body this window now wraps, so it is the single most
     * exposed file in the cell; {@link #GUSI} and {@link #GUTI} are the guard's other two
     * declining files and are GREEN. All three must byte-match golden.
     *
     * <p>The GUSI/GUTI pin is NOT a duplicate of
     * {@code DefaultRightNestedThenHoistSeatTest.corpus_control0}: that control asserts they
     * are EMITTED and that no golden carries {@code .then(}; this one is the byte compare
     * against golden, which is what an over-fire at the window would break.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c3_theWindowMovesNeitherTheHealedCarrierNorTheGreenGuardWitnesses()
            throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        for (String path : List.of(TNQ, GUSI, GUTI)) {
            String gen = drrAOutput.get(path);
            assertNotNull(gen, "not generated: " + path);
            assertEquals(normalize(Files.readString(GOLDEN_A.resolve(path))), normalize(gen),
                    "the k > 0 restructure window must not move " + path
                    + " - TotalNotionalQuantity is law D.3's whole heal one commit earlier,"
                    + " GetUniqueSwapIdentifier / GetUniqueTransactionIdentifier are the"
                    + " guard's hasSink=false green witnesses at a k == 0 position");
        }
    }

    // =========================================================================
    // corpus_control1 -- LAW 79, the UNION whole-cell scan.
    //
    // INSTRUMENT: the (T1, T2, T3) triple of DefaultRightNestedThenHoistSeatTest, reused
    // VERBATIM (the one-instrument law) -- this law is in the same F7 `.then(` family and that
    // suite's scan is already the family's census, and its own corpus_control1 pins the same
    // cell with the same harness:
    //   T1  `.then(`          -- the REMOVED inline fallback. Golden's count is ZERO in EVERY
    //                            file of this cell (DERIVED: a read-only os.walk over
    //                            test-corpus/drr/drr-7.0.0/rosetta-source/src/generated/java at
    //                            fa49da010 with a python transcription of `scan` below --
    //                            7,808 files, 3,607 token-bearing, T1 total 0).
    //   T2  `.getOrDefault(`  -- the `default` seat's total; this law does not change it, so a
    //                            MOVED T2 is an over-fire at a seat it does not name.
    //   T3  `final MapperC<`  -- the ADDED hoist decls. The carrier moves T1 3 -> 0 and
    //                            T3 1 -> 4.
    //
    // THE OVER-FIRE THIS CONTROL EXISTS FOR: the window's four unsized consumers
    // (CollectionHandler:6935 / :11026 / :11113, ReferenceHandler:6643). Any of them moving a
    // green file shows up here as a new residue row.
    //
    // DOMAIN_DRR7 is the value DefaultRightNestedThenHoistSeatTest pins for the SAME cell with
    // the SAME instrument and the SAME harness (its DOMAIN_DRR7 = 1261, MEASURED at the seat-30
    // head and green through seats 31 and 32). PRINT-FIRST: if it has moved, transcribe from
    // this assert's own "MEASURED domain=" output.
    // =========================================================================

    private static final int DOMAIN_DRR7 = 1261;

    /**
     * DERIVED at the pre-law head. At {@code fa49da010} the residue is THREE rows -- Price,
     * QuantityUnitOfMeasure, TotalNotionalQuantity (reproducing
     * {@code DefaultRightNestedThenHoistSeatTest.KNOWN_RESIDUE_DRR7} row for row, which is how
     * the instrument was validated before it was reused). Law D.3 removes the
     * TotalNotionalQuantity row one commit earlier; THIS law is claimed to remove the
     * QuantityUnitOfMeasure row too, because its triple reaches golden's {@code [0, 7, 4]}
     * even though the FILE does not (T1 3 -> 0 as the three nested-chain {@code .then(} sites
     * hoist, T3 1 -> 4 as thenArg4/5/6 take MapperC decls, T2 unchanged at 7).
     *
     * <p><b>PRINT-FIRST, and the failure modes are NOT symmetric.</b> If the print shows
     * QuantityUnitOfMeasure still present, that is a DISCLOSURE (B.1 left more than predicted)
     * -- add its MEASURED row back and say so. If it shows a row this list does not name, that
     * is an OVER-FIRE at one of the four unsized window consumers and must be investigated,
     * never pinned away.
     *
     * <p>LAW 81 (seat 33, law B.24's batch): Price's row LEFT - law C.2 took Price WHOLE in
     * all four drr 7.x cells. This suite was NOT in C.2's 28-suite batch, so the row was caught
     * one law later (B24-trip1.log: expected [Price ...] but was []) - a pure removal, the
     * C.2 tripwire that batch list missed (BareRuleRefFunctionHost's control1 the same). The
     * list is EMPTY: every token-bearing drr 7.0.0 file agrees with golden on the triple.
     */
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of(
            // the Price row (fork=[0, 8, 1] golden=[0, 7, 1]) LEFT this list at law B.24's LAW-81
            // batch - see the javadoc above.
            );

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_drr700WholeCellThenHoistShapesEqualGoldenFileByFile()
            throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error - the scan is incomplete");
        assertTrue(drrAOutput.containsKey(QUOM) && drrAOutput.containsKey(TNQ)
                        && drrAOutput.containsKey(GUSI) && drrAOutput.containsKey(GUTI),
                "the carrier AND all three no-move witnesses must be INSIDE this scan's"
                + " domain, else control1 proves nothing about them (LAW: a control scans the"
                + " domain it claims)");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR7, DOMAIN_DRR7);
    }

    /**
     * corpus_control2 -- LAW 77, per FILE. The edited method has no IR override, so the two
     * routes must agree on the carrier AND on all three witnesses. Compared LEGACY vs IR (the
     * carrier is not golden-equal at this head, so a golden compare would prove nothing about
     * the route). Skips unless the IR provider is on the classpath ({@code -Pir-on}).
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForTheCarrierAndTheWitnesses() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT),
                new ArrayList<>());
        for (String path : List.of(QUOM, TNQ, GUSI, GUTI)) {
            assertEquals(drrAOutput.get(path), irOut.get(path), "route divergence: " + path);
        }
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
        RModel main = AstBuilder.buildFromString(MODEL, "seat33b1.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat33b1".equals(m.namespace()));
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
            throw new AssertionError("[SetSeatNestedValueThenTogetherHoistSeatTest] builtins"
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
