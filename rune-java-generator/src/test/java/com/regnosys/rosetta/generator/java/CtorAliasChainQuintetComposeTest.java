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
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #389 — the ctor/alias-chain quintet (5 byte flips:
 * MapBasketConstituentWithLocation + MapFxFixingScheduleToObservationDates +
 * StandardizedScheduleVarianceSwapNotionalAmount +
 * MapPayerReceiverToAccountPartyReference [cdm6 FUNCTION] +
 * QuantityToDeliveryCapacity [drr FUNCTION]).
 *
 * <p><b>ctorChoiceSuperAttrs + ladderElementCollisionQualify</b> (HandlerHelper +
 * ConstructionHandler + FunctionExpressionRenderer + FunctionGenerator): a data
 * type EXTENDING A CHOICE inherits the choice's OPTIONS as attributes (the
 * choice-super-aware walk projects via the SAME RChoiceTypeRef.asRDataType bridge,
 * consumed ONLY at the two ConstructionHandler ctor seats), unlocking the typed
 * builder block whose EXISTING arms render the #232 ArrayList+getMulti value-setter,
 * the bare explicit-arg alias splice and the #227 fqnWitness qualified ctor head;
 * the switch-ladder ofNull terminals lift the element VERBATIM from the rendered
 * signature return type (aliasReturnElementOrNull — the #247 sentinel ride-along
 * law), with the Seat-1 #388 OBS-5 guard declining a qualified element + a
 * conditional case arm.
 *
 * <p><b>ctorEvalArgRecordDateIteHoist</b> (ControlFlowHandler): a then-chain whose
 * LEAF is the {@code date} RECORD feature types the #181 item-hoist local from the
 * SAME isDateRecordFeature gate the record-nav render fires on (the walk-render
 * alignment law) — {@code final Date ifThenElseResult0;} at the ctor-setter
 * evaluate-arg seat.
 *
 * <p><b>aliasCondLevelObjectRecovery + deepThenSentinelOperandTyping</b>
 * (CollectionHandler + FunctionExpressionRenderer + StatementHoistSession +
 * ArithmeticHandler + ComparisonHandler + HandlerHelper): a DISCARDABLE-OWNER sink
 * session exempts the #351 pre-scan's Object-snapshot decline (the handshake's
 * compiled-arm recovery runs instead; the flag-scoped runtime-{@code .then(} strand
 * guard contains a genuine mid-loop decline); the Mapper-typed #351 sentinel's
 * MISSING-snapshot implicit re-read types arith/comparison operands from the
 * render-truth then-step BINDING (the sentinel splices BARE at the MapperMaths
 * seat, the int literal wraps {@code BigDecimal.valueOf(1)} via the asymmetric
 * literal-only contract).
 *
 * <p><b>aliasSelfShadowDisguisedChain + disguisedInputRootCardinality +
 * filterBaseElementRecovery</b> (ReferenceHandler + NavigationHandler +
 * CollectionHandler): the #372 F-delta-5 self-reference law at the DISGUISED-chain
 * seats — a head matching the ENCLOSING shortcut is the shadowed ITEM feature at
 * BOTH the render synthesis and the shared walk (which previously early-returned
 * null); chainProvesMulti gains the MULTI function-INPUT root sibling of the #345
 * alias-root consult (filterItemNullSafe/mapItem arity); the k==0 FILTER-base and
 * k&gt;0 PLAIN-STEP decl elements recover through the walk ladder
 * (DISCARDABLE-OWNER-scoped — an Object decl DISCARDS the alias render today, so
 * recovery is a pure decline&rarr;hoist transition).
 *
 * <p><b>disguisedAliasChainOperandRetype</b> (ArithmeticHandler): a DISGUISED
 * 2-name chain over an ALIAS head compiles null-typed, so the operand retypes from
 * the SAME resolveDisguisedFeature walk (NUMERIC leaves only) and the EXISTING #334
 * wrapper-level coercion emits the guarded {@code BigDecimal.valueOf} hop with
 * method-wide deferred numbering ({@code integer0..15}).
 *
 * <p>Whole-file byte locks run through the REAL D11 FUNCTION routes (cdm6 + drr)
 * and revert RED without the facets; every witness token is occurrence-counted
 * (python {@code str.count} semantics — the #352 law) and PRE-counted against
 * f-probe-388post (each removal token PRE &ge; 1 / golden 0; each golden token
 * PRE 0 / golden &ge; 1 — witness389.py).
 */
class CtorAliasChainQuintetComposeTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String MAP_BASKET =
            "cdm/ingest/fpml/confirmation/pricequantity/functions/MapBasketConstituentWithLocation.java";
    private static final String MAP_FX =
            "cdm/ingest/fpml/confirmation/product/fxvarianceswap/functions/MapFxFixingScheduleToObservationDates.java";
    private static final String SSVSNA =
            "cdm/margin/schedule/functions/StandardizedScheduleVarianceSwapNotionalAmount.java";
    private static final String MAP_PAYER =
            "cdm/ingest/fpml/confirmation/party/functions/MapPayerReceiverToAccountPartyReference.java";
    private static final String QTY_DELIVERY =
            "drr/regulation/common/functions/QuantityToDeliveryCapacity.java";

    private static Map<String, String> cdm6FnOutput;
    private static Map<String, String> drrFnOutput;

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

    // ==== byte locks (all 5 flips through the REAL D11 FUNCTION routes) ====

    /** ctorChoiceSuperAttrs + ladderElementCollisionQualify: the composite carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapBasketConstituentWithLocation_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_BASKET);
    }

    /** ctorEvalArgRecordDateIteHoist: the record-date-leaf evaluate-arg carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapFxFixingScheduleToObservationDates_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_FX);
    }

    /** aliasCondLevelObjectRecovery + deepThenSentinelOperandTyping: the composite carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void standardizedScheduleVarianceSwapNotionalAmount_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, SSVSNA);
    }

    /** aliasSelfShadowDisguisedChain + the arity/element recoveries: the composite carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapPayerReceiverToAccountPartyReference_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_PAYER);
    }

    /** disguisedAliasChainOperandRetype: the ×16 guarded-hop carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void quantityToDeliveryCapacity_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, QTY_DELIVERY);
    }

    // ==== occurrence-counted witnesses (python str.count semantics — the #352 law) ====

    /**
     * ctorChoiceSuperAttrs + ladderElementCollisionQualify: the qualified ctor head
     * (PRE 0 / golden 2), the qualified ofNull terminal (PRE 0 / golden 1), the #232
     * ArrayList value-setter (PRE 0 / golden 2), the bare explicit-arg alias splice
     * (PRE 0 / golden 1) + the ArrayList import (PRE 0 / golden 1); negative — the
     * spurious MapperS.of wrap at the setter-arg seat is gone (PRE 1 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void mapBasket_choiceSuperCtor_witness() {
        String gen = cdm6FnOutput.get(MAP_BASKET);
        assertNotNull(gen, "MapBasketConstituentWithLocation not generated");
        assertEquals(2, count(gen, "cdm.observable.asset.BasketConstituent.builder()"),
                "the collision-qualified ctor head, both arms (PRE 0 / golden 2)");
        assertEquals(1, count(gen, "return MapperS.<cdm.observable.asset.BasketConstituent>ofNull();"),
                "the qualified ofNull terminal (PRE 0 / golden 1)");
        assertEquals(2, count(gen, ".setQuantityValue(new ArrayList<>("
                        + "basketConstituentQuantity(fpmlBasketConstituent).getMulti()))"),
                "the #232 ArrayList value-setter, both arms (PRE 0 / golden 2)");
        assertEquals(1, count(gen,
                ".setAsset(mapAsset.evaluate(fpmlUnderlyingAsset(fpmlBasketConstituent).get()))"),
                "the explicit-arg alias splices bare (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "import java.util.ArrayList;"),
                "the ArrayList import lands (PRE 0 / golden 1)");
        assertEquals(0, count(gen, ".setAsset(MapperS.of(mapAsset.evaluate("),
                "the spurious setter-arg wrap is gone (PRE 1 / golden 0)");
    }

    /**
     * ctorEvalArgRecordDateIteHoist: both evaluate-arg conditionals hoist
     * {@code final Date ifThenElseResultN;} (PRE 0 / golden 1 each) and the bare
     * locals splice into the callee (PRE 0 / golden 1 each); negative — the inline
     * ternary form is gone (PRE 2 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void mapFx_recordDateItemHoist_witness() {
        String gen = cdm6FnOutput.get(MAP_FX);
        assertNotNull(gen, "MapFxFixingScheduleToObservationDates not generated");
        assertEquals(1, count(gen, "final Date ifThenElseResult0;"),
                "the startDate hoist declares Date (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final Date ifThenElseResult1;"),
                "the endDate hoist declares Date (PRE 0 / golden 1)");
        assertEquals(1, count(gen, ".evaluate(ifThenElseResult0, null)"),
                "the bare local splices into the callee (PRE 0 / golden 1)");
        assertEquals(1, count(gen, ".evaluate(ifThenElseResult1, null)"),
                "the second bare local splices too (PRE 0 / golden 1)");
        assertEquals(0, count(gen, ".getOrDefault(false) ? "),
                "the inline ternary form is gone (PRE 2 / golden 0)");
    }

    /**
     * aliasCondLevelObjectRecovery + deepThenSentinelOperandTyping: the base thenArg
     * types the META wrapper (PRE 0 / golden 1), both cond levels hoist Mapper-form
     * sentinels (ifThenElseResult0 decl PRE 0 / golden 1; the bare return PRE 0 /
     * golden 1), the coercion group renumbers to 3 (PRE 0 / golden 3 occurrences of
     * the last param), the int literal wraps BigDecimal.valueOf (PRE 0 / golden 1)
     * and the sentinel splices BARE at the MapperMaths seat (PRE 0 / golden 1);
     * negative — the runtime .then( form is gone (PRE 2 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void ssvsna_sentinelOperandTyping_witness() {
        String gen = cdm6FnOutput.get(SSVSNA);
        assertNotNull(gen, "StandardizedScheduleVarianceSwapNotionalAmount not generated");
        assertEquals(1, count(gen,
                "final MapperS<ReferenceWithMetaNonNegativeQuantitySchedule> thenArg = "),
                "the base thenArg types the meta wrapper (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final MapperS<BigDecimal> ifThenElseResult0;"),
                "the k=1 cond level hoists the Mapper-form sentinel (PRE 0 / golden 1)");
        assertEquals(3, count(gen, "referenceWithMetaNonNegativeQuantitySchedule3"),
                "the coercion group renumbers in text order (PRE 0 / golden 3)");
        assertEquals(1, count(gen, "MapperS.of(BigDecimal.valueOf(1))"),
                "the int literal wraps at the comparison seat (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                "multiply(MapperS.of(new BigDecimal(\"0.01\")), ifThenElseResult0)"),
                "the sentinel splices BARE at the MapperMaths seat (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "return ifThenElseResult1;"),
                "the consumer returns the bare sentinel (PRE 0 / golden 1)");
        assertEquals(0, count(gen, ".then(item -> "),
                "the runtime then form is gone (PRE 2 / golden 0)");
    }

    /**
     * aliasSelfShadowDisguisedChain + disguisedInputRootCardinality +
     * filterBaseElementRecovery: the shadowed item features render (payer + receiver,
     * PRE 0 / golden 1 each), the filter arity reads Item (PRE 0 / golden 2), the
     * typed thenArg0 decl lands (PRE 0 / golden 1) and both consumers collapse
     * (PRE 0 / golden 2); negatives — the infinitely-recursive self-call and the
     * Single filter arity are gone (PRE 1 and 2 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void mapPayer_selfShadowChain_witness() {
        String gen = cdm6FnOutput.get(MAP_PAYER);
        assertNotNull(gen, "MapPayerReceiverToAccountPartyReference not generated");
        assertEquals(1, count(gen, "item.<PartyReference>map(\"getPayerPartyReference\", "
                        + "payerModel -> payerModel.getPayerPartyReference())"),
                "the payer self-shadow resolves to the item feature (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "item.<PartyReference>map(\"getReceiverPartyReference\", "
                        + "receiverModel -> receiverModel.getReceiverPartyReference())"),
                "the receiver self-shadow resolves too (PRE 0 / golden 1)");
        assertEquals(2, count(gen, ".filterItemNullSafe("),
                "the MULTI input root drives the Item filter arity (PRE 0 / golden 2)");
        assertEquals(1, count(gen, "final MapperC<PayerModel> thenArg0 = "),
                "the filter-base element recovers PayerModel (PRE 0 / golden 1)");
        assertEquals(2, count(gen, "return MapperS.of(distinct(thenArg1).get());"),
                "both consumers collapse on the typed local (PRE 0 / golden 2)");
        assertEquals(0, count(gen,
                "payerPartyReference(fpmlAccount, fpmlPayerReceiverModelList).map(\"getHref\""),
                "the infinitely-recursive self-call is gone (PRE 1 / golden 0)");
        assertEquals(0, count(gen, ".filterSingleNullSafe("),
                "the Single filter arity is gone (PRE 2 / golden 0)");
    }

    /**
     * disguisedAliasChainOperandRetype: the guarded valueOf hop fires ×16 with
     * method-wide deferred numbering — the first hop (PRE 0 / golden 1), the full
     * family (PRE 0 / golden 16) and the numbering tail integer15 (PRE 0 / golden 3).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void qtyDelivery_disguisedAliasOperandHop_witness() {
        String gen = drrFnOutput.get(QTY_DELIVERY);
        assertNotNull(gen, "QuantityToDeliveryCapacity not generated");
        assertEquals(1, count(gen, ".<BigDecimal>map(\"Type coercion\", integer0 -> "
                        + "integer0 == null ? null : BigDecimal.valueOf(integer0))"),
                "the first guarded hop (PRE 0 / golden 1)");
        assertEquals(16, count(gen, ".<BigDecimal>map(\"Type coercion\", integer"),
                "the whole ×16 family fires (PRE 0 / golden 16)");
        assertEquals(3, count(gen, "integer15"),
                "the method-wide numbering reaches the tail (PRE 0 / golden 3)");
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
