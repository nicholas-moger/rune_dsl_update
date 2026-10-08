package com.regnosys.rosetta.ast.mapping;

import com.regnosys.rosetta.ast.RNode;

import java.util.Collections;
import java.util.List;

/**
 * Map test for path existence, corresponding to
 * {@code rosettaMapPathValue EXISTS} in the grammar.
 */
public class RMapTestExists extends RMapTest {

    private RMapPathValue pathValue;

    // -- pathValue ------------------------------------------------------------

    public RMapPathValue pathValue() {
        return pathValue;
    }

    public void setPathValue(RMapPathValue pathValue) {
        checkMutable();
        this.pathValue = pathValue;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        if (pathValue != null) {
            return Collections.singletonList(pathValue);
        }
        return Collections.emptyList();
    }
}
