package com.regnosys.rosetta.ast.supporting;

import com.regnosys.rosetta.ast.RNode;

import java.util.List;

/**
 * Record type field node, corresponding to the {@code recordFeature} grammar rule.
 *
 * <p>Grammar:
 * <pre>
 * recordFeature:
 *     validID typeCall
 * ;
 * </pre>
 */
public class RRecordFeature extends RNode {

    private String name;
    private RTypeCall typeCall;

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

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        if (typeCall != null) {
            return List.of(typeCall);
        }
        return List.of();
    }
}
