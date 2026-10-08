package com.regnosys.rosetta.ir.adapter;

import com.regnosys.rosetta.ir.core.Cardinality;
import com.regnosys.rosetta.ir.core.IRAnnotationUse;
import com.regnosys.rosetta.ir.core.IRBounds;
import com.regnosys.rosetta.ir.core.IRDocReference;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRLabel;
import com.regnosys.rosetta.ir.core.IRNode;
import com.regnosys.rosetta.ir.core.IRRuleReference;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.core.IRTypeArgument;
import com.regnosys.rosetta.ir.core.Metadata;
import com.regnosys.rosetta.ir.core.SourceRange;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable {@link IRField} (a field of a STRUCT/CHOICE {@link IRType}).
 * {@link #kind()} is always {@link IRKind#FIELD}; a field is a leaf in the IR
 * graph, so {@link #children()} is empty and {@link #type()} is a type
 * <em>reference</em> ({@link IRTypeNode} with empty fields) rather than the
 * expanded target type.
 *
 * <p>The declaration-IR enrichment (decision D55) added the components after {@code metadata} as PROPER
 * NAMED FIELDS — the exact bounds, {@code override}, the type arguments, the documentation, the doc
 * references, the annotations, the labels and the rule references. The five-component constructor is
 * kept (every new fact at its empty default), so a site that predates the enrichment compiles unchanged.
 *
 * <p>Lab-authored Phase-1 IR adapter (decision L-004).
 */
public record IRFieldNode(String name, IRType type, Cardinality cardinality,
                          Optional<SourceRange> sourceRange, Metadata metadata,
                          Optional<IRBounds> bounds, boolean isOverride, List<IRTypeArgument> typeArguments,
                          Optional<String> definition, List<IRDocReference> docReferences,
                          List<IRAnnotationUse> annotations, List<IRLabel> labels,
                          List<IRRuleReference> ruleReferences)
        implements IRField {

    public IRFieldNode {
        Objects.requireNonNull(bounds, "bounds");
        typeArguments = List.copyOf(typeArguments);
        Objects.requireNonNull(definition, "definition");
        docReferences = List.copyOf(docReferences);
        annotations = List.copyOf(annotations);
        labels = List.copyOf(labels);
        ruleReferences = List.copyOf(ruleReferences);
    }

    /** The pre-enrichment arity: every declaration fact of D55 at its empty default. */
    public IRFieldNode(String name, IRType type, Cardinality cardinality,
                       Optional<SourceRange> sourceRange, Metadata metadata) {
        this(name, type, cardinality, sourceRange, metadata, Optional.empty(), false, List.of(),
                Optional.empty(), List.of(), List.of(), List.of(), List.of());
    }

    @Override
    public IRKind kind() {
        return IRKind.FIELD;
    }

    @Override
    public List<? extends IRNode> children() {
        return List.of();
    }
}
