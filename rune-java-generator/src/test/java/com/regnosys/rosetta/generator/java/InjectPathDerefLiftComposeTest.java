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
 * PR #364 — the inject-dep collision numbering + the set-path choice-option projection +
 * the arm-deref hoist channel + the add-arg meta wrap + the string-join trade residual +
 * the MapperC ite lift: 12 byte flips (1 cdm5 FUNCTION + 2 cdm6 FUNCTION + 5 drr
 * FUNCTION + 4 drr POJO Rules) + 0 new / 0 away by content (regscan364: 12/0/2
 * line-ratio-flagged — the two AnnaDsb riders are content-TOWARD, golden carries 17/6
 * hoists of the exact fired form; the #332/#356 ratio-noise class).
 *
 * <p><b>injectDepCollision</b> (FunctionDependencyCollector + ReferenceHandler): two
 * DIFFERENT functions whose lowerCamel field names collide render upstream's numbered
 * group — {@code commodityLeg10} (group head, imported) / {@code commodityLeg11} (FQN
 * inline, import suppressed: the head's single-type-import shadows the simple name even
 * same-package). The pure {@code collidingFunctionDependencyNumbering} map (FQN-sorted
 * groups) is consulted at BOTH the collector's field decls and the call-site receiver
 * (IsCSALeg1Aligned + IsCSALeg2Aligned drr; corpus census: exactly the 2 carriers).
 *
 * <p><b>setPathProjection</b> (FunctionExpressionRenderer): the #348-noted gap — the
 * choice-option / wrapper-aware multi-hop segment projection ({@code projectSegmentHops},
 * ORIGINAL attached nodes only per the #363 DETACHED-subtree law). A NULL projection
 * keeps the legacy reads byte-for-byte (the #341 wrapper-level-field belt for free).
 * Golden Create_AssetTransfer cdm6: {@code transfer.getOrCreateAsset()
 * .getOrCreateInstrument().getOrCreateSecurity().addIdentifier(…)}; the
 * {@code .getOrCreateValue()} wrapper-hop insert widens to BOTH meta kinds + MULTI hops
 * with upstream's placement law (mid-chain same-line, leaf-adjacent setter-line).
 *
 * <p><b>guardedDerefHoist</b> (JavaStatementScope + ControlFlowHandler +
 * ReferenceHandler + NavigationHandler): the RESTRICTED arm-deref sink channel
 * ({@code markArmDerefHoistSink}/{@code findArmDerefHoistSink}) unlocks the #237
 * evaluate-arg and #317 collapsed-meta producers at pathed-conditional ARM seats
 * without opening the conditional-hoist producers (arm-value ternaries keep their
 * golden form). Drains: the then/else arm windows relocate INTO the owning branch; a
 * NESTED conditional's cond-seat hoist drains before the nested {@code if (}
 * (UpdateIndexTransitionPriceAndRateOption cdm5/cdm6 ×2 +
 * Create_AnnaDsbUpiRequestUnderlyingForCredit drr).
 *
 * <p><b>metaWrapCoerce</b> (FunctionExpressionRenderer): a SINGLE Mapper-valued bare
 * alias/fn reference of the leaf's BARE value type ADDed to a wrapper-typed MULTI leaf
 * wraps element-wise at the MAPPER level ({@code .<Wrapper>map("Type coercion", x -> x
 * == null ? W.builder().build() : W.builder().setValue(x).build()).getMulti()}), params
 * numbered method-wide via the unified naming replay
 * (EnrichReportableEventWithUpiForSwaption drr ×2 statements).
 *
 * <p><b>stringJoinTradeResidual</b> (ReferenceHandler + CollectionHandler +
 * ArithmeticHandler): (1) the #357 named-top-level-extract law WITHOUT the
 * restructure-window gate — a named extract does not rebind the implicit item, so
 * name-unprefixed implicit receivers root on {@code MapperS.of(input)}; (2)
 * {@code isProvablyBareArm} gains the THEN-pipe recursion (through the #352
 * then-extract RExtractExpr wrapping) so the #334 mixed-join deref fires on the meta
 * if-arm; (3) the #362 comparison-seat bare-item retype mirrored at the ARITHMETIC
 * seat — the piped {@code MapperC<FieldWithMetaString>} item joins
 * {@code <String, String, String>} with the guarded in-join deref, the
 * {@code _fieldWithMetaString} escape falling out of the #363 escaped-iff-taken law
 * (TechnicalRecordIdRule asic/jfsa/mas + TechnicalRecordIdentificationRule hkma).
 *
 * <p><b>mapperCIteLift</b> (FunctionExpressionRenderer + ControlFlowHandler +
 * CollectionHandler): an INTERMEDIATE then-body conditional (the #257 Shape B) whose
 * then-arm is the BARE piped ITEM over a MapperC-bound thenArg lifts the
 * {@code ifThenElseResult} decl to MapperC (render truth — the #362 COMPILED-TYPE
 * stamp law), threading the #350 mapperFormArms mode: the then-arm keeps its Mapper
 * form + the elementwise unguarded deref, the scalar else coerces
 * {@code MapperC.of(Collections.singletonList(…))} (GetReportTrackingNumber drr).
 *
 * <p>Whole-file byte comparisons run through the REAL D11 generation paths and revert
 * RED without the facets. The negative witnesses are load-bearing per the
 * witness-uniqueness law (OCCURRENCE counts, never line counts): every token below was
 * occurrence-counted in its PRE gen (f-probe-363post; counts noted per witness) and 0
 * in its golden — the flips REMOVE them.
 */
class InjectPathDerefLiftComposeTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    /** The 1 cdm5 FUNCTION flip carrier (guardedDerefHoist + setPathProjection). */
    private static final String[] CDM5_FUNCTIONS = {
            "cdm/event/common/functions/UpdateIndexTransitionPriceAndRateOption.java",
    };

    /** The 2 cdm6 FUNCTION flip carriers. */
    private static final String[] CDM6_FUNCTIONS = {
            "cdm/event/common/functions/Create_AssetTransfer.java",
            "cdm/event/common/functions/UpdateIndexTransitionPriceAndRateOption.java",
    };

    /** The 5 drr FUNCTION flip carriers. */
    private static final String[] DRR_FUNCTIONS = {
            "drr/regulation/csa/rewrite/trade/functions/IsCSALeg1Aligned.java",
            "drr/regulation/csa/rewrite/trade/functions/IsCSALeg2Aligned.java",
            "drr/enrichment/upi/functions/Create_AnnaDsbUpiRequestUnderlyingForCredit.java",
            "drr/enrichment/upi/functions/EnrichReportableEventWithUpiForSwaption.java",
            "drr/regulation/common/functions/GetReportTrackingNumber.java",
    };

    /** The 4 drr POJO Rule flip carriers (stringJoinTradeResidual). */
    private static final String[] DRR_POJO_RULES = {
            "drr/regulation/asic/rewrite/trade/reports/TechnicalRecordIdRule.java",
            "drr/regulation/hkma/rewrite/trade/reports/TechnicalRecordIdentificationRule.java",
            "drr/regulation/jfsa/rewrite/trade/reports/TechnicalRecordIdRule.java",
            "drr/regulation/mas/rewrite/trade/reports/TechnicalRecordIdRule.java",
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

    // -------------------------------------------- cdm5 FUNCTION byte lock (1)

    @Test
    @EnabledIf("cdm5CellAvailable")
    void cdm5Functions_byteMatchGolden() throws IOException {
        for (String path : CDM5_FUNCTIONS) {
            assertBytes(path, fn(cdm5FnOutput, path), CDM5_GOLDEN_DIR);
        }
    }

    // -------------------------------------------- cdm6 FUNCTION byte locks (2)

    @Test
    @EnabledIf("cdm6CellAvailable")
    void cdm6Functions_byteMatchGolden() throws IOException {
        for (String path : CDM6_FUNCTIONS) {
            assertBytes(path, fn(cdm6FnOutput, path), CDM6_GOLDEN_DIR);
        }
    }

    // -------------------------------------------- drr FUNCTION byte locks (5)

    @Test
    @EnabledIf("drrCellAvailable")
    void drrFunctions_byteMatchGolden() throws IOException {
        for (String path : DRR_FUNCTIONS) {
            assertBytes(path, fn(drrFnOutput, path), DRR_GOLDEN_DIR);
        }
    }

    // -------------------------------------------- drr POJO Rule byte locks (4)

    @Test
    @EnabledIf("drrCellAvailable")
    void drrPojoRules_byteMatchGolden() throws IOException {
        for (String path : DRR_POJO_RULES) {
            assertBytes(path, cell(path), DRR_GOLDEN_DIR);
        }
    }

    // ------------------------------------------------------- negative witnesses

    /**
     * The injectDepCollision witness (IsCSALeg1Aligned drr): the single UN-numbered
     * dependency field {@code CommodityLeg1 commodityLeg1;} counted EXACTLY 1
     * occurrence in the PRE gen (f-probe-363post) and 0 in the golden — the collision
     * numbers the pair {@code commodityLeg10}/{@code commodityLeg11}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void isCsaLeg1_unNumberedDepFieldGone() {
        assertEquals(0, count(fn(drrFnOutput, DRR_FUNCTIONS[0]),
                        "CommodityLeg1 commodityLeg1;"),
                "The un-numbered collision dep field is gone (count 0 in golden)");
    }

    /**
     * The injectDepCollision witness (IsCSALeg2Aligned drr): the same un-numbered
     * field token for the Leg2 pair — PRE count 1, golden 0.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void isCsaLeg2_unNumberedDepFieldGone() {
        assertEquals(0, count(fn(drrFnOutput, DRR_FUNCTIONS[1]),
                        "CommodityLeg2 commodityLeg2;"),
                "The un-numbered collision dep field is gone (count 0 in golden)");
    }

    /**
     * The setPathProjection witness (Create_AssetTransfer cdm6): the collapsed
     * root-builder ADD {@code transfer.addAll(toBuilder(assetPayout(instruction)}
     * counted EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the projected
     * chain renders {@code .getOrCreateAsset().getOrCreateInstrument()
     * .getOrCreateSecurity().addIdentifier(…)}.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void createAssetTransfer_collapsedAddAllGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[0]),
                        "transfer.addAll(toBuilder(assetPayout(instruction)"),
                "The collapsed root-builder addAll is gone (count 0 in golden)");
    }

    /**
     * The guardedDerefHoist witnesses (UpdateIndexTransitionPriceAndRateOption
     * cdm5 + cdm6): the unhoisted re-wrap {@code MapperS.of(MapperS.of(priceQuantity)
     * .<FieldWithMetaPriceSchedule>mapC} counted EXACTLY 2 occurrences in each PRE gen
     * and 0 in the goldens — the operands hoist {@code final FieldWithMetaPriceSchedule
     * fieldWithMetaPriceSchedule0/1/2 = …get();} + the guarded
     * {@code (x == null ? MapperS.<PriceSchedule>ofNull() : MapperS.of(x.getValue()))}.
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void updateIndexTransition_cdm5_unhoistedRewrapGone() {
        assertEquals(0, count(fn(cdm5FnOutput, CDM5_FUNCTIONS[0]),
                        "MapperS.of(MapperS.of(priceQuantity).<FieldWithMetaPriceSchedule>mapC"),
                "The unhoisted operand re-wrap is gone (count 0 in golden)");
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void updateIndexTransition_cdm6_unhoistedRewrapGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[1]),
                        "MapperS.of(MapperS.of(priceQuantity).<FieldWithMetaPriceSchedule>mapC"),
                "The unhoisted operand re-wrap is gone (count 0 in golden)");
    }

    /**
     * The guardedDerefHoist witness (Create_AnnaDsbUpiRequestUnderlyingForCredit drr):
     * the raw wrapper evaluate-arg {@code translateIndexNameToId.evaluate(
     * indexName(product, useCase).get())} counted EXACTLY 2 occurrences in the PRE gen
     * (the nested cond + then-arm) and 0 in the golden — both deref through the hoisted
     * {@code fieldWithMetaString0/1} locals.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void annaDsbCredit_rawWrapperEvaluateArgGone() {
        assertEquals(0, count(fn(drrFnOutput, DRR_FUNCTIONS[2]),
                        "translateIndexNameToId.evaluate(indexName(product, useCase).get())"),
                "The raw wrapper evaluate-arg is gone (count 0 in golden)");
    }

    /**
     * The metaWrapCoerce witness (EnrichReportableEventWithUpiForSwaption drr): the
     * bare-list add {@code .addProductIdentifier(addProductIdentifier(reportableEvent,
     * upi).getMulti())} counted EXACTLY 2 occurrences in the PRE gen and 0 in the
     * golden — both wrap element-wise into ReferenceWithMetaProductIdentifier.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void enrichSwaption_bareAddProductIdentifierGone() {
        assertEquals(0, count(fn(drrFnOutput, DRR_FUNCTIONS[3]),
                        ".addProductIdentifier(addProductIdentifier(reportableEvent, upi).getMulti())"),
                "The bare-list addProductIdentifier is gone (count 0 in golden)");
    }

    /**
     * The mapperCIteLift witness (GetReportTrackingNumber drr): the MapperS-typed decl
     * {@code final MapperS<String> ifThenElseResult;} counted EXACTLY 1 occurrence in
     * the PRE gen and 0 in the golden — the render-truth lift declares
     * {@code final MapperC<String> ifThenElseResult;}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getReportTrackingNumber_mapperSDeclGone() {
        assertEquals(0, count(fn(drrFnOutput, DRR_FUNCTIONS[4]),
                        "final MapperS<String> ifThenElseResult;"),
                "The MapperS-typed ite decl is gone (count 0 in golden)");
    }

    /**
     * The stringJoinTradeResidual witnesses (the trade TechnicalRecordId rules ×4):
     * the BigDecimal-typed bare-item join {@code MapperMaths.<BigDecimal, BigDecimal,
     * BigDecimal>add(item, MapperS.of("_"))} counted EXACTLY 1 occurrence in each PRE
     * gen and 0 in the goldens — the join types {@code <String, String, String>} with
     * the guarded {@code item.<String>map("Type coercion", _fieldWithMetaString → …)}
     * deref.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void tradeTechnicalRecordIds_bigDecimalJoinGone() {
        for (String path : DRR_POJO_RULES) {
            assertEquals(0, count(cell(path),
                            "MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(item, MapperS.of(\"_\"))"),
                    () -> "The BigDecimal-typed bare-item join is gone in " + path);
        }
    }

    // ------------------------------------------------------------------ helpers

    private static String fn(Map<String, String> output, String path) {
        assertNotNull(output, "generation output missing (cell unavailable?)");
        String gen = output.get(path);
        assertNotNull(gen, () -> "no generated output for " + path);
        return gen;
    }

    private String cell(String path) {
        assertNotNull(drrCellOutput, "drr cell output missing");
        String gen = drrCellOutput.get(path);
        assertNotNull(gen, () -> "no generated output for " + path);
        return gen;
    }

    private static void assertBytes(String path, String gen, Path goldenDir) throws IOException {
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), () -> "golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(gen),
                () -> "byte mismatch vs golden for " + path);
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

    /** OCCURRENCE count (never line count — the #352 law). */
    private static int count(String haystack, String needle) {
        int n = 0;
        int i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }
}
