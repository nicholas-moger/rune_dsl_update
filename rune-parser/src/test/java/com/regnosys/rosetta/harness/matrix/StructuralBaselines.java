package com.regnosys.rosetta.harness.matrix;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Loads the per-{@code (corpus, version)} structural-count baseline table
 * that {@code StructuralComparisonTest} asserts against.
 *
 * <p>P1.2 audit hook H26b. The committed resource
 * ({@value #DEFAULT_RESOURCE}) is produced by
 * {@code StructuralBaselineDumper} and pins an expected count for every
 * {@code (corpus-version, kind)} pair across all 13 CATALOGUE versions
 * (13 cells × 18 kinds = 234 entries). Kinds absent from a given cell
 * are pinned {@code =0} so an extractor regression that starts emitting
 * a previously-zero kind fails loudly.
 *
 * <h2>Key format</h2>
 * <pre>
 * &lt;corpus-dirName&gt;-&lt;version-display&gt;.&lt;kind&gt;=&lt;count&gt;
 *
 * e.g.  cdm-6.18.0.type=719
 *       drr-7.0.0-dev.113.rule=2033
 * </pre>
 *
 * <p>The loader is intentionally strict: a missing resource throws,
 * because running {@code StructuralComparisonTest} without baselines
 * would assert nothing. A missing cell (no entries for a given corpus
 * and version) returns an empty map; {@code StructuralComparisonTest}
 * treats that as a hard assertion failure (not a skip) so a discovered
 * {@code (corpus, version)} that lacks a baseline row fails loudly —
 * it likely means a version was added without regenerating the table.
 */
public final class StructuralBaselines {

    public static final String DEFAULT_RESOURCE = "/structural-baselines.properties";

    private final Map<String, Map<String, Integer>> byCell;

    private StructuralBaselines(Map<String, Map<String, Integer>> byCell) {
        // Deep-unmodifiable — the record wrapping Properties in tests
        // shouldn't be able to mutate our parsed state.
        Map<String, Map<String, Integer>> copy = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> e : byCell.entrySet()) {
            copy.put(e.getKey(), Collections.unmodifiableMap(new TreeMap<>(e.getValue())));
        }
        this.byCell = Collections.unmodifiableMap(copy);
    }

    /**
     * Load baselines from {@link #DEFAULT_RESOURCE}. Missing resource
     * throws — baselines are a correctness gate; a silent empty table
     * would turn {@code StructuralComparisonTest} into a no-op.
     */
    public static StructuralBaselines fromDefaultResource() {
        try (InputStream in = StructuralBaselines.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(
                        "missing " + DEFAULT_RESOURCE
                                + " on classpath — regenerate via StructuralBaselineDumper");
            }
            // Load via a UTF-8 Reader rather than the raw InputStream —
            // Properties#load(InputStream) decodes as ISO-8859-1 per the
            // properties-file spec, but the dumper writes this file as
            // UTF-8 (header comments contain em-dashes and other non-ASCII
            // text). Using Reader guarantees the decoder matches the
            // writer and future non-ASCII entries in keys/values round-trip
            // correctly.
            Properties props = new Properties();
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                props.load(reader);
            }
            return fromProperties(props);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "failed to load " + DEFAULT_RESOURCE + ": " + e.getMessage(), e);
        }
    }

    /** Build from an explicit {@link Properties} (useful for tests). */
    public static StructuralBaselines fromProperties(Properties props) {
        Objects.requireNonNull(props, "props");
        Map<String, Map<String, Integer>> byCell = new HashMap<>();
        for (String key : props.stringPropertyNames()) {
            int lastDot = key.lastIndexOf('.');
            if (lastDot <= 0 || lastDot == key.length() - 1) {
                // Malformed key — skip silently. Matches the "degrade
                // gracefully" discipline used elsewhere in the harness
                // (RuleWeights, MatrixCellsProvider layout-parsing).
                continue;
            }
            String cell = key.substring(0, lastDot);
            String kind = key.substring(lastDot + 1);
            int count;
            try {
                count = Integer.parseInt(props.getProperty(key).trim());
            } catch (NumberFormatException e) {
                continue;
            }
            byCell.computeIfAbsent(cell, c -> new TreeMap<>()).put(kind, count);
        }
        return new StructuralBaselines(byCell);
    }

    /**
     * Expected counts for one {@code (corpus, version)} cell. Returns
     * an empty map if the cell isn't in the table — callers should treat
     * that as a missing baseline row and fail with a diagnostic rather
     * than silently skipping the comparison, since a discovered-but-
     * unbaselined cell almost always signals a forgotten regeneration.
     *
     * <p>Cell key format: {@code <corpus.dirName>-<version.display>}.
     */
    public Map<String, Integer> forCell(Corpus corpus, Version version) {
        String key = corpus.dirName() + "-" + version.display();
        Map<String, Integer> row = byCell.get(key);
        return row == null ? Collections.emptyMap() : row;
    }

    /** Number of {@code (corpus, version)} cells loaded. */
    public int cellCount() {
        return byCell.size();
    }
}
