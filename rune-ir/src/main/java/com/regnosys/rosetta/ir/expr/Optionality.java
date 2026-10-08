package com.regnosys.rosetta.ir.expr;

/**
 * Whether an expression may yield an <em>absent</em> value at runtime. This is one of
 * two orthogonal cardinality axes carried by an {@link IRExpr}; the other is
 * {@code com.regnosys.rosetta.types.ExpressionCardinality} (SINGLE vs MULTI). Keeping
 * them separate matters: a value can be single-and-optional ({@code [0..1]}) or
 * multi-and-non-empty ({@code [1..*]}), and emitters lower the two axes independently.
 *
 * <p>Optionality is a build-time <strong>fact computed by the adapter</strong> — from
 * {@code RCardinality.inf()} along the resolved attribute/feature chain plus node-kind
 * rules — never re-derived per emitter and never a Java lowering decision (design §6).
 * Along a navigation chain it behaves as an absorbing element: any {@code OPTIONAL} hop
 * makes the whole chain {@code OPTIONAL} ("any input absent ⇒ result absent").
 *
 * <p>Wave-0 leaves: literals and list construction are {@code PRESENT}; the {@code empty}
 * leaf ({@link IREmptyLiteral}) is {@code OPTIONAL} (though it carries no monad identity —
 * the absent/zero interpretation is assigned later, in optionality-lowering). Variable
 * and reference leaves take their optionality from the resolved binding.
 */
public enum Optionality {

    /** The expression always yields a value. */
    PRESENT,

    /** The expression may yield an absent value. */
    OPTIONAL
}
