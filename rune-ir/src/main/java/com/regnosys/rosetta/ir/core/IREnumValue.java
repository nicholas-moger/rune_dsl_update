package com.regnosys.rosetta.ir.core;

import java.util.List;
import java.util.Optional;

/**
 * A value within an {@link IREnum}. Extends {@link IRNode} for graph
 * uniformity (per spec Section 2 reviewer fix I1).
 *
 * <p>{@link #kind()} returns {@link IRKind#ENUM_VALUE}.
 */
public interface IREnumValue extends IRNode {
    /**
     * Optional human-readable display label, if declared in source.
     *
     * <p>Returned as a single locale-naive string. Localisation is out of
     * scope for V1; M9 Python target may introduce locale-aware accessors
     * via additive default-method extension if needed.
     */
    Optional<String> displayName();

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

    /** The {@code [synonym …]} uses written on this value, in source order. */
    default List<IREnumSynonym> synonyms() {
        return List.of();
    }
}
