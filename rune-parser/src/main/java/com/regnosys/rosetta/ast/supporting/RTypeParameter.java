package com.regnosys.rosetta.ast.supporting;

import com.regnosys.rosetta.ast.RDefinable;
import com.regnosys.rosetta.ast.RNode;

import java.util.List;
import java.util.Optional;

/**
 * Type parameter node, corresponding to the {@code typeParameter} grammar rule.
 *
 * <p>Grammar:
 * <pre>
 * typeParameter:
 *     typeParameterValidID typeCall definable?
 * ;
 * </pre>
 */
public class RTypeParameter extends RNode implements RDefinable {

    private String name;
    private RTypeCall typeCall;
    private String definition;

    // -- name -----------------------------------------------------------------

    public String name() {
        return name;
    }

    public void setName(String name) {
        checkMutable();
        this.name = name;
    }

    // -- typeCall --------------------------------------------------------------

    public RTypeCall typeCall() {
        return typeCall;
    }

    public void setTypeCall(RTypeCall typeCall) {
        checkMutable();
        this.typeCall = typeCall;
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

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        if (typeCall != null) {
            return List.of(typeCall);
        }
        return List.of();
    }
}
