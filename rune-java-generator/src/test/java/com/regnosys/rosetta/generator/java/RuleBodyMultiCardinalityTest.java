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
 * PR #274 — facet bodyMultiCardinality (GENERATOR, the M7b-3 rule-body cluster): the extract
 * LAMBDA-BODY-seat completion of PR #273's report-output cardinality. 16 drr POJO Rule byte flips.
 *
 * <p><b>The gap #273 left.</b> #273 fixed the WHOLE-OUTPUT terminal of a {@code filter … then
 * <bare sub-rule>} (the single bare-rule-then seat, {@code renderBareInvokableThenSet}). But a
 * reporting rule whose body is {@code filter … then extract <sub-rule>} routes the sub-rule
 * invocation through the EXTRACT LAMBDA body, not that terminal: golden renders
 * {@code thenArg.mapSingleToList(item -> MapperC.<Elem>of(<rule>.evaluate(item.get()))).getMulti()}
 * whenever the invoked sub-rule's OWN body is multi (its {@code evaluate()} returns a {@code List}).
 * The fork rendered the SINGLE form {@code mapSingleToItem(item -> MapperS.of(<rule>.evaluate(item.get())))}
 * — a {@code MapperS.of(List)} that assigns a {@code List} to a {@code MapperS<Elem>}
 * (non-compiling) — because the bare sub-rule body read SINGLE at this seat.
 *
 * <p><b>The fix (two coordinated, rule-scoped halves — the function tail stays byte-frozen).</b>
 * <ul>
 *   <li><b>Method selection</b> — {@code CollectionHandler.isBodyMulti} gains a MONOTONE rule-aware
 *       overlay: when the global {@code CardinalityComputer.compute} reads SINGLE and we are in a
 *       rule ({@code findEnclosingRule != null}), recompute via {@code computeRuleBody}, which fires
 *       #273's {@code RRule} recursion (thenAware/rule-output gated) at the extract-body seat. So
 *       {@code mapMethod} selects {@code mapSingleToList} over {@code mapSingleToItem} for a multi
 *       sub-rule body.</li>
 *   <li><b>Body wrap</b> — {@code ReferenceHandler.renderImplicitRuleInvocation} wraps a multi
 *       sub-rule invocation in {@code MapperC.<Elem>of(…)} ({@code wrappedInMapperCOfSingle}) instead
 *       of {@code MapperS.of(…)} — the {@code renderBareInvokableThenSet} {@code ruleMultiThen} arm
 *       one seat down, gated on the SAME {@code getRuleBodyCardinality == MULTI} signal so the two
 *       halves agree.</li>
 * </ul>
 *
 * <p><b>Green-safe by construction.</b> The byte-oracle measured exactly 16 flips, 0 within-waiver
 * regressions, 0 new mismatches, FUNCTION-byte-neutral (the {@code RRule} recursion is reached ONLY
 * via the rule-emission {@code thenAware} path, and both halves are {@code findEnclosingRule}-gated;
 * a reporting rule is only referenced from a rule/report by grammar). golden never emits
 * {@code MapperS.of(List)} for a multi sub-rule invocation, so every carrier was a waivered
 * (non-compiling) mismatch. The decline locks below are GREEN single-output sub-rule delegations the
 * rule-aware overlay must NOT promote to multi (a single sub-rule's body is single).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr POJO output (Rule kind) against the
 * frozen goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleBodyMultiCardinalityTest {

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

    // ==== Flip locks (revert-RED): a `filter … then extract <multi sub-rule>` body now renders
    //      mapSingleToList(item -> MapperC.<Elem>of(<rule>.evaluate(item.get()))). ====

    @Test
    @EnabledIf("drrCellAvailable")
    void otherPayment_asic_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/OtherPaymentRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void otherPayment_cftc_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/trade/reports/OtherPaymentRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void otherPayment_mas_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/mas/rewrite/trade/reports/OtherPaymentRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void basketConstituents_asic_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/BasketConstituentsRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void basketConstituents_jfsa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/jfsa/rewrite/trade/reports/BasketConstituentsRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void dtccOtherPaymentPayerIdType_cftc_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/dtcc/reports/DTCC_OtherPaymentPayerIDTypeRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void dtccOtherPaymentReceiverIdType_csa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/csa/rewrite/dtcc/reports/DTCC_OtherPaymentReceiverIDTypeRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void notionalAmountScheduleLeg1_cftc_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/trade/reports/NotionalAmountScheduleLeg1Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void notionalAmountScheduleLeg2_cftc_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/trade/reports/NotionalAmountScheduleLeg2Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void traderLocation_hkma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/hkma/rewrite/trade/reports/TraderLocationRule.java");
    }

    // ==== Green-safety decline locks: a GREEN `then extract <SINGLE sub-rule>` body stays
    //      mapSingleToItem(item -> MapperS.of(<rule>.evaluate(item.get()))) (the single sub-rule's
    //      body is single, so the rule-aware overlay reads SINGLE and both halves decline). ====

    /**
     * Green-safety lock (DECLINE — single sub-rule extract body): {@code ActionTypeRule} (asic) is a
     * {@code … then extract <single sub-rule>} producing a single value, so the rule-aware overlay
     * reads SINGLE — the extract stays {@code mapSingleToItem(item -> MapperS.of(…))}. Proves the
     * cardinality overlay does not spuriously promote a single sub-rule body to multi.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void actionType_singleSubRuleExtractBody_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/ActionTypeRule.java");
    }

    /**
     * Green-safety lock (DECLINE — single sub-rule extract body): {@code BrokerRule} (asic) likewise
     * delegates to a single sub-rule via an extract body and stays the single
     * {@code mapSingleToItem(item -> MapperS.of(…))} form. Proves the {@code MapperC.<Elem>of}
     * body-wrap arm in {@code renderImplicitRuleInvocation} declines on a single sub-rule.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void broker_singleSubRuleExtractBody_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/BrokerRule.java");
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
                + path + " (PR #274 bodyMultiCardinality).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
