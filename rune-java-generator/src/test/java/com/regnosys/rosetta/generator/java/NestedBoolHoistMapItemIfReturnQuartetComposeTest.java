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
 * PR #393 — the nested-boolHoist/mapItem-if-return quartet (4 byte flips:
 * ExpirationDateRule + ExecutionAgentCounterparty1Rule +
 * ExecutionAgentOfTheCounterparty1DTCCRule + ExecutionAgentOfCounterparty1DTCCRule,
 * all drr POJO).
 *
 * <p><b>deepThenLambdaNestedBoolHoist</b> (ControlFlowHandler): the #179 nested-else
 * boolHoist restructure at the deep-then handshake seat — a NESTED-ELSE rung of
 * {@code hoistAsDeepThenMapperLocalOrNull}'s ladder whose condition is a bare fn/rule
 * call ({@code isBareFunctionCallCondition} + the {@code unwrapToBuilder} structural
 * witness) deepens the ladder remainder one level ({@code } else { final Boolean
 * _boolean = <bare call>; if ((_boolean == null ? false : _boolean)) { … } else if
 * (…) { … } else { … } }}) instead of flattening to {@code } else if (}.
 * LAMBDA-route + singleton-scoped (the per-lambda {@code _boolean} law, #179/#281);
 * the statement route and a &gt;1-bare-fn-rung ladder keep the flat form (zero
 * carriers). Restructured rungs contribute the UNWRAPPED inner's refs only (the #217
 * law); zero boolHoist rungs = identity emission (every pre-#393 carrier
 * byte-frozen).
 *
 * <p><b>ruleLadderMixedCondArmChainAdmit</b> (CollectionHandler): the nested-mapItem
 * if-return block-lambda class — a RULE-path {@code then extract <ladder>} step
 * whose ctl is confined to the UNION of the #379 (CR-terminal chains in rung
 * CONDITIONS, any rung — the extract-aware CR-terminal recognition) and #381
 * (confined ARM chains) cells reads HANDLED. The mid-level compile runs under the
 * crThenCond window ({@code tryDeepThenHoist}'s push); the window read walks
 * per-rung conditions (the #379 reach-law's banked multi-rung widening); the
 * suppression decline exempts window-admitted chains (the #366/#382
 * together-restructure argument); the block dispatch admits the window node
 * directly; {@code renderLadderLevel} re-anchors a window rung's multi-line
 * hoisted-chain condition at rung indent (golden's assignment+1). The two-channel
 * naming ({@code _thenArg0}/{@code _thenArg1} per-lambda ancestor-collision escapes
 * beside plain {@code thenArg2}/{@code thenArg3}) rides the #350/#381
 * collision-group law unchanged.
 *
 * <p>Whole-file byte locks run through the REAL D11 POJO route (drr) and revert RED
 * without the facets; every witness token is occurrence-counted (python
 * {@code str.count} semantics — the #352 law) and PRE-counted against
 * f-probe-392post (witness393.py).
 */
class NestedBoolHoistMapItemIfReturnQuartetComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String EXPD =
            "drr/standards/iosco/cde/version1/datetime/reports/ExpirationDateRule.java";
    private static final String EAC1 =
            "drr/regulation/common/trade/party/reports/ExecutionAgentCounterparty1Rule.java";
    private static final String EAT1 =
            "drr/regulation/mas/rewrite/margin/reports/ExecutionAgentOfTheCounterparty1DTCCRule.java";
    private static final String EAO1 =
            "drr/regulation/mas/rewrite/valuation/reports/ExecutionAgentOfCounterparty1DTCCRule.java";

    private static Map<String, String> drrPojoOutput;

    static boolean cellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellAvailable()) {
            drrPojoOutput = generatePojoCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    /**
     * The REAL D11 POJO-cell generation path (Rule/Report/LabelProvider included) —
     * mirrors {@code D11CorpusRegressionTest#pojo_comparison}'s wiring (the
     * RuleIteHoistTest harness).
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

    // ==== byte locks (all 4 flips through the REAL D11 POJO route) ====

    /** deepThenLambdaNestedBoolHoist: the nested-else bare-fn rung carrier. */
    @Test
    @EnabledIf("cellAvailable")
    void expirationDateRule_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrPojoOutput, DRR_GOLDEN_DIR, EXPD);
    }

    /** ruleLadderMixedCondArmChainAdmit: the nested-mapItem if-return carrier (common). */
    @Test
    @EnabledIf("cellAvailable")
    void executionAgentCounterparty1_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrPojoOutput, DRR_GOLDEN_DIR, EAC1);
    }

    /** the mas margin twin. */
    @Test
    @EnabledIf("cellAvailable")
    void executionAgentOfTheCounterparty1DTCC_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrPojoOutput, DRR_GOLDEN_DIR, EAT1);
    }

    /** the mas valuation twin. */
    @Test
    @EnabledIf("cellAvailable")
    void executionAgentOfCounterparty1DTCC_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrPojoOutput, DRR_GOLDEN_DIR, EAO1);
    }

    // ==== occurrence-counted witnesses (python str.count semantics — the #352 law) ====

    /**
     * deepThenLambdaNestedBoolHoist (drr ExpirationDateRule): the per-lambda
     * {@code _boolean} hoist + the null-safe guard (PRE 0 / golden 1 each); the
     * following CR rung rides INLINE one level deeper (PRE 1 / golden 1 — position
     * moves, occurrence holds); negative — the flat bare-fn {@code } else if (}
     * rung is gone (PRE 1 / golden 0).
     */
    @Test
    @EnabledIf("cellAvailable")
    void expirationDateRule_nestedBoolHoist_witness() {
        String gen = drrPojoOutput.get(EXPD);
        assertNotNull(gen, "ExpirationDateRule not generated");
        assertEquals(1, count(gen,
                "final Boolean _boolean = isCommodityFixedPriceForward.evaluate(product.get());"),
                "the bare-fn rung hoists the per-lambda _boolean (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "if ((_boolean == null ? false : _boolean)) {"),
                "the null-safe guard replaces the getOrDefault form (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                "} else if (ComparisonResult.ofNullSafe(MapperS.of(isCommodityOption.evaluate(product.get())))"),
                "the following CR rung stays INLINE at the deeper level (PRE 1 / golden 1)");
        assertEquals(0, count(gen,
                "} else if (MapperS.of(isCommodityFixedPriceForward.evaluate(product.get())).getOrDefault(false)) {"),
                "the flat bare-fn else-if rung is gone (PRE 1 / golden 0)");
    }

    /**
     * ruleLadderMixedCondArmChainAdmit two-channel naming (drr EAC1): the inner
     * mapItem lambda's first two hoists escape the outer collision
     * ({@code _thenArg0}/{@code _thenArg1}) while the next two keep the plain names
     * ({@code thenArg2}/{@code thenArg3}) — the #350/#381 collision-group law
     * (PRE 0 / golden 1 each); the if-RETURN arms carry the literal and typed-empty
     * terminals (PRE 0 / golden 1 each).
     */
    @Test
    @EnabledIf("cellAvailable")
    void executionAgentCounterparty1_twoChannelNaming_witness() {
        String gen = drrPojoOutput.get(EAC1);
        assertNotNull(gen, "ExecutionAgentCounterparty1Rule not generated");
        assertEquals(1, count(gen, "final MapperS<PartyInformation> _thenArg0 = item"),
                "the rung-1 condition chain hoists with the ancestor-collision escape"
                + " (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final MapperS<PartyInformation> _thenArg1 = item"),
                "the rung-1 arm chain's first level escapes too (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final MapperS<Party> thenArg2 = _thenArg1"),
                "the arm chain's second level keeps the plain name (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final MapperS<PartyInformation> thenArg3 = item"),
                "the rung-2 condition chain keeps the plain name (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "return MapperS.of(\"NOAP\");"),
                "the rung-2 literal arm returns early (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "return MapperS.<String>ofNull();"),
                "the elseless fall-through returns the typed empty (PRE 0 / golden 1)");
    }

    /**
     * the outer mapSingleToItem block decomposition (drr EAC1): the outer nav +
     * mapItem levels hoist as method-group {@code thenArg0}/{@code thenArg1} and
     * the consumer re-roots {@code return thenArg1.first()} (PRE 0 / golden 1
     * each); the inner ladder renders as the block-form mapItem (PRE 0 / golden 1).
     */
    @Test
    @EnabledIf("cellAvailable")
    void executionAgentCounterparty1_outerDecomposition_witness() {
        String gen = drrPojoOutput.get(EAC1);
        assertNotNull(gen, "ExecutionAgentCounterparty1Rule not generated");
        assertEquals(1, count(gen, "final MapperC<PartyInformation> thenArg0 = reportableEvent"),
                "the outer nav level hoists (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final MapperC<String> thenArg1 = thenArg0"),
                "the mapItem level hoists off the nav local (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "return thenArg1"),
                "the consumer re-roots on the hoisted level (PRE 0 / golden 1)");
        assertEquals(1, count(gen, ".mapItem(item -> {"),
                "the ladder renders as the block-form mapItem (PRE 0 / golden 1)");
    }

    /**
     * the runtime-then negatives across the trio: the inline {@code .then(} forms
     * the decline produced are gone (EAC1 PRE 2 + 4 [item/_item] / golden 0;
     * the mas twins PRE 2 each / golden 0).
     */
    @Test
    @EnabledIf("cellAvailable")
    void executionAgentTrio_runtimeThenGone_witness() {
        String eac1 = drrPojoOutput.get(EAC1);
        String eat1 = drrPojoOutput.get(EAT1);
        String eao1 = drrPojoOutput.get(EAO1);
        assertNotNull(eac1, "ExecutionAgentCounterparty1Rule not generated");
        assertNotNull(eat1, "ExecutionAgentOfTheCounterparty1DTCCRule not generated");
        assertNotNull(eao1, "ExecutionAgentOfCounterparty1DTCCRule not generated");
        assertEquals(0, count(eac1, ".then(item ->"),
                "the runtime then form is gone (PRE 2 / golden 0)");
        assertEquals(0, count(eac1, ".then(_item ->"),
                "the escaped-param runtime then form is gone (PRE 4 / golden 0)");
        assertEquals(0, count(eat1, ".then(item ->"),
                "the mas margin twin's runtime then form is gone (PRE 2 / golden 0)");
        assertEquals(0, count(eao1, ".then(item ->"),
                "the mas valuation twin's runtime then form is gone (PRE 2 / golden 0)");
    }

    // ==== helpers ====

    /** Occurrence count — python {@code str.count} semantics (the #352 law). */
    private static int count(String haystack, String needle) {
        int n = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) >= 0) {
            n++;
            idx += needle.length();
        }
        return n;
    }

    private void assertByteMatchesGolden(Map<String, String> output, Path goldenDir, String path)
            throws IOException {
        assertNotNull(output, "cell output not generated");
        String gen = output.get(path);
        assertNotNull(gen, "missing generated output: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "missing golden: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(gen),
                path + " must byte-match the frozen 9.83.0 golden");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
