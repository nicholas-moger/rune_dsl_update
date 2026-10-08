package com.regnosys.rosetta.generator.java.template;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TemplateRendererTest {

    private static final String GROUP = "templates/test-simple.stg";

    @Test void renders_simple_template() {
        var renderer = new TemplateRenderer();
        renderer.loadGroupFromClasspath(GROUP);

        var result = renderer.render(GROUP, "hello", "name", "World");
        assertEquals("Hello, World!", result);
    }

    @Test void renders_with_null_attribute() {
        var renderer = new TemplateRenderer();
        renderer.loadGroupFromClasspath(GROUP);

        var result = renderer.render(GROUP, "nullable", "value", null);
        assertEquals("Value: ", result);
    }

    @Test void throws_on_missing_template() {
        var renderer = new TemplateRenderer();
        renderer.loadGroupFromClasspath(GROUP);

        assertThrows(IllegalArgumentException.class, () ->
            renderer.render(GROUP, "nonexistent", "x", "y")
        );
    }

    @Test void loads_from_classpath() {
        var renderer = new TemplateRenderer();
        assertDoesNotThrow(() ->
            renderer.loadGroupFromClasspath(GROUP)
        );
    }

    @Test void rendered_output_uses_unix_line_endings() {
        var renderer = new TemplateRenderer();
        renderer.loadGroupFromClasspath(GROUP);

        var result = renderer.render(GROUP, "multiline",
                "name", "Test", "items", List.of("A", "B"));
        assertFalse(result.contains("\r\n"), "Output must use \\n, not \\r\\n");
        assertTrue(result.contains("\n"), "Output should contain newlines");
    }

    @Test void throws_on_missing_group() {
        var renderer = new TemplateRenderer();

        assertThrows(IllegalArgumentException.class, () ->
            renderer.loadGroupFromClasspath("templates/nonexistent.stg")
        );
    }

    @Test void throws_on_render_without_loading_group() {
        var renderer = new TemplateRenderer();

        assertThrows(IllegalArgumentException.class, () ->
            renderer.render("not-loaded", "hello", "name", "World")
        );
    }

    @Test void function_template_loads_successfully() {
        var renderer = new TemplateRenderer();
        assertDoesNotThrow(() ->
            renderer.loadGroupFromClasspath("templates/java-function.stg")
        );
        assertNotNull(renderer.getGroup("templates/java-function.stg"));
    }

    @Test void function_dispatch_template_loads_successfully() {
        var renderer = new TemplateRenderer();
        assertDoesNotThrow(() ->
            renderer.loadGroupFromClasspath("templates/java-function-dispatch.stg")
        );
        assertNotNull(renderer.getGroup("templates/java-function-dispatch.stg"));
    }

    @Test void multiline_template_preserves_structure() {
        var renderer = new TemplateRenderer();
        renderer.loadGroupFromClasspath(GROUP);

        var result = renderer.render(GROUP, "multiline",
                "name", "Test", "items", List.of("Alpha", "Beta"));
        assertTrue(result.contains("Name: Test"));
        assertTrue(result.contains("- Alpha"));
        assertTrue(result.contains("- Beta"));
    }
}
