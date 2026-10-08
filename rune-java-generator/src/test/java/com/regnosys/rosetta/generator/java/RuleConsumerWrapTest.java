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
 * PR #266 — facet ruleConsumerWrap (GENERATOR, the M7b-3 rule-body cluster): the
 * {@code only-element}/{@code distinct} COLLAPSE consumer-wrap. 14 drr POJO Rule byte flips.
 *
 * <p><b>The law.</b> A then-body that collapses a multi receiver to a single BARE value via
 * {@code only-element} (or {@code distinct only-element}) is consumed where a {@code MapperS} is
 * expected. Golden re-presents the collapsed value as a {@code MapperS} for the uniform consumer —
 * {@code MapperS.of(<collapse>)} — but the fork emits the self-unwrapping bare collapse. The law
 * fires at TWO render seats:
 * <ul>
 *   <li><b>Seat A — output-SET</b> ({@code FunctionExpressionRenderer.renderThenExtractSet}, the
 *       {@code outBodyUnwrapped} arm): a {@code then distinct only-element} whole-output body
 *       ({@code only-element(distinct(item))}) renders {@code output = MapperS.of(distinct(thenArgN)
 *       .get()).get();} not the bare {@code output = distinct(thenArgN).get();}. Gated by
 *       {@code isDistinctCollapseThenBody}; reuses the PR #172 arm-S3
 *       ({@code isBareItemOnlyElement}) wrap. <b>COMPILES_DIVERGENT</b> (the bare collapse already
 *       compiles + behaves identically — a behaviour-neutral round-trip).</li>
 *   <li><b>Seat B — MapperS-expecting lambda body</b> ({@code CollectionHandler.compileLambda}
 *       MAPPER_EXPECTING): an {@code extract <nav> only-element} map body
 *       ({@code mapSingleToItem(item -> <nav>.get())}) wraps the collapse
 *       {@code mapSingleToItem(item -> MapperS.of(<nav>.get()))}. Gated on {@code RListOpExpr}
 *       ONLY_ELEMENT. <b>NON_COMPILING</b> (a bare value does not satisfy the
 *       {@code Function<MapperS<T>, MapperS<F>>} map signature), so these also raise functional
 *       parity.</li>
 * </ul>
 *
 * <p><b>Green-safe by construction.</b> Seat A: the fork's bare {@code distinct(thenArgN).get()}
 * never byte-matched golden's wrapped form, so every carrier was a waivered mismatch. Seat B: a
 * bare {@code only-element} collapse at a {@code mapSingleToItem} body does not compile, so golden
 * ALWAYS wraps it and NO green file carries the bare form. Neither gate fires on a plain nav body
 * (the {@code only-element}/{@code distinct} AST shape is the discriminator).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr POJO output (Rule kind) against
 * the frozen goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleConsumerWrapTest {

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

    // ==== Seat A flip locks (revert-RED): distinct-only-element output wrap (COMPILES_DIVERGENT). ====

    @Test
    @EnabledIf("drrCellAvailable")
    void natureOfCounterparty1_esma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/trade/reports/NatureOfCounterparty1Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void natureOfCounterparty2_esma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/trade/reports/NatureOfCounterparty2Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void natureOfCounterparty1_fca_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/fca/ukemir/refit/trade/reports/NatureOfCounterparty1Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void natureOfTheCounterparty2_fca_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/fca/ukemir/refit/trade/reports/NatureOfTheCounterparty2Rule.java");
    }

    // ==== Seat B flip locks (revert-RED): only-element MAPPER_EXPECTING lambda-body wrap (NON_COMPILING). ====

    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRateIdentifierLeg1_common_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/common/trade/underlier/reports/FloatingRateIdentifierLeg1Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRateIdentifierLeg2_common_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/common/trade/underlier/reports/FloatingRateIdentifierLeg2Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void identifierOfFloatingRateOfLeg1_esma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/trade/reports/IdentifierOfFloatingRateOfLeg1Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void identifierOfFloatingRateOfLeg2_esma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/trade/reports/IdentifierOfFloatingRateOfLeg2Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void identifierOfFloatingRateOfLeg1_fca_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/fca/ukemir/refit/trade/reports/IdentifierOfFloatingRateOfLeg1Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void identifierOfFloatingRateOfLeg2_fca_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/fca/ukemir/refit/trade/reports/IdentifierOfFloatingRateOfLeg2Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void uti_esma_margin_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/margin/reports/UTIRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void uti_fca_margin_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/fca/ukemir/refit/margin/reports/UTIRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void uti_jfsa_margin_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/jfsa/rewrite/margin/reports/UTIRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void eventIdentifier_iosco_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/standards/iosco/cde/version3/event/reports/EventIdentifierRule.java");
    }

    // ==== Green-safety DECLINE locks: rules at the SAME seats the change must NOT perturb. ====

    /**
     * Green-safety lock (seat B DECLINE — plain nav map body, no only-element):
     * {@code CollateralisationCategoryRule} (asic margin) is a GREEN drr Rule whose body is
     * {@code .mapSingleToItem(item -> item.<…>map(…).checkedMap("to-enum", …))} — a plain
     * navigation + conversion map body at the SAME MAPPER_EXPECTING seat this PR wraps, but its
     * top-level AST node is NOT an {@code only-element} list-op, so the seat-B arm declines and the
     * body stays unwrapped (byte-identical). Proves the only-element AST gate, not the seat.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void collateralisationCategory_plainNavMapBody_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/margin/reports/CollateralisationCategoryRule.java");
    }

    /**
     * Green-safety lock (seat A + seat B DECLINE — plain multi-step nav, no collapse):
     * {@code BasketStructurerRule} (asic) is a GREEN drr Rule whose whole body is a single
     * {@code .mapSingleToItem(item -> item.<…>map(…).<…>map(…).<PartyIdentifier>map(…))} plain nav
     * extract — neither an {@code only-element}/{@code distinct} output collapse (seat A declines)
     * nor an {@code only-element} map body (seat B declines). A multi-step nav map body stays
     * unwrapped, byte-identical. The reportable-event sibling of the deferred non-literal-default
     * {@code CustomBasketCodeIdentifier} carrier.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void basketStructurer_plainNavExtract_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/BasketStructurerRule.java");
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
                + path + " (PR #266 ruleConsumerWrap).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
