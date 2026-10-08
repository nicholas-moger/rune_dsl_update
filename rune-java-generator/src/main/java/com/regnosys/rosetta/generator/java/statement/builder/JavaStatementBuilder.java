package com.regnosys.rosetta.generator.java.statement.builder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.regnosys.rosetta.generator.GeneratedIdentifier;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.JavaLambdaBody;
import com.regnosys.rosetta.generator.java.statement.JavaStatement;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;

/**
 * A convenient API for building a Java statement.
 *
 * <p>A statement builder consists of a list of statements, ending with one
 * or more expressions. The builder can be "completed" into a JavaStatement
 * by converting the ending expression(s) into a valid statement (return,
 * assignment, etc.).
 *
 * <p>The simplest statement builder is a single {@link JavaExpression}.
 *
 * <p>Ported from upstream — replaced {@code StringConcatenationClient}
 * with {@link StringBuilder}-based rendering.
 */
public abstract class JavaStatementBuilder {

    /**
     * Invoke a method with the given argument builders. Collapses all
     * arguments into single expressions, then applies the method invoker.
     */
    public static JavaStatementBuilder invokeMethod(
            List<JavaStatementBuilder> arguments,
            Function<JavaExpression, ? extends JavaStatementBuilder> methodInvoker,
            JavaStatementScope scope) {
        if (arguments.isEmpty()) {
            return methodInvoker.apply(null);
        }
        JavaStatementBuilder argCode = arguments.get(0);
        for (var i = 1; i < arguments.size(); i++) {
            final int idx = i;
            argCode = argCode.then(
                    arguments.get(idx),
                    (argList, newArg) -> {
                        // Union refs + staticWildcardImports from both operands so
                        // the synthesised comma-separated JavaExpression carries
                        // every import its rendered source references. Copilot
                        // round 8 finding — the legacy 2-arg factory hard-codes
                        // empty sets, silently dropping imports for any type
                        // referenced inside the arguments once ImportCollector
                        // consumes the structured path.
                        Set<JavaClass<?>> combinedRefs = new HashSet<>(argList.getRefs());
                        combinedRefs.addAll(newArg.getRefs());
                        Set<JavaClass<?>> combinedWildcards =
                                new HashSet<>(argList.getStaticWildcardImports());
                        combinedWildcards.addAll(newArg.getStaticWildcardImports());
                        return JavaExpression.from(
                                argList.renderToString() + ", " + newArg.renderToString(),
                                null,
                                combinedRefs,
                                combinedWildcards);
                    },
                    scope
            );
        }
        return argCode.collapseToSingleExpression(scope).mapExpression(methodInvoker);
    }

    /**
     * Get the type of the last expression of this builder.
     */
    public abstract JavaType getExpressionType();

    /**
     * Library and domain classes this builder's rendered source references.
     *
     * <p>Under PR-A option F (v6.1), refs travel with the builder through
     * unwrap / map / then / collapse and feed {@code ImportCollector}
     * directly. The structural invariant is
     * {@code builder.refs ⊇ library/domain classes textually present in
     * builder.renderToString()} — supersetting is safe for import lists.
     */
    public abstract Set<JavaClass<?>> getRefs();

    /**
     * Static wildcard imports (e.g. {@code import static Foo.*}) this
     * builder's rendered source depends on.
     *
     * <p>Introduced in PR-A v6.2 to port the substring path at
     * {@code FunctionGenerator.java:843-849} which adds
     * {@code com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*}
     * when the emitted source contains any of 11 operator method names
     * (areEqual, notEqual, greaterThan, etc.). Handlers emitting those
     * methods declare {@code HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE}
     * in this set at builder construction.
     */
    public abstract Set<JavaClass<?>> getStaticWildcardImports();

    /**
     * Complete this builder by mapping all expressions to a statement.
     */
    public abstract JavaStatement complete(Function<JavaExpression, JavaStatement> completer);

    /** Complete by returning all expressions. */
    public abstract JavaStatement completeAsReturn();

    /** Complete by ending all expressions with a semicolon. */
    public abstract JavaStatement completeAsExpressionStatement();

    /** Complete by assigning all expressions to a variable. */
    public abstract JavaStatement completeAsAssignment(GeneratedIdentifier variableId);

    /**
     * Map all expressions in this builder to a new builder.
     */
    public abstract JavaStatementBuilder mapExpression(
            Function<JavaExpression, ? extends JavaStatementBuilder> mapper);

    /**
     * Map non-null expressions. Null literals pass through unchanged.
     */
    public JavaStatementBuilder mapExpressionIfNotNull(
            Function<JavaExpression, ? extends JavaStatementBuilder> mapper) {
        return mapExpression(expr ->
                expr == JavaLiteral.NULL ? JavaLiteral.NULL : mapper.apply(expr));
    }

    /**
     * Assign all expressions to a new variable.
     */
    public abstract JavaStatementBuilder declareAsVariable(
            boolean isFinal, String variableId, JavaStatementScope scope);

    /**
     * If this builder ends with multiple branches, collapse to a single expression.
     */
    public abstract JavaStatementBuilder collapseToSingleExpression(JavaStatementScope scope);

    /**
     * Append another builder, combining expressions with the given operation.
     */
    public abstract JavaStatementBuilder then(
            JavaStatementBuilder after,
            BiFunction<JavaExpression, JavaExpression, JavaStatementBuilder> combineExpressions,
            JavaStatementScope scope);

    /**
     * Convert this builder into a valid lambda body.
     */
    public abstract JavaLambdaBody toLambdaBody();
}
