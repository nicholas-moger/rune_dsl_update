package com.regnosys.rosetta.symbols.diagnostics;

/**
 * Severity of an {@link RDiagnostic}. M3 emits ERROR only;
 * M6 validation adds WARNING and INFO.
 *
 * <p>Spec: D10 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md},
 * D9 in {@code docs/specs/2026-04-09-m6-validation-design.md}.
 */
public enum Severity { ERROR, WARNING, INFO }
