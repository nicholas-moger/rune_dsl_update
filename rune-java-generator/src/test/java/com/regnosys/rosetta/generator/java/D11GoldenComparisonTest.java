package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
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

import static org.junit.jupiter.api.Assertions.*;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * D11 golden comparison tests. Parses real CDM .rosetta files,
 * generates Java source, and diffs against the upstream golden output
 * in common-domain-model/.
 *
 * <p>These tests require the CDM corpus to be present. They are skipped
 * if the golden output directory doesn't exist.
 */
class D11GoldenComparisonTest {

    private static final Path CDM_ROSETTA_DIR = Path.of("../common-domain-model/rosetta-source/src/main/rosetta");
    private static final Path CDM_GOLDEN_DIR = Path.of("../common-domain-model/rosetta-source/src/generated/java");
    private static final Path BUILTINS_DIR = Path.of("../test-corpus/rune-dsl-builtins");

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    static boolean cdmAvailable() {
        return Files.isDirectory(CDM_ROSETTA_DIR) && Files.isDirectory(CDM_GOLDEN_DIR);
    }

    // =========================================================================
    // Enum D11 comparison
    // =========================================================================

    @Test
    @EnabledIf("cdmAvailable")
    void enum_RoundingDirectionEnum_matches_golden() throws IOException {
        var rosettaFile = CDM_ROSETTA_DIR.resolve("base-math-enum.rosetta");
        RModel model = AstBuilder.buildFromFile(rosettaFile);
        // CDM version placeholder → actual value used in golden output
        model.setVersion("0.0.0.master-SNAPSHOT");

        List<RModel> models = loadModelsWithBuiltins(model);
        var result = RWorkspace.build(models);
        var gm = new GeneratorModel(result.workspace());

        var enumGen = new EnumGenerator(gm);
        Map<String, String> output = new LinkedHashMap<>();
        assertNoGenerationErrors(enumGen.generateClasses(model, gm.version(model), output));

        assertGoldenMatch(output, "RoundingDirectionEnum",
                "cdm/base/math/RoundingDirectionEnum.java");
    }

    @Test
    @EnabledIf("cdmAvailable")
    void enum_CompareOp_matches_golden() throws IOException {
        var rosettaFile = CDM_ROSETTA_DIR.resolve("base-math-enum.rosetta");
        RModel model = AstBuilder.buildFromFile(rosettaFile);
        model.setVersion("0.0.0.master-SNAPSHOT");

        List<RModel> models = loadModelsWithBuiltins(model);
        var result = RWorkspace.build(models);
        var gm = new GeneratorModel(result.workspace());

        var enumGen = new EnumGenerator(gm);
        Map<String, String> output = new LinkedHashMap<>();
        assertNoGenerationErrors(enumGen.generateClasses(model, gm.version(model), output));

        assertGoldenMatch(output, "CompareOp",
                "cdm/base/math/CompareOp.java");
    }

    @Test
    @EnabledIf("cdmAvailable")
    void enum_all_base_math_enums_match_golden() throws IOException {
        var rosettaFile = CDM_ROSETTA_DIR.resolve("base-math-enum.rosetta");
        RModel model = AstBuilder.buildFromFile(rosettaFile);
        model.setVersion("0.0.0.master-SNAPSHOT");

        List<RModel> models = loadModelsWithBuiltins(model);
        var result = RWorkspace.build(models);
        var gm = new GeneratorModel(result.workspace());

        var enumGen = new EnumGenerator(gm);
        Map<String, String> output = new LinkedHashMap<>();
        assertNoGenerationErrors(enumGen.generateClasses(model, gm.version(model), output));

        // All generated enums should match golden
        int checked = 0;
        for (var entry : output.entrySet()) {
            Path goldenPath = CDM_GOLDEN_DIR.resolve(entry.getKey());
            if (Files.exists(goldenPath)) {
                String golden = normalize(Files.readString(goldenPath));
                String generated = normalize(entry.getValue());
                if (!golden.equals(generated)) {
                    fail(detailedDiff(entry.getKey(), golden, generated));
                }
                checked++;
            }
        }
        assertTrue(checked > 0, "No golden files found for comparison");
    }

    @Test
    @EnabledIf("cdmAvailable")
    void enum_BusinessCenterEnum_matches_golden() throws IOException {
        var rosettaFile = CDM_ROSETTA_DIR.resolve("base-datetime-enum.rosetta");
        RModel model = AstBuilder.buildFromFile(rosettaFile);
        model.setVersion("0.0.0.master-SNAPSHOT");

        // Need base-desc.rosetta for corpus definitions
        var descFile = CDM_ROSETTA_DIR.resolve("base-desc.rosetta");
        List<RModel> models = loadModelsWithBuiltins(model);
        try { models.add(AstBuilder.buildFromFile(descFile)); } catch (Exception e) { /* skip */ }
        var result = RWorkspace.build(models);
        var gm = new GeneratorModel(result.workspace());

        var enumGen = new EnumGenerator(gm);
        Map<String, String> output = new LinkedHashMap<>();
        assertNoGenerationErrors(enumGen.generateClasses(model, gm.version(model), output));

        assertGoldenMatch(output, "BusinessCenterEnum",
                "cdm/base/datetime/BusinessCenterEnum.java");
    }

