package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.handlers.NavigationHandler;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * SEAT 30 — the TWO zero-carrier guards on facet {@code ruleOutputDefaultSpineMulti} (seat 29
 * law 2), banked by the PR #601 independent adversarial review as corpus-invisible latent gaps.
 * Both are HARDENING, not widening: neither can admit a row the pre-guard predicate declined.
 *
 * <p><b>GUARD 1 — the cycle gap.</b> {@code NavigationHandler.defaultSpineProvesMulti}'s
 * default-leaf branch hands off to {@code chainProvesMulti}, whose {@code RRule} arm re-enters
 * {@code ruleOutputProvesMulti}, which allocated a FRESH visited set every time. A grammar-legal
 * cyclic rule reference therefore recursed unboundedly through the mutual path
 * <pre>
 *   defaultSpineProvesMulti → chainProvesMulti → ruleOutputProvesMulti → defaultSpineProvesMulti
 * </pre>
 * and overflowed the stack. The identity guard that WAS there only covered the DIRECT
 * {@code defaultSpineProvesMulti} → {@code defaultSpineProvesMulti} descent, so it never saw the
 * round trip. The fix threads the visited set through the hand-off — i.e. through every composing
 * edge of {@code chainProvesMulti}'s own walk — as an ON-PATH set (added descending, removed in a
 * {@code finally}), which is what makes it byte-neutral BY CONSTRUCTION: that walk BRANCHES, and a
 * visited-ever set could let one branch's rule visit wrongly decline another branch's. It is the
 * ENGINE's own pattern: {@code CardinalityComputer.compute(expr, visited, thenAware)} guards the
 * identical malformed-cycle case the identical way and documents it in the identical words
 * ("makes a malformed self-/mutually-referential rule chain read SINGLE instead of overflowing the
 * stack"). LAW 69 — the engine half and the overlay half of one question now read one design.
 *
 * <p><b>GUARD 2 — the unary-collapser blind spot.</b> The collapse guard rejects a collapsing
 * spine leaf but tested {@code instanceof RListOpExpr} only. {@code min}/{@code max}/
 * {@code reduce}/{@code count} are NOT {@code RListOpExpr} — they are their own node kinds
 * ({@code RMinExpr}, {@code RMaxExpr}, {@code RReduceExpr}, {@code RCountExpr}), and the engine
 * reads all four SINGLE ({@code CardinalityComputer} :94/:99/:100/:101). A
 * {@code multi default X then min} spine therefore ADMITTED and the rule took a
 * {@code List}-signatured {@code ReportFunction} over a scalar body — an ENTERING-class defect,
 * not merely an unclaimed heal. The fix names the classification ({@code collapsesToSingle}) and
 * extends the leaf rejection to the four. {@code RSortExpr} stays OUT — {@code sort} is
 * element-preserving and the engine reads it MULTI ({@code CardinalityComputer} :116).
 *
 * <p><b>Why fixtures and not corpus rows.</b> Both gaps are ZERO-CARRIER on all 25 cells: no
 * corpus rule graph has a cycle, and no admitted spine carries a unary-collapser leaf (the seat-29
 * probe's twelve {@code FER-thenExtractTerminal} admit candidates recorded
 * {@code leaves=RDefaultExpr,RListOpExpr} and {@code leaves=RListOpExpr,RDefaultExpr,RListOpExpr}
 * — {@code RListOpExpr} only, both routes). A corpus control can therefore only prove the guards
 * do NOT fire; the fixtures are the only place they can be proved to WORK. The seat's
 * byte-neutrality proof is the unchanged matrix digest across the law commit — the seat-29 law-7
 * zero-carrier precedent.
 *
 * <p><b>MEASURED (first run at the applied head): the type computer's own cycle refusal FRONTS
 * the generator pipeline, and it narrows guard 1's claim.</b> {@code RuleGenerator}'s Phase-X1
 * output-typeCall back-fill refuses a MULTI-rule cycle before any expression is compiled —
 * {@code "RuleGenerator: rule '…' expression has unresolved type — workspace type inference
 * returned RMissingType. Cannot back-fill output.typeCall"} ({@code RuleGenerator:305-315}),
 * because a mutual rule reference leaves the inferred types unresolvable. So on today's corpus
 * and today's entry points the overlay recursion is <b>unreachable through generation for a
 * multi-rule cycle</b>. The measured split:
 * <ul>
 *   <li>the <b>SELF</b> cycle (c0) generates end-to-end and reaches the predicate on the real
 *       rendering path;</li>
 *   <li>the <b>two-</b> and <b>three-rule</b> cycles are refused upstream, so c2/c3 pin the
 *       predicate at the <b>unit boundary</b> — workspace + {@code ExpressionCompiler} +
 *       {@code NavigationHandler.chainProvesMulti(ruleBody, compiler)} — which is the seam the
 *       guard actually lives at.</li>
 * </ul>
 * Guard 1 is therefore <b>defence-in-depth</b>, and the honest statement of its reach is: it is
 * exercised today by the self-cycle through generation and by c2/c3 at the unit boundary, and it
 * protects any future caller that evaluates the predicate without rule-emission type validation
 * in front of it (an IR-route seam, a validator/LSP consult, or a back-fill that learns to type
 * cyclic rules). It is NOT a live corpus fix — no corpus rule graph has a cycle at all. That is a
 * narrower claim than this suite's draft made; it is the one the measurement supports.
 *
 * <p><b>RED at the pre-guard head — MEASURED at {@code 6c8e1544}, BOTH routes</b>
 * ({@code f30-red-default.log}, {@code f30-red-on.log}): <b>19 run / 7F / 0E / 1 skip</b>
 * (default) and <b>19 / 7F / 0E / 0 skip</b> ({@code -Pir-on}) — the SAME seven both routes,
 * <b>c0, c2, c3, d1, d2, d3, d4</b>, i.e. exactly the union the two guards claim and nothing
 * else. c1, c1b, d0, d5 and <b>every corpus control</b> are green at RED, which is the
 * zero-carrier claim asserted rather than argued. <b>GREEN at the CURRENT head is 19/0F on both
 * routes</b> — measured by chain run 2 at {@code 71a92e826}. At the run-1 head {@code e223ce19}
 * it was <b>19/1F</b>, the one failure being {@code corpus_control1} — <b>a mover this seat
 * owns</b>: it PASSED at
 * {@code 6c8e1544} and FAILED at {@code e223ce19} because a LATER commit of the same seat (law
 * 5) healed {@code GetUnderlierProductIdentifierLeg1.java} out of the pinned residue. The
 * within-seat LAW-81 hand-off; re-pinned in this file at {@code 80bbe57b}, not waived — and that
 * re-pin is exactly what took the suite to 0F. Detail below on
 * {@code KNOWN_RESIDUE_DRR7}.
 * <ul>
 *   <li><b>guard 1</b>: c0 (self cycle, through generation) and c2/c3 (the unit-boundary pins)
 *       FAIL — {@code StackOverflowError} out of {@code NavigationHandler}. c1 asserts only that
 *       the pipeline TERMINATES on a cyclic model, which it must in both states once the
 *       {@code RMissingType} refusal fronts it; it is the escape-catcher, not a verdict pin.
 *       c1b (link-only) is GREEN in BOTH states and is the positive control that isolates the
 *       failure to DOWNSTREAM of linking.</li>
 *   <li><b>guard 2</b>: d1, d2, d3, d4 FAIL — each rule takes the {@code List} surface it must
 *       not. d0 (the admit-path positive control) and d5 (the {@code sort} over-widen tripwire)
 *       are GREEN in BOTH states.</li>
 *   <li>every corpus control is GREEN in BOTH states — that is the zero-carrier claim, asserted.</li>
 * </ul>
 *
 * <p><b>The RED shape for the cycle tests, deliberately.</b> A {@code StackOverflowError} is an
 * {@code Error}, not an {@code Exception}, and letting one unwind the JUnit worker is a poor
 * instrument. Every cycle probe — the c0/c1 renders and the c2/c3 unit calls alike — therefore
 * runs on its OWN daemon thread with an explicit {@value #STACK_BYTES}-byte stack, inside a
 * {@code catch (Throwable)}, and is joined with a timeout. The pre-guard failure is then a clean
 * {@code assertNull(error)} assertion carrying the top stack frames, the post-guard pass is an
 * ordinary return, and a walk that hangs rather than overflows (the same defect wearing a
 * different hat) fails on the join timeout instead of wedging the suite. The three cycle models
 * are also handled SEPARATELY, so an overflow in one cannot poison another's fixture — and cannot
 * poison the guard-2 fixture, which keeps the two mutation lanes' failing sets disjoint.
 *
 * <p><b>MUTATION LANES (LAW 82). One lane MEASURED, one lane still a CLAIM — stated apart.</b>
 * Sets below are transcribed from the archived logs {@code f30-mut-&lt;lane&gt;.log} of BOTH
 * chain runs: run 1 at {@code e223ce19} and run 2 at the re-pin head {@code 71a92e826}. The
 * RAW counts quoted below are run 2's; run 1's are given in parentheses where they differ (they
 * differ by exactly the suite's then-standing failure, which the {@code 80bbe57b} re-pin retired,
 * so every NET set is identical across the two runs). <b>⚠ the lane loop runs the DEFAULT profile only</b>, so this suite's
 * IR-gated control is the one skip in every {@code 19 run / 1 skipped} lane and cannot fail in
 * a lane by construction.
 * <ul>
 *   <li><b>m-law2g-i</b> ({@code f30-mut-m-law2g-i.log}) — guard 1 alone reverted
 *       ({@code law2guards-apply.py --revert --guard1}).
 *       <b>NOT MEASURED: the mutation did not COMPILE, so not one test ran.</b> The log ends in
 *       {@code NavigationHandler.java:[5225,89] cannot find symbol  symbol: variable
 *       ruleVisited} → {@code BUILD FAILURE}. The chain recorded {@code EXIT=1}, which reads
 *       like a test failure in the status file and is NOT one — <b>a non-compiling mutation is
 *       not a measured anything</b>, neither a set nor a zero. <b>The cause, diagnosed:</b> the
 *       OPPOSITE-direction LAW-81 decay — a later law of this same seat (law 3, rung B) wrote a
 *       3-arg call against the LANDED guard, so reverting the guard's signature left that
 *       caller dangling. The lane has been RE-SITED to carry the law-3 rung-B late consumer as
 *       its own pair, and now asserts zero {@code ruleVisited} tokens post-revert.
 *       <b>MEASURED at the re-sited lane's standalone run (post-re-pin head {@code a5b3d8bd}):
 *       19/3F/0E/1S = c0, c2, c3 — exactly the claim (MATCH).</b> The two unit pins overflow
 *       inside {@code chainProvesMulti}, the self cycle overflows through generation. <b>c1
 *       measured UNMOVED</b>, exactly as claimed: its model is refused by the
 *       {@code RMissingType} back-fill before the predicate runs. c1b, d0–d5 and every corpus
 *       control also unmoved (control1 had been re-pinned by that head and stayed green
 *       under the lane).</li>
 *   <li><b>m-law2g-ii</b> ({@code f30-mut-m-law2g-ii.log}) — guard 2 alone reverted
 *       ({@code law2guards-apply.py --revert --guard2}). <b>MEASURED at the run-2 head
 *       {@code 71a92e826}: 19/4F/0E/1S = d1, d2, d3, d4 — exactly the claim (MATCH), with
 *       nothing else failing.</b> (Run 1 at {@code e223ce19} measured 19/<b>5</b>F/0E/1S, the
 *       extra member being {@code corpus_control1}, the suite's then-standing failure.) The
 *       correction that is NOT
 *       needed, named so no reader adds one: {@code corpus_control1} WAS the suite's standing
 *       failure at the run-1 head (see the RED paragraph above), and its print under this lane
 *       was <b>byte-identical to the GREEN leg's</b> — {@code in 1 file(s)} and the same single
 *       {@code IndicatorOfTheUnderlyingIndexRule.java fork=[1, 1, 1] golden=[1, 1, 0]} row. The
 *       lane moved it by NOTHING; net of that standing failure the measured set was exactly
 *       d1–d4. d0, d5, c0–c3 and every other corpus control unmoved, as claimed. <b>The netting
 *       is historical:</b> the {@code 80bbe57b} re-pin retired {@code corpus_control1}'s
 *       standing failure, so at the run-2 head this lane's raw set IS d1–d4 with nothing to net.</li>
 * </ul>
 * The two lanes' sets are DISJOINT by construction (separate models, separate renders,
 * textually disjoint anchor sets), which is the point: a lane that moves the other guard's tests
 * means the anchors are not as independent as the apply script asserts. <b>Both halves are
 * now measured: m-law2g-ii moved d1–d4 only, m-law2g-i moved c0/c2/c3 only — disjoint
 * exactly as constructed.</b>
 *
 * <p><b>The seat-31 lane (LAW 82) -- m-law0-kinds, the collapse-guard KIND EXTENSION severed
 * ({@code lanes31.py --law0-kinds}: the six kinds leave {@code collapsesToSingle}).</b> MEASURED at
 * the seat-31 chain head {@code f2a4d5c0} ({@code f31-mut-m-law0-kinds.log}): <b>26/6F/1S = e1,
 * e2, e3, e4, e5, e6</b> -- exactly the claim (MATCH); e0 (the admit-path positive control), d0-d5,
 * c0-c3 and every corpus control PASSED under the lane, so the extension is decline-only and its
 * six locks are each witnessed by one kind. RED at the seat-31 base {@code 0c8fc7dc4} measured the
 * same six on both routes (e1-e6, nothing else -- {@code f31-red-default.log} 24F over the seven
 * seat suites, this suite's share exactly 6), and GREEN at the head is 26/0F/1skip default,
 * 26/0F/0skip {@code -Pir-on}.
 *
 * <p><b>Decline-lock witness uniqueness.</b> Each of d1–d4 asserts the ABSENCE of
 * {@code implements ReportFunction<…, List<…>>} on its rule — a token the guard REMOVES, present
 * pre-guard and absent post-guard, and removed by NOTHING else in the seat (d0 keeps it, so the
 * absence is not a property of the fixture's shape). d5 asserts the PRESENCE of that same token,
 * so an over-widen that adds {@code RSortExpr} to {@code collapsesToSingle} fails d5 while leaving
 * d1–d4 green — the two directions are separately witnessed.
 */
class Law2GuardsSeatTest {

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

    /**
     * A generous 8 MiB — the POST-guard walk must clear it with room to spare (the fixture models
     * are three types and three rules), while an unbounded mutual recursion burns it in
     * milliseconds. Deliberately NOT small: a tight stack would risk a false RED from the
     * link/generate pipeline's own depth rather than from the walk under test.
     */
    private static final long STACK_BYTES = 8L * 1024 * 1024;

    /** A walk that hangs instead of overflowing is the SAME defect; the join timeout catches it. */
    private static final long JOIN_MILLIS = 120_000L;

    // =========================================================================
    // Part A — GUARD 1, the rule-reference cycle
    //
    // Each model puts the RULE REFERENCE on the LEFT of the `default`, which is
    // the only position that reaches chainProvesMulti's RRule arm (the hand-off
    // edge). A multi navigation on the left short-circuits chainProvesMulti to
    // true before any rule hop and proves nothing about the cycle.
    // =========================================================================

    /**
     * The shared shape. {@code CycleSelf}'s body is {@code extract (CycleSelf default "none")}:
     * <ul>
     *   <li>the ENGINE reads the whole body SINGLE — {@code CardinalityComputer} reads a bare
     *       {@code RDefaultExpr} SINGLE (:189) and its own {@code RRule} arm's identity set cuts
     *       the cyclic hop to SINGLE — so the engine-first check in {@code ruleOutputProvesMulti}
     *       does NOT answer, and the overlay is consulted. That is required: an engine MULTI would
     *       return before the walk ran and the fixture would prove nothing;</li>
     *   <li>the spine walk's leaves are {@code RImplicitVariable} (the extract's synthetic
     *       operand) and {@code RDefaultExpr}, so no collapse-guard leaf declines it and the
     *       default-leaf branch is reached;</li>
     *   <li>{@code chainProvesMulti(CycleSelf)} takes the {@code RSymbolReference} →
     *       {@code RRule} arm ({@code findEnclosingRule} is non-null — the reference is lexically
     *       inside a reporting rule), the engine's {@code computeRuleBody} answers SINGLE, and the
     *       arm falls through to {@code ruleOutputProvesMulti} — the hand-off.</li>
     * </ul>
     * PRE-guard the hand-off allocated a fresh set, so the round trip never terminates.
     */
    private static final String MODEL_CYCLE_SELF = """
            namespace census.seat30g1a
            version "1.0.0"

            type Ident:
                identifier string (0..1)

            type Leg:
                idents Ident (0..*)
                code string (0..1)

            reporting rule CycleSelf from Leg: <"c0 - the minimal cycle: the default LEFT is this rule">
                extract (CycleSelf default "none")
            """;

    /** THE PIN the review chartered — a two-rule cycle, neither rule self-referential. */
    private static final String MODEL_CYCLE_PAIR = """
            namespace census.seat30g1b
            version "1.0.0"

            type Ident:
                identifier string (0..1)

            type Leg:
                idents Ident (0..*)
                code string (0..1)

            reporting rule CycleP from Leg: <"c1 - P's default LEFT is Q">
                extract (CycleQ default "none")

            reporting rule CycleQ from Leg: <"c1 - Q's default LEFT is P: the cycle closes">
                extract (CycleP default "none")
            """;

    /**
     * The REACH model. A depth-2 guard would cut the pair and still hang here, so the three-rule
     * cycle is what proves the guard is a set and not a one-step memo.
     */
    private static final String MODEL_CYCLE_TRIO = """
            namespace census.seat30g1c
            version "1.0.0"

            type Ident:
                identifier string (0..1)

            type Leg:
                idents Ident (0..*)
                code string (0..1)

            reporting rule CycleA from Leg: <"c3 - A -> B">
                extract (CycleB default "none")

            reporting rule CycleB from Leg: <"c3 - B -> C">
                extract (CycleC default "none")

            reporting rule CycleC from Leg: <"c3 - C -> A: a three-rule cycle">
                extract (CycleA default "none")
            """;

    /**
     * c0 — the MINIMAL witness, and (MEASURED, first run) <b>the only cycle in this suite that
     * reaches the predicate through the FULL generation pipeline</b>. A self-referential rule
     * overflows PRE-guard for exactly the same reason a two-rule cycle does: the existing identity
     * set is re-allocated at every {@code ruleOutputProvesMulti} entry, so it never sees the rule
     * it is already inside.
     *
     * <p>The measured asymmetry that shapes this whole Part: the SELF cycle generates end-to-end
     * — its rule renders, and the assertions below hold on the rendered file — while the two- and
     * three-rule cycles are refused UPSTREAM by {@code RuleGenerator}'s Phase-X1 output-typeCall
     * back-fill ({@code RMissingType}, {@code RuleGenerator:305-315}). So c0 pins the guard
     * through generation, and c2/c3 pin it at the unit boundary. Keep both seams: c0 is the only
     * evidence that the guard matters on the real rendering path today, and c2/c3 are the only
     * evidence that it handles cycles the type computer refuses to type.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void c0_selfReferentialRuleTerminatesInsteadOfOverflowing() {
        Render r = renderOnDedicatedStack(MODEL_CYCLE_SELF, "census.seat30g1a", "seat30g1a.rosetta");
        assertNoEscape(r, "a self-referential rule reference on a `default` LEFT");
        assertSingleSurface(r, "CycleSelf");
    }

    /**
     * c1 — the GENERATION-LEVEL half of the pin: rendering the two-rule cycle must TERMINATE and
     * must not let anything escape the pipeline. Deliberately asserts termination ONLY.
     *
     * <p>MEASURED, first run: this model's rules never reach {@code chainProvesMulti} at all —
     * {@code RuleGenerator}'s Phase-X1 output-typeCall back-fill refuses them first
     * ({@code "workspace type inference returned RMissingType. Cannot back-fill output.typeCall"},
     * {@code RuleGenerator:305-315}), collected as a generation error rather than thrown out. So
     * the correct thing for this test to assert is exactly what it does assert: the pipeline
     * terminates promptly and cleanly on a cyclic model. Asserting a rendered SURFACE here would
     * pin the type computer's cycle refusal, not this guard — which is why the verdict half moved
     * to c2 at the unit boundary rather than being strengthened here.
     *
     * <p>Pre-guard the escape this catches is a {@code StackOverflowError} out of
     * {@code NavigationHandler.defaultSpineProvesMulti} / {@code chainProvesMulti} /
     * {@code ruleOutputProvesMulti}; the failure message prints the top frames so the seat can
     * confirm the overflow is in the walk under test and not somewhere else.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void c1_twoRuleCycleTerminatesInsteadOfOverflowing() {
        Render r = renderOnDedicatedStack(MODEL_CYCLE_PAIR, "census.seat30g1b", "seat30g1b.rosetta");
        assertNoEscape(r, "a two-rule cycle (CycleP's default LEFT is CycleQ and vice versa)");
    }

    /**
     * c1b — THE POSITIVE CONTROL for c1, c2 and c3, and the reason their REDs can be attributed.
     * It links the SAME cyclic model and does not generate. GREEN in BOTH states: the cycle is
     * grammar-legal and linkable, so a RED c1/c2/c3 is a defect DOWNSTREAM of linking and not a
     * parser or linker limitation. Without this control, "the cyclic fixture blew up" is an
     * unattributed fact (prove-the-instrument-can-fail: c1b is the arm that must NOT move).
     *
     * <p>It earned its keep on the first run: it is what let the {@code RMissingType} back-fill
     * refusal be identified as a TYPE-INFERENCE limit sitting between a cleanly-linked model and
     * the predicate, rather than as a broken fixture — which is what moved c2/c3 to the unit
     * boundary instead of retracting them.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void c1b_theCyclicModelLinksCleanly() {
        Throwable error = linkOnDedicatedStack(MODEL_CYCLE_PAIR, "seat30g1b.rosetta");
        assertNull(error, "the cyclic model must LINK — if it does not, c1's RED is not this"
                + " predicate's fault and the fixture must be re-shaped:\n" + stack(error));
    }

    /**
     * c2 — the VERDICT half of the pin, taken at the UNIT BOUNDARY.
     *
     * <p><b>Why not through generation (MEASURED, first run).</b> The two-rule cycle is refused
     * UPSTREAM of the predicate: {@code RuleGenerator}'s Phase-X1 output-typeCall back-fill throws
     * {@code "RuleGenerator: rule '…' expression has unresolved type — workspace type inference
     * returned RMissingType. Cannot back-fill output.typeCall"} ({@code RuleGenerator:305-315})
     * because the mutual reference leaves the rules' inferred types unresolvable. The rendering
     * pipeline therefore never reaches {@code chainProvesMulti} at all, so a generation-level
     * assertion here would pin the TYPE COMPUTER's cycle refusal and say nothing whatever about
     * this guard. c1 keeps that generation-level surface; this test moves to the seam where the
     * predicate actually lives.
     *
     * <p><b>What this test does.</b> Builds the workspace from the same cyclic model (which
     * {@code c1b} proves links cleanly), constructs an {@code ExpressionCompiler} the way the
     * repo's own unit seats do ({@code AliasHeadThenChainCardinalitySeatTest:239-245} —
     * {@code new GeneratorModel(workspace)} + {@code new JavaTypeTranslator(typeUtil)}), takes
     * {@code CycleP}'s body straight off the linked {@code RRule} ({@code RRule.expression()}),
     * and calls {@code NavigationHandler.chainProvesMulti(body, compiler)} on the dedicated
     * {@value #STACK_BYTES}-byte thread. Pre-guard that call recurses unboundedly
     * ({@code chainProvesMulti} → {@code RRule} arm → {@code ruleOutputProvesMulti} → fresh set →
     * {@code defaultSpineProvesMulti} → back to {@code chainProvesMulti}) and the daemon thread's
     * {@code catch (Throwable)} turns the {@code StackOverflowError} into a clean assertion
     * failure carrying the top frames. Post-guard it returns an ordinary boolean.
     *
     * <p><b>Both halves are asserted.</b> Terminating is necessary but not sufficient: the guard
     * must terminate by DECLINING. A guard that terminated by ADMITTING would claim MULTI on the
     * strength of a cycle — worse than the overflow — so the verdict is pinned {@code false} as
     * well.
     *
     * <p><b>What this means for the guard's reach.</b> The type computer's own cycle refusal
     * currently FRONTS the generator pipeline, so on today's corpus and today's entry points the
     * overlay recursion is unreachable through generation. The guard is therefore
     * defence-in-depth: it is reachable at this unit boundary, and at any future caller that
     * evaluates the predicate without the rule-emission type validation in front of it (the IR
     * route's own seams, a validator/LSP consult, or a back-fill that learns to resolve cyclic
     * types). That is a narrower claim than the draft made, and it is the measured one.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void c2_twoRuleCyclePredicateTerminatesAndDeclines() {
        Verdict v = chainProvesMultiOnRuleBody(
                MODEL_CYCLE_PAIR, "seat30g1b.rosetta", "CycleP");
        assertNull(v.error(), "chainProvesMulti on a two-rule cycle's body must TERMINATE."
                + " Pre-guard this is a StackOverflowError on the mutual path chainProvesMulti →"
                + " ruleOutputProvesMulti → defaultSpineProvesMulti → chainProvesMulti, because"
                + " the hand-off allocated a FRESH visited set on every re-entry. Threading the"
                + " on-path set through the hand-off is the fix; c1b proves the model links, so"
                + " this is the predicate's own defect.\n" + stack(v.error()));
        assertNotNull(v.value(), "the probe returned no verdict for CycleP");
        assertFalse(v.value(), "a cycle proves nothing — the guard must terminate by DECLINING,"
                + " so chainProvesMulti(CycleP.body) is false. A true here would mean the guard"
                + " cut the cycle by ADMITTING, which is worse than the overflow it replaced.");
    }

    /**
     * c3 — the REACH witness at the same unit boundary: a three-rule cycle {@code A → B → C → A}.
     * Pins that the guard is an on-path SET and not a one-step memo — a depth-2 special case
     * passes c2 and hangs here. Refused upstream by the same {@code RMissingType} back-fill as the
     * pair (MEASURED, first run), so it takes the same seam for the same reason.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void c3_threeRuleCyclePredicateTerminatesAndDeclines() {
        Verdict v = chainProvesMultiOnRuleBody(
                MODEL_CYCLE_TRIO, "seat30g1c.rosetta", "CycleA");
        assertNull(v.error(), "chainProvesMulti on a three-rule cycle's body must TERMINATE — a"
                + " depth-2 memo cuts the pair and still recurses unboundedly here:\n"
                + stack(v.error()));
        assertNotNull(v.value(), "the probe returned no verdict for CycleA");
        assertFalse(v.value(), "a three-rule cycle proves nothing — chainProvesMulti(CycleA.body)"
                + " must be false.");
    }

    // =========================================================================
    // Part B — GUARD 2, the unary collapsers
    // =========================================================================

    /**
     * Every rule here carries the SAME admitting spine — {@code extract head then extract
     * (&lt;multi chain&gt; default &lt;literal&gt;)} — and differs ONLY in its tail, so the tail
     * is the single variable and each test's verdict is attributable to one leaf kind.
     *
     * <p>{@code AmountOf} is the seat-29 {@code GradeFrom} trick: a SINGLE-output function tail
     * keeps the ENGINE reading the whole body SINGLE, so the overlay is what decides and the
     * fixture is not accidentally engine-answered. d0 and d5 carry it; d1–d4 do not need it (the
     * engine reads {@code min}/{@code max}/{@code count}/{@code reduce} SINGLE by itself), which
     * is the point — pre-guard the OVERLAY overrode an engine answer that was already correct.
     *
     * <p>{@code amt} rather than {@code amount}/{@code value}, and no identifier named
     * {@code min}/{@code max}/{@code count}/{@code reduce}/{@code sort}/{@code single} — all
     * lexer keywords.
     */
    private static final String MODEL_COLLAPSE = """
            namespace census.seat30g2
            version "1.0.0"

            type Ident:
                identifier string (0..1)
                amt number (0..1)

            type Leg:
                idents Ident (0..*)
                code string (0..1)

            type Holder:
                head Leg (0..1)
                legs Leg (0..*)
                mark string (0..1)

            func AmountOf: <"the SINGLE-output tail that keeps the ENGINE reading the body SINGLE">
                inputs:
                    n number (0..1)
                output:
                    r number (0..1)
                set r:
                    n

            reporting rule D0DefaultThenFnTail from Holder: <"d0 - the POSITIVE control: this spine ADMITS">
                extract head
                then extract (item -> idents -> amt default 0)
                then extract AmountOf(item)

            reporting rule D1MinAfterDefault from Holder: <"d1 - RMinExpr on the spine: decline">
                extract head
                then extract (item -> idents -> amt default 0)
                then min

            reporting rule D2MaxAfterDefault from Holder: <"d2 - RMaxExpr on the spine: decline">
                extract head
                then extract (item -> idents -> amt default 0)
                then max

            reporting rule D3CountAfterDefault from Holder: <"d3 - RCountExpr on the spine: decline">
                extract head
                then extract (item -> idents -> amt default 0)
                then count

            reporting rule D4ReduceAfterDefault from Holder: <"d4 - RReduceExpr on the spine: decline">
                extract head
                then extract (item -> idents -> amt default 0)
                then reduce [a, b -> a]

            reporting rule D5SortAfterDefault from Holder: <"d5 - RSortExpr is element-preserving: ADMIT">
                extract head
                then extract (item -> idents -> amt default 0)
                then sort
                then extract AmountOf(item)
            """;

    /**
     * d0 — THE POSITIVE CONTROL, and the test that makes d1–d4 mean something. It is the seat-29
     * a1 shape: the same {@code default} with the same multi LEFT and a non-collapsing tail, so
     * the overlay ADMITS and the rule takes the {@code List} surface. Without d0, d1–d4's
     * "declines" could all be vacuous — a fixture whose spine never reached the admit path at all
     * would pass every one of them. GREEN in BOTH states: neither guard touches it.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void d0_theAdmitPathIsLiveInThisFixture() throws IOException {
        String out = collapseRule("D0DefaultThenFnTail");
        assertContains(out, "implements ReportFunction<Holder, List<");
        assertTrue(codeOnly(out).contains(".getMulti();"),
                "the admit path must produce the MULTI terminal, or d1-d4 prove nothing:\n" + out);
    }

    /** d1 — {@code then min}. THE decline-lock the review chartered by name. */
    @Test
    @EnabledIf("builtinsAvailable")
    void d1_minTailKeepsTheSingleSurface() throws IOException {
        assertDeclined("D1MinAfterDefault", "min", "RMinExpr");
    }

    /** d2 — {@code then max}. */
    @Test
    @EnabledIf("builtinsAvailable")
    void d2_maxTailKeepsTheSingleSurface() throws IOException {
        assertDeclined("D2MaxAfterDefault", "max", "RMaxExpr");
    }

    /** d3 — {@code then count}. */
    @Test
    @EnabledIf("builtinsAvailable")
    void d3_countTailKeepsTheSingleSurface() throws IOException {
        assertDeclined("D3CountAfterDefault", "count", "RCountExpr");
    }

    /**
     * d4 — {@code then reduce}. {@code reduce} has no corpus usage at all (a {@code .rosetta} grep
     * over the 25 cells finds it only in prose), so if the RENDERER declines the shape rather than
     * the predicate, drop d4 and record it: {@code RReduceExpr}'s clause then rests on d1–d3 plus
     * the code review, and the notes must say so rather than leave a clause unwitnessed.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void d4_reduceTailKeepsTheSingleSurface() throws IOException {
        assertDeclined("D4ReduceAfterDefault", "reduce", "RReduceExpr");
    }

    /**
     * d5 — THE OVER-WIDEN TRIPWIRE, the other direction. {@code sort} is element-preserving: the
     * engine reads {@code RSortExpr} MULTI, {@code chainProvesMulti} treats
     * {@code distinct}/{@code reverse} the same way at the {@code RListOpExpr} node kind, and this
     * spine must still ADMIT. Adding {@code RSortExpr} to {@code collapsesToSingle} — the single
     * most likely wrong generalisation of guard 2 — fails HERE and nowhere else, so the
     * classification is pinned in both directions rather than only in the safe one.
     *
     * <p>PIN AT RED: if the engine already reads this body MULTI (the {@code sort} hop rather than
     * the {@code AmountOf} tail deciding), the test is still correct but is engine-answered rather
     * than overlay-answered and no longer tripwires the classification. Read the pre-law verdict
     * and re-shape the tail if so — do not assume it.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void d5_sortTailIsElementPreservingAndStillAdmits() throws IOException {
        String out = collapseRule("D5SortAfterDefault");
        assertContains(out, "implements ReportFunction<Holder, List<");
        assertTrue(codeOnly(out).contains(".getMulti();"),
                "sort is element-preserving — adding RSortExpr to collapsesToSingle is an"
                + " over-widen and this is the only test that sees it:\n" + out);
    }

    // =========================================================================
    // Part B-ext (seat 31) — the collapse-guard KIND EXTENSION: the six kinds the
    // seat-30 javadoc BANKED by name. The classification evidence is the engine's
    // own read — CardinalityComputer's global arms are UNCONDITIONAL for all six:
    //
    //   case RJoinExpr j -> SINGLE;              (:91)
    //   case RExistenceExpr e -> SINGLE;         (:95)
    //   case RCardinalityCheckExpr c -> SINGLE;  (:96)
    //   case ROnlyExistsExpr o -> SINGLE;        (:97)
    //   case RToStringExpr t -> SINGLE;          (:98)
    //   case RConversionExpr c -> SINGLE;        (:102)
    //
    // — which also answers the banked "RToStringExpr/RConversionExpr are
    // per-operator and need a probe": the engine's cardinality read has NO
    // operator dimension for either kind, so both classify with the other four.
    // Same decline-only shape as d1–d4; e0 is the admit-path positive control
    // (without it every e-decline could be vacuous), and d5 remains the
    // over-widen tripwire for the whole predicate.
    // =========================================================================

    /**
     * The seat-31 extension fixture. Same type spine as {@link #MODEL_COLLAPSE} (its own
     * namespace so the two renders stay independent); string-flavored leaves where the
     * collapser needs string items ({@code join}, {@code to-number}). No identifier named
     * {@code exists}/{@code join} — lexer keywords.
     */
    private static final String MODEL_COLLAPSE_EXT = """
            namespace census.seat31g
            version "1.0.0"

            type Ident:
                identifier string (0..1)
                amt number (0..1)

            type Leg:
                idents Ident (0..*)
                code string (0..1)

            type Holder:
                head Leg (0..1)
                legs Leg (0..*)
                mark string (0..1)

            func AmountOf: <"the SINGLE-output tail that keeps the ENGINE reading the body SINGLE">
                inputs:
                    n number (0..1)
                output:
                    r number (0..1)
                set r:
                    n

            reporting rule E0DefaultThenFnTail from Holder: <"e0 - the POSITIVE control: this spine ADMITS">
                extract head
                then extract (item -> idents -> amt default 0)
                then extract AmountOf(item)

            reporting rule E1ExistsAfterDefault from Holder: <"e1 - RExistenceExpr on the spine: decline">
                extract head
                then extract (item -> idents -> amt default 0)
                then exists

            reporting rule E2OneOfAfterDefault from Holder: <"e2 - RCardinalityCheckExpr (one-of): decline">
                extract head
                then extract (item -> idents -> amt default 0)
                then one-of

            reporting rule E3OnlyExistsAfterDefault from Holder: <"e3 - ROnlyExistsExpr: decline">
                extract head
                then extract (item -> idents default Ident { identifier: "x", amt: 0 })
                then extract (item -> identifier) only exists

            reporting rule E4JoinAfterDefault from Holder: <"e4 - RJoinExpr: decline">
                extract head
                then extract (item -> idents -> identifier default "x")
                then join ","

            reporting rule E5ToStringAfterDefault from Holder: <"e5 - RToStringExpr: decline">
                extract head
                then extract (item -> idents -> amt default 0)
                then to-string

            reporting rule E6ToNumberAfterDefault from Holder: <"e6 - RConversionExpr (to-number): decline">
                extract head
                then extract (item -> idents -> identifier default "0")
                then to-number
            """;

    /**
     * e0 — the POSITIVE CONTROL for the extension fixture, exactly as d0 is for d1–d4:
     * the same spine with a non-collapsing tail must ADMIT, or e1–e6 prove nothing.
     * GREEN in BOTH states.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e0_theAdmitPathIsLiveInTheExtFixture() throws IOException {
        String out = collapseExtRule("E0DefaultThenFnTail");
        assertContains(out, "implements ReportFunction<Holder, List<");
        assertTrue(codeOnly(out).contains(".getMulti();"),
                "the ext admit path must produce the MULTI terminal, or e1-e6 prove nothing:\n"
                + out);
    }

    /** e1 — {@code then exists}. {@code RExistenceExpr}, corpus-attested pipe form. */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_existsTailKeepsTheSingleSurface() throws IOException {
        assertDeclinedExt("E1ExistsAfterDefault", "exists", "RExistenceExpr");
    }

    /** e2 — {@code then one-of}. {@code RCardinalityCheckExpr} (the one-of/choice kind). */
    @Test
    @EnabledIf("builtinsAvailable")
    void e2_oneOfTailKeepsTheSingleSurface() throws IOException {
        assertDeclinedExt("E2OneOfAfterDefault", "one-of", "RCardinalityCheckExpr");
    }

    /**
     * e3 — {@code only exists}. {@code ROnlyExistsExpr} has no pipe (without-left) form, so
     * the collapser sits as the LAST extract's body — the same leaf position the spine walk
     * already reads for the {@code default} itself.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e3_onlyExistsTailKeepsTheSingleSurface() throws IOException {
        assertDeclinedExt("E3OnlyExistsAfterDefault", "only exists", "ROnlyExistsExpr");
    }

    /** e4 — {@code then join ","}. {@code RJoinExpr}, corpus-attested pipe form. */
    @Test
    @EnabledIf("builtinsAvailable")
    void e4_joinTailKeepsTheSingleSurface() throws IOException {
        assertDeclinedExt("E4JoinAfterDefault", "join", "RJoinExpr");
    }

    /** e5 — {@code then to-string}. {@code RToStringExpr} — unconditional SINGLE (:98). */
    @Test
    @EnabledIf("builtinsAvailable")
    void e5_toStringTailKeepsTheSingleSurface() throws IOException {
        assertDeclinedExt("E5ToStringAfterDefault", "to-string", "RToStringExpr");
    }

    /** e6 — {@code then to-number}. {@code RConversionExpr} — unconditional SINGLE (:102). */
    @Test
    @EnabledIf("builtinsAvailable")
    void e6_toNumberTailKeepsTheSingleSurface() throws IOException {
        assertDeclinedExt("E6ToNumberAfterDefault", "to-number", "RConversionExpr");
    }

    /**
     * The decline lock, shared by d1–d4 (and, through {@link #assertDeclinedExt}, by
     * e1–e6). The witness token is
     * {@code implements ReportFunction<…, List<…>>}: PRESENT pre-guard (the spine admitted),
     * ABSENT post-guard, and removed by nothing else in the seat — d0 keeps it on the same spine
     * with a different tail, so its absence here is a property of the TAIL, which is the claim.
     */
    private static void assertDeclined(String ruleName, String tail, String nodeKind)
            throws IOException {
        String out = collapseRule(ruleName);
        String why = "`" + tail + "` collapses a list to a single value (" + nodeKind
                + ", read SINGLE by CardinalityComputer) — the default's left proving MULTI"
                + " cannot survive it, and a List-signatured rule over a scalar body does not"
                + " type-check:\n" + out;
        assertTrue(!out.contains("implements ReportFunction<Holder, List<"), why);
        assertTrue(!codeOnly(out).contains(".getMulti();"), why);
        // PIN AT RED: add the byte-exact `implements` line and the assignOutput terminal captured
        // from the post-guard run. Read the element spelling (Integer for count, BigDecimal for
        // the rest) from the render — do not guess it.
    }

    /** The e-series twin of {@link #assertDeclined}, reading the EXT render. */
    private static void assertDeclinedExt(String ruleName, String tail, String nodeKind)
            throws IOException {
        String out = collapseExtRule(ruleName);
        String why = "`" + tail + "` collapses a list to a single value (" + nodeKind
                + ", read SINGLE by CardinalityComputer's unconditional arm) — the default's left"
                + " proving MULTI cannot survive it, and a List-signatured rule over a scalar body"
                + " does not type-check:\n" + out;
        assertTrue(!out.contains("implements ReportFunction<Holder, List<"), why);
        assertTrue(!codeOnly(out).contains(".getMulti();"), why);
    }

    // =========================================================================
    // Part C — the corpus controls. Both guards are ZERO-CARRIER, so every one
    // of these is GREEN in BOTH states; they are the assertion of that claim.
    // =========================================================================

    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CELL_C_ROOT = Path.of("../test-corpus/cdm/cdm-6.21.0");
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

    /** The two seat-29 law-2 WHOLE heals. Guard 2 firing on the law-2 admit path breaks them. */
    private static final String ESMA_CONSUMER =
            "drr/regulation/esma/emir/refit/trade/reports/IndicatorOfTheUnderlyingIndexRule.java";
    private static final String FCA_CONSUMER =
            "drr/regulation/fca/ukemir/refit/trade/reports/IndicatorOfTheUnderlyingIndexRule.java";

    /**
     * The seat-29 collapse-guard corpus witnesses — the two GREEN drr 7.x functions whose spines
     * are {@code (… default …) then distinct then only-element}. They exercise the
     * {@code RListOpExpr} half of {@code collapsesToSingle} on real bytes, so a mis-split of the
     * FLATTEN/DISTINCT/REVERSE terms while guard 2 is being written fails here.
     */
    private static final String UTI_FN =
            "drr/standards/iosco/uti/functions/GetUniqueTransactionIdentifier.java";
    private static final String USI_FN =
            "drr/standards/iosco/uti/functions/UniqueSwapIdentifierForValuation.java";

    /** c0a/c0b — the law-2 heals stay byte-identical. Head-stable: no residue set to re-pin. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c0a_law2EsmaHealStaysByteIdentical() throws IOException {
        lockA(ESMA_CONSUMER);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c0b_law2FcaHealStaysByteIdentical() throws IOException {
        lockA(FCA_CONSUMER);
    }

    /** c0c/c0d — the RListOpExpr-half witnesses stay byte-identical. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c0c_utiFunctionStaysByteIdentical() throws IOException {
        lockA(UTI_FN);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c0d_usiFunctionStaysByteIdentical() throws IOException {
        lockA(USI_FN);
    }

    /**
     * control1 — LAW 79, the whole-cell UNION scan on drr 7.0.0: per file the (MULTI rule
     * signature, {@code mapSingleToList} site, MULTI terminal) triple must equal golden's, file
     * for file over the UNION, beyond the NAMED residue. For a zero-carrier pair of guards this is
     * the primary instrument: guard 2 is decline-only, so a corpus carrier would REMOVE a heal and
     * GROW the residue, and guard 1 is answer-preserving, so any movement at all refutes the
     * on-path argument.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr7WholeCellOutputCardSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR7, DOMAIN_DRR7);
    }

    /** control2 — LAW 77 route parity on the named files; the band is route-identical. */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForTheNamedFiles() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), new ArrayList<>());
        for (String p : List.of(ESMA_CONSUMER, FCA_CONSUMER, UTI_FN, USI_FN)) {
            assertEquals(drrAOutput.get(p), irOut.get(p), "route divergence: " + p);
        }
    }

    /** control3 — LAW 79 on drr 5.61.0, the reach cell. */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkDrr561WholeCellOutputCardSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrBOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrBGenErrors,
                "drr 5.61.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrBOutput), scan(readGoldenTree(GOLDEN_B)), drrBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR561, DOMAIN_DRR561);
    }

    /**
     * control4 — LAW 79 on cdm 6.21.0, the NON-drr tripwire. Every other corpus control here is
     * drr-scoped; the seat-28 law-6 lesson is that a cdm/iosco over-fire is invisible to a
     * drr-scoped control. Guard 1 threads a set through {@code chainProvesMulti}, which is read by
     * 61 call sites across five handlers — the widest-reach edit in this pair — so a non-drr
     * tripwire is not optional here.
     */
    @Test
    @EnabledIf("cellCAvailable")
    void corpus_control4_forkCdm621WholeCellOutputCardSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(cdmCOutput, "cdm 6.21.0 generation did not run");
        assertEquals(List.of(), cdmCGenErrors,
                "cdm 6.21.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(cdmCOutput), scan(readGoldenTree(GOLDEN_C)), cdmCOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_CDM621, DOMAIN_CDM621);
    }

    /**
     * MEASURED AT THE LAW'S HEAD (LAW 73: pin the SET, not the count). Never carried forward on
     * faith (LAW 81: a control pinned to a residue set fires when another law heals a member,
     * and the re-pin is transcribed from the measured print at that head, with the whole chain
     * re-run there).
     *
     * <p><b>RE-TRANSCRIBED at the seat-30 chain head {@code e223ce19}</b> — 1 entry, VERBATIM from
     * this control's own failing print (LAW 81), down from 2. <b>THE MOVER, NAMED.</b> This control
     * PASSES at the seat's RED base {@code 6c8e1544} and FAILS at HEAD, i.e. the seat moved it
     * INTO failure: the pin was measured at the law-2-guards commit, and a LATER commit of the SAME
     * seat — <b>law 5</b> ({@code thenArgDeclKindFromCompiled}: the then-arg DECL reads the compiled
     * stamp instead of re-reading S) — healed {@code GetUnderlierProductIdentifierLeg1.java} WHOLE
     * in all four drr 7.x cells, taking its spurious {@code .mapSingleToList(} site (T2) with it.
     * That is the within-seat LAW-81 hand-off firing exactly as this javadoc predicted, and NOT a
     * regression of the guards: the guards remain zero-carrier, and the remaining entry is
     * unchanged.
     */
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of();
            // the IndicatorOfTheUnderlyingIndexRule row (fork=[1, 1, 1] golden=[1, 1, 0]) LEFT this list at seat 32: law D.2
            // (extractBodyMultiDefaultTernary, on law D.1's left deref) healed it WHOLE in all four drr 7.x cells;
            // transcribed from this control's own print (D2-trip1.log).

    /** MEASURED EMPTY at the law's head, and green in every leg the seat ran. */
    private static final List<String> KNOWN_RESIDUE_DRR561 = List.of();

    /** MEASURED EMPTY at the law's head — a non-empty set here would be an over-fire. */
    private static final List<String> KNOWN_RESIDUE_CDM621 = List.of();

    /**
     * The drr 7.0.0 union domain.
     *
     * <p><b>244 -> 243 at seat 30.</b> DERIVED, not printed: {@code assertUnionEqual} fires the
     * residue assert BEFORE the domain assert, so the seat-30 run never reached this line. Law 5's
     * whole heal of {@code GetUnderlierProductIdentifierLeg1} takes its fork triple to
     * {@code [0, 0, 0]} and golden's is already {@code [0, 0, 0]}, so {@code scan()}'s
     * {@code t1 + t2 + t3 > 0} predicate drops the file from BOTH sides of the union.
     *
     * <p><b>MEASURED-CONFIRMED at {@code 71a92e826}</b> (chain run 2). The run-2 GREEN leg is 19/0F
     * on both routes, so the residue assert PASSED and execution reached the domain assert — which
     * also passed against this value. 243 is no longer derived; the adjudication the paragraph
     * above asked the next chain for has happened, and it agreed.
     */
    private static final int DOMAIN_DRR7 = 243;

    /** MEASURED at the law's head — the drr 5.61.0 union domain. */
    private static final int DOMAIN_DRR561 = 229;

    /** MEASURED at the law's head — the cdm 6.21.0 union domain. */
    private static final int DOMAIN_CDM621 = 3;

    /**
     * (T1, T2, T3) = MULTI-output rule signatures ({@code implements ReportFunction<…, List<…>>}),
     * MULTI rule-call method sites ({@code .mapSingleToList(}) and MULTI whole-output terminals.
     * T1 is counted per LINE so the {@code , List<} must belong to the {@code implements} clause;
     * T2 and T3 are whole-file counts. No regex — plain literal matching on generated text, per
     * the engineering standard.
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(e.getValue());
            int t1 = 0;
            for (String line : code.split("\n", -1)) {
                if (line.contains("implements ReportFunction<") && line.contains(", List<")) {
                    t1++;
                }
            }
            int t2 = count(code, ".mapSingleToList(");
            int t3 = count(code, "}).getMulti();");
            if (t1 + t2 + t3 > 0) {
                out.put(e.getKey(), new int[] {t1, t2, t3});
            }
        }
        return out;
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

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - a negative value means an"
                + " unpinned call site");
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
                "(T1, T2, T3) differ beyond the named residue in " + mismatched.size() + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted token-bearing files (" + expectedDomain + ")");
    }

    // =========================================================================
    // Harness — the seat-29 corpus half verbatim, plus the dedicated-stack
    // fixture renderer this seat needs.
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> drrBOutput;
    private static List<String> drrBGenErrors;
    private static Map<String, String> cdmCOutput;
    private static List<String> cdmCGenErrors;

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
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_B_ROOT), errs);
            drrBGenErrors = errs;
        }
        if (cellCAvailable()) {
            List<String> errs = new ArrayList<>();
            cdmCOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.21.0", CELL_C_ROOT), errs);
            cdmCGenErrors = errs;
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

    /** The cell through the REAL {@code IRGeneration} seams (the D11 ON ring's wiring). */
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
            errors.forEach(e -> sink.add(e.getTargetPath() + " — " + e));
        }
    }

    private static void lockA(String path) throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run — corpus unavailable?");
        List<String> lockedErrors = drrAGenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drrAOutput.get(path);
        assertNotNull(generated, "not generated in drr 7.0.0: " + path);
        Path goldenPath = GOLDEN_A.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 7.0.0 output must byte-match golden (newline-normalized) for "
                + path + " — seat 30's two law-2 guards are ZERO-CARRIER: neither may move a"
                + " single corpus byte, and this file is where the movement would show first.");
    }

    private static void assertContains(String out, String needle) {
        assertTrue(out.contains(needle), "expected <" + needle + "> in:\n" + out);
    }

    /** Strip line and block comments plus string literals so a javadoc or label never counts as code. */
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

    // ---------------------------------------------------------------------
    // The fixture renderer.
    //
    // Unlike the seat-29 suite's render(), this one does NOT throw on a
    // generation error: each test filters the errors for its OWN rule, so a
    // broken rule fails exactly one test instead of taking the whole class
    // with it. That is what keeps the m-law2g-i and m-law2g-ii failing sets
    // disjoint, which is the whole point of two independently-revertible
    // guards.
    // ---------------------------------------------------------------------

    /** What one fixture render produced — or the Throwable that escaped it (the pre-guard witness). */
    private record Render(Map<String, String> output, List<String> errors, Throwable error) {}

    private static Render renderModel(String model, String namespace, String fileName)
            throws IOException {
        RModel main = AstBuilder.buildFromString(model, fileName);
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RLinkingResult linking = RWorkspace.build(models);
        GeneratorModel gm = new GeneratorModel(linking.workspace(),
                m -> namespace.equals(m.namespace()));
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
        RuleGenerator ruleGen = new RuleGenerator(gm, tt, fg);
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        ruleGen.generateClasses(main, "1.0", out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        return new Render(out, errors, null);
    }

    /**
     * Render on a dedicated daemon thread with an explicit stack, catching {@code Throwable}. A
     * {@code StackOverflowError} is then DATA — a clean assertion failure carrying its own top
     * frames — instead of an {@code Error} unwinding the JUnit worker, and a walk that hangs
     * rather than overflows fails on the join timeout instead of wedging the run.
     */
    private static Render renderOnDedicatedStack(String model, String namespace, String fileName) {
        return onDedicatedStack(fileName, () -> renderModel(model, namespace, fileName));
    }

    /** Link only — the c1b positive control that attributes a RED c1 to GENERATION. */
    private static Throwable linkOnDedicatedStack(String model, String fileName) {
        return onDedicatedStack(fileName, () -> {
            RModel main = AstBuilder.buildFromString(model, fileName);
            main.setVersion("0.0.0.test");
            List<RModel> models = new ArrayList<>();
            models.add(main);
            models.addAll(loadBuiltinsOnly());
            RWorkspace.build(models);
            return new Render(Map.of(), List.of(), null);
        }).error();
    }

    // ---------------------------------------------------------------------
    // The UNIT-BOUNDARY probe (c2 / c3).
    //
    // MEASURED, first run: generation refuses a multi-rule cycle upstream of
    // this predicate — RuleGenerator's Phase-X1 output-typeCall back-fill
    // throws on RMissingType (RuleGenerator:305-315) because a mutual rule
    // reference leaves the inferred types unresolvable — so the rendering
    // pipeline never reaches chainProvesMulti and a generation-level assertion
    // would pin the TYPE COMPUTER's refusal instead of this guard. The probe
    // calls the predicate directly on the linked rule's body, which is the seam
    // the guard actually lives at.
    // ---------------------------------------------------------------------

    /** A unit-boundary probe result: the predicate's verdict, or the Throwable that escaped it. */
    private record Verdict(Boolean value, Throwable error) {}

    @FunctionalInterface
    private interface VerdictTask {
        boolean run() throws Exception;
    }

    /**
     * Link the model, build the compiler the way the repo's own unit seats do
     * ({@code AliasHeadThenChainCardinalitySeatTest:239-245}), take the named rule's BODY off the
     * linked {@link RRule}, and evaluate {@code NavigationHandler.chainProvesMulti} on it — all on
     * the dedicated {@value #STACK_BYTES}-byte thread, so a pre-guard {@code StackOverflowError}
     * is DATA rather than an {@code Error} unwinding the JUnit worker.
     */
    private static Verdict chainProvesMultiOnRuleBody(String model, String fileName,
            String ruleName) {
        return probeOnDedicatedStack(fileName + "-" + ruleName, () -> {
            RModel main = AstBuilder.buildFromString(model, fileName);
            main.setVersion("0.0.0.test");
            List<RModel> models = new ArrayList<>();
            models.add(main);
            models.addAll(loadBuiltinsOnly());
            RWorkspace workspace = RWorkspace.build(models).workspace();
            GeneratorModel gm = new GeneratorModel(workspace);
            JavaTypeUtil typeUtil = new JavaTypeUtil();
            ExpressionCompiler compiler =
                    new ExpressionCompiler(gm, new JavaTypeTranslator(typeUtil), typeUtil);
            return NavigationHandler.chainProvesMulti(ruleBody(main, ruleName), compiler);
        });
    }

    /** The rule BODY off the linked model — {@code RModel.rootElements()} → {@code RRule.expression()}. */
    private static RExpression ruleBody(RModel model, String ruleName) {
        List<String> seen = new ArrayList<>();
        for (RRootElement element : model.rootElements()) {
            if (element instanceof RRule rule) {
                seen.add(rule.name());
                if (ruleName.equals(rule.name())) {
                    return rule.expression().orElseThrow(() -> new AssertionError(
                            "reporting rule " + ruleName + " carries no body expression"));
                }
            }
        }
        throw new AssertionError("reporting rule " + ruleName + " not found; rules in the model: "
                + seen);
    }

    private static Verdict probeOnDedicatedStack(String name, VerdictTask task) {
        final Object[] box = new Object[2];
        Thread t = new Thread(null, () -> {
            try {
                box[0] = task.run();
            } catch (Throwable th) {
                box[1] = th;   // StackOverflowError included — the pre-guard witness
            }
        }, "law2guards-probe-" + name, STACK_BYTES);
        t.setDaemon(true);
        t.start();
        try {
            t.join(JOIN_MILLIS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return new Verdict(null, ie);
        }
        if (t.isAlive()) {
            return new Verdict(null, new AssertionError(
                    "the unit probe did not finish within " + JOIN_MILLIS + " ms — an unbounded"
                    + " walk that is not overflowing is the SAME defect wearing a different hat"));
        }
        if (box[1] != null) {
            return new Verdict(null, (Throwable) box[1]);
        }
        return new Verdict((Boolean) box[0], null);
    }

    @FunctionalInterface
    private interface RenderTask {
        Render run() throws Exception;
    }

    private static Render onDedicatedStack(String name, RenderTask task) {
        final Object[] box = new Object[2];
        Thread t = new Thread(null, () -> {
            try {
                box[0] = task.run();
            } catch (Throwable th) {
                box[1] = th;   // StackOverflowError included — the pre-guard witness
            }
        }, "law2guards-" + name, STACK_BYTES);
        t.setDaemon(true);
        t.start();
        try {
            t.join(JOIN_MILLIS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return new Render(null, List.of(), ie);
        }
        if (t.isAlive()) {
            return new Render(null, List.of(), new AssertionError(
                    "the render did not finish within " + JOIN_MILLIS + " ms — an unbounded walk"
                    + " that is not overflowing is the SAME defect wearing a different hat"));
        }
        if (box[1] != null) {
            return new Render(null, List.of(), (Throwable) box[1]);
        }
        return (Render) box[0];
    }

    private static void assertNoEscape(Render r, String what) {
        assertNull(r.error(), "generating " + what + " must TERMINATE. Pre-guard this is a"
                + " StackOverflowError raised on the mutual path defaultSpineProvesMulti →"
                + " chainProvesMulti → ruleOutputProvesMulti → defaultSpineProvesMulti, because"
                + " the hand-off allocated a FRESH visited set on every re-entry. Threading the"
                + " on-path set through the hand-off is the fix; c1b proves the model links, so"
                + " this is a generation defect.\n" + stack(r.error()));
        assertNotNull(r.output(), "the render produced no output map for " + what);
    }

    /** The verdict half: the rule must keep the engine's SINGLE answer, not merely terminate. */
    private static void assertSingleSurface(Render r, String ruleName) {
        String path = ruleName + "Rule.java";
        List<String> own = r.errors().stream().filter(e -> e.contains(path)).toList();
        assertTrue(own.isEmpty(), "the generator reported errors for " + path + ": " + own);
        String out = lookup(r.output(), path);
        assertTrue(!out.contains("implements ReportFunction<Leg, List<"),
                "a cycle proves nothing — the guard must terminate by DECLINING, so " + ruleName
                + " keeps the engine's SINGLE answer:\n" + out);
        assertTrue(!codeOnly(out).contains(".getMulti();"),
                "the whole-output terminal must stay single for " + ruleName + ":\n" + out);
    }

    private static String stack(Throwable t) {
        if (t == null) {
            return "(no error)";
        }
        StringBuilder sb = new StringBuilder(t.toString()).append('\n');
        StackTraceElement[] frames = t.getStackTrace();
        for (int i = 0; i < Math.min(frames.length, 24); i++) {
            sb.append("    at ").append(frames[i]).append('\n');
        }
        if (frames.length > 24) {
            sb.append("    … ").append(frames.length - 24).append(" more frames\n");
        }
        return sb.toString();
    }

    private static Render collapseRender;

    private static Render collapse() throws IOException {
        if (collapseRender == null) {
            collapseRender = renderModel(MODEL_COLLAPSE, "census.seat30g2", "seat30g2.rosetta");
        }
        return collapseRender;
    }

    private static String collapseRule(String ruleName) throws IOException {
        Render r = collapse();
        String path = ruleName + "Rule.java";
        List<String> own = r.errors().stream().filter(e -> e.contains(path)).toList();
        assertTrue(own.isEmpty(), "the generator reported errors for " + path + ": " + own);
        return lookup(r.output(), path);
    }

    private static Render collapseExtRender;

    private static Render collapseExt() throws IOException {
        if (collapseExtRender == null) {
            collapseExtRender = renderModel(MODEL_COLLAPSE_EXT, "census.seat31g", "seat31g.rosetta");
        }
        return collapseExtRender;
    }

    private static String collapseExtRule(String ruleName) throws IOException {
        Render r = collapseExt();
        String path = ruleName + "Rule.java";
        List<String> own = r.errors().stream().filter(e -> e.contains(path)).toList();
        assertTrue(own.isEmpty(), "the generator reported errors for " + path + ": " + own);
        return lookup(r.output(), path);
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
            throw new AssertionError("[Law2GuardsSeatTest] builtins parse failures: "
                    + String.join("; ", failures));
        }
        return models;
    }
}
