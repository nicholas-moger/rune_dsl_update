package com.regnosys.rosetta.harness.matrix;

/**
 * One axis of the scope matrix: which corpus a {@code .rosetta} input comes from.
 *
 * <p>Part of the P1.2 test-harness infrastructure (audit hook H13). Every matrix
 * test carries a {@link MatrixCoordinate} that names its {@code Corpus}, so
 * failure reports can group by corpus and so coverage claims qualify against
 * the full scope matrix (per project scope-honesty rule — no silent CDM-only
 * defaults).
 *
 * <p>The directory name each enum value expects under {@code test-corpus/} is
 * {@link #dirName()}.
 */
public enum Corpus {

    CDM("cdm"),
    DRR("drr"),
    ISO20022("iso20022"),
    RUNE_FPML("rune-fpml"),
    /** The AUTHORED adversarial cell (the v3.2 chaos-gate charter) — committed in-repo, never cloned. */
    CHAOS("chaos");

    private final String dirName;

    Corpus(String dirName) {
        this.dirName = dirName;
    }

    /** Directory name under {@code test-corpus/} containing this corpus's clones. */
    public String dirName() {
        return dirName;
    }

    /**
     * Resolve a corpus from a directory name, or {@code null} if not recognised.
     * Uses directory-name matching only — no regex, no string munging beyond
     * a direct equality check against {@link #dirName()}.
     */
    public static Corpus fromDirName(String name) {
        for (Corpus c : values()) {
            if (c.dirName.equals(name)) return c;
        }
        return null;
    }
}
