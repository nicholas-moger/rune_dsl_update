package com.regnosys.rosetta.parser;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * v3.2 PR-1 — THE COVERAGE CENSUS (the chaos-gate charter § 2.4, on the #608
 * parse-tree-census precedent): every ANTLR parser-rule node kind counted over the EXPANDED chaos
 * corpus (the expander's sink) and over the whole vendored test corpus, then diffed. The claim
 * "the chaos corpus covers the whole grammar" is a MEASURED figure here, not a reading of 22 seed
 * family labels against 18 root-element names:
 *
 * <ul>
 *   <li><b>The zero-set</b> (corpus &gt; 0, chaos = 0) is PINNED below — every member enumerated
 *       and dispositioned at PR-1 review (add a seed row, or record why not). A kind silently
 *       joining the set is a coverage REGRESSION (the anti-shrink ratchet: the charter's "the seed
 *       is rewritten" hatch becomes a visible count drop); a kind silently leaving it is a verdict
 *       change to re-ratify.</li>
 *   <li><b>The chaos-only set</b> (chaos &gt; 0, corpus = 0) is pinned too — the adversarial-win
 *       list: grammar surfaces the 25 real projects never write, which is exactly the § 7a gap the
 *       gate exists to close.</li>
 *   <li>The pair-level census (parent-kind → child-kind seat contexts) is PRINTED for the review;
 *       the disposition grain is the kind level.</li>
 * </ul>
 *
 * <p>The BOM files the parser refuses (22 at chaos-1.0.0, 34 at chaos-1.1.0 — one per
 * A5-applicable family) still contribute their error-RECOVERY parse trees to the chaos kind
 * census (nil effect today — their slim-carrier content is duplicated by the sibling A5
 * variants — but stated rather than discovered; the cq review's NIT-13).
 *
 * <p><b>chaos-1.1.0 (v3.2 seat 10, D49):</b> the 22 gen-1 families are kept whole, twelve fresh
 * shape families and the base-only s9x band join — 988 files. The zero-set stays
 * EMPTY by construction (gen-2 ⊇ gen-1 at this grain) and the chaos-only set is UNMOVED: the
 * fresh families add no parser-rule KIND the vendored corpus never writes (167 chaos kinds =
 * 163 corpus kinds + the four below), so their adversarial power is in shape conjunctions the
 * would-have-caught table and D49 name, not in grammar surface — the pair-level seat contexts
 * (informational) moved 412 → 443 and the pair-level zero contexts 257 → 232 between the
 * 1.0.0 print (the #630 chain's parser log) and this seat's (scratch/census-s10-run1.log).
 *
 * <p><b>The instrument's stated blind spot</b> (the PR-1 spec review's MF-2): the census counts
 * PARSER-RULE contexts, so grammar alternatives that share one context are indistinguishable —
 * {@code regulatoryReference} vs {@code docReference} (one {@code DocReferenceContext}),
 * {@code rationale_author}, {@code using standard} (an optional token inside
 * {@code rosettaReport}), and metadata annotation kinds ({@code template} vs {@code key} — plain
 * identifiers). "Zero-set EMPTY" therefore guarantees node-KIND coverage, not token-alternative
 * coverage; the token-level gaps that review measured (311/21/11/3 corpus files) were closed by
 * seed rows (S14/S07/S09) and any future ones are the would-have-caught table's business, not
 * this census's.
 *
 * <p>Both walks are corpus-gated with the {@code -Dcorpus.required=true} contract (the #606
 * {@code Drr7Corpus} shape): the chaos sink is regenerable on demand
 * ({@code python scripts/chaos-expander/expand.py} — deterministic, digest-receipted), so its
 * absence is a skip on a fresh clone and a FAILURE under the receipts chain's setting.
 */
class ChaosCoverageCensusTest {

    private static final Path CORPUS_DIR = Path.of("../test-corpus");
    private static final Path CHAOS_DIR = Path.of("../target/chaos-expander/work");

    static final String REQUIRED_PROPERTY = "corpus.required";

    // THE PINS — measured at the post-review expansion against the manifest corpus population;
    // re-taken live at every run. CHAOS_SINK_DIGEST is expand.py's own digest algorithm
    // (sha256 over sorted name\0bytes\0 pairs, first 16 hex) recomputed here over the sink, so
    // a STALE or hand-edited sink fails THIS test instead of silently measuring old bytes (the
    // cq review's SF-5); on a mismatch, re-run `python scripts/chaos-expander/expand.py` and,
    // if the seeds changed deliberately, re-ratify both pins from its printed digest.
    static final int CORPUS_FILES = 4_137;
    // chaos-1.1.0 (v3.2 seat 10, D49): 44 seeds (22 gen-1 + 12 fresh shape families + the
    // base-only s9x band: seven refusal-parity files, s95 the upstream-CLEAN duplicate-parameter
    // shape, s98 the mojo-crash shape, s99 the java.lang shadow) -> 988 files under the
    // charter's 1,000 cap; the digest from expand.py's own print at the re-admitted lab head
    // (l1-s10-run7, deterministic x2).
    static final int CHAOS_FILES = 988;
    static final String CHAOS_SINK_DIGEST = "313bfac85dbf843e";
    // The VENDORED corpus parses clean — zero syntax-error files. (The CHAOS refusal set —
    // exactly the 22 a5bom variants, the § 4b BOM-REFUSAL refusal-parity fact whose per-file
    // data lives in expectations/fork-diagnostics.tsv — is asserted as a SET by
    // expectedBomSet() below, not by this corpus-side constant.)
    static final int CORPUS_FILES_WITH_SYNTAX_ERRORS = 0;

    /**
     * The dispositioned ZERO-SET: parser-rule kinds the vendored corpus exercises that the chaos
     * corpus does not. <b>EMPTY at PR-1</b> — the first measurement (l1-run3) found 41 zeros
     * (synonym-mapping bodies, the leftless/then expression forms, conversions, cross-function
     * calls, labels, with-meta, annotation for-paths); every one was dispositioned ADD-A-SEED-ROW
     * and the seeds enriched (S04/S05/S07/S09/S11/S12/S14), so the chaos cell now exercises every
     * node kind the whole vendored corpus does. Any member appearing here again is a coverage
     * REGRESSION to fix, never to re-pin without review.
     */
    static final Set<String> DISPOSITIONED_ZERO_KINDS = Set.of();

    /**
     * The pinned CHAOS-ONLY set: kinds the chaos corpus exercises at &gt; 0 that the whole
     * vendored corpus holds at ZERO — the measured adversarial win (the § 7a "placement shapes
     * real projects never write", now with names): the {@code single}/{@code multiple} exists
     * modifiers, the synonym-mapping {@code is absent} test, and the then-form
     * {@code reduce}/{@code reverse} bodies.
     */
    static final Set<String> CHAOS_ONLY_KINDS = Set.of(
            "ExistsModifierContext",
            "MapTestAbsentExprContext",
            "ReduceWithoutLeftExprContext",
            "ReverseWithoutLeftExprContext");

    static final class Census {
        int files;
        int filesWithSyntaxErrors;
        final TreeMap<String, Integer> kinds = new TreeMap<>();
        final TreeMap<String, Integer> pairs = new TreeMap<>();
        final List<String> syntaxErrors = new ArrayList<>();
        final TreeSet<String> syntaxErrorFiles = new TreeSet<>();
    }

    /** The diff at the disposition grain — extracted so the control can exercise it directly. */
    static TreeSet<String> presentLeftOnly(TreeMap<String, Integer> left, TreeMap<String, Integer> right) {
        TreeSet<String> out = new TreeSet<>();
        for (var e : left.entrySet()) {
            if (right.getOrDefault(e.getKey(), 0) == 0) {
                out.add(e.getKey());
            }
        }
        return out;
    }

    static Census take(List<Path> files) {
        Census c = new Census();
        var listener = new RosettaParserBaseListener() {
            @Override
            public void enterEveryRule(ParserRuleContext ctx) {
                String kind = ctx.getClass().getSimpleName();
                c.kinds.merge(kind, 1, Integer::sum);
                if (ctx.getParent() != null) {
                    c.pairs.merge(ctx.getParent().getClass().getSimpleName() + " > " + kind,
                            1, Integer::sum);
                }
            }
        };
        for (Path file : files) {
            c.files++;
            RosettaParseResult result = RosettaParserFacade.parseFile(file);
            if (!result.errors().isEmpty()) {
                c.filesWithSyntaxErrors++;
                c.syntaxErrorFiles.add(file.getFileName().toString());
                if (c.syntaxErrors.size() < 5) {
                    c.syntaxErrors.add(file.getFileName() + ": " + result.errors().get(0));
                }
            }
            ParseTreeWalker.DEFAULT.walk(listener, result.tree());
        }
        return c;
    }

    /**
     * The a5bom SET (LAW 73: pin the set, not the count) — the § 4b BOM refusal-parity fork half.
     * chaos-1.1.0 (D49): one per A5-applicable family, s01–s34 (the s9x refusal-parity band
     * expands under no axis).
     */
    static TreeSet<String> expectedBomSet() {
        TreeSet<String> s = new TreeSet<>();
        for (int i = 1; i <= 34; i++) {
            s.add(String.format("chaos-s%02d-a5bom.rosetta", i));
        }
        return s;
    }

    static List<Path> rosettaFilesUnder(Path root) throws IOException {
        List<Path> files = new ArrayList<>();
        try (var walk = Files.walk(root)) {
            walk.filter(p -> p.toString().endsWith(".rosetta"))
                    // Since PR-2 the chaos cell lives INSIDE test-corpus/ — the corpus
                    // side of this diff must stay the VENDORED corpora, or the census
                    // compares chaos against itself and the chaos-only set (the § 7a
                    // adversarial-win figure) collapses to empty by construction.
                    .filter(p -> !p.toString().replace('\\', '/').contains("/test-corpus/chaos/"))
                    .sorted().forEach(files::add);
        }
        return files;
    }

    /** expand.py's digest, replicated: sha256 over sorted (basename, bytes) pairs NUL-joined,
     *  first 16 hex — the sink-freshness pin (SF-5). */
    static String sinkDigest(List<Path> files) throws IOException {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            for (Path p : files) {
                md.update(p.getFileName().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                md.update((byte) 0);
                md.update(Files.readAllBytes(p));
                md.update((byte) 0);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.substring(0, 16);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void gate(Path dir, String hint) {
        String reason = null;
        if (!Files.isDirectory(dir)) {
            reason = "[ABSENT] " + dir + " is not a directory (" + hint + ")";
        } else {
            try (var walk = Files.walk(dir, 10)) {
                if (walk.noneMatch(p -> p.toString().endsWith(".rosetta"))) {
                    reason = "[ABSENT] " + dir + " holds no .rosetta file (" + hint + ")";
                }
            } catch (IOException e) {
                // An IO failure is NOT the same fact as an absent corpus (the cq review's
                // NIT-12) — name it as itself.
                reason = "[UNREADABLE] " + dir + " could not be walked: " + e;
            }
        }
        if (reason != null) {
            if (Boolean.getBoolean(REQUIRED_PROPERTY)) {
                fail(reason + " and -D" + REQUIRED_PROPERTY + "=true forbids skipping the census");
            }
            assumeTrue(false, reason);
        }
    }

    @Test
    void the_chaos_corpus_coverage_diffed_against_the_vendored_corpus_at_node_kind_grain() throws IOException {
        gate(CORPUS_DIR, "acquire it as docs/CORPUS-9.83.md describes");
        gate(CHAOS_DIR, "regenerate with: python scripts/chaos-expander/expand.py");

        long t0 = System.nanoTime();
        List<Path> chaosFiles = rosettaFilesUnder(CHAOS_DIR);
        String liveDigest = sinkDigest(chaosFiles);
        Census corpus = take(rosettaFilesUnder(CORPUS_DIR));
        Census chaos = take(chaosFiles);

        TreeSet<String> zero = presentLeftOnly(corpus.kinds, chaos.kinds);
        TreeSet<String> chaosOnly = presentLeftOnly(chaos.kinds, corpus.kinds);
        TreeSet<String> pairZero = new TreeSet<>();
        for (var e : corpus.pairs.entrySet()) {
            if (chaos.pairs.getOrDefault(e.getKey(), 0) == 0) {
                pairZero.add(e.getKey());
            }
        }

        StringBuilder report = new StringBuilder();
        report.append("corpus files ").append(corpus.files)
                .append(" (errors ").append(corpus.filesWithSyntaxErrors).append(corpus.syntaxErrors)
                .append("), kinds ").append(corpus.kinds.size())
                .append(", pairs ").append(corpus.pairs.size()).append('\n');
        report.append("chaos files ").append(chaos.files)
                .append(" (errors ").append(chaos.filesWithSyntaxErrors).append(chaos.syntaxErrors)
                .append("), kinds ").append(chaos.kinds.size())
                .append(", pairs ").append(chaos.pairs.size()).append('\n');
        report.append("ZERO-SET (corpus>0, chaos=0): ").append(zero.size()).append(' ')
                .append(zero).append('\n');
        report.append("CHAOS-ONLY (chaos>0, corpus=0): ").append(chaosOnly.size()).append(' ')
                .append(chaosOnly).append('\n');
        report.append("pair-level zero contexts (informational, review grain): ")
                .append(pairZero.size()).append('\n');
        System.out.println("[" + getClass().getSimpleName() + "] ("
                + ((System.nanoTime() - t0) / 1_000_000_000L) + " s)\n" + report);

        assertAll("the coverage census\n" + report,
                () -> assertEquals(CORPUS_FILES, corpus.files, "corpus .rosetta population (the manifest's)"),
                () -> assertEquals(CORPUS_FILES_WITH_SYNTAX_ERRORS, corpus.filesWithSyntaxErrors,
                        "corpus files with syntax errors " + corpus.syntaxErrors),
                () -> assertEquals(CHAOS_FILES, chaos.files, "chaos sink population (the expander's cap receipt)"),
                () -> assertEquals(CHAOS_SINK_DIGEST, liveDigest,
                        "the sink digest must match the pinned expand.py digest — a mismatch means the sink is STALE"
                                + " or hand-edited: re-run python scripts/chaos-expander/expand.py (and re-ratify the pin"
                                + " only for a deliberate seed change)"),
                () -> assertEquals(expectedBomSet(), chaos.syntaxErrorFiles,
                        "the fork parser's refusal SET must be exactly the 22 a5bom files (LAW 73 — the set, not the count)"),
                () -> assertEquals(DISPOSITIONED_ZERO_KINDS, zero,
                        "the dispositioned zero-set (a NEW member is a coverage regression; a healed one is a verdict change to re-ratify)"),
                () -> assertEquals(CHAOS_ONLY_KINDS, chaosOnly,
                        "the chaos-only adversarial-win set"));
    }

    /**
     * The positive control (prove the instrument can fail), in TWO legs. Leg 1: a known model must
     * move exactly the counters its constructs name — including {@code single exists}
     * ({@code ExistsModifierContext}), a kind the pinned census holds CHAOS-ONLY, so the rare-kind
     * counting is exercised on a known input. Leg 2: the set-difference logic itself
     * ({@link #presentLeftOnly}) is fed synthetic censuses and must classify a left-only kind into
     * the zero-set and a right-only kind into the chaos-only set — the DETECTION has its own
     * control, not just the counters. (The first draft of this control claimed {@code rule source}
     * was corpus-zero; the spec review refuted that against the repo — the corpus writes it — and
     * the pin's own arithmetic agreed: {@code RosettaExternalRuleSourceContext} is absent from
     * {@code CHAOS_ONLY_KINDS} precisely because the corpus count is non-zero.)
     */
    @Test
    void the_census_counters_and_the_diff_logic_move_on_known_inputs() throws IOException {
        String model = String.join("\n",
                "namespace census.control",
                "version \"0.0.0\"",
                "type ControlType:",
                "    a string (0..*)",
                "    condition ControlSingle:",
                "        a single exists",
                "rule source ControlRuleSource",
                "{",
                "}");
        Path tmp = Files.createTempFile("chaos-coverage-control", ".rosetta");
        try {
            Files.writeString(tmp, model);
            Census c = take(List.of(tmp));
            TreeMap<String, Integer> left = new TreeMap<>();
            TreeMap<String, Integer> right = new TreeMap<>();
            left.put("Shared", 3);
            left.put("LeftOnly", 1);
            right.put("Shared", 5);
            right.put("RightOnly", 2);
            assertAll("the control census: " + c.kinds,
                    () -> assertEquals(1, c.files),
                    () -> assertEquals(0, c.filesWithSyntaxErrors, "the control model must parse clean: " + c.syntaxErrors),
                    () -> assertEquals(1, (int) c.kinds.getOrDefault("DataTypeContext", 0), "one type declaration"),
                    () -> assertEquals(1, (int) c.kinds.getOrDefault("ExistsModifierContext", 0),
                            "one `single exists` modifier (the pinned CHAOS-ONLY kind, counted on a known input)"),
                    () -> assertEquals(1, (int) c.kinds.getOrDefault("RosettaExternalRuleSourceContext", 0),
                            "one rule source"),
                    () -> assertEquals(1, (int) c.kinds.getOrDefault("AttributeContext", 0), "one attribute"),
                    () -> assertTrue(c.pairs.containsKey("DataTypeContext > AttributeContext"),
                            "the pair census records the attribute's exact seat context: " + c.pairs.keySet()),
                    () -> assertEquals(new TreeSet<>(List.of("LeftOnly")), presentLeftOnly(left, right),
                            "the zero-set detection classifies a left-only kind"),
                    () -> assertEquals(new TreeSet<>(List.of("RightOnly")), presentLeftOnly(right, left),
                            "the chaos-only detection classifies a right-only kind"));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
