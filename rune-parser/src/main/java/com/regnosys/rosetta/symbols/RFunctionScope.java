package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;

import java.util.*;

/**
 * Lexical scope inside a function body. Holds parameter names, the output
 * attribute, shortcuts, and (for dispatch functions) the dispatch parameter.
 * Scratch object — created per resolution walk in pass 5, never retained.
 *
 * <p>Spec: D8 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
public final class RFunctionScope {

    private final RFileScope parent;
    private final Map<String, RNode> locals = new HashMap<>();

    public RFunctionScope(RFileScope parent) {
        this.parent = Objects.requireNonNull(parent);
    }

    public void register(String name, RNode binding) {
        if (name != null && !name.isEmpty()) {
            locals.put(name, binding);
        }
    }

    public Optional<RNode> lookup(String name) {
        RNode local = locals.get(name);
        if (local != null) return Optional.of(local);
        Optional<RRootElement> fileHit = parent.lookup(name);
        return fileHit.map(r -> (RNode) r);
    }
}
