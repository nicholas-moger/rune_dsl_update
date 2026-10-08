// ============================================================================
// SEAT 30 tidy 2 - DRAFT.  Proposed path:
//   rune-parser/src/test/java/com/regnosys/rosetta/hygiene/SourceBraceHygieneTest.java
// See doubledbrace-notes.md for the home decision, the pattern derivation and the
// false-positive measurement.  This file is a DRAFT under target/ - it is not
// compiled from here.
// ============================================================================
package com.regnosys.rosetta.hygiene;

import com.regnosys.rosetta.testutil.CorpusWalker;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks production Java source against the <b>doubled-brace template artifact</b> — the
 * literal {@code {{} / {@code }}} residue a templating pass leaves behind when its
 * brace-escaping is applied to text that was never a template.
 *
 * <p><b>Why this exists.</b> PR #601 (v3.1 flip seat 29) shipped six such lines into
 * {@code src/main} inside two law commits, and the adversarial review — not any gate —
 * caught them (commit {@code e09b9e0f}, "six doubled-brace template artifacts removed").
 * They compiled, and they were semantics-identical: a doubled {@code {} merely opens a
 * redundant inner block and the doubled {@code }} closes it, so {@code javac} is silent,
 * the corpus bytes are unmoved, and only a human reading the diff can see them. The
 * review's own suggestion — "add the hygiene grep" — was banked to seat 30 because the
 * existing {@code scripts/ci/ci-hygiene-lints.sh} lints workflow YAML, not source.
 *
 * <p><b>The rule, derived from the six actual artifacts</b> (all six are reproduced in
 * {@link #theRuleFiresOnTheSixArtifactsItWasBuiltFrom()}). A line is a violation when
 * either:
 * <ol>
 *   <li><b>OPEN</b> — the line, with trailing whitespace removed, <b>ends with</b>
 *       {@code {{}; or</li>
 *   <li><b>CLOSE</b> — the line, trimmed, <b>is exactly</b> {@code }}}.</li>
 * </ol>
 *
 * <p><b>Why the line SHAPE and not the substring.</b> A bare search for {@code }}} is
 * useless here: 110 lines of the scanned tree contain it legitimately, essentially all of
 * them javadoc of the form <code>{&#64;code {"ev":"done","metrics":{...}}}</code>. The
 * shape constraints remove that surface completely without an allow-list:
 * <ul>
 *   <li>a javadoc continuation line starts {@code *}, so it can never <i>trim to</i>
 *       {@code }}}; a {@code //} comment line can never trim to it either;</li>
 *   <li>a Java string literal cannot span a line, so a line that <i>ends with</i>
 *       {@code {{} cannot have that {@code {{} inside a string — the literal would be
 *       unterminated and would not compile;</li>
 *   <li>the same argument covers a line that trims to exactly {@code }}}.</li>
 * </ul>
 * No comment mask, no string mask, and — deliberately — <b>no regex</b> is needed.
 *
 * <p><b>Implementation note (the project's no-regex-on-structured-content rule).</b> The
 * scan is {@link String#stripTrailing()} / {@link String#strip()} plus
 * {@link String#endsWith(String)} / {@link String#equals(Object)} — literal, per line,
 * never {@code Pattern.compile}. This follows
 * {@code com.regnosys.rosetta.ir.contract.IRPackageStructureTest}, which records the same
 * carve-out for the same reason: a fixed brace pair is literal text, comparable to the
 * "fixed copyright headers" exception, not a structural parse.
 *
 * <p><b>The one legitimate shape this would reject</b> is Java's double-brace
 * initialisation idiom ({@code new HashMap<>() {{ put(…); }}}). The scanned tree uses it
 * <b>nowhere</b> — measured 0 occurrences of either rule over all 522 files, and 0 over
 * all 741 {@code src/main/java} files in every module including {@code demo/}. If it is
 * ever wanted, add the file to {@link #EXEMPT_FILES} with a written reason (the ArchUnit
 * Freezing pattern this project already uses for {@code KNOWN_EXCEPTIONS}) rather than
 * weakening the rule. Note the idiom is independently discouraged: it leaks an anonymous
 * subclass and a reference to the enclosing instance.
 *
 * <p><b>Scope.</b> {@link #SCANNED_MODULES} is the ledger's own
 * {@code LABEL_MODULES} set ({@code scripts/ci/evidence-api-ledger.py} :93-95) — the four
 * generator/IR modules plus {@code rune-parser}. Keeping the scope a re-used, already
 * justified constant rather than a new invention is SDLC Rule 3. {@code src/test} is out
 * of scope on purpose: {@code ModelNodeTest} deliberately builds the string
 * {@code "not valid rune at all {{{{"} as a bad-input fixture.
 *
 * <p><b>Path resolution</b> via {@link CorpusWalker#repoRoot()} — classfile-derived and
 * JVM-cwd independent, the same idiom the repo-wide doclocks use. {@code Path.of("../…")}
 * would break under {@code mvn -f rune-parser/pom.xml test} run from the repo root.
 */
class SourceBraceHygieneTest {

    /** The ledger's LABEL_MODULES (evidence-api-ledger.py :93-95), re-used verbatim. */
    private static final List<String> SCANNED_MODULES = List.of(
            "rune-java-generator", "rune-ir", "rune-ir-java", "rune-ir-java-optimised",
            "rune-parser");

    private static final String SRC = "src/main/java";

    /**
     * Repo-relative paths permitted to carry a doubled brace, each with the reason.
     * EMPTY, and expected to stay empty — the double-brace-initialisation idiom is
     * absent from this tree and independently discouraged. Adding an entry is a
     * deliberate act that shows up in review.
     */
    private static final List<String> EXEMPT_FILES = List.of();

    /**
     * -> the violation kind, or {@code null}. The whole rule lives here so the positive
     * control below exercises exactly the predicate the tree scan uses.
     */
    static String violationKind(String line) {
        if (line.stripTrailing().endsWith("{{")) {
            return "doubled OPEN brace at end of line";
        }
        if (line.strip().equals("}}")) {
            return "doubled CLOSE brace alone on a line";
        }
        return null;
    }

    @Test
    void productionSourceCarriesNoDoubledBraceTemplateArtifacts() throws IOException {
        List<String> violations = new ArrayList<>();
        int scanned = 0;
        for (String module : SCANNED_MODULES) {
            Path root = CorpusWalker.repoRoot().resolve(module).resolve(SRC);
            // A module root that has gone missing must FAIL, not silently scan nothing —
            // a gate that can quietly measure zero is not a gate.
            assertTrue(Files.isDirectory(root),
                    "scanned module root missing (has a module been renamed? update "
                            + "SCANNED_MODULES in the same commit): " + root);
            int inModule = 0;
            try (Stream<Path> files = Files.walk(root)) {
                for (Path java : (Iterable<Path>) files
                        .filter(p -> p.toString().endsWith(".java"))::iterator) {
                    String rel = CorpusWalker.repoRoot().relativize(java).toString()
                            .replace('\\', '/');
                    inModule++;
                    if (EXEMPT_FILES.contains(rel)) {
                        continue;
                    }
                    int lineNo = 0;
                    for (String line : Files.readAllLines(java)) {
                        lineNo++;
                        String kind = violationKind(line);
                        if (kind != null) {
                            violations.add(rel + ":" + lineNo + "  " + kind + "  |"
                                    + line + "|");
                        }
                    }
                }
            }
            assertTrue(inModule > 0, "scanned zero .java files under " + root);
            scanned += inModule;
        }
        assertTrue(scanned > 0, "scanned zero files in total");
        assertEquals(List.of(), violations,
                "Doubled-brace template artifacts in production source. These COMPILE and "
                        + "are semantics-identical (a redundant inner block), so javac and "
                        + "the byte-compare cannot see them — which is why PR #601 shipped "
                        + "six of them and only a human review caught it (commit e09b9e0f). "
                        + "Remove the doubling; do not weaken this rule. If a line is a "
                        + "genuine double-brace initialisation, add the file to "
                        + "EXEMPT_FILES with a written reason.");
    }

    /**
     * The positive control (prove the instrument can fail). These are the six lines
     * removed by commit {@code e09b9e0f}, quoted verbatim from
     * {@code git show e09b9e0f} — four from {@code CollectionHandler.filterMethod}'s
     * 3-arg overload, two from the {@code FunctionExpressionRenderer} law-4 arm. The
     * same predicate, run over the same tree at {@code e09b9e0f^}, returns EXACTLY these
     * six and nothing else (522 files, 5 modules), and returns ZERO at this head.
     */
    @Test
    void theRuleFiresOnTheSixArtifactsItWasBuiltFrom() {
        List<String> knownBad = List.of(
                "            JavaStatementScope scope) {{",
                "        if (boundT != null && tu != null && tu.isMapperListOfLists(boundT)) {{",
                "        }}",
                "    }}",
                "                        || NavigationHandler.ruleOutputProvesMulti(expression, compiler))) {{",
                "        }}");
        List<String> missed = new ArrayList<>();
        for (String line : knownBad) {
            if (violationKind(line) == null) {
                missed.add(line);
            }
        }
        assertEquals(List.of(), missed,
                "the rule no longer fires on the artifacts it was derived from");

        // ...and the negative half: the legitimate shapes the scanned tree actually
        // contains must NOT fire. The first three are real javadoc lines from the tree
        // (demo/harness/.../DemoOut.java:50, :35 and a nested-close pair); the last is a
        // nested block close, which Java writes one brace per line.
        List<String> legitimate = List.of(
                "    /** {@code {\"ev\":\"done\",\"metrics\":{...}}} */",
                "    /** {@code {\"ev\":\"start\",\"id\":...,\"detail\":...}} */",
                " * \"fields\": {...scalar accessors...}, \"children\": [...]}}. Depth is capped at",
                "        }",
                "            }");
        List<String> falsePositives = new ArrayList<>();
        for (String line : legitimate) {
            if (violationKind(line) != null) {
                falsePositives.add(line);
            }
        }
        assertEquals(List.of(), falsePositives,
                "the rule fires on a legitimate line — the shape constraints have been "
                        + "weakened into a bare substring search");
    }
}
