package com.regnosys.rosetta.generator.java.expression.coercers;

import com.regnosys.rosetta.generator.java.expression.TypeCoercionService;
import com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;

import java.util.Set;

/**
 * Coerces between unwrapped item types: number promotions, downcast.
 * Implements the item-to-item quadrant of the dispatch matrix.
 *
 * <h3>Number promotion/narrowing (12 patterns)</h3>
 * <ul>
 *   <li>Widening (int→long, int→BigInteger, etc.) — always safe, no conditional</li>
 *   <li>Narrowing (long→int, BigInteger→int, etc.) — throwOnFail controls behavior</li>
 * </ul>
 *
 * <h3>Subtype downcast</h3>
 * <p>When expected is a subtype of actual, emit a cast (with optional instanceof guard
 * when throwOnFail=false).</p>
 */
public class ItemToItemCoercer implements TypeCoercer {

    @Override
    public JavaStatementBuilder coerce(
            JavaStatementBuilder expr, JavaType actualTypeIn,
            JavaType expectedTypeIn, boolean throwOnFail,
            JavaStatementScope scope, JavaTypeUtil typeUtil,
            TypeCoercionService service) {

        // v3.2 seat 1 (F2 dropped-coercion): a PRIMITIVE item (the count operator's `int` —
        // upstream's caseCountOperation stamp, carried by the fork since this seat) promotes
        // through the same table as its box: `BigDecimal.valueOf(<int>)`. The null-guard
        // question is the CALLER's, decided from the unboxed type (a primitive can never be
        // null: the wrapper arms and the parameter form guard boxed items only).
        final JavaType actualType =
                actualTypeIn instanceof com.rosetta.util.types.JavaPrimitiveType primitiveActual
                        ? primitiveActual.toReferenceType() : actualTypeIn;
        final JavaType expectedType =
                expectedTypeIn instanceof com.rosetta.util.types.JavaPrimitiveType primitiveExpected
                        ? primitiveExpected.toReferenceType() : expectedTypeIn;
        if (actualType.equals(expectedType)) {
            return expr;
        }

        // Meta-value unwrap: RJavaWithMetaValue → its value type. Mirror upstream
        // metaToItemConversionExpression — emit `«expr».getValue()` typed as the
        // meta's valueType, then a further item→item conversion to the expected type
        // (identity when valueType == expected, the dominant case). This is the ~99%
        // coercion shape in the drr goldens (meta-unwrap, not number promotion).
        if (actualType instanceof RJavaWithMetaValue meta) {
            JavaType valueType = meta.getValueType();
            return expr.mapExpression(e -> {
                JavaExpression unwrapped = JavaExpression.from(
                        e.renderToString() + ".getValue()", valueType,
                        e.getRefs(), e.getStaticWildcardImports());
                return service.coerceExpression(
                        unwrapped, valueType, expectedType, throwOnFail, scope);
            });
        }

        // Number promotions / narrowing
        if (typeUtil.extendsNumber(actualType) && typeUtil.extendsNumber(expectedType)) {
            return coerceNumber(expr, actualType, expectedType, throwOnFail, typeUtil,
                    actualTypeIn instanceof com.rosetta.util.types.JavaPrimitiveType);
        }

        // Subtype downcast: expected is a strict subtype of actual (not identity)
        if (!expectedType.equals(actualType) && expectedType.isSubtypeOf(actualType)) {
            return coerceDowncast(expr, actualType, expectedType, throwOnFail, typeUtil);
        }

        // No coercion applicable — return unchanged
        return expr;
    }

