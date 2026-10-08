package com.regnosys.rosetta.ast.types;

import com.regnosys.rosetta.ast.RDefinable;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.regulatory.RDocReference;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.synonyms.RClassSynonym;

import com.regnosys.rosetta.symbols.StaleSymbolIdException;
import com.regnosys.rosetta.symbols.SymbolId;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.linker.CrossRefField;
import com.regnosys.rosetta.ast.types.RChoice;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data type declaration node, corresponding to the {@code dataType} grammar rule.
 *
 * <p>Represents a {@code type Foo extends Bar:} declaration with attributes,
 * conditions, annotations, doc references, and class synonyms.
 *
 * <p>Grammar:
 * <pre>
 * dataType:
 *     runeAnnotations?                          // P1.4.2 H2 — hoisted from RRootElement, see U008
 *     TYPE qualifiedName (EXTENDS superType=qualifiedName)? definable?
 *     docReference*
 *     annotationRef*
 *     classSynonym*
 *     attributes+=attribute*
 *     conditions+=condition*
 * ;
 * </pre>
 */
public class RDataType extends RRootElement implements RDefinable {

    private String name;
    private String superTypeName;
    private String definition;
    private final List<RAttribute> attributes = new ArrayList<>();

    private final List<RDocReference> docReferences = new ArrayList<>();

    private final List<RAnnotationRef> annotationRefs = new ArrayList<>();

    private final List<RClassSynonym> classSynonyms = new ArrayList<>();

    private final List<RCondition> conditions = new ArrayList<>();

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

    // -- attributes -----------------------------------------------------------

    public List<RAttribute> attributes() {
        return attributes;
    }

    // -- docReferences --------------------------------------------------------

    public List<RDocReference> docReferences() {
        return docReferences;
    }

    // -- annotationRefs -------------------------------------------------------

    public List<RAnnotationRef> annotationRefs() {
        return annotationRefs;
    }

    // -- classSynonyms --------------------------------------------------------

    public List<RClassSynonym> classSynonyms() {
        return classSynonyms;
    }

    // -- conditions -----------------------------------------------------------

    public List<RCondition> conditions() {
        return conditions;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        result.addAll(super.children());     // hoisted runeAnnotations (P1.4.2 H2)
        result.addAll(docReferences);
        result.addAll(annotationRefs);
        result.addAll(classSynonyms);
        result.addAll(attributes);
        result.addAll(conditions);
        return List.copyOf(result);
    }

    // === M3 resolved fields (D2) — migrated to SymbolId in P1.4.1b (H7 / U005)

    // Note: superTypeId covers the common case (data type extends data type).
    // choiceSuperTypeId covers the alternate case (data type extends choice type).
    // The @CrossRefField audit should consider the super type resolved when EITHER is set.
    // See isSuperTypeResolved() below.

    // Cross-reference key: rune-dsl super-type qualified name resolved to
    // (namespace, localName) at GlobalResolutionPass time. SymbolId carries the
    // workspace generation for staleness detection. See U005.
    @CrossRefField(category = DiagnosticCategory.SUPER_TYPE_NOT_FOUND, tokenRangeKey = "superType")
    private SymbolId superTypeId;

    /**
     * Returns the resolved data-type super-type, looked up lazily through the
     * attached workspace. Returns {@link Optional#empty()} if not declared, not
     * resolved, OR the super-type is a choice type (use {@link #choiceSuperType()}).
     *
     * @throws IllegalStateException if this node has no attached workspace
     *     (e.g., constructed manually outside {@code RWorkspace.build(List)})
     * @throws StaleSymbolIdException if the stored SymbolId was issued by
     *     a different workspace generation
     */
    public Optional<RDataType> superType() {
        if (superTypeId == null) return Optional.empty();
        // PR #445: this node is the requester — duplicate FQNs resolve
        // same-file/same-cell first (CellPreference).
        return Optional.ofNullable(workspace().resolve(superTypeId, RDataType.class, this));
    }

    /**
     * Returns the {@link SymbolId} of the resolved super-type (data-type case
     * only — choice super-type IDs are accessed via {@link #choiceSuperTypeId()};
     * use {@link #choiceSuperType()} for the resolved choice node).
     * New in P1.4.1b.
     */
    public Optional<SymbolId> superTypeId() {
        return Optional.ofNullable(superTypeId);
    }

    /**
     * Sets the super-type SymbolId. Called by {@code GlobalResolutionPass.resolveDataSuperType}
     * (replaces the removed {@code setResolvedSuperType(RDataType)} setter).
     */
    public void setSuperTypeId(SymbolId id) {
        checkMutable();
        this.superTypeId = id;
    }

    // Choice type super type: when `type Foo extends ChoiceBar`, the super type is a choice.
    // Choice types generate as POJO interfaces (upstream: caseChoiceType → caseDataType).
    // Migrated to SymbolId in Task 4.2 (P1.4.1b — H7 / U005).
    // Both superTypeId and choiceSuperTypeId share "superType" tokenRangeKey and
    // DiagnosticCategory.SUPER_TYPE_NOT_FOUND since they resolve the same source field.
    @CrossRefField(category = DiagnosticCategory.SUPER_TYPE_NOT_FOUND, tokenRangeKey = "superType")
    private SymbolId choiceSuperTypeId;

    /**
     * Returns the resolved choice-type super-type, looked up lazily through the
     * attached workspace. Returns {@link Optional#empty()} if not declared, not
     * resolved, OR the super-type is a data type (use {@link #superType()}).
     *
     * @throws IllegalStateException if this node has no attached workspace
     *     (e.g., constructed manually outside {@code RWorkspace.build(List)})
     * @throws StaleSymbolIdException if the stored SymbolId was issued by
     *     a different workspace generation
     */
    public Optional<RChoice> choiceSuperType() {
        if (choiceSuperTypeId == null) return Optional.empty();
        // PR #445: this node is the requester — duplicate FQNs resolve
        // same-file/same-cell first (CellPreference).
        return Optional.ofNullable(workspace().resolve(choiceSuperTypeId, RChoice.class, this));
    }

    /**
     * Returns the {@link SymbolId} of the resolved choice-type super-type
     * (choice case only — data-type super-type IDs are accessed via
     * {@link #superTypeId()}; use {@link #superType()} for the resolved
     * data-type node).
     * New in P1.4.1b.
     */
    public Optional<SymbolId> choiceSuperTypeId() {
        return Optional.ofNullable(choiceSuperTypeId);
    }

    /**
     * Sets the choice-super-type SymbolId. Called by
     * {@code GlobalResolutionPass.resolveDataSuperType} choice branch
     * (replaces the removed {@code setResolvedChoiceSuperType(RChoice)} setter).
     */
    public void setChoiceSuperTypeId(SymbolId id) {
        checkMutable();
        this.choiceSuperTypeId = id;
    }

    /** Whether the super type is resolved (either as data type via SymbolId or choice type via SymbolId). */
    public boolean isSuperTypeResolved() {
        return superTypeId != null || choiceSuperTypeId != null;
    }
}
