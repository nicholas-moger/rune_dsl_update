package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #328 anchors — the meta-axis compose: facets {@code asKeyReference} (F1) +
 * {@code valueMetaWrapCompletion} (F2) + {@code sumTypedFunctionPath} (F3) +
 * {@code metaIteArmWrapperTyping} (F4).
 *
 * <ul>
 *   <li><b>F1 (as-key):</b> an {@code as-key} SET on a {@code [metadata reference]}
 *       path leaf copies the SOURCE's meta keys — the
 *       {@code Optional.ofNullable(<v>).map(r -> r.getMeta()).map(m -> m.get…Key())}
 *       wrapper builder, never {@code setValue} (upstream
 *       {@code FunctionGenerator.assignValue}'s {@code assignAsKey} arm;
 *       {@code FunctionExpressionRenderer.renderAsKeySetOrNull}). Value shapes:
 *       simple identifier inline; complex value → {@code final} local named
 *       {@code <pathHead><SegNames…>}; no-real-else conditional → the non-final
 *       null-init initializer form.</li>
 *   <li><b>F2 (convertNullSafe completion):</b> F2a a COMPLEX value SET on a meta
 *       leaf hoists {@code final <Bare> <name> = <value>;} + the #187 ternary over
 *       the local (gate = {@code NavigationHandler.terminalProvablyBare}); F2b a
 *       MULTI bare-item value ADDed to a META leaf wraps element-wise via
 *       {@code stream().map(…).collect(Collectors.toList())}; F2c a whole-output
 *       meta wrap hoists the ctor value + distributes the null-check if/else.</li>
 *   <li><b>F3 (typed sum, function path):</b> the #299 typed-sum arm un-gated for
 *       the ALIAS-BODY seat + the {@code FunctionAliasHelper} SUM signature arm
 *       ({@code MapperS<BigDecimal>}, not the output-type fallback leak).</li>
 *   <li><b>F4 (ite wrapper typing):</b> an ite-hoist local whose arms are
 *       META-output fn calls types as the WRAPPER when the consuming ctor-pair
 *       attribute is itself META + single; the absent else takes the #193
 *       meta-EMPTY builder (blank-final form) and the setter keys PLAIN in
 *       lock-step.</li>
 * </ul>
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of generated output against the frozen
 * 9.83.0 goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 *
 * <p><b>Deliberate coverage gap (the #325 S1 convention):</b> the cdm5 twins of the
 * cdm6-locked carriers (Create_* WorkflowSteps, Create_BillingRecord, ResolveTransfer,
 * Apply*Formula, Create_AssetReset) have no second lock — byte-identical rosetta,
 * verified by the byte-oracle + probe dumps; the cdm5 cell loads here ONLY for the
 * two shapes it alone carries (the EquityCashSettlementAmount complex-value as-key
 * hoist + the ConvertToAdjustableOrRelativeDate green pin).
 */
class FunctionMetaAxisComposeTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // F1 flip carrier (cdm6): the SIMPLE as-key shape — inline Optional.ofNullable
    // meta-key builder on the bare input param.
    private static final String CREATE_PROPOSED_WORKFLOW_STEP =
            "cdm/event/workflow/functions/Create_ProposedWorkflowStep.java";
    // F1 flip carrier (cdm6): the CONDITIONAL as-key shape — the non-final null-init
    // `WorkflowStep workflowStepPreviousWorkflowStep = null;` initializer-form local.
    private static final String CREATE_WORKFLOW_STEP =
            "cdm/event/workflow/functions/Create_WorkflowStep.java";
    // F1 flip carrier (cdm5-ONLY): the COMPLEX-value as-key shape — the final hoist
    // `final PerformancePayout equityCashSettlementAmountSettlementOriginPerformancePayout
    // = equityPerformancePayout(tradeState, date).get();` through a getOrCreate
    // intermediate (the co-occupied carrier F1 healed).
    private static final String EQUITY_CASH_SETTLEMENT_AMOUNT_CDM5 =
            "cdm/event/common/functions/EquityCashSettlementAmount.java";
    // F2a flip carrier (cdm6): the scope-escaped hoist local (`_tradeState` — the
    // `tradeState` ALIAS holds the plain name).
    private static final String CREATE_BILLING_RECORD =
            "cdm/event/common/functions/Create_BillingRecord.java";
    // F2a flip carrier (cdm6): the plain hoist local (`string`).
    private static final String GET_GROSS_INITIAL_MARGIN =
            "cdm/margin/schedule/functions/GetGrossInitialMarginFromStandardizedSchedule.java";
    // F2b flip carrier (cdm6): the element-wise stream wrap at the segment ADD.
    private static final String CREATE_ASSET_RESET =
            "cdm/observable/event/functions/Create_AssetReset.java";
    // F2c flip carrier (cdm6): the whole-output wrap with the ctor-value hoist +
    // distributed if/else.
    private static final String MAP_PERSON_IDENTIFIER =
            "cdm/ingest/fpml/confirmation/party/functions/MapPersonIdentifier.java";
    // F3 flip carrier (cdm6): alias-terminal typed sum + the SUM signature arm.
    private static final String APPLY_COMPOUNDING_FORMULA =
            "cdm/observable/asset/calculatedrate/functions/ApplyCompoundingFormula.java";
    // F4 flip carrier (cdm6): wrapper-typed local + meta-EMPTY else + plain setter.
    private static final String MAP_NUMBER_OF_OPTIONS =
            "cdm/ingest/fpml/confirmation/pricequantity/functions/MapNumberOfOptionsAndOptionEntitlementToQuantity.java";
    // F4 flip carrier (cdm6): the ReferenceWithMeta wrapper local through an
    // if/else-if ladder.
    private static final String MAP_RESOLVABLE_PRICE_QUANTITY =
            "cdm/ingest/fpml/confirmation/product/returnswap/functions/MapResolvablePriceQuantity.java";
    // Green-safety pin (cdm5): GREEN ConvertToAdjustableOrRelativeDate's complex value
    // ALREADY produces the wrapper (`<FieldWithMetaDate>map(…)` terminal — golden
    // splices it BARE, wrapper→wrapper identity) — the F2a terminalProvablyBare gate
    // must DECLINE it (the first-cut regression carrier).
    private static final String CONVERT_TO_ADJUSTABLE_GREEN =
            "cdm/base/datetime/functions/ConvertToAdjustableOrRelativeDate.java";
    // F4 consumer-gate decline pin (cdm6, still waivered): MapUnitTypeWithScheme's
    // consumers are BARE (non-meta) attributes — the wrapper lift must keep the bare
    // null-init locals (golden derefs the meta arms INSIDE the branch — the deferred
    // in-branch deref form; the unconditional first cut moved this file AWAY, the
    // regscan catch).
    private static final String MAP_UNIT_TYPE_WITH_SCHEME =
            "cdm/ingest/fpml/confirmation/common/functions/MapUnitTypeWithScheme.java";

    private static Map<String, String> cdm5Output;
    private static Map<String, String> cdm6Output;

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cdm5CellAvailable()) {
            cdm5Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
        }
        if (cdm6CellAvailable()) {
            cdm6Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var enumGen = new EnumGenerator(gm);
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
                assertNoGenerationErrors(enumGen.generateClasses(model, version, output));
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

    // ==== Flip locks (revert-RED): the carriers now byte-match golden. ====

    @Test
    @EnabledIf("cdm6CellAvailable")
    void createProposedWorkflowStep_simpleAsKey_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, CREATE_PROPOSED_WORKFLOW_STEP);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void createWorkflowStep_conditionalAsKeyInitializerForm_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, CREATE_WORKFLOW_STEP);
    }

    @Test
    @EnabledIf("cdm5CellAvailable")
    void equityCashSettlementAmount_complexValueAsKeyHoist_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm5Output, CDM5_GOLDEN_DIR, EQUITY_CASH_SETTLEMENT_AMOUNT_CDM5);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void createBillingRecord_metaWrapHoistEscapedLocal_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, CREATE_BILLING_RECORD);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void getGrossInitialMargin_metaWrapHoistPlainLocal_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, GET_GROSS_INITIAL_MARGIN);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void createAssetReset_addMetaStreamWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, CREATE_ASSET_RESET);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapPersonIdentifier_wholeOutputMetaWrapCtorHoist_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, MAP_PERSON_IDENTIFIER);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void applyCompoundingFormula_aliasTypedSum_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, APPLY_COMPOUNDING_FORMULA);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapNumberOfOptions_wrapperTypedIteLocal_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, MAP_NUMBER_OF_OPTIONS);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapResolvablePriceQuantity_referenceWrapperIteLadder_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, MAP_RESOLVABLE_PRICE_QUANTITY);
    }

    // ==== Positive-content locks (revert-RED). ====

    @Test
    @EnabledIf("cdm6CellAvailable")
    void createProposedWorkflowStep_emitsMetaKeyCopyNeverSetValue() {
        String gen = gen(cdm6Output, CREATE_PROPOSED_WORKFLOW_STEP);
        assertTrue(gen.contains(".setGlobalReference(Optional.ofNullable(previousWorkflowStep)"),
                "the as-key value must copy the source's global key through Optional.ofNullable");
        assertTrue(gen.contains(".map(m -> m.getExternalKey())"),
                "the as-key value must copy the source's external key");
        assertFalse(gen.contains(
                "ReferenceWithMetaWorkflowStep.builder().setValue(previousWorkflowStep)"),
                "an as-key SET must never setValue (the pre-#328 #187 wrap form)");
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void applyCompoundingFormula_sumSignatureAndTypedMethod() {
        String gen = gen(cdm6Output, APPLY_COMPOUNDING_FORMULA);
        assertTrue(gen.contains(
                "protected abstract MapperS<BigDecimal> totalWeight(List<BigDecimal> observations,"),
                "the sum alias signature must type from the sum result, not the output fallback");
        assertTrue(gen.contains(".sumBigDecimal();"),
                "the alias-terminal sum must render the typed variant");
        assertFalse(gen.contains(".sum();"),
                "the non-existent generic .sum() must not render");
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapNumberOfOptions_blankFinalWrapperLocalWithMetaEmptyElseAndPlainSetter() {
        String gen = gen(cdm6Output, MAP_NUMBER_OF_OPTIONS);
        assertTrue(gen.contains("final FieldWithMetaString ifThenElseResult0;"),
                "the meta-consumer ite local must type as the wrapper (blank-final form)");
        assertTrue(gen.contains("ifThenElseResult0 = FieldWithMetaString.builder().build();"),
                "the absent else must take the #193 meta-EMPTY builder");
        assertTrue(gen.contains(".setCurrency(ifThenElseResult0)"),
                "the consuming setter must key PLAIN for the wrapper-typed local");
        assertFalse(gen.contains(".setCurrencyValue(ifThenElseResult0)"),
                "the Value-form setter would disagree with the wrapper local (pre-#328)");
    }

    // ==== Green-safety / decline locks. ====

    @Test
    @EnabledIf("cdm5CellAvailable")
    void convertToAdjustable_greenWrapperTerminalValue_staysByteIdentical() throws IOException {
        assertByteMatchesGolden(cdm5Output, CDM5_GOLDEN_DIR, CONVERT_TO_ADJUSTABLE_GREEN);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapUnitTypeWithScheme_bareConsumerKeepsBareIteLocals() {
        String gen = gen(cdm6Output, MAP_UNIT_TYPE_WITH_SCHEME);
        assertTrue(gen.contains("CapacityUnitEnum ifThenElseResult0 = null;"),
                "a BARE (non-meta) consumer must keep the bare null-init ite local");
        assertFalse(gen.contains("final FieldWithMetaCapacityUnitEnum ifThenElseResult0;"),
                "the wrapper lift must not fire for a bare consumer (the regscan-caught"
                        + " AWAY-mover of the unconditional first cut)");
    }

    private static String gen(Map<String, String> output, String path) {
        assertNotNull(output, "generation did not run for the cell of " + path);
        String gen = output.get(path);
        assertNotNull(gen, "missing generated file: " + path);
        return gen;
    }

    private void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String path) throws IOException {
        String gen = gen(output, path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "missing golden: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(gen),
                path + " must byte-match the frozen 9.83.0 golden");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
