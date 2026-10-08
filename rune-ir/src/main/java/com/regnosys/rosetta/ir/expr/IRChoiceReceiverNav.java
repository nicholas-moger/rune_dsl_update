package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A choice-OPTION selection over a LOWERED receiver — {@code <receiver> -> OptionType} where
 * the receiver's proven form is a CHOICE and the feature segment selects one of the choice's
 * own options BY TYPE NAME — the #529 {@code featureUnresolved.headAttr.declMiss} teach (35
 * cdm6-f sole at the #528 SOT; the #529 headAttrGate census read the pool 100%
 * {@code ht:choice:<name>.qual:noMeta} — Observable 16 / Payout 9 / CollateralCriteria 6 /
 * Instrument 2 / Underlier 2, the {@code observable -> Asset}-family navigations whose head
 * attribute declares a CHOICE type, which the #492 facet's {@code declaredDataTypeOf} ladder
 * can never derive).
 *
 * <p>The RECEIVER subtree is carried (the {@link IRChoiceOptionNav} →
 * {@code IRChoiceReceiverNav} relationship is {@link IRRecordFeatureNav} →
 * {@link IRRecordReceiverNav} at the choice seat — the #525 receiver-carrying twin
 * convention: when a childless kind's class re-arrives one hop deeper, mint the
 * receiver-carrying sibling, never widen the childless shape); the option and choice NAMES
 * are the neutral facts, and the option-selection render (the plain member map, the
 * with-meta coercion legs where the option carries them — legacy's
 * {@code NavigationHandler} choice arms) stays legacy's own render decision (the L-029
 * split). The DISTINCT kind is the safety (the #499 law): the consumers that admit it are
 * NAMED — the #529 chain-hop leg (a deeper option hop composes another
 * {@code IRChoiceReceiverNav} over it), the equality and existence operand gates — and the
 * Java emitter routes every containing claim root through the compiler's oracle-root serve
 * (the shared {@code containsOracleLeaf} walk), so the option machinery is byte-identical BY
 * IDENTITY while this kind itself has NO leaf-emitter arm (a native compose can never reach
 * it — the routing safety).
 *
 * <p>Fork-authored (PR #529); not present upstream.
 */
public record IRChoiceReceiverNav(
        IRExpr receiver,
        String optionName,
        String choiceName,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.CHOICE_RECEIVER_NAV;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of(receiver);
    }
}
