package com.regnosys.rosetta.parser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The committed PARSE-level expectations for the chaos cell (v3.2 PR-2, charter § 4b):
 * which chaos files the fork parser is EXPECTED to refuse, and with what diagnostic.
 *
 * <p><b>The data lives in ONE place</b> (Rule 3):
 * {@code scripts/chaos-expander/expectations/fork-diagnostics.tsv} — the § 4b per-file
 * fork-side rows pinned at PR-1 (the spec review's MF-4: data per file, not an
 * aggregate count). This reader exposes the {@code FORK-REFUSES} rows to every suite
 * gate that walks chaos sources; rows with any other verdict (e.g. the
 * {@code PENDING-PR2-L2} resolution-level plan row) are parse-irrelevant and skipped.
 *
 * <p><b>Why the gates need it:</b> the chaos cell is DESIGNED to carry 22 BOM-refused
 * files (refusal parity with upstream, measured at PR-1 and asserted as a SET by
 * {@code ChaosCoverageCensusTest.expectedBomSet()}). A gate that hard-asserts
 * zero parse errors over corpus files ({@code CorpusParseTest}, the structural
 * baseline pair, {@code AstCorpusRegressionTest}) must treat an expected-refusal
 * file as: <b>assert it still refuses</b> (LAW 81 — a control pinned to a refusal
 * set fires when the refusals heal) and contribute nothing else. Every OTHER chaos
 * file keeps the full zero-error contract — deliberate breakage is enumerated,
 * never open-ended.
 *
 * <p>{@code ChaosForkDiagnosticsGateTest} is the standing suite gate that asserts
 * this file's refusal set, the programmatic pin, and the LIVE parse verdicts are
 * all one set (both directions).
 */
public final class ChaosParseExpectations {

    /** {@code <repo>/scripts/chaos-expander/expectations/fork-diagnostics.tsv} (committed). */
    public static final Path FORK_DIAGNOSTICS_TSV =
            com.regnosys.rosetta.testutil.CorpusWalker.repoRoot()
                    .resolve("scripts").resolve("chaos-expander")
                    .resolve("expectations").resolve("fork-diagnostics.tsv");

    private static final String FORK_REFUSES = "FORK-REFUSES";

    /**
     * The validator-grain band verdicts (chaos-1.1.0, v3.2 seat 10 — D49): refusal parity with
     * the released plugin's EXPECTED row, or a DECLARED divergence (the fork accepts a shape the
     * released plugin refuses — a priced parser / resolver / validator seat, shrink-only).
     */
    public static final String FORK_REFUSES_VALIDATION = "FORK-REFUSES-VALIDATION";
    public static final String FORK_ACCEPTS_DIVERGENT = "FORK-ACCEPTS-DIVERGENT";
    /** Both validators admit the file (an ORACLE-CLEAN or MOJO-CRASH band row): acceptance parity. */
    public static final String FORK_ACCEPTS_PARITY = "FORK-ACCEPTS-PARITY";

    /** Lazily-read, immutable {@code filename -> required diagnostic substring} map. */
    private static volatile Map<String, String> refusals;

    private ChaosParseExpectations() {}

    /**
     * The expected-refusal rows: chaos source FILENAME (the tsv's own key — chaos
     * filenames are globally unique, {@code chaos-sNN-<variant>.rosetta}) to the
     * substring the first parse diagnostic must contain. Read once per JVM; the tsv
     * is committed, so absence is a broken checkout, not a skippable state.
     */
    public static Map<String, String> expectedRefusals() {
        Map<String, String> r = refusals;
        if (r == null) {
            r = read();
            refusals = r;
        }
        return r;
    }

    /** True if {@code file} lies under the chaos corpus directory. */
    public static boolean isChaosPath(Path file) {
        // Literal path-segment match on filesystem layout (the no-regex law's
        // trivial-literal exception), mirroring CorpusWalker.isTransitiveDepPath.
        return file.toString().replace('\\', '/').contains("/test-corpus/chaos/");
    }

    /**
     * True if {@code file} is a chaos source the fork parser is EXPECTED to refuse.
     * Keyed on filename AND the chaos path (a vendored file could in principle share
     * a basename; an expectation must never leak onto a vendored cell).
     */
    public static boolean isExpectedRefusal(Path file) {
        return isChaosPath(file)
                && expectedRefusals().containsKey(file.getFileName().toString());
    }

    /** The required diagnostic substring for an expected-refusal file (null if none). */
    public static String requiredDiagnosticSubstring(Path file) {
        return expectedRefusals().get(file.getFileName().toString());
    }

