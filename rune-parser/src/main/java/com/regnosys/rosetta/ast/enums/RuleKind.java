package com.regnosys.rosetta.ast.enums;

/**
 * Kind of rule declaration.
 *
 * <p>Grammar: {@code (REPORTING | ELIGIBILITY) RULE validID ...} (rosettaRule).
 */
public enum RuleKind {
    REPORTING,
    ELIGIBILITY
}
