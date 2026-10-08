package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A single-hop feature navigation (design §3 Group B) — {@code receiver -> feature} via the
 * resolved attribute. Its single {@link #children() child} (the {@code receiver} subtree) is
 * load-bearing: the receiver recurses through the adapter and emitter, and this node appends one
 * getter step. This is the first {@link IRExpr} whose emission walks BOTH the receiver subtree and
 * a node-local Java idiom (the {@code .<Witness>map("getX", v -> v.getX())} step).
 *
 * <p><strong>Result facts.</strong> {@code type} is the navigation's result type — for a MULTI feature
 * the ELEMENT type (never a list/collection type), so the {@code <Witness>} generic AND the next hop's
 * lambda-variable element-type name match legacy; this element-type invariant is load-bearing for
 * chain-through-multi byte-parity. The result type is the source of the {@code <Witness>} generic
 * the emitter maps through the translator;
 * {@code featureCardinality} — THIS hop's own declared step cardinality — selects the Java map method
 * ({@code SINGLE} → {@code map}, {@code MULTI} → {@code mapC}), mirroring legacy {@code resolveMapMethod}
 * (the token is per-hop, receiver-agnostic, so a single feature off a multi receiver still renders
 * {@code map}); {@code cardinality} is the ACCUMULATED result cardinality (MULTI if this hop OR any
 * receiver hop is — a monotone multi overlay, design §6 V2-B), read by the operand gates;
 * {@code optionality} is the absorbing-monoid result — OPTIONAL if the resolved
 * feature is optional <em>or</em> the receiver is (any absent hop ⇒ the whole chain is absent,
 * design §6). {@code feature} is the source feature name (→ {@code getFeature} getter); the
 * design's richer {@code FQName} is a later enrichment — the name plus the carried result type are
 * what the Java emission needs.
 *
 * <p>Lab-authored Phase-2; not present upstream.
 */
public record FieldAccess(
        IRExpr receiver,
        String feature,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        ExpressionCardinality featureCardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.FIELD_ACCESS;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of(receiver);
    }
}
