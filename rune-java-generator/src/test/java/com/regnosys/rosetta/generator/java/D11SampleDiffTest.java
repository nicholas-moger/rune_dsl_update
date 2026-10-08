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
 * Diagnostic test: dumps detailed diffs for 5 representative early-mismatch files.
 */
class D11SampleDiffTest {

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
    void dump_5_representative_mismatches() throws IOException {
        // 5 files with early diffs (line < 30) - closest to matching
        String[] samples = {
            "cdm/base/datetime/functions/ConvertToAdjustableOrAdjustedOrRelativeDate.java",
            "cdm/base/datetime/functions/BusinessCenterHolidaysMultiple.java",
            "cdm/base/datetime/functions/GetAllBusinessCenters.java",
            "cdm/base/datetime/functions/ToDateTime.java",
            "cdm/base/datetime/functions/ConvertToAdjustableOrRelativeDate.java"
        };

        // Load full corpus. Diagnostic-only: parse failures are logged (not thrown)
        // so the test can still dump diffs for the samples that DID load. Silent
        // swallowing was flagged by Copilot R21 M3 — a missing/renamed model
        // upstream would otherwise make "sample not generated" look like a
        // codegen bug rather than an input-corpus problem.
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

        for (String sample : samples) {
            if (!output.containsKey(sample)) {
                System.out.println("\n=== " + sample + " ===");
                System.out.println("NOT GENERATED");
                continue;
            }

            Path goldenPath = CDM_GOLDEN_DIR.resolve(sample);
            if (!Files.exists(goldenPath)) {
                System.out.println("\n=== " + sample + " ===");
                System.out.println("NO GOLDEN");
                continue;
            }

            String golden = normalize(Files.readString(goldenPath));
            String generated = normalize(output.get(sample));

            if (golden.equals(generated)) {
                System.out.println("\n=== " + sample + " ===");
                System.out.println("MATCH");
                continue;
            }

            System.out.println("\n=== " + sample + " ===");
            String[] gl = golden.split("\n", -1);
            String[] ge = generated.split("\n", -1);

            // Find first diff
            int firstDiff = 0;
            int maxLines = Math.max(gl.length, ge.length);
            for (int i = 0; i < maxLines; i++) {
                String g = i < gl.length ? gl[i] : "<EOF>";
                String e = i < ge.length ? ge[i] : "<EOF>";
                if (!g.equals(e)) {
                    firstDiff = i + 1;
                    break;
                }
            }
            System.out.println("First diff at line: " + firstDiff);

            // Show context around first diff (5 lines before, 10 after)
            int start = Math.max(0, firstDiff - 6);
            int end = Math.min(Math.max(gl.length, ge.length), firstDiff + 9);

            System.out.println("\nGOLDEN:");
            for (int i = start; i < end; i++) {
                String line = i < gl.length ? gl[i] : "<EOF>";
                String marker = (i + 1 == firstDiff) ? ">>>" : "   ";
                System.out.printf("%s %3d: %s\n", marker, i + 1, line);
            }

            System.out.println("\nGENERATED:");
            for (int i = start; i < end; i++) {
                String line = i < ge.length ? ge[i] : "<EOF>";
                String marker = (i + 1 == firstDiff) ? ">>>" : "   ";
                System.out.printf("%s %3d: %s\n", marker, i + 1, line);
            }
        }
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    /** Diagnostic-only: record a corpus-load failure so the sample-not-generated
     *  trace in test output is correlated to the source cause. */
    private static void logParseFailure(Path p, Exception e) {
        System.err.println("D11SampleDiffTest: failed to parse "
                + p.getFileName() + " — " + e.getClass().getSimpleName()
                + ": " + e.getMessage());
    }
}
