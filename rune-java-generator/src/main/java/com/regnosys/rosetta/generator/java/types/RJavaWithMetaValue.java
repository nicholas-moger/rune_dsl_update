package com.regnosys.rosetta.generator.java.types;

import java.util.Collection;

import com.regnosys.rosetta.generator.java.scoping.JavaPackageName;
import com.rosetta.util.DottedPath;
import com.rosetta.util.types.JavaReferenceType;

/**
 * Java-type representation of a metafield wrapper that carries a single value
 * (a {@code FieldWithMetaX} or {@code ReferenceWithMetaX}). Holds the bare
 * {@code valueType} the wrapper wraps, so the coercion service can dispatch the
 * meta-unwrap ({@code «expr».getValue()}) on the type intrinsically rather than
 * by string-matching the {@code FieldWithMeta*}/{@code ReferenceWithMeta*} name
 * (forbidden — CLAUDE.md (local) engineering standard).
 *
 * <p>Faithful port of upstream {@code RJavaWithMetaValue} (rune-dsl 9.83.0-line upstream). The
 * fork previously had no such type — {@link JavaTypeTranslator#toMetaJavaType}
 * returns the generic {@code FieldWithMeta<T>} and the concrete per-namespace
 * {@code metafields.FieldWithMetaX} wrapper is built text-only by
 * {@code MetaFieldGenerator}. This class is the {@code RJavaWithMetaValue}
 * analogue the typed-pipeline coercer dispatch (M7b-3) requires.
 */
public abstract class RJavaWithMetaValue extends JavaPojoInterface {
    protected final JavaReferenceType valueType;

    public RJavaWithMetaValue(JavaReferenceType valueType, JavaPackageName packageName,
                              String simpleName, JavaTypeUtil typeUtil) {
        super(packageName, simpleName, typeUtil);
        this.valueType = valueType;
    }

    /**
     * Build the concrete per-namespace meta wrapper for a bare value type. The
     * {@code metafields} package is derived from the value type's own namespace for
     * generated types ({@link RGeneratedJavaClass}) and from {@code com.rosetta.model}
     * for builtins — identical to {@code RJavaPojoInterface.resolveWrappedTypeNamespace}
     * and {@code MetaFieldGenerator}'s {@code .child("metafields")}, so the wrapper's
     * fully-qualified name byte-matches the generated {@code metafields.FieldWithMetaX}/
     * {@code metafields.ReferenceWithMetaX} class (and thus the golden import).
     *
     * <p>This is the single construction site the typed-pipeline navigation wiring
     * (engine PR #9 / PR-B) uses to surface a real {@code RJavaWithMetaValue} into a
     * navigation receiver's expression type, so the dormant {@code ItemToItemCoercer}
     * meta-unwrap (PR-A) dispatches on it intrinsically rather than by name-matching.
     *
     * @param isReference {@code true} for {@code [metadata reference|address]}
     *                    ({@link RJavaReferenceWithMeta}); {@code false} for
     *                    {@code [metadata scheme|id|location]} ({@link RJavaFieldWithMeta}).
     *                    Callers derive this from {@code MetaFieldGenerator.detectMetaKind};
     *                    it stays a {@code boolean} here to avoid a {@code types -> object}
     *                    package cycle.
     * @param valueType   the bare (meta-stripped) value type the wrapper wraps
     * @param typeUtil    the shared type utility
     */
    public static RJavaWithMetaValue create(boolean isReference, JavaReferenceType valueType,
                                            JavaTypeUtil typeUtil) {
        JavaPackageName metafieldsPackage = metafieldsPackage(valueType);
        return isReference
                ? new RJavaReferenceWithMeta(valueType, metafieldsPackage, typeUtil)
                : new RJavaFieldWithMeta(valueType, metafieldsPackage, typeUtil);
    }

    /**
     * Resolve the {@code <value-namespace>.metafields} package for a value type,
     * mirroring {@code RJavaPojoInterface.resolveWrappedTypeNamespace} +
     * {@code .child("metafields")}: a generated type's own namespace, or
     * {@code com.rosetta.model} for builtins (String, Date, ...).
     */
    private static JavaPackageName metafieldsPackage(JavaReferenceType valueType) {
        DottedPath valueNamespace = (valueType instanceof RGeneratedJavaClass<?> generated)
                ? DottedPath.splitOnDots(generated.getPackageName().toString())
                : DottedPath.splitOnDots("com.rosetta.model");
        return JavaPackageName.escape(valueNamespace.child("metafields"));
    }

    public JavaReferenceType getValueType() {
        return valueType;
    }

    @Override
    public Collection<JavaPojoProperty> getAllProperties() {
        return getOwnProperties();
    }

    @Override
    public JavaPojoInterface getSuperPojo() {
        return null;
    }

    @Override
    public String getJavadoc() {
        return null;
    }

    @Override
    public String getRosettaName() {
        return getSimpleName();
    }

    @Override
    public String getVersion() {
        return "0.0.0";
    }
}
