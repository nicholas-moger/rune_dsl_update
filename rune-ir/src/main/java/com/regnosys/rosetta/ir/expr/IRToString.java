package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A {@code to-string} conversion over a single receiver subtree (design §3 conversions) —
 * {@code <child> to-string} — whose single {@link #children() child} is load-bearing and recurses
 * through the adapter and emitter exactly as an {@link IRListOp} child does. A <strong>neutral</strong>
 * conversion-to-text fact: it records "render {@code child} as text", not any target form (Python
 * {@code str(x)}, Rust {@code x.to_string()}, Morphir) — the {@code "to-string"} lexeme is a render decision.
 *
 * <p>At the Java target the legacy render is {@code <child>.map("to-string", Object::toString)} for a generic
 * source and {@code <child>.map("to-string", <SourceEnum>::toDisplayString)} for an enum source. The mapping
 * function (the source-enum detection via {@code NavigationHandler.leafEnumeration} etc.), the meta-unwrap
 * ({@code coerceNavigationReceiver}) and the source-enum import are <strong>Java-emission decisions</strong>
 * kept OFF this neutral node — the emitter reproduces them by reusing the legacy {@code ConversionHandler}
 * oracle verbatim (the L-029/L-050 split, mirroring {@link IRListOp}'s {@code CollectionHandler} reuse), so a
 * future Python/Rust emitter renders from {@code child} directly.
 *
 * <p>The adapter admits this node ONLY when the {@code child} subtree itself lowers (an alias / implicit
 * {@code item} / call receiver does not — the shortcut-type-inference gap, L-032), so the IR
 * genuinely represents a fully-lowered conversion subtree (the L-050 child-lowering / neutrality gate).
 *
 * <p>Lab-authored Phase-2; not present upstream.
 */
public record IRToString(
        IRExpr child,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.TO_STRING;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of(child);
    }
}
