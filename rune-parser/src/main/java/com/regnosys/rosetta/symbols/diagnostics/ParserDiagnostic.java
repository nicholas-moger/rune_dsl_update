package com.regnosys.rosetta.symbols.diagnostics;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.parser.diagnostics.RuneErrorCode;

import java.util.Objects;
import java.util.Optional;

/**
 * Parser-emitted diagnostic carrying a structured {@link RuneErrorCode}
 * (RUNE-001..099 band).
 *
 * <p>Lives in {@code symbols.diagnostics} package alongside {@link LinkingDiagnostic}
 * and {@link ValidationDiagnostic} so the {@code RDiagnostic} sealed permits clause
 * works without a {@code module-info.java} (rune-parser is in the unnamed module;
 * sealed cross-package permits require a named module).
 *
 * <p>Spec: §6.2 in {@code docs/superpowers/specs/2026-04-26-p1.4.1-internal-hardening-design.md}.
 *
 * <p>{@link #toLegacyString()} produces byte-identical output to the
 * pre-P1.4.1c {@code RosettaErrorListener} format ({@code line:col message},
 * with col 0-indexed per ANTLR convention). The legacy format and the
 * structured form are populated in lockstep by
 * {@link com.regnosys.rosetta.parser.RosettaParserFacade}.
 *
 * <p>Field name {@code errorCode} (rather than {@code code}) avoids accessor
 * conflict with the {@link RDiagnostic#code()} contract method (record auto-accessor
 * return type must match the component type, but the contract returns
 * {@code Optional<RuneErrorCode>}). Access the underlying value via
 * {@link #errorCode()}; access the contract-typed wrapper via {@link #code()}.
 */
public record ParserDiagnostic(
        RuneErrorCode errorCode,
        Severity severity,
        DiagnosticCategory category,
        SourceRange range,
        String message,
        Optional<String> hint
) implements RDiagnostic {

    public ParserDiagnostic {
        Objects.requireNonNull(errorCode, "errorCode");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(range, "range; use SourceRange.NONE if truly unknown");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(hint, "hint; use Optional.empty() if absent");
    }

    @Override
    public Optional<RuneErrorCode> code() {
        return Optional.of(errorCode);
    }

    /**
     * Byte-equal inverse of the legacy {@code RosettaErrorListener} formatter.
     * Format: {@code line:col message} where {@code col} is 0-indexed
     * (subtracts 1 from {@link SourceRange#startCol()} which is 1-indexed).
     *
     * <p>{@link SourceRange#NONE} (or any range with {@code startCol == 0})
     * is clamped to legacy col {@code 0} — never emits a negative column.
     * Pre-clamp behaviour produced strings like {@code "0:-1 ..."} which
     * legacy consumers cannot parse.
     *
     * <p>Round-trip: any legacy string parsed back into a {@code ParserDiagnostic}
     * via the facade's classifier and re-formatted through this method must
     * produce the original string. Exercised by
     * {@code LegacyRoundTripPropertyTest}.
     */
    public String toLegacyString() {
        int legacyCol = Math.max(range.startCol() - 1, 0);
        return range.startLine() + ":" + legacyCol + " " + message;
    }
}
