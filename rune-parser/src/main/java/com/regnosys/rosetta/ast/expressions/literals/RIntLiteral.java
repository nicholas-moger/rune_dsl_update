package com.regnosys.rosetta.ast.expressions.literals;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

import java.math.BigInteger;

/**
 * Integer literal expression node, corresponding to the
 * {@code IntLiteralExpr} grammar alternative.
 *
 * <p>Represents an integer literal in the Rune DSL expression grammar.
 * The grammar's {@code INT_LITERAL} token has no upper bound, so the value
 * is stored as a {@link BigInteger} to handle arbitrary-precision integers
 * (real-world Rune DSL files contain literals like
 * {@code 9999999999999999999999999} that exceed {@code long} range).
 *
 * <p>EMF equivalent: {@code RosettaIntLiteral}.
 */
public class RIntLiteral extends RExpression {

    private BigInteger value;

    // -- value ----------------------------------------------------------------

    public BigInteger value() {
        return value;
    }

    public void setValue(BigInteger value) {
        checkMutable();
        this.value = value;
    }

    /**
     * Convenience setter accepting a Java {@code int}. Equivalent to calling
     * {@link #setValue(BigInteger)} with {@code BigInteger.valueOf(intValue)}.
     */
    public void setValue(int intValue) {
        checkMutable();
        this.value = BigInteger.valueOf(intValue);
    }

    /**
     * Convenience setter accepting a Java {@code long}. Equivalent to calling
     * {@link #setValue(BigInteger)} with {@code BigInteger.valueOf(longValue)}.
     */
    public void setValue(long longValue) {
        checkMutable();
        this.value = BigInteger.valueOf(longValue);
    }

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitIntLiteral(this, context);
    }
}
