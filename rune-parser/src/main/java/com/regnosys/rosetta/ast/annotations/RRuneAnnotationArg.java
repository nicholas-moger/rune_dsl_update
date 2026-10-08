package com.regnosys.rosetta.ast.annotations;

import com.regnosys.rosetta.ast.RNode;

import java.util.List;

/**
 * Argument inside a {@link RRuneAnnotation} parenthesis list.
 *
 * <p>Grammar (P1.4.2 H2):
 * <pre>
 * runeAnnotationArg:
 *     validID EQ runeAnnotationLiteral
 * ;
 * </pre>
 *
 * <p>The literal value is stored as a string projection of the source token for
 * Layer-1 simplicity. Typed-value reconstruction (numeric, boolean,
 * {@code qualifiedName} reference) is reserved for the future W13 feature-registry
 * runtime.
 */
public class RRuneAnnotationArg extends RNode {

    private String name;
    private String valueAsString;

    // -- name -----------------------------------------------------------------

    public String name() {
        return name;
    }

    public void setName(String n) {
        checkMutable();
        this.name = n;
    }

    // -- valueAsString --------------------------------------------------------

    public String valueAsString() {
        return valueAsString;
    }

    public void setValueAsString(String v) {
        checkMutable();
        this.valueAsString = v;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        return List.of();
    }
}
