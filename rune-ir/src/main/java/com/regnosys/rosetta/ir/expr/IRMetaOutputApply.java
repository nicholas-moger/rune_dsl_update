package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A FUNCTION call whose callee OUTPUT is {@code [metadata …]}-annotated — the #502 arm-A teach
 * of the {@code calleeMetaOutput} face (406 sole at the #501 SOT; 382 atRoot / 33 interior
 * node-unit at the census, 100% FUNCTION callees, every argument independently lowerable, six
 * qualifier spellings scheme/location/reference/address/id/id+scheme). The neutral fact
 * carried: {@link #calleeName()}.
 *
 * <p><strong>Deliberately SHALLOW</strong> (the #501 {@link IRSymbolNav} pattern):
 * {@link #children()} is empty — the arguments are NOT carried as IR children, because a deep
 * args-carrying form would invite native argument consumption while the call's VALUE is the
 * meta-wrapped output legacy alone knows how to thread (the gm-aware coercions — the L-029
 * split). The DISTINCT kind is the safety (the #499 law): NO consumer admission names it BY
 * DESIGN — the routing argument, not a corpus claim: an interior occurrence declines its
 * containing claim honestly at the kind-dispatch end (or the containing root serves whole
 * through its standing oracle slot) — and the Java emitter serves a claim root through the
 * compiler's oracle-root dispatch ({@code super.visitSymbolReference} — the literal legacy call
 * line, {@code MapperS.of(callee.evaluate(…))} with the meta output threading intact), while
 * this kind itself has NO leaf-emitter arm.
 *
 * <p>Optionality is NORMALIZED to {@code PRESENT} (the #500/#501 shallow-kind convention: the
 * seat carries no optionality semantics of its own, and optionality() is serialized — a
 * builder-happenstance split would fork non-Java consumers on nothing).
 *
 * <p>Fork-authored (PR #502); not present upstream.
 */
public record IRMetaOutputApply(
        String calleeName,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.META_OUTPUT_APPLY;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
