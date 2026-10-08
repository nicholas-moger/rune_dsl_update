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
 * PR #212 facet isoSingletonSetCoerce (23 FUNCTION flips: cdm5 2 + cdm6 3 + drr 18), whole-file
 * byte anchors through the REAL D11 loader. Each anchor reverts RED if the fix is removed; the law
 * is green-safe by construction (a single value spliced BARE into a {@code List} setter does not
 * compile, so every carrier was already a waivered mismatch — the stash-baseline confirmed 0
 * now-matching pristine, and {@code function_comparison} stays green).
 *
 * <p><b>The law.</b> A deep-path {@code set}/{@code add} to a MULTI (List) leaf whose value is
 * PROVABLY single-cardinality — a CONSTRUCTOR ({@code RConstructorExpr} — one built object) or a
 * direct call to a SINGLE-output function ({@code RSymbolReference} -> {@code RFunction}) — coerces
 * the value single-&gt;list. Upstream {@code FunctionGenerator.xtend assign} coerces every operation
 * value to the path-leaf attribute's Java type (a {@code List} for a multi leaf), and
 * {@code TypeCoercionService.convertNullSafe} null-safe-converts a single value (consumed TWICE, so
 * it hoists to a {@code final} local):
 *
 * <pre>
 * final &lt;Item&gt; &lt;name&gt; = &lt;value&gt;;
 * &lt;target&gt;.getOrCreate&lt;Intermediate&gt;().set&lt;Leaf&gt;((&lt;name&gt; == null
 *     ? Collections.&lt;Item&gt;emptyList() : Collections.singletonList(&lt;name&gt;)));
 * </pre>
 *
 * The fork spliced the single value BARE ({@code .setRpt(create_X.evaluate(...))} /
 * {@code .addRpt(X.builder()...build())}). The fix is the setter-chain analog of #198
 * {@code ConstructionHandler.hoistSingleValueIntoMultiOrNull}, shared by the SET seat
 * ({@code FunctionExpressionRenderer.renderSetSingleIntoMultiOrNull}) and the ADD seat
 * ({@code renderAddSegmentChainOrNull}) via {@code hoistSingleValueIntoMultiLeafOrNull}; a
 * provably-MULTI callee output, a meta leaf, or a base-name scope collision decline.
 *
 * <p>Carriers span both seats, both value shapes, and all three function-bearing cells: the 18
 * {@code Project_*ToIso20022} drr projections (the SET-seat {@code *ToIso20022} +
 * the ADD-seat HKMA variants) plus bonus cdm carriers ({@code SetCashCurrency} SET-ctor,
 * {@code Create_Execution} / {@code Create_SecurityLendingInvoice} ADD-ctor) the same law unlocked.
 */
class FunctionPr212IsoSingletonSetCoerceTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path CDM5_GOLDEN_DIR = CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_GOLDEN_DIR = CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_GOLDEN_DIR = DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm5FunctionOutput;
    private static Map<String, String> cdm6FunctionOutput;
    private static Map<String, String> drrFunctionOutput;

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
            cdm5FunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
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

    private static final String REVERT =
            "the hoist (final <Item> v = <value>;) + null-guarded singletonList coercion reverts to "
            + "the bare single value spliced into the List setter/adder — the "
            + "renderSetSingleIntoMultiOrNull / renderAddSegmentChainOrNull arm (the shared "
            + "hoistSingleValueIntoMultiLeafOrNull) was removed";

    // --- drr Project_*ToIso20022 : SET seat ---

    /** SET-segment + single-output fn-call value: {@code .setRpt(create_TradeReport33Choice__1.evaluate(...))}. */
    @Test
    @EnabledIf("cellsAvailable")
    void drrAsicTrade_setSegmentFnCall_byteMatchesGolden() throws IOException {
        assertByteMatches(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/asic/rewrite/trade/functions/Project_ASICTradeReportToIso20022.java", REVERT);
    }

    /** SET-segment + CONSTRUCTOR-block value: {@code .setRpt(TradeReport34Choice__1.builder()...build())}. */
    @Test
    @EnabledIf("cellsAvailable")
    void drrAsicMargin_setSegmentCtor_byteMatchesGolden() throws IOException {
        assertByteMatches(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/asic/rewrite/margin/functions/Project_ASICMarginReportToIso20022.java", REVERT);
    }

    /** A second SET-seat regime (esma emir refit trade) — same fn-call law, different namespace. */
    @Test
    @EnabledIf("cellsAvailable")
    void drrEsmaEmirTrade_setSegmentFnCall_byteMatchesGolden() throws IOException {
        assertByteMatches(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/esma/emir/refit/trade/functions/Project_EsmaEmirTradeReportToIso20022.java", REVERT);
    }

    // --- drr Project_*ToIso20022 : ADD seat (the HKMA variants) ---

    /** ADD-segment + fn-call value: {@code .addRpt(create_TradeReport33Choice__1.evaluate(...))}. */
    @Test
    @EnabledIf("cellsAvailable")
    void drrHkmaDtccTrade_addSegmentFnCall_byteMatchesGolden() throws IOException {
        assertByteMatches(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/hkma/rewrite/trade/dtcc/functions/Project_HKMADtccTradeReportToIso20022.java", REVERT);
    }

    /** ADD-segment + CONSTRUCTOR-block value: {@code .addRpt(TradeReport34Choice__1.builder()...build())}. */
    @Test
    @EnabledIf("cellsAvailable")
    void drrHkmaDtccMargin_addSegmentCtor_byteMatchesGolden() throws IOException {
        assertByteMatches(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/hkma/rewrite/margin/dtcc/functions/Project_HKMADtccMarginReportToIso20022.java", REVERT);
    }

    // --- cdm bonus carriers (the same law unlocked beyond the Iso20022 family) ---

    /** cdm6 SET-segment + ctor value: {@code SetCashCurrency.setIdentifier(AssetIdentifier.builder()...)}. */
    @Test
    @EnabledIf("cellsAvailable")
    void cdm6SetCashCurrency_setSegmentCtor_byteMatchesGolden() throws IOException {
        assertByteMatches(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/base/staticdata/asset/common/functions/SetCashCurrency.java", REVERT);
    }

    /** cdm6 ADD-segment + ctor value (after/tradeLot into the created TradeState). */
    @Test
    @EnabledIf("cellsAvailable")
    void cdm6CreateExecution_addSegmentCtor_byteMatchesGolden() throws IOException {
        assertByteMatches(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/common/functions/Create_Execution.java", REVERT);
    }

    /** cdm5 Create_Execution — the same law one model version back. */
    @Test
    @EnabledIf("cellsAvailable")
    void cdm5CreateExecution_addSegmentCtor_byteMatchesGolden() throws IOException {
        assertByteMatches(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/common/functions/Create_Execution.java", REVERT);
    }

    private static void assertByteMatches(Map<String, String> output, Path goldenDir,
            String path, String revertHint) throws IOException {
        assertNotNull(output, "Function generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — " + revertHint + ".");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
