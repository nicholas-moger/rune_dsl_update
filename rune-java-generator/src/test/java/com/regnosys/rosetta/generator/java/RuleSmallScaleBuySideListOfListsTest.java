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
 * PR #293 — smallScaleBuySideListOfLists: the {@code MapperListOfLists} THIRD cardinality state
 * (the user-chosen NEW mechanism) + a coupled feature-call-chain cardinality lift. 7 drr POJO Rule
 * byte flips, 3 source files ({@code NavigationHandler} + {@code CollectionHandler} +
 * {@code FunctionExpressionRenderer}).
 *
 * <p><b>Background.</b> The fork's cardinality model has only two states — single ({@code MapperS})
 * and multi ({@code MapperC}). A {@code then extract X} over a MULTI receiver whose body is also
 * MULTI selects {@code mapItemToList} (already correct in {@link
 * com.regnosys.rosetta.generator.java.expression.handlers.CollectionHandler#mapMethod}), which at
 * runtime returns a {@code MapperListOfLists<X>} (a list per element). The fork declared that
 * {@code thenArg} as the NON-compiling {@code MapperC<X>}, so every such rule was waivered.
 *
 * <p><b>(1) MapperListOfLists decl ({@code FunctionExpressionRenderer.renderThenExtractSet}).</b>
 * When {@code CollectionHandler.producesListOfLists} (= {@code mapMethod == "mapItemToList"}) the
 * {@code thenArg} declares {@code MapperListOfLists<X>}. Reuses {@code mapMethod} as the single
 * source of truth so the declared wrapper and the rendered method cannot disagree (the #274
 * two-halves-agree pattern). Green-safe by construction: a {@code MapperC<X> = <chain>.mapItemToList(
 * ...)} assignment never compiled, so every carrier was an already-waivered mismatch. A SHARED seat
 * (function + rule): 5 drr FUNCTION cells move toward-golden ({@code MapperC} → {@code
 * MapperListOfLists}, 0 regressions) — NOT byte-neutral, but green-safe.
 *
 * <p><b>(2) feature-call-chain cardinality.</b> {@code CollectionHandler.isBodyMulti} now consults
 * the gm-aware {@code NavigationHandler.chainProvesMulti} for an {@code RFeatureCall} body too (not
 * only the #288 disguised {@code REnumValueRef}), and {@code chainProvesMulti} gains a RULE-scoped
 * {@code RSymbolReference → RFunction} arm — a function CALL used as a navigation RECEIVER carries
 * its OUTPUT cardinality, which the parser's {@code computeFeatureCallCardinality} misses (it reads
 * only the navigated LEAF). Both RULE-scoped + monotone, so the FUNCTION tail's cardinality stays
 * byte-frozen. This lifts a {@code mapSingleToItem}/{@code MapperS} body to {@code mapSingleToList}/
 * {@code MapperC} when the body's feature-call chain is multi via a mid-chain or function-call
 * receiver.
 *
 * <p>byte-oracle / stash-baseline measured exactly 7 flips (drr POJO 434 → 427; 19 now-matching = 7
 * mine + 12 stale, clean-source re-dump = exactly the 12 stale); regscan 0 within-waiver regressions
 * / 0 new mismatches / 7 flipped-out / 14 toward-golden + 6 neutral across all 8 cells; FUNCTION
 * cells (cdm5 81 / cdm6 236 / drr 207) count UNCHANGED.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen goldens
 * (newline-normalized), generated through the REAL {@link
 * D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED (the 7 flip locks fail on clean
 * source; the green-safety + decline locks pass either way).
 */
class RuleSmallScaleBuySideListOfListsTest {

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

    // ==== MapperListOfLists flip locks (revert-RED): MapperC<X> -> MapperListOfLists<X>. ====

    /**
     * Flip — asic-margin SmallScaleBuySideEntityIndicatorRule. The named carrier:
     * {@code … reportableInformation -> partyInformation (multi) then extract regimeInformation
     * (multi)} selects {@code mapItemToList} (multi receiver + multi body), so the {@code thenArg}
     * is {@code MapperListOfLists<ReportingRegime>} (golden), not the non-compiling
     * {@code MapperC<ReportingRegime>} (fork). REVERT-RED.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void smallScaleBuySide_asicMargin_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/asic/rewrite/margin/reports/SmallScaleBuySideEntityIndicatorRule.java");
    }

    /**
     * Flip — asic-margin PortfolioContainingNonReportedComponentIndicatorRule. Same
     * {@code partyInformation then extract regimeInformation} list-of-lists shape →
     * {@code MapperListOfLists<ReportingRegime>}. A co-occupied carrier the MapperListOfLists
     * mechanism flips (the aim-bigger compounding). REVERT-RED.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void portfolioContaining_asicMargin_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/asic/rewrite/margin/reports/PortfolioContainingNonReportedComponentIndicatorRule.java");
    }

    /**
     * Flip — csa-dtcc DTCC_ExecutionVenueIDRule. A multi-receiver multi-body {@code thenArg4}
     * ({@code MapperListOfLists<PartyIdentifier>}). REVERT-RED.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccExecutionVenueId_csa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/csa/rewrite/dtcc/reports/DTCC_ExecutionVenueIDRule.java");
    }

    /**
     * Flip — csa-dtcc DTCC_ExecutionVenueIDTypeRule (the type sibling of the above,
     * {@code MapperListOfLists<PartyIdentifier>}). REVERT-RED.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccExecutionVenueIdType_csa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/csa/rewrite/dtcc/reports/DTCC_ExecutionVenueIDTypeRule.java");
    }

    /**
     * Flip — iosco-cde-v1 PlatformIdentifierRule ({@code MapperListOfLists<PartyIdentifier>}).
     * REVERT-RED.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void platformIdentifier_ioscoV1_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version1/execution/reports/PlatformIdentifierRule.java");
    }

    // ==== feature-call-chain cardinality-lift flip locks (revert-RED). ====

    /**
     * Flip — asic-trade SmallScaleBuySideEntityIndicatorRule. The body {@code
     * common.party.ExtractRegimeInformation(item, item -> reportingSide -> reportingParty) ->
     * asicPartyInformation} is a feature call whose RECEIVER is a MULTI function call
     * (ExtractRegimeInformation returns a List); the single leaf {@code asicPartyInformation} hid
     * the multi from {@code computeFeatureCallCardinality}. The {@code chainProvesMulti}
     * RFunction-receiver arm + the {@code isBodyMulti} RFeatureCall arm lift the body to multi, so a
     * SINGLE receiver (the filtered {@code thenArg0}) selects {@code mapSingleToList}/{@code MapperC}
     * (golden), not {@code mapSingleToItem}/{@code MapperS} (fork). REVERT-RED.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void smallScaleBuySide_asicTrade_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/asic/rewrite/trade/reports/SmallScaleBuySideEntityIndicatorRule.java");
    }

    /**
     * Flip — iosco-cde-v1-link PackageIdentifierRule. The extract body {@code getListId ->
     * assignedIdentifier (multi) -> identifier} is multi via a MID-chain multi feature; the
     * leaf-only {@code computeFeatureCallCardinality} read it SINGLE. {@code isBodyMulti}'s new
     * {@code RFeatureCall} consult of {@code chainProvesMulti} (whose receiver recursion already
     * caught mid-chain multi) lifts {@code mapSingleToItem} → {@code mapSingleToList}. REVERT-RED.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void packageIdentifier_ioscoV1_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version1/link/reports/PackageIdentifierRule.java");
    }

    // ==== Green-safety + decline locks (pass on clean source AND with the fix). ====

    /**
     * Green-safety — cftc-dtcc DTCC_MandatoryClearingIndicatorRule. A MULTI-receiver extract with a
     * SINGLE body renders {@code .mapItem(...)} → {@code MapperC<...>} (NOT {@code mapItemToList}/
     * {@code MapperListOfLists}). Locks that {@code producesListOfLists} returns FALSE for a
     * multi-receiver SINGLE-body extract (the {@code mapItem} branch — the load-bearing
     * MapperListOfLists discriminator) AND that {@code isBodyMulti}'s RFeatureCall arm does not
     * over-fire on a genuinely-single body. Passes on clean source too (a pure green-safety guard).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccMandatoryClearingIndicator_mapItemStaysMapperC() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/cftc/rewrite/dtcc/reports/DTCC_MandatoryClearingIndicatorRule.java");
    }

    /**
     * CONVERTED at PR #361 (facet ruleRuntimeThenHoist — the deferred-carrier law: this lock's own
     * javadoc named "the lambda-interior then-hoist (the deferred #282/#262-scale foundation)" as
     * the missing piece, and the #361 RULE-path single-conditional then-extract consumer admit at
     * {@code thenChainHasUnhandledControlFlow} IS that unfreeze for this shape). The third
     * SmallScaleBuySide variant nests the whole {@code partyInformation … then extract
     * regimeInformation -> asicPartyInformation then flatten then only-element then extract …}
     * pipeline INSIDE a {@code .mapSingleToItem(reportInstruction -> { … })} lambda; golden hoists
     * the inner pipe into {@code thenArg0..thenArg4} statements (with {@code thenArg2} a
     * {@code MapperListOfLists}) — which the fork now renders byte-identically (the cp1 GROUP-H
     * flip). The former decline lock ("stays divergent") flips to the byte-equality lock.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void smallScaleBuySide_asicValuation_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/asic/rewrite/valuation/reports/SmallScaleBuySideEntityIndicatorRule.java");
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
                + path + " (PR #293 smallScaleBuySideListOfLists).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
