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

/**
 * PR #395 — the case-narrowed-witness / then-flatten-LoL / FX-interior-decomposition
 * septet (7 byte flips: CheckCriteria cdm6 FUNCTION + GetReportableDelivery +
 * ExtractFinalContractualSettlementDate drr FUNCTION + esma/fca ExchangeRateRule +
 * esma/fca ForwardExchangeRateRule drr POJO).
 *
 * <p><b>switchCaseNarrowedLeafWitness</b> (NavigationHandler): a feature call whose
 * receiver is the IMPLICIT ITEM inside a switch-case expression resolves as a feature
 * of the case guard's NARROWED type (AST-static — the #394 cardinality sibling's
 * WITNESS/type seat), consult-seat-scoped at the {@code handle(RFeatureCall)}
 * {@code resolvedAttr} null-fallback with a stop-at-{@code RInlineFunction} walk (the
 * Seat-1 #394 OBS-2 note applied at birth) — golden CheckCriteria's
 * {@code .<ISOCountryCodeEnum>map(…)} / {@code .<AgencyRatingCriteria>map(…)}
 * disguised-leaf witnesses + their 2 imports.
 *
 * <p><b>fnThenFlattenLoLDecomp</b> (CollectionHandler + ControlFlowHandler +
 * FunctionExpressionRenderer): the GRD three-family FUNCTION-path restructure — the
 * ADD-seat conditional distribution admits hoistable+ctl-clean THEN-CHAIN arms (the
 * in-branch {@code final MapperListOfLists<ReportableDelivery> thenArg} decl +
 * {@code thenArg\n.flattenList().getMulti()}); the ctl-scan FUNCTION exemptions
 * (nested hoistable then-chains + fn-call extracts with elseless-LADDER args) + the
 * isBodyMulti FUNCTION twins select mapItemToList/mapSingleToList block lambdas with
 * in-lambda {@code _thenArg} LoL singletons; the fn-call ARG seat types NAV-CHAIN and
 * disguised then-arms from the leaf attribute (the #365 walk at the #392 seat), so
 * the elseful evaluate-arg blank-final {@code ifThenElseResult0/1/2} ladders with
 * in-arm {@code fieldWithMetaString0/1} guarded derefs hoist through the #355/#368
 * machinery unchanged.
 *
 * <p><b>ruleInteriorBoundChainDecomp</b> (CollectionHandler + LiteralHandler): a
 * RULE-path chain inside a lambda whose EVERY then-body ancestor is thenArg-BOUND
 * compiles under a LIVE outer restructure, so the #219/#250 suppressions lift and
 * the chain decomposes per-level through the lambda channel (the ER twins'
 * {@code _thenArg0.._thenArg4} in-lambda decls); the rule-bound ctl-scan variant
 * admits nested-then bases path-blind under the SAME exempt predicate; a base
 * containing a same-boundary hoistable nested chain late-creates its decl identifier
 * (the FER {@code thenArg6} decl numbering AFTER the interior 0..5); the #362
 * consumer stamp widens to FILTER bodies (meta-only) and the list-literal element
 * seat coerces a MapperC-kinded META element at the joined bare item type through
 * the TypeCoercionService (the #310 bare {@code .map("Type coercion", …getValue())}
 * deref). A conditional/switch ancestor before the first lambda boundary declines
 * (the cp6 DTCC ternary-arm AWAY catch — DTCC_UAR byte-frozen).
 *
 * <p>Whole-file byte locks run through the REAL D11 routes (FUNCTION: cdm6 + drr;
 * POJO/Rule: drr) and revert RED without the facets; every witness token is
 * occurrence-counted (python {@code str.count} semantics — the #352 law) and
 * PRE-counted against f-probe-394post (witness395.py: 36 tokens, all PRE≠GOLD).
 */
