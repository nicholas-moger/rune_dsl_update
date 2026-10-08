package com.regnosys.rosetta.generator.java.statement;

import java.util.Set;

import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.rosetta.util.types.JavaClass;

public class JavaExpressionStatement extends JavaStatement {
    private final JavaExpression expression;

    public JavaExpressionStatement(JavaExpression expression) {
        this.expression = expression;
    }

    @Override
    public void render(StringBuilder sb) {
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
