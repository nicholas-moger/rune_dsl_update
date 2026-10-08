package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Leg-C port (PR #414) of upstream
 * {@code rune-integration-tests/.../object/RosettaObjectInheritanceGeneratorTest.xtend}
 * — 4/4 methods (3 upstream-@Disabled: the override-across-namespaces family,
 * marked "override is deprecated" upstream; carried as @Disabled stubs — the
 * deprecated feature is not revived, the ledger records the disposition).
 * Ledger: the development audit "2026-07-17-leg-c-integration-port-ledger".
 */
class UpstreamRosettaObjectInheritancePortTest {

    @BeforeAll
    static void requireReleasedRuntime() {
        UpstreamPortHarness.assumeReleasedRuntime();
    }

    /** Upstream {@code shouldGenerateJavaClassWithMultipleParents}. */
    @Test
    void shouldGenerateJavaClassWithMultipleParents() {
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(
                UpstreamPortHarness.generateCode("""
                        type A:
                        	aa string (0..1)

                        type B extends A:
                        	bb string (0..1)

                        type C extends B:
                        	cc string (0..1)

                        type D extends C:
                        	dd string (0..1)

                        """));

        Class<?> a = classes.get(UpstreamPortHarness.ROOT_PACKAGE + ".A");
        Class<?> b = classes.get(UpstreamPortHarness.ROOT_PACKAGE + ".B");
        Class<?> c = classes.get(UpstreamPortHarness.ROOT_PACKAGE + ".C");
        Class<?> d = classes.get(UpstreamPortHarness.ROOT_PACKAGE + ".D");

        assertTrue(c.isAssignableFrom(d));
        assertTrue(b.isAssignableFrom(c));
        assertTrue(a.isAssignableFrom(b));
    }

    /** Upstream-@Disabled ("override is deprecated") — carried, not revived. */
    @Test
    @Disabled("upstream-@Disabled: override-across-namespaces is deprecated upstream")
    void shouldGenerateJavaClassWithOverridenAttributesAcrossNamespaces() {
    }

    /** Upstream-@Disabled ("override is deprecated") — carried, not revived. */
    @Test
    @Disabled("upstream-@Disabled: override-across-namespaces is deprecated upstream")
    void shouldGenerateJavaClassWithOverridenListAttributesAcrossNamespaces() {
    }

    /** Upstream-@Disabled ("override is deprecated") — carried, not revived. */
    @Test
    @Disabled("upstream-@Disabled: override-across-namespaces is deprecated upstream")
    void shouldGenerateJavaClassWithConditionsListAttributesAcrossNamespaces() {
    }
}
