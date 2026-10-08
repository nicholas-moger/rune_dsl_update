package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A lambda-bodied collection operation — {@code <receiver> extract <body>} / {@code <receiver>
 * filter <body>} — the #496 monster wave's leg-1 teach of the two biggest untargeted families
 * (3,140 + 2,014 events at the #495 census). One kind for both operators (they share the exact
 * AST shape: a receiver, an optional named binder, a body): the flavour is {@link #op()}, the
 * {@link IRListOp} precedent for flat postfix operators extended to the lambda-bodied pair.
 *
 * <h2>Deliberately DEEP (the #495 conditional convention, not the #494 shallow one)</h2>
 * {@code children()} are {@code [receiver, body]}, both load-bearing and D1 all-or-nothing: if
 * either subtree does not adapt, the whole node stays declined — never a half-built lambda that
 * would disagree with the legacy emitter on binding context. The #496 lambdaVisit probe sized the
 * deep gate BEFORE the arm (bodyLower 2,802 of 5,154 = 54.4%, above the wave's armed
 * falsifiability bar). The body's interior {@code item} references bind through the adapter's
 * L-080 filter/extract-binder machinery over the RAW AST parent chain — position-independent, so
 * the child-position recursion reads exactly what the probe's root re-adapt read (the two named
 * one-level divergences aside — see the adapter's body-slot admission).
 *
 * <h2>{@code binderName} is the neutral binder fact</h2>
 * The single declared closure parameter's name ({@code x extract a [ .. a .. ]}), or {@code null}
 * for an implicit-or-paramless lambda (the {@code item} binding — the L-080 class). A lambda with
 * MORE than one declared parameter never lowers (the adapter's {@code multiParamBinder} belt —
 * the extract/filter grammar carries at most one).
 *
 * <h2>Cardinality/optionality — the neutral facts (render-inert at the Java target)</h2>
 * EXTRACT: cardinality is the D4-style join ({@code MULTI} if the receiver or the body is
 * {@code MULTI} — a multi receiver maps per element, a single receiver takes the body's own
 * form); optionality is the absorbing rule ({@code OPTIONAL} if either child is). FILTER:
 * cardinality is the RECEIVER's (a predicate never changes element multiplicity); optionality is
 * {@code OPTIONAL} unconditionally (a filter can reject every element — the absent-value
 * absorbing rule, mirroring the else-less conditional).
 *
 * <p>At the Java target the ENTIRE render — legacy {@code CollectionHandler}'s
 * {@code mapItem}/{@code filterItemNullSafe} lambda forms, the named-parameter vs {@code item}
 * lexeme, the receiver-cardinality {@code MapperS}/{@code MapperC} split — is a
 * <strong>Java-emission decision</strong> kept off this neutral node (the L-029 split): the
 * emitter delegates to the compiler's range-correlated {@code LambdaOpRenderer}, which calls
 * {@code super.visitExtract(site, ctx)} / {@code super.visitFilter(site, ctx)} on the
 * source-range-correlated raw node — the LITERAL legacy fallback, same method, same handler
 * instance, same arguments, byte-identical to the decline path by the strongest argument. A
 * future Python/Rust emitter renders the lambda natively from the two children + the binder fact.
 *
 * <p>Fork-authored (PR #496); not present upstream.
 */
public record IRLambdaOp(
        Op op,
        IRExpr receiver,
        String binderName,
        IRExpr body,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    /** The operator flavour — which lambda-bodied collection operation this node is. */
    public enum Op {
        /** {@code receiver extract body} — map each element through the body. */
        EXTRACT,
        /** {@code receiver filter body} — keep the elements the body predicate accepts. */
        FILTER
    }

    @Override
    public IRExprKind kind() {
        return IRExprKind.LAMBDA_OP;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of(receiver, body);
    }
}
