package com.regnosys.rosetta.ir.contract;

import com.regnosys.rosetta.testutil.CorpusWalker;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the production dependency surface of {@code com.regnosys.rosetta.ir.json}: it may import only
 * the IR public surface ({@code ir.core}, {@code ir.expr}, {@code ir.adapter}), the inferred-type
 * model ({@code com.regnosys.rosetta.types}), and the JDK. The serializer reads via the {@code ir.core}
 * interfaces; the deserializer additionally constructs the {@code ir.adapter} records (a conscious,
 * documented read-vs-write asymmetry — see the step-2b spec). Any new non-allowlisted dependency
 * (a third-party JSON library, Xtext/EMF, the Java generator) fails this test.
 */
class IRJsonDependencyTest {

    private static final Path JSON_DIR = CorpusWalker.moduleRoot()
            .resolve("src/main/java/com/regnosys/rosetta/ir/json");

    // Allowed import prefixes for production ir.json classes (JDK imports start with "java.").
    private static final List<String> ALLOWED = List.of(
            "import com.regnosys.rosetta.ir.core.",
            "import com.regnosys.rosetta.ir.expr.",
            "import com.regnosys.rosetta.ir.adapter.",
            "import com.regnosys.rosetta.types.",
            "import java.");

    @Test
    void irJsonImportsOnlyTheAllowedSurface() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(JSON_DIR)) {
            for (Path java : (Iterable<Path>) files
                    .filter(p -> p.toString().endsWith(".java"))::iterator) {
                for (String line : Files.readAllLines(java)) {
                    String trimmed = line.strip();
                    if (trimmed.startsWith("import ") && !isAllowed(trimmed)) {
                        violations.add(java.getFileName() + ": " + trimmed);
                    }
                }
            }
        }
        assertEquals(List.of(), violations,
                "com.regnosys.rosetta.ir.json imported a non-allowlisted dependency. "
                        + "If intended, update ALLOWED (and the step-2b spec). Found: " + violations);
    }

    private static boolean isAllowed(String importLine) {
        return ALLOWED.stream().anyMatch(importLine::startsWith);
    }
}
