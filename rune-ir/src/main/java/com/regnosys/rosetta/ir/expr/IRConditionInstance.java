package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * The data-rule CONDITION'S implicit instance ({@code IRExprKind#CONDITION_INSTANCE}, v3.3 seat 4 / PR #640) - the
 * reference legacy synthesizes to the condition method's own parameter ({@code adjustableDates} inside
 * {@code type AdjustableDates}) while it renders a condition body, and re-enters the compiler with. Deliberately SHALLOW
 * (the {@link IRImplicitAttrNav} pattern): only the owning type's NAME is carried, typed as that data type; the parameter
 * naming and every navigation off it are legacy's own. An oracle leaf: every root containing it renders WHOLE through the
 * legacy handler by identity (the compiler's {@code containsOracleLeaf} / {@code isOracleRootShape}); no leaf-emitter arm.
 *
 * @param typeName the owning data type's name (the condition's declaring type)
 */
public record IRConditionInstance(
        String typeName,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.CONDITION_INSTANCE;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
