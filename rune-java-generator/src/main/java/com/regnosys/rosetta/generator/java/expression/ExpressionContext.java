package com.regnosys.rosetta.generator.java.expression;

import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.rosetta.util.types.JavaType;

/**
 * Immutable context for expression compilation.
 * Carries the expected output type, current variable scope, and cardinality.
 */
public record ExpressionContext(JavaType expectedType, JavaStatementScope scope, boolean isMultiValued) {

    /** Backward-compatible factory for M7b-1 code. Defaults isMultiValued to false. */
    public static ExpressionContext of(JavaType expectedType, JavaStatementScope scope) {
        return new ExpressionContext(expectedType, scope, false);
    }

    public ExpressionContext withExpected(JavaType type) {
        return new ExpressionContext(type, scope, isMultiValued);
    }

    public ExpressionContext withScope(JavaStatementScope scope) {
        return new ExpressionContext(expectedType, scope, isMultiValued);
    }

    public ExpressionContext withMultiValued(boolean multi) {
        return new ExpressionContext(expectedType, scope, multi);
    }
}
