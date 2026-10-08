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

class ItemToWrapperCoercerTest {

    private ItemToWrapperCoercer coercer;
    private JavaTypeUtil typeUtil;
    private TypeCoercionService service;
    private JavaStatementScope scope;

    @BeforeEach
    void setUp() {
        typeUtil = new JavaTypeUtil();
        service = new TypeCoercionService(typeUtil);
        coercer = new ItemToWrapperCoercer();
        scope = new JavaStatementScope("test", null);
    }

    // =========================================================================
    // Item → MapperS
    // =========================================================================

    @Test
    void item_to_MapperS() {
        var expr = JavaExpression.from("x", typeUtil.INTEGER);
        var expected = typeUtil.wrap(typeUtil.MAPPER_S, typeUtil.INTEGER);
        var result = coercer.coerce(expr, typeUtil.INTEGER, expected, true, scope, typeUtil, service);
        assertEquals("MapperS.of(x)", render(result));
    }

    // =========================================================================
    // Item → MapperC
    // =========================================================================

    @Test
    void item_to_MapperC() {
        var expr = JavaExpression.from("x", typeUtil.INTEGER);
        var expected = typeUtil.wrap(typeUtil.MAPPER_C, typeUtil.INTEGER);
        var result = coercer.coerce(expr, typeUtil.INTEGER, expected, true, scope, typeUtil, service);
        assertEquals("MapperC.of(Collections.singletonList(x))", render(result));
    }

    // =========================================================================
    // Item → Mapper (base)
    // =========================================================================

    @Test
    void item_to_Mapper_base() {
        var expr = JavaExpression.from("x", typeUtil.INTEGER);
        var expected = typeUtil.wrap(typeUtil.MAPPER, typeUtil.INTEGER);
        var result = coercer.coerce(expr, typeUtil.INTEGER, expected, true, scope, typeUtil, service);
        assertEquals("MapperS.of(x)", render(result));
    }

    // =========================================================================
    // Item → List
    // =========================================================================

    @Test
    void item_to_List() {
        var expr = JavaExpression.from("x", typeUtil.INTEGER);
        var expected = typeUtil.wrap(typeUtil.LIST, typeUtil.INTEGER);
        var result = coercer.coerce(expr, typeUtil.INTEGER, expected, true, scope, typeUtil, service);
        assertEquals("Collections.singletonList(x)", render(result));
    }

    // =========================================================================
    // Item → ComparisonResult
    // =========================================================================

    @Test
    void item_to_ComparisonResult() {
        var expr = JavaExpression.from("x", typeUtil.BOOLEAN);
        var result = coercer.coerce(expr, typeUtil.BOOLEAN, typeUtil.COMPARISON_RESULT,
                true, scope, typeUtil, service);
        assertEquals("ComparisonResult.ofNullSafe(MapperS.of(x))", render(result));
    }

    // =========================================================================
    // Item → MapperListOfLists throws
    // =========================================================================

    @Test
    void item_to_MapperListOfLists_throws() {
        var expr = JavaExpression.from("x", typeUtil.INTEGER);
        var expected = typeUtil.wrap(typeUtil.MAPPER_LIST_OF_LISTS, typeUtil.INTEGER);
        assertThrows(IllegalArgumentException.class,
                () -> coercer.coerce(expr, typeUtil.INTEGER, expected, true, scope, typeUtil, service));
    }

    // =========================================================================
    // Composed: int → MapperS<BigDecimal> (item coercion + wrap)
    // =========================================================================

    @Test
    void int_to_MapperS_of_BigDecimal() {
        var expr = JavaExpression.from("x", typeUtil.INTEGER);
        var expected = typeUtil.wrap(typeUtil.MAPPER_S, typeUtil.BIG_DECIMAL);
        var result = coercer.coerce(expr, typeUtil.INTEGER, expected, true, scope, typeUtil, service);
        assertEquals("MapperS.of(BigDecimal.valueOf(x))", render(result));
    }

    // =========================================================================
    // Composed: int → List<BigDecimal> (item coercion + wrap)
    // =========================================================================

    @Test
    void int_to_List_of_BigDecimal() {
        var expr = JavaExpression.from("x", typeUtil.INTEGER);
        var expected = typeUtil.wrap(typeUtil.LIST, typeUtil.BIG_DECIMAL);
        var result = coercer.coerce(expr, typeUtil.INTEGER, expected, true, scope, typeUtil, service);
        assertEquals("Collections.singletonList(BigDecimal.valueOf(x))", render(result));
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
