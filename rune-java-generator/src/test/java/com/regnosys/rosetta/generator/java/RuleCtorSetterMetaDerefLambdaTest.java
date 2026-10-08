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
 * Anchor for facet {@code ctorSetterMetaDerefLambda} (PR #312): a ctor-setter value that is a
 * NAVIGATION producing a meta wrapper (compiled item type {@code RJavaWithMetaValue}, e.g.
 * {@code item.<FieldWithMetaString>map("getIdentifier", …)}) consumed by a NON-meta single setter,
 * INSIDE a {@code mapItem}/{@code mapSingleToItem} lambda, hoists {@code final <Wrapper> <name> =
 * <nav>.get();} through the LAMBDA_CHANNEL ({@code registerPendingLambdaHoist} →
 * {@code CollectionHandler.compileLambda}'s {@code drainPendingLambdaHoists} block-converts the
 * expression lambda) + derefs {@code (<name> == null ? null : <name>.getValue())} — the #236
 * {@code ctorSetterMetaDerefHoist} analogue for a NAV value at the LAMBDA_CHANNEL seat (#236 handles
 * fn-call values at the STATEMENT_SINK). {@code ConstructionHandler.coerceCtorArg} gains
 * {@code hoistMetaDerefCtorNavInLambdaOrNull}.
 *
 * <p>RULE-scoped ({@code findEnclosingRule}) → FUNCTION-byte-neutral (#232): cdm5 79 / cdm6 232 /
 * drr 206 FUNCTION mismatch UNCHANGED, cdm/iso/fpml POJO byte-IDENTICAL. Green-safe by construction:
 * the fork's bare-wrapper-into-bare-setter never compiled, so every carrier was already a waivered
 * mismatch (a green file cannot carry the firing shape).
 *
 * <p>{@code isInsideDrainableMapLambda} gates the DIRECT map/extract-lambda body only (no intervening
 * {@code RConditionalExpr}) — the conditional-arm exclusion the regscan caught: a pending lambda
 * hoist inside a conditional-ladder body (DTCC_UnderlyingAssetReport's {@code item -> exists(…) ?
 * Report.builder()… : …}) makes {@code compileLambda}'s conditional-block form DECLINE to the inline
 * ternary, mangling the (co-occupied, still-divergent) file away from golden (the
 * {@link #dtccUnderlyingAssetReport_conditionalArm_declines} lock).
 *
 * <p>The census surfaced ~3 clean deref targets (ctor-setter / evaluate-arg / checkedMap-wrap); the
 * byte-oracle refuted it to 1 CLEAN at the ctor-setter seat. The evaluate-arg arm (QuantityUnitOfMeasure,
 * a reporting RULE) needs the shared chain-walk ({@code thenOwnerArgument}) to reach a conditional
 * producer through a multi-extract chain — it resolves to the chain-root filter instead — a
 * cascade-prone rework the #285 OtherPaymentPayer family depends on, DEFERRED (honest-refute).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED 2/4 — the flip lock +
 * the positive-content lock fail on clean source; the 2 green-safety/decline locks pass either way.
 */
class RuleCtorSetterMetaDerefLambdaTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String BASKET =
            "drr/standards/iosco/cde/version1/basket/reports/BasketConstituentsRule.java";
    private static final String DTCC_UNDERLYING_ASSET_REPORT =
            "drr/regulation/csa/rewrite/dtcc/reports/DTCC_UnderlyingAssetReportRule.java";

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

    // ==== Flip lock (revert-RED): the carrier now byte-matches golden. ====

    /** Flip — BasketConstituentsRule (iosco cde version1): the ctor-setter meta-deref-in-lambda. */
    @Test
    @EnabledIf("drrCellAvailable")
    void basketConstituents_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(BASKET);
    }

    // ==== Positive-content lock (revert-RED): the hoist + deref block, not just byte-match. ====

    /**
     * The {@code identifier} ctor-setter (a meta wrapper nav) hoists
     * {@code final FieldWithMetaString fieldWithMetaString = item.<FieldWithMetaString>map(
     * "getIdentifier", …).get();} at the lambda top + derefs
     * {@code .setIdentifier((fieldWithMetaString == null ? null : fieldWithMetaString.getValue()))},
     * NOT the fork's bare {@code .setIdentifier(item.<FieldWithMetaString>map(…).get())} (which passed
     * a FieldWithMetaString where String is expected — never compiled).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void basketConstituents_rendersMetaDerefHoist() {
        String gen = gen(BASKET);
        assertTrue(gen.contains(
                "final FieldWithMetaString fieldWithMetaString = item.<FieldWithMetaString>map("
                + "\"getIdentifier\", productIdentifier -> productIdentifier.getIdentifier()).get();"),
                "Expected the identifier meta wrapper hoisted to a final local at the lambda top");
        assertTrue(gen.contains(
                ".setIdentifier((fieldWithMetaString == null ? null : fieldWithMetaString.getValue()))"),
                "Expected the null-safe .getValue() deref in the setter");
        assertTrue(!gen.contains(
                ".setIdentifier(item.<FieldWithMetaString>map(\"getIdentifier\", "
                + "productIdentifier -> productIdentifier.getIdentifier()).get())"),
                "The fork's bare wrapper-into-bare-setter form must be gone");
    }

    // ==== Green-safety / decline locks. ====

    /**
     * Green-safety — the NON-meta {@code source} setter (a {@code ProductIdTypeEnum}, not a meta
     * wrapper) in the SAME lambda STAYS bare {@code item.<ProductIdTypeEnum>map("getSource", …).get()}:
     * the arm declines a non-{@code RJavaWithMetaValue} item type, so only the genuine meta wrapper
     * ({@code identifier}) is hoisted. Passes on clean source too (the fork always rendered it bare).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void basketConstituents_nonMetaSourceSetter_staysBare() {
        String gen = gen(BASKET);
        assertTrue(gen.contains(
                ".setSource(item.<ProductIdTypeEnum>map(\"getSource\", "
                + "productIdentifier -> productIdentifier.getSource()).get())"),
                "The non-meta source setter must stay bare (only the meta identifier setter derefs)");
    }

    /**
     * DTCC_UnderlyingAssetReportRule's {@code underlyingAssetID}-family ctor-setters sit
     * inside the mapSingleToList CASCADE body (PR #397's {@code ruleCascadeElselessBlock}
     * guard-return block). PR #398 facet {@code effElseCtorSetterMetaDeref} landed the
     * in-lambda META residual: the effective-else conditional block registers the #354
     * blockArmSeatConditionals channel around its ARM compiles, so the #312/#360
     * ctor-setter NAV meta-deref hoist ADMITS at those arms — the assertion INVERTED
     * with the flip (the pre-#398 lock asserted {@code !contains}; golden HAS the
     * hoists): the numbered {@code final FieldWithMetaString fieldWithMetaString0/1}
     * pair inside the inner-mapItem arms + the UNNUMBERED consumer hoist, each with the
     * guarded {@code .getValue()} deref. The {@code isInsideDrainableMapLambda}
     * conditional-arm exclusion is UNCHANGED — the admits flow through the
     * channel-registered block-arm seat, not the exclusion's inline-ternary path. The
     * file is whole-file byte-locked at RuleIteHoistTest (the #257→#398 graduation).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccUnderlyingAssetReport_effElseArms_hoistWrapperDerefs() {
        String gen = gen(DTCC_UNDERLYING_ASSET_REPORT);
        assertTrue(gen.contains(".mapSingleToList(item -> {"),
                "DTCC_UnderlyingAssetReport renders the #397 cascade guard-return block "
                + "(facet ruleCascadeElselessBlock)");
        assertTrue(gen.contains("final FieldWithMetaString fieldWithMetaString0"),
                "facet effElseCtorSetterMetaDeref REGRESSED: the effective-else arm's numbered "
                + "wrapper hoist (final FieldWithMetaString fieldWithMetaString0) is missing");
        assertTrue(gen.contains("(fieldWithMetaString0 == null ? null : "
                        + "fieldWithMetaString0.getValue())"),
                "facet effElseCtorSetterMetaDeref REGRESSED: the guarded .getValue() deref "
                + "consuming the arm hoist is missing");
    }

    private static String gen(String path) {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String g = drrOutput.get(path);
        assertNotNull(g, "Class not generated: " + path);
        return g;
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
                + path + " (PR #312 ctorSetterMetaDerefLambda).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
