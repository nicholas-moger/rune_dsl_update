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
 * PR #204 — TWO disjoint FUNCTION-codegen mechanisms (8 flips), whole-file byte anchors through the
 * REAL D11 loader (transitive rune-fpml dep per PR #184). Each anchor reverts RED if its mechanism is
 * removed; both are green-safe by construction (each replaces a non-compiling, already-waivered form —
 * stash-baseline confirmed 0 now-matching pristine).
 *
 * <ul>
 *   <li><b>thenArgTypeFromCompiled</b> (7) — {@code FunctionExpressionRenderer.renderThenExtractSet}'s
 *       hoisted {@code final Mapper*<itemType> thenArg} reads {@code gm.workspace().getInferredType}
 *       (the parser snapshot), which erases the element type to {@code Object} for many then-chains.
 *       The new {@code thenArgItemRType} fallback recovers the concrete element type by unwrapping the
 *       {@code then}/{@code filter}/{@code extract}/list-op wrappers + the gm-aware
 *       {@code NavigationHandler.resolveReceiverRType} (plus the implicit-item attribute fallback for a
 *       mis-bound/empty leaf symbol). The corrected type threads through {@code prevRef}, re-rooting the
 *       next then-body's implicit-item navigation; the {@code resolveReceiverDataType} filter/extract/then
 *       arms + the {@code synthesizeImplicitItemBareNav} gate widening (the linker mis-binds an item
 *       feature to a same-named {@code RAnnotation}/{@code RRecordType}/{@code REnumeration}) complete
 *       the downstream cascade. Anchors: cdm6 {@code GetPartyPersonForRelatedPerson} (single-level
 *       declaration, {@code MapperC<Object>} -> {@code MapperC<Person>}), cdm6
 *       {@code MapFxOptionFeaturesToObservationTerms} ({@code MapperS} form), drr
 *       {@code GetExecutionTimestamp} (the FULL cascade — {@code thenArg0} EventTimestamp + filter-body
 *       {@code qualification} re-root + {@code thenArg1} ZonedDateTime + extract-body {@code dateTime}
 *       re-root), drr {@code CommodityFixedPriceQuantity}.</li>
 *   <li><b>returnCtorWrap</b> (1) — {@code FunctionExpressionRenderer.appendReturnLadder} wraps a
 *       constructor-expression rung value in {@code MapperS.of(...)} when the alias's declared return
 *       type is a single {@code MapperS<...>} (upstream {@code caseConstructorExpr} coerced to the
 *       MAPPER_S expected type — the return-seat sibling of #180's map-lambda-body ctor wrap and #183's
 *       return ladder). Anchor: cdm6 {@code MapCurrencyAmountToQuantity}.</li>
 * </ul>
 */
class FunctionPr204BundleTest {

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

    /** thenArgTypeFromCompiled — single-level then-arg declaration: MapperC<Object> -> MapperC<Person>. */
    @Test
    @EnabledIf("cellsAvailable")
    void getPartyPersonForRelatedPerson_cdm6_thenArgType_byteMatchesGolden() throws IOException {
        assertCdm6("cdm/ingest/fpml/confirmation/party/functions/GetPartyPersonForRelatedPerson.java",
                "the thenArg element type recovers to the concrete chain type (thenArgItemRType) and "
                + "reverts to the non-compiling `MapperC<Object> thenArg`");
    }

    /** thenArgTypeFromCompiled — the MapperS single-cardinality then-arg form. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapFxOptionFeatures_cdm6_thenArgType_byteMatchesGolden() throws IOException {
        assertCdm6("cdm/ingest/fpml/confirmation/product/fxoption/functions/MapFxOptionFeaturesToObservationTerms.java",
                "the MapperS<Object> thenArg recovers the concrete element type");
    }

    /** returnCtorWrap — an alias return-ladder rung wraps a bare ctor in MapperS.of(...). */
    @Test
    @EnabledIf("cellsAvailable")
    void mapCurrencyAmountToQuantity_cdm6_returnCtorWrap_byteMatchesGolden() throws IOException {
        assertCdm6("cdm/ingest/fpml/confirmation/pricequantity/functions/MapCurrencyAmountToQuantity.java",
                "the `return MapperS.of(<Ctor>.builder()…build())` wrap (appendReturnLadder / "
                + "wrapCtorRungInMapperSOf) reverts to the bare `return <Ctor>.builder()…build()`");
    }

    /**
     * thenArgTypeFromCompiled — the FULL cascade: thenArg0 (EventTimestamp) + the filter-body
     * {@code qualification} implicit-item re-root + thenArg1 (ZonedDateTime) + the extract-body
     * {@code dateTime} re-root. Reverts RED on ANY of the four sub-fixes.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void getExecutionTimestamp_drr_thenArgCascade_byteMatchesGolden() throws IOException {
        assertDrr("drr/regulation/common/functions/GetExecutionTimestamp.java",
                "the then-chain Object-type cascade (thenArg declaration + filter/extract body item "
                + "re-root) reverts to `MapperC<Object>` + the bare `MapperS.of(qualification)`/"
                + "`MapperS.of(dateTime)` non-navigations");
    }

    /** thenArgTypeFromCompiled — a second drr cascade carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void commodityFixedPriceQuantity_drr_thenArgType_byteMatchesGolden() throws IOException {
        assertDrr("drr/regulation/common/functions/CommodityFixedPriceQuantity.java",
                "the thenArg element type recovers to the concrete chain type");
    }

    private static void assertCdm6(String path, String revertHint) throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR, path, revertHint);
    }

    private static void assertDrr(String path, String revertHint) throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR, path, revertHint);
    }

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String path, String revertHint) throws IOException {
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
