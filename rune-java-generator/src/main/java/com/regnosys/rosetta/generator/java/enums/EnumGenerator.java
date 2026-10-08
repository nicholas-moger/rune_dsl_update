package com.regnosys.rosetta.generator.java.enums;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.JavaClassGenerator;
import com.regnosys.rosetta.generator.java.template.ImportCollector;
import com.regnosys.rosetta.generator.java.template.JavaStringUtil;
import com.regnosys.rosetta.generator.java.template.TemplateRenderer;
import com.regnosys.rosetta.generator.java.template.model.EnumTemplateModel;
import com.regnosys.rosetta.generator.java.template.model.EnumValueModel;
import com.regnosys.rosetta.generator.java.template.model.SynonymModel;
import com.regnosys.rosetta.generator.java.types.RJavaEnum;
import com.regnosys.rosetta.generator.java.types.RJavaEnumValue;
import com.regnosys.rosetta.generator.java.util.ModelGeneratorUtil;
import com.rosetta.util.DottedPath;

/**
 * Generates a Java enum for each {@link REnumeration} in the model.
 * Output must be byte-identical to upstream Xtend EnumGenerator (D11).
 *
 * <p>Uses ST4 template {@code java-enum.stg} via {@link TemplateRenderer}.
 * The {@link #buildModel} method pre-computes all data needed by the template;
 * the template contains zero business logic, only formatting.
 *
 * <p>Migrated from StringBuilder to ST4 as part of M8 (D15).
 */
public class EnumGenerator extends JavaClassGenerator<REnumeration, RJavaEnum> {

    private static final String TEMPLATE_GROUP = "templates/java-enum.stg";

    private final GeneratorModel generatorModel;
    private final ModelGeneratorUtil generatorUtil;
    private final TemplateRenderer renderer;

    public EnumGenerator(GeneratorModel generatorModel) {
        this.generatorModel = generatorModel;
        this.generatorUtil = new ModelGeneratorUtil(generatorModel.workspace());
        this.renderer = new TemplateRenderer();
        this.renderer.loadGroupFromClasspath(TEMPLATE_GROUP);
    }

    @Override
    protected Stream<? extends REnumeration> streamObjects(RModel model) {
        return model.rootElements().stream()
                .filter(e -> e instanceof REnumeration)
                .map(e -> (REnumeration) e);
    }

    @Override
    protected RJavaEnum createTypeRepresentation(REnumeration enumeration) {
        DottedPath namespace = generatorModel.namespace(enumeration);
        return new RJavaEnum(enumeration, namespace);
    }

    @Override
    protected String generate(REnumeration enumeration, RJavaEnum javaEnum, String version) {
        EnumTemplateModel model = buildModel(enumeration, javaEnum, version);
        return renderer.render(TEMPLATE_GROUP, "enumFile", "m", model);
    }

    /**
     * Build the template model from AST + type representation.
     * All business logic (import computation, name formatting, javadoc rendering,
     * synonym/display name extraction) happens here — the template is pure formatting.
     */
    EnumTemplateModel buildModel(REnumeration enumeration, RJavaEnum javaEnum, String version) {
        String packageName = javaEnum.getPackageName().withDots();
        String enumName = javaEnum.getSimpleName();
        List<RJavaEnumValue> javaValues = javaEnum.getEnumValues();

        // Imports — must match upstream ordering exactly (sorted alphabetically)
        var imports = new ImportCollector(packageName);
        imports.addImport("com.rosetta.model.lib.annotations.RosettaEnum");
        imports.addImport("com.rosetta.model.lib.annotations.RosettaEnumValue");
        imports.addImport("java.util.Collections");
        imports.addImport("java.util.Map");
        imports.addImport("java.util.concurrent.ConcurrentHashMap");

        boolean hasSynonyms = javaValues.stream()
                .anyMatch(v -> !v.getAstNode().synonyms().isEmpty());
        if (hasSynonyms) {
            imports.addImport("com.rosetta.model.lib.annotations.RosettaSynonym");
        }

        // Javadoc — enum style (pojoStyle=false): upstream's enum generator renders its
        // own javadoc template with RAW multi-line continuations and no doc-reference
        // path lines (facet pojoOverrideNaming, PR #323 — the green cdm6
        // RatingPriorityResolutionEnum golden carries the raw form).
        String definition = enumeration.definition().orElse(null);
        String javadoc = generatorUtil.javadoc(definition, enumeration.docReferences(), version, false);

        // Values
        List<EnumValueModel> values = new ArrayList<>();
        for (int i = 0; i < javaValues.size(); i++) {
            RJavaEnumValue jv = javaValues.get(i);
            boolean isLast = (i == javaValues.size() - 1);

            // Definition — escape HTML
            String valDef = jv.getAstNode().definition().orElse(null);
            if (valDef != null && !valDef.isEmpty()) {
                valDef = ModelGeneratorUtil.escapeHtml(valDef);
            } else {
                valDef = null;
            }

            // Full javadoc with doc references (for enum values that have them)
            // Indent with tab since enum values are inside the enum body
            // (enum style — see the type-level javadoc note above)
            String valJavadoc = generatorUtil.javadoc(
                    jv.getAstNode().definition().orElse(null),
                    jv.getAstNode().docReferences(), null, false);
            if (valJavadoc != null) {
                valJavadoc = valJavadoc.lines()
                        .map(line -> "\t" + line)
                        .collect(java.util.stream.Collectors.joining("\n"));
            }

            // Synonyms - v3.2 seat 9 (D46, F1): RAW value and sources, the annotation seat's law (the
            // released plugin splices the model text into @RosettaSynonym unescaped - the oracle group
            // enum-unicode-display-edge's SynonymEnum, `value = "µ–syn"`); the escape used to sit here.
            List<SynonymModel> synonyms = new ArrayList<>();
            for (var synonym : jv.getAstNode().synonyms()) {
                synonyms.add(new SynonymModel(synonym.value(), synonym.sources()));
            }

            // v3.2 seat 9 (D46, F1 - the chaos a5uni family, 22 declared ENUM rows): ONE escape law, at the
            // CONSTRUCTOR argument alone. The released plugin renders the display name RAW inside the
            // @RosettaEnumValue annotation (non-ASCII kept; a quote / a backslash / a tab kept too - its own
            // non-compiling emission, EscapeEnum of the edge group, reproduced under the byte contract) and
            // Java-escaped as the constructor's string literal; the generator escaped once and java-enum.stg
            // read that one string at both seats. The rosetta name is an identifier, so its escape is a no-op
            // at either seat and stays as it was.
            String displayName = jv.getDisplayName();
            values.add(new EnumValueModel(
                    jv.getName(),
                    JavaStringUtil.escapeJava(jv.getRosettaName()),
                    displayName,
                    displayName != null ? JavaStringUtil.escapeJava(displayName) : null,
                    valDef, valJavadoc,
                    synonyms,
                    isLast));
        }

        return new EnumTemplateModel(packageName, enumName, version, javadoc,
                imports.getImports(), values, hasSynonyms);
    }
}
