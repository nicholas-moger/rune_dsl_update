package com.regnosys.rosetta.ir.core;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One declared parameter of a {@code typeAlias} (v3.3 seat 7, PR #643): the {@code digits int} of
 * {@code typeAlias int(digits int, min int, max int): number(...)}. The body passes a parameter through by NAME
 * ({@link IRTypeArgument#nameValue()}); a use site binds it by a literal argument.
 *
 * <p>Since PR #644 (the banked cq SF-1 of PR #643) the parameter also carries the arguments its OWN type call
 * wrote: the {@code fractionalDigits: 0} of {@code typeAlias X(n number(fractionalDigits: 0)): ...} - grammar-admitted
 * ({@code typeParameter: typeParameterValidID typeCall definable?}), and the fact that decides the parameter's Java
 * type ({@code BigInteger}, not {@code BigDecimal}). Before it, the adapter dropped them and a constrained parameter
 * lost its constraint silently. Zero such parameters on the 26 cells; the fact exists so the lane can fail.
 *
 * @param name          the parameter's name
 * @param type          the parameter's declared type, a REFERENCE (kind, namespace, resolved name)
 * @param definition    the parameter's documentation, when the model wrote one
 * @param typeArguments the arguments the parameter's own type call wrote, AS WRITTEN and in the call's order; empty
 *                      for a bare {@code int}
 */
public record IRTypeParameter(String name, IRType type, Optional<String> definition,
                              List<IRTypeArgument> typeArguments) {
    public IRTypeParameter {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(definition, "definition");
        typeArguments = List.copyOf(Objects.requireNonNull(typeArguments, "typeArguments"));
        if (name.isBlank()) {
            throw new IllegalArgumentException("a type parameter needs a name");
        }
    }
}
