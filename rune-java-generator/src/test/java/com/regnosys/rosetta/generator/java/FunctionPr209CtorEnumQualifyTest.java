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
 * PR #209 — facet ctorSetterEnumQualify (15 cdm6 FUNCTION flips), whole-file byte anchors
 * through the REAL D11 loader. Each anchor reverts RED if the fix is removed; the law is
 * green-safe by construction (the pre-fix bare splice never compiled, so every carrier was
 * already waivered — stash-baseline confirmed 0 now-matching pristine).
 *
 * <p><b>The law.</b> A BARE unresolved enum value used as a ctor-setter argument (e.g.
 * {@code ExerciseTerms { style: European, ... }}) parses as an UNRESOLVED
 * {@code RSymbolReference} (enum values are not in function scope — PR #154/#206), so the fork
 * rendered the bare rune name {@code European} — a non-compiling undefined symbol — where the
 * attribute is an enum type. Root cause: {@code ConstructionHandler.coerceCtorArg}'s
 * single-attribute bare-return path spliced the unwrapped value verbatim with no enum
 * qualification. Fix: {@code tryCtorEnumQualify} — gated to an unresolved bare enum reference whose
 * name matches a value of the attribute's enum RType ({@code REnumTypeRef}), emit
 * {@code EnumName.CONSTANT} ({@code EnumHelper.convertValue}: {@code European → EUROPEAN}) + the
 * enum import, resolved DIRECTLY from the attribute's enum type (no sibling inference, unlike the
 * comparison-operand #206 enumQualify). All 15 carriers are cdm6 ingest-fpml {@code Map*}.
 */
class FunctionPr209CtorEnumQualifyTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR = CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;

    static boolean cdm6Available() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cdm6Available()) {
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

    /** style: European → OptionExerciseStyleEnum.EUROPEAN (single-word constant). */
    @Test
    @EnabledIf("cdm6Available")
    void mapCommodityPhysicalEuropeanExercise_styleEnum_byteMatchesGolden() throws IOException {
        assertByteMatches("cdm/ingest/fpml/confirmation/common/functions/MapCommodityPhysicalEuropeanExercise.java",
                "the qualified `.setStyle(OptionExerciseStyleEnum.EUROPEAN)` + import reverts to the bare "
                + "`.setStyle(European)` — tryCtorEnumQualify removed");
    }

    /** settlementType: Physical → SettlementTypeEnum.PHYSICAL (a second enum type / setter). */
    @Test
    @EnabledIf("cdm6Available")
    void mapSwaptionPhysicalSettlement_settlementTypeEnum_byteMatchesGolden() throws IOException {
        assertByteMatches("cdm/ingest/fpml/confirmation/product/swaption/functions/MapSwaptionPhysicalSettlementToSettlementTerms.java",
                "the qualified `.setSettlementType(SettlementTypeEnum.PHYSICAL)` + import reverts to the bare value");
    }

    /** identifierType: CurrencyCode → AssetIdTypeEnum.CURRENCY_CODE (a compound camelCase → SNAKE constant). */
    @Test
    @EnabledIf("cdm6Available")
    void mapMoneyToTransferCashAsset_identifierTypeEnum_byteMatchesGolden() throws IOException {
        assertByteMatches("cdm/ingest/fpml/confirmation/payment/functions/MapMoneyToTransferCashAsset.java",
                "the qualified `.setIdentifierType(AssetIdTypeEnum.CURRENCY_CODE)` + import reverts to the bare value");
    }

    /** cashSettlementMethod: CollateralizedCashPriceMethod → the long multi-underscore constant. */
    @Test
    @EnabledIf("cdm6Available")
    void mapCollateralizedCashPriceMethod_cashSettlementMethodEnum_byteMatchesGolden() throws IOException {
        assertByteMatches("cdm/ingest/fpml/confirmation/settlement/functions/MapCollateralizedCashPriceMethodToCashSettlementTerms.java",
                "the qualified `.setCashSettlementMethod(CashSettlementMethodEnum.COLLATERALIZED_CASH_PRICE_METHOD)` "
                + "+ import reverts to the bare value");
    }

    private static void assertByteMatches(String path, String revertHint) throws IOException {
        assertNotNull(cdm6FunctionOutput, "Function generation did not run — corpus unavailable?");
        String generated = cdm6FunctionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = CDM6_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — " + revertHint + ".");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
