package com.regnosys.rosetta.generator.java.types;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.regnosys.rosetta.generator.java.JavaNamingUtil;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.util.types.JavaType;

/**
 * Represents a property within a generated POJO — name, type, metadata,
 * annotations, and parent property (for inheritance).
 *
 * <p>Controls getter/setter method name generation via
 * {@link #getOperationName(JavaPojoPropertyOperationType)}.
 *
 * <p>Ported from upstream — removed Xtend
 * {@code StringConcatenationClient} dependency.
 */
public class JavaPojoProperty {
    private static final Set<String> OPERATION_NAMES_TO_ESCAPE =
            Set.of("getClass", "getType");

    private final JavaPojoInterface pojo;
    private final String name;
    private final String runeName;
    private final String serializedName;
    private final String getterCompatibilityName;
    private final String setterCompatibilityName;
    private final JavaType type;
    /** The underlying value type before FieldWithMeta/ReferenceWithMeta wrapping, or null if not wrapped. */
    private final JavaType metaValueType;
    private final String javadoc;
    private final JavaPojoProperty parentProperty;
    private final boolean isRequired;
    private final AttributeMeta meta;
    private final boolean hasLocation;
    private final List<AttributeMetaType> attributeMetaTypes;
    /**
     * facet pojoOverrideNaming (PR #323): whether this SPECIALIZED property's type is a
     * (generic) subtype of its parent property's type — computed at specialization time by
     * {@link RJavaPojoInterface} (which can see MODEL-level covariance through the Rosetta
     * supertype chain; the flat {@code RGeneratedJavaClass} instances the property types are
     * built from all report {@code RosettaModelObject} as their only supertype, so the
     * runtime {@code JavaType.isSubtypeOf} cannot). Meaningless (true) when
     * {@code parentProperty == null}.
     */
    private final boolean compatibleWithParent;

    public JavaPojoProperty(JavaPojoInterface pojo, String name, String runeName,
                            String serializedName, String getterCompatibilityName,
                            String setterCompatibilityName, JavaType type, String javadoc,
                            AttributeMeta meta, boolean hasLocation,
                            List<AttributeMetaType> attributeMetaTypes, boolean isRequired) {
        this(pojo, name, runeName, serializedName, getterCompatibilityName,
                setterCompatibilityName, type, null, javadoc, meta, hasLocation,
                attributeMetaTypes, isRequired, null, true);
    }

    public JavaPojoProperty(JavaPojoInterface pojo, String name, String runeName,
                            String serializedName, String getterCompatibilityName,
                            String setterCompatibilityName, JavaType type,
                            JavaType metaValueType, String javadoc,
                            AttributeMeta meta, boolean hasLocation,
                            List<AttributeMetaType> attributeMetaTypes, boolean isRequired) {
        this(pojo, name, runeName, serializedName, getterCompatibilityName,
                setterCompatibilityName, type, metaValueType, javadoc, meta, hasLocation,
                attributeMetaTypes, isRequired, null, true);
    }

    private JavaPojoProperty(JavaPojoInterface pojo, String name, String runeName,
                             String serializedName, String getterCompatibilityName,
                             String setterCompatibilityName, JavaType type,
                             JavaType metaValueType, String javadoc,
                             AttributeMeta meta, boolean hasLocation,
                             List<AttributeMetaType> attributeMetaTypes, boolean isRequired,
                             JavaPojoProperty parentProperty, boolean compatibleWithParent) {
        this.pojo = pojo;
        this.name = name;
        this.runeName = runeName;
        this.serializedName = serializedName;
        this.getterCompatibilityName = getterCompatibilityName;
        this.setterCompatibilityName = setterCompatibilityName;
        this.type = type;
        this.metaValueType = metaValueType;
        this.javadoc = javadoc;
        this.meta = meta;
        this.hasLocation = hasLocation;
        this.attributeMetaTypes = attributeMetaTypes;
        this.isRequired = isRequired;
        this.parentProperty = parentProperty;
        this.compatibleWithParent = compatibleWithParent;
    }

    /**
     * Create a specialised version of this property for a sub-type.
     *
     * <p>facet pojoOverrideNaming (PR #323): callers pass the upstream-law compatibility
     * names ({@code RJavaPojoInterface.addProperty} computes them per the 9.83.0-line upstream
     * {@code addPropertyIfNecessary} law) plus the model-aware subtype verdict
     * ({@code compatibleWithParent}) used by {@link #isCompatibleTypeWithParent()}.
     */
    public JavaPojoProperty specialize(JavaPojoInterface pojo, String getterCompatibilityName,
                                        String setterCompatibilityName, JavaType newType,
                                        JavaType newMetaValueType, String newJavadoc,
                                        AttributeMeta newMeta, boolean newHasLocation,
                                        List<AttributeMetaType> attributeMetaTypes,
                                        boolean isRequired, boolean compatibleWithParent) {
        return new JavaPojoProperty(pojo, name, runeName, serializedName,
                getterCompatibilityName, setterCompatibilityName, newType, newMetaValueType,
                newJavadoc, newMeta, newHasLocation, attributeMetaTypes, isRequired, this,
                compatibleWithParent);
    }

