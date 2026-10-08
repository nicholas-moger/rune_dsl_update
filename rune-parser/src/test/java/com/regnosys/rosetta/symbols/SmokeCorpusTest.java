package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.symbols.linker.ResolutionAudit;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs the M3 linker over the hand-picked smoke corpus and asserts
 * every file links cleanly, the resolution audit passes, and the
 * linker invariants hold. Total runtime under 30 seconds.
 *
 * <p>Spec: D5/E3 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
class SmokeCorpusTest {

    private static final Path SMOKE_DIR = Paths.get("src/test/resources/symbols/smoke-corpus");

    @Test
    void smoke_corpus_links_cleanly() throws IOException {
        if (!Files.exists(SMOKE_DIR)) {
            System.out.println("Skipping — smoke corpus not present");
            return;
        }

        long start = System.currentTimeMillis();

        // Load all smoke files
        List<RModel> allFiles = new ArrayList<>();
        try (Stream<Path> stream = Files.list(SMOKE_DIR)) {
            stream.filter(p -> p.toString().endsWith(".rosetta"))
                  .sorted()
                  .forEach(p -> {
                      try {
                          String src = Files.readString(p);
                          allFiles.add(AstBuilder.buildFromString(src, p.getFileName().toString()));
                      } catch (IOException e) {
                          throw new RuntimeException(e);
                      }
                  });
        }

        assertFalse(allFiles.isEmpty(), "smoke corpus should have files");

        // Build one workspace from all smoke files
        RLinkingResult result = RWorkspace.build(allFiles);

        // Audit must pass
        ResolutionAudit.assertFullyHandled(result.workspace());

        // Invariants must hold
        List<String> violations = LinkerInvariants.checkAll(result.workspace());
        assertTrue(violations.isEmpty(),
            "smoke corpus violates invariants: " + violations);

        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 30_000,
            "smoke corpus must run in under 30s, took " + elapsed + "ms");
        System.out.println("Smoke corpus: " + allFiles.size() + " files linked in " + elapsed + "ms");
    }
}
