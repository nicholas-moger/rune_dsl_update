package com.regnosys.rosetta.ast.mapping;

import com.regnosys.rosetta.ast.RNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Standalone set-to mapping form, corresponding to the
 * {@code rosettaMappingSetTo} grammar rule.
 *
 * <p>Used by the SET_TO alternative of {@code RSynonymBody}.
 *
 * <p>Grammar:
 * <pre>
 * rosettaMappingSetTo:
 *     instances+=rosettaMappingSetToInstance
 *     (COMMA instances+=rosettaMappingSetToInstance)*
 * ;
 * </pre>
 */
public class RMappingSetTo extends RNode {

    private final List<RMappingSetToInstance> instances = new ArrayList<>();

    // -- instances ------------------------------------------------------------

    public List<RMappingSetToInstance> instances() {
        return instances;
    }

    // -- children (for traversal) ---------------------------------------------

    /**
     * Returns an unmodifiable view of the set-to instances, per the
     * {@link RNode#children()} contract.
     */
    @Override
    public List<? extends RNode> children() {
        return List.copyOf(instances);
    }
}
