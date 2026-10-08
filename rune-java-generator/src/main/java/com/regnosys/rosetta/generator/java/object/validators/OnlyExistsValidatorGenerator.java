package com.regnosys.rosetta.generator.java.object.validators;

import java.util.ArrayList;
import java.util.stream.Stream;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.JavaClassGenerator;
import com.regnosys.rosetta.generator.java.template.ImportCollector;
import com.regnosys.rosetta.generator.java.template.ImportCollisionResolver;
import com.regnosys.rosetta.generator.java.template.TemplateRenderer;
import com.regnosys.rosetta.generator.java.template.model.PojoTemplateModel;
import com.regnosys.rosetta.generator.java.template.model.ValidatorCheckModel;
import com.regnosys.rosetta.generator.java.template.model.ValidatorTemplateModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RGeneratedJavaClass;
import com.regnosys.rosetta.generator.java.types.RJavaPojoInterface;
import com.rosetta.model.lib.ModelSymbolId;

/**
 * Generates a {@code ValidatorWithArg} implementation that checks only-exists
 * constraints: one {@code .put("name", ExistenceChecker.isSet((Cast) o.getX()))}
 * entry per effective attribute — ALL attributes including unbounded {@code 0..*}
 * (golden-verified: {@code Party}'s {@code person}/{@code personRole} present here,
 * absent from the cardinality validator) — followed by the fixed set-comparison
 * boilerplate. No version javadoc.
 *
 * <p>Covers every {@code Data} AND every {@code choice} type (the golden quartet
 * convention); choice options check under their PascalCase option names. Casts and
 * getter names come from the byte-proven POJO property machinery via
 * {@link ValidatorScan} — meta-wrapped attributes stay wrapped
 * ({@code (FieldWithMetaDate) o.getAdjustedDate()}).
 *
 * <p>Output must match upstream Xtend OnlyExistsValidatorGenerator at 9.83.0 (D11).
 * Uses ST4 template {@code java-validator-onlyexists.stg} via {@link TemplateRenderer}.
 *
 * <p>Migrated from StringBuilder to ST4 as part of M8 (D15); rebuilt on the POJO
 * property surface + wired into the D11 gate at PR #405 (coverage burn-down wave A).
 */
public class OnlyExistsValidatorGenerator extends JavaClassGenerator<RRootElement, RGeneratedJavaClass<?>> {

    private static final String TEMPLATE_GROUP = "templates/java-validator-onlyexists.stg";
    /**
     * v3.2 seat 11 (D50): the library types the class-body template writes, each a first-claim sentinel
     * under its simple name ({@code <t.List>} ...). The list IS the template's token census - a token the
     * template writes that this list lacks renders empty, which the D11 and the hold-out bars catch on
     * every only-exists validator. Public for the template tests, which render the two steps as generate() does.
     */
    public static final java.util.Map<String, String> ONLY_EXISTS_TOKENS = ImportCollisionResolver.typeRefs(java.util.List.of(
            "com.google.common.collect.ImmutableMap",
            "com.rosetta.model.lib.path.RosettaPath",
            "com.rosetta.model.lib.validation.ValidationResult",
            "com.rosetta.model.lib.validation.ValidatorWithArg",
            "java.util.Map",
            "java.util.Set",
            "java.util.stream.Collectors"));

    private final GeneratorModel generatorModel;
    private final JavaTypeTranslator typeTranslator;
    private final JavaTypeUtil typeUtil;
    private final TemplateRenderer renderer;

    public OnlyExistsValidatorGenerator(GeneratorModel generatorModel,
                                        JavaTypeTranslator typeTranslator, JavaTypeUtil typeUtil) {
        this.generatorModel = generatorModel;
        this.typeTranslator = typeTranslator;
        this.typeUtil = typeUtil;
        this.renderer = new TemplateRenderer();
        this.renderer.loadGroupFromClasspath(TEMPLATE_GROUP);
    }

    @Override
    protected Stream<? extends RRootElement> streamObjects(RModel model) {
        return model.rootElements().stream()
                .filter(ValidatorScan::isValidatedType)
                .map(e -> (RRootElement) e);
    }

    @Override
    protected RGeneratedJavaClass<?> createTypeRepresentation(RRootElement element) {
        ModelSymbolId typeId = new ModelSymbolId(generatorModel.namespace(element),
                ValidatorScan.elementName(element));
        return typeTranslator.toOnlyExistsValidatorClass(typeId);
    }