    private JavaStatementBuilder coerceNumber(
            JavaStatementBuilder expr, JavaType actualType,
            JavaType expectedType, boolean throwOnFail,
            JavaTypeUtil typeUtil, boolean actualPrimitive) {

        return expr.mapExpression(e -> {
            String code = e.renderToString();
            Set<JavaClass<?>> baseRefs = e.getRefs();
            Set<JavaClass<?>> baseWildcards = e.getStaticWildcardImports();

            // int → long: the PRIMITIVE (an int literal, the count's int) takes the widening cast `(long) 42`;
            // a BOXED Integer takes `.longValue()` (v3.2 seat 13, D53 - the M5d heal, oracle group
            // conv-int-literal-long-output: `r = (long) 42;` beside `r = n.longValue();` in the released plugin's
            // goldens; the pre-seat single cast form had no golden witness for the boxed item).
            // #634 round 1, cq NIT-3 DECLINED at the site: `.longValue()` is appended UNPARENTHESISED like the
            // sibling `(long) ` cast - the oracle golden `r = n.longValue();` (LongOutIntInput.java in that group) pins
            // the bare form on the identifier the boxed arm meets, and a parenthesise-unless-simple rule would be a
            // decision on rendered text; a boxed operand that is not a postfix expression has no witness at this
            // arm (banked with the seat's other unwitnessed shapes).
            if (typeUtil.isInteger(actualType) && typeUtil.isLong(expectedType)) {
                return JavaExpression.from(actualPrimitive ? "(long) " + code : code + ".longValue()",
                        expectedType, baseRefs, baseWildcards);
            }
            if (typeUtil.isInteger(actualType) && typeUtil.isBigInteger(expectedType)) {
                return JavaExpression.from("BigInteger.valueOf(" + code + ")", expectedType,
                        HandlerHelper.union(baseRefs, Set.of(HandlerHelper.BIG_INTEGER)), baseWildcards);
            }
            if (typeUtil.isInteger(actualType) && typeUtil.isBigDecimal(expectedType)) {
                return JavaExpression.from("BigDecimal.valueOf(" + code + ")", expectedType,
                        HandlerHelper.union(baseRefs, Set.of(HandlerHelper.BIG_DECIMAL)), baseWildcards);
            }

            if (typeUtil.isLong(actualType) && typeUtil.isInteger(expectedType)) {
                if (throwOnFail) {
                    return JavaExpression.from("Math.toIntExact(" + code + ")", expectedType,
                            baseRefs, baseWildcards);
                }
                // Upstream TypeCoercionService null-safe form (9.83 line, vendored source
                // L479): `«i» <= Integer.MAX_VALUE && «i» >= Integer.MIN_VALUE ? (int) «i»
                // : null` — MAX before MIN, plain (int) cast. The fork's prior
                // MIN-first/intValueExact-free form was zero-witness latent divergence
                // (no corpus golden exercises a null-safe numeric narrowing — D11 green
                // proves it); healed at the builder-compat burn (PR #412), where the POJO
                // seat makes the narrowing forms golden-witnessed for the first time.
                return nullSafeNarrowing(code, "(int) " + code,
                        code + " <= Integer.MAX_VALUE && " + code + " >= Integer.MIN_VALUE",
                        expectedType, baseRefs, baseWildcards);
            }
            if (typeUtil.isLong(actualType) && typeUtil.isBigInteger(expectedType)) {
                return JavaExpression.from("BigInteger.valueOf(" + code + ")", expectedType,
                        HandlerHelper.union(baseRefs, Set.of(HandlerHelper.BIG_INTEGER)), baseWildcards);
            }
            if (typeUtil.isLong(actualType) && typeUtil.isBigDecimal(expectedType)) {
                return JavaExpression.from("BigDecimal.valueOf(" + code + ")", expectedType,
                        HandlerHelper.union(baseRefs, Set.of(HandlerHelper.BIG_DECIMAL)), baseWildcards);
            }

            if (typeUtil.isBigInteger(actualType) && typeUtil.isInteger(expectedType)) {
                if (throwOnFail) {
                    return JavaExpression.from(code + ".intValueExact()", expectedType,
                            baseRefs, baseWildcards);
                }
                // Upstream null-safe form (vendored L504): round-trip equality guard +
                // PLAIN intValue() conversion — `BigInteger.valueOf(«i».intValue())
                // .equals(«i») ? «i».intValue() : null`. Golden-witnessed at the POJO
                // seat (Foo3 setNumberAttr(BigInteger), PR #412); the prior
                // bitLength/intValueExact form was zero-witness latent divergence.
                return nullSafeNarrowing(code, code + ".intValue()",
                        "BigInteger.valueOf(" + code + ".intValue()).equals(" + code + ")",
                        expectedType,
                        HandlerHelper.union(baseRefs, Set.of(HandlerHelper.BIG_INTEGER)),
                        baseWildcards);
            }
            if (typeUtil.isBigInteger(actualType) && typeUtil.isLong(expectedType)) {
                if (throwOnFail) {
                    return JavaExpression.from(code + ".longValueExact()", expectedType,
                            baseRefs, baseWildcards);
                }
                // Upstream null-safe form (vendored L519): `BigInteger.valueOf(
                // «i».longValue()).equals(«i») ? «i».longValue() : null`.
                return nullSafeNarrowing(code, code + ".longValue()",
                        "BigInteger.valueOf(" + code + ".longValue()).equals(" + code + ")",
                        expectedType,
                        HandlerHelper.union(baseRefs, Set.of(HandlerHelper.BIG_INTEGER)),
                        baseWildcards);
            }
            if (typeUtil.isBigInteger(actualType) && typeUtil.isBigDecimal(expectedType)) {
                return JavaExpression.from("new BigDecimal(" + code + ")", expectedType,
                        HandlerHelper.union(baseRefs, Set.of(HandlerHelper.BIG_DECIMAL)), baseWildcards);
            }

            if (typeUtil.isBigDecimal(actualType) && typeUtil.isInteger(expectedType)) {
                if (throwOnFail) {
                    return JavaExpression.from(code + ".intValueExact()", expectedType,
                            baseRefs, baseWildcards);
                }
                // Upstream null-safe form (vendored L541): round-trip compareTo guard +
                // PLAIN intValue() — `BigDecimal.valueOf(«d».intValue()).compareTo(«d»)
                // == 0 ? «d».intValue() : null`. Golden-witnessed at the POJO seat
                // (Foo3 setNumberAttr(BigDecimal), PR #412).
                return nullSafeNarrowing(code, code + ".intValue()",
                        "BigDecimal.valueOf(" + code + ".intValue()).compareTo(" + code + ") == 0",
                        expectedType,
                        HandlerHelper.union(baseRefs, Set.of(HandlerHelper.BIG_DECIMAL)),
                        baseWildcards);
            }
            if (typeUtil.isBigDecimal(actualType) && typeUtil.isLong(expectedType)) {
                if (throwOnFail) {
                    return JavaExpression.from(code + ".longValueExact()", expectedType,
                            baseRefs, baseWildcards);
                }
                // Upstream null-safe form (vendored L556): `BigDecimal.valueOf(
                // «d».longValue()).compareTo(«d») == 0 ? «d».longValue() : null`.
                return nullSafeNarrowing(code, code + ".longValue()",
                        "BigDecimal.valueOf(" + code + ".longValue()).compareTo(" + code + ") == 0",
                        expectedType,
                        HandlerHelper.union(baseRefs, Set.of(HandlerHelper.BIG_DECIMAL)),
                        baseWildcards);
            }
            if (typeUtil.isBigDecimal(actualType) && typeUtil.isBigInteger(expectedType)) {
                if (throwOnFail) {
                    return JavaExpression.from(code + ".toBigIntegerExact()", expectedType,
                            baseRefs, baseWildcards);
                }
                // emits `new BigDecimal(...).compareTo(...)` + operand .toBigInteger()
                return JavaExpression.from(
                        "new BigDecimal(" + code + ".toBigInteger()).compareTo(" + code + ") == 0 ? "
                                + code + ".toBigInteger() : null",
                        expectedType,
                        HandlerHelper.union(baseRefs, Set.of(HandlerHelper.BIG_DECIMAL)), baseWildcards);
            }

            return e;
        });
    }

