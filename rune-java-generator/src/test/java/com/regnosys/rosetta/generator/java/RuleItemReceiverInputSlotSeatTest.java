package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.unary.RExtractExpr;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompilerTest;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.expression.handlers.ReferenceHandler;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * v3.1 LADDER RETIREMENT — flip seat 9: the literal-item-receiver input-slot
 * family (the LAW-65 charter {@code target/seat9-charter.md}, written before
 * any emitter edit). Upstream's implicit variable IS the rule input in EVERY
 * slot (the law the fork's own #437 comment quotes at
 * {@code ReferenceHandler:6247-6249}). The fork's #143 arm
 * ({@code ReferenceHandler:6253-6259}) renders a NON-synthetic literal
 * {@code item} at rule-body top level as {@code MapperS.of(input)} — but its
 * parent-slot admission was {@code RSymbolReference || RBinaryExpression}
 * ONLY. The FEATURE-CALL RECEIVER slot ({@code item -> attr}: parent
 * {@link RFeatureCall} with {@code receiver() == expr}) was missing, so the
 * literal {@code item} rooting a navigation chain inside a with-args
 * function-call argument fell through to the {@code :6276} bare-{@code item}
 * fall-through — UNDEFINED at assignOutput root scope (the only {@code item}
 * identifiers there are later lambda params), NON-COMPILING. Golden
 * materializes the input: {@code MapperS.of(input).<X>map(…)}. The 31 drr
 * 7.0.0 carriers (all whole-file, all POJO/report kind):
 * {@code UniqueTransactionIdentifier*}/{@code Uti*}/{@code EsmaUti}/
 * {@code UTIFCAValue} rules across asic/cftc/csa/esma/fca/hkma/jfsa/mas ×
 * trade/margin/valuation — the drr 7.x model migration's
 * {@code uti.GetUniqueTransactionIdentifier(item -> reportableInformation, …)}
 * call shape (zero at 6.34.1).
 *
 * <p><b>The two-forms-agree mirror:</b> a BARE attribute at rule root already
 * synthesizes implicit-INPUT navigation
 * ({@code synthesizeImplicitInputNavigation}, {@code ReferenceHandler:335}) —
 * the arm makes the explicit {@code item -> x} form render EXACTLY like the
 * established bare {@code x} form. No engine read, no parser change.
 *
 * <p><b>Green-safety (LAW-66-controlled scans in the charter):</b> goldens
 * COMPILE; bare {@code item} at rule top level CANNOT; therefore no golden —
 * and no currently-identical gen file — can carry the form the arm replaces.
 * Scan A: golden drr 7.0.0 {@code evaluate(item.<} = 373 hits, EVERY one
 * lambda-bound (the no-{@code -> }-on-line refinement = 0; control fires 47
 * on the gen dump). Scan B: golden statement-root bare-item chains = 0
 * (synthetic known-hit control fires 2/2).
 *
 * <p><b>Untouched neighbors, each pinned:</b> the op-RECEIVER slot
 * ({@code item only-element} — the {@code ReferenceHandlerTest:790} law,
 * restated at b5) · the then-body thenArg binding (runs BEFORE the arm — b2 +
 * the stays-identical corpus lock) · in-lambda literal {@code item} (the
 * {@code nearestEnclosingInlineFunction} gate — b3) · FUNCTION-context
 * top-level (the {@code findEnclosingRule} gate — b1, THE adversarial
 * placement pin) · the binary-operand #437 slot (b4) · the bare whole-arg
 * {@code item} (the #143 RSymbolReference slot — asserted inside a1/a3/a4
 * needles: {@code …, null, input)}).
 *
 * <p><b>Test geometry (the seat-1..8 pattern):</b> AST-grain slot admission
 * (a0, RED pre-seat) + same-workspace controls (a1-a4, RED) + inert pins
 * (b1-b5, GREEN before AND after) + drr 7.0.0 corpus locks (4 whole-file
 * locks RED + 1 stays-identical lock GREEN). Fixture-truth honoured: every
 * PRE form was probed and recorded before any assertion was frozen (the
 * temporary Seat9ProbeTest, deleted with the seat landing).
 */
