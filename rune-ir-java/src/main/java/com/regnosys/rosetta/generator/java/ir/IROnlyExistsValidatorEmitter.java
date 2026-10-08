package com.regnosys.rosetta.generator.java.ir;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.regnosys.rosetta.generator.java.template.ImportCollector;
import com.regnosys.rosetta.generator.java.template.ImportCollisionResolver;
import com.regnosys.rosetta.generator.java.template.TemplateRenderer;
import com.regnosys.rosetta.generator.java.template.model.PojoTemplateModel;
import com.regnosys.rosetta.generator.java.template.model.ValidatorCheckModel;
import com.regnosys.rosetta.generator.java.template.model.ValidatorTemplateModel;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;

/**
 * THE ONLY-EXISTS VALIDATOR, FROM THE IR ALONE (v3.3 seat 9, PR #645 commit 12) - the
 * {@link IRTypeUnit.Member#ONLY_EXISTS_VALIDATOR} member of the type unit.
 *
 * <p>The file is a {@code ValidatorWithArg} with one
 * {@code .put("name", ExistenceChecker.isSet((cast) o.getX()))} per effective member - ALL members including the
 * fully unbounded ones (golden-verified: {@code Party}'s {@code person} / {@code personRole} are present here and
 * absent from the cardinality validator) - followed by the fixed set-comparison boilerplate.
 *
 * <p><b>EVERY FACT IS AN IR READ.</b> The member list and the synthetic-{@code meta} skip, the cast text, the
 * getter name and the {@code java.lang} collision come from {@link IRValidatorScan}; the rendering laws below are
 * ported method for method from {@code OnlyExistsValidatorGenerator.buildModel} ({@code :105-155}) and each names
 * the line it copies. NOTHING reads an AST node, a {@code GeneratorModel} or a workspace, and nothing calls a
 * legacy generator's model-reading path: the only shared code is PURE TEXT machinery ({@link ImportCollector},
 * {@link ImportCollisionResolver}, {@link TemplateRenderer} and the three template-model carriers), which reads no
 * model. A section it cannot render THROWS by name rather than guessing.
 *
 * <p>The template is a BYTE-COPY of the old generator's ({@code templates/ir-java-validator-onlyexists.stg} of
 * {@code templates/java-validator-onlyexists.stg}, as {@code ir-java-pojo.stg} is of the POJO's), so a change to
 * the old template cannot silently move the IR route.
 */
final class IROnlyExistsValidatorEmitter implements IRTypeUnit.MemberEmitter {

    private static final String TEMPLATE_GROUP = "templates/ir-java-validator-onlyexists.stg";

    /**
     * {@code OnlyExistsValidatorGenerator.ONLY_EXISTS_TOKENS} ({@code :51-58}) - the library types the class-body
     * template writes, each a first-claim sentinel under its simple name. The list IS the template's token census.
     */
    private static final Map<String, String> ONLY_EXISTS_TOKENS = ImportCollisionResolver.typeRefs(List.of(
            "com.google.common.collect.ImmutableMap",
            "com.rosetta.model.lib.path.RosettaPath",
            "com.rosetta.model.lib.validation.ValidationResult",
            "com.rosetta.model.lib.validation.ValidatorWithArg",
            "java.util.Map",
            "java.util.Set",
            "java.util.stream.Collectors"));

    private final IRTypeIndex index;
    private final IRDerivedFacts facts;
    private final TemplateRenderer renderer;

    IROnlyExistsValidatorEmitter(IRTypeIndex index, IRDerivedFacts facts) {
        this.index = Objects.requireNonNull(index, "index");
        this.facts = Objects.requireNonNull(facts, "facts");
        this.renderer = new TemplateRenderer();
        this.renderer.loadGroupFromClasspath(TEMPLATE_GROUP);
    }

    /** Every validated type writes this file - the member never answers "no file by law". */
    @Override
    public Optional<String> emit(IRTypeNode node) {
        return Optional.of(render(node));
    }

    /**
     * The whole file. {@code OnlyExistsValidatorGenerator.generate} ({@code :88-100}), the D50 two-step: the class
     * text first with every TYPE position a first-claim sentinel, resolved in text order from the ONE seed the
     * validator class is, the losers' imports dropped, then the file wrapper.
     */
    String render(IRTypeNode node) {
        ValidatorTemplateModel model = buildModel(node);
        String classText = renderer.render(TEMPLATE_GROUP, "onlyExistsValidatorBody",
                "m", model, "t", ONLY_EXISTS_TOKENS);
        ImportCollisionResolver.ClassResolution resolved = ImportCollisionResolver.resolveClass(
                classText, model.getPackageName() + "." + model.getValidatorClassName(), model.getImports());
        return renderer.render(TEMPLATE_GROUP, "onlyExistsValidator", "m", new PojoTemplateModel(
                model.getPackageName(), resolved.imports(), model.getStaticImports(), resolved.classText()));
    }

    /** {@code OnlyExistsValidatorGenerator.buildModel} ({@code :105-155}) over the IR surface. */
    ValidatorTemplateModel buildModel(IRTypeNode node) {
        IRValidatorScan.Surface surface = IRValidatorScan.scan(node, index, facts);
        String dataClassName = IRTypeUnit.simpleName(node);                             // :107
        String validatorClassName =
                IRValidatorScan.validatorClassName(node, IRTypeUnit.Member.ONLY_EXISTS_VALIDATOR);   // :108
        String packageName =
                IRValidatorScan.validatorPackage(node, IRTypeUnit.Member.ONLY_EXISTS_VALIDATOR);     // :109
        String dataClassFqn = IRValidatorScan.dataClassFqn(node);                       // :110
        boolean collides = facts.collides(dataClassName);                               // :115
        String dataClassJavaType = IRValidatorScan.dataClassJavaType(dataClassFqn, collides);        // :118

        var imports = new ImportCollector(packageName);                                 // :120-130
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

        var staticImports = new ImportCollector(packageName);                           // :132-134
        staticImports.addStaticImport("com.rosetta.model.lib.validation.ValidationResult.failure");
        staticImports.addStaticImport("com.rosetta.model.lib.validation.ValidationResult.success");

        var checks = new ArrayList<ValidatorCheckModel>();                              // :136-144
        for (IRValidatorScan.Scanned attr : surface.scanned()) {
            IRValidatorScan.addCastImports(imports, attr.prop());
            // v3.2 seat 11 (D50): the checker is a first-claim sentinel like the cast it wraps
            String putExpr = ".put(\"" + attr.name()
                    + "\", " + ImportCollisionResolver.typeRefOrBare(
                            "com.rosetta.model.lib.validation.ExistenceChecker")
                    + ".isSet((" + attr.castType() + ") " + attr.getterExpr() + "))";
            checks.add(new ValidatorCheckModel(putExpr, false));
        }
        if (!checks.isEmpty()) {                                                        // :145-150
            // the golden drops the ExistenceChecker import for zero-attribute types (the used-only import law;
            // golden-verified MasterAgreementBaseOnlyExistsValidator)
            imports.addImport("com.rosetta.model.lib.validation.ExistenceChecker");
        }

        return new ValidatorTemplateModel(packageName, validatorClassName,              // :152-154
                dataClassName, dataClassJavaType, dataClassFqn,
                imports.getImports(), staticImports.getStaticImports(), checks);
    }
}
