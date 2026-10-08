package com.regnosys.rosetta.harness.matrix;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;

/**
 * A single cell of the test scope matrix: a {@code .rosetta} input file's
 * position in (corpus × project × version × element kind).
 *
 * <p>Every P1.2 matrix-aware test carries exactly one {@code MatrixCoordinate}.
 * Display names, failure grouping, cache keys, and skip diagnostics all derive
 * from the coordinate. No string-path munging or regex — the coordinate is the
 * authoritative handle.
 *
 * <p>Part of P1.2 test-harness infrastructure (audit hook H13). Used by
 * {@code MatrixCellsProvider} (H14), the matrix-aware reporter (H14), the
 * content-addressable cache extension (H24), and the fork-per-cell wiring (H16).
 *
 * @param corpus  which corpus this cell belongs to
 * @param project sub-module / project name within the corpus (e.g. {@code "base-model"},
 *                {@code "product-model"}); empty-string if the corpus has no
 *                sub-module structure (single-project corpora)
 * @param version which version of the corpus this cell is drawn from
 * @param kind    element kind the cell exercises; {@link ElementKind#UNKNOWN} if
 *                discovery could not classify the input
 * @param source  path to the {@code .rosetta} source file for this cell. May
 *                be absolute (production discovery typically resolves
 *                {@code test-corpus/...} to an absolute path) or relative
 *                (convenient for unit tests). The type does not enforce either
 *                — downstream consumers that need an absolute path should call
 *                {@link java.nio.file.Path#toAbsolutePath()} explicitly.
 */
public record MatrixCoordinate(
        Corpus corpus,
        String project,
        Version version,
        ElementKind kind,
        Path source) implements Comparable<MatrixCoordinate> {

    /**
     * Canonical ordering by {@code (corpus, project, version, kind, source)}.
     *
     * <p>Source path is compared as its forward-slash form so iteration
     * order — and therefore NDJSON record order and parameterized-test
     * order — is identical on Windows and Linux. Without the
     * normalisation, a {@code \}-separated path on Windows would sort
     * differently from the same logical path on CI.
     */
    private static final Comparator<MatrixCoordinate> NATURAL =
            Comparator.comparing(MatrixCoordinate::corpus)
                    .thenComparing(MatrixCoordinate::project)
                    .thenComparing(MatrixCoordinate::version)
                    .thenComparing(MatrixCoordinate::kind)
                    .thenComparing(mc -> mc.source.toString().replace('\\', '/'));

    public MatrixCoordinate {
        Objects.requireNonNull(corpus, "corpus");
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(source, "source");
    }

    /**
     * Human-readable name used in JUnit {@code @DisplayName}-style reports.
     * Format: {@code corpus/project/version/kind:filename}.
     *
     * <p>Deterministic — safe to use as part of a stable cache key or deduplication
     * handle.
     */
    public String displayName() {
        String file = source.getFileName() == null ? "<unnamed>" : source.getFileName().toString();
        String proj = project.isEmpty() ? "-" : project;
        return corpus.dirName() + "/" + proj + "/" + version.display() + "/" + kind + ":" + file;
    }

    @Override
    public int compareTo(MatrixCoordinate o) {
        return NATURAL.compare(this, o);
    }

    /**
     * Returns {@link #displayName()} so JUnit's
     * {@code @ParameterizedTest(name = "{0}")} renders cells as the
     * slash-separated display form rather than the record's canonical
     * {@code MatrixCoordinate[...]} representation. Downstream diagnostics
     * (NDJSON messages, parameterized-test failure lines) stay stable.
     */
    @Override
    public String toString() {
        return displayName();
    }
}
