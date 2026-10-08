package com.regnosys.rosetta.ast.supporting;

import com.regnosys.rosetta.ast.RDefinable;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.regulatory.RDocReference;
import com.regnosys.rosetta.ast.synonyms.REnumSynonym;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Enum value node, corresponding to the {@code rosettaEnumValue} grammar rule.
 *
 * <p>Represents a single value inside an enumeration, such as
 * {@code Active displayName "Active" <"An active state">}.
 *
 * <p>Grammar:
 * <pre>
 * rosettaEnumValue:
 *     validID (DISPLAY_NAME displayName=STRING)? definable?
 *     docReference*
 *     annotationRef*
 *     synonymDecl*
 * ;
 * </pre>
 */
public class REnumValue extends RNode implements RDefinable {

    private String name;
    private String displayName;
    private String definition;

    private final List<RDocReference> docReferences = new ArrayList<>();

    private final List<RAnnotationRef> annotationRefs = new ArrayList<>();

    private final List<REnumSynonym> synonyms = new ArrayList<>();

    // -- name -----------------------------------------------------------------

    public String name() {
        return name;
    }

    public void setName(String name) {
        checkMutable();
        this.name = name;
    }

    // -- displayName ----------------------------------------------------------

    public Optional<String> displayName() {
        return Optional.ofNullable(displayName);
    }

    public void setDisplayName(String displayName) {
        checkMutable();
        this.displayName = displayName;
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

    // -- docReferences --------------------------------------------------------

    public List<RDocReference> docReferences() {
        return docReferences;
    }

    // -- annotationRefs -------------------------------------------------------

    public List<RAnnotationRef> annotationRefs() {
        return annotationRefs;
    }

    // -- synonyms -------------------------------------------------------------

    public List<REnumSynonym> synonyms() {
        return synonyms;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        result.addAll(docReferences);
        result.addAll(annotationRefs);
        result.addAll(synonyms);
        return List.copyOf(result);
    }
}
