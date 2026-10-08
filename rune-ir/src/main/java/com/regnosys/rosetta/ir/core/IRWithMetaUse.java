package com.regnosys.rosetta.ir.core;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One {@code with-meta} expression in a model's bodies (v3.3 seat 8, PR #644): the entry names it writes
 * ({@code scheme}, {@code reference}, {@code id}, {@code location}, {@code address}, {@code key}, {@code template} -
 * as written, in order) and its ARGUMENT's inferred type as a reference. The wrapper set's fourth source: an entry that
 * names attribute meta pulls a {@code FieldWithMeta*} / {@code ReferenceWithMeta*} wrapper over the argument's type into
 * emission even when no attribute declares that meta ({@code MetaFieldGenerator.collectFromWithMetaExprs}).
 *
 * <p>THE INFERRED TYPE IS CARRIED AS ITS LEAF: a resolved alias type names no declaration the IR could reference, so the
 * adapter unwraps it to the leaf the old generator itself types ({@code toJavaType(alias.refersTo())}) - a declared type
 * as a STRUCT / ENUM / CHOICE reference, a builtin as a BASIC_TYPE / RECORD_TYPE reference with the constraints in force
 * stated as LITERAL {@link #typeArguments()} (the number ladder reads them). The wrapper's identity is the Java type, which
 * the leaf and its arguments determine.
 *
 * <p>THE NAMED REFUSAL: an argument the workspace types {@code nothing} ({@code empty with-meta {…}}) or cannot type at
 * all carries NO type and instead a {@link #refusal()} token - {@code nothing} or {@code missing}. The old generator skips
 * such a use silently (the {@code ReferenceWithMetaVoid} line, the ONE PERMANENT waiver of the byte gate); the IR states
 * the skip as a fact, so the emitter's register can show it. EXACTLY ONE of {@code argumentType} / {@code refusal} is
 * present, and a refused use carries no arguments - the record's law.
 *
 * @param entryNames    the with-meta entries' names, as written, in order
 * @param argumentType  the argument's inferred type as a reference (its leaf), when the workspace typed it
 * @param typeArguments the LITERAL constraints in force on a builtin leaf ({@code digits}, {@code fractionalDigits},
 *                      {@code min}, {@code max}, {@code minLength}, {@code maxLength}, {@code pattern}); empty for a
 *                      declared type
 * @param refusal       why no type is carried: {@code nothing} (the argument is typed {@code nothing}) or {@code missing}
 *                      (unresolvable) - present exactly when {@code argumentType} is absent
 */
public record IRWithMetaUse(List<String> entryNames, Optional<IRType> argumentType, List<IRTypeArgument> typeArguments,
                            Optional<String> refusal) {

    /** The two refusal tokens. */
    public static final List<String> REFUSALS = List.of("nothing", "missing");

    public IRWithMetaUse {
        entryNames = List.copyOf(entryNames);
        Objects.requireNonNull(argumentType, "argumentType");
        typeArguments = List.copyOf(typeArguments);
        Objects.requireNonNull(refusal, "refusal");
        if (argumentType.isPresent() == refusal.isPresent()) {
            throw new IllegalArgumentException("a with-meta use carries "
                    + (argumentType.isPresent() ? "BOTH an argument type and a refusal" : "NEITHER an argument type nor a refusal")
                    + " - exactly one is the law");
        }
        if (refusal.isPresent()) {
            if (!REFUSALS.contains(refusal.get())) {
                throw new IllegalArgumentException("a with-meta use's refusal is one of " + REFUSALS + ", not '" + refusal.get() + "'");
            }
            if (!typeArguments.isEmpty()) {
                throw new IllegalArgumentException("a refused with-meta use carries no type arguments");
            }
        }
        for (IRTypeArgument argument : typeArguments) {
            if (argument.nameValue().isPresent()) {
                throw new IllegalArgumentException("with-meta argument constraint '" + argument.parameter()
                        + "' passes a parameter through by name - an inferred type's constraints are literals");
            }
        }
    }
}
