package com.regnosys.rosetta.ir.core;

import java.util.List;
import java.util.Optional;

/**
 * THE MODEL-LEVEL NODE (v3.3 seat 8, PR #644 - the property gate; {@link IRKind#MODEL}): the facts of ONE
 * {@code namespace} declaration that no type, enum or alias node carries and that the data-type emitter's derived
 * files read - the first model-level node the declaration IR has carried (the maintainer's ratification of
 * 2026-09-19: additive, every member optional, {@code irFormatVersion} stays 1).
 *
 * <p>What it carries, and which derived file reads it:
 * <ul>
 *   <li>{@link #definition()} - the namespace's documentation: {@code package-info.java} is written for a namespace whose
 *       model carries one ({@code JavaPackageInfoGenerator});</li>
 *   <li>{@link #version()} - the model's version string, stamped on every {@code *Meta} and POJO header
 *       ({@code ModelMetaGenerator}, {@code ModelObjectGenerator}) - an IR fact since this seat, not a host parameter
 *       (the maintainer's answer to PLAN.md § F question 4: the emitter is IR-self-sufficient);</li>
 *   <li>{@link #qualifiableConfigs()} - the model's own {@code isEvent root X} / {@code isProduct root Y} declarations;
 *       the "FIRST root of a kind over the workspace's load order" law ({@code RQualifiableConfig.firstRoot}) stays the
 *       INDEX's, computed over the model nodes in load order;</li>
 *   <li>{@link #qualificationFunctions()} - every {@code [qualification]} function of the model with at least one input,
 *       and its FIRST input's type as a reference: the {@code *Meta} qualify wing lists those whose first input IS the
 *       root ({@code ModelMetaGenerator.collectQualifyFunctions} - identity there, the qualified name here);</li>
 *   <li>{@link #functionSignatures()} - every function's inputs and output as fields: the {@code FieldWithMeta*} /
 *       {@code ReferenceWithMeta*} wrapper set is collected from their {@code [metadata …]} annotations too
 *       ({@code MetaFieldGenerator.collectSpecs});</li>
 *   <li>{@link #withMetaUses()} - every {@code with-meta} expression in the model's bodies, with its entry names and its
 *       argument's inferred type: the same wrapper set's fourth source; an argument typed {@code nothing} or unresolvable
 *       is a NAMED refusal on the use, never a silent drop (the {@code ReferenceWithMetaVoid} line, the one permanent waiver).</li>
 * </ul>
 * {@link IRNode#name()} is the namespace itself.
 */
public interface IRModel extends IRNode {

    /** The namespace this node describes - the same string as {@link #name()}. */
    String namespace();

    /** The namespace's documentation ({@code namespace a.b : <"...">}), when the model wrote one. */
    default Optional<String> definition() {
        return Optional.empty();
    }

    /** The model's {@code version "..."} string, when stated. */
    default Optional<String> version() {
        return Optional.empty();
    }

    /** The model's own {@code isEvent root} / {@code isProduct root} declarations, in declaration order. */
    default List<IRQualifiableConfig> qualifiableConfigs() {
        return List.of();
    }

    /** The model's {@code [qualification]} functions with at least one input, in declaration order. */
    default List<IRQualificationFunction> qualificationFunctions() {
        return List.of();
    }

    /** Every function the model declares, its inputs and output as fields, in declaration order. */
    default List<IRFunctionSignature> functionSignatures() {
        return List.of();
    }

    /** Every {@code with-meta} expression in the model's bodies, in the adapter's subtree-walk order. */
    default List<IRWithMetaUse> withMetaUses() {
        return List.of();
    }
}
