package com.regnosys.rosetta.generator.java.optimised;

import com.regnosys.rosetta.ast.functions.ROperation;
import com.regnosys.rosetta.generator.java.function.FunctionExpressionRenderer;
import com.regnosys.rosetta.generator.java.function.RenderedStatement;

/**
 * The v3 optimised renderer — behaviour-transparent except for ONE thing: it brackets
 * the canonical {@code renderOperation} entry (the 7-arg overload every shallower
 * overload funnels into) with {@link OptimisedExpressionCompiler#enterOperationBody()} /
 * {@code exitOperationBody()}, marking the OPERATION-body context the tranche-1
 * eligibility cut requires (the census § 4 text-safety argument: operation bodies
 * discard ComparisonResult text; conditions, postConditions, aliases and datarule
 * renders never toggle the flag and stay reference-emitted). Depth-counted, so nested
 * render entries stay correct.
 */
public class OptimisedFunctionExpressionRenderer extends FunctionExpressionRenderer {

    private final OptimisedExpressionCompiler optimisedCompiler;

    public OptimisedFunctionExpressionRenderer(OptimisedExpressionCompiler compiler) {
        super(compiler);
        this.optimisedCompiler = compiler;
    }

    @Override
    public RenderedStatement renderOperation(ROperation operation, int indentLevel,
            boolean outputNeedsBuilder, boolean functionHasDeepOperations, String outputTypeName,
            String rawOutputName, String escapedOutputName) {
        optimisedCompiler.enterOperationBody();
        try {
            return super.renderOperation(operation, indentLevel, outputNeedsBuilder,
                    functionHasDeepOperations, outputTypeName, rawOutputName, escapedOutputName);
        } finally {
            optimisedCompiler.exitOperationBody();
        }
    }
}
