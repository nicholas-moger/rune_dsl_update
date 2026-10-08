package com.regnosys.rosetta.ast;

import com.regnosys.rosetta.ast.enums.ArithOp;
import com.regnosys.rosetta.ast.enums.CardCheckOp;
import com.regnosys.rosetta.ast.enums.CardMod;
import com.regnosys.rosetta.ast.enums.CompOp;
import com.regnosys.rosetta.ast.enums.ConversionKind;
import com.regnosys.rosetta.ast.enums.EqOp;
import com.regnosys.rosetta.ast.enums.ExistenceOp;
import com.regnosys.rosetta.ast.enums.ExistsModifier;
import com.regnosys.rosetta.ast.enums.ListOp;
import com.regnosys.rosetta.ast.enums.LogOp;
import com.regnosys.rosetta.ast.enums.Necessity;
import com.regnosys.rosetta.ast.enums.SwitchGuardKind;
import com.regnosys.rosetta.ast.expressions.binary.RArithmeticExpr;
import com.regnosys.rosetta.ast.expressions.binary.RComparisonExpr;
import com.regnosys.rosetta.ast.expressions.binary.RContainsExpr;
import com.regnosys.rosetta.ast.expressions.binary.RDefaultExpr;
import com.regnosys.rosetta.ast.expressions.binary.RDisjointExpr;
import com.regnosys.rosetta.ast.expressions.binary.REqualityExpr;
import com.regnosys.rosetta.ast.expressions.binary.RJoinExpr;
import com.regnosys.rosetta.ast.expressions.binary.RLogicalExpr;
import com.regnosys.rosetta.ast.expressions.binary.RThenExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConstructorExpr;
import com.regnosys.rosetta.ast.expressions.literals.RBooleanLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RListLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RNumberLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RStringLiteral;
import com.regnosys.rosetta.ast.expressions.references.RDeepFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.REmptyLiteral;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCase;
import com.regnosys.rosetta.ast.expressions.unary.RCardinalityCheckExpr;
import com.regnosys.rosetta.ast.expressions.unary.RConversionExpr;
import com.regnosys.rosetta.ast.expressions.unary.RCountExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExistenceExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExtractExpr;
import com.regnosys.rosetta.ast.expressions.unary.RFilterExpr;
import com.regnosys.rosetta.ast.expressions.unary.RListOpExpr;
import com.regnosys.rosetta.ast.expressions.unary.RMaxExpr;
import com.regnosys.rosetta.ast.expressions.unary.RMinExpr;
import com.regnosys.rosetta.ast.expressions.unary.ROnlyExistsExpr;
import com.regnosys.rosetta.ast.expressions.unary.RReduceExpr;
import com.regnosys.rosetta.ast.expressions.unary.RSortExpr;
import com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr;
import com.regnosys.rosetta.ast.expressions.unary.RToStringExpr;
import com.regnosys.rosetta.ast.expressions.unary.RWithMetaExpr;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.ROperation;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.model.RModel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for expression visitor consolidation (Task 13).
 *
 * <p>These tests parse complete Rune DSL source and verify that expressions
 * within conditions, operations, and shortcuts produce correctly typed AST
 * nodes with the right enum values and structure.
 */
class ExpressionConsolidationTest extends BaseAstTest {

    // =========================================================================
    // Helper: wrap an expression in a condition for parsing
    // =========================================================================

    /**
     * Wraps the given expression source in a function with a condition,
     * parses it, and returns the condition's expression.
     */
    private RExpression parseExpr(String exprSource) {
        String source = "namespace test.example\n"
                + "func TestFunc:\n"
                + "    output:\n"
                + "        result number (1..1)\n"
                + "    condition Test:\n"
                + "        " + exprSource;
        RModel model = parseAndBuild(source);
        RFunction fn = (RFunction) model.rootElements().get(0);
        RCondition cond = fn.conditions().get(0);
        assertNotNull(cond.expression(),
                "Expression should not be null for: " + exprSource);
        return cond.expression();
    }

    // =========================================================================
    // Arithmetic operators (ArithOp consolidation)
    // =========================================================================

    @Nested
    class ArithmeticExpressions {

        @Test
        void plus() {
            RExpression expr = parseExpr("1 + 2");
            assertInstanceOf(RArithmeticExpr.class, expr);
            assertEquals(ArithOp.PLUS, ((RArithmeticExpr) expr).op());
        }

        @Test
        void minus() {
            RExpression expr = parseExpr("3 - 1");
            assertInstanceOf(RArithmeticExpr.class, expr);
            assertEquals(ArithOp.MINUS, ((RArithmeticExpr) expr).op());
        }

        @Test
        void multiply() {
            RExpression expr = parseExpr("2 * 3");
            assertInstanceOf(RArithmeticExpr.class, expr);
            assertEquals(ArithOp.MULTIPLY, ((RArithmeticExpr) expr).op());
        }

        @Test
        void divide() {
            RExpression expr = parseExpr("10 / 2");
            assertInstanceOf(RArithmeticExpr.class, expr);
            assertEquals(ArithOp.DIVIDE, ((RArithmeticExpr) expr).op());
        }

        @Test
        void leftAndRightPopulated() {
            RExpression expr = parseExpr("3 + 4");
            RArithmeticExpr arith = (RArithmeticExpr) expr;
            assertNotNull(arith.rawLeft());
            assertNotNull(arith.rawRight());
            assertInstanceOf(RIntLiteral.class, arith.rawLeft());
            assertInstanceOf(RIntLiteral.class, arith.rawRight());
        }
    }

    // =========================================================================
    // Comparison operators (CompOp consolidation)
    // =========================================================================

    @Nested
    class ComparisonExpressions {

        @Test
        void lessThan() {
            RExpression expr = parseExpr("1 < 2");
            assertInstanceOf(RComparisonExpr.class, expr);
            assertEquals(CompOp.LT, ((RComparisonExpr) expr).op());
        }

        @Test
        void greaterThan() {
            RExpression expr = parseExpr("2 > 1");
            assertInstanceOf(RComparisonExpr.class, expr);
            assertEquals(CompOp.GT, ((RComparisonExpr) expr).op());
        }

        @Test
        void lessThanOrEqual() {
            RExpression expr = parseExpr("1 <= 2");
            assertInstanceOf(RComparisonExpr.class, expr);
            assertEquals(CompOp.LTE, ((RComparisonExpr) expr).op());
        }

        @Test
        void greaterThanOrEqual() {
            RExpression expr = parseExpr("2 >= 1");
            assertInstanceOf(RComparisonExpr.class, expr);
            assertEquals(CompOp.GTE, ((RComparisonExpr) expr).op());
        }

        @Test
        void withAllModifier() {
            RExpression expr = parseExpr("result all > 0");
            assertInstanceOf(RComparisonExpr.class, expr);
            RComparisonExpr comp = (RComparisonExpr) expr;
            assertEquals(CompOp.GT, comp.op());
            assertEquals(Optional.of(CardMod.ALL), comp.mod());
        }

        @Test
        void withAnyModifier() {
            RExpression expr = parseExpr("result any < 10");
            assertInstanceOf(RComparisonExpr.class, expr);
            RComparisonExpr comp = (RComparisonExpr) expr;
            assertEquals(CompOp.LT, comp.op());
            assertEquals(Optional.of(CardMod.ANY), comp.mod());
        }

        @Test
        void noModifier() {
            RExpression expr = parseExpr("1 < 2");
            RComparisonExpr comp = (RComparisonExpr) expr;
            assertEquals(Optional.empty(), comp.mod());
        }
    }

