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
 * Facet {@code bare_enum_resolution} (PR #239) — the bare-enum #154 family at three
 * residual seats where a BARE Rosetta enum value was emitted raw ({@code MapperS.of(Commodity)},
 * {@code .setPriceType(CashPrice)}, {@code .getOrDefault(Name)}) instead of the qualified
 * {@code EnumType.CONSTANT} (a non-compiling undefined / type-used-as-value symbol — every
 * carrier already a waivered {@code incompatible-types}/{@code cannot-find-symbol} mismatch):
 *
 * <ol>
 *   <li><b>Comparison operand — disguised alias-headed sibling.</b> A bare enum operand whose
 *       SIBLING is a disguised {@code REnumValueRef} navigation with an ALIAS receiver
 *       ({@code quotedCurrencyPair(...) -> quoteBasis}) — {@code NavigationHandler.leafEnumeration}
 *       resolves the disguised receiver with the compiler-LESS resolver and returns null, so the
 *       new {@code NavigationHandler.disguisedSiblingEnumeration} (compiler-carrying alias arm)
 *       recovers {@code QuoteBasisEnum} and {@code ComparisonHandler.tryBareEnumComparand} qualifies
 *       {@code QuoteBasisEnum.CURRENCY_2_PER_CURRENCY_1}
 *       ({@code GetFpmlExchangedCurrency}/{@code MapExchangeRateToPrice}/
 *       {@code MapFxCoreDetailsModelQuantityWithAddress}, cdm6).</li>
 *   <li><b>Comparison operand AND ctor-setter — TYPE-SHADOW.</b> A bare enum value whose simple
 *       name collides with a model type of the same name ({@code Commodity} is both
 *       {@code AssetClassEnum.Commodity} and the {@code Commodity} data type; {@code CashPrice} is
 *       both {@code PriceTypeEnum.CashPrice} and the {@code CashPrice} type) resolves its
 *       {@code RSymbolReference} symbol to that TYPE, so the STRICT
 *       {@code HandlerHelper.bareEnumValueName} declines. The new
 *       {@code HandlerHelper.typeShadowEnumValueName} admits the candidate name, gated by the
 *       sibling-/attribute-enum value-match that keeps a genuine type reference from being rewritten
 *       ({@code IsCommoditySwap_SingleIndex} comparison drr;
 *       {@code MapCommmodityFixedPriceToPriceSchedule}/
 *       {@code MapCommmodityFixedPriceScheduleToPriceSchedule} ctor-setter cdm6).</li>
 *   <li><b>{@code default} right operand.</b> A bare enum-value {@code default} right
 *       ({@code <l> default Name}) renders the un-prefixed {@code Name}; the new
 *       {@code SetOperationHandler.tryDefaultBareEnumRightOrNull} qualifies it from the
 *       {@code RDefaultExpr}'s OWN joined inferred type ({@code AssetIdTypeEnum}, where {@code Name}
 *       is inherited from {@code ProductIdTypeEnum} so the match walks the {@code extends} chain) —
 *       {@code .getOrDefault(AssetIdTypeEnum.NAME)} ({@code MapIndexIdToAssetIdentifier}, cdm6).</li>
 * </ol>
 *
 * <p>GREEN-SAFE BY CONSTRUCTION: every arm fires only when the recovered/target enum DECLARES the
 * bare value name (the load-bearing match gate), and the pre-fix bare render is a non-compiling
 * symbol, so the rewrite only ever touches currently-waivered output. A resolved enum operand
 * carries a present {@code enumeration()}/symbol, so both name-extractors decline and the resolved
 * path is untouched — {@code GetPriceNotation} (drr) compares a RESOLVED
 * {@code PriceTypeEnum.CASH_PRICE} against a disguised {@code priceType} sibling and stays
 * byte-identical (the green-safety lock).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens (newline-normalized),
 * generated through the REAL {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class FunctionBareEnumResolutionTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;
    private static Map<String, String> drrFunctionOutput;

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
        if (cdm6CellAvailable()) {
            cdm6FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
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

    // -------------------------------------------------------------------------
    // Seat 1 — comparison operand, disguised alias-headed sibling (QuoteBasisEnum)
    // -------------------------------------------------------------------------

    /**
     * Disguised alias-headed sibling: {@code quotedCurrencyPair(...) -> quoteBasis} parses as an
     * {@code REnumValueRef} whose alias receiver resolves only through the compiler-carrying disguise
     * resolver, so {@code disguisedSiblingEnumeration} recovers {@code QuoteBasisEnum} and the bare
     * {@code Currency2PerCurrency1} qualifies to {@code QuoteBasisEnum.CURRENCY_2_PER_CURRENCY_1} (cdm6).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void getFpmlExchangedCurrencyCdm6_disguisedAliasSiblingEnum_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/pricequantity/functions/GetFpmlExchangedCurrency.java");
    }

    /** Sibling case of {@code GetFpmlExchangedCurrency}: {@code Currency1PerCurrency2} (cdm6). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapExchangeRateToPriceCdm6_disguisedAliasSiblingEnum_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/pricequantity/functions/MapExchangeRateToPrice.java");
    }

    /** Two-input alias-headed sibling {@code quotedCurrencyPair(model, leg) -> quoteBasis} (cdm6). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapFxCoreDetailsModelQuantityWithAddressCdm6_disguisedAliasSiblingEnum_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/pricequantity/functions/MapFxCoreDetailsModelQuantityWithAddress.java");
    }

    // -------------------------------------------------------------------------
    // Seat 2 — type-shadow (Commodity / CashPrice / PrincipalPayment)
    // -------------------------------------------------------------------------

    /**
     * Comparison TYPE-SHADOW: bare {@code Commodity} resolves its symbol to the {@code Commodity} data
     * type, so the strict gate declines; {@code typeShadowEnumValueName} admits it and the sibling
     * {@code AssetClassEnum} leaf qualifies {@code AssetClassEnum.COMMODITY} (drr).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void isCommoditySwapSingleIndexDrr_comparisonTypeShadow_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/IsCommoditySwap_SingleIndex.java");
    }

    /**
     * Ctor-setter TYPE-SHADOW: {@code priceType: CashPrice} where {@code CashPrice} resolves its
     * symbol to the {@code CashPrice} data type; the attribute enum {@code PriceTypeEnum} declares the
     * value, so {@code tryCtorEnumQualify} emits {@code .setPriceType(PriceTypeEnum.CASH_PRICE)} +
     * import (cdm6).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapCommmodityFixedPriceToPriceScheduleCdm6_ctorTypeShadow_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/pricequantity/functions/MapCommmodityFixedPriceToPriceSchedule.java");
    }

    /** Sibling ctor type-shadow of the schedule variant (cdm6). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapCommmodityFixedPriceScheduleToPriceScheduleCdm6_ctorTypeShadow_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/pricequantity/functions/MapCommmodityFixedPriceScheduleToPriceSchedule.java");
    }

    // -------------------------------------------------------------------------
    // Seat 3 — default right operand
    // -------------------------------------------------------------------------

    /**
     * {@code default} right operand: {@code mapAssetIdType.evaluate(...) default Name} — the bare
     * {@code Name} qualifies from the default's own inferred {@code AssetIdTypeEnum} (where
     * {@code Name} is inherited from {@code ProductIdTypeEnum}, so the match walks the {@code extends}
     * chain) to {@code .getOrDefault(AssetIdTypeEnum.NAME)} + import (cdm6).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapIndexIdToAssetIdentifierCdm6_defaultBareEnum_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/pricequantity/functions/MapIndexIdToAssetIdentifier.java");
    }

    // -------------------------------------------------------------------------
    // Green-safety lock — a RESOLVED enum comparison must stay untouched
    // -------------------------------------------------------------------------

    /**
     * GREEN-SAFETY lock: {@code GetPriceNotation} compares a RESOLVED enum value
     * ({@code PriceTypeEnum.CashPrice}, carrying a present {@code enumeration()}) against a disguised
     * {@code priceType} sibling. Both name-extractors decline a resolved operand, so
     * {@code tryBareEnumComparand} returns before recovering the (disguised) sibling enum, and the
     * file stays byte-identical. Locks that the PR #239 type-shadow + disguised-sibling fallbacks do
     * NOT perturb the resolved-enum comparison path (drr).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getPriceNotationDrr_resolvedEnumComparisonUnchanged_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/GetPriceNotation.java");
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
                + " — the bare_enum_resolution facet (PR #239: the disguised alias-headed "
                + "sibling enum recovery, the type-shadow bare-value admission, or the "
                + "default-right bare-enum qualify) is missing or regressed if reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
