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
 * Facet {@code accessorTypeEscape} (PR #242) — a function-navigation lambda body that
 * accesses an attribute literally named {@code type} (or {@code class}) escapes the
 * accessor to {@code _getType()} / {@code _getClass()}, matching the golden.
 *
 * <p>The golden escapes the accessor because {@code getType}/{@code getClass} clash with
 * inherited {@code RosettaModelObject.getType()} / {@code Object.getClass()} methods — the
 * POJO subsystem ({@code JavaPojoProperty.getOperationName} → {@code escapeOperationName},
 * the {@code OPERATION_NAMES_TO_ESCAPE = {getClass, getType}} SOT) already declares the
 * escaped accessor on the interface. Pre-fix, {@code NavigationHandler.handle(RFeatureCall)}
 * re-derived the accessor name as the bare {@code getter} for BOTH the witness/map LABEL and
 * the lambda-BODY invocation, so it emitted the inherited-method-shadowing
 * {@code telephoneNumber.getType()} where the golden has {@code telephoneNumber._getType()}
 * (an {@code incompatible-types}/cannot-find-symbol NON_COMPILING mismatch — already waivered).
 * The fix routes ONLY the lambda-body invocation through {@code JavaPojoProperty.escapeOperationName}
 * (the shared SOT); the witness/map LABEL stays the bare {@code "getType"} (golden keeps it bare).
 *
 * <p>GREEN-SAFE BY CONSTRUCTION: the escape is an EXACT match on {@code {getType, getClass}}.
 * No golden FUNCTION file carries a bare {@code -> X.getType()} lambda body (every golden
 * navigation of a {@code type} attribute is already {@code ._getType()}, corpus-wide), so the
 * escape only ever touches currently-waivered output. A {@code getType}-CONTAINING getter
 * (e.g. {@code getIdentifierType}) is NOT escaped — locked by {@link #assetIdentifierByType}.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens (newline-normalized),
 * generated through the REAL {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class FunctionAccessorTypeEscapeTest {

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

    /**
     * Carrier: {@code MapTelephoneNumber} navigates the fpml {@code TelephoneNumber.type}
     * enum attribute — golden {@code telephoneNumber._getType()}; the witness label stays
     * {@code map("getType", …)}. Reverting the escape re-emits the bare {@code .getType()}.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapTelephoneNumberCdm6_accessorEscaped_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/party/functions/MapTelephoneNumber.java");
    }

    /** Carrier: {@code MapConfirmationLegalAgreement} — {@code contractualTermsSupplement._getType()}. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapConfirmationLegalAgreementCdm6_accessorEscaped_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/legal/functions/MapConfirmationLegalAgreement.java");
    }

    /** Carrier: {@code MapCreditSupportAgreement} — {@code creditSupportAgreement._getType()}. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapCreditSupportAgreementCdm6_accessorEscaped_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/legal/functions/MapCreditSupportAgreement.java");
    }

    /**
     * GREEN-SAFETY lock: {@code AssetIdentifierByType} (green) navigates
     * {@code assetIdentifier.getIdentifierType()} — a getter CONTAINING {@code Type} but not
     * equal to {@code getType}, so the EXACT-match escape is a no-op. Locks that the fix does
     * NOT over-fire on a substring/prefix {@code …Type} getter (would corrupt it to
     * {@code _getIdentifierType()}).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void assetIdentifierByType_typeContainingGetterUnescaped_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/base/staticdata/asset/common/functions/AssetIdentifierByType.java");
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
                + " — the accessorTypeEscape fix (NavigationHandler routes the lambda-body getType/"
                + "getClass accessor through JavaPojoProperty.escapeOperationName) is missing or "
                + "regressed if reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