    @Override
    protected String generate(RRootElement element, RGeneratedJavaClass<?> validatorClass, String version) {
        ValidatorTemplateModel model = buildModel(element, validatorClass);
        // v3.2 seat 11 (D50 - the file-scope first-claim law): the class text first, every TYPE position a
        // sentinel; resolved in text order from the ONE seed upstream's JavaClassScope registers before
        // writing a byte (the validator class itself); the losers' imports dropped; then the file wrapper.
        // A validator with no collision renders the pre-seat bytes.
        String classText = renderer.render(TEMPLATE_GROUP, "onlyExistsValidatorBody", "m", model, "t", ONLY_EXISTS_TOKENS);
        ImportCollisionResolver.ClassResolution resolved = ImportCollisionResolver.resolveClass(
                classText, model.getPackageName() + "." + model.getValidatorClassName(), model.getImports());
        return renderer.render(TEMPLATE_GROUP, "onlyExistsValidator", "m", new PojoTemplateModel(
                model.getPackageName(), resolved.imports(), model.getStaticImports(), resolved.classText()));
    }

    /**
     * Build the template model from the element's effective POJO property surface.
     */
    ValidatorTemplateModel buildModel(RRootElement element, RGeneratedJavaClass<?> validatorClass) {
        RJavaPojoInterface pojo = ValidatorScan.toPojo(element, generatorModel, typeTranslator, typeUtil);
        String dataClassName = pojo.getSimpleName();
        String validatorClassName = validatorClass.getSimpleName();
        String packageName = validatorClass.getPackageName().withDots();
        String dataClassFqn = pojo.getCanonicalName().withDots();
        // The #306 collision law applied to the subject type itself: a data class whose
        // simple name collides with java.lang (iso `Error`) is never imported — every
        // Java TYPE position fully qualifies; the success/failure STRING literals keep
        // the simple name (golden-verified ErrorOnlyExistsValidator/ErrorValidator).
        boolean collides = ValidatorScan.collidesWithJavaLang(dataClassName);
        // v3.2 seat 11 (D50): the data class is a first-claim sentinel at every TYPE position unless the
        // #306 java.lang law already writes it canonical (the string positions keep dataClassName)
        String dataClassJavaType = collides ? dataClassFqn : ImportCollisionResolver.typeRefOrBare(dataClassFqn);

        var imports = new ImportCollector(packageName);
        if (!collides) {
            imports.addImport(dataClassFqn);
        }
        imports.addImport("com.google.common.collect.ImmutableMap");
        imports.addImport("com.rosetta.model.lib.path.RosettaPath");
        imports.addImport("com.rosetta.model.lib.validation.ValidationResult");
        imports.addImport("com.rosetta.model.lib.validation.ValidatorWithArg");
        imports.addImport("java.util.Map");
        imports.addImport("java.util.Set");
        imports.addImport("java.util.stream.Collectors");

        var staticImports = new ImportCollector(packageName);
        staticImports.addStaticImport("com.rosetta.model.lib.validation.ValidationResult.failure");
        staticImports.addStaticImport("com.rosetta.model.lib.validation.ValidationResult.success");

        var checks = new ArrayList<ValidatorCheckModel>();
        for (ValidatorScan.ScannedAttribute attr : ValidatorScan.scan(element, pojo, generatorModel, typeUtil)) {
            ValidatorScan.addCastImports(imports, attr.prop(), typeUtil);
            // v3.2 seat 11 (D50): the checker is a first-claim sentinel like the cast it wraps
            String putExpr = ".put(\"" + attr.name()
                    + "\", " + ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.validation.ExistenceChecker")
                    + ".isSet((" + attr.castType() + ") " + attr.getterExpr() + "))";
            checks.add(new ValidatorCheckModel(putExpr, false));
        }
        if (!checks.isEmpty()) {
            // The golden drops the ExistenceChecker import for zero-attribute types
            // (nothing references it — the used-only import law; golden-verified
            // MasterAgreementBaseOnlyExistsValidator).
            imports.addImport("com.rosetta.model.lib.validation.ExistenceChecker");
        }

        return new ValidatorTemplateModel(packageName, validatorClassName,
                dataClassName, dataClassJavaType, dataClassFqn,
                imports.getImports(), staticImports.getStaticImports(), checks);
    }
}
