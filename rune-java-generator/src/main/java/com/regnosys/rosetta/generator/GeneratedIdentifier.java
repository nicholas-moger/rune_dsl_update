package com.regnosys.rosetta.generator;

import java.util.NoSuchElementException;

/**
 * A generated identifier tracked within a {@link GeneratorScope}. The
 * identifier has a <em>desired name</em> which may be adjusted by the scope
 * to avoid clashes (appending numeric suffixes or escaping reserved words).
 *
 * <p>The actual name is resolved lazily when first requested, triggering
 * scope closure and name computation.
 *
 * <p>Ported from upstream — replaced {@code TargetStringConcatenation} with
 * direct {@link #getActualName()} usage. Generators call
 * {@code getActualName()} when emitting code via {@link
 * com.regnosys.rosetta.generator.java.CodeWriter CodeWriter}.
 */
public class GeneratedIdentifier {
    protected final GeneratorScope<?> scope;
    private final String desiredName;

    public GeneratedIdentifier(GeneratorScope<?> scope, String desiredName) {
        this.scope = scope;
        this.desiredName = desiredName;
    }

    public String getDesiredName() {
        return this.desiredName;
    }

    /**
     * Resolve the actual name assigned by the scope. Closes the scope if
     * it is still open.
     */
    public String getActualName() {
        return this.scope.getActualName(this)
                .orElseThrow(() -> new NoSuchElementException(
                        "No actual name for " + this + " in scope.\n" + scope));
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " (desired name=\"" + desiredName + "\")";
    }
}
