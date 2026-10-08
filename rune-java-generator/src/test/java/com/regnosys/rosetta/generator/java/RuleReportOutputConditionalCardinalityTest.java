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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #289 — facet reportOutputConditionalCardinality: the report-output cardinality FOUNDATION
 * (the #272/#273/#274/#275 lineage). ONE green-safe, RULE-SCOPED (FUNCTION-byte-neutral) root-cause
 * fix, 4 drr POJO Rule byte flips.
 *
 * <p><b>The bug.</b> {@code CardinalityComputer} hard-coded an if-then-else EXPRESSION to
 * {@code SINGLE} ({@code case RConditionalExpr c -> SINGLE; // conservative}). A reporting rule
 * whose whole-output body is {@code filter … then if cond then <multi sub-rule> else <multi
 * sub-rule>} (csa NotionalAmount/QuantityScheduleLeg1/2 — the arms delegate to the multi
 * {@code cde.quantity.*} sub-rules) is therefore mis-computed SINGLE: golden renders
 * {@code ReportFunction<I, List<? extends NotionalPeriod>>} with {@code final MapperC<NotionalPeriod>
 * ifThenElseResult; … output = toBuilder(ifThenElseResult.getMulti());}, but the fork rendered the
 * SINGLE form ({@code ReportFunction<I, NotionalPeriod>}, {@code MapperS<NotionalPeriod>}
 * ifThenElseResult, {@code .get()}) while the conditional ARMS already wrap {@code MapperC.<X>of(…)}
 * (#273/#274) — a {@code MapperS<X> = MapperC.<X>of(…)} type mismatch (NON_COMPILING).
 *
 * <p><b>The fix (GENERATOR + parser, 3 source files).</b> (1) {@code CardinalityComputer}'s
 * {@code RConditionalExpr} arm becomes thenAware-cardinality-aware (the MAX of its arms, the
 * elseless synthetic empty-list else EXCLUDED so an {@code if cond then <single>} stays single).
 * (2) {@code FunctionExpressionRenderer.appendIteHoistChainCore} / {@code appendThenConditionalBlock}
 * gain a MULTI variant (MapperC decl + {@code .getMulti()} terminal), driven by the whole-output
 * {@code getRuleBodyCardinality == MULTI}. (3) {@code CollectionHandler.isBodyMulti} gates a
 * conditional EXTRACT body out of the now-cardinality-aware {@code computeRuleBody} consult — the
 * facet's scope is the whole-output then-body conditional, not the extract-method seat (a multi
 * conditional inside an extract lambda is a deferred block-lambda-for-multi sub-case, see
 * {@link #uniqueTransactionIdentifier_conditionalExtractBody_keepsMapSingleToItem}). All
 * rule-scoped / thenAware → the FUNCTION tail + cdm/iso/fpml POJO stay byte-IDENTICAL (the #232
 * lesson).
 *
 * <p><b>Green-safe by construction.</b> A green rule whose whole-output conditional has a multi arm
 * would render SINGLE on clean source (the conditional read SINGLE) while golden renders MULTI — so
 * it is already a waivered mismatch, never green; recovering the correct MULTI cardinality only
 * flips a waivered file. byte-oracle / stash-baseline measured exactly 4 flips (drr POJO 444 → 440;
 * 16 now-matching = 4 mine + 12 stale, clean-main re-dump = exactly the 12 stale), regscan 0
 * within-waiver regressions / 0 new mismatches / 5 toward-golden + 4 neutral churn; all FUNCTION
 * cells (cdm5 81 / cdm6 236 / drr 207) + cdm/iso/fpml POJO byte-IDENTICAL.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleReportOutputConditionalCardinalityTest {

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

    // ==== Flip locks (revert-RED): the whole-output conditional cardinality carriers now byte-match. ====

    /**
     * Flip — csa NotionalQuantityScheduleLeg1Rule: {@code filter … then if isCSAAligned then
     * notionalQuantityScheduleLeg1Rule else notionalQuantityScheduleLeg2Rule} (both arms multi
     * cde.quantity sub-rules). The conditional now proves MULTI, so the rule output is
     * {@code List<? extends NotionalPeriod>} and the ite-hoist declares {@code MapperC} +
     * {@code output = toBuilder(ifThenElseResult.getMulti())}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void notionalQuantityScheduleLeg1_csa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/csa/rewrite/trade/reports/NotionalQuantityScheduleLeg1Rule.java");
    }

    /** Flip — csa NotionalQuantityScheduleLeg2Rule: the Leg2 sibling (arms swapped). */
    @Test
    @EnabledIf("drrCellAvailable")
    void notionalQuantityScheduleLeg2_csa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/csa/rewrite/trade/reports/NotionalQuantityScheduleLeg2Rule.java");
    }

    /** Flip — csa NotionalAmountScheduleLeg1Rule: the NotionalAmount sibling (same conditional shape). */
    @Test
    @EnabledIf("drrCellAvailable")
    void notionalAmountScheduleLeg1_csa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/csa/rewrite/trade/reports/NotionalAmountScheduleLeg1Rule.java");
    }

    /** Flip — csa NotionalAmountScheduleLeg2Rule: the NotionalAmount Leg2 sibling. */
    @Test
    @EnabledIf("drrCellAvailable")
    void notionalAmountScheduleLeg2_csa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/csa/rewrite/trade/reports/NotionalAmountScheduleLeg2Rule.java");
    }

    // ==== Facet-boundary locks: the elseless over-fire guard + the deferred block-for-multi sub-case. ====

    /**
     * Green-safety / over-fire guard — UpiPreEnrichmentDataRule is GREEN: its body is
     * {@code if upiValidation absent … then extract [if Create_AnnaDsbUpiRequestFromReportableEvent(item)
     * -> requestType = ProductRequest then Create_AnnaDsbUpiRequestFromReportableEvent(item)]} — an
     * ELSELESS conditional whose then-arm is a SINGLE function ({@code Create_AnnaDsb…} output 0..1).
     * Without the elseless-else exclusion the synthetic empty-list else (an {@code RListLiteral},
     * which reads MULTI) would make the conditional MULTI, wrongly lifting the rule output to
     * {@code List<? extends AnnaDsbUpiRequestAndType>} (this regressed ~37 green enrichment/margin
     * rules in the first impl). The elseless exclusion keeps it SINGLE — byte-IDENTICAL to golden.
     * Locks the elseless over-fire guard.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void upiPreEnrichmentData_elselessConditional_staysSingleGreen() throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String path = "drr/enrichment/common/reports/UpiPreEnrichmentDataRule.java";
        String gen = drrOutput.get(path);
        assertNotNull(gen, "UpiPreEnrichmentDataRule not generated");
        assertTrue(gen.contains(
                        "implements ReportFunction<TransactionReportInstruction, AnnaDsbUpiRequestAndType>"),
                "the elseless conditional must stay SINGLE (bare AnnaDsbUpiRequestAndType output) — "
                + "the synthetic empty-list else must NOT make it List<? extends …>");
        assertFalse(gen.contains(
                        "implements ReportFunction<TransactionReportInstruction, List<? extends AnnaDsbUpiRequestAndType>>"),
                "the elseless over-fire (List output) must NOT occur");
        String golden = Files.readString(DRR_GOLDEN_DIR.resolve(path));
        assertEquals(normalize(golden), normalize(gen),
                "UpiPreEnrichmentDataRule must STAY byte-IDENTICAL to golden (elseless over-fire guard)");
    }

    /**
     * SUPERSEDED deferred-boundary lock (PR #339 breadcrumb) — the #289-deferred
     * block-lambda-for-multi sub-case FLIPPED: facet condBothArmsMultiCardinality's
     * {@code chainProvesMulti} RConditionalExpr arm (rule-scoped, both-arms-strict) now
     * reads the csa UniqueTransactionIdentifier conditional extract body MULTI, and the
     * MAPPER_C_EXPECTING effective-else block admission closes the #289 deferral reason
     * (the block no longer reverts to the inline ternary). The carrier byte-matches golden
     * — the whole-file flip lock lives in
     * {@link DeepThenChainComposeTest#utiCsaMargin_condBothArmsMultiCardinality_byteMatchesGolden}.
     * This breadcrumb keeps the ORIGINAL over-fire boundary observable: golden's
     * mapSingleToList block form is now the GEN form too.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void uniqueTransactionIdentifier_conditionalExtractBody_flippedToGoldenMultiForm() throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String path = "drr/regulation/csa/rewrite/margin/reports/UniqueTransactionIdentifierRule.java";
        String gen = drrOutput.get(path);
        assertNotNull(gen, "csa-margin UniqueTransactionIdentifierRule not generated");
        assertTrue(gen.contains(".mapSingleToList(item -> {"),
                "the both-arms-multi conditional extract body renders golden's BLOCK-lambda"
                + " mapSingleToList form (facet condBothArmsMultiCardinality, PR #339)");
        String golden = Files.readString(DRR_GOLDEN_DIR.resolve(path));
        assertEquals(normalize(golden), normalize(gen),
                "the #289-deferred carrier now byte-matches golden");
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
                + path + " (PR #289 reportOutputConditionalCardinality).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
