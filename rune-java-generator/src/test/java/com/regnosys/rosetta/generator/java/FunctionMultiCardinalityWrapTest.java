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
 * Facet {@code multi_cardinality_value_wrap} — a RENDERER-ONLY fix
 * ({@code ReferenceHandler} + {@code JavaExpression} wrap factory) that wraps a
 * MULTI-cardinality value as the witnessed {@code MapperC.<T>of(x)} the upstream
 * 9.83.0 goldens carry, instead of the cardinality-blind {@code MapperS.of(x)}.
 *
 * <p>Two emission shapes share the one root cause
 * ({@code ReferenceHandler.handle(RSymbolReference)} is cardinality-blind):
 * <ul>
 *   <li><b>Bare variable</b> — a reference to a multi-cardinality ({@code (0..*)} /
 *       {@code (1..*)}) function input renders {@code MapperS.of(param)} where the
 *       golden has {@code MapperC.<Item>of(param)} (the param's Java type is
 *       {@code List<? extends Item>}, so the MapperS form is a non-compiling
 *       single-where-List mismatch wherever it survives to the output).</li>
 *   <li><b>Function-call result</b> — an explicit-args call to a function whose
 *       OUTPUT is multi renders {@code MapperS.of(callee.evaluate(...))} where the
 *       golden has {@code MapperC.<Item>of(callee.evaluate(...))} ({@code evaluate}
 *       returns {@code List<? extends Item>}).</li>
 * </ul>
 *
 * <p>Upstream reference (in-tree 9.83.0 {@code ExpressionGenerator.xtend}): a symbol
 * reference / {@code evaluateCall} carries its NATURAL type ({@code List<T>} for
 * multi) and the expected-type-driven coercion picks the wrapper from cardinality —
 * multi coerces to {@code MapperC.<itemType>of(...)}, single to {@code MapperS.of(...)}.
 * The fork wraps eagerly at the reference site, so the cardinality gate lives there.
 *
 * <p>This test is REVERT-VERIFIED RED against the renderer: it generates through
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} (the REAL D11 loader — for the
 * drr cell that includes the transitive-CDM and transitive-ISO20022 closures), so
 * reverting the {@code ReferenceHandler} cardinality gate turns every anchor RED with
 * the {@code MapperS.of} shape.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized) — fragment assertions are insufficient per the PR #153 lesson
 * (the wrap swap also flips the {@code MapperC}/{@code MapperS} imports via the wrap
 * factory's refs channel, and a fragment test cannot see a missing import):
 * <ul>
 *   <li>cdm/5.38.0 {@code IsHoliday} — multi-OUTPUT call result returned from an
 *       alias body ({@code return MapperC.<Date>of(businessCenterHolidaysMultiple
 *       .evaluate(businessCenters));}) — also locks the raw multi-var evaluate-arg
 *       ({@code businessCenters} passes UNWRAPPED, proving the structural-unwrap
 *       contract carries over to the MapperC wrap);</li>
 *   <li>cdm/5.38.0 {@code IndexValueObservationMultiple} — multi input param wrapped
 *       as a navigation receiver inside an {@code addAll}
 *       ({@code observedValues.addAll(MapperC.<Date>of(observationDate)…)});</li>
 *   <li>cdm/5.38.0 {@code GetRateScheduleAmount} — multi-OUTPUT call result at an
 *       assignment with a chained tail
 *       ({@code amount = MapperC.<BigDecimal>of(getRateScheduleStepValues.evaluate(…))…});</li>
 *   <li>drr/6.34.1 {@code OtherPaymentDate_Validation} — multi input param wrapped as
 *       a NAVIGATED receiver in comparison/existence operands
 *       ({@code exists(MapperC.<OtherPayment>of(otherPayment).<BigDecimal>map(…))});</li>
 *   <li>drr/6.34.1 {@code Compute_NotionalScheduleType} — multi input param in a
 *       {@code notExists} operand ({@code notExists(MapperC.<BigDecimal>of(values))}).</li>
 * </ul>
 */
class FunctionMultiCardinalityWrapTest {

    private static final Path CDM_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM_GOLDEN_DIR =
            CDM_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path ISO_DEP_ROSETTA_DIR =
            Path.of("../test-corpus/iso20022/iso20022-1.38.0/rosetta-source/src/main/rosetta");

    private static Map<String, String> cdmFunctionOutput;
    private static Map<String, String> drrFunctionOutput;

    static boolean cdmCellAvailable() {
        return Files.isDirectory(CDM_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM_GOLDEN_DIR);
    }

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR)
                && Files.isDirectory(CDM_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(ISO_DEP_ROSETTA_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
        if (cdmCellAvailable()) {
            cdmFunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM_CELL_ROOT));
        }
        if (drrCellAvailable()) {
            drrFunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        // The REAL D11 loader (cached; shares the workspace with a same-JVM D11 run) —
        // builtins + (for drr) the transitive-CDM and transitive-ISO20022 closures
        // + the cell's own version-stamped models.
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

    /**
     * Multi-OUTPUT function-call result returned from an alias body:
     * {@code return MapperC.<Date>of(businessCenterHolidaysMultiple.evaluate(businessCenters));}
     * — the callee declares {@code output: holidayDates date (0..*)}. The multi input
     * {@code businessCenters} passes into {@code evaluate(...)} RAW (structural unwrap).
     */
    @Test
    @EnabledIf("cdmCellAvailable")
    void isHoliday_multiOutputCallInAliasReturn_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdmFunctionOutput, CDM_GOLDEN_DIR,
                "cdm/base/datetime/functions/IsHoliday.java");
    }

    /**
     * Multi input param ({@code observationDate date (1..*)}) wrapped as a navigation
     * receiver inside an {@code addAll}: {@code observedValues.addAll(MapperC.<Date>of(observationDate)…)}.
     */
    @Test
    @EnabledIf("cdmCellAvailable")
    void indexValueObservationMultiple_multiParamWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdmFunctionOutput, CDM_GOLDEN_DIR,
                "cdm/observable/asset/fro/functions/IndexValueObservationMultiple.java");
    }

    /**
     * Multi-OUTPUT function-call result at an assignment with a chained tail:
     * {@code amount = MapperC.<BigDecimal>of(getRateScheduleStepValues.evaluate(schedule, periodStartDate))…}
     * — the callee declares {@code output: stepValues number (0..*)}.
     */
    @Test
    @EnabledIf("cdmCellAvailable")
    void getRateScheduleAmount_multiOutputCallAtAssignment_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdmFunctionOutput, CDM_GOLDEN_DIR,
                "cdm/product/asset/floatingrate/functions/GetRateScheduleAmount.java");
    }

    /**
     * Multi input param ({@code otherPayment OtherPayment (0..*)}) wrapped as a
     * NAVIGATED receiver in comparison/existence operands:
     * {@code exists(MapperC.<OtherPayment>of(otherPayment).<BigDecimal>map("getAmount", …))}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void otherPaymentDateValidation_multiParamNavigatedReceiver_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/trade/payment/functions/OtherPaymentDate_Validation.java");
    }

    /**
     * Multi input param ({@code values number (0..*)}) in a {@code notExists} operand:
     * {@code notExists(MapperC.<BigDecimal>of(values))}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void computeNotionalScheduleType_multiParamExistenceOperand_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/enrichment/upi/functions/Compute_NotionalScheduleType.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> functionOutput,
            Path goldenDir, String path) throws IOException {
        assertNotNull(functionOutput, "Function generation did not run — corpus unavailable?");
        String generated = functionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " — the cardinality-blind MapperS.of shape reappears if the "
                + "ReferenceHandler multi-cardinality wrap gate is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
