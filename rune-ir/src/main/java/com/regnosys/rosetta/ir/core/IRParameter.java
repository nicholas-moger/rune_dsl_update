package com.regnosys.rosetta.ir.core;

/**
 * A parameter of an {@link IRFunction}. Extends {@link IRNode} for graph
 * uniformity (per spec Section 2 reviewer fix I1).
 *
 * <p>{@link #kind()} returns {@link IRKind#PARAMETER}.
 */
public interface IRParameter extends IRNode {
    /** The declared parameter type. Non-null. */
    IRType type();

    /**
     * The cardinality of this parameter (e.g. {@link Cardinality#ZERO_TO_ONE},
     * {@link Cardinality#ONE_TO_MANY}). Non-null.
     */
    Cardinality cardinality();
}
