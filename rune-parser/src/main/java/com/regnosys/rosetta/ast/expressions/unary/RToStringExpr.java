package com.regnosys.rosetta.ast.expressions.unary;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

import java.util.List;
import java.util.Optional;

/**
 * To-string postfix expression node, corresponding to the
 * {@code ToStringExpr} grammar alternative.
 *
 * <p>Represents the {@code to-string} operator that converts an expression
 * to its string representation. Kept separate from {@link RConversionExpr}
 * because of its distinct semantics.
 */
public class RToStringExpr extends RExpression {

    private RExpression argument;

    // -- argument (the expression being converted) ----------------------------

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

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        if (argument != null) {
            return List.of(argument);
        }
        return List.of();
    }

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitToString(this, context);
    }
}
