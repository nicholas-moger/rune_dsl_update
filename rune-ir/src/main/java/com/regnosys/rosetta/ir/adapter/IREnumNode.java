package com.regnosys.rosetta.ir.adapter;

import com.regnosys.rosetta.ir.core.IRAnnotationUse;
import com.regnosys.rosetta.ir.core.IRDocReference;
import com.regnosys.rosetta.ir.core.IREnum;
import com.regnosys.rosetta.ir.core.IREnumValue;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRNode;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.core.Metadata;
import com.regnosys.rosetta.ir.core.SourceRange;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable {@link IREnum}. {@link #kind()} is always {@link IRKind#ENUM};
 * {@link #children()} surfaces {@link #values()} for IR graph uniformity.
 *
 * <p>Values are the LOCALLY-DECLARED values in source-declaration order; an enum that {@code extends}
 * another states it through {@link #parent()} (a reference carrying the parent's namespace) and a backend
 * that flattens inherited values walks that reference — the adapter does not flatten.
 *
 * <p>The declaration-IR enrichment (decision D55) added the components after {@code metadata} as PROPER
 * NAMED FIELDS; the four-component constructor is kept (every new fact at its empty default).
 * Lab-authored Phase-1 IR adapter (decision L-004).
 */
public record IREnumNode(String name, List<IREnumValue> values,
                         Optional<SourceRange> sourceRange, Metadata metadata,
                         Optional<String> namespace, Optional<IRType> parent,
                         Optional<String> definition, List<IRDocReference> docReferences,
                         List<IRAnnotationUse> annotations)
        implements IREnum {

    public IREnumNode {
        values = List.copyOf(values);
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(definition, "definition");
        docReferences = List.copyOf(docReferences);
        annotations = List.copyOf(annotations);
    }

    /** The pre-enrichment arity: every declaration fact of D55 at its empty default. */
    public IREnumNode(String name, List<IREnumValue> values,
                      Optional<SourceRange> sourceRange, Metadata metadata) {
        this(name, values, sourceRange, metadata, Optional.empty(), Optional.empty(), Optional.empty(),
                List.of(), List.of());
    }

    @Override
    public IRKind kind() {
        return IRKind.ENUM;
    }

    @Override
    public List<? extends IRNode> children() {
        return values;
    }
}
