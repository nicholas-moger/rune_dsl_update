package com.regnosys.rosetta.generator.java.statement;

import java.util.Set;

import com.rosetta.util.types.JavaClass;

/**
 * A rendered Java statement. The base class for all concrete statements
 * (return, assignment, block, if-then-else, etc.).
 *
 * <p>Replaces upstream's {@code StringConcatenationClient}-based rendering
 * with {@link #render(StringBuilder)} and {@link #toBlock()} methods.
 *
 * <p>Statements are produced by "completing" a {@link
 * com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder
 * JavaStatementBuilder} (e.g., {@code builder.completeAsReturn()}).
 */
public abstract class JavaStatement {

    /**
     * Render this statement to a StringBuilder. Does NOT include
     * indentation — the caller (CodeWriter) manages indentation.
     */
    public abstract void render(StringBuilder sb);

    /**
     * Library and domain classes this statement's rendered source references.
     * Mirrors {@code JavaStatementBuilder.getRefs()} — refs travel with the
     * statement through the rendering pipeline to feed {@code ImportCollector}
     * without string scanning.
     */
    public abstract Set<JavaClass<?>> getRefs();

    /**
     * Static wildcard imports this statement's rendered source depends on.
     * See {@code JavaStatementBuilder.getStaticWildcardImports()} for the
     * v6.2 rationale.
     */
    public abstract Set<JavaClass<?>> getStaticWildcardImports();

    /**
     * Render this statement wrapped in a block: {@code { ... }}.
     */
    public String toBlock() {
        var sb = new StringBuilder();
        sb.append("{\n");
        render(sb);
        sb.append("}");
        return sb.toString();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        render(sb);
        return sb.toString();
    }
}