class ThenFlattenInteriorDecompSeptetComposeTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String CHECK_CRITERIA =
            "cdm/product/collateral/functions/CheckCriteria.java";
    private static final String GRD =
            "drr/regulation/common/functions/GetReportableDelivery.java";
    private static final String FCA_ER =
            "drr/regulation/fca/ukemir/refit/trade/reports/ExchangeRateRule.java";
    private static final String FCA_FER =
            "drr/regulation/fca/ukemir/refit/trade/reports/ForwardExchangeRateRule.java";

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
     * The REAL D11 FUNCTION-cell generation path (readDoNotPrune deliberately
     * omitted — correct for function cells, the Seat-1 #386 OBS-5 note). Per-file
     * generation errors are tolerated exactly as the D11 route tolerates them
     * (generateWithErrors continues past waivered files); a summary is printed so
     * a downstream missing-output assertion keeps its root cause visible
     * (Copilot #395 R1).
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
     * The REAL D11 POJO-cell generation path (Rule/Report/LabelProvider included) —
     * mirrors {@code D11CorpusRegressionTest#pojo_comparison}'s wiring (the
     * RuleIteHoistTest harness), INCLUDING the cell's {@code readDoNotPrune} config
     * (Copilot #395 R1 — a no-op for the drr/cdm cells, which carry no
     * {@code generators.doNotPrune} entries; only iso20022 does today). Per-model
     * generation errors are tolerated exactly as the D11 route tolerates them; a
     * summary is printed so a downstream missing-output assertion keeps its root
     * cause visible.
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

    /** the case-narrowed disguised-leaf witness carrier (the README worked example). */
    @Test
    @EnabledIf("cellsAvailable")
    void checkCriteria_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, CHECK_CRITERIA);
    }

    /** the three-family FUNCTION-path then-flatten-LoL decomposition carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void getReportableDelivery_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, GRD);
    }

    /** the rule-interior decomposition + literal-element-deref + late-name carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void fcaForwardExchangeRateRule_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrPojoOutput, DRR_GOLDEN_DIR, FCA_FER);
    }

    // ==== occurrence-counted witnesses (python str.count semantics — the #352 law) ====

    /**
     * switchCaseNarrowedLeafWitness (cdm6 CheckCriteria): the 5 disguised-leaf type
     * witnesses (ISOCountryCodeEnum ×1 shown + AgencyRatingCriteria ×3) + the 2
     * recovered imports (PRE 0 / golden 1 each — witness395.py).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void checkCriteria_caseNarrowedLeafWitness_witness() {
        String gen = cdm6FnOutput.get(CHECK_CRITERIA);
        assertNotNull(gen, "CheckCriteria not generated");
        assertEquals(1, count(gen, "import cdm.base.staticdata.asset.common.ISOCountryCodeEnum;"),
                "the enum-leaf witness import recovers (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "import cdm.product.collateral.AgencyRatingCriteria;"),
                "the data-leaf witness import recovers (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                ".<ISOCountryCodeEnum>map(\"getIssuerCountryOfOrigin\", _issuerCountryOfOrigin -> _issuerCountryOfOrigin.getIssuerCountryOfOrigin())"),
                "the same-named disguised leaf types off the guard (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                ".<AgencyRatingCriteria>map(\"getIssuerAgencyRating\", _issuerAgencyRating -> _issuerAgencyRating.getIssuerAgencyRating())"),
                "the issuer agency-rating leaf witness (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                ".<AgencyRatingCriteria>map(\"getSovereignAgencyRating\", _sovereignAgencyRating -> _sovereignAgencyRating.getSovereignAgencyRating())"),
                "the sovereign agency-rating leaf witness (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                ".<AgencyRatingCriteria>map(\"getAssetAgencyRating\", _assetAgencyRating -> _assetAgencyRating.getAssetAgencyRating())"),
                "the asset agency-rating leaf witness (PRE 0 / golden 1)");
    }

    /**
     * the ADD-seat conditional distribution (drr GetReportableDelivery): the
     * statement-level if/else + the in-branch thenArg LoL decl + the block method
     * selections (PRE 0 / golden 1 each); negatives — the runtime {@code .then(}
     * links and the mapSingleToItem selection are gone (PRE 3+1 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void getReportableDelivery_addSeatDistribution_witness() {
        String gen = drrFnOutput.get(GRD);
        assertNotNull(gen, "GetReportableDelivery not generated");
        assertEquals(1, count(gen,
                "if (exists(customizedSchedule(reportableEvent)).getOrDefault(false)) {"),
                "the ADD distributes to the statement-level if/else (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                "final MapperListOfLists<ReportableDelivery> thenArg = customizedSchedule(reportableEvent)"),
                "the THEN branch hoists the LoL thenArg in-branch (PRE 0 / golden 1)");
        assertEquals(1, count(gen, ".mapItemToList(schedulePeriod -> {"),
                "the outer extract selects the mapItemToList block (PRE 0 / golden 1)");
        assertEquals(1, count(gen, ".mapSingleToList(periods -> {"),
                "the else-side extract selects the mapSingleToList block (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "import com.rosetta.model.lib.mapper.MapperListOfLists;"),
                "the LoL import swaps in (PRE 0 / golden 1)");
        assertEquals(0, count(gen, ".then(item -> item"),
                "the runtime then links are gone (PRE 3 / golden 0)");
        assertEquals(0, count(gen, ".mapSingleToItem(periods -> "),
                "the single-item else selection is gone (PRE 1 / golden 0)");
    }

    /**
     * the in-lambda LoL singleton + the elseful evaluate-arg ladders (drr GRD): the
     * escaped {@code _thenArg} decl + its flattenList re-root + the blank-final
     * {@code ifThenElseResult0/1/2} ladders + the in-arm meta hoist and guarded
     * deref + the else-branch UNNUMBERED singleton (PRE 0 / golden 1-2 each).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void getReportableDelivery_inLambdaLoLAndIteLadders_witness() {
        String gen = drrFnOutput.get(GRD);
        assertNotNull(gen, "GetReportableDelivery not generated");
        assertEquals(1, count(gen,
                "final MapperListOfLists<ReportableDelivery> _thenArg = schedulePeriod."),
                "the in-lambda LoL singleton escapes against the session thenArg "
                + "(PRE 0 / golden 1)");
        assertEquals(2, count(gen, "return _thenArg"),
                "both branch lambdas re-root on the LoL local (PRE 0 / golden 2)");
        assertEquals(1, count(gen, "final Quantity ifThenElseResult0;"),
                "the elseful arg ladder 0 — blank-final (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final BigDecimal ifThenElseResult1;"),
                "the elseful arg ladder 1 — blank-final (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final String ifThenElseResult2;"),
                "the meta-tail arg ladder 2 — blank-final String (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final FieldWithMetaString fieldWithMetaString0 = "),
                "the in-arm wrapper hoist numbers per-lambda (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                "ifThenElseResult2 = fieldWithMetaString0 == null ? null : fieldWithMetaString0.getValue();"),
                "the guarded deref assigns the ladder local (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final Quantity ifThenElseResult;"),
                "the else-branch lambda's singleton stays UNNUMBERED — the per-lambda "
                + "collision-group law (PRE 0 / golden 1)");
    }

    /**
     * ruleInteriorBoundChainDecomp (drr fca ExchangeRateRule): the interior 5-level
     * per-level decomposition inside the bound restructure — the {@code _thenArg0..4}
     * decls + the only-element collapse + the re-rooted last body (PRE 0 / golden
     * 1-2 each); negative — the inline runtime {@code .then(_item} links are gone
     * (PRE 10 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void fcaExchangeRateRule_interiorDecomp_witness() {
        String gen = drrPojoOutput.get(FCA_ER);
        assertNotNull(gen, "fca ExchangeRateRule not generated");
        assertEquals(1, count(gen,
                "final MapperC<PriceQuantity> _thenArg0 = thenArg1.<TradableProduct>map(\"getTradableProduct\""),
                "the interior chain's base level decl (PRE 0 / golden 1)");
        assertEquals(2, count(gen,
                "final MapperListOfLists<FieldWithMetaPriceSchedule> _thenArg1 = _thenArg0"),
                "the mapItemToList LoL level — both branch lambdas (PRE 0 / golden 2)");
        assertEquals(2, count(gen,
                "final MapperS<FieldWithMetaPriceSchedule> _thenArg4 = MapperS.of(_thenArg3.get());"),
                "the only-element collapse level (PRE 0 / golden 2)");
        assertEquals(2, count(gen, "return _thenArg4"),
                "the last body re-roots on the final level (PRE 0 / golden 2)");
        assertEquals(1, count(gen, ".mapSingleToItem(trade -> {"),
                "the trade lambda block-converts (PRE 0 / golden 1)");
        assertEquals(0, count(gen, ".then(_item -> _item"),
                "the inline runtime then links are gone (PRE 10 / golden 0)");
    }

    /**
     * the same-boundary late-name + the list-literal element deref (drr fca
     * ForwardExchangeRateRule): the outer then-last decl numbers LAST
     * ({@code thenArg6} after the interior 0..5) + the literal's META element
     * coerces at the joined bare type — the #310 bare MapperC deref (PRE 0 /
     * golden 1 each); negative — the runtime {@code .then(item} links are gone
     * (PRE 7 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void fcaForwardExchangeRateRule_lateNameAndLiteralDeref_witness() {
        String gen = drrPojoOutput.get(FCA_FER);
        assertNotNull(gen, "fca ForwardExchangeRateRule not generated");
        assertEquals(1, count(gen,
                "final MapperC<BigDecimal> thenArg6 = MapperC.<PriceSchedule>of(_thenArg0"),
                "the outer then-last decl late-names AFTER the interior group "
                + "(PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                ".<PriceSchedule>map(\"Type coercion\", fieldWithMetaPriceSchedule -> fieldWithMetaPriceSchedule.getValue()))"),
                "the list-literal META element coerces at the joined bare item — the "
                + "#310 bare MapperC deref (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "return thenArg6"),
                "the body re-roots on the late-named decl (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                "final MapperS<TransactionReportInstruction> _thenArg0 = thenArg0"),
                "the first interior decl escapes against the method group "
                + "(PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final MapperS<Trade> _thenArg1 = thenArg0"),
                "the second interior decl escapes — text-order numbering "
                + "(PRE 0 / golden 1)");
        assertEquals(0, count(gen, ".then(item -> item"),
                "the inline runtime then links are gone (PRE 7 / golden 0)");
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
