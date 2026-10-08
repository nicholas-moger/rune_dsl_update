package com.regnosys.rosetta.harness.matrix;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.opentest4j.TestAbortedException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * JUnit {@link ArgumentsProvider} yielding one argument-tuple per
 * {@code (corpus, version, versionDir)} on-disk combination — not per
 * {@code .rosetta} file.
 *
 * <p>P1.2 audit hook H26b. {@code StructuralComparisonTest} asserts
 * aggregate element-kind counts across a whole corpus-version
 * directory, so parametrising by {@link MatrixCellsProvider} (one cell
 * per file) would over-fire the test by three orders of magnitude.
 *
 * <h2>Discovery strategy</h2>
 * Enumerates version directories directly: for each {@link Corpus}
 * that has a subdirectory under {@code root}, lists
 * {@code <corpus>/<corpus>-<version>/} entries whose names match the
 * expected prefix and whose version segment parses via
 * {@link Version#parse(String)}. One tuple per version directory,
 * regardless of how many {@code .rosetta} files live inside — or
 * whether any do.
 *
 * <p>A direct-enumeration strategy (rather than rolling the discovery
 * up from {@link MatrixCellsProvider#discover(Path)}) is deliberate:
 * it means a version directory that exists but is empty of
 * {@code .rosetta} files still yields a tuple, so
 * {@code StructuralComparisonTest}'s own "no .rosetta files under ..."
 * assertion surfaces the empty cell as a loud failure rather than a
 * silent omission. Layout-parsing discipline (no regex, literal
 * {@link String#startsWith(String)} + single {@link String#substring(int)})
 * mirrors {@link MatrixCellsProvider}'s own corpus-layout walker.
 *
 * <p>Missing or empty corpus tree is treated as a JUnit skip with a
 * {@link MatrixCellsProvider#missingCorporaDiagnostic(Path)} diagnostic
 * — consistent with {@link MatrixCellsProvider}'s Q8 tiered-skip
 * behaviour.
 */
public final class StructuralCellsProvider implements ArgumentsProvider {

    private final Path root;

    public StructuralCellsProvider() {
        this(MatrixCellsProvider.DEFAULT_ROOT);
    }

    /** Visible for tests — inject a hermetic tmp root. */
    StructuralCellsProvider(Path root) {
        this.root = Objects.requireNonNull(root, "root");
    }

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        // LinkedHashMap preserves insertion order across iteration — combined
        // with the explicit lexicographic sort inside collectVersionDirs (which
        // overrides Files.list's non-deterministic traversal order), this
        // yields reproducible parameterised test ordering across runs, OSes,
        // and filesystems.
        Map<Key, Path> cells = new LinkedHashMap<>();
        if (Files.isDirectory(root)) {
            for (Corpus corpus : Corpus.values()) {
                Path corpusDir = root.resolve(corpus.dirName());
                if (!Files.isDirectory(corpusDir)) continue;
                collectVersionDirs(corpus, corpusDir, cells);
            }
        }

        if (cells.isEmpty()) {
            String diag = MatrixCellsProvider.missingCorporaDiagnostic(root);
            String reason = diag.isEmpty()
                    ? "no corpus-version directories discovered under " + root
                    : diag;
            throw new TestAbortedException(reason);
        }

        return cells.entrySet().stream()
                .map(e -> Arguments.of(e.getKey().corpus(), e.getKey().version(), e.getValue()));
    }

    private static void collectVersionDirs(Corpus corpus, Path corpusDir, Map<Key, Path> out) {
        String prefix = corpus.dirName() + "-";
        try (Stream<Path> children = Files.list(corpusDir)) {
            // Explicit lexicographic sort by directory name — Files.list
            // traversal order is filesystem-dependent and not guaranteed
            // reproducible across OSes. Sorting here makes the parameter
            // order stable so CI logs and test report ordering are
            // comparable across machines.
            children.filter(Files::isDirectory)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .forEach(verDir -> {
                        String dirName = verDir.getFileName().toString();
                        if (!dirName.startsWith(prefix)) return;
                        // PR #185: skip transitive-DEPENDENCY cells (rune-fpml-1.5.3) — a
                        // dependency closure loaded for another cell's resolution, never a
                        // CATALOGUE cell with its own structural baseline (still manifest-pinned).
                        if (com.regnosys.rosetta.testutil.CorpusWalker.TRANSITIVE_DEP_CELL_DIRS.contains(dirName)) return;
                        String rawVersion = dirName.substring(prefix.length());
                        Version version;
                        try {
                            version = Version.parse(rawVersion);
                        } catch (IllegalArgumentException e) {
                            // Directory name doesn't parse as a version — skip
                            // silently. Matches MatrixCellsProvider's own policy
                            // for unparseable version dirs.
                            return;
                        }
                        out.putIfAbsent(new Key(corpus, version), verDir);
                    });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private record Key(Corpus corpus, Version version) {}
}
