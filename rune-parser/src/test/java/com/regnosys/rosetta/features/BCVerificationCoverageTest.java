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
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Locks {@code docs/bc-verification.md} {@code ### Per-feature locks} as a
 * complete, single-occurrence pairing across {@code docs/upgrades/U*.md}
 * manifests. Every U-NNN file MUST appear exactly once as a bullet under the
 * {@code ### Per-feature locks} subheading.
 *
 * <p>This is the third leg of the documentation triplet (rule 1 of the project
 * SDLC rules in CLAUDE.md): manifest -> INDEX.md (locked by
 * {@link FeaturesIndexCoverageTest}) -> bc-verification.md (locked here).
 *
 * <p>Anchor design: matches {@code **U001 } (bold + space) to scope hits to
 * bullet entries. The {@code ### Per-feature locks} subheading + next
 * {@code \n## } parent boundary scope the haystack to that one section
 * (other sections cross-reference U-NNNs in narrative role and must NOT
 * be counted).
 *
 * <p>Path resolution via {@link CorpusWalker#repoRoot()} for JVM-cwd
 * independence.
 */
class BCVerificationCoverageTest {

    @Test
    void everyUpgradeManifestEntryAppearsUnderExactlyOnePerFeatureLock() throws IOException {
        Path upgradesDir = CorpusWalker.repoRoot().resolve("docs/upgrades");
        Path bcDoc       = CorpusWalker.repoRoot().resolve("docs/bc-verification.md");

        Set<String> manifestFiles;
        try (Stream<Path> files = Files.list(upgradesDir)) {
            manifestFiles = files
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.matches("U\\d{3}-.*\\.md"))
                    .collect(Collectors.toCollection(TreeSet::new));
        }

        String bcContent = Files.readString(bcDoc);

        // Scope the haystack to the Per-feature-locks subsection only.
        // Other sections of the doc cross-reference U-NNNs in narrative role
        // (Layer N prose, cross-references) and must NOT be counted.
        int sectionStart = bcContent.indexOf("### Per-feature locks");
        if (sectionStart < 0) {
            fail("docs/bc-verification.md must contain a " +
                    "'### Per-feature locks' subheading; the BC pairing test " +
                    "depends on this section anchor.");
        }
        int sectionEnd = bcContent.indexOf("\n## ", sectionStart);
        if (sectionEnd < 0) sectionEnd = bcContent.length();
        String section = bcContent.substring(sectionStart, sectionEnd);

        // Anchor on bold prefix + space (matches "**U001 " regardless of what
        // character follows — em-dash, hyphen, period, etc.). Trailing space is
        // the bullet-pattern signature; plain "U001" would false-match
        // cross-references.
        Set<String> mismatched = new TreeSet<>();
        for (String name : manifestFiles) {
            String prefix = name.substring(0, 4); // first 4 chars = "U" + 3 digits, guaranteed by the filter regex above
            int hits = countOccurrences(section, "**" + prefix + " ");
            if (hits != 1) {
                mismatched.add(prefix + " (" + hits + " bullets in Per-feature locks)");
            }
        }
        assertEquals(Set.of(), mismatched,
                "Every U-NNN manifest entry must appear exactly once as a bullet in " +
                        "docs/bc-verification.md '### Per-feature locks'. " +
                        "Missing-or-duplicate entries: " + mismatched);
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
