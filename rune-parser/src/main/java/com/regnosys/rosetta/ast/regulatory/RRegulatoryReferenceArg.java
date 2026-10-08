package com.regnosys.rosetta.ast.regulatory;

import com.regnosys.rosetta.ast.RNode;

import java.util.List;

/**
 * Named argument inside a regulatoryReference clause (P1.4.2 H4).
 *
 * <p>Grammar:
 * <pre>
 * regulatoryReferenceArgs : LPAREN namedArg (COMMA namedArg)* RPAREN ;
 * namedArg                : validID EQ STRING ;
 * </pre>
 *
 * <p>Layer-1 only — values are stored verbatim as strings; no typed
 * reconstruction (e.g. parsing dates / numbers from STRING values is
 * deferred to validation phase, not parser scope).
 */
public class RRegulatoryReferenceArg extends RNode {

    private String name;
    private String value;

    // -- name -----------------------------------------------------------------

    public String name() {
        return name;
    }

    public void setName(String n) {
        checkMutable();
        this.name = n;
    }

    // -- value ----------------------------------------------------------------

    public String value() {
        return value;
    }

    public void setValue(String v) {
        checkMutable();
        this.value = v;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        return List.of();
    }
}
