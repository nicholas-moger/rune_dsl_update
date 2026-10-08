// SEAT 30 LAW 7 — facet multiEmptyElseArmToBuilder (apply script: drafts30/law7s-apply.py).
// SHIPPED.
// *** COMMIT ORDER IS LOAD-BEARING: law 6 landed FIRST. Law 6's PRESCRIBED LAW-81 tripwire
// *** disposition was NOT carried in law 7's own commit; the tripwire duly fired at the chain
// *** head e223ce19 and the disposition was applied in the seat's re-pin pass. ***
// Every PIN marked  ///PIN:  carries its MEASURED value, transcribed from the chain's own
// print. The chain that measured them: chain-all30.ps1 @ e223ce19; logs
// f30-{red,green}-{default,on}.log and f30-mut-m-law7{,-unflagged}.log.
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
 * SEAT 30, law 7 — facet {@code multiEmptyElseArmToBuilder}: <b>the implicit-empty MULTI else
 * arm reads {@code wrapArmsToBuilder}, exactly like its three siblings in the same ladder.</b>
 *
 * <p><b>The one arm that did not read the flag (LAW 69, four lines apart).</b>
 * {@code FunctionExpressionRenderer.renderConditionalAssignment}'s arms:
 * <pre>
 * then arm             if (wrapArmsToBuilder &amp;&amp; !"null".equals(thenAssign)) { … wrapToBuilder(…) }
 * EXPLICIT else arm    if (wrapArmsToBuilder &amp;&amp; !"null".equals(elseAssign)) { … wrapToBuilder(…) }
 * IMPLICIT empty else  elseRendered = targetName + " = Collections.&lt;X&gt;emptyList();";   &lt;- no flag
 * singleMetaOutput     if (wrapArmsToBuilder) { emptyMeta = wrapToBuilder(…) }
 * </pre>
 * The last arm's own javadoc pairs itself with the third — <i>"multiOutputEmptyElseItem and
 * this arm are mutually exclusive (MULTI-non-meta vs SINGLE-meta)"</i> — i.e. the author wrote
 * them as a pair and gave the {@code toBuilder} wrap to only one of them.
 *
 * <p><b>Golden vs fork</b> — drr 7.0.0–7.3.0 {@code GetBasket.java}, hunk 2 of 2:
 * <pre>
 * golden  basketConstituent = toBuilder(Collections.&lt;BasketConstituent&gt;emptyList());
 * fork    basketConstituent = Collections.&lt;BasketConstituent&gt;emptyList();
 * </pre>
 * Independently compile-breaking ({@code List<BasketConstituent>} into a
 * {@code List<BasketConstituent.BasketConstituentBuilder>} target) and independent of law 6:
 * neither fix implies the other, and {@code GetBasket} needs BOTH to go whole.
 * {@code wrapArmsToBuilder} is demonstrably TRUE for this statement — the then arm's
 * {@code toBuilder(fieldWithMetaBasketConstituent.getValue())} is produced by exactly that flag.
 *
 * <p><b>The gate, read from the source rather than inferred.</b> This arm is reached only when
 * {@code multiOutputEmptyElseItem != null}, whose producer (FER
 * {@code multiOutputEmptyElseItem}) requires: an EMPTY segment, a target that IS the enclosing
 * function's output, {@code gm.isMulti(output)}, and {@code detectMetaKind(output) == NONE}.
 * And {@code wrapArmsToBuilder} is {@code outputNeedsBuilder && operation.segment().isEmpty()}
 * — the segment is ALREADY empty here, so at this arm {@code wrapArmsToBuilder} is EXACTLY
 * {@code outputNeedsBuilder}. The fire condition is therefore precisely <i>a whole-output,
 * MULTI, non-meta, MODEL-typed function output whose conditional SET has an implicit /
 * {@code DefaultElseRule} empty else</i> — and the wrap then follows the SAME flag that decided
 * the then arm of the SAME statement.
 *
 * <p><b>⚠ A correction to {@code band30-check.md} §3b's blast-radius reading.</b> band30
 * measured the drr 7.0.0 GOLDEN tree at <b>39 bare</b> {@code X = Collections.<T>emptyList();}
 * sites vs <b>2 wrapped</b>, and inferred that every bare site has {@code wrapArmsToBuilder ==
 * false}. That inference is not what carries the safety: those 39 are a GOLDEN-TEXT census, and
 * a hoisted {@code final List<T> ifThenElseResultN;} local is rendered by the ITE-HOIST path,
 * not by {@code renderConditionalAssignment} — so most of the 39 never reach this arm at all.
 * What carries the safety is the gate above. <b>Do not ship on the inference:
 * {@code corpus_control1} scans EVERY {@code emptyList()} assignment in the cell (union,
 * wrapped and bare) and {@code corpus_control3} repeats it cross-corpus.</b> band30's own
 * in-band finding stands and is re-asserted by {@code corpus_control1}: inside the 128-file
 * band the wrapped/bare delta is carried by {@code GetBasket} ×4 and nothing else.
 *
 * <p><b>Green safety, by construction.</b> Where the arm fires with {@code wrapArmsToBuilder ==
 * true}, today's bare {@code List<Item>} is assigned to a {@code List<Item.ItemBuilder>} target
 * — a Java compile error — so every carrier is an already-waivered non-compiling mismatch. A
 * non-model (enum/builtin element) MULTI output keeps {@code outputNeedsBuilder == false} and
 * stays bare, byte-identical. {@code b1} is that pin.
 *
 * <p><b>Carriers.</b> {@code GetBasket.java} × drr 7.0.0/7.1.0/7.2.0/7.3.0, hunk 2 of 2.
 * <b>Law 7 alone heals ZERO whole files; laws 6 + 7 TOGETHER heal 4.</b> With law 6 already
 * landed, {@code corpus_c2} in this suite is the JOINT byte-WHOLE lock and is the seat's proof
 * that the two laws produce the WHOLE golden file with no residue.
 *
 * <p><b>⚠ LAW-81 DISPOSITION — PRESCRIBED HERE, APPLIED AT THE SEAT'S RE-PIN PASS.</b> Law 6's
 * suite ({@code CondArmMultiMetaElementDerefSeatTest}) pins the bare {@code emptyList()} line as
 * NAMED residue in its own {@code corpus_c1}. This law REMOVES that token, so that pin fires as
 * a HEAL tripwire the moment this commit lands. The prescribed disposition — written into law
 * 6's javadoc in advance — is: delete law 6's {@code HUNK2_RESIDUE} assertion and PROMOTE its
 * {@code corpus_c1} to a byte-WHOLE lock. <b>It was NOT carried in this commit.</b> The tripwire
 * duly fired at the seat-30 chain head {@code e223ce19} ({@code hunk 2 is law 7's and MUST still
 * be present at law 6's head … expected: <true> but was: <false>}) and the disposition was
 * applied in the seat's LAW-81 re-pin pass, alongside the fifteen other tripwires this seat's
 * heals fired. Both suites now carry a whole-file lock on {@code GetBasket}, deliberately, so a
 * later regression in either half is caught twice.
 *
 * <p><b>LAW 74.</b> The third of {@code GetBasket}'s three per-cell compile errors
 * ({@code List<BasketConstituent>} into {@code List<…Builder>}) is this law's.
 * {@code javac30 POST} reaches 0 for these four files only once BOTH laws have landed.
 *
 * <p><b>RED at the pre-law head — MEASURED at {@code 6c8e1544}, BOTH routes</b>
 * ({@code f30-red-default.log}, {@code f30-red-on.log}): <b>11 run / 4F / 0E / 1 skip</b>
 * (default) and <b>11 / 4F / 0E / 0 skip</b> ({@code -Pir-on}) — the SAME four both routes:
 * <b>a1, corpus_c1, corpus_c2, corpus_control1</b> ({@code control1} at {@code 1 file(s)},
 * naming {@code GetBasket.java fork=[0, 1, 1] golden=[1, 0, 1]}). <b>Drafted a1, a2, corpus_c1,
 * corpus_c2, corpus_control2; two corrections:</b> {@code a2} does NOT fail at RED (see the
 * failing-first note below), and {@code corpus_control2} does not fail even on {@code -Pir-on}
 * where it runs — {@code corpus_control1}, unclaimed, is the control that carries the evidence.
 * GREEN at the seat head is <b>11/0F</b> on both routes: no standing failure, so the lane sets
 * below are exact.
 *
 * <p><b>⚠ FAILING-FIRST NOTE ON a2 (LAW 66), MEASURED.</b> {@code a2} passes at the RED base AND
 * under {@code m-law7}, the whole-law revert, and no other lane in this suite moves it either.
 * <b>Nothing this seat ran gives a2 a witness.</b> It is a post-law mechanism pin, not a
 * failing-first pin, and must be read as such. The law's failing-first evidence is {@code a1} at
 * fixture grain plus {@code corpus_c1} / {@code corpus_c2} (carrier locks) and
 * {@code corpus_control1} at corpus grain — which is real evidence, and is corroborated by the
 * LAW-74 javac census closing {@code GetBasket}'s third per-cell error. <b>The falsifier that
 * would close the gap:</b> re-cut a2 so its PRE-law render actually carries the unwrapped arm,
 * and confirm it RED at {@code 6c8e1544} before trusting it.
 *
 * <p><b>MUTATION LANES — MEASURED (LAW 82). Sets transcribed from the archived logs
 * {@code f30-mut-&lt;lane&gt;.log} at {@code e223ce19}; corrections NAMED in place.</b>
 * <b>⚠ THE LANE LOOP RUNS THE DEFAULT PROFILE ONLY.</b> {@code corpus_control2} is
 * {@code @EnabledIf}-gated on the IR route and is the ONE skip in every lane's
 * {@code …/1 skipped} run, so it CANNOT fail in a lane by construction; it is UNMEASURED in
 * both lanes below.
 * <ul>
 *   <li><b>m-law7</b> ({@code f30-mut-m-law7.log}) = {@code law7s-apply.py --revert}.
 *       <b>MEASURED 11/4F/0E/1S: a1, corpus_c1, corpus_c2 and corpus_control1</b> (control1 at
 *       {@code 1 file(s)}, the same {@code GetBasket} row RED prints). <b>Drafted a1, a2,
 *       corpus_c1, corpus_c2 (+control2 on {@code -Pir-on}); measured without a2 and with
 *       corpus_control1 in place of the unmeasurable control2 — the difference explained</b> as
 *       in the two notes above. The lane reproduces the RED set exactly, so law 7 alone accounts
 *       for this suite's whole RED movement.</li>
 *   <li><b>m-law7-unflagged</b> ({@code f30-mut-m-law7-unflagged.log}) = {@code --mut-unflagged}
 *       (the {@code wrapArmsToBuilder} gate escaped — every implicit-empty MULTI else wraps).
 *       <b>MEASURED 11/2F/0E/1S: b1 and corpus_control1</b> — the control printing
 *       {@code 1 file(s)}, {@code ExtractPartySector.java fork=[1, 0, 1] golden=[0, 1, 1]},
 *       a DIFFERENT file from the one law 7 heals. <b>Drafted b1, corpus_control1,
 *       corpus_control3; measured without corpus_control3 — the difference explained:</b> the
 *       over-fire is drr 7.0.0-scoped and does not reach the second control's cell, so the gate's
 *       blast radius is narrower than the draft assumed. <b>The lane's real question is answered
 *       in the good direction:</b> it did NOT come back empty, so the "zero green movement" claim
 *       is <b>measured, not vacuous</b> — escaping the gate moves a green file
 *       ({@code ExtractPartySector}) into the band, which is exactly what the gate exists to
 *       prevent. The {@code wrapArmsToBuilder} gate is load-bearing at this corpus, and b1 is its
 *       fixture-grain witness.</li>
 * </ul>
 * <b>Lane tally for this suite: 0 MATCH · 2 MISMATCH-corrected</b> — both corrections NARROW the
 * drafted claims rather than expand them.
 */
class MultiEmptyElseArmToBuilderSeatTest {

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

    /** Cell A = drr 7.0.0 — the carrier cell. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /** Cell B = cdm 6.20.6 — CROSS-CORPUS over-fire control (the seat-28 law-6 lesson). */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
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
    // The fixture — the arm and its three siblings, minimal pairs on ONE flag
    // =========================================================================

    /**
     * Every function here is the SAME conditional-SET shape; only the OUTPUT's element type and
     * the else form vary, so each pair isolates exactly one input to {@code wrapArmsToBuilder}
     * or to {@code multiOutputEmptyElseItem}.
     * <ul>
     *   <li>{@code A1ModelMultiImplicitElse} — model element + MULTI + implicit else = THE
     *       CARRIER shape.</li>
     *   <li>{@code A2ExplicitElseSibling} — the same output with an EXPLICIT else: golden's
     *       arms are already {@code toBuilder}-wrapped, so the two else forms must AGREE after
     *       the law. This is the LAW-69 sibling-agreement pin.</li>
     *   <li>{@code B1EnumMultiImplicitElse} — a builtin/enum element ({@code outputNeedsBuilder
     *       == false}): the bare {@code emptyList()} is CORRECT and must survive. This is the
     *       {@code m-law7-unflagged} pin.</li>
     *   <li>{@code B2SingleMetaImplicitElse} — the SINGLE-meta sibling arm
     *       ({@code emptyMetaSet}): already correct, must be byte-unchanged.</li>
     *   <li>{@code B3SegmentedMultiImplicitElse} — a segment-carrying SET, which
     *       {@code multiOutputEmptyElseItem} rejects; the else must stay {@code = null;}.</li>
     * </ul>
     */
    private static final String MODEL = """
            namespace census.seat30law7
            version "1.0.0"

            enum Colour:
                RED
                BLUE

            type Item:
                code string (0..1)

            type MetaItem:
                code string (0..1)

            type Bag:
                items Item (0..*)
                more Item (0..*)
                colours Colour (0..*)
                metaOne MetaItem (0..1)
                    [metadata location]
                flag boolean (0..1)

            type Sink:
                held Item (0..*)

            func A1ModelMultiImplicitElse: <"a1 - THE CARRIER shape: model element, MULTI, implicit else">
                inputs:
                    bag Bag (0..1)
                output:
                    out Item (0..*)
                set out:
                    if bag -> flag exists
                    then bag -> items

            func A2ExplicitElseSibling: <"a2 - the EXPLICIT else sibling: the two else forms must agree">
                inputs:
                    bag Bag (0..1)
                output:
                    out Item (0..*)
                set out:
                    if bag -> flag exists
                    then bag -> items
                    else bag -> more

            func B1EnumMultiImplicitElse: <"b1 - DECLINE: a non-model element keeps the bare emptyList">
                inputs:
                    bag Bag (0..1)
                output:
                    out Colour (0..*)
                set out:
                    if bag -> flag exists
                    then bag -> colours

            func B2SingleMetaImplicitElse: <"b2 - DECLINE: the SINGLE-meta sibling arm is already correct">
                inputs:
                    bag Bag (0..1)
                output:
                    out MetaItem (0..1)
                        [metadata location]
                set out:
                    if bag -> flag exists
                    then bag -> metaOne

            func B3SegmentedMultiImplicitElse: <"b3 - DECLINE: a segment-carrying SET never reaches the arm">
                inputs:
                    bag Bag (0..1)
                output:
                    sink Sink (0..1)
                set sink -> held:
                    if bag -> flag exists
                    then bag -> items
            """;

    // =========================================================================
    // Part A — the failing-first pins (RED pre-law)
    // =========================================================================

    /**
     * a1 — the arm itself. A model-typed MULTI whole output's implicit-empty else must wrap.
     * Fails pre-law: the arm rendered the bare {@code Collections.<Item>emptyList()}.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_modelMultiImplicitElseWrapsToBuilder() throws IOException {
        String out = fn("A1ModelMultiImplicitElse.java");
        String code = normalize(out);
        assertTrue(code.contains("toBuilder(Collections.<Item>emptyList())"),
                "the implicit-empty MULTI else must be toBuilder-wrapped:\n" + out);
        assertTrue(!code.contains("= Collections.<Item>emptyList();"),
                "the bare emptyList assignment must be gone:\n" + out);
    }

    /**
     * a2 — the LAW-69 sibling-agreement pin: in the SAME shape, the EXPLICIT else arm and the
     * IMPLICIT else arm must both be {@code toBuilder}-wrapped. This is the test that catches a
     * future divergence in either half — the defect existed precisely because nothing checked
     * the four arms against each other.
     *
     * <p><b>⚠ MEASURED (LAW 66/82): a2 is NOT a failing-first pin.</b> It passes at the RED base
     * {@code 6c8e1544} on both routes and under {@code m-law7}, the whole-law revert
     * ({@code f30-mut-m-law7.log}, 11/4F without it), and {@code m-law7-unflagged} does not move
     * it either. So <b>no state this seat measured makes a2 fail</b>: its implicit half evidently
     * renders {@code toBuilder(} in the pre-law state too, which means the sibling-agreement it
     * asserts is not what law 7 changed in THIS fixture. a2 is a post-law mechanism pin —
     * genuinely useful against a FUTURE divergence, which is what it was written for — but it is
     * not evidence the law works, and the draft's placement of it in the RED set was wrong.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_theExplicitAndImplicitElseArmsAgree() throws IOException {
        String implicitOut = fn("A1ModelMultiImplicitElse.java");
        String explicitOut = fn("A2ExplicitElseSibling.java");
        assertTrue(normalize(explicitOut).contains("toBuilder("),
                "the EXPLICIT else sibling wraps today (the reference half):\n" + explicitOut);
        assertTrue(normalize(implicitOut).contains("toBuilder("),
                "so the IMPLICIT else arm must wrap too:\n" + implicitOut);
    }

    // =========================================================================
    // Part B — the decline pins (witness-unique on the token the flip ADDS)
    // =========================================================================

    /**
     * b1 — the {@code wrapArmsToBuilder} gate. A non-model (enum) MULTI element has
     * {@code outputNeedsBuilder == false}, so the bare form is CORRECT and must survive.
     * WITNESS-UNIQUE on {@code toBuilder(} being ABSENT — the token the flip adds. Fires under
     * {@code m-law7-unflagged}.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_enumMultiImplicitElseKeepsTheBareEmptyList() throws IOException {
        String out = fn("B1EnumMultiImplicitElse.java");
        String code = normalize(out);
        assertTrue(code.contains("= Collections.<Colour>emptyList();"),
                "a non-model MULTI element must keep the bare emptyList:\n" + out);
        assertTrue(!code.contains("toBuilder(Collections."),
                "and must NOT gain a toBuilder wrap:\n" + out);
    }

    /**
     * b2 — the SINGLE-meta sibling arm ({@code emptyMetaSet}) is mutually exclusive with this
     * one and already correct; it must be byte-unchanged. This pins that the edit stayed inside
     * its own {@code else if}.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_singleMetaImplicitElseIsUnmoved() throws IOException {
        String out = fn("B2SingleMetaImplicitElse.java");
        String code = normalize(out);
        assertTrue(code.contains(".builder().build()"),
                "the SINGLE-meta empty arm must still render the empty wrapper builder:\n" + out);
        assertTrue(!code.contains("Collections.<"),
                "and must not gain a list form:\n" + out);
    }

    /**
     * b3 — the WHOLE-OUTPUT conjunct, inherited from {@code multiOutputEmptyElseItem}'s own
     * {@code segment().isEmpty()} gate: a path-tailed SET never reaches this arm, so its else
     * stays the plain {@code = null;} form.
     *
     * <p><b>UN-WITNESSED, and stated as such (LAW 66/76).</b> No lane this seat ran moves b3 —
     * {@code m-law7} reverts the whole law and {@code m-law7-unflagged} escapes the
     * {@code wrapArmsToBuilder} gate, and neither reaches a SEGMENT-tailed SET, because the
     * {@code segment().isEmpty()} conjunct declines it for an independent reason. So b3 locks a
     * conjunct that no shipped mutation escapes; it ships on the shape argument, honestly scored
     * as un-witnessed rather than load-bearing. <b>The falsifier that would give it a witness</b>
     * is a lane that severs the {@code segment().isEmpty()} test itself, which this seat did not
     * build. (Same disposition as the {@code CondArmMultiMetaElementDerefSeatTest} b2 sibling.)
     * The PREMISE asserts below stop the negative passing vacuously in the meantime.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_segmentedSetImplicitElseIsUnmoved() throws IOException {
        String out = fn("B3SegmentedMultiImplicitElse.java");
        String code = normalize(out);
        // PREMISE anchors (the BlockLambda b2/b3 pattern): a file-wide negative passes
        // vacuously if the fixture never reached the shape, so assert the shape FIRST.
        assertTrue(code.contains("getFlag"),
                "PREMISE: the fixture's conditional must have rendered (its `bag -> flag exists`"
                + " condition), else this decline is vacuous:\n" + out);
        assertTrue(code.contains("getItems"),
                "PREMISE: the MULTI then-arm must have rendered, else b3 pins nothing:\n" + out);
        assertTrue(!code.contains("toBuilder(Collections."),
                "a segment-carrying SET must not take this arm:\n" + out);
    }

    // =========================================================================
    // Part C — the corpus (LAW 79/80; c2 is the JOINT whole-file lock)
    // =========================================================================

    private static final String GB = "drr/base/trade/basket/functions/GetBasket.java";

    /**
     * control0 — golden is the oracle (prove the instrument can fail). Read from GOLDEN bytes
     * only: golden wraps GetBasket's implicit-empty else, and the drr 7.0.0 golden tree carries
     * BOTH forms ({@code toBuilder(Collections.…emptyList())} and the bare assignment), so a
     * scan that could only ever see one of them cannot pass this quietly.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenWrapsTheCarrierAndKeepsBothFormsInTheCell() throws IOException {
        String g = normalize(Files.readString(GOLDEN_A.resolve(GB)));
        assertTrue(g.contains("basketConstituent = toBuilder(Collections.<BasketConstituent>emptyList());"),
                "golden must wrap the implicit-empty else in " + GB);
        Map<String, int[]> golden = scan(readGoldenTree(GOLDEN_A));
        int wrapped = golden.values().stream().mapToInt(t -> t[0]).sum();
        int bare = golden.values().stream().mapToInt(t -> t[1]).sum();
        assertTrue(wrapped > 0 && bare > 0,
                "the drr 7.0.0 golden tree must carry BOTH emptyList forms (measured "
                        + wrapped + " wrapped / " + bare + " bare) - otherwise the scan is"
                        + " one-sided and the control proves nothing");
        ///PIN: band30-check.md §3b measured 2 wrapped / 39 bare over this tree. The assert above
        ///PIN: is deliberately the WEAK form (both > 0) rather than the exact pair: it is a
        ///PIN: two-sidedness premise, and pinning the pair would make control0 fail on any
        ///PIN: unrelated corpus movement. control0 is green in every leg the seat ran.
    }

    /** c1 — the hunk-2 LINE lock. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_getBasketHunk2MatchesGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        String gen = drrAOutput.get(GB);
        assertNotNull(gen, "not generated: " + GB);
        String code = normalize(gen);
        assertTrue(code.contains(
                        "basketConstituent = toBuilder(Collections.<BasketConstituent>emptyList());"),
                "hunk 2: the implicit-empty else must be toBuilder-wrapped in " + GB + ":\n" + gen);
        assertTrue(!code.contains("basketConstituent = Collections.<BasketConstituent>emptyList();"),
                "hunk 2: the bare form must be gone in " + GB + ":\n" + gen);
    }

    /**
     * c2 — THE JOINT WHOLE-FILE LOCK (LAW 80). With law 6 landed in the preceding commit and
     * law 7 here, {@code GetBasket}'s TWO hunks are both closed and the file must equal golden
     * BYTE FOR BYTE, in all four cells. This is the seat's proof that laws 6 + 7 together
     * produce the WHOLE golden file with no residue — if this fails, state the residue
     * explicitly rather than weakening the lock.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c2_getBasketIsByteIdenticalToGoldenWithLaw6() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        String gen = drrAOutput.get(GB);
        assertNotNull(gen, "not generated: " + GB);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(GB))), normalize(gen),
                GB + " must be byte-identical to golden once laws 6 AND 7 have landed"
                        + " (2 hunks, 2 laws, 4 cells)");
    }

    /**
     * control1 — LAW 79, the whole-cell UNION scan on drr 7.0.0. Domain = every file whose
     * GOLDEN <b>or</b> FORK text carries a {@code Collections.<X>emptyList()} assignment, in
     * EITHER form; per file the (wrapped, bare, total-emptyList) triple must equal golden's
     * beyond the NAMED residue. <b>This is the control that replaces band30 §3b's inference
     * with a measurement.</b>
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr700WholeCellEmptyListArmsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR7, DOMAIN_DRR7);
    }

    /** control2 — LAW 77 route parity for the carrier. */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForTheCarrier() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), new ArrayList<>());
        assertEquals(drrAOutput.get(GB), irOut.get(GB), "route divergence: " + GB);
    }

    /** control3 — LAW 79 CROSS-CORPUS (cdm 6.20.6): the same UNION triple. */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkCdm6206WholeCellEmptyListArmsEqualGoldenFileByFile() throws IOException {
        assertNotNull(cdmBOutput, "cdm 6.20.6 generation did not run");
        assertEquals(List.of(), cdmBGenErrors,
                "cdm 6.20.6 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(cdmBOutput), scan(readGoldenTree(GOLDEN_B)), cdmBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_CDM6206, DOMAIN_CDM6206);
    }

    ///PIN: MEASURED EMPTY at the law's head — the strongest form of this pin, and it held on
    ///PIN: both routes. The lanes prove the empty is a verdict rather than a blind spot: RED and
    ///PIN: m-law7 each print 1 file(s) (GetBasket), and m-law7-unflagged prints 1 file(s) naming
    ///PIN: a DIFFERENT file (ExtractPartySector) — the gate's measured over-fire.
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of();

    ///PIN: MEASURED EMPTY at the law's head. A NON-EMPTY result is the ENTERING signal.
    private static final List<String> KNOWN_RESIDUE_CDM6206 = List.of();

    ///PIN: MEASURED at the first corpus run (LAW 73).
    ///PIN: Falsifier: the domain assert itself
    ///PIN: (assertEquals(expectedDomain, universe.size())) failing on a later run. NOT the
    ///PIN: harness's "MEASURED domain=" print, which only surfaces at the -1 SENTINEL and is
    ///PIN: never emitted once a real value is pinned; on a run where the RESIDUE assert fails
    ///PIN: first the domain assert is simply unreachable.
    private static final int DOMAIN_DRR7 = 253;

    ///PIN: MEASURED at the first corpus run (LAW 73). Same falsifier as DOMAIN_DRR7.
    private static final int DOMAIN_CDM6206 = 191;

    /**
     * (T1, T2, T3) per file: {@code toBuilder(Collections.<X>emptyList())} assignments (the
     * form this law adds), bare {@code = Collections.<X>emptyList();} assignments (the form it
     * removes), and the file's TOTAL {@code Collections.<} occurrence count (the over-fire net
     * — deliberately global, so a rewrite that swaps a form somewhere else in the cell cannot
     * cancel out).
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            int[] t = new int[3];
            for (String line : normalize(e.getValue()).split("\n")) {
                String s = line.trim();
                if (s.contains("= toBuilder(Collections.<") && s.contains(">emptyList());")) {
                    t[0]++;
                } else if (s.contains("= Collections.<") && s.endsWith(">emptyList();")) {
                    t[1]++;
                }
                int from = 0;
                while ((from = s.indexOf("Collections.<", from)) >= 0) {
                    t[2]++;
                    from += "Collections.<".length();
                }
            }
            if (t[0] + t[1] + t[2] > 0) {
                out.put(e.getKey(), t);
            }
        }
        return out;
    }

    // =========================================================================
    // The union assert (LAW 73: pin the SET, not the count)
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
        // The domain-pin flow (LAW 73): the sentinel failure PRINTS the measured values.
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - transcribe from this"
                        + " print: MEASURED domain=" + universe.size()
                        + " residue=" + mismatched);
        assertEquals(knownResidue, mismatched,
                "(T1, T2, T3) differ beyond the named residue in " + mismatched.size() + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted token-bearing files (" + expectedDomain
                        + ")");
    }

    // =========================================================================
    // Harness (the seat-26/27/28/29 suite shape verbatim)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> cdmBOutput;
    private static List<String> cdmBGenErrors;

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
            cdmBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CELL_B_ROOT), errs);
            cdmBGenErrors = errs;
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

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat30law7.rosetta");
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
            fixtureOut = render(m -> "census.seat30law7".equals(m.namespace()));
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
            throw new AssertionError("[MultiEmptyElseArmToBuilderSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
