package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #335 anchors — {@code collapsedMetaDerefFunctionSink} (3 flips, drr FUNCTION).
 *
 * <p>The #317 collapsed-meta-deref hoist+reconstruct (a wrapper-typed
 * {@code <chain>.get()} navigated to a bare value feature — {@code
 * NavigationHandler.collapsedMetaDerefRewrapOrNull}) was frozen to the RULE path by the
 * #232 {@code findEnclosingRule} gate. The #262 unconditional {@code hoistSessionEligible}
 * already marks every function SET-body statement scope a statement-hoist sink, so the
 * ONLY blocker for the FUNCTION path was that rule gate. #335 opens it when {@code
 * findStatementHoistSink() != null} — the whole-output statement-sink path. The firing
 * surface is the reachable statement sink, NOT "every non-lambda seat": a reduce/min
 * DATA-TRANSFORM lambda interior ({@code .min(item -> …)}) is NOT a sink (the walk stops
 * at its {@code lambdaScope} boundary, so {@code findStatementHoistSink()} is null and the
 * gate still declines — CommodityBasisLeg1's A3 reduce-lambda drain was deferred here and
 * CONVERTED at PR #342 via the MAXMIN_KEY collapsed-key pending-lambda-hoist arm, the
 * flip locks below),
 * whereas a condition-validator SUPPLIER lambda body ({@code conditionValidator.validate(()
 * -> { … })}) IS a statement-hoist sink and DOES fire on the function path (the composed
 * Create_CashflowFromSettlementPayout heal — 2 of its 3 collapse seats hoist TOWARD golden;
 * it stays waivered on a residual {@code ComparisonResult} import, a #336 lead). The #232
 * FUNCTION-byte-neutral freeze is preserved for every function WITHOUT a reachable sink.
 *
 * <p>The three IsCommodity{Forward,Option,TotalReturnSwap}_SingleIndex carriers hoist the
 * {@code final ReferenceWithMetaProductIdentifier referenceWithMetaProductIdentifier =
 * <chain>.get();} decl before the {@code result =} assignment and consume the null-guarded
 * {@code (w == null ? MapperS.<ProductIdentifier>ofNull() : MapperS.of(w.getValue()))}
 * reconstruct, adding the {@code ProductIdentifier} import — the emission machinery
 * ({@code collapsedMetaDerefRewrapOrNull}'s STATEMENT_SINK branch) was already
 * byte-faithful; #335 only fires its gate on the function path.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of generated output against the frozen
 * 9.83.0 goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} FUNCTION-kind path
 * (function_comparison). Green-safety: the pre-fix form navigated a value getter on a
 * wrapper-typed lambda param ({@code MapperS.of(<wrapper>.get()).<Value>map(...)}), which
 * does not compile, so no green FUNCTION golden carries it; the D11 strict gate (20/20
 * green pre- and post-facet) is the mechanical backstop.
 */
class FunctionCollapsedMetaDerefSinkTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // ==== flip carriers (the #317 hoist+reconstruct opened to the FUNCTION sink) ====
    private static final String IS_COMMODITY_FORWARD =
            "drr/regulation/common/functions/IsCommodityForward_SingleIndex.java";
    private static final String IS_COMMODITY_OPTION =
            "drr/regulation/common/functions/IsCommodityOption_SingleIndex.java";
    private static final String IS_COMMODITY_TRS =
            "drr/regulation/common/functions/IsCommodityTotalReturnSwap_SingleIndex.java";

    // ==== LAMBDA_CHANNEL flip carrier (the A3 deferral CONVERTED at PR #342) ====
    // CommodityBasisLeg1's collapsed-meta-deref sits inside a `.min(item -> ...)` lambda;
    // the #335-era sink walk stopped at the lambda boundary and declined. PR #342's
    // MAXMIN_KEY collapsed-key arm routes the hoist through the pending-lambda-hoist
    // channel instead (block-form lambda), so the file now byte-matches golden.
    private static final String COMMODITY_BASIS_LEG1_DECLINE =
            "drr/regulation/csa/rewrite/trade/functions/CommodityBasisLeg1.java";

    private static Map<String, String> drrFnOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            drrFnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    /** The REAL D11 FUNCTION-kind generation path (function_comparison). */
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

    // ==== flip locks (revert-RED) ====

    @Test
    @EnabledIf("cellsAvailable")
    void isCommodityForward_functionSinkHoistReconstruct_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, IS_COMMODITY_FORWARD);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void isCommodityOption_functionSinkHoistReconstruct_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, IS_COMMODITY_OPTION);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void isCommodityTotalReturnSwap_functionSinkHoistReconstruct_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, IS_COMMODITY_TRS);
    }

    // ==== LAMBDA_CHANNEL flip locks (the A3 deferral CONVERTED at PR #342) ====

    /**
     * facet minMaxKeyBlockLambdaMetaDeref (PR #342): the A3 reduce-lambda deferral this
     * class carried CONVERTS — the MAXMIN_KEY collapsed-key arm
     * ({@code CollectionHandler.maxMinCollapsedMetaKeyDerefOrNull}) hoists the wrapper
     * local into the min/max lambda via the pending-lambda-hoist drain (block form) and
     * returns the guarded {@code MapperS} reconstruct, so CommodityBasisLeg1 ({@code .min})
     * and CommodityBasisLeg2 ({@code .max}) now byte-match golden. The old decline lock's
     * SHARP witness inverts: the collapsed {@code .get()).get())} double-get counts ZERO
     * in golden AND in the correct gen (the pre-#342 un-flipped form), asserted below.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void commodityBasisLeg1_minKeyHoistReconstruct_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, COMMODITY_BASIS_LEG1_DECLINE);
        String gen = drrFnOutput.get(COMMODITY_BASIS_LEG1_DECLINE);
        assertTrue(gen.contains(".min(item -> {"),
                "CommodityBasisLeg1's min key must be the BLOCK-form lambda (the hoisted"
                        + " wrapper local + guarded reconstruct)");
        assertFalse(gen.contains(".get()).get())"),
                "The collapsed `.get()).get())` double-get is the PRE-#342 un-flipped form"
                        + " — golden carries zero, so the flipped render must not either");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void commodityBasisLeg2_maxKeyHoistReconstruct_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR,
                "drr/regulation/csa/rewrite/trade/functions/CommodityBasisLeg2.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String path) throws IOException {
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
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
