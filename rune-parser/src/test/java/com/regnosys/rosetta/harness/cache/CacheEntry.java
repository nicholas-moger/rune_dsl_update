package com.regnosys.rosetta.harness.cache;

import java.util.Objects;

/**
 * A cached test-result record keyed by a {@link CacheKey}.
 *
 * <p>P1.2 audit hook H24 foundation. Mirrors the {@code Reporter.Result}
 * shape so a cache-hit path can replay the NDJSON record without re-running
 * the test:
 * <ul>
 *   <li>{@link Outcome} — pass/fail/skip
 *   <li>{@code durationMs} — wall-clock of the original run
 *   <li>{@code message} — null for PASSED; non-null for FAILED/SKIPPED
 * </ul>
 *
 * <p>On-disk serialisation is a single NDJSON line per entry (see
 * {@link LocalDiskCache}) so the cache directory is inspectable by
 * standard tools.
 */
public record CacheEntry(CacheKey key, Outcome outcome, long durationMs, String message) {

    public enum Outcome { PASSED, FAILED, SKIPPED }

    public CacheEntry {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(outcome, "outcome");
        if (durationMs < 0) {
            throw new IllegalArgumentException("durationMs must be >= 0, got " + durationMs);
        }
        if (outcome == Outcome.PASSED && message != null) {
            throw new IllegalArgumentException("PASSED must have null message");
        }
        if (outcome != Outcome.PASSED && message == null) {
            throw new IllegalArgumentException(outcome + " must have non-null message");
        }
    }

    public static CacheEntry passed(CacheKey key, long durationMs) {
        return new CacheEntry(key, Outcome.PASSED, durationMs, null);
    }

    public static CacheEntry failed(CacheKey key, long durationMs, String message) {
        return new CacheEntry(key, Outcome.FAILED, durationMs, message);
    }

    public static CacheEntry skipped(CacheKey key, long durationMs, String reason) {
        return new CacheEntry(key, Outcome.SKIPPED, durationMs, reason);
    }
}
