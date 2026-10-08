package com.regnosys.rosetta.generator.java.types;

import java.util.Objects;

import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.generator.java.enums.EnumHelper;

/**
 * Represents a single value in a generated Java enum. Holds the
 * Java-ified name, the original AST node, and optional parent value
 * (from enum inheritance).
 */
public class RJavaEnumValue {
    private final RJavaEnum enumeration;
    private final String name;
    private final REnumValue astNode;
    private final RJavaEnumValue parentValue;

    public RJavaEnumValue(RJavaEnum enumeration, String name, REnumValue astNode,
                          RJavaEnumValue parentValue) {
        this.enumeration = enumeration;
        this.name = name;
        this.astNode = astNode;
        this.parentValue = parentValue;
    }

    public RJavaEnum getEnumeration() {
        return enumeration;
    }

    /** The AST node for this enum value. */
    public REnumValue getAstNode() {
        return astNode;
    }

    /** The Java-ified constant name (e.g., "ACTIVE"). */
    public String getName() {
        return name;
    }

    /**
     * The original Rosetta name with the parser-escape marker stripped
     * (e.g., {@code "Active"}; for source {@code ^E}, returns {@code "E"}).
     * Strip applied per P2.1.1 T3 Cluster A pilot sub-cause A.3 — the
     * leading caret ({@code ^}) is a parser disambiguation marker, not
     * part of the identifier; codegen drops it to match upstream
     * {@code @RosettaEnumValue(value = "...")} byte-parity.
     */
    public String getRosettaName() {
        return EnumHelper.stripEscape(astNode.name());
    }

    /** The display name, or null if not specified. */
    public String getDisplayName() {
        return astNode.displayName().orElse(null);
    }

    public RJavaEnumValue getParentValue() {
        return parentValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(astNode);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RJavaEnumValue other = (RJavaEnumValue) obj;
        return Objects.equals(astNode, other.astNode);
    }
}
