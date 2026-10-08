package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
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
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #358 — the then-var re-wrap elision + the alias element joins + the withMeta lambda
 * channel + the enum-owner re-qualify + the alias/ladder value then-hoist: 33 byte flips
 * (13 drr POJO Rules + 11 drr FUNCTION + 3 cdm5 FUNCTION + 6 cdm6 FUNCTION) + 0 new /
 * 0 content-away (regscan358 — the 2 line-ratio movers content-verified TOWARD).
 *
 * <p><b>F-A — {@code thenVarRewrapElision}</b> (NavigationHandler): a nav-receiver
 * closure param renders BARE through the #342 closureParamDirectNav arm once its element
 * resolves — the walk gains (1) the RULE-TOP implicit fallback (the bare rule input types
 * as the rule from-type), (2) the SINGLE-BRANCH conditional join (an absent OR
 * materialized-empty-{@code RListLiteral} else joins to the then-branch), (3) the
 * rule-gated item-rooted disguised-EVR arm (the #225 route inlined with the on-stack
 * set), (4) {@code chainRendersMapperC}'s closure-param-rooted disguise arm (the BARE
 * MapperC Type-coercion deref), and (5) the element-derived lambda var pre-escapes the
 * self-shadow ({@code businessEvent.<Instruction>mapC("getInstruction",
 * _businessEvent -> …)}).
 *
 * <p><b>F-B — {@code aliasCallElementType}</b> (NavigationHandler + FunctionAliasHelper):
 * the conditional-branch ANCESTOR join (LUB on the {@code extends} chain — golden
 * {@code quantitySchedule} from the Quantity/QuantitySchedule join), the
 * {@code RConstructorExpr} receiver arm, and the extract-item disguise arm in the alias
 * signature walk ({@code MapperC<trade>} → {@code MapperC<? extends TradeIdentifier>}).
 *
 * <p><b>F-C — {@code withMetaExternalKeyLambda}</b> (ConstructionHandler): the #214
 * POJO-meta with-meta gains the LAMBDA_CHANNEL twin — the {@code withMetaArgument} decl +
 * {@code getOrCreateMeta().setExternalKey(…)} register as pending lambda hoists and the
 * mapItem lambda returns {@code MapperS.of(withMetaArgument)} (the block form).
 *
 * <p><b>F-D — {@code switchCaseEnumOwner}</b> (FunctionExpressionRenderer): a mis-bound
 * bare enum case result re-qualifies against the enclosing function's OUTPUT enum via the
 * #211 hierarchy walk ({@code OptionTypeEnum extends PutCallEnum} — the flattened child
 * constant {@code OptionTypeEnum.CALL} + the wrong owner's import pruned).
 *
 * <p><b>F-E — {@code aliasValueThenHoist}</b> (CollectionHandler): the plain-element
 * bare-item only-element anchor (the #352 i1b meta arm's sibling), the shared-walk
 * Object-snapshot element recovery, and the #339 ladder-arm blessing widened to ctl-free
 * hoistable VALUE chains (golden's in-rung {@code thenArg0/1/2} decls).
 *
 * <p>Whole-file byte comparisons run through the REAL D11 generation paths and revert
 * RED without the facets. The negative witnesses are load-bearing per the
 * witness-uniqueness law (OCCURRENCE counts, never line counts): every token below was
 * occurrence-counted in its PRE gen (f-probe-357post; counts noted per witness) and 0
 * in its golden — the flips REMOVE them.
 */
class ThenVarRewrapElisionComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    /** The 13 F-A/F-E drr POJO Rule flip carriers. */
    private static final String[] DRR_POJO_RULES = {
            "drr/regulation/common/trade/execution/reports/TraderLocationRule.java",
            "drr/standards/iosco/cde/version1/quantity/reports/NotionalCurrencyLeg1Rule.java",
            "drr/standards/iosco/cde/version1/quantity/reports/NotionalCurrencyLeg2Rule.java",
            "drr/standards/iosco/cde/version1/party/reports/Beneficiary1Rule.java",
            "drr/standards/iosco/cde/version1/party/reports/Beneficiary2Rule.java",
            "drr/regulation/cftc/rewrite/trade/reports/ClearingExceptionsAndExemptionsCounterparty1Rule.java",
            "drr/regulation/cftc/rewrite/trade/reports/ClearingExceptionsAndExemptionsCounterparty2Rule.java",
            "drr/regulation/cftc/rewrite/trade/reports/ClearingSwapUSIsRule.java",
            "drr/regulation/cftc/rewrite/trade/reports/ClearingSwapUTIsRule.java",
            "drr/regulation/cftc/rewrite/margin/reports/Counterparty2IdentifierSourceRule.java",
            "drr/regulation/cftc/rewrite/valuation/reports/Counterparty2IdentifierSourceRule.java",
            "drr/regulation/csa/rewrite/margin/reports/Counterparty2IdentifierSourceRule.java",
            "drr/regulation/csa/rewrite/valuation/reports/Counterparty2IdentifierSourceRule.java",
    };

    /** The 11 F-A/F-E drr FUNCTION flip carriers. */
    private static final String[] DRR_FUNCTIONS = {
            "drr/enrichment/upi/functions/DebtSeniority.java",
            "drr/regulation/common/trade/index/functions/GetSeniority.java",
            "drr/standards/iosco/cde/version1/party/functions/Direction1.java",
            "drr/standards/iosco/cde/version1/party/functions/Direction1BuyerParty.java",
            "drr/standards/iosco/cde/version1/party/functions/Direction1SellerParty.java",
            "drr/regulation/common/functions/ExtractCommodityClassificationLeg1.java",
            "drr/regulation/common/functions/ExtractCommodityClassificationLeg2.java",
            "drr/enrichment/upi/functions/Create_AnnaDsbUpiRequestOtherLegUnderlyingForRate.java",
            "drr/enrichment/common/event/functions/Create_AlphaTerminationReportableEventFromBetaAndGamma.java",
            "drr/regulation/common/functions/GetDSBRecord.java",
            "drr/regulation/common/functions/GetDeliveryTypeDSBRecord.java",
    };

    /** The 3 cdm5 F-B flip carriers. */
    private static final String[] CDM5_FUNCTIONS = {
            "cdm/event/common/functions/SecurityFinanceCashSettlementAmount.java",
            "cdm/event/common/functions/Create_AssetTransfer.java",
            "cdm/event/common/functions/Create_PairOffInstruction.java",
    };

    /** The 6 cdm6 F-B/F-C/F-D flip carriers. */
    private static final String[] CDM6_FUNCTIONS = {
            "cdm/event/common/functions/SecurityFinanceCashSettlementAmount.java",
            "cdm/event/common/functions/Create_PairOffInstruction.java",
            "cdm/ingest/fpml/confirmation/other/functions/MapOptionTypeEnum.java",
            "cdm/ingest/fpml/confirmation/pricequantity/functions/MapNonNegativeStepListToDatedValueList.java",
            "cdm/ingest/fpml/confirmation/pricequantity/functions/MapQuantityStepListToDatedValueList.java",
            "cdm/ingest/fpml/confirmation/pricequantity/functions/MapScheduleToDatedValueList.java",
    };

    private static Map<String, String> drrFnOutput;
    private static Map<String, String> drrCellOutput;
    private static Map<String, String> cdm5FnOutput;
    private static Map<String, String> cdm6FnOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    static boolean cdmCellsAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR)
                && Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            var drrCell = new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT);
            drrFnOutput = generateFunctions(drrCell);
            drrCellOutput = generateCell(drrCell);
        }
        if (cdmCellsAvailable()) {
            cdm5FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
            cdm6FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
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
        var errors = funcGen.generateWithErrors(output);
        assertTrue(errors.isEmpty(),
                () -> "Function generation reported " + errors.size() + " error(s): " + errors);
        return output;
    }

    /** The REAL D11 full-cell path (the POJO/Rule kinds ride RuleGenerator/ReportGenerator). */
    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell),
                D11CorpusRegressionTest.readDoNotPrune(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var enumGen = new EnumGenerator(gm);
        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                assertNoGenerationErrors(enumGen.generateClasses(model, version, output));
                assertNoGenerationErrors(pojoGen.generateClasses(model, version, output));
                assertNoGenerationErrors(choiceGen.generateClasses(model, version, output));
                assertNoGenerationErrors(ruleGen.generateClasses(model, version, output));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
            }
        }
        return output;
    }

    // -------------------------------------- F-A/F-E drr POJO Rule byte locks (13)

    @Test
    @EnabledIf("drrCellAvailable")
    void faFeDrrPojoRules_byteMatchGolden() throws IOException {
        for (String path : DRR_POJO_RULES) {
            assertCellByteMatchesGolden(path);
        }
    }

    // -------------------------------------- F-A/F-E drr FUNCTION byte locks (11)

    @Test
    @EnabledIf("drrCellAvailable")
    void faFeDrrFunctions_byteMatchGolden() throws IOException {
        for (String path : DRR_FUNCTIONS) {
            assertDrrFnByteMatchesGolden(path);
        }
    }

    // -------------------------------------- F-B cdm5 FUNCTION byte locks (3)

    @Test
    @EnabledIf("cdmCellsAvailable")
    void fbCdm5Functions_byteMatchGolden() throws IOException {
        for (String path : CDM5_FUNCTIONS) {
            assertBytes(path, cdmFn(cdm5FnOutput, path), CDM5_GOLDEN_DIR);
        }
    }

    // -------------------------------------- F-B/F-C/F-D cdm6 FUNCTION byte locks (6)

    @Test
    @EnabledIf("cdmCellsAvailable")
    void fbFcFdCdm6Functions_byteMatchGolden() throws IOException {
        for (String path : CDM6_FUNCTIONS) {
            assertBytes(path, cdmFn(cdm6FnOutput, path), CDM6_GOLDEN_DIR);
        }
    }

    // ------------------------------------------------------- negative witnesses

    /**
     * The F-A negative witness (TraderLocation): the shadow-wrap
     * {@code MapperS.of(reportInstruction).map("getReportingSide"} counted EXACTLY 1
     * occurrence in the PRE gen and 0 in the golden — the elision renders the bare typed
     * {@code reportInstruction.<ReportingSide>map("getReportingSide",
     * transactionReportInstruction -> …)}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void traderLocation_shadowWrapGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[0]),
                        "MapperS.of(reportInstruction).map(\"getReportingSide\""),
                "The then-var shadow wrap is gone (count 0 in golden)");
    }

    /**
     * The F-A negative witness (ClearingSwapUSIs): the shadow-wrap
     * {@code MapperS.of(businessEvent).map("getInstruction"} counted EXACTLY 1 occurrence
     * in the PRE gen and 0 in the golden — the elision renders
     * {@code businessEvent.<Instruction>mapC("getInstruction", _businessEvent -> …)}
     * (the mapC cardinality + the self-shadow escape + the BARE MapperC coercion deref).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void clearingSwapUsis_shadowWrapGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[7]),
                        "MapperS.of(businessEvent).map(\"getInstruction\""),
                "The then-var shadow wrap is gone (count 0 in golden)");
    }

    /**
     * The F-E negative witness (cftc-margin Counterparty2IdentifierSource): the in-arm
     * runtime {@code .then(_item -> _item} chain counted EXACTLY 3 occurrences in the PRE
     * gen and 0 in the golden — the widened ladder-arm blessing hoists golden's in-rung
     * {@code final MapperC<PersonIdentifierTypeEnum> thenArg0/1} + {@code final
     * MapperS<PersonIdentifierTypeEnum> thenArg2} decls before the re-rooted return.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty2IdentifierSource_inArmRuntimeThenGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[9]), ".then(_item -> _item"),
                "The in-arm runtime .then( chain is gone (count 0 in golden)");
    }

    /**
     * The F-E negative witness (Create_AlphaTerminationReportableEvent): the
     * only-element collapse step {@code .then(item -> item.get())} counted EXACTLY 1
     * occurrence in the PRE gen and 0 in the golden — the alias hoist renders
     * {@code final MapperS<RelatedParty> thenArg1 = MapperS.of(thenArg0.get());}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void alphaTermination_collapseThenGone() {
        assertEquals(0, count(drrFn(DRR_FUNCTIONS[8]), ".then(item -> item.get())"),
                "The only-element collapse .then( step is gone (count 0 in golden)");
    }

    /**
     * The F-B negative witness (cdm5 Create_AssetTransfer): the witness-less
     * symbol-named deref {@code .map("getValue", _securityQuantity ->} counted EXACTLY 1
     * occurrence in the PRE gen and 0 in the golden — the ancestor-join + ctor arm type
     * the alias receiver, so the nav renders {@code .<BigDecimal>map("getValue",
     * quantity -> quantity.getValue())}.
     */
    @Test
    @EnabledIf("cdmCellsAvailable")
    void assetTransfer_witnessLessDerefGone() {
        assertEquals(0, count(cdmFn(cdm5FnOutput, CDM5_FUNCTIONS[1]),
                        ".map(\"getValue\", _securityQuantity ->"),
                "The witness-less symbol-named deref is gone (count 0 in golden)");
    }

    /**
     * The F-B negative witness (cdm5 Create_PairOffInstruction): the leaked lowercase
     * signature element {@code MapperC<trade>} counted EXACTLY 2 occurrences in the PRE
     * gen (abstract + impl decls) and 0 in the golden — the extract-item disguise arm
     * types the alias signature {@code MapperC<? extends TradeIdentifier>}.
     */
    @Test
    @EnabledIf("cdmCellsAvailable")
    void pairOffInstruction_signatureEchoGone() {
        assertEquals(0, count(cdmFn(cdm5FnOutput, CDM5_FUNCTIONS[2]), "MapperC<trade>"),
                "The lowercase signature echo is gone (count 0 in golden)");
    }

    /**
     * The F-C negative witness (cdm6 MapNonNegativeStepList): the inline wrapper-form
     * meta {@code .setMeta(MetaFields.builder().setKey(} counted EXACTLY 1 occurrence in
     * the PRE gen and 0 in the golden — the lambda channel hoists the
     * {@code withMetaArgument} decl + {@code getOrCreateMeta().setExternalKey(…)} and
     * returns {@code MapperS.of(withMetaArgument)}.
     */
    @Test
    @EnabledIf("cdmCellsAvailable")
    void mapNonNegativeStepList_inlineSetMetaGone() {
        assertEquals(0, count(cdmFn(cdm6FnOutput, CDM6_FUNCTIONS[3]),
                        ".setMeta(MetaFields.builder().setKey("),
                "The inline setMeta(setKey) form is gone (count 0 in golden)");
    }

    /**
     * The F-D negative witness (cdm6 MapOptionTypeEnum): the wrong-owner constant
     * {@code PutCallEnum.CALL} counted EXACTLY 1 occurrence in the PRE gen and 0 in the
     * golden — the output-enum re-qualify renders the flattened child constant
     * {@code OptionTypeEnum.CALL} and the {@code PutCallEnum} import prunes.
     */
    @Test
    @EnabledIf("cdmCellsAvailable")
    void mapOptionTypeEnum_wrongOwnerGone() {
        assertEquals(0, count(cdmFn(cdm6FnOutput, CDM6_FUNCTIONS[2]), "PutCallEnum.CALL"),
                "The wrong-owner enum constant is gone (count 0 in golden)");
    }

    // ---------------------------------------------------------------- helpers

    private static int count(String haystack, String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
    }

    private static String drrFn(String path) {
        assertNotNull(drrFnOutput, "function generation did not run — corpus unavailable?");
        String g = drrFnOutput.get(path);
        assertNotNull(g, "Function not generated: " + path);
        return g;
    }

    private static String cdmFn(Map<String, String> output, String path) {
        assertNotNull(output, "cdm function generation did not run — corpus unavailable?");
        String g = output.get(path);
        assertNotNull(g, "Function not generated: " + path);
        return g;
    }

    private static String cell(String path) {
        assertNotNull(drrCellOutput, "cell generation did not run — corpus unavailable?");
        String g = drrCellOutput.get(path);
        assertNotNull(g, "Class not generated: " + path);
        return g;
    }

    private static void assertDrrFnByteMatchesGolden(String path) throws IOException {
        assertBytes(path, drrFn(path), DRR_GOLDEN_DIR);
    }

    private static void assertCellByteMatchesGolden(String path) throws IOException {
        assertBytes(path, cell(path), DRR_GOLDEN_DIR);
    }

    private static void assertBytes(String path, String gen, Path goldenDir) throws IOException {
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath).replace("\r", "");
        assertEquals(golden, gen.replace("\r", ""),
                "Generated bytes must match the golden for " + path);
    }
}
