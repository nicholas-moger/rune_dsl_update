package com.regnosys.rosetta.ast.model;

import com.regnosys.rosetta.ast.RDefinable;
import com.regnosys.rosetta.ast.RNode;

import java.util.Optional;

/**
 * Scope declaration node, corresponding to the {@code rosettaScope} grammar rule.
 *
 * <p>Grammar:
 * <pre>
 * rosettaScope:
 *     SCOPE validID definable?
 * ;
 * </pre>
 */
public class RScope extends RNode implements RDefinable {

    private String name;
    private String definition;

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
}
