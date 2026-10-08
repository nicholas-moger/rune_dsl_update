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
 * Facet {@code condition_continuation_indent} — a multi-line if-condition chain
 * re-indents its continuation lines at statement depth.
 * {@code FunctionExpressionRenderer.renderConditionalAssignment} was the single
 * if-condition site splicing the RAW relative continuation ({@code \n\t.first()…}
 * landing at ONE absolute tab) — every sibling statement-position consumer,
 * including the then-route's {@code appendThenConditionalBlock} condition,
 * applies {@code reindentContinuation(…, indentLevel)} (continuations land at
 * statement-depth + 1, the Xtend IndentedTarget contract upstream gets for
 * free).
 *
 * <p>Green-safety by construction: NO golden in the 5-cell corpus carries a
 * 1-tab continuation line, so no green file renders a multi-line condition
 * through this site — every re-indented file was already divergent + waivered;
 * the full 5-cell D11 matrix (20/20, rule emission included — the renderer is
 * shared) is the empirical arbiter.
 *
 * <p>The anchor carrier needs the sibling facet first_receiver_lambda_naming
 * too (its hunk 1 carries BOTH the 1-tab condition continuation and the
 * underscore lambda var; hunk 2 is naming-only at an already-correct indent) —
 * the joint whole-file anchor pins both facets' bytes
 * ({@link FunctionFirstReceiverNamingTest} pins naming SOLO on its own carrier).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class FunctionConditionContinuationIndentTest {

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
     * The joint carrier: the if-condition's {@code .first().<…>map(…)…exists}
     * continuation lands at statement-depth + 1 (golden 4 tabs, was 1), and the
     * two steps off the {@code .first()} receiver name their lambda vars from
     * the element type ({@code transactionInformation}, facet
     * first_receiver_lambda_naming) — together recovering the golden bytes (drr).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void extractUnderlyingAssetTradingPlatformIdentifierDrr_conditionContinuationDepthAndFirstNaming_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/hkma/rewrite/trade/functions/Extract_UnderlyingAssetTradingPlatformIdentifier.java");
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
                + path + " — the if-condition continuation re-indent (facet "
                + "condition_continuation_indent) or the .first()-receiver lambda "
                + "naming (facet first_receiver_lambda_naming) is missing or regressed.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
