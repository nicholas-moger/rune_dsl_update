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
 * PR #270 — facet metaWrapLambda265 TRANSITIVE recovery (GENERATOR, the M7b-3 rule-body cluster):
 * the #265 follow-on. 22 drr POJO Rule byte flips.
 *
 * <p><b>The mechanism.</b> The #265 {@code .mapSingleToItem(item -> <innerRule>)} value→meta-wrap
 * (the inner rule's rosetta {@code [metadata scheme]} output is honoured by wrapping the bare
 * {@code evaluate()} value in a {@code FieldWithMetaString} block inside the extract lambda + a
 * dereference at the output) fires only when
 * {@link com.regnosys.rosetta.generator.java.expression.handlers.NavigationHandler#recoverInnerRuleMetaWrapper}
 * recovers the inner rule's meta wrapper. #265 read the inner rule's terminal nav attribute directly;
 * the iosco-cde report rules carry the meta TRANSITIVELY, so {@code recoverInnerRuleMetaWrapper} (via
 * the new {@code recoverMetaFromExpr} worker) now recovers it through two transitive shapes:
 * <ul>
 *   <li><b>shape 1 — bare-rule delegation</b>: a regime rule whose whole body is {@code <baseRule>}
 *       (the iosco-cde {@code v3 -> v2 -> v1} alias chain, e.g. {@code PackageIdentifier}); recurse
 *       into the delegate, which has the SAME rosetta output type;</li>
 *   <li><b>shape 3 — fused cardinality list-op</b>: a terminal then-body {@code … -> identifier last}
 *       where the meta leaf is INSIDE a {@code last}/{@code only-element} (the common
 *       {@code InitialMargin}/{@code VariationMargin}/{@code CollateralPortfolioCode} rules);
 *       #264's whole-body element-preserving skip missed it.</li>
 * </ul>
 * The byte-oracle measured exactly 22 flips: {@code PackageIdentifier} ×8 (shape 1), {@code Initial}/
 * {@code VariationMarginCollateralPortfolioCode} ×10 + {@code CollateralPortfolioCode} ×2 (shape 3),
 * {@code PlatformIdentifier} cftc/jfsa ×2 (the #267-deferred leaf-meta-cast residual, now recovered).
 *
 * <p><b>Green-safe by construction.</b> ~351 GREEN drr Rule goldens carry the EXACT fork bare form
 * {@code mapSingleToItem(item -> MapperS.of(<rule>.evaluate(item.get()))).get()} for a NON-meta inner
 * rule, so the recovery returning null for them is the entire safety argument. The transitive walk is
 * faithful: it recurses a pure delegate (same rosetta output), descends fused list-ops only to the
 * meta terminal, and STOPS at the first terminal-output body that recovers no meta (never falsely
 * recovering an upstream intermediate's meta). The decline locks below are GREEN files whose inner
 * rule is a v3 delegate chain to a NON-meta leaf (currency / enum) or a then-chain to a non-meta DATE
 * — my recovery must keep them bare. (The remaining metaWrapLambda265 carriers — SubsequentPositionUTI
 * / DTCC_ProductID / PTRRID / SecondaryTransactionIdentifier — decline because the shared
 * {@code terminalNavAttr}/{@code metaNavResultType} cannot resolve a parser-erased {@code identifier}
 * leaf on a multi receiver (#238 objFallback territory, function-tail-shared); BasketConstituent /
 * SwapLinkID need a conditional-descent shape — both #271 leads.)
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr POJO output (Rule kind) against the
 * frozen goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleMetaWrapLambdaTransitiveTest {

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
     * {@link D11CorpusRegressionTest#pojo_comparison}'s generator wiring.
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

    // ==== Flip locks (revert-RED): shape 1 — PackageIdentifier (iosco-cde v3 -> v2 -> v1 delegate
    //      chain bottoming at the v1 `… listId -> assignedIdentifier -> identifier` meta leaf). ====

    @Test
    @EnabledIf("drrCellAvailable")
    void packageIdentifier_cftc_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/trade/reports/PackageIdentifierRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void packageIdentifier_esma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/trade/reports/PackageIdentifierRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void packageIdentifier_mas_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/mas/rewrite/trade/reports/PackageIdentifierRule.java");
    }

    // ==== Flip locks (revert-RED): shape 3 — Initial/VariationMarginCollateralPortfolioCode +
    //      CollateralPortfolioCode (the common rules whose terminal then-body fuses the meta leaf
    //      inside a `… -> identifier last`). ====

    @Test
    @EnabledIf("drrCellAvailable")
    void initialMarginCollateralPortfolioCode_cftc_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/trade/reports/InitialMarginCollateralPortfolioCodeRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void initialMarginCollateralPortfolioCode_hkma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/hkma/rewrite/trade/reports/InitialMarginCollateralPortfolioCodeRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void variationMarginCollateralPortfolioCode_csa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/csa/rewrite/trade/reports/VariationMarginCollateralPortfolioCodeRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void variationMarginCollateralPortfolioCode_jfsa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/jfsa/rewrite/trade/reports/VariationMarginCollateralPortfolioCodeRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void collateralPortfolioCode_esma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/trade/reports/CollateralPortfolioCodeRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void collateralPortfolioCode_fca_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/fca/ukemir/refit/trade/reports/CollateralPortfolioCodeRule.java");
    }

    // ==== Flip locks (revert-RED): PlatformIdentifier cftc/jfsa — the #267-deferred leaf-meta-cast
    //      residual, now recovered by the transitive walk. ====

    @Test
    @EnabledIf("drrCellAvailable")
    void platformIdentifier_cftc_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/trade/reports/PlatformIdentifierRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void platformIdentifier_jfsa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/jfsa/rewrite/trade/reports/PlatformIdentifierRule.java");
    }

    // ==== Green-safety decline locks: GREEN drr Rules at the SAME .mapSingleToItem lambda seat whose
    //      inner rule the transitive recovery must NOT recover (return null -> bare form -> green). ====

    /**
     * Green-safety lock (DECLINE — shape-1 recursion to a NON-meta leaf): {@code PackageTransaction
     * PriceCurrencyRule} (asic) references the iosco-cde {@code v3 -> v2 -> v1} delegate chain
     * {@code PackageTransactionPriceCurrency}, which bottoms at a non-meta CURRENCY leaf. The recursion
     * follows the chain but recovers null (no meta terminal), so the wrap declines and the file stays
     * byte-identical to the golden bare {@code mapSingleToItem(item -> MapperS.of(...)).get()}. Proves
     * the recursion does not over-fire on the ~351 green delegate-chain carriers.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void packageTransactionPriceCurrency_v3ChainNonMeta_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/PackageTransactionPriceCurrencyRule.java");
    }

    /**
     * Green-safety lock (DECLINE — shape-1 recursion to a NON-meta ENUM): {@code ActionTypeRule}
     * (asic) references the iosco-cde v3 delegate chain {@code ActionType}, whose output is a plain
     * ENUM. The recursion recovers null, the wrap declines, the file stays green. A second recursion
     * decline distinct from the currency case.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void actionType_v3ChainNonMetaEnum_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/ActionTypeRule.java");
    }

    /**
     * Green-safety lock (DECLINE — shape-3 then-chain to a NON-meta DATE): {@code EffectiveDateRule}
     * (asic) references the common {@code EffectiveDate} rule, a then-chain whose terminal navigation
     * lands on a plain DATE (non-meta). The per-body walk reaches the terminal, recovers null, and
     * STOPS — the wrap declines and the file stays green. Proves the fused/per-body walk does not
     * over-fire on a non-meta terminal.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void effectiveDate_thenChainNonMetaDate_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/EffectiveDateRule.java");
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
                + path + " (PR #270 metaWrapLambda265 transitive recovery).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
