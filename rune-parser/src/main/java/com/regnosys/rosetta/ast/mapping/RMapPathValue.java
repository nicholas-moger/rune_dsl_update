package com.regnosys.rosetta.ast.mapping;

import com.regnosys.rosetta.ast.RNode;

/**
 * Map path value node — a STRING-valued path in mapping tests.
 *
 * <p>Grammar: {@code rosettaMapPathValue: value=STRING ;}
 */
public class RMapPathValue extends RNode {

    private String value;

    // -- value ----------------------------------------------------------------

    public String value() {
        return value;
    }

    public void setValue(String value) {
        checkMutable();
        this.value = value;
    }
}
