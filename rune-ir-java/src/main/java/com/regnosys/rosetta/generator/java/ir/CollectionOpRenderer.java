package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;

import java.util.Optional;

/**
 * Produces the Java render of a flat postfix list/collection operation
 * ({@link com.regnosys.rosetta.ir.expr.IRListOp} — {@code distinct}/{@code flatten}/{@code first}/
 * {@code last}/{@code reverse}/{@code count}, + {@code only-element} since the #498 teach) by
 * reusing the legacy {@code CollectionHandler} oracle
 * VERBATIM — {@code distinct(<arg>)} / {@code <arg>.first()} / {@code <arg>.resultCount()} /
 * {@code <arg>.get()} (the only-element inline collapse, whose {@code selfUnwrapping}
 * consumer-marker rides the oracle's own {@code JavaExpression}) etc.
 *
 * <p>The flat wrap is a single Java expression, but reproducing it lab-side would mean re-deriving the
 * op&rarr;method mapping, the chain link, and the {@code distinct} static-wildcard import — a parity
 * risk the L-029 split forbids. So {@link IRExpressionCompiler} captures the AST node + the live
 * {@code ExpressionContext} and renders through {@code CollectionHandler.handle(...)} on the compiler's
 * own handler instance (the exact instance the legacy fallback would use), making the operation
 * byte-identical to Path-1 by construction — the same verbatim-oracle reuse the L-049
 * {@code RuleDelegationRenderer} uses. The emitter therefore stays free of any direct dependence on
 * the AST / {@code ExpressionCompiler}, depending only on this focused interface (the L-029 split).
 *
 * <p>Installed by the compiler only when the adapter admitted the {@link IRListOp} — i.e. only when the
 * receiver subtree itself lowered (a param / navigation receiver; an alias / implicit {@code item} /
 * call receiver does not, the L-032 gap), so the IR genuinely represents a fully-lowered
 * collection subtree. A {@code null} renderer makes the operation decline to legacy.
 *
 * <p>Lab-authored Phase-2; not present upstream.
 */
@FunctionalInterface
public interface CollectionOpRenderer {

    /**
     * The full flat-list-op render for the captured AST node, or {@link Optional#empty()} when it
     * cannot be produced (&rarr; the operation declines to legacy).
     */
    Optional<JavaStatementBuilder> render();
}
