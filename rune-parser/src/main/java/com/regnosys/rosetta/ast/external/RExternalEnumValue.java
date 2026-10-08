package com.regnosys.rosetta.ast.external;

import com.regnosys.rosetta.ast.RNode;

import java.util.ArrayList;
import java.util.List;

/**
 * External enum value mapping node, corresponding to the
 * {@code rosettaExternalEnumValue} grammar rule.
 *
 * <p>Represents a value-level mapping within an external enum.
 * The {@code isAddition} flag distinguishes additions ({@code +}) from
 * removals ({@code -}).
 *
 * <p>Grammar:
 * <pre>
 * rosettaExternalEnumValue:
 *     (PLUS | MINUS) validID
 *         rosettaExternalEnumSynonym*
 * ;
 * </pre>
 */
public class RExternalEnumValue extends RNode {

    private boolean addition;
    private String name;
    private final List<RExternalEnumSynonym> synonyms = new ArrayList<>();

    // -- addition -------------------------------------------------------------

    public boolean isAddition() {
        return addition;
    }

    public void setAddition(boolean addition) {
        checkMutable();
        this.addition = addition;
    }

    // -- name -----------------------------------------------------------------

    public String name() {
        return name;
    }

    public void setName(String name) {
        checkMutable();
        this.name = name;
    }

    // -- synonyms -------------------------------------------------------------

    public List<RExternalEnumSynonym> synonyms() {
        return synonyms;
    }

    // -- children (for traversal) ---------------------------------------------

    /**
     * Returns an unmodifiable view of the enum value synonyms, per the
     * {@link RNode#children()} contract.
     */
    @Override
    public List<? extends RNode> children() {
        return List.copyOf(synonyms);
    }

    // === M4 resolved fields (D9) =============================================

    @com.regnosys.rosetta.symbols.linker.CrossRefField(
        category = com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory.EXTERNAL_ENUM_VALUE_NOT_FOUND)
    private com.regnosys.rosetta.ast.supporting.REnumValue resolvedEnumValue;

    public java.util.Optional<com.regnosys.rosetta.ast.supporting.REnumValue> resolvedEnumValue() {
        return java.util.Optional.ofNullable(resolvedEnumValue);
    }
    public void setResolvedEnumValue(com.regnosys.rosetta.ast.supporting.REnumValue resolved) {
        checkMutable();
        this.resolvedEnumValue = resolved;
    }
}
