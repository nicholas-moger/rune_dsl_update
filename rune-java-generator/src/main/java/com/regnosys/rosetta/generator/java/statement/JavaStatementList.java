package com.regnosys.rosetta.generator.java.statement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.rosetta.util.types.JavaClass;

public class JavaStatementList extends JavaStatement {
    private final List<JavaStatement> statements;

    private JavaStatementList(List<JavaStatement> statements) {
        this.statements = statements;
    }

    public static JavaStatementList of(JavaStatement... statements) {
        return new JavaStatementList(List.of(statements));
    }

    public static JavaStatementList of(List<JavaStatement> statements) {
        return new JavaStatementList(List.copyOf(statements));
    }

    public List<JavaStatement> getStatements() {
        return statements;
    }

    @Override
    public void render(StringBuilder sb) {
        for (var stmt : statements) {
            stmt.render(sb);
        }
    }

    @Override
    public Set<JavaClass<?>> getRefs() {
        Set<JavaClass<?>> out = new HashSet<>();
        for (JavaStatement s : statements) {
            out.addAll(s.getRefs());
        }
        return Set.copyOf(out);
    }

    @Override
    public Set<JavaClass<?>> getStaticWildcardImports() {
        Set<JavaClass<?>> out = new HashSet<>();
        for (JavaStatement s : statements) {
            out.addAll(s.getStaticWildcardImports());
        }
        return Set.copyOf(out);
    }
}
