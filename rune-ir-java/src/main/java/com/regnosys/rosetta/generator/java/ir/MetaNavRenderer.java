package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.ir.expr.IRMetaAccess;

/**
 * Renders a meta-feature navigation ({@link IRMetaAccess} — a {@code receiver -> feature} hop whose
 * resolved attribute carries a {@code [metadata …]} annotation) — the #499 conversion of the L-109d
 * relabel belt's convertible slice. The Java render is the {@code FieldWithMetaX}/
 * {@code ReferenceWithMetaX} wrapper family: the {@code MapperS<FieldWithMetaX>} retype, the
 * {@code "Type coercion"} value-deref steps and the per-kind witness/getter choices are gm-aware
 * Java-emission decisions the L-029 split keeps legacy-side, so re-deriving ANY of it emitter-side
 * would be drift surface.
 *
 * <p>The compiler ({@link IRExpressionCompiler}) serves the node on the ROOT-SITE FAST PATH (the
 * #494 root-only law: no subtree pre-walk, no site map): the builder captures the claim root's own
 * meta-annotated {@code RFeatureCall}, the renderer serves the node ONLY when its source range
 * equals that root's ({@code Objects.equals} — null-tolerant, sound BY ROUTING exactly as the #498
 * {@link RuleApplyRenderer}: the slot is INSTALLED only when the claim root IS the meta navigation,
 * and at that root the emitter whole-renders through this oracle BEFORE its walk could reach any
 * interior node — a NESTED meta hop under any OTHER claim root finds a null slot and that claim
 * declines at the emitter's null-renderer gate, the leafEmitter seat), and the render is
 * {@code super.visitFeatureCall(site, ctx)} — the LITERAL relabel-belt line (the L-109d
 * delegation IS {@code super.visitFeatureCall(metaFc, ctx)} verbatim), same method, same
 * dispatch, same receiver/interior compiles re-entering this IR compiler through the oracle's own
 * recursion — so the render is byte-identical to the belt it converts by the strongest argument. A
 * non-correlated node returns {@code null} (→ the claim declines to legacy at the emitter — never
 * render a site the correlation cannot prove).
 *
 * <p>Fork-authored (PR #499); not present in the lab tree.
 */
@FunctionalInterface
public interface MetaNavRenderer {

    /**
     * The complete legacy render of this meta-feature navigation, or {@code null} when the node
     * cannot be correlated to the captured claim-root site (→ the emitter declines to legacy).
     */
    JavaStatementBuilder render(IRMetaAccess metaAccess);
}
