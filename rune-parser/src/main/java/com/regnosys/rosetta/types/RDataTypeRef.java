package com.regnosys.rosetta.types;

import com.regnosys.rosetta.ast.types.RDataType;
import java.util.Objects;

/**
 * Type model reference to a user-defined data type. Holds a reference to
 * the M2 AST node (D5 — references, not wrappers).
 */
public final class RDataTypeRef implements RType {
    private final RDataType astNode;

    public RDataTypeRef(RDataType astNode) {
        this.astNode = Objects.requireNonNull(astNode);
    }

    @Override public String name() { return astNode.name(); }
    @Override public boolean hasNaturalOrder() { return false; }
    public RDataType astNode() { return astNode; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RDataTypeRef that)) return false;
        return astNode == that.astNode; // identity comparison on AST node
    }
    @Override public int hashCode() { return System.identityHashCode(astNode); }
    @Override public String toString() { return name(); }
}
