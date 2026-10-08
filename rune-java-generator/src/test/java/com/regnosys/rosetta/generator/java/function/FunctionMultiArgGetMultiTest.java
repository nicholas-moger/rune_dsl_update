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
 * PR #146 (facet {@code getMulti_arg_cardinality}) — guards that a multi-cardinality
 * navigation ARGUMENT passed into a multi-cardinality (List) function parameter
 * unwraps with {@code .getMulti()} (→ {@code List<T>}), not the non-compiling scalar
 * {@code .get()} (→ single {@code T}).
 *
 * <p>Root cause: {@code ReferenceHandler.evaluateArgIsMulti}'s arg-side cardinality
 * probe read the AST-level {@code RWorkspace.getCardinality}, which is RESOLUTION-BLIND
 * — it returns {@code SINGLE} for a top-level function-body {@link
 * com.regnosys.rosetta.ast.expressions.references.RFeatureCall} whose {@code resolvedFeature} was
 * never set, and for a disguised {@link com.regnosys.rosetta.ast.expressions.references.REnumValueRef}
 * navigation (it has no {@code REnumValueRef} case). The navigation side still emits
 * {@code .mapC(...)} for these via its receiver-chain fallback, so the compiled argument
 * IS a multi mapper; PR #146 adds {@code NavigationHandler.navLeafFeatureMulti}, which
 * recovers the leaf feature's cardinality the SAME way, so the {@code .getMulti()} gate
 * fires for these resolution-blind navs too.
 *
 * <p>Probe: {@code Create_IndexTransitionTermsChange} passes {@code instruction ->
 * priceQuantity} ({@code PriceQuantity (1..*)}) — a DISGUISED {@code REnumValueRef} nav
 * (its AST cardinality reads {@code SINGLE}) — into
 * {@code UpdateSpreadAdjustmentAndRateOptions.instructions (1..*)}. Before the fix the
 * fork emitted the non-compiling scalar {@code .get()} (a {@code PriceQuantity} into a
 * {@code List<? extends PriceQuantity>} parameter); the fix produces the golden
 * {@code .getMulti()}.
 *
 * <p>Determinism follows {@link FunctionGeneratorImportCollectionTest}: the corpus is the
 * version-frozen {@code cdm/6.20.6} cell, so the generator output is a pure function of
 * the fork's code — this test flips only when the generator changes.
 */
class FunctionMultiArgGetMultiTest {

    private static final Path CDM_ROSETTA_DIR =
            Path.of("../test-corpus/cdm/cdm-6.20.6/rosetta-source/src/main/rosetta");
    private static final Path CDM_GOLDEN_DIR =
            Path.of("../test-corpus/cdm/cdm-6.20.6/rosetta-source/src/generated/java");
    private static final Path BUILTINS_DIR = Path.of("../test-corpus/rune-dsl-builtins");

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    private static Map<String, String> functionOutput;

    static boolean cdmAvailable() {
        if (!Files.isDirectory(CDM_ROSETTA_DIR) || !Files.isDirectory(CDM_GOLDEN_DIR)) {
            return false;
        }
        try (var stream = Files.walk(CDM_ROSETTA_DIR)) {
            return stream.anyMatch(p -> p.toString().endsWith(".rosetta"));
        } catch (IOException e) {
            return false;
        }
    }

    @BeforeAll
    static void generateAllFunctions() throws IOException {
        if (!cdmAvailable()) return;
        var corpus = loadFullCorpus();
        var gm = new GeneratorModel(corpus.workspace());
        var gen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        functionOutput = new LinkedHashMap<>();
        gen.generate(functionOutput);
    }

