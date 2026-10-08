package com.regnosys.rosetta.ast;

import com.regnosys.rosetta.ast.builder.AstBuildException;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.parser.ChaosParseExpectations;
import com.regnosys.rosetta.testutil.CorpusWalker;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Corpus-occurrence guard (restored in the review export from the retired corpus audit test,
 * separated from that test's comparison against a development audit document).
 *
 * <p>Walks every parseable {@code .rosetta} file under the real-model cell directories staged
 * under {@code test-corpus/} (a cell's own test models included) plus the two builtin models, and
 * nothing else: the authored chaos cell is
 * excluded, as the original audit excluded it, and so are the test fixtures that a full rune-dsl
 * checkout under {@code test-corpus/rune-dsl-builtins} carries. It asserts that every
 * {@code RRootElement} subclass registered in {@link CorpusExpected#EXPECTED} occurs at least once:
 * a registered subclass that no real model produces is a dead subclass or a stale registry.
 *
 * <p>Prerequisites: the corpus population the registry was recorded over is the full catalogue
 * (see {@code test-corpus/corpus-cells.tsv}); with a partial corpus some regulatory kinds
 * (reports, rules, bodies, corpora, segments) may have no occurrence and the test then fails by
 * design, naming the missing subclasses. It skips when no real-model cell is staged (builtins alone are not a real-model population).
 */
class RRootElementCorpusOccurrenceTest {

    @Test
    void everyExpectedRootSubclassOccursInTheStagedRealModels() throws IOException {
        assumeTrue(CorpusWalker.corpusExists(), "test-corpus directory not found - occurrence check skipped");

        // The population is the real-model cells plus the two builtin models, nothing else. A
        // real-model file lies under a corpus cell of test-corpus/ but NOT under the shared builtins
        // clone (test-corpus/rune-dsl-builtins is a whole rune-dsl checkout whose own test fixtures
        // are .rosetta files too); the builtin models are that clone's rune-runtime model directory
        // or the rune-dsl worktree's (CorpusWalker.BUILTINS_DIR). The chaos cell is skipped, as the
        // original audit skipped it; every other file the walker yields is left out of the tally.
        Path corpusDir = CorpusWalker.CORPUS_DIR.toAbsolutePath();
        Path builtinsClone = corpusDir.resolve("rune-dsl-builtins");
        Path builtinsCloneModels = builtinsClone.resolve("rune-runtime/src/main/resources/model");
        Path builtinsWorktreeModels = CorpusWalker.BUILTINS_DIR.toAbsolutePath();
        Map<String, Integer> totalByClass = new TreeMap<>();
        int walked = 0;
        int realModelFiles = 0;
        for (Path file : CorpusWalker.allRosettaFiles()) {
            if (ChaosParseExpectations.isChaosPath(file)) {
                continue;
            }
            Path abs = file.toAbsolutePath();
            boolean realModel = abs.startsWith(corpusDir) && !abs.startsWith(builtinsClone);
            boolean builtinModel = abs.startsWith(builtinsCloneModels) || abs.startsWith(builtinsWorktreeModels);
            if (!realModel && !builtinModel) {
                continue;
            }
            walked++;
            if (realModel) {
                realModelFiles++;
            }
            String content = Files.readString(file);
            try {
                RModel model = AstBuilder.buildFromString(content, file.toString());
                AstWalker.walk(model, node -> {
                    if (node instanceof RRootElement re) {
                        totalByClass.merge(re.getClass().getName(), 1, Integer::sum);
                    }
                });
            } catch (AstBuildException e) {
                // parse-error files are covered by AstCorpusRegressionTest
            }
        }
        assumeTrue(realModelFiles > 0, "no real-model cell staged (builtins only) - occurrence check skipped");

        List<String> absent = new ArrayList<>();
        for (String fqn : CorpusExpected.EXPECTED) {
            if (totalByClass.getOrDefault(fqn, 0) == 0) {
                absent.add(fqn);
            }
        }
        assertTrue(absent.isEmpty(),
                "RRootElement subclasses registered in CorpusExpected.EXPECTED with 0 occurrences in the "
                        + walked + " walked files of the real-model cells plus the builtin models (a dead subclass, "
                        + "a stale registry, or a partial "
                        + "corpus - see docs/TESTING.md): " + absent);
    }
}
