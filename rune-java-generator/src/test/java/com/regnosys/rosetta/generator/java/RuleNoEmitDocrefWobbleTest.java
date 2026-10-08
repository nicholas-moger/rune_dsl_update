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
 * PR #330 anchors — the biggest-drains compose: facets {@code noEmitInference}
 * (CP4, parser) + {@code noEmitMetaDelegation}/{@code noEmitSelfInject} (CP5,
 * generator) + {@code docrefCorpusScope}/{@code docrefInheritance} (CP3) +
 * {@code wobbleDeterminism} (CP2).
 *
 * <ul>
 *   <li><b>CP4 (inference):</b> {@code TypeInferenceEngine.MAX_ITERATIONS} 10 → 40
 *       (the drr workspace CONVERGES at 15 passes; the old budget cut the
 *       cross-rule memo-read cascade off mid-convergence — a consumer whose file
 *       sorts AFTER its target in the reversed phase-B order reads one pass
 *       stale, so csa/esma/fca/hkma/jfsa delegators stayed RMissingType while
 *       cftc typed) + a DIRECT rule self-reference types NOTHING (upstream's
 *       least fixed point; golden {@code ReportFunction<I, Void>}). Together they
 *       ELIMINATED all 25 drr no-emit Rule POJOs (the RuleGenerator RMissingType
 *       fail-fast never fires on the drr corpus now).</li>
 *   <li><b>CP5 (meta recovery + self-inject):</b> {@code recoverMetaFromExpr}
 *       gains (e) EXTRACT-descent (a {@code then extract [ <conditional> ]}
 *       terminal reaches the conditional-arm recovery) + (f) the DISGUISED
 *       2-name-chain leaf read ({@code assignedIdentifier -> identifier} via the
 *       parser-bound {@code resolvedAttributeChain}, the #291 pattern), and case
 *       (d) is TIGHTENED to all-present-arms-agree (a MIXED meta+bare conditional
 *       joins BARE — the un-gated first cut regressed 4 GREEN files, caught by
 *       the D11 strict gate; a no-value else is neutral per the #328
 *       isNoValueElse triple). {@code injectRuleDependency} gains the
 *       function-path package guard so a SELF-inject renders the SIMPLE name.</li>
 *   <li><b>CP3 (docref):</b> {@code lookupCorpus} resolves a corpus ref in the
 *       REFERENCING file's scope (import-alias prefix → literal namespace for
 *       qualified refs; own namespace → wildcard imports for simple refs) +
 *       renders the RESOLVED corpus's own simple name; getter javadocs carry ALL
 *       doc references (override-parent chain first — upstream
 *       {@code RAttribute.getAllDocReferences} via the #321
 *       {@code parentAttributeOf} walk).</li>
 *   <li><b>CP2 (wobble determinism):</b> the #144 meta-wrapper recovery adopts a
 *       wrapper iff EXACTLY ONE distinct candidate matches the value type — an
 *       AMBIGUOUS body (two wrappers over one value type) keeps the BARE
 *       inferred type (golden's form; the old first-match scan over the
 *       salted-order {@code Set.copyOf} refs was the #231 run-to-run wobble).</li>
 * </ul>
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of generated output against the
 * frozen 9.83.0 goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. All carriers are drr
 * (6.34.1) POJO-kind rule files — the one cell the compose touches.
 */
class RuleNoEmitDocrefWobbleTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // CP3 (i) flip carrier: 5 same-named DTCC_Specs corpus declarations — the scoped
    // lookup picks cftc's own (the global index picked asic's "(v0.6)" text).
    private static final String CFTC_MARGIN_REPORT =
            "drr/regulation/cftc/rewrite/margin/CFTCMarginReport.java";
    // CP3 (i) flip carrier: 3 same-named CDE corpus declarations — version3's own text.
    private static final String CRITICAL_DATA_ELEMENT_V3 =
            "drr/standards/iosco/cde/version3/CriticalDataElement.java";
    // CP3 (i) flip carrier: BOTH sub-bugs on one line — the import-ALIASED qualified ref
    // (cftcTrade.Trade -> golden `Corpus Dissemination Trade`, the resolved corpus's own
    // simple name) + the wrong-version DTCC_Specs text.
    private static final String DTCC_ADDITIONAL_FIELDS =
            "drr/regulation/cftc/rewrite/dtcc/DTCCAdditionalFields.java";
    // CP3 (ii) flip carrier: the specialized leg1/leg2 override getters inherit the
    // Case-0 annotations-only parent's regulatoryReference blocks (1,039 golden lines).
    private static final String CFTC_PART45_TRANSACTION_REPORT =
            "drr/regulation/cftc/rewrite/trade/CFTCPart45TransactionReport.java";
    // CP4 flip carrier (formerly NO-EMIT): a `filter … then extract <rule ref>` delegator
    // whose target types only past iteration 10 — the plain delegation form, NO meta wrap
    // (the inner rule's conditional arms are MIXED, so the tightened case (d) declines).
    private static final String ESMA_NAME_OF_THE_UNDERLYING_INDEX =
            "drr/regulation/esma/emir/refit/trade/reports/NameOfTheUnderlyingIndexRule.java";
    // CP4 flip carrier (formerly NO-EMIT): the direct delegation `cdeV1.basket.CustomBasketCode`
    // — types once the budget lets the v2 -> v1 memo-read cascade converge.
    private static final String IOSCO_V2_CUSTOM_BASKET_CODE =
            "drr/standards/iosco/cde/version2/basket/reports/CustomBasketCodeRule.java";
    // CP4+CP5 flip carrier (formerly NO-EMIT): the DIRECT SELF-REFERENCE — types NOTHING
    // (ReportFunction<I, Void> + `output = null;`) and self-@Injects under the SIMPLE name.
    private static final String IOSCO_V2_REPORTING_TIMESTAMP =
            "drr/standards/iosco/cde/version2/datetime/reports/ReportingTimestampRule.java";
    // CP5 flip carrier (formerly NO-EMIT): the #265 value->meta wrap fires through the
    // v3 -> v2 -> v1 delegation chain + the (e) extract-descent + (d) all-arms-agree
    // (v1's ladder arms ALL end at [metadata scheme] leaves).
    private static final String CSA_CUSTOM_BASKET_CODE =
            "drr/regulation/csa/rewrite/trade/reports/CustomBasketCodeRule.java";
    // CP5 flip carrier (previously emitting, healed): the (f) disguised-chain leaf —
    // the common inner rule's meta terminal is `then extract assignedIdentifier ->
    // identifier` (an REnumValueRef the walker was blind to).
    private static final String CFTC_DTCC_DELIVERY_LOCATION =
            "drr/regulation/cftc/rewrite/dtcc/reports/DTCC_DeliveryLocationRule.java";
    // CP5 flip carrier (previously emitting, healed): the #329-predicted Counterparty2Name
    // compounding — the recovery extension healed its last divergence.
    private static final String COMMON_COUNTERPARTY2_NAME =
            "drr/regulation/common/trade/party/reports/Counterparty2NameRule.java";
    // Green-safety pin (GREEN): the CP5 first-cut regression carrier — its inner rule's
    // conditional is MIXED (meta arm + bare arm), so the all-arms-agree gate must keep
    // the plain form byte-identical.
    private static final String ASIC_COUNTERPARTY2_GREEN =
            "drr/regulation/asic/rewrite/trade/reports/Counterparty2Rule.java";
    // Green-safety pin (GREEN): the same filter+extract+`as`-clause delegation shape as
    // the no-emit family, green pre-#330 — the inference + recovery + self-inject changes
    // must keep its bytes.
    private static final String CSA_DTCC_CLEARING_VENUE_ID_TYPE_GREEN =
            "drr/regulation/csa/rewrite/dtcc/reports/DTCC_ClearingVenueIDTypeRule.java";
    // CP2 content-lock carrier (WAIVERED, stays divergent): the wobble pool member whose
    // thenArg decl wobbled ReferenceWithMeta <-> FieldWithMeta per run pre-#330.
    private static final String EQUITY_NOTIONAL_QUANTITY =
            "drr/regulation/common/trade/quantity/reports/EquityNotionalQuantityRule.java";

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
    void cftcMarginReport_scopedCorpusText_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(CFTC_MARGIN_REPORT);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void criticalDataElementV3_ownVersionCorpusText_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(CRITICAL_DATA_ELEMENT_V3);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void dtccAdditionalFields_aliasedCorpusRefResolvedName_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(DTCC_ADDITIONAL_FIELDS);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void cftcPart45_inheritedDocReferences_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(CFTC_PART45_TRANSACTION_REPORT);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void esmaNameOfTheUnderlyingIndex_budgetDelegatorEmits_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(ESMA_NAME_OF_THE_UNDERLYING_INDEX);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void ioscoV2CustomBasketCode_directDelegationEmits_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(IOSCO_V2_CUSTOM_BASKET_CODE);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void ioscoV2ReportingTimestamp_selfCycleTypesVoid_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(IOSCO_V2_REPORTING_TIMESTAMP);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void csaCustomBasketCode_metaWrapThroughDelegationChain_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(CSA_CUSTOM_BASKET_CODE);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void cftcDtccDeliveryLocation_disguisedChainLeafMeta_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(CFTC_DTCC_DELIVERY_LOCATION);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void commonCounterparty2Name_recoveryCompounding_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(COMMON_COUNTERPARTY2_NAME);
    }

    // ==== Positive-content locks (revert-RED). ====

    @Test
    @EnabledIf("drrCellAvailable")
    void reportingTimestamp_voidOutputAndSimpleSelfInject() {
        String gen = gen(IOSCO_V2_REPORTING_TIMESTAMP);
        assertTrue(gen.contains("ReportFunction<TransactionReportInstruction, Void>"),
                "the self-cycle must type NOTHING -> a Void report output");
        assertTrue(gen.contains("@Inject protected ReportingTimestampRule reportingTimestampRule;"),
                "the SELF-inject must render the SIMPLE name (a class never imports itself)");
        assertFalse(gen.contains(
                "@Inject protected drr.standards.iosco.cde.version2.datetime.reports.ReportingTimestampRule"),
                "the pre-#330 name-only collision check FQN-inlined the self-inject");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void equityNotionalQuantity_ambiguousWrapperKeepsBareType() {
        // WAIVERED carrier (stays divergent on the block-lambda facets) — this locks the
        // CP2 DETERMINISM law only: the body carries BOTH ReferenceWithMeta- and
        // FieldWithMeta-NonNegativeQuantitySchedule, so the thenArg decl must keep the
        // BARE element type (golden's form; the pre-#330 first-match scan rendered a
        // per-run-random wrapper — the #231 wobble).
        String gen = gen(EQUITY_NOTIONAL_QUANTITY);
        assertTrue(gen.contains("final MapperS<NonNegativeQuantitySchedule> thenArg"),
                "the ambiguous #144 recovery must keep the BARE element type");
        assertFalse(gen.contains("final MapperS<ReferenceWithMetaNonNegativeQuantitySchedule> thenArg"),
                "no per-run-random ReferenceWithMeta wrapper pick");
        assertFalse(gen.contains("final MapperS<FieldWithMetaNonNegativeQuantitySchedule> thenArg"),
                "no per-run-random FieldWithMeta wrapper pick");
    }

    // ==== Green-safety pins. ====

    @Test
    @EnabledIf("drrCellAvailable")
    void asicCounterparty2_mixedArmsDecline_staysByteIdentical() throws IOException {
        assertByteMatchesGolden(ASIC_COUNTERPARTY2_GREEN);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void csaDtccClearingVenueIdType_greenDelegation_staysByteIdentical() throws IOException {
        assertByteMatchesGolden(CSA_DTCC_CLEARING_VENUE_ID_TYPE_GREEN);
    }

    private static String gen(String path) {
        assertNotNull(drrOutput, "generation did not run for the drr cell");
        String gen = drrOutput.get(path);
        assertNotNull(gen, "missing generated file: " + path);
        return gen;
    }

    private void assertByteMatchesGolden(String path) throws IOException {
        String gen = gen(path);
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "missing golden: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(gen),
                path + " must byte-match the frozen 9.83.0 golden");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
