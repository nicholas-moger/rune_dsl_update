package com.regnosys.rosetta.ast.supporting;

import com.regnosys.rosetta.ast.RNode;

import java.util.List;

/**
 * Type call argument node, corresponding to the {@code typeCallArgument}
 * grammar rule.
 *
 * <p>Grammar:
 * <pre>
 * typeCallArgument:
 *     parameterName=typeParameterValidID COLON value=typeCallArgumentExpression
 * ;
 * </pre>
 */
public class RTypeCallArgument extends RNode {

    private String parameterName;
    private RTypeCallArgumentExpression value;

    // -- parameterName --------------------------------------------------------

    public String parameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        checkMutable();
        this.parameterName = parameterName;
    }

    // -- value ----------------------------------------------------------------

    public RTypeCallArgumentExpression value() {
        return value;
    }

    public void setValue(RTypeCallArgumentExpression value) {
        checkMutable();
        this.value = value;
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
