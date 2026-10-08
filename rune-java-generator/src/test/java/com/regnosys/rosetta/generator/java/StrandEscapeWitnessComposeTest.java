package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #376 anchor — the strand/escape/witness quintet (5 byte flips: 3 drr FUNCTION +
 * 2 drr POJO Rule) + the M3/M4 EMIR riders.
 *
 * <p><b>S1 strandedBlankFinalArmHoistPull</b> (FunctionExpressionRenderer
 * {@code armReferencesHoistDecl}): the declared name of a BLANK-FINAL block hoist
 * ({@code final MapperS<X> <name>;} + if/else assignment lines — the #351 deep-then
 * ite block) extracts from the FIRST line, so the ITE-arm drain pulls the block —
 * and the thenArg decl only IT references, via the #333 transitive closure — into
 * the owning branch instead of stranding both (the #331 retro-5 drop class):
 * Counterparty1FinancialEntityIndicatorRule.
 *
 * <p><b>S2 aliasLadderArmThenHoist</b> (appendReturnLadder): the THEN-ARM twin of
 * the #359 F-8 terminal-else mark/drain — a return-ladder rung whose value is a
 * then-chain splices its sink-registered {@code thenArg0..2} decls before the
 * in-rung return. <b>E onlyElemLambdaVarScopeEscape</b> (NavigationHandler): the
 * ONLY_ELEMENT-collapse receiver arm registers its type-derived lambda var
 * DEFERRED (a fresh per-call lambda child), so the seeded-scope self-shadow escape
 * emerges ({@code _tradeLot ->} under a {@code tradeLot} method param — the
 * unescaped form is an illegal Java shadow, so no green file carries it):
 * ETDNotionalOption + ETDNotionalFuture.
 *
 * <p><b>J listLiteralThenMetaWitness</b> (LiteralHandler part 1 + the
 * renderMetaValueDerefOrNull part-2 lockstep): a then-chain list-literal element
 * resolves its leaf through the LAST then-body (unwrapping the {@code then …
 * extract [<nav>]} RExtractExpr), so a homogeneous meta-leaf literal witnesses the
 * WRAPPER ({@code MapperC.<FieldWithMetaString>of(…)}) and the whole-output
 * hoist + null-guarded {@code .getValue()} deref fires on the SAME predicate
 * (render-truth by shared-predicate lockstep, NOT the #361-refuted AST walk):
 * GetCreditUnderlierISIN.
 *
 * <p><b>G1 getOrDefaultMinMaxArgCollapse + G2 thenTerminalCtorWrap</b>: a MIN/MAX-
 * collapse {@code default} RIGHT collapses the arg {@code .get()} and re-wraps
 * {@code MapperS.of(…)} (the #234 dual-consumer law — golden never keeps a Mapper
 * as a getOrDefault argument); a LAST then-body CONSTRUCTOR wraps
 * {@code toBuilder(MapperS.of(<ctor>).get())} (the #180 lambda-seat law; the
 * 947-file direct-ctor bare-golden population keeps its bytes by the AST gate):
 * SingleOrUpperAndLowerBarrierRule.
 *
 * <p><b>M4 rider</b> (the banked M1/M2 venueMic restructure completes the
 * EMIR_ISIN/UKEMIR_ISIN flips at #377): corpusCollisionEnumArg — an RCorpus-bound
 * bare enum value qualifies against the callee's declared enum param
 * ({@code RegimeNameEnum.EMIR} — rides EMIR/UKEMIR_ISIN + the Venue twins).
 *
 * <p>Every pre-fix form was NON_COMPILING (a dangling sentinel reference; a lambda
 * param shadowing a method param; a bare {@code <String>} witness over
 * wrapper-typed Mapper args; a Mapper into {@code getOrDefault(T)}; an unqualified
 * bare name) — zero of the 34,686 goldens carry any pre-fix token (every witness
 * below was pre-counted against f-probe-375post and 0 in golden). Whole-file byte
 * comparisons run through the REAL D11 generation paths and revert RED without
 * the facets.
 */
class StrandEscapeWitnessComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String ETD_OPTION =
            "drr/regulation/common/functions/ETDNotionalOption.java";
    private static final String ETD_FUTURE =
            "drr/regulation/common/functions/ETDNotionalFuture.java";
    private static final String GET_CREDIT_UNDERLIER_ISIN =
            "drr/enrichment/upi/functions/GetCreditUnderlierISIN.java";
    private static final String COUNTERPARTY1_FEI =
            "drr/regulation/cftc/rewrite/trade/reports/Counterparty1FinancialEntityIndicatorRule.java";
    private static final String SINGLE_OR_UPPER_LOWER =
            "drr/regulation/common/trade/price/reports/SingleOrUpperAndLowerBarrierRule.java";
    private static final String EMIR_ISIN =
            "drr/regulation/esma/emir/refit/trade/functions/EMIR_ISIN.java";

    private static Map<String, String> drrFnOutput;
    private static Map<String, String> drrRuleOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            drrFnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
            drrRuleOutput = generateRuleKinds(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
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

    /** The REAL D11 rule-kind generation path (the drr POJO Rule carriers). */
    private static Map<String, String> generateRuleKinds(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell),
                D11CorpusRegressionTest.readDoNotPrune(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var enumGen = new EnumGenerator(gm);
        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                assertNoGenerationErrors(enumGen.generateClasses(model, version, output));
                assertNoGenerationErrors(pojoGen.generateClasses(model, version, output));
                assertNoGenerationErrors(choiceGen.generateClasses(model, version, output));
                assertNoGenerationErrors(ruleGen.generateClasses(model, version, output));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
            }
        }
        return output;
    }

    // ==== byte locks (all 5 flips through the REAL D11 routes) ====

    @Test
    @EnabledIf("drrCellAvailable")
    void drrFunctionFlips_byteMatchGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, ETD_OPTION);
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, ETD_FUTURE);
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, GET_CREDIT_UNDERLIER_ISIN);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void drrRuleFlips_byteMatchGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, COUNTERPARTY1_FEI);
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, SINGLE_OR_UPPER_LOWER);
    }

    // ==== occurrence-counted witnesses (tokens PRE-counted vs f-probe-375post) ====

    /**
     * E: the pre-fix ETDNotionalOption carried the self-shadowing
     * {@code tradeLot -> tradeLot.getPriceQuantity()} at exactly 5 seats (7 in
     * Future) — an illegal Java shadow of the method param, count 0 in golden.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void etdNotional_selfShadowEscaped() {
        assertEquals(0, count(generated(drrFnOutput, ETD_OPTION),
                "tradeLot -> tradeLot.getPriceQuantity()"),
                "the ONLY_ELEMENT-receiver lambda var must escape _tradeLot (PRE 5)");
        assertEquals(0, count(generated(drrFnOutput, ETD_FUTURE),
                "tradeLot -> tradeLot.getPriceQuantity()"),
                "the ONLY_ELEMENT-receiver lambda var must escape _tradeLot (PRE 7)");
    }

    /**
     * S1: golden carries the un-stranded in-branch then-hoist decl (PRE 0 — the
     * registration happened but the statement dropped at the ITE-arm drain).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty1_strandedHoistPulled() {
        assertEquals(1, count(generated(drrRuleOutput, COUNTERPARTY1_FEI),
                "final MapperC<ReportingRegime> thenArg1 = thenArg0"),
                "the blank-final block pull must relocate the thenArg decl in-branch");
    }

    /**
     * J: the pre-fix value-typed join ({@code result = MapperC.<String>of(thenArg0}
     * — a type lie over wrapper-typed Mapper args, count 0 in golden) is replaced
     * by the wrapper witness + the hoisted null-guarded deref.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getCreditUnderlierIsin_wrapperWitnessAndDeref() {
        String gen = generated(drrFnOutput, GET_CREDIT_UNDERLIER_ISIN);
        assertEquals(0, count(gen, "result = MapperC.<String>of(thenArg0"),
                "the value-typed join must be gone (PRE 1, golden 0)");
        assertEquals(1, count(gen,
                "final FieldWithMetaString fieldWithMetaString = MapperC.<FieldWithMetaString>of(thenArg0"),
                "the wrapper-typed join hoist must land");
    }

    /**
     * G1+G2: the Mapper-arg getOrDefault (PRE 1, golden 0) and the bare
     * then-terminal ctor toBuilder (PRE 1, golden 0) are both re-formed.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void singleOrUpperLower_getOrDefaultAndCtorWrap() {
        String gen = generated(drrRuleOutput, SINGLE_OR_UPPER_LOWER);
        assertEquals(0, count(gen, "thenArg1 = MapperC.<TriggerEvent>of(thenArg0"),
                "the Mapper-form getOrDefault arm must re-wrap (PRE 1, golden 0)");
        assertEquals(0, count(gen, "output = toBuilder(SingleOrUpperAndLowerBarrier.builder()"),
                "the bare then-terminal ctor toBuilder must wrap (PRE 1, golden 0)");
        assertEquals(1, count(gen, "output = toBuilder(MapperS.of(SingleOrUpperAndLowerBarrier.builder()"),
                "golden's wrapped ctor output must land");
    }

    /**
     * M4 rider: the bare corpus-collision {@code EMIR} arg (PRE 1, golden 0)
     * qualifies to the enum constant; the EMIR_ISIN flip itself is BANKED on the
     * M1/M2 venueMic remainder (#377).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void emirIsin_corpusCollisionArgQualified() {
        String gen = generated(drrFnOutput, EMIR_ISIN);
        assertEquals(0, count(gen, ", EMIR, SupervisoryBodyEnum.ESMA))"),
                "the bare corpus-collision arg must be gone (PRE 1, golden 0)");
        assertEquals(1, count(gen, "RegimeNameEnum.EMIR"),
                "the qualified enum constant must land");
    }

    // ==== helpers ====

    private String generated(Map<String, String> output, String path) {
        assertNotNull(output, "cell output not generated");
        String gen = output.get(path);
        assertNotNull(gen, "missing generated output: " + path);
        return gen;
    }

    /** Occurrence count (python str.count semantics — the #352 law, NOT line count). */
    private static int count(String haystack, String needle) {
        int n = 0;
        for (int at = haystack.indexOf(needle); at >= 0; at = haystack.indexOf(needle, at + needle.length())) {
            n++;
        }
        return n;
    }

    private void assertByteMatchesGolden(Map<String, String> output, Path goldenDir, String path)
            throws IOException {
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "missing golden: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated(output, path)),
                path + " must byte-match the frozen 9.83.0 golden");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
