package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A POINT-FREE function application — a bare no-argument reference to a function used as a value,
 * whose IMPLICIT argument the render derives from the position's enclosing context: a call argument
 * ({@code Callee(BareF)}), a navigation head ({@code BareF -> field}), a filter/extract body
 * ({@code partyRoles filter Foo}), an operand. The {@link #callee()} simple name is the neutral
 * fact: "apply this function to the context's implicit input".
 *
 * <p>Deliberately NOT an {@link IRApply} (the #492 resolution of the L-109 deferral): lowering
 * point-free to an empty-args {@code IRApply} would pollute every {@code IRApply}-keyed admission
 * gate at once ({@code isSimpleCallArg} / {@code isScalarOperand} / {@code isExistenceOperand} /
 * {@code isNavigableReceiver}), admitting positions whose render composition is unproven. As its
 * own kind, each position admits EXPLICITLY (#492 admits the call-argument and nav-receiver
 * positions — both byte-identical by construction through the oracle below; the operand positions
 * stay declined pending their own wrap-lever decode).
 *
 * <p>At the Java target the implicit-argument derivation (the enclosing rule input / lambda item /
 * condition instance walks), the {@code MapperS.of} nav-receiver wrap and the zero-real-input
 * {@code .evaluate()} form are <strong>Java-emission decisions</strong> kept off this neutral node
 * (the L-029 split): the emitter delegates to the compiler's range-correlated
 * {@code PointFreeRenderer}, which reuses legacy
 * {@code ReferenceHandler.renderImplicitFunctionInvocation} verbatim on the source-range-correlated
 * raw node — the SAME public oracle (the D43 seam) the top-level point-free claim already reuses,
 * so the render is byte-identical to the decline path by the strongest argument. A future
 * Python/Rust emitter renders from {@link #callee()} + its own context convention directly.
 *
 * <p>The {@link #type()}/{@link #cardinality()} carry the callee OUTPUT's engine channels (the
 * adapter's cache-boundary fallback — the bare reference's own node type reads MISSING for the
 * L-109 shapes), so the consuming gates and the nav lambda-var naming read honest facts.
 *
 * <p>Fork-authored (PR #492); not present upstream.
 */
public record IRPointFreeApply(
        String callee,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.POINT_FREE_APPLY;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
