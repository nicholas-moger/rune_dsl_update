package com.regnosys.rosetta.ast.expressions.binary;

import com.regnosys.rosetta.ast.RBinaryExpression;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

/**
 * Default binary expression node, corresponding to the
 * {@code DefaultExpr} grammar alternative.
 *
 * <p>Represents the {@code default} operator that provides a fallback
 * value when the left operand is absent or empty.
 *
 * <p>Has no additional fields beyond left and right from {@link RBinaryExpression}.
 *
 * <p>EMF equivalent: {@code DefaultOperation}.
 */
public class RDefaultExpr extends RBinaryExpression {

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitDefault(this, context);
    }
}
