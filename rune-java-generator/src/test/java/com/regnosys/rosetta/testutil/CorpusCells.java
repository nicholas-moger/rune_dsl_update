package com.regnosys.rosetta.testutil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal reader for {@code test-corpus/corpus-cells.tsv}, the corpus cell source
 * of truth, for use by the generator module's tests.
 *
 * <p>rune-parser has a fuller {@code CorpusCatalogue} over the same file, but the
 * modules do not share a test-jar, so this is a deliberate second reader of ONE
 * shared data file — not a second copy of the data. That distinction is the whole
 * point: before 2026-08-14 the cell list and its dependency pins were restated as
 * code in seven places, and the resulting drift manufactured thousands of phantom
 * "engine defects" the moment the corpus stopped being frozen.
 *
 * <p>Column contract is documented in the TSV header. Whitespace-separated,
 * {@code -} for empty, {@code #} comments.
 */
public final class CorpusCells {

    /** One row of the source of truth. */
    public record Row(String corpus, String version, boolean active, boolean catalogue,
                      Map<String, String> resolved) {
        public String dirName() {
            return corpus + "-" + version;
        }

        public String relPath() {
            return corpus + "/" + dirName();
        }
    }

    /**
     * Parsed rows, KEYED BY the resolved TSV path.
     *
     * <p>It was a single unkeyed field: the first caller's rows were returned to
     * every later caller regardless of the root it asked for, so a second root
     * would have silently answered with the first one's band. No caller passes a
     * second root today, which is exactly why it would have gone unnoticed —
     * a producer/consumer mismatch that reads as a cache hit (Copilot R1,
     * PR #566).
     */
    private static final java.util.Map<Path, List<Row>> CACHE = new java.util.HashMap<>();

    private CorpusCells() {
        // utility class — no instances
    }

    /** Every row, in file order. {@code testCorpusRoot} is the {@code test-corpus} dir. */
    public static synchronized List<Row> all(Path testCorpusRoot) {
        Path tsv = testCorpusRoot.resolve("corpus-cells.tsv").toAbsolutePath().normalize();
        List<Row> cached = CACHE.get(tsv);
        if (cached != null) {
            return cached;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(tsv);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read the corpus cell SOT at "
                    + tsv.toAbsolutePath().normalize(), e);
        }
        List<Row> rows = new ArrayList<>();
        int lineNo = 0;
        for (String raw : lines) {
            lineNo++;
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\\s+");
            if (f.length != 7) {
                throw new IllegalStateException(tsv + ":" + lineNo
                        + " expected 7 columns, found " + f.length + ": " + line);
            }
            // REJECT an unknown state/role rather than reading it as false. `"active"
            // .equals(f[2])` silently demoted any typo to inactive, which does not fail —
            // it QUIETLY SHRINKS THE TESTED MATRIX, the worst possible direction for a
            // misconfiguration to fail in. rune-parser's CorpusCatalogue, reading the SAME
            // file, has always thrown here; the two readers disagreeing about how strict
            // the format is meant the strict one could pass while this one dropped a cell
            // (Copilot R20, PR #566).
            boolean active = switch (f[2]) {
                case "active" -> true;
                case "planned" -> false;
                default -> throw new IllegalStateException(
                        tsv + ":" + lineNo + " state must be 'active' or 'planned', was: " + f[2]);
            };
            boolean catalogue = switch (f[3]) {
                case "catalogue" -> true;
                case "resolution" -> false;
                default -> throw new IllegalStateException(
                        tsv + ":" + lineNo + " role must be 'catalogue' or 'resolution', was: " + f[3]);
            };
            rows.add(new Row(f[0], f[1], active, catalogue, parseDeps(f[6], tsv, lineNo)));
        }
        if (rows.isEmpty()) {
            throw new IllegalStateException("no cells parsed from " + tsv);
        }
        List<Row> frozen = List.copyOf(rows);
        CACHE.put(tsv, frozen);
        return frozen;
    }

    private static Map<String, String> parseDeps(String field, Path tsv, int lineNo) {
        if ("-".equals(field)) {
            return Map.of();
        }
        Map<String, String> deps = new LinkedHashMap<>();
        for (String part : field.split(",")) {
            int eq = part.indexOf('=');
            if (eq <= 0 || eq == part.length() - 1) {
                throw new IllegalStateException(tsv + ":" + lineNo
                        + " dependency must be '<corpus>=<version>', was: " + part);
            }
            String corpus = part.substring(0, eq);
            String previous = deps.put(corpus, part.substring(eq + 1));
            if (previous != null) {
                // "cdm=6.21.0,cdm=5.38.0" used to keep the LAST silently, producing a
                // resolved closure that matches no declared pin and reads as deliberate.
                throw new IllegalStateException(tsv + ":" + lineNo
                        + " dependency corpus '" + corpus + "' appears twice in: " + field);
            }
        }
        return Map.copyOf(deps);
    }

    /** Active cells that carry their own goldens — the tested matrix, in file order. */
    public static List<Row> activeCatalogue(Path testCorpusRoot) {
        return all(testCorpusRoot).stream().filter(r -> r.active() && r.catalogue()).toList();
    }

    /**
     * The RESOLVED version map from one corpus to another, e.g.
     * {@code depMap(root, "drr", "cdm")} yields the DRR-version-to-CDM-version
     * pins. Only ACTIVE cells contribute, and only where the dependency exists —
     * so a corpus with no such dependency simply has no entry, exactly like the
     * hand-written maps this replaces.
     *
     * <p>The map reflects what is actually LOADED, which for drr 6.34.1 is a
     * deliberate substitution (it declares cdm 5.37.0 / iso 1.37.0; we load
     * 5.38.0 / 1.38.0). The substitution is visible in the TSV's declared-vs-
     * resolved columns rather than buried here.
     */
    public static Map<String, String> depMap(Path testCorpusRoot, String fromCorpus, String toCorpus) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Row r : all(testCorpusRoot)) {
            if (!r.active() || !r.corpus().equals(fromCorpus)) {
                continue;
            }
            String v = r.resolved().get(toCorpus);
            if (v != null) {
                out.put(r.version(), v);
            }
        }
        return Map.copyOf(out);
    }
}
