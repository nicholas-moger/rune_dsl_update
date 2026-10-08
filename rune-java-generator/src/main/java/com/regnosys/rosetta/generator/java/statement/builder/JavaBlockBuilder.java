package com.regnosys.rosetta.generator.java.statement.builder;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.regnosys.rosetta.generator.GeneratedIdentifier;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.JavaLambdaBody;
import com.regnosys.rosetta.generator.java.statement.JavaStatement;
import com.regnosys.rosetta.generator.java.statement.JavaStatementList;
import com.regnosys.rosetta.generator.java.statement.JavaLocalVariableDeclarationStatement;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A builder that prepends statements before an ending expression (or statement builder).
 * E.g., variable declaration + return.
 *
 * <p>The {@code lastStatement} field holds the "ending" builder — in the common case this
 * is a {@link JavaExpression}, but it may also be a {@link JavaIfThenElseBuilder} when the
 * block wraps an if-then-else for lambda-body purposes (matching upstream's
 * {@code new JavaBlockBuilder(ifThenElse).toLambdaBody()} pattern).
 */
public class JavaBlockBuilder extends JavaStatementBuilder {
    private final JavaStatementList leadingStatements;
    /** The last/ending statement builder; most callers expect this to be a {@link JavaExpression}. */
    private final JavaStatementBuilder lastStatement;

    /**
     * Wraps a single {@link JavaStatementBuilder} with no leading statements.
     * Used by {@link JavaIfThenElseBuilder#toLambdaBody()} to delegate lambda-body
     * rendering to this class (matching upstream's {@code new JavaBlockBuilder(this).toLambdaBody()}).
     */
    public JavaBlockBuilder(JavaStatementBuilder lastStatement) {
        this(JavaStatementList.of(), lastStatement);
    }

    /**
     * Primary constructor: leading statements followed by an expression as the last element.
     */
    public JavaBlockBuilder(JavaStatementList leadingStatements, JavaExpression endExpression) {
        this(leadingStatements, (JavaStatementBuilder) endExpression);
    }

    private JavaBlockBuilder(JavaStatementList leadingStatements, JavaStatementBuilder lastStatement) {
        this.leadingStatements = leadingStatements;
        this.lastStatement = lastStatement;
    }

    /**
     * Return the last statement as a {@link JavaExpression}, throwing if it is not one.
     * Most operations require the last statement to be an expression.
     */
    private JavaExpression endExpression() {
        if (lastStatement instanceof JavaExpression expr) {
            return expr;
        }
        throw new IllegalStateException(
                "JavaBlockBuilder.endExpression() called but lastStatement is not a JavaExpression: "
                + lastStatement.getClass().getSimpleName()
                + ". Use this JavaBlockBuilder only for toLambdaBody() when constructed with a non-expression.");
    }

    /**
     * The statements prepended before the ending expression (e.g. a hoisted
     * coercion local {@code final BigInteger bigInteger = …;}). Exposed so a
     * statement-level renderer (e.g.
     * {@code FunctionExpressionRenderer.renderThenExtractSet}) can LIFT the hoist
     * out of the block to a line before its {@code output =} assignment rather than
     * collapse the whole block into a single expression.
     */
    public JavaStatementList getLeadingStatements() {
        return leadingStatements;
    }

    /**
     * The block's ending expression (the trailing value after the leading
     * statements). Throws if the last element is not a {@link JavaExpression}
     * (see {@link #endExpression()}). Pairs with {@link #getLeadingStatements()}
     * for the statement-level hoist-lift described there.
     */
    public JavaExpression getEndExpression() {
        return endExpression();
    }

    /**
     * The last statement as a {@link JavaExpression}, or {@code null} when it is not one —
     * the non-throwing probe for callers that must DECLINE on a non-expression tail instead
     * of erroring (facet lambdaCondBlockEveryShape, seat 26).
     */
    public JavaExpression endExpressionOrNull() {
        return lastStatement instanceof JavaExpression expr ? expr : null;
    }

