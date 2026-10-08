package com.regnosys.rosetta.generator.java.function;

import com.rosetta.util.types.JavaClass;

import java.util.Set;

/**
 * A rendered Java source fragment paired with the set of Java classes the
 * fragment references. Produced by {@link FunctionExpressionRenderer} and
 * consumed by {@code FunctionGenerator} to build the import block via
 * structured ref-propagation rather than regex-over-joined-string (PR-A,
 * D38-F regression-safety, master doc §9.1).
 *
 * <p>{@code refs} holds referenced library and domain Java classes (e.g.
 * {@code MapperS}, {@code Instruction}, {@code BigDecimal}). Callers feed
 * each class's {@link JavaClass#getCanonicalName()}{@code .withDots()} to
 * {@link com.regnosys.rosetta.generator.java.template.ImportCollector#addImport(String)}.
 *
 * <p><strong>Immutability contract:</strong> the constructor defensively
 * copies {@code refs} into an unmodifiable set ({@link Set#copyOf}), so the
 * {@link #refs()} accessor returns a read-only view. Callers that mutate
 * their source set after construction do NOT observe a change on this
 * record. {@link #refs()} throws {@link UnsupportedOperationException} on
 * any mutation call (asserted by {@code RenderedStatementTest}).
 */
public record RenderedStatement(
        String source,
        Set<JavaClass<?>> refs,
        Set<JavaClass<?>> staticWildcardImports) {

    public RenderedStatement(String source,
                             Set<JavaClass<?>> refs,
                             Set<JavaClass<?>> staticWildcardImports) {
        this.source = java.util.Objects.requireNonNull(source, "source");
        this.refs = Set.copyOf(java.util.Objects.requireNonNull(refs, "refs"));
        this.staticWildcardImports = Set.copyOf(
                java.util.Objects.requireNonNull(staticWildcardImports, "staticWildcardImports"));
    }

    /**
     * Backward-compat 2-arg constructor. Defaults staticWildcardImports to
     * empty. Introduced in C3c.1 when the structured channel gained
     * staticWildcardImports support; legacy callers continue to work.
     */
    public RenderedStatement(String source, Set<JavaClass<?>> refs) {
        this(source, refs, Set.of());
    }

    /** Factory for callers that have no refs to attach. */
    public static RenderedStatement empty(String source) {
        return new RenderedStatement(source, Set.of(), Set.of());
    }
}
