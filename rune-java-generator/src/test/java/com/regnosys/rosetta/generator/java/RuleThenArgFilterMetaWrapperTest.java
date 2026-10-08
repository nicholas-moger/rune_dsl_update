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
 * PR #297 — facet thenArgFilterMetaWrapper: ONE green-safe GENERATOR mechanism, 20 drr POJO Rule
 * byte flips (the #144 meta-leaf residual at the FILTER/collapse thenArg seat).
 *
 * <p><b>The mechanism.</b> A {@code then}-body that is an ELEMENT-PRESERVING op (filter /
 * only-element / first / last / distinct / reverse / flatten) keeps its receiver's element type.
 * When the receiver ({@code prevRef} = the preceding {@code thenArg}) is a META-WRAPPED collection
 * ({@code Mapper*<ReferenceWithMetaX>}/{@code FieldWithMetaX}), the filtered/collapsed {@code thenArg}
 * keeps the wrapper, but {@code getInferredType} is meta-blind (returns the bare {@code X}) AND the
 * #144 ref-scan cannot recover it: the filter's compiled value type is {@code null}
 * ({@code CollectionHandler.handle(RFilterExpr)} returns a null-typed expression) and the
 * receiver/item wrapper is NOT in the compiled refs (only the witness bare types + intermediate
 * predicate derefs are). {@code FunctionExpressionRenderer.renderThenExtractSetImpl} recovers the
 * wrapper from {@code prevRef}'s OWN element type. The downstream nav off the now-wrapper-typed
 * {@code thenArg} derefs via the existing {@code coerceNavigationReceiver} machinery; the colliding
 * deref-param escape falls out of the scope name allocation.
 *
 * <p><b>FILTER-anchored gate (the wide-blast-radius guard).</b> Recover ONLY when the
 * element-preserving op is a FILTER (whose predicate DEREFS the items — confirming {@code prevRef}'s
 * items ARE real meta wrappers golden keeps) OR when the wrapper PROPAGATES from a preceding filter
 * recovery (a collapse/distinct after a filter). A {@code distinct(thenArg0)} over a {@code thenArg0}
 * whose wrapper #144 wrongly KEPT (a {@code mapSingleToList} lambda golden DEREFS at its return,
 * forced by a bare sibling branch of a conditional — the UTI/USI family) is NOT filter-anchored, so
 * it declines: the un-derefed-lambda divergence is a SEPARATE (thenArg0-level) bug this fix must not
 * propagate (a first un-anchored impl regressed those 16 carriers, the regscan caught it).
 *
 * <p><b>Green-safety (regscan: 20 flipped-out / 0 within-waiver regressions / 0 new).</b> A
 * {@code Mapper*<BareX> = <MapperC<RefWithMetaX>>.filterItemNullSafe(...)} is a generics MISMATCH
 * (never compiled), so every flipped carrier is an already-waivered mismatch; a filter/collapse over
 * a NON-meta collection has {@code prevRef}'s element == inferred element (both bare) so the override
 * is a no-op (byte-identical for every green carrier). byte-oracle / stash-baseline measured exactly
 * 20 flips (drr POJO 422 → 402; clean source = 12 stale waivers, {@code comm -23} = exactly the 20).
 * FUNCTION mismatch count UNCHANGED (cdm5 81 / cdm6 236 / drr 207; 2 drr FUNCTION
 * ExtractUpi/Extract_BondConnect moved toward-golden, the shared seat); cdm/iso/fpml POJO
 * byte-IDENTICAL.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr Rule output against the frozen
 * goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED for the flip locks.
 */
class RuleThenArgFilterMetaWrapperTest {

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

    // ==== Flip locks (revert-RED): a filter+collapse thenArg over a meta-wrapped collection keeps
    //      the wrapper element type + derefs at the next nav, byte-matching golden. ====

