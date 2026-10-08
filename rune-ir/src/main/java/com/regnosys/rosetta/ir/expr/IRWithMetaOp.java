package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A with-metadata annotation expression ({@code <arg> with-meta { <entries> }} —
 * {@link com.regnosys.rosetta.ast.expressions.unary.RWithMetaExpr}) — the #515 untargeted-close
 * teach (the family was the untargeted meter's dominant residue: 92 root visits, the LAST
 * big no-IR-attempt family; the #513 seven-family sweep deliberately left it as the
 * meta-channel design question, answered here the same way — the render IS the serve). One
 * kind for the family (the {@link IRDefaultOp} pattern) — deliberately NOT a wrapper node
 * with IR children (the #499 distinct-kind law): the metadata entry semantics (the
 * FieldWithMetaX builder threading, the scheme/reference/id setters) are legacy's own render
 * decisions (the L-029 split).
 *
 * <p><strong>Deliberately SHALLOW</strong>: the entry count is the neutral fact; renders via
 * the {@code withMetaOp} oracle leg (the literal {@code super.visitWithMeta} line —
 * byte-identical BY IDENTITY, the oracle serve re-walking the raw subtree with every
 * interior claiming at its own seat, the #504 re-entrant law); no leaf-emitter arm (a native
 * compose can never reach it — the routing safety).
 *
 * <p>Fork-authored (PR #515); not present upstream.
 */
public record IRWithMetaOp(
        int entryCount,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.WITH_META_OP;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
