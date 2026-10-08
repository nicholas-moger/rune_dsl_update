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
 * PR #207 — navigation set-cover entry: two disjoint root-law facets (20 FUNCTION flips),
 * whole-file byte anchors through the REAL D11 loader. Each anchor reverts RED if its law's
 * fix is removed; both laws are green-safe by construction (the pre-fix forms never compiled,
 * so every carrier was already waivered — combined revert-baseline confirmed 0 now-matching
 * pristine).
 *
 * <p><b>Law 1 — witnessDrop (3 cdm6 flips).</b> A navigate-by-type choice-option step
 * ({@code transferableProduct -> Commodity}) whose target option lives on the receiver type's
 * CHOICE supertype ({@code type TransferableProduct extends Asset} where {@code Asset} is a
 * {@code choice} and {@code Commodity} is one of its options) dropped its {@code <Commodity>}
 * generic witness ({@code .map("getCommodity", ...)} not {@code .<Commodity>map(...)}). Root cause:
 * {@code RDataType.superType()} is EMPTY for an extends-choice relationship (the resolved choice
 * node is held by the SEPARATE {@code RDataType.choiceSuperType()}), so the data-supertype walk in
 * {@code HandlerHelper.findAttributeOnDataType} never reached the option. Fix:
 * {@code NavigationHandler.findChoiceSuperOption} — when the direct lookup fails, match the
 * (type-named) feature against the choice supertype's options by TYPE NAME (the option identity per
 * {@code RChoiceTypeRef.asRDataType}) and return a synthetic attribute carrying the option's
 * deep-copied typeCall. Exemplars Qualify_Commodity_Option / Qualify_AssetClass_Commodity.
 *
 * <p><b>Law 2 — numLit (17 drr flips).</b> An int-literal comparison operand whose SIBLING is a
 * resolution-blind navigation onto a number/int type-ALIAS attribute (e.g. drr's
 * {@code valuationAmount ShortFraction5DecimalNumber}, a {@code number} alias) stayed unwrapped
 * ({@code MapperS.of(0)} not {@code MapperS.of(BigDecimal.valueOf(0))}). Root cause:
 * {@code HandlerHelper.kindFromTypeCall} raw-string-matched only the bare builtins
 * {@code "number"}/{@code "int"}, missing the alias name, so the typed-model literal-typing arm
 * ({@code ComparisonHandler.literalSiblingNumericType} → {@code numericOperandKind}) declined.
 * Fix: {@code kindFromTypeCall} resolves the typeCall transitively through the engine + translator
 * ({@code ShortFraction5DecimalNumber} → BigDecimal → NUMBER), the SAME alias resolution the witness
 * path already uses; declines (no byte delta) for any non-numeric resolved type.
 */
class FunctionPr207NavLiteralTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR = CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR = DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;
    private static Map<String, String> drrFunctionOutput;

    static boolean cdm6Available() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    static boolean drrAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cdm6Available()) {
            cdm6FunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
        if (drrAvailable()) {
            drrFunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
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

    /** witnessDrop: `... -> TransferableProduct -> Commodity` recovers the `<Commodity>` witness via the Asset choice supertype. */
    @Test
    @EnabledIf("cdm6Available")
    void cdm6QualifyCommodityOption_choiceSuperOptionWitness_byteMatchesGolden() throws IOException {
        assertByteMatches(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/product/qualification/functions/Qualify_Commodity_Option.java",
                "the `.<Commodity>map(\"getCommodity\", ...)` witness reverts to the bare `.map(\"getCommodity\", ...)` — "
                + "findChoiceSuperOption removed (the Asset choice supertype's Commodity option is no longer matched)");
    }

    /** witnessDrop sibling carrier (same TransferableProduct -> Commodity choice-super-option witness). */
    @Test
    @EnabledIf("cdm6Available")
    void cdm6QualifyAssetClassCommodity_choiceSuperOptionWitness_byteMatchesGolden() throws IOException {
        assertByteMatches(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/product/qualification/functions/Qualify_AssetClass_Commodity.java",
                "the `<Commodity>` choice-super-option witness reverts to bare");
    }

    /** numLit: `valuationAmount < 0` over the `ShortFraction5DecimalNumber` (number alias) sibling → BigDecimal literal. */
    @Test
    @EnabledIf("drrAvailable")
    void drrCreateCounterpartySpecificData36_numberAliasLiteral_byteMatchesGolden() throws IOException {
        assertByteMatches(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/asic/rewrite/trade/functions/Create_CounterpartySpecificData36__3.java",
                "the `MapperS.of(BigDecimal.valueOf(0))` reverts to the bare `MapperS.of(0)` — "
                + "kindFromTypeCall no longer resolves the ShortFraction5DecimalNumber alias to BigDecimal");
    }

    /** numLit sibling carrier in a different drr namespace/shape (notional-quantity comparison). */
    @Test
    @EnabledIf("drrAvailable")
    void drrGetNtnlQty_numberAliasLiteral_byteMatchesGolden() throws IOException {
        assertByteMatches(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/jfsa/rewrite/trade/functions/GetNtnlQty.java",
                "the number-alias-sibling int literal reverts to the bare (Integer) form");
    }

    private static void assertByteMatches(Map<String, String> output, Path goldenDir, String path, String revertHint)
            throws IOException {
        assertNotNull(output, "Function generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
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
