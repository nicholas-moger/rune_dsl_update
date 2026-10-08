package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Leg-C port (PR #414) of upstream
 * {@code rune-integration-tests/.../object/ModelObjectBuilderGeneratorTest.xtend} —
 * 5/5 methods. Topic overlap note: the builder-ancestor-compat SOURCE bytes are
 * hold-out-locked (pojo-inheritance group, PR #412); these ports add the RUNTIME
 * side (builder chains, list adders, toBuilder round-trips). Ledger:
 * the development audit "2026-07-17-leg-c-integration-port-ledger".
 */
class UpstreamModelObjectBuilderGeneratorPortTest {

    @BeforeAll
    static void requireReleasedRuntime() {
        UpstreamPortHarness.assumeReleasedRuntime();
    }

    /** Upstream {@code shouldGenerateJavaBuilderThatExtendsParentBuilders} —
     * the exact toString stamp across a 4-deep builder chain. */
    @Test
    void shouldGenerateJavaBuilderThatExtendsParentBuilders() {
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
                        	dd2 string (0..1)

                        """));

        Object builder = UpstreamPortHarness.createBuilderInstance(classes, "D");
        UpstreamPortHarness.setAttribute(builder, "aa", "fieldA");
        UpstreamPortHarness.setAttribute(builder, "bb", "fieldB");
        UpstreamPortHarness.setAttribute(builder, "cc", "fieldC");
        UpstreamPortHarness.setAttribute(builder, "dd", "fieldD");

        Object d = UpstreamPortHarness.build(builder);

        assertEquals("D {dd=fieldD, dd2=null} C {cc=fieldC} B {bb=fieldB} A {aa=fieldA}",
                d.toString());
    }

    /** Upstream {@code useBuilderAddMultipleTimes}. */
    @Test
    void useBuilderAddMultipleTimes() {
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(
                UpstreamPortHarness.generateCode("""
                        type Tester:
                        	items string (0..*)

                        """));

        Object builder = UpstreamPortHarness.createBuilderInstance(classes, "Tester");
        // faithful to upstream: three explicit addItems(String) calls
        invokeAdd(builder, "item1");
        invokeAdd(builder, "item2");
        invokeAdd(builder, "item3");

        Object tester = UpstreamPortHarness.build(builder);
        Object items = UpstreamPortHarness.call(tester, "getItems");

        assertEquals(List.of("item1", "item2", "item3"), items);
    }

    private static void invokeAdd(Object builder, String value) {
        var m = UpstreamPortHarness.getMatchingMethod(builder.getClass(), "addItems",
                new Class<?>[] { String.class });
        assertTrue(m != null, "no addItems(String) on " + builder.getClass());
        try {
            m.invoke(builder, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /** Upstream {@code shouldGenerateObjectUsingBuilderOfBuildersPattern} —
     * the six source witnesses of the builder-of-builders shape. */
    @Test
    void shouldGenerateObjectUsingBuilderOfBuildersPattern() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                enum TestEnum:
                	testEnumValueOne

                type One:
                	oneField string (1..1)

                type Test:
                	multipleEnums TestEnum (1..*)
                	multipleOnes One (1..*)
                	singleOne One (1..1)

                """);

        String testClassCode = code.get(UpstreamPortHarness.ROOT_PACKAGE + ".Test");

        // Base case
        assertTrue(testClassCode.contains(
                "this.multipleOnes = ofNullable(builder.getMultipleOnes()).filter(_l->!_l.isEmpty())"
                + ".map(list -> list.stream().filter(Objects::nonNull).map(f->f.build())"
                + ".filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);"),
                "missing builder-of-builders impl-ctor mapping in:\n" + testClassCode);

        // Only for Rosetta-typed attributes
        assertFalse(testClassCode.contains("this.multipleEnums = builder.multipleEnums.stream()"),
                "enum list must not take the builder-of-builders mapping");

        // Builder holds builders of Rosetta types
        assertTrue(testClassCode.contains("List<? extends One> multipleOnes;"),
                "missing impl-side wildcard list field");
        assertTrue(testClassCode.contains("One.OneBuilder singleOne;"),
                "missing builder-typed single field");

        // Builder setters handle builder types
        assertTrue(testClassCode.contains("public Test.TestBuilder setSingleOne(One _singleOne) {"),
                "missing setSingleOne(One)");
        assertTrue(testClassCode.contains("public Test.TestBuilder addMultipleOnes(One _multipleOnes) {"),
                "missing addMultipleOnes(One)");

        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldGenerateObjectWhenSomeValuesAreNull}. */
    @Test
    void shouldGenerateObjectWhenSomeValuesAreNull() {
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(
                UpstreamPortHarness.generateCode("""
                        type One:
                        	oneField string (1..1)

                        type Test:
                        	rosettaObjectListField One (1..*)
                        	rosettaObjectField One (1..1)
                        	stringField string (1..1)

                        """));

        Object builder = UpstreamPortHarness.createBuilderInstance(classes, "Test");
        UpstreamPortHarness.setAttribute(builder, "stringField", "test-value");
        Object test = UpstreamPortHarness.build(builder);

        assertEquals("test-value", UpstreamPortHarness.call(test, "getStringField"),
                "Test object built with null values");

        Object roundTrip = UpstreamPortHarness.toBuilder(test);
        assertEquals("test-value", UpstreamPortHarness.call(roundTrip, "getStringField"));
    }

    /** Upstream {@code shouldGenerateObjectWithMethodToReturnItsStateAsBuilder} —
     * including upstream's builder-reuse detail (the same RosettaObject builder
     * builds the parent value, is re-set, then builds the child value). */
    @Test
    void shouldGenerateObjectWithMethodToReturnItsStateAsBuilder() {
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(
                UpstreamPortHarness.generateCode("""
                        type RosettaObject:
                        	rosettaField string (1..1)

                        type Parent:
                        	parentField RosettaObject (1..*)
                        	anotherParentField RosettaObject (1..1)

                        type Child extends Parent:
                        	childField RosettaObject (1..1)
                        	anotherChildField RosettaObject (1..*)

                        """));

        Object rosettaObjectBuilder = UpstreamPortHarness.createBuilderInstance(classes, "RosettaObject");

        UpstreamPortHarness.setAttribute(rosettaObjectBuilder, "rosettaField", "test-value-parent");
        Object rosettaObjectParent = UpstreamPortHarness.build(rosettaObjectBuilder);

        UpstreamPortHarness.setAttribute(rosettaObjectBuilder, "rosettaField", "test-value-child");
        Object rosettaObjectChild = UpstreamPortHarness.build(rosettaObjectBuilder);

        Object childBuilder = UpstreamPortHarness.createBuilderInstance(classes, "Child");
        UpstreamPortHarness.setAttribute(childBuilder, "childField", rosettaObjectChild);
        var adder = UpstreamPortHarness.getMatchingMethod(childBuilder.getClass(), "addParentField",
                new Class<?>[] { rosettaObjectParent.getClass() });
        assertTrue(adder != null, "no addParentField(RosettaObject) on " + childBuilder.getClass());
        try {
            adder.invoke(childBuilder, rosettaObjectParent);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        Object child = UpstreamPortHarness.build(childBuilder);

        assertEquals(rosettaObjectChild, UpstreamPortHarness.call(child, "getChildField"),
                "childField is stamped correctly");
        assertEquals(List.of(rosettaObjectParent), UpstreamPortHarness.call(child, "getParentField"),
                "parentField is stamped correctly");
    }
}
