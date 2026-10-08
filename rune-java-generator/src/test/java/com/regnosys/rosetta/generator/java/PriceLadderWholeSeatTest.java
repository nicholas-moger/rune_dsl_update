// SEAT 33 LAW C.2 - facets heteroMetaDefaultJoinDeref (R3a) + iteArmMultiDefaultTernary (R3b)
// (landed at 358046781; the law's record is target/seat33-instruments/drafts33/C2/NOTES.md).
// Drafted before any run; every value marked ///PIN: was then TRANSCRIBED from the assert's own
// print at RED/GREEN (C2-red2 / C2-green3 logs). This law is the CLOSER: with law C.1 (landed
// immediately before, the ladder restructure) `Price` x drr 7.0-7.3 went WHOLE, 4 files - and
// QuantityUnitOfMeasure, the residue file this suite pinned, went whole at law B.24 (bf0519655).
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
 * SEAT 33, law C.2 - facets {@code heteroMetaDefaultJoinDeref} (rung R3a) and
 * {@code iteArmMultiDefaultTernary} (rung R3b): <b>a MULTI {@code default} whose two operands
 * carry DIFFERENT meta wrappers over the SAME value type joins to the BARE value - both sides
 * deref - and, at the ite-ARM seat, renders upstream's list-form ternary instead of
 * {@code getOrDefault}.</b>
 *
 * <p><b>THIS LAW CLOSES {@code Price}.</b> Law <b>C.1</b> (the ENABLER, landing immediately
 * before) restructures the whole seven-rung ladder and leaves exactly ONE residue - rung 1's
 * {@code default} join - which these two rungs own. <b>WHOLE 4</b>: {@code Price} x drr
 * 7.0/7.1/7.2/7.3 FUNCTION. Golden ← fork, on one line of the render:
 * <pre>
 * // GOLDEN (Price:123-125, both operands deref'd, list-form ternary, the LEFT repeated)
 * thenArg = &lt;L&gt;.&lt;PriceSchedule&gt;map("Type coercion",
 *                 referenceWithMetaPriceSchedule -&gt; referenceWithMetaPriceSchedule.getValue())
 *         .getMulti().isEmpty()
 *         ? &lt;R&gt;.&lt;PriceSchedule&gt;map("Type coercion",
 *                 fieldWithMetaPriceSchedule -&gt; fieldWithMetaPriceSchedule.getValue())
 *         : &lt;L&gt;.&lt;PriceSchedule&gt;map("Type coercion",
 *                 referenceWithMetaPriceSchedule -&gt; referenceWithMetaPriceSchedule.getValue());
 * // FORK
 * thenArg = &lt;L&gt;.getOrDefault(&lt;R&gt;);
 * </pre>
 *
 * <p><b>R3a - the discriminator, MEASURED.</b> {@code [P33-DEFJOIN] sameWrap=false vtEq=true}
 * is <b>12 rows / 2 {@code where=}</b> - {@code fn:QuantityUnitOfMeasure} 8 and {@code fn:Price}
 * 4 - and ZERO of them is green; identical on the IR route. Price's raw row:
 * {@code metaJoin=false leftKind=RFeatureCall rightKind=RFilterExpr
 * leftWrap=...ReferenceWithMetaPriceSchedule rightWrap=...FieldWithMetaPriceSchedule
 * rightChan=stamp sameWrap=false vtEq=true leftVt=...PriceSchedule rightVt=...PriceSchedule
 * fires=false}. The declining conjunct is the COMPILED-STAMP early return inside
 * {@code rightRendersNoMetaWrapper} ({@code SetOperationHandler:1057-1061}), whose own javadoc
 * at {@code :1035-1041} names {@code fn:Price} as the shape it holds out - the fork telling us
 * where its own law stops. The stamp-first ORDER is unchanged: R3a is an {@code else if}.
 * The GREEN set the {@code sameWrap} conjunct protects is measured too
 * ({@code sameWrap=true --group where}): {@code fn:QuantityUnitOfMeasure} 32,
 * {@code rule:QuantitySchedule} 20, {@code fn:MessageID} 9, {@code fn:GetBasketConstituents} 4.
 *
 * <p><b>R3b - the discriminator, MEASURED, and the bare parent set is NOT safe.</b>
 * {@code [P33-DEFSEAT] parentKind=RConditionalExpr} alone is <b>106 rows / 10 {@code where=}</b>.
 * Conjoining {@code chainProvesMulti} on BOTH raw operands closes it EXACTLY:
 * {@code parentKind=RConditionalExpr leftMulti=true rightMulti=true} = <b>4 rows / 1
 * {@code where=} / {@code fn:Price}</b> (1,175 rows both routes, identical histograms). Green
 * would-fire ZERO. Positive control (so that zero is trusted): the same tag's
 * {@code thenBase=true} rows are 29 / 5 {@code where=}, and the gate histogram reads
 * {@code fallthrough} 935 / {@code kvPair} 164 / {@code elidedLeft} 60 / {@code thenBase} 12 /
 * {@code extractBody} 4. Price's own raw row reads {@code gate=fallthrough} - all five existing
 * gates declined, exactly as traced by reading.
 *
 * <p><b>THE FORM CORRECTION THAT COSTS THE FILE IF MISSED.</b> Golden's rung-1 derefs are
 * UNGUARDED and UNNUMBERED on BOTH sides. {@code coerceNavigationReceiver} mints the GUARDED
 * NUMBERED form ({@code ... == null ? null : ....getValue()}). Golden's split is by MAPPER FORM:
 * {@code mapC}-level (MULTI) derefs are bare, {@code map}-level (SINGLE) derefs are guarded and
 * numbered. R3a's operands are both MULTI, so the law reuses {@code renderCoercedArm} - the
 * emitter of exactly that string, already in this file (LAW 69).
 *
 * <p><b>LAW 80 - a NAMED MOVER.</b> R3a moves {@code fn:QuantityUnitOfMeasure}'s 8 rows too.
 * QUOM is a band file that goes WHOLE at law <b>B.24</b> (charter row 17), not here, so its
 * bytes IMPROVE without closing: {@code corpus_control1} pins it as the ONLY expected residue
 * row in cell A after this law, and {@code NOTES.md} lists its 28 refs / 20 files for the
 * LAW-81 re-pin batch.
 *
 * <p><b>LAW 77 - INHERITS.</b> {@code IRExpressionCompiler.visitDefault:3440} is
 * {@code tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitDefault(expr, ctx))} - the #513
 * noAdaptArm count-then-super meter - so the seat is reached on both routes and the law
 * inherits provided {@code tryEmitFromIR} declines for this node. {@code corpus_control2}
 * measures it; the seat's both-routes checkpoint and the route CONTENT compare are the standing
 * check.
 *
 * <p><b>LAW 74 - COMPILE.</b> The fork's
 * {@code MapperC<ReferenceWithMetaPriceSchedule>.getOrDefault(MapperC<FieldWithMetaPriceSchedule>)}
 * is analytically non-compiling (the T-typed overload expects the ITEM), <b>but it is NOT one of
 * the four measured C11 errors</b> - those are all {@code bad type in conditional expression} on
 * the ternary law C.1 deletes ({@code Seat33Pre.java:620, 621, 622 x2}). <b>Analytic, not
 * measured: no commit message may put it in.</b> C.2 is a BYTE law as far as {@code javac33}
 * goes; what it may claim is that C11 reaches 0 only after BOTH C.1 and C.2, re-measured against
 * the law's own output.
 *
 * <p><b>CLAIMED RED at the law's base head</b> (the fourteen earlier seat-33 laws, C.1 included;
 * default route, and the chain measures both): {@code a1}, {@code a2}, {@code corpus_c1},
 * {@code corpus_c2}, {@code corpus_control1}. <b>GREEN AT BOTH HEADS</b> (controls):
 * {@code e2}, {@code corpus_control0}, {@code corpus_control3}. <b>CLAIMED GREEN after the
 * law</b>: the whole suite ({@code corpus_control2} skips without {@code -Pir-on}).
 * {@code e1} is a SHAPE lock on R3a only and is GREEN at both heads for its own assert (see its
 * javadoc - its {@code getOrDefault} does become a ternary, which is R3b doing its job).
 *
 * <p><b>MUTATION LANES (LAW 66/76) - CLAIMED; LAW 82 applies.</b>
 * <table>
 *   <caption>lanes</caption>
 *   <tr><th>lane</th><th>severed</th><th>CLAIMED failing set</th></tr>
 *   <tr><td>{@code m-c2-hetero}</td><td>the hetero arm never fires</td>
 *       <td>{@code a1}, {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1}.
 *           <b>NOT EMPTY at the corpus grain</b> - at this law's head QUOM's 8 rows moved
 *           back with it; since law B.24 took QUOM's then-chain rights to its own seat, the
 *           lane names Price alone (the LAW-82 block below, f33-mut-m-lawC2-hetero.log)</td></tr>
 *   <tr><td>{@code m-c2-itearm}</td><td>the fourth gate</td>
 *       <td>{@code a2}, {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1};
 *           {@code a1} stays GREEN - the lane that proves the two rungs are independent.
 *           Green movement EMPTY (declared, measured)</td></tr>
 *   <tr><td>{@code m-c2-samewrap}</td><td>ONLY the homogeneous-meta protection</td>
 *       <td>{@code e1} and {@code corpus_control0} (the 33 protected green rows hop);
 *           {@code a1}, {@code a2}, {@code corpus_c1}, {@code corpus_c2} stay GREEN - the lane
 *           that says the DISCRIMINATOR, not the seat, is what keeps this law green-safe</td></tr>
 * </table>
 *
 * <p><b>LAW 82 - MEASURED by the receipts chain, run 1 at ce1a06292 (final33.status; every
 * figure below is transcribed from the chain's own logs, never from this file's earlier
 * CLAIMED paragraphs, which it supersedes).</b> GREEN 10/0F/1skip default (f33-green-default.log) /
 * 10/0F/0skip {@code -Pir-on} (f33-green-on.log); RED at the pre-seat base {@code fa49da010}: default
 * 7F = a1, a2, corpus_c1, corpus_c2, corpus_control0, corpus_control1, corpus_control3; {@code -Pir-on} 7F = a1, a2, corpus_c1, corpus_c2, corpus_control0, corpus_control1, corpus_control3 (f33-red-{default,on}.log).
 * Mutation lanes ({@code mut33.py}, the default profile; {@code lanes33.py --summary}):
 * <ul>
 *   <li><b>{@code m-lawC2-hetero}</b> ({@code SOH_PAIRS_MUT_HETERO}): MEASURED 10/5F/1skip = a1, a2, corpus_c1, corpus_c2, corpus_control1 - RE-SCORED (superset): a2 fell as well as the claimed a1, c1, c2, control1 - R3b's ternary renders R3a's deref'd operands, so severing R3a takes a2 with it; the coupling runs one way only (itearm leaves a1 green).</li>
 *   <li><b>{@code m-lawC2-itearm}</b> ({@code SOH_PAIRS_MUT_ITEARM}): MEASURED 10/4F/1skip = a2, corpus_c1, corpus_c2, corpus_control1 - MATCH (a2, c1, c2, control1; a1 held).</li>
 *   <li><b>{@code m-lawC2-samewrap}</b> ({@code SOH_PAIRS_MUT_SAMEWRAP}): MEASURED 10/3F/1skip = corpus_control0, corpus_control1, e1 - RE-SCORED (superset): control1 moved as well as the claimed e1 and control0 - the 33 protected green rows hopping is visible to the whole-cell union scan too; the discriminator's reach measured twice.</li>
 * </ul>
 */
