package com.regnosys.rosetta.ast.mapping;

import com.regnosys.rosetta.ast.RNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Set-to mapping instance node, corresponding to the
 * {@code rosettaMappingSetToInstance} grammar rule.
 *
 * <p>Represents a single set-to value with an optional when clause.
 *
 * <p>Grammar:
 * <pre>
 * rosettaMappingSetToInstance:
 *     value=rosettaMapPrimaryExpression
 *     (WHEN when=rosettaMappingPathTests)?
 * ;
 * </pre>
 */
public class RMappingSetToInstance extends RNode {

    private RMapPrimaryExpression value;
    private RMappingPathTests when;

    // -- value ----------------------------------------------------------------

    public RMapPrimaryExpression value() {
        return value;
    }

    public void setValue(RMapPrimaryExpression value) {
        checkMutable();
        this.value = value;
    }

    // -- when -----------------------------------------------------------------

    public Optional<RMappingPathTests> when() {
        return Optional.ofNullable(when);
    }

    public void setWhen(RMappingPathTests when) {
        checkMutable();
        this.when = when;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        if (value != null) {
            result.add(value);
        }
        if (when != null) {
            result.add(when);
        }
        return List.copyOf(result);
    }
}
