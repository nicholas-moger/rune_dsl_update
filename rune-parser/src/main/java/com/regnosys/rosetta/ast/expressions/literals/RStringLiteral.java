package com.regnosys.rosetta.ast.expressions.literals;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

/**
 * String literal expression node, corresponding to the
 * {@code StringLiteralExpr} grammar alternative.
 *
 * <p>Represents a quoted string value in the Rune DSL expression grammar.
 *
 * <p>EMF equivalent: {@code RosettaStringLiteral}.
 */
public class RStringLiteral extends RExpression {

    private String value;

    // -- value ----------------------------------------------------------------

    public String value() {
        return value;
    }

    public void setValue(String value) {
        checkMutable();
        this.value = value;
    }

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitStringLiteral(this, context);
    }
}
