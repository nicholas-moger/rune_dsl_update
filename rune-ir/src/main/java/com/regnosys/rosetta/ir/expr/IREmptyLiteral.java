package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * The {@code empty} absent-value leaf (Wave-0 family 6), corresponding to the Rune AST
 * {@code REmptyLiteral}.
 *
 * <p><strong>Carries no monad identity.</strong> This node states only "an absent value
 * of {@link #type()} at {@link #cardinality()}". Whether that lowers to
 * {@code MapperS.ofNull()}, an empty list, {@code null}, a {@code Maybe.Nothing} or a
 * Rust {@code None} is decided later — in optionality-lowering and the emitter — never
 * here (design §3 Group A). Its {@link #optionality()} is therefore {@code OPTIONAL}.
 *
 * <p>{@link #source()} preserves provenance because the reference generator may emit a
 * user-written {@code empty} differently from a compiler-injected default-else empty;
 * Wave-0 only ever constructs {@link EmptySource#USER_EMPTY}, but the discriminator
 * exists so later waves (and the default-else rule) need not reshape the node.
 */
public record IREmptyLiteral(
        EmptySource source,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    /** Where an {@code empty} came from — user-written vs compiler-synthesized. */
    public enum EmptySource {
        /** The user-written {@code empty} keyword. */
        USER_EMPTY,
        /** A default-else-rule-injected empty (introduced in a later wave). */
        SYNTH_DEFAULT_ELSE
    }

    @Override
    public IRExprKind kind() {
        return IRExprKind.EMPTY_LITERAL;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
