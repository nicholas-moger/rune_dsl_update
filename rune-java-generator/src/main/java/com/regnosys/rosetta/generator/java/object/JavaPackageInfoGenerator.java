package com.regnosys.rosetta.generator.java.object;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.template.TemplateRenderer;
import com.regnosys.rosetta.generator.java.template.model.PackageInfoModel;

/**
 * Generates {@code package-info.java} files for each namespace that has
 * at least one model with a definition. Collects descriptions across all
 * models in a namespace and produces a Javadoc comment.
 *
 * <p>Unlike other generators, this does not extend {@link
 * com.regnosys.rosetta.generator.java.JavaClassGenerator JavaClassGenerator}
 * because it generates one file per namespace, not per element.
 */
public class JavaPackageInfoGenerator {

    private static final String TEMPLATE_GROUP = "templates/java-package-info.stg";

    private final GeneratorModel generatorModel;
    private final TemplateRenderer renderer;

    public JavaPackageInfoGenerator(GeneratorModel generatorModel) {
        this.generatorModel = generatorModel;
        this.renderer = new TemplateRenderer();
        this.renderer.loadGroupFromClasspath(TEMPLATE_GROUP, '$', '$');
    }

    /**
     * Generate package-info.java files for all namespaces.
     *
     * @param output map to collect generated files: path → source
     */
    public void generatePackageInfoClasses(Map<String, String> output) {
        Map<String, List<String>> nsToDescriptions = namespaceToDescriptionMap();
        nsToDescriptions.forEach((packageName, descriptions) -> {
            if (descriptions != null && !descriptions.isEmpty()) {
                String path = packageName.replace('.', '/') + "/package-info.java";
                output.put(path, generatePackageInfo(packageName, descriptions));
            }
        });
    }

    private Map<String, List<String>> namespaceToDescriptionMap() {
        // Upstream collects into a Guava LinkedHashMultimap<namespace, definition>,
        // which (a) keeps first-insertion order across BOTH keys and values (= the
        // full-path-sorted file-walk order, ordering law 2) and (b) DEDUPES exact
        // (namespace, definition) pairs — N files in one namespace sharing one
        // verbatim definition contribute ONE description (golden-verified: every
        // iso20022 namespace; the PR #405 wave-A audit measured 0/16 identical
        // from duplicate descriptions alone). LinkedHashSet mirrors both.
        //
        // Emission gating: only models the cell actually emits contribute —
        // resolution-only dependency closures (DRR's transitive CDM/ISO trees,
        // cdm6's transitive fpml) and builtins must not mint package-info files
        // (the audit's 39 cdm6 noGolden files were all fpml.* dependency
        // namespaces). Same shouldGenerate gate every per-model generator uses.
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (RModel model : generatorModel.files()) {
            if (!generatorModel.shouldGenerate(model)) {
                continue;
            }
            String definition = model.definition().orElse(null);
            if (definition != null) {
                result.computeIfAbsent(model.namespace(), k -> new LinkedHashSet<>())
                        .add(definition);
            }
        }
        Map<String, List<String>> ordered = new LinkedHashMap<>();
        result.forEach((ns, defs) -> ordered.put(ns, new ArrayList<>(defs)));
        return ordered;
    }

    /**
     * Generate package-info.java source via ST4 template.
     * Output must match upstream Xtend format byte-for-byte (D11).
     */
    private String generatePackageInfo(String packageName, List<String> descriptions) {
        var model = new PackageInfoModel(packageName, descriptions);
        return renderer.render(TEMPLATE_GROUP, "packageInfo", "model", model);
    }
}
