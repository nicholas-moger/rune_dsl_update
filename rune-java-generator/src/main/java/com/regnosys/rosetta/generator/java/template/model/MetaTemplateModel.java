package com.regnosys.rosetta.generator.java.template.model;

import java.util.List;

/**
 * Template model for the {@code RosettaMetaData} implementation generated
 * by {@code ModelMetaGenerator}. Provides validator references, condition
 * (data rule) class references, qualification-function references, and
 * version metadata.
 *
 * <p>{@code dataClassJavaType} carries the #306 java.lang-collision law for the
 * subject type: the FQN when the simple name collides with an implicitly-imported
 * {@code java.lang} type (golden iso {@code ErrorMeta} — FQN at every TYPE
 * position including {@code @RosettaMeta(model=…)}, no data-class import),
 * otherwise the simple name.
 */
public class MetaTemplateModel {

    private final String packageName;
    private final String metaClassName;
    private final String dataClassName;
    private final String dataClassJavaType;
    private final String version;
    private final List<String> imports;
    private final String validatorSimple;
    private final String typeFormatValidatorSimple;
    private final String onlyExistsValidatorSimple;
    private final List<ConditionRefModel> conditionRefs;
    private final List<QualifyRefModel> qualifyRefs;

    public MetaTemplateModel(String packageName, String metaClassName,
                              String dataClassName, String dataClassJavaType, String version,
                              List<String> imports,
                              String validatorSimple,
                              String typeFormatValidatorSimple,
                              String onlyExistsValidatorSimple,
                              List<ConditionRefModel> conditionRefs,
                              List<QualifyRefModel> qualifyRefs) {
        this.packageName = packageName;
        this.metaClassName = metaClassName;
        this.dataClassName = dataClassName;
        this.dataClassJavaType = dataClassJavaType;
        this.version = version;
        this.imports = List.copyOf(imports);
        this.validatorSimple = validatorSimple;
        this.typeFormatValidatorSimple = typeFormatValidatorSimple;
        this.onlyExistsValidatorSimple = onlyExistsValidatorSimple;
        this.conditionRefs = List.copyOf(conditionRefs);
        this.qualifyRefs = List.copyOf(qualifyRefs);
    }

    public String getPackageName() { return packageName; }
    public String getMetaClassName() { return metaClassName; }
    public String getDataClassName() { return dataClassName; }
    public String getDataClassJavaType() { return dataClassJavaType; }
    public String getVersion() { return version; }
    public List<String> getImports() { return imports; }
    public String getValidatorSimple() { return validatorSimple; }
    public String getTypeFormatValidatorSimple() { return typeFormatValidatorSimple; }
    public String getOnlyExistsValidatorSimple() { return onlyExistsValidatorSimple; }
    public List<ConditionRefModel> getConditionRefs() { return conditionRefs; }
    public List<QualifyRefModel> getQualifyRefs() { return qualifyRefs; }
}
