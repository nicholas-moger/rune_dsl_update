package com.regnosys.rosetta.ast.enums;

/**
 * Arithmetic operators in expressions.
 *
 * <p>Grammar: {@code expression (STAR | SLASH) expression} (MultiplicativeExpr)
 * and {@code expression (PLUS | MINUS) expression} (AdditiveExpr).
 */
public enum ArithOp {
    PLUS,
    MINUS,
    MULTIPLY,
    DIVIDE
}
