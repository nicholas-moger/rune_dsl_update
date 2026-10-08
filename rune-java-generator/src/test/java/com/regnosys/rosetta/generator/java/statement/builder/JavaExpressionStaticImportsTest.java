package com.regnosys.rosetta.generator.java.statement.builder;

import com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper;
import com.rosetta.util.types.JavaClass;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Invariant tests for the v6.2 static-wildcard-import channel on
 * {@link JavaStatementBuilder#getStaticWildcardImports()}.
 *
 * <p>Ports the substring path at {@code FunctionGenerator.java:843-849}
 * — handlers emitting the 11 {@code ExpressionOperatorsNullSafe.*}
 * methods (areEqual, notEqual, greaterThan, etc.) declare
 * {@link HandlerHelper#EXPRESSION_OPERATORS_NULL_SAFE} in the
 * staticWildcardImports set at construction. The set travels with the
 * builder through wrap/unwrap so deleting the substring check in C3c.2
 * does not regress those functions.
 *
 * <p>See PR-A plan v6.2 Task 3.1 Step 3.1.7b.
 */
class JavaExpressionStaticImportsTest {

    @Test
    void static_imports_round_trip_on_from_factory() {
        Set<JavaClass<?>> refs = Set.of(HandlerHelper.COMPARISON_RESULT);
        Set<JavaClass<?>> staticWildcards = Set.of(
                HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE);
        JavaExpression e = JavaExpression.from(
                "areEqual(a, b, CardinalityOperator.All)", null, refs, staticWildcards);

        assertEquals(staticWildcards, e.getStaticWildcardImports(),
                "4-arg from() must round-trip staticWildcardImports");
        assertEquals(refs, e.getRefs(),
                "4-arg from() must round-trip refs alongside staticWildcardImports");
    }

    @Test
    void static_imports_preserved_through_unwrap() {
        // Construct an inner expression carrying a staticWildcardImport.
        Set<JavaClass<?>> innerStaticWildcards = Set.of(
                HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE);
        JavaExpression inner = JavaExpression.from(
                "areEqual(a, b, CardinalityOperator.All)", null,
                Set.of(HandlerHelper.COMPARISON_RESULT),
                innerStaticWildcards);

        // Wrap with MapperS.of(...) — refs gains MAPPER_S; staticWildcards
        // must carry through unchanged (v6.2: wraps contribute nothing to
        // the static-wildcard channel).
        JavaExpression wrapped = JavaExpression.wrappedInMapperSOf(inner);
        assertEquals(innerStaticWildcards, wrapped.getStaticWildcardImports(),
                "wrappedInMapperSOf must carry inner.staticWildcardImports unchanged");
        assertEquals(
                Set.of(HandlerHelper.COMPARISON_RESULT, HandlerHelper.MAPPER_S),
                wrapped.getRefs(),
                "wrappedInMapperSOf adds MAPPER_S to refs (sanity check)");

        // Unwrap returns the inner builder — staticWildcardImports still
        // intact and refs reverts to inner.refs (MAPPER_S dropped).
        JavaStatementBuilder unwrapped = wrapped.unwrapToBuilder()
                .orElseThrow(() -> new AssertionError(
                        "wrappedInMapperSOf must set unwrapToBuilder"));
        assertEquals(innerStaticWildcards, unwrapped.getStaticWildcardImports(),
                "Unwrap must preserve staticWildcardImports");
        assertEquals(Set.of(HandlerHelper.COMPARISON_RESULT), unwrapped.getRefs(),
                "Unwrap drops MAPPER_S from refs atomically");
    }
}
