package com.regnosys.rosetta.ast.expressions.binary;

import com.regnosys.rosetta.ast.RBinaryExpression;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

/**
 * Disjoint binary expression node, corresponding to the
 * {@code DisjointExpr} grammar alternative.
 *
 * <p>Represents the {@code disjoint} operator that checks if the left
 * and right operands have no common elements.
 *
 * <p>Has no additional fields beyond left and right from {@link RBinaryExpression}.
 *
 * <p>EMF equivalent: {@code RosettaDisjointExpression}.
 */
public class RDisjointExpr extends RBinaryExpression {

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitDisjoint(this, context);
    }
}
