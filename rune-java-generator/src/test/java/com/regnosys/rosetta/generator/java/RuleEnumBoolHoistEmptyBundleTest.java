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
 * PR #269 — FOUR disjoint, green-safe, RULE-scoped GENERATOR mechanisms (the M7b-3 rule-body
 * cluster), composed into one bundle. 32 drr POJO Rule byte flips (8 per facet). All four are
 * gated to the rule-emission path ({@code HandlerHelper.findEnclosingRule != null}) so the shared
 * render paths stay FUNCTION-byte-neutral (cdm5 83 / cdm6 239 / drr 211 UNCHANGED — the #232
 * load-bearing green gate).
 *
 * <ul>
 *   <li><b>B — enumArgCollections</b> ({@code ReferenceHandler.tryEnumSingletonListArg}): a PRESENT
 *       enum constant passed to a MULTI ({@code 1..*}) callee parameter
 *       ({@code getUniqueTransactionIdentifier.evaluate(input, SupervisoryBodyEnum.ASIC)}) hoists the
 *       constant to a named local + coerces the arg to {@code (supervisoryBodyEnum == null ?
 *       Collections.<…>emptyList() : Collections.singletonList(supervisoryBodyEnum))} (the #192
 *       emptyMultiArg singletonList sibling). Carriers (8): {@code UniqueTransactionIdentifier} /
 *       {@code Uti} / {@code …Proprietary} across asic/hkma/jfsa/mas.</li>
 *   <li><b>C — boolHoistInIte</b> ({@code FunctionExpressionRenderer.appendIteHoistChainCore}): a
 *       bare-function-call TOP condition of a rule then-chain ite-hoist hoists {@code final Boolean
 *       _boolean = <fn>.evaluate(…);} ahead of the {@code ifThenElseResult} decl + guards
 *       {@code if ((_boolean == null ? false : _boolean))} — the #179/#217 boolean_condition_hoist
 *       law at the last un-wired seat (now reachable for rules via the #267 sink-opening). Carriers
 *       (8): {@code CallAmount} / {@code CallCurrency} / {@code PutAmount} / {@code PutCurrency}
 *       across cftc/csa/jfsa.</li>
 *   <li><b>D-voidNull</b> ({@code FunctionExpressionRenderer.renderOperationInner}): a reporting rule
 *       whose Java output type is {@code Void} ({@code ReportFunction<…, Void>}) assigns the whole
 *       output {@code output = null;} (golden short-circuits a value-less output); the fork emitted
 *       the full {@code mapSingleToItem(...).get()} chain. Carriers (8): {@code DTCC_Comment1} /
 *       {@code DTCC_CorporateActionNewTradeParty1LEI} / {@code DTCC_ResponsibleDataSubmitterID} /
 *       {@code …IDType} across cftc/csa.</li>
 *   <li><b>D-typedEmptyReturn</b> ({@code CollectionHandler.compileEffectiveElseConditionalBlock}):
 *       an {@code empty} arm of a present-else map-lambda conditional ({@code if cond then empty else
 *       value}) renders the typed {@code return MapperS.<T>ofNull();} instead of the bare
 *       {@code return null;}. Carriers (8): {@code PriceCurrency} (asic/mas),
 *       {@code IndicatorOfTheFloatingRateOfLeg1/2} (esma+fca),
 *       {@code ExecutionAgentReportingCounterparty/Party} (fca).</li>
 * </ul>
 *
 * <p><b>Green-safe by construction</b> (corpus-verified, frozen 9.83.0): each fork form never
 * byte-matched a green file (the bare enum-into-multi / inline ite-hoist condition / Void chain /
 * bare empty arm is always coerced by golden), so every carrier was an already-waivered mismatch.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr POJO output (Rule kind) against
 * the frozen goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED (the 8 flip locks fail
 * on stashed clean-main source; the 4 green-safety decline locks pass on both).
 */
class RuleEnumBoolHoistEmptyBundleTest {

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

    // ==== Facet B (enumArgCollections) flip locks (revert-RED). ====

    /** B: a present enum constant into a MULTI param hoists the local + Collections coercion. */
    @Test
    @EnabledIf("drrCellAvailable")
    void uniqueTransactionIdentifier_asic_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/UniqueTransactionIdentifierRule.java");
    }

    /** B: jfsa Uti sibling. */
    @Test
    @EnabledIf("drrCellAvailable")
    void uti_jfsa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/jfsa/rewrite/trade/reports/UtiRule.java");
    }

    // ==== Facet C (boolHoistInIte) flip locks (revert-RED). ====

    /** C: a bare-fn ite-hoist top condition hoists `final Boolean _boolean = …;`. */
    @Test
    @EnabledIf("drrCellAvailable")
    void callAmount_cftc_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/trade/reports/CallAmountRule.java");
    }

    /** C: jfsa PutCurrency sibling. */
    @Test
    @EnabledIf("drrCellAvailable")
    void putCurrency_jfsa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/jfsa/rewrite/trade/reports/PutCurrencyRule.java");
    }

    // ==== Facet D-voidNull flip locks (revert-RED). ====

    /** D-voidNull: a Void-output rule assigns `output = null;`. */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccComment1_cftc_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/dtcc/reports/DTCC_Comment1Rule.java");
    }

    /** D-voidNull: csa DTCC_ResponsibleDataSubmitterID sibling. */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccResponsibleDataSubmitterID_csa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/csa/rewrite/dtcc/reports/DTCC_ResponsibleDataSubmitterIDRule.java");
    }

    // ==== Facet D-typedEmptyReturn flip locks (revert-RED). ====

    /** D-typedEmptyReturn: an `empty` then-arm renders `return MapperS.<ISOCurrencyCodeEnum>ofNull();`. */
    @Test
    @EnabledIf("drrCellAvailable")
    void priceCurrency_asic_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/PriceCurrencyRule.java");
    }

    /** D-typedEmptyReturn: esma IndicatorOfTheFloatingRateOfLeg1 (`MapperS.<IndexEnum>ofNull()`). */
    @Test
    @EnabledIf("drrCellAvailable")
    void indicatorOfTheFloatingRateOfLeg1_esma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/trade/reports/IndicatorOfTheFloatingRateOfLeg1Rule.java");
    }

    // ==== Green-safety DECLINE locks: green rules at the SAME seats the change must NOT perturb. ====

    /**
     * Green-safety lock (D-voidNull DECLINE — a green Void rule): {@code DTCC_Leg1SettlementPeriodRule}
     * (csa) is a GREEN drr Void rule whose body is {@code empty}, so it already emits {@code output =
     * null;}. The Void short-circuit produces the IDENTICAL {@code output = null;} — byte-unchanged.
     * Proves the gate does not perturb a green Void rule.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccLeg1SettlementPeriod_csa_greenVoid_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/csa/rewrite/dtcc/reports/DTCC_Leg1SettlementPeriodRule.java");
    }

    /**
     * Green-safety lock (C-boolHoist DECLINE — a wrapper-typed ite-hoist condition):
     * {@code Direction2Leg1Rule} (asic) is a GREEN drr rule with an {@code ifThenElseResult} ite-hoist
     * whose condition is a {@code ComparisonResult}/Mapper chain (NOT a bare function call), so it
     * keeps the inline {@code .getOrDefault(false)}. The {@code isBareFunctionCallCondition} gate
     * declines — byte-unchanged.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void direction2Leg1_asic_wrapperIteCondition_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/Direction2Leg1Rule.java");
    }

    /**
     * Green-safety lock (D-typedEmptyReturn DECLINE — a NON-empty effective-else block):
     * {@code DTCC_SEFOrDCMIndicatorRule} (cftc) is a GREEN drr rule with an {@code if cond then <value>
     * else <value>} map-lambda whose BOTH arms are non-empty, so the {@code isEmptyLiteral} arm gate
     * declines and the arms render via their compiled strings — byte-unchanged.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccSefOrDcmIndicator_cftc_nonEmptyEffectiveElse_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/dtcc/reports/DTCC_SEFOrDCMIndicatorRule.java");
    }

    /**
     * Green-safety lock (B-enumArgCollections DECLINE — a single-cardinality enum arg):
     * {@code PriorUTIProprietaryRule} (hkma) is a GREEN drr rule that passes an enum constant to a
     * SINGLE-cardinality callee parameter (golden keeps it bare — no Collections coercion). The
     * {@code paramAcceptsMulti} gate declines — byte-unchanged.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void priorUtiProprietary_hkma_singleCardEnumArg_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/hkma/rewrite/trade/reports/PriorUTIProprietaryRule.java");
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
                + path + " (PR #269 rule-body enum/boolHoist/empty bundle).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
