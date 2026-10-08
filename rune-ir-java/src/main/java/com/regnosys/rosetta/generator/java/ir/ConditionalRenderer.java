package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.ir.expr.IRConditional;

/**
 * Renders a conditional ({@link IRConditional}) — the #495 teach of the census-marked
 * adapter-ready family: {@code if <cond> then <then> (else <else>)?}, whose Java render is the
 * ANF/hoist statement family ({@code ifThenElseResult} locals, the ladder forms, the
 * nested-conditional statement hoists — legacy {@code ControlFlowHandler}'s years-deep parity
 * surface). Re-deriving ANY of that emitter-side would be drift surface (the L-029 split's
 * "irreproducible without the gm-aware legacy walks" class), so this wave the render is WHOLESALE
 * the oracle's.
 *
 * <p>The compiler ({@link IRExpressionCompiler}) serves the node by SOURCE-RANGE correlation on
 * the ROOT-SITE FAST PATH (the #494 construct pattern exactly — no subtree pre-walk, no site
 * map): the builder captures the claim root's own {@code RConditionalExpr}, the renderer serves
 * the node ONLY when its range equals that root's (correlate-or-decline), and the render is
 * {@code super.visitConditional(site, ctx)} — the LITERAL legacy fallback (legacy
 * {@code ExpressionCompiler.visitConditional} routes to
 * {@code controlFlowHandler.handle(expr, ctx, this)}), same method, same handler instance, same
 * arguments, same virtual dispatch for the condition/branch compiles — so the render is
 * byte-identical to the decline path by the strongest argument, statement hoists included (the
 * oracle registers them itself). A non-root node returns {@code null} (→ the claim declines to
 * legacy; never render a site the correlation cannot prove) — a child-position
 * {@link IRConditional} inside another claim's lowered tree therefore declines exactly as
 * pre-#495. A #495-admitted ctor slot is render-inert here: the wholesale oracle render
 * re-enters the seam through the branch compiles, where the constructor claims at its OWN root.
 *
 * <p>Fork-authored (PR #495); not present in the lab tree.
 */
@FunctionalInterface
public interface ConditionalRenderer {

    /**
     * The complete legacy render of this conditional, or {@code null} when the node cannot be
     * range-correlated to the claim root's raw conditional site (→ the emitter declines to
     * legacy).
     */
    JavaStatementBuilder render(IRConditional conditional);
}
