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
 * PR #264 — facet ruleThenValueMetaWrap (GENERATOR, the M7b-3 rule-body cluster): the value→meta-wrap
 * -then-deref block golden emits at the TOP-LEVEL bare-rule-then SET seat ({@code renderBareInvokableThenSet}).
 * 12 drr POJO Rule byte flips.
 *
 * <p><b>The mechanism.</b> A reporting rule body {@code <filter> then <innerRule>} whose inner rule's
 * rosetta OUTPUT is META-typed ({@code [metadata scheme]} etc.) but whose Java {@code evaluate()}
 * returns the BARE value (the fork rule generator drops the meta from the signature), and whose own
 * output is the plain value. Golden honours the inner rule's rosetta meta type by round-tripping the
 * bare value through the {@code FieldWithMetaString} wrapper then dereferencing back:
 * <pre>
 *   final String string = nameOfTheFloatingRateOfLeg1Rule.evaluate(thenArg.get());
 *   final FieldWithMetaString fieldWithMetaString = (string == null ? MapperS.&lt;FieldWithMetaString&gt;ofNull()
 *           : MapperS.of(FieldWithMetaString.builder().setValue(string).build())).get();
 *   if (fieldWithMetaString == null) { output = null; } else { output = fieldWithMetaString.getValue(); }
 * </pre>
 * The fork emitted the bare {@code output = MapperS.of(nameOfTheFloatingRateOfLeg1Rule.evaluate(thenArg.get())).get();}.
 *
 * <p><b>Recovering the lost meta.</b> The inner rule's meta is invisible to the fork's then-inference
 * ({@code gm.workspace().getInferredType(then).type()} is the meta-DROPPED plain String). It is
 * recovered from the inner RULE's own body: flatten the inner rule's left-associative then-chain into
 * its bodies, walk to the LAST navigating body (skipping element-preserving tail ops — first/last/
 * flatten/distinct/filter), and read its terminal nav attribute's meta wrapper via
 * {@link com.regnosys.rosetta.generator.java.expression.handlers.NavigationHandler#tryTerminalMetaMapperType}
 * (the same #237/#249/#263 erased-type recovery). The wrap fires only when the recovered wrapper's
 * VALUE type is exactly the bare then result type.
 *
 * <p><b>Green-safe by construction.</b> The fork's bare {@code MapperS.of(<call>).get()} already
 * COMPILES and behaves identically (the wrap-then-deref is a semantic round-trip), so every carrier is
 * a byte-divergent COMPILES_DIVERGENT mismatch — a green file cannot carry the golden round-trip form.
 * The decline gate (non-meta inner rule → recovery null; or a meta wrapper navigated through → value
 * type ≠ leaf type) keeps green rules byte-identical.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr POJO output (Rule kind) against the
 * frozen goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleThenValueMetaWrapTest {

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
     * {@link D11CorpusRegressionTest#pojo_comparison}'s generator wiring — the rule-body Rule classes
     * this facet touches are emitted by {@link RuleGenerator} (which delegates to
     * {@link FunctionGenerator}, the shared expression compiler this PR fixes).
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

    // ==== Flip locks (revert-RED): NameOfTheFloatingRate (inner NameOfTheFloatingRateOfLegN, meta indexName). ====

    @Test
    @EnabledIf("drrCellAvailable")
    void nameOfTheFloatingRateLeg1_hkma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/hkma/rewrite/trade/reports/NameOfTheFloatingRateLeg1Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void nameOfTheFloatingRateLeg2_hkma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/hkma/rewrite/trade/reports/NameOfTheFloatingRateLeg2Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void nameOfTheFloatingRateLeg1_jfsa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/jfsa/rewrite/trade/reports/NameOfTheFloatingRateLeg1Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void nameOfTheFloatingRateLeg2_jfsa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/jfsa/rewrite/trade/reports/NameOfTheFloatingRateLeg2Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void nameOfTheFloatingRateLeg1_mas_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/mas/rewrite/trade/reports/NameOfTheFloatingRateLeg1Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void nameOfTheFloatingRateLeg2_mas_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/mas/rewrite/trade/reports/NameOfTheFloatingRateLeg2Rule.java");
    }

    // ==== Flip locks (revert-RED): FloatingRateIdentifier (inner FloatingRateIdentifierOfLegN, meta floatingRateIndex). ====

    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRateIdentifierLeg1_hkma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/hkma/rewrite/trade/reports/FloatingRateIdentifierLeg1Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRateIdentifierLeg2_hkma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/hkma/rewrite/trade/reports/FloatingRateIdentifierLeg2Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRateIdentifierLeg1_jfsa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/jfsa/rewrite/trade/reports/FloatingRateIdentifierLeg1Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRateIdentifierLeg2_jfsa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/jfsa/rewrite/trade/reports/FloatingRateIdentifierLeg2Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRateIdentifierLeg1_mas_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/mas/rewrite/trade/reports/FloatingRateIdentifierLeg1Rule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRateIdentifierLeg2_mas_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/mas/rewrite/trade/reports/FloatingRateIdentifierLeg2Rule.java");
    }

    // ==== Green-safety locks: rules the change must NOT perturb. ====

    /**
     * Green-safety lock (DECLINE at this seat): {@code CentralCounterpartyRule} (asic) is a GREEN drr
     * Rule whose body is a single-{@code thenArg} bare-rule-then ({@code output =
     * MapperS.of(<rule>.evaluate(thenArg.get())).get();}) routed through the SAME
     * {@code renderBareInvokableThenSet} seat this PR touches, but whose inner rule output is NON-meta —
     * so {@code tryTerminalMetaMapperType} recovers null and the value→meta-wrap declines. Proves the
     * wrap fires only on a meta-typed inner rule.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void centralCounterparty_nonMetaInnerRule_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/CentralCounterpartyRule.java");
    }

    /**
     * Green-safety lock (CHAINED then — gated out by {@code isBareRuleThen}'s single-then gate):
     * {@code CallCurrencyRule} (asic) is a GREEN drr Rule whose body is a CHAINED then
     * ({@code filter … then filter … then <rule>}, rendered with {@code thenArg0}/{@code thenArg1}),
     * so {@code isBareRuleThen} returns false and {@code renderRuleThenValueMetaWrapOrNull} is never
     * reached (gated out before recovery). Proves the chained-then path ({@code renderThenExtractSet})
     * is untouched — distinct from the {@code CentralCounterparty} single-bare-rule-then meta-recovery
     * decline lock above.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void callCurrency_chainedThen_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/CallCurrencyRule.java");
    }

    /**
     * Green-safety lock (different seat — renderThenExtractSet): {@code CallAmountRule} (asic) is a GREEN
     * drr Rule whose output is a non-meta then-CHAIN routed through {@code renderThenExtractSet} (NOT
     * the {@code renderBareInvokableThenSet} seat this PR touches). Proves the change is scoped to the
     * single-bare-rule-then seat.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void callAmount_renderThenExtractSet_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/CallAmountRule.java");
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
                + path + " (PR #264 ruleThenValueMetaWrap).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
