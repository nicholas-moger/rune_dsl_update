package com.regnosys.rosetta.generator.java.expression.coercers;

import com.regnosys.rosetta.generator.java.expression.TypeCoercionService;
import com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaPrimitiveType;
import com.rosetta.util.types.JavaType;

import java.util.Set;

/**
 * Coerces a wrapper type into an unwrapped value using {@code get()},
 * {@code getOrDefault()}, etc.
 * Implements the wrapper-to-item quadrant of the dispatch matrix.
 *
 * <p>After unwrapping, if the item types differ, delegates to
 * {@link TypeCoercionService#coerceExpression} for item-level coercion.
 */
public class WrapperToItemCoercer implements TypeCoercer {

    @Override
    public JavaStatementBuilder coerce(
            JavaStatementBuilder expr, JavaType actualType,
            JavaType expectedType, boolean throwOnFail,
            JavaStatementScope scope, JavaTypeUtil typeUtil,
            TypeCoercionService service) {

        JavaType actualItemType = typeUtil.getItemType(actualType);

        // PR-A §9.1 C3a.4.o: all emissions carry structured refs (instance
        // method calls — no new library classes added, operand refs flow).

        // ComparisonResult → boolean (primitive)
        if (typeUtil.isComparisonResult(actualType)
                && JavaPrimitiveType.BOOLEAN.equals(expectedType)) {
            return expr.mapExpression(e ->
                    JavaExpression.from(
                            e.renderToString() + ".getOrDefault(false)",
                            expectedType,
                            e.getRefs(),
                            e.getStaticWildcardImports()));
        }

        // ComparisonResult → Boolean (boxed)
        if (typeUtil.isComparisonResult(actualType) && typeUtil.isBoolean(expectedType)) {
            return expr.mapExpression(e ->
                    JavaExpression.from(
                            e.renderToString() + ".get()",
                            expectedType,
                            e.getRefs(),
                            e.getStaticWildcardImports()));
        }

        // MapperS → primitive
        if (typeUtil.isMapperS(actualType) && expectedType instanceof JavaPrimitiveType) {
            JavaType expectedRef = ((JavaPrimitiveType) expectedType).toReferenceType();
            if (actualItemType != null && !actualItemType.equals(expectedRef)) {
                JavaStatementBuilder unwrapped = expr.mapExpression(e ->
                        JavaExpression.from(
                                e.renderToString() + ".get()",
                                actualItemType,
                                e.getRefs(),
                                e.getStaticWildcardImports()));
                return unwrapped.mapExpression(e ->
                        service.coerceExpression(e, actualItemType, expectedType, throwOnFail, scope));
            }
            String defaultValue = defaultValueFor(expectedType);
            return expr.mapExpression(e ->
                    JavaExpression.from(
                            e.renderToString() + ".getOrDefault(" + defaultValue + ")",
                            expectedType,
                            e.getRefs(),
                            e.getStaticWildcardImports()));
        }

        // MapperS/MapperC/Mapper → item: .get()
        if (typeUtil.isMapperS(actualType) || typeUtil.isMapperC(actualType) || typeUtil.isMapper(actualType)) {
            JavaStatementBuilder unwrapped = expr.mapExpression(e ->
                    JavaExpression.from(
                            e.renderToString() + ".get()",
                            actualItemType,
                            e.getRefs(),
                            e.getStaticWildcardImports()));
            if (!actualItemType.equals(expectedType)) {
                return unwrapped.mapExpression(e ->
                        service.coerceExpression(e, actualItemType, expectedType, throwOnFail, scope));
            }
            return unwrapped;
        }

        // List → item: MapperC.of(list).get()
        if (typeUtil.isList(actualType)) {
            JavaStatementBuilder unwrapped = expr.mapExpression(e ->
                    JavaExpression.from(
                            "MapperC.of(" + e.renderToString() + ").get()",
                            actualItemType,
                            HandlerHelper.union(e.getRefs(), Set.of(HandlerHelper.MAPPER_C)),
                            e.getStaticWildcardImports()));
            if (!actualItemType.equals(expectedType)) {
                return unwrapped.mapExpression(e ->
                        service.coerceExpression(e, actualItemType, expectedType, throwOnFail, scope));
            }
            return unwrapped;
        }

        // MapperListOfLists → item
        if (typeUtil.isMapperListOfLists(actualType)) {
            JavaStatementBuilder unwrapped = expr.mapExpression(e ->
                    JavaExpression.from(
                            "MapperC.of(" + e.renderToString() + ".flattenList()).get()",
                            actualItemType,
                            HandlerHelper.union(e.getRefs(), Set.of(HandlerHelper.MAPPER_C)),
                            e.getStaticWildcardImports()));
            if (!actualItemType.equals(expectedType)) {
                return unwrapped.mapExpression(e ->
                        service.coerceExpression(e, actualItemType, expectedType, throwOnFail, scope));
            }
            return unwrapped;
        }

        // Fallback: return unchanged
        return expr;
    }

    /**
     * Return the default value string for a primitive type.
     */
    private String defaultValueFor(JavaType primitiveType) {
        if (JavaPrimitiveType.BOOLEAN.equals(primitiveType)) {
            return "false";
        }
        if (JavaPrimitiveType.INT.equals(primitiveType)) {
            return "0";
        }
        if (JavaPrimitiveType.LONG.equals(primitiveType)) {
            return "0L";
        }
        if (JavaPrimitiveType.DOUBLE.equals(primitiveType)) {
            return "0.0";
        }
        if (JavaPrimitiveType.FLOAT.equals(primitiveType)) {
            return "0.0f";
        }
        return "0";
    }

}
