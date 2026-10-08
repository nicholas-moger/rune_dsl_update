package com.regnosys.rosetta.generator.java;

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
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;

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
 * PR #388 — the rule-cond-base / alias-type-switch trio (3 byte flips:
 * CountryAndProvinceOrTerritoryOfIndividualRule [drr POJO csa Rule] +
 * GetUnitTypeForUnderlyingAsset + MapAssetToObservableWithLocation [cdm6 FUNCTION]).
 *
 * <p><b>ruleCondBaseEvalArgDeref</b> (ReferenceHandler + ControlFlowHandler +
 * CollectionHandler): three composed mechanisms at the #374 cond-at-base ite-hoist
 * seat — (i) the evaluate-arg meta-hoist at the RULE in-lambda cond/arm seats (a
 * {@code deepThenIteCondSeat} admission node-identity-keyed on the #351
 * {@code DeepThenIteHoist} handshake cond — the (a)-arm LAMBDA_CHANNEL class the
 * #363 note left declined until a golden carrier; the cond-position
 * {@code ItemGetMetaDerefHoist} drains at lambda top, the ARM-position one INTO the
 * owning branch, and a NAV-RECEIVER deref'd call keeps the flat route's wrap via the
 * shared {@code wrapNavReceiverInvocation}); (ii) the nav-only-element bare-collapse
 * re-wrap at the ite-arm assign seat ({@code wrapDeepThenIteArm} — the trailing
 * {@code .get()} is the render arbiter, a balanced wrap ends {@code )}); (iii) the
 * disguised item-feature bare-sibling evidence at the effective-else seat (the bare
 * symbol-EMPTY {@code state} re-runs the render's own
 * {@code synthesizeImplicitItemBareNav} walk — a RESOLVED meta-free attribute is
 * POSITIVE bare evidence, not the #295 null-walk trap — beside the compiled
 * wrapper-of-the-join {@code country} arm, so the else return derefs in place and
 * the coercion params renumber 0/1 in text order).
 *
 * <p><b>aliasTypeSwitchCtorCondArm</b> (FunctionAliasHelper +
 * FunctionExpressionRenderer): two arm-class widenings of the #365/#369/#382 alias
 * type-switch RETURN ladder — a CTOR case result joins by the CONSTRUCTED type (the
 * #358 law; MapAsset's Observable ctor arms ride the existing bare re-wrap +
 * multi-line re-anchor) and a CONDITIONAL case result joins by its ARM join and
 * renders as the INNER if/return ladder inside the case block (the #183
 * {@code appendReturnLadder} at firstRung=false on the case scope, the bound switch
 * subject re-rooting the narrowed reads; GetUnitType's signature retypes from the
 * {@code ? extends UnitType} fallback to {@code MapperS<FinancialUnitEnum>} — the
 * #353 signature/renderer lockstep law).
 *
 * <p>Whole-file byte locks run through the REAL D11 routes (drr full cell + cdm6
 * FUNCTION) and revert RED without the facets; every witness token is
 * occurrence-counted (python {@code str.count} semantics — the #352 law) and
 * PRE-counted against f-probe-387post (each removal token PRE &ge; 1 / golden 0;
 * each golden token PRE 0 / golden &ge; 1 — witness388.py).
 */
class RuleCondBaseAliasTypeSwitchTrioComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String CAP =
            "drr/regulation/csa/rewrite/trade/reports/CountryAndProvinceOrTerritoryOfIndividualRule.java";
    private static final String GET_UNIT_TYPE =
            "cdm/ingest/fpml/confirmation/common/functions/GetUnitTypeForUnderlyingAsset.java";
    private static final String MAP_ASSET =
            "cdm/ingest/fpml/confirmation/pricequantity/functions/MapAssetToObservableWithLocation.java";
    private static final String MAP_BASKET =
            "cdm/ingest/fpml/confirmation/pricequantity/functions/MapBasketConstituentWithLocation.java";

    private static Map<String, String> drrPojoOutput;
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
            cdm6FnOutput = generateFunctions(cdm6Spec);
        }
    }

    /**
     * The D11-shaped POJO/rule generation path — generator wiring identical to
     * {@code D11CorpusRegressionTest}'s cell loop (same emission filter, same
     * {@code generators.doNotPrune} configuration, same generator classes and
     * order). The harness-side generator-error capture and the standalone
     * function-file emission are deliberately omitted: this helper serves only
     * the rule byte lock, whose render is byte-verified against golden.
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

    // ==== byte locks (all 3 flips through the REAL D11 routes) ====

    /** ruleCondBaseEvalArgDeref: the three-mechanism composite carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void countryAndProvinceOrTerritoryOfIndividual_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrPojoOutput, DRR_GOLDEN_DIR, CAP);
    }

    /** aliasTypeSwitchCtorCondArm: the CONDITIONAL-arm + signature-retype carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void getUnitTypeForUnderlyingAsset_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, GET_UNIT_TYPE);
    }

    /** aliasTypeSwitchCtorCondArm: the CTOR-arm carrier (2 cases + ctor default). */
    @Test
    @EnabledIf("cellsAvailable")
    void mapAssetToObservableWithLocation_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_ASSET);
    }

    // ==== occurrence-counted witnesses (python str.count semantics — the #352 law) ====

    /**
     * ruleCondBaseEvalArgDeref: the cond-position hoist lands at LAMBDA TOP
     * (referenceWithMetaParty0 — PRE 0 / golden 1), the arm-position hoist INSIDE
     * the then branch (referenceWithMetaParty1 — PRE 0 / golden 1), the arm re-wraps
     * {@code MapperS.of(MapperS.of(…)…get())} (PRE 0 / golden 1), the else return
     * derefs guarded with the TEXT-ORDER-renumbered coercion group
     * (fieldWithMetaString0/1 — PRE 0 / golden 1 each); negative — the raw
     * {@code evaluate(item.get())} form is gone (PRE 2 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void countryAndProvince_condBaseDerefs_witness() {
        String gen = drrPojoOutput.get(CAP);
        assertNotNull(gen, "csa CountryAndProvinceOrTerritoryOfIndividualRule not generated");
        assertEquals(1, count(gen, "final ReferenceWithMetaParty referenceWithMetaParty0 = item.get();"),
                "the cond-position hoist lands at lambda top (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final ReferenceWithMetaParty referenceWithMetaParty1 = item.get();"),
                "the arm-position hoist lands inside the then branch (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "thenArg = MapperS.of(MapperS.of("
                        + "naturalPersonBuyerOrSeller.evaluate((referenceWithMetaParty1"),
                "the nav-only-element arm re-wraps MapperS.of(...) (PRE 0 / golden 1)");
        assertEquals(1, count(gen, ".<String>map(\"Type coercion\", fieldWithMetaString1 -> "
                        + "fieldWithMetaString1 == null ? null : fieldWithMetaString1.getValue());"),
                "the else return derefs guarded (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "fieldWithMetaString0 -> fieldWithMetaString0 == null"),
                "the condition's coercion renumbers 0 in text order (PRE 0 / golden 1)");
        assertEquals(0, count(gen, "naturalPersonBuyerOrSeller.evaluate(item.get())"),
                "the raw wrapper-into-value arg form must be gone (PRE 2 / golden 0)");
    }

    /**
     * aliasTypeSwitchCtorCondArm (GetUnitType): the signature retypes to the arm
     * join (PRE 0 / golden 1), the instanceof arm + narrowed local land (PRE 0 /
     * golden 1 each), the CONDITIONAL arm renders the inner if/return ladder over
     * the narrowed subject ({@code MapperS.of(index)…} with the {@code _index}
     * self-shadow escape — PRE 0 / golden 1) with the typed ofNull terminal ×2
     * (null guard + inner empty — PRE 0 / golden 2), and the enum default wraps
     * (PRE 0 / golden 1); negative — the {@code Objects.equals(fpml.Index} ternary
     * junk is gone (PRE 1 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void getUnitType_condArmLadder_witness() {
        String gen = cdm6FnOutput.get(GET_UNIT_TYPE);
        assertNotNull(gen, "GetUnitTypeForUnderlyingAsset not generated");
        assertEquals(1, count(gen, "protected MapperS<FinancialUnitEnum> financialUnit(Asset fpmlUnderlyingAsset)"),
                "the override signature retypes to the arm join (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "if (fpmlUnderlyingAsset instanceof Index) {"),
                "the instanceof arm lands (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final Index index = (Index) fpmlUnderlyingAsset;"),
                "the narrowed case local lands (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "exists(MapperS.of(index).<InstrumentId>mapC(\"getInstrumentId\", "
                        + "_index -> _index.getInstrumentId()))"),
                "the inner condition re-roots on the narrowed subject (PRE 0 / golden 1)");
        assertEquals(2, count(gen, "return MapperS.<FinancialUnitEnum>ofNull();"),
                "the typed ofNull lands at the null guard AND the inner-ladder empty (PRE 0 / golden 2)");
        assertEquals(1, count(gen, "return MapperS.of(FinancialUnitEnum.SHARE);"),
                "the enum default wraps (PRE 0 / golden 1)");
        assertEquals(0, count(gen, "Objects.equals(fpml.Index"),
                "the value-switch ternary junk must be gone (PRE 1 / golden 0)");
    }

    /**
     * aliasTypeSwitchCtorCondArm (MapAsset): both instanceof arms + narrowed locals
     * land (PRE 0 / golden 1 each), the ctor arms' fn-call args read the NARROWED
     * locals through the bound subject (PRE 0 / golden 1 each), and the null guard
     * types (PRE 0 / golden 1); negative — the {@code Objects.equals(fpml.Basket}
     * junk is gone (PRE 1 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void mapAsset_ctorArms_witness() {
        String gen = cdm6FnOutput.get(MAP_ASSET);
        assertNotNull(gen, "MapAssetToObservableWithLocation not generated");
        assertEquals(1, count(gen, "if (fpmlAsset instanceof Basket) {"),
                "the Basket instanceof arm lands (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final Basket basket = (Basket) fpmlAsset;"),
                "the narrowed Basket local lands (PRE 0 / golden 1)");
        assertEquals(1, count(gen, ".setBasket(mapBasket.evaluate(basket))"),
                "the ctor arg reads the narrowed local bare (PRE 0 / golden 1)");
        assertEquals(1, count(gen, ".setIndex(mapEquityIndex.evaluate(index))"),
                "the Index arm's ctor arg reads its narrowed local (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "return MapperS.<Observable>ofNull();"),
                "the null guard types from the ctor join (PRE 0 / golden 1)");
        assertEquals(0, count(gen, "Objects.equals(fpml.Basket"),
                "the value-switch ternary junk must be gone (PRE 1 / golden 0)");
    }

    /**
     * MapBasketConstituentWithLocation — GRADUATED to a whole-file byte lock at
     * PR #389 (the graduation pattern; the #387 NameOfTheUnderlyingIndex
     * precedent): this lock's #388 partial-heal javadoc NAMED its residual
     * mechanisms — (i) the collision-QUALIFIED ctor head + typed ofNull, (ii) the
     * alias-valued LIST-field value-setter, (iii) the explicit-arg alias deref +
     * the ctor multi-line form — and the #389 facets ctorChoiceSuperAttrs +
     * ladderElementCollisionQualify landed exactly those (see
     * CtorAliasChainQuintetComposeTest for the per-mechanism witnesses).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void mapBasketConstituent_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_BASKET);
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
