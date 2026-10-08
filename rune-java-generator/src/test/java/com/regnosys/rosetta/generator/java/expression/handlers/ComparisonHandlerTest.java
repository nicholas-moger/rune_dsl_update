package com.regnosys.rosetta.generator.java.expression.handlers;

import com.regnosys.rosetta.ast.enums.CardMod;
import com.regnosys.rosetta.ast.enums.CompOp;
import com.regnosys.rosetta.ast.enums.EqOp;
import com.regnosys.rosetta.ast.expressions.binary.RComparisonExpr;
import com.regnosys.rosetta.ast.expressions.binary.REqualityExpr;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompilerTest;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ComparisonHandlerTest {

    private ComparisonHandler handler;
    private ExpressionCompiler compiler;
    private ExpressionContext ctx;

    @BeforeEach
    void setUp() {
        handler  = new ComparisonHandler();
        compiler = new ExpressionCompiler();
        ctx      = ExpressionContext.of(null, ExpressionCompilerTest.createTestScope());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private RIntLiteral intLiteral(int value) {
        var lit = new RIntLiteral();
        lit.setValue(value);
        return lit;
    }

    /**
     * Build a fully-resolved enum-value reference that renders as the bare
     * dotted enum constant {@code EnumType.VALUE} (the ReferenceHandler
     * enumeration-present path). Mirrors {@code ReferenceHandlerTest.resolvedEnumRef}.
     */
    private REnumValueRef resolvedEnumRef(String enumName, String valueName) {
        var ref = new REnumValueRef();
        ref.setEnumName(enumName);
        ref.setValueName(valueName);
        var mockEnum = new REnumeration();
        mockEnum.setName(enumName);
        ref.setResolvedEnum(mockEnum);
        return ref;
    }

    private String render(JavaStatementBuilder result) {
        assertInstanceOf(JavaExpression.class, result);
        return ((JavaExpression) result).renderToString();
    }

    // =========================================================================
    // Equality — EQ, NEQ
    // =========================================================================

    @Test
    void equality_eq_default_all() {
        var expr = new REqualityExpr();
        expr.setLeft(intLiteral(1));
        expr.setRight(intLiteral(2));
        expr.setOp(EqOp.EQ);

        assertEquals(
            "areEqual(MapperS.of(1), MapperS.of(2), CardinalityOperator.All)",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void equality_neq_default_any() {
        // <> with no explicit modifier defaults to Any (operator-dependent default,
        // mirroring upstream ExpressionGenerator: defaultModifier = op == "<>" ? ANY : ALL).
        var expr = new REqualityExpr();
        expr.setLeft(intLiteral(3));
        expr.setRight(intLiteral(4));
        expr.setOp(EqOp.NEQ);

        assertEquals(
            "notEqual(MapperS.of(3), MapperS.of(4), CardinalityOperator.Any)",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void equality_neq_all_mod_overrides_default() {
        // An explicit `all` modifier overrides the <> Any default.
        var expr = new REqualityExpr();
        expr.setLeft(intLiteral(3));
        expr.setRight(intLiteral(4));
        expr.setOp(EqOp.NEQ);
        expr.setMod(CardMod.ALL);

        assertEquals(
            "notEqual(MapperS.of(3), MapperS.of(4), CardinalityOperator.All)",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void equality_eq_any_mod() {
        var expr = new REqualityExpr();
        expr.setLeft(intLiteral(5));
        expr.setRight(intLiteral(6));
        expr.setOp(EqOp.EQ);
        expr.setMod(CardMod.ANY);

        assertEquals(
            "areEqual(MapperS.of(5), MapperS.of(6), CardinalityOperator.Any)",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // Comparison — LT, GT, LTE, GTE
    // =========================================================================

    @Test
    void comparison_lt() {
        var expr = new RComparisonExpr();
        expr.setLeft(intLiteral(1));
        expr.setRight(intLiteral(2));
        expr.setOp(CompOp.LT);

        assertEquals(
            "lessThan(MapperS.of(1), MapperS.of(2), CardinalityOperator.All)",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void comparison_gt() {
        var expr = new RComparisonExpr();
        expr.setLeft(intLiteral(10));
        expr.setRight(intLiteral(5));
        expr.setOp(CompOp.GT);

        assertEquals(
            "greaterThan(MapperS.of(10), MapperS.of(5), CardinalityOperator.All)",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void comparison_lte() {
        var expr = new RComparisonExpr();
        expr.setLeft(intLiteral(3));
        expr.setRight(intLiteral(3));
        expr.setOp(CompOp.LTE);

        assertEquals(
            "lessThanEquals(MapperS.of(3), MapperS.of(3), CardinalityOperator.All)",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void comparison_gte() {
        var expr = new RComparisonExpr();
        expr.setLeft(intLiteral(7));
        expr.setRight(intLiteral(4));
        expr.setOp(CompOp.GTE);

        assertEquals(
            "greaterThanEquals(MapperS.of(7), MapperS.of(4), CardinalityOperator.All)",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void comparison_lt_any_mod() {
        var expr = new RComparisonExpr();
        expr.setLeft(intLiteral(1));
        expr.setRight(intLiteral(2));
        expr.setOp(CompOp.LT);
        expr.setMod(CardMod.ANY);

        assertEquals(
            "lessThan(MapperS.of(1), MapperS.of(2), CardinalityOperator.Any)",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // Dispatch via ExpressionCompiler
    // =========================================================================

    @Test
    void compiler_dispatches_equality() {
        var expr = new REqualityExpr();
        expr.setLeft(intLiteral(9));
        expr.setRight(intLiteral(0));
        expr.setOp(EqOp.EQ);

        var scope  = ExpressionCompilerTest.createTestScope();
        var result = compiler.compile(expr, null, scope);

        assertInstanceOf(JavaExpression.class, result);
        assertEquals(
            "areEqual(MapperS.of(9), MapperS.of(0), CardinalityOperator.All)",
            ((JavaExpression) result).renderToString());
    }

    @Test
    void compiler_dispatches_comparison() {
        var expr = new RComparisonExpr();
        expr.setLeft(intLiteral(2));
        expr.setRight(intLiteral(8));
        expr.setOp(CompOp.GT);

        var scope  = ExpressionCompilerTest.createTestScope();
        var result = compiler.compile(expr, null, scope);

        assertInstanceOf(JavaExpression.class, result);
        assertEquals(
            "greaterThan(MapperS.of(2), MapperS.of(8), CardinalityOperator.All)",
            ((JavaExpression) result).renderToString());
    }

    // =========================================================================
    // PR-A §9.1 C3a.4.b — refs-carrying invariant tests
    // =========================================================================
    //
    // These pin the structured refs + staticWildcardImports contract for
    // ComparisonHandler emissions post-C3a.4.b (landed this PR). Both
    // emission sites (L65 equality, L94 relational) surface
    // refs={CARDINALITY_OPERATOR} + staticWildcardImports={
    // EXPRESSION_OPERATORS_NULL_SAFE} via the structured channel.
    // Regression guard: any future edit that reverts a 4-arg
    // JavaExpression.from to the 2-arg overload, or removes
    // CARDINALITY_OPERATOR / EXPRESSION_OPERATORS_NULL_SAFE from a
    // ref set, will be caught here before it silently drops the
    // import post-C3c.2 (where the structured channel is the sole
    // feeder of ImportCollector).

    @Test
    void equality_emission_carries_cardinality_operator_ref() {
        var expr = new REqualityExpr();
        expr.setLeft(intLiteral(1));
        expr.setRight(intLiteral(2));
        expr.setOp(EqOp.EQ);

        var result = handler.handle(expr, ctx, compiler);

        Set<?> refs = result.getRefs();
        assertTrue(refs.contains(HandlerHelper.CARDINALITY_OPERATOR),
                "L65 equality emission must carry CARDINALITY_OPERATOR ref "
                        + "(source textually contains CardinalityOperator.All)");
    }

    @Test
    void equality_emission_carries_expression_operators_null_safe_wildcard() {
        var expr = new REqualityExpr();
        expr.setLeft(intLiteral(1));
        expr.setRight(intLiteral(2));
        expr.setOp(EqOp.EQ);

        var result = handler.handle(expr, ctx, compiler);

        Set<?> wildcards = result.getStaticWildcardImports();
        assertTrue(wildcards.contains(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE),
                "L65 equality emission must carry EXPRESSION_OPERATORS_NULL_SAFE "
                        + "staticWildcardImport (source uses the static areEqual / "
                        + "notEqual methods from that class)");
    }

    @Test
    void comparison_emission_carries_cardinality_operator_ref() {
        var expr = new RComparisonExpr();
        expr.setLeft(intLiteral(5));
        expr.setRight(intLiteral(10));
        expr.setOp(CompOp.LT);

        var result = handler.handle(expr, ctx, compiler);

        Set<?> refs = result.getRefs();
        assertTrue(refs.contains(HandlerHelper.CARDINALITY_OPERATOR),
                "L94 comparison emission must carry CARDINALITY_OPERATOR ref");
    }

    @Test
    void comparison_emission_carries_expression_operators_null_safe_wildcard() {
        var expr = new RComparisonExpr();
        expr.setLeft(intLiteral(5));
        expr.setRight(intLiteral(10));
        expr.setOp(CompOp.LT);

        var result = handler.handle(expr, ctx, compiler);

        Set<?> wildcards = result.getStaticWildcardImports();
        assertTrue(wildcards.contains(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE),
                "L94 comparison emission must carry EXPRESSION_OPERATORS_NULL_SAFE "
                        + "staticWildcardImport (source uses the static lessThan / "
                        + "greaterThan / lessThanEquals / greaterThanEquals methods)");
    }

    // =========================================================================
    // Phase X1 Gap #3 — bare enum-constant operands are wrapped in MapperS.of
    // =========================================================================
    //
    // The ReferenceHandler renders an enum value reference as the BARE dotted
    // constant {@code EnumType.VALUE} (relying on the enclosing context to apply
    // any Mapper wrap — see ReferenceHandler L132-134 / L450-452). In a
    // comparison the operand must be a Mapper, so the bare enum constant is
    // wrapped: {@code MapperS.of(EnumType.VALUE)}. This matches the
    // DeliveryTypeFromSettlementRule golden:
    //   areEqual(MapperS.of(input).<...>map(...), MapperS.of(SettlementTypeEnum.CASH), ...)
    // A Mapper-chain operand (e.g. a navigation, a literal already wrapped by
    // LiteralHandler, a variable wrapped by ReferenceHandler) is NOT re-wrapped.

    @Test
    void equality_bareEnumOperand_wrappedInMapperSOf() {
        var expr = new REqualityExpr();
        expr.setLeft(intLiteral(1));
        expr.setRight(resolvedEnumRef("SettlementTypeEnum", "CASH"));
        expr.setOp(EqOp.EQ);

        assertEquals(
            "areEqual(MapperS.of(1), MapperS.of(SettlementTypeEnum.CASH), CardinalityOperator.All)",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void equality_bothEnumOperands_eachWrappedInMapperSOf() {
        var expr = new REqualityExpr();
        expr.setLeft(resolvedEnumRef("SettlementTypeEnum", "CASH"));
        expr.setRight(resolvedEnumRef("SettlementTypeEnum", "PHYSICAL"));
        expr.setOp(EqOp.EQ);

        assertEquals(
            "areEqual(MapperS.of(SettlementTypeEnum.CASH), MapperS.of(SettlementTypeEnum.PHYSICAL), CardinalityOperator.All)",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void comparison_bareEnumOperand_wrappedInMapperSOf() {
        var expr = new RComparisonExpr();
        expr.setLeft(intLiteral(1));
        expr.setRight(resolvedEnumRef("FooEnum", "BAR"));
        expr.setOp(CompOp.LT);

        assertEquals(
            "lessThan(MapperS.of(1), MapperS.of(FooEnum.BAR), CardinalityOperator.All)",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void equality_enumOperand_carriesMapperSRef() {
        // Both operands are bare enum constants — neither carries MAPPER_S until
        // the wrap is applied, so MAPPER_S in the result isolates the wrap's
        // contribution (a literal/variable left would carry MAPPER_S anyway).
        var expr = new REqualityExpr();
        expr.setLeft(resolvedEnumRef("SettlementTypeEnum", "CASH"));
        expr.setRight(resolvedEnumRef("SettlementTypeEnum", "PHYSICAL"));
        expr.setOp(EqOp.EQ);

        var result = handler.handle(expr, ctx, compiler);

        assertTrue(result.getRefs().contains(HandlerHelper.MAPPER_S),
                "wrapping a bare enum operand in MapperS.of(...) must contribute "
                        + "the MAPPER_S ref so the import is emitted");
    }
}
