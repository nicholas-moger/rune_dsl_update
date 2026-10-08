package com.regnosys.rosetta.ir.core;

import java.util.List;
import java.util.Optional;

/**
 * Root IR (Intermediate Representation) interface. Open (not sealed) to
 * permit external implementations such as cross-DSL importers (W22).
 *
 * <p>Discrimination is via the closed {@link IRKind} enum at {@link #kind()};
 * exhaustive {@code switch} on that enum is supported. The interface itself
 * is open.
 *
 * <p><b>Additive extension contract:</b> this interface MAY grow via Java
 * default-method extension (additive, BC-safe under japicmp). Downstream
 * implementors override as needed. Removal or signature change of any
 * method requires a new D-entry per D21 honest-limitation #2.
 *
 * <p><b>Forward path (deferred to P2 per Q2=C):</b> graph-shape APIs such
 * as dependency-edge accessors (W1) and structural-diff support (W16) land
 * via additive default methods on this interface once the first IR consumer
 * informs the right edge shape.
 *
 * <p><b>AST↔IR coherence (forward, P2 commitment per spec Section 8
 * reviewer fix C4):</b> Once P2 adapters wire, AST and IR derived from the
 * same parse must satisfy a structural-equivalence relation R defined in
 * P2-rebaseline. Deviation requires a D-entry. Three-way Layer-3 elevation
 * per D21 depends on R.
 */
public interface IRNode {
    /** Qualified name (namespace + simple name). Non-null. */
    String name();

    /** Closed enum discriminator. Non-null. */
    IRKind kind();

    /**
     * Optional source range; empty for non-source-backed importers
     * (e.g. an importer without source offsets).
     */
    Optional<SourceRange> sourceRange();

    /** Tree walk; unmodifiable. Non-null (returns empty list when leaf). */
    List<? extends IRNode> children();

    /** Metadata bag (W31). Non-null. */
    Metadata metadata();
}
