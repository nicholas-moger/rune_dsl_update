package com.regnosys.rosetta.generator.java.template;

import com.regnosys.rosetta.generator.java.template.model.PackageInfoModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PackageInfoModelTest {

    @Test void builds_from_single_description() {
        var model = new PackageInfoModel("com.example", List.of("A description"));
        assertEquals("com.example", model.getPackageName());
        assertEquals(1, model.getDescriptions().size());
        assertEquals("A description", model.getDescriptions().get(0));
    }

    @Test void builds_from_multiple_descriptions() {
        var model = new PackageInfoModel("com.example",
                List.of("First", "Second"));
        assertEquals(2, model.getDescriptions().size());
        assertEquals("First", model.getDescriptions().get(0));
        assertEquals("Second", model.getDescriptions().get(1));
    }

    @Test void descriptions_are_immutable() {
        var original = new java.util.ArrayList<>(List.of("A"));
        var model = new PackageInfoModel("com.example", original);
        original.add("B");
        assertEquals(1, model.getDescriptions().size(),
                "Model should not be affected by mutations to the original list");
    }

    @Test void renders_via_template_to_match_golden() {
        var model = new PackageInfoModel("cdm.base.datetime",
                List.of("Basic date and time concepts: relative date, date range, offset, business centre etc."));

        var renderer = new TemplateRenderer();
        renderer.loadGroupFromClasspath("templates/java-package-info.stg", '$', '$');
        String rendered = renderer.render("templates/java-package-info.stg",
                "packageInfo", "model", model);

        String expected = "/**\n" +
                "*\t\n" +
                "*\tBasic date and time concepts: relative date, date range, offset, business centre etc.\n" +
                "*\t<p>\n" +
                "*\t\n" +
                "*\n" +
                "*/\n" +
                "\n" +
                "package cdm.base.datetime;\n";

        assertEquals(expected, rendered,
                "Template rendering must be byte-identical to golden output");
    }

    @Test void renders_multiple_descriptions() {
        var model = new PackageInfoModel("com.example",
                List.of("First description.", "Second description."));

        var renderer = new TemplateRenderer();
        renderer.loadGroupFromClasspath("templates/java-package-info.stg", '$', '$');
        String rendered = renderer.render("templates/java-package-info.stg",
                "packageInfo", "model", model);

        assertTrue(rendered.contains("First description."));
        assertTrue(rendered.contains("Second description."));
        assertTrue(rendered.contains("package com.example;"));
    }
}