    // =========================================================================
    // Equality operators (EqOp consolidation)
    // =========================================================================

    @Nested
    class EqualityExpressions {

        @Test
        void equal() {
            RExpression expr = parseExpr("1 = 1");
            assertInstanceOf(REqualityExpr.class, expr);
            assertEquals(EqOp.EQ, ((REqualityExpr) expr).op());
        }

        @Test
        void notEqual() {
            RExpression expr = parseExpr("1 <> 2");
            assertInstanceOf(REqualityExpr.class, expr);
            assertEquals(EqOp.NEQ, ((REqualityExpr) expr).op());
        }

        @Test
        void withAllModifier() {
            RExpression expr = parseExpr("result all = 1");
            assertInstanceOf(REqualityExpr.class, expr);
            REqualityExpr eq = (REqualityExpr) expr;
            assertEquals(EqOp.EQ, eq.op());
            assertEquals(Optional.of(CardMod.ALL), eq.mod());
        }

        @Test
        void withAnyModifier() {
            RExpression expr = parseExpr("result any <> 0");
            assertInstanceOf(REqualityExpr.class, expr);
            REqualityExpr eq = (REqualityExpr) expr;
            assertEquals(EqOp.NEQ, eq.op());
            assertEquals(Optional.of(CardMod.ANY), eq.mod());
        }
    }

    // =========================================================================
    // Logical operators (LogOp consolidation)
    // =========================================================================

    @Nested
    class LogicalExpressions {

        @Test
        void andOperator() {
            RExpression expr = parseExpr("True and False");
            assertInstanceOf(RLogicalExpr.class, expr);
            assertEquals(LogOp.AND, ((RLogicalExpr) expr).op());
        }

        @Test
        void orOperator() {
            RExpression expr = parseExpr("True or False");
            assertInstanceOf(RLogicalExpr.class, expr);
            assertEquals(LogOp.OR, ((RLogicalExpr) expr).op());
        }
    }

    // =========================================================================
    // Conversion kinds (ConversionKind consolidation)
    // =========================================================================

    @Nested
    class ConversionExpressions {

        @Test
        void toNumber() {
            RExpression expr = parseExpr("result to-number");
            assertInstanceOf(RConversionExpr.class, expr);
            assertEquals(ConversionKind.NUMBER, ((RConversionExpr) expr).kind());
            assertNotNull(((RConversionExpr) expr).argument());
        }

        @Test
        void toInt() {
            RExpression expr = parseExpr("result to-int");
            assertInstanceOf(RConversionExpr.class, expr);
            assertEquals(ConversionKind.INT, ((RConversionExpr) expr).kind());
        }

        @Test
        void toTime() {
            RExpression expr = parseExpr("result to-time");
            assertInstanceOf(RConversionExpr.class, expr);
            assertEquals(ConversionKind.TIME, ((RConversionExpr) expr).kind());
        }

        @Test
        void toEnum() {
            RExpression expr = parseExpr("result to-enum MyEnum");
            assertInstanceOf(RConversionExpr.class, expr);
            RConversionExpr conv = (RConversionExpr) expr;
            assertEquals(ConversionKind.ENUM, conv.kind());
            assertTrue(conv.targetEnumName().isPresent());
            assertEquals("MyEnum", conv.targetEnumName().get());
        }

        @Test
        void toEnumQualifiedName() {
            RExpression expr = parseExpr("result to-enum com.example.MyEnum");
            assertInstanceOf(RConversionExpr.class, expr);
            RConversionExpr conv = (RConversionExpr) expr;
            assertEquals(ConversionKind.ENUM, conv.kind());
            assertEquals("com.example.MyEnum", conv.targetEnumName().get());
        }

        @Test
        void toNumberHasNoTargetEnumName() {
            RExpression expr = parseExpr("result to-number");
            RConversionExpr conv = (RConversionExpr) expr;
            assertEquals(ConversionKind.NUMBER, conv.kind());
            assertTrue(conv.targetEnumName().isEmpty());
        }

        @Test
        void toDate() {
            RExpression expr = parseExpr("result to-date");
            assertInstanceOf(RConversionExpr.class, expr);
            assertEquals(ConversionKind.DATE, ((RConversionExpr) expr).kind());
        }

        @Test
        void toDateTime() {
            RExpression expr = parseExpr("result to-date-time");
            assertInstanceOf(RConversionExpr.class, expr);
            assertEquals(ConversionKind.DATE_TIME, ((RConversionExpr) expr).kind());
        }

        @Test
        void toZonedDateTime() {
            RExpression expr = parseExpr("result to-zoned-date-time");
            assertInstanceOf(RConversionExpr.class, expr);
            assertEquals(ConversionKind.ZONED_DATE_TIME, ((RConversionExpr) expr).kind());
        }
    }

    // =========================================================================
    // List operations (ListOp consolidation)
    // =========================================================================

    @Nested
    class ListOpExpressions {

        @Test
        void onlyElement() {
            RExpression expr = parseExpr("result only-element");
            assertInstanceOf(RListOpExpr.class, expr);
            assertEquals(ListOp.ONLY_ELEMENT, ((RListOpExpr) expr).op());
            assertNotNull(((RListOpExpr) expr).argument());
        }

        @Test
        void flatten() {
            RExpression expr = parseExpr("result flatten");
            assertInstanceOf(RListOpExpr.class, expr);
            assertEquals(ListOp.FLATTEN, ((RListOpExpr) expr).op());
        }

        @Test
        void distinct() {
            RExpression expr = parseExpr("result distinct");
            assertInstanceOf(RListOpExpr.class, expr);
            assertEquals(ListOp.DISTINCT, ((RListOpExpr) expr).op());
        }

        @Test
        void reverse() {
            RExpression expr = parseExpr("result reverse");
            assertInstanceOf(RListOpExpr.class, expr);
            assertEquals(ListOp.REVERSE, ((RListOpExpr) expr).op());
        }

        @Test
        void first() {
            RExpression expr = parseExpr("result first");
            assertInstanceOf(RListOpExpr.class, expr);
            assertEquals(ListOp.FIRST, ((RListOpExpr) expr).op());
        }

        @Test
        void last() {
            RExpression expr = parseExpr("result last");
            assertInstanceOf(RListOpExpr.class, expr);
            assertEquals(ListOp.LAST, ((RListOpExpr) expr).op());
        }

        @Test
        void sum() {
            RExpression expr = parseExpr("result sum");
            assertInstanceOf(RListOpExpr.class, expr);
            assertEquals(ListOp.SUM, ((RListOpExpr) expr).op());
        }
    }

    // =========================================================================
    // Existence expressions
    // =========================================================================

    @Nested
    class ExistenceExpressions {

        @Test
        void exists() {
            RExpression expr = parseExpr("result exists");
            assertInstanceOf(RExistenceExpr.class, expr);
            RExistenceExpr ee = (RExistenceExpr) expr;
            assertEquals(ExistenceOp.EXISTS, ee.op());
            assertNotNull(ee.argument());
            assertEquals(Optional.empty(), ee.modifier());
        }

        @Test
        void singleExists() {
            RExpression expr = parseExpr("result single exists");
            assertInstanceOf(RExistenceExpr.class, expr);
            RExistenceExpr ee = (RExistenceExpr) expr;
            assertEquals(ExistenceOp.EXISTS, ee.op());
            assertEquals(Optional.of(ExistsModifier.SINGLE), ee.modifier());
        }

