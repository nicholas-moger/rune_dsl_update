package com.regnosys.rosetta.ast.expressions.supporting;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;

import java.util.List;

/**
 * With-meta entry node for the {@code with-meta} expression.
 *
 * <p>Represents a single metadata key-value assignment within a
 * {@code with-meta} expression block, e.g., {@code scheme: "http://..."}.
 */
public class RWithMetaEntry extends RNode {

    private String metaName;
    private RExpression value;

    // -- metaName -------------------------------------------------------------

    public String metaName() {
        return metaName;
    }

    public void setMetaName(String metaName) {
        checkMutable();
        this.metaName = metaName;
    }

    // -- value ----------------------------------------------------------------

    public RExpression value() {
        return value;
    }

    public void setValue(RExpression value) {
        checkMutable();
        this.value = value;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        if (value != null) {
            return List.of(value);
        }
        return List.of();
    }

    // === M4 resolved fields (D9) =============================================

    @com.regnosys.rosetta.symbols.linker.CrossRefField(
        category = com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory.META_KEY_NOT_FOUND)
    private com.regnosys.rosetta.ast.RNode resolvedMetaKey;

    public java.util.Optional<com.regnosys.rosetta.ast.RNode> resolvedMetaKey() {
        return java.util.Optional.ofNullable(resolvedMetaKey);
    }
    public void setResolvedMetaKey(com.regnosys.rosetta.ast.RNode resolved) {
        checkMutable();
        this.resolvedMetaKey = resolved;
    }
}
