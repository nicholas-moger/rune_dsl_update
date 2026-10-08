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
 * Facet {@code returnIte} — an alias body whose expression is a Rosetta
 * if-then-else compiles as the flat {@code if/return} ladder upstream emits at a
 * RETURN seat ({@code JavaIfThenElseBuilder.completeAsReturn}, in-tree 9.83.0
 * JavaIfThenElseBuilder.java:97-100):
 *
 * <pre>
 * if (&lt;cond&gt;.getOrDefault(false)) {
 *     return &lt;then&gt;;
 * }
 * return &lt;else&gt;;
 * </pre>
 *
 * replacing the fork's inline {@code return <cond>.getOrDefault(false) ? <then>
 * : <else>;} ternary ({@code ControlFlowHandler#handle}). A chained else-ITE
 * flattens to sequential if/return rungs ending in a bare {@code return
 * <else>;} (never {@code else}/{@code else if}); branch VALUES compile
 * identically to the ternary path, so ONLY the statementization differs —
 * except the absent Rosetta else, which the ternary renders as the untyped
 * {@code MapperC.of()} (the empty-list literal) and upstream types at the seat:
 * a {@code MapperS<Item>} (or {@code MapperS<? extends Item>}) signature yields
 * {@code MapperS.<Item>ofNull()}, dropping the gen-only {@code MapperC} import
 * when it was its only use.
 *
 * <p>This facet fires at the ALIAS-METHOD seat only ({@code FunctionGenerator}
 * compileAliases → {@code FunctionExpressionRenderer#renderAliasReturnLadderOrNull}).
 * Item-Boolean condition hoists (the #179 {@code final Boolean booleanN} law),
 * the pre/post-condition validate-lambda seat, the in-lambda map body and
 * {@code MapperC}/{@code ComparisonResult} empty-else forms are DEFERRED — their
 * carriers keep the inline form and stay waivered.
 *
 * <p>Green-safe by construction: ZERO of the 34,686 frozen goldens carry the
 * {@code getOrDefault(false) ? } ternary in ANY seat (corpus-wide plain-grep),
 * so every alias body this fires on lives in an already-waivered FUNCTION
 * mismatch; the full 5-cell D11 matrix (20/20) is the empirical arbiter. The
 * facet pays 9 FUNCTION flips (cdm5 ×4 + cdm6 ×4 + drr ×1).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class FunctionReturnIteTest {

    private static final Path CDM_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM_GOLDEN_DIR =
            CDM_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdmFunctionOutput;
    private static Map<String, String> cdm5FunctionOutput;
    private static Map<String, String> drrFunctionOutput;

    static boolean cdmCellAvailable() {
        return Files.isDirectory(CDM_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM_GOLDEN_DIR);
    }

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
    }

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
        if (cdmCellAvailable()) {
            cdmFunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM_CELL_ROOT));
        }
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

    /**
     * The cleanest single-rung carrier: one wrapper condition
     * ({@code exists(...)}) with an EXPLICIT else ({@code else 0.0} →
     * {@code MapperS.of(new BigDecimal("0.0"))}, rendered verbatim) — the only
     * divergence from gen is the {@code return ... ? ... : ...} ternary becoming
     * the {@code if (...) { return ...; } return ...;} ladder.
     */
    @Test
    @EnabledIf("cdmCellAvailable")
    void vectorScalarOperationCdm_singleRungExplicitElse_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdmFunctionOutput, CDM_GOLDEN_DIR,
                "cdm/base/math/functions/VectorScalarOperation.java");
    }

    /**
     * Absent-else typed empty: the elseless {@code if/then} alias renders its
     * ladder terminal as the seat-typed {@code MapperS.<FloatingRateSettingDetails>ofNull()}
     * read from the {@code MapperS<? extends FloatingRateSettingDetails>}
     * signature (the {@code ? extends } wildcard stripped to its bound),
     * replacing the gen ternary's untyped {@code MapperC.of()} — and the gen-only
     * {@code MapperC} import (its sole use) drops with it.
     */
    @Test
    @EnabledIf("cdmCellAvailable")
    void floatingAmountCalculationCdm_absentElseTypedEmptyImportDrop_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdmFunctionOutput, CDM_GOLDEN_DIR,
                "cdm/product/asset/calculation/functions/FloatingAmountCalculation.java");
    }

    /**
     * SIX returnIte alias-body ladders in one file, each an explicit-else
     * conditional — every one statementizes (mix of {@code exists(...)} and
     * {@code areEqual(...)} wrapper conditions, then-values kept verbatim
     * including the {@code MapperS.of(max.evaluate(...))} chains). The golden
     * carries 8 {@code if (...) { return ...; }} rungs total: these six plus two
     * pre-existing from other facets (the whole-file byte-match still locks all
     * eight, but only the six are this facet's work).
     */
    @Test
    @EnabledIf("cdmCellAvailable")
    void applyFloatingRateProcessingCdm_sixAliasLadders_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdmFunctionOutput, CDM_GOLDEN_DIR,
                "cdm/product/asset/floatingrate/functions/ApplyFloatingRateProcessing.java");
    }

    /**
     * TWO alias methods with {@code andNullSafe}-composed wrapper conditions and
     * explicit else (a sibling alias call / a bare local), confirming the
     * compound-condition rendering survives the statementization byte-for-byte.
     */
    @Test
    @EnabledIf("cdmCellAvailable")
    void applyCapsAndFloorsCdm_compoundConditionTwoAliases_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdmFunctionOutput, CDM_GOLDEN_DIR,
                "cdm/product/asset/floatingrate/functions/ApplyCapsAndFloors.java");
    }

    /**
     * The cdm/5.38.0 cell is anchored explicitly (not only transitively via the
     * D11 matrix): the four cdm5 flips are the cdm5-model versions of the cdm6
     * carriers, so two representative cdm5 anchors — the cleanest single-rung
     * explicit-else ({@code VectorScalarOperation}) and the absent-else typed-empty
     * + MapperC import drop ({@code FloatingAmountCalculation}) — pin the cell so a
     * cdm5-only regression cannot stay green here (the other two cdm5 carriers,
     * {@code ApplyCapsAndFloors}/{@code ApplyFloatingRateProcessing}, are covered
     * by their cdm6 twins plus the full D11 matrix).
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void vectorScalarOperationCdm5_singleRungExplicitElse_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/base/math/functions/VectorScalarOperation.java");
    }

    /**
     * cdm/5.38.0 absent-else typed empty + MapperC import drop — the cdm5 twin of
     * the cdm6 {@code FloatingAmountCalculation} anchor, pinning the cdm5 cell.
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void floatingAmountCalculationCdm5_absentElseTypedEmptyImportDrop_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/product/asset/calculation/functions/FloatingAmountCalculation.java");
    }

    /**
     * drr carrier: two aliases ({@code leg1}/{@code leg2}) with a
     * {@code ComparisonResult.ofNullSafe(...).orNullSafe(...)} condition and an
     * absent else typed {@code MapperS.<AnnaDsbNotionalScheduleEnum>ofNull()}
     * from a plain (non-wildcard) {@code MapperS<AnnaDsbNotionalScheduleEnum>}
     * signature — plus the {@code MapperC} import drop.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void isNotionalScheduleCustomDrr_comparisonResultTypedEmpty_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/IsNotionalScheduleCustom.java");
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
                + path + " — the alias-body if-then-else's flat return ladder (its "
                + "if/return statementization, typed-empty else, or MapperC import "
                + "drop) is missing or regressed, if the returnIte recovery is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
