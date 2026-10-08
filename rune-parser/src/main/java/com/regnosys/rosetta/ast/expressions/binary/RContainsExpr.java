package com.regnosys.rosetta.ast.expressions.binary;

import com.regnosys.rosetta.ast.RBinaryExpression;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

/**
 * Contains binary expression node, corresponding to the
 * {@code ContainsExpr} grammar alternative.
 *
 * <p>Represents the {@code contains} operator that checks if the left
 * operand contains the right operand.
 *
 * <p>Has no additional fields beyond left and right from {@link RBinaryExpression}.
 *
 * <p>EMF equivalent: {@code RosettaContainsExpression}.
 */
public class RContainsExpr extends RBinaryExpression {

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitContains(this, context);
    }
}
