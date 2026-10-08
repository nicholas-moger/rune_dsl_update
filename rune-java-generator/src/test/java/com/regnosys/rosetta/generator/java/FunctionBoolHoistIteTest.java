package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.generator.GenerationException;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PR #217 facet {@code boolHoistInIte} — a bare function-call condition inside an
 * {@code ifThenElseResult} hoist block hoists the item-typed Boolean and guards it
 * with the null-safe ternary (the PR #179 {@code boolean_condition_hoist} law applied
 * at the {@code ControlFlowHandler.appendConditionalChain} seat, not just
 * {@code FunctionExpressionRenderer.renderConditionalAssignment}).
 *
 * <p>GEN (pre-fix):
 * <pre>{@code if (MapperS.of(isCommoditySwap.evaluate(...)).getOrDefault(false)) { ... }}</pre>
 * GOLDEN:
 * <pre>{@code final Boolean boolean0 = isCommoditySwap.evaluate(...);
 * if ((boolean0 == null ? false : boolean0)) { ... }}</pre>
 *
 * <p>The condition is rendered by the {@code ifThenElseResult} hoist machinery
 * ({@code buildIfThenElseHoistBlock} -> {@code appendConditionalChain}), which previously
 * always emitted the inline {@code .getOrDefault(false)}. The fix applies the SAME boolean
 * hoist that PR #179 applies in {@code renderConditionalAssignment}, gated by the shared
 * {@code HandlerHelper.isBareFunctionCallCondition} predicate + the structural
 * {@code MapperS.of} invocation-wrap witness ({@code unwrapToBuilder().isPresent()}). The
 * decl rides the sink's {@code StatementHoistSession} {@code boolean} group: a single hoist
 * escapes to {@code _boolean}, n>=2 number {@code boolean0..n-1} (the multi-condition
 * {@code *PartyLeg*} / {@code Direction2*} carriers exercise the numbered form; the
 * {@code Create_Margin*} carriers exercise the {@code _boolean} singleton).
 *
 * <p>Green-safe by construction: zero byte-identical goldens carry the inline
 * {@code MapperS.of(<fn>.evaluate(...)).getOrDefault(false)} condition in this seat (upstream
 * always hoists a bare fn-call condition), so the rewrite only touches currently-waivered
 * output. Whole-file byte anchors through the REAL D11 loader over all 12 clean carriers (all
 * drr). REVERT-VERIFIED RED: reverting the {@code appendConditionalChain} boolHoist arm reverts
 * these anchors to the inline {@code MapperS.of(...).getOrDefault(false)} form.
 */
class FunctionBoolHoistIteTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR = DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> drrFunctionOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            drrFunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell) throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(), D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = gen.generateWithErrors(output);
        assertTrue(genErrors.isEmpty(), cell + " FUNCTION generation reported errors: " + genErrors);
        return output;
    }

    /** margin projection — the `_boolean` singleton form (one bare fn-call condition). */
    @Test
    @EnabledIf("drrCellAvailable")
    void createMarginCorrectionData_singletonBoolHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/projection/iso20022/fca/ukemir/refit/margin/functions/Create_MarginCorrectionData.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void createMarginUpdateData_singletonBoolHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/projection/iso20022/fca/ukemir/refit/margin/functions/Create_MarginUpdateData.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void createMarginReportData_singletonBoolHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/projection/iso20022/jfsa/rewrite/margin/functions/Create_MarginReportData.java");
    }

    /** cftc PayerParty/ReceiverParty legs — the numbered `boolean0..n-1` multi-condition form. */
    @Test
    @EnabledIf("drrCellAvailable")
    void payerPartyLeg1_numberedBoolHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/functions/PayerPartyLeg1.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void payerPartyLeg2_numberedBoolHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/functions/PayerPartyLeg2.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void receiverPartyLeg1_numberedBoolHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/functions/ReceiverPartyLeg1.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void receiverPartyLeg2_numberedBoolHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/functions/ReceiverPartyLeg2.java");
    }

    /** iosco cde Direction2 party legs — a second package proving the law is seat-, not package-, scoped. */
    @Test
    @EnabledIf("drrCellAvailable")
    void direction2PayerPartyLeg1_numberedBoolHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/standards/iosco/cde/version1/party/functions/Direction2PayerPartyLeg1.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void direction2PayerPartyLeg2_numberedBoolHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/standards/iosco/cde/version1/party/functions/Direction2PayerPartyLeg2.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void direction2ReceiverPartyLeg1_numberedBoolHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/standards/iosco/cde/version1/party/functions/Direction2ReceiverPartyLeg1.java");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void direction2ReceiverPartyLeg2_numberedBoolHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/standards/iosco/cde/version1/party/functions/Direction2ReceiverPartyLeg2.java");
    }

    /** iosco cde price — a single-condition carrier in a third package. */
    @Test
    @EnabledIf("drrCellAvailable")
    void getReportablePricePeriod_boolHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/standards/iosco/cde/version1/price/functions/GetReportablePricePeriod.java");
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(drrFunctionOutput, "Function generation did not run — corpus unavailable?");
        String generated = drrFunctionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — a bare function-call condition inside an ifThenElseResult hoist must hoist "
                + "`final Boolean <id> = fn.evaluate(...);` and guard with `(<id> == null ? false : <id>)`; "
                + "reverting ControlFlowHandler.appendConditionalChain's boolHoist arm reverts this anchor "
                + "to the inline `MapperS.of(fn.evaluate(...)).getOrDefault(false)` (RED).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