    /**
     * Compute the Java method name for the given operation type.
     * E.g., GET + "price" → "getPrice", SET + "price" → "setPrice".
     * Escapes names that clash with Object/RosettaModelObject methods.
     */
    public String getOperationName(JavaPojoPropertyOperationType operationType) {
        String compatibilityName;
        if (operationType == JavaPojoPropertyOperationType.GET
                || operationType == JavaPojoPropertyOperationType.GET_OR_CREATE) {
            compatibilityName = this.getterCompatibilityName;
        } else {
            compatibilityName = this.setterCompatibilityName;
        }
        String opName = operationType.getPrefix()
                + JavaNamingUtil.toFirstUpper(compatibilityName)
                + operationType.getPostfix();
        return escapeOperationName(opName);
    }

    /**
     * Escape a Java accessor name that clashes with an inherited {@code Object} /
     * {@code RosettaModelObject} method ({@code getClass}, {@code getType}) by prepending
     * {@code _} — exactly as the golden does (e.g. an attribute literally named {@code type}
     * declares {@code _getType()} because {@code RosettaModelObject.getType()} returns
     * {@code Class<? extends X>}).
     *
     * <p>Public + shared SOT (the single {@link #OPERATION_NAMES_TO_ESCAPE} set): the POJO
     * accessor declarations ({@link #getOperationName}) AND the function-navigation lambda body
     * (NavigationHandler's {@code .map("getType", v -> v._getType())}) must agree on the escaped
     * name, or the generated function fails to compile against its own POJO. Both route through
     * this method so the escape can never drift between the two subsystems.
     */
    public static String escapeOperationName(String opName) {
        if (OPERATION_NAMES_TO_ESCAPE.contains(opName)) {
            return "_" + opName;
        }
        return opName;
    }

    /**
     * Upstream {@code JavaPojoProperty.isCompatibleTypeWithParent} — drives the Impl
     * {@code extends} predicate. facet pojoOverrideNaming (PR #323): reads the
     * specialization-time verdict instead of {@code type.isSubtypeOf(parentProperty.type)}
     * because the fork's generated model classes carry no model-level supertype links
     * (see {@link #compatibleWithParent}).
     */
    public boolean isCompatibleTypeWithParent() {
        return parentProperty == null || compatibleWithParent;
    }

    public boolean isSameTypeAsParent() {
        return parentProperty == null || type.equals(parentProperty.type);
    }

    public boolean getterOverridesParentGetter() {
        return parentProperty != null
                && getterCompatibilityName.equals(parentProperty.getterCompatibilityName);
    }

    // -- Getters --------------------------------------------------------------

    public String getName() { return name; }
    /** The pojo interface that declares this property (seat 21: the override-chain selector maps a selected property back to its owner's attribute). */
    public JavaPojoInterface getPojo() { return pojo; }
    public String getRuneName() { return runeName; }
    public String getSerializedName() { return serializedName; }
    public String getGetterCompatibilityName() { return getterCompatibilityName; }
    public String getSetterCompatibilityName() { return setterCompatibilityName; }
    public JavaType getType() { return type; }
    /** The underlying value type before FieldWithMeta/ReferenceWithMeta wrapping, or null. */
    public JavaType getMetaValueType() { return metaValueType; }
    public String getJavadoc() { return javadoc; }
    public AttributeMeta getMeta() { return meta; }
    public boolean hasLocation() { return hasLocation; }
    public List<AttributeMetaType> getAttributeMetaTypes() { return attributeMetaTypes; }
    public JavaPojoProperty getParentProperty() { return parentProperty; }
    public boolean isRequired() { return isRequired; }

    @Override
    public String toString() {
        return JavaPojoProperty.class.getSimpleName()
                + "[" + type.getSimpleName() + " " + getterCompatibilityName + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(pojo, getterCompatibilityName, setterCompatibilityName,
                hasLocation, javadoc, meta, name, runeName, serializedName,
                parentProperty, type, metaValueType, attributeMetaTypes, isRequired);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        JavaPojoProperty other = (JavaPojoProperty) obj;
        return Objects.equals(pojo, other.pojo)
                && Objects.equals(getterCompatibilityName, other.getterCompatibilityName)
                && Objects.equals(setterCompatibilityName, other.setterCompatibilityName)
                && hasLocation == other.hasLocation
                && Objects.equals(javadoc, other.javadoc) && meta == other.meta
                && Objects.equals(name, other.name)
                && Objects.equals(runeName, other.runeName)
                && Objects.equals(serializedName, other.serializedName)
                && Objects.equals(attributeMetaTypes, other.attributeMetaTypes)
                && Objects.equals(parentProperty, other.parentProperty)
                && Objects.equals(type, other.type)
                && Objects.equals(metaValueType, other.metaValueType)
                && isRequired == other.isRequired;
    }
}
