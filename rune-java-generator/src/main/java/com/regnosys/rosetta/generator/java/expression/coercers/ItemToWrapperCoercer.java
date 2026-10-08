package com.regnosys.rosetta.generator.java.expression.coercers;

import com.regnosys.rosetta.generator.java.expression.TypeCoercionService;
import com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.rosetta.util.types.JavaType;

import java.util.Set;

/**
 * Coerces an unwrapped value into a wrapper type:
 * MapperS, MapperC, Mapper, List, ComparisonResult.
 * Implements the item-to-wrapper quadrant of the dispatch matrix.
 *
 * <p>When the item type of the wrapper differs from the actual type, this coercer
 * first coerces the item type (via {@link TypeCoercionService#coerceExpression}),
 * then wraps.
 */
public class ItemToWrapperCoercer implements TypeCoercer {

    @Override
    public JavaStatementBuilder coerce(
            JavaStatementBuilder expr, JavaType actualType,
            JavaType expectedType, boolean throwOnFail,
            JavaStatementScope scope, JavaTypeUtil typeUtil,
            TypeCoercionService service) {

        // Determine the expected wrapper's item type
        JavaType expectedItemType = typeUtil.getItemType(expectedType);

        // If item types differ, coerce item type first
        if (!actualType.equals(expectedItemType)) {
            expr = expr.mapExpression(e -> {
                JavaExpression coerced = service.coerceExpression(
                        e, actualType, expectedItemType, throwOnFail, scope);
                return coerced;
            });
            // After item coercion, the actual type is now the expected item type
        }

        // MapperListOfLists — cannot wrap a single item
        if (typeUtil.isMapperListOfLists(expectedType)) {
            throw new IllegalArgumentException(
                    "Cannot coerce a single item to MapperListOfLists: " + actualType + " -> " + expectedType);
        }

        // PR-A §9.1 C3a.4.m: all 5 emissions carry structured refs so
        // FunctionGenerator substring ladder L822-871 can delete atomically
        // at C3c.2 without regressing coercer-driven imports.

        // MapperS — use wrappedInMapperSOf factory so unwrapToBuilder is set
        // (enables the unwrap path to strip MapperS.of cleanly). Pass the
        // expected MapperS<T> wrapper JavaType so the returned expression
        // reports the correct wrapper type for downstream type-driven
        // coercion (Copilot round-13).
        if (typeUtil.isMapperS(expectedType)) {
            return expr.mapExpression(e -> JavaExpression.wrappedInMapperSOf(e, expectedType));
        }

        // MapperC
        if (typeUtil.isMapperC(expectedType)) {
            return expr.mapExpression(e ->
                    JavaExpression.from(
                            "MapperC.of(Collections.singletonList(" + e.renderToString() + "))",
                            expectedType,
                            HandlerHelper.union(e.getRefs(), Set.of(HandlerHelper.MAPPER_C, HandlerHelper.COLLECTIONS)),
                            e.getStaticWildcardImports()));
        }

        // Mapper (base interface) — treat as MapperS with wrapper type
        if (typeUtil.isMapper(expectedType)) {
            return expr.mapExpression(e -> JavaExpression.wrappedInMapperSOf(e, expectedType));
        }

        // List
        if (typeUtil.isList(expectedType)) {
            return expr.mapExpression(e ->
                    JavaExpression.from(
                            "Collections.singletonList(" + e.renderToString() + ")",
                            expectedType,
                            HandlerHelper.union(e.getRefs(), Set.of(HandlerHelper.COLLECTIONS)),
                            e.getStaticWildcardImports()));
        }

        // ComparisonResult — MapperS.of inner, ComparisonResult.ofNullSafe outer.
        if (typeUtil.isComparisonResult(expectedType)) {
            return expr.mapExpression(e -> {
                JavaExpression wrappedMapperS = JavaExpression.wrappedInMapperSOf(e);
                return JavaExpression.from(
                        "ComparisonResult.ofNullSafe(" + wrappedMapperS.renderToString() + ")",
                        expectedType,
                        HandlerHelper.union(wrappedMapperS.getRefs(), Set.of(HandlerHelper.COMPARISON_RESULT)),
                        wrappedMapperS.getStaticWildcardImports());
            });
        }

        // No wrapper recognized — return unchanged
        return expr;
    }
}
