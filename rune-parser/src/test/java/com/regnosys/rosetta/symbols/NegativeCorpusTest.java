package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs every .rosetta file under {@code resources/symbols/negative/} through
 * the M3 linker and asserts that the diagnostics produced match the
 * expected category + line written in the paired .expected file.
 *
 * <p>Spec: D5/E15 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
class NegativeCorpusTest {

    private static final Path NEGATIVE_DIR = Paths.get("src/test/resources/symbols/negative");

    @TestFactory
    Stream<DynamicTest> negative_corpus_files_produce_expected_diagnostics() throws IOException {
        if (!Files.exists(NEGATIVE_DIR)) {
            return Stream.empty();
        }
        try (Stream<Path> files = Files.list(NEGATIVE_DIR)) {
            return files
                .filter(p -> p.toString().endsWith(".rosetta"))
                .sorted()
                .map(this::makeTest)
                .toList()
                .stream();
        }
    }

    private DynamicTest makeTest(Path rosettaFile) {
        return DynamicTest.dynamicTest(rosettaFile.getFileName().toString(), () -> {
            String source = Files.readString(rosettaFile);
            Path expectedFile = rosettaFile.resolveSibling(
                rosettaFile.getFileName().toString().replace(".rosetta", ".expected"));
            assertTrue(Files.exists(expectedFile),
                "Missing .expected file for " + rosettaFile.getFileName());
            Expected expected = Expected.parse(Files.readString(expectedFile));

            RModel model = AstBuilder.buildFromString(source, rosettaFile.getFileName().toString());
            RLinkingResult result = RWorkspace.build(List.of(model));

            boolean found = result.linkingDiagnostics().stream()
                .anyMatch(d -> d.category() == expected.category
                            && d.range().startLine() == expected.line);
            if (!found) {
                fail("expected " + expected.category + " at line " + expected.line
                   + " in " + rosettaFile.getFileName()
                   + "; actual diagnostics: " + result.linkingDiagnostics());
            }
        });
    }

    private record Expected(DiagnosticCategory category, int line) {
        static Expected parse(String text) {
            DiagnosticCategory cat = null;
            int line = -1;
            for (String l : text.split("\n")) {
                l = l.trim();
                if (l.startsWith("category=")) cat = DiagnosticCategory.valueOf(l.substring(9).trim());
                else if (l.startsWith("line=")) line = Integer.parseInt(l.substring(5).trim());
            }
            return new Expected(Objects.requireNonNull(cat, "category missing"), line);
        }
    }
}
