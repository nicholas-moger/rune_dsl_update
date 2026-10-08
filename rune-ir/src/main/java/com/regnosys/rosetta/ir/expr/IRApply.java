package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.ArrayList;
import java.util.List;

/**
 * Application of a {@link #callee()} to zero or more {@link #args()} (Wave-0 family 8,
 * the call case of {@code RSymbolReference} — e.g. {@code MyFunction(a, b)}). In Wave 0
 * the callee is an {@link IRReference} of kind {@code FUNCTION}; the bare rule delegation
 * later added the args-empty {@code RULE}-callee shape, and the #498 teach the args-present
 * one ({@code SomeRule(arg)} — DRR's rule-as-function invocation); richer callees (curried
 * applications, method values) are later-wave concerns.
 *
 * <p>{@link #children()} are the callee followed by the arguments in source order — the
 * order that fixes the operands' {@link NodeId}s. A call is IR-expressible in Wave 0
 * only when the callee and every argument are themselves Wave-0-expressible (the adapter
 * enforces this).
 *
 * <p>Currying, argument coercion and the {@code MapperS.of(fn.evaluate(...))} wrapping
 * are emitter concerns; this node states only the call shape.
 */
public record IRApply(
        IRExpr callee,
        List<IRExpr> args,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    /** Defensively copies the argument list so the node is immutable. */
    public IRApply {
        args = List.copyOf(args);
    }

    @Override
    public IRExprKind kind() {
        return IRExprKind.APPLY;
    }

    /** The callee first, then the arguments in source order. */
    @Override
    public List<? extends IRExpr> children() {
        List<IRExpr> kids = new ArrayList<>(args.size() + 1);
        kids.add(callee);
        kids.addAll(args);
        return List.copyOf(kids);
    }
}
