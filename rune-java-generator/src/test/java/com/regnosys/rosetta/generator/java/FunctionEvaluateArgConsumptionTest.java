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
 * Facet {@code evaluate_arg_consumption} — upstream compiles every explicit
 * function-call argument against the CALLEE PARAMETER's expected Java type
 * (the {@code ExpressionGenerator} universal expected-type coercion at the arg
 * site), so a multi parameter takes the {@code List} arity and a meta-ending
 * chain coerces META-FREE first — bare iff MapperC, item-level null-guarded
 * hoist iff MapperS (the facet meta_coercion_numbering wrapper-kind law applied
 * at the evaluate-arg consumption site). The #168-deferred
 * {@code evaluateArgIsMulti} overlay, unblocked by PR #170's scope numbering.
 *
 * <p>Three fork arms ({@code ReferenceHandler} arg loop):
 *
 * <ol>
 *   <li><b>B1 — whole-chain multi proof.</b> {@code evaluateArgIsMulti}'s two
 *       probes (the resolution-blind workspace computer + the LEAF-feature
 *       {@code navLeafFeatureMulti}) both read SINGLE for a chain that is multi
 *       via an INTERIOR {@code mapC} step with a single leaf
 *       ({@code businessEvent -> after -> transferHistory -> transfer} — the
 *       leaf {@code transfer} is {@code (0..1)}). The arm adds the SAME
 *       gm-aware receiver-propagating walk the renderer's {@code mapItem}
 *       selection trusts ({@code NavigationHandler.chainProvesMulti}), so a
 *       multi nav-arg into a multi parameter unwraps {@code .getMulti()}
 *       instead of the non-compiling scalar {@code .get()}.</li>
 *   <li><b>B2 — mapper-level meta coercion for a MULTI arg.</b> A MapperC arg
 *       chain ending meta-item-typed, passed to a multi parameter whose
 *       declared attribute is meta-FREE, appends the BARE Type-coercion map
 *       before {@code .getMulti()} — the EXISTING
 *       {@code ExpressionCompiler.coerceNavigationReceiver} lever (bare iff
 *       MapperC), the same gate arms A4/comparison-operands already fire
 *       through. Pre-fix render passes
 *       {@code List<ReferenceWithMetaTradeState>} where
 *       {@code List<? extends TradeState>} is expected (NON-COMPILING).</li>
 *   <li><b>B3 — item-level hoist for a SINGLE meta arg.</b> A MapperS alias
 *       arg ending meta-typed into a value-typed single parameter hoists
 *       {@code final <Meta> <var> = <alias>(<args>).get();} and passes the
 *       null-guarded {@code (<var> == null ? null : <var>.getValue())} — the
 *       EXISTING {@code tryMetaDerefArg} machinery, extended two ways: the
 *       arg's item type is recovered from the SAME
 *       {@code FunctionAliasHelper} walk that renders the alias method
 *       signature (PR #170 arm A1's typing channel — the alias-call rendering
 *       carries a null expression type), and the hoisted local registers
 *       through the PR #170 deferred-naming machinery
 *       ({@code registerDeferredCoercionName}, the identifier-sharing variant
 *       — one {@code GeneratedIdentifier} shared by the decl statement and
 *       the sentinel-rendered references) so it NUMBERS with the
 *       statement's same-base guarded coercion params
 *       ({@code referenceWithMetaTradeState4} after the in-chain {@code 0..3})
 *       instead of the pre-#170 disambiguate.</li>
 * </ol>
 *
 * <p>Green-safety: every firing shape renders non-compiling Java pre-fix (a
 * scalar {@code T} / {@code List<MetaWrapper>} / raw wrapper where the callee's
 * declared parameter type differs), so no byte-matching file carries one; the
 * B3 naming change resolves single-member groups to the IDENTICAL
 * escaped-iff-taken outcome the old disambiguate produced, and a green file
 * cannot carry an UNNUMBERED hoist sharing a scope with same-base registered
 * params (golden always numbers such groups).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}:
 * <ul>
 *   <li>cdm/5.38.0 + cdm/6.20.6 {@code Qualify_SecuritySettlement} — B1 alone
 *       (the {@code transfers} alias method's first evaluate arg);</li>
 *   <li>cdm/5.38.0 {@code Qualify_Renegotiation} + cdm/6.20.6
 *       {@code Qualify_Termination} — B1+B2 (the
 *       {@code QuantityDecreasedToZero} before-arg meta chain + the
 *       {@code transfers} tail);</li>
 *   <li>cdm/5.38.0 {@code Qualify_FullReturn} — B1+B2 (the cdm6 sibling flipped
 *       at PR #343 via deepPathLambdaNaming — see DeepPathSortThenComposeTest);</li>
 *   <li>cdm/5.38.0 {@code Qualify_PartialNovation} + cdm/6.20.6
 *       {@code Qualify_Reallocation} — B3 + facet void_witness_bare_enum's D1
 *       closure-param witness ({@code MapperC.<TradeState>of(item)}) — the
 *       joint carriers.</li>
 * </ul>
 */
class FunctionEvaluateArgConsumptionTest {

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

    /** B1 alone: interior-mapC multi chain into a multi param unwraps {@code .getMulti()} (cdm5). */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void qualifySecuritySettlementCdm5_multiArgGetMulti_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/qualification/functions/Qualify_SecuritySettlement.java");
    }

    /** B1 alone: the identical shape in the cdm6 cell (free version-pair pin). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifySecuritySettlementCdm6_multiArgGetMulti_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/qualification/functions/Qualify_SecuritySettlement.java");
    }

    /** B1+B2: bare Type-coercion before {@code .getMulti()} on the meta-ending multi arg (cdm5). */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void qualifyRenegotiationCdm5_multiMetaArgBareCoercion_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/qualification/functions/Qualify_Renegotiation.java");
    }

    /** B1+B2: the same arg shapes pinned in the cdm6 cell. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyTerminationCdm6_multiMetaArgBareCoercion_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/qualification/functions/Qualify_Termination.java");
    }

    /** B1+B2: the third two-site carrier (cdm5; the cdm6 sibling is M7b-4-blocked). */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void qualifyFullReturnCdm5_multiMetaArgBareCoercion_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/qualification/functions/Qualify_FullReturn.java");
    }

    /** B3 + D1 joint: alias-arg hoist numbered {@code 4} + {@code <TradeState>} item witness (cdm5). */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void qualifyPartialNovationCdm5_singleMetaArgHoistNumbered_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/qualification/functions/Qualify_PartialNovation.java");
    }

    /** B3 + D1 joint: the same shape pinned in the cdm6 cell via the sibling function. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyReallocationCdm6_singleMetaArgHoistNumbered_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/qualification/functions/Qualify_Reallocation.java");
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
                + path + " — the whole-chain multi-arg .getMulti() unwrap (B1), the "
                + "mapper-level bare meta coercion on a multi evaluate-arg (B2), or the "
                + "single-meta alias-arg hoist typing/deferred numbering (B3) is missing "
                + "or regressed, if the evaluate_arg_consumption recovery is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
