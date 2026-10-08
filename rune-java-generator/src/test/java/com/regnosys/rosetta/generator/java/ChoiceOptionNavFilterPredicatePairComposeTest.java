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
 * PR #394 — the choice-option-nav / elseless-filter-predicate pair (2 byte flips:
 * PTRRIDRule drr POJO + CriteriaMatchesAssetType cdm6 FUNCTION; CheckCriteria rides
 * TOWARD dl 92 → 14).
 *
 * <p><b>ruleFilterPredicateElselessNestedAdmit + filterPredArmComparandCollapseHoist</b>
 * (CollectionHandler + ComparisonHandler + FunctionExpressionRenderer): the #393-banked
 * PTRRID decode shipped whole — the #365 filter-predicate block widens to the ELSELESS
 * class (incl. the parser-materialized empty-RListLiteral else) with the bare
 * {@code return null;} terminal and ONE nested elseFUL conditional arm (early-return
 * form); a {@code .get()}-collapsed REFERENCE_WITH_META comparand hoists in-branch
 * through the filterPredArm window ({@code final ReferenceWithMetaParty
 * referenceWithMetaParty1 = <collapse>;} + the guarded parenthesized reconstruct — the
 * maxMin pattern at the comparison-operand seat, interleaved param0/local1/param2/local3
 * per-lambda numbering); the ctl-scan filter-body admit + the isBodyMulti rule-path
 * then-chain arm + the bare-closure-param base FUSION restructure the named-param
 * {@code then extract eventIdentifier […]} step into golden's
 * mapItemToList/MapperListOfLists/{@code _thenArg} block + {@code .flattenList()}; the
 * whole-output meta-deref tail rides the #392 render-truth arm widened with the
 * extract-base nested-chain disjunct.
 *
 * <p><b>choiceOptionNavAssignLadder</b> (FunctionExpressionRenderer +
 * NavigationHandler): a RUNE CHOICE subject ({@code RChoiceTypeRef}) renders the
 * OPTION-NAV ladder at the SET seat — the {@code final MapperS<CollateralCriteria>
 * switchArgument = MapperS.of(inputCriteria);} hoist, per-case
 * {@code switchArgument.<X>map("getX", …).get() != null} rungs, Mapper-typed case
 * locals with deferred-name registration ({@code _assetType} vs the fn input;
 * interior params escalate {@code __assetType}), REVERSE-order arm compiles
 * (upstream's right-fold: Any→{@code thenArg0} / All→{@code thenArg1}) with in-arm
 * statement-hoist drains, wrap-factoried arms assigned bare, Mapper/CR arms collapsed
 * {@code .get()}; {@code switchCaseNarrowedBareSymbolProvesMulti} reads a
 * case-narrowed bare feature's cardinality off the guard type (AST-static). Every
 * EXTENDS-based subject keeps the instanceof branch byte-frozen.
 *
 * <p>Whole-file byte locks run through the REAL D11 routes (POJO/Rule: drr;
 * FUNCTION: cdm6) and revert RED without the facets; every witness token is
 * occurrence-counted (python {@code str.count} semantics — the #352 law) and
 * PRE-counted against f-probe-393post (witness394.py).
 */
class ChoiceOptionNavFilterPredicatePairComposeTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String PTRRID =
            "drr/regulation/common/trade/party/reports/PTRRIDRule.java";
    private static final String CMAT =
            "cdm/product/collateral/functions/CriteriaMatchesAssetType.java";

    private static Map<String, String> cdm6FnOutput;
    private static Map<String, String> drrPojoOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR)
                && Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            cdm6FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
            drrPojoOutput = generatePojoCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
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

    /**
     * The REAL D11 POJO-cell generation path (Rule/Report/LabelProvider included) —
     * mirrors {@code D11CorpusRegressionTest#pojo_comparison}'s wiring (the
     * RuleIteHoistTest harness).
     */
    private static Map<String, String> generatePojoCell(D11CorpusRegressionTest.CellSpec cell)
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
        return output;
    }

    // ==== byte locks (both flips through the REAL D11 routes) ====

    /** the elseless-filter-predicate / comparand-hoist / mapItemToList-block carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void ptrridRule_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrPojoOutput, DRR_GOLDEN_DIR, PTRRID);
    }

    /** the choice-option-nav ASSIGN ladder carrier (the README worked example). */
    @Test
    @EnabledIf("cellsAvailable")
    void criteriaMatchesAssetType_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, CMAT);
    }

    // ==== occurrence-counted witnesses (python str.count semantics — the #352 law) ====

    /**
     * ruleFilterPredicateElselessNestedAdmit (drr PTRRIDRule): the elseless block's
     * bare {@code return null;} terminal + the in-branch comparand collapse hoists +
     * the guarded reconstruct (PRE 0 / golden 1 each — witness394.py); negative —
     * the inline runtime {@code .then(item -> item} forms are gone (PRE 4 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void ptrridRule_elselessFilterComparandHoist_witness() {
        String gen = drrPojoOutput.get(PTRRID);
        assertNotNull(gen, "PTRRIDRule not generated");
        assertEquals(1, count(gen, "return null;"),
                "the elseless predicate's bare null terminal (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                "final ReferenceWithMetaParty referenceWithMetaParty1 = distinct(MapperC.<PartyRole>of(filterPartyRole.evaluate("),
                "the arm-1 comparand collapse hoists in-branch (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                "final ReferenceWithMetaParty referenceWithMetaParty3 = distinct(MapperC.<RelatedParty>of(filterRelatedPartyByRole.evaluate("),
                "the arm-2 comparand collapse hoists in-branch — the interleaved "
                + "param0/local1/param2/local3 numbering (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                "(referenceWithMetaParty1 == null ? MapperS.<Party>ofNull() : MapperS.of(referenceWithMetaParty1.getValue()))"),
                "the guarded parenthesized comparand reconstruct (PRE 0 / golden 1)");
        assertEquals(0, count(gen, ".then(item -> item"),
                "the inline runtime then forms are gone (PRE 4 / golden 0)");
    }

    /**
     * the named-param extract block decomposition (drr PTRRIDRule): the
     * mapItemToList block + the MapperListOfLists decl + the fused {@code _thenArg}
     * singleton + {@code .flattenList()} + the whole-output meta-deref tail
     * (PRE 0 / golden 1 each); negative — the mapItem-over-MapperS.of inline form
     * is gone (PRE 1 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void ptrridRule_mapItemToListBlock_witness() {
        String gen = drrPojoOutput.get(PTRRID);
        assertNotNull(gen, "PTRRIDRule not generated");
        assertEquals(1, count(gen, ".mapItemToList(eventIdentifier -> {"),
                "the named-param extract renders the mapItemToList block (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final MapperListOfLists<FieldWithMetaString> thenArg1 = thenArg0"),
                "the list-of-lists decl off the nav local (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final MapperS<Identifier> _thenArg = eventIdentifier"),
                "the bare-param base FUSES into the filter receiver — the escaped "
                + "in-lambda singleton (PRE 0 / golden 1)");
        assertEquals(1, count(gen, ".flattenList();"),
                "the flatten consumer re-roots on the LoL local (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                "final FieldWithMetaString fieldWithMetaString = MapperS.of(thenArg.get()).get();"),
                "the whole-output meta-deref tail rides (PRE 0 / golden 1)");
        assertEquals(0, count(gen, ".mapItem(eventIdentifier -> MapperS.of(eventIdentifier)"),
                "the inline mapItem-over-rewrapped-param form is gone (PRE 1 / golden 0)");
    }

    /**
     * choiceOptionNavAssignLadder (cdm6 CriteriaMatchesAssetType): the Mapper-typed
     * switchArgument hoist + the null rung + the option-nav rung + the escaped case
     * local + the double-escaped interior param (PRE 0 / golden 1 each); negatives —
     * the instanceof rung and the cast local are gone (PRE 1 / golden 0 each).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void criteriaMatchesAssetType_optionNavLadder_witness() {
        String gen = cdm6FnOutput.get(CMAT);
        assertNotNull(gen, "CriteriaMatchesAssetType not generated");
        assertEquals(1, count(gen,
                "final MapperS<CollateralCriteria> switchArgument = MapperS.of(inputCriteria);"),
                "the Mapper-typed subject hoist (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "if (switchArgument.get() == null) {"),
                "the null rung reads the hoisted Mapper (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                "} else if (switchArgument.<AssetType>map(\"getAssetType\", collateralCriteria -> collateralCriteria.getAssetType()).get() != null) {"),
                "the option-nav rung replaces instanceof (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final MapperS<AssetType> _assetType = switchArgument"),
                "the case local escapes the fn-input collision (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "__assetType -> __assetType.getSecurityType()"),
                "the interior nav param double-escapes — the n2DoubleUnderscore "
                + "family (PRE 0 / golden 1)");
        assertEquals(0, count(gen, " instanceof AssetType) {"),
                "the instanceof rung is gone (PRE 1 / golden 0)");
        assertEquals(0, count(gen, " = (AssetType) inputCriteria;"),
                "the cast case local is gone (PRE 1 / golden 0)");
    }

    /**
     * the REVERSE-order arm-chain numbering + the arm collapses (cdm6 CMAT): the
     * All arm's chain hoists {@code thenArg1} and the TEXT-LATER Any arm's
     * {@code thenArg0} (upstream's right-fold — PRE 0 / golden 1 each); the piped
     * comparison arms collapse {@code .asMapper().get()} (PRE 0 / golden 2).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void criteriaMatchesAssetType_reverseArmNumbering_witness() {
        String gen = cdm6FnOutput.get(CMAT);
        assertNotNull(gen, "CriteriaMatchesAssetType not generated");
        assertEquals(1, count(gen, "final MapperC<Boolean> thenArg1 = allCriteria"),
                "the TEXT-EARLIER All arm numbers thenArg1 (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final MapperC<Boolean> thenArg0 = anyCriteria"),
                "the TEXT-LATER Any arm numbers thenArg0 — the reverse-fold "
                + "registration order (PRE 0 / golden 1)");
        assertEquals(2, count(gen, ".asMapper().get();"),
                "the piped comparison arms collapse at the Boolean target "
                + "(PRE 0 / golden 2)");
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
