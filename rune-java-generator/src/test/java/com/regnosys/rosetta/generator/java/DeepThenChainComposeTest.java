package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
 * PR #339 anchor — the three-facet compose (14 flips: 11 drr POJO Rule + 2 drr FUNCTION +
 * 1 cdm5 FUNCTION).
 *
 * <p><b>Facet A — {@code deepBareInvokableThenHoist}</b> (11 POJO flips): the #254/#276
 * SET-seat bare-invokable then-chain admission transplanted to the DEEP seat
 * ({@code CollectionHandler.tryDeepThenHoist}). {@code isHoistableThenChainLocal} admits a
 * bare rule/function then-body on the rule path; the lambda channel widens {@code n == 1}
 * → {@code n >= 1} for bare-bearing chains (in-lambda {@code thenArgN} numbering via the
 * per-scope collision-group law); per-level {@code MapperS.of}/{@code MapperC.<T>of} wraps
 * (the #276 dual wrap-factory law) + the intermediate bare-item only-element collapse wrap
 * (the #260 law at this seat); {@code DeepThenArgHoist} pendings drain INTO the owning
 * conditional arm (the #301 pattern at the effective-else block; the identity-keyed
 * drainable-arm handshake at the ladder rungs — a nested chain stays suppressed, the
 * #219/#250 cascade guard); the #334 mixed-join else-deref widens to bare-INVOKABLE sibling
 * evidence ({@code isProvablyBareJoinArm} — {@code recoverInnerRuleMetaWrapper} null for
 * rules / declared-output {@code detectMetaKind} NONE for functions, the #338 law) at the
 * effective-else AND ite-hoist ladder seats; the implicit evaluate-arg accessor follows the
 * callee parameter's cardinality ({@code .getMulti()} into {@code List} params — the #191
 * law at the implicit seat).
 *
 * <p><b>Facet B — {@code filterPredicateGet}</b> (3 FUNCTION flips): a
 * {@code contains(...)}/{@code disjoint(...)} MEMBERSHIP filter-predicate body is a
 * ComparisonResult (a {@code Mapper<Boolean>}, not the Boolean the {@code filter*NullSafe}
 * signature demands) — coerced {@code .get()} exactly like the comparison/logical/existence
 * arms.
 *
 * <p><b>Facet C — {@code condBothArmsMultiCardinality}</b> (2 of the POJO flips): the
 * #289-comment DEFERRED block-lambda-for-multi sub-case — a rule-scoped BOTH-arms-multi
 * conditional extract body is MULTI (the {@code chainProvesMulti} RConditionalExpr arm) →
 * {@code mapSingleToList} + a {@code MapperC<String>} declaration + {@code MapperC.<T>of}
 * arm wraps at the MAPPER_C_EXPECTING effective-else block; the #295
 * {@code blockArmDerefsToBareLeaf} rendered-shape gate keeps the declaration bare. The
 * FOURTH deferred-decline-lock flip in a row.
 *
 * <p>Every pre-fix form was NON_COMPILING (there is no runtime {@code Mapper.then(Function)}
 * method; a ComparisonResult does not satisfy a Boolean predicate signature; a
 * {@code MapperS<X> = MapperC.<X>of(...)} assignment is a type mismatch) — zero of the
 * 34,686 goldens carry any pre-fix form. Whole-file byte comparisons run through the REAL
 * D11 generation paths and revert RED without the facets.
 */
class DeepThenChainComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // Facet A flip carriers (drr POJO Rule).
    private static final String DELIVERY_ESMA =
            "drr/regulation/esma/emir/refit/trade/reports/DeliveryRule.java";
    private static final String DELIVERY_FCA =
            "drr/regulation/fca/ukemir/refit/trade/reports/DeliveryRule.java";
    private static final String BROKER_ID =
            "drr/regulation/hkma/rewrite/trade/reports/BrokerIdRule.java";
    private static final String COUNTERPARTY2 =
            "drr/regulation/hkma/rewrite/trade/reports/Counterparty2Rule.java";
    private static final String CENTRAL_COUNTERPARTY =
            "drr/regulation/hkma/rewrite/trade/reports/CentralCounterpartyRule.java";
    private static final String CLEARING_MEMBER =
            "drr/regulation/hkma/rewrite/trade/reports/ClearingMemberRule.java";
    private static final String TOTAL_NOTIONAL_QTY_LEG1_ESMA =
            "drr/regulation/esma/emir/refit/trade/reports/TotalNotionalQuantityOfLeg1Rule.java";
    private static final String TOTAL_NOTIONAL_QTY_LEG1_FCA =
            "drr/regulation/fca/ukemir/refit/trade/reports/TotalNotionalQuantityOfLeg1Rule.java";
    private static final String UNDERLYING_ID_OTHER_SOURCE_ASIC =
            "drr/regulation/asic/rewrite/trade/reports/UnderlyingIdOtherSourceRule.java";
    // Facet C flip carriers (drr POJO Rule).
    private static final String UTI_CSA_MARGIN =
            "drr/regulation/csa/rewrite/margin/reports/UniqueTransactionIdentifierRule.java";
    private static final String UTI_CSA_VALUATION =
            "drr/regulation/csa/rewrite/valuation/reports/UniqueTransactionIdentifierRule.java";
    // Facet B flip carriers (drr + cdm5 FUNCTION).
    private static final String COMMODITY_FORWARD_OBSERVABLE =
            "drr/regulation/common/functions/CommodityForwardObservablePriceQuantity.java";
    private static final String COMMODITY_OBSERVABLE =
            "drr/regulation/common/functions/CommodityObservablePriceQuantity.java";
    private static final String CHECK_AGENCY_RATING =
            "cdm/product/collateral/functions/CheckAgencyRating.java";
    // Facet A partial-heal lock — the in-rung hoist lands in golden's exact form but a
    // co-occupying base-conditional divergence forces the OUTER thenArg group to number,
    // escaping the inner pair to _thenArg0/1 (golden: bare thenArg0/1). (The mas DTCC
    // siblings in the same away-4 regscan class differ by a NUMBERING offset instead —
    // gen _thenArg0/1 vs golden _thenArg1/2 under a pre-existing un-hoisted ternary —
    // the Seat-1 OBS-2 distinction; this lock pins the hkma representative.)
    private static final String UNDERLIER_ID_OTHER_SOURCE_HKMA =
            "drr/regulation/hkma/rewrite/trade/reports/UnderlierIdOtherSourceRule.java";
    // Facet A decline lock — a value-then chain whose BASE is a conditional carries control
    // flow, so the #250 thenChainHasControlFlow guard keeps the deep hoist declined.
    private static final String SINGLE_OR_UPPER_LOWER_BARRIER =
            "drr/regulation/common/trade/price/reports/SingleOrUpperAndLowerBarrierRule.java";

    private static Map<String, String> drrFnOutput;
    private static Map<String, String> drrRuleOutput;
    private static Map<String, String> cdm5FnOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            drrFnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
            drrRuleOutput = generateRuleKinds(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
        if (cdm5CellAvailable()) {
            cdm5FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
        }
    }

    /** The REAL D11 FUNCTION-kind generation path (function_comparison). */
    private static Map<String, String> generateFunctions(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var funcGen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        assertNoGenerationErrors(funcGen.generateWithErrors(output));
        return output;
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

    // ==== facet A flip locks (revert-RED) ====

    @Test
    @EnabledIf("drrCellAvailable")
    void deliveryEsma_deepBareInvokableThenHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, DELIVERY_ESMA);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void deliveryFca_deepBareInvokableThenHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, DELIVERY_FCA);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void brokerId_deepBareInvokableThenHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, BROKER_ID);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty2_deepBareInvokableThenHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, COUNTERPARTY2);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void centralCounterparty_iteLadderElseDeref_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, CENTRAL_COUNTERPARTY);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void clearingMember_iteLadderElseDeref_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, CLEARING_MEMBER);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void totalNotionalQtyLeg1Esma_onlyElementCollapseWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, TOTAL_NOTIONAL_QTY_LEG1_ESMA);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void totalNotionalQtyLeg1Fca_onlyElementCollapseWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, TOTAL_NOTIONAL_QTY_LEG1_FCA);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void underlyingIdOtherSourceAsic_rungDrainGetMulti_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, UNDERLYING_ID_OTHER_SOURCE_ASIC);
    }

    // ==== facet C flip locks (revert-RED; the #289-deferred cardinality) ====

    @Test
    @EnabledIf("drrCellAvailable")
    void utiCsaMargin_condBothArmsMultiCardinality_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, UTI_CSA_MARGIN);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void utiCsaValuation_condBothArmsMultiCardinality_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, UTI_CSA_VALUATION);
    }

    // ==== facet B flip locks (revert-RED) ====

    @Test
    @EnabledIf("drrCellAvailable")
    void commodityForwardObservable_filterPredicateGet_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, COMMODITY_FORWARD_OBSERVABLE);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void commodityObservable_filterPredicateGet_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, COMMODITY_OBSERVABLE);
    }

    @Test
    @EnabledIf("cdm5CellAvailable")
    void checkAgencyRating_filterPredicateGet_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, CHECK_AGENCY_RATING);
    }

    // ==== facet A partial-heal lock (the away-4 naming class) ====

    /**
     * GRADUATED at PR #385 (the conversion this lock's original javadoc PROMISED — the
     * anticipated-seat law): the facet {@code nestedThenIteLadder} landed the
     * base-conditional co-occupier as golden's NESTED {@code ifThenElseResult} if/else
     * (the THEN-side one-level nesting — {@code if (c1) { if (c2) {…} else { ofNull } }
     * else { ofNull }}), so the conditional level left the thenArg group
     * ({@code effectiveThenArgs} 2 → 1 → the bare {@code thenArg} base decl), the
     * in-rung pair unprefixed to {@code thenArg0/thenArg1} (the #381 no-collision cell —
     * the original positive witness {@code _thenArg0} now counts 0 in gen AND golden),
     * and the file byte-matches golden (the #329/#380 converting-lock graduation law).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void underlierIdOtherSourceHkma_nestedLadderConverted_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, UNDERLIER_ID_OTHER_SOURCE_HKMA);
    }

    // ==== facet A decline lock (the control-flow guard) ====

    /**
     * SingleOrUpperAndLowerBarrierRule's value-then chain has a CONDITIONAL base — the
     * #250 decline this lock originally pinned was CONVERTED by the #351 F3-i3
     * conditional-BASE thenArg hoist (this lock's own javadoc promised the conversion:
     * the restructure removed the inline {@code .then(} positive witness, count 0 in
     * every golden). The base now hoists as golden's mapper-typed forced-name block
     * ({@code final MapperS<TriggerEvent> thenArg1;} / {@code thenArg2;} — one per
     * chain, sharing the per-method thenArg sequence).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void singleOrUpperLowerBarrier_conditionalBase_hoistsAsForcedNameThenArg() {
        String gen = generated(drrRuleOutput, SINGLE_OR_UPPER_LOWER_BARRIER);
        assertFalse(gen.contains(".then(item ->"),
                "the #351 base restructure removes the inline runtime .then( form "
                + "(count 0 in every golden)");
        assertTrue(gen.contains("final MapperS<TriggerEvent> thenArg1;"),
                "the conditional base hoists as the mapper-typed forced-name thenArg block");
        assertTrue(gen.contains("final MapperS<TriggerEvent> thenArg2;"),
                "the second chain's conditional base shares the per-method thenArg sequence");
    }

    // ==== helpers ====

    private String generated(Map<String, String> output, String path) {
        assertNotNull(output, "cell output not generated");
        String gen = output.get(path);
        assertNotNull(gen, "missing generated output: " + path);
        return gen;
    }

    private String golden(Path goldenDir, String path) throws IOException {
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "missing golden: " + goldenPath);
        return Files.readString(goldenPath);
    }

    private void assertByteMatchesGolden(Map<String, String> output, Path goldenDir, String path)
            throws IOException {
        assertEquals(normalize(golden(goldenDir, path)), normalize(generated(output, path)),
                path + " must byte-match the frozen 9.83.0 golden");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
