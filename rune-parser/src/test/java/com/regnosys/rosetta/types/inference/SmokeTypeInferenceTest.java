package com.regnosys.rosetta.types.inference;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.expressions.literals.*;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.types.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M4 T23 — Smoke tests: hand-picked Rune DSL snippets exercising
 * end-to-end type inference through RWorkspace.build().
 */
class SmokeTypeInferenceTest {

    @Test void literal_int_in_function() {
        var ws = buildWorkspace("""
            namespace "smoke.test"
            func F:
                output: result int (1..1)
                set result: 42
            """);
        var intLiterals = AstWalker.findAll(ws.files().get(0), RIntLiteral.class);
        assertFalse(intLiterals.isEmpty());
        for (var lit : intLiterals) {
            RMetaAnnotatedType t = ws.getInferredType(lit);
            assertInstanceOf(RNumberType.class, t.type());
            assertTrue(((RNumberType) t.type()).isInteger());
        }
    }

    @Test void boolean_literal_in_condition() {
        var ws = buildWorkspace("""
            namespace "smoke.test"
            func F:
                output: result int (1..1)
                set result:
                    if True then 1 else 2
            """);
        var boolLits = AstWalker.findAll(ws.files().get(0), RBooleanLiteral.class);
        for (var lit : boolLits) {
            assertEquals(RBasicType.BOOLEAN, ws.getInferredType(lit).type());
        }
    }

    @Test void string_literal() {
        var ws = buildWorkspace("""
            namespace "smoke.test"
            func F:
                output: result string (1..1)
                set result: "hello"
            """);
        var strLits = AstWalker.findAll(ws.files().get(0), RStringLiteral.class);
        assertFalse(strLits.isEmpty());
        for (var lit : strLits) {
            assertInstanceOf(RStringType.class, ws.getInferredType(lit).type());
        }
    }

    @Test void empty_namespace_no_crash() {
        var ws = buildWorkspace("""
            namespace "smoke.empty"
            """);
        assertNotNull(ws);
    }

    @Test void data_type_only_no_expressions() {
        var ws = buildWorkspace("""
            namespace "smoke.data"
            type Foo:
                bar int (1..1)
                baz string (0..1)
            """);
        var exprs = AstWalker.findAll(ws.files().get(0), RExpression.class);
        assertTrue(exprs.isEmpty());
    }

    @Test void multiple_functions() {
        var ws = buildWorkspace("""
            namespace "smoke.multi"
            func A:
                output: result int (1..1)
                set result: 1
            func B:
                output: result int (1..1)
                set result: 2
            """);
        var intLits = AstWalker.findAll(ws.files().get(0), RIntLiteral.class);
        assertEquals(2, intLits.size());
    }

    // facet sumArgItemTyping (W42 finding #19, PR #432): a sum types as its
    // ARGUMENT's item type (upstream RosettaTypeProvider.caseSumOperation =
    // safeRType(expr.getArgument())) — an int-list sum is int, so a list
    // literal like `[item count, item sum]` joins <Integer>; the pre-#432
    // computer forced the unconstrained-number fallback (BigDecimal in Java),
    // which mis-joined the literal and dragged a spurious int coercion tail.
    @Test void sum_follows_argument_item_type() {
        var ws = buildWorkspace("""
            namespace "smoke.sum"
            func F:
                output: result int (1..1)
                set result: [1, 2, 3] sum
            """);
        var sums = AstWalker.findAll(ws.files().get(0),
                com.regnosys.rosetta.ast.expressions.unary.RListOpExpr.class);
        assertFalse(sums.isEmpty());
        for (var lop : sums) {
            RMetaAnnotatedType t = ws.getInferredType(lop);
            assertInstanceOf(RNumberType.class, t.type());
            assertTrue(((RNumberType) t.type()).isInteger());
        }
    }

    @Test void meta_feature_scheme_types_string() {
        // Post-parity finding #21 (PR #433): an unresolved `-> scheme` meta-feature
        // read on a [metadata scheme]-annotated input types string (upstream
        // RosettaTypeProvider law), so `myInput + myInput -> scheme` joins
        // <String, String, String> instead of the BigDecimal fallback.
        var ws = buildWorkspace("""
            namespace "smoke.metafeature"
            func SomeFunc:
                inputs:
                    myInput string (1..1)
                    [metadata scheme]
                output:
                    myResult string (1..1)
                set myResult: myInput + myInput -> scheme
            """);
        // The nav reaches typing as either shape: RFeatureCall (the linked form)
        // or the disguised 2-name REnumValueRef (head=myInput, value=scheme) —
        // both must type string.
        boolean sawScheme = false;
        for (var fc : AstWalker.findAll(ws.files().get(0),
                com.regnosys.rosetta.ast.expressions.references.RFeatureCall.class)) {
            if ("scheme".equals(fc.featureName())) {
                sawScheme = true;
                RMetaAnnotatedType t = ws.getInferredType(fc);
                assertInstanceOf(RStringType.class, t.type(),
                        "scheme meta-feature must type string, got " + t);
            }
        }
        for (var enr : AstWalker.findAll(ws.files().get(0),
                com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class)) {
            if ("scheme".equals(enr.valueName())) {
                sawScheme = true;
                RMetaAnnotatedType t = ws.getInferredType(enr);
                assertInstanceOf(RStringType.class, t.type(),
                        "disguised scheme meta-feature must type string, got " + t);
            }
        }
        assertTrue(sawScheme, "the model must contain the -> scheme nav");
    }

    @Test void alias_rooted_set_segment_resolves_against_alias_terminal_type() {
        // Post-parity finding #25 (PR #435, facet aliasRootedSetSegmentResolution):
        // an operation whose TARGET names an alias of the enclosing function
        // resolves its path segments against the alias body's TERMINAL type
        // (upstream Xtext links them against the shortcut expression's type) —
        // pre-fix they resolved against the function OUTPUT's type and stayed
        // silently unresolved, so the `id`-named leaf fell into the renderer's
        // C3 meta-pseudo arm. The two-segment alias (`topOut -> foo -> bar`,
        // the workspace parse = RFeatureCall over the disguised REnumValueRef
        // root) must resolve `id` as Bar.id; the single-segment alias
        // (`topOut -> foo`, the bare disguised root) must resolve `bar1` as
        // Foo.bar1; an alias rooted at an INPUT (not the output) declines —
        // its segment stays unresolved exactly as before.
        var ws = buildWorkspace("""
            namespace "smoke.alias"
            type Top:
                foo Foo (1..1)
            type Foo:
                bar1 Bar (0..1)
                bar2 Bar (0..1)
            type Bar:
                id number (1..1)
            func UpdateBarId:
                inputs:
                    top Top (1..1)
                    newId number (1..1)
                output: topOut Top (1..1)
                alias barAlias : topOut -> foo -> bar1
                alias fooAlias : topOut -> foo
                alias inAlias : top -> foo
                set barAlias -> id: newId
                set fooAlias -> bar2: top -> foo -> bar2
                set inAlias -> bar1: top -> foo -> bar1
            """);
        boolean sawId = false;
        boolean sawBar2 = false;
        boolean sawInputRooted = false;
        for (var op : AstWalker.findAll(ws.files().get(0),
                com.regnosys.rosetta.ast.functions.ROperation.class)) {
            var seg = op.segment().orElse(null);
            if (seg == null) continue;
            switch (op.targetName()) {
                case "barAlias" -> {
                    sawId = true;
                    assertTrue(seg.resolvedAttribute().isPresent(),
                            "the id leaf must resolve against the alias terminal type Bar");
                    assertEquals("id", seg.resolvedAttribute().get().name());
                }
                case "fooAlias" -> {
                    sawBar2 = true;
                    assertTrue(seg.resolvedAttribute().isPresent(),
                            "the bar2 leaf must resolve against the alias terminal type Foo");
                    assertEquals("bar2", seg.resolvedAttribute().get().name());
                }
                case "inAlias" -> {
                    sawInputRooted = true;
                    assertTrue(seg.resolvedAttribute().isEmpty(),
                            "an input-rooted alias target must DECLINE (root != output)");
                }
                default -> { }
            }
        }
        assertTrue(sawId && sawBar2 && sawInputRooted,
                "all three alias-target operations must be present");
    }

    @Test void rule_recursion_types_from_non_recursive_arm() {
        // Post-parity finding #32 (PR #437, facet ruleRecursionTyping): the in-cycle
        // rule call seeds NOTHING (upstream RosettaTypeProvider's cycleTracker law)
        // and the literal `item` keyword DIRECTLY in a rule body types as the rule's
        // from-type — so the recursive factorial rule infers int from its
        // non-recursive arm, and the memo-aware seed converges the CALL itself to
        // int (the generator's operand walk reads the settled type, not bottom).
        var ws = buildWorkspace("""
            namespace "smoke.rrec"
            reporting rule Fac from int:
                if item = 1
                then 1
                else item * Fac(item - 1)
            """);
        var rule = AstWalker.findAll(ws.files().get(0),
                com.regnosys.rosetta.ast.functions.RRule.class).get(0);
        RMetaAnnotatedType bodyType = ws.getInferredType(rule.expression().orElseThrow());
        assertInstanceOf(RNumberType.class, bodyType.type(),
                "the recursive rule body must infer int, got: " + bodyType);
        assertTrue(((RNumberType) bodyType.type()).isInteger());
        for (var ref : AstWalker.findAll(ws.files().get(0),
                com.regnosys.rosetta.ast.expressions.references.RSymbolReference.class)) {
            if ("Fac".equals(ref.name())) {
                RMetaAnnotatedType callType = ws.getInferredType(ref);
                assertInstanceOf(RNumberType.class, callType.type(),
                        "the converged in-body rule call must read the settled int, got: "
                                + callType);
            }
        }
    }

    @Test void omitted_multiplicative_left_materializes_implicit_item() {
        // Post-parity finding #38 (PR #437, facet omittedParamBinding): the
        // left-less multiplicative (`a extract [* 2]`) materialises the SYNTHETIC
        // implicit item as its left operand — upstream's derived-state law
        // (ArithmeticOperationImpl.needsGeneratedInput + the default implicit
        // variable) — so the lambda multiplies item * 2 (int) instead of the
        // pre-#437 unary-sign constant fold to -2. The ADDITIVE without-left form
        // stays the prefix unary sign (upstream has no left-less additive).
        var ws = buildWorkspace("""
            namespace "smoke.omit"
            func F1:
                inputs:
                    a int (0..*)
                output:
                    result int (0..*)
                add result:
                    a extract [* 2]
            """);
        var mults = AstWalker.findAll(ws.files().get(0),
                com.regnosys.rosetta.ast.expressions.binary.RArithmeticExpr.class);
        assertEquals(1, mults.size());
        var mult = mults.get(0);
        assertTrue(mult.left().isPresent(),
                "the omitted left operand must be materialised");
        assertInstanceOf(com.regnosys.rosetta.ast.expressions.references.RImplicitVariable.class,
                mult.left().get());
        assertTrue(((com.regnosys.rosetta.ast.expressions.references.RImplicitVariable)
                mult.left().get()).isSynthetic(), "the materialised item is the SYNTHETIC form");
        RMetaAnnotatedType multType = ws.getInferredType(mult);
        assertInstanceOf(RNumberType.class, multType.type(),
                "item * 2 must type int, got: " + multType);
        assertTrue(((RNumberType) multType.type()).isInteger());
    }

    @Test void equality_list_vs_single_without_modifier_errors() {
        // PR #437 (the recorded validation item, half 1 — facet
        // listEqualsCardinalityModifier): an unmodified equality over
        // cardinality-mismatched operands errors with upstream
        // ExpressionValidator.checkEqualityOperation's message byte-identical;
        // the `all`-modified form is upstream's separate modified-operation
        // check — NO cardinality-mismatch error (the negative control).
        var ws = buildWorkspace("""
            namespace "smoke.eqcard"
            type T1:
                num number (1..1)
                nums number (1..*)
            func F1:
                inputs: t1 T1 (1..1)
                        t2 T1 (1..1)
                output: res boolean (1..1)
                set res: t1 -> num = t2 -> nums
            """);
        var errors = ws.validationDiagnostics().stream()
                .filter(d -> d.message().equals(
                        "Operator `=` should specify `all` or `any` when comparing a list to a single value"))
                .toList();
        assertEquals(1, errors.size(),
                "expected exactly the upstream equality-cardinality error: "
                        + ws.validationDiagnostics());
        var wsModified = buildWorkspace("""
            namespace "smoke.eqcard2"
            type T1:
                num number (1..1)
                nums number (1..*)
            func F1:
                inputs: t1 T1 (1..1)
                        t2 T1 (1..1)
                output: res boolean (1..1)
                set res: t1 -> nums all = t2 -> num
            """);
        assertTrue(wsModified.validationDiagnostics().stream()
                .noneMatch(d -> d.message().contains("should specify `all` or `any`")),
                "the modified form must NOT take the unmodified-equality error: "
                        + wsModified.validationDiagnostics());
    }

