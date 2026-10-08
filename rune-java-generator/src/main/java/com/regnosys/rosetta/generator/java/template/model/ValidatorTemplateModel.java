package com.regnosys.rosetta.generator.java.template.model;

import java.util.List;

/**
 * Template model shared by the three validator generators:
 * CardinalityValidator, TypeFormatValidator, OnlyExistsValidator.
 *
 * <p>The same model structure serves all three with different {@code checks}
 * content and different {@code .stg} template files.
 */
public class ValidatorTemplateModel {

    private final String packageName;
    private final String validatorClassName;
    private final String dataClassName;
    private final String dataClassJavaType;
    private final String dataClassFqn;
    private final List<String> imports;
    private final List<String> staticImports;
    private final List<ValidatorCheckModel> checks;
    private final List<ConditionDependencyModel> conditionDeps;
    private final String runConditionsBody;
    private final String pathName;

    /**
     * v3.2 seat 3 (F9) — the type-format validator's alias-condition wing: one injected
     * condition-class field per DISTINCT alias condition the validated type's attributes walk
     * through (upstream {@code conditionDependencies}, first-appearance order), rendered
     * {@code @Inject} / {@code protected <typeName> <fieldName>;}.
     */
    public record ConditionDependencyModel(String typeName, String fieldName) {
        public String getTypeName() { return typeName; }
        public String getFieldName() { return fieldName; }
    }

    /**
     * @param dataClassName the rosetta type name — used in the golden's
     *     success/failure STRING literals, which stay simple even when the Java
     *     type is FQN-inlined
     * @param dataClassJavaType the data class as referenced at Java TYPE positions:
     *     the simple name normally; the FQN when the simple name collides with an
     *     implicitly-imported {@code java.lang} type (golden refuses to import e.g.
     *     {@code iso20022.dtcc.rds.harmonized.Error} and fully qualifies every type
     *     reference — the #306 collision law applied to the validator's own subject)
     */
    public ValidatorTemplateModel(String packageName, String validatorClassName,
                                   String dataClassName, String dataClassJavaType,
                                   String dataClassFqn,
                                   List<String> imports, List<String> staticImports,
                                   List<ValidatorCheckModel> checks) {
        this(packageName, validatorClassName, dataClassName, dataClassJavaType, dataClassFqn,
                imports, staticImports, checks, List.of(), "");
    }

    /**
     * The type-format form with the alias-condition wing (v3.2 seat 3, F9): {@code conditionDeps}
     * empty and {@code runConditionsBody} blank render the simple form byte-for-byte as before;
     * a non-empty dependency list renders the {@code @Inject} fields, the {@code runConditions}
     * method around the generator-computed body (every line carrying its own indentation tabs
     * and newline — the datarule class-body convention) and the {@code Streams.concat} form of
     * {@code getValidationResults}.
     */
    public ValidatorTemplateModel(String packageName, String validatorClassName,
                                   String dataClassName, String dataClassJavaType,
                                   String dataClassFqn,
                                   List<String> imports, List<String> staticImports,
                                   List<ValidatorCheckModel> checks,
                                   List<ConditionDependencyModel> conditionDeps,
                                   String runConditionsBody) {
        this(packageName, validatorClassName, dataClassName, dataClassJavaType, dataClassFqn,
                imports, staticImports, checks, conditionDeps, runConditionsBody, "path");
    }

    /**
     * The full form (v3.2 seat 3, round-1 cq MF-2): {@code pathName} is the validator's
     * class-scope {@code path} identifier as upstream's scope names it — {@code path} unless an
     * injected condition-class field wants the same name (a condition class named {@code Path}),
     * when both are numbered ({@code path0} the parameter, {@code path1} the field; oracle
     * group alias-conditions-reserved). Rendered at every {@code path} position of the file.
     */
    public ValidatorTemplateModel(String packageName, String validatorClassName,
                                   String dataClassName, String dataClassJavaType,
                                   String dataClassFqn,
                                   List<String> imports, List<String> staticImports,
                                   List<ValidatorCheckModel> checks,
                                   List<ConditionDependencyModel> conditionDeps,
                                   String runConditionsBody, String pathName) {
        this.packageName = packageName;
        this.validatorClassName = validatorClassName;
        this.dataClassName = dataClassName;
        this.dataClassJavaType = dataClassJavaType;
        this.dataClassFqn = dataClassFqn;
        this.imports = List.copyOf(imports);
        this.staticImports = staticImports != null ? List.copyOf(staticImports) : List.of();
        this.checks = List.copyOf(checks);
        this.conditionDeps = conditionDeps != null ? List.copyOf(conditionDeps) : List.of();
        this.runConditionsBody = runConditionsBody != null ? runConditionsBody : "";
        this.pathName = pathName != null ? pathName : "path";
    }

    /** The alias-condition dependencies; EMPTY renders the simple form (ST4 treats an empty list as false). */
    public List<ConditionDependencyModel> getConditionDeps() { return conditionDeps; }
    /** The generator-computed {@code runConditions} statements, or blank. */
    public String getRunConditionsBody() { return runConditionsBody; }
    /** The class-scope {@code path} identifier's actual name ({@code path} unless numbered). */
    public String getPathName() { return pathName; }

    public String getPackageName() { return packageName; }
    public String getValidatorClassName() { return validatorClassName; }
    public String getDataClassName() { return dataClassName; }
    public String getDataClassJavaType() { return dataClassJavaType; }
    public String getDataClassFqn() { return dataClassFqn; }
    public List<String> getImports() { return imports; }
    public List<String> getStaticImports() { return staticImports; }
    public List<ValidatorCheckModel> getChecks() { return checks; }
}