        @Test
        void multipleExists() {
            RExpression expr = parseExpr("result multiple exists");
            assertInstanceOf(RExistenceExpr.class, expr);
            RExistenceExpr ee = (RExistenceExpr) expr;
            assertEquals(ExistenceOp.EXISTS, ee.op());
            assertEquals(Optional.of(ExistsModifier.MULTIPLE), ee.modifier());
        }

        @Test
        void absent() {
            RExpression expr = parseExpr("result is absent");
            assertInstanceOf(RExistenceExpr.class, expr);
            RExistenceExpr ee = (RExistenceExpr) expr;
            assertEquals(ExistenceOp.ABSENT, ee.op());
            assertNotNull(ee.argument());
        }
    }

    // =========================================================================
    // Cardinality check expressions
    // =========================================================================

    @Nested
    class CardinalityCheckExpressions {

        @Test
        void oneOf() {
            RExpression expr = parseExpr("result one-of");
            assertInstanceOf(RCardinalityCheckExpr.class, expr);
            RCardinalityCheckExpr cc = (RCardinalityCheckExpr) expr;
            assertEquals(CardCheckOp.ONE_OF, cc.op());
            assertNotNull(cc.argument());
        }

        @Test
        void optionalChoice() {
            RExpression expr = parseExpr("result optional choice fieldA, fieldB");
            assertInstanceOf(RCardinalityCheckExpr.class, expr);
            RCardinalityCheckExpr cc = (RCardinalityCheckExpr) expr;
            assertEquals(CardCheckOp.CHOICE, cc.op());
            assertEquals(Optional.of(Necessity.OPTIONAL), cc.necessity());
            assertEquals(2, cc.attributes().size());
            assertEquals("fieldA", cc.attributes().get(0));
            assertEquals("fieldB", cc.attributes().get(1));
        }

        @Test
        void requiredChoice() {
            RExpression expr = parseExpr("result required choice attrX");
            assertInstanceOf(RCardinalityCheckExpr.class, expr);
            RCardinalityCheckExpr cc = (RCardinalityCheckExpr) expr;
            assertEquals(CardCheckOp.CHOICE, cc.op());
            assertEquals(Optional.of(Necessity.REQUIRED), cc.necessity());
            assertEquals(1, cc.attributes().size());
            assertEquals("attrX", cc.attributes().get(0));
        }
    }

    // =========================================================================
    // ToString expression (separate from RConversionExpr)
    // =========================================================================

    @Test
    void toStringExpr() {
        RExpression expr = parseExpr("result to-string");
        assertInstanceOf(RToStringExpr.class, expr);
        assertNotNull(((RToStringExpr) expr).argument());
    }

    // =========================================================================
    // Count expression
    // =========================================================================

    @Test
    void countExpr() {
        RExpression expr = parseExpr("result count");
        assertInstanceOf(RCountExpr.class, expr);
        assertNotNull(((RCountExpr) expr).argument());
    }

    // =========================================================================
    // Contains and Disjoint
    // =========================================================================

    @Test
    void containsExpr() {
        RExpression expr = parseExpr("result contains 1");
        assertInstanceOf(RContainsExpr.class, expr);
        RContainsExpr ce = (RContainsExpr) expr;
        assertNotNull(ce.rawLeft());
        assertNotNull(ce.rawRight());
    }

    @Test
    void disjointExpr() {
        RExpression expr = parseExpr("result disjoint result");
        assertInstanceOf(RDisjointExpr.class, expr);
        RDisjointExpr de = (RDisjointExpr) expr;
        assertNotNull(de.rawLeft());
        assertNotNull(de.rawRight());
    }

    // =========================================================================
    // Default expression
    // =========================================================================

    @Test
    void defaultExpr() {
        RExpression expr = parseExpr("result default 0");
        assertInstanceOf(RDefaultExpr.class, expr);
        RDefaultExpr de = (RDefaultExpr) expr;
        assertNotNull(de.rawLeft());
        assertNotNull(de.rawRight());
    }

    // =========================================================================
    // Join expression
    // =========================================================================

    @Test
    void joinExprWithSeparator() {
        RExpression expr = parseExpr("result join \", \"");
        assertInstanceOf(RJoinExpr.class, expr);
        RJoinExpr je = (RJoinExpr) expr;
        assertNotNull(je.rawLeft());
        assertTrue(je.separator().isPresent());
    }

    @Test
    void joinExprWithoutSeparator() {
        RExpression expr = parseExpr("result join");
        assertInstanceOf(RJoinExpr.class, expr);
        RJoinExpr je = (RJoinExpr) expr;
        assertNotNull(je.rawLeft());
        assertFalse(je.separator().isPresent());
    }

    // =========================================================================
    // Then expression
    // =========================================================================

    @Test
    void thenExprWithInlineFunction() {
        RExpression expr = parseExpr("result then x [x + 1]");
        assertInstanceOf(RThenExpr.class, expr);
        RThenExpr te = (RThenExpr) expr;
        assertNotNull(te.argument());
        assertTrue(te.body().isPresent());
        RInlineFunction fn = te.body().get();
        assertFalse(fn.isImplicit());
        assertEquals(1, fn.paramNames().size());
        assertEquals("x", fn.paramNames().get(0));
        assertNotNull(fn.body());
    }

    @Test
    void thenExprWithImplicitInlineFunction() {
        // "result then item + 1" — the implicit form
        RExpression expr = parseExpr("result then item + 1");
        assertInstanceOf(RThenExpr.class, expr);
        RThenExpr te = (RThenExpr) expr;
        assertTrue(te.body().isPresent());
        assertTrue(te.body().get().isImplicit());
    }

    // =========================================================================
    // Feature call expressions
    // =========================================================================

    @Nested
    class FeatureCallExpressions {

        @Test
        void simpleFeatureCall() {
            // "result -> name" is parsed as EnumValueRefExpr (primary) since
            // qualifiedName ARROW validID is a primary alternative that the parser
            // prefers over the left-recursive FeatureCallExpr.
            // To get a true FeatureCallExpr, the left side must be a non-name expression.
            // Use "(result) -> name" to force it through parenthesized expression first.
            RExpression expr = parseExpr("(result) -> name");
            assertInstanceOf(RFeatureCall.class, expr);
            RFeatureCall fc = (RFeatureCall) expr;
            assertEquals("name", fc.featureName());
            assertNotNull(fc.receiver());
        }

        @Test
        void chainedFeatureCall() {
            // "result -> trade -> price": the first arrow is EnumValueRefExpr,
            // but adding a second arrow makes the whole thing a FeatureCallExpr
            // on top of the EnumValueRefExpr.
            RExpression expr = parseExpr("result -> trade -> price");
            assertInstanceOf(RFeatureCall.class, expr);
            RFeatureCall outerFc = (RFeatureCall) expr;
            assertEquals("price", outerFc.featureName());
            // The inner part "result -> trade" is an EnumValueRefExpr
            assertInstanceOf(REnumValueRef.class, outerFc.receiver());
            REnumValueRef innerRef = (REnumValueRef) outerFc.receiver();
            assertEquals("result", innerRef.enumName());
            assertEquals("trade", innerRef.valueName());
        }

        @Test
        void deepFeatureCall() {
            // Use parenthesized expr for receiver to ensure FeatureCallExpr
            RExpression expr = parseExpr("(result) ->> id");
            assertInstanceOf(RDeepFeatureCall.class, expr);
            RDeepFeatureCall dfc = (RDeepFeatureCall) expr;
            assertEquals("id", dfc.featureName());
            assertNotNull(dfc.receiver());
        }

