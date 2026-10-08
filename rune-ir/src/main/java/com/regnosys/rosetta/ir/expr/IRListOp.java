package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A <strong>flat</strong> postfix list/collection operation over a single receiver subtree (design
 * §3 collections) — {@code <child> distinct}, {@code <child> flatten}, {@code <child> first},
 * {@code <child> last}, {@code <child> reverse}, and {@code <child> count}. Its single
 * {@link #children() child} (the receiver subtree) is load-bearing and recurses through the adapter
 * and emitter exactly as an {@link Existence} operand does.
 *
 * <p>Only the operations whose legacy render is a flat single Java expression are modelled here:
 * {@code distinct} (the prefix runtime fn {@code distinct(arg)}), the member-call wraps
 * {@code arg.first()}/{@code .last()}/{@code .flattenList()}/{@code .reverse()}, {@code count}
 * ({@code arg.resultCount()}), and — since the #498 teach — the self-unwrapping
 * {@code only-element} ({@code arg.get()}, the inline collapse; its {@code selfUnwrapping}
 * consumer-marker is a Java-emission fact riding the oracle's own {@code JavaExpression}, not this
 * neutral node). The hoisting / lambda-bodied collection operations
 * ({@code filter}/{@code extract}/{@code sort}/{@code reduce}/{@code map}/{@code then}) and
 * {@code sum} (whose legacy render carries a deeper diff) are NOT modelled — they stay on the
 * legacy handler (the Wave-6 hoist tier).
 *
 * <p>{@code op} ({@link Kind}) is a <strong>neutral</strong> nested enum — no rune-AST enum is
 * imported, preserving IRExpr neutrality (cf. {@link BinaryOp.BinOp} and {@link Existence.ExistOp}).
 * The adapter admits this node ONLY when the {@code child} subtree itself lowers (an alias / implicit
 * {@code item} / call receiver does not — see the shortcut-type-inference gap, L-032; an
 * only-element receiver lowers since #498, as a nested node of this same kind);
 * the Java render reuses the legacy {@code CollectionHandler} oracle verbatim (the method name, the
 * chain link and the {@code distinct} static-wildcard import are Java-emission decisions kept out of
 * the neutral IR — the L-029/L-049 split), so a future Python/Rust emitter renders from {@code op} +
 * {@code child} directly.
 *
 * <p>Lab-authored Phase-2; not present upstream.
 */
public record IRListOp(
        Kind op,
        IRExpr child,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    /**
     * The flat postfix list/collection operator. {@code COUNT} originates from the AST
     * {@code RCountExpr}; the rest from the flat arms of {@code RListOpExpr}'s {@code ListOp}.
     * {@code SUM} (#519) is the numeric aggregate — it has NO native Java emitter arm (legacy's
     * sum render derives the element-type method inside its own composition): a top-level sum
     * serves through the standing {@code CollectionOpRenderer} slot verbatim, and an interior
     * sum is an ORACLE LEAF ({@code IRExpressionCompiler.containsOracleLeaf}'s SUM-scoped leg),
     * so every containing root renders whole-legacy — the COUNT/collapse kinds keep their
     * standing native composes (the leg keys SUM only).
     */
    public enum Kind { DISTINCT, FLATTEN, FIRST, LAST, REVERSE, COUNT, ONLY_ELEMENT, SUM }

    @Override
    public IRExprKind kind() {
        return IRExprKind.LIST_OP;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of(child);
    }
}
