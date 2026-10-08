package com.regnosys.rosetta.ir.emit;

import com.regnosys.rosetta.ir.expr.IRExpr;

/**
 * A target backend that lowers neutral IR <em>expressions</em> to a target form {@code T}
 * (e.g. {@code String} of Python/Rust source). The contract a new neutral emitter implements and a
 * consumer calls.
 *
 * <p>This is the <strong>expression</strong> SPI. The broad names {@code IREmitter}/{@code IRNodeEmitter}
 * are reserved for a future declaration-emitter layer (over {@code IRNode}/{@code IRKind}).
 *
 * <p>The byte-parity Java emitter ({@code IRJavaLeafEmitter}) deliberately does <em>not</em> implement
 * this interface: it is the deprecation-bound bridge, lives in a downstream module, is frozen, and
 * declines by returning {@code Optional.empty()} to fall back to the legacy AST path — a strangler
 * behaviour a neutral target (which has no legacy to fall back to) does not want.
 *
 * @param <T> the target output form produced by {@link #emit(IRExpr)}
 */
public interface IRExprEmitter<T> {

    /**
     * Lowers a neutral IR expression to this target's form.
     *
     * <p>The argument must be a <strong>canonical adapter-produced</strong> {@link IRExpr}: one whose
     * {@link IRExpr#kind()} matches its concrete record type (the invariant the
     * {@code ExpressionToIRAdapter} always upholds). {@link IRExpr} is an open interface, so a foreign
     * implementation returning a {@code kind()} that does not match its runtime type yields a
     * {@link ClassCastException}; cross-checking foreign inputs is out of scope until external-impl
     * emission is.
     *
     * @param node the expression to lower; must be a canonical adapter-produced node
     * @return the target form; never {@code null}
     * @throws EmitterException if this backend does not lower the given construct
     */
    T emit(IRExpr node);
}