        @Test
        void singleNameArrowIsParsedAsEnumValueRef() {
            // "result -> name" matches the primary EnumValueRefExpr alternative
            RExpression expr = parseExpr("result -> name");
            assertInstanceOf(REnumValueRef.class, expr);
            REnumValueRef ref = (REnumValueRef) expr;
            assertEquals("result", ref.enumName());
            assertEquals("name", ref.valueName());
        }
    }

    // =========================================================================
    // Function call expression
    // =========================================================================

    @Test
    void functionCallWithArgs() {
        RExpression expr = parseExpr("MyFunc(1, 2)");
        assertInstanceOf(RSymbolReference.class, expr);
        RSymbolReference sr = (RSymbolReference) expr;
        assertEquals("MyFunc", sr.name());
        assertEquals(2, sr.args().size());
        assertInstanceOf(RIntLiteral.class, sr.args().get(0));
        assertInstanceOf(RIntLiteral.class, sr.args().get(1));
    }

    @Test
    void functionCallNoArgs() {
        RExpression expr = parseExpr("MyFunc()");
        assertInstanceOf(RSymbolReference.class, expr);
        RSymbolReference sr = (RSymbolReference) expr;
        assertEquals("MyFunc", sr.name());
        assertTrue(sr.args().isEmpty());
    }

    // =========================================================================
    // Symbol reference and enum value reference
    // =========================================================================

    @Test
    void symbolReference() {
        RExpression expr = parseExpr("result");
        assertInstanceOf(RSymbolReference.class, expr);
        RSymbolReference sr = (RSymbolReference) expr;
        assertEquals("result", sr.name());
        assertTrue(sr.args().isEmpty());
    }

    @Test
    void enumValueRef() {
        RExpression expr = parseExpr("MyEnum -> Value1");
        // This could be parsed as feature call or enum value ref depending on context
        // In our grammar, qualifiedName ARROW validID is #EnumValueRefExpr
        // But a simple "result -> name" is #FeatureCallExpr (expression ARROW validID)
        // EnumValueRefExpr is a primary, FeatureCallExpr is postfix
        // "MyEnum -> Value1" could be parsed either way, but since MyEnum is just a
        // qualifiedName with no left expression, it matches EnumValueRefExpr first
        assertInstanceOf(REnumValueRef.class, expr);
        REnumValueRef ref = (REnumValueRef) expr;
        assertEquals("MyEnum", ref.enumName());
        assertEquals("Value1", ref.valueName());
    }

    // =========================================================================
    // Literal types
    // =========================================================================

    @Nested
    class LiteralExpressions {

        @Test
        void stringLiteral() {
            RExpression expr = parseExpr("\"hello world\"");
            assertInstanceOf(RStringLiteral.class, expr);
            assertEquals("hello world", ((RStringLiteral) expr).value());
        }

        @Test
        void intLiteral() {
            RExpression expr = parseExpr("42");
            assertInstanceOf(RIntLiteral.class, expr);
            assertEquals(BigInteger.valueOf(42), ((RIntLiteral) expr).value());
        }

        @Test
        void intLiteral_bigIntegerOverflow() {
            // Real-world Rune DSL files contain integer literals exceeding
            // long range (e.g., DRR uses 9999999999999999999999999 as a
            // sentinel). Verify the AST stores them as BigInteger without
            // loss of precision.
            RExpression expr = parseExpr("9999999999999999999999999");
            assertInstanceOf(RIntLiteral.class, expr);
            assertEquals(new BigInteger("9999999999999999999999999"),
                    ((RIntLiteral) expr).value());
        }

        @Test
        void numberLiteral() {
            RExpression expr = parseExpr("3.14");
            assertInstanceOf(RNumberLiteral.class, expr);
            assertEquals(new BigDecimal("3.14"), ((RNumberLiteral) expr).value());
        }

        @Test
        void booleanTrue() {
            RExpression expr = parseExpr("True");
            assertInstanceOf(RBooleanLiteral.class, expr);
            assertTrue(((RBooleanLiteral) expr).value());
        }

        @Test
        void booleanFalse() {
            RExpression expr = parseExpr("False");
            assertInstanceOf(RBooleanLiteral.class, expr);
            assertFalse(((RBooleanLiteral) expr).value());
        }
    }

    // =========================================================================
    // List literal
    // =========================================================================

    @Test
    void listLiteral() {
        RExpression expr = parseExpr("[1, 2, 3]");
        assertInstanceOf(RListLiteral.class, expr);
        RListLiteral ll = (RListLiteral) expr;
        assertEquals(3, ll.elements().size());
        assertInstanceOf(RIntLiteral.class, ll.elements().get(0));
        assertInstanceOf(RIntLiteral.class, ll.elements().get(1));
        assertInstanceOf(RIntLiteral.class, ll.elements().get(2));
    }

    @Test
    void emptyListLiteral() {
        RExpression expr = parseExpr("[]");
        assertInstanceOf(RListLiteral.class, expr);
        assertTrue(((RListLiteral) expr).elements().isEmpty());
    }

    // =========================================================================
    // Empty expression
    // =========================================================================

    @Test
    void emptyExpr() {
        RExpression expr = parseExpr("empty");
        assertInstanceOf(REmptyLiteral.class, expr);
    }

    // =========================================================================
    // Implicit variable (item)
    // =========================================================================

    @Test
    void implicitVarExpr() {
        RExpression expr = parseExpr("item");
        assertInstanceOf(RImplicitVariable.class, expr);
    }

    // =========================================================================
    // Super call
    // =========================================================================

    @Test
    void superCallExpr() {
        // 'super' is parsed via the SuperCallExpr alternative — verifies the visitor
        // produces RSuperCall end-to-end through parsing.
        RExpression expr = parseExpr("super");
        assertInstanceOf(com.regnosys.rosetta.ast.expressions.references.RSuperCall.class, expr);
    }

    // =========================================================================
    // Parenthesized expression (D9: transparent)
    // =========================================================================

    @Test
    void parenExprIsTransparent() {
        RExpression expr = parseExpr("(1 + 2)");
        // D9: no wrapper node, should return the inner expression directly
        assertInstanceOf(RArithmeticExpr.class, expr);
        assertEquals(ArithOp.PLUS, ((RArithmeticExpr) expr).op());
    }

    // =========================================================================
    // Conditional expression (if/then/else)
    // =========================================================================

    @Test
    void conditionalWithElse() {
        RExpression expr = parseExpr("if True then 1 else 2");
        assertInstanceOf(RConditionalExpr.class, expr);
        RConditionalExpr ce = (RConditionalExpr) expr;
        assertNotNull(ce.condition());
        assertNotNull(ce.thenBranch());
        assertTrue(ce.elseBranch().isPresent());
    }

    @Test
    void conditionalWithoutElse() {
        RExpression expr = parseExpr("if True then 1");
        assertInstanceOf(RConditionalExpr.class, expr);
        RConditionalExpr ce = (RConditionalExpr) expr;
        assertNotNull(ce.condition());
        assertNotNull(ce.thenBranch());
        assertFalse(ce.elseBranch().isPresent());
    }

    // =========================================================================
    // Switch expression
    // =========================================================================

