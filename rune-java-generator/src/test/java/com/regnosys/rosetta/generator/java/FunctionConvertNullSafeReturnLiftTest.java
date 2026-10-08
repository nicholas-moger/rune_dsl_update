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
 * PR #188 (facet {@code convertNullSafe}, slice 2 — the B meta→value arg-hoist
 * lifted at the ALIAS-BODY / RETURN seat) — the continuation of PR #187's
 * top-level convertNullSafe slice into the alias (shortcut) helper-method seat.
 *
 * <p>An alias whose body is a direct FUNCTION call passing a value-level meta
 * wrapper ({@link com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue}
 * {@code FieldWithMetaX} / {@code ReferenceWithMetaX}) where the callee parameter
 * expects the plain value now hoists the wrapper into a {@code final <Meta> name =
 * …;} local BEFORE the {@code return}, and consumes it via
 * {@code (name == null ? null : name.getValue())} — exactly the upstream
 * {@code TypeCoercionService.convertNullSafe} statement form. Pre-fix the fork
 * emitted the bare wrapper straight into the {@code evaluate(…)} argument (a
 * non-compiling type mismatch) inside the inline {@code return MapperS.of(…)} body.
 *
 * <p>Wiring (all three pieces revert-locked by these whole-file anchors):
 * <ul>
 *   <li>{@code ReferenceHandler.metaDerefHoistRoute} gains an
 *       {@code RShortcut}-parent arm beside the PR #143 {@code RRule} and PR #187
 *       {@code ROperation(SET, no-segment)} arms — a direct top-level call that IS
 *       an alias body routes to {@code BLOCK}, so the existing PR #143/#170
 *       {@code tryMetaDerefArg} machinery hoists the meta arg;</li>
 *   <li>{@code FunctionExpressionRenderer.renderAliasLiftedReturnOrNull} lifts the
 *       resulting {@code JavaBlockBuilder} (leading {@code final … = …;} decl(s) +
 *       trailing {@code MapperS.of(callee.evaluate(…))} value) to the statement form
 *       {@code final … = …; return <trailing>;}, via the existing
 *       {@code liftThenBodyHoists} helper (the SET-path analogue);</li>
 *   <li>{@code FunctionGenerator.compileAliases} calls it after the PR #183
 *       {@code renderAliasReturnLadderOrNull} check and before the plain
 *       {@code renderAlias} fallback.</li>
 * </ul>
 *
 * <p>This is the revert-verified lock: it generates through
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} (the REAL D11 loader, not a
 * mirrored copy) and byte-compares WHOLE FILES against the frozen goldens
 * (newline-normalized — fragment assertions are insufficient per the PR #153
 * lesson). Reverting any of the three pieces turns these anchors RED with the bare
 * (non-compiling) inline-arg shape. {@code EvaluateScreenRate} is the pure B-return
 * hoist ({@code ReferenceWithMetaInterestRateIndex} / {@code …FloatingRateOption});
 * {@code CalculateFloatingCashFlow} and {@code FixedAmountCalculation} are the joint
 * cases whose A {@code .setCurrency} wrap already shipped at PR #187, so they flip
 * the moment the B-return lift lands ({@code FieldWithMetaDayCountFractionEnum} hoist,
 * additionally dropping a now-unused bare-enum {@code DayCountFractionEnum} import).
 * All THREE functions are anchored in both cells (cdm 5.38.0 + cdm 6.20.6) — six
 * whole-file anchors.
 *
 * <p>Remaining convertNullSafe positions (conditional-arm SET, whole-RHS block,
 * then-path; A multi-cardinality singletonList) stay characterized in
 * {@code target/wf187-reversal.json} for follow-on slices.
 */
class FunctionConvertNullSafeReturnLiftTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");

    private static Map<String, String> cdm5Output;
    private static Map<String, String> cdm6Output;

    static boolean cellsAvailable() {
        return goldenDir(CDM5_CELL_ROOT) != null && goldenDir(CDM6_CELL_ROOT) != null;
    }

    private static Path goldenDir(Path cellRoot) {
        Path golden = cellRoot.resolve("rosetta-source/src/generated/java");
        Path source = cellRoot.resolve("rosetta-source/src/main/rosetta");
        return (Files.isDirectory(golden) && Files.isDirectory(source)) ? golden : null;
    }

    @BeforeAll
    static void generate() throws IOException {
        if (!cellsAvailable()) return;
        cdm5Output = generateCell("cdm", "5.38.0", CDM5_CELL_ROOT);
        cdm6Output = generateCell("cdm", "6.20.6", CDM6_CELL_ROOT);
    }

    private static Map<String, String> generateCell(String corpus, String version, Path cellRoot)
            throws IOException {
        var cell = new D11CorpusRegressionTest.CellSpec(corpus, version, cellRoot);
        var c = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(c.workspace(), D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> out = new LinkedHashMap<>();
        List<GenerationException> errors = gen.generateWithErrors(out);
        assertTrue(errors.isEmpty(), corpus + " " + version + " FUNCTION generation errors: " + errors);
        return out;
    }

    // ---- B-return: pure alias-body meta-arg hoist (no co-occupation) ----------

    /** B-return — ReferenceWithMetaInterestRateIndex hoisted before the alias-body return (cdm6). */
    @Test
    @EnabledIf("cellsAvailable")
    void evaluateScreenRate_cdm6_returnLift_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_CELL_ROOT,
                "cdm/product/asset/floatingrate/functions/EvaluateScreenRate.java");
    }

    /** B-return — the cdm5 sibling (ReferenceWithMetaFloatingRateOption). */
    @Test
    @EnabledIf("cellsAvailable")
    void evaluateScreenRate_cdm5_returnLift_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5Output, CDM5_CELL_ROOT,
                "cdm/product/asset/floatingrate/functions/EvaluateScreenRate.java");
    }

    // ---- joint-AB: B-return lift + already-shipped A .setCurrency + import drop --

    /** joint-AB — FieldWithMetaDayCountFractionEnum hoist + unused-import drop (cdm6). */
    @Test
    @EnabledIf("cellsAvailable")
    void calculateFloatingCashFlow_cdm6_returnLift_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_CELL_ROOT,
                "cdm/product/asset/calculation/functions/CalculateFloatingCashFlow.java");
    }

    /** joint-AB — the cdm5 sibling. */
    @Test
    @EnabledIf("cellsAvailable")
    void calculateFloatingCashFlow_cdm5_returnLift_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5Output, CDM5_CELL_ROOT,
                "cdm/product/asset/calculation/functions/CalculateFloatingCashFlow.java");
    }

    /** joint-AB — same dcf-hoist + DayCountFractionEnum import drop as CalculateFloatingCashFlow (cdm6). */
    @Test
    @EnabledIf("cellsAvailable")
    void fixedAmountCalculation_cdm6_returnLift_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_CELL_ROOT,
                "cdm/product/asset/calculation/functions/FixedAmountCalculation.java");
    }

    /** joint-AB — the cdm5 sibling. */
    @Test
    @EnabledIf("cellsAvailable")
    void fixedAmountCalculation_cdm5_returnLift_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5Output, CDM5_CELL_ROOT,
                "cdm/product/asset/calculation/functions/FixedAmountCalculation.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> output, Path cellRoot, String path)
            throws IOException {
        assertNotNull(output, "Function generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = cellRoot.resolve("rosetta-source/src/generated/java").resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — the bare inline meta-arg shape reappears if the alias-body convertNullSafe "
                + "return-lift (metaDerefHoistRoute RShortcut arm + renderAliasLiftedReturnOrNull) is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
