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
 * PR #232 — the {@code objFallback} residual bundle: THREE disjoint green-safe
 * GENERATOR facets composed in one PR (15 FUNCTION flips), each locked here by a
 * WHOLE-FILE byte anchor through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} + {@link FunctionGenerator}
 * (newline-normalized, revert-RED). The three facets touch disjoint seats:
 *
 * <p>(A fourth candidate — {@code defaultChainOperandUnwrap} at
 * {@code SetOperationHandler.handle(RDefaultExpr)} — was characterized and prototyped
 * but DROPPED: it regressed 8 green drr functions whose {@code default} operand is a
 * bare VALUE call (e.g. {@code Create_SubmissionHeader}'s {@code getOrDefault(now.evaluate())}),
 * which the shared handler cannot cleanly distinguish from a Mapper-chain operand. It
 * is carried as a #233 lead.)
 *
 * <ul>
 *   <li><b>B — enumReturnWrap</b>
 *       ({@code FunctionExpressionRenderer.appendReturnLadder}): a bare Rosetta
 *       enum-constant return-ladder rung in a {@code MapperS<Enum>} alias wraps
 *       {@code MapperS.of(<Enum>.<CONST>)} (the enum sibling of the existing ctor /
 *       ComparisonResult rung wraps; the ladder already wraps integer literals).</li>
 *   <li><b>R — aliasReturnChainContinuationIndent</b>
 *       ({@code FunctionExpressionRenderer.renderAlias}): the plain alias-body
 *       fall-through re-anchors a wrapped list-op chain continuation to the alias-method
 *       statement indent via {@code reindentContinuation} — the one alias path that
 *       previously skipped it (golden lays a wrapped {@code .first()} continuation at
 *       4 tabs; the fork emitted 1).</li>
 *   <li><b>D — ctorArgArrayListCopy</b>
 *       ({@code ConstructionHandler.coerceCtorArg}): a MULTI ctor-setter whose value is
 *       a direct alias/function call takes the defensive copy
 *       {@code new ArrayList<>(<call>.getMulti())} (the with-diamond chain sibling of
 *       arm C2r; a navigation-chain value stays bare).</li>
 * </ul>
 *
 * <p>All anchors use cdm5 / cdm6 carriers; reverting the corresponding seat restores
 * the divergent form and turns the anchor RED. Green-safety for every facet is
 * established by the full 5-cell D11 matrix (20/20) + the PRE/POST within-waiver
 * regression scan (0 regressions); these anchors lock the byte form against
 * regression, not green-safety per se.
 */
class FunctionObjFallbackBundleTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm5FunctionOutput;
    private static Map<String, String> cdm6FunctionOutput;

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
        if (cdm5CellAvailable()) {
            cdm5FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
        }
        if (cdm6CellAvailable()) {
            cdm6FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
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

    // ---- B: enumReturnWrap --------------------------------------------------

    /**
     * ApplyFinalRateRounding (cdm6) — the {@code direction} alias return-ladder's ELSE
     * rung {@code return MapperS.of(RoundingDirectionEnum.NEAREST);} (golden); the fork
     * emitted the bare enum constant. Reverting facet B restores the bare rung.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void applyFinalRateRoundingCdm6_enumReturnElseRung_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/product/asset/floatingrate/functions/ApplyFinalRateRounding.java");
    }

    /** ApplyFinalRateRounding (cdm5) — the cdm5 twin of the enum else-rung carrier. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void applyFinalRateRoundingCdm5_enumReturnElseRung_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/product/asset/floatingrate/functions/ApplyFinalRateRounding.java");
    }

    /**
     * MapFxCashSettlementToSettlementTerms (cdm6) — the {@code settlementType} alias
     * return-ladder's THEN rung {@code return MapperS.of(SettlementTypeEnum.CASH);}
     * (golden), exercising the THEN-rung enum wrap (the else rung is a typed
     * {@code MapperS.<SettlementTypeEnum>ofNull()}).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapFxCashSettlement_enumReturnThenRung_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/settlement/functions/MapFxCashSettlementToSettlementTerms.java");
    }

    // ---- R: aliasReturnChainContinuationIndent ------------------------------

    /**
     * MapRelativeDateSequenceToRelativeDates (cdm6) — a plain alias body whose wrapped
     * list-op continuation {@code .first();} golden lays at 4 tabs (baseIndent 3 + the
     * relative CHAIN_LINK tab); the fork's plain fall-through emitted 1 tab. Reverting
     * facet R (the {@code reindentContinuation} in {@code renderAlias}) restores the
     * single-tab continuation.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapRelativeDateSequenceToRelativeDates_chainContinuationIndent_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/datetime/functions/MapRelativeDateSequenceToRelativeDates.java");
    }

    /** MapCreditDefaultSwapPayout (cdm6) — a second alias-continuation-indent carrier. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapCreditDefaultSwapPayout_chainContinuationIndent_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/creditdefaultswap/functions/MapCreditDefaultSwapPayout.java");
    }

    // ---- D: ctorArgArrayListCopy --------------------------------------------

    /**
     * MapTrade (cdm6) — a MULTI ctor-setter whose value is the direct alias call
     * {@code counterpartyList(...)}: golden
     * {@code .setCounterparty(new ArrayList<>(counterpartyList(...).getMulti()))}; the
     * fork emitted the bare {@code .setCounterparty(counterpartyList(...).getMulti())}.
     * Reverting facet D restores the bare splice.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapTrade_ctorArgArrayListCopy_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/tradestate/functions/MapTrade.java");
    }

    /** MapQuantityChangeInstruction (cdm6) — a second direct-call ctor-arrayList carrier. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapQuantityChangeInstruction_ctorArgArrayListCopy_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/workflowstep/functions/MapQuantityChangeInstruction.java");
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
                + path + " — PR #232 objFallback bundle (enumReturnWrap / "
                + "aliasReturnChainContinuationIndent / ctorArgArrayListCopy).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