class PriceLadderWholeSeatTest {

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

    private static final String PRICE =
            "drr/standards/iosco/cde/version1/price/functions/Price.java";
    private static final String QUOM =
            "drr/standards/iosco/cde/version1/quantity/functions/QuantityUnitOfMeasure.java";

    /** Cell A = drr 7.0.0 - a carrier cell. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /** Cell B = drr 7.1.0 - the SECOND carrier cell (7.2/7.3 carry the identical rows). */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.1.0");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");
    /**
     * Cell C = drr 5.61.0 - a NON-carrier cell, dense in {@code default} joins. Neither measured
     * population ({@code sameWrap=false vtEq=true} = 12 rows / 2 wheres,
     * {@code parentKind=RConditionalExpr leftMulti rightMulti} = 4 rows / 1 where) has a row
     * here, so its whole-cell tuple must be UNMOVED.
     */
    private static final Path CELL_C_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path GOLDEN_C = CELL_C_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A);
    }

    static boolean cellBAvailable() {
        return Files.isDirectory(GOLDEN_B);
    }

    static boolean cellCAvailable() {
        return Files.isDirectory(GOLDEN_C);
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    // =========================================================================
    // The fixture - reduced from the carrier's REAL source
    // =========================================================================

    /**
     * Reduced from {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/main/rosetta/
     * standards-iosco-cde-version1-price-func.rosetta:99-102} - {@code func Price}'s rung 1:
     * <pre>
     * then (economicTerms -&gt; payout -&gt; SettlementPayout only-element -&gt; priceQuantity
     *           -&gt; priceSchedule
     *       default (tradableProduct -&gt; tradeLot -&gt; priceQuantity -&gt; price
     *                filter item -&gt; priceType = ...))
     * </pre>
     * whose LEFT element is {@code ReferenceWithMetaPriceSchedule}, whose RIGHT element is
     * {@code FieldWithMetaPriceSchedule}, and whose two wrappers are over the SAME value type.
     *
     * <p>FAITHFULNESS (the seat-32 E.3 {@code a1} lesson): the {@code default} sits as an ARM of
     * an {@code if / else} whose OTHER arm is a bare MULTI nav (so the ladder's join element is
     * the BARE value, as Price's is, and the k==0 FUNCTION MULTI SET seat is the one reached);
     * BOTH operands keep {@code (0..*)} so {@code chainProvesMulti} holds on each; the two meta
     * annotations are {@code reference} and {@code location}, which
     * {@code MetaFieldGenerator.detectMetaKind} maps to REFERENCE_WITH_META and FIELD_WITH_META
     * respectively - the same pair the carrier has; and the output is {@code (0..1)} closed by
     * {@code then only-element}.
     */
    private static final String MODEL = """
            namespace census.seat33c2
            version "1.0.0"

            type Sched:
                tagText string (0..1)

            type Box:
                refSched Sched (0..*)
                    [metadata reference]
                fldSched Sched (0..*)
                    [metadata location]
                sameA Sched (0..*)
                    [metadata location]
                sameB Sched (0..*)
                    [metadata location]
                oneSched Sched (0..1)
                otherSched Sched (0..1)

            type Root:
                box Box (0..1)
                legs Sched (0..*)
                flag boolean (0..1)

            func A1HeteroDefaultInIteArm: <"a1 + a2 - THE CARRIER shape: a MULTI `default` whose two operands carry DIFFERENT wrappers over the SAME value type, as an ARM of an ite ladder">
                inputs:
                    r Root (1..1)
                output:
                    out Sched (0..1)
                set out:
                    if r -> flag exists
                    then (r -> box -> refSched default r -> box -> fldSched)
                    else r -> legs
                        then only-element

            func E1HomogeneousDefaultKeepsWrapper: <"e1 - R3a DECLINE LOCK: both operands under the SAME wrapper - a homogeneous-meta default keeps the wrapper and must not hop">
                inputs:
                    r Root (1..1)
                output:
                    out Sched (0..1)
                set out:
                    if r -> flag exists
                    then (r -> box -> sameA default r -> box -> sameB)
                    else r -> legs
                        then only-element

            func E2SingleDefaultInIteArmStaysGetOrDefault: <"e2 - R3b DECLINE LOCK: the ite-arm parent alone is a 106-row set; the both-operands-MULTI conjunct is what closes it to the carrier">
                inputs:
                    r Root (1..1)
                output:
                    out Sched (0..1)
                set out:
                    if r -> flag exists
                    then (r -> box -> oneSched default r -> box -> otherSched)
                    else r -> legs
                        then only-element
            """;

    // =========================================================================
    // Part A - the mechanism pins (CLAIMED failing-first)
    // =========================================================================

    /**
     * a1 - R3a. BOTH operands deref to the bare join element, in the UNGUARDED UNNUMBERED MULTI
     * form. Pre-law {@code rightRendersNoMetaWrapper} declines on the compiled stamp (the RIGHT
     * IS meta), the seat-28 arm does not fire, and neither operand hops.
     *
     * <p>The precondition assert comes first and holds at BOTH heads: if it fails, the fixture
     * stopped reaching the k==0 FUNCTION MULTI SET seat and must be RESHAPED, never weakened.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_heterogeneousMetaDefaultDerefsBothOperands() throws IOException {
        String out = normalize(fn("A1HeteroDefaultInIteArm.java"));
        assertContains(out, "final MapperC<Sched> thenArg;");
        assertContains(out, ".<Sched>map(\"Type coercion\","
                + " referenceWithMetaSched -> referenceWithMetaSched.getValue())");
        assertContains(out, ".<Sched>map(\"Type coercion\","
                + " fieldWithMetaSched -> fieldWithMetaSched.getValue())");
        assertTrue(!out.contains(" == null ? null : referenceWithMetaSched"),
                "a MULTI operand takes the UNGUARDED bare deref, not the coercion service's"
                        + " guarded numbered form:\n" + out);
    }

    /**
     * a2 - R3b. The ite-ARM {@code default} renders upstream's list-form ternary with the LEFT
     * repeated, not {@code getOrDefault}. Pre-law it falls through all five existing gates to
     * {@code left + ".getOrDefault(" + right + ")"}.
     *
     * <p>The {@code .getValue()).getOrDefault(} assert is the exact post-R3a / pre-R3b shape, so
     * lane {@code m-c2-itearm} - which leaves R3a firing - fails HERE and only here.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_iteArmMultiDefaultRendersTheListFormTernary() throws IOException {
        String out = normalize(fn("A1HeteroDefaultInIteArm.java"));
        assertContains(out, "final MapperC<Sched> thenArg;");
        assertContains(out, ".getMulti().isEmpty() ? ");
        assertTrue(!out.contains(".getValue()).getOrDefault("),
                "the ite-arm MULTI default must not keep the getOrDefault form:\n" + out);
        assertEquals(3, count(out, "map(\"Type coercion\", "),
                "the list-form ternary repeats the LEFT, so its deref appears TWICE and the"
                        + " RIGHT's once:\n" + out);
    }

    /**
     * e1 - the R3a DECLINE LOCK, and it locks the DISCRIMINATOR, not the seat: the same ite-arm
     * MULTI default with BOTH operands under the SAME wrapper. Upstream's
     * {@code joinMetaAnnotatedTypes} keeps the meta when both operands agree, so the join is the
     * WRAPPER and neither side hops - golden drr {@code MessageID} (9 GREEN cells, both operands
     * {@code FieldWithMetaString}) and {@code GetBasketConstituents:173} are the corpus witnesses.
     *
     * <p>DELIBERATELY a SHAPE lock, not a bytes-unchanged lock: R3b still applies here (both
     * operands are MULTI and the parent is a conditional), so the {@code getOrDefault} does
     * become a ternary. What must not happen is the DEREF. Lane {@code m-c2-samewrap} inverts
     * exactly this test.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_homogeneousMetaDefaultKeepsItsWrapper() throws IOException {
        String out = normalize(fn("E1HomogeneousDefaultKeepsWrapper.java"));
        assertContains(out, "final MapperC<");
        assertTrue(!out.contains("fieldWithMetaSched -> fieldWithMetaSched.getValue()"),
                "a HOMOGENEOUS-meta default keeps the wrapper - neither operand may hop:\n" + out);
        assertTrue(!out.contains("map(\"Type coercion\", "),
                "no coercion hop at all is emitted for the homogeneous join:\n" + out);
    }

    /**
     * e2 - the R3b DECLINE LOCK. The ite-arm PARENT alone is a 106-row / 10-{@code where=} set
     * ({@code [P33-DEFSEAT]}); the {@code chainProvesMulti}-on-both-operands conjunct is what
     * closes it to the 4 carrier rows. This fixture is the same seat with SINGLE operands, so
     * the conjunct declines and {@code getOrDefault} stays. GREEN at both heads.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e2_singleCardinalityDefaultInAnIteArmKeepsGetOrDefault() throws IOException {
        String out = normalize(fn("E2SingleDefaultInIteArmStaysGetOrDefault.java"));
        assertContains(out, ".getOrDefault(");
        assertTrue(!out.contains(".getMulti().isEmpty() ? "),
                "an unproven-MULTI default must keep its bytes:\n" + out);
    }

    // =========================================================================
    // Part B - the corpus locks and the whole-cell controls
    // =========================================================================

    /**
     * corpus_c1 - the WHOLE-FILE byte compare for {@code Price} in drr 7.0.0. This is the law
     * that closes the file: law C.1 restructured the ladder and left exactly rung 1's
     * {@code default} join, and these two rungs are it.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr700PriceMatchesGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(PRICE)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + PRICE + ": " + own);
        String gen = drrAOutput.get(PRICE);
        assertNotNull(gen, "not generated: " + PRICE);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(PRICE))), normalize(gen),
                "Price must byte-match golden - seat 33 laws C.1 + C.2");
    }

    /** corpus_c2 - the same whole-file compare in drr 7.1.0. */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_drr710PriceMatchesGolden() throws IOException {
        assertNotNull(drrBOutput, "drr 7.1.0 generation did not run");
        String gen = drrBOutput.get(PRICE);
        assertNotNull(gen, "not generated: " + PRICE);
        assertEquals(normalize(Files.readString(GOLDEN_B.resolve(PRICE))), normalize(gen),
                "Price must byte-match golden in the second cell - seat 33 laws C.1 + C.2");
    }

    ///PIN: the three files below are the MEASURED GREEN population the `sameWrap` conjunct
    ///PIN: protects: rule:QuantitySchedule 20 rows, fn:MessageID 9 rows (the 9 GREEN cells the
    ///PIN: seat-28 javadoc names by hand), fn:GetBasketConstituents 4 rows (whose golden line 173
    ///PIN: that javadoc also names). GBC is a band file that goes WHOLE at law A.2, ten commits
    ///PIN: earlier, so it is byte-clean at this law's base head. All three must stay byte-clean.
    private static final List<String> CELL_A_UNMOVED = List.of(
            "drr/base/trade/quantity/reports/QuantityScheduleRule.java",
            "drr/regulation/common/trade/link/functions/MessageID.java",
            "drr/base/trade/basket/functions/GetBasketConstituents.java");

    /**
     * corpus_control0 - the protected-green no-move control. GREEN at BOTH heads; it fails the
     * instant the {@code sameWrap} conjunct stops protecting the homogeneous-meta join, which is
     * exactly what lane {@code m-c2-samewrap} does.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_theHomogeneousMetaGreenSetStaysWhole() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        for (String path : CELL_A_UNMOVED) {
            String gen = drrAOutput.get(path);
            assertNotNull(gen, "not generated: " + path);
            assertEquals(normalize(Files.readString(GOLDEN_A.resolve(path))), normalize(gen),
                    "this file must stay byte-identical to golden across law C.2: " + path);
        }
    }

    ///PIN: DERIVABLE at the FILE grain and pinned as such. At this law's base head the drr 7.x
    ///PIN: band is EIGHT files - Price x4 (charter rows 15/16) and QuantityUnitOfMeasure x4
    ///PIN: (which goes whole at law B.24, charter row 17) - and every other file in a cell is
    ///PIN: byte-identical to golden. This law takes Price WHOLE and MOVES QUOM (LAW 80
    ///PIN: IMPROVED-not-whole), so after it exactly ONE file in cell A may carry a residue row.
    ///PIN: LAW 81 re-pin (seat 33, law B.24): QuantityUnitOfMeasure went WHOLE in all four drr 7.x
    ///PIN: cells (QuomDefaultJoinIteArmWholeSeatTest corpus_c1/c2 are byte compares), so the residue
    ///PIN: FILE SET is EMPTY - this control's own print (B24-trip1.log): expected [QUOM] but was [].
    private static final List<String> RESIDUE_FILES_DRR700 = List.of();

    /**
     * corpus_control1 - LAW 79, the whole-cell UNION scan on drr 7.0.0, over the three
     * {@code default}-join tokens this law moves. Two asserts, in this order: the residue FILE
     * SET must be exactly {@code {QuantityUnitOfMeasure}} at this law's head and EMPTY once law
     * B.24 lands (it did, later in the same seat) - Price's row REMOVED, no other file joined -
     * and then the full tuple rows against the print-first pin.
     *
     * <p>This control fails when the law OVER-fires (a homogeneous-meta green file hops, or a
     * non-carrier ite-arm default turns into a ternary) AND when it UNDER-fires (Price's row
     * survives). {@code GOLDEN_DOMAIN_DRR700} is an instrument-integrity pin DERIVED BY A
     * READ-ONLY WALK: {@code python target/seat33-instruments/drafts33/C2/derive-domain.py} from
     * the repo root, which mirrors {@code scan()} exactly and printed
     * {@code token-bearing=3570 total=7808}.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr700WholeCellDefaultFormsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error - the scan is incomplete");
        Map<String, int[]> golden = scan(readGoldenTree(GOLDEN_A));
        assertEquals(GOLDEN_DOMAIN_DRR700, golden.size(),
                "the GOLDEN-side token-bearing file count is the instrument's own integrity pin"
                        + " (derive-domain.py)");
        assertEquals(RESIDUE_FILES_DRR700, mismatchedFiles(scan(drrAOutput), golden,
                        drrAOutput.keySet()),
                "after law C.2 exactly ONE file in this cell could still differ on the default-join"
                        + " tuple (QuantityUnitOfMeasure); since law B.24 took it whole NO file may -"
                        + " Price's row and QUOM's are GONE and no other file may have joined");
        assertUnionEqual(scan(drrAOutput), golden, drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR700, UNION_DOMAIN_DRR700);
    }

    /**
     * corpus_control2 - LAW 77 route parity for the carrier.
     * {@code IRExpressionCompiler.visitDefault} is the #513 count-then-super meter, so the seat
     * is reached on both routes and the law inherits; this MEASURES that rather than asserting
     * it. Skips without {@code -Pir-on}.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForPrice() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT),
                new ArrayList<>());
        assertEquals(drrAOutput.get(PRICE), irOut.get(PRICE), "route divergence: " + PRICE);
    }

    /**
     * corpus_control3 - LAW 79 in a NON-carrier cell (drr 5.61.0), the same three-tuple. Neither
     * measured population has a row in this cell, so {@code KNOWN_RESIDUE_DRR561} must be
     * UNMOVED - a change here is an over-fire, not a re-pin.
     */
    @Test
    @EnabledIf("cellCAvailable")
    void corpus_control3_forkDrr561WholeCellDefaultFormsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrCOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrCGenErrors,
                "drr 5.61.0 reported a generation error - the scan is incomplete");
        Map<String, int[]> golden = scan(readGoldenTree(GOLDEN_C));
        assertEquals(GOLDEN_DOMAIN_DRR561, golden.size(),
                "the GOLDEN-side token-bearing file count (derive-domain.py)");
        assertUnionEqual(scan(drrCOutput), golden, drrCOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR561, UNION_DOMAIN_DRR561);
    }

    ///PIN: PRINT-FIRST SENTINELS. The FILE set is pinned above and IS derivable; the per-file
    ///PIN: TUPLES are not (fourteen earlier seat-33 laws land first). `List.of("PIN-AT-RED")`
    ///PIN: makes the union assert fail with the MEASURED list in its message; the lead
    ///PIN: transcribes, re-runs, and confirms that the only surviving row is QUOM's and that
    ///PIN: cell C's list is unchanged from its value at the law's RED head.
    private static final List<String> KNOWN_RESIDUE_DRR700 = List.of(
            // TRANSCRIBED from the control's own print at GREEN (C2-green1.log): the ONE file still
            // differing on the default-join tuple after Price went whole - QUOM, which closes at B.24.
            // LAW 81 re-pin (seat 33, law B.24): the QUOM row (fork=[0, 7, 3] golden=[0, 7, 2]) LEFT -
            // B.24 took the file WHOLE (a byte-identical file scans to golden's tuple by construction;
            // the file-set assert above fires first, so this empty list is DERIVED, not printed -
            // and MEASURED GREEN by the batch's re-run, B24-trip2.log 272/0F). Empty = every drr
            // 7.0.0 file agrees with golden on the three tokens.
            );
    private static final List<String> KNOWN_RESIDUE_DRR561 = List.of();   // TRANSCRIBED AT RED (C2-red1.log): drr 5.61.0 carries no default-join residue, as bounded

    ///PIN: MEASURED by a READ-ONLY WALK over the golden trees (derive-domain.py, mirroring
    ///PIN: scan() exactly): drr 7.0.0 = 3570 token-bearing of 7808 .java goldens; drr 5.61.0 =
    ///PIN: 2586 of 5249. The goldens are frozen, so these should never move.
    private static final int GOLDEN_DOMAIN_DRR700 = 3570;
    private static final int GOLDEN_DOMAIN_DRR561 = 2586;

    ///PIN: PRINT-FIRST SENTINEL (the seat-30 discipline). The UNION domain is golden-side UNION
    ///PIN: fork-side, RESTRICTED to what the suite actually emits - it cannot be derived from the
    ///PIN: golden tree alone. -1 makes the domain assert fail with "MEASURED domain=N" in its own
    ///PIN: message; transcribe N here.
    private static final int UNION_DOMAIN_DRR700 = 1224;   // TRANSCRIBED from the control's own print (C2-green2.log)
    private static final int UNION_DOMAIN_DRR561 = 937;   // TRANSCRIBED from the control's own print (C2-green1.log)

    /**
     * (T1..T3) per file: {@code .getMulti().isEmpty() ? } list-form ternary sites (what R3b
     * mints), {@code .getOrDefault(} sites (what R3b displaces at the ite-arm seat - and the
     * ordinary condition sites, which must NOT move), and {@code map("Type coercion", } hops
     * (R3a adds one per operand, and the repeated LEFT makes that three per carrier site).
     * Independent on purpose: a control that derived one from another would be blind to exactly
     * the half that regressed.
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            int[] t = new int[3];
            for (String line : normalize(e.getValue()).split("\n")) {
                String s = line.trim();
                t[0] += count(s, ".getMulti().isEmpty() ? ");
                t[1] += count(s, ".getOrDefault(");
                t[2] += count(s, "map(\"Type coercion\", ");
            }
            if (t[0] + t[1] + t[2] > 0) {
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

    /** The FILE names whose tuple differs - the derivable half of the union control. */
    private static List<String> mismatchedFiles(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA) {
        List<String> out = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        universe.retainAll(emittedA);
        int[] zero = new int[3];
        for (String key : universe) {
            if (!java.util.Arrays.equals(a.getOrDefault(key, zero), b.getOrDefault(key, zero))) {
                out.add(key);
            }
        }
        return out;
    }

    // =========================================================================
    // The union assert (LAW 73: pin the SET, not the count) - the seat-30/31/32 shape
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
        assertEquals(knownResidue, mismatched,
                "the default-join tuple differs beyond the named residue in " + mismatched.size()
                        + " file(s)");
        // The domain-pin flow (LAW 73): the sentinel failure PRINTS the measured value so the pin
        // is transcribed from this assert's own output, never invented.
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - transcribe from this print:"
                        + " MEASURED domain=" + universe.size());
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted token-bearing files (" + expectedDomain
                        + ")");
    }

    // =========================================================================
    // Harness - the seat-30/31/32 suite shape
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> drrBOutput;
    private static List<String> drrBGenErrors;
    private static Map<String, String> drrCOutput;
    private static List<String> drrCGenErrors;

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
                    new D11CorpusRegressionTest.CellSpec("drr", "7.1.0", CELL_B_ROOT), errs);
            drrBGenErrors = errs;
        }
        if (cellCAvailable()) {
            List<String> errs = new ArrayList<>();
            drrCOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_C_ROOT), errs);
            drrCGenErrors = errs;
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
                    "the IR provider must be resolvable under -Pir-on, else this is not an"
                            + " ON-route render");
            var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            var typeUtil = new JavaTypeUtil();
            var typeTranslator = new JavaTypeTranslator(typeUtil);
            FunctionGenerator funcGen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            assertTrue(!funcGen.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator, got "
                            + funcGen.getClass());
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

    private static void assertContains(String out, String token) {
        assertTrue(out.contains(token), "expected token missing:\n  " + token + "\nin:\n" + out);
    }

    // =========================================================================
    // Fixture harness (the seat-30 fn()/fixture() shape)
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat33c2.rosetta");
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
            throw new AssertionError("fixture generation errors (a broken fixture must fail"
                    + " loudly, not skip): " + errors);
        }
        return out;
    }

    private static Map<String, String> fixture() throws IOException {
        if (fixtureOut == null) {
            fixtureOut = render(m -> "census.seat33c2".equals(m.namespace()));
        }
        return fixtureOut;
    }

    private static String fn(String fileName) throws IOException {
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
            throw new AssertionError("[PriceLadderWholeSeatTest] builtins parse failures: "
                    + String.join("; ", failures));
        }
        return models;
    }
}
