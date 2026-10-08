package com.regnosys.rosetta.ir.core;

import java.util.Optional;
import java.util.Set;

/**
 * Typed metadata bag attached to an {@link IRNode}. Replaces the untyped
 * {@code Map<String, Object>} originally proposed (per spec Section 2
 * reviewer fix C1).
 *
 * <p>Semantics:
 * <ul>
 *   <li>{@link #get(String)} performs an untyped lookup; callers cast or use
 *       {@code instanceof} checks.
 *   <li>{@link #getAs(MetadataKey)} performs a typed lookup; only returns a
 *       present value when the stored value is an instance of
 *       {@link MetadataKey#type()} (i.e. {@code key.type().isInstance(value)}).
 *   <li>{@link #keys()} returns all known keys (immutable view).
 * </ul>
 *
 * <p>Implementations MUST treat the underlying storage as immutable —
 * {@link IRNode} is frozen post-construction (per H6).
 *
 * <p>This interface MAY grow via additive default-method extension (per W31
 * forward path — additional structured accessors, e.g.
 * {@code getInt(String)}). Removal or signature change requires a D-entry.
 */
public interface Metadata {
    /** Untyped key lookup. Returns empty if no such key. */
    Optional<Object> get(String key);

    /** All known keys. Immutable view. */
    Set<String> keys();

    /**
     * Typed key lookup. Returns empty if no such key OR if the stored
     * value is not an instance of {@code key.type()} (equivalently,
     * if {@code key.type().isInstance(value)} is {@code false}).
     */
    <T> Optional<T> getAs(MetadataKey<T> key);
}