    @Test
    void switchWithLiteralGuard() {
        RExpression expr = parseExpr("result switch 1 then \"one\", 2 then \"two\", default \"other\"");
        assertInstanceOf(RSwitchExpr.class, expr);
        RSwitchExpr se = (RSwitchExpr) expr;
        assertNotNull(se.argument());
        assertEquals(3, se.cases().size());

        // First case: literal guard 1
        RSwitchCase c0 = se.cases().get(0);
        assertFalse(c0.isDefault());
        assertTrue(c0.guard().isPresent());
        assertEquals(SwitchGuardKind.LITERAL, c0.guard().get().kind());

        // Second case: literal guard 2
        RSwitchCase c1 = se.cases().get(1);
        assertFalse(c1.isDefault());
        assertTrue(c1.guard().isPresent());

        // Third case: default
        RSwitchCase c2 = se.cases().get(2);
        assertTrue(c2.isDefault());
        assertFalse(c2.guard().isPresent());
    }

    @Test
    void switchWithNameGuard() {
        RExpression expr = parseExpr("result switch MyEnum then 1, default 0");
        assertInstanceOf(RSwitchExpr.class, expr);
        RSwitchExpr se = (RSwitchExpr) expr;
        assertEquals(2, se.cases().size());

        RSwitchCase c0 = se.cases().get(0);
        assertFalse(c0.isDefault());
        assertTrue(c0.guard().isPresent());
        assertEquals(SwitchGuardKind.NAME, c0.guard().get().kind());
        assertEquals("MyEnum", c0.guard().get().qualifiedName().orElse(null));
    }

    @Test
    void switchWithStringLiteralGuardStripsQuotes() {
        // Verifies that STRING literal guards have quotes stripped
        RExpression expr = parseExpr("result switch \"one\" then 1, \"two\" then 2, default 0");
        RSwitchExpr se = (RSwitchExpr) expr;
        assertEquals(3, se.cases().size());

        RSwitchCase c0 = se.cases().get(0);
        assertEquals(SwitchGuardKind.LITERAL, c0.guard().get().kind());
        // CRITICAL: literal value should NOT include surrounding quotes
        assertEquals("one", c0.guard().get().literalValue().orElse(null));

        RSwitchCase c1 = se.cases().get(1);
        assertEquals("two", c1.guard().get().literalValue().orElse(null));
    }

    @Test
    void switchWithIntLiteralGuardKeepsValue() {
        // Verifies that non-string literal guards retain their text value
        RExpression expr = parseExpr("result switch 42 then 1, default 0");
        RSwitchExpr se = (RSwitchExpr) expr;
        RSwitchCase c0 = se.cases().get(0);
        assertEquals(SwitchGuardKind.LITERAL, c0.guard().get().kind());
        assertEquals("42", c0.guard().get().literalValue().orElse(null));
    }

    // =========================================================================
    // Constructor expression
    // =========================================================================

    @Test
    void constructorExpr() {
        // Use a typeCall to construct — requires the source have a type to reference
        String source = "namespace test.example\n"
                + "type Party:\n"
                + "    name string (1..1)\n"
                + "\n"
                + "func TestFunc:\n"
                + "    output:\n"
                + "        result Party (1..1)\n"
                + "    set result:\n"
                + "        Party { name: \"ACME\" }";
        RModel model = parseAndBuild(source);
        RFunction fn = (RFunction) model.rootElements().get(1);
        ROperation op = fn.operations().get(0);
        assertNotNull(op.expression());
        RExpression expr = op.expression();
        assertInstanceOf(RConstructorExpr.class, expr);
        RConstructorExpr ce = (RConstructorExpr) expr;
        assertNotNull(ce.typeCall());
        assertEquals("Party", ce.typeCall().typeName());
        assertEquals(1, ce.pairs().size());
        assertEquals("name", ce.pairs().get(0).key());
        assertFalse(ce.isSpread());
    }

    @Test
    void constructorWithSpread() {
        String source = "namespace test.example\n"
                + "type Party:\n"
                + "    name string (1..1)\n"
                + "\n"
                + "func TestFunc:\n"
                + "    output:\n"
                + "        result Party (1..1)\n"
                + "    set result:\n"
                + "        Party { name: \"ACME\", ... }";
        RModel model = parseAndBuild(source);
        RFunction fn = (RFunction) model.rootElements().get(1);
        ROperation op = fn.operations().get(0);
        RExpression expr = op.expression();
        assertInstanceOf(RConstructorExpr.class, expr);
        RConstructorExpr ce = (RConstructorExpr) expr;
        assertTrue(ce.isSpread());
    }

    // =========================================================================
    // Sort, Min, Max, Filter, Extract, Reduce with inline functions
    // =========================================================================

    @Test
    void sortExpr() {
        RExpression expr = parseExpr("result sort");
        assertInstanceOf(RSortExpr.class, expr);
        RSortExpr se = (RSortExpr) expr;
        assertNotNull(se.argument());
        assertFalse(se.body().isPresent());
    }

    @Test
    void sortWithInlineFunction() {
        RExpression expr = parseExpr("result sort x [x]");
        assertInstanceOf(RSortExpr.class, expr);
        RSortExpr se = (RSortExpr) expr;
        assertTrue(se.body().isPresent());
        assertFalse(se.body().get().isImplicit());
    }

    @Test
    void filterExpr() {
        RExpression expr = parseExpr("result filter x [x > 0]");
        assertInstanceOf(RFilterExpr.class, expr);
        RFilterExpr fe = (RFilterExpr) expr;
        assertNotNull(fe.argument());
        assertNotNull(fe.body());
        assertFalse(fe.body().isImplicit());
    }

    @Test
    void filterWithImplicit() {
        RExpression expr = parseExpr("result filter item > 0");
        assertInstanceOf(RFilterExpr.class, expr);
        RFilterExpr fe = (RFilterExpr) expr;
        assertNotNull(fe.body());
        assertTrue(fe.body().isImplicit());
    }

    @Test
    void extractExpr() {
        RExpression expr = parseExpr("result extract x [x + 1]");
        assertInstanceOf(RExtractExpr.class, expr);
        RExtractExpr ee = (RExtractExpr) expr;
        assertNotNull(ee.argument());
        assertNotNull(ee.body());
        assertFalse(ee.body().isImplicit());
    }

    @Test
    void reduceExpr() {
        RExpression expr = parseExpr("result reduce a, b [a + b]");
        assertInstanceOf(RReduceExpr.class, expr);
        RReduceExpr re = (RReduceExpr) expr;
        assertNotNull(re.argument());
        assertNotNull(re.body());
        assertEquals(2, re.body().paramNames().size());
        assertEquals("a", re.body().paramNames().get(0));
        assertEquals("b", re.body().paramNames().get(1));
    }

    @Test
    void minExpr() {
        RExpression expr = parseExpr("result min");
        assertInstanceOf(RMinExpr.class, expr);
        assertNotNull(((RMinExpr) expr).argument());
    }

    @Test
    void maxExpr() {
        RExpression expr = parseExpr("result max");
        assertInstanceOf(RMaxExpr.class, expr);
        assertNotNull(((RMaxExpr) expr).argument());
    }

    // =========================================================================
    // WithMeta expression
    // =========================================================================

    @Test
    void withMetaExpr() {
        RExpression expr = parseExpr("result with-meta { scheme: \"http://example.com\" }");
        assertInstanceOf(RWithMetaExpr.class, expr);
        RWithMetaExpr wm = (RWithMetaExpr) expr;
        assertNotNull(wm.argument());
        assertEquals(1, wm.entries().size());
        assertEquals("scheme", wm.entries().get(0).metaName());
        assertInstanceOf(RStringLiteral.class, wm.entries().get(0).value());
    }

    // =========================================================================
    // WithoutLeft forms produce same node types
    // =========================================================================

    @Nested
    class WithoutLeftForms {

