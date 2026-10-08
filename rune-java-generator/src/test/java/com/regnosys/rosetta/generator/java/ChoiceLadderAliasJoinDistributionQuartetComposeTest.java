package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

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
 * PR #396 — the choice-deep-ladder / alias-extends-join / ADD-distribution quartet
 * (4 byte flips: Qualify_AssetClass_Credit + ExtractNotionalAdjustmentByLeg cdm6
 * FUNCTION + CheckEligibilityByDetails cdm5 FUNCTION + Contract_Price_Monetary drr
 * FUNCTION — the cdm5 FUNCTION cell EMPTIES).
 *
 * <p><b>choiceDeepNavLadderArm</b> (ControlFlowHandler + NavigationHandler): a RUNE
 * CHOICE-subject switch that is a hoisted-conditional ARM VALUE renders the upstream
 * DEEP option-nav ladder assigning the blank-final target at every leaf — the alias
 * signature's {@code MapperS<? extends T>} subject decl, per-case DEEP option paths
 * through the nested-choice AST tree, META options rendering the wrapper witness +
 * the guarded {@code Type coercion} deref registered on the STATEMENT scope (golden
 * {@code referenceWithMetaObservable0..3}), case locals on the SHARED scope (the
 * sibling-arm {@code _instrument} escape), reverse-fold compile + text-order emit;
 * the case-narrowed DEEP-call ({@code ->>}) receiver resolves off the guard type
 * (the #395 leaf-attr walk's RType sibling — the M7b-4 {@code TODO} carrier retires:
 * {@code instrumentDeepPathUtil.chooseInstrumentType(…)} + the @Inject pair, with
 * the guard resolved INSIDE the choice's option tree, namespace-correct).
 *
 * <p><b>fnFilterPredicateThenChainAdmit</b> (CollectionHandler +
 * FunctionExpressionRenderer): a FUNCTION-path {@code then filter} body whose
 * predicate ROOT is a nested hoistable+ctl-clean then-chain reads HANDLED and the
 * chain decomposes per-level through the lambda channel INSIDE the predicate's block
 * lambda ({@code _thenArg0/_thenArg1/thenArg2..5} under the alias's session-seeded
 * {@code thenArg0/thenArg1}), terminated {@code return exists(thenArg5).asMapper()
 * .get();} at the Boolean predicate seat.
 *
 * <p><b>aliasSwitchExtendsJoin + caseNarrowedToStringArm</b> (FunctionAliasHelper +
 * NavigationHandler + CollectionHandler + ConversionHandler): disagreeing bare-item
 * case guards join at the nearest common EXTENDS supertype (the fpmlProduct alias:
 * {@code MapperS<? extends ReturnSwapBase>} + the instanceof RETURN ladder with
 * {@code MapperS.of(<castLocal>)} arms); the receiver-type walk types an alias-body
 * switch as the joined guard type (the {@code <DirectionalLeg>mapC} witness cascade);
 * the #226 extract-switch block admits a {@code <bare attr> to-string} case (the
 * #368-F-B re-root off the bound cast var + the case-type-derived {@code _returnLeg}
 * param + {@code NotionalAdjustmentEnum::toDisplayString}).
 *
 * <p><b>addDistNestedCondArm + addDistDefaultArm</b> (FunctionExpressionRenderer):
 * the ADD-seat distribution recurses NESTED-conditional arms (per-leaf
 * {@code prices.addAll(toBuilder(…))} + {@code Collections.<PriceSchedule>
 * emptyList()} empty leaves + the #390 boolean0/boolean1 nesting) and distributes
 * {@code <chain> default <chain>} arms as the {@code getMulti().isEmpty()} guard
 * with per-side addAll.
 *
 * <p>Whole-file byte locks run through the REAL D11 FUNCTION route (cdm5 + cdm6 +
 * drr cells) and revert RED without the facets; every witness token is
 * occurrence-counted (python {@code str.count} semantics — the #352 law) and
 * PRE-counted against f-probe-395post (witness396.py: 31 tokens, all PRE≠GOLD).
 */
class ChoiceLadderAliasJoinDistributionQuartetComposeTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String QAC =
            "cdm/product/qualification/functions/Qualify_AssetClass_Credit.java";
    private static final String CEBD =
            "cdm/product/collateral/functions/CheckEligibilityByDetails.java";
    private static final String ENABL =
            "cdm/ingest/fpml/confirmation/tradestate/functions/ExtractNotionalAdjustmentByLeg.java";
    private static final String CPM =
            "drr/standards/iosco/cde/base/price/functions/Contract_Price_Monetary.java";

    private static Map<String, String> cdm5FnOutput;
    private static Map<String, String> cdm6FnOutput;
    private static Map<String, String> drrFnOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR)
                && Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR)
                && Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            cdm5FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
            cdm6FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
            drrFnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    /**
     * The REAL D11 FUNCTION-cell generation path (readDoNotPrune deliberately omitted
     * — correct for function cells, the Seat-1 #386 OBS-5 note). Per-file generation
     * errors are tolerated exactly as the D11 route tolerates them; a summary is
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

    // ==== byte locks (one carrier per facet through the REAL D11 FUNCTION route) ====

    /** the choice deep-nav ladder + DeepPathUtil carrier (the README worked example). */
    @Test
    @EnabledIf("cellsAvailable")
    void qualifyAssetClassCredit_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, QAC);
    }

    /** the filter-predicate then-chain decomposition carrier (cdm5 FUNCTION empties). */
    @Test
    @EnabledIf("cellsAvailable")
    void checkEligibilityByDetails_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, CEBD);
    }

    /** the alias extends-join + instanceof ladder + to-string case carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void extractNotionalAdjustmentByLeg_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, ENABL);
    }

    /** the recursive ADD-seat distribution + default-arm carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void contractPriceMonetary_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, CPM);
    }

    // ==== occurrence-counted witnesses (python str.count semantics — the #352 law) ====

    /**
     * choiceDeepNavLadderArm (cdm6 QAC): the wildcard subject decl, the DeepPathUtil
     * injections + the namespace-correct import, the case-narrowed deep calls, the
     * reverse-fold coercion numbering and the default arm (witness396.py PRE counts).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void qualifyAssetClassCredit_choiceDeepNavLadder_witness() {
        String gen = cdm6FnOutput.get(QAC);
        assertNotNull(gen, "QAC not generated");
        assertEquals(1, count(gen,
                "final MapperS<? extends Underlier> switchArgument = performanceUnderlier(economicTerms);"),
                "the alias-signature-typed subject decl (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "@Inject protected InstrumentDeepPathUtil instrumentDeepPathUtil;"),
                "the Instrument DeepPathUtil injection (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "@Inject protected IndexDeepPathUtil indexDeepPathUtil;"),
                "the Index DeepPathUtil injection (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "import cdm.observable.asset.util.IndexDeepPathUtil;"),
                "the guard resolves INSIDE the choice option tree — the cdm (not fpml) "
                + "IndexDeepPathUtil namespace (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "instrumentDeepPathUtil.chooseInstrumentType(_instrument)"),
                "the case-narrowed deep call wires M7b-4 (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "indexDeepPathUtil.chooseAssetClass(_index)"),
                "the Index-case deep call (PRE 0 / golden 1)");
        assertEquals(6, count(gen, "referenceWithMetaObservable3"),
                "the Loan-rung coercion param numbers LAST in the method group — "
                + "reverse-fold registration (PRE 0 / golden 6)");
        assertEquals(1, count(gen, "final MapperS<Loan> loan = switchArgument."),
                "the always-declared case local (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "ifThenElseResult2 = ComparisonResult.ofNullSafe(MapperS.of(false));"),
                "the default arm's CR wrap (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                "referenceWithMetaObservable0 == null ? null : referenceWithMetaObservable0.getValue()"),
                "the outer evaluate-arg hoist renumbers 0 with the rung group (PRE 0 / golden 1)");
    }

    /**
     * fnFilterPredicateThenChainAdmit (cdm5 CEBD): the alias statement-channel decls,
     * the in-predicate collision-escalated interior decls and the exists terminals.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void checkEligibilityByDetails_filterPredicateThenChain_witness() {
        String gen = cdm5FnOutput.get(CEBD);
        assertNotNull(gen, "CEBD not generated");
        assertEquals(1, count(gen,
                "final MapperC<EligibleCollateralCriteria> thenArg0 = MapperS.of(specification)"),
                "the alias statement-channel base decl (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                "final MapperC<AssetCriteria> _thenArg0 = item.<AssetCriteria>mapC(\"getAsset\""),
                "the interior base decl escapes against the seeded session names "
                + "(PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final MapperC<AssetCriteria> thenArg2 = _thenArg1"),
                "the third interior decl stays plain — per-member escape after "
                + "numbering (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "return exists(thenArg5).asMapper().get();"),
                "the asset predicate's CR-terminal Boolean collapse (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "return exists(thenArg2).asMapper().get();"),
                "the issuer predicate's CR-terminal Boolean collapse (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                "final MapperC<IssuerCriteria> _thenArg0 = item.<IssuerCriteria>mapC(\"getIssuer\""),
                "the second predicate's own per-lambda group re-escapes (PRE 0 / golden 1)");
    }

    /**
     * aliasSwitchExtendsJoin + caseNarrowedToStringArm (cdm6 ENABL): the LUB-joined
     * alias signature + instanceof RETURN ladder, the joined-element nav witness and
     * the to-string case's case-narrowed enum form.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void extractNotionalAdjustmentByLeg_extendsJoinToString_witness() {
        String gen = cdm6FnOutput.get(ENABL);
        assertNotNull(gen, "ENABL not generated");
        assertEquals(2, count(gen, "MapperS<? extends ReturnSwapBase> fpmlProduct"),
                "the extends-LUB alias signature — abstract + impl (PRE 0 / golden 2)");
        assertEquals(1, count(gen,
                "final Product switchArgument = MapperS.of(fpmlTrade).<Product>map(\"getProduct\", trade -> trade.getProduct()).get();"),
                "the unwrapped alias-seat subject hoist (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "if (switchArgument instanceof EquitySwapTransactionSupplement)"),
                "the bare-item instanceof arm (PRE 0 / golden 1)");
        assertEquals(2, count(gen, "return MapperS.<ReturnSwapBase>ofNull();"),
                "the joined null-guard + terminal (PRE 0 / golden 2)");
        assertEquals(1, count(gen,
                ".<DirectionalLeg>mapC(\"getReturnSwapLeg\", returnSwapBase -> returnSwapBase.getReturnSwapLeg())"),
                "the joined-element nav witness + cardinality + param cascade "
                + "(PRE 0 / golden 1)");
        assertEquals(1, count(gen, "NotionalAdjustmentEnum::toDisplayString"),
                "the case-narrowed enum to-string label (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final ReturnLeg returnLeg = (ReturnLeg) switchArgument;"),
                "the mapItem block's cast case local (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "_returnLeg -> _returnLeg.getNotionalAdjustments()"),
                "the case-type-derived nav param (PRE 0 / golden 1)");
        assertEquals(2, count(gen, "return MapperS.<String>ofNull();"),
                "the all-to-string String result witness (PRE 0 / golden 2)");
    }

    /**
     * addDistNestedCondArm + addDistDefaultArm (drr CPM): the empty leaves, the
     * default-arm isEmpty guard, the in-arm boolHoists and the per-arm meta derefs.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void contractPriceMonetary_addDistribution_witness() {
        String gen = drrFnOutput.get(CPM);
        assertNotNull(gen, "CPM not generated");
        assertEquals(2, count(gen, "prices.addAll(toBuilder(Collections.<PriceSchedule>emptyList()));"),
                "the nested empty leaves distribute (PRE 0 / golden 2)");
        assertEquals(1, count(gen, ".getMulti().isEmpty()) {"),
                "the default-arm isEmpty guard (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "\t\t\t\tfinal Boolean boolean0 = isCommoditySwapFixedFloat.evaluate("),
                "the first boolHoist moves IN-ARM (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "\t\t\t\t\tfinal Boolean boolean1 = isCommodityForward.evaluate("),
                "the second boolHoist nests one deeper (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                "referenceWithMetaPriceSchedule0 == null ? null : referenceWithMetaPriceSchedule0.getValue()"),
                "the first arm's guarded MapperS deref numbers 0 (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                "referenceWithMetaPriceSchedule1 == null ? null : referenceWithMetaPriceSchedule1.getValue()"),
                "the second arm's guarded deref numbers 1 (PRE 0 / golden 1)");
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