    @Test
    @EnabledIf("cdmAvailable")
    void multiNavArg_intoMultiParam_unwrapsWithGetMulti_notScalarGet() {
        String body = requireGenerated(
                "cdm/event/common/functions/Create_IndexTransitionTermsChange.java");

        // The multi nav-arg `instruction -> priceQuantity` compiles to a MapperC mapC
        // tail; into the multi `instructions` param it must unwrap with .getMulti().
        String multiArgTail = ".<PriceQuantity>mapC(\"getPriceQuantity\", "
                + "indexTransitionInstruction -> indexTransitionInstruction.getPriceQuantity())"
                + ".getMulti()";
        assertTrue(body.contains(multiArgTail),
                "multi nav-arg into a multi (List) param must unwrap with .getMulti() — "
                + "the navLeafFeatureMulti fallback gate did not fire for the disguised "
                + "REnumValueRef nav. Body:\n" + body);

        // The non-compiling scalar form (single PriceQuantity into List<PriceQuantity>)
        // must no longer be emitted.
        assertFalse(body.contains("indexTransitionInstruction.getPriceQuantity()).get()"),
                "the non-compiling scalar .get() form of the multi nav-arg must be gone");
    }

    @Test
    @EnabledIf("cdmAvailable")
    void multiNavArg_viaUnresolvedFeatureCall_unwrapsWithGetMulti_notScalarGet() {
        // Companion to the disguised-REnumValueRef case above: this exercises
        // navLeafFeatureMulti's OTHER branch — an RFeatureCall whose resolvedFeature
        // is unset. Qualify_BaseProduct_EquityForward passes `economicTerms -> payout`
        // (Payout (0..*)) — a top-level function-body RFeatureCall navigation — into
        // SettlementPayoutOnlyExists's multi `payouts` parameter. CardinalityComputer
        // reads SINGLE for the unresolved-feature node, so the fallback recovers the
        // multi leaf via fallbackResolveFeature and the arg unwraps with .getMulti().
        String body = requireGenerated(
                "cdm/product/qualification/functions/Qualify_BaseProduct_EquityForward.java");

        String multiArgTail = ".<Payout>mapC(\"getPayout\", "
                + "_economicTerms -> _economicTerms.getPayout()).getMulti()";
        assertTrue(body.contains(multiArgTail),
                "multi nav-arg (RFeatureCall branch) into the SettlementPayoutOnlyExists "
                + "List param must unwrap with .getMulti(). Body:\n" + body);

        assertFalse(body.contains("_economicTerms.getPayout()).get()"),
                "the non-compiling scalar .get() form of the multi nav-arg must be gone");
    }

    // =========================================================================
    // Helpers (mirror FunctionGeneratorImportCollectionTest)
    // =========================================================================

    private static String requireGenerated(String path) {
        assertNotNull(functionOutput,
                "Function generation did not run — CDM corpus unavailable?");
        String generated = functionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        return generated.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static RLinkingResult loadFullCorpus() throws IOException {
        List<RModel> models = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        if (Files.isDirectory(BUILTINS_DIR)) {
            try (var stream = Files.walk(BUILTINS_DIR)) {
                stream.filter(Files::isRegularFile)
                      .filter(p -> p.toString().endsWith(".rosetta"))
                      .sorted()
                      .forEach(p -> {
                          try { models.add(AstBuilder.buildFromFile(p)); }
                          catch (Exception e) {
                              skipped.add(p + ": " + e.getMessage());
                          }
                      });
            }
        }
        try (var stream = Files.walk(CDM_ROSETTA_DIR)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> p.toString().endsWith(".rosetta"))
                  .sorted()
                  .forEach(p -> {
                      try {
                          RModel model = AstBuilder.buildFromFile(p);
                          model.setVersion("0.0.0.master-SNAPSHOT");
                          models.add(model);
                      } catch (Exception e) {
                          skipped.add(p + ": " + e.getMessage());
                      }
                  });
        }
        if (!skipped.isEmpty()) {
            throw new IOException(
                    "Corpus loader failed to parse " + skipped.size()
                            + " file(s); refusing to produce partial workspace: "
                            + String.join("; ", skipped));
        }
        return RWorkspace.build(models);
    }
}
