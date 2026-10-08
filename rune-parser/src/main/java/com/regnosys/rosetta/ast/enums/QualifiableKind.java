package com.regnosys.rosetta.ast.enums;

/**
 * Kind of qualifiable configuration.
 *
 * <p>Grammar: {@code (IS_EVENT | IS_PRODUCT) ROOT qualifiedName SEMI}
 * (rosettaQualifiableConfiguration).
 */
public enum QualifiableKind {
    IS_EVENT,
    IS_PRODUCT
}
