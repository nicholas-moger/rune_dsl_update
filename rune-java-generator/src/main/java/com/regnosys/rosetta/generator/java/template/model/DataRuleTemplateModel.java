package com.regnosys.rosetta.generator.java.template.model;

import java.util.List;

/**
 * Template model for {@code DataRuleGenerator} ({@code java-datarule.stg}).
 *
 * <p>The interface body arrives fully rendered ({@code classBody} — the
 * NAME/DEFINITION constants, the {@code Default} + {@code NoOp} nested classes,
 * every in-class blank separator carrying its indentation tabs) per the wave-C
 * generator-computed-body convention; the template contributes the file frame:
 * package, imports + the static-import gap law (one blank line after the
 * regular block, then the static wildcard + one blank — or two blank lines when
 * no static import), the version javadoc, the two annotations, and the
 * {@code extends Validator<T>} interface line.
 */
public class DataRuleTemplateModel {

    private final String packageName;
    private final String className;
    private final String subjectJavaType;
    private final String version;
    private final List<String> imports;
    private final List<String> staticImports;
    private final String classBody;

    public DataRuleTemplateModel(String packageName, String className, String subjectJavaType,
                                 String version, List<String> imports, List<String> staticImports,
                                 String classBody) {
        this.packageName = packageName;
        this.className = className;
        this.subjectJavaType = subjectJavaType;
        this.version = version;
        this.imports = imports;
        this.staticImports = staticImports;
        this.classBody = classBody;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public String getSubjectJavaType() {
        return subjectJavaType;
    }

    public String getVersion() {
        return version;
    }

    public List<String> getImports() {
        return imports;
    }

    public List<String> getStaticImports() {
        return staticImports;
    }

    public String getClassBody() {
        return classBody;
    }
}
