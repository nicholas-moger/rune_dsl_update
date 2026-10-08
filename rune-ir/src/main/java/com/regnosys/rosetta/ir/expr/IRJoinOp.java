package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A list-join expression ({@code <list> join [<separator>]} —
 * {@link com.regnosys.rosetta.ast.expressions.binary.RJoinExpr}) — the #515 untargeted-close
 * teach (2 root visits — the untargeted meter's last small row beside RWithMetaExpr; the
 * #513 sweep deliberately left the family as its fixture-contrast anchor, and the five
 * #513-recut locks move on to the still-unarmed reduce/cardinality-check families with this
 * teach). One kind for the family (the {@link IRCollectOp} pattern) — deliberately NOT a
 * {@link BinaryOp} flavour (the #499 distinct-kind law).
 *
 * <p><strong>Deliberately SHALLOW</strong>: the separator's presence is the neutral fact
 * (the {@link IRCollectOp#hasBody()} precedent — the #513 Copilot R1 catch pinned the
 * separator to its own {@code setSeparator} slot, never {@code rawRight}); renders via the
 * {@code joinOp} oracle leg (the literal {@code super.visitJoin} line — byte-identical BY
 * IDENTITY); no leaf-emitter arm.
 *
 * <p>Fork-authored (PR #515); not present upstream.
 */
public record IRJoinOp(
        boolean hasSeparator,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.JOIN_OP;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
