package com.regnosys.rosetta.ir.core;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

/**
 * The EXACT cardinality bounds of a field, as the model declares them: {@code (lower..upper)} with an
 * empty {@link #upper()} for the unbounded form {@code (lower..*)}.
 *
 * <p>The four-bucket {@link Cardinality} cannot say {@code (0..2)}, {@code (2..2)} or {@code (2..*)};
 * the cardinality validator reads the exact pair. A field carries BOTH: the bucket (what a property's
 * Java shape turns on) and, when the model declares a cardinality at all, these bounds.
 *
 * <p>Two invariants are ENFORCED (the model cannot declare either shape, so they are byte-neutral on
 * the corpus): the lower bound is non-negative, and it does not exceed a stated upper bound. The
 * JSON schema states the first as {@code "minimum": 0} on both members, and the IR JSON reader
 * re-raises both as an {@code IRJsonException} naming the document path.
 *
 * @param lower the inclusive lower bound, non-negative
 * @param upper the inclusive upper bound; empty = unbounded ({@code *}); never below {@code lower}
 * @throws IllegalArgumentException when {@code lower} is negative, or above a stated {@code upper}
 */
public record IRBounds(BigInteger lower, Optional<BigInteger> upper) {

    public IRBounds {
        Objects.requireNonNull(lower, "lower");
        Objects.requireNonNull(upper, "upper");
        if (lower.signum() < 0) {
            throw new IllegalArgumentException("cardinality bounds (" + lower + ".."
                    + upper.map(BigInteger::toString).orElse("*")
                    + ") declare a NEGATIVE lower bound");
        }
        if (upper.isPresent() && lower.compareTo(upper.get()) > 0) {
            throw new IllegalArgumentException("cardinality bounds (" + lower + ".." + upper.get()
                    + ") declare a lower bound ABOVE the upper bound");
        }
    }

    /** True for the {@code (n..*)} form. */
    public boolean isUnbounded() {
        return upper.isEmpty();
    }

    /** The model's own spelling: {@code (0..1)}, {@code (2..*)}. */
    public String display() {
        return "(" + lower + ".." + upper.map(BigInteger::toString).orElse("*") + ")";
    }
}
