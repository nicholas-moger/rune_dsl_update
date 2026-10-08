package com.regnosys.rosetta.ast.mapping;

import com.regnosys.rosetta.ast.RNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapping node, corresponding to the {@code rosettaMapping} grammar rule.
 *
 * <p>Contains a list of mapping instances (set-when / default-to clauses).
 *
 * <p>Grammar:
 * <pre>
 * rosettaMapping:
 *     LBRACKET instances+=rosettaMappingInstance
 *     (COMMA instances+=rosettaMappingInstance)*
 *     RBRACKET
 * ;
 * </pre>
 */
public class RMapping extends RNode {

    private final List<RMappingInstance> instances = new ArrayList<>();

    // -- instances ------------------------------------------------------------

    public List<RMappingInstance> instances() {
        return instances;
    }

    // -- children (for traversal) ---------------------------------------------

    /**
     * Returns an unmodifiable view of the mapping instances, per the
     * {@link RNode#children()} contract.
     */
    @Override
    public List<? extends RNode> children() {
        return List.copyOf(instances);
    }
}
