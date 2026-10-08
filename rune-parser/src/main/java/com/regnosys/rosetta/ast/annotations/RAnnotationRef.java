package com.regnosys.rosetta.ast.annotations;

import com.regnosys.rosetta.ast.RNode;

import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.linker.CrossRefField;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Annotation reference node, corresponding to the {@code annotationRef}
 * grammar rule.
 *
 * <p>Represents an annotation usage such as {@code [metadata key]} or
 * {@code [rootType]} applied to a type, attribute, function, etc.
 *
 * <p>Grammar:
 * <pre>
 * annotationRef:
 *     LBRACK validID validID? annotationQualifier* RBRACK
 * ;
 * </pre>
 */
public class RAnnotationRef extends RNode {

    private String annotationName;
    private String qualifierName;
    private final List<RAnnotationQualifier> qualifiers = new ArrayList<>();

    // -- annotationName -------------------------------------------------------

    public String annotationName() {
        return annotationName;
    }

    public void setAnnotationName(String annotationName) {
        checkMutable();
        this.annotationName = annotationName;
    }

    // -- qualifierName --------------------------------------------------------

    public Optional<String> qualifierName() {
        return Optional.ofNullable(qualifierName);
    }

    public void setQualifierName(String qualifierName) {
        checkMutable();
        this.qualifierName = qualifierName;
    }

    // -- qualifiers -----------------------------------------------------------

    public List<RAnnotationQualifier> qualifiers() {
        return qualifiers;
    }

    // -- children (for traversal) ---------------------------------------------

    /**
     * Returns an unmodifiable view of the annotation qualifiers, per the
     * {@link RNode#children()} contract.
     */
    @Override
    public List<? extends RNode> children() {
        return List.copyOf(qualifiers);
    }

    // === M3 resolved fields (D2) =============================================

    @CrossRefField(category = DiagnosticCategory.ANNOTATION_NOT_FOUND)
    private com.regnosys.rosetta.ast.annotations.RAnnotation resolvedAnnotation;

    @CrossRefField(category = DiagnosticCategory.ANNOTATION_QUALIFIER_NOT_FOUND)
    private com.regnosys.rosetta.ast.supporting.RAttribute resolvedQualifier;

    public java.util.Optional<com.regnosys.rosetta.ast.annotations.RAnnotation> annotation() { return java.util.Optional.ofNullable(resolvedAnnotation); }
    public void setResolvedAnnotation(com.regnosys.rosetta.ast.annotations.RAnnotation resolved) { checkMutable(); this.resolvedAnnotation = resolved; }

    public java.util.Optional<com.regnosys.rosetta.ast.supporting.RAttribute> qualifier() { return java.util.Optional.ofNullable(resolvedQualifier); }
    public void setResolvedQualifier(com.regnosys.rosetta.ast.supporting.RAttribute resolved) { checkMutable(); this.resolvedQualifier = resolved; }
}
