package com.regnosys.rosetta.harness.matrix;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * NDJSON sink for matrix-test results. One record per line, UTF-8, every line
 * terminated with {@code \n} (including the last) — the standard
 * newline-delimited-JSON convention so CI can concatenate shard outputs with
 * plain file append.
 *
 * <p>Part of P1.2 test-harness infrastructure (audit hook H14). Intended to be
 * a single dependency for the matrix runner — no JSON library needed, no
 * reflection, stable on-disk format across Windows dev machines and Linux CI.
 * Control characters are escaped using the JSON {@code backslash-u-HHHH} form.
 *
 * <h2>Record shape</h2>
 * {@snippet :
 * {"corpus":"CDM","project":"base","version":"6.16.0","kind":"TYPE",
 *  "source":"test-corpus/cdm/cdm-6.16.0/rosetta-source/a.rosetta",
 *  "outcome":"PASSED","duration_ms":42,"message":null}
 * }
 *
 * <p>The {@code source} path is serialised with forward slashes regardless of
 * host filesystem — the NDJSON artifact is a cross-platform comparable record,
 * not a platform-local path listing.
 *
 * <p>Usage:
 * {@snippet :
 * try (Reporter r = Reporter.open(path)) {
 *     r.record(Reporter.Result.passed(coord, durationMs));
 * }
 * }
 */
public final class Reporter implements AutoCloseable {

    /** Test-result outcome. */
    public enum Outcome { PASSED, FAILED, SKIPPED }

    /**
     * One test-result record. {@code message} is {@code null} for {@link Outcome#PASSED};
     * non-null for {@link Outcome#FAILED} and {@link Outcome#SKIPPED}.
     */
    public record Result(MatrixCoordinate coordinate, Outcome outcome, long durationMs, String message) {
        public Result {
            Objects.requireNonNull(coordinate, "coordinate");
            Objects.requireNonNull(outcome, "outcome");
            if (durationMs < 0) throw new IllegalArgumentException("durationMs >= 0 required, got " + durationMs);
            if (outcome == Outcome.PASSED && message != null) {
                throw new IllegalArgumentException("PASSED must have null message");
            }
            if (outcome != Outcome.PASSED) Objects.requireNonNull(message, "message for " + outcome);
        }

        public static Result passed(MatrixCoordinate coord, long ms) {
            return new Result(coord, Outcome.PASSED, ms, null);
        }

        public static Result failed(MatrixCoordinate coord, long ms, String msg) {
            return new Result(coord, Outcome.FAILED, ms, msg);
        }

        public static Result skipped(MatrixCoordinate coord, long ms, String reason) {
            return new Result(coord, Outcome.SKIPPED, ms, reason);
        }
    }

    private final Writer out;

    private Reporter(Writer out) {
        this.out = out;
    }

    /**
     * Open an NDJSON reporter writing to {@code path}. Truncates any existing
     * file. A filename-only path (no parent) is accepted — the working
     * directory is used and no directory is created.
     */
    public static Reporter open(Path path) {
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            return new Reporter(new BufferedWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Append one result as a single NDJSON line. */
    public void record(Result r) {
        try {
            out.write(toJson(r));
            out.write('\n');
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        try { out.close(); } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    /** Visible for test — same format as {@link #record(Result)}. */
    static String toJson(Result r) {
        MatrixCoordinate c = r.coordinate();
        StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        appendString(sb, "corpus", c.corpus().name()); sb.append(',');
        appendString(sb, "project", c.project());     sb.append(',');
        appendString(sb, "version", c.version().display()); sb.append(',');
        appendString(sb, "kind", c.kind().name());    sb.append(',');
        appendString(sb, "source", toForwardSlash(c.source())); sb.append(',');
        appendString(sb, "outcome", r.outcome().name()); sb.append(',');
        sb.append('"').append("duration_ms").append('"').append(':').append(r.durationMs()); sb.append(',');
        sb.append('"').append("message").append('"').append(':');
        if (r.message() == null) sb.append("null");
        else { sb.append('"'); escape(sb, r.message()); sb.append('"'); }
        sb.append('}');
        return sb.toString();
    }

    private static void appendString(StringBuilder sb, String key, String value) {
        sb.append('"').append(key).append('"').append(':').append('"');
        escape(sb, value);
        sb.append('"');
    }

    private static String toForwardSlash(Path p) {
        return p.toString().replace('\\', '/');
    }

    /**
     * JSON string escaping per RFC 8259 §7. Handles the mandatory escapes plus
     * control characters via {@code backslash-u-HHHH}. No library needed — the surface is
     * small and well-specified.
     */
    private static void escape(StringBuilder sb, String s) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (ch < 0x20) sb.append(String.format("\\" + "u%04x", (int) ch));
                    else sb.append(ch);
                }
            }
        }
    }
}
