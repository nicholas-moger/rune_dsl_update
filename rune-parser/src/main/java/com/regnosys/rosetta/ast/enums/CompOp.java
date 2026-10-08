package com.regnosys.rosetta.ast.enums;

/**
 * Comparison operators in expressions.
 *
 * <p>Grammar: {@code expression cardinalityModifier? (LT | GT | LTE | GTE) expression}
 * (ComparisonExpr).
 */
public enum CompOp {
    LT,
    GT,
    LTE,
    GTE
}
