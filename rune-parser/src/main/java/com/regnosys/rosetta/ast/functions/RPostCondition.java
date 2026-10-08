package com.regnosys.rosetta.ast.functions;

import com.regnosys.rosetta.ast.RDefinable;
import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Post-condition block node, corresponding to the {@code postCondition}
 * grammar rule.
 *
 * <p>Post-conditions are separate from {@link RCondition} — they appear only
 * in function bodies and are evaluated after the function executes.
 *
 * <p>Grammar:
 * <pre>
 * postCondition:
 *     POST_CONDITION validID? definable? COLON
 *     expression
 * ;
 * </pre>
 */
public class RPostCondition extends RNode implements RDefinable {

    private String name;
    private String definition;
    private RExpression expression;

    // -- name -----------------------------------------------------------------

    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    public void setName(String name) {
        checkMutable();
        this.name = name;
    }

    // -- definition (RDefinable) ----------------------------------------------

    @Override
    public Optional<String> definition() {
        return Optional.ofNullable(definition);
    }

    public void setDefinition(String definition) {
        checkMutable();
        this.definition = definition;
    }

    // -- expression -----------------------------------------------------------

    public RExpression expression() {
        return expression;
    }

    public void setExpression(RExpression expression) {
        checkMutable();
        this.expression = expression;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        if (expression != null) {
            return List.of(expression);
        }
        return List.of();
    }
}
