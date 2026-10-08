package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A single-hop navigation to a {@code [metadata …]}-annotated feature (#499 — the metaNav
 * conversion): {@code receiver -> feature} where the resolved attribute carries one or more
 * {@code metadata} annotations ({@code scheme}/{@code reference}/{@code address}/{@code location}/
 * {@code id} — the {@link #metaQualifiers} payload, sorted). Structurally the meta twin of
 * {@link FieldAccess}: the single {@link #children() child} (the {@code receiver} subtree) is
 * load-bearing and recurses through the adapter; the node itself represents the meta hop.
 *
 * <p><strong>Why a DISTINCT kind, not a flagged {@link FieldAccess}.</strong> The Java render of a
 * meta hop is the {@code FieldWithMetaX}/{@code ReferenceWithMetaX} wrapper family — the retype +
 * coercion decisions are gm-aware Java-emission facts the neutral IR cannot make (the L-029 split),
 * so the Java emitter serves the claim ROOT by reusing the meta-aware legacy renderer verbatim
 * (the range-correlated {@code MetaNavRenderer} → {@code super.visitFeatureCall} — the ONE line the
 * former L-109d relabel belt was). A flagged {@code FieldAccess} would be silently ADMITTED by every
 * existing receiver/operand/arg gate, wrapping meta-blind native composition around a wrapper-typed
 * value — the exact divergence class the per-hop meta gates exist to prevent. Since the #500
 * consumer admissions the nav/operand/arg/existence gates admit the kind EXPLICITLY, and the
 * distinct kind is what keeps the routing safe: a meta-bearing claim ROOT renders WHOLE through the
 * compiler's oracle-root renderer (the literal {@code super.visit<X>} legacy lines), an interior
 * meta hop under a natively-composed root declines at the emitter's uncorrelated-renderer gate
 * (never a meta-blind native compose), and one under a whole-oracle-rendered root rides the
 * oracle's own recursion.
 *
 * <p><strong>Result facts.</strong> {@code type} is the engine-inferred navigation result (may be
 * MISSING — the #497 census read the seat's typing heavily fragmented, and the oracle render never
 * consults it: the type-blind posture of the delegation family, the #498 rule-apply precedent);
 * {@code featureCardinality} is THIS hop's declared step cardinality, {@code cardinality} the
 * accumulated monotone-multi overlay and {@code optionality} the absorbing-monoid result — the same
 * three laws as {@link FieldAccess}. {@code metaQualifiers} carries the annotation qualifier names
 * sorted ({@code bare} for a qualifier-less {@code [metadata]}) — the render-class discriminant a
 * functional target needs (each kind has its own wrapper/getter family).
 *
 * <p>Lab-authored Phase-2; not present upstream.
 */
public record IRMetaAccess(
        IRExpr receiver,
        String feature,
        List<String> metaQualifiers,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        ExpressionCardinality featureCardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    public IRMetaAccess {
        metaQualifiers = List.copyOf(metaQualifiers);
    }

    @Override
    public IRExprKind kind() {
        return IRExprKind.META_ACCESS;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of(receiver);
    }
}
