package com.regnosys.rosetta.ast.enums;

/**
 * The lexer-terminal kind of a LITERAL switch-case guard — the fact the front-end alone can see.
 *
 * <p>Grammar: {@code switchCaseGuard : literal | qualifiedName ;} with
 * {@code literal : STRING | INT_LITERAL | BIG_DECIMAL | TRUE | FALSE ;}. The guard node stores the
 * literal's VALUE as text ({@code RSwitchCaseGuard.literalValue()} — quote-stripped for a STRING),
 * which makes a STRING guard {@code "42"} and an INT_LITERAL guard {@code 42} indistinguishable
 * downstream; this kind, recorded by the AST builder from the terminal that produced the value,
 * is the typed channel a consumer reads instead of classifying the value's text (v3.1 C2d,
 * retirement family 1 {@code numeric-literal-kind}).
 */
public enum SwitchGuardLiteralKind {
    /** A {@code STRING} terminal — the value is the quote-stripped, unescaped text. */
    STRING,
    /** An {@code INT_LITERAL} terminal ({@code DIGIT+}) — the value is the raw digits. */
    INT,
    /** A {@code BIG_DECIMAL} terminal — the value is the raw decimal / exponent text. */
    DECIMAL,
    /** A {@code TRUE} / {@code FALSE} terminal — the value is the raw keyword, {@code True} / {@code False}. */
    BOOLEAN
}
