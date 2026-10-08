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
 * PR #196 (facet {@code checkedMapFqn} — a {@code to-enum} conversion's TARGET enum is rendered
 * fully-qualified inline, with its import suppressed, when its Java simple name collides with the
 * conversion's SOURCE enum but a DIFFERENT canonical name).
 *
 * <p>The law (upstream {@code ImportingStringConcatenation.internalDoImportIfPossible} —
 * first-claim-wins, the same law as PR #195's {@code fpmlInputFqn}): when a generated file
 * references two distinct types sharing a simple name, the FIRST claimant is imported + rendered
 * simple; a later collider with a different canonical name renders FQN-inline with NO import. In a
 * {@code to-enum} re-map {@code e -> Target.valueOf(e.name())}, the SOURCE enum (the navigation
 * {@code <Type>} witness or the input-parameter type, e.g. {@code fpml.consolidated.fpmlenum.PeriodEnum})
 * claims the bare {@code PeriodEnum} first; the TARGET enum ({@code cdm.base.datetime.PeriodEnum})
 * collides and renders {@code e -> cdm.base.datetime.PeriodEnum.valueOf(e.name())} with no
 * {@code import cdm.base.datetime.PeriodEnum;}.
 *
 * <p>The fork rendered the target by its SIMPLE name AND imported it, so a file already importing
 * the same-simple source enum emitted a DUPLICATE same-simple-name import = a Java compile error —
 * every carrier was already a waivered, non-compiling mismatch (green-safe by construction). The fix
 * (ConversionHandler.enumTargetCollisionFqn) resolves the source enum through the SAME route the
 * to-string source uses (sourceEnumeration) and, on a same-simple/different-FQN collision, renders
 * the target FQN-inline + suppresses its import. The MANDATORY FQN-difference gate (the sibling of
 * PR #195's resolveInputParamForOutputCollision) keeps a same-canonical-name self-conversion bare.
 *
 * <p>Whole-file byte anchors through the REAL D11 loader (which loads the transitive rune-fpml dep
 * per PR #184). Carriers span cdm6 ingest-fpml AND drr projection. Note MapDateOffsetToRelativeDateOffset
 * is a MIXED carrier — it ALSO carries the PR #195 fpmlInputFqn input-param collision (now merged to
 * main), so it fully flips only with BOTH halves of the first-claim-wins law in place (the joint
 * unlock). NO green to-enum FUNCTION carrier exists in the compared cdm6/drr population (every
 * to-enum collides), so the over-FIRE guard (the gate must not FQN a non-colliding target) is the
 * full all-kinds D11 20/20 (zero collateral) + the byte-oracle's exact-33, not a separate anchor.
 *
 * <p>Anchored:
 * <ul>
 *   <li>{@code MapValuationMethod} (cdm6) — a PURE single-enum collision (input-param-typed source
 *       {@code fpml…QuotationRateTypeEnum} vs target {@code cdm…QuotationRateTypeEnum}).</li>
 *   <li>{@code MapDateOffsetToRelativeDateOffset} (cdm6) — a MIXED, MULTI-enum carrier (three
 *       collisions PeriodEnum/DayTypeEnum/BusinessDayConventionEnum + the PR #195 input-param);
 *       proves multi-enum + the joint unlock.</li>
 *   <li>{@code GetIntrstRate} (drr asic) — cross-corpus.</li>
 *   <li>{@code Create_MarginReportData} (drr asic) — a second drr carrier.</li>
 * </ul>
 */
class FunctionCheckedMapFqnTest {

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

    // ---- to-enum target FQN-inline on a source-name collision ----

    /** PURE single-enum collision (cdm6 settlement). */
    @Test
    @EnabledIf("cellsAvailable")
    void mapValuationMethod_cdm6_toEnumTargetCollidesWithSource_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/settlement/functions/MapValuationMethod.java");
    }

    /** MIXED, MULTI-enum (3 collisions) + the PR #195 input-param — proves the joint unlock. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapDateOffsetToRelativeDateOffset_cdm6_multiEnumJointUnlock_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/datetime/functions/MapDateOffsetToRelativeDateOffset.java");
    }

    /** Cross-corpus (drr asic projection). */
    @Test
    @EnabledIf("cellsAvailable")
    void getIntrstRate_drr_toEnumTargetCollidesWithSource_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/asic/rewrite/trade/functions/GetIntrstRate.java");
    }

    /** Second drr carrier (asic margin). */
    @Test
    @EnabledIf("cellsAvailable")
    void createMarginReportData_drr_toEnumTargetCollidesWithSource_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/asic/rewrite/margin/functions/Create_MarginReportData.java");
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
                + " — the duplicate same-simple-name import (source enum + target enum) reappears if "
                + "the checkedMapFqn collision fix (ConversionHandler.enumTargetCollisionFqn — render "
                + "the to-enum target FQN-inline + suppress its import) is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
