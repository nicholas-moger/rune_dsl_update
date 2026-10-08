package com.regnosys.rosetta.ast.enums;

/**
 * Kind of mapping instance in synonym mapping logic.
 *
 * <p>Grammar: {@code SET WHEN rosettaMappingPathTests} or
 * {@code DEFAULT TO rosettaMapPrimaryExpression} (rosettaMappingInstance).
 */
public enum MappingInstanceKind {
    SET_WHEN,
    DEFAULT_TO
}
