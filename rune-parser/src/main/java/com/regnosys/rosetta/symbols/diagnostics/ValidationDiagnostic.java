package com.regnosys.rosetta.symbols.diagnostics;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.validation.ValidationIssueCode;

import java.util.Objects;

/**
 * M6 validation diagnostic — produced by semantic validators when a
 * program is syntactically valid and fully resolved but violates a
 * semantic rule (type mismatch, cardinality violation, naming convention, etc.).
 *
 * <p>Spec: D2 in {@code docs/specs/2026-04-09-m6-validation-design.md}.
 */
public record ValidationDiagnostic(
        Severity severity,
        SourceRange range,
        String message,
        ValidationIssueCode issueCode) implements RDiagnostic {

    public ValidationDiagnostic {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(range, "range");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(issueCode, "issueCode");
    }
}
