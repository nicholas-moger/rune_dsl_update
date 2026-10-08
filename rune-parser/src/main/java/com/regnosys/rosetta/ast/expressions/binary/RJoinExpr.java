package com.regnosys.rosetta.ast.expressions.binary;

import com.regnosys.rosetta.ast.RBinaryExpression;
import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Join binary expression node, corresponding to the
 * {@code JoinExpr} grammar alternative.
 *
 * <p>Represents the {@code join} operator that concatenates list elements
 * into a string, optionally using a separator expression.
 *
 * <p>EMF equivalent: {@code JoinOperation}.
 */
public class RJoinExpr extends RBinaryExpression {

    private RExpression separator;

    // -- separator (optional) -------------------------------------------------

    public Optional<RExpression> separator() {
        return Optional.ofNullable(separator);
    }

    public void setSeparator(RExpression separator) {
        checkMutable();
        this.separator = separator;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        if (rawLeft() != null) {
            result.add(rawLeft());
        }
        if (rawRight() != null) {
            result.add(rawRight());
        }
        if (separator != null) {
            result.add(separator);
        }
        return List.copyOf(result);
    }

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitJoin(this, context);
    }
}
