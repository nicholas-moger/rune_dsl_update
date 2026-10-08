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
 * PR #360 — the evaluate-arg expectedType reset + the rule meta-list wrap + the rule-ref
 * wrap thenArg + the output-meta-deref gates + the mapItem closure-param receiver: 18
 * byte flips (13 drr POJO Rules + 2 drr FUNCTION + 1 cdm5 + 2 cdm6 FUNCTION) + 0 new /
 * 0 away (regscan360 — 7 TOWARD movers all content-verified at checkpoints).
 *
 * <p><b>F-A — {@code evaluateArgExpectedTypeReset}</b> (ReferenceHandler +
 * FunctionExpressionRenderer): a call boundary RESETS the argument expectation — args
 * compile NEUTRAL, so a meta-wrapper arg reaches {@code tryMetaDerefArg} instead of
 * being inline-coerced by the caller's context expectation; {@code armHoistPullSet}
 * admits the {@code FieldWithMeta}/{@code ReferenceWithMeta} decl class into the owning
 * branch (ExchangeRateBasisRule iosco).
 *
 * <p><b>F-A — {@code ctorNavMetaDerefBlockArm}</b> (ConstructionHandler +
 * CollectionHandler): the #312 ctor-setter NAV meta-deref helper admits the
 * conditional-arm seat through the #354 {@code blockArmSeatConditionals} channel
 * (registered by {@code compileElselessConditionalBlock} around its arm compiles); the
 * marker-classed decl drains INSIDE the if-branch and the bare-CTOR then-arm wraps
 * {@code MapperS.of} including the multi-line chain form (IdentifierOfBasket-
 * ConstituentsRule esma/fca; the ctor-arm wrap alone flips MapEventTimestamp cdm6).
 *
 * <p><b>F-B — {@code ruleMetaListWrap}</b> (NavigationHandler + ReferenceHandler +
 * FunctionExpressionRenderer): a MULTI inner rule whose rosetta OUTPUT is a META
 * wrapper (the recovery walker gains the extract-wrapper then-chain descent, the
 * identity-arm conditional filter skip and the RECOVERY-LOCAL rule-context disguised
 * leaf) re-presents the MODEL type: the lambda-seat stream/map/collect element wrap +
 * the element-wise Type-coercion deref at BOTH whole-output seats
 * (DTCC_TradeParty1/2ReportingDestinationRule cftc/csa dtcc ×4).
 *
 * <p><b>F-C — {@code ruleRefWrapThenArg}</b> (FunctionExpressionRenderer +
 * ReferenceHandler + CollectionHandler): a k==0 bare-RULE thenArg base whose inner
 * rule's output is a REFERENCE meta wrapper hoists the bare evaluate() value and
 * re-types the decl to {@code MapperS<ReferenceWithMetaX>} with the #264 null-guard
 * wrap ternary; the in-lambda consumers deref through the #354-channel-admitted
 * CONDITION/ARM seats (SpreadLeg1/2 Currency+NotationEnum iosco ×4).
 *
 * <p><b>F-D — {@code outputMetaDerefIfNull}</b> (FunctionExpressionRenderer): the
 * #290/#320/#340 collapse recovery drops its RULE gate and the #353 extract-wrapped
 * nested-chain recovery drops its FUNCTION gate — the SAME hoist + if-null deref
 * emission serves both paths (ProductClassificationRule esma/fca + GetVenueOfExecution
 * + GetPriorTransactionIdentifier).
 *
 * <p><b>F-G — {@code mapItemClosureParamReceiver}</b> (NavigationHandler):
 * {@code chainProvesMulti} gains the closure-param disguised-leaf arm (a 2-name chain
 * rooted at an ENCLOSING extract's explicit param resolves the leaf on the param
 * element — monotone multi-only), so the mapC-tailed receiver selects {@code mapItem};
 * the #358 self-shadow pre-escape extends to RESOLVED-attribute receivers whose derived
 * lambda var equals an enclosing explicit param
 * (UpdateAmountForEachMatchingQuantity cdm5 + cdm6).
 *
 * <p>Whole-file byte comparisons run through the REAL D11 generation paths and revert
 * RED without the facets. The negative witnesses are load-bearing per the
 * witness-uniqueness law (OCCURRENCE counts, never line counts): every token below was
 * occurrence-counted in its PRE gen (f-probe-359post; counts noted per witness) and 0
 * in its golden — the flips REMOVE them.
 */
class RuleMetaListWrapComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    /** The 13 F-A/F-B/F-C/F-D drr POJO Rule flip carriers. */
    private static final String[] DRR_POJO_RULES = {
            "drr/standards/iosco/cde/version1/price/reports/ExchangeRateBasisRule.java",
            "drr/regulation/esma/emir/refit/trade/reports/IdentifierOfBasketConstituentsRule.java",
            "drr/regulation/fca/ukemir/refit/trade/reports/IdentifierOfBasketConstituentsRule.java",
            "drr/regulation/cftc/rewrite/dtcc/reports/DTCC_TradeParty1ReportingDestinationRule.java",
            "drr/regulation/cftc/rewrite/dtcc/reports/DTCC_TradeParty2ReportingDestinationRule.java",
            "drr/regulation/csa/rewrite/dtcc/reports/DTCC_TradeParty1ReportingDestinationRule.java",
            "drr/regulation/csa/rewrite/dtcc/reports/DTCC_TradeParty2ReportingDestinationRule.java",
            "drr/standards/iosco/cde/version1/price/reports/SpreadLeg1CurrencyRule.java",
            "drr/standards/iosco/cde/version1/price/reports/SpreadLeg2CurrencyRule.java",
            "drr/standards/iosco/cde/version1/price/reports/SpreadLeg1NotationEnumRule.java",
            "drr/standards/iosco/cde/version1/price/reports/SpreadLeg2NotationEnumRule.java",
            "drr/regulation/esma/emir/refit/trade/reports/ProductClassificationRule.java",
            "drr/regulation/fca/ukemir/refit/trade/reports/ProductClassificationRule.java",
    };

    /** The 2 F-D drr FUNCTION flip carriers. */
    private static final String[] DRR_FUNCTIONS = {
            "drr/regulation/common/functions/GetVenueOfExecution.java",
            "drr/regulation/common/trade/link/functions/GetPriorTransactionIdentifier.java",
    };

    private static final String UPDATE_AMOUNT =
            "cdm/product/common/settlement/functions/UpdateAmountForEachMatchingQuantity.java";
    private static final String MAP_EVENT_TIMESTAMP =
            "cdm/ingest/fpml/confirmation/datetime/functions/MapEventTimestamp.java";

    private static Map<String, String> drrFnOutput;
    private static Map<String, String> drrCellOutput;
    private static Map<String, String> cdm5FnOutput;
    private static Map<String, String> cdm6FnOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

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
        if (drrCellAvailable()) {
            var drrCell = new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT);
            drrFnOutput = generateFunctions(drrCell);
            drrCellOutput = generateCell(drrCell);
        }
        if (cdm5CellAvailable()) {
            cdm5FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
        }
        if (cdm6CellAvailable()) {
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

    // ------------------------- F-A/F-B/F-C/F-D drr POJO Rule byte locks (13)

    @Test
    @EnabledIf("drrCellAvailable")
    void fAtoFDDrrPojoRules_byteMatchGolden() throws IOException {
        for (String path : DRR_POJO_RULES) {
            assertCellByteMatchesGolden(path);
        }
    }

    // ------------------------------------- F-D drr FUNCTION byte locks (2)

    @Test
    @EnabledIf("drrCellAvailable")
    void fDDrrFunctions_byteMatchGolden() throws IOException {
        for (String path : DRR_FUNCTIONS) {
            assertDrrFnByteMatchesGolden(path);
        }
    }

    // ------------------------------------- F-A/F-G cdm FUNCTION byte locks (3)

    @Test
    @EnabledIf("cdm5CellAvailable")
    void fGUpdateAmountCdm5_byteMatchesGolden() throws IOException {
        assertBytes(UPDATE_AMOUNT, fn(cdm5FnOutput, UPDATE_AMOUNT), CDM5_GOLDEN_DIR);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void fGUpdateAmountCdm6_byteMatchesGolden() throws IOException {
        assertBytes(UPDATE_AMOUNT, fn(cdm6FnOutput, UPDATE_AMOUNT), CDM6_GOLDEN_DIR);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void fAMapEventTimestampCdm6_byteMatchesGolden() throws IOException {
        assertBytes(MAP_EVENT_TIMESTAMP, fn(cdm6FnOutput, MAP_EVENT_TIMESTAMP), CDM6_GOLDEN_DIR);
    }

    // ------------------------------------------------------- negative witnesses

    /**
     * The F-A arm-hoist witness (ExchangeRateBasisRule iosco): the collapsed inline
     * deref {@code .getValue().get())} counted EXACTLY 2 occurrences in the PRE gen
     * (both string-concat arms) and 0 in the golden — the neutral arg compile + the
     * pulled {@code fieldWithMetaString2/3} branch decls replace it.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void exchangeRateBasis_collapsedInlineDerefGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[0]), ".getValue().get())"),
                "The collapsed inline deref is gone (count 0 in golden)");
    }

    /**
     * The F-A ctor-arm witness (esma basket): the BARE ctor setter value
     * {@code productIdentifier.getIdentifier()).get())} counted EXACTLY 1 occurrence in
     * the PRE gen and 0 in the golden — the block-arm channel hoists the wrapper and
     * the setter takes the guarded deref.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void basketConstituents_bareSetterCollapseGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[1]),
                        "productIdentifier.getIdentifier()).get())"),
                "The bare meta-collapse setter value is gone (count 0 in golden)");
    }

    /**
     * The F-B witness (cftc dtcc TradeParty1): the BARE-element list wrap
     * {@code MapperC.<SupervisoryBodyEnum>of(} counted EXACTLY 1 occurrence in the PRE
     * gen and 0 in the golden — the model type re-presents as
     * {@code MapperC.<FieldWithMetaSupervisoryBodyEnum>of(…stream()…)} with the
     * element-wise Type-coercion deref before the List collapse.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccTradeParty_bareEnumListWrapGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[3]), "MapperC.<SupervisoryBodyEnum>of("),
                "The bare-element list wrap is gone (count 0 in golden)");
    }

    /**
     * The F-C witness (SpreadLeg1Currency): the BARE-typed thenArg base decl value
     * {@code = MapperS.of(spreadLeg1Rule.evaluate(input));} counted EXACTLY 1
     * occurrence in the PRE gen and 0 in the golden — the decl re-types to the
     * REFERENCE wrapper with the value hoist + null-guard wrap ternary.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void spreadLeg_bareRuleBaseDeclGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[7]),
                        "= MapperS.of(spreadLeg1Rule.evaluate(input));"),
                "The bare-typed rule-base thenArg decl is gone (count 0 in golden)");
    }

    /**
     * The F-D rule-path witness (esma ProductClassification): the bare whole-output
     * assignment head {@code output = thenArg1} counted EXACTLY 1 occurrence in the PRE
     * gen and 0 in the golden — the recovery hoists the wrapper and distributes the
     * if-null deref block.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void productClassification_bareOutputAssignGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[11]), "output = thenArg1"),
                "The bare whole-output assignment is gone (count 0 in golden)");
    }

    /**
     * The F-D function-path witness (GetVenueOfExecution): the bare output assignment
     * head {@code venueOfExecution = thenArg4} counted EXACTLY 1 occurrence in the PRE
     * gen and 0 in the golden — the collapse recovery now serves the FUNCTION path.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getVenueOfExecution_bareOutputAssignGone() {
        assertEquals(0, count(drrFn(DRR_FUNCTIONS[0]), "venueOfExecution = thenArg4"),
                "The bare function-output assignment is gone (count 0 in golden)");
    }

    /**
     * The F-D function-path witness (GetPriorTransactionIdentifier): the bare re-wrap
     * collapse {@code uti = MapperS.of(thenArg4.get()).get();} counted EXACTLY 1
     * occurrence in the PRE gen and 0 in the golden.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getPriorTransactionIdentifier_bareCollapseGone() {
        assertEquals(0, count(drrFn(DRR_FUNCTIONS[1]),
                        "uti = MapperS.of(thenArg4.get()).get();"),
                "The bare re-wrap collapse is gone (count 0 in golden)");
    }

    /**
     * The F-G witness (UpdateAmount cdm5): the single-receiver map form
     * {@code .mapSingleToItem(item -> MapperS.of(updatePriceAmountForEachMatchingQuantity}
     * counted EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the
     * closure-param leaf proves the mapC-tailed receiver MULTI, selecting
     * {@code mapItem}.
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void updateAmount_mapSingleToItemGone() {
        assertEquals(0, count(fn(cdm5FnOutput, UPDATE_AMOUNT),
                        ".mapSingleToItem(item -> MapperS.of(updatePriceAmountForEachMatchingQuantity"),
                "The single-receiver map form is gone (count 0 in golden)");
    }

    /**
     * The F-A ctor-arm witness (MapEventTimestamp cdm6): the UNwrapped ctor arm return
     * {@code return EventTimestamp.builder()} counted EXACTLY 4 occurrences in the PRE
     * gen (four elseless arms) and 0 in the golden — every arm wraps
     * {@code MapperS.of(EventTimestamp.builder()…build())}.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapEventTimestamp_unwrappedCtorArmsGone() {
        assertEquals(0, count(fn(cdm6FnOutput, MAP_EVENT_TIMESTAMP),
                        "return EventTimestamp.builder()"),
                "The unwrapped ctor arm returns are gone (count 0 in golden)");
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
        return fn(drrFnOutput, path);
    }

    private static String fn(Map<String, String> output, String path) {
        assertNotNull(output, "function generation did not run — corpus unavailable?");
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
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath).replace("\r", "");
        assertEquals(golden, gen.replace("\r", ""), "byte mismatch vs golden: " + path);
    }
}