        /**
         * Parse a "without left" expression in a then-chain context:
         * "result then [expression_that_uses_without_left]".
         * The without-left form appears as the body of a then expression.
         */
        private RExpression parseWithoutLeft(String withoutLeftExpr) {
            // Use the then-chain pattern: result then [withoutLeftExpr]
            // Actually, the "without left" forms are used directly in then bodies.
            // In DSL: result then = 1 (produces EqualityWithoutLeftExpr)
            RExpression outer = parseExpr("result then " + withoutLeftExpr);
            assertInstanceOf(RThenExpr.class, outer);
            RThenExpr then = (RThenExpr) outer;
            assertTrue(then.body().isPresent());
            return then.body().get().body();
        }

        @Test
        void orWithoutLeft() {
            RExpression body = parseWithoutLeft("or True");
            assertInstanceOf(RLogicalExpr.class, body);
            assertEquals(LogOp.OR, ((RLogicalExpr) body).op());
            assertNull(((RLogicalExpr) body).rawLeft());
        }

        @Test
        void andWithoutLeft() {
            RExpression body = parseWithoutLeft("and True");
            assertInstanceOf(RLogicalExpr.class, body);
            assertEquals(LogOp.AND, ((RLogicalExpr) body).op());
            assertNull(((RLogicalExpr) body).rawLeft());
        }

        @Test
        void equalityWithoutLeft() {
            RExpression body = parseWithoutLeft("= 1");
            assertInstanceOf(REqualityExpr.class, body);
            assertEquals(EqOp.EQ, ((REqualityExpr) body).op());
            assertNull(((REqualityExpr) body).rawLeft());
        }

        @Test
        void comparisonWithoutLeft() {
            RExpression body = parseWithoutLeft("> 0");
            assertInstanceOf(RComparisonExpr.class, body);
            assertEquals(CompOp.GT, ((RComparisonExpr) body).op());
            assertNull(((RComparisonExpr) body).rawLeft());
        }

        @Test
        void existsWithoutLeft() {
            RExpression body = parseWithoutLeft("exists");
            assertInstanceOf(RExistenceExpr.class, body);
            assertEquals(ExistenceOp.EXISTS, ((RExistenceExpr) body).op());
            assertNull(((RExistenceExpr) body).argument());
        }

        @Test
        void absentWithoutLeft() {
            RExpression body = parseWithoutLeft("is absent");
            assertInstanceOf(RExistenceExpr.class, body);
            assertEquals(ExistenceOp.ABSENT, ((RExistenceExpr) body).op());
            assertNull(((RExistenceExpr) body).argument());
        }

        @Test
        void countWithoutLeft() {
            RExpression body = parseWithoutLeft("count");
            assertInstanceOf(RCountExpr.class, body);
            // Engine PR #2 Bucket A — synthesised implicit-input operand.
            assertInstanceOf(RImplicitVariable.class, ((RCountExpr) body).argument());
        }

        @Test
        void onlyElementWithoutLeft() {
            RExpression body = parseWithoutLeft("only-element");
            assertInstanceOf(RListOpExpr.class, body);
            assertEquals(ListOp.ONLY_ELEMENT, ((RListOpExpr) body).op());
            assertInstanceOf(RImplicitVariable.class, ((RListOpExpr) body).argument());
        }

        @Test
        void flattenWithoutLeft() {
            RExpression body = parseWithoutLeft("flatten");
            assertInstanceOf(RListOpExpr.class, body);
            assertEquals(ListOp.FLATTEN, ((RListOpExpr) body).op());
        }

        @Test
        void distinctWithoutLeft() {
            RExpression body = parseWithoutLeft("distinct");
            assertInstanceOf(RListOpExpr.class, body);
            assertEquals(ListOp.DISTINCT, ((RListOpExpr) body).op());
        }

        @Test
        void toNumberWithoutLeft() {
            // After elided-operand implicit-input synthesis, the without-left
            // conversion's argument is the materialised RImplicitVariable
            // (mirrors upstream rune-dsl).
            RExpression body = parseWithoutLeft("to-number");
            assertInstanceOf(RConversionExpr.class, body);
            assertEquals(ConversionKind.NUMBER, ((RConversionExpr) body).kind());
            assertInstanceOf(RImplicitVariable.class, ((RConversionExpr) body).argument());
        }

        @Test
        void toStringWithoutLeft() {
            // After elided-operand implicit-input synthesis (engine PR #1 + R3 F1
            // extension for RToStringExpr), the without-left to-string's argument
            // is the materialised RImplicitVariable.
            RExpression body = parseWithoutLeft("to-string");
            assertInstanceOf(RToStringExpr.class, body);
            assertInstanceOf(RImplicitVariable.class, ((RToStringExpr) body).argument());
        }

        // -- Additional WithoutLeft coverage (26 forms) -----------------------

        @Test
        void multiplicativeWithoutLeft() {
            // PR #437 (facet omittedParamBinding, finding #38): the left-less
            // multiplicative materialises the SYNTHETIC implicit item as its left
            // operand — upstream's derived-state law (needsGeneratedInput == null
            // left + the default implicit variable); `[* 2]` is `item * 2`, never
            // the unary-sign fold. The ADDITIVE form below stays left-null: it is
            // genuinely prefix unary sign (upstream has no left-less additive).
            RExpression body = parseWithoutLeft("* 2");
            assertInstanceOf(RArithmeticExpr.class, body);
            assertEquals(ArithOp.MULTIPLY, ((RArithmeticExpr) body).op());
            assertInstanceOf(RImplicitVariable.class, ((RArithmeticExpr) body).rawLeft());
            assertTrue(((RImplicitVariable) ((RArithmeticExpr) body).rawLeft()).isSynthetic());
        }

        @Test
        void additiveWithoutLeft() {
            RExpression body = parseWithoutLeft("+ 3");
            assertInstanceOf(RArithmeticExpr.class, body);
            assertEquals(ArithOp.PLUS, ((RArithmeticExpr) body).op());
            assertNull(((RArithmeticExpr) body).rawLeft());
        }

        @Test
        void containsWithoutLeft() {
            RExpression body = parseWithoutLeft("contains 1");
            assertInstanceOf(RContainsExpr.class, body);
            assertNull(((RContainsExpr) body).rawLeft());
        }

        @Test
        void disjointWithoutLeft() {
            RExpression body = parseWithoutLeft("disjoint other");
            assertInstanceOf(RDisjointExpr.class, body);
            assertNull(((RDisjointExpr) body).rawLeft());
        }

        @Test
        void defaultWithoutLeft() {
            RExpression body = parseWithoutLeft("default 0");
            assertInstanceOf(RDefaultExpr.class, body);
            assertNull(((RDefaultExpr) body).rawLeft());
        }

        @Test
        void joinWithoutLeft() {
            RExpression body = parseWithoutLeft("join \", \"");
            assertInstanceOf(RJoinExpr.class, body);
            assertNull(((RJoinExpr) body).rawLeft());
        }

        @Test
        void reverseWithoutLeft() {
            RExpression body = parseWithoutLeft("reverse");
            assertInstanceOf(RListOpExpr.class, body);
            assertEquals(ListOp.REVERSE, ((RListOpExpr) body).op());
            assertInstanceOf(RImplicitVariable.class, ((RListOpExpr) body).argument());
        }

        @Test
        void firstWithoutLeft() {
            RExpression body = parseWithoutLeft("first");
            assertInstanceOf(RListOpExpr.class, body);
            assertEquals(ListOp.FIRST, ((RListOpExpr) body).op());
            assertInstanceOf(RImplicitVariable.class, ((RListOpExpr) body).argument());
        }

