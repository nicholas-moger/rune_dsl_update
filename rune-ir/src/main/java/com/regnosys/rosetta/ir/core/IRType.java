package com.regnosys.rosetta.ir.core;

import java.util.List;
import java.util.Optional;

/**
 * A type-like IR node. Covers all 6 RRootElement type kinds via the
 * {@link IRKind} discriminator: {@link IRKind#STRUCT}, {@link IRKind#CHOICE},
 * {@link IRKind#TYPE_ALIAS}, {@link IRKind#BASIC_TYPE},
 * {@link IRKind#RECORD_TYPE}, {@link IRKind#META_TYPE}.
 *
 * <p>Generators that don't need to distinguish sub-kinds pattern-match on
 * {@code IRType} alone; generators that do (e.g. CHOICE-aware vs STRUCT-aware
 * codegen) switch on {@link #kind()}.
 */
public interface IRType extends IRNode {
    /**
     * Declared fields of this type, in source-declaration order. Empty for
     * {@link IRKind#BASIC_TYPE}, {@link IRKind#RECORD_TYPE},
     * {@link IRKind#META_TYPE}, and {@link IRKind#TYPE_ALIAS}.
     * Non-null; unmodifiable.
     */
    List<IRField> fields();

    /** Inheritance / type-alias target. Empty for non-derived types. */
    Optional<IRType> baseType();

    /** True if this type is declared abstract (cannot be instantiated directly). */
    boolean isAbstract();

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
     * The namespace this type lives in. For a DECLARATION it is the declaring model's namespace
     * ({@link #name()} is then {@code namespace + "." + simple name}); for a REFERENCE (a field's type, a
     * base type) it is the namespace of the declaration the reference RESOLVES to, while {@link #name()}
     * stays the text the model wrote. Empty for an unresolved reference and for a built-in the model
     * library declares in no namespace.
     */
    default Optional<String> namespace() {
        return Optional.empty();
    }

    /**
     * The resolved declaration's own simple name, for a REFERENCE — it differs from {@link #name()} when
     * the model wrote a qualified or import-aliased name. Empty for an unresolved reference.
     */
    default Optional<String> resolvedName() {
        return Optional.empty();
    }

    /**
     * {@code namespace.resolvedName} of a RESOLVED REFERENCE. Empty for an unresolved reference, for a name only the
     * builtin fallback places (it has a resolved name and no namespace) and for a DECLARATION node, which carries no
     * resolved name - a declaration's own qualified name is {@link #name()}.
     */
    default Optional<String> resolvedQualifiedName() {
        return namespace().flatMap(ns -> resolvedName().map(n -> ns.isEmpty() ? n : ns + "." + n));
    }

    /** The names of the conditions declared on this type, in source order; an unnamed condition reads empty. */
    default List<Optional<String>> conditionNames() {
        return List.of();
    }

    /**
     * On a {@link IRKind#TYPE_ALIAS} REFERENCE (a field's or a parameter's type, a type alias's own body): the alias chain
     * COLLAPSED at this use site - the declaration it ends on and the arguments in force after substitution
     * ({@link IREffectiveBase}). Empty on every other kind, and on an alias reference whose chain the adapter could not
     * collapse (a cycle, a body no declaration and no registry name answers) - the absence is the fact, and a backend
     * REFUSES to type such a reference. Additive since v3.3 seat 7 (PR #643, the type gate): an implementation that
     * predates it reads empty.
     */
    default Optional<IREffectiveBase> effectiveBase() {
        return Optional.empty();
    }

    /**
     * On a {@link IRKind#TYPE_ALIAS} DECLARATION: its declared parameters ({@code typeAlias int(digits int, ...)}), in
     * source order. Empty on every other kind and on a reference.
     */
    default List<IRTypeParameter> typeParameters() {
        return List.of();
    }

    /**
     * On a {@link IRKind#TYPE_ALIAS} DECLARATION: the arguments its body wrote on {@link #baseType()}, AS WRITTEN - a
     * literal, or one of the alias's own parameters passed through by name ({@link IRTypeArgument#nameValue()}). Empty on
     * every other kind (a data type's {@code extends} takes no argument) and on a reference.
     */
    default List<IRTypeArgument> baseTypeArguments() {
        return List.of();
    }

    /**
     * Index-parallel to {@link #conditionNames()}: the expression ROOT kind of each condition declared here, one of
     * {@link IRAliasLink#CONDITION_KINDS} ({@code OneOf} / {@code Choice} / {@code DataRule}) - the token the old
     * generator names an UNNAMED condition's class by ({@code ModelMetaGenerator.unnamedConditionKind}, the oracle).
     * The kind is stated for EVERY condition, named ones included: a fact stated only where it is ambiguous can never
     * be reconciled against the whole population, and a backend reads the same token for a named condition when it
     * decides a {@code one-of}-derived law (the deep-path family's eligibility is exactly an OWN {@code OneOf}).
     * EMPTY when the producer stated no kinds. Additive since v3.3 seat 8 (PR #644, the property gate): an
     * implementation that predates it reads empty.
     */
    default List<String> conditionKinds() {
        return List.of();
    }

    /**
     * On a {@link IRKind#TYPE_ALIAS} REFERENCE: the alias declarations this use site walks, OUTERMOST-FIRST - the alias
     * the reference names, then the alias its body names, and so on to the last alias before the leaf. Beside
     * {@link #effectiveBase()}, which COLLAPSES the same chain: the base is what a field's Java type needs, the RUNGS
     * are what the type-format validator needs (it wires every condition of every alias in the chain, and REFUSES the
     * whole validator file when a PARAMETERISED alias in the chain carries a condition). Empty on every other kind, on
     * a DECLARATION, and on a reference whose chain the adapter could not walk. The chain is WHAT WAS WALKED: a cycle
     * or an unresolvable body ends it at the last link, and an empty {@link #effectiveBase()} beside a non-empty chain
     * is how a failed collapse states itself. Additive since v3.3 seat 8 (PR #644, the property gate): an
     * implementation that predates it reads empty.
     */
    default List<IRAliasLink> aliasChain() {
        return List.of();
    }
}
