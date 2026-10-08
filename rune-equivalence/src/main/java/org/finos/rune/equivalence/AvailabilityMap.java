package org.finos.rune.equivalence;

import org.finos.rune.benchmarks.corpus.CorpusClasses;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Leg S's per-cell availability map (the plan § 4.1: "absence is RECORDED,
 * never silently skipped"): for each corpus cell, the known sample roots are
 * walked and censused by file extension; a root that does not exist is a
 * RECORDED absence line, not a skip. The printed map is part of the equiv
 * receipt and is the authoritative count surface (the census doc
 * research/p3-rosetta-common-census.md § 7 defers to it).
 *
 * <p>The three roots mirror the corpus cells' own build layout (all under
 * {@code rosetta-source/src/main/resources/}): {@code result-json-files}
 * (CDM-instance JSONs — the loadable leg-S sets), {@code available-samples}
 * (instance JSONs), and {@code cdm-sample-files} (FpML XML ingestion inputs +
 * per-set {@code expectations.json} — XML enters when an XML ingestion route
 * is wired; until then the map says so).
 */
public final class AvailabilityMap {

    /** One sample root's census: per-extension file counts (absent ⇒ {@code null} counts). */
    public record RootCensus(String cell, String root, boolean present,
                             Map<String, Integer> countsByExtension) {
        public int total() {
            return countsByExtension == null ? 0
                    : countsByExtension.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    /** The sample roots recognised by this map version, relative to the cell dir. */
    public static final List<String> SAMPLE_ROOTS = List.of(
            "rosetta-source/src/main/resources/result-json-files",
            "rosetta-source/src/main/resources/available-samples",
            "rosetta-source/src/main/resources/cdm-sample-files");

    private AvailabilityMap() {}

    /** Census every root of {@code cellRel} (e.g. {@code "cdm/cdm-5.38.0"}). */
    public static List<RootCensus> census(String cellRel) {
        Path cell = CorpusClasses.repoRoot().resolve("test-corpus").resolve(cellRel);
        List<RootCensus> out = new ArrayList<>();
        for (String root : SAMPLE_ROOTS) {
            Path dir = cell.resolve(root);
            if (!Files.isDirectory(dir)) {
                out.add(new RootCensus(cellRel, root, false, null));
                continue;
            }
            Map<String, Integer> counts = new TreeMap<>();
            try (Stream<Path> s = Files.walk(dir)) {
                s.filter(Files::isRegularFile).forEach(p -> {
                    String name = p.getFileName().toString();
                    int dot = name.lastIndexOf('.');
                    String ext = dot < 0 ? "<none>" : name.substring(dot + 1).toLowerCase();
                    counts.merge(ext, 1, Integer::sum);
                });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            out.add(new RootCensus(cellRel, root, true, counts));
        }
        return out;
    }

    /** Print one cell's map lines (the receipt surface). */
    public static void print(String cellRel, PrintStream out) {
        for (RootCensus c : census(cellRel)) {
            if (!c.present()) {
                out.printf("[AvailabilityMap %s] %s ABSENT (recorded, not a skip)%n",
                        c.cell(), c.root());
            } else {
                out.printf("[AvailabilityMap %s] %s total=%d %s%n",
                        c.cell(), c.root(), c.total(), c.countsByExtension());
            }
        }
    }
}
