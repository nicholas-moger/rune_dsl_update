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
 * PR #277 — TWO disjoint, green-safe, FUNCTION-byte-neutral GENERATOR mechanisms in the M7b-3
 * rule-body cluster (the user-chosen clean compose). 5 drr POJO Rule byte flips.
 *
 * <p><b>(A) bareEnumLambdaWrap (1) — a bare enum-constant map body.</b> A rule
 * {@code extract ReportLevelEnum -> TCTN} compiles to the bare dotted constant
 * {@code ReportLevelEnum.TCTN} (handle(REnumValueRef)/the Cat-13 bare-enum path emit it UNWRAPPED —
 * right for a comparison/contains operand the consumer self-wraps, wrong for the map*-method
 * {@code Function<MapperS<T>, MapperS<F>>} body signature), so golden re-presents it
 * {@code MapperS.of(ReportLevelEnum.TCTN)}. {@code CollectionHandler.compileLambda}'s MAPPER_EXPECTING
 * block gains a {@code isBareEnumReference} arm — the bare-ENUM sibling of the bare-FUNCTION /
 * RConstructorExpr / only-element arms (the SAME {@code wrappedInMapperSOf} re-wrap). Carrier:
 * {@code LevelRule} (cde version3).
 *
 * <p><b>(B) numCoerceArgHoist (4) — an Integer evaluate-arg into a BigDecimal param.</b> A NULLABLE
 * Integer nav-chain value ({@code …<Integer>map("getPeriodMultiplier", …).get()}) passed to a
 * BigDecimal-expecting function param hoists {@code final Integer integer = <chain>.get();} + coerces
 * {@code (integer == null ? null : BigDecimal.valueOf(integer))} (upstream convertNullSafe for a boxed
 * Integer narrow — {@code BigDecimal.valueOf(int)} auto-unboxes, so the value is null-guarded). The
 * fork passed the bare Integer (non-compiling). {@code ReferenceHandler.tryMetaDerefArg}'s gate
 * (which already serves the meta-deref and BigInteger→BigDecimal arg coercions) gains an
 * Integer→BigDecimal arm, LITERAL Integer args excluded (an {@code RIntLiteral} is provably non-null,
 * handled bare by {@code LiteralHandler}). Carriers: {@code FloatingRateResetFrequencyPeriodMultiplierLeg1/2}
 * (cftc — the #276 bare-function-then terminal whose arg now coerces) +
 * {@code FloatingRateResetFrequencyMultiplierOfLeg1/2} (fca — an explicit-args thenArg).
 *
 * <p><b>Green-safe by construction (both).</b> A bare enum constant at a MapperS-expecting map body /
 * a bare Integer into a BigDecimal param both do not compile, so golden ALWAYS coerces and NO green
 * file carries the uncoerced form — every carrier was a NON_COMPILING waivered mismatch. The
 * byte-oracle measured exactly 5 flips (drr POJO mismatches 546 → 541), FUNCTION-byte-neutral
 * (cdm5 83 / cdm6 239 / drr 211 UNCHANGED), 0 within-waiver regressions. All 5 are NON_COMPILING, so
 * byte AND functional parity each rise by 5.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr POJO output (Rule kind) against the
 * frozen goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleBodyCleanComposeTest {

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

    // ==== (A) bareEnumLambdaWrap flip lock (revert-RED): a bare enum-constant map body now wraps
    //      `item -> MapperS.of(ReportLevelEnum.TCTN)`. ====

    /** Flip lock: Level (cde version3) — {@code extract ReportLevelEnum -> TCTN/PSTN} map bodies. */
    @Test
    @EnabledIf("drrCellAvailable")
    void level_cdeV3_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/standards/iosco/cde/version3/event/reports/LevelRule.java");
    }

    // ==== (B) numCoerceArgHoist flip locks (revert-RED): an Integer evaluate-arg into a BigDecimal
    //      param now hoists `final Integer integer = <chain>.get();` + coerces
    //      `(integer == null ? null : BigDecimal.valueOf(integer))`. ====

    /** Flip lock: FloatingRateResetFrequencyPeriodMultiplierLeg1 (cftc) — bare-function-then terminal arg. */
    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRateResetFrequencyPeriodMultiplierLeg1_cftc_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/trade/reports/FloatingRateResetFrequencyPeriodMultiplierLeg1Rule.java");
    }

    /** Flip lock: FloatingRateResetFrequencyPeriodMultiplierLeg2 (cftc). */
    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRateResetFrequencyPeriodMultiplierLeg2_cftc_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/trade/reports/FloatingRateResetFrequencyPeriodMultiplierLeg2Rule.java");
    }

    /** Flip lock: FloatingRateResetFrequencyMultiplierOfLeg1 (fca) — explicit-args thenArg coercion. */
    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRateResetFrequencyMultiplierOfLeg1_fca_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/fca/ukemir/refit/trade/reports/FloatingRateResetFrequencyMultiplierOfLeg1Rule.java");
    }

    /** Flip lock: FloatingRateResetFrequencyMultiplierOfLeg2 (fca). */
    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRateResetFrequencyMultiplierOfLeg2_fca_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/fca/ukemir/refit/trade/reports/FloatingRateResetFrequencyMultiplierOfLeg2Rule.java");
    }

    // ==== Green-safety decline locks: GREEN files my mechanisms could touch but correctly leave alone. ====

    /**
     * Green-safety lock (bareEnumLambdaWrap DECLINE): {@code ActionType} (asic margin) is a GREEN rule
     * carrying enum constants in NON-MapperS-expecting positions (comparison operands the
     * {@code wrapEnumOperand} seat handles, not a {@code mapSingleToItem} body). Proves the new
     * {@code isBareEnumReference} arm is MAPPER_EXPECTING-scoped — it does not perturb an enum at a
     * comparison/filter seat.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void actionType_asic_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/margin/reports/ActionTypeRule.java");
    }

    /**
     * Green-safety lock (numCoerceArgHoist DECLINE): {@code FixedRateOfLeg1OrCoupon} (esma) is a GREEN
     * rule (a #276 carrier) that already carries a {@code BigDecimal.valueOf} numeric coercion. Proves
     * the new Integer→BigDecimal arm in {@code tryMetaDerefArg} does not double-coerce / perturb a green
     * rule whose numeric args are already correct.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void fixedRateOfLeg1OrCoupon_esma_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/trade/reports/FixedRateOfLeg1OrCouponRule.java");
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
                + path + " (PR #277 bareEnumLambdaWrap + numCoerceArgHoist).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
