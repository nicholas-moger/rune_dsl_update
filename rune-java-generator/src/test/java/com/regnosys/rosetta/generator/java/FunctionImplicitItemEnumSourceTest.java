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
 * Facet {@code implicitItemEnumSource} (PR #225) — the implicit-item-in-lambda
 * extension of PR #224's {@code checkedMapEnumSource}. A conversion ({@code to-enum}
 * / {@code to-string}) whose SOURCE rides the enclosing {@code extract}/map lambda's
 * implicit ITEM must re-map by Java constant name like the upstream 9.83.0 golden
 * ({@code checkedMap("to-enum", e -> Target.valueOf(e.name()), …)} /
 * {@code map("to-string", SourceEnum::toDisplayString)}) — not parse the display name
 * ({@code Target::fromDisplayName}) / call the generic {@code Object::toString} (the
 * string-source forms #224 deferred for this shape).
 *
 * <p>Inside {@code drrReport -> leg1 extract InterestRate33Choice {… periodicPayment
 * -> fixedRateDayCountConvention to-enum …, spreadCurrency to-string …}}, the source
 * is rooted at the implicit item ({@code CommonLeg}). {@code #224}'s
 * {@code ConversionHandler.conversionLeafEnumeration} resolved a disguised
 * {@code REnumValueRef} only against the function's inputs/outputs/aliases, so an
 * item-rooted source fell to the string form. PR #225 adds two conversion-scoped
 * helpers, both wired into {@code ConversionHandler.conversionLeafAttribute}:
 *
 * <ol>
 *   <li><b>2-name disguised {@code REnumValueRef}</b> ({@code periodicPayment ->
 *       fixedRateDayCountConvention}) — {@code NavigationHandler.implicitItemDisguisedLeaf}
 *       roots {@code enumName} on the lambda element type (the head→leaf walk
 *       {@code implicitItemDataType} + {@code findAttributeOnDataType} +
 *       {@code attributeToDataType}, the same as {@code synthesizeImplicitItemChain}).</li>
 *   <li><b>1-name bare {@code RSymbolReference}</b> ({@code spreadCurrency} /
 *       {@code period}) — {@code NavigationHandler.implicitItemSymbolLeaf} resolves the
 *       single feature directly on the lambda element type.</li>
 * </ol>
 *
 * <p><b>Green-safe by construction:</b> both helpers return only a resolved leaf
 * attribute; the enum-ness test stays at {@code conversionLeafEnumeration} (filters
 * {@link com.regnosys.rosetta.ast.types.REnumeration}), so a STRING source resolves to
 * a basic/alias leaf → the generic form is preserved, and a context with no enclosing
 * lambda yields a null item type → the helper declines. The PRE/POST regression scan
 * over all still-waivered files is 0 regressions; the full 5-cell D11 matrix (20/20)
 * is the empirical arbiter. The facet pays 3 FUNCTION flips (cdm6 ×1 + drr ×2).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}, mirroring the 3 flips 1:1 —
 * reverting either helper restores the {@code ::fromDisplayName} / {@code Object::toString}
 * form and turns these RED.
 */
class FunctionImplicitItemEnumSourceTest {

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

    /**
     * MapFraToFixedInterestRatePayout (cdm6) — a {@code period to-string} bare item
     * feature inside {@code fpmlFra -> indexTenor first extract Frequency {…}} re-maps
     * via {@code PeriodEnum::toDisplayString} (the 1-name {@code implicitItemSymbolLeaf}
     * route).
     */
    @Test
    @EnabledIf("cdmCellAvailable")
    void mapFraToFixedInterestRatePayout_implicitItemBareToString_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdmFunctionOutput, CDM_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/fra/functions/MapFraToFixedInterestRatePayout.java");
    }

    /**
     * GetIntrstRate (drr esma) — inside {@code drrReport -> leg1 extract
     * InterestRate33Choice {…}}, the {@code periodicPayment -> fixedRateDayCountConvention
     * to-enum} (2-name {@code implicitItemDisguisedLeaf}) + {@code spreadCurrency to-string}
     * (1-name {@code implicitItemSymbolLeaf}) sources all re-map by Java constant name.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getIntrstRateEsma_implicitItemEnumSources_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/esma/emir/refit/trade/functions/GetIntrstRate.java");
    }

    /** GetIntrstRate (drr fca/ukemir) — the fca twin of the implicit-item enum-source carrier. */
    @Test
    @EnabledIf("drrCellAvailable")
    void getIntrstRateFca_implicitItemEnumSources_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/fca/ukemir/refit/trade/functions/GetIntrstRate.java");
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
                + path + " — an implicit-item-rooted enum conversion source falls to the "
                + "string-source Target::fromDisplayName / Object::toString form if the "
                + "implicitItemDisguisedLeaf / implicitItemSymbolLeaf fallback is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
