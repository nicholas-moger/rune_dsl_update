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
 * PR #309 — facets lambdaInteriorThenHoist (A) + builderListEquals (C): TWO disjoint green-safe
 * GENERATOR mechanisms (parser UNTOUCHED), 5 drr POJO byte flips; 2 source files
 * ({@code CollectionHandler} + {@code ModelObjectGenerator}). The user-chosen clean compose; the
 * third candidate (B whole-output MULTI meta-deref, OriginalSwapUTIRule common) was DROPPED —
 * honest-refute, its disguised-chain wrapper recovery flipped 0 (the #288 disguised-chain
 * territory).
 *
 * <ul>
 *   <li><b>A lambdaInteriorThenHoist</b> (FloatingRateReferencePeriodOfLeg{1,2}{Multiplier,
 *       TimePeriod} esma): the #282/#294-deferred foundation. Inside a {@code mapSingleToItem} block
 *       lambda the statement-hoist sink walk STOPS at the lambda boundary, so a SINGLE {@code .then(}
 *       declined to the inline runtime form. {@code CollectionHandler.tryDeepThenHoist} now hoists
 *       its {@code final MapperS<X> thenArg = <receiver>;} through the LAMBDA_CHANNEL
 *       ({@code registerPendingLambdaHoist}, which {@code compileLambda} drains into the block lambda
 *       in registration order — the then's arg compiles before the sibling Integer-coercion arg, so
 *       the thenArg decl lands first, matching golden) + re-roots the then-body on {@code thenArg}.
 *       Gated n==1 + isValueThen + isInsideEnclosingLambda so the broad sink-opening (un-freezing
 *       ifThenElseResult/boolHoist inside every lambda) and the multi-level boolean-then cascade are
 *       avoided.</li>
 *   <li><b>C builderListEquals</b> (CommonLeg): the BUILDER equals() compares builderProps (=
 *       allProps when the builder is NON-extended due to type-changing overrides), so it lists an
 *       INHERITED list prop the impl-only ListEquals import gate (keyed on implProps) misses.
 *       {@code ModelObjectGenerator} adds {@code com.rosetta.util.ListEquals} when builderProps
 *       carries any list — the identical {@code typeUtil.isList} check the builder equals() uses,
 *       idempotent for the impl-gate case.</li>
 * </ul>
 *
 * <p><b>Green-safe by construction.</b> (A) ZERO of the 34,686 goldens carry the inline runtime
 * {@code .then(}, so every then-carrier is an already-waivered non-compiling mismatch. (C) a
 * used-but-unimported ListEquals never compiled, and the gate fires exactly when the builder
 * equals() emits the call (no over-import). byte-oracle / stash-baseline measured exactly 5 (drr
 * POJO 368 → 363; 17 now-matching = 5 mine + 12 stale, clean re-dump = exactly the 12 stale);
 * regscan 0 within-waiver regressions / 0 new / 5 flipped-out; all FUNCTION cells (cdm5 79 / cdm6
 * 232 / drr 206) + cdm/iso/fpml POJO byte-IDENTICAL.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED 7/9 — the 5 flip locks +
 * the 2 positive-content locks fail on clean source; the 2 green-safety locks pass either way.
 */
class RuleLambdaInteriorThenHoistTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> drrOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            drrOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
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
        funcGen.generate(output);
        return output;
    }

    // ==== Flip locks (revert-RED): the 5 carriers now byte-match golden. ====

    /** Flip — FloatingRateReferencePeriodOfLeg1Multiplier (A lambdaInteriorThenHoist). */
    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRateReferencePeriodOfLeg1Multiplier_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/esma/emir/refit/trade/reports/FloatingRateReferencePeriodOfLeg1MultiplierRule.java");
    }

    /** Flip — FloatingRateReferencePeriodOfLeg1TimePeriod (A). */
    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRateReferencePeriodOfLeg1TimePeriod_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/esma/emir/refit/trade/reports/FloatingRateReferencePeriodOfLeg1TimePeriodRule.java");
    }

    /** Flip — FloatingRateReferencePeriodOfLeg2Multiplier (A). */
    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRateReferencePeriodOfLeg2Multiplier_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/esma/emir/refit/trade/reports/FloatingRateReferencePeriodOfLeg2MultiplierRule.java");
    }

    /** Flip — FloatingRateReferencePeriodOfLeg2TimePeriod (A). */
    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRateReferencePeriodOfLeg2TimePeriod_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/esma/emir/refit/trade/reports/FloatingRateReferencePeriodOfLeg2TimePeriodRule.java");
    }

    /** Flip — CommonLeg (C builderListEquals). */
    @Test
    @EnabledIf("drrCellAvailable")
    void commonLeg_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/common/trade/CommonLeg.java");
    }

    // ==== Positive-content locks (revert-RED): the rendered facet form, not just byte-match. ====

    /**
     * A — FloatingRateReferencePeriodOfLeg1Multiplier hoists the then-receiver to
     * {@code final MapperS<PeriodEnum> thenArg = …;} and re-roots the then-body on {@code thenArg}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRateReferencePeriod_rendersLambdaInteriorThenHoist() {
        String gen = gen(
                "drr/regulation/esma/emir/refit/trade/reports/FloatingRateReferencePeriodOfLeg1MultiplierRule.java");
        assertTrue(gen.contains(
                "final MapperS<PeriodEnum> thenArg = MapperS.of(rateOption.evaluate(item.get()))"
                + ".<Period>map(\"getIndexTenor\", floatingRateOption -> floatingRateOption.getIndexTenor())"
                + ".<PeriodEnum>map(\"getPeriod\", period -> period.getPeriod());"),
                "Expected the lambda-interior then-receiver hoisted to a final MapperS<PeriodEnum> thenArg local");
        assertTrue(gen.contains("adjustPeriodMultiplier.evaluate(thenArg"),
                "Expected the then-body re-rooted on thenArg (the inline runtime .then( gone)");
    }

    /** C — CommonLeg imports com.rosetta.util.ListEquals (the builder equals() needs it). */
    @Test
    @EnabledIf("drrCellAvailable")
    void commonLeg_importsListEquals() {
        String gen = gen("drr/regulation/common/trade/CommonLeg.java");
        assertTrue(gen.contains("import com.rosetta.util.ListEquals;"),
                "Expected the inherited-list-prop builder equals() to import com.rosetta.util.ListEquals");
    }

    // ==== Green-safety / decline locks. ====

    /**
     * FLIPPED at PR #379 (facet crThenCondBlockAdmit, A — this lock converts): the
     * higher-level restructure its pre-#379 javadoc anticipated landed WHOLE. The
     * else-arm chain admits at the #361 rule-path consumer arm (its ladder's only ctl is
     * the CR-terminal {@code partyInformation filter […] then exists} inside the
     * condition — {@code condCtlIsAdmissibleCrThenOnly}); the name-taken decline relaxes
     * so the method group renumbers ({@code thenArg0} the filter step, {@code thenArg1}
     * the in-branch level hoist); the consumer compiles under the single-slot window so
     * the in-lambda channel hoists the condition's chain as the per-lambda
     * {@code final MapperC<PartyInformation> thenArg = …;} and re-roots
     * {@code ComparisonResult.ofNullSafe(exists(thenArg).asMapper())}; the elseless
     * block form drains the cond-position DeepThenArgHoist at BLOCK TOP with the typed
     * {@code MapperS.<Boolean>ofNull()} fall-through. The whole file (and its csa twin
     * PlatformAnonymousExecutionIndicatorRule) is now BYTE-IDENTICAL to golden.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccSefOrDcmAnonymous_comparisonResultThen_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/cftc/rewrite/dtcc/reports/DTCC_SEFOrDCMAnonymousExecutionIndicatorRule.java");
    }

    /** Flip twin (PR #379, A) — the csa PlatformAnonymousExecutionIndicatorRule. */
    @Test
    @EnabledIf("drrCellAvailable")
    void platformAnonymousExecutionIndicator_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/csa/rewrite/trade/reports/PlatformAnonymousExecutionIndicatorRule.java");
    }

    /**
     * Green-safety (C over-import guard) — PartyInformation.java (GREEN, a core drr type that
     * imports {@code com.rosetta.util.ListEquals} for its list-prop equals()) STAYS byte-matching
     * golden: the builderListEquals gate is idempotent where ListEquals is already collected.
     * Confirms the gate does not over-import (it fires exactly when the builder equals() emits the
     * ListEquals call).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void partyInformation_listEqualsIdempotent_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/common/PartyInformation.java");
    }

    private static String gen(String path) {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String g = drrOutput.get(path);
        assertNotNull(g, "Class not generated: " + path);
        return g;
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String generated = drrOutput.get(path);
        assertNotNull(generated, "Class not generated: " + path
                + " (emission failed or the path differs)");
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated drr output must byte-match the golden (newline-normalized) for "
                + path + " (PR #309 lambdaInteriorThenHoist + builderListEquals).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
