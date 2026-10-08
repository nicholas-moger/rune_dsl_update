package com.regnosys.rosetta.parser.snippet;

import com.regnosys.rosetta.parser.RosettaParserFacade;
import com.regnosys.rosetta.parser.RosettaParseResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Parameterized test that auto-discovers all .rosetta snippet files and parses each one.
 * New snippets added under src/test/resources/snippets/ are automatically included.
 */
class SnippetParseTest {

    static Stream<Path> rosettaSnippetFiles() throws IOException {
        List<Path> files;
        try (Stream<Path> paths = Files.walk(Path.of("src/test/resources/snippets"))) {
            files = paths
                .filter(p -> p.toString().endsWith(".rosetta"))
                .sorted()
                .toList();
        }
        return files.stream();
    }

    @ParameterizedTest
    @MethodSource("rosettaSnippetFiles")
    void parsesAllSnippetsWithoutErrors(Path path) {
        RosettaParseResult result = RosettaParserFacade.parseFile(path);
        assertTrue(result.errors().isEmpty(),
            "Parse errors in " + path + ":\n" + String.join("\n", result.errors()));
        assertNotNull(result.tree());
    }
}
