package com.regnosys.rosetta.generator;

import com.regnosys.rosetta.ast.RNode;

/**
 * Exception thrown during Java code generation. Carries the source file URI
 * and optional AST context node for diagnostic reporting.
 */
public class GenerationException extends RuntimeException {
    private static final long serialVersionUID = -6542373098869340042L;
    private final String resourceUri;
    private final RNode context;

    /**
     * Relative output path of the Java file this failure was producing, in the
     * KNOWN_DIVERGENT waiver-key format ({@code <namespace-path>/<Name>.java},
     * forward-slashed). Populated by {@link JavaClassGenerator#generateClasses}
     * from the target type representation, which is known before the body
     * emission that may throw. {@code null} when the failure occurred before the
     * target path could be determined (e.g. building the type representation
     * itself failed). Lets the D11 regression distinguish a generation failure
     * for an already-waivered (known-incomplete) element from a new/unexpected
     * one. See the development audit "codegen-completeness-audit-2026-05-27".
     */
    private String targetPath;

    public GenerationException(String message, String resourceUri, RNode context) {
        super(message);
        this.resourceUri = resourceUri;
        this.context = context;
    }

    public GenerationException(String message, String resourceUri, RNode context, Throwable cause) {
        super(message, cause);
        this.resourceUri = resourceUri;
        this.context = context;
    }

    public RNode getContext() {
        return context;
    }

    public String getResourceUri() {
        return resourceUri;
    }

    /** @return the target output path (waiver-key format), or {@code null} if unknown. */
    public String getTargetPath() {
        return targetPath;
    }

    /**
     * Records the target output path. Set once by the generation framework at the
     * point a per-object failure is captured; harmless to call when already set to
     * the same value. Does not overwrite a non-null path with {@code null}.
     */
    public void setTargetPath(String targetPath) {
        if (targetPath != null) {
            this.targetPath = targetPath;
        }
    }

    @Override
    public String toString() {
        return "GenerationException [resourceUri=" + resourceUri + ", context=" + context
                + ", getMessage()=" + getMessage() + "]";
    }
}
