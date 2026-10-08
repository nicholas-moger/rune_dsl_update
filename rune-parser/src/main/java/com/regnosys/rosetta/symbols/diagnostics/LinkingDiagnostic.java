package com.regnosys.rosetta.symbols.diagnostics;

import com.regnosys.rosetta.ast.SourceRange;

import java.util.List;
import java.util.Objects;

/**
 * One linking diagnostic — produced by the M3 linker when a cross-reference
 * cannot be resolved. Diagnostic-first resolution is one of the load-bearing
 * design decisions in M3 (D2): the linker NEVER silently sets a field to null
 * on failure; it always emits a {@code LinkingDiagnostic} with a precise
 * source range so consumers can either render it (M6/M8) or assert in tests
 * that the failure mode is loud.
 *
 * <p>Spec: D10 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
public record LinkingDiagnostic(
        Severity severity,
        DiagnosticCategory category,
        SourceRange range,
        String unresolvedName,
        String message,
        List<String> candidates) implements RDiagnostic {

    public LinkingDiagnostic {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(range, "range; use SourceRange.NONE if truly unknown");
        Objects.requireNonNull(unresolvedName, "unresolvedName");
        Objects.requireNonNull(message, "message");
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }
}
