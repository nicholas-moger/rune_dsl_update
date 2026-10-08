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
 * PR #383 — the value-setter/flatten/Mapper-slot/rule-ladder-arm septet (7 byte flips:
 * MapSwapPayout cdm6 + Create_OnDemandRateChangePriceChangeInstruction cdm5+cdm6
 * [FUNCTION] + TotalNotionalQuantityLeg1 [drr FUNCTION] + NotionalQuantityLeg1Rule +
 * esma TotalNotionalQuantityOfLeg2Rule + QuantityUnitOfMeasureLeg1Rule [drr POJO]).
 *
 * <p><b>ctorCondValueSetterList</b> (ConstructionHandler + CondListCoerce): the
 * CTOR-seat META attribute with meta-FREE conditional arms takes the VALUE-list form
 * end-to-end ({@code final List<PriceSchedule> ifThenElseResult0;} + bare
 * {@code Collections.singletonList(priceSchedule)} + the {@code set<Name>Value}
 * setter via {@code ctorSetterName}'s existing fall-through); the segment-ADD leaf
 * seat keeps the #190 wrap law byte-frozen (golden
 * Enrich_ReportableEventWithUpiFromAnnaDsb — the conditional channel's one GREEN
 * wrap carrier). The pre-#383 ctor wrap law was LATENT (the #327 SF-2 note) —
 * MapSwapPayout, its first carrier, proved golden takes the Value form.
 *
 * <p><b>flattenElementTransparent</b> (NavigationHandler): FLATTEN joins the
 * element-transparent walk family (distinct/reverse) — it collapses list NESTING but
 * keeps the leaf element data type, so the #373 F-eps explicitParamPipedMeta arm's
 * element resolution crosses {@code then flatten} (the NotionalQuantityLeg1Rule
 * named-param pipe; tracer T383A: 5 NULL consults → all 10 resolve). BONUS: the arm
 * unblocked Create_OnDemandRateChangePriceChangeInstruction whole-file (cdm5+cdm6).
 *
 * <p><b>mapperCFactoryArmNoRewrap</b> (CollectionHandler): the #373 F-alpha
 * single-arm wrap treats an arm whose RENDER is already a {@code MapperC}
 * factory/chain as list-form by render truth — the re-wrap composed the
 * non-compiling {@code MapperC.of(MapperC.<PriceQuantity>of(…))} double-wrap
 * (golden carries ZERO such forms corpus-wide).
 *
 * <p><b>iteArmDistinctCollapseWrap</b> (FunctionExpressionRenderer): a rule-path
 * ITE-hoist arm that is a then-chain ending in the {@code distinct only-element}
 * collapse re-presents the collapsed value as a Mapper for the MapperS-typed slot
 * ({@code ifThenElseResult = MapperS.of(distinct(thenArg6).get());}) — the #266
 * ruleConsumerWrap law at the ITE-arm seat WITHOUT the consumer {@code .get()}.
 *
 * <p><b>defaultOperandMapperIteSlot + inLambdaBoolRungNest</b> (ControlFlowHandler):
 * the DEFAULT-operator LEFT operand joins the #357/#361 Mapper-form ITE slot family
 * ({@code final MapperS<BigDecimal> ifThenElseResult;} + {@code MapperS.of(…)} arms +
 * the typed {@code MapperS.<BigDecimal>ofNull()} terminal, consumed by
 * {@code ifThenElseResult.getOrDefault(defaultValue)}), and the #378-C bool-hoist
 * else-rung nesting gains its LAMBDA-channel twin — in-lambda bare-fn rung
 * conditions hoist {@code final Boolean booleanN} through the per-lambda group
 * (boolean0..4 pre-order), each hoisted rung nesting {@code } else { decl; if …}.
 *
 * <p><b>ruleLadderPlainArmChainDecomp + sumBlockArmSeat</b> (CollectionHandler): the
 * #382 D4b-2 law at the RULE block-ladder seat the #382 javadoc anticipated — the
 * nested-tree leaf-arm-then decline admits a RULE-path tree whose then-carrying leaf
 * arms are ALL plain arm-ROOT drain-admissible chains, and the arm bless gains the
 * RULE-path PLAIN-arm window class (renumber-safe: rule-path hoist naming is
 * per-scope literal — no method session, so the d5ef2ed0 FUNCTION-path exclusion
 * stands). The in-rung hoist keeps the wrapper ({@code final
 * MapperS<ReferenceWithMetaNonNegativeQuantitySchedule> thenArg = …;}) and a sum at
 * a block-arm {@code return} seat renders golden's {@code .sumBigDecimal()} via the
 * #354 blockArmSeatConditionals channel read. QuantityUnitOfMeasureLeg1Rule rides
 * the widening free (the #382 bonus-flip class).
 *
 * <p>Whole-file byte locks run through the REAL D11 routes (drr full cell + cdm5/cdm6
 * FUNCTION) and revert RED without the facets; every witness token is
 * occurrence-counted (python {@code str.count} semantics — the #352 law) and
 * PRE-counted against f-probe-382post (each removal token PRE &ge; 1 / golden 0;
 * each golden token PRE 0 / golden &ge; 1).
 */
class ValueSetterFlattenRuleLadderSeptetComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String MAP_SWAP_PAYOUT =
            "cdm/ingest/fpml/confirmation/product/swap/functions/MapSwapPayout.java";
    private static final String ON_DEMAND_RATE_CHANGE =
            "cdm/event/common/functions/Create_OnDemandRateChangePriceChangeInstruction.java";
    private static final String TOTAL_NOTIONAL_QUANTITY_LEG1 =
            "drr/standards/iosco/cde/version1/quantity/functions/TotalNotionalQuantityLeg1.java";
    private static final String NOTIONAL_QUANTITY_LEG1_RULE =
            "drr/regulation/common/trade/quantity/reports/NotionalQuantityLeg1Rule.java";
    private static final String ESMA_TOTAL_NOTIONAL_LEG2_RULE =
            "drr/regulation/esma/emir/refit/trade/reports/TotalNotionalQuantityOfLeg2Rule.java";
    private static final String QUANTITY_UOM_LEG1_RULE =
            "drr/standards/iosco/cde/version1/quantity/reports/QuantityUnitOfMeasureLeg1Rule.java";

    private static Map<String, String> drrOutput;
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
            drrOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
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

    /** The REAL D11 full-cell generation path (POJO/choice/rule/report/label + functions). */
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

    /** The REAL D11 FUNCTION-cell generation path. */
    private static Map<String, String> generateFunctions(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var funcGen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        assertNoGenerationErrors(funcGen.generateWithErrors(output));
        return output;
    }

    // ==== byte locks (all 7 flips through the REAL D11 routes) ====

    /** ctorCondValueSetterList: the ctor-seat Value-list carrier. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapSwapPayout_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_SWAP_PAYOUT);
    }

    /** flattenElementTransparent: the cdm5 bonus whole-file flip. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void onDemandRateChangeCdm5_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, ON_DEMAND_RATE_CHANGE);
    }

    /** flattenElementTransparent: the cdm6 bonus whole-file flip. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void onDemandRateChangeCdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, ON_DEMAND_RATE_CHANGE);
    }

    /** defaultOperandMapperIteSlot + inLambdaBoolRungNest: the Mapper-slot ladder carrier. */
    @Test
    @EnabledIf("drrCellAvailable")
    void totalNotionalQuantityLeg1_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, TOTAL_NOTIONAL_QUANTITY_LEG1);
    }

    /** flatten + doubleWrap + distinct-collapse wrap: the three-mechanism rule carrier. */
    @Test
    @EnabledIf("drrCellAvailable")
    void notionalQuantityLeg1Rule_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, NOTIONAL_QUANTITY_LEG1_RULE);
    }

    /** ruleLadderPlainArmChainDecomp + sumBlockArmSeat: the esma target carrier. */
    @Test
    @EnabledIf("drrCellAvailable")
    void esmaTotalNotionalQuantityOfLeg2Rule_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, ESMA_TOTAL_NOTIONAL_LEG2_RULE);
    }

    /** ruleLadderPlainArmChainDecomp: the bonus flip riding the same widening. */
    @Test
    @EnabledIf("drrCellAvailable")
    void quantityUnitOfMeasureLeg1Rule_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, QUANTITY_UOM_LEG1_RULE);
    }

    // ==== occurrence-counted witnesses (python str.count semantics — the #352 law) ====

    /**
     * ctorCondValueSetterList: the VALUE-list decl (PRE 0 / golden 1), the Value-setter
     * consumer (PRE 0 / golden 1) and the wrapper-builder wrap GONE (PRE 1 / golden 0 —
     * the negative witness the flip removes).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapSwapPayout_valueSetterList_witness() {
        String gen = cdm6FnOutput.get(MAP_SWAP_PAYOUT);
        assertNotNull(gen, "MapSwapPayout not generated");
        assertEquals(1, count(gen, "final List<PriceSchedule> ifThenElseResult0;"),
                "the conditional hoists the VALUE-typed list (PRE 0 / golden 1)");
        assertEquals(1, count(gen, ".setPriceScheduleValue(ifThenElseResult0)"),
                "the ctor consumes through the set<Name>Value setter (PRE 0 / golden 1)");
        assertEquals(0, count(gen,
                "Collections.singletonList(ReferenceWithMetaPriceSchedule.builder()"),
                "the wrapper-builder wrap must be gone (PRE 1 / golden 0)");
    }

    /**
     * flatten + doubleWrap + distinct-collapse: the per-use NUMBERED wrapper deref
     * (PRE 0 / golden 3 — the 0th param + its guard uses), the Mapper-wrapped
     * distinct-collapse assignment (PRE 0 / golden 1), and the three negatives —
     * the double-wrap (PRE 3 / golden 0), the MapperS.of(quantitySchedule) untyped
     * nav (PRE 2 / golden 0) and the bare collapse assignment (PRE 1 / golden 0).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void notionalQuantityLeg1Rule_flattenDerefDoubleWrapDistinct_witness() {
        String gen = drrOutput.get(NOTIONAL_QUANTITY_LEG1_RULE);
        assertNotNull(gen, "NotionalQuantityLeg1Rule not generated");
        assertEquals(3, count(gen, "fieldWithMetaNonNegativeQuantitySchedule0"),
                "the named-param wrapper derefs per-use with numbered coercion params "
                        + "(PRE 0 / golden 3 for the 0th)");
        assertEquals(1, count(gen, "ifThenElseResult = MapperS.of(distinct(thenArg6).get());"),
                "the distinct-collapse arm wraps MapperS.of at the Mapper-typed slot "
                        + "(PRE 0 / golden 1)");
        assertEquals(0, count(gen, "MapperC.of(MapperC.<"),
                "the double-wrap must be gone (PRE 3 / golden 0)");
        assertEquals(0, count(gen, "MapperS.of(quantitySchedule).map(\"getValue\""),
                "the untyped bare-symbol nav must be gone (PRE 2 / golden 0)");
        assertEquals(0, count(gen, "ifThenElseResult = distinct(thenArg6).get();"),
                "the bare collapse assignment must be gone (PRE 1 / golden 0)");
    }

    /**
     * defaultOperandMapperIteSlot + inLambdaBoolRungNest: the Mapper-typed blank-final
     * (PRE 0 / golden 1), the typed terminal (PRE 0 / golden 1), the LAST numbered
     * rung hoist boolean4 (PRE 0 / golden 1 — proves the 5-member numbered group),
     * and the two negatives — the bare-typed decl (PRE 1 / golden 0) and the
     * singleton {@code _boolean} (PRE 1 / golden 0 — renumbered by the rung hoists).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void totalNotionalQuantityLeg1_mapperSlotBoolRungs_witness() {
        String gen = drrOutput.get(TOTAL_NOTIONAL_QUANTITY_LEG1);
        assertNotNull(gen, "TotalNotionalQuantityLeg1 not generated");
        assertEquals(1, count(gen, "final MapperS<BigDecimal> ifThenElseResult;"),
                "the DEFAULT-left-operand slot types MapperS (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "MapperS.<BigDecimal>ofNull();"),
                "the elseless terminal takes the typed empty (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final Boolean boolean4"),
                "the bare-fn rung hoists number boolean0..4 (PRE 0 / golden 1 for the last)");
        assertEquals(0, count(gen, "final BigDecimal ifThenElseResult;"),
                "the bare-typed blank-final must be gone (PRE 1 / golden 0)");
        assertEquals(0, count(gen, "final Boolean _boolean"),
                "the singleton _boolean must renumber into the group (PRE 1 / golden 0)");
    }

    /**
     * ruleLadderPlainArmChainDecomp + sumBlockArmSeat: the plain in-rung WRAPPER-kept
     * hoist (PRE 0 / golden 1), the numbered rung hoist (PRE 0 / golden 1), the typed
     * block-arm sum (PRE 0 / golden 1), and the three negatives — the inline
     * {@code .sum()} (PRE 1 / golden 0), the shadowing runtime-then (PRE 1 /
     * golden 0) and the untyped empty terminal (PRE 2 / golden 0).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void esmaTotalNotionalQuantityOfLeg2Rule_ruleLadderArmSum_witness() {
        String gen = drrOutput.get(ESMA_TOTAL_NOTIONAL_LEG2_RULE);
        assertNotNull(gen, "TotalNotionalQuantityOfLeg2Rule not generated");
        assertEquals(1, count(gen,
                "final MapperS<ReferenceWithMetaNonNegativeQuantitySchedule> thenArg = "),
                "the plain arm hoists its chain base wrapper-KEPT (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final Boolean boolean1"),
                "the block ladder numbers its bare-fn rung hoists (PRE 0 / golden 1)");
        assertEquals(1, count(gen, ".sumBigDecimal()"),
                "the block-arm sum renders the typed method (PRE 0 / golden 1)");
        assertEquals(0, count(gen, ".sum()"),
                "the inline .sum() must be gone (PRE 1 / golden 0)");
        assertEquals(0, count(gen, ".then(_item -> _item"),
                "the shadowing runtime-then must be gone (PRE 1 / golden 0)");
        assertEquals(0, count(gen, "MapperC.of()"),
                "the untyped empty terminals must be gone (PRE 2 / golden 0)");
    }

    /**
     * ruleLadderPlainArmChainDecomp (the bonus flip): the numbered rung hoist
     * boolean2 (PRE 0 / golden 1) and EVERY runtime {@code .then(} gone (PRE 4 /
     * golden 0 — the whole file de-thens through the same widening).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void quantityUnitOfMeasureLeg1Rule_deThen_witness() {
        String gen = drrOutput.get(QUANTITY_UOM_LEG1_RULE);
        assertNotNull(gen, "QuantityUnitOfMeasureLeg1Rule not generated");
        assertEquals(1, count(gen, "final Boolean boolean2"),
                "the block ladder numbers its bare-fn rung hoists (PRE 0 / golden 1)");
        assertEquals(0, count(gen, ".then("),
                "every runtime .then( must be gone (PRE 4 / golden 0)");
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        int i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String path) throws IOException {
        assertNotNull(output, "generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "Class not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path);
    }
}
