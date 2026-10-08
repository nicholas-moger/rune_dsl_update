package com.regnosys.rosetta.generator.java.expression.coercers;

import com.regnosys.rosetta.generator.java.expression.TypeCoercionService;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.rosetta.util.types.JavaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WrapperToWrapperCoercerTest {

    private WrapperToWrapperCoercer coercer;
    private WrappedItemCoercer wrappedItem;
    private JavaTypeUtil typeUtil;
    private TypeCoercionService service;
    private JavaStatementScope scope;

    @BeforeEach
    void setUp() {
        typeUtil = new JavaTypeUtil();
        service = new TypeCoercionService(typeUtil);
        wrappedItem = new WrappedItemCoercer();
        coercer = new WrapperToWrapperCoercer(wrappedItem);
        scope = new JavaStatementScope("test", null);
    }

    // =========================================================================
    // MapperS → MapperC
    // =========================================================================

    @Test
    void MapperS_to_MapperC() {
        var actual = typeUtil.wrap(typeUtil.MAPPER_S, typeUtil.INTEGER);
        var expected = typeUtil.wrap(typeUtil.MAPPER_C, typeUtil.INTEGER);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, expected, true, scope, typeUtil, service);
        assertEquals("MapperC.of(x)", render(result));
    }

    // =========================================================================
    // MapperC → MapperS
    // =========================================================================

    @Test
    void MapperC_to_MapperS() {
        var actual = typeUtil.wrap(typeUtil.MAPPER_C, typeUtil.INTEGER);
        var expected = typeUtil.wrap(typeUtil.MAPPER_S, typeUtil.INTEGER);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, expected, true, scope, typeUtil, service);
        assertEquals("MapperS.of(x.get())", render(result));
    }

    // =========================================================================
    // MapperS → List
    // =========================================================================

    @Test
    void MapperS_to_List() {
        var actual = typeUtil.wrap(typeUtil.MAPPER_S, typeUtil.STRING);
        var expected = typeUtil.wrap(typeUtil.LIST, typeUtil.STRING);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, expected, true, scope, typeUtil, service);
        assertEquals("new ArrayList<>(x.getMulti())", render(result));
    }

    // =========================================================================
    // MapperC → List
    // =========================================================================

    @Test
    void MapperC_to_List() {
        var actual = typeUtil.wrap(typeUtil.MAPPER_C, typeUtil.STRING);
        var expected = typeUtil.wrap(typeUtil.LIST, typeUtil.STRING);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, expected, true, scope, typeUtil, service);
        assertEquals("new ArrayList<>(x.getMulti())", render(result));
    }

    // =========================================================================
    // List → MapperC
    // =========================================================================

    @Test
    void List_to_MapperC() {
        var actual = typeUtil.wrap(typeUtil.LIST, typeUtil.STRING);
        var expected = typeUtil.wrap(typeUtil.MAPPER_C, typeUtil.STRING);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, expected, true, scope, typeUtil, service);
        assertEquals("MapperC.of(x)", render(result));
    }

    // =========================================================================
    // List → MapperS
    // =========================================================================

    @Test
    void List_to_MapperS() {
        var actual = typeUtil.wrap(typeUtil.LIST, typeUtil.STRING);
        var expected = typeUtil.wrap(typeUtil.MAPPER_S, typeUtil.STRING);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, expected, true, scope, typeUtil, service);
        assertEquals("MapperS.of(MapperC.of(x).get())", render(result));
    }

    // =========================================================================
    // ComparisonResult → MapperS<Boolean>
    // =========================================================================

    @Test
    void ComparisonResult_to_MapperS_Boolean() {
        var expected = typeUtil.wrap(typeUtil.MAPPER_S, typeUtil.BOOLEAN);
        var expr = JavaExpression.from("x", typeUtil.COMPARISON_RESULT);
        var result = coercer.coerce(expr, typeUtil.COMPARISON_RESULT, expected, true, scope, typeUtil, service);
        assertEquals("x.asMapper()", render(result));
    }

    // =========================================================================
    // MapperS<Boolean> → ComparisonResult
    // =========================================================================

    @Test
    void MapperS_Boolean_to_ComparisonResult() {
        var actual = typeUtil.wrap(typeUtil.MAPPER_S, typeUtil.BOOLEAN);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, typeUtil.COMPARISON_RESULT, true, scope, typeUtil, service);
        assertEquals("ComparisonResult.ofNullSafe(x)", render(result));
    }

    // =========================================================================
    // Wildcard to concrete: .map("Make mutable", Function.identity())
    // =========================================================================

    @Test
    void wildcard_MapperS_to_concrete_MapperS() {
        var actual = typeUtil.wrapExtends(typeUtil.MAPPER_S, typeUtil.INTEGER);
        var expected = typeUtil.wrap(typeUtil.MAPPER_S, typeUtil.INTEGER);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, expected, true, scope, typeUtil, service);
        assertEquals("x.map(\"Make mutable\", Function.identity())", render(result));
    }

    // =========================================================================
    // Wildcard to concrete: List<? extends T> → List<T> uses new ArrayList<>()
    // =========================================================================

    @Test
    void wildcard_List_to_concrete_List() {
        var actual = typeUtil.wrapExtends(typeUtil.LIST, typeUtil.STRING);
        var expected = typeUtil.wrap(typeUtil.LIST, typeUtil.STRING);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, expected, true, scope, typeUtil, service);
        assertEquals("new ArrayList<>(x)", render(result));
    }

    // =========================================================================
    // MapperListOfLists → MapperC: flattenList()
    // =========================================================================

    @Test
    void MapperListOfLists_to_MapperC_flattenList() {
        var actual = typeUtil.wrap(typeUtil.MAPPER_LIST_OF_LISTS, typeUtil.INTEGER);
        var expected = typeUtil.wrap(typeUtil.MAPPER_C, typeUtil.INTEGER);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, expected, true, scope, typeUtil, service);
        assertEquals("x.flattenList()", render(result));
    }

    // =========================================================================
    // MapperC → MapperListOfLists: return as-is (type change only)
    // =========================================================================

    @Test
    void MapperC_to_MapperListOfLists_returnAsIs() {
        var actual = typeUtil.wrap(typeUtil.MAPPER_C, typeUtil.INTEGER);
        var expected = typeUtil.wrap(typeUtil.MAPPER_LIST_OF_LISTS, typeUtil.INTEGER);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, expected, true, scope, typeUtil, service);
        assertEquals("x", render(result));
    }

    // =========================================================================
    // Composed: MapperS<Integer> → MapperC<BigDecimal>
    // =========================================================================

    @Test
    void MapperS_Integer_to_MapperC_BigDecimal() {
        var actual = typeUtil.wrap(typeUtil.MAPPER_S, typeUtil.INTEGER);
        var expected = typeUtil.wrap(typeUtil.MAPPER_C, typeUtil.BIG_DECIMAL);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, expected, true, scope, typeUtil, service);
        // Byte-faithful composition: getMapperSItemConversionExpression (witness +
        // null-safe, type-derived param) inside the MapperS→MapperC wrapper
        // conversion (MapperC.of(...)). Matches upstream TypeCoercionService.
        assertEquals("MapperC.of(x.<BigDecimal>map(\"Type coercion\", integer -> integer == null ? null : BigDecimal.valueOf(integer)))", render(result));
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private String render(JavaStatementBuilder builder) {
        String rendered = builder instanceof JavaExpression e
                ? e.renderToString()
                : ((JavaExpression) builder.collapseToSingleExpression(scope)).renderToString();
        // facet meta_coercion_numbering: resolve the guarded arm's deferred
        // param sentinel as the renderer's finalizeDeferredNames does (no-op
        // for renders without a registered guarded coercion param).
        return scope.resolveDeferredCoercionNames(rendered);
    }
}