    // =========================================================================
    // POJO D11 comparison
    // =========================================================================

    @Test
    @EnabledIf("cdmAvailable")
    void pojo_MeasureBase_structure_check() throws IOException {
        // Parse the math types file — MeasureBase has value:number + unit:UnitType
        var rosettaFile = CDM_ROSETTA_DIR.resolve("base-math-type.rosetta");
        RModel model = AstBuilder.buildFromFile(rosettaFile);
        model.setVersion("0.0.0.master-SNAPSHOT");

        // Also parse enum file for UnitType's enum references
        var enumFile = CDM_ROSETTA_DIR.resolve("base-math-enum.rosetta");
        RModel enumModel = AstBuilder.buildFromFile(enumFile);
        enumModel.setVersion("0.0.0.master-SNAPSHOT");

        List<RModel> models = loadModelsWithBuiltins(model);
        models.add(enumModel);
        var result = RWorkspace.build(models);
        var gm = new GeneratorModel(result.workspace());

        var pojoGen = new com.regnosys.rosetta.generator.java.object.ModelObjectGenerator(
                gm, TYPE_TRANSLATOR, TYPE_UTIL);
        // P2.1.3c T2: parallel choice POJO emission (β1 fix). Wired here for symmetry
        // with D11CorpusRegressionTest; this test happens to load a data-only fixture,
        // so the choice generator will be a no-op, but the wiring stays consistent.
        var choiceGen = new com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator(
                gm, TYPE_TRANSLATOR, TYPE_UTIL, pojoGen);
        Map<String, String> output = new LinkedHashMap<>();
        assertNoGenerationErrors(pojoGen.generateClasses(model, gm.version(model), output));
        assertNoGenerationErrors(choiceGen.generateClasses(model, gm.version(model), output));

        // MeasureBase should be generated
        String key = output.keySet().stream()
                .filter(k -> k.contains("MeasureBase") && !k.contains("Meta") && !k.contains("Validator"))
                .findFirst()
                .orElse(null);
        assertNotNull(key, "MeasureBase should be generated. Keys: " + output.keySet());

        String generated = normalize(output.get(key));

        // Compare against golden
        Path goldenPath = CDM_GOLDEN_DIR.resolve("cdm/base/math/MeasureBase.java");
        if (Files.exists(goldenPath)) {
            String golden = normalize(Files.readString(goldenPath));
            if (!golden.equals(generated)) {
                fail(detailedDiff("MeasureBase.java", golden, generated));
            }
        }
    }

    @Test
    @EnabledIf("cdmAvailable")
    @org.junit.jupiter.api.Disabled("12/17 types fail — needs inheritance, list impl constructor, meta convenience setters")
    void pojo_all_base_math_types() throws IOException {
        var rosettaFile = CDM_ROSETTA_DIR.resolve("base-math-type.rosetta");
        RModel model = AstBuilder.buildFromFile(rosettaFile);
        model.setVersion("0.0.0.master-SNAPSHOT");

        var enumFile = CDM_ROSETTA_DIR.resolve("base-math-enum.rosetta");
        RModel enumModel = AstBuilder.buildFromFile(enumFile);
        enumModel.setVersion("0.0.0.master-SNAPSHOT");

        List<RModel> models = loadModelsWithBuiltins(model);
        models.add(enumModel);
        var result = RWorkspace.build(models);
        var gm = new GeneratorModel(result.workspace());

        var pojoGen = new com.regnosys.rosetta.generator.java.object.ModelObjectGenerator(
                gm, TYPE_TRANSLATOR, TYPE_UTIL);
        // P2.1.3c T2: parallel choice POJO emission (β1 fix). See first call site
        // earlier in this file for the full rationale.
        var choiceGen = new com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator(
                gm, TYPE_TRANSLATOR, TYPE_UTIL, pojoGen);
        Map<String, String> output = new LinkedHashMap<>();
        assertNoGenerationErrors(pojoGen.generateClasses(model, gm.version(model), output));
        assertNoGenerationErrors(choiceGen.generateClasses(model, gm.version(model), output));

        // Compare generated POJOs against golden — skip types with meta convenience setters
        // (setCurrencyValue etc.) which are Phase D features
        int checked = 0;
        var failures = new java.util.ArrayList<String>();
        for (var entry : output.entrySet()) {
            Path goldenPath = CDM_GOLDEN_DIR.resolve(entry.getKey());
            if (Files.exists(goldenPath)) {
                String golden = normalize(Files.readString(goldenPath));
                String generated = normalize(entry.getValue());
                if (!golden.equals(generated)) {
                    failures.add(entry.getKey());
                }
                checked++;
            }
        }
        assertTrue(checked > 0, "No golden files found. Keys: " + output.keySet());
        if (!failures.isEmpty()) {
            // Show first failure detail
            Path first = CDM_GOLDEN_DIR.resolve(failures.get(0));
            fail("D11 mismatches in " + failures.size() + "/" + checked + " files: " + failures
                    + "\n\n" + detailedDiff(failures.get(0),
                    normalize(Files.readString(first)),
                    normalize(output.get(failures.get(0)))));
        }
    }

