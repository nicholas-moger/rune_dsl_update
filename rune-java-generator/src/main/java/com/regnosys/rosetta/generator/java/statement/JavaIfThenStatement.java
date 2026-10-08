package com.regnosys.rosetta.generator.java.statement;

import java.util.HashSet;
import java.util.Set;

import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.rosetta.util.types.JavaClass;

/**
 * An {@code if (cond) { ... }} statement (no else branch).
 *
 * <p>Ported from upstream — replaced {@code StringConcatenationClient}-based
 * rendering with {@link #render(StringBuilder)}.
 *
 * <p>The then-branch is always wrapped in curly braces (style preference).
 */
public class JavaIfThenStatement extends JavaStatement {
    private final JavaExpression condition;
    private final JavaStatement thenBranch;

    public JavaIfThenStatement(JavaExpression condition, JavaStatement thenBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
    }

    @Override
    public void render(StringBuilder sb) {
        sb.append("if (");
        condition.render(sb);
        sb.append(") ");
        sb.append(thenBranch.toBlock());
        sb.append("\n");
    }

    /**
     * Append another statement after this if-then block, producing a
     * {@link JavaStatementList}.
     */
    public JavaStatementList append(JavaStatement next) {
        return JavaStatementList.of(this, next);
    }

    @Override
    public Set<JavaClass<?>> getRefs() {
        Set<JavaClass<?>> out = new HashSet<>();
        out.addAll(condition.getRefs());
        out.addAll(thenBranch.getRefs());
        return Set.copyOf(out);
    }

    @Override
    public Set<JavaClass<?>> getStaticWildcardImports() {
        Set<JavaClass<?>> out = new HashSet<>();
        out.addAll(condition.getStaticWildcardImports());
        out.addAll(thenBranch.getStaticWildcardImports());
        return Set.copyOf(out);
    }
}