        @Test
        void lastWithoutLeft() {
            RExpression body = parseWithoutLeft("last");
            assertInstanceOf(RListOpExpr.class, body);
            assertEquals(ListOp.LAST, ((RListOpExpr) body).op());
            assertInstanceOf(RImplicitVariable.class, ((RListOpExpr) body).argument());
        }

        @Test
        void sumWithoutLeft() {
            RExpression body = parseWithoutLeft("sum");
            assertInstanceOf(RListOpExpr.class, body);
            assertEquals(ListOp.SUM, ((RListOpExpr) body).op());
            assertInstanceOf(RImplicitVariable.class, ((RListOpExpr) body).argument());
        }

        @Test
        void oneOfWithoutLeft() {
            RExpression body = parseWithoutLeft("one-of");
            assertInstanceOf(RCardinalityCheckExpr.class, body);
            assertEquals(CardCheckOp.ONE_OF, ((RCardinalityCheckExpr) body).op());
            assertNull(((RCardinalityCheckExpr) body).argument());
        }

        @Test
        void choiceWithoutLeft() {
            RExpression body = parseWithoutLeft("required choice fieldA, fieldB");
            assertInstanceOf(RCardinalityCheckExpr.class, body);
            RCardinalityCheckExpr cc = (RCardinalityCheckExpr) body;
            assertEquals(CardCheckOp.CHOICE, cc.op());
            assertEquals(Necessity.REQUIRED, cc.necessity().orElse(null));
            assertNull(cc.argument());
        }

        @Test
        void toIntWithoutLeft() {
            RExpression body = parseWithoutLeft("to-int");
            assertInstanceOf(RConversionExpr.class, body);
            assertEquals(ConversionKind.INT, ((RConversionExpr) body).kind());
            assertInstanceOf(RImplicitVariable.class, ((RConversionExpr) body).argument());
        }

        @Test
        void toTimeWithoutLeft() {
            RExpression body = parseWithoutLeft("to-time");
            assertInstanceOf(RConversionExpr.class, body);
            assertEquals(ConversionKind.TIME, ((RConversionExpr) body).kind());
        }

        @Test
        void toEnumWithoutLeft() {
            RExpression body = parseWithoutLeft("to-enum MyEnum");
            assertInstanceOf(RConversionExpr.class, body);
            RConversionExpr conv = (RConversionExpr) body;
            assertEquals(ConversionKind.ENUM, conv.kind());
            assertEquals("MyEnum", conv.targetEnumName().orElse(null));
            assertInstanceOf(RImplicitVariable.class, conv.argument());
        }

        @Test
        void toDateWithoutLeft() {
            RExpression body = parseWithoutLeft("to-date");
            assertInstanceOf(RConversionExpr.class, body);
            assertEquals(ConversionKind.DATE, ((RConversionExpr) body).kind());
        }

        @Test
        void toDateTimeWithoutLeft() {
            RExpression body = parseWithoutLeft("to-date-time");
            assertInstanceOf(RConversionExpr.class, body);
            assertEquals(ConversionKind.DATE_TIME, ((RConversionExpr) body).kind());
        }

        @Test
        void toZonedDateTimeWithoutLeft() {
            RExpression body = parseWithoutLeft("to-zoned-date-time");
            assertInstanceOf(RConversionExpr.class, body);
            assertEquals(ConversionKind.ZONED_DATE_TIME, ((RConversionExpr) body).kind());
        }

        @Test
        void switchWithoutLeft() {
            RExpression body = parseWithoutLeft("switch 1 then \"one\"");
            assertInstanceOf(RSwitchExpr.class, body);
            RSwitchExpr sw = (RSwitchExpr) body;
            assertNull(sw.argument());
            assertEquals(1, sw.cases().size());
        }

        @Test
        void withMetaWithoutLeft() {
            RExpression body = parseWithoutLeft("with-meta { scheme: \"http://example.com\" }");
            assertInstanceOf(RWithMetaExpr.class, body);
            RWithMetaExpr wm = (RWithMetaExpr) body;
            assertNull(wm.argument());
            assertEquals(1, wm.entries().size());
        }

        @Test
        void sortWithoutLeft() {
            RExpression body = parseWithoutLeft("sort");
            assertInstanceOf(RSortExpr.class, body);
            // Engine PR #2 Bucket A — synthesised implicit-input operand.
            assertInstanceOf(RImplicitVariable.class, ((RSortExpr) body).argument());
        }

        @Test
        void minWithoutLeft() {
            RExpression body = parseWithoutLeft("min");
            assertInstanceOf(RMinExpr.class, body);
            // Engine PR #2 Bucket A — synthesised implicit-input operand.
            assertInstanceOf(RImplicitVariable.class, ((RMinExpr) body).argument());
        }

        @Test
        void maxWithoutLeft() {
            RExpression body = parseWithoutLeft("max");
            assertInstanceOf(RMaxExpr.class, body);
            // Engine PR #2 Bucket A — synthesised implicit-input operand.
            assertInstanceOf(RImplicitVariable.class, ((RMaxExpr) body).argument());
        }

        @Test
        void reduceWithoutLeft() {
            RExpression body = parseWithoutLeft("reduce a, b [a + b]");
            assertInstanceOf(RReduceExpr.class, body);
            RReduceExpr r = (RReduceExpr) body;
            // Engine PR #2 Bucket A — synthesised implicit-input operand.
            assertInstanceOf(RImplicitVariable.class, r.argument());
            assertNotNull(r.body());
        }

        @Test
        void filterWithoutLeft() {
            // After elided-operand implicit-input synthesis (PR engine-1), the
            // without-left filter's argument is the materialised RImplicitVariable.
            RExpression body = parseWithoutLeft("filter item > 0");
            assertInstanceOf(RFilterExpr.class, body);
            RFilterExpr f = (RFilterExpr) body;
            assertInstanceOf(RImplicitVariable.class, f.argument());
            assertNotNull(f.body());
        }

        @Test
        void extractWithoutLeft() {
            // After elided-operand implicit-input synthesis (PR engine-1), the
            // without-left extract's argument is the materialised RImplicitVariable.
            RExpression body = parseWithoutLeft("extract item + 1");
            assertInstanceOf(RExtractExpr.class, body);
            RExtractExpr e = (RExtractExpr) body;
            assertInstanceOf(RImplicitVariable.class, e.argument());
            assertNotNull(e.body());
        }
    }

    // =========================================================================
    // Expressions wired into conditions, operations, shortcuts
    // =========================================================================

    @Nested
    class WiringTests {

        @Test
        void conditionGetsExpression() {
            String source = "namespace test.example\n"
                    + "func TestFunc:\n"
                    + "    output:\n"
                    + "        result number (1..1)\n"
                    + "    condition MustBePositive:\n"
                    + "        result > 0";
            RModel model = parseAndBuild(source);
            RFunction fn = (RFunction) model.rootElements().get(0);
            RCondition cond = fn.conditions().get(0);
            assertNotNull(cond.expression());
            assertInstanceOf(RComparisonExpr.class, cond.expression());
        }

        @Test
        void operationGetsExpression() {
            String source = "namespace test.example\n"
                    + "func TestFunc:\n"
                    + "    output:\n"
                    + "        result number (1..1)\n"
                    + "    set result:\n"
                    + "        42";
            RModel model = parseAndBuild(source);
            RFunction fn = (RFunction) model.rootElements().get(0);
            ROperation op = fn.operations().get(0);
            assertNotNull(op.expression());
            assertInstanceOf(RIntLiteral.class, op.expression());
            assertEquals(BigInteger.valueOf(42), ((RIntLiteral) op.expression()).value());
        }

