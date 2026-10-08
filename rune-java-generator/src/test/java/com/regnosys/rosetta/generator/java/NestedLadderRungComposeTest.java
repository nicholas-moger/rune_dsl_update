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
 * PR #356 — the nested-ladder-rung admit + the ctor-chain numeric hoist's lambda channel
 * + the ctor-pair to-string collapse re-wrap: 12 byte flips (11 drr FUNCTION + 1 drr
 * POJO Rule) + 1 TOWARD mover / 0 new / 0 AWAY (regscan356).
 *
 * <p><b>F-1 — {@code nestedLadderRung}</b> (CollectionHandler + JavaStatementScope +
 * FunctionExpressionRenderer): {@code compileLadderConditionalBlock}'s rung walk no
 * longer declines a rung whose THEN is itself a conditional — the walk builds a
 * {@code LadderLevel} tree (evidence-capped at 2 inner levels) and PASS 1/PASS 2
 * recurse per level, the inner rungs rendering as if/return ladders INSIDE the rung
 * braces at +1 tab with the pre-order boolHoist numbering shared across levels
 * (golden GetContractType's {@code boolean0}/{@code boolean1}; DTCC_TradeLegTypes'
 * {@code boolean0..12} over TWO nesting levels), the #331 per-rung condition hoists,
 * the #339 drainable-arm hoists, the #354 seat registration (nested pushes = union)
 * and the #355 fn-output enum arm qualification
 * ({@code MapperS.of(CommonContractType.FUTR)}) all applying at every level. Inner
 * levels drain arm-registered pending lambda hoists IN-RUNG (golden
 * ExtractCallAmount's in-rung {@code final ReferenceWithMeta…} pair); the top level
 * keeps the byte-frozen block-top tail drain. The ExtractCall/PutAmount class
 * additionally needed ARGUMENT-side render truth: a {@code <chain with ladder> then
 * <bareFn>} SET chain compiles its argument BEFORE any step body binds, so the three
 * argument-side producers (renderThenExtractSetImpl k==0, renderBareInvokableThenSet,
 * tryDeepThenHoist k==0) flag their chain top on the scope (node identity,
 * single-slot save/restore) while the base compiles, and
 * {@code isCleanLadderContext} ascends past the flagged chain. Declines: nested trees
 * with then/switch-carrying leaf arms (golden hoists those as thenArg locals — the
 * cp4/cp5 StandardizedScheduleDuration AWAY catches at #356; SSD itself FLIPPED at
 * PR #379-B once the receiver hoist landed and the multi-line-arm decline lifted with
 * it — the render-truth {@code .then(}-in-arm backstop is the surviving decline),
 * depth &gt; 2.
 *
 * <p><b>F-2 — {@code numericCoerceChainInLambdaHoist}</b> (ConstructionHandler): the
 * #332 ctor-setter chain hoist's LAMBDA_CHANNEL twin — a lambda-interior numeric
 * chain value hoists {@code final BigDecimal bigDecimal = <chain>.get();} through the
 * pending-lambda-hoist channel (a #346-safe raw-token statement, the #355 per-lambda
 * sub-group naming) and splices the null-guarded exact conversion
 * ({@code .setBsisPtSprd((bigDecimal == null ? null : bigDecimal.intValueExact()))}).
 * Declines: suppression (P352A), non-drainable lambdas, unbound runtime
 * {@code .then(} ancestors (the #350 handshake read).
 *
 * <p><b>F-3 — {@code toStringOverCollapse}</b> (ConversionHandler): a to-string whose
 * argument renders as a {@code .get()}-collapsed bare item re-wraps
 * {@code MapperS.of(<chain>.get()).map("to-string", …)} — evidence-scoped to
 * single-line collapses at the ctor-pair seat with a non-meta terminal (the P356C
 * vector; list-literal/equality-operand and meta-terminal collapses keep their
 * bytes).
 *
 * <p>Whole-file byte comparisons run through the REAL D11 generation paths and revert
 * RED without the facets. The negative witnesses are load-bearing per the
 * witness-uniqueness law (OCCURRENCE counts, never line counts): every token below
 * was occurrence-counted in its PRE gen (counts noted per witness) and 0 in its
 * golden — the flips REMOVE them.
 */
class NestedLadderRungComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String ASIC = "drr/projection/iso20022/asic/rewrite/trade/functions/";
    private static final String ESMA = "drr/projection/iso20022/esma/emir/refit/trade/functions/";
    private static final String FCA = "drr/projection/iso20022/fca/ukemir/refit/trade/functions/";
    private static final String HKMA_DTCC =
            "drr/projection/iso20022/hkma/rewrite/trade/dtcc/functions/";
    private static final String HKMA_TR =
            "drr/projection/iso20022/hkma/rewrite/trade/tr/functions/";
    private static final String JFSA = "drr/projection/iso20022/jfsa/rewrite/trade/functions/";

    /** The 3 F-1 drr FUNCTION flip carriers (the nested-rung admit proper). */
    private static final String GET_CONTRACT_TYPE =
            "drr/regulation/common/functions/GetContractType.java";
    private static final String EXTRACT_CALL_AMOUNT =
            "drr/standards/iosco/cde/version1/quantity/functions/ExtractCallAmount.java";
    private static final String EXTRACT_PUT_AMOUNT =
            "drr/standards/iosco/cde/version1/quantity/functions/ExtractPutAmount.java";

    /** The 6 F-2 GetPackg flip carriers (one producer, six regimes). */
    private static final String[] GET_PACKG_CARRIERS = {
            ASIC + "GetPackg.java",
            ESMA + "GetPackg.java",
            FCA + "GetPackg.java",
            HKMA_DTCC + "GetPackg.java",
            HKMA_TR + "GetPackg.java",
            JFSA + "GetPackg.java",
    };

    /** The 2 F-3 flip carriers (the #355-deferred to-string-over-collapse pair). */
    private static final String[] CREATE_CONTRACT_TYPE_15_PAIR = {
            HKMA_DTCC + "Create_ContractType15__1.java",
            HKMA_TR + "Create_ContractType15__1.java",
    };

    /** The F-1 drr POJO Rule flip carrier (the 2-level recursion proof). */
    private static final String DTCC_TRADE_LEG_TYPES =
            "drr/regulation/common/dtcc/reports/DTCC_TradeLegTypesRule.java";

    private static Map<String, String> drrFnOutput;
    private static Map<String, String> drrCellOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
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

    // ------------------------------------------------- F-1 FUNCTION byte locks (3)

    @Test
    @EnabledIf("drrCellAvailable")
    void f1NestedRungCarriers_byteMatchGolden() throws IOException {
        assertFnByteMatchesGolden(GET_CONTRACT_TYPE);
        assertFnByteMatchesGolden(EXTRACT_CALL_AMOUNT);
        assertFnByteMatchesGolden(EXTRACT_PUT_AMOUNT);
    }

    // ------------------------------------------------- F-2 FUNCTION byte locks (6)

    @Test
    @EnabledIf("drrCellAvailable")
    void f2GetPackgCarriers_byteMatchGolden() throws IOException {
        for (String path : GET_PACKG_CARRIERS) {
            assertFnByteMatchesGolden(path);
        }
    }

    // ------------------------------------------------- F-3 FUNCTION byte locks (2)

    @Test
    @EnabledIf("drrCellAvailable")
    void f3CreateContractType15Pair_byteMatchGolden() throws IOException {
        for (String path : CREATE_CONTRACT_TYPE_15_PAIR) {
            assertFnByteMatchesGolden(path);
        }
    }

    // ------------------------------------------------- F-1 POJO Rule byte lock (1)

    @Test
    @EnabledIf("drrCellAvailable")
    void dtccTradeLegTypes_byteMatchesGolden() throws IOException {
        assertCellByteMatchesGolden(DTCC_TRADE_LEG_TYPES);
    }

    // ------------------------------------------------- negative witnesses

    /**
     * The F-1 negative witness (GetContractType): the unqualified inline-ternary enum
     * arm {@code ? MapperS.of(FUTR)} counted EXACTLY 2 occurrences in the PRE gen
     * (the nested FUTR arm + the flat ETD-rung FUTR arm of the one giant ternary) and
     * 0 in the golden — the nested-rung block conversion + the #355 enum
     * qualification REMOVE both ({@code return MapperS.of(CommonContractType.FUTR);}).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getContractType_inlineTernaryEnumEchoGone() {
        assertEquals(0, count(fn(GET_CONTRACT_TYPE), "? MapperS.of(FUTR)"),
                "The unqualified ternary FUTR arm is gone (count 0 in golden)");
    }

    /**
     * The F-1 negative witness (ExtractCallAmount): the inline-ternary rule-invocation
     * arm {@code ? MapperS.of(commodityOptionNotionalRule.evaluate(} counted EXACTLY 1
     * occurrence in the PRE gen and 0 in the golden — the nested level renders it as
     * an in-rung {@code return}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void extractCallAmount_ternaryRuleArmGone() {
        assertEquals(0,
                count(fn(EXTRACT_CALL_AMOUNT), "? MapperS.of(commodityOptionNotionalRule.evaluate("),
                "The ternary rule-invocation arm is gone (count 0 in golden)");
    }

    /**
     * The F-2 negative witness (asic GetPackg): the bare BigDecimal chain setter arg
     * {@code .setBsisPtSprd(item.<PriceFormat>map(} counted EXACTLY 1 occurrence in
     * the PRE gen and 0 in the golden — the lambda-channel hoist replaces it with the
     * null-guarded {@code (bigDecimal == null ? null : bigDecimal.intValueExact())}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getPackgAsic_bareSetterChainGone() {
        assertEquals(0, count(fn(ASIC + "GetPackg.java"), ".setBsisPtSprd(item.<PriceFormat>map("),
                "The bare BigDecimal setter chain is gone (count 0 in golden)");
    }

    /**
     * The F-3 negative witness (hkma-dtcc Create_ContractType15__1): the collapsed
     * to-string receiver {@code .get().map("to-string", Object::toString)} counted
     * EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the re-wrap reads
     * {@code MapperS.of(<chain>.get()).map("to-string", …)}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void createContractType15_collapsedToStringGone() {
        assertEquals(0, count(fn(HKMA_DTCC + "Create_ContractType15__1.java"),
                        ".get().map(\"to-string\", Object::toString)"),
                "The collapsed to-string receiver is gone (count 0 in golden)");
    }

    /**
     * The F-1 recursion negative witness (DTCC_TradeLegTypesRule): the inline-ternary
     * string arm {@code ? MapperS.of("FO:AVG:BUL")} counted EXACTLY 1 occurrence in
     * the PRE gen and 0 in the golden — the TWO-level nested conversion renders it as
     * the inner-inner rung's {@code return MapperS.of("FO:AVG:BUL");}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccTradeLegTypes_inlineStringTernaryGone() {
        assertEquals(0, count(cell(DTCC_TRADE_LEG_TYPES), "? MapperS.of(\"FO:AVG:BUL\")"),
                "The inline string-ternary arm is gone (count 0 in golden)");
    }

    // ---------------------------------------------------------------- helpers

    private static int count(String haystack, String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
    }

    private static String fn(String path) {
        assertNotNull(drrFnOutput, "function generation did not run — corpus unavailable?");
        String g = drrFnOutput.get(path);
        assertNotNull(g, "Function not generated: " + path);
        return g;
    }

    private static String cell(String path) {
        assertNotNull(drrCellOutput, "cell generation did not run — corpus unavailable?");
        String g = drrCellOutput.get(path);
        assertNotNull(g, "Class not generated: " + path);
        return g;
    }

    private static void assertFnByteMatchesGolden(String path) throws IOException {
        assertBytes(path, fn(path));
    }

    private static void assertCellByteMatchesGolden(String path) throws IOException {
        assertBytes(path, cell(path));
    }

    private static void assertBytes(String path, String gen) throws IOException {
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath).replace("\r", "");
        assertEquals(golden, gen.replace("\r", ""),
                "Generated bytes must match the golden for " + path);
    }
}
