package com.regnosys.rosetta.ast.expressions.binary;

import com.regnosys.rosetta.ast.RBinaryExpression;
import com.regnosys.rosetta.ast.enums.LogOp;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

/**
 * Logical binary expression node, corresponding to the
 * {@code AndExpr} and {@code OrExpr} grammar alternatives.
 *
 * <p>Represents logical operations: AND and OR.
 *
 * <p>EMF equivalent: {@code LogicalOperation}.
 */
public class RLogicalExpr extends RBinaryExpression {

    private LogOp op;

    // -- op -------------------------------------------------------------------

    public LogOp op() {
        return op;
    }

    public void setOp(LogOp op) {
        checkMutable();
        this.op = op;
    }

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitLogical(this, context);
    }
}
