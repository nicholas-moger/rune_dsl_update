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
 * PR #261 — two disjoint, green-safe rule-body facets (M7b-3; the "ship the clean bundle" choice
 * after the big rule-body root-cause lever was measured to fragment with no clean step-change).
 *
 * <p><b>Facet A — {@code topLevelBareFunctionInput} (3 flips).</b> A bare no-args FUNCTION
 * invocation at rule-body TOP LEVEL (outside any extract/filter/then lambda) was rendered
 * {@code <fn>.evaluate(item.get())} — but {@code item} is undefined at rule-body top level (it is
 * the lambda binding), so the form never compiled. The golden (upstream rune-dsl 9.83.0) passes the
 * rule's {@code input} parameter: {@code <fn>.evaluate(input)}.
 * {@code ReferenceHandler.renderImplicitFunctionInvocation} now mirrors the bare-RULE sibling
 * {@code renderImplicitRuleInvocation}: when there is an enclosing {@link
 * com.regnosys.rosetta.ast.functions.RRule} with a from-type AND no enclosing inline lambda, it
 * builds the synthetic {@code input} receiver; INSIDE a lambda it keeps {@code item.get()}, and a
 * plain FUNCTION body (no enclosing rule) keeps the synthetic {@code item} path — byte-neutral for
 * the FUNCTION tail. Carriers: OptionPremiumAmount (common) + IsCSAAligned (csa) +
 * FinalContractualSettlementDate (iosco/cde/version1/execution).
 *
 * <p><b>Facet B — {@code boolHoistValueUnwrap} (8 flips).</b> The #260 {@code ruleBlockLambdaBoolHoist}
 * hoist (a bare boolean FUNCTION-call condition compiles to {@code final Boolean _boolean = <call>;}
 * + the {@code (_boolean == null ? false : _boolean)} null-guard) rendered the value WRAPPED
 * ({@code final Boolean _boolean = MapperS.of(<call>)}, a {@code MapperS<Boolean>} assigned to a raw
 * {@code Boolean} = non-compiling) for an EXPLICIT-args condition ({@code isCleared(originatingWorkflowStep)},
 * rendered through the general compile path which coerces it to {@code MapperS.of(...)}). The #260
 * carriers were IMPLICIT calls rendered UNWRAPPED by {@code renderImplicitFunctionInvocation}, so
 * their value was already bare. {@code CollectionHandler.boolHoistValue} now strips a
 * whole-expression {@code MapperS.of(...)} wrap from the hoist value via
 * {@code HandlerHelper.unwrapMapperSOf} (a no-op on the already-bare implicit form), so BOTH the
 * implicit (#260) and explicit (this facet) carriers emit the bare {@code <call>}. Carriers: Cleared
 * (asic) + CDSIndex{Attachment,Detachment}Point (csa) + CryptoAssetUnderlyingIndicatorLeg1 (csa) +
 * ClearingObligation (esma) + FinalContractualSettlementDate (esma + fca) + EarlyTerminationDate
 * (iosco/cde/version1/datetime).
 *
 * <p><b>Both green-safe by construction</b> (corpus-verified, frozen 9.83.0 baseline): {@code item}
 * at rule-body top level and a {@code MapperS<Boolean>}-to-{@code Boolean} assignment both never
 * compiled, so every carrier was an already-waivered (non-compiling) mismatch — neither facet can
 * touch a green file. The green-safety locks below pin the in-lambda + implicit-call forms the fixes
 * must NOT perturb.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr POJO output (Rule kind) against
 * the frozen goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleTopLevelInputBoolHoistTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

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
     * classes these facets touch are emitted by {@link RuleGenerator} (which delegates to
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
                assertNoGenerationErrors(ruleGen.generateClasses(model, version, output));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
                assertNoGenerationErrors(labelProviderGen.generateClasses(model, version, output));
            }
        }
        return output;
    }

    // ==== Facet A flip locks (revert-RED): top-level bare-FUNCTION input arg. ====

    /**
     * Top-level {@code if (areEqual(MapperS.of(getContractType.evaluate(input)), …))} — the
     * bare-FUNCTION call at {@code assignOutput} top level passes the rule {@code input} (the
     * sibling bare-RULE {@code optionPremiumAmountRule.evaluate(input)} already did so).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void optionPremiumAmountRuleCommon_topLevelInput_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/common/trade/price/reports/OptionPremiumAmountRule.java");
    }

    /** Top-level multi-condition ladder ({@code isCSALeg1Aligned.evaluate(input)} …): csa. */
    @Test
    @EnabledIf("drrCellAvailable")
    void isCsaAlignedRuleCsa_topLevelInput_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/csa/rewrite/trade/reports/IsCSAAlignedRule.java");
    }

    /** Top-level bare-FUNCTION input arg, iosco/cde standard. */
    @Test
    @EnabledIf("drrCellAvailable")
    void finalContractualSettlementDateRuleCde_topLevelInput_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version1/execution/reports/FinalContractualSettlementDateRule.java");
    }

    // ==== Facet B flip locks (revert-RED): boolHoist value unwrap (explicit-args condition). ====

    /**
     * Elseless block-lambda conditional, nav-chain arg:
     * {@code final Boolean _boolean = isCleared.evaluate(item.<WorkflowStep>map(…).get());} — the
     * general-path {@code MapperS.of(...)} wrap is stripped so the {@code final Boolean} decl holds
     * the bare call.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void clearedRuleAsic_boolHoistUnwrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/ClearedRule.java");
    }

    /** Nested-call arg ({@code isCreditDefaultSwap.evaluate(productForEvent.evaluate(item.get()))}): csa. */
    @Test
    @EnabledIf("drrCellAvailable")
    void cdsIndexAttachmentPointRuleCsa_boolHoistUnwrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/csa/rewrite/trade/reports/CDSIndexAttachmentPointRule.java");
    }

    /** Nested-call arg, detachment-point sibling: csa. */
    @Test
    @EnabledIf("drrCellAvailable")
    void cdsIndexDetachmentPointRuleCsa_boolHoistUnwrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/csa/rewrite/trade/reports/CDSIndexDetachmentPointRule.java");
    }

    /** Deeply-nested-call arg ({@code qualify_AssetClass_Commodity.evaluate(…)}): csa. */
    @Test
    @EnabledIf("drrCellAvailable")
    void cryptoAssetUnderlyingIndicatorLeg1RuleCsa_boolHoistUnwrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/csa/rewrite/trade/reports/CryptoAssetUnderlyingIndicatorLeg1Rule.java");
    }

    /** Nav-chain arg, esma sibling of ClearedRule. */
    @Test
    @EnabledIf("drrCellAvailable")
    void clearingObligationRuleEsma_boolHoistUnwrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/trade/reports/ClearingObligationRule.java");
    }

    /** Nested-call arg ({@code isFRA.evaluate(productForEvent.evaluate(item.get()))}): esma. */
    @Test
    @EnabledIf("drrCellAvailable")
    void finalContractualSettlementDateRuleEsma_boolHoistUnwrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/esma/emir/refit/trade/reports/FinalContractualSettlementDateRule.java");
    }

    /** Nested-call arg, fca sibling. */
    @Test
    @EnabledIf("drrCellAvailable")
    void finalContractualSettlementDateRuleFca_boolHoistUnwrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/fca/ukemir/refit/trade/reports/FinalContractualSettlementDateRule.java");
    }

    /** Nav-chain arg, iosco/cde datetime standard. */
    @Test
    @EnabledIf("drrCellAvailable")
    void earlyTerminationDateRuleCde_boolHoistUnwrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version1/datetime/reports/EarlyTerminationDateRule.java");
    }

    // ==== Green-safety locks: forms NEITHER fix may perturb. ====

    /**
     * Dual green-safety lock: {@code ReturnorPayoutTriggerRule} (a #260 boolHoist carrier, now GREEN)
     * has a bare boolean function-call condition INSIDE a {@code mapSingleToItem(item -> { … })}
     * lambda where the call is IMPLICIT ({@code isReturnorPayoutTriggerCFD.evaluate(item.get())}).
     * It pins BOTH facets at once: facet A must NOT fire in-lambda (the call keeps {@code item.get()},
     * not {@code input}, because {@code nearestEnclosingInlineFunction != null}), and facet B's
     * {@code unwrapMapperSOf} must be a no-op on the already-bare implicit value. (This file regressed
     * under an earlier {@code unwrapForEvaluateArg}-based attempt that appended {@code .get()} to the
     * bare value — proving the lock's sensitivity.)
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void returnorPayoutTriggerRuleCsa_inLambdaImplicitBoolHoist_staysBare() throws IOException {
        assertByteMatchesGolden("drr/regulation/csa/rewrite/trade/reports/ReturnorPayoutTriggerRule.java");
    }

    /**
     * Green-safety lock (non-bare-function-call block conditional): {@code UpiPostEnrichmentDataRule}
     * is a GREEN drr Rule whose block conditional has an {@code areEqual(…)} (ComparisonResult)
     * condition — {@code isBareFunctionCallCondition} declines, so the boolHoist (and its value
     * unwrap) never fires; byte-identical.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void upiPostEnrichmentDataRule_nonBareFnBlockConditional_staysInline() throws IOException {
        assertByteMatchesGolden("drr/enrichment/common/reports/UpiPostEnrichmentDataRule.java");
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
                + path + " (PR #261 topLevelBareFunctionInput + boolHoistValueUnwrap).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
