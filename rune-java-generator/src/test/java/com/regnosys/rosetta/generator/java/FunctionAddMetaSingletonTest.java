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
 * PR #202 (facet {@code metaFinisher} — the META-output complement of PR #199/#201's
 * {@code addAllItemIntoList}/{@code addAllToBuilderList}). A whole-output ADD whose value is
 * SINGLE-cardinality into a MULTI (List) output whose element is a META-ANNOTATED wrapper
 * ({@code FieldWithMetaX} / {@code ReferenceWithMetaX}) coerces the item into a list via the same
 * null-guarded {@code if/else} {@code addAll(toBuilder(...))} block as #201, but the hoisted local,
 * the {@code Collections.<X>emptyList()} type-arg and the hoist base name all use the META WRAPPER
 * simple name:
 *
 * <pre>
 * final &lt;FieldWithMetaX&gt; &lt;fieldWithMetaX[N]&gt; = &lt;fn&gt;.evaluate(...);
 * if (&lt;fieldWithMetaX[N]&gt; == null) {
 *     &lt;list&gt;.addAll(toBuilder(Collections.&lt;FieldWithMetaX&gt;emptyList()));
 * } else {
 *     &lt;list&gt;.addAll(toBuilder(Collections.singletonList(&lt;fieldWithMetaX[N]&gt;)));
 * }
 * </pre>
 *
 * <p>PR #199/#201 DECLINED the meta output via the {@code detectMetaKind(output) != NONE} gate in
 * {@code FunctionExpressionRenderer.renderAddSingletonItemOrNull}. PR #202 drops that gate and lifts
 * the bare item type to the meta wrapper via {@code RJavaWithMetaValue.create} (the #186/#193
 * resolveParam / singleMetaOutputEmptyElseWrapper pattern). The wrapper class is already imported in
 * gen (the #186 output-type resolveParam), and {@code outputNeedsBuilder} stays true (the wrapper is a
 * RosettaModelObject), so each {@code addAll} arm toBuilder-wraps exactly as #201.
 *
 * <p>Green-safe by construction: the fork's pre-fix meta form is the non-compiling
 * {@code <list>.addAll(toBuilder(<single wrapper>))} (a single object into {@code addAll(Collection)});
 * every carrier was already a waivered mismatch. The inherited #201 value-admissibility guard
 * ({@code RSymbolReference(RFunction)}/{@code RConstructorExpr} + RShortcut-exclusion +
 * {@code getCardinality != MULTI}) excludes the only green meta-list ADD ({@code MapContractualParty},
 * whose value is a genuinely-MULTI {@code MapperC} nav chain).
 *
 * <p>Whole-file byte anchors through the REAL D11 loader (transitive rune-fpml dep per PR #184):
 * <ul>
 *   <li>{@code MapNotionalAmountToQuantityList} (cdm6 product/returnswap) — ONE fn-call ADD hoisting an
 *       un-suffixed {@code final FieldWithMetaNonNegativeQuantitySchedule fieldWithMetaNonNegativeQuantitySchedule = …;}.</li>
 *   <li>{@code MapFxOptionToQuantityListWithLocation} (cdm6 pricequantity) — TWO fn-call ADDs hoisting
 *       {@code fieldWithMetaNonNegativeQuantitySchedule0/1} numbered through the StatementHoistSession.</li>
 *   <li>{@code MapCalculationPeriodAmountToQuantityList} (cdm6 product/swap) — a third Map*List sub-shape.</li>
 * </ul>
 */
class FunctionAddMetaSingletonTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR = CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            cdm6FunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
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

    /** ONE fn-call ADD: un-suffixed {@code fieldWithMetaNonNegativeQuantitySchedule} hoist + toBuilder block. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapNotionalAmountToQuantityList_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/returnswap/functions/MapNotionalAmountToQuantityList.java");
    }

    /** TWO fn-call ADDs: numbered {@code fieldWithMetaNonNegativeQuantitySchedule0/1} hoists. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapFxOptionToQuantityListWithLocation_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/pricequantity/functions/MapFxOptionToQuantityListWithLocation.java");
    }

    /** A third Map*List sub-shape. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapCalculationPeriodAmountToQuantityList_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/swap/functions/MapCalculationPeriodAmountToQuantityList.java");
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
                + " — the non-compiling <list>.addAll(toBuilder(<single meta wrapper>)) reappears if the "
                + "metaFinisher fix (FunctionExpressionRenderer.renderAddSingletonItemOrNull — drop the "
                + "detectMetaKind(output)!=NONE decline + RJavaWithMetaValue.create meta-lift of itemJavaType) "
                + "is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
