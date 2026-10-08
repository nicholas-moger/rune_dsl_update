package com.regnosys.rosetta.generator.java.statement;

import java.util.Set;

import com.regnosys.rosetta.generator.GeneratedIdentifier;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;

public class JavaLocalVariableDeclarationStatement extends JavaStatement {
    private final boolean isFinal;
    private final JavaType type;
    private final GeneratedIdentifier variableId;
    private final JavaExpression initializer;

    public JavaLocalVariableDeclarationStatement(boolean isFinal, JavaType type,
                                                  GeneratedIdentifier variableId,
                                                  JavaExpression initializer) {
        this.isFinal = isFinal;
        this.type = type;
        this.variableId = variableId;
        this.initializer = initializer;
    }

    /** Declare a variable with no initializer. */
    public JavaLocalVariableDeclarationStatement(boolean isFinal, JavaType type,
                                                  GeneratedIdentifier variableId) {
        this(isFinal, type, variableId, null);
    }

    /**
     * The declared variable type. Exposed (PR #301) so a renderer can scope a pending-lambda-hoist
     * drain by the hoist's kind (e.g. {@code CollectionHandler.compileElselessConditionalBlock} drains
     * only the numeric Integer/BigInteger&rarr;BigDecimal coercion into the if-branch, declining a
     * meta-deref wrapper hoist) — without rendering it (which would call {@code getActualName} and
     * close the scope prematurely).
     */
    public JavaType getDeclaredType() {
        return type;
    }

    @Override
    public void render(StringBuilder sb) {
        if (isFinal) sb.append("final ");
        sb.append(type.getSimpleName()).append(' ');
        sb.append(variableId.getActualName());
        if (initializer != null) {
            sb.append(" = ");
            initializer.render(sb);
        }
        sb.append(";\n");
    }

    @Override
    public Set<JavaClass<?>> getRefs() {
        return initializer == null ? Set.of() : initializer.getRefs();
    }

    @Override
    public Set<JavaClass<?>> getStaticWildcardImports() {
        return initializer == null ? Set.of() : initializer.getStaticWildcardImports();
    }
}
