package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.ir.expr.IRLambdaOp;

/**
 * Renders a lambda-bodied collection operation ({@link IRLambdaOp}) — the #496 monster wave's
 * leg-1 teach of the two biggest untargeted families: {@code <receiver> extract <body>} /
 * {@code <receiver> filter <body>}, whose Java render is legacy {@code CollectionHandler}'s
 * lambda-form family ({@code mapItem}/{@code mapSingleToItem}/{@code filterItemNullSafe}, the
 * named-parameter vs {@code item} lexeme, the receiver-cardinality {@code MapperS}/{@code MapperC}
 * split, the chain-link line breaks — years-deep parity surface). Re-deriving ANY of that
 * emitter-side would be drift surface (the L-029 split's "irreproducible without the gm-aware
 * legacy walks" class), so this wave the render is WHOLESALE the oracle's.
 *
 * <p>The compiler ({@link IRExpressionCompiler}) serves the node by SOURCE-RANGE correlation on
 * the ROOT-SITE FAST PATH (the #494/#495 pattern exactly — no subtree pre-walk, no site map): the
 * builder captures the claim root's own {@code RExtractExpr}/{@code RFilterExpr}, the renderer
 * serves the node ONLY when its range equals that root's (correlate-or-decline), and the render
 * is {@code super.visitExtract(site, ctx)} / {@code super.visitFilter(site, ctx)} — the LITERAL
 * legacy fallbacks, same method, same handler instance, same arguments, same virtual dispatch for
 * the receiver/body compiles — so the render is byte-identical to the decline path by the
 * strongest argument, the lambda's interior claims included (the oracle's own body compile
 * re-enters the seam, where interiors claim at their own roots). A non-root node returns
 * {@code null} (→ the claim declines to legacy; never render a site the correlation cannot
 * prove) — a child-position {@link IRLambdaOp} inside another claim's lowered tree therefore
 * declines exactly as pre-#496. A #496-admitted ctor body is render-inert here: the wholesale
 * oracle render re-enters the seam through the body compile, where the constructor claims at its
 * OWN root (the #494 nested-ctor law verbatim).
 *
 * <p>Fork-authored (PR #496); not present in the lab tree.
 */
@FunctionalInterface
public interface LambdaOpRenderer {

    /**
     * The complete legacy render of this lambda operation, or {@code null} when the node cannot
     * be range-correlated to the claim root's raw extract/filter site (→ the emitter declines to
     * legacy).
     */
    JavaStatementBuilder render(IRLambdaOp lambdaOp);
}
