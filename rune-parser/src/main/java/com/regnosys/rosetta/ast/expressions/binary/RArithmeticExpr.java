package com.regnosys.rosetta.ast.expressions.binary;

import com.regnosys.rosetta.ast.RBinaryExpression;
import com.regnosys.rosetta.ast.enums.ArithOp;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

/**
 * Arithmetic binary expression node, corresponding to the
 * {@code AdditiveExpr} and {@code MultiplicativeExpr} grammar alternatives.
 *
 * <p>Represents arithmetic operations: addition (+), subtraction (-),
 * multiplication (*), and division (/).
 *
 * <p>EMF equivalent: {@code ArithmeticOperation}.
 */
public class RArithmeticExpr extends RBinaryExpression {

    private ArithOp op;

    // -- op -------------------------------------------------------------------

    public ArithOp op() {
        return op;
    }

    public void setOp(ArithOp op) {
        checkMutable();
        this.op = op;
    }

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitArithmetic(this, context);
    }
}
