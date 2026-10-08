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
 * Facet {@code importCollisionFqn} RETURN-TYPE (PR #247) — the #245-deferred completion of the
 * upstream {@code ImportingStringConcatenation} first-claim-wins import-collision law (the #194-#197
 * / #227 / #245 {@code fqnWitness} family) at the seat #245 explicitly deferred: the alias signature
 * return type is itself the LOSER.
 *
 * <p>When an alias's Mapper return-type ELEMENT (a cdm model type) shares its Java simple name with
 * an INPUT param whose canonical differs (a fpml type seeded first), the fork imported BOTH — a
 * duplicate same-simple-name import = a Java compile error — so every carrier was already a waivered
 * mismatch. Golden keeps the first-claim (the fpml input param) bare + imported and renders the cdm
 * return element FULLY-QUALIFIED inline at all three signature/body sites (the abstract method decl,
 * the impl method decl, the typed-empty else {@code MapperS.<Item>ofNull()}) with NO cdm import.
 *
 * <p><b>Fix</b> ({@code FunctionAliasHelper.buildMapperReturnType} + {@code FunctionGenerator}): the
 * model-type return element is emitted as an {@code ImportCollisionResolver.typeRef} sentinel
 * carrying the canonical (via {@code ExpressionTypeInfo.javaItemCanonical}); the abstract + impl
 * method declarations render it (and {@code typedEmptyElseOrNull} lifts it verbatim into the body
 * {@code ofNull}), and {@code FunctionGenerator} resolves the return-type STRINGS in the SAME
 * import-collision pass as the bodies — seeded with the signature output/input/super + alias-return
 * elements (PR #245) — so a return element that LOSES to a seeded different-canonical input renders
 * FQN-inline with its import suppressed.
 *
 * <p>GREEN-SAFE BY CONSTRUCTION (corpus-verified, frozen 9.83.0 baseline): a same-simple-name
 * collision is a duplicate import = a compile error = already waivered, so the fix only ever touches
 * waivered output. The sentinel is emitted ONLY when the return element actually collides with a
 * signature type (a same-simple-name input/output of a different canonical); a non-colliding
 * model-type alias return renders bare DIRECTLY and never enters the import-collision resolution
 * path (the fast path) — locked GREEN by
 * {@link #mapCurrencyAmountToQuantity_aliasReturnTypeNoCollision_byteMatchesGolden} (a green
 * ladder+ofNull alias whose {@code UnitType} return element collides with no param, so the gate
 * emits no sentinel and BOTH the abstract/impl decls AND the {@code MapperS.<UnitType>ofNull()}
 * stay bare) and {@link #equityPerformance_aliasReturnTypeWinnerBare_byteMatchesGolden} (a green
 * alias whose {@code PerformancePayout} return element is the seed WINNER — it stays bare while a
 * same-canonical body witness also stays bare).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens (newline-normalized),
 * generated through the REAL {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class FunctionImportCollisionFqnReturnTypeTest {

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

    // -------------------------------------------------------------------------
    // Carrier — the alias return type is itself the loser
    // -------------------------------------------------------------------------

    /**
     * Carrier: {@code MapExerciseTerms} takes an fpml {@code ExerciseProcedure} INPUT param
     * ({@code fpml.consolidated.shared.ExerciseProcedure}, seeded first) and its alias
     * {@code cdmExerciseProcedure} returns the cdm {@code ExerciseProcedure}
     * ({@code cdm.product.template.ExerciseProcedure}). The two canonicals share the simple name, so
     * golden FQN-s the cdm return element at all three sites — the abstract decl, the impl decl, and
     * the {@code MapperS.<cdm.product.template.ExerciseProcedure>ofNull()} typed-empty else — and
     * drops the cdm import (the fpml input keeps the bare name). Reverting the return-type sentinel +
     * the {@code FunctionGenerator} return-type resolution re-emits the bare colliding form (a
     * duplicate same-simple-name import = the pre-facet compile error).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapExerciseTerms_aliasReturnTypeLoserFqn_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/common/functions/MapExerciseTerms.java");
    }

    // -------------------------------------------------------------------------
    // Green-safety locks — the OFF-collision (no-op) path
    // -------------------------------------------------------------------------

    /**
     * GREEN-SAFETY lock: {@code MapCurrencyAmountToQuantity} (green) has a conditional alias
     * {@code mappedUnit} returning {@code MapperS<? extends UnitType>} with a typed-empty else
     * {@code MapperS.<UnitType>ofNull()}. {@code UnitType} collides with NO param (the inputs are
     * {@code BigDecimal} / {@code Step} / {@code Currency} / {@code FinancialUnitEnum}), so the
     * collision gate emits NO sentinel and the element renders bare DIRECTLY (the fast path) at the
     * abstract decl, the impl decl AND the {@code ofNull}. Locks that the return-type fix is a byte
     * no-op for a non-colliding model-type alias return — it does not corrupt the {@code ofNull}
     * and does not put a non-colliding function on the import-collision resolution path.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapCurrencyAmountToQuantity_aliasReturnTypeNoCollision_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/pricequantity/functions/MapCurrencyAmountToQuantity.java");
    }

    /**
     * GREEN-SAFETY lock: {@code EquityPerformance} (green) has an alias returning
     * {@code MapperS<? extends PerformancePayout>} (the seed WINNER for its simple name) AND a body
     * witness {@code .<PerformancePayout>map(…)} of the SAME canonical — the return-element sentinel
     * resolves bare (seed-matched) and the same-canonical body witness it adds to {@code everBare}
     * keeps the witness bare too. Locks that the return-type sentinel + the #245 alias-return seed
     * stay a no-op for a winner (no spurious FQN of either the return type or a same-canonical
     * witness), complementing the #245 {@code FunctionImportCollisionFqnTest} lock on the same file.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void equityPerformance_aliasReturnTypeWinnerBare_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/common/functions/EquityPerformance.java");
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
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — the importCollisionFqn return-type fix (FunctionAliasHelper.buildMapperReturnType "
                + "typeRef sentinel + the FunctionGenerator alias-return-type resolution) is missing or "
                + "regressed if reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
