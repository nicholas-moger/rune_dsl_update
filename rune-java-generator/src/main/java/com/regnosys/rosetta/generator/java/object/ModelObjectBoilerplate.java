package com.regnosys.rosetta.generator.java.object;

import java.util.Collection;
import java.util.List;

import com.regnosys.rosetta.generator.java.JavaNamingUtil;
import com.regnosys.rosetta.generator.java.types.AttributeMetaType;
import com.regnosys.rosetta.generator.java.types.JavaPojoInterface;
import com.regnosys.rosetta.generator.java.types.JavaPojoProperty;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.rosetta.util.types.JavaType;

/**
 * Generates equals, hashCode, toString, and process method bodies for
 * POJO interface implementations and builders.
 *
 * <p>This is a helper class used by {@code ModelObjectGenerator} and
 * {@code ModelObjectBuilderGenerator}. It does not extend JavaClassGenerator.
 *
 * <p>Ported from upstream Xtend ModelObjectBoilerPlate.xtend (192 lines).
 * Output must match upstream exactly for D11 byte-identical contract.
 */
public class ModelObjectBoilerplate {

    private final JavaTypeUtil typeUtil;

    /**
     * THE BOILERPLATE TEXT SEAM (v3.3 seat 9, PR #645 commit 8): the text the LAST {@link #boilerPlate} or
     * {@link #builderBoilerPlate} call produced, remembered so the IR emitter's section-13 text law can be held
     * against these exact bytes on their own, apart from the {@code Impl} / {@code BuilderImpl} whole they are
     * embedded and re-indented inside. Assigned at the end of each of the two methods and read by nothing in
     * production; not one byte of either method's output moves.
     */
    private String lastBoilerPlate = "";

    public ModelObjectBoilerplate(JavaTypeUtil typeUtil) {
        this.typeUtil = typeUtil;
    }

    /** TEST SEAM (v3.3 seat 9, PR #645 commit 8): {@link #lastBoilerPlate}. No production caller. */
    String lastBoilerPlate() {
        return lastBoilerPlate;
    }

    // -- Top-level methods called by generators --------------------------------

    /**
     * Generate boilerplate for the immutable implementation class.
     * Uses the identity class name function (e.g., "AdjustableDate").
     */
    public String boilerPlate(JavaPojoInterface javaType, boolean extended,
                              Collection<JavaPojoProperty> properties) {
        var sb = new StringBuilder();
        sb.append(contributeEquals(javaType, extended, properties));
        sb.append(contributeHashCode(extended, properties));
        sb.append(contributeToString(javaType.getSimpleName(), extended, properties));
        lastBoilerPlate = sb.toString();   // v3.3 seat 9 (PR #645 commit 8) text seam - see lastBoilerPlate()
        return lastBoilerPlate;
    }

    /**
     * Generate boilerplate for the builder implementation class.
     * Uses the builder class name function (e.g., "AdjustableDateBuilder").
     */
    public String builderBoilerPlate(JavaPojoInterface javaType, boolean extended,
                                     Collection<JavaPojoProperty> properties) {
        var sb = new StringBuilder();
        sb.append(contributeEquals(javaType, extended, properties));
        sb.append(contributeHashCode(extended, properties));
        sb.append(contributeToString(javaType.getSimpleName() + "Builder",
                extended, properties));
        lastBoilerPlate = sb.toString();   // v3.3 seat 9 (PR #645 commit 8) text seam - see lastBoilerPlate()
        return lastBoilerPlate;
    }

    /**
     * Generate the Processor visitor method for the interface default.
     */
    public String processMethod(JavaPojoInterface javaType) {
        var sb = new StringBuilder();
        sb.append("\t@Override\n");
        sb.append("\tdefault void process(").append(ModelObjectGenerator.T_ROSETTA_PATH).append(" path, ")
          .append(ModelObjectGenerator.T_PROCESSOR).append(" processor) {\n");
        for (JavaPojoProperty prop : javaType.getAllProperties()) {
            String getterName = prop.getOperationName(
                    com.regnosys.rosetta.generator.java.types.JavaPojoPropertyOperationType.GET);
            JavaType itemType = typeUtil.getItemType(prop.getType());
            // facet javaLangAttrFqn (PR #306): the interface-process visits the VALUE type
            // (X.class). When the model attribute type's simple name collides with java.lang,
            // golden refuses its import (first-claim-wins) and FQN-inlines the reference here.
            // v3.2 seat 11 (D50): the ONE value-site law, ModelObjectGenerator.valueSiteTypeRef -
            // the java.lang collision canonical, every other class a first-claim sentinel. (The
            // builder-process visits X.XBuilder, which golden leaves bare.)
            String itemTypeName = itemType != null ? ModelObjectGenerator.valueSiteTypeRef(itemType) : "Object";
            String metaFlags = getMetaFlags(prop);

            sb.append("\t\t");
            if (typeUtil.isRosettaModelObject(prop.getType())) {
                sb.append("processRosetta(path.newSubPath(\"").append(prop.getName())
                  .append("\"), processor, ").append(itemTypeName).append(".class, ")
                  .append(getterName).append("()").append(metaFlags).append(");\n");
            } else {
                sb.append("processor.processBasic(path.newSubPath(\"").append(prop.getName())
                  .append("\"), ").append(itemTypeName).append(".class, ")
                  .append(getterName).append("(), this").append(metaFlags).append(");\n");
            }
        }
        sb.append("\t}\n");
        sb.append("\t\n");
        return sb.toString();
    }

