package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Leg-C port (PR #428, slice 3b-ii — the FINAL leg-C item) of upstream
 * {@code rune-integration-tests/.../function/FunctionGeneratorTest.xtend} —
 * 148 upstream methods = 146 active + 2 upstream-{@code @Disabled} carried.
 * The red-run split (probe-measured, one generation+compile sweep over all
 * 148 models, PLUS the first-run invoke batteries — six compile-clean
 * methods surfaced RUNTIME-behavior divergences the probe cannot see):
 * <b>129 running green</b> (77 invoke batteries through the released
 * runtime + 21 full-text locks byte-identical to the upstream expected
 * blocks + 4 text+invoke hybrids + 25 compile-only batteries + 2 VALID
 * methods carried on the fork's own diagnostics, messages measured;
 * {@code testDeepPathOperatorWithMeta} joined the invoke set at the #429
 * typing-channel heal — finding #16's deep-path freshname-shadow carrier,
 * un-pinned; {@code nestedInlineFunctionsTest} joined at the #431
 * then-hoist heal — finding #18's lambda-interior face, the nested
 * then-chains now hoist in-lambda thenArg block lambdas and cross-lambda
 * closure params resolve through the nested lexical scope;
 * {@code thenOperationTest}, {@code canChainAfterConditional} and
 * {@code canReturnDifferingCardinalitiesInIfThenElseBranches} joined at
 * the #432 sum/conditional-form heal — findings #19+#20 healed whole:
 * the sum types as its argument's item type, conditional
 * receivers/arms/whole-outputs take the Mapper-form slot + arity
 * coercions, and the two runtime-only empty faces render the typed
 * {@code MapperS.<Void>ofNull()}; ELEVEN meta-channel methods joined at
 * the #433 wave — finding #22 healed WHOLE — and the LAST TWO at the
 * #434 wave, RETIRING finding #21: meta inputs read by CONSUMPTION
 * [the raw-wrapper scheme nav two-step, the null-guarded value deref,
 * the fused aggregate stream unwrap, the sort key extractor, the
 * element-wise whole-output coercion, the meta→meta passthrough, the
 * literal singleton add wrap, the #434 as-key value hoist and the #434
 * braced-lambda list rewrap], all thirteen oracle-byte-identical — the
 * func-meta-* hold-out groups; FOUR record methods joined at the same
 * #434 wave, RETIRING findings #23 and #24: the {@code date { … }} ctor
 * lowers to canonical {@code Date.of(…)} with the non-literal null-guard,
 * and the {@code time}/{@code timezone} record members lower to the
 * method-ref / getZone-getId forms — the func-record-* hold-out groups;
 * BOTH alias methods joined at the #435 wave, RETIRING finding #25: the
 * alias-rooted SET target fuses the alias body's output nav onto one
 * line and the op's own segments resolve against the alias's terminal
 * type — the func-alias-* hold-out groups)
 * and
 * <b>17 PINNED facet leads</b> across THIRTEEN leg-C findings
 * #26–#38 and two zero-diagnostic validation pins (the recorded
 * validation item; finding #18's carrier here was HEALED at the #431
 * wave, the #19/#20 carriers at the #432 wave, finding #22 whole and
 * seven of the nine #21 pins at the #433 wave, the two #21 residuals
 * plus findings #23+#24 whole at the #434 wave, finding #25 whole at
 * the #435 wave — findings #21, #23, #24 and #25 RETIRED):
 * <ul>
 *   <li><b>the recorded validation item</b> (facet lead recordedValidationItem, 2 pins) — the recorded validation item's false-NEGATIVE half: upstream
 *       rejects the model (assertError/assertWarning); the fork emits ZERO
 *       validation diagnostics (the slice-2 zero-diagnostic-pin
 *       precedent);
 *   </li>
 *   <li><b>finding #26</b> (facet lead namedFunctionRefLowering, 1 pin) — a NAMED function reference as a functional-operation argument
 *       (extract Incr / filter IsAnswerToTheUniverse / extract IsLeapYear)
 *       is not lowered: renders MapperS.of(IsLeapYear) — the class
 *       reference as a value;
 *   </li>
 *   <li><b>finding #27</b> (facet lead funcCallSingleToListArg, 1 pin) — a (0..1) value passed to a (0..*) function input misses the
 *       single-to-list arg coercion AND the injected dependency field is
 *       shadowed by the like-named local input (a.evaluate(a));
 *   </li>
 *   <li><b>finding #28</b> (facet lead selfCallWrapCoherence, 1 pin) — a recursive self-call renders with incoherent wraps:
 *       rec.evaluate().get() (CFS on the bare Integer) at the set seat and
 *       a bare rec.evaluate() where MapperS<Integer> is due at the alias
 *       seat;
 *   </li>
 *   <li><b>finding #29</b> (facet lead filterPredicateCoercion, 1 pin) — a filter over a single with an input-ref predicate renders the
 *       lambda returning the Mapper (MapperS.of(inp)) where Boolean is
 *       due;
 *   </li>
 *   <li><b>finding #30</b> (facet lead javaLangSelfCollision, 1 pin) — inside a generated function class named like a java.lang type
 *       (func Boolean), bare same-name type references resolve to the
 *       enclosing class — the #304-#306 collision law unenforced at the
 *       function-SELF seat;
 *   </li>
 *   <li><b>finding #31</b> (facet lead javaKeywordEscape, 1 pin) — attributes named with Java keywords (static) are emitted verbatim
 *       as identifiers — not a statement;
 *   </li>
 *   <li><b>finding #32</b> (facet lead ruleRecursionTyping, 1 pin) — a RECURSIVE reporting rule does not resolve through the workspace
 *       inference (RMissingType on the recursive conditional); generation
 *       declines LOUD per the A2 law instead of emitting Object;
 *   </li>
 *   <li><b>finding #33</b> (facet lead setSingleToListCoercion, 2 pins) — set of a (1..*) attribute from a single-valued RHS misses the
 *       single-to-list coercion at the direct-set seat (.setFoos(newFoo) /
 *       .setAttrList(s)) — the healed #12's law at yet another seat;
 *   </li>
 *   <li><b>finding #34</b> (facet lead ifCondLiteralWrap, 1 pin) — COMPILING text divergence: boolean LITERAL if/else-if conditions
 *       render MapperS.of(true).getOrDefault(false) where upstream renders
 *       the bare literal (if (true));
 *   </li>
 *   <li><b>finding #35</b> (facet lead constructorAsKeyReference, 2 pins) — the healed #15's as-key law at the CONSTRUCTOR-expression seat:
 *       Bar { b: myInput as-key } stores the raw meta object
 *       (.setB(myInput)) / value-only setters (.setAttrSingleValue)
 *       instead of stripping to external+global references — COMPILES,
 *       wrong at run (attrs prune to null);
 *   </li>
 *   <li><b>finding #36</b> (facet lead oneOfEmptyAttrList, 1 pin) — one-of renders choice(MapperS.of(a), Arrays.asList(), REQUIRED)
 *       with an EMPTY attribute list — the static type's attribute names
 *       are not populated; FALSE over a subtype instance where upstream's
 *       static-list form returns TRUE (the only-exists half of the method
 *       is GREEN) — COMPILES, wrong at run;
 *   </li>
 *   <li><b>finding #37</b> (facet lead closureEmptyArmNull, 1 pin) — an empty arm inside an extract closure returns a null Mapper and
 *       the enclosing chain NPEs at run (upstream yields empty/null
 *       gracefully);
 *   </li>
 *   <li><b>finding #38</b> (facet lead omittedParamBinding, 1 pin) — the omitted-parameter unary lambda ([* 2]) is CONSTANT-FOLDED to a
 *       negated literal (MapperS.of(-2)) — the implicit item binding is
 *       lost; COMPILES, every item maps to -2 at run (upstream
 *       multiplies);
 *   </li>
 * </ul>
 * Every generation pin asserts the CURRENT fork bytes (positive token),
 * the upstream form's absence where its text is known (negative token),
 * and the measured real-javac NON-compile; the RUNTIME pins (#35–#38 —
 * #21's choice face HEALED at the #433 wave) compile and RUN, asserting
 * the measured wrong behavior;
 * the compiling-text pin #34 and the GEN_ERR pin #32 assert their own
 * measured shapes — on a heal the pin breaks and the method gets
 * upstream's own asserts (the #426 un-pin precedent). The two
 * zero-diagnostic validation pins break when the validator starts firing.
 *
 * <p>Port deltas (semantics-identical, the 3b-i law): hamcrest
 * {@code hasItems} → {@code List.containsAll} under the upstream size
 * pins (distinct items verified at build time); Guava
 * {@code ImmutableList.of}/xtend {@code #[...]} → {@code List.of};
 * construction and invocation fully reflective (the isolated-loader law —
 * results held as {@code Object}); {@code MetaFields}/{@code Reference}
 * runtime objects built reflectively; upstream's {@code a.class} result
 * classes → {@code Object.class} (the cast target is advisory under
 * reflective invocation). VALID methods ride the slice-2 diagnostics
 * channel ({@code UpstreamExpressionPortSupport}) — the fork's measured
 * message forms are asserted and upstream's message is recorded per
 * method. Models and expected blocks extracted MECHANICALLY from the
 * upstream xtend (the banked {@code target/425-expected/} pipeline + the
 * space-margin v2 law for the three space-indented tail methods).
 * Ledger: the development audit "2026-07-17-leg-c-integration-port-ledger".
 */
class UpstreamFunctionGeneratorPortTest {

    @BeforeAll
    static void requireReleasedRuntime() {
        UpstreamPortHarness.assumeReleasedRuntime();
    }

    /**
     * Upstream {@code reportingRuleSupportsRecursion} — leg-C finding #32 HEALED at
     * the #437 board-clearing wave (facet ruleRecursionTyping: the in-cycle rule
     * call seeds NOTHING — upstream's cycleTracker law — and the literal {@code item}
     * keyword at rule-body top level types + renders as the rule input, so the
     * recursive rule infers int and self-injects; oracle-byte-identical, the
     * report-rule-recursion group).
     */
    @Test
    void reportingRuleSupportsRecursion() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                reporting rule Fac from int:
                	if item = 1
                	then 1
                	else item * Fac(item - 1)
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object facRule = UpstreamPortHarness.createFunc(classes, "FacRule",
                "com.rosetta.test.model.reports");
        assertEquals(120, UpstreamPortHarness.invokeFunc(facRule, Integer.class, 5));
    }

    /**
     * Upstream {@code testCanPassMetaFromOutputOfFunctionCall} — leg-C finding #21
     * HEALED at the #433 meta-channel wave (facets fnIoMetaCallResultType +
     * metaPathShortForm: the call result reports its wrapper type, so the scheme
     * nav renders the two-step {@code getMeta→getScheme} short form —
     * oracle-byte-identical, the func-meta-call-scheme group).
     */
    @Test
    void testCanPassMetaFromOutputOfFunctionCall() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func A:
                	inputs:
                		myInput string (1..1)
                			[metadata scheme]
                  	output:
                    	result string (1..1)
                      		[metadata scheme]
                     set result: myInput

                func B:
                	inputs:
                		myInput string (1..1)
                			[metadata scheme]
                  	output:
                    	result string (1..1)
                  	set result:
                    	A(myInput) -> scheme
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object funcB = UpstreamPortHarness.createFunc(classes, "B");
        Object myInput = UpstreamPortHarness.createFieldWithMetaString(classes, "myValue", "myScheme");
        assertEquals("myScheme", UpstreamPortHarness.invokeFunc(funcB, String.class, myInput));
    }

    /**
     * Upstream {@code testIgnoreMetaOnChoiceTypes} — leg-C finding #21 (the RUNTIME
     * face) HEALED at the #433 meta-channel wave (facet fnIoMetaChoiceValueDeref:
     * the required-choice fires on the VALUE via the null-guarded deref —
     * oracle-byte-identical, the func-meta-choice-ignore group).
     */
    @Test
    void testIgnoreMetaOnChoiceTypes() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                  a string (1..1)
                  b int (0..1)
                  c number (0..1)
                
                func Test:
                  inputs:
                    foo Foo (1..1)
                      [metadata scheme]
                  output:
                    result boolean (1..1)
                  set result:
                    foo required choice b, c
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "Test");
        Object fooVal = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo",
                Map.of("a", "aValue", "c", BigDecimal.valueOf(20)));
        Object myInput = UpstreamPortHarness.createInstanceUsingBuilder(classes,
                "com.rosetta.test.model.metafields", "FieldWithMetaFoo",
                Map.of("value", fooVal, "meta", metaFields(classes, "myScheme", null, null)), Map.of());
        assertTrue(UpstreamPortHarness.invokeFunc(func, Boolean.class, myInput));
    }

    /**
     * Upstream {@code testSortFunctionsOnMetaItemsInInput} — leg-C finding #22
     * HEALED at the #433 meta-channel wave (facets fnIoMetaListWrap +
     * fnIoMetaSortKeyExtractor + fnIoMetaMultiSetElemCoerce: the wrapper pipe
     * sorts by the unwrapped value and the terminal coerces element-wise —
     * oracle-byte-identical, the func-meta-sort group).
     */
    @Test
    void testSortFunctionsOnMetaItemsInInput() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func Test:
                	inputs:
                		myInputs string (1..*)
                		[metadata scheme]
                	output:
                		result string (1..*)
                \t\t
                	set result: myInputs sort
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "Test");
        List<Object> myInputs = List.of(
                UpstreamPortHarness.createFieldWithMetaString(classes, "DDDD", "myScheme"),
                UpstreamPortHarness.createFieldWithMetaString(classes, "AAAA", "myScheme"),
                UpstreamPortHarness.createFieldWithMetaString(classes, "HHHH", "myScheme"));
        assertEquals(List.of("AAAA", "DDDD", "HHHH"),
                UpstreamPortHarness.invokeFunc(func, List.class, myInputs));
    }

    /**
     * Upstream {@code testSumOnMetaIntegers} — leg-C finding #22 HEALED at the
     * #433 meta-channel wave (facet fnIoMetaAggregateStreamUnwrap: the fused
     * value-unwrap receiver — oracle-byte-identical, the func-meta-sum group).
     */
    @Test
    void testSumOnMetaIntegers() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func Test:
                  inputs:
                    n int (0..*)
                      [metadata scheme]
                  output:
                    result int (1..1)
                  set result:
                    n sum
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "Test");
        List<Object> myInputs = List.of(
                UpstreamPortHarness.createInstanceUsingBuilder(classes,
                        "com.rosetta.model.metafields", "FieldWithMetaInteger",
                        Map.of("value", 6, "meta", metaFields(classes, "myScheme", null, null)),
                        Map.of()),
                UpstreamPortHarness.createInstanceUsingBuilder(classes,
                        "com.rosetta.model.metafields", "FieldWithMetaInteger",
                        Map.of("value", 5, "meta", metaFields(classes, "myScheme", null, null)),
                        Map.of()));
        assertEquals(11, UpstreamPortHarness.invokeFunc(func, Integer.class, myInputs));
    }

    /**
     * Upstream {@code testMaxFunctionsOnMetaItemsInInput} — leg-C finding #22
     * HEALED at the #433 meta-channel wave (facet fnIoMetaAggregateStreamUnwrap —
     * oracle-byte-identical, the func-meta-max group).
     */
    @Test
    void testMaxFunctionsOnMetaItemsInInput() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func Test:
                	inputs:
                		myInputs string (1..*)
                		[metadata scheme]
                	output:
                		result string (1..1)
                \t\t
                	set result: myInputs max
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "Test");
        List<Object> myInputs = List.of(
                UpstreamPortHarness.createFieldWithMetaString(classes, "AAAA", "myScheme"),
                UpstreamPortHarness.createFieldWithMetaString(classes, "BBBB", "myScheme"));
        assertEquals("BBBB", UpstreamPortHarness.invokeFunc(func, String.class, myInputs));
    }

    /**
     * Upstream {@code testMinFunctionsOnMetaItemsInInput} — leg-C finding #22
     * HEALED at the #433 meta-channel wave (facet fnIoMetaAggregateStreamUnwrap —
     * oracle-byte-identical, the func-meta-min group).
     */
    @Test
    void testMinFunctionsOnMetaItemsInInput() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func Test:
                	inputs:
                		myInputs string (1..*)
                		[metadata scheme]
                	output:
                		result string (1..1)
                \t\t
                	set result: myInputs min
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "Test");
        List<Object> myInputs = List.of(
                UpstreamPortHarness.createFieldWithMetaString(classes, "AAAA", "myScheme"),
                UpstreamPortHarness.createFieldWithMetaString(classes, "BBBB", "myScheme"));
        assertEquals("AAAA", UpstreamPortHarness.invokeFunc(func, String.class, myInputs));
    }

    /** Upstream {@code testToStringOnEnumWithMeta}. */
    @Test
    void testToStringOnEnumWithMeta() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                enum MyEnum:
                	A
                	B
                	C
                \t
                type Foo:
                	myEnum MyEnum (1..1)
                	[metadata scheme]
                
                func Test:
                	inputs:
                		myInput Foo (1..1)
                \t\t
                	output:
                		result string (1..1)
                \t\t
                	set result: myInput -> myEnum to-string
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "Test");
        Object myEnum = UpstreamPortHarness.createInstanceUsingBuilder(classes,
                "com.rosetta.test.model.metafields", "FieldWithMetaMyEnum",
                Map.of("value", UpstreamPortHarness.createEnumInstance(classes, "MyEnum", "B"),
                        "meta", metaFields(classes, "myScheme", null, null)), Map.of());
        Object myInput = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo", Map.of("myEnum", myEnum));
        assertEquals("B", UpstreamPortHarness.invokeFunc(func, String.class, myInput));
    }

    /**
     * Upstream {@code testSettingMetaOnOutputWithReferenceAsKey} — leg-C finding #21
     * HEALED at the #434 wave (facet fnIoMetaAsKeyValueHoist: the as-key SET over a
     * {@code [metadata reference]} input hoists the null-guarded value deref into
     * the target-path local — {@code final Foo resultB = myInput == null ? null :
     * myInput.getValue();} — and the standard key strip navigates off the LOCAL —
     * oracle-byte-identical, the func-meta-as-key-set group).
     */
    @Test
    void testSettingMetaOnOutputWithReferenceAsKey() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	[metadata key]

                type Bar:
                	b Foo (1..1)
                	[metadata reference]
                \t
                func Test:
                	inputs:
                		myInput Foo (1..1)
                		[metadata reference]
                	output:
                		result Bar (1..1)
                \t
                	set result -> b: myInput as-key
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "Test");
        Object myInput = UpstreamPortHarness.createInstanceUsingBuilder(classes,
                "com.rosetta.test.model.metafields", "ReferenceWithMetaFoo",
                Map.of("value", UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo",
                        Map.of("meta", metaFields(classes, null, "myExternalKey", "myGlobalKey")))),
                Map.of());
        Object expected = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Bar",
                Map.of("b", UpstreamPortHarness.createInstanceUsingBuilder(classes,
                        "com.rosetta.test.model.metafields", "ReferenceWithMetaFoo",
                        Map.of("externalReference", "myExternalKey",
                                "globalReference", "myGlobalKey"),
                        Map.of())));
        assertEquals(expected, UpstreamPortHarness.invokeFunc(func, Object.class, myInput));
    }

    /**
     * Upstream {@code testSettingMetaOnOutput} — leg-C finding #21 HEALED at the
     * #433 meta-channel wave (facet fnIoMetaPassthrough: the same-wrapper meta
     * input passes through RAW, {@code .setB(myInput)} —
     * oracle-byte-identical, the func-meta-passthrough group).
     */
    @Test
    void testSettingMetaOnOutput() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Bar:
                	b string (1..1)
                	[metadata scheme]
                \t
                func Test:
                	inputs:
                		myInput string (1..1)
                		[metadata scheme]
                	output:
                		result Bar (1..1)
                \t
                	set result -> b: myInput
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "Test");
        Object myInput = UpstreamPortHarness.createInstanceUsingBuilder(classes,
                "com.rosetta.model.metafields", "FieldWithMetaString",
                Map.of("value", "someInput", "meta", metaFields(classes, "myScheme", null, null)),
                Map.of());
        Object expected = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Bar",
                Map.of("b", UpstreamPortHarness.createInstanceUsingBuilder(classes,
                        "com.rosetta.model.metafields", "FieldWithMetaString",
                        Map.of("value", "someInput",
                                "meta", metaFields(classes, "myScheme", null, null)),
                        Map.of())));
        assertEquals(expected, UpstreamPortHarness.invokeFunc(func, Object.class, myInput));
    }

    /** Upstream {@code testTransitivilyPassingMetaReference}. */
    @Test
    void testTransitivilyPassingMetaReference() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type FooContainer:
                	foo Foo (1..1)
                	[metadata reference]
                
                type Foo:
                	[metadata key]
                
                type Bar:
                	b Foo (1..1)
                	[metadata reference]
                \t
                func Test:
                	inputs:
                		myInput FooContainer (1..1)
                \t\t
                	output:
                		result Bar (1..1)
                
                	set result: Bar {
                		b: myInput -> foo as-key
                	}
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "Test");
        Object fooMeta = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo",
                Map.of("meta", metaFields(classes, null, "myExternalKey", "myGlobalKey")));
        Object refFoo = UpstreamPortHarness.createInstanceUsingBuilder(classes,
                "com.rosetta.test.model.metafields", "ReferenceWithMetaFoo",
                Map.of("value", fooMeta), Map.of());
        Object myInput = UpstreamPortHarness.createInstanceUsingBuilder(classes, "FooContainer", Map.of("foo", refFoo));
        Object expectedRef = UpstreamPortHarness.createInstanceUsingBuilder(classes,
                "com.rosetta.test.model.metafields", "ReferenceWithMetaFoo",
                Map.of("externalReference", "myExternalKey", "globalReference", "myGlobalKey"), Map.of());
        Object expected = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Bar", Map.of("b", expectedRef));
        assertEquals(expected, UpstreamPortHarness.invokeFunc(func, Object.class, myInput));
    }

    /**
     * Upstream {@code testPassingMetaItemToConstructorWithReferenceAsKey} — leg-C
     * finding #35 HEALED at the #437 board-clearing wave (facet
     * ctorAsKeyReference, the meta-input identifier arm: the value deref hoists
     * into the ctor-FIELD-named local and the single Optional-chain reference
     * copy reads it — oracle-byte-identical, the func-ctor-as-key-meta group).
     */
    @Test
    void testPassingMetaItemToConstructorWithReferenceAsKey() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	[metadata key]
                
                type Bar:
                	b Foo (1..1)
                	[metadata reference]
                \t
                func Test:
                	inputs:
                		myInput Foo (1..1)
                		[metadata reference]
                	output:
                		result Bar (1..1)
                \t
                	set result: Bar {
                		b: myInput as-key
                	}
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "Test");
        Object fooMeta = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo",
                Map.of("meta", metaFields(classes, null, "myExternalKey", "myGlobalKey")));
        Object myInput = UpstreamPortHarness.createInstanceUsingBuilder(classes,
                "com.rosetta.test.model.metafields", "ReferenceWithMetaFoo",
                Map.of("value", fooMeta), Map.of());
        Object expectedRef = UpstreamPortHarness.createInstanceUsingBuilder(classes,
                "com.rosetta.test.model.metafields", "ReferenceWithMetaFoo",
                Map.of("externalReference", "myExternalKey", "globalReference", "myGlobalKey"), Map.of());
        Object expected = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Bar", Map.of("b", expectedRef));
        assertEquals(expected, UpstreamPortHarness.invokeFunc(func, Object.class, myInput));
    }

    /** Upstream {@code testPassingMetaItemToConstructor}. */
    @Test
    void testPassingMetaItemToConstructor() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	a string (1..1)
                	[metadata scheme]
                \t
                func Test:
                	inputs:
                		myInput string (1..1)
                		[metadata scheme]
                	output:
                		result Foo (1..1)
                \t
                	set result: Foo {
                		a: myInput
                	}
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "Test");
        Object myInput = UpstreamPortHarness.createFieldWithMetaString(classes, "someInput", "myScheme");
        Object expected = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo",
                Map.of("a", UpstreamPortHarness.createFieldWithMetaString(classes, "someInput", "myScheme")));
        assertEquals(expected, UpstreamPortHarness.invokeFunc(func, Object.class, myInput));
    }

    /** Upstream {@code testCompareItemWithMetaToItemWithMeta}. */
    @Test
    void testCompareItemWithMetaToItemWithMeta() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func Test:
                	inputs:
                		a string (1..1)
                		[metadata scheme]
                		b string (1..1)
                		[metadata scheme]
                	output:
                		result boolean (1..1)
                	set result: a = b
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "Test");
        Object a = UpstreamPortHarness.createFieldWithMetaString(classes, "foo", "myScheme");
        Object b = UpstreamPortHarness.createFieldWithMetaString(classes, "foo", "myScheme");
        assertTrue(UpstreamPortHarness.invokeFunc(func, Boolean.class, a, b));
    }

    /** Upstream {@code testCompareItemToItemWithMeta}. */
    @Test
    void testCompareItemToItemWithMeta() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func Test:
                	inputs:
                		a string (1..1)
                		b string (1..1)
                		[metadata scheme]
                	output:
                		result boolean (1..1)
                	set result: a = b
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "Test");
        Object a = "foo";
        Object b = UpstreamPortHarness.createFieldWithMetaString(classes, "foo", "myScheme");
        assertTrue(UpstreamPortHarness.invokeFunc(func, Boolean.class, a, b));
    }

    /** Upstream {@code testCompareItemWithMetaToItemWithReference}. */
    @Test
    void testCompareItemWithMetaToItemWithReference() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func Test:
                	inputs:
                		a string (1..1)
                		[metadata scheme]
                		b string (1..1)
                		[metadata reference]
                	output:
                		result boolean (1..1)
                	set result: a = b
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "Test");
        Object a = UpstreamPortHarness.createFieldWithMetaString(classes, "foo", "myScheme");
        Object b = UpstreamPortHarness.createInstanceUsingBuilder(classes, "com.rosetta.model.metafields",
                "ReferenceWithMetaString",
                Map.of("value", "foo", "reference", reference(classes, "myRef")), Map.of());
        assertTrue(UpstreamPortHarness.invokeFunc(func, Boolean.class, a, b));
    }

    /** Upstream {@code testDeepFeatureCallWithMeta}. */
    @Test
    void testDeepFeatureCallWithMeta() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                choice Foo:
                	[metadata key]
                \t
                    A
                    B
                
                type A:
                    attr int (1..1)
                
                type B:
                    attr int (1..1)
                
                func Test:
                    inputs:
                        fooWithReference Foo (1..1)
                            [metadata reference]
                    output:
                        result int (1..1)
                    set result:
                        fooWithReference ->> attr
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "Test");
        Object aVal = UpstreamPortHarness.createInstanceUsingBuilder(classes, "A", Map.of("attr", 99));
        Object fooVal = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo", Map.of("a", aVal));
        Object fooWithReference = UpstreamPortHarness.createInstanceUsingBuilder(classes,
                "com.rosetta.test.model.metafields", "ReferenceWithMetaFoo",
                Map.of("value", fooVal, "reference", reference(classes, "myRef")), Map.of());
        assertEquals(99, UpstreamPortHarness.invokeFunc(func, Integer.class, fooWithReference));
    }

    /**
     * Upstream {@code canHandleMetaCoecrion} — leg-C finding #21 HEALED at the #434
     * wave (facet fnIoMetaListRewrap: the (0..*) reference→(0..*) scheme
     * whole-output SET takes upstream's List-seat wrapperToWrapper item-conversion
     * stream — the braced-lambda unwrap-convert-rewrap with the coercion-service
     * {@code BigDecimal.valueOf} inner conversion — oracle-byte-identical, the
     * func-meta-coercion group; the method name is upstream's own spelling —
     * {@code Coecrion} — ported 1:1).
     */
    @Test
    void canHandleMetaCoecrion() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                metaType reference string

                func SomeFunc:
                	inputs:
                		myInput int (0..*)
                		[metadata reference]

                	output:
                		myResult number (0..*)
                		[metadata scheme]
                \t\t
                	set myResult: myInput\t
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "SomeFunc");
        List<Object> myInput = List.of(
                UpstreamPortHarness.createInstanceUsingBuilder(classes,
                        "com.rosetta.model.metafields", "ReferenceWithMetaInteger",
                        Map.of("value", 5, "reference", reference(classes, "myRef")), Map.of()),
                UpstreamPortHarness.createInstanceUsingBuilder(classes,
                        "com.rosetta.model.metafields", "ReferenceWithMetaInteger",
                        Map.of("value", 10, "reference", reference(classes, "myRef2")), Map.of()),
                UpstreamPortHarness.createInstanceUsingBuilder(classes,
                        "com.rosetta.model.metafields", "ReferenceWithMetaInteger",
                        Map.of("value", 15, "reference", reference(classes, "myRef3")), Map.of()));
        List<Object> expected = List.of(
                UpstreamPortHarness.createInstanceUsingBuilder(classes,
                        "com.rosetta.model.metafields", "FieldWithMetaBigDecimal",
                        Map.of("value", BigDecimal.valueOf(5)), Map.of()),
                UpstreamPortHarness.createInstanceUsingBuilder(classes,
                        "com.rosetta.model.metafields", "FieldWithMetaBigDecimal",
                        Map.of("value", BigDecimal.valueOf(10)), Map.of()),
                UpstreamPortHarness.createInstanceUsingBuilder(classes,
                        "com.rosetta.model.metafields", "FieldWithMetaBigDecimal",
                        Map.of("value", BigDecimal.valueOf(15)), Map.of()));
        assertEquals(expected, UpstreamPortHarness.invokeFunc(func, List.class, myInput));
    }

    /**
     * Upstream {@code canPassMetadataToFunctionAndUseInExpression} — leg-C finding
     * #21 HEALED at the #433 meta-channel wave (the typing arm — the unresolved
     * scheme meta-feature types string, so the join renders
     * {@code <String, String, String>add} — + facet fnIoMetaOperandValueDeref:
     * the value operand takes the null-guarded param deref —
     * oracle-byte-identical, the func-meta-scheme-arith group).
     */
    @Test
    void canPassMetadataToFunctionAndUseInExpression() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func SomeFunc:
                    inputs:
                        myInput string (1..1)
                        [metadata scheme]
                    output:
                        myResult string (1..1)
                
                    set myResult: myInput + myInput -> scheme
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "SomeFunc");
        Object myInput = UpstreamPortHarness.createFieldWithMetaString(classes, "myInputValue", "myScheme");
        assertEquals("myInputValuemyScheme",
                UpstreamPortHarness.invokeFunc(func, String.class, myInput));
    }

    /** Upstream {@code canSetFunctionWithMetaOutput}. */
    @Test
    void canSetFunctionWithMetaOutput() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func SomeFunc:
                    inputs:
                        myInput string (1..1)
                        [metadata scheme]
                    output:
                        myResult string (1..1)
                        [metadata scheme]\t
                
                    set myResult: myInput
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "SomeFunc");
        Object myInput = UpstreamPortHarness.createFieldWithMetaString(classes, "myInputValue", "myScheme");
        Object result = UpstreamPortHarness.invokeFunc(func, Object.class, myInput);
        assertEquals(myInput, result);
    }

    /**
     * Upstream {@code canPassMetadataToFunctions} — leg-C finding #21 HEALED at the
     * #433 meta-channel wave (facet fnIoMetaSchemeNavReceiver: the nav receiver
     * keeps the raw wrapper, so metaPathShortForm renders the two-step
     * {@code getMeta→getScheme} — oracle-byte-identical, the
     * func-meta-scheme-nav group).
     */
    @Test
    void canPassMetadataToFunctions() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func SomeFunc:
                    inputs:
                        myInput string (1..1)
                        [metadata scheme]
                    output:
                        myResult string (1..1)
                
                    set myResult: myInput -> scheme
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "SomeFunc");
        Object myInput = UpstreamPortHarness.createFieldWithMetaString(classes, "myInputValue", "myScheme");
        assertEquals("myScheme", UpstreamPortHarness.invokeFunc(func, String.class, myInput));
    }

    /**
     * Upstream {@code assignToMultiMetaFeature} — leg-C finding #21 HEALED at the
     * #433 meta-channel wave (facet fnIoMetaAddLiteralSingletonWrap: the literal
     * hoists to {@code final String string = "Hello";} and the LOCAL wraps into
     * the singleton meta-builder list — oracle-byte-identical, the
     * func-meta-add-value group).
     */
    @Test
    void assignToMultiMetaFeature() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type A:
                    a string (0..*)
                    	[metadata reference]
                
                func Test:
                	output:
                		result A (1..1)
                \t
                	add result -> a:
                		"Hello"
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "Test");
        Object expected = UpstreamPortHarness.createInstanceUsingBuilder(classes, "A",
                Map.of("a", List.of(UpstreamPortHarness.createInstanceUsingBuilder(classes,
                        "com.rosetta.model.metafields", "ReferenceWithMetaString",
                        Map.of("value", "Hello"), Map.of()))));
        assertEquals(expected, UpstreamPortHarness.invokeFunc(func, Object.class));
    }

    /** Upstream {@code onlyExistsOnAbsentParent}. */
    @Test
    void onlyExistsOnAbsentParent() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type A:
                    a1 string (0..1)
                    a2 string (0..1)
                
                func TestOnlyExists:
                	inputs:
                		a A (1..1)
                	output:
                		result boolean (1..1)
                \t
                	set result:
                		a -> a1 only exists
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "TestOnlyExists");
        assertFalse(UpstreamPortHarness.invokeFunc(func, Boolean.class, (Object) null));
    }

    /**
     * Upstream {@code onlyExistsAndOneOfWorkOnStaticType} — leg-C finding #36 HEALED
     * at the #437 board-clearing wave (facet oneOfStaticAttrList: the one-of choice
     * enumerates the argument's STATIC type's attribute names — upstream
     * caseOneOfOperation's t.allAttributes; oracle-byte-identical, the
     * func-one-of-static group).
     */
    @Test
    void onlyExistsAndOneOfWorkOnStaticType() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type A:
                    a1 string (0..1)
                    a2 string (0..1)
                    a3 boolean (0..1)
                
                type B extends A:
                    b1 string (0..1)
                
                func TestOnlyExists:
                	inputs:
                		a A (1..1)
                	output:
                		result boolean (1..1)
                \t
                	set result:
                		a -> a1 only exists
                
                func TestOneOf:
                	inputs:
                		a A (1..1)
                	output:
                		result boolean (1..1)
                \t
                	set result:
                		a one-of
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object b1 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "B",
                Map.of("a1", "some value", "b1", "other value"));
        Object b2 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "B",
                Map.of("b1", "other value"));
        Object testOnlyExists = UpstreamPortHarness.createFunc(classes, "TestOnlyExists");
        assertTrue(UpstreamPortHarness.invokeFunc(testOnlyExists, Boolean.class, b1));
        assertFalse(UpstreamPortHarness.invokeFunc(testOnlyExists, Boolean.class, b2));
        Object testOneOf = UpstreamPortHarness.createFunc(classes, "TestOneOf");
        assertTrue(UpstreamPortHarness.invokeFunc(testOneOf, Boolean.class, b1));
        assertFalse(UpstreamPortHarness.invokeFunc(testOneOf, Boolean.class, b2));
    }

    /**
     * Upstream {@code testDeepPathOperatorWithMeta} — un-pinned at the #429
     * typing-channel heal (finding #16's freshname-shadow face: the meta-tail
     * short form now mints its lambda param scope-uniquely, {@code _a} against
     * the in-scope input {@code a}, exactly upstream's
     * {@code createUniqueIdentifier}). Port delta (semantics-identical):
     * upstream decorates the B instance with a MetaFields key — not read by
     * the invoked {@code a ->> id -> scheme} path or its assert.
     */
    @Test
    void testDeepPathOperatorWithMeta() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                choice A:
                	B
                		[metadata reference]
                	C
                		[metadata reference]
                
                type B:
                	[metadata key]
                	id string (1..1)
                		[metadata scheme]
                
                type C:
                	[metadata key]
                	id string (1..1)
                		[metadata scheme]
                
                func Test:
                	inputs:
                		a A (1..1)
                	output:
                		result string (1..1)
                \t
                	set result:
                		a ->> id -> scheme
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "Test");
        Object id = UpstreamPortHarness.createFieldWithMetaString(classes, "abc123", "myScheme");
        Object b = UpstreamPortHarness.createInstanceUsingBuilder(classes, "B", Map.of("id", id));
        Object refB = UpstreamPortHarness.createInstanceUsingBuilder(classes,
                "com.rosetta.test.model.metafields", "ReferenceWithMetaB",
                Map.of("value", b, "globalReference", "globalRef", "externalReference", "externalRef"),
                Map.of());
        Object aB = UpstreamPortHarness.createInstanceUsingBuilder(classes, "A", Map.of("B", refB));
        assertEquals("myScheme", UpstreamPortHarness.invokeFunc(func, String.class, aB));
    }

    /**
     * Upstream {@code testDeepPathOperatorWithMultiMeta} — leg-C finding #21 HEALED
     * at the #433 meta-channel wave (facet fnIoMetaMultiSetElemCoerce: the MULTI
     * deep-path pipe keeps its wrapper items and the whole-output terminal
     * coerces element-wise + {@code .getMulti()} — oracle-byte-identical, the
     * func-meta-deep-path-multi group).
     */
    @Test
    void testDeepPathOperatorWithMultiMeta() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                choice A:
                	B
                	C
                
                type ABase:
                	prop int (0..*)
                		[metadata scheme]
                
                type B extends ABase:
                
                type C extends ABase:
                
                func Test:
                	inputs:
                		a A (1..1)
                	output:
                		result int (0..*)
                \t
                	set result:
                		a ->> prop
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "Test");
        Object aB = UpstreamPortHarness.createInstanceUsingBuilder(classes, "A",
                Map.of("B", UpstreamPortHarness.createInstanceUsingBuilder(classes, "B",
                        Map.of("prop", List.of(
                                UpstreamPortHarness.createInstanceUsingBuilder(classes,
                                        "com.rosetta.model.metafields", "FieldWithMetaInteger",
                                        Map.of("meta", metaFields(classes, "myScheme", null, null),
                                                "value", 42),
                                        Map.of()),
                                UpstreamPortHarness.createInstanceUsingBuilder(classes,
                                        "com.rosetta.model.metafields", "FieldWithMetaInteger",
                                        Map.of("meta", metaFields(classes, "otherScheme", null, null),
                                                "value", 0),
                                        Map.of()))))));
        assertEquals(List.of(42, 0), UpstreamPortHarness.invokeFunc(func, List.class, aB));
    }

    /** Upstream {@code testDeepPathOperator}. */
    @Test
    void testDeepPathOperator() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                choice A:
                	B
                	C
                
                type B:
                	opt1 Option1 (0..1)
                	opt2 Option2 (0..1)
                	attr Foo (0..1)
                \t
                	condition Choice: one-of
                
                type C:
                	opt1 Option1 (0..1)
                \t
                	condition Choice: one-of
                
                type Option1:
                	attr Foo (1..1)
                
                type Option2:
                	attr Foo (1..1)
                	otherAttr string (1..1)
                
                type Option3:
                	attr Foo (1..1)
                
                type Foo:
                	id string (1..1)
                
                func Test:
                	inputs:
                		a A (1..1)
                		b B (1..1)
                		aList A (0..*)
                	output:
                		result Foo (0..*)
                \t
                	add result:
                		a ->> attr
                	add result:
                		a ->> opt1 -> attr
                	add result:
                		b ->> attr
                	add result:
                		aList ->> attr
                	add result:
                		aList ->> opt1 -> attr
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object injector = UpstreamPortHarness.guiceInjector(
                classes.get("com.rosetta.test.model.functions.Test").getClassLoader());
        Object func = UpstreamPortHarness.getInstance(injector,
                classes.get("com.rosetta.test.model.functions.Test"));
        Object foo1 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo", Map.of("id", "aBOpt1"));
        Object bOpt1 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "B",
                Map.of("opt1", UpstreamPortHarness.createInstanceUsingBuilder(classes, "Option1", Map.of("attr", foo1))));
        Object aBOpt1 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "A", Map.of("b", bOpt1));
        Object foo2 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo", Map.of("id", "aBOpt2"));
        Object bOpt2 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "B",
                Map.of("opt2", UpstreamPortHarness.createInstanceUsingBuilder(classes, "Option2",
                        Map.of("attr", foo2, "otherAttr", "some value"))));
        Object aBOpt2 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "A", Map.of("b", bOpt2));
        Object foo3 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo", Map.of("id", "aBAttr"));
        Object bAttr = UpstreamPortHarness.createInstanceUsingBuilder(classes, "B", Map.of("attr", foo3));
        Object aBAttr = UpstreamPortHarness.createInstanceUsingBuilder(classes, "A", Map.of("b", bAttr));
        Object foo4 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo", Map.of("id", "aCOpt1"));
        Object aCOpt1 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "A",
                Map.of("c", UpstreamPortHarness.createInstanceUsingBuilder(classes, "C",
                        Map.of("opt1", UpstreamPortHarness.createInstanceUsingBuilder(classes, "Option1",
                                Map.of("attr", foo4))))));
        assertEquals(List.of(foo1, foo1, foo2, foo4, foo3, foo2, foo4),
                UpstreamPortHarness.invokeFunc(func, List.class, aBOpt1, bOpt2, List.of(aCOpt1, aBAttr, aBOpt2)));
    }

    /** Upstream {@code testChoiceAttributeAccess}. */
    @Test
    void testChoiceAttributeAccess() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type A:
                	b B (1..1)
                
                type B:
                	val boolean (0..1)
                
                choice AB:
                	A
                	B
                
                func Foo:
                	inputs:
                		ab AB (1..1)
                	output:
                		result boolean (1..1)
                
                	set result:
                		if ab -> A exists
                		then ab -> A -> b -> val
                		else if ab -> B exists
                		then ab -> B -> val
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /**
     * Upstream {@code handlesNullWhenConstructingRecords} — leg-C finding #23 HEALED
     * at the #434 wave (facet dateRecordCtor: the {@code date { … }} ctor renders
     * {@code Date.of(<year>, <month>, <day>)} in canonical order with the
     * null-guard if/else over the non-literal values; the zonedDateTime ctor was
     * already byte-correct via the #349-B arm — oracle-byte-identical, the
     * func-record-ctor-null group).
     */
    @Test
    void handlesNullWhenConstructingRecords() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func Foo:
                    inputs:
                        date date (0..1)
                        time time (0..1)
                        zone string (0..1)
                    output: result zonedDateTime (0..1)
                    set result:
                        zonedDateTime {
                            date: date,
                            time: time,
                            timezone: zone
                        }

                func Bar:
                    inputs:
                        day int (0..1)
                    output: result date (0..1)
                    set result:
                        date {
                            day: day,
                            year: 2024,
                            month: 2
                        }
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object foo = UpstreamPortHarness.createFunc(classes, "Foo");
        Object date = recordDate(classes, 2024, 2, 26);
        LocalTime time = LocalTime.of(11, 10);
        String zone = "Europe/Paris";
        ZonedDateTime zdt = ZonedDateTime.of(toLocalDate(classes, date), time, ZoneId.of(zone));
        assertEquals(zdt, UpstreamPortHarness.invokeFunc(foo, ZonedDateTime.class, date, time, zone));
        assertEquals(null, UpstreamPortHarness.invokeFunc(foo, ZonedDateTime.class, null, time, zone));
        assertEquals(null, UpstreamPortHarness.invokeFunc(foo, ZonedDateTime.class, date, null, zone));
        assertEquals(null, UpstreamPortHarness.invokeFunc(foo, ZonedDateTime.class, date, time, null));
        Object bar = UpstreamPortHarness.createFunc(classes, "Bar");
        assertEquals(recordDate(classes, 2024, 2, 26), UpstreamPortHarness.invokeFunc(bar, Object.class, 26));
        assertEquals(null, UpstreamPortHarness.invokeFunc(bar, Object.class, (Object) null));
    }

    /** Upstream {@code canEscapeIdentifiers}. */
    @Test
    void canEscapeIdentifiers() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func Foo:
                	inputs: ^func int (1..1)
                	output: result int (1..1)
                	set result:
                		^func
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.RosettaFunction;
                
                
                @ImplementedBy(Foo.FooDefault.class)
                public abstract class Foo implements RosettaFunction {
                
                	/**
                	* @param func\s
                	* @return result\s
                	*/
                	public Integer evaluate(Integer func) {
                		Integer result = doEvaluate(func);
                \t\t
                		return result;
                	}
                
                	protected abstract Integer doEvaluate(Integer func);
                
                	public static class FooDefault extends Foo {
                		@Override
                		protected Integer doEvaluate(Integer func) {
                			Integer result = null;
                			return assignOutput(result, func);
                		}
                \t\t
                		protected Integer assignOutput(Integer result, Integer func) {
                			result = func;
                \t\t\t
                			return result;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.Foo"));
        UpstreamPortHarness.compileToClasses(code);
    }

    /**
     * Upstream {@code canReturnEmptyInsideExtract} — leg-C finding #37 HEALED at the
     * #437 board-clearing wave (facet closureEmptyArmOfNull: the {@code empty} arm
     * inside the extract closure returns the typed {@code MapperS.<Integer>ofNull()}
     * — the rule seat's #269 law joined by the FUNCTION path;
     * oracle-byte-identical, the func-extract-empty-arm group).
     */
    @Test
    void canReturnEmptyInsideExtract() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func Foo:
                	output:
                		result int (0..1)
                	set result:
                		42 extract
                		    if item > 0
                		    then empty
                		    else item
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "Foo");
        assertEquals(null, UpstreamPortHarness.invokeFunc(func, Integer.class));
    }

    /** Upstream {@code canConstructTypeWithEmptyValue}. */
    @Test
    void canConstructTypeWithEmptyValue() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type A:
                	prop1 int (0..1)
                	prop2 int (0..*)
                	prop3 A (0..1)
                	prop4 A (0..*)
                
                func CreateA:
                	output: result A (1..1)
                	set result:
                		A {
                			prop1: empty,
                			prop2: empty,
                			prop3: empty,
                			prop4: empty,
                		}
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "CreateA");
        Object a = UpstreamPortHarness.createInstanceUsingBuilder(classes, "A",
                map("prop1", null, "prop2", List.of(), "prop3", null, "prop4", List.of()));
        assertEquals(a, UpstreamPortHarness.invokeFunc(func, Object.class));
    }

    /** Upstream {@code constructorExpression}. */
    @Test
    void constructorExpression() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type A:
                	a int (1..1)
                	b string (0..*)
                	c A (0..1)
                
                func CreateA:
                	output: result A (1..1)
                	set result:
                		A {
                			c: A { a: 0, ... },
                			b: ["A", "B"],
                			a: 2*21,
                		}
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "CreateA");
        Object a = UpstreamPortHarness.createInstanceUsingBuilder(classes, "A",
                Map.of("a", 42, "b", List.of("A", "B"),
                        "c", UpstreamPortHarness.createInstanceUsingBuilder(classes, "A", Map.of("a", 0))));
        assertEquals(a, UpstreamPortHarness.invokeFunc(func, Object.class));
    }

    /**
     * Upstream {@code recordConstructorExpression} — leg-C finding #23 HEALED at
     * the #434 wave (facet dateRecordCtor: the all-literal {@code date { … }} ctor
     * renders the guard-free canonical {@code Date.of(1998, 11, 4)} —
     * oracle-byte-identical, the func-record-ctor group).
     */
    @Test
    void recordConstructorExpression() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func CreateDate:
                	output: result date (1..1)
                	set result:
                		date {
                			day: 4,
                			month: 11,
                			year: 1998
                		}
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object createDate = UpstreamPortHarness.createFunc(classes, "CreateDate");
        assertEquals(recordDate(classes, 1998, 11, 4),
                UpstreamPortHarness.invokeFunc(createDate, Object.class));
    }

    /**
     * Upstream {@code constructorExpressionWithReference} — leg-C finding #35 HEALED
     * at the #437 board-clearing wave (facet ctorAsKeyReference, the bare-identifier
     * single arm — the Optional-chain reference copy over the keyed input, no hoist
     * — + the MULTI stream arm sharing the segment-ADD seat's element-wise tail;
     * oracle-byte-identical, the func-ctor-as-key-ref group).
     */
    @Test
    void constructorExpressionWithReference() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type TypeWithKey:
                	[metadata key]
                
                type OtherType:
                	attrSingle TypeWithKey (1..1)
                		[metadata reference]
                	attrMulti TypeWithKey (0..*)
                		[metadata reference]
                
                func CreateOtherType:
                	inputs:
                		key TypeWithKey (1..1)
                	output: result OtherType (1..1)
                	set result:
                		OtherType {
                			attrSingle: key as-key,
                			attrMulti: [key, key] as-key
                		}
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "CreateOtherType");
        Object objectWithKey = UpstreamPortHarness.createInstanceUsingBuilder(classes, "TypeWithKey",
                Map.of("meta", metaFields(classes, null, "external", "global")));
        Object objectWithKeyReference = UpstreamPortHarness.createInstanceUsingBuilder(classes,
                "com.rosetta.test.model.metafields", "ReferenceWithMetaTypeWithKey",
                Map.of("externalReference", "external", "globalReference", "global"), Map.of());
        Object otherObject = UpstreamPortHarness.createInstanceUsingBuilder(classes, "OtherType",
                Map.of("attrSingle", objectWithKeyReference),
                Map.of("attrMulti", List.of(objectWithKeyReference, objectWithKeyReference)));
        assertEquals(otherObject, UpstreamPortHarness.invokeFunc(func, Object.class, objectWithKey));
    }

    /** Upstream {@code singularExtractWithEmptyValueReturnsEmpty}. */
    @Test
    void singularExtractWithEmptyValueReturnsEmpty() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func A:
                	inputs:
                		input int (0..1)
                	output:
                		result boolean (0..1)
                	set result:
                		input
                			extract
                				if item = 0
                				then True
                				else False
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "A");
        assertNull(UpstreamPortHarness.invokeFunc(func, String.class, (Object) null));
    }

    /** Upstream {@code testDispatchFunction}. */
    @Test
    void testDispatchFunction() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                enum DayCountFractionEnum:
                	ACT_360 displayName "ACT/360"
                	ACT_365L displayName "ACT/365L"
                	ACT_364 displayName "ACT/364"
                	ACT_365_fixed displayName "ACT/365.FIXED"
                	_30E_360 displayName "30E/360"
                	_30_360 displayName "30/360"
                
                func DayCountBasis:
                	inputs:
                		dcf DayCountFractionEnum (1..1)
                	output:
                		basis int (1..1)
                
                func DayCountBasis(dcf: DayCountFractionEnum -> ACT_360):
                	set basis: 360
                
                func DayCountBasis(dcf: DayCountFractionEnum ->_30_360):
                	set basis: 360
                
                func DayCountBasis(dcf: DayCountFractionEnum ->_30E_360):
                	set basis: 360
                
                func DayCountBasis(dcf: DayCountFractionEnum -> ACT_365L):
                	set basis: 365
                
                func DayCountBasis(dcf: DayCountFractionEnum -> ACT_365_fixed):
                	set basis: 365
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "DayCountBasis");
        Class<?> dcfe = classes.get("com.rosetta.test.model.DayCountFractionEnum");
        Object act360 = fromDisplayName(dcfe, "ACT/360");
        Object act365Fixed = fromDisplayName(dcfe, "ACT/365.FIXED");
        Object act364 = fromDisplayName(dcfe, "ACT/364");
        assertEquals(360, UpstreamPortHarness.invokeFunc(func, Integer.class, act360));
        assertEquals(365, UpstreamPortHarness.invokeFunc(func, Integer.class, act365Fixed));
        assertThrows(IllegalArgumentException.class,
                () -> UpstreamPortHarness.invokeFunc(func, Integer.class, act364));
    }

    /** Upstream {@code conditionalThenJoin}. */
    @Test
    void conditionalThenJoin() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func A:
                	output:
                		result string (1..1)
                	set result:
                		if True
                	    then ["Foo", "Bar"]
                	    else "Bar"
                	    then join ", "
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "A");
        assertEquals("Foo, Bar", UpstreamPortHarness.invokeFunc(func, String.class));
    }

    /**
     * Upstream {@code canPassEmptyToFunctionThatExpectsList} — leg-C finding #27
     * HEALED at the #436 singles wave (facets fnInputDepCollisionEscape — the input
     * colliding with the {@code @Inject A a} dependency field escapes {@code _a} at
     * every signature/javadoc/body seat — + singleVarArgIntoMulti — the single
     * {@code (0..1)} arg into the {@code (0..*)} param takes the null-guarded
     * {@code Collections.singletonList} lift; the func-call-single-to-list oracle
     * group, 3 goldens byte-identical).
     */
    @Test
    void canPassEmptyToFunctionThatExpectsList() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func A:
                	inputs:
                		a int (0..*)
                	output:
                		result int (0..*)
                	add result:
                		a
                
                func B:
                	output: result int (0..*)
                	add result:
                		A(empty)
                
                func C:
                	inputs:
                		a int (0..1)
                	output:
                		result int (0..*)
                	add result:
                		A(a)
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);

        Object b = UpstreamPortHarness.createFunc(classes, "B");
        assertEquals(List.of(), UpstreamPortHarness.invokeFunc(b, List.class));

        Object c = UpstreamPortHarness.createFunc(classes, "C");
        assertEquals(List.of(), UpstreamPortHarness.invokeFunc(c, List.class, (Object) null));
    }

    /** Upstream {@code canUseNullAsCondition}. */
    @Test
    void canUseNullAsCondition() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func Test:
                	inputs: inp boolean (0..1)
                	output: result int (0..1)
                	set result:
                		if inp then 42
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "Test");
        assertNull(UpstreamPortHarness.invokeFunc(func, Integer.class, (Object) null));
    }

    /**
     * Upstream {@code canUseNullInFilter} — leg-C finding #29 HEALED at the #436
     * singles wave (facet filterPredicateBareBooleanInputValueForm: the bare
     * single-boolean input predicate renders the raw name —
     * {@code .filterSingleNullSafe(item -> inp)} — the Boolean the signature
     * demands; the func-filter-null-pred oracle group, byte-identical).
     */
    @Test
    void canUseNullInFilter() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func Test:
                	inputs: inp boolean (0..1)
                	output: result int (0..1)
                	set result:
                		42
                			filter inp
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object test = UpstreamPortHarness.createFunc(classes, "Test");

        assertNull(UpstreamPortHarness.invokeFunc(test, Integer.class, (Object) null));
    }

    /** Upstream {@code canChainAfterConditional}. */
    @Test
    void canChainAfterConditional() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func Test:
                	output: result int (0..*)
                	set result:
                		(if True then 42 else 0)
                			extract item + 1
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object test = UpstreamPortHarness.createFunc(classes, "Test");
        assertEquals(List.of(43), UpstreamPortHarness.invokeFunc(test, List.class));
    }

    /** Upstream {@code canReturnDifferingCardinalitiesInIfThenElseBranches}. */
    @Test
    void canReturnDifferingCardinalitiesInIfThenElseBranches() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func Test:
                	output: result int (0..*)
                	set result:
                		42
                			extract
                				if False
                				then [1, 2]
                				else 0
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object test = UpstreamPortHarness.createFunc(classes, "Test");
        assertEquals(List.of(0), UpstreamPortHarness.invokeFunc(test, List.class));
    }

    /** Upstream {@code passSingleItemToFunctionWhenMultiIsExpectedDoesNotResultInStaticCompilationError}. */
    @Test
    void passSingleItemToFunctionWhenMultiIsExpectedDoesNotResultInStaticCompilationError() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func A:
                	inputs: a int (0..*)
                	output: result int (1..1)
                	set result: 42
                
                func Foo:
                	output: result int (0..*)
                	set result:
                		[1, 2, 3]
                			extract A(item)
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code toEnumTest}. */
    @Test
    void toEnumTest() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                enum Bar:
                	Value1
                	Value2 displayName "Value 2"
                
                func ToBar:
                	inputs: input string (1..1)
                	output: result Bar (1..1)
                	set result:
                		input to-enum Bar
                
                func ToString:
                	inputs: input Bar (1..1)
                	output: result string (1..1)
                	set result:
                		input to-string
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Class<?> barClass = classes.get("com.rosetta.test.model.Bar");
        Object value1 = barClass.getEnumConstants()[0];
        Object value2 = barClass.getEnumConstants()[1];
        Object toBar = UpstreamPortHarness.createFunc(classes, "ToBar");
        assertEquals(value1, UpstreamPortHarness.invokeFunc(toBar, Object.class, "Value1"));
        assertNull(UpstreamPortHarness.invokeFunc(toBar, Object.class, "Value2"));
        assertEquals(value2, UpstreamPortHarness.invokeFunc(toBar, Object.class, "Value 2"));
        Object toString = UpstreamPortHarness.createFunc(classes, "ToString");
        assertEquals("Value1", UpstreamPortHarness.invokeFunc(toString, String.class, value1));
        assertEquals("Value 2", UpstreamPortHarness.invokeFunc(toString, String.class, value2));
    }

    /** Upstream {@code basicConversionTest}. */
    @Test
    void basicConversionTest() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func ToNumber:
                	inputs: input string (1..1)
                	output: result number (1..1)
                	set result:
                		input to-number
                
                func ToInt:
                	inputs: input string (1..1)
                	output: result int (1..1)
                	set result:
                		input to-int
                
                func ToTime:
                	inputs: input string (1..1)
                	output: result time (1..1)
                	set result:
                		input to-time
                
                func NumberToString:
                	inputs: input number (1..1)
                	output: result string (1..1)
                	set result:
                		input to-string
                
                func TimeToString:
                	inputs: input time (1..1)
                	output: result string (1..1)
                	set result:
                		input to-string
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object toNumber = UpstreamPortHarness.createFunc(classes, "ToNumber");
        assertEquals(BigDecimal.valueOf(3.14), UpstreamPortHarness.invokeFunc(toNumber, BigDecimal.class, "3.14"));
        assertNull(UpstreamPortHarness.invokeFunc(toNumber, BigDecimal.class, "test"));
        assertEquals(BigDecimal.valueOf(-42), UpstreamPortHarness.invokeFunc(toNumber, BigDecimal.class, "-42"));
        Object toInt = UpstreamPortHarness.createFunc(classes, "ToInt");
        assertEquals(3, UpstreamPortHarness.invokeFunc(toInt, Integer.class, "3"));
        assertNull(UpstreamPortHarness.invokeFunc(toInt, Integer.class, "test"));
        assertEquals(-42, UpstreamPortHarness.invokeFunc(toInt, Integer.class, "-42"));
        Object toTime = UpstreamPortHarness.createFunc(classes, "ToTime");
        assertEquals(LocalTime.of(15, 7, 42), UpstreamPortHarness.invokeFunc(toTime, LocalTime.class, "15:07:42"));
        assertNull(UpstreamPortHarness.invokeFunc(toTime, LocalTime.class, "42:00:00"));
        assertEquals(LocalTime.of(23, 7, 0), UpstreamPortHarness.invokeFunc(toTime, LocalTime.class, "23:07"));
        Object numberToString = UpstreamPortHarness.createFunc(classes, "NumberToString");
        assertEquals("3.14", UpstreamPortHarness.invokeFunc(numberToString, String.class, BigDecimal.valueOf(3.14)));
        assertEquals("-42", UpstreamPortHarness.invokeFunc(numberToString, String.class, BigDecimal.valueOf(-42)));
        Object timeToString = UpstreamPortHarness.createFunc(classes, "TimeToString");
        assertEquals("15:07:42", UpstreamPortHarness.invokeFunc(timeToString, String.class, LocalTime.of(15, 7, 42)));
        assertEquals("23:07", UpstreamPortHarness.invokeFunc(timeToString, String.class, LocalTime.of(23, 7, 0)));
    }

    /** Upstream {@code recordConversionTest}. */
    @Test
    void recordConversionTest() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func ToDate:
                	inputs: input string (1..1)
                	output: result date (1..1)
                	set result:
                		input to-date
                
                func ToDateTime:
                	inputs: input string (1..1)
                	output: result dateTime (1..1)
                	set result:
                		input to-date-time
                
                func ToZonedDateTime:
                	inputs: input string (1..1)
                	output: result zonedDateTime (1..1)
                	set result:
                		input to-zoned-date-time
                
                func DateToString:
                	inputs: input date (1..1)
                	output: result string (1..1)
                	set result:
                		input to-string
                
                func DateTimeToString:
                	inputs: input dateTime (1..1)
                	output: result string (1..1)
                	set result:
                		input to-string
                
                func ZonedDateTimeToString:
                	inputs: input zonedDateTime (1..1)
                	output: result string (1..1)
                	set result:
                		input to-string
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object toDate = UpstreamPortHarness.createFunc(classes, "ToDate");
        String dateStr = "2024-04-18";
        Object dateRes = date(classes, 2024, 4, 18);
        assertEquals(dateRes, UpstreamPortHarness.invokeFunc(toDate, Object.class, dateStr));
        assertNull(UpstreamPortHarness.invokeFunc(toDate, Object.class, "test"));
        Object toDateTime = UpstreamPortHarness.createFunc(classes, "ToDateTime");
        String dateTimeStr = "2024-04-18T13:06:26";
        LocalDateTime dateTimeRes = LocalDateTime.of(2024, 4, 18, 13, 6, 26);
        assertEquals(dateTimeRes, UpstreamPortHarness.invokeFunc(toDateTime, LocalDateTime.class, dateTimeStr));
        assertNull(UpstreamPortHarness.invokeFunc(toDateTime, LocalDateTime.class, "test"));
        Object toZonedDateTime = UpstreamPortHarness.createFunc(classes, "ToZonedDateTime");
        String zonedDateTimeStr1 = "2024-04-18T13:06:26+02:00[Europe/Brussels]";
        ZonedDateTime zonedDateTimeRes1 = ZonedDateTime.of(2024, 4, 18, 13, 6, 26, 0, ZoneId.of("Europe/Brussels"));
        String zonedDateTimeStr2 = "2024-04-18T11:06:26Z";
        ZonedDateTime zonedDateTimeRes2 = ZonedDateTime.of(2024, 4, 18, 11, 6, 26, 0, ZoneId.of("Z"));
        assertEquals(zonedDateTimeRes1, UpstreamPortHarness.invokeFunc(toZonedDateTime, ZonedDateTime.class, zonedDateTimeStr1));
        assertNull(UpstreamPortHarness.invokeFunc(toZonedDateTime, ZonedDateTime.class, "test"));
        assertEquals(zonedDateTimeRes2, UpstreamPortHarness.invokeFunc(toZonedDateTime, ZonedDateTime.class, zonedDateTimeStr2));
        Object dateToString = UpstreamPortHarness.createFunc(classes, "DateToString");
        assertEquals(dateStr, UpstreamPortHarness.invokeFunc(dateToString, String.class, dateRes));
        Object dateTimeToString = UpstreamPortHarness.createFunc(classes, "DateTimeToString");
        assertEquals(dateTimeStr, UpstreamPortHarness.invokeFunc(dateTimeToString, String.class, dateTimeRes));
        Object zonedDateTimeToString = UpstreamPortHarness.createFunc(classes, "ZonedDateTimeToString");
        assertEquals(zonedDateTimeStr1, UpstreamPortHarness.invokeFunc(zonedDateTimeToString, String.class, zonedDateTimeRes1));
        assertEquals(zonedDateTimeStr2, UpstreamPortHarness.invokeFunc(zonedDateTimeToString, String.class, zonedDateTimeRes2));
    }

    /** Upstream {@code testSingularFilterOperation}. */
    @Test
    void testSingularFilterOperation() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func NonZero:
                	inputs:
                		input int (0..1)
                	output:
                		result int (0..1)
                	set result:
                		input filter item <> 0
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "NonZero");
        assertEquals(42, UpstreamPortHarness.invokeFunc(func, Integer.class, 42));
        assertNull(UpstreamPortHarness.invokeFunc(func, Integer.class, 0));
    }

    /**
     * Upstream {@code testJavaLangNames} — leg-C finding #30 HEALED at the #436
     * singles wave (facets javaLangSelfFqn — the basic output type renders
     * {@code java.lang.Boolean} at every type seat inside the same-named class —
     * + fnOutputNameEscape's class-name arm — the output variable escapes
     * {@code _Boolean}; the func-java-lang-self oracle group, byte-identical).
     */
    @Test
    void testJavaLangNames() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func Boolean:
                	output:
                		Boolean boolean (1..1)
                	set Boolean:
                		True extract [ False ]
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /**
     * Upstream {@code testJavaKeywordNames} — leg-C finding #31 HEALED at the #436
     * singles wave (facet fnOutputNameEscape's keyword arm: the keyword-named
     * output escapes {@code _static} at every identifier seat — decl, params,
     * assign, javadoc; the func-java-keyword-attr oracle group, byte-identical).
     */
    @Test
    void testJavaKeywordNames() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func This:
                	output:
                		static int (1..1)
                	set static:
                		42
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code testAccessToDateMembers}. */
    @Test
    void testAccessToDateMembers() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func GetDay:
                	inputs:
                		d date (1..1)
                	output:
                		result int (1..1)
                	set result:
                		d -> day
                
                func GetMonth:
                	inputs:
                		d date (1..1)
                	output:
                		result int (1..1)
                	set result:
                		d -> month
                
                func GetYear:
                	inputs:
                		d date (1..1)
                	output:
                		result int (1..1)
                	set result:
                		d -> year
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object getDay = UpstreamPortHarness.createFunc(classes, "GetDay");
        Object getMonth = UpstreamPortHarness.createFunc(classes, "GetMonth");
        Object getYear = UpstreamPortHarness.createFunc(classes, "GetYear");
        Object d = date(classes, 2023, 1, 19);
        assertEquals(19, UpstreamPortHarness.invokeFunc(getDay, Integer.class, d));
        assertEquals(1, UpstreamPortHarness.invokeFunc(getMonth, Integer.class, d));
        assertEquals(2023, UpstreamPortHarness.invokeFunc(getYear, Integer.class, d));
    }

    /**
     * Upstream {@code testAccessToDateTimeMembers} — leg-C finding #24 HEALED at
     * the #434 wave (facet recordTimeMemberNav: {@code dt -> time} renders the
     * bare method ref {@code .<LocalTime>map("Time", LocalDateTime::toLocalTime)};
     * the {@code date} member was already byte-correct via the PR-147 arm —
     * oracle-byte-identical, the func-record-datetime-members group).
     */
    @Test
    void testAccessToDateTimeMembers() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func GetDate:
                	inputs:
                		dt dateTime (1..1)
                	output:
                		result date (1..1)
                	set result:
                		dt -> date

                func GetTime:
                	inputs:
                		dt dateTime (1..1)
                	output:
                		result time (1..1)
                	set result:
                		dt -> time
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object getDate = UpstreamPortHarness.createFunc(classes, "GetDate");
        Object getTime = UpstreamPortHarness.createFunc(classes, "GetTime");
        Object date = recordDate(classes, 2023, 1, 19);
        LocalTime time = LocalTime.of(11, 2);
        LocalDateTime dt = LocalDateTime.of(toLocalDate(classes, date), time);
        assertEquals(date, UpstreamPortHarness.invokeFunc(getDate, Object.class, dt));
        assertEquals(time, UpstreamPortHarness.invokeFunc(getTime, LocalTime.class, dt));
    }

    /**
     * Upstream {@code testAccessToZonedDateTimeMembers} — leg-C finding #24 HEALED
     * at the #434 wave (facets recordTimeMemberNav + recordTimezoneMemberNav:
     * {@code zdt -> time} renders {@code .<LocalTime>map("Time",
     * ZonedDateTime::toLocalTime)} and {@code zdt -> timezone} the lambda
     * {@code .<String>map("Timezone", _zdt -> _zdt.getZone().getId())}; the
     * {@code date} member was already byte-correct via the PR-147 arm —
     * oracle-byte-identical, the func-record-zoned-members group).
     */
    @Test
    void testAccessToZonedDateTimeMembers() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func GetDate:
                	inputs:
                		zdt zonedDateTime (1..1)
                	output:
                		result date (1..1)
                	set result:
                		zdt -> date

                func GetTime:
                	inputs:
                		zdt zonedDateTime (1..1)
                	output:
                		result time (1..1)
                	set result:
                		zdt -> time
                \t\t\t\t
                func GetZone:
                	inputs:
                		zdt zonedDateTime (1..1)
                	output:
                		result string (1..1)
                	set result:
                		zdt -> timezone
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object getDate = UpstreamPortHarness.createFunc(classes, "GetDate");
        Object getTime = UpstreamPortHarness.createFunc(classes, "GetTime");
        Object getZone = UpstreamPortHarness.createFunc(classes, "GetZone");
        Object date = recordDate(classes, 2023, 1, 19);
        LocalTime time = LocalTime.of(11, 2);
        String zone = "Europe/Paris";
        ZonedDateTime zdt = ZonedDateTime.of(toLocalDate(classes, date), time, ZoneId.of(zone));
        assertEquals(date, recordDate(classes, zdt.toLocalDate()));
        assertEquals(time, zdt.toLocalTime());
        assertEquals(zone, zdt.getZone().getId());
        assertEquals(date, UpstreamPortHarness.invokeFunc(getDate, Object.class, zdt));
        assertEquals(time, UpstreamPortHarness.invokeFunc(getTime, LocalTime.class, zdt));
        assertEquals(zone, UpstreamPortHarness.invokeFunc(getZone, String.class, zdt));
    }

    /**
     * Upstream {@code mayDoRecursiveCalls} — leg-C finding #28 HEALED at the #436
     * singles wave (facets zeroArgCallSetAssign — the whole-output SET assigns the
     * bare argless call, {@code result = rec.evaluate();}, no spurious
     * {@code .get()} — + zeroArgCallAliasWrap — the alias return wraps
     * {@code MapperS.of(rec.evaluate())}; the fault was the ZERO-ARG call form,
     * not recursion — the non-self twin probe reproduced both faces; the
     * func-self-call oracle group, byte-identical).
     */
    @Test
    void mayDoRecursiveCalls() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func Rec:
                	output: result int (1..1)
                	alias test: Rec()
                	set result: Rec()
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code nestedInlineFunctionsTest}. */
    @Test
    void nestedInlineFunctionsTest() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                func F1:
                	output:
                		result int (1..1)
                \t
                	set result:
                		1 extract [
                			item then extract param1 [
                				10 extract [
                					item then extract param2 [
                						100 extract [
                							item*10
                						] then extract [
                							item + param1 + param2
                						]
                					]
                				]
                			]
                		]
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func1 = UpstreamPortHarness.createFunc(classes, "F1");
        assertEquals(1011, UpstreamPortHarness.invokeFunc(func1, Object.class));
    }

    /** Upstream {@code directlyUseAttributesOfImplicitVariableTest}. */
    @Test
    void directlyUseAttributesOfImplicitVariableTest() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                type Foo:
                	a int (1..1)
                	b string (0..*)
                
                func F1:
                	inputs:
                		foos Foo (0..*)
                	output:
                		result int (0..*)
                \t
                	add result:
                		foos
                			extract [ a + b count]
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "F1");
        Object foo1 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo",
                Map.of("a", 42, "b", List.of()));
        Object foo2 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo",
                Map.of("a", -5, "b", List.of("Hello", "World!")));
        assertEquals(List.of(42, -3), UpstreamPortHarness.invokeFunc(func, List.class, List.of(foo1, foo2)));
    }

    /**
     * Upstream {@code omittedParameterInFunctionalOperationTest} — leg-C finding #38
     * HEALED at the #437 board-clearing wave (facet omittedParamBinding: the
     * left-less multiplicative materialises the synthetic implicit item as its left
     * operand — upstream's derived-state law — so {@code [* 2]} is {@code item * 2},
     * not the unary-sign constant fold; oracle-byte-identical, the
     * func-omitted-param group).
     */
    @Test
    void omittedParameterInFunctionalOperationTest() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"

                func F1:
                	inputs:
                		a int (0..*)
                	output:
                		result int (0..*)
                \t
                	add result:
                		a extract [* 2]
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "F1");
        assertEquals(List.of(2, 4, 6), UpstreamPortHarness.invokeFunc(func, List.class, List.of(1, 2, 3)));
    }

    /**
     * Upstream {@code namedFunctionInFunctionalOperationTest} — leg-C finding #26
     * HEALED at the #436 singles wave (facets pointFreeLibraryFnRef — the builtin
     * {@code extract IsLeapYear} lowers to the guarded braced item lambda via the
     * shared #369 IsLeapYear block — + itemLambdaParamRawRead — the mapItem
     * explicit closure param reads raw, killing the F5 double-wrap; F1/F3/F4 had
     * healed collaterally by #429–#435; the func-named-func-ref oracle group, 8
     * goldens byte-identical).
     */
    @Test
    void namedFunctionInFunctionalOperationTest() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                func Incr:
                	inputs:
                		a int (1..1)
                	output:
                		result int (1..1)
                \t
                	set result:
                		a + 1
                
                func IsAnswerToTheUniverse:
                	inputs:
                		a int (1..1)
                	output:
                		result boolean (1..1)
                \t
                	set result:
                		a = 42
                
                func ClosestToTen:
                	inputs:
                		a int (1..1)
                		b int (1..1)
                	output:
                		result int (1..1)
                \t
                	set result:
                		if a < 10 then
                			if b < 10 then
                				if a > b then a else b
                			else
                				if 10 - a < b - 10 then a else b
                		else
                			if b < 10 then
                				if a - 10 < 10 - b then a else b
                			else
                				if a < b then a else b
                
                func F1:
                	inputs:
                		list int (0..*)
                	output:
                		res int (0..*)
                \t
                	add res:
                		list
                			extract Incr
                
                func F2:
                	inputs:
                		list int (0..*)
                	output:
                		res boolean (0..*)
                \t
                	add res:
                		list
                			extract IsLeapYear
                
                func F3:
                	inputs:
                		list int (0..*)
                	output:
                		res int (0..*)
                \t
                	add res:
                		list
                			filter IsAnswerToTheUniverse
                
                func F4:
                	inputs:
                		list int (0..*)
                	output:
                		res int (1..1)
                \t
                	set res:
                		list
                			reduce acc, v [ ClosestToTen(acc, v) ]
                
                func F5:
                	inputs:
                		list int (0..*)
                	output:
                		res int (0..*)
                \t
                	add res:
                		list
                			extract Incr
                			then extract Incr
                			then extract item + 1
                			then extract a [ a extract Incr ]
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);

        Object func1 = UpstreamPortHarness.createFunc(classes, "F1");
        assertEquals(List.of(2, 3, 4),
                UpstreamPortHarness.invokeFunc(func1, List.class, List.of(1, 2, 3)));

        Object func2 = UpstreamPortHarness.createFunc(classes, "F2");
        assertEquals(List.of(true, false, false),
                UpstreamPortHarness.invokeFunc(func2, List.class, List.of(2000, 2001, 2002)));

        Object func3 = UpstreamPortHarness.createFunc(classes, "F3");
        assertEquals(List.of(42, 42),
                UpstreamPortHarness.invokeFunc(func3, List.class, List.of(1, 2, 42, 3, 42)));

        Object func4 = UpstreamPortHarness.createFunc(classes, "F4");
        assertEquals(8, UpstreamPortHarness.invokeFunc(func4, Integer.class, List.of(0, 5, 8)));
        assertEquals(11, UpstreamPortHarness.invokeFunc(func4, Integer.class, List.of(0, 5, 8, 11, 15)));

        Object func5 = UpstreamPortHarness.createFunc(classes, "F5");
        assertEquals(List.of(5, 6, 7),
                UpstreamPortHarness.invokeFunc(func5, List.class, List.of(1, 2, 3)));
    }

    /** Upstream {@code emptyArgumentTest}. */
    @Test
    void emptyArgumentTest() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                func F1:
                	output:
                		res int (1..1)
                	set res:
                		F2(empty)
                
                func F2:
                	inputs:
                		a int (0..1)
                	output:
                		res int (1..1)
                	set res:
                		42
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code thenOperationTest}. */
    @Test
    void thenOperationTest() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"

                func F1:
                	output:
                		res boolean (1..1)
                	set res:
                		empty then item = empty

                func F2:
                	output:
                		res int (1..1)
                	set res:
                		42 then item + item

                func F3:
                	output:
                		res int (2..2)
                	set res:
                		[1, 2, 3] then [ [item count, item sum] ]

                func F4:
                	output:
                		res int (2..2)
                	set res:
                		[1, 2, 3]
                			extract [ [item, item] ]
                			then extract l [ l count ]

                func F5:
                	output:
                		res int (2..2)
                	set res:
                		[1, 2, 3]
                			extract [ [item, item] ]
                			then extract l [ [ l count, l sum ] ]
                			then extract l [ l sum ]
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func1 = UpstreamPortHarness.createFunc(classes, "F1");
        assertTrue(UpstreamPortHarness.invokeFunc(func1, Boolean.class));
        Object func2 = UpstreamPortHarness.createFunc(classes, "F2");
        assertEquals(84, UpstreamPortHarness.invokeFunc(func2, Integer.class));
        Object func3 = UpstreamPortHarness.createFunc(classes, "F3");
        assertEquals(List.of(3, 6), UpstreamPortHarness.invokeFunc(func3, List.class));
        Object func4 = UpstreamPortHarness.createFunc(classes, "F4");
        assertEquals(List.of(2, 2, 2), UpstreamPortHarness.invokeFunc(func4, List.class));
        Object func5 = UpstreamPortHarness.createFunc(classes, "F5");
        assertEquals(List.of(4, 6, 8), UpstreamPortHarness.invokeFunc(func5, List.class));
    }

    /** Upstream {@code singularExtractTest}. */
    @Test
    void singularExtractTest() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                func F1:
                	output:
                		res int (1..1)
                	set res:
                		42
                			extract [item + 1]
                
                func F2:
                	output:
                		res boolean (1..1)
                	set res:
                		42
                			extract item + 1
                			then extract item = 42
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func1 = UpstreamPortHarness.createFunc(classes, "F1");
        assertEquals(43, UpstreamPortHarness.invokeFunc(func1, Integer.class));
        Object func2 = UpstreamPortHarness.createFunc(classes, "F2");
        assertFalse(UpstreamPortHarness.invokeFunc(func2, Boolean.class));
    }

    /** Upstream {@code largeNumberTest}. */
    @Test
    void largeNumberTest() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                func F1:
                	output:
                		res number (1..1)
                	set res:
                		99999999999999999999.99999
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "F1");
        assertEquals(new BigDecimal("99999999999999999999.99999"), UpstreamPortHarness.invokeFunc(func, Number.class));
    }

    /** Upstream {@code testPreconditionValidGeneration}. */
    @Test
    void testPreconditionValidGeneration() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                	inputs:
                		a int (1..1)
                	output:
                		result int (1..1)
                \t
                	condition PositiveArgument:
                		if True then a = 0
                \t
                	set result:
                		a
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code testExpressionValidGeneration}. */
    @Test
    void testExpressionValidGeneration() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type A:
                	a int (0..1)
                
                func FuncFoo:
                	inputs:
                		a A (0..*)
                	output:
                		result A (0..*)
                \t
                	set result:
                		a filter [item->a exists]
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code testSimpleFunctionGeneration}. */
    @Test
    void testSimpleFunctionGeneration() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		name string  (0..1)
                 		name2 string (0..1)
                	output:
                		result string (0..1)
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.RosettaFunction;
                
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                
                	/**
                	* @param name\s
                	* @param name2\s
                	* @return result\s
                	*/
                	public String evaluate(String name, String name2) {
                		String result = doEvaluate(name, name2);
                \t\t
                		return result;
                	}
                
                	protected abstract String doEvaluate(String name, String name2);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected String doEvaluate(String name, String name2) {
                			String result = null;
                			return assignOutput(result, name, name2);
                		}
                \t\t
                		protected String assignOutput(String result, String name, String name2) {
                			return result;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldGenerateFunctionWithStringListOutput}. */
    @Test
    void shouldGenerateFunctionWithStringListOutput() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		name string  (0..1)
                 		name2 string (0..1)
                	output:
                		result string (0..*)
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import java.util.ArrayList;
                import java.util.List;
                
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                
                	/**
                	* @param name\s
                	* @param name2\s
                	* @return result\s
                	*/
                	public List<String> evaluate(String name, String name2) {
                		List<String> result = doEvaluate(name, name2);
                \t\t
                		return result;
                	}
                
                	protected abstract List<String> doEvaluate(String name, String name2);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<String> doEvaluate(String name, String name2) {
                			List<String> result = new ArrayList<>();
                			return assignOutput(result, name, name2);
                		}
                \t\t
                		protected List<String> assignOutput(List<String> result, String name, String name2) {
                			return result;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldGenerateFunctionWithNumberListOutput}. */
    @Test
    void shouldGenerateFunctionWithNumberListOutput() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		name string  (0..1)
                 		name2 string (0..1)
                	output:
                		result number (0..*)
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import java.math.BigDecimal;
                import java.util.ArrayList;
                import java.util.List;
                
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                
                	/**
                	* @param name\s
                	* @param name2\s
                	* @return result\s
                	*/
                	public List<BigDecimal> evaluate(String name, String name2) {
                		List<BigDecimal> result = doEvaluate(name, name2);
                \t\t
                		return result;
                	}
                
                	protected abstract List<BigDecimal> doEvaluate(String name, String name2);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<BigDecimal> doEvaluate(String name, String name2) {
                			List<BigDecimal> result = new ArrayList<>();
                			return assignOutput(result, name, name2);
                		}
                \t\t
                		protected List<BigDecimal> assignOutput(List<BigDecimal> result, String name, String name2) {
                			return result;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldGenerateFunctionWithIntListOutput}. */
    @Test
    void shouldGenerateFunctionWithIntListOutput() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		name string  (0..1)
                 		name2 string (0..1)
                	output:
                		result int (0..*)
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import java.util.ArrayList;
                import java.util.List;
                
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                
                	/**
                	* @param name\s
                	* @param name2\s
                	* @return result\s
                	*/
                	public List<Integer> evaluate(String name, String name2) {
                		List<Integer> result = doEvaluate(name, name2);
                \t\t
                		return result;
                	}
                
                	protected abstract List<Integer> doEvaluate(String name, String name2);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<Integer> doEvaluate(String name, String name2) {
                			List<Integer> result = new ArrayList<>();
                			return assignOutput(result, name, name2);
                		}
                \t\t
                		protected List<Integer> assignOutput(List<Integer> result, String name, String name2) {
                			return result;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldGenerateFunctionWithDateListOutput}. */
    @Test
    void shouldGenerateFunctionWithDateListOutput() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		name string  (0..1)
                 		name2 string (0..1)
                	output:
                		result date (0..*)
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.records.Date;
                import java.util.ArrayList;
                import java.util.List;
                
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                
                	/**
                	* @param name\s
                	* @param name2\s
                	* @return result\s
                	*/
                	public List<Date> evaluate(String name, String name2) {
                		List<Date> result = doEvaluate(name, name2);
                \t\t
                		return result;
                	}
                
                	protected abstract List<Date> doEvaluate(String name, String name2);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<Date> doEvaluate(String name, String name2) {
                			List<Date> result = new ArrayList<>();
                			return assignOutput(result, name, name2);
                		}
                \t\t
                		protected List<Date> assignOutput(List<Date> result, String name, String name2) {
                			return result;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldGenerateFuncWithAssignOutputDoIfBooleanLiterals}. */
    @Test
    void shouldGenerateFuncWithAssignOutputDoIfBooleanLiterals() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func Foo:
                	inputs:
                		foo int (0..1)
                	output:\s
                		result boolean (1..1)
                \t\t
                	set result:\s
                		if foo exists
                		then False
                		else True
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldGenerateFuncWithAssignOutputDoIfBooleanLiteralsAndNoElse}. */
    @Test
    void shouldGenerateFuncWithAssignOutputDoIfBooleanLiteralsAndNoElse() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func Foo:
                	inputs:
                		foo int (0..1)
                	output:\s
                		result boolean (1..1)
                \t\t
                	set result:\s
                		if foo exists
                		then False
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldGenerateFuncWithAssignOutputDoIfFuncCall}. */
    @Test
    void shouldGenerateFuncWithAssignOutputDoIfFuncCall() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func Bar:
                	inputs:
                		bar number (0..1)
                	output:\s
                		result number (1..1)
                
                func Foo:
                	inputs:
                		foo number (0..1)
                	output:\s
                		result number (1..1)
                \t
                	set result:\s
                		if foo exists
                		then Bar( foo )
                		else 0.0
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldGenerateFuncWithAssignOutputDoIfFuncCallAndElseBoolean}. */
    @Test
    void shouldGenerateFuncWithAssignOutputDoIfFuncCallAndElseBoolean() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func Bar:
                	inputs:
                		bar number (0..1)
                	output:\s
                		result boolean (1..1)
                
                func Foo:
                	inputs:
                		foo number (0..1)
                	output:\s
                		result boolean (1..1)
                \t
                	set result:\s
                		if foo exists
                		then Bar( foo )
                		else True
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldGenerateFuncWithAssignOutputDoIfFuncCallAndNoElse}. */
    @Test
    void shouldGenerateFuncWithAssignOutputDoIfFuncCallAndNoElse() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func Bar:
                	inputs:
                		bar number (0..1)
                	output:\s
                		result boolean (1..1)
                
                func Foo:
                	inputs:
                		foo number (0..1)
                	output:\s
                		result boolean (1..1)
                \t
                	set result:\s
                		if foo exists
                		then Bar( foo )
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldGenerateFuncWithAssignOutputDoIfBigDecimalAndFeatureCall}. */
    @Test
    void shouldGenerateFuncWithAssignOutputDoIfBigDecimalAndFeatureCall() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Bar:
                	baz number (1..1)
                
                func Foo:
                	inputs:
                		bar Bar (0..1)
                	output:\s
                		result number (1..1)
                \t
                	set result:\s
                		if bar exists
                		then 30.0
                		else bar -> baz
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldGenerateFuncWithAssignOutputDoIfComparisonResultAndElseBoolean}. */
    @Test
    void shouldGenerateFuncWithAssignOutputDoIfComparisonResultAndElseBoolean() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Bar:
                	baz number (1..1)
                
                func Foo:
                	inputs:
                		bar Bar (0..1)
                	output:\s
                		result boolean (1..1)
                \t
                	set result:\s
                		if bar -> baz exists
                		then bar -> baz > 5
                		else True
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldGenerateFuncWithAssignOutputDoIfComparisonResultAndNoElse}. */
    @Test
    void shouldGenerateFuncWithAssignOutputDoIfComparisonResultAndNoElse() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Bar:
                	baz number (1..1)
                
                func Foo:
                	inputs:
                		bar Bar (0..1)
                	output:\s
                		result boolean (1..1)
                \t
                	set result:\s
                		if bar -> baz exists
                		then bar -> baz > 5
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldGenerateFuncWithNestedBooleanExpressionCondition}. */
    @Test
    void shouldGenerateFuncWithNestedBooleanExpressionCondition() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Money:
                	amount number (1..1)
                	currency string (1..1)
                
                func Foo:
                	inputs:
                		m1 Money  (0..1)
                		m2 Money (0..1)
                		currency string (0..1)
                	output:
                		result string (0..1)
                \t
                	condition:
                		[ m1 -> currency , m2 -> currency ] any = currency
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldGenerateFuncWithKeyReferenceFromAnotherNamespace}. */
    @Test
    void shouldGenerateFuncWithKeyReferenceFromAnotherNamespace() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model.party
                version "test"
                
                type Party:
                	[metadata key]
                	id number (1..1)
                	name string (1..1)
                """, """
                namespace com.rosetta.test.model.agreement
                version "test"
                
                import com.rosetta.test.model.party.*
                
                type Agreement:
                	id number (1..1)
                	party Party (1..1)
                		[metadata reference]
                """, """
                namespace "com.rosetta.test.model.func"
                version "test"
                
                import com.rosetta.test.model.party.*
                import com.rosetta.test.model.agreement.*
                
                func Create_Agreement:
                 	inputs:
                 		party Party (1..1)
                 	id number (1..1)
                	output:
                		agreement Agreement (1..1)
                
                	set agreement -> id: id
                	set agreement -> party: party as-key
                
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldGenerateFunctionWithAssignemtnAsReference} — upstream-@Disabled, carried (generation-only body; never runs). */
    @Test
    @Disabled("upstream-@Disabled; carried (generation-only body; probe-measured GEN_OK/COMPILE_OK on the fork)")
    void shouldGenerateFunctionWithAssignemtnAsReference() {
        UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model.party
                version "test"
                
                type Party:
                	id number (1..1)
                	name MyData (1..1)
                
                type MyData:
                	val string (1..1)
                """, """
                namespace com.rosetta.test.model.agreement
                version "test"
                
                import com.rosetta.test.model.party.*
                
                type Agreement:
                	id number (1..1)
                	party Party (1..1)
                
                	condition AgreementValid:
                	if Get_Party_Id() exists
                		then id is absent
                
                func Get_Party_Id:
                 	inputs:
                 		agreement Agreement (1..1)
                	output:
                		result MyData (1..1)
                
                	set result : agreement -> party -> name
                
                
                """);
    }

    /** Upstream {@code shouldGenerateFunctionWithAssignmentAsMeta} — upstream-@Disabled, carried (generation-only body; never runs). */
    @Test
    @Disabled("upstream-@Disabled; carried (generation-only body; probe-measured GEN_OK/COMPILE_OK on the fork)")
    void shouldGenerateFunctionWithAssignmentAsMeta() {
        UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model.party
                version "test"
                
                type Party:
                	id number (1..1)
                	name string (1..1)
                
                type MyData:
                	val Party (1..1)
                		[metadata id]
                """, """
                namespace com.rosetta.test.model.agreement
                version "test"
                
                import com.rosetta.test.model.party.*
                
                type Agreement:
                	id number (1..1)
                	party Party (1..1)
                		[metadata id]
                
                	condition AgreementValid:
                		if Get_Party_Id() exists
                			then id is absent
                
                func Get_Party_Id:
                 	inputs:
                 		agreement Agreement (1..1)
                	output:
                		result MyData (1..1)
                
                	set result-> val : agreement -> party
                
                
                """);
    }

    /** Upstream {@code shouldGenerateFunctionWithConditionalAssignment}. */
    @Test
    void shouldGenerateFunctionWithConditionalAssignment() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model.agreement
                version "test"
                
                type Top:
                	foo Foo (1..*)
                
                type Foo:
                	bar1 Bar (0..1)
                	bar2 Bar (0..1)
                
                type Bar:
                	id number (1..1)
                
                func ExtractBar: <"Extracts a bar">
                	inputs: top Top (1..1)
                	output: bar Bar (1..1)
                	alias foo: top -> foo  only-element
                	set bar:
                		if foo -> bar1 exists then foo -> bar1
                		//else if foo -> bar2 exists then foo -> bar2
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /**
     * Upstream {@code shouldGenerateFunctionWithCreationLHSUsingAlias} — leg-C finding #25
     * HEALED at the #435 alias wave (facets aliasRootedSetSegmentResolution +
     * aliasRootedSetFusedTargetPrefix: the alias-rooted SET target fuses the alias
     * body's nav onto one line — {@code topOut.getOrCreateFoo()} — and the op's own
     * segment resolves against the alias's terminal type; the full-text lock is the
     * upstream expected block verbatim — oracle-byte-identical, the
     * func-alias-creation-lhs group).
     */
    @Test
    void shouldGenerateFunctionWithCreationLHSUsingAlias() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model.agreement
                version "test"

                type Top:
                	foo Foo (1..1)

                type Foo:
                	bar1 Bar (0..1)
                	bar2 Bar (0..1)

                type Bar:
                	id number (1..1)

                func ExtractBar: <"Extracts a bar">
                	inputs: top Top (1..1)
                	output: topOut Top (1..1)
                	alias fooAlias : topOut -> foo
                	set fooAlias -> bar1:
                		top -> foo -> bar1
                	set topOut -> foo -> bar2:
                		top -> foo -> bar2
                """);
        assertEquals("""
                package com.rosetta.test.model.agreement.functions;

                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.ModelObjectValidator;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperS;
                import com.rosetta.test.model.agreement.Bar;
                import com.rosetta.test.model.agreement.Foo;
                import com.rosetta.test.model.agreement.Top;
                import java.util.Optional;
                import javax.inject.Inject;


                @ImplementedBy(ExtractBar.ExtractBarDefault.class)
                public abstract class ExtractBar implements RosettaFunction {
                \t
                	@Inject protected ModelObjectValidator objectValidator;

                	/**
                	* @param top\s
                	* @return topOut\s
                	*/
                	public Top evaluate(Top top) {
                		Top.TopBuilder topOutBuilder = doEvaluate(top);
                \t\t
                		final Top topOut;
                		if (topOutBuilder == null) {
                			topOut = null;
                		} else {
                			topOut = topOutBuilder.build();
                			objectValidator.validate(Top.class, topOut);
                		}
                \t\t
                		return topOut;
                	}

                	protected abstract Top.TopBuilder doEvaluate(Top top);

                	protected abstract Foo.FooBuilder fooAlias(Top.TopBuilder topOut, Top top);

                	public static class ExtractBarDefault extends ExtractBar {
                		@Override
                		protected Top.TopBuilder doEvaluate(Top top) {
                			Top.TopBuilder topOut = Top.builder();
                			return assignOutput(topOut, top);
                		}
                \t\t
                		protected Top.TopBuilder assignOutput(Top.TopBuilder topOut, Top top) {
                			topOut.getOrCreateFoo()
                				.setBar1(MapperS.of(top).<Foo>map("getFoo", _top -> _top.getFoo()).<Bar>map("getBar1", foo -> foo.getBar1()).get());
                \t\t\t
                			topOut
                				.getOrCreateFoo()
                				.setBar2(MapperS.of(top).<Foo>map("getFoo", _top -> _top.getFoo()).<Bar>map("getBar2", foo -> foo.getBar2()).get());
                \t\t\t
                			return Optional.ofNullable(topOut)
                				.map(o -> o.prune())
                				.orElse(null);
                		}
                \t\t
                		@Override
                		protected Foo.FooBuilder fooAlias(Top.TopBuilder topOut, Top top) {
                			return toBuilder(MapperS.of(topOut).<Foo>map("getFoo", _top -> _top.getFoo()).get());
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.agreement.functions.ExtractBar"));
        UpstreamPortHarness.compileToClasses(code);
    }

    /**
     * Upstream {@code shouldGenerateFunctionWithAliasAssignOutput} — leg-C finding #25
     * HEALED at the #435 alias wave (facets aliasRootedSetSegmentResolution +
     * aliasRootedSetFusedTargetPrefix: the two-segment alias body fuses
     * {@code topOut.getOrCreateFoo().getOrCreateBar()} and the {@code id} leaf
     * resolves against the alias's terminal type Bar — killing the C3 meta-pseudo
     * {@code .getOrCreateMeta().setExternalKey} mis-render; upstream's own assert
     * is the compile — oracle-byte-identical, the func-alias-assign-output group).
     */
    @Test
    void shouldGenerateFunctionWithAliasAssignOutput() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model.agreement
                version "test"

                type Top:
                	foo Foo (1..1)

                type Foo:
                	bar Bar (0..1)

                type Bar:
                	id number (1..1)

                func UpdateBarId: <"Updates Bar.id by set on an alias">
                	inputs:
                		top Top (1..1)
                		newId number (1..1)

                	output:
                		topOut Top (1..1)

                	alias barAlias :
                		topOut -> foo -> bar

                	set barAlias -> id:
                		newId
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldGenerateDisjoint}. */
    @Test
    void shouldGenerateDisjoint() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model.agreement
                version "test"
                
                type Top:
                	foo Foo (1..*)
                
                type Foo:
                	bar1 number (0..1)
                
                func Disjoint: <"checks disjoint">
                	inputs:
                		top1 Top (1..1)
                		top2 Top (1..1)
                
                	output: result boolean (1..1)
                	set result:
                		top1-> foo disjoint top2 -> foo
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /**
     * Upstream {@code shouldNotGenerateDisjointDifferentTypes} — VALID method through the slice-2
     * diagnostics channel; the fork REJECTS the model with its own
     * measured message form(s), asserted here.
     * Upstream: upstream assertError(ROSETTA_DISJOINT_EXPRESSION): "Types
     * `Foo` and `string` are not comparable".
     */
    @Test
    void shouldNotGenerateDisjointDifferentTypes() {
        var p = UpstreamExpressionPortSupport.parse(List.of("""
                namespace com.rosetta.test.model.agreement
                version "test"
                
                type Top:
                	foo Foo (1..*)
                	bar string (1..*)
                
                type Foo:
                	bar1 number (0..1)
                
                func ExtractBar: <"tries disjoint differnt types">
                	inputs:\s
                		top1 Top (1..1)
                		top2 Top (1..1)
                \t
                	output: result boolean (1..1)
                	set result:
                		top1-> foo disjoint top2 -> bar
                """), List.of(), "True");
        var contextValidation = p.ws().validationDiagnostics().stream()
                .filter(d -> d.range().file().startsWith("expression-port-context-"))
                .toList();
        assertEquals(1, contextValidation.size(),
                "the upstream comparable check's single error (both operands multi — no "
                + "cardinality warnings); got: " + contextValidation);
        assertTrue(contextValidation.stream().anyMatch(d -> d.message().equals(
                        "Types `Foo` and `string` are not comparable")),
                "PR #454 (the Annex-A equality/contains/disjoint family): the fork-native "
                + "'Incompatible types' bytes flipped to upstream's comparableTypeCheck "
                + "message; got: " + contextValidation);
    }

    /**
     * Upstream {@code shouldNotAndInts} — VALID method through the slice-2
     * diagnostics channel; the fork REJECTS the model with its own
     * measured message form(s), asserted here.
     * Upstream: upstream assertError(LOGICAL_OPERATION): "Expected type
     * `boolean`, but got `Foo` instead. Cannot use `Foo` with operator
     * `and`".
     */
    @Test
    void shouldNotAndInts() {
        var p = UpstreamExpressionPortSupport.parse(List.of("""
                namespace com.rosetta.test.model.agreement
                version "test"
                
                type Top:
                	foo Foo (1..1)
                
                type Foo:
                	bar1 number (1..1)
                
                func ExtractBar: <"tries anding integers">
                	inputs:\s
                		top1 Top (1..1)
                		top2 Top (1..1)
                \t
                	output: result boolean (1..1)
                \t
                	set result:
                		top1 -> foo and top2 -> foo
                """), List.of(), "True");
        var contextValidation = p.ws().validationDiagnostics().stream()
                .filter(d -> d.range().file().startsWith("expression-port-context-"))
                .toList();
        assertEquals(2, contextValidation.size(),
                "measured fork-native diagnostic count; got: " + contextValidation);
        assertTrue(contextValidation.stream().anyMatch(d -> d.message().equals("Expected boolean for left operand of logical but got 'Foo'")),
                "expected [Expected boolean for left operand of logical but got 'Foo']; got: " + contextValidation);
        assertTrue(contextValidation.stream().anyMatch(d -> d.message().equals("Expected boolean for right operand of logical but got 'Foo'")),
                "expected [Expected boolean for right operand of logical but got 'Foo']; got: " + contextValidation);
    }

    /** Upstream {@code shouldReturnMultiple}. */
    @Test
    void shouldReturnMultiple() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model.agreement
                version "test"
                
                type Top:
                	foo Foo (1..*)
                	foob Foo (1..1)
                
                type Foo:
                	bar1 number (1..*)
                \t
                func ExtractFoo: <"tries returning list of complex">
                	inputs:\s
                		top1 Top (1..1)
                	output:\s
                		result Foo (1..*)
                	add result:
                		top1 -> foo
                
                func ExtractFoowithAlias: <"tries returning list of complex">
                	inputs:\s
                		top1 Top (1..1)
                	output:\s
                		result Foo (0..*)
                	alias foos: top1 -> foo
                	set result:
                		foos
                
                func ExtractBar: <"tries returning list of basic">
                	inputs:\s
                		top1 Top (1..1)
                	output:\s
                		result number (1..*)
                	add result:
                		top1-> foo -> bar1
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code funcCallingMultipleFunc}. */
    @Test
    void funcCallingMultipleFunc() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func F1:
                	inputs: f1Input date (1..1)
                	output: f1OutputList date (1..*)
                \t\t
                func F2:
                	inputs: f2InputList date (1..*)
                	output: f2Output date (1..1)
                \t
                func F3:
                	inputs: f3Input date (1..1)
                	output: f3Output date (1..1)
                	set f3Output: F2(F1(f3Input))
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.records.Date;
                import javax.inject.Inject;
                
                
                @ImplementedBy(F3.F3Default.class)
                public abstract class F3 implements RosettaFunction {
                \t
                	// RosettaFunction dependencies
                	//
                	@Inject protected F1 f1;
                	@Inject protected F2 f2;
                
                	/**
                	* @param f3Input\s
                	* @return f3Output\s
                	*/
                	public Date evaluate(Date f3Input) {
                		Date f3Output = doEvaluate(f3Input);
                \t\t
                		return f3Output;
                	}
                
                	protected abstract Date doEvaluate(Date f3Input);
                
                	public static class F3Default extends F3 {
                		@Override
                		protected Date doEvaluate(Date f3Input) {
                			Date f3Output = null;
                			return assignOutput(f3Output, f3Input);
                		}
                \t\t
                		protected Date assignOutput(Date f3Output, Date f3Input) {
                			f3Output = f2.evaluate(f1.evaluate(f3Input));
                \t\t\t
                			return f3Output;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.F3"));
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code testDelegateFunctionCallWithInputAlias}. */
    @Test
    void testDelegateFunctionCallWithInputAlias() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func F1:
                	inputs: f1Input string (1..1)
                	output: f1Output string (1..1)
                \t
                func F2:
                	inputs: f2Input string (1..1)
                	output: f2Output string (1..1)
                	alias foo: F1(f2Input)
                	set f2Output: foo
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.RosettaFunction;
                
                
                @ImplementedBy(F1.F1Default.class)
                public abstract class F1 implements RosettaFunction {
                
                	/**
                	* @param f1Input\s
                	* @return f1Output\s
                	*/
                	public String evaluate(String f1Input) {
                		String f1Output = doEvaluate(f1Input);
                \t\t
                		return f1Output;
                	}
                
                	protected abstract String doEvaluate(String f1Input);
                
                	public static class F1Default extends F1 {
                		@Override
                		protected String doEvaluate(String f1Input) {
                			String f1Output = null;
                			return assignOutput(f1Output, f1Input);
                		}
                \t\t
                		protected String assignOutput(String f1Output, String f1Input) {
                			return f1Output;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.F1"));
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperS;
                import javax.inject.Inject;
                
                
                @ImplementedBy(F2.F2Default.class)
                public abstract class F2 implements RosettaFunction {
                \t
                	// RosettaFunction dependencies
                	//
                	@Inject protected F1 f1;
                
                	/**
                	* @param f2Input\s
                	* @return f2Output\s
                	*/
                	public String evaluate(String f2Input) {
                		String f2Output = doEvaluate(f2Input);
                \t\t
                		return f2Output;
                	}
                
                	protected abstract String doEvaluate(String f2Input);
                
                	protected abstract MapperS<String> foo(String f2Input);
                
                	public static class F2Default extends F2 {
                		@Override
                		protected String doEvaluate(String f2Input) {
                			String f2Output = null;
                			return assignOutput(f2Output, f2Input);
                		}
                \t\t
                		protected String assignOutput(String f2Output, String f2Input) {
                			f2Output = foo(f2Input).get();
                \t\t\t
                			return f2Output;
                		}
                \t\t
                		@Override
                		protected MapperS<String> foo(String f2Input) {
                			return MapperS.of(f1.evaluate(f2Input));
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.F2"));
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code funcCallingMultipleFunc2}. */
    @Test
    void funcCallingMultipleFunc2() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func F1:
                	inputs: f1Input date (1..1)
                	output: f1OutputList date (1..*)
                \t\t
                func F2:
                	inputs: f2InputList date (1..*)
                	output: f2Output date (1..1)
                \t
                func F3:
                	inputs: f3Input date (1..1)
                	output: f3Output date (1..1)
                	alias f1OutList: F1(f3Input)
                	set f3Output: F2(f1OutList)
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.records.Date;
                import java.util.ArrayList;
                import java.util.List;
                
                
                @ImplementedBy(F1.F1Default.class)
                public abstract class F1 implements RosettaFunction {
                
                	/**
                	* @param f1Input\s
                	* @return f1OutputList\s
                	*/
                	public List<Date> evaluate(Date f1Input) {
                		List<Date> f1OutputList = doEvaluate(f1Input);
                \t\t
                		return f1OutputList;
                	}
                
                	protected abstract List<Date> doEvaluate(Date f1Input);
                
                	public static class F1Default extends F1 {
                		@Override
                		protected List<Date> doEvaluate(Date f1Input) {
                			List<Date> f1OutputList = new ArrayList<>();
                			return assignOutput(f1OutputList, f1Input);
                		}
                \t\t
                		protected List<Date> assignOutput(List<Date> f1OutputList, Date f1Input) {
                			return f1OutputList;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.F1"));
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.records.Date;
                import java.util.Collections;
                import java.util.List;
                
                
                @ImplementedBy(F2.F2Default.class)
                public abstract class F2 implements RosettaFunction {
                
                	/**
                	* @param f2InputList\s
                	* @return f2Output\s
                	*/
                	public Date evaluate(List<Date> f2InputList) {
                		Date f2Output = doEvaluate(f2InputList);
                \t\t
                		return f2Output;
                	}
                
                	protected abstract Date doEvaluate(List<Date> f2InputList);
                
                	public static class F2Default extends F2 {
                		@Override
                		protected Date doEvaluate(List<Date> f2InputList) {
                			if (f2InputList == null) {
                				f2InputList = Collections.emptyList();
                			}
                			Date f2Output = null;
                			return assignOutput(f2Output, f2InputList);
                		}
                \t\t
                		protected Date assignOutput(Date f2Output, List<Date> f2InputList) {
                			return f2Output;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.F2"));
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperC;
                import com.rosetta.model.lib.records.Date;
                import javax.inject.Inject;
                
                
                @ImplementedBy(F3.F3Default.class)
                public abstract class F3 implements RosettaFunction {
                \t
                	// RosettaFunction dependencies
                	//
                	@Inject protected F1 f1;
                	@Inject protected F2 f2;
                
                	/**
                	* @param f3Input\s
                	* @return f3Output\s
                	*/
                	public Date evaluate(Date f3Input) {
                		Date f3Output = doEvaluate(f3Input);
                \t\t
                		return f3Output;
                	}
                
                	protected abstract Date doEvaluate(Date f3Input);
                
                	protected abstract MapperC<Date> f1OutList(Date f3Input);
                
                	public static class F3Default extends F3 {
                		@Override
                		protected Date doEvaluate(Date f3Input) {
                			Date f3Output = null;
                			return assignOutput(f3Output, f3Input);
                		}
                \t\t
                		protected Date assignOutput(Date f3Output, Date f3Input) {
                			f3Output = f2.evaluate(f1OutList(f3Input).getMulti());
                \t\t\t
                			return f3Output;
                		}
                \t\t
                		@Override
                		protected MapperC<Date> f1OutList(Date f3Input) {
                			return MapperC.<Date>of(f1.evaluate(f3Input));
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.F3"));
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code funcCallingMultipleFuncWithAlias}. */
    @Test
    void funcCallingMultipleFuncWithAlias() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace "demo"
                version "${project.version}"
                
                type Number:
                	num number (1..1)
                
                func F1:
                	inputs: num number (1..1)
                	output: numbers Number (1..*)
                
                func F2:
                	inputs: nums number(1..*)
                	output: str string (1..1)
                
                func F3:
                	inputs: num number (1..1)
                	output: str string (1..1)
                
                	alias f1: F1(num)
                	set str: F2(f1 -> num)
                
                func F4:
                	inputs: num number (1..*)
                	output: str string (1..1)
                
                	alias f2: F2(num)
                
                	set str: f2
                
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code typeWithCondition}. */
    @Test
    void typeWithCondition() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace "demo"
                version "${project.version}"
                
                type Foo:
                	bar Bar (1..1)
                
                	condition XXX:
                	if bar -> num exists
                	then bar -> zap contains Zap -> A
                	and if bar -> zap contains Zap -> A
                	then bar -> num exists
                
                type Bar:
                	num number (0..1)
                	zap Zap (1..2)
                
                enum Zap:
                	A B C
                
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /**
     * Upstream {@code funcUsingListEquals} — the recorded validation item's
     * false-NEGATIVE half 1, HEALED at the #437 board-clearing wave (facet
     * listEqualsCardinalityModifier: an unmodified equality over
     * cardinality-mismatched operands errors — upstream
     * ExpressionValidator.checkEqualityOperation, message byte-identical; the
     * faithful cardinality entry sees the disguised {@code t2->nums} multi side).
     * Upstream: assertError(EQUALITY_OPERATION).
     */
    @Test
    void funcUsingListEquals() {
        var p = UpstreamExpressionPortSupport.parse(List.of("""
                namespace "demo"
                version "${project.version}"

                type T1:
                		num number (1..1)
                		nums number (1..*)

                func F1:
                	inputs: t1 T1(1..1)
                			t2 T1(1..1)
                	output: res boolean (1..1)
                	set res: t1->num = t2->nums

                """), List.of(), "True");
        var contextValidation = p.ws().validationDiagnostics().stream()
                .filter(d -> d.range().file().startsWith("expression-port-context-"))
                .toList();
        assertEquals(1, contextValidation.size(),
                "expected exactly the upstream equality-cardinality error: " + contextValidation);
        assertTrue(contextValidation.get(0).message().equals(
                "Operator `=` should specify `all` or `any` when comparing a list to a single value"),
                "upstream's message byte-identical, got: " + contextValidation.get(0).message());
    }

    /** Upstream {@code funcUsingListEqualsAll}. */
    @Test
    void funcUsingListEqualsAll() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                func F1:
                	inputs:
                		s1 string (1..*)
                		s2 string (1..1)
                	output:
                		res boolean (1..1)
                	set res: s1 all = s2
                
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "F1");
        assertTrue(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of("a", "a"), "a"));
        assertFalse(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of("a", "b"), "a"));
        assertFalse(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of("b", "b"), "a"));
    }

    /** Upstream {@code funcUsingListEqualsAny}. */
    @Test
    void funcUsingListEqualsAny() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                func F1:
                	inputs:
                		s1 string (1..*)
                		s2 string (1..1)
                	output:
                		res boolean (1..1)
                	set res: s1 any = s2
                
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "F1");
        assertTrue(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of("a", "a"), "a"));
        assertTrue(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of("a", "b"), "a"));
        assertFalse(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of("b", "b"), "a"));
    }

    /** Upstream {@code funcUsingListComparableEqualsAll}. */
    @Test
    void funcUsingListComparableEqualsAll() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                func F1:
                	inputs:
                		n1 int (1..*)
                		n2 int (1..1)
                	output:
                		res boolean (1..1)
                	set res: n1 all = n2
                
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "F1");
        assertTrue(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of(1, 1), 1));
        assertFalse(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of(1, 2), 1));
        assertFalse(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of(1, 1), 2));
    }

    /** Upstream {@code funcUsingListComparableEqualsAny}. */
    @Test
    void funcUsingListComparableEqualsAny() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                func F1:
                	inputs:
                		n1 int (1..*)
                		n2 int (1..1)
                	output:
                		res boolean (1..1)
                	set res: n1 any = n2
                
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "F1");
        assertTrue(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of(1, 1), 1));
        assertTrue(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of(1, 2), 1));
        assertFalse(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of(1, 1), 2));
    }

    /** Upstream {@code funcUsingZonedDateTimeEquality}. */
    @Test
    void funcUsingZonedDateTimeEquality() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                func F1:
                	inputs:
                		dt1 zonedDateTime (1..1)
                		dt2 zonedDateTime (1..1)
                	output:
                		res boolean (1..1)
                	set res: dt1 = dt2
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "F1");
        ZonedDateTime dt1 = ZonedDateTime.of(2022, 10, 13, 14, 0, 0, 0, ZoneId.of("Europe/Brussels"));
        ZonedDateTime dt2 = ZonedDateTime.of(2022, 10, 13, 14, 0, 0, 0, ZoneId.of("Europe/Brussels"));
        assertTrue(UpstreamPortHarness.invokeFunc(func, Boolean.class, dt1, dt2));
        dt1 = ZonedDateTime.of(2022, 10, 13, 14, 0, 0, 0, ZoneId.of("Europe/Brussels"));
        dt2 = ZonedDateTime.of(2022, 10, 13, 14, 0, 0, 0, ZoneId.of("Europe/London"));
        assertFalse(UpstreamPortHarness.invokeFunc(func, Boolean.class, dt1, dt2));
        dt1 = ZonedDateTime.of(2022, 10, 13, 15, 0, 0, 0, ZoneId.of("Europe/Brussels"));
        dt2 = ZonedDateTime.of(2022, 10, 13, 14, 0, 0, 0, ZoneId.of("Europe/London"));
        assertTrue(UpstreamPortHarness.invokeFunc(func, Boolean.class, dt1, dt2));
    }

    /** Upstream {@code funcUsingListNotEqualsAll}. */
    @Test
    void funcUsingListNotEqualsAll() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                func F1:
                	inputs:
                		s1 string (1..*)
                		s2 string (1..1)
                	output:
                		res boolean (1..1)
                	set res: s1 all <> s2
                
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "F1");
        assertFalse(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of("a", "a"), "a"));
        assertFalse(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of("a", "b"), "a"));
        assertTrue(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of("a", "a"), "b"));
    }

    /** Upstream {@code funcUsingListNotEqualsAny}. */
    @Test
    void funcUsingListNotEqualsAny() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                func F1:
                	inputs:
                		s1 string (1..*)
                		s2 string (1..1)
                	output:
                		res boolean (1..1)
                	set res: s1 any <> s2
                
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "F1");
        assertFalse(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of("a", "a"), "a"));
        assertTrue(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of("a", "b"), "a"));
        assertTrue(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of("a", "a"), "b"));
    }

    /** Upstream {@code funcUsingListComparableNotEqualsAll}. */
    @Test
    void funcUsingListComparableNotEqualsAll() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                func F1:
                	inputs:
                		n1 int (1..*)
                		n2 int (1..1)
                	output:
                		res boolean (1..1)
                	set res: n1 all <> n2
                
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "F1");
        assertFalse(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of(1, 1), 1));
        assertFalse(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of(1, 2), 1));
        assertTrue(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of(1, 1), 2));
    }

    /** Upstream {@code funcUsingListComparableNotEqualsAny}. */
    @Test
    void funcUsingListComparableNotEqualsAny() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                func F1:
                	inputs:
                		n1 int (1..*)
                		n2 int (1..1)
                	output:
                		res boolean (1..1)
                	set res: n1 any <> n2
                
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "F1");
        assertFalse(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of(1, 1), 1));
        assertTrue(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of(1, 2), 1));
        assertTrue(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of(1, 1), 2));
    }

    /** Upstream {@code funcUsingListComparableGreaterThanAll}. */
    @Test
    void funcUsingListComparableGreaterThanAll() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                func F1:
                	inputs:
                		n1 int (1..*)
                		n2 int (1..1)
                	output:
                		res boolean (1..1)
                	set res: n1 all > n2
                
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "F1");
        assertFalse(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of(1, 1), 2));
        assertFalse(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of(1, 3), 2));
        assertTrue(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of(3, 3), 2));
    }

    /** Upstream {@code funcUsingListComparableGreaterThanAny}. */
    @Test
    void funcUsingListComparableGreaterThanAny() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                func F1:
                	inputs:
                		n1 int (1..*)
                		n2 int (1..1)
                	output:
                		res boolean (1..1)
                	set res: n1 any > n2
                
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "F1");
        assertFalse(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of(1, 1), 2));
        assertTrue(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of(1, 3), 2));
        assertTrue(UpstreamPortHarness.invokeFunc(func, Boolean.class, List.of(3, 3), 2));
    }

    /** Upstream {@code funcUsingZonedDateTimeGreaterThan}. */
    @Test
    void funcUsingZonedDateTimeGreaterThan() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                func F1:
                	inputs:
                		dt1 zonedDateTime (1..1)
                		dt2 zonedDateTime (1..1)
                	output:
                		res boolean (1..1)
                	set res: dt1 > dt2
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "F1");
        ZonedDateTime dt1 = ZonedDateTime.of(2022, 10, 13, 14, 0, 0, 0, ZoneId.of("Europe/Brussels"));
        ZonedDateTime dt2 = ZonedDateTime.of(2022, 10, 13, 14, 0, 0, 0, ZoneId.of("Europe/Brussels"));
        assertFalse(UpstreamPortHarness.invokeFunc(func, Boolean.class, dt1, dt2));
        dt1 = ZonedDateTime.of(2022, 10, 13, 14, 0, 0, 0, ZoneId.of("Europe/Brussels"));
        dt2 = ZonedDateTime.of(2022, 10, 13, 14, 0, 0, 0, ZoneId.of("Europe/London"));
        assertFalse(UpstreamPortHarness.invokeFunc(func, Boolean.class, dt1, dt2));
        dt1 = ZonedDateTime.of(2022, 10, 13, 15, 0, 0, 0, ZoneId.of("Europe/Brussels"));
        dt2 = ZonedDateTime.of(2022, 10, 13, 14, 0, 0, 0, ZoneId.of("Europe/London"));
        assertFalse(UpstreamPortHarness.invokeFunc(func, Boolean.class, dt1, dt2));
        dt1 = ZonedDateTime.of(2022, 10, 13, 16, 0, 0, 0, ZoneId.of("Europe/Brussels"));
        dt2 = ZonedDateTime.of(2022, 10, 13, 14, 0, 0, 0, ZoneId.of("Europe/London"));
        assertTrue(UpstreamPortHarness.invokeFunc(func, Boolean.class, dt1, dt2));
    }

    /** Upstream {@code funcUsingZonedDateTimeGreatherThanOrEqual}. */
    @Test
    void funcUsingZonedDateTimeGreatherThanOrEqual() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                func F1:
                	inputs:
                		dt1 zonedDateTime (1..1)
                		dt2 zonedDateTime (1..1)
                	output:
                		res boolean (1..1)
                	set res: dt1 >= dt2
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "F1");
        ZonedDateTime dt1 = ZonedDateTime.of(2022, 10, 13, 14, 0, 0, 0, ZoneId.of("Europe/Brussels"));
        ZonedDateTime dt2 = ZonedDateTime.of(2022, 10, 13, 14, 0, 0, 0, ZoneId.of("Europe/Brussels"));
        assertTrue(UpstreamPortHarness.invokeFunc(func, Boolean.class, dt1, dt2));
        dt1 = ZonedDateTime.of(2022, 10, 13, 14, 0, 0, 0, ZoneId.of("Europe/Brussels"));
        dt2 = ZonedDateTime.of(2022, 10, 13, 14, 0, 0, 0, ZoneId.of("Europe/London"));
        assertFalse(UpstreamPortHarness.invokeFunc(func, Boolean.class, dt1, dt2));
        dt1 = ZonedDateTime.of(2022, 10, 13, 15, 0, 0, 0, ZoneId.of("Europe/Brussels"));
        dt2 = ZonedDateTime.of(2022, 10, 13, 14, 0, 0, 0, ZoneId.of("Europe/London"));
        assertTrue(UpstreamPortHarness.invokeFunc(func, Boolean.class, dt1, dt2));
        dt1 = ZonedDateTime.of(2022, 10, 13, 16, 0, 0, 0, ZoneId.of("Europe/Brussels"));
        dt2 = ZonedDateTime.of(2022, 10, 13, 14, 0, 0, 0, ZoneId.of("Europe/London"));
        assertTrue(UpstreamPortHarness.invokeFunc(func, Boolean.class, dt1, dt2));
    }

    /** Upstream {@code funcWithListOfIntDistinct}. */
    @Test
    void funcWithListOfIntDistinct() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                type Foo:
                	n int (0..*)
                
                func DistinctFunc:
                	inputs:
                		foo Foo (0..1)
                	output:
                		res int (0..*)
                	set res: foo -> n distinct
                
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperS;
                import com.rosetta.test.model.Foo;
                import java.util.ArrayList;
                import java.util.List;
                
                import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;
                
                @ImplementedBy(DistinctFunc.DistinctFuncDefault.class)
                public abstract class DistinctFunc implements RosettaFunction {
                
                	/**
                	* @param foo\s
                	* @return res\s
                	*/
                	public List<Integer> evaluate(Foo foo) {
                		List<Integer> res = doEvaluate(foo);
                \t\t
                		return res;
                	}
                
                	protected abstract List<Integer> doEvaluate(Foo foo);
                
                	public static class DistinctFuncDefault extends DistinctFunc {
                		@Override
                		protected List<Integer> doEvaluate(Foo foo) {
                			List<Integer> res = new ArrayList<>();
                			return assignOutput(res, foo);
                		}
                \t\t
                		protected List<Integer> assignOutput(List<Integer> res, Foo foo) {
                			res = distinct(MapperS.of(foo).<Integer>mapC("getN", _foo -> _foo.getN())).getMulti();
                \t\t\t
                			return res;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.DistinctFunc"));
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "DistinctFunc");
        Object foo = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo",
                Map.of(), Map.of("n", List.of(1, 1, 1, 2, 2, 3)));
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, foo);
        assertEquals(3, res.size());
        assertTrue(res.containsAll(List.of(1, 2, 3)), "hasItems(1, 2, 3)");
    }

    /** Upstream {@code funcWithListOfIntDistinct2}. */
    @Test
    void funcWithListOfIntDistinct2() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                func DistinctFunc:
                	inputs:
                		n int (0..*)
                	output:
                		res int (0..*)
                	set res: n distinct
                
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperC;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                
                import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;
                
                @ImplementedBy(DistinctFunc.DistinctFuncDefault.class)
                public abstract class DistinctFunc implements RosettaFunction {
                
                	/**
                	* @param n\s
                	* @return res\s
                	*/
                	public List<Integer> evaluate(List<Integer> n) {
                		List<Integer> res = doEvaluate(n);
                \t\t
                		return res;
                	}
                
                	protected abstract List<Integer> doEvaluate(List<Integer> n);
                
                	public static class DistinctFuncDefault extends DistinctFunc {
                		@Override
                		protected List<Integer> doEvaluate(List<Integer> n) {
                			if (n == null) {
                				n = Collections.emptyList();
                			}
                			List<Integer> res = new ArrayList<>();
                			return assignOutput(res, n);
                		}
                \t\t
                		protected List<Integer> assignOutput(List<Integer> res, List<Integer> n) {
                			res = distinct(MapperC.<Integer>of(n)).getMulti();
                \t\t\t
                			return res;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.DistinctFunc"));
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "DistinctFunc");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(1, 1, 1, 2, 2, 3));
        assertEquals(3, res.size());
        assertTrue(res.containsAll(List.of(1, 2, 3)), "hasItems(1, 2, 3)");
    }

    /** Upstream {@code funcWithListOfStringDistinct}. */
    @Test
    void funcWithListOfStringDistinct() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                type Foo:
                	n string (0..*)
                
                func DistinctFunc:
                	inputs:
                		foo Foo (0..1)
                	output:
                		res string (0..*)
                	add res: foo -> n distinct
                
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "DistinctFunc");
        Object foo = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo",
                Map.of(), Map.of("n", List.of("1", "1", "1", "2", "2", "3")));
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, foo);
        assertEquals(3, res.size());
        assertTrue(res.containsAll(List.of("1", "2", "3")), "hasItems(1, 2, 3)");
    }

    /** Upstream {@code funcWithListOfStringDistinct2}. */
    @Test
    void funcWithListOfStringDistinct2() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                func DistinctFunc:
                	inputs:
                		n string (0..*)
                	output:
                		res string (0..*)
                	add res: n distinct
                
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "DistinctFunc");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of("1", "1", "1", "2", "2", "3"));
        assertEquals(3, res.size());
        assertTrue(res.containsAll(List.of("1", "2", "3")), "hasItems(1, 2, 3)");
    }

    /** Upstream {@code funcWithListOfComplexTypeDistinct}. */
    @Test
    void funcWithListOfComplexTypeDistinct() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                type Foo:
                	barList Bar (0..*)
                
                type Bar:
                	n int (0..1)
                
                func DistinctFunc:
                	inputs:
                		foo Foo (0..1)
                	output:
                		res Bar (0..*)
                	add res: foo -> barList distinct
                
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.ModelObjectValidator;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperS;
                import com.rosetta.test.model.Bar;
                import com.rosetta.test.model.Foo;
                import java.util.ArrayList;
                import java.util.List;
                import java.util.Optional;
                import java.util.stream.Collectors;
                import javax.inject.Inject;
                
                import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;
                
                @ImplementedBy(DistinctFunc.DistinctFuncDefault.class)
                public abstract class DistinctFunc implements RosettaFunction {
                \t
                	@Inject protected ModelObjectValidator objectValidator;
                
                	/**
                	* @param foo\s
                	* @return res\s
                	*/
                	public List<? extends Bar> evaluate(Foo foo) {
                		List<Bar.BarBuilder> resBuilder = doEvaluate(foo);
                \t\t
                		final List<? extends Bar> res;
                		if (resBuilder == null) {
                			res = null;
                		} else {
                			res = resBuilder.stream().map(Bar::build).collect(Collectors.toList());
                			objectValidator.validate(Bar.class, res);
                		}
                \t\t
                		return res;
                	}
                
                	protected abstract List<Bar.BarBuilder> doEvaluate(Foo foo);
                
                	public static class DistinctFuncDefault extends DistinctFunc {
                		@Override
                		protected List<Bar.BarBuilder> doEvaluate(Foo foo) {
                			List<Bar.BarBuilder> res = new ArrayList<>();
                			return assignOutput(res, foo);
                		}
                \t\t
                		protected List<Bar.BarBuilder> assignOutput(List<Bar.BarBuilder> res, Foo foo) {
                			res.addAll(toBuilder(distinct(MapperS.of(foo).<Bar>mapC("getBarList", _foo -> _foo.getBarList())).getMulti()));
                \t\t\t
                			return Optional.ofNullable(res)
                				.map(o -> o.stream().map(i -> i.prune()).collect(Collectors.toList()))
                				.orElse(null);
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.DistinctFunc"));
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "DistinctFunc");
        Object bar1 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Bar", Map.of("n", 1), Map.of());
        Object bar2 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Bar", Map.of("n", 2), Map.of());
        Object bar3 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Bar", Map.of("n", 3), Map.of());
        Object foo = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo",
                Map.of(), Map.of("barList", List.of(bar1, bar1, bar1, bar2, bar2, bar3)));
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, foo);
        assertEquals(3, res.size());
        assertTrue(res.containsAll(List.of(bar1, bar2, bar3)), "hasItems(bar1, bar2, bar3)");
    }

    /** Upstream {@code funcWithListOfComplexTypeDistinct2}. */
    @Test
    void funcWithListOfComplexTypeDistinct2() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                type Bar:
                	n int (0..1)
                
                func DistinctFunc:
                	inputs:
                		barList Bar (0..*)
                	output:
                		res Bar (0..*)
                	add res: barList distinct
                
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.ModelObjectValidator;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperC;
                import com.rosetta.test.model.Bar;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                import java.util.Optional;
                import java.util.stream.Collectors;
                import javax.inject.Inject;
                
                import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;
                
                @ImplementedBy(DistinctFunc.DistinctFuncDefault.class)
                public abstract class DistinctFunc implements RosettaFunction {
                \t
                	@Inject protected ModelObjectValidator objectValidator;
                
                	/**
                	* @param barList\s
                	* @return res\s
                	*/
                	public List<? extends Bar> evaluate(List<? extends Bar> barList) {
                		List<Bar.BarBuilder> resBuilder = doEvaluate(barList);
                \t\t
                		final List<? extends Bar> res;
                		if (resBuilder == null) {
                			res = null;
                		} else {
                			res = resBuilder.stream().map(Bar::build).collect(Collectors.toList());
                			objectValidator.validate(Bar.class, res);
                		}
                \t\t
                		return res;
                	}
                
                	protected abstract List<Bar.BarBuilder> doEvaluate(List<? extends Bar> barList);
                
                	public static class DistinctFuncDefault extends DistinctFunc {
                		@Override
                		protected List<Bar.BarBuilder> doEvaluate(List<? extends Bar> barList) {
                			if (barList == null) {
                				barList = Collections.emptyList();
                			}
                			List<Bar.BarBuilder> res = new ArrayList<>();
                			return assignOutput(res, barList);
                		}
                \t\t
                		protected List<Bar.BarBuilder> assignOutput(List<Bar.BarBuilder> res, List<? extends Bar> barList) {
                			res.addAll(toBuilder(distinct(MapperC.<Bar>of(barList)).getMulti()));
                \t\t\t
                			return Optional.ofNullable(res)
                				.map(o -> o.stream().map(i -> i.prune()).collect(Collectors.toList()))
                				.orElse(null);
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.DistinctFunc"));
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "DistinctFunc");
        Object bar1 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Bar", Map.of("n", 1), Map.of());
        Object bar2 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Bar", Map.of("n", 2), Map.of());
        Object bar3 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Bar", Map.of("n", 3), Map.of());
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class,
                List.of(bar1, bar1, bar1, bar2, bar2, bar3));
        assertEquals(3, res.size());
        assertTrue(res.containsAll(List.of(bar1, bar2, bar3)), "hasItems(bar1, bar2, bar3)");
    }

    /** Upstream {@code funcWithListOfStringDistinctThenOnlyElement}. */
    @Test
    void funcWithListOfStringDistinctThenOnlyElement() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                type Foo:
                	n string (0..*)
                
                func DistinctFunc:
                	inputs:
                		foo Foo (0..1)
                	output:
                		res string (0..1)
                	set res: foo -> n distinct only-element
                
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "DistinctFunc");
        Object foo = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo",
                Map.of(), Map.of("n", List.of("1", "1", "1")));
        assertEquals("1", UpstreamPortHarness.invokeFunc(func, String.class, foo));
    }

    /** Upstream {@code funcWithListOfStringDistinctThenOnlyElement2}. */
    @Test
    void funcWithListOfStringDistinctThenOnlyElement2() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                func DistinctFunc:
                	inputs:
                		n string (0..*)
                	output:
                		res string (0..1)
                	set res: n distinct only-element
                
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "DistinctFunc");
        assertEquals("1", UpstreamPortHarness.invokeFunc(func, String.class, List.of("1", "1", "1")));
    }

    /** Upstream {@code funcWithListOfStringDistinctThenOnlyElement3}. */
    @Test
    void funcWithListOfStringDistinctThenOnlyElement3() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                func DistinctFunc:
                	inputs:
                		n string (0..*)
                	output:
                		res string (0..1)
                	alias x:
                		n distinct only-element
                	set res:\s
                		x
                
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "DistinctFunc");
        assertEquals("1", UpstreamPortHarness.invokeFunc(func, String.class, List.of("1", "1", "1")));
    }

    /** Upstream {@code funcWithListOfStringDistinctThenOnlyElement4}. */
    @Test
    void funcWithListOfStringDistinctThenOnlyElement4() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace com.rosetta.test.model
                version "${project.version}"
                
                func DistinctFunc:
                	inputs:
                		n string (0..*)
                	output:
                		res string (0..1)
                	alias x:
                		n
                	set res:\s
                		x distinct only-element
                
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "DistinctFunc");
        assertEquals("1", UpstreamPortHarness.invokeFunc(func, String.class, List.of("1", "1", "1")));
    }

    /** Upstream {@code funcOnlyElementAnyMultiple}. */
    @Test
    void funcOnlyElementAnyMultiple() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace "demo"
                version "${project.version}"
                
                type Type1:
                		t Type2 (1..1)
                		ts Type2 (1..*)
                type Type2:
                		num number (1..1)
                		nums number (1..*)
                
                func Func1:
                	inputs: t1 Type1(1..1)
                	output: res number (1..1)
                	set res: t1->ts->num only-element
                
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /**
     * Upstream {@code funcOnlyElementOnlySingle} — the recorded validation item's
     * false-NEGATIVE half 2, HEALED at the #437 board-clearing wave (facet
     * onlyElementSingleReceiver: only-element over a resolved single-cardinality
     * receiver warns — upstream RosettaSimpleValidator.checkUnaryOperation, message
     * byte-identical). Upstream: assertWarning(ROSETTA_ONLY_ELEMENT). (The probe's
     * recorded upstream template quirk stands: the only-exists validator hard-codes
     * type parameter T2, so a model type named T2 emits {@code <T2 extends T2>} —
     * upstream never generates this model in the test, and neither does this
     * validation-only port.)
     */
    @Test
    void funcOnlyElementOnlySingle() {
        var p = UpstreamExpressionPortSupport.parse(List.of("""
                namespace "demo"
                version "${project.version}"

                type T1:
                		t T2 (1..1)
                		ts T2 (1..*)
                type T2:
                		num number (1..1)
                		nums number (1..*)

                func F1:
                	inputs: t1 T1(1..1)
                	output: res number (1..1)
                	set res: t1->t->num only-element

                """), List.of(), "True");
        var contextValidation = p.ws().validationDiagnostics().stream()
                .filter(d -> d.range().file().startsWith("expression-port-context-"))
                .toList();
        assertEquals(1, contextValidation.size(),
                "expected exactly the upstream only-element warning: " + contextValidation);
        assertTrue(contextValidation.get(0).message().equals(
                "List only-element operation cannot be used for single cardinality expressions."),
                "upstream's message byte-identical, got: " + contextValidation.get(0).message());
    }

    /** Upstream {@code nestedIfElse}. */
    @Test
    void nestedIfElse() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace "demo"
                version "${project.version}"
                
                func IfElseTest:
                inputs:
                	s1 string (1..1)
                	s2 string (1..1)
                output: result string (1..1)
                
                set result:
                	if s1 = "1"
                		then if s2 = "a"
                			then "result1a"
                		else
                			if s2 = "b"
                				then "result1b"
                	else
                		"result1"
                	else if s1 = "2" then
                		if s2 = "a"
                		then "result2a"
                		else if s2 = "b"
                		then "result2b"
                		else "result2"
                		  else
                "result"
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code mathOperationInsideIfStatement}. */
    @Test
    void mathOperationInsideIfStatement() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace "demo"
                version "${project.version}"
                
                func AddInsideIf:
                	inputs:
                		i1 int (1..1)
                		i2 int (1..1)
                		b boolean (1..1)
                	output: result int (1..1)
                \t
                	set result:
                		if b = True
                		then i1 + i2
                		else 0
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code assignOutputOnResolvedQuantity}. */
    @Test
    void assignOutputOnResolvedQuantity() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace "demo"
                version "${project.version}"
                
                type Quantity:
                	amount number (1..1)
                \t
                type PriceQuantity:
                	[metadata key]
                	quantity Quantity (0..*)
                	    [metadata location]
                \t\s\s\s\s
                type ResolvablePayoutQuantity:
                	resolvedQuantity Quantity (0..1)
                	[metadata address "pointsTo"=PriceQuantity->quantity]
                
                type Cashflow:
                	payoutQuantity ResolvablePayoutQuantity (1..1)
                
                func InterestCashSettlementAmount:
                	inputs:
                		x number (1..1)
                	output:
                		cashflow Cashflow (1..1)
                
                set cashflow -> payoutQuantity -> resolvedQuantity -> amount:
                	 x
                
                """);
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code ifWithSingleStringType}. */
    @Test
    void ifWithSingleStringType() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		test boolean (1..1)
                 		t1 string  (1..1)
                 		t2 string (1..1)
                	output:
                		result string (1..1)
                \t
                	set result:
                		if test = True
                		then t1
                		else t2
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.expression.CardinalityOperator;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperS;
                
                import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                
                	/**
                	* @param test\s
                	* @param t1\s
                	* @param t2\s
                	* @return result\s
                	*/
                	public String evaluate(Boolean test, String t1, String t2) {
                		String result = doEvaluate(test, t1, t2);
                \t\t
                		return result;
                	}
                
                	protected abstract String doEvaluate(Boolean test, String t1, String t2);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected String doEvaluate(Boolean test, String t1, String t2) {
                			String result = null;
                			return assignOutput(result, test, t1, t2);
                		}
                \t\t
                		protected String assignOutput(String result, Boolean test, String t1, String t2) {
                			if (areEqual(MapperS.of(test), MapperS.of(true), CardinalityOperator.All).getOrDefault(false)) {
                				result = t1;
                			} else {
                				result = t2;
                			}
                \t\t\t
                			return result;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code ifWithMultipleStringType}. */
    @Test
    void ifWithMultipleStringType() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		test boolean (1..1)
                 		t1 string  (1..*)
                 		t2 string (1..*)
                	output:
                		result string (1..*)
                \t
                	add result:
                		if test = True
                		then t1
                		else t2
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.expression.CardinalityOperator;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperS;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                
                import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                
                	/**
                	* @param test\s
                	* @param t1\s
                	* @param t2\s
                	* @return result\s
                	*/
                	public List<String> evaluate(Boolean test, List<String> t1, List<String> t2) {
                		List<String> result = doEvaluate(test, t1, t2);
                \t\t
                		return result;
                	}
                
                	protected abstract List<String> doEvaluate(Boolean test, List<String> t1, List<String> t2);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<String> doEvaluate(Boolean test, List<String> t1, List<String> t2) {
                			if (t1 == null) {
                				t1 = Collections.emptyList();
                			}
                			if (t2 == null) {
                				t2 = Collections.emptyList();
                			}
                			List<String> result = new ArrayList<>();
                			return assignOutput(result, test, t1, t2);
                		}
                \t\t
                		protected List<String> assignOutput(List<String> result, Boolean test, List<String> t1, List<String> t2) {
                			if (areEqual(MapperS.of(test), MapperS.of(true), CardinalityOperator.All).getOrDefault(false)) {
                				result.addAll(t1);
                			} else {
                				result.addAll(t2);
                			}
                \t\t\t
                			return result;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code ifWithSingleNumberType}. */
    @Test
    void ifWithSingleNumberType() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		test boolean (1..1)
                 		t1 number  (1..1)
                 		t2 number (1..1)
                	output:
                		result number (1..1)
                \t
                	set result:
                		if test = True
                		then t1
                		else t2
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.expression.CardinalityOperator;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperS;
                import java.math.BigDecimal;
                
                import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                
                	/**
                	* @param test\s
                	* @param t1\s
                	* @param t2\s
                	* @return result\s
                	*/
                	public BigDecimal evaluate(Boolean test, BigDecimal t1, BigDecimal t2) {
                		BigDecimal result = doEvaluate(test, t1, t2);
                \t\t
                		return result;
                	}
                
                	protected abstract BigDecimal doEvaluate(Boolean test, BigDecimal t1, BigDecimal t2);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected BigDecimal doEvaluate(Boolean test, BigDecimal t1, BigDecimal t2) {
                			BigDecimal result = null;
                			return assignOutput(result, test, t1, t2);
                		}
                \t\t
                		protected BigDecimal assignOutput(BigDecimal result, Boolean test, BigDecimal t1, BigDecimal t2) {
                			if (areEqual(MapperS.of(test), MapperS.of(true), CardinalityOperator.All).getOrDefault(false)) {
                				result = t1;
                			} else {
                				result = t2;
                			}
                \t\t\t
                			return result;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code ifWithMultipleNumberType}. */
    @Test
    void ifWithMultipleNumberType() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		test boolean (1..1)
                 		t1 number  (1..*)
                 		t2 number (1..*)
                	output:
                		result number (1..*)
                \t
                	add result:
                		if test = True
                		then t1
                		else t2
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.expression.CardinalityOperator;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperS;
                import java.math.BigDecimal;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                
                import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                
                	/**
                	* @param test\s
                	* @param t1\s
                	* @param t2\s
                	* @return result\s
                	*/
                	public List<BigDecimal> evaluate(Boolean test, List<BigDecimal> t1, List<BigDecimal> t2) {
                		List<BigDecimal> result = doEvaluate(test, t1, t2);
                \t\t
                		return result;
                	}
                
                	protected abstract List<BigDecimal> doEvaluate(Boolean test, List<BigDecimal> t1, List<BigDecimal> t2);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<BigDecimal> doEvaluate(Boolean test, List<BigDecimal> t1, List<BigDecimal> t2) {
                			if (t1 == null) {
                				t1 = Collections.emptyList();
                			}
                			if (t2 == null) {
                				t2 = Collections.emptyList();
                			}
                			List<BigDecimal> result = new ArrayList<>();
                			return assignOutput(result, test, t1, t2);
                		}
                \t\t
                		protected List<BigDecimal> assignOutput(List<BigDecimal> result, Boolean test, List<BigDecimal> t1, List<BigDecimal> t2) {
                			if (areEqual(MapperS.of(test), MapperS.of(true), CardinalityOperator.All).getOrDefault(false)) {
                				result.addAll(t1);
                			} else {
                				result.addAll(t2);
                			}
                \t\t\t
                			return result;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code ifWithSingleDataType}. */
    @Test
    void ifWithSingleDataType() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		test boolean (1..1)
                 		b1 Bar (1..1)
                 		b2 Bar (1..1)
                	output:
                		result Bar (1..1)
                \t
                	set result:
                		if test = True
                		then b1
                		else b2
                
                type Bar:
                	s1 string (1..1)
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.expression.CardinalityOperator;
                import com.rosetta.model.lib.functions.ModelObjectValidator;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperS;
                import com.rosetta.test.model.Bar;
                import java.util.Optional;
                import javax.inject.Inject;
                
                import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                \t
                	@Inject protected ModelObjectValidator objectValidator;
                
                	/**
                	* @param test\s
                	* @param b1\s
                	* @param b2\s
                	* @return result\s
                	*/
                	public Bar evaluate(Boolean test, Bar b1, Bar b2) {
                		Bar.BarBuilder resultBuilder = doEvaluate(test, b1, b2);
                \t\t
                		final Bar result;
                		if (resultBuilder == null) {
                			result = null;
                		} else {
                			result = resultBuilder.build();
                			objectValidator.validate(Bar.class, result);
                		}
                \t\t
                		return result;
                	}
                
                	protected abstract Bar.BarBuilder doEvaluate(Boolean test, Bar b1, Bar b2);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected Bar.BarBuilder doEvaluate(Boolean test, Bar b1, Bar b2) {
                			Bar.BarBuilder result = Bar.builder();
                			return assignOutput(result, test, b1, b2);
                		}
                \t\t
                		protected Bar.BarBuilder assignOutput(Bar.BarBuilder result, Boolean test, Bar b1, Bar b2) {
                			if (areEqual(MapperS.of(test), MapperS.of(true), CardinalityOperator.All).getOrDefault(false)) {
                				result = toBuilder(b1);
                			} else {
                				result = toBuilder(b2);
                			}
                \t\t\t
                			return Optional.ofNullable(result)
                				.map(o -> o.prune())
                				.orElse(null);
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code ifWithMultipleDataTypes}. */
    @Test
    void ifWithMultipleDataTypes() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		test boolean (1..1)
                 		b1 Bar (1..*)
                 		b2 Bar (1..*)
                	output:
                		result Bar (1..*)
                \t
                	add result:
                		if test = True
                		then b1
                		else b2
                
                type Bar:
                	s1 string (1..1)
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.expression.CardinalityOperator;
                import com.rosetta.model.lib.functions.ModelObjectValidator;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperS;
                import com.rosetta.test.model.Bar;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                import java.util.Optional;
                import java.util.stream.Collectors;
                import javax.inject.Inject;
                
                import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                \t
                	@Inject protected ModelObjectValidator objectValidator;
                
                	/**
                	* @param test\s
                	* @param b1\s
                	* @param b2\s
                	* @return result\s
                	*/
                	public List<? extends Bar> evaluate(Boolean test, List<? extends Bar> b1, List<? extends Bar> b2) {
                		List<Bar.BarBuilder> resultBuilder = doEvaluate(test, b1, b2);
                \t\t
                		final List<? extends Bar> result;
                		if (resultBuilder == null) {
                			result = null;
                		} else {
                			result = resultBuilder.stream().map(Bar::build).collect(Collectors.toList());
                			objectValidator.validate(Bar.class, result);
                		}
                \t\t
                		return result;
                	}
                
                	protected abstract List<Bar.BarBuilder> doEvaluate(Boolean test, List<? extends Bar> b1, List<? extends Bar> b2);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected List<Bar.BarBuilder> doEvaluate(Boolean test, List<? extends Bar> b1, List<? extends Bar> b2) {
                			if (b1 == null) {
                				b1 = Collections.emptyList();
                			}
                			if (b2 == null) {
                				b2 = Collections.emptyList();
                			}
                			List<Bar.BarBuilder> result = new ArrayList<>();
                			return assignOutput(result, test, b1, b2);
                		}
                \t\t
                		protected List<Bar.BarBuilder> assignOutput(List<Bar.BarBuilder> result, Boolean test, List<? extends Bar> b1, List<? extends Bar> b2) {
                			if (areEqual(MapperS.of(test), MapperS.of(true), CardinalityOperator.All).getOrDefault(false)) {
                				result.addAll(toBuilder(b1));
                			} else {
                				result.addAll(toBuilder(b2));
                			}
                \t\t\t
                			return Optional.ofNullable(result)
                				.map(o -> o.stream().map(i -> i.prune()).collect(Collectors.toList()))
                				.orElse(null);
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldSetMathsOperation}. */
    @Test
    void shouldSetMathsOperation() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		n1 number (1..1)
                 		n2 number (1..1)
                	output:
                		res number (1..1)
                \t
                	set res:
                		n1 * n2
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.expression.MapperMaths;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperS;
                import java.math.BigDecimal;
                
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                
                	/**
                	* @param n1\s
                	* @param n2\s
                	* @return res\s
                	*/
                	public BigDecimal evaluate(BigDecimal n1, BigDecimal n2) {
                		BigDecimal res = doEvaluate(n1, n2);
                \t\t
                		return res;
                	}
                
                	protected abstract BigDecimal doEvaluate(BigDecimal n1, BigDecimal n2);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected BigDecimal doEvaluate(BigDecimal n1, BigDecimal n2) {
                			BigDecimal res = null;
                			return assignOutput(res, n1, n2);
                		}
                \t\t
                		protected BigDecimal assignOutput(BigDecimal res, BigDecimal n1, BigDecimal n2) {
                			res = MapperMaths.<BigDecimal, BigDecimal, BigDecimal>multiply(MapperS.of(n1), MapperS.of(n2)).get();
                \t\t\t
                			return res;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldSetList}. */
    @Test
    void shouldSetList() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	outList string (0..*)
                
                func FuncFoo:
                 	inputs:
                 		inList string (0..*)
                	output:
                		foo Foo (1..1)
                \t
                	set foo -> outList:
                		inList
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.ModelObjectValidator;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.test.model.Foo;
                import java.util.Collections;
                import java.util.List;
                import java.util.Optional;
                import javax.inject.Inject;
                
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                \t
                	@Inject protected ModelObjectValidator objectValidator;
                
                	/**
                	* @param inList\s
                	* @return foo\s
                	*/
                	public Foo evaluate(List<String> inList) {
                		Foo.FooBuilder fooBuilder = doEvaluate(inList);
                \t\t
                		final Foo foo;
                		if (fooBuilder == null) {
                			foo = null;
                		} else {
                			foo = fooBuilder.build();
                			objectValidator.validate(Foo.class, foo);
                		}
                \t\t
                		return foo;
                	}
                
                	protected abstract Foo.FooBuilder doEvaluate(List<String> inList);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected Foo.FooBuilder doEvaluate(List<String> inList) {
                			if (inList == null) {
                				inList = Collections.emptyList();
                			}
                			Foo.FooBuilder foo = Foo.builder();
                			return assignOutput(foo, inList);
                		}
                \t\t
                		protected Foo.FooBuilder assignOutput(Foo.FooBuilder foo, List<String> inList) {
                			foo
                				.setOutList(inList);
                \t\t\t
                			return Optional.ofNullable(foo)
                				.map(o -> o.prune())
                				.orElse(null);
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldAddList}. */
    @Test
    void shouldAddList() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	outList string (0..*)
                
                func FuncFoo:
                 	inputs:
                 		inList string (0..*)
                	output:
                		foo Foo (1..1)
                \t
                	add foo -> outList:
                		inList
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.ModelObjectValidator;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.test.model.Foo;
                import java.util.Collections;
                import java.util.List;
                import java.util.Optional;
                import javax.inject.Inject;
                
                
                @ImplementedBy(FuncFoo.FuncFooDefault.class)
                public abstract class FuncFoo implements RosettaFunction {
                \t
                	@Inject protected ModelObjectValidator objectValidator;
                
                	/**
                	* @param inList\s
                	* @return foo\s
                	*/
                	public Foo evaluate(List<String> inList) {
                		Foo.FooBuilder fooBuilder = doEvaluate(inList);
                \t\t
                		final Foo foo;
                		if (fooBuilder == null) {
                			foo = null;
                		} else {
                			foo = fooBuilder.build();
                			objectValidator.validate(Foo.class, foo);
                		}
                \t\t
                		return foo;
                	}
                
                	protected abstract Foo.FooBuilder doEvaluate(List<String> inList);
                
                	public static class FuncFooDefault extends FuncFoo {
                		@Override
                		protected Foo.FooBuilder doEvaluate(List<String> inList) {
                			if (inList == null) {
                				inList = Collections.emptyList();
                			}
                			Foo.FooBuilder foo = Foo.builder();
                			return assignOutput(foo, inList);
                		}
                \t\t
                		protected Foo.FooBuilder assignOutput(Foo.FooBuilder foo, List<String> inList) {
                			foo
                				.addOutList(inList);
                \t\t\t
                			return Optional.ofNullable(foo)
                				.map(o -> o.prune())
                				.orElse(null);
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.FuncFoo"));
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldMergeComplexTypeList}. */
    @Test
    void shouldMergeComplexTypeList() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		foos Foo (0..*)
                 		newFoo Foo (1..1) <"Add single Foo">
                	output:
                		mergedFoos Foo (0..*)
                \t
                	set mergedFoos:
                		foos
                \t
                	add mergedFoos:
                		newFoo
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "1");
        Object foo2 = foo(classes, "2");
        Object newFoo = foo(classes, "3");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(foo1, foo2), newFoo);
        assertEquals(3, res.size());
        assertTrue(res.containsAll(List.of(foo1, foo2, newFoo)), "hasItems(foo1, foo2, newFoo)");
    }

    /** Upstream {@code shouldMergeComplexTypeList2}. */
    @Test
    void shouldMergeComplexTypeList2() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		foos Foo (0..*)
                 		newFoo Foo (1..1) <"Add single Foo">
                	output:
                		mergedFoos Foo (0..*)
                \t
                	add mergedFoos:
                		foos
                \t
                	add mergedFoos:
                		newFoo
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "1");
        Object foo2 = foo(classes, "2");
        Object newFoo = foo(classes, "3");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(foo1, foo2), newFoo);
        assertEquals(3, res.size());
        assertTrue(res.containsAll(List.of(foo1, foo2, newFoo)), "hasItems(foo1, foo2, newFoo)");
    }

    /** Upstream {@code shouldMergeComplexTypeList3}. */
    @Test
    void shouldMergeComplexTypeList3() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		foos Foo (0..*)
                 		newFoos Foo (0..*) <"Add Foo list">
                	output:
                		mergedFoos Foo (0..*)
                \t
                	add mergedFoos:
                		foos
                \t
                	add mergedFoos:
                		newFoos
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "1");
        Object foo2 = foo(classes, "2");
        Object foo3 = foo(classes, "3");
        Object foo4 = foo(classes, "4");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of(foo1, foo2), List.of(foo3, foo4));
        assertEquals(4, res.size());
        assertTrue(res.containsAll(List.of(foo1, foo2, foo3, foo4)), "hasItems(foo1..foo4)");
    }

    /** Upstream {@code shouldMergeBasicTypeList}. */
    @Test
    void shouldMergeBasicTypeList() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		foos string (0..*)
                 		newFoo string (1..1) <"Add single Foo">
                	output:
                		mergedFoos string (0..*)
                \t
                	set mergedFoos:
                		foos
                \t
                	add mergedFoos:
                		newFoo
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of("1", "2"), "3");
        assertEquals(3, res.size());
        assertTrue(res.containsAll(List.of("1", "2", "3")), "hasItems(1, 2, 3)");
    }

    /** Upstream {@code shouldMergeBasicTypeList2}. */
    @Test
    void shouldMergeBasicTypeList2() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		foos string (0..*)
                 		newFoo string (1..1) <"Add single Foo">
                	output:
                		mergedFoos string (0..*)
                \t
                	add mergedFoos:
                		foos
                \t
                	add mergedFoos:
                		newFoo
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of("1", "2"), "3");
        assertEquals(3, res.size());
        assertTrue(res.containsAll(List.of("1", "2", "3")), "hasItems(1, 2, 3)");
    }

    /** Upstream {@code shouldMergeBasicTypeList3}. */
    @Test
    void shouldMergeBasicTypeList3() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func FuncFoo:
                 	inputs:
                 		foos string (0..*)
                 		newFoos string (0..*) <"Add Foo list">
                	output:
                		mergedFoos string (0..*)
                \t
                	add mergedFoos:
                		foos
                \t
                	add mergedFoos:
                		newFoos
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        List<?> res = UpstreamPortHarness.invokeFunc(func, List.class, List.of("1", "2"), List.of("3", "4"));
        assertEquals(4, res.size());
        assertTrue(res.containsAll(List.of("1", "2", "3", "4")), "hasItems(1, 2, 3, 4)");
    }

    /** Upstream {@code shouldAddComplexTypeList}. */
    @Test
    void shouldAddComplexTypeList() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Bar:
                	foos Foo (0..*)
                
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		bar Bar (1..1)
                 		newFoo Foo (1..1) <"Add single Foo">
                	output:
                		updatedBar Bar (1..1)
                \t
                	set updatedBar:
                		bar
                \t
                	add updatedBar -> foos:
                		newFoo
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "1");
        Object foo2 = foo(classes, "2");
        Object barObj = bar(classes, List.of(foo1, foo2));
        Object newFoo = foo(classes, "3");
        Object res = UpstreamPortHarness.invokeFunc(func, Object.class, barObj, newFoo);
        List<?> foos = (List<?>) UpstreamPortHarness.call(res, "getFoos");
        assertEquals(3, foos.size());
        assertTrue(foos.containsAll(List.of(foo1, foo2, newFoo)), "appends to existing list");
    }

    /** Upstream {@code shouldAddComplexTypeList2}. */
    @Test
    void shouldAddComplexTypeList2() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Bar:
                	foos Foo (0..*)
                
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		bar Bar (1..1)
                 		newFoos Foo (0..*) <"Add Foo list">
                	output:
                		updatedBar Bar (1..1)
                \t
                	set updatedBar:
                		bar
                \t
                	add updatedBar -> foos:
                		newFoos
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "1");
        Object foo2 = foo(classes, "2");
        Object barObj = bar(classes, List.of(foo1, foo2));
        Object foo3 = foo(classes, "3");
        Object foo4 = foo(classes, "4");
        Object res = UpstreamPortHarness.invokeFunc(func, Object.class, barObj, List.of(foo3, foo4));
        List<?> foos = (List<?>) UpstreamPortHarness.call(res, "getFoos");
        assertEquals(4, foos.size());
        assertTrue(foos.containsAll(List.of(foo1, foo2, foo3, foo4)), "appends to existing list");
    }

    /**
     * Upstream {@code shouldSetComplexTypeList} — leg-C finding #33 HEALED at the
     * #437 board-clearing wave (facet setSingleSymbolIntoMultiLeaf: the bare single
     * symbol SET on a multi leaf takes the #217 inline null-guarded singletonList
     * coercion at the SET seat too; oracle-byte-identical, the
     * func-set-single-complex group).
     */
    @Test
    void shouldSetComplexTypeList() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Bar:
                	foos Foo (0..*)
                
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		bar Bar (1..1)
                 		newFoo Foo (1..1) <"Add single Foo">
                	output:
                		updatedBar Bar (1..1)
                \t
                	set updatedBar:
                		bar
                \t
                	set updatedBar -> foos:
                		newFoo
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "1");
        Object foo2 = foo(classes, "2");
        Object barObj = bar(classes, List.of(foo1, foo2));
        Object newFoo = foo(classes, "3");
        Object res = UpstreamPortHarness.invokeFunc(func, Object.class, barObj, newFoo);
        List<?> foos = (List<?>) UpstreamPortHarness.call(res, "getFoos");
        assertEquals(1, foos.size());
        assertTrue(foos.contains(newFoo), "overwrites existing list");
    }

    /** Upstream {@code shouldSetComplexTypeList2}. */
    @Test
    void shouldSetComplexTypeList2() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Bar:
                	foos Foo (0..*)
                
                type Foo:
                	attr string (1..1)
                
                func FuncFoo:
                 	inputs:
                 		bar Bar (1..1)
                 		newFoos Foo (0..*) <"Add Foo list">
                	output:
                		updatedBar Bar (1..1)
                \t
                	set updatedBar:
                		bar
                \t
                	set updatedBar -> foos:
                		newFoos
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object foo1 = foo(classes, "1");
        Object foo2 = foo(classes, "2");
        Object barObj = bar(classes, List.of(foo1, foo2));
        Object foo3 = foo(classes, "3");
        Object foo4 = foo(classes, "4");
        Object res = UpstreamPortHarness.invokeFunc(func, Object.class, barObj, List.of(foo3, foo4));
        List<?> foos = (List<?>) UpstreamPortHarness.call(res, "getFoos");
        assertEquals(2, foos.size());
        assertTrue(foos.containsAll(List.of(foo3, foo4)), "overwrites existing list");
    }

    /** Upstream {@code shouldAddBasicTypeList}. */
    @Test
    void shouldAddBasicTypeList() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Baz:
                	attrList string (0..*)
                
                func FuncFoo:
                 	inputs:
                 		baz Baz (1..1)
                 		s string (1..1) <"Add single">
                	output:
                		updatedBaz Baz (1..1)
                \t
                	set updatedBaz:
                		baz
                \t
                	add updatedBaz -> attrList:
                		s
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object bazObj = baz(classes, List.of("1", "2"));
        Object res = UpstreamPortHarness.invokeFunc(func, Object.class, bazObj, "3");
        List<?> attrList = (List<?>) UpstreamPortHarness.call(res, "getAttrList");
        assertEquals(3, attrList.size());
        assertTrue(attrList.containsAll(List.of("1", "2", "3")), "appends to existing list");
    }

    /** Upstream {@code shouldAddBasicTypeList2}. */
    @Test
    void shouldAddBasicTypeList2() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Baz:
                	attrList string (0..*)
                
                func FuncFoo:
                 	inputs:
                 		baz Baz (1..1)
                 		sList string (0..*) <"Add list">
                	output:
                		updatedBaz Baz (1..1)
                \t
                	set updatedBaz:
                		baz
                \t
                	add updatedBaz -> attrList:
                		sList
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object bazObj = baz(classes, List.of("1", "2"));
        Object res = UpstreamPortHarness.invokeFunc(func, Object.class, bazObj, List.of("3", "4"));
        List<?> attrList = (List<?>) UpstreamPortHarness.call(res, "getAttrList");
        assertEquals(4, attrList.size());
        assertTrue(attrList.containsAll(List.of("1", "2", "3", "4")), "appends to existing list");
    }

    /**
     * Upstream {@code shouldSetBasicTypeList} — leg-C finding #33 HEALED at the
     * #437 board-clearing wave (facet setSingleSymbolIntoMultiLeaf, the basic-type
     * face: {@code .setAttrList((s == null ? Collections.<String>emptyList() :
     * Collections.singletonList(s)));} — oracle-byte-identical, the
     * func-set-single-basic group).
     */
    @Test
    void shouldSetBasicTypeList() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Baz:
                	attrList string (0..*)
                
                func FuncFoo:
                 	inputs:
                 		baz Baz (1..1)
                 		s string (1..1) <"Add single">
                	output:
                		updatedBaz Baz (1..1)
                \t
                	set updatedBaz:
                		baz
                \t
                	set updatedBaz -> attrList:
                		s
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object bazObj = baz(classes, List.of("1", "2"));
        Object res = UpstreamPortHarness.invokeFunc(func, Object.class, bazObj, "3");
        List<?> attrList = (List<?>) UpstreamPortHarness.call(res, "getAttrList");
        assertEquals(1, attrList.size());
        assertTrue(attrList.contains("3"), "overwrites existing list");
    }

    /** Upstream {@code shouldSetBasicTypeList2}. */
    @Test
    void shouldSetBasicTypeList2() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Baz:
                	attrList string (0..*)
                
                func FuncFoo:
                 	inputs:
                 		baz Baz (1..1)
                 		sList string (0..*) <"Add list">
                	output:
                		updatedBaz Baz (1..1)
                \t
                	set updatedBaz:
                		baz
                \t
                	set updatedBaz -> attrList:
                		sList
                """);
        Map<String, Class<?>> classes = UpstreamPortHarness.compileToClasses(code);
        Object func = UpstreamPortHarness.createFunc(classes, "FuncFoo");
        Object bazObj = baz(classes, List.of("1", "2"));
        Object res = UpstreamPortHarness.invokeFunc(func, Object.class, bazObj, List.of("3", "4"));
        List<?> attrList = (List<?>) UpstreamPortHarness.call(res, "getAttrList");
        assertEquals(2, attrList.size());
        assertTrue(attrList.containsAll(List.of("3", "4")), "overwrites existing list");
    }

    /** Upstream {@code shouldCallFuncTwiceInCondition}. */
    @Test
    void shouldCallFuncTwiceInCondition() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                type Foo:
                	test boolean (1..1)
                	attr string (1..1)
                \t
                	condition Bar:
                		if test = True then
                			FuncFoo( attr, "x" )
                		else
                			FuncFoo( attr, "y" )
                
                func FuncFoo:
                 	inputs:
                 		a string (1..1)
                 		b string (1..1)
                	output:
                		result boolean (1..1)
                
                """);
        assertEquals("""
                package com.rosetta.test.model.validation.datarule;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.annotations.RosettaDataRule;
                import com.rosetta.model.lib.expression.CardinalityOperator;
                import com.rosetta.model.lib.expression.ComparisonResult;
                import com.rosetta.model.lib.mapper.MapperS;
                import com.rosetta.model.lib.path.RosettaPath;
                import com.rosetta.model.lib.validation.ValidationResult;
                import com.rosetta.model.lib.validation.Validator;
                import com.rosetta.test.model.Foo;
                import com.rosetta.test.model.functions.FuncFoo;
                import java.util.Arrays;
                import java.util.Collections;
                import java.util.List;
                import javax.inject.Inject;
                
                import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;
                
                /**
                 * @version test
                 */
                @RosettaDataRule("FooBar")
                @ImplementedBy(FooBar.Default.class)
                public interface FooBar extends Validator<Foo> {
                \t
                	String NAME = "FooBar";
                	String DEFINITION = "if test = True then FuncFoo( attr, \\"x\\" ) else FuncFoo( attr, \\"y\\" )";
                \t
                	class Default implements FooBar {
                \t
                		@Inject protected FuncFoo funcFoo;
                \t\t
                		@Override
                		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Foo foo) {
                			ComparisonResult result = executeDataRule(foo);
                			if (result.getOrDefault(true)) {
                				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Foo", path, DEFINITION));
                			}
                \t\t\t
                			String failureMessage = result.getError();
                			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
                				failureMessage = "Condition has failed.";
                			}
                			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Foo", path, DEFINITION, failureMessage));
                		}
                \t\t
                		private ComparisonResult executeDataRule(Foo foo) {
                			try {
                				if (areEqual(MapperS.of(foo).<Boolean>map("getTest", _foo -> _foo.getTest()), MapperS.of(true), CardinalityOperator.All).getOrDefault(false)) {
                					return ComparisonResult.ofNullSafe(MapperS.of(funcFoo.evaluate(MapperS.of(foo).<String>map("getAttr", _foo -> _foo.getAttr()).get(), "x")));
                				}
                				return ComparisonResult.ofNullSafe(MapperS.of(funcFoo.evaluate(MapperS.of(foo).<String>map("getAttr", _foo -> _foo.getAttr()).get(), "y")));
                			}
                			catch (Exception ex) {
                				return ComparisonResult.failure(ex.getMessage());
                			}
                		}
                	}
                \t
                	@SuppressWarnings("unused")
                	class NoOp implements FooBar {
                \t
                		@Override
                		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Foo foo) {
                			return Collections.emptyList();
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.validation.datarule.FooBar"));
        UpstreamPortHarness.compileToClasses(code);
    }

    /**
     * Upstream {@code canUseNestedIfThenElseInsideFunctionCall} — leg-C finding #34
     * HEALED at the #437 board-clearing wave (facet ifCondLiteralBare: boolean-LITERAL
     * if-conditions render the bare Java literal — {@code if (true)} /
     * {@code else if (false)} — with no getOrDefault wrap and no MapperS import;
     * oracle-byte-identical, the func-if-literal-call group). The expected block is
     * upstream's own full-text assert, derived MECHANICALLY from the oracle golden
     * (namespace-swapped; decode-verified against the golden bytes).
     */
    @Test
    void canUseNestedIfThenElseInsideFunctionCall() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func A:
                    inputs:
                        a boolean (1..1)
                    output:
                        result boolean (1..1)
                
                func B:
                    output:
                        result boolean (1..1)
                \s\s\s\s
                    set result:
                        A(if True then True else if False then True)
                
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;

                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import javax.inject.Inject;


                @ImplementedBy(B.BDefault.class)
                public abstract class B implements RosettaFunction {
                \t
                	// RosettaFunction dependencies
                	//
                	@Inject protected A a;

                	/**
                	* @return result\s
                	*/
                	public Boolean evaluate() {
                		Boolean result = doEvaluate();
                \t\t
                		return result;
                	}

                	protected abstract Boolean doEvaluate();

                	public static class BDefault extends B {
                		@Override
                		protected Boolean doEvaluate() {
                			Boolean result = null;
                			return assignOutput(result);
                		}
                \t\t
                		protected Boolean assignOutput(Boolean result) {
                			final Boolean ifThenElseResult;
                			if (true) {
                				ifThenElseResult = true;
                			} else if (false) {
                				ifThenElseResult = true;
                			} else {
                				ifThenElseResult = null;
                			}
                			result = a.evaluate(ifThenElseResult);
                \t\t\t
                			return result;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.B"));
        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldCompareDateExtractedFromZonedDateTime}. */
    @Test
    void shouldCompareDateExtractedFromZonedDateTime() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                func IsDateGreaterThan:
                	inputs:\s
                        date date (1..1)
                        zonedDateTime zonedDateTime (1..1)
                	output:\s
                		result boolean (1..1)
                
                    set result:\s
                        date <= zonedDateTime -> date
                
                """);
        assertEquals("""
                package com.rosetta.test.model.functions;
                
                import com.google.inject.ImplementedBy;
                import com.rosetta.model.lib.expression.CardinalityOperator;
                import com.rosetta.model.lib.functions.RosettaFunction;
                import com.rosetta.model.lib.mapper.MapperS;
                import com.rosetta.model.lib.records.Date;
                import java.time.ZonedDateTime;
                
                import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;
                
                @ImplementedBy(IsDateGreaterThan.IsDateGreaterThanDefault.class)
                public abstract class IsDateGreaterThan implements RosettaFunction {
                
                	/**
                	* @param date\s
                	* @param zonedDateTime\s
                	* @return result\s
                	*/
                	public Boolean evaluate(Date date, ZonedDateTime zonedDateTime) {
                		Boolean result = doEvaluate(date, zonedDateTime);
                \t\t
                		return result;
                	}
                
                	protected abstract Boolean doEvaluate(Date date, ZonedDateTime zonedDateTime);
                
                	public static class IsDateGreaterThanDefault extends IsDateGreaterThan {
                		@Override
                		protected Boolean doEvaluate(Date date, ZonedDateTime zonedDateTime) {
                			Boolean result = null;
                			return assignOutput(result, date, zonedDateTime);
                		}
                \t\t
                		protected Boolean assignOutput(Boolean result, Date date, ZonedDateTime zonedDateTime) {
                			result = lessThanEquals(MapperS.of(date), MapperS.of(zonedDateTime).<Date>map("Date", zdt -> Date.of(zdt.toLocalDate())), CardinalityOperator.All).get();
                \t\t\t
                			return result;
                		}
                	}
                }
                """, code.get("com.rosetta.test.model.functions.IsDateGreaterThan"));
        UpstreamPortHarness.compileToClasses(code);
    }

    // ---------------------------------------------------------------- helpers

    /** Upstream {@code createFoo(Map, String)}. */
    private static Object foo(Map<String, Class<?>> classes, String attr) {
        return UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo",
                Map.of("attr", attr), Map.of());
    }

    /** Upstream {@code createBar(Map, List)}. */
    private static Object bar(Map<String, Class<?>> classes, List<Object> foos) {
        return UpstreamPortHarness.createInstanceUsingBuilder(classes, "Bar",
                Map.of(), Map.of("foos", foos));
    }

    /** Upstream {@code createBaz(Map, List)}. */
    private static Object baz(Map<String, Class<?>> classes, List<String> attrList) {
        return UpstreamPortHarness.createInstanceUsingBuilder(classes, "Baz",
                Map.of(), Map.of("attrList", attrList));
    }

    /** Upstream {@code Date.of} — reflective on the isolated loader's released runtime. */
    private static Object date(Map<String, Class<?>> classes, int year, int month, int day) {
        try {
            Class<?> dateClass = UpstreamPortHarness.loadRuntimeClass(classes,
                    "com.rosetta.model.lib.records.Date");
            return dateClass.getMethod("of", int.class, int.class, int.class)
                    .invoke(null, year, month, day);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Date.of unavailable on the released runtime", e);
        }
    }

    /** Upstream {@code MetaFields.builder}...{@code .build()} — reflective. */
    private static Object metaFields(Map<String, Class<?>> classes, String scheme,
            String externalKey, String globalKey) {
        try {
            Class<?> metaClass = UpstreamPortHarness.loadRuntimeClass(classes,
                    "com.rosetta.model.metafields.MetaFields");
            Object builder = metaClass.getMethod("builder").invoke(null);
            if (scheme != null) {
                UpstreamPortHarness.setAttribute(builder, "scheme", scheme);
            }
            if (externalKey != null) {
                UpstreamPortHarness.setAttribute(builder, "externalKey", externalKey);
            }
            if (globalKey != null) {
                UpstreamPortHarness.setAttribute(builder, "globalKey", globalKey);
            }
            return UpstreamPortHarness.build(builder);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("MetaFields unavailable on the released runtime", e);
        }
    }

    /**
     * Upstream {@code Date.of(y, m, d)} — reflective on the GENERATED-CODE loader.
     * The records {@code Date} is a runtime class: a test-classpath instance would
     * cross classloaders and never equal (nor pass as) an invoke-side value (the
     * same split {@link #metaFields}/{@link #reference} handle for their runtime
     * classes).
     */
    private static Object recordDate(Map<String, Class<?>> classes, int year, int month, int day) {
        try {
            return UpstreamPortHarness.loadRuntimeClass(classes,
                            "com.rosetta.model.lib.records.Date")
                    .getMethod("of", int.class, int.class, int.class)
                    .invoke(null, year, month, day);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("records.Date unavailable on the released runtime", e);
        }
    }

    /** Upstream {@code Date.of(localDate)} — the LocalDate overload, reflective. */
    private static Object recordDate(Map<String, Class<?>> classes, java.time.LocalDate localDate) {
        try {
            return UpstreamPortHarness.loadRuntimeClass(classes,
                            "com.rosetta.model.lib.records.Date")
                    .getMethod("of", java.time.LocalDate.class)
                    .invoke(null, localDate);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("records.Date unavailable on the released runtime", e);
        }
    }

    /** Upstream {@code date.toLocalDate} — reflective via the runtime interface. */
    private static java.time.LocalDate toLocalDate(Map<String, Class<?>> classes, Object date) {
        try {
            return (java.time.LocalDate) UpstreamPortHarness.loadRuntimeClass(classes,
                            "com.rosetta.model.lib.records.Date")
                    .getMethod("toLocalDate")
                    .invoke(date);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("toLocalDate failed on " + date.getClass(), e);
        }
    }

    /** Upstream {@code (Reference.builder.reference = ...).build} — reflective. */
    private static Object reference(Map<String, Class<?>> classes, String ref) {
        try {
            Class<?> refClass = UpstreamPortHarness.loadRuntimeClass(classes,
                    "com.rosetta.model.lib.meta.Reference");
            Object builder = refClass.getMethod("builder").invoke(null);
            UpstreamPortHarness.setAttribute(builder, "reference", ref);
            return UpstreamPortHarness.build(builder);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Reference unavailable on the released runtime", e);
        }
    }

    /** Upstream {@code <enum>.fromDisplayName(name)} — reflective static call. */
    private static Object fromDisplayName(Class<?> enumClass, String name) {
        try {
            return enumClass.getDeclaredMethod("fromDisplayName", String.class)
                    .invoke(null, name);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new AssertionError("fromDisplayName failed on " + enumClass, e);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("fromDisplayName unavailable on " + enumClass, e);
        }
    }

    /** Xtend {@code #{...}} with null values — {@code Map.of} rejects nulls. */
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }
}
