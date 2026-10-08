package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Leg-C port (PR #414) of upstream
 * {@code rune-integration-tests/.../object/ModelObjectGeneratorTest.xtend} —
 * 26/26 methods (2 upstream-@Disabled carried as stubs: {@code testGenerateClassList}
 * [the Rosetta.classes() registry never shipped] and
 * {@code shouldNotCopyCertainFieldsIntoBuilder} [pre-annotation {@code type Foo
 * globalKey} syntax — no longer parseable]). Basic-type getter mappings, the
 * meta/scheme/reference runtime, prune and inheritance round-trips — the runtime
 * face of shapes whose SOURCE bytes the corpus + hold-out bars lock. Ledger:
 * the development audit "2026-07-17-leg-c-integration-port-ledger".
 */
class UpstreamModelObjectGeneratorPortTest {

    private static final String ROOT = UpstreamPortHarness.ROOT_PACKAGE;

    @BeforeAll
    static void requireReleasedRuntime() {
        UpstreamPortHarness.assumeReleasedRuntime();
    }

    private static Map<String, Class<?>> compile(String snippet) {
        return UpstreamPortHarness.compileToClasses(UpstreamPortHarness.generateCode(snippet));
    }

    private static Class<?> returnTypeOf(Map<String, Class<?>> classes, String cls, String method) {
        try {
            return classes.get(cls).getMethod(method).getReturnType();
        } catch (NoSuchMethodException e) {
            throw new AssertionError("missing " + method + "() on " + cls, e);
        }
    }

    /** Upstream {@code testAttributeWithSameNameAsJavaKeyword}. */
    @Test
    void testAttributeWithSameNameAsJavaKeyword() {
        compile("""
                type A:
                	new string (0..1)

                	condition Foo:
                		new exists
                """);
    }

    /**
     * Upstream {@code testObjectReservedNames} — <b>HEALED at PR #415</b> (was the
     * PR #414 pinned facet lead: the fork emitted
     * {@code getValidationResults(RosettaPath path, Path path)} — a javac
     * duplicate-parameter error). The heal is BYTE-PINNED against the released-9.83.0
     * oracle (holdout group {@code reserved-names}, 18/18 byte-identical): for a
     * subject type named {@code Path}, upstream escapes the fixed {@code RosettaPath}
     * parameter NUMERICALLY ({@code path0} in Default; {@code path0}/{@code path1} in
     * NoOp) and the instance keeps {@code path} — NOT the underscore convention the
     * vendored-source reading predicted (the oracle outranks source inference). The
     * sibling collisions ({@code Result} → {@code _result} local WITH upstream's
     * literal-{@code result} guard bug reproduced bug-compat;
     * {@code FailureMessage} → {@code _failureMessage} consistently) are byte-locked
     * by the same holdout group; the {@code Result} shape is upstream-NON-COMPILING
     * and carries the CompileGate's documented pin.
     */
    @Test
    void testObjectReservedNames() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Path:
                	name string (0..1)

                	condition NameExists:
                		name exists
                """);
        String dataRule = code.get(ROOT + ".validation.datarule.PathNameExists");
        assertNotNull(dataRule, "PathNameExists datarule missing from " + code.keySet());
        assertTrue(dataRule.contains(
                "public List<ValidationResult<?>> getValidationResults(RosettaPath path0, Path path) {"),
                "the oracle-pinned path0 escape missing in:\n" + dataRule);

        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code useBuilderAddMultipleTimes}. */
    @Test
    void useBuilderAddMultipleTimes() {
        Map<String, Class<?>> classes = compile("""
                type Tester:
                	items string (0..*)
                """);

        Object builder = UpstreamPortHarness.createBuilderInstance(classes, "Tester");
        for (String item : List.of("item1", "item2", "item3")) {
            var m = UpstreamPortHarness.getMatchingMethod(builder.getClass(), "addItems",
                    new Class<?>[] { String.class });
            assertNotNull(m, "no addItems(String) on " + builder.getClass());
            try {
                m.invoke(builder, item);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        }
        Object tester = UpstreamPortHarness.build(builder);

        assertEquals(List.of("item1", "item2", "item3"),
                UpstreamPortHarness.call(tester, "getItems"));
    }

    /** Upstream {@code generateStringBasicType}. */
    @Test
    void generateStringBasicType() {
        Map<String, Class<?>> classes = compile("""
                type Tester:
                	one string (0..1)
                	list string (0..*)
                """);
        assertEquals(String.class, returnTypeOf(classes, ROOT + ".Tester", "getOne"));
    }

    /** Upstream {@code generateIntBasicType}. */
    @Test
    void generateIntBasicType() {
        Map<String, Class<?>> classes = compile("""
                type Tester:
                	one int (0..1)
                	list int (0..*)
                """);
        assertEquals(Integer.class, returnTypeOf(classes, ROOT + ".Tester", "getOne"));
    }

    /** Upstream {@code generateNumberBasicType}. */
    @Test
    void generateNumberBasicType() {
        Map<String, Class<?>> classes = compile("""
                type Tester:
                	one number (0..1)
                	list number (0..*)
                """);
        assertEquals(BigDecimal.class, returnTypeOf(classes, ROOT + ".Tester", "getOne"));
    }

    /** Upstream {@code generateBooleanBasicType}. */
    @Test
    void generateBooleanBasicType() {
        Map<String, Class<?>> classes = compile("""
                type Tester:
                	one boolean (0..1)
                	list boolean (0..*)
                """);
        assertEquals(Boolean.class, returnTypeOf(classes, ROOT + ".Tester", "getOne"));
    }

    /** Upstream {@code generateDateBasicType} — the runtime {@code Date} record
     * class, compared BY NAME (the loader-isolation law: the released-runtime
     * class is not this JVM's vendored one). */
    @Test
    void generateDateBasicType() {
        Map<String, Class<?>> classes = compile("""
                type Tester:
                	one date (0..1)
                	list date (0..*)
                """);
        assertEquals("com.rosetta.model.lib.records.Date",
                returnTypeOf(classes, ROOT + ".Tester", "getOne").getName());
    }

    /** Upstream {@code generateDateTimeBasicType}. */
    @Test
    void generateDateTimeBasicType() {
        Map<String, Class<?>> classes = compile("""
                type Tester:
                	one date (0..1)
                	list date (0..*)
                	zoned zonedDateTime (0..1)
                """);
        assertEquals("com.rosetta.model.lib.records.Date",
                returnTypeOf(classes, ROOT + ".Tester", "getOne").getName());
        assertEquals(ZonedDateTime.class, returnTypeOf(classes, ROOT + ".Tester", "getZoned"));
    }

    /** Upstream {@code generateTimeBasicType}. */
    @Test
    void generateTimeBasicType() {
        Map<String, Class<?>> classes = compile("""
                type Tester:
                	one time (0..1)
                	list time (0..*)
                """);
        assertEquals(LocalTime.class, returnTypeOf(classes, ROOT + ".Tester", "getOne"));
    }

    /** Upstream {@code shouldGenerateFunctioningJavaObjects} — prune/build on an
     * empty builder, then set + rebuild. */
    @Test
    void shouldGenerateFunctioningJavaObjects() {
        Map<String, Class<?>> classes = compile("""
                type TestObject: <"">
                	fieldOne string (0..1) <"">
                """);
        Object builder = UpstreamPortHarness.createBuilderInstance(classes, "TestObject");
        UpstreamPortHarness.call(builder, "prune");
        Object inst = UpstreamPortHarness.build(builder);
        assertNull(UpstreamPortHarness.call(inst, "getFieldOne"));

        UpstreamPortHarness.setAttribute(builder, "fieldOne", "value");
        inst = UpstreamPortHarness.build(builder);
        assertEquals("value", UpstreamPortHarness.call(inst, "getFieldOne"));
    }

    /** Upstream {@code shouldGenerateMetadFieldWhenAttributeSchemePresent} —
     * getOrCreate + setValue on a [metadata scheme] field, round-tripped. */
    @Test
    void shouldGenerateMetadFieldWhenAttributeSchemePresent() {
        Map<String, Class<?>> classes = compile("""
                type TestObject: <"">
                	fieldOne string (0..1) [metadata scheme]
                """);
        Class<?> generatedClass = classes.get(ROOT + ".TestObject");
        assertNotNull(getMethodOrNull(generatedClass, "getFieldOne"));

        Object builder = UpstreamPortHarness.createBuilderInstance(classes, "TestObject");
        Object metad = UpstreamPortHarness.call(builder, "getOrCreateFieldOne");
        UpstreamPortHarness.setAttribute(metad, "value", "fieldOne");

        Object inst = UpstreamPortHarness.build(builder);
        Object metadValue = UpstreamPortHarness.call(inst, "getFieldOne");
        assertEquals("fieldOne", UpstreamPortHarness.call(metadValue, "getValue"));
    }

    /** Upstream {@code shouldGenerateRosettaReferenceField}. */
    @Test
    void shouldGenerateRosettaReferenceField() {
        Map<String, Class<?>> classes = compile("""
                type TestObject: <"">
                	fieldOne Test2 (0..1)
                		[metadata reference]

                type Test2:
                	[metadata key]
                """);
        Class<?> generatedClass = classes.get(ROOT + ".TestObject");
        assertNotNull(getMethodOrNull(generatedClass, "getFieldOne"));

        Object builder = UpstreamPortHarness.createBuilderInstance(classes, "TestObject");
        Object metad = UpstreamPortHarness.call(builder, "getOrCreateFieldOne");
        UpstreamPortHarness.setAttribute(metad, "externalReference", "fieldOne");

        Object inst = UpstreamPortHarness.build(builder);
        Object metadValue = UpstreamPortHarness.call(inst, "getFieldOne");
        assertEquals("fieldOne", UpstreamPortHarness.call(metadValue, "getExternalReference"));
    }

    /** Upstream {@code shouldGenerateBasicReferenceField} — a reference on a BASIC
     * type in a custom (unversioned) namespace. */
    @Test
    void shouldGenerateBasicReferenceField() {
        String ns = "test.ns.basicref";
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(
                UpstreamPortHarness.generateCode("""
                        namespace "test.ns.basicref"

                        // import basic types
                        import com.rosetta.test.model.*

                        type TestObject: <"">
                        	fieldOne date (0..1) [metadata reference]
                        """));
        Class<?> generatedClass = classes.get(ns + ".TestObject");
        assertNotNull(generatedClass, "TestObject missing in " + classes.keySet());
        assertNotNull(getMethodOrNull(generatedClass, "getFieldOne"));

        Object builder = UpstreamPortHarness.createBuilderInstance(classes, ns, "TestObject");
        Object metad = UpstreamPortHarness.call(builder, "getOrCreateFieldOne");
        UpstreamPortHarness.setAttribute(metad, "externalReference", "fieldOne");

        Object inst = UpstreamPortHarness.build(builder);
        Object metadValue = UpstreamPortHarness.call(inst, "getFieldOne");
        assertEquals("fieldOne", UpstreamPortHarness.call(metadValue, "getExternalReference"));
    }

    /** Upstream {@code shouldCreateFieldWithReferenceTypeWhenAttributeIsReference}. */
    @Test
    void shouldCreateFieldWithReferenceTypeWhenAttributeIsReference() {
        Map<String, Class<?>> classes = compile("""

                type ComplexObject:
                	[metadata key]

                type TestObject: <"">
                	fieldOne ComplexObject (0..1)
                		[metadata reference]
                """);
        assertEquals("com.rosetta.test.model.metafields.ReferenceWithMetaComplexObject",
                returnTypeOf(classes, ROOT + ".TestObject", "getFieldOne").getName());
    }

    /** Upstream {@code shouldGenerateTypeWithMetaFieldImport}. */
    @Test
    void shouldGenerateTypeWithMetaFieldImport() {
        String ns = "test.ns.metafield";
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(
                UpstreamPortHarness.generateCode("""
                        namespace "test.ns.metafield"
                        version "test"

                        // import basic types
                        import com.rosetta.test.model.*

                        type Foo:
                            [metadata key]

                            attr string (0..1)
                        """));
        Class<?> generatedClass = classes.get(ns + ".Foo");
        assertNotNull(generatedClass, "Foo missing in " + classes.keySet());
        assertNotNull(getMethodOrNull(generatedClass, "getAttr"));
    }

    /** Upstream {@code shouldImplementGlobalKeyWhenDefined}. */
    @Test
    void shouldImplementGlobalKeyWhenDefined() {
        Map<String, Class<?>> classes = compile("""
                type WithGlobalKey:
                	[metadata key]
                	bar string (1..1)
                """);
        Class<?> withGlobalKeys = classes.get(ROOT + ".WithGlobalKey");

        assertTrue(Arrays.stream(withGlobalKeys.getInterfaces())
                        .anyMatch(i -> i.getName().equals("com.rosetta.model.lib.GlobalKey")),
                "expected WithGlobalKey to implement com.rosetta.model.lib.GlobalKey");
    }

    /** Upstream {@code shouldOmmitGlobalKeyAnnotationWhenNotDefined} (sic). */
    @Test
    void shouldOmmitGlobalKeyAnnotationWhenNotDefined() {
        Map<String, Class<?>> classes = compile("""
                type AttributeGlobalKeyTest:
                	withoutGlobalKey string (1..1)
                """);
        Class<?> testClass = classes.get(ROOT + ".AttributeGlobalKeyTest");
        Annotation[] annotations;
        try {
            annotations = testClass.getMethod("getWithoutGlobalKey").getAnnotations();
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }

        assertFalse(Arrays.stream(annotations)
                        .anyMatch(a -> a.annotationType().getName().contains("GlobalKey")),
                "no GlobalKey annotation expected on a plain getter");
    }

    /** Upstream {@code shouldGenerateReferenceAttributeAsReference}. */
    @Test
    void shouldGenerateReferenceAttributeAsReference() {
        Map<String, Class<?>> classes = compile("""
                type Foo:
                	[metadata key]
                	bar string (1..1)

                type AttributeGlobalKeyTest:
                	withGlobalKey Foo (1..1) [metadata reference]
                """);
        assertEquals("ReferenceWithMetaFoo",
                returnTypeOf(classes, ROOT + ".AttributeGlobalKeyTest", "getWithGlobalKey")
                        .getSimpleName());
    }

    /** Upstream-@Disabled — the {@code Rosetta.classes()} registry never shipped
     * upstream; carried as a stub, not revived. */
    @Test
    @Disabled("upstream-@Disabled: the Rosetta.classes() registry does not exist")
    void testGenerateClassList() {
    }

    /** Upstream {@code shouldExtendATypeWithSameAttribute}. */
    @Test
    void shouldExtendATypeWithSameAttribute() {
        compile("""
                type Foo:
                	a string (0..1)
                	b string (0..1)

                type Bar extends Foo:
                	override a string (0..1)
                """);
    }

    /** Upstream {@code shouldSetAttributesOnEmptyClassWithInheritance} — set a
     * parent attribute through an EMPTY subclass builder + toBuilder round-trip. */
    @Test
    void shouldSetAttributesOnEmptyClassWithInheritance() {
        Map<String, Class<?>> classes = compile("""
                type Foo:
                	attr string (0..1)

                type Bar extends Foo:
                """);

        Object builder = UpstreamPortHarness.createBuilderInstance(classes, "Bar");
        UpstreamPortHarness.setAttribute(builder, "attr", "blah");
        Object bar = UpstreamPortHarness.build(builder);

        assertEquals("blah", UpstreamPortHarness.call(bar, "getAttr"));

        Object roundTrip = UpstreamPortHarness.build(UpstreamPortHarness.toBuilder(bar));
        assertEquals("blah", UpstreamPortHarness.call(roundTrip, "getAttr"));
    }

    /** Upstream {@code isProductWithEnumValueRef}. */
    @Test
    void isProductWithEnumValueRef() {
        compile("""
                isProduct root Foo;

                enum Enum:
                	A
                	B

                type Foo:
                	attr Enum (0..1)

                func Qualify_FooProd:
                	[qualification Product]
                	inputs: foo Foo (1..1)
                	output: is_product boolean (1..1)
                	set is_product:
                		foo -> attr = Enum -> A
                """);
    }

    /** Upstream {@code internalReferenceTest} — [metadata location] +
     * [metadata address "pointsTo"=...]: the address getter's ReferenceWithMetaString
     * shape + the location key minted on getOrCreateMeta. */
    @Test
    void internalReferenceTest() {
        Map<String, Class<?>> classes = compile("""

                type Foo:
                	foo string (1..1)
                		[metadata location]

                type Bar:
                	bar string (1..1)
                		[metadata address "pointsTo"=Foo->foo]

                """);

        assertEquals("com.rosetta.model.metafields.ReferenceWithMetaString",
                returnTypeOf(classes, ROOT + ".Bar", "getBar").getName());

        Object builder = UpstreamPortHarness.createBuilderInstance(classes, "Foo");
        Object metad = UpstreamPortHarness.call(builder, "getOrCreateFoo");
        Object metas = UpstreamPortHarness.call(metad, "getOrCreateMeta");
        Object keys = UpstreamPortHarness.call(metas, "getKey");
        assertEquals(1, ((List<?>) keys).size());
    }

    /** Upstream-@Disabled — pre-annotation {@code type Foo globalKey} syntax (no
     * longer parseable); carried as a stub, not revived. */
    @Test
    @Disabled("upstream-@Disabled: pre-annotation `type Foo globalKey` syntax")
    void shouldNotCopyCertainFieldsIntoBuilder() {
    }

    /** Upstream {@code shouldPruneListUnchanged}. */
    @Test
    void shouldPruneListUnchanged() {
        Map<String, Class<?>> classes = compile("""
                type Foo:
                	bar string (0..*)
                """);

        Object foo = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo",
                Map.of(), Map.of("bar", List.of("a", "b")));
        Object pruned = UpstreamPortHarness.call(UpstreamPortHarness.toBuilder(foo), "prune");
        Object bar = UpstreamPortHarness.call(pruned, "getBar");

        assertNotNull(bar);
        assertEquals(2, ((List<?>) bar).size());
    }

    private static java.lang.reflect.Method getMethodOrNull(Class<?> cls, String name) {
        try {
            return cls.getMethod(name);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
