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
 * Facet {@code inlineTernaryIteHoist} (PR #223) — a Rosetta if-then-else consumed
 * at an OPERAND seat renders as the upstream hoisted statement / block lambda, not
 * the fork's inline {@code <cond>.getOrDefault(false) ? <then> : <else>} ternary
 * ({@code ControlFlowHandler#handle}). The operand-seat continuation of the
 * #173/#181/#183 ite-hoist campaign, in TWO green-safe, regression-free mechanisms:
 *
 * <ol>
 *   <li><b>alias-body statement hoist</b> — a conditional NESTED in a coerced alias
 *       body (e.g. {@code return MapperS.of(UnitType.builder().setCurrency(
 *       mapCurrency.evaluate(if cond then A else B)))}) hoists {@code final <T>
 *       ifThenElseResult; if/else {…}} BEFORE the return, via
 *       {@code FunctionExpressionRenderer#renderAliasReturnCoerceOrNull} marking the
 *       per-method alias body scope a statement-hoist sink and draining the nested
 *       hoist (MapFxOptionStrikePrice / InterestRateLeg1FixedFixed /
 *       InterestRateLeg2FixedFixed).</li>
 *   <li><b>in-lambda effective-else block lambda</b> — a WHOLE-body EFFECTIVE-else
 *       single-cardinality conditional map body renders {@code item -> { if
 *       (<cond>.getOrDefault(false)) { return <then>; } return <else>; }}
 *       ({@code CollectionHandler#compileEffectiveElseConditionalBlock}, the
 *       effective-else sibling of the #168 elseless block), the arms compiled with
 *       the SAME expected type the ternary uses so the {@code return} arms are
 *       byte-identical to the ternary arms — only the restructure differs
 *       (PriceQuantityTriangulation / ReplaceTradeLot / ReplaceParty).</li>
 * </ol>
 *
 * <p><b>Green-safe by construction:</b> ZERO of the 34,686 frozen goldens carry the
 * inline {@code getOrDefault(false) ? } ternary in ANY seat (corpus-wide plain-grep),
 * so every carrier lives in an already-waivered FUNCTION mismatch; the full 5-cell
 * D11 matrix (20/20) plus the PRE/POST regression scan (0 removed-golden-line
 * regressions) are the empirical arbiters. The facet pays 9 FUNCTION flips
 * (cdm5 ×3 + cdm6 ×4 + drr ×2). The IN-LAMBDA NESTED statement hoist (a conditional
 * nested in a builder lambda body → block-lambda with hoisted locals; the drr
 * {@code Get*} projection family) is DEFERRED — it re-indents builder lines that
 * coincidentally matched golden on co-occupied files whose enclosing chain golden
 * restructures ({@code thenArg}/{@code mapListToItem}), a within-waiver regression
 * the {@code function_comparison} BUILD SUCCESS masks but the regression scan flags.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class FunctionInlineTernaryIteHoistTest {

    private static final Path CDM_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM_GOLDEN_DIR =
            CDM_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdmFunctionOutput;
    private static Map<String, String> cdm5FunctionOutput;
    private static Map<String, String> drrFunctionOutput;

    static boolean cdmCellAvailable() {
        return Files.isDirectory(CDM_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM_GOLDEN_DIR);
    }

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
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
        if (cdm5CellAvailable()) {
            cdm5FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
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

    // ----- mechanism 1: alias-body statement hoist -----

    /**
     * MapFxOptionStrikePrice — TWO alias methods ({@code priceUnits}/{@code perUnitOf})
     * whose coerced body {@code return MapperS.of(UnitType.builder().setCurrency(
     * mapCurrency.evaluate(if … then … else …)))} hoists {@code final Currency
     * ifThenElseResult; if/else {…}} before the return.
     */
    @Test
    @EnabledIf("cdmCellAvailable")
    void mapFxOptionStrikePriceCdm_aliasBodyNestedHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdmFunctionOutput, CDM_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/fxoption/functions/MapFxOptionStrikePrice.java");
    }

    /**
     * InterestRateLeg1FixedFixed (drr) — an alias body whose nested conditional hoists
     * (the alias-body mechanism at a drr seat), pinning the drr cell.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void interestRateLeg1FixedFixedDrr_aliasBodyHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/InterestRateLeg1FixedFixed.java");
    }

    /** InterestRateLeg2FixedFixed (drr) — the Leg2 twin of the alias-body hoist carrier. */
    @Test
    @EnabledIf("drrCellAvailable")
    void interestRateLeg2FixedFixedDrr_aliasBodyHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/InterestRateLeg2FixedFixed.java");
    }

    // ----- mechanism 2: in-lambda effective-else block lambda -----

    /**
     * PriceQuantityTriangulation — a WHOLE-body effective-else conditional
     * {@code mapItem(item -> contains(…).getOrDefault(false) ? MapperS.of(…) :
     * MapperS.of(true))} renders the {@code if}/{@code return} block lambda.
     */
    @Test
    @EnabledIf("cdmCellAvailable")
    void priceQuantityTriangulationCdm_inLambdaEffectiveElseBlock_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdmFunctionOutput, CDM_GOLDEN_DIR,
                "cdm/product/template/functions/PriceQuantityTriangulation.java");
    }

    /** cdm/5.38.0 twin of PriceQuantityTriangulation, pinning the cdm5 cell. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void priceQuantityTriangulationCdm5_inLambdaEffectiveElseBlock_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/product/template/functions/PriceQuantityTriangulation.java");
    }

    /** ReplaceTradeLot (cdm6) — a second in-lambda effective-else block carrier. */
    @Test
    @EnabledIf("cdmCellAvailable")
    void replaceTradeLotCdm_inLambdaEffectiveElseBlock_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdmFunctionOutput, CDM_GOLDEN_DIR,
                "cdm/product/template/functions/ReplaceTradeLot.java");
    }

    /**
     * ReplaceParty (cdm5) — an in-lambda effective-else conditional whose block-lambda
     * conversion drops the gen-only MapperC import; pins the cdm5 cell.
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void replacePartyCdm5_inLambdaEffectiveElseBlock_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/base/staticdata/party/functions/ReplaceParty.java");
    }

    /** ReplaceParty (cdm6) — the cdm6 twin of the in-lambda effective-else block carrier. */
    @Test
    @EnabledIf("cdmCellAvailable")
    void replacePartyCdm_inLambdaEffectiveElseBlock_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdmFunctionOutput, CDM_GOLDEN_DIR,
                "cdm/base/staticdata/party/functions/ReplaceParty.java");
    }

    /** ReplaceTradeLot (cdm5) — the cdm5 twin of the in-lambda effective-else block carrier. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void replaceTradeLotCdm5_inLambdaEffectiveElseBlock_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/product/template/functions/ReplaceTradeLot.java");
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
                + path + " — the operand-seat ite-hoist (the alias-body statement hoist "
                + "or the in-lambda effective-else block lambda) is missing or regressed, "
                + "if the inlineTernaryIteHoist recovery is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
