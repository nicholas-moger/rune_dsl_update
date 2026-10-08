package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A bound-variable reference (Wave-0 families 7–8, the variable-binding cases),
 * corresponding to {@code RImplicitVariable} and the bare-variable case of
 * {@code RSymbolReference} (a parameter or alias reference with no argument list).
 *
 * <p>The {@link #variableKind()} records <em>which</em> binder the name resolves to.
 * The distinction matters to lowering (an implicit {@code item} is a lambda binder; a
 * parameter is a method argument; an alias is a shortcut binding), but those are all
 * emitter decisions — this node only states the name and binder flavour.
 */
public record IRVariable(
        String name,
        VariableKind variableKind,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    /**
     * The binder a variable resolves to. {@code USER_ITEM} is the user-written
     * {@code item} keyword; {@code SYNTHETIC_ITEM} is the compiler-elided operand
     * (without-left forms); {@code PARAM} a function parameter; {@code ALIAS} a
     * shortcut/alias binder. Lambda/let binders ({@code CLOSURE_PARAM}, {@code LET_BINDER})
     * arrive with the waves that introduce lambdas and {@code then}.
     */
    public enum VariableKind { USER_ITEM, SYNTHETIC_ITEM, PARAM, ALIAS, CLOSURE_PARAM, LET_BINDER }

    @Override
    public IRExprKind kind() {
        return IRExprKind.VARIABLE;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
