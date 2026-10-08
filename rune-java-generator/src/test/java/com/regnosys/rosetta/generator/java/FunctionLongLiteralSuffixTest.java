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
 * Facet {@code long_literal_suffix} — an integer literal beyond int range in a
 * BigDecimal context renders with the lowercase {@code l} long suffix
 * ({@code BigDecimal.valueOf(99999999999l)}, the upstream Xtend convention);
 * the fork's {@code LiteralHandler.renderIntValueForBigDecimal} previously
 * emitted the bare digits — a NON-COMPILING out-of-range int literal — for the
 * whole {@code (int, long]} band. In-int-range literals stay suffix-free (the
 * green corpus convention) and the beyond-long band keeps the
 * {@code new BigDecimal(new BigInteger("…"))} form only where no
 * statement-hoist sink is reachable (facet biginteger_literal_hoist hoists it
 * otherwise — see {@code FunctionBigIntegerLiteralHoistTest}).
 *
 * <p>Green-safe by construction: the bare beyond-int form does not compile, so
 * no green golden carries it; the two corpus carriers (both drr, both SOLE
 * single-hunk) are the only MEASURED FUNCTION-kind carriers the band reaches
 * (a handful of drr RULE-family goldens carry the suffixed form too — they sit
 * in the CODEGEN_BODY_GAP bucket whose body emission is pending, untouched by
 * this facet).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class FunctionLongLiteralSuffixTest {

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
            var cell = new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT);
            var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            var typeUtil = new JavaTypeUtil();
            var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
            Map<String, String> output = new LinkedHashMap<>();
            List<GenerationException> genErrors = gen.generateWithErrors(output);
            assertTrue(genErrors.isEmpty(),
                    cell + " FUNCTION generation reported errors: " + genErrors);
            drrFunctionOutput = output;
        }
    }

    /** The 11-digit carrier: {@code BigDecimal.valueOf(99999999999l)} (drr). */
    @Test
    @EnabledIf("drrCellAvailable")
    void formatToBaseOneRateDrr_beyondIntLiteralLongSuffix_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/standards/iosco/cde/base/price/functions/FormatToBaseOneRate.java");
    }

    /** The 18-digit sibling: {@code BigDecimal.valueOf(999999999999999999l)} (drr). */
    @Test
    @EnabledIf("drrCellAvailable")
    void formatToBaseOne18RateDrr_beyondIntLiteralLongSuffix_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/standards/iosco/cde/base/price/functions/FormatToBaseOne18Rate.java");
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
                + path + " — the beyond-int-range lowercase-l long suffix in "
                + "BigDecimal.valueOf is missing or regressed, if the "
                + "long_literal_suffix recovery is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
