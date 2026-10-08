package com.regnosys.rosetta.ast.supporting;

import com.regnosys.rosetta.ast.RNode;

import java.util.List;

/**
 * Library function parameter node, corresponding to the {@code parameter}
 * grammar rule inside {@code rosettaLibraryFunction}.
 *
 * <p>Grammar:
 * <pre>
 * parameter:
 *     validID typeCall (LBRACK RBRACK)?
 * ;
 * </pre>
 */
public class RParameter extends RNode {

    private String name;
    private RTypeCall typeCall;
    private boolean isArray;

    // -- name -----------------------------------------------------------------

    public String name() {
        return name;
    }

    public void setName(String name) {
        checkMutable();
        this.name = name;
    }

    // -- typeCall --------------------------------------------------------------

    public RTypeCall typeCall() {
        return typeCall;
    }

    public void setTypeCall(RTypeCall typeCall) {
        checkMutable();
        this.typeCall = typeCall;
    }

    // -- isArray --------------------------------------------------------------

    public boolean isArray() {
        return isArray;
    }

    public void setArray(boolean isArray) {
        checkMutable();
        this.isArray = isArray;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        if (typeCall != null) {
            return List.of(typeCall);
        }
        return List.of();
    }
}
