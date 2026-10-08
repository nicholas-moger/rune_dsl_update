package com.regnosys.rosetta.generator.java.expression.handlers;

import com.regnosys.rosetta.ast.expressions.binary.RContainsExpr;
import com.regnosys.rosetta.ast.expressions.binary.RDefaultExpr;
import com.regnosys.rosetta.ast.expressions.binary.RDisjointExpr;
import com.regnosys.rosetta.ast.expressions.binary.RJoinExpr;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompilerTest;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SetOperationHandlerTest {

    private SetOperationHandler handler;
    private ExpressionCompiler compiler;
    private ExpressionContext ctx;

    @BeforeEach
    void setUp() {
        handler  = new SetOperationHandler();
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
    // contains — static call
    // =========================================================================

    @Test
    void contains_static_call() {
        var expr = new RContainsExpr();
        expr.setLeft(intLiteral(1));
        expr.setRight(intLiteral(2));

        assertEquals(
            "contains(MapperS.of(1), MapperS.of(2))",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // disjoint — static call
    // =========================================================================

    @Test
    void disjoint_static_call() {
        var expr = new RDisjointExpr();
        expr.setLeft(intLiteral(3));
        expr.setRight(intLiteral(4));

        assertEquals(
            "disjoint(MapperS.of(3), MapperS.of(4))",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // default — instance call on left
    // =========================================================================

    @Test
    void default_instance_call() {
        var expr = new RDefaultExpr();
        expr.setLeft(intLiteral(5));
        expr.setRight(intLiteral(0));

        // facet defaultForm (PR #218): a SCALAR-LITERAL default value is the plain-T
        // getOrDefault overload — the arg renders BARE (`0`, not `MapperS.of(0)`) and the
        // whole result wraps in MapperS.of (golden `MapperS.of(<X>.getOrDefault(false))`).
        assertEquals(
            "MapperS.of(MapperS.of(5).getOrDefault(0))",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // join — instance call on left
    // =========================================================================

    @Test
    void join_with_separator_instance_call() {
        var expr = new RJoinExpr();
        expr.setLeft(intLiteral(6));
        expr.setSeparator(intLiteral(7));

        assertEquals(
            "MapperS.of(6).join(MapperS.of(7))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void join_without_separator_instance_call() {
        var expr = new RJoinExpr();
        expr.setLeft(intLiteral(6));
        // no separator set

        assertEquals(
            "MapperS.of(6).join()",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // Dispatch via ExpressionCompiler
    // =========================================================================

    @Test
    void compiler_dispatches_contains() {
        var expr = new RContainsExpr();
        expr.setLeft(intLiteral(10));
        expr.setRight(intLiteral(20));

        var scope  = ExpressionCompilerTest.createTestScope();
        var result = compiler.compile(expr, null, scope);

        assertInstanceOf(JavaExpression.class, result);
        assertEquals(
            "contains(MapperS.of(10), MapperS.of(20))",
            ((JavaExpression) result).renderToString());
    }

    @Test
    void compiler_dispatches_disjoint() {
        var expr = new RDisjointExpr();
        expr.setLeft(intLiteral(11));
        expr.setRight(intLiteral(22));

        var scope  = ExpressionCompilerTest.createTestScope();
        var result = compiler.compile(expr, null, scope);

        assertInstanceOf(JavaExpression.class, result);
        assertEquals(
            "disjoint(MapperS.of(11), MapperS.of(22))",
            ((JavaExpression) result).renderToString());
    }

    @Test
    void compiler_dispatches_default() {
        var expr = new RDefaultExpr();
        expr.setLeft(intLiteral(30));
        expr.setRight(intLiteral(0));

        var scope  = ExpressionCompilerTest.createTestScope();
        var result = compiler.compile(expr, null, scope);

        assertInstanceOf(JavaExpression.class, result);
        // facet defaultForm (PR #218): scalar-literal default → bare arg + MapperS.of-wrapped result.
        assertEquals(
            "MapperS.of(MapperS.of(30).getOrDefault(0))",
            ((JavaExpression) result).renderToString());
    }

    @Test
    void compiler_dispatches_join_with_separator() {
        var expr = new RJoinExpr();
        expr.setLeft(intLiteral(40));
        expr.setSeparator(intLiteral(50));

        var scope  = ExpressionCompilerTest.createTestScope();
        var result = compiler.compile(expr, null, scope);

        assertInstanceOf(JavaExpression.class, result);
        assertEquals(
            "MapperS.of(40).join(MapperS.of(50))",
            ((JavaExpression) result).renderToString());
    }

    @Test
    void compiler_dispatches_join_without_separator() {
        var expr = new RJoinExpr();
        expr.setLeft(intLiteral(40));
        // no separator

        var scope  = ExpressionCompilerTest.createTestScope();
        var result = compiler.compile(expr, null, scope);

        assertInstanceOf(JavaExpression.class, result);
        assertEquals(
            "MapperS.of(40).join()",
            ((JavaExpression) result).renderToString());
    }
}
