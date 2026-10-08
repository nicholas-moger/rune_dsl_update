package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.ref.Reference.reachabilityFence;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * v3.1 LADDER RETIREMENT — flip seat 8: the rule-output cardinality-form family
 * (the LAW-65 charter {@code target/seat8-charter.md}, written before any
 * emitter edit). Upstream renders {@code Function} and {@code RosettaRule}
 * through ONE {@code callableWithArgsCall} case ({@code ExpressionGenerator
 * .xtend:239-250}) — a with-args call to a MULTI-bodied rule is stamped
 * {@code List<X>} and coerced {@code MapperC.<X>of(…)} at any Mapper seat
 * ({@code TypeCoercionService.xtend:336-338}); and a rule's own output
 * cardinality is {@code cardinalityProvider.isMulti(body)}
 * ({@code RObjectFactory.java:106}) where {@code caseFeatureCall} ORs the
 * receiver — a multi HEAD makes {@code payout -> ChoiceOption} MULTI with no
 * separate choice-option channel. The fork's two declines: (Rung A) the
 * with-args callable branch's only multi consult is RFunction-scoped
 * ({@code ReferenceHandler:1469-1471} — null for a rule), so a with-args MULTI
 * rule call falls to the scalar {@code MapperS.of(…)} the no-args sibling
 * outgrew at #273/#274 ({@code :3861-3909}); (Rung B) the rule-output back-fill
 * ({@code reports/RuleGenerator.java:328}) consults the engine's
 * {@code getRuleBodyCardinality}, whose {@code disguisedChainCardinality} has
 * no {@code resolvedChoiceOption} arm ({@code CardinalityComputer:589} — the
 * channel-exclusive binding both other arms are guarded against), so the drr
 * 7.x {@code extract payout -> CommodityPayout} spine reads SINGLE and the
 * whole evaluate surface stays scalar.
 *
 * <p><b>Green-safety:</b> the Rung-A gen form NEVER COMPILED
 * ({@code MapperS.of(<List-typed evaluate>)} infers
 * {@code MapperS<List<? extends X>>}; {@code .getMulti()} yields
 * {@code List<List<…>>}, rejected by both {@code toBuilder} overloads; the
 * jfsa decl seat assigns {@code MapperS<List<X>>} to {@code MapperS<X>}).
 * 819 golden FILES carry the {@code MapperS.of(<rule>.evaluate(} scalar wrap
 * vs 78 the MapperC form (930/80 occurrences — the indep review's NIT-3 unit
 * correction) — the arm stays gated on the CALLEE body cardinality the fork
 * already computes correctly both ways (LAW-66-controlled scans in the
 * charter: risk pattern 0 golden hits, positive control 28). Rung B's overlay
 * is monotone add-only on the choice-option channel only — a green single-form
 * golden cannot carry a multi-head choice-option chain (upstream ORs the head:
 * the #325/#347/#384/#394 mirror argument); the frozen engine read (the #454
 * NB) is untouched.
 *
 * <p><b>Test geometry (the seat-1..7 pattern):</b> same-workspace controls
 * (RED before the seat) + same-workspace inert pins (GREEN before AND after)
 * + drr 7.0.0 corpus locks (RED before the seat; ALL FOUR whole-file byte
 * locks, classifier-proven single-mechanism). Fixture-truth honoured: every
 * PRE form below was probed and recorded before any assertion was frozen
 * (the temporary Seat8ProbeTest, deleted with the seat landing).
 */
class RuleOutputCardFormSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model")
    );

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    // =========================================================================
    // Control model (same-workspace — the corpus shapes at unit grain)
    // =========================================================================

    /**
     * The corpus shapes at unit grain. {@code A1Sched} plays asic
     * NotionalAmountScheduleLeg1Rule (the whole-then-body with-args MULTI rule
     * call), {@code A2TwoSeat} plays jfsa NotionalAmountLeg1Rule (the SAME call
     * at an exists operand AND a collapsed decl-RHS seat — its own output
     * stays SINGLE), {@code CommonGrade}/{@code TwinGrade} play the common/cftc
     * DTCC_ProductGradeRule pair (the choice-option root + the no-args
     * cascade; the fixture cascade takes the BARE then route where the corpus
     * twin takes the lambda seat — the corpus lock covers that consumer).
     */
    private static final String CONTROL_MODEL = """
            namespace census.seat8
            version "1.0.0"

            type Trade:
                flag boolean (0..1)
                leg Leg (0..1)
                legs Leg (0..*)
                pay Payout (0..1)
                payout Payout (0..*)
                terms Terms (0..1)

            type Leg:
                vals number (0..*)
                val number (0..1)
                periods Period (0..*)
                sub SubT (0..1)
                mids string (0..*)
                    [metadata scheme]

            type SubT:
                sval number (0..1)

            type Period:
                pval string (0..1)

            type Terms:
                payout Payout (0..*)

            choice Payout:
                ComPay
                IrPay

            type ComPay:
                grade string (0..1)

            type IrPay:
                rate number (0..1)

            type Rep:
                g string (1..1)

            reporting rule IsOk from Trade: <"the filter control">
                extract flag

            reporting rule LegOne from Trade: <"single callee-arg rule">
                extract leg

            reporting rule Sched from Leg: <"the MULTI with-args callee — per-item periods">
                extract periods
                    then extract Period {
                        pval: item -> pval
                    }

            reporting rule SchedOne from Leg: <"the SINGLE with-args callee (b1)">
                extract val

            reporting rule MetaSched from Leg: <"the MULTI meta-output callee (b8)">
                extract mids

            reporting rule A1Sched from Trade: <"a1 — with-args MULTI call, whole then body">
                filter IsOk
                then Sched(LegOne)

            reporting rule A2TwoSeat from Trade: <"a2 — the exists operand + the decl-RHS seat">
                filter IsOk
                then if Sched(LegOne) exists
                    then (Sched(LegOne)
                        first
                        then extract pval)
                    else "none"

            reporting rule CommonGrade from Trade: <"a3 — the choice-option root">
                extract terms
                    then extract payout -> ComPay
                    then extract Rep {
                        g: item -> grade
                    }

            reporting rule TwinGrade from Trade: <"a4 — the no-args cascade over a3">
                filter IsOk
                then CommonGrade

            reporting rule B1Single from Trade: <"b1 — with-args SINGLE call stays MapperS">
                filter IsOk
                then SchedOne(LegOne)

            reporting rule B4Cat10 from Trade: <"b4a — attribute-channel silence: 2-name chain over MULTI then-items">
                extract legs
                    then extract sub -> sval

            reporting rule B4Cat10One from Trade: <"b4b — attribute-channel silence: 2-name chain over a SINGLE item">
                extract leg
                    then extract sub -> sval

            reporting rule B5SingleHead from Trade: <"b5 — choice-option nav over a SINGLE head">
                extract pay -> ComPay
                    then extract Rep {
                        g: item -> grade
                    }

            reporting rule B6Collapse from Trade: <"b6 — collapsing tail after the option nav">
                extract terms
                    then extract payout -> ComPay
                    then extract item -> grade
                    then first

            reporting rule B7NoArgsMulti from Trade: <"b7 — the existing no-args MapperC arm (adjacent control)">
                filter IsOk
                then extract leg
                then Sched

            reporting rule B8MetaArgs from Trade: <"b8 — with-args MULTI META-output callee (decline posture)">
                filter IsOk
                then MetaSched(LegOne)

            func B2Fn: <"b2 — the multi FUNCTION callee">
                inputs:
                    t Trade (1..1)
                output:
                    out number (0..*)
                set out:
                    t -> leg -> vals

            reporting rule B2FnCall from Trade: <"b2 — the bare FUNCTION then (transparent wrap)">
                filter IsOk
                then B2Fn

            func B3Fn: <"b3 — a with-args MULTI rule call in a FUNCTION body (strip seat)">
                inputs:
                    t Trade (1..1)
                output:
                    out Period (0..*)
                set out:
                    Sched(LegOne(t))

            func B3bFn: <"b3b — the NON-strip FUNCTION seat: the wrap survives into bytes (the gate discriminator)">
                inputs:
                    t Trade (1..1)
                output:
                    out boolean (1..1)
                set out:
                    Sched(LegOne(t)) exists
            """;

    // =========================================================================
    // Part A — controls (RED pre-seat)
    // =========================================================================

    /**
     * Rung A — the whole-then-body with-args MULTI rule call takes the
     * witnessed MapperC wrap (the asic shape). PRE (probed):
     * {@code output = toBuilder(MapperS.of(schedRule.evaluate(legOneRule
     * .evaluate(thenArg.get()))).getMulti());} — non-compiling
     * ({@code List<List<…>>} into {@code toBuilder}).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_withArgsMultiRuleCall_wholeThenBody_mapperCWrap() throws IOException {
        String out = generateRule(fixture(), "A1Sched");
        assertContains(out,
                "output = toBuilder(MapperC.<Period>of(schedRule.evaluate(legOneRule.evaluate(thenArg.get()))).getMulti());");
        assertNotContains(out, "MapperS.of(schedRule.evaluate(");
        assertContains(out, "import com.rosetta.model.lib.mapper.MapperC;");
        // The signature surface was ALREADY correct pre-seat (the engine's
        // thenAware recursion answers MULTI through the with-args ref) — the
        // rung touches ONLY the wrap token.
        assertContains(out, "implements ReportFunction<Trade, List<? extends Period>>");
    }

    /**
     * Rung A — the SAME call flips at BOTH expression seats of the jfsa shape:
     * the exists operand and the collapsed decl-RHS. The rule's OWN output
     * stays SINGLE (the consult is the CALLEE body's cardinality at the call
     * ref, never the enclosing rule's). PRE (probed): both seats
     * {@code MapperS.of(schedRule.evaluate(…))}; decl
     * {@code final MapperS<Period> thenArg1 = MapperS.of(…)} +
     * {@code .first()} continuation (assigns {@code MapperS<List<Period>>} to
     * {@code MapperS<Period>} — non-compiling).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_withArgsMultiRuleCall_existsOperandAndDeclSeat() throws IOException {
        String out = generateRule(fixture(), "A2TwoSeat");
        assertContains(out,
                "if (exists(MapperC.<Period>of(schedRule.evaluate(legOneRule.evaluate(thenArg0.get())))).getOrDefault(false)) {");
        assertContains(out,
                "final MapperS<Period> thenArg1 = MapperC.<Period>of(schedRule.evaluate(legOneRule.evaluate(thenArg0.get())))");
        assertNotContains(out, "MapperS.of(schedRule.evaluate(");
        // The enclosing rule's own scalar surface is untouched.
        assertContains(out, "implements ReportFunction<Trade, String>");
    }

    /**
     * Rung B — the choice-option root's whole output surface goes MULTI (the
     * common DTCC_ProductGradeRule shape): the overlay proves
     * {@code payout -> ComPay} multi through the SAME
     * {@code disguisedChainProvesMulti} walk the render's own
     * {@code mapSingleToList}/{@code mapC} selection used (the body's
     * MapperC pipeline was ALREADY byte-correct pre-seat — probed). PRE:
     * {@code ReportFunction<Trade, Rep>} + {@code .get());} terminal +
     * scalar builder/prune surfaces.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_choiceOptionRoot_outputSurfaceMulti() throws IOException {
        String out = generateRule(fixture(), "CommonGrade");
        assertContains(out, "implements ReportFunction<Trade, List<? extends Rep>>");
        assertContains(out, "public List<? extends Rep> evaluate(Trade input) {");
        assertContains(out, "protected abstract List<Rep.RepBuilder> doEvaluate(Trade input);");
        assertContains(out,
                "output = outputBuilder.stream().map(Rep::build).collect(Collectors.toList());");
        assertContains(out, "List<Rep.RepBuilder> output = new ArrayList<>();");
        assertContains(out, ".build())).getMulti());");
        assertContains(out,
                ".map(o -> o.stream().map(i -> i.prune()).collect(Collectors.toList()))");
        // The body pipeline stays byte-stable (it was already multi-proven).
        assertContains(out,
                ".mapSingleToList(item -> item.<Payout>mapC(\"getPayout\", terms -> terms.getPayout()).<ComPay>map(\"getComPay\", payout -> payout.getComPay()));");
    }

    /**
     * Rung B — the no-args cascade over the choice-option root: the overlay
     * recurses through the bare rule ref into the callee body (the engine's
     * {@code :241-248} thenAware recursion mirrored), so the twin's OWN
     * surface goes MULTI and its bare then-RHS takes the MapperC form through
     * the re-pointed no-args consult. PRE (probed):
     * {@code ReportFunction<Trade, Rep>} +
     * {@code output = toBuilder(MapperS.of(commonGradeRule.evaluate(thenArg.get())).get());}.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_cascadeOverChoiceOptionRoot_outputSurfaceMulti() throws IOException {
        String out = generateRule(fixture(), "TwinGrade");
        assertContains(out, "implements ReportFunction<Trade, List<? extends Rep>>");
        assertContains(out,
                "output = toBuilder(MapperC.<Rep>of(commonGradeRule.evaluate(thenArg.get())).getMulti());");
        assertContains(out,
                ".map(o -> o.stream().map(i -> i.prune()).collect(Collectors.toList()))");
    }

    // =========================================================================
    // Part B — inert pins (GREEN pre-seat AND post-seat)
    // =========================================================================

    /** b1 — a with-args SINGLE-bodied rule call keeps the scalar wrap (the
     *  819-golden-carrier class; the arm is MULTI-gated). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_withArgsSingleRuleCall_staysMapperS() throws IOException {
        String out = generateRule(fixture(), "B1Single");
        assertContains(out,
                "output = MapperS.of(schedOneRule.evaluate(legOneRule.evaluate(thenArg.get()))).get();");
        assertNotContains(out, "MapperC.<BigDecimal>of(schedOneRule");
    }

    /** b2 — the bare FUNCTION then keeps its transparent wrap (Rung A is
     *  rule-gated; the function route is untouched). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_bareFunctionThen_staysTransparentWrap() throws IOException {
        String out = generateRule(fixture(), "B2FnCall");
        assertContains(out,
                "output = MapperS.of(b2Fn.evaluate(thenArg.get())).get();");
        assertNotContains(out, "MapperC.<BigDecimal>of(b2Fn");
    }

    /** b3 — a with-args MULTI rule call in a FUNCTION body at a STRIP seat
     *  stays on today's raw stripped form. (The strip contract makes this pin
     *  gate-blind on its own — b3b is the discriminator; the indep review's
     *  NIT-1.) */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_functionBodyWithArgsRuleCall_staysRawStripped() throws IOException {
        String out = generateFunction(fixture(), "B3Fn");
        assertContains(out,
                "out = toBuilder(schedRule.evaluate(legOneRule.evaluate(t)));");
        assertNotContains(out, "MapperC.<Period>of(schedRule");
    }

    /** b3b — THE adversarial placement pin (indep review NIT-1): a with-args
     *  MULTI rule call at a NON-strip FUNCTION seat (an {@code exists}
     *  operand — the wrap SURVIVES into bytes; golden FUNCTION files carry the
     *  MapperS form, e.g. IsNotionalScheduleCustom) stays MapperS-wrapped —
     *  this pin goes RED if the {@code findEnclosingRule} gate is dropped
     *  (Rung A would MapperC-wrap it), unlike b3 whose strip seat renders
     *  identically either way. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3b_functionBodyWithArgsRuleCall_nonStripSeat_staysMapperS() throws IOException {
        String out = generateFunction(fixture(), "B3bFn");
        assertContains(out,
                "exists(MapperS.of(schedRule.evaluate(legOneRule.evaluate(t))))");
        assertNotContains(out, "MapperC.<Period>of(schedRule");
    }

    /** b4a — the #454 boundary: the attribute-chain channel (a 2-name
     *  single-declared chain over MULTI then-items) keeps today's surface —
     *  the overlay is choice-option-channel-exclusive and stays silent. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4a_attributeChainOverMultiItems_surfaceUnchanged() throws IOException {
        String out = generateRule(fixture(), "B4Cat10");
        assertContains(out, "implements ReportFunction<Trade, List<BigDecimal>>");
        assertContains(out,
                ".mapItem(item -> item.<SubT>map(\"getSub\", leg -> leg.getSub()).<BigDecimal>map(\"getSval\", subT -> subT.getSval())).getMulti();");
    }

    /** b4b — the attribute-chain channel over a SINGLE item stays scalar. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4b_attributeChainOverSingleItem_staysScalar() throws IOException {
        String out = generateRule(fixture(), "B4Cat10One");
        assertContains(out, "implements ReportFunction<Trade, BigDecimal>");
        assertNotContains(out, "List<? extends BigDecimal>");
    }

    /** b5 — a choice-option nav over a SINGLE head stays scalar (the overlay's
     *  proof is the head ATTRIBUTE's own declared cardinality). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b5_choiceOptionSingleHead_staysScalar() throws IOException {
        String out = generateRule(fixture(), "B5SingleHead");
        assertContains(out, "implements ReportFunction<Trade, Rep>");
        assertContains(out, "output = toBuilder(thenArg");
        assertContains(out, ".build())).get());");
    }

    /** b6 — a collapsing tail after the choice-option stage declines the
     *  overlay (the spine walk requires per-item non-collapsing later
     *  stages). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b6_collapsingTailAfterOptionNav_staysScalar() throws IOException {
        String out = generateRule(fixture(), "B6Collapse");
        assertContains(out, "implements ReportFunction<Trade, String>");
        assertContains(out, ".first().get();");
    }

    /** b7 — the EXISTING no-args MapperC arm's carrier renders byte-identically
     *  under the re-pointed consult (engine-first short-circuit). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b7_noArgsMultiDelegation_existingArmUnchanged() throws IOException {
        String out = generateRule(fixture(), "B7NoArgsMulti");
        assertContains(out, "implements ReportFunction<Trade, List<? extends Period>>");
        assertContains(out,
                "output = toBuilder(MapperC.<Period>of(schedRule.evaluate(thenArg1.get())).getMulti());");
    }

    /** b8 — the decline posture: a with-args MULTI META-output callee keeps
     *  today's bytes (zero corpus carriers; the faithful consumer form would
     *  be the upstream itemToWrapper re-lift, unknowable from goldens — the
     *  seat-5 Rung-C decline law; recorded as an OBS). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b8_withArgsMultiMetaCallee_declines() throws IOException {
        String out = generateRule(fixture(), "B8MetaArgs");
        assertContains(out,
                "output = MapperS.of(metaSchedRule.evaluate(legOneRule.evaluate(thenArg.get()))).getMulti();");
        assertNotContains(out, "MapperC.<String>of(metaSchedRule");
    }

    // =========================================================================
    // Part C — drr 7.0.0 corpus locks (RED pre-seat; ALL FOUR WHOLE-FILE,
    // classifier-proven single-mechanism)
    // =========================================================================

    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN_DIR =
            DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR7_GOLDEN_DIR), RuleOutputCardFormSeatTest.class);
    }

    private static Map<String, String> drr7Output;
    private static List<String> drr7GenErrors;

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
        var labelProviderGen = new LabelProviderGenerator(
                gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        Map<String, String> output = new LinkedHashMap<>();
        // The #577 R1/R2 harness law: capture the generator error lists; a
        // generation error naming a LOCKED file's FULL slash path fails that
        // lock loudly (the drr 7.0.0 cell's 3 standing TYPE_SWITCH_TERNARY_STUB
        // FUNCTION refusals named other files and passed through by design until
        // seat 23 healed them; the cell reports none today). Since the v3.1
        // close-out's discard sweep (#606) only the rule/function lists ride this
        // tolerance; the object-side generators' lists are strict ZERO through
        // assertNoGenerationErrors.
        drr7GenErrors = new ArrayList<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                assertNoGenerationErrors(pojoGen.generateClasses(model, version, output));
                assertNoGenerationErrors(choiceGen.generateClasses(model, version, output));
                ruleGen.generateClasses(model, version, output)
                        .forEach(e -> drr7GenErrors.add(e.getTargetPath() + " — " + e));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
                assertNoGenerationErrors(labelProviderGen.generateClasses(model, version, output));
            }
        }
        funcGen.generateWithErrors(output)
                .forEach(e -> drr7GenErrors.add(e.getTargetPath() + " — " + e));
        return output;
    }

    /** Rung A — the 3-line whole-file S10 exemplar (the asic grid). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_notionalAmountScheduleLeg1_asic_wholeFile() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/asic/rewrite/trade/reports/NotionalAmountScheduleLeg1Rule.java");
    }

    /** Rung A — the two-seat 5-line carrier (exists operand + decl RHS). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_notionalAmountLeg1_jfsa_wholeFile() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/jfsa/rewrite/trade/reports/NotionalAmountLeg1Rule.java");
    }

    /** Rung B — the choice-option root (the whole signature surface). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_dtccProductGrade_common_wholeFile() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/dtcc/trade/reports/DTCC_ProductGradeRule.java");
    }

    /** Rung B — the cascade twin (signature + the lambda-seat consumer). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_dtccProductGrade_cftc_wholeFile() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/cftc/rewrite/dtcc/trade/reports/DTCC_ProductGradeRule.java");
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        // Full-slash-path match (the #577 R2 law) — a twin's error must never
        // cross-attribute; a null-path error naming only the class still lands
        // on the byte-compare below.
        List<String> lockedErrors = drr7GenErrors.stream()
                .filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drr7Output.get(path);
        assertNotNull(generated, "not generated: " + path);
        Path goldenPath = DRR7_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated drr 7.0.0 output must byte-match the golden (newline-normalized) for "
                + path + " (seat 8 — the rule-output cardinality-form family).");
    }

    // =========================================================================
    // Harness (the seat-7 pattern, self-contained)
    // =========================================================================

    private static final class FixtureResult {
        final RModel model;
        final RLinkingResult linkingResult;
        final GeneratorModel generatorModel;
        final FunctionGenerator functionGenerator;

        FixtureResult(RModel model, RLinkingResult linkingResult,
                      GeneratorModel generatorModel, FunctionGenerator functionGenerator) {
            this.model = model;
            this.linkingResult = linkingResult;
            this.generatorModel = generatorModel;
            this.functionGenerator = functionGenerator;
        }
    }

    private static FixtureResult fixtureInstance;

    private static FixtureResult fixture() throws IOException {
        if (fixtureInstance == null) {
            fixtureInstance = loadFixture(CONTROL_MODEL);
        }
        return fixtureInstance;
    }

    private static FixtureResult loadFixture(String source) throws IOException {
        RModel model = AstBuilder.buildFromString(source, "seat8.rosetta");
        model.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(model);
        models.addAll(loadBuiltinsOnly());
        RLinkingResult linkingResult = RWorkspace.build(models);
        GeneratorModel gm = new GeneratorModel(linkingResult.workspace());
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        FunctionGenerator fg = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        return new FixtureResult(model, linkingResult, gm, fg);
    }

    /** All fixture outputs (rules via {@link RuleGenerator#generateClasses}, functions via
     *  {@link FunctionGenerator#generateWithErrors}) keyed by emitted path — built once. */
    private static Map<String, String> fixtureOutput;

    private static Map<String, String> fixtureOutput(FixtureResult fx) {
        if (fixtureOutput == null) {
            Map<String, String> output = new LinkedHashMap<>();
            JavaTypeUtil typeUtil = new JavaTypeUtil();
            RuleGenerator ruleGen = new RuleGenerator(fx.generatorModel,
                    new JavaTypeTranslator(typeUtil), fx.functionGenerator);
            List<String> errors = new ArrayList<>();
            ruleGen.generateClasses(fx.model, "1.0", output)
                    .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
            fx.functionGenerator.generateWithErrors(output)
                    .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
            reachabilityFence(fx.linkingResult);
            if (!errors.isEmpty()) {
                throw new AssertionError("fixture generation errors (a broken fixture"
                        + " must fail loudly, not skip): " + errors);
            }
            fixtureOutput = output;
        }
        return fixtureOutput;
    }

    private static String generateRule(FixtureResult fx, String ruleName) throws IOException {
        return lookup(fixtureOutput(fx), ruleName + "Rule.java");
    }

    private static String generateFunction(FixtureResult fx, String functionName) throws IOException {
        return lookup(fixtureOutput(fx), functionName + ".java");
    }

    private static String lookup(Map<String, String> output, String suffix) {
        return output.entrySet().stream()
                .filter(e -> e.getKey().endsWith("/" + suffix) || e.getKey().endsWith(suffix))
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
            throw new AssertionError("[RuleOutputCardFormSeatTest] builtins parse failures: "
                    + String.join("; ", failures));
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
