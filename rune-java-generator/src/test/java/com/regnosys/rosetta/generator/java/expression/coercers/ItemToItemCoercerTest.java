package com.regnosys.rosetta.generator.java.expression.coercers;

import com.regnosys.rosetta.generator.java.expression.TypeCoercionService;
import com.regnosys.rosetta.generator.java.scoping.JavaPackageName;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RGeneratedJavaClass;
import com.regnosys.rosetta.generator.java.types.RJavaFieldWithMeta;
import com.regnosys.rosetta.generator.java.types.RJavaReferenceWithMeta;
import com.rosetta.util.types.JavaPrimitiveType;
import com.rosetta.util.types.JavaReferenceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemToItemCoercerTest {

    private ItemToItemCoercer coercer;
    private JavaTypeUtil typeUtil;
    private TypeCoercionService service;
    private JavaStatementScope scope;

    @BeforeEach
    void setUp() {
        typeUtil = new JavaTypeUtil();
        service = new TypeCoercionService(typeUtil);
        coercer = new ItemToItemCoercer();
        scope = new JavaStatementScope("test", null);
    }

    // =========================================================================
    // Widening: int → long
    // =========================================================================

    @Test
    void int_to_long() {
        // v3.2 seat 13 (D53, the M5d heal - oracle group conv-int-literal-long-output): a BOXED Integer into a Long
        // takes `.longValue()` (the released plugin's `r = n.longValue();`); the widening cast is the PRIMITIVE's
        // form (int_primitive_to_long below, `r = (long) 42;`). Until seat 13 this pin carried the cast for the
        // boxed item with no golden witness.
        var expr = JavaExpression.from("x", typeUtil.INTEGER);
        var result = coercer.coerce(expr, typeUtil.INTEGER, typeUtil.LONG, true, scope, typeUtil, service);
        assertEquals("x.longValue()", render(result));
    }

    @Test
    void int_primitive_to_long() {
        var expr = JavaExpression.from("42", com.rosetta.util.types.JavaPrimitiveType.INT);
        var result = coercer.coerce(expr, com.rosetta.util.types.JavaPrimitiveType.INT, typeUtil.LONG, true, scope,
                typeUtil, service);
        assertEquals("(long) 42", render(result));
    }

    // =========================================================================
    // Widening: int → BigInteger
    // =========================================================================

    @Test
    void int_to_BigInteger() {
        var expr = JavaExpression.from("x", typeUtil.INTEGER);
        var result = coercer.coerce(expr, typeUtil.INTEGER, typeUtil.BIG_INTEGER, true, scope, typeUtil, service);
        assertEquals("BigInteger.valueOf(x)", render(result));
    }

    // =========================================================================
    // Widening: int → BigDecimal
    // =========================================================================

    @Test
    void int_to_BigDecimal() {
        var expr = JavaExpression.from("x", typeUtil.INTEGER);
        var result = coercer.coerce(expr, typeUtil.INTEGER, typeUtil.BIG_DECIMAL, true, scope, typeUtil, service);
        assertEquals("BigDecimal.valueOf(x)", render(result));
    }

    // =========================================================================
    // Narrowing: long → int (throwOnFail=true/false)
    // =========================================================================

    @Test
    void long_to_int_throwOnFail_true() {
        var expr = JavaExpression.from("x", typeUtil.LONG);
        var result = coercer.coerce(expr, typeUtil.LONG, typeUtil.INTEGER, true, scope, typeUtil, service);
        assertEquals("Math.toIntExact(x)", render(result));
    }

    @Test
    void long_to_int_throwOnFail_false() {
        var expr = JavaExpression.from("x", typeUtil.LONG);
        var result = coercer.coerce(expr, typeUtil.LONG, typeUtil.INTEGER, false, scope, typeUtil, service);
        assertEquals("x <= Integer.MAX_VALUE && x >= Integer.MIN_VALUE ? (int) x : null", render(result));
    }

    // =========================================================================
    // Widening: long → BigInteger
    // =========================================================================

    @Test
    void long_to_BigInteger() {
        var expr = JavaExpression.from("x", typeUtil.LONG);
        var result = coercer.coerce(expr, typeUtil.LONG, typeUtil.BIG_INTEGER, true, scope, typeUtil, service);
        assertEquals("BigInteger.valueOf(x)", render(result));
    }

    // =========================================================================
    // Widening: long → BigDecimal
    // =========================================================================

    @Test
    void long_to_BigDecimal() {
        var expr = JavaExpression.from("x", typeUtil.LONG);
        var result = coercer.coerce(expr, typeUtil.LONG, typeUtil.BIG_DECIMAL, true, scope, typeUtil, service);
        assertEquals("BigDecimal.valueOf(x)", render(result));
    }

    // =========================================================================
    // Narrowing: BigInteger → int (throwOnFail=true/false)
    // =========================================================================

    @Test
    void BigInteger_to_int_throwOnFail_true() {
        var expr = JavaExpression.from("x", typeUtil.BIG_INTEGER);
        var result = coercer.coerce(expr, typeUtil.BIG_INTEGER, typeUtil.INTEGER, true, scope, typeUtil, service);
        assertEquals("x.intValueExact()", render(result));
    }

    @Test
    void BigInteger_to_int_throwOnFail_false() {
        var expr = JavaExpression.from("x", typeUtil.BIG_INTEGER);
        var result = coercer.coerce(expr, typeUtil.BIG_INTEGER, typeUtil.INTEGER, false, scope, typeUtil, service);
        assertEquals("BigInteger.valueOf(x.intValue()).equals(x) ? x.intValue() : null", render(result));
    }

    // =========================================================================
    // Narrowing: BigInteger → long (throwOnFail=true/false)
    // =========================================================================

    @Test
    void BigInteger_to_long_throwOnFail_true() {
        var expr = JavaExpression.from("x", typeUtil.BIG_INTEGER);
        var result = coercer.coerce(expr, typeUtil.BIG_INTEGER, typeUtil.LONG, true, scope, typeUtil, service);
        assertEquals("x.longValueExact()", render(result));
    }

    @Test
    void BigInteger_to_long_throwOnFail_false() {
        var expr = JavaExpression.from("x", typeUtil.BIG_INTEGER);
        var result = coercer.coerce(expr, typeUtil.BIG_INTEGER, typeUtil.LONG, false, scope, typeUtil, service);
        assertEquals("BigInteger.valueOf(x.longValue()).equals(x) ? x.longValue() : null", render(result));
    }

    // =========================================================================
    // Widening: BigInteger → BigDecimal
    // =========================================================================

    @Test
    void BigInteger_to_BigDecimal() {
        var expr = JavaExpression.from("x", typeUtil.BIG_INTEGER);
        var result = coercer.coerce(expr, typeUtil.BIG_INTEGER, typeUtil.BIG_DECIMAL, true, scope, typeUtil, service);
        assertEquals("new BigDecimal(x)", render(result));
    }

    // =========================================================================
    // Narrowing: BigDecimal → int (throwOnFail=true/false)
    // =========================================================================

    @Test
    void BigDecimal_to_int_throwOnFail_true() {
        var expr = JavaExpression.from("x", typeUtil.BIG_DECIMAL);
        var result = coercer.coerce(expr, typeUtil.BIG_DECIMAL, typeUtil.INTEGER, true, scope, typeUtil, service);
        assertEquals("x.intValueExact()", render(result));
    }

    @Test
    void BigDecimal_to_int_throwOnFail_false() {
        var expr = JavaExpression.from("x", typeUtil.BIG_DECIMAL);
        var result = coercer.coerce(expr, typeUtil.BIG_DECIMAL, typeUtil.INTEGER, false, scope, typeUtil, service);
        assertEquals("BigDecimal.valueOf(x.intValue()).compareTo(x) == 0 ? x.intValue() : null", render(result));
    }

    // =========================================================================
    // Narrowing: BigDecimal → long (throwOnFail=true/false)
    // =========================================================================

    @Test
    void BigDecimal_to_long_throwOnFail_true() {
        var expr = JavaExpression.from("x", typeUtil.BIG_DECIMAL);
        var result = coercer.coerce(expr, typeUtil.BIG_DECIMAL, typeUtil.LONG, true, scope, typeUtil, service);
        assertEquals("x.longValueExact()", render(result));
    }

    @Test
    void BigDecimal_to_long_throwOnFail_false() {
        var expr = JavaExpression.from("x", typeUtil.BIG_DECIMAL);
        var result = coercer.coerce(expr, typeUtil.BIG_DECIMAL, typeUtil.LONG, false, scope, typeUtil, service);
        assertEquals("BigDecimal.valueOf(x.longValue()).compareTo(x) == 0 ? x.longValue() : null", render(result));
    }

    // =========================================================================
    // Narrowing: BigDecimal → BigInteger (throwOnFail=true/false)
    // =========================================================================

    @Test
    void BigDecimal_to_BigInteger_throwOnFail_true() {
        var expr = JavaExpression.from("x", typeUtil.BIG_DECIMAL);
        var result = coercer.coerce(expr, typeUtil.BIG_DECIMAL, typeUtil.BIG_INTEGER, true, scope, typeUtil, service);
        assertEquals("x.toBigIntegerExact()", render(result));
    }

    @Test
    void BigDecimal_to_BigInteger_throwOnFail_false() {
        var expr = JavaExpression.from("x", typeUtil.BIG_DECIMAL);
        var result = coercer.coerce(expr, typeUtil.BIG_DECIMAL, typeUtil.BIG_INTEGER, false, scope, typeUtil, service);
        assertEquals("new BigDecimal(x.toBigInteger()).compareTo(x) == 0 ? x.toBigInteger() : null", render(result));
    }

    // =========================================================================
    // A PRIMITIVE actual (the count render's `int` stamp since PR #622): the box's
    // table entry, or identity with its own box (the promotion at the coercer's entry)
    // =========================================================================

    @Test
    void primitive_int_to_Long_takesTheBoxesTableEntry() {
        var expr = JavaExpression.from("x", JavaPrimitiveType.INT);
        var result = coercer.coerce(expr, JavaPrimitiveType.INT, typeUtil.LONG, true, scope, typeUtil, service);
        assertEquals("(long) x", render(result));
    }

    @Test
    void primitive_int_to_BigDecimal_takesTheBoxesTableEntry() {
        var expr = JavaExpression.from("x", JavaPrimitiveType.INT);
        var result = coercer.coerce(expr, JavaPrimitiveType.INT, typeUtil.BIG_DECIMAL, true, scope, typeUtil, service);
        assertEquals("BigDecimal.valueOf(x)", render(result));
    }

    @Test
    void primitive_int_to_Integer_isIdentity() {
        var expr = JavaExpression.from("x", JavaPrimitiveType.INT);
        var result = coercer.coerce(expr, JavaPrimitiveType.INT, typeUtil.INTEGER, true, scope, typeUtil, service);
        assertSame(expr, result);
    }

    // =========================================================================
    // Unrecognized number types return unchanged
    // =========================================================================

    @Test
    void same_type_returns_unchanged() {
        var expr = JavaExpression.from("x", typeUtil.STRING);
        var result = coercer.coerce(expr, typeUtil.STRING, typeUtil.STRING, true, scope, typeUtil, service);
        assertSame(expr, result);
    }

    // =========================================================================
    // Meta-value unwrap: RJavaWithMetaValue → value type via getValue()
    // (mirror upstream metaToItemConversionExpression — the ~99% coercion form)
    // =========================================================================

    @Test
    void meta_unwrap_ReferenceWithMetaParty_to_Party() {
        JavaReferenceType party = partyValueType();
        var meta = new RJavaReferenceWithMeta(party, metafieldsPackage(), typeUtil);
        var expr = JavaExpression.from("referenceWithMetaParty", meta);
        var result = coercer.coerce(expr, meta, party, true, scope, typeUtil, service);
        assertEquals("referenceWithMetaParty.getValue()", render(result));
    }

    @Test
    void meta_unwrap_FieldWithMetaString_to_String() {
        var meta = new RJavaFieldWithMeta(typeUtil.STRING, metafieldsPackage(), typeUtil);
        var expr = JavaExpression.from("fieldWithMetaString", meta);
        var result = coercer.coerce(expr, meta, typeUtil.STRING, true, scope, typeUtil, service);
        assertEquals("fieldWithMetaString.getValue()", render(result));
    }

    /** A synthetic generated POJO named {@code Party} to act as a meta value type. */
    private JavaReferenceType partyValueType() {
        return (JavaReferenceType) RGeneratedJavaClass.createWithSuperclass(
                JavaPackageName.splitOnDotsAndEscape("com.rosetta.test"), "Party",
                typeUtil.ROSETTA_MODEL_OBJECT);
    }

    private JavaPackageName metafieldsPackage() {
        return JavaPackageName.splitOnDotsAndEscape("com.rosetta.test.metafields");
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
