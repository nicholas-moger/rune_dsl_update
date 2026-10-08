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
 * Facet {@code objFallback_witness_recovery} Stage B — alias-base {@code ? extends}
 * wildcard, extended to a FUNCTION-CALL alias body (PR #241; the deferred #238/#239
 * {@code Create_AnnaDsbUpiRequestFromReportableEvent} carrier).
 *
 * <p>The deep-then hoist path ({@code CollectionHandler.tryDeepThenHoist}) hoists a
 * {@code then} over a direct alias call to a {@code final Mapper*<X> thenArg} statement
 * local. At {@code e691cb16}'s pin the deep path carried only the Stage A invariant
 * recovery ({@code NavigationHandler.recoverThenArgItemRType}); PR #241 wires in the
 * Stage B alias {@code ? extends} arm ({@code NavigationHandler.aliasDerivedThenArgItemType})
 * the SHALLOW then-SET path already had, AND fixes the gap that kept it from resolving
 * here: a function-call alias body
 * ({@code alias product: ProductForTrade(TradeForEvent(reportableEvent))}) parses as an
 * {@code RSymbolReference} whose symbol is an {@code RFunction}, a shape
 * {@code recoverThenArgItemRType}'s {@code resolveReceiverRType} does not resolve
 * (RSymbolReference→RAttribute only — its data-type sibling {@code resolveReceiverDataType}
 * does carry the RFunction arm). {@code aliasDerivedThenArgItemType} now recovers the
 * callee's declared OUTPUT type, so the {@code isSwaption} alias's
 * {@code product then (IsCreditSwaption or IsIRSwaption)} hoist declares
 * {@code final MapperS<? extends Product> thenArg = product(reportableEvent)} instead of
 * the non-compiling {@code MapperS<Object> thenArg} (an {@code incompatible-types}
 * NON_COMPILING mismatch, already waivered).
 *
 * <p>GREEN-SAFE BY CONSTRUCTION: the wildcard arm is gated on the parser snapshot having
 * erased to {@code Object} (a resolved non-Object type is kept, byte-identical) AND the
 * leaf being an alias; the new function-call fallback is SCOPED to the alias-body walk, so
 * the shared Stage A recovery (direct then-arguments) is unchanged. Zero golden carries the
 * {@code Mapper*<Object>} gen form, so the Object-gate only ever touches currently-waivered
 * output.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class FunctionThenArgAliasFnCallWildcardTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> drrFunctionOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
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

    /**
     * The carrier: {@code Create_AnnaDsbUpiRequestFromReportableEvent}'s {@code isSwaption}
     * alias is {@code product then (IsCreditSwaption or IsIRSwaption)}; the deep-then hoist
     * declares {@code final MapperS<? extends Product> thenArg = product(reportableEvent)}.
     * The element {@code Product} is recovered by walking the {@code product} alias's own
     * function-call body {@code ProductForTrade(TradeForEvent(reportableEvent))} to the
     * {@code ProductForTrade} output type — the PR #241 function-call-body fallback. Reverting
     * the fallback (or the deep-path Stage B arm) re-emits {@code MapperS<Object> thenArg} and
     * fails this byte match.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void createAnnaDsbFromReportableEventDrr_aliasFnCallBodyWildcard_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/enrichment/upi/functions/Create_AnnaDsbUpiRequestFromReportableEvent.java");
    }

    /**
     * GREEN-SAFETY lock: {@code InterestRateLeg1FixedFixed} declares a CONCRETE
     * {@code final MapperC<Integer> thenArg = interestRatePayouts(product).<…>map(…)} — its
     * element ({@code Integer}) comes from a further map, so the parser snapshot resolves
     * non-Object and the Object-gated alias-wildcard (and therefore the PR #241 function-call
     * fallback inside it) never fires. Locks that the PR #241 fallback does NOT over-fire onto
     * a green concrete alias-base thenArg (would regress it to {@code MapperC<? extends Integer>}).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void interestRateLeg1FixedFixedDrr_concreteAliasThenArgUnchanged_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/InterestRateLeg1FixedFixed.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> functionOutput,
            Path goldenDir, String path) throws IOException {
        assertNotNull(functionOutput, "Function generation did not run — corpus unavailable?");
        String generated = functionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — the objFallback_witness_recovery Stage B alias-base ? extends wildcard "
                + "extended to a function-call alias body (PR #241: "
                + "NavigationHandler.aliasDerivedThenArgItemType's RFunction-output fallback + "
                + "the deep-then hoist Stage B arm) is missing or regressed if reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
