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
 * PR #363 — the n2 double-underscore name burn + the mapBasket extract map method +
 * the getOrDefault-arg meta deref + the in-lambda filter-arg deref + the with-meta
 * argument hoist: 10 byte flips (3 cdm5 FUNCTION + 4 cdm6 FUNCTION + 1 drr FUNCTION
 * + 2 drr POJO Rules) + 0 new / 0 away (regscan363; riders Create_PartyChange
 * cdm5+cdm6 byteChanged-FLAT content-TOWARD, cp1-verified).
 *
 * <p><b>n2DoubleUnderscore</b> (JavaStatementScope + FunctionExpressionRenderer): the
 * raw-input null-guard NAME BURN — upstream {@code JavaVariable.declareAsVariable}'s
 * synonym route ({@code convertNullSafe} + the as-key single branch) re-registers the
 * variable's identifier into the statement scope, whose {@code computeActualNames}
 * escapes it against the method params and BURNS the escaped name
 * ({@code _observation}) into the scope's taken set without rendering it, so
 * same-name lambda params escalate {@code _x} → {@code __x}. The fork mirrors it with
 * {@code registerNameBurn}: a burnOnly deferred entry whose sentinel rides the op text
 * (keptness = the unified-replay survival filter) and substitutes to the empty string.
 * Seats: the #217 + #347 inline singleton arms + the as-key bare-identifier branch
 * (Resolve{InterestRate,Performance}Reset cdm5/cdm6 ×4 + InterestCashSettlementAmount
 * cdm5; the corpus {@code __} census is CLOSED — 10 tokens in 8 files).
 *
 * <p><b>mapBasketExtractMapMethod</b> (CollectionHandler): {@code isBodyMulti} gains
 * the RDefaultExpr-gated {@code chainProvesMulti} consult — the #362 aliasDefaultJoin
 * both-strict arm now answers the extract map-method seat too (the #325 consult was
 * RFeatureCall-gated, so a {@code default} body never descended — the INERT-arm decode
 * from the #362 resume). Golden renders {@code mapSingleToList} against the alias
 * MapperC signature (MapBasketReferenceInformation cdm6).
 *
 * <p><b>getOrDefaultArgMetaDeref</b> (SetOperationHandler): the render-truth channel
 * the #362 cp6b REFUTED-class javadoc banked — the getOrDefault-argument gate reads
 * the right's chain root through the scope thenArg BINDING ({@code bindThenArg},
 * wrapper-typed by the #362 alias-signature decl re-type), hoists the collapsed item
 * (#237-convention statement hoist) and derefs
 * {@code getOrDefault((x == null ? null : x.getValue()))} SELF-UNWRAPPING
 * (GetInternalId drr; Contract_Price_Monetary iosco — the cp6b AWAY carrier —
 * verified untouched).
 *
 * <p><b>inLambdaFilterArgDeref</b> (ReferenceHandler): {@code metaDerefHoistRoute}
 * gains the RFilterExpr arm (a call NESTED in a filter predicate routes
 * LAMBDA_CHANNEL) and {@code renderImplicitFunctionInvocation}'s #144/#346 meta-deref
 * block admits the filter-predicate entrant, operand-class BY CONSTRUCTION (sentinel
 * decl + pending-lambda registration — never a JavaBlockBuilder, the #340 degradation
 * law). The piped {@code MapperC<FieldWithMetaString>} item hoists in-lambda + derefs
 * the evaluate arg; the {@code _fieldWithMetaString} escape against the method-level
 * #362 output-deref hoist falls out of the unified naming replay (jfsa
 * OriginalSwapUTIRule + OriginalSwapUTIProprietaryRule).
 *
 * <p><b>withMetaArgumentHoist</b> (ConstructionHandler): the #359 nested
 * double-with-meta relax the #358 gotcha banked — {@code tryPojoMetaWithMeta}'s
 * OUTPUT-metaKind gate admits a NESTED seat (the with-meta is the ARGUMENT of an
 * enclosing RWithMetaExpr), POJO-ness proven by the ARGUMENT ctor; the inner
 * {@code key:} renders the withMetaArgument hoist + the
 * {@code getOrCreateMeta().setExternalKey()} statement, the outer {@code reference:}
 * arm consumes the bare local through its needsBuilder guard; the decl type routes
 * through the #245 import-collision sentinel (the cdm/fpml CreditEvents collision —
 * golden's FQN builder type) (MapCreditEventsReferenceWithReference cdm6).
 *
 * <p>Whole-file byte comparisons run through the REAL D11 generation paths and revert
 * RED without the facets. The negative witnesses are load-bearing per the
 * witness-uniqueness law (OCCURRENCE counts, never line counts): every token below was
 * occurrence-counted in its PRE gen (f-probe-362post; counts noted per witness) and 0
 * in its golden — the flips REMOVE them.
 */
class N2BurnFilterDerefComposeTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    /** The 3 cdm5 FUNCTION flip carriers (n2DoubleUnderscore). */
    private static final String[] CDM5_FUNCTIONS = {
            "cdm/event/common/functions/InterestCashSettlementAmount.java",
            "cdm/event/common/functions/ResolveInterestRateReset.java",
            "cdm/event/common/functions/ResolvePerformanceReset.java",
    };

    /** The 4 cdm6 FUNCTION flip carriers. */
    private static final String[] CDM6_FUNCTIONS = {
            "cdm/event/common/functions/ResolveInterestRateReset.java",
            "cdm/event/common/functions/ResolvePerformanceReset.java",
            "cdm/ingest/fpml/confirmation/product/creditdefaultswap/functions/MapBasketReferenceInformation.java",
            "cdm/ingest/fpml/confirmation/product/creditdefaultswapoption/functions/MapCreditEventsReferenceWithReference.java",
    };

    /** The 1 drr FUNCTION flip carrier (getOrDefaultArgMetaDeref). */
    private static final String[] DRR_FUNCTIONS = {
            "drr/regulation/common/trade/party/functions/GetInternalId.java",
    };

    /** The 2 drr POJO Rule flip carriers (inLambdaFilterArgDeref). */
    private static final String[] DRR_POJO_RULES = {
            "drr/regulation/jfsa/rewrite/trade/reports/OriginalSwapUTIProprietaryRule.java",
            "drr/regulation/jfsa/rewrite/trade/reports/OriginalSwapUTIRule.java",
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
            var drrCell = new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT);
            drrFnOutput = generateFunctions(drrCell);
            drrCellOutput = generateCell(drrCell);
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

    // -------------------------------------------- cdm5 FUNCTION byte locks (3)

    @Test
    @EnabledIf("cdm5CellAvailable")
    void cdm5Functions_byteMatchGolden() throws IOException {
        for (String path : CDM5_FUNCTIONS) {
            assertBytes(path, fn(cdm5FnOutput, path), CDM5_GOLDEN_DIR);
        }
    }

    // -------------------------------------------- cdm6 FUNCTION byte locks (4)

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

    // -------------------------------------------- drr POJO Rule byte locks (2)

    @Test
    @EnabledIf("drrCellAvailable")
    void drrPojoRules_byteMatchGolden() throws IOException {
        for (String path : DRR_POJO_RULES) {
            assertBytes(path, cell(path), DRR_GOLDEN_DIR);
        }
    }

    // ------------------------------------------------------- negative witnesses

    /**
     * The n2DoubleUnderscore witness (ResolveInterestRateReset cdm5): the
     * single-escaped lambda param {@code "getObservedValue", _observation ->} counted
     * EXACTLY 1 occurrence in the PRE gen (f-probe-362post) and 0 in the golden — the
     * addObservations null-guard burn escalates it to {@code __observation}.
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void resolveInterestRateReset_cdm5_singleEscapeGone() {
        assertEquals(0, count(fn(cdm5FnOutput, CDM5_FUNCTIONS[1]),
                        "\"getObservedValue\", _observation ->"),
                "The single-underscore escape is gone (count 0 in golden)");
    }

    /**
     * The n2DoubleUnderscore witness (ResolvePerformanceReset cdm5): the same
     * single-escaped param token — PRE count 1, golden 0.
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void resolvePerformanceReset_cdm5_singleEscapeGone() {
        assertEquals(0, count(fn(cdm5FnOutput, CDM5_FUNCTIONS[2]),
                        "\"getObservedValue\", _observation ->"),
                "The single-underscore escape is gone (count 0 in golden)");
    }

    /**
     * The n2DoubleUnderscore witnesses (ResolveInterestRateReset +
     * ResolvePerformanceReset cdm6): the same single-escaped param token — PRE count
     * 1 in each, golden 0.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void resolveResets_cdm6_singleEscapeGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[0]),
                        "\"getObservedValue\", _observation ->"),
                "The single-underscore escape is gone (count 0 in golden)");
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[1]),
                        "\"getObservedValue\", _observation ->"),
                "The single-underscore escape is gone (count 0 in golden)");
    }

    /**
     * The as-key burn witness (InterestCashSettlementAmount cdm5): the assignOutput
     * currency chain's single-escaped head {@code .setCurrency(MapperS.of(
     * interestRatePayout).<ResolvablePriceQuantity>map("getPriceQuantity",
     * _interestRatePayout} counted EXACTLY 1 occurrence in the PRE gen and 0 in the
     * golden — the setCurrency anchor pins the assignOutput seat (the {@code
     * performance} method's OWN single escape is golden and stays).
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void interestCashSettlement_cdm5_assignOutputSingleEscapeGone() {
        assertEquals(0, count(fn(cdm5FnOutput, CDM5_FUNCTIONS[0]),
                        ".setCurrency(MapperS.of(interestRatePayout)"
                        + ".<ResolvablePriceQuantity>map(\"getPriceQuantity\", _interestRatePayout"),
                "The assignOutput single-underscore escape is gone (count 0 in golden)");
    }

    /**
     * The mapBasketExtractMapMethod witness (MapBasketReferenceInformation cdm6): the
     * single-form method {@code .mapSingleToItem(item -> item.<BasketIdentifierModelSequence>}
     * counted EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the default
     * extract body proves multi and selects mapSingleToList.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapBasket_cdm6_singleMapMethodGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[2]),
                        ".mapSingleToItem(item -> item.<BasketIdentifierModelSequence>"),
                "The mapSingleToItem selection is gone (count 0 in golden)");
    }

    /**
     * The withMetaArgumentHoist witness (MapCreditEventsReferenceWithReference cdm6):
     * the legacy stub's {@code .build().toBuilder().setMeta(MetaFields.builder().setKey(}
     * counted EXACTLY 2 occurrences in the PRE gen (the null-guard ternary duplicates
     * the ctor) and 0 in the golden — the nested POJO-meta relax replaces both with
     * the withMetaArgument hoist + the getOrCreateMeta().setExternalKey() statement.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapCreditEvents_cdm6_setMetaStubGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[3]),
                        ".build().toBuilder().setMeta(MetaFields.builder().setKey("),
                "The legacy setMeta stub is gone (count 0 in golden; PRE counted 2)");
    }

    /**
     * The getOrDefaultArgMetaDeref witness (GetInternalId drr): the raw Mapper-arg
     * form {@code .getOrDefault(distinct(thenArg)} counted EXACTLY 1 occurrence in
     * the PRE gen and 0 in the golden — the then-bound meta-collapse deref hoists the
     * collapsed item and passes the null-guarded value.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getInternalId_rawMapperArgGone() {
        assertEquals(0, count(fn(drrFnOutput, DRR_FUNCTIONS[0]),
                        ".getOrDefault(distinct(thenArg)"),
                "The raw Mapper getOrDefault arg is gone (count 0 in golden)");
    }

    /**
     * The inLambdaFilterArgDeref witness (jfsa OriginalSwapUTIRule): the raw wrapper
     * arg {@code evaluate(item.get())), MapperS.of(true)} counted EXACTLY 1 occurrence
     * in the PRE gen and 0 in the golden — the filter-predicate entrant hoists
     * {@code _fieldWithMetaString} in-lambda and derefs the arg.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void originalSwapUti_jfsa_rawWrapperArgGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[1]),
                        "evaluate(item.get())), MapperS.of(true)"),
                "The raw wrapper evaluate-arg is gone (count 0 in golden)");
    }

    /**
     * The inLambdaFilterArgDeref witness (jfsa OriginalSwapUTIProprietaryRule): the
     * {@code MapperS.of(false)} sibling — PRE count 1, golden 0.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void originalSwapUtiProprietary_jfsa_rawWrapperArgGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[0]),
                        "evaluate(item.get())), MapperS.of(false)"),
                "The raw wrapper evaluate-arg is gone (count 0 in golden)");
    }

    // ----------------------------------------------------------------- helpers

    private static String cell(String path) {
        assertNotNull(drrCellOutput, "drr cell generation did not run — corpus unavailable?");
        String generated = drrCellOutput.get(path);
        assertNotNull(generated, "Class not generated: " + path);
        return generated;
    }

    private static String fn(Map<String, String> output, String path) {
        assertNotNull(output, "function generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "Class not generated: " + path);
        return generated;
    }

    private static void assertBytes(String path, String generated, Path goldenDir)
            throws IOException {
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " (PR #363).");
    }

    private static int count(String text, String token) {
        int n = 0;
        int i = text.indexOf(token);
        while (i >= 0) {
            n++;
            i = text.indexOf(token, i + token.length());
        }
        return n;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
