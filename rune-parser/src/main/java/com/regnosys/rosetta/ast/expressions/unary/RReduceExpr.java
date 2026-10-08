package com.regnosys.rosetta.ast.expressions.unary;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reduce postfix expression node, corresponding to the
 * {@code ReduceExpr} grammar alternative.
 *
 * <p>Represents the {@code reduce} operator with a required inline function
 * body that specifies the reduction logic.
 */
public class RReduceExpr extends RExpression {

    private RExpression argument;
    private RInlineFunction body;

    // -- argument (the list expression being reduced) -------------------------

    public RExpression argument() {
        return argument;
    }

    public void setArgument(RExpression argument) {
        checkMutable();
        this.argument = argument;
    }

    @Override
    public Optional<RExpression> left() {
        return Optional.ofNullable(argument);
    }

    // -- body (required) ------------------------------------------------------

    public RInlineFunction body() {
        return body;
    }

    public void setBody(RInlineFunction body) {
        checkMutable();
        this.body = body;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        if (argument != null) {
            result.add(argument);
        }
        if (body != null) {
            result.add(body);
        }
        return List.copyOf(result);
    }

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitReduce(this, context);
    }
}
