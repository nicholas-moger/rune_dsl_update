package com.regnosys.rosetta.ast.expressions.binary;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Then expression node, corresponding to the {@code ThenExpr} grammar alternative.
 *
 * <p>Grammar: {@code expression THEN (inlineFunction | implicitInlineFunction)?}
 *
 * <p>Represents the {@code then} combinator that pipes a left-hand expression
 * (the {@code argument}) into an inline function {@code body}. Although
 * categorised under the {@code binary/} package for spec consistency with the
 * other postfix-style binary forms, the grammar has no separate right operand:
 * the body itself plays the role of the right side.
 *
 * <p>Extends {@link RExpression} (not {@code RBinaryExpression}) because there
 * is no second expression operand — only an argument and an optional body.
 *
 * <p>EMF equivalent: {@code ThenOperation}.
 */
public class RThenExpr extends RExpression {

    private RExpression argument;
    private RInlineFunction body;

    // -- argument (the left-hand expression piped into THEN) ------------------

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

    // -- body (optional) ------------------------------------------------------

    public Optional<RInlineFunction> body() {
        return Optional.ofNullable(body);
    }

    public void setBody(RInlineFunction body) {
        checkMutable();
        this.body = body;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        if (argument != null) {
            result.add(argument);
        }
        if (body != null) {
            result.add(body);
        }
        return List.copyOf(result);
    }

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitThen(this, context);
    }
}
