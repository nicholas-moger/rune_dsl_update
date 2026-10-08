package com.regnosys.rosetta.generator.java.statement;

import java.util.Set;

import com.rosetta.util.types.JavaClass;

/**
 * A code block {@code { ... }} containing a list of statements.
 */
public class JavaBlock extends JavaStatement {
    private final JavaStatementList body;

    public JavaBlock(JavaStatementList body) {
        this.body = body;
    }

    @Override
    public void render(StringBuilder sb) {
        sb.append("{\n");
        body.render(sb);
        sb.append("}\n");
    }

    @Override
    public Set<JavaClass<?>> getRefs() {
        return body.getRefs();
    }

    @Override
    public Set<JavaClass<?>> getStaticWildcardImports() {
        return body.getStaticWildcardImports();
    }
}
