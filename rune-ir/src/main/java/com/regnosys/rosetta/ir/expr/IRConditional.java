package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.ArrayList;
import java.util.List;

/**
 * A conditional {@code if <condition> then <thenBranch> (else <elseBranch>)?} (design §3 control-flow,
 * §5 hoisting tier) — the first control-flow {@link IRExpr} node. It is a neutral <em>fact</em>: it
 * records the branch structure, never a hoisting strategy or a target form. The Java target lowers it in
 * the {@link com.regnosys.rosetta.ir.expr.anf ANF tier} — a statement-position (SET) conditional hoists
 * into an {@code ifThenElseResult} local ({@code <Type> t = null; if(<cond>){t = <then>;}}; an absent
 * {@code else} means the local stays {@code null}), an operand-position conditional becomes a
 * {@code JoinPoint}; a Python target lowers to a ternary, a Rust target to a {@code let t = if …}.
 *
 * <h2>Children are load-bearing; {@code elseBranch} is optional</h2>
 * {@code children()} are {@code [condition, thenBranch]} plus {@code elseBranch} when present, recursed
 * through the adapter and emitter exactly as a {@link BinaryOp}'s operands are. {@code type()} is the
 * conditional's inferred type — when {@code else} is absent it is the then-branch's type (Rune's
 * absent-else yields the then value or {@code null}), which is exactly the {@code ifThenElseResult}
 * local's declared type at the Java target.
 *
 * <h2>Live driving is the open obligation ("C"); offline validation goes through facts</h2>
 * This node is defined for the live SET-position driving at <strong>C</strong> (subclassing
 * {@code FunctionExpressionRenderer} — the only way to reach the renderer-intercepted SET hoists;
 * decision-log L-053/L-055/L-056). At the vendored #219 pin the dominant corpus carriers have
 * alias-receiver branch interiors whose shortcut type is {@code MISSING} (L-032), so they do not lower
 * to a fully-childed {@code IRConditional}; the Phase-A offline harness therefore validates the hoist
 * <em>skeleton</em> (temp name/number/order + declared type + declare-then-assign shape) from the
 * conditional's {@link com.regnosys.rosetta.ir.expr.anf.ConditionalHoist} facts, not from a fully-lowered
 * node (see {@code notes/wave6-anf-slice1-conditional.md}).
 *
 * <p>Lab-authored Phase-2; not present upstream.
 *
 * @param condition  the boolean condition subtree (non-null)
 * @param thenBranch the value when the condition holds (non-null)
 * @param elseBranch the value when it does not — {@code null} for an {@code else}-less conditional
 *                   (absent {@code else} ⇒ the Java {@code ifThenElseResult} local stays {@code null})
 */
public record IRConditional(
        IRExpr condition,
        IRExpr thenBranch,
        IRExpr elseBranch,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.CONDITIONAL;
    }

    @Override
    public List<? extends IRExpr> children() {
        List<IRExpr> children = new ArrayList<>(3);
        children.add(condition);
        children.add(thenBranch);
        if (elseBranch != null) {
            children.add(elseBranch);
        }
        return children;
    }
}
