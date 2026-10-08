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
 * Coerces between different wrapper types: MapperS ↔ MapperC, MapperS ↔ List,
 * ComparisonResult ↔ MapperS, etc. Internally composes with {@link WrappedItemCoercer}
 * when item types also differ.
 * Implements the wrapper-to-wrapper quadrant of the dispatch matrix.
 *
 * <h3>Composed coercions</h3>
 * <p>When both wrapper kind AND item type differ, this coercer first delegates to
 * {@link WrappedItemCoercer} to change the item type within the current wrapper,
 * then changes the wrapper kind.</p>
 */
public class WrapperToWrapperCoercer implements TypeCoercer {

    private final WrappedItemCoercer wrappedItem;

    public WrapperToWrapperCoercer(WrappedItemCoercer wrappedItem) {
        this.wrappedItem = wrappedItem;
    }

    @Override
    public JavaStatementBuilder coerce(
            JavaStatementBuilder expr, JavaType actualType,
            JavaType expectedType, boolean throwOnFail,
            JavaStatementScope scope, JavaTypeUtil typeUtil,
            TypeCoercionService service) {

        JavaType actualItemType = typeUtil.getItemType(actualType);
        JavaType expectedItemType = typeUtil.getItemType(expectedType);

        // Wildcard to concrete: strip the wildcard bound
        if (typeUtil.hasWildcardArgument(actualType) && !typeUtil.hasWildcardArgument(expectedType)) {
            // Same wrapper kind, just fixing wildcard
            if (sameWrapperKind(actualType, expectedType, typeUtil)) {
                if (typeUtil.isList(actualType)) {
                    // List<? extends T> → List<T>: copy to a mutable ArrayList.
                    // Copilot round 15 bug-class sweep: ArrayList needs a
                    // structured ref since the needsArrayList template-flag
                    // only fires on outputIsMulti, not on intermediate alias
                    // coercions inside a single-valued function.
                    return expr.mapExpression(e ->
                            JavaExpression.from(
                                    "new ArrayList<>(" + e.renderToString() + ")",
                                    expectedType,
                                    HandlerHelper.union(e.getRefs(), Set.of(HandlerHelper.ARRAY_LIST)),
                                    e.getStaticWildcardImports()));
                }
                // MapperS/MapperC/Mapper<? extends T> → concrete: use .map() to reify type.
                // Function.identity needs java.util.function.Function import
                // — no template-flag path for it; structured ref is sole feeder.
                return expr.mapExpression(e ->
                        JavaExpression.from(
                                e.renderToString() + ".map(\"Make mutable\", Function.identity())",
                                expectedType,
                                HandlerHelper.union(e.getRefs(), Set.of(HandlerHelper.FUNCTION)),
                                e.getStaticWildcardImports()));
            }
        }

        // If item types differ and we're not dealing with ComparisonResult, coerce item first
        boolean itemTypesDiffer = !actualItemType.equals(expectedItemType);
        if (itemTypesDiffer
                && !typeUtil.isComparisonResult(actualType)
                && !typeUtil.isComparisonResult(expectedType)) {
            // Build an intermediate type with the actual wrapper + expected item type
            JavaType intermediateType = typeUtil.changeItemType(actualType, expectedItemType);
            expr = wrappedItem.coerce(expr, actualType, intermediateType, throwOnFail,
                    scope, typeUtil, service);
            actualType = intermediateType;
        }

        // Now wrapper kinds differ but item types match (or are handled specially)

        // PR-A §9.1 C3a.4.n: all emissions carry structured refs.
        // ComparisonResult → MapperS<Boolean>  — no library refs (.asMapper instance)
        if (typeUtil.isComparisonResult(actualType) && typeUtil.isMapperS(expectedType)) {
            return expr.mapExpression(e ->
                    JavaExpression.from(
                            e.renderToString() + ".asMapper()",
                            expectedType,
                            e.getRefs(),
                            e.getStaticWildcardImports()));
        }

        // MapperS<Boolean> → ComparisonResult
        if (typeUtil.isMapperS(actualType) && typeUtil.isComparisonResult(expectedType)) {
            return expr.mapExpression(e ->
                    JavaExpression.from(
                            "ComparisonResult.ofNullSafe(" + e.renderToString() + ")",
                            expectedType,
                            HandlerHelper.union(e.getRefs(), Set.of(HandlerHelper.COMPARISON_RESULT)),
                            e.getStaticWildcardImports()));
        }

        // MapperS → MapperC
        if (typeUtil.isMapperS(actualType) && typeUtil.isMapperC(expectedType)) {
            return expr.mapExpression(e ->
                    JavaExpression.from(
                            "MapperC.of(" + e.renderToString() + ")",
                            expectedType,
                            HandlerHelper.union(e.getRefs(), Set.of(HandlerHelper.MAPPER_C)),
                            e.getStaticWildcardImports()));
        }

        // MapperC → MapperS
        if (typeUtil.isMapperC(actualType) && typeUtil.isMapperS(expectedType)) {
            return expr.mapExpression(e ->
                    JavaExpression.from(
                            "MapperS.of(" + e.renderToString() + ".get())",
                            expectedType,
                            HandlerHelper.union(e.getRefs(), Set.of(HandlerHelper.MAPPER_S)),
                            e.getStaticWildcardImports()));
        }

        // Mapper → MapperS
        if (typeUtil.isMapper(actualType) && typeUtil.isMapperS(expectedType)) {
            return expr.mapExpression(e ->
                    JavaExpression.from(
                            "MapperS.of(" + e.renderToString() + ".get())",
                            expectedType,
                            HandlerHelper.union(e.getRefs(), Set.of(HandlerHelper.MAPPER_S)),
                            e.getStaticWildcardImports()));
        }

        // Mapper → MapperC
        if (typeUtil.isMapper(actualType) && typeUtil.isMapperC(expectedType)) {
            return expr.mapExpression(e ->
                    JavaExpression.from(
                            "MapperC.of(" + e.renderToString() + ")",
                            expectedType,
                            HandlerHelper.union(e.getRefs(), Set.of(HandlerHelper.MAPPER_C)),
                            e.getStaticWildcardImports()));
        }

        // MapperS → Mapper (identity render, type change only)
        if (typeUtil.isMapperS(actualType) && typeUtil.isMapper(expectedType)) {
            return expr.mapExpression(e ->
                    JavaExpression.from(
                            e.renderToString(),
                            expectedType,
                            e.getRefs(),
                            e.getStaticWildcardImports()));
        }

        // MapperC → Mapper (identity render)
        if (typeUtil.isMapperC(actualType) && typeUtil.isMapper(expectedType)) {
            return expr.mapExpression(e ->
                    JavaExpression.from(
                            e.renderToString(),
                            expectedType,
                            e.getRefs(),
                            e.getStaticWildcardImports()));
        }

        // MapperS/MapperC → List (emits `new ArrayList<>`). Copilot round 15:
        // the needsArrayList template-flag path only fires on outputIsMulti,
        // so an intermediate alias that coerces a Mapper wrapper into a List
        // in a single-valued function would miss the ArrayList import
        // post-C3c.2. Wire a structured ref so the channel is complete.
        if ((typeUtil.isMapperS(actualType) || typeUtil.isMapperC(actualType))
                && typeUtil.isList(expectedType)) {
            return expr.mapExpression(e ->
                    JavaExpression.from(
                            "new ArrayList<>(" + e.renderToString() + ".getMulti())",
                            expectedType,
                            HandlerHelper.union(e.getRefs(), Set.of(HandlerHelper.ARRAY_LIST)),
                            e.getStaticWildcardImports()));
        }

        // List → MapperC
        if (typeUtil.isList(actualType) && typeUtil.isMapperC(expectedType)) {
            return expr.mapExpression(e ->
                    JavaExpression.from(
                            "MapperC.of(" + e.renderToString() + ")",
                            expectedType,
                            HandlerHelper.union(e.getRefs(), Set.of(HandlerHelper.MAPPER_C)),
                            e.getStaticWildcardImports()));
        }

        // List → MapperS
        if (typeUtil.isList(actualType) && typeUtil.isMapperS(expectedType)) {
            return expr.mapExpression(e ->
                    JavaExpression.from(
                            "MapperS.of(MapperC.of(" + e.renderToString() + ").get())",
                            expectedType,
                            HandlerHelper.union(e.getRefs(), Set.of(HandlerHelper.MAPPER_S, HandlerHelper.MAPPER_C)),
                            e.getStaticWildcardImports()));
        }

        // MapperListOfLists → MapperC: flatten nested lists (instance method)
        if (typeUtil.isMapperListOfLists(actualType) && typeUtil.isMapperC(expectedType)) {
            return expr.mapExpression(e ->
                    JavaExpression.from(
                            e.renderToString() + ".flattenList()",
                            expectedType,
                            e.getRefs(),
                            e.getStaticWildcardImports()));
        }

        // MapperC → MapperListOfLists: identity
        if (typeUtil.isMapperC(actualType) && typeUtil.isMapperListOfLists(expectedType)) {
            return expr.mapExpression(e ->
                    JavaExpression.from(
                            e.renderToString(),
                            expectedType,
                            e.getRefs(),
                            e.getStaticWildcardImports()));
        }

        // No wrapper change needed — return expression (possibly with changed item type above)
        return expr;
    }

    /**
     * Check if two types use the same wrapper kind (both MapperS, both MapperC, etc.).
     */
    private boolean sameWrapperKind(JavaType a, JavaType b, JavaTypeUtil typeUtil) {
        return (typeUtil.isMapperS(a) && typeUtil.isMapperS(b))
                || (typeUtil.isMapperC(a) && typeUtil.isMapperC(b))
                || (typeUtil.isMapper(a) && typeUtil.isMapper(b))
                || (typeUtil.isList(a) && typeUtil.isList(b))
                || (typeUtil.isMapperListOfLists(a) && typeUtil.isMapperListOfLists(b))
                || (typeUtil.isComparisonResult(a) && typeUtil.isComparisonResult(b));
    }

}
