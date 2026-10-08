package com.regnosys.rosetta.ast.annotations;

import com.regnosys.rosetta.ast.RNode;

/**
 * Annotation qualifier node, corresponding to the
 * {@code annotationQualifier} grammar rule.
 *
 * <p>Represents a key-value qualifier inside an annotation reference,
 * such as {@code "rationale" = "Some reason"} or
 * {@code "path" = Trade -> tradeLeg}.
 *
 * <p>Grammar:
 * <pre>
 * annotationQualifier:
 *     STRING EQ (STRING | rosettaAttributeReference)
 * ;
 * </pre>
 */
public class RAnnotationQualifier extends RNode {

    private String key;
    private String value;
    private boolean attributeRef;

    // -- key ------------------------------------------------------------------

    public String key() {
        return key;
    }

    public void setKey(String key) {
        checkMutable();
        this.key = key;
    }

    // -- value ----------------------------------------------------------------

    public String value() {
        return value;
    }

    public void setValue(String value) {
        checkMutable();
        this.value = value;
    }

    // -- attributeRef ---------------------------------------------------------

    /**
     * Returns {@code true} if the value is a {@code rosettaAttributeReference}
     * rather than a plain STRING literal.
     */
    public boolean isAttributeRef() {
        return attributeRef;
    }

    public void setAttributeRef(boolean attributeRef) {
        checkMutable();
        this.attributeRef = attributeRef;
    }
}
