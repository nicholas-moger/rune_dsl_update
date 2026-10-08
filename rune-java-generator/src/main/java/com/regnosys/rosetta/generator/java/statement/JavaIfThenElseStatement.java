package com.regnosys.rosetta.generator.java.statement;

import java.util.HashSet;
import java.util.Set;

import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.rosetta.util.types.JavaClass;

/**
 * An {@code if (cond) { ... } else { ... }} statement.
 * Handles else-if chaining: if the else branch is itself an
 * if-then-else, it renders as {@code } else if (...)} without
 * double braces (D11 requirement).
 */
public class JavaIfThenElseStatement extends JavaStatement {
    private final JavaExpression condition;
    private final JavaStatement thenBranch;
    private final JavaStatement elseBranch;

    public JavaIfThenElseStatement(JavaExpression condition,
                                    JavaStatement thenBranch,
                                    JavaStatement elseBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    @Override
    public void render(StringBuilder sb) {
        sb.append("if (");
        condition.render(sb);
        sb.append(") ");
        sb.append(thenBranch.toBlock());
        if (elseBranch instanceof JavaIfThenElseStatement) {
            // else-if chain: render without double braces
            sb.append(" else ");
            elseBranch.render(sb);
        } else {
            sb.append(" else ");
            sb.append(elseBranch.toBlock());
            sb.append("\n");
        }
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
}
