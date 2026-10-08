package com.regnosys.rosetta.ast.util;

import com.regnosys.rosetta.ast.RNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Interface-dispatch registry for AST visitor patterns.
 *
 * <p>Replaces ad-hoc {@code instanceof} chains. Register a handler per
 * interface implemented by RNode subclasses; {@link #dispatch(RNode)}
 * routes a node to the matching handler.
 *
 * <p><strong>Multi-interface ambiguity:</strong> if a node implements two or
 * more registered interfaces, {@link #dispatch} throws
 * {@link IllegalStateException}. Multiple-match silently picking one would
 * make handler authors unaware of the ambiguity. Callers wanting "first
 * match wins" semantics should use a single combined interface or a
 * wrapper.
 *
 * <p>Per audit hook H9 in
 * the development audit "p1-parser-architectural-audit" (local). Manifest
 * entry U004 in {@code docs/upgrades/U004-ast-interface-registry.md}.
 */
public final class AstInterfaceRegistry<T> {

    private final Map<Class<?>, Function<RNode, T>> handlers = new LinkedHashMap<>();

    /**
     * Register a handler for nodes implementing {@code iface}.
     *
     * <p><strong>Handler contract:</strong> handlers MUST return a non-null
     * value. {@code null} is reserved as the "no registered interface
     * matches" signal — see {@link #dispatch(RNode)}. If a caller needs an
     * "absent value" semantic, parameterise the registry with
     * {@code Optional<X>} and have handlers return {@code Optional.empty()}.
     *
     * <p><strong>Duplicate-registration is fail-loud.</strong> Calling
     * {@code register} twice with the same {@code iface} throws
     * {@link IllegalStateException} — silently overwriting the first
     * handler would change dispatch behaviour without a visible signal.
     * If a caller genuinely needs to swap a handler, build a fresh
     * registry.
     *
     * @return this, for fluent chaining
     * @throws IllegalStateException if {@code iface} is already registered
     */
    public <I> AstInterfaceRegistry<T> register(Class<I> iface, Function<I, T> handler) {
        Objects.requireNonNull(iface, "iface must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        if (handlers.containsKey(iface)) {
            throw new IllegalStateException(
                    "Handler for " + iface.getSimpleName() + " is already registered. "
                            + "Build a fresh AstInterfaceRegistry to swap handlers.");
        }
        handlers.put(iface, n -> {
            T result = handler.apply(iface.cast(n));
            if (result == null) {
                throw new IllegalStateException(
                        "Handler for " + iface.getSimpleName() + " returned null. "
                                + "Handlers MUST return non-null; null is reserved for "
                                + "\"no matching handler\". Use Optional<T> if absence "
                                + "is a valid result.");
            }
            return result;
        });
        return this;
    }

    /**
     * Dispatch a node to its registered handler.
     *
     * <p>Returns the handler's non-null result, or {@code null} if no
     * registered interface matches the node. Per the {@link #register
     * register} contract, handlers must not return {@code null}; if one
     * does, dispatch fails fast with {@link IllegalStateException}.
     *
     * @return the handler's result, or {@code null} if no handler matches
     * @throws IllegalStateException if more than one registered interface
     *         matches, or if the matching handler returns {@code null}
     *         (handler-contract violation)
     */
    public T dispatch(RNode node) {
        Objects.requireNonNull(node, "node must not be null");
        List<Map.Entry<Class<?>, Function<RNode, T>>> matches = handlers.entrySet().stream()
                .filter(e -> e.getKey().isInstance(node))
                .toList();
        if (matches.size() > 1) {
            throw new IllegalStateException(
                    "Multiple handlers match " + node.getClass().getSimpleName() + ": "
                            + matches.stream()
                                    .map(e -> e.getKey().getSimpleName())
                                    .collect(Collectors.joining(", ")));
        }
        return matches.isEmpty() ? null : matches.get(0).getValue().apply(node);
    }

    /**
     * @return number of registered handlers
     */
    public int size() {
        return handlers.size();
    }
}