    @Test void only_element_on_single_receiver_warns() {
        // PR #437 (the recorded validation item, half 2 — facet
        // onlyElementSingleReceiver): only-element over a resolved
        // single-cardinality receiver warns with upstream
        // RosettaSimpleValidator.checkUnaryOperation's message byte-identical;
        // a MULTI receiver stays silent (the negative control).
        var ws = buildWorkspace("""
            namespace "smoke.onlyelem"
            type T1:
                num number (1..1)
                nums number (1..*)
            func F1:
                inputs: t1 T1 (1..1)
                output: res number (1..1)
                set res: t1 -> num only-element
            """);
        var warnings = ws.validationDiagnostics().stream()
                .filter(d -> d.message().equals(
                        "List only-element operation cannot be used for single cardinality expressions."))
                .toList();
        assertEquals(1, warnings.size(),
                "expected exactly the upstream only-element warning: "
                        + ws.validationDiagnostics());
        var wsMulti = buildWorkspace("""
            namespace "smoke.onlyelem2"
            type T1:
                num number (1..1)
                nums number (1..*)
            func F1:
                inputs: t1 T1 (1..1)
                output: res number (1..1)
                set res: t1 -> nums only-element
            """);
        assertTrue(wsMulti.validationDiagnostics().stream()
                .noneMatch(d -> d.message().contains("only-element operation cannot")),
                "a MULTI receiver must NOT warn: " + wsMulti.validationDiagnostics());
    }

    @Test void arithmetic_algebra_admits_upstream_legal_forms() {
        // PR #441 (the finding-#5 wave — facet arithTypeAlgebra, positive
        // controls): upstream's arithmetic type algebra admits string
        // concatenation (the 64-strong DRR false-positive class the wave
        // retires), date - date (the day-count algebra) and date + time (the
        // dateTime algebra) — ZERO diagnostics on all three; the pre-#441
        // numeric-only check errored the string and date forms.
        var wsConcat = buildWorkspace("""
            namespace "smoke.arith1"
            type T1:
                s1 string (1..1)
                s2 string (1..1)
            func F1:
                inputs: t1 T1 (1..1)
                output: res string (1..1)
                set res: t1 -> s1 + t1 -> s2
            """);
        assertTrue(wsConcat.validationDiagnostics().isEmpty(),
                "legal string concatenation must draw ZERO diagnostics: "
                        + wsConcat.validationDiagnostics());
        var wsDateSub = buildWorkspace("""
            namespace "smoke.arith2"
            type T1:
                one date (1..1)
                two date (1..1)
            func F1:
                inputs: t1 T1 (1..1)
                output: res int (1..1)
                set res: t1 -> one - t1 -> two
            """);
        assertTrue(wsDateSub.validationDiagnostics().isEmpty(),
                "legal date subtraction must draw ZERO diagnostics: "
                        + wsDateSub.validationDiagnostics());
        var wsDateTime = buildWorkspace("""
            namespace "smoke.arith3"
            type T1:
                d date (1..1)
                t time (1..1)
            func F1:
                inputs: t1 T1 (1..1)
                output: res dateTime (1..1)
                set res: t1 -> d + t1 -> t
            """);
        assertTrue(wsDateTime.validationDiagnostics().isEmpty(),
                "legal date + time must draw ZERO diagnostics: "
                        + wsDateTime.validationDiagnostics());
    }

    @Test void arithmetic_algebra_rejects_with_upstream_messages() {
        // PR #441 (the finding-#5 wave — facet arithTypeAlgebra, negative
        // controls): the ported algebra still rejects ill-typed arithmetic,
        // message bytes upstream's own (the vendored expression
        // ExpressionValidator.java:144-187 + the AbstractExpressionValidator
        // helpers). date + date = upstream's shouldParseWithErrorWhenAddingDates
        // carrier; "ab" + 3 and 1.5 * False = upstream's
        // testArithmeticOperationTypeChecking carriers.
        var wsAddDates = buildWorkspace("""
            namespace "smoke.arith4"
            type T1:
                one date (1..1)
                two date (1..1)
            func F1:
                inputs: t1 T1 (1..1)
                output: res dateTime (1..1)
                set res: t1 -> one + t1 -> two
            """);
        assertEquals(1, wsAddDates.validationDiagnostics().size(),
                "the date+date model must draw EXACTLY ONE diagnostic: "
                        + wsAddDates.validationDiagnostics());
        assertEquals("Expected type `time`, but got `date` instead. Cannot add `date` to a `date`",
                wsAddDates.validationDiagnostics().get(0).message(),
                "the upstream date+date message bytes");
        var wsStrInt = buildWorkspace("""
            namespace "smoke.arith5"
            type T1:
                s1 string (1..1)
            func F1:
                inputs: t1 T1 (1..1)
                output: res string (1..1)
                set res: t1 -> s1 + 3
            """);
        assertEquals(1, wsStrInt.validationDiagnostics().size(),
                "the string+int model must draw EXACTLY ONE diagnostic: "
                        + wsStrInt.validationDiagnostics());
        assertEquals("Expected type `string`, but got `int` instead. Cannot add `int` to a `string`",
                wsStrInt.validationDiagnostics().get(0).message(),
                "the upstream string+int message bytes");
        var wsMul = buildWorkspace("""
            namespace "smoke.arith6"
            type T1:
                num number (1..1)
                flag boolean (1..1)
            func F1:
                inputs: t1 T1 (1..1)
                output: res number (1..1)
                set res: t1 -> num * t1 -> flag
            """);
        assertEquals(1, wsMul.validationDiagnostics().size(),
                "the number*boolean model must draw EXACTLY ONE diagnostic: "
                        + wsMul.validationDiagnostics());
        assertEquals("Expected type `number`, but got `boolean` instead. Cannot use `boolean` with operator `*`",
                wsMul.validationDiagnostics().get(0).message(),
                "the upstream multiply-boolean message bytes (the default suggestion)");
        // The unsupported-LEFT arm fires ALONE when the right side is one of
        // the operator's admissible types (upstream's right-side gate):
        var wsUnsupportedLeft = buildWorkspace("""
            namespace "smoke.arith7"
            type T1:
                flag boolean (1..1)
            func F1:
                inputs: t1 T1 (1..1)
                output: res string (1..1)
                set res: t1 -> flag + 1
            """);
        assertEquals(1, wsUnsupportedLeft.validationDiagnostics().size(),
                "EXACTLY ONE diagnostic — the admissible int right operand must "
                        + "NOT draw the right-side unsupported error: "
                        + wsUnsupportedLeft.validationDiagnostics());
        assertEquals("Operator `+` is not supported for type `boolean`. Supported types are `number`, `string` and `date`",
                wsUnsupportedLeft.validationDiagnostics().get(0).message(),
                "the upstream unsupported-left message bytes");
        // BOTH unsupported arms fire when neither side is admissible:
        var wsBothBad = buildWorkspace("""
            namespace "smoke.arith8"
            type T1:
                flag boolean (1..1)
                flag2 boolean (1..1)
            func F1:
                inputs: t1 T1 (1..1)
                output: res number (1..1)
                set res: t1 -> flag - t1 -> flag2
            """);
        assertEquals(2, wsBothBad.validationDiagnostics().size(),
                "EXACTLY TWO diagnostics — both unsupported-operand errors and "
                        + "nothing else: " + wsBothBad.validationDiagnostics());
        assertTrue(wsBothBad.validationDiagnostics().stream()
                .allMatch(d -> d.message().equals(
                        "Operator `-` is not supported for type `boolean`. Supported types are `number` and `date`")),
                "every diagnostic carries the upstream unsupported-operand "
                        + "message bytes: " + wsBothBad.validationDiagnostics());
    }

    @Test void attribute_override_admits_upstream_legal_forms() {
        // PR #442 (the Annex-B(B-1) wave — the override seat, positive
        // controls): upstream's checkAttributeOverride admits any SUBTYPE
        // override (restriction) — the pre-#442 fork seat compared raw
        // type-call name strings for equality and errored all 171 legal DRR
        // restriction overrides. Data-type restriction, alias-to-alias
        // restriction (parameter-blind Rule 5 — the corpus's
        // Min20Max72-vs-Max52 class admits even though the intervals do not
        // nest), and duplicate condition names (the retired fork-native
        // uniqueness check — upstream has NO such check and shipped DRR
        // models carry duplicates) all draw ZERO diagnostics; an override on
        // an UNRESOLVED supertype declines rather than fabricating.
        var wsRestrict = buildWorkspace("""
            namespace "smoke.override1"
            type Base:
            type Narrow extends Base:
            type Foo:
                attr Base (0..1)
            type Bar extends Foo:
                override attr Narrow (0..1)
            """);
        assertTrue(wsRestrict.validationDiagnostics().isEmpty(),
                "a legal data-type restriction override must draw ZERO diagnostics: "
                        + wsRestrict.validationDiagnostics());
        var wsAlias = buildWorkspace("""
            namespace "smoke.override2"
            typeAlias Max52Text: string(maxLength: 52)
            typeAlias Min20Max72Text: string(minLength: 20, maxLength: 72)
            type Foo:
                attr Max52Text (0..1)
            type Bar extends Foo:
                override attr Min20Max72Text (0..1)
            """);
        assertTrue(wsAlias.validationDiagnostics().isEmpty(),
                "a string-alias override must draw ZERO diagnostics (Rule 5 is "
                        + "parameter-blind, matching upstream): "
                        + wsAlias.validationDiagnostics());
        var wsDupCond = buildWorkspace("""
            namespace "smoke.override3"
            type T1:
                num number (1..1)
                condition C1: num exists
                condition C1: num exists
            """);
        assertTrue(wsDupCond.validationDiagnostics().isEmpty(),
                "duplicate condition names are LEGAL upstream (no uniqueness "
                        + "check exists) — the fork-native check is retired: "
                        + wsDupCond.validationDiagnostics());
        var wsUnresolved = buildWorkspace("""
            namespace "smoke.override4"
            type Bar extends UnknownParent:
                override attr number (0..1)
            """);
        assertTrue(wsUnresolved.validationDiagnostics().isEmpty(),
                "an override on an UNRESOLVED supertype must DECLINE (the #437 "
                        + "fabrication guard), not error: "
                        + wsUnresolved.validationDiagnostics());
        var wsSameChoice = buildWorkspace("""
            namespace "smoke.override9"
            type Opt1:
            type Opt2:
            choice DataChoice:
                Opt1
                Opt2
            type Foo:
                attr DataChoice (1..1)
            type Bar extends Foo:
                override attr DataChoice (1..1)
            """);
        assertTrue(wsSameChoice.validationDiagnostics().isEmpty(),
                "re-declaring the SAME choice type admits by identity (upstream's "
                        + "data-view seat): " + wsSameChoice.validationDiagnostics());
        var wsExtendsChoice = buildWorkspace("""
            namespace "smoke.override10"
            type Opt1:
            type Opt2:
            choice DataChoice:
                Opt1
                Opt2
            type Impl extends DataChoice:
            type Foo:
                attr DataChoice (1..1)
            type Bar extends Foo:
                override attr Impl (1..1)
            """);
        // PR #455: the extends-choice fixture legitimately carries upstream's
        // `Extending a choice type is deprecated` WARNING (the warning-family
        // wave); the override admission contract is ERROR-freedom.
        assertTrue(wsExtendsChoice.validationDiagnostics().stream()
                        .noneMatch(d -> d.severity()
                                == com.regnosys.rosetta.symbols.diagnostics.Severity.ERROR),
                "a data type EXTENDING the parent choice admits (upstream's "
                        + "data-view walk reaches the same choice — the Seat-1 "
                        + "#442 MF-1 witness; Rune allows type-extends-choice): "
                        + wsExtendsChoice.validationDiagnostics());
        assertEquals(1, wsExtendsChoice.validationDiagnostics().stream()
                        .filter(d -> d.message().equals("Extending a choice type is deprecated"))
                        .count(),
                "the extends-choice deprecation warns exactly once (PR #455)");
    }

