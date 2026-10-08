package com.regnosys.rosetta.ir.core;

import java.util.List;
import java.util.Optional;

/**
 * An enumeration. {@link #kind()} returns {@link IRKind#ENUM}.
 */
public interface IREnum extends IRNode {
    /**
     * Declared enum values, in source-declaration order. Non-null;
     * unmodifiable. Empty list permitted for vacuous enums (rare).
     */
    List<IREnumValue> values();

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
     * The enumeration this one {@code extends}, as a REFERENCE ({@link IRKind#ENUM}, no values) carrying
     * the parent's namespace. {@link #values()} holds the locally-declared values only; a backend that
     * flattens inherited values walks this reference.
     */
    default Optional<IRType> parent() {
        return Optional.empty();
    }

    /** The declaring model's namespace ({@link #name()} is {@code namespace + "." + simple name}). */
    default Optional<String> namespace() {
        return Optional.empty();
    }
}
