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
 * Facet {@code biginteger_literal_hoist} — a beyond-long integer literal
 * compiled in BigDecimal context hoists a statement local at the nearest
 * statement-hoist sink and consumes it through the null-guarded
 * Mapper ternary, the upstream {@code TypeCoercionService.convertNullSafe}
 * law (every non-primitive item conversion consumed at a wrapper hoists —
 * {@code declareAsVariable(true, actual.simpleName.toFirstLower, scope)}):
 *
 * <pre>
 * final BigInteger bigInteger = new BigInteger("9999999999999999999999999");
 * … (bigInteger == null ? MapperS.&lt;BigDecimal&gt;ofNull() : MapperS.of(new BigDecimal(bigInteger))) …
 * </pre>
 *
 * replacing the fork's inline {@code MapperS.of(new BigDecimal(new
 * BigInteger("…")))} — a shape ZERO goldens carry. Numbering rides the
 * {@code StatementHoistSession} name-group law ({@code bigInteger} bare for a
 * singleton, {@code bigInteger0..n-1} for n &ge; 2, numbered independently of
 * the {@code ifThenElseResult} group — the jfsa GetNtnlQty golden interleave).
 * Placement: a top-level if-condition's hoist rides the caller-collected
 * condition channel before the whole statement; a NESTED-then inner
 * if-condition's hoist drains at the inner statement's own indent inside the
 * outer branch; a then-arm assignment-RHS hoist rides the existing per-arm
 * drain.
 *
 * <p>Green-safety rests on two verified facts: (a) the inline BigDecimal-context
 * form {@code new BigDecimal(new BigInteger(} appears in ZERO goldens
 * corpus-wide — every golden site hoists; and (b) the hoist session/sink never
 * opens on the rule/report path ({@code FunctionGenerator.compileOperations}
 * {@code hoistSessionEligible} gate), so the 27 GREEN rule-kind report goldens
 * that DO carry {@code new BigInteger(} hoists — byte-matched via the existing
 * #129/#170 evaluate-arg channel — are unreachable by this arm (it declines to
 * the pre-facet bytes there). The full 5-cell D11 matrix (20/20) is the
 * empirical arbiter.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class FunctionBigIntegerLiteralHoistTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> drrFunctionOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
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
     * Top-level if-CONDITION site, 20-digit literal: the hoist rides the
     * condition channel before the whole statement at statement indent.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void formatToLongFraction20DecimalNumberDrr_topLevelConditionHoist_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/functions/FormatToLongFraction20DecimalNumber.java");
    }

    /** Top-level if-CONDITION site, 25-digit literal. */
    @Test
    @EnabledIf("drrCellAvailable")
    void formatToShortFraction5DecimalNumberDrr_topLevelConditionHoist_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/functions/FormatToShortFraction5DecimalNumber.java");
    }

    /**
     * NESTED-then inner if-CONDITION site: the hoist lands INSIDE the outer
     * branch at the inner statement's own indent (NOT bubbled before the
     * whole outer statement).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void notionalQuantityLeg2ValidationDrr_nestedThenConditionHoist_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/trade/quantity/functions/NotionalQuantityLeg2_Validation.java");
    }

    /** NESTED-then inner if-CONDITION site (or-chained comparison operand). */
    @Test
    @EnabledIf("drrCellAvailable")
    void totalNotionalQuantityLeg1ValidationDrr_nestedThenConditionHoist_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/trade/quantity/functions/TotalNotionalQuantityLeg1_Validation.java");
    }

    /**
     * Then-arm assignment-RHS site: the hoist rides the existing per-arm
     * drain inside the if-branch, immediately before the assignment.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void notionalAmountLeg2_01ValidationDrr_thenArmAssignmentHoist_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/trade/quantity/functions/NotionalAmountLeg2_01_Validation.java");
    }

    /**
     * Then-arm assignment-RHS site AFTER an A2 ifThenElseResult block in the
     * same arm: registration order places the bigInteger hoist between the
     * ifThenElseResult block and the consuming assignment (the cross-family
     * ordering the jfsa GetNtnlQty golden proves at n &ge; 2).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void notionalAmountLeg2_02ValidationDrr_thenArmAssignmentAfterIteBlock_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/trade/quantity/functions/NotionalAmountLeg2_02_Validation.java");
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(drrFunctionOutput, "Function generation did not run — corpus unavailable?");
        String generated = drrFunctionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " — the beyond-long BigInteger literal statement hoist + "
                + "null-guarded Mapper ternary is missing or regressed, if the "
                + "biginteger_literal_hoist recovery is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
