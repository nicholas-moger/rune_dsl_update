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
 * PR #273 — facet injectedSubRuleCardinality (GENERATOR, the M7b-3 rule-body cluster): the
 * report-output-cardinality COMPLETION of PR #272. 30 drr POJO Rule byte flips.
 *
 * <p><b>The gap PR #272 left.</b> #272 made a reporting rule's then-aware cardinality multi for an
 * element-wise {@code filter … then extract …} body. But a rule whose body is — or ends in — a BARE
 * reference to ANOTHER reporting rule still read SINGLE: a sub-rule's synthetic {@code RFunction}
 * output is hard-coded {@code (0..1)} by {@code RFunction.fromRule}, and {@code CardinalityComputer}
 * had no {@code RRule} case, so the sub-rule's true (multi) cardinality — which lives in ITS body —
 * was invisible. Two shapes carry this: a WHOLE-BODY delegation
 * ({@code reporting rule X: cdeV1.basket.BasketConstituentNumberOfUnits}) and a then-chain tail
 * ({@code filter IsAllowableActionForASIC then cde.quantity.NotionalAmountScheduleLeg1}).
 *
 * <p><b>The fix (two coordinated, rule-scoped halves — the function tail stays byte-frozen).</b>
 * <ul>
 *   <li><b>Detection</b> — {@code CardinalityComputer.computeSymbolRefCardinality} gains an
 *       {@code RRule} case (rule-output path only, {@code thenAware}): recurse into the referenced
 *       rule's own body via {@code computeRuleBody}, transitively (asic → cdeV2 → cdeV1 until a
 *       directly-multi {@code extract} is reached), guarded by an identity-set against a malformed
 *       cyclic rule chain (the visited set widened from {@code Set<RShortcut>} to {@code Set<RNode>}
 *       to hold both alias and rule hops). This drives the {@code ReportFunction<I, List<O>>}
 *       signature + (for a whole-body delegation) the direct multi terminal — the 14 iosco cde
 *       version2/version3 delegations.</li>
 *   <li><b>Terminal</b> — {@code FunctionExpressionRenderer.renderBareInvokableThenSet} (the
 *       single bare-rule-then seat — {@code filter … then <rule>}) wraps a multi sub-rule
 *       invocation in {@code MapperC.<Elem>of(…).getMulti()} instead of the single
 *       {@code MapperS.of(…).get()} (which assigns a {@code List} to a single {@code MapperS},
 *       non-compiling). Gated on {@code isBareRuleThen} + {@code findEnclosingRule} +
 *       {@code getRuleBodyCardinality == MULTI} — the 16 asic/hkma/jfsa/mas Notional schedule
 *       rules.</li>
 * </ul>
 *
 * <p><b>Green-safe by construction.</b> The byte-oracle measured exactly 30 flips, 0 within-waiver
 * regressions, 0 new mismatches, FUNCTION-byte-neutral (cdm5 83 / cdm6 239 / drr 211 UNCHANGED — the
 * {@code RRule} recursion is reached ONLY via the rule-emission {@code thenAware} path, and the
 * terminal is bare-RULE + rule scoped). golden never emits the single {@code MapperS.of(…).get()}
 * for a multi sub-rule invocation, so every carrier was a waivered (non-compiling) mismatch. The
 * decline locks below are GREEN single-output delegations the recursion must NOT promote to multi.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr POJO output (Rule kind) against the
 * frozen goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleInjectedSubRuleCardinalityTest {

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

    // ==== Flip locks (revert-RED): DETECTION half — a WHOLE-BODY bare sub-rule delegation
    //      (`reporting rule X: cdeV1.…`) now reads multi via the RRule recursion → List<>
    //      signature + multi terminal (the iosco cde version2/version3 rules). ====

    @Test
    @EnabledIf("drrCellAvailable")
    void basketConstituentNumberOfUnits_v2_primitive_byteMatchesGolden() throws IOException {
        // primitive List<BigDecimal> output, delegation cdeV1.basket.BasketConstituentNumberOfUnits
        assertByteMatchesGolden("drr/standards/iosco/cde/version2/basket/reports/BasketConstituentNumberOfUnitsRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void basketConstituents_v2_model_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/standards/iosco/cde/version2/basket/reports/BasketConstituentsRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void priceSchedule_v2_model_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/standards/iosco/cde/version2/price/reports/PriceScheduleRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void strikePriceSchedule_v3_model_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/standards/iosco/cde/version3/price/reports/StrikePriceScheduleRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void notionalAmountScheduleLeg1_v2_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/standards/iosco/cde/version2/quantity/reports/NotionalAmountScheduleLeg1Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void notionalQuantityScheduleLeg1_v3_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/standards/iosco/cde/version3/quantity/reports/NotionalQuantityScheduleLeg1Rule.java");
    }

    // ==== Flip locks (revert-RED): TERMINAL half — a `filter … then <bare sub-rule>` tail now
    //      collapses with MapperC.<Elem>of(…).getMulti() (the asic/hkma/jfsa/mas schedule rules). ====

    @Test
    @EnabledIf("drrCellAvailable")
    void notionalAmountScheduleLeg1_asic_terminal_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/NotionalAmountScheduleLeg1Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void notionalQuantityScheduleLeg1_hkma_terminal_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/hkma/rewrite/trade/reports/NotionalQuantityScheduleLeg1Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void notionalAmountScheduleLeg2_jfsa_terminal_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/jfsa/rewrite/trade/reports/NotionalAmountScheduleLeg2Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void notionalQuantityScheduleLeg2_mas_terminal_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/mas/rewrite/trade/reports/NotionalQuantityScheduleLeg2Rule.java");
    }

    // ==== Green-safety decline locks: GREEN single-output sub-rule delegations the recursion must
    //      NOT promote to multi (a single sub-rule's body is single, so the rule stays single). ====

    /**
     * Green-safety lock (DECLINE — then-chain to a SINGLE sub-rule): {@code ExecutionTimestampRule}
     * (asic) is {@code filter IsAllowableActionForASIC or IsActionTypeTERM then
     * cde.datetime.ExecutionTimestamp}. The sub-rule's body produces a single {@code ZonedDateTime},
     * so the {@code RRule} recursion reads SINGLE and the terminal stays the single
     * {@code MapperS.of(…).get()} (ruleMultiThen == false). Proves the then-chain terminal fix does
     * not spuriously MapperC-wrap a single bare-rule-then.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void executionTimestamp_singleSubRuleThen_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/ExecutionTimestampRule.java");
    }

    /**
     * Green-safety lock (DECLINE — whole-body delegation to a SINGLE sub-rule): {@code EventTypeRule}
     * (asic) is a whole-body {@code cde.event.EventType} delegation producing a single
     * {@code EventTypeEnum}. The recursion reads the sub-rule SINGLE, so the output stays
     * {@code ReportFunction<I, EventTypeEnum>} — byte-identical to golden. Proves the whole-body
     * RRule recursion does not spuriously promote a single delegation to multi.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void eventType_singleWholeBodyDelegation_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/EventTypeRule.java");
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
                + path + " (PR #273 injectedSubRuleCardinality).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
