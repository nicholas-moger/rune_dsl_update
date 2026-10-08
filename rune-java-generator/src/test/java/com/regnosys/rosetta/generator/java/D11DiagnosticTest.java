package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * Diagnostic test: generates full corpus, then dumps detailed diffs
 * for sampled mismatched files across the first-diff spectrum.
 * Used for mismatch taxonomy analysis.
 */
class D11DiagnosticTest {

    private static final Path CDM_ROSETTA_DIR = Path.of("../common-domain-model/rosetta-source/src/main/rosetta");
    private static final Path CDM_GOLDEN_DIR = Path.of("../common-domain-model/rosetta-source/src/generated/java");
    private static final Path BUILTINS_DIR = Path.of("../test-corpus/rune-dsl-builtins");

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    static boolean cdmAvailable() {
        return Files.isDirectory(CDM_ROSETTA_DIR) && Files.isDirectory(CDM_GOLDEN_DIR);
    }

    @Test
    @EnabledIf("cdmAvailable")
    void classify_all_mismatches() throws IOException {
        // Load full corpus
        List<RModel> models = new ArrayList<>();
        if (Files.isDirectory(BUILTINS_DIR)) {
            try (var stream = Files.list(BUILTINS_DIR)) {
                stream.filter(p -> p.toString().endsWith(".rosetta"))
                      .sorted()
                      .forEach(p -> {
                          try { models.add(AstBuilder.buildFromFile(p)); }
                          catch (Exception e) { System.err.println("  skip: " + e.getMessage()); }
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
                      } catch (Exception e) { System.err.println("  skip: " + e.getMessage()); }
                  });
        }

        var result = RWorkspace.build(models);
        var gm = new GeneratorModel(result.workspace());
        var pojoGen = new ModelObjectGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        // P2.1.3c T2: emit top-level choice POJOs alongside data-type POJOs
        // (β1 fix; ChoiceObjectGenerator parallels ModelObjectGenerator for RChoice
        // root elements). Diagnostic-only test path; throws are caught + skipped.
        var choiceGen = new com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator(
                gm, TYPE_TRANSLATOR, TYPE_UTIL, pojoGen);

        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : result.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                try { assertNoGenerationErrors(pojoGen.generateClasses(model, gm.version(model), output)); }
                catch (Exception e) { System.err.println("  skip: " + e.getMessage()); }
                try { assertNoGenerationErrors(choiceGen.generateClasses(model, gm.version(model), output)); }
                catch (Exception e) { System.err.println("  skip: " + e.getMessage()); }
            }
        }

        // Root cause counters
        int rcMissingImport = 0;
        int rcExtraImport = 0;
        int rcPropertyOrder = 0;
        int rcMetaSetter = 0;
        int rcEnumHashCode = 0;
        int rcListWildcard = 0;
        int rcGlobalKey = 0;
        int rcOther = 0;
        int matched = 0;

        // Track which categories each file falls into (by first diff)
        var categoryFiles = new LinkedHashMap<String, List<String>>();
        categoryFiles.put("MISSING_IMPORT", new ArrayList<>());
        categoryFiles.put("EXTRA_IMPORT", new ArrayList<>());
        categoryFiles.put("PROPERTY_ORDER", new ArrayList<>());
        categoryFiles.put("META_SETTER", new ArrayList<>());
        categoryFiles.put("ENUM_HASHCODE", new ArrayList<>());
        categoryFiles.put("LIST_WILDCARD", new ArrayList<>());
        categoryFiles.put("GLOBAL_KEY", new ArrayList<>());
        categoryFiles.put("OTHER", new ArrayList<>());

        for (var entry : output.entrySet()) {
            Path goldenPath = CDM_GOLDEN_DIR.resolve(entry.getKey());
            if (!Files.exists(goldenPath)) continue;
            String golden = normalize(Files.readString(goldenPath));
            String generated = normalize(entry.getValue());
            if (golden.equals(generated)) { matched++; continue; }

            String[] gl = golden.split("\n", -1);
            String[] ge = generated.split("\n", -1);

            // Classify by analyzing diff lines directly
            boolean hasMissingImport = false;
            boolean hasExtraImport = false;
            boolean hasPropertyOrder = false;
            boolean hasMetaSetter = false;
            boolean hasEnumHashCode = false;
            boolean hasListWildcard = false;
            boolean hasGlobalKey = false;

            int maxLines = Math.max(gl.length, ge.length);
            for (int i = 0; i < maxLines; i++) {
                String g = i < gl.length ? gl[i] : "<EOF>";
                String e = i < ge.length ? ge[i] : "<EOF>";
                if (g.equals(e)) continue;

                // Enum hashCode pattern
                if (g.contains("getClass().getName().hashCode()") && !e.contains("getClass().getName().hashCode()")) {
                    hasEnumHashCode = true;
                }
                // Meta convenience setter
                if (g.contains("set") && g.contains("Value(") && !e.contains("Value(")) {
                    hasMetaSetter = true;
                }
                // List wildcard for basic types
                if (e.contains("? extends String") || e.contains("? extends Integer") ||
                    e.contains("? extends BigDecimal") || e.contains("? extends LocalDate") ||
                    e.contains("? extends Boolean") || e.contains("? extends LocalTime")) {
                    hasListWildcard = true;
                }
                // GlobalKey/HasKey interface
                if (g.contains("GlobalKey") || g.contains("HasKey") || g.contains("RuneMetaType") || g.contains("AttributeMeta")) {
                    hasGlobalKey = true;
                }
                // Import diffs
                if (g.trim().startsWith("import ") || e.trim().startsWith("import ")) {
                    if (g.trim().startsWith("import ") && e.trim().startsWith("import ")) {
                        hasMissingImport = true; // different imports at same position
                    } else if (g.trim().startsWith("import ") && !e.trim().startsWith("import ")) {
                        hasMissingImport = true;
                    } else if (!g.trim().startsWith("import ") && e.trim().startsWith("import ")) {
                        hasExtraImport = true;
                    }
                }
                // Property ordering (different getters at same line)
                if (g.trim().matches(".*\\bget\\w+\\(\\).*") && e.trim().matches(".*\\bget\\w+\\(\\).*")
                    && !g.trim().startsWith("import") && !g.contains("hashCode")) {
                    hasPropertyOrder = true;
                }
            }

            // Classify by FIRST root cause (primary category)
            String primary;
            if (hasMissingImport) { primary = "MISSING_IMPORT"; rcMissingImport++; }
            else if (hasExtraImport) { primary = "EXTRA_IMPORT"; rcExtraImport++; }
            else if (hasPropertyOrder) { primary = "PROPERTY_ORDER"; rcPropertyOrder++; }
            else if (hasMetaSetter) { primary = "META_SETTER"; rcMetaSetter++; }
            else if (hasEnumHashCode) { primary = "ENUM_HASHCODE"; rcEnumHashCode++; }
            else if (hasListWildcard) { primary = "LIST_WILDCARD"; rcListWildcard++; }
            else if (hasGlobalKey) { primary = "GLOBAL_KEY"; rcGlobalKey++; }
            else { primary = "OTHER"; rcOther++; }
            categoryFiles.get(primary).add(entry.getKey());
        }

        System.out.println("=== D11 ROOT CAUSE TAXONOMY ===");
        System.out.println("Matched: " + matched);
        System.out.println("RC1 MISSING_IMPORT: " + rcMissingImport);
        System.out.println("RC2 EXTRA_IMPORT: " + rcExtraImport);
        System.out.println("RC3 PROPERTY_ORDER: " + rcPropertyOrder);
        System.out.println("RC4 META_SETTER: " + rcMetaSetter);
        System.out.println("RC5 ENUM_HASHCODE: " + rcEnumHashCode);
        System.out.println("RC6 LIST_WILDCARD: " + rcListWildcard);
        System.out.println("RC7 GLOBAL_KEY: " + rcGlobalKey);
        System.out.println("RC8 OTHER: " + rcOther);
        System.out.println("\nTotal mismatched: " + (rcMissingImport + rcExtraImport + rcPropertyOrder
                + rcMetaSetter + rcEnumHashCode + rcListWildcard + rcGlobalKey + rcOther));

        // Show "OTHER" files for investigation
        if (!categoryFiles.get("OTHER").isEmpty()) {
            System.out.println("\nOTHER files (unclassified):");
            for (String f : categoryFiles.get("OTHER")) {
                System.out.println("  " + f);
            }
        }

        // Show sample from each category
        for (var cat : categoryFiles.entrySet()) {
            if (!cat.getValue().isEmpty()) {
                System.out.println("\nSample " + cat.getKey() + ": " + cat.getValue().get(0));
            }
        }

    }

    @Test
    @EnabledIf("cdmAvailable")
    void dump_mismatch_taxonomy_samples() throws IOException {
        // Load full corpus
        List<RModel> models = new ArrayList<>();
        if (Files.isDirectory(BUILTINS_DIR)) {
            try (var stream = Files.list(BUILTINS_DIR)) {
                stream.filter(p -> p.toString().endsWith(".rosetta"))
                      .sorted()
                      .forEach(p -> {
                          try { models.add(AstBuilder.buildFromFile(p)); }
                          catch (Exception e) { System.err.println("  skip: " + e.getMessage()); }
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
                      } catch (Exception e) { System.err.println("  skip: " + e.getMessage()); }
                  });
        }

        var result = RWorkspace.build(models);
        var gm = new GeneratorModel(result.workspace());
        var pojoGen = new ModelObjectGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        // P2.1.3c T2: emit top-level choice POJOs alongside data-type POJOs
        // (β1 fix; ChoiceObjectGenerator parallels ModelObjectGenerator for RChoice
        // root elements). Diagnostic-only test path; throws are caught + skipped.
        var choiceGen = new com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator(
                gm, TYPE_TRANSLATOR, TYPE_UTIL, pojoGen);

        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : result.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                try { assertNoGenerationErrors(pojoGen.generateClasses(model, gm.version(model), output)); }
                catch (Exception e) { System.err.println("  skip: " + e.getMessage()); }
                try { assertNoGenerationErrors(choiceGen.generateClasses(model, gm.version(model), output)); }
                catch (Exception e) { System.err.println("  skip: " + e.getMessage()); }
            }
        }

        // Sample files from different first-diff buckets
        String[] samples = {
            // Line 3-4 bucket (imports differ early)
            "cdm/event/common/Trade.java",
            "cdm/event/common/CollateralPosition.java",
            // Line 5-6 bucket
            "cdm/base/datetime/AdjustableDate.java",
            "cdm/base/math/QuantitySchedule.java",
            // Line 15-30 bucket (imports differ later, or class decl)
            "cdm/legaldocumentation/common/NoticeContactInformation.java",
            "cdm/base/datetime/BusinessDateRange.java",
            // Line 40-50 bucket
            "cdm/product/template/OptionPayout.java",
            // Line 79+ bucket (body structure)
            "cdm/base/datetime/BusinessCenterTime.java",
            "cdm/observable/asset/CreditRatingDebt.java",
            // Line 125+ bucket
            "cdm/product/collateral/IssuerCountryOfOrigin.java",
            // Line 144+ bucket (deep body)
            "cdm/base/math/Rounding.java",
            "cdm/base/staticdata/party/TelephoneNumber.java",
            // Line 190+ bucket
            "cdm/base/staticdata/party/ContactInformation.java",
            // Line 265+ bucket
            "cdm/product/collateral/EligibilityQuery.java",
            // Line 567 bucket (very deep)
            "cdm/product/common/settlement/DeliverableObligations.java",
            // Post-P14 samples
            "cdm/event/common/AccrualFactor.java",
            "cdm/legaldocumentation/master/MasterAgreementBase.java",
            "cdm/product/asset/CorrelationReturnTerms.java",
            "cdm/base/datetime/DateList.java",
            "cdm/base/math/Measure.java",
            "cdm/base/datetime/AdjustableDates.java",
            "cdm/base/staticdata/party/AncillaryParty.java",
            "cdm/base/datetime/RelativeDateOffset.java",
            // OTHER (unclassified) files
            "cdm/legaldocumentation/csa/Regime.java",
            "cdm/legaldocumentation/csa/Threshold.java",
            "cdm/legaldocumentation/master/isda/AutomaticEarlyTermination.java",
        };

        System.out.println("=== D11 MISMATCH TAXONOMY DIAGNOSTIC ===\n");
        for (String sample : samples) {
            String generated = output.get(sample);
            if (generated == null) {
                System.out.println("--- " + sample + " --- NOT GENERATED\n");
                continue;
            }
            Path goldenPath = CDM_GOLDEN_DIR.resolve(sample);
            if (!Files.exists(goldenPath)) {
                System.out.println("--- " + sample + " --- NO GOLDEN FILE\n");
                continue;
            }
            String golden = normalize(Files.readString(goldenPath));
            generated = normalize(generated);

            if (golden.equals(generated)) {
                System.out.println("--- " + sample + " --- MATCH\n");
                continue;
            }

            String[] gl = golden.split("\n", -1);
            String[] ge = generated.split("\n", -1);
            int firstDiff = -1;
            int maxLines = Math.max(gl.length, ge.length);
            for (int i = 0; i < maxLines; i++) {
                String g = i < gl.length ? gl[i] : "<EOF>";
                String e = i < ge.length ? ge[i] : "<EOF>";
                if (!g.equals(e)) { firstDiff = i; break; }
            }

            System.out.println("--- " + sample + " --- FIRST DIFF AT LINE " + (firstDiff + 1)
                    + " (golden=" + gl.length + " lines, generated=" + ge.length + " lines)");

            // Show 15 diff lines for context
            int diffCount = 0;
            for (int i = 0; i < maxLines && diffCount < 15; i++) {
                String g = i < gl.length ? gl[i] : "<EOF>";
                String e = i < ge.length ? ge[i] : "<EOF>";
                if (!g.equals(e)) {
                    System.out.println("  L" + (i + 1) + " GOLDEN:    |" + g + "|");
                    System.out.println("  L" + (i + 1) + " GENERATED: |" + e + "|");
                    diffCount++;
                }
            }
            System.out.println();
        }
    }

    @Test
    @EnabledIf("cdmAvailable")
    void dump_all_remaining_mismatches() throws IOException {
        // Load full corpus
        List<RModel> models = new ArrayList<>();
        if (Files.isDirectory(BUILTINS_DIR)) {
            try (var stream = Files.list(BUILTINS_DIR)) {
                stream.filter(p -> p.toString().endsWith(".rosetta"))
                      .sorted()
                      .forEach(p -> {
                          try { models.add(AstBuilder.buildFromFile(p)); }
                          catch (Exception e) { System.err.println("  skip: " + e.getMessage()); }
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
                      } catch (Exception e) { System.err.println("  skip: " + e.getMessage()); }
                  });
        }

        var result = RWorkspace.build(models);
        var gm = new GeneratorModel(result.workspace());
        var pojoGen = new ModelObjectGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        // P2.1.3c T2: emit top-level choice POJOs alongside data-type POJOs
        // (β1 fix; ChoiceObjectGenerator parallels ModelObjectGenerator for RChoice
        // root elements). Diagnostic-only test path; throws are caught + skipped.
        var choiceGen = new com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator(
                gm, TYPE_TRANSLATOR, TYPE_UTIL, pojoGen);

        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : result.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                try { assertNoGenerationErrors(pojoGen.generateClasses(model, gm.version(model), output)); }
                catch (Exception e) { System.err.println("  skip: " + e.getMessage()); }
                try { assertNoGenerationErrors(choiceGen.generateClasses(model, gm.version(model), output)); }
                catch (Exception e) { System.err.println("  skip: " + e.getMessage()); }
            }
        }

        System.out.println("=== ALL REMAINING POJO MISMATCHES ===\n");
        int mismatchCount = 0;
        for (var entry : output.entrySet()) {
            Path goldenPath = CDM_GOLDEN_DIR.resolve(entry.getKey());
            if (!Files.exists(goldenPath)) continue;
            String golden = normalize(Files.readString(goldenPath));
            String generated = normalize(entry.getValue());
            if (golden.equals(generated)) continue;

            mismatchCount++;
            String[] gl = golden.split("\n", -1);
            String[] ge = generated.split("\n", -1);
            int maxLines = Math.max(gl.length, ge.length);

            System.out.println("--- " + entry.getKey() + " (golden=" + gl.length + " gen=" + ge.length + ") ---");
            int diffCount = 0;
            for (int i = 0; i < maxLines && diffCount < 15; i++) {
                String g = i < gl.length ? gl[i] : "<EOF>";
                String e = i < ge.length ? ge[i] : "<EOF>";
                if (!g.equals(e)) {
                    System.out.println("  L" + (i + 1) + " G:|" + g + "|");
                    System.out.println("  L" + (i + 1) + " O:|" + e + "|");
                    diffCount++;
                }
            }
            System.out.println();
        }
        System.out.println("Total mismatched: " + mismatchCount);
    }

    @Test
    @EnabledIf("cdmAvailable")
    void dump_all_remaining_enum_mismatches() throws IOException {
        List<RModel> models = new ArrayList<>();
        if (Files.isDirectory(BUILTINS_DIR)) {
            try (var stream = Files.list(BUILTINS_DIR)) {
                stream.filter(p -> p.toString().endsWith(".rosetta"))
                      .sorted()
                      .forEach(p -> {
                          try { models.add(AstBuilder.buildFromFile(p)); }
                          catch (Exception e) { System.err.println("  skip: " + e.getMessage()); }
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
                      } catch (Exception e) { System.err.println("  skip: " + e.getMessage()); }
                  });
        }

        var result = RWorkspace.build(models);
        var gm = new GeneratorModel(result.workspace());
        var enumGen = new EnumGenerator(gm);

        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : result.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                assertNoGenerationErrors(enumGen.generateClasses(model, gm.version(model), output));
            }
        }

        System.out.println("=== ALL REMAINING ENUM MISMATCHES ===\n");
        int mismatchCount = 0;
        for (var entry : output.entrySet()) {
            Path goldenPath = CDM_GOLDEN_DIR.resolve(entry.getKey());
            if (!Files.exists(goldenPath)) continue;
            String golden = normalize(Files.readString(goldenPath));
            String generated = normalize(entry.getValue());
            if (golden.equals(generated)) continue;

            mismatchCount++;
            String[] gl = golden.split("\n", -1);
            String[] ge = generated.split("\n", -1);
            int maxLines = Math.max(gl.length, ge.length);
            System.out.println("--- " + entry.getKey()
                    + " (golden=" + gl.length + " gen=" + ge.length + ") ---");
            for (int i = 0; i < maxLines; i++) {
                String g = i < gl.length ? gl[i] : "<EOF>";
                String e = i < ge.length ? ge[i] : "<EOF>";
                if (!g.equals(e)) {
                    System.out.println("  L" + (i + 1) + " G:|" + g + "|");
                    System.out.println("  L" + (i + 1) + " O:|" + e + "|");
                }
            }
            System.out.println();
        }
        System.out.println("Total mismatched: " + mismatchCount);
    }

    @Test
    @EnabledIf("cdmAvailable")
    void dump_function_diffs() throws IOException {
        List<RModel> models = new ArrayList<>();
        if (Files.isDirectory(BUILTINS_DIR)) {
            try (var stream = Files.list(BUILTINS_DIR)) {
                stream.filter(p -> p.toString().endsWith(".rosetta"))
                      .sorted()
                      .forEach(p -> {
                          try { models.add(AstBuilder.buildFromFile(p)); }
                          catch (Exception e) { System.err.println("  skip: " + e.getMessage()); }
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
                      } catch (Exception e) { System.err.println("  skip: " + e.getMessage()); }
                  });
        }

        var result = RWorkspace.build(models);
        var gm = new GeneratorModel(result.workspace());
        var funcGen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);

        Map<String, String> output = new LinkedHashMap<>();
        funcGen.generate(output);

        String[] samples = {
            "cdm/base/math/functions/Max.java",
            "cdm/legaldocumentation/csa/functions/DeliveryAmount.java",
            "cdm/event/qualification/functions/Qualify_CashTransfer.java",
            "cdm/ingest/fpml/confirmation/workflowstep/functions/MapWorkflowStep.java",
            "cdm/base/staticdata/codelist/functions/LoadCodeList.java",
            "cdm/margin/schedule/functions/StandardizedScheduleNotionalCurrency.java",
            "cdm/event/position/functions/FxMarkToMarket.java",
        };

        System.out.println("=== FUNCTION DIFF DIAGNOSTIC ===\n");
        for (String sample : samples) {
            String generated = output.get(sample);
            if (generated == null) {
                System.out.println("--- " + sample + " --- NOT GENERATED\n");
                continue;
            }
            Path goldenPath = CDM_GOLDEN_DIR.resolve(sample);
            if (!Files.exists(goldenPath)) {
                System.out.println("--- " + sample + " --- NO GOLDEN FILE\n");
                continue;
            }
            String golden = normalize(Files.readString(goldenPath));
            generated = normalize(generated);

            if (golden.equals(generated)) {
                System.out.println("--- " + sample + " --- MATCH\n");
                continue;
            }

            String[] gl = golden.split("\n", -1);
            String[] ge = generated.split("\n", -1);
            int maxLines = Math.max(gl.length, ge.length);
            int firstDiff = -1;
            for (int i = 0; i < maxLines; i++) {
                String g = i < gl.length ? gl[i] : "<EOF>";
                String e = i < ge.length ? ge[i] : "<EOF>";
                if (!g.equals(e)) { firstDiff = i; break; }
            }

            System.out.println("--- " + sample + " --- FIRST DIFF AT LINE " + (firstDiff + 1)
                    + " (golden=" + gl.length + " lines, generated=" + ge.length + " lines)");

            int diffCount = 0;
            for (int i = 0; i < maxLines && diffCount < 25; i++) {
                String g = i < gl.length ? gl[i] : "<EOF>";
                String e = i < ge.length ? ge[i] : "<EOF>";
                if (!g.equals(e)) {
                    System.out.println("  L" + (i + 1) + " G:|" + g + "|");
                    System.out.println("  L" + (i + 1) + " O:|" + e + "|");
                    diffCount++;
                }
            }
            System.out.println();
        }
    }

    @Test
    @EnabledIf("cdmAvailable")
    void dump_condition_rendering() throws IOException {
        List<RModel> models = new ArrayList<>();
        if (Files.isDirectory(BUILTINS_DIR)) {
            try (var stream = Files.list(BUILTINS_DIR)) {
                stream.filter(p -> p.toString().endsWith(".rosetta")).sorted().forEach(p -> {
                    try { models.add(AstBuilder.buildFromFile(p)); }
                    catch (Exception e) {}
                });
            }
        }
        try (var stream = Files.list(CDM_ROSETTA_DIR)) {
            stream.filter(p -> p.toString().endsWith(".rosetta")).sorted().forEach(p -> {
                try {
                    RModel model = AstBuilder.buildFromFile(p);
                    model.setVersion("0.0.0.master-SNAPSHOT");
                    models.add(model);
                } catch (Exception e) {}
            });
        }

        var result = RWorkspace.build(models);
        var gm = new GeneratorModel(result.workspace());
        var funcGen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);

        Map<String, String> output = new LinkedHashMap<>();
        funcGen.generate(output);

        // Focus on functions with conditions
        String[] conditionFunctions = {
            "cdm/legaldocumentation/csa/functions/DeliveryAmount.java",
            "cdm/margin/schedule/functions/StandardizedScheduleNotionalCurrency.java",
            "cdm/event/position/functions/FxMarkToMarket.java",
        };

        System.out.println("=== CONDITION RENDERING DIAGNOSTIC ===\n");
        for (String key : conditionFunctions) {
            String gen = output.get(key);
            if (gen == null) { System.out.println("--- " + key + " --- NOT GENERATED\n"); continue; }

            String[] lines = gen.split("\n");
            System.out.println("--- " + key + " ---");
            // Find and print lines containing condition-related content
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].contains("conditionValidator") || lines[i].contains("// pre-conditions")
                        || lines[i].contains("// post-conditions") || lines[i].contains("\"\")")
                        || lines[i].contains("validate(")) {
                    // Print 1 line before for context, the matching line, and 1 after
                    if (i > 0) System.out.println("  L" + i + ": " + lines[i-1]);
                    System.out.println("  L" + (i+1) + ": " + lines[i]);
                    if (i+1 < lines.length) System.out.println("  L" + (i+2) + ": " + lines[i+1]);
                    System.out.println("  ---");
                }
            }
            System.out.println();
        }
    }

    /**
     * FUNCTION mismatch taxonomy — classifies ALL 1,234 function mismatches
     * into import-vs-body categories so PR-A's actual expected D11 lift is
     * measurable before C3c.2's load-bearing delete.
     *
     * <p>Categories:
     * <ul>
     *   <li>IMPORTS_MISSING — golden has imports generated doesn't</li>
     *   <li>IMPORTS_EXTRA — generated has imports golden doesn't</li>
     *   <li>IMPORTS_ORDER — same import set, different ordering</li>
     *   <li>BODY_ONLY — import sets identical, body differs</li>
     * </ul>
     *
     * <p>"Imports" covers both regular (`import X;`) and static (`import
     * static X.*;`). Import section is defined as consecutive lines starting
     * with `import` at top of file, up to first non-import non-blank line.
     */
    @Test
    @EnabledIf("cdmAvailable")
    void classify_function_mismatches() throws IOException {
        List<RModel> models = new ArrayList<>();
        if (Files.isDirectory(BUILTINS_DIR)) {
            try (var stream = Files.walk(BUILTINS_DIR)) {
                stream.filter(Files::isRegularFile)
                      .filter(p -> p.toString().endsWith(".rosetta"))
                      .sorted()
                      .forEach(p -> {
                          try { models.add(AstBuilder.buildFromFile(p)); }
                          catch (Exception e) { System.err.println("  skip: " + e.getMessage()); }
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
                      } catch (Exception e) { System.err.println("  skip: " + e.getMessage()); }
                  });
        }

        var result = RWorkspace.build(models);
        var gm = new GeneratorModel(result.workspace());
        var funcGen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);

        Map<String, String> output = new LinkedHashMap<>();
        funcGen.generate(output);

        int total = 0, identical = 0;
        int importsMissing = 0, importsExtra = 0, importsOrder = 0, bodyOnly = 0, both = 0;
        List<String> importMissingSamples = new ArrayList<>();
        List<String> bodyOnlySamples = new ArrayList<>();

        for (var entry : output.entrySet()) {
            Path goldenPath = CDM_GOLDEN_DIR.resolve(entry.getKey());
            if (!Files.exists(goldenPath)) continue;
            total++;
            String golden = normalize(Files.readString(goldenPath));
            String generated = normalize(entry.getValue());
            if (golden.equals(generated)) {
                identical++;
                continue;
            }

            List<String> goldenImports = extractImports(golden);
            List<String> genImports = extractImports(generated);
            String goldenBody = stripImports(golden);
            String genBody = stripImports(generated);

            Set<String> goldenSet = new HashSet<>(goldenImports);
            Set<String> genSet = new HashSet<>(genImports);
            Set<String> missing = new HashSet<>(goldenSet); missing.removeAll(genSet);
            Set<String> extra = new HashSet<>(genSet); extra.removeAll(goldenSet);
            boolean sameSet = missing.isEmpty() && extra.isEmpty();
            boolean sameOrder = goldenImports.equals(genImports);
            boolean sameBody = goldenBody.equals(genBody);

            boolean hasImportDiff = !missing.isEmpty() || !extra.isEmpty() || !sameOrder;
            boolean hasBodyDiff = !sameBody;

            if (hasImportDiff && hasBodyDiff) {
                both++;
                if (!missing.isEmpty() && importMissingSamples.size() < 5) {
                    importMissingSamples.add(entry.getKey() + " — missing: " + missing);
                }
            } else if (!missing.isEmpty()) {
                importsMissing++;
                if (importMissingSamples.size() < 5) {
                    importMissingSamples.add(entry.getKey() + " — missing: " + missing);
                }
            } else if (!extra.isEmpty()) {
                importsExtra++;
            } else if (sameSet && !sameOrder) {
                importsOrder++;
            } else if (hasBodyDiff) {
                bodyOnly++;
                if (bodyOnlySamples.size() < 5) {
                    bodyOnlySamples.add(entry.getKey());
                }
            }
        }

        int mismatches = total - identical;
        System.out.println("\n=== FUNCTION MISMATCH TAXONOMY ===");
        System.out.println("Total:           " + total);
        System.out.println("Identical:       " + identical);
        System.out.println("Mismatches:      " + mismatches);
        System.out.println("  IMPORTS_MISSING:  " + importsMissing
                + " (PR-A fixable via C3c.2 structured refs)");
        System.out.println("  IMPORTS_EXTRA:    " + importsExtra);
        System.out.println("  IMPORTS_ORDER:    " + importsOrder
                + " (PR-A fixable via ImportCollector sort)");
        System.out.println("  BOTH:             " + both
                + " (partial PR-A fixable)");
        System.out.println("  BODY_ONLY:        " + bodyOnly
                + " (NOT PR-A scope — body-fix PR)");
        System.out.println();
        System.out.println("PR-A addressable upper bound: "
                + (importsMissing + importsExtra + importsOrder + both)
                + " of " + mismatches + " ("
                + (mismatches == 0 ? "0" : Math.round(100.0 * (importsMissing + importsExtra + importsOrder + both) / mismatches))
                + "%)");
        System.out.println();
        if (!importMissingSamples.isEmpty()) {
            System.out.println("Sample IMPORTS_MISSING cases:");
            importMissingSamples.forEach(s -> System.out.println("  " + s));
            System.out.println();
        }
        if (!bodyOnlySamples.isEmpty()) {
            System.out.println("Sample BODY_ONLY cases:");
            bodyOnlySamples.forEach(s -> System.out.println("  " + s));
        }
    }

    /**
     * Extract the import-block lines from a Java source. Imports are
     * consecutive lines at the top (after package + blank) starting with
     * {@code import} (both regular and static).
     */
    private static List<String> extractImports(String source) {
        List<String> imports = new ArrayList<>();
        String[] lines = source.split("\n", -1);
        boolean seenImport = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("import ")) {
                imports.add(trimmed);
                seenImport = true;
            } else if (seenImport && !trimmed.isEmpty()) {
                // End of import block
                break;
            }
        }
        return imports;
    }

    /**
     * Return the source with its import block removed. Used for body-only
     * diff comparison. Preserves the original line structure (via
     * {@code String.join}) so a trailing-newline difference between golden
     * and generated is not masked or amplified by this helper — the
     * taxonomy classifier must see the same body-equality result it would
     * see comparing the non-stripped sources.
     */
    private static String stripImports(String source) {
        String[] lines = source.split("\n", -1);
        List<String> kept = new ArrayList<>(lines.length);
        boolean inImportBlock = false;
        boolean importBlockDone = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (!importBlockDone && trimmed.startsWith("import ")) {
                inImportBlock = true;
                continue;
            }
            if (inImportBlock && !trimmed.isEmpty() && !trimmed.startsWith("import ")) {
                importBlockDone = true;
            }
            kept.add(line);
        }
        return String.join("\n", kept);
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
