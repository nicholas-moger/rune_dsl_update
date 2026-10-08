package com.regnosys.rosetta.ast.expressions.references;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

import java.util.List;
import java.util.Optional;

/**
 * Deep feature call expression node, corresponding to the
 * {@code DeepFeatureCallExpr} grammar alternative.
 *
 * <p>Represents a deep path access on an expression, e.g.,
 * {@code trade ->> price}. The receiver is the left-hand expression
 * and the featureName is the deeply accessed member.
 *
 * <p>EMF equivalent: {@code RosettaDeepFeatureCall}.
 */
public class RDeepFeatureCall extends RExpression {

    private RExpression receiver;
    private String featureName;

    // -- receiver -------------------------------------------------------------

    public RExpression receiver() {
        return receiver;
    }

    public void setReceiver(RExpression receiver) {
        checkMutable();
        this.receiver = receiver;
    }

    // -- featureName ----------------------------------------------------------

    public String featureName() {
        return featureName;
    }

    public void setFeatureName(String featureName) {
        checkMutable();
        this.featureName = featureName;
    }

    // -- left (the receiver serves as the left operand) -----------------------

    @Override
    public Optional<RExpression> left() {
        return Optional.ofNullable(receiver);
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        if (receiver != null) {
            return List.of(receiver);
        }
        return List.of();
    }

    // === M4 resolved fields (D9) =============================================

    @com.regnosys.rosetta.symbols.linker.CrossRefField(
        category = com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory.DEEP_FEATURE_NOT_FOUND,
        tokenRangeKey = "feature")
    private com.regnosys.rosetta.ast.supporting.RAttribute resolvedFeature;

    public java.util.Optional<com.regnosys.rosetta.ast.supporting.RAttribute> resolvedFeature() {
        return java.util.Optional.ofNullable(resolvedFeature);
    }
    public void setResolvedFeature(com.regnosys.rosetta.ast.supporting.RAttribute resolved) {
        checkMutable();
        this.resolvedFeature = resolved;
    }

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitDeepFeatureCall(this, context);
    }
}
