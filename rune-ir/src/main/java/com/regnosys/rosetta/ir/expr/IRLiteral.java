package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A scalar literal — integer, decimal, string or boolean (Wave-0 families 1–4).
 * Corresponds to the Rune AST nodes {@code RIntLiteral} / {@code RNumberLiteral} /
 * {@code RStringLiteral} / {@code RBooleanLiteral}.
 *
 * <p>The {@link #value()} is held as an {@code Object} whose concrete type is fixed by
 * {@link #literalKind()}:
 * <ul>
 *   <li>{@link LiteralKind#INT} → {@link java.math.BigInteger} (the grammar's
 *       {@code INT_LITERAL} is unbounded);</li>
 *   <li>{@link LiteralKind#NUMBER} → {@link java.math.BigDecimal};</li>
 *   <li>{@link LiteralKind#STRING} → {@link String} (the un-escaped Rune lexeme — Java
 *       string-escaping is an emitter concern);</li>
 *   <li>{@link LiteralKind#BOOLEAN} → {@link Boolean}.</li>
 * </ul>
 *
 * <p>Always {@code SINGLE} and {@code PRESENT}. Neutrality note: how a literal lowers
 * (e.g. {@code MapperS.of(...)}, {@code BigDecimal.valueOf} vs {@code new BigDecimal},
 * long-suffix choice) is entirely the emitter's decision, keyed off {@link #type()} —
 * none of it lives here.
 */
public record IRLiteral(
        LiteralKind literalKind,
        Object value,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    /** The four scalar literal flavours; fixes the runtime type of {@link #value()}. */
    public enum LiteralKind { INT, NUMBER, STRING, BOOLEAN }

    @Override
    public IRExprKind kind() {
        return IRExprKind.LITERAL;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
