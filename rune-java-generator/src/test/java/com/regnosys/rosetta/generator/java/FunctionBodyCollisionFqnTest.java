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
 * PR #227 (facet {@code fqnWitness} resolver — the render-order-aware, first-claim-wins
 * import-collision RESOLVER: the general form of the upstream {@code ImportingStringConcatenation}
 * streaming-import law that PRs #194–#197 patched at individual <em>guaranteed-first</em> anchor
 * seats). Where PR #197's {@code FunctionFqnWitnessTest} locks the witness-vs-OUTPUT collision
 * (anchor = the always-first signature output), this locks the BODY-INTERNAL collisions whose winner
 * depends on render order.
 *
 * <p>The law: when a generated class references two distinct types sharing a Java simple name, the
 * FIRST reference (in render order) keeps the bare name + import; a later reference to a different
 * canonical with the same simple name renders FULLY-QUALIFIED inline with NO import. The #227
 * collisions are body-internal — an fpml input navigation witness ({@code .<Offset>map(...)}) vs a
 * cdm construction type ({@code Offset.builder()} / a hoisted {@code final Offset …}) — so BOTH
 * directions occur: witness-loses (the cdm construction claims first, the witness FQN-ed) and
 * ctor-loses (the witness, hoisted into a condition / local, claims first, the ctor FQN-ed).
 *
 * <p>The fork baked type simple names into body strings and imported every distinct canonical, so a
 * collision emitted a DUPLICATE same-simple-name import = a Java compile error — every carrier was
 * already a waivered, non-compiling mismatch (green-safe by construction). The fix
 * ({@code ImportCollisionResolver}, exercised in isolation by {@code ImportCollisionResolverTest}):
 * the colliding seats emit sentinels carrying the canonical name; {@code FunctionGenerator} resolves
 * them PER BODY (the within-body sentinel order is the true render order) and suppresses the loser's
 * import. A non-colliding class resolves byte-identically to the pre-facet form, so the over-FIRE
 * guard is the full all-kinds D11 20/20 (zero collateral), not a separate anchor.
 *
 * <p>Whole-file byte anchors through the REAL D11 loader (the transitive rune-fpml dep per PR #184),
 * covering both directions:
 * <ul>
 *   <li>{@code MapSwapPaymentDates} — witness-loses via builder-ctor (the cdm {@code Offset.builder()}
 *       claims first; the fpml {@code .<Offset>map} witness FQN-ed).</li>
 *   <li>{@code MapEquityAmericanExerciseTerms} — witness-loses via a hoisted singletonList local
 *       ({@code final AdjustableOrRelativeDate …} claims first; the fpml witness FQN-ed).</li>
 *   <li>{@code MapExerciseProcedure} — ctor-loses (the fpml {@code .<ManualExercise>map} witness in a
 *       hoisted if-condition claims first; the cdm {@code ManualExercise.builder()} FQN-ed).</li>
 *   <li>{@code MapAdjustableOrRelativeDateToObservationTerms} — ctor-loses (a second direction).</li>
 * </ul>
 */
class FunctionBodyCollisionFqnTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR = CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;

    static boolean cellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellAvailable()) {
            cdm6FunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell) throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(), D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = gen.generateWithErrors(output);
        assertTrue(genErrors.isEmpty(), cell + " FUNCTION generation reported errors: " + genErrors);
        return output;
    }

    /** Witness-loses via builder-ctor: cdm {@code Offset.builder()} wins, fpml {@code <Offset>} witness FQN-ed. */
    @Test
    @EnabledIf("cellAvailable")
    void mapSwapPaymentDates_witnessLosesToBuilderCtor_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "cdm/ingest/fpml/confirmation/product/swap/functions/MapSwapPaymentDates.java");
    }

    /** Witness-loses via a hoisted singletonList local ({@code final AdjustableOrRelativeDate …}). */
    @Test
    @EnabledIf("cellAvailable")
    void mapEquityAmericanExerciseTerms_witnessLosesToHoistedLocal_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "cdm/ingest/fpml/confirmation/common/functions/MapEquityAmericanExerciseTerms.java");
    }

    /** Ctor-loses: fpml {@code <ManualExercise>} witness (in a hoisted if-condition) wins, cdm ctor FQN-ed. */
    @Test
    @EnabledIf("cellAvailable")
    void mapExerciseProcedure_ctorLosesToHoistedWitness_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "cdm/ingest/fpml/confirmation/common/functions/MapExerciseProcedure.java");
    }

    /** Ctor-loses (a second direction carrier). */
    @Test
    @EnabledIf("cellAvailable")
    void mapAdjustableOrRelativeDateToObservationTerms_ctorLoses_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "cdm/ingest/fpml/confirmation/common/functions/MapAdjustableOrRelativeDateToObservationTerms.java");
    }

    private void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(cdm6FunctionOutput, "Function generation did not run — corpus unavailable?");
        String generated = cdm6FunctionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = CDM6_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — the duplicate same-simple-name import (fpml witness + cdm construction type) "
                + "reappears if the first-claim-wins import resolver (ImportCollisionResolver, wired "
                + "through the colliding seats + FunctionGenerator.buildStandardModel) is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
