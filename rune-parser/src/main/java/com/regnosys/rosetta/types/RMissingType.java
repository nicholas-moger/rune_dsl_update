package com.regnosys.rosetta.types;

/**
 * Sentinel type for inference failures. Replaces null throughout the
 * type system (D4). Singleton.
 *
 * <p>Xtext equivalent: {@code NOTHING_WITH_ANY_META}.
 */
public final class RMissingType implements RType {
    public static final RMissingType INSTANCE = new RMissingType();
    private RMissingType() {}

    @Override public String name() { return "MISSING"; }
    @Override public boolean hasNaturalOrder() { return false; }
    @Override public String toString() { return "MISSING"; }
}