        @Test
        void shortcutGetsExpression() {
            String source = "namespace test.example\n"
                    + "func TestFunc:\n"
                    + "    inputs:\n"
                    + "        trade Trade (1..1)\n"
                    + "    output:\n"
                    + "        result number (1..1)\n"
                    + "    alias tradeDate:\n"
                    + "        trade -> tradeDate\n"
                    + "    set result:\n"
                    + "        1";
            RModel model = parseAndBuild(source);
            RFunction fn = (RFunction) model.rootElements().get(0);
            assertEquals(1, fn.shortcuts().size());
            RShortcut sc = fn.shortcuts().get(0);
            assertNotNull(sc.expression());
            // "trade -> tradeDate" is parsed as EnumValueRefExpr (primary)
            assertInstanceOf(REnumValueRef.class, sc.expression());
        }
    }

    // =========================================================================
    // OnlyExists expression
    // =========================================================================

    @Test
    void onlyExistsWithSingleElement() {
        RExpression expr = parseExpr("result -> fieldA only exists");
        assertInstanceOf(ROnlyExistsExpr.class, expr);
        ROnlyExistsExpr oe = (ROnlyExistsExpr) expr;
        assertEquals(1, oe.elements().size());
    }

    /**
     * Verifies that a parenthesised list of elements parses with each element
     * appearing in the {@link ROnlyExistsExpr#elements()} list. Grammar form:
     * {@code (a, b) only exists}.
     */
    @Test
    void onlyExistsWithMultipleElements() {
        String source = "namespace test.example\n"
                + "type Trade:\n"
                + "    price number (1..1)\n"
                + "    quantity number (1..1)\n"
                + "\n"
                + "func TestFunc:\n"
                + "    inputs:\n"
                + "        trade Trade (1..1)\n"
                + "    output:\n"
                + "        result boolean (1..1)\n"
                + "    set result:\n"
                + "        (trade -> price, trade -> quantity) only exists";
        RModel model = parseAndBuild(source);
        RFunction fn = (RFunction) model.rootElements().get(1);
        RExpression expr = fn.operations().get(0).expression();
        assertInstanceOf(ROnlyExistsExpr.class, expr);
        ROnlyExistsExpr oe = (ROnlyExistsExpr) expr;
        assertEquals(2, oe.elements().size(),
                "Parenthesised multi-element only exists should yield both elements");
        assertNotNull(oe.elements().get(0));
        assertNotNull(oe.elements().get(1));
    }

    // =========================================================================
    // Source range checks
    // =========================================================================

    @Test
    void expressionNodesHaveSourceRange() {
        RExpression expr = parseExpr("1 + 2");
        assertSourceRangeSet((RNode) expr);
        // Also check children
        RArithmeticExpr arith = (RArithmeticExpr) expr;
        assertSourceRangeSet((RNode) arith.rawLeft());
        assertSourceRangeSet((RNode) arith.rawRight());
    }

    /**
     * Verifies that the source range of a left-associative binary chain
     * ({@code 1 + 2 + 3}) spans from the leftmost to the rightmost operand.
     * The expression parses as {@code (1 + 2) + 3}; the outer arithmetic
     * node's range must therefore cover all three integer tokens.
     */
    @Test
    void nestedBinaryExpressionHasSpanningSourceRange() {
        RExpression expr = parseExpr("1 + 2 + 3");
        assertInstanceOf(RArithmeticExpr.class, expr);
        RArithmeticExpr outer = (RArithmeticExpr) expr;
        assertSourceRangeSet((RNode) outer);

        // Verify left-associative parse: outer = (1 + 2) + 3
        assertInstanceOf(RArithmeticExpr.class, outer.rawLeft());
        assertInstanceOf(RIntLiteral.class, outer.rawRight());
        assertEquals(BigInteger.valueOf(3), ((RIntLiteral) outer.rawRight()).value());

        RArithmeticExpr inner = (RArithmeticExpr) outer.rawLeft();
        assertInstanceOf(RIntLiteral.class, inner.rawLeft());
        assertInstanceOf(RIntLiteral.class, inner.rawRight());
        assertEquals(BigInteger.valueOf(1), ((RIntLiteral) inner.rawLeft()).value());
        assertEquals(BigInteger.valueOf(2), ((RIntLiteral) inner.rawRight()).value());

        // The outer range must start at or before the leftmost literal '1'
        // and end at or after the rightmost literal '3'.
        SourceRange outerRange = outer.sourceRange();
        SourceRange leftmost = ((RNode) inner.rawLeft()).sourceRange();
        SourceRange rightmost = ((RNode) outer.rawRight()).sourceRange();
        assertTrue(
                outerRange.startLine() < leftmost.startLine()
                        || (outerRange.startLine() == leftmost.startLine()
                                && outerRange.startCol() <= leftmost.startCol()),
                "Outer range start should be at/before leftmost operand start");
        assertTrue(
                outerRange.endLine() > rightmost.endLine()
                        || (outerRange.endLine() == rightmost.endLine()
                                && outerRange.endCol() >= rightmost.endCol()),
                "Outer range end should be at/after rightmost operand end");
    }

    /**
     * Verifies that source ranges nest correctly in a multi-level mixed
     * expression ({@code (a + b) * (c - d)}), i.e. each sub-expression's
     * range is contained within its parent's range.
     */
    @Test
    void complexNestedExpressionHasNestedSourceRanges() {
        String source = "namespace test.example\n"
                + "func TestFunc:\n"
                + "    inputs:\n"
                + "        a number (1..1)\n"
                + "        b number (1..1)\n"
                + "        c number (1..1)\n"
                + "        d number (1..1)\n"
                + "    output:\n"
                + "        result number (1..1)\n"
                + "    set result:\n"
                + "        (a + b) * (c - d)";
        RModel model = parseAndBuild(source);
        RFunction fn = (RFunction) model.rootElements().get(0);
        RExpression expr = fn.operations().get(0).expression();

        // Top level: RArithmeticExpr with MULTIPLY
        assertInstanceOf(RArithmeticExpr.class, expr);
        RArithmeticExpr top = (RArithmeticExpr) expr;
        assertEquals(ArithOp.MULTIPLY, top.op());

        // Per design D9, parens are transparent, so left/right are
        // themselves RArithmeticExpr nodes.
        assertInstanceOf(RArithmeticExpr.class, top.rawLeft());
        assertInstanceOf(RArithmeticExpr.class, top.rawRight());
        RArithmeticExpr left = (RArithmeticExpr) top.rawLeft();
        RArithmeticExpr right = (RArithmeticExpr) top.rawRight();
        assertEquals(ArithOp.PLUS, left.op());
        assertEquals(ArithOp.MINUS, right.op());

        SourceRange topRange = top.sourceRange();
        SourceRange leftRange = left.sourceRange();
        SourceRange rightRange = right.sourceRange();

        assertSourceRangeSet(top);
        assertSourceRangeSet(left);
        assertSourceRangeSet(right);

        // left sub-range must be contained in top range (same start line)
        assertEquals(topRange.startLine(), leftRange.startLine());
        assertTrue(leftRange.startCol() >= topRange.startCol(),
                "Left sub-range start should be at/after top range start");
        assertTrue(rightRange.endCol() <= topRange.endCol(),
                "Right sub-range end should be at/before top range end");
        assertTrue(leftRange.endCol() <= rightRange.startCol(),
                "Left sub-range must end before right sub-range begins");
    }
}
