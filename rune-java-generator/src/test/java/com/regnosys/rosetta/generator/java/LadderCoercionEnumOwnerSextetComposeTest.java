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
 * PR #387 — the ladder-coercion / enum-owner sextet (6 byte flips:
 * GetUnderlierLEIForCreditCSA [drr FUNCTION] + MapAssetIdType [cdm6 FUNCTION] +
 * esma/fca IndicatorOfTheUnderlyingIndexRule + jfsa UnderlyingIndexIndicatorRule +
 * common NameOfTheUnderlyingIndexRule [drr POJO Rules]).
 *
 * <p><b>extractLadderChainTypeStamp</b> (CollectionHandler): the mcLadder block's
 * typed-empty terminal renders the arm-join's META element — the render truth — but the
 * extract assembly returned the chain type-less, blocking the EXISTING
 * {@code coerceAddValueMetaItem} A4 arm at the generic-isAdd seat (the #386-banked
 * decode; a parallel deref splice was built + REVERTED zero-fire there).
 * {@code LambdaCompiled} carries the ladder's META item out (populated ONLY by
 * {@code compileLadderConditionalBlock} for a {@code RJavaWithMetaValue} typed empty);
 * the assembly stamps {@code MapperC<meta>} scoped to the verified
 * {@code mapSingleToList} carrier form — A4 then derefs {@code .<String>map("Type
 * coercion", …)} before {@code .getMulti()}.
 *
 * <p><b>toStringLadderArmBareEvidence + drainConsumerChainTypeStamp +
 * toStringCollapsedMetaRetype</b> (CollectionHandler + ConversionHandler +
 * HandlerHelper + JavaStatementScope): a to-string arm is definitionally BARE
 * evidence (rune {@code to-string} produces plain {@code string}), so the four
 * Indicator/Name ladders join bare and the EXISTING #295 deref pass fires; the
 * drain-hoisted consumer chains ({@code _thenArg1.mapItem(…)}) stamp their type from
 * the raw thenArg binding + the #237 terminal walker INSIDE the join-bare arm-REPLAY
 * window only ({@code isLadderJoinBareArmReplay} — an unscoped stamp regressed green
 * FilterInvalidFloatingRateIndexTradeDate, the cp2-387 catch); the to-string collapse
 * arms retype via the SAME walker so the shared receiver unwrap emits the guarded
 * deref, and the sink-less in-rung hoist rides the pending-lambda channel as a
 * sentinel {@code ItemGetMetaDerefHoist} (the #346 law) with the multi-line collapse
 * admit scoped to the replay window (the cp8-356 sideways catch stands elsewhere).
 *
 * <p><b>enumAssignArmOwnerRequalify</b> (FunctionExpressionRenderer): the
 * RESOLVED-MIS-BOUND sibling of the #372 bare-echo arm — a bare enum-value arm the
 * linker bound to the WRONG same-named enum ({@code then CUSIP} on an AssetIdTypeEnum
 * output rendering {@code ProductIdTypeEnum.CUSIP} — the exact bug the old waiver
 * comment predicted as "enumTypeRename") requalifies against the output enum,
 * RENDER-TRUTH-gated on the assignment being exactly the bound-owner constant, with
 * the wrong owner's ref swapped so its import prunes; the terminal else arm takes the
 * same helper symmetric with the then-arm.
 *
 * <p>Whole-file byte locks run through the REAL D11 routes (drr full cell + drr/cdm6
 * FUNCTION) and revert RED without the facets; every witness token is
 * occurrence-counted (python {@code str.count} semantics — the #352 law) and
 * PRE-counted against f-probe-386post (each removal token PRE &ge; 1 / golden 0;
 * each golden token PRE 0 / golden &ge; 1 — witness387.py).
 */
class LadderCoercionEnumOwnerSextetComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String LEI =
            "drr/regulation/csa/rewrite/functions/GetUnderlierLEIForCreditCSA.java";
    private static final String MAP_ASSET_ID_TYPE =
            "cdm/ingest/fpml/confirmation/pricequantity/functions/MapAssetIdType.java";
    private static final String IND_ESMA =
            "drr/regulation/esma/emir/refit/trade/reports/IndicatorOfTheUnderlyingIndexRule.java";
    private static final String IND_FCA =
            "drr/regulation/fca/ukemir/refit/trade/reports/IndicatorOfTheUnderlyingIndexRule.java";
    private static final String UII_JFSA =
            "drr/regulation/jfsa/rewrite/trade/reports/UnderlyingIndexIndicatorRule.java";
    private static final String NAME_COMMON =
            "drr/regulation/common/trade/underlier/reports/NameOfTheUnderlyingIndexRule.java";

    private static Map<String, String> drrPojoOutput;
    private static Map<String, String> drrFnOutput;
    private static Map<String, String> cdm6FnOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR)
                && Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            var drrSpec = new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT);
            var cdm6Spec = new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT);
            drrPojoOutput = generateCell(drrSpec);
            drrFnOutput = generateFunctions(drrSpec);
            cdm6FnOutput = generateFunctions(cdm6Spec);
        }
    }

    /**
     * The D11-shaped POJO/rule generation path — generator wiring identical to
     * {@code D11CorpusRegressionTest}'s cell loop (same emission filter, same
     * {@code generators.doNotPrune} configuration, same generator classes and
     * order). The harness-side generator-error capture and the standalone
     * function-file emission are deliberately omitted: this helper serves only
     * the four POJO byte locks, whose renders are byte-verified against golden.
     */
    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell),
                D11CorpusRegressionTest.readDoNotPrune(cell));
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
        return output;
    }

    /**
     * The REAL D11 FUNCTION-cell generation path (readDoNotPrune deliberately
     * omitted — correct for function cells, the Seat-1 #386 OBS-5 note).
     */
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

    // ==== byte locks (all 6 flips through the REAL D11 routes) ====

    /** extractLadderChainTypeStamp: the A4 add-seat deref carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void getUnderlierLeiForCreditCsa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, LEI);
    }

    /** enumAssignArmOwnerRequalify: the mis-bound assignment-ladder carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapAssetIdType_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_ASSET_ID_TYPE);
    }

    /** The join-bare trio: the esma Indicator carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void indicatorOfTheUnderlyingIndexEsma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrPojoOutput, DRR_GOLDEN_DIR, IND_ESMA);
    }

    /** The join-bare trio: the fca twin. */
    @Test
    @EnabledIf("cellsAvailable")
    void indicatorOfTheUnderlyingIndexFca_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrPojoOutput, DRR_GOLDEN_DIR, IND_FCA);
    }

    /** The join-bare trio: the jfsa sibling (numbered to-string coercion group). */
    @Test
    @EnabledIf("cellsAvailable")
    void underlyingIndexIndicatorJfsa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrPojoOutput, DRR_GOLDEN_DIR, UII_JFSA);
    }

    /** The join-bare trio: the multi-line list-literal collapse hoist carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void nameOfTheUnderlyingIndexCommon_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrPojoOutput, DRR_GOLDEN_DIR, NAME_COMMON);
    }

    // ==== occurrence-counted witnesses (python str.count semantics — the #352 law) ====

    /**
     * extractLadderChainTypeStamp: the A4 elementwise deref lands BARE before the
     * {@code .getMulti()} (PRE 0 / golden 1); negative — the un-deref'd
     * {@code }).getMulti());} block tail (PRE 1 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void lei_addSeatDeref_witness() {
        String gen = drrFnOutput.get(LEI);
        assertNotNull(gen, "GetUnderlierLEIForCreditCSA not generated");
        assertEquals(1, count(gen, ".<String>map(\"Type coercion\", "
                        + "fieldWithMetaString -> fieldWithMetaString.getValue()).getMulti());"),
                "the A4 add-seat deref must land BARE (PRE 0 / golden 1)");
        assertEquals(0, count(gen, "\t\t\t\t}).getMulti());"),
                "the un-deref'd block tail must be gone (PRE 1 / golden 0)");
    }

    /**
     * enumAssignArmOwnerRequalify: the output enum's constants land on every rung +
     * the terminal else (CUSIP PRE 0 / golden 1; OTHER PRE 0 / golden 1); negative —
     * the wrong owner {@code ProductIdTypeEnum} disappears entirely, import included
     * (PRE 9 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void mapAssetIdType_enumOwner_witness() {
        String gen = cdm6FnOutput.get(MAP_ASSET_ID_TYPE);
        assertNotNull(gen, "MapAssetIdType not generated");
        assertEquals(1, count(gen, "identifierType = AssetIdTypeEnum.CUSIP;"),
                "the first rung requalifies to the OUTPUT enum (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "identifierType = AssetIdTypeEnum.OTHER;"),
                "the terminal else requalifies too (PRE 0 / golden 1)");
        assertEquals(0, count(gen, "ProductIdTypeEnum"),
                "the wrong owner must be gone, import included (PRE 9 / golden 0)");
    }

    /**
     * The join-bare trio (esma): the decl follows the bare join (PRE 0 / golden 1;
     * negative: the wrapper decl PRE 1 / golden 0), the typed empty follows (PRE 0 /
     * golden 1; negative: the wrapper ofNull PRE 1 / golden 0), the MapperC.of arm
     * derefs GUARDED + numbered in text order (PRE 0 / golden 1), the mapItem
     * consumer tails deref BARE (PRE 0 / golden 3), the to-string receiver derefs
     * before the map (PRE 0 / golden 1), and the downstream consumer simplifies to
     * the direct {@code item.get()} form (PRE 0 / golden 1).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void indicatorEsma_joinBareDerefs_witness() {
        String gen = drrPojoOutput.get(IND_ESMA);
        assertNotNull(gen, "esma IndicatorOfTheUnderlyingIndexRule not generated");
        assertEquals(1, count(gen, "final MapperC<String> thenArg2 = thenArg1"),
                "the pipe decl follows the bare join (PRE 0 / golden 1)");
        assertEquals(0, count(gen, "final MapperC<FieldWithMetaString> thenArg2"),
                "the wrapper decl must be gone (PRE 1 / golden 0)");
        assertEquals(1, count(gen, "MapperC.<String>ofNull()"),
                "the typed empty follows the bare join (PRE 0 / golden 1)");
        assertEquals(0, count(gen, "MapperC.<FieldWithMetaString>ofNull()"),
                "the wrapper ofNull must be gone (PRE 1 / golden 0)");
        assertEquals(1, count(gen, "fieldWithMetaString0 -> fieldWithMetaString0 == null "
                        + "? null : fieldWithMetaString0.getValue()"),
                "the MapperC.of arm derefs guarded, text-order numbered (PRE 0 / golden 1)");
        assertEquals(3, count(gen, ".<String>map(\"Type coercion\", "
                        + "fieldWithMetaString -> fieldWithMetaString.getValue());"),
                "the mapItem consumer tails deref BARE (PRE 0 / golden 3)");
        assertEquals(1, count(gen, ".<FloatingRateIndexEnum>map(\"Type coercion\", "
                        + "fieldWithMetaFloatingRateIndexEnum ->"),
                "the to-string receiver derefs before the map (PRE 0 / golden 1)");
        assertEquals(1, count(gen, ".mapItem(item -> MapperS.of("
                        + "getIndexIndicatorFromFloatingRate.evaluate(item.get())));"),
                "the downstream consumer simplifies to the direct form (PRE 0 / golden 1)");
    }

    /**
     * The join-bare trio (jfsa): the to-string coercion param NUMBERS within its own
     * name group (PRE 0 / golden 1 — the file registers two of the name); the decl +
     * typed empty follow the bare join (PRE 0 / golden 1 each).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void underlyingIndexIndicatorJfsa_numberedToString_witness() {
        String gen = drrPojoOutput.get(UII_JFSA);
        assertNotNull(gen, "jfsa UnderlyingIndexIndicatorRule not generated");
        assertEquals(1, count(gen, "final MapperC<String> thenArg2 = thenArg1"),
                "the pipe decl follows the bare join (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "MapperC.<String>ofNull()"),
                "the typed empty follows the bare join (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "fieldWithMetaFloatingRateIndexEnum0 -> "
                        + "fieldWithMetaFloatingRateIndexEnum0 == null"),
                "the to-string coercion numbers within its name group (PRE 0 / golden 1)");
    }

    /**
     * The join-bare trio (common Name): the multi-line list-literal collapse hoists
     * IN-RUNG as the deferred-token decl, numbering WITH the guarded coercion group
     * (fieldWithMetaString3 — PRE 0 / golden 1), and the guarded re-presentation
     * feeds the to-string map (PRE 0 / golden 1); negative — the collapsed
     * {@code .get().map("to-string", …)} form must be gone (PRE 1 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void nameCommon_collapseHoist_witness() {
        String gen = drrPojoOutput.get(NAME_COMMON);
        assertNotNull(gen, "common NameOfTheUnderlyingIndexRule not generated");
        assertEquals(1, count(gen, "final FieldWithMetaString fieldWithMetaString3 = "
                        + "MapperC.<FieldWithMetaString>of("),
                "the multi-line collapse hoists in-rung, group-numbered (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "(fieldWithMetaString3 == null ? MapperS.<String>ofNull() : "
                        + "MapperS.of(fieldWithMetaString3.getValue())).map(\"to-string\", "
                        + "Object::toString)"),
                "the guarded re-presentation feeds the to-string map (PRE 0 / golden 1)");
        assertEquals(0, count(gen, ".get().map(\"to-string\", Object::toString)"),
                "the bare collapsed to-string form must be gone (PRE 1 / golden 0)");
    }

    // ==== helpers ====

    /** Occurrence count — python {@code str.count} semantics (the #352 law). */
    private static int count(String haystack, String needle) {
        int n = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) >= 0) {
            n++;
            idx += needle.length();
        }
        return n;
    }

    private void assertByteMatchesGolden(Map<String, String> output, Path goldenDir, String path)
            throws IOException {
        assertNotNull(output, "cell output not generated");
        String gen = output.get(path);
        assertNotNull(gen, "missing generated output: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "missing golden: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(gen),
                path + " must byte-match the frozen 9.83.0 golden");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
