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
 * PR #220 — aliasReturnCoerce + ofNullSafe. The PLAIN alias-method return seat
 * ({@code FunctionGenerator.compileAliases}, after the {@code returnIte} ladder /
 * lifted-call / then-hoist arms decline) coerces a bare body to its declared
 * {@code MapperS<…>} signature — the uncovered-seat sibling of PR #180
 * (map-lambda-body ctor wrap) / #183 (if-then-else return ladder) / #218&ndash;#219
 * (then-chain {@code asMapper}). Three coercion sub-forms + one operand sub-form:
 *
 * <ol>
 *   <li><b>ctor wrap</b> — a bare {@code RConstructorExpr} body in a {@code MapperS<X>}
 *       alias &rarr; {@code return MapperS.of(<ctor>);} (the builder-chain continuation
 *       re-indents one level deeper). {@code FunctionExpressionRenderer.renderAliasReturnCoerceOrNull}.</li>
 *   <li><b>plain asMapper</b> — a {@code ComparisonResult} body in a {@code MapperS<Boolean>}
 *       alias &rarr; {@code return <body>.asMapper();}. Same method.</li>
 *   <li><b>ladder-rung asMapper</b> — a {@code ComparisonResult} rung of a conditional
 *       (return-ladder) alias body &rarr; {@code .asMapper()} per rung
 *       ({@code FunctionExpressionRenderer.wrapComparisonResultRungAsMapper}, applied beside
 *       {@code wrapCtorRungInMapperSOf} in {@code appendReturnLadder}).</li>
 *   <li><b>ofNullSafe operand</b> — a {@code MapperS<Boolean>} alias ({@code RShortcut})
 *       operand of {@code and}/{@code orNullSafe} &rarr;
 *       {@code ComparisonResult.ofNullSafe(<alias(...)>)}
 *       ({@code LogicalHandler.wrapBooleanFunctionOperand}, the {@code RShortcut} arm).</li>
 * </ol>
 *
 * <p><b>Green-safe by construction:</b> every bare form is a COMPILE ERROR in a
 * {@code MapperS<…>}-declared method ({@code X.builder()} is not a {@code MapperS}; a
 * {@code ComparisonResult} is not a {@code MapperS} subtype; {@code MapperS<Boolean>.andNullSafe}
 * does not exist), so each carrier was already a waivered mismatch — the coercion can only turn
 * a waivered red into golden-identical, never green&rarr;red. Whole-file byte anchors through the
 * REAL D11 loader over all 15 clean carriers (cdm5 ×3, cdm6 ×11, drr ×1). REVERT-VERIFIED RED.
 */
class FunctionAliasReturnCoerceTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path CDM5_GOLDEN_DIR = CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_GOLDEN_DIR = CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_GOLDEN_DIR = DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm5FunctionOutput;
    private static Map<String, String> cdm6FunctionOutput;
    private static Map<String, String> drrFunctionOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR)
                && Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR)
                && Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            cdm5FunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
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

    // ---- ctor wrap (bare RConstructorExpr body → return MapperS.of(<ctor>);) ----

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_mapMasterAgreement_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/legal/functions/MapMasterAgreement.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_mapMasterConfirmation_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/legal/functions/MapMasterConfirmation.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_mapInterestRatePriceSchedule_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/pricequantity/functions/MapInterestRatePriceSchedule.java");
    }

    /** Two ctor-wrap alias returns in one file. */
    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_mapBondOptionStrikeToOptionStrike_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/bondoption/functions/MapBondOptionStrikeToOptionStrike.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_mapCommodityOptionPayout_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/commodityoption/functions/MapCommodityOptionPayout.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_mapReferencePrice_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/creditdefaultswap/functions/MapReferencePrice.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_mapEquityOptionTransactionSupplementPayout_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/equityoptiontransactionsupplement/functions/MapEquityOptionTransactionSupplementPayout.java");
    }

    // ---- plain asMapper (ComparisonResult body in a MapperS<Boolean> alias → .asMapper()) ----

    @Test
    @EnabledIf("cellsAvailable")
    void cdm5_determineResetDate_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/product/asset/floatingrate/functions/DetermineResetDate.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_determineResetDate_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/product/asset/floatingrate/functions/DetermineResetDate.java");
    }

    /** Three plain-asMapper alias bodies (notExists().andNullSafe().orNullSafe()). */
    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_validateFloatingRateIndexName_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/observable/asset/fro/functions/ValidateFloatingRateIndexName.java");
    }

    // ---- ladder-rung asMapper (ComparisonResult rung of a conditional alias body) ----

    /** floatingRateIndexChanged (plain asMapper) + adjustmentSpreadAdded (ladder then-rung asMapper). */
    @Test
    @EnabledIf("cellsAvailable")
    void cdm5_qualifyIndexTransition_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/qualification/functions/Qualify_IndexTransition.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_qualifyIndexTransition_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/qualification/functions/Qualify_IndexTransition.java");
    }

    // ---- ofNullSafe operand (MapperS<Boolean> alias operand of and/orNullSafe) ----
    // PriceUnitEquals + IsValidRefEntity each carry BOTH the plain/ladder asMapper alias bodies
    // AND the ComparisonResult.ofNullSafe operand wrap on the result= chain — they flip only when
    // both the aliasReturnCoerce (asMapper) and the ofNullSafe (LogicalHandler) sub-forms land.

    @Test
    @EnabledIf("cellsAvailable")
    void cdm5_priceUnitEquals_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/product/common/settlement/functions/PriceUnitEquals.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_priceUnitEquals_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/product/common/settlement/functions/PriceUnitEquals.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void drr_isValidRefEntity_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/IsValidRefEntity.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> cellOutput, Path goldenDir, String path)
            throws IOException {
        assertNotNull(cellOutput, "Function generation did not run — corpus unavailable?");
        String generated = cellOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — a plain alias-method return must coerce to its declared MapperS<…> signature "
                + "(bare ctor → MapperS.of(…); ComparisonResult → .asMapper(); MapperS<Boolean> alias "
                + "operand of and/orNullSafe → ComparisonResult.ofNullSafe(…)). Reverting the "
                + "aliasReturnCoerce / ofNullSafe arms reverts this anchor to the bare non-compiling "
                + "form (RED).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
