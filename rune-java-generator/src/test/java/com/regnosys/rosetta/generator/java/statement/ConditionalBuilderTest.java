package com.regnosys.rosetta.generator.java.statement;

import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.JavaLocalVariableDeclarationStatement;
import com.regnosys.rosetta.generator.java.statement.JavaStatementList;
import com.regnosys.rosetta.generator.java.statement.builder.JavaBlockBuilder;
import com.regnosys.rosetta.generator.java.statement.builder.JavaConditionalExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaIfThenElseBuilder;
import com.regnosys.rosetta.generator.java.statement.builder.JavaLiteral;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaPrimitiveType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JavaConditionalExpression} and {@link JavaIfThenElseBuilder}.
 */
class ConditionalBuilderTest {

    private JavaTypeUtil typeUtil;

    @BeforeEach
    void setUp() {
        typeUtil = new JavaTypeUtil();
    }

    // ==========================================================================
    // JavaConditionalExpression — rendering
    // ==========================================================================

    @Test
    void ternary_renders_correctly() {
        var cond = JavaExpression.from("x != null", JavaPrimitiveType.BOOLEAN);
        var thenExpr = JavaExpression.from("x", JavaClass.from(String.class));
        var elseExpr = JavaExpression.from("\"default\"", JavaClass.from(String.class));

        var ternary = new JavaConditionalExpression(
                cond, thenExpr, elseExpr, JavaClass.from(String.class), typeUtil);

        assertEquals("x != null ? x : \"default\"", ternary.toString());
    }

    @Test
    void ternary_convenience_ctor_infers_type() {
        var cond = JavaExpression.from("flag", JavaPrimitiveType.BOOLEAN);
        var thenExpr = JavaExpression.from("42", JavaClass.from(Integer.class));
        var elseExpr = JavaExpression.from("0", JavaClass.from(Integer.class));

        var ternary = new JavaConditionalExpression(cond, thenExpr, elseExpr, typeUtil);

        assertEquals("flag ? 42 : 0", ternary.toString());
        assertEquals(JavaClass.from(Integer.class), ternary.getExpressionType());
    }

    @Test
    void ternary_complete_as_return() {
        var cond = JavaExpression.from("b", JavaPrimitiveType.BOOLEAN);
        var thenExpr = JavaExpression.from("1", JavaPrimitiveType.INT);
        var elseExpr = JavaExpression.from("-1", JavaPrimitiveType.INT);

        var ternary = new JavaConditionalExpression(
                cond, thenExpr, elseExpr, JavaPrimitiveType.INT, typeUtil);
        var stmt = ternary.completeAsReturn();

        assertTrue(stmt.toString().contains("return b ? 1 : -1;"),
                "Expected ternary return, got: " + stmt);
    }

    @Test
    void ternary_collapse_to_single_expression_wraps_in_parens() {
        var cond = JavaExpression.from("c", JavaPrimitiveType.BOOLEAN);
        var thenExpr = JavaExpression.from("a", JavaPrimitiveType.INT);
        var elseExpr = JavaExpression.from("b", JavaPrimitiveType.INT);

        var ternary = new JavaConditionalExpression(
                cond, thenExpr, elseExpr, JavaPrimitiveType.INT, typeUtil);
        var scope = new JavaStatementScope("test", null);
        var collapsed = ternary.collapseToSingleExpression(scope);

        assertTrue(collapsed instanceof JavaExpression, "Collapsed should be a JavaExpression");
        var rendered = ((JavaExpression) collapsed).renderToString();
        assertTrue(rendered.startsWith("(") && rendered.endsWith(")"),
                "Expected parens around ternary, got: " + rendered);
        assertTrue(rendered.contains("c ? a : b"),
                "Expected ternary body, got: " + rendered);
    }

    @Test
    void ternary_map_expression_stays_ternary_when_both_results_are_expressions() {
        var cond = JavaExpression.from("flag", JavaPrimitiveType.BOOLEAN);
        var thenExpr = JavaExpression.from("x", JavaClass.from(String.class));
        var elseExpr = JavaExpression.from("y", JavaClass.from(String.class));

        var ternary = new JavaConditionalExpression(
                cond, thenExpr, elseExpr, JavaClass.from(String.class), typeUtil);
        var mapped = ternary.mapExpression(e ->
                JavaExpression.from("wrap(" + e.renderToString() + ")", JavaClass.from(String.class)));

        assertTrue(mapped instanceof JavaConditionalExpression,
                "Mapped result should still be a ternary");
        assertTrue(mapped.toString().contains("wrap(x)") && mapped.toString().contains("wrap(y)"),
                "Expected wrapped branches, got: " + mapped);
    }

