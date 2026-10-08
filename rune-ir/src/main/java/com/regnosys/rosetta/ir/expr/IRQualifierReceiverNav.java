package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A METADATA-QUALIFIER read over a LOWERED bare-item member head — {@code partyReference ->
 * reference} / {@code identifier -> scheme} where the head is a disguised bare-item member
 * navigation (an EMPTY-symbol
 * {@link com.regnosys.rosetta.ast.expressions.references.RSymbolReference} the #524
 * admission lowers — the census's uniform {@code headLowers:IRMetaAccess} fact), the head's
 * resolved attribute carries a {@code [metadata …]} annotation, and the nav's feature name IS
 * one of that attribute's metadata qualifiers (never a member — no attribute resolves a
 * qualifier leaf, the {@code RFeatureCall:featureUnresolved.headUnresolved} class) — the #525
 * teach of the #524 wave's own exposed frontier (the #509-banked headUnGate census:
 * {@code noMatch.headLowers:IRMetaAccess.ctMiss.noCt.atRoot.inFunction.typeMissing} — the
 * {@code filter partyReference -> reference exists} ingest-fpml chains + the
 * {@code filter identifier -> scheme = "…"} drr product-taxonomy reads). The #512
 * {@link IRQualifierItemNav} twin covers the ITEM seat (childless — the qualifier read
 * directly off the bound implicit item); this kind is the receiver-carrying sibling for the
 * one-hop-deeper class the childless shape cannot spell (the {@link IRRecordFeatureNav} →
 * {@link IRRecordReceiverNav} relationship at the qualifier seat).
 *
 * <p><strong>Receiver-bearing</strong> (the {@link IRRecordReceiverNav} pattern): the HEAD
 * subtree is a real IR child (it lowers by the arm's own gate — the census's uniform
 * {@code headLowers} fact; a blocked head keeps the honest decline). The qualifier's name is
 * the neutral fact; the FieldWithMetaX/ReferenceWithMetaX qualifier deref (legacy's
 * {@code getExternalReference()} / {@code getMeta().getScheme()}-family composition) is
 * legacy's own render decision (the L-029 split) — NOT modeled here. The node's inferred
 * {@code type} is MISSING BY CONSTRUCTION (the census's {@code typeMissing} column — the
 * engine types no qualifier leaf); no typing gate applies, because the kind is served only
 * BY IDENTITY and no render ever reads it.
 *
 * <p>The DISTINCT kind is the safety (the #499 law): the consumers that admit it are NAMED —
 * the equality-operand and existence-operand gates (the containing {@code exists} /
 * {@code = …} filter predicates) — and every claim root that carries it renders through the
 * compiler's oracle-root serve (the shared {@code containsOracleLeaf} walk; an at-root claim
 * serves through the standing {@code navChain} dispatch leg — the literal
 * {@code super.visitFeatureCall} line), byte-identical BY IDENTITY, while the kind itself has
 * NO leaf-emitter arm (a native compose can never reach it — the routing safety).
 *
 * <p>Fork-authored (PR #525); not present upstream.
 */
public record IRQualifierReceiverNav(
        IRExpr receiver,
        String qualifierName,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.QUALIFIER_RECEIVER_NAV;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of(receiver);
    }
}
