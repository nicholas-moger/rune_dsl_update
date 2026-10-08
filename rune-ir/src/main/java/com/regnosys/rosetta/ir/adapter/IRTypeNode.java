package com.regnosys.rosetta.ir.adapter;

import com.regnosys.rosetta.ir.core.IRAliasLink;
import com.regnosys.rosetta.ir.core.IRAnnotationUse;
import com.regnosys.rosetta.ir.core.IRDocReference;
import com.regnosys.rosetta.ir.core.IREffectiveBase;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRNode;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.core.IRTypeArgument;
import com.regnosys.rosetta.ir.core.IRTypeParameter;
import com.regnosys.rosetta.ir.core.Metadata;
import com.regnosys.rosetta.ir.core.SourceRange;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable {@link IRType} covering the six type kinds (STRUCT, CHOICE,
 * TYPE_ALIAS, BASIC_TYPE, RECORD_TYPE, META_TYPE) via its {@code kind}
 * discriminator. Phase-1 emit-bearing uses are STRUCT (data) and CHOICE; since v3.3 seat 7 (PR #643, the type gate) a
 * TYPE_ALIAS declaration is adapted too.
 *
 * <p>Also doubles as a lightweight type <em>reference</em> for a field's or
 * parameter's declared type: construct with empty {@code fields}, empty
 * {@code baseType}, {@code isAbstract=false} — or, since the declaration-IR enrichment, through
 * {@link #reference}. This keeps the declaration IR acyclic (a field's {@link IRField#type()} is a
 * reference, not the fully expanded target type).
 *
 * <p>The declaration-IR enrichment (decision D55) added the components after {@code metadata} as PROPER
 * NAMED FIELDS: the namespace and — on a reference — the resolved declaration's own name, the
 * documentation, the doc references, the annotations and the condition names. The seven-component
 * constructor is kept (every new fact at its empty default), and so is the thirteen-component one.
 *
 * <p>The type gate (v3.3 seat 7, PR #643) added three more, after {@code conditionNames}: on a TYPE_ALIAS REFERENCE the
 * chain's {@link #effectiveBase()}; on a TYPE_ALIAS DECLARATION its {@link #typeParameters()} and the arguments its body
 * wrote, {@link #baseTypeArguments()}. The record ENFORCES that each is stated on a TYPE_ALIAS node only.
 *
 * <p>The property gate (v3.3 seat 8, PR #644) added the last two: {@link #conditionKinds()}, index-parallel to
 * {@code conditionNames} and stated for EVERY condition (the record refuses a half-stated list — the shared law
 * {@link IRAliasLink#checkConditionKinds}), and, on a TYPE_ALIAS REFERENCE, the {@link #aliasChain()} the use site
 * walks OUTERMOST-FIRST beside the {@code effectiveBase} that collapses it. The record ENFORCES the chain's
 * TYPE_ALIAS-only law exactly as it enforces the effective base's. The sixteen-component constructor of the type
 * gate is kept, so every caller that predates the property gate compiles unchanged and reads both facts empty.
 *
 * <p>{@link #children()} surfaces {@link #fields()} for IR graph uniformity.
 * Lab-authored Phase-1 IR adapter (decision L-004).
 */
public record IRTypeNode(String name, IRKind kind, List<IRField> fields,
                         Optional<IRType> baseType, boolean isAbstract,
                         Optional<SourceRange> sourceRange, Metadata metadata,
                         Optional<String> namespace, Optional<String> resolvedName,
                         Optional<String> definition, List<IRDocReference> docReferences,
                         List<IRAnnotationUse> annotations, List<Optional<String>> conditionNames,
                         Optional<IREffectiveBase> effectiveBase, List<IRTypeParameter> typeParameters,
                         List<IRTypeArgument> baseTypeArguments, List<String> conditionKinds,
                         List<IRAliasLink> aliasChain)
        implements IRType {

    public IRTypeNode {
        fields = List.copyOf(fields);
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(resolvedName, "resolvedName");
        Objects.requireNonNull(definition, "definition");
        docReferences = List.copyOf(docReferences);
        annotations = List.copyOf(annotations);
        conditionNames = List.copyOf(conditionNames);
        Objects.requireNonNull(effectiveBase, "effectiveBase");
        typeParameters = List.copyOf(typeParameters);
        baseTypeArguments = List.copyOf(baseTypeArguments);
        conditionKinds = List.copyOf(conditionKinds);
        aliasChain = List.copyOf(aliasChain);
        IRAliasLink.checkConditionKinds("type " + name, conditionNames, conditionKinds);
        if (kind != IRKind.TYPE_ALIAS) {
            if (effectiveBase.isPresent()) {
                throw new IllegalArgumentException("type " + name + " of kind " + kind
                        + " carries an effective base - only a TYPE_ALIAS reference collapses to one");
            }
            if (!typeParameters.isEmpty()) {
                throw new IllegalArgumentException("type " + name + " of kind " + kind
                        + " declares type parameters - only a TYPE_ALIAS declaration does");
            }
            if (!baseTypeArguments.isEmpty()) {
                throw new IllegalArgumentException("type " + name + " of kind " + kind
                        + " writes arguments on its base type - only a TYPE_ALIAS body does");
            }
            if (!aliasChain.isEmpty()) {
                throw new IllegalArgumentException("type " + name + " of kind " + kind
                        + " carries an alias chain - only a TYPE_ALIAS reference walks one");
            }
        }
    }

    /** The pre-enrichment arity: every declaration fact of D55 at its empty default. */
    public IRTypeNode(String name, IRKind kind, List<IRField> fields,
                      Optional<IRType> baseType, boolean isAbstract,
                      Optional<SourceRange> sourceRange, Metadata metadata) {
        this(name, kind, fields, baseType, isAbstract, sourceRange, metadata, Optional.empty(), Optional.empty(),
                Optional.empty(), List.of(), List.of(), List.of());
    }

    /** The D55 arity: every fact of the type gate (PR #643) at its empty default. */
    public IRTypeNode(String name, IRKind kind, List<IRField> fields,
                      Optional<IRType> baseType, boolean isAbstract,
                      Optional<SourceRange> sourceRange, Metadata metadata,
                      Optional<String> namespace, Optional<String> resolvedName,
                      Optional<String> definition, List<IRDocReference> docReferences,
                      List<IRAnnotationUse> annotations, List<Optional<String>> conditionNames) {
        this(name, kind, fields, baseType, isAbstract, sourceRange, metadata, namespace, resolvedName, definition,
                docReferences, annotations, conditionNames, Optional.empty(), List.of(), List.of());
    }

    /** The type-gate arity (PR #643): both facts of the property gate (PR #644) at their empty default. */
    public IRTypeNode(String name, IRKind kind, List<IRField> fields,
                      Optional<IRType> baseType, boolean isAbstract,
                      Optional<SourceRange> sourceRange, Metadata metadata,
                      Optional<String> namespace, Optional<String> resolvedName,
                      Optional<String> definition, List<IRDocReference> docReferences,
                      List<IRAnnotationUse> annotations, List<Optional<String>> conditionNames,
                      Optional<IREffectiveBase> effectiveBase, List<IRTypeParameter> typeParameters,
                      List<IRTypeArgument> baseTypeArguments) {
        this(name, kind, fields, baseType, isAbstract, sourceRange, metadata, namespace, resolvedName, definition,
                docReferences, annotations, conditionNames, effectiveBase, typeParameters, baseTypeArguments,
                List.of(), List.of());
    }

    /**
     * A type REFERENCE: the name the model wrote, the TRUE kind of the declaration it resolves to, and that
     * declaration's namespace and own simple name (both empty for an unresolved reference).
     */
    public static IRTypeNode reference(String writtenName, IRKind kind, Optional<String> namespace,
                                       Optional<String> resolvedName) {
        return reference(writtenName, kind, namespace, resolvedName, Optional.empty());
    }

    /**
     * A type REFERENCE that may carry its collapsed alias chain: the effective base is stated on a TYPE_ALIAS reference
     * whose chain the adapter collapsed, and empty otherwise (the record refuses it on any other kind). The walked
     * chain is left empty — use the six-argument overload to state both.
     */
    public static IRTypeNode reference(String writtenName, IRKind kind, Optional<String> namespace,
                                       Optional<String> resolvedName, Optional<IREffectiveBase> effectiveBase) {
        return reference(writtenName, kind, namespace, resolvedName, effectiveBase, List.of());
    }

    /**
     * A type REFERENCE stating BOTH halves of an alias chain (v3.3 seat 8, PR #644): the chain COLLAPSED
     * ({@code effectiveBase}, what a Java type is derived from) and the chain WALKED ({@code aliasChain}, the rungs
     * the type-format validator reads). Both are refused on a kind other than TYPE_ALIAS, and each is independently
     * empty — a chain the walk ended without a leaf carries its links and no base.
     */
    public static IRTypeNode reference(String writtenName, IRKind kind, Optional<String> namespace,
                                       Optional<String> resolvedName, Optional<IREffectiveBase> effectiveBase,
                                       List<IRAliasLink> aliasChain) {
        return new IRTypeNode(writtenName, kind, List.of(), Optional.empty(), false, Optional.empty(),
                IRMetadata.EMPTY, namespace, resolvedName, Optional.empty(), List.of(), List.of(), List.of(),
                effectiveBase, List.of(), List.of(), List.of(), aliasChain);
    }

    @Override
    public List<? extends IRNode> children() {
        return fields;
    }
}
