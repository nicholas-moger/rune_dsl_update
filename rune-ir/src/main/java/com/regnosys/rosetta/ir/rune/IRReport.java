package com.regnosys.rosetta.ir.rune;

import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRNode;
import com.regnosys.rosetta.ir.core.IRRule;
import com.regnosys.rosetta.ir.core.IRType;

import java.util.List;
import java.util.Optional;

/**
 * Regulatory report declaration. Rune-specific; not part of the DSL-agnostic
 * core. {@link IRNode#kind()} returns {@link IRKind#REPORT}.
 */
public interface IRReport extends IRNode {
    /** The regulatory unit this report attaches to (typically a Body or Corpus). */
    Optional<IRRegulatoryUnit> regulatoryUnit();

    /**
     * Rules that contribute to this report, in source-declaration order.
     * Non-null; unmodifiable.
     */
    List<IRRule> rules();

    /** Output type the report produces. Empty for reports without explicit output type. */
    Optional<IRType> outputType();
}