class RuleItemReceiverInputSlotSeatTest {

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
     * The corpus shapes at unit grain. {@code A1Line} plays the asic/jfsa/mas
     * trade single-line shape (receiver arg + {@code empty} + whole-item arg,
     * then a function-compare filter); {@code A2TwoRecv} plays the mas/jfsa
     * margin multiline TWO-receiver shape (the second a 2-step chain —
     * {@code item -> collateralDetails -> uniqueTradeIdentifier});
     * {@code A3Bare} plays esma EsmaUtiRule (the whole-body bare call — the
     * bare-seat unwrap); {@code A4IfDecl} plays hkma
     * UniqueTransactionIdentifierProprietaryRule (the SAME call at the
     * if-exists CONDITION and the thenArg DECL — an {@code if} is not a
     * lambda; both seats are top-level; the else branch's bare whole-item +
     * enum args pin the adjacent #143 slot).
     */
    private static final String CONTROL_MODEL = """
            namespace census.seat9
            version "1.0.0"

            type Trade:
                info Info (0..1)
                det Det (0..1)
                legs Leg (0..*)
                flag boolean (0..1)

            type Info:
                name string (0..1)

            type Det:
                uti Uti (0..1)

            type Uti:
                idval string (0..1)

            type Leg:
                val number (0..1)

            enum RegimeEnum:
                RA
                RB

            func GetId:
                inputs:
                    info Info (0..1)
                    uti Uti (0..1)
                    t Trade (0..1)
                output:
                    out string (0..1)
                set out:
                    info -> name

            func GetAlt:
                inputs:
                    t Trade (0..1)
                    r RegimeEnum (0..1)
                output:
                    out string (0..1)
                set out:
                    t -> info -> name

            func IsOkStr:
                inputs:
                    s string (0..1)
                output:
                    out boolean (1..1)
                set out:
                    s exists

            func LegNum:
                inputs:
                    v number (0..1)
                output:
                    out number (0..1)
                set out:
                    v

            reporting rule IsOkTrade from Trade: <"the filter control">
                extract flag

            reporting rule A1Line from Trade: <"a1 — the single-line trade shape">
                GetId(item -> info, empty, item)
                    then filter IsOkStr = True

            reporting rule A2TwoRecv from Trade: <"a2 — two receiver args, one a 2-step chain">
                GetId(
                        item -> info,
                        item -> det -> uti,
                        item
                    )
                    then filter IsOkStr = True

            reporting rule A3Bare from Trade: <"a3 — the whole-body bare call (EsmaUti)">
                GetId(item -> info, empty, item)

            reporting rule A4IfDecl from Trade: <"a4 — the two-seat if+decl (hkma)">
                if GetId(item -> info, empty, item) exists
                then (GetId(item -> info, empty, item)
                    then filter IsOkStr = False)
                else GetAlt(item, RegimeEnum -> RA)

            reporting rule B2ThenBody from Trade: <"b2 — the then-body item receiver (thenArg binding)">
                filter IsOkTrade
                then if GetId(item -> info, empty, item) exists
                    then "Y"
                    else "N"

            reporting rule B3Lambda from Trade: <"b3 — the in-lambda item receiver (bound binding)">
                extract legs
                    then extract LegNum(item -> val)
            """;

    /** b4 — the #437 binary-operand slot control (own workspace: a rule from a
     *  basic type, the report-rule-recursion golden shape). */
    private static final String B4_MODEL = """
            namespace census.seat9b
            version "1.0.0"

            reporting rule B4Binop from string: <"b4 — binary-operand slot control">
                if item = "x"
                then "Y"
                else "N"
            """;

    /** b1 — THE adversarial placement pin (own workspace so its untyped render
     *  cannot disturb the main fixture): a FUNCTION-body top-level literal
     *  {@code item -> attr} — {@code findEnclosingRule} is null, the arm must
     *  DECLINE and keep today's bare render (no valid corpus shape exists;
     *  probed PRE == POST). */
    private static final String B1_MODEL = """
            namespace census.seat9c
            version "1.0.0"

            type Trade:
                info Info (0..1)

            type Info:
                name string (0..1)

            func B1Fn: <"b1 — function-context top-level item receiver">
                inputs:
                    t Trade (1..1)
                output:
                    out string (0..1)
                set out:
                    item -> info -> name
            """;

    // =========================================================================
    // Part A0 — the slot admission at AST grain (RED pre-seat)
    // =========================================================================

    /**
     * The arm itself: a NON-synthetic literal {@code item} in the FEATURE-CALL
     * RECEIVER slot at rule-body top level renders {@code MapperS.of(input)}
     * — joining the #143 call-argument and #437 binary-operand slots. PRE
     * (probed): the bare {@code item} fall-through.
     */
    @Test
    void a0_literalItemFeatureCallReceiver_ruleTopLevel_rendersInput() {
        var iv = new RImplicitVariable();
        var fc = new RFeatureCall();
        fc.setFeatureName("info");
        fc.setReceiver(iv);
        iv.setParent(fc);
        var rule = new RRule();
        rule.setName("R");
        fc.setParent(rule);

        assertEquals("MapperS.of(input)", renderAst(iv));
    }

    /** b5 — the op-RECEIVER slot stays bare (the {@code ReferenceHandlerTest:790}
     *  law restated at seat grain: {@code item only-element} keeps the
     *  bare-item path — no witness moves it). */
    @Test
    void b5_literalItemOpReceiverSlot_staysBareItem() {
        var iv = new RImplicitVariable();
        var extract = new RExtractExpr();
        extract.setArgument(iv);
        iv.setParent(extract);
        var rule = new RRule();
        rule.setName("R");
        extract.setParent(rule);

        assertEquals("item", renderAst(iv));
    }

    private static String renderAst(RImplicitVariable iv) {
        var handler = new ReferenceHandler();
        var compiler = new ExpressionCompiler();
        var ctx = ExpressionContext.of(null, ExpressionCompilerTest.createTestScope());
        var result = handler.handle(iv, ctx, compiler);
        assertInstanceOf(JavaExpression.class, result);
        return ((JavaExpression) result).renderToString();
    }

    // =========================================================================
    // Part A — controls (RED pre-seat)
    // =========================================================================

    /**
     * a1 — the single-line trade shape: the receiver arg materializes the
     * input root; the {@code empty} arg stays {@code null}; the bare
     * whole-item arg stays {@code input} (the adjacent #143 slot). PRE
     * (probed): {@code evaluate(item.<Info>map(…).get(), null, input)}.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_singleLineShape_receiverMaterializesInput() throws IOException {
        String out = generateRule(fixture(), "A1Line");
        assertContains(out,
                "final MapperS<String> thenArg = MapperS.of(getId.evaluate(MapperS.of(input).<Info>map(\"getInfo\", trade -> trade.getInfo()).get(), null, input));");
        assertNotContains(out, "evaluate(item.<");
    }

    /**
     * a2 — the multiline margin shape: BOTH receiver args materialize,
     * including the 2-step chain. PRE (probed): both roots bare {@code item.}.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_twoReceiverShape_bothMaterialize() throws IOException {
        String out = generateRule(fixture(), "A2TwoRecv");
        assertContains(out,
                "final MapperS<String> thenArg = MapperS.of(getId.evaluate(MapperS.of(input).<Info>map(\"getInfo\", trade -> trade.getInfo()).get(), MapperS.of(input).<Det>map(\"getDet\", trade -> trade.getDet()).<Uti>map(\"getUti\", det -> det.getUti()).get(), input));");
        assertNotContains(out, "evaluate(item.<");
    }

    /**
     * a3 — the whole-body bare call (EsmaUti): the bare-seat unwrap keeps the
     * evaluate-arg strip, the receiver root materializes. PRE (probed):
     * {@code output = getId.evaluate(item.<Info>map(…).get(), null, input);}.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_wholeBodyBareCall_receiverMaterializes() throws IOException {
        String out = generateRule(fixture(), "A3Bare");
        assertContains(out,
                "output = getId.evaluate(MapperS.of(input).<Info>map(\"getInfo\", trade -> trade.getInfo()).get(), null, input);");
        assertNotContains(out, "evaluate(item.<");
    }

    /**
     * a4 — the two-seat hkma shape: the if-exists CONDITION and the thenArg
     * DECL both materialize (an {@code if} is not a lambda — both seats are
     * top-level); the else branch's bare whole-item + enum call is untouched.
     * PRE (probed): both seats bare {@code item.}; else already
     * {@code getAlt.evaluate(input, RegimeEnum.RA)}.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_ifConditionAndDeclSeats_bothMaterialize() throws IOException {
        String out = generateRule(fixture(), "A4IfDecl");
        assertContains(out,
                "if (exists(MapperS.of(getId.evaluate(MapperS.of(input).<Info>map(\"getInfo\", trade -> trade.getInfo()).get(), null, input))).getOrDefault(false)) {");
        assertContains(out,
                "final MapperS<String> thenArg = MapperS.of(getId.evaluate(MapperS.of(input).<Info>map(\"getInfo\", trade -> trade.getInfo()).get(), null, input));");
        assertContains(out, "output = getAlt.evaluate(input, RegimeEnum.RA);");
        assertNotContains(out, "evaluate(item.<");
    }

    // =========================================================================
    // Part B — inert pins (GREEN pre-seat AND post-seat)
    // =========================================================================

    /** b1 — THE adversarial placement pin: the FUNCTION-context top-level
     *  literal-item receiver keeps today's bare render ({@code
     *  findEnclosingRule} gate; probed PRE — the untyped {@code .map} chain,
     *  non-compiling, zero corpus carriers, the decline law). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_functionContextTopLevel_staysBare() throws IOException {
        String out = lookup(b1Output(), "B1Fn.java");
        assertContains(out,
                "out = item.map(\"getInfo\", _info -> _info.getInfo()).map(\"getName\", info -> info.getName()).get();");
        assertNotContains(out, "MapperS.of(input)");
    }

    /** b2 — the then-body item receiver resolves via the thenArg binding
     *  (which runs BEFORE the arm): BOTH the receiver chain and the whole-item
     *  arg stay thenArg-rooted; the input root must NOT reach through the
     *  then boundary. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_thenBodyReceiver_staysThenArgRooted() throws IOException {
        String out = generateRule(fixture(), "B2ThenBody");
        assertContains(out,
                "if (exists(MapperS.of(getId.evaluate(thenArg.<Info>map(\"getInfo\", trade -> trade.getInfo()).get(), null, thenArg.get()))).getOrDefault(false)) {");
        assertNotContains(out, "MapperS.of(input).<Info>");
    }

    /** b3 — the in-lambda item receiver keeps its bound-binding render (the
     *  {@code nearestEnclosingInlineFunction} gate; the 373-golden-hit class). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_inLambdaReceiver_staysLambdaBound() throws IOException {
        String out = generateRule(fixture(), "B3Lambda");
        assertContains(out,
                ".mapItem(item -> MapperS.of(legNum.evaluate(item.<BigDecimal>map(\"getVal\", leg -> leg.getVal()).get()))).getMulti();");
        assertNotContains(out, "MapperS.of(input).<BigDecimal>");
    }

    /** b4 — the #437 binary-operand slot keeps firing (a regression control on
     *  the shared condition expression). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_binaryOperandSlot_staysInput() throws IOException {
        String out = lookup(b4Output(), "B4BinopRule.java");
        assertContains(out,
                "if (areEqual(MapperS.of(input), MapperS.of(\"x\"), CardinalityOperator.All).getOrDefault(false)) {");
    }

    // =========================================================================
    // Part C — drr 7.0.0 corpus locks (4 whole-file RED + 1 stays-identical
    // GREEN; classifier-proven single-mechanism, the 31-carrier family)
    // =========================================================================

    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN_DIR =
            DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    /** The cell gate ALSO requires the builtins (Copilot #579 R1-1): the D11
     *  loader fails LOUD by design when the corpus is present but the builtins
     *  are absent (its R7/R8 dependency-resolution law), so a corpus-only gate
     *  would run these locks into that AssertionError instead of skipping —
     *  the same {@code builtinsAvailable} predicate the fixture tests use. */
    static boolean drr7Available() {
        return Drr7Corpus.gate(builtinsAvailable()
                && Files.isDirectory(DRR7_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR7_GOLDEN_DIR), RuleItemReceiverInputSlotSeatTest.class);
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

    /** The single-line trade shape — the 20-sibling grid representative. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_uniqueTransactionIdentifier_asicTrade_wholeFile() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/asic/rewrite/trade/reports/UniqueTransactionIdentifierRule.java");
    }

    /** The multiline TWO-receiver margin shape. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_utiProprietary_masMargin_wholeFile() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/mas/rewrite/margin/reports/UniqueTransactionIdentifierProprietaryRule.java");
    }

    /** The whole-body bare-call carrier — the MapperS import RETURNS with the
     *  healed receiver (the 3-line diff: import + statement). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_esmaUti_wholeFile() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/esma/emir/refit/trade/reports/EsmaUtiRule.java");
    }

    /** The two-seat if+decl carrier (the 4-line diff). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_utiProprietary_hkmaTrade_wholeFile() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/hkma/rewrite/trade/reports/UniqueTransactionIdentifierProprietaryRule.java");
    }

    /** The then-body frozen shape STAYS identical (cftc
     *  {@code then (if IsCleared(item -> originatingWorkflowStep)…)} — the
     *  thenArg binding path; byte-identical PRE and POST — the receipt's
     *  zero-movement prediction pinned). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_counterparty1FinancialEntityIndicator_cftcTrade_staysIdentical()
            throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/cftc/rewrite/trade/reports/Counterparty1FinancialEntityIndicatorRule.java");
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
                + path + " (seat 9 — the literal-item-receiver input-slot family).");
    }

    // =========================================================================
    // Harness (the seat-8 pattern, self-contained)
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
            fixtureInstance = loadFixture(CONTROL_MODEL, "seat9.rosetta");
        }
        return fixtureInstance;
    }

    private static FixtureResult loadFixture(String source, String fileName) throws IOException {
        RModel model = AstBuilder.buildFromString(source, fileName);
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

    /** All fixture outputs (rules via {@link RuleGenerator#generateClasses},
     *  functions via {@link FunctionGenerator#generateWithErrors}) keyed by
     *  emitted path — built once per workspace. */
    private static Map<String, String> fixtureOutput;
    private static Map<String, String> b4OutputMap;
    private static Map<String, String> b1OutputMap;

    private static Map<String, String> fixtureOutput(FixtureResult fx) {
        if (fixtureOutput == null) {
            fixtureOutput = buildOutput(fx);
        }
        return fixtureOutput;
    }

    private static Map<String, String> b4Output() throws IOException {
        if (b4OutputMap == null) {
            b4OutputMap = buildOutput(loadFixture(B4_MODEL, "seat9b.rosetta"));
        }
        return b4OutputMap;
    }

    private static Map<String, String> b1Output() throws IOException {
        if (b1OutputMap == null) {
            b1OutputMap = buildOutput(loadFixture(B1_MODEL, "seat9c.rosetta"));
        }
        return b1OutputMap;
    }

    private static Map<String, String> buildOutput(FixtureResult fx) {
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
        return output;
    }

    private static String generateRule(FixtureResult fx, String ruleName) throws IOException {
        return lookup(fixtureOutput(fx), ruleName + "Rule.java");
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
            throw new AssertionError("[RuleItemReceiverInputSlotSeatTest] builtins parse failures: "
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
