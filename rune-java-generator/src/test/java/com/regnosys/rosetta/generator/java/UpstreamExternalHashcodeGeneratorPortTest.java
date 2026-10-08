package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Leg-C port (PR #414) of upstream
 * {@code rune-integration-tests/.../object/ExternalHashcodeGeneratorTest.xtend} —
 * 5/5 methods. Ledger: the development audit "2026-07-17-leg-c-integration-port-ledger".
 */
class UpstreamExternalHashcodeGeneratorPortTest {

    @BeforeAll
    static void requireReleasedRuntime() {
        UpstreamPortHarness.assumeReleasedRuntime();
    }

    /** Upstream {@code shouldGenerateExternalHashMethod}. */
    @Test
    void shouldGenerateExternalHashMethod() {
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(
                UpstreamPortHarness.generateCode("""
                        enum Enum: one

                        type RosettaType:

                        type PlainOldRosettaObject:
                        	basicTypE string (1..1)
                        	basicTypeList string (1..*)
                        	rosettaObject RosettaType (1..1)
                        	rosettaObjectList RosettaType (1..*)
                        	enumeration Enum (1..1)
                        	enumerationList Enum (1..*)
                        """));
        Class<?> poro = classes.get(UpstreamPortHarness.ROOT_PACKAGE + ".PlainOldRosettaObject");

        assertTrue(Arrays.stream(poro.getDeclaredMethods()).map(Method::getName)
                        .anyMatch("process"::equals),
                "expected a declared process(...) method on PlainOldRosettaObject");
    }

    /** Upstream {@code shouldHandleSuperClass}. */
    @Test
    void shouldHandleSuperClass() {
        UpstreamPortHarness.compileToClasses(UpstreamPortHarness.generateCode("""
                type Super:
                type Sub extends Super:
                	basicTypE string (1..1)
                """));
    }

    /** Upstream {@code shouldHandleEmptyClass}. */
    @Test
    void shouldHandleEmptyClass() {
        UpstreamPortHarness.compileToClasses(UpstreamPortHarness.generateCode("""
                type Empty:
                """));
    }

    /** Upstream {@code shouldHandleGlobalKeys}. */
    @Test
    void shouldHandleGlobalKeys() {
        UpstreamPortHarness.compileToClasses(UpstreamPortHarness.generateCode("""
                type WithGlobalKey:
                	[metadata key]
                	foo string (1..1)
                """));
    }

    /** Upstream {@code shouldNotGenerateForEnums}. */
    @Test
    void shouldNotGenerateForEnums() {
        UpstreamPortHarness.compileToClasses(UpstreamPortHarness.generateCode("""
                enum Enum: foo
                """));
    }
}
