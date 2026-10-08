package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A bracketed list literal {@code [a, b, ...]} (Wave-0 family 5), corresponding to the
 * Rune AST {@code RListLiteral}. The {@link #elements()} are the operand expressions in
 * source order, which is preserved exactly (the emitter's element sequence depends on
 * it) and is the basis of the child {@link NodeId}s.
 *
 * <p>Strictly a leaf-composition: in Wave 0 a list literal is IR-expressible only when
 * every element is itself Wave-0-expressible (the adapter enforces this). The element
 * witness type and the Java collection form ({@code MapperC.<T>of(...)}) are emitter
 * concerns derived from {@link #type()} — not modelled here.
 */
public record IRListConstruct(
        List<IRExpr> elements,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    /** Defensively copies the element list so the node is immutable. */
    public IRListConstruct {
        elements = List.copyOf(elements);
    }

    @Override
    public IRExprKind kind() {
        return IRExprKind.LIST_CONSTRUCT;
    }

    @Override
    public List<? extends IRExpr> children() {
        return elements;
    }
}
