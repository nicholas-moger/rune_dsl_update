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
 * PR #263 — two disjoint, green-safe GENERATOR mechanisms at the reporting-rule body seat
 * (continuing the M7b-3 rule-body cluster on the #262 foundation; 15 drr POJO Rule byte flips).
 *
 * <p><b>Facet injectedRuleRef (12 flips).</b> An EXPLICIT-args reference to a cross-namespace RULE
 * inside a reporting-rule body (rosetta {@code then extract cde.valuation.ValuationMethod(GetValuation)})
 * must invoke the injected {@code <Name>Rule} field, exactly like the no-arg {@code then <rule>} form
 * (#100/#101/#254). The fork mishandled it at TWO points, both keyed on an obsolete
 * {@code args().isEmpty()} gate that admitted only the no-arg shape:
 * <ul>
 *   <li>{@code ReferenceHandler}'s explicit-args receiver derivation filtered the call symbol to an
 *       {@code RFunction}; an {@code RRule} symbol fell through to {@code .orElse(name)} = the raw
 *       cross-namespace qualified reference name ({@code cde.valuation.ValuationMethod}), a
 *       non-compiling undefined identifier. A new RRule arm derives the receiver from the rule's
 *       GENERATED class via the SAME {@code RFunction.fromRule → toFunctionJavaClass →
 *       lowerCamelCase} path {@code FunctionDependencyCollector.injectRuleDependency} uses (single
 *       source of truth — the receiver matches the {@code @Inject} field name).</li>
 *   <li>{@code FunctionDependencyCollector}'s RRule branch (which registers the {@code @Inject
 *       <Name>Rule} field) carried the same {@code ref.args().isEmpty()} gate, so an explicit-args
 *       rule reference gained no field. The gate was dropped; {@code injectRuleDependency} is
 *       idempotent (a no-arg + with-args reference to the same rule injects one field).</li>
 * </ul>
 * Both gated on the rule-family path ({@code findEnclosingRule != null} / {@code ruleGateSimpleName
 * != null}). Green-safe by construction: the mangled {@code cde.X.Y.evaluate(…)} static call never
 * compiled, so every carrier was already a waivered mismatch and no green file carries it.
 *
 * <p><b>Facet thenOutputMetaDerefRecovery (3 flips).</b> {@code renderThenExtractSet}'s output line
 * called {@code renderMetaValueDerefOrNull} with a hard-coded {@code null} rhsExpr, so the
 * {@code tryTerminalMetaMapperType} fallback (#237/#249) could not recover the terminal meta wrapper
 * when the then-rebound chain's compiled expression type erased to null (a {@code thenArg}-rooted
 * {@code …<FieldWithMetaString>map(…).get()} terminal). Passing the last then-body AST as rhsExpr —
 * exactly as the SET-path call site (renderOperationInner) already does — lets the deref recover the
 * wrapper and emit the {@code final FieldWithMetaString fieldWithMetaString = …; if (… == null) {
 * output = null; } else { output = ….getValue(); }} block. STRICTLY a fallback (only consulted when
 * the compiled type did not surface a meta wrapper), so a non-meta then-output is byte-identical.
 * Green-safe by construction: the bare {@code output = «wrapper».get()} form never compiled.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr POJO output (Rule kind) against the
 * frozen goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleInjectedRuleRefAndThenOutputMetaDerefTest {

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

    /**
     * Generate the drr POJO cell (Rule/Report/LabelProvider included), mirroring
     * {@link D11CorpusRegressionTest#pojo_comparison}'s generator wiring — the rule-body Rule classes
     * this facet touches are emitted by {@link RuleGenerator} (which delegates to
     * {@link FunctionGenerator}, the shared expression compiler + dependency collector this PR fixes).
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

    // ==== Flip locks (revert-RED): injectedRuleRef — explicit-args cross-namespace rule ref. ====

    /** {@code valuationMethodRule} injected field (FQN-inline on the self-name collision) + receiver. */
    @Test
    @EnabledIf("drrCellAvailable")
    void valuationMethod_asic_injectedRuleRef_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/ValuationMethodRule.java");
    }

    /** esma sibling. */
    @Test
    @EnabledIf("drrCellAvailable")
    void valuationMethod_esma_injectedRuleRef_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/trade/reports/ValuationMethodRule.java");
    }

    /** fca sibling. */
    @Test
    @EnabledIf("drrCellAvailable")
    void valuationMethod_fca_injectedRuleRef_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/fca/ukemir/refit/trade/reports/ValuationMethodRule.java");
    }

    /** jfsa sibling. */
    @Test
    @EnabledIf("drrCellAvailable")
    void valuationMethod_jfsa_injectedRuleRef_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/jfsa/rewrite/trade/reports/ValuationMethodRule.java");
    }

    /** mas sibling. */
    @Test
    @EnabledIf("drrCellAvailable")
    void valuationMethod_mas_injectedRuleRef_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/mas/rewrite/trade/reports/ValuationMethodRule.java");
    }

    /** Direction1BuyerParty rule ref + import (no self-collision). */
    @Test
    @EnabledIf("drrCellAvailable")
    void buyerIdentifierFormat_cftc_injectedRuleRef_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/trade/reports/BuyerIdentifierFormatRule.java");
    }

    /** Direction1SellerParty sibling. */
    @Test
    @EnabledIf("drrCellAvailable")
    void sellerIdentifierFormat_cftc_injectedRuleRef_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/trade/reports/SellerIdentifierFormatRule.java");
    }

    /** InterestRateFixedRate rule ref: cftc. */
    @Test
    @EnabledIf("drrCellAvailable")
    void fixedRateLeg1_cftc_injectedRuleRef_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/trade/reports/FixedRateLeg1Rule.java");
    }

    /** Leg2 sibling: cftc. */
    @Test
    @EnabledIf("drrCellAvailable")
    void fixedRateLeg2_cftc_injectedRuleRef_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/trade/reports/FixedRateLeg2Rule.java");
    }

    /** InterestRateFixedRate rule ref: common. */
    @Test
    @EnabledIf("drrCellAvailable")
    void fixedRateLeg1_common_injectedRuleRef_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/common/trade/price/reports/FixedRateLeg1Rule.java");
    }

    /** Leg2 sibling: common. */
    @Test
    @EnabledIf("drrCellAvailable")
    void fixedRateLeg2_common_injectedRuleRef_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/common/trade/price/reports/FixedRateLeg2Rule.java");
    }

    /** ExpirationDate rule ref nested in a map lambda: common. */
    @Test
    @EnabledIf("drrCellAvailable")
    void maturityDateOfTheUnderlier_injectedRuleRef_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/trade/datetime/reports/MaturityDateOfTheUnderlierRule.java");
    }

    // ==== Flip locks (revert-RED): thenOutputMetaDerefRecovery — type-erased terminal recovery. ====

    /** {@code final FieldWithMetaString fieldWithMetaString = thenArg2…get();} + if/else deref. */
    @Test
    @EnabledIf("drrCellAvailable")
    void basketStructurer_thenOutputMetaDeref_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/BasketStructurerRule.java");
    }

    /** UniqueProductIdentifier (cftc valuation): thenArg5 collapse → meta deref. */
    @Test
    @EnabledIf("drrCellAvailable")
    void uniqueProductIdentifier_cftc_thenOutputMetaDeref_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/cftc/rewrite/valuation/reports/UniqueProductIdentifierRule.java");
    }

    /** csa sibling. */
    @Test
    @EnabledIf("drrCellAvailable")
    void uniqueProductIdentifier_csa_thenOutputMetaDeref_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/csa/rewrite/valuation/reports/UniqueProductIdentifierRule.java");
    }

    // ==== Green-safety locks: rules the two changes must NOT perturb. ====

    /**
     * Green-safety lock (explicit-args FUNCTION receiver): {@code CurrencyOfInitialMarginCollectedRule}
     * is a GREEN drr Rule whose body calls a FUNCTION with explicit args
     * ({@code initialMarginCollectedByReportingCounterpartyCurrency.evaluate(item.get(), …)}). The
     * injectedRuleRef receiver change adds an RRule arm but leaves the RFunction path (the {@code
     * else} branch) byte-identical — this rule must stay green, proving the receiver restructure does
     * not perturb explicit-args FUNCTION calls.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void currencyOfInitialMarginCollected_explicitArgsFunction_staysGreen() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/asic/rewrite/margin/reports/CurrencyOfInitialMarginCollectedRule.java");
    }

    /**
     * Green-safety lock (no-arg bare-rule then): {@code CallCurrencyRule} is a GREEN drr Rule with a
     * no-arg {@code then <rule>} body ({@code <name>Rule.evaluate(thenArg.get())}). Dropping the
     * {@code args().isEmpty()} gate on the dependency collector's RRule branch must leave the no-arg
     * case byte-identical (injectRuleDependency is idempotent), so this rule stays green.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void callCurrency_noArgBareRuleThen_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/CallCurrencyRule.java");
    }

    /**
     * Green-safety lock (non-meta then-output): {@code CallAmountRule} is a GREEN drr Rule whose
     * {@code renderThenExtractSet} output is a non-meta then-chain. The thenOutputMetaDerefRecovery
     * rhsExpr is a STRICT fallback (consulted only when the compiled type did not surface a meta
     * wrapper, and recovers null for a non-meta terminal), so this rule stays byte-identical.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void callAmount_nonMetaThenOutput_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/CallAmountRule.java");
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
                + path + " (PR #263 injectedRuleRef + thenOutputMetaDerefRecovery).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
