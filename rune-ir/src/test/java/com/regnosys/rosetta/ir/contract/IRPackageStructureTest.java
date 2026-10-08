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
 * Locks the package-direction invariant: {@code .rune} may import
 * {@code .core} but {@code .core} must NOT import {@code .rune}. External
 * cross-DSL importers (W22) depend on {@code .core} only.
 *
 * <p><b>Implementation note (per spec Section 7 reviewer fix I2):</b> uses
 * {@link String#contains(CharSequence)} on the source-file text to scan for
 * the disallowed import string. <b>NO regex.</b> Hand-written Java imports
 * are literal strings, comparable to the "fixed copyright headers"
 * exception to the project's no-regex-on-structured-content rule.
 *
 * <p>Path resolution uses {@link CorpusWalker#moduleRoot()} so the scan is
 * independent of JVM working directory.
 */
class IRPackageStructureTest {

    private static final Path CORE_DIR = CorpusWalker.moduleRoot()
            .resolve("src/main/java/com/regnosys/rosetta/ir/core");

    /**
     * Trailing {@code .} is intentional — matches sub-package imports like
     * {@code import com.regnosys.rosetta.ir.rune.IRReport} but does NOT
     * match a hypothetical type-as-import {@code com.regnosys.rosetta.ir.rune}
     * (which would be invalid Java anyway). Keeps the literal scan precise.
     */
    private static final String DISALLOWED_IMPORT =
            "import com.regnosys.rosetta.ir.rune.";

    @Test
    void coreMustNotImportRune() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(CORE_DIR)) {
            for (Path java : (Iterable<Path>) files
                    .filter(p -> p.toString().endsWith(".java"))::iterator) {
                String content = Files.readString(java);
                // Literal scan — String.contains, NOT Pattern.compile.
                if (content.contains(DISALLOWED_IMPORT)) {
                    violations.add(java.toString());
                }
            }
        }
        assertEquals(List.of(), violations,
                "Files in com.regnosys.rosetta.ir.core import com.regnosys.rosetta.ir.rune. " +
                        "Per W22 DSL-agnostic constraint, .core must not depend on .rune.");
    }
}
