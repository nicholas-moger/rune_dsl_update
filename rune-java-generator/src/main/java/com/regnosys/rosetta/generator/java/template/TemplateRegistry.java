package com.regnosys.rosetta.generator.java.template;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps generator names to their ST4 template group resource keys.
 * One instance per {@link com.regnosys.rosetta.generator.java.JavaCodeGenerator},
 * populated at construction time.
 *
 * <p>The registry is a parallel lookup for template resources alongside the existing
 * generator list — it does not replace the generator registration mechanism. Each
 * generator looks itself up via {@link #getGroupKey(String)}, which returns the
 * classpath resource key; loading that key into an {@code STGroup} is handled by
 * {@link TemplateRenderer}.
 *
 * <p>Future language constructs (FE-2) add entries here alongside their
 * generator class registration.
 */
public class TemplateRegistry {

    private final Map<String, String> generatorToTemplate = new ConcurrentHashMap<>();
    private final TemplateRenderer renderer;

    /**
     * Creates a registry backed by the given renderer.
     *
     * @param renderer the template renderer that will load and render templates
     */
    public TemplateRegistry(TemplateRenderer renderer) {
        this.renderer = java.util.Objects.requireNonNull(renderer, "renderer");
    }

    /**
     * Register a generator's template group. Loads the group immediately.
     *
     * @param generatorName logical name (e.g., "enum", "pojo", "cardinality-validator")
     * @param classpathResource .stg file path (e.g., "templates/java-enum.stg")
     */
    public void register(String generatorName, String classpathResource) {
        renderer.loadGroupFromClasspath(classpathResource);
        generatorToTemplate.put(generatorName, classpathResource);
    }

    /**
     * Register a generator's template group with custom delimiters.
     *
     * @param generatorName logical name
     * @param classpathResource .stg file path
     * @param delimStart start delimiter character
     * @param delimStop stop delimiter character
     */
    public void register(String generatorName, String classpathResource,
                          char delimStart, char delimStop) {
        renderer.loadGroupFromClasspath(classpathResource, delimStart, delimStop);
        generatorToTemplate.put(generatorName, classpathResource);
    }

    /**
     * Get the template group resource key for a generator.
     *
     * @param generatorName the logical generator name
     * @return the classpath resource key for the .stg group
     * @throws IllegalArgumentException if no template is registered for the generator
     */
    public String getGroupKey(String generatorName) {
        String key = generatorToTemplate.get(generatorName);
        if (key == null) {
            throw new IllegalArgumentException("No template registered for: " + generatorName);
        }
        return key;
    }

    /** Get the underlying renderer. */
    public TemplateRenderer getRenderer() {
        return renderer;
    }

    /** Check if a generator has a registered template. */
    public boolean isRegistered(String generatorName) {
        return generatorToTemplate.containsKey(generatorName);
    }
}
