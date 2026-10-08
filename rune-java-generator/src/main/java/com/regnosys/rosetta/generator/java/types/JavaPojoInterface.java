package com.regnosys.rosetta.generator.java.types;

import java.util.Collection;
import java.util.NoSuchElementException;

import com.regnosys.rosetta.generator.java.scoping.JavaPackageName;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.util.DottedPath;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;

/**
 * Abstract representation of a generated POJO interface. Subclasses
 * provide the concrete property lists, javadoc, version, and inheritance.
 *
 * <p>Creates nested class representations: {@link JavaPojoImpl},
 * {@link JavaPojoBuilderInterface}, {@link JavaPojoBuilderImpl}.
 */
public abstract class JavaPojoInterface extends RGeneratedJavaClass<RosettaModelObject> {
    private final JavaTypeUtil typeUtil;

    protected JavaPojoInterface(JavaPackageName packageName, String simpleName,
                                JavaTypeUtil typeUtil) {
        super(packageName, DottedPath.of(simpleName));
        this.typeUtil = typeUtil;
    }

    public abstract String getJavadoc();
    public abstract String getRosettaName();
    public abstract String getVersion();

    public abstract Collection<JavaPojoProperty> getOwnProperties();
    public abstract Collection<JavaPojoProperty> getAllProperties();

    public abstract JavaPojoInterface getSuperPojo();

    public JavaPojoProperty findProperty(String propertyName, JavaType desiredType) {
        JavaPojoProperty prop = findProperty(propertyName);
        JavaPojoProperty currentProp = prop;
        while (currentProp != null) {
            if (desiredType.isSubtypeOf(currentProp.getType())) {
                return currentProp;
            }
            currentProp = currentProp.getParentProperty();
        }
        return prop;
    }

    public JavaPojoProperty findProperty(String propertyName) {
        return getAllProperties().stream()
                .filter(prop -> prop.getName().equals(propertyName))
                .findAny()
                .orElseThrow(() -> new NoSuchElementException(
                        "No property named " + propertyName + " in pojo " + this));
    }

    public JavaPojoBuilderInterface toBuilderInterface() {
        return new JavaPojoBuilderInterface(this, typeUtil);
    }

    public JavaPojoImpl toImplClass() {
        return new JavaPojoImpl(this);
    }

    public JavaPojoBuilderImpl toBuilderImplClass() {
        return new JavaPojoBuilderImpl(this);
    }

    @Override
    public JavaClass<? super RosettaModelObject> getSuperclassDeclaration() {
        return JavaClass.OBJECT;
    }

    @Override
    public JavaClass<? super RosettaModelObject> getSuperclass() {
        return getSuperclassDeclaration();
    }
}
