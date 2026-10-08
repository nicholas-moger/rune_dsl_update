package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
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

/**
 * PR #343 anchor — the two-facet compose (4 FUNCTION flips: 3 cdm6 + 1 drr).
 *
 * <p><b>F1 — {@code deepPathLambdaNaming}</b> (3 cdm6 flips: Qualify_FullReturn,
 * Qualify_BuySellBack, Qualify_RepurchaseAgreement): a navigation step whose receiver is a
 * deep-feature call ({@code Product ->> economicTerms -> payout}) named its lambda var from
 * the raw feature-name fallback ({@code _payout}) because
 * {@code NavigationHandler.resolveLambdaVarName} had arms for the chained RFeatureCall /
 * list-op / disguised-chain receivers but NONE for RDeepFeatureCall (which extends
 * RExpression, not RFeatureCall). Golden names it from the deep feature's RETURN type — the
 * type the {@code ->>} navigates TO ({@code _economicTerms}), scope-disambiguated (the input
 * {@code economicTerms} collision gives golden's {@code _economicTerms}; a non-colliding
 * receiver stays bare). The new RDeepFeatureCall arm reads the SAME {@code resolveDeepFeature}
 * the {@code handle(RDeepFeatureCall)} witness/map-method paths render from, so naming and
 * witness cannot disagree. Green-safe: no golden carries the {@code _feature} fallback after a
 * deep-path step (corpus-verified), and the byte-green deep-path datarule siblings
 * (EconomicTermsQuantity / OptionPayoutClearedPhysicalSettlementExists) stay green (the full
 * D11 POJO kind reported 0 mismatch).
 *
 * <p><b>F2 — {@code objFallbackSortThenArg}</b> (1 drr flip: FindLatestAssignedIdentifier):
 * a hoisted {@code final Mapper*<Object> thenArg = <alias>.sort(...)} declaration stayed
 * {@code Object} because the objFallback element-recovery walks
 * ({@code NavigationHandler.thenArgElementLeaf} + {@code recoverThenArgItemRType}) unwrapped
 * then / extract / filter / list-op wrappers but NOT {@code RSortExpr} — the sort node
 * blocked the walk before it reached the alias-invocation leaf, so
 * {@code aliasDerivedThenArgItemType} never recovered the element type. Sort is
 * element-PRESERVING (reorder, same element type), so both walks gain an {@code RSortExpr}
 * arm recursing through {@code .argument()}, exactly like filter/list-op.
 * FindLatestAssignedIdentifier's {@code assignedIdentifiersWithVersion sort [item -> version]
 * then last} now declares golden's alias-derived
 * {@code MapperC<? extends AssignedIdentifier> thenArg}. Green-safe: Object-gated (a resolved
 * non-Object thenArg is untouched) and the alias-wildcard re-apply only fires on an alias leaf
 * whose signature element matches — golden always emits {@code ? extends X} for an alias-leaf
 * thenArg.
 *
 * <p>Whole-file byte comparisons run through the REAL D11 FUNCTION generation path and revert
 * RED without the facets (each flip carrier was NON_COMPILING or COMPILES-divergent pre-fix:
 * Qualify_* COMPILES-divergent — a lambda-var rename, legal Java but byte-different;
 * FindLatestAssignedIdentifier NON_COMPILING — {@code MapperC<Object>} into a
 * {@code MapperC<? extends AssignedIdentifier>} consumer).
 */
class DeepPathSortThenComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // F1 flip carriers (deep-path lambda-var naming).
    private static final String QUALIFY_FULL_RETURN =
            "cdm/event/qualification/functions/Qualify_FullReturn.java";
    private static final String QUALIFY_BUY_SELL_BACK =
            "cdm/product/qualification/functions/Qualify_BuySellBack.java";
    private static final String QUALIFY_REPURCHASE_AGREEMENT =
            "cdm/product/qualification/functions/Qualify_RepurchaseAgreement.java";
    // F2 flip carrier (sort-wrapped alias thenArg element recovery).
    private static final String FIND_LATEST_ASSIGNED_IDENTIFIER =
            "drr/regulation/common/functions/FindLatestAssignedIdentifier.java";

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
        if (cdm6CellAvailable()) {
            cdm6FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
        if (drrCellAvailable()) {
            drrFnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
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
        // Surface any per-function emission failures directly (Copilot R1): the flip
        // carriers all emit (D11 shows emitted==expected, missingOutput=0 for both cells,
        // so this list is empty today), but a future emission regression should fail here
        // with the underlying GenerationException(s) rather than downstream with a vague
        // "Class not generated" message.
        var errors = funcGen.generateWithErrors(output);
        assertTrue(errors.isEmpty(),
                () -> "Function generation reported " + errors.size() + " error(s): " + errors);
        return output;
    }

    // ==== F1 deepPathLambdaNaming flip locks (revert-RED) ====

    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyFullReturn_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, QUALIFY_FULL_RETURN);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyBuySellBack_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, QUALIFY_BUY_SELL_BACK);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyRepurchaseAgreement_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, QUALIFY_REPURCHASE_AGREEMENT);
    }

    /**
     * Positive-content lock: the post-deep-path step names its lambda var from the deep
     * feature's return type ({@code economicTerms}), never the pre-fix feature-name fallback
     * ({@code _payout -> _payout.getPayout()}). The negative witness counts 0 in the fixed
     * output AND in golden — it is a token the flip REMOVES.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyBuySellBack_cdm6_deepPathVarIsReceiverType() {
        assertNotNull(cdm6FnOutput);
        String gen = cdm6FnOutput.get(QUALIFY_BUY_SELL_BACK);
        assertNotNull(gen, "Qualify_BuySellBack not generated");
        assertTrue(gen.contains("_economicTerms -> _economicTerms.getPayout()"),
                "deep-path step must name its lambda var from the deep feature's return type");
        assertFalse(gen.contains("_payout -> _payout.getPayout()"),
                "the pre-fix feature-name fallback must be gone");
    }

    // ==== F2 objFallbackSortThenArg flip lock (revert-RED) ====

    @Test
    @EnabledIf("drrCellAvailable")
    void findLatestAssignedIdentifier_drr_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, FIND_LATEST_ASSIGNED_IDENTIFIER);
    }

    /**
     * Positive-content lock: the sort-wrapped alias thenArg declares the alias-derived
     * upper-bounded element type, never the pre-fix non-compiling {@code MapperC<Object>}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void findLatestAssignedIdentifier_drr_sortThenArgIsAliasElement() {
        assertNotNull(drrFnOutput);
        String gen = drrFnOutput.get(FIND_LATEST_ASSIGNED_IDENTIFIER);
        assertNotNull(gen, "FindLatestAssignedIdentifier not generated");
        assertTrue(gen.contains("MapperC<? extends AssignedIdentifier> thenArg"),
                "sort-wrapped alias thenArg must declare the alias-derived element type");
        assertFalse(gen.contains("MapperC<Object> thenArg"),
                "the pre-fix non-compiling Object erasure must be gone");
    }

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String path) throws IOException {
        assertNotNull(output, "generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "Class not generated: " + path
                + " (emission failed or the path differs)");
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " (PR #343 the two-facet compose).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
