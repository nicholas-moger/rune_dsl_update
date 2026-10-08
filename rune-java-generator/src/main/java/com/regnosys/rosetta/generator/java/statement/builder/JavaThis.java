package com.regnosys.rosetta.generator.java.statement.builder;

import com.rosetta.util.types.JavaType;

/**
 * The {@code this} reference expression.
 */
public class JavaThis extends JavaExpression {
    public JavaThis(JavaType type) {
        super(type);
    }

    @Override
    public void render(StringBuilder sb) {
        sb.append("this");
    }
}
