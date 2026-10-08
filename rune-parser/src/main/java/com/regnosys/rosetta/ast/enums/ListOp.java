package com.regnosys.rosetta.ast.enums;

/**
 * List/collection postfix operators in expressions.
 *
 * <p>Grammar alternatives: {@code ONLY_ELEMENT}, {@code FLATTEN},
 * {@code DISTINCT}, {@code REVERSE}, {@code FIRST}, {@code LAST}, {@code SUM}.
 */
public enum ListOp {
    ONLY_ELEMENT,
    FLATTEN,
    DISTINCT,
    REVERSE,
    FIRST,
    LAST,
    SUM
}
