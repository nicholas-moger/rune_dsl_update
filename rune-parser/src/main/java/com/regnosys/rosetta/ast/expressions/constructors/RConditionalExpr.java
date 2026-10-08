package com.regnosys.rosetta.ast.expressions.constructors;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Conditional (if-then-else) expression node, corresponding to the
 * {@code ConditionalExpr} grammar alternative.
 *
 * <p>Represents a conditional expression: {@code if condition then thenBranch
 * else elseBranch}. The else branch is optional.
 */
public class RConditionalExpr extends RExpression {

    private RExpression condition;
    private RExpression thenBranch;
    private RExpression elseBranch;

    // -- condition ------------------------------------------------------------

    public RExpression condition() {
        return condition;
    }

    public void setCondition(RExpression condition) {
        checkMutable();
        this.condition = condition;
    }

    // -- thenBranch -----------------------------------------------------------

    public RExpression thenBranch() {
        return thenBranch;
    }

    public void setThenBranch(RExpression thenBranch) {
        checkMutable();
        this.thenBranch = thenBranch;
    }

    // -- elseBranch (optional) ------------------------------------------------

    public Optional<RExpression> elseBranch() {
        return Optional.ofNullable(elseBranch);
    }

    public void setElseBranch(RExpression elseBranch) {
        checkMutable();
        this.elseBranch = elseBranch;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        if (condition != null) {
            result.add(condition);
        }
        if (thenBranch != null) {
            result.add(thenBranch);
        }
        if (elseBranch != null) {
            result.add(elseBranch);
        }
        return List.copyOf(result);
    }

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitConditional(this, context);
    }
}
