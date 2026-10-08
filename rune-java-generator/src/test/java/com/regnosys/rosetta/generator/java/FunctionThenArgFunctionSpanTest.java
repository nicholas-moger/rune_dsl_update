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
 * PR #251 — facet thenArgFunctionSpanning ({@code FunctionExpressionRenderer.renderThenExtractSet}).
 *
 * <p>The SET-position then-hoist named its {@code thenArg} locals LOCALLY per then-chain
 * ({@code (n == 1) ? "thenArg" : "thenArg" + k}), so two single-then chains in SEPARATE
 * conditional arms each rendered the bare {@code thenArg}. Upstream's {@code declareAsVariable}
 * numbers every collapsed then-arg in one assignOutput METHOD as ONE group, so golden renders
 * {@code thenArg0}/{@code thenArg1} function-spanning. The {@code thenArg} naming is now routed
 * through the per-method {@link com.regnosys.rosetta.generator.java.function.StatementHoistSession}
 * (when active) exactly as the {@code boolean} / {@code ifThenElseResult} groups (#179/#173) and
 * #250's {@code CollectionHandler.tryDeepThenHoist} already do; the rule path (no session) keeps
 * the local naming (today's bytes).
 *
 * <p><b>Flip carrier:</b> drr {@code GetLeg1ResolvablePriceQuantity} — a single-then chain in the
 * {@code if (boolean0)} arm + another in the nested {@code else}/{@code if (boolean1)} arm, now
 * numbered {@code thenArg0}/{@code thenArg1} function-spanning (the bodies were already
 * byte-identical to golden).
 *
 * <p><b>Green-safe by construction.</b> ZERO of the 34,686 corpus-9.83.0 goldens carry a runtime
 * {@code .then(} form, and any method whose SET-position thenArgs golden spans (>=2) was rendered
 * bare by the prior per-chain naming — already a waivered mismatch. A lone-thenArg method stays
 * bare {@code thenArg} (singleton group) — byte-identical (the green-safety lock below). Verified:
 * the full all-kinds gensuite (D11 20/20) + the PRE/POST waivered-population regscan (1 flipped-out
 * = GetLeg1, 0 regressions, 6 improvement-churn toward golden).
 *
 * <p>Whole-file byte anchors through the REAL D11 loader. REVERT-VERIFIED RED.
 */
class FunctionThenArgFunctionSpanTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path CDM6_GOLDEN_DIR = CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_GOLDEN_DIR = DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;
    private static Map<String, String> drrFunctionOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR)
                && Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            cdm6FunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
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

    // ---- flip lock: function-spanning thenArg numbering ----

    /**
     * drr {@code GetLeg1ResolvablePriceQuantity}: two single-then SET-position chains in separate
     * conditional arms must number {@code thenArg0}/{@code thenArg1} function-spanning (not two
     * bare {@code thenArg}). Reverting the {@code renderThenExtractSet} session routing reverts
     * this anchor to the per-chain bare {@code thenArg} (RED).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void drr_getLeg1ResolvablePriceQuantity_thenArgSpansFunction() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/GetLeg1ResolvablePriceQuantity.java");
    }

    // ---- green-safety lock: a lone thenArg stays bare ----

    /**
     * cdm6 {@code ExtractCounterpartyByRole} is a GREEN single-then SET-position function — its one
     * {@code final MapperC<Counterparty> thenArg = …} must keep the bare {@code thenArg} (the
     * {@link com.regnosys.rosetta.generator.java.function.StatementHoistSession} singleton group
     * resolves bare, NOT {@code thenArg0}). The function-spanning routing must not spuriously number
     * a lone thenArg; asserting it stays byte-matching golden guards the singleton path (RED if the
     * routing numbered every thenArg unconditionally).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_extractCounterpartyByRole_loneThenArgStaysBare() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/base/staticdata/party/functions/ExtractCounterpartyByRole.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> cellOutput, Path goldenDir, String path)
            throws IOException {
        assertNotNull(cellOutput, "Function generation did not run — corpus unavailable?");
        String generated = cellOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — PR #251 thenArg function-spanning numbering (renderThenExtractSet routed through "
                + "the per-method StatementHoistSession). Reverting the routing reverts this anchor (RED).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
