package com.regnosys.rosetta.ast.enums;

/**
 * Equality operators in expressions.
 *
 * <p>Grammar: {@code expression cardinalityModifier? (EQ | NEQ) expression}
 * (EqualityExpr).
 */
public enum EqOp {
    EQ,
    NEQ
}