    @Test
    void ternary_map_expression_upgrades_to_if_then_else_when_branch_has_statements() {
        var cond = JavaExpression.from("flag", JavaPrimitiveType.BOOLEAN);
        var thenExpr = JavaExpression.from("x", JavaClass.from(String.class));
        var elseExpr = JavaExpression.from("y", JavaClass.from(String.class));

        var ternary = new JavaConditionalExpression(
                cond, thenExpr, elseExpr, JavaClass.from(String.class), typeUtil);

        // Mapper returns a non-expression for one branch (simulated by JavaIfThenElseBuilder)
        var mapped = ternary.mapExpression(e -> {
            // Create a non-trivial builder that requires statements
            var inner = JavaExpression.from("compute(" + e.renderToString() + ")",
                    JavaClass.from(String.class));
            return new JavaIfThenElseBuilder(
                    JavaExpression.from("inner != null", JavaPrimitiveType.BOOLEAN),
                    inner,
                    JavaLiteral.NULL,
                    typeUtil
            );
        });

        assertTrue(mapped instanceof JavaIfThenElseBuilder,
                "Mapped result should upgrade to JavaIfThenElseBuilder when branches have statements");
    }

    @Test
    void ternary_get_expression_type_uses_explicit_common_type() {
        var cond = JavaExpression.from("b", JavaPrimitiveType.BOOLEAN);
        var thenExpr = JavaExpression.from("s", JavaClass.from(String.class));
        var elseExpr = JavaExpression.from("null", JavaClass.OBJECT);

        var ternary = new JavaConditionalExpression(
                cond, thenExpr, elseExpr, JavaClass.from(String.class), typeUtil);

        assertEquals(JavaClass.from(String.class), ternary.getExpressionType());
    }

    // ==========================================================================
    // JavaIfThenElseBuilder — rendering
    // ==========================================================================

    @Test
    void if_then_else_builder_toString() {
        var cond = JavaExpression.from("x == null", JavaPrimitiveType.BOOLEAN);
        var thenBranch = JavaExpression.from("defaultVal", JavaClass.from(String.class));
        var elseBranch = JavaExpression.from("x", JavaClass.from(String.class));

        var builder = new JavaIfThenElseBuilder(
                cond, thenBranch, elseBranch, JavaClass.from(String.class), typeUtil);

        String rendered = builder.toString();
        // Note: renderBlock() uses single-tab indentation — this is debug/toString output only.
        // Actual code generation uses completeAsReturn() / completeAsAssignment() paths.
        assertEquals("if (x == null) {\n\tdefaultVal\n} else {\n\tx\n}", rendered);
    }

    @Test
    void if_then_else_builder_complete_as_assignment() {
        var cond = JavaExpression.from("ready", JavaPrimitiveType.BOOLEAN);
        var thenBranch = JavaExpression.from("computeA()", JavaClass.from(String.class));
        var elseBranch = JavaExpression.from("computeB()", JavaClass.from(String.class));

        var builder = new JavaIfThenElseBuilder(
                cond, thenBranch, elseBranch, JavaClass.from(String.class), typeUtil);

        var scope = new JavaStatementScope("test", null);
        var id = scope.createUniqueIdentifier("result");
        var stmt = builder.completeAsAssignment(id);

        String rendered = stmt.toString();
        assertEquals(
                "if (ready) {\nresult = computeA();\n} else {\nresult = computeB();\n}\n",
                rendered);
    }

    @Test
    void if_then_else_builder_complete_as_return() {
        var cond = JavaExpression.from("ok", JavaPrimitiveType.BOOLEAN);
        var thenBranch = JavaExpression.from("\"yes\"", JavaClass.from(String.class));
        var elseBranch = JavaExpression.from("\"no\"", JavaClass.from(String.class));

        var builder = new JavaIfThenElseBuilder(
                cond, thenBranch, elseBranch, JavaClass.from(String.class), typeUtil);
        var stmt = builder.completeAsReturn();

        String rendered = stmt.toString();
        // JavaIfThenStatement renders then-branch in a block; else is a bare return statement.
        assertEquals("if (ok) {\nreturn \"yes\";\n}\nreturn \"no\";\n", rendered);
    }

    @Test
    void if_then_else_builder_declare_as_variable_with_literal_else() {
        // Special case: else is a literal — uses optimized form (declare with initializer,
        // then conditionally overwrite in if-branch)
        var cond = JavaExpression.from("flag", JavaPrimitiveType.BOOLEAN);
        var thenBranch = JavaExpression.from("\"dynamic\"", JavaClass.from(String.class));
        var elseBranch = JavaLiteral.NULL;

        var builder = new JavaIfThenElseBuilder(
                cond, thenBranch, elseBranch, JavaClass.from(String.class), typeUtil);
        var scope = new JavaStatementScope("test", null);
        var declared = builder.declareAsVariable(false, "val", scope);
        var stmt = declared.completeAsReturn();

        String rendered = stmt.toString();
        // The optimized path: String val = null; if (flag) { val = "dynamic"; } return val;
        assertTrue(rendered.contains("null"), "Expected null initializer");
        assertTrue(rendered.contains("if (flag)"), "Expected condition");
        assertTrue(rendered.contains("return val"), "Expected return of variable");
    }

