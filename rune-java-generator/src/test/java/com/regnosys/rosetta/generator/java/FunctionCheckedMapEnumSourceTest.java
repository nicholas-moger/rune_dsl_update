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
 * Facet {@code checkedmap_enum_source} — a RENDERER-ONLY source-type fix in
 * {@code ConversionHandler}: a {@code to-enum} conversion whose source is an
 * ENUM-typed navigation ({@code drrReport -> eventType to-enum DerivativeEventType3Code__1})
 * must re-map by Java constant name like the upstream 9.83.0 golden —
 * {@code checkedMap("to-enum", e -> Target.valueOf(e.name()), …)} — not parse a
 * display name ({@code Target::fromDisplayName}, the string-source form). The fork's
 * {@code isEnumSource} read ONLY the workspace inferred-type map, which is MISSING for
 * (almost) every function-body navigation source — a single-step nav parses as a
 * disguised {@link com.regnosys.rosetta.ast.expressions.references.REnumValueRef}
 * (the grammar's {@code EnumName -> valueName} ambiguity) and a chained nav as an
 * {@link com.regnosys.rosetta.ast.expressions.references.RFeatureCall}, neither of
 * which the inference engine types on this path — so every navigation source fell to
 * the string form. The fix routes the source-type test through the SAME best-effort
 * receiver-chain resolution the {@code <Type>} witness on the very same line already
 * uses ({@code NavigationHandler.leafEnumeration}, PR #154), and registers the target
 * enum's import ref ({@code REnumTypeRef} → JavaClass, dedup'd downstream per PR #130)
 * — the {@code fromDisplayName} emission referenced the target enum's SIMPLE name
 * without ever importing it, so a file whose target enum had no ambient import did not
 * even compile.
 *
 * <p>This test is REVERT-VERIFIED RED: reverting the {@code leafEnumeration} fallback
 * (or the target-enum ref) reverts every anchor to the {@code fromDisplayName} /
 * missing-import shape. Anchors are WHOLE-FILE byte comparisons against the frozen
 * goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}, chosen from the 36
 * sole-mechanism waivered files (all drr/6.34.1) to cover the facet's sub-shapes:
 * <ul>
 *   <li>hkma dtcc {@code GetDerivEvtTp} — single-step disguised-REnumValueRef source
 *       ({@code drrReport -> eventType}); conversion-line fix only (the target enum is
 *       the function output type, so its import is already ambient);</li>
 *   <li>hkma dtcc {@code GetDerivEvt4} — single-step source whose target enum appears
 *       ONLY inside the conversion: exercises the import half;</li>
 *   <li>jfsa {@code Create_TradeCounterpartyReport20__1} — three conversion sites over
 *       two target enums, including chained {@code RFeatureCall} sources
 *       ({@code drrReport -> leg1 -> direction2}) + two conversion-only imports;</li>
 *   <li>mas {@code GetNtnlQty} — chained source under the conditional SET path, the
 *       same target enum converted in both branches, one conversion-only import.</li>
 * </ul>
 */
class FunctionCheckedMapEnumSourceTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM5_DEP_ROSETTA_DIR =
            Path.of("../test-corpus/cdm/cdm-5.38.0/rosetta-source/src/main/rosetta");
    private static final Path ISO_DEP_ROSETTA_DIR =
            Path.of("../test-corpus/iso20022/iso20022-1.38.0/rosetta-source/src/main/rosetta");

    private static Map<String, String> drrFunctionOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR)
                && Files.isDirectory(CDM5_DEP_ROSETTA_DIR)
                && Files.isDirectory(ISO_DEP_ROSETTA_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
        if (drrCellAvailable()) {
            var cell = new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT);
            var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            var typeUtil = new JavaTypeUtil();
            var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
            Map<String, String> output = new LinkedHashMap<>();
            List<GenerationException> genErrors = gen.generateWithErrors(output);
            assertTrue(genErrors.isEmpty(),
                    cell + " FUNCTION generation reported errors: " + genErrors);
            drrFunctionOutput = output;
        }
    }

    /** Single-step disguised-REnumValueRef source; conversion-line fix only. */
    @Test
    @EnabledIf("drrCellAvailable")
    void hkmaDtccGetDerivEvtTp_singleStepNavSource_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/projection/iso20022/hkma/rewrite/trade/dtcc/functions/GetDerivEvtTp.java");
    }

    /** Single-step source whose target enum appears ONLY in the conversion (import half). */
    @Test
    @EnabledIf("drrCellAvailable")
    void hkmaDtccGetDerivEvt4_conversionOnlyImport_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/projection/iso20022/hkma/rewrite/trade/dtcc/functions/GetDerivEvt4.java");
    }

    /** Three conversion sites / two target enums, incl. chained RFeatureCall sources. */
    @Test
    @EnabledIf("drrCellAvailable")
    void jfsaCreateTradeCounterpartyReport_chainedNavSources_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/projection/iso20022/jfsa/rewrite/trade/functions/Create_TradeCounterpartyReport20__1.java");
    }

    /** Chained source under the conditional SET path; both branches convert the same enum. */
    @Test
    @EnabledIf("drrCellAvailable")
    void masGetNtnlQty_conditionalSetPath_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/projection/iso20022/mas/rewrite/trade/functions/GetNtnlQty.java");
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(drrFunctionOutput, "Function generation did not run — corpus unavailable?");
        String generated = drrFunctionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " — an enum-typed navigation source falls to the string-source "
                + "Target::fromDisplayName form (and drops the target enum's import) if "
                + "the ConversionHandler leafEnumeration fallback is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
