package com.regnosys.rosetta.ir.core;

import java.util.List;
import java.util.Optional;

/**
 * A function or library function. {@link #kind()} returns either
 * {@link IRKind#FUNCTION} (RFunction) or {@link IRKind#LIBRARY_FUNCTION}
 * (RLibraryFunction); use {@link #isLibrary()} as a convenience check.
 *
 * <p>Body resolution is deferred to P2 (no concrete IR adapter in P1.4.3).
 * {@link #hasBody()} is forward-state metadata only — V1 implementations
 * may return {@code false} uniformly until the P2 adapter wires up.
 */
public interface IRFunction extends IRNode {
    /**
     * Declared parameters, in source-declaration order.
     * Non-null; unmodifiable.
     */
    List<IRParameter> parameters();

    /** Return type, if declared. Empty for void-returning functions. */
    Optional<IRType> returnType();

    /** True for {@code RLibraryFunction}, false for {@code RFunction}. */
    boolean isLibrary();

    /** True if the function declaration includes a body block. */
    boolean hasBody();
}
