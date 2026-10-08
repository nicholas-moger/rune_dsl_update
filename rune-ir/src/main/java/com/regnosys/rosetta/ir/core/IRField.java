package com.regnosys.rosetta.ir.core;

import java.util.List;
import java.util.Optional;

/**
 * A field of an {@link IRType}. Extends {@link IRNode} for graph
 * uniformity (per spec Section 2 reviewer fix I1) — every IR participant
 * is queryable through {@link IRNode#children()} and has a
 * {@link Metadata} bag.
 *
 * <p>{@link #kind()} returns {@link IRKind#FIELD}.
 */
public interface IRField extends IRNode {
    /** The declared field type. Non-null. */
    IRType type();

    /**
     * The cardinality of this field (e.g. {@link Cardinality#ZERO_TO_ONE},
     * {@link Cardinality#ONE_TO_MANY}). Non-null.
     */
    Cardinality cardinality();

    /**
     * The declaration's documentation text ({@code <"…">}), as the model wrote it. Empty when the model
     * wrote none. Additive since the declaration-IR enrichment (decision D55): an implementation that
     * predates it reads empty.
     */
    default Optional<String> definition() {
        return Optional.empty();
    }

    /** The {@code [docReference …]} / {@code [regulatoryReference …]} uses written here, in source order. */
    default List<IRDocReference> docReferences() {
        return List.of();
    }

    /** The annotations written here ({@code [metadata scheme]}, {@code [deprecated]}, …), in source order. */
    default List<IRAnnotationUse> annotations() {
        return List.of();
    }

    /**
     * The EXACT bounds the model declared ({@code (0..2)}, {@code (2..*)}) — what the four-bucket
     * {@link #cardinality()} cannot say. Empty when the model declares no cardinality (a choice option).
     */
    default Optional<IRBounds> bounds() {
        return Optional.empty();
    }

    /** True when the attribute is declared {@code override} — it re-declares an inherited attribute. */
    default boolean isOverride() {
        return false;
    }

    /** The {@code [label …]} annotations written here, in source order. */
    default List<IRLabel> labels() {
        return List.of();
    }

    /** The {@code [ruleReference …]} annotations written here, in source order. */
    default List<IRRuleReference> ruleReferences() {
        return List.of();
    }

    /** The arguments of a parameterised type reference ({@code string(maxLength: 35)}), in source order. */
    default List<IRTypeArgument> typeArguments() {
        return List.of();
    }
}
