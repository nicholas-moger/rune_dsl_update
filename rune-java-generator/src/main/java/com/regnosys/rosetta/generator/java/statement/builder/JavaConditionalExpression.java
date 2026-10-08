package com.regnosys.rosetta.generator.java.statement.builder;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.regnosys.rosetta.generator.GeneratedIdentifier;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.JavaAssignment;
import com.regnosys.rosetta.generator.java.statement.JavaIfThenElseStatement;
import com.regnosys.rosetta.generator.java.statement.JavaLambdaBody;
import com.regnosys.rosetta.generator.java.statement.JavaReturnStatement;
import com.regnosys.rosetta.generator.java.statement.JavaStatement;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;

/**
 * A Java ternary (conditional) expression: {@code condition ? thenExpr : elseExpr}.
 *
 * <p>Based on the Java specification:
 * https://docs.oracle.com/javase/specs/jls/se11/html/jls-15.html#jls-ConditionalExpression
 *
 * <p>Extends {@link JavaStatementBuilder} and implements {@link JavaLambdaBody}.
 * Ported from upstream — replaced {@code StringConcatenationClient}-based rendering
 * with {@link #render(StringBuilder)}.
 *
 * <p>See {@link JavaStatementBuilder} for documentation of the builder pattern.
 */
public class JavaConditionalExpression extends JavaStatementBuilder implements JavaLambdaBody {
    private final JavaExpression condition;
    private final JavaExpression thenBranch;
    private final JavaExpression elseBranch;
    private final JavaType commonType;

    private final JavaTypeUtil typeUtil;

