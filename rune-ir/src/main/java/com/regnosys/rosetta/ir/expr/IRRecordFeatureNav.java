package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A RECORD-feature read spelled as a single-arrow navigation — {@code head -> feature} where
 * the head names an in-scope function value of a RECORD type ({@code date}/{@code dateTime}/
 * {@code zonedDateTime} — {@code basictypes.rosetta}) and the leaf is one of the record's OWN
 * features ({@code RRecordType.features()} — the #444 membership rule) — the #507
 * {@code REnumValueRef:noResolutionChannel} teach (175 sole at the #506 SOT; the #507
 * noChanGate census read the dominant faces {@code dispatchInput.rec:date.leafRecHit} — the
 * YearFraction-family dispatch bodies reading {@code startDate -> day} on the dispatch-BASE
 * input — and {@code input.rec:date/zonedDateTime.leafRecHit}, the plain-input drr slice).
 *
 * <p>Record features are NOT {@code RAttribute}s, so the fork's resolver deliberately binds NO
 * channel for these navs (the #444 clear-without-binding law) and the node's inferred type is
 * MISSING BY CONSTRUCTION — the mint carries the workspace's sentinel and no typing gate
 * applies (unlike {@link IRChoiceOptionNav}): the kind is served only BY IDENTITY, never
 * emitter-composed, so no render ever reads the type.
 *
 * <p><strong>Deliberately SHALLOW</strong> (the {@link IRSymbolNav} pattern): the two name
 * segments ({@link #headName()}, {@link #featureName()}) and the record type's name
 * ({@link #recordTypeName()}) are the neutral facts; the head's own resolution (a plain
 * input/output by legacy's {@code resolveNameInFunction} order, the dispatch-base scope-join)
 * and the record-feature render (legacy's record-feature machinery behind the disguised-ENR
 * fall-through) are legacy's own render decisions (the L-029 split), so neither is carried as
 * an IR child ({@link #children()} is empty). The DISTINCT kind is the safety (the #499 law):
 * the consumers that admit it are NAMED — the nav-receiver gate, the equality and existence
 * operand gates and the call-arg gate — and the Java emitter routes every containing claim
 * root through the compiler's oracle-root serve (the shared {@code containsOracleLeaf} walk,
 * incl. the #505 {@code enumChain} dispatch leg — the literal {@code super.visitEnumValueRef}
 * line), so the record machinery is byte-identical BY IDENTITY while this kind itself has NO
 * leaf-emitter arm (a native compose can never reach it — the routing safety).
 *
 * <p>Fork-authored (PR #507); not present upstream.
 */
public record IRRecordFeatureNav(
        String headName,
        String featureName,
        String recordTypeName,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.RECORD_FEATURE_NAV;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