    @Test void attribute_override_rejects_with_upstream_messages() {
        // PR #442 (the Annex-B(B-1) wave — the override seat, negative
        // controls): message bytes are upstream's own test carriers (vendored
        // AttributeValidatorTest: testCannotOverrideAttributeToNonSubtype,
        // testCannotOverrideNonExistingAttribute,
        // testCannotOverrideAttributeToDifferentSubtype — which also witnesses
        // the nearest-ancestor shadowing walk — and
        // testCannotOverrideChoiceTypeToOption, upstream's documented #797
        // limitation: the seat treats a choice as its DATA view, so narrowing
        // to an option rejects even though the fork's shared Rule 8 would
        // admit it).
        var wsNonSubtype = buildWorkspace("""
            namespace "smoke.override5"
            type Foo:
                attr number (0..1)
            type Bar extends Foo:
                override attr string (0..1)
            """);
        assertEquals(1, wsNonSubtype.validationDiagnostics().size(),
                "the non-subtype override must draw EXACTLY ONE diagnostic: "
                        + wsNonSubtype.validationDiagnostics());
        assertEquals("The overridden type should be a subtype of the parent type number",
                wsNonSubtype.validationDiagnostics().get(0).message(),
                "the upstream non-subtype message bytes");
        var wsNotFound = buildWorkspace("""
            namespace "smoke.override6"
            type Foo:
                attr number (0..1)
            type Bar extends Foo:
                override otherAttr number (0..1)
            """);
        assertEquals(1, wsNotFound.validationDiagnostics().size(),
                "the absent-parent-attribute override must draw EXACTLY ONE "
                        + "diagnostic: " + wsNotFound.validationDiagnostics());
        assertEquals("Attribute otherAttr does not exist in supertype",
                wsNotFound.validationDiagnostics().get(0).message(),
                "the upstream not-found message bytes");
        var wsShadow = buildWorkspace("""
            namespace "smoke.override7"
            type Base:
            type Child1 extends Base:
            type Child2 extends Base:
            type Foo:
                attr Base (0..1)
            type Bar extends Foo:
                override attr Child1 (0..1)
            type Qux extends Bar:
                override attr Child2 (0..1)
            """);
        assertEquals(1, wsShadow.validationDiagnostics().size(),
                "only the sibling-subtype override errs (Bar's restriction is "
                        + "legal; Qux's parent is Bar's Child1 restriction — the "
                        + "nearest-ancestor walk): " + wsShadow.validationDiagnostics());
        assertEquals("The overridden type should be a subtype of the parent type Child1",
                wsShadow.validationDiagnostics().get(0).message(),
                "the upstream different-subtype message bytes (the parent type "
                        + "is the MIDDLE type's restriction)");
        var wsChoice = buildWorkspace("""
            namespace "smoke.override8"
            type Opt1:
            type Opt2:
            choice DataChoice:
                Opt1
                Opt2
            type Foo:
                attr DataChoice (1..1)
            type Bar extends Foo:
                override attr Opt1 (1..1)
            """);
        assertEquals(1, wsChoice.validationDiagnostics().size(),
                "narrowing a choice-typed parent to an option must draw EXACTLY "
                        + "ONE diagnostic (upstream's #797 data-view seat): "
                        + wsChoice.validationDiagnostics());
        assertEquals("The overridden type should be a subtype of the parent type DataChoice",
                wsChoice.validationDiagnostics().get(0).message(),
                "the upstream choice-parent message bytes (the choice renders "
                        + "by NAME, not the fork's `choice X` toString)");
    }

    @Test void lexical_head_navs_resolve_without_enum_diagnostics() {
        // PR #443 (the #4-class-(a) wave — positive controls): a disguised
        // 2-segment nav whose head resolves in the LEXICAL context binds the
        // typing-only resolvedInputFeature channel AND clears the stale
        // ENUM_NOT_FOUND the linker's tried-enum interpretation emitted
        // (upstream's parse never takes the enum path for these — it emits
        // NOTHING). Three head shapes: the enclosing function's INPUT (the
        // clearing was the 2026-06-27 channel's documented follow-on), the
        // enclosing function's ALIAS (the #443 channel), and the attribute of
        // the data type DECLARING the enclosing condition (the #443 channel —
        // the drr `leg1 -> direction2` census shape).
        String[] bindingSources = {
            // input-head
            """
            namespace "smoke.lexnav1"
            type Bar:
                y number (0..1)
            func F1:
                inputs: foo Bar (1..1)
                output: res number (0..1)
                set res: foo -> y
            """,
            // condition-context head
            """
            namespace "smoke.lexnav3"
            type Leg:
                direction string (0..1)
            type Report:
                leg1 Leg (0..1)
                condition DirectionPresent:
                    leg1 -> direction exists
            """};
        for (String source : bindingSources) {
            RModel model = AstBuilder.buildFromString(source, "smoke.rosetta");
            var result = RWorkspace.build(List.of(model));
            assertTrue(result.linkingDiagnostics().stream()
                    .noneMatch(d -> d.category().name().startsWith("ENUM_")),
                    "a lexically-resolvable head nav must carry NO enum diagnostic: "
                            + result.linkingDiagnostics());
            var navs = AstWalker.findAll(model,
                    com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class);
            assertFalse(navs.isEmpty(), "the disguised-nav parse shape is the premise");
            for (var nav : navs) {
                assertTrue(nav.resolvedInputFeature().isPresent(),
                        "the lexical-head channel must bind the leaf feature");
                assertFalse(result.workspace().getInferredType(nav).isMissing(),
                        "the bound nav must expose the leaf's type");
            }
        }
        // Alias-head: BIND-MODE since PR #447 (the typed-alias-nav wave). The
        // #443 arm was clearing-only — exposing typed-alias navs then moved 2
        // drr FUNCTION goldens at the cp gate; #447 lands the bind WITH the
        // generator's meta-coercion seat aligned (the conditional-base ladder
        // keeps upstream's raw-meta arms + whole-output deref placement), so
        // the alias channel now binds and types exactly like the other two.
        RModel aliasModel = AstBuilder.buildFromString("""
            namespace "smoke.lexnav2"
            type Bar:
                y number (0..1)
            type Baz:
                bar Bar (0..1)
            func F1:
                inputs: baz Baz (1..1)
                output: res number (0..1)
                alias theBar: baz -> bar
                set res: theBar -> y
            """, "smoke.rosetta");
        var aliasResult = RWorkspace.build(List.of(aliasModel));
        assertTrue(aliasResult.linkingDiagnostics().stream()
                .noneMatch(d -> d.category().name().startsWith("ENUM_")),
                "an alias-headed nav must carry NO enum diagnostic: "
                        + aliasResult.linkingDiagnostics());
        var aliasHeadNavs = AstWalker.findAll(aliasModel,
                com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class)
                .stream().filter(n -> "theBar".equals(n.enumName())).toList();
        assertEquals(1, aliasHeadNavs.size(), "the alias-headed disguised-nav parse "
                + "shape is the premise (the alias BODY `baz -> bar` is a separate, "
                + "legitimately input-bound nav)");
        assertTrue(aliasHeadNavs.get(0).resolvedInputFeature().isPresent(),
                "the #447 alias arm binds the leaf feature");
        assertFalse(aliasResult.workspace()
                        .getInferredType(aliasHeadNavs.get(0)).isMissing(),
                "the bound alias nav must expose the leaf's type");
    }

    @Test void chained_alias_navs_type_through_fixed_point() {
        // PR #447 (the typed-alias-nav wave): an alias whose expression HEADS
        // ANOTHER ALIAS — the census's alias/headTypeMissing block (101 cdm +
        // 84 drr rows) — could never reach the #443 arm because the head
        // alias's own expression stayed MISSING. The bind makes each
        // alias-headed nav type as its leaf attribute, so chains converge in
        // the engine's fixed-point iteration: theBaz (input nav) types →
        // theBar's body `theBaz -> bar` binds+types → `theBar -> y` binds.
        RModel model = AstBuilder.buildFromString("""
            namespace "smoke.aliaschain1"
            type Bar:
                y number (0..1)
            type Baz:
                bar Bar (0..1)
            type Qux:
                baz Baz (0..1)
            func F1:
                inputs: qux Qux (1..1)
                output: res number (0..1)
                alias theBaz: qux -> baz
                alias theBar: theBaz -> bar
                set res: theBar -> y
            """, "smoke.rosetta");
        var result = RWorkspace.build(List.of(model));
        assertTrue(result.linkingDiagnostics().stream()
                .noneMatch(d -> d.category().name().startsWith("ENUM_")),
                "chained alias navs must carry NO enum diagnostic: "
                        + result.linkingDiagnostics());
        var navs = AstWalker.findAll(model,
                com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class);
        assertEquals(3, navs.size(), "all three 2-segment navs parse disguised "
                + "(qux -> baz · theBaz -> bar · theBar -> y)");
        for (var nav : navs) {
            assertTrue(nav.resolvedInputFeature().isPresent(),
                    "every link of the alias chain binds its leaf: " + nav.enumName());
            assertFalse(result.workspace().getInferredType(nav).isMissing(),
                    "every link exposes its leaf's type: " + nav.enumName());
        }
        // Negative twin: a leaf resolving on NO channel of a TYPED alias keeps
        // EXACTLY one diagnostic at the linker's exact message bytes — the
        // bind must not have widened admission beyond the attribute table.
        RModel negModel = AstBuilder.buildFromString("""
            namespace "smoke.aliaschain2"
            type Bar:
                y number (0..1)
            type Baz:
                bar Bar (0..1)
            func F1:
                inputs: baz Baz (1..1)
                output: res number (0..1)
                alias theBar: baz -> bar
                set res: if theBar -> nope exists then 1 else 0
            """, "smoke.rosetta");
        var negResult = RWorkspace.build(List.of(negModel));
        var enumErrors = negResult.linkingDiagnostics().stream()
                .filter(d -> d.category().name().equals("ENUM_NOT_FOUND"))
                .toList();
        assertEquals(1, enumErrors.size(),
                "a non-attribute leaf on a typed alias must keep EXACTLY one "
                        + "ENUM_NOT_FOUND: " + negResult.linkingDiagnostics());
        assertEquals("Enum 'theBar' not found", enumErrors.get(0).message(),
                "the linker's message bytes");
    }

    @Test void lexical_head_cardinality_is_head_aware() {
        // PR #443 (the #4-class-(a) wave — the drr carrier's regression
        // witness): `[<enum values>] <> <multi input> -> <single feature>` is
        // list-vs-LIST upstream (CardinalityProvider ORs the receiver into a
        // feature call's cardinality), so the unmodified `<>` draws NO
        // cardinality error — the V0-DRR oracle is silent on the corpus
        // carrier (regulation-common-trade-payment-func.rosetta:94). The
        // pre-#443 leaf-only read fired the all-any ERROR here.
        var ws = buildWorkspace("""
            namespace "smoke.lexcard"
            enum Ccy:
                XAG
                XAU
            type Pay:
                currency Ccy (0..1)
            func F1:
                inputs: pay Pay (1..*)
                output: res boolean (0..1)
                set res: [Ccy -> XAG, Ccy -> XAU] <> pay -> currency
            """);
        assertEquals(List.of(), ws.validationDiagnostics(),
                "both sides are multi upstream — the unmodified <> must draw "
                        + "NO diagnostic");
        // The same carrier shape inside a FUNCTION condition (Copilot #443 R1):
        // the head walk must fall THROUGH the function-condition RCondition
        // ancestor to the RFunction arm — an early return there would drop the
        // (1..*) head and re-arm the list-vs-single fabrication class.
        var wsCondition = buildWorkspace("""
            namespace "smoke.lexcard2"
            enum Ccy:
                XAG
                XAU
            type Pay:
                currency Ccy (0..1)
            func F1:
                inputs: pay Pay (1..*)
                output: res boolean (0..1)
                condition CcyKnown:
                    [Ccy -> XAG, Ccy -> XAU] <> pay -> currency
                set res: True
            """);
        assertEquals(List.of(), wsCondition.validationDiagnostics(),
                "the function-condition carrier must stay head-aware — the "
                        + "RCondition ancestor is not this nav's scope boundary");
    }

    @Test void unresolvable_disguised_nav_still_errors() {
        // PR #443 (the #4-class-(a) wave — negative controls): the clearing
        // must NOT eat a genuine miss. A head matching NOTHING lexically keeps
        // the linker's diagnostic at exact message bytes — in a function body
        // and in a data-type condition alike.
        String[][] cases = {
            {"""
            namespace "smoke.lexneg1"
            func F1:
                output: res number (0..1)
                set res: nosuch -> thing
            """, "Enum 'nosuch' not found"},
            {"""
            namespace "smoke.lexneg2"
            type Leg:
                direction string (0..1)
            type Report:
                leg1 Leg (0..1)
                condition DirectionPresent:
                    nosuch -> direction exists
            """, "Enum 'nosuch' not found"}};
        for (String[] c : cases) {
            RModel model = AstBuilder.buildFromString(c[0], "smoke.rosetta");
            var result = RWorkspace.build(List.of(model));
            var enumErrors = result.linkingDiagnostics().stream()
                    .filter(d -> d.category().name().equals("ENUM_NOT_FOUND"))
                    .toList();
            assertEquals(1, enumErrors.size(),
                    "a genuinely-unresolvable head must keep EXACTLY one "
                            + "ENUM_NOT_FOUND: " + result.linkingDiagnostics());
            assertEquals(c[1], enumErrors.get(0).message(), "the linker's message bytes");
        }
    }

