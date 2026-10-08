package com.regnosys.rosetta.ast.expressions.literals;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

import java.math.BigDecimal;

/**
 * Number literal expression node, corresponding to the
 * {@code NumberLiteralExpr} grammar alternative.
 *
 * <p>Represents a decimal number literal in the Rune DSL expression grammar.
 *
 * <p>EMF equivalent: {@code RosettaNumberLiteral}.
 */
public class RNumberLiteral extends RExpression {

    private BigDecimal value;

    // -- value ----------------------------------------------------------------

    public BigDecimal value() {
        return value;
    }

    public void setValue(BigDecimal value) {
        checkMutable();
        this.value = value;
    }

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitNumberLiteral(this, context);
    }
}
