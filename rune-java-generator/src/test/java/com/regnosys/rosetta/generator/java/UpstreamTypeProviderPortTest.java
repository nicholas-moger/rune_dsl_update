package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.expressions.binary.RLogicalExpr;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.generator.java.UpstreamExpressionPortSupport.ParsedExpression;
import com.regnosys.rosetta.types.RBasicType;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.RNumberType;
import com.regnosys.rosetta.types.RStringType;
import com.regnosys.rosetta.types.RType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Leg-C port (PR #423, slice 2) of upstream
 * {@code rune-integration-tests/.../types/RosettaTypeProviderXtendTest.xtend}
 * — 36 methods, 1:1 by name (33 active + the 3 upstream-EMPTY carried
 * {@code @Disabled}; upstream's own header: "Should be moved over to Java"). The
 * type-assert stack: {@link UpstreamExpressionPortSupport} parses each
 * expression in context (the ALIAS channel by default; the SET-BODY channel
 * for {@code input -> feature} navs — historical: the #279 typing arm gated
 * out alias bodies until the #429 lift, leg-C finding #9 CLOSED at #438);
 * {@code ws.getInferredType}/{@code ws.getCardinality} stand in for
 * upstream's {@code RMetaAnnotatedType}/{@code isMulti}.
 *
 * <p><b>The red-run verdict map (every divergence measured, none silent):</b>
 * <ul>
 *   <li><b>finding #6 — constraint-fold gaps:</b> literals carry
 *       digits/fractionalDigits/lengths but NOT min/max; operations JOIN
 *       operand constraints instead of upstream's interval algebra (3 + 4
 *       keeps digits 1, no [7,7]; string concat joins lengths to (2,2)
 *       instead of summing to (4,4)); division joins to int instead of
 *       upstream's unconstrained number. Methods assert the MEASURED fork
 *       types with upstream's expectation documented per line.</li>
 *   <li><b>finding #7 — meta flow:</b> a declared {@code [metadata scheme]}
 *       on an input does NOT reach the inferred type's
 *       {@code metaAttributes()} (upstream: {@code int with meta [scheme]}).</li>
 *   <li><b>finding #8 — cardinality laws:</b> the empty list literal is
 *       MULTI (upstream: single); a conditional with list branches is SINGLE
 *       (upstream: multi); an {@code input -> feature} nav's cardinality
 *       ignores both the input's and the feature's multiplicity (always
 *       single).</li>
 *   <li><b>finding #4 (documented at #279 — HEALED at PR #443):</b> resolved
 *       input-navs kept a stale {@code ENUM_NOT_FOUND} linking diagnostic;
 *       the #443 class-(a) wave clears it on bind and the inline pin here
 *       un-pinned to upstream's clean-linking assert
 *       ({@code testProjectionTypeInference}).</li>
 *   <li><b>the recorded expression-type-validation item (false-NEGATIVE
 *       half):</b> equality/comparison-cardinality/conditional-branch/
 *       function-call/list-literal/only-exists checks are MISSING — each
 *       carrier method is a zero-diagnostic Freezing pin; logical/arithmetic
 *       checks EXIST with fork-native messages (asserted verbatim).</li>
 * </ul>
 * The three upstream-EMPTY methods (all-commented bodies) are carried
 * {@code @Disabled}. Ledger:
 * the development audit "2026-07-17-leg-c-integration-port-ledger".
 */
class UpstreamTypeProviderPortTest {

    // ------------------------------------------------------- type factories

    private static RMetaAnnotatedType noMeta(RType t) {
        return RMetaAnnotatedType.withNoMeta(t);
    }

    private static RMetaAnnotatedType intNoMeta(Integer digits) {
        return noMeta(new RNumberType(
                digits == null ? OptionalInt.empty() : OptionalInt.of(digits),
                OptionalInt.of(0), Optional.empty(), Optional.empty()));
    }

    private static RMetaAnnotatedType numberNoMeta(int digits, int fractionalDigits) {
        return noMeta(new RNumberType(OptionalInt.of(digits), OptionalInt.of(fractionalDigits),
                Optional.empty(), Optional.empty()));
    }

    private static RMetaAnnotatedType stringNoMeta(int minLength, int maxLength) {
        return noMeta(new RStringType(
                OptionalInt.of(minLength), OptionalInt.of(maxLength), Optional.empty()));
    }

    private static final RMetaAnnotatedType BOOLEAN_NO_META = noMeta(RBasicType.BOOLEAN);
    private static final RMetaAnnotatedType NOTHING_NO_META = noMeta(RBasicType.NOTHING);
    private static final RMetaAnnotatedType UNCONSTRAINED_INT_NO_META = noMeta(RNumberType.intType());
    private static final RMetaAnnotatedType UNCONSTRAINED_NUMBER_NO_META = noMeta(RNumberType.unconstrained());

    // ----------------------------------------------------------- assert core

    private static void assertIsValidWithType(ParsedExpression p, RMetaAnnotatedType expectedType,
            boolean expectedIsMulti) {
        p.assertNoIssues();
        assertType(p, expectedType, expectedIsMulti);
    }

    private static void assertType(ParsedExpression p, RMetaAnnotatedType expectedType,
            boolean expectedIsMulti) {
        assertEquals(expectedType, p.type(), "inferred type");
        assertEquals(expectedIsMulti, p.isMulti(), "cardinality (isMulti)");
    }

    private static RModel contextModel(ParsedExpression p) {
        for (RModel m : p.ws().files()) {
            if (m.sourceRange() != null && m.sourceRange().file() != null
                    && m.sourceRange().file().startsWith("expression-port-context-0")) {
                return m;
            }
        }
        throw new AssertionError("context model not found");
    }

    private static RFunction func(RModel model, String name) {
        for (var el : model.rootElements()) {
            if (el instanceof RFunction f && name.equals(f.name())) {
                return f;
            }
        }
        throw new AssertionError("no func " + name);
    }

    // ------------------------------------------------------------- inference

    /**
     * Upstream {@code testLiteralTypeInference}. Fork-native on the number
     * literals: upstream folds literal VALUES into min/max
     * ({@code 3.14 → number(3, 2, 3.14, 3.14)}, {@code 1 → int(1, 1, 1)});
     * the fork tracks digits/fractionalDigits/lengths only (finding #6).
     * The {@code empty} row is also a representation mapping: upstream's
     * {@code NOTHING_WITH_ANY_META} (nothing matches ANY meta) has no fork
     * counterpart — the fork wraps NOTHING with the ordinary empty meta list.
     */
    @Test
    void testLiteralTypeInference() {
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("False"), BOOLEAN_NO_META, false);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("\"Some string\""), stringNoMeta(11, 11), false);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("3.14"), numberNoMeta(3, 2), false);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("1"), intNoMeta(1), false);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("empty"), NOTHING_NO_META, false);
    }

    /**
     * Upstream {@code testVariableTypeInference}. Two fork deltas, both
     * measured: the input's {@code [metadata scheme]} does NOT flow into
     * {@code metaAttributes()} (finding #7 — upstream expects
     * {@code int with meta [scheme]}; pinned here), and the alias
     * {@code if True then 42 else -1/12} types as {@code int(digits: 2)}
     * (finding #6: division joins to int; upstream: unconstrained number).
     */
    @Test
    void testVariableTypeInference() {
        String context = """
                func TestVar:
                	output: result number (1..4)
                	alias c: if True then 42 else -1/12
                	add result:
                		c

                func TestImplicitVar:
                	output: result int (3..3)
                	add result:
                		[1, 2, 3] extract item + 1
                """;
        ParsedExpression a = UpstreamExpressionPortSupport.parse(
                List.of("a int (2..4) [metadata scheme]"), "a");
        a.assertNoIssues();
        // finding #7 pin: upstream = UNCONSTRAINED_INT.withMeta(#[SCHEME]).
        assertType(a, UNCONSTRAINED_INT_NO_META, true);
        assertTrue(a.type().metaAttributes().isEmpty(),
                "pin drift: the input's [metadata scheme] reached metaAttributes — "
                + "HEALED? un-pin to the upstream withMeta([scheme]) expectation");

        assertIsValidWithType(
                UpstreamExpressionPortSupport.parse(List.of("b boolean (1..1)"), "b"),
                BOOLEAN_NO_META, false);

        ParsedExpression inContext = UpstreamExpressionPortSupport.parse(List.of(context), List.of(), "True");
        RModel ctxModel = contextModel(inContext);
        RFunction testVar = func(ctxModel, "TestVar");
        RExpression aliasCall = testVar.operations().get(0).expression();
        // Upstream: UNCONSTRAINED_NUMBER (division yields number). Measured fork:
        // int(digits: 2) — finding #6's division arm.
        assertEquals(intNoMeta(2), inContext.ws().getInferredType(aliasCall),
                "the alias-typed operation expression (finding #6 division arm — "
                + "upstream expects unconstrained number)");

        RFunction testImplicitVar = func(ctxModel, "TestImplicitVar");
        var mapLefts = AstWalker.findAll(testImplicitVar.operations().get(0).expression(),
                com.regnosys.rosetta.ast.expressions.binary.RArithmeticExpr.class);
        assertFalse(mapLefts.isEmpty(), "the extract-lambda arithmetic must parse");
        RExpression itemRef = mapLefts.get(0).left().orElseThrow();
        // Upstream: intWithNoMeta(1, "1", "3") — the item of [1, 2, 3]. Fork:
        // digits fold, min/max absent (finding #6).
        assertEquals(intNoMeta(1), inContext.ws().getInferredType(itemRef),
                "the extract item type (upstream folds min 1 / max 3)");
    }

    /** Upstream {@code testLogicalOperationTypeInference}. */
    @Test
    void testLogicalOperationTypeInference() {
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("True or False"), BOOLEAN_NO_META, false);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("True and False"), BOOLEAN_NO_META, false);
    }

    /**
     * Upstream {@code testLogicalOperationTypeChecking}. The fork's logical
     * operand TYPE check stays fork-native (upstream: "Expected type
     * `boolean`, but got `int` instead. Cannot use `int` with operator `or`"
     * — an out-of-block byte divergence, recorded). The operand CARDINALITY
     * check UN-PINNED at PR #454 (the Annex-A inline pin): the released
     * 9.83.0 emits the single-cardinality WARNING (the
     * {@code annexAReleasedSeverities} facet's severity oracle — the released
     * {@code isSingleCheck} bytecode warns; the vendored source's error is a
     * post-release flip).
     */
    @Test
    void testLogicalOperationTypeChecking() {
        UpstreamExpressionPortSupport.parse("1 or False").assertHasValidationError(
                "Expected boolean for left operand of logical but got 'int'");
        UpstreamExpressionPortSupport.parse("True or 3.14").assertHasValidationError(
                "Expected boolean for right operand of logical but got 'number'");
        ParsedExpression multi = UpstreamExpressionPortSupport.parse(
                List.of("a boolean (1..2)"), "a or False");
        multi.assertHasValidationWarning(
                "Expecting single cardinality. The `or` operator requires a single cardinality input");
        assertEquals(0, multi.validationErrorCount(),
                "the released 9.83.0 emits the logical cardinality check as a WARNING only");
    }

    /** Upstream {@code testEqualityOperationTypeInference}. */
    @Test
    void testEqualityOperationTypeInference() {
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("[2, 3] = [6.0, 7, 8]"), BOOLEAN_NO_META, false);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("[2, 3] <> [6.0, 7, 8]"), BOOLEAN_NO_META, false);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("[1, 3] all = 5.0"), BOOLEAN_NO_META, false);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("[1, 3] any = 5.0"), BOOLEAN_NO_META, false);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse(List.of("a int (0..1)"), "a = 1"),
                BOOLEAN_NO_META, false);
    }

    /**
     * Upstream {@code testEqualityOperationTypeChecking} — UN-PINNED at PR
     * #454 (the Annex-A equality family): comparability is the ERROR ("Types
     * `int` and `boolean` are not comparable" — upstream
     * {@code comparableTypeCheck}, either-subtype semantics) and the modified
     * ({@code all}/{@code any}) cardinality-shape checks are WARNINGS in the
     * released 9.83.0 ({@code checkModifiedBinaryOperation} → the released
     * {@code isMultiCheck}/{@code isSingleCheck} bytecode — the
     * {@code annexAReleasedSeverities} facet's severity oracle): a failing
     * LEFT suggests flip (right multi) or remove-the-modifier (right single);
     * a failing RIGHT suggests flip (left single) or carries NO suggestion
     * (both multi — the bare form, the V0 banks' corpus class).
     */
    @Test
    void testEqualityOperationTypeChecking_pinnedFacetLead() {
        UpstreamExpressionPortSupport.parse("1 = True").assertHasValidationError(
                "Types `int` and `boolean` are not comparable");
        UpstreamExpressionPortSupport.parse("[1, 2] <> [True, False, False]")
                .assertHasValidationError("Types `int` and `boolean` are not comparable");

        ParsedExpression allEq = UpstreamExpressionPortSupport.parse("[1, 2] all = [1, 2]");
        allEq.assertHasValidationWarning("Expecting single cardinality");
        assertEquals(0, allEq.validationErrorCount(),
                "comparable ints — the modified-shape finding is the bare right-multi WARNING only");

        ParsedExpression anyNeq = UpstreamExpressionPortSupport.parse("5 any <> [1, 2]");
        anyNeq.assertHasValidationWarning(
                "Expecting multi cardinality. Did you mean to flip around the operands of the `<>` operator?");
        anyNeq.assertHasValidationWarning(
                "Expecting single cardinality. Did you mean to flip around the operands of the `<>` operator?");

        UpstreamExpressionPortSupport.parse(List.of("a int (1..2)"), "[1, 3] any <> a")
                .assertHasValidationWarning("Expecting single cardinality");
    }

    /**
     * Upstream {@code testArithmeticOperationTypeInference}. Fork-native
     * (finding #6): operations JOIN operand digit/length constraints (no
     * interval fold — upstream {@code 3 + 4 → int(min 7, max 7)}, string
     * concat length-SUM {@code (4,4)}); division joins to int (upstream:
     * unconstrained number). The string concat validates CLEAN since the
     * #441 finding-#5 wave (upstream's arithmetic algebra admits it — the
     * two numeric-only false positives died; the un-pin).
     */
    @Test
    void testArithmeticOperationTypeInference() {
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("3 + 4"), intNoMeta(1), false);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("3.0 + 4"), numberNoMeta(2, 1), false);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("3 + 4.0"), numberNoMeta(2, 1), false);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("3.0 + 4.0"), numberNoMeta(2, 1), false);

        assertIsValidWithType(UpstreamExpressionPortSupport.parse("\"ab\" + \"cd\""),
                stringNoMeta(2, 2), false);

        assertIsValidWithType(UpstreamExpressionPortSupport.parse("3 - 4"), intNoMeta(1), false);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("3 - 4.0"), numberNoMeta(2, 1), false);

        assertIsValidWithType(UpstreamExpressionPortSupport.parse("3 * 4"), intNoMeta(1), false);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("3.0 * 4"), numberNoMeta(2, 1), false);

        assertIsValidWithType(UpstreamExpressionPortSupport.parse("3 / 4"), intNoMeta(1), false);
    }

    /**
     * Upstream {@code testArithmeticOperationTypeChecking}. The TYPE errors
     * assert upstream's own message bytes since the #441 finding-#5 wave
     * (facet arithTypeAlgebra — the un-pin); the CARDINALITY checks assert
     * the RELEASED-9.83.0 bytes since the #455 warning-family wave (facet
     * warningFamilyWaves — upstream's per-operand {@code isSingleCheck}
     * WARNS in the released bytecode, the #454 severity oracle; the
     * pre-#455 fork-native {@code Expected single value for ...} ERROR was
     * the recorded #454 OBS severity divergence).
     */
    @Test
    void testArithmeticOperationTypeChecking() {
        UpstreamExpressionPortSupport.parse("[1, 2] + 3").assertHasValidationWarning(
                "Expecting single cardinality. The `+` operator requires a single cardinality input");
        UpstreamExpressionPortSupport.parse("1.5 * False").assertHasValidationError(
                "Expected type `number`, but got `boolean` instead. Cannot use `boolean` with operator `*`");
        UpstreamExpressionPortSupport.parse("\"ab\" + 3").assertHasValidationError(
                "Expected type `string`, but got `int` instead. Cannot add `int` to a `string`");
        UpstreamExpressionPortSupport.parse(List.of("a int (1..2)"), "a + 5").assertHasValidationWarning(
                "Expecting single cardinality. The `+` operator requires a single cardinality input");
    }

    /** Upstream {@code testComparisonOperationTypeInference}. */
    @Test
    void testComparisonOperationTypeInference() {
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("1 < 2"), BOOLEAN_NO_META, false);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("3 > 3.14"), BOOLEAN_NO_META, false);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("-5.1 <= 42"), BOOLEAN_NO_META, false);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("-3.14 >= 3.14"), BOOLEAN_NO_META, false);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("[1, 2] any < 5"), BOOLEAN_NO_META, false);
    }

    /**
     * Upstream {@code testComparisonOperationTypeChecking} — UN-PINNED at PR
     * #454 (the Annex-A comparison family): the ordered-type LADDER errors
     * (zonedDateTime/date/number left-driven dispatch — "Cannot compare a
     * `boolean` to a `number`" replaces the fork's boolean-is-ordered
     * natural-order laxity, the pin's recorded gap) and every cardinality
     * shape — the unmodified per-operand "in front of" form and the modified
     * flip/remove forms — is a released-9.83.0 WARNING (the
     * {@code annexAReleasedSeverities} facet's severity oracle). An
     * {@code empty} operand short-circuits the ladder (NOTHING is the bottom
     * type — upstream's own left-NOTHING arm).
     */
    @Test
    void testComparisonOperationTypeChecking_pinnedFacetLead() {
        UpstreamExpressionPortSupport.parse("[1, 2] < 3").assertHasValidationWarning(
                "Expecting single cardinality. Did you mean to use `all` or `any` in front of the `<` operator?");
        UpstreamExpressionPortSupport.parse("1.5 <= False").assertHasValidationError(
                "Expected type `number`, but got `boolean` instead. Cannot compare a `boolean` to a `number`");

        ParsedExpression emptyCmp = UpstreamExpressionPortSupport.parse("empty any < empty");
        emptyCmp.assertHasValidationWarning(
                "Expecting multi cardinality. Did you mean to remove the `any` modifier on the `<` operator?");
        assertEquals(0, emptyCmp.validationErrorCount(),
                "NOTHING short-circuits the comparison ladder — no type error on empty operands");

        UpstreamExpressionPortSupport.parse("[1, 2] all > [1, 2]")
                .assertHasValidationWarning("Expecting single cardinality");

        ParsedExpression anyLte = UpstreamExpressionPortSupport.parse("5 any <= [1, 2]");
        anyLte.assertHasValidationWarning(
                "Expecting multi cardinality. Did you mean to flip around the operands of the `<=` operator?");
        anyLte.assertHasValidationWarning(
                "Expecting single cardinality. Did you mean to flip around the operands of the `<=` operator?");

        UpstreamExpressionPortSupport.parse("5 all >= 1").assertHasValidationWarning(
                "Expecting multi cardinality. Did you mean to remove the `all` modifier on the `>=` operator?");

        UpstreamExpressionPortSupport.parse(List.of("a int (1..2)"), "a < 5")
                .assertHasValidationWarning(
                "Expecting single cardinality. Did you mean to use `all` or `any` in front of the `<` operator?");
        UpstreamExpressionPortSupport.parse(List.of("a int (1..2)"), "[1, 2] any < a")
                .assertHasValidationWarning("Expecting single cardinality");
    }

    /**
     * Upstream {@code testConditionalExpressionTypeInference}. Fork-native:
     * the branch JOIN folds digits/fractionalDigits (no min/max — finding #6)
     * and the conditional's cardinality is SINGLE even with list branches
     * (finding #8 — upstream: MULTI).
     */
    @Test
    void testConditionalExpressionTypeInference() {
        ParsedExpression p = UpstreamExpressionPortSupport.parse(
                "if True then [1, 2] else [3.0, 4.0, 5.0, 6.0]");
        p.assertNoIssues();
        assertEquals(numberNoMeta(2, 1), p.type(), "the branch join (upstream adds min 1 / max 6)");
        assertFalse(p.isMulti(),
                "pin drift (finding #8: a conditional with list branches must eventually be "
                + "MULTI like upstream): cardinality flipped — HEALED? un-pin to isMulti==true");
    }

    /**
     * Upstream {@code testConditionalExpressionTypeChecking}. The condition
     * single-cardinality check asserts the RELEASED-9.83.0 bytes since the
     * #455 warning-family wave (the {@code isSingleCheck} WARNING with the
     * released suggestion — the recorded out-of-block divergence CLOSED);
     * the branch common-supertype check UN-PINNED at PR #454 (the Annex-A
     * inline pin): the branches must join below ANY — else the ERROR
     * "Types `int` and `boolean` do not have a common supertype".
     */
    @Test
    void testConditionalExpressionTypeChecking() {
        UpstreamExpressionPortSupport.parse("if [True, False] then 1 else 2").assertHasValidationWarning(
                "Expecting single cardinality. The condition of an if-then-else expression should be single cardinality");
        for (String expr : List.of("if True then 1 else False",
                "if True then [1, 2, 3] else [False, True]")) {
            UpstreamExpressionPortSupport.parse(expr).assertHasValidationError(
                    "Types `int` and `boolean` do not have a common supertype");
        }
    }

    /**
     * Upstream {@code testListLiteralTypeInference}. Fork-native: the item
     * join folds digits/fractionalDigits (no min/max — finding #6) and the
     * EMPTY list literal is MULTI (finding #8 — upstream: single).
     */
    @Test
    void testListLiteralTypeInference() {
        ParsedExpression empty = UpstreamExpressionPortSupport.parse("[]");
        empty.assertNoIssues();
        assertEquals(NOTHING_NO_META, empty.type());
        assertTrue(empty.isMulti(),
                "pin drift (finding #8: upstream types [] as SINGLE): cardinality flipped — "
                + "HEALED? un-pin to isMulti==false");
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("[2, 4.5, 7, -3.14]"),
                numberNoMeta(3, 2), true);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("[2, [1, 2], -3.14]"),
                numberNoMeta(3, 2), true);
    }

    /**
     * Upstream {@code testListLiteralTypeChecking} — UN-PINNED at PR #454
     * (the Annex-A list-literal family): the elements must share a common
     * supertype ({@code commonTypeCheck} — the join degenerating to ANY is
     * the failure), the ERROR "Types `int` and `boolean` do not have a
     * common supertype" anchored at the failing element.
     */
    @Test
    void testListLiteralTypeChecking_pinnedFacetLead() {
        UpstreamExpressionPortSupport.parse("[1, True]").assertHasValidationError(
                "Types `int` and `boolean` do not have a common supertype");
    }

    /** Upstream {@code testFunctionCallTypeInference}. */
    @Test
    void testFunctionCallTypeInference() {
        String context = """
                func SomeFunc:
                	inputs:
                		a int (1..1)
                		b boolean (2..4)
                	output: result number (3..5)
                	add result:
                		[1.0, 2.0, 3.0]
                """;
        assertIsValidWithType(
                UpstreamExpressionPortSupport.parse(List.of(context), List.of(),
                        "SomeFunc(42, [True, False, True])"),
                UNCONSTRAINED_NUMBER_NO_META, true);
    }

    /**
     * Upstream {@code testFunctionCallTypeChecking} — UN-PINNED at PR #454
     * (the Annex-A function-call family): ARITY is the ERROR ("Expected 2
     * arguments, but got 3 instead" — released-verified singular/plural
     * recipe), per-argument ASSIGNABILITY is the ERROR ("Expected type
     * `boolean`, but got `int` instead. Cannot assign `int` to input `b`"),
     * and per-argument cardinality (single-parameter positions only) is the
     * released-9.83.0 bare WARNING — not exercised here (the multi param
     * {@code b (2..4)} takes list arguments legally).
     */
    @Test
    void testFunctionCallTypeChecking_pinnedFacetLead() {
        String context = """
                namespace test

                func SomeFunc:
                	inputs:
                	    a int (1..1)
                	    b boolean (2..4)
                	output: result int (1..1)
                	set result:
                		42
                """;
        ParsedExpression threeArgs = UpstreamExpressionPortSupport.parse(List.of(context),
                List.of(), "SomeFunc(1, [False, True], True)");
        threeArgs.assertHasValidationError("Expected 2 arguments, but got 3 instead");
        ParsedExpression badArgType = UpstreamExpressionPortSupport.parse(List.of(context),
                List.of(), "SomeFunc(1, [2, 3])");
        badArgType.assertHasValidationError(
                "Expected type `boolean`, but got `int` instead. Cannot assign `int` to input `b`");
        assertEquals(1, badArgType.validationErrorCount(),
                "arity matches — exactly the one assignability error");
    }

    /**
     * Upstream {@code testProjectionTypeInference} — the SET-BODY channel
     * (the #279 arm types {@code input -> feature} navs there; alias bodies
     * type identically since the #429 gate lift — finding #9 CLOSED at #438).
     * Types port fully. The finding-#4 pin (the stale {@code ENUM_NOT_FOUND}
     * linking diagnostic per nav, documented at #279) HEALED at the PR #443
     * class-(a) wave — the input-feature channel clears on bind; un-pinned to
     * upstream's clean linking. ONE measured delta stays pinned inline: the
     * nav cardinality staying SINGLE regardless of input/feature multiplicity
     * on the GLOBAL channel (finding #8 — upstream: {@code a -> y}/{@code a ->
     * z}/multi-{@code a} forms are MULTI; the #443 head-aware arm fixed the
     * RULE-BODY channel only — the global read is function-tail byte-frozen).
     */
    @Test
    void testProjectionTypeInference() {
        String context = """
                namespace test

                type A:
                	x int (1..1)
                	y number (0..*)
                	z boolean (3..7)
                """;
        record Case(String attr, String expr, RMetaAnnotatedType type, boolean upstreamMulti) { }
        List<Case> cases = List.of(
                new Case("a A (1..1)", "a -> x", UNCONSTRAINED_INT_NO_META, false),
                new Case("a A (1..1)", "a -> y", UNCONSTRAINED_NUMBER_NO_META, true),
                new Case("a A (1..1)", "a -> z", BOOLEAN_NO_META, true),
                new Case("a A (2..5)", "a -> x", UNCONSTRAINED_INT_NO_META, true));
        for (Case c : cases) {
            ParsedExpression p = UpstreamExpressionPortSupport.parseInSetBody(
                    List.of(context), List.of(c.attr()), c.expr());
            assertEquals(c.type(), p.type(), c.expr() + " [" + c.attr() + "]");
            assertFalse(p.isMulti(),
                    "pin drift (finding #8: upstream cardinality " + c.upstreamMulti()
                    + " for " + c.expr() + " [" + c.attr() + "]) — HEALED? un-pin to upstream");
            assertTrue(p.wrapperLinking().isEmpty(),
                    "the finding-#4 stale nav diagnostic healed at #443 — upstream "
                    + "links these navs clean: " + p.wrapperLinking());
        }
    }

    /** Upstream {@code testEnumTypeInference}. */
    @Test
    void testEnumTypeInference() {
        String context = """
                namespace test

                enum A:
                	V1
                	V2

                func Test:
                	output: result A (1..1)
                	set result:
                		A -> V1
                """;
        ParsedExpression p = UpstreamExpressionPortSupport.parse(List.of(context), List.of(), "A -> V1");
        p.assertNoIssues();
        assertEquals("A", p.type().type().name(), "enum value ref must type as the enum");
        assertTrue(p.type().metaAttributes().isEmpty(), "no meta on the bare enum ref");
        assertFalse(p.isMulti());
    }

    /** Upstream {@code testExistsTypeInference}. */
    @Test
    void testExistsTypeInference() {
        assertIsValidWithType(UpstreamExpressionPortSupport.parse(
                List.of("a int (0..1)"), "a exists"), BOOLEAN_NO_META, false);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse(
                List.of("a int (0..3)"), "a exists"), BOOLEAN_NO_META, false);
    }

    /** Upstream {@code testExistsTypeChecking} — upstream-EMPTY (all-commented body), carried. */
    @Test
    @Disabled("upstream-EMPTY: every assert in the upstream body is commented out (TODO)")
    void testExistsTypeChecking() {
    }

    /** Upstream {@code testAbsentTypeInference}. */
    @Test
    void testAbsentTypeInference() {
        assertIsValidWithType(UpstreamExpressionPortSupport.parse(
                List.of("a int (0..1)"), "a is absent"), BOOLEAN_NO_META, false);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse(
                List.of("a int (0..3)"), "a is absent"), BOOLEAN_NO_META, false);
    }

    /** Upstream {@code testAbsentTypeChecking} — upstream-EMPTY (all-commented body), carried. */
    @Test
    @Disabled("upstream-EMPTY: every assert in the upstream body is commented out (TODO)")
    void testAbsentTypeChecking() {
    }

    /**
     * Upstream {@code testCountTypeInference}. Fork-native: {@code count}
     * types as the unconstrained int (upstream folds the non-negative bound —
     * {@code min 0}; finding #6).
     */
    @Test
    void testCountTypeInference() {
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("empty count"), UNCONSTRAINED_INT_NO_META, false);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("42 count"), UNCONSTRAINED_INT_NO_META, false);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse("[1, 2, 3] count"), UNCONSTRAINED_INT_NO_META, false);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse(
                "(if True then empty else [1, 2, 3]) count"), UNCONSTRAINED_INT_NO_META, false);
    }

    /**
     * Upstream {@code testOnlyExistsTypeInference} — the nav forms ride the
     * SET-BODY channel (clean here: the only-exists path list resolves without
     * the finding-#4 stale diagnostic); the condition-context halves assert on
     * the parsed context model exactly like upstream.
     */
    @Test
    void testOnlyExistsTypeInference() {
        String context = """
                namespace test

                type A:
                	x int (0..1)
                	y number (0..3)
                	z boolean (0..*)

                	condition C:
                		x only exists and (x, y) only exists
                """;
        ParsedExpression p = UpstreamExpressionPortSupport.parseInSetBody(
                List.of(context), List.of("a A (1..1)"), "a -> x only exists");
        p.assertNoIssues();
        assertType(p, BOOLEAN_NO_META, false);
        ParsedExpression p2 = UpstreamExpressionPortSupport.parseInSetBody(
                List.of(context), List.of("a A (1..1)"), "(a -> x, a -> y) only exists");
        p2.assertNoIssues();
        assertType(p2, BOOLEAN_NO_META, false);

        RModel ctxModel = contextModel(p);
        RDataType a = (RDataType) ctxModel.rootElements().get(0);
        RLogicalExpr condition = (RLogicalExpr) a.conditions().get(0).expression();
        assertEquals(BOOLEAN_NO_META, p.ws().getInferredType(condition.left().orElseThrow()),
                "condition left half");
        assertEquals(BOOLEAN_NO_META, p.ws().getInferredType(condition.right().orElseThrow()),
                "condition right half");
    }

    /**
     * Upstream {@code testOnlyExistsTypeChecking} — UN-PINNED at PR #454 (the
     * Annex-A only-exists family): the five structural ERROR classes + the
     * parent-cardinality WARNING on the pin's carrier model, derived
     * per-construct from the released semantics:
     *
     * <ul>
     *   <li>UNSUPPORTED TYPE ×5 — every {@code Foo}-parented group errors
     *       ({@code bar (1..1)} is required): condition X's bare {@code baz}
     *       (implicit parent Foo), C1's {@code (x -> baz, …)} group, C2's
     *       {@code (x -> baz, x -> baz)} group, the func's
     *       {@code (a -> x -> baz, a -> x)} group (parent {@code a -> x}
     *       types Foo), and {@code foo -> baz}; {@code A}-parented groups
     *       pass (all attributes optional);</li>
     *   <li>PARENT-PATH EQUALITY ×3 — C1's both mixed groups (an explicit
     *       {@code x} parent vs the parentless {@code y}) and the func's
     *       {@code (a -> x -> baz, a -> x)};</li>
     *   <li>DUPLICATE ATTRIBUTE ×2 — C2's {@code (x, x)} and
     *       {@code (x -> baz, x -> baz)};</li>
     *   <li>PARENT-OBJECT REQUIREMENT ×1 — the func body's bare
     *       {@code c only exists} (no implicit variable in a function
     *       operation);</li>
     *   <li>the parent-cardinality WARNING ×1 — {@code b -> x} over the
     *       multi input {@code b A (2..3)} (released severity: the
     *       {@code isSingleCheck} warning).</li>
     * </ul>
     */
    @Test
    void testOnlyExistsTypeChecking_pinnedFacetLead() {
        String model = """
                namespace test

                type Foo:
                	bar int (1..1)
                	baz boolean (0..1)

                	condition X:
                		baz only exists

                type A:
                	x Foo (0..1)
                	y number (0..3)
                	z boolean (0..*)

                	condition C1:
                		(x -> baz, y) only exists and (y, x -> baz) only exists
                	condition C2:
                		(x, x) only exists and (x -> baz, x -> baz) only exists

                func Test:
                	inputs:
                	    a A (1..1)
                	    foo Foo (1..1)
                	    b A (2..3)
                	    c A (0..1)
                	output: result boolean (0..*)
                	add result:
                		b -> x only exists
                	add result:
                		c only exists
                	add result:
                		(a -> x -> baz, a -> x) only exists
                	add result:
                		foo -> baz only exists
                """;
        ParsedExpression p = UpstreamExpressionPortSupport.parse(List.of(model), List.of(), "True");
        List<com.regnosys.rosetta.symbols.diagnostics.ValidationDiagnostic> context =
                p.ws().validationDiagnostics().stream()
                        .filter(d -> d.range().file().startsWith("expression-port-context-0"))
                        .toList();
        java.util.Map<String, Long> errors = context.stream()
                .filter(d -> d.severity() == com.regnosys.rosetta.symbols.diagnostics.Severity.ERROR)
                .collect(java.util.stream.Collectors.groupingBy(
                                d -> d.message(), java.util.TreeMap::new,
                                java.util.stream.Collectors.counting()));
        assertEquals(
                new java.util.TreeMap<>(java.util.Map.of(
                        "All parent paths must be equal", 3L,
                        "Duplicate attribute", 2L,
                        "Object must have a parent object", 1L,
                        "Operator `only exists` is not supported for type `Foo`. "
                                + "All attributes of input type should be optional", 5L)),
                errors,
                "the five-class only-exists ERROR surface on the pin's carrier model");
        List<String> warnings = context.stream()
                .filter(d -> d.severity() == com.regnosys.rosetta.symbols.diagnostics.Severity.WARNING)
                .map(d -> d.message()).sorted().toList();
        assertEquals(List.of("Expecting single cardinality. "
                        + "The `only exists` operator requires a single cardinality input"),
                warnings,
                "the multi-parent `b -> x` carries the released-severity WARNING exactly once");
    }

    /**
     * Upstream {@code testOnlyElementTypeInference}. Fork-native: the
     * conditional/list joins fold digits/fractionalDigits only (finding #6 —
     * upstream folds min/max: {@code int(1, 0, 2)}, {@code number(3, 1, 0, 42.0)}).
     */
    @Test
    void testOnlyElementTypeInference() {
        assertIsValidWithType(UpstreamExpressionPortSupport.parse(
                "(if True then 0 else [1, 2]) only-element"), intNoMeta(1), false);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse(
                "(if True then empty else [True, False]) only-element"), BOOLEAN_NO_META, false);
        assertIsValidWithType(UpstreamExpressionPortSupport.parse(
                "(if True then 0 else [1, 2, 3, 42.0]) only-element"), numberNoMeta(3, 1), false);
    }

    /** Upstream {@code testOnlyElementTypeChecking} — upstream-EMPTY (all-commented body), carried. */
    @Test
    @Disabled("upstream-EMPTY: every assert in the upstream body is commented out (TODO)")
    void testOnlyElementTypeChecking() {
    }

    /** Upstream {@code testTypeAliasJoin}. */
    @Test
    void testTypeAliasJoin() {
        String model = """
                namespace test

                typeAlias maxNString(n int): string(minLength: 1, maxLength: n)
                typeAlias max3String: maxNString(n: 3)
                typeAlias max4String: maxNString(n: 4)

                func Test:
                	inputs:
                		s1 max3String (1..1)
                		s2 max4String (1..1)
                		s3 maxNString(n: 4) (1..1)
                	output: result string (0..*)
                	add result: if True then s1 else s2
                	add result: if True then s2 else s2
                	add result: if True then s2 else s3
                """;
        ParsedExpression p = UpstreamExpressionPortSupport.parse(List.of(model), List.of(), "True");
        RModel ctxModel = contextModel(p);
        RFunction fn = func(ctxModel, "Test");
        assertEquals(3, fn.operations().size(), "context func did not parse");

        // Upstream: op0 joins max3String|max4String to the parameterized base
        // (maxNString(n: 4)), op1 keeps max4String, op2 joins max4String with the
        // structurally-identical maxNString(n: 4). Fork-native form: op1's join
        // of s2 with itself must EQUAL the type of the bare s2 reference (its
        // then-branch), and every join must type (non-MISSING) with op0 ≡ op2
        // (both are the maxLength-4 join outcome).
        var cond1 = (com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr)
                fn.operations().get(1).expression();
        RMetaAnnotatedType s2RefType = p.ws().getInferredType(cond1.thenBranch());
        RMetaAnnotatedType op0 = p.ws().getInferredType(fn.operations().get(0).expression());
        RMetaAnnotatedType op1 = p.ws().getInferredType(fn.operations().get(1).expression());
        RMetaAnnotatedType op2 = p.ws().getInferredType(fn.operations().get(2).expression());
        assertFalse(op0.isMissing(), "op0 must type");
        assertFalse(op1.isMissing(), "op1 must type");
        assertFalse(op2.isMissing(), "op2 must type");
        assertEquals(s2RefType, op1, "if True then s2 else s2 must keep s2's type");
        assertEquals(op0, op2, "the 3|4 join and the 4|4-structural join must agree");
    }

    /** Upstream {@code shouldCoerceStringToParameterizedString}. */
    @Test
    void shouldCoerceStringToParameterizedString() {
        parseModelNoIssues("""
                namespace test

                func Test:
                	inputs: str string (1..1)
                	output: max3String string(minLength: 1, maxLength: 3) (1..1)
                	set max3String: str
                """);
    }

    /** Upstream {@code shouldCoerceParameterizedStringToString}. */
    @Test
    void shouldCoerceParameterizedStringToString() {
        parseModelNoIssues("""
                namespace test

                func Test:
                	inputs: max3String string(minLength: 1, maxLength: 3) (1..1)
                	output: str string (1..1)
                	set str: max3String
                """);
    }

    /** Upstream {@code shouldCoerceStringToStringTypeAlias}. */
    @Test
    void shouldCoerceStringToStringTypeAlias() {
        parseModelNoIssues("""
                namespace test

                typeAlias Max3String: string(minLength: 1, maxLength: 3)

                func Test:
                	inputs: str string (1..1)
                	output: max3String Max3String (1..1)
                	set max3String: str
                """);
    }

    /** Upstream {@code shouldCoerceStringTypeAliasToString}. */
    @Test
    void shouldCoerceStringTypeAliasToString() {
        parseModelNoIssues("""
                namespace test

                typeAlias Max3String: string(minLength: 1, maxLength: 3)

                func Test:
                	inputs: max3String Max3String (1..1)
                	output: str string (1..1)
                	set str: max3String
                """);
    }

    /** Upstream {@code shouldCoerceDifferentParameterizedStrings}. */
    @Test
    void shouldCoerceDifferentParameterizedStrings() {
        parseModelNoIssues("""
                namespace test

                func Test:
                	inputs: max10String string(minLength: 1, maxLength: 10) (1..1)
                	output: max3String string(minLength: 1, maxLength: 3) (1..1)
                	set max3String: max10String
                """);
    }

    /** Upstream {@code shouldCoerceDifferentTypeAliases}. */
    @Test
    void shouldCoerceDifferentTypeAliases() {
        parseModelNoIssues("""
                namespace test

                typeAlias Max10String: string(minLength: 1, maxLength: 10)
                typeAlias Max3String: string(minLength: 1, maxLength: 3)

                func Test:
                	inputs: max10String Max10String (1..1)
                	output: max3String Max3String (1..1)
                	set max3String: max10String
                """);
    }

    /** Upstream {@code testAttributeSameNameAsAnnotationTest}. */
    @Test
    void testAttributeSameNameAsAnnotationTest() {
        parseModelNoIssues("""
                type A:
                	[rootType]
                	rootType string (0..1)

                	condition C:
                		rootType exists
                """);
    }

    /** The context model parses, links and validates with no issues (upstream parseRosettaWithNoIssues). */
    private static void parseModelNoIssues(String model) {
        ParsedExpression p = UpstreamExpressionPortSupport.parse(List.of(model), List.of(), "True");
        var contextLinking = p.allLinking().stream()
                .filter(d -> d.range().file().startsWith("expression-port-context-"))
                .toList();
        var contextValidation = p.ws().validationDiagnostics().stream()
                .filter(d -> d.range().file().startsWith("expression-port-context-"))
                .toList();
        assertTrue(contextLinking.isEmpty() && contextValidation.isEmpty(),
                "expected no issues, got linking=" + contextLinking
                        + " validation=" + contextValidation);
    }

    /** Upstream {@code testBinaryExpressionCommonType}. */
    @Test
    void testBinaryExpressionCommonType() {
        String model = """
                isEvent root Foo;

                type Foo:
                	iBar int (0..*)
                	nBar number (0..*)
                	nBuz number (0..*)

                func Qualify_AllNumber:
                	[qualification BusinessEvent]
                	inputs: foo Foo (1..1)
                	output: is_event boolean (1..1)
                	set is_event:
                		[foo -> nBar, foo -> nBuz] contains 4.0

                func Qualify_MixedNumber:
                	[qualification BusinessEvent]
                	inputs: foo Foo (1..1)
                	output: is_event boolean (1..1)
                	set is_event:
                		[foo -> nBar, foo -> iBar] contains 4.0

                func Qualify_IntOnly:
                	[qualification BusinessEvent]
                	inputs: foo Foo (1..1)
                	output: is_event boolean (1..1)
                	set is_event:
                		foo -> iBar any = 4.0
                """;
        ParsedExpression p = UpstreamExpressionPortSupport.parse(List.of(model), List.of(), "True");
        RModel ctxModel = contextModel(p);

        assertEquals("number", containsLeftTypeName(p, ctxModel, "Qualify_AllNumber"));
        assertEquals("number", containsLeftTypeName(p, ctxModel, "Qualify_MixedNumber"));
        assertEquals("int", equalityLeftTypeName(p, ctxModel, "Qualify_IntOnly"));
    }

    private static String containsLeftTypeName(ParsedExpression p, RModel model, String funcName) {
        var contains = AstWalker.findAll(
                func(model, funcName).operations().get(0).expression(),
                com.regnosys.rosetta.ast.expressions.binary.RContainsExpr.class).get(0);
        return p.ws().getInferredType(contains.left().orElseThrow()).type().name();
    }

    private static String equalityLeftTypeName(ParsedExpression p, RModel model, String funcName) {
        var eq = AstWalker.findAll(
                func(model, funcName).operations().get(0).expression(),
                com.regnosys.rosetta.ast.expressions.binary.REqualityExpr.class).get(0);
        return p.ws().getInferredType(eq.left().orElseThrow()).type().name();
    }
}
