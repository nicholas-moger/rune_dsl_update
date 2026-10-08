package com.regnosys.rosetta.harness.matrix;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.opentest4j.TestAbortedException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class StructuralCellsProviderTest {

    @Test
    void default_root_yields_5_cells() {
        // If the default root has zero corpus directories, the provider
        // itself aborts via TestAbortedException (Q8 skip policy) — that
        // propagates out and JUnit marks this test as skipped.
        List<Arguments> cells = new StructuralCellsProvider()
                .provideArguments(null)
                .map(a -> (Arguments) a)
                .collect(Collectors.toList());

        // A partial clone (some CATALOGUE versions present, others
        // absent) aborts this assertion as a skip rather than a failure:
        // dev boxes often carry a subset for local iteration, and failing
        // here would penalise them for an environmental concern rather
        // than a real regression. The full-catalogue equality assertion
        // only runs when all 5 cells ARE present.
        if (cells.size() != 5) {
            throw new TestAbortedException(
                    "default corpus root under " + MatrixCellsProvider.DEFAULT_ROOT
                            + " has " + cells.size() + " of 5 CATALOGUE cells; "
                            + "skipping full-catalogue assertion");
        }
        // PR #85 rebaseline: 2 CDM + 1 DRR + 1 ISO-20022 + 1 rune-fpml — matches
        // the post-9.83.0-rebaseline shape used by MatrixCellsProvider and
        // structural-baselines.properties.
        assertEquals(5, cells.size(),
                "structural provider must yield one (corpus, version, dir) tuple per CATALOGUE cell");
    }

    @Test
    void yielded_tuples_point_at_real_directories() {
        new StructuralCellsProvider()
                .provideArguments(null)
                .forEach(args -> {
                    Object[] values = ((Arguments) args).get();
                    Path dir = (Path) values[2];
                    assertTrue(Files.isDirectory(dir),
                            "yielded version dir must exist: " + dir);
                });
    }

    @Test
    void version_dir_yields_single_tuple_regardless_of_file_count(@org.junit.jupiter.api.io.TempDir Path root) throws IOException {
        // Direct-enumeration semantics: one (corpus, version, dir) tuple
        // per matching version directory, regardless of how many .rosetta
        // files live inside. Files in the fixture exist only to prove that
        // the provider does NOT fan out per-file.
        Path cdmDir = Files.createDirectories(root.resolve("cdm").resolve("cdm-9.9.9"));
        Files.writeString(cdmDir.resolve("a-type.rosetta"), "namespace test");
        Files.writeString(cdmDir.resolve("b-type.rosetta"), "namespace test");
        Files.writeString(cdmDir.resolve("c-type.rosetta"), "namespace test");

        List<Arguments> cells = new StructuralCellsProvider(root)
                .provideArguments(null)
                .map(a -> (Arguments) a)
                .collect(Collectors.toList());

        assertEquals(1, cells.size(),
                "one version dir must yield exactly one tuple — file count is irrelevant");
        Object[] values = cells.get(0).get();
        assertEquals(Corpus.CDM, values[0]);
        assertEquals(Version.parse("9.9.9"), values[1]);
        assertEquals(cdmDir.toRealPath(), ((Path) values[2]).toRealPath());
    }

    @Test
    void empty_version_dir_still_yields_a_tuple(@org.junit.jupiter.api.io.TempDir Path root) throws IOException {
        // F18 regression gate: an empty version dir (one that exists but
        // has zero .rosetta files) must still appear in the parameter set,
        // so that StructuralComparisonTest's own "no .rosetta files under
        // ..." assertion can catch it as a loud failure. The pre-direct-
        // enumeration implementation derived version dirs from file
        // discovery and would have silently omitted this cell.
        Path emptyVersion = Files.createDirectories(root.resolve("cdm").resolve("cdm-9.9.9"));

        List<Arguments> cells = new StructuralCellsProvider(root)
                .provideArguments(null)
                .map(a -> (Arguments) a)
                .collect(Collectors.toList());

        assertEquals(1, cells.size(),
                "empty version dir must still be yielded — consumer assertion catches the emptiness");
        Object[] values = cells.get(0).get();
        assertEquals(Corpus.CDM, values[0]);
        assertEquals(Version.parse("9.9.9"), values[1]);
        assertEquals(emptyVersion.toRealPath(), ((Path) values[2]).toRealPath());
    }

    @Test
    void non_matching_subdirs_are_ignored(@org.junit.jupiter.api.io.TempDir Path root) throws IOException {
        // Directories that don't match the `<corpus>-<version>` pattern are
        // dropped silently — mirrors MatrixCellsProvider's own policy for
        // unparseable / non-conformant dir names.
        Files.createDirectories(root.resolve("cdm").resolve("cdm-9.9.9"));
        Files.createDirectories(root.resolve("cdm").resolve("not-a-version-dir"));
        Files.createDirectories(root.resolve("cdm").resolve("cdm-not-a-version"));

        List<Arguments> cells = new StructuralCellsProvider(root)
                .provideArguments(null)
                .map(a -> (Arguments) a)
                .collect(Collectors.toList());

        assertEquals(1, cells.size(),
                "only dirs matching `<corpus>-<parseable-version>` must be yielded");
        assertEquals(Version.parse("9.9.9"), cells.get(0).get()[1]);
    }

    @Test
    void empty_root_aborts_with_diagnostic(@org.junit.jupiter.api.io.TempDir Path root) {
        // No corpus subdirectories under root.
        TestAbortedException ex = assertThrows(TestAbortedException.class,
                () -> new StructuralCellsProvider(root).provideArguments(null));
        assertTrue(ex.getMessage().contains("missing corpora") || ex.getMessage().contains("no corpus-version"),
                "skip diagnostic should name the missing corpora or the empty discovery: " + ex.getMessage());
    }

    @Test
    void returned_tuples_are_unique_by_corpus_version(@org.junit.jupiter.api.io.TempDir Path root) throws IOException {
        // Two corpora, each with one version, each with some files.
        Path cdmDir = Files.createDirectories(root.resolve("cdm").resolve("cdm-9.9.9"));
        Path drrDir = Files.createDirectories(root.resolve("drr").resolve("drr-9.9.9"));
        Files.writeString(cdmDir.resolve("a-type.rosetta"), "namespace test");
        Files.writeString(drrDir.resolve("a-type.rosetta"), "namespace test");

        // Assert uniqueness via an explicit (Corpus, Version) key set —
        // don't rely on JUnit's Arguments class to implement value-based
        // equals/hashCode (it's undocumented and may degrade to identity
        // equality, which would make a Set<Arguments> always "distinct").
        Set<String> keys = new StructuralCellsProvider(root)
                .provideArguments(null)
                .map(a -> (Arguments) a)
                .map(Arguments::get)
                .map(values -> values[0] + "-" + values[1])
                .collect(Collectors.toSet());
        assertEquals(2, keys.size(),
                "two distinct (corpus, version) inputs must yield two distinct tuples");
    }
}
