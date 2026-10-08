package com.regnosys.rosetta.ast.expressions.binary;

import com.regnosys.rosetta.ast.RBinaryExpression;
import com.regnosys.rosetta.ast.enums.CardMod;
import com.regnosys.rosetta.ast.enums.CompOp;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

import java.util.Optional;

/**
 * Comparison binary expression node, corresponding to the
 * {@code ComparisonExpr} grammar alternative.
 *
 * <p>Represents comparison operations: less than (&lt;), greater than (&gt;),
 * less than or equal (&lt;=), and greater than or equal (&gt;=).
 * Optionally includes a cardinality modifier (ANY, ALL).
 *
 * <p>EMF equivalent: {@code ComparisonOperation}.
 */
public class RComparisonExpr extends RBinaryExpression {

    private CompOp op;
    private CardMod mod;

    // -- op -------------------------------------------------------------------

    public CompOp op() {
        return op;
    }

    public void setOp(CompOp op) {
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
        return visitor.visitComparison(this, context);
    }
}
