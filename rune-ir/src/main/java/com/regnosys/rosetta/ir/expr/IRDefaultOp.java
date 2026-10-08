package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A {@code default} fallback expression — {@code <left> default <right>} (the value of
 * {@code left} unless absent, then {@code right}) — the #513 noAdaptArm-sweep teach of the
 * {@code RDefaultExpr} family ({@code RDefaultExpr:noAdaptArm} 24 sole + 84 untargeted root
 * visits at the #512 SOT; the standing censuses read the class at ctor-VALUE, lambda-BODY,
 * conditional-THEN and then-BODY seats across cdm6 + both drr cells).
 *
 * <p>Deliberately SHALLOW (the #500 {@link IRPipe} family-arm pattern): the node carries NO
 * extra facts and NO IR children — the operand subtrees stay raw, and the whole render
 * (legacy's null-safe default composition) is legacy's own (the L-029 split): a claim ROOT
 * serves through the compiler's {@code defaultOp} oracle leg (the literal
 * {@code super.visitDefault} line), whose legacy re-walk visits every interior node at its
 * own seat (the #504 re-entrant law). The DISTINCT kind is the safety (the #499 law): the
 * consumers that admit it are NAMED — the equality/comparison/existence operand gates, the
 * call-arg gate and the nav-receiver gate — and every containing claim root renders through
 * the shared {@code containsOracleLeaf} routing, byte-identical BY IDENTITY, while the kind
 * itself has NO leaf-emitter arm.
 *
 * <p>Fork-authored (PR #513); not present upstream.
 */
public record IRDefaultOp(
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.DEFAULT_OP;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
