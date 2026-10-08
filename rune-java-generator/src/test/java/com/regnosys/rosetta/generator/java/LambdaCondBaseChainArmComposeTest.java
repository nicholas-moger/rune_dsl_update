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
 * PR #374 — the lambda-cond-base/chain-arm/deref-join sextet: 6 byte flips (2 drr
 * FUNCTIONs + 4 drr POJO Rules) + 0 new / 0 away (regscan374: 6/0/0; 4 common
 * movers all content-verified TOWARD — the Country csa A1 partial carry + the
 * Equity pair and csa QuantityUnitOfMeasureLeg1 riding the C1 ladder deref with
 * the deref-group NUMBERING interleave banked).
 *
 * <p><b>lambdaCondBaseThenArg</b> (CollectionHandler tryDeepThenHoist +
 * ControlFlowHandler hoistAsDeepThenMapperLocalOrNull): the in-lambda k==0
 * conditional-BASE hoists as golden's {@code final MapperS<Product> _thenArg;
 * if (…) { _thenArg = …; } else { _thenArg = …; }} block and the continuation
 * applies ONCE to the local — the #351-i3 pattern at the LAMBDA channel (the
 * anyCondLevel sink gate admits the cond-only-at-base lambda route; the
 * handshake arm registers the pre-built block through registerPendingLambdaHoist
 * as the marker-classed DeepThenCondArgHoist; compileElselessConditionalBlock
 * drains it into the if-branch, renderDrainedHoists re-anchoring per line):
 * UnderlyingIdentificationRule esma + fca, and GetReportableSchedulePeriod drr
 * FN as the family bonus (the MapperC-kind sibling with the block-lambda ctor
 * conversion riding free).
 *
 * <p><b>ruleSingleCondBaseChainArm</b> (FunctionExpressionRenderer): the #316
 * SINGLE+RULE quadrant's CHAIN-ARM widening — a then-CARRYING conditional base
 * renders the if/else thenArg block on the REAL scope under the #366 chain-top
 * flag (the arm-interior chain hoists {@code thenArg0} in-arm via the #361
 * extract-conditional admit + the #333 armDrains pull), the base thenArg
 * registers AFTER the core (the #327 consumption-order placeholder), and
 * wrapMapperFormIteArm gains the seat's {@code multi} flag so the SINGLE
 * (MapperS-decl) seat keeps mapper-chain arms verbatim:
 * PackageTransactionSpreadCurrencyRule iosco cde v1.
 *
 * <p><b>flattenAddMetaElemDeref</b> (FunctionExpressionRenderer, the ADD seat):
 * a standalone {@code then flatten} tail over a MapperListOfLists decl whose
 * ELEMENT is a meta wrapper derefs element-wise before the List expansion (the
 * bare MapperC lambda, non-registering — the #178 law), render-truth anchored on
 * the renderer's own decl element + the {@code .flattenList().getMulti()}
 * suffix: GetAllUnderlierProductIdentifier drr FN.
 *
 * <p><b>ladderHeteroWrapperJoin</b> (CollectionHandler, the #356 ladder path): a
 * ladder whose COMPILED leaf arms carry TWO DISTINCT meta wrappers of ONE value
 * type joins BARE — the #295 deref pass runs and the typed empty follows the
 * value type: QuantityUnitOfMeasureLeg2Rule iosco cde v1.
 *
 * <p>The negative witnesses are load-bearing per the witness-uniqueness law
 * (OCCURRENCE counts, never line counts): every token was PRE-counted against
 * f-probe-373post (the counts cited per witness) and 0 in its golden — the flips
 * REMOVE them, so each witness is RED on the pre-facet source.
 */
class LambdaCondBaseChainArmComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    /** The 2 drr FUNCTION flip carriers (A1 family bonus + A3). */
    private static final String[] DRR_FNS = {
            "drr/standards/iosco/cde/base/price/functions/GetReportableSchedulePeriod.java",
            "drr/regulation/common/functions/GetAllUnderlierProductIdentifier.java",
    };

    /** The 4 drr POJO Rule flip carriers (A1 ×2 + A2 + C1). */
    private static final String[] DRR_RULES = {
            "drr/regulation/esma/emir/refit/trade/reports/UnderlyingIdentificationRule.java",
            "drr/regulation/fca/ukemir/refit/trade/reports/UnderlyingIdentificationRule.java",
            "drr/standards/iosco/cde/version1/price/reports/PackageTransactionSpreadCurrencyRule.java",
            "drr/standards/iosco/cde/version1/quantity/reports/QuantityUnitOfMeasureLeg2Rule.java",
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
     * The A1 witness (esma UnderlyingIdentificationRule): the runtime-then on the
     * BARE else-arm call {@code productForEvent.evaluate(item.get()).then(} counted
     * EXACTLY 1 occurrence in the PRE gen (f-probe-373post) and 0 in the golden —
     * the conditional base now hoists as the in-lambda {@code _thenArg} if/else.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void esmaUnderlyingIdentification_runtimeThenOnBareElseArmGone() {
        assertEquals(0, count(gen(drrRuleOutput, DRR_RULES[0]),
                        "productForEvent.evaluate(item.get()).then("),
                "The runtime .then( on the bare else arm must be gone (PRE count 1)");
    }

    /** The A1 witness, fca sibling — the SAME token (PRE 1 / golden 0). */
    @Test
    @EnabledIf("cellsAvailable")
    void fcaUnderlyingIdentification_runtimeThenOnBareElseArmGone() {
        assertEquals(0, count(gen(drrRuleOutput, DRR_RULES[1]),
                        "productForEvent.evaluate(item.get()).then("),
                "The runtime .then( on the bare else arm must be gone (PRE count 1)");
    }

    /**
     * The A1 family-bonus witness (GetReportableSchedulePeriod): the inline-ternary
     * conditional base {@code .getOrDefault(false) ? MapperS.of(calculationSchedule)}
     * counted EXACTLY 1 in the PRE gen and 0 in the golden — the base hoists as the
     * in-lambda {@code final MapperC<SchedulePeriod> thenArg0;} if/else block.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void getReportableSchedulePeriod_inlineTernaryCondBaseGone() {
        assertEquals(0, count(gen(drrFnOutput, DRR_FNS[0]),
                        ".getOrDefault(false) ? MapperS.of(calculationSchedule)"),
                "The inline-ternary conditional base must be gone (PRE count 1)");
    }

    /**
     * The A2 witness (PackageTransactionSpreadCurrencyRule): the un-parameterized
     * wrong-kind ternary else {@code : MapperC.of();} counted EXACTLY 1 in the PRE
     * gen and 0 in the golden — the else arm now renders the typed
     * {@code MapperS.<FieldWithMetaString>ofNull()} inside the if/else block.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void packageTransactionSpread_bareMapperCOfElseGone() {
        assertEquals(0, count(gen(drrRuleOutput, DRR_RULES[2]), ": MapperC.of();"),
                "The bare MapperC.of() ternary else must be gone (PRE count 1)");
    }

    /**
     * The A3 witness (GetAllUnderlierProductIdentifier): the deref-less flatten
     * terminal {@code .flattenList().getMulti()} counted EXACTLY 2 occurrences in
     * the PRE gen and 0 in the golden — both ADD tails now deref element-wise
     * ({@code .flattenList().<ProductIdentifier>map("Type coercion", …).getMulti()}).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void getAllUnderlierProductIdentifier_bareFlattenGetMultiGone() {
        assertEquals(0, count(gen(drrFnOutput, DRR_FNS[1]), ".flattenList().getMulti()"),
                "The deref-less flatten terminals must be gone (PRE count 2)");
    }

    /**
     * The C1 witness (QuantityUnitOfMeasureLeg2Rule): the wrapper-typed empty
     * {@code MapperC.<FieldWithMetaNonNegativeQuantitySchedule>ofNull()} counted
     * EXACTLY 1 in the PRE gen and 0 in the golden — the heterogeneous-wrapper
     * ladder now joins BARE ({@code MapperC.<NonNegativeQuantitySchedule>ofNull()}
     * with both arms deref'd to the value type).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void quantityUnitOfMeasureLeg2_wrapperTypedEmptyGone() {
        assertEquals(0, count(gen(drrRuleOutput, DRR_RULES[3]),
                        "MapperC.<FieldWithMetaNonNegativeQuantitySchedule>ofNull()"),
                "The wrapper-typed empty must be gone (PRE count 1)");
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
