package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
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

/**
 * PR #397 — the closure-hoist/report-cardinality/wrapper-decl/setter-arg/cascade dectet
 * (10 byte flips: NewEquitySwapProduct [cdm6 FUNCTION — the cell EMPTIES, the THIRD
 * FUNCTION cell to zero] + BasketConstituentUnitOfMeasureRule v1/v2/v3 +
 * JurisdictionOfCounterparty1/2Rule + jfsa/hkma UnderlyingIdentificationRule [drr POJO]
 * + Create_AnnaDsbUpiRequestUnderlyingForCreditNonStandard +
 * Create_AnnaDsbUpiRequestBaseProductForCommodity [drr FUNCTION]).
 *
 * <p><b>conditionRungThenChainHoist</b> (FunctionExpressionRenderer): a condition-ladder
 * rung whose then-branch is a THEN-CHAIN compiles with a per-ladder statement-hoist sink
 * (tryDeepThenHoist registers the {@code final MapperC<Boolean> thenArg} decl in-rung) and
 * returns {@code ComparisonResult.ofNullSafe(<terminal>.asMapper());} — golden NESP's
 * in-closure form; direct-CR rungs never see the sink (the 18 green closure carriers).
 *
 * <p><b>reportOutputThenItemCardinality + reportOutputNavReceiverCardinality</b>
 * (rune-parser CardinalityComputer, thenAware-gated) — a then-body bare implicit reads the
 * piped argument's cardinality (the nearest-definer walk, upstream
 * safeIsClosureParameterMulti) and a feature call ORs its receiver (upstream
 * caseFeatureCall), so the report-output whole-signature class heals:
 * {@code ReportFunction<TransactionReportInstruction, List<String>>} + the
 * {@code List<String> output = new ArrayList<>();} init + the {@code getMulti()} terminal.
 * <b>reportOutputLoLCollapseStep</b> (CollectionHandler.mapMethod): a SINGLE body over the
 * piped mapItemToList list-of-lists takes upstream's {@code mapListToItem}.
 * <b>coerceLiteralIteArmToSingletonList</b>: a MULTI ladder's scalar-literal arm re-wraps
 * {@code MapperC.of(Collections.singletonList("NON-CA"))} (the JoC DSL-1064 workaround).
 *
 * <p><b>deepThenFirstCollapseElementPreserve</b> (CollectionHandler): the bare-item
 * FIRST/LAST collapse decl anchors to the RECEIVER's META wrapper
 * ({@code final MapperS<ReferenceWithMetaProductIdentifier> _thenArg1}) and the re-rooted
 * consumer inserts the standard Type-coercion deref (jfsa/hkma).
 *
 * <p><b>ctorSetterArgIteHoist</b> (FunctionExpressionRenderer + StatementHoistSession +
 * ControlFlowHandler): the pathed-SET constructor arm opens the FULL sink so setter-arg
 * conditionals hoist as in-branch initializer/blank-final ladders; the OUTER local is
 * mint/attach LATE-NAMED after the interiors (golden interiors 0..7, outer 8); a
 * bare-FUNCTION-call arm renders the RAW value (zero goldens carry
 * {@code .evaluate().get()}); the registrar opens the top/rung boolHoist
 * ({@code final Boolean boolean0 = qualify_…}).
 *
 * <p><b>ruleCascadeElselessBlock</b> (CollectionHandler): the DTCC mapSingleToList else-if
 * cascade renders sequential guard-returns — locked by the rewritten
 * {@code RuleIteHoistTest} pin (the #257 cascade-fallback's SIXTH repoint), not here
 * (DTCC is a content-verified TOWARD rider, not a flip).
 *
 * <p>4 whole-file byte locks through the REAL D11 routes (cdm6 FUNCTION / drr POJO ×2 /
 * drr FUNCTION) + 4 occurrence-counted witness methods — 24 tokens PRE-counted against
 * f-probe-396post, ALL PRE 0 / GOLD 1 (witness397.py; the #352 str.count law).
 */