    /**
     * Null-safe narrowing: {@code condition ? conversion : null}.
     */
    private JavaExpression nullSafeNarrowing(
            String exprCode, String conversion, String condition,
            JavaType expectedType,
            Set<JavaClass<?>> refs, Set<JavaClass<?>> wildcards) {
        return JavaExpression.from(
                condition + " ? " + conversion + " : null",
                expectedType,
                refs,
                wildcards);
    }

    /**
     * Subtype downcast: cast to the expected type, optionally with instanceof guard.
     *
     * <p>Upstream form ({@code downCastConversionExpression}, vendored L427-442):
     * {@code «expected».class.cast(«expr»)} — the {@code Class.cast} call, NOT a
     * parenthesized C-style cast — with the null-safe variant
     * {@code «e» instanceof «X» ? «X».class.cast(«e») : null}. The fork's prior
     * {@code (X) e} parens form was zero-witness latent divergence (no corpus golden
     * exercises an expression-seat downcast — D11 green proves it); healed at the
     * builder-compat burn (PR #412), where the POJO seat golden-witnesses the
     * instanceof/class.cast form (Foo2 setParent(Parent) et al.).
     */
    private JavaStatementBuilder coerceDowncast(
            JavaStatementBuilder expr, JavaType actualType,
            JavaType expectedType, boolean throwOnFail,
            JavaTypeUtil typeUtil) {
        return expr.mapExpression(e -> {
            String code = e.renderToString();
            String expectedName = expectedType.getSimpleName();

            if (throwOnFail) {
                return JavaExpression.from(
                        expectedName + ".class.cast(" + code + ")",
                        expectedType,
                        e.getRefs(),
                        e.getStaticWildcardImports());
            } else {
                return JavaExpression.from(
                        code + " instanceof " + expectedName
                                + " ? " + expectedName + ".class.cast(" + code + ") : null",
                        expectedType,
                        e.getRefs(),
                        e.getStaticWildcardImports());
            }
        });
    }

}
