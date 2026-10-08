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
 * PR #373 — the cardinality-join/meta-param septet: 7 byte flips (2 drr FUNCTIONs +
 * 5 drr POJO Rules) + 0 new / 0 away (regscan373: 7/0/0; 11 common movers all
 * content-verified TOWARD — the F-alpha join riding the 15-carrier
 * mapSingleToList family, incl the jfsa/hkma UnderlyingIdentification pair
 * dl 143→82 / 181→122).
 *
 * <p><b>F-eps explicitParamPipedMeta</b> (NavigationHandler closureParamDirectNav):
 * a THEN-PIPED explicit extract param ({@code then extract quantitySchedule [ … ]})
 * owns an IMPLICIT argument, so the wrapper evidence recovers from the OWNING
 * then-chain (thenOwnerArgument → the #264/#270 walker — the #285
 * implicitItemArgMeta sibling for EXPLICIT params); coerceNavigationReceiver then
 * fires golden's numbered null-guarded deref hops
 * ({@code fieldWithMetaNonNegativeQuantitySchedule0..4}, method-wide):
 * NotionalQuantityLeg2Rule common.
 *
 * <p><b>F-eps ctorSetterElementwiseWrapperDeref</b> (ConstructionHandler
 * coerceCtorArg): a MULTI meta-FREE attribute whose Mapper-chain value carries
 * META-WRAPPER items derefs ELEMENTWISE before {@code .getMulti()} — the #349-S2
 * law at the CTOR-SETTER seat, MapperS → the guarded numbered deferred-coercion
 * form / MapperC → the bare form, the wrapper KIND read from the #339 BINDING's
 * decl via the render-root token guard. <b>thenChainTerminalCtorWrap</b>
 * (CollectionHandler renderLadderLevel): a then-chain arm whose TERMINAL body is
 * a ctor wraps {@code MapperS.of(…)} at the rung + terminal returns (the #334
 * law), render-truth-guarded ({@code endsWith(".build()")} — an undrained runtime
 * {@code .then(} render stays bare): DTCC_UnderlyingAssetNameRule csa.
 *
 * <p><b>F-alpha blockLambdaCardinalityJoin</b>: the conditional arm-JOIN goes to
 * upstream's OR — ANY multi arm makes the block MULTI (was ALL-arms-strict) and
 * nested-THEN conditionals recurse the same join; the MapperC seat coerces SINGLE
 * plain-chain arms {@code MapperC.of(…)} (multi arms stay bare;
 * compiled-type-first, the AST join as the null-type fallback) and types empties
 * {@code MapperC.<T>ofNull()}; the nested-then block admission widens to
 * MAPPER_C_EXPECTING: CallCurrencyRule + PutCurrencyRule iosco cde v1 (the
 * mapSingleToList hop + MapperC decl + the downstream mapItem cascade) +
 * GetUnderlierProductIdentifier / GetUnderlierProductIdentifierLeg1 drr FN (the
 * family bonus — the base ITE decl lifts MapperC with the
 * {@code MapperC.of(Collections.singletonList(…))} single-arm coercion).
 *
 * <p><b>F-d the PriceCurrency composition</b>: the SET-path k>0 intermediate
 * ITE's iteMulti gains the arm-join consult (the decl lifts
 * {@code final MapperC<FieldWithMetaString> ifThenElseResult;} with mapper-form
 * arms), the #144/#370 wrapper-element recovery extends to the MULTI mode (the
 * #294 in-arm-deref marker check keeps the #364/#366 bare-element carriers
 * frozen), a bare MULTI-output FUNCTION invocation as an extract RECEIVER
 * re-presents as {@code MapperC.<PriceSchedule>of(…)}, and the collapse-decl
 * wrapper recovery gains the ITE-hoist anchor (prevRef's element IS the
 * renderer's own just-emitted decl — render truth by construction):
 * PriceCurrencyRule iosco cde v1.
 *
 * <p>Whole-file byte comparisons run through the REAL D11 generation paths and
 * revert RED without the facets. The negative witnesses are load-bearing per the
 * witness-uniqueness law (OCCURRENCE counts, never line counts): every token
 * below was occurrence-counted in its PRE gen (f-probe-372post — counts stated
 * per witness) and 0 in its golden — the flips REMOVE them.
 */
class CardinalityJoinMetaParamComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    /** The 2 drr FUNCTION flip carriers (the F-alpha family bonus). */
    private static final String[] DRR_FNS = {
            "drr/regulation/common/functions/GetUnderlierProductIdentifier.java",
            "drr/regulation/common/functions/GetUnderlierProductIdentifierLeg1.java",
    };

    /** The 5 drr POJO Rule flip carriers (F-eps ×2 + F-alpha ×2 + F-d). */
    private static final String[] DRR_RULES = {
            "drr/regulation/common/trade/quantity/reports/NotionalQuantityLeg2Rule.java",
            "drr/regulation/csa/rewrite/dtcc/reports/DTCC_UnderlyingAssetNameRule.java",
            "drr/standards/iosco/cde/version1/quantity/reports/CallCurrencyRule.java",
            "drr/standards/iosco/cde/version1/quantity/reports/PutCurrencyRule.java",
            "drr/standards/iosco/cde/version1/price/reports/PriceCurrencyRule.java",
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

    // ----------------------------------------------------------- byte locks (7)

    @Test
    @EnabledIf("cellsAvailable")
    void drrFunctionFamily_byteMatchesGolden() throws IOException {
        for (String path : DRR_FNS) {
            assertBytes(path, gen(drrFnOutput, path));
        }
    }

    @Test
    @EnabledIf("cellsAvailable")
    void drrRuleFamily_byteMatchesGolden() throws IOException {
        for (String path : DRR_RULES) {
            assertBytes(path, gen(drrRuleOutput, path));
        }
    }

    // ------------------------------------------------------- negative witnesses

    /**
     * The F-eps param-deref witness (NotionalQuantityLeg2Rule): the DEREF-LESS
     * param-rooted nav {@code quantitySchedule.<BigDecimal>map("getValue"} counted
     * EXACTLY 2 occurrences in the PRE gen (f-probe-372post) and 0 in the golden —
     * every param-rooted nav now takes the numbered Type-coercion first hop.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void notionalQuantityLeg2_derefLessParamNavGone() {
        assertEquals(0, count(gen(drrRuleOutput, DRR_RULES[0]),
                        "quantitySchedule.<BigDecimal>map(\"getValue\""),
                "The deref-less param-rooted nav must be gone (PRE count 2)");
    }

    /**
     * The F-eps ctor-setter witnesses (DTCC_UnderlyingAssetNameRule): the
     * deref-less setter tail {@code creditIndexReferenceInformation.getIndexName())
     * .getMulti()} counted EXACTLY 2 occurrences in the PRE gen and 0 in the
     * golden (the elementwise deref hops land between); the UNWRAPPED then-chain-
     * terminal ctor return {@code return UnderlyingAssetNameReport.builder()}
     * counted EXACTLY 1 and 0 in the golden (the MapperS.of wrap lands).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void dtccUnderlyingAssetName_derefLessSetterAndBareCtorGone() {
        String gen = gen(drrRuleOutput, DRR_RULES[1]);
        assertEquals(0, count(gen,
                        "creditIndexReferenceInformation.getIndexName()).getMulti()"),
                "The deref-less setter tail must be gone (PRE count 2)");
        assertEquals(0, count(gen, "return UnderlyingAssetNameReport.builder()"),
                "The unwrapped then-chain-terminal ctor return must be gone (PRE count 1)");
    }

    /**
     * The F-alpha join witnesses (CallCurrencyRule + PutCurrencyRule): the
     * SINGLE-form block decl {@code final MapperS<FieldWithMetaString> thenArg2}
     * counted EXACTLY 1 occurrence in EACH PRE gen and 0 in each golden — the
     * OR-join lifts the block to mapSingleToList + the MapperC decl.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void callPutCurrency_singleFormBlockDeclGone() {
        assertEquals(0, count(gen(drrRuleOutput, DRR_RULES[2]),
                        "final MapperS<FieldWithMetaString> thenArg2"),
                "CallCurrency's single-form decl must be gone (PRE count 1)");
        assertEquals(0, count(gen(drrRuleOutput, DRR_RULES[3]),
                        "final MapperS<FieldWithMetaString> thenArg2"),
                "PutCurrency's single-form decl must be gone (PRE count 1)");
    }

    /**
     * The F-d witnesses (PriceCurrencyRule): the SINGLE-form ITE decl
     * {@code final MapperS<FieldWithMetaString> ifThenElseResult;} and the BARE
     * multi-callable arm root {@code ifThenElseResult = contract_Price_Monetary
     * .evaluate(thenArg0.get())} counted EXACTLY 1 occurrence EACH in the PRE gen
     * and 0 in the golden — the arm-join lifts the decl MapperC and the receiver
     * wraps {@code MapperC.<PriceSchedule>of(…)}.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void priceCurrency_singleFormIteAndBareReceiverGone() {
        String gen = gen(drrRuleOutput, DRR_RULES[4]);
        assertEquals(0, count(gen, "final MapperS<FieldWithMetaString> ifThenElseResult;"),
                "The single-form ITE decl must be gone (PRE count 1)");
        assertEquals(0, count(gen,
                        "ifThenElseResult = contract_Price_Monetary.evaluate(thenArg0.get())"),
                "The bare multi-callable arm root must be gone (PRE count 1)");
    }

    /**
     * The F-alpha family-bonus witnesses (GetUnderlierProductIdentifier): the
     * SINGLE-form base ITE decl {@code final MapperS<Product> thenArg2;} and its
     * single-wrap arm {@code thenArg2 = MapperS.of(underlierForProduct
     * .evaluate(product));} counted EXACTLY 1 occurrence EACH in the PRE gen and
     * 0 in the golden — the join lifts the decl MapperC with the singletonList
     * arm coercion.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void getUnderlierProductIdentifier_singleFormBaseIteGone() {
        String gen = gen(drrFnOutput, DRR_FNS[0]);
        assertEquals(0, count(gen, "final MapperS<Product> thenArg2;"),
                "The single-form base ITE decl must be gone (PRE count 1)");
        assertEquals(0, count(gen, "thenArg2 = MapperS.of(underlierForProduct.evaluate(product));"),
                "The MapperS single-wrap arm must be gone (PRE count 1)");
    }

    /**
     * The F-alpha family-bonus witness (GetUnderlierProductIdentifierLeg1): the
     * SINGLE-form base ITE decl {@code final MapperS<Product> thenArg0;} counted
     * EXACTLY 1 occurrence in the PRE gen and 0 in the golden.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void getUnderlierProductIdentifierLeg1_singleFormBaseIteGone() {
        assertEquals(0, count(gen(drrFnOutput, DRR_FNS[1]), "final MapperS<Product> thenArg0;"),
                "The single-form base ITE decl must be gone (PRE count 1)");
    }

    // ---------------------------------------------------------------- helpers

    private static int count(String haystack, String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
    }

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
                "Generated bytes must match the golden for " + path);
    }
}