class CascadeCardinalityCtorHoistDectetComposeTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String NESP =
            "cdm/event/common/functions/NewEquitySwapProduct.java";
    private static final String BCUOM1 =
            "drr/standards/iosco/cde/version1/basket/reports/BasketConstituentUnitOfMeasureRule.java";
    private static final String JOC1 =
            "drr/regulation/csa/rewrite/trade/reports/JurisdictionOfCounterparty1Rule.java";
    private static final String JFSA_UI =
            "drr/regulation/jfsa/rewrite/trade/reports/UnderlyingIdentificationRule.java";
    private static final String ADSB438 =
            "drr/enrichment/upi/functions/Create_AnnaDsbUpiRequestBaseProductForCommodity.java";
    private static final String ADSB69 =
            "drr/enrichment/upi/functions/Create_AnnaDsbUpiRequestUnderlyingForCreditNonStandard.java";

    private static Map<String, String> cdm6FnOutput;
    private static Map<String, String> drrFnOutput;
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
            drrFnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
            drrPojoOutput = generatePojoCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    /**
     * The REAL D11 FUNCTION route (loadCellCorpusCached + emissionFilter +
     * FunctionGenerator.generateWithErrors). Tolerated generation errors are counted +
     * printed so a downstream missing-output assertion keeps its root cause visible
     * (the Copilot #395 R1 convention).
     */
    private static Map<String, String> generateFunctions(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var funcGen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        var errors = funcGen.generateWithErrors(output);
        if (!errors.isEmpty()) {
            System.err.println("[" + cell.corpus() + "-" + cell.version()
                    + " FUNCTION] generation errors tolerated (D11 parity): " + errors.size());
        }
        return output;
    }

    /**
     * The REAL D11 POJO route (the #395 anchor's generatePojoCell — pojo/choice/rule/
     * report/labelProvider generators over the shouldGenerate models, readDoNotPrune
     * threaded; a no-op for the drr cell, only iso20022 carries entries).
     */
    private static Map<String, String> generatePojoCell(D11CorpusRegressionTest.CellSpec cell)
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
        int generationErrors = 0;
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                generationErrors += pojoGen.generateClasses(model, version, output).size();
                generationErrors += choiceGen.generateClasses(model, version, output).size();
                generationErrors += ruleGen.generateClasses(model, version, output).size();
                generationErrors += reportGen.generateClasses(model, version, output).size();
                generationErrors += labelProviderGen.generateClasses(model, version, output).size();
            }
        }
        if (generationErrors > 0) {
            System.err.println("[" + cell.corpus() + "-" + cell.version()
                    + " POJO] generation errors tolerated (D11 parity): " + generationErrors);
        }
        return output;
    }

    // ==== byte locks (one carrier per facet through the REAL D11 routes) ====

    /** the condition-rung then-chain hoist carrier (the cdm6 FUNCTION cell empties). */
    @Test
    @EnabledIf("cellsAvailable")
    void newEquitySwapProduct_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, NESP);
    }

    /** the receiver-OR cardinality + mapListToItem collapse carrier (the whole-signature class). */
    @Test
    @EnabledIf("cellsAvailable")
    void basketConstituentUnitOfMeasureV1_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrPojoOutput, DRR_GOLDEN_DIR, BCUOM1);
    }

    /** the then-if bare-item cardinality + singleton-literal ladder arm carrier (DSL-1064). */
    @Test
    @EnabledIf("cellsAvailable")
    void jurisdictionOfCounterparty1_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrPojoOutput, DRR_GOLDEN_DIR, JOC1);
    }

    /** the setter-arg conditional hoist + late-name + raw-fn-arm carrier (the 438-line monster). */
    @Test
    @EnabledIf("cellsAvailable")
    void annaDsbBaseProductForCommodity_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, ADSB438);
    }

    // ==== occurrence-counted witnesses (python str.count semantics — the #352 law) ====

    /**
     * conditionRungThenChainHoist (cdm6 NESP): the in-rung thenArg decl, the
     * ofNullSafe(.asMapper()) terminal, the ofEmpty fallthrough + the ComparisonResult
     * import the block adds (witness397.py PRE 0 / GOLD 1 ×4).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void newEquitySwapProduct_conditionRungThenChain_witness() {
        String gen = cdm6FnOutput.get(NESP);
        assertNotNull(gen, "NESP not generated");
        assertEquals(1, count(gen, "final MapperC<Boolean> thenArg = MapperS.of(product)"),
                "the in-rung tryDeepThenHoist decl (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                "return ComparisonResult.ofNullSafe(areEqual(thenArg, MapperS.of(true), "
                + "CardinalityOperator.All).asMapper());"),
                "the CR-wrapped .asMapper() rung terminal (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "return ComparisonResult.ofEmpty();"),
                "the ladder's elseless fallthrough (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "import com.rosetta.model.lib.expression.ComparisonResult;"),
                "the ComparisonResult import rides the block (PRE 0 / golden 1)");
    }

    /**
     * reportOutputThenItemCardinality + receiver-OR + mapListToItem + the singleton
     * literal arm (drr POJO BCUoM v1 + JoC1): the List signature triple + the LoL
     * collapse + the MapperC ladder with the coerced literal + the getMulti terminal.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void reportCardinalityQuintet_witness() {
        String bc = drrPojoOutput.get(BCUOM1);
        assertNotNull(bc, "BCUoM v1 not generated");
        assertEquals(1, count(bc, "implements ReportFunction<TransactionReportInstruction, List<String>>"),
                "the whole-signature List type-arg (PRE 0 / golden 1)");
        assertEquals(1, count(bc, "List<String> output = new ArrayList<>();"),
                "the ArrayList output init (PRE 0 / golden 1)");
        assertEquals(1, count(bc, "protected abstract List<String> doEvaluate(TransactionReportInstruction input);"),
                "the abstract doEvaluate List signature (PRE 0 / golden 1)");
        assertEquals(1, count(bc, ".mapListToItem(item -> {"),
                "the LoL/single mapListToItem collapse (PRE 0 / golden 1)");
        String joc = drrPojoOutput.get(JOC1);
        assertNotNull(joc, "JoC1 not generated");
        assertEquals(1, count(joc, "final MapperC<String> ifThenElseResult;"),
                "the MULTI ladder decl (PRE 0 / golden 1)");
        assertEquals(1, count(joc, "ifThenElseResult = MapperC.of(Collections.singletonList(\"NON-CA\"));"),
                "the literal arm's singleton coercion (PRE 0 / golden 1)");
        assertEquals(1, count(joc, "output = ifThenElseResult.getMulti();"),
                "the getMulti terminal (PRE 0 / golden 1)");
        assertEquals(1, count(joc, "import java.util.Collections;"),
                "the Collections import rides the coercion (PRE 0 / golden 1)");
    }

    /**
     * deepThenFirstCollapseElementPreserve (drr POJO jfsa): the wrapper-typed FIRST
     * collapse decls + the consumer-side numbered Type-coercion deref.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void jfsaUnderlyingIdentification_wrapperDecl_witness() {
        String gen = drrPojoOutput.get(JFSA_UI);
        assertNotNull(gen, "jfsa UnderlyingIdentificationRule not generated");
        assertEquals(1, count(gen, "final MapperS<ReferenceWithMetaProductIdentifier> _thenArg1 = _thenArg0"),
                "the wrapper-typed first-collapse decl (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final MapperS<ReferenceWithMetaProductIdentifier> thenArg3 = _thenArg2"),
                "the sibling wrapper-typed decl (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                ".<ProductIdentifier>map(\"Type coercion\", referenceWithMetaProductIdentifier0 -> "
                + "referenceWithMetaProductIdentifier0 == null ? null : "
                + "referenceWithMetaProductIdentifier0.getValue()).<FieldWithMetaString>map(\"getIdentifier\""),
                "the consumer-side numbered guarded deref (PRE 0 / golden 1)");
    }

    /**
     * ctorSetterArgIteHoist (drr F AnnaDsb 438 + 69): the late-named outers (8/18 after
     * the interiors), the RAW bare-fn arms, the bare-local setter splice, the boolHoisted
     * guard and the elseful blank-final interior pair.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void annaDsbSetterArgHoist_witness() {
        String big = drrFnOutput.get(ADSB438);
        assertNotNull(big, "AnnaDsb BaseProductForCommodity not generated");
        assertEquals(1, count(big, "AnnaDsbNRGY ifThenElseResult8 = null;"),
                "the FIRST outer late-names AFTER its 0..7 interiors (PRE 0 / golden 1)");
        assertEquals(1, count(big, "AnnaDsbAGRI ifThenElseResult18 = null;"),
                "the SECOND outer continues the method group after 9..17 (PRE 0 / golden 1)");
        assertEquals(1, count(big, "ifThenElseResult3 = create_AnnaDsbEmpty.evaluate();"),
                "the RAW bare-FUNCTION ladder arm (no .get() — PRE 0 / golden 1)");
        assertEquals(1, count(big, "ifThenElseResult50 = create_AnnaDsbEmpty.evaluate();"),
                "the RAW whole-arm bare-FUNCTION call at the pathed seat (PRE 0 / golden 1)");
        assertEquals(1, count(big, ".setELEC(ifThenElseResult0)"),
                "the bare-local setter splice (PRE 0 / golden 1)");
        String cred = drrFnOutput.get(ADSB69);
        assertNotNull(cred, "AnnaDsb UnderlyingForCreditNonStandard not generated");
        assertEquals(1, count(cred,
                "final Boolean boolean0 = qualify_CreditDefaultSwap_Index.evaluate(economicTerms(product).get());"),
                "the registrar-opened guard boolHoist (PRE 0 / golden 1)");
        assertEquals(1, count(cred, "final AnnaDsbUnderlierIDSourceEnum ifThenElseResult0;"),
                "the elseful blank-final interior ladder decl (PRE 0 / golden 1)");
        assertEquals(1, count(cred, "final String ifThenElseResult1;"),
                "the sibling interior ladder decl (PRE 0 / golden 1)");
        assertEquals(1, count(cred, ".setUnderlierIDSource(ifThenElseResult0)"),
                "the interior local's setter splice (PRE 0 / golden 1)");
    }

    // ==== support ====

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String path) throws IOException {
        assertNotNull(output, "generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "generated output must byte-match the golden (newline-normalized) for " + path);
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        int i = haystack.indexOf(needle);
        while (i != -1) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
