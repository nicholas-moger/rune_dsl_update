package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A LIBRARY-function application ({@code Min(a, b)} / {@code Max(a, b)} /
 * {@code IsLeapYear(x)} — an args-present {@code RSymbolReference} whose symbol resolves to
 * an {@link com.regnosys.rosetta.ast.types.RLibraryFunction} builtin) — the #520 teach of
 * the {@code calleeNotFunction} face (the #519-SOT calleeGate census read the class 100%
 * {@code RLibraryFunction}, 100% atRoot: 7 two-arg + 3 one-arg per cdm FUNCTION cell — the
 * {@code Min(endDate -> day, 30)} daycount family and the {@code IsLeapYear(...)}
 * alias bodies).
 *
 * <p><strong>Why DISTINCT and childless (the #502 {@link IRMetaOutputApply} pattern
 * verbatim):</strong> the args are NOT carried — a deep form would invite native argument
 * consumption while the call's render is legacy's own external-function threading
 * ({@code new Min().execute(...)}-family — a compose no Mapper-shaped native arm
 * reproduces). The kind has NO leaf-emitter arm and NO consumer admission names it BY
 * DESIGN (the routing argument, not a corpus claim): an at-root claim renders through the
 * compiler's oracle-root callArgs dispatch ({@code super.visitSymbolReference} — the
 * literal legacy call line, the external-function threading intact), an interior
 * occurrence routes its containing claim root whole-legacy through
 * {@code containsOracleLeaf}, and a drift arrival at the leaf emitter declines at the
 * kind-dispatch end — never a native compose.
 *
 * <p>Deliberately SHALLOW: only the callee name is carried; the type/cardinality channels
 * are stamped as read from the node's own engine channels (the #491 convention — the
 * census read the live class 100% {@code typeMissing}: the engine never types the
 * library-call results; nothing reads the channels at render, every route is
 * whole-legacy).
 *
 * <p>Fork-authored (PR #520); not present upstream.
 */
public record IRLibraryApply(
        String calleeName,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.LIBRARY_APPLY;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
