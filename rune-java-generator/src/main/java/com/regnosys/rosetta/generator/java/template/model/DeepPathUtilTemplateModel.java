package com.regnosys.rosetta.generator.java.template.model;

import java.util.List;

/**
 * Template model for {@code DeepPathUtilGenerator} ({@code java-deeppath-util.stg}).
 *
 * <p>The class body arrives fully rendered ({@code classBody} — tab-bearing member
 * blocks incl. the in-class blank-line separators), the FUNCTION-template
 * convention for generator-computed multi-line bodies; the template contributes
 * the file frame only. {@code hasMethods} drives the frame split: methods →
 * imports + the {@code ExpressionOperatorsNullSafe} wildcard static import; no
 * methods → the EMPTY-class frame (package + three blank lines — golden
 * {@code UnitTypeDeepPathUtil}).
 */
public class DeepPathUtilTemplateModel {

    private final String packageName;
    private final String className;
    private final List<String> imports;
    private final boolean hasMethods;
    private final String classBody;

    public DeepPathUtilTemplateModel(String packageName, String className,
                                     List<String> imports, boolean hasMethods, String classBody) {
        this.packageName = packageName;
        this.className = className;
        this.imports = imports;
        this.hasMethods = hasMethods;
        this.classBody = classBody;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public List<String> getImports() {
        return imports;
    }

    public boolean getHasMethods() {
        return hasMethods;
    }

    public String getClassBody() {
        return classBody;
    }
}
