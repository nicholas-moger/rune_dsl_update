package com.regnosys.rosetta.ast.enums;

/**
 * Existence-checking postfix operators.
 *
 * <p>Grammar: {@code expression existsModifier? EXISTS} (ExistsExpr)
 * and {@code expression IS ABSENT} (AbsentExpr).
 */
public enum ExistenceOp {
    EXISTS,
    ABSENT
}
