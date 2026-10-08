package com.regnosys.rosetta.types;

import com.regnosys.rosetta.ast.types.REnumeration;
import java.util.Objects;

/**
 * Type model reference to a user-defined enumeration.
 */
public final class REnumTypeRef implements RType {
    private final REnumeration astNode;

    public REnumTypeRef(REnumeration astNode) {
        this.astNode = Objects.requireNonNull(astNode);
    }

    @Override public String name() { return astNode.name(); }
    @Override public boolean hasNaturalOrder() { return false; }
    public REnumeration astNode() { return astNode; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof REnumTypeRef that)) return false;
        return astNode == that.astNode;
    }
    @Override public int hashCode() { return System.identityHashCode(astNode); }
    @Override public String toString() { return name(); }
}
