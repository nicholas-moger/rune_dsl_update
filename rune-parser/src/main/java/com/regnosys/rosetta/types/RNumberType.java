package com.regnosys.rosetta.types;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Parametric number type: {@code number(digits, fractionalDigits, min, max)}.
 * {@code int} is represented as {@code number(fractionalDigits: 0)}.
 */
public final class RNumberType implements RType {
    private final OptionalInt digits;
    private final OptionalInt fractionalDigits;
    private final Optional<BigDecimal> min;
    private final Optional<BigDecimal> max;

    public RNumberType(OptionalInt digits, OptionalInt fractionalDigits,
                       Optional<BigDecimal> min, Optional<BigDecimal> max) {
        this.digits = Objects.requireNonNull(digits);
        this.fractionalDigits = Objects.requireNonNull(fractionalDigits);
        this.min = Objects.requireNonNull(min);
        this.max = Objects.requireNonNull(max);
    }

    public static RNumberType unconstrained() {
        return new RNumberType(OptionalInt.empty(), OptionalInt.empty(),
            Optional.empty(), Optional.empty());
    }

    public static RNumberType intType() {
        return new RNumberType(OptionalInt.empty(), OptionalInt.of(0),
            Optional.empty(), Optional.empty());
    }

    @Override public String name() { return isInteger() ? "int" : "number"; }
    @Override public boolean hasNaturalOrder() { return true; }

    public OptionalInt digits() { return digits; }
    public OptionalInt fractionalDigits() { return fractionalDigits; }
    public Optional<BigDecimal> min() { return min; }
    public Optional<BigDecimal> max() { return max; }

    public boolean isInteger() {
        return fractionalDigits.isPresent() && fractionalDigits.getAsInt() == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RNumberType that)) return false;
        return Objects.equals(digits, that.digits)
            && Objects.equals(fractionalDigits, that.fractionalDigits)
            && Objects.equals(min, that.min)
            && Objects.equals(max, that.max);
    }

    @Override public int hashCode() { return Objects.hash(digits, fractionalDigits, min, max); }

    @Override
    public String toString() {
        var sb = new StringBuilder(name());
        var parts = new java.util.ArrayList<String>();
        digits.ifPresent(d -> parts.add("digits: " + d));
        fractionalDigits.ifPresent(f -> parts.add("fractionalDigits: " + f));
        min.ifPresent(m -> parts.add("min: " + m));
        max.ifPresent(m -> parts.add("max: " + m));
        if (!parts.isEmpty()) sb.append("(").append(String.join(", ", parts)).append(")");
        return sb.toString();
    }
}
