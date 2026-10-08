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
 * PR #200 (facet {@code fqnWitnessMapperC} — the {@code MapperC.<Item>of(...)} render position of the
 * {@code ImportingStringConcatenation} first-claim-wins import-collision law, the 4th/final render
 * position after PR #194 (usRename) / #195 (fpmlInputFqn) / #196 (checkedMapFqn) / #197 (fqnWitness,
 * the navigation-witness position)).
 *
 * <p>THE LAW — when a {@code MapperC.<Item>of(...)} cardinality-coercion witness's Java simple name
 * collides with the enclosing function OUTPUT type (same simple name, DIFFERENT fully-qualified name),
 * the witness renders FQN-inline ({@code MapperC.<fpml.consolidated.shared.Party>of(...)}) and its
 * import is SUPPRESSED — the golden form. The fork's bare-simple-name witness instead adds a SECOND
 * {@code import fpml.consolidated.shared.Party;} that duplicates the simple name the CDM output import
 * {@code import cdm.base.staticdata.party.Party;} already claims — a duplicate-simple-name import that
 * does NOT compile, so every collision carrier is already a waivered mismatch (green-safe by
 * construction). Implemented in {@code ReferenceHandler.mapperCWitnessOutputCollisionFqn} (mirroring
 * {@code NavigationHandler.witnessOutputCollisionFqn} at the MapperC.of locus) + threaded into
 * {@code JavaExpression.wrappedInMapperCOfSingle}'s FQN-witness overload.
 *
 * <p>Whole-file byte anchor through the REAL D11 loader (which loads the transitive rune-fpml dep per
 * PR #184). Anchored: {@code MapPartyList} (cdm6 ingest-fpml confirmation.party) — the ONE clean carrier
 * (witness {@code fpml.consolidated.shared.Party} of the {@code fpmlParties} input param vs the CDM
 * output {@code cdm.base.staticdata.party.Party}). The two census siblings
 * {@code MapAveragingObservations}/{@code MapCalculationPeriodList} carry the SAME witness-FQN fix but
 * were heavily co-occupied with other mechanisms at the time, so they are NOT anchored here.
 * (PR #344: MapCalculationPeriodList's co-occupants — the navWalkChoiceDisguise walk cascade —
 * landed and the file byte-flipped; its whole-file lock lives in
 * {@code NavWalkChoiceDisguiseComposeTest}. PR #347: MapAveragingObservations's co-occupant —
 * the addHoistLocalParamEscape session-replay law — landed and the file byte-flipped; its
 * whole-file lock lives in {@code ThenSetAddEscapeComposeTest}.)
 */
class FunctionMapperCWitnessFqnTest {

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

    /** The clean carrier: MapperC witness {@code fpml.consolidated.shared.Party} renders FQN-inline + its import dropped. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapPartyList_cdm6_mapperCWitnessFqnInline_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/party/functions/MapPartyList.java");
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
                + " — the non-compiling duplicate `import fpml.consolidated.shared.Party;` + the bare "
                + "`MapperC.<Party>of(...)` witness reappear if the fqnWitnessMapperC fix "
                + "(ReferenceHandler.mapperCWitnessOutputCollisionFqn → JavaExpression.wrappedInMapperCOfSingle "
                + "FQN-witness overload: render the witness FQN-inline + suppress its import) is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
