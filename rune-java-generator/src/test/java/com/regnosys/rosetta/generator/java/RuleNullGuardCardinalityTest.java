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
 * PR #310 — facet nullGuardCardinality: ONE green-safe GENERATOR mechanism (parser UNTOUCHED),
 * 1 drr POJO byte flip; 1 source file ({@code NavigationHandler}). The user-chosen null-guard
 * cardinality lever — a genuine cardinality FOUNDATION at the {@code chainRendersMapperC} seat
 * (the #282/#286/#288 disguised-chain-off-the-item lineage).
 *
 * <p><b>The bug.</b> A meta-annotated navigation LEAF whose receiver is a DISGUISED 2-name chain
 * ({@code partyInformation -> regimeInformation}, an {@link com.regnosys.rosetta.ast.model.REnumValueRef}
 * rooted on the implicit ITEM — both features off {@code ReportableInformation}, not a function
 * input/output/shortcut) reads SINGLE at the {@code NavigationHandler.chainRendersMapperC} seat: its
 * 1-arg {@code resolveDisguisedFeature} resolves a disguised head only against function-scoped names,
 * so the item-rooted head is unresolved and the chain wrongly reads single. The meta leaf therefore
 * renders a {@code MapperS} result type, and {@code WrappedItemCoercer}'s MapperS arm emits the
 * null-guarded {@code fieldWithMetaSupervisoryBodyEnum == null ? null : fieldWithMetaSupervisoryBodyEnum.getValue()}
 * where golden bares {@code fieldWithMetaSupervisoryBodyEnum.getValue()} (its MapperC arm — a
 * MapperC's items are non-null) for the genuinely-multi receiver (the chain renders an uncollapsed
 * {@code item.mapC("getPartyInformation").mapC("getRegimeInformation").map("getSupervisoryBody")} at
 * runtime). The spurious null-guard COMPILES (the fork's {@code fieldWithMetaSupervisoryBodyEnum ==
 * null ? null : fieldWithMetaSupervisoryBodyEnum.getValue()} is a valid, more-null-safe lambda body)
 * but byte-differs from golden's bare {@code fieldWithMetaSupervisoryBodyEnum.getValue()}, so the
 * carrier was a COMPILES_DIVERGENT waivered mismatch (Copilot R1 #310: corrected from the earlier
 * "does not compile" — the flip moves COMPILES_DIVERGENT 149 → 148, functional 93.25% HELD).
 *
 * <p><b>The fix.</b> {@code chainRendersMapperC}'s {@code REnumValueRef} branch falls back, after the
 * function-scoped resolvers decline, to the #288 {@code disguisedChainProvesMulti} re-root (the same
 * one {@code chainProvesMulti}'s REnumValueRef arm uses): re-root the disguised chain onto its item
 * type and read head/leaf cardinality. RULE-scoped ({@code findEnclosingRule}) → FUNCTION-byte-neutral
 * (#232); monotone (only ADDs multi — a resolved head keeps its legacy answer); green-safe (a
 * guard-vs-bare divergence on a provably-multi chain is already a waivered mismatch — the fork
 * guards, golden bares).
 *
 * <p><b>Green-safe by construction.</b> The fix only widens the wrapper kind of a META leaf whose
 * receiver is a provably-MULTI item-rooted disguised chain. A per-element item receiver (a filter
 * item of a {@code mapItemToList} step, an {@link com.regnosys.rosetta.ast.model.RImplicitVariable},
 * NOT a disguised {@code REnumValueRef}) never enters the widened branch, so a genuinely-single
 * receiver keeps its null-guard (the GREEN {@code PortfolioContainingNonReportedComponentIndicatorRule}
 * navigates {@code partyInformation}/{@code regimeInformation} in SEPARATE mapSingleToList/mapItemToList
 * steps, so its {@code getSupervisoryBody} receiver is a per-element ReportingRegime — MapperS,
 * guard KEPT — and stays byte-matching golden). byte-oracle / stash-baseline measured exactly 1
 * (drr POJO 363 → 362; 13 now-matching = 1 mine + 12 stale, clean re-dump = exactly the 12 stale);
 * regscan 0 within-waiver regressions / 0 new / 1 flipped-out; all FUNCTION cells (cdm5 79 / cdm6
 * 232 / drr 206) + cdm/iso/fpml POJO byte-IDENTICAL.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED 2/4 — the 1 flip lock +
 * the 1 positive-content lock fail on clean source; the 2 green-safety locks pass either way.
 */
