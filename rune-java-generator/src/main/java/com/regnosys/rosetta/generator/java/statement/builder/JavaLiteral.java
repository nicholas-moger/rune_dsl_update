package com.regnosys.rosetta.generator.java.statement.builder;

import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaPrimitiveType;
import com.rosetta.util.types.JavaReferenceType;

/**
 * A literal value expression. Includes well-known singletons.
 */
public class JavaLiteral extends JavaExpression {
    public static final JavaLiteral NULL = new JavaLiteral("null", JavaReferenceType.NULL_TYPE);
    public static final JavaLiteral TRUE = new JavaLiteral("true", JavaPrimitiveType.BOOLEAN);
    public static final JavaLiteral FALSE = new JavaLiteral("false", JavaPrimitiveType.BOOLEAN);

    private final String literal;

    public JavaLiteral(String literal, com.rosetta.util.types.JavaType type) {
        super(type);
        this.literal = literal;
    }

    @Override
    public void render(StringBuilder sb) {
        sb.append(literal);
    }
}
