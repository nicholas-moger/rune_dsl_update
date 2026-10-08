package com.regnosys.rosetta.ast.annotations;

import com.regnosys.rosetta.ast.RNode;

/**
 * A single segment in an annotation path expression, corresponding to
 * {@code (ARROW validID | DEEP_ARROW validID)} in the grammar.
 *
 * <p>Each segment records whether it uses deep navigation ({@code ->>}) vs
 * shallow navigation ({@code ->}), plus the attribute name.
 */
public class RAnnotationPathSegment extends RNode {

    private boolean deep;
    private String name;

    // -- deep -----------------------------------------------------------------

    /**
     * Returns {@code true} if this segment uses the deep-arrow ({@code ->>})
     * operator, {@code false} for the regular arrow ({@code ->}).
     */
    public boolean isDeep() {
        return deep;
    }

    public void setDeep(boolean deep) {
        checkMutable();
        this.deep = deep;
    }

    // -- name -----------------------------------------------------------------

    public String name() {
        return name;
    }

    public void setName(String name) {
        checkMutable();
        this.name = name;
    }

    // === M4 resolved fields (D9) =============================================

    @com.regnosys.rosetta.symbols.linker.CrossRefField(
        category = com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory.FEATURE_NOT_FOUND,
        tokenRangeKey = "name")
    private com.regnosys.rosetta.ast.supporting.RAttribute resolvedAttribute;

    public java.util.Optional<com.regnosys.rosetta.ast.supporting.RAttribute> resolvedAttribute() {
        return java.util.Optional.ofNullable(resolvedAttribute);
    }
    public void setResolvedAttribute(com.regnosys.rosetta.ast.supporting.RAttribute resolved) {
        checkMutable();
        this.resolvedAttribute = resolved;
    }
}
