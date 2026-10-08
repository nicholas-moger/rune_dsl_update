package com.regnosys.rosetta.ast.expressions.references;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.linker.CrossRefField;

import java.util.ArrayList;
import java.util.List;

/**
 * Symbol reference expression node, corresponding to the
 * {@code SymbolReferenceExpr} grammar alternative.
 *
 * <p>Represents a reference to a named symbol (function, parameter, alias, etc.)
 * with optional arguments. For example, {@code MyFunction(arg1, arg2)} or simply
 * {@code myParam}.
 *
 * <p>EMF equivalent: {@code RosettaSymbolReference}.
 */
public class RSymbolReference extends RExpression {

    private String name;
    private final List<RExpression> args = new ArrayList<>();

    // -- name -----------------------------------------------------------------

    public String name() {
        return name;
    }

    public void setName(String name) {
        checkMutable();
        this.name = name;
    }

    // -- args -----------------------------------------------------------------

    public List<RExpression> args() {
        return args;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        return List.copyOf(args);
    }

    // === M3 resolved fields (D2) =============================================

    @CrossRefField(category = DiagnosticCategory.SYMBOL_NOT_FOUND)
    private com.regnosys.rosetta.ast.RNode resolvedSymbol;

    // === v3.1 C1 — THE AUTHORITATIVE BINDING FOR A NAME THAT NAMES A FEATURE ==
    //
    // A bare name inside an extract/filter/then body may name a feature of the
    // implicit item (spec R7.1). Where that feature is an ATTRIBUTE the engine
    // binds it into {@link #symbol()} — but where it is a CHOICE OPTION or a
    // META feature there was nothing to bind: the arms cleared the stale
    // diagnostic and recorded no binding, exactly as their comments say. That
    // left the name resolved in upstream and unresolved here.
    //
    // This slot is that binding. It is ADDITIVE — symbol() is untouched, so the
    // 62 call sites reading it and the render arms keyed on its bind state are
    // unaffected — and it is the AUTHORITY for what such a name means.
    private com.regnosys.rosetta.ast.RNode resolvedFeatureNode;

    public java.util.Optional<com.regnosys.rosetta.ast.RNode> resolvedFeatureNode() {
        return java.util.Optional.ofNullable(resolvedFeatureNode);
    }

    public void setResolvedFeatureNode(com.regnosys.rosetta.ast.RNode resolved) {
        checkMutable();
        this.resolvedFeatureNode = resolved;
    }

    public java.util.Optional<com.regnosys.rosetta.ast.RNode> symbol() { return java.util.Optional.ofNullable(resolvedSymbol); }
    public void setResolvedSymbol(com.regnosys.rosetta.ast.RNode resolved) { checkMutable(); this.resolvedSymbol = resolved; }

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitSymbolReference(this, context);
    }
}