    // =========================================================================
    // Function golden comparison (Layer 2 gate — 10 representative files)
    // =========================================================================

    private static Map<String, String> functionOutput;

    @BeforeAll
    static void generateAllFunctions() throws IOException {
        if (!cdmAvailable()) return;
        var corpus = loadFullCorpusStatic();
        var gm = new GeneratorModel(corpus.workspace());
        var gen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        functionOutput = new LinkedHashMap<>();
        gen.generate(functionOutput);
    }

    @Test
    @EnabledIf("cdmAvailable")
    void function_Max_matches_golden() throws IOException {
        assertFunctionGoldenMatch("cdm/base/math/functions/Max.java");
    }

    @Test
    @EnabledIf("cdmAvailable")
    void function_Abs_matches_golden() throws IOException {
        assertFunctionGoldenMatch("cdm/base/math/functions/Abs.java");
    }

    @Test
    @EnabledIf("cdmAvailable")
    @org.junit.jupiter.api.Disabled("L3 — needs alias type resolution, MapperC/mapC, lambda naming (T17+T18+T19)")
    void function_Qualify_CashTransfer_matches_golden() throws IOException {
        assertFunctionGoldenMatch("cdm/event/qualification/functions/Qualify_CashTransfer.java");
    }

    @Test
    @EnabledIf("cdmAvailable")
    @org.junit.jupiter.api.Disabled("L3 — needs MapperC wrapping, type params on map(), lambda naming (T17+T18)")
    void function_FilterQuantity_matches_golden() throws IOException {
        assertFunctionGoldenMatch("cdm/base/math/functions/FilterQuantity.java");
    }

    @Test
    @EnabledIf("cdmAvailable")
    @org.junit.jupiter.api.Disabled("Targets the RETIRED pre-#85 clone dirs; the dispatch "
            + "expression/alias-resolution gap it named LANDED at PR #369 — the live "
            + "byte-locks are DispatchVariantResolutionComposeTest against the 5-cell corpus")
    void function_YearFraction_matches_golden() throws IOException {
        assertFunctionGoldenMatch("cdm/base/datetime/daycount/functions/YearFraction.java");
    }

    @Test
    @EnabledIf("cdmAvailable")
    void function_ResolveAdjustableDate_matches_golden() throws IOException {
        assertFunctionGoldenMatch("cdm/base/datetime/functions/ResolveAdjustableDate.java");
    }

    @Test
    @EnabledIf("cdmAvailable")
    @org.junit.jupiter.api.Disabled("L3 — needs condition expression rendering, alias bodies, type coercion (T17+T18+T19)")
    void function_DeliveryAmount_matches_golden() throws IOException {
        assertFunctionGoldenMatch("cdm/legaldocumentation/csa/functions/DeliveryAmount.java");
    }

    @Test
    @EnabledIf("cdmAvailable")
    @org.junit.jupiter.api.Disabled("L3 — needs segment path rendering, condition rendering, imports (T17+T18)")
    void function_Create_AcceptedWorkflowStep_matches_golden() throws IOException {
        assertFunctionGoldenMatch("cdm/event/workflow/functions/Create_AcceptedWorkflowStep.java");
    }

    @Test
    @EnabledIf("cdmAvailable")
    @org.junit.jupiter.api.Disabled("L3 — needs MapperC wrapping, lambda naming, imports for expression types (T17+T18)")
    void function_FilterOpenTradeStates_matches_golden() throws IOException {
        assertFunctionGoldenMatch("cdm/event/common/functions/FilterOpenTradeStates.java");
    }

