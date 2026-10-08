package com.regnosys.rosetta.ir.core;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One link of a {@code typeAlias} CHAIN as a use site walks it OUTERMOST-FIRST (v3.3 seat 8, PR #644 - the property
 * gate): the alias the reference names, then the alias its body names, and so on to the leaf. {@link IRType#effectiveBase()}
 * collapses the chain to that leaf; this record keeps the RUNGS, because two derived files read them - the type-format
 * validator wires every condition of every alias in the chain (outermost first, conditions in declaration order), and
 * REFUSES the whole validator when a PARAMETERISED alias in the chain carries a condition (the old generator's
 * {@code TypeFormatValidatorGenerator}, the {@code TYPE_ALIAS_CONDITION_DROPPED} site). Both laws are pure functions of
 * these facts.
 *
 * @param name           the alias declaration's own simple name
 * @param namespace      the declaring model's namespace (empty only for a detached declaration)
 * @param parameterNames the alias's declared parameters, in declaration order (empty for a plain alias)
 * @param conditionNames the alias's conditions in declaration order, an unnamed one as {@code Optional.empty()}
 * @param conditionKinds index-parallel to {@code conditionNames}: the expression ROOT kind of each condition, one of
 *                       {@code OneOf} / {@code Choice} / {@code DataRule} (the token the old generator names an unnamed
 *                       condition's class by); EMPTY when the producer stated no kinds, else EXACTLY one per condition
 */
public record IRAliasLink(String name, Optional<String> namespace, List<String> parameterNames,
                          List<Optional<String>> conditionNames, List<String> conditionKinds) {

    /** The three tokens an unnamed condition's class name is built from (the old generator's own vocabulary). */
    public static final List<String> CONDITION_KINDS = List.of("OneOf", "Choice", "DataRule");

    public IRAliasLink {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(namespace, "namespace");
        parameterNames = List.copyOf(parameterNames);
        conditionNames = List.copyOf(conditionNames);
        conditionKinds = List.copyOf(conditionKinds);
        if (name.isBlank()) {
            throw new IllegalArgumentException("an alias link needs the alias's name");
        }
        checkConditionKinds("alias link '" + name + "'", conditionNames, conditionKinds);
    }

    /**
     * THE CONDITION-KINDS LAW, shared with {@code IRTypeNode}: the kinds are EMPTY (the producer stated none) or
     * index-parallel to the names, every token one of {@link #CONDITION_KINDS}. A kinds list of the wrong length or
     * with a token outside the vocabulary is refused - a fact half-stated is no fact.
     */
    public static void checkConditionKinds(String owner, List<Optional<String>> names, List<String> kinds) {
        if (!kinds.isEmpty() && kinds.size() != names.size()) {
            throw new IllegalArgumentException(owner + " states " + kinds.size() + " condition kinds for "
                    + names.size() + " conditions - the kinds are index-parallel to the names, or absent");
        }
        for (String kind : kinds) {
            if (!CONDITION_KINDS.contains(kind)) {
                throw new IllegalArgumentException(owner + " states the condition kind '" + kind
                        + "' - the vocabulary is " + CONDITION_KINDS);
            }
        }
    }

    /** {@code namespace.name}, or the bare name for a detached declaration. */
    public String qualifiedName() {
        return namespace.map(ns -> ns.isEmpty() ? name : ns + "." + name).orElse(name);
    }

    /** Whether this alias declares parameters - the half of the whole-validator refusal law. */
    public boolean isParameterised() {
        return !parameterNames.isEmpty();
    }
}
