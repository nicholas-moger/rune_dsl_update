package com.regnosys.rosetta.ast.expressions.unary;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCase;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Switch expression node, corresponding to the {@code SwitchExpr}
 * grammar alternative.
 *
 * <p>Represents a switch expression that matches the argument against
 * a set of cases, each with a guard condition and a result expression.
 */
public class RSwitchExpr extends RExpression {

    private RExpression argument;
    private final List<RSwitchCase> cases = new ArrayList<>();

    // -- argument (the expression being switched on) --------------------------

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

    // -- cases ----------------------------------------------------------------

    public List<RSwitchCase> cases() {
        return cases;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        if (argument != null) {
            result.add(argument);
        }
        result.addAll(cases);
        return List.copyOf(result);
    }

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitSwitch(this, context);
    }
}
