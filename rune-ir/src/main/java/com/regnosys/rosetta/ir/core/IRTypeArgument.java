package com.regnosys.rosetta.ir.core;

import java.util.Objects;
import java.util.Optional;

/**
 * One argument of a parameterised type reference: the {@code digits: 18} of
 * {@code number(digits: 18, fractionalDigits: 2)}, the {@code maxLength: 35} of
 * {@code string(maxLength: 35)}.
 *
 * <p>The value is carried as the model spells it — EXACTLY ONE of {@link #nameValue()} (a type
 * parameter passed through by name) and {@link #literalValue()} (a literal, with its optional
 * {@code -} prefix stated by {@link #negated()}). The type-format validator reads these. The law is
 * ENFORCED by the constructor (both, or neither, is refused - and so is a sign without a literal), stated
 * by the JSON schema ({@code oneOf}) and re-raised by the reader as an {@code IRJsonException}.
 *
 * @param parameter    the parameter's name
 * @param nameValue    the value when it names a type parameter
 * @param literalValue the value when it is a literal, as written, WITHOUT the sign
 * @param negated      true when the literal carries the {@code -} prefix
 */
public record IRTypeArgument(String parameter, Optional<String> nameValue, Optional<String> literalValue,
                             boolean negated) {
    public IRTypeArgument {
        Objects.requireNonNull(parameter, "parameter");
        Objects.requireNonNull(nameValue, "nameValue");
        Objects.requireNonNull(literalValue, "literalValue");
        if (nameValue.isPresent() == literalValue.isPresent()) {
            throw new IllegalArgumentException("type argument '" + parameter + "' carries "
                    + (nameValue.isPresent() ? "BOTH a name value and a literal value" : "NEITHER a name value nor a literal value")
                    + " - exactly one is the law");
        }
        if (negated && literalValue.isEmpty()) {
            throw new IllegalArgumentException("type argument '" + parameter + "' is negated without a literal value");
        }
    }
}
