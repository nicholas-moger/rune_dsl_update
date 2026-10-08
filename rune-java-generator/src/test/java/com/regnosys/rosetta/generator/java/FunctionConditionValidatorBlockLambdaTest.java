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
 * Facet {@code conditionValidatorBlockLambda} (PR #225) — the control-flow extension
 * of PR #222's flat-conditional validate block lambda. A
 * {@code conditionValidator.validate(() -> …)} condition with control flow renders as
 * a BLOCK lambda matching the upstream 9.83.0 golden; the fork's inline ternary
 * ({@code <cond>.getOrDefault(false) ? <ComparisonResult> : <MapperC/Mapper>}) does not
 * compile (the ternary's common type is not a {@code ComparisonResult}), so every
 * carrier is already a waivered mismatch — and the goldens carry ZERO inline ternaries
 * in a {@code validate(() -> …)} lambda (every control-flow validate condition is a
 * block). Two renderer arms + one template fix:
 *
 * <ol>
 *   <li><b>if/return-ladder</b> ({@code FunctionExpressionRenderer.renderConditionLadderOrNull})
 *       — the #222 flat form generalised to a nested else-CHAIN
 *       ({@code c1?t1:c2?t2:empty} → {@code if (c1) { return t1; } if (c2) { return t2; }
 *       return ComparisonResult.ofEmpty();}).</li>
 *   <li><b>operand hoist</b> ({@code FunctionExpressionRenderer.renderConditionHoistBlockOrNull})
 *       — a non-conditional whole body whose nested {@code ComparisonResult} conditional
 *       operand (e.g. an {@code andNullSafe} argument {@code <cond> ? <cmp> : <mapper>})
 *       hoists {@code final ComparisonResult ifThenElseResult; if/else {…}} via the
 *       {@code ControlFlowHandler} arm-A2 lift (the lambda scope marked a statement-hoist
 *       sink), the chain returned with the local spliced in.</li>
 *   <li><b>condition separator</b> ({@code java-function.stg}) — the pre/post-condition
 *       iteration now emits the per-condition trailing blank line via an iteration
 *       {@code separator} (the goldens carry a 2-tab blank after every condition; a
 *       single-condition function is byte-unchanged, so a green simple validate stays
 *       identical).</li>
 * </ol>
 *
 * <p><b>Green-safe by construction:</b> the inline ternary is zero-golden, and the
 * separator leaves single-condition output untouched (the regression scan + the full
 * 5-cell D11 matrix 20/20 are the empirical arbiters). The facet pays 2 FUNCTION flips:
 * CreditSupportAmount (cdm6) needs BOTH the ladder (validator 2) AND the operand-hoist
 * (validator 1) AND the separator blank between its two validators;
 * Create_OnDemandRateChangePrimitiveInstruction (cdm5) is a pure separator flip (a
 * multi-condition function whose only divergence was the missing inter-condition blank).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}, mirroring the 2 flips 1:1.
 */
class FunctionConditionValidatorBlockLambdaTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;
    private static Map<String, String> cdm5FunctionOutput;

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
        if (cdm6CellAvailable()) {
            cdm6FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
        if (cdm5CellAvailable()) {
            cdm5FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
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
     * CreditSupportAmount (cdm6) — two pre-condition validators: validator 1 hoists an
     * {@code andNullSafe} operand ternary into {@code final ComparisonResult
     * ifThenElseResult; if/else {…}}; validator 2 renders an
     * {@code if (c1) { return …; } if (c2) { return …; } return ComparisonResult.ofEmpty();}
     * ladder; the two are separated by the per-condition blank line.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void creditSupportAmount_validatorOperandHoistAndLadder_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/legaldocumentation/csa/functions/CreditSupportAmount.java");
    }

    /**
     * Create_OnDemandRateChangePrimitiveInstruction (cdm5) — a multi-condition function
     * whose sole divergence was the missing inter-condition blank line; a pure
     * condition-separator flip.
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void createOnDemandRateChangePrimitiveInstruction_conditionSeparator_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/common/functions/Create_OnDemandRateChangePrimitiveInstruction.java");
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
                + path + " — a control-flow validate condition falls to the non-compiling "
                + "inline ternary (or loses the inter-condition blank line) if the "
                + "renderConditionLadderOrNull / renderConditionHoistBlockOrNull arm or the "
                + "java-function.stg condition separator is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
