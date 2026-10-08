package com.regnosys.rosetta.generator.java.expression.handlers;

import com.regnosys.rosetta.ast.enums.LogOp;
import com.regnosys.rosetta.ast.expressions.binary.RLogicalExpr;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompilerTest;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogicalHandlerTest {

    private LogicalHandler handler;
    private ExpressionCompiler compiler;
    private ExpressionContext ctx;

    @BeforeEach
    void setUp() {
        handler  = new LogicalHandler();
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

    private String render(JavaStatementBuilder result) {
        assertInstanceOf(JavaExpression.class, result);
        return ((JavaExpression) result).renderToString();
    }

    // =========================================================================
    // AND, OR
    // =========================================================================

    @Test
    void logical_and() {
        var expr = new RLogicalExpr();
        expr.setLeft(intLiteral(1));
        expr.setRight(intLiteral(2));
        expr.setOp(LogOp.AND);

        assertEquals(
            "MapperS.of(1).andNullSafe(MapperS.of(2))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void logical_or() {
        var expr = new RLogicalExpr();
        expr.setLeft(intLiteral(3));
        expr.setRight(intLiteral(4));
        expr.setOp(LogOp.OR);

        assertEquals(
            "MapperS.of(3).orNullSafe(MapperS.of(4))",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // Dispatch via ExpressionCompiler
    // =========================================================================

    @Test
    void compiler_dispatches_and() {
        var expr = new RLogicalExpr();
        expr.setLeft(intLiteral(5));
        expr.setRight(intLiteral(6));
        expr.setOp(LogOp.AND);

        var scope  = ExpressionCompilerTest.createTestScope();
        var result = compiler.compile(expr, null, scope);

        assertInstanceOf(JavaExpression.class, result);
        assertEquals(
            "MapperS.of(5).andNullSafe(MapperS.of(6))",
            ((JavaExpression) result).renderToString());
    }

    @Test
    void compiler_dispatches_or() {
        var expr = new RLogicalExpr();
        expr.setLeft(intLiteral(7));
        expr.setRight(intLiteral(8));
        expr.setOp(LogOp.OR);

        var scope  = ExpressionCompilerTest.createTestScope();
        var result = compiler.compile(expr, null, scope);

        assertInstanceOf(JavaExpression.class, result);
        assertEquals(
            "MapperS.of(7).orNullSafe(MapperS.of(8))",
            ((JavaExpression) result).renderToString());
    }
}
