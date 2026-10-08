package com.regnosys.rosetta.generator.java.statement.builder;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.regnosys.rosetta.generator.GeneratedIdentifier;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.JavaIfThenElseStatement;
import com.regnosys.rosetta.generator.java.statement.JavaIfThenStatement;
import com.regnosys.rosetta.generator.java.statement.JavaLambdaBody;
import com.regnosys.rosetta.generator.java.statement.JavaLocalVariableDeclarationStatement;
import com.regnosys.rosetta.generator.java.statement.JavaStatement;
import com.regnosys.rosetta.generator.java.statement.JavaStatementList;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;

/**
 * A branching statement builder representing an if-then-else where each branch
 * may contain its own sub-statements and ending expression.
 *
 * <p>Based on the Java specification:
 * https://docs.oracle.com/javase/specs/jls/se11/html/jls-14.html#jls-IfThenElseStatement
 *
 * <p>Example:
 * <pre>
 * if (cond) {
 *     int x = 42;
 *     x
 * } else {
 *     -1
 * }
 * </pre>
 *
 * <p>Ported from upstream — replaced {@code StringConcatenationClient}-based rendering
 * with {@link StringBuilder}-based rendering. Removed Guice {@code @Inject}.
 *
 * <p>See {@link JavaStatementBuilder} for documentation of the builder pattern.
 */
public class JavaIfThenElseBuilder extends JavaStatementBuilder {
    private final JavaExpression condition;
    private final JavaStatementBuilder thenBranch;
    private final JavaStatementBuilder elseBranch;
    private final JavaType commonType;

    private final JavaTypeUtil typeUtil;

    /**
     * Full constructor with explicit common type.
     */
    public JavaIfThenElseBuilder(JavaExpression condition, JavaStatementBuilder thenBranch,
                                  JavaStatementBuilder elseBranch, JavaType commonType,
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
    public JavaIfThenElseBuilder(JavaExpression condition, JavaStatementBuilder thenBranch,
                                  JavaStatementBuilder elseBranch, JavaTypeUtil typeUtil) {
        this(condition, thenBranch, elseBranch,
             typeUtil.join(thenBranch.getExpressionType(), elseBranch.getExpressionType()),
             typeUtil);
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

    @Override
    public JavaIfThenElseBuilder mapExpression(
            Function<JavaExpression, ? extends JavaStatementBuilder> mapper) {
        JavaStatementBuilder mappedThenBranch = thenBranch.mapExpression(mapper);
        JavaStatementBuilder mappedElseBranch = elseBranch.mapExpression(mapper);
        return new JavaIfThenElseBuilder(condition, mappedThenBranch, mappedElseBranch, typeUtil);
    }

    @Override
    public JavaStatementBuilder then(
            JavaStatementBuilder after,
            BiFunction<JavaExpression, JavaExpression, JavaStatementBuilder> combineExpressions,
            JavaStatementScope scope) {
        return this.collapseToSingleExpression(scope)
                .then(after, combineExpressions, scope);
    }

    private JavaIfThenElseStatement completeBranches(
            Function<JavaStatementBuilder, JavaStatement> mapper) {
        return new JavaIfThenElseStatement(condition,
                mapper.apply(thenBranch), mapper.apply(elseBranch));
    }

    @Override
    public JavaIfThenElseStatement complete(Function<JavaExpression, JavaStatement> completer) {
        return completeBranches(b -> b.complete(completer));
    }

    @Override
    public JavaStatement completeAsReturn() {
        return new JavaIfThenStatement(condition, thenBranch.completeAsReturn())
                .append(elseBranch.completeAsReturn());
    }

    @Override
    public JavaIfThenElseStatement completeAsExpressionStatement() {
        return completeBranches(JavaStatementBuilder::completeAsExpressionStatement);
    }

    @Override
    public JavaIfThenElseStatement completeAsAssignment(GeneratedIdentifier variableId) {
        return completeBranches(b -> b.completeAsAssignment(variableId));
    }

    @Override
    public JavaBlockBuilder declareAsVariable(
            boolean isFinal, String variableId, JavaStatementScope scope) {
        GeneratedIdentifier id = scope.createIdentifier(this, variableId);
        if (elseBranch instanceof JavaLiteral elseLiteral) {
            return new JavaBlockBuilder(
                    JavaStatementList.of(
                            new JavaLocalVariableDeclarationStatement(
                                    false, commonType, id, elseLiteral),
                            new JavaIfThenStatement(condition, thenBranch.completeAsAssignment(id))
                    ),
                    new JavaVariable(id, commonType)
            );
        }
        return new JavaBlockBuilder(
                JavaStatementList.of(
                        new JavaLocalVariableDeclarationStatement(isFinal, commonType, id),
                        this.completeAsAssignment(id)
                ),
                new JavaVariable(id, commonType)
        );
    }

    @Override
    public JavaStatementBuilder collapseToSingleExpression(JavaStatementScope scope) {
        return this.declareAsVariable(true, "ifThenElseResult", scope);
    }

    @Override
    public JavaLambdaBody toLambdaBody() {
        // Delegate to JavaBlockBuilder so that branches with leading statements
        // (e.g. a JavaBlockBuilder branch) are rendered correctly via completeAsReturn().
        // Matches upstream: new JavaBlockBuilder(this).toLambdaBody()
        return new JavaBlockBuilder(this).toLambdaBody();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("if (");
        condition.render(sb);
        sb.append(") ");
        sb.append(renderBlock(thenBranch));
        sb.append(" else ");
        if (elseBranch instanceof JavaIfThenElseBuilder elseIf) {
            sb.append(elseIf);
        } else {
            sb.append(renderBlock(elseBranch));
        }
        return sb.toString();
    }

    /**
     * Render a branch wrapped in curly braces (style preference: always use braces).
     * If the branch is already a {@link JavaBlockBuilder}, delegate to it; otherwise
     * wrap manually.
     *
     * <p><b>Note:</b> The single-tab indentation in the fallback path is intentional for
     * debug/toString output only. Actual code generation goes through the {@code complete*}
     * methods (e.g. {@link #completeAsReturn()}, {@link #completeAsAssignment}), not this
     * method.
     */
    private String renderBlock(JavaStatementBuilder stat) {
        if (stat instanceof JavaBlockBuilder block) {
            return block.toString();
        }
        return "{\n\t" + stat + "\n}";
    }
}
