package com.regnosys.rosetta.ir.core;

/**
 * Typed key into a {@link Metadata} bag. Permits typed lookup via
 * {@link Metadata#getAs(MetadataKey)} without unsafe casts at call sites.
 *
 * <p>Implementations should be effectively-immutable (record-style or
 * {@code public static final} singleton constants).
 *
 * @param <T> the value type associated with this key
 */
public interface MetadataKey<T> {
    /** Stable key name. Used as the lookup key and for diagnostics. */
    String name();

    /** Value type. Used by {@link Metadata#getAs(MetadataKey)} for type-safe extraction. */
    Class<T> type();
}
