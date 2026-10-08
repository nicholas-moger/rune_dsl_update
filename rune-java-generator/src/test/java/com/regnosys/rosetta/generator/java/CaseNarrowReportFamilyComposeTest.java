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
 * PR #368 — the OtherPaymentRule report family + the case-narrowed disguised-nav
 * re-root + the single-fn-call-arg singletonList guard: 14 byte flips (1 cdm5
 * FUNCTION + 6 cdm6 FUNCTION + 1 drr FUNCTION + 6 drr POJO Rules) + 0 new / 0 away.
 *
 * <p><b>F-D singleFnCallArgIntoMulti</b> (ReferenceHandler + FunctionExpressionRenderer):
 * a DIRECT single-output fn-call arg into a MULTI callee param hoists
 * {@code final TradeState tradeState = tradeStateForEvent.evaluate(reportableEvent);}
 * and null-guards the singletonList coercion (the #212 law at the evaluate-arg seat;
 * renderAliasSinkHoistsOrNull gains the ONLY_ELEMENT-top admission behind the
 * bodyCarriesSingleFnCallArgIntoMulti pre-gate): IsActionTypeMODI drr.
 *
 * <p><b>F-A namedExtractBaseCallRebind + ctorFieldNestedThenAdmit + ctorCondFieldAdmit
 * + iteArmMetaCollapseDeref</b> (ReferenceHandler + CollectionHandler +
 * ControlFlowHandler): the named-extract BASE-CALL implicit arg walks out to the
 * wrapper's binding (the #367-F4 sibling — {@code thenArg.get()} not the param name);
 * the ctor-field nested-then / flat-conditional admissions open the outer in-lambda
 * lowering (the #352-i1 together-restructure law one seat deeper); an ite ARM whose
 * Mapper item is a META wrapper hoists the wrapper local IN-ARM with the guarded
 * getValue (metaWrapperOf — the concrete wrapper, not the generic toMetaJavaType):
 * OtherPaymentRule esma + fca + hkma + iosco cde v1/v2/v3, riders
 * Create_ShapingInstruction cdm5 + cdm6 and MapFxOptionToSettlementTerms cdm6.
 *
 * <p><b>F-B caseNarrowedDisguisedNav</b> (ReferenceHandler + NavigationHandler +
 * FunctionExpressionRenderer): a disguised 1-name/2-name nav inside a TYPE-guard
 * switch CASE re-roots on the #221-bound cast var through the NARROWED case type —
 * the shared caseNarrowedImplicitType walk feeds the chain/bare synthesis, the hop-1
 * {@code _}-pre-escaped receiver-type lambda naming, the chainProvesMulti case arm,
 * the ADD {@code .getMulti()}/SET {@code .get()} consumption, and the meta-wrapper
 * ADD empty element; the RDataType-bound bare-enum-arg mis-bind joins the #143 RBody
 * class: MapCap/MapFloorRateScheduleToPriceWithLocation + MapAsset +
 * GetFpmlPayerReceiver cdm6.
 *
 * <p>Whole-file byte comparisons run through the REAL D11 generation paths and revert
 * RED without the facets. The negative witnesses are load-bearing per the
 * witness-uniqueness law (OCCURRENCE counts, never line counts): every token below was
 * occurrence-counted in its PRE gen (f-probe-367post — counts stated per witness) and
 * 0 in its golden — the flips REMOVE them.
 */
class CaseNarrowReportFamilyComposeTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    /** The 1 cdm5 FUNCTION flip carrier (the F-A ctorCondFieldAdmit rider). */
    private static final String[] CDM5_FUNCTIONS = {
            "cdm/event/common/functions/Create_ShapingInstruction.java",
    };

    /** The 6 cdm6 FUNCTION flip carriers (F-A riders + F-B). */
    private static final String[] CDM6_FUNCTIONS = {
            "cdm/event/common/functions/Create_ShapingInstruction.java",
            "cdm/ingest/fpml/confirmation/settlement/functions/MapFxOptionToSettlementTerms.java",
            "cdm/ingest/fpml/confirmation/pricequantity/functions/MapCapRateScheduleToPriceWithLocation.java",
            "cdm/ingest/fpml/confirmation/pricequantity/functions/MapFloorRateScheduleToPriceWithLocation.java",
            "cdm/ingest/fpml/confirmation/pricequantity/functions/MapAsset.java",
            "cdm/ingest/fpml/confirmation/product/commodityswap/functions/GetFpmlPayerReceiver.java",
    };

    /** The 1 drr FUNCTION flip carrier (F-D). */
    private static final String[] DRR_FUNCTIONS = {
            "drr/regulation/common/functions/IsActionTypeMODI.java",
    };

    /** The 6 drr POJO Rule flip carriers (F-A). */
    private static final String[] DRR_POJO_RULES = {
            "drr/regulation/esma/emir/refit/trade/reports/OtherPaymentRule.java",
            "drr/regulation/fca/ukemir/refit/trade/reports/OtherPaymentRule.java",
            "drr/regulation/hkma/rewrite/trade/reports/OtherPaymentRule.java",
            "drr/standards/iosco/cde/version1/payment/reports/OtherPaymentRule.java",
            "drr/standards/iosco/cde/version2/payment/reports/OtherPaymentRule.java",
            "drr/standards/iosco/cde/version3/payment/reports/OtherPaymentRule.java",
    };

    private static Map<String, String> cdm5FnOutput;
    private static Map<String, String> cdm6FnOutput;
    private static Map<String, String> drrFnOutput;
    private static Map<String, String> drrCellOutput;

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cdm5CellAvailable()) {
            cdm5FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
        }
        if (cdm6CellAvailable()) {
            cdm6FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
        if (drrCellAvailable()) {
            drrFnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
            drrCellOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
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

    // -------------------------------------------- cdm5 FUNCTION byte lock (1)

    @Test
    @EnabledIf("cdm5CellAvailable")
    void cdm5Functions_byteMatchGolden() throws IOException {
        for (String path : CDM5_FUNCTIONS) {
            assertBytes(path, fn(cdm5FnOutput, path), CDM5_GOLDEN_DIR);
        }
    }

    // -------------------------------------------- cdm6 FUNCTION byte locks (6)

    @Test
    @EnabledIf("cdm6CellAvailable")
    void cdm6Functions_byteMatchGolden() throws IOException {
        for (String path : CDM6_FUNCTIONS) {
            assertBytes(path, fn(cdm6FnOutput, path), CDM6_GOLDEN_DIR);
        }
    }

    // -------------------------------------------- drr FUNCTION byte lock (1)

    @Test
    @EnabledIf("drrCellAvailable")
    void drrFunctions_byteMatchGolden() throws IOException {
        for (String path : DRR_FUNCTIONS) {
            assertBytes(path, fn(drrFnOutput, path), DRR_GOLDEN_DIR);
        }
    }

    // -------------------------------------------- drr POJO Rule byte locks (6)

    @Test
    @EnabledIf("drrCellAvailable")
    void drrPojoRules_byteMatchGolden() throws IOException {
        for (String path : DRR_POJO_RULES) {
            assertBytes(path, cell(path), DRR_GOLDEN_DIR);
        }
    }

    // ------------------------------------------------------- negative witnesses

    /**
     * The F-D witness (IsActionTypeMODI drr): the un-coerced nested scalar call
     * {@code filterOpenTradeStates.evaluate(tradeStateForEvent.evaluate(reportableEvent))}
     * counted EXACTLY 1 occurrence in the PRE gen (f-probe-367post) and 0 in the
     * golden — the hoist + null-guarded emptyList/singletonList ternary replace it.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void isActionTypeMODI_nestedScalarCallGone() {
        assertEquals(0, count(fn(drrFnOutput, DRR_FUNCTIONS[0]),
                        "filterOpenTradeStates.evaluate(tradeStateForEvent.evaluate(reportableEvent))"),
                "The un-coerced nested scalar arg is gone (count 0 in golden)");
    }

    /**
     * The F-A1 witness (fca OtherPaymentRule drr): the named-param deref
     * {@code tradeStateForEvent.evaluate(transactionReportInstruction.get())} counted
     * EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the base-call
     * implicit arg walks out to the wrapper's binding ({@code thenArg.get()}).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void fcaOtherPayment_namedParamDerefGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[1]),
                        "tradeStateForEvent.evaluate(transactionReportInstruction.get())"),
                "The named-param deref is gone (count 0 in golden)");
    }

    /**
     * The F-A2 witness (esma OtherPaymentRule drr): the un-lowered runtime then-chain
     * head {@code tradeStateForEvent.evaluate(transactionReportInstruction.get()).then(}
     * counted EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the
     * ctor-field nested-then admission lowers the chain into the block lambda.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void esmaOtherPayment_runtimeThenChainGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[0]),
                        "tradeStateForEvent.evaluate(transactionReportInstruction.get()).then("),
                "The un-lowered runtime then-chain is gone (count 0 in golden)");
    }

    /**
     * The F-A3 witness (iosco v1 OtherPaymentRule drr): the inline-ternary empty else
     * {@code : MapperC.of().get())} counted EXACTLY 2 occurrences in the PRE gen
     * (the payerFormat + receiverFormat elseless fields) and 0 in the golden — the
     * per-setter elseless {@code ifThenElseResultN = null;} hoists replace them.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void ioscoOtherPayment_inlineTernaryEmptyElseGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[3]), ": MapperC.of().get())"),
                "The inline-ternary empty else is gone (count 0 in golden)");
    }

    /**
     * The F-B witnesses (MapCapRateScheduleToPriceWithLocation cdm6): the unresolved
     * bare-symbol nav {@code MapperS.of(floatingRateModel).map("getCapRateSchedule",
     * floatingRateModel -> floatingRateModel.getCapRateSchedule())} (EXACTLY 1 in the
     * PRE gen) and the bare-item empty element {@code Collections.<PriceSchedule>
     * emptyList()} (EXACTLY 2 — the null + default arms) each counted 0 in the golden
     * — the case-narrowed re-root and the meta-wrapper element replace them.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapCapRateSchedule_bareSymbolNavAndBareEmptyGone() {
        String gen = fn(cdm6FnOutput, CDM6_FUNCTIONS[2]);
        assertEquals(0, count(gen,
                        "MapperS.of(floatingRateModel).map(\"getCapRateSchedule\", "
                                + "floatingRateModel -> floatingRateModel.getCapRateSchedule())"),
                "The unresolved bare-symbol nav is gone (count 0 in golden)");
        assertEquals(0, count(gen, "Collections.<PriceSchedule>emptyList()"),
                "The bare-item empty element is gone (count 0 in golden)");
    }

    /**
     * The F-B bare-sibling witness (GetFpmlPayerReceiver cdm6): the broken
     * type-literal ternary head {@code Objects.equals(fpml.FloatingLeg,} counted
     * EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the bare-nav
     * re-root lets the instanceof ladder admit (the isName decline no longer fires).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void getFpmlPayerReceiver_typeLiteralTernaryGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[5]),
                        "Objects.equals(fpml.FloatingLeg,"),
                "The type-literal switch ternary is gone (count 0 in golden)");
    }

    /**
     * The F-B enum-rider witness (MapAsset cdm6): the type-bound bare enum arg
     * {@code , ListedDerivative))} counted EXACTLY 1 occurrence in the PRE gen and 0
     * in the golden — the RDataType-bound mis-bind admission qualifies it
     * {@code InstrumentTypeEnum.LISTED_DERIVATIVE}.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapAsset_typeBoundBareEnumArgGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[4]), ", ListedDerivative))"),
                "The type-bound bare enum arg is gone (count 0 in golden)");
    }

    /**
     * The F-A rider witnesses: Create_ShapingInstruction cdm6's un-lowered
     * mapItemToList runtime then {@code .mapItemToList(item -> item.<PriceQuantity>
     * mapC("getPriceQuantity", tradeLot -> tradeLot.getPriceQuantity())).then(item ->
     * item} and MapFxOptionToSettlementTerms cdm6's un-lowered getFixing runtime then
     * {@code .<FxFixing>mapC("getFixing", fxCashSettlement -> fxCashSettlement
     * .getFixing()).then(item -> item} each counted EXACTLY 1 occurrence in the PRE
     * gen and 0 in the golden — the ctor-field admissions lower both chains.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void ctorAdmissionRiders_runtimeThenChainsGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[0]),
                        ".mapItemToList(item -> item.<PriceQuantity>mapC(\"getPriceQuantity\", "
                                + "tradeLot -> tradeLot.getPriceQuantity())).then(item -> item"),
                "Create_ShapingInstruction's un-lowered runtime then is gone (count 0 in golden)");
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[1]),
                        ".<FxFixing>mapC(\"getFixing\", fxCashSettlement -> "
                                + "fxCashSettlement.getFixing()).then(item -> item"),
                "MapFxOption's un-lowered runtime then is gone (count 0 in golden)");
    }

    // ------------------------------------------------------------------ helpers

    private static String fn(Map<String, String> output, String path) {
        assertNotNull(output, "Function generation did not run — corpus unavailable?");
        String gen = output.get(path);
        assertNotNull(gen, () -> "Function output missing for " + path);
        return gen;
    }

    private static String cell(String path) {
        assertNotNull(drrCellOutput, "Cell generation did not run — corpus unavailable?");
        String gen = drrCellOutput.get(path);
        assertNotNull(gen, () -> "Cell output missing for " + path);
        return gen;
    }

    private static void assertBytes(String path, String gen, Path goldenDir) throws IOException {
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), () -> "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath).replace("\r\n", "\n");
        assertEquals(golden, gen.replace("\r\n", "\n"),
                () -> "Generated bytes must match golden for " + path);
    }

    /** OCCURRENCE count (the witness-uniqueness law — never line counts). */
    private static int count(String text, String token) {
        int n = 0;
        int i = text.indexOf(token);
        while (i >= 0) {
            n++;
            i = text.indexOf(token, i + 1);
        }
        return n;
    }
}
