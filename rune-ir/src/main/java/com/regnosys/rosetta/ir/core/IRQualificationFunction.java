package com.regnosys.rosetta.ir.core;

import java.util.Objects;

/**
 * One {@code [qualification]} function of a model with at least one input (v3.3 seat 8, PR #644): its name and its
 * FIRST input's type as a reference. The {@code *Meta} qualify wing lists, for the workspace's first root of a kind,
 * every such function whose first input is that root - the old generator compares node identity
 * ({@code firstInputType == element}); the IR compares the reference's resolved qualified name, which the index's
 * AMBIGUOUS refusal keeps sound (a name declared twice resolves to nothing, never to a first-wins pick).
 *
 * <p>The function's NAMESPACE is not repeated here: a qualification function is carried by the {@code IRModel} of the
 * model that declares it, whose {@code namespace()} IS the function's (PLAN § B family 5 wrote "name, namespace, and
 * its first input's type" - the namespace lives one level up, on the owning model node).
 *
 * @param name           the function's name
 * @param firstInputType the first input's declared type as a reference
 */
public record IRQualificationFunction(String name, IRType firstInputType) {
    public IRQualificationFunction {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(firstInputType, "firstInputType");
        if (name.isBlank()) {
            throw new IllegalArgumentException("a qualification function needs a name");
        }
    }
}
