package com.regnosys.rosetta.ir.core;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One annotation written on a declaration, a field, a choice option, an enum or an enum value:
 * {@code [metadata scheme]}, {@code [rootType]}, {@code [deprecated]}, {@code [metadata key]}.
 *
 * <p>The annotation is carried as the model SPELLS it — its name, its optional qualifier (the
 * {@code scheme} of {@code [metadata scheme]}) and its {@code key = value} arguments in source order.
 * What a backend makes of {@code metadata:reference} (a {@code ReferenceWithMeta*} property type, a
 * meta class) is the backend's; the IR states the fact.
 *
 * @param name      the annotation's name as written ({@code metadata}, {@code rootType}, …)
 * @param qualifier the single bare qualifier, when the use carries one ({@code scheme}, {@code key}, …)
 * @param arguments the {@code key = value} qualifiers in source order; empty for most uses
 */
public record IRAnnotationUse(String name, Optional<String> qualifier, List<Argument> arguments) {

    public IRAnnotationUse {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(qualifier, "qualifier");
        arguments = List.copyOf(arguments);
    }

    /** A use with no arguments: {@code [metadata scheme]}, {@code [deprecated]}. */
    public IRAnnotationUse(String name, Optional<String> qualifier) {
        this(name, qualifier, List.of());
    }

    /** {@code metadata:scheme} for a qualified use, the bare name otherwise — the census key. */
    public String key() {
        return qualifier.map(q -> name + ":" + q).orElse(name);
    }

    /**
     * One {@code key = value} argument of an annotation use.
     *
     * @param key          the argument's name
     * @param value        its value as written (a string literal's content, or an attribute's name)
     * @param attributeRef true when the value names an attribute rather than spelling a literal
     */
    public record Argument(String key, String value, boolean attributeRef) {
        public Argument {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(value, "value");
        }
    }
}
