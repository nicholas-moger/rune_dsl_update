package com.regnosys.rosetta.ast.expressions.unary;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.enums.ExistenceOp;
import com.regnosys.rosetta.ast.enums.ExistsModifier;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

import java.util.List;
import java.util.Optional;

/**
 * Existence check expression node, consolidating the {@code ExistsExpr}
 * and {@code AbsentExpr} grammar alternatives.
 *
 * <p>Represents existence checking: {@code expression exists} (with optional
 * modifier SINGLE/MULTIPLE) and {@code expression is absent}.
 *
 * <p>2-to-1 consolidation of EMF's separate exists and absent expression types.
 */
public class RExistenceExpr extends RExpression {

    private RExpression argument;
    private ExistenceOp op;
    private ExistsModifier modifier;

    // -- argument (the expression being checked) ------------------------------

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

    public ExistenceOp op() {
        return op;
    }

    public void setOp(ExistenceOp op) {
        checkMutable();
        this.op = op;
    }

    // -- modifier (optional, only for EXISTS) ---------------------------------

    public Optional<ExistsModifier> modifier() {
        return Optional.ofNullable(modifier);
    }

    public void setModifier(ExistsModifier modifier) {
        checkMutable();
        this.modifier = modifier;
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
        return visitor.visitExistence(this, context);
    }
}
