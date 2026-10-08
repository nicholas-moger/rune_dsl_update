package com.regnosys.rosetta.ast.expressions.unary;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.expressions.supporting.RWithMetaEntry;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * With-meta expression node, corresponding to the {@code WithMetaExpr}
 * grammar alternative.
 *
 * <p>Represents a {@code with-meta} expression that associates metadata
 * entries with the argument expression.
 */
public class RWithMetaExpr extends RExpression {

    private RExpression argument;
    private final List<RWithMetaEntry> entries = new ArrayList<>();

    // -- argument (the expression being annotated with metadata) ---------------

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

    // -- entries --------------------------------------------------------------

    public List<RWithMetaEntry> entries() {
        return entries;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        if (argument != null) {
            result.add(argument);
        }
        result.addAll(entries);
        return List.copyOf(result);
    }

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitWithMeta(this, context);
    }
}
