package com.regnosys.rosetta.generator.java.function;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Engine PR (facet {@code bare_enum_comparison_operand}) — a RENDERER-ONLY fix that resolves a
 * BARE enum value used as a COMPARISON operand ({@code <nav> = Clearing}) to its qualified Java
 * constant ({@code MapperS.of(EventIntentEnum.CLEARING)}), matching upstream 9.83.0.
 *
 * <p>On current {@code main} the fork emits the BARE unresolved value name wrapped as a plain
 * variable ({@code MapperS.of(Clearing)} / {@code MapperS.of(Monetary)}), which does not compile
 * — the bare enum value parses as an UNRESOLVED single-identifier
 * {@link com.regnosys.rosetta.ast.expressions.references.RSymbolReference} (enum VALUES are not in
 * function scope), and the {@code TypeInferenceEngine} only type-directs a bare enum value to its
 * {@link com.regnosys.rosetta.ast.supporting.REnumValue} when a SIBLING already types as the enum
 * — but the comparison sibling's enum type is MISSING on the expression node (the
 * {@code resolvedFeature} IR gap: {@code ExpressionTypeComputer.computeFeatureCall} returns a type
 * only when {@code resolvedFeature} is present, which {@code TypeDirectedResolver} sets only when
 * the receiver is non-MISSING). So the bare operand falls through to the variable path
 * ({@code ReferenceHandler.handle(RSymbolReference)} → {@code MapperS.of(name)}).
 *
 * <p>Fix: {@code ComparisonHandler.handle(REqualityExpr)} resolves the SIBLING operand's leaf enum
 * type via the renderer's gm-aware navigation resolution ({@code NavigationHandler.leafEnumeration}
 * — the SAME best-effort resolution that already recovers the sibling's {@code <EventIntentEnum>}
 * witness), then, when the bare operand's name matches a value of that enum, emits the qualified
 * {@code EnumName.CONSTANT} (via {@code EnumHelper.convertValue}), which the existing
 * {@code wrapEnumOperand} wraps in {@code MapperS.of(...)}. PARSER UNCHANGED.
 *
 * <p>This test is REVERT-VERIFIED RED: revert the {@code ComparisonHandler} bare-enum-comparand
 * resolution → the bare names {@code MapperS.of(Clearing)} / {@code MapperS.of(Monetary)} reappear
 * (the negative assertions fail) and the qualified-constant positive assertions fail.
 *
 * <p>Determinism follows {@link FunctionSetEnumConditionalTest}: the corpus is the version-frozen
 * {@code drr/6.34.1} cell (+ its transitive CDM 5.38.0 resolution closure), so the generator
 * output is a pure function of the fork's code — this test flips only when the fork changes.
 *
 * <p>Anchors (exact golden strings, materialised from
 * {@code test-corpus/drr/drr-6.34.1/.../generated/java}):
 * <ul>
 *   <li>{@code IsCleared} — sibling is an {@link
 *       com.regnosys.rosetta.ast.expressions.references.RFeatureCall} nav
 *       ({@code … -> businessEvent -> intent}); bare {@code Clearing} →
 *       {@code MapperS.of(EventIntentEnum.CLEARING)}.</li>
 *   <li>{@code RequiresUpFrontFeeProjection} — sibling is an {@code RFeatureCall} nav
 *       ({@code … -> primaryAssetClass}); bare {@code Credit} →
 *       {@code MapperS.of(AssetClassEnum.CREDIT)}.</li>
 *   <li>{@code SpreadCurrencyLeg1_Validation} / {@code SpreadCurrencyLeg2_Validation} — sibling is
 *       a DISGUISED nav {@link
 *       com.regnosys.rosetta.ast.expressions.references.REnumValueRef} ({@code spreadLeg1 ->
 *       spreadNotation}, the grammar's {@code EnumName -> valueName} ambiguity), NOT an
 *       {@code RFeatureCall}; bare {@code Monetary}/{@code Decimal}/{@code Basis} →
 *       {@code MapperS.of(PriceNotationEnum.MONETARY/DECIMAL/BASIS)}. Locks the disguised-sibling
 *       resolution path.</li>
 * </ul>
 */
class FunctionBareEnumComparisonTest {

    private static final Path DRR_ROSETTA_DIR =
            Path.of("../test-corpus/drr/drr-6.34.1/rosetta-source/src/main/rosetta");
    private static final Path DRR_GOLDEN_DIR =
            Path.of("../test-corpus/drr/drr-6.34.1/rosetta-source/src/generated/java");
    // DRR 6.34.1 bundles CDM 5.38.0 transitively (DRR_TO_CDM_VERSION in D11CorpusRegressionTest);
    // the CDM .rosetta closure is loaded resolution-only (excluded from emission by the filter).
    private static final Path CDM_DEP_ROSETTA_DIR =
            Path.of("../test-corpus/cdm/cdm-5.38.0/rosetta-source/src/main/rosetta");
    private static final Path BUILTINS_DIR = Path.of("../test-corpus/rune-dsl-builtins");

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    private static Map<String, String> functionOutput;

    static boolean drrAvailable() {
        if (!Files.isDirectory(DRR_ROSETTA_DIR) || !Files.isDirectory(DRR_GOLDEN_DIR)
                || !Files.isDirectory(CDM_DEP_ROSETTA_DIR)) {
            return false;
        }
        try (var stream = Files.walk(DRR_ROSETTA_DIR)) {
            return stream.anyMatch(p -> p.toString().endsWith(".rosetta"));
        } catch (IOException e) {
            return false;
        }
    }

    @BeforeAll
    static void generateDrrFunctions() throws IOException {
        if (!drrAvailable()) return;
        var corpus = loadDrrCorpus();
        // DRR emission filter — mirrors D11CorpusRegressionTest.emissionFilter("drr"): only
        // drr.* namespaces emit Java; the transitive-CDM closure stays resolution-only.
        var gm = new GeneratorModel(corpus.workspace(),
                model -> model.namespace() != null && model.namespace().startsWith("drr."));
        var gen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        functionOutput = new LinkedHashMap<>();
        gen.generate(functionOutput);
    }

    /** Sibling is an RFeatureCall nav; bare {@code Clearing} → {@code EventIntentEnum.CLEARING}. */
    @Test
    @EnabledIf("drrAvailable")
    void isCleared_featureCallSibling_bareEnumComparandResolvesToEnumConstant() {
        String body = requireGenerated("drr/regulation/common/functions/IsCleared.java");

        assertTrue(body.contains("MapperS.of(EventIntentEnum.CLEARING)"),
                "the bare comparison operand `Clearing` (sibling nav `… -> intent`) must resolve to "
                + "MapperS.of(EventIntentEnum.CLEARING). Body:\n" + body);
        assertFalse(body.contains("MapperS.of(Clearing)"),
                "the BARE unresolved operand `MapperS.of(Clearing)` must be gone — it reappears if the "
                + "ComparisonHandler bare-enum-comparand resolution is reverted. Body:\n" + body);
    }

    /** Sibling is an RFeatureCall nav; bare {@code Credit} → {@code AssetClassEnum.CREDIT}. */
    @Test
    @EnabledIf("drrAvailable")
    void requiresUpFrontFeeProjection_featureCallSibling_bareEnumComparandResolvesToEnumConstant() {
        String body = requireGenerated(
                "drr/projection/dtcc/rds/harmonized/cftc/rewrite/trade/functions/RequiresUpFrontFeeProjection.java");

        assertTrue(body.contains("MapperS.of(AssetClassEnum.CREDIT)"),
                "the bare comparison operand `Credit` (sibling nav `… -> primaryAssetClass`) must "
                + "resolve to MapperS.of(AssetClassEnum.CREDIT). Body:\n" + body);
        assertFalse(body.contains("MapperS.of(Credit)"),
                "the BARE unresolved operand `MapperS.of(Credit)` must be gone (reverts with the "
                + "ComparisonHandler fix). Body:\n" + body);
    }

    /**
     * Sibling is a DISGUISED nav (REnumValueRef `spreadLeg1 -> spreadNotation`), NOT an
     * RFeatureCall — exercises the disguised-sibling resolution path. Three bare enums.
     */
    @Test
    @EnabledIf("drrAvailable")
    void spreadCurrencyLeg1_disguisedNavSibling_bareEnumComparandsResolveToEnumConstants() {
        String body = requireGenerated(
                "drr/regulation/common/trade/price/functions/SpreadCurrencyLeg1_Validation.java");

        assertTrue(body.contains("MapperS.of(PriceNotationEnum.MONETARY)")
                        && body.contains("MapperS.of(PriceNotationEnum.DECIMAL)")
                        && body.contains("MapperS.of(PriceNotationEnum.BASIS)"),
                "the bare operands Monetary/Decimal/Basis (sibling DISGUISED nav `spreadLeg1 -> "
                + "spreadNotation`) must resolve to MapperS.of(PriceNotationEnum.MONETARY/DECIMAL/"
                + "BASIS). Body:\n" + body);
        assertFalse(body.contains("MapperS.of(Monetary)")
                        || body.contains("MapperS.of(Decimal)")
                        || body.contains("MapperS.of(Basis)"),
                "no BARE unresolved operand (MapperS.of(Monetary)/(Decimal)/(Basis)) may remain "
                + "(reverts with the ComparisonHandler fix). Body:\n" + body);
    }

    /** Second disguised-sibling case (SpreadCurrencyLeg2_Validation). */
    @Test
    @EnabledIf("drrAvailable")
    void spreadCurrencyLeg2_disguisedNavSibling_bareEnumComparandsResolveToEnumConstants() {
        String body = requireGenerated(
                "drr/regulation/common/trade/price/functions/SpreadCurrencyLeg2_Validation.java");

        assertTrue(body.contains("MapperS.of(PriceNotationEnum.MONETARY)")
                        && body.contains("MapperS.of(PriceNotationEnum.DECIMAL)")
                        && body.contains("MapperS.of(PriceNotationEnum.BASIS)"),
                "the bare operands Monetary/Decimal/Basis must resolve to the qualified "
                + "PriceNotationEnum constants. Body:\n" + body);
        assertFalse(body.contains("MapperS.of(Monetary)")
                        || body.contains("MapperS.of(Decimal)")
                        || body.contains("MapperS.of(Basis)"),
                "no BARE unresolved operand may remain. Body:\n" + body);
    }

    // =========================================================================
    // Helpers (mirror FunctionSetEnumConditionalTest + D11CorpusRegressionTest DRR loading)
    // =========================================================================

    private static String requireGenerated(String path) {
        assertNotNull(functionOutput,
                "Function generation did not run — DRR corpus unavailable?");
        String generated = functionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        return generated.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static RLinkingResult loadDrrCorpus() throws IOException {
        List<RModel> models = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        // 1. builtins.
        addRosetta(BUILTINS_DIR, models, skipped, null);
        // 2. transitive-CDM 5.38.0 closure — resolution-only (NOT version-stamped; excluded
        //    from emission by the drr namespace filter). Mirrors D11 loadCellCorpus DRR branch.
        addRosetta(CDM_DEP_ROSETTA_DIR, models, skipped, null);
        // 3. DRR cell source — version-stamped like D11 (CellSpec.versionStamp default).
        addRosetta(DRR_ROSETTA_DIR, models, skipped, "0.0.0.master-SNAPSHOT");
        if (!skipped.isEmpty()) {
            throw new IOException(
                    "Corpus loader failed to parse " + skipped.size()
                            + " file(s); refusing to produce partial workspace: "
                            + String.join("; ", skipped));
        }
        return RWorkspace.build(models);
    }

    private static void addRosetta(Path dir, List<RModel> models, List<String> skipped,
            String versionStamp) throws IOException {
        if (!Files.isDirectory(dir)) return;
        try (var stream = Files.walk(dir)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> p.toString().endsWith(".rosetta"))
                  .sorted()
                  .forEach(p -> {
                      try {
                          RModel model = AstBuilder.buildFromFile(p);
                          if (versionStamp != null) {
                              model.setVersion(versionStamp);
                          }
                          models.add(model);
                      } catch (Exception e) {
                          skipped.add(p + ": " + e.getMessage());
                      }
                  });
        }
    }
}
