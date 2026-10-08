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
 * PR #327 anchors — facets {@code condListCoerce} (A) + {@code setterNameValueMeta} (B)
 * + {@code hoistReorder} (C): the conditional single→multi per-branch coercion family
 * and the two ctor laws it surfaced.
 *
 * <ul>
 *   <li><b>A2 (ADD distribution):</b> a whole-output ADD of a no-real-else conditional
 *       over a MULTI output distributes the {@code addAll} into the branches — a SINGLE
 *       arm hoists a type-named local + the #199/#201 null-guarded
 *       singletonList/emptyList inner if/else, a MULTI arm adds directly, the terminal
 *       else adds emptyList; arm-interior hoists relocate INSIDE the branch and else-if
 *       chains recurse ({@code FunctionExpressionRenderer.renderAddConditionalDistributionOrNull}).</li>
 *   <li><b>A1 (expression seats):</b> a ctor pair / segment-ADD leaf consumes the
 *       LIST-typed blank-final hoist ({@code final List<X> ifThenElseResultN;} with
 *       per-arm coercion) via the {@code CondListCoerce} handshake
 *       ({@code ControlFlowHandler.hoistAsListLocalOrNull}); a META attribute with a
 *       meta-free arm composes {@code Wrapper.builder().setValue(local).build()} inside
 *       the singletonList (the #190 law). The coupled ITEM-seat completion: an
 *       {@code else empty} selects the initializer form, arm-VALUE-interior hoists
 *       relocate inside the branch, and the outer local registers AFTER its arms
 *       (inner-first numbering).</li>
 *   <li><b>A3:</b> a MapperC-typed alias return-ladder single rung wraps
 *       {@code MapperC.of(Collections.singletonList(<chain>.get()))}.</li>
 *   <li><b>B:</b> the meta setter + list witness key on the VALUE's proven meta
 *       (upstream {@code requiresValueAssignment}) — B1 META-alias call, B2 the
 *       compiled wrapper item type (render-truth), B3 a list literal of meta-output fn
 *       calls + the {@code MapperC.<FieldWithMetaX>of} witness lift.</li>
 *   <li><b>C:</b> ctor coercion-phase hoists insert at the pair's CONSUMPTION position
 *       (before any LATER pair's ifThenElseResult block).</li>
 * </ul>
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of generated output against the frozen
 * 9.83.0 goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 *
 * <p><b>Deliberate coverage gap (the #325 S1 convention):</b> the cdm/5.38.0 flips —
 * GetAllBusinessCenters + Create_QuantityChange — have no byte lock here: locking them
 * would load a THIRD cell corpus for two same-family files (cdm6 GetAllBusinessCenters
 * is locked below and is byte-identical rosetta; Create_QuantityChange's A3 rung wrap
 * is content-locked via the flip set's byte-oracle + probe dumps).
 */
class FunctionCondListCoerceTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // A2 flip carrier (drr): the ADD distribution with ctor arms, type-named value
    // locals (organisationIdentification15Choice__30/31) + relocated inner ite-hoists.
    private static final String GET_EXCTN_AGT_MAS =
            "drr/projection/iso20022/mas/rewrite/trade/functions/GetExctnAgt.java";
    // A2 flip carrier (drr): six literal arms — `final String string0..5` locals.
    private static final String GET_DAYS_OF_THE_WEEK =
            "drr/regulation/common/functions/GetDaysOfTheWeek.java";
    // A1 flip carrier (drr): the segment-ADD leaf seat (`.addUpiData(ifThenElseResult0)`)
    // + the meta compose (`ReferenceWithMetaProductIdentifier.builder().setValue(...)`).
    private static final String ENRICH_ANNA_DSB =
            "drr/enrichment/upi/functions/Enrich_ReportableEventWithUpiFromAnnaDsb.java";
    // A1 flip carrier (drr): nested-conditional-in-arm-value — inner-first numbering
    // (inner ifThenElseResult8 INSIDE the outer 9's branch) + the else-empty
    // initializer form on the rltshRcrd conditional.
    private static final String CREATE_TRADE_COUNTERPARTY_REPORT =
            "drr/projection/iso20022/fca/ukemir/refit/trade/functions/Create_TradeCounterpartyReport20__1.java";
    // A1 flip carrier (cdm6): two ctor-pair List-form hoists (triggerEvent0/1).
    private static final String MAP_KNOCK =
            "cdm/ingest/fpml/confirmation/product/creditdefaultswapoption/functions/MapKnock.java";
    // A2 flip carrier (cdm6): the else-if ladder with MIXED arm cardinality (single
    // fn-call arm → inner if/else; MULTI fn-call arm → direct addAll) + the meta lift.
    private static final String MAP_FIXED_OR_FLOATING =
            "cdm/ingest/fpml/confirmation/product/creditdefaultswap/functions/MapFixedOrFloatingAmountCalculationToPriceListWithLocation.java";
    // A2 flip carrier (cdm6): the multi-arm direct-addAll shape + relocated meta-deref.
    private static final String GET_ALL_BUSINESS_CENTERS =
            "cdm/base/datetime/functions/GetAllBusinessCenters.java";
    // B3 flip carrier (cdm6): list-literal witness lift + plain setter.
    private static final String MAP_MESSAGE_INFORMATION =
            "cdm/ingest/fpml/confirmation/workflowstep/functions/MapMessageInformation.java";
    // B1 flip carrier (cdm6): META-alias-call value → plain setter.
    private static final String MAP_RATE_OPTION =
            "cdm/ingest/fpml/confirmation/pricequantity/functions/MapRateOptionToObservableWithLocation.java";
    // B2 flip carrier (cdm6): compiled-wrapper (render-truth) nav values → plain setters.
    private static final String CREATE_ON_DEMAND_INTEREST_PAYMENT =
            "cdm/event/common/functions/Create_OnDemandInterestPaymentPrimitiveInstruction.java";
    // C flip carrier (cdm6): the value hoist precedes the ifThenElseResult block.
    private static final String MAP_AMERICAN_EXERCISE_TERMS =
            "cdm/ingest/fpml/confirmation/common/functions/MapAmericanExerciseTerms.java";
    // Green-safety pin (cdm6): GREEN AppendToVector carries the #199 addAll block for a
    // NON-conditional (bare param) value — the A2 distribution must decline it.
    private static final String APPEND_TO_VECTOR_GREEN =
            "cdm/base/math/functions/AppendToVector.java";
    // Green-safety pin (cdm6): GREEN MapAdjustedDateToAdjustableDate legitimately keeps
    // the `set...Value(` form for META-FREE values — the B proof must NOT fire.
    private static final String MAP_ADJUSTED_DATE_GREEN =
            "cdm/ingest/fpml/confirmation/datetime/functions/MapAdjustedDateToAdjustableDate.java";

    private static Map<String, String> drrOutput;
    private static Map<String, String> cdm6Output;

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
        if (drrCellAvailable()) {
            drrOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
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
    @EnabledIf("drrCellAvailable")
    void getExctnAgt_addDistributionWithCtorArms_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, GET_EXCTN_AGT_MAS);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void getDaysOfTheWeek_addDistributionLiteralArms_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, GET_DAYS_OF_THE_WEEK);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void enrichAnnaDsb_segmentAddListFormWithMetaCompose_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, ENRICH_ANNA_DSB);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void createTradeCounterpartyReport_nestedArmValueInnerFirst_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, CREATE_TRADE_COUNTERPARTY_REPORT);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapKnock_ctorPairListFormHoists_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, MAP_KNOCK);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapFixedOrFloating_elseIfLadderMixedArms_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, MAP_FIXED_OR_FLOATING);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void getAllBusinessCenters_multiArmDirectAddAll_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, GET_ALL_BUSINESS_CENTERS);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapMessageInformation_listLiteralWitnessLiftAndPlainSetter_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, MAP_MESSAGE_INFORMATION);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapRateOption_metaAliasValuePlainSetter_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, MAP_RATE_OPTION);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void createOnDemandInterestPayment_compiledWrapperPlainSetters_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, CREATE_ON_DEMAND_INTEREST_PAYMENT);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapAmericanExerciseTerms_hoistConsumptionOrder_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, MAP_AMERICAN_EXERCISE_TERMS);
    }

    // ==== Positive-content locks (revert-RED). ====

    @Test
    @EnabledIf("drrCellAvailable")
    void getExctnAgt_distributesAddAllPerBranchWithTypeNamedLocals() {
        String gen = gen(drrOutput, GET_EXCTN_AGT_MAS);
        assertTrue(gen.contains(
                "exctnAgt.addAll(toBuilder(Collections.singletonList(organisationIdentification15Choice__30)));"),
                "the single ctor arm must hoist the type-named local and singletonList it"
                        + " inside the branch");
        assertTrue(gen.contains(
                "exctnAgt.addAll(toBuilder(Collections.<OrganisationIdentification15Choice__3>emptyList()));"),
                "the else arms must add the typed emptyList");
        assertFalse(gen.contains(".addAll(toBuilder(ifThenElseResult"),
                "no bare ite-local may reach the addAll (the pre-#327 non-golden form)");
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapKnock_declaresListTypedBlankFinalWithPerArmCoercion() {
        String gen = gen(cdm6Output, MAP_KNOCK);
        assertTrue(gen.contains("final List<TriggerEvent> ifThenElseResult0;"),
                "the ctor-pair conditional must hoist the LIST-typed blank-final form");
        assertTrue(gen.contains(
                "ifThenElseResult0 = triggerEvent0 == null ? Collections.<TriggerEvent>emptyList()"
                        + " : Collections.singletonList(triggerEvent0);"),
                "the then-arm must coerce via the type-named local + null-guard ternary");
        assertTrue(gen.contains(".setKnockIn(ifThenElseResult0)"),
                "the setter must splice the list local bare");
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapAmericanExerciseTerms_valueHoistPrecedesIteBlock() {
        String gen = gen(cdm6Output, MAP_AMERICAN_EXERCISE_TERMS);
        int valueHoist = gen.indexOf("final AdjustableOrRelativeDate adjustableOrRelativeDate =");
        int iteBlock = gen.indexOf("ExpirationTimeTypeEnum ifThenElseResult = null;");
        assertTrue(valueHoist >= 0 && iteBlock >= 0,
                "both the value hoist and the ite block must render");
        assertTrue(valueHoist < iteBlock,
                "the pair's coercion-phase value hoist must precede the later pair's"
                        + " ifThenElseResult block (consumption order)");
    }

    // ==== Green-safety locks (pass with AND without the facets). ====

    @Test
    @EnabledIf("cdm6CellAvailable")
    void appendToVector_greenNonConditionalAddBlock_staysByteIdentical() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, APPEND_TO_VECTOR_GREEN);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapAdjustedDate_greenMetaFreeValueForm_staysByteIdentical() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, MAP_ADJUSTED_DATE_GREEN);
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
