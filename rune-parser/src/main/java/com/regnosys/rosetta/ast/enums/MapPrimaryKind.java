package com.regnosys.rosetta.ast.enums;

/**
 * Kind of primary expression in mapping test / set-to expressions.
 *
 * <p>Grammar: {@code rosettaMapPrimaryExpression} can be an enum value reference,
 * a string literal, a boolean, an integer, or a decimal.
 */
public enum MapPrimaryKind {
    ENUM_VALUE,
    STRING,
    BOOLEAN,
    INT,
    DECIMAL
}
