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
 * PR #187 (facet {@code convertNullSafe}, first slice) — the upstream
 * {@code TypeCoercionService.convertNullSafe} meta-value coercion at its two
 * top-level statement positions, both wired through ONE coercer law:
 *
 * <ul>
 *   <li><b>A — value→meta WRAP</b> (a bare value SET on a value-level meta leaf,
 *       {@link com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue}
 *       {@code FieldWithMetaX} / {@code ReferenceWithMetaX}): the fork now emits the
 *       null-safe builder ternary
 *       {@code (v == null ? Meta.builder().build() : Meta.builder().setValue(v).build())}
 *       (renderSetBuilderChain leaf), where it previously assigned the bare value
 *       straight into the meta-typed setter (a non-compiling type mismatch);</li>
 *   <li><b>B — meta→value UNWRAP at argument position</b> (a direct top-level
 *       FUNCTION-body call in a no-segment SET operation whose meta args need the
 *       value type): each meta arg now hoists to a {@code final <Meta> name = …;}
 *       local and consumes via {@code (name == null ? null : name.getValue())} —
 *       the existing PR #143/#170 {@code tryMetaDerefArg} machinery, newly reachable
 *       through the {@code metaDerefHoistRoute} {@code ROperation(SET, no-segment)}
 *       gate beside the existing {@code RRule} rule-body arm.</li>
 * </ul>
 *
 * <p>This is the revert-verified lock for that change: it generates through
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} (the REAL D11 loader, not a
 * mirrored copy) and byte-compares WHOLE FILES against the frozen goldens
 * (newline-normalized — fragment assertions are insufficient per the PR #153
 * lesson). Reverting either the renderSetBuilderChain meta-leaf wrap (A) or the
 * metaDerefHoistRoute ROperation arm (B) turns the relevant anchors RED with the
 * bare (non-compiling) shape. Anchors span both directions and all three populated
 * cells; the meta KINDS split by direction this slice — the A wrap is anchored on a
 * FIELD wrapper ({@code FieldWithMetaString}, the only A flip), while the B unwrap
 * covers both FIELD ({@code FieldWithMetaString}) and REFERENCE
 * ({@code ReferenceWithMetaParty}). The REFERENCE_WITH_META A-wrap path is exercised
 * at the unit level by the shared {@code RJavaWithMetaValue.create} call.
 *
 * <p>Remaining convertNullSafe positions (conditional-arm SET, return / alias body,
 * whole-RHS block, then-path; A multi-cardinality singletonList) are characterized
 * in {@code target/wf187-reversal.json} for follow-on slices.
 */
class FunctionMetaConvertNullSafeTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");

    private static Map<String, String> cdm5Output;
    private static Map<String, String> cdm6Output;
    private static Map<String, String> drrOutput;

    static boolean cellsAvailable() {
        return goldenDir(CDM5_CELL_ROOT) != null
                && goldenDir(CDM6_CELL_ROOT) != null
                && goldenDir(DRR_CELL_ROOT) != null;
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
        drrOutput = generateCell("drr", "6.34.1", DRR_CELL_ROOT);
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

    // ---- A (value→meta setter wrap) ---------------------------------------

    /** A — FIELD_WITH_META single-cardinality {@code .setCurrency} wrap (FieldWithMetaString). */
    @Test
    @EnabledIf("cellsAvailable")
    void postedCreditSupportItemAmount_metaSetterWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_CELL_ROOT,
                "cdm/legaldocumentation/csa/functions/PostedCreditSupportItemAmount.java");
    }

    // ---- B (meta→value arg-hoist, top-level no-segment SET) ----------------

    /** B — single meta arg ({@code currency(…).get()}) hoisted into a function-call arg (cdm5). */
    @Test
    @EnabledIf("cellsAvailable")
    void applyFloatingRateSetting_cdm5_metaArgHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5Output, CDM5_CELL_ROOT,
                "cdm/product/asset/calculation/functions/ApplyFloatingRateSetting.java");
    }

    /** B — the cdm6 sibling (same single-meta-arg hoist shape). */
    @Test
    @EnabledIf("cellsAvailable")
    void applyFloatingRateSetting_cdm6_metaArgHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_CELL_ROOT,
                "cdm/product/asset/calculation/functions/ApplyFloatingRateSetting.java");
    }

    /** B — drr reference-meta arg hoist (ReferenceWithMetaParty → value). */
    @Test
    @EnabledIf("cellsAvailable")
    void extractOtherPaymentPayer_metaArgHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_CELL_ROOT,
                "drr/regulation/common/functions/ExtractOtherPaymentPayer.java");
    }

    /** B — the receiver-side drr sibling (same reference-meta arg hoist). */
    @Test
    @EnabledIf("cellsAvailable")
    void extractOtherPaymentReceiver_metaArgHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_CELL_ROOT,
                "drr/regulation/common/functions/ExtractOtherPaymentReceiver.java");
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
                + " — the bare meta-coercion shape reappears if the convertNullSafe wrap (A) "
                + "or the meta-arg-hoist gate (B) is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
