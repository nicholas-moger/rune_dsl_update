package com.regnosys.rosetta.harness.jqwik;

import com.regnosys.rosetta.parser.RosettaLexer;
import com.regnosys.rosetta.parser.RosettaParseResult;
import com.regnosys.rosetta.parser.RosettaParser;
import com.regnosys.rosetta.parser.RosettaParserFacade;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Dev-only utility that rebuilds
 * {@code rune-parser/src/test/resources/property-weights.properties}
 * from a target corpus.
 *
 * <p>P1.2 audit hook H18.2. The generator biases ATN transition picks
 * by rule-level weights; the weights must reflect actual corpus
 * frequencies, not guesses, or the "corpus-weighted" claim is just
 * decoration. This test parses every {@code .rosetta} file under the
 * configured corpus root, walks each parse tree, tallies each rule
 * invocation, and writes the resulting counts out as a properties file
 * keyed by rule name.
 *
 * <h2>How to re-run</h2>
 * This test is gated by a system property so routine {@code mvn test}
 * runs stay fast — scanning the corpus is not a correctness check, and
 * the committed properties file is a versioned snapshot, not a generated
 * artefact. To regenerate locally:
 *
 * <ol>
 *   <li>Ensure the target corpus is cloned to the path below (or edit
 *       {@link #CORPUS_ROOT} / {@link #OUTPUT_FILE}).</li>
 *   <li>Run: {@code mvn test -Dtest=CorpusRuleCounter -Dweights.regen=true}.</li>
 *   <li>Commit the regenerated {@code property-weights.properties}
 *       as its own change.</li>
 * </ol>
 *
 * <h2>Corpus choice</h2>
 * CDM 6.16.0 is the default target — it's the largest current-release
 * CDM corpus, covers all four CDM projects, and is the corpus whose
 * numbers anchored pre-rebaseline measurements. Other corpora can be
 * substituted by changing {@link #CORPUS_ROOT}.
 *
 * <h2>Method</h2>
 * Every {@link RuleContext} node in every parse tree contributes one
 * tally to its rule name. Only top-level rule names (via
 * {@link RosettaParser#getRuleNames()} indexed by
 * {@link RuleContext#getRuleIndex()}) are written; label-scoped rule
 * names (e.g. {@code LiteralExprContext} as a subclass of
 * {@code ExpressionContext}) collapse onto their parent rule — that's
 * the right granularity for {@link RuleWeights}, which weights by
 * rule-index, not by labelled alternative.
 */
final class CorpusRuleCounter {

    /** Default corpus root. Relative to the module dir. */
    private static final Path CORPUS_ROOT =
            Path.of("..", "test-corpus", "cdm", "cdm-6.16.0").toAbsolutePath().normalize();

    /** Where the generated properties file is written. */
    private static final Path OUTPUT_FILE =
            Path.of("src", "test", "resources", "property-weights.properties").toAbsolutePath().normalize();

    @Test
    @EnabledIfSystemProperty(named = "weights.regen", matches = "true",
            disabledReason = "Dev-only regeneration utility — run with -Dweights.regen=true")
    void regenerate_weights_from_corpus() throws IOException {
        if (!Files.isDirectory(CORPUS_ROOT)) {
            throw new IllegalStateException(
                    "corpus root not found: " + CORPUS_ROOT
                            + " — clone the target corpus or edit CORPUS_ROOT in this test");
        }

        Map<String, Long> countsByRule = new HashMap<>();
        String[] ruleNames = parserRuleNames();
        long[] fileAndNodeCounts = walkCorpus(CORPUS_ROOT, ruleNames, countsByRule);

        writeWeightsFile(OUTPUT_FILE, CORPUS_ROOT, fileAndNodeCounts, countsByRule);
    }

    /**
     * Walk every {@code .rosetta} file under {@code root}, tally each
     * rule-invocation into {@code countsByRule}. Returns a 2-element
     * array {@code [files, totalNodes]} for the output file's header.
     */
    private static long[] walkCorpus(Path root, String[] ruleNames, Map<String, Long> countsByRule)
            throws IOException {
        long files = 0;
        long totalNodes = 0;
        try (Stream<Path> stream = Files.walk(root)) {
            var iterator = stream.filter(p -> p.toString().endsWith(".rosetta")).iterator();
            while (iterator.hasNext()) {
                Path file = iterator.next();
                RosettaParseResult result = RosettaParserFacade.parseFile(file);
                if (result.tree() == null) continue;
                files++;
                totalNodes += tallyTree(result.tree(), ruleNames, countsByRule);
            }
        }
        return new long[] { files, totalNodes };
    }

    /**
     * Recurse over {@code node} and its descendants; increment the tally
     * for each {@link RuleContext} node encountered. Returns the number
     * of rule nodes counted.
     */
    private static long tallyTree(ParseTree node, String[] ruleNames, Map<String, Long> countsByRule) {
        long count = 0;
        if (node instanceof RuleContext ctx) {
            int ruleIndex = ctx.getRuleIndex();
            if (ruleIndex >= 0 && ruleIndex < ruleNames.length) {
                countsByRule.merge(ruleNames[ruleIndex], 1L, Long::sum);
                count++;
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            count += tallyTree(node.getChild(i), ruleNames, countsByRule);
        }
        return count;
    }

    private static void writeWeightsFile(Path out, Path corpusRoot, long[] fileAndNodeCounts,
                                         Map<String, Long> counts) throws IOException {
        Files.createDirectories(out.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            w.write("# H18.2 — rule-level weights for AtnStringGenerator");
            w.newLine();
            w.write("#");
            w.newLine();
            w.write("# Each entry maps a RosettaParser rule name to a relative weight.");
            w.newLine();
            w.write("# The generator uses these to bias RuleTransition selection at");
            w.newLine();
            w.write("# decision points. Weights are RELATIVE, not probabilities.");
            w.newLine();
            w.write("#");
            w.newLine();
            w.write("# Regenerate via CorpusRuleCounter — see that test class for how.");
            w.newLine();
            w.write("#");
            w.newLine();
            w.write("# === Generated from ===");
            w.newLine();
            // Record the corpus as a portable identifier (last two path
            // segments, e.g. "cdm/cdm-6.16.0") — the absolute filesystem
            // path is machine-specific and would leak developer
            // environment details into a committed snapshot.
            w.write("# corpus:   " + corpusIdentifier(corpusRoot));
            w.newLine();
            w.write("# files:    " + fileAndNodeCounts[0]);
            w.newLine();
            w.write("# nodes:    " + fileAndNodeCounts[1]);
            w.newLine();
            w.write("# date:     " + LocalDate.now());
            w.newLine();
            w.newLine();

            // Sort descending by count so the file doubles as a human-
            // readable frequency table.
            counts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                            .thenComparing(Map.Entry.comparingByKey()))
                    .forEach(e -> {
                        try {
                            w.write(e.getKey() + "=" + e.getValue());
                            w.newLine();
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
        }
    }

    private static String[] parserRuleNames() {
        RosettaLexer lexer = new RosettaLexer(CharStreams.fromString(""));
        return new RosettaParser(new CommonTokenStream(lexer)).getRuleNames();
    }

    /**
     * Render {@code corpusRoot} as a portable identifier for the header.
     * Uses the last two path segments when available (e.g.
     * {@code cdm/cdm-6.16.0}) so the committed snapshot carries no
     * machine-specific filesystem path. Falls back to the leaf segment
     * if the path has fewer than two segments.
     */
    private static String corpusIdentifier(Path corpusRoot) {
        int count = corpusRoot.getNameCount();
        if (count >= 2) {
            return corpusRoot.getName(count - 2) + "/" + corpusRoot.getName(count - 1);
        }
        return count == 1 ? corpusRoot.getName(0).toString() : corpusRoot.toString();
    }
}
