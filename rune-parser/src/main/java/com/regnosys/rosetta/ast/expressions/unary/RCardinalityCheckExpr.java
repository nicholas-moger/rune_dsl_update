package com.regnosys.rosetta.ast.expressions.unary;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.enums.CardCheckOp;
import com.regnosys.rosetta.ast.enums.Necessity;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cardinality check expression node, consolidating the {@code OneOfExpr}
 * and {@code ChoiceExpr} grammar alternatives.
 *
 * <p>Represents cardinality constraints: {@code expression one-of} and
 * {@code expression necessity choice attr1, attr2, ...}.
 *
 * <p>2-to-1 consolidation of EMF's separate one-of and choice expression types.
 */
public class RCardinalityCheckExpr extends RExpression {

    private RExpression argument;
    private CardCheckOp op;
    private Necessity necessity;
    private final List<String> attributes = new ArrayList<>();

    // -- argument (the expression being checked) ------------------------------

    public RExpression argument() {
        return argument;
    }

    public void setArgument(RExpression argument) {
        checkMutable();
        this.argument = argument;
    }

    @Override
    public Optional<RExpression> left() {
        return Optional.ofNullable(argument);
    }

    // -- op -------------------------------------------------------------------

    public CardCheckOp op() {
        return op;
    }

    public void setOp(CardCheckOp op) {
        checkMutable();
        this.op = op;
    }

    // -- necessity (optional, only for CHOICE) --------------------------------

    public Optional<Necessity> necessity() {
        return Optional.ofNullable(necessity);
    }

    public void setNecessity(Necessity necessity) {
        checkMutable();
        this.necessity = necessity;
    }

    // -- attributes (for CHOICE: the attribute names) -------------------------

    public List<String> attributes() {
        return attributes;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        if (argument != null) {
            return List.of(argument);
        }
        return List.of();
    }

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitCardinalityCheck(this, context);
    }
}
