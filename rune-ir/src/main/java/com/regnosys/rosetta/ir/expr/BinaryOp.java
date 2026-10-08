package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A binary operator expression (design §3 Group C) — the <strong>first structural</strong>
 * {@link IRExpr} node, whose {@link #children()} (the two operand subtrees) are load-bearing:
 * unlike a leaf, the IR <em>tree</em> drives emission, the operands recursing through the
 * adapter and emitter exactly as a list literal's elements do.
 *
 * <p><strong>Scope.</strong> {@link BinOp} carries the ordered <em>comparisons</em>
 * ({@code <}, {@code >}, {@code <=}, {@code >=}), the <em>equalities</em> ({@code =},
 * {@code <>}) and the <em>logical</em> operators ({@code and}, {@code or}); it grows to the
 * arithmetic operators as those families land (each an additive member + an emitter arm). All
 * three families so far yield a present, single boolean ({@code ComparisonResult}), so
 * {@code optionality} is {@code PRESENT}.
 *
 * <p>{@code left} = {@code child(0)}, {@code right} = {@code child(1)} — a fixed two-slot
 * order matching the AST {@code RBinaryExpression.children()} (left-then-right) and the
 * structural-path NodeId child-index convention. For comparison/equality the cardinality
 * modifier ({@code all}/{@code any}) is not modelled yet: a node carries only the operator's
 * <em>default</em> cardinality — {@code CardinalityOperator.All} for every comparison/equality
 * operator except {@code <>} ({@code NEQ}), which defaults to {@code Any} — derived in the
 * emitter from {@link #op()}, an explicit source modifier deferred at the adapter. The logical
 * operators take no cardinality (they chain {@code ComparisonResult}s).
 *
 * <p>Lab-authored Phase-2; not present upstream.
 */
public record BinaryOp(
        BinOp op,
        IRExpr left,
        IRExpr right,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    /**
     * The binary operator. Carries the four ordered comparisons ({@code LT}/{@code GT}/
     * {@code LTE}/{@code GTE}), the two equalities ({@code EQ}/{@code NEQ}) and the two logical
     * operators ({@code AND}/{@code OR}); later waves add the arithmetic operators (each with its
     * own emitter arm), keeping the IR language-neutral (no rune-AST operator enum is imported).
     * The Java-specific lowering shape — a static {@code ExpressionOperatorsNullSafe} call with a
     * cardinality default for comparison/equality, a {@code ComparisonResult} instance-method
     * chain for logical, a {@code MapperMaths.<R,O,O>method} call for arithmetic — is the emitter's
     * concern, not a member of this neutral enum.
     *
     * <p>The arithmetic operators ({@code ADD}/{@code SUB}/{@code MUL}/{@code DIV}) yield a NUMBER,
     * not a {@code ComparisonResult}, so they do not compose through the logical operators
     * (the adapter's {@code producesComparisonResult} excludes them).
     */
    public enum BinOp { LT, GT, LTE, GTE, EQ, NEQ, AND, OR, ADD, SUB, MUL, DIV }

    @Override
    public IRExprKind kind() {
        return IRExprKind.BINARY_OP;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of(left, right);
    }
}
