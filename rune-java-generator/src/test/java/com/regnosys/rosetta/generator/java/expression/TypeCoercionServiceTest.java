package com.regnosys.rosetta.generator.java.expression;

import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaLiteral;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaPrimitiveType;
import com.rosetta.util.types.JavaReferenceType;
import com.rosetta.util.types.JavaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TypeCoercionServiceTest {

    private TypeCoercionService service;
    private JavaTypeUtil typeUtil;
    private JavaStatementScope scope;

    @BeforeEach
    void setUp() {
        typeUtil = new JavaTypeUtil();
        service = new TypeCoercionService(typeUtil);
        scope = new JavaStatementScope("test", null);
    }

    // =========================================================================
    // THE NO-OP IDENTITY CONTRACT (PR #622): every path that performs no conversion
    // returns the SAME instance - the contract the identity-decline seats read by
    // reference (FunctionExpressionRenderer.renderNumericOutputCoerceOrNull arms a/b,
    // NavigationHandler.numericInputParamCoerceOrNull) instead of comparing text.
    // =========================================================================

    @Test
    void noOpCoercion_returnsTheSameInstance() {
        var x = JavaExpression.from("x", typeUtil.INTEGER);
        // early exit 3: identity
        assertSame(x, service.coerceExpression(x, typeUtil.INTEGER, typeUtil.INTEGER, true, scope));
        // early exit 4: auto-boxing, both directions
        assertSame(x, service.coerceExpression(x, typeUtil.INTEGER, JavaPrimitiveType.INT, true, scope));
        var p = JavaExpression.from("p", JavaPrimitiveType.INT);
        assertSame(p, service.coerceExpression(p, JavaPrimitiveType.INT, typeUtil.INTEGER, true, scope));
        // item->item with no conversion applicable (a non-numeric pair)
        assertSame(x, service.coerceExpression(x, typeUtil.INTEGER, typeUtil.STRING, true, scope));
        // the number table's fall-through: a Number subtype the twelve-entry table does not
        // cover (mapExpression applies the mapper to `this`; the mapper returns its input)
        JavaClass<Double> boxedDouble = JavaClass.from(Double.class);
        assertTrue(typeUtil.extendsNumber(boxedDouble), "the control needs a Number subtype");
        assertSame(x, service.coerceExpression(x, typeUtil.INTEGER, boxedDouble, true, scope));
        // the positive control: a real conversion is a NEW instance
        assertNotSame(x, service.coerceExpression(x, typeUtil.INTEGER, typeUtil.BIG_DECIMAL, true, scope));
    }

    // =========================================================================
    // Early exit: identity
    // =========================================================================

    @Test
    void identity_returns_same_expression() {
        var expr = JavaExpression.from("x", typeUtil.INTEGER);
        var result = service.coerce(expr, typeUtil.INTEGER, typeUtil.INTEGER, scope);
        assertSame(expr, result);
    }

    @Test
    void identity_big_decimal_returns_same_expression() {
        var expr = JavaExpression.from("x", typeUtil.BIG_DECIMAL);
        var result = service.coerce(expr, typeUtil.BIG_DECIMAL, typeUtil.BIG_DECIMAL, scope);
        assertSame(expr, result);
    }

    // =========================================================================
    // Early exit: null actual/expected type
    // =========================================================================

    @Test
    void null_actual_type_returns_same_expression() {
        var expr = JavaExpression.from("x", null);
        var result = service.coerce(expr, null, typeUtil.INTEGER, scope);
        assertSame(expr, result);
    }

    @Test
    void null_expected_type_returns_same_expression() {
        var expr = JavaExpression.from("x", typeUtil.INTEGER);
        var result = service.coerce(expr, typeUtil.INTEGER, null, scope);
        assertSame(expr, result);
    }

    // =========================================================================
    // Early exit: auto-boxing
    // =========================================================================

    @Test
    void autoboxing_int_to_Integer_returns_same() {
        var expr = JavaExpression.from("x", JavaPrimitiveType.INT);
        var result = service.coerce(expr, JavaPrimitiveType.INT, typeUtil.INTEGER, scope);
        assertSame(expr, result);
    }

    @Test
    void autoboxing_Integer_to_int_returns_same() {
        var expr = JavaExpression.from("x", typeUtil.INTEGER);
        var result = service.coerce(expr, typeUtil.INTEGER, JavaPrimitiveType.INT, scope);
        assertSame(expr, result);
    }

    @Test
    void autoboxing_long_to_Long_returns_same() {
        var expr = JavaExpression.from("x", JavaPrimitiveType.LONG);
        var result = service.coerce(expr, JavaPrimitiveType.LONG, typeUtil.LONG, scope);
        assertSame(expr, result);
    }

    // =========================================================================
    // Early exit: primitive VOID throws IllegalArgumentException
    // =========================================================================

    @Test
    void primitive_void_actual_throws_IllegalArgumentException() {
        var expr = JavaExpression.from("x", JavaPrimitiveType.VOID);
        assertThrows(IllegalArgumentException.class,
                () -> service.coerce(expr, JavaPrimitiveType.VOID, typeUtil.INTEGER, scope));
    }

    // =========================================================================
    // Early exit: NULL_TYPE item returns emptyValueFor
    // =========================================================================

    @Test
    void null_type_actual_returns_emptyValueFor() {
        // NULL_TYPE as the actual type (non-wrapper) — getItemType returns itself
        var expr = JavaExpression.from("null", JavaReferenceType.NULL_TYPE);
        var result = service.coerce(expr, JavaReferenceType.NULL_TYPE, typeUtil.INTEGER, scope);
        // emptyValueFor(INTEGER) → JavaLiteral.NULL
        assertSame(JavaLiteral.NULL, result);
    }

    // =========================================================================
    // emptyValueFor
    // =========================================================================

    /**
     * v3.2 seat 9 (PR #630, F8 / D47 - the {@code nothing} render law): the empty LIST carries its item witness -
     * upstream's {@code Collections.<Integer>emptyList()} ({@code TypeCoercionService.empty}) - since the render-law
     * commit; this pin had held the bare {@code Collections.emptyList()} and the c6 lane sweep at {@code 1460e3380}
     * found it RED under every generator mutation (the whole-suite law, missed by the seat's targeted fix runs).
     */
    @Test
    void emptyValueFor_list_returns_collections_emptyList() {
        var listType = typeUtil.wrap(typeUtil.LIST, typeUtil.INTEGER);
        var result = service.emptyValueFor(listType);
        assertInstanceOf(JavaExpression.class, result);
        assertEquals("Collections.<Integer>emptyList()", ((JavaExpression) result).renderToString());
    }

    @Test
    void emptyValueFor_mapperS_returns_ofNull_with_type_param() {
        var mapperSType = typeUtil.wrap(typeUtil.MAPPER_S, typeUtil.INTEGER);
        var result = service.emptyValueFor(mapperSType);
        assertInstanceOf(JavaExpression.class, result);
        assertEquals("MapperS.<Integer>ofNull()", ((JavaExpression) result).renderToString());
    }

    @Test
    void emptyValueFor_mapperC_returns_ofNull_with_type_param() {
        var mapperCType = typeUtil.wrap(typeUtil.MAPPER_C, typeUtil.STRING);
        var result = service.emptyValueFor(mapperCType);
        assertInstanceOf(JavaExpression.class, result);
        assertEquals("MapperC.<String>ofNull()", ((JavaExpression) result).renderToString());
    }

    @Test
    void emptyValueFor_comparisonResult_returns_ofEmpty() {
        var result = service.emptyValueFor(typeUtil.COMPARISON_RESULT);
        assertInstanceOf(JavaExpression.class, result);
        assertEquals("ComparisonResult.ofEmpty()", ((JavaExpression) result).renderToString());
    }

    @Test
    void emptyValueFor_other_returns_null_literal() {
        var result = service.emptyValueFor(typeUtil.INTEGER);
        assertSame(JavaLiteral.NULL, result);
    }
}
