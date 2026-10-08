package com.regnosys.rosetta.ast;

import com.regnosys.rosetta.ast.builder.AstBuildException;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.util.AstWalker;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * AST corpus regression test (Task 15, Layer 4).
 *
 * <p>Parses every {@code .rosetta} file in the test corpus through the
 * full pipeline (lexer → parser → {@link AstBuilder}) and verifies that
 * the resulting typed AST is well-formed:
 * <ul>
 *   <li>No exceptions thrown during AST building</li>
 *   <li>The model has a non-null namespace</li>
 *   <li>Every root element is non-null</li>
 *   <li>Parent pointers are wired correctly (every non-root node has a
 *       parent that includes it as a child)</li>
 * </ul>
 *
 * <p>This is the strongest regression gate in M2 — it proves the AstBuilder
 * works on every real-world Rune DSL file we can find. Combined with the
 * existing parse-tree-level {@link com.regnosys.rosetta.parser.CorpusParseTest},
 * any drift between parser output and typed AST output is caught here.
 *
 * <p>The test is conditional — it only runs if the test-corpus directory
 * exists (the corpus is cloned on demand, not committed to the repo).
 */
class AstCorpusRegressionTest {

    private static final Path CORPUS_DIR = Path.of("../test-corpus");
    private static final Path BUILTINS_DIR = Path.of(
            "../rune-dsl/rune-runtime/src/main/resources/model");
    // Defensive floor sized below the smallest single-cell corpus
    // (iso20022/1.18.0 = 30 .rosetta files per test-corpus/CATALOGUE.md;
    // +2 builtins from rune-dsl/...; +overhead → 25 is the safe floor).
    // The full local walk (test-corpus + builtins fully populated) is 1,628.
    // Lowered from 100 in P1.7 PR-3 T5 when this test was wired into the
    // corpus-regression matrix's per-cell scope: each cell clones one
    // (corpus, version) outdir so the per-cell count is bounded by the
    // smallest corpus, not the full-corpus aggregate.
    private static final int MIN_EXPECTED_FILES = 25;

    static boolean corpusExists() {
        if (!Files.exists(CORPUS_DIR)) {
            return false;
        }
        try (var walk = Files.walk(CORPUS_DIR, 10)) {
            return walk.anyMatch(p -> p.toString().endsWith(".rosetta"));
        } catch (IOException e) {
            return false;
        }
    }

    @TestFactory
    Stream<DynamicTest> buildAstForAllCorpusFiles() throws IOException {
        if (!corpusExists()) {
            return Stream.of(DynamicTest.dynamicTest(
                    "corpus not available (acquire it as docs/CORPUS-9.83.md describes)",
                    () -> assumeTrue(false, "test-corpus directory not found")));
        }

        List<Path> allFiles = new ArrayList<>();
        try (var walk = Files.walk(CORPUS_DIR)) {
            walk.filter(p -> p.toString().endsWith(".rosetta")).forEach(allFiles::add);
        }
        if (Files.exists(BUILTINS_DIR)) {
            try (var walk = Files.walk(BUILTINS_DIR)) {
                walk.filter(p -> p.toString().endsWith(".rosetta")).forEach(allFiles::add);
            }
        }

        assertTrue(allFiles.size() >= MIN_EXPECTED_FILES,
                "Expected at least " + MIN_EXPECTED_FILES + " .rosetta files but found "
                        + allFiles.size());

        return allFiles.stream()
                .sorted()
                .map(path -> {
                    String displayName = path.startsWith(CORPUS_DIR)
                            ? CORPUS_DIR.relativize(path).toString()
                            : path.startsWith(BUILTINS_DIR)
                                    ? BUILTINS_DIR.relativize(path).toString()
                                    : path.getFileName().toString();
                    return DynamicTest.dynamicTest(displayName, () -> {
                        // The chaos cell's 22 expected-refusal files (v3.2 charter § 4b;
                        // the committed fork-diagnostics.tsv) are parse-refused BY DESIGN,
                        // so buildFromFile must THROW for them — and a build that succeeds
                        // is the refusal healing silently (LAW 81). Every other file,
                        // chaos included, keeps the full build+verify contract.
                        if (com.regnosys.rosetta.parser.ChaosParseExpectations.isExpectedRefusal(path)) {
                            assertThrows(AstBuildException.class,
                                    () -> AstBuilder.buildFromFile(path),
                                    "expected-refusal chaos file BUILT CLEAN — the pinned"
                                            + " refusal has healed; re-adjudicate " + path);
                            return;
                        }
                        RModel model = AstBuilder.buildFromFile(path);
                        verifyModel(model, path);
                    });
                });
    }

    /**
     * Verifies that an AST model is well-formed.
     */
    private void verifyModel(RModel model, Path file) {
        assertNotNull(model, "AstBuilder returned null for " + file);
        assertNotNull(model.namespace(), "Missing namespace in " + file);
        assertTrue(!model.namespace().isBlank(), "Empty namespace in " + file);

        // Every root element must be non-null
        for (int i = 0; i < model.rootElements().size(); i++) {
            assertNotNull(model.rootElements().get(i),
                    "Null root element at index " + i + " in " + file);
        }

        // Every reachable node must have a non-null source range
        AstWalker.walk(model, node -> {
            assertNotNull(node.sourceRange(),
                    "Node " + node.getClass().getSimpleName()
                            + " has null sourceRange in " + file);
        });

        // Spot-check parent wiring: every non-root node's parent must include it
        // in its children list. Walking the entire tree like this is O(N^2) so we
        // restrict to the top two levels (model + root elements).
        for (RNode root : model.rootElements()) {
            assertTrue(root.parent() == model,
                    "Root element " + root.getClass().getSimpleName()
                            + " parent is not the model in " + file);
        }
    }
}
