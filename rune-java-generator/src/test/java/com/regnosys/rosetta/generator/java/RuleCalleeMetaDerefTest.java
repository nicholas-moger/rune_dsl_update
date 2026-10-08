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
 * PR #336 anchor — {@code ruleCalleeMetaDeref} (1 flip, drr POJO Rule).
 *
 * <p>{@code ReferenceHandler.renderImplicitRuleInvocation} now routes a reporting-RULE
 * callee (a {@code then extract <Rule>} whose implicit item is a META wrapper) through the
 * same {@code tryMetaDerefArg} meta-deref machinery the FUNCTION sibling
 * ({@code renderImplicitFunctionInvocation}) has used since PR #144. The carrier —
 * {@code QuantityUnitOfMeasureLeg2Rule} (csa/rewrite/trade) — pipes the item
 * {@code item.get()} (a {@code ReferenceWithMetaNonNegativeQuantitySchedule}) into
 * {@code quantityUnitOfMeasureRule.evaluate(...)}, whose {@code from} VALUE type is
 * {@code NonNegativeQuantitySchedule}. The bare {@code item.get()} arg did not compile; the
 * fix hoists {@code final <Wrapper> w = item.get();} and passes the null-guarded
 * {@code w.getValue()} deref, drained on the LAMBDA_CHANNEL (the rule call is a nested
 * operand inside a {@code .mapSingleToItem(item -> …)} lambda).
 *
 * <p>The synthetic {@code RFunction.fromRule(rule)} input's typeCall is restored from
 * {@code rule.fromType()} because {@code RTypeCall.deepCopy} deliberately drops the
 * {@code referencedTypeId} resolution state, so the ad-hoc synthetic input would otherwise
 * resolve MISSING and {@code tryMetaDerefArg}'s param-type gate would decline.
 *
 * <p>Green-safe by construction: a bare meta wrapper passed into a value-typed rule input
 * never compiled, so no green golden carries the pre-fix form (the D11 strict gate holds
 * 20/20 pre- and post-trim). The whole-file byte comparison below is generated through the
 * REAL {@link D11CorpusRegressionTest#loadCellCorpusCached} rule-kind path and reverts RED
 * without the facet.
 */
class RuleCalleeMetaDerefTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // The flip carrier — the reporting rule call pipes a meta wrapper into a value-typed
    // rule input; the fix hoists the wrapper + passes the null-guarded value deref.
    private static final String QUANTITY_UOM_LEG2 =
            "drr/regulation/csa/rewrite/trade/reports/QuantityUnitOfMeasureLeg2Rule.java";

    private static Map<String, String> drrRuleOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            drrRuleOutput = generateRuleKinds(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
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

    // ==== flip lock (revert-RED) ====

    @Test
    @EnabledIf("cellsAvailable")
    void quantityUnitOfMeasureLeg2_ruleCalleeMetaDeref_byteMatchesGolden() throws IOException {
        assertNotNull(drrRuleOutput, "cell output not generated");
        String gen = drrRuleOutput.get(QUANTITY_UOM_LEG2);
        assertNotNull(gen, "missing generated output: " + QUANTITY_UOM_LEG2);
        Path goldenPath = DRR_GOLDEN_DIR.resolve(QUANTITY_UOM_LEG2);
        assertTrue(Files.isRegularFile(goldenPath), "missing golden: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(gen),
                QUANTITY_UOM_LEG2 + " must byte-match the frozen 9.83.0 golden");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
