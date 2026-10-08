package com.regnosys.rosetta.generator.java;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * v3.1 CLOSE-OUT (the close census's standing list (d), "corpus-only drr7Available
 * gates") — the ONE verdict behind every {@code @EnabledIf("drr7Available")} corpus
 * lock in this tree.
 *
 * <p>Forty seat suites lock whole drr 7.x golden files against the fork's render, and
 * every one of them is corpus-gated: JUnit SKIPS the lock when the local
 * {@code test-corpus/drr/drr-7.0.0} cell is absent. That is the right behaviour on a
 * fresh clone and in CI (the corpus is local-only, never committed) and the wrong
 * behaviour in the LOCAL gating chain, where a green run that had quietly skipped 168
 * whole-file locks would be counted as a measurement. Before this class each suite
 * carried its own copy of the predicate — forty copies in four spellings — and every
 * one of them skipped in silence.
 *
 * <p>Two things change, and only two. The verdict is REPORTED: the first absent
 * verdict in a JVM prints one {@code [DRR7-CORPUS ABSENT]} line naming the suite and
 * the property below. And it can be made FATAL: with {@code -Dcorpus.required=true} an
 * absent corpus throws instead of returning {@code false}, so the {@code @EnabledIf}
 * condition ERRORS the test instead of skipping it — the local receipts chain passes
 * that property, so a local green can never again rest on a skipped lock; CI and fresh
 * clones do not set it and keep the skip. Each suite's OWN predicate expression (which
 * directories it checks) is unchanged: this class wraps the verdict, it does not
 * re-derive it — so a false verdict may also mean one of the suite's OTHER required
 * directories (six predicates also test the rune-dsl builtins) is absent, and the
 * report says so. {@link Drr7CorpusGateTest} is the positive control for both modes.
 *
 * <p>"Once per JVM" is literal: surefire forks this module with {@code forkCount=6},
 * so a default run may print the report up to six times, one per fork.
 */
final class Drr7Corpus {

    /** {@code -Dcorpus.required=true} turns an absent-corpus SKIP into a FAILURE. */
    static final String REQUIRED_PROPERTY = "corpus.required";

    private static final String TAG = "[DRR7-CORPUS ABSENT]";
    private static final AtomicBoolean REPORTED = new AtomicBoolean();

    private Drr7Corpus() {
    }

    /** Test-only: re-arms the once-per-JVM report so the positive control does not consume it. */
    static void resetReportForTest() {
        REPORTED.set(false);
    }

    /**
     * The gate every {@code drr7Available()} predicate delegates to.
     *
     * @param present the suite's own on-disk verdict (its constants, its directories)
     * @param suite   the calling suite, named in the report line
     * @return {@code present} — unchanged when true; when false, after the one-line
     *         report, unless {@link #REQUIRED_PROPERTY} is set, in which case an
     *         {@link AssertionError} is thrown so the condition errors loudly
     */
    static boolean gate(boolean present, Class<?> suite) {
        if (present) {
            return true;
        }
        String where = suite == null ? "?" : suite.getSimpleName();
        if (Boolean.getBoolean(REQUIRED_PROPERTY)) {
            throw new AssertionError(TAG + " " + where + ": its drr7Available() predicate is false"
                    + " (the drr 7.x corpus cell and/or this suite's other required directories are"
                    + " not on disk) and -D" + REQUIRED_PROPERTY + "=true forbids skipping its"
                    + " whole-file locks (a local gating run may never pass by skipping)");
        }
        if (REPORTED.compareAndSet(false, true)) {
            System.err.println(TAG + " " + where + ": its drr7Available() predicate is false (the"
                    + " drr 7.x corpus cell and/or this suite's other required directories are not"
                    + " on disk); it and every other drr7Available-gated suite in this JVM SKIPS"
                    + " its corpus locks; pass -D" + REQUIRED_PROPERTY + "=true to fail instead");
        }
        return false;
    }
}
