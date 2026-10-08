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
 * Engine PR (facet {@code nav_after_get_rewrap}) — a RENDERER-ONLY fix
 * ({@code NavigationHandler}) that renders a navigation step whose RECEIVER is a
 * single-cardinality {@code only-element} ({@code .get()}) collapse like upstream 9.83.0:
 * the {@code .get()} receiver is re-wrapped in {@code MapperS.of(...)}, the navigated
 * feature's generic type witness is recovered, and the lambda variable is derived from the
 * element data type the {@code .get()} produces (the owner type), not the synthetic
 * {@code _featureName}.
 *
 * <p>On current {@code main} the fork emits the unwrapped, witness-less form — e.g.
 * {@code <chain>.get().map("getTradableOnTradingVenue", _tradableOnTradingVenue ->
 * _tradableOnTradingVenue.getTradableOnTradingVenue())} — which does not compile (a raw POJO
 * item has no {@code .map(...)}). Upstream emits
 * {@code MapperS.of(<chain>.get()).<TradableOnTradingVenueEnum>map("getTradableOnTradingVenue",
 * eSMATransactionInformation -> eSMATransactionInformation.getTradableOnTradingVenue())}.
 *
 * <p>Root cause: {@code NavigationHandler.resolveReceiverDataType} has no case for an
 * {@code RListOpExpr(ONLY_ELEMENT)} receiver, so {@code fallbackResolveFeature} returns
 * {@code null} for the step navigating FROM the {@code .get()} → no {@code <T>} witness
 * ({@code resolveTypeParam}) and a default {@code _feature} lambda var
 * ({@code resolveLambdaVarName}); and {@code handle(RFeatureCall)} never re-wraps the raw
 * {@code .get()} receiver in {@code MapperS.of(...)}. The fix adds the type-transparent
 * {@code ONLY_ELEMENT} case (the {@code .get()} collapses cardinality but keeps the element
 * type, so it recurses into the operated-on list expression) plus the structural
 * {@code MapperS.of(...)} re-wrap, both gated to the {@code ONLY_ELEMENT} receiver shape.
 *
 * <p>This test is REVERT-VERIFIED RED: revert the {@code NavigationHandler} change → the
 * {@code ONLY_ELEMENT} receiver loses its witness/lambda/wrap → the unwrapped
 * {@code .get().map("get...", _tradableOnTradingVenue -> ...)} form reappears (the negative
 * assertions fail) and the qualified positive assertions fail.
 *
 * <p>Determinism follows {@link FunctionSetEnumConditionalTest}: the corpus is the
 * version-frozen {@code drr/6.34.1} cell (+ its transitive CDM 5.38.0 resolution closure),
 * so the generator output is a pure function of the fork's code — this test flips only when
 * the fork changes.
 *
 * <p>Anchors (exact golden fragments, materialised from
 * {@code test-corpus/drr/drr-6.34.1/.../generated/java}). All four are single-step
 * {@code .get() -> tradableOnTradingVenue} navigations; the ESMA pair derives the lambda var
 * from {@code ESMATransactionInformation} ({@code eSMATransactionInformation}) and the UK
 * pair from {@code CommonTransactionInformation} ({@code commonTransactionInformation}),
 * locking the owner-type lambda derivation across two distinct element types.
 */
class FunctionNavAfterGetRewrapTest {

    private static final Path DRR_ROSETTA_DIR =
            Path.of("../test-corpus/drr/drr-6.34.1/rosetta-source/src/main/rosetta");
    private static final Path DRR_GOLDEN_DIR =
            Path.of("../test-corpus/drr/drr-6.34.1/rosetta-source/src/generated/java");
    // DRR 6.34.1 bundles CDM 5.38.0 transitively; the CDM .rosetta closure is loaded
    // resolution-only (excluded from emission by the drr namespace filter).
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

    /** ESMA EMIR REFIT — lambda var from ESMATransactionInformation. */
    @Test
    @EnabledIf("drrAvailable")
    void euEmirIsMicXOFF_navAfterGet_rewrapsWitnessAndOwnerLambda() {
        assertRewrapped(
                "drr/regulation/esma/emir/refit/trade/functions/EUEMIRIsMicXOFF.java",
                "eSMATransactionInformation");
    }

    @Test
    @EnabledIf("drrAvailable")
    void euEmirIsMicXXXX_navAfterGet_rewrapsWitnessAndOwnerLambda() {
        assertRewrapped(
                "drr/regulation/esma/emir/refit/trade/functions/EUEMIRIsMicXXXX.java",
                "eSMATransactionInformation");
    }

    /** FCA UK EMIR REFIT — lambda var from CommonTransactionInformation (distinct owner type). */
    @Test
    @EnabledIf("drrAvailable")
    void ukEmirIsMicXOFF_navAfterGet_rewrapsWitnessAndOwnerLambda() {
        assertRewrapped(
                "drr/regulation/fca/ukemir/refit/trade/functions/UKEMIRIsMicXOFF.java",
                "commonTransactionInformation");
    }

    @Test
    @EnabledIf("drrAvailable")
    void ukEmirIsMicXXXX_navAfterGet_rewrapsWitnessAndOwnerLambda() {
        assertRewrapped(
                "drr/regulation/fca/ukemir/refit/trade/functions/UKEMIRIsMicXXXX.java",
                "commonTransactionInformation");
    }

    private static void assertRewrapped(String path, String ownerLambdaVar) {
        String body = requireGenerated(path);
        String expected = ".get()).<TradableOnTradingVenueEnum>map(\"getTradableOnTradingVenue\", "
                + ownerLambdaVar + " -> " + ownerLambdaVar + ".getTradableOnTradingVenue())";
        assertTrue(body.contains(expected),
                "the only-element receiver must be re-wrapped in MapperS.of(...) with the "
                + "recovered <TradableOnTradingVenueEnum> witness and the owner-derived lambda var `"
                + ownerLambdaVar + "`. Expected fragment:\n  " + expected + "\nBody:\n" + body);
        assertFalse(body.contains(".get().map(\"getTradableOnTradingVenue\""),
                "the unwrapped, witness-less `.get().map(\"getTradableOnTradingVenue\", ...)` form "
                + "must be gone — it reappears if the NavigationHandler only-element fix is reverted. "
                + "Body:\n" + body);
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
