package com.regnosys.rosetta.generator.java.expression.handlers;

import com.regnosys.rosetta.ast.expressions.literals.*;
import com.regnosys.rosetta.ast.expressions.references.REmptyLiteral;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompilerTest;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaLiteral;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LiteralHandlerTest {

    private LiteralHandler handler;
    private ExpressionCompiler compiler;
    private ExpressionContext ctx;

    @BeforeEach
    void setUp() {
        handler = new LiteralHandler();
        compiler = new ExpressionCompiler();
        ctx = ExpressionContext.of(null, ExpressionCompilerTest.createTestScope());
    }

    // =========================================================================
    // Int literals
    // =========================================================================

    @Test
    void handle_int_literal_small() {
        var expr = new RIntLiteral();
        expr.setValue(42);
        var result = handler.handle(expr, ctx, compiler);
        String rendered = renderToString(result);
        assertEquals("MapperS.of(42)", rendered);
    }

    @Test
    void handle_int_literal_zero() {
        var expr = new RIntLiteral();
        expr.setValue(0);
        var result = handler.handle(expr, ctx, compiler);
        assertEquals("MapperS.of(0)", renderToString(result));
    }

    @Test
    void handle_int_literal_large_long() {
        var expr = new RIntLiteral();
        expr.setValue(Long.MAX_VALUE);
        var result = handler.handle(expr, ctx, compiler);
        // v3.2 seat 13 (D53, the M5c heal - oracle group conv-bigint-alias-long): the long band's suffix is the
        // LOWERCASE `l` on both routes (LiteralHandler.intLiteralCode, the ONE render); the uppercase pin had no golden.
        assertEquals("MapperS.of(9223372036854775807l)", renderToString(result));
    }

    @Test
    void handle_int_literal_very_large_biginteger() {
        var expr = new RIntLiteral();
        expr.setValue(new BigInteger("99999999999999999999999999999"));
        var result = handler.handle(expr, ctx, compiler);
        assertEquals("MapperS.of(new BigInteger(\"99999999999999999999999999999\"))", renderToString(result));
    }

    // =========================================================================
    // Int literals — BigDecimal context
    // =========================================================================

    @Test
    void handle_int_literal_bigdecimal_context() {
        // When expected type is BigDecimal, int literal should be wrapped
        var typeUtil = new com.regnosys.rosetta.generator.java.types.JavaTypeUtil();
        var bigDecimalCompiler = new ExpressionCompiler(null, null, typeUtil);
        var bigDecimalCtx = ExpressionContext.of(typeUtil.BIG_DECIMAL,
                ExpressionCompilerTest.createTestScope());

        var expr = new RIntLiteral();
        expr.setValue(0);
        var result = handler.handle(expr, bigDecimalCtx, bigDecimalCompiler);
        assertEquals("MapperS.of(BigDecimal.valueOf(0))", renderToString(result));
    }

    @Test
    void handle_int_literal_negative_bigdecimal_context() {
        var typeUtil = new com.regnosys.rosetta.generator.java.types.JavaTypeUtil();
        var bigDecimalCompiler = new ExpressionCompiler(null, null, typeUtil);
        var bigDecimalCtx = ExpressionContext.of(typeUtil.BIG_DECIMAL,
                ExpressionCompilerTest.createTestScope());

        var expr = new RIntLiteral();
        expr.setValue(-1);
        var result = handler.handle(expr, bigDecimalCtx, bigDecimalCompiler);
        assertEquals("MapperS.of(BigDecimal.valueOf(-1))", renderToString(result));
    }

    /**
     * Facet long_literal_suffix: a literal beyond int range in a BigDecimal
     * context needs the lowercase {@code l} long suffix (upstream Xtend
     * convention — golden {@code BigDecimal.valueOf(99999999999l)}); the bare
     * form is a non-compiling out-of-range int literal. In-int-range literals
     * stay suffix-free (the green corpus convention, pinned above).
     */
    @Test
    void handle_int_literal_beyond_int_range_bigdecimal_context_gets_long_suffix() {
        var typeUtil = new com.regnosys.rosetta.generator.java.types.JavaTypeUtil();
        var bigDecimalCompiler = new ExpressionCompiler(null, null, typeUtil);
        var bigDecimalCtx = ExpressionContext.of(typeUtil.BIG_DECIMAL,
                ExpressionCompilerTest.createTestScope());

        var expr = new RIntLiteral();
        expr.setValue(new BigInteger("99999999999"));
        var result = handler.handle(expr, bigDecimalCtx, bigDecimalCompiler);
        assertEquals("MapperS.of(BigDecimal.valueOf(99999999999l))", renderToString(result));
    }

    /**
     * Facet long_literal_suffix band boundaries: int-max stays suffix-free,
     * int-max+1 and long-max take the lowercase {@code l}, beyond-long keeps
     * the {@code new BigDecimal(new BigInteger("…"))} constructor form in a
     * SINK-LESS context (facet biginteger_literal_hoist's DECLINE path —
     * rule/report emission, lambda interiors; the sink-marked hoist path is
     * pinned at {@link #handle_int_literal_beyond_long_with_sink_hoists}).
     */
    @Test
    void handle_int_literal_bigdecimal_context_band_boundaries() {
        var typeUtil = new com.regnosys.rosetta.generator.java.types.JavaTypeUtil();
        var bigDecimalCompiler = new ExpressionCompiler(null, null, typeUtil);
        var bigDecimalCtx = ExpressionContext.of(typeUtil.BIG_DECIMAL,
                ExpressionCompilerTest.createTestScope());

        var intMax = new RIntLiteral();
        intMax.setValue(new BigInteger("2147483647"));
        assertEquals("MapperS.of(BigDecimal.valueOf(2147483647))",
                renderToString(handler.handle(intMax, bigDecimalCtx, bigDecimalCompiler)));

        var beyondInt = new RIntLiteral();
        beyondInt.setValue(new BigInteger("2147483648"));
        assertEquals("MapperS.of(BigDecimal.valueOf(2147483648l))",
                renderToString(handler.handle(beyondInt, bigDecimalCtx, bigDecimalCompiler)));

        var longMax = new RIntLiteral();
        longMax.setValue(new BigInteger("9223372036854775807"));
        assertEquals("MapperS.of(BigDecimal.valueOf(9223372036854775807l))",
                renderToString(handler.handle(longMax, bigDecimalCtx, bigDecimalCompiler)));

        var beyondLong = new RIntLiteral();
        beyondLong.setValue(new BigInteger("9223372036854775808"));
        assertEquals("MapperS.of(new BigDecimal(new BigInteger(\"9223372036854775808\")))",
                renderToString(handler.handle(beyondLong, bigDecimalCtx, bigDecimalCompiler)));
    }

    /**
     * Facet biginteger_literal_hoist: with a statement-hoist sink reachable, a
     * beyond-long literal in BigDecimal context registers the
     * {@code final BigInteger … = new BigInteger("…");} hoist block on the
     * sink (bigInteger name group — bare for this singleton) and renders the
     * null-guarded Mapper ternary in place of the inline constructor form.
     */
    @Test
    void handle_int_literal_beyond_long_with_sink_hoists() {
        var typeUtil = new com.regnosys.rosetta.generator.java.types.JavaTypeUtil();
        var bigDecimalCompiler = new ExpressionCompiler(null, null, typeUtil);
        var scope = ExpressionCompilerTest.createTestScope();
        var session = new com.regnosys.rosetta.generator.java.function.StatementHoistSession();
        scope.markStatementHoistSink(session);
        var bigDecimalCtx = ExpressionContext.of(typeUtil.BIG_DECIMAL, scope);

        var expr = new RIntLiteral();
        expr.setValue(new BigInteger("9999999999999999999999999"));
        var result = handler.handle(expr, bigDecimalCtx, bigDecimalCompiler);
        assertEquals(
                "(bigInteger == null ? MapperS.<BigDecimal>ofNull() : MapperS.of(new BigDecimal(bigInteger)))",
                session.resolve(renderToString(result)),
                "the consumption renders the null-guarded Mapper ternary");

        var hoists = scope.drainStatementHoistsSince(0);
        assertEquals(1, hoists.size(), "exactly one hoist block registers on the sink");
        assertEquals(
                "final BigInteger bigInteger = new BigInteger(\"9999999999999999999999999\");",
                session.resolve(hoists.get(0)),
                "the hoist declares the literal as a final BigInteger local");

        // Refs-carrying invariant (matches this file's refs section for every
        // other literal path): the ternary expression carries the imports for
        // BOTH the hoist declaration and its own consumption form.
        assertTrue(result.getRefs().contains(HandlerHelper.BIG_INTEGER),
                "hoist path must carry the BigInteger ref");
        assertTrue(result.getRefs().contains(HandlerHelper.BIG_DECIMAL),
                "hoist path must carry the BigDecimal ref");
        assertTrue(result.getRefs().contains(HandlerHelper.MAPPER_S),
                "hoist path must carry the MapperS ref");
    }

    @Test
    void handle_int_literal_null_expected_type_stays_bare() {
        // When expected type is null, int literal stays bare (backward compat)
        var expr = new RIntLiteral();
        expr.setValue(42);
        var result = handler.handle(expr, ctx, compiler);
        assertEquals("MapperS.of(42)", renderToString(result));
    }

    @Test
    void handle_int_literal_integer_context_stays_bare() {
        // When expected type is Integer, int literal stays bare
        var typeUtil = new com.regnosys.rosetta.generator.java.types.JavaTypeUtil();
        var typedCompiler = new ExpressionCompiler(null, null, typeUtil);
        var intCtx = ExpressionContext.of(typeUtil.INTEGER,
                ExpressionCompilerTest.createTestScope());

        var expr = new RIntLiteral();
        expr.setValue(1);
        var result = handler.handle(expr, intCtx, typedCompiler);
        assertEquals("MapperS.of(1)", renderToString(result));
    }

    // =========================================================================
    // Number literals
    // =========================================================================

    @Test
    void handle_number_literal() {
        var expr = new RNumberLiteral();
        expr.setValue(new BigDecimal("3.14"));
        var result = handler.handle(expr, ctx, compiler);
        assertEquals("MapperS.of(new BigDecimal(\"3.14\"))", renderToString(result));
    }

    @Test
    void handle_number_literal_integer_value() {
        var expr = new RNumberLiteral();
        expr.setValue(new BigDecimal("100.00"));
        var result = handler.handle(expr, ctx, compiler);
        assertEquals("MapperS.of(new BigDecimal(\"100.00\"))", renderToString(result));
    }

    // =========================================================================
    // String literals
    // =========================================================================

    @Test
    void handle_string_literal() {
        var expr = new RStringLiteral();
        expr.setValue("hello");
        var result = handler.handle(expr, ctx, compiler);
        assertEquals("MapperS.of(\"hello\")", renderToString(result));
    }

    @Test
    void handle_string_literal_with_quotes() {
        var expr = new RStringLiteral();
        expr.setValue("say \"hi\"");
        var result = handler.handle(expr, ctx, compiler);
        assertEquals("MapperS.of(\"say \\\"hi\\\"\")", renderToString(result));
    }

    // =========================================================================
    // Boolean literals
    // =========================================================================

    @Test
    void handle_boolean_literal_true() {
        var expr = new RBooleanLiteral();
        expr.setValue(true);
        var result = handler.handle(expr, ctx, compiler);
        assertEquals("MapperS.of(true)", renderToString(result));
    }

    @Test
    void handle_boolean_literal_false() {
        var expr = new RBooleanLiteral();
        expr.setValue(false);
        var result = handler.handle(expr, ctx, compiler);
        assertEquals("MapperS.of(false)", renderToString(result));
    }

    // =========================================================================
    // Empty literal
    // =========================================================================

    @Test
    void handle_empty_literal() {
        var expr = new REmptyLiteral();
        var result = handler.handle(expr, ctx, compiler);
        assertSame(JavaLiteral.NULL, result);
        assertEquals("null", renderToString(result));
    }

    // =========================================================================
    // List literals
    // =========================================================================

    @Test
    void handle_list_literal_two_ints() {
        var expr = new RListLiteral();
        var elem1 = new RIntLiteral();
        elem1.setValue(1);
        var elem2 = new RIntLiteral();
        elem2.setValue(2);
        expr.elements().add(elem1);
        expr.elements().add(elem2);
        var result = handler.handle(expr, ctx, compiler);
        assertEquals("MapperC.of(MapperS.of(1), MapperS.of(2))", renderToString(result));
    }

    @Test
    void handle_list_literal_single_element() {
        var expr = new RListLiteral();
        var elem = new RIntLiteral();
        elem.setValue(99);
        expr.elements().add(elem);
        var result = handler.handle(expr, ctx, compiler);
        assertEquals("MapperC.of(MapperS.of(99))", renderToString(result));
    }

    // =========================================================================
    // List literals — MapperC list-literal witness + enum-element wrap
    // (mirrors upstream ExpressionGenerator.caseListLiteral)
    // =========================================================================

    /**
     * An empty list literal keeps the bare {@code MapperC.of()} form (no witness):
     * the inferred item type is {@code NOTHING} (which the translator maps to Void),
     * so {@code listItemJavaType} returns null — unchanged from before the witness.
     */
    @Test
    void handle_list_literal_empty_stays_bare() {
        var expr = new RListLiteral();
        var result = handler.handle(expr, ctx, compiler);
        assertEquals("MapperC.of()", renderToString(result));
    }

    /**
     * A bare {@link REnumValueRef} element (the {@code EnumName -> valueName} form,
     * e.g. {@code [ActionTypeEnum -> NEWT]}) renders the qualified constant
     * {@code ActionTypeEnum.NEWT} unwrapped (null expression type → coercion never
     * fires). Golden renders every list element as a {@code Mapper}, so the list
     * literal handler wraps it in {@code MapperS.of(...)}. The {@code <Item>}
     * witness is model-dependent (read from the inference engine) and is exercised
     * end-to-end by the D11 corpus byte-oracle; with the test's model-less compiler
     * the bare {@code MapperC.of(...)} form is emitted (no witness), so the
     * wrap is asserted in isolation here.
     */
    @Test
    void handle_list_literal_wraps_bare_enum_value_in_mapper_s() {
        var expr = new RListLiteral();
        expr.elements().add(resolvedEnumRef("ActionTypeEnum", "NEWT"));
        expr.elements().add(resolvedEnumRef("ActionTypeEnum", "MODI"));
        var result = handler.handle(expr, ctx, compiler);
        assertEquals(
                "MapperC.of(MapperS.of(ActionTypeEnum.NEWT), MapperS.of(ActionTypeEnum.MODI))",
                renderToString(result));
    }

    /**
     * A list element that is already a {@code Mapper} (e.g. a self-wrapping string
     * literal) is NOT double-wrapped — only bare {@link REnumValueRef}s are wrapped.
     */
    @Test
    void handle_list_literal_does_not_double_wrap_self_wrapping_literals() {
        var expr = new RListLiteral();
        var s1 = new RStringLiteral();
        s1.setValue("BE");
        var s2 = new RStringLiteral();
        s2.setValue("BG");
        expr.elements().add(s1);
        expr.elements().add(s2);
        var result = handler.handle(expr, ctx, compiler);
        assertEquals("MapperC.of(MapperS.of(\"BE\"), MapperS.of(\"BG\"))", renderToString(result));
    }

    /**
     * The witness-aware {@code MapperC.of} factory renders {@code MapperC.<Item>of(...)}
     * and carries the witness class as a ref (so its import is collected) — the
     * model-dependent path the list-literal handler drives once an item type is
     * inferred. A null witness renders the bare {@code MapperC.of(...)} form
     * (cardinality-coercion compatible).
     */
    @Test
    void mapperC_factory_emits_generic_witness_and_carries_ref() {
        List<JavaExpression> inners = List.of(
                JavaExpression.from("MapperS.of(BigDecimal.valueOf(1))", null),
                JavaExpression.from("MapperS.of(BigDecimal.valueOf(2))", null));

        var withWitness = JavaExpression.wrappedInMapperCOf(inners, null, HandlerHelper.BIG_DECIMAL);
        assertEquals("MapperC.<BigDecimal>of(MapperS.of(BigDecimal.valueOf(1)), MapperS.of(BigDecimal.valueOf(2)))",
                withWitness.renderToString());
        assertTrue(withWitness.getRefs().contains(HandlerHelper.MAPPER_C),
                "witness factory must carry MAPPER_C ref");
        assertTrue(withWitness.getRefs().contains(HandlerHelper.BIG_DECIMAL),
                "witness factory must carry the witness class ref for import collection");

        var bare = JavaExpression.wrappedInMapperCOf(inners, null, null);
        assertEquals("MapperC.of(MapperS.of(BigDecimal.valueOf(1)), MapperS.of(BigDecimal.valueOf(2)))",
                bare.renderToString());
    }

    /**
     * Build a resolved real-enum reference (the {@code EnumName -> valueName} form).
     * Mirrors {@code ReferenceHandlerTest.resolvedEnumRef}: only when
     * {@code resolvedEnum} is set is the node treated as a real enum reference
     * (otherwise it is a disguised feature call).
     */
    private static REnumValueRef resolvedEnumRef(String enumName, String valueName) {
        var ref = new REnumValueRef();
        ref.setEnumName(enumName);
        ref.setValueName(valueName);
        var mockEnum = new REnumeration();
        mockEnum.setName(enumName);
        ref.setResolvedEnum(mockEnum);
        return ref;
    }

    // =========================================================================
    // PR-A §9.1 C3a.4.e — refs-carrying invariant tests
    // =========================================================================
    //
    // Each literal handler emits via wrappedInMapperSOf / wrappedInMapperCOf
    // factories post-C3a.4.e (landed this PR); these factories structurally
    // carry MAPPER_S / MAPPER_C + inner refs. These tests pin the refs
    // contract as a regression guard: a future edit that reverts any
    // emission to the 2-arg JavaExpression.from overload, or removes a
    // HandlerHelper ref from a wrap-factory call site, will be caught
    // here before it silently drops imports post-C3c.2 (where the
    // structured channel is the sole feeder of ImportCollector).

    @Test
    void int_literal_default_carries_mapper_s_ref() {
        var expr = new RIntLiteral();
        expr.setValue(42);
        var result = handler.handle(expr, ctx, compiler);
        Set<?> refs = result.getRefs();
        assertTrue(refs.contains(HandlerHelper.MAPPER_S),
                "Int literal default path (L79) emits MapperS.of(42) — "
                        + "must carry MAPPER_S ref structurally");
    }

    @Test
    void int_literal_bigger_than_long_carries_big_integer_ref() {
        // Values that don't fit in long (bitLength > 63) trigger the
        // `new BigInteger("...")` constructor path at L79 default branch.
        // That inline `new BigInteger(...)` textually references the class,
        // so BIG_INTEGER must be carried as a ref.
        var expr = new RIntLiteral();
        // 2^64 + 1 — fits in no primitive, requires BigInteger
        expr.setValue(new BigInteger("18446744073709551617"));
        var result = handler.handle(expr, ctx, compiler);
        Set<?> refs = result.getRefs();
        assertTrue(refs.contains(HandlerHelper.MAPPER_S),
                "Int literal bigger than long must carry MAPPER_S ref");
        assertTrue(refs.contains(HandlerHelper.BIG_INTEGER),
                "Int literal bigger than long emits `new BigInteger(\"...\")` — "
                        + "must carry BIG_INTEGER ref");
    }

    @Test
    void number_literal_carries_mapper_s_and_big_decimal_refs() {
        var expr = new RNumberLiteral();
        expr.setValue(new BigDecimal("3.14"));
        var result = handler.handle(expr, ctx, compiler);
        Set<?> refs = result.getRefs();
        assertTrue(refs.contains(HandlerHelper.MAPPER_S),
                "Number literal emission (L140) wraps in MapperS.of(...) — "
                        + "must carry MAPPER_S ref");
        assertTrue(refs.contains(HandlerHelper.BIG_DECIMAL),
                "Number literal emission (L140) inlines `new BigDecimal(\"...\")` — "
                        + "must carry BIG_DECIMAL ref");
    }

    @Test
    void string_literal_carries_mapper_s_ref() {
        var expr = new RStringLiteral();
        expr.setValue("hello");
        var result = handler.handle(expr, ctx, compiler);
        Set<?> refs = result.getRefs();
        assertTrue(refs.contains(HandlerHelper.MAPPER_S),
                "String literal emission (L153) must carry MAPPER_S ref");
    }

    @Test
    void boolean_literal_carries_mapper_s_ref() {
        var expr = new RBooleanLiteral();
        expr.setValue(true);
        var result = handler.handle(expr, ctx, compiler);
        Set<?> refs = result.getRefs();
        assertTrue(refs.contains(HandlerHelper.MAPPER_S),
                "Boolean literal emission (L175) must carry MAPPER_S ref");
    }

    @Test
    void list_literal_carries_mapper_c_and_inner_refs() {
        var expr = new RListLiteral();
        var elem1 = new RIntLiteral();
        elem1.setValue(1);
        var elem2 = new RIntLiteral();
        elem2.setValue(2);
        expr.elements().add(elem1);
        expr.elements().add(elem2);

        var result = handler.handle(expr, ctx, compiler);
        Set<?> refs = result.getRefs();
        assertTrue(refs.contains(HandlerHelper.MAPPER_C),
                "List literal emission (L213) must carry MAPPER_C ref");
        assertTrue(refs.contains(HandlerHelper.MAPPER_S),
                "Each int element compiles via `MapperS.of(N)` — inner "
                        + "MAPPER_S refs must union into the wrap factory's "
                        + "combined ref set");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String renderToString(JavaStatementBuilder result) {
        assertInstanceOf(JavaExpression.class, result);
        return ((JavaExpression) result).renderToString();
    }
}
