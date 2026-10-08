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
 * Facet {@code checkedMapEnumSource} (PR #224) — the residual of the #159
 * {@code checkedmap_enum_source} mechanism: a conversion ({@code to-enum} /
 * {@code to-string}) whose source is an ENUM must re-map by Java constant name
 * ({@code checkedMap("to-enum", e -> Target.valueOf(e.name()), …)} /
 * {@code map("to-string", SourceEnum::toDisplayString)}) like the upstream 9.83.0
 * golden — not parse the display name ({@code Target::fromDisplayName}) / call the
 * generic {@code Object::toString} (the string-source forms). #159 routed the
 * source-type test through {@code NavigationHandler.leafEnumeration} (the chained
 * {@code RFeatureCall} / input-rooted disguised {@code REnumValueRef} receiver
 * walk), which left TWO enum-source shapes falling to the string form:
 *
 * <ol>
 *   <li><b>alias/function-rooted disguised {@link
 *       com.regnosys.rosetta.ast.expressions.references.REnumValueRef}</b>
 *       ({@code optionSettlementModel -> settlementType} where the alias body wraps
 *       {@code fpmlOptionSettlementModel(...)}) — {@code leafEnumeration}'s
 *       {@code REnumValueRef} arm uses the bare 1-arg disguise resolver that does not
 *       walk an alias/function root. {@code ConversionHandler.conversionLeafEnumeration}
 *       resolves the SAME leaf the meta-unwrap arm C5 gate + the rendered
 *       {@code <Type>} witness use (gm-aware {@code resolveDisguisedFeature} + the
 *       parameterless list-op collapse).</li>
 *   <li><b>bare alias / function-call {@link
 *       com.regnosys.rosetta.ast.expressions.references.RSymbolReference} source</b>
 *       ({@code lvl(drrReport) to-enum ModificationLevel1Code} — {@code lvl} a
 *       function/alias whose result is the source enum), recovered by
 *       {@code NavigationHandler.siblingComparandEnumeration} (the alias body / the
 *       callee OUTPUT enum, the same resolver the bare-enum comparison operand uses).</li>
 * </ol>
 *
 * <p><b>Green-safe by construction:</b> both fallbacks fire ONLY when they resolve a
 * concrete enumeration; a genuine STRING source (the 68 green {@code ::fromDisplayName}
 * / 55 green {@code Object::toString} carriers) resolves to a basic/alias leaf → no
 * enum → the string form is preserved. The PRE/POST regression scan over all
 * still-waivered files is 0 regressions / 0 churn, and the full 5-cell D11 matrix
 * (20/20) is the empirical arbiter. The facet pays 5 FUNCTION flips
 * (cdm6 ×3 + drr ×2), covering both conversion seats and both new routes.
 *
 * <p>The IMPLICIT-ITEM-in-lambda enum source ({@code periodicPayment ->
 * fixedRateDayCountConvention} rooted at an {@code extract} element type — the drr
 * {@code GetIntrstRate} family) is DEFERRED: its disguised root must be rooted against
 * the enclosing lambda's element type, a broader resolution that the conversion-scoped
 * fallbacks here do not reach.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}, mirroring the 5 flips 1:1.
 */
class FunctionCheckedMapEnumSourceFallbackTest {

    private static final Path CDM_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM_GOLDEN_DIR =
            CDM_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdmFunctionOutput;
    private static Map<String, String> drrFunctionOutput;

    static boolean cdmCellAvailable() {
        return Files.isDirectory(CDM_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM_GOLDEN_DIR);
    }

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
        if (cdmCellAvailable()) {
            cdmFunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM_CELL_ROOT));
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

    // ----- route 1: conversionLeafEnumeration (alias/function-rooted disguised REnumValueRef) -----

    /**
     * MapCorrelationLegToSettlementTerms (cdm6) — a {@code to-enum} whose source
     * {@code optionSettlementModel -> settlementType} (alias body wraps
     * {@code fpmlOptionSettlementModel(...)}) re-maps via
     * {@code e -> cdm…SettlementTypeEnum.valueOf(e.name())}.
     */
    @Test
    @EnabledIf("cdmCellAvailable")
    void mapCorrelationLegToSettlementTerms_toEnumDisguisedSource_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdmFunctionOutput, CDM_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/settlement/functions/MapCorrelationLegToSettlementTerms.java");
    }

    /**
     * MapCommodityCalculationPeriodFrequency (cdm6) — a {@code to-string} whose source
     * {@code calculationPeriod(...) -> period} re-maps via
     * {@code PeriodExtendedEnum::toDisplayString} (not {@code Object::toString}).
     */
    @Test
    @EnabledIf("cdmCellAvailable")
    void mapCommodityCalculationPeriodFrequency_toStringDisguisedSource_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdmFunctionOutput, CDM_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/datetime/functions/MapCommodityCalculationPeriodFrequency.java");
    }

    /**
     * MapCommodityRelativePaymentDates (cdm6) — a {@code to-string} whose source
     * {@code relativePaymentDates(...) -> payRelativeTo} re-maps via
     * {@code CommodityPayRelativeToEnum::toDisplayString}.
     */
    @Test
    @EnabledIf("cdmCellAvailable")
    void mapCommodityRelativePaymentDates_toStringDisguisedSource_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdmFunctionOutput, CDM_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/datetime/functions/MapCommodityRelativePaymentDates.java");
    }

    // ----- route 2: siblingComparandEnumeration (bare alias/function-call RSymbolReference) -----

    /**
     * Create_TradeReport32Choice__1 (drr esma) — a {@code to-enum} whose source
     * {@code lvl(drrReport)} (a function/alias returning the source enum) re-maps via
     * {@code e -> ModificationLevel1Code.valueOf(e.name())}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void createTradeReport32ChoiceEsma_toEnumBareFunctionSource_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/esma/emir/refit/trade/functions/Create_TradeReport32Choice__1.java");
    }

    /** Create_TradeReport32Choice__1 (drr fca/ukemir) — the fca twin of the bare-function-source carrier. */
    @Test
    @EnabledIf("drrCellAvailable")
    void createTradeReport32ChoiceFca_toEnumBareFunctionSource_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/fca/ukemir/refit/trade/functions/Create_TradeReport32Choice__1.java");
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
                + path + " — an alias/function-rooted enum conversion source falls to the "
                + "string-source Target::fromDisplayName / Object::toString form if the "
                + "checkedMapEnumSource conversionLeafEnumeration / siblingComparandEnumeration "
                + "fallback is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
