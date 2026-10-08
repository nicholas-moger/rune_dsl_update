package com.regnosys.rosetta.ast;

import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

import java.util.Optional;

/**
 * Abstract base class for all expression nodes in the Rune DSL AST.
 *
 * <p>Expressions form a rich hierarchy reflecting the grammar's
 * ~90 labelled alternatives in the {@code expression} rule.
 *
 * <p>The {@link #left()} method supports the "without left" pattern: several
 * grammar alternatives (e.g., {@code OrWithoutLeftExpr}, {@code EqualityWithoutLeftExpr})
 * represent partial expressions that omit the left operand. For these nodes,
 * {@code left()} returns {@link Optional#empty()}, and the enclosing expression
 * tree reconstructs the full form via the {@code then} combinator.
 *
 * <p>By default, {@code left()} returns {@link Optional#empty()}.
 * Subclasses that carry a left operand (e.g., {@link RBinaryExpression})
 * override this.
 */
public abstract class RExpression extends RNode {

    /**
     * Returns the left-hand operand of this expression, if present.
     *
     * <p>Returns {@link Optional#empty()} by default. Binary and "with left"
     * expressions override this to return their left operand.
     *
     * @return the left operand, or empty for "without left" forms and leaf expressions
     */
    public Optional<RExpression> left() {
        return Optional.empty();
    }

    /**
     * Accepts a visitor, dispatching to the appropriate visit method.
     *
     * @param visitor the expression visitor
     * @param context arbitrary context value passed through to the visitor
     * @param <R> return type
     * @param <C> context type
     * @return the result of the visitor's visit method
     */
    public abstract <R, C> R accept(RExpressionVisitor<R, C> visitor, C context);
}
