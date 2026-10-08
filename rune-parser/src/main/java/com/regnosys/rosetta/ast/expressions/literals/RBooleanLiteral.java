package com.regnosys.rosetta.ast.expressions.literals;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

/**
 * Boolean literal expression node, corresponding to the
 * {@code BooleanLiteralExpr} grammar alternative.
 *
 * <p>Represents a boolean literal value ({@code True} or {@code False}) in the
 * Rune DSL expression grammar.
 *
 * <p>EMF equivalent: {@code RosettaBooleanLiteral}.
 */
public class RBooleanLiteral extends RExpression {

    private boolean value;

    // -- value ----------------------------------------------------------------

    public boolean value() {
        return value;
    }

    public void setValue(boolean value) {
        checkMutable();
        this.value = value;
    }

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitBooleanLiteral(this, context);
    }
}
