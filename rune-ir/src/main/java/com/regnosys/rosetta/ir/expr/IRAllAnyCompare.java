package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * An equality or comparison carrying an explicit {@code all}/{@code any} cardinality modifier —
 * {@code xs any = y} / {@code xs all <> z} — the #503 arm-A teach of the {@code allAnyModifier}
 * face (1,166 sole at the #502 SOT across BOTH binary mirrors; the census read the population
 * ~96% {@code eq.ANY.EQ.low2} — both operands independently lowerable — with the ALL.EQ/ALL.NEQ
 * and comparison tails, 100% typed). The neutral facts carried: {@link #op()} (the operator
 * name) and {@link #modifier()} ({@code ALL}/{@code ANY}).
 *
 * <p><strong>Deliberately SHALLOW</strong> (the #501 {@link IRSymbolNav} pattern):
 * {@link #children()} is empty — the operands are NOT carried as IR children, because legacy
 * renders the whole modified comparison through its own operator family
 * ({@code areEqual/notEqual/…} with the modifier threaded as the {@code CardinalityOperator}
 * argument — the unmodified render's implicit default made explicit), and that operator
 * selection plus the operand composition is one legacy line only legacy can decide. The
 * DISTINCT kind is the safety (the #499 law): no native consumer gate admits it silently — the
 * #503 {@code producesComparisonResult} admission names it explicitly (the census's dominant
 * interior.RLogicalExpr consumers) — and the Java emitter routes every containing claim root
 * through the compiler's oracle-root serve (the standing equality/comparison dispatch legs —
 * {@code super.visitEquality}/{@code super.visitComparison}, the literal legacy lines), while
 * this kind itself has NO leaf-emitter arm (a native compose can never reach it).
 *
 * <p>Optionality is NORMALIZED to {@code PRESENT} (the #500–#502 shallow-kind convention: the
 * seat carries no optionality semantics of its own, and optionality() is serialized — a
 * builder-happenstance split would fork non-Java consumers on nothing).
 *
 * <p>Fork-authored (PR #503); not present upstream.
 */
public record IRAllAnyCompare(
        String op,
        String modifier,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.ALL_ANY_COMPARE;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
