package com.regnosys.rosetta.testutil;

import java.nio.file.Path;
import java.util.List;

/**
 * THE ONE READ of the chaos cell's location for the generator module's tests (v3.2 seat 10,
 * PR #631 — D49, the version cut {@code chaos-1.0.0 → chaos-1.1.0}).
 *
 * <p>Until this seat seven generator seat suites each carried a literal
 * {@code Path.of("../test-corpus/chaos/chaos-1.0.0")} — seven copies of one fact the corpus SOT
 * ({@code test-corpus/corpus-cells.tsv}) already states, exactly the drift class that file was
 * written to end (its own header: seven hand-maintained copies of the cell list). The charter's
 * § 9 law is that versions REPLACE, one chaos row at a time; this helper derives the cell
 * directory from that row through the derived-path law ({@code <corpus>/<corpus>-<version>}),
 * so the NEXT version cut is a TSV edit and nothing else in this module.
 *
 * <p>Exactly ONE active catalogue chaos row is the law (charter § 9: coexistence would pay chain
 * cost twice); zero or two refuse loudly rather than pick one.
 */
public final class ChaosCell {

    private static final Path TEST_CORPUS = Path.of("..", "test-corpus");

    private ChaosCell() {
        // utility class — no instances
    }

    /** The active catalogue chaos row of the corpus SOT. */
    public static CorpusCells.Row row() {
        return rowOf(CorpusCells.activeCatalogue(TEST_CORPUS));
    }

    /**
     * The pure half of {@link #row()} — the one-row law over a given catalogue (the seat suite's
     * seam, {@code ChaosCellSeatTest} b1–b3: zero or two chaos rows refuse, one is returned).
     */
    public static CorpusCells.Row rowOf(List<CorpusCells.Row> catalogue) {
        List<CorpusCells.Row> chaos = catalogue.stream()
                .filter(r -> "chaos".equals(r.corpus()))
                .toList();
        if (chaos.size() != 1) {
            throw new IllegalStateException("exactly one active catalogue chaos row is the law"
                    + " (charter § 9: versions REPLACE); found " + chaos.size() + " in "
                    + TEST_CORPUS.resolve("corpus-cells.tsv").toAbsolutePath().normalize());
        }
        return chaos.get(0);
    }

    /** {@code ../test-corpus/chaos/chaos-<version>} — the cell directory, derived. */
    public static Path root() {
        return TEST_CORPUS.resolve(row().relPath());
    }

    /** The cell's pinned goldens: {@code <root>/rosetta-source/src/generated/java}. */
    public static Path goldens() {
        return root().resolve("rosetta-source").resolve("src").resolve("generated").resolve("java");
    }

    /** The cell's sources: {@code <root>/rosetta-source/src/main/rosetta}. */
    public static Path sources() {
        return root().resolve("rosetta-source").resolve("src").resolve("main").resolve("rosetta");
    }

    /** The D11 / declared-set key prefix, {@code chaos/<version>} (corpus slash version). */
    public static String label() {
        CorpusCells.Row r = row();
        return r.corpus() + "/" + r.version();
    }
}
