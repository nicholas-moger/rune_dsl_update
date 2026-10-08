package com.regnosys.rosetta.generator.java.statement.builder;

import com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper;
import com.regnosys.rosetta.generator.java.expression.handlers.ReferenceHandler;
import com.regnosys.rosetta.generator.java.statement.JavaReturnStatement;
import com.regnosys.rosetta.generator.java.statement.JavaStatementList;
import com.rosetta.util.types.JavaClass;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Invariant tests for the PR-A v6.1 refs-on-builder infrastructure
 * ({@link JavaStatementBuilder#getRefs()} + its carriers on
 * {@link JavaExpression} wrap factories).
 *
 * <p>See PR-A plan v6.2 Task 3.1 Step 3.1.7 (C3a.1) and Task 3.2 Step
 * 3.2.5 (C3a.2). Six invariants total — four landed in C3a.1 commit
 * 99f1649; two added in C3a.2: {@code refs_aggregate_through_javablockbuilder}
 * (R2-Q5a follow-up) and {@code refs_unwrap_falls_through_on_chained_form}
 * (chained-form invariant that depends on
 * {@link ReferenceHandler#unwrapForEvaluateArg} being builder-level,
 * which C3a.2 provides).
 */
class JavaStatementBuilderRefsTest {

    @Test
    void refs_immutable_view_on_javaexpression_from() {
        Set<JavaClass<?>> originalRefs = new HashSet<>();
        originalRefs.add(HandlerHelper.MAPPER_S);
        JavaExpression e = JavaExpression.from("x", null, originalRefs);

        // Mutate caller's set after construction — the builder must have
        // defensively copied.
        originalRefs.add(HandlerHelper.MAPPER_C);
        assertEquals(Set.of(HandlerHelper.MAPPER_S), e.getRefs(),
                "Builder must copy refs defensively");
    }

    @Test
    void refs_null_throws_npe() {
        // R2 D#4 — common handler mistake: passing null for refs.
        assertThrows(NullPointerException.class,
                () -> JavaExpression.from("x", null, null));
    }

    @Test
    void refs_aggregate_through_javastatementlist() {
        JavaExpression a = JavaExpression.from("a", null, Set.of(HandlerHelper.MAPPER_S));
        JavaExpression b = JavaExpression.from("b", null, Set.of(HandlerHelper.MAPPER_C));
        JavaStatementList list = JavaStatementList.of(
                new JavaReturnStatement(a),
                new JavaReturnStatement(b));

        Set<JavaClass<?>> expected = Set.of(HandlerHelper.MAPPER_S, HandlerHelper.MAPPER_C);
        assertEquals(expected, list.getRefs(),
                "JavaStatementList.getRefs() must aggregate via union over all statements");
    }

    @Test
    void refs_aggregate_through_javablockbuilder() {
        // R2-Q5a follow-up from C3a.1 review. JavaBlockBuilder wraps a
        // leading JavaStatementList plus an ending expression; its getRefs()
        // must union BOTH sides. Without this aggregation, refs carried by
        // declared-variable expressions (the pattern FunctionGenerator uses
        // when flattening a chained builder via declareAsVariable) would be
        // silently dropped at codegen time and downstream imports would
        // regress. The JavaStatementList aggregate is already exercised by
        // refs_aggregate_through_javastatementlist; this test adds the
        // complementary JavaBlockBuilder path so both composite builders
        // are guarded.
        JavaExpression leadingExpr =
                JavaExpression.from("a", null, Set.of(HandlerHelper.MAPPER_S));
        JavaExpression endingExpr =
                JavaExpression.from("b", null, Set.of(HandlerHelper.MAPPER_C));
        JavaStatementList leading =
                JavaStatementList.of(new JavaReturnStatement(leadingExpr));

        JavaBlockBuilder block = new JavaBlockBuilder(leading, endingExpr);

        Set<JavaClass<?>> expected =
                Set.of(HandlerHelper.MAPPER_S, HandlerHelper.MAPPER_C);
        assertEquals(expected, block.getRefs(),
                "JavaBlockBuilder.getRefs() must union leadingStatements.getRefs() "
                        + "with lastStatement.getRefs()");
    }

    @Test
    void refs_unwrap_falls_through_on_chained_form() {
        // Deferred 5th test from C3a.1 — now landable because
        // ReferenceHandler.unwrapForEvaluateArg is builder-level in C3a.2.
        //
        // Chained form: MapperS.of(inner).map(...). Construct via
        // JavaExpression.from(...) directly (not via wrappedInMapperSOf) so
        // unwrapToBuilder stays Optional.empty — verifying that the
        // structured unwrap branch does NOT fire, and the helper falls
        // through to the ".get()" suffix branch instead.
        //
        // This pins a load-bearing invariant: handler code that constructs
        // a chained builder by string concatenation (MapperS.of wrapper +
        // .map suffix) must NEVER route through the wrap factory — if it
        // did, a later unwrap would strip MapperS.of PLUS the chain, which
        // is semantically wrong. The refs field of the chained builder
        // carries MAPPER_S explicitly because the source text still
        // contains MapperS.of.
        JavaExpression chained = JavaExpression.from(
                "MapperS.of(x).map(\"getFoo\", _p -> _p.getFoo())",
                null,
                Set.of(HandlerHelper.MAPPER_S));
        assertTrue(chained.unwrapToBuilder().isEmpty(),
                "Chained form (built via JavaExpression.from) must NOT set "
                        + "unwrapToBuilder — only wrappedInMapperSOf does");

        JavaStatementBuilder unwrapped = ReferenceHandler.unwrapForEvaluateArg(chained);
        assertTrue(unwrapped instanceof JavaExpression,
                "Unwrap must return a JavaExpression for the chained-form input");
        JavaExpression unwrappedExpr = (JavaExpression) unwrapped;
        assertEquals(
                "MapperS.of(x).map(\"getFoo\", _p -> _p.getFoo()).get()",
                unwrappedExpr.renderToString(),
                "Fall-through branch must append .get() to chained source");
        // Refs preservation: MAPPER_S must survive because the source still
        // contains MapperS.of — the fall-through branch preserves refs
        // verbatim, unlike the wrap-factory unwrap which drops MAPPER_S.
        assertEquals(Set.of(HandlerHelper.MAPPER_S), unwrappedExpr.getRefs(),
                "Fall-through branch must preserve inbound refs (MAPPER_S stays "
                        + "because source still contains MapperS.of)");
    }

    @Test
    void conditional_collapse_preserves_refs_and_staticWildcards() {
        // Copilot round-8 finding: JavaConditionalExpression aggregates
        // refs/staticWildcards in getRefs()/getStaticWildcardImports(), but
        // collapseToSingleExpression() + toExpression() previously used the
        // 2-arg JavaExpression.from factory which discarded both. Downstream
        // consumers (then / invokeMethod / import collection) would therefore
        // lose imports for any type referenced inside a ternary's branches.
        JavaExpression condition = JavaExpression.from(
                "areEqual(a, b, CardinalityOperator.All)",
                null,
                Set.of(HandlerHelper.CARDINALITY_OPERATOR),
                Set.of(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE));
        JavaExpression thenBranch = JavaExpression.from(
                "MapperS.of(x)", null, Set.of(HandlerHelper.MAPPER_S));
        JavaExpression elseBranch = JavaExpression.from(
                "MapperS.of(y)", null, Set.of(HandlerHelper.MAPPER_S));

        // Full-constructor form avoids the typeUtil.join() convenience path so
        // the test does not depend on a real JavaTypeUtil.
        JavaConditionalExpression cond = new JavaConditionalExpression(
                condition, thenBranch, elseBranch, null, null);

        JavaExpression collapsed = cond.collapseToSingleExpression(null);
        assertEquals(
                Set.of(HandlerHelper.CARDINALITY_OPERATOR, HandlerHelper.MAPPER_S),
                collapsed.getRefs(),
                "collapseToSingleExpression must union refs across all three branches");
        assertEquals(
                Set.of(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE),
                collapsed.getStaticWildcardImports(),
                "collapseToSingleExpression must propagate staticWildcardImports");
    }

    @Test
    void invokeMethod_arglist_unions_refs_and_staticWildcards_across_arguments() {
        // Copilot round-8 finding: JavaStatementBuilder.invokeMethod built
        // its comma-separated argument list via the 2-arg JavaExpression.from
        // factory which hard-codes empty refs + staticWildcardImports — any
        // type referenced inside the arguments was silently dropped.
        //
        // Verification is via public invokeMethod: pass two JavaExpression
        // arguments each carrying distinct refs/wildcards, supply an identity
        // methodInvoker, and assert the final builder's getRefs() +
        // getStaticWildcardImports() contain the union of both args.
        JavaExpression arg1 = JavaExpression.from(
                "areEqual(a, b, CardinalityOperator.All)",
                null,
                Set.of(HandlerHelper.CARDINALITY_OPERATOR),
                Set.of(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE));
        JavaExpression arg2 = JavaExpression.from(
                "MapperS.of(c)",
                null,
                Set.of(HandlerHelper.MAPPER_S));

        JavaStatementBuilder result = JavaStatementBuilder.invokeMethod(
                java.util.List.of(arg1, arg2),
                expr -> expr,  // identity invoker — the returned expression IS the joined arg list
                null);

        // The joined arg list must carry refs + staticWildcards from BOTH args.
        assertTrue(result.getRefs().contains(HandlerHelper.CARDINALITY_OPERATOR),
                "invokeMethod arg-list must union refs from argument 1");
        assertTrue(result.getRefs().contains(HandlerHelper.MAPPER_S),
                "invokeMethod arg-list must union refs from argument 2");
        assertTrue(result.getStaticWildcardImports()
                        .contains(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE),
                "invokeMethod arg-list must union staticWildcardImports across args");
    }

    @Test
    void refs_mutate_atomically_in_wrap_strip() {
        // wrap: inner (Set.of()) → MapperS.of(inner) carries {MAPPER_S}.
        JavaExpression inner = JavaExpression.from("varName", null, Set.of());
        JavaExpression wrapped = JavaExpression.wrappedInMapperSOf(inner);
        assertEquals(Set.of(HandlerHelper.MAPPER_S), wrapped.getRefs(),
                "Wrap must add MAPPER_S to refs");
        assertEquals("MapperS.of(varName)", wrapped.renderToString());

        // strip: unwrapToBuilder().get() returns the inner builder with
        // MAPPER_S dropped atomically with the source strip.
        JavaStatementBuilder unwrapped = wrapped.unwrapToBuilder()
                .orElseThrow(() -> new AssertionError(
                        "wrappedInMapperSOf must set unwrapToBuilder"));
        assertEquals(Set.of(), unwrapped.getRefs(),
                "Unwrap must drop MAPPER_S atomically with source strip");
        assertEquals("varName",
                ((JavaExpression) unwrapped).renderToString());
    }
}
