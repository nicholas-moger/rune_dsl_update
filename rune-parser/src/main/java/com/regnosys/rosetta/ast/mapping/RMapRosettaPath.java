package com.regnosys.rosetta.ast.mapping;

/**
 * Map test for a Rosetta attribute path reference.
 *
 * <p>Grammar alternative: {@code ROSETTA_PATH attributeReference=qualifiedName}
 */
public class RMapRosettaPath extends RMapTest {

    private String attributeReference;

    // -- attributeReference ----------------------------------------------------

    public String attributeReference() {
        return attributeReference;
    }

    public void setAttributeReference(String attributeReference) {
        checkMutable();
        this.attributeReference = attributeReference;
    }
}
