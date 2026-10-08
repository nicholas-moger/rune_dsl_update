package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.symbols.diagnostics.LinkingDiagnostic;
import com.regnosys.rosetta.symbols.diagnostics.RDiagnostic;

import java.util.List;
import java.util.Objects;

/**
 * The public build output. A thin record wrapping {@link RWorkspace}.
 * {@link #diagnostics()} delegates to {@code workspace.diagnostics()},
 * so there is a single source of truth.
 *
 * <p>Spec: D2 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md},
 * D9 in {@code docs/specs/2026-04-09-m6-validation-design.md}.
 */
public record RLinkingResult(RWorkspace workspace) {

    public RLinkingResult {
        Objects.requireNonNull(workspace, "workspace");
    }

    /** All diagnostics (linking + validation). */
    public List<RDiagnostic> diagnostics() {
        return workspace.diagnostics();
    }

    /** M3/M4 linking diagnostics only. */
    public List<LinkingDiagnostic> linkingDiagnostics() {
        return workspace.linkingDiagnostics();
    }

    public boolean isFullyResolved() {
        return workspace.isFullyResolved();
    }
}
