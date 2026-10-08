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
 * Facet {@code tostring_operand_meta_unwrap} — a {@code to-string} OPERAND
 * compiles against the meta-STRIPPED wrapper, mirroring upstream
 * {@code caseToStringOperation} ({@code ExpressionGenerator} L1164-74:
 * the argument's expected is {@code MAPPER_S.wrapExtendsWithoutMeta(expr.argument)}),
 * so a {@code FieldWithMeta}-typed operand gains the Type-coercion unwrap
 * immediately BEFORE the to-string map:
 *
 * <pre>
 * .&lt;FloatingRateIndexEnum&gt;map("Type coercion", fieldWithMetaFloatingRateIndexEnum -&gt;
 *         fieldWithMetaFloatingRateIndexEnum == null ? null : fieldWithMetaFloatingRateIndexEnum.getValue())
 * .map("to-string", FloatingRateIndexEnum::toDisplayString)
 * </pre>
 *
 * The unwrap rides the shared {@code coerceNavigationReceiver} lever, so the
 * guard kind follows the #170 law (guarded iff the chain rides {@code MapperS}
 * — every corpus to-string carrier is a {@code MapperS} item chain, hence the
 * guarded ternary); the lever no-ops on every non-meta / untyped operand, the
 * byte-flat guarantee for the green to-string population (facet
 * tostring_enum_source, #165).
 *
 * <p>Both anchors close JOINTLY with facet {@code interior_position_coercion}
 * (same PR): their if-condition hunks need the interior-position law and the
 * alias-headed {@code MapperC} kind, their min/max-key hunks need THIS operand
 * unwrap — the whole-file byte match pins both.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class FunctionToStringOperandMetaUnwrapTest {

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
     * {@code min}-key to-string operand: the guarded unwrap lands between the
     * meta nav step and {@code .map("to-string", FloatingRateIndexEnum::toDisplayString)}
     * inside the {@code .min(item -> ...)} key chain; the same file's
     * if-condition coercions pin the joint interior_position_coercion closure.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void interestRateLeg1BasisDrr_minKeyToStringUnwrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/csa/rewrite/trade/functions/InterestRateLeg1Basis.java");
    }

    /** {@code max}-key mirror of the anchor above. */
    @Test
    @EnabledIf("drrCellAvailable")
    void interestRateLeg2BasisDrr_maxKeyToStringUnwrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/csa/rewrite/trade/functions/InterestRateLeg2Basis.java");
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
                + path + " — the to-string operand's guarded Type-coercion meta "
                + "unwrap (or the joint interior_position_coercion closure) is "
                + "missing or regressed, if the tostring_operand_meta_unwrap "
                + "recovery is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
