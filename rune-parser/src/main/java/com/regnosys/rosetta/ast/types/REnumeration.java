package com.regnosys.rosetta.ast.types;

import com.regnosys.rosetta.ast.RDefinable;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.regulatory.RDocReference;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.synonyms.RSynonym;

import com.regnosys.rosetta.symbols.SymbolId;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.linker.CrossRefField;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Enumeration declaration node, corresponding to the {@code enumeration} grammar rule.
 *
 * <p>Represents an {@code enum Foo extends Bar:} declaration with values,
 * annotations, doc references, and synonyms.
 *
 * <p>Grammar:
 * <pre>
 * enumeration:
 *     runeAnnotations?                          // P1.4.2 H2 — hoisted from RRootElement, see U008
 *     ENUM qualifiedName (EXTENDS superType=qualifiedName)? definable?
 *     docReference*
 *     annotationRef*
 *     synonymDecl*
 *     values+=rosettaEnumValue*
 * ;
 * </pre>
 */
public class REnumeration extends RRootElement implements RDefinable {

    private String name;
    private String superTypeName;
    private String definition;
    private final List<REnumValue> values = new ArrayList<>();

    private final List<RDocReference> docReferences = new ArrayList<>();

    private final List<RAnnotationRef> annotationRefs = new ArrayList<>();

    private final List<RSynonym> synonyms = new ArrayList<>();

    // -- name -----------------------------------------------------------------

    public String name() {
        return name;
    }

    public void setName(String name) {
        checkMutable();
        this.name = name;
    }

    // -- superTypeName --------------------------------------------------------

    public Optional<String> superTypeName() {
        return Optional.ofNullable(superTypeName);
    }

    public void setSuperTypeName(String superTypeName) {
        checkMutable();
        this.superTypeName = superTypeName;
    }

    // -- definition (RDefinable) ----------------------------------------------

    @Override
    public Optional<String> definition() {
        return Optional.ofNullable(definition);
    }

    public void setDefinition(String definition) {
        checkMutable();
        this.definition = definition;
    }

    // -- values ---------------------------------------------------------------

    public List<REnumValue> values() {
        return values;
    }

    // -- docReferences --------------------------------------------------------

    public List<RDocReference> docReferences() {
        return docReferences;
    }

    // -- annotationRefs -------------------------------------------------------

    public List<RAnnotationRef> annotationRefs() {
        return annotationRefs;
    }

    // -- synonyms -------------------------------------------------------------

    public List<RSynonym> synonyms() {
        return synonyms;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        result.addAll(super.children());     // hoisted runeAnnotations (P1.4.2 H2)
        result.addAll(docReferences);
        result.addAll(annotationRefs);
        result.addAll(synonyms);
        result.addAll(values);
        return List.copyOf(result);
    }

    // === M3 resolved fields (D2) =============================================

    @CrossRefField(category = DiagnosticCategory.SUPER_TYPE_NOT_FOUND, tokenRangeKey = "superType")
    private SymbolId superTypeId;

    /**
     * Returns the resolved super-enum, looked up lazily through the attached workspace.
     * Returns Optional.empty() if not declared or not resolved.
     *
     * @throws IllegalStateException if this node has no attached workspace
     * @throws com.regnosys.rosetta.symbols.StaleSymbolIdException if the stored SymbolId was issued by a different
     *     workspace generation
     */
    public Optional<REnumeration> superType() {
        if (superTypeId == null) return Optional.empty();
        // PR #445: this node is the requester — duplicate FQNs resolve
        // same-file/same-cell first (CellPreference).
        return Optional.ofNullable(workspace().resolve(superTypeId, REnumeration.class, this));
    }

    /**
     * Returns the {@link SymbolId} of the resolved super-enum. Use {@link #superType()}
     * for the resolved REnumeration node. New in P1.4.1b.
     */
    public Optional<SymbolId> superTypeId() {
        return Optional.ofNullable(superTypeId);
    }

    public void setSuperTypeId(SymbolId id) {
        checkMutable();
        this.superTypeId = id;
    }
}
