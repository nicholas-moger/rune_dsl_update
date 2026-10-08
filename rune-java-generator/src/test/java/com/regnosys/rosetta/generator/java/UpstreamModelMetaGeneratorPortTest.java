package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Leg-C port (PR #423, slice 2) of upstream
 * {@code rune-integration-tests/.../object/ModelMetaGeneratorTest.xtend} —
 * 5/5 methods. Three seams:
 * <ul>
 *   <li>the qualify-function REGISTRY count (FooMeta carries the two
 *       {@code [qualification BusinessEvent]} funcs, the extending BarMeta
 *       carries none — upstream's meta-registry law);</li>
 *   <li>the two validator FULL-TEXT locks (cardinality + type-format) — the
 *       upstream expected blocks pasted byte-verbatim (trailing spaces kept via
 *       {@code \s}); the fork emission is corpus-locked to the same 9.83.0
 *       shapes, so these double as snippet-level byte oracles;</li>
 *   <li>the runtime VALIDATION behaviour ({@code meta.validator(factory)} /
 *       {@code meta.typeFormatValidator(factory)} through REAL Guice in the
 *       isolated loader) — failure messages come from the RELEASED runtime's
 *       {@code checkCardinality}/{@code checkNumber}/{@code checkString}.</li>
 * </ul>
 * Ledger: the development audit "2026-07-17-leg-c-integration-port-ledger".
 */
class UpstreamModelMetaGeneratorPortTest {

    @BeforeAll
    static void requireReleasedRuntime() {
        UpstreamPortHarness.assumeReleasedRuntime();
    }

    // ------------------------------------------------------------- helpers

    /** {@code <Type>Meta.getQualifyFunctions(QualifyFunctionFactory.Default).size()}. */
    private static int qualifyFunctionsSize(Map<String, Class<?>> classes, String typeName) {
        Object meta = UpstreamPortHarness.metaInstance(classes, typeName);
        ClassLoader loader = meta.getClass().getClassLoader();
        Object injector = UpstreamPortHarness.guiceInjector(loader);
        Object factory = UpstreamPortHarness.getInstance(injector, UpstreamPortHarness
                .loadRuntimeClass(classes, "com.rosetta.model.lib.qualify.QualifyFunctionFactory$Default"));
        try {
            Class<?> factoryIface = loader.loadClass("com.rosetta.model.lib.qualify.QualifyFunctionFactory");
            Object funcs = meta.getClass().getMethod("getQualifyFunctions", factoryIface)
                    .invoke(meta, factory);
            return ((List<?>) funcs).size();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("getQualifyFunctions failed on " + meta.getClass(), e);
        }
    }

    private static List<Object> failedResults(List<Object> results) {
        List<Object> failed = new ArrayList<>();
        for (Object r : results) {
            if (!((Boolean) UpstreamPortHarness.call(r, "isSuccess"))) {
                failed.add(r);
            }
        }
        return failed;
    }

    @SuppressWarnings("unchecked")
    private static List<String> failureReasons(List<Object> failed) {
        List<String> reasons = new ArrayList<>();
        for (Object r : failed) {
            reasons.add(((Optional<String>) UpstreamPortHarness.call(r, "getFailureReason")).get());
        }
        return reasons;
    }

    private static void assertAllValidationType(List<Object> results, String expected) {
        for (Object r : results) {
            // No `r` in the assertion message: ModelValidationResult.toString()
            // dereferences the (deliberately null) RosettaPath.
            assertEquals(expected,
                    String.valueOf(UpstreamPortHarness.call(r, "getValidationType")),
                    "unexpected validation type");
        }
    }

    private static boolean success(Object validationResult) {
        return (Boolean) UpstreamPortHarness.call(validationResult, "isSuccess");
    }

    // --------------------------------------------------------------- tests

    /** Upstream {@code shouldGenerateGetQualifyFunctions}. */
    @Test
    void shouldGenerateGetQualifyFunctions() {
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(
                UpstreamPortHarness.generateCode("""
                isEvent root Foo;

                type Foo:
                	a string (0..1)

                type Bar extends Foo:
                	b string (0..1)

                func Qualify_AExists:
                	[qualification BusinessEvent]
                	inputs: foo Foo (1..1)
                	output: is_event boolean (1..1)
                	set is_event:
                		foo -> a exists

                func Qualify_AEqualsSomeValue:
                	[qualification BusinessEvent]
                	inputs: foo Foo (1..1)
                	output: is_event boolean (1..1)
                	set is_event:
                		foo -> a = "someValue"
                """));

        assertEquals(2, qualifyFunctionsSize(classes, "Foo"));
        assertEquals(0, qualifyFunctionsSize(classes, "Bar"));
    }

    /** Upstream {@code shouldGenerateBasicTypeReferences}. */
    @Test
    void shouldGenerateBasicTypeReferences() {
        UpstreamPortHarness.compileToClasses(UpstreamPortHarness.generateCode("""
                type Flat:
                	oneField string (1..1)
                		[metadata scheme]
                	two int (1..*)\s
                		[metadata reference]
                	three date (1..1)
                		[metadata reference]
                """));
    }

    /** Upstream {@code shouldGenerateValidators} — full-text + runtime halves. */
    @Test
    void shouldGenerateValidators() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                typeAlias Max5Text: string(maxLength: 5)

                type Foo:
                	a string (1..2)
                	b number (1..1)
                	c int (1..*)
                	d number(min: -1) (0..1)
                	f Max5Text (0..*)
                """);

        assertEquals("""
                package com.rosetta.test.model.validation;

                import com.google.common.collect.Lists;
                import com.rosetta.model.lib.expression.ComparisonResult;
                import com.rosetta.model.lib.path.RosettaPath;
                import com.rosetta.model.lib.validation.ValidationResult;
                import com.rosetta.model.lib.validation.Validator;
                import com.rosetta.test.model.Foo;
                import java.math.BigDecimal;
                import java.util.List;

                import static com.google.common.base.Strings.isNullOrEmpty;
                import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
                import static com.rosetta.model.lib.validation.ValidationResult.failure;
                import static com.rosetta.model.lib.validation.ValidationResult.success;
                import static java.util.stream.Collectors.toList;

                public class FooValidator implements Validator<Foo> {

                	private List<ComparisonResult> getComparisonResults(Foo o) {
                		return Lists.<ComparisonResult>newArrayList(
                				checkCardinality("a", (List<String>) o.getA() == null ? 0 : o.getA().size(), 1, 2),\s
                				checkCardinality("b", (BigDecimal) o.getB() != null ? 1 : 0, 1, 1),\s
                				checkCardinality("c", (List<Integer>) o.getC() == null ? 0 : o.getC().size(), 1, 0),\s
                				checkCardinality("d", (BigDecimal) o.getD() != null ? 1 : 0, 0, 1)
                			);
                	}

                	@Override
                	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Foo o) {
                		return getComparisonResults(o)
                			.stream()
                			.map(res -> {
                				if (!isNullOrEmpty(res.getError())) {
                					return failure("Foo", ValidationResult.ValidationType.CARDINALITY, "Foo", path, "", res.getError());
                				}
                				return success("Foo", ValidationResult.ValidationType.CARDINALITY, "Foo", path, "");
                			})
                			.collect(toList());
                	}

                }
                """,
                code.get("com.rosetta.test.model.validation.FooValidator"));
        assertEquals("""
                package com.rosetta.test.model.validation;

                import com.google.common.collect.Lists;
                import com.rosetta.model.lib.expression.ComparisonResult;
                import com.rosetta.model.lib.path.RosettaPath;
                import com.rosetta.model.lib.validation.ValidationResult;
                import com.rosetta.model.lib.validation.Validator;
                import com.rosetta.test.model.Foo;
                import java.math.BigDecimal;
                import java.util.List;

                import static com.google.common.base.Strings.isNullOrEmpty;
                import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
                import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkString;
                import static com.rosetta.model.lib.validation.ValidationResult.failure;
                import static com.rosetta.model.lib.validation.ValidationResult.success;
                import static java.util.Optional.empty;
                import static java.util.Optional.of;
                import static java.util.stream.Collectors.toList;

                public class FooTypeFormatValidator implements Validator<Foo> {

                	private List<ComparisonResult> getComparisonResults(Foo o) {
                		return Lists.<ComparisonResult>newArrayList(
                				checkNumber("c", o.getC(), empty(), of(0), empty(), empty()),\s
                				checkNumber("d", o.getD(), empty(), empty(), of(new BigDecimal("-1")), empty()),\s
                				checkString("f", o.getF(), 0, of(5), empty())
                			);
                	}

                	@Override
                	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Foo o) {
                		return getComparisonResults(o)
                			.stream()
                			.map(res -> {
                				if (!isNullOrEmpty(res.getError())) {
                					return failure("Foo", ValidationResult.ValidationType.TYPE_FORMAT, "Foo", path, "", res.getError());
                				}
                				return success("Foo", ValidationResult.ValidationType.TYPE_FORMAT, "Foo", path, "");
                			})
                			.collect(toList());
                	}

                }
                """,
                code.get("com.rosetta.test.model.validation.FooTypeFormatValidator"));

        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);

        Object fooMeta = UpstreamPortHarness.metaInstance(classes, "Foo");
        Object validator = UpstreamPortHarness.metaValidator(classes, fooMeta, "validator");
        Object typeFormatValidator = UpstreamPortHarness.metaValidator(classes, fooMeta, "typeFormatValidator");

        Object validFoo = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo", Map.of(
                "a", List.of("test"),
                "b", new BigDecimal("123.42"),
                "c", List.of(-2),
                "d", new BigDecimal("0"),
                "f", List.of("abcde", "")));
        assertTrue(success(UpstreamPortHarness.validationResults(validator, validFoo).get(0)));
        assertTrue(success(UpstreamPortHarness.validationResults(typeFormatValidator, validFoo).get(0)));

        Object invalidFoo1 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo", Map.of(
                "a", List.of("a", "b", "c"),
                "c", List.of(),
                "f", List.of()));
        List<Object> res1 = failedResults(UpstreamPortHarness.validationResults(validator, invalidFoo1));
        List<String> reasons1 = failureReasons(res1);
        assertTrue(reasons1.contains("Maximum of 2 'a' are expected but found 3."), reasons1.toString());
        assertTrue(reasons1.contains("'b' is a required field but does not exist."), reasons1.toString());
        assertTrue(reasons1.contains("'c' is a required field but does not exist."), reasons1.toString());
        assertAllValidationType(res1, "CARDINALITY");
        assertTrue(success(UpstreamPortHarness.validationResults(typeFormatValidator, invalidFoo1).get(0)));

        Object invalidFoo2 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo", Map.of(
                "a", List.of("a", "b"),
                "b", new BigDecimal("123.42"),
                "c", List.of(-2),
                "d", new BigDecimal("-1.1"),
                "f", List.of("aaaaaa", "bb", "ccccccc")));
        assertTrue(success(UpstreamPortHarness.validationResults(validator, invalidFoo2).get(0)));
        List<Object> res2 = failedResults(UpstreamPortHarness.validationResults(typeFormatValidator, invalidFoo2));
        List<String> reasons2 = failureReasons(res2);
        assertTrue(reasons2.contains(
                "Expected a number greater than or equal to -1 for 'd', but found -1.1."),
                reasons2.toString());
        assertTrue(reasons2.contains(
                "Field 'f' must have a value with maximum length of 5 characters but value 'aaaaaa' has length of 6 characters. "
                + "- Field 'f' must have a value with maximum length of 5 characters but value 'ccccccc' has length of 7 characters."),
                reasons2.toString());
        assertAllValidationType(res2, "TYPE_FORMAT");
    }

    /** Upstream {@code shouldGenerateUserFriendlyTypeFormatValidationErrors}. */
    @Test
    void shouldGenerateUserFriendlyTypeFormatValidationErrors() {
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(
                UpstreamPortHarness.generateCode("""
                type A:
                	a string(minLength: 3, pattern: "A.*Z") (1..3)
                	b string(maxLength: 5) (1..1)

                type B:
                	a number(digits: 3) (1..1)
                	b number(fractionalDigits: 2) (1..1)
                	c number(min: 0, max: 10) (1..1)
                """));

        Object aMeta = UpstreamPortHarness.metaInstance(classes, "A");
        Object aTypeFormatValidator = UpstreamPortHarness.metaValidator(classes, aMeta, "typeFormatValidator");

        Object invalidA1 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "A", Map.of(
                "a", List.of("AZ", "ABZ", "AA"),
                "b", "AAAAAA"));
        List<String> resA1 = failureReasons(failedResults(
                UpstreamPortHarness.validationResults(aTypeFormatValidator, invalidA1)));
        assertTrue(resA1.contains(
                "Field 'a' requires a value with minimum length of 3 characters but value 'AZ' has length of 2 characters. "
                + "- Field 'a' requires a value with minimum length of 3 characters but value 'AA' has length of 2 characters. "
                + "Field 'a' with value 'AA' does not match the pattern /A.*Z/."),
                resA1.toString());
        assertTrue(resA1.contains(
                "Field 'b' must have a value with maximum length of 5 characters but value 'AAAAAA' has length of 6 characters."),
                resA1.toString());

        Object bMeta = UpstreamPortHarness.metaInstance(classes, "B");
        Object bTypeFormatValidator = UpstreamPortHarness.metaValidator(classes, bMeta, "typeFormatValidator");

        Object invalidB1 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "B", Map.of(
                "a", new BigDecimal("-1000"),
                "b", new BigDecimal("13.1415"),
                "c", new BigDecimal("-1")));
        List<String> resB1 = failureReasons(failedResults(
                UpstreamPortHarness.validationResults(bTypeFormatValidator, invalidB1)));
        assertTrue(resB1.contains("Expected a maximum of 3 digits for 'a', but the number -1000 has 4."),
                resB1.toString());
        assertTrue(resB1.contains("Expected a maximum of 2 fractional digits for 'b', but the number 13.1415 has 4."),
                resB1.toString());
        assertTrue(resB1.contains("Expected a number greater than or equal to 0 for 'c', but found -1."),
                resB1.toString());

        Object invalidB2 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "B", Map.of(
                "a", new BigDecimal("10.01"),
                "b", new BigDecimal("-123.14"),
                "c", new BigDecimal("11")));
        List<String> resB2 = failureReasons(failedResults(
                UpstreamPortHarness.validationResults(bTypeFormatValidator, invalidB2)));
        assertTrue(resB2.contains("Expected a maximum of 3 digits for 'a', but the number 10.01 has 4."),
                resB2.toString());
        assertTrue(resB2.contains("Expected a number less than or equal to 10 for 'c', but found 11."),
                resB2.toString());

        Object invalidB3 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "B", Map.of(
                "a", new BigDecimal("0.001"),
                "b", new BigDecimal("0"),
                "c", new BigDecimal("0")));
        Object resB3 = UpstreamPortHarness.validationResults(bTypeFormatValidator, invalidB3).get(0);
        assertFalse(success(resB3));
        assertEquals("Expected a maximum of 3 digits for 'a', but the number 0.001 has 4.",
                ((Optional<?>) UpstreamPortHarness.call(resB3, "getFailureReason")).get());
    }

    /**
     * Upstream {@code typeFormatValidationShouldWorkForDifferentJavaNumberTypes} —
     * un-pinned at PR #424 (leg-C finding #2 HEALED — facet inlineNumberLadder:
     * {@code GeneratorModel.resolveTypeCall} now applies an inline type call's own
     * literal args over the builtin base via
     * {@code TypeAliasSolver.applyDirectTypeCallArguments}, so
     * {@code number(digits: N, fractionalDigits: 0)} ladders
     * Integer/Long/BigInteger exactly like the typeAlias-wrapped form; the
     * {@code pojo-number-ladder} oracle group is the byte witness, 5/5
     * byte-identical). Body = the upstream test verbatim: integer-typed builder
     * drives ({@code List.of(1, 2, 3)} / {@code 4L} / {@code BigInteger.valueOf(5)})
     * through assignability-matched setters + the type-format success assert.
     */
    @Test
    void typeFormatValidationShouldWorkForDifferentJavaNumberTypes() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type A:
                	integers number(digits: 8, fractionalDigits: 0) (0..*)
                	long number(digits: 10, fractionalDigits: 0) (1..1)
                	bigInteger number(digits: 20, fractionalDigits: 0) (1..1)
                """);
        String a = code.get(UpstreamPortHarness.ROOT_PACKAGE + ".A");
        assertTrue(a.contains("Long getLong();"),
                "healed ladder drift: inline number(digits:10, fractionalDigits:0) must map Long in:\n" + a);
        assertTrue(a.contains("BigInteger getBigInteger();"),
                "healed ladder drift: inline number(digits:20, fractionalDigits:0) must map BigInteger in:\n" + a);

        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object aMeta = UpstreamPortHarness.metaInstance(classes, "A");
        Object aTypeFormatValidator = UpstreamPortHarness.metaValidator(classes, aMeta, "typeFormatValidator");

        // `invalidA1` is upstream's OWN variable name (ModelMetaGeneratorTest.xtend —
        // ported 1:1; the values are digit-conforming and the assert expects success).
        Object invalidA1 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "A", Map.of(
                "integers", List.of(1, 2, 3),
                "long", 4L,
                "bigInteger", BigInteger.valueOf(5)));
        assertTrue(success(UpstreamPortHarness.validationResults(aTypeFormatValidator, invalidA1).get(0)));
    }
}
