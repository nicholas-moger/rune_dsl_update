package com.regnosys.rosetta.ast.mapping;

import com.regnosys.rosetta.ast.RNode;

import java.util.ArrayList;
import java.util.List;

/**
 * AND-chained mapping path tests node, corresponding to the
 * {@code rosettaMappingPathTests} grammar rule.
 *
 * <p>Represents a conjunction of map tests such as
 * {@code "path1" = "val1" and "path2" exists}.
 *
 * <p>Grammar:
 * <pre>
 * rosettaMappingPathTests:
 *     tests+=rosettaMapTest (AND tests+=rosettaMapTest)*
 * ;
 * </pre>
 */
public class RMappingPathTests extends RNode {

    private final List<RMapTest> tests = new ArrayList<>();

    // -- tests ----------------------------------------------------------------

    public List<RMapTest> tests() {
        return tests;
    }

    // -- children (for traversal) ---------------------------------------------

    /**
     * Returns an unmodifiable view of the chained map tests, per the
     * {@link RNode#children()} contract.
     */
    @Override
    public List<? extends RNode> children() {
        return List.copyOf(tests);
    }
}
