package com.regnosys.rosetta.ast.expressions.constructors;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.expressions.supporting.RKeyValuePair;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Constructor expression node, corresponding to the
 * {@code ConstructorExpr} grammar alternative.
 *
 * <p>Represents a type construction expression, e.g.,
 * {@code Party { name: "ACME", role: PartyRoleEnum -> Client }}.
 * The {@code spread} flag indicates whether the {@code ...} spread
 * operator was used.
 */
public class RConstructorExpr extends RExpression {

    private RTypeCall typeCall;
    private final List<RKeyValuePair> pairs = new ArrayList<>();
    private boolean spread;

    // -- typeCall --------------------------------------------------------------

    public RTypeCall typeCall() {
        return typeCall;
    }

    public void setTypeCall(RTypeCall typeCall) {
        checkMutable();
        this.typeCall = typeCall;
    }

    // -- pairs ----------------------------------------------------------------

    public List<RKeyValuePair> pairs() {
        return pairs;
    }

    // -- spread ---------------------------------------------------------------

    public boolean isSpread() {
        return spread;
    }

    public void setSpread(boolean spread) {
        checkMutable();
        this.spread = spread;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        if (typeCall != null) {
            result.add(typeCall);
        }
        result.addAll(pairs);
        return List.copyOf(result);
    }

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitConstructor(this, context);
    }
}
