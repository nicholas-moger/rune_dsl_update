package com.regnosys.rosetta.symbols;

import java.util.Objects;

/**
 * Thrown by {@link RWorkspace#resolve(SymbolId, Class)} when the supplied
 * {@link SymbolId}'s {@link SymbolId#generation()} does not match the
 * workspace's current generation token.
 *
 * <p>This indicates the SymbolId was issued by a previous build of a
 * different workspace instance and is being re-used against a fresh build.
 * Callers should re-acquire the ID from the new workspace's accessors
 * rather than caching SymbolIds across builds.
 *
 * <p>Per H7 / U005.
 */
public final class StaleSymbolIdException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final SymbolId symbolId;
    private final long expectedGeneration;

    public StaleSymbolIdException(SymbolId symbolId, long expectedGeneration) {
        super("SymbolId " + Objects.requireNonNull(symbolId, "symbolId must not be null").fqn()
                + " was issued by workspace generation " + symbolId.generation()
                + ", but current workspace generation is " + expectedGeneration);
        this.symbolId = symbolId;
        this.expectedGeneration = expectedGeneration;
    }

    public SymbolId symbolId() { return symbolId; }
    public long expectedGeneration() { return expectedGeneration; }
}
