package com.regnosys.rosetta.generator.java.template.model;

import java.util.List;

/**
 * Template model for FieldWithMeta and ReferenceWithMeta ST4 templates.
 * Provides all values needed by the ST4 templates to generate wrapper classes.
 */
public class MetaFieldTemplateModel {

    private final String packageName;
    private final String className;
    private final String wrappedTypeName;
    private final String wrappedTypeFqn;
    private final boolean isComposite;
    private final boolean isEnum;
    private final String modelName;
    private final List<String> imports;

    public MetaFieldTemplateModel(String packageName, String className,
                                   String wrappedTypeName, String wrappedTypeFqn,
                                   boolean isComposite, boolean isEnum,
                                   String modelName, List<String> imports) {
        this.packageName = packageName;
        this.className = className;
        this.wrappedTypeName = wrappedTypeName;
        this.wrappedTypeFqn = wrappedTypeFqn;
        this.isComposite = isComposite;
        this.isEnum = isEnum;
        this.modelName = modelName;
        this.imports = List.copyOf(imports);
    }

    public String getPackageName() { return packageName; }
    public String getClassName() { return className; }
    public String getWrappedTypeName() { return wrappedTypeName; }
    public String getWrappedTypeFqn() { return wrappedTypeFqn; }
    public boolean getIsComposite() { return isComposite; }
    public boolean getIsEnum() { return isEnum; }
    public String getModelName() { return modelName; }
    public List<String> getImports() { return imports; }
}
