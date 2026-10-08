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
 * Facet {@code maxmin_body_coercion} (the max/min lambda BODY coercion family,
 * characterized from the f-probe-175-mid residuals and deferred at PR #175) —
 * upstream compiles a {@code max}/{@code min} comparator-key lambda body against
 * its META-STRIPPED element type ({@code caseMaxOperation}/{@code caseMinOperation}
 * pass {@code MAPPER_S.wrapExtendsWithoutMeta(function.body)} and the generic
 * {@code addCoercions} appends the terminal unwrap — the comparator extracts a
 * {@code Comparable} key, which a meta wrapper is not), types the implicit item
 * from the operation's ARGUMENT including a LIST-LITERAL argument (the element
 * join), and renders a record-feature LEAF through {@code RecordJavaUtil}. Three
 * arms:
 *
 * <ol>
 *   <li><b>A — terminal meta-unwrap.</b> {@code CollectionHandler}'s max/min
 *       comparator-key lambdas compile at the new
 *       {@code LambdaBodyPosition.MAXMIN_KEY}, whose arm meta-strips the body's
 *       TERMINAL step via the navigation receivers' single-source-of-truth lever
 *       ({@code ExpressionCompiler.coerceNavigationReceiver} →
 *       {@code WrappedItemCoercer}): a chain ending on a meta attribute
 *       ({@code FieldWithMetaString}) appends the golden
 *       {@code .<String>map("Type coercion", x -> x == null ? null : x.getValue())}
 *       (MapperS null-guarded / MapperC bare — the upstream null-guard
 *       asymmetry). Fires iff the body's item type is a concrete
 *       {@code RJavaWithMetaValue} (only meta TERMINAL steps carry one), in the
 *       lambda's OWN body scope (the meta_coercion_numbering scope law) — drr
 *       {@code InterestRateLeg1/2CrossCurrency}.</li>
 *   <li><b>B — list-literal receiver item typing.</b> The gm-aware
 *       {@code NavigationHandler.resolveReceiverDataType} gains an
 *       {@code RListLiteral} arm: the receiver types as the IDENTICAL-OR-DECLINE
 *       join of its element types (upstream {@code caseListLiteral}'s
 *       {@code joinMetaAnnotatedTypes}; the PR #171 void_witness list-literal law
 *       at the {@code RDataType} level), so a min/max over
 *       {@code [chain -> exchangedCurrency1, chain -> exchangedCurrency2]} types
 *       its implicit item as {@code Cashflow} — cascading the {@code <T>map}
 *       witnesses, the type-derived lambda vars, and the interior receiver-side
 *       meta-unwraps through the EXISTING consumers, with arm A then appending
 *       the terminal unwrap — drr {@code FXLeg1/2} (function-call-rooted
 *       elements), {@code FXSwapLeg1/2} (alias-rooted elements).</li>
 *   <li><b>C — record-feature leaf synthesis.</b> A disguised 2-name chain whose
 *       LEAF is the {@code date} RECORD feature ({@code max [ timestamp -> date ]},
 *       {@code timestamp} a {@code zonedDateTime}) has no {@code RAttribute} leaf
 *       to bind, so {@code ReferenceHandler.synthesizeImplicitItemChain}
 *       structurally declined. The new {@code synthesizeRecordLeafChain} arm
 *       synthesizes the RESOLVED head + featureName-only leaf, which re-enters
 *       {@code NavigationHandler.handle(RFeatureCall)} where
 *       {@code tryRecordFeatureNav} (PR #147) renders the upstream
 *       {@code RecordJavaUtil} form
 *       {@code .<Date>map("Date", zdt -> Date.of(zdt.toLocalDate()))}. Admission
 *       reads the SAME {@code resolveReceiverRType} + {@code isDateRecordFeature}
 *       gate the record nav reads, on the SAME synthesized head node, so the arm
 *       fires exactly when the record nav will — drr {@code GetValuation}.</li>
 * </ol>
 *
 * <p>Green-safety: arm A's lever no-ops for every non-meta body (only meta
 * terminal steps carry a concrete {@code RJavaWithMetaValue} expression type),
 * and a meta-terminal comparator key previously rendered the wrapper-typed
 * (non-{@code Comparable}) key no golden carries; arm B's join is strictly
 * additive (the {@code RListLiteral} receiver shape previously always resolved
 * {@code null} = witness-less bytes) and declines on any unresolved or
 * disagreeing element; arm C replaces a NON-COMPILING invented getter
 * ({@code ZonedDateTime} carries no {@code getDate()}) and fires only where the
 * by-name leaf binding structurally declined. The full 5-cell D11 matrix (20/20)
 * is the empirical arbiter.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class FunctionMaxMinBodyCoercionTest {

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
     * B+A: a min over a LIST LITERAL of two function-call-rooted chains
     * ({@code [UnderlierForProduct(product) -> … -> exchangedCurrency1, … ->
     * exchangedCurrency2] min [item -> priceQuantity -> … -> currency]}) types
     * its implicit item as {@code Cashflow} via the element join — recovering the
     * {@code <T>map} witnesses, the type-derived lambda vars, the interior
     * receiver-side meta-unwraps — and arm A appends the terminal
     * {@code <String>} unwrap (drr).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void fxLeg1Drr_listLiteralItemJoinAndTerminalUnwrap_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/FXLeg1.java");
    }

    /** B+A: the {@code .max}-side sibling pins the same join + unwrap (drr). */
    @Test
    @EnabledIf("drrCellAvailable")
    void fxLeg2Drr_listLiteralItemJoinAndTerminalUnwrap_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/FXLeg2.java");
    }

    /**
     * B+A: the list-literal elements root at an ALIAS call ({@code [farLeg ->
     * underlier -> … -> exchangedCurrency1, …]}) — the element walk resolves the
     * leaf attribute regardless of the chain root, joining to {@code Cashflow} (drr).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void fxSwapLeg1Drr_aliasRootedListLiteralJoin_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/FXSwapLeg1.java");
    }

    /** B+A: the {@code .max}-side alias-rooted sibling (drr). */
    @Test
    @EnabledIf("drrCellAvailable")
    void fxSwapLeg2Drr_aliasRootedListLiteralJoin_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/FXSwapLeg2.java");
    }

    /**
     * A: both {@code .min} comparator keys — the direct
     * {@code quantitySchedule -> unit -> currency} chain AND the
     * {@code quantityReference}-indirected one — append ONLY the terminal
     * {@code .<String>map("Type coercion", … .getValue())} step; every interior
     * byte (witnesses, mid-chain coercions, lambda vars) was already golden (drr).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void interestRateLeg1CrossCurrencyDrr_terminalMetaUnwrapInMinKeys_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/InterestRateLeg1CrossCurrency.java");
    }

    /** A: the {@code .max}-side sibling pins the same terminal unwrap (drr). */
    @Test
    @EnabledIf("drrCellAvailable")
    void interestRateLeg2CrossCurrencyDrr_terminalMetaUnwrapInMaxKeys_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/InterestRateLeg2CrossCurrency.java");
    }

    /**
     * C: {@code max [ timestamp -> date ]} — the {@code date} record feature of
     * the {@code zonedDateTime}-typed head renders the upstream
     * {@code RecordJavaUtil} form
     * {@code .<Date>map("Date", zdt -> Date.of(zdt.toLocalDate()))} (with the
     * {@code Date} import) instead of the invented non-compiling
     * {@code .map("getDate", …)} getter, in BOTH conditional branches (drr).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getValuationDrr_recordFeatureLeafInMaxKey_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/GetValuation.java");
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
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " — the max/min comparator-key terminal meta-unwrap (arm A), "
                + "the list-literal receiver element join (arm B), or the record-feature "
                + "leaf synthesis (arm C) is missing or regressed, if the "
                + "maxmin_body_coercion recovery is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
