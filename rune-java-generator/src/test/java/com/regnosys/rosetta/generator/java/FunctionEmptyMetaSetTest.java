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
 * PR #193 (facet {@code emptyMetaSet} — the implicit-empty else of a conditional whole-output
 * SET whose output is a SINGLE-cardinality META wrapper, the single-card SET-position sibling
 * of PR #192's {@code emptyMultiArg}).
 *
 * <p>The law: a {@code set <output>: if <cond> then <value>} with NO explicit {@code else},
 * whose output is a single-cardinality META-annotated attribute ({@code [metadata address]} →
 * {@code ReferenceWithMetaX} / {@code [metadata location|scheme]} → {@code FieldWithMetaX}),
 * coerces the absent else into the EMPTY meta wrapper {@code toBuilder(<Wrapper>.builder()
 * .build())} — exactly upstream's {@code empty(JavaType)} for an {@code RJavaWithMetaValue}
 * expected type ({@code TypeCoercionService.xtend} L389-390; the toBuilder wrap is the same
 * whole-output {@code wrapArmsToBuilder} the then-arm already applies). The fork emitted bare
 * {@code <output> = null;} (the single-card non-meta form, which stays {@code null}; the MULTI
 * non-meta form is PR #181's arm C6 {@code Collections.<T>emptyList()}).
 *
 * <p>Example ({@code MapAssetToObservableWithAddress}, output {@code observable Observable
 * (0..1) [metadata address]} → {@code ReferenceWithMetaObservable}):
 * <pre>
 *   if (exists(MapperS.of(fpmlAsset)).getOrDefault(false)) {
 *       observable = toBuilder(createObservableWithAddress.evaluate(...));
 *   } else {
 *       observable = toBuilder(ReferenceWithMetaObservable.builder().build());   // golden
 *       observable = null;                                                        // pre-fix gen
 *   }
 * </pre>
 * Because the fork rendered {@code null} for EVERY absent single-card meta else while the golden
 * always renders the empty meta builder, every such file was already a mismatch (waivered); no
 * green file can carry the pre-fix form, so the rewrite touches only waivered files (green-safe
 * by construction).
 *
 * <p>Wiring (revert-locked by these whole-file anchors): the final {@code else} arm of
 * {@link com.regnosys.rosetta.generator.java.function.FunctionExpressionRenderer}'s
 * {@code renderConditionalAssignment} gains a SINGLE-card meta arm BEFORE the {@code null}
 * fallback, driven by a new {@code singleMetaOutputEmptyElseWrapper(operation)} helper (the
 * sibling of {@code multiOutputEmptyElseItem}): it fires when the whole-output SET's single-card
 * output is meta-annotated, resolving the wrapper via {@code RJavaWithMetaValue.create} +
 * {@code JavaTypeTranslator.toJavaReferenceType} and emitting
 * {@code toBuilder(<Wrapper>.builder().build())} (the wrapper import rides the refs channel).
 *
 * <p>This is the revert-verified lock: it generates through
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} (the REAL D11 loader) and byte-compares
 * WHOLE FILES against the frozen goldens (newline-normalized — fragment assertions are
 * insufficient per the PR #153 lesson).
 *
 * <p>Anchored (all cdm6 ingest-fpml pricequantity):
 * <ul>
 *   <li>{@code MapAssetToObservableWithAddress} — a {@code [metadata address]} REFERENCE wrapper
 *       ({@code ReferenceWithMetaObservable}).</li>
 *   <li>{@code MapCommodityFixedLegToPriceWithAddress} — REFERENCE wrapper
 *       ({@code ReferenceWithMetaPriceSchedule}).</li>
 *   <li>{@code MapCommodityFixedLegToPriceWithLocation} — the {@code [metadata location]} FIELD
 *       wrapper of the SAME value type ({@code FieldWithMetaPriceSchedule}), proving the
 *       address→Reference / location→Field correspondence.</li>
 *   <li>{@code MapEquityDerivativeBaseQuantityListWithAddress} — a {@code List}-NAMED but
 *       single-cardinality output ({@code ReferenceWithMetaNonNegativeQuantitySchedule}),
 *       proving the SINGLE arm fires (not the MULTI {@code emptyList()} arm).</li>
 * </ul>
 */
class FunctionEmptyMetaSetTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
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

    // ---- implicit-empty else of a single-card meta-output conditional SET ----

    /** REFERENCE wrapper ({@code ReferenceWithMetaObservable}). */
    @Test
    @EnabledIf("cellsAvailable")
    void mapAssetToObservableWithAddress_cdm6_emptyMetaReferenceSet_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/pricequantity/functions/MapAssetToObservableWithAddress.java");
    }

    /** REFERENCE wrapper of a Price schedule ({@code ReferenceWithMetaPriceSchedule}). */
    @Test
    @EnabledIf("cellsAvailable")
    void mapCommodityFixedLegToPriceWithAddress_cdm6_emptyMetaReferenceSet_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/pricequantity/functions/MapCommodityFixedLegToPriceWithAddress.java");
    }

    /** FIELD wrapper of the SAME value type — proves address→Reference / location→Field. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapCommodityFixedLegToPriceWithLocation_cdm6_emptyMetaFieldSet_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/pricequantity/functions/MapCommodityFixedLegToPriceWithLocation.java");
    }

    /** {@code List}-NAMED but single-card output — proves the SINGLE arm fires, not MULTI emptyList(). */
    @Test
    @EnabledIf("cellsAvailable")
    void mapEquityDerivativeBaseQuantityListWithAddress_cdm6_singleCardListNamed_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/pricequantity/functions/MapEquityDerivativeBaseQuantityListWithAddress.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir, String path)
            throws IOException {
        assertNotNull(output, "Function generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — the bare `<output> = null;` at the implicit-empty single-card meta else "
                + "reappears if the singleMetaOutputEmptyElseWrapper toBuilder(<Wrapper>.builder()"
                + ".build()) coercion is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
