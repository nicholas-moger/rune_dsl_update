package com.regnosys.rosetta.ast.builder;

/**
 * Runtime exception thrown when the AST builder encounters an unrecoverable
 * error while converting a parse tree to a typed AST.
 *
 * <p>This typically indicates a structural mismatch between the grammar and
 * the builder logic (i.e., a bug), not a user-facing parse error.
 */
public class AstBuildException extends RuntimeException {

    public AstBuildException(String message) {
        super(message);
    }

    public AstBuildException(String message, Throwable cause) {
        super(message, cause);
    }
}