    /**
     * Generate the BuilderProcessor visitor method for the builder interface default.
     */
    public String builderProcessMethod(JavaPojoInterface javaType) {
        var sb = new StringBuilder();
        sb.append("\t@Override\n");
        sb.append("\tdefault void process(").append(ModelObjectGenerator.T_ROSETTA_PATH).append(" path, ")
          .append(ModelObjectGenerator.T_BUILDER_PROCESSOR).append(" processor) {\n");
        for (JavaPojoProperty prop : javaType.getAllProperties()) {
            String getterName = prop.getOperationName(
                    com.regnosys.rosetta.generator.java.types.JavaPojoPropertyOperationType.GET);
            String metaFlags = getMetaFlags(prop);

            sb.append("\t\t");
            if (typeUtil.isRosettaModelObject(prop.getType())) {
                // Builder process uses the builder type
                JavaType itemType = typeUtil.getItemType(prop.getType());
                String builderTypeName = itemType != null
                        ? itemType.getSimpleName() + "Builder"
                        : "Object";
                sb.append("processRosetta(path.newSubPath(\"").append(prop.getName())
                  .append("\"), processor, ").append(builderTypeName).append(".class, ")
                  .append(getterName).append("()").append(metaFlags).append(");\n");
            } else {
                JavaType itemType = typeUtil.getItemType(prop.getType());
                // facet javaLangAttrFqn (W42 finding #13, PR #426): the BASIC branch visits the
                // VALUE type (X.class) - the same law as processMethod above (v3.2 seat 11, D50:
                // ModelObjectGenerator.valueSiteTypeRef, one declaration). This emitter has no
                // production caller - ModelObjectGenerator emits the builder-process inline - but
                // the twin carries the same law so a future revival cannot resurrect the gap.
                String itemTypeName = itemType != null ? ModelObjectGenerator.valueSiteTypeRef(itemType) : "Object";
                sb.append("processor.processBasic(path.newSubPath(\"").append(prop.getName())
                  .append("\"), ").append(itemTypeName).append(".class, ")
                  .append(getterName).append("(), this").append(metaFlags).append(");\n");
            }
        }
        sb.append("\t}\n");
        sb.append("\t\n");
        return sb.toString();
    }

    // -- equals ---------------------------------------------------------------

    private String contributeEquals(JavaPojoInterface javaType, boolean extended,
                                    Collection<JavaPojoProperty> properties) {
        var sb = new StringBuilder();
        sb.append("\t@Override\n");
        sb.append("\tpublic boolean equals(Object o) {\n");
        sb.append("\t\tif (this == o) return true;\n");
        sb.append("\t\tif (o == null || !(o instanceof ").append(ModelObjectGenerator.T_ROSETTA_MODEL_OBJECT)
          .append(") || !getType().equals(((").append(ModelObjectGenerator.T_ROSETTA_MODEL_OBJECT)
          .append(")o).getType())) return false;\n");
        if (extended) {
            sb.append("\t\tif (!super.equals(o)) return false;\n");
        }
        sb.append("\t\n");
        if (!properties.isEmpty()) {
            sb.append("\t\t").append(javaType.getSimpleName())
              .append(" _that = getType().cast(o);\n");
            sb.append("\t\n");
        } else {
            // Types with no properties: extra blank line
            // (upstream Xtend FOR loop leaves blank line when empty)
            sb.append("\t\n");
        }
        for (JavaPojoProperty prop : properties) {
            // P2.1.3c β1: lowercased so choice options (PascalCase getName()) reference
            // their lowercased Java field; identity for regular RDataType attr names.
            // PR #231: escape reserved-word field names (e.g. `new` → `_new`) so the
            // equals/hashCode/toString field references compile, mirroring the field
            // declaration in ModelObjectGenerator. The toString LABEL (prop.getName())
            // stays raw. No-op for ordinary names.
            String fieldName = JavaNamingUtil.escapeJavaKeyword(
                    JavaNamingUtil.toFirstLower(prop.getName()));
            String getterName = prop.getOperationName(
                    com.regnosys.rosetta.generator.java.types.JavaPojoPropertyOperationType.GET);
            if (typeUtil.isList(prop.getType())) {
                sb.append("\t\tif (!").append(ModelObjectGenerator.T_LIST_EQUALS).append(".listEquals(").append(fieldName)
                  .append(", _that.").append(getterName).append("())) return false;\n");
            } else {
                sb.append("\t\tif (!").append(ModelObjectGenerator.T_OBJECTS).append(".equals(").append(fieldName)
                  .append(", _that.").append(getterName).append("())) return false;\n");
            }
        }
        sb.append("\t\treturn true;\n");
        sb.append("\t}\n");
        sb.append("\t\n");
        return sb.toString();
    }

