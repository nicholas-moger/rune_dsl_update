package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Analyzes mismatch patterns across 1235 mismatched functions.
 */
class D11MismatchAnalysisTest {

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
    void categorize_all_mismatches() throws IOException {
        // Diagnostic-only: parse failures are logged (not thrown). Silent
        // swallowing was flagged by Copilot R21 M3 — a missing/renamed model
        // upstream would otherwise skew the mismatch-category counts without
        // any indication why.
        List<RModel> models = new ArrayList<>();
        if (Files.isDirectory(BUILTINS_DIR)) {
            try (var stream = Files.list(BUILTINS_DIR)) {
                stream.filter(p -> p.toString().endsWith(".rosetta"))
                      .sorted()
                      .forEach(p -> {
                          try { models.add(AstBuilder.buildFromFile(p)); }
                          catch (Exception e) { logParseFailure(p, e); }
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
                      } catch (Exception e) { logParseFailure(p, e); }
                  });
        }

        var result = RWorkspace.build(models);
        var gm = new GeneratorModel(result.workspace());
        var funcGen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);

        Map<String, String> output = new LinkedHashMap<>();
        funcGen.generate(output);

        // Track all mismatches by category
        Map<String, Integer> categoryCount = new LinkedHashMap<>();
        categoryCount.put("GENERATED_NOT_IN_GOLDEN", 0);
        categoryCount.put("MISSING_IMPORTS", 0);
        categoryCount.put("EXTRA_IMPORTS", 0);
        categoryCount.put("WRONG_IMPORT_ORDER", 0);
        categoryCount.put("MISSING_STATIC_IMPORT", 0);
        categoryCount.put("BODY_DIFF", 0);
        categoryCount.put("OTHER_DIFF", 0);

        List<String> missingImportFiles = new ArrayList<>();
        List<String> extraImportFiles = new ArrayList<>();
        List<String> bodyDiffFiles = new ArrayList<>();

        for (var entry : output.entrySet()) {
            Path goldenPath = CDM_GOLDEN_DIR.resolve(entry.getKey());
            if (!Files.exists(goldenPath)) {
                categoryCount.put("GENERATED_NOT_IN_GOLDEN", categoryCount.get("GENERATED_NOT_IN_GOLDEN") + 1);
                continue;
            }

            String golden = normalize(Files.readString(goldenPath));
            String generated = normalize(entry.getValue());

            if (golden.equals(generated)) {
                continue; // Match
            }

            // Analyze the diff
            String[] gl = golden.split("\n", -1);
            String[] ge = generated.split("\n", -1);

            boolean hasMissingImport = false;
            boolean hasExtraImport = false;
            boolean hasMissingStaticImport = false;
            boolean hasBodyDiff = false;
            boolean inImportSection = true;
            int firstDiffLine = 0;

            int maxLines = Math.max(gl.length, ge.length);
            for (int i = 0; i < maxLines; i++) {
                String g = i < gl.length ? gl[i] : "<EOF>";
                String e = i < ge.length ? ge[i] : "<EOF>";
                
                if (!g.equals(e)) {
                    if (firstDiffLine == 0) firstDiffLine = i + 1;
                    
                    // Categorize
                    if (inImportSection && (g.trim().startsWith("import ") || e.trim().startsWith("import "))) {
                        if (g.trim().startsWith("import ") && !e.trim().startsWith("import ")) {
                            hasMissingImport = true;
                        } else if (!g.trim().startsWith("import ") && e.trim().startsWith("import ")) {
                            hasExtraImport = true;
                        }
                    } else if (inImportSection && g.trim().startsWith("import static")) {
                        if (!e.contains("import static")) {
                            hasMissingStaticImport = true;
                        }
                    } else {
                        inImportSection = false;
                        hasBodyDiff = true;
                    }
                }
                
                if (i > 20 && !g.trim().startsWith("import ") && !e.trim().startsWith("import ")) {
                    inImportSection = false;
                }
            }

            // Classify
            if (firstDiffLine <= 30) { // Early diffs are import-related
                if (hasMissingImport) {
                    categoryCount.put("MISSING_IMPORTS", categoryCount.get("MISSING_IMPORTS") + 1);
                    missingImportFiles.add(entry.getKey() + " (line " + firstDiffLine + ")");
                } else if (hasExtraImport) {
                    categoryCount.put("EXTRA_IMPORTS", categoryCount.get("EXTRA_IMPORTS") + 1);
                    extraImportFiles.add(entry.getKey() + " (line " + firstDiffLine + ")");
                } else if (hasMissingStaticImport) {
                    categoryCount.put("MISSING_STATIC_IMPORT", categoryCount.get("MISSING_STATIC_IMPORT") + 1);
                } else {
                    categoryCount.put("WRONG_IMPORT_ORDER", categoryCount.get("WRONG_IMPORT_ORDER") + 1);
                }
            } else if (hasBodyDiff) {
                categoryCount.put("BODY_DIFF", categoryCount.get("BODY_DIFF") + 1);
                bodyDiffFiles.add(entry.getKey() + " (line " + firstDiffLine + ")");
            } else {
                categoryCount.put("OTHER_DIFF", categoryCount.get("OTHER_DIFF") + 1);
            }
        }

        System.out.println("\n=== MISMATCH CATEGORY SUMMARY ===");
        categoryCount.forEach((cat, count) -> System.out.println(cat + ": " + count));

        System.out.println("\n=== SAMPLE MISSING IMPORT FILES (first 10) ===");
        missingImportFiles.stream().limit(10).forEach(System.out::println);

        System.out.println("\n=== SAMPLE EXTRA IMPORT FILES (first 10) ===");
        extraImportFiles.stream().limit(10).forEach(System.out::println);

        System.out.println("\n=== SAMPLE BODY DIFF FILES (first 10) ===");
        bodyDiffFiles.stream().limit(10).forEach(System.out::println);
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    /** Diagnostic-only: record a corpus-load failure so miscounted categories
     *  in test output are correlated to the source cause. */
    private static void logParseFailure(Path p, Exception e) {
        System.err.println("D11MismatchAnalysisTest: failed to parse "
                + p.getFileName() + " — " + e.getClass().getSimpleName()
                + ": " + e.getMessage());
    }
}
