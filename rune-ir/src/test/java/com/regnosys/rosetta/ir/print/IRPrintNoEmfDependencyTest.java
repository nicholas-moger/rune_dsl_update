package com.regnosys.rosetta.ir.print;

import com.regnosys.rosetta.testutil.CorpusWalker;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Guards the L-029 neutrality rule (spec Tests bullet): no Xtext / EMF / Ecore /
 * {@code rune-java-generator} type may appear in {@code ir.print} <em>or the IR's
 * public surface</em> ({@code ir.core}, {@code ir.expr}, {@code ir.adapter}). A
 * plain source scan (no ArchUnit dependency, per simplicity-first). The IR
 * surface is EMF-free today, so this is a guard that must STAY green, not a fix.
 *
 * <p>The scan inspects only lines beginning with {@code import } — an inline
 * fully-qualified class name (e.g. in a cast or {@code catch}) would not be
 * caught; this is an accepted limitation under the simplicity-first no-ArchUnit
 * decision.
 */
class IRPrintNoEmfDependencyTest {

    private static final List<String> FORBIDDEN =
            List.of("org.eclipse", "xtext", "emf", "ecore", "generator.java");

    /** ir.print, ir.json, ir.emit, plus the IR public surface the spec names. */
    private static final List<String> SCANNED_PACKAGES = List.of(
            "src/main/java/com/regnosys/rosetta/ir/print",
            "src/main/java/com/regnosys/rosetta/ir/json",
            "src/main/java/com/regnosys/rosetta/ir/emit",
            "src/main/java/com/regnosys/rosetta/ir/core",
            "src/main/java/com/regnosys/rosetta/ir/expr",
            "src/main/java/com/regnosys/rosetta/ir/adapter");

    @Test
    void noForbiddenImportsInIrSurface() throws Exception {
        Path moduleRoot = CorpusWalker.moduleRoot();
        for (String rel : SCANNED_PACKAGES) {
            Path dir = moduleRoot.resolve(rel);
            try (Stream<Path> files = Files.walk(dir)) {
                files.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                    try {
                        for (String l : Files.readAllLines(p)) {
                            String line = l.strip();
                            if (line.startsWith("import ")) {
                                String lower = line.toLowerCase();
                                for (String bad : FORBIDDEN) {
                                    assertFalse(lower.contains(bad),
                                        p.getFileName() + " has forbidden import: " + line);
                                }
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }
}
