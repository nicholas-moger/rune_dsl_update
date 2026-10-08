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
 * PR #215 facet {@code checkedMapTargetSimpleName} — the SIMPLE-NAME sibling of PR #196's
 * {@code checkedMapFqn} ({@link FunctionCheckedMapFqnTest}). A {@code to-enum} conversion whose
 * rosetta-written TARGET reference is QUALIFIED (e.g.
 * {@code schemeName to-enum iso20022.auth030.hkma.dtcc.HKTRPartyScheme}) leaked the fully-qualified
 * name inline ({@code iso20022.auth030.hkma.dtcc.HKTRPartyScheme::fromDisplayName}) because the
 * off-collision render arm echoed {@code RConversionExpr.targetEnumName()} verbatim. Upstream emits
 * the SIMPLE name + the (already-collected) import ({@code HKTRPartyScheme::fromDisplayName}).
 *
 * <p>The fix ({@code ConversionHandler.targetEnumSimpleName}) renders the off-collision target via the
 * translator's Java simple name instead of the rosetta reference; the PR #196 collision arm
 * ({@code enumTargetCollisionFqn} — an ENUM source whose simple name collides with the target) is
 * UNCHANGED, so a genuinely-colliding target keeps its FQN-inline form. Green-safe: no golden carries
 * the FQN-inline {@code pkg.Enum::fromDisplayName} form (every off-collision target is simple-named).
 *
 * <p>Whole-file byte anchors through the REAL D11 loader (which loads the transitive rune-fpml dep per
 * PR #184), spanning both cells:
 * <ul>
 *   <li>{@code MapSwaptionPayout} (cdm6 ingest-fpml) — a STRING-source {@code to-enum} with a
 *       qualified target written in the rosetta.</li>
 *   <li>the 4 drr hkma VALUATION {@code Create_OrganisationIdentification15Choice} variants —
 *       {@code schemeName to-enum iso20022…HKTRPartyScheme} on a {@code String schemeName} (no
 *       collision; the TRADE variants flipped via {@link FunctionEnumQualifyComparandTest}).</li>
 * </ul>
 * REVERT-VERIFIED RED: reverting {@code targetEnumSimpleName} reverts these anchors to the
 * fully-qualified inline enum reference.
 */
class FunctionCheckedMapTargetSimpleNameTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR = CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR = DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;
    private static Map<String, String> drrFunctionOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR)
                && Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            cdm6FunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
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

    /** cdm6 — a STRING-source to-enum with a rosetta-qualified target. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapSwaptionPayout_cdm6_qualifiedTargetRendersSimple_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/swaption/functions/MapSwaptionPayout.java");
    }

    /** valuation/dtcc __2 — String schemeName to-enum iso20022…HKTRPartyScheme (no collision). */
    @Test
    @EnabledIf("cellsAvailable")
    void organisationIdentification15Choice_valuationDtcc2_qualifiedTargetRendersSimple_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/hkma/rewrite/valuation/dtcc/functions/Create_OrganisationIdentification15Choice__2.java");
    }

    /** valuation/dtcc __3 — a second valuation dtcc variant. */
    @Test
    @EnabledIf("cellsAvailable")
    void organisationIdentification15Choice_valuationDtcc3_qualifiedTargetRendersSimple_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/hkma/rewrite/valuation/dtcc/functions/Create_OrganisationIdentification15Choice__3.java");
    }

    /** valuation/tr __2 — a different projection target proving the law is package-independent. */
    @Test
    @EnabledIf("cellsAvailable")
    void organisationIdentification15Choice_valuationTr2_qualifiedTargetRendersSimple_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/hkma/rewrite/valuation/tr/functions/Create_OrganisationIdentification15Choice__2.java");
    }

    /** valuation/tr __3 — the 4th and final drr valuation checkedMap carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void organisationIdentification15Choice_valuationTr3_qualifiedTargetRendersSimple_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/hkma/rewrite/valuation/tr/functions/Create_OrganisationIdentification15Choice__3.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir, String path)
            throws IOException {
        assertNotNull(output, "Function generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — an off-collision to-enum TARGET must render by its Java simple name + import "
                + "(HKTRPartyScheme::fromDisplayName), not the rosetta-written FQN; reverting "
                + "ConversionHandler.targetEnumSimpleName reverts this anchor to the fully-qualified "
                + "inline enum reference (RED).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