    @Test
    void if_then_else_builder_map_expression() {
        var cond = JavaExpression.from("c", JavaPrimitiveType.BOOLEAN);
        var thenBranch = JavaExpression.from("a", JavaClass.from(Integer.class));
        var elseBranch = JavaExpression.from("b", JavaClass.from(Integer.class));

        var builder = new JavaIfThenElseBuilder(
                cond, thenBranch, elseBranch, JavaClass.from(Integer.class), typeUtil);
        var mapped = builder.mapExpression(e ->
                JavaExpression.from(e.renderToString() + " * 2", JavaClass.from(Integer.class)));

        assertTrue(mapped instanceof JavaIfThenElseBuilder);
        String rendered = mapped.toString();
        assertTrue(rendered.contains("a * 2"), "Expected mapped then branch");
        assertTrue(rendered.contains("b * 2"), "Expected mapped else branch");
    }

    @Test
    void if_then_else_builder_to_lambda_body() {
        var cond = JavaExpression.from("check()", JavaPrimitiveType.BOOLEAN);
        var thenBranch = JavaExpression.from("\"yes\"", JavaClass.from(String.class));
        var elseBranch = JavaExpression.from("\"no\"", JavaClass.from(String.class));

        var builder = new JavaIfThenElseBuilder(
                cond, thenBranch, elseBranch, JavaClass.from(String.class), typeUtil);
        var lambdaBody = builder.toLambdaBody();

        var sb = new StringBuilder();
        lambdaBody.render(sb);
        String rendered = sb.toString();
        assertTrue(rendered.contains("if (check())"), "Expected condition in lambda body");
        assertTrue(rendered.contains("return \"yes\""), "Expected then return in lambda body");
        assertTrue(rendered.contains("return \"no\""), "Expected else return in lambda body");
    }

    @Test
    void if_then_else_builder_to_lambda_body_with_leading_statements() {
        // Exercises toLambdaBody() when the then-branch is a JavaBlockBuilder
        // (i.e., it has leading statements before its ending expression).
        // This is the scenario that BLOCKER-1 guards: the upstream pattern
        //   new JavaBlockBuilder(this).toLambdaBody()
        // must correctly delegate completeAsReturn() through the if-then-else so
        // that leading statements in a branch are preserved.
        var scope = new JavaStatementScope("test", null);
        var varId = scope.createUniqueIdentifier("x");

        // Build: { String x = "computed"; x } as the then-branch
        var leadingDecl = new JavaLocalVariableDeclarationStatement(
                false, JavaClass.from(String.class), varId,
                JavaExpression.from("\"computed\"", JavaClass.from(String.class)));
        var endExpr = JavaExpression.from("x", JavaClass.from(String.class));
        var thenBranch = new JavaBlockBuilder(JavaStatementList.of(leadingDecl), endExpr);

        var elseBranch = JavaExpression.from("\"default\"", JavaClass.from(String.class));
        var cond = JavaExpression.from("flag", JavaPrimitiveType.BOOLEAN);

        var builder = new JavaIfThenElseBuilder(
                cond, thenBranch, elseBranch, JavaClass.from(String.class), typeUtil);
        var lambdaBody = builder.toLambdaBody();

        var sb = new StringBuilder();
        lambdaBody.render(sb);
        String rendered = sb.toString();

        // then-branch leading statement must be inside the if block. Re-anchored at
        // PR #422: a statement-carrying lambda body renders BRACED (upstream's
        // JavaBlock toLambdaBody form) — '{', each flat line one embedded tab deeper
        // (the stream-continuation level), '\t}' close with no trailing newline; the
        // brace-aware reindent channel (PojoCompatEmitter.renderBody) restores the
        // visual nesting. Golden witnesses: the pojo-bulk-value-narrow compat arms.
        assertEquals(
                "{\n\tif (flag) {\n\tString x = \"computed\";\n\treturn x;\n\t}\n\treturn \"default\";\n\t}",
                rendered);
    }

    // ==========================================================================
    // JavaConditionalExpression implements JavaLambdaBody
    // ==========================================================================

    @Test
    void ternary_as_lambda_body_renders_ternary() {
        var cond = JavaExpression.from("b", JavaPrimitiveType.BOOLEAN);
        var thenExpr = JavaExpression.from("1", JavaPrimitiveType.INT);
        var elseExpr = JavaExpression.from("0", JavaPrimitiveType.INT);

        var ternary = new JavaConditionalExpression(
                cond, thenExpr, elseExpr, JavaPrimitiveType.INT, typeUtil);
        var lambdaBody = ternary.toLambdaBody();

        var sb = new StringBuilder();
        lambdaBody.render(sb);
        assertEquals("b ? 1 : 0", sb.toString());
    }
}
