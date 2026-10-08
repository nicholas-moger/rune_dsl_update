package com.regnosys.rosetta.ast.functions;

import com.regnosys.rosetta.ast.RDefinable;
import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;

import java.util.List;
import java.util.Optional;

/**
 * Shortcut (alias) declaration node in a function body, corresponding to the
 * {@code shortcutDeclaration} grammar rule.
 *
 * <p>Represents a named alias for an expression within a function, such as
 * {@code alias myAlias <"description">: someExpression}.
 *
 * <p>Grammar:
 * <pre>
 * shortcutDeclaration:
 *     ALIAS validID definable? COLON expression
 * ;
 * </pre>
 */
public class RShortcut extends RNode implements RDefinable {

    private String name;
    private String definition;
    private RExpression expression;

    // -- name -----------------------------------------------------------------

    public String name() {
        return name;
    }

    public void setName(String name) {
        checkMutable();
        this.name = name;
    }

    // -- definition (RDefinable) ----------------------------------------------

    @Override
    public Optional<String> definition() {
        return Optional.ofNullable(definition);
    }

    public void setDefinition(String definition) {
        checkMutable();
        this.definition = definition;
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
        if (expression != null) {
            return List.of(expression);
        }
        return List.of();
    }
}
