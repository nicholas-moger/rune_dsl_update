package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * An element-collecting postfix operation — {@code <arg> max} / {@code <arg> min} /
 * {@code <arg> sort}, each with an optional comparison-key body ({@code <arg> max [key]})
 * — the #513 noAdaptArm-sweep teach of the {@code RMaxExpr} + {@code RMinExpr} +
 * {@code RSortExpr} families ({@code noAdaptArm} 13 + 3 + 1 sole and 86 + 37 + 10
 * untargeted root visits at the #512 SOT; the standing censuses read the class at
 * listLit-ELEMENT, lambda-RECEIVER, implicit-binder and root seats, drr-dominant). One kind
 * for the three operators (the {@link IRListOp} flavour precedent) — and deliberately NOT
 * new {@link IRListOp} flavours: the #511 receiver/operand admissions are IRListOp-KIND-WIDE
 * ({@code isNonCountListOp}, the collapse shape legs), so a collect flavour there would ride
 * into proven gates unexamined (the #499 distinct-kind law is exactly this safety).
 *
 * <p>Deliberately SHALLOW (the #500 {@link IRPipe} family-arm pattern): the operator and the
 * body's presence are the neutral facts; the argument and key-body subtrees are NOT carried
 * as IR children, and the whole render (legacy's {@code CollectionHandler} min/max/sort
 * composition — the comparator lambda, the {@code MapperListOf...} wraps) is legacy's own
 * (the L-029 split): a claim ROOT serves through the compiler's {@code maxOp}/{@code minOp}/
 * {@code sortOp} oracle legs (the literal {@code super.visitMax}/{@code super.visitMin}/
 * {@code super.visitSort} lines), whose legacy re-walk visits every interior node at its own
 * seat (the #504 re-entrant law). The DISTINCT kind is the safety (the #499 law): the
 * consumers that admit it are NAMED — the equality/comparison/existence operand gates, the
 * call-arg gate and the nav-receiver gate — and every containing claim root renders through
 * the shared {@code containsOracleLeaf} routing, byte-identical BY IDENTITY, while the kind
 * itself has NO leaf-emitter arm.
 *
 * <p>Fork-authored (PR #513); not present upstream.
 */
public record IRCollectOp(
        Op op,
        boolean hasBody,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    /** The collect operator — {@code MAX} from {@code RMaxExpr}, {@code MIN} from {@code RMinExpr}, {@code SORT} from {@code RSortExpr}. */
    public enum Op { MAX, MIN, SORT }

    @Override
    public IRExprKind kind() {
        return IRExprKind.COLLECT_OP;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
