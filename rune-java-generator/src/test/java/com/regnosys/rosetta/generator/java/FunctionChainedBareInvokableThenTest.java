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
 * PR #254 — facet {@code chained_bare_invokable_then} (rule-body emission, M7b-3 first slice).
 *
 * <p>A CHAINED bare-invokable then-chain on the drr Rule path — a report rule whose body is
 * {@code filter … then <rule1> then <rule2>} (or {@code … then <function> then <rule>}) —
 * declined to the non-compiling inline runtime {@code .then(} form. There is no runtime
 * {@code Mapper.then(Function)} method, so every such rule was a waivered
 * {@code CODEGEN_BODY_GAP} mismatch. The single-level bare-invokable then
 * ({@code filter … then <rule>}) was already handled by
 * {@code FunctionExpressionRenderer.renderBareInvokableThenSet} (engine PR #6/#13); the CHAINED
 * form was an explicitly-deferred facet (flagged at {@code isImplicitThenChain}).
 *
 * <p><b>Fix (GENERATOR-only, 2 source files):</b>
 * <ul>
 *   <li>{@code FunctionExpressionRenderer.isHoistableThenChain} — a strict SUPERSET of
 *       {@code isImplicitThenChain} used at the top-level SET then-dispatch: it ALSO admits a
 *       chain containing a bare-RULE body on the rule path, so the chain routes through
 *       {@code renderThenExtractSet}'s hoisted {@code final Mapper*<X> thenArgN = …;} form. The
 *       bare-RULE admission requires the rule emission path (the {@code <Name>Rule} receiver
 *       field is injected only on rule emission); a bare-FUNCTION then-body is DEFERRED entirely
 *       (a scalar function is not a wrap-factory the chained renderer can yet distinguish).</li>
 *   <li>{@code FunctionExpressionRenderer.isInvocationWrapFactory} — extended to recognise a
 *       bare-RULE reference, not just a function-invocation: a bare rule/function then-OUTPUT
 *       compiles to the same {@code MapperS.of(receiver.evaluate(…))} wrap-factory, and the
 *       prior RFunction-only gate left a bare-RULE output to the generic
 *       {@code unwrapForAssignment} which STRUCTURALLY STRIPPED the wrap to the non-compiling
 *       bare {@code output = receiver.evaluate(…)}. Keeping the wrap + appending {@code .get()}
 *       matches golden ({@code output = MapperS.of(callCurrencyRule.evaluate(thenArg1.get()))
 *       .get()}).</li>
 *   <li>{@code CollectionHandler.isBareRuleBody} — the gateless bare-RULE body predicate the
 *       rule-path safety gate uses.</li>
 * </ul>
 *
 * <p><b>Green-safe by construction:</b> zero of the 34,686 corpus-9.83.0 goldens carry the
 * runtime {@code .then(} form, so every file emitting the inline chain is already a
 * non-compiling, already-waivered mismatch — admitting it to the hoist can only move waivered
 * output toward golden. A green file cannot carry the inline form (it does not compile). 23
 * drr POJO Rule files flip (byte-oracle + stash-baseline: 35 now-matching − 12 pre-existing
 * stale = 23 mine); the full all-kinds D11 stays 20/20 with zero regression.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr POJO output (Rule kind)
 * against the frozen goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class FunctionChainedBareInvokableThenTest {

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
     * classes the chained-bare-invokable-then facet touches are emitted by
     * {@link RuleGenerator} (which delegates to {@link FunctionGenerator}).
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
                // Rule-family generators are still converging on body emission (M7b-3); a
                // failure for an already-waivered element is tolerated debt. The anchored
                // carriers below MUST emit + byte-match, so a swallowed failure surfaces as a
                // null lookup in assertByteMatchesGolden.
                assertNoGenerationErrors(ruleGen.generateClasses(model, version, output));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
                assertNoGenerationErrors(labelProviderGen.generateClasses(model, version, output));
            }
        }
        return output;
    }

    // ---- Flip locks (revert-RED): chained-bare-invokable then-chains, one per shape/region.

    /** asic: {@code filter … then <filter> then <rule>} — bare-RULE output wrap (callCurrencyRule). */
    @Test
    @EnabledIf("drrCellAvailable")
    void callCurrencyRuleAsic_chainedBareRuleThen_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/CallCurrencyRule.java");
    }

    /** asic: {@code filter … then <function-extract-chain> then <rule>} (FixedRateLeg1). */
    @Test
    @EnabledIf("drrCellAvailable")
    void fixedRateLeg1RuleAsic_chainedExtractThenBareRule_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/FixedRateLeg1Rule.java");
    }

    /** asic: chained then whose last bare-rule output sits beside a getOrDefault default chain. */
    @Test
    @EnabledIf("drrCellAvailable")
    void optionPremiumCurrencyRuleAsic_chainedBareRuleThen_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/OptionPremiumCurrencyRule.java");
    }

    /** asic: PutCurrency — the put-side sibling of the call-currency chained shape. */
    @Test
    @EnabledIf("drrCellAvailable")
    void putCurrencyRuleAsic_chainedBareRuleThen_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/PutCurrencyRule.java");
    }

    /** asic: FloatingRateReferencePeriodLeg2 — a different chained-then leaf shape. */
    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRateReferencePeriodLeg2RuleAsic_chainedBareRuleThen_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/asic/rewrite/trade/reports/FloatingRateReferencePeriodLeg2Rule.java");
    }

    /** Cross-region: the SAME mechanism fires in the hkma rewrite (not asic-specific). */
    @Test
    @EnabledIf("drrCellAvailable")
    void fixedRateLeg1RuleHkma_chainedBareRuleThen_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/hkma/rewrite/trade/reports/FixedRateLeg1Rule.java");
    }

    /** Cross-region: the mas rewrite FirstExerciseDate chained-then. */
    @Test
    @EnabledIf("drrCellAvailable")
    void firstExerciseDateRuleMas_chainedBareRuleThen_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/mas/rewrite/trade/reports/FirstExerciseDateRule.java");
    }

    // ---- Green-safety lock: an EXISTING green chained-then must stay byte-identical.

    /**
     * Green-safety lock: {@code CallAmountRule} (asic) is a GREEN chained-then whose output is an
     * explicit-args FUNCTION invocation ({@code extractCallAmount.evaluate(thenArg1.get(), …)}).
     * It exercises (a) {@code renderThenExtractSet}'s EXISTING {@code isImplicitThenChain} path
     * and (b) {@code isInvocationWrapFactory}'s RFunction case — both must be byte-unchanged by
     * the {@code isHoistableThenChain} SUPERSET relaxation + the bare-RULE extension. If either
     * regressed, this green file would diverge.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void callAmountRuleAsic_greenChainedFunctionThen_staysByteIdentical() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/CallAmountRule.java");
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
                + path + " — the chained-bare-invokable then-hoist (isHoistableThenChain admission "
                + "+ the bare-RULE isInvocationWrapFactory output wrap) is missing or regressed.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
