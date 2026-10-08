package com.regnosys.rosetta.ast.enums;

/**
 * Operation type for function body operations.
 *
 * <p>Grammar: {@code (SET | ADD) validID segment? COLON definable? expressionWithAsKey}
 * (operation rule).
 */
public enum OperationOp {
    SET,
    ADD
}
