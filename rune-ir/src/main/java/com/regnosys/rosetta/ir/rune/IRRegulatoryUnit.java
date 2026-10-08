package com.regnosys.rosetta.ir.rune;

import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRNode;

import java.util.Optional;

/**
 * Regulatory unit covering BODY → CORPUS → SEGMENT_DEF. Rune-specific.
 *
 * <p><b>Hierarchy invariant</b> (per spec Section 2 reviewer fix S2):
 * <ul>
 *   <li>A {@link UnitKind#SEGMENT_DEF} unit's {@link #parent()} must be a
 *       {@link UnitKind#CORPUS}.
 *   <li>A {@link UnitKind#CORPUS} unit's {@link #parent()} must be a
 *       {@link UnitKind#BODY}.
 *   <li>A {@link UnitKind#BODY} unit's {@link #parent()} must be empty.
 * </ul>
 * Validation in P2 adapter; P1.4.3 only documents the invariant.
 *
 * <p>{@link IRNode#kind()} returns one of {@link IRKind#REGULATORY_BODY},
 * {@link IRKind#REGULATORY_CORPUS}, or {@link IRKind#REGULATORY_SEGMENT_DEF}
 * per {@link #unitKind()}.
 */
public interface IRRegulatoryUnit extends IRNode {
    /** Closed enum sub-discriminator within {@link IRRegulatoryUnit}. */
    enum UnitKind { BODY, CORPUS, SEGMENT_DEF }

    /** The unit-kind discriminator. Non-null. */
    UnitKind unitKind();

    /**
     * Parent in the BODY → CORPUS → SEGMENT_DEF chain. Empty for
     * {@link UnitKind#BODY}.
     */
    Optional<IRRegulatoryUnit> parent();
}
