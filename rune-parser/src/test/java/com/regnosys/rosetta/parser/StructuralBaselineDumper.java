package com.regnosys.rosetta.parser;

import com.regnosys.rosetta.harness.matrix.Corpus;
import com.regnosys.rosetta.harness.matrix.MatrixCellsProvider;
import com.regnosys.rosetta.harness.matrix.MatrixCoordinate;
import com.regnosys.rosetta.harness.matrix.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Dev-only utility that extracts {@link RosettaStructureExtractor}
 * element-kind counts for every {@code (corpus, version)} cell in the
 * {@code test-corpus/} tree, prints them in a human-readable table, AND
 * writes a canonical {@code structural-baselines.properties} file that
 * {@link StructuralComparisonTest} consumes at test time.
 *
 * <p>P1.2 audit hook H26b — replaces the previous hardcoded CDM 6.16.0 /
 * DRR 6.28.0 baselines with a version-keyed table covering all 13
 * CATALOGUE cells. The committed properties file is a versioned snapshot,
 * not a generated artefact; refreshes should land as their own commit.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 *   mvn test -Dtest=StructuralBaselineDumper -Dstructural.dump=true
 * }</pre>
 *
 * <p>Behaviour:
 * <ul>
 *   <li>Prints a human-readable per-cell table to stdout (for commit
 *       messages / review).</li>
 *   <li>Writes {@code src/test/resources/structural-baselines.properties}
 *       with one entry per {@code (corpus-version).kind} pair, including
 *       explicit {@code =0} entries for every one of the 18 known kinds
 *       — so an extractor regression that starts emitting a previously-
 *       zero kind fails loudly.</li>
 *   <li>Refuses to emit if any cell had parse errors (self-certifying).</li>
 *   <li>Refuses to emit if discovery did not yield the full 13-cell
 *       CATALOGUE — partial corpus checkouts or wrong-directory runs
 *       must not overwrite the committed baseline with truncated data.</li>
 * </ul>
 */
class StructuralBaselineDumper {

    /**
     * Every element kind emitted by {@link RosettaStructureExtractor}.
     * The dumper writes an explicit {@code =0} entry for every kind not
     * observed in a given cell, so the baseline is a fully-specified
     * fingerprint rather than "absence means default".
     */
    static final List<String> ALL_KINDS = List.of(
            "annotation", "basicType", "body", "choice", "corpus",
            "enum", "externalRuleSource", "externalSynonymSource",
            "func", "libraryFunction", "metaType", "recordType",
            "report", "rule", "segment", "synonymSource", "type",
            "typeAlias"
    );

