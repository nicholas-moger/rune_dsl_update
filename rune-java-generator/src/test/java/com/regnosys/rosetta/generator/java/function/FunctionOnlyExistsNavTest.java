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
 * Engine PR (facet {@code onlyexists_path_rendering}) — the PARAM-root NAVIGATION lock,
 * companion to {@link FunctionOnlyExistsTest} (which covers the simple bare-mapper receiver +
 * the alias-root scoped decline on the {@code cdm/6.20.6} cell).
 *
 * <p>The param-root navigation form ({@code MapperS.of(economicTerms).<Payout>map("getPayout",
 * _economicTerms -> _economicTerms.getPayout())}) is the entire cdm/5.38.0 nav population (the
 * cdm/6.20.6 FUNCTION cell has no param-root nav only-exists — its navs use an alias root), so
 * this lock loads the version-frozen {@code cdm/5.38.0} cell and asserts the byte-exact upstream
 * nav fragment for {@code Qualify_BaseProduct_EquitySwap}.
 *
 * <p>Anchor (exact golden, materialised from
 * {@code test-corpus/cdm/cdm-5.38.0/.../Qualify_BaseProduct_EquitySwap.java}): the element
 * {@code economicTerms -> payout -> interestRatePayout} navigates the param {@code economicTerms}
 * through its {@code payout} feature (parent type {@code Payout}) before the leaf, so the receiver
 * is the full parent navigation, {@code allFields} is {@code Payout}'s complete attribute set
 * (declaration order), and the selected fields are the per-element leaf names.
 *
 * <p>REVERT-VERIFIED RED: reverting either module restores the FQ-dotted-path placeholder
 * ({@code onlyExists(economicTerms, Arrays.asList("economicTerms.payout.interestRatePayout"), ...)}),
 * so the positive assertion fails.
 */
class FunctionOnlyExistsNavTest {

    private static final Path CDM_ROSETTA_DIR =
            Path.of("../test-corpus/cdm/cdm-5.38.0/rosetta-source/src/main/rosetta");
    private static final Path CDM_GOLDEN_DIR =
            Path.of("../test-corpus/cdm/cdm-5.38.0/rosetta-source/src/generated/java");
    private static final Path BUILTINS_DIR = Path.of("../test-corpus/rune-dsl-builtins");

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    /** The full Payout attribute list, declaration order (cdm 5.38.0). */
    private static final String PAYOUT_ATTRS =
            "Arrays.asList(\"interestRatePayout\", \"creditDefaultPayout\", \"optionPayout\", "
            + "\"commodityPayout\", \"forwardPayout\", \"fixedPricePayout\", \"securityPayout\", "
            + "\"cashflow\", \"performancePayout\", \"assetPayout\")";

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

    /**
     * Param-root navigation: {@code economicTerms -> payout -> <leaf>}. The receiver is the
     * compiled parent navigation {@code MapperS.of(economicTerms).<Payout>map("getPayout",
     * _economicTerms -> _economicTerms.getPayout())}, {@code allFields} is Payout's full attribute
     * set, and the leaf names are the selected fields (the {@code .orNullSafe(...)} chain exercises
     * multiple leaf lists sharing the one parent).
     */
    @Test
    @EnabledIf("cdmAvailable")
    void onlyExists_paramNavigationReceiver_rendersParentNavigationChainWithFullParentAttrs() {
        String body = requireGenerated(
                "cdm/product/qualification/functions/Qualify_BaseProduct_EquitySwap.java");

        assertTrue(body.contains(
                "onlyExists(MapperS.of(economicTerms).<Payout>map(\"getPayout\", "
                + "_economicTerms -> _economicTerms.getPayout()), " + PAYOUT_ATTRS
                + ", Arrays.asList(\"interestRatePayout\", \"performancePayout\"))"),
                "param-root nav only-exists must render the parent navigation receiver "
                + "MapperS.of(economicTerms).<Payout>map(\"getPayout\", ...), the full Payout "
                + "attribute set, and the short leaf names. Body:\n" + body);

        // Negative: the FQ-dotted-path placeholder must be gone.
        assertFalse(body.contains("\"economicTerms.payout.interestRatePayout\""),
                "the fully-qualified dotted-path field name "
                + "\"economicTerms.payout.interestRatePayout\" must be gone (it reappears if the "
                + "fix is reverted). Body:\n" + body);
        assertFalse(body.contains("onlyExists(economicTerms,"),
                "the raw-root receiver onlyExists(economicTerms, ...) must be gone — the receiver is "
                + "now the parent navigation. Body:\n" + body);
    }

    // =========================================================================
    // Helpers (mirror FunctionMapEnumSwitchTest, cdm/5.38.0 cell)
    // =========================================================================

    private static String requireGenerated(String path) {
        assertNotNull(functionOutput,
                "Function generation did not run — CDM 5.38.0 corpus unavailable?");
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
