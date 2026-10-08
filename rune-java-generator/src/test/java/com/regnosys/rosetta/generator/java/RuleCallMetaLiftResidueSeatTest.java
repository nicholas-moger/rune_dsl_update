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
 * v3.1 LADDER RETIREMENT — flip seat 6: the rule-call meta-lift residue. A
 * reporting rule whose rosetta OUTPUT is META-typed ({@code [metadata scheme]}/
 * {@code [metadata reference]}) has a BARE-value Java {@code evaluate()}, and
 * golden re-LIFTS the bare value into the meta wrapper at every consuming seat
 * (upstream {@code TypeCoercionService.itemToWrapper}). The fork owns four lift
 * facets (#264/#265/#315/#331), all gated on ONE shared recovery
 * ({@code NavigationHandler.recoverInnerRuleMetaWrapper}); the drr 7.x rule
 * refactor introduced callee body shapes the recovery cannot type — a
 * point-free FUNC head ({@code RateOption ->> name}), a deep-path ({@code ->>})
 * terminal, and interior CHOICE-OPTION hops — so the seats decline and 18 drr
 * 7.0.0 POJO rule files stay banded whole (the seat-6 LAW-65 census,
 * {@code target/seat6-charter.md}). Two further gaps ride the same family: the
 * WITH-ARGS rule-call exists operand has no #315-style wrap arm at all
 * ({@code Spread(PayoutLeg2) exists}), and the #331 STATEMENT-seat wrap's live
 * {@code JavaConditionalExpression} refuses at the parenthesized ladder's
 * inner-rung condition consumer (the {@code UnderlyingIdentificationTypeRule}
 * whole-body TODO refusal).
 *
 * <p><b>Green-safety (the #264/#315 argument, verbatim from the shipped
 * Javadocs):</b> the bare {@code MapperS.of(<call>)} forms COMPILE and behave
 * identically (the lift is a semantic round-trip), so every firing site is a
 * COMPILES_DIVERGENT byte mismatch — a green file cannot carry the golden lift
 * form. The recovery widening keeps the DECLINE polarity (null on any
 * unresolvable hop — never a wrong proof). Monotone add-only.
 *
 * <p><b>Test geometry (the seat-1..5 pattern):</b> same-workspace controls
 * (RED before the seat — the defect is a recovery/arm gap, namespace-
 * independent) + same-workspace inert pins (GREEN before AND after) + drr
 * 7.0.0 corpus locks (RED before the seat; FOUR WHOLE-FILE byte locks — the
 * carriers' entire diff is this seat, classifier-proven).
 */
class RuleCallMetaLiftResidueSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model")
    );

    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN_DIR =
            DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR7_GOLDEN_DIR), RuleCallMetaLiftResidueSeatTest.class);
    }

    // =========================================================================
    // Control model (same-workspace — the corpus shapes at unit grain)
    // =========================================================================

    /**
     * The corpus shapes at unit grain. {@code RateIdx} plays InterestRateIndex
     * (a choice), {@code PickIdx} plays the {@code RateOption} func (the
     * point-free head), {@code RateA.name}/{@code Ident.idValue}/{@code
     * Terms.mnote} carry {@code [metadata scheme]} (FieldWithMetaString
     * leaves), {@code Terms.sched} carries {@code [metadata reference]} (the
     * ReferenceWithMeta leaf the Spread analog ends at).
     */
    private static final String CONTROL_MODEL = """
            namespace census.seat6
            version "1.0.0"

            type Ident:
                idValue string (1..1)
                    [metadata scheme]
                kind string (1..1)

            type IdxBase:
                name string (0..1)
                    [metadata scheme]

            type RateA extends IdxBase:
                ident Ident (0..*)

            type RateB extends IdxBase:
                other string (0..1)

            choice RateIdx:
                RateA
                RateB

            type Sched:
                val string (0..1)

            type Terms:
                sched Sched (0..1)
                    [metadata reference]
                mnote string (0..1)
                    [metadata scheme]
                note string (0..1)

            type Pay:
                idx RateIdx (0..1)
                terms Terms (0..1)
                note string (0..1)

            func PickIdx:
                inputs:
                    pay Pay (1..1)
                output:
                    idx RateIdx (0..1)
                set idx: pay -> idx

            reporting rule DeepName from Pay: <"A1 callee - func head + deep-path meta leaf">
                PickIdx ->> name

            reporting rule DeepNameCaller from Pay: <"A1 - bare-then terminal to the deep-path callee">
                filter note exists
                then DeepName

            reporting rule DeepNameChainedCaller from Pay: <"O - CHAINED terminal to the meta callee">
                filter note exists
                then filter note exists
                then DeepName

            reporting rule ChainIdent from Pay: <"A2 callee - func head + choice-option hop + meta leaf">
                PickIdx
                    then filter item -> RateA -> ident -> kind any = "ISIN"
                    then extract item -> RateA -> ident -> idValue only-element

            reporting rule ChainIdentCaller from Pay: <"A2 - bare-then terminal to the chain callee">
                filter note exists
                then ChainIdent

            reporting rule PayTerms from Pay: <"A3 arg rule">
                extract terms

            reporting rule PickSched from Terms: <"A3 with-args callee - metadata-reference output">
                extract
                    if sched exists
                    then sched

            reporting rule SchedExistsCaller from Pay: <"A3 - with-args + bare exists operands">
                filter note exists
                then extract
                    if PickSched(PayTerms) exists
                    then "HAS"
                    else if DeepName exists
                    then "NAME"

            reporting rule EnumFirst from Pay: <"A4 first-rung callee - non-meta">
                extract note

            reporting rule PlainMeta from Pay: <"A4 second-rung callee - meta output, recovery works today">
                extract terms -> mnote

            reporting rule LadderCaller from Pay: <"A4 - the parenthesized ladder statement seat">
                filter note exists
                then (if EnumFirst exists
                    then EnumFirst
                    else if PlainMeta exists
                    then "OTHR")

            reporting rule PlainCallee from Pay: <"B1 non-meta callee">
                extract note

            reporting rule PlainCaller from Pay: <"B1 - bare form stays">
                filter note exists
                then PlainCallee

            reporting rule ThroughMeta from Pay: <"B2 callee - navigates THROUGH the meta wrapper to a plain leaf">
                extract terms -> sched -> val

            reporting rule ThroughMetaCaller from Pay: <"B2 - recovery declines, wrapper navigated through">
                filter note exists
                then ThroughMeta

            reporting rule ExplicitMetaCaller from Pay: <"B4 - the already-working 264 lift stays identical">
                filter note exists
                then PlainMeta

            reporting rule ValueSeatCaller from Pay: <"B5 - the with-args meta call as a branch VALUE">
                filter note exists
                then extract
                    if note exists
                    then PickSched(PayTerms)

            func FnSibling:
                inputs:
                    pay Pay (1..1)
                output:
                    result string (0..1)
                set result: PickIdx(pay) ->> name
            """;

    private static FixtureResult fixture;

    @BeforeAll
    static void buildFixture() throws IOException {
        if (builtinsAvailable()) {
            fixture = loadFixture(CONTROL_MODEL);
        }
    }

    // =========================================================================
    // Part A — same-workspace controls (RED before the seat; fixture-truth: the
    // asserted needles were probe-recorded PRE and POST — seat6-probe runs 1..8)
    // =========================================================================

    /** Rung R1 + the #264 seat: the SINGLE-then bare-rule terminal over a DEEP-PATH
     *  meta callee lifts and unwraps (PRE: bare {@code output = MapperS.of(
     *  deepNameRule.evaluate(thenArg.get())).get();} — the recovery declined on the
     *  {@code ->>} body). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_deepPathCallee_bareThenTerminal_liftsAndUnwraps() {
        String out = generateRule(fixture, "DeepNameCaller");
        assertContains(out, "final String string = deepNameRule.evaluate(thenArg.get());");
        assertContains(out, "final FieldWithMetaString fieldWithMetaString = (string == null"
                + " ? MapperS.<FieldWithMetaString>ofNull() : MapperS.of(FieldWithMetaString"
                + ".builder().setValue(string).build())).get();");
        assertContains(out, "output = fieldWithMetaString.getValue();");
        assertNotContains(out, "output = MapperS.of(deepNameRule.evaluate(thenArg.get())).get();");
    }

    /** Rung O: the CHAINED-terminal twin — a bare-rule terminal over the hoisted
     *  thenArg chain lifts identically (PRE: the #254 route's bare terminal; the
     *  #264 arm was single-then-gated and never reached the chain). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_metaCallee_chainedTerminal_liftsAndUnwraps() {
        String out = generateRule(fixture, "DeepNameChainedCaller");
        assertContains(out, "final MapperS<Pay> thenArg1 = thenArg0");
        assertContains(out, "final String string = deepNameRule.evaluate(thenArg1.get());");
        assertContains(out, "final FieldWithMetaString fieldWithMetaString = (string == null"
                + " ? MapperS.<FieldWithMetaString>ofNull() : MapperS.of(FieldWithMetaString"
                + ".builder().setValue(string).build())).get();");
        assertContains(out, "output = fieldWithMetaString.getValue();");
    }

    /** Rung E + R1: the WITH-ARGS rule-call exists operand wraps (the arm never
     *  existed at the with-args seat) AND the bare else-if exists over the
     *  deep-path callee wraps (recovery via R1) — both hoists drain into the map
     *  lambda's block body (PRE: both bare {@code exists(MapperS.of(<call>))}). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_withArgsAndBareExistsOperands_wrapDistributes() {
        String out = generateRule(fixture, "SchedExistsCaller");
        assertContains(out, "final Sched sched = pickSchedRule.evaluate("
                + "payTermsRule.evaluate(item.get()));");
        assertContains(out, "if ((sched == null ? exists(MapperS.<ReferenceWithMetaSched>"
                + "ofNull()).getOrDefault(false) : exists(MapperS.of(ReferenceWithMetaSched"
                + ".builder().setValue(sched).build())).getOrDefault(false))) {");
        assertContains(out, "final String string = deepNameRule.evaluate(item.get());");
        assertContains(out, "if ((string == null ? exists(MapperS.<FieldWithMetaString>"
                + "ofNull()).getOrDefault(false) : exists(MapperS.of(FieldWithMetaString"
                + ".builder().setValue(string).build())).getOrDefault(false))) {");
        assertNotContains(out, "exists(MapperS.of(pickSchedRule.evaluate(");
    }

    /** Rung L: the ladder-rung exists-wrap consumer — PRE the WHOLE body refused
     *  ({@code /* TODO: expression compilation error: Expected JavaExpression but
     *  got JavaConditionalExpression … }); POST the rung renders golden's
     *  nested-else form (the hoist INSIDE the else block, ahead of the inner if
     *  with the collapsed parenthesized ternary). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_ladderRungExistsWrap_rendersNestedElseCollapsedTernary() {
        String out = generateRule(fixture, "LadderCaller");
        assertNotContains(out, "expression compilation error");
        int elseIdx = out.indexOf("} else {");
        int hoistIdx = out.indexOf("final String string = plainMetaRule.evaluate(thenArg.get());");
        int rungIdx = out.indexOf("if ((string == null ? exists(MapperS.<FieldWithMetaString>"
                + "ofNull()).getOrDefault(false) : exists(MapperS.of(FieldWithMetaString"
                + ".builder().setValue(string).build())).getOrDefault(false))) {");
        assertTrue(elseIdx >= 0 && hoistIdx > elseIdx && rungIdx > hoistIdx,
                "the else block must open, then hoist the value decl, then render the"
                + " collapsed-ternary inner if (indexes: else=" + elseIdx + " hoist="
                + hoistIdx + " rung=" + rungIdx + ")\n" + out);
    }

    /** Rung W2: the bare point-free FUNC-invocation receiver of a deep-path step
     *  wraps {@code MapperS.of(…)} (PRE: {@code pickIdx.evaluate(input)
     *  .<FieldWithMetaString>map(…)} — {@code .map} on a bare model value,
     *  non-compiling; the corpus twin is drr NameOfTheFloatingRateRule). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a5_deepPathFuncReceiver_wrapsMapperSOf() {
        String out = generateRule(fixture, "DeepName");
        assertContains(out, "MapperS.of(pickIdx.evaluate(input)).<FieldWithMetaString>map("
                + "\"chooseName\", rateIdx -> rateIdxDeepPathUtil.chooseName(rateIdx))");
        assertNotContains(out, "= pickIdx.evaluate(input).<FieldWithMetaString>map(");
    }

    // =========================================================================
    // Part B — same-workspace INERT PINS (GREEN before AND after the seat)
    // =========================================================================

    /** LAW 60's untouched arm: a NON-meta callee keeps the bare round-trip form —
     *  the recovery declines (no meta anywhere) and no lift token appears. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_nonMetaCallee_keepsBareForm() {
        String out = generateRule(fixture, "PlainCaller");
        assertContains(out, "output = MapperS.of(plainCalleeRule.evaluate(thenArg.get())).get();");
        assertNotContains(out, ".builder().setValue(");
    }

    /** The #264 recovery decline control: a callee navigating THROUGH a meta
     *  wrapper to a PLAIN leaf ({@code terms -> sched -> val}) recovers null
     *  (the terminal is meta-free) — the bare form stays. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_metaNavigatedThrough_keepsBareForm() {
        String out = generateRule(fixture, "ThroughMetaCaller");
        assertContains(out, "output = MapperS.of(throughMetaRule.evaluate(thenArg.get())).get();");
        assertNotContains(out, ".builder().setValue(");
    }

    /** The FUNCTION-context sibling (the cp2e recovery-local visibility pin): the
     *  with-args deep-path receiver in a FUNCTION body keeps its pre-seat form
     *  byte-for-byte (already wrapped — Rung W2's bare-point-free gate excludes
     *  the explicit-args shape; the RULE-scoped rungs R1/O/E/L never reach a
     *  function). Rung W2 itself is SHAPE-scoped, not rule-scoped — a bare
     *  point-free func-ref deep receiver in a FUNCTION body would wrap too,
     *  green-safe by the same never-compiled argument (the indep review's
     *  balanced-paren corpus scan found ZERO goldens chaining map/mapC on a bare
     *  evaluate() result). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_functionContextDeepPath_staysByteFrozen() {
        String out = generateFunction(fixture, "FnSibling");
        assertContains(out, "final FieldWithMetaString fieldWithMetaString = "
                + "MapperS.of(pickIdx.evaluate(pay)).<FieldWithMetaString>map(\"chooseName\","
                + " rateIdx -> rateIdxDeepPathUtil.chooseName(rateIdx)).get();");
    }

    /** The adjacent-arm control: the already-working #264 SINGLE-then lift over an
     *  explicit-nav meta callee (the 6.34.1-healed shape) renders IDENTICALLY
     *  through the widened seat. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_explicitNavMetaCallee_singleThenLift_unchanged() {
        String out = generateRule(fixture, "ExplicitMetaCaller");
        assertContains(out, "final String string = plainMetaRule.evaluate(thenArg.get());");
        assertContains(out, "output = fieldWithMetaString.getValue();");
    }

    /** The adversarial placement pin: the with-args META rule call as a branch
     *  VALUE (not an exists operand) keeps today's bytes — Rung E is gated on
     *  {@code expr.parent() instanceof RExistenceExpr}. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b5_withArgsMetaCallAsBranchValue_keepsBytes() {
        String out = generateRule(fixture, "ValueSeatCaller");
        assertNotContains(out, "== null ? exists(");
    }

    /** The recovery-already-works chain-callee pin: the SINGLE-then caller over the
     *  func-head + choice-option-hop callee lifted BEFORE the seat (the recovery
     *  handled this body pre-seat) and must render IDENTICALLY after. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b6_chainCalleeSingleThen_liftAlreadyFires_unchanged() {
        String out = generateRule(fixture, "ChainIdentCaller");
        assertContains(out, "final String string = chainIdentRule.evaluate(thenArg.get());");
        assertContains(out, "output = fieldWithMetaString.getValue();");
    }

    // =========================================================================
    // Part C — drr 7.0.0 corpus locks (RED pre-seat; WHOLE-FILE, classifier-proven)
    // =========================================================================

    private static Map<String, String> drr7PojoOutput;

    @BeforeAll
    static void generateDrr7Pojo() throws IOException {
        if (drr7Available()) {
            drr7PojoOutput = generatePojoCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT));
        }
    }

    private static Map<String, String> generatePojoCell(D11CorpusRegressionTest.CellSpec cell)
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
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                assertNoGenerationErrors(pojoGen.generateClasses(model, version, output));
                assertNoGenerationErrors(choiceGen.generateClasses(model, version, output));
                assertNoGenerationErrors(ruleGen.generateClasses(model, version, output));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
                assertNoGenerationErrors(labelProviderGen.generateClasses(model, version, output));
            }
        }
        return output;
    }

    /** The output-assign lift (deep-path callee, reached transitively). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_nameOfTheFloatingRateLeg1_hkma_wholeFile() throws IOException {
        assertByteMatchesGolden("drr/regulation/hkma/rewrite/trade/reports/NameOfTheFloatingRateLeg1Rule.java");
    }

    /** The output-assign lift (func-head + choice-option-hop chain callee). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_floatingRateIdentifierLeg1_mas_wholeFile() throws IOException {
        assertByteMatchesGolden("drr/regulation/mas/rewrite/trade/reports/FloatingRateIdentifierLeg1Rule.java");
    }

    /** The exists-operand lifts (with-args + bare, two per file). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_spreadOfLeg2_common_wholeFile() throws IOException {
        assertByteMatchesGolden("drr/regulation/common/emir/reports/SpreadOfLeg2Rule.java");
    }

    /**
     * The ladder-rung consumer refusal heal — SCOPED NEEDLES, not whole-file: the
     * probe measured the post-seat residue as EXACTLY the conditional-hoist
     * Mapper-form family (bare-typed {@code ifThenElseResult} + bare branch values
     * + the {@code .get()}-less output — the NEXT seat's family, explicitly not
     * this one), so the file heals PARTIAL here. The needles lock this seat's
     * lines golden-quoted: the refusal token REMOVED (the negative witness — the
     * pre-seat gen body was the TODO comment), the else block opening, the value
     * hoist INSIDE it, and the collapsed-ternary inner if — in golden's order.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_underlyingIdentificationType_hkma_refusalHealNeedles() {
        assertNotNull(drr7PojoOutput, "drr 7.0.0 POJO generation did not run — corpus unavailable?");
        String out = drr7PojoOutput.get(
                "drr/regulation/hkma/rewrite/trade/reports/UnderlyingIdentificationTypeRule.java");
        assertNotNull(out, "UnderlyingIdentificationTypeRule was not generated");
        assertNotContains(out, "expression compilation error");
        int elseIdx = out.indexOf("} else {");
        int hoistIdx = out.indexOf(
                "final String string = underlierIdOtherRule.evaluate(thenArg.get());");
        int rungIdx = out.indexOf("if ((string == null ? exists(MapperS.<FieldWithMetaString>"
                + "ofNull()).getOrDefault(false) : exists(MapperS.of(FieldWithMetaString"
                + ".builder().setValue(string).build())).getOrDefault(false))) {");
        assertTrue(elseIdx >= 0 && hoistIdx > elseIdx && rungIdx > hoistIdx,
                "golden's nested-else + hoist + collapsed-ternary order must hold"
                + " (else=" + elseIdx + " hoist=" + hoistIdx + " rung=" + rungIdx + ")");
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(drr7PojoOutput, "drr 7.0.0 POJO generation did not run — corpus unavailable?");
        String generated = drr7PojoOutput.get(path);
        assertNotNull(generated, "Rule class not generated: " + path);
        Path goldenPath = DRR7_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated drr 7.0.0 Rule output must byte-match the golden (newline-normalized) for "
                + path + " (seat 6 — the rule-call meta-lift residue).");
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

    // =========================================================================
    // Harness (the RuleGeneratorTest pattern, self-contained)
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

    private static FixtureResult loadFixture(String source) throws IOException {
        RModel model = AstBuilder.buildFromString(source, "seat6.rosetta");
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

    private static String generateRule(FixtureResult fx, String ruleName) {
        return lookup(fixtureOutput(fx), ruleName + "Rule.java");
    }

    private static String generateFunction(FixtureResult fx, String functionName) {
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
            throw new AssertionError("[RuleCallMetaLiftResidueSeatTest] builtins parse failures: "
                    + String.join("; ", failures));
        }
        return models;
    }
}
