package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;

import java.util.Optional;

/**
 * Produces the Java render of a {@code to-string} conversion ({@link com.regnosys.rosetta.ir.expr.IRToString})
 * by reusing the legacy {@code ConversionHandler.handle(RToStringExpr)} oracle VERBATIM —
 * {@code <arg>.map("to-string", Object::toString)} (or {@code <SourceEnum>::toDisplayString} for an enum source).
 *
 * <p>Reproducing the wrap lab-side would mean re-deriving the source-enum detection
 * ({@code NavigationHandler.leafEnumeration} / {@code inferredEnumeration} / {@code implicitItemEnumeration} +
 * the alias-strip subtlety), the {@code coerceNavigationReceiver} meta-unwrap, and the source-enum import — a
 * parity risk the L-029 split forbids. So {@link IRExpressionCompiler} captures the AST node + the live
 * {@code ExpressionContext} and renders through {@code ConversionHandler.handle(...)} on the compiler's own
 * handler instance (the exact instance the legacy fallback would use), making the conversion byte-identical to
 * Path-1 by construction — the same verbatim-oracle reuse the L-050 {@link CollectionOpRenderer} uses. The
 * emitter therefore stays free of any direct dependence on the AST / {@code ExpressionCompiler}, depending only
 * on this focused interface (the L-029 split).
 *
 * <p>Installed by the compiler only when the adapter admitted the {@link com.regnosys.rosetta.ir.expr.IRToString}
 * — i.e. only when the receiver subtree itself lowered (an alias / implicit {@code item} / call
 * receiver does not, the L-032 gap), so the IR genuinely represents a fully-lowered conversion subtree. A
 * {@code null} renderer makes the conversion decline to legacy.
 *
 * <p>Lab-authored Phase-2; not present upstream.
 */
@FunctionalInterface
public interface ToStringRenderer {

    /**
     * The full {@code to-string} render for the captured AST node, or {@link Optional#empty()} when it cannot be
     * produced (&rarr; the conversion declines to legacy).
     */
    Optional<JavaStatementBuilder> render();
}
