package com.regnosys.rosetta.generator.java.object.validators;

import java.util.ArrayList;
import java.util.List;
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
 * Generates a {@code Validator} implementation that checks cardinality constraints:
 * one {@code checkCardinality(...)} entry per effective attribute, SKIPPING fully
 * unbounded ({@code 0..*}) attributes (golden-verified: {@code Party}'s
 * {@code person}/{@code personRole} absent here, present in the only-exists
 * validator). A bounded-below unbounded attribute ({@code 1..*}) renders with the
 * {@code 0} "no upper bound" sentinel ({@code 1, 0}). No version javadoc.
 *
 * <p>Covers every {@code Data} AND every {@code choice} type; choice options check
 * as {@code 0, 1} under their PascalCase option names. Casts and getter names come
 * from the byte-proven POJO property surface via {@link ValidatorScan}: single
 * {@code (X) o.getX() != null ? 1 : 0}; list
 * {@code (List<? extends X>) o.getX() == null ? 0 : o.getX().size()} (wildcard for
 * model types only); meta-wrapped attributes stay wrapped
 * ({@code (FieldWithMetaString) o.getName()}).
 *
 * <p>Output must match upstream Xtend CardinalityValidatorGenerator at 9.83.0 (D11).
 * Uses ST4 template {@code java-validator-cardinality.stg} via {@link TemplateRenderer};
 * entries separate with {@code ", "} (comma + trailing space, template-driven via
 * {@code isLast}); an empty check list renders {@code newArrayList(\n\t\t\t);}.
 *
 * <p>Migrated from StringBuilder to ST4 as part of M8 (D15); rebuilt on the POJO
 * property surface + wired into the D11 gate at PR #405 (coverage burn-down wave A).
 */
public class CardinalityValidatorGenerator extends JavaClassGenerator<RRootElement, RGeneratedJavaClass<?>> {

    private static final String TEMPLATE_GROUP = "templates/java-validator-cardinality.stg";
    /**
     * v3.2 seat 11 (D50): the library types the class-body template writes, each a first-claim sentinel
     * under its simple name ({@code <t.List>} ...). The list IS the template's token census - a token the
     * template writes that this list lacks renders empty, which the D11 and the hold-out bars catch on
     * every cardinality validator. Public for the template tests, which render the two steps as generate() does.
     */
    public static final java.util.Map<String, String> CARDINALITY_TOKENS = ImportCollisionResolver.typeRefs(java.util.List.of(
            "com.google.common.collect.Lists",
            "com.rosetta.model.lib.expression.ComparisonResult",
            "com.rosetta.model.lib.path.RosettaPath",
            "com.rosetta.model.lib.validation.ValidationResult",
            "com.rosetta.model.lib.validation.Validator",
            "java.util.List"));

    private final GeneratorModel generatorModel;
    private final JavaTypeTranslator typeTranslator;
    private final JavaTypeUtil typeUtil;
    private final TemplateRenderer renderer;

    public CardinalityValidatorGenerator(GeneratorModel generatorModel,
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
        return typeTranslator.toValidatorClass(typeId);
    }

    @Override
    protected String generate(RRootElement element, RGeneratedJavaClass<?> validatorClass, String version) {
        ValidatorTemplateModel model = buildModel(element, validatorClass);
        // v3.2 seat 11 (D50 - the file-scope first-claim law): the class text first, every TYPE position a
        // sentinel; resolved in text order from the ONE seed upstream's JavaClassScope registers before
        // writing a byte (the validator class itself); the losers' imports dropped; then the file wrapper.
        // A validator with no collision renders the pre-seat bytes.
        String classText = renderer.render(TEMPLATE_GROUP, "cardinalityValidatorBody", "m", model, "t", CARDINALITY_TOKENS);
        ImportCollisionResolver.ClassResolution resolved = ImportCollisionResolver.resolveClass(
                classText, model.getPackageName() + "." + model.getValidatorClassName(), model.getImports());
        return renderer.render(TEMPLATE_GROUP, "cardinalityValidator", "m", new PojoTemplateModel(
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
        // The #306 collision law applied to the subject type itself — see
        // OnlyExistsValidatorGenerator.buildModel (golden-verified ErrorValidator:
        // no data-class import; every TYPE position FQN; string literals simple).
        boolean collides = ValidatorScan.collidesWithJavaLang(dataClassName);
        // v3.2 seat 11 (D50): the data class is a first-claim sentinel at every TYPE position unless the
        // #306 java.lang law already writes it canonical (the string positions keep dataClassName)
        String dataClassJavaType = collides ? dataClassFqn : ImportCollisionResolver.typeRefOrBare(dataClassFqn);

        var imports = new ImportCollector(packageName);
        if (!collides) {
            imports.addImport(dataClassFqn);
        }
        imports.addImport("com.google.common.collect.Lists");
        imports.addImport("com.rosetta.model.lib.expression.ComparisonResult");
        imports.addImport("com.rosetta.model.lib.path.RosettaPath");
        imports.addImport("com.rosetta.model.lib.validation.ValidationResult");
        imports.addImport("com.rosetta.model.lib.validation.Validator");
        imports.addImport("java.util.List");

        List<ValidatorScan.ScannedAttribute> checked =
                ValidatorScan.scan(element, pojo, generatorModel, typeUtil).stream()
                        .filter(attr -> !attr.isFullyUnbounded())
                        .toList();

        var staticImports = new ImportCollector(packageName);
        staticImports.addStaticImport("com.google.common.base.Strings.isNullOrEmpty");
        if (!checked.isEmpty()) {
            // The golden drops the checkCardinality static import when the check list
            // is empty (nothing references it — upstream's ImportingStringConcatenation
            // imports only what is used; the boilerplate keeps the other four).
            staticImports.addStaticImport("com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality");
        }
        staticImports.addStaticImport("com.rosetta.model.lib.validation.ValidationResult.failure");
        staticImports.addStaticImport("com.rosetta.model.lib.validation.ValidationResult.success");
        staticImports.addStaticImport("java.util.stream.Collectors.toList");

        var checks = new ArrayList<ValidatorCheckModel>();
        for (int i = 0; i < checked.size(); i++) {
            ValidatorScan.ScannedAttribute attr = checked.get(i);
            ValidatorScan.addCastImports(imports, attr.prop(), typeUtil);
            boolean isMulti = typeUtil.isList(attr.prop().getType());
            String valueExpr = isMulti
                    ? "(" + attr.castType() + ") " + attr.getterExpr()
                            + " == null ? 0 : " + attr.getterExpr() + ".size()"
                    : "(" + attr.castType() + ") " + attr.getterExpr() + " != null ? 1 : 0";
            String checkExpr = "checkCardinality(\"" + attr.name() + "\", " + valueExpr
                    + ", " + attr.min() + ", " + attr.max() + ")";
            checks.add(new ValidatorCheckModel(checkExpr, i == checked.size() - 1));
        }

        return new ValidatorTemplateModel(packageName, validatorClassName,
                dataClassName, dataClassJavaType, dataClassFqn,
                imports.getImports(), staticImports.getStaticImports(), checks);
    }
}
