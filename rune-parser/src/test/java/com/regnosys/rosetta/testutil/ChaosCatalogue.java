package com.regnosys.rosetta.testutil;

import java.nio.file.Path;
import java.util.List;

/**
 * THE ONE READ of the chaos cell's location for the parser module's tests (v3.2 seat 10,
 * PR #631 — D49, the version cut {@code chaos-1.0.0 → chaos-1.1.0}); the generator module's
 * twin is {@code com.regnosys.rosetta.testutil.ChaosCell} (the modules share no test-jar — one
 * shared DATA file, {@code test-corpus/corpus-cells.tsv}, two readers of it, never two copies of
 * the fact).
 *
 * <p>Until this seat four parser tests each carried a literal
 * {@code test-corpus/chaos/chaos-1.0.0} path. The charter's § 9 law is that versions REPLACE,
 * one chaos row at a time; this helper derives the cell from that row, so the next cut is a TSV
 * edit. Exactly ONE active catalogue chaos row is the law — zero or two refuse loudly.
 */
public final class ChaosCatalogue {

    private ChaosCatalogue() {
        // utility class — no instances
    }

    /** The active catalogue chaos cell of the corpus SOT. */
    public static CorpusCatalogue.Cell cell() {
        return cellOf(CorpusCatalogue.activeCatalogue());
    }

    /**
     * The pure half of {@link #cell()} — the one-row law over a given catalogue (the seat suite's
     * seam, {@code ChaosBandSeatTest} a5–a7: zero or two chaos rows refuse, one is returned).
     */
    public static CorpusCatalogue.Cell cellOf(List<CorpusCatalogue.Cell> catalogue) {
        List<CorpusCatalogue.Cell> chaos = catalogue.stream()
                .filter(c -> "chaos".equals(c.corpus()))
                .toList();
        if (chaos.size() != 1) {
            throw new IllegalStateException("exactly one active catalogue chaos row is the law"
                    + " (charter § 9: versions REPLACE); found " + chaos.size() + " in "
                    + CorpusCatalogue.tsvPath());
        }
        return chaos.get(0);
    }

    /** {@code <repo>/test-corpus/chaos/chaos-<version>} — derived, never restated. */
    public static Path root() {
        return CorpusCatalogue.cellDir(cell());
    }

    /** The cell's sources: {@code <root>/rosetta-source/src/main/rosetta}. */
    public static Path sources() {
        return root().resolve("rosetta-source").resolve("src").resolve("main").resolve("rosetta");
    }
}