    /**
     * Full constructor with an explicit common type.
     */
    public JavaConditionalExpression(JavaExpression condition, JavaExpression thenBranch,
                                     JavaExpression elseBranch, JavaType commonType,
                                     JavaTypeUtil typeUtil) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
        this.commonType = commonType;
        this.typeUtil = typeUtil;
    }

    /**
     * Convenience constructor — infers the common type as the join of the two branch types.
     */
    public JavaConditionalExpression(JavaExpression condition, JavaExpression thenBranch,
                                     JavaExpression elseBranch, JavaTypeUtil typeUtil) {
        this(condition, thenBranch, elseBranch,
             typeUtil.join(thenBranch.getExpressionType(), elseBranch.getExpressionType()),
             typeUtil);
    }

    /** The ternary's pieces — read-only accessors for seat-level re-composition (facet lambdaCondBlockEveryShape, seat 26). */
    public JavaExpression getConditionExpression() {
        return condition;
    }

    public JavaExpression getThenExpression() {
        return thenBranch;
    }

    public JavaExpression getElseExpression() {
        return elseBranch;
    }

    @Override
    public JavaType getExpressionType() {
        return commonType;
    }

    @Override
    public Set<JavaClass<?>> getRefs() {
        Set<JavaClass<?>> out = new HashSet<>();
        out.addAll(condition.getRefs());
        out.addAll(thenBranch.getRefs());
        out.addAll(elseBranch.getRefs());
        return Set.copyOf(out);
    }

    @Override
    public Set<JavaClass<?>> getStaticWildcardImports() {
        Set<JavaClass<?>> out = new HashSet<>();
        out.addAll(condition.getStaticWildcardImports());
        out.addAll(thenBranch.getStaticWildcardImports());
        out.addAll(elseBranch.getStaticWildcardImports());
        return Set.copyOf(out);
    }

    /**
     * Map the expression value: apply {@code mapper} to both branches.
     * If both results are still {@link JavaExpression}s the result is a new
     * {@link JavaConditionalExpression}; otherwise it upgrades to a
     * {@link JavaIfThenElseBuilder}.
     */
    @Override
    public JavaStatementBuilder mapExpression(
            Function<JavaExpression, ? extends JavaStatementBuilder> mapper) {
        JavaStatementBuilder newThenBranch = mapper.apply(thenBranch);
        JavaStatementBuilder newElseBranch = mapper.apply(elseBranch);
        if (newThenBranch instanceof JavaExpression newThenExpr
                && newElseBranch instanceof JavaExpression newElseExpr) {
            return new JavaConditionalExpression(condition, newThenExpr, newElseExpr, typeUtil);
        } else {
            return new JavaIfThenElseBuilder(condition, newThenBranch, newElseBranch, typeUtil);
        }
    }

    @Override
    public JavaStatementBuilder then(
            JavaStatementBuilder after,
            BiFunction<JavaExpression, JavaExpression, JavaStatementBuilder> combineExpressions,
            JavaStatementScope scope) {
        return this.collapseToSingleExpression(scope)
                .then(after, combineExpressions, scope);
    }

    @Override
    public JavaStatement complete(Function<JavaExpression, JavaStatement> completer) {
        return completer.apply(this.toExpression());
    }

    @Override
    public JavaReturnStatement completeAsReturn() {
        return this.toExpression().completeAsReturn();
    }

    @Override
    public JavaIfThenElseStatement completeAsExpressionStatement() {
        return new JavaIfThenElseBuilder(condition, thenBranch, elseBranch, commonType, typeUtil)
                .completeAsExpressionStatement();
    }

    @Override
    public JavaAssignment completeAsAssignment(GeneratedIdentifier variableId) {
        return this.toExpression().completeAsAssignment(variableId);
    }

    @Override
    public JavaStatementBuilder declareAsVariable(
            boolean isFinal, String variableId, JavaStatementScope scope) {
        JavaExpression expression = this.toExpression();
        JavaStatementBuilder variable = expression.declareAsVariable(isFinal, variableId, scope);
        scope.createKeySynonym(this, expression);
        return variable;
    }

    @Override
    public JavaExpression collapseToSingleExpression(JavaStatementScope scope) {
        // Preserve aggregated refs + staticWildcardImports (getRefs / getStaticWildcardImports
        // union condition/thenBranch/elseBranch) — without this, the legacy
        // 2-arg JavaExpression.from factory would discard them and any
        // subsequent consumer reading refs on the collapsed expression would
        // lose imports. Copilot round 8 finding.
        return JavaExpression.from(
                "(" + this.renderToString() + ")",
                commonType,
                this.getRefs(),
                this.getStaticWildcardImports());
    }

    @Override
    public JavaLambdaBody toLambdaBody() {
        return this;
    }

    /**
     * Render the ternary expression to a {@link StringBuilder}.
     * Format: {@code condition ? thenBranch : elseBranch}
     */
    @Override
    public void render(StringBuilder sb) {
        condition.render(sb);
        sb.append(" ? ");
        thenBranch.render(sb);
        sb.append(" : ");
        elseBranch.render(sb);
    }

    /** Render to a plain string (used by {@link #collapseToSingleExpression}). */
    private String renderToString() {
        var sb = new StringBuilder();
        render(sb);
        return sb.toString();
    }

    /**
     * Wrap this ternary in a {@link JavaExpression} (for use as a simple expression
     * where a ternary is valid). Carries aggregated refs + staticWildcardImports
     * from all three sub-expressions so downstream consumers (then / invokeMethod /
     * import collection) never lose required imports. Copilot round 8 finding.
     *
     * <p>Public since PR #331 (facet existsMetaAsMapper): the compileLambda drain
     * site needs the BARE ternary form ({@code cond ? a : b}, golden's
     * {@code return}-position rendering) as a block end-expression —
     * {@link #collapseToSingleExpression} adds the parentheses golden only uses
     * in method-chain position.
     */
    public JavaExpression toExpression() {
        return JavaExpression.from(
                renderToString(),
                commonType,
                this.getRefs(),
                this.getStaticWildcardImports());
    }

    @Override
    public String toString() {
        return renderToString();
    }
}
