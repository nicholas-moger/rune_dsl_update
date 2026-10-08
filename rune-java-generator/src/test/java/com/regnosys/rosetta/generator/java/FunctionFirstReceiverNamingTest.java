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
 * Facet {@code first_receiver_lambda_naming} — a navigation step whose receiver
 * is a {@code .first()}/{@code .last()} collapse derives its lambda var from the
 * receiver's ELEMENT data type (golden
 * {@code .first().<FieldWithMetaString>map("getIdentifier", assignedIdentifier -> …)}),
 * not the navigated-feature underscore fallback ({@code _identifier}).
 * {@code NavigationHandler.resolveLambdaVarName} gains the FIRST/LAST receiver
 * arm reading the SAME first/last-transparent gm-aware receiver walk the witness
 * path reads (facet then_maxmin_item_typing arm S3), scope-disambiguated like
 * every type-derived lambda var; compiler-less callers keep the pre-facet
 * fallback.
 *
 * <p>Green-safety: no golden renders the underscore-fallback var off a
 * first/last receiver where the element type resolves (the pre-facet form was
 * the divergence), and the walk declines (pre-facet bytes) when the element
 * type does not resolve; the full 5-cell D11 matrix (20/20) is the empirical
 * arbiter.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. The facet's second
 * carrier ({@code Extract_UnderlyingAssetTradingPlatformIdentifier}) needs the
 * sibling facet condition_continuation_indent too — anchored at
 * {@link FunctionConditionContinuationIndentTest}.
 */
class FunctionFirstReceiverNamingTest {

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
     * The naming-only SOLE carrier: the step off
     * {@code … -> assignedIdentifier first} names its lambda var from the
     * element type ({@code assignedIdentifier}), recovering the golden bytes
     * with NO other divergence in the file (drr).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void extractHkmaTradeIdentifierDrr_firstReceiverElementTypeVar_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/hkma/rewrite/valuation/functions/Extract_HKMATradeIdentifier.java");
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
                + path + " — the .first()/.last()-receiver element-type lambda var is "
                + "missing or regressed, if the first_receiver_lambda_naming recovery "
                + "is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