    @Test void dispatch_variant_resolves_against_base_signature() {
        // PR #444 (dispatch-base inheritance): a DISPATCH VARIANT declares only
        // its dispatch parameter — the body resolves against the BASE
        // declaration's signature (upstream getSymbolParentScope builds every
        // Function scope from the base-aware getInputs/getOutput; the base =
        // the same-file, first-in-document-order, operations-empty sibling —
        // RosettaFunctionExtensions.getMainFunction). Locks all three healed
        // faces on one model: the dispatch param resolves (DISPATCH_PARAM), a
        // bare base-input reference resolves (SYMBOL), and a record-member nav
        // on a date-typed base input clears (ENUM — the daycount shape).
        RModel model = AstBuilder.buildFromString("""
            namespace "smoke.dispatch1"
            enum Basis:
                B30
                B360
            func Frac:
                inputs:
                    basis Basis (1..1)
                    endDate date (1..1)
                    scale number (1..1)
                output: res number (0..1)
            func Frac(basis: Basis -> B30):
                alias scaled: scale * 2
                set res: endDate -> year + scaled
            """, "smoke.rosetta");
        var result = RWorkspace.build(List.of(model));
        // The string-built harness has no builtins corpus, so builtin TYPE
        // CALLS (`date`, `number`) emit TYPE_NOT_FOUND here — a harness
        // artifact outside this wave (typing still works via the registry
        // fallback). Assert the wave's own three categories.
        assertTrue(result.linkingDiagnostics().stream()
                .noneMatch(d -> d.category().name().equals("DISPATCH_PARAM_NOT_FOUND")
                        || d.category().name().equals("SYMBOL_NOT_FOUND")
                        || d.category().name().startsWith("ENUM_")),
                "the variant body resolves fully against the base signature: "
                        + result.linkingDiagnostics());
        var variant = AstWalker.findAll(model,
                com.regnosys.rosetta.ast.functions.RFunction.class).stream()
                .filter(f -> f.dispatch().isPresent()).findFirst().orElseThrow();
        assertTrue(variant.dispatch().get().parameter().isPresent(),
                "the dispatch param binds the BASE's input attribute");
        assertEquals("basis", variant.dispatch().get().parameter().get().name());
        // The negative twin (the cdm cells' own tests/ fixture shape): a param
        // the base does NOT declare keeps the diagnostic at exact bytes —
        // upstream's scope is the base's inputs, so it diagnoses this too.
        RModel negModel = AstBuilder.buildFromString("""
            namespace "smoke.dispatch2"
            enum Basis:
                B30
            func Frac:
                inputs: other number (1..1)
                output: res number (0..1)
            func Frac(basis: Basis -> B30):
                set res: 1
            """, "smoke.rosetta");
        var negResult = RWorkspace.build(List.of(negModel));
        var dispatchErrors = negResult.linkingDiagnostics().stream()
                .filter(d -> d.category().name().equals("DISPATCH_PARAM_NOT_FOUND"))
                .toList();
        assertEquals(1, dispatchErrors.size(),
                "a param absent from the base keeps EXACTLY one diagnostic: "
                        + negResult.linkingDiagnostics());
        assertEquals("Dispatch parameter 'basis' not found",
                dispatchErrors.get(0).message(), "the linker's message bytes");
        // The replace-not-merge face (Seat-1 #444 MF-3): upstream's
        // getInputs/getOutput IGNORE a variant's own input/output declarations
        // whenever a base exists (RosettaFunctionExtensions:69-82 return the
        // base's alone) — a reference to a variant-own input must stay
        // unresolved exactly as upstream leaves it, while base inputs resolve.
        RModel ownModel = AstBuilder.buildFromString("""
            namespace "smoke.dispatch4"
            enum Basis:
                B30
            func Frac:
                inputs:
                    basis Basis (1..1)
                    real number (1..1)
                output: res number (0..1)
            func Frac(basis: Basis -> B30):
                inputs: own number (1..1)
                set res: own + real
            """, "smoke.rosetta");
        var ownResult = RWorkspace.build(List.of(ownModel));
        var symbolErrors = ownResult.linkingDiagnostics().stream()
                .filter(d -> d.category().name().equals("SYMBOL_NOT_FOUND"))
                .toList();
        assertEquals(1, symbolErrors.size(),
                "the variant-own input is OUTSIDE upstream's scope — exactly "
                        + "one unresolved symbol (`real` resolves via the base): "
                        + ownResult.linkingDiagnostics());
        assertEquals("Symbol 'own' not found", symbolErrors.get(0).message(),
                "the linker's message bytes");
    }

    @Test void record_member_navs_clear_without_enum_diagnostics() {
        // PR #444 (record-member leaves, CLEARING-ONLY): a disguised nav whose
        // head types as a builtin RECORD (date / zonedDateTime) and whose leaf
        // is one of its features carries NO enum diagnostic — upstream types
        // record features via RRecordFeature and emits nothing. The node stays
        // deliberately UNBOUND (record features are not RAttributes; nothing
        // exposes new types — the #443 typing-exposure law). Both channel
        // contexts: a plain function INPUT head and a data-type CONDITION head
        // (the drr `reportingTimestamp -> date` census shape).
        String[] sources = {
            """
            namespace "smoke.record1"
            func F1:
                inputs: endDate date (1..1)
                output: res int (0..1)
                set res: endDate -> year
            """,
            """
            namespace "smoke.record2"
            type Report:
                reportingTimestamp zonedDateTime (0..1)
                condition TsHasDate:
                    reportingTimestamp -> date exists
            """};
        for (String source : sources) {
            RModel model = AstBuilder.buildFromString(source, "smoke.rosetta");
            var result = RWorkspace.build(List.of(model));
            assertTrue(result.linkingDiagnostics().stream()
                    .noneMatch(d -> d.category().name().startsWith("ENUM_")),
                    "a record-member nav must carry NO enum diagnostic: "
                            + result.linkingDiagnostics());
            var navs = AstWalker.findAll(model,
                    com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class);
            assertFalse(navs.isEmpty(), "the disguised-nav parse shape is the premise");
            for (var nav : navs) {
                assertTrue(nav.resolvedInputFeature().isEmpty(),
                        "the record arm is clearing-only — record features are "
                                + "not RAttributes, nothing binds");
            }
        }
        // Negative: a non-feature leaf on a record-typed head keeps the
        // diagnostic at exact message bytes (`zonedDateTime` has no `day`).
        RModel negModel = AstBuilder.buildFromString("""
            namespace "smoke.record3"
            func F1:
                inputs: ts zonedDateTime (1..1)
                output: res int (0..1)
                set res: ts -> day
            """, "smoke.rosetta");
        var negResult = RWorkspace.build(List.of(negModel));
        var enumErrors = negResult.linkingDiagnostics().stream()
                .filter(d -> d.category().name().equals("ENUM_NOT_FOUND"))
                .toList();
        assertEquals(1, enumErrors.size(),
                "a non-member leaf must keep EXACTLY one ENUM_NOT_FOUND: "
                        + negResult.linkingDiagnostics());
        assertEquals("Enum 'ts' not found", enumErrors.get(0).message(),
                "the linker's message bytes");
    }

    @Test void output_headed_navs_clear_without_enum_diagnostics() {
        // PR #444 (output-headed navs, then CLEARING-ONLY); PR #449 BIND-MODE:
        // `<output> -> <feature>` parses as the same disguised REnumValueRef;
        // the output symbol is in upstream's function body scope exactly like
        // the inputs (POST-condition and operation contexts — see the negative
        // below), so a leaf that resolves on the output's type is a proven
        // false positive. #444 landed the arm clearing-only under the #443
        // typing-exposure law; the #449 bind-mode probe measured the bind
        // byte-neutral over the full 55-param population, so the arm now BINDS
        // exactly like the input channel and the nav TYPES as its leaf (the
        // cascade that heals the corpus's output-bodied alias:MISSING family).
        RModel model = AstBuilder.buildFromString("""
            namespace "smoke.outhead1"
            type Wrapper:
                value number (0..1)
            func F1:
                output: res Wrapper (0..1)
                set res: Wrapper { value: 1 }
                post-condition Positive:
                    res -> value exists
            """, "smoke.rosetta");
        var result = RWorkspace.build(List.of(model));
        assertTrue(result.linkingDiagnostics().stream()
                .noneMatch(d -> d.category().name().startsWith("ENUM_")),
                "an output-headed nav must carry NO enum diagnostic: "
                        + result.linkingDiagnostics());
        var navs = AstWalker.findAll(model,
                com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class)
                .stream().filter(n -> "res".equals(n.enumName())).toList();
        assertEquals(1, navs.size(), "the output-headed disguised-nav parse "
                + "shape is the premise");
        assertTrue(navs.get(0).resolvedInputFeature().isPresent(),
                "the output arm binds its leaf attribute (PR #449)");
        assertFalse(result.workspace().getInferredType(navs.get(0)).isMissing(),
                "the output-headed nav exposes its leaf's type (PR #449)");
        // Negative (upstream's pre-condition exclusion): a non-post condition
        // cannot see the output — upstream filters FUNCTION__OUTPUT descriptors
        // from its scope (RosettaScopeProvider's Condition arm) and DIAGNOSES
        // the reference; the fork's diagnostic must survive the arm.
        RModel preModel = AstBuilder.buildFromString("""
            namespace "smoke.outhead2"
            type Wrapper:
                value number (0..1)
            func F1:
                output: res Wrapper (0..1)
                condition Positive:
                    res -> value exists
                set res: Wrapper { value: 1 }
            """, "smoke.rosetta");
        var preResult = RWorkspace.build(List.of(preModel));
        var preErrors = preResult.linkingDiagnostics().stream()
                .filter(d -> d.category().name().equals("ENUM_NOT_FOUND"))
                .toList();
        assertEquals(1, preErrors.size(),
                "an output read inside a PRE-condition keeps EXACTLY one "
                        + "diagnostic — upstream's scope excludes the output there: "
                        + preResult.linkingDiagnostics());
        assertEquals("Enum 'res' not found", preErrors.get(0).message(),
                "the linker's message bytes");
    }

    @Test void inherited_enum_values_resolve_through_supertype_chain() {
        // PR #444 (inherited enum values): upstream scopes an enum-value
        // reference against getAllEnumValues() — own AND inherited (the DRR
        // reality: PartyIdentifierFormatEnum extends PartyIdentifierFormat2Enum
        // extends LeiIdentifierFormatEnum, with `Lei` two levels up). The
        // global pass's value lookup walks the supertype chain pass-safely
        // (each hop's extends resolved in its declaring file's scope) and
        // BINDS the inherited value.
        RModel model = AstBuilder.buildFromString("""
            namespace "smoke.enuminherit1"
            enum LeiFormat:
                Lei
            enum Format2 extends LeiFormat:
                NaturalPerson
            enum Format extends Format2:
            func F1:
                output: res Format (0..1)
                set res: Format -> Lei
            """, "smoke.rosetta");
        var result = RWorkspace.build(List.of(model));
        assertTrue(result.linkingDiagnostics().stream()
                .noneMatch(d -> d.category().name().equals("ENUM_VALUE_NOT_FOUND")),
                "a value declared on a transitive super-enum resolves: "
                        + result.linkingDiagnostics());
        var evr = AstWalker.findAll(model,
                com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class)
                .stream().filter(n -> "Format".equals(n.enumName())).findFirst().orElseThrow();
        assertTrue(evr.enumValue().isPresent(), "the inherited value BINDS");
        assertEquals("Lei", evr.enumValue().get().name());
        assertFalse(result.workspace().getInferredType(evr).isMissing(),
                "the resolved reference types as the enumeration");
        // Negative: a value on NO enum in the chain keeps the diagnostic at
        // exact message bytes (named for the enum the reference bound).
        RModel negModel = AstBuilder.buildFromString("""
            namespace "smoke.enuminherit2"
            enum LeiFormat:
                Lei
            enum Format extends LeiFormat:
            func F1:
                output: res Format (0..1)
                set res: Format -> NoSuchValue
            """, "smoke.rosetta");
        var negResult = RWorkspace.build(List.of(negModel));
        var valueErrors = negResult.linkingDiagnostics().stream()
                .filter(d -> d.category().name().equals("ENUM_VALUE_NOT_FOUND"))
                .toList();
        assertEquals(1, valueErrors.size(),
                "a genuine value miss must keep EXACTLY one diagnostic: "
                        + negResult.linkingDiagnostics());
        assertEquals("Enum value 'NoSuchValue' not found in Format",
                valueErrors.get(0).message(), "the linker's message bytes");
    }