    @Test
    @EnabledIf("cdmAvailable")
    // RE-ENABLED at PR #360 (facet mapItemClosureParamReceiver): the closure-param
    // disguised-leaf cardinality read + the resolved-input shadow escape flipped the
    // file to byte parity (both cdm cells).
    void function_UpdateAmountForEachMatchingQuantity_matches_golden() throws IOException {
        assertFunctionGoldenMatch("cdm/product/common/settlement/functions/UpdateAmountForEachMatchingQuantity.java");
    }

    private void assertFunctionGoldenMatch(String relativePath) throws IOException {
        assertNotNull(functionOutput, "Function generation did not run (CDM unavailable?)");
        String generated = functionOutput.get(relativePath);
        assertNotNull(generated, "Function not generated: " + relativePath
                + ". Available keys containing '" + relativePath.substring(relativePath.lastIndexOf('/') + 1, relativePath.lastIndexOf('.'))
                + "': " + functionOutput.keySet().stream()
                    .filter(k -> k.contains(relativePath.substring(relativePath.lastIndexOf('/') + 1, relativePath.lastIndexOf('.'))))
                    .toList());

        Path goldenPath = CDM_GOLDEN_DIR.resolve(relativePath);
        assertTrue(Files.exists(goldenPath), "Golden file not found: " + goldenPath);
        String golden = normalize(Files.readString(goldenPath));
        generated = normalize(generated);

        if (!golden.equals(generated)) {
            fail(detailedDiff(relativePath, golden, generated));
        }
    }

    private static com.regnosys.rosetta.symbols.RLinkingResult loadFullCorpusStatic() throws IOException {
        List<RModel> models = new ArrayList<>();
        if (Files.isDirectory(BUILTINS_DIR)) {
            try (var stream = Files.list(BUILTINS_DIR)) {
                stream.filter(p -> p.toString().endsWith(".rosetta"))
                      .sorted()
                      .forEach(p -> {
                          try { models.add(AstBuilder.buildFromFile(p)); }
                          catch (Exception e) { /* skip */ }
                      });
            }
        }
        try (var stream = Files.list(CDM_ROSETTA_DIR)) {
            stream.filter(p -> p.toString().endsWith(".rosetta"))
                  .sorted()
                  .forEach(p -> {
                      try {
                          RModel model = AstBuilder.buildFromFile(p);
                          model.setVersion("0.0.0.master-SNAPSHOT");
                          models.add(model);
                      } catch (Exception e) { /* skip */ }
                  });
        }
        return RWorkspace.build(models);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void assertGoldenMatch(Map<String, String> output, String typeName,
                                    String goldenRelPath) throws IOException {
        String key = output.keySet().stream()
                .filter(k -> k.contains(typeName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        typeName + " not generated. Keys: " + output.keySet()));

        String generated = normalize(output.get(key));
        Path goldenPath = CDM_GOLDEN_DIR.resolve(goldenRelPath);
        assertTrue(Files.exists(goldenPath),
                "Golden file not found: " + goldenPath);
        String golden = normalize(Files.readString(goldenPath));

        if (!golden.equals(generated)) {
            fail(detailedDiff(goldenRelPath, golden, generated));
        }
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    private String detailedDiff(String name, String golden, String generated) {
        String[] goldLines = golden.split("\n", -1);
        String[] genLines = generated.split("\n", -1);
        var diff = new StringBuilder();
        diff.append("D11 MISMATCH for ").append(name).append(":\n");
        int maxLines = Math.max(goldLines.length, genLines.length);
        int diffCount = 0;
        for (int i = 0; i < maxLines && diffCount < 10; i++) {
            String gl = i < goldLines.length ? goldLines[i] : "<EOF>";
            String ge = i < genLines.length ? genLines[i] : "<EOF>";
            if (!gl.equals(ge)) {
                diff.append("  Line ").append(i + 1).append(":\n");
                diff.append("    GOLDEN:    |").append(gl).append("|\n");
                diff.append("    GENERATED: |").append(ge).append("|\n");
                diffCount++;
            }
        }
        if (diffCount >= 10) {
            diff.append("  ... (").append(maxLines - 10).append(" more lines)\n");
        }
        diff.append("  Total lines — golden: ").append(goldLines.length)
            .append(", generated: ").append(genLines.length);
        return diff.toString();
    }

    private List<RModel> loadModelsWithBuiltins(RModel mainModel) {
        List<RModel> models = new ArrayList<>();
        models.add(mainModel);
        if (Files.isDirectory(BUILTINS_DIR)) {
            try (var stream = Files.list(BUILTINS_DIR)) {
                stream.filter(p -> p.toString().endsWith(".rosetta"))
                      .sorted()
                      .forEach(p -> {
                          try {
                              models.add(AstBuilder.buildFromFile(p));
                          } catch (Exception e) {
                              // Skip files that fail to parse
                          }
                      });
            } catch (IOException e) {
                // Builtins not available — proceed without them
            }
        }
        return models;
    }
}
