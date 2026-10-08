package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.ir.expr.IRPointFreeApply;

/**
 * Renders a POINT-FREE function application ({@link IRPointFreeApply}) — the #492 resolution of
 * the L-109 deferral: a bare no-argument function reference used as a value (a call argument
 * {@code Callee(BareF)}, a navigation head {@code BareF -> field}) whose IMPLICIT argument legacy
 * derives from the position's enclosing context (the rule input, the lambda item, the condition
 * instance) — walks a node-local render cannot reproduce (the L-029 split's "irreproducible
 * without the raw parent chain" class).
 *
 * <p>The compiler ({@link IRExpressionCompiler}) serves the node by SOURCE-RANGE correlation: a
 * pre-walk of the claimed AST subtree indexes each bare function reference by its own range (a
 * value-equal record, unique per physical site), and the renderer calls legacy
 * {@code ReferenceHandler.renderImplicitFunctionInvocation(rawNode, callee, ctx, compiler)}
 * verbatim — the SAME public oracle (the D43 seam) the top-level point-free claim reuses, so the
 * implicit-argument derivation, the nav-receiver {@code MapperS.of} wrap (the oracle reads the RAW
 * node's own parent) and the zero-real-input {@code .evaluate()} form are byte-identical to the
 * decline path by the strongest argument. An uncorrelated node returns {@code null} (→ the claim
 * declines to legacy; never guess an implicit binding where the raw site is unknown).
 *
 * <p>Fork-authored (PR #492); not present in the lab tree.
 */
@FunctionalInterface
public interface PointFreeRenderer {

    /**
     * The complete legacy render of this point-free application, or {@code null} when the node
     * cannot be range-correlated to a raw bare-function site (→ the emitter declines to legacy).
     */
    JavaStatementBuilder render(IRPointFreeApply pointFree);
}
