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
 * PR #382 — the ladder-arm/metaSigKeep/blank-final-enum/type-switch quartet (4 byte
 * flips, all FUNCTION: TotalNotionalQuantityLeg2 iosco cde v1 [drr] +
 * StandardizedScheduleFXSwapNotional + MapExecutionDetails + MapAccount [cdm6]).
 *
 * <p><b>D4b-2 inLambdaLadderArmChainDecomp</b> (CollectionHandler): the FUNCTION-path
 * in-lambda ladder admits rung arms whose ONLY control flow is an arm-ROOT
 * drain-admissible then-chain under a then-free {@code default} wrap — the single-slot
 * node-identity {@code ladderArmDrainChainWindow} (the #377-L1 pattern) is pushed
 * around BOTH the decision and the arm compile, the {@code isCleanLadderContext}
 * #350-F1/#356 scoping law relaxes to
 * {@code condLadderThenConfinedToDrainableArmChains}, and the all-or-nothing guard
 * exempts ONLY the window-blessed node (scoped to the {@code default}-wrapped class —
 * a PLAIN then-chain arm keeps the exact pre-#382 predicate, the regscan382
 * Create_AnnaDsb* numbering-cascade catch). The metaSigKeep wrapper-keep law renders
 * FREE through the existing machinery once the hoist fires: the in-rung decl keeps
 * the wrapper ({@code final MapperS<ReferenceWithMetaNonNegativeQuantitySchedule>
 * thenArg}) and the inner lambda derefs per-use with NUMBERED coercion params
 * ({@code referenceWithMetaNonNegativeQuantitySchedule0..3}).
 *
 * <p><b>metaSigKeep</b> (CollectionHandler + NavigationHandler +
 * FunctionExpressionRenderer): the alias-signature wrapper-keep law through ONE
 * channel ({@code tryAliasReceiverMapperType} — the SAME walk the alias method's
 * {@code Mapper*<? extends FieldWithMetaX>} return type renders from, the #178
 * same-walk invariant): the deep-then level decl probe looks through
 * element-preserving FILTER steps (the signature wrapper WINS over the #238
 * alias-leaf arm's meta-blind body walk), the filter/sort predicate ITEM re-stamps
 * from the alias signature (the #375-A4-c sibling), the whole-output SET gains the
 * alias-signature rung (the guarded {@code toBuilder(<w>.getValue())} deref block)
 * and the sort comparator KEY meta-strips its terminal
 * ({@code LambdaBodyPosition.SORT_KEY} = the MAXMIN arm-A coercion and nothing else).
 *
 * <p><b>E-i blankFinalCtorLadderEnumJoin</b> (ControlFlowHandler): a #204-class
 * MIS-BOUND bare enum then-arm ({@code CashPrice} binds the RDataType CashPrice)
 * types AND qualifies from a SIBLING ladder arm's parser-BOUND enum value
 * ({@code InterestRate} → REnumValue → PriceTypeEnum) when the mis-bound name
 * matches the enum hierarchy — one evidence channel for both the decl-type recovery
 * and the {@code PriceTypeEnum.CASH_PRICE} arm render, unlocking the existing #203
 * blank-final elseful ladder + the bare {@code .setPriceType(ifThenElseResult)}
 * consumer.
 *
 * <p><b>E-ii/iii aliasTypeSwitchFnArm</b> (FunctionAliasHelper +
 * FunctionExpressionRenderer): the alias-body TYPE-switch RETURN ladder admits
 * FUNCTION-CALL case arms (the callee-output join via the SAME
 * {@code inferExpressionType} walk — MapCurrency's {@code string [metadata scheme]}
 * output joins FieldWithMetaString, fixing the signature), a NON-bare hoisted subject
 * ({@code final Product switchArgument = <arg>.get();} — the #370 assignment-seat law
 * at the alias RETURN seat, decl type from the NEW shared
 * {@code switchSubjectTypeInfoOrNull}) and multi-line fn-call arms re-anchored at the
 * arm depth. The case-scope {@code bindSwitchSubject} re-root (existing #368/#369
 * machinery) resolves the mis-bound bare {@code swapStream} through the cast var.
 * MapAccount is the BONUS flip riding the same widening.
 *
 * <p>Whole-file byte locks run through the REAL D11 FUNCTION routes (drr + cdm6) and
 * revert RED without the facets; every witness token is occurrence-counted (python
 * {@code str.count} semantics — the #352 law) and PRE-counted against
 * f-probe-381post (each removal token PRE &ge; 1 / golden 0; each golden token
 * PRE 0 / golden &ge; 1).
 */
class LadderArmMetaSigKeepQuartetComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String TOTAL_NOTIONAL_QUANTITY_LEG2 =
            "drr/standards/iosco/cde/version1/quantity/functions/TotalNotionalQuantityLeg2.java";
    private static final String FX_SWAP_NOTIONAL =
            "cdm/margin/schedule/functions/StandardizedScheduleFXSwapNotional.java";
    private static final String MAP_EXECUTION_DETAILS =
            "cdm/ingest/fpml/confirmation/tradestate/functions/MapExecutionDetails.java";
    private static final String MAP_ACCOUNT =
            "cdm/ingest/fpml/confirmation/party/functions/MapAccount.java";

    private static Map<String, String> drrFnOutput;
    private static Map<String, String> cdm6FnOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            drrFnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
        if (cdm6CellAvailable()) {
            cdm6FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
    }

    /** The REAL D11 FUNCTION-cell generation path. */
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

    // ==== byte locks (all 4 flips through the REAL D11 FUNCTION routes) ====

    /** D4b-2 + metaSigKeep: the in-lambda ladder-arm drain + wrapper-keep carrier. */
    @Test
    @EnabledIf("drrCellAvailable")
    void totalNotionalQuantityLeg2_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, TOTAL_NOTIONAL_QUANTITY_LEG2);
    }

    /** metaSigKeep: the alias-signature wrapper-keep 4-seat carrier. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void fxSwapNotional_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, FX_SWAP_NOTIONAL);
    }

    /** E-i + E-ii/iii: the blank-final enum join + alias type-switch composite. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapExecutionDetails_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_EXECUTION_DETAILS);
    }

    /** E-ii/iii: the bonus flip riding the same type-switch widening. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapAccount_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_ACCOUNT);
    }

    // ==== occurrence-counted witnesses (python str.count semantics — the #352 law) ====

    /**
     * D4b-2 + metaSigKeep: the in-rung WRAPPER-KEEP hoist (PRE 0 / golden 1), the
     * NUMBERED per-use coercion params (PRE 0 / golden 3 — the param + its two guard
     * uses), the {@code _boolean} rung hoist (PRE 0 / golden 1), the typed
     * fall-through (PRE 0 / golden 1) and the runtime-then shadow GONE (PRE 1 /
     * golden 0 — the negative witness the flip removes).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void totalNotionalQuantityLeg2_armHoistWrapperKeep_witness() {
        String gen = drrFnOutput.get(TOTAL_NOTIONAL_QUANTITY_LEG2);
        assertNotNull(gen, "TotalNotionalQuantityLeg2 not generated");
        assertEquals(1, count(gen,
                "final MapperS<ReferenceWithMetaNonNegativeQuantitySchedule> thenArg = "
                        + "MapperS.of(interestRateLeg2.evaluate(item.get()))"),
                "the in-rung hoist keeps the WRAPPER type (PRE 0 / golden 1)");
        assertEquals(3, count(gen, "referenceWithMetaNonNegativeQuantitySchedule0"),
                "the per-use coercion params number 0..3 (PRE 0 / golden 3 for the 0th)");
        assertEquals(1, count(gen, "final Boolean _boolean = isCommoditySwap.evaluate(item.get());"),
                "the bare-fn rung condition hoists _boolean (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "return MapperS.<BigDecimal>ofNull();"),
                "the elseless fall-through takes the typed empty (PRE 0 / golden 1)");
        assertEquals(0, count(gen, ".then(_item -> _item"),
                "the shadowing runtime-then must be gone (PRE 1 / golden 0)");
    }

    /**
     * metaSigKeep: the wrapper-kept thenArg0 decl (PRE 0 / golden 1), the guarded
     * whole-output deref hoist (PRE 0 / golden 1), the sort-key TERMINAL meta-strip
     * (PRE 0 / golden 1) and the two negatives the flip removes — the stripped decl
     * (PRE 1 / golden 0) and the bare toBuilder assign (PRE 1 / golden 0).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void fxSwapNotional_wrapperKeepAndGuardedDeref_witness() {
        String gen = cdm6FnOutput.get(FX_SWAP_NOTIONAL);
        assertNotNull(gen, "StandardizedScheduleFXSwapNotional not generated");
        assertEquals(1, count(gen,
                "final MapperC<? extends FieldWithMetaNonNegativeQuantitySchedule> thenArg0"),
                "the thenArg0 decl keeps the alias-signature wrapper (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                "final FieldWithMetaNonNegativeQuantitySchedule "
                        + "fieldWithMetaNonNegativeQuantitySchedule = "
                        + "extractedExchangedCurrency(farLeg, tradeLot).get();"),
                "the whole-output SET hoists the guarded deref (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                "getCurrency()).<String>map(\"Type coercion\", fieldWithMetaString -> "
                        + "fieldWithMetaString == null ? null : fieldWithMetaString.getValue()))"),
                "the sort KEY derefs its meta terminal to the Comparable String "
                        + "(PRE 0 / golden 1)");
        assertEquals(0, count(gen, "final MapperC<? extends NonNegativeQuantitySchedule> thenArg0"),
                "the meta-stripped decl must be gone (PRE 1 / golden 0)");
        assertEquals(0, count(gen,
                "quantity = toBuilder(extractedExchangedCurrency(farLeg, tradeLot).get());"),
                "the bare wrapper-into-builder assign must be gone (PRE 1 / golden 0)");
    }

    /**
     * E-i: the blank-final decl (PRE 0 / golden 1), the hierarchy-qualified mis-bound
     * arm (PRE 0 / golden 1), the bare setter consumer (PRE 0 / golden 1) and the
     * mis-bound bare render GONE (PRE 1 / golden 0).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapExecutionDetails_blankFinalEnumJoin_witness() {
        String gen = cdm6FnOutput.get(MAP_EXECUTION_DETAILS);
        assertNotNull(gen, "MapExecutionDetails not generated");
        assertEquals(1, count(gen, "final PriceTypeEnum ifThenElseResult;"),
                "the ladder hoists the blank-final decl (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "ifThenElseResult = PriceTypeEnum.CASH_PRICE;"),
                "the mis-bound CashPrice arm renders hierarchy-qualified (PRE 0 / golden 1)");
        assertEquals(1, count(gen, ".setPriceType(ifThenElseResult)"),
                "the setter consumes the sentinel BARE (PRE 0 / golden 1)");
        assertEquals(0, count(gen, "MapperS.of(CashPrice)"),
                "the mis-bound bare render must be gone (PRE 1 / golden 0)");
    }

    /**
     * E-ii/iii: the fixed alias signature (PRE 0 / golden 2 — abstract + impl), the
     * hoisted subject's cast arm (PRE 0 / golden 1), the cast-var re-rooted nav with
     * the {@code _swap} escape (PRE 0 / golden 1) and the two negatives — the old
     * signature (PRE 2 / golden 0) and the Objects.equals junk ternary (PRE 1 /
     * golden 0).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapExecutionDetails_typeSwitchReturnLadder_witness() {
        String gen = cdm6FnOutput.get(MAP_EXECUTION_DETAILS);
        assertNotNull(gen, "MapExecutionDetails not generated");
        assertEquals(2, count(gen, "MapperS<? extends FieldWithMetaString> swapStreamNotionalCurrency"),
                "the alias signature joins the callee's meta output (PRE 0 / golden 2)");
        assertEquals(1, count(gen, "final Swap swap = (Swap) switchArgument;"),
                "the instanceof arm declares the cast local (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "_swap -> _swap.getSwapStream()"),
                "the mis-bound swapStream re-roots on the cast var with the _swap escape "
                        + "(PRE 0 / golden 1)");
        assertEquals(0, count(gen, "MapperS<? extends ExecutionDetails> swapStreamNotionalCurrency"),
                "the enclosing-output signature echo must be gone (PRE 2 / golden 0)");
        assertEquals(0, count(gen, "Objects.equals(fpml.Swap"),
                "the junk type-compare ternary must be gone (PRE 1 / golden 0)");
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        int i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String path) throws IOException {
        assertNotNull(output, "generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "Class not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path);
    }
}
