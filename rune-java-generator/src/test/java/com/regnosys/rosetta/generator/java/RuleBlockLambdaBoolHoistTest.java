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
 * PR #260 — facet {@code ruleBlockLambdaBoolHoist} (rule-body emission, M7b-3; the modest clean
 * bundle, facet A).
 *
 * <p>A bare boolean FUNCTION-call condition ({@code <fn>.evaluate(item.get())}) consumed in a
 * rule-body map-lambda conditional ({@code mapSingleToItem(item -> { if (<cond>) … })}) compiles
 * ITEM-typed Boolean — {@code RosettaFunction.evaluate(...)} returns the raw {@code Boolean} output,
 * not a {@code Mapper}. The fork rendered the inline {@code <cond>.getOrDefault(false)} form, which
 * calls a {@code Mapper} method on a raw {@code Boolean} = non-compiling. The golden (upstream
 * rune-dsl 9.83.0) instead HOISTS the call via the {@code convertNullSafe} statement form:
 * <pre>
 * item -&gt; {
 *     final Boolean _boolean = &lt;fn&gt;.evaluate(item.get());
 *     if ((_boolean == null ? false : _boolean)) {
 *         return &lt;then&gt;;
 *     }
 *     return &lt;else / MapperS.&lt;T&gt;ofNull()&gt;;
 * }
 * </pre>
 * This is the #179/#217 {@code boolean_condition_hoist} law at the rule-body block-lambda
 * conditional seat — the in-lambda analogue handled by {@code CollectionHandler}'s
 * {@code compileElselessConditionalBlock} (the elseless {@code MapperS.<T>ofNull()} form) and
 * {@code compileEffectiveElseConditionalBlock} (the present-else form). The {@code _boolean}
 * singleton name is per-lambda-scope (the fresh map-lambda scope holds only the bound {@code item}),
 * so no collision/numbering is needed. The AST gate is the shared
 * {@code HandlerHelper.isBareFunctionCallCondition} (an {@code RSymbolReference} resolving to an
 * {@code RFunction}); every other condition shape (ComparisonResult / {@code exists} / {@code
 * areEqual} / Mapper chains) keeps the inline {@code .getOrDefault(false)}.
 *
 * <p><b>Green-safe by construction</b> (corpus-verified, frozen 9.83.0 baseline): the inline
 * {@code <rawBoolean>.getOrDefault(false)} form never compiled, so every carrier is an
 * already-waivered (non-compiling) mismatch — the hoist can only flip a waivered file. A
 * non-bare-function-call block conditional (the {@code areEqual} / {@code exists} green population)
 * keeps the inline form — see the green-safety locks below.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr POJO output (Rule kind) against
 * the frozen goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleBlockLambdaBoolHoistTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR = DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> drrPojoOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generatePojo() throws IOException {
        if (drrCellAvailable()) {
            drrPojoOutput = generatePojoCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    /**
     * Generate the drr POJO cell (Rule/Report/LabelProvider included), mirroring
     * {@link D11CorpusRegressionTest#pojo_comparison}'s generator wiring — the rule-body Rule
     * classes this facet touches are emitted by {@link RuleGenerator} (which delegates to
     * {@link FunctionGenerator}, the shared expression compiler).
     */
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
                // Rule-family generators are still converging on body emission (M7b-3); a failure
                // for an already-waivered element is tolerated debt. The anchored carriers below
                // MUST emit + byte-match, so a swallowed failure surfaces as a null lookup in
                // assertByteMatchesGolden.
                assertNoGenerationErrors(ruleGen.generateClasses(model, version, output));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
                assertNoGenerationErrors(labelProviderGen.generateClasses(model, version, output));
            }
        }
        return output;
    }

    // ---- Flip locks (revert-RED): a bare-boolean-FUNCTION-call block-lambda conditional.

    /**
     * Elseless form ({@code compileElselessConditionalBlock}): {@code mapSingleToItem(item -> { if
     * (isReturnorPayoutTriggerCFD.evaluate(item.get())) … return MapperS.<String>ofNull(); })} —
     * the bare boolean fn call hoists to {@code final Boolean _boolean = …;} + the null-guard.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void returnorPayoutTriggerRuleCsa_elselessBoolHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/csa/rewrite/trade/reports/ReturnorPayoutTriggerRule.java");
    }

    /**
     * Present-else form ({@code compileEffectiveElseConditionalBlock}): {@code mapSingleToItem(item
     * -> { if (isFRA.evaluate(item.get())) return …Leg1; return …Leg2; })} — the bare boolean fn
     * call hoists; the real else (interestRateLeg2) is preserved.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void fixedRateDayCountConventionLeg2RuleEsma_effectiveElseBoolHoist_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/esma/emir/refit/trade/reports/FixedRateDayCountConventionLeg2Rule.java");
    }

    /** Present-else form, cross-region: fca. */
    @Test
    @EnabledIf("drrCellAvailable")
    void fixedRateDayCountConventionLeg2RuleFca_effectiveElseBoolHoist_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/fca/ukemir/refit/trade/reports/FixedRateDayCountConventionLeg2Rule.java");
    }

    /** Present-else form (PaymentFrequencyPeriod variant): esma. */
    @Test
    @EnabledIf("drrCellAvailable")
    void fixedRatePaymentFrequencyPeriodLeg2RuleEsma_effectiveElseBoolHoist_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/esma/emir/refit/trade/reports/FixedRatePaymentFrequencyPeriodLeg2Rule.java");
    }

    /** Present-else form (PaymentFrequencyPeriod variant): fca. */
    @Test
    @EnabledIf("drrCellAvailable")
    void fixedRatePaymentFrequencyPeriodLeg2RuleFca_effectiveElseBoolHoist_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/fca/ukemir/refit/trade/reports/FixedRatePaymentFrequencyPeriodLeg2Rule.java");
    }

    /** Present-else form (PaymentFrequencyPeriodMultiplier variant): esma. */
    @Test
    @EnabledIf("drrCellAvailable")
    void fixedRatePaymentFrequencyPeriodMultiplierLeg2RuleEsma_effectiveElseBoolHoist_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/esma/emir/refit/trade/reports/FixedRatePaymentFrequencyPeriodMultiplierLeg2Rule.java");
    }

    /** Present-else form (PaymentFrequencyPeriodMultiplier variant): fca. */
    @Test
    @EnabledIf("drrCellAvailable")
    void fixedRatePaymentFrequencyPeriodMultiplierLeg2RuleFca_effectiveElseBoolHoist_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/fca/ukemir/refit/trade/reports/FixedRatePaymentFrequencyPeriodMultiplierLeg2Rule.java");
    }

    // ---- Green-safety locks: forms the fix must NOT perturb.

    /**
     * Green-safety lock (non-bare-function-call block conditional): {@code UpiPostEnrichmentDataRule}
     * is a GREEN drr Rule whose {@code mapSingleToItem(item -> { … })} block conditional has an
     * {@code areEqual(…)} condition (a ComparisonResult, NOT a bare {@code RFunction} call). The
     * {@code isBareFunctionCallCondition} gate declines, so it keeps the inline
     * {@code .getOrDefault(false)} form — byte-identical. Confirms the hoist fires ONLY on bare
     * boolean function-call conditions.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void upiPostEnrichmentDataRule_nonBareFnBlockConditional_staysInline() throws IOException {
        assertByteMatchesGolden("drr/enrichment/common/reports/UpiPostEnrichmentDataRule.java");
    }

    /** Green-safety lock (non-bare-function-call block conditional), sibling: pre-enrichment. */
    @Test
    @EnabledIf("drrCellAvailable")
    void upiPreEnrichmentDataRule_nonBareFnBlockConditional_staysInline() throws IOException {
        assertByteMatchesGolden("drr/enrichment/common/reports/UpiPreEnrichmentDataRule.java");
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(drrPojoOutput, "drr POJO generation did not run — corpus unavailable?");
        String generated = drrPojoOutput.get(path);
        assertNotNull(generated, "Rule class not generated: " + path
                + " (RuleGenerator emission failed or the path differs)");
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated drr Rule output must byte-match the golden (newline-normalized) for "
                + path + " (facet ruleBlockLambdaBoolHoist, PR #260).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
