package com.regnosys.rosetta.ast.enums;

/**
 * Logical operators in expressions.
 *
 * <p>Grammar: {@code expression AND expression} (AndExpr)
 * and {@code expression OR expression} (OrExpr).
 */
public enum LogOp {
    AND,
    OR
}
