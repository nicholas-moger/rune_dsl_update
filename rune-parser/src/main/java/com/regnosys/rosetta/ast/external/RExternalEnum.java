package com.regnosys.rosetta.ast.external;

import com.regnosys.rosetta.ast.RNode;

import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.linker.CrossRefField;

import java.util.ArrayList;
import java.util.List;

/**
 * External enum mapping node, corresponding to the
 * {@code rosettaExternalEnum} grammar rule.
 *
 * <p>Maps an external schema enum to a Rosetta enumeration, carrying
 * value-level mappings.
 *
 * <p>Grammar:
 * <pre>
 * rosettaExternalEnum:
 *     qualifiedName COLON
 *         rosettaExternalEnumValue*
 * ;
 * </pre>
 */
public class RExternalEnum extends RNode {

    private String typeName;
    private final List<RExternalEnumValue> values = new ArrayList<>();

    // -- typeName --------------------------------------------------------------

    public String typeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        checkMutable();
        this.typeName = typeName;
    }

    // -- values ---------------------------------------------------------------

    public List<RExternalEnumValue> values() {
        return values;
    }

    // -- children (for traversal) ---------------------------------------------

    /**
     * Returns an unmodifiable view of the enum values, per the
     * {@link RNode#children()} contract.
     */
    @Override
    public List<? extends RNode> children() {
        return List.copyOf(values);
    }

    // === M3 resolved fields (D2) =============================================

    @CrossRefField(category = DiagnosticCategory.EXTERNAL_TYPE_NOT_FOUND, tokenRangeKey = "typeName")
    private com.regnosys.rosetta.ast.types.REnumeration resolvedType;

    public java.util.Optional<com.regnosys.rosetta.ast.types.REnumeration> referencedType() { return java.util.Optional.ofNullable(resolvedType); }
    public void setResolvedType(com.regnosys.rosetta.ast.types.REnumeration resolved) { checkMutable(); this.resolvedType = resolved; }
}
