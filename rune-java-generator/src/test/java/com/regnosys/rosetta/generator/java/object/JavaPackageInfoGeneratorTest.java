package com.regnosys.rosetta.generator.java.object;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JavaPackageInfoGeneratorTest {

    @Test void generates_package_info_for_namespace_with_definition() {
        var model = makeModel("cdm.base.datetime",
                "Basic date and time concepts: relative date, date range, offset, business centre etc.");

        Map<String, String> output = generate(model);

        assertTrue(output.containsKey("cdm/base/datetime/package-info.java"));
        String source = output.get("cdm/base/datetime/package-info.java");
        assertTrue(source.contains("package cdm.base.datetime;"));
        assertTrue(source.contains("Basic date and time concepts"));
    }

    @Test void matches_golden_output() {
        var model = makeModel("cdm.base.datetime",
                "Basic date and time concepts: relative date, date range, offset, business centre etc.");

        Map<String, String> output = generate(model);
        String source = output.get("cdm/base/datetime/package-info.java");

        // Match exact upstream format
        String expected = "/**\n" +
                "*\t\n" +
                "*\tBasic date and time concepts: relative date, date range, offset, business centre etc.\n" +
                "*\t<p>\n" +
                "*\t\n" +
                "*\n" +
                "*/\n" +
                "\n" +
                "package cdm.base.datetime;\n";

        assertEquals(expected, source);
    }

    @Test void no_output_for_model_without_definition() {
        var model = new RModel();
        model.setNamespace("com.example");
        // No definition set

        Map<String, String> output = generate(model);

        assertTrue(output.isEmpty());
    }

    @Test void multiple_models_same_namespace() {
        var model1 = makeModel("com.example", "First description.");
        var model2 = makeModel("com.example", "Second description.");

        var result = RWorkspace.build(List.of(model1, model2));
        var gm = new GeneratorModel(result.workspace());
        var generator = new JavaPackageInfoGenerator(gm);
        Map<String, String> output = new LinkedHashMap<>();
        generator.generatePackageInfoClasses(output);

        assertEquals(1, output.size());
        String source = output.get("com/example/package-info.java");
        assertTrue(source.contains("First description."));
        assertTrue(source.contains("Second description."));
    }

    @Test void multiple_namespaces() {
        var model1 = makeModel("com.example.a", "Package A.");
        var model2 = makeModel("com.example.b", "Package B.");

        var result = RWorkspace.build(List.of(model1, model2));
        var gm = new GeneratorModel(result.workspace());
        var generator = new JavaPackageInfoGenerator(gm);
        Map<String, String> output = new LinkedHashMap<>();
        generator.generatePackageInfoClasses(output);

        assertEquals(2, output.size());
        assertTrue(output.containsKey("com/example/a/package-info.java"));
        assertTrue(output.containsKey("com/example/b/package-info.java"));
    }

    // === Helpers ==============================================================

    private Map<String, String> generate(RModel model) {
        var result = RWorkspace.build(List.of(model));
        var gm = new GeneratorModel(result.workspace());
        var generator = new JavaPackageInfoGenerator(gm);
        Map<String, String> output = new LinkedHashMap<>();
        generator.generatePackageInfoClasses(output);
        return output;
    }

    private RModel makeModel(String namespace, String definition) {
        var model = new RModel();
        model.setNamespace(namespace);
        model.setDefinition(definition);
        return model;
    }
}
