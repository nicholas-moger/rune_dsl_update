package com.regnosys.rosetta.symbols.diagnostics;

/**
 * Category of an {@link RDiagnostic} variant ({@link LinkingDiagnostic},
 * {@link ValidationDiagnostic}, {@link ParserDiagnostic}). Each category
 * corresponds to a specific class of failure. The M3 negative test corpus
 * (D5/E15) asserts that for each linking category there is a deliberately
 * broken input file producing exactly that category at exactly the right
 * source range.
 *
 * <p>The {@link #PARSER} value (P1.4.1c, see U006) tags every
 * {@link ParserDiagnostic} regardless of underlying {@code RuneErrorCode}.
 *
 * <p>Spec: D10 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md} +
 * P1.4.1c §6.2 in {@code docs/superpowers/specs/2026-04-26-p1.4.1-internal-hardening-design.md}.
 */
public enum DiagnosticCategory {
    SUPER_TYPE_NOT_FOUND,
    IMPORT_UNRESOLVED,
    IMPORT_AMBIGUOUS,
    TYPE_NOT_FOUND,
    ANNOTATION_NOT_FOUND,
    ANNOTATION_QUALIFIER_NOT_FOUND,
    ENUM_NOT_FOUND,
    ENUM_VALUE_NOT_FOUND,
    RULE_NOT_FOUND,
    FUNCTION_NOT_FOUND,
    SUPER_FUNCTION_NOT_FOUND,
    SEGMENT_DEFINITION_NOT_FOUND,
    EXTERNAL_SOURCE_NOT_FOUND,
    EXTERNAL_TYPE_NOT_FOUND,
    SYMBOL_NOT_FOUND,
    DISPATCH_PARAM_NOT_FOUND,
    DERIVED_STATE_FAILED,
    CIRCULAR_INHERITANCE,

    // M4: Type inference failures
    TYPE_INFERENCE_FAILED,
    TYPE_MISMATCH,

    // M4: Type-directed resolution failures
    FEATURE_NOT_FOUND,
    DEEP_FEATURE_NOT_FOUND,
    OPERATION_PATH_NOT_FOUND,
    SWITCH_GUARD_TYPE_MISMATCH,
    META_KEY_NOT_FOUND,
    EXTERNAL_ATTRIBUTE_NOT_FOUND,
    EXTERNAL_ENUM_VALUE_NOT_FOUND,

    // M4: Type alias failures
    ALIAS_PARAMETER_MISMATCH,
    ALIAS_REVERSE_FAILED,

    // P1.4.1c (H1): structured parser diagnostics — see U006
    PARSER
}
