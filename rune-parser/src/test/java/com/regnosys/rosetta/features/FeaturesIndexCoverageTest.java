package com.regnosys.rosetta.features;

import com.regnosys.rosetta.testutil.CorpusWalker;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Locks {@code docs/features/INDEX.md} as a complete, single-occurrence
 * navigator across {@code docs/upgrades/U*.md} manifests. Every U-NNN file
 * MUST be referenced from INDEX.md exactly once (per spec § 8 fix I2).
 *
 * <p>Catches two failure modes:
 * <ul>
 *   <li>A new U-NNN entry lands without a matching INDEX.md row.
 *   <li>A row gets duplicated under two topics (which would suggest a
 *       topic-classification ambiguity that needs resolving, not silently
 *       double-listing).
 * </ul>
 *
 * <p>Path resolution via {@link CorpusWalker#repoRoot()} for JVM-cwd
 * independence (R2/R3 hardening pattern).
 */
class FeaturesIndexCoverageTest {

    @Test
    void everyUpgradeManifestEntryAppearsUnderExactlyOneIndexTopic() throws IOException {
        Path upgradesDir = CorpusWalker.repoRoot().resolve("docs/upgrades");
        Path index = CorpusWalker.repoRoot().resolve("docs/features/INDEX.md");

        Set<String> manifestFiles;
        try (Stream<Path> files = Files.list(upgradesDir)) {
            manifestFiles = files
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.matches("U\\d{3}-.*\\.md"))
                    .collect(Collectors.toCollection(TreeSet::new));
        }

        String indexContent = Files.readString(index);

        Set<String> mismatched = new TreeSet<>();
        for (String name : manifestFiles) {
            int hits = countOccurrences(indexContent, name);
            if (hits != 1) {
                mismatched.add(name + " (" + hits + " occurrences in INDEX.md)");
            }
        }
        assertEquals(Set.of(), mismatched,
                "Every U-NNN manifest entry must appear exactly once in " +
                        "docs/features/INDEX.md. Missing-or-duplicate entries: " +
                        mismatched);
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}
