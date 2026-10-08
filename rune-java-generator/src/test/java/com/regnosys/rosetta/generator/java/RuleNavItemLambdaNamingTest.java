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
 * PR #255 — facet {@code navItemLambdaTyping} (rule-body emission, M7b-3).
 *
 * <p>A drr report rule body is a hoisted then-chain
 * ({@code MapperS.of(input).filterSingleNullSafe(item -> …).mapSingleToItem(item -> …) …}); a
 * later stage's filter/extract lambda iterates the NAVIGATED element type, not the rule's
 * from-type. The first navigation step off that lambda's implicit {@code item} names its lambda
 * variable from the receiver's data type — golden uses the actual navigated element type
 * ({@code item.<RateSpecification>map("getRateSpecification", interestRatePayout ->
 * interestRatePayout.getRateSpecification())}), whereas
 * {@code NavigationHandler.resolveLambdaVarName}'s {@code RImplicitVariable} branch fell back to
 * the rule's FROM-type ({@code transactionReportInstruction -> …}) on the assumption that the
 * implicit item is always the from-type. Inside a hoisted then-chain that assumption is false.
 *
 * <p><b>Fix (GENERATOR-only, 1 source file — {@code NavigationHandler}).</b> In the
 * {@code RImplicitVariable} branch of {@code resolveLambdaVarName}, when a compiler is carried
 * (the D11 rule/function emission path) prefer the resolved item data type
 * ({@code implicitItemDataType}) over the rule from-type; keep the from-type when the item IS the
 * from-type (the resolution returns the SAME type / identical name) or when it declines.
 *
 * <p><b>Green-safe by construction:</b> a green from-typed-rule file cannot carry
 * item-type != from-type at this navigation (golden would already name the var from the item
 * type, so the pre-facet fork from-type name would disagree = not green). The compiler-less
 * unit-only callers keep the pre-facet from-type naming (the #176/#225 compiler-gated contract).
 *
 * <p>The byte-oracle + stash-baseline (clean-main {@code dump-now-matching} = 12 pre-existing
 * stale drr Rule waivers, all 51 facet flips were clean-main mismatches) confirm 51 drr POJO
 * Rule files flip; the full all-kinds D11 stays 20/20 with zero regression.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr POJO output (Rule kind)
 * against the frozen goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleNavItemLambdaNamingTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR = DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

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

    /**
     * Generate the drr POJO cell (Rule/Report/LabelProvider included), mirroring
     * {@link D11CorpusRegressionTest#pojo_comparison}'s generator wiring — the rule-body Rule
     * classes the navItemLambdaTyping facet touches are emitted by {@link RuleGenerator} (which
     * delegates to {@link FunctionGenerator}, the shared expression compiler).
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
                // Rule-family generators are still converging on body emission (M7b-3); a
                // failure for an already-waivered element is tolerated debt. The anchored
                // carriers below MUST emit + byte-match, so a swallowed failure surfaces as a
                // null lookup in assertByteMatchesGolden.
                assertNoGenerationErrors(ruleGen.generateClasses(model, version, output));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
                assertNoGenerationErrors(labelProviderGen.generateClasses(model, version, output));
            }
        }
        return output;
    }

    // ---- Flip locks (revert-RED): the navigated-item-type lambda name, one per golden token/region.

    /** cftc / interestRatePayout: {@code interestRatePayout -> interestRatePayout.getRateSpecification()}. */
    @Test
    @EnabledIf("drrCellAvailable")
    void fixedRateDayCountConventionLeg1RuleCftc_navItemName_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/cftc/rewrite/trade/reports/FixedRateDayCountConventionLeg1Rule.java");
    }

    /** asic / party: {@code party -> party.getPartyId()} (a mapC receiver). */
    @Test
    @EnabledIf("drrCellAvailable")
    void newDerivativeTradeRepositoryRuleAsic_navItemName_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/asic/rewrite/trade/reports/NewDerivativeTradeRepositoryRule.java");
    }

    /** common / floatingRateOption: a different navigated element type. */
    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRateIndicatorLeg1RuleCommon_navItemName_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/trade/underlier/reports/FloatingRateIndicatorLeg1Rule.java");
    }

    /** iosco standards / settlementTerms — a non-regulation cell region. */
    @Test
    @EnabledIf("drrCellAvailable")
    void settlementLocationLeg1RuleIosco_navItemName_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version1/execution/reports/SettlementLocationLeg1Rule.java");
    }

    /** esma / trade — cross-region. */
    @Test
    @EnabledIf("drrCellAvailable")
    void hasContractRuleEsma_navItemName_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/trade/reports/HasContractRule.java");
    }

    /** cftc / reportingRegime — a third golden token. */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty1FederalEntityIndicatorRuleCftc_navItemName_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/cftc/rewrite/trade/reports/Counterparty1FederalEntityIndicatorRule.java");
    }

    /** common / interestRatePayout — the dominant token in a different region from cftc. */
    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRateResetFrequencyLeg1RuleCommon_navItemName_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/trade/payment/reports/FloatingRateResetFrequencyLeg1Rule.java");
    }

    // ---- Green-safety lock: an item==from-type nav lambda must stay byte-identical.

    /**
     * Green-safety lock: {@code Counterparty1Rule} (cftc) is a GREEN rule whose FIRST navigation
     * step IS directly off the from-type input — golden names it
     * {@code item.<ReportingSide>map("getReportingSide", transactionReportInstruction ->
     * transactionReportInstruction.getReportingSide())} (the implicit item IS the
     * {@code TransactionReportInstruction} from-type). It exercises the SAME changed branch (1):
     * {@code implicitItemDataType} must resolve to the from-type here, so the chosen name stays
     * {@code transactionReportInstruction} and the file stays byte-identical. If the fix wrongly
     * preferred a different type for the genuine item==from-type case, this green file would
     * diverge.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty1RuleCftc_itemEqualsFromType_staysByteIdentical() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/trade/reports/Counterparty1Rule.java");
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
                + path + " — the implicit-item nav lambda must name its variable from the navigated "
                + "item type (NavigationHandler.resolveLambdaVarName), not the rule from-type.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
