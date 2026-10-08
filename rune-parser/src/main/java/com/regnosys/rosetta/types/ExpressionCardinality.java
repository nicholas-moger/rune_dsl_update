package com.regnosys.rosetta.types;

/**
 * Expression-level cardinality: is this expression single-valued or
 * multi-valued (list)? Computed in parallel with type inference (D6).
 *
 * <p>Distinct from {@link com.regnosys.rosetta.ast.supporting.RCardinality}
 * which is the declaration-level (grammar) cardinality constraint.
 */
public enum ExpressionCardinality {
    /** Expression produces exactly one value. */
    SINGLE,
    /** Expression produces zero or more values (list). */
    MULTI
}
