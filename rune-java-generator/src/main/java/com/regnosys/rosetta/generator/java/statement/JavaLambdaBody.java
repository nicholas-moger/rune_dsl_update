package com.regnosys.rosetta.generator.java.statement;

/**
 * Marker interface for things that can serve as a lambda body:
 * either a single expression or a block of statements.
 */
public interface JavaLambdaBody {
    void render(StringBuilder sb);
}
