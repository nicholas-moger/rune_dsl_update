package com.regnosys.rosetta.symbols.derived;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.symbols.diagnostics.Diagnostics;

/**
 * Implicit variable rule — inline functions without explicit closure
 * parameters gain a synthetic closure parameter named {@code item}.
 */
public final class ImplicitVariableRule implements DerivedStateRule {

    @Override
    public void apply(RModel model, Diagnostics collector) {
        for (RInlineFunction inline : AstWalker.findAll(model, RInlineFunction.class)) {
            if (inline.isImplicit() && inline.paramNames().isEmpty()) {
                // Only inject "item" into implicit inline functions (no explicit
                // bracket syntax). Explicit empty-bracket forms like `extract []`
                // deliberately declare an empty parameter list and should NOT
                // receive a synthetic binding. Matches Xtext behaviour — D11.
                inline.paramNames().add("item");
            }
        }
    }
}
