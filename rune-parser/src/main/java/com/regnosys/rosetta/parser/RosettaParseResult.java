package com.regnosys.rosetta.parser;

import com.regnosys.rosetta.symbols.diagnostics.RDiagnostic;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;
import java.util.Objects;

/**
 * Result of parsing a {@code .rosetta} source file.
 *
 * <p>Two diagnostic surfaces are populated in lockstep:
 * <ul>
 *   <li>{@link #errors()} — legacy {@code List<String>} in {@code line:col message}
 *       format (col 0-indexed). {@code @Deprecated(forRemoval=false)} since
 *       version 0.1.0; preserved for back-compat. Migrate to
 *       {@link #diagnostics()}.</li>
 *   <li>{@link #diagnostics()} — structured {@code List<RDiagnostic>} carrying
 *       typed {@link com.regnosys.rosetta.parser.diagnostics.RuneErrorCode}
 *       (RUNE-001..099 band) + source range + severity + category. Preferred
 *       surface for new consumers.</li>
 * </ul>
 *
 * <p>{@link com.regnosys.rosetta.symbols.diagnostics.ParserDiagnostic#toLegacyString()}
 * is the byte-equal inverse of the legacy formatter — round-trip provable by construction.
 *
 * <p>Spec: §6.5 in {@code docs/superpowers/specs/2026-04-26-p1.4.1-internal-hardening-design.md}.
 *
 * <p>Manifest entry: U006 (structured diagnostics).
 */
public record RosettaParseResult(
        ParseTree tree,
        List<String> errors,
        List<RDiagnostic> diagnostics) {

    public RosettaParseResult {
        Objects.requireNonNull(tree, "tree");
        errors = errors == null ? List.of() : List.copyOf(errors);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    /**
     * Backward-compatible 2-arg constructor preserved for pre-P1.4.1c callers.
     * Delegates to the canonical 3-arg constructor with empty {@code diagnostics}.
     *
     * @deprecated since 0.1.0. New code should use the 3-arg canonical constructor
     * to populate both diagnostic surfaces. This 2-arg form keeps the legacy
     * {@code List<String>}-only call sites compiling unchanged across the
     * P1.4.1c structural change. See {@code docs/upgrades/U006-structured-diagnostics.md}.
     */
    @Deprecated(since = "0.1.0", forRemoval = false)
    public RosettaParseResult(ParseTree tree, List<String> errors) {
        this(tree, errors, List.of());
    }

    /**
     * @deprecated since 0.1.0. New consumers should use {@link #diagnostics()},
     * which provides structured {@link RDiagnostic} records with typed
     * {@link com.regnosys.rosetta.parser.diagnostics.RuneErrorCode} payloads.
     * This accessor is preserved for backward compatibility and continues to
     * populate via
     * {@link com.regnosys.rosetta.symbols.diagnostics.ParserDiagnostic#toLegacyString()}.
     * See {@code docs/upgrades/U006-structured-diagnostics.md}.
     */
    @Override
    @Deprecated(since = "0.1.0", forRemoval = false)
    public List<String> errors() {
        return errors;
    }
}
