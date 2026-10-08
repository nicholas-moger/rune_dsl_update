package com.regnosys.rosetta.generator.java.template;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TemplateRegistryTest {

    private static final String GROUP = "templates/test-simple.stg";

    @Test void register_and_retrieve_group_key() {
        var renderer = new TemplateRenderer();
        var registry = new TemplateRegistry(renderer);

        registry.register("test", GROUP);

        assertEquals(GROUP, registry.getGroupKey("test"));
    }

    @Test void getGroupKey_throws_on_unregistered() {
        var renderer = new TemplateRenderer();
        var registry = new TemplateRegistry(renderer);

        assertThrows(IllegalArgumentException.class, () ->
            registry.getGroupKey("nonexistent")
        );
    }

    @Test void isRegistered_returns_correct_boolean() {
        var renderer = new TemplateRenderer();
        var registry = new TemplateRegistry(renderer);

        assertFalse(registry.isRegistered("test"));
        registry.register("test", GROUP);
        assertTrue(registry.isRegistered("test"));
    }

    @Test void register_with_custom_delimiters() {
        var renderer = new TemplateRenderer();
        var registry = new TemplateRegistry(renderer);

        // java-package-info.stg uses $ delimiters
        registry.register("package-info",
                "templates/java-package-info.stg", '$', '$');

        assertEquals("templates/java-package-info.stg",
                registry.getGroupKey("package-info"));
    }

    @Test void renderer_can_render_after_registry_load() {
        var renderer = new TemplateRenderer();
        var registry = new TemplateRegistry(renderer);

        registry.register("test", GROUP);
        String result = renderer.render(GROUP, "hello", "name", "World");
        assertEquals("Hello, World!", result);
    }
}
