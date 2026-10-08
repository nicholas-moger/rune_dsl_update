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
 * Facet {@code aliasSigDisguisedChainReceiver} (PR #228) — a chained alias whose
 * navigation RECEIVER is itself a disguised 2-name {@code head -> feature} chain
 * (an {@link com.regnosys.rosetta.ast.expressions.references.REnumValueRef} the
 * grammar cannot disambiguate at parse time), or one bottoming out in a deep call
 * or an {@code only-element}/{@code first}/{@code last} list op, lost its receiver
 * type in {@code FunctionAliasHelper.inferRTypeFromExpr}. That receiver-type walker
 * — which {@code lookupParameterType} / {@code inferReceiverRType} drive for a
 * shortcut receiver — handled only {@code RFeatureCall} and {@code RSymbolReference}
 * (NOT the shapes the complete {@code inferExpressionType} / {@code inferReceiverRType}
 * walkers handle), so the resolution returned {@code null} and the consuming alias
 * signature leaked the receiver NAME as a bogus type arg:
 * <ul>
 *   <li>{@code MapperS<product>} instead of golden {@code MapperS<? extends EconomicTerms>}
 *       — the cdm {@code economicTerms} alias body {@code product -> economicTerms} where
 *       {@code product} is the alias {@code trade -> product};</li>
 *   <li>{@code MapperS<params>} / {@code MapperS<optionPayout>} — the same shape with a
 *       deep-call / only-element receiver.</li>
 * </ul>
 *
 * <p>PR #228 adds the missing {@code REnumValueRef}, {@code RDeepFeatureCall} and
 * {@code RListOpExpr(only-element/first/last)} arms to {@code inferRTypeFromExpr}: the
 * {@code RDeepFeatureCall} and {@code RListOpExpr} arms mirror the sibling
 * {@code inferReceiverRType} verbatim, and the {@code REnumValueRef} arm mirrors
 * {@code inferEnumValueRefType}'s {@code paramName->featureName} resolution (with the
 * same {@code enumeration().isEmpty()} guard the receiver-walker carries). The receiver
 * then resolves to its navigated element type and the signature renders the golden
 * {@code MapperS<? extends T>}.
 *
 * <p><b>Green-safe by construction:</b> a leaked lowercase type arg never compiles,
 * so every carrier was already a waivered mismatch; and a green file whose signature
 * matches via the output-type fallback has {@code OutputType == FeatureType} (the
 * fallback and the resolved type coincide), so the fix is a no-op there. The PRE/POST
 * regression scan over all still-waivered files is 0 regressions / 0 churn; the full
 * 5-cell D11 matrix (20/20) is the empirical arbiter. The facet pays 6 FUNCTION flips
 * (cdm5 ×3 + cdm6 ×3).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}, mirroring the 6 flips 1:1 —
 * reverting the {@code inferRTypeFromExpr} arms restores the {@code MapperS<receiverName>}
 * leak and turns these RED.
 */
class FunctionAliasSigDisguisedChainReceiverTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm5FunctionOutput;
    private static Map<String, String> cdm6FunctionOutput;

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
        if (cdm5CellAvailable()) {
            cdm5FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
        }
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

    // ---- cdm6 ----------------------------------------------------------------

    /**
     * StandardizedScheduleAssetClass (cdm6) — the {@code economicTerms} alias body
     * {@code product -> economicTerms} (an {@code REnumValueRef} whose head {@code product}
     * is the alias {@code trade -> product}). The {@code product} receiver resolves through
     * the new {@code REnumValueRef} arm; the leak was {@code MapperS<product>}.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void standardizedScheduleAssetClass_renumChainReceiver_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/margin/schedule/functions/StandardizedScheduleAssetClass.java");
    }

    /** StandardizedScheduleProductClass (cdm6) — the sibling {@code economicTerms} alias carrier. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void standardizedScheduleProductClass_renumChainReceiver_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/margin/schedule/functions/StandardizedScheduleProductClass.java");
    }

    /**
     * ProcessObservations (cdm6) — the {@code cap}/{@code floor} aliases whose
     * disguised-chain receiver leaked {@code MapperS<params>}.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void processObservationsCdm6_disguisedChainReceiver_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/observable/asset/calculatedrate/functions/ProcessObservations.java");
    }

    // ---- cdm5 ----------------------------------------------------------------

    /** ProcessObservations (cdm5) — the cdm5 twin of the {@code MapperS<params>} carrier. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void processObservationsCdm5_disguisedChainReceiver_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/observable/asset/calculatedrate/functions/ProcessObservations.java");
    }

    /** EquityPerformance (cdm5) — a disguised-chain-receiver alias-signature carrier. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void equityPerformance_disguisedChainReceiver_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/common/functions/EquityPerformance.java");
    }

    /**
     * Qualify_EquityOption_PriceReturnBasicPerformance_SingleName (cdm5) — the
     * {@code optionUnderlier} alias body {@code optionPayout -> underlier} where the receiver
     * {@code optionPayout} bottoms out in an {@code only-element} list op (the new
     * {@code RListOpExpr} arm); the leak was {@code MapperS<optionPayout>} vs golden
     * {@code MapperS<? extends Product>}.
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void qualifyEquityOptionSingleName_onlyElementChainReceiver_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/product/qualification/functions/Qualify_EquityOption_PriceReturnBasicPerformance_SingleName.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String path) throws IOException {
        assertNotNull(output, "Function generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " — a chained alias whose receiver is a disguised 2-name chain / deep "
                + "call / only-element list op leaks the receiver NAME as the signature type arg "
                + "(MapperS<receiverName>) if the inferRTypeFromExpr REnumValueRef / RDeepFeatureCall "
                + "/ RListOpExpr arms are reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
