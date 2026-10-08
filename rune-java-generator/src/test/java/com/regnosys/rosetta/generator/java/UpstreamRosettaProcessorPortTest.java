package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Leg-C port (PR #423, slice 2) of upstream
 * {@code rune-integration-tests/.../object/RosettaProcessorTest.xtend} — 4/4
 * methods. Upstream's test-local {@code RosettaAttributePathProcessor}
 * (implements the runtime {@code Processor}, collecting every visited
 * {@code RosettaPath}) becomes
 * {@link UpstreamPortHarness#pathCollectingProcessor} — a
 * {@code java.lang.reflect.Proxy} over the ISOLATED loader's {@code Processor}
 * interface (the ledger-designed seat: a test-classpath implementation could
 * never cross the loader boundary), recording {@code path.toString()}
 * (= {@code RosettaPath.buildPath()}, the exact form upstream joins). The
 * traversal itself — indexed list sub-paths, nested descent, supertype and
 * override flattening — is the RELEASED runtime's
 * {@code RosettaModelObject.process} contract over the fork's generated
 * {@code process(...)} bodies. Ledger:
 * the development audit "2026-07-17-leg-c-integration-port-ledger".
 */
class UpstreamRosettaProcessorPortTest {

    @BeforeAll
    static void requireReleasedRuntime() {
        UpstreamPortHarness.assumeReleasedRuntime();
    }

    private static void assertProcessEquals(List<String> expectedLines, Object rmo) {
        List<String> sink = new ArrayList<>();
        Object processor = UpstreamPortHarness.pathCollectingProcessor(
                rmo.getClass().getClassLoader(), sink);
        UpstreamPortHarness.process(rmo, "ROOT", processor);
        assertEquals(String.join(System.lineSeparator(), expectedLines) + System.lineSeparator(),
                String.join(System.lineSeparator(), sink) + System.lineSeparator());
    }

    /** Upstream {@code processFlatType}. */
    @Test
    void processFlatType() {
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(
                UpstreamPortHarness.generateCode("""
                type Foo:
                	attr1 int (0..1)
                	attr2 string (0..2)
                	attr3 int (1..1)
                """));

        Map<String, Object> items1 = new HashMap<>();
        items1.put("attr1", 42);
        items1.put("attr2", List.of("A", "B"));
        items1.put("attr3", 0);
        Object foo1 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo", items1);
        assertProcessEquals(List.of("ROOT.attr1", "ROOT.attr2", "ROOT.attr3"), foo1);

        Map<String, Object> items2 = new HashMap<>();
        items2.put("attr1", null);
        items2.put("attr2", List.of());
        items2.put("attr3", 0);
        Object foo2 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo", items2);
        assertProcessEquals(List.of("ROOT.attr1", "ROOT.attr2", "ROOT.attr3"), foo2);
    }

    /** Upstream {@code processNestedType}. */
    @Test
    void processNestedType() {
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(
                UpstreamPortHarness.generateCode("""
                type Foo:
                	attr1 int (0..1)
                	attr2 Bar (0..2)
                	attr3 Bar (1..1)

                type Bar:
                	bar Bar (0..1)
                """));

        Map<String, Object> barNull = new HashMap<>();
        barNull.put("bar", null);
        Object barLeaf1 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Bar", barNull);
        Object barNested = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Bar",
                Map.of("bar", UpstreamPortHarness.createInstanceUsingBuilder(classes, "Bar", barNull)));

        Map<String, Object> items = new HashMap<>();
        items.put("attr1", 42);
        items.put("attr2", List.of(barLeaf1, barNested));
        items.put("attr3", UpstreamPortHarness.createInstanceUsingBuilder(classes, "Bar", barNull));
        Object foo = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo", items);

        assertProcessEquals(List.of(
                "ROOT.attr1",
                "ROOT.attr2",
                "ROOT.attr2(0).bar",
                "ROOT.attr2(1).bar",
                "ROOT.attr2(1).bar.bar",
                "ROOT.attr3",
                "ROOT.attr3.bar"), foo);
    }

    /** Upstream {@code processTypeWithSupertype}. */
    @Test
    void processTypeWithSupertype() {
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(
                UpstreamPortHarness.generateCode("""
                type Foo:
                	attr1 int (0..1)

                type Bar extends Foo:
                	attr2 string (0..2)
                	attr3 int (1..1)
                """));

        Map<String, Object> items = new HashMap<>();
        items.put("attr1", 42);
        items.put("attr2", List.of("A", "B"));
        items.put("attr3", 0);
        Object bar = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Bar", items);
        assertProcessEquals(List.of("ROOT.attr1", "ROOT.attr2", "ROOT.attr3"), bar);
    }

    /** Upstream {@code processTypeWithOverridenAttributes}. */
    @Test
    void processTypeWithOverridenAttributes() {
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(
                UpstreamPortHarness.generateCode("""
                type A:
                	a int (1..1)

                type Foo:
                	attr1 int (0..1)
                	attr2 A (0..2)
                	attr3 string (1..1)

                type Bar extends Foo:
                	override attr1 int (0..1)
                	override attr2 A (0..2)
                	attr4 int (1..1)
                """));

        Object a = UpstreamPortHarness.createInstanceUsingBuilder(classes, "A", Map.of("a", 42));
        Map<String, Object> items = new HashMap<>();
        items.put("attr1", 42);
        items.put("attr2", List.of(a));
        items.put("attr3", "Bla");
        items.put("attr4", 0);
        Object bar = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Bar", items);

        assertProcessEquals(List.of(
                "ROOT.attr1",
                "ROOT.attr2",
                "ROOT.attr2(0).a",
                "ROOT.attr3",
                "ROOT.attr4"), bar);
    }
}
