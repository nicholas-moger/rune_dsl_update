package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A list-membership test — {@code <left> contains <right>} / {@code <left> disjoint
 * <right>} — the #513 noAdaptArm-sweep teach of the {@code RContainsExpr} +
 * {@code RDisjointExpr} families ({@code RContainsExpr:noAdaptArm} 23 sole + 33 untargeted
 * and {@code RDisjointExpr:noAdaptArm} 2 sole + 2 untargeted root visits at the #512 SOT;
 * the standing censuses read the class at lambda-BODY, listLit-ELEMENT and root seats).
 * One kind for both operators (the {@link IRListOp} flavour precedent) — and deliberately
 * NOT new {@link BinaryOp} operator flavours: the #511 equality/comparison admissions are
 * {@code BinaryOp}-KIND-WIDE, so a membership flavour there would ride into proven-operand
 * gates unexamined (the #499 distinct-kind law is exactly this safety).
 *
 * <p>Deliberately SHALLOW (the #500 {@link IRPipe} family-arm pattern): the operator is the
 * neutral fact; the operand subtrees are NOT carried as IR children, and the whole render
 * (legacy's {@code contains(...)}/{@code disjoint(...)} runtime composition) is legacy's own
 * (the L-029 split): a claim ROOT serves through the compiler's {@code contains}/
 * {@code disjoint} oracle legs (the literal {@code super.visitContains}/
 * {@code super.visitDisjoint} lines), whose legacy re-walk visits every interior node at its
 * own seat (the #504 re-entrant law). The DISTINCT kind is the safety (the #499 law): the
 * consumers that admit it are NAMED — the equality/comparison/existence operand gates, the
 * call-arg gate, the nav-receiver gate and the coerced-boolean logical operand gate (a
 * membership test IS a boolean producer) — and every containing claim root renders through
 * the shared {@code containsOracleLeaf} routing, byte-identical BY IDENTITY, while the kind
 * itself has NO leaf-emitter arm.
 *
 * <p>Fork-authored (PR #513); not present upstream.
 */
public record IRMembershipOp(
        Op op,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    /** The membership operator — {@code CONTAINS} from {@code RContainsExpr}, {@code DISJOINT} from {@code RDisjointExpr}. */
    public enum Op { CONTAINS, DISJOINT }

    @Override
    public IRExprKind kind() {
        return IRExprKind.MEMBERSHIP_OP;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
