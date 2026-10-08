package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.utils.DeepFeatureCallUtil;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.ast.model.RModel;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #379 — the ladder/CR-then/enum-switch quintet (5 byte flips: cdm6 FUNCTION
 * StandardizedScheduleDuration; drr POJO Rules FirstExerciseDateRule +
 * DTCC_SEFOrDCMAnonymousExecutionIndicatorRule + PlatformAnonymousExecutionIndicatorRule +
 * PlatformIdentifierRule).
 *
 * <p><b>B aliasLadderNestedArmAdmit</b> (CollectionHandler + FunctionExpressionRenderer):
 * the #377-M1 nested-cond consumer predicate generalizes from the single-rung shape to
 * the full ELSELESS SPINE (each rung arm ctl-free or a ctl-free nested conditional
 * ladder, &ge;1 nested arm — the class marker), so the {@code optionExpiry} alias chain
 * hoists its invocation-bearing base as {@code final MapperS<ExerciseTerms> thenArg} and
 * the consumer renders golden's block ladder; the nested-tree multi-line-arm render
 * decline lifts WITH it (the #356 coupled pair; the render-truth {@code .then(} backstop
 * + the whole-discard guard protect the partial-restructure class). <b>B2</b> the
 * nav-tail extract transparency (isCleanLadderContext skips ONE enclosing extract whose
 * lambda body ROOT is the ladder's own map as a pure-nav chain tail — golden renders the
 * depth-2 block in place) + <b>B3</b> the wrapper-vs-join compiled deref evidence (AST
 * bare evidence + a COMPILED wrapper arm whose value type equals the meta-blind inferred
 * join → the #295 deref pass; the closure-param-rooted {@code trade -> tradeDate} arm)
 * flip FirstExerciseDateRule.
 *
 * <p><b>A crThenCondBlockAdmit</b> (CollectionHandler ×4 seats): a rule-path consumer
 * ladder whose ONLY ctl is CR-terminal nested then-chains inside rung CONDITIONS admits
 * at the #361 arm ({@code condCtlIsAdmissibleCrThenOnly}); tryDeepThenHoist pushes the
 * single-slot node-identity window around the consumer compile so the in-lambda
 * channel's isValueThen gate admits the condition's chain (hoisted per-lambda
 * {@code final MapperC<PartyInformation> thenArg = …;} + re-rooted
 * {@code ComparisonResult.ofNullSafe(exists(thenArg).asMapper())}); the name-taken
 * decline relaxes so the method group renumbers ({@code thenArg0/thenArg1} — the #350
 * collision-group law); the elseless block drains the cond-position DeepThenArgHoist at
 * BLOCK TOP (the #360 placement) with the typed {@code MapperS.<Boolean>ofNull()}
 * fall-through.
 *
 * <p><b>C enumSwitchLambdaBlock</b> (CollectionHandler): the ENUM-argument sibling of
 * the #226 choice-switch block lambda — the subject enum resolves via the #175
 * implicit-item walk's enum analogue (the parser leaves these guards UNRESOLVED: the
 * subject inference is blind, the same blindness that mis-binds the ternary's bare case
 * symbols), each NAME guard hierarchy-resolves ({@code findEnumValueInHierarchy} +
 * SUBJECT-name qualification — the #358 flatten law), and the block renders upstream's
 * {@code ==}-guard form with the {@code switchArgument} local + typed ofNull
 * null-guard/fall-throughs.
 *
 * <p>Whole-file byte locks run through the REAL D11 generation paths (the FUNCTION
 * route for cdm6; the full POJO/rule cell route for drr) and revert RED without the
 * facets; every witness token is occurrence-counted (python {@code str.count} semantics
 * — the #352 law) and PRE-counted against f-probe-378post (each removal token PRE
 * &ge; 1 / golden 0; each golden token PRE 0 / golden &ge; 1).
 */
class LadderCrThenEnumSwitchQuintetComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String STANDARDIZED_SCHEDULE_DURATION =
            "cdm/margin/schedule/functions/StandardizedScheduleDuration.java";
    private static final String FIRST_EXERCISE_DATE_RULE =
            "drr/standards/iosco/cde/version1/price/reports/FirstExerciseDateRule.java";
    private static final String DTCC_SEF_OR_DCM_ANONYMOUS_RULE =
            "drr/regulation/cftc/rewrite/dtcc/reports/DTCC_SEFOrDCMAnonymousExecutionIndicatorRule.java";
    private static final String PLATFORM_ANONYMOUS_RULE =
            "drr/regulation/csa/rewrite/trade/reports/PlatformAnonymousExecutionIndicatorRule.java";
    private static final String PLATFORM_IDENTIFIER_RULE =
            "drr/regulation/mas/rewrite/trade/reports/PlatformIdentifierRule.java";

    private static Map<String, String> drrOutput;
    private static Map<String, String> cdm6FnOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            drrOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
        if (cdm6CellAvailable()) {
            cdm6FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
    }

    /** The REAL D11 full-cell generation path (POJO/choice/rule/report/label + functions). */
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
        funcGen.generate(output);
        return output;
    }

    /** The REAL D11 FUNCTION-kind generation path (function_comparison). */
    private static Map<String, String> generateFunctions(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var funcGen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        assertNoGenerationErrors(funcGen.generateWithErrors(output));
        return output;
    }

    // ==== byte locks (all 5 flips through the REAL D11 routes) ====

    /** B: StandardizedScheduleDuration — the alias base hoist + nested multi-line-arm block. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void standardizedScheduleDuration_byteMatchesGolden() throws IOException {
        assertCdm6ByteMatchesGolden(STANDARDIZED_SCHEDULE_DURATION);
    }

    /** B2+B3, A ×2, C: the four drr POJO rules. */
    @Test
    @EnabledIf("drrCellAvailable")
    void drrQuartet_byteMatchesGolden() throws IOException {
        assertDrrByteMatchesGolden(FIRST_EXERCISE_DATE_RULE);
        assertDrrByteMatchesGolden(DTCC_SEF_OR_DCM_ANONYMOUS_RULE);
        assertDrrByteMatchesGolden(PLATFORM_ANONYMOUS_RULE);
        assertDrrByteMatchesGolden(PLATFORM_IDENTIFIER_RULE);
    }

    // ==== occurrence-counted witnesses (python str.count semantics — the #352 law) ====

    /**
     * B: the runtime {@code .then(item -> } form GONE from the optionExpiry alias (PRE 1 /
     * golden 0) and golden's hoisted base decl + typed ofNull fall-throughs PRESENT
     * (PRE 0 / golden 1 and 2).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void ssd_baseHoistAndTypedOfNull_witness() {
        String gen = cdm6FnOutput.get(STANDARDIZED_SCHEDULE_DURATION);
        assertNotNull(gen, "StandardizedScheduleDuration not generated");
        assertEquals(0, count(gen, ".then(item -> "),
                "the runtime .then( form must be gone (PRE 1)");
        assertEquals(1, count(gen, "final MapperS<ExerciseTerms> thenArg = "),
                "golden's hoisted base decl (PRE 0 / golden 1)");
        assertEquals(2, count(gen, "MapperS.<Date>ofNull()"),
                "golden's typed ofNull fall-throughs (PRE 0 / golden 2)");
        assertEquals(1, count(gen, ".mapSingleToItem(item -> {"),
                "golden's block-lambda consumer (PRE 0 / golden 1)");
    }

    /**
     * B2+B3: FirstExerciseDate — the inline-ternary tail GONE (PRE 1 / golden 0), the
     * wrapper-vs-join arm deref + the typed ofNull terminal PRESENT (PRE 0 / golden 1).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void firstExerciseDate_armDerefAndBlock_witness() {
        String gen = drrOutput.get(FIRST_EXERCISE_DATE_RULE);
        assertNotNull(gen, "FirstExerciseDateRule not generated");
        assertEquals(0, count(gen, " : MapperC.of())).get();"),
                "the inline-ternary tail must be gone (PRE 1)");
        assertEquals(1, count(gen, ".<Date>map(\"Type coercion\", fieldWithMetaDate -> "
                        + "fieldWithMetaDate == null ? null : fieldWithMetaDate.getValue())"),
                "the B3 wrapper-vs-join arm deref (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "MapperS.<Date>ofNull()"),
                "golden's typed ofNull terminal (PRE 0 / golden 1)");
    }

    /**
     * A: the DTCC twin — the in-branch level hoist ({@code thenArg1 = thenArg0}, the
     * method-group renumber), the re-rooted CR window condition, and the typed Boolean
     * ofNull fall-through PRESENT (each PRE 0 / golden 1); the runtime
     * {@code .then(item -> item} form GONE (PRE 1 / golden 0).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccSefOrDcm_crThenWindow_witness() {
        String gen = drrOutput.get(DTCC_SEF_OR_DCM_ANONYMOUS_RULE);
        assertNotNull(gen, "DTCC_SEFOrDCMAnonymousExecutionIndicatorRule not generated");
        assertEquals(0, count(gen, ".then(item -> item"),
                "the runtime .then( form must be gone (PRE 1)");
        assertEquals(1, count(gen, "final MapperS<ReportableInformation> thenArg1 = thenArg0"),
                "the in-branch level hoist + method renumber (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "ComparisonResult.ofNullSafe(exists(thenArg).asMapper())"),
                "the re-rooted CR window condition (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "return MapperS.<Boolean>ofNull();"),
                "the typed Boolean ofNull fall-through (PRE 0 / golden 1)");
    }

    /**
     * C: PlatformIdentifier — the mis-bound {@code Objects.equals(Admitted, item)}
     * ternary GONE (PRE 1 / golden 0); the {@code switchArgument} decl, the first
     * {@code ==} guard and the typed String ofNulls PRESENT (PRE 0 / golden 1, 1, 2).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void platformIdentifier_enumSwitchBlock_witness() {
        String gen = drrOutput.get(PLATFORM_IDENTIFIER_RULE);
        assertNotNull(gen, "PlatformIdentifierRule not generated");
        assertEquals(0, count(gen, "Objects.equals(Admitted, item)"),
                "the mis-bound ternary must be gone (PRE 1)");
        assertEquals(1, count(gen, "final TradableOnTradingVenueEnum switchArgument = item.get();"),
                "the switchArgument decl (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "if (switchArgument == TradableOnTradingVenueEnum.ADMITTED) {"),
                "the first ==-guard (PRE 0 / golden 1)");
        assertEquals(2, count(gen, "MapperS.<String>ofNull()"),
                "the typed String ofNulls (PRE 0 / golden 2)");
    }

    /** A twin: PlatformAnonymous carries the same window form (PRE 0 / golden 1). */
    @Test
    @EnabledIf("drrCellAvailable")
    void platformAnonymous_crThenWindow_witness() {
        String gen = drrOutput.get(PLATFORM_ANONYMOUS_RULE);
        assertNotNull(gen, "PlatformAnonymousExecutionIndicatorRule not generated");
        assertEquals(1, count(gen, "ComparisonResult.ofNullSafe(exists(thenArg).asMapper())"),
                "the re-rooted CR window condition (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "return MapperS.<Boolean>ofNull();"),
                "the typed Boolean ofNull fall-through (PRE 0 / golden 1)");
    }

    // ==== helpers ====

    /** Occurrence count — python {@code str.count} semantics (the #352 law). */
    private static int count(String haystack, String needle) {
        int n = 0;
        int i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }

    private static void assertDrrByteMatchesGolden(String path) throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String generated = drrOutput.get(path);
        assertNotNull(generated, "Class not generated: " + path);
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated drr output must byte-match the golden (newline-normalized) for " + path);
    }

    private static void assertCdm6ByteMatchesGolden(String path) throws IOException {
        assertNotNull(cdm6FnOutput, "cdm6 generation did not run — corpus unavailable?");
        String generated = cdm6FnOutput.get(path);
        assertNotNull(generated, "Class not generated: " + path);
        Path goldenPath = CDM6_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated cdm6 output must byte-match the golden (newline-normalized) for " + path);
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
