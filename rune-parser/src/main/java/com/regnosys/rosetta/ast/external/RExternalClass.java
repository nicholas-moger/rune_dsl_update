package com.regnosys.rosetta.ast.external;

import com.regnosys.rosetta.ast.RNode;

import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.linker.CrossRefField;

import java.util.ArrayList;
import java.util.List;

/**
 * External class mapping node, corresponding to the
 * {@code rosettaExternalClass} grammar rule.
 *
 * <p>Maps an external schema class to a Rosetta data type, carrying
 * class-level synonyms and attribute-level mappings.
 *
 * <p>Grammar:
 * <pre>
 * rosettaExternalClass:
 *     qualifiedName COLON
 *         rosettaExternalClassSynonym*
 *         rosettaExternalRegularAttribute*
 * ;
 * </pre>
 */
public class RExternalClass extends RNode {

    private String typeName;
    private final List<RExternalClassSynonym> classSynonyms = new ArrayList<>();
    private final List<RExternalRegularAttribute> attributes = new ArrayList<>();

    // -- typeName --------------------------------------------------------------

    public String typeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        checkMutable();
        this.typeName = typeName;
    }

    // -- classSynonyms --------------------------------------------------------

    public List<RExternalClassSynonym> classSynonyms() {
        return classSynonyms;
    }

    // -- attributes -----------------------------------------------------------

    public List<RExternalRegularAttribute> attributes() {
        return attributes;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        result.addAll(classSynonyms);
        result.addAll(attributes);
        return List.copyOf(result);
    }

    // === M3 resolved fields (D2) =============================================

    @CrossRefField(category = DiagnosticCategory.EXTERNAL_TYPE_NOT_FOUND, tokenRangeKey = "typeName")
    private com.regnosys.rosetta.ast.types.RDataType resolvedType;

    public java.util.Optional<com.regnosys.rosetta.ast.types.RDataType> referencedType() { return java.util.Optional.ofNullable(resolvedType); }
    public void setResolvedType(com.regnosys.rosetta.ast.types.RDataType resolved) { checkMutable(); this.resolvedType = resolved; }

    /**
     * PR #445 — the CHOICE-typed external class binding. An external synonym
     * source can annotate a {@code choice} declaration (CDM 6 restructured
     * {@code Payout}/{@code Product}/{@code Underlier}/{@code Asset}… as
     * choices, and the cdm6 mapping files carry external synonyms for them);
     * upstream scopes external classes over data AND choice types (its
     * choice-as-data model) and its 6.20.6 build is error-free over every
     * carrier. Bound in a PARALLEL field — {@link #referencedType()} keeps
     * its {@code RDataType} shape for the existing consumers (none of which
     * emit for the choice case: generation is byte-identical with these refs
     * unresolved, so the parallel bind is generation-invisible by
     * construction).
     */
    private com.regnosys.rosetta.ast.types.RChoice resolvedChoice;

    public java.util.Optional<com.regnosys.rosetta.ast.types.RChoice> referencedChoice() { return java.util.Optional.ofNullable(resolvedChoice); }
    public void setResolvedChoice(com.regnosys.rosetta.ast.types.RChoice resolved) { checkMutable(); this.resolvedChoice = resolved; }
}