    // -- hashCode -------------------------------------------------------------

    private String contributeHashCode(boolean extended,
                                      Collection<JavaPojoProperty> properties) {
        var sb = new StringBuilder();
        sb.append("\t@Override\n");
        sb.append("\tpublic int hashCode() {\n");
        sb.append("\t\tint _result = ").append(extended ? "super.hashCode()" : "0")
          .append(";\n");
        for (JavaPojoProperty prop : properties) {
            // P2.1.3c β1: lowercase field reference for choice-option props.
            // PR #231: escape reserved-word field names (e.g. `new` → `_new`) so the
            // equals/hashCode/toString field references compile, mirroring the field
            // declaration in ModelObjectGenerator. The toString LABEL (prop.getName())
            // stays raw. No-op for ordinary names.
            String fieldName = JavaNamingUtil.escapeJavaKeyword(
                    JavaNamingUtil.toFirstLower(prop.getName()));
            // v3.3 seat 9 (PR #645 commit 8): ONE declaration of the item-is-enum law (LAW 69). This site WRITES
            // the bytes and now delegates to the seam IRPropertyReconciler reconciles the IR's
            // property.<name>.itemIsEnum fact against, so the two can never drift.
            boolean isEnum = ModelObjectGenerator.itemIsEnum(prop, typeUtil);

            if (isEnum) {
                if (typeUtil.isList(prop.getType())) {
                    sb.append("\t\t_result = 31 * _result + (").append(fieldName)
                      .append(" != null ? ").append(fieldName)
                      .append(".stream().map(Object::getClass).map(Class::getName).mapToInt(String::hashCode).sum() : 0);\n");
                } else {
                    sb.append("\t\t_result = 31 * _result + (").append(fieldName)
                      .append(" != null ? ").append(fieldName)
                      .append(".getClass().getName().hashCode() : 0);\n");
                }
            } else {
                sb.append("\t\t_result = 31 * _result + (").append(fieldName)
                  .append(" != null ? ").append(fieldName).append(".hashCode() : 0);\n");
            }
        }
        sb.append("\t\treturn _result;\n");
        sb.append("\t}\n");
        sb.append("\t\n");
        return sb.toString();
    }

    // -- toString -------------------------------------------------------------

    private String contributeToString(String className, boolean extended,
                                      Collection<JavaPojoProperty> properties) {
        var sb = new StringBuilder();
        sb.append("\t@Override\n");
        sb.append("\tpublic String toString() {\n");
        sb.append("\t\treturn \"").append(className).append(" {\" +\n");

        var propList = List.copyOf(properties);
        for (int i = 0; i < propList.size(); i++) {
            JavaPojoProperty prop = propList.get(i);
            // Label keeps the rosetta property name (PascalCase for choice options);
            // field reference uses the lowercased Java identifier. P2.1.3c β1.
            String label = prop.getName();
            // PR #231: escape reserved-word field names (e.g. `new` → `_new`) so the
            // equals/hashCode/toString field references compile, mirroring the field
            // declaration in ModelObjectGenerator. The toString LABEL (prop.getName())
            // stays raw. No-op for ordinary names.
            String fieldName = JavaNamingUtil.escapeJavaKeyword(
                    JavaNamingUtil.toFirstLower(prop.getName()));
            sb.append("\t\t\t\"").append(label).append("=\" + this.")
              .append(fieldName);
            if (i < propList.size() - 1) {
                sb.append(" + \", \" +\n");
            } else {
                sb.append(" +\n");
            }
        }

        sb.append("\t\t'}'");
        if (extended) {
            sb.append(" + \" \" + super.toString()");
        }
        sb.append(";\n");
        sb.append("\t}\n");
        return sb.toString();
    }

    // -- Helpers --------------------------------------------------------------

    private String getMetaFlags(JavaPojoProperty prop) {
        if (prop.getMeta() != null) {
            return ", " + ModelObjectGenerator.T_ATTRIBUTE_META + "." + prop.getMeta().name();
        }
        return "";
    }

    /**
     * Whether a property has a scoped reference annotation.
     */
    public boolean isScopedReference(JavaPojoProperty prop) {
        return prop.getAttributeMetaTypes().contains(AttributeMetaType.SCOPED_REFERENCE);
    }

    /**
     * Whether a property has a scoped key annotation.
     */
    public boolean isScopedKey(JavaPojoProperty prop) {
        return prop.getAttributeMetaTypes().contains(AttributeMetaType.SCOPED_KEY);
    }
}
