package com.regnosys.rosetta.validation;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.symbols.diagnostics.Severity;
import com.regnosys.rosetta.symbols.diagnostics.ValidationDiagnostic;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable collector for M6 validation diagnostics. Used by validators
 * during Pass 7. The {@link com.regnosys.rosetta.symbols.RWorkspace}
 * exposes only the immutable list (via {@link #toList()}).
 *
 * <p>Spec: D2 in {@code docs/specs/2026-04-09-m6-validation-design.md}.
 */
public final class ValidationCollector {

    private final List<ValidationDiagnostic> entries = new ArrayList<>();

    public void error(SourceRange range, String message, ValidationIssueCode code) {
        entries.add(new ValidationDiagnostic(Severity.ERROR, range, message, code));
    }

    public void warning(SourceRange range, String message, ValidationIssueCode code) {
        entries.add(new ValidationDiagnostic(Severity.WARNING, range, message, code));
    }

    public void info(SourceRange range, String message, ValidationIssueCode code) {
        entries.add(new ValidationDiagnostic(Severity.INFO, range, message, code));
    }

    public boolean isEmpty() { return entries.isEmpty(); }
    public int size() { return entries.size(); }

    public List<ValidationDiagnostic> toList() {
        return List.copyOf(entries);
    }
}
