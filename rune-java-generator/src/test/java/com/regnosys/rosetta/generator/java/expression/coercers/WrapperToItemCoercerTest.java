package com.regnosys.rosetta.generator.java.expression.coercers;

import com.regnosys.rosetta.generator.java.expression.TypeCoercionService;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.rosetta.util.types.JavaPrimitiveType;
import com.rosetta.util.types.JavaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WrapperToItemCoercerTest {

    private WrapperToItemCoercer coercer;
    private JavaTypeUtil typeUtil;
    private TypeCoercionService service;
    private JavaStatementScope scope;

    @BeforeEach
    void setUp() {
        typeUtil = new JavaTypeUtil();
        service = new TypeCoercionService(typeUtil);
        coercer = new WrapperToItemCoercer();
        scope = new JavaStatementScope("test", null);
    }

    // =========================================================================
    // MapperS → item: .get()
    // =========================================================================

    @Test
    void MapperS_to_item_get() {
        var actual = typeUtil.wrap(typeUtil.MAPPER_S, typeUtil.INTEGER);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, typeUtil.INTEGER, true, scope, typeUtil, service);
        assertEquals("x.get()", render(result));
    }

    // =========================================================================
    // MapperC → item: .get()
    // =========================================================================

    @Test
    void MapperC_to_item_get() {
        var actual = typeUtil.wrap(typeUtil.MAPPER_C, typeUtil.STRING);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, typeUtil.STRING, true, scope, typeUtil, service);
        assertEquals("x.get()", render(result));
    }

    // =========================================================================
    // Mapper → item: .get()
    // =========================================================================

    @Test
    void Mapper_to_item_get() {
        var actual = typeUtil.wrap(typeUtil.MAPPER, typeUtil.INTEGER);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, typeUtil.INTEGER, true, scope, typeUtil, service);
        assertEquals("x.get()", render(result));
    }

    // =========================================================================
    // ComparisonResult → primitive boolean: .getOrDefault(false)
    // =========================================================================

    @Test
    void ComparisonResult_to_primitive_boolean() {
        var expr = JavaExpression.from("x", typeUtil.COMPARISON_RESULT);
        var result = coercer.coerce(expr, typeUtil.COMPARISON_RESULT, JavaPrimitiveType.BOOLEAN,
                true, scope, typeUtil, service);
        assertEquals("x.getOrDefault(false)", render(result));
    }

    // =========================================================================
    // ComparisonResult → Boolean: .get()
    // =========================================================================

    @Test
    void ComparisonResult_to_boxed_boolean() {
        var expr = JavaExpression.from("x", typeUtil.COMPARISON_RESULT);
        var result = coercer.coerce(expr, typeUtil.COMPARISON_RESULT, typeUtil.BOOLEAN,
                true, scope, typeUtil, service);
        assertEquals("x.get()", render(result));
    }

    // =========================================================================
    // MapperS → primitive boolean: .getOrDefault(false)
    // =========================================================================

    @Test
    void MapperS_to_primitive_boolean() {
        var actual = typeUtil.wrap(typeUtil.MAPPER_S, typeUtil.BOOLEAN);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, JavaPrimitiveType.BOOLEAN,
                true, scope, typeUtil, service);
        assertEquals("x.getOrDefault(false)", render(result));
    }

    // =========================================================================
    // MapperS → primitive int: .getOrDefault(0)
    // =========================================================================

    @Test
    void MapperS_to_primitive_int() {
        var actual = typeUtil.wrap(typeUtil.MAPPER_S, typeUtil.INTEGER);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, JavaPrimitiveType.INT,
                true, scope, typeUtil, service);
        assertEquals("x.getOrDefault(0)", render(result));
    }

    // =========================================================================
    // List → item: via MapperC.of(list).get()
    // =========================================================================

    @Test
    void List_to_item() {
        var actual = typeUtil.wrap(typeUtil.LIST, typeUtil.STRING);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, typeUtil.STRING, true, scope, typeUtil, service);
        assertEquals("MapperC.of(x).get()", render(result));
    }

    // =========================================================================
    // MapperListOfLists → item: flatten then get
    // =========================================================================

    @Test
    void MapperListOfLists_to_item_flatten_then_get() {
        var actual = typeUtil.wrap(typeUtil.MAPPER_LIST_OF_LISTS, typeUtil.STRING);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, typeUtil.STRING, true, scope, typeUtil, service);
        assertEquals("MapperC.of(x.flattenList()).get()", render(result));
    }

    // =========================================================================
    // MapperS<Long> → primitive int: unwrap to Long first, then item-coerce
    // =========================================================================

    @Test
    void MapperS_Long_to_primitive_int() {
        var actual = typeUtil.wrap(typeUtil.MAPPER_S, typeUtil.LONG);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, JavaPrimitiveType.INT,
                true, scope, typeUtil, service);
        // Must NOT emit x.getOrDefault(0) — item types differ (Long vs Integer)
        // Should unwrap to Long via .get(), then item-coerce Long→int
        String rendered = render(result);
        assertFalse(rendered.equals("x.getOrDefault(0)"),
                "Should not use getOrDefault when item type differs from primitive");
        assertTrue(rendered.contains("x.get()"),
                "Should unwrap via .get() first; got: " + rendered);
    }

    // =========================================================================
    // Composed: MapperS<Integer> → BigDecimal (unwrap + item coerce)
    // =========================================================================

    @Test
    void MapperS_Integer_to_BigDecimal() {
        var actual = typeUtil.wrap(typeUtil.MAPPER_S, typeUtil.INTEGER);
        var expr = JavaExpression.from("x", actual);
        var result = coercer.coerce(expr, actual, typeUtil.BIG_DECIMAL, true, scope, typeUtil, service);
        assertEquals("BigDecimal.valueOf(x.get())", render(result));
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private String render(JavaStatementBuilder builder) {
        if (builder instanceof JavaExpression e) {
            return e.renderToString();
        }
        return ((JavaExpression) builder.collapseToSingleExpression(scope)).renderToString();
    }
}