    @Test void same_cell_preference_resolves_duplicate_fqns() {
        // PR #445 (same-cell preference): a merged multi-closure workspace
        // (the V1 gate's two-cell cdm population) carries duplicate FQNs and
        // namespace-LAYOUT drift between cells. With cell roots supplied,
        // resolution prefers same-file then same-cell candidates — each
        // cell's references resolve exactly as upstream's isolated per-cell
        // build would — while a name found only in the OTHER cell still
        // resolves (the lenient fallback: the preference never creates a new
        // failure).
        RModel aShared = AstBuilder.buildFromString("""
            namespace "smoke.shared"
            type T:
                a int (1..1)
            type OnlyA:
                x int (1..1)
            """, "cellA/shared.rosetta");
        RModel aOld = AstBuilder.buildFromString("""
            namespace "smoke.nsold"
            type D:
                olda int (1..1)
            """, "cellA/nsold.rosetta");
        RModel bShared = AstBuilder.buildFromString("""
            namespace "smoke.shared"
            type T:
                b int (1..1)
            """, "cellB/shared.rosetta");
        RModel bNew = AstBuilder.buildFromString("""
            namespace "smoke.nsnew"
            type D:
                newb int (1..1)
            """, "cellB/nsnew.rosetta");
        RModel aUse = AstBuilder.buildFromString("""
            namespace "smoke.usea"
            import smoke.shared.*
            func FA:
                inputs:
                    t T (1..1)
                output: res int (1..1)
                set res: t -> a
            """, "cellA/use.rosetta");
        // the drift face: the c-B file imports the OLD-layout namespace FIRST
        // (upstream's cell-B build finds no D there and falls through; the
        // pre-#445 merged-space walk bound cell-A's D through it), plus the
        // cross-cell fallback face (OnlyA exists in cell A alone).
        RModel bUse = AstBuilder.buildFromString("""
            namespace "smoke.useb"
            import smoke.nsold.*
            import smoke.nsnew.*
            import smoke.shared.*
            func FB:
                inputs:
                    t T (1..1)
                    d D (1..1)
                    o OnlyA (1..1)
                output: res int (1..1)
                set res: t -> b + d -> newb + o -> x
            """, "cellB/use.rosetta");
        var result = RWorkspace.build(
                List.of(aShared, aOld, bShared, bNew, aUse, bUse),
                List.of(java.nio.file.Path.of("cellA"), java.nio.file.Path.of("cellB")));
        assertTrue(result.linkingDiagnostics().stream()
                .noneMatch(d -> d.category().name().equals("TYPE_NOT_FOUND")
                        && !"int".equals(d.unresolvedName())),
                "every model type resolves (incl. the cross-cell fallback; the "
                        + "builtin `int` is absent from this fixture workspace): "
                        + result.linkingDiagnostics());
        assertTrue(result.linkingDiagnostics().stream()
                .noneMatch(d -> d.category().name().equals("ENUM_NOT_FOUND")),
                "each cell's navs resolve against its OWN cell's declarations: "
                        + result.linkingDiagnostics());
    }

    @Test void same_file_preference_without_cell_roots() {
        // PR #445: with NO cell roots (the plain build overload), pass 1 of
        // the two-pass lookup degenerates to the same-FILE test — Xtext's
        // local-scope-shadows-global. Each file's own duplicate-FQN
        // declaration wins for that file's references; a third file keeps
        // the historical first-registered pick.
        RModel fileA = AstBuilder.buildFromString("""
            namespace "smoke.samefile"
            type S:
                a int (1..1)
            func FA:
                inputs:
                    s S (1..1)
                output: res int (1..1)
                set res: s -> a
            """, "fileA.rosetta");
        RModel fileB = AstBuilder.buildFromString("""
            namespace "smoke.samefile"
            type S:
                b int (1..1)
            func FB:
                inputs:
                    s S (1..1)
                output: res int (1..1)
                set res: s -> b
            """, "fileB.rosetta");
        RModel fileC = AstBuilder.buildFromString("""
            namespace "smoke.samefile"
            func FC:
                inputs:
                    s S (1..1)
                output: res int (1..1)
                set res: s -> a
            """, "fileC.rosetta");
        var result = RWorkspace.build(List.of(fileA, fileB, fileC));
        assertTrue(result.linkingDiagnostics().stream()
                .noneMatch(d -> d.category().name().equals("ENUM_NOT_FOUND")),
                "fileB's own S shadows fileA's for fileB's references; fileC keeps "
                        + "the first-registered pick (fileA's S carries `a`): "
                        + result.linkingDiagnostics());
    }

    @Test void deep_feature_cardinality_is_receiver_aware() {
        // PR #445: the faithful (thenAware) cardinality path takes upstream's
        // deep-feature rule — CardinalityProvider.caseDeepFeatureCall is
        // isFeatureMulti(feature) || isMulti(receiver) (vendored :361-366).
        // The c6 carrier shape: `instrument ->> instrumentType` over a
        // (1..1) choice input is SINGLE, so `<alias> = <enum value>` draws
        // NOTHING (the fork's blanket deep-always-MULTI armed the #437
        // all-any check on exactly this shape once same-cell preference
        // resolved the c6 choice).
        var ws = buildWorkspace("""
            namespace "smoke.deep1"
            enum E:
                V
            type Opt1:
                f E (1..1)
            choice Ch:
                Opt1
            func F:
                inputs:
                    c Ch (1..1)
                output: res boolean (1..1)
                alias v: c ->> f
                set res: v = E -> V
            """);
        assertEquals(List.of(), ws.validationDiagnostics().stream()
                .filter(d -> d.issueCode().name().equals("CARDINALITY_ERROR"))
                .toList(),
                "single ->> single = SINGLE: the all-any check must not fire");
        // Negative: a MULTI receiver makes the deep feature MULTI (upstream's
        // receiver OR) — the all-any check fires at exact message bytes.
        var wsNeg = buildWorkspace("""
            namespace "smoke.deep2"
            enum E:
                V
            type Opt1:
                f E (1..1)
            choice Ch:
                Opt1
            func F:
                inputs:
                    cs Ch (1..*)
                output: res boolean (1..1)
                alias v: cs ->> f
                set res: v = E -> V
            """);
        var cardErrors = wsNeg.validationDiagnostics().stream()
                .filter(d -> d.issueCode().name().equals("CARDINALITY_ERROR"))
                .toList();
        assertEquals(1, cardErrors.size(),
                "a multi-receiver deep feature is MULTI — exactly one all-any error: "
                        + wsNeg.validationDiagnostics());
        assertEquals("Operator `=` should specify `all` or `any` when comparing a "
                + "list to a single value", cardErrors.get(0).message());
    }

    @Test void external_class_admits_choice_and_type_shadowed_names() {
        // PR #445: an external synonym source's class position is a TYPE
        // position. Upstream admits CHOICE declarations (choice-as-data —
        // CDM 6's Payout/Product/Underlier/Asset carry external synonyms)
        // and resolves type/function name shadowing to the TYPE
        // (CalculationPeriod / ReturnAmount / DeliveryAmount); its builds
        // are error-free over every corpus carrier. The func is declared
        // BEFORE the type so the generic first-registered lookup lands the
        // function — the type-position re-resolve is what this locks.
        RModel model = AstBuilder.buildFromString("""
            namespace "smoke.ext1"
            type Base:
                x int (1..1)
            choice Ch:
                Base
            func Shadow:
                output: res int (1..1)
                set res: 1
            type Shadow:
                y int (1..1)
            synonym source Src
            {
                Ch:
                    + Base
                        [value "b"]
                Shadow:
                    + y
                        [value "y"]
            }
            """, "smoke.rosetta");
        var result = RWorkspace.build(List.of(model));
        assertTrue(result.linkingDiagnostics().stream()
                .noneMatch(d -> d.category().name().equals("EXTERNAL_TYPE_NOT_FOUND")),
                "the choice and the func-shadowed type both resolve: "
                        + result.linkingDiagnostics());
        var externals = AstWalker.findAll(model,
                com.regnosys.rosetta.ast.external.RExternalClass.class);
        var ch = externals.stream().filter(e -> "Ch".equals(e.typeName()))
                .findFirst().orElseThrow();
        assertTrue(ch.referencedChoice().isPresent(), "the choice binds the parallel field");
        assertTrue(ch.referencedType().isEmpty(), "the data-type field stays empty for a choice");
        var shadow = externals.stream().filter(e -> "Shadow".equals(e.typeName()))
                .findFirst().orElseThrow();
        assertTrue(shadow.referencedType().isPresent(), "the TYPE wins the type position");
        // Negative: a genuinely-missing external class keeps the diagnostic
        // at exact message bytes.
        RModel negModel = AstBuilder.buildFromString("""
            namespace "smoke.ext2"
            synonym source Src
            {
                Missing:
                    + x
                        [value "x"]
            }
            """, "smoke.rosetta");
        var negResult = RWorkspace.build(List.of(negModel));
        var extErrors = negResult.linkingDiagnostics().stream()
                .filter(d -> d.category().name().equals("EXTERNAL_TYPE_NOT_FOUND"))
                .toList();
        assertEquals(1, extErrors.size(), "exactly one diagnostic: "
                + negResult.linkingDiagnostics());
        assertEquals("External class type 'Missing' not found", extErrors.get(0).message());
    }

    @Test void choice_option_navs_clear_without_enum_diagnostics() {
        // PR #446 (choice-option leaves, then CLEARING-ONLY); PR #449
        // BIND-MODE: a disguised nav whose head types as a CHOICE and whose
        // leaf names one of its OPTIONS carries NO enum diagnostic —
        // upstream's feature scope on a choice receiver IS its
        // options-as-attributes (RChoiceType.asRDataType: ChoiceOption
        // extends Attribute, name = the written type-reference text,
        // cardinality (0..1)), so the nav is plain attribute navigation
        // upstream and it emits nothing. #446 landed these channels
        // clearing-only under the #443 typing-exposure law (the engine's T10
        // arms bound resolvedChoiceOption in ITEM-headed contexts only); the
        // #449 bind-mode probe measured the bind byte-neutral over the full
        // 55-param population, so the channels now BIND the matched option
        // and the nav TYPES as it (the cascade that heals the corpus's
        // choice-bodied alias:MISSING family). All three measured channel
        // contexts: a function INPUT head (`pick -> OptionA`), a data-type
        // CONDITION head (the census's dominant 66-row shape), and an ALIAS
        // head (the census's 45-row shape).
        String choicePrelude = """
            type OptionA:
                fieldA number (0..1)
            type OptionB:
                fieldB number (0..1)
            choice Pick:
                OptionA
                OptionB
            """;
        String[] sources = {
            """
            namespace "smoke.choiceopt1"
            """ + choicePrelude + """
            func F1:
                inputs: pick Pick (1..1)
                output: res boolean (0..1)
                set res: pick -> OptionA exists
            """,
            """
            namespace "smoke.choiceopt2"
            """ + choicePrelude + """
            type Holder:
                pick Pick (0..1)
                condition HasA:
                    pick -> OptionA exists
            """,
            """
            namespace "smoke.choiceopt3"
            """ + choicePrelude + """
            type Holder:
                pick Pick (0..1)
            func F2:
                inputs: h Holder (1..1)
                output: res boolean (0..1)
                alias thePick: h -> pick
                set res: thePick -> OptionB exists
            """};
        for (String source : sources) {
            RModel model = AstBuilder.buildFromString(source, "smoke.rosetta");
            var result = RWorkspace.build(List.of(model));
            assertTrue(result.linkingDiagnostics().stream()
                    .noneMatch(d -> d.category().name().startsWith("ENUM_")),
                    "a choice-option nav must carry NO enum diagnostic: "
                            + result.linkingDiagnostics());
            var optionNavs = AstWalker.findAll(model,
                    com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class)
                    .stream()
                    .filter(n -> n.valueName() != null && n.valueName().startsWith("Option"))
                    .toList();
            assertFalse(optionNavs.isEmpty(),
                    "the option-leaf disguised-nav parse shape is the premise");
            // (fixture 3's `h -> pick` nav legitimately BINDS via the #443
            // input channel — pick IS an attribute on Holder; the OPTION-leaf
            // navs carry this wave's bind contract)
            for (var nav : optionNavs) {
                assertTrue(nav.resolvedChoiceOption().isPresent(),
                        "the choice-option arm binds the matched option at "
                                + "every channel (PR #449): " + nav.enumName());
                assertFalse(result.workspace().getInferredType(nav).isMissing(),
                        "the choice-option nav exposes the option's type "
                                + "(PR #449): " + nav.enumName());
            }
        }
        // Negative: a leaf that names NO option on the choice-typed head keeps
        // the diagnostic at exact message bytes (`Pick` has no `OptionC`).
        RModel negModel = AstBuilder.buildFromString("""
            namespace "smoke.choiceopt4"
            """ + choicePrelude + """
            func F1:
                inputs: pick Pick (1..1)
                output: res boolean (0..1)
                set res: pick -> OptionC exists
            """, "smoke.rosetta");
        var negResult = RWorkspace.build(List.of(negModel));
        var enumErrors = negResult.linkingDiagnostics().stream()
                .filter(d -> d.category().name().equals("ENUM_NOT_FOUND"))
                .toList();
        assertEquals(1, enumErrors.size(),
                "a non-option leaf must keep EXACTLY one ENUM_NOT_FOUND: "
                        + negResult.linkingDiagnostics());
        assertEquals("Enum 'pick' not found", enumErrors.get(0).message(),
                "the linker's message bytes");
    }

