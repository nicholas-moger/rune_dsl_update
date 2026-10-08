package com.regnosys.rosetta.types;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Pattern;

/**
 * Parametric string type: {@code string(minLength, maxLength, pattern)}.
 */
public final class RStringType implements RType {
    private final OptionalInt minLength;
    private final OptionalInt maxLength;
    private final Optional<Pattern> pattern;

    public RStringType(OptionalInt minLength, OptionalInt maxLength, Optional<Pattern> pattern) {
        this.minLength = Objects.requireNonNull(minLength);
        this.maxLength = Objects.requireNonNull(maxLength);
        this.pattern = Objects.requireNonNull(pattern);
    }

    public static RStringType unconstrained() {
        return new RStringType(OptionalInt.empty(), OptionalInt.empty(), Optional.empty());
    }

    @Override public String name() { return "string"; }
    @Override public boolean hasNaturalOrder() { return false; }

    public OptionalInt minLength() { return minLength; }
    public OptionalInt maxLength() { return maxLength; }
    public Optional<Pattern> pattern() { return pattern; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RStringType that)) return false;
        return Objects.equals(minLength, that.minLength)
            && Objects.equals(maxLength, that.maxLength)
            && Objects.equals(
                pattern.map(Pattern::pattern),
                that.pattern.map(Pattern::pattern));
    }

    @Override public int hashCode() {
        return Objects.hash(minLength, maxLength, pattern.map(Pattern::pattern));
    }

    @Override public String toString() {
        var parts = new java.util.ArrayList<String>();
        minLength.ifPresent(m -> parts.add("minLength: " + m));
        maxLength.ifPresent(m -> parts.add("maxLength: " + m));
        pattern.ifPresent(p -> parts.add("pattern: " + p.pattern()));
        return parts.isEmpty() ? "string" : "string(" + String.join(", ", parts) + ")";
    }
}
