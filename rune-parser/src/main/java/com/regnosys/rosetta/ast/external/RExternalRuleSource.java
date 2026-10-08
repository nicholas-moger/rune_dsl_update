package com.regnosys.rosetta.ast.external;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;

import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.linker.CrossRefField;

import java.util.ArrayList;
import java.util.List;

/**
 * External rule source declaration node, corresponding to the
 * {@code rosettaExternalRuleSource} grammar rule.
 *
 * <p>Represents an external rule source that maps external schema elements
 * to Rosetta reporting rules. This is a top-level (root) element.
 *
 * <p>Grammar:
 * <pre>
 * rosettaExternalRuleSource:
 *     RULE SOURCE qualifiedName
 *     (EXTENDS qualifiedName (COMMA qualifiedName)*)?
 *     LBRACE
 *         rosettaExternalClass*
 *         (ENUMS rosettaExternalEnum*)?
 *     RBRACE
 * ;
 * </pre>
 */
public class RExternalRuleSource extends RRootElement {

    private String name;
    private final List<String> superSourceNames = new ArrayList<>();
    private final List<RExternalClass> classes = new ArrayList<>();
    private final List<RExternalEnum> enums = new ArrayList<>();

    // -- name -----------------------------------------------------------------

    public String name() {
        return name;
    }

    public void setName(String name) {
        checkMutable();
        this.name = name;
    }

    // -- superSourceNames -----------------------------------------------------

    public List<String> superSourceNames() {
        return superSourceNames;
    }

    // -- classes --------------------------------------------------------------

    public List<RExternalClass> classes() {
        return classes;
    }

    // -- enums ----------------------------------------------------------------

    public List<RExternalEnum> enums() {
        return enums;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        result.addAll(super.children());     // hoisted runeAnnotations (P1.4.2 H2)
        result.addAll(classes);
        result.addAll(enums);
        return List.copyOf(result);
    }

    // === M3 resolved fields (D2) =============================================

    @CrossRefField(category = DiagnosticCategory.EXTERNAL_SOURCE_NOT_FOUND, tokenRangePrefix = "superSource")
    private final java.util.List<RExternalRuleSource> resolvedSuperSources = new java.util.ArrayList<>();

    public java.util.List<RExternalRuleSource> superSources() { return java.util.List.copyOf(resolvedSuperSources); }
    public void addResolvedSuperSource(RExternalRuleSource source) { checkMutable(); this.resolvedSuperSources.add(source); }
}
