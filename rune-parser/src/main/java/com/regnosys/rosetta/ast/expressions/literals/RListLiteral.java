package com.regnosys.rosetta.ast.expressions.literals;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * List literal expression node, corresponding to the
 * {@code ListLiteralExpr} grammar alternative.
 *
 * <p>Represents a list literal (e.g., {@code [1, 2, 3]}) in the
 * Rune DSL expression grammar.
 *
 * <p>EMF equivalent: {@code ListLiteral}.
 */
public class RListLiteral extends RExpression {

    private final List<RExpression> elements = new ArrayList<>();

    // -- elements -------------------------------------------------------------

    public List<RExpression> elements() {
        return elements;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        return List.copyOf(elements);
    }

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitListLiteral(this, context);
    }
}
