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
 * PR #250 — deep_value_then_hoist (the VALUE-output slice of the inline-then hoist).
 * Extends PR #219 (which scoped the deep-position {@code thenArg} hoist to a
 * ComparisonResult-OUTPUT then) to a VALUE-output {@code then} (identity / map / extract /
 * mapItem / mapSingleToItem) consumed at a NESTED position (an evaluate-arg, a ctor-setter
 * arg, a {@code MapperC.of(...)} arg, a block-rendered conditional arm) where a statement-hoist
 * sink is reachable:
 *
 * <pre>
 * final MapperS&lt;FxLinkedNotionalSchedule&gt; thenArg = MapperS.of(fpmlCalculation).&lt;...&gt;map(...);
 * quantityMultiplier = toBuilder(QuantityMultiplier.builder()
 *     .setFxLinkedNotionalSchedule(thenArg.mapSingleToItem(item -&gt; ...)).build());
 * </pre>
 *
 * <p>The fork emitted the broken inline {@code <receiver>.then(item -> <body>)} (there is no
 * runtime {@code Mapper.then(Function)} method — ZERO corpus-9.83.0 goldens carry the runtime
 * {@code .then(} form anywhere, so every carrier this fires on was already a waivered, non-
 * compiling mismatch — <b>green-safe by construction</b>).
 *
 * <p><b>CASCADE GATES</b> (the VALUE-then hoist is cascade-prone — golden restructures
 * context-dependently into block-lambdas / if-else hoists / list-of-lists forms a local hoist
 * cannot replicate). A deep value-then hoists ONLY when:
 * <ol>
 *   <li>it is NOT inside an inline ternary / chained-ternary switch arm
 *       ({@code JavaStatementScope.thenValueHoistSuppression}, raised by
 *       {@code ControlFlowHandler.handle(RConditionalExpr|RSwitchExpr)});</li>
 *   <li>its chain (base + every then-body) carries NO control flow — a nested {@code then},
 *       conditional, or switch ({@code CollectionHandler.thenChainHasControlFlow});</li>
 *   <li>the ENCLOSING FUNCTION has no control-flow value-then ELSEWHERE (all-or-nothing —
 *       a partial hoist re-numbers the per-function {@code thenArg} group against golden's
 *       all-hoisted form; {@code CollectionHandler.functionHasControlFlowValueThen});</li>
 *   <li>its element type resolves (the {@code Object}-typed hoist golden never emits is
 *       declined at the chain base).</li>
 * </ol>
 *
 * <p>Whole-file byte anchors through the REAL D11 loader over the 3 clean carriers (cdm6 ×1,
 * drr ×2) + a cascade-DECLINE lock (a ternary-body carrier keeps its inline {@code .then(}).
 * REVERT-VERIFIED RED.
 */
class FunctionDeepValueThenHoistTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path CDM6_GOLDEN_DIR = CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_GOLDEN_DIR = DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;
    private static Map<String, String> drrFunctionOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR)
                && Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            cdm6FunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
            drrFunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell) throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(), D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = gen.generateWithErrors(output);
        assertTrue(genErrors.isEmpty(), cell + " FUNCTION generation reported errors: " + genErrors);
        return output;
    }

    // ---- flip carriers: deep value-then hoist makes the file byte-match golden ----

    /** cdm6: a value-then ({@code mapSingleToItem}) nested in a ctor-setter arg of a top-level SET. */
    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_mapQuantityMultiplier_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/swap/functions/MapQuantityMultiplier.java");
    }

    /** drr: two sibling value-thens ({@code mapSingleToItem}) as args to a top-level {@code MapperC.of(...)} SET. */
    @Test
    @EnabledIf("cellsAvailable")
    void drr_getCreditUnderlierLei_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/enrichment/upi/functions/GetCreditUnderlierLEI.java");
    }

    /** drr: two value-thens ({@code mapItem}) nested in top-level ADD operands. */
    @Test
    @EnabledIf("cellsAvailable")
    void drr_createSubmissionSchedules_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/dtcc/rds/harmonized/cftc/rewrite/trade/functions/Create_SubmissionSchedules.java");
    }

    // ---- restructure lock (pin MOVED at PR #368): the ctor-conditional admission ----

    /**
     * Create_ShapingInstruction's value-then body is a ctor carrying an inline conditional —
     * golden block-hoists it to a {@code mapListToItem(item -> { ifThenElseResult … })}
     * block-lambda over a {@code MapperListOfLists}. Pre-#368 the cascade gate
     * ({@code thenChainHasUnhandledControlFlow}) DECLINED it and this pin locked the inline
     * {@code .then(} decline; the PR #368 ctorCondFieldAdmit arm now ADMITS flat ctl-free
     * conditional ctor fields, so the chain RESTRUCTURES to exactly golden's form (the file
     * FLIPPED byte-identical — the byte lock lives in
     * {@code CaseNarrowReportFamilyComposeTest}). The inline {@code .then(} re-appearing
     * means the admission regressed.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_createShapingInstruction_controlFlowThenRestructures() {
        // Pin MOVED with the facet (PR #368 ctorCondFieldAdmit): the ctor-field
        // flat-conditional admission now RESTRUCTURES the chain into golden's
        // mapListToItem block-lambda (the file FLIPPED; the byte lock lives in
        // CaseNarrowReportFamilyComposeTest.cdm6Functions_byteMatchGolden). The
        // pre-#368 decline pin asserted the inline `.then(` stayed — that
        // #219/#232 cascade protection is superseded by the together-restructure
        // admission.
        assertNotNull(cdm6FunctionOutput, "Function generation did not run — corpus unavailable?");
        String generated = cdm6FunctionOutput.get("cdm/event/common/functions/Create_ShapingInstruction.java");
        assertNotNull(generated, "Function not generated: Create_ShapingInstruction.java");
        assertTrue(!generated.contains(".then("),
                "Create_ShapingInstruction's value-then carries an inline conditional (golden block-hoists "
                + "it to a mapListToItem block-lambda over a MapperListOfLists); the cascade gate must DECLINE "
                + "the deep value-then hoist, keeping the inline `arg.then(item -> …)` form. The hoist firing "
                + "here (no `.then(`) would move the co-occupied waivered file away from golden — the #219/#232 "
                + "cascade this gate prevents.");
    }

    private static void assertByteMatchesGolden(Map<String, String> cellOutput, Path goldenDir, String path)
            throws IOException {
        assertNotNull(cellOutput, "Function generation did not run — corpus unavailable?");
        String generated = cellOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — a deep-position VALUE-output then(-chain) consumed at a nested seat must hoist "
                + "`final Mapper*<X> thenArg = <arg>;` and re-root the body (deep_value_then_hoist). "
                + "Reverting the deep value-then hoist reverts this anchor to the inline "
                + "arg.then(item -> ...) form (RED).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
