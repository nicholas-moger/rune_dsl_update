package com.regnosys.rosetta.ast.types;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.supporting.RTypeCall;

import java.util.ArrayList;
import java.util.List;

/**
 * Meta type declaration node, corresponding to the {@code rosettaMetaType}
 * grammar rule.
 *
 * <p>Represents a {@code metaType Foo typeCall} declaration.
 * NOTE: Meta types do NOT implement {@code RDefinable} — the grammar
 * has no {@code definable?} for meta types.
 *
 * <p>Grammar:
 * <pre>
 * rosettaMetaType:
 *     META_TYPE validID typeCall
 * ;
 * </pre>
 */
public class RMetaType extends RRootElement {

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
        List<RNode> result = new ArrayList<>();
        result.addAll(super.children());     // hoisted runeAnnotations (P1.4.2 H2)
        if (typeCall != null) {
            result.add(typeCall);
        }
        return List.copyOf(result);
    }
}
