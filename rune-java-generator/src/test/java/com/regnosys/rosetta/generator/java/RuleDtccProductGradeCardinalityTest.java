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
 * PR #291 — facet dtccProductGradeCardinality: a parser-side cardinality FOUNDATION (the #274
 * lineage). A DISGUISED 2-name navigation chain ({@code head -> feature}, parsed as an
 * {@code REnumValueRef} when the head did not resolve to a real enum) had NO case in
 * {@code CardinalityComputer.compute()} → defaulted SINGLE, so a reporting rule whose whole-output
 * navigates a multi disguised chain read SINGLE. ONE green-safe, thenAware-gated
 * (FUNCTION-byte-neutral) parser-side mechanism + one coupled correctness fix; 2 drr POJO Rule byte
 * flips. 1 source file ({@code CardinalityComputer}).
 *
 * <p><b>The bug.</b> The common reporting rule {@code DTCC_ProductGrade} body
 * {@code extract EconomicTermsForProduct(ProductForEvent) then filter Qualify_AssetClass_Commodity
 * then extract payout -> commodityPayout then extract ProductGradeReport{…}} navigates the disguised
 * chain {@code payout -> commodityPayout} ({@code commodityPayout} is {@code 0..*}), so the whole
 * output is MULTI. But {@code compute(REnumValueRef)} fell to {@code default → SINGLE}, so the rule
 * read SINGLE and the cftc/csa {@code extract commondtcc.DTCC_ProductGrade} delegations (which recurse
 * into the common body via the #273 {@code RRule} case) stayed SINGLE: golden renders
 * {@code ReportFunction<I, List<? extends ProductGradeReport>>} + the whole cascade
 * ({@code mapSingleToList}/{@code .getMulti()}, the prune stream map), the fork rendered the SINGLE
 * form (NON_COMPILING — the body wraps {@code MapperC.of} via #273/#274).
 *
 * <p><b>The fix.</b> {@code CardinalityComputer.disguisedChainCardinality} (a new thenAware-gated
 * {@code REnumValueRef} case) reads the leaf cardinality DIRECTLY off the parser's already-bound
 * resolution ({@code resolvedAttributeChain} / {@code resolvedInputFeature}) — no {@code gm} re-rooting
 * is needed (the generator's {@code disguisedChainProvesMulti} of #288 re-roots because it lacks the
 * parser's resolution context; here we have it); MULTI iff the head OR the leaf is multi. The
 * thenAware gate keeps the global {@code compute} (function tail) byte-frozen (#232). A COUPLED fix:
 * {@code isElementWiseThenBody} now excludes a FUNCTION-headed disguised chain
 * ({@code resolvedSymbol = RFunction}) — {@code <fn> -> <feature>} applies the function to the WHOLE
 * piped list and collapses it, so a multi argument does NOT propagate through it.
 *
 * <p><b>Green-safe by construction.</b> A green rule whose whole-output disguised chain is multi
 * rendered SINGLE on clean source (the chain read SINGLE) while golden renders MULTI, so it was an
 * already-waivered NON_COMPILING mismatch; recovering the correct MULTI cardinality only flips a
 * waivered file. byte-oracle / stash-baseline measured exactly 2 flips (drr POJO 438 → 436; 14
 * now-matching = 2 mine + 12 stale, clean-main re-dump = exactly the 12 stale, no DTCC); regscan 0
 * within-waiver regressions / 0 new / 2 flipped-out / 6 toward-golden + 2 neutral; all FUNCTION cells
 * (cdm5 81 / cdm6 236 / drr 207) + cdm/iso/fpml POJO byte-IDENTICAL.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED — the 2 flip locks
 * (cftc/csa DTCC_ProductGrade) fail on clean source; the boundary lock + the 2 green-safety locks
 * (BasketConstituentIdentifierSource csa/hkma) pass either way (the latter go RED only if the coupled
 * {@code isElementWiseThenBody} function-collapse fix is reverted while the cardinality arm is kept).
 */
class RuleDtccProductGradeCardinalityTest {

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

    // ==== Flip locks (revert-RED): the DTCC_ProductGrade delegations now read MULTI. ====

    /**
     * Flip — cftc DTCC_ProductGradeRule: {@code filter cftcTrade.IsAllowableActionForCFTC then extract
     * commondtcc.DTCC_ProductGrade}. The delegation's cardinality recurses into the common rule (the
     * #273 {@code RRule} case), whose whole-output disguised chain {@code payout -> commodityPayout}
     * now reads MULTI, so golden's {@code ReportFunction<I, List<? extends ProductGradeReport>>}
     * cascade ({@code .mapSingleToList(item -> MapperC.<ProductGradeReport>of(…)).getMulti()}, the
     * prune stream map) renders — matching golden.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccProductGrade_cftc_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/cftc/rewrite/dtcc/reports/DTCC_ProductGradeRule.java");
    }

    /**
     * Flip — csa DTCC_ProductGradeRule: the csa-regime sibling of the cftc delegation (same
     * {@code extract commondtcc.DTCC_ProductGrade} shape, same MULTI cardinality cascade).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccProductGrade_csa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/csa/rewrite/dtcc/reports/DTCC_ProductGradeRule.java");
    }

    // ==== Boundary / green-safety decline locks. ====

    /**
     * Boundary → FLIP (completed at PR #292). At #291 the COMMON DTCC_ProductGradeRule stayed
     * divergent: the cardinality fix moved it 27 → 2 diff-lines toward golden (correct MULTI signature
     * + cascade) but it was co-occupied with a lambda-param rename ({@code .mapItem(_item -> _item.…)}
     * in golden vs {@code .mapItem(item -> item.…)} in the fork) — pre-staged for a future
     * lambdaParamUS fix. PR #292's {@code lambdaParamItemEscape} resolved exactly that residual (the
     * inner {@code extract to-string} lambda now escapes {@code item -> _item} because the outer
     * {@code .mapItem(item -> ProductGradeReport{…})} bound {@code item}), so the common variant now
     * BYTE-MATCHES golden. This lock confirms the #291 cardinality foundation + the #292 lambda-param
     * escape compose to the byte-exact common output. (Also locked by
     * {@code RuleCleanFinisherComposeTest.dtccProductGrade_common_byteMatchesGolden}.)
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccProductGrade_common_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/dtcc/reports/DTCC_ProductGradeRule.java");
    }

    /**
     * Green-safety lock — csa BasketConstituentIdentifierSourceRule STAYS byte-matching golden. It
     * delegates {@code extract common.basket.BasketConstituentIdentifierSource}, whose common body is a
     * conditional ladder whose arms navigate the MULTI {@code security -> productIdentifier} disguised
     * chain THEN collapse via {@code then GetProductIdentifierFilteringISIN -> source} (a function
     * consuming the whole list, returning a single {@code ProductIdTypeEnum}). The dtccProductGrade
     * cardinality fix makes the conditional arms multi; the COUPLED {@code isElementWiseThenBody}
     * function-headed exclusion keeps the function-collapse single, so the whole rule stays SINGLE —
     * matching golden. Without the coupled fix this green rule regresses to a spurious
     * {@code ReportFunction<I, List<…>>} signature. This is the load-bearing green-safety guard for the
     * cardinality foundation's wide blast radius.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void basketConstituentIdentifierSource_csa_staysGreen() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/csa/rewrite/trade/reports/BasketConstituentIdentifierSourceRule.java");
    }

    /**
     * Green-safety lock — hkma BasketConstituentIdentifierSourceRule STAYS byte-matching golden (the
     * hkma-regime sibling of the csa green-safety guard, same delegation + function-collapse shape).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void basketConstituentIdentifierSource_hkma_staysGreen() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/hkma/rewrite/trade/reports/BasketConstituentIdentifierSourceRule.java");
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
                + path + " (PR #291 dtccProductGradeCardinality).");
    }

    private static void assertStaysDivergent(String path) throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String generated = drrOutput.get(path);
        assertNotNull(generated, "Class not generated: " + path);
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertFalse(normalize(golden).equals(normalize(generated)),
                path + " must STAY divergent (the dtccProductGradeCardinality fix corrects its "
                + "cardinality but it is co-occupied with a lambda-param rename).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
