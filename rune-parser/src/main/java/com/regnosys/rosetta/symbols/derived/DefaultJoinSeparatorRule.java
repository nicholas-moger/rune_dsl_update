package com.regnosys.rosetta.symbols.derived;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ast.expressions.binary.RJoinExpr;
import com.regnosys.rosetta.ast.expressions.literals.RStringLiteral;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.symbols.diagnostics.Diagnostics;

/**
 * Default join separator rule — adds an empty string literal as the
 * separator of any {@code join} expression that has no explicit separator.
 */
public final class DefaultJoinSeparatorRule implements DerivedStateRule {

    @Override
    public void apply(RModel model, Diagnostics collector) {
        for (RJoinExpr join : AstWalker.findAll(model, RJoinExpr.class)) {
            if (join.separator().isEmpty()) {
                RStringLiteral empty = new RStringLiteral();
                empty.setValue("");
                empty.setSourceRange(SourceRange.NONE);
                join.setSeparator(empty);
            }
        }
    }
}