    /**
     * True for a base-only band file ({@code chaos-s9N-base.rosetta}, N a digit): the s9x
     * families no axis expands (axes.tsv), one shape per file, judged alone on both sides.
     */
    public static boolean isBandFile(String fileName) {
        // A literal shape test on a FILE NAME (the no-regex law's trivial-literal exception).
        return fileName.length() == "chaos-s9N-base.rosetta".length()
                && fileName.startsWith("chaos-s9") && fileName.endsWith("-base.rosetta")
                && Character.isDigit(fileName.charAt(8));
    }

    /**
     * The band rows of the tsv: band filename to {@code {verdict, detail}} for the three
     * validator-grain verdicts (every other verdict is skipped here, as {@link #read()} skips
     * these three — the parse grain and the validator grain read ONE file through two filters).
     */
    public static Map<String, String[]> bandRows() {
        Map<String, String[]> out = new LinkedHashMap<>();
        try {
            int lineNo = 0;
            for (String raw : Files.readAllLines(FORK_DIAGNOSTICS_TSV, StandardCharsets.UTF_8)) {
                lineNo++;
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] f = line.split("\t");
                if (f.length != 3) {
                    throw new IllegalStateException(FORK_DIAGNOSTICS_TSV + ":" + lineNo
                            + " expected 3 tab-separated columns, found " + f.length + ": " + line);
                }
                if (!FORK_REFUSES_VALIDATION.equals(f[1]) && !FORK_ACCEPTS_DIVERGENT.equals(f[1])
                        && !FORK_ACCEPTS_PARITY.equals(f[1])) continue;
                if (!isBandFile(f[0])) {
                    throw new IllegalStateException(FORK_DIAGNOSTICS_TSV + ":" + lineNo
                            + " a validator-grain verdict on a non-band file: " + line);
                }
                if (out.put(f[0], new String[] {f[1], f[2]}) != null) {
                    throw new IllegalStateException(FORK_DIAGNOSTICS_TSV + ":" + lineNo
                            + " duplicate band row: " + f[0]);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + FORK_DIAGNOSTICS_TSV, e);
        }
        return Collections.unmodifiableMap(out);
    }

    /**
     * A {@code FORK-REFUSES-VALIDATION} row's detail column: EVERY error message of the file as a
     * {@code ||}-separated list of substrings, one per ERROR, any order (round 1, cq MF-1). Blank items
     * refuse — a declared error is never empty.
     */
    public static java.util.List<String> declaredErrorSubstrings(String detail) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String part : detail.split("\\|\\|")) {
            String sub = part.trim();
            if (sub.isEmpty()) {
                throw new IllegalStateException("an empty declared error substring in: " + detail);
            }
            out.add(sub);
        }
        return Collections.unmodifiableList(out);
    }

    private static Map<String, String> read() {
        if (!Files.isRegularFile(FORK_DIAGNOSTICS_TSV)) {
            throw new IllegalStateException("committed chaos expectations missing: "
                    + FORK_DIAGNOSTICS_TSV + " — broken checkout (the tsv is the § 4b"
                    + " SOT for the fork-side parse contract; it is committed, never generated)");
        }
        Map<String, String> out = new LinkedHashMap<>();
        try {
            int lineNo = 0;
            for (String raw : Files.readAllLines(FORK_DIAGNOSTICS_TSV, StandardCharsets.UTF_8)) {
                lineNo++;
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                // Tab-delimited config row, not language content (the no-regex law's
                // trivial-literal exception).
                String[] f = line.split("\t");
                if (f.length != 3) {
                    throw new IllegalStateException(FORK_DIAGNOSTICS_TSV + ":" + lineNo
                            + " expected 3 tab-separated columns, found " + f.length + ": " + line);
                }
                if (!FORK_REFUSES.equals(f[1])) continue; // e.g. PENDING-PR2-L2: not a parse expectation
                if (out.put(f[0], f[2]) != null) {
                    throw new IllegalStateException(FORK_DIAGNOSTICS_TSV + ":" + lineNo
                            + " duplicate expected-refusal row: " + f[0]);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + FORK_DIAGNOSTICS_TSV, e);
        }
        if (out.isEmpty()) {
            throw new IllegalStateException(FORK_DIAGNOSTICS_TSV
                    + " carries no FORK-REFUSES rows — the § 4b refusal-parity contract"
                    + " cannot be empty while the a5bom family exists");
        }
        return Collections.unmodifiableMap(out);
    }
}
