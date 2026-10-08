package com.regnosys.rosetta.ir.adapter;

import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRNode;
import com.regnosys.rosetta.ir.core.Metadata;
import com.regnosys.rosetta.ir.core.SourceRange;

import java.util.List;
import java.util.Optional;

/**
 * Generic immutable {@link IRNode} fallback for the RRootElement kinds that
 * Phase-1 constructs for completeness but does not emit (FUNCTION,
 * LIBRARY_FUNCTION, RULE, REPORT, ANNOTATION_DECL, the synonym/external/
 * regulatory kinds, and the built-in TYPE_ALIAS/BASIC_TYPE/RECORD_TYPE/
 * META_TYPE where a richer node is unnecessary).
 *
 * <p>Constructing a node for <em>every</em> kind keeps the AST→IR adapter
 * exhaustive over all 18 top-level RRootElement subclasses, so the IR
 * generalises rather than overfitting the visible POJO/ENUM slice. Lab-authored
 * Phase-1 IR adapter (decision L-004).
 */
public record IRNodeImpl(String name, IRKind kind, List<? extends IRNode> children,
                         Optional<SourceRange> sourceRange, Metadata metadata)
        implements IRNode {

    public IRNodeImpl {
        children = List.copyOf(children);
    }
}
