package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.RNode;

import java.util.*;

/**
 * Lexical scope inside an inline function expression. Nests under
 * {@link RFunctionScope}; resolves closure parameters first, then falls
 * through to the parent function scope.
 *
 * <p>Spec: D8 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
public final class RInlineFunctionScope {

    private final RFunctionScope parent;
    private final Map<String, RNode> closureLocals = new HashMap<>();

    public RInlineFunctionScope(RFunctionScope parent) {
        this.parent = Objects.requireNonNull(parent);
    }

    public void register(String name, RNode binding) {
        if (name != null && !name.isEmpty()) {
            closureLocals.put(name, binding);
        }
    }

    public Optional<RNode> lookup(String name) {
        RNode local = closureLocals.get(name);
        if (local != null) return Optional.of(local);
        return parent.lookup(name);
    }
}
