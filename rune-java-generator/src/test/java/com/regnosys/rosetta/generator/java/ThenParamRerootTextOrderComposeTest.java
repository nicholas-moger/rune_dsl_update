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
 * PR #375 — the re-root/text-order sextet: 6 byte flips (2 drr FUNCTIONs + 4 drr
 * POJO Rules) + 0 new / 0 away (regscan375: 6/0/0; every byteChanged rider
 * content-verified TOWARD with exact golden-token matches).
 *
 * <p><b>thenParamImplicitReroot + thenParamCollisionEscape</b> (ReferenceHandler +
 * CollectionHandler.resolveParamName): an implicit whose nearest boundary is an
 * UNBOUND named (explicit-param) extract fn walks out ONE fn to the named-extract
 * WRAPPER's #350 binding wherever that binding is LIVE — upstream's named param
 * does not rebind the implicit item, so chain-base reads render the piped local
 * ({@code thenArg.<ReportableInformation>map(…)}) while explicit param ARG reads
 * keep the param; the param escapes {@code _<name>} exactly when it collides with
 * the piped element's own type simple name (the corpus-complete 7-file
 * Capitalized-param population law): DTCC_OptionTypeRule ({@code _Product}).
 *
 * <p><b>thenParamBareNavSynthesis + inLambdaPlainBaseThenHoist</b>
 * (ReferenceHandler.synthesizeImplicitItemBareNav + CollectionHandler
 * compileEffectiveElseConditionalBlock + NavigationHandler naming arm): a
 * symbol-EMPTY bare attribute inside a named then-step fn synthesizes on the
 * piped item (golden {@code thenArg.<WorkflowStep>map("getOriginatingWorkflowStep",
 * …)}); the effective-else block pushes the #356 restructure window around its
 * ARM compiles so a hoistable plain-base then-chain hoists the in-lambda
 * {@code final MapperS<FieldWithMetaString> _thenArg = …;} + the applied
 * continuation, and the MapperMaths join types {@code <String, String, String>}
 * with the guarded meta-operand deref: MessageID drr FN.
 *
 * <p><b>iteChainArmThenHoist</b> (FunctionExpressionRenderer
 * renderPathedConditionalSetOrNull + HandlerHelper.bareItemThenPipeMetaType +
 * NavigationHandler receiver re-stamp): a hoistable ctl-free VALUE then-chain ARM
 * of a pathed-conditional SET restructures in-arm (a full-sink child scope relays
 * the {@code final MapperC<FieldWithMetaProductIdentifier> thenArg = …;} decl onto
 * the arm-deref sink, relocated INSIDE the owning branch), the collapse-widened
 * pipe-meta channel re-stamps the consumer lambda's item so the guarded deref hop
 * fires, and the restructure-gated wrapper local + guarded ternary consume the
 * collapse: Create_AnnaDsbUpiRequestUnderlyingForRate drr FN.
 *
 * <p><b>ladderDerefTextOrder</b> (CollectionHandler): the join-bare ladder deref
 * RE-COMPILES with the leaf deref applied inline per arm, so the guarded coercion
 * group numbers in TEXT order (arm/cond/arm/cond — the #374 retro-3 law; the
 * discarded verdict pass self-cleans via the unified replay's sentinel-survival
 * filter): EquityNotionalQuantityRule + EquityTotalNotionalQuantityRule + csa
 * QuantityUnitOfMeasureLeg1Rule (the family bonus).
 *
 * <p>The negative witnesses are load-bearing per the witness-uniqueness law
 * (OCCURRENCE counts, never line counts): every token was PRE-counted against
 * f-probe-374post (the counts cited per witness) and 0 in its golden — the flips
 * REMOVE them, so each witness is RED on the pre-facet source.
 */
class ThenParamRerootTextOrderComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    /** The 2 drr FUNCTION flip carriers (B + A4). */
    private static final String[] DRR_FNS = {
            "drr/regulation/common/trade/link/functions/MessageID.java",
            "drr/enrichment/upi/functions/Create_AnnaDsbUpiRequestUnderlyingForRate.java",
    };

    /** The 4 drr POJO Rule flip carriers (B1 + C2 ×3). */
    private static final String[] DRR_RULES = {
            "drr/regulation/common/dtcc/reports/DTCC_OptionTypeRule.java",
            "drr/regulation/common/trade/quantity/reports/EquityNotionalQuantityRule.java",
            "drr/standards/iosco/cde/base/quantity/reports/EquityTotalNotionalQuantityRule.java",
            "drr/regulation/csa/rewrite/trade/reports/QuantityUnitOfMeasureLeg1Rule.java",
    };

    private static Map<String, String> drrFnOutput;
    private static Map<String, String> drrRuleOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            D11CorpusRegressionTest.CellSpec drr =
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT);
            drrFnOutput = generateFunctions(drr);
            drrRuleOutput = generateRuleKinds(drr);
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

    /** The REAL D11 rule-kind generation path (the drr POJO Rule carriers). */
    private static Map<String, String> generateRuleKinds(D11CorpusRegressionTest.CellSpec cell)
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

    // ----------------------------------------------------------- byte locks (6)

    @Test
    @EnabledIf("cellsAvailable")
    void drrFunctionFlips_byteMatchGolden() throws IOException {
        for (String path : DRR_FNS) {
            assertBytes(path, gen(drrFnOutput, path));
        }
    }

    @Test
    @EnabledIf("cellsAvailable")
    void drrRuleFlips_byteMatchGolden() throws IOException {
        for (String path : DRR_RULES) {
            assertBytes(path, gen(drrRuleOutput, path));
        }
    }

    // ------------------------------------------------------- negative witnesses

    /**
     * The B1 escape witness (DTCC_OptionTypeRule): the UNESCAPED param decl
     * {@code .mapSingleToItem(Product -> {} counted EXACTLY 1 occurrence in the
     * PRE gen (f-probe-374post) and 0 in the golden — the param now escapes
     * {@code _Product} (it collides with the piped element's type simple name).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void dtccOptionType_unescapedProductParamGone() {
        assertEquals(0, count(gen(drrRuleOutput, DRR_RULES[0]),
                        ".mapSingleToItem(Product -> {"),
                "The unescaped Product param must be gone (PRE count 1)");
    }

    /**
     * The B1 arg-read witness (DTCC_OptionTypeRule): the unescaped explicit-param
     * evaluate arg {@code isFloor.evaluate(Product.get())} counted EXACTLY 1 in
     * the PRE gen and 0 in the golden ({@code _Product.get()} — the escape-aware
     * render-equals-name guard keeps the {@code .get()} collapse).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void dtccOptionType_unescapedParamArgReadGone() {
        assertEquals(0, count(gen(drrRuleOutput, DRR_RULES[0]),
                        "isFloor.evaluate(Product.get())"),
                "The unescaped Product.get() arg read must be gone (PRE count 1)");
    }

    /**
     * The B2 witness (MessageID): the bare-symbol echo
     * {@code MapperS.of(originatingWorkflowStep)} counted EXACTLY 1 in the PRE gen
     * and 0 in the golden — the symbol-EMPTY bare attribute now synthesizes on the
     * piped item ({@code thenArg.<WorkflowStep>map("getOriginatingWorkflowStep", …)}).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void messageId_bareSymbolEchoGone() {
        assertEquals(0, count(gen(drrFnOutput, DRR_FNS[0]),
                        "MapperS.of(originatingWorkflowStep)"),
                "The bare-symbol echo must be gone (PRE count 1)");
    }

    /**
     * The B4 witness (MessageID): the BigDecimal-defaulted MapperMaths typing
     * {@code MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(} counted EXACTLY
     * 1 in the PRE gen and 0 in the golden — the resolved join now types
     * {@code <String, String, String>} with the guarded meta-operand deref.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void messageId_bigDecimalMapperMathsGone() {
        assertEquals(0, count(gen(drrFnOutput, DRR_FNS[0]),
                        "MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add("),
                "The BigDecimal-defaulted add typing must be gone (PRE count 1)");
    }

    /**
     * The A4 witness (Create_AnnaDsbUpiRequestUnderlyingForRate): the deref-less
     * inline consumer tail {@code .mapSingleToItem(item ->
     * item.<FieldWithMetaString>map("getIdentifier", productIdentifier ->
     * productIdentifier.getIdentifier()))).get();} counted EXACTLY 1 in the PRE gen
     * and 0 in the golden — the chain restructures in-arm with the mid-chain
     * guarded deref + the wrapper-local ternary consumption.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void annaDsbUpiForRate_inlineConsumerTailGone() {
        assertEquals(0, count(gen(drrFnOutput, DRR_FNS[1]),
                        ".mapSingleToItem(item -> item.<FieldWithMetaString>map(\"getIdentifier\","
                                + " productIdentifier -> productIdentifier.getIdentifier()))).get();"),
                "The deref-less inline consumer tail must be gone (PRE count 1)");
    }

    /**
     * The C2 witness (EquityNotionalQuantityRule + EquityTotalNotionalQuantityRule):
     * the PASS-ORDER pairing of the OptionPayout arm chain with deref index 2
     * counted EXACTLY 1 per file in the PRE gen and 0 in each golden — the
     * text-order replay numbers the first arm's deref 0 (arm/cond/arm/cond).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void equityPair_passOrderDerefNumberingGone() {
        String token = "optionPayout.getPriceQuantity()).<ReferenceWithMetaNonNegativeQuantity"
                + "Schedule>map(\"getQuantitySchedule\", resolvablePriceQuantity -> "
                + "resolvablePriceQuantity.getQuantitySchedule()).<NonNegativeQuantitySchedule>"
                + "map(\"Type coercion\", referenceWithMetaNonNegativeQuantitySchedule2";
        assertEquals(0, count(gen(drrRuleOutput, DRR_RULES[1]), token),
                "EquityNotionalQuantity's pass-order index-2 pairing must be gone (PRE count 1)");
        assertEquals(0, count(gen(drrRuleOutput, DRR_RULES[2]), token),
                "EquityTotalNotionalQuantity's pass-order index-2 pairing must be gone (PRE count 1)");
    }

    /**
     * The C2 family-bonus witness (csa QuantityUnitOfMeasureLeg1Rule): the
     * pass-order pairing of the commodityLeg1 arm chain with deref index 2 counted
     * EXACTLY 1 in the PRE gen and 0 in the golden — the text-order replay pairs
     * it with index 0.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void csaQuantityUnitOfMeasureLeg1_passOrderDerefNumberingGone() {
        assertEquals(0, count(gen(drrRuleOutput, DRR_RULES[3]),
                        "commodityLeg1.evaluate(productForTrade.evaluate(item.get()))).<Resolvable"
                                + "PriceQuantity>map(\"getPriceQuantity\", commodityPayout -> "
                                + "commodityPayout.getPriceQuantity()).<ReferenceWithMetaNonNegative"
                                + "QuantitySchedule>map(\"getQuantitySchedule\", resolvablePrice"
                                + "Quantity -> resolvablePriceQuantity.getQuantitySchedule())."
                                + "<NonNegativeQuantitySchedule>map(\"Type coercion\", "
                                + "referenceWithMetaNonNegativeQuantitySchedule2"),
                "csa Leg1's pass-order index-2 pairing must be gone (PRE count 1)");
    }

    // ------------------------------------------------------------------ helpers

    private static String gen(Map<String, String> output, String path) {
        assertNotNull(output, "generation did not run — corpus unavailable?");
        String g = output.get(path);
        assertNotNull(g, "Class not generated: " + path);
        return g;
    }

    private static void assertBytes(String path, String gen) throws IOException {
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath).replace("\r", "");
        assertEquals(golden, gen.replace("\r", ""),
                path + " must byte-match the frozen 9.83.0 golden");
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
    }
}
