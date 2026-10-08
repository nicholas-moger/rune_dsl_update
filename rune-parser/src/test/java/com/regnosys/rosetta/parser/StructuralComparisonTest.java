package com.regnosys.rosetta.parser;

import com.regnosys.rosetta.harness.matrix.Corpus;
import com.regnosys.rosetta.harness.matrix.StructuralBaselines;
import com.regnosys.rosetta.harness.matrix.StructuralCellsProvider;
import com.regnosys.rosetta.harness.matrix.Version;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * Structural comparison baseline test — every {@code (corpus, version)}
 * cell in the CATALOGUE asserts its element-kind counts against the
 * committed baseline table.
 *
 * <p>P1.2 audit hook H26b. Replaces the previous hardcoded pair of
 * baselines (CDM 6.16.0 + DRR 6.28.0) with a 13-cell version-keyed
 * matrix driven by {@link StructuralCellsProvider}. Each cell parses
 * every {@code .rosetta} file under the version directory, sums
 * {@link RosettaStructureExtractor} element-kind counts, and asserts
 * per-kind equality against the baseline in
 * {@code structural-baselines.properties}. The file is produced by
 * {@code StructuralBaselineDumper} and pins all 18 known kinds per
 * cell (including {@code =0} for kinds that are absent in a given
 * version), so an extractor regression that starts emitting a
 * previously-zero kind fails loudly.
 *
 * <p>Missing corpus directories surface as JUnit skips with diagnostic,
 * per Q8 tiered-skip policy.
 */
class StructuralComparisonTest {

    private static final StructuralBaselines BASELINES = StructuralBaselines.fromDefaultResource();

    @ParameterizedTest(name = "{0}-{1}")
    @ArgumentsSource(StructuralCellsProvider.class)
    void structuralBaselineMatchesExpected(Corpus corpus, Version version, Path versionDir) throws IOException {
        // Assumption: corpus directory must exist on disk. A missing
        // corpus is a legitimate skip (the dev box hasn't cloned that
        // version yet). Per Q8 tiered-skip policy.
        assumeTrue(Files.isDirectory(versionDir),
                corpus.dirName() + "-" + version.display() + " directory missing: " + versionDir);

        // Assertion (not assumption): a discovered (corpus, version) MUST
        // have a baseline row. A silent skip here would mask either a
        // baseline-file regression (entries lost / typo'd) OR a new
        // version added to test-corpus without a corresponding baseline
        // refresh. Either case is a real bug the test should surface.
        Map<String, Integer> expected = BASELINES.forCell(corpus, version);
        assertFalse(expected.isEmpty(),
                "no baseline entry for " + corpus.dirName() + "-" + version.display()
                        + " — regenerate structural-baselines.properties via:\n"
                        + "  mvn test -Dtest=StructuralBaselineDumper -Dstructural.dump=true");

        // A baseline row MUST specify every kind explicitly (including =0
        // for absent ones) — that's the whole point of the "18 kinds per
        // cell" invariant. If a kind were silently dropped from the
        // properties file, the per-kind union below would treat it as an
        // implicit 0 via `expected.getOrDefault(...)` — masking any drift
        // on a previously-=0 kind. Assert key-set equality against
        // ALL_KINDS before comparing values so a dropped entry fails
        // loudly instead of collapsing into the `expected 0, got 0` path.
        Set<String> expectedKinds = new TreeSet<>(expected.keySet());
        Set<String> requiredKinds = new TreeSet<>(StructuralBaselineDumper.ALL_KINDS);
        assertEquals(requiredKinds, expectedKinds,
                "incomplete baseline row for " + corpus.dirName() + "-" + version.display()
                        + " — every cell must pin all " + requiredKinds.size()
                        + " kinds (including =0 for absent ones)."
                        + " Regenerate structural-baselines.properties via:\n"
                        + "  mvn test -Dtest=StructuralBaselineDumper -Dstructural.dump=true");

        Map<String, Integer> actual = extractCounts(corpus, versionDir);

        // Union of kinds in either map — guarantees we assert every
        // baseline entry AND every unexpected kind the extractor emits.
        // A kind present in `expected` but missing from `actual` fails
        // with a clear "expected N, got 0" message; a kind present in
        // `actual` but missing from `expected` fails with "expected 0
        // (implicit), got N", surfacing a new-kind regression.
        List<String> allKinds = new ArrayList<>(expected.keySet());
        for (String k : actual.keySet()) if (!allKinds.contains(k)) allKinds.add(k);
        Collections.sort(allKinds);

        List<String> mismatches = new ArrayList<>();
        for (String kind : allKinds) {
            int want = expected.getOrDefault(kind, 0);
            int got = actual.getOrDefault(kind, 0);
            if (want != got) {
                mismatches.add("  " + kind + ": expected " + want + ", got " + got);
            }
        }

        if (!mismatches.isEmpty()) {
            String cell = corpus.dirName() + "-" + version.display();
            fail("Structural baseline drift in " + cell + ":\n"
                    + String.join("\n", mismatches)
                    + "\n\nIf this reflects a deliberate extractor or corpus change, "
                    + "regenerate the baseline via:\n"
                    + "  mvn test -Dtest=StructuralBaselineDumper -Dstructural.dump=true");
        }
    }

    private static Map<String, Integer> extractCounts(Corpus corpus, Path versionDir) throws IOException {
        Map<String, Integer> counts = new TreeMap<>();
        // Use a pruning walkFileTree rather than Files.walk so we do not
        // descend into VCS metadata (`.git`) or build output (`target`).
        // Under the full 13-cell CI matrix the test-corpus clones carry
        // sizeable `.git` directories; walking them 13 times adds cost
        // that scales with repo metadata rather than source size.
        List<Path> files = new ArrayList<>();
        Files.walkFileTree(versionDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                Path name = dir.getFileName();
                if (name != null) {
                    String n = name.toString();
                    if (n.equals(".git") || n.equals("target") || n.equals("node_modules")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(".rosetta")) {
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        Collections.sort(files);
        assertFalse(files.isEmpty(), "no .rosetta files under " + versionDir);

        for (Path file : files) {
            RosettaParseResult result = RosettaParserFacade.parseFile(file);
            // The chaos cell's 22 expected-refusal files (v3.2 charter § 4b; the
            // committed fork-diagnostics.tsv) must STILL refuse — a clean parse is
            // the refusal healing (LAW 81) — and contribute zero elements. This
            // branch MIRRORS StructuralBaselineDumper.extractCounts exactly: the
            // producer and consumer of the baselines must count the same universe.
            if (corpus == Corpus.CHAOS && ChaosParseExpectations.isExpectedRefusal(file)) {
                assertFalse(result.errors().isEmpty(),
                        "expected-refusal chaos file PARSED CLEAN — the pinned refusal has"
                                + " healed; re-adjudicate " + file + " against "
                                + ChaosParseExpectations.FORK_DIAGNOSTICS_TSV);
                continue;
            }
            assertTrue(result.errors().isEmpty(),
                    "Parse errors in " + file + ":\n" + String.join("\n", result.errors()));

            RosettaStructureExtractor extractor = new RosettaStructureExtractor();
            RosettaStructureExtractor.FileStructure structure =
                    extractor.extract((RosettaParser.RosettaModelContext) result.tree());

            assertNotNull(structure.namespace(), "Missing namespace in " + file);

            for (RosettaStructureExtractor.ElementInfo elem : structure.elements()) {
                counts.merge(elem.kind(), 1, Integer::sum);
            }
        }
        return counts;
    }
}
