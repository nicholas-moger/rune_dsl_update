package com.regnosys.rosetta.symbols.linker;

import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a private field on an M2 AST class as a resolved-field that the
 * M3 linker writes during pass 4 or pass 5. The reflection-driven
 * {@code ResolutionAudit} (test infrastructure) walks every
 * {@code @CrossRefField} on every node after linking and asserts that
 * the field is set OR a diagnostic of the matching {@link #category()}
 * exists for the node's source range.
 *
 * <p>Spec: D2 + D5/E2 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CrossRefField {
    /** The diagnostic category emitted when this field cannot be resolved. */
    DiagnosticCategory category();

    /**
     * The token-range key on the parent node carrying the source range for
     * this reference. Empty string means "use the node's overall sourceRange".
     */
    String tokenRangeKey() default "";

    /**
     * For multi-valued cross-refs, the prefix of the indexed token-range
     * keys (e.g. "superSource" → "superSource_0", "superSource_1", ...).
     * Empty for single-valued.
     */
    String tokenRangePrefix() default "";
}
