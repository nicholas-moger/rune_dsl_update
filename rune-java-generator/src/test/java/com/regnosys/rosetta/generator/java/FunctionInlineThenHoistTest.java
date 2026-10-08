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
 * PR #219 — inline_then_hoist (the boolean-context slice). A {@code then}(-chain)
 * whose OUTPUT is a {@code ComparisonResult} ({@code … then exists} / {@code … then
 * all = True}), consumed at a NESTED position (an if-condition, an {@code and}/{@code
 * orNullSafe} operand, or an alias-body {@code return}) where a statement-hoist sink is
 * reachable, is HOISTED to the upstream {@code thenArg} form instead of the fork's
 * legacy inline {@code arg.then(item -> …)}:
 *
 * <pre>
 * final MapperC&lt;String&gt; thenArg = MapperC.&lt;CommodityPayout&gt;of(commodityPayouts)
 *     .mapItem(item -&gt; MapperS.of(getCommodityKey.evaluate(item.get())));
 * if (exists(thenArg).asMapper().getOrDefault(false)) { … }
 * </pre>
 *
 * <p>The deep-position analogue of {@code FunctionExpressionRenderer.renderThenExtractSet}'s
 * SET-position {@code thenArg} hoist (PR #98/#172). Three seats:
 * <ol>
 *   <li>{@code CollectionHandler.tryDeepThenHoist} — registers {@code final Mapper*<X>
 *       thenArg = <arg>;} on {@code scope.findStatementHoistSink()} (via the
 *       {@code StatementHoistSession} {@code thenArg} group) and returns the re-rooted body
 *       as a Mapper; the consuming context applies its own {@code .get()}/{@code
 *       .getOrDefault(...)} unwrap. Chain-link over-fire (an implicit SUB-chain of a chain
 *       whose OUTERMOST then is non-hoistable) is suppressed via
 *       {@code JavaStatementScope.pushThenHoistSuppression} around the inline argument
 *       compile; the Object-erased element type is recovered from the compiled value
 *       (PR #204 pattern).</li>
 *   <li>{@code FunctionExpressionRenderer.renderAliasThenHoistOrNull} — the alias-body seat
 *       (marks a per-method sink, prepends the decls, wraps a single {@code return}).</li>
 *   <li>{@code LogicalHandler.wrapBooleanFunctionOperand} — an {@code and}/{@code orNullSafe}
 *       {@code then exists} operand gets the {@code ComparisonResult.ofNullSafe(…)}
 *       coercion the receiver method requires.</li>
 * </ol>
 *
 * <p><b>SCOPED to a ComparisonResult OUTPUT</b> (the boolean-context slice): a VALUE
 * then-output (identity / extract / map) hoisted from a deep position CASCADES into the
 * enclosing ctor-builder / conditional rendering (the statement-hoist forces a block form
 * that renumbers co-resident hoist groups), so it DECLINES to the inline form (a later
 * facet). <b>Green-safe by construction:</b> the inline {@code arg.then(item -> …)} form
 * never compiled (there is no runtime {@code Mapper.then(Function)} method) — ZERO
 * corpus-9.83.0 goldens carry it (plain grep), so every carrier this fires on was already
 * a waivered mismatch. Whole-file byte anchors through the REAL D11 loader over all 6 clean
 * carriers (cdm5 ×1, cdm6 ×2, drr ×3). REVERT-VERIFIED RED.
 */
class FunctionInlineThenHoistTest {

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

    // ---- if-condition seat (then exists → exists(thenArg).asMapper().getOrDefault(false)) ----

    /** drr `if commodityPayouts ... then exists` in an if-condition. */
    @Test
    @EnabledIf("cellsAvailable")
    void drr_commodityCommodityLeg1_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/CommodityCommodityLeg1.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void drr_commodityCommodityLeg2_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/CommodityCommodityLeg2.java");
    }

    // ---- orNullSafe operand seat (then exists → ComparisonResult.ofNullSafe(exists(thenArg).asMapper())) ----

    /** cdm `notExists(...).orNullSafe(<then-chain> then exists)` — 4-level thenArg chain. */
    @Test
    @EnabledIf("cellsAvailable")
    void cdm5_checkAssetType_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/product/collateral/functions/CheckAssetType.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_checkAssetType_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/product/collateral/functions/CheckAssetType.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_checkAgencyRating_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/product/collateral/functions/CheckAgencyRating.java");
    }

    // ---- alias-body seat (then exists → final thenArg + return <ComparisonResult>.asMapper()) ----

    @Test
    @EnabledIf("cellsAvailable")
    void drr_createAnnaDsbUpiRequest_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/enrichment/upi/functions/Create_AnnaDsbUpiRequest.java");
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
                + " — a ComparisonResult-output then(-chain) consumed at a nested boolean position "
                + "must hoist `final Mapper*<X> thenArg = <arg>;` and return the re-rooted body "
                + "(inline_then_hoist). Reverting the deep thenArg hoist reverts this anchor to the "
                + "inline arg.then(item -> ...) form (RED).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
