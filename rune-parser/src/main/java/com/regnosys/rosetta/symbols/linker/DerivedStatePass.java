package com.regnosys.rosetta.symbols.linker;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.symbols.derived.*;
import com.regnosys.rosetta.symbols.diagnostics.Diagnostics;

import java.util.List;

/**
 * Pass 3 — applies the four derived state rules (D9) to every file.
 * Runs BEFORE the resolution passes so they see the complete AST
 * including injected synthetic nodes.
 *
 * <p>Spec: D7 (pass 3) + D9 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
public final class DerivedStatePass implements LinkerPass {

    private static final List<DerivedStateRule> RULES = List.of(
        new DefaultElseRule(),
        new DefaultJoinSeparatorRule(),
        new ImplicitVariableRule(),
        new GeneratedInputRule());

    public void run(List<RModel> files, Diagnostics collector) {
        for (RModel file : files) {
            for (DerivedStateRule rule : RULES) {
                rule.apply(file, collector);
            }
        }
    }
}
