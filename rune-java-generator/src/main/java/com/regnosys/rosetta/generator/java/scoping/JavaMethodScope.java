package com.regnosys.rosetta.generator.java.scoping;

/**
 * Method-level scope. Creates a body scope on first access for
 * statement-level identifier management.
 */
public class JavaMethodScope extends AbstractJavaScope<JavaClassScope> {
    private JavaStatementScope bodyScope;

    JavaMethodScope(String methodName, JavaClassScope parent) {
        super("Method[" + methodName + "]", parent);
    }

    /** Get or create the statement-level body scope for this method. */
    public JavaStatementScope getBodyScope() {
        if (bodyScope == null) {
            bodyScope = new JavaStatementScope("Body", this);
        }
        return bodyScope;
    }
}
