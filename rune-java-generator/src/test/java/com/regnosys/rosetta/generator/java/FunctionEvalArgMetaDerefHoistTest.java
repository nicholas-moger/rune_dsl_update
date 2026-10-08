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
 * PR #237 — facet {@code evalArgMetaDerefHoist}: the #236 {@code ctorSetterMetaDerefHoist}
 * convertNullSafe meta→item deref extended from the ctor-setter seat to the EVALUATE-ARG
 * seat at NESTED-operand statement positions. A function-call argument that PROVABLY
 * produces an attribute-meta wrapper ({@code FieldWithMeta*}/{@code ReferenceWithMeta*}),
 * consumed where the callee expects the bare item, but whose enclosing call sits NESTED in
 * a statement-level expression with NO enclosing extract/map lambda — inside
 * {@code areEqual(...)} / {@code notExists(...)} / {@code ComparisonResult.ofNullSafe(...)}, a
 * conditional-branch {@code result =} SET, or a {@code return} — must hoist the wrapper to a
 * {@code final <Wrapper> <name> = <chain>;} statement local (referenced TWICE by the null
 * guard) and pass {@code (<name> == null ? null : <name>.getValue())}. The fork spliced the
 * wrapper BARE (a {@code FieldWithMetaX} where the bare item is expected — non-compiling, so
 * every carrier was already a waivered mismatch → green-safe by construction).
 *
 * <p>Two cooperating pieces (see
 * {@link com.regnosys.rosetta.generator.java.expression.handlers.ReferenceHandler}
 * {@code MetaDerefHoistRoute.STATEMENT_SINK} + {@code tryMetaDerefArg}, and
 * {@link com.regnosys.rosetta.generator.java.expression.handlers.NavigationHandler}
 * {@code tryTerminalMetaMapperType}):
 * <ol>
 *   <li>the new {@code STATEMENT_SINK} route registers the hoist decl on the nearest
 *       statement-hoist sink ({@code prependStatementHoists} lifts it ahead of the
 *       enclosing statement, the #236 ctor-setter analogue at the evaluate-arg seat);</li>
 *   <li>a meta-wrapper item-type recovery for a multi-intermediate {@code mapC} chain
 *       collapsed through a list-op ({@code ONLY_ELEMENT}/{@code extract}-map), whose
 *       compiled expression type erased to {@code null} so the deref gate would otherwise
 *       decline.</li>
 * </ol>
 *
 * <p>8 FUNCTION flips, each locked here by a WHOLE-FILE byte anchor through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} + {@link FunctionGenerator}
 * (newline-normalized, revert-RED): cdm5 {@code UnitEquals} / {@code Qualify_Increase} /
 * {@code Qualify_PartialTermination}; cdm6 {@code UnitEquals} / {@code EquityPerformance} /
 * {@code Qualify_Increase} / {@code Qualify_PartialTermination} /
 * {@code Qualify_AssetClass_InterestRate}.
 *
 * <p><b>Green-safety lock</b> — {@code Qualify_PartialNovation} (cdm6): a same-family sibling
 * that was ALREADY green before this PR. Its {@code quantityDecreased.evaluate((... == null ?
 * null : ...getValue()))} meta-deref sits in a {@code then}/reduce LAMBDA body, so it routes
 * through the existing {@code LAMBDA_CHANNEL} (not the new {@code STATEMENT_SINK}); its hoist
 * {@code final ReferenceWithMetaTradeState referenceWithMetaTradeState4 = ...} numbers {@code 4}
 * AFTER the in-chain Type-coercion params {@code 0..3} (the deferred-coercion group law). This
 * byte-lock asserts the new route + recovery leave that green lambda-channel meta-deref + its
 * numbering untouched (full green-safety is established by the 5-cell D11 byte-compare matrix
 * 20/20 + the PRE/POST within-waiver regression scan, 0 regressions).
 */
class FunctionEvalArgMetaDerefHoistTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm5FunctionOutput;
    private static Map<String, String> cdm6FunctionOutput;

    static boolean cdm5CellAvailable() {
        return cellAvailable(CDM5_CELL_ROOT, CDM5_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return cellAvailable(CDM6_CELL_ROOT, CDM6_GOLDEN_DIR);
    }

    private static boolean cellAvailable(Path root, Path golden) {
        return Files.isDirectory(root.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(golden);
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

    // ===================== cdm5 flip locks =====================

    /**
     * UnitEquals (cdm5) — a conditional-branch {@code result = stringEquals.evaluate(...)}
     * hoists BOTH meta args ({@code final FieldWithMetaString fieldWithMetaString0/1 = ...get();})
     * and passes the null-guarded {@code getValue()} derefs.
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void cdm5_unitEquals_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/base/math/functions/UnitEquals.java");
    }

    /**
     * Qualify_Increase (cdm5) — {@code quantityIncreased.evaluate(...)} nested in
     * {@code areEqual(MapperS.of(...))} hoists the wrapper at the assignOutput body top; the
     * downstream {@code lessThan} Type-coercion lambda param scope-disambiguates to
     * {@code _referenceWithMetaTradeState} (the hoist took the bare name).
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void cdm5_qualifyIncrease_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/qualification/functions/Qualify_Increase.java");
    }

    /**
     * Qualify_PartialTermination (cdm5) — {@code quantityDecreased.evaluate(...)} nested in
     * {@code areEqual(MapperS.of(...))}; a single hoist via the list-op-wrapped meta-arg
     * type recovery (the {@code instruction} mapC chain erased its expression type).
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void cdm5_qualifyPartialTermination_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/qualification/functions/Qualify_PartialTermination.java");
    }

    // ===================== cdm6 flip locks =====================

    /** UnitEquals (cdm6) — the cdm6 sibling of the cdm5 conditional-branch two-arg hoist. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void cdm6_unitEquals_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/base/math/functions/UnitEquals.java");
    }

    /**
     * EquityPerformance (cdm6) — {@code return MapperS.of(resolvePerformancePeriodStartPrice
     * .evaluate(..., (fieldWithMetaObservable == null ? null : ...), date))}; the meta arg is a
     * list-op-wrapped {@code mapC} chain recovered via {@code tryTerminalMetaMapperType}.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void cdm6_equityPerformance_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/common/functions/EquityPerformance.java");
    }

    /** Qualify_Increase (cdm6) — the cdm6 sibling (downstream {@code _}-disambiguated lambda). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void cdm6_qualifyIncrease_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/qualification/functions/Qualify_Increase.java");
    }

    /** Qualify_PartialTermination (cdm6) — the cdm6 sibling (single list-op-wrapped hoist). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void cdm6_qualifyPartialTermination_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/qualification/functions/Qualify_PartialTermination.java");
    }

    /**
     * Qualify_AssetClass_InterestRate (cdm6) — {@code observableQualification.evaluate((... ==
     * null ? null : ...getValue()), ...)} nested in {@code ComparisonResult.ofNullSafe(MapperS.of(...))};
     * a single hoist co-resident with (but independent of) the file's {@code ifThenElseResult0/1}
     * hoist group — the new base name does not renumber the existing group.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void cdm6_qualifyAssetClassInterestRate_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/product/qualification/functions/Qualify_AssetClass_InterestRate.java");
    }

    // ===================== green-safety lock =====================

    /**
     * Qualify_PartialNovation (cdm6) — green-safety: a same-family sibling already green before
     * this PR. Its {@code quantityDecreased.evaluate((referenceWithMetaTradeState4 == null ?
     * null : referenceWithMetaTradeState4.getValue()), ...)} meta-deref sits in a
     * {@code then}/reduce LAMBDA body, routing through the existing {@code LAMBDA_CHANNEL} (not
     * the new {@code STATEMENT_SINK}); the hoist numbers {@code 4} after the in-chain
     * Type-coercion params {@code 0..3}. The new route + recovery must leave it byte-identical.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void cdm6_qualifyPartialNovation_greenSafety_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/qualification/functions/Qualify_PartialNovation.java");
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
                + path + " — PR #237 evalArgMetaDerefHoist.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
