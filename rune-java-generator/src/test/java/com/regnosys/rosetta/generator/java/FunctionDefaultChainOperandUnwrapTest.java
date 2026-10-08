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
 * PR #234 — facet {@code defaultChainOperandUnwrap} (the dropped #232 candidate, done
 * properly): the RIGHT operand of a single-cardinality {@code default}
 * ({@code <l> default <r>} → {@code <l>.getOrDefault(<r>)}) must be the BARE item value,
 * mirroring upstream {@code ExpressionGenerator.binaryExpr} {@code case "default"}, which
 * compiles the right at {@code withExpected(joined)} (the bare item type). The fork's
 * compile-time coercion is dormant for the null-typed Mapper chains these fpml-ingest
 * defaults produce, so {@link com.regnosys.rosetta.generator.java.expression.handlers.SetOperationHandler}
 * reduces the right by shape and wraps the result in {@code MapperS.of} WITH the
 * unwrap contract (a Mapper consumer — a return — keeps the wrap; a bare consumer —
 * evaluate-arg / toBuilder / setter — strips it via {@code unwrapToBuilder}).
 *
 * <p>11 cdm6 FUNCTION flips, each locked here by a WHOLE-FILE byte anchor through the
 * REAL {@link D11CorpusRegressionTest#loadCellCorpusCached} + {@link FunctionGenerator}
 * (newline-normalized, revert-RED). The anchors below cover the distinct mechanisms /
 * consumer positions:
 *
 * <ul>
 *   <li><b>nav-chain → {@code .get()}, return consumer</b> keeps the {@code MapperS.of}
 *       wrap: {@code MapBondOptionPayout}, {@code MapCalculationPeriodAmountToPriceList}
 *       ({@code return MapperS.of(<l>.getOrDefault(<chain>.get()))});</li>
 *   <li><b>nav-chain → {@code .get()}, bare consumer</b> strips the wrap:
 *       {@code MapInterestLegToPriceQuantity} (evaluate-arg),
 *       {@code GetInterestRatePriceCurrency} (toBuilder)
 *       ({@code <l>.getOrDefault(<chain>.get())});</li>
 *   <li><b>unwrap a {@code MapperS.of(value)} wrap (bare param / var)</b>:
 *       {@code MapOptionStrikePrice} (setter → {@code getOrDefault(fpmlStrikePercentage)}),
 *       {@code MapPaymentListToTransferStateList} (evaluate-arg → {@code getOrDefault(cdmFeeType)},
 *       dropping the spurious trailing {@code .get()}).</li>
 * </ul>
 *
 * <p>The remaining 5 of the 11 flips ({@code MapBondOptionPriceQuantityList},
 * {@code MapBrokerEquityOptionPayout}, {@code MapCapfloorCalculationPeriodAmountToPriceList},
 * {@code MapCommodityNotionalQuantityToQuantityListWithLocation}, {@code MapSwapOptionStrikePrice})
 * share these same mechanisms and are locked globally by the D11 byte-compare matrix (20/20),
 * not individually anchored here.
 *
 * <p><b>Green-safety lock</b> — {@code Create_SubmissionHeader} (drr): its {@code default}
 * right is a function {@code .evaluate()} VALUE call ({@code getOrDefault(now.evaluate())}),
 * the exact shape that regressed 8 green drr functions in the dropped #232 prototype. It
 * DECLINES the reduction (a value-call is neither a nav-chain nor an unwrappable wrap), so
 * it stays byte-identical. This anchor turns RED if the decline gate is ever dropped (the
 * #232 trap). Green-safety in full is established by the 5-cell D11 matrix (20/20) + the
 * PRE/POST within-waiver regression scan (0 regressions); these anchors lock the byte form.
 */
class FunctionDefaultChainOperandUnwrapTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;
    private static Map<String, String> drrFunctionOutput;

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
        if (cdm6CellAvailable()) {
            cdm6FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
        if (drrCellAvailable()) {
            drrFunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = gen.generateWithErrors(output);
        assertTrue(genErrors.isEmpty(),
                cell + " FUNCTION generation reported errors: " + genErrors);
        return output;
    }

    // ---- nav-chain → .get(), return consumer KEEPS the MapperS.of wrap ----------

    /**
     * MapBondOptionPayout (cdm6) — {@code return MapperS.of(<bond-choice-chain>.getOrDefault(
     * <convertible-bond-chain>.get()));} (golden). The fork emitted
     * {@code return <chain>.getOrDefault(<chain>);} (right not unwrapped, result not wrapped).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapBondOptionPayout_navChainReturn_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/bondoption/functions/MapBondOptionPayout.java");
    }

    /** MapCalculationPeriodAmountToPriceList (cdm6) — a second nav-chain / return carrier. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapCalculationPeriodAmountToPriceList_navChainReturn_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/swap/functions/MapCalculationPeriodAmountToPriceList.java");
    }

    // ---- nav-chain → .get(), bare consumer STRIPS the wrap ---------------------

    /**
     * MapInterestLegToPriceQuantity (cdm6) — the {@code default} feeds an evaluate-arg:
     * golden {@code ...getOrDefault(<returnLeg-currency-chain>.get()), fpmlInterestLeg)} —
     * the {@code .get()} moves INSIDE the right operand and the spurious trailing
     * {@code .get()} on the result disappears (the evaluate-arg strips the MapperS.of wrap).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapInterestLegToPriceQuantity_navChainEvaluateArg_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/returnswap/functions/MapInterestLegToPriceQuantity.java");
    }

    /**
     * GetInterestRatePriceCurrency (cdm6) — the {@code default} feeds a {@code toBuilder(...)}
     * arg: golden {@code toBuilder(<chain>.getOrDefault(<fx-linked-chain>.get()))} (the wrap
     * is stripped; no trailing {@code .get()}).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void getInterestRatePriceCurrency_navChainToBuilder_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/swap/functions/GetInterestRatePriceCurrency.java");
    }

    // ---- unwrap a MapperS.of(value) wrap (bare param / var) --------------------

    /**
     * MapOptionStrikePrice (cdm6) — a bare param right operand:
     * {@code .setValue(MapperS.of(fpmlStrikePrice).getOrDefault(fpmlStrikePercentage))} (golden);
     * the fork wrapped the param {@code getOrDefault(MapperS.of(fpmlStrikePercentage))}.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapOptionStrikePrice_unwrapBareParam_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/pricequantity/functions/MapOptionStrikePrice.java");
    }

    /**
     * MapPaymentListToTransferStateList (cdm6) — a bare var right operand inside an
     * evaluate-arg: golden {@code getOrDefault(cdmFeeType)} (the {@code MapperS.of(cdmFeeType)}
     * wrap AND the spurious trailing {@code .get()} both disappear).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapPaymentListToTransferStateList_unwrapBareVar_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/payment/functions/MapPaymentListToTransferStateList.java");
    }

    // ---- green-safety: a function value-call right DECLINES (the #232 trap) -----

    /**
     * Create_SubmissionHeader (drr) — its {@code default} right is the function value call
     * {@code now.evaluate()}: golden {@code <reportingTimestamp-chain>.getOrDefault(now.evaluate())}.
     * A value call is neither a nav-chain nor an unwrappable {@code MapperS.of} wrap, so the
     * reduction DECLINES and the output stays byte-identical. This is the exact shape that
     * regressed 8 green drr functions in the dropped #232 prototype (which appended a
     * spurious {@code .get()}); this anchor turns RED if the value-call decline is dropped.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void createSubmissionHeader_valueCallDeclines_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/dtcc/rds/harmonized/cftc/rewrite/trade/functions/Create_SubmissionHeader.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String path) throws IOException {
        assertNotNull(output, "Function generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " — PR #234 defaultChainOperandUnwrap.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