    /**
     * Flip — the canonical FILTER+collapse carrier. {@code InitialMarginCollateralPortfolioCode}'s
     * body navigates the {@code (positionForEvent ->) tradeForEvent -> collateral -> collateralPortfolio}
     * list ({@code ReferenceWithMetaCollateralPortfolio}, a meta list), then {@code filterItemNullSafe}
     * (thenArg1) + {@code only-element} (thenArg2). Golden keeps the
     * {@code MapperC<ReferenceWithMetaCollateralPortfolio>} wrapper through both thenArgs (the FILTER
     * anchors the recovery, the collapse propagates) and derefs at the final
     * {@code .getPortfolioIdentifier} nav; the fork un-wrapped thenArg1 to the bare
     * {@code MapperC<CollateralPortfolio>} (a generics mismatch).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void initialMarginCollateralPortfolioCode_filterCollapseKeepsWrapper_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/trade/collateral/reports/InitialMarginCollateralPortfolioCodeRule.java");
    }

    /** Flip — the VARIATION_MARGIN sibling of the same filter+collapse shape. */
    @Test
    @EnabledIf("drrCellAvailable")
    void variationMarginCollateralPortfolioCode_filterCollapseKeepsWrapper_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/trade/collateral/reports/VariationMarginCollateralPortfolioCodeRule.java");
    }

    /**
     * Flip — the PROPAGATION carrier. {@code UniqueProductIdentifier} (iosco upi) is a
     * NAV→FILTER→collapse chain: thenArg0 is bare {@code MapperS<Trade>}, thenArg1 navigates to the
     * {@code ReferenceWithMetaProductIdentifier} list (the #144 nav recovery establishes the wrapper),
     * thenArg2 {@code filterItemNullSafe} (FILTER-anchored recovery), thenArg3
     * {@code MapperS.of(thenArg2.get())} (collapse, propagated). Exercises the FILTER-anchored gate at
     * a thenArg whose wrapper originated upstream (not at thenArg0) + the propagation flag.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void uniqueProductIdentifier_navFilterCollapsePropagation_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/upi/reports/UniqueProductIdentifierRule.java");
    }

    // ==== Green-safety / gate lock: the FILTER-anchored gate does NOT over-fire on a
    //      distinct-first chain over a divergent (conditional-lambda) thenArg0. ====

    /**
     * Green-safety / gate lock — the {@code distinct(thenArg0)}-FIRST UTI/USI family stays DECLINED.
     * {@code UniqueTransactionIdentifier} (cftc margin) is {@code mapSingleToList then distinct then …}:
     * golden DEREFS the {@code FieldWithMetaString} at the {@code mapSingleToList} lambda return
     * (forced by a bare sibling branch of an in-lambda conditional), so golden's thenArg0 is the bare
     * {@code MapperC<String>}. (PR #339 prose refresh: facet condBothArmsMultiCardinality's else-deref
     * now produces the bare {@code MapperC<String>} thenArg0 for the cftc carrier too — dl 13 → 7,
     * regscan TOWARD — so this lock's assertions hold on BOTH sides of that facet; the file stays
     * divergent on its enum→singletonList residual, the sized #340 compose.) Because thenArg1 is a
     * {@code distinct} (NOT a filter) and thenArg0 was not filter-recovered by this fix, the
     * FILTER-anchored gate DECLINES — so thenArg1 stays the bare
     * {@code MapperC<String> thenArg1 = distinct(thenArg0)} (NOT the over-fired
     * {@code MapperC<FieldWithMetaString>} a naive un-anchored fix produced, which the regscan flagged
     * as a 2-diff-line regression).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void uniqueTransactionIdentifier_distinctFirstOverDivergentThenArg0_keepsDecline() {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String gen = drrOutput.get(
                "drr/regulation/cftc/rewrite/margin/reports/UniqueTransactionIdentifierRule.java");
        assertNotNull(gen, "UniqueTransactionIdentifierRule not generated");
        assertTrue(gen.contains("final MapperC<String> thenArg1 = distinct(thenArg0)"),
                "a distinct-first thenArg over a thenArg0 that #144 wrongly kept wrapper-typed must "
                + "DECLINE (stay bare MapperC<String>) — the FILTER-anchored gate must not propagate "
                + "the un-derefed-lambda divergence");
        assertTrue(!gen.contains("final MapperC<FieldWithMetaString> thenArg1 = distinct(thenArg0)"),
                "the gate must NOT over-fire to MapperC<FieldWithMetaString> on the distinct-first "
                + "UTI/USI family (the regscan caught this regression in a first un-anchored impl)");
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
                + path + " (PR #297 thenArgFilterMetaWrapper).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
