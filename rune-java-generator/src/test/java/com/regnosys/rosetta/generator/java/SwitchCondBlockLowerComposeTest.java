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
 * PR #365 — the switch/conditional block-lowering class: 15 byte flips (1 cdm5 FUNCTION +
 * 9 cdm6 FUNCTION + 5 drr POJO Rules) + 0 new / 0 away (regscan365: 15/0/0; riders
 * content-TOWARD: MapRateOptionWithLocation cdm6, CountryOfCounterparty2Rule asic,
 * EsmaUti/UTIFCAValue/HKMAUniqueTransactionIdentifier drr POJO,
 * GetBasketConstituentsProductIdentifier drr FN).
 *
 * <p><b>aliasSwitchValueLadder</b> (FunctionAliasHelper + FunctionExpressionRenderer +
 * FunctionGenerator): an alias body that is a CHOICE/TYPE-keyed switch with VALUE-class
 * case results (enum value refs / boolean literals under resolvable NAME guards over a
 * bare subject) renders the upstream {@code instanceof} RETURN ladder (null-guard +
 * per-case cast local + {@code MapperS.of(<value>)} returns + {@code MapperS.<T>ofNull()}
 * terminals; a value default renders {@code MapperS.of(<value>)}), and the alias
 * signature types from the SAME case-result join ({@code MapperS<FinancialUnitEnum>} /
 * {@code MapperS<Boolean>} — never the fn-OUTPUT fallback leak), so signature and ladder
 * agree by construction (GetMultiplerUnitTypeUnderlyingAsset + MapRateOptionWithAddress
 * cdm6).
 *
 * <p><b>condArmBasicSwitch</b> (FunctionExpressionRenderer): an else-arm BASIC-type
 * switch renders the #149 switchArgument ladder INSIDE the arm block; the rosetta
 * CAPITALIZED boolean literals lower to the Java literals ({@code MapperS.of(true)});
 * requalifyMisBoundEnumCase gains the UNRESOLVED bare-symbol arm — the #355 value-name
 * gate against the output enum ({@code action = ActionEnum.CORRECT;} — MapMessageAction
 * cdm6).
 *
 * <p><b>choiceSwitchLambdaBlock</b> (CollectionHandler): a MODEL-CHOICE switch EXTRACT
 * body with MULTI fn-call case results over a SINGLE receiver renders the
 * option-attribute-PRESENCE {@code mapSingleToList} block (item null-guard + option-nav
 * null-tests + the UNCONDITIONAL {@code MapperS<Option>} case locals +
 * {@code MapperC.<R>ofNull()} terminals — Create_CashflowFromPayout cdm6; the
 * extends-based instanceof route declines MapperC case bodies, so the two are disjoint).
 *
 * <p><b>filterPredicateCondBlock</b> (CollectionHandler): a conditional filter-predicate
 * body ({@code filter if <cond> then <comparison> else True}) lowers to the early-return
 * block with the per-arm Boolean collapse ({@code return <comparison>.get(); … return
 * true;} — FilterPrice cdm5 + cdm6).
 *
 * <p><b>ctorSetterNavCollapseType</b> (ControlFlowHandler): the #181 ctor-setter
 * ite-hoist typing ladder gains the ALIAS-CALL-rooted nav-collapse LEAF-attribute arm
 * (onlyElementLeafAttribute's gm-aware resolution — the same the rendered witness uses),
 * SEAT-gated to the ctor-FIELD value (RKeyValuePair parent — the Contract_Price_Monetary
 * chain-receiver over-fire catch): MapVarianceLegToVarianceReturnTerms +
 * MapFxPerformanceSwapToReturnTerms + MapFxVolatilitySwapReturnTerms cdm6.
 *
 * <p><b>multiDefaultTernary</b> (SetOperationHandler + ConstructionHandler): the
 * PARENTHESIZED MULTI-default ternary siblings — the ELIDED-left alias form
 * ({@code return (thenArg.getMulti().isEmpty() ? <chain> : thenArg);}) and the
 * ctor-FIELD ArrayList-distributed form ({@code (l.getMulti().isEmpty() ? new
 * ArrayList<>(r.getMulti()) : new ArrayList<>(l.getMulti()))}, spliced VERBATIM at the
 * ctor seat under textually identical gates — MapBreakdown cdm6).
 *
 * <p><b>ruleMultiCondBaseThenArg</b> (FunctionExpressionRenderer + ReferenceHandler):
 * the RULE-path MULTI conditional thenArg if/else block (the #350-F2 sibling; the #316
 * meta recovery keeps the WRAPPER element — golden derefs at the CONSUMER) + the
 * then-BOUND elementwise evaluate-arg deref (the #349-S2 unguarded law):
 * StrikePriceCurrencyRule + BasketConstituentIdentifierRule +
 * BasketConstituentIdentifierSourceRule drr POJO.
 *
 * <p><b>condArmDerefBlockRelocate</b> (CollectionHandler + FunctionExpressionRenderer):
 * the COND-position EvalArgMetaDerefHoist admit (rendered at block top before the
 * {@code if (}), the RULE-scoped materialized-empty else falling through to the
 * effective-else compiler (whose per-arm drains + elseEmpty terminal serve the
 * relocated-hoist class), the ofNull-witness decl element at BOTH thenArg decl seats
 * ({@code final MapperC<Party> thenArg2} — the block's own typed-empty return witnesses
 * the bare join), and the RDefaultExpr empty-arm recovery:
 * Beneficiary1/2IdentifierTypeIndicatorRule iosco drr POJO.
 *
 * <p>Whole-file byte comparisons run through the REAL D11 generation paths and revert
 * RED without the facets. The negative witnesses are load-bearing per the
 * witness-uniqueness law (OCCURRENCE counts, never line counts): every token below was
 * occurrence-counted in its PRE gen (f-probe-364post; counts noted per witness) and 0
 * in its golden — the flips REMOVE them.
 */
class SwitchCondBlockLowerComposeTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    /** The 1 cdm5 FUNCTION flip carrier (filterPredicateCondBlock). */
    private static final String[] CDM5_FUNCTIONS = {
            "cdm/observable/asset/functions/FilterPrice.java",
    };

    /** The 9 cdm6 FUNCTION flip carriers. */
    private static final String[] CDM6_FUNCTIONS = {
            "cdm/ingest/fpml/confirmation/common/functions/GetMultiplerUnitTypeUnderlyingAsset.java",
            "cdm/ingest/fpml/confirmation/common/functions/MapMessageAction.java",
            "cdm/ingest/fpml/confirmation/pricequantity/functions/MapRateOptionWithAddress.java",
            "cdm/ingest/fpml/confirmation/product/fxvarianceswap/functions/MapFxPerformanceSwapToReturnTerms.java",
            "cdm/ingest/fpml/confirmation/product/fxvolatilityswap/functions/MapFxVolatilitySwapReturnTerms.java",
            "cdm/ingest/fpml/confirmation/product/varianceswap/functions/MapVarianceLegToVarianceReturnTerms.java",
            "cdm/ingest/fpml/confirmation/workflowstep/functions/MapBreakdown.java",
            "cdm/observable/asset/functions/FilterPrice.java",
            "cdm/product/template/functions/Create_CashflowFromPayout.java",
    };

    /** The 5 drr POJO Rule flip carriers. */
    private static final String[] DRR_POJO_RULES = {
            "drr/standards/iosco/cde/version1/price/reports/StrikePriceCurrencyRule.java",
            "drr/regulation/common/trade/basket/reports/BasketConstituentIdentifierRule.java",
            "drr/regulation/common/trade/basket/reports/BasketConstituentIdentifierSourceRule.java",
            "drr/standards/iosco/cde/version1/party/reports/Beneficiary1IdentifierTypeIndicatorRule.java",
            "drr/standards/iosco/cde/version1/party/reports/Beneficiary2IdentifierTypeIndicatorRule.java",
    };

    private static Map<String, String> cdm5FnOutput;
    private static Map<String, String> cdm6FnOutput;
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

    // -------------------------------------------- cdm6 FUNCTION byte locks (9)

    @Test
    @EnabledIf("cdm6CellAvailable")
    void cdm6Functions_byteMatchGolden() throws IOException {
        for (String path : CDM6_FUNCTIONS) {
            assertBytes(path, fn(cdm6FnOutput, path), CDM6_GOLDEN_DIR);
        }
    }

    // -------------------------------------------- drr POJO Rule byte locks (5)

    @Test
    @EnabledIf("drrCellAvailable")
    void drrPojoRules_byteMatchGolden() throws IOException {
        for (String path : DRR_POJO_RULES) {
            assertBytes(path, cell(path), DRR_GOLDEN_DIR);
        }
    }

    // ------------------------------------------------------- negative witnesses

    /**
     * The aliasSwitchValueLadder witness (GetMultiplerUnitTypeUnderlyingAsset cdm6): the
     * broken type-vs-Mapper equality {@code Objects.equals(fpml.Equity,
     * MapperS.of(fpmlUnderlyingAsset))} counted EXACTLY 1 occurrence in the PRE gen
     * (f-probe-364post) and 0 in the golden — the ladder renders the null-guard +
     * {@code instanceof Equity} + the cast case local.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void getMultiplerUnitType_typeEqualsTernaryGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[0]),
                        "Objects.equals(fpml.Equity, MapperS.of(fpmlUnderlyingAsset))"),
                "The type-vs-Mapper equality ternary is gone (count 0 in golden)");
    }

    /**
     * The aliasSwitchValueLadder witness (MapRateOptionWithAddress cdm6): the same broken
     * form for the FloatingRateCalculation case — PRE count 1, golden 0; the signature
     * narrows {@code MapperS<? extends InterestRateIndex>} → {@code MapperS<Boolean>}.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapRateOptionWithAddress_typeEqualsTernaryGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[2]),
                        "Objects.equals(fpml.FloatingRateCalculation, MapperS.of(fpmlRate))"),
                "The type-vs-Mapper equality ternary is gone (count 0 in golden)");
    }

    /**
     * The condArmBasicSwitch witness (MapMessageAction cdm6): the raw capitalized-literal
     * string equality {@code Objects.equals("True", MapperS.of(isCorrection))} counted
     * EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the arm renders the
     * switchArgument ladder with {@code MapperS.of(true)} guards.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapMessageAction_rawLiteralEqualityGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[1]),
                        "Objects.equals(\"True\", MapperS.of(isCorrection))"),
                "The raw capitalized-literal equality is gone (count 0 in golden)");
    }

    /**
     * The choiceSwitchLambdaBlock witness (Create_CashflowFromPayout cdm6): the
     * single-mapping ternary head {@code .mapSingleToItem(item ->
     * Objects.equals(OptionPayout, item)} counted EXACTLY 1 occurrence in the PRE gen and
     * 0 in the golden — the switch renders the {@code mapSingleToList} option-presence
     * block.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void createCashflowFromPayout_choiceTernaryGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[8]),
                        ".mapSingleToItem(item -> Objects.equals(OptionPayout, item)"),
                "The choice-switch ternary mapSingleToItem is gone (count 0 in golden)");
    }

    /**
     * The filterPredicateCondBlock witnesses (FilterPrice cdm5 + cdm6): the ternary
     * else-tail {@code : MapperS.of(true));} counted EXACTLY 2 occurrences in each PRE
     * gen (the two filter predicates) and 0 in the goldens — each predicate lowers to
     * the early-return block ending {@code return true;}.
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void filterPrice_cdm5_predicateTernaryGone() {
        assertEquals(0, count(fn(cdm5FnOutput, CDM5_FUNCTIONS[0]), ": MapperS.of(true));"),
                "The filter-predicate ternary else-tail is gone (count 0 in golden)");
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void filterPrice_cdm6_predicateTernaryGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[7]), ": MapperS.of(true));"),
                "The filter-predicate ternary else-tail is gone (count 0 in golden)");
    }

    /**
     * The ctorSetterNavCollapseType witnesses: the inline setter-ternary heads counted
     * EXACTLY 1 (MapVarianceLeg: {@code .setCurrencyValue(exists(fpmlVariance(
     * fpmlVarianceLeg)}) / 2 (each MapFx file: {@code .setCurrencyValue(areEqual(
     * fpmlQuoteBasis(fpmlFxPerformanceSwap)}) occurrences in the PRE gens and 0 in the
     * goldens — the seats hoist {@code String ifThenElseResultN} if-blocks and splice
     * the bare locals.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapVarianceLeg_inlineSetterTernaryGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[5]),
                        ".setCurrencyValue(exists(fpmlVariance(fpmlVarianceLeg)"),
                "The inline setter ternary is gone (count 0 in golden)");
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapFxPair_inlineSetterTernaryGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[3]),
                        ".setCurrencyValue(areEqual(fpmlQuoteBasis(fpmlFxPerformanceSwap)"),
                "The inline setter ternary is gone in MapFxPerformanceSwap (0 in golden)");
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[4]),
                        ".setCurrencyValue(areEqual(fpmlQuoteBasis(fpmlFxPerformanceSwap)"),
                "The inline setter ternary is gone in MapFxVolatilitySwap (0 in golden)");
    }

    /**
     * The multiDefaultTernary witness (MapBreakdown cdm6): the alias-return
     * {@code return thenArg.getOrDefault(} counted EXACTLY 2 occurrences in the PRE gen
     * and 0 in the golden — a MapperC-into-getOrDefault never compiled; the returns
     * render the parenthesized {@code (thenArg.getMulti().isEmpty() ? … : thenArg)}
     * ternaries.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapBreakdown_getOrDefaultReturnsGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[6]),
                        "return thenArg.getOrDefault("),
                "The MapperC getOrDefault returns are gone (count 0 in golden)");
    }

    /**
     * The ruleMultiCondBaseThenArg witnesses: the mixed-type inline-ternary decl head
     * {@code final MapperC<PriceSchedule> thenArg0 = areEqual(} (StrikePriceCurrencyRule)
     * and the item-typed local {@code final ProductIdentifier ifThenElseResult;} (each
     * BasketConstituentIdentifier file) counted EXACTLY 1 occurrence in their PRE gens
     * and 0 in the goldens — the bases render the MAPPER-TYPED if/else thenArg blocks.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void strikePriceCurrency_inlineTernaryDeclGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[0]),
                        "final MapperC<PriceSchedule> thenArg0 = areEqual("),
                "The mixed-type inline-ternary decl is gone (count 0 in golden)");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void basketConstituentIdentifierPair_itemLocalGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[1]),
                        "final ProductIdentifier ifThenElseResult;"),
                "The item-typed ite local is gone in BasketConstituentIdentifier (0 in golden)");
        assertEquals(0, count(cell(DRR_POJO_RULES[2]),
                        "final ProductIdentifier ifThenElseResult;"),
                "The item-typed ite local is gone in BasketConstituentIdentifierSource (0 in golden)");
    }

    /**
     * The condArmDerefBlockRelocate witnesses (Beneficiary1/2 iosco): the wrapper-element
     * decl {@code final MapperC<ReferenceWithMetaParty> thenArg2} counted EXACTLY 1
     * occurrence in each PRE gen and 0 in the goldens — the block's own
     * {@code return MapperS.<Party>ofNull();} witnesses the bare join, so the decl types
     * {@code MapperC<Party>} and the downstream item derefs drop.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void beneficiaryPair_wrapperDeclGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[3]),
                        "final MapperC<ReferenceWithMetaParty> thenArg2"),
                "The wrapper-element thenArg2 decl is gone in Beneficiary1 (0 in golden)");
        assertEquals(0, count(cell(DRR_POJO_RULES[4]),
                        "final MapperC<ReferenceWithMetaParty> thenArg2"),
                "The wrapper-element thenArg2 decl is gone in Beneficiary2 (0 in golden)");
    }

    // ------------------------------------------------------------------ helpers

    private static String fn(Map<String, String> output, String path) {
        String gen = output.get(path);
        assertNotNull(gen, () -> "Function output missing for " + path);
        return gen;
    }

    private static String cell(String path) {
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
