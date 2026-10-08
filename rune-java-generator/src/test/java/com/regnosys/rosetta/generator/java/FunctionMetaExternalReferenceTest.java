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
 * Facet {@code metaExternalReference} (PR #214, cluster 2) — a pathed
 * {@code set out -> intermediate -> reference: <href>} whose leaf {@code reference}
 * does NOT resolve to a real attribute is the {@code [metadata reference]}
 * pseudo-feature (the EXTERNAL reference of a {@code ReferenceWithMeta} wrapper).
 * Upstream PojoPropertyUtil maps {@code reference → externalReference}, so the leaf
 * setter is a DIRECT {@code .setExternalReference(<href>.get())} on the wrapper. The
 * fork emitted {@code .setReference(<href String>)} — there is no String-typed
 * {@code reference} setter, so the chain does not compile and every {@code -> reference}
 * carrier was already a waivered mismatch; GREEN-SAFE by construction (a model
 * attribute genuinely named {@code reference} resolves and keeps the plain path; the
 * arm fires only on the unresolved pseudo-meta leaf, in
 * {@link com.regnosys.rosetta.generator.java.function.FunctionExpressionRenderer#leafSetterCall}).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen cdm/6.20.6 goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}, chosen from the cluster's
 * waivered-flip files (byte-oracle over f-probe-214). {@code MapSwapResetDates}
 * additionally carries the {@code metaLocationConstruction} (cluster 1)
 * {@code withMetaArgument} hoist, so it flips ONLY when both meta facets compose —
 * a deliberate composition anchor. REVERT-VERIFIED RED: reverting the
 * {@code reference → setExternalReference} arm reverts these anchors to the
 * {@code .setReference(...)} shape.
 */
class FunctionMetaExternalReferenceTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
        if (cdm6CellAvailable()) {
            cdm6FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
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

    /** Minimal single {@code -> reference} leaf: setExternalReference of an href. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapStubCalculationPeriodAmountToStubPeriod_externalReference_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/swap/functions/MapStubCalculationPeriodAmountToStubPeriod.java");
    }

    /** Two {@code -> reference} leaves (payer + receiver party references). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapPaymentToPartyReferencePayerReceiver_externalReference_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/payment/functions/MapPaymentToPartyReferencePayerReceiver.java");
    }

    /** Three {@code -> reference} leaves (protection/cash/physical settlement terms references). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapReferencePoolItem_externalReference_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/creditdefaultswap/functions/MapReferencePoolItem.java");
    }

    /** Composition anchor: BOTH metaLocationConstruction (withMetaArgument) AND metaExternalReference. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapSwapResetDates_externalReferencePlusWithMetaArgument_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/swap/functions/MapSwapResetDates.java");
    }

    private static void assertByteMatchesGolden(
            Map<String, String> functionOutput, Path goldenDir, String path) throws IOException {
        assertNotNull(functionOutput, "Function generation did not run — corpus unavailable?");
        String generated = functionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " — a pathed set whose leaf `reference` is the [metadata reference] "
                + "pseudo-feature maps to a DIRECT .setExternalReference(<href>.get()) on the "
                + "ReferenceWithMeta wrapper (upstream PojoPropertyUtil reference→externalReference); "
                + "reverting the metaExternalReference facet emits the non-compiling "
                + ".setReference(<String>) and reverts this anchor to RED.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
