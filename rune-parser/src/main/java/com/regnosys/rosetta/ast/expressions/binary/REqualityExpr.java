package com.regnosys.rosetta.ast.expressions.binary;

import com.regnosys.rosetta.ast.RBinaryExpression;
import com.regnosys.rosetta.ast.enums.CardMod;
import com.regnosys.rosetta.ast.enums.EqOp;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

import java.util.Optional;

/**
 * Equality binary expression node, corresponding to the
 * {@code EqualityExpr} grammar alternative.
 *
 * <p>Represents equality operations: equal (=) and not-equal (&lt;&gt;).
 * Optionally includes a cardinality modifier (ANY, ALL).
 *
 * <p>EMF equivalent: {@code EqualityOperation}.
 */
public class REqualityExpr extends RBinaryExpression {

    private EqOp op;
    private CardMod mod;

    // -- op -------------------------------------------------------------------

    public EqOp op() {
        return op;
    }

    public void setOp(EqOp op) {
        checkMutable();
        this.op = op;
    }

    // -- mod (optional) -------------------------------------------------------

    public Optional<CardMod> mod() {
        return Optional.ofNullable(mod);
    }

    public void setMod(CardMod mod) {
        checkMutable();
        this.mod = mod;
    }

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitEquality(this, context);
    }
}