    @Test void choice_and_output_bound_navs_cascade_through_aliases() {
        // PR #449 (the clearing-to-BIND wave's cascade contract): the corpus's
        // whole alias:MISSING family (25 cdm + 6 drr rows) was aliases whose
        // BODIES were choice-option navs (#446, cleared-not-typed) or
        // output-headed nav chains (#444, cleared-not-typed) — the alias typed
        // MISSING, so every consumer nav heading the alias failed. The #449
        // binds make the bodies TYPE, and the consumers then bind via the
        // #447 alias arm in the same fixed point.
        //
        // Fixture A — the cdm6 InterestCashSettlementAmount shape: an alias
        // whose body is a choice-option nav; its consumer binds + types.
        RModel choiceModel = AstBuilder.buildFromString("""
            namespace "smoke.bindcascade1"
            type OptionA:
                fieldA number (0..1)
            type OptionB:
                fieldB number (0..1)
            choice Pick:
                OptionA
                OptionB
            func F1:
                inputs: pick Pick (1..1)
                output: res number (0..1)
                alias theA: pick -> OptionA
                set res: theA -> fieldA
            """, "smoke.rosetta");
        var choiceResult = RWorkspace.build(List.of(choiceModel));
        assertTrue(choiceResult.linkingDiagnostics().stream()
                .noneMatch(d -> d.category().name().startsWith("ENUM_")),
                "the choice-bodied alias cascade must carry NO enum "
                        + "diagnostic: " + choiceResult.linkingDiagnostics());
        var bodyNav = AstWalker.findAll(choiceModel,
                com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class)
                .stream().filter(n -> "OptionA".equals(n.valueName())).toList();
        assertEquals(1, bodyNav.size(), "the alias-body option nav parses "
                + "disguised — the premise");
        assertTrue(bodyNav.get(0).resolvedChoiceOption().isPresent(),
                "the alias BODY binds its option (the #449 choice bind)");
        var consumerNav = AstWalker.findAll(choiceModel,
                com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class)
                .stream().filter(n -> "theA".equals(n.enumName())).toList();
        assertEquals(1, consumerNav.size(), "the alias-consumer nav parses "
                + "disguised — the premise");
        assertTrue(consumerNav.get(0).resolvedInputFeature().isPresent(),
                "the CONSUMER binds via the #447 alias arm once the body "
                        + "types — the cascade");
        assertFalse(choiceResult.workspace()
                .getInferredType(consumerNav.get(0)).isMissing(),
                "the consumer exposes the leaf's type");
        // Fixture B — the cdm5 NewEquitySwapProduct shape: an alias whose
        // body is an output-headed nav CHAIN; its post-condition consumer
        // binds + types (the #444 output arm now types the disguised head,
        // the chain follows, the alias types, the consumer cascades).
        RModel outModel = AstBuilder.buildFromString("""
            namespace "smoke.bindcascade2"
            type Inner:
                leaf number (0..1)
            type Mid:
                inner Inner (0..1)
            type Outer:
                mid Mid (0..1)
            func F2:
                output: outR Outer (1..1)
                alias theInner: outR -> mid -> inner
                set outR: Outer { mid: empty }
                post-condition P:
                    theInner -> leaf exists
            """, "smoke.rosetta");
        var outResult = RWorkspace.build(List.of(outModel));
        assertTrue(outResult.linkingDiagnostics().stream()
                .noneMatch(d -> d.category().name().startsWith("ENUM_")),
                "the output-chained alias cascade must carry NO enum "
                        + "diagnostic: " + outResult.linkingDiagnostics());
        var outConsumer = AstWalker.findAll(outModel,
                com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class)
                .stream().filter(n -> "theInner".equals(n.enumName())).toList();
        assertEquals(1, outConsumer.size(), "the alias-consumer nav parses "
                + "disguised — the premise");
        assertTrue(outConsumer.get(0).resolvedInputFeature().isPresent(),
                "the post-condition consumer binds once the output-headed "
                        + "alias body types — the cascade");
        assertFalse(outResult.workspace()
                .getInferredType(outConsumer.get(0)).isMissing(),
                "the consumer exposes the leaf's type");
        // Negative twin — a NON-option alias body stays MISSING and the
        // cascade must NOT over-heal: the body keeps the linker's diagnostic
        // at exact bytes AND the consumer keeps its own (nothing binds on
        // either side). OptionC is a declared type that is NOT an option of
        // Pick, so both witness tokens are removable only by widening the
        // admission — which the bind must never do.
        RModel negModel = AstBuilder.buildFromString("""
            namespace "smoke.bindcascade3"
            type OptionA:
                fieldA number (0..1)
            type OptionC:
                fieldC number (0..1)
            choice Pick:
                OptionA
            func F3:
                inputs: pick Pick (1..1)
                output: res number (0..1)
                alias theC: pick -> OptionC
                set res: theC -> fieldC
            """, "smoke.rosetta");
        var negResult = RWorkspace.build(List.of(negModel));
        var negErrors = negResult.linkingDiagnostics().stream()
                .filter(d -> d.category().name().equals("ENUM_NOT_FOUND"))
                .map(d -> d.message())
                .sorted()
                .toList();
        assertEquals(List.of("Enum 'pick' not found", "Enum 'theC' not found"),
                negErrors,
                "the non-option body and its consumer each keep EXACTLY one "
                        + "diagnostic at the linker's exact bytes: "
                        + negResult.linkingDiagnostics());
        for (var nav : AstWalker.findAll(negModel,
                com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class)) {
            assertTrue(nav.resolvedChoiceOption().isEmpty()
                            && nav.resolvedInputFeature().isEmpty(),
                    "nothing binds on the declined pair: " + nav.enumName());
        }
    }

    @Test void bare_enum_values_bind_by_expected_position() {
        // PR #448 (Cat 16 — the bare-enum-value expected-type family), flipped
        // clearing-only → BIND at PR #452 (the recorded typed follow-on, the
        // #447/#449 contract-flip law — this lock REWRITES the #448 clearing
        // lock): a bare name in an expected-ENUM position resolves to the
        // position enum's value and carries NO symbol diagnostic — upstream's
        // ExpectedTypeProvider computes the position's expected type
        // (constructor value → the key attribute; call argument → the
        // callee's declared input at the index; conditional arms / lambda
        // bodies / transparent unaries flow the container's expected type
        // down) and RosettaScopeProvider admits the expected enum's values as
        // implicit features (parent-wins ReversedSimpleScope — the values are
        // the LAST-RESORT fallback), so the reference BINDS to the value
        // upstream's scope provides. The #452 wave lands the bind WITH its
        // two measured generator seats (the getOrDefault-arg hoist gate's
        // bound-arm admission + the ctor-value/default-RHS child
        // requalification — facet cat16BindEnumSeats, probe-measured cp
        // 55/55 byte-equal to the #437 SOT). Four measured position shapes:
        // the direct constructor value, the call argument, the
        // nested-conditional-through-extract-lambda chain to an enum-typed
        // output (the drr GetContractType shape), and a super-enum-declared
        // value admitted through the expected CHILD enum (the
        // AssetIdTypeEnum extends ProductIdTypeEnum shape — the bound
        // instance is the PARENT-declared value, asserted below).
        String prelude = """
            enum Colour:
                Red
                Blue
            type Holder:
                colour Colour (0..1)
            """;
        String[] sources = {
            """
            namespace "smoke.cat16a"
            """ + prelude + """
            func F1:
                inputs: x number (0..1)
                output: h Holder (0..1)
                set h:
                    Holder { colour: Red }
            """,
            """
            namespace "smoke.cat16b"
            """ + prelude + """
            func Callee:
                inputs:
                    a number (0..1)
                    c Colour (0..1)
                output: res number (0..1)
                set res: a
            func Caller:
                inputs: x number (0..1)
                output: res number (0..1)
                set res: Callee(x, Blue)
            """,
            """
            namespace "smoke.cat16c"
            """ + prelude + """
            func Pipe:
                inputs: xs number (1..*)
                output: colour Colour (0..1)
                set colour:
                    xs first then extract
                        if item = 1
                        then Red
                        else Blue
            """,
            """
            namespace "smoke.cat16d"
            enum Parent:
                Other
            enum Child extends Parent:
                Extra
            type ChildHolder:
                kind Child (0..1)
            func F2:
                inputs: x number (0..1)
                output: h ChildHolder (0..1)
                set h:
                    ChildHolder { kind: Other }
            """};
        for (String source : sources) {
            RModel model = AstBuilder.buildFromString(source, "smoke.rosetta");
            var result = RWorkspace.build(List.of(model));
            assertTrue(result.linkingDiagnostics().stream()
                    .noneMatch(d -> d.category().name().equals("SYMBOL_NOT_FOUND")),
                    "a bare enum value in an expected-enum position must carry "
                            + "NO symbol diagnostic: " + result.linkingDiagnostics());
            // the arm's OWN subjects: every bare enum-value name BINDS to the
            // position enum's REnumValue of the same name (the #452 bind
            // contract — the symbol is the value node, name-matched exactly
            // like upstream's expected-type scope admission)
            var bareValues = AstWalker.findAll(model,
                    com.regnosys.rosetta.ast.expressions.references.RSymbolReference.class)
                    .stream()
                    .filter(r -> r.args().isEmpty())
                    .filter(r -> List.of("Red", "Blue", "Other").contains(r.name()))
                    .toList();
            assertFalse(bareValues.isEmpty(),
                    "the bare enum-value parse shape is the premise");
            for (var ref : bareValues) {
                assertTrue(ref.symbol().isPresent()
                                && ref.symbol().get()
                                        instanceof com.regnosys.rosetta.ast.supporting.REnumValue ev
                                && ev.name().equals(ref.name()),
                        "Cat 16 BINDS the position enum's value at '"
                                + ref.name() + "' (PR #452)");
                if ("Other".equals(ref.name())) {
                    // the super-enum admission witness (the cat16d fixture):
                    // the bound instance IS the PARENT-declared value reached
                    // through the expected CHILD enum's extends chain — the
                    // generator seats re-qualify it by the child (the
                    // #211/#358 flatten law), which only works because the
                    // engine binds the parent's instance, not a copy.
                    var ev = (com.regnosys.rosetta.ast.supporting.REnumValue) ref.symbol().get();
                    assertTrue(ev.parent() instanceof com.regnosys.rosetta.ast.types.REnumeration en
                                    && "Parent".equals(en.name()),
                            "the bound value is the PARENT-declared instance");
                }
            }
        }
        // Negative: a bare name that is NOT a value of the position's
        // expected enum keeps EXACTLY one SYMBOL_NOT_FOUND at the linker's
        // exact message bytes.
        RModel negModel = AstBuilder.buildFromString("""
            namespace "smoke.cat16e"
            """ + prelude + """
            func F3:
                inputs: x number (0..1)
                output: h Holder (0..1)
                set h:
                    Holder { colour: Green }
            """, "smoke.rosetta");
        var negResult = RWorkspace.build(List.of(negModel));
        var symbolErrors = negResult.linkingDiagnostics().stream()
                .filter(d -> d.category().name().equals("SYMBOL_NOT_FOUND"))
                .toList();
        assertEquals(1, symbolErrors.size(),
                "a non-value bare name must keep EXACTLY one SYMBOL_NOT_FOUND: "
                        + negResult.linkingDiagnostics());
        assertEquals("Symbol 'Green' not found", symbolErrors.get(0).message(),
                "the linker's message bytes");
    }

    @Test void synonym_source_extends_admits_plain_and_external_forms() {
        // PR #450 (EXT_SRC): upstream's extends cross-ref on an external
        // synonym source is typed [RosettaSynonymSource|QualifiedName]
        // (Rosetta.xtext:821) and RosettaExternalSynonymSource EXTENDS
        // RosettaSynonymSource in the Ecore model (Rosetta.xcore:480) — the
        // plain body-less legacy form and the external form are BOTH
        // admissible super-source targets. The corpus witness family: cdm's
        // `synonym source FIS extends FIS_BASE` where FIS_BASE is the plain
        // form declared two lines above (likewise FpML / ORE / CreateiQ —
        // the whole EXTERNAL_SOURCE_NOT_FOUND budget, 7 cdm + 3 drr rows,
        // was exactly the plain-form targets).
        RModel model = AstBuilder.buildFromString("""
            namespace "smoke.extsrc1"
            synonym source PLAIN_BASE
            synonym source MID extends PLAIN_BASE
            {
            }
            synonym source TOP extends MID
            {
            }
            """, "smoke.rosetta");
        var result = RWorkspace.build(List.of(model));
        assertTrue(result.linkingDiagnostics().stream()
                .noneMatch(d -> d.category().name().equals("EXTERNAL_SOURCE_NOT_FOUND")),
                "both super-source forms must resolve: " + result.linkingDiagnostics());
        var externals = AstWalker.findAll(model,
                com.regnosys.rosetta.ast.external.RExternalSynonymSource.class);
        assertEquals(2, externals.size(), "the two extending sources are the premise");
        var mid = externals.stream().filter(e -> "MID".equals(e.name())).findFirst().orElseThrow();
        assertEquals(1, mid.superSources().size(),
                "MID must bind its plain-form super source");
        var midSuper = mid.superSources().get(0);
        assertEquals("PLAIN_BASE", midSuper.name());
        assertFalse(midSuper instanceof com.regnosys.rosetta.ast.external.RExternalSynonymSource,
                "the PLAIN legacy form is the admissible target the pre-#450 "
                        + "admission rejected — it must bind as the plain node");
        var top = externals.stream().filter(e -> "TOP".equals(e.name())).findFirst().orElseThrow();
        assertEquals(1, top.superSources().size(),
                "TOP must bind its external-form super source");
        assertInstanceOf(com.regnosys.rosetta.ast.external.RExternalSynonymSource.class,
                top.superSources().get(0),
                "the external form stays admissible (the pre-#450 contract)");
        assertEquals("MID", top.superSources().get(0).name());
        // Negative: a missing super-source name keeps EXACTLY one
        // EXTERNAL_SOURCE_NOT_FOUND at the linker's exact message bytes.
        RModel negModel = AstBuilder.buildFromString("""
            namespace "smoke.extsrc2"
            synonym source BAD extends NOPE
            {
            }
            """, "smoke.rosetta");
        var negResult = RWorkspace.build(List.of(negModel));
        var extErrors = negResult.linkingDiagnostics().stream()
                .filter(d -> d.category().name().equals("EXTERNAL_SOURCE_NOT_FOUND"))
                .toList();
        assertEquals(1, extErrors.size(),
                "a missing super source must keep EXACTLY one EXTERNAL_SOURCE_NOT_FOUND: "
                        + negResult.linkingDiagnostics());
        assertEquals("External source 'NOPE' not found", extErrors.get(0).message(),
                "the linker's message bytes");
    }

