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
 * THE CARDINALITY VALIDATOR, FROM THE IR ALONE (v3.3 seat 9, PR #645 commit 12) - the
 * {@link IRTypeUnit.Member#CARDINALITY_VALIDATOR} member of the type unit.
 *
 * <p>The file is a {@code Validator} with one {@code checkCardinality(...)} per effective member, SKIPPING the
 * fully unbounded ({@code 0..*}) ones; a bounded-below unbounded member ({@code 1..*}) renders with the {@code 0}
 * "no upper bound" sentinel.
 *
 * <p><b>EVERY FACT IS AN IR READ</b> - see {@link IROnlyExistsValidatorEmitter}'s class javadoc for the law and the
 * list of shared PURE text machinery. The bounds and the fully-unbounded skip are
 * {@link IRDerivedFacts#bounds}'s, reconciled per property as {@code cardinality.<p>.bounds} and
 * {@code cardinality.<p>.skipped} since PR #644; the rendering laws below are ported from
 * {@code CardinalityValidatorGenerator.buildModel} ({@code :109-168}) and each names the line it copies.
 */
final class IRCardinalityValidatorEmitter implements IRTypeUnit.MemberEmitter {

    private static final String TEMPLATE_GROUP = "templates/ir-java-validator-cardinality.stg";

    /** {@code CardinalityValidatorGenerator.CARDINALITY_TOKENS} ({@code :56-62}) - the template's token census. */
    private static final Map<String, String> CARDINALITY_TOKENS = ImportCollisionResolver.typeRefs(List.of(
            "com.google.common.collect.Lists",
            "com.rosetta.model.lib.expression.ComparisonResult",
            "com.rosetta.model.lib.path.RosettaPath",
            "com.rosetta.model.lib.validation.ValidationResult",
            "com.rosetta.model.lib.validation.Validator",
            "java.util.List"));

    private final IRTypeIndex index;
    private final IRDerivedFacts facts;
    private final TemplateRenderer renderer;

    IRCardinalityValidatorEmitter(IRTypeIndex index, IRDerivedFacts facts) {
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

    /** The whole file - {@code CardinalityValidatorGenerator.generate} ({@code :92-104}), the D50 two-step. */
    String render(IRTypeNode node) {
        ValidatorTemplateModel model = buildModel(node);
        String classText = renderer.render(TEMPLATE_GROUP, "cardinalityValidatorBody",
                "m", model, "t", CARDINALITY_TOKENS);
        ImportCollisionResolver.ClassResolution resolved = ImportCollisionResolver.resolveClass(
                classText, model.getPackageName() + "." + model.getValidatorClassName(), model.getImports());
        return renderer.render(TEMPLATE_GROUP, "cardinalityValidator", "m", new PojoTemplateModel(
                model.getPackageName(), resolved.imports(), model.getStaticImports(), resolved.classText()));
    }

    /** {@code CardinalityValidatorGenerator.buildModel} ({@code :109-168}) over the IR surface. */
    ValidatorTemplateModel buildModel(IRTypeNode node) {
        IRValidatorScan.Surface surface = IRValidatorScan.scan(node, index, facts);
        String dataClassName = IRTypeUnit.simpleName(node);                             // :111
        String validatorClassName =
                IRValidatorScan.validatorClassName(node, IRTypeUnit.Member.CARDINALITY_VALIDATOR);   // :112
        String packageName =
                IRValidatorScan.validatorPackage(node, IRTypeUnit.Member.CARDINALITY_VALIDATOR);     // :113
        String dataClassFqn = IRValidatorScan.dataClassFqn(node);                       // :114
        boolean collides = facts.collides(dataClassName);                               // :118
        String dataClassJavaType = IRValidatorScan.dataClassJavaType(dataClassFqn, collides);        // :121

        var imports = new ImportCollector(packageName);                                 // :123-132
        if (!collides) {
            imports.addImport(dataClassFqn);
        }
        imports.addImport("com.google.common.collect.Lists");
        imports.addImport("com.rosetta.model.lib.expression.ComparisonResult");
        imports.addImport("com.rosetta.model.lib.path.RosettaPath");
        imports.addImport("com.rosetta.model.lib.validation.ValidationResult");
        imports.addImport("com.rosetta.model.lib.validation.Validator");
        imports.addImport("java.util.List");

        List<IRValidatorScan.Scanned> checked = new ArrayList<>();                      // :134-137
        for (IRValidatorScan.Scanned attr : surface.scanned()) {
            if (!attr.fullyUnbounded()) {
                checked.add(attr);
            }
        }

        var staticImports = new ImportCollector(packageName);                           // :139-149
        staticImports.addStaticImport("com.google.common.base.Strings.isNullOrEmpty");
        if (!checked.isEmpty()) {
            // the golden drops the checkCardinality static import when the check list is empty (nothing
            // references it - upstream's ImportingStringConcatenation imports only what is used)
            staticImports.addStaticImport(
                    "com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality");
        }
        staticImports.addStaticImport("com.rosetta.model.lib.validation.ValidationResult.failure");
        staticImports.addStaticImport("com.rosetta.model.lib.validation.ValidationResult.success");
        staticImports.addStaticImport("java.util.stream.Collectors.toList");

        var checks = new ArrayList<ValidatorCheckModel>();                              // :151-163
        for (int i = 0; i < checked.size(); i++) {
            IRValidatorScan.Scanned attr = checked.get(i);
            IRValidatorScan.addCastImports(imports, attr.prop());
            boolean isMulti = attr.prop().multi();
            String valueExpr = isMulti
                    ? "(" + attr.castType() + ") " + attr.getterExpr()
                            + " == null ? 0 : " + attr.getterExpr() + ".size()"
                    : "(" + attr.castType() + ") " + attr.getterExpr() + " != null ? 1 : 0";
            String checkExpr = "checkCardinality(\"" + attr.name() + "\", " + valueExpr
                    + ", " + attr.min() + ", " + attr.max() + ")";
            checks.add(new ValidatorCheckModel(checkExpr, i == checked.size() - 1));
        }

        return new ValidatorTemplateModel(packageName, validatorClassName,              // :165-167
                dataClassName, dataClassJavaType, dataClassFqn,
                imports.getImports(), staticImports.getStaticImports(), checks);
    }
}
