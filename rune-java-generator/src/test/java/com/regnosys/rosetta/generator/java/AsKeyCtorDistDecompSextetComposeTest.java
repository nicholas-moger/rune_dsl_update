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
 * PR #390 — the as-key/distribution/decomposition sextet (6 byte flips:
 * Create_PartyChange [cdm5 + cdm6 FUNCTION] +
 * Create_ReportingSideFromReportableEvent + Contract_StrikePrice +
 * IsCommodityBullion + IsCommodityMetal [drr FUNCTION]).
 *
 * <p><b>ctorAsKeyReference + addSegmentLeafMetaDeref</b> (ConstructionHandler +
 * FunctionExpressionRenderer): the first {@code RKeyValuePair.isAsKey()} generator
 * consumer — an as-key ctor field renders the upstream meta-key copy (the #328
 * renderAsKeySetOrNull law at the ctor seat): the hoisted (wrapper, value) local
 * pair + the reference-only {@code ReferenceWithMetaX.builder()
 * .setGlobalReference(…).setExternalReference(…)} value. The wrapper locals number
 * through the statement-hoist session group ({@code referenceWithMetaParty0..3} in
 * Create_ReportingSideFromReportableEvent's assignOutput; BARE in
 * Create_PartyChange's per-method alias sessions, landed in-branch by the #376 arm
 * drain); the value local is the ctor FIELD name emitted literally (the #328
 * precedent). {@code coerceAddValueMetaItem} widens to SEGMENT-carrying ADDs
 * against the path LEAF attribute (bare element-wise on MapperC, null-guarded
 * deferred-numbered on MapperS — the addParty pair).
 *
 * <p><b>addDistNestedBoolHoist</b> (FunctionExpressionRenderer): the #179
 * nested-else boolHoist restructure at the add-distribution seat —
 * {@code addDistributionAdmissible} admits a bare-fn-call condition at NESTED-ELSE
 * levels (the TOP-level decline stays) and the render deepens one level per rung
 * ({@code } else { final Boolean booleanN = <bare call>; if ((booleanN == null ?
 * false : booleanN)) { prices.addAll(…); } … }}) with the sentinel on the method
 * session in render order — the numbering the non-distributed ControlFlowHandler
 * route already produced, so the skeleton lines stay byte-identical.
 *
 * <p><b>aliasListLiteralMetaItemForward + listLiteralMetaAliasElementJoin +
 * aliasListElementThenDecomp</b> (FunctionAliasHelper + LiteralHandler +
 * FunctionExpressionRenderer): the RListLiteral walk arm forwards the META-arm
 * javaItemType when every element agrees (the {@code [list] only-element} alias
 * body then types {@code MapperS<FieldWithMetaString>} and the evaluate-arg deref
 * hoists fire); walkedItemType joins a META alias element at its CONCRETE wrapper
 * via the same walk that renders the alias signature (the D1 NOTHING-join recovery
 * then witnesses {@code MapperC.<FieldWithMetaString>of} over alias-ref elements
 * instead of the non-compiling Void); renderAliasSinkHoistsOrNull admits an
 * ONLY_ELEMENT top over a then-chain-carrying list literal (the #368 admission
 * pattern) and the #350-F4 decomposition renders {@code thenArg0..5}.
 *
 * <p>Whole-file byte locks run through the REAL D11 FUNCTION routes (cdm5 + cdm6 +
 * drr) and revert RED without the facets; every witness token is
 * occurrence-counted (python {@code str.count} semantics — the #352 law) and
 * PRE-counted against f-probe-389post (each removal token PRE &ge; 1 / golden 0;
 * each golden token PRE 0 / golden &ge; 1 — witness390.py).
 */
class AsKeyCtorDistDecompSextetComposeTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String CPC =
            "cdm/event/common/functions/Create_PartyChange.java";
    private static final String CRSFRE =
            "drr/enrichment/common/trade/functions/Create_ReportingSideFromReportableEvent.java";
    private static final String CSP =
            "drr/standards/iosco/cde/base/price/functions/Contract_StrikePrice.java";
    private static final String BULLION =
            "drr/regulation/common/functions/IsCommodityBullion.java";
    private static final String METAL =
            "drr/regulation/common/functions/IsCommodityMetal.java";

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

    // ==== byte locks (all 6 flips through the REAL D11 FUNCTION routes) ====

    /** ctorAsKeyReference + addSegmentLeafMetaDeref: the cdm5 twin. */
    @Test
    @EnabledIf("cellsAvailable")
    void createPartyChangeCdm5_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, CPC);
    }

    /** ctorAsKeyReference + addSegmentLeafMetaDeref: the cdm6 twin. */
    @Test
    @EnabledIf("cellsAvailable")
    void createPartyChangeCdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, CPC);
    }

    /** ctorAsKeyReference: the 4-field statement-level numbering carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void createReportingSideFromReportableEvent_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, CRSFRE);
    }

    /** addDistNestedBoolHoist: the 5-rung nested-else distribution carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void contractStrikePrice_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, CSP);
    }

    /** the alias list-literal trio: the Bullion carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void isCommodityBullion_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, BULLION);
    }

    /** the alias list-literal trio: the Metal twin. */
    @Test
    @EnabledIf("cellsAvailable")
    void isCommodityMetal_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, METAL);
    }

    // ==== occurrence-counted witnesses (python str.count semantics — the #352 law) ====

    /**
     * ctorAsKeyReference + addSegmentLeafMetaDeref (cdm6 Create_PartyChange): both
     * alias methods render the reference-only builder (PRE 0 / golden 2), hoist the
     * bare wrapper local (PRE 0 / golden 2) and the field-name value local (PRE 0 /
     * golden 2) consumed by the meta-key copy (PRE 0 / golden 2); the addParty pair
     * derefs bare-element-wise on the MapperC chain (PRE 0 / golden 1) and
     * null-guarded deferred-numbered on the MapperS chain (PRE 0 / golden 1);
     * negative — the whole-wrapper ctor splice is gone (PRE 2 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void createPartyChange_asKeyCtor_witness() {
        String gen = cdm6FnOutput.get(CPC);
        assertNotNull(gen, "Create_PartyChange not generated");
        assertEquals(2, count(gen, ".setPartyReference(ReferenceWithMetaParty.builder()"),
                "the reference-only builder renders in both alias methods (PRE 0 / golden 2)");
        assertEquals(2, count(gen,
                "final ReferenceWithMetaParty referenceWithMetaParty = MapperS.of(counterparty)"),
                "the bare wrapper local hoists per method (PRE 0 / golden 2)");
        assertEquals(2, count(gen, "final Party partyReference = referenceWithMetaParty == null"
                        + " ? null : referenceWithMetaParty.getValue();"),
                "the field-name value local hoists per method (PRE 0 / golden 2)");
        assertEquals(2, count(gen, ".setGlobalReference(Optional.ofNullable(partyReference)"),
                "the meta-key copy consumes the field local (PRE 0 / golden 2)");
        assertEquals(1, count(gen, ".<Party>map(\"Type coercion\", referenceWithMetaParty -> "
                        + "referenceWithMetaParty.getValue()).getMulti())"),
                "the MapperC addParty seat derefs bare element-wise (PRE 0 / golden 1)");
        assertEquals(1, count(gen, ".<Party>map(\"Type coercion\", referenceWithMetaParty2 -> "
                        + "referenceWithMetaParty2 == null ? null : "
                        + "referenceWithMetaParty2.getValue()).getMulti())"),
                "the MapperS addParty seat derefs guarded + deferred-numbered (PRE 0 / golden 1)");
        assertEquals(0, count(gen, ".setPartyReference(MapperS.of(counterparty)"
                        + ".<ReferenceWithMetaParty>map(\"getPartyReference\", "
                        + "_counterparty -> _counterparty.getPartyReference()).get())"),
                "the whole-wrapper ctor splice is gone (PRE 2 / golden 0)");
    }

    /**
     * ctorAsKeyReference (Create_ReportingSideFromReportableEvent): the 4 as-key
     * pairs number the wrapper group 0..3 through the statement-hoist session
     * (referenceWithMetaParty0 decl PRE 0 / golden 1; the tail member appears decl +
     * guard + getValue = PRE 0 / golden 3), the field-name value local consumes the
     * guarded deref (PRE 0 / golden 1), the builder value lands per field (PRE 0 /
     * golden 1) with the meta-key copy ×4 (PRE 0 / golden 4) and the bare value
     * type's import (PRE 0 / golden 1); negative — the whole-wrapper setter splice
     * is gone (PRE 1 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void createReportingSide_asKeyNumbering_witness() {
        String gen = drrFnOutput.get(CRSFRE);
        assertNotNull(gen, "Create_ReportingSideFromReportableEvent not generated");
        assertEquals(1, count(gen, "final ReferenceWithMetaParty referenceWithMetaParty0 = "),
                "the wrapper group numbers from 0 (PRE 0 / golden 1)");
        assertEquals(3, count(gen, "referenceWithMetaParty3"),
                "the group tail reaches 3 — decl + guard + getValue (PRE 0 / golden 3)");
        assertEquals(1, count(gen, "final Party reportingParty = referenceWithMetaParty0 == null"
                        + " ? null : referenceWithMetaParty0.getValue();"),
                "the field-name value local consumes the guarded deref (PRE 0 / golden 1)");
        assertEquals(1, count(gen, ".setReportingParty(ReferenceWithMetaParty.builder()"),
                "the reference-only builder lands at the ctor field (PRE 0 / golden 1)");
        assertEquals(4, count(gen, ".map(m -> m.getGlobalKey())"),
                "the meta-key copy renders for all four fields (PRE 0 / golden 4)");
        assertEquals(1, count(gen, "import cdm.base.staticdata.party.Party;"),
                "the bare value type's import lands (PRE 0 / golden 1)");
        assertEquals(0, count(gen, ".setReportingParty(MapperS.of(extractCounterpartyByRole.evaluate("),
                "the whole-wrapper setter splice is gone (PRE 1 / golden 0)");
    }

    /**
     * addDistNestedBoolHoist (Contract_StrikePrice): the distribution consumes every
     * arm (PRE 1 / golden 10 — the PRE 1 is the single trailing addAll), the
     * terminal else takes the typed emptyList (PRE 0 / golden 1) + the Collections
     * import (PRE 0 / golden 1), and the meta-ended arms deref through the
     * underscore-escaped bare element-wise hop (PRE 0 / golden 4); negatives — the
     * scalar hoist decl and the trailing addAll are gone (PRE 1 each / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void contractStrikePrice_distribution_witness() {
        String gen = drrFnOutput.get(CSP);
        assertNotNull(gen, "Contract_StrikePrice not generated");
        assertEquals(10, count(gen, "prices.addAll(toBuilder("),
                "the distribution consumes every arm + the terminal else (PRE 1 / golden 10)");
        assertEquals(1, count(gen, "prices.addAll(toBuilder(Collections.<PriceSchedule>emptyList()));"),
                "the terminal else takes the typed emptyList (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "import java.util.Collections;"),
                "the Collections import lands (PRE 0 / golden 1)");
        assertEquals(4, count(gen, "_referenceWithMetaPriceSchedule -> "
                        + "_referenceWithMetaPriceSchedule.getValue()).getMulti()"),
                "the meta-ended arms deref via the escaped bare hop (PRE 0 / golden 4)");
        assertEquals(0, count(gen, "final PriceSchedule ifThenElseResult;"),
                "the scalar hoist decl is gone (PRE 1 / golden 0)");
        assertEquals(0, count(gen, "prices.addAll(toBuilder(ifThenElseResult));"),
                "the trailing single addAll is gone (PRE 1 / golden 0)");
    }

    /**
     * the alias list-literal trio (IsCommodityBullion): the productClass join
     * witnesses the concrete wrapper (PRE 0 / golden 1 — the PRE carried the
     * non-compiling {@code MapperC.<Void>of}), the commodityReferencePrice body
     * decomposes (thenArg0 decl count 1 &rarr; 2; the thenArg5 only-element collapse
     * PRE 0 / golden 1), the evaluate-arg deref fires (PRE 0 / golden 1) and the
     * #179 nested-else restructure hoists the CRP wrapper local in the third arm
     * (PRE 0 / golden 1); negatives — the Void witness and the runtime then form
     * are gone (PRE 1 and 6 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void isCommodityBullion_aliasDecomp_witness() {
        String gen = drrFnOutput.get(BULLION);
        assertNotNull(gen, "IsCommodityBullion not generated");
        assertEquals(1, count(gen, "MapperC.<FieldWithMetaString>of(productTaxonomy(product), "
                        + "productIdentifier(product))"),
                "the alias-ref element join witnesses the wrapper (PRE 0 / golden 1)");
        assertEquals(2, count(gen, "final MapperC<ReferenceWithMetaProductIdentifier> thenArg0 = "),
                "the CRP base chain hoists beside the pre-existing alias decomp (PRE 1 / golden 2)");
        assertEquals(1, count(gen, "final MapperS<ReferenceWithMetaProductIdentifier> thenArg5 = "
                        + "MapperS.of(thenArg4.get());"),
                "the second element's only-element collapse hoists (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "isCRPBullion.evaluate((fieldWithMetaString1 == null ? null : "
                        + "fieldWithMetaString1.getValue()))"),
                "the evaluate-arg deref fires off the alias walk type (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final FieldWithMetaString fieldWithMetaString3 = "
                        + "commodityReferencePrice(product).get();"),
                "the third arm's condition hoist triggers the #179 restructure (PRE 0 / golden 1)");
        assertEquals(0, count(gen, "MapperC.<Void>of("),
                "the non-compiling Void witness is gone (PRE 1 / golden 0)");
        assertEquals(0, count(gen, ".then(item -> item"),
                "the runtime then form is gone (PRE 6 / golden 0)");
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
