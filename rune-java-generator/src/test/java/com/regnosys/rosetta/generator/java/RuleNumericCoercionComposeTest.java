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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #300 — numericCoercionCompose: THREE disjoint green-safe GENERATOR mechanisms coercing a number
 * to {@code BigDecimal} at a consumption site (the #276/#277 numeric-coercion family at NEW seats),
 * 5 drr POJO Rule byte flips.
 *
 * <p><b>(M1) nav-chain Integer&rarr;BigDecimal null-safe hoist (2 flips).</b> A NULLABLE Integer
 * nav-chain arg compiled INLINE ({@code …<Integer>map("getPeriodMultiplier", …).get()}) into a
 * {@code BigDecimal}-expecting param surfaces a NULL expression type, so the #277
 * {@code integerToBigDecimal} arm could not fire (#277 only reached carriers whose Integer came via a
 * declared {@code MapperS<Integer> thenArg} local, whose {@code .get()} surfaces the type).
 * {@code ReferenceHandler.tryMetaDerefArg} now recovers the bare numeric item type from the AST
 * inferred type (the same type the chain's {@code <Integer>map} witness derives from), so the hoist
 * {@code final Integer integer = <chain>.get();} + coercion
 * {@code (integer == null ? null : BigDecimal.valueOf(integer))} fires. Gated to DECLINE a conditional
 * -block-lambda body (the LAMBDA_CHANNEL route, where registering a pending lambda hoist would re-render
 * the inner conditional as an inline ternary — moving the carrier AWAY from golden). Carriers:
 * FloatingRateResetFrequencyMultiplierOfLeg1 (esma, a then-body thenArg-decl) and
 * PaymentFrequencyPeriodMultiplierAdjusted (iosco, an expr-lambda whose body IS the call) — both the
 * BLOCK route.
 *
 * <p><b>(M2) literal Integer&rarr;BigDecimal (1 flip).</b> A LITERAL within-long int arg into a
 * {@code BigDecimal} param (IndexFactor's {@code formatToBaseOne18Rate.evaluate(1)}) — golden renders
 * the INLINE {@code BigDecimal.valueOf(1)} (a literal is provably non-null, so NO hoist / NO null-guard;
 * #277 deliberately EXCLUDED {@code RIntLiteral} from {@code integerToBigDecimal} because the
 * hoisted-and-guarded form diverges from this inline literal form). The fork passed the bare {@code 1}
 * (an int into a {@code BigDecimal} param &mdash; non-compiling).
 *
 * <p><b>(M3) BigInteger-literal&rarr;BigDecimal ite-hoist (2 flips).</b> A beyond-long
 * {@code number} literal arm value ({@code MapperS.of(new BigInteger("9999999999999999999999999"))}) at
 * a single {@code MapperS<BigDecimal>} ite-hoist arm (OptionPremiumAmount esma/fca) — golden coerces it
 * null-safe: {@code FunctionExpressionRenderer.coerceBigIntegerLiteralIteArm} hoists
 * {@code final BigInteger bigInteger = new BigInteger("…");} into the branch (the #276
 * {@code bigIntHoistDrain} line order) + renders the arm
 * {@code bigInteger == null ? MapperS.<BigDecimal>ofNull() : MapperS.of(new BigDecimal(bigInteger))}.
 * The fork spliced the bare {@code MapperS<BigInteger>} literal into the {@code MapperS<BigDecimal>}
 * {@code ifThenElseResult} (non-compiling). This is the ARM-VALUE sibling of the #276 drain (whose
 * BigInteger came from the evaluate-ARG path).
 *
 * <p><b>Green-safety (regscan: 5 flipped-out / 0 within-waiver regressions / 4 toward-golden /
 * 2 neutral; ALL FUNCTION cells + cdm/iso/fpml POJO byte-IDENTICAL).</b> All 5 carriers were
 * NON_COMPILING (the bare int / bare {@code MapperS<BigInteger>} forms never compiled), so green-safe
 * by construction. The M1 conditional-block-lambda gate drove a first un-gated impl's regression on
 * FloatingRateResetFrequency*OfLeg2 / *PeriodOfLeg{1,2} (esma) to 0 — the REGSCAN, not the byte-oracle
 * (5 flips either way), caught it. byte-oracle / stash-baseline measured exactly 5 flips (drr POJO
 * 391 &rarr; 386; clean source = 12 stale waivers, {@code comm -23} = exactly these 5 carriers).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr Rule output against the frozen
 * goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED for the 5 flip locks +
 * the positive-content lock.
 */
class RuleNumericCoercionComposeTest {

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

    // ==== (M1) nav-chain Integer->BigDecimal hoist flip locks (revert-RED) ====

    /**
     * Flip: {@code FloatingRateResetFrequencyMultiplierOfLeg1} (esma) — the periodMultiplier is
     * navigated INLINE as the 2nd {@code adjustPeriodMultiplier.evaluate(…, <chain>)} arg at a
     * {@code thenArg6} decl (the BLOCK route); golden hoists {@code final Integer integer = <chain>.get();}
     * + {@code (integer == null ? null : BigDecimal.valueOf(integer))}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRateResetFrequencyMultiplierOfLeg1Esma_navChainHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/esma/emir/refit/trade/reports/FloatingRateResetFrequencyMultiplierOfLeg1Rule.java");
    }

    /**
     * Flip: {@code PaymentFrequencyPeriodMultiplierAdjusted} (iosco cde) — an expr-lambda whose body IS
     * the {@code adjustPeriodMultiplier(…)} call; the BLOCK route converts it to a block lambda with the
     * Integer hoist at the lambda top (golden's shape, no inner conditional).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void paymentFrequencyPeriodMultiplierAdjustedIosco_navChainHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version1/payment/reports/PaymentFrequencyPeriodMultiplierAdjustedRule.java");
    }

    // ==== (M2) literal Integer->BigDecimal flip lock (revert-RED) ====

    /** Flip: {@code IndexFactor} (common) — {@code formatToBaseOne18Rate.evaluate(1)} &rarr;
     *  {@code evaluate(BigDecimal.valueOf(1))} (the #277-excluded inline literal case). */
    @Test
    @EnabledIf("drrCellAvailable")
    void indexFactorCommon_literalBigDecimalValueOf_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/trade/index/reports/IndexFactorRule.java");
    }

    // ==== (M3) BigInteger-literal->BigDecimal ite-hoist flip locks (revert-RED) ====

    /** Flip: {@code OptionPremiumAmount} (esma) — a {@code MapperS.of(new BigInteger("…"))} ite-hoist
     *  arm coerced null-safe to {@code MapperS<BigDecimal>}. */
    @Test
    @EnabledIf("drrCellAvailable")
    void optionPremiumAmountEsma_bigIntegerLiteralIteArm_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/esma/emir/refit/trade/reports/OptionPremiumAmountRule.java");
    }

    /** Flip: {@code OptionPremiumAmount} (fca) — the same BigInteger-literal ite-hoist arm, FCA regime. */
    @Test
    @EnabledIf("drrCellAvailable")
    void optionPremiumAmountFca_bigIntegerLiteralIteArm_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/fca/ukemir/refit/trade/reports/OptionPremiumAmountRule.java");
    }

    // ==== Green-safety / decline + positive-content locks ====

    /**
     * CROSS-PR LIFT (was the #300 conditional-block-lambda DECLINE lock; CONVERTED to a byte-match at
     * PR #301). {@code FloatingRateResetFrequencyMultiplierOfLeg2} (esma) navigates periodMultiplier
     * inline as the {@code adjustPeriodMultiplier} arg inside a {@code mapSingleToItem(item -> if … then …)}
     * ELSELESS conditional block-lambda. #300 DECLINED the M1 numeric recovery there
     * ({@code numCoerceArgInConditionalLambdaBody}) because the LAMBDA_CHANNEL drain at the lambda TOP
     * re-rendered the conditional as an inline ternary; PR #301 (numericCoercionIfBranchDrain) refined
     * the gate to ALLOW the elseless form and drains the {@code final Integer integer = …;} hoist INSIDE
     * the if-branch, so this carrier now byte-matches golden (one of the 3 #301 A flips). Kept here (the
     * #300 anchor that named it) as the cross-PR lift record; the primary #301 flip lock lives in
     * {@code RuleNumCoerceIfBranchSortExistsTest}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void multiplierOfLeg2Esma_pr301IfBranchDrainLift_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/esma/emir/refit/trade/reports/FloatingRateResetFrequencyMultiplierOfLeg2Rule.java");
    }

    /**
     * Positive-content lock (revert-RED) — the three distinct render shapes: M2's inline
     * {@code BigDecimal.valueOf(1)} (IndexFactor), M3's BigInteger null-safe ternary (OptionPremiumAmount),
     * and M1's nav-chain hoist + coercion (MultiplierOfLeg1). Fails RED on clean source.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void numericCoercionRenderShapesPresent() throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String indexFactor = drrOutput.get(
                "drr/regulation/common/trade/index/reports/IndexFactorRule.java");
        assertNotNull(indexFactor, "IndexFactor not generated");
        assertTrue(indexFactor.contains("formatToBaseOne18Rate.evaluate(BigDecimal.valueOf(1))"),
                "M2: IndexFactor must render the inline BigDecimal.valueOf(1) literal coercion");

        String optionPremium = drrOutput.get(
                "drr/regulation/esma/emir/refit/trade/reports/OptionPremiumAmountRule.java");
        assertNotNull(optionPremium, "OptionPremiumAmount (esma) not generated");
        assertTrue(optionPremium.contains(
                "bigInteger == null ? MapperS.<BigDecimal>ofNull() : MapperS.of(new BigDecimal(bigInteger))"),
                "M3: OptionPremiumAmount must render the BigInteger null-safe ite-hoist arm coercion");

        String multiplierLeg1 = drrOutput.get(
                "drr/regulation/esma/emir/refit/trade/reports/FloatingRateResetFrequencyMultiplierOfLeg1Rule.java");
        assertNotNull(multiplierLeg1, "FloatingRateResetFrequencyMultiplierOfLeg1 not generated");
        assertTrue(multiplierLeg1.contains("(integer == null ? null : BigDecimal.valueOf(integer))"),
                "M1: MultiplierOfLeg1 must render the nav-chain Integer->BigDecimal null-safe coercion");
    }

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
                + path + " (PR #300 numericCoercionCompose).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
