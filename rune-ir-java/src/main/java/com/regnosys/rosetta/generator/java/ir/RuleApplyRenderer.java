package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.ir.expr.IRApply;

/**
 * Renders an args-present rule invocation ({@link IRApply} whose callee reference carries kind
 * {@code RULE} and whose {@code args} are non-empty) — the #498 teach of the {@code <Rule>(<arg>)}
 * call shape (DRR's rule-as-function invocation: dominantly the {@code RFunction.fromRule}/
 * {@code fromReport} wrapper factories' own synthesized per-field calls, plus the source-level
 * cross-namespace rule-to-rule delegations). The Java render is the injected {@code <name>Rule}
 * field receiver + {@code .evaluate(<unwrapped arg>)} with legacy's own receiver derivation
 * ({@code ruleDependencyFieldName} over the {@code RFunction.fromRule} bridge — the #263/#322/#332
 * lineage) and arg unwraps ({@code unwrapForEvaluateArg}) — gm-aware decisions the L-029 split
 * keeps legacy-side, so re-deriving ANY of it emitter-side would be drift surface.
 *
 * <p>The compiler ({@link IRExpressionCompiler}) serves the node on the ROOT-SITE FAST PATH (the
 * #494 root-only law: no subtree pre-walk, no site map): the builder captures the claim root's own
 * args-present rule {@code RSymbolReference}, the renderer serves the node ONLY when its source
 * range equals that root's ({@code Objects.equals} — NULL-TOLERANT, because the dominant face is
 * factory-synthesized outside any source file; the null≡null match is sound BY ROUTING, not by
 * shape-impossibility: the slot is INSTALLED only when the claim root IS the args-present rule
 * reference, and at that root the emitter whole-renders through this oracle BEFORE its walk could
 * reach any interior node — so the renderer is only ever invoked with the root's own node, where
 * the range equality is trivially true, the check standing as defense-in-depth for any future
 * non-root consult. A NESTED args-present rule invocation CAN lower into an argument slot —
 * {@code isSimpleCallArg} admits any {@code IRApply}, and the #498 census read the interior faces
 * live — but under any OTHER claim root the slot is null and that claim declines at the emitter's
 * null-renderer gate, the leafEmitter seat), and the render is
 * {@code getReferenceHandler().handle(site, ctx, this)} — the LITERAL legacy fallback (legacy
 * {@code ExpressionCompiler.visitSymbolReference} is one line: {@code referenceHandler.handle(expr,
 * ctx, this)}), same method, same handler instance, same arguments, same virtual dispatch for the
 * arg compiles (the arg re-enters this IR compiler through the oracle's own {@code compiler.compile}
 * — a NORMAL recursion, byte-transparent), so the render is byte-identical to the decline path by
 * the strongest argument. A non-correlated node returns {@code null} (→ the claim declines to
 * legacy at the emitter — the census's interior residue, the leafEmitter seat; never render a site
 * the correlation cannot prove).
 *
 * <p>Fork-authored (PR #498); not present in the lab tree.
 */
@FunctionalInterface
public interface RuleApplyRenderer {

    /**
     * The complete legacy render of this args-present rule invocation, or {@code null} when the
     * node cannot be correlated to the captured claim-root site (→ the emitter declines to legacy).
     */
    JavaStatementBuilder render(IRApply apply);
}
