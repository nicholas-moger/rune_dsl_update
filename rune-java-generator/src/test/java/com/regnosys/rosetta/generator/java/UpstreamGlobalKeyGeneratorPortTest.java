package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Leg-C port (PR #414) of upstream
 * {@code rune-integration-tests/.../object/GlobalKeyGeneratorTest.xtend} — 4/4
 * methods (1 upstream-@Disabled, carried as-is). Ledger:
 * the development audit "2026-07-17-leg-c-integration-port-ledger".
 */
class UpstreamGlobalKeyGeneratorPortTest {

    @BeforeAll
    static void requireReleasedRuntime() {
        UpstreamPortHarness.assumeReleasedRuntime();
    }

    private static boolean hasDeclaredMethod(Class<?> cls, String name) {
        return Arrays.stream(cls.getDeclaredMethods()).map(Method::getName).anyMatch(name::equals);
    }

    private static boolean hasMethod(Class<?> cls, String name) {
        return Arrays.stream(cls.getMethods()).map(Method::getName).anyMatch(name::equals);
    }

    /** Upstream {@code shouldGenerateGlobalKeyFieldAndGetterWhenSet}. */
    @Test
    void shouldGenerateGlobalKeyFieldAndGetterWhenSet() {
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(
                UpstreamPortHarness.generateCode("""
                        type WithGlobalKey:
                        	[metadata key]
                        	foo string (1..1)
                        """));
        Class<?> withGlobalKey = classes.get(UpstreamPortHarness.ROOT_PACKAGE + ".WithGlobalKey");

        assertTrue(hasDeclaredMethod(withGlobalKey, "getMeta"),
                "expected getMeta() on a [metadata key] type");
    }

    /** Upstream {@code shouldNotGenerateFieldsAndGetterWhenNotDefined}. */
    @Test
    void shouldNotGenerateFieldsAndGetterWhenNotDefined() {
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(
                UpstreamPortHarness.generateCode("""
                        type WithoutGlobalKeys:
                        	foo string (1..1)
                        """));
        Class<?> withoutGlobalKeys = classes.get(UpstreamPortHarness.ROOT_PACKAGE + ".WithoutGlobalKeys");

        assertFalse(hasMethod(withoutGlobalKeys, "getMeta"),
                "getMeta() must be absent without [metadata key]");
    }

    /** Upstream {@code shouldGenerateGlobalReferenceField} (its own inline
     * {@code metaType reference string} — upstream's TODO notes the builtins don't
     * declare it; the harness companion carries only {@code scheme}, so the inline
     * declaration is preserved verbatim). */
    @Test
    void shouldGenerateGlobalReferenceField() {
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(
                UpstreamPortHarness.generateCode("""
                        metaType reference string

                        type Foo:
                        	[metadata key]
                        	bar string (1..1)

                        type Baz:
                        	foo Foo (1..1)
                        		[metadata reference]

                        	condition:
                        		foo -> reference = "reference"
                        """));

        Class<?> foo = classes.get(UpstreamPortHarness.ROOT_PACKAGE + ".Foo");
        assertTrue(hasDeclaredMethod(foo, "getBar"), "expected getBar() on Foo");
        assertTrue(hasDeclaredMethod(foo, "getMeta"), "expected getMeta() on Foo");

        Class<?> baz = classes.get(UpstreamPortHarness.ROOT_PACKAGE + ".Baz");
        assertTrue(hasDeclaredMethod(baz, "getFoo"), "expected getFoo() on Baz");
        assertEquals("ReferenceWithMetaFoo", getReturnTypeSimpleName(baz, "getFoo"));
    }

    private static String getReturnTypeSimpleName(Class<?> cls, String method) {
        try {
            return cls.getMethod(method).getReturnType().getSimpleName();
        } catch (NoSuchMethodException e) {
            throw new AssertionError("missing " + method + "() on " + cls, e);
        }
    }

    /** Upstream {@code shouldGenerateGlobalReferenceField2} — upstream-@Disabled
     * ("the path containing a reference should work — the path starts at the
     * attribute rather than the type level"); carried as-is, never revived
     * unilaterally (the ledger records the upstream reason). */
    @Test
    @Disabled("upstream-@Disabled: attribute-level reference path unsupported upstream")
    void shouldGenerateGlobalReferenceField2() {
        // Upstream body preserved for fidelity; unreached under @Disabled.
        UpstreamPortHarness.compileToClasses(UpstreamPortHarness.generateCode("""
                metaType reference string

                type Foo:
                	[metadata key]
                	bar string (1..1)

                type Baz:
                	foo Foo (1..1)
                		[metadata reference]

                	condition:
                		foo -> reference exists
                """));
    }
}
