package com.regnosys.rosetta.generator.java.expression.coercers;

import com.regnosys.rosetta.generator.java.expression.TypeCoercionService;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.rosetta.util.types.JavaType;

/**
 * Strategy interface for type coercion. Each coercer handles one quadrant
 * of the (is-actual-wrapper × is-expected-wrapper) dispatch matrix.
 *
 * <p>Extension point for future SDK/maintenance layer.
 */
public interface TypeCoercer {

    /**
     * Apply the coercion.
     *
     * @param expr         the expression to coerce
     * @param actualType   the current type of the expression
     * @param expectedType the target type
     * @param throwOnFail  true = throw on narrowing overflow; false = return null
     * @param scope        current variable scope for intermediate declarations
     * @param typeUtil     type query utilities
     * @param service      the parent service for composed coercions
     */
    JavaStatementBuilder coerce(
            JavaStatementBuilder expr, JavaType actualType,
            JavaType expectedType, boolean throwOnFail,
            JavaStatementScope scope, JavaTypeUtil typeUtil,
            TypeCoercionService service);
}
