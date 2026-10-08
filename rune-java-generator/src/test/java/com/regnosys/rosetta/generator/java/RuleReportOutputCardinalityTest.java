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
 * PR #272 — facet reportOutputCardinality (GENERATOR, the M7b-3 rule-body cluster): the foundational
 * report-output cardinality fix. 35 drr POJO Rule byte flips.
 *
 * <p><b>The root cause.</b> A reporting rule's synthetic {@code RFunction} output is created by
 * {@code RFunction.fromRule} with a hard-coded {@code (0..1)} cardinality; {@code RuleGenerator}
 * back-fills it to MULTI only when {@code ws.getCardinality(expr) == MULTI}. But
 * {@code CardinalityComputer} read a {@code then} pipe as conservative {@code SINGLE} (its historical
 * default the function tail depends on), so a multi {@code filter … then extract …} rule body
 * (e.g. {@code ClearingThresholdOfCounterparty1}, whose chain becomes multi at the
 * {@code then extract ExtractRegimeInformation(…)} multi-output call) stayed single — the
 * {@code ReportFunction<I,O>} emitted the bare {@code O} type-arg + a {@code .get()} body terminal
 * where golden emits {@code List<O>} / {@code List<? extends O>} + {@code .getMulti()}.
 *
 * <p><b>The fix (rule-scoped — the function tail stays byte-frozen, the #232/#269 lesson).</b>
 * <ul>
 *   <li>{@code CardinalityComputer.computeRuleBody} (a new then-aware path; the global
 *       {@code compute} is unchanged) computes a {@code then} pipe faithfully — multi iff the
 *       argument is multi AND the body is an element-wise nav/extract/filter (NOT an AGGREGATE body
 *       like the {@code if exists(…) then True else False} conditional, which collapses to single).
 *       {@code RuleGenerator} reads it via {@code RWorkspace.getRuleBodyCardinality}.</li>
 *   <li>{@code JavaTypeTranslator.listWrap} wraps the base-interface output type — polymorphic
 *       {@code List<? extends O>} for a MODEL-typed element (a data/choice type — the Schedule
 *       rules' {@code PricePeriod}/{@code NotionalPeriod}), invariant {@code List<O>} for a
 *       primitive/enum/string element (the Counterparty rules' {@code Boolean}/{@code String}).</li>
 *   <li>{@code FunctionExpressionRenderer} coerces the whole-output then-chain terminal with
 *       {@code .getMulti()} (the fall-through nav + the {@code isInvocationWrapFactory} function-call
 *       arms, and the generic-SET {@code isMultiToMultiSet}), rule-scoped via
 *       {@code getRuleBodyCardinality}.</li>
 *   <li>{@code FunctionGenerator.collectImportsFromTypeArgument} +
 *       {@code FunctionTemplateModel.renderTypeArgumentSimpleName} now handle a
 *       {@code JavaWildcardTypeArgument} bound (rules are the first base interface with a
 *       {@code List<? extends X>} type arg) so {@code PricePeriod} imports + renders by simple name
 *       instead of FQN-inline.</li>
 * </ul>
 *
 * <p><b>The 35 flips</b> split 13 primitive-output (invariant {@code List<X>}: ClearingThreshold /
 * CorporateSector / Sector / ReportingObligation / IsReportableEvent across esma/fca/hkma) + 22
 * model-typed-output (wildcard {@code List<? extends X>}: PriceSchedule / StrikePriceSchedule /
 * ContractPriceSchedule / NotionalAmountSchedule / NotionalQuantitySchedule across the regimes).
 *
 * <p><b>Green-safe by construction.</b> The byte-oracle measured exactly 35 flips, 0 within-waiver
 * regressions, FUNCTION-byte-neutral (cdm5 83 / cdm6 239 / drr 211 UNCHANGED — the then-aware
 * cardinality is reached ONLY via the rule-emission path; the global {@code compute} the function
 * tail uses is untouched). The decline locks below are GREEN single-output rules whose then-chain
 * body the cardinality must NOT promote to multi: {@code Counterparty1/2FederalEntityIndicator}
 * (cftc) end in an {@code if exists(…) then True else False} conditional — an AGGREGATE body that
 * collapses the multi piped chain to a single {@code Boolean} (the element-wise whitelist excludes
 * conditionals), and {@code ActionType} (asic) is a single enum-output rule (proves no spurious
 * multi).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr POJO output (Rule kind) against the
 * frozen goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleReportOutputCardinalityTest {

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

    // ==== Flip locks (revert-RED): PRIMITIVE-output rules — invariant `List<Boolean>` / `List<String>`
    //      base interface + `.getMulti()` terminal (the esma/fca/hkma Counterparty rules). ====

    @Test
    @EnabledIf("drrCellAvailable")
    void clearingThresholdOfCounterparty1_esma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/trade/reports/ClearingThresholdOfCounterparty1Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void clearingThresholdOfCounterparty2_fca_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/fca/ukemir/refit/trade/reports/ClearingThresholdOfCounterparty2Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void corporateSectorOfTheCounterparty1_esma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/trade/reports/CorporateSectorOfTheCounterparty1Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void sectorOfTheCounterparty1_hkma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/hkma/rewrite/trade/reports/SectorOfTheCounterparty1Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void isReportableEvent_esma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/trade/reports/IsReportableEventRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void reportingObligationOfTheCounterparty2_fca_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/fca/ukemir/refit/trade/reports/ReportingObligationOfTheCounterparty2Rule.java");
    }

    // ==== Flip locks (revert-RED): MODEL-typed-output rules — polymorphic `List<? extends PricePeriod>`
    //      / `List<? extends NotionalPeriod>` base interface (the Schedule rules; exercises the
    //      wildcard import + simple-name render fix). ====

    @Test
    @EnabledIf("drrCellAvailable")
    void priceSchedule_asic_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/PriceScheduleRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void strikePriceSchedule_asic_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/StrikePriceScheduleRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void notionalAmountScheduleLeg1_esma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/trade/reports/NotionalAmountScheduleLeg1Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void contractPriceSchedule_csa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/csa/rewrite/trade/reports/ContractPriceScheduleRule.java");
    }

    // ==== Green-safety decline locks: GREEN single-output rules whose then-chain body the cardinality
    //      must NOT promote to multi. ====

    /**
     * Green-safety lock (DECLINE — AGGREGATE body collapses a multi chain to single): {@code
     * Counterparty1FederalEntityIndicatorRule} (cftc) pipes a multi {@code thenArg1} into an
     * {@code if exists(…) then True else False} conditional, whose result is a single {@code Boolean}.
     * The element-wise whitelist in {@code CardinalityComputer.isElementWiseThenBody} excludes
     * {@code RConditionalExpr}, so {@code computeRuleBody} returns SINGLE and the output stays
     * {@code ReportFunction<I, Boolean>} — byte-identical to golden. Without the aggregate-body
     * exclusion the first implementation OVER-FIRED this to {@code List<Boolean>} (a regression caught
     * by the byte-oracle).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty1FederalEntityIndicator_aggregateBody_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/trade/reports/Counterparty1FederalEntityIndicatorRule.java");
    }

    /**
     * Green-safety lock (DECLINE — second aggregate-body instance): {@code
     * Counterparty2FederalEntityIndicatorRule} (cftc), the sibling of the above. A second proof that
     * the conditional/exists aggregate-body decline holds.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty2FederalEntityIndicator_aggregateBody_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/trade/reports/Counterparty2FederalEntityIndicatorRule.java");
    }

    /**
     * Green-safety lock (DECLINE — genuinely single chain): {@code ActionTypeRule} (asic), a single
     * enum-output report rule whose body produces one {@code ActionTypeEnum}. Proves the then-aware
     * cardinality does not spuriously promote a single chain to multi (output stays
     * {@code ReportFunction<I, ActionTypeEnum>}).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void actionType_singleChain_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/ActionTypeRule.java");
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
                + path + " (PR #272 reportOutputCardinality).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
