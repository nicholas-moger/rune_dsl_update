package com.regnosys.rosetta.generator.java.scoping;

import com.rosetta.util.types.JavaTypeDeclaration;

/**
 * Class-level scope. Manages nested class identifiers and creates
 * method-level child scopes.
 */
public class JavaClassScope extends AbstractJavaScope<AbstractJavaScope<?>> {

    private JavaClassScope(String className, AbstractJavaScope<?> parentScope) {
        super("Class[" + className + "]", parentScope);
    }

    /**
     * Create a top-level class scope with its own file scope.
     * Registers the class identifier in the file scope.
     */
    public static JavaClassScope createAndRegisterIdentifier(JavaTypeDeclaration<?> clazz) {
        JavaFileScope fileScope = new JavaFileScope(
                clazz.getSimpleName() + ".java", clazz.getPackageName());
        fileScope.createIdentifier(clazz, clazz.getSimpleName());
        return new JavaClassScope(clazz.getSimpleName(), fileScope);
    }

    /**
     * Create a nested class scope within this class scope.
     * Registers the nested class in both this scope and the file scope.
     */
    public JavaClassScope createNestedClassScopeAndRegisterIdentifier(JavaTypeDeclaration<?> clazz) {
        this.createIdentifier(clazz, clazz.getSimpleName());
        this.getFileScope().createIdentifier(clazz, clazz.getNestedTypeName().withDots());
        return new JavaClassScope(clazz.getSimpleName(), this);
    }

    /** Create a method-level child scope. */
    public JavaMethodScope createMethodScope(String methodName) {
        return new JavaMethodScope(methodName, this);
    }

    @Override
    public String escapeName(String name) {
        return "_" + name;
    }
}
