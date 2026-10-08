package com.regnosys.rosetta.ast.expressions.references;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

/**
 * Empty literal expression node, corresponding to the
 * {@code EmptyLiteralExpr} grammar alternative.
 *
 * <p>Represents the {@code empty} keyword in expressions.
 * Has no additional fields beyond the base {@link RExpression}.
 *
 * <p>EMF equivalent: {@code EmptyLiteral}.
 */
public class REmptyLiteral extends RExpression {

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitEmptyLiteral(this, context);
    }
}
