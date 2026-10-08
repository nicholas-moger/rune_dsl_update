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
 * Test-only implementation of {@link SymbolResolver}. Holds a manual
 * (namespace, localName) → {@link RNode} mapping for direct-construction
 * tests that don't go through {@code RWorkspace.build()}.
 *
 * <p>Pattern:
 * <pre>{@code
 * TestSymbolResolver resolver = new TestSymbolResolver();
 * RDataType parent = new RDataType();
 * RDataType child = new RDataType();
 * resolver.wireSuperType(child, "test.ns", "Parent", parent, child::setSuperTypeId);
 * assertSame(parent, child.superType().orElseThrow());
 * }</pre>
 */
public final class TestSymbolResolver implements SymbolResolver {

    /**
     * All {@code TestSymbolResolver} instances share generation = 1L.
     * This means SymbolIds emitted by one test resolver instance can be
     * resolved by another test resolver instance — fine for direct-construction
     * tests that don't care about cross-resolver staleness. Tests that DO need
     * cross-build staleness semantics use {@code RWorkspace.build} directly
     * (each call increments the process-wide AtomicLong).
     *
     * <p><b>WARNING — intentionally insecure for testing.</b> Production code
     * relies on {@code RWorkspace}'s monotonic AtomicLong (Task 2) for the SF8
     * staleness invariant. Never use {@code TestSymbolResolver} to test code
     * that asserts cross-build staleness — use {@code RWorkspace.build()}
     * directly so each invocation gets a fresh generation token.
     */
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

    /**
     * Convenience: build a {@link SymbolId} for the given namespace + name
     * using this resolver's generation token.
     */
    public SymbolId idFor(String namespace, String localName) {
        return SymbolId.of(namespace, localName, TEST_GENERATION);
    }

    /**
     * Register a (namespace, localName) → target binding. Returns this for
     * fluent chaining. Re-registering the same key overwrites.
     *
     * <p>Fail-fast on null target — a null binding would be indistinguishable
     * from an unbound SymbolId at resolve-time (both return null), silently
     * masking miswired test fixtures.
     */
    public TestSymbolResolver bind(String namespace, String localName, RNode target) {
        entries.put(idFor(namespace, localName),
                Objects.requireNonNull(target, "target must not be null"));
        return this;
    }

    /**
     * One-call wiring for the most common test pattern: attach both nodes
     * to this resolver, register the target, set the SymbolId on the source
     * via the supplied setter. Returns this for fluent chaining.
     *
     * <p>Despite the name, the helper is generic over any
     * {@code Consumer<SymbolId>} setter — also valid for
     * {@code child::setReferenceId}, {@code attribute::setTypeCallId},
     * {@code config::setRootTypeId}, etc. The "SuperType" suffix reflects
     * the most common Task-4 site of P1.4.1b, not a constraint.
     *
     * @param source the cross-ref-bearing node (e.g., child RDataType)
     * @param namespace target namespace
     * @param localName target local name
     * @param target the resolved node
     * @param setter the SymbolId setter on source (e.g. {@code child::setSuperTypeId})
     */
    public TestSymbolResolver wireSuperType(
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
