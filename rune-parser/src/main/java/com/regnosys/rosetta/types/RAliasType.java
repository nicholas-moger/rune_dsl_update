package com.regnosys.rosetta.types;

import java.util.Map;
import java.util.Objects;

/**
 * Parametric type alias. Wraps an underlying {@link RType} with named
 * parameter arguments. Transparent for subtype checking (alias is unwrapped).
 */
public final class RAliasType implements RType {
    private final String name;
    private final Map<String, Object> arguments;
    private final RType refersTo;

    public RAliasType(String name, Map<String, Object> arguments, RType refersTo) {
        this.name = Objects.requireNonNull(name);
        this.arguments = Map.copyOf(arguments);
        this.refersTo = Objects.requireNonNull(refersTo);
    }

    @Override public String name() { return name; }
    @Override public boolean hasNaturalOrder() { return refersTo.hasNaturalOrder(); }
    public Map<String, Object> arguments() { return arguments; }
    public RType refersTo() { return refersTo; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RAliasType that)) return false;
        return name.equals(that.name)
            && arguments.equals(that.arguments)
            && refersTo.equals(that.refersTo);
    }
    @Override public int hashCode() { return Objects.hash(name, arguments, refersTo); }
    @Override public String toString() { return name + "(" + arguments + ") -> " + refersTo; }
}
