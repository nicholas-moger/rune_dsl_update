package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.generator.java.expression.TypeCoercionService;
import com.regnosys.rosetta.generator.java.scoping.JavaPackageName;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RJavaFieldWithMeta;
import com.regnosys.rosetta.generator.java.types.RJavaReferenceWithMeta;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaPrimitiveType;
import com.rosetta.util.types.JavaType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Leg-C port (PR #425, slice 3a) of upstream
 * {@code rune-integration-tests/.../expression/TypeCoercionTest.xtend} — 10
 * methods driving the coercion service DIRECTLY at its RAW seat.
 *
 * <p><b>THE CHANNEL-SPLIT LAW (every divergence below traces to it — the #412
 * retro-3 rider honoured by measuring, not locking, the raw seat):</b> upstream's
 * {@code TypeCoercionService} itself declares intermediates
 * ({@code declareAsVariable}), null-guards them and meta-wraps values, so its
 * unit test sees {@code {\n final X x = …;\n return x == null ? … : …;\n}}
 * envelopes. The fork deliberately splits those duties: the SERVICE emits the
 * bare conversion phrase and the RENDERER seats add declare/guard/meta-wrap
 * choreography where golden-witnessed (the M7b grind's architecture; the #421
 * with-meta facets own value→wrapper construction). The COMPOSED output is
 * byte-identical on every witnessed shape — 34,685/34,686 corpus (the one
 * PERMANENT missing-output waiver aside) + 319/319 hold-out goldens — so this
 * port asserts the fork's MEASURED raw-seat forms and
 * documents upstream's envelope per case (nothing silent). Genuine raw-seat-only
 * corners recorded inline: the primitive-int→Long identity (the BOXED path
 * casts {@code (long) x} — {@code ItemToItemCoercerTest.int_to_long}; zero
 * corpus witnesses either way); the untyped {@code Collections.emptyList()}
 * empty witness the port had recorded here CONVERGED at v3.2 seat 9 (PR #630 —
 * the coercer's List arm carries the item witness, upstream's
 * {@code Collections.<T>emptyList()}, since the {@code nothing} render law; the
 * two pins below moved to upstream's form, RED in the chain's whole generator
 * suite at {@code 44d323815} — the seat's own catch).
 *
 * <p><b>The adapter:</b> statement = {@code coerce(…).completeAsReturn()}
 * rendered through the SAME deferred-name finalization hook the renderer uses
 * ({@code JavaStatementScope.resolveUnifiedDeferredNames} — the #419
 * to-time-lambda law: a raw-driven scope leaves {@code __COERCION_PARAM_n__}
 * sentinels otherwise); refs = the builder's ref FQN set with the INPUT
 * expression's refs modeled exactly as upstream's template interpolations
 * ({@code «MapperS».of(42)} carries the MapperS ref). Upstream's
 * {@code addCoercions(expr, expected, scope)} reads the actual type off the
 * expression; the fork's {@code coerce(expr, actual, expected, scope)} takes it
 * explicitly — same inputs.
 * Ledger: the development audit "2026-07-17-leg-c-integration-port-ledger".
 */
class UpstreamTypeCoercionPortTest {

    private static final JavaTypeUtil TU = new JavaTypeUtil();
    private static final TypeCoercionService SERVICE = new TypeCoercionService(TU);
    private static final JavaPackageName TEST_PKG = JavaPackageName.splitOnDotsAndEscape("test");

    private static final JavaClass<?> MAPPER_S_REF = JavaClass.from(MapperS.class);
    private static final JavaClass<?> MAPPER_C_REF = JavaClass.from(MapperC.class);
    private static final JavaClass<?> COMP_RES_REF = JavaClass.from(ComparisonResult.class);
    private static final JavaClass<?> BIG_DECIMAL_REF = JavaClass.from(BigDecimal.class);
    private static final JavaClass<?> BIG_INTEGER_REF = JavaClass.from(BigInteger.class);
    private static final JavaClass<?> ARRAYS_REF = JavaClass.from(Arrays.class);

    /**
     * The port's assertCoercion: the coerced statement text (finalized through the
     * renderer's deferred-name hook) + the ref FQN set (input refs modeled as
     * upstream's template interpolations; java.lang never imports on either side).
     */
    private static void assertCoercion(String expectedStatement, List<String> expectedRefs,
            String exprCode, Set<JavaClass<?>> exprRefs, JavaType actual, JavaType expected) {
        JavaStatementScope scope = new JavaStatementScope("test", null);
        JavaStatementBuilder coerced = SERVICE.coerce(
                JavaExpression.from(exprCode, actual, exprRefs, Set.of()),
                actual, expected, scope);
        String stmt = coerced.completeAsReturn().toString();
        stmt = JavaStatementScope.resolveUnifiedDeferredNames(
                List.of(scope), List.of(), List.of(stmt), Map.of()).get(0);
        assertEquals(expectedStatement.stripTrailing(), stmt.stripTrailing(), "statement text");
        List<String> refFqns = coerced.getRefs().stream()
                .map(c -> c.getPackageName() + "." + c.getSimpleName())
                .filter(n -> !n.startsWith("java.lang."))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        assertEquals(expectedRefs.stream().sorted().toList(), refFqns, "ref set");
    }

    /**
     * Upstream {@code testConvertBigDecimalToFieldWithMetaInteger} — upstream
     * declares, null-guards and META-WRAPS
     * ({@code … ? FieldWithMetaInteger.builder().build() :
     * ….setValue(bigDecimal.intValueExact()).build()}); the fork's raw seat
     * narrows the VALUE only ({@code intValueExact}) — value→meta construction is
     * the renderer's (the #421 with-meta facets). MEASURED: the raw seat emits the
     * item conversion alone; the meta target leaves the phrase unwrapped.
     */
    @Test
    void testConvertBigDecimalToFieldWithMetaInteger() {
        JavaType expectedType = new RJavaFieldWithMeta(TU.INTEGER, TEST_PKG, TU);
        assertCoercion("return BigDecimal.valueOf(10);",
                List.of("java.math.BigDecimal"),
                "BigDecimal.valueOf(10)", Set.of(BIG_DECIMAL_REF), TU.BIG_DECIMAL, expectedType);
    }

    /**
     * Upstream {@code testConvertFieldWithMetaIntegerToBigDecimal} — upstream:
     * decl + if-null-return-null + {@code getValue()} decl + guarded
     * {@code BigDecimal.valueOf}. The fork's raw seat: the inline unwrap-and-widen
     * phrase (guards are renderer choreography).
     */
    @Test
    void testConvertFieldWithMetaIntegerToBigDecimal() {
        JavaType actualType = new RJavaFieldWithMeta(TU.INTEGER, TEST_PKG, TU);
        assertCoercion(
                "return BigDecimal.valueOf(FieldWithMetaInteger.builder().setValue(10).build().getValue());",
                List.of("java.math.BigDecimal"),
                "FieldWithMetaInteger.builder().setValue(10).build()", Set.of(),
                actualType, TU.BIG_DECIMAL);
    }

    /**
     * Upstream {@code testConvertMetaReferenceToMetaField} — upstream re-wraps the
     * unwrapped value into the TARGET wrapper's builder behind guards; the fork's
     * raw seat unwraps only ({@code .getValue()}) — the re-wrap is the renderer's.
     */
    @Test
    void testConvertMetaReferenceToMetaField() {
        JavaType actualType = new RJavaReferenceWithMeta(TU.STRING, TEST_PKG, TU);
        JavaType expectedType = new RJavaFieldWithMeta(TU.STRING, TEST_PKG, TU);
        assertCoercion(
                "return ReferenceWithMetaString.builder().setValue(\"foo\").build().getValue();",
                List.of(),
                "ReferenceWithMetaString.builder().setValue(\"foo\").build()", Set.of(),
                actualType, expectedType);
    }

    /** Upstream {@code testConvertMetaFieldToMetaReference} — the mirror direction, same law. */
    @Test
    void testConvertMetaFieldToMetaReference() {
        JavaType actualType = new RJavaFieldWithMeta(TU.STRING, TEST_PKG, TU);
        JavaType expectedType = new RJavaReferenceWithMeta(TU.STRING, TEST_PKG, TU);
        assertCoercion(
                "return FieldWithMetaString.builder().setValue(\"foo\").build().getValue();",
                List.of(),
                "FieldWithMetaString.builder().setValue(\"foo\").build()", Set.of(),
                actualType, expectedType);
    }

    /**
     * Upstream {@code testConvertStringToMeta} — upstream declares + guards + wraps
     * into the meta builder (both wrapper kinds); the fork's raw seat passes the
     * value through UNCHANGED (value→meta construction is the renderer's — the
     * #421 with-meta facets are its byte witnesses).
     */
    @Test
    void testConvertStringToMeta() {
        assertCoercion("return \"foo\";", List.of(),
                "\"foo\"", Set.of(), TU.STRING, new RJavaFieldWithMeta(TU.STRING, TEST_PKG, TU));
        assertCoercion("return \"foo\";", List.of(),
                "\"foo\"", Set.of(), TU.STRING, new RJavaReferenceWithMeta(TU.STRING, TEST_PKG, TU));
    }

    /**
     * Upstream {@code testConvertMetaToString} — upstream declares + null-guards the
     * unwrap; the fork's raw seat emits the bare {@code .getValue()} deref. The
     * second case keeps upstream's own stray-semicolon fixture verbatim (the input
     * expr ends {@code ;}), producing the same mid-phrase artifact upstream's decl
     * absorbs — measured as-is.
     */
    @Test
    void testConvertMetaToString() {
        assertCoercion(
                "return FieldWithMetaString.builder().setValue(\"foo\").build().getValue();",
                List.of(),
                "FieldWithMetaString.builder().setValue(\"foo\").build()", Set.of(),
                new RJavaFieldWithMeta(TU.STRING, TEST_PKG, TU), TU.STRING);
        assertCoercion(
                "return ReferenceWithMetaString.builder().setValue(\"foo\").build();.getValue();",
                List.of(),
                "ReferenceWithMetaString.builder().setValue(\"foo\").build();", Set.of(),
                new RJavaReferenceWithMeta(TU.STRING, TEST_PKG, TU), TU.STRING);
    }

    /**
     * Upstream {@code testItemToItemConversion} — five cases. Divergence notes:
     * case 1 primitive-int→Long MATCHES upstream ({@code return (long) 42;}) since
     * v3.2 seat 1 (PR #622): a primitive item promotes through its box's table
     * ({@code ItemToItemCoercer}), so the raw seat now carries the cast the BOXED
     * Integer→Long path always did ({@code ItemToItemCoercerTest.int_to_long});
     * before the seat the raw seat was identity — zero corpus witnesses of the
     * primitive-source form either way; cases 2/3 emit the bare conversion phrase
     * (upstream declares + guards); case 4 Boolean→boolean is identity (upstream
     * declares + null-defaults {@code ? false :}); case 5 matches upstream.
     */
    @Test
    void testItemToItemConversion() {
        assertCoercion("return (long) 42;", List.of(),
                "42", Set.of(), JavaPrimitiveType.INT, TU.LONG);

        assertCoercion("return BigDecimal.valueOf(Integer.valueOf(42));",
                List.of("java.math.BigDecimal"),
                "Integer.valueOf(42)", Set.of(), TU.INTEGER, TU.BIG_DECIMAL);

        assertCoercion("return BigInteger.valueOf(42).longValueExact();",
                List.of("java.math.BigInteger"),
                "BigInteger.valueOf(42)", Set.of(BIG_INTEGER_REF), TU.BIG_INTEGER, TU.LONG);

        assertCoercion("return Boolean.valueOf(true);", List.of(),
                "Boolean.valueOf(true)", Set.of(), TU.BOOLEAN, JavaPrimitiveType.BOOLEAN);

        assertCoercion("return null;", List.of(),
                "null", Set.of(), TU.VOID, TU.LONG);
    }

    /**
     * Upstream {@code testItemToWrapperConversion} — four cases. Cases 1 and 4
     * match upstream exactly; case 2 emits the unguarded wrap (upstream declares +
     * guards with the {@code MapperC.<BigInteger>ofNull()} arm — the BigInteger
     * import exists upstream ONLY for that guard arm); case 3's empty witness carries
     * the item type since v3.2 seat 9 (#630) — upstream's own
     * {@code Collections.<Long>emptyList()}, the raw seat converged.
     */
    @Test
    void testItemToWrapperConversion() {
        assertCoercion("return MapperS.of(42);",
                List.of("com.rosetta.model.lib.mapper.MapperS"),
                "42", Set.of(), JavaPrimitiveType.INT, TU.wrap(TU.MAPPER_S, TU.INTEGER));

        assertCoercion(
                "return MapperC.of(Collections.singletonList(BigDecimal.valueOf(42).toBigIntegerExact()));",
                List.of("com.rosetta.model.lib.mapper.MapperC", "java.math.BigDecimal",
                        "java.util.Collections"),
                "BigDecimal.valueOf(42)", Set.of(BIG_DECIMAL_REF),
                TU.BIG_DECIMAL, TU.wrap(TU.MAPPER_C, TU.BIG_INTEGER));

        // MEASURED at v3.2 seat 9 (#630): the typed empty-value phrase registers the
        // Collections ref at the raw seat too (upstream imports java.util.Collections
        // here) — the pre-seat bare form had registered NO ref (the renderer's
        // empty-list seats owned the import); both halves of the corner converged
        // with the item witness (scratch/t-targeted-pre8.log, local).
        assertCoercion("return Collections.<Long>emptyList();",
                List.of("java.util.Collections"),
                "null", Set.of(), TU.VOID, TU.wrap(TU.LIST, TU.LONG));

        assertCoercion("return ComparisonResult.ofNullSafe(MapperS.of(Boolean.valueOf(true)));",
                List.of("com.rosetta.model.lib.expression.ComparisonResult",
                        "com.rosetta.model.lib.mapper.MapperS"),
                "Boolean.valueOf(true)", Set.of(), TU.BOOLEAN, TU.COMPARISON_RESULT);
    }

    /**
     * Upstream {@code testWrapperToItemConversion} — five cases. Cases 1/4/5 match
     * upstream exactly; cases 2/3 emit the bare narrow-after-get phrase (upstream
     * declares the {@code .get()} result and guards the narrowing).
     */
    @Test
    void testWrapperToItemConversion() {
        assertCoercion("return MapperS.of(\"ABC\").get();",
                List.of("com.rosetta.model.lib.mapper.MapperS"),
                "MapperS.of(\"ABC\")", Set.of(MAPPER_S_REF),
                TU.wrap(TU.MAPPER_S, TU.STRING), TU.STRING);

        assertCoercion("return Math.toIntExact(MapperS.of(42).get());",
                List.of("com.rosetta.model.lib.mapper.MapperS"),
                "MapperS.of(42)", Set.of(MAPPER_S_REF),
                TU.wrap(TU.MAPPER_S, TU.LONG), TU.INTEGER);

        assertCoercion("return BigInteger.valueOf(MapperC.of(Arrays.asList(1, 2, 3)).get());",
                List.of("com.rosetta.model.lib.mapper.MapperC", "java.math.BigInteger",
                        "java.util.Arrays"),
                "MapperC.of(Arrays.asList(1, 2, 3))", Set.of(MAPPER_C_REF, ARRAYS_REF),
                TU.wrap(TU.MAPPER_C, TU.INTEGER), TU.BIG_INTEGER);

        assertCoercion("return ComparisonResult.success().get();",
                List.of("com.rosetta.model.lib.expression.ComparisonResult"),
                "ComparisonResult.success()", Set.of(COMP_RES_REF),
                TU.COMPARISON_RESULT, TU.BOOLEAN);

        assertCoercion("return null;", List.of(),
                "MapperS.ofNull()", Set.of(MAPPER_S_REF),
                TU.wrap(TU.MAPPER_S, TU.VOID), TU.INTEGER);
    }

    /**
     * Upstream {@code testWrapperToWrapperConversion} — five cases. Cases 1/4 match
     * upstream exactly; cases 2/3 carry the fork's {@code new ArrayList<>(…)}
     * defensive copy around {@code getMulti()} (upstream returns the bare
     * {@code getMulti()} list) with the item-coercion lambda param finalized
     * through the deferred-name hook ({@code _long} — upstream's own param name);
     * case 5's empty witness carries the item type since v3.2 seat 9 (#630) —
     * upstream's own {@code Collections.<String>emptyList()}, the raw seat converged.
     */
    @Test
    void testWrapperToWrapperConversion() {
        assertCoercion("return ComparisonResult.ofNullSafe(MapperS.of(true));",
                List.of("com.rosetta.model.lib.expression.ComparisonResult",
                        "com.rosetta.model.lib.mapper.MapperS"),
                "MapperS.of(true)", Set.of(MAPPER_S_REF),
                TU.wrap(TU.MAPPER_S, TU.BOOLEAN), TU.COMPARISON_RESULT);

        assertCoercion(
                "return new ArrayList<>(MapperS.of(42).<Integer>map(\"Type coercion\", _long -> _long == null ? null : Math.toIntExact(_long)).getMulti());",
                List.of("com.rosetta.model.lib.mapper.MapperS", "java.util.ArrayList"),
                "MapperS.of(42)", Set.of(MAPPER_S_REF),
                TU.wrap(TU.MAPPER_S, TU.LONG), TU.wrap(TU.LIST, TU.INTEGER));

        assertCoercion(
                "return new ArrayList<>(MapperC.of(Arrays.asList(1, 2, 3)).<BigInteger>map(\"Type coercion\", integer -> BigInteger.valueOf(integer)).getMulti());",
                List.of("com.rosetta.model.lib.mapper.MapperC", "java.math.BigInteger",
                        "java.util.ArrayList", "java.util.Arrays"),
                "MapperC.of(Arrays.asList(1, 2, 3))", Set.of(MAPPER_C_REF, ARRAYS_REF),
                TU.wrap(TU.MAPPER_C, TU.INTEGER), TU.wrap(TU.LIST, TU.BIG_INTEGER));

        assertCoercion("return ComparisonResult.success().asMapper();",
                List.of("com.rosetta.model.lib.expression.ComparisonResult"),
                "ComparisonResult.success()", Set.of(COMP_RES_REF),
                TU.COMPARISON_RESULT, TU.wrap(TU.MAPPER_S, TU.BOOLEAN));

        // MEASURED at v3.2 seat 9 (#630): the same convergence as the item-side empty
        // case — the Collections ref registered at the raw seat (upstream imports
        // java.util.Collections); the input MapperS ref is still dropped with the whole
        // input phrase — the empty-value arm replaces it.
        assertCoercion("return Collections.<String>emptyList();",
                List.of("java.util.Collections"),
                "MapperS.ofNull()", Set.of(MAPPER_S_REF),
                TU.wrap(TU.MAPPER_S, TU.VOID), TU.wrap(TU.LIST, TU.STRING));
    }
}
