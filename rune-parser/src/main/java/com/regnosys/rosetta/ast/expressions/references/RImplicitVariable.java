package com.regnosys.rosetta.ast.expressions.references;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

/**
 * Implicit variable expression node, corresponding to the
 * {@code ImplicitVariableExpr} grammar alternative.
 *
 * <p>Represents the implicit {@code item} variable available in
 * inline functions and certain expression contexts.
 *
 * <p>Two construction paths produce this node type:
 * <ul>
 *   <li>The user-written literal {@code item} keyword (via
 *       {@code AstBuilder.visitImplicitVarExpr}). {@link #isSynthetic()}
 *       returns {@code false}.</li>
 *   <li>The synthetic elided-operand materialisation for without-left
 *       list/extract/filter/conversion/toString forms (via
 *       {@code AstBuilder.syntheticImplicitInput} — engine PR #1).
 *       {@link #isSynthetic()} returns {@code true}.</li>
 * </ul>
 *
 * <p>Downstream consumers ({@code TypeInferenceEngine} Cat 8 elided-branch,
 * {@code ReferenceHandler.handle}) discriminate via {@link #isSynthetic()}
 * so the literal {@code item} keyword used as an explicit receiver (e.g.
 * {@code item only-element} inside a lambda body) is not misclassified as
 * elided — its parent op's {@code argument} slot identity is the same shape
 * either way, so structural detection alone is not sufficient.
 *
 * <p>EMF equivalent: {@code RosettaImplicitVariable}.
 */
public class RImplicitVariable extends RExpression {

    private boolean synthetic;

    public boolean isSynthetic() {
        return synthetic;
    }

    public void setSynthetic(boolean synthetic) {
        checkMutable();
        this.synthetic = synthetic;
    }

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitImplicitVariable(this, context);
    }
}
