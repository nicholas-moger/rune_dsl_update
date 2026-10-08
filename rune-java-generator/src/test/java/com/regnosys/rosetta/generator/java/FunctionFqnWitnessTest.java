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
 * PR #197 (facet {@code fqnWitness} — a navigation {@code <Type>} generic WITNESS is rendered
 * fully-qualified inline, with its import suppressed, when its Java simple name collides with the
 * enclosing function's OUTPUT type but a DIFFERENT canonical name).
 *
 * <p>The THIRD render position of the same first-claim-wins import-collision law shipped at PR #195
 * (input-param position) and PR #196 (to-enum target position) — upstream
 * {@code ImportingStringConcatenation.internalDoImportIfPossible}: when a generated file references
 * two distinct types sharing a Java simple name, the FIRST claimant imports + renders simple, and a
 * later collider with a different canonical name renders FQN-inline with NO import. Here the OUTPUT
 * type (registered first, e.g. {@code cdm.base.staticdata.party.Account}) keeps the bare {@code Account}
 * + import; the {@code .<Account>mapC(...)} navigation witness ({@code fpml.consolidated.shared.Account},
 * a different canonical name) collides and renders {@code .<fpml.consolidated.shared.Account>mapC(...)}
 * with no {@code import fpml.consolidated.shared.Account;}.
 *
 * <p>The fork rendered the witness by its SIMPLE name AND imported it
 * ({@code NavigationHandler.addWitnessTypeRef}), so a file already importing the same-simple output
 * type emitted a DUPLICATE same-simple-name import = a Java compile error — every carrier was already
 * a waivered, non-compiling mismatch (green-safe by construction). The fix
 * ({@code NavigationHandler.witnessOutputCollisionFqn}, the NavigationHandler analogue of PR #196's
 * {@code ConversionHandler.enumTargetCollisionFqn}) compares the witness's Java simple name to the
 * enclosing function's output-type simple name via {@code HandlerHelper.findEnclosingFunction}; on a
 * same-simple/different-FQN collision it renders the witness FQN-inline ({@code resolveTypeParam}) and
 * suppresses its import ({@code addWitnessTypeRef} skips it). The MANDATORY FQN-difference gate (the
 * sibling of PR #195/#196) keeps a same-canonical witness (the witness IS the output type) bare.
 *
 * <p>Whole-file byte anchors through the REAL D11 loader (which loads the transitive rune-fpml dep per
 * PR #184). All carriers are cdm6 ingest-fpml (the witness is an fpml type colliding with a cdm output).
 * NO green FUNCTION carrier exists with this collision (a duplicate same-simple import never compiled),
 * so the over-FIRE guard (the gate must not FQN a non-colliding witness) is the full all-kinds D11 20/20
 * (zero collateral) + the byte-oracle's exact count, not a separate anchor.
 *
 * <p>Anchored:
 * <ul>
 *   <li>{@code MapAccountList} (cdm6 party) — a {@code .<Account>mapC(...)} witness whose simple name
 *       equals the multi-output element type {@code Account}; proves the {@code mapC} + multi-output case.</li>
 *   <li>{@code MapCapRateScheduleWithAddress} (cdm6 pricequantity) — a {@code .<StrikeSchedule>mapC(...)}
 *       witness inside an {@code exists(...)} predicate; single-output {@code StrikeSchedule}.</li>
 *   <li>{@code MapInitialOrFinalStub} (cdm6 swap) — a {@code .<StubFloatingRate>mapC(...)} witness,
 *       multi-output via {@code addAll(toBuilder(...))}.</li>
 *   <li>{@code MapPaymentCalculationPeriodList} (cdm6 swap) — a {@code .<PaymentCalculationPeriod>mapC(...)}
 *       witness; a different package, confirms generality.</li>
 * </ul>
 */
class FunctionFqnWitnessTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR = CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
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

    // ---- navigation <Type> witness FQN-inline on an output-name collision ----

    /** {@code .<Account>mapC} witness == multi-output element type {@code Account}. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapAccountList_cdm6_witnessCollidesWithOutput_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/party/functions/MapAccountList.java");
    }

    /** {@code .<StrikeSchedule>mapC} witness inside {@code exists(...)}; single-output. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapCapRateScheduleWithAddress_cdm6_witnessInExists_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/pricequantity/functions/MapCapRateScheduleWithAddress.java");
    }

    /** {@code .<StubFloatingRate>mapC} witness; multi-output via {@code addAll(toBuilder(...))}. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapInitialOrFinalStub_cdm6_witnessCollidesWithOutput_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/swap/functions/MapInitialOrFinalStub.java");
    }

    /** {@code .<PaymentCalculationPeriod>mapC} witness; a different package confirms generality. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapPaymentCalculationPeriodList_cdm6_witnessCollidesWithOutput_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/swap/functions/MapPaymentCalculationPeriodList.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir, String path)
            throws IOException {
        assertNotNull(output, "Function generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — the duplicate same-simple-name import (fpml witness + cdm output type) reappears if "
                + "the fqnWitness collision fix (NavigationHandler.witnessOutputCollisionFqn — render the "
                + "navigation <Type> witness FQN-inline + suppress its import) is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
