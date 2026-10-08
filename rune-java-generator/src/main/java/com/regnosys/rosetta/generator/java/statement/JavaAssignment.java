package com.regnosys.rosetta.generator.java.statement;

import java.util.Set;

import com.regnosys.rosetta.generator.GeneratedIdentifier;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.rosetta.util.types.JavaClass;

public class JavaAssignment extends JavaStatement {
    private final GeneratedIdentifier variableId;
    private final JavaExpression expression;

    public JavaAssignment(GeneratedIdentifier variableId, JavaExpression expression) {
        this.variableId = variableId;
        this.expression = expression;
    }

    @Override
    public void render(StringBuilder sb) {
        sb.append(variableId.getActualName()).append(" = ");
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