    @Override
    public JavaType getExpressionType() {
        return lastStatement.getExpressionType();
    }

    @Override
    public Set<JavaClass<?>> getRefs() {
        Set<JavaClass<?>> out = new HashSet<>();
        out.addAll(leadingStatements.getRefs());
        out.addAll(lastStatement.getRefs());
        return Set.copyOf(out);
    }

    @Override
    public Set<JavaClass<?>> getStaticWildcardImports() {
        Set<JavaClass<?>> out = new HashSet<>();
        out.addAll(leadingStatements.getStaticWildcardImports());
        out.addAll(lastStatement.getStaticWildcardImports());
        return Set.copyOf(out);
    }

    /**
     * Complete the ending builder with the given mapper and prepend the leading
     * statements — upstream's {@code completeLastStatement} delegation. Byte-identical
     * for expression ends (delegating to a {@link JavaExpression} applies the completer
     * directly); a branching end (e.g. a {@link JavaIfThenElseBuilder} produced by the
     * PR #412 POJO compat coercions) completes per-branch instead of throwing via
     * {@code endExpression()}.
     */
    private JavaStatement completeLastStatement(
            Function<JavaStatementBuilder, JavaStatement> mapper) {
        List<JavaStatement> all = new ArrayList<>(leadingStatements.getStatements());
        all.add(mapper.apply(lastStatement));
        return JavaStatementList.of(all);
    }

    @Override
    public JavaStatement complete(Function<JavaExpression, JavaStatement> completer) {
        return completeLastStatement(l -> l.complete(completer));
    }

    @Override
    public JavaStatement completeAsReturn() {
        return completeLastStatement(JavaStatementBuilder::completeAsReturn);
    }

    @Override
    public JavaStatement completeAsExpressionStatement() {
        return completeLastStatement(JavaStatementBuilder::completeAsExpressionStatement);
    }

    @Override
    public JavaStatement completeAsAssignment(GeneratedIdentifier variableId) {
        return completeLastStatement(l -> l.completeAsAssignment(variableId));
    }

    @Override
    public JavaStatementBuilder mapExpression(
            Function<JavaExpression, ? extends JavaStatementBuilder> mapper) {
        // Delegate the mapping to the ending builder (a plain expression applies the
        // mapper directly; a conditional/if-else distributes it into its branches),
        // then re-attach this block's leading statements. The pre-#412 code returned
        // the mapped result BARE in the general case (mapper yields a
        // JavaConditionalExpression / JavaIfThenElseBuilder), silently DROPPING the
        // leading statements — e.g. a hoisted coercion local — a latent bug with zero
        // function-pipeline witnesses (no corpus path maps a block into a branching
        // builder), first exercised by the POJO builder-compat coercions (PR #412:
        // the meta→meta unwrap local `final GrandChild grandChild = …` followed by a
        // branching re-wrap). The two previously-handled shapes are byte-identical:
        // expression→expression keeps (leading, mappedExpr); expression→block merges
        // the statement lists in the same order.
        JavaStatementBuilder mapped = lastStatement.mapExpression(mapper);
        if (mapped instanceof JavaBlockBuilder mappedBlock) {
            List<JavaStatement> combined = new ArrayList<>(leadingStatements.getStatements());
            combined.addAll(mappedBlock.leadingStatements.getStatements());
            return new JavaBlockBuilder(JavaStatementList.of(combined), mappedBlock.lastStatement);
        }
        return new JavaBlockBuilder(leadingStatements, mapped);
    }

