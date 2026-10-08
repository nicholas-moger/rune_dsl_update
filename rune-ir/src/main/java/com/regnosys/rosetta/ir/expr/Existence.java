package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * An existence / absence check (design §3 Group F) — the first <strong>unary</strong>
 * structural {@link IRExpr} node: {@code <arg> exists}, {@code <arg> is absent}, or the
 * cardinality-qualified {@code single}/{@code multiple} exists. Its single
 * {@link #children() child} (the {@code arg} subtree) is load-bearing — the operand recurses
 * through the adapter and emitter exactly as a {@link BinaryOp}'s operands do.
 *
 * <p>Like the boolean {@link BinaryOp}s, an existence check yields a present, single boolean
 * ({@code ComparisonResult}), so {@code optionality} is {@code PRESENT}: the check is
 * <em>total</em> — an absent operand <em>answers</em> the check ({@code false}/{@code true}), it
 * does not propagate absence. That is what lets an {@link Existence} be a logical {@code and}/
 * {@code or} operand (it renders as a {@code ComparisonResult}, like a comparison).
 *
 * <p>{@code op} ({@link ExistOp}) and the optional {@code modifier} ({@link ExistMod}, {@code null}
 * unless a {@code single}/{@code multiple} qualifier is present) are carried as separate fields,
 * faithfully mirroring the AST {@code RExistenceExpr}; the emitter folds them into the single Java
 * method name ({@code exists}/{@code notExists}/{@code singleExists}/{@code multipleExists}). Both
 * are <strong>neutral</strong> nested enums — no rune-AST enum is imported, preserving IRExpr
 * neutrality (cf. {@link BinaryOp.BinOp}).
 *
 * <p>Lab-authored Phase-2; not present upstream.
 */
public record Existence(
        ExistOp op,
        ExistMod modifier,
        IRExpr arg,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    /** The existence operator — {@code exists} ({@code EXISTS}) or {@code is absent} ({@code ABSENT}). */
    public enum ExistOp { EXISTS, ABSENT }

    /** The optional cardinality qualifier on an {@code exists} check ({@code single}/{@code multiple}). */
    public enum ExistMod { SINGLE, MULTIPLE }

    @Override
    public IRExprKind kind() {
        return IRExprKind.EXISTENCE;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of(arg);
    }
}
