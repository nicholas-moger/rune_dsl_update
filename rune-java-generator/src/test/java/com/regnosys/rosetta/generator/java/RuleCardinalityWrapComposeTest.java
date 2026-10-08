package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.utils.DeepFeatureCallUtil;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.ast.model.RModel;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #308 — facet cardinalityWrapCompose: FOUR disjoint green-safe GENERATOR facets (parser
 * UNTOUCHED), 4 drr POJO Rule byte flips; 3 source files
 * ({@code LogicalHandler} + {@code CollectionHandler} + {@code FunctionExpressionRenderer}).
 *
 * <ul>
 *   <li><b>andNullSafe RRule operand</b> (csa BasketConstituents): a bare REPORTING-RULE (RRule)
 *       operand of {@code and}/{@code orNullSafe} (the filter {@code IsAllowableActionForCSA and
 *       CustomBasketIndicator}, where {@code CustomBasketIndicator → customBasketIndicatorRule})
 *       renders {@code MapperS.of(<rule>.evaluate(...))}, a {@code MapperS<Boolean>} not a
 *       {@code ComparisonResult}, so {@code .andNullSafe(...)} does not compile; golden wraps it
 *       {@code ComparisonResult.ofNullSafe(MapperS.of(...))}. {@code LogicalHandler
 *       .wrapBooleanFunctionOperand}'s gate extended {@code RFunction → RFunction|RRule}.</li>
 *   <li><b>M1 reportOutputCardinalityBaseWrap</b> (PriceNoFormat/PriceNotation if-arm): a bare
 *       MULTI-output FUNCTION thenArg BASE ({@code PriceOfZeroCouponSwaps then …}) wraps
 *       {@code MapperC.<X>of(...)} not the #276 {@code MapperS.of(...)} (a {@code MapperS<List<X>>}
 *       type mismatch).</li>
 *   <li><b>M2 reportOutputCardinalityOnlyElement</b> (PriceNotation/StrikePriceNotationEnum): an
 *       only-element over a bare MULTI-output FUNCTION ({@code Contract_Price_Monetary /
 *       Contract_StrikePrice only-element extract GetPriceNotation}) wraps
 *       {@code MapperS.of(MapperC.of(<fn>.evaluate(...)).get())} — the raw {@code <List>.get()} has
 *       no no-arg get(), and the downstream extract needs a Mapper receiver.</li>
 *   <li><b>M3 reportOutputCardinalityArmGet</b> (PriceNoFormat else-arm): a bare SINGLE-output
 *       FUNCTION conditional arm ({@code else PriceOfEvent}) assigns its {@code <fn>.evaluate(input)}
 *       verbatim — no spurious {@code .get()} on the bare value.</li>
 * </ul>
 *
 * <p><b>Green-safe by construction.</b> Every facet rewrites only a currently NON_COMPILING shape:
 * {@code MapperS<Boolean>.andNullSafe(...)}, {@code MapperC<X> = MapperS.of(<list>)},
 * {@code <List>.get()}, and {@code .get()} on a bare value all fail to compile, so no green file
 * carries the pre-fix form. byte-oracle / stash-baseline measured exactly 4 (drr POJO 372 → 368; 16
 * now-matching = 4 mine + 12 stale, clean re-dump = exactly the 12 stale); regscan 0 within-waiver
 * regressions / 0 new / 4 flipped-out; all FUNCTION cells (cdm5 79 / cdm6 232 / drr 206) + cdm/iso/fpml
 * POJO byte-IDENTICAL.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED — the 4 flip locks + the 4
 * positive-content locks fail on clean source; the 3 green-safety locks pass either way.
 */
class RuleCardinalityWrapComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> drrOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            drrOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        var labelProviderGen = new LabelProviderGenerator(
                gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                assertNoGenerationErrors(pojoGen.generateClasses(model, version, output));
                assertNoGenerationErrors(choiceGen.generateClasses(model, version, output));
                assertNoGenerationErrors(ruleGen.generateClasses(model, version, output));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
                assertNoGenerationErrors(labelProviderGen.generateClasses(model, version, output));
            }
        }
        funcGen.generate(output);
        return output;
    }

    // ==== Flip locks (revert-RED): the 4 carriers now byte-match golden. ====

    /** Flip — csa BasketConstituents (andNullSafe RRule operand). */
    @Test
    @EnabledIf("drrCellAvailable")
    void basketConstituents_csa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/csa/rewrite/trade/reports/BasketConstituentsRule.java");
    }

    /** Flip — iosco cde version1 PriceNoFormat (M1 base MapperC wrap + M3 else-arm .get() strip). */
    @Test
    @EnabledIf("drrCellAvailable")
    void priceNoFormat_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version1/price/reports/PriceNoFormatRule.java");
    }

    /** Flip — iosco cde version1 PriceNotation (M1 base MapperC wrap + M2 only-element wrap). */
    @Test
    @EnabledIf("drrCellAvailable")
    void priceNotation_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version1/price/reports/PriceNotationRule.java");
    }

    /** Flip — iosco cde version1 StrikePriceNotationEnum (M2 only-element wrap). */
    @Test
    @EnabledIf("drrCellAvailable")
    void strikePriceNotationEnum_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version1/price/reports/StrikePriceNotationEnumRule.java");
    }

    // ==== Positive-content locks (revert-RED): the rendered facet form, not just byte-match. ====

    /** andNullSafe — BasketConstituents wraps the RRule operand ComparisonResult.ofNullSafe(...). */
    @Test
    @EnabledIf("drrCellAvailable")
    void basketConstituents_rendersComparisonResultWrap() {
        String gen = gen("drr/regulation/csa/rewrite/trade/reports/BasketConstituentsRule.java");
        assertTrue(gen.contains(
                ".andNullSafe(ComparisonResult.ofNullSafe(MapperS.of(customBasketIndicatorRule.evaluate(item.get()))))"),
                "Expected the RRule operand wrapped ComparisonResult.ofNullSafe(MapperS.of(...))");
    }

    /** M1 — PriceNoFormat wraps the bare MULTI-output function base MapperC.<X>of(...). */
    @Test
    @EnabledIf("drrCellAvailable")
    void priceNoFormat_rendersMapperCBaseWrap() {
        String gen = gen("drr/standards/iosco/cde/version1/price/reports/PriceNoFormatRule.java");
        assertTrue(gen.contains(
                "MapperC.<NonNegativeQuantitySchedule>of(priceOfZeroCouponSwaps.evaluate(input))"),
                "Expected the multi-output base wrapped MapperC.<NonNegativeQuantitySchedule>of(...)");
    }

    /** M2 — StrikePriceNotationEnum wraps the only-element MapperS.of(MapperC.of(<fn>).get()). */
    @Test
    @EnabledIf("drrCellAvailable")
    void strikePriceNotationEnum_rendersOnlyElementWrap() {
        String gen = gen("drr/standards/iosco/cde/version1/price/reports/StrikePriceNotationEnumRule.java");
        assertTrue(gen.contains(
                "MapperS.of(MapperC.of(contract_StrikePrice.evaluate(input)).get())"),
                "Expected the only-element over a bare multi fn wrapped MapperS.of(MapperC.of(...).get())");
    }

    /** M3 — PriceNoFormat assigns the bare single-output function else-arm verbatim (no .get()). */
    @Test
    @EnabledIf("drrCellAvailable")
    void priceNoFormat_rendersVerbatimArm() {
        String gen = gen("drr/standards/iosco/cde/version1/price/reports/PriceNoFormatRule.java");
        assertTrue(gen.contains("output = priceOfEvent.evaluate(input);"),
                "Expected the bare single-output fn else-arm assigned verbatim (no spurious .get())");
        assertTrue(!gen.contains("priceOfEvent.evaluate(input).get()"),
                "The spurious .get() on the bare value must be gone");
    }

    // ==== Green-safety decline locks: each gate boundary stays byte-matching golden. ====

    /**
     * Green-safety — asic SmallScaleBuySideEntityIndicatorRule STAYS byte-matching golden. Its
     * filter uses {@code exists(...).andNullSafe(areEqual(...))} — a NON-rule (comparison/existence)
     * operand. The andNullSafe RRule gate fires only on an RFunction/RShortcut/RRule operand, so a
     * comparison operand is untouched.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void smallScaleBuySide_andNullSafeComparison_staysGreen() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/asic/rewrite/margin/reports/SmallScaleBuySideEntityIndicatorRule.java");
    }

    /**
     * Green-safety — asic UniqueTransactionIdentifierProprietaryRule STAYS byte-matching golden. Its
     * then-chain base is a bare SINGLE-output function ({@code MapperS.of(<fn>.evaluate(input))}); the
     * M1 base-wrap fires only on a MULTI-output function base, so this single-output base keeps the
     * #276 MapperS.of form.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void uti_singleOutputBase_staysGreen() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/asic/rewrite/margin/reports/UniqueTransactionIdentifierProprietaryRule.java");
    }

    /**
     * Green-safety — asic AssetClassRule STAYS byte-matching golden. Its whole-output body is a bare
     * single-output function ({@code output = <fn>.evaluate(input);}) handled by the whole-output
     * fast path (renderOperationInner); the M3 conditional-arm verbatim treatment is a DIFFERENT seat
     * and does not touch it.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void assetClass_wholeOutputBareFn_staysGreen() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/asic/rewrite/trade/reports/AssetClassRule.java");
    }

    private static String gen(String path) {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String g = drrOutput.get(path);
        assertNotNull(g, "Class not generated: " + path);
        return g;
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String generated = drrOutput.get(path);
        assertNotNull(generated, "Class not generated: " + path
                + " (emission failed or the path differs)");
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated drr output must byte-match the golden (newline-normalized) for "
                + path + " (PR #308 cardinalityWrapCompose).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
