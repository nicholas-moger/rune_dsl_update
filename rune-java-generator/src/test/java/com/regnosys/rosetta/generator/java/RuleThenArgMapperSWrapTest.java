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
 * PR #260 — facet {@code thenArgMapperSWrap} (rule-body emission, M7b-3; the modest clean bundle,
 * facet B).
 *
 * <p>An intermediate then-body that COLLAPSES the preceding {@code thenArg} to a SINGLE item (a
 * cardinality op {@code only-element}/{@code first}/{@code last}, alone or in {@code distinct(...)},
 * compiles to {@code <prev>.get()}, a BARE item) is declared {@code final MapperS<X> thenArgN} in
 * {@code FunctionExpressionRenderer.renderThenExtractSet}'s k-loop. A bare {@code X} does NOT compile
 * against the {@code MapperS<X>} declaration; the golden (upstream rune-dsl 9.83.0) wraps it
 * {@code MapperS.of(<prev>.get())} — the #256 {@code bareValueMapperSWrap} mechanism at the
 * thenArg-decl seat.
 *
 * <p>The collapsed value's compiled expression type is erased to {@code null} (the parser snapshot is
 * cardinality-blind for the re-rooted collapse), so the gate keys on the rendered output SHAPE — a
 * SINGLE declaration whose value is a bare {@code .get()} unwrap and is not already
 * {@code MapperS.of(...)}-wrapped. This is the same emitted-output-shape gate the #257 cascade
 * fallback uses ({@code containsUnhoistedThen}). A normal nav/extract then-body renders a Mapper
 * chain that does NOT end in {@code .get()} (the decl keeps the Mapper), and a function-call base
 * renders {@code <fn>.evaluate(item.get())} ending in {@code ))}, so both decline.
 *
 * <p><b>Green-safe by construction</b> (corpus-verified, frozen 9.83.0 baseline): a bare-item
 * assignment to a {@code MapperS<X>} local never compiled, so every carrier is an already-waivered
 * mismatch — a green file with a collapse already carries the {@code MapperS.of(...)} wrap (which the
 * {@code !startsWith("MapperS.of(")} guard declines), and a green then-chain with no collapse has no
 * trailing {@code .get()}. See the green-safety locks below.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr POJO output (Rule kind) against
 * the frozen goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleThenArgMapperSWrapTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR = DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

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

    // ---- Flip locks (revert-RED): a SINGLE thenArg decl with a bare `.get()` collapse value.

    /**
     * {@code thenArg3 = thenArg2.get()} (a {@code MapperC} filter collapsed to a single
     * {@code MapperS<ReportingRegime>}) wraps to {@code MapperS.of(thenArg2.get())}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void postPricedSwapIndicatorRuleCftc_collapseWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/trade/reports/PostPricedSwapIndicatorRule.java");
    }

    /** Same collapse-wrap shape: esma. */
    @Test
    @EnabledIf("drrCellAvailable")
    void directlyLinkedRuleEsma_collapseWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/esma/emir/refit/trade/reports/DirectlyLinkedToCommercialActivityOrTreasuryFinancingRule.java");
    }

    /** Same collapse-wrap shape, cross-region: fca. */
    @Test
    @EnabledIf("drrCellAvailable")
    void directlyLinkedRuleFca_collapseWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/fca/ukemir/refit/trade/reports/DirectlyLinkedToCommercialActivityOrTreasuryFinancingRule.java");
    }

    // ---- Green-safety lock: a form the fix must NOT perturb.

    /**
     * Green-safety lock (no collapse): {@code UniqueTransactionIdentifierProprietaryRule} (asic) is a
     * GREEN drr Rule with a then-chain whose intermediate thenArgs are normal Mapper chains (no
     * trailing {@code .get()} collapse), so the {@code value.endsWith(".get()")} gate declines —
     * byte-identical. (The no-double-wrap guard {@code !value.startsWith("MapperS.of(")} is exercised
     * by the flip carriers above, which emit exactly one {@code MapperS.of(...)} wrap matching golden;
     * no pre-existing green file carries the k-loop collapse-wrap, since it is this facet's new
     * capability — the golden carriers that DO carry it are currently waivered or missing-output.)
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void uniqueTransactionIdentifierProprietaryRuleAsic_noCollapseThenChain_staysByteIdentical()
            throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/asic/rewrite/margin/reports/UniqueTransactionIdentifierProprietaryRule.java");
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
                + path + " (facet thenArgMapperSWrap, PR #260).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
