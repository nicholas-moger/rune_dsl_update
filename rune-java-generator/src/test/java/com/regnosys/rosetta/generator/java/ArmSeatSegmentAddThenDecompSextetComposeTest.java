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
 * PR #391 — the arm-seat/segment-add then-decomposition sextet (6 byte flips:
 * Create_BusinessEvent [cdm5 + cdm6 FUNCTION] + StandardizedScheduleNotional +
 * StandardizedScheduleNotionalCurrency [cdm6 FUNCTION] + Create_SubmissionSchedules
 * + Extract_ReferenceEntityFormat [drr FUNCTION]).
 *
 * <p><b>armSeatChainedBareFnThen + bareInvokableThenArg typing/naming +
 * collapseStepElementFromPrevRef + fnCallReceiverLambdaTyping</b>
 * (FunctionExpressionRenderer + NavigationHandler): the conditional-ARM seat admits
 * the SAME then-chain population as the top-level SET seat (isHoistableThenChain —
 * the #359 F-7 predicate), so a ladder arm's {@code … then first then priceQuantity
 * then <Fn>} decomposes to method-session-numbered {@code thenArg0..14} hoists with
 * the {@code MapperS.of(<fn>.evaluate(thenArgN.get())).get()} terminal;
 * renderBareInvokableThenSet types an Object-erased decl through the #238/#388
 * recoverThenArgItemRType walk ladder (collapsing the #14 coercion cast bridge) and
 * joins the method session's thenArg group; a k&gt;0 bare-item single collapse
 * (first/last/only-element) erased to Object takes its element from the PRECEDING
 * thenArg (render truth, both the SET and deep routes); a FUNCTION-CALL receiver
 * names its step lambda from the callee output's data type via the SAME
 * resolveReceiverDataType walk the witness reads ({@code underlier}, not
 * {@code _underlierForProduct}).
 *
 * <p><b>bareEnumComparandHierarchy + bareItemPipeSiblingEnum</b>
 * (ComparisonHandler + NavigationHandler): tryBareEnumComparand's value match goes
 * hierarchy-aware (findEnumValueInHierarchy + CHILD-name qualification — the
 * #211/#358 flatten law; EntityIdentifierTypeEnum extends PartyIdentifierTypeEnum,
 * LEI/CountryCode live on the parent), and a BARE implicit-ITEM sibling resolves
 * its enum by walking the owning then-step's pipe (through item-rooted extract
 * wraps) to the producing body and re-running the sibling rung ladder on it.
 *
 * <p><b>thenAddSegmentPath + addSegmentLadderArmDrain + ladderArmBareElementStamp</b>
 * (FunctionExpressionRenderer + CollectionHandler): a SEGMENT-path ADD whose value
 * is a hoistable then-chain routes through renderThenExtractSet with the segment
 * threaded (the #347 SET twin — single resolved MULTI meta-free leaf), the leaf
 * add-method consuming the hoisted chain
 * ({@code businessEvent\n\t.addAfter(thenArg\n\t\t.flattenList().getMulti());});
 * an ARM-position EvalArgMetaDerefHoist pulls INTO its owning rung while the #356
 * chain-top window is ACTIVE (the #366 together-restructure law — ladders outside
 * any window keep the block-top drains byte-frozen, the cp2-387 class); the k==0
 * decl element keeps the BARE join when every extract-ladder leaf arm is a
 * with-args fn call whose DECLARED output is meta-free (the #339 law at the
 * with-args shape).
 *
 * <p>Whole-file byte locks run through the REAL D11 FUNCTION routes (cdm5 + cdm6 +
 * drr) and revert RED without the facets; every witness token is
 * occurrence-counted (python {@code str.count} semantics — the #352 law) and
 * PRE-counted against f-probe-390post (each removal token PRE &ge; 1 / golden 0;
 * each golden token PRE 0 / golden &ge; 1 — witness391.py).
 */
class ArmSeatSegmentAddThenDecompSextetComposeTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String CBE =
            "cdm/event/common/functions/Create_BusinessEvent.java";
    private static final String SSN =
            "cdm/margin/schedule/functions/StandardizedScheduleNotional.java";
    private static final String SSNC =
            "cdm/margin/schedule/functions/StandardizedScheduleNotionalCurrency.java";
    private static final String CSS =
            "drr/projection/dtcc/rds/harmonized/csa/rewrite/trade/functions/Create_SubmissionSchedules.java";
    private static final String ERF =
            "drr/regulation/hkma/rewrite/trade/functions/Extract_ReferenceEntityFormat.java";

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

    /** thenAddSegmentPath + addSegmentLadderArmDrain + ladderArmBareElementStamp: the cdm5 twin. */
    @Test
    @EnabledIf("cellsAvailable")
    void createBusinessEventCdm5_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, CBE);
    }

    /** thenAddSegmentPath + addSegmentLadderArmDrain + ladderArmBareElementStamp: the cdm6 twin. */
    @Test
    @EnabledIf("cellsAvailable")
    void createBusinessEventCdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, CBE);
    }

    /** armSeatChainedBareFnThen: the 15-thenArg method-session carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void standardizedScheduleNotional_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, SSN);
    }

    /** armSeatChainedBareFnThen: the NotionalCurrency twin. */
    @Test
    @EnabledIf("cellsAvailable")
    void standardizedScheduleNotionalCurrency_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, SSNC);
    }

    /** thenAddSegmentPath: the 4-add-seat + in-lambda ctor-arg ite carrier (BONUS flip). */
    @Test
    @EnabledIf("cellsAvailable")
    void createSubmissionSchedules_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, CSS);
    }

    /** the deep-route collapse typing + enum requalify carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void extractReferenceEntityFormat_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, ERF);
    }

    // ==== occurrence-counted witnesses (python str.count semantics — the #352 law) ====

    /**
     * armSeatChainedBareFnThen + collapseStepElementFromPrevRef +
     * fnCallReceiverLambdaTyping (cdm6 StandardizedScheduleNotional): the collapse
     * step types from prevRef (PRE 0 / golden 1), the fn-call receiver names its
     * step lambda from the callee output type (PRE 0 / golden 3), the terminal
     * renders the receiver-call form (PRE 0 / golden 1); negative — the
     * Object-typed partial decl is gone (PRE 2 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void standardizedScheduleNotional_armSeatDecomp_witness() {
        String gen = cdm6FnOutput.get(SSN);
        assertNotNull(gen, "StandardizedScheduleNotional not generated");
        assertEquals(1, count(gen, "final MapperS<InterestRatePayout> thenArg1 = thenArg0"),
                "the k>0 collapse step types from the preceding thenArg (PRE 0 / golden 1)");
        assertEquals(3, count(gen, "underlier -> underlier.getProduct()"),
                "the fn-call receiver's step lambda names from the callee output type"
                + " (PRE 0 / golden 3)");
        assertEquals(1, count(gen,
                "MapperS.of(standardizedScheduleMonetaryNotionalFromResolvablePQ"
                + ".evaluate(thenArg2.get())).get()"),
                "the chained bare-fn terminal renders the receiver-call form"
                + " (PRE 0 / golden 1)");
        assertEquals(0, count(gen, "final MapperS<Object> thenArg"),
                "the Object-typed partial decl is gone (PRE 2 / golden 0)");
    }

    /**
     * bareEnumComparandHierarchy + bareItemPipeSiblingEnum (drr
     * Extract_ReferenceEntityFormat): the parent-declared LEI qualifies by the
     * child enum at the filter AND ladder seats (PRE 0 / golden 2), the elseless
     * ladder's typed-empty terminal renders (PRE 0 / golden 2), CountryCode
     * requalifies at the ladder seat beside the parser-resolved filter one
     * (PRE 1 / golden 2); negative — the runtime then form is gone
     * (PRE 7 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void extractReferenceEntityFormat_enumRequalify_witness() {
        String gen = drrFnOutput.get(ERF);
        assertNotNull(gen, "Extract_ReferenceEntityFormat not generated");
        assertEquals(2, count(gen, "MapperS.of(EntityIdentifierTypeEnum.LEI)"),
                "the mis-bound bare LEI requalifies through the hierarchy at both seats"
                + " (PRE 0 / golden 2)");
        assertEquals(2, count(gen, "return MapperS.<ReferenceEntityFormatEnum>ofNull();"),
                "the mapSingleToItem ladder's typed-empty terminal renders in both"
                + " dispatch methods (PRE 0 / golden 2)");
        assertEquals(2, count(gen, "MapperS.of(EntityIdentifierTypeEnum.COUNTRY_CODE)"),
                "the bare-item pipe sibling rung requalifies the ladder-seat CountryCode"
                + " (PRE 1 / golden 2)");
        assertEquals(0, count(gen, ".then(item ->"),
                "the runtime then form is gone (PRE 7 / golden 0)");
    }

    /**
     * thenAddSegmentPath + ladderArmBareElementStamp (cdm5 Create_BusinessEvent):
     * the segment-ADD consumes the hoisted chain through the leaf add-method
     * (PRE 0 / golden 1), the LoL decl keeps the BARE arm join (PRE 0 / golden 1);
     * negative — the runtime then form is gone (PRE 1 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void createBusinessEvent_segmentAddThenArg_witness() {
        String gen = cdm5FnOutput.get(CBE);
        assertNotNull(gen, "Create_BusinessEvent not generated");
        assertEquals(1, count(gen, ".addAfter(thenArg"),
                "the leaf add-method consumes the hoisted chain (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final MapperListOfLists<TradeState> thenArg"),
                "the LoL decl keeps the bare fn-call arm join (PRE 0 / golden 1)");
        assertEquals(0, count(gen, ".then(item ->"),
                "the runtime then form is gone (PRE 1 / golden 0)");
    }

    /**
     * thenAddSegmentPath + the in-lambda ctor-arg ite blocks (drr
     * Create_SubmissionSchedules): the add-seat chains hoist to thenArg0..3
     * (thenArg0 PRE 0 / golden 1; the CdePriceSchedule consumer PRE 0 / golden 1)
     * and the mapItem block lambdas hoist the ctor-arg conditionals
     * (PRE 0 / golden 2); negatives — the runtime then form and the non-compiling
     * MapperC.of().get() else arm are gone (PRE 4 and 2 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void createSubmissionSchedules_inLambdaIte_witness() {
        String gen = drrFnOutput.get(CSS);
        assertNotNull(gen, "Create_SubmissionSchedules not generated");
        assertEquals(1, count(gen, "final MapperC<NotionalPeriod> thenArg0"),
                "the first add-seat chain hoists (PRE 0 / golden 1)");
        assertEquals(2, count(gen, "final BigDecimal ifThenElseResult;"),
                "the in-lambda ctor-arg conditionals hoist per mapItem block"
                + " (PRE 0 / golden 2)");
        assertEquals(1, count(gen, ".addCdePriceSchedule(thenArg2"),
                "the third leaf add-method consumes its hoisted chain (PRE 0 / golden 1)");
        assertEquals(0, count(gen, ".then(item ->"),
                "the runtime then form is gone (PRE 4 / golden 0)");
        assertEquals(0, count(gen, " : MapperC.of().get())"),
                "the non-compiling empty-else splice is gone (PRE 2 / golden 0)");
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
