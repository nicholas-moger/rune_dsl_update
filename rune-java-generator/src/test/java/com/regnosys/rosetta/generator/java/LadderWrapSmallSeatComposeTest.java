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
 * PR #372 — the ladder/wrap/small-seat decathlon: 10 byte flips (7 FUNCTIONs +
 * 3 drr POJO Rules) + 0 new / 0 away (regscan372: 10/0/0; 3 common movers all
 * content-verified TOWARD — the ForRegime trio's reportingSide self-shadow
 * family bonus).
 *
 * <p><b>F-beta fnSetMultiCondBaseThenArg</b> (FunctionExpressionRenderer): the
 * FOURTH k==0 conditional-base quadrant — FUNCTION path + SET seat + MULTI base
 * renders the Mapper-form thenArg ladder ({@code final MapperC<String> thenArg2;}
 * with {@code MapperC.<String>of(fn)} arms + the #364 in-arm mixed-join derefs);
 * the base thenArg registers AFTER the arm interiors via placeholder substitution
 * (the #327 consumption-order law — golden thenArg0/1 in-arm, thenArg2 = the
 * base) and the real render runs on the REAL scope under the #366 chain-top flag:
 * GetUniqueSwapIdentifier + GetUniqueTransactionIdentifier drr FN.
 *
 * <p><b>F-gamma-A ruleCondBareInvokableValueMetaWrap</b>: the #264 value→meta
 * wrap law at the Shape-A ladder — all-bare-invokable arms hoist
 * {@code final String string0 = <rule>.evaluate(…);} + the wrap ternary, the
 * ladder decl takes the #331-joined WRAPPER element, the whole-output consumption
 * takes the #264 deref tail; the boolHoist gate admits bare RULE-call conditions
 * ({@code final Boolean _boolean = isCSAAlignedRule.evaluate(…);}):
 * DTCC_Leg1CommodityInstrumentIDRule + DTCC_Leg2CommodityInstrumentIDRule csa.
 *
 * <p><b>F-gamma-B condRungRuleValueMetaWrap</b> (ReferenceHandler +
 * CollectionHandler + FunctionExpressionRenderer): the #331 conditional-arm wrap
 * widens to nested else-if RUNGS (the elseBranch walk-up to the outermost
 * conditional), the MetaWrapValueHoist pulls into the owning rung, and the #333
 * whole-output deref channel gains the collapse-terminated piped-arm carve-out:
 * CollateralPortfolioCodeVariationMarginRule asic.
 *
 * <p><b>F-gamma-C setLeafValueMetaWrapHop</b>: a SINGLE Mapper-chain value SET on
 * a MULTI meta leaf renders the elementwise wrap hop + {@code .getMulti()}
 * ({@code .first().<ReferenceWithMetaTrade>map("Type coercion", trade -> trade ==
 * null ? ReferenceWithMetaTrade.builder().build() : …setValue(trade).build())
 * .getMulti()}); wrap-factory values decline to the #328 singletonList form (the
 * cp4b green catch): Create_ExposureFromTrades cdm6.
 *
 * <p><b>F-delta</b>: (1) mapperBooleanOperandNullSafe — the collapse-over-
 * invokable ({@code postAllocation(…) first}) and boolean-nav
 * ({@code -> inclusive}, leaf via the fallbackResolveFeature backup) operand
 * shapes join the {@code ComparisonResult.ofNullSafe} wrap; (2)
 * enumAssignArmRequalify — a bare mis-bound Capitalized ladder-arm RHS qualifies
 * by the OUTPUT enum ({@code intent = EventIntentEnum.CREDIT_EVENT;} /
 * {@code indicator = IndexEnum.ISDA;}); (3) comparisonIntWiden — the
 * numericOperandKind JOIN appends the elementwise Integer→BigDecimal hop at
 * INEQUALITY seats ({@code .<BigDecimal>map("Type coercion", integer0 -> …
 * BigDecimal.valueOf(integer0))}, method-wide numbering); (4)
 * boolLiteralLadderCondition — a LITERAL boolean ladder condition renders BARE
 * ({@code } else if (false) {}); (5) aliasSelfShadowItemFeature — a bare name
 * bound to the enclosing alias ITSELF is the upstream features-first mis-bind
 * (an alias cannot recurse), so the item nav synthesizes: CheckMaturity
 * cdm5+cdm6 + MapIntent cdm6 + GetIndexIndicatorFromFloatingRate drr FN.
 *
 * <p>Whole-file byte comparisons run through the REAL D11 generation paths and
 * revert RED without the facets. The negative witnesses are load-bearing per the
 * witness-uniqueness law (OCCURRENCE counts, never line counts): every token
 * below was occurrence-counted in its PRE gen (f-probe-371post — counts stated
 * per witness) and 0 in its golden — the flips REMOVE them.
 */
class LadderWrapSmallSeatComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    /** The 3 drr FUNCTION flip carriers (F-beta ×2 + F-delta). */
    private static final String[] DRR_FNS = {
            "drr/standards/iosco/uti/functions/GetUniqueSwapIdentifier.java",
            "drr/standards/iosco/uti/functions/GetUniqueTransactionIdentifier.java",
            "drr/regulation/common/functions/GetIndexIndicatorFromFloatingRate.java",
    };

    /** The 3 drr POJO Rule flip carriers (F-gamma-A ×2 + F-gamma-B). */
    private static final String[] DRR_RULES = {
            "drr/regulation/csa/rewrite/dtcc/reports/DTCC_Leg1CommodityInstrumentIDRule.java",
            "drr/regulation/csa/rewrite/dtcc/reports/DTCC_Leg2CommodityInstrumentIDRule.java",
            "drr/regulation/asic/rewrite/trade/reports/CollateralPortfolioCodeVariationMarginRule.java",
    };

    /** The cdm6 FUNCTION flip carriers (F-gamma-C + F-delta ×2). */
    private static final String[] CDM6_FNS = {
            "cdm/margin/schedule/functions/Create_ExposureFromTrades.java",
            "cdm/ingest/fpml/confirmation/workflowstep/functions/MapIntent.java",
            "cdm/product/collateral/functions/CheckMaturity.java",
    };

    /** The cdm5 FUNCTION flip carrier (F-delta). */
    private static final String CDM5_CHECK_MATURITY =
            "cdm/product/collateral/functions/CheckMaturity.java";

    private static Map<String, String> drrFnOutput;
    private static Map<String, String> drrRuleOutput;
    private static Map<String, String> cdm5FnOutput;
    private static Map<String, String> cdm6FnOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR)
                && Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR)
                && Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            D11CorpusRegressionTest.CellSpec drr =
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT);
            drrFnOutput = generateFunctions(drr);
            drrRuleOutput = generateRuleKinds(drr);
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

    // ---------------------------------------------------------- byte locks (10)

    @Test
    @EnabledIf("cellsAvailable")
    void drrFunctionFamily_byteMatchesGolden() throws IOException {
        for (String path : DRR_FNS) {
            assertBytes(path, gen(drrFnOutput, path), DRR_GOLDEN_DIR);
        }
    }

    @Test
    @EnabledIf("cellsAvailable")
    void drrRuleFamily_byteMatchesGolden() throws IOException {
        for (String path : DRR_RULES) {
            assertBytes(path, gen(drrRuleOutput, path), DRR_GOLDEN_DIR);
        }
    }

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6FunctionFamily_byteMatchesGolden() throws IOException {
        for (String path : CDM6_FNS) {
            assertBytes(path, gen(cdm6FnOutput, path), CDM6_GOLDEN_DIR);
        }
    }

    @Test
    @EnabledIf("cellsAvailable")
    void cdm5CheckMaturity_byteMatchesGolden() throws IOException {
        assertBytes(CDM5_CHECK_MATURITY, gen(cdm5FnOutput, CDM5_CHECK_MATURITY),
                CDM5_GOLDEN_DIR);
    }

    // ------------------------------------------------------- negative witnesses

    /**
     * The F-beta witnesses (GetUniqueSwapIdentifier): the VALUE-form ladder decl
     * {@code final String ifThenElseResult;} and the re-wrap thenArg decl
     * {@code final MapperS<FieldWithMetaString> thenArg2 = ifThenElseResult;}
     * counted EXACTLY 1 occurrence EACH in the PRE gen (f-probe-371post) and 0 in
     * the golden — the Mapper-form ladder IS thenArg2.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void getUniqueSwap_valueFormLadderGone() {
        String gen = gen(drrFnOutput, DRR_FNS[0]);
        assertEquals(0, count(gen, "final String ifThenElseResult;"),
                "The value-form ladder decl must be gone (PRE count 1)");
        assertEquals(0, count(gen, "final MapperS<FieldWithMetaString> thenArg2 = ifThenElseResult;"),
                "The re-wrap thenArg decl must be gone (PRE count 1)");
    }

    /**
     * The F-gamma-A arm witness (DTCC_Leg1): the UNWRAPPED bare-value arm
     * {@code ifThenElseResult = MapperS.of(dTCC_Leg1CommodityInstrumentIDRule
     * .evaluate(thenArg.get()));} counted EXACTLY 1 occurrence in the PRE gen and
     * 0 in the golden — the arm hoists the value local + the wrap ternary.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void dtccLeg1_unwrappedRuleArmGone() {
        assertEquals(0, count(gen(drrRuleOutput, DRR_RULES[0]),
                        "ifThenElseResult = MapperS.of(dTCC_Leg1CommodityInstrumentIDRule.evaluate(thenArg.get()));"),
                "The unwrapped bare-value arm must be gone (PRE count 1)");
    }

    /**
     * The F-gamma-A deref-tail witness (DTCC_Leg2): the direct output consumption
     * {@code output = ifThenElseResult.get();} counted EXACTLY 1 occurrence in the
     * PRE gen and 0 in the golden — the #264 deref tail hoists the wrapper local +
     * the if-null block.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void dtccLeg2_directOutputGetGone() {
        assertEquals(0, count(gen(drrRuleOutput, DRR_RULES[1]),
                        "output = ifThenElseResult.get();"),
                "The direct output .get() must be gone (PRE count 1)");
    }

    /**
     * The F-gamma-B rung witness (CollateralPortfolioCodeVariationMargin): the
     * unwrapped rung-2 return {@code return MapperS.of(collateralPortfolioCodeRule
     * .evaluate(item.get()));} counted EXACTLY 1 occurrence in the PRE gen and 0
     * in the golden — the rung hoists the value local + the wrap ternary in-branch.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void variationMargin_unwrappedRungReturnGone() {
        assertEquals(0, count(gen(drrRuleOutput, DRR_RULES[2]),
                        "return MapperS.of(collateralPortfolioCodeRule.evaluate(item.get()));"),
                "The unwrapped rung return must be gone (PRE count 1)");
    }

    /**
     * The F-gamma-C witness (Create_ExposureFromTrades): the scalar setter unwrap
     * {@code .first().get());} counted EXACTLY 1 occurrence in the PRE gen and 0
     * in the golden — the elementwise wrap hop + {@code .getMulti()} replaces it.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void exposureFromTrades_scalarSetterUnwrapGone() {
        assertEquals(0, count(gen(cdm6FnOutput, CDM6_FNS[0]), ".first().get());"),
                "The scalar setter unwrap must be gone (PRE count 1)");
    }

    /**
     * The F-delta-1/-3 witnesses (CheckMaturity cdm6; the cdm5 twin carries the
     * same counts): the UNWRAPPED boolean-nav orNullSafe receiver (PRE count 2)
     * and the UN-widened Integer operand juxtaposed against the BigDecimal sibling
     * (PRE count 4) — 0 in the goldens (the ofNullSafe wrap + the elementwise
     * BigDecimal hop land between).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void checkMaturity_unwrappedOperandsGone() {
        String gen = gen(cdm6FnOutput, CDM6_FNS[2]);
        assertEquals(0, count(gen,
                        ".<Boolean>map(\"getInclusive\", periodBound -> periodBound.getInclusive()).orNullSafe("),
                "The unwrapped boolean-nav orNullSafe receiver must be gone (PRE count 2)");
        assertEquals(0, count(gen,
                        ".<Integer>map(\"getPeriodMultiplier\", period -> period.getPeriodMultiplier()), MapperS.of(query)"),
                "The un-widened Integer comparison operand must be gone (PRE count 4)");
    }

    /**
     * The F-delta-2/-5 witnesses (MapIntent): the raw mis-bound enum assignment
     * {@code intent = CreditEvent;} (PRE count 1) and the recursive self-call
     * {@code areEqual(intentToAllocate(fpmlTrade} (PRE count 1) — 0 in the golden
     * (the qualified constant + the item-feature nav land).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void mapIntent_misBindsGone() {
        String gen = gen(cdm6FnOutput, CDM6_FNS[1]);
        assertEquals(0, count(gen, "intent = CreditEvent;"),
                "The raw enum-name assignment must be gone (PRE count 1)");
        assertEquals(0, count(gen, "areEqual(intentToAllocate(fpmlTrade"),
                "The recursive self-call must be gone (PRE count 1)");
    }

    /**
     * The F-delta-2/-4 witnesses (GetIndexIndicatorFromFloatingRate): the raw
     * mis-bound enum assignment {@code indicator = ISDA;} (PRE count 1) and the
     * wrapped literal ladder condition {@code } else if (MapperS.of(false)
     * .getOrDefault(false)) {} (PRE count 5) — 0 in the golden.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void getIndexIndicator_wrappedLiteralsGone() {
        String gen = gen(drrFnOutput, DRR_FNS[2]);
        assertEquals(0, count(gen, "indicator = ISDA;"),
                "The raw enum-name assignment must be gone (PRE count 1)");
        assertEquals(0, count(gen, "} else if (MapperS.of(false).getOrDefault(false)) {"),
                "The wrapped literal conditions must be gone (PRE count 5)");
    }

    // ------------------------------------------------------------------ helpers

    private static String gen(Map<String, String> output, String path) {
        assertNotNull(output, "generation did not run — corpus unavailable?");
        String gen = output.get(path);
        assertNotNull(gen, "Class not generated: " + path);
        return gen;
    }

    private static void assertBytes(String path, String generated, Path goldenDir)
            throws IOException {
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "missing golden: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                path + " must byte-match the frozen 9.83.0 golden (PR #372)");
    }

    /** OCCURRENCE count (python str.count semantics) — the #352 law, never line counts. */
    private static int count(String s, String token) {
        int n = 0;
        int i = s.indexOf(token);
        while (i >= 0) {
            n++;
            i = s.indexOf(token, i + 1);
        }
        return n;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
