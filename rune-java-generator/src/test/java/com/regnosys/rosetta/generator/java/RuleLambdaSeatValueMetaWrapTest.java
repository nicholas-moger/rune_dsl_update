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
 * PR #265 — facet ruleThenValueMetaWrap LAMBDA seat (GENERATOR, the M7b-3 rule-body cluster): the
 * inner-rule value→meta-wrap at the {@code .mapSingleToItem(item -> <rule>)} extract-lambda seat — the
 * DEEPER half of the #264 top-level bare-rule-then family. 15 drr POJO Rule byte flips.
 *
 * <p><b>The mechanism.</b> A reporting rule body {@code <filter> then extract <innerRule>} whose inner
 * rule's rosetta OUTPUT is META-typed ({@code [metadata scheme]} etc.) but whose Java {@code evaluate()}
 * returns the BARE value (the fork rule generator drops the meta from the signature), and whose own
 * output is the plain value. Golden honours the inner rule's rosetta meta type by wrapping the bare
 * value INSIDE the extract lambda and dereferencing at the output:
 * <pre>
 *   final FieldWithMetaString fieldWithMetaString = thenArg
 *       .mapSingleToItem(item -> {
 *           final String string = uniqueProductIdentifierRule.evaluate(item.get());
 *           return string == null ? MapperS.&lt;FieldWithMetaString&gt;ofNull()
 *                : MapperS.of(FieldWithMetaString.builder().setValue(string).build());
 *       }).get();
 *   if (fieldWithMetaString == null) { output = null; } else { output = fieldWithMetaString.getValue(); }
 * </pre>
 * The fork emitted the bare {@code output = thenArg.mapSingleToItem(item ->
 * MapperS.of(uniqueProductIdentifierRule.evaluate(item.get()))).get();} (a {@code MapperS<String>} the
 * output deref cannot fire on).
 *
 * <p><b>Two coordinated changes.</b> {@code ReferenceHandler.renderImplicitRuleInvocation} hoists the
 * bare value into a lambda-channel local (drained into the brace-block body by
 * {@code CollectionHandler.compileLambda}) and returns the {@code MapperS<FieldWithMetaString>} wrap;
 * {@code FunctionExpressionRenderer.renderThenExtractSet} emits the output deref with the meta wrapper.
 * Both seats are coordinated on the SAME {@code NavigationHandler.recoverInnerRuleMetaWrapper} (shared
 * with the #264 top-level seat) + the terminal/value-type gate, so the deref fires iff the lambda body
 * wrapped. The inner rule's meta is invisible to the fork's then-inference, so it is recovered from the
 * inner rule's OWN body (flatten its then-chain, walk to the last navigating body, read the terminal
 * nav attribute's meta wrapper).
 *
 * <p><b>Green-safe by construction.</b> The fork's bare {@code MapperS.of(<call>)} already COMPILES and
 * behaves identically (the wrap is a semantic round-trip), so every carrier is a byte-divergent
 * COMPILES_DIVERGENT mismatch — a green file cannot carry the golden wrap form. The decline gates
 * (inner is a FUNCTION not a RULE → never reaches the wrap; a non-meta inner RULE → recovery null; a
 * rule navigated through / not the terminal lambda body → not fired) keep green rules byte-identical.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr POJO output (Rule kind) against the
 * frozen goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleLambdaSeatValueMetaWrapTest {

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

    // ==== Flip locks (revert-RED): UniqueProductIdentifier (inner iosco UPI rule, meta identifier). ====

    @Test
    @EnabledIf("drrCellAvailable")
    void uniqueProductIdentifier_asic_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/UniqueProductIdentifierRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void uniqueProductIdentifier_cftc_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/trade/reports/UniqueProductIdentifierRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void uniqueProductIdentifier_csa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/csa/rewrite/trade/reports/UniqueProductIdentifierRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void uniqueProductIdentifier_hkma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/hkma/rewrite/trade/reports/UniqueProductIdentifierRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void uniqueProductIdentifier_mas_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/mas/rewrite/trade/reports/UniqueProductIdentifierRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void upi_jfsa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/jfsa/rewrite/trade/reports/UPIRule.java");
    }

    // ==== Flip locks (revert-RED): EventIdentifier (inner iosco EventIdentifier rule, meta identifier). ====

    @Test
    @EnabledIf("drrCellAvailable")
    void eventIdentifier_asic_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/EventIdentifierRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void eventIdentifier_cftc_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/trade/reports/EventIdentifierRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void eventIdentifier_csa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/csa/rewrite/trade/reports/EventIdentifierRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void eventIdentifier_hkma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/hkma/rewrite/trade/reports/EventIdentifierRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void eventIdentifier_jfsa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/jfsa/rewrite/trade/reports/EventIdentifierRule.java");
    }

    // ==== Flip locks (revert-RED): DTCC_* (cftc dtcc — inner meta-rule references). ====

    @Test
    @EnabledIf("drrCellAvailable")
    void dtccLeg1CommodityInstrumentId_cftc_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/dtcc/reports/DTCC_Leg1CommodityInstrumentIDRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void dtccLeg2CommodityInstrumentId_cftc_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/dtcc/reports/DTCC_Leg2CommodityInstrumentIDRule.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void dtccSecondaryAssetClass_cftc_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/dtcc/reports/DTCC_SecondaryAssetClassRule.java");
    }

    // ==== Flip lock (revert-RED): Counterparty2NameDTCC (mas — inner meta-rule reference). ====

    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty2NameDtcc_mas_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/mas/rewrite/trade/reports/Counterparty2NameDTCCRule.java");
    }

    // ==== Green-safety locks: rules at the SAME .mapSingleToItem seat the change must NOT perturb. ====

    /**
     * Green-safety lock (DECLINE — inner is a FUNCTION, not a RULE): {@code PriorUTIRule} (cftc) is a
     * GREEN drr Rule whose body is {@code filter … then extract GetPriorTransactionIdentifier(item,
     * DoddFrankAct)} — the {@code .mapSingleToItem(item -> MapperS.of(getPriorTransactionIdentifier
     * .evaluate(item.get(), RegimeNameEnum.DODD_FRANK_ACT)))} seat this PR touches, but the inner
     * invokable is a FUNCTION (an {@link com.regnosys.rosetta.ast.functions.RFunction}, with explicit
     * args) so {@code renderImplicitRuleInvocation} is never reached and the wrap declines. Proves the
     * wrap fires only on a bare-args RULE reference.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void priorUti_functionInner_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/trade/reports/PriorUTIRule.java");
    }

    /**
     * Green-safety lock (DECLINE — RULE inner but NON-meta output): {@code FinalContractualSettlementDate
     * Rule} (cftc) is a GREEN drr Rule whose body is {@code filter … then extract cdeV3.execution.
     * FinalContractualSettlementDate} — a bare-args RULE reference at the SAME extract-lambda seat, but
     * the inner rule's output is a plain DATE (non-meta), so {@code recoverInnerRuleMetaWrapper}
     * recovers null and the wrap declines. Proves the wrap fires only on a meta-typed inner rule —
     * distinct from the PriorUTI function-inner decline.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void finalContractualSettlementDate_nonMetaInnerRule_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/trade/reports/FinalContractualSettlementDateRule.java");
    }

    /**
     * Green-safety lock (DECLINE — INTERIOR extract + non-meta inner RULE): {@code EventTypeRule}
     * (cftc) is a GREEN drr Rule whose body carries a bare meta-rule reference at an INTERIOR
     * {@code .mapSingleToItem(item -> MapperS.of(eventTypeRule.evaluate(item.get())))} (an else-arm
     * inside a larger chain, NOT the output body). {@code renderImplicitRuleInvocation}'s wrap would
     * fire at ANY extract-lambda terminal (interior OR output), so this proves the meta-recovery
     * decline — not the output position — keeps it bare: the inner {@code EventType} rule's output is
     * an ENUM ({@code EventTypeEnum}, non-meta), so {@code recoverInnerRuleMetaWrapper} returns null.
     * Distinct from the two output-seat declines above (function-inner / non-meta-DATE-inner).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void eventType_interiorExtractNonMetaInner_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/trade/reports/EventTypeRule.java");
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
                + path + " (PR #265 ruleThenValueMetaWrap LAMBDA seat).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
