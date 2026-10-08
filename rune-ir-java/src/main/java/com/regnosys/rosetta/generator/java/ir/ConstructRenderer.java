package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.ir.expr.IRConstruct;

/**
 * Renders a type-construction expression ({@link IRConstruct}) — the #494 teach of the biggest
 * untargeted family: {@code Type { field: value, ... }}, whose Java render is the multi-line typed
 * builder block with its coercion/hoist facet family ({@code condListCoerce}, {@code hoistReorder},
 * {@code ctorSetterNumericNarrowChain}, {@code ctor_nested_builder_indent}, the SET/SET_VALUE
 * setter naming — legacy {@code ConstructionHandler}'s years-deep parity surface), or legacy's
 * one-line placeholder where the typed block declines. Re-deriving ANY of that emitter-side would
 * be drift surface (the L-029 split's "irreproducible without the gm-aware legacy walks" class).
 *
 * <p>The compiler ({@link IRExpressionCompiler}) serves the node by SOURCE-RANGE correlation on
 * the ROOT-SITE FAST PATH (the #494 root-only law — the Copilot R4/R5 rounds' shape: no subtree
 * pre-walk, no site map): the builder captures the claim root's own {@code RConstructorExpr}, the
 * renderer serves the node ONLY when its range equals that root's (correlate-or-decline kept via
 * the range-equality check), and the render is {@code super.visitConstructor(site, ctx)} — the
 * LITERAL legacy fallback (legacy {@code ExpressionCompiler.visitConstructor} is one line:
 * {@code constructionHandler.handle(expr, ctx, this)}), same method, same handler instance, same
 * arguments, same virtual dispatch for the per-pair value compiles, so the render is byte-identical
 * to the decline path by the strongest argument. A non-root node returns {@code null} (→ the
 * claim declines to legacy; never render a site the correlation cannot prove). The deep-enrichment
 * wave (CONSTRUCT nodes away from the claim root) must reintroduce the #479
 * range-collision-poisoning subtree index — {@code indexPointFreeSites} is the living template.
 *
 * <p>Fork-authored (PR #494); not present in the lab tree.
 */
@FunctionalInterface
public interface ConstructRenderer {

    /**
     * The complete legacy render of this construction, or {@code null} when the node cannot be
     * range-correlated to a raw constructor site (→ the emitter declines to legacy).
     */
    JavaStatementBuilder render(IRConstruct construct);
}
