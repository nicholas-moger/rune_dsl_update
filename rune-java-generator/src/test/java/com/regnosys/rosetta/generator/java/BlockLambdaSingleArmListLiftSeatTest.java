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
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;

/**
 * SEAT 17 — facets {@code singleWrapArmSingletonLift} + {@code effElseMultiArmListLift}:
 * <b>a SINGLE arm at a MULTI block-lambda seat coerces item&rarr;list, and it does so in golden's
 * form.</b>
 *
 * <p><b>The law, stated as it was MEASURED.</b> At a MapperC-returning block-lambda seat a SINGLE
 * arm must be coerced item&rarr;list, and the form is
 * {@code MapperC.of(Collections.singletonList(<inner>))}. The fork wraps a raw value (a literal, a
 * bare {@code evaluate()} result) {@code MapperS.of(…)} on the way to the return seat, and then
 * either wraps the wrap (rung 1) or does not wrap at all (rung 2).
 *
 * <p><b>What is NOT claimed (the #589 review's REFUTATION 4).</b> An earlier revision derived the
 * form from a law — "a raw value has no Mapper to wrap, so golden lists it". <b>That law is
 * false:</b> golden writes the WITNESSED {@code MapperC.<T>of(MapperS.of(<raw evaluate()>))} in
 * thousands of places (cdm 5.38.0 {@code Create_AdjustmentPrimitiveInstruction:105} among them), so
 * raw-vs-Mapper is not the discriminator. The true, narrower statement is the one the scan
 * measured: the UN-witnessed {@code MapperC.of(<exact single wrap>)} appears in no golden anywhere,
 * and the witnessed form is a different emitter this seat never produces (the gate short-circuits
 * on any {@code MapperC.}-prefixed render).
 *
 * <p>This is the same law {@code ControlFlowHandler.wrapDeepThenIteArm} already applies (facet
 * {@code nullTypedIteLiteralArmLift}, seat 16 / PR #588) — <b>LAW 69</b>, the two-halves-agree law.
 * The two halves are near-twins, not twins, and the difference is recorded rather than glossed
 * (the #589 review's REFUTATION 3): seat 16's predicate is TYPE-first
 * ({@code isMapperS(getExpressionType())}) with the render identity only as its null-type
 * fallback; this seat has no type leg at all, so it is strictly narrower.
 *
 * <p><b>There are FIVE block renderers at a MapperC seat, not two</b> (the #589 review's
 * REFUTATION 2, count re-corrected at Copilot R6 — the first correction said FOUR while listing
 * five). TWO coerced before this seat and THREE do after it: the ladder
 * ({@code compileLadderConditionalBlock}) and the nested tree
 * ({@code compileNestedConditionalBlock}) coerced already; this seat now does; and
 * {@code compileElselessConditionalBlock} plus {@code appendCascadeArmReturn} still do NOT — both
 * pre-existing, both with no measured carrier here, both banked as seat 18 rather than fixed on no
 * evidence. Neither is visible to {@code corpus_control1}, whose token is
 * {@code MapperC.of(MapperS.of(} — a shape the un-coerced seats never emit.
 *
 * <p><b>Rung 1 — {@code singleWrapArmSingletonLift}</b> ({@code CollectionHandler
 * .wrapSingleArmMapperCOf}, the helper shared by the LADDER seat's rung arms + terminal and by the
 * NESTED-TREE seat). It emitted {@code MapperC.of(<armReturn>)} unconditionally; when
 * {@code armReturn} is EXACTLY a single {@code MapperS.of(<inner>)} wrap the result was
 * {@code MapperC.of(MapperS.of(<raw>))}, which golden never writes. Carrier: drr
 * {@code common/trade/index IndexFactorRule} (5 diff-lines, 4 cells).
 *
 * <p><b>Rung 2 — {@code effElseMultiArmListLift}</b> ({@code
 * CollectionHandler.compileEffectiveElseConditionalBlock}). Its MapperC overload uses its
 * {@code multi} flag for the bare-enum / bare-fn / ctor wrap choices but <b>never called the
 * coercion helper at all</b>, so a single arm returned verbatim. Carrier: drr cftc
 * {@code BlockTradeElectionIndicatorRule} (3 diff-lines, 4 cells).
 *
 * <p><b>PRODUCERS CONFIRMED BY RUNTIME PROBE (LAW 72), and the blast radius MEASURED, not
 * predicted.</b> Instrumenting both sites and generating drr 7.0.0 (target/seat17-probe2.log):
 * <ul>
 *   <li>{@code wrapSingleArmMapperCOf}: 69 sites reached, 6 fire ({@code armMulti=false}), and
 *       <b>exactly TWO pass the gate</b> — both {@code IndexFactorRule}'s.</li>
 *   <li>{@code compileEffectiveElseConditionalBlock}: <b>three</b> sites reach {@code multi=true};
 *       exactly ONE has the identity ({@code elseNode=RBooleanLiteral ELSE=MapperS.of(false)}) —
 *       the carrier. <b>50 sites at {@code multi=false} DO carry the identity and must not
 *       move</b>; they are excluded because {@code multi} gates the whole block (the #588 MF-5
 *       ordering law).</li>
 * </ul>
 *
 * <p><b>WHY THE UNWRAP GATE, not the prefix reason an earlier revision gave</b> (the #589 review's
 * REFUTATION 1). That revision said an exact test was needed because a CHAINED arm "also begins
 * {@code MapperS.of(}", citing
 * {@code MapperS.of(underlierForProduct.evaluate(item.get())).<ReferenceWithMetaObservable>map(…)}
 * from the probe log. But that line reads <b>{@code unwrap=false}</b> — it is declined by the
 * unwrap check before any render test runs, and a {@code startsWith} test would decline it
 * identically. Across all 69 sites in the cell there is <b>not one</b> with {@code unwrap=true} and
 * a failing render test, so equals-vs-prefix separates nothing measurable. What protects the
 * 192-occurrence chained population is STRUCTURAL and cell-independent: only FOUR factories set
 * {@code unwrapToBuilder}, each by passing {@code Optional.of(inner)} to a
 * {@code JavaExpression} constructor: {@code wrappedInMapperSOf(JavaExpression, JavaType)}
 * (renders {@code MapperS.of(}), the {@code MapperCOfSingleWrap} ctor ({@code MapperC.<}), that
 * class's {@code witnesslessForm()} ({@code MapperC.of(}) and
 * {@code selfUnwrapping(JavaExpression)} (its inner VERBATIM) — all four in
 * {@code JavaExpression}. The two MapperC forms are excluded twice over, and
 * {@code selfUnwrapping} is the only
 * shape that could carry an arbitrary prefix — so requiring unwrap present AND a render that is
 * exactly one balanced {@code MapperS.of(…)} admits a {@code wrappedInMapperSOf} wrap and nothing
 * else.
 *
 * <p><b>What protects the two non-carrier {@code multi=true} sites</b> (the #589 review's MF-2),
 * which matters because their files are byte-identical GREEN. Not "already MULTI, identity false":
 * their compiled type is {@code Mapper<? extends String>}, which {@code JavaTypeUtil}'s exact
 * generic comparison calls neither MapperC nor MapperS, so the helper falls through to
 * {@code chainProvesMulti}. It is that predicate that keeps them bare — and what attests it is the
 * full-matrix receipt: zero rows dropped {@code identical} on either route.
 *
 * <p><b>THE TWO RUNGS ARE NOT EQUALLY SAFE, AND THIS SUITE DOES NOT PRETEND OTHERWISE.</b>
 * <ul>
 *   <li><b>Rung 2 is green-safe by construction, settled on a COMPILER (LAW 74)</b>.
 *       {@code MapperS.mapSingleToList} is {@code <F> MapperC<F> mapSingleToList(
 *       Function<MapperS<T>, MapperC<F>>)}, so a {@code MapperS} return from its lambda cannot
 *       compile. javac against the shipped {@code rune-runtime}:
 *       <pre>
 *   PRE : return MapperS.of(false);   from a mapSingleToList lambda
 *         error: incompatible types: cannot infer type-variable(s) F
 *           (argument mismatch; bad return type in lambda expression
 *            no instance(s) of type variable(s) T#2 exist so that MapperS&lt;T#2&gt; conforms to MapperC&lt;F&gt;)
 *   POST: return MapperC.of(Collections.singletonList(false));   compiles
 *       </pre>
 *       Four previously non-compiling files repaired.</li>
 *   <li><b>Rung 1 is NOT.</b> {@code MapperC.of(MapperBuilder<? extends T>…)} ({@code MapperC:48})
 *       accepts a {@code MapperS}, so {@code MapperC.of(MapperS.of(x))} <b>compiles</b> — verified
 *       by javac both PRE and POST. Its safety had to be MEASURED, and it was, over the FULL
 *       population: a paren-balanced, literal-aware scan of all <b>174,141</b> goldens found
 *       <b>ZERO</b> carrying {@code MapperC.of(<arg>)} where the arg is an exact single
 *       {@code MapperS.of(…)} wrap, and <b>192</b> occurrences of the CHAINED form it leaves
 *       alone.</li>
 * </ul>
 *
 * <p><b>And rung 1 is not "pure byte" in BEHAVIOUR</b> (the #589 review's MF-4), though it is
 * byte-directed. The two {@code MapperC.of} overloads differ: {@code of(MapperBuilder…)} COPIES the
 * source {@code MapperItem}s (path seed, parent and error flag preserved) while {@code of(List)}
 * builds FRESH root items seeded on {@code ele.getClass()}. For a {@code RosettaModelObject} inner
 * the reported {@code MapperPath} root therefore changes from the model type to the runtime impl
 * class. Golden emits the listed form, so the change is parity-correct — but "pure byte" would
 * have understated it, so it is recorded.
 *
 * <p><b>Fixture truth (the #588 lesson).</b> These fixtures were PROBED before a single assertion
 * was written — a throwaway test rendered them and the expectations were read off the output. They
 * reproduce both carrier shapes exactly: {@code mapSingleToList(inn -> { … return MapperS.of(new
 * BigDecimal("0.0")); })} for rung 2 and {@code return MapperC.of(MapperS.of(scale.evaluate(…)));}
 * for rung 1. {@code b4} and {@code b5} were added at #589 to reach the two things the first
 * revision could not see: an arm whose RENDER was rewritten under its expression, and the
 * NESTED-TREE seat (the helper's third caller).
 */
class BlockLambdaSingleArmListLiftSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            // THIS repo's own builtins, LAST so the roots above keep their precedence — a checkout
            // without the external corpus RUNS these tests instead of skipping them (#586: a test
            // that skips is not a lock).
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    /**
     * The fixtures mirror the CORPUS shapes (the #583 fixture-truth law). {@code IndexFactor} is
     * {@code extract <3-rung ladder>} whose first arm is a MULTI nav and whose later arms are
     * function calls; {@code BlockTradeElectionIndicator} is {@code then extract <if MULTI-chain
     * else literal>}. A fixture whose conditional sat at the rule's top level would exercise a
     * different seat entirely.
     */
    private static final String MODEL_DEP = """
            namespace census.seat17.dep
            version "1.0.0"

            type Leaf: <"the MULTI arm's element">
                amt number (0..1)

            type Holder:
                leaves Leaf (0..*)
                flag boolean (0..1)

            type Inp: <"the rule input">
                holder Holder (0..1)

            func Scale: <"a call whose result is a RAW value the renderer wraps in MapperS.of(...)">
                inputs:
                    v number (1..1)
                output:
                    result number (1..1)
                set result: v

            func PickLeaf: <"a call whose result is NAVIGATED - the chained arm">
                inputs:
                    v number (1..1)
                output:
                    result Leaf (1..1)
                set result -> amt: v
            """;

    private static final String MODEL_MAIN = """
            namespace census.seat17
            version "1.0.0"

            import census.seat17.dep.*

            reporting rule EffElseMultiArm from Inp: <"a1/a2 - RUNG 2: effective-else at a MapperC seat">
                extract inn [
                    if inn -> holder -> flag exists
                    then inn -> holder -> leaves -> amt
                    else 0.0
                ]

            reporting rule LadderMultiArm from Inp: <"a3/a4 - RUNG 1: a ladder at a MapperC seat">
                extract inn [
                    if inn -> holder -> flag exists
                    then inn -> holder -> leaves -> amt
                    else if inn -> holder exists
                    then Scale(1.0)
                    else Scale(2.0)
                ]

            reporting rule LadderChainedArm from Inp: <"b2 - the CHAINED arm the identity must decline">
                extract inn [
                    if inn -> holder -> flag exists
                    then inn -> holder -> leaves -> amt
                    else PickLeaf(1.0) -> amt
                ]

            reporting rule EffElseSingleArm from Inp: <"b1 - the SINGLE seat: no coercion exists to make">
                extract inn [
                    if inn -> holder -> flag exists
                    then inn -> holder -> flag
                    else False
                ]

            reporting rule EffElseIntLiteralArm from Inp: <"b4 - the arm whose RENDER was rewritten under the expression">
                extract inn [
                    if inn -> holder -> flag exists
                    then inn -> holder -> leaves -> amt
                    else 1
                ]

            reporting rule EffElseEmptyArm from Inp: <"b6 - the EMPTY arm must take the seat's cardinality too">
                extract inn [
                    if inn -> holder -> flag exists
                    then inn -> holder -> leaves -> amt
                    else empty
                ]

            reporting rule EffElseCtorArm from Inp: <"b8 - a DIRECT constructor arm at the MULTI seat">
                extract inn [
                    if inn -> holder -> flag exists
                    then inn -> holder -> leaves
                    else Leaf { amt: 1.0 }
                ]

            reporting rule NestedTreeMultiArm from Inp: <"b5 - the NESTED-TREE seat, the helper's third caller">
                extract inn [
                    if inn -> holder -> flag exists
                    then if inn -> holder exists
                        then inn -> holder -> leaves -> amt
                        else Scale(3.0)
                    else Scale(4.0)
                ]
            """;

    // ---- the tokens ---------------------------------------------------------
    private static final String EFF_LIFT =
            "return MapperC.of(Collections.singletonList(new BigDecimal(\"0.0\")));";
    private static final String EFF_PRE = "return MapperS.of(new BigDecimal(\"0.0\"));";
    private static final String LADDER_LIFT_1 =
            "return MapperC.of(Collections.singletonList(scale.evaluate(new BigDecimal(\"1.0\"))));";
    private static final String LADDER_LIFT_2 =
            "return MapperC.of(Collections.singletonList(scale.evaluate(new BigDecimal(\"2.0\"))));";
    private static final String DOUBLE_WRAP = "MapperC.of(MapperS.of(";

    // =========================================================================
    // Part A — the carriers (failing-first)
    // =========================================================================

    /**
     * a1 — RUNG 2. The effective-else block at the MapperC seat lifts its raw literal arm to a
     * singleton list. Failing-first: at the pre-seat emitter this is
     * {@code return MapperS.of(new BigDecimal("0.0"));} from a {@code mapSingleToList} lambda,
     * which does not compile (see the class javadoc's javac probe).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_effectiveElseBlockLiftsItsRawArmAtTheMultiSeat() throws IOException {
        String out = filtered("EffElseMultiArmRule.java");
        // anti-vacuity: the fixture really does render an effective-else block at a MapperC seat
        assertContains(out, ".mapSingleToList(inn -> {");
        assertContains(out, EFF_LIFT);
        assertNotContains(out, EFF_PRE);
    }

    /** a2 — the rung-2 lift registers {@code java.util.Collections}, exactly as golden does. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_theEffectiveElseLiftRegistersTheCollectionsImport() throws IOException {
        assertContains(filtered("EffElseMultiArmRule.java"), "import java.util.Collections;");
    }

    /**
     * a3 — RUNG 1. The ladder's single-wrap arms take golden's {@code singletonList} FORM, not the
     * double wrap. Failing-first: at the pre-seat emitter both render
     * {@code MapperC.of(MapperS.of(scale.evaluate(…)))}.
     *
     * <p>Both arms are asserted, not one: {@code wrapSingleArmMapperCOf} is called from the rung
     * seat AND the terminal seat — both inside {@code CollectionHandler.renderLadderLevel} — so a
     * fix applied to only one of them would leave this half-red. (Named by LANDMARK, not line
     * number: the #589 R5 finding was that the numbers this javadoc first carried had already
     * gone stale within the same PR.)
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_ladderSingleWrapArmsTakeTheSingletonListForm() throws IOException {
        String out = filtered("LadderMultiArmRule.java");
        // anti-vacuity: the fixture really does render a LADDER at a MapperC seat
        assertContains(out, ".mapSingleToList(inn -> {");
        assertContains(out, LADDER_LIFT_1);
        assertContains(out, LADDER_LIFT_2);
        assertNotContains(out, DOUBLE_WRAP);
    }

    /** a4 — the rung-1 lift registers {@code java.util.Collections}. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_theLadderLiftRegistersTheCollectionsImport() throws IOException {
        assertContains(filtered("LadderMultiArmRule.java"), "import java.util.Collections;");
    }

    // =========================================================================
    // Part B — decline controls (each asserts its fixture REACHED the seat)
    // =========================================================================

    /**
     * b1 — the SINGLE seat keeps the bare {@code MapperS.of(…)}. The lift is a CARDINALITY
     * coercion: at a {@code mapSingleToItem} seat there is nothing to coerce, and 50 sites in the
     * drr 7.0.0 cell alone carry the identity at {@code multi=false}. They are excluded because
     * {@code multi} leads the conjunction.
     *
     * <p>The first assertion is the ANTI-VACUITY pin — without it a fixture that never rendered a
     * block at all would pass. Three controls in the seat-15 suite shipped mis-designed for
     * exactly this reason, and a fourth ({@code b5}) was later proven by mutation not to pin what
     * it claimed.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_theSingleSeatKeepsTheBareWrap() throws IOException {
        String out = filtered("EffElseSingleArmRule.java");
        assertContains(out, ".mapSingleToItem(inn -> {");
        assertContains(out, "return MapperS.of(false);");
        assertNotContains(out, "Collections.singletonList");
    }

    /**
     * b2 — THE CHAINED-ARM FORM PIN. A CHAINED arm — one that begins {@code MapperS.of(} but
     * continues into a nav — is already a real Mapper, so it takes the Mapper-level coercion
     * {@code MapperC.of(<chain>)} and must NOT be listed. The population scan found <b>192</b>
     * live occurrences of that form across the goldens, so listing it would be a mass regression.
     *
     * <p><b>WHAT THIS DOES AND DOES NOT PIN — corrected after the #589 review, which refuted the
     * earlier claim.</b> An earlier revision said this was "RED-CAPABLE BY MUTATION: replacing the
     * seat's {@code equals} with {@code startsWith} fails this test and only this test."
     * <b>False.</b> This fixture's arm is a {@code NavigationHandler} {@code .map()} chain built
     * through {@code JavaExpression.from(...)}, which leaves {@code unwrapToBuilder} EMPTY — so it
     * is declined by the gate's unwrap check before any render test runs, and a prefix test would
     * decline it identically. The seat's own probe log says the same thing about the corpus:
     * across all 69 helper sites in drr 7.0.0 there is <b>not one</b> with {@code unwrap=true} and
     * a failing render test, so equals-vs-prefix separates nothing measurable.
     *
     * <p>What this test really pins is the UNWRAP leg: an arm with no wrap factory behind it must
     * still be coerced, and coerced as a Mapper rather than listed. That is worth a lock — it is
     * the 192-occurrence population — but it is not an identity discriminator, and the safety of
     * the identity rests on the STRUCTURAL enumeration in this class's javadoc instead.
     *
     * <p>It is a rung-2 fixture (a single {@code else}), so it is also RED at the pre-seat emitter
     * — where the chained arm gets no coercion at all. The decline controls proper are {@code b1}
     * and {@code b3}.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_aChainedArmTakesTheMapperFormAndIsNotListed() throws IOException {
        String out = filtered("LadderChainedArmRule.java");
        // anti-vacuity: the arm really is a MapperS.of-PREFIXED chain, i.e. it reaches the gate
        // and is declined there rather than by never arriving.
        assertContains(out,
                "return MapperC.of(MapperS.of(pickLeaf.evaluate(new BigDecimal(\"1.0\")))"
                + ".<BigDecimal>map(\"getAmt\", leaf -> leaf.getAmt()));");
        assertNotContains(out, "Collections.singletonList");
    }

    /**
     * b4 — THE RENDER-TRUTH PIN, and the reason the identity is read off {@code armReturn} rather
     * than off {@code unwrapToBuilder()}'s own render. Found by Copilot on #589 as a
     * <b>SUPPRESSED</b> comment — the third time in this programme that the suppressed block held
     * the real finding.
     *
     * <p>{@code compileEffectiveElseConditionalBlock}'s {@code fnNotionalTogetherRestructure}
     * numeric coercion (PR #398) REASSIGNS {@code elseStr} to
     * {@code MapperS.of(BigDecimal.valueOf(N))} for an int-literal arm at a BigDecimal-joined
     * conditional, and does <b>not</b> update the {@code JavaExpression}. So the expression's own
     * unwrapped inner still renders {@code 1} while the emitted arm reads
     * {@code MapperS.of(BigDecimal.valueOf(1))}. A gate comparing against
     * {@code render(unwrapToBuilder())} fails here and emits
     * {@code MapperC.of(MapperS.of(BigDecimal.valueOf(1)))} — precisely the form this seat exists
     * to remove.
     *
     * <p>RED against that form of the gate; green against the shipped one.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_anArmWhoseRenderWasRewrittenUnderTheExpressionIsStillLifted() throws IOException {
        String out = filtered("EffElseIntLiteralArmRule.java");
        // anti-vacuity: the numeric coercion really did fire (a bare `1` would render MapperS.of(1))
        assertContains(out, "BigDecimal.valueOf(1)");
        assertContains(out,
                "return MapperC.of(Collections.singletonList(BigDecimal.valueOf(1)));");
        assertNotContains(out, DOUBLE_WRAP);
    }

    /**
     * b5 — THE NESTED-TREE SEAT, the helper's THIRD caller (Copilot #589, also a SUPPRESSED
     * comment). {@code wrapSingleArmMapperCOf} is shared by the ladder rung/terminal seats, the
     * nested-tree seat ({@code appendNestedArm}) and — as of this PR — the effective-else seat.
     * {@code a1}/{@code a3} reach only the last two, so the exact-wrap branch could have regressed
     * or become unreachable for a nested-tree arm without this suite noticing.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b5_theNestedTreeSeatTakesTheSameLiftedForm() throws IOException {
        String out = filtered("NestedTreeMultiArmRule.java");
        // anti-vacuity: the fixture really does render the recursive nested-tree block
        assertContains(out, ".mapSingleToList(inn -> {");
        assertContains(out,
                "return MapperC.of(Collections.singletonList(scale.evaluate(new BigDecimal(\"3.0\"))));");
        assertContains(out,
                "return MapperC.of(Collections.singletonList(scale.evaluate(new BigDecimal(\"4.0\"))));");
        assertNotContains(out, DOUBLE_WRAP);
    }

    /**
     * b3 — the already-MULTI arm in the SAME block stays bare. The coercion is item&rarr;list, so
     * an arm whose value is already a {@code MapperC} must not be wrapped a second time (the
     * {@code armMulti} short-circuit, and the reason the two non-carrier effective-else sites in
     * the probe are correctly untouched).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_theAlreadyMultiArmStaysBare() throws IOException {
        String out = filtered("EffElseMultiArmRule.java");
        assertContains(out,
                "return inn.<Holder>map(\"getHolder\", inp -> inp.getHolder())"
                + ".<Leaf>mapC(\"getLeaves\", holder -> holder.getLeaves())"
                + ".<BigDecimal>map(\"getAmt\", leaf -> leaf.getAmt());");
    }

    /**
     * b6 — THE EMPTY ARM TAKES THE SEAT'S CARDINALITY TOO. At a MapperC seat
     * {@code MapperS.<T>ofNull()} is the SAME non-compiling shape as the un-lifted literal:
     * {@code mapSingleToList} wants {@code MapperC<F>}. Both sibling renderers already emit the
     * MapperC form ({@code appendTypedEmptyReturn}, and the ladder's {@code typedEmptyReturn}), so
     * this seat was the odd one out.
     *
     * <p><b>Raised three times before it was fixed</b> — by this seat's own deep self-review, by
     * the #589 independent review, and by Copilot R2 — and each of the first two BANKED it on "zero
     * corpus carriers, so there is nothing to verify a fix against". That reasoning was wrong: the
     * evidence needed was never the corpus. It was the sibling's own form plus a compiler.
     *
     * <p>javac against the shipped {@code rune-runtime}, same method as LAW 74:
     * <pre>
     *   PRE : return MapperS.&lt;BigDecimal&gt;ofNull();   from a mapSingleToList lambda   DOES NOT COMPILE
     *   POST: return MapperC.&lt;BigDecimal&gt;ofNull();                                    compiles
     * </pre>
     * Zero bytes move in the 25-cell matrix (no {@code multi=true} site in the probe has an empty
     * arm), which is why this is locked by a fixture rather than by a corpus heal.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b6_theEmptyArmTakesTheSeatsCardinality() throws IOException {
        String out = filtered("EffElseEmptyArmRule.java");
        // anti-vacuity: the fixture really does render the block at a MapperC seat
        assertContains(out, ".mapSingleToList(inn -> {");
        assertContains(out, "return MapperC.<BigDecimal>ofNull();");
        assertNotContains(out, "MapperS.<BigDecimal>ofNull()");
    }

    /**
     * b8 — THE CONSTRUCTOR ARM AT THE MULTI SEAT TAKES THE SEAT'S OWN LIFT
     * (Copilot #589 R7 asked for the coverage; R8's NEW inline comment — its first since R1 —
     * asked for the fix the coverage exposed, and both were right).
     *
     * <p>R6 added {@code isCtorArm}'s terminal-constructor leg and locked it only with
     * {@code corpus_control3}, a contract with zero carriers, so deleting either branch left the
     * suite green (R7). A probe at the seat settled which shapes reach it — a DIRECT constructor
     * arm does: {@code then=RFeatureCall else=RConstructorExpr}, {@code elseWrap=false},
     * {@code elseEmpty=false}. This fixture is that shape.
     *
     * <p>Rendering it is what ended #589 R3's banked guess. The no-op emitted
     * {@code return Leaf.builder()….build();} from a {@code mapSingleToList} lambda — whose
     * parameter is {@code Function<MapperS<T>, MapperC<F>>} — so it did not compile, and neither
     * would the {@code MapperC.of(<POJO>)} the coercion helper's fall-through would have produced.
     * Exactly one candidate compiles and golden writes it: see {@link CollectionHandler#isCtorArm}
     * for the overload argument (LAW 74) and for the sibling's byte-equal golden pin.
     *
     * <p>RED-CAPABLE BY MUTATION, both directions, each verified by running it. Reverting the
     * caller to the R6 skip leaves the bare {@code return Leaf.builder()…} and fails the LIFT
     * assertion below; deleting {@code arm instanceof RConstructorExpr} routes the arm to
     * {@code wrapSingleArmMapperCOf}, whose fall-through emits {@code MapperC.of(Leaf.builder()…)}
     * and fails the same one. ⚠️ Mutate the <b>ELSE</b> caller: this fixture's constructor is its
     * else arm, so mutating the then caller leaves the suite GREEN and reads as a vacuous test.
     * That mistake was made and caught here — a mutation aimed at the wrong arm proves nothing.
     *
     * <p>The OTHER leg — a drained then-chain terminal constructor — still has no fixture at this
     * seat, and the reason is measured, not assumed: written here it never arrives, because the
     * then-in-subtree hoist intercepts it one level up and emits a {@code thenArg} local. In the
     * corpus the arm KIND does reach ({@code else=RThenExpr}, twice in drr 7.0.0) but only with
     * {@code __COERCION_PARAM_NNNN__} renders and no constructor terminal. A recorded gap, stated
     * rather than dressed up as a lock (Copilot #589 R8, suppressed).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b8_theDirectConstructorArmTakesTheSeatsListLift() throws IOException {
        String out = filtered("EffElseCtorArmRule.java");
        // anti-vacuity: the fixture really does render the block at the MULTI (MapperC) seat
        assertContains(out, ".mapSingleToList(inn -> {");
        // the lift: golden's form, and the only candidate that compiles
        assertContains(out, "return MapperC.of(Collections.singletonList(Leaf.builder()");
        // neither of the two forms that do NOT compile survives
        assertNotContains(out, "MapperC.of(Leaf.builder()");
        assertNotContains(out, "return Leaf.builder()");
        // the lift earns its own imports
        assertContains(out, "import com.rosetta.model.lib.mapper.MapperC;");
        assertContains(out, "import java.util.Collections;");
    }

    /**
     * b7 — COPILOT R2's STALE-IMPORT CONCERN, SETTLED BY MEASUREMENT RATHER THAN BANKED.
     * {@code JavaExpression.wrappedInMapperSOf} contributes {@code MAPPER_S} to the arm refs that
     * {@code compileEffectiveElseConditionalBlock} merges unconditionally, so when this seat
     * rewrites the wrap away the ref survives — and Copilot asked for either a scoped-merge fix or
     * a lock proving no stale {@code import ...MapperS;} is emitted.
     *
     * <p>The lock is here, and it shows the exposure is <b>not observable at this seat</b>: a rule
     * reaching a MapperC block lambda is ALWAYS entered through {@code MapperS.of(input)…}
     * (or a {@code MapperS}-typed {@code thenArg}), so {@code MapperS} is used by construction in
     * every file this gate can touch. This test asserts exactly that — the lifted arm is present,
     * no {@code MapperS.of(<the lifted inner>)} survives, and the {@code MapperS} import is
     * genuinely EARNED by another use rather than left dangling.
     *
     * <p>So the ref is redundant, not wrong, on this path. It is still recorded as a latent class
     * issue for {@code ControlFlowHandler}'s twin (whose seat is a local decl, not a lambda body,
     * and therefore has no such structural guarantee) — see that arm's javadoc.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b7_theRewrittenWrapLeavesNoDanglingMapperSImport() throws IOException {
        String out = filtered("EffElseMultiArmRule.java");
        assertContains(out, EFF_LIFT);
        assertNotContains(out, EFF_PRE);
        // The javadoc's structural argument is a POSITIVE claim — a rule reaching this seat is
        // ALWAYS entered through `MapperS.of(input)`, so `MapperS` is used by construction — and
        // the ABSENCE of the import falsifies it. Guarding the earned-use assertion behind
        // `if (out.contains(<the import>))` made this test blind to exactly that falsification: a
        // render that dropped the import entirely passed silently (Copilot #589 R4, suppressed).
        // Assert the import is PRESENT first, then that it is earned.
        assertContains(out, "import com.rosetta.model.lib.mapper.MapperS;");
        // the import must be EARNED: strip the import line itself, then require a real use
        String body = out.replace("import com.rosetta.model.lib.mapper.MapperS;", "");
        assertTrue(body.contains("MapperS."),
                "a MapperS import survives with no MapperS use left in the body — the "
                + "discarded wrap's ref is now a dangling import (Copilot #589 R2). The "
                + "structural argument in this test's javadoc no longer holds; scope the "
                + "ref merge.");
    }

    // =========================================================================
    // Part C — corpus locks (drr 7.0.0) + the cross-cell identity
    // =========================================================================

    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN_DIR =
            DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_CELL_ROOT) && Files.isDirectory(DRR7_GOLDEN_DIR), BlockLambdaSingleArmListLiftSeatTest.class);
    }

    private static final String BLOCKTRADE =
            "drr/regulation/cftc/rewrite/trade/reports/BlockTradeElectionIndicatorRule.java";
    private static final String INDEXFACTOR =
            "drr/regulation/common/trade/index/reports/IndexFactorRule.java";
    private static final String JURIS1 =
            "drr/regulation/csa/rewrite/trade/reports/JurisdictionOfCounterparty1Rule.java";

    /** c1 — RUNG 2's whole-file heal. All 3 of its diff-lines were this seat's. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c1_blockTradeElectionIndicator() throws IOException { lock(BLOCKTRADE); }

    /** c2 — RUNG 1's whole-file heal. All 5 of its diff-lines were this seat's. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c2_indexFactor() throws IOException { lock(INDEXFACTOR); }

    /**
     * c3 — THE CROSS-CELL PRECONDITION. Golden-side AND fork-side: the three sibling cells carry a
     * byte-identical golden AND a byte-identical {@code .rosetta} source for both carriers, so the
     * fork compiles the same AST against the same expectation there.
     *
     * <p><b>What this does NOT do, corrected after Copilot #589 R2 (suppressed).</b> An earlier
     * revision called this "THE CROSS-CELL IDENTITY that makes pinning drr 7.0.0 pin all FOUR
     * cells" and "the ONLY thing extending a 2-file lock to the claimed 8". That over-claims: this
     * test never RUNS the generator for drr 7.1.0–7.3.0, so a workspace-context or dependency
     * difference could change the fork's output there while this still passes. It is a
     * PRECONDITION for the stand-in argument, not proof of it.
     *
     * <p>What actually attests the other six heals is the FULL-MATRIX receipt, which is measured
     * per cell: all four drr 7.x POJO rows moved 43 → 41, with zero rows dropping {@code identical}
     * on either route. That is a per-cell measurement of the real generator output; this test
     * guards the assumption it rests on and fails loudly if a sibling ever diverges.
     *
     * <p>An absent sibling FAILS rather than being skipped (the #588 MF-3 rule): all three are in
     * the frozen 9.83.0 manifest, so absence is a broken checkout.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c3_theSiblingCellsAreIdentical() throws IOException {
        int compared = 0;
        for (String rel : List.of(BLOCKTRADE, INDEXFACTOR)) {
            Path base = DRR7_GOLDEN_DIR.resolve(rel);
            assertTrue(Files.isRegularFile(base), "golden missing: " + base);
            String expected = normalize(Files.readString(base));
            for (String v : List.of("7.1.0", "7.2.0", "7.3.0")) {
                Path sib = Path.of("../test-corpus/drr/drr-" + v)
                        .resolve("rosetta-source/src/generated/java").resolve(rel);
                assertTrue(Files.isRegularFile(sib),
                        "sibling cell golden missing: " + sib + " — drr " + v + " is in the frozen"
                        + " 9.83.0 manifest, so this checkout is incomplete and drr 7.0.0 can no"
                        + " longer stand in for it.");
                assertEquals(expected, normalize(Files.readString(sib)),
                        "drr " + v + "'s golden for " + rel + " has diverged from drr 7.0.0's —"
                        + " this cell no longer stands in for it, so pin " + v + " directly.");
                compared++;
            }
        }
        assertEquals(6, compared, "expected 2 files x 3 sibling cells = 6 comparisons");

        for (String src : List.of("regulation-cftc-rewrite-trade-rule.rosetta",
                                  "regulation-common-trade-index-rule.rosetta")) {
            String src0 = normalize(Files.readString(Path.of("../test-corpus/drr/drr-7.0.0")
                    .resolve("rosetta-source/src/main/rosetta").resolve(src)));
            for (String v : List.of("7.1.0", "7.2.0", "7.3.0")) {
                Path p = Path.of("../test-corpus/drr/drr-" + v)
                        .resolve("rosetta-source/src/main/rosetta").resolve(src);
                assertTrue(Files.isRegularFile(p), "sibling cell source missing: " + p);
                assertEquals(src0, normalize(Files.readString(p)),
                        "drr " + v + "'s " + src + " has diverged from drr 7.0.0's — the fork no"
                        + " longer compiles the same AST there, so pin " + v + " directly.");
            }
        }
    }

    /**
     * control 1 — THE GATE'S CONTRACT, CHECKED OVER THE WHOLE CELL. Its domain is the MECHANISM's
     * reach, not the carrier's (LAW 72): every one of the ~6,900 files this cell generates, not
     * the two the seat heals.
     *
     * <p>Three assertions, and the first is the anti-vacuity pin without which the other two would
     * pass over an empty population:
     * <ol>
     *   <li>the fork still emits at least one CHAINED {@code MapperC.of(MapperS.of(…)<chain>)} —
     *       the 192-occurrence population the identity must leave alone is non-empty here;</li>
     *   <li>the set of files still emitting {@code MapperC.of(<exact single MapperS.of wrap>)} is
     *       EXACTLY the pinned inline-ternary set below — the SET, not a count, because the
     *       failure mode here is substitution (LAW 73);</li>
     *   <li>ZERO {@code Collections.singletonList(MapperS.of(} — the signature of a gate widened
     *       to a prefix test, which would list a chain instead of coercing it.</li>
     * </ol>
     *
     * <p><b>THE PINNED EXCLUSION, AND ITS OPERATIVE REASON.</b> Two sites survive, both in
     * {@code GetBasketConstituents}, and both inside {@code ControlFlowHandler.handle}'s INLINE
     * TERNARY ({@code <cond>.getOrDefault(false) ? <then> : <else>}) — a fourth site of the same
     * law, reached by facet {@code ternaryMixedArityArmLift} (PR #432), which lifts with a
     * {@code startsWith("MapperS.of(")} test. This seat deliberately does NOT widen there, and the
     * reason is measured, not stylistic:
     * <ul>
     *   <li>the inline ternary has <b>ZERO golden carriers</b> — {@code ControlFlowHandler}'s own
     *       zero-golden law — so no lift there can heal anything;</li>
     *   <li>golden's own form at these two sites is {@code return MapperS.of(financialUnitTo…);}
     *       at a SINGLE seat inside a block lambda ({@code GetBasketConstituents} golden :153 and
     *       :188), i.e. the fork's whole restructure there differs and the file carries 58
     *       diff-lines either way;</li>
     *   <li>so lifting them would move those bytes <b>away</b> from golden's token content for no
     *       parity movement — a change with no measured benefit and a plausible small harm.</li>
     * </ul>
     * Banked as a follow-up with that measurement attached, rather than silently excluded. If a
     * NEW exact-single-wrap site appears in any other file, this test fails and the exclusion has
     * to be re-argued.
     *
     * <p>An earlier draft of this control pinned the fork's chained-form COUNT against golden's in
     * one file ({@code NameOfTheUnderlyingIndexRule}) and was RED for a reason this seat does not
     * own — that file carries a pre-existing 6-line divergence in which golden emits an extra wrap
     * the fork never emits. A control that fails on someone else's defect measures nothing; it was
     * replaced rather than waived. Its successor is what found the ternary seat.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control1_theGatesContractHoldsOverTheWholeCell() {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        // FAIL-CLOSED (Copilot #589 R2, suppressed): this control's verdict is a statement about
        // the WHOLE cell, so a generation error that removed a file from the map would shrink the
        // population and leave the contract green over an incomplete scan.
        //
        // The cell was NOT error-free when this control was written, and pinning it to zero was
        // wrong THEN — the first attempt did exactly that and went RED: drr 7.x carried exactly
        // THREE DECLARED refusals (`TYPE_SWITCH_TERNARY_STUB`: the v3.1 C0 refusal contract
        // emitting nothing rather than a ternary that would compare against a type name — two in
        // `ingest-fpml-confirmation-pricequantity-func.rosetta` (:938, :960), one in
        // `ingest-fpml-recordkeeping-message-func.rosetta` (:25)), absent from the scan BY DESIGN,
        // and the guard pinned that SET by SITE — not a category token plus a count (Copilot #589
        // R4, suppressed; LAW 73), so a substituted or duplicated refusal could not pass.
        // Seat 23 (facets aliasSwitchBareCaseNav + pathedChoiceSwitchSet) HEALED all three: the
        // files are emitted, byte-identical to golden, and none carries the token this control
        // scans (measured: 0 `MapperC.of(MapperS.of(` in the three goldens), so they simply join
        // the population. The declared set is therefore EMPTY, and the pin says so in both
        // directions: ANY generation error in this cell now means a file is missing from the scan
        // for an unknown reason, and the contract below cannot be asserted over it.
        assertEquals(List.of(), drr7GenErrors,
                "drr 7.0.0's declared refusal SET moved (EMPTY since seat 23 healed the three "
                + "TYPE_SWITCH_TERNARY_STUB sites) — the scanned population has changed shape, so "
                + "re-establish what this control covers before trusting its verdict.");
        int chained = 0;
        int scanned = 0;
        List<String> exactSites = new ArrayList<>();
        for (Map.Entry<String, String> e : drr7Output.entrySet()) {
            String src = e.getValue();
            assertNotNull(src, "null output for " + e.getKey()
                    + " — the whole-cell scan cannot skip it silently.");
            scanned++;
            // Copilot #589 (SUPPRESSED): classify over a CODE-ONLY view, so a `MapperC.of(
            // MapperS.of(` inside a string literal or a comment cannot be counted as a real site.
            String code = codeOnly(src);
            for (int i = code.indexOf(DOUBLE_WRAP); i >= 0; i = code.indexOf(DOUBLE_WRAP, i + 1)) {
                int open = i + "MapperC.of".length();
                int close = matchParen(code, open);
                // NOT a `continue` (the #588 MF-3 rule): an unbalanced site would silently shrink
                // this control's domain, which is exactly the failure mode it exists to catch.
                assertTrue(close >= 0,
                        "unbalanced MapperC.of( in " + e.getKey() + " at offset " + i
                        + " — the scan cannot classify it, so this control's domain is not the"
                        + " whole cell and its verdict cannot be trusted.");
                String arg = code.substring(open + 1, close);
                if (matchParen(arg, "MapperS.of".length()) == arg.length() - 1) {
                    // Copilot #589 (SUPPRESSED): record the SITE, not just the file — otherwise a
                    // substitution WITHIN the pinned file leaves the same filename multiset and
                    // passes. LAW 73: pin the SET when the failure mode is substitution.
                    exactSites.add(e.getKey() + ":" + (countNewlines(code, i) + 1));
                } else {
                    chained++;
                }
            }
        }
        assertTrue(scanned > 6000,
                "only " + scanned + " files were scanned; this cell generates ~6,900, so the "
                + "population is not the whole cell and the verdict below means nothing.");
        assertTrue(chained > 0,
                "no CHAINED MapperC.of(MapperS.of(…)<chain>) survives anywhere in drr 7.0.0 — "
                + "either the gate has widened into the chained population or this control's "
                + "premise is gone; re-measure before believing the other assertion.");
        exactSites.sort(String::compareTo);
        assertEquals(TERNARY_EXCLUSION_SITES, exactSites,
                "the SET of SITES emitting MapperC.of(<exact single MapperS.of wrap>) has moved. "
                + "ZERO of the 174,141 goldens carry that form; the only sites this seat leaves "
                + "are the two inline-ternary ones in " + TERNARY_EXCLUSION + " (see this test's "
                + "javadoc for why). Anything else here is an unlifted block-lambda arm.");
    }

    private static final String TERNARY_EXCLUSION =
            "drr/base/trade/basket/functions/GetBasketConstituents.java";

    /**
     * The sites this seat deliberately left, pinned by SITE rather than by filename so a
     * substitution inside the same file fails (LAW 73 — the failure mode here is substitution).
     * EMPTY since seat 33 law A.3 (ladderBlockCtorNestedExtract): the two pinned sites WERE the two
     * inline ternaries that law block-converts, and the carrier was the only file left carrying the
     * form - so the whole cell now emits it ZERO times, exactly as golden does; the {@code chained > 0}
     * guard below still holds (the chained population is elsewhere in the cell). Transcribed from this
     * control print (A3-trip1.log: {@code expected: <[...:144, ...:167]> but was: <[]>}).
     */
    private static final List<String> TERNARY_EXCLUSION_SITES = List.of();
    // (re-pinned 143/166 -> 144/167 at seat 33: law A.4 (lolDefaultBodyMulti) adds the B001
    //  `import java.util.Collections;` line above both sites; the sites are byte-unchanged -
    //  transcribed from this control print, A4-trip1.log)
    // (re-pinned 140/163 -> 142/165 at seat 28: law 8's bare coercion added two lines
    //  ABOVE these sites in the same file - the file itself IMPROVED in place, the two
    //  inline-ternary sites are byte-unchanged, only their line numbers moved.
    //  re-pinned 142/165 -> 143/166 at seat 29: law 6's recovered Asset import adds ONE
    //  import line above both sites - same class of movement, sites byte-unchanged.)

    /**
     * A CODE-ONLY view of generated Java: string literals, char literals and both comment forms
     * blanked to spaces, with newlines preserved so line numbers still compute. A character walk,
     * not a pattern match — and it performs no structural analysis of the language content, only a
     * literal-token scan of the fork's own just-emitted output.
     *
     * <p>Why it is needed: an earlier revision scanned the raw text, so a `MapperC.of(MapperS.of(`
     * appearing inside a generated string literal or javadoc would have been classified as a real
     * emission site (Copilot #589, suppressed).
     */
    private static String codeOnly(String s) {
        char[] out = s.toCharArray();
        for (int i = 0; i < out.length; i++) {
            char c = out[i];
            if (c == '"' || c == '\'') {
                char quote = c;
                int j = i + 1;
                while (j < out.length && out[j] != quote) {
                    j += out[j] == '\\' ? 2 : 1;
                }
                for (int k = i; k <= Math.min(j, out.length - 1); k++) {
                    if (out[k] != '\n') {
                        out[k] = ' ';
                    }
                }
                i = j;
            } else if (c == '/' && i + 1 < out.length && out[i + 1] == '/') {
                int j = i;
                while (j < out.length && out[j] != '\n') {
                    out[j++] = ' ';
                }
                i = j;
            } else if (c == '/' && i + 1 < out.length && out[i + 1] == '*') {
                int j = i;
                while (j + 1 < out.length && !(out[j] == '*' && out[j + 1] == '/')) {
                    if (out[j] != '\n') {
                        out[j] = ' ';
                    }
                    j++;
                }
                if (j < out.length) {
                    out[j] = ' ';
                }
                if (j + 1 < out.length) {
                    out[j + 1] = ' ';
                }
                i = j + 1;
            }
        }
        return new String(out);
    }

    private static int countNewlines(String s, int end) {
        int n = 0;
        for (int i = 0; i < end; i++) {
            if (s.charAt(i) == '\n') {
                n++;
            }
        }
        return n;
    }

    /**
     * Index of the ')' matching the '(' at {@code open}, or -1. Operates on a {@link #codeOnly}
     * view, so no literal-skipping is needed here.
     */
    private static int matchParen(String s, int open) {
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')' && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * control 2 — seat 16's heal must not regress. {@code JurisdictionOfCounterparty1Rule} was
     * made byte-identical at #588 by the sibling arm this seat ports; it carries a
     * {@code Collections.singletonList} lift of its own, so a change to the lift's FORM would show
     * up here first.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control2_seat16sHealStaysHealed() throws IOException { lock(JURIS1); }

    /**
     * control 3 — THE NON-COMPILING FORM {@code isCtorArm}'s second leg EXISTS TO PREVENT
     * (Copilot #589 R6, a SUPPRESSED finding that was REAL).
     *
     * <p>{@code wrapSingleArmMapperCOf}'s {@code MapperS.of(…)} identity chooses WHICH wrap; it
     * never declines one. Anything the effective-else MapperC seat hands it that misses the
     * identity takes the UNCONDITIONAL fall-through {@code MapperC.of(<armReturn>)}. When
     * {@code armReturn} is a bare built constructor that is {@code MapperC.of(<POJO>)}, and
     * {@code MapperC.of} has exactly two overloads — {@code of(MapperBuilder<? extends T>...)}
     * and {@code of(List<? extends T>)} — so it does not compile (LAW 74).
     *
     * <p>THIS CONTROL HAS ZERO CARRIERS TODAY AND SAYS SO. It is a CONTRACT over the whole cell,
     * not a lock on the R6 fix: a runtime probe at the call site found three {@code RThenExpr}
     * arms reaching {@code isCtorArm} in drr 7.0.0 and none with a constructor terminal, so the
     * fix moves no bytes and no test can witness it here. What this control does do is fail the
     * moment any future change emits the form — which is the only guarantee available without a
     * carrier, and is stated rather than dressed up as a lock on this seat (the #589 R4 lesson: a
     * guard that cannot distinguish the fix from its absence must not claim to).
     *
     * <p>Red-capability was proven by MUTATION rather than shipped: relaxing the predicate below
     * from {@code .build()} to {@code )} turns the empty expectation into 49 sites.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control3_noMapperCOfWrapsABareBuiltConstructor() {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        int scanned = 0;
        List<String> sites = new ArrayList<>();
        for (Map.Entry<String, String> e : drr7Output.entrySet()) {
            String src = e.getValue();
            assertNotNull(src, "null output for " + e.getKey()
                    + " — the whole-cell scan cannot skip it silently.");
            scanned++;
            String code = codeOnly(src);
            for (int i = code.indexOf("MapperC.of("); i >= 0;
                    i = code.indexOf("MapperC.of(", i + 1)) {
                int open = i + "MapperC.of".length();
                int close = matchParen(code, open);
                // fail-closed, per the #588 MF-3 rule — an unbalanced site would silently shrink
                // this control's domain, the exact failure mode it exists to catch.
                assertTrue(close >= 0,
                        "unbalanced MapperC.of( in " + e.getKey() + " at offset " + i
                        + " — the scan cannot classify it, so this control's domain is not the"
                        + " whole cell and its verdict cannot be trusted.");
                if (code.substring(open + 1, close).endsWith(".build()")) {
                    sites.add(e.getKey() + ":" + (countNewlines(code, i) + 1));
                }
            }
        }
        assertTrue(scanned > 6000,
                "only " + scanned + " files were scanned; this cell generates ~6,900, so the "
                + "population is not the whole cell and the verdict below means nothing.");
        sites.sort(String::compareTo);
        assertEquals(List.of(), sites,
                "MapperC.of(<a bare built constructor>) was emitted. MapperC.of takes a "
                + "MapperBuilder varargs or a List, so a built POJO does not compile — this is "
                + "the fall-through isCtorArm's terminal-constructor leg exists to prevent.");
    }

    private static void lock(String path) throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        List<String> lockedErrors = drr7GenErrors.stream()
                .filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drr7Output.get(path);
        assertNotNull(generated, "not generated: " + path);
        Path goldenPath = DRR7_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 7.0.0 output must byte-match golden (newline-normalized) for "
                + path + " — seat 17: a SINGLE arm at a MULTI block-lambda seat coerces item->list"
                + " in golden's Collections.singletonList form.");
    }

    // =========================================================================
    // Harness
    // =========================================================================

    private static Map<String, String> drr7Output;
    private static List<String> drr7GenErrors = new ArrayList<>();

    @BeforeAll
    static void generateDrr7() throws IOException {
        if (drr7Available()) {
            drr7Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        var dataRuleGen = new DataRuleGenerator(gm, typeTranslator, typeUtil);
        var labelProviderGen = new LabelProviderGenerator(
                gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        Map<String, String> output = new LinkedHashMap<>();
        // #588 (Copilot) started this: generateWithErrors' per-function failures must not be
        // discarded, because dropping them lets a byte-lock pass while unrelated work in the same
        // cell failed to generate — green for the wrong reason.
        // #589 (Copilot) FINISHED it: `generateClasses` ALSO returns per-model errors, and every
        // one of the six calls below was discarding them, so the #588 fix covered only the
        // FunctionGenerator leg. All seven legs are aggregated now.
        drr7GenErrors = new ArrayList<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                collect(pojoGen.generateClasses(model, version, output));
                collect(choiceGen.generateClasses(model, version, output));
                collect(ruleGen.generateClasses(model, version, output));
                collect(reportGen.generateClasses(model, version, output));
                collect(dataRuleGen.generateClasses(model, version, output));
                collect(labelProviderGen.generateClasses(model, version, output));
            }
        }
        collect(funcGen.generateWithErrors(output));
        return output;
    }

    /** Aggregate one generator leg's reported errors; surfaced per locked file by {@link #lock}. */
    private static void collect(List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> drr7GenErrors.add(e.getTargetPath() + " — " + e));
        }
    }

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> filteredOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel dep = AstBuilder.buildFromString(MODEL_DEP, "seat17-dep.rosetta");
            RModel main = AstBuilder.buildFromString(MODEL_MAIN, "seat17.rosetta");
            dep.setVersion("0.0.0.test");
            main.setVersion("0.0.0.test");
            List<RModel> models = new ArrayList<>();
            models.add(dep);
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
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        if (!errors.isEmpty()) {
            throw new AssertionError("fixture generation errors (a broken fixture"
                    + " must fail loudly, not skip): " + errors);
        }
        return out;
    }

    private static String filtered(String fileName) throws IOException {
        if (filteredOut == null) {
            filteredOut = render(m -> "census.seat17".equals(m.namespace()));
        }
        return lookup(filteredOut, fileName);
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
            if (!Files.isDirectory(root)) continue;
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
                    try { models.add(AstBuilder.buildFromFile(p)); }
                    catch (Exception e) { failures.add(p + " — " + e); }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[BlockLambdaSingleArmListLiftSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static void assertContains(String out, String needle) {
        assertTrue(out.contains(needle),
                "expected needle missing:\n" + needle + "\n--- in output:\n" + out);
    }

    private static void assertNotContains(String out, String token) {
        assertTrue(!out.contains(token),
                "forbidden token present: " + token + "\n--- in output:\n" + out);
    }
}