    @Test void rule_reference_binds_rule_kind_over_type_shadow() {
        // PR #450 (RULE): a ruleReference position is a RULE position —
        // upstream's grammar types the cross-ref [RosettaRule|QualifiedName]
        // (Rosetta.xtext:906), so its scope candidates are rules ONLY and a
        // same-named type is never admissible (the D40 type-vs-rule
        // invariant's ruleReference seat). The witness mechanism (iosco
        // `payment.OtherPayment`): the requester wildcard-imports the BASE
        // namespace family FIRST — whose `payment` sub-namespace holds only
        // the same-named TYPE — and the version family holding the RULE
        // LATER, so the kind-blind wildcard walk returned the type at its
        // first hit and errored; the rule-kind-filtered ladder empties that
        // first hit and continues to upstream's target.
        RModel base = AstBuilder.buildFromString("""
            namespace "smoke.rulekind.base.payment"
            type OtherPayment:
                amount int (0..1)
            """, "base-payment-type.rosetta");
        RModel ver = AstBuilder.buildFromString("""
            namespace "smoke.rulekind.ver.payment"
            reporting rule OtherPayment from int:
                item
            """, "ver-payment-rule.rosetta");
        RModel req = AstBuilder.buildFromString("""
            namespace "smoke.rulekind.ver"
            import smoke.rulekind.base.*
            import smoke.rulekind.ver.*
            type Cde:
                otherPayment payment.OtherPayment (0..*)
                    [ruleReference payment.OtherPayment]
            """, "ver-type.rosetta");
        var result = RWorkspace.build(List.of(base, ver, req));
        assertTrue(result.linkingDiagnostics().stream()
                .noneMatch(d -> d.category().name().equals("RULE_NOT_FOUND")),
                "the rule-kind target must resolve: " + result.linkingDiagnostics());
        assertTrue(result.linkingDiagnostics().stream()
                .noneMatch(d -> d.category().name().equals("TYPE_NOT_FOUND")
                        && "payment.OtherPayment".equals(d.unresolvedName())),
                "the attribute's type call resolves the TYPE at the same name "
                        + "(builtin `int` noise aside — smoke workspaces carry "
                        + "no builtin namespace): " + result.linkingDiagnostics());
        var ruleRefs = AstWalker.findAll(req,
                com.regnosys.rosetta.ast.annotations.RRuleReferenceAnnotation.class);
        assertEquals(1, ruleRefs.size(), "the ruleReference parse shape is the premise");
        var bound = ruleRefs.get(0).rule().orElseThrow(() ->
                new AssertionError("the ruleReference must BIND its rule"));
        assertEquals("OtherPayment", bound.name());
        assertTrue(bound.sourceRange().file().contains("ver-payment-rule"),
                "the bound target must be the RULE declaration (the later "
                        + "import), never the same-named type the kind-blind "
                        + "walk hit first — bound from: " + bound.sourceRange().file());
        // Negative: a missing rule name keeps EXACTLY one RULE_NOT_FOUND at
        // the linker's exact message bytes (the kind-filtered fallback must
        // never manufacture a binding).
        RModel negReq = AstBuilder.buildFromString("""
            namespace "smoke.rulekind2"
            type Cde:
                amount int (0..1)
                    [ruleReference payment.Nonexistent]
            """, "smoke.rosetta");
        var negResult = RWorkspace.build(List.of(negReq));
        var ruleErrors = negResult.linkingDiagnostics().stream()
                .filter(d -> d.category().name().equals("RULE_NOT_FOUND"))
                .toList();
        assertEquals(1, ruleErrors.size(),
                "a missing rule must keep EXACTLY one RULE_NOT_FOUND: "
                        + negResult.linkingDiagnostics());
        assertEquals("Rule 'payment.Nonexistent' not found", ruleErrors.get(0).message(),
                "the linker's message bytes");
    }

    @Test void switch_case_item_narrows_to_guard_type() {
        // PR #451 (switch-case item narrowing): a non-default case with a
        // DATA (or choice-option) guard DEFINES the implicit variable as the
        // GUARD's type — upstream ImplicitVariableUtil
        // .findContainerDefiningImplicitVariable stops at the case and
        // safeTypeOfImplicitVariable returns the guard type — so a bare ref
        // (Cat 9) and a disguised nav (Cat 10) inside the case body resolve
        // as features of the NARROWED type (the ingest-fpml
        // `fpmlProduct switch fpml.CreditDefaultSwap then feeLeg -> ...`
        // census family, 13 ENUM + 45 SYMBOL rows). The guard resolves at
        // the linker's kind-gated ladder (resolveSwitchGuardType).
        RModel model = AstBuilder.buildFromString("""
            namespace "smoke.swnarrow1"
            type Inner:
                leaf string (0..1)
            type Base:
                common string (0..1)
            type Sub extends Base:
                subOnly string (0..1)
                inner Inner (0..1)
            func F:
                inputs: b Base (1..1)
                output: result string (0..1)
                set result:
                    b switch
                        Sub then subOnly,
                        default empty
            func G:
                inputs: b Base (1..1)
                output: result string (0..1)
                set result:
                    b switch
                        Sub then inner -> leaf,
                        default empty
            """, "smoke.rosetta");
        var result = RWorkspace.build(List.of(model));
        assertTrue(result.linkingDiagnostics().stream()
                .noneMatch(d -> d.category().name().equals("SYMBOL_NOT_FOUND")
                        || d.category().name().startsWith("ENUM_")),
                "case-body refs must resolve against the narrowed guard type: "
                        + result.linkingDiagnostics());
        // The bare ref BINDS the narrowed item's attribute (the ordinary
        // Cat 9 contract riding the narrowed item type).
        var bareRefs = AstWalker.findAll(model,
                com.regnosys.rosetta.ast.expressions.references.RSymbolReference.class)
                .stream().filter(r -> "subOnly".equals(r.name())).toList();
        assertEquals(1, bareRefs.size(), "the bare case-body ref is the premise");
        assertTrue(bareRefs.get(0).symbol().isPresent(),
                "Cat 9 binds the narrowed item's attribute");
        // The disguised nav binds the 2-step chain on the narrowed type.
        var navs = AstWalker.findAll(model,
                com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class)
                .stream().filter(n -> "inner".equals(n.enumName())).toList();
        assertEquals(1, navs.size(), "the disguised-nav parse shape is the premise");
        assertTrue(navs.get(0).resolvedAttributeChain().isPresent(),
                "Cat 10's 2-step binds on the narrowed item type");
        // Negative 1: a name on NEITHER the guard type nor its supertypes
        // keeps EXACTLY one SYMBOL_NOT_FOUND at the linker's message bytes.
        // Negative 2: the DEFAULT case defines NO implicit variable
        // (upstream's walk requires a narrowing guard), so the same bare
        // name there also fails — the narrowing never leaks past its case.
        RModel negModel = AstBuilder.buildFromString("""
            namespace "smoke.swnarrow2"
            type Base:
                common string (0..1)
            type Sub extends Base:
                subOnly string (0..1)
            func F:
                inputs: b Base (1..1)
                output: result string (0..1)
                set result:
                    b switch
                        Sub then nope,
                        default empty
            func G:
                inputs: b Base (1..1)
                output: result string (0..1)
                set result:
                    b switch
                        Sub then empty,
                        default subOnly
            """, "smoke.rosetta");
        var negResult = RWorkspace.build(List.of(negModel));
        var symMessages = negResult.linkingDiagnostics().stream()
                .filter(d -> d.category().name().equals("SYMBOL_NOT_FOUND"))
                .map(d -> d.message())
                .sorted()
                .toList();
        // Message SET, not emission order (Copilot #451 R1 — insertion order
        // across linker passes is an implementation detail): the non-feature
        // name keeps its diagnostic at exact bytes, and the default-case ref
        // proves the narrowing never leaks (upstream defines the implicit
        // variable only for guard-narrowing cases).
        assertEquals(List.of("Symbol 'nope' not found", "Symbol 'subOnly' not found"),
                symMessages,
                "the non-feature name and the default-case ref must each keep "
                        + "EXACTLY one SYMBOL_NOT_FOUND at the linker's message "
                        + "bytes: " + negResult.linkingDiagnostics());
    }

    @Test void item_headed_record_and_meta_faces_clear() {
        // PR #451 (record + metadata faces at the item-headed 2-step and the
        // condition channel): `stepDate -> date` on a zonedDateTime ITEM
        // feature is a record-member nav (the #444 contract extended to
        // Cat 10's chain) and `party -> reference` on a [metadata reference]
        // attribute is upstream's meta-face feature admission
        // (getMetaDescriptions — the receiver's meta-annotated type names
        // the face). Both CLEARING-ONLY: no fork AST node backs a record
        // feature or a meta face, nothing binds, the stale tried-enum
        // diagnostics are removed (the census's partyReference ×7 /
        // stepDate-family ×9 / quantityReference ×3 / identifier ×2 rows).
        String[] sources = {
            """
            namespace "smoke.faces1"
            type Step:
                stepDate zonedDateTime (0..1)
                stepValue number (0..1)
            func F:
                inputs: steps Step (0..*)
                output: out date (0..*)
                add out:
                    steps extract stepDate -> date
            """,
            """
            namespace "smoke.faces2"
            type Party:
                name string (0..1)
            type Holder:
                party Party (0..1)
                    [metadata reference]
            func F:
                inputs: hs Holder (0..*)
                output: out Holder (0..*)
                add out:
                    hs filter party -> reference exists
            """,
            """
            namespace "smoke.faces3"
            type Quantity:
                amount number (0..1)
            type Payout:
                quantityReference Quantity (0..1)
                    [metadata reference]
                multiplier number (0..1)
                condition QM:
                    if multiplier exists
                    then quantityReference -> reference exists
            """};
        for (String source : sources) {
            RModel model = AstBuilder.buildFromString(source, "smoke.rosetta");
            var result = RWorkspace.build(List.of(model));
            assertTrue(result.linkingDiagnostics().stream()
                    .noneMatch(d -> d.category().name().startsWith("ENUM_")),
                    "a record/meta-face nav must carry NO enum diagnostic: "
                            + result.linkingDiagnostics());
            var navs = AstWalker.findAll(model,
                    com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class);
            assertFalse(navs.isEmpty(), "the disguised-nav parse shape is the premise");
            for (var nav : navs) {
                assertTrue(nav.resolvedAttributeChain().isEmpty()
                                && nav.resolvedInputFeature().isEmpty()
                                && nav.resolvedChoiceOption().isEmpty(),
                        "the face arms are clearing-only — nothing bindable");
            }
        }
        // Negative: a face the attribute does NOT declare ([metadata
        // reference] admits `reference`, never `scheme`) keeps EXACTLY one
        // ENUM_NOT_FOUND at the linker's message bytes.
        RModel negModel = AstBuilder.buildFromString("""
            namespace "smoke.faces4"
            type Party:
                name string (0..1)
            type Holder:
                party Party (0..1)
                    [metadata reference]
            func F:
                inputs: hs Holder (0..*)
                output: out Holder (0..*)
                add out:
                    hs filter party -> scheme exists
            """, "smoke.rosetta");
        var negResult = RWorkspace.build(List.of(negModel));
        var enumErrors = negResult.linkingDiagnostics().stream()
                .filter(d -> d.category().name().equals("ENUM_NOT_FOUND"))
                .toList();
        assertEquals(1, enumErrors.size(),
                "an undeclared face must keep EXACTLY one ENUM_NOT_FOUND: "
                        + negResult.linkingDiagnostics());
        assertEquals("Enum 'party' not found", enumErrors.get(0).message(),
                "the linker's message bytes");
    }