    /**
     * Canonical list of the 13 CATALOGUE cells this dumper expects to
     * discover. Keyed as {@code <corpus.dirName>-<version.display>} — the
     * same key format {@code structural-baselines.properties} uses. The
     * dumper refuses to emit unless the discovered cell-set matches this
     * exactly. That rules out the two ways partial discovery silently
     * corrupts the committed baseline:
     * <ul>
     *   <li>Running from the wrong working directory (discovery finds a
     *       strict subset of the CATALOGUE — the committed baseline would
     *       shrink to whatever happened to be on disk).</li>
     *   <li>A test-corpus checkout where a version has been renamed or
     *       replaced without updating this list — the dumper surfaces the
     *       divergence instead of quietly rebaselining against it.</li>
     * </ul>
     */
    /**
     * DERIVED from {@code test-corpus/corpus-cells.tsv} — the corpus cell source of
     * truth — rather than restated here. PR #85 hardcoded 5 cells, which was harmless
     * only while the corpus was frozen; the 2026-08-14 band expansion turned every
     * such hand-maintained copy into a drift surface. Deriving means this list cannot
     * fall out of step, instead of merely being policed for having done so.
     */
    static final Set<String> EXPECTED_CELLS =
            com.regnosys.rosetta.testutil.CorpusCatalogue.activeCatalogue().stream()
                    .map(com.regnosys.rosetta.testutil.CorpusCatalogue.Cell::dirName)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());

    /** Where the canonical baseline file is written. */
    private static final Path OUTPUT_FILE =
            Path.of("src", "test", "resources", "structural-baselines.properties")
                    .toAbsolutePath().normalize();

    @Test
    @EnabledIfSystemProperty(named = "structural.dump", matches = "true",
            disabledReason = "Dev-only baseline dumper — run with -Dstructural.dump=true")
    void dump_counts_for_every_cell() throws IOException {
        Path root = MatrixCellsProvider.DEFAULT_ROOT;

        // Group discovered files by (corpus, version) so we count per cell.
        Map<CorpusVersion, List<MatrixCoordinate>> byCell = MatrixCellsProvider.discover(root)
                .collect(Collectors.groupingBy(
                        c -> new CorpusVersion(c.corpus(), c.version()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        // Refuse to emit unless discovery matches the full CATALOGUE exactly.
        // Runs from the wrong working directory or against a partial corpus
        // checkout would otherwise overwrite the committed 13-cell baseline
        // with a truncated subset. A superset (unknown versions on disk)
        // fails equally loudly — an unexpected version slipping into the
        // baseline without updating EXPECTED_CELLS is a real drift signal.
        Set<String> discoveredNames = byCell.keySet().stream()
                .map(cv -> cv.corpus().dirName() + "-" + cv.version().display())
                .collect(Collectors.toCollection(TreeSet::new));
        if (!discoveredNames.equals(EXPECTED_CELLS)) {
            Set<String> missing = new TreeSet<>(EXPECTED_CELLS);
            missing.removeAll(discoveredNames);
            Set<String> extra = new TreeSet<>(discoveredNames);
            extra.removeAll(EXPECTED_CELLS);
            throw new AssertionError(
                    "Refusing to emit baselines: discovered " + discoveredNames.size()
                            + " cells under " + root.toAbsolutePath().normalize()
                            + ", expected " + EXPECTED_CELLS.size() + " CATALOGUE cells."
                            + " Missing=" + missing + " Extra=" + extra
                            + ". Fix the test-corpus checkout (or update EXPECTED_CELLS if"
                            + " the CATALOGUE has legitimately changed) before regenerating.");
        }

        System.out.println();
        System.out.println("# Structural baseline counts (H26b research)");
        System.out.println("# corpus-root: " + root.toAbsolutePath().normalize());
        System.out.println();

        // Sort cells so the output order is stable across machines.
        List<Map.Entry<CorpusVersion, List<MatrixCoordinate>>> sorted = new ArrayList<>(byCell.entrySet());
        sorted.sort(Comparator.<Map.Entry<CorpusVersion, List<MatrixCoordinate>>, Corpus>comparing(
                        e -> e.getKey().corpus())
                .thenComparing(e -> e.getKey().version()));

        int totalParseErrors = 0;
        Map<String, Map<String, Integer>> perCell = new LinkedHashMap<>();
        Map<String, Integer> perCellFiles = new LinkedHashMap<>();
        Map<String, Integer> perCellRefused = new LinkedHashMap<>();
        for (Map.Entry<CorpusVersion, List<MatrixCoordinate>> entry : sorted) {
            CorpusVersion cv = entry.getKey();
            List<MatrixCoordinate> cells = entry.getValue();
            CellResult result = extractCounts(cells);
            totalParseErrors += result.parseErrors;

            String cellName = cv.corpus().dirName() + "-" + cv.version().display();
            perCell.put(cellName, result.counts);
            perCellFiles.put(cellName, cells.size());
            perCellRefused.put(cellName, result.parseRefused);
            System.out.println("## " + cellName + "  (files: " + cells.size()
                    + ", elements: " + result.total() + ", parse_errors: "
                    + result.parseErrors + ")");
            for (Map.Entry<String, Integer> e : result.counts.entrySet()) {
                if (e.getValue() == 0) continue;
                System.out.printf("  %-28s %6d%n", e.getKey(), e.getValue());
            }
            System.out.println();
        }

        // Fail loud if any cell had parse errors: freezing a baseline from
        // a partially-parsed corpus would silently bake undercount into
        // StructuralComparisonTest. Per the independent review on H26b,
        // a baseline must be self-certifying.
        if (totalParseErrors > 0) {
            throw new AssertionError(
                    "Refusing to emit baselines: " + totalParseErrors
                            + " parse error(s) across the 5-version matrix. "
                            + "Fix the grammar / corpus first, then re-run the dumper.");
        }

        writeBaselinesFile(OUTPUT_FILE, perCell, perCellFiles, perCellRefused);
        System.out.println("# wrote " + OUTPUT_FILE);
    }

    /**
     * Emit the canonical properties file consumed by
     * {@link StructuralComparisonTest}. Every {@code (cell, kind)} pair
     * gets an explicit entry, including {@code =0} for the three kinds
     * ({@code basicType}, {@code recordType}, {@code libraryFunction})
     * that are unused across all 13 current versions — so an extractor
     * regression that starts emitting them fails loudly against the
     * frozen baseline.
     */
    private static void writeBaselinesFile(Path out,
                                           Map<String, Map<String, Integer>> perCell,
                                           Map<String, Integer> perCellFiles,
                                           Map<String, Integer> perCellRefused) throws IOException {
        Files.createDirectories(out.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            w.write("# H26b — structural-baseline counts per (corpus, version)"); w.newLine();
            w.write("#"); w.newLine();
            w.write("# Keys: <corpus>-<version>.<kind>"); w.newLine();
            w.write("# Values: integer element-kind count from RosettaStructureExtractor"); w.newLine();
            w.write("#"); w.newLine();
            w.write("# Every cell is fully specified — all 18 known kinds have an entry,"); w.newLine();
            w.write("# including =0 for kinds that happen to be absent. A 0→N drift on a"); w.newLine();
            w.write("# previously-zero kind is a real grammar/extractor regression signal."); w.newLine();
            w.write("#"); w.newLine();
            w.write("# Regenerate: mvn test -Dtest=StructuralBaselineDumper -Dstructural.dump=true"); w.newLine();
            // Header is intentionally date-free: an unchanged corpus + extractor
            // must produce a byte-identical properties file, so `git diff` after
            // a regeneration is an exact signal of real count drift rather than
            // a reshuffled timestamp. A dated stamp would defeat that property.
            w.newLine();

            for (Map.Entry<String, Map<String, Integer>> cell : perCell.entrySet()) {
                String cellName = cell.getKey();
                int files = perCellFiles.get(cellName);
                int total = cell.getValue().values().stream().mapToInt(Integer::intValue).sum();
                int refused = perCellRefused.getOrDefault(cellName, 0);
                // The chaos header says what its files= actually counts (the cq
                // review's NIT: 606 walked, 22 parse-refused by design, elements
                // from the 584 that parse) — every other cell reads as before.
                w.write("# " + cellName + "  (files=" + files + ", elements=" + total
                        + (refused == 0 ? "" : ", parseRefusedByDesign=" + refused)
                        + ")");
                w.newLine();
                for (String kind : ALL_KINDS) {
                    int count = cell.getValue().getOrDefault(kind, 0);
                    w.write(cellName + "." + kind + "=" + count);
                    w.newLine();
                }
                w.newLine();
            }
        }
    }

    private static CellResult extractCounts(List<MatrixCoordinate> cells) {
        Map<String, Integer> counts = new TreeMap<>();
        // Count individual diagnostics rather than files-with-errors so the
        // `parse_errors: N` / `N parse error(s)` labels printed downstream
        // match what the number actually measures. A file with three syntax
        // errors contributes 3, not 1 — consistent with how humans read the
        // dumper output when auditing a corpus refresh.
        int parseErrors = 0;
        int parseRefused = 0;
        for (MatrixCoordinate cell : cells) {
            RosettaParseResult result = RosettaParserFacade.parseFile(cell.source());
            // The chaos cell's 22 expected-refusal files (v3.2 charter § 4b; the
            // committed fork-diagnostics.tsv) refuse BY DESIGN and contribute zero
            // elements — they are not parse errors this dumper should refuse to
            // emit over. A file expected to refuse that parses CLEAN fails loud
            // instead (LAW 81 — the refusal healing must never bake silently into
            // a baseline). This branch MIRRORS StructuralComparisonTest.extractCounts
            // exactly: producer and consumer must count the same universe.
            if (cell.corpus() == Corpus.CHAOS
                    && ChaosParseExpectations.isExpectedRefusal(cell.source())) {
                if (result.errors().isEmpty()) {
                    throw new AssertionError("expected-refusal chaos file PARSED CLEAN — the"
                            + " pinned refusal has healed; re-adjudicate " + cell.source()
                            + " against " + ChaosParseExpectations.FORK_DIAGNOSTICS_TSV
                            + " before regenerating baselines");
                }
                parseRefused++;
                continue;
            }
            if (!result.errors().isEmpty()) {
                parseErrors += result.errors().size();
                System.err.println("[parse errors] " + cell.source() + ": "
                        + result.errors().size() + " error(s)");
                continue;
            }
            RosettaStructureExtractor extractor = new RosettaStructureExtractor();
            RosettaStructureExtractor.FileStructure structure =
                    extractor.extract((RosettaParser.RosettaModelContext) result.tree());
            // Mirror the namespace invariant asserted by
            // StructuralComparisonTest. If a file lands in the corpus
            // without a namespace header the comparison test would fail
            // on every run; catching it here stops the dumper from
            // baking a broken baseline that the test would then reject.
            if (structure.namespace() == null) {
                throw new AssertionError(
                        "missing namespace in " + cell.source()
                                + " — refusing to dump baseline from corpus with invariant violation");
            }
            for (RosettaStructureExtractor.ElementInfo elem : structure.elements()) {
                counts.merge(elem.kind(), 1, Integer::sum);
            }
        }
        return new CellResult(counts, parseErrors, parseRefused);
    }

    private record CellResult(Map<String, Integer> counts, int parseErrors, int parseRefused) {
        int total() {
            return counts.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    private record CorpusVersion(Corpus corpus, Version version) {}
}
