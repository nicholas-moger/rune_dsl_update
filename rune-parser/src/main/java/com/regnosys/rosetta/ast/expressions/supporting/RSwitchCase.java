package com.regnosys.rosetta.ast.expressions.supporting;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Switch case node, corresponding to the {@code switchCaseOrDefault}
 * grammar rule.
 *
 * <p>Represents a single case in a switch expression. Either a guarded case
 * (with a {@link RSwitchCaseGuard}) or a default case (where
 * {@link #isDefault()} returns {@code true}).
 *
 * <p>Grammar:
 * <pre>
 * switchCaseOrDefault:
 *     switchCaseGuard THEN expression   // guarded case
 *   | DEFAULT THEN expression           // default case
 * ;
 * </pre>
 */
public class RSwitchCase extends RNode {

    private boolean isDefault;
    private RSwitchCaseGuard guard;
    private RExpression expression;

    // -- isDefault ------------------------------------------------------------

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        checkMutable();
        this.isDefault = isDefault;
    }

    // -- guard (optional, absent when isDefault) ------------------------------

    public Optional<RSwitchCaseGuard> guard() {
        return Optional.ofNullable(guard);
    }

    public void setGuard(RSwitchCaseGuard guard) {
        checkMutable();
        this.guard = guard;
    }

    // -- expression -----------------------------------------------------------

    public RExpression expression() {
        return expression;
    }

    public void setExpression(RExpression expression) {
        checkMutable();
        this.expression = expression;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        if (guard != null) {
            result.add(guard);
        }
        if (expression != null) {
            result.add(expression);
        }
        return List.copyOf(result);
    }
}
