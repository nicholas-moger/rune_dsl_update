// SEAT 30 LAW 6 — facet condArmMultiMetaElementDeref (apply script: drafts30/law6s-apply.py).
// SHIPPED.
// *** COMMIT ORDER IS LOAD-BEARING: law 6 landed BEFORE law 7, and the LAW-81 heal tripwire
// *** that ordering created FIRED and had its prescribed disposition applied — see corpus_c1.
// Every PIN marked  ///PIN:  carries its MEASURED value, transcribed from the chain's own
// print. The chain that measured them: chain-all30.ps1 @ e223ce19; logs
// f30-{red,green}-{default,on}.log and f30-mut-m-law6{,-nosuppress,-nomulti,-else}.log.
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
 * SEAT 30, law 6 — facet {@code condArmMultiMetaElementDeref}: <b>at a whole-output MULTI
 * (non-meta) SET, a meta-wrapped conditional arm derefs ELEMENT-WISE inside the mapper chain —
 * never through the single hoist + null-guard block.</b>
 *
 * <p><b>The two decisions that disagreed.</b> In
 * {@code FunctionExpressionRenderer.renderConditionalAssignment}, the arm's unwrap suffix and
 * the arm's meta-deref shape are taken independently, one statement apart:
 * <pre>
 * JavaStatementBuilder thenUnwrapped = … unwrapForAssignment(thenCompiled,
 *         multiOutputEmptyElseItem != null ? ".getMulti()" : ".get()");   &lt;- knows it is MULTI
 * …
 * String thenDeref = renderMetaValueDerefOrNull(targetName, thenUnwrapped, thenBranch, …);
 *                                                            &lt;- renders the SINGLE hoist anyway
 * </pre>
 * {@code renderMetaValueDerefOrNull} is cardinality-BLIND, and facet
 * {@code metaCollapseConsumerTypeStamp} (PR #362) then re-presents the MapperC RHS as
 * {@code MapperS.of(<chain>.getMulti()).get()} — a {@code List<FieldWithMetaX>} assigned to a
 * {@code FieldWithMetaX} local.
 *
 * <p><b>The producer already exists, on the ADD side only (LAW 69).</b>
 * {@code coerceAddValueMetaItem}'s whole-output arm ends
 * {@code return compiler.coerceNavigationReceiver(compiled, scope);} and is reachable ONLY
 * under {@code if (isAdd)}. There was no SET sibling. Facet
 * {@code thenOutputMetaDerefRecovery} (PR #263) states the missing law in its OWN javadoc —
 * <i>"a multi output derefs each element via a MapperC map (not the single-value hoist the
 * deref emits)"</i> — but only ever used it to SUPPRESS a recovery, never to RENDER the
 * element-wise form.
 *
 * <p><b>Golden vs fork</b> — drr 7.0.0–7.3.0 {@code drr/base/trade/basket/functions/
 * GetBasket.java}, hunk 1 of 2:
 * <pre>
 * golden  basketConstituent = toBuilder(MapperS.of(basket)
 *             .&lt;FieldWithMetaBasketConstituent&gt;mapC("getBasketConstituent", …)
 *             .&lt;BasketConstituent&gt;map("Type coercion", fwm -&gt; fwm.getValue())
 *             .getMulti());
 * fork    final FieldWithMetaBasketConstituent fieldWithMetaBasketConstituent =
 *             MapperS.of(MapperS.of(basket).&lt;FieldWithMetaBasketConstituent&gt;mapC(…).getMulti()).get();
 *         if (fieldWithMetaBasketConstituent == null) { basketConstituent = null; }
 *         else { basketConstituent = toBuilder(fieldWithMetaBasketConstituent.getValue()); }
 * </pre>
 * Note the hop carries NO null guard: the receiver is a {@code MapperC}, and the coercer's
 * element-wise form is the un-guarded one (the #320 wrapper-kind law). {@code a2} pins that.
 *
 * <p><b>THE LAW IS TWO STEPS, AND THE SECOND ONE IS NOT OPTIONAL.</b>
 * {@code band30-check.md} §3a says that once the chain is coerced,
 * {@code renderMetaValueDerefOrNull}'s {@code itemType instanceof RJavaWithMetaValue} gate
 * "declines by itself". <b>That is false, and this suite's {@code m-law6-nosuppress} lane
 * measures it.</b> That gate is only the FIRST rung; behind it sit four AST reads — #249
 * {@code tryTerminalMetaMapperType}, #382 {@code metaSigKeep}, #376 {@code listLiteral}, #285
 * delegation — which see the UNCOERCED {@code thenBranch}. {@code GetBasket}'s terminal nav
 * attribute {@code basketConstituent} IS meta-annotated ({@code [metadata location]}), so
 * {@code terminalNavAttr} + {@code metaNavResultType} would recover
 * {@code FieldWithMetaBasketConstituent} again and the single hoist would render ON TOP of the
 * coerced chain. The law therefore (a) coerces before the unwrap and (b) does not ALSO run the
 * single-hoist deref on the arm it coerced.
 *
 * <p><b>The flag is GATE-based, not identity-based</b> ({@code thenArmMetaCoerced} is set from
 * "MULTI seat AND wrapper item", not from {@code coerced != thenCompiled}): at this seat the
 * single hoist is the wrong SHAPE regardless of whether the coercion service rendered a hop.
 *
 * <p><b>Green safety, by construction, in both sub-cases.</b> The gate is
 * {@code multiOutputEmptyElseItem != null} — whose producer (FER {@code multiOutputEmptyElseItem})
 * requires an EMPTY segment, a target that IS the enclosing function's output, {@code isMulti},
 * and {@code detectMetaKind == NONE} — AND a wrapper item type. In that intersection today the
 * deref either fires (a single value assigned to a {@code List<…Builder>} target) or declines
 * (wrapper elements assigned into a bare-element list): neither compiles, so no green file can
 * carry the pre-fix form.
 *
 * <p><b>⚠ THE SIZING IS UNMEASURED — the controls are load-bearing.</b> No
 * {@code P30-SETSINK} probe was run this round; {@code band30-check.md} §3e names the exact
 * open question ("how many carriers reach {@code site=setArm} with {@code multiOut=true} AND
 * {@code wrapper=true} across all 25 cells, and whether any GREEN file sits in that
 * intersection"). {@code corpus_control1} (whole-cell UNION, drr 7.0.0) and
 * {@code corpus_control3} (cdm 6.20.6, cross-corpus) are the measurement, and the
 * <b>mid-seat whole-matrix checkpoint MUST run after this law</b> — the seat-28 law-6
 * precedent, where a cell-scoped control could not see 10 ENTERED cdm files.
 *
 * <p><b>Carriers.</b> {@code GetBasket.java} × drr 7.0.0/7.1.0/7.2.0/7.3.0
 * ({@code lines=9 hunks=2}, all four cells byte-identical, digest {@code 5715f552c892}).
 * <b>Law 6 owns hunk 1 only.</b> Hunk 2 (the bare {@code Collections.<X>emptyList()} else arm)
 * is a SECOND, independent defect and is law 7's. <b>Law 6 alone heals ZERO whole files</b>;
 * {@code corpus_c1} was written as a hunk-1 LINE lock with the hunk-2 line NAMED as residue, and
 * is now — law 7 having landed — a hunk-1 line lock PLUS the byte-whole lock (see the tripwire
 * paragraph below).
 *
 * <p><b>⚠ LAW-81 HEAL TRIPWIRE, PRESCRIBED IN ADVANCE — FIRED, AND ITS DISPOSITION APPLIED.</b>
 * {@code corpus_c1}'s named residue ({@code HUNK2_RESIDUE}) was a token law 7 REMOVES. Law 7
 * ({@code multiEmptyElseArmToBuilder}) landed in the very next commit and the tripwire fired
 * exactly as written at the seat-30 chain head {@code e223ce19} — {@code hunk 2 is law 7's and
 * MUST still be present at law 6's head … expected: <true> but was: <false>} — which is the
 * tripwire working, not a regression. <b>The prescribed disposition has been applied:</b> the
 * {@code HUNK2_RESIDUE} constant and its assertion are DELETED and {@code corpus_c1} is PROMOTED
 * to a byte-WHOLE lock against golden (law 7's suite carries the joint whole-file lock too; both
 * are kept, deliberately, so a later regression in either half is caught by both suites).
 *
 * <p><b>LAW 74.</b> {@code GetBasket} does not compile at this head — three distinct errors per
 * cell ({@code List<FieldWithMetaBasketConstituent>} into a {@code FieldWithMetaBasketConstituent}
 * local; {@code toBuilder(<single BasketConstituent>)} into a {@code List<…Builder>};
 * {@code List<BasketConstituent>} into a {@code List<…Builder>}). {@code javac30 PRE} must hold
 * these four files; the first two errors are law 6's, the third is law 7's, and POST must be 0
 * only after BOTH land.
 *
 * <p><b>RED at the pre-law head — MEASURED at {@code 6c8e1544}, BOTH routes</b>
 * ({@code f30-red-default.log}, {@code f30-red-on.log}): <b>11 run / 5F / 0E / 1 skip</b>
 * (default) and <b>11 / 5F / 0E / 0 skip</b> ({@code -Pir-on}) — the SAME five both routes:
 * <b>a1, a2, a3, corpus_c1, corpus_control1</b> ({@code control1} at {@code 13 file(s)};
 * {@code corpus_c1} failing on its HUNK-1 assert, {@code "hunk 1: the element-wise hop must
 * render"}). <b>Drafted a1, a2, a3, corpus_c1, corpus_control2; measured corpus_control1
 * instead of corpus_control2 — the difference explained:</b> {@code corpus_control2} does not
 * fail at RED even on {@code -Pir-on}, where it actually runs (RED-on is 5F, not 6F), and
 * {@code corpus_control1} — never claimed — does. The whole-cell control is what carries this
 * law's corpus evidence.
 *
 * <p><b>⚠ THE STANDING FAILURE THE LANE SETS BELOW ARE NETTED AGAINST, AND WHY IT IS GONE.</b>
 * At the measurement head {@code e223ce19} the GREEN leg was <b>11/1F</b>: {@code corpus_c1}
 * failing on the LAW-81 heal tripwire ({@code "hunk 2 is law 7's and MUST still be present at
 * law 6's head"}) because law 7 had landed. Every lane run at that head therefore shows
 * {@code corpus_c1} in its log, and the lane sets below are read by WHICH assert fired — the
 * hunk-1 assert (real movement) or the hunk-2 tripwire (standing). The tripwire's prescribed
 * disposition has since been applied in the seat's re-pin commit: {@code HUNK2_RESIDUE} is
 * deleted and {@code corpus_c1} is a byte-WHOLE lock. <b>MEASURED-CONFIRMED, not expected: chain
 * run 2 at {@code 71a92e826} reads this suite <b>11/0F</b> on both routes.</b> The standing
 * failure is gone, the netted sets below ARE the exact sets, and the netting rule above is
 * HISTORICAL — it applies to the run-1 logs the sets were transcribed from, not to this head.
 * (This paragraph previously closed with "an EXPECTATION the final chain must confirm, not a
 * measurement"; the chain confirmed it, so the sentence is discharged rather than left standing.)
 *
 * <p><b>MUTATION LANES — MEASURED (LAW 82). Sets transcribed from the archived logs
 * {@code f30-mut-&lt;lane&gt;.log} of BOTH chain runs — run 1 at {@code e223ce19} and run 2 at the
 * re-pin head {@code 71a92e826}. The RAW counts quoted below are run 2's, with run 1's in
 * parentheses where they differ (by exactly the suite's then-standing failure, retired at
 * {@code 80bbe57b}); every NET set is identical across the two runs. Corrections NAMED in
 * place.</b>
 * <b>⚠ THE LANE LOOP RUNS THE DEFAULT PROFILE ONLY.</b> {@code corpus_control2} is
 * {@code @EnabledIf}-gated on the IR route and is the ONE skip in every lane's
 * {@code …/1 skipped} run, so it CANNOT fail in a lane by construction; it is UNMEASURED in
 * every lane below.
 * <ul>
 *   <li><b>m-law6</b> ({@code f30-mut-m-law6.log}) = {@code law6s-apply.py --revert}.
 *       <b>MEASURED 11/5F/0E/1S: a1, a2, a3, corpus_c1 and corpus_control1</b> (control1 at
 *       {@code 12 file(s)}). <b>Drafted a1, a2, a3, corpus_c1 (+control2 on {@code -Pir-on});
 *       measured that set with corpus_control1 in place of the unmeasurable control2 — the
 *       difference explained</b> as in the RED paragraph. <b>And one reading correction that
 *       matters:</b> {@code corpus_c1} here is NOT the standing tripwire — the log shows it
 *       failing on {@code "hunk 1: the element-wise hop must render in …GetBasket.java"}, the
 *       law's OWN assert, at a different line from the GREEN leg's tripwire message. A count-only
 *       reading would have scored corpus_c1 as unmoved; the assert text says it moved. The lane
 *       reproduces the RED set exactly.</li>
 *   <li><b>m-law6-nosuppress</b> ({@code f30-mut-m-law6-nosuppress.log}) = {@code --mut-nosuppress}
 *       — step (a) WITHOUT step (b): the chain is coerced but the deref call is left unguarded.
 *       <b>MEASURED 11/2F/0E/1S: a3 and corpus_c1</b> (again on the HUNK-1 assert, i.e. real
 *       movement). <b>Drafted a1, a3, corpus_c1; measured without a1 — the difference
 *       explained:</b> with the chain coerced, a1's assertion (the MULTI meta arm derefs
 *       element-wise before {@code .getMulti()}) is already satisfied by step (a) alone; only the
 *       leftover single hoist a3 pins survives. <b>The lane's real question is answered:</b> it
 *       came back NON-empty, so the {@code band30-check.md} §3a correction is REFUTED — <b>the
 *       suppression is NOT dead weight</b>, it is what removes the hybrid render, and the law
 *       correctly ships as (a)+(b) rather than being re-cut to (a) alone.</li>
 *   <li><b>m-law6-nomulti</b> ({@code f30-mut-m-law6-nomulti.log}) = {@code --mut-nomulti} (the
 *       {@code multiOutputEmptyElseItem != null} conjunct escaped, so SINGLE-cardinality arms
 *       coerce too). <b>MEASURED at the run-2 head {@code 71a92e826}: 11/4F/0E/1S = b1, b3,
 *       corpus_control1 and corpus_control3.</b> (Run 1 at {@code e223ce19} measured
 *       11/<b>5</b>F/0E/1S, the extra member being the then-standing {@code corpus_c1} heal
 *       tripwire, retired by the {@code 80bbe57b} re-pin; the NET set is the same either way.)
 *       <b>Drafted b1, b2, control1, control3;
 *       measured b3 in place of b2 — the difference explained:</b> escaping the conjunct reaches
 *       the SEGMENTED SET arm (b3's shape, {@code "a segment-carrying SET must not take the
 *       whole-output element-wise cut"}) rather than b2's, so the over-fire's true boundary is
 *       the segmented set, not the shape b2 pins. The corpus half of the claim is emphatically
 *       confirmed and then some: <b>control1 blows out to {@code 54 file(s)}</b> (from a pinned
 *       0) and <b>control3 to {@code 36 file(s)}</b> in cdm 6.20.6 — a cross-corpus over-fire.
 *       This is the strongest lane in the suite and the conjunct is measured load-bearing.</li>
 *   <li><b>m-law6-else</b> ({@code f30-mut-m-law6-else.log}) = {@code --mut-else} — the SYMMETRY
 *       lane: the same coercion applied at the EXPLICIT else arm as well.
 *       <b>MEASURED at the run-2 head {@code 71a92e826}: 11/0F/0E/1S — an unambiguous,
 *       contamination-free EMPTY set, and EMPTY was the claim: MATCH.</b> (Run 1 measured
 *       11/<b>1</b>F/0E/1S, the one member being the then-standing {@code corpus_c1} tripwire →
 *       the same net EMPTY, inferred then and now measured directly.)
 *       The designed adjudication discharged in the direction
 *       declared up front: <b>the asymmetry is FREE</b> — no measured carrier needs the else-arm
 *       coercion at 25 cells or in the fixture set, so the smaller cut is the right one and the
 *       law does not widen. ({@code GetBasket}'s else arm is the IMPLICIT empty arm, which is
 *       law 7's seat, exactly as the claim reasoned.)</li>
 * </ul>
 * <b>Lane tally for this suite: 1 MATCH (the designed zero, ADJUDICATED) · 3
 * MISMATCH-corrected.</b> Note that TWO of the three corrections make the law's case STRONGER
 * than the draft did (the suppression is load-bearing; the conjunct's over-fire is cross-corpus).
 */
class CondArmMultiMetaElementDerefSeatTest {

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

    /** Cell A = drr 7.0.0 — the carrier cell (7.1/7.2/7.3 are byte-identical here). */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /**
     * Cell B = cdm 6.20.6 — CROSS-CORPUS over-fire control. cdm is the densest corpus in
     * meta-annotated attributes and in whole-output conditional SETs, so if this law reaches
     * beyond its measured population anywhere, it reaches here. <b>Chosen deliberately over a
     * second drr cell</b> because the seat-28 law-6 regression that the mid-seat checkpoint
     * caught was exactly 10 ENTERED cdm files no drr-scoped control could see.
     */
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
    // The fixture — GetBasket's source verbatim, minimised, plus the decline twins
    // =========================================================================

    /**
     * The carrier's own source ({@code drr base-trade-basket-func.rosetta:123-131}) is already
     * minimal:
     * <pre>
     * func GetBasket:
     *     inputs:  basket Basket (0..1)
     *     output:  basketConstituent BasketConstituent (0..*)
     *     set basketConstituent:
     *         if basket -&gt; basketConstituent exists
     *         then basket -&gt; basketConstituent
     * </pre>
     * with {@code basketConstituent BasketConstituent (1..*) [metadata location]} on the input
     * type (cdm {@code observable-asset-type.rosetta:209-210}) — i.e. a MULTI meta-wrapped
     * navigation into a MULTI, non-meta, MODEL-typed whole output with an IMPLICIT else.
     *
     * <p><b>The decline twins</b> — each escapes exactly one conjunct:
     * <ul>
     *   <li>{@code B1SingleMetaArm} — the SAME shape with a SINGLE output. The single hoist +
     *       null guard is CORRECT there and must survive byte-identically. This is the pin
     *       {@code m-law6-nomulti} fires.</li>
     *   <li>{@code B2MultiBareArm} — MULTI output, meta-FREE navigation. No wrapper item, so
     *       the coercion self-declines and no {@code "Type coercion"} hop may appear.</li>
     *   <li>{@code B3SegmentedMetaArm} — a SEGMENT-carrying (path-tailed) SET, which
     *       {@code multiOutputEmptyElseItem} rejects outright. The seat must be unreachable.
     *       </li>
     * </ul>
     */
    private static final String MODEL = """
            namespace census.seat30law6
            version "1.0.0"

            type Constituent:
                code string (0..1)

            type Wrapper:
                one Constituent (0..1)

            type Bag:
                metaMulti Constituent (1..*)
                    [metadata location]
                metaOne Constituent (0..1)
                    [metadata location]
                bareMulti Constituent (0..*)

            type Sink:
                held Constituent (0..*)

            func A1MultiMetaArmDerefsElementWise: <"a1/a2/a3 - THE CARRIER (GetBasket's source verbatim)">
                inputs:
                    bag Bag (0..1)
                output:
                    out Constituent (0..*)
                set out:
                    if bag -> metaMulti exists
                    then bag -> metaMulti

            func B1SingleMetaArm: <"b1 - DECLINE: a SINGLE output keeps the hoist + null guard">
                inputs:
                    bag Bag (0..1)
                output:
                    out Constituent (0..1)
                set out:
                    if bag -> metaOne exists
                    then bag -> metaOne

            func B2MultiBareArm: <"b2 - DECLINE: no wrapper item, the coercion self-declines">
                inputs:
                    bag Bag (0..1)
                output:
                    out Constituent (0..*)
                set out:
                    if bag -> bareMulti exists
                    then bag -> bareMulti

            func B3SegmentedMetaArm: <"b3 - DECLINE: a segment-carrying SET never reaches the seat">
                inputs:
                    bag Bag (0..1)
                output:
                    sink Sink (0..1)
                set sink -> held:
                    if bag -> metaMulti exists
                    then bag -> metaMulti
            """;

    ///PIN: MEASURED from the rendered fixture text at the law head — confirmed by b1 asserting
    ///PIN: this exact hoist local and passing green, and by a3 asserting its absence.
    private static final String WRAPPER = "FieldWithMetaConstituent";

    // =========================================================================
    // Part A — the failing-first pins (RED pre-law)
    // =========================================================================

    /**
     * a1 — the ELEMENT-WISE hop lands inside the chain, before the terminal unwrap. Fails
     * pre-law: the arm rendered the single hoist block instead.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_multiMetaArmDerefsInsideTheChainBeforeGetMulti() throws IOException {
        String out = fn("A1MultiMetaArmDerefsElementWise.java");
        String code = normalize(out);
        assertTrue(code.contains("\"Type coercion\"") && code.contains(").getMulti()"),
                "the MULTI meta arm must deref element-wise before .getMulti():\n" + out);
        int hop = code.indexOf("\"Type coercion\"");
        int mult = code.indexOf(").getMulti()");
        assertTrue(hop >= 0 && mult > hop,
                "the coercion hop must precede the terminal .getMulti():\n" + out);
    }

    /**
     * a2 — the WRAPPER-KIND pin. The receiver is a {@code MapperC}, so the coercer's
     * element-wise form is the UN-GUARDED lambda ({@code fwm -> fwm.getValue()}), not the
     * MapperS null-guarded one (the #320 law). This is the assert that separates a correct
     * element-wise deref from a MapperS-shaped one that happens to sit in the right place.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_theElementWiseHopCarriesNoNullGuard() throws IOException {
        String out = fn("A1MultiMetaArmDerefsElementWise.java");
        String code = normalize(out);
        int hop = code.indexOf("\"Type coercion\"");
        assertTrue(hop >= 0, "the coercion hop was not rendered:\n" + out);
        String tail = code.substring(hop, Math.min(code.length(), hop + 200));
        assertTrue(!tail.contains("== null ? null :"),
                "a MapperC element-wise hop must carry NO null guard (the #320 law):\n" + tail);
    }

    /**
     * a3 — the SUPPRESSION pin, and the whole point of step (b): the single hoist block must be
     * GONE. WITNESS-UNIQUE on the three tokens the flip removes together — the wrapper-typed
     * local, its {@code == null} guard, and the {@code targetName = null;} arm. Fires under
     * {@code m-law6-nosuppress}, where the hoist would render ON TOP of the coerced chain.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_theSingleHoistBlockIsGone() throws IOException {
        String out = fn("A1MultiMetaArmDerefsElementWise.java");
        String code = normalize(out);
        assertTrue(!code.contains("final " + WRAPPER + " "),
                "the single-value hoist local must not be emitted at a MULTI seat:\n" + out);
        assertTrue(!code.contains("MapperS.of(MapperS.of("),
                "the #362 MapperS.of(<MapperC>.getMulti()).get() re-present must be gone:\n" + out);
    }

    // =========================================================================
    // Part B — the decline pins (witness-unique on tokens the flip must NOT touch)
    // =========================================================================

    /**
     * b1 — the MULTI conjunct. At a SINGLE-cardinality output the hoist + null guard is the
     * CORRECT render and must survive byte-identically. WITNESS-UNIQUE on the hoist local +
     * the {@code == null} guard: exactly the tokens a3 asserts absent at the MULTI seat.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_singleMetaArmKeepsTheHoistAndNullGuard() throws IOException {
        String out = fn("B1SingleMetaArm.java");
        String code = normalize(out);
        assertTrue(code.contains("final " + WRAPPER + " "),
                "a SINGLE meta arm must keep its hoist local:\n" + out);
        assertTrue(code.contains("== null) {"),
                "a SINGLE meta arm must keep its null guard:\n" + out);
    }

    /**
     * b2 — the WRAPPER conjunct. A meta-free MULTI arm has no wrapper item, so the coercion
     * self-declines and nothing may be added. WITNESS-UNIQUE on the token the flip ADDS.
     *
     * <p><b>MEASURED CORRECTION (LAW 82).</b> b2 was named in {@code m-law6-nomulti}'s drafted
     * set; <b>that lane measured b1 and b3, not b2</b> ({@code f30-mut-m-law6-nomulti.log},
     * 11/5F). Escaping the {@code multiOutputEmptyElseItem != null} conjunct does not reach a
     * meta-FREE arm — the wrapper conjunct declines it for an independent reason — so b2 stays
     * green. b2 is therefore un-witnessed by any lane the seat ran; it locks a conjunct that no
     * shipped mutation escapes. The falsifier that would give it a witness is a lane that
     * severs the WRAPPER test itself, which this seat did not build.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_multiBareArmGainsNoCoercionHop() throws IOException {
        String out = fn("B2MultiBareArm.java");
        String code = normalize(out);
        // PREMISE anchors (the BlockLambda b2/b3 pattern): a file-wide negative passes
        // vacuously if the fixture never reached the shape, so assert the shape FIRST.
        assertTrue(code.contains(".getMulti()"),
                "PREMISE: the fixture must reach the MULTI whole-output SET arm (its terminal"
                + " .getMulti() unwrap), else this decline is vacuous:\n" + out);
        assertTrue(code.contains("BareMulti"),
                "PREMISE: the arm must render the meta-FREE navigation the wrapper conjunct"
                + " declines on, else b2 pins nothing:\n" + out);
        assertTrue(!code.contains("\"Type coercion\""),
                "a meta-FREE MULTI arm must gain no coercion hop:\n" + out);
    }

    /**
     * b3 — the WHOLE-OUTPUT conjunct, inherited from {@code multiOutputEmptyElseItem}'s own
     * {@code segment().isEmpty()} gate. A path-tailed SET never reaches this seat, so its arm
     * must be byte-unchanged. This is the pin that keeps the law off the segment-SET family
     * that PR #390 owns on the ADD side.
     *
     * <p><b>MEASURED (LAW 82) — b3 is this suite's strongest decline lock.</b> It FIRES under
     * {@code m-law6-nomulti} ({@code f30-mut-m-law6-nomulti.log}, 11/5F), which the draft
     * attributed to b2. So the over-fire boundary the {@code multiOutputEmptyElseItem != null}
     * conjunct actually protects is the SEGMENTED SET, and b3 is the fixture-grain witness for
     * it — alongside the same lane's corpus blow-out ({@code corpus_control1} to 54 files,
     * {@code corpus_control3} to 36 in cdm 6.20.6). b3 is green at RED and under every other
     * lane, so the witness is specific rather than incidental.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_segmentedSetArmIsUnmoved() throws IOException {
        String out = fn("B3SegmentedMetaArm.java");
        String code = normalize(out);
        ///PIN: MEASURED — this assertion is the minimum (no element-wise hop introduced) and it
        ///PIN: DISCRIMINATES: it fires under m-law6-nomulti and is green in every other state.
        assertTrue(!code.contains("\"Type coercion\"") || code.contains("final " + WRAPPER + " "),
                "a segment-carrying SET must not take the whole-output element-wise cut:\n" + out);
    }

    // =========================================================================
    // Part C — the corpus (LAW 79/80: hunk-1 line asserts + the PROMOTED byte-whole lock,
    //          both halves of the 2-defect file now landed — laws 6 and 7)
    // =========================================================================

    private static final String GB = "drr/base/trade/basket/functions/GetBasket.java";

    /**
     * control0 — golden is the oracle (prove the instrument can fail). Read from GOLDEN bytes
     * only: golden's then arm carries the element-wise hop before {@code .getMulti()} and
     * carries NO hoist local; and golden's else arm carries the {@code toBuilder(} wrap that
     * law 7 owns (asserted here so this suite states the FULL golden shape even while law 6
     * only delivers half of it).
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenDerefsElementWiseAtTheMultiSeat() throws IOException {
        String g = normalize(Files.readString(GOLDEN_A.resolve(GB)));
        assertTrue(g.contains(".<BasketConstituent>map(\"Type coercion\", "
                        + "fieldWithMetaBasketConstituent -> fieldWithMetaBasketConstituent.getValue())"),
                "golden must carry the un-guarded element-wise hop in " + GB);
        assertTrue(!g.contains("final FieldWithMetaBasketConstituent "),
                "golden must carry NO single hoist local in " + GB);
        assertTrue(g.contains("toBuilder(Collections.<BasketConstituent>emptyList())"),
                "golden's else arm carries the toBuilder wrap (law 7's hunk) in " + GB);
    }

    /**
     * c1 — <b>the hunk-1 line lock, PROMOTED to a byte-WHOLE lock (the prescribed LAW-81
     * disposition, applied).</b> The tripwire fired exactly as written at the seat-30 chain head
     * {@code e223ce19}: {@code hunk 2 is law 7's and MUST still be present at law 6's head …
     * expected: <true> but was: <false>}. Law 7
     * ({@code multiEmptyElseArmToBuilder}) landed in the commit after law 6's and REMOVED the bare
     * {@code basketConstituent = Collections.<BasketConstituent>emptyList();} token this test used
     * to name as residue, so the class javadoc's prescription applies verbatim: the
     * {@code HUNK2_RESIDUE} constant and its assertion are DELETED and this test is promoted to a
     * whole-file lock. Both hunk-1 line assertions are KEPT above the byte compare on purpose — if
     * a later regression moves only hunk 1, the line assert names WHICH half moved before the byte
     * compare dumps the whole file. Law 7's own suite keeps the joint whole-file lock
     * ({@code MultiEmptyElseArmToBuilderSeatTest.corpus_c2}); both are kept, deliberately.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_getBasketHunk1MatchesGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        String gen = drrAOutput.get(GB);
        assertNotNull(gen, "not generated: " + GB);
        String code = normalize(gen);
        assertTrue(code.contains(".<BasketConstituent>map(\"Type coercion\", "
                        + "fieldWithMetaBasketConstituent -> fieldWithMetaBasketConstituent.getValue())"),
                "hunk 1: the element-wise hop must render in " + GB + ":\n" + gen);
        assertTrue(!code.contains("final FieldWithMetaBasketConstituent "),
                "hunk 1: the single hoist local must be gone in " + GB + ":\n" + gen);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(GB))), code,
                GB + " must be byte-identical to golden once laws 6 AND 7 have landed"
                        + " (2 hunks, 2 laws, 4 cells) — the promoted LAW-81 disposition");
    }

    /**
     * control1 — LAW 79, the whole-cell UNION scan on drr 7.0.0. Domain = every file whose
     * GOLDEN <b>or</b> FORK text carries a {@code "Type coercion"} hop or a wrapper-typed hoist
     * local; per file the (element-wise hops, hoist locals, {@code .getMulti()} unwraps) triple
     * must equal golden's beyond the NAMED residue. <b>This control carries the law's UNMEASURED
     * sizing question and must not be weakened.</b>
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr700WholeCellArmDerefsEqualGoldenFileByFile() throws IOException {
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

    /**
     * control3 — LAW 79 CROSS-CORPUS (cdm 6.20.6), the same UNION triple. This is the control
     * the seat-28 law-6 regression proved necessary: a drr-scoped control cannot see cdm files
     * entering.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkCdm6206WholeCellArmDerefsEqualGoldenFileByFile() throws IOException {
        assertNotNull(cdmBOutput, "cdm 6.20.6 generation did not run");
        assertEquals(List.of(), cdmBGenErrors,
                "cdm 6.20.6 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(cdmBOutput), scan(readGoldenTree(GOLDEN_B)), cdmBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_CDM6206, DOMAIN_CDM6206);
    }

    ///PIN: MEASURED at the law's head — the drr 7.0.0 files whose triple still differs, 11
    ///PIN: entries, transcribed VERBATIM from control1's own print. The lanes MOVE it and that
    ///PIN: is how they are scored: RED prints 13 file(s), m-law6 prints 12, and m-law6-nomulti
    ///PIN: blows out to 54 (with control3 at 36 in cdm 6.20.6) — the measured over-fire.
    ///PIN: RE-MEASURED at the seat-31 chain head f2a4d5c0 — 9 entries, transcribed VERBATIM from
    ///PIN: control1's own failing print (LAW 81): two rows LEFT, both noted inline below.
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of(
            // the GetBasketConstituents row (fork=[1, 2, 1] golden=[1, 3, 1]) LEFT this list: law A.1
            // (ctorSetterMetaDerefFunctionHost + the rung-3 ctorSetterHoistTextOrder numbering) took its tuples to
            // golden's in all four drr 7.x cells; the file stays BANDED on law A.2's lambda-name line, which this
            // tuple set cannot see; transcribed from this control's own print (A1-trip1.log).
            // UnderlierBasketIdentifier.java (was fork=[0, 0, 0] golden=[0, 0, 2]) left this list at
            // seat 31: law 4a (choiceOptionNavLadderDeepHop - the FER SET-seat option ladder walks the NESTED choice option tree
            // through ChoiceSwitchSupport.findChoiceOptionPath and derefs the META option hop into the bare output)
            // healed it WHOLE in all four drr 7.x cells, so golden's two guarded value-derefs (T3) now render. Golden's T3 = 2
            // keeps the file inside the union domain: DOMAIN_DRR7 UNMOVED at 475.
            // the UnderlierProductIdentifier row (fork=[4, 0, 10] golden=[4, 0, 20]) LEFT this list at seat 32: law A.2 (wrapperItemReceiverBind)
            // healed it WHOLE in all four drr 7.x cells after this suite's pins were measured; transcribed from
            // the checkpoint-2 full-gensuite print (ckpt2-gensuite.log).
            // the IndicatorOfTheUnderlyingIndexRule row (fork=[2, 0, 3] golden=[3, 0, 4]) LEFT this list at seat 32: law D.2
            // (extractBodyMultiDefaultTernary, on law D.1's left deref) healed it WHOLE in all four drr 7.x cells;
            // transcribed from this control's own print (D2-trip1.log).
            // GetUnderlierLEIForCredit.java (was fork=[0, 0, 0] golden=[1, 0, 1]) left this list at
            // seat 31: law 2 rung 2 (lambdaItemReceiverType - the then-piped whole-output ADD terminal
            // derefs element-wise before .getMulti() when the terminal-meta walk and the body's inferred
            // type disagree wrapper-vs-bare) healed it WHOLE in all four drr 7.x cells. Golden's tuple
            // stays non-zero, so the file remains inside the union domain (DOMAIN_DRR7 UNMOVED at 475).
            // the UnderlyingIndexIndicatorRule row (fork=[2, 1, 4] golden=[2, 0, 5]) LEFT this list at seat 32: law D.1 (defaultJoinDerefAtCollapsedLeft)
            // healed it WHOLE in all four drr 7.x cells; transcribed from this control's own print (D1-trip1.log).
            // the Price row (fork=[5, 1, 17] golden=[8, 1, 20]) LEFT this list: law C.2 (heteroMetaDefaultJoinDeref + iteArmMultiDefaultTernary)
            // took the rung-1 default join, the last residue after C.1 - Price is WHOLE in all four drr 7.x cells;
            // transcribed from this control's own print (C2-trip1.log), a pure row removal.
            // LAW 81 re-pin (seat 33, law C.1): fork=[0, 0, 10] -> fork=[5, 1, 17] - the statement ladder moved this scan's fork side toward golden; the file stays BANDED on C.2's default join; from C1-trip1.log.
            // the QuantityUnitOfMeasure row (fork=[1, 4, 3] golden=[0, 3, 2]) LEFT this list: law B.24 (defaultJoinHeteroMetaDerefBoth +
            // iteArmMetaCollapseDerefSinkChannel + three in-seat rungs) healed the file WHOLE in all four drr 7.x
            // cells - the band's last four files; transcribed from this control's own print (B24-trip1.log),
            // a pure row removal (was == expected minus it).
            // LAW 81 re-pin (seat 33, law C.2): fork=[0, 3, 1] -> fork=[1, 4, 3] - R3a's join deref moved this scan's fork side (LAW 80 IMPROVED-not-whole, planned; QUOM closes at B.24); from C2-trip1.log.
            // LAW 81 re-pin (seat 33, law B.3): fork=[0, 0, 1] -> fork=[0, 3, 1] - the bare-rule invocations + their guarded arg derefs moved this scan's fork side; the file stays BANDED (B.24); from B3-trip1.log.
            // the TotalNotionalQuantity row (fork=[1, 6, 5] golden=[1, 0, 12]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit,
            // seven rungs) healed the file WHOLE in all four drr 7.x cells; transcribed from this control's own
            // print (D3-trip1.log), a pure row removal (was == expected minus it).
            );
            // the UnderlyingAssetTradingPlatformIdentifierLeg1/Leg2 rows LEFT this list at seat 32: law A.1
            // (choiceOptionProjectionTypeId) healed both WHOLE in all four drr 7.x cells; transcribed from this
            // control's own print (A1-trip1.log).

    ///PIN: MEASURED EMPTY at the law's head, and the empty HELD in every leg the seat ran —
    ///PIN: the cross-corpus no-entering claim, measured. A NON-EMPTY result here is the ENTERING
    ///PIN: signal the mid-seat whole-matrix checkpoint exists to catch: do not pin it away,
    ///PIN: re-cut the law. (m-law6-nomulti makes it 36 files — that is the instrument proving
    ///PIN: it can fail, so the empty is a verdict rather than a blind spot.)
    private static final List<String> KNOWN_RESIDUE_CDM6206 = List.of();

    ///PIN: MEASURED at the first corpus run (LAW 73), and CONFIRMED at 71a92e826 (chain run 2:
    ///PIN: 11/0F both routes, so the residue assert passed and execution REACHED and passed the
    ///PIN: domain assert). THE FALSIFIER is that domain assert failing on a later run — NOT the
    ///PIN: harness's "MEASURED domain=" print, which only surfaces when the value is the -1
    ///PIN: sentinel and is therefore never emitted by a green run. Read it on a run that is
    ///PIN: already RED at the residue assert, where the domain assert is unreachable.
    private static final int DOMAIN_DRR7 = 475;

    ///PIN: MEASURED at the first corpus run (LAW 73). Same falsifier as DOMAIN_DRR7.
    private static final int DOMAIN_CDM6206 = 145;

    /**
     * (T1, T2, T3) per file: element-wise {@code "Type coercion"} hops that are NOT null-guarded
     * (the MapperC form this law adds), wrapper-typed single hoist locals (the form it removes),
     * and the file's TOTAL {@code "Type coercion"} count (the over-fire net — deliberately
     * global: the deref population must not move by one occurrence anywhere in the cell).
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            int[] t = new int[3];
            for (String line : normalize(e.getValue()).split("\n")) {
                String s = line.trim();
                int from = 0;
                while ((from = s.indexOf("\"Type coercion\"", from)) >= 0) {
                    t[2]++;
                    String tail = s.substring(from,
                            Math.min(s.length(), from + 200));
                    if (!tail.contains("== null ? null :")) {
                        t[0]++;
                    }
                    from += "\"Type coercion\"".length();
                }
                if (s.startsWith("final ") && s.contains("WithMeta") && s.endsWith(".get();")) {
                    t[1]++;
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
        // The domain-pin flow (LAW 73): the sentinel failure PRINTS the measured values so
        // the pin is transcribed from this assert's own output, never invented.
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
            RModel main = AstBuilder.buildFromString(MODEL, "seat30law6.rosetta");
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
            fixtureOut = render(m -> "census.seat30law6".equals(m.namespace()));
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
            throw new AssertionError("[CondArmMultiMetaElementDerefSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
