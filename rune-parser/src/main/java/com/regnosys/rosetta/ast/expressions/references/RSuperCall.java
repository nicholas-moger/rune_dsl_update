package com.regnosys.rosetta.ast.expressions.references;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

/**
 * Super call expression node, corresponding to the {@code SuperCallExpr}
 * grammar alternative.
 *
 * <p>Represents a call to the super function in a dispatching function.
 * Has no additional fields beyond the base {@link RExpression}.
 *
 * <p>EMF equivalent: {@code RosettaSuperCall}.
 */
public class RSuperCall extends RExpression {

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitSuperCall(this, context);
    }
}
