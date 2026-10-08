package com.regnosys.rosetta.testutil;

import com.regnosys.rosetta.generator.GenerationException;

import java.util.List;

/**
 * v3.1 CLOSE-OUT (the close census's standing list (c), "the generateWithErrors discard
 * sweep") — the assertion every test-side generator call now wraps.
 *
 * <p>{@code FunctionGenerator#generateWithErrors} and every
 * {@code JavaClassGenerator#generateClasses} RETURN their per-element failures as a
 * {@code List<GenerationException>} instead of throwing: the production boundary
 * ({@code JavaClassGenerator#generateClasses}, the refusal contract's delivery point)
 * collects them and reports them as generation errors. A test that calls the generator
 * as a bare statement throws that list away, and with it every refusal the C0 refusal
 * contract exists to make loud: the test then fails LATER on a missing or truncated
 * rendering — or not at all, when it asserts on a fragment the error never touched.
 * The 2026-08-22 close census counted the discarding sites; the live census at the
 * close-out found 694 across 151 suites (656 {@code generateClasses}, 38
 * {@code generateWithErrors}), every one rewritten to pass through here.
 *
 * <p>The assertion is deliberately the strictest reading: ANY reported error fails the
 * test that generated it, naming the target path and message of each. A suite that
 * genuinely expects errors captures the returned list itself and asserts on it — that
 * shape never reached this sweep, because it was never a discarded statement.
 */
public final class GenerationErrors {

    private GenerationErrors() {
    }

    /**
     * Fails when {@code errors} is non-empty; a {@code null} list (a generator with
     * nothing to report) passes. Returns nothing, so a call site reads as the statement
     * it replaced.
     */
    public static void assertNoGenerationErrors(List<GenerationException> errors) {
        if (errors == null || errors.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder("the generator reported ").append(errors.size())
                .append(" generation error(s) at a call site whose error list was discarded before")
                .append(" the v3.1 close-out sweep (a refusal here used to vanish; the test would fail")
                .append(" later on the missing output, or not at all):");
        for (GenerationException e : errors) {
            // toString() carries resourceUri + context + message; the target path may be
            // null when the failure preceded the type representation (see GenerationException).
            sb.append("\n  ").append(e.getTargetPath()).append(" — ").append(e);
        }
        AssertionError failure = new AssertionError(sb.toString());
        failure.initCause(errors.get(0));
        throw failure;
    }
}
