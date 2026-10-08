package com.regnosys.rosetta.ast.supporting;

import com.regnosys.rosetta.ast.RDefinable;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.annotations.RLabelAnnotation;
import com.regnosys.rosetta.ast.annotations.RRuleReferenceAnnotation;
import com.regnosys.rosetta.ast.regulatory.RDocReference;
import com.regnosys.rosetta.ast.synonyms.RSynonym;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Choice option node, corresponding to the {@code choiceOption} grammar rule.
 *
 * <p>Represents a single option inside a choice type. NOTE: There is no
 * separate {@code name} field — the {@code typeCall} IS the identity.
 *
 * <p>Grammar:
 * <pre>
 * choiceOption:
 *     typeCall definable?
 *     docReference*
 *     annotationRef*
 *     synonymDecl*
 *     labelAnnotation*
 *     ruleReferenceAnnotation*
 * ;
 * </pre>
 */
public class RChoiceOption extends RNode implements RDefinable {

    private RTypeCall typeCall;
    private String definition;

    private final List<RDocReference> docReferences = new ArrayList<>();

    private final List<RAnnotationRef> annotationRefs = new ArrayList<>();

    private final List<RSynonym> synonyms = new ArrayList<>();

    private final List<RLabelAnnotation> labelAnnotations = new ArrayList<>();

    private final List<RRuleReferenceAnnotation> ruleReferenceAnnotations = new ArrayList<>();

    // -- typeCall --------------------------------------------------------------

    public RTypeCall typeCall() {
        return typeCall;
    }

    public void setTypeCall(RTypeCall typeCall) {
        checkMutable();
        this.typeCall = typeCall;
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

    public List<RSynonym> synonyms() {
        return synonyms;
    }

    // -- labelAnnotations -----------------------------------------------------

    public List<RLabelAnnotation> labelAnnotations() {
        return labelAnnotations;
    }

    // -- ruleReferenceAnnotations ---------------------------------------------

    public List<RRuleReferenceAnnotation> ruleReferenceAnnotations() {
        return ruleReferenceAnnotations;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        if (typeCall != null) {
            result.add(typeCall);
        }
        result.addAll(docReferences);
        result.addAll(annotationRefs);
        result.addAll(synonyms);
        result.addAll(labelAnnotations);
        result.addAll(ruleReferenceAnnotations);
        return List.copyOf(result);
    }
}
