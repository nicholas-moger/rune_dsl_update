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
 * Facet {@code interior_position_coercion} — an interior chain position (a
 * navigation receiver, a {@code count} operand, a list-op argument) never
 * receives the entry-point terminal coercion, mirroring the upstream positional
 * law ({@code ExpressionGenerator} compiles interior expecteds WRAPPER-level —
 * {@code MAPPER.wrapExtendsWithoutMeta} at L774/785, re-coerced wrapper-level
 * at attributeCall L355/385, the seven non-collapsing list ops + count likewise;
 * the lone item-level interior expected, {@code caseOnlyElementOperation}
 * L998-1001, is upstream's collapse leaf, which the fork's only-element handler
 * collapses string-level itself — bare-item coercion otherwise happens only at
 * genuine value-consumption leaves).
 *
 * <p>Pre-facet, {@code ComparisonHandler.inferNumericType}'s ITEM-level
 * {@code Integer} expected (count-comparison operands) leaked through
 * {@code CollectionHandler}'s count/list-op argument compiles and
 * {@code NavigationHandler}'s receiver compiles into MID-CHAIN meta steps —
 * the only positions whose sub-results carry expression types
 * ({@code metaNavResultType}) — where {@code ExpressionCompiler.compile}'s
 * blanket terminal coercion collapsed the wrapper to the non-compiling
 * {@code .get().getValue().&lt;T&gt;map(...)} / {@code .getValue().resultCount()}
 * deref. With the leak closed ({@code compileInterior} at the four interior
 * call sites), the EXISTING {@code coerceNavigationReceiver} gate fires at the
 * next step and emits the golden inline {@code map("Type coercion", ...)}
 * step; the guard kind and naming ride the #170 law unchanged — guarded
 * REGISTERED param iff the chain rides {@code MapperS}, bare un-registered
 * param iff {@code MapperC}. The {@code MapperC} kind for ALIAS-headed chains
 * (plain and disguised-{@code REnumValueRef} heads) reads from the SAME
 * {@code FunctionAliasHelper} walk that renders the alias method's
 * {@code MapperS/MapperC} signature into the same file
 * ({@code inferShortcutIsMulti}), so the kind read and the rendered wrapper
 * cannot disagree; the arm is MONOTONE (only a provably-multi alias widens to
 * {@code MapperC}).
 *
 * <p>Green-safety: the suppressed emission is non-compiling Java and ZERO
 * goldens carry it (plain-grep over the frozen corpus-baseline-9.83, all
 * 34,686 goldens: 0 occurrences of {@code getValue().&lt;} and
 * {@code getValue().resultCount()}); the alias-kind arm cannot move a green
 * file because the alias signature rendered from the same walk is IN the same
 * file's bytes. The full 5-cell D11 matrix (20/20) is the empirical arbiter.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class FunctionInteriorPositionCoercionTest {

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
     * Spurious TERMINAL deref drop (sub-shape B): a count-comparison operand's
     * chain ends at a meta step; golden counts the WRAPPER items —
     * {@code ...getFloatingRateIndex()).resultCount()} with NO unwrap — where
     * the leaked item-Integer expected appended {@code .get().getValue()}.
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void qualifyCancellationCdm5_terminalDerefDrop_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/common/functions/Qualify_Cancellation.java");
    }

    /** Terminal deref drop, cdm/6.20.6 sibling of the anchor above. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyCancellationCdm6_terminalDerefDrop_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/common/functions/Qualify_Cancellation.java");
    }

    /** Terminal deref drop in an assignment-RHS areEqual count comparison. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void qualifyOnDemandPaymentCdm5_terminalDerefDrop_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/common/functions/Qualify_OnDemandPayment.java");
    }

    /** Terminal deref drop in an orNullSafe operand count comparison. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void qualifyAssetClassCommodityCdm5_terminalDerefDrop_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/product/qualification/functions/Qualify_AssetClass_Commodity.java");
    }

    /**
     * BOTH sub-shapes in one file: a mid-chain meta step inside
     * {@code distinct(...)} gains the BARE un-registered inline coercion (the
     * chain rides {@code MapperC}) AND the terminal step before
     * {@code ).resultCount()} keeps its wrapper.
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void qualifyBaseProductCrossCurrencyCdm5_midChainAndTerminal_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/product/qualification/functions/Qualify_BaseProduct_CrossCurrency.java");
    }

    /** Mid-chain + terminal sub-shapes, cdm/6.20.6 sibling. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyBaseProductCrossCurrencyCdm6_midChainAndTerminal_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/product/qualification/functions/Qualify_BaseProduct_CrossCurrency.java");
    }

    /**
     * GUARDED MapperS-context inline variant: the coercion that now fires rides
     * a {@code MapperS} chain, so the param is guarded, REGISTERED, and numbers
     * with the file's existing same-name coercion group (the #170/#177
     * name-group machinery, untouched by this facet).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void standardizedScheduleMonetaryNotionalCdm6_guardedVariantAndRenumber_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/margin/schedule/functions/StandardizedScheduleMonetaryNotionalFromResolvablePQ.java");
    }

    /**
     * GUARDED variant inside a {@code filterItemNullSafe} LAMBDA interior: the
     * item chain rides {@code MapperS}, so the newly-firing mid-chain coercion
     * is the null-guarded ternary — proving the fix is guard-POLYMORPHIC on the
     * wrapper kind, not a hardcoded bare form.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void commodityBasisLegWithNoSpreadDrr_guardedLambdaInterior_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/CommodityBasisLegWithNoSpread.java");
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
                + path + " — an interior chain position received the terminal "
                + "entry coercion (the mid-chain .get().getValue(). deref) or the "
                + "alias-headed chain's MapperC kind regressed, if the "
                + "interior_position_coercion recovery is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
