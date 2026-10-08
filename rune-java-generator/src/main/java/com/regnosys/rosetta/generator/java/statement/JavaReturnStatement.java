package com.regnosys.rosetta.generator.java.statement;

import java.util.Objects;
import java.util.Set;

import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.rosetta.util.types.JavaClass;

public class JavaReturnStatement extends JavaStatement {
    private final JavaExpression expression;

    public JavaReturnStatement(JavaExpression expression) {
        this.expression = Objects.requireNonNull(expression, "expression");
    }

    @Override
    public void render(StringBuilder sb) {
        sb.append("return ");
        expression.render(sb);
        sb.append(";\n");
    }

    @Override
    public Set<JavaClass<?>> getRefs() {
        return expression.getRefs();
    }

    @Override
    public Set<JavaClass<?>> getStaticWildcardImports() {
        return expression.getStaticWildcardImports();
    }
}
