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
 * Facet {@code tostring_enum_source} — a RENDERER-ONLY fix in
 * {@code ConversionHandler.handle(RToStringExpr)}, the {@code to-string} sibling of the
 * PR #159 {@code to-enum} enum-source facet: upstream 9.83.0
 * ({@code ExpressionGenerator.caseToStringOperation}) labels the {@code .map()} with the
 * OPERATOR KEYWORD and selects the mapping function from the SOURCE type —
 * {@code .map("to-string", SourceEnum::toDisplayString)} for an enum source,
 * {@code .map("to-string", Object::toString)} otherwise. The fork emitted the camelCase
 * label with the generic function for EVERY source
 * ({@code .map("toString", Object::toString)}) — a shape that appears in ZERO goldens
 * across all 5 cells (the goldens carry 1,000+ {@code "to-string"} labels, 0
 * {@code "toString"}), so no green file can carry the pre-fix emission and the whole
 * facet is green-file-safe by corpus law. The source enum resolves through the SAME
 * routes the {@code to-enum} facet pinned: the workspace inferred-type map for a
 * directly-typed source, {@code NavigationHandler.leafEnumeration} for a navigation
 * source, PLUS the implicit-item route ({@code NavigationHandler.implicitItemEnumeration},
 * the enum analogue of the PR #161 {@code implicitItemDataType} walk) for a bare
 * {@code item} source inside a filter/extract lambda. The resolved enum's import ref
 * registers exactly as the {@code to-enum} arm's target ref does ({@code REnumTypeRef}
 * → JavaClass, dedup'd downstream).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}, chosen from the sole-mechanism
 * waivered files (all drr/6.34.1) to cover the facet's source shapes:
 * <ul>
 *   <li>jfsa {@code Create_FixedRateLeg1} — one chained-{@code RFeatureCall} enum source
 *       ({@code drrReport -> leg1 -> periodicPayment -> fixedRateDayCountConvention},
 *       leaf {@code InterestComputationMethod4Code}) beside an already-passing
 *       {@code to-enum} site on the same statement;</li>
 *   <li>jfsa {@code Create_FloatingRateLeg1} — SIX to-string sites over three distinct
 *       source enums ({@code FrequencyPeriodEnum} ×3, {@code ISOCurrencyCodeEnum},
 *       {@code InterestComputationMethod4Code}) in one evaluate-call argument list;</li>
 *   <li>regulation-common {@code GetFinancialCorporateSector} — the implicit-item route:
 *       {@code financialSector extract [item to-string]} renders
 *       {@code .mapItem(item -> item.map("to-string", FinancialSectorEnum::toDisplayString))},
 *       the bare {@code item} typed from the enclosing extract's argument.</li>
 * </ul>
 */
class FunctionToStringEnumSourceTest {

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

    /** Chained-RFeatureCall enum source beside an already-passing to-enum site. */
    @Test
    @EnabledIf("drrCellAvailable")
    void jfsaCreateFixedRateLeg1_chainedNavSource_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/projection/iso20022/jfsa/rewrite/trade/functions/Create_FixedRateLeg1.java");
    }

    /** Six to-string sites over three distinct source enums in one argument list. */
    @Test
    @EnabledIf("drrCellAvailable")
    void jfsaCreateFloatingRateLeg1_sixSitesThreeEnums_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/projection/iso20022/jfsa/rewrite/trade/functions/Create_FloatingRateLeg1.java");
    }

    /** Implicit-item source: bare {@code item} inside a mapItem extract lambda. */
    @Test
    @EnabledIf("drrCellAvailable")
    void getFinancialCorporateSector_implicitItemSource_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/functions/GetFinancialCorporateSector.java");
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
                + path + " — a to-string site renders the camelCase generic form "
                + ".map(\"toString\", Object::toString) instead of golden's "
                + ".map(\"to-string\", SourceEnum::toDisplayString) if the "
                + "ConversionHandler source-enum resolution is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
