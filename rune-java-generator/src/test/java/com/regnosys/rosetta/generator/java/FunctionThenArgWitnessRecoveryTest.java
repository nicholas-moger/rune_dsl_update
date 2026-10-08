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
 * Facet {@code objFallback_witness_recovery} (PR #238) — a hoisted
 * {@code final Mapper*<X> thenArg} statement local whose element type the parser
 * snapshot ({@code gm.workspace().getInferredType}) erased to {@code Object}, where
 * golden carries the concrete element type. The byte-identical RHS chain already
 * renders the concrete witness (its terminal {@code .<X>map}/{@code .<X>mapC} was
 * resolved per-feature by {@code NavigationHandler}), so javac rejects assigning
 * {@code Mapper*<X>} to the declared {@code Mapper*<Object>} local (or its
 * {@code toBuilder}/{@code areEqual} consumer) — every carrier is an
 * {@code incompatible-types} NON_COMPILING mismatch, already waivered.
 *
 * <p>Two arms (this is the dominant-NC objFallback bulk lever at its cleanest seat):
 *
 * <ol>
 *   <li><b>Stage A — nav-chain / filter recovery (shared walk).</b> PR #204's
 *       AST-walk recovery ({@code FunctionExpressionRenderer.thenArgItemRType}) was
 *       PROMOTED to {@code NavigationHandler.recoverThenArgItemRType} so the deep-then
 *       hoist path ({@code CollectionHandler.tryDeepThenHoist}) — which previously had
 *       ONLY a compiled-value-type fallback, {@code null} for a {@code filter}
 *       terminal — shares it ({@code Qualify_SubProduct_FixedFloat}, cdm5+cdm6). The
 *       walk also gained a filter-preserves arm: a then-body that FILTERS the piped
 *       item ({@code thenArg(k-1).filterItemNullSafe(...)}) preserves the then
 *       ARGUMENT's element type, recovered from the argument rather than the
 *       unresolvable {@code RImplicitVariable} filter receiver
 *       ({@code GetTransactionInformationForRegime}, drr).</li>
 *   <li><b>Stage B — alias base + {@code ? extends} wildcard.</b> A then-arg whose
 *       element-producing leaf is an ALIAS call ({@code interestRatePayouts(product)})
 *       declares the alias signature's upper-bounded {@code Mapper*<? extends X>}
 *       return form, the element resolved by walking the alias's OWN expression body
 *       via {@code NavigationHandler.aliasDerivedThenArgItemType}
 *       ({@code InterestRateLeg1Basis}/{@code InterestRateLeg2Basis}, drr).</li>
 * </ol>
 *
 * <p>GREEN-SAFE BY CONSTRUCTION: every recovery is gated on the snapshot having
 * erased to {@code Object} (a resolved non-Object type is kept, byte-identical), and
 * zero golden carries the {@code Mapper*<Object>} gen form. The wildcard fires ONLY
 * for an alias-derived element — a green concrete alias-base thenArg whose element
 * comes from a further map ({@code InterestRateLeg1FixedFixed}'s
 * {@code MapperC<Integer> thenArg = interestRatePayouts(product).<…>map(…)}) keeps
 * the snapshot's resolved {@code Integer} and is untouched (the green-safety lock).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class FunctionThenArgWitnessRecoveryTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm5FunctionOutput;
    private static Map<String, String> cdm6FunctionOutput;
    private static Map<String, String> drrFunctionOutput;

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
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
     * Stage A — deep-then hoist nav/filter recovery: a {@code filter} over a
     * {@code economicTerms -> payout -> interestRatePayout} nav chain hoists
     * {@code final MapperC<InterestRatePayout> thenArg = ...}. The deep path's
     * compiled-value-type fallback is null for the filter terminal; the promoted
     * AST-walk {@code NavigationHandler.recoverThenArgItemRType} resolves the
     * element type instead of the non-compiling {@code MapperC<Object>} (cdm5).
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void qualifySubProductFixedFloatCdm5_deepThenNavFilterRecovery_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/product/qualification/functions/Qualify_SubProduct_FixedFloat.java");
    }

    /** Stage A — the cdm6 sibling pins the same deep-then nav/filter recovery. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifySubProductFixedFloatCdm6_deepThenNavFilterRecovery_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/product/qualification/functions/Qualify_SubProduct_FixedFloat.java");
    }

    /**
     * Stage A — filter-preserves: {@code thenArg1 = thenArg0.filterItemNullSafe(...)}
     * preserves {@code thenArg0}'s element type ({@code TransactionInformation}); the
     * recovery walk recurses to the then ARGUMENT rather than the unresolvable
     * {@code RImplicitVariable} filter receiver, declaring
     * {@code MapperC<TransactionInformation>} not {@code MapperC<Object>} (drr).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getTransactionInformationForRegimeDrr_filterPreservesElementType_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/GetTransactionInformationForRegime.java");
    }

    /**
     * Stage B — alias base + wildcard: a {@code filter} over the alias call
     * {@code interestRatePayouts(product)} declares the alias signature's
     * upper-bounded {@code MapperC<? extends InterestRatePayout> thenArg}, the element
     * recovered by walking the alias's own {@code EconomicTermsForProduct(product) ->
     * payout -> interestRatePayout} body (drr).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void interestRateLeg1BasisDrr_aliasBaseWildcard_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/InterestRateLeg1Basis.java");
    }

    /** Stage B — the Leg2 sibling pins the same alias-base {@code ? extends} wildcard (drr). */
    @Test
    @EnabledIf("drrCellAvailable")
    void interestRateLeg2BasisDrr_aliasBaseWildcard_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/InterestRateLeg2Basis.java");
    }

    /**
     * GREEN-SAFETY lock: {@code InterestRateLeg1FixedFixed} declares a CONCRETE
     * {@code final MapperC<Integer> thenArg = interestRatePayouts(product).<…>map(…)} —
     * its element ({@code Integer}) comes from a further map, so the snapshot resolves
     * non-Object and the Object-gated alias-wildcard never fires. Locks that the PR
     * #238 recovery does NOT over-fire the {@code ? extends} wildcard onto a green
     * concrete alias-base thenArg (would regress it to {@code MapperC<? extends Integer>}).
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
                + " — the objFallback_witness_recovery facet (PR #238: the shared "
                + "NavigationHandler.recoverThenArgItemRType walk in the deep-then hoist path, "
                + "the filter-preserves arm, or the alias-base ? extends wildcard) is missing "
                + "or regressed if reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
