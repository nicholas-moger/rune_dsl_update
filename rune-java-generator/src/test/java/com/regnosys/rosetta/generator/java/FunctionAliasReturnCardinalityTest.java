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
 * Facet {@code aliasReturnCardinality} (PR #248) — an alias method's return
 * <em>cardinality</em> ({@code MapperS} vs {@code MapperC}) is computed by
 * {@code FunctionAliasHelper}'s own focused inference walk (not the body's
 * {@code ExpressionCompiler}). Two gaps left a genuinely-MULTI alias signed as
 * single {@code MapperS<? extends T>} over a {@code MapperC} body — a Java type
 * mismatch, so the carrier never compiled and was already a waivered mismatch:
 *
 * <ol>
 *   <li><b>Facet A — disguised-head cardinality.</b>
 *       {@code FunctionAliasHelper.isReceiverMultiInner}'s disguised
 *       {@code head -> feature} ({@code REnumValueRef}) branch resolved the head
 *       ONLY as a single parameter ({@code lookupParameterType} returns the alias
 *       ELEMENT type, discarding the alias's multi-ness) and then read the
 *       feature's own (single) cardinality. So a chain whose head is a MULTI
 *       ALIAS — {@code openTradeState -> trade} where {@code openTradeState} is
 *       {@code MapperC<TradeState>} — leaked single. This is the cardinality
 *       counterpart of PR #228's disguised-chain-receiver TYPE fix (the TYPE came
 *       out right because it rides {@code fc.resolvedFeature()}; only the
 *       receiver-walked cardinality was wrong).</li>
 *   <li><b>Facet B — extract preserves receiver cardinality.</b> The
 *       {@code RExtractExpr} arm of {@code inferExpressionType} returned ONLY the
 *       map BODY's cardinality, so an extract/map over a multi receiver —
 *       {@code openTradeStates extract [ item -> Fn(item) ]} — leaked single
 *       (despite the arm's own comment "keeps cardinality from receiver"). An
 *       extract PRESERVES the receiver's cardinality.</li>
 * </ol>
 *
 * <p><b>Green-safe by construction.</b> A {@code MapperS}-declared alias over a
 * {@code MapperC} body is a compile error, so every carrier was already a
 * waivered mismatch; and the fix only ADDS multi where a genuine multi step
 * exists ({@code isReceiverMultiInner} true) — it never turns single into multi
 * spuriously. A receiver-collapsing list op ({@code only-element}/{@code first}/
 * {@code last}) tops the body as {@code RListOpExpr} (the hard {@code false}
 * barrier), so a collapsed chain is not reached. The corpus carries no green
 * counterexample: an extract over a multi receiver renders {@code .mapItem} and
 * is {@code MapperC}; a single-receiver extract renders {@code .map} and stays
 * {@code MapperS}. The PRE/POST within-waiver regression scan over all 1,503
 * still-waivered files is 0 regressions / 14 improvement-churn (co-occupied
 * carriers whose cardinality is now corrected, moving toward golden) / 2
 * neutral-churn; the full 5-cell D11 matrix (20/20) is the empirical arbiter.
 * The facet pays 4 FUNCTION flips (cdm5 ×2 + cdm6 ×2).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. Each whole-file lock also
 * pins the co-resident SINGLE aliases (e.g. {@code instruction},
 * {@code beforeTradeState}) staying {@code MapperS} — reverting either fork arm
 * restores the {@code MapperS<…>}-over-{@code MapperC}-body leak and turns these
 * RED.
 */
class FunctionAliasReturnCardinalityTest {

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

    // ---- cdm6 ----------------------------------------------------------------

    /**
     * Qualify_PairOff (cdm6) — the {@code packageRef} alias body
     * {@code openTradeState -> trade -> executionDetails -> packageReference}: the
     * disguised {@code openTradeState -> trade} head is the multi alias
     * {@code openTradeState} ({@code MapperC<? extends TradeState>}). Facet A makes
     * the signature {@code MapperC<? extends IdentifiedList>}; the leak was
     * {@code MapperS<? extends IdentifiedList>}.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyPairOffCdm6_disguisedAliasHead_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/common/functions/Qualify_PairOff.java");
    }

    /**
     * Qualify_Shaping (cdm6) — carries BOTH facets: {@code packageRef} (facet A,
     * disguised multi-alias head) and {@code openTradeNoExecutionDetails}
     * ({@code openTradeStates extract [ item -> Fn(item) ]}, facet B — an extract
     * over the multi alias {@code openTradeStates}). Both signatures become
     * {@code MapperC}; the file flips only when BOTH arms land.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyShapingCdm6_extractOverMultiReceiver_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/common/functions/Qualify_Shaping.java");
    }

    // ---- cdm5 ----------------------------------------------------------------

    /** Qualify_PairOff (cdm5) — the cdm5 twin of the facet-A disguised-head carrier. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void qualifyPairOffCdm5_disguisedAliasHead_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/common/functions/Qualify_PairOff.java");
    }

    /** Qualify_Shaping (cdm5) — the cdm5 twin of the facet-A + facet-B carrier. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void qualifyShapingCdm5_extractOverMultiReceiver_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/common/functions/Qualify_Shaping.java");
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
                + path + " — a MULTI alias whose receiver is a disguised multi-alias head "
                + "(facet A) or an extract over a multi receiver (facet B) leaks the single "
                + "MapperS<… extends T> signature over a MapperC body if the "
                + "FunctionAliasHelper.isReceiverMultiInner REnumValueRef-head arm or the "
                + "inferExpressionType RExtractExpr receiver-cardinality arm is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
