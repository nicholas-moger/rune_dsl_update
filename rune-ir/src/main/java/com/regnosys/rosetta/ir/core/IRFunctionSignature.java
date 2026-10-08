package com.regnosys.rosetta.ir.core;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One function's SIGNATURE (v3.3 seat 8, PR #644): its inputs and its output as {@link IRField}s - name, type
 * reference, cardinality, bounds and annotations, exactly what the wrapper collector reads of a function
 * ({@code MetaFieldGenerator.collectSpecs}: a {@code [metadata …]} annotation on an input or output adds a
 * {@code FieldWithMeta*} / {@code ReferenceWithMeta*} wrapper to the workspace's set). The body is NOT here - this is the
 * declaration IR; the expression IR carries bodies.
 *
 * @param name   the function's name
 * @param inputs the declared inputs, in declaration order
 * @param output the declared output, when the function has one
 */
public record IRFunctionSignature(String name, List<IRField> inputs, Optional<IRField> output) {
    public IRFunctionSignature {
        Objects.requireNonNull(name, "name");
        inputs = List.copyOf(inputs);
        Objects.requireNonNull(output, "output");
        if (name.isBlank()) {
            throw new IllegalArgumentException("a function signature needs a name");
        }
    }
}
