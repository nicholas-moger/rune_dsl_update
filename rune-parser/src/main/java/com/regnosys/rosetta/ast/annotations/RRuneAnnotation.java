package com.regnosys.rosetta.ast.annotations;

import com.regnosys.rosetta.ast.RNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Rune-style annotation with @-prefix syntax: {@code @namespace.feature(arg = value, ...)}.
 *
 * <p>Distinct from {@link RAnnotationRef}, which represents the legacy bracket-style
 * {@code [annotation qualifier "value"]} syntax. Both mechanisms co-exist; rune
 * annotations are reserved for fork-specific features such as W13 opt-in markers.
 *
 * <p>Grammar (P1.4.2 H2):
 * <pre>
 * runeAnnotation:
 *     AT qualifiedName (LPAREN runeAnnotationArgs? RPAREN)?
 * ;
 * </pre>
 *
 * <p>This is a Layer-1 surface only (P1.4.2). Annotations are parsed and stored but
 * carry no runtime semantics until the W13 feature-registry lands in a future
 * sub-phase.
 */
public class RRuneAnnotation extends RNode {

    private String annotationName;
    private final List<RRuneAnnotationArg> arguments = new ArrayList<>();

    // -- annotationName -------------------------------------------------------

    public String annotationName() {
        return annotationName;
    }

    public void setAnnotationName(String n) {
        checkMutable();
        this.annotationName = n;
    }

    // -- arguments ------------------------------------------------------------

    public List<RRuneAnnotationArg> arguments() {
        return arguments;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        return List.copyOf(arguments);
    }
}
