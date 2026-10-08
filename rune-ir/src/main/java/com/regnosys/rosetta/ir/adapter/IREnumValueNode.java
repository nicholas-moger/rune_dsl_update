package com.regnosys.rosetta.ir.adapter;

import com.regnosys.rosetta.ir.core.IRAnnotationUse;
import com.regnosys.rosetta.ir.core.IRDocReference;
import com.regnosys.rosetta.ir.core.IREnumSynonym;
import com.regnosys.rosetta.ir.core.IREnumValue;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRNode;
import com.regnosys.rosetta.ir.core.Metadata;
import com.regnosys.rosetta.ir.core.SourceRange;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable {@link IREnumValue}. {@link #kind()} is always
 * {@link IRKind#ENUM_VALUE}; a value is a leaf, so {@link #children()} is empty.
 *
 * <p>{@link #displayName()} maps 1:1 to the AST's declared display name —
 * present vs. empty is preserved exactly (never normalised), because Path-1
 * toggles both the {@code @RosettaEnumValue displayName=} clause and the
 * constructor argument on that distinction.
 *
 * <p>The declaration-IR enrichment (decision D55) added the components after {@code metadata} as PROPER
 * NAMED FIELDS; the four-component constructor is kept (every new fact at its empty default).
 * Lab-authored Phase-1 IR adapter (decision L-004).
 */
public record IREnumValueNode(String name, Optional<String> displayName,
                              Optional<SourceRange> sourceRange, Metadata metadata,
                              Optional<String> definition, List<IRDocReference> docReferences,
                              List<IRAnnotationUse> annotations, List<IREnumSynonym> synonyms)
        implements IREnumValue {

    public IREnumValueNode {
        Objects.requireNonNull(definition, "definition");
        docReferences = List.copyOf(docReferences);
        annotations = List.copyOf(annotations);
        synonyms = List.copyOf(synonyms);
    }

    /** The pre-enrichment arity: every declaration fact of D55 at its empty default. */
    public IREnumValueNode(String name, Optional<String> displayName,
                           Optional<SourceRange> sourceRange, Metadata metadata) {
        this(name, displayName, sourceRange, metadata, Optional.empty(), List.of(), List.of(), List.of());
    }

    @Override
    public IRKind kind() {
        return IRKind.ENUM_VALUE;
    }

    @Override
    public List<? extends IRNode> children() {
        return List.of();
    }
}
