package com.regnosys.rosetta.symbols.symbolid;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.symbols.StaleSymbolIdException;
import com.regnosys.rosetta.symbols.SymbolId;
import com.regnosys.rosetta.symbols.SymbolResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Test-only mirror of the {@code rune-parser} {@code TestSymbolResolver}.
 * Copied here because {@code TestSymbolResolver} lives in {@code rune-parser}'s
 * test sources (not a published test-jar), so it cannot be depended on directly
 * from {@code rune-java-generator} tests.
 *
 * <p>Kept intentionally minimal — only the methods used by generator tests.
 * If the rune-parser version changes materially, update this copy in the same
 * commit (both classes are exercised by their own tests).
 *
 * <p>Per P1.4.1b Task 4.1 migration of {@code rune-java-generator} test sites.
 */
public final class GeneratorTestSymbolResolver implements SymbolResolver {

    private static final long TEST_GENERATION = 1L;
    private final Map<SymbolId, RNode> entries = new HashMap<>();

    @Override public long generation() { return TEST_GENERATION; }

    @Override
    public <T extends RNode> T resolve(SymbolId id, Class<T> expected) {
        Objects.requireNonNull(expected, "expected must not be null");
        if (id == null) return null;
        if (id.generation() != TEST_GENERATION) {
            throw new StaleSymbolIdException(id, TEST_GENERATION);
        }
        RNode target = entries.get(id);
        if (target == null) return null;
        if (!expected.isInstance(target)) {
            throw new IllegalArgumentException(
                    "SymbolId " + id.fqn() + " resolves to "
                            + target.getClass().getSimpleName()
                            + ", but caller expected " + expected.getSimpleName());
        }
        return expected.cast(target);
    }

    public SymbolId idFor(String namespace, String localName) {
        return SymbolId.of(namespace, localName, TEST_GENERATION);
    }

    public GeneratorTestSymbolResolver bind(String namespace, String localName, RNode target) {
        // Fail-fast on null target — a null binding would be indistinguishable
        // from an unbound SymbolId at resolve-time (both return null), silently
        // masking miswired generator test fixtures.
        entries.put(idFor(namespace, localName),
                Objects.requireNonNull(target, "target must not be null"));
        return this;
    }

    /**
     * One-call wiring: attach both nodes, register the target, set the SymbolId
     * on source via the supplied setter. Returns this for fluent chaining.
     */
    public GeneratorTestSymbolResolver wireSuperType(
            RNode source,
            String namespace,
            String localName,
            RNode target,
            Consumer<SymbolId> setter) {
        source.attachToWorkspace(this);
        target.attachToWorkspace(this);
        bind(namespace, localName, target);
        setter.accept(idFor(namespace, localName));
        return this;
    }
}
