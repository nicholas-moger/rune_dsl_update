package com.regnosys.rosetta.generator.java.object;

import com.regnosys.rosetta.generator.java.types.JavaPojoInterface;
import com.regnosys.rosetta.generator.java.types.JavaPojoProperty;

/**
 * Determines whether a property should be pruned (removed when empty)
 * during builder pruning, and whether it may be empty (for hasData checks).
 *
 * <p>Upstream uses complex analysis of type structure. Our default
 * implementation follows the simple rule: model object properties
 * are always prunable, basic types are never prunable.
 */
public interface IShouldPrune {

    /**
     * Whether a property should be pruned (nullified) when it has no data.
     * Used in the prune() method generation.
     */
    boolean shouldBePruned(JavaPojoInterface type, JavaPojoProperty prop);

    /**
     * Whether a property may be empty (needs hasData() check).
     * Model object properties may be empty; basic types cannot.
     */
    boolean mayBeEmpty(JavaPojoInterface type, JavaPojoProperty prop);

    /**
     * Default implementation: model objects are always prunable/may-be-empty.
     */
    static IShouldPrune defaultInstance() {
        return new IShouldPrune() {
            @Override
            public boolean shouldBePruned(JavaPojoInterface type, JavaPojoProperty prop) {
                return true; // upstream default for model objects
            }

            @Override
            public boolean mayBeEmpty(JavaPojoInterface type, JavaPojoProperty prop) {
                return true; // model objects need hasData() check
            }
        };
    }
}
