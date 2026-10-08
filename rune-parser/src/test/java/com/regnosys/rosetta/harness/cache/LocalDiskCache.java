package com.regnosys.rosetta.harness.cache;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * On-disk content-addressable cache for matrix-cell test results.
 *
 * <p>P1.2 audit hook H24 foundation. A single cache lives under a given
 * root directory; each {@link CacheEntry} is stored as one
 * {@code <fingerprint>.ndjson} file so the directory is inspectable with
 * grep/cat and a failed cache-hit path can be diffed by standard tools.
 *
 * <p>Serialisation uses a fixed-shape, hand-rolled JSON object — matches
 * the shape of the matrix-run NDJSON reporter ({@code corpus}/{@code kind}/
 * {@code outcome}/{@code duration_ms}/{@code message}) so a future tool can
 * load either a cache file or a reporter shard without a schema switch.
 * Only the fields needed to reconstruct a {@link CacheEntry} are written;
 * a full matrix coordinate is not stored here because the cache is keyed
 * by {@link CacheKey#fingerprint()}, not by coordinate.
 *
 * <p>Concurrency: this is a single-writer single-reader cache. H16's
 * per-fork argLine wiring guarantees that two forks write to different
 * shard files, and the fingerprint-per-file layout means concurrent puts
 * for different keys are conflict-free at the filesystem level. Two
 * concurrent puts for the <em>same</em> key are a test authoring bug, not
 * a cache correctness concern.
 */
public final class LocalDiskCache {

    private final Path root;

    private LocalDiskCache(Path root) {
        this.root = root;
    }

    /** Open a cache rooted at {@code root}. Directory is created lazily on first put. */
    public static LocalDiskCache at(Path root) {
        Objects.requireNonNull(root, "root");
        return new LocalDiskCache(root);
    }

    /** Fetch a cached entry, or {@link Optional#empty()} on miss. */
    public Optional<CacheEntry> get(CacheKey key) {
        Objects.requireNonNull(key, "key");
        Path file = root.resolve(key.fingerprint() + ".ndjson");
        if (!Files.isRegularFile(file)) return Optional.empty();
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            if (lines.isEmpty()) return Optional.empty();
            return Optional.of(deserialise(key, lines.get(0)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Store an entry; overwrites any prior entry for the same key. */
    public void put(CacheEntry entry) {
        Objects.requireNonNull(entry, "entry");
        try {
            Files.createDirectories(root);
            Path file = root.resolve(entry.key().fingerprint() + ".ndjson");
            Files.writeString(file, serialise(entry) + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---- serialisation ----

    /**
     * Serialises a cache entry into one JSON object. Field layout is
     * stable; see the class javadoc for the matrix-report shape it
     * matches.
     */
    static String serialise(CacheEntry entry) {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        appendString(sb, "inputSha", entry.key().inputSha()); sb.append(',');
        appendString(sb, "testBytecodeSha", entry.key().testBytecodeSha()); sb.append(',');
        appendString(sb, "toolVersionSha", entry.key().toolVersionSha()); sb.append(',');
        appendString(sb, "outcome", entry.outcome().name()); sb.append(',');
        sb.append('"').append("duration_ms").append('"').append(':').append(entry.durationMs()); sb.append(',');
        sb.append('"').append("message").append('"').append(':');
        if (entry.message() == null) sb.append("null");
        else { sb.append('"'); escape(sb, entry.message()); sb.append('"'); }
        sb.append('}');
        return sb.toString();
    }

    /**
     * Deserialise a cache-entry line. This is a minimal hand-rolled reader
     * — it trusts that the file was written by {@link #serialise(CacheEntry)}
     * and that no external tool has edited it. The companion test pins the
     * round-trip contract.
     */
    static CacheEntry deserialise(CacheKey key, String line) {
        String outcome = extractStringField(line, "outcome");
        long durationMs = Long.parseLong(extractRawField(line, "duration_ms"));
        String raw = extractRawField(line, "message");
        String message = raw.equals("null") ? null : unescapeString(raw);
        CacheEntry.Outcome o = CacheEntry.Outcome.valueOf(outcome);
        return new CacheEntry(key, o, durationMs, message);
    }

    // ---- JSON helpers ----

    private static void appendString(StringBuilder sb, String key, String value) {
        sb.append('"').append(key).append('"').append(':').append('"');
        escape(sb, value);
        sb.append('"');
    }

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

    /**
     * Extract a quoted-string JSON field by key. Returns the inner
     * unescaped string. Literal parsing only — the cache format is fixed,
     * so there is no ambiguity and no regex.
     */
    private static String extractStringField(String line, String key) {
        String raw = extractRawField(line, key);
        if (raw.length() < 2 || raw.charAt(0) != '"' || raw.charAt(raw.length() - 1) != '"') {
            throw new IllegalStateException("expected quoted string for field " + key + ": " + raw);
        }
        return unescapeString(raw);
    }

    /** Return the raw literal value (with surrounding quotes if string) for {@code key}. */
    private static String extractRawField(String line, String key) {
        String needle = "\"" + key + "\":";
        int start = line.indexOf(needle);
        if (start < 0) throw new IllegalStateException("missing field " + key + " in: " + line);
        int valueStart = start + needle.length();
        char first = line.charAt(valueStart);
        int end;
        if (first == '"') {
            end = valueStart + 1;
            while (end < line.length()) {
                char c = line.charAt(end);
                if (c == '\\') { end += 2; continue; }
                if (c == '"') { end++; break; }
                end++;
            }
        } else {
            end = valueStart;
            while (end < line.length()) {
                char c = line.charAt(end);
                if (c == ',' || c == '}') break;
                end++;
            }
        }
        return line.substring(valueStart, end);
    }

    private static String unescapeString(String quoted) {
        if (quoted.length() < 2) throw new IllegalStateException("not a quoted string: " + quoted);
        String body = quoted.substring(1, quoted.length() - 1);
        StringBuilder out = new StringBuilder(body.length());
        int i = 0;
        while (i < body.length()) {
            char c = body.charAt(i);
            if (c != '\\') { out.append(c); i++; continue; }
            if (i + 1 >= body.length()) throw new IllegalStateException("trailing backslash");
            char n = body.charAt(i + 1);
            switch (n) {
                case '"'  -> { out.append('"'); i += 2; }
                case '\\' -> { out.append('\\'); i += 2; }
                case 'n'  -> { out.append('\n'); i += 2; }
                case 'r'  -> { out.append('\r'); i += 2; }
                case 't'  -> { out.append('\t'); i += 2; }
                case 'b'  -> { out.append('\b'); i += 2; }
                case 'f'  -> { out.append('\f'); i += 2; }
                case 'u'  -> {
                    if (i + 6 > body.length()) throw new IllegalStateException("short \\uXXXX");
                    out.append((char) Integer.parseInt(body.substring(i + 2, i + 6), 16));
                    i += 6;
                }
                default -> throw new IllegalStateException("unknown escape: \\" + n);
            }
        }
        return out.toString();
    }
}
