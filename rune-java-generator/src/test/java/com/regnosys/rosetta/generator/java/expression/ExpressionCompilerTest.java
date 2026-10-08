package com.regnosys.rosetta.generator.java.expression;

import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ExpressionCompilerTest {

    @Test
    void compile_dispatches_int_literal_to_handler() {
        var compiler = new ExpressionCompiler();
        var scope = createTestScope();
        var expr = new RIntLiteral();
        expr.setValue(42);
        var result = compiler.compile(expr, null, scope);
        assertInstanceOf(JavaExpression.class, result);
        assertEquals("MapperS.of(42)", ((JavaExpression) result).renderToString());
    }

    @Test
    void typed_constructor_creates_compiler_with_coercion() {
        var typeUtil = new JavaTypeUtil();
        var compiler = new ExpressionCompiler(null, null, typeUtil);
        assertNotNull(compiler.getTypeUtil());
    }

    @Test
    void coercion_dormant_when_handler_types_null() {
        var compiler = new ExpressionCompiler();
        var scope = createTestScope();
        var expr = new RIntLiteral();
        expr.setValue(42);
        // M7b-1 handlers produce null types, so coercion should not activate
        var result = compiler.compile(expr, null, scope);
        assertEquals("MapperS.of(42)", ((JavaExpression) result).renderToString());
    }

    /** Simple scope for tests — null parent is valid per GeneratorScopeTest pattern. */
    public static JavaStatementScope createTestScope() {
        return new JavaStatementScope("test", null);
    }
}
