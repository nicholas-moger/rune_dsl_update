package com.regnosys.rosetta.ast.expressions.unary;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.enums.ListOp;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

import java.util.List;
import java.util.Optional;

/**
 * List operation postfix expression node, consolidating 7 grammar
 * alternatives into one.
 *
 * <p>Represents list operations: {@code only-element}, {@code flatten},
 * {@code distinct}, {@code reverse}, {@code first}, {@code last}, and
 * {@code sum}.
 *
 * <p>7-to-1 consolidation of EMF's separate list expression types.
 */
public class RListOpExpr extends RExpression {

    private RExpression argument;
    private ListOp op;

    // -- argument (the list expression being operated on) ----------------------

    public RExpression argument() {
        return argument;
    }

    public void setArgument(RExpression argument) {
        checkMutable();
        this.argument = argument;
    }

    @Override
    public Optional<RExpression> left() {
        return Optional.ofNullable(argument);
    }

    // -- op -------------------------------------------------------------------

    public ListOp op() {
        return op;
    }

    public void setOp(ListOp op) {
        checkMutable();
        this.op = op;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        if (argument != null) {
            return List.of(argument);
        }
        return List.of();
    }

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitListOp(this, context);
    }
}
