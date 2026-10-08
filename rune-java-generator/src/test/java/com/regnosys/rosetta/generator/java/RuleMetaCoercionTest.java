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
 * PR #285 — facet metaCoercionRule: TWO disjoint, green-safe, RULE-scoped (FUNCTION-byte-neutral)
 * convertNullSafe meta-coercion mechanisms at distinct drr rule-body seats, 4 drr POJO Rule byte
 * flips. The user-chosen "ship the clean meta tail" after the byte-oracle REFUTED the canon-census's
 * ~25 estimate (the receiver-deref bulk, ~20 files, needs a #204/#282-class disguised-chain
 * meta-recovery FOUNDATION — {@code getInferredType} drops the meta to the bare value and
 * {@code terminalNavAttr} cannot descend the 2-name disguised chain {@code reportingSide ->
 * reportingCounterparty}; deferred). Only these two seats recover cleanly.
 *
 * <p><b>(A) whole-output meta-unwrap (wou, 2 files).</b> A rule whose body DELEGATES to a sub-rule
 * via an {@code extract}/{@code then extract <innerRule>} whose inner rule output is META-typed
 * ({@code [metadata scheme]} …) but whose Java {@code evaluate()} returns the bare value. The #265
 * lambda wrap ALREADY rendered the {@code FieldWithMetaX} wrap INSIDE the {@code .mapSingleToItem}
 * block (gen == golden there), but the whole-output assignment stayed the bare
 * {@code output = <RHS>.get();}; golden hoists the wrapper local + null-guards the {@code .getValue()}
 * deref. {@code FunctionExpressionRenderer.renderMetaValueDerefOrNull} gains a sub-rule-delegation
 * recovery ({@code terminalDelegatedRule} + {@code NavigationHandler.recoverInnerRuleMetaWrapper})
 * after the compiled-type + navigation-terminal recoveries decline (a bare rule ref is neither). The
 * recovery requires the inner rule be reached THROUGH an {@code extract}/{@code then} (the #265-wrap
 * context) — a DIRECT bare delegation ({@code output = <innerRule>.evaluate(input)}, e.g.
 * CollateralPortfolioCode v2) renders with NO #265 wrap and MUST NOT be unwrapped. Carriers:
 * Counterparty2Name (asic, FieldWithMetaString), DTCC_SecondaryAssetClass (csa,
 * FieldWithMetaAssetClassEnum).
 *
 * <p><b>(B) lambda-channel evaluate-arg meta-deref (lcu, 2 files, the #267-deferred LAMBDA_CHANNEL
 * lead).</b> A rule-body {@code .mapSingleToItem(item -> MapperS.of(<fn>.evaluate(item.get(), …)))}
 * lambda whose implicit {@code item} is a {@code ReferenceWithMetaX} wrapper passed where the callee
 * wants the bare value: golden converts the lambda to a block hoisting {@code final <Wrapper> w =
 * item.get();} and derefs the arg {@code (w == null ? null : w.getValue())}. The implicit item's
 * compiled type erases to null, so {@code ReferenceHandler.tryMetaDerefArg}'s compiled-type +
 * alias/terminal fallbacks decline; {@code NavigationHandler.implicitItemArgMeta} recovers the
 * wrapper from the owning then-chain's terminal nav (the same machinery the receiver-coercion path
 * uses), then the existing LAMBDA_CHANNEL route ({@code registerPendingLambdaHoist} →
 * {@code CollectionHandler.compileLambda}) converts the lambda to a block. Carriers (iosco cde v1,
 * ReferenceWithMetaParty): OtherPaymentPayer, OtherPaymentReceiver.
 *
 * <p><b>Green-safe by construction.</b> Both fork forms — {@code output = <FieldWithMetaX>} (a wrapper
 * into a bare-value output) and {@code <fn>.evaluate(item.get())} on a wrapper item — do not compile,
 * so every carrier was an already-waivered NON_COMPILING mismatch. byte-oracle / stash-baseline
 * measured exactly 4 flips (drr POJO 472 → 468; clean-main now-matching 12 stale, with-fix 16 = 4
 * mine + 12 stale); 0 within-waiver regressions; all FUNCTION cells (cdm5 81 / cdm6 236 / drr 207) +
 * cdm/iso/fpml POJO byte-IDENTICAL (the wou recovery is RRule-delegation-only — a FUNCTION body
 * delegates to an RFunction, never an RRule — and the lcu recovery is {@code findEnclosingRule}-gated).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleMetaCoercionTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> drrOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            drrOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
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

    // ==== (A) whole-output meta-unwrap (wou) flip locks (revert-RED). ====

    /**
     * Flip — Counterparty2Name (asic) delegates {@code extract common.party.Counterparty2Name}; the
     * #265 wrap produces a {@code FieldWithMetaString} inside the lambda, and golden hoists the
     * wrapper + null-guards {@code output = fieldWithMetaString.getValue();}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty2Name_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/Counterparty2NameRule.java");
    }

    /** Flip — DTCC_SecondaryAssetClass (csa): the FieldWithMetaAssetClassEnum whole-output unwrap. */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccSecondaryAssetClass_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/csa/rewrite/dtcc/reports/DTCC_SecondaryAssetClassRule.java");
    }

    // ==== (B) lambda-channel evaluate-arg meta-deref (lcu) flip locks (revert-RED). ====

    /**
     * Flip — OtherPaymentReceiver (iosco cde v1): {@code partyLeiAndPersonByRoles.evaluate(item.get(),
     * null)} on a {@code ReferenceWithMetaParty} item; golden block-hoists {@code final
     * ReferenceWithMetaParty referenceWithMetaParty = item.get();} + derefs the evaluate-arg.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void otherPaymentReceiver_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/standards/iosco/cde/version1/payment/reports/OtherPaymentReceiverRule.java");
    }

    /** Flip — the payer-side sibling of {@code OtherPaymentReceiverRule}. */
    @Test
    @EnabledIf("drrCellAvailable")
    void otherPaymentPayer_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/standards/iosco/cde/version1/payment/reports/OtherPaymentPayerRule.java");
    }

    // ==== Facet-boundary / green-safety locks (revert-RED). ====

    /**
     * Green-safety lock (wou) — the {@code sawExtractOrThen} gate. CollateralPortfolioCode v2
     * delegates DIRECTLY ({@code output = collateralPortfolioCodeRule.evaluate(input);}, no
     * {@code extract}) so NO #265 wrap is applied and the output is already byte-correct: the
     * whole-output unwrap MUST NOT fire. Locks that {@code terminalDelegatedRule} declines a bare
     * delegation (an earlier ungated version regressed 10 such green files). The file is GREEN
     * (gen == golden), so a byte-match is the lock.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void collateralPortfolioCode_v2_directDelegation_noOverFire() throws IOException {
        String path = "drr/standards/iosco/cde/version2/collateral/reports/CollateralPortfolioCodeRule.java";
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String gen = drrOutput.get(path);
        assertNotNull(gen, "CollateralPortfolioCodeRule v2 not generated");
        assertTrue(gen.contains("output = collateralPortfolioCodeRule.evaluate(input);"),
                "the direct bare delegation must keep its byte-correct form (no whole-output unwrap)");
        assertByteMatchesGolden(path);
    }

    // Facet-boundary lock (lcu) REMOVED at PR #336. QuantityUnitOfMeasureLeg2 (csa) — the SAME lcu
    // evaluate-arg shape (quantityUnitOfMeasureRule.evaluate(item.get()) on a
    // ReferenceWithMetaNonNegativeQuantitySchedule item) — was DEFERRED here because its then-terminal
    // is a CONDITIONAL (then extract [if IsCommoditySwap … else …]), so implicitItemArgMeta's
    // recoverMetaFromExpr walker STOPPED (no clean nav terminal) and the deref declined. PR #336's
    // ruleCalleeMetaDeref does NOT depend on recoverMetaFromExpr — it types the piped item via
    // enclosingThenArgType and restores the rule's from-type on the synthetic RFunction.fromRule
    // callee, so the deref fires regardless of the conditional terminal (the deeper block_lambda
    // recovery this lock referenced is no longer the blocker). The flip is now locked by
    // RuleCalleeMetaDerefTest.

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String generated = drrOutput.get(path);
        assertNotNull(generated, "Class not generated: " + path
                + " (emission failed or the path differs)");
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated drr output must byte-match the golden (newline-normalized) for "
                + path + " (PR #285 metaCoercionRule).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
