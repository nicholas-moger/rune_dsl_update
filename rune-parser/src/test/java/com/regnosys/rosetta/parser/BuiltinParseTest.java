package com.regnosys.rosetta.parser;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * Tests that the two built-in .rosetta files from rune-runtime parse correctly.
 * These define the foundation types (boolean, number, string, date, etc.)
 * and built-in annotations (metadata, calculation, rootType, etc.).
 *
 * Looks in two locations:
 *   1. ../test-corpus/rune-dsl-builtins/rune-runtime/... (test corpus clone)
 *   2. ../rune-dsl/rune-runtime/... (local full clone)
 * Skips if neither is available.
 */
class BuiltinParseTest {

    private static final String[] SEARCH_ROOTS = {
        "../test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model/",
        "../rune-dsl/rune-runtime/src/main/resources/model/"
    };

    @ParameterizedTest
    @ValueSource(strings = {
        "basictypes.rosetta",
        "annotations.rosetta"
    })
    void parsesBuiltinFiles(String fileName) {
        Path filePath = null;
        for (String root : SEARCH_ROOTS) {
            Path candidate = Path.of(root + fileName);
            if (Files.exists(candidate)) {
                filePath = candidate;
                break;
            }
        }
        assumeTrue(filePath != null,
            "Skipping: " + fileName + " not found (neither test-corpus nor rune-dsl cloned)");
        RosettaParseResult result = RosettaParserFacade.parseFile(filePath);
        assertTrue(result.errors().isEmpty(),
            "Parse errors in " + filePath + ":\n" + String.join("\n", result.errors()));
    }
}
