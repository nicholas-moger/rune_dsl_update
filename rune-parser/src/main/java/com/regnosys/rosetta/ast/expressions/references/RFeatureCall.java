package com.regnosys.rosetta.ast.expressions.references;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

import java.util.List;
import java.util.Optional;

/**
 * Feature call expression node, corresponding to the
 * {@code FeatureCallExpr} grammar alternative.
 *
 * <p>Represents accessing a feature (attribute, field) on an expression,
 * e.g., {@code trade -> price}. The receiver is the left-hand expression
 * and the featureName is the accessed member.
 *
 * <p>EMF equivalent: {@code RosettaFeatureCall}.
 */
public class RFeatureCall extends RExpression {

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
        category = com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory.FEATURE_NOT_FOUND,
        tokenRangeKey = "feature")
    private com.regnosys.rosetta.ast.supporting.RAttribute resolvedFeature;

    public java.util.Optional<com.regnosys.rosetta.ast.supporting.RAttribute> resolvedFeature() {
        return java.util.Optional.ofNullable(resolvedFeature);
    }
    // === v3.1 C1 — THE AUTHORITATIVE FEATURE BINDING ========================
    //
    // {@link #resolvedFeature} is typed to RAttribute, so it structurally cannot
    // hold a CHOICE OPTION — which is why `Asset -> Instrument -> Security` left
    // every hop after the first unresolved. Upstream has no such limit: it models
    // a choice as a data type whose options ARE its attributes, so one slot holds
    // either. This field is that slot, filled by TypeDirectedResolver.lookupFeature
    // (spec R9) with whatever the feature namespace legitimately yields — an
    // RAttribute or an RChoiceOption.
    //
    // ADDITIVE: resolvedFeature keeps being set whenever the answer IS an
    // attribute, so all 153 of its call sites are untouched. Migrating them here
    // is a recorded follow-on.
    private com.regnosys.rosetta.ast.RNode resolvedFeatureNode;

    public java.util.Optional<com.regnosys.rosetta.ast.RNode> resolvedFeatureNode() {
        return java.util.Optional.ofNullable(resolvedFeatureNode);
    }

    public void setResolvedFeatureNode(com.regnosys.rosetta.ast.RNode resolved) {
        checkMutable();
        this.resolvedFeatureNode = resolved;
    }

    public void setResolvedFeature(com.regnosys.rosetta.ast.supporting.RAttribute resolved) {
        checkMutable();
        this.resolvedFeature = resolved;
    }

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitFeatureCall(this, context);
    }
}