class RuleNullGuardCardinalityTest {

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

    // ==== Flip lock (revert-RED): the 1 carrier now byte-matches golden. ====

    /** Flip — DTCC_LargeNotionalOffFacilitySwapElectionIndicator (nullGuardCardinality). */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccLargeNotional_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/cftc/rewrite/dtcc/reports/DTCC_LargeNotionalOffFacilitySwapElectionIndicatorRule.java");
    }

    // ==== Positive-content lock (revert-RED): the rendered facet form, not just byte-match. ====

    /**
     * The item-rooted disguised chain {@code partyInformation -> regimeInformation} renders an
     * uncollapsed {@code mapC.mapC}, so the meta leaf's coercion is BARE (MapperC arm) — NOT the
     * null-guarded MapperS form.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccLargeNotional_rendersBareMapperCMetaDeref() {
        String gen = gen(
                "drr/regulation/cftc/rewrite/dtcc/reports/DTCC_LargeNotionalOffFacilitySwapElectionIndicatorRule.java");
        assertTrue(gen.contains(
                ".<ReportingRegime>mapC(\"getRegimeInformation\", partyInformation -> partyInformation.getRegimeInformation())"
                + ".<FieldWithMetaSupervisoryBodyEnum>map(\"getSupervisoryBody\", reportingRegime -> reportingRegime.getSupervisoryBody())"
                + ".<SupervisoryBodyEnum>map(\"Type coercion\", fieldWithMetaSupervisoryBodyEnum -> fieldWithMetaSupervisoryBodyEnum.getValue())"),
                "Expected the multi item-rooted disguised chain to bare the meta-deref (MapperC arm)");
        assertTrue(!gen.contains(
                "fieldWithMetaSupervisoryBodyEnum -> fieldWithMetaSupervisoryBodyEnum == null ? null : fieldWithMetaSupervisoryBodyEnum.getValue()"),
                "The multi receiver must NOT emit the null-guarded MapperS form");
    }

    // ==== Green-safety / decline locks. ====

    /**
     * Green-safety (regression-prevention) — the GREEN
     * {@code PortfolioContainingNonReportedComponentIndicatorRule} navigates {@code partyInformation}
     * / {@code regimeInformation} in SEPARATE {@code mapSingleToList}/{@code mapItemToList} steps, so
     * its {@code getSupervisoryBody} receiver is a per-element ReportingRegime item (an
     * {@code RImplicitVariable}, NOT a disguised {@code REnumValueRef}) — MapperS, so golden KEEPS the
     * {@code fieldWithMetaSupervisoryBodyEnum == null ? null : …} guard. The fix's REnumValueRef
     * branch never fires on it, so it STAYS byte-matching golden (the null-guard is not stripped).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void portfolioContainingNonReported_singleItemKeepsGuard_staysGreen() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/asic/rewrite/margin/reports/PortfolioContainingNonReportedComponentIndicatorRule.java");
    }

    /**
     * Green-safety — DTCC_MandatoryClearingIndicatorRule (GREEN) STAYS byte-matching golden: a
     * confirmation the RULE-scoped monotone widening does not disturb the surrounding drr rule family.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccMandatoryClearing_staysGreen() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/cftc/rewrite/dtcc/reports/DTCC_MandatoryClearingIndicatorRule.java");
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
                + path + " (PR #310 nullGuardCardinality).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
