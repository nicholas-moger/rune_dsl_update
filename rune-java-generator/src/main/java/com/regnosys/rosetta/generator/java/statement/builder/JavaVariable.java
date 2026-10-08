package com.regnosys.rosetta.generator.java.statement.builder;

import com.regnosys.rosetta.generator.GeneratedIdentifier;
import com.rosetta.util.types.JavaType;

/**
 * A variable reference expression. Renders as the variable's actual name.
 */
public class JavaVariable extends JavaExpression {
    private final GeneratedIdentifier id;

    public JavaVariable(GeneratedIdentifier id, JavaType type) {
        super(type);
        this.id = id;
    }

    public GeneratedIdentifier getId() {
        return id;
    }

    @Override
    public void render(StringBuilder sb) {
        sb.append(id.getActualName());
    }
}
