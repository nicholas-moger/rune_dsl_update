package com.regnosys.rosetta.types;

/**
 * Zero-parameter built-in type: boolean, time, pattern, nothing (bottom), any (top).
 */
public final class RBasicType implements RType {
    public static final RBasicType BOOLEAN = new RBasicType("boolean", true);
    public static final RBasicType TIME = new RBasicType("time", true);
    public static final RBasicType PATTERN = new RBasicType("pattern", false);
    public static final RBasicType NOTHING = new RBasicType("nothing", true);
    public static final RBasicType ANY = new RBasicType("any", false);

    private final String name;
    private final boolean naturalOrder;

    private RBasicType(String name, boolean naturalOrder) {
        this.name = name;
        this.naturalOrder = naturalOrder;
    }

    @Override public String name() { return name; }
    @Override public boolean hasNaturalOrder() { return naturalOrder; }
    @Override public boolean equals(Object o) { return this == o; }
    @Override public int hashCode() { return System.identityHashCode(this); }
    @Override public String toString() { return name; }
}
