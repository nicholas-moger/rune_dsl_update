package com.regnosys.rosetta.ir.rune;

import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRNode;

import java.util.Optional;

/**
 * Source declaration covering legacy + external synonym sources + external
 * rule sources. Rune-specific.
 *
 * <p>{@link IRNode#kind()} returns one of {@link IRKind#SYNONYM_SOURCE},
 * {@link IRKind#EXTERNAL_SYNONYM_SOURCE}, or
 * {@link IRKind#EXTERNAL_RULE_SOURCE} per {@link #sourceKind()}.
 */
public interface IRSourceDecl extends IRNode {
    /** Closed enum sub-discriminator within {@link IRSourceDecl}. */
    enum SourceKind { SYNONYM_LEGACY, SYNONYM_EXTERNAL, RULE_EXTERNAL }

    /** The source-kind discriminator. Non-null. */
    SourceKind sourceKind();

    /** Parent source in {@code extends} chains. Empty for top-level. */
    Optional<IRSourceDecl> parent();
}