    @Override
    public JavaStatementBuilder declareAsVariable(
            boolean isFinal, String variableId, JavaStatementScope scope) {
        // Upstream JavaBlockBuilder.declareAsVariable: delegate to the ending builder
        // and re-attach the leading statements (a branching end declares via its own
        // ifThenElseResult-style path). Byte-identical for the expression-ended case
        // (the ending expression binds to a fresh local exactly as before); the
        // branching-ended case previously threw via endExpression() — first exercised
        // by the POJO builder-compat coercions (PR #412).
        JavaStatementBuilder declared = lastStatement.declareAsVariable(isFinal, variableId, scope);
        if (declared instanceof JavaBlockBuilder declaredBlock) {
            List<JavaStatement> combined = new ArrayList<>(leadingStatements.getStatements());
            combined.addAll(declaredBlock.leadingStatements.getStatements());
            return new JavaBlockBuilder(JavaStatementList.of(combined),
                    declaredBlock.lastStatement);
        }
        return new JavaBlockBuilder(leadingStatements, declared);
    }

    @Override
    public JavaStatementBuilder collapseToSingleExpression(JavaStatementScope scope) {
        // Upstream JavaBlockBuilder.collapseToSingleExpression: delegate to the ending
        // builder and re-attach the leading statements. An expression end collapses to
        // itself (upstream introduces NO synthetic local — the previous fork version
        // bound a `blockResult` local here, a form no corpus golden carries, so the
        // path was provably unreached); a branching end collapses via its own
        // `ifThenElseResult` law.
        JavaStatementBuilder collapsed = lastStatement.collapseToSingleExpression(scope);
        if (collapsed instanceof JavaBlockBuilder collapsedBlock) {
            List<JavaStatement> combined = new ArrayList<>(leadingStatements.getStatements());
            combined.addAll(collapsedBlock.leadingStatements.getStatements());
            return new JavaBlockBuilder(JavaStatementList.of(combined),
                    collapsedBlock.lastStatement);
        }
        return new JavaBlockBuilder(leadingStatements, collapsed);
    }

    @Override
    public JavaStatementBuilder then(
            JavaStatementBuilder after,
            BiFunction<JavaExpression, JavaExpression, JavaStatementBuilder> combineExpressions,
            JavaStatementScope scope) {
        // Upstream JavaBlockBuilder.then: delegate to the ending builder and re-attach
        // the leading statements (Copilot #412 R1: the same recursive-delegation
        // contract as mapExpression/collapse/declareAsVariable/complete* — a branching
        // end combines per-branch instead of throwing via endExpression()). For the
        // expression-ended case this is byte-identical to the previous combine
        // (JavaExpression.then applies the combiner directly); the previous fallback
        // minted a `blockResult` local no corpus golden carries — provably unreached.
        JavaStatementBuilder combined = lastStatement.then(after, combineExpressions, scope);
        if (combined instanceof JavaBlockBuilder combinedBlock) {
            List<JavaStatement> merged = new ArrayList<>(leadingStatements.getStatements());
            merged.addAll(combinedBlock.leadingStatements.getStatements());
            return new JavaBlockBuilder(JavaStatementList.of(merged),
                    combinedBlock.lastStatement);
        }
        return new JavaBlockBuilder(leadingStatements, combined);
    }

    @Override
    public JavaLambdaBody toLambdaBody() {
        // Delegate to completeAsReturn() so non-expression last statements (e.g.
        // JavaIfThenElseBuilder) are rendered correctly via their own completeAsReturn().
        // A statement-carrying lambda body renders BRACED — upstream's toLambdaBody
        // returns the JavaBlock form. In the fork's flat-render-then-reindent channel
        // (PojoCompatEmitter.renderBody: base + brace-depth per line, embedded tabs
        // preserved AFTER the computed prefix) that shape is: '{' + newline, each
        // statement line carrying ONE embedded tab (the stream-continuation level the
        // enclosing '\n\t' phrase sits at), and the close as '\t}' with NO trailing
        // newline — the phrase continues right after the brace (the golden `})`), and
        // the tab-tolerant dedent puts it back AT the continuation level. First
        // golden witnesses: the pojo-bulk-value-narrow compat arms (PR #422).
        JavaStatement completed = completeAsReturn();
        return sb -> {
            StringBuilder body = new StringBuilder();
            completed.render(body);
            sb.append("{\n");
            body.toString().lines().forEach(line -> sb.append('\t').append(line).append('\n'));
            sb.append("\t}");
        };
    }
}
