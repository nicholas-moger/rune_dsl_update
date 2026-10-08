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
 * PR #186 (facet {@code metaWit}) — a function OUTPUT attribute declared with
 * {@code [metadata reference|address]} ({@code → ReferenceWithMetaX}) or
 * {@code [metadata scheme|id|location]} ({@code → FieldWithMetaX}) must carry the
 * concrete per-namespace meta wrapper into the OUTPUT type, exactly as the upstream
 * rune-dsl generator does.
 *
 * <p>Before the fix the fork erased the wrapper to the bare value type {@code X} across
 * every output-type site driven by {@code FunctionTemplateModel.output.typeName} — the
 * public {@code evaluate} return, the {@code …Builder} locals, the
 * {@code .stream().map(X::build)}, the {@code objectValidator.validate(X.class, …)}, the
 * abstract + Default {@code doEvaluate}, the {@code assignOutput} signature, and the
 * output import — while the BODY witnesses (input-driven) were already correct. The fix
 * makes {@code FunctionGenerator.resolveParam} (OUTPUT path only) build {@code typeName}
 * / {@code typeFqn} from {@code RJavaWithMetaValue.create(...)} when
 * {@code MetaFieldGenerator.detectMetaKind(output) != NONE} — the CONCRETE
 * {@code metafields.ReferenceWithMetaX} / {@code metafields.FieldWithMetaX} (not the
 * generic {@code FieldWithMeta<T>} of {@code JavaTypeTranslator.toMetaJavaType}). All
 * output-type sites cascade from {@code typeName}/{@code typeFqn}, so one resolveParam
 * change flips every site at once.
 *
 * <p>This is the revert-verified lock for that generator change: it generates through
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} (the REAL D11 loader, not a
 * mirrored copy) and byte-compares WHOLE FILES against the frozen cdm/6.20.6 goldens
 * (newline-normalized — fragment assertions are insufficient per the PR #153 lesson).
 * Reverting the resolveParam OUTPUT-meta arm turns every anchor RED with the bare-{@code X}
 * erased shape. Anchors span both meta kinds and both cardinalities:
 * <ul>
 *   <li>{@code MapContractualParty} — REFERENCE_WITH_META, LIST output
 *       ({@code List<? extends ReferenceWithMetaParty>}), CDM-only I/O, body-clean;</li>
 *   <li>{@code MapCommodityForwardAccountPartyReference} /
 *       {@code MapSwapAccountPartyReference} — REFERENCE_WITH_META, SINGLE output
 *       ({@code ReferenceWithMetaParty});</li>
 *   <li>{@code MapCurrencyToObservableCashWithLocation} — FIELD_WITH_META, SINGLE output
 *       ({@code observable.asset.metafields.FieldWithMetaObservable});</li>
 *   <li>{@code MapNotionalAmountToQuantityWithLocation} — FIELD_WITH_META, SINGLE output
 *       ({@code FieldWithMetaNonNegativeQuantitySchedule}, a distinct value namespace);</li>
 *   <li>{@code MapCommodityClassificationListToObservableCommodityWithLocation} —
 *       FIELD_WITH_META, SINGLE output with a LIST input;</li>
 *   <li>{@code MapCurrency} — FIELD_WITH_META, BUILTIN value {@code String} →
 *       {@code FieldWithMetaString} (the {@code outputNeedsBuilder} false→true case);</li>
 *   <li>{@code MapCurrencyReference} — REFERENCE_WITH_META, BUILTIN value {@code String} →
 *       {@code ReferenceWithMetaString} (the reference-side builtin sibling).</li>
 * </ul>
 */
class FunctionMetaWitOutputTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> functionOutput;

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generateCdm6Functions() throws IOException {
        if (!cdm6CellAvailable()) return;
        var cell = new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT);
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        functionOutput = new LinkedHashMap<>();
        List<GenerationException> genErrors = gen.generateWithErrors(functionOutput);
        assertTrue(genErrors.isEmpty(),
                "cdm6 FUNCTION generation reported errors: " + genErrors);
    }

    /** REFERENCE_WITH_META, LIST output — the exemplar; CDM-only I/O, body-clean. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapContractualParty_referenceListOutput_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "cdm/ingest/fpml/confirmation/legal/functions/MapContractualParty.java");
    }

    /** REFERENCE_WITH_META, SINGLE output ({@code ReferenceWithMetaParty}). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapCommodityForwardAccountPartyReference_referenceSingleOutput_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "cdm/ingest/fpml/confirmation/product/commodityforward/functions/MapCommodityForwardAccountPartyReference.java");
    }

    /** REFERENCE_WITH_META, SINGLE output — second single-reference carrier. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapSwapAccountPartyReference_referenceSingleOutput_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "cdm/ingest/fpml/confirmation/product/swap/functions/MapSwapAccountPartyReference.java");
    }

    /** FIELD_WITH_META, SINGLE output ({@code observable.asset.metafields.FieldWithMetaObservable}). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapCurrencyToObservableCashWithLocation_fieldSingleOutput_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "cdm/ingest/fpml/confirmation/pricequantity/functions/MapCurrencyToObservableCashWithLocation.java");
    }

    /** FIELD_WITH_META, SINGLE output — distinct value namespace ({@code FieldWithMetaNonNegativeQuantitySchedule}). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapNotionalAmountToQuantityWithLocation_fieldSingleOutput_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "cdm/ingest/fpml/confirmation/pricequantity/functions/MapNotionalAmountToQuantityWithLocation.java");
    }

    /** FIELD_WITH_META, SINGLE output with a LIST input. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapCommodityClassificationListToObservableCommodityWithLocation_fieldSingleOutput_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "cdm/ingest/fpml/confirmation/pricequantity/functions/MapCommodityClassificationListToObservableCommodityWithLocation.java");
    }

    /**
     * FIELD_WITH_META, BUILTIN value type {@code String} → {@code FieldWithMetaString}.
     * The load-bearing builtin case: because {@code String} is NOT a {@code RosettaModelObject},
     * the bare-erased form had {@code outputNeedsBuilder=false} (non-builder shape); wrapping
     * flips it true and restores the full builder/validator/{@code Optional.prune} form. This
     * anchors the {@code resolveParam} model-ness probe on the EFFECTIVE (wrapped) type.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapCurrency_fieldWithMetaStringBuiltinValue_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "cdm/ingest/fpml/confirmation/common/functions/MapCurrency.java");
    }

    /**
     * REFERENCE_WITH_META, BUILTIN value type {@code String} → {@code ReferenceWithMetaString}.
     * The REFERENCE-side builtin sibling of {@code MapCurrency} — same {@code outputNeedsBuilder}
     * false→true transition, the reference wrapper kind.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapCurrencyReference_referenceWithMetaStringBuiltinValue_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "cdm/ingest/fpml/confirmation/common/functions/MapCurrencyReference.java");
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(functionOutput, "Function generation did not run — corpus unavailable?");
        String generated = functionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = CDM6_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the cdm/6.20.6 golden (newline-normalized) "
                + "for " + path + " — the bare-X erased output-meta shape reappears if the "
                + "resolveParam OUTPUT-meta-wrapper arm is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
