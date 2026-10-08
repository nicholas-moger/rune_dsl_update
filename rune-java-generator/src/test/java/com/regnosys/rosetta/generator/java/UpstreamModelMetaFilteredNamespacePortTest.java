package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Leg-C port (PR #423, slice 2) of upstream
 * {@code rune-integration-tests/.../object/ModelMetaGeneratorFilteredNamespaceTest.xtend}
 * — 1/1 method. Upstream's channel is a {@code rosetta-config.yml}
 * {@code generators.namespaces} filter bound through
 * {@code RosettaConfigurationFileProvider}; the fork seat for the SAME semantic
 * is {@code GeneratorModel}'s emission-filter predicate (the caller-provided
 * pattern, PR #331) — the upstream ymls
 * ({@code rosetta-filtered-config-model1.yml}: {@code [model1.*, com.rosetta.model]};
 * {@code model2} twin) map to namespace predicates here. The observable is
 * upstream's: each filtered run emits ONLY its own namespace's classes (plus
 * demanded metafields), and the two runs' outputs COMBINED compile — i.e. the
 * model2 run resolves {@code model1.Foo} without re-emitting model1's classes.
 * Ledger: the development audit "2026-07-17-leg-c-integration-port-ledger".
 */
class UpstreamModelMetaFilteredNamespacePortTest {

    @BeforeAll
    static void requireReleasedRuntime() {
        UpstreamPortHarness.assumeReleasedRuntime();
    }

    private static final String MODEL_1 = """
            namespace model1

            	type Foo:
            """;

    private static final String MODEL_2 = """
            namespace model2

            import model1.Foo

            	type Bar:
            		foo Foo (1..1)
            		[metadata reference]
            """;

    /** Upstream's {@code generators.namespaces} glob list as a namespace predicate. */
    private static Predicate<String> namespaces(String own) {
        return ns -> ns.equals(own) || ns.startsWith(own + ".") || ns.equals("com.rosetta.model");
    }

    /** Upstream {@code shouldGenerateBasicTypeReferencesFoo}. */
    @Test
    void shouldGenerateBasicTypeReferencesFoo() {
        Map<String, String> model1Code = UpstreamPortHarness.byClassName(
                UpstreamPortHarness.generateWithNamespaceFilter(namespaces("model1"), MODEL_1));

        Map<String, String> model2Code = UpstreamPortHarness.byClassName(
                UpstreamPortHarness.generateWithNamespaceFilter(namespaces("model2"), MODEL_1, MODEL_2));

        // The filter law: each run carries its OWN namespace's model classes only.
        assertTrue(model1Code.containsKey("model1.Foo"),
                "model1 run must emit model1.Foo (got: " + model1Code.keySet() + ")");
        assertFalse(model1Code.containsKey("model2.Bar"),
                "model1 run must not emit model2 classes");
        assertTrue(model2Code.containsKey("model2.Bar"),
                "model2 run must emit model2.Bar (got: " + model2Code.keySet() + ")");
        assertFalse(model2Code.containsKey("model1.Foo"),
                "model2 run must NOT re-emit the filtered-out model1.Foo");

        // Upstream's final observable: the merged outputs compile together.
        Map<String, String> merged = new LinkedHashMap<>(model1Code);
        merged.putAll(model2Code);
        UpstreamPortHarness.compileToClasses(merged);
    }
}
