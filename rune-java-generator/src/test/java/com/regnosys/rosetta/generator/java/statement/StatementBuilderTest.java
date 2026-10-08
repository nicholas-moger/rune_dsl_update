package com.regnosys.rosetta.generator.java.statement;

import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaLiteral;
import com.regnosys.rosetta.generator.java.statement.builder.JavaThis;
import com.regnosys.rosetta.generator.java.statement.builder.JavaVariable;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaPrimitiveType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatementBuilderTest {

    // === JavaExpression =======================================================

    @Test void expression_from_string() {
        var expr = JavaExpression.from("42", JavaPrimitiveType.INT);
        assertEquals("42", expr.renderToString());
        assertEquals(JavaPrimitiveType.INT, expr.getExpressionType());
    }

    @Test void expression_complete_as_return() {
        var expr = JavaExpression.from("result", JavaClass.from(String.class));
        var stmt = expr.completeAsReturn();
        assertEquals("return result;\n", stmt.toString());
    }

    @Test void expression_complete_as_expression_statement() {
        var expr = JavaExpression.from("doSomething()", JavaClass.OBJECT);
        var stmt = expr.completeAsExpressionStatement();
        assertEquals("doSomething();\n", stmt.toString());
    }

    @Test void expression_complete_as_assignment() {
        var scope = new JavaStatementScope("test", null);
        var id = scope.createUniqueIdentifier("x");
        var expr = JavaExpression.from("42", JavaPrimitiveType.INT);
        var stmt = expr.completeAsAssignment(id);
        assertEquals("x = 42;\n", stmt.toString());
    }

    // === Variable declaration =================================================

    @Test void declare_as_variable() {
        var scope = new JavaStatementScope("test", null);
        var expr = JavaExpression.from("computeValue()", JavaPrimitiveType.INT);
        var builder = expr.declareAsVariable(true, "result", scope);

        var stmt = builder.completeAsReturn();
        String rendered = stmt.toString();
        assertTrue(rendered.contains("final int result = computeValue();"),
                "Expected variable declaration, got: " + rendered);
        assertTrue(rendered.contains("return result;"),
                "Expected return statement, got: " + rendered);
    }

    // === Literals =============================================================

    @Test void null_literal() {
        assertEquals("null", JavaLiteral.NULL.renderToString());
    }

    @Test void true_literal() {
        assertEquals("true", JavaLiteral.TRUE.renderToString());
        assertEquals(JavaPrimitiveType.BOOLEAN, JavaLiteral.TRUE.getExpressionType());
    }

    @Test void false_literal() {
        assertEquals("false", JavaLiteral.FALSE.renderToString());
    }

    // === JavaThis =============================================================

    @Test void this_expression() {
        var thisExpr = new JavaThis(JavaClass.from(String.class));
        assertEquals("this", thisExpr.renderToString());
    }

    // === JavaVariable =========================================================

    @Test void variable_renders_actual_name() {
        var scope = new JavaStatementScope("test", null);
        var id = scope.createIdentifier("key", "myVar");
        var variable = new JavaVariable(id, JavaClass.from(String.class));
        assertEquals("myVar", variable.renderToString());
    }

    // === Statement list =======================================================

    @Test void statement_list_renders_all() {
        var s1 = JavaExpression.from("a()", JavaClass.OBJECT).completeAsExpressionStatement();
        var s2 = JavaExpression.from("b()", JavaClass.OBJECT).completeAsExpressionStatement();
        var list = JavaStatementList.of(s1, s2);
        assertEquals("a();\nb();\n", list.toString());
    }

    // === Block ================================================================

    @Test void block_wraps_in_braces() {
        var stmt = JavaExpression.from("x", JavaPrimitiveType.INT).completeAsReturn();
        var block = new JavaBlock(JavaStatementList.of(stmt));
        String rendered = block.toString();
        assertTrue(rendered.startsWith("{"));
        assertTrue(rendered.contains("return x;"));
        assertTrue(rendered.endsWith("}\n"));
    }

    // === mapExpression ========================================================

    @Test void map_expression_transforms() {
        var expr = JavaExpression.from("x", JavaPrimitiveType.INT);
        var mapped = expr.mapExpression(e ->
                JavaExpression.from(e.renderToString() + " + 1", JavaPrimitiveType.INT));
        var stmt = mapped.completeAsReturn();
        assertTrue(stmt.toString().contains("return x + 1;"));
    }

    // === mapExpressionIfNotNull ===============================================

    @Test void map_if_not_null_skips_null_literal() {
        var result = JavaLiteral.NULL.mapExpressionIfNotNull(e ->
                JavaExpression.from("SHOULD_NOT_APPEAR", JavaClass.OBJECT));
        assertTrue(result instanceof JavaExpression);
        assertEquals("null", ((JavaExpression) result).renderToString());
    }

    @Test void map_if_not_null_transforms_non_null() {
        var expr = JavaExpression.from("x", JavaPrimitiveType.INT);
        var result = expr.mapExpressionIfNotNull(e ->
                JavaExpression.from("wrapped(" + e.renderToString() + ")", JavaClass.OBJECT));
        assertEquals("wrapped(x)", ((JavaExpression) result).renderToString());
    }

    // === toLambdaBody =========================================================

    @Test void expression_as_lambda_body() {
        var expr = JavaExpression.from("x + 1", JavaPrimitiveType.INT);
        var body = expr.toLambdaBody();
        var sb = new StringBuilder();
        body.render(sb);
        assertEquals("x + 1", sb.toString());
    }
}
