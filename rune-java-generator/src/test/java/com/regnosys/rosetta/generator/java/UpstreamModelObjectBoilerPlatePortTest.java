package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Leg-C port (PR #414) of upstream
 * {@code rune-integration-tests/.../object/ModelObjectBoilerPlateTest.xtend} —
 * 3/3 methods. The runtime equals/toString contracts byte-locked nowhere else:
 * the hold-out bars prove SOURCE bytes, these prove the BEHAVIOUR of the same
 * boilerplate. Ledger: the development audit "2026-07-17-leg-c-integration-port-ledger".
 */
class UpstreamModelObjectBoilerPlatePortTest {

    @BeforeAll
    static void requireReleasedRuntime() {
        UpstreamPortHarness.assumeReleasedRuntime();
    }

    private static final String TEST_TYPE = """
            type Test:
            	testField string (1..1)
            """;

    /** Upstream {@code shouldGenerateObjectWithBoilerPlate}. */
    @Test
    void shouldGenerateObjectWithBoilerPlate() {
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(
                UpstreamPortHarness.generateCode(TEST_TYPE));

        Object thisTest = buildTest(classes);
        Object thatTest = buildTest(classes);

        assertEquals(thisTest, thatTest, "two identically-built instances must be equal");
        String s = thisTest.toString();
        assertTrue(s.contains("Test"), "toString must carry the type name: " + s);
        assertTrue(s.contains("testField=test-value"), "toString must carry the field: " + s);
    }

    /** Upstream {@code shouldGenerateObjectBuilderWithBoilerPlate}. */
    @Test
    void shouldGenerateObjectBuilderWithBoilerPlate() {
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(
                UpstreamPortHarness.generateCode(TEST_TYPE));

        Object thisBuilder = newSetBuilder(classes);
        Object thatBuilder = newSetBuilder(classes);

        assertEquals(thisBuilder, thatBuilder, "two identically-set builders must be equal");
        String s = thisBuilder.toString();
        assertTrue(s.contains("TestBuilder"), "builder toString must carry the builder name: " + s);
        assertTrue(s.contains("testField=test-value"), "builder toString must carry the field: " + s);
    }

    private static Object newSetBuilder(Map<String, Class<?>> classes) {
        Object builder = UpstreamPortHarness.createBuilderInstance(classes, "Test");
        UpstreamPortHarness.setAttribute(builder, "testField", "test-value");
        return builder;
    }

    private static Object buildTest(Map<String, Class<?>> classes) {
        return UpstreamPortHarness.build(newSetBuilder(classes));
    }

    /** Upstream {@code shouldGenerateHashCodeForEnumsUsingClassName}. */
    @Test
    void shouldGenerateHashCodeForEnumsUsingClassName() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                enum TestEnum :
                	TestEnumValue

                type ThatHasEnums:
                	testEnum TestEnum (1..1)
                	testEnums TestEnum (1..*)
                """);

        String thatHasEnumsClass = code.get(UpstreamPortHarness.ROOT_PACKAGE + ".ThatHasEnums");

        String singleHashCodeContribution =
                "_result = 31 * _result + (testEnum != null ? testEnum.getClass().getName().hashCode() : 0);";
        String multipleHashCodeContribution =
                "_result = 31 * _result + (testEnums != null ? testEnums.stream().map(Object::getClass).map(Class::getName).mapToInt(String::hashCode).sum() : 0);";

        assertTrue(thatHasEnumsClass.contains(singleHashCodeContribution),
                "missing single-enum hashCode contribution in:\n" + thatHasEnumsClass);
        assertTrue(thatHasEnumsClass.contains(multipleHashCodeContribution),
                "missing list-enum hashCode contribution in:\n" + thatHasEnumsClass);
    }
}
