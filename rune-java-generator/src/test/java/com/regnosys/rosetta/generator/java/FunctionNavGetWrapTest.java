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
 * Facet {@code navGetWrap} (PR #243) — an operand that rendered as a transparent
 * <em>item-collapse</em> (a {@code selfUnwrapping} {@link com.regnosys.rosetta.generator.java.statement.builder.JavaExpression}
 * whose text ends in {@code .get()} — a {@code MapperC}/{@code Mapper} navigation collapsed to
 * its single item) is re-wrapped {@code MapperS.of(<chain>.get())} at every Mapper-consuming seat
 * so the seat sees a {@code Mapper}: comparison operands ({@code ComparisonHandler}), exists /
 * notExists arguments ({@code ExistenceHandler}), and Mapper-typed alias-method return-ladder rungs
 * ({@code FunctionExpressionRenderer}).
 *
 * <p>These operands carry a {@code null} expression type, so the type-driven
 * {@code WrapperToWrapperCoercer} (which already emits {@code MapperS.of(<e>.get())} for a typed
 * {@code MapperC}/{@code Mapper}→{@code MapperS} coercion) never fires and the fork passes the bare
 * {@code .get()} value — which does not compile against the Mapper-operand signature, so the carrier
 * is already waivered. Upstream compiles every such operand against
 * {@code MAPPER.wrapExtendsWithoutMeta(joined)} and the coercion service re-wraps the collapsed item.
 *
 * <p>GREEN-SAFE BY CONSTRUCTION (corpus-verified, frozen 9.83.0 baseline, all 5 cells): ZERO golden
 * leaves a {@code .get()}-collapsed operand bare at any exists/comparison seat and ZERO function
 * golden leaves a method-level {@code return <chain>.get();} bare — every one is
 * {@code MapperS.of(...)}-wrapped — so the rewrite only ever touches currently-waivered output. The
 * {@code selfUnwrapping} + transparency discriminator is the load-bearing gate (the {@code selfUnwrapping}
 * marker is overloaded): an already-{@code MapperS.of(…)} / {@code MapperC.<T>of(…)} wrap renders
 * NON-transparently and never ends in {@code .get()}, so it is excluded — locked GREEN by
 * {@link #leiRegistrationStatusIsValid_wrappedExistsArgUnchanged_byteMatchesGolden}.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens (newline-normalized),
 * generated through the REAL {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class FunctionNavGetWrapTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm5FunctionOutput;
    private static Map<String, String> drrFunctionOutput;

    static boolean cdm5CellAvailable() {
        return cellAvailable(CDM5_CELL_ROOT, CDM5_GOLDEN_DIR);
    }

    static boolean drrCellAvailable() {
        return cellAvailable(DRR_CELL_ROOT, DRR_GOLDEN_DIR);
    }

    private static boolean cellAvailable(Path cellRoot, Path goldenDir) {
        return Files.isDirectory(cellRoot.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(goldenDir);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
        if (cdm5CellAvailable()) {
            cdm5FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
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

    // -------------------------------------------------------------------------
    // Comparison-operand seat (areEqual)
    // -------------------------------------------------------------------------

    /**
     * Carrier (cdm5, areEqual operand): {@code Create_SecurityTransfer} compares
     * {@code …getTransferSettlementType().get()} against an enum constant — golden wraps the
     * collapsed item {@code areEqual(MapperS.of(<chain>.get()), MapperS.of(TransferSettlementEnum.…), …)}.
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void createSecurityTransferCdm5_areEqualGetOperandWrapped_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/common/functions/Create_SecurityTransfer.java");
    }

    /**
     * Carrier (drr, areEqual operand): {@code IsActionTypeTERM} compares a
     * {@code …getPositionState().get()} collapse against {@code PositionStatusEnum.CLOSED}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void isActionTypeTermDrr_areEqualGetOperandWrapped_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/IsActionTypeTERM.java");
    }

    // -------------------------------------------------------------------------
    // Existence-argument seat (exists / notExists)
    // -------------------------------------------------------------------------

    /**
     * Carrier (drr, exists argument): {@code PayoutLeg1} checks
     * {@code exists(<chain>.<ForwardPayout>mapC(…).get())} — golden wraps it
     * {@code exists(MapperS.of(<chain>.get()))}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void payoutLeg1Drr_existsGetArgWrapped_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/PayoutLeg1.java");
    }

    /**
     * Carrier (drr, exists argument nested in areEqual): {@code SettlementTermsLeg2} checks
     * {@code areEqual(exists(MapperS.of(<chain>.get())), MapperS.of(false), …)} — the exists
     * argument is the {@code .get()} collapse.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void settlementTermsLeg2Drr_existsGetArgWrapped_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/SettlementTermsLeg2.java");
    }

    // -------------------------------------------------------------------------
    // Mapper-typed alias-method return-ladder rung seat
    // -------------------------------------------------------------------------

    /**
     * Carrier (drr, Mapper-typed alias return): {@code GetReportableStrikePricePeriod}'s
     * {@code customizedSchedule} alias returns {@code MapperS<? extends CalculationSchedule>} —
     * golden {@code return MapperS.of(<chain>.<CalculationSchedule>map(…).get());}. Locks the
     * return-ladder rung wrap (gated on the alias return type being {@code MapperS<…>}; in-lambda
     * item-typed bare returns are NOT this seat).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getReportableStrikePricePeriodDrr_mapperReturnGetRungWrapped_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/standards/iosco/cde/version1/price/functions/GetReportableStrikePricePeriod.java");
    }

    // -------------------------------------------------------------------------
    // Green-safety lock (the discriminator's DECLINE path)
    // -------------------------------------------------------------------------

    /**
     * GREEN-SAFETY lock: {@code LeiRegistrationStatusIsValid} (green) carries an
     * {@code exists(MapperS.of(<x>))} argument — an already-{@code MapperS.of(…)}-wrapped operand
     * that renders NON-transparently ({@code render(operand) != render(unwrap)}) and does not end in
     * {@code .get()}, so the navGetWrap discriminator DECLINES. Locks that the wrap does NOT
     * double-wrap a green {@code MapperS.of(…)} operand (would corrupt it to
     * {@code exists(MapperS.of(MapperS.of(<x>)))}).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void leiRegistrationStatusIsValid_wrappedExistsArgUnchanged_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/enrichment/lei/functions/LeiRegistrationStatusIsValid.java");
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
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — the navGetWrap fix (HandlerHelper.wrapSelfUnwrappingGetOperand at the "
                + "comparison/exists seats + FunctionExpressionRenderer.wrapSelfUnwrappingGetRungInMapperSOf "
                + "at the Mapper-typed alias-return rung) is missing or regressed if reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
