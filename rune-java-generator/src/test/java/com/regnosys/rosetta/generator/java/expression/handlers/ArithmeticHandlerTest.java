package com.regnosys.rosetta.generator.java.expression.handlers;

import com.regnosys.rosetta.ast.enums.ArithOp;
import com.regnosys.rosetta.ast.expressions.binary.RArithmeticExpr;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompilerTest;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ArithmeticHandlerTest {

    private ArithmeticHandler handler;
    private ExpressionCompiler compiler;
    private ExpressionContext ctx;

    @BeforeEach
    void setUp() {
        handler  = new ArithmeticHandler();
        compiler = new ExpressionCompiler();
        ctx      = ExpressionContext.of(null, ExpressionCompilerTest.createTestScope());
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private RIntLiteral intLiteral(int value) {
        var lit = new RIntLiteral();
        lit.setValue(value);
        return lit;
    }

    private String render(JavaStatementBuilder result) {
        assertInstanceOf(JavaExpression.class, result);
        return ((JavaExpression) result).renderToString();
    }

    // =========================================================================
    // Tests — one per operator
    // =========================================================================

    @Test
    void addition() {
        // facet numeric_literal_typing: int literals COUNT as join evidence
        // (upstream joins an int literal as int), so literal-literal add joins
        // int x int -> Integer witnesses with the literals BARE.
        var expr = new RArithmeticExpr();
        expr.setLeft(intLiteral(1));
        expr.setRight(intLiteral(2));
        expr.setOp(ArithOp.PLUS);

        var result = handler.handle(expr, ctx, compiler);

        assertEquals("MapperMaths.<Integer, Integer, Integer>add(MapperS.of(1), MapperS.of(2))", render(result));
    }

    @Test
    void subtraction() {
        var expr = new RArithmeticExpr();
        expr.setLeft(intLiteral(10));
        expr.setRight(intLiteral(3));
        expr.setOp(ArithOp.MINUS);

        var result = handler.handle(expr, ctx, compiler);

        assertEquals("MapperMaths.<Integer, Integer, Integer>subtract(MapperS.of(10), MapperS.of(3))", render(result));
    }

    @Test
    void multiplication() {
        var expr = new RArithmeticExpr();
        expr.setLeft(intLiteral(4));
        expr.setRight(intLiteral(5));
        expr.setOp(ArithOp.MULTIPLY);

        var result = handler.handle(expr, ctx, compiler);

        assertEquals("MapperMaths.<Integer, Integer, Integer>multiply(MapperS.of(4), MapperS.of(5))", render(result));
    }

    @Test
    void division() {
        // facet numeric_literal_typing: divide's RESULT witness is ALWAYS
        // BigDecimal (upstream caseDivideOperation = unconstrained number)
        // while the operand witnesses keep the int x int join — golden's only
        // mixed combo (YearFractionForOneDay).
        var expr = new RArithmeticExpr();
        expr.setLeft(intLiteral(8));
        expr.setRight(intLiteral(2));
        expr.setOp(ArithOp.DIVIDE);

        var result = handler.handle(expr, ctx, compiler);

        assertEquals("MapperMaths.<BigDecimal, Integer, Integer>divide(MapperS.of(8), MapperS.of(2))", render(result));
    }

    // =========================================================================
    // Dispatch via ExpressionCompiler
    // =========================================================================

    @Test
    void compiler_dispatches_to_arithmetic_handler() {
        var expr = new RArithmeticExpr();
        expr.setLeft(intLiteral(7));
        expr.setRight(intLiteral(3));
        expr.setOp(ArithOp.PLUS);

        var scope  = ExpressionCompilerTest.createTestScope();
        var result = compiler.compile(expr, null, scope);

        assertInstanceOf(JavaExpression.class, result);
        assertEquals("MapperMaths.<Integer, Integer, Integer>add(MapperS.of(7), MapperS.of(3))",
                ((JavaExpression) result).renderToString());
    }

    // =========================================================================
    // PR-A §9.1 C3a.4.a — refs-carrying invariant tests
    // =========================================================================
    //
    // These pin the structured refs contract for ArithmeticHandler
    // emissions post-C3a.4.a (landed this PR). Each test asserts that a
    // specific emission path surfaces the library-class FQNs it textually
    // references (MAPPER_MATHS, MAPPER_S, BIG_DECIMAL) via the
    // JavaStatementBuilder.getRefs() structured channel. Regression
    // guard: any future edit that reverts a 4-arg JavaExpression.from
    // back to the 2-arg overload (or removes a HandlerHelper constant
    // from a ref set) will be caught here before it silently drops the
    // import post-C3c.2 (where the structured channel is the sole
    // feeder of ImportCollector).

    @Test
    void binary_divide_carries_mapper_maths_and_big_decimal() {
        // facet numeric_literal_typing: `8 / 2` joins int x int but divide's
        // RESULT witness is BigDecimal, so typeParams textually contains
        // BigDecimal and the BIG_DECIMAL ref must flow (the
        // YearFractionForOneDay golden keeps its java.math.BigDecimal import
        // for exactly this witness). MAPPER_MATHS emitted unconditionally.
        var expr = new RArithmeticExpr();
        expr.setLeft(intLiteral(8));
        expr.setRight(intLiteral(2));
        expr.setOp(ArithOp.DIVIDE);

        var result = handler.handle(expr, ctx, compiler);

        Set<?> refs = result.getRefs();
        assertTrue(refs.contains(HandlerHelper.MAPPER_MATHS),
                "divide emission must carry MAPPER_MATHS ref");
        assertTrue(refs.contains(HandlerHelper.BIG_DECIMAL),
                "divide emission must carry BIG_DECIMAL ref (the result witness "
                        + "is always BigDecimal, so typeParams textually contains it)");
    }

    @Test
    void binary_int_add_drops_big_decimal_ref() {
        // facet numeric_literal_typing: an all-Integer emission
        // (`<Integer, Integer, Integer>add(MapperS.of(1), MapperS.of(2))`)
        // references no BigDecimal text, so the BIG_DECIMAL ref must NOT flow
        // (corpus law: the Qualify_CashTransfer golden drops the
        // java.math.BigDecimal import when its witnesses go all-Integer).
        var expr = new RArithmeticExpr();
        expr.setLeft(intLiteral(1));
        expr.setRight(intLiteral(2));
        expr.setOp(ArithOp.PLUS);

        var result = handler.handle(expr, ctx, compiler);

        Set<?> refs = result.getRefs();
        assertTrue(refs.contains(HandlerHelper.MAPPER_MATHS),
                "int add emission must carry MAPPER_MATHS ref");
        assertFalse(refs.contains(HandlerHelper.BIG_DECIMAL),
                "all-Integer typeParams must NOT carry the BIG_DECIMAL ref");
    }

    @Test
    void unary_minus_of_bare_int_literal_renders_negated_literal() {
        // facet bareNegLiteral (PR #351): a unary-negated BARE int literal renders as
        // the negated literal itself per the LiteralHandler context law (bare int form
        // when the expected type is Integer or unknown) — NOT the general
        // multiply-by--1 desugar. Golden carriers: AddBusinessDays cdm5/cdm6
        // `MapperS.of(-1)`. (Pre-#351 this shape took the general path and pinned all
        // three library refs; the general path's contract now lives in the
        // non-literal-operand test below.)
        var expr = new RArithmeticExpr();
        expr.setLeft(null);   // unary
        expr.setRight(intLiteral(5));
        expr.setOp(ArithOp.MINUS);

        var result = handler.handle(expr, ctx, compiler);

        assertEquals("MapperS.of(-5)", render(result));
        Set<?> refs = result.getRefs();
        assertTrue(refs.contains(HandlerHelper.MAPPER_S),
                "bareNegLiteral emission inlines MapperS.of(...) — must carry MAPPER_S ref");
        assertFalse(refs.contains(HandlerHelper.MAPPER_MATHS),
                "bareNegLiteral renders NO MapperMaths call — must not carry MAPPER_MATHS ref");
        assertFalse(refs.contains(HandlerHelper.BIG_DECIMAL),
                "bare int form carries no BigDecimal — must not carry BIG_DECIMAL ref");
    }

    @Test
    void unary_minus_general_path_carries_mapper_maths_mapper_s_and_big_decimal() {
        // The general unary-minus desugar (multiply by -1) still owns every
        // NON-bare-literal, non-rewrite operand — e.g. a parenthesised addition.
        // MapperMaths.<...>multiply(MapperS.of(BigDecimal.valueOf(-1)), <inner>):
        // all three library classes are textually present — must be declared as refs.
        var inner = new RArithmeticExpr();
        inner.setLeft(intLiteral(3));
        inner.setRight(intLiteral(4));
        inner.setOp(ArithOp.PLUS);

        var expr = new RArithmeticExpr();
        expr.setLeft(null);   // unary
        expr.setRight(inner);
        expr.setOp(ArithOp.MINUS);

        var result = handler.handle(expr, ctx, compiler);

        String text = render(result);
        assertTrue(text.contains("multiply(MapperS.of(BigDecimal.valueOf(-1)), "),
                "general unary minus keeps the multiply-by--1 desugar: " + text);
        Set<?> refs = result.getRefs();
        assertTrue(refs.contains(HandlerHelper.MAPPER_MATHS),
                "Unary-minus general-path emission must carry MAPPER_MATHS ref");
        assertTrue(refs.contains(HandlerHelper.MAPPER_S),
                "Unary-minus general-path emission inlines MapperS.of(...) — must carry MAPPER_S ref");
        assertTrue(refs.contains(HandlerHelper.BIG_DECIMAL),
                "Unary-minus general-path emission inlines BigDecimal.valueOf(-1) — must carry BIG_DECIMAL ref");
    }

    /**
     * v3.1 C2d family 1 ({@code numeric-literal-kind}): the {@code BIG_DECIMAL} ref is keyed on the
     * boolean witness recorded beside each {@code typeParams} assignment, no longer on the witness
     * text containing {@code "BigDecimal"}. The negated-literal rewrite's DIVIDE arm is the one
     * int-arithmetic branch whose witness names BigDecimal ({@code <BigDecimal, Integer, Integer>}
     * — divide's result is always BigDecimal): it must carry the ref while its MULTIPLY twin
     * (the test below) must not. Together with the binary-site witnesses above these pin every
     * branch; each was proven able to fail under a flipped-witness mutant.
     */
    @Test
    void unary_minus_rewrite_of_int_divide_carries_big_decimal() {
        var inner = new RArithmeticExpr();
        inner.setLeft(intLiteral(8));
        inner.setRight(intLiteral(2));
        inner.setOp(ArithOp.DIVIDE);
        var expr = new RArithmeticExpr();
        expr.setLeft(null);
        expr.setRight(inner);
        expr.setOp(ArithOp.MINUS);
        var result = handler.handle(expr, ctx, compiler);
        String text = render(result);
        assertTrue(text.contains("MapperMaths.<BigDecimal, Integer, Integer>divide(MapperS.of(-8), "),
                "the int-arithmetic DIVIDE rewrite renders the BigDecimal result witness: " + text);
        Set<?> refs = result.getRefs();
        assertTrue(refs.contains(HandlerHelper.MAPPER_MATHS), "must carry MAPPER_MATHS ref");
        assertTrue(refs.contains(HandlerHelper.BIG_DECIMAL),
                "the DIVIDE result witness names BigDecimal - must carry BIG_DECIMAL ref");
    }

    @Test
    void unary_minus_rewrite_of_int_operands_renders_integer_no_big_decimal() {
        // facet negLiteral (PR #206): unary minus of (intLit * intLit) rewrites as
        // (-intLit * intLit) and — both operands resolving INT (literals count as int
        // evidence, mirroring the binary numeric_literal_typing path) — renders the
        // Integer form `MapperMaths.<Integer, Integer, Integer>multiply(MapperS.of(-3),
        // MapperS.of(4))` with a BARE negated literal and NO java.math.BigDecimal ref.
        // (Pre-#206 this rewrite hard-coded the BigDecimal form; a `number` or unresolved
        // operand still DECLINES to it byte-verbatim — corpus-verified by the
        // GenerateObservationPeriod flip + the general-unary-minus test above, which keeps
        // the BigDecimal path for the non-literal `-(3 + 4)` shape. The bare `-5` shape
        // moved to the #351 bareNegLiteral arm — see its test above.)
        var inner = new RArithmeticExpr();
        inner.setLeft(intLiteral(3));
        inner.setRight(intLiteral(4));
        inner.setOp(ArithOp.MULTIPLY);

        var expr = new RArithmeticExpr();
        expr.setLeft(null);
        expr.setRight(inner);
        expr.setOp(ArithOp.MINUS);

        var result = handler.handle(expr, ctx, compiler);
        String text = ((JavaExpression) result).renderToString();
        Set<?> refs = result.getRefs();

        assertTrue(refs.contains(HandlerHelper.MAPPER_MATHS),
                "Unary-minus-rewrite emission must carry MAPPER_MATHS ref");
        assertTrue(refs.contains(HandlerHelper.MAPPER_S),
                "Unary-minus-rewrite inlines MapperS.of(...) — must carry MAPPER_S ref");
        assertFalse(refs.contains(HandlerHelper.BIG_DECIMAL),
                "facet negLiteral: INT*INT renders the Integer form — must NOT carry BIG_DECIMAL ref: " + text);
        assertTrue(text.contains("<Integer, Integer, Integer>multiply"),
                "INT*INT renders the Integer multiply witness: " + text);
        assertTrue(text.contains("MapperS.of(-3)"),
                "the negated int literal renders BARE, not BigDecimal.valueOf(-3): " + text);
    }

    // Note: no test asserts absence of trackType calls — the entire
    // tracker API (seedTrackedTypes/trackType/drainTrackedTypes) was
    // removed from ExpressionCompiler at C3c.3, so such an assertion
    // would be redundant with the API's non-existence. The 3 refs-
    // assertion tests above are the primary contract — they fail-fast
    // if a future edit reverts the 4-arg JavaExpression.from emissions
    // to the 2-arg overload or drops any of MAPPER_MATHS / MAPPER_S /
    // BIG_DECIMAL from the ref sets.
}
