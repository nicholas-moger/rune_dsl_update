package com.regnosys.rosetta.symbols.derived;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.ast.expressions.literals.RListLiteral;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.symbols.diagnostics.Diagnostics;

/**
 * Default else rule — adds an empty list literal as the else branch of any
 * {@code if X then Y} expression that has no explicit else clause.
 * Matches Xtext's RosettaDerivedStateComputer behaviour for D11.
 */
public final class DefaultElseRule implements DerivedStateRule {

    @Override
    public void apply(RModel model, Diagnostics collector) {
        for (RConditionalExpr cond : AstWalker.findAll(model, RConditionalExpr.class)) {
            if (cond.elseBranch().isEmpty()) {
                RListLiteral empty = new RListLiteral();
                empty.setSourceRange(SourceRange.NONE);
                cond.setElseBranch(empty);
            }
        }
    }
}
