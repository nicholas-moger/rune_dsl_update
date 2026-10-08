package com.regnosys.rosetta.ir.core;

import java.util.Optional;

/**
 * A rule (reporting rule, eligibility rule, etc.). {@link #kind()} returns
 * {@link IRKind#RULE}.
 *
 * <p>Expression resolution is deferred to P2 (no concrete IR adapter in
 * P1.4.3). {@link #hasExpression()} is forward-state metadata only — V1
 * implementations may return {@code false} uniformly until the P2 adapter
 * wires up.
 */
public interface IRRule extends IRNode {
    /** The input type the rule applies to. Empty for input-less rules. */
    Optional<IRType> inputType();

    /** The output type the rule produces. Empty for void rules. */
    Optional<IRType> outputType();

    /** True if the rule declaration includes an expression block. */
    boolean hasExpression();
}
