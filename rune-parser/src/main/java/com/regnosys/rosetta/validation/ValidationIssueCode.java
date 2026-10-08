package com.regnosys.rosetta.validation;

/**
 * Issue codes for M6 validation diagnostics. Each code corresponds to a
 * specific class of validation failure. M8 LSP quickfixes will match on
 * these codes.
 *
 * <p>Derived from upstream {@code RosettaIssueCodes} in rune-dsl.
 * Spec: D6 in {@code docs/specs/2026-04-09-m6-validation-design.md}.
 */
public enum ValidationIssueCode {
    // Naming
    INVALID_NAME,
    INVALID_CASE,
    INVALID_ELEMENT_NAME,

    // Structural uniqueness
    DUPLICATE_ATTRIBUTE,
    DUPLICATE_ENUM_VALUE,
    DUPLICATE_ELEMENT_NAME,
    DUPLICATE_CHOICE_RULE_ATTRIBUTE,

    // Type errors
    TYPE_ERROR,
    INVALID_TYPE,

    // Cardinality
    CARDINALITY_ERROR,

    // Attribute
    MISSING_ATTRIBUTE,

    // Import
    UNUSED_IMPORT,
    DUPLICATE_IMPORT,

    // Mapping
    MAPPING_RULE_INVALID,
    MAPPING_RULE_NOT_USED,

    // Enum
    MISSING_ENUM_VALUE,

    // Condition
    CLASS_WITH_CHOICE_RULE_AND_ONE_OF_RULE,
    MULIPLE_CLASS_REFERENCES_DEFINED_FOR_CONDITION, // sic — upstream typo preserved for D8 compatibility

    // Syntax sugar
    MANDATORY_SQUARE_BRACKETS,
    REDUNDANT_SQUARE_BRACKETS,
    MANDATORY_THEN,

    // Constructor / function
    MISSING_MANDATORY_CONSTRUCTOR_ARGUMENT,
    CHANGED_EXTENDED_FUNCTION_PARAMETERS,

    // Deprecation (PR #455 — the warning-family waves: upstream emits these
    // as plain code-less warnings; the fork's collector requires a code)
    DEPRECATION,
    /** v3.2 seat 4 (PR #625, F10): a qualification function's input is not the first-wins root. */
    QUALIFICATION_ROOT_MISMATCH,

    // Implementation coverage (PR #455 — upstream's codeImplementation /
    // dispatch missing-implementation warning family, likewise code-less)
    MISSING_IMPLEMENTATION
}
