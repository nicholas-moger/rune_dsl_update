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
 * Facet {@code importCollisionFqn} (PR #245) — the upstream
 * {@code ImportingStringConcatenation} first-claim-wins import-collision law (the #194-#197 /
 * #227 {@code fqnWitness} family) at two render seats the {@link
 * com.regnosys.rosetta.generator.java.template.ImportCollisionResolver}'s model did not reach.
 * When two DIFFERENT canonical types share a Java simple name in one generated class, the fork
 * imported BOTH (a duplicate same-simple-name import = a Java compile error), so every carrier was
 * already a waivered mismatch. Golden lets the FIRST claim of a simple name keep the bare name +
 * import and renders the later different-canonical reference FULLY-QUALIFIED inline with NO import.
 *
 * <ul>
 *   <li><b>Fix A</b> — the {@code FieldWithMeta} {@code withMetaArgument} toBuilder-decl outer type
 *       ({@code ConstructionHandler.tryTypedWithMeta}) now emits an
 *       {@code ImportCollisionResolver.typeRef} sentinel, so a same-simple-name collision with the
 *       fpml INPUT param (already seeded) renders the cdm value type FQN-inline (carrier
 *       {@code MapQuotedCurrencyPairWithLocation}: golden
 *       {@code cdm.observable.asset.QuotedCurrencyPair.QuotedCurrencyPairBuilder}).</li>
 *   <li><b>Fix B</b> — the alias signature return-type ELEMENT canonical is now SEEDED
 *       ({@code FunctionAliasHelper} → {@code AliasModel.returnTypeElementFqn} →
 *       {@code FunctionGenerator}); the alias signatures are emitted ahead of the bodies, so a body
 *       witness / builder-ctor whose canonical differs from a seeded alias return type loses the
 *       first-claim and renders FQN-inline with its import suppressed (carriers
 *       {@code MapFraPayoutList}: the fpml {@code AdjustableDate} witness loses to the cdm alias
 *       return type; {@code MapFxPerformanceSwapToObservationTerms}: the cdm {@code FxSpotRateSource}
 *       ctor loses to the fpml alias return type).</li>
 * </ul>
 *
 * <p>GREEN-SAFE BY CONSTRUCTION (corpus-verified, frozen 9.83.0 baseline): a same-simple-name
 * collision is a duplicate import = a compile error = already waivered, so the fix only ever touches
 * waivered output; OFF-collision every sentinel resolves to the bare simple name and the seed adds a
 * claim no different-canonical reference contests, so the output is byte-identical — locked GREEN by
 * {@link #mapBusinessCenter_withMetaArgumentBareNoCollision_byteMatchesGolden} (Fix A: a bare
 * {@code withMetaArgument} decl stays bare) and
 * {@link #equityPerformance_aliasReturnSeedNoCollision_byteMatchesGolden} (Fix B: an alias return
 * element + a same-canonical body witness both stay bare). DEFERRED: {@code MapExerciseTerms} — the
 * alias return type is itself the LOSER (in the signature, not a body), needing the return-type
 * rendering sentinel-ized (a separate sub-mechanism).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens (newline-normalized),
 * generated through the REAL {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class FunctionImportCollisionFqnTest {

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
    // Carriers — Fix A: withMetaArgument toBuilder-decl outer type FQN-ed
    // -------------------------------------------------------------------------

    /**
     * Carrier (Fix A): {@code MapQuotedCurrencyPairWithLocation} takes an fpml
     * {@code QuotedCurrencyPair} INPUT param (seeded) and builds the cdm
     * {@code QuotedCurrencyPair} via the FieldWithMeta {@code withMetaArgument} hoist — the two
     * canonicals share the simple name, so golden FQN-s the decl type
     * {@code cdm.observable.asset.QuotedCurrencyPair.QuotedCurrencyPairBuilder} and drops the cdm
     * import. Reverting the {@code typeRef} sentinel re-emits the bare colliding decl.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapQuotedCurrencyPairWithLocation_withMetaArgumentFqn_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/common/functions/MapQuotedCurrencyPairWithLocation.java");
    }

    // -------------------------------------------------------------------------
    // Carriers — Fix B: alias signature return element seeded; body loses
    // -------------------------------------------------------------------------

    /**
     * Carrier (Fix B): {@code MapFraPayoutList}'s alias {@code paymentDate} returns the cdm
     * {@code AdjustableDate} (signature, seeded first), while the body navigation witness
     * {@code .<AdjustableDate>map("getPaymentDate", …)} is the fpml {@code AdjustableDate} — golden
     * FQN-s the witness {@code <fpml.consolidated.shared.AdjustableDate>} and drops the fpml import.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapFraPayoutList_aliasReturnSeedWitnessFqn_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/fra/functions/MapFraPayoutList.java");
    }

    /**
     * Carrier (Fix B): {@code MapFxPerformanceSwapToObservationTerms}'s alias
     * {@code fpmlFixingInformationSource} returns the fpml {@code FxSpotRateSource} (signature,
     * seeded first), while a setter builds the cdm {@code FxSpotRateSource} ctor — golden FQN-s the
     * ctor {@code cdm.observable.asset.FxSpotRateSource.builder()} and drops the cdm import (the fpml
     * alias-return witness keeps the bare name + import).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapFxPerformanceSwapToObservationTerms_aliasReturnSeedCtorFqn_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/fxvarianceswap/functions/MapFxPerformanceSwapToObservationTerms.java");
    }

    // -------------------------------------------------------------------------
    // Green-safety locks — the OFF-collision DECLINE path
    // -------------------------------------------------------------------------

    /**
     * GREEN-SAFETY lock (Fix A): {@code MapBusinessCenter} (green) builds a FieldWithMeta
     * {@code withMetaArgument} whose value type {@code BusinessCenterEnum} does NOT collide (the fpml
     * input is {@code fpml.consolidated.shared.BusinessCenter}, a different simple name) — the
     * {@code typeRef} sentinel resolves to the bare {@code BusinessCenterEnum}. Locks that a
     * non-colliding {@code withMetaArgument} decl is NOT corrupted into an FQN form.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapBusinessCenter_withMetaArgumentBareNoCollision_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/datetime/functions/MapBusinessCenter.java");
    }

    /**
     * GREEN-SAFETY lock (Fix B): {@code EquityPerformance} (green) has an alias returning
     * {@code MapperS<? extends PerformancePayout>} (seeded) AND a body witness
     * {@code .<PerformancePayout>map(…)} of the SAME canonical — the seed adds a claim the witness
     * does not contest, so the witness stays bare. Locks that the alias-return-type seed is a NO-OP
     * off-collision (no spurious FQN of a same-canonical body witness).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void equityPerformance_aliasReturnSeedNoCollision_byteMatchesGolden() throws IOException {
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
                + " — the importCollisionFqn fix (ConstructionHandler.tryTypedWithMeta typeRef "
                + "sentinel + the FunctionGenerator alias-return-element seed) is missing or "
                + "regressed if reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
