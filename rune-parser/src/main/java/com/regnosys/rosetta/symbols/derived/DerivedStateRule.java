package com.regnosys.rosetta.symbols.derived;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.symbols.diagnostics.Diagnostics;

/**
 * One derived state rule (D9). Each rule is a pure structural mutation
 * over the AST — no references introduced, no resolution required.
 *
 * <p>Spec: D9 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
public interface DerivedStateRule {
    void apply(RModel model, Diagnostics collector);
}
