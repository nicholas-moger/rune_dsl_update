package com.regnosys.rosetta.ast.supporting;

import com.regnosys.rosetta.ast.RNode;

import java.math.BigInteger;

/**
 * Cardinality node, corresponding to the {@code rosettaCardinality} grammar rule.
 *
 * <p>Represents a cardinality constraint such as {@code (1..1)}, {@code (0..*)},
 * or {@code (0..5)}.
 *
 * <p>Grammar:
 * <pre>
 * rosettaCardinality:
 *     LPAREN inf=INT_LITERAL DOTDOT (sup=INT_LITERAL | unbounded=STAR) RPAREN
 * ;
 * </pre>
 *
 * <p>{@code inf} and {@code sup} are parsed from the grammar's
 * {@code INT_LITERAL} token, which has no upper bound. They are stored as
 * {@link BigInteger} to handle arbitrary-precision integers — the
 * {@link com.regnosys.rosetta.ast.expressions.literals.RIntLiteral} class
 * uses the same approach for the same reason. Convenience {@code int} setter
 * overloads cover the common case where the cardinality fits in a Java
 * {@code int}.
 *
 * <p>When the upper bound is a star ({@code *}), {@link #isUnbounded()}
 * returns {@code true} and {@link #sup()} returns {@link #UNBOUNDED} as a
 * sentinel. The {@code unbounded} flag and the {@code sup} field are kept
 * fully in sync via the setters — the invariant
 * {@code isUnbounded() == sup().equals(UNBOUNDED)} always holds:
 * <ul>
 *   <li>{@link #setSup(BigInteger) setSup(UNBOUNDED)} sets {@code unbounded = true}</li>
 *   <li>{@link #setSup(BigInteger) setSup(N)} for any other N sets {@code unbounded = false}</li>
 *   <li>{@link #setUnbounded(boolean) setUnbounded(true)} sets {@code sup = UNBOUNDED}</li>
 *   <li>{@link #setUnbounded(boolean) setUnbounded(false)} clears the
 *       {@code UNBOUNDED} sentinel from {@code sup} (resetting it to
 *       {@link BigInteger#ZERO}) so callers must subsequently call
 *       {@link #setSup(BigInteger)} with the desired bounded upper value</li>
 * </ul>
 *
 * @see #isUnbounded()
 */
public class RCardinality extends RNode {

    /** Sentinel value for unbounded upper cardinality. */
    public static final BigInteger UNBOUNDED = BigInteger.valueOf(-1);

    private BigInteger inf;
    private BigInteger sup;
    private boolean unbounded;

    // -- inf ------------------------------------------------------------------

    public BigInteger inf() {
        return inf;
    }

    public void setInf(BigInteger inf) {
        checkMutable();
        this.inf = inf;
    }

    /** Convenience overload for callers that have a plain {@code int}. */
    public void setInf(int inf) {
        checkMutable();
        this.inf = BigInteger.valueOf(inf);
    }

    // -- sup ------------------------------------------------------------------

    /**
     * Returns the upper bound of the cardinality. If the cardinality is
     * unbounded (i.e., {@code *}), returns {@link #UNBOUNDED}.
     */
    public BigInteger sup() {
        return sup;
    }

    /**
     * Sets the upper bound. Passing {@link #UNBOUNDED} is equivalent to
     * calling {@code setUnbounded(true)} — both fields are kept in sync.
     * Passing any other value clears the {@code unbounded} flag.
     */
    public void setSup(BigInteger sup) {
        checkMutable();
        this.sup = sup;
        this.unbounded = UNBOUNDED.equals(sup);
    }

    /** Convenience overload for callers that have a plain {@code int}. */
    public void setSup(int sup) {
        checkMutable();
        setSup(BigInteger.valueOf(sup));
    }

    // -- unbounded ------------------------------------------------------------

    public boolean isUnbounded() {
        return unbounded;
    }

    /**
     * Sets the unbounded flag. The {@code sup} field is kept in sync to
     * preserve the {@code isUnbounded() == sup().equals(UNBOUNDED)} invariant:
     * <ul>
     *   <li>When called with {@code true}, {@code sup} is set to
     *       {@link #UNBOUNDED}.</li>
     *   <li>When called with {@code false}, {@code sup} is reset to
     *       {@link BigInteger#ZERO} if it was previously {@link #UNBOUNDED}.
     *       Callers should subsequently call {@link #setSup(BigInteger)}
     *       with the desired bounded upper value.</li>
     * </ul>
     */
    public void setUnbounded(boolean unbounded) {
        checkMutable();
        this.unbounded = unbounded;
        if (unbounded) {
            this.sup = UNBOUNDED;
        } else if (UNBOUNDED.equals(this.sup)) {
            this.sup = BigInteger.ZERO;
        }
    }
}
