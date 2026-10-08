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
 * PR #275 — facet termGetMulti (GENERATOR, the M7b-3 rule-body cluster): the whole-output TERMINAL
 * completion of PR #274's bodyMultiCardinality. 2 drr POJO Rule byte flips.
 *
 * <p><b>The gap #274 left.</b> #274 fixed the METHOD selection (a {@code single extract
 * [multi-lambda-body]} rule body now renders {@code mapSingleToList} via
 * {@code CollectionHandler.isBodyMulti}) and the {@code MapperC.<Elem>of} body wrap. But the
 * whole-output ASSIGNMENT terminal — {@code output = toBuilder(<chain>.getMulti())} for a multi
 * output — stayed the wrong-arity {@code .get()} for these direct-extract bodies. The iosco cde
 * version1 {@code PriceSchedule}/{@code StrikePriceSchedule} rules ({@code extract
 * GetReportablePricePeriod(item, PriceNotation)} — a {@code single extract} whose lambda invokes the
 * MULTI sub-function {@code GetReportablePricePeriod}) reach the no-segment whole-output SET terminal
 * via {@code FunctionExpressionRenderer.isMultiToMultiSet}: golden renders
 * {@code …mapSingleToList(item -> MapperC.<PricePeriod>of(getReportablePricePeriod.evaluate(…))).getMulti()},
 * the fork {@code …get()} (a single from a list-of-lists assigned to a {@code List} output —
 * non-compiling).
 *
 * <p><b>The fix (one source seat, the #274 two-halves-agree pattern).</b>
 * {@code FunctionExpressionRenderer.isMultiToMultiSet} recovers the multi via the SAME
 * extract-lambda-body signal the method selection uses ({@code CollectionHandler.isBodyMulti},
 * promoted to {@code public static}), rule-scoped via the EXPRESSION (the #272 rule branch gates on
 * {@code findEnclosingRule(OPERATION)}, which the iosco version1 ROperation node does NOT resolve —
 * its parent chain is not wired to the rule, unlike the expression's). So the whole-output terminal
 * is {@code .getMulti()}, in agreement with the {@code mapSingleToList} method.
 *
 * <p><b>Green-safe by construction.</b> golden never emits {@code .get()} on a multi
 * {@code mapSingleToList} output assigned to a {@code List} output (it does not compile), so every
 * carrier was a waivered (non-compiling) mismatch; the byte-oracle measured exactly 2 flips, 0
 * within-waiver regressions, 0 new mismatches, FUNCTION-byte-neutral (rule-scoped via
 * {@code findEnclosingRule}). A {@code single extract [single-body]} rule reads
 * {@code isBodyMulti == false}, so the overlay declines and the terminal stays {@code .get()} (the
 * {@code CollateralisationCategory} decline lock).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr POJO output (Rule kind) against the
 * frozen goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleTermGetMultiTest {

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

    // ==== Flip locks (revert-RED): a `single extract [multi sub-function]` whole-output now
    //      terminates `.getMulti()` (the #274 mapSingleToList method's correct multi terminal). ====

    /**
     * Flip lock: {@code PriceSchedule} (iosco cde version1) — {@code extract
     * GetReportablePricePeriod(item, PriceNotation)} over a multi sub-function now terminates
     * {@code .getMulti()} not {@code .get()}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void priceSchedule_ioscoV1_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/standards/iosco/cde/version1/price/reports/PriceScheduleRule.java");
    }

    /**
     * Flip lock: {@code StrikePriceSchedule} (iosco cde version1) — the sibling carrier
     * ({@code extract GetReportableStrikePricePeriod(item, StrikePriceNotationEnum)}).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void strikePriceSchedule_ioscoV1_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/standards/iosco/cde/version1/price/reports/StrikePriceScheduleRule.java");
    }

    // ==== Green-safety decline lock: a GREEN `single extract [single-body]` whole-output stays
    //      `.get()` (the lambda body is single, so isBodyMulti reads false and the overlay declines). ====

    /**
     * Green-safety lock (DECLINE — single-body extract): {@code CollateralisationCategory} (asic
     * margin) is a {@code single extract [single nav-chain body]} whole-output ({@code MapperS.of(input)
     * .mapSingleToItem(item -> item.…checkedMap("to-enum", …)).get()}) to a SINGLE
     * ({@code CollateralisationType3Code__1}) output. It reaches the SAME no-segment whole-output SET
     * terminal as the flip carriers but {@code isBodyMulti} reads false (the lambda body is a single
     * nav chain), so the termGetMulti overlay declines and the terminal stays {@code .get()}. Proves
     * the overlay does not spuriously promote a single-body extract to {@code .getMulti()}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void collateralisationCategory_singleBodyExtract_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/margin/reports/CollateralisationCategoryRule.java");
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
                + path + " (PR #275 termGetMulti).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
