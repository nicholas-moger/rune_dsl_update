package com.regnosys.rosetta.generator.java.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.generator.java.enums.EnumHelper;
import com.regnosys.rosetta.generator.java.scoping.JavaPackageName;
import com.rosetta.util.DottedPath;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaTypeDeclaration;

/**
 * Represents a generated Java enum class from an {@link REnumeration} AST node.
 * Computes the Java-ified value names and handles enum inheritance.
 *
 * <p>Adapted from upstream to use our AST types instead of EMF.
 */
public class RJavaEnum extends RGeneratedJavaClass<Object> {
    private final REnumeration enumeration;

    private RJavaEnum parent = null;
    private List<RJavaEnumValue> enumValues = null;

    /**
     * Create an RJavaEnum from an enumeration AST node.
     *
     * @param enumeration the AST node
     * @param namespace the Rosetta namespace (from the containing RModel)
     */
    public RJavaEnum(REnumeration enumeration, DottedPath namespace) {
        super(JavaPackageName.escape(namespace), DottedPath.of(enumeration.name()));
        this.enumeration = enumeration;
    }

    /** The underlying AST node. */
    public REnumeration getAstNode() {
        return enumeration;
    }

    /**
     * The parent enum (from {@code extends}), or null if none.
     *
     * <p><strong>KNOWN DEFECT (tracked for T4):</strong> uses current enum's
     * namespace for parent, which is wrong when parent lives in a different
     * namespace. {@code GeneratorModel} (T4) must provide the correct
     * namespace. CDM/DRR corpus should be tested for cross-namespace enum
     * inheritance to validate this assumption.
     */
    public RJavaEnum getParent() {
        if (enumeration.superType().isPresent()) {
            if (parent == null) {
                // FIXME(T4): parent namespace must come from GeneratorModel,
                // not from current enum's package. Wrong if parent crosses namespaces.
                parent = new RJavaEnum(enumeration.superType().get(),
                        getPackageName());
            }
        }
        return parent;
    }

    /**
     * All enum values including inherited ones from the parent chain.
     * Inherited values come first, own values appended after.
     */
    public List<RJavaEnumValue> getEnumValues() {
        if (enumValues == null) {
            enumValues = new ArrayList<>();
            RJavaEnum p = getParent();
            if (p != null) {
                for (RJavaEnumValue v : p.getEnumValues()) {
                    enumValues.add(new RJavaEnumValue(this, v.getName(), v.getAstNode(), v));
                }
            }
            for (REnumValue v : enumeration.values()) {
                enumValues.add(new RJavaEnumValue(this, EnumHelper.convertValue(v), v, null));
            }
        }
        return enumValues;
    }

    @Override
    public JavaClass<? super Object> getSuperclassDeclaration() {
        return JavaClass.OBJECT;
    }

    @Override
    public JavaClass<? super Object> getSuperclass() {
        return getSuperclassDeclaration();
    }

    @Override
    public List<JavaClass<?>> getInterfaceDeclarations() {
        return Collections.emptyList();
    }

    @Override
    public List<JavaClass<?>> getInterfaces() {
        return Collections.emptyList();
    }

    @Override
    public boolean extendsDeclaration(JavaTypeDeclaration<?> other) {
        return other.equals(JavaClass.OBJECT);
    }

    @Override
    public boolean isFinal() {
        return true;
    }
}
