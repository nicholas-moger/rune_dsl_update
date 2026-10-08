package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.model.RModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Snapshot test (D5/E10). For each corpus project with a snapshot file,
 * serializes the current-milestone symbol table and compares. Any drift fails loudly.
 *
 * <p>Update: {@code mvn -Dtest=SymbolTableSnapshotTest#update_snapshots -Dupdate.snapshots=true test}
 */
class SymbolTableSnapshotTest {

    private static final Path SNAPSHOT_DIR = Paths.get("src/test/resources/symbols/snapshots");

    private static final Map<String, String> CORPORA = Map.of(
        "cdm", "../test-corpus/cdm",
        "drr", "../test-corpus/drr"
    );

    /**
     * Invariant: each committed snapshot's first line must carry the
     * project's current-milestone marker. Prevents stale-header bit-rot
     * (the M3 serializer literal persisted untouched through M4/M6/D11
     * because the first line was only a comment, not asserted anywhere).
     * When the repo moves to a new milestone (M7c, M8, etc.), bump BOTH
     * {@link SymbolTableSerializer}'s header literal AND this constant.
     */
    private static final String EXPECTED_CURRENT_MILESTONE = "M7b";

    @Test
    void snapshots_match() throws IOException {
        for (Map.Entry<String, String> entry : CORPORA.entrySet()) {
            String name = entry.getKey();
            Path corpusRoot = Paths.get(entry.getValue());
            if (!Files.exists(corpusRoot)) {
                System.out.println("Skipping " + name + " — corpus not present");
                continue;
            }
            Path snapshotPath = SNAPSHOT_DIR.resolve(name + ".symbol-table.txt");
            if (!Files.exists(snapshotPath)) {
                System.out.println("Skipping " + name + " — snapshot not generated yet");
                continue;
            }

            List<RModel> files = SymbolTestCorpus.loadCorpus(corpusRoot);
            RLinkingResult result = RWorkspace.build(files);
            String current = SymbolTableSerializer.serialize(result.workspace());
            String expected = Files.readString(snapshotPath);

            if (!current.equals(expected)) {
                Path actualPath = snapshotPath.resolveSibling(name + ".symbol-table.actual.txt");
                Files.writeString(actualPath, current);
                fail("Symbol table snapshot drift for " + name);
            }
        }
    }

    @Test
    void snapshot_header_matches_expected_milestone() throws IOException {
        for (Map.Entry<String, String> entry : CORPORA.entrySet()) {
            Path snapshotPath = SNAPSHOT_DIR.resolve(entry.getKey() + ".symbol-table.txt");
            if (!Files.exists(snapshotPath)) continue;

            try (Stream<String> lines = Files.lines(snapshotPath)) {
                Optional<String> firstLine = lines.findFirst();
                assertTrue(
                    firstLine.isPresent(),
                    "Snapshot " + snapshotPath + " must not be empty. "
                        + "If the snapshot was truncated or not generated correctly, "
                        + "regenerate it before running this test."
                );
                String header = firstLine.get();
                assertTrue(
                    header.startsWith("# " + EXPECTED_CURRENT_MILESTONE + " "),
                    "Snapshot " + snapshotPath + " must start with '# "
                        + EXPECTED_CURRENT_MILESTONE + " …' — got: " + header
                        + ". If the project moved to a new milestone, bump BOTH "
                        + "SymbolTableSerializer's header literal AND the "
                        + "EXPECTED_CURRENT_MILESTONE constant in this test file."
                );
            }
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "update.snapshots", matches = "true")
    void update_snapshots() throws IOException {
        Files.createDirectories(SNAPSHOT_DIR);
        for (Map.Entry<String, String> entry : CORPORA.entrySet()) {
            Path corpusRoot = Paths.get(entry.getValue());
            if (!Files.exists(corpusRoot)) continue;
            List<RModel> files = SymbolTestCorpus.loadCorpus(corpusRoot);
            RLinkingResult result = RWorkspace.build(files);
            String snapshot = SymbolTableSerializer.serialize(result.workspace());
            Path snapshotPath = SNAPSHOT_DIR.resolve(entry.getKey() + ".symbol-table.txt");
            Files.writeString(snapshotPath, snapshot);
            System.out.println("Updated: " + snapshotPath + " (" + snapshot.length() + " chars)");
        }
    }
}
