package com.regnosys.rosetta.ir.adapter;

import com.regnosys.rosetta.ir.core.Metadata;
import com.regnosys.rosetta.ir.core.MetadataKey;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Immutable {@link Metadata} implementation backed by an unmodifiable map.
 *
 * <p>Satisfies the {@code MetadataContractTest} invariants: never null, empty
 * lookups on a miss, type-checked {@link #getAs(MetadataKey)}, and an immutable
 * {@link #keys()} view.
 *
 * <p>Lab-authored Phase-1 IR adapter (decision L-004). The declaration-surface
 * IR carries no metadata in Phase 1, so {@link #EMPTY} is the common instance;
 * the map-backed constructor exists so later phases can attach typed metadata
 * without changing the node records.
 */
public final class IRMetadata implements Metadata {

    /** Shared empty metadata bag. */
    public static final IRMetadata EMPTY = new IRMetadata(Map.of());

    private final Map<String, Object> values;

    public IRMetadata(Map<String, Object> values) {
        this.values = Map.copyOf(values);
    }

    @Override
    public Optional<Object> get(String key) {
        return Optional.ofNullable(values.get(key));
    }

    @Override
    public Set<String> keys() {
        // Map.copyOf yields an unmodifiable map, so keySet() is an unmodifiable view.
        return values.keySet();
    }

    @Override
    public <T> Optional<T> getAs(MetadataKey<T> key) {
        Object value = values.get(key.name());
        if (value != null && key.type().isInstance(value)) {
            return Optional.of(key.type().cast(value));
        }
        return Optional.empty();
    }

    /**
     * A deterministic, identity-free rendering — {@code IRMetadata[]} for the empty bag, or
     * {@code IRMetadata[a, b]} with the keys sorted. This overrides {@code Object.toString} (which would emit a
     * non-deterministic identity hash like {@code IRMetadata@1a2b3c}) so any reflective dump of a node holding
     * metadata — e.g. the printer's unmodelled-node fallback — stays byte-stable across runs. Only the sorted
     * key set is rendered, never the values, because a value may be an arbitrary object whose own
     * {@code toString} is itself non-deterministic.
     */
    @Override
    public String toString() {
        return "IRMetadata" + new TreeSet<>(values.keySet());
    }
}
