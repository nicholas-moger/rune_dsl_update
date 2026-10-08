package com.regnosys.rosetta.symbols.diagnostics;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.parser.diagnostics.RuneErrorCode;

import java.util.Optional;

/**
 * Sealed interface for all diagnostics: linking (M3/M4), validation (M6),
 * and parser (P1.4.1c).
 *
 * <p>Provides a uniform API for M8 LSP to consume a single diagnostic stream.
 * Each variant may carry a structured {@link RuneErrorCode} via the optional
 * {@link #code()} accessor; only {@link ParserDiagnostic} populates it today
 * (RUNE-001..099 band). Future PRs may add code() overrides on
 * {@link LinkingDiagnostic} (RUNE-100..199) and {@link ValidationDiagnostic}
 * (RUNE-300..399) without breaking this contract.
 *
 * <p>Spec: D9 in {@code docs/specs/2026-04-09-m6-validation-design.md} +
 * P1.4.1c §6.1 in {@code docs/superpowers/specs/2026-04-26-p1.4.1-internal-hardening-design.md}.
 */
public sealed interface RDiagnostic
    permits LinkingDiagnostic, ValidationDiagnostic, ParserDiagnostic {

    Severity severity();
    SourceRange range();
    String message();

    /**
     * Optional structured error code. Returns {@link Optional#empty()} by default;
     * {@link ParserDiagnostic} overrides to return its {@link RuneErrorCode}.
     * Linking/validation diagnostics inherit the default until future PRs assign
     * RUNE-100/300 codes to them.
     */
    default Optional<RuneErrorCode> code() {
        return Optional.empty();
    }
}
