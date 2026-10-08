package com.regnosys.rosetta.ast.expressions.supporting;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;

import java.util.List;

/**
 * Key-value pair node for constructor expressions.
 *
 * <p>Represents a single field assignment in a type constructor,
 * e.g., {@code price: 100.0} or {@code party: myParty as-key}.
 */
public class RKeyValuePair extends RNode {

    private String key;
    private RExpression value;
    private boolean asKey;

    // -- key ------------------------------------------------------------------

    public String key() {
        return key;
    }

    public void setKey(String key) {
        checkMutable();
        this.key = key;
    }

    // -- value ----------------------------------------------------------------

    public RExpression value() {
        return value;
    }

    public void setValue(RExpression value) {
        checkMutable();
        this.value = value;
    }

    // -- asKey ----------------------------------------------------------------

    public boolean isAsKey() {
        return asKey;
    }

    public void setAsKey(boolean asKey) {
        checkMutable();
        this.asKey = asKey;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        if (value != null) {
            return List.of(value);
        }
        return List.of();
    }
}
