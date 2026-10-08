package com.regnosys.rosetta.generator.java.types;

import java.util.ArrayList;
import java.util.List;

import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaTypeDeclaration;

/**
 * Represents the Builder interface nested inside a POJO interface.
 * E.g., {@code FooBuilder} inside {@code Foo}.
 */
public class JavaPojoBuilderInterface extends RGeneratedJavaClass<RosettaModelObjectBuilder> {
    private final JavaPojoInterface pojoInterface;
    private final JavaTypeUtil typeUtil;

    protected JavaPojoBuilderInterface(JavaPojoInterface pojoInterface, JavaTypeUtil typeUtil) {
        super(pojoInterface.getEscapedPackageName(),
                pojoInterface.getNestedTypeName().child(pojoInterface.getSimpleName() + "Builder"));
        this.pojoInterface = pojoInterface;
        this.typeUtil = typeUtil;
    }

    // Return type matches upstream exactly: JavaClass<? super RosettaModelObject>
    // (not RosettaModelObjectBuilder) — see upstream JavaPojoBuilderInterface.java
    @Override
    @SuppressWarnings("unchecked")
    public JavaClass<? super RosettaModelObjectBuilder> getSuperclassDeclaration() {
        return (JavaClass<? super RosettaModelObjectBuilder>) (JavaClass<?>) JavaClass.OBJECT;
    }

    @Override
    public JavaClass<? super RosettaModelObjectBuilder> getSuperclass() {
        return getSuperclassDeclaration();
    }

    @Override
    public List<? extends JavaTypeDeclaration<?>> getInterfaceDeclarations() {
        List<? extends JavaTypeDeclaration<?>> baseInterfaces = pojoInterface.getInterfaceDeclarations();
        List<JavaTypeDeclaration<?>> interfaces = new ArrayList<>(baseInterfaces.size() + 1);
        interfaces.add(pojoInterface);
        for (var baseInterface : baseInterfaces) {
            interfaces.add(typeUtil.toBuilder(baseInterface));
        }
        return interfaces;
    }

    @Override
    public List<JavaClass<?>> getInterfaces() {
        List<JavaClass<?>> baseInterfaces = pojoInterface.getInterfaces();
        List<JavaClass<?>> interfaces = new ArrayList<>(baseInterfaces.size() + 1);
        interfaces.add(pojoInterface);
        for (var baseInterface : baseInterfaces) {
            interfaces.add(typeUtil.toBuilder(baseInterface));
        }
        return interfaces;
    }
}
