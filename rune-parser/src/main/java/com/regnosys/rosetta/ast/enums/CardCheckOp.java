package com.regnosys.rosetta.ast.enums;

/**
 * Cardinality check operators.
 *
 * <p>Grammar: {@code expression ONE_OF} (OneOfExpr)
 * and {@code expression necessity CHOICE validID (COMMA validID)*} (ChoiceExpr).
 */
public enum CardCheckOp {
    ONE_OF,
    CHOICE
}
