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
 * PR #385 — the nested-ladder/sum-chain/ctor-field septet (7 byte flips:
 * UnderlierIdOtherRule + UnderlierIdOtherSourceRule [drr POJO hkma] +
 * GetNetInitialMarginFromExposure [cdm6 FUNCTION] + GetReportableQuantityPeriodLeg1 +
 * GetReportableQuantityPeriodLeg2 + Create_SubmissionHarmonizedData cftc + csa
 * [drr FUNCTION]).
 *
 * <p><b>nestedThenIteLadder</b> (FunctionExpressionRenderer): a RULE-path k&gt;0
 * then-level conditional whose THEN branch is itself a one-level conditional (empty
 * inner else) renders golden's NESTED {@code if (c1) { if (c2) { ifThenElseResult =
 * <arm>; } else { ofNull } } else { ofNull }} ladder instead of declining to the
 * inline nested ternary — {@code isHoistableThenConditional} gains the
 * {@code allowNestedThen} admit (passed ONLY at the {@code renderThenExtractSetImpl}
 * k&gt;0 seat + its pre-scan twin, RULE-gated) and {@code appendIteHoistChainCore}
 * renders the 4-element nested rung. The naming cascade is automatic: the conditional
 * level leaves the thenArg group ({@code effectiveThenArgs} 2 → 1 → the bare
 * {@code thenArg} base decl) and the consumer lambda's {@code _thenArg0/1} unprefix
 * to {@code thenArg0/1} (the #381 no-collision cell).
 *
 * <p><b>aliasSumChainRoot</b> (FunctionAliasHelper + CollectionHandler): the
 * #384-reverted GetNetIM lead landed with both seats firing — the SIGNATURE walk
 * ({@code inferRTypeFromExpr}) gains the EXTRACT-keeps-its-body-type +
 * CONSTRUCTOR-types-as-constructed arms (the #358 F-B receiver arm's RType twin), so
 * the extract-constructor alias types as the disguised sum-chain root and all three
 * alias sigs render {@code MapperS<BigDecimal>} via the existing #328 SUM arm; the
 * RENDER seat recovers the numeric element through the SAME walk
 * ({@code aliasSumBodyElementOrNull} — the #178 same-walk law), scoped to a sum that
 * IS the alias body root (directly or as the last then-body), so {@code .sum()} →
 * the wrap-form {@code .sumBigDecimal()} ×3.
 *
 * <p><b>ctorFieldBareInvokableThenArg</b> (CollectionHandler + NavigationHandler): a
 * FUNCTION-path bare-invokable then-body admits when the WHOLE chain is a constructor
 * FIELD VALUE ({@code then.parent() instanceof RKeyValuePair} — the #368 F-A2
 * ctor-field walk and the gate move in lockstep), so the in-lambda mapItem chain
 * restructures together with its inner value-field chain (the #350-F5b law; the #339
 * consumer wrap {@code MapperS.of(fn.evaluate(thenArg.get()))} already existed); plus
 * the #360 {@code closureParamDisguisedLeaf} walk wires into
 * {@code recoverThenArgItemRType} (the k==0 Object decline lifts — basic leaves
 * included), {@code resolveReceiverDataType}'s evr arm (the hop-2 {@code <Date>}
 * witness) and {@code resolveLambdaVarName}'s evr arm (the {@code dateRange}
 * type-derived param), with the owner set widened to FILTER params (the #342 owner
 * set). The Create_SubmissionHarmonizedData pair are the all-or-nothing-guard
 * unblock riders: their ctor-field bare-invokable chains previously counted as
 * unhandled control flow, blocking the sibling thenArg hoists function-wide.
 *
 * <p>Whole-file byte locks run through the REAL D11 routes (drr full cell + drr/cdm6
 * FUNCTION) and revert RED without the facets; every witness token is
 * occurrence-counted (python {@code str.count} semantics — the #352 law) and
 * PRE-counted against f-probe-384post (each removal token PRE &ge; 1 / golden 0;
 * each golden token PRE 0 / golden &ge; 1).
 */
class NestedLadderSumChainCtorFieldSeptetComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String HKMA_UNDERLIER =
            "drr/regulation/hkma/rewrite/trade/reports/UnderlierIdOtherRule.java";
    private static final String HKMA_UNDERLIER_SOURCE =
            "drr/regulation/hkma/rewrite/trade/reports/UnderlierIdOtherSourceRule.java";
    private static final String GET_NET_IM =
            "cdm/margin/schedule/functions/GetNetInitialMarginFromExposure.java";
    private static final String QUANTITY_PERIOD_LEG1 =
            "drr/regulation/common/functions/GetReportableQuantityPeriodLeg1.java";
    private static final String QUANTITY_PERIOD_LEG2 =
            "drr/regulation/common/functions/GetReportableQuantityPeriodLeg2.java";
    private static final String SUBMISSION_CFTC =
            "drr/projection/dtcc/rds/harmonized/cftc/rewrite/trade/functions/"
                    + "Create_SubmissionHarmonizedData.java";
    private static final String SUBMISSION_CSA =
            "drr/projection/dtcc/rds/harmonized/csa/rewrite/trade/functions/"
                    + "Create_SubmissionHarmonizedData.java";

    private static Map<String, String> drrRuleOutput;
    private static Map<String, String> drrFnOutput;
    private static Map<String, String> cdm6FnOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            var drrSpec = new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT);
            drrRuleOutput = generateCell(drrSpec);
            drrFnOutput = generateFunctions(drrSpec);
        }
        if (cdm6CellAvailable()) {
            cdm6FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
    }

    /**
     * The D11-shaped POJO/rule generation path — generator wiring identical to
     * {@code D11CorpusRegressionTest}'s cell loop (same emission filter, same
     * {@code generators.doNotPrune} configuration — Copilot R1 #385 — same
     * generator classes and order). The harness-side generator-error capture and
     * the standalone function-file emission are deliberately omitted: this helper
     * serves only the two hkma POJO rule byte locks, whose renders are
     * byte-verified against golden below.
     */
    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell),
                D11CorpusRegressionTest.readDoNotPrune(cell));
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

    /** The REAL D11 FUNCTION-cell generation path. */
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

    // ==== byte locks (all 7 flips through the REAL D11 routes) ====

    /** nestedThenIteLadder: the nested-ladder + naming-cascade carrier. */
    @Test
    @EnabledIf("drrCellAvailable")
    void hkmaUnderlierIdOtherRule_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, HKMA_UNDERLIER);
    }

    /** nestedThenIteLadder: the source twin. */
    @Test
    @EnabledIf("drrCellAvailable")
    void hkmaUnderlierIdOtherSourceRule_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, HKMA_UNDERLIER_SOURCE);
    }

    /** aliasSumChainRoot: the sum-signature + sumBigDecimal carrier. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void getNetInitialMarginFromExposure_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, GET_NET_IM);
    }

    /** ctorFieldBareInvokableThenArg: the mapItem-decomposition carrier, Leg1. */
    @Test
    @EnabledIf("drrCellAvailable")
    void getReportableQuantityPeriodLeg1_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, QUANTITY_PERIOD_LEG1);
    }

    /** ctorFieldBareInvokableThenArg: the Leg2 twin. */
    @Test
    @EnabledIf("drrCellAvailable")
    void getReportableQuantityPeriodLeg2_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, QUANTITY_PERIOD_LEG2);
    }

    /** ctorFieldBareInvokableThenArg: the all-or-nothing unblock rider, cftc. */
    @Test
    @EnabledIf("drrCellAvailable")
    void submissionHarmonizedDataCftc_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, SUBMISSION_CFTC);
    }

    /** ctorFieldBareInvokableThenArg: the all-or-nothing unblock rider, csa. */
    @Test
    @EnabledIf("drrCellAvailable")
    void submissionHarmonizedDataCsa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, SUBMISSION_CSA);
    }

    // ==== occurrence-counted witnesses (python str.count semantics — the #352 law) ====

    /**
     * nestedThenIteLadder: the blank-final ladder decl (PRE 0 / golden 1), the typed
     * ofNull terminals — one per nesting level (PRE 0 / golden 2), the bare singleton
     * base thenArg (PRE 0 / golden 1), the UNPREFIXED in-lambda hoist (PRE 0 /
     * golden 1); negatives — the ternary-level thenArg1 decl (PRE 1 / golden 0), the
     * nested ternary's stacked empty arms (PRE 1 / golden 0), the escaped
     * {@code _thenArg0} (PRE 2 / golden 0).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void hkmaUnderlier_nestedLadderNamingCascade_witness() {
        String gen = drrRuleOutput.get(HKMA_UNDERLIER);
        assertNotNull(gen, "UnderlierIdOtherRule not generated");
        assertEquals(1, count(gen, "final MapperS<Product> ifThenElseResult;"),
                "the blank-final nested-ladder decl lands (PRE 0 / golden 1)");
        assertEquals(2, count(gen, "ifThenElseResult = MapperS.<Product>ofNull();"),
                "both nesting levels take the typed ofNull terminal (PRE 0 / golden 2)");
        assertEquals(1, count(gen,
                "final MapperS<TransactionReportInstruction> thenArg = MapperS.of(input)"),
                "the base decl collapses to the bare singleton thenArg (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                "final MapperC<ProductIdentifier> thenArg0 = MapperC.<ProductIdentifier>of("),
                "the in-lambda hoist unprefixes to thenArg0 (PRE 0 / golden 1)");
        assertEquals(0, count(gen, "final MapperS<Product> thenArg1 = "),
                "the inline-ternary level decl must be gone (PRE 1 / golden 0)");
        assertEquals(0, count(gen, "MapperC.of() : MapperC.of();"),
                "the nested ternary's stacked empty arms must be gone (PRE 1 / golden 0)");
        assertEquals(0, count(gen, "_thenArg0"),
                "the escaped in-lambda names must be gone (PRE 2 / golden 0)");
    }

    /**
     * aliasSumChainRoot: the BigDecimal alias signature (PRE 0 / golden 1), the
     * wrap-form typed sum (PRE 0 / golden 3); negatives — the function-output
     * signature leak (PRE 2 / golden 0) and the inline generic {@code .sum()}
     * (PRE 3 / golden 0).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void getNetIM_sumSignatureAndRender_witness() {
        String gen = cdm6FnOutput.get(GET_NET_IM);
        assertNotNull(gen, "GetNetInitialMarginFromExposure not generated");
        assertEquals(1, count(gen,
                "protected abstract MapperS<BigDecimal> totalGIM(Exposure exposure);"),
                "the sum alias signature types MapperS<BigDecimal> (PRE 0 / golden 1)");
        assertEquals(3, count(gen, ".sumBigDecimal();"),
                "all three sums render the wrap-form typed variant (PRE 0 / golden 3)");
        assertEquals(0, count(gen,
                "MapperS<? extends StandardizedScheduleInitialMargin> totalGIM"),
                "the function-output signature leak must be gone (PRE 2 / golden 0)");
        assertEquals(0, count(gen, ".sum();"),
                "the inline generic .sum() must be gone (PRE 3 / golden 0)");
    }

    /**
     * ctorFieldBareInvokableThenArg (Leg1): the in-lambda ctor-field thenArg hoist —
     * one per outer-conditional arm (PRE 0 / golden 2), the #339 consumer wrap at the
     * setValue seat (PRE 0 / golden 2), the hop-2 {@code <Date>} witness + dateRange
     * param (PRE 0 / golden 2), the outer decomposition's thenArg0 decl (PRE 0 /
     * golden 1); negatives — the runtime {@code .then(} field form (PRE 2 /
     * golden 0) and the witness-less feature-named hop (PRE 2 / golden 0).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void quantityPeriodLeg1_ctorFieldDecompositionAndWalk_witness() {
        String gen = drrFnOutput.get(QUANTITY_PERIOD_LEG1);
        assertNotNull(gen, "GetReportableQuantityPeriodLeg1 not generated");
        assertEquals(2, count(gen, "final MapperS<BigDecimal> thenArg = quantityPeriod"
                        + ".<BigDecimal>map(\"getValue\", datedValue -> datedValue.getValue());"),
                "the ctor-field base hoists in-lambda, one per arm (PRE 0 / golden 2)");
        assertEquals(2, count(gen, ".setValue(MapperS.of(formatToShortFraction5DecimalNumber"
                        + ".evaluate(thenArg.get())).get())"),
                "the #339 bare-fn consumer wrap lands at the setValue seat (PRE 0 / golden 2)");
        assertEquals(2, count(gen, ".<Date>map(\"getStartDate\", dateRange -> "
                        + "dateRange.getStartDate())"),
                "the hop-2 witness + type-derived param land (PRE 0 / golden 2)");
        assertEquals(1, count(gen, "final MapperC<SchedulePeriod> thenArg0 = "
                        + "customizedSchedule(trade)"),
                "the outer mapItem chain decomposes to the block lambda (PRE 0 / golden 1)");
        assertEquals(0, count(gen, ".then(item -> formatToShortFraction5DecimalNumber"
                        + ".evaluate(item.get()))"),
                "the runtime .then( field form must be gone (PRE 2 / golden 0)");
        assertEquals(0, count(gen, ".map(\"getStartDate\", calculationPeriod -> "
                        + "calculationPeriod.getStartDate())"),
                "the witness-less feature-named hop must be gone (PRE 2 / golden 0)");
    }

    /**
     * ctorFieldBareInvokableThenArg (the cftc unblock rider): the previously-blocked
     * sibling hoist (PRE 0 / golden 1) and the renumbered frequency hoist (PRE 0 /
     * golden 1); negative — the old thenArg0 binding of the frequency chain (PRE 1 /
     * golden 0: the sibling unblock renumbers it to thenArg3).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void submissionCftc_allOrNothingUnblock_witness() {
        String gen = drrFnOutput.get(SUBMISSION_CFTC);
        assertNotNull(gen, "Create_SubmissionHarmonizedData (cftc) not generated");
        assertEquals(1, count(gen, "final MapperS<PriceNotationEnum> thenArg0 = "
                        + "MapperS.of(drrReport).<PriceNotationEnum>map(\"getPriceNotation\""),
                "the previously-blocked sibling chain hoists (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final MapperS<FrequencyPeriodEnum> thenArg3 = "),
                "the frequency hoist renumbers behind the unblocked siblings"
                        + " (PRE 0 / golden 1)");
        assertEquals(0, count(gen, "final MapperS<FrequencyPeriodEnum> thenArg0 = "),
                "the old lone-hoist numbering must be gone (PRE 1 / golden 0)");
    }

    // ==== helpers ====

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String relPath) throws IOException {
        assertNotNull(output, "cell not generated");
        String gen = output.get(relPath);
        assertNotNull(gen, relPath + " not generated");
        Path goldenPath = goldenDir.resolve(relPath);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath).replace("\r\n", "\n").replace("\r", "\n");
        assertEquals(golden, gen.replace("\r\n", "\n").replace("\r", "\n"),
                relPath + " must byte-match golden");
    }

    /** Occurrence count — python str.count semantics (the #352 law). */
    private static int count(String haystack, String needle) {
        int n = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) >= 0) {
            n++;
            idx += needle.length();
        }
        return n;
    }
}
