package com.regnosys.rosetta.ast.enums;

/**
 * Kind of switch case guard.
 *
 * <p>Grammar: {@code switchCaseGuard : literal | qualifiedName ;}
 * — a literal value or a named reference (typically an enum value).
 */
public enum SwitchGuardKind {
    LITERAL,
    NAME
}
