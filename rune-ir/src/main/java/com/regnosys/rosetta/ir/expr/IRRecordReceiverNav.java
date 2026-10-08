package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A RECORD-feature read over a LOWERED compound receiver — {@code <receiver> -> feature} where
 * the receiver subtree is any non-symbol expression whose ENGINE type is a RECORD type
 * ({@code date}/{@code dateTime}/{@code zonedDateTime} — {@code basictypes.rosetta}) and the
 * leaf is one of the record's OWN features ({@code RRecordType.features()} — the #444
 * membership rule) — the #512 arm-B1 teach of the {@code RFeatureCall:featureUnresolved.
 * nonSymbolReceiver} record class (the #509-banked nsrGate census read the class
 * {@code recvLowers:FieldAccess/IRSymbolNav + rec:zonedDateTime/dateTime + mRecHit}, the
 * {@code fpmlAdjustableDate2 -> unadjustedDate -> value -> date}-family chains). The #507
 * {@link IRRecordFeatureNav} twin covers the SYMBOL-head seat (childless, head-keyed); this
 * kind is the receiver-carrying sibling for the compound-receiver class the head-keyed shape
 * cannot spell.
 *
 * <p>Record features are NOT {@code RAttribute}s, so the fork's resolver deliberately binds NO
 * channel for these navs (the #444 clear-without-binding law); the node carries the engine's
 * own inferred type verbatim (the census read the class {@code typed} — no typing gate
 * applies, because the kind is served only BY IDENTITY and no render ever reads it).
 *
 * <p><strong>Receiver-bearing</strong> (the {@link IRDeepFeatureNav} pattern): the RECEIVER
 * subtree is a real IR child (it lowers by the arm's own gate — the census's uniform
 * {@code recvLowers} fact); the record-feature RESOLUTION and render (legacy's record-feature
 * machinery — the {@code toLocalDate()}-family composition) are legacy's own render decisions
 * (the L-029 split), so only the resolved feature's NAME and the record type's name are
 * carried as neutral facts. The DISTINCT kind is the safety (the #499 law): the consumers
 * that admit it are NAMED — the call-arg gate (the census's live {@code interior.
 * RSymbolReference} claim roots) — and the Java emitter routes every containing claim root
 * through the compiler's oracle-root serve (the shared {@code containsOracleLeaf} walk), so
 * the record machinery is byte-identical BY IDENTITY while this kind itself has NO
 * leaf-emitter arm (a native compose can never reach it — the routing safety).
 *
 * <p>Fork-authored (PR #512); not present upstream.
 */
public record IRRecordReceiverNav(
        IRExpr receiver,
        String featureName,
        String recordTypeName,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.RECORD_RECEIVER_NAV;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of(receiver);
    }
}
