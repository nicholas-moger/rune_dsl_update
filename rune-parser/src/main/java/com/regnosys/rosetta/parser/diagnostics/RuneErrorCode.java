package com.regnosys.rosetta.parser.diagnostics;

import org.antlr.v4.runtime.Token;

import java.util.Optional;

/**
 * Sealed interface for parser-band diagnostic codes (RUNE-001..099).
 *
 * <p>Each record variant carries the structured payload for one error class.
 * {@link #code()} returns the canonical {@code RUNE-NNN} string;
 * {@link #docPath()} points at the per-code documentation page under
 * {@code docs/diagnostics/}.
 *
 * <p>Records-with-payload: {@code equals}/{@code hashCode} over fields.
 * Two {@code UnexpectedToken} with same payload are {@code .equals()};
 * useful for diagnostic deduplication. Consumers needing identity use
 * {@link #code()} string equality.
 *
 * <p>Spec: §6.3 in {@code docs/superpowers/specs/2026-04-26-p1.4.1-internal-hardening-design.md}.
 *
 * <p>Banding scheme (D9 §codes-registry):
 * <ul>
 *   <li>RUNE-001..099 — parser (this interface)</li>
 *   <li>RUNE-100..199 — scope/linker (future)</li>
 *   <li>RUNE-200..299 — type system (future)</li>
 *   <li>RUNE-300..399 — validation (future)</li>
 *   <li>RUNE-400..499 — codegen (future)</li>
 *   <li>RUNE-900..999 — internal (future)</li>
 * </ul>
 */
public sealed interface RuneErrorCode {

    /** Canonical code string, e.g. "RUNE-001". */
    String code();

    /** Path to the per-code documentation page, relative to repo root. */
    default String docPath() {
        return "docs/diagnostics/" + code() + ".md";
    }

    // --- RUNE-001: UnexpectedToken (most common ANTLR InputMismatchException) ---
    record UnexpectedToken(
            Optional<String> expected,
            Optional<Token> offending,
            String rawAntlrMessage
    ) implements RuneErrorCode {
        @Override public String code() { return "RUNE-001"; }
    }

    record MissingToken(String tokenName) implements RuneErrorCode {
        @Override public String code() { return "RUNE-002"; }
    }

    record ExtraneousInput(String text) implements RuneErrorCode {
        @Override public String code() { return "RUNE-003"; }
    }

    record NoViableAlternative(String context) implements RuneErrorCode {
        @Override public String code() { return "RUNE-004"; }
    }

    record FailedPredicate(String predicate) implements RuneErrorCode {
        @Override public String code() { return "RUNE-005"; }
    }

    record GrammarAmbiguity(String rule, int startIndex, int stopIndex)
            implements RuneErrorCode {
        @Override public String code() { return "RUNE-006"; }
    }

    record ContextSensitiveParse(String rule, int startIndex, int stopIndex)
            implements RuneErrorCode {
        @Override public String code() { return "RUNE-007"; }
    }

    record LexerNoViableAlternative(String lexeme) implements RuneErrorCode {
        @Override public String code() { return "RUNE-008"; }
    }

    record MismatchedInput(String expected, String actual) implements RuneErrorCode {
        @Override public String code() { return "RUNE-009"; }
    }

    record UnexpectedEof(String inContext) implements RuneErrorCode {
        @Override public String code() { return "RUNE-010"; }
    }

    record InvalidStringLiteral(String literal) implements RuneErrorCode {
        @Override public String code() { return "RUNE-011"; }
    }

    record InvalidNumericLiteral(String literal) implements RuneErrorCode {
        @Override public String code() { return "RUNE-012"; }
    }

    record UnclosedComment(int startLine) implements RuneErrorCode {
        @Override public String code() { return "RUNE-013"; }
    }

    record DuplicateModifier(String modifier) implements RuneErrorCode {
        @Override public String code() { return "RUNE-014"; }
    }

    record UnknownErrorPattern(String rawMessage) implements RuneErrorCode {
        @Override public String code() { return "RUNE-015"; }
    }

    // --- RUNE-016..-019: P1.4.2 grammar-group additions (H2/H3/H4) -----------
    // Reserved code points for malformed input on the P1.4.2 grammar surfaces.
    // Layer-1 only — today the generic ANTLR error path may classify these as
    // RUNE-001 (UnexpectedToken) when the rule context isn't surfaced. Future
    // RuneErrorClassifier refinement (with rule-context awareness) can route
    // grammar-rule-specific failures to these codes for clearer diagnostics.

    record InvalidRuneAnnotationArg(String detail) implements RuneErrorCode {
        @Override public String code() { return "RUNE-016"; }
    }

    record InvalidFileHeaderField(String detail) implements RuneErrorCode {
        @Override public String code() { return "RUNE-017"; }
    }

    record InvalidRegulatoryReferenceArg(String detail) implements RuneErrorCode {
        @Override public String code() { return "RUNE-018"; }
    }

    record InvalidRuneAnnotationName(String detail) implements RuneErrorCode {
        @Override public String code() { return "RUNE-019"; }
    }
}