    @Test void choice_options_resolve_as_item_features() {
        // PR #451 (choice options as implicit-item features): upstream's
        // feature set on a choice receiver IS its options-as-attributes
        // (the asRDataType view — the #446 law), and a
        // `type X extends ChoiceC` data type INHERITS them through the
        // supertype walk (getAllAttributes) — the fork's choice VIEW
        // (choiceViewOfType). Three census shapes: a BARE option name on a
        // choice item (`payouts extract InterestRatePayout exists` — Cat 9,
        // CLEARING-ONLY: no RAttribute backs an option-as-feature), an
        // option-of-option nav on an extends-choice item
        // (`basketConstituent extract [ Asset -> Commodity exists ]` —
        // the L10 arm BINDS through the widened view), and an
        // option-headed nav whose leaf is the option type's own ATTRIBUTE
        // (clearing-only; the chain cannot carry an option head).
        RModel model = AstBuilder.buildFromString("""
            namespace "smoke.chopt2"
            type C1:
                deep string (0..1)
            type C2:
                other string (0..1)
            choice InnerPick:
                C1
                C2
            type A:
                x string (0..1)
            choice Outer:
                InnerPick
                A
            type OuterExt extends Outer:
                extra string (0..1)
            func Q:
                inputs: picks Outer (0..*)
                output: found boolean (1..1)
                set found:
                    picks extract A exists
                    then any = True
            func R:
                inputs: exts OuterExt (0..*)
                output: found boolean (1..1)
                set found:
                    exts extract [ InnerPick -> C1 exists ]
                    then any = True
            func S:
                inputs: picks Outer (0..*)
                output: found boolean (1..1)
                set found:
                    picks extract [ A -> x exists ]
                    then any = True
            """, "smoke.rosetta");
        var result = RWorkspace.build(List.of(model));
        assertTrue(result.linkingDiagnostics().stream()
                .noneMatch(d -> d.category().name().equals("SYMBOL_NOT_FOUND")
                        || d.category().name().startsWith("ENUM_")),
                "option-as-feature refs must resolve: "
                        + result.linkingDiagnostics());
        // CONTROL (Seat-1 #451 MF-1): the bare `A` here is declared in the
        // SAME file, so pass 5 binds it to the global TYPE and no
        // SYMBOL_NOT_FOUND ever fires — the Cat-9 clearing rung is SKIPPED
        // (symbol-empty gate). That resolved-to-global-type render class
        // generates byte-identically today and the wave leaves it untouched;
        // this leg locks exactly that.
        var bare = AstWalker.findAll(model,
                com.regnosys.rosetta.ast.expressions.references.RSymbolReference.class)
                .stream().filter(r -> "A".equals(r.name())).toList();
        assertEquals(1, bare.size(), "the bare option ref is the premise");
        assertTrue(bare.get(0).symbol().isPresent(),
                "the same-file bare option binds to the global type at pass 5 "
                        + "— the untouched control class");
        // The option-of-option nav BINDS through the widened choice view.
        var l10 = AstWalker.findAll(model,
                com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class)
                .stream().filter(n -> "InnerPick".equals(n.enumName())).toList();
        assertEquals(1, l10.size(), "the option-of-option nav is the premise");
        assertTrue(l10.get(0).resolvedChoiceOption().isPresent(),
                "the L10 arm binds the inner option through the extends-choice "
                        + "item's widened view");
        // The option->attribute nav stays clearing-only.
        var optAttr = AstWalker.findAll(model,
                com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class)
                .stream().filter(n -> "A".equals(n.enumName())).toList();
        assertEquals(1, optAttr.size(), "the option->attribute nav is the premise");
        assertTrue(optAttr.get(0).resolvedChoiceOption().isEmpty()
                        && optAttr.get(0).resolvedAttributeChain().isEmpty(),
                "an option-headed attribute leaf is clearing-only — the chain "
                        + "cannot carry an option head");
        // The Cat-9 clearing rung's REAL lock (Seat-1 #451 MF-1): the bare
        // option name must be UNRESOLVABLE as a symbol for the rung to have
        // work — the corpus witness (`InterestRatePayout` declared in
        // cdm.product.asset, which product-qualification-func.rosetta never
        // imports) is reproduced as three namespaces: the option's TYPE
        // lives in a namespace the USE file does not import (the choice's
        // file imports it; the use file imports only the choice's
        // namespace), so pass 5 emits SYMBOL_NOT_FOUND and the rung clears
        // it against the item's option list with the symbol still EMPTY
        // (clearing-only proven — no RAttribute backs an option-as-feature).
        RModel optModel = AstBuilder.buildFromString("""
            namespace "smoke.chopt4.opts"
            type Away:
                z string (0..1)
            """, "smoke-opts.rosetta");
        RModel choiceModel = AstBuilder.buildFromString("""
            namespace "smoke.chopt4.pick"
            import smoke.chopt4.opts.*
            type Near:
                w string (0..1)
            choice Pick:
                Away
                Near
            """, "smoke-pick.rosetta");
        RModel useModel = AstBuilder.buildFromString("""
            namespace "smoke.chopt4.use"
            import smoke.chopt4.pick.*
            func Q:
                inputs: picks Pick (0..*)
                output: found boolean (1..1)
                set found:
                    picks extract Away exists
                    then any = True
            """, "smoke-use.rosetta");
        var crossResult = RWorkspace.build(List.of(optModel, choiceModel, useModel));
        assertTrue(crossResult.linkingDiagnostics().stream()
                .noneMatch(d -> d.category().name().equals("SYMBOL_NOT_FOUND")),
                "the rung must clear the unresolvable bare option: "
                        + crossResult.linkingDiagnostics());
        var awayRefs = AstWalker.findAll(useModel,
                com.regnosys.rosetta.ast.expressions.references.RSymbolReference.class)
                .stream().filter(r -> "Away".equals(r.name())).toList();
        assertEquals(1, awayRefs.size(), "the cross-namespace bare option is the premise");
        assertTrue(awayRefs.get(0).symbol().isEmpty(),
                "the rung is CLEARING-ONLY — the symbol stays empty");
        // Negative: a name that is neither a feature nor an option keeps
        // EXACTLY one SYMBOL_NOT_FOUND at the linker's message bytes.
        RModel negModel = AstBuilder.buildFromString("""
            namespace "smoke.chopt3"
            type A:
                x string (0..1)
            type B:
                y string (0..1)
            choice Pick:
                A
                B
            func Q:
                inputs: picks Pick (0..*)
                output: found boolean (1..1)
                set found:
                    picks extract Nope exists
                    then any = True
            """, "smoke.rosetta");
        var negResult = RWorkspace.build(List.of(negModel));
        var symErrors = negResult.linkingDiagnostics().stream()
                .filter(d -> d.category().name().equals("SYMBOL_NOT_FOUND"))
                .toList();
        assertEquals(1, symErrors.size(),
                "a non-option name must keep EXACTLY one SYMBOL_NOT_FOUND: "
                        + negResult.linkingDiagnostics());
        assertEquals("Symbol 'Nope' not found", symErrors.get(0).message(),
                "the linker's message bytes");
    }

    @Test void alias_self_name_binds_shadowed_item_feature() {
        // PR #453 (facet aliasSelfScopeFilter): upstream filters an alias's
        // OWN NAME from its body's symbol scope (a ShortcutDeclaration removes
        // every same-named descr from its parent scope — vendored
        // RosettaScopeProvider:401-402), so a bare self-name inside the alias
        // body is the shadowed implicit-ITEM feature, never the alias. The
        // witness topology (cdm MapIntent :471 / the three drr reportingSide
        // sites): the extract receiver is a SIBLING alias (the #447
        // typed-alias machinery supplies the item type) and the item type
        // carries the same-named attribute — the pass-5 filter unbinds the
        // old SELF-reference and the engine's existing Cat 9 arm binds the
        // attribute, exactly like the already-clearing sibling branches.
        RModel model = AstBuilder.buildFromString("""
            namespace "smoke.aliasself1"
            type Info:
                flagged boolean (0..1)
            type Trade:
                info Info (0..*)
            func F:
                inputs: t Trade (1..1)
                output: res boolean (0..1)
                alias infos: t -> info
                alias flagged: infos extract flagged = True then only-element
                set res: flagged
            """, "smoke.rosetta");
        var result = RWorkspace.build(List.of(model));
        assertTrue(result.linkingDiagnostics().stream()
                .noneMatch(d -> d.category().name().equals("SYMBOL_NOT_FOUND")),
                "the self-name re-binds via Cat 9 — no symbol diagnostic: "
                        + result.linkingDiagnostics());
        assertEquals(List.of(), result.workspace().validationDiagnostics().stream()
                .filter(d -> d.issueCode().name().equals("CARDINALITY_ERROR"))
                .toList(),
                "the witness's validation class: the single item feature "
                        + "comparison draws no cardinality error");
        var fn = AstWalker.findAll(model,
                com.regnosys.rosetta.ast.functions.RFunction.class).get(0);
        var selfAlias = fn.shortcuts().stream()
                .filter(sc -> "flagged".equals(sc.name())).findFirst().orElseThrow();
        var selfRef = AstWalker.findAll(selfAlias,
                com.regnosys.rosetta.ast.expressions.references.RSymbolReference.class)
                .stream().filter(r -> "flagged".equals(r.name())).findFirst().orElseThrow();
        assertTrue(selfRef.symbol().isPresent(),
                "the self-name BINDS (not clearing-only)");
        assertTrue(selfRef.symbol().get()
                        instanceof com.regnosys.rosetta.ast.supporting.RAttribute attr
                        && "flagged".equals(attr.name()),
                "…to the implicit item's ATTRIBUTE, not the enclosing alias: "
                        + selfRef.symbol().get());
        // The positional CONTROL: the same name OUTSIDE the alias body (the
        // set body) still binds the ALIAS — the filter is the alias's own
        // boundary, not a name-global ban.
        var outerRef = AstWalker.findAll(fn.operations().get(0),
                com.regnosys.rosetta.ast.expressions.references.RSymbolReference.class)
                .stream().filter(r -> "flagged".equals(r.name())).findFirst().orElseThrow();
        assertTrue(outerRef.symbol().isPresent()
                        && outerRef.symbol().get() == selfAlias,
                "outside the alias body the name means the alias: "
                        + outerRef.symbol());
        // Negative (the flip's own semantics — pre-#453 this silently
        // self-bound): when the item type carries NO same-named attribute the
        // filtered name has nowhere to go — the honest diagnostic at exact
        // bytes, matching upstream's unresolved-reference error class.
        RModel negModel = AstBuilder.buildFromString("""
            namespace "smoke.aliasself2"
            type Info:
                other boolean (0..1)
            type Trade:
                info Info (0..*)
            func F:
                inputs: t Trade (1..1)
                output: res boolean (0..1)
                alias missing: t -> info extract missing = True then only-element
                set res: missing
            """, "smoke.rosetta");
        var negResult = RWorkspace.build(List.of(negModel));
        var symErrors = negResult.linkingDiagnostics().stream()
                .filter(d -> d.category().name().equals("SYMBOL_NOT_FOUND"))
                .toList();
        assertEquals(1, symErrors.size(),
                "the filtered self-name with no item feature diagnoses EXACTLY once: "
                        + negResult.linkingDiagnostics());
        assertEquals("Symbol 'missing' not found", symErrors.get(0).message(),
                "the linker's message bytes");
    }

    @Test void pre_condition_output_filtered_post_condition_binds() {
        // PR #453 (facet aliasSelfScopeFilter, the second vendored row):
        // upstream filters the function OUTPUT from a condition body's scope
        // unless the condition is a POST-condition (RosettaScopeProvider
        // :405-406 — feature-identity-based: only the output descr is
        // removed; inputs and globals stay). The fork splits the classes, so
        // the gate is an RCondition ancestor — the census's 7 live
        // post-condition output refs (RPostCondition) keep binding.
        RModel model = AstBuilder.buildFromString("""
            namespace "smoke.condout1"
            func G:
                inputs: a boolean (0..1)
                output: res boolean (1..1)
                condition Pre:
                    res = True
                set res: True
                post-condition Post:
                    a = True and res = True
            """, "smoke.rosetta");
        var result = RWorkspace.build(List.of(model));
        var symErrors = result.linkingDiagnostics().stream()
                .filter(d -> d.category().name().equals("SYMBOL_NOT_FOUND"))
                .toList();
        assertEquals(1, symErrors.size(),
                "the PRE-condition's bare output name is filtered (no item "
                        + "context to fall to) — exactly one diagnostic; the "
                        + "post-condition's output AND input refs both bind: "
                        + result.linkingDiagnostics());
        assertEquals("Symbol 'res' not found", symErrors.get(0).message(),
                "the linker's message bytes");
        var fn = AstWalker.findAll(model,
                com.regnosys.rosetta.ast.functions.RFunction.class).get(0);
        var output = fn.output().orElseThrow();
        var postRefs = AstWalker.findAll(fn.postConditions().get(0),
                com.regnosys.rosetta.ast.expressions.references.RSymbolReference.class);
        var postOut = postRefs.stream()
                .filter(r -> "res".equals(r.name())).findFirst().orElseThrow();
        assertTrue(postOut.symbol().isPresent() && postOut.symbol().get() == output,
                "the post-condition binds the OUTPUT node itself: " + postOut.symbol());
        var postIn = postRefs.stream()
                .filter(r -> "a".equals(r.name())).findFirst().orElseThrow();
        assertTrue(postIn.symbol().isPresent(),
                "the post-condition's input ref binds too: " + postIn.symbol());
    }

    private RWorkspace buildWorkspace(String source) {
        RModel model = AstBuilder.buildFromString(source, "smoke.rosetta");
        return RWorkspace.build(List.of(model)).workspace();
    }
}
