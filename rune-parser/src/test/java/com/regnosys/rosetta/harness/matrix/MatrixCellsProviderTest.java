package com.regnosys.rosetta.harness.matrix;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.TestAbortedException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class MatrixCellsProviderTest {

    @Test
    void discover_yields_one_coordinate_per_rosetta_file_under_corpus_version(@TempDir Path root) throws IOException {
        Path cdmVer = mkDirs(root, "cdm", "cdm-6.16.0", "rosetta-source", "src", "main", "rosetta");
        touch(cdmVer.resolve("base-datetime-type.rosetta"));
        touch(cdmVer.resolve("base-math-enum.rosetta"));

        List<MatrixCoordinate> cells = MatrixCellsProvider.discover(root).toList();

        assertEquals(2, cells.size(), "expected one cell per .rosetta file");
        for (MatrixCoordinate mc : cells) {
            assertEquals(Corpus.CDM, mc.corpus());
            assertEquals("6.16.0", mc.version().display());
        }
    }

    @Test
    void discover_classifies_element_kind_by_filename_suffix() throws IOException {
        Path root = Files.createTempDirectory("matrix-kind");
        try {
            Path ver = mkDirs(root, "cdm", "cdm-6.16.0", "rosetta-source", "src", "main", "rosetta");
            touch(ver.resolve("a-type.rosetta"));
            touch(ver.resolve("a-enum.rosetta"));
            touch(ver.resolve("a-func.rosetta"));
            touch(ver.resolve("a-rule.rosetta"));
            touch(ver.resolve("a-synonym.rosetta"));
            touch(ver.resolve("a-annotations.rosetta"));
            touch(ver.resolve("a-basictypes.rosetta"));
            touch(ver.resolve("a-desc.rosetta"));
            touch(ver.resolve("no-suffix-match.rosetta"));

            List<MatrixCoordinate> cells = MatrixCellsProvider.discover(root).toList();
            var byFile = cells.stream().collect(Collectors.toMap(
                    mc -> mc.source().getFileName().toString(),
                    MatrixCoordinate::kind));

            assertEquals(ElementKind.TYPE, byFile.get("a-type.rosetta"));
            assertEquals(ElementKind.ENUM, byFile.get("a-enum.rosetta"));
            assertEquals(ElementKind.FUNCTION, byFile.get("a-func.rosetta"));
            assertEquals(ElementKind.RULE, byFile.get("a-rule.rosetta"));
            assertEquals(ElementKind.SYNONYM, byFile.get("a-synonym.rosetta"));
            assertEquals(ElementKind.ANNOTATION, byFile.get("a-annotations.rosetta"));
            assertEquals(ElementKind.BASIC_TYPE, byFile.get("a-basictypes.rosetta"));
            assertEquals(ElementKind.UNKNOWN, byFile.get("a-desc.rosetta"),
                    "desc files are descriptive and multi-kind — UNKNOWN is the honest classification");
            assertEquals(ElementKind.UNKNOWN, byFile.get("no-suffix-match.rosetta"));
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    void discover_parses_version_from_directory_name() throws IOException {
        Path root = Files.createTempDirectory("matrix-versions");
        try {
            Path cdmOld = mkDirs(root, "cdm", "cdm-6.10.0", "rosetta-source", "src", "main", "rosetta");
            Path cdmPre = mkDirs(root, "cdm", "cdm-7.0.0-dev.98", "rosetta-source", "src", "main", "rosetta");
            Path drrAsc = mkDirs(root, "drr", "drr-7.0.0-asc.96", "rosetta-source", "src", "main", "rosetta");
            touch(cdmOld.resolve("x-type.rosetta"));
            touch(cdmPre.resolve("x-type.rosetta"));
            touch(drrAsc.resolve("x-type.rosetta"));

            var versions = MatrixCellsProvider.discover(root)
                    .map(mc -> mc.corpus() + "/" + mc.version().display())
                    .sorted()
                    .toList();

            assertEquals(List.of(
                    "CDM/6.10.0",
                    "CDM/7.0.0-dev.98",
                    "DRR/7.0.0-asc.96"
            ), versions);
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    void discover_yields_deterministic_ordering(@TempDir Path root) throws IOException {
        Path ver = mkDirs(root, "cdm", "cdm-6.16.0", "rosetta-source", "src", "main", "rosetta");
        // Touch files in reverse alpha order to make sure provider doesn't leak filesystem order.
        touch(ver.resolve("z-type.rosetta"));
        touch(ver.resolve("m-enum.rosetta"));
        touch(ver.resolve("a-func.rosetta"));

        List<MatrixCoordinate> first = MatrixCellsProvider.discover(root).toList();
        List<MatrixCoordinate> second = MatrixCellsProvider.discover(root).toList();

        assertEquals(first, second, "two discover() calls must return identical ordered lists");
        // And the ordering must follow MatrixCoordinate.compareTo (natural order).
        List<MatrixCoordinate> sorted = first.stream().sorted().toList();
        assertEquals(sorted, first, "discover() must yield cells in natural MatrixCoordinate order");
    }

    @Test
    void discover_ignores_unknown_top_level_dirs() throws IOException {
        Path root = Files.createTempDirectory("matrix-ignore");
        try {
            Path bogus = mkDirs(root, "not-a-corpus", "not-a-corpus-1.0.0", "rosetta-source");
            touch(bogus.resolve("x-type.rosetta"));
            Path cdmVer = mkDirs(root, "cdm", "cdm-6.16.0", "rosetta-source", "src", "main", "rosetta");
            touch(cdmVer.resolve("real-type.rosetta"));

            List<MatrixCoordinate> cells = MatrixCellsProvider.discover(root).toList();
            assertEquals(1, cells.size(), "only recognised corpora should yield cells");
            assertEquals("real-type.rosetta", cells.get(0).source().getFileName().toString());
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    void discover_ignores_version_dirs_with_mismatched_prefix() throws IOException {
        Path root = Files.createTempDirectory("matrix-prefix");
        try {
            // Inside cdm/ there's an accidental drr-shaped dir — prefix must match.
            Path wrong = mkDirs(root, "cdm", "drr-6.28.0", "rosetta-source");
            touch(wrong.resolve("x-type.rosetta"));
            Path right = mkDirs(root, "cdm", "cdm-6.16.0", "rosetta-source", "src", "main", "rosetta");
            touch(right.resolve("real-type.rosetta"));

            List<MatrixCoordinate> cells = MatrixCellsProvider.discover(root).toList();
            assertEquals(1, cells.size());
            assertEquals("real-type.rosetta", cells.get(0).source().getFileName().toString());
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    void discover_of_missing_root_returns_empty_stream_with_no_exception() throws IOException {
        Path missing = Files.createTempDirectory("matrix-empty").resolve("does-not-exist");
        assertFalse(Files.exists(missing));
        List<MatrixCoordinate> cells = MatrixCellsProvider.discover(missing).toList();
        assertTrue(cells.isEmpty(), "missing root is a skip-by-diagnostic scenario, not a crash");
    }

    @Test
    void missingCorporaDiagnostic_lists_all_expected_corpora_when_root_is_empty(@TempDir Path root) {
        // Root exists but has no corpus directories.
        String msg = MatrixCellsProvider.missingCorporaDiagnostic(root);
        for (Corpus c : Corpus.values()) {
            assertTrue(msg.contains(c.dirName()),
                    "diagnostic should mention missing corpus " + c.dirName() + ", got: " + msg);
        }
    }

    @Test
    void missingCorporaDiagnostic_only_lists_actually_missing_corpora(@TempDir Path root) throws IOException {
        mkDirs(root, "cdm", "cdm-6.16.0");
        mkDirs(root, "drr", "drr-7.0.0-dev.111");

        String msg = MatrixCellsProvider.missingCorporaDiagnostic(root);
        assertFalse(msg.contains("cdm"), "present corpus should not be listed as missing");
        assertFalse(msg.contains("drr"), "present corpus should not be listed as missing");
        assertTrue(msg.contains("iso20022"));
        assertTrue(msg.contains("rune-fpml"));
    }

    @Test
    void provideArguments_aborts_with_diagnostic_when_no_cells_discovered(@TempDir Path root) {
        // Empty root — provideArguments must abort (skip) the parameterized test,
        // not return an empty stream (which JUnit reports as a CONFIGURATION ERROR,
        // not a skip).
        MatrixCellsProvider provider = new MatrixCellsProvider(root);
        TestAbortedException abort = assertThrows(TestAbortedException.class,
                () -> provider.provideArguments(null));
        String msg = abort.getMessage().toLowerCase();
        assertTrue(msg.contains("corpora") || msg.contains("corpus"),
                "abort message should mention corpora; got: " + abort.getMessage());
    }

    @Test
    void provideArguments_yields_arguments_when_cells_exist(@TempDir Path root) throws IOException {
        Path ver = mkDirs(root, "cdm", "cdm-6.16.0", "rosetta-source");
        touch(ver.resolve("a-type.rosetta"));
        MatrixCellsProvider provider = new MatrixCellsProvider(root);
        long count = provider.provideArguments(null).count();
        assertEquals(1L, count);
    }

    // ---- helpers ----

    private static Path mkDirs(Path root, String... parts) throws IOException {
        Path p = root;
        for (String part : parts) p = p.resolve(part);
        Files.createDirectories(p);
        return p;
    }

    private static void touch(Path p) throws IOException {
        Files.createDirectories(p.getParent());
        Files.writeString(p, "namespace test\n");
    }

    private static void deleteRecursively(Path p) throws IOException {
        if (!Files.exists(p)) return;
        try (var s = Files.walk(p)) {
            s.sorted(java.util.Comparator.reverseOrder()).forEach(f -> {
                try { Files.deleteIfExists(f); } catch (IOException ignored) {}
            });
        }
    }
}
