package com.regnosys.rosetta.ast.external;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.annotations.RRuleReferenceAnnotation;

import java.util.ArrayList;
import java.util.List;

/**
 * External regular attribute mapping node, corresponding to the
 * {@code rosettaExternalRegularAttribute} grammar rule.
 *
 * <p>Represents an attribute-level mapping within an external class.
 * The {@code isAddition} flag distinguishes additions ({@code +}) from
 * removals ({@code -}).
 *
 * <p>Grammar:
 * <pre>
 * rosettaExternalRegularAttribute:
 *     (PLUS | MINUS) validID
 *         rosettaExternalSynonym*
 *         ruleReferenceAnnotation*
 * ;
 * </pre>
 */
public class RExternalRegularAttribute extends RNode {

    private boolean addition;
    private String name;
    private final List<RExternalSynonym> synonyms = new ArrayList<>();
    private final List<RRuleReferenceAnnotation> ruleRefs = new ArrayList<>();

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

    public List<RExternalSynonym> synonyms() {
        return synonyms;
    }

    // -- ruleRefs -------------------------------------------------------------

    public List<RRuleReferenceAnnotation> ruleRefs() {
        return ruleRefs;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        result.addAll(synonyms);
        result.addAll(ruleRefs);
        return List.copyOf(result);
    }

    // === M4 resolved fields (D9) =============================================

    @com.regnosys.rosetta.symbols.linker.CrossRefField(
        category = com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory.EXTERNAL_ATTRIBUTE_NOT_FOUND)
    private com.regnosys.rosetta.ast.supporting.RAttribute resolvedAttribute;

    public java.util.Optional<com.regnosys.rosetta.ast.supporting.RAttribute> resolvedAttribute() {
        return java.util.Optional.ofNullable(resolvedAttribute);
    }
    public void setResolvedAttribute(com.regnosys.rosetta.ast.supporting.RAttribute resolved) {
        checkMutable();
        this.resolvedAttribute = resolved;
    }
}
