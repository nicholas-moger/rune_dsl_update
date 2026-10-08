package com.regnosys.rosetta.generator.java.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static java.lang.ref.Reference.reachabilityFence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RGeneratedJavaClass;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.rosetta.model.lib.functions.RosettaFunction;

/**
 * Phase X T5 unit tests for {@link RuleGenerator}.
 *
 * <p>Each test builds a synthetic {@code .rosetta} corpus, parses it via
 * {@link AstBuilder#buildFromString(String, String)}, attaches the workspace
 * via {@link RWorkspace#build(List)}, and exercises the generator's three
 * lifecycle methods ({@code streamObjects} / {@code createTypeRepresentation}
 * / {@code generate}) directly.
 *
 * <p>Mirrors the pattern at {@code LabelProviderGeneratorTest} (T3) — the
 * predecessor generator. {@link java.lang.ref.Reference#reachabilityFence}
 * (statically imported as {@code reachabilityFence}) guards against
 * premature GC of the WeakReference-held workspace, matching the fence
 * pattern used by every fixture-loading test in this codebase since
 * P2.1.3c.
 *
 * <p>Grammar paste-quote (verified against
 * {@code rune-parser/src/main/antlr4/.../RosettaParser.g4} lines 808-809):
 * <ul>
 *   <li>{@code reporting rule <Name> from <Type>: ...} — produces an
 *       {@link com.regnosys.rosetta.ast.functions.RRule RRule}
 *       root element with {@code kind=REPORTING}.</li>
 *   <li>{@code eligibility rule <Name> from <Type>: ...} — produces an
 *       {@link com.regnosys.rosetta.ast.functions.RRule RRule}
 *       root element with {@code kind=ELIGIBILITY}.</li>
 * </ul>
 *
 * <p>Per spec § 4.1 this class covers ≥ 7 named scenarios (§ 4.1.1–4.1.6 +
 * one additional eligibility-kind scenario retained alongside the cardinality
 * edge case added at T5.0.5). The thin shim delegates emission to
 * {@code FunctionGenerator.buildClassWithBaseInterface} (T4) with
 * {@code ReportFunction<I,O>} as the base interface; the emission shape is
 * exercised at the function-generator level by
 * {@code FunctionGeneratorTest.buildClassWithBaseInterface_*} — here we lock
 * the {@link RuleGenerator}-specific behaviour (filter / FQN / base interface
 * type-argument construction / cardinality-edge-case fail-fast diagnostics).
 */
class RuleGeneratorTest {

    /**
     * Repository-root-anchored test-corpus search root. Surefire forks each
     * test JVM with {@code user.dir} pinned to the test module
     * ({@code rune-java-generator}), so {@code "../test-corpus/..."} resolves
     * to the repo root via the module's parent. Mirrors the convention used
     * by {@code LabelProviderGeneratorTest} + {@code ChoiceObjectGeneratorTest}.
     */
    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    /**
     * Two-root builtins search so synthetic-source tests can resolve
     * {@code string} / {@code int} / annotation declarations. Mirrors
     * {@code LabelProviderGeneratorTest.BUILTINS_SEARCH_ROOTS}.
     */
    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model")
    );

    private final JavaTypeUtil typeUtil = new JavaTypeUtil();
    private final JavaTypeTranslator typeTranslator = new JavaTypeTranslator(typeUtil);

    // === Test 1: streamObjects filters RRule =================================

    @Test
    void streamObjects_filtersRRule_skipsRFunctionAndOthers() throws IOException {
        // Corpus: 1 reporting rule + 1 plain func + 1 plain data type.
        // streamObjects must emit ONLY the rule (mapped via fromRule).
        String source = String.join("\n",
                "namespace com.example.test",
                "",
                "type Trade:",
                "    id string (1..1)",
                "",
                "func Plain:",
                "    inputs: x string (1..1)",
                "    output: result string (1..1)",
                "",
                "reporting rule TradeIdRule from Trade:",
                "    extract id"
        );
        FixtureResult fx = loadFixture(source);
        try {
            RuleGenerator gen = newGenerator(fx);
            List<? extends RFunction> emitted = gen.streamObjects(fx.model).toList();

            assertEquals(1, emitted.size(),
                    "streamObjects must emit exactly one RFunction (the rule); got names: "
                    + emitted.stream().map(RFunction::name).toList());
            assertEquals("TradeIdRule", emitted.get(0).name(),
                    "the streamed RFunction must be the bridged rule");
            // The bridge uses RFunction.fromRule — guard that originRule is
            // populated (the back-pointer is what createTypeRepresentation
            // relies on for namespace resolution).
            assertTrue(emitted.get(0).originRule().isPresent(),
                    "bridged RFunction must carry originRule back-pointer");
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 2: FQN derivation via toFunctionJavaClass =======================

    @Test
    void createTypeRepresentation_derivesFqnFromBridgedSymbolId() throws IOException {
        // FQN derivation must compose the namespace of the source RRule (which
        // IS RModel-attached) with the rule's name. Per T6.0.5 principled
        // origin-dispatch (RFunction.origin() == RULE), the rule routes to
        // <namespace>.reports/<Name>Rule via JavaTypeTranslator.toJavaRuleClass
        // — mirrors upstream lines 127-131 verbatim. The pre-T6.0.5 T5
        // RuleGenerator routed to <namespace>.functions which was wrong.
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Trade:",
                "    id string (1..1)",
                "",
                "reporting rule TradeIdRule from Trade:",
                "    extract id"
        );
        FixtureResult fx = loadFixture(source);
        try {
            RuleGenerator gen = newGenerator(fx);
            RFunction f = gen.streamObjects(fx.model).findFirst().orElseThrow();

            RGeneratedJavaClass<? extends RosettaFunction> clazz = gen.createTypeRepresentation(f);
            assertNotNull(clazz, "createTypeRepresentation must return non-null");

            // Per JavaTypeTranslator.toJavaRuleClass (T6.0.5) — package is
            // <namespace>.reports and simple name is <ruleName>Rule.
            assertEquals("TradeIdRuleRule", clazz.getSimpleName(),
                    "simple name must be <ruleName>Rule per T6.0.5 RULE-origin "
                    + "dispatch (upstream appends Rule suffix verbatim); got "
                    + clazz.getSimpleName());
            assertEquals("com.example.reports.reports",
                    clazz.getPackageName().withDots(),
                    "package must be <namespace>.reports per T6.0.5 RULE-origin "
                    + "dispatch (was <namespace>.functions pre-T6.0.5 which was "
                    + "wrong); got " + clazz.getPackageName().withDots());
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 3: ReportFunction<I,O> base interface emission ==================

    @Test
    void generate_emitsReportFunctionBaseInterface() throws IOException {
        // The generate path delegates to FunctionGenerator.buildClassWithBaseInterface
        // with renderAsReportFunction=true. The emitted source must:
        //   - contain "implements ReportFunction<...>" (simple-name; FQN goes
        //     to imports per T4.0.5 C1 invariant)
        //   - contain "import com.rosetta.model.lib.reports.ReportFunction;"
        //   - NOT contain "implements RosettaFunction" (replaced, not appended)
        //
        // The Trade input type + a rule that extracts a string attribute
        // exercises the synthetic input + output meta-resolution.
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Trade:",
                "    id string (1..1)",
                "",
                "reporting rule TradeIdRule from Trade:",
                "    extract id"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "TradeIdRule");

            assertTrue(code.contains("import com.rosetta.model.lib.reports.ReportFunction;"),
                    "imports must contain ReportFunction FQN; got:\n" + code);
            assertTrue(code.contains(" implements ReportFunction<"),
                    "implements clause must use ReportFunction simple name with type args; got:\n"
                    + code);
            assertFalse(code.contains("implements RosettaFunction"),
                    "default 'implements RosettaFunction' must be REPLACED, not appended; got:\n"
                    + code);
            // Sanity-check the class declaration is abstract + carries the
            // rule's name suffixed with "Rule" (T6.0.5 RULE-origin dispatch
            // appends Rule suffix verbatim per upstream lines 127-131).
            assertTrue(code.contains("public abstract class TradeIdRuleRule "),
                    "class decl must be 'public abstract class TradeIdRuleRule ' "
                    + "per T6.0.5 RULE-origin Rule-suffix; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 3b: PR #117 — chained-nav lambda var from receiver TYPE (real workspace) ===

    @Test
    void generate_chainedNavigation_lambdaVarFromReceiverType() throws IOException {
        // PR #117 end-to-end convention guard (real linked workspace, full rule-body
        // codegen through the expression compiler + NavigationHandler). A chained
        // navigation `fixedRate -> rateSchedule` where the attribute NAME (`fixedRate`)
        // differs from its declared TYPE (`FixedRateSpecification`): the lambda var for
        // the step navigating FROM `fixedRate` must be the lowerCamel of the receiver
        // TYPE (`fixedRateSpecification`), not the feature name (`fixedRate`).
        //
        // SCOPE NOTE (verified empirically): a freshly link-built synthetic model
        // POPULATES `resolvedFeature` on chained steps, so this exercises the
        // resolvedFeature-PRESENT path (asserting the convention both old and new code
        // satisfy) — NOT the resolvedFeature-EMPTY fallbackResolveFeature path PR #117
        // added. That empty-resolvedFeature IR gap is corpus-specific (the linker does
        // not reproduce it for hand-written sources) and is therefore covered by the
        // D11 byte-diff on the 11 real flipped FUNCTION files, not by this unit. This
        // test still guards the end-to-end convention against a future regression in
        // the present-path branch.
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Foo:",
                "    fixedRate FixedRateSpecification (1..1)",
                "",
                "type FixedRateSpecification:",
                "    rateSchedule string (1..1)",
                "",
                "reporting rule FooRateSchedule from Foo:",
                "    extract fixedRate -> rateSchedule"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "FooRateSchedule");
            assertTrue(code.contains("fixedRateSpecification -> fixedRateSpecification.getRateSchedule()"),
                    "chained lambda var must derive from the receiver TYPE "
                    + "(FixedRateSpecification -> fixedRateSpecification); got:\n" + code);
            assertFalse(code.contains("fixedRate -> fixedRate.getRateSchedule()"),
                    "chained lambda var must NOT collapse to the feature name (fixedRate); got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 3c: then-extract function-invocation output keeps MapperS.of(...).get() wrap ===

    @Test
    void generate_thenExtractFunctionInvocation_keepsMapperSOfGetWrap() throws IOException {
        // A reporting rule whose body is `filter <BoolFunc> then <FunctionCall(args)>` routes
        // through renderThenExtractSet (the implicit-then-chain rule-body path). The last
        // then-body is a function INVOCATION with args, which ReferenceHandler compiles to a
        // `MapperS.of(formatId.evaluate(...))` wrap-factory expression. unwrapForAssignment
        // would STRUCTURALLY strip that single-value wrap back to the bare invocation
        // (`output = formatId.evaluate(...);`), but upstream caseThenOperation always `.get()`s
        // the Mapper-valued body without peephole-stripping the redundant wrap — golden emits
        // `output = MapperS.of(formatId.evaluate(...)).get();`. This is the wrap_then_invocation
        // facet (24 drr rule-family POJO sole-flips: Spread*/PackageTransactionSpread* etc.).
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Trade:",
                "    id string (1..1)",
                "    active boolean (1..1)",
                "",
                "func IsActive:",
                "    inputs: trade Trade (1..1)",
                "    output: result boolean (1..1)",
                "    set result: trade -> active",
                "",
                "func FormatId:",
                "    inputs:",
                "        raw string (1..1)",
                "        upper boolean (1..1)",
                "    output: result string (1..1)",
                "    set result: raw",
                "",
                "reporting rule FooFormattedId from Trade:",
                "    filter IsActive",
                "    then FormatId(id, False)"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "FooFormattedId");
            assertTrue(code.contains("output = MapperS.of(formatId.evaluate("),
                    "a then-extract function-invocation output must keep the MapperS.of(...) wrap "
                    + "(upstream caseThenOperation .get()s the Mapper-valued body without stripping "
                    + "the single-value wrap); got:\n" + code);
            assertTrue(code.contains(")).get();"),
                    "the wrapped invocation output must be .get()-unwrapped; got:\n" + code);
            assertFalse(code.contains("output = formatId.evaluate("),
                    "the output must NOT be the structurally-stripped bare invocation; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 3d: no-input function rule body → output = now.evaluate() (no arg, no .get()) ===

    @Test
    void generate_noInputFunctionRuleBody_emitsArglessEvaluateNoGet() throws IOException {
        // A reporting rule whose body is a bare reference to a NO-INPUT function
        // (the CDM `Now`/`Today` shape: a [codeImplementation] function with an
        // output and no `inputs:`) must render `output = currentTime.evaluate();` —
        // the function takes NO argument and its scalar output is assigned directly.
        // The generic compile path miscompiled it to
        // `output = currentTime.evaluate(item.get()).get();` — a spurious implicit
        // `item.get()` argument (ReferenceHandler.renderImplicitFunctionInvocation's
        // missing arity guard; `item` is undefined at rule-body top level) plus a
        // spurious `.get()` (unwrapForAssignment's fall-through) — which does not
        // compile. This is the no_arg_evaluate facet (~15 drr ReportingTimestampRule
        // sole-flips, all calling cdm `Now`).
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Trade:",
                "    id string (1..1)",
                "",
                "func CurrentTime:",
                "    [codeImplementation]",
                "    output:",
                "        result zonedDateTime (1..1)",
                "",
                "reporting rule FooTimestamp from Trade:",
                "    CurrentTime"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "FooTimestamp");
            assertTrue(code.contains("output = currentTime.evaluate();"),
                    "a no-input function rule body must render an argless evaluate() assigned "
                    + "directly (no implicit item.get() arg, no .get()); got:\n" + code);
            assertFalse(code.contains("currentTime.evaluate(item.get())"),
                    "the no-input evaluate must NOT pass a spurious implicit item.get() arg; got:\n" + code);
            assertFalse(code.contains(".evaluate().get()"),
                    "the no-input scalar evaluate must NOT be .get()-unwrapped; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    @Test
    void generate_withInputFunctionRuleBody_emitsEvaluateOnInputNoGet() throws IOException {
        // A reporting rule whose body is a bare reference to a WITH-INPUT function
        // (the function is implicitly invoked on the rule's input) must render
        // `output = getThingId.evaluate(input);` — the rule `input` parameter is
        // passed directly and the function's scalar output is assigned without a
        // trailing `.get()`. The generic compile path miscompiled it to
        // `output = getThingId.evaluate(item.get()).get();` — a spurious implicit
        // `item.get()` argument (ReferenceHandler.renderImplicitFunctionInvocation's
        // missing arity guard; `item` is undefined at rule-body top level) plus a
        // spurious `.get()` (unwrapForAssignment's fall-through) — which does not
        // compile. The FUNCTION sibling of the bare-RULE top-level case
        // (generate_bareRuleTopLevelSetOutput_invokesOnInput) and the WITH-input
        // generalisation of the no_arg_evaluate fast-path
        // (generate_noInputFunctionRuleBody_emitsArglessEvaluateNoGet). ~8 drr POJO
        // sole-flips (iosco cde Direction1*/Direction2*PartyLeg* + common ContractType).
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Trade:",
                "    id string (1..1)",
                "",
                "func GetThingId:",
                "    [codeImplementation]",
                "    inputs:",
                "        trade Trade (1..1)",
                "    output:",
                "        result string (1..1)",
                "",
                "reporting rule FooThing from Trade:",
                "    GetThingId"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "FooThing");
            assertTrue(code.contains("output = getThingId.evaluate(input);"),
                    "a with-input function rule body must invoke the function on the rule "
                    + "`input` parameter and assign its scalar output directly "
                    + "(`output = getThingId.evaluate(input);`); got:\n" + code);
            assertFalse(code.contains("getThingId.evaluate(item.get())"),
                    "the top-level form must use the `input` parameter, NOT the in-lambda "
                    + "`item.get()` binding (`item` is undefined outside a lambda); got:\n" + code);
            assertFalse(code.contains(".evaluate(input).get()"),
                    "the with-input scalar evaluate must NOT be .get()-unwrapped; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    @Test
    void generate_thenConditionalBody_emitsIfThenElseResultBlock() throws IOException {
        // A reporting rule whose body is `filter <BoolRule> then if <cond> then <A>
        // else <B>` routes the conditional through renderThenExtractSet as the LAST
        // then-body. ControlFlowHandler compiles a conditional to a TERNARY
        // (`<cond>.getOrDefault(false) ? <then> : <else>`) — on the RULE path the
        // ifthenelse_result_hoisting session never opens (FunctionGenerator's
        // hoistSessionEligible gate), so the ternary fallback is structural here —
        // and unwrapForAssignment's
        // fall-through naively appends `.get()` to the ternary string — binding it to
        // the ELSE branch only (`? a : b.get()`), a non-compiling mixed-type ternary.
        // Upstream renders a then-body conditional as the JavaIfThenElseBuilder
        // collapse form — a typed `ifThenElseResult` temporary holding the Mapper
        // result, an `if/else` block assigning each branch, then `output =
        // ifThenElseResult.get();` (the conditional_block_then facet; the dominant
        // drr rule-family POJO residual — flat single-then two-branch sole-flips
        // across csa/cftc/esma/fca trade+dtcc reports).
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Trade:",
                "    id string (1..1)",
                "    altId string (1..1)",
                "    active boolean (1..1)",
                "",
                "reporting rule TradeActive from Trade:",
                "    active",
                "",
                "reporting rule PrimaryId from Trade:",
                "    id",
                "",
                "reporting rule SecondaryId from Trade:",
                "    altId",
                "",
                "reporting rule FooConditional from Trade:",
                "    filter TradeActive",
                "    then if TradeActive = True then PrimaryId else SecondaryId"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "FooConditional");
            assertTrue(code.contains("final MapperS<String> ifThenElseResult;"),
                    "a then-body conditional must declare a typed `ifThenElseResult` "
                    + "temporary holding the Mapper result (the JavaIfThenElseBuilder "
                    + "collapse form), NOT a ternary; got:\n" + code);
            assertTrue(code.contains("ifThenElseResult = MapperS.of(primaryIdRule.evaluate(thenArg.get()));"),
                    "the then-branch must assign the (wrapped) Mapper to the temporary; got:\n" + code);
            assertTrue(code.contains("} else {"),
                    "the conditional must render as an if/else block; got:\n" + code);
            assertTrue(code.contains("output = ifThenElseResult.get();"),
                    "the output must unwrap the `ifThenElseResult` temporary; got:\n" + code);
            assertFalse(code.contains("? MapperS.of("),
                    "the then-body conditional must NOT render as a ternary (the "
                    + "`? a : b.get()` form is non-compiling); got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    @Test
    void generate_elselessThenConditionalBody_emitsIfThenElseResultBlockWithOfNullElse() throws IOException {
        // stmt_elseless_conditional_block facet (engine, drr rule-family): a reporting rule
        // whose body is `filter <BoolRule> then if <cond> then <A>` with NO explicit else
        // routes the conditional through renderThenExtractSet as the LAST then-body, at the
        // assignOutput STATEMENT level (NOT a lambda map body — PR #140's else-less analogue
        // lives in CollectionHandler.compileElselessConditionalBlock). DefaultElseRule
        // synthesises an empty-list else, so ControlFlowHandler renders a ternary
        // `<cond>.getOrDefault(false) ? <then> : MapperC.of()`; unwrapForAssignment then
        // appends `.get()` to the ternary string, binding it to the MapperC.of() else only —
        // a non-compiling `? <then> : MapperC.of().get()` (mixed MapperS/MapperC, plus a
        // spurious MapperC import). Upstream renders the if/else block with a typed
        // MapperS.<T>ofNull() default else (the statement-level complement of PR #135's
        // present-else appendThenConditionalBlock + PR #140's lambda-body
        // compileElselessConditionalBlock). Corpus witness: drr Direction2Leg1Rule /
        // Direction2Leg2Rule across asic/hkma/... trade reports.
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Trade:",
                "    id string (1..1)",
                "    active boolean (1..1)",
                "",
                "reporting rule TradeActive from Trade:",
                "    active",
                "",
                "reporting rule PrimaryId from Trade:",
                "    id",
                "",
                "reporting rule FooElselessConditional from Trade:",
                "    filter TradeActive",
                "    then if TradeActive = True then PrimaryId"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "FooElselessConditional");
            assertTrue(code.contains("final MapperS<String> ifThenElseResult;"),
                    "an else-less then-body conditional must declare a typed `ifThenElseResult` "
                    + "temporary (the JavaIfThenElseBuilder collapse form), NOT a ternary; got:\n" + code);
            assertTrue(code.contains("ifThenElseResult = MapperS.of(primaryIdRule.evaluate(thenArg.get()));"),
                    "the then-branch must assign the (wrapped) Mapper to the temporary; got:\n" + code);
            assertTrue(code.contains("} else {"),
                    "the conditional must render as an if/else block; got:\n" + code);
            assertTrue(code.contains("ifThenElseResult = MapperS.<String>ofNull();"),
                    "the implicit (empty-list) else must assign a typed `MapperS.ofNull()` default, "
                    + "NOT `MapperC.of()`; got:\n" + code);
            assertTrue(code.contains("output = ifThenElseResult.get();"),
                    "the output must unwrap the `ifThenElseResult` temporary; got:\n" + code);
            assertFalse(code.contains("MapperC.of()"),
                    "the spurious MapperC empty-list-else usage/import must drop once the if/else "
                    + "block replaces the ternary; got:\n" + code);
            assertFalse(code.contains("? MapperS.of("),
                    "the else-less then-body conditional must NOT render as a ternary (the "
                    + "`? <then> : MapperC.of().get()` form is non-compiling); got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    @Test
    void generate_elselessConditionalMapBody_emitsIfReturnBlock() throws IOException {
        // elseless_conditional_block facet (engine, drr rule-family): a reporting rule
        // whose extract/map body is an else-less conditional (`extract if <cond> then
        // <value>`) sits in a mapSingleToItem (MapperS-expecting) position. DefaultElseRule
        // synthesises an empty-list else, so ControlFlowHandler renders a ternary
        // `<cond>.getOrDefault(false) ? <then> : MapperC.of()` — which does NOT compile
        // (the mapSingleToItem lambda needs a MapperS, but the MapperC.of() else branch is
        // a MapperC). Upstream renders the if/return block with a typed MapperS.ofNull()
        // default instead (the lambda-position analogue of PR #135's appendThenConditionalBlock,
        // which excludes exactly the else-less case). Corpus witness: drr ActionTypeRule /
        // ConfirmedRule / IntragroupRule (else-less `if ... then ...` map bodies).
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Trade:",
                "    id string (1..1)",
                "    active boolean (1..1)",
                "",
                "reporting rule ActiveId from Trade:",
                "    extract if active = True then id"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "ActiveId");
            assertTrue(code.contains("mapSingleToItem(item -> {"),
                    "an else-less conditional map body must open a block lambda "
                    + "(`mapSingleToItem(item -> {`), NOT a ternary; got:\n" + code);
            assertTrue(code.contains(".getOrDefault(false)) {"),
                    "the condition must guard an `if (...) {` block; got:\n" + code);
            assertTrue(code.contains("return MapperS.<String>ofNull();"),
                    "the implicit (empty-list) else must render as a typed `MapperS.ofNull()` "
                    + "default return, NOT `MapperC.of()`; got:\n" + code);
            assertFalse(code.contains("getOrDefault(false) ?"),
                    "the else-less conditional must NOT render as a ternary "
                    + "(`<cond>.getOrDefault(false) ? <then> : MapperC.of()` is non-compiling "
                    + "in a mapSingleToItem MapperS-to-MapperS lambda); got:\n" + code);
            assertFalse(code.contains("MapperC.of()"),
                    "the spurious MapperC empty-list-else import/usage must drop once the "
                    + "block path replaces the ternary; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    @Test
    void generate_elselessConditionalMapBody_bareEnumThen_wrapsInMapperSOfBlock() throws IOException {
        // enum_const_then facet (PR #141 facet G, builds on PR #140's elseless_conditional_block):
        // when the then-branch of an else-less SINGLE conditional map body is a BARE ENUM constant,
        // ReferenceHandler renders it UNWRAPPED as `Enum.VALUE` (a non-Mapper expression — the
        // qualified REnumValueRef enumeration-present branch / the bare-REnumValue Category-13
        // branch). A bare `return Enum.VALUE;` would not compile against the mapSingleToItem
        // (MapperS<F>-returning) lambda, so compileElselessConditionalBlock WRAPS it
        // `return MapperS.of(Enum.VALUE);` inside the if/return block (the golden shape; PR #140
        // declined this case, PR #141 handles it). String / number / boolean literal then-branches
        // self-wrap in MapperS.of via LiteralHandler, so they are NOT re-wrapped.
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "enum Color:",
                "    RED",
                "    GREEN",
                "",
                "type Trade:",
                "    active boolean (1..1)",
                "",
                "reporting rule ActiveColor from Trade:",
                "    extract if active = True then Color -> RED"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "ActiveColor");
            assertTrue(code.contains("mapSingleToItem(item -> {"),
                    "a bare-enum else-less conditional map body must open the if/return block "
                    + "lambda (`mapSingleToItem(item -> {`), NOT a ternary; got:\n" + code);
            assertTrue(code.contains("return MapperS.of(Color.RED);"),
                    "the bare-enum then-branch must be WRAPPED `return MapperS.of(Color.RED);` "
                    + "(a MapperS, satisfying the mapSingleToItem lambda return), NOT a bare "
                    + "`return Color.RED;` (non-compiling); got:\n" + code);
            assertTrue(code.contains("return MapperS.<Color>ofNull();"),
                    "the implicit (empty-list) else must render as a typed `MapperS.<Color>ofNull()` "
                    + "default return; got:\n" + code);
            assertFalse(code.contains("return Color.RED;"),
                    "the bare-enum then-branch must NOT be emitted UNWRAPPED as a non-compiling "
                    + "`return Color.RED;`; got:\n" + code);
            assertFalse(code.contains("MapperC.of()"),
                    "the spurious MapperC empty-list-else (ternary path) must not appear once the "
                    + "block wraps the bare enum; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 4: Empty model emits nothing ====================================

    @Test
    void streamObjects_emptyModel_emitsNothing() throws IOException {
        // A model with no RRule root elements must produce an empty stream.
        String source = String.join("\n",
                "namespace com.example.test",
                "",
                "type Trade:",
                "    id string (1..1)",
                "",
                "func Plain:",
                "    inputs: x string (1..1)",
                "    output: result string (1..1)"
        );
        FixtureResult fx = loadFixture(source);
        try {
            RuleGenerator gen = newGenerator(fx);
            List<? extends RFunction> emitted = gen.streamObjects(fx.model).toList();
            assertEquals(0, emitted.size(),
                    "model with no RRules must stream nothing; got " + emitted.size());
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 5: Multiple rules in one model =================================

    @Test
    void streamObjects_multipleRules_emitsAllIndependently() throws IOException {
        // Two reporting rules — both must be streamed; each must produce a
        // distinct generated class (different simple name).
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Trade:",
                "    id string (1..1)",
                "    description string (0..1)",
                "",
                "reporting rule TradeIdRule from Trade:",
                "    extract id",
                "",
                "reporting rule TradeDescriptionRule from Trade:",
                "    extract description"
        );
        FixtureResult fx = loadFixture(source);
        try {
            RuleGenerator gen = newGenerator(fx);
            List<? extends RFunction> emitted = gen.streamObjects(fx.model).toList();
            assertEquals(2, emitted.size(),
                    "both rules must be streamed; got names: "
                    + emitted.stream().map(RFunction::name).toList());

            List<String> classNames = new ArrayList<>();
            for (RFunction f : emitted) {
                classNames.add(gen.createTypeRepresentation(f).getSimpleName());
            }
            // Per T6.0.5 RULE-origin dispatch, the class simple name is
            // <ruleName>Rule (upstream appends a literal Rule suffix). Real
            // corpora use rule names that don't end in "Rule" — the synthetic
            // names here are intentionally generic and produce <Name>Rule.
            assertTrue(classNames.contains("TradeIdRuleRule"),
                    "TradeIdRule class must be generated as TradeIdRuleRule per "
                    + "T6.0.5 RULE-origin Rule-suffix; got " + classNames);
            assertTrue(classNames.contains("TradeDescriptionRuleRule"),
                    "TradeDescriptionRule class must be generated as "
                    + "TradeDescriptionRuleRule per T6.0.5; got " + classNames);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 6: Eligibility rule (kind variant; same emission path) =========

    @Test
    void streamObjects_eligibilityRule_streamedSameAsReportingRule() throws IOException {
        // Per grammar paste-quote (RosettaParser.g4 `reportingRule` /
        // `eligibilityRule` rules), eligibility rules are RRule instances
        // with kind=ELIGIBILITY — they share the same RRule type and
        // therefore stream identically. Upstream's RuleGenerator.xtend
        // `streamObjects` filters by `instanceof RosettaRule` without
        // distinguishing kind; we mirror that.
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Trade:",
                "    id string (1..1)",
                "    active boolean (1..1)",
                "",
                "eligibility rule IsActive from Trade:",
                "    filter active"
        );
        FixtureResult fx = loadFixture(source);
        try {
            RuleGenerator gen = newGenerator(fx);
            List<? extends RFunction> emitted = gen.streamObjects(fx.model).toList();
            assertEquals(1, emitted.size(),
                    "eligibility rule must be streamed identically to reporting "
                    + "rule; got names: "
                    + emitted.stream().map(RFunction::name).toList());
            assertEquals("IsActive", emitted.get(0).name(),
                    "the streamed RFunction's name must match the eligibility rule");

            // And the generate path must still emit the ReportFunction base
            // interface — the kind difference doesn't change emission shape.
            String code = generateFor(fx, "IsActive");
            assertTrue(code.contains("import com.rosetta.model.lib.reports.ReportFunction;"),
                    "eligibility rule must also import ReportFunction; got:\n" + code);
            assertTrue(code.contains(" implements ReportFunction<"),
                    "eligibility rule must also implement ReportFunction<I,O>; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 7: Cardinality edge case (spec § 4.1.6) =========================

    /**
     * Spec § 4.1.6 — "Rule with no inputs/outputs (edge case) — verify error
     * handling vs upstream". The synthetic {@link RFunction} produced by
     * {@link RFunction#fromRule(com.regnosys.rosetta.ast.functions.RRule)}
     * always sets one input + one output (mirrors upstream
     * {@code createArtificialAttribute} pattern), so the error-handling
     * branches in {@link RuleGenerator#generate} are unreachable from
     * normal grammar input. This test exercises them directly by mutating
     * the synthetic's inputs / output after construction — locking the
     * fail-fast diagnostic contract so a future refactor that bypasses the
     * factory invariant produces a workspace-construction-time error rather
     * than a downstream {@code IndexOutOfBoundsException} / silent
     * miscompilation.
     */
    @Test
    void generate_emptyInputsOrMissingOutput_throwsWithDiagnostic() throws IOException {
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Trade:",
                "    id string (1..1)",
                "",
                "reporting rule TradeIdRule from Trade:",
                "    extract id"
        );
        FixtureResult fx = loadFixture(source);
        try {
            RuleGenerator gen = newGenerator(fx);
            // -- 7a: empty inputs -------------------------------------------
            // Synthetic produced by RFunction.fromRule is not yet frozen, so
            // we can clear inputs() to simulate the bug-shape that motivates
            // the Code-F1 guard.
            RFunction emptyInputs = gen.streamObjects(fx.model).findFirst().orElseThrow();
            emptyInputs.inputs().clear();
            RGeneratedJavaClass<? extends RosettaFunction> clazz1 =
                    gen.createTypeRepresentation(emptyInputs);
            IllegalStateException isEmpty = assertThrows(IllegalStateException.class,
                    () -> gen.generate(emptyInputs, clazz1, "1.0"),
                    "generate() must fail-fast when synthetic RFunction has no inputs");
            assertTrue(isEmpty.getMessage().contains("no input attribute"),
                    "diagnostic must name the missing surface 'no input attribute'; got: "
                    + isEmpty.getMessage());
            assertTrue(isEmpty.getMessage().contains("TradeIdRule"),
                    "diagnostic must include the function name 'TradeIdRule' for triage; got: "
                    + isEmpty.getMessage());

            // -- 7b: missing output ----------------------------------------
            RFunction missingOutput = gen.streamObjects(fx.model).findFirst().orElseThrow();
            missingOutput.setOutput(null);
            RGeneratedJavaClass<? extends RosettaFunction> clazz2 =
                    gen.createTypeRepresentation(missingOutput);
            IllegalStateException isMissing = assertThrows(IllegalStateException.class,
                    () -> gen.generate(missingOutput, clazz2, "1.0"),
                    "generate() must fail-fast when synthetic RFunction has no output");
            assertTrue(isMissing.getMessage().contains("no output attribute"),
                    "diagnostic must name the missing surface 'no output attribute'; got: "
                    + isMissing.getMessage());
            assertTrue(isMissing.getMessage().contains("TradeIdRule"),
                    "diagnostic must include the function name 'TradeIdRule' for triage; got: "
                    + isMissing.getMessage());
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 8 (Phase X1 T2.0): back-fill of output.typeCall from expression ===

    /**
     * Phase X1 § 4.3 — body emission requires the synthetic output attribute's
     * {@code typeCall} to be back-filled at codegen time from the rule
     * expression's inferred type. Without the back-fill,
     * {@code generatorModel.getType(output)} returns {@code RMissingType} which
     * maps to {@code Object} in the emission — wrong vs the legacy plugin which
     * compiles {@code extract id} (string-typed) to a string-typed output.
     *
     * <p>This test asserts the back-fill is applied end-to-end. The M3 implicit-
     * input resolution at T2.0 (Categories 8+9+10) is what makes
     * {@code ws.getInferredType(expr)} return {@code string} (non-MISSING) for
     * the {@code extract id} rule body. Without that, the back-fill would throw
     * {@code IllegalStateException}.
     */
    @Test
    void generate_backfillsOutputTypeCallFromExpressionType() throws IOException {
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Trade:",
                "    id string (1..1)",
                "",
                "reporting rule TradeIdRule from Trade:",
                "    extract id"
        );
        FixtureResult fx = loadFixture(source);
        try {
            RuleGenerator gen = newGenerator(fx);
            RFunction f = gen.streamObjects(fx.model)
                    .filter(rf -> "TradeIdRule".equals(rf.name()))
                    .findFirst()
                    .orElseThrow();

            String code = gen.generate(f, gen.createTypeRepresentation(f), "1.0");

            // Semantic: back-fill mutates the synthetic output's typeCall to
            // the inferred RType's name ("string") prior to emission.
            assertNotNull(f.output().orElseThrow().typeCall(),
                    "output.typeCall must be back-filled from rule expression "
                    + "inferred type at codegen time; was null");
            assertEquals("string", f.output().orElseThrow().typeCall().typeName(),
                    "output.typeCall.typeName must equal 'string' (the inferred "
                    + "RType.name() of `extract id` over Trade where id is string); got: "
                    + f.output().orElseThrow().typeCall().typeName());

            // Emission: ReportFunction<I,O> output slot must be the string-typed
            // Java reference type — String — NOT Object. Without back-fill the
            // RMissingType.INSTANCE → OBJECT path would surface "Object>" in the
            // type argument list.
            assertFalse(code.contains("ReportFunction<Trade, Object>"),
                    "without back-fill, the emission falls back to Object output — "
                    + "the typeCall back-fill must run before toMetaJavaType; got:\n"
                    + code);
            assertTrue(code.contains("ReportFunction<Trade, String>"),
                    "ReportFunction output type argument must be String (resolved "
                    + "from the back-filled output.typeCall='string'); got:\n"
                    + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 9 (Copilot PR #76 R19 F1): MULTI cardinality back-fill =============

    /**
     * Phase X1 § 4.3 — when the rule expression's inferred cardinality is
     * {@link com.regnosys.rosetta.types.ExpressionCardinality#MULTI} the
     * codegen path must emit a list-shaped output ({@code List<T>} rather than
     * {@code T} in the {@code ReportFunction<I, O>} type argument and the
     * {@code assignOutput} signature). {@code RuleGenerator#generate} contains
     * a cardinality back-fill that sets the synthetic output's cardinality to
     * unbounded based on {@code ws.getCardinality(expr) == MULTI}; R19 F1
     * flagged the missing test coverage on this path.
     *
     * <p>This test exercises the contract behaviourally — source has a
     * {@code (0..*)} attribute extracted in the rule body; the emitted Java
     * must use the list-shaped output. We assert against the emission only,
     * not against {@code output.cardinality()} internal state: the back-fill
     * code path mutates the synthetic {@link RAttribute} via
     * {@code setCardinality(...)}, but reflecting that state through
     * {@code f.output().orElseThrow().cardinality()} after {@code generate(...)}
     * returned an inconsistent {@code isUnbounded()} flag in spot-checks —
     * either the synthetic attribute freezes mid-generate or the list-shaping
     * is driven by a code path other than {@code output.cardinality()}. The
     * emission shape is the observable behavioural contract; this test pins
     * it directly (paste-quoted assertions on {@code code.contains(...)}).
     *
     * <p>Source shape: {@code type Trade { id string (0..*) }} +
     * {@code extract id} over {@code Trade} —
     * {@link com.regnosys.rosetta.types.inference.CardinalityComputer#compute}
     * returns {@code MULTI} for every {@code RExtractExpr} unconditionally, so
     * the back-fill branch fires.
     */
    @Test
    void generate_multiCardinalityBody_emitsListShapedOutput() throws IOException {
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Trade:",
                "    id string (0..*)",
                "",
                "reporting rule TradeIdsRule from Trade:",
                "    extract id"
        );
        FixtureResult fx = loadFixture(source);
        try {
            RuleGenerator gen = newGenerator(fx);
            RFunction f = gen.streamObjects(fx.model)
                    .filter(rf -> "TradeIdsRule".equals(rf.name()))
                    .findFirst()
                    .orElseThrow();

            String code = gen.generate(f, gen.createTypeRepresentation(f), "1.0");

            // doEvaluate + assignOutput signatures must use List<String> —
            // the multi-cardinality emission path is the observable contract
            // pinned by this test. A synthetic single-valued output would
            // render as `String doEvaluate(Trade input)` + `String assignOutput(
            // String output, Trade input)`. The (0..*) source attribute +
            // RExtractExpr unconditionally-MULTI cardinality (CardinalityComputer
            // line 62) drives the codegen wrapping decision.
            assertTrue(code.contains("List<String> doEvaluate(Trade input)"),
                    "doEvaluate signature must return List<String> for MULTI "
                    + "expression cardinality; got:\n" + code);
            assertTrue(code.contains("List<String> assignOutput(List<String> output"),
                    "assignOutput signature must take and return List<String> "
                    + "for MULTI expression cardinality; got:\n" + code);

            // Note: the outer `ReportFunction<Trade, String>` interface type
            // argument currently stays single-shape even when the rule body is
            // multi — that surface uses `inferredOutputType` (the bare RType
            // name) directly, bypassing `output.cardinality()`. Whether to flip
            // it is a follow-on emission concern (separate from the R19 F1
            // back-fill scope); pinning just the inner-method signatures here
            // matches the observable behaviour today and lets the outer-shape
            // decision land in a future PR with explicit byte-diff vs the
            // legacy plugin output.
            assertTrue(code.contains("ReportFunction<Trade, "),
                    "ReportFunction type argument list must be present (regardless "
                    + "of single vs multi shape); got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 10 (Phase X1 T0n / Gap E): bare enum value in conditional branch ==

    /**
     * Phase X1 T0n (Gap E) — end-to-end through the generator: a bare enum
     * value in a conditional branch resolves at inference time (Category 13)
     * and renders as the Java enum constant. {@code if flag then Color -> RED
     * else GREEN}: the else-branch {@code GREEN} parses as an unresolved
     * {@code RSymbolReference}; the sibling {@code Color -> RED} branch seeds
     * the expected enum type {@code Color}, so {@code GREEN} binds to the enum
     * value {@code Color.GREEN}. The renderer's {@code REnumValue} branch
     * ({@code ReferenceHandler}) emits {@code Color.GREEN} UNWRAPPED — matching
     * the qualified {@code Color -> RED} path that emits {@code Color.RED}.
     * Without Gap E, {@code GREEN} would mis-render as a bare-variable
     * {@code MapperS.of(GREEN)}.
     */
    @Test
    void generate_conditionalBareEnumBranch_emitsEnumConstant() throws IOException {
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "enum Color:",
                "    RED",
                "    GREEN",
                "",
                "type Trade:",
                "    flag boolean (1..1)",
                "",
                "reporting rule PickColorRule from Trade:",
                "    if flag then Color -> RED else GREEN"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "PickColorRule");

            assertTrue(code.contains("Color.GREEN"),
                    "bare enum-value branch GREEN must render as the Java enum "
                    + "constant `Color.GREEN` (Gap E renderer branch); got:\n" + code);
            assertTrue(code.contains("Color.RED"),
                    "qualified enum-value branch `Color -> RED` must render as "
                    + "`Color.RED` (existing REnumValueRef path); got:\n" + code);
            assertFalse(code.contains("MapperS.of(GREEN)"),
                    "bare enum value GREEN must NOT render as a bare-variable "
                    + "`MapperS.of(GREEN)`; the Gap E enum-value branch must win; "
                    + "got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 11 (Phase X1 T0n / Gap E): bare enum operand across `=` (mangled) =

    /**
     * Phase X1 T0n (Gap E) comparison-operand sub-shape — end-to-end: a bare
     * enum value as a comparison operand resolves at inference time (Category
     * 14) and renders as the MANGLED Java enum constant. {@code settlementType
     * = Cash}: the RHS {@code Cash} parses as an unresolved bare
     * {@code RSymbolReference}; the sibling attribute operand types as
     * {@code SettlementTypeEnum}, so {@code Cash} binds to the enum value
     * {@code SettlementTypeEnum.Cash}. The renderer must emit the mangled
     * constant {@code SettlementTypeEnum.CASH} ({@code EnumHelper.convertValue}
     * maps source {@code Cash} -> Java {@code CASH}) — NOT raw
     * {@code SettlementTypeEnum.Cash}, NOT bare-variable {@code MapperS.of(Cash)}.
     * Category 14 is byte-identity only: the comparison itself stays BOOLEAN,
     * so this shape does not reduce the fail-fast count.
     */
    @Test
    void generate_comparisonBareEnumOperand_emitsMangledEnumConstant() throws IOException {
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "enum SettlementTypeEnum:",
                "    Cash",
                "    Physical",
                "",
                "type Trade:",
                "    settlementType SettlementTypeEnum (1..1)",
                "",
                "reporting rule IsCashRule from Trade:",
                "    settlementType = Cash"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "IsCashRule");

            assertTrue(code.contains("SettlementTypeEnum.CASH"),
                    "bare enum operand Cash must render as the MANGLED Java enum "
                    + "constant `SettlementTypeEnum.CASH` (EnumHelper.convertValue "
                    + "maps `Cash` -> `CASH`); got:\n" + code);
            assertFalse(code.contains("MapperS.of(Cash)"),
                    "bare enum operand Cash must NOT render as a bare-variable "
                    + "`MapperS.of(Cash)`; the Gap E enum-value branch must win; "
                    + "got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 11b (PR #130 / enum_ref_import): enum-value-ref fn-arg import =====

    /**
     * PR #130 (facet {@code enum_ref_import}): a qualified enum-value reference
     * ({@code EnumName -> VALUE}) used as a FUNCTION-CALL ARGUMENT in a rule body
     * renders the constant {@code EnumName.VALUE} correctly but DROPS the enum's
     * import — {@code ReferenceHandler.handle(REnumValueRef)} emitted the constant
     * via {@code JavaExpression.from(name, null)} with an empty refs channel,
     * relying on an "ambient import contributed by the output / attribute type"
     * (the comment on the bare-enum branch). That assumption holds for a
     * comparison-operand / output enum, but FAILS when the enum is a function
     * argument whose type is otherwise unreferenced by the rule — the generated
     * Java then uses the simple name {@code RoundingDirectionEnum.NEAREST} with NO
     * matching import, which does not compile.
     *
     * <p>The enum lives in a DIFFERENT namespace ({@code com.example.math}) from
     * the rule ({@code com.example.reports}) so the import is genuinely required
     * (a same-package enum would need no import and would not reproduce the bug).
     * This mirrors the corpus shape: 11 drr esma/hkma {@code ExcessCollateral*} /
     * margin report rules call {@code cdm.base.math.RoundToPrecision(value, 5,
     * RoundingDirectionEnum -> NEAREST)} and drop {@code import
     * cdm.base.math.RoundingDirectionEnum;} as their sole divergence.
     *
     * <p>Fix: register the enum's {@code JavaClass} in the refs channel
     * ({@code from(code, null, Set.of(enumClass))}) — type stays {@code null} so
     * rendering / coercion is byte-unchanged; only the import is added.
     */
    @Test
    void generate_enumValueRefAsFunctionArg_importsEnumType() throws IOException {
        String mathSource = String.join("\n",
                "namespace com.example.math",
                "",
                "enum RoundingDirectionEnum:",
                "    NEAREST",
                "    UP",
                "",
                "func RoundIt:",
                "    inputs:",
                "        value number (1..1)",
                "        direction RoundingDirectionEnum (1..1)",
                "    output:",
                "        result number (1..1)",
                "    set result:",
                "        value"
        );
        String reportSource = String.join("\n",
                "namespace com.example.reports",
                "",
                "import com.example.math.*",
                "",
                "type Trade:",
                "    amount number (1..1)",
                "",
                "reporting rule RoundAmountRule from Trade:",
                "    RoundIt(amount, RoundingDirectionEnum -> NEAREST)"
        );
        FixtureResult fx = loadFixtureMulti(List.of(reportSource, mathSource), m -> true);
        try {
            String code = generateFor(fx, "RoundAmountRule");

            // Sanity: the enum constant is emitted by simple name (the rendering
            // is already correct — only the import is missing).
            assertTrue(code.contains("RoundingDirectionEnum.NEAREST"),
                    "the enum-value-ref argument must render as the Java constant "
                    + "`RoundingDirectionEnum.NEAREST`; got:\n" + code);
            // THE bug: the enum's import must be present, else the simple-name
            // reference does not compile.
            assertTrue(code.contains("import com.example.math.RoundingDirectionEnum;"),
                    "the enum type referenced only via the function-call argument "
                    + "`RoundingDirectionEnum -> NEAREST` must be imported "
                    + "(`import com.example.math.RoundingDirectionEnum;`); the refs "
                    + "channel of the enum-constant emission was empty, dropping it; "
                    + "got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 11c (PR #137 / enum_const_arg): BARE enum-value fn-arg ============

    /**
     * PR #137 (facet {@code enum_const_arg}): a BARE enum value ({@code Nearest},
     * NOT the qualified {@code RoundingDirectionEnum -> Nearest}) used as a
     * FUNCTION-CALL ARGUMENT in a rule body. Unlike a comparison operand or a
     * conditional branch (Gap E, Tests 10/11) — where the sibling operand seeds
     * the expected enum type and the {@code TypeInferenceEngine} binds the bare
     * symbol to its {@code REnumValue} — a bare enum value in argument position is
     * NOT type-directed-resolved: it stays an UNRESOLVED bare
     * {@code RSymbolReference} ({@code symbol().isEmpty()}) with no per-argument
     * expected type, so the variable path renders the bare Rune value name
     * {@code Nearest} (a non-compiling undefined symbol).
     *
     * <p>The QUALIFIED sibling ({@code RoundingDirectionEnum -> Nearest}, Test 11b)
     * already renders {@code RoundingDirectionEnum.NEAREST} + the import. This
     * facet pins the bare value via the callee's DECLARED enum parameter type:
     * the explicit-args loop resolves the bare name against
     * {@code calleeFn.inputs().get(argIndex)}'s {@link
     * com.regnosys.rosetta.ast.types.REnumeration} and emits the same qualified
     * Java constant ({@code EnumHelper.convertValue}: {@code Nearest} -> {@code
     * NEAREST}) + import. Mirrors the corpus shape: 7 drr hkma margin
     * {@code Initial/VariationMargin*Counterparty1*HaircutRule}s call
     * {@code RoundToPrecision(item, 5, Nearest)} — bare, no qualifier — and emit
     * the non-compiling bare {@code Nearest} as their sole divergence.
     *
     * <p>The enum lives in a DIFFERENT namespace ({@code com.example.math}) so the
     * import is genuinely required (a same-package enum would not reproduce the
     * missing-import half). Regression-safe by construction: the pre-fix bare
     * {@code Nearest} does not compile, so no currently-green file carries it.
     */
    @Test
    void generate_bareEnumValueAsFunctionArg_emitsQualifiedConstantAndImport() throws IOException {
        String mathSource = String.join("\n",
                "namespace com.example.math",
                "",
                "enum RoundingDirectionEnum:",
                "    Nearest",
                "    Up",
                "",
                "func RoundIt:",
                "    inputs:",
                "        value number (1..1)",
                "        direction RoundingDirectionEnum (1..1)",
                "    output:",
                "        result number (1..1)",
                "    set result:",
                "        value"
        );
        String reportSource = String.join("\n",
                "namespace com.example.reports",
                "",
                "import com.example.math.*",
                "",
                "type Trade:",
                "    amount number (1..1)",
                "",
                "reporting rule RoundAmountRule from Trade:",
                "    RoundIt(amount, Nearest)"
        );
        FixtureResult fx = loadFixtureMulti(List.of(reportSource, mathSource), m -> true);
        try {
            String code = generateFor(fx, "RoundAmountRule");

            // The bare value resolves to the MANGLED Java constant (Nearest -> NEAREST)
            // qualified by the parameter's enum type — matching the qualified path.
            assertTrue(code.contains("RoundingDirectionEnum.NEAREST"),
                    "the bare enum-value argument `Nearest` must render as the Java "
                    + "constant `RoundingDirectionEnum.NEAREST` (resolved against the "
                    + "callee's declared enum parameter type, EnumHelper.convertValue "
                    + "maps `Nearest` -> `NEAREST`); got:\n" + code);
            // The pre-fix bug: the bare value rendered as a bare-variable.
            assertFalse(code.contains("MapperS.of(Nearest)") || code.contains(", Nearest)"),
                    "the bare enum value must NOT render as the bare Rune value name "
                    + "`Nearest` (the non-compiling variable-path form); got:\n" + code);
            // The enum's import must be present (cross-namespace, function-arg only).
            assertTrue(code.contains("import com.example.math.RoundingDirectionEnum;"),
                    "the enum type referenced only via the bare function-call argument "
                    + "`Nearest` must be imported (`import "
                    + "com.example.math.RoundingDirectionEnum;`); got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === enum_arg_qualify (PR #143): bare enum arg false-resolved to an RBody =====

    /**
     * PR #143 (facet {@code enum_arg_qualify}): a BARE enum value used as a
     * function-call ARGUMENT whose acronym name COLLIDES with a regulatory
     * {@code body Authority <NAME>} declaration. The linker false-resolves the bare
     * reference's symbol to the {@link com.regnosys.rosetta.ast.regulatory.RBody}
     * (a real global named e.g. {@code ASIC}), so PR #137's
     * {@code ReferenceHandler.tryBareEnumArg} declines — its shape gate rejects any
     * bare ref whose {@code symbol().isPresent()}, and here the present symbol is the
     * RBody, not an enum value. The arg then falls through to the variable path and
     * renders the bare Rune name {@code ASIC} (no qualifier, no import) — a
     * non-compiling undefined symbol.
     *
     * <p>Distinct from
     * {@link #generate_bareEnumValueAsFunctionArg_emitsQualifiedConstantAndImport}
     * (PR #137), where the bare value is UNRESOLVED (no colliding body) so the gate
     * passes. Corpus shape: drr {@code PriorUTIRule}/{@code PriorUTIProprietaryRule}
     * across asic/hkma/jfsa/mas call {@code GetPriorTransactionIdentifier(item, ASIC)}
     * with the bare regime acronym colliding with {@code body Authority ASIC}. The
     * fix relaxes the gate to also accept a symbol that is an {@code RBody}, then
     * resolves the bare name against the callee's declared enum parameter type
     * exactly as PR #137.
     *
     * <p>Regression-safe by construction: the pre-fix bare {@code ASIC} does not
     * compile, so no currently-green file carries it.
     */
    @Test
    void generate_bareEnumArg_rBodyNameCollision_emitsQualifiedConstantAndImport() throws IOException {
        String mathSource = String.join("\n",
                "namespace com.example.math",
                "",
                "body Authority ASIC <\"The Australian Securities and Investments Commission.\">",
                "",
                "enum RegimeEnum:",
                "    ASIC",
                "    CFTC",
                "",
                "func GetUti:",
                "    inputs:",
                "        id string (1..1)",
                "        regime RegimeEnum (1..1)",
                "    output:",
                "        result string (1..1)",
                "    set result:",
                "        id"
        );
        String reportSource = String.join("\n",
                "namespace com.example.reports",
                "",
                "import com.example.math.*",
                "",
                "type Trade:",
                "    uti string (1..1)",
                "",
                "reporting rule PriorUtiRule from Trade:",
                "    GetUti(uti, ASIC)"
        );
        FixtureResult fx = loadFixtureMulti(List.of(reportSource, mathSource), m -> true);
        try {
            String code = generateFor(fx, "PriorUtiRule");

            // The bare value (false-resolved to body Authority ASIC) must render as
            // the Java enum constant qualified by the callee's declared enum param type.
            assertTrue(code.contains("RegimeEnum.ASIC"),
                    "the bare enum-value argument `ASIC` (name-colliding with `body "
                    + "Authority ASIC`) must render as `RegimeEnum.ASIC` (resolved against "
                    + "GetUti's declared enum parameter type); got:\n" + code);
            // The pre-fix bug: the bare value rendered as a bare-variable.
            assertFalse(code.contains("MapperS.of(ASIC)") || code.contains(", ASIC)"),
                    "the bare enum value must NOT render as the bare Rune name `ASIC` "
                    + "(the non-compiling variable-path form); got:\n" + code);
            // The enum's import must be present (cross-namespace, function-arg only).
            assertTrue(code.contains("import com.example.math.RegimeEnum;"),
                    "the enum type referenced only via the bare colliding function-call "
                    + "argument `ASIC` must be imported; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    /**
     * PR #131 (facet {@code getmulti_multi_arg}). A function-call ARGUMENT that is
     * a navigation ending in a multi-cardinality feature (compiled to a
     * {@code MapperC<T>}) passed where the callee's parameter is also
     * multi-cardinality ({@code (1..*)} / {@code (0..*)}) must be unwrapped with
     * {@code .getMulti()} (→ {@code List<T>}), not {@code .get()} (→ scalar {@code T}).
     * Upstream's {@code ExpressionGenerator} coerces a {@code MapperC} argument to a
     * {@code List}-typed parameter via {@code getMulti()}; the fork's
     * {@code ReferenceHandler.unwrapForEvaluateArg} unconditionally appended
     * {@code .get()}, so a {@code MapperC} argument into a {@code List} parameter was
     * a non-compiling type mismatch (a single {@code T} where {@code List<T>} is
     * expected).
     *
     * <p>Real-corpus anchor (10–11 drr report POJOs):
     * {@code drr/regulation/asic/rewrite/valuation/reports/Counterparty1Rule.java}
     * — {@code reporting rule Counterparty1 ... extract PartyLei(reportingSide ->
     * reportingParty -> partyId)} where {@code func PartyLei: inputs: partyIdentifier
     * PartyIdentifier (1..*)} and the {@code partyId} navigation tail is a
     * {@code (0..*)} {@code mapC}. Golden ends the argument
     * {@code ....mapC("getPartyId", party -> party.getPartyId()).getMulti()}; the fork
     * emitted {@code .get()}.
     *
     * <p>Regression-safe by construction: the only behaviour change is for a
     * {@code MapperC} argument into a multi-cardinality parameter — a case whose
     * pre-fix {@code .get()} form does not compile, so no currently-green (byte-
     * matching) file carries it; only already-waivered files are affected.
     */
    @Test
    void generate_multiCardinalityNavArg_unwrapsWithGetMulti() throws IOException {
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Instr:",
                "    side Side (1..1)",
                "",
                "type Side:",
                "    party Party (1..1)",
                "",
                "type Party:",
                "    partyId PartyId (0..*)",
                "",
                "type PartyId:",
                "    identifier string (1..1)",
                "",
                "func PickLei:",
                "    inputs:",
                "        ids PartyId (1..*)",
                "    output:",
                "        result string (0..1)",
                "    set result:",
                "        ids only-element then identifier",
                "",
                "reporting rule LeiRule from Instr:",
                "    extract PickLei(side -> party -> partyId)"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "LeiRule");

            // THE bug: the multi-cardinality navigation argument (a MapperC, the
            // `partyId (0..*)` mapC tail) passed into PickLei's `(1..*)` parameter
            // must unwrap via `.getMulti()` (→ List), not the scalar `.get()`. The
            // `mapC` is witnessed (`.<PartyId>mapC(...)`), so the assertion anchors on
            // the lambda-body tail rather than the `.mapC(` token.
            assertTrue(
                    code.contains("party -> party.getPartyId()).getMulti()"),
                    "a MapperC navigation argument into a multi-cardinality function "
                    + "parameter must be unwrapped with `.getMulti()`; got:\n" + code);
            assertFalse(
                    code.contains("party -> party.getPartyId()).get()"),
                    "the scalar `.get()` unwrap on a MapperC argument into a multi "
                    + "parameter is a non-compiling type mismatch (single where List "
                    + "expected); got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    /**
     * bare_fn_invocation_arg (PR #132): a bare FUNCTION reference invoked
     * implicitly — rendered by {@code ReferenceHandler.renderImplicitFunctionInvocation}
     * as a BARE scalar {@code innerFn.evaluate(item.get())} with NO
     * {@code MapperS.of} wrap (correct for predicate position) — and then used as
     * an ARGUMENT to another function-call {@code outerFn.evaluate(...)} must be
     * passed RAW. The explicit-args loop's {@code unwrapForEvaluateArg}
     * fall-through blindly appended {@code .get()} to the bare scalar, yielding the
     * non-compiling {@code outerFn.evaluate(innerFn.evaluate(item.get()).get())}
     * ({@code .get()} on a plain value). A bare RULE argument never hit this — the
     * rule path wraps in {@code MapperS.of} and the wrap is stripped structurally —
     * so only the bare FUNCTION argument over-unwrapped. Byte-identity-safe by
     * construction: the pre-fix {@code .get()} form does not compile, so no
     * currently-green (byte-matching) file carries it; only already-waivered files
     * are affected.
     */
    @Test
    void generate_bareFunctionInvocationArg_passedRawWithoutGet() throws IOException {
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Trade:",
                "    tradeId string (1..1)",
                "",
                "func InnerFn:",
                "    inputs:",
                "        trade Trade (1..1)",
                "    output:",
                "        result string (1..1)",
                "    set result:",
                "        trade -> tradeId",
                "",
                "func OuterFn:",
                "    inputs:",
                "        s string (1..1)",
                "    output:",
                "        result string (1..1)",
                "    set result:",
                "        s",
                "",
                "reporting rule FormatRule from Trade:",
                "    extract OuterFn( InnerFn )"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "FormatRule");

            // THE bug: the bare-function-invocation arg `innerFn.evaluate(item.get())`
            // is already the function's scalar output, so it must be passed RAW into
            // outerFn.evaluate(...). The pre-fix fall-through appended a spurious
            // `.get()` on the scalar — a non-compiling type mismatch.
            assertTrue(
                    code.contains("outerFn.evaluate(innerFn.evaluate(item.get()))"),
                    "a bare function-invocation argument must be passed RAW (it is "
                    + "already the callee's scalar output); got:\n" + code);
            assertFalse(
                    code.contains("innerFn.evaluate(item.get()).get()"),
                    "appending `.get()` to a bare scalar function-invocation argument "
                    + "is a non-compiling type mismatch; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 12 (Phase X1 rendering-pivot / Gap #2): implicit-input nav =======

    /**
     * Phase X1 rendering-layer pivot (Gap #2) — a bare {@link RSymbolReference}
     * that resolves (Category 9) to an attribute which is a <em>feature of the
     * rule's implicit input</em> must render as a navigation FROM the implicit
     * {@code input} parameter, not as a bare variable.
     *
     * <p>Byte target (verified against the as-cloned golden
     * {@code DeliveryTypeFromSettlementRule.java:39}):
     * <pre>
     *   MapperS.of(input).&lt;SettlementTypeEnum&gt;map("getSettlementType", trade -&gt; trade.getSettlementType())
     * </pre>
     * Two facts are locked here:
     * <ul>
     *   <li><b>Synthesis:</b> {@code settlementType} renders as a feature call on
     *       the implicit {@code input} ({@code MapperS.of(input).<...>map("getSettlementType", ...)}),
     *       NOT a bare-variable {@code MapperS.of(settlementType)}.</li>
     *   <li><b>Scope-faithful lambda name:</b> the lambda var is {@code trade}
     *       (lowerCamelCase of the receiver type {@code Trade}) with NO leading
     *       underscore — the rule body scope holds only {@code input}, so
     *       {@code trade} does not collide and is not {@code escapeName}'d. (In a
     *       function body whose input param IS named {@code trade}, the same code
     *       path collides and yields {@code _trade} — upstream
     *       {@code ExpressionGenerator.xtend:367} via
     *       {@code GeneratorScope.createUniqueIdentifier} / {@code escapeName}.)</li>
     * </ul>
     */
    @Test
    void generate_implicitInputAttribute_rendersAsInputNavigation() throws IOException {
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "enum SettlementTypeEnum:",
                "    Cash",
                "    Physical",
                "",
                "type Trade:",
                "    settlementType SettlementTypeEnum (1..1)",
                "",
                "reporting rule SettlementTypeRule from Trade:",
                "    settlementType = Cash"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "SettlementTypeRule");

            assertTrue(code.contains(
                    "MapperS.of(input).<SettlementTypeEnum>map(\"getSettlementType\", "
                    + "trade -> trade.getSettlementType())"),
                    "implicit-input attribute `settlementType` must render as a "
                    + "navigation from the implicit `input` with a scope-faithful "
                    + "no-underscore lambda var (`trade`, lowerCamelCase of receiver "
                    + "type `Trade`, no collision with `input`); got:\n" + code);
            assertFalse(code.contains("MapperS.of(settlementType)"),
                    "implicit-input attribute must NOT render as a bare variable "
                    + "`MapperS.of(settlementType)` — that is the Gap #2 defect; "
                    + "got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 13 (Phase X1 rendering-pivot / Gap #1): input type from excluded ns ==

    /**
     * Phase X1 rendering-layer pivot (Gap #1) — the synthetic input attribute's
     * {@code typeCall} is a deep-copy of the rule's {@code from} type that DROPS
     * the resolved {@code referencedTypeId} (per {@code RTypeCall.deepCopy}).
     * {@code generatorModel.getType(input)} therefore falls into the
     * {@code shouldGenerate}-filtered workspace-search-by-name, which returns
     * {@code Object} when the from-type lives in an emission-EXCLUDED namespace —
     * the real-corpus shape (a DRR rule {@code from} a CDM transitive type like
     * {@code SettlementTerms}, whose namespace is not in the cell's emission
     * filter). The byte target {@code DeliveryTypeFromSettlementRule.java:16}
     * is {@code ReportFunction<SettlementTerms, DeliveryTypeEnum>}, NOT
     * {@code ReportFunction<Object, ...>}.
     *
     * <p>The fix resolves the input type from the ORIGIN rule's already-resolved
     * {@code fromType()} (linker-attached; its {@code referencedTypeId} is intact),
     * which bypasses the emission filter entirely. Here the from-type {@code Ext}
     * lives in {@code com.example.types}, which the emission filter excludes —
     * reproducing the transitive-dependency shape in a unit test.
     */
    @Test
    void generate_inputTypeFromExcludedNamespace_resolvesViaOrigin() throws IOException {
        // The from-type `Ext` resolves on the source rule (its `from Ext`
        // reference has an intact referencedTypeId) but its model is excluded
        // from emission — the essential conditions of the real-corpus shape
        // (a DRR rule `from` a CDM transitive type whose namespace is not in
        // the cell's emission filter). The emission filter rejects ALL models
        // here, which is a faithful reduction: the synthetic input attribute's
        // deep-copied typeCall (referencedTypeId dropped) falls into the
        // shouldGenerate-filtered workspace search and misses, while the origin
        // rule's resolved fromType bypasses the filter.
        String source = String.join("\n",
                "namespace com.example.excluded",
                "",
                "type Ext:",
                "    id string (1..1)",
                "",
                "reporting rule ExtIdRule from Ext:",
                "    extract id"
        );
        FixtureResult fx = loadFixtureMulti(List.of(source), m -> false);
        try {
            String code = generateFor(fx, "ExtIdRule");

            assertFalse(code.contains("(Object input)"),
                    "input type from an emission-excluded namespace must NOT "
                    + "collapse to Object — the fix resolves it via the origin "
                    + "rule's resolved fromType; got:\n" + code);
            assertTrue(code.contains("ReportFunction<Ext, String>"),
                    "input type must resolve to the from-type `Ext` (via origin "
                    + "fromType) even though its model is emission-excluded; "
                    + "got:\n" + code);
            assertTrue(code.contains("doEvaluate(Ext input)"),
                    "doEvaluate signature must use the resolved input type Ext, "
                    + "not Object; got:\n" + code);
            // Gap #1 residual: the abstract `evaluate` overrides
            // ReportFunction<I,O>.evaluate, so it must carry @Override (1-tab
            // indent — distinct from the 2-tab doEvaluate override in the
            // nested Default class). Matches the legacy plugin golden.
            assertTrue(code.contains("\t@Override\n\tpublic "),
                    "abstract `evaluate` must carry @Override (it overrides "
                    + "ReportFunction.evaluate); got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 14 (Engine PR #10 / F1): output type from excluded ns ===========

    /**
     * Engine PR #10 (facet F1 — rule-output type inference). The output-side
     * mirror of {@link #generate_inputTypeFromExcludedNamespace_resolvesViaOrigin}.
     *
     * <p>The synthetic {@code RFunction}'s output {@code typeCall} is back-filled
     * by {@link RuleGenerator#generate} from the rule expression's inferred type
     * with the {@code typeName} ONLY (no {@code referencedTypeId}). The base
     * interface {@code ReportFunction<I,O>} is correct because the generator feeds
     * it {@code inferredOutputType} directly — but the method SIGNATURES
     * ({@code evaluate} / {@code doEvaluate} / {@code assignOutput} return + the
     * {@code output} local) flow through {@code FunctionGenerator.buildStandardModel}
     * → {@code resolveParam(output)} → {@code generatorModel.getType(output)},
     * which resolves the name-only {@code typeCall} via the {@code shouldGenerate}-
     * filtered workspace search. When the output type lives in an emission-EXCLUDED
     * namespace that search misses → {@code RMissingType} → {@code Object}. This is
     * the real-corpus shape: 26 drr currency-leg report rules whose output enum
     * {@code ISOCurrencyCodeEnum} lives in the transitive-CDM
     * {@code cdm.base.staticdata.asset.common} namespace render
     * {@code public Object evaluate(...)} instead of
     * {@code public ISOCurrencyCodeEnum evaluate(...)}.
     *
     * <p>The fix mints a {@code referencedTypeId} from the inferred type's
     * declaration node and attaches it to the back-filled output {@code typeCall}
     * (the exact mirror of the input-side {@code fromType} re-attach), so
     * {@code resolveTypeCall} resolves it via {@code workspace.resolveTypeLike}
     * BEFORE the filtered name-search — bypassing the emission filter. Here the
     * enum {@code Color} lives in {@code com.example.excluded}, which the emission
     * filter rejects, reproducing the transitive-dependency shape.
     */
    @Test
    void generate_outputTypeFromExcludedNamespace_resolvesViaInferredType() throws IOException {
        String source = String.join("\n",
                "namespace com.example.excluded",
                "",
                "enum Color:",
                "    RED",
                "    GREEN",
                "",
                "type Ext:",
                "    color Color (1..1)",
                "",
                "reporting rule ExtColorRule from Ext:",
                "    extract color"
        );
        FixtureResult fx = loadFixtureMulti(List.of(source), m -> false);
        try {
            String code = generateFor(fx, "ExtColorRule");

            // The bug signatures: output type collapsed to Object in the methods.
            assertFalse(code.contains("public Object evaluate("),
                    "output type from an emission-excluded namespace must NOT "
                    + "collapse the `evaluate` return to Object — the fix attaches "
                    + "a referencedTypeId minted from the inferred type; got:\n" + code);
            assertFalse(code.contains("Object output"),
                    "the `output` local must use the resolved output type, not "
                    + "Object; got:\n" + code);
            // The fixed signatures: concrete output type Color throughout.
            assertTrue(code.contains("public Color evaluate(Ext input)"),
                    "abstract `evaluate` must return the resolved output type Color "
                    + "(via inferred-type referencedTypeId) even though its model is "
                    + "emission-excluded; got:\n" + code);
            assertTrue(code.contains("protected abstract Color doEvaluate(Ext input)"),
                    "`doEvaluate` signature must use the resolved output type Color, "
                    + "not Object; got:\n" + code);
            // The base interface was already correct (it uses inferredOutputType
            // directly) — assert it stays correct as a guard.
            assertTrue(code.contains("ReportFunction<Ext, Color>"),
                    "base interface must carry the concrete output type Color; got:\n"
                    + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 15 (Engine PR #12 / F5): chained-nav lambda var = receiver type ==

    /**
     * Engine PR #12 (facet F5 — post-unwrap lambda naming). A chained navigation
     * step's lambda variable must be lowerCamelCase of the RECEIVER's RETURN
     * TYPE (upstream {@code ExpressionGenerator.xtend:368} —
     * {@code javaType.rosettaName.toFirstLower}), NOT the receiver step's FEATURE
     * name. The two coincide whenever an attribute is named like its type (the
     * CDM convention: {@code collateralDetails : CollateralDetails}), which masked
     * the defect — it surfaces only when the attribute name differs from the type
     * name (e.g. {@code uniqueTradeIdentifier : TradeIdentifier} in the real ASIC
     * UTI rule; facet-dump {@code all-diffs.txt} line 280/286 — golden
     * {@code tradeIdentifier -> tradeIdentifier.getAssignedIdentifier()} vs fork
     * {@code uniqueTradeIdentifier -> uniqueTradeIdentifier.getAssignedIdentifier()}).
     *
     * <p>Root cause (localised to the {@code REnumValueRef} branch — NOT
     * {@code RFeatureCall}): inside a synthetic-{@code item} lambda the {@code a -> b}
     * chain segments parse as disguised {@code REnumValueRef}s (the grammar reads
     * {@code EnumType -> VALUE}; they don't resolve to a real enum, so
     * {@code enumeration()} is empty). When the next nav step computes its lambda
     * var via {@code resolveLambdaVarName}, its receiver IS that disguised
     * {@code REnumValueRef}; the branch called the weaker
     * {@code resolveDisguisedFeature} (which resolves names against the enclosing
     * FUNCTION's inputs/output only — and the chain head is a feature of the rule's
     * implicit INPUT, not a function input), so it returned {@code null} and fell
     * through to {@code evr.valueName()} (the raw trailing source name). The same
     * step's {@code <Type>} witness already renders correctly because the rendering
     * path ({@code ReferenceHandler.synthesizeImplicitInputChain}) resolves the
     * disguised chain via its {@code resolvedAttributeChain} (D39 Category 10) — the
     * lambda-var path simply ignored it. The fix derives the lambda var from the
     * chain's terminal {@code chain.feature().typeCall().typeName()}, the exact
     * attribute the witness uses. (The {@code RFeatureCall} branch's
     * {@code fc.featureName()} fallback is a latent same-class gap, but it does not
     * fire here: input-rooted chains wire {@code resolvedFeature}, and synthetic-item
     * chains route through {@code REnumValueRef} — completeness-ledger Tier-3 #23.)
     *
     * <p>Fixture: an {@code if/then/else} inside {@code extract} (the real-corpus
     * trigger — cf. the ASIC {@code ASICUniqueTransactionIdentifier} rule whose
     * {@code else collateralDetails -> uniqueTradeIdentifier -> assignedIdentifier
     * -> identifier} chain renders inside a {@code mapSingleToItem(item -> ...)}
     * lambda; facet-dump line 280/286). The conditional forces the synthetic-
     * {@code item} lambda; the chain head navigates from {@code item} correctly
     * (PR #1/#4 facet C), but the mid-chain {@code RFeatureCall}s have UNWIRED
     * {@code resolvedFeature} (the elided-operand {@code RImplicitVariable}
     * synthesis runs AFTER type-directed resolution). The {@code name} step's
     * receiver is {@code getLeafNode} (returns {@code Leaf}, attr name
     * {@code leafNode} != lowerCamelCase(type) {@code leaf}); its lambda var must
     * be {@code leaf}, not {@code leafNode}. (A directly input-rooted chain like
     * a plain {@code extract mid -> leafNode -> name} DOES wire
     * {@code resolvedFeature} and renders {@code leaf} already — the defect is
     * specific to the synthetic-item lambda context, which is why F5 earns a
     * dedicated regression-validated PR.)
     */
    @Test
    void generate_chainedNavLambdaVar_usesReceiverTypeNotFeatureName() throws IOException {
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Leaf:",
                "    name string (1..1)",
                "",
                "type Mid:",
                "    leafNode Leaf (1..1)",
                "",
                "type Container:",
                "    mid Mid (1..1)",
                "",
                "reporting rule LeafNameRule from Container:",
                "    extract if mid exists then mid -> leafNode -> name else mid -> leafNode -> name"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "LeafNameRule");

            assertTrue(code.contains("leaf -> leaf.getName()"),
                    "the chained nav step's lambda var must be lowerCamelCase of "
                    + "the receiver's RETURN type (`Leaf` -> `leaf`), per upstream "
                    + "ExpressionGenerator.xtend:368; got:\n" + code);
            assertFalse(code.contains("leafNode -> leafNode.getName()"),
                    "the lambda var must NOT be the receiver step's FEATURE name "
                    + "(`leafNode`) — that is the F5 fallback defect when the "
                    + "mid-chain resolvedFeature is unwired in the synthetic-item "
                    + "lambda; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 16 (Engine PR #13 / F6): bare-function-then 2-statement form =====

    /**
     * Engine PR #13 (facet F6 — then-local form, bare-FUNCTION-then sub-shape).
     * A {@code then <Function>} body (a bare, no-args reference resolving to an
     * {@link RFunction}) in a RULE body must render as the upstream 2-statement
     * {@code thenArg} form, identical in shape to the bare-RULE-then form engine
     * PR #6 already emits via {@code renderBareInvokableThenSet}:
     * <pre>
     * final MapperS&lt;BigDecimal&gt; thenArg = MapperS.of(input).&lt;BigDecimal&gt;map("getVal", ext -&gt; ext.getVal());
     * output = MapperS.of(fmt.evaluate(thenArg.get())).get();
     * </pre>
     *
     * <p>Pre-fix the bare-function-then fell through {@code isBareRuleThen}
     * (which only accepts {@code RRule}) to the generic
     * {@code CollectionHandler.handle(RThenExpr)} path, emitting the broken inline
     * {@code arg.then(item -> fmt.evaluate(item.get())).get()} — there is no
     * runtime {@code Mapper.then(Function)} method, so it never matches a golden.
     * The receiver {@code fmt} is the function's injected lowerCamel field name
     * (single source of truth {@code FunctionDependencyCollector.lowerCamelCase}),
     * matching the {@code @Inject} field the dependency collector already registers.
     * Real-corpus anchor: {@code drr/standards/iosco/cde/version1/price/reports/
     * InterestRateFixedRateRule.java} (body {@code … then FormatToBaseOneRate}).
     */
    @Test
    void generate_bareFunctionThen_emitsThenArgTwoStatementForm() throws IOException {
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "func Fmt:",
                "    inputs:",
                "        x number (1..1)",
                "    output:",
                "        result number (1..1)",
                "    set result:",
                "        x",
                "",
                "type Ext:",
                "    val number (1..1)",
                "",
                "reporting rule ExtFmtRule from Ext:",
                "    val then Fmt"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "ExtFmtRule");

            assertFalse(code.contains(".then("),
                    "bare-function-then must NOT emit the broken inline "
                    + "`arg.then(item -> ...)` form (there is no runtime "
                    + "Mapper.then(Function) method); got:\n" + code);
            assertTrue(code.contains("final MapperS<BigDecimal> thenArg = "),
                    "bare-function-then must declare the `final MapperS<…> thenArg = …;` "
                    + "local (upstream caseThenOperation declareAsVariable); got:\n" + code);
            assertTrue(code.contains("MapperS.of(fmt.evaluate(thenArg.get())).get()"),
                    "the SET RHS must invoke the injected function instance on "
                    + "thenArg.get(), receiver = lowerCamelCase(function simple name) "
                    + "= `fmt`; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    /**
     * Engine PR #14 (facet F6 — coercion variant): a bare-function-then whose
     * target function's single input type DIFFERS from the piped element type
     * coerces the evaluate argument. Here {@code mult} is {@code int} (→ Integer)
     * and {@code FmtNum} takes {@code number} (→ BigDecimal), so the golden hoists
     * the unwrapped value (`final Integer integer = thenArg.get();`) and passes the
     * null-guarded conversion {@code (integer == null ? null : BigDecimal.valueOf(integer))}
     * to {@code evaluate(...)} — mirroring upstream
     * {@code TypeCoercionService.convertNullSafe}. The conversion expression is
     * produced by the (PR-A) coercion service, not hardcoded. The no-coercion
     * sibling ({@link #generate_bareFunctionThen_emitsThenArgTwoStatementForm})
     * stays the bare {@code evaluate(thenArg.get())} form — verified there.
     * Real-corpus anchor: {@code drr/standards/iosco/cde/version1/payment/reports/
     * PaymentFrequencyPeriodMultiplierRule.java}.
     */
    @Test
    void generate_bareFunctionThen_coercesArgToFunctionInputType() throws IOException {
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "func FmtNum:",
                "    inputs:",
                "        x number (1..1)",
                "    output:",
                "        result number (1..1)",
                "    set result:",
                "        x",
                "",
                "type Ext:",
                "    mult int (1..1)",
                "",
                "reporting rule ExtFmtNumRule from Ext:",
                "    mult then FmtNum"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "ExtFmtNumRule");

            assertFalse(code.contains(".then("),
                    "bare-function-then must NOT emit the broken inline `.then(...)` form; got:\n" + code);
            assertTrue(code.contains("final MapperS<Integer> thenArg = "),
                    "must declare the `final MapperS<Integer> thenArg = …;` local; got:\n" + code);
            assertTrue(code.contains("final Integer integer = thenArg.get();"),
                    "the coercion variant must hoist `final Integer integer = thenArg.get();`; got:\n" + code);
            assertTrue(code.contains(
                    "fmtNum.evaluate((integer == null ? null : BigDecimal.valueOf(integer)))"),
                    "the evaluate arg must be the null-guarded BigDecimal.valueOf coercion of the "
                    + "hoisted local; got:\n" + code);
            assertFalse(code.contains("evaluate(thenArg.get())"),
                    "the coercion case must NOT pass the bare `thenArg.get()`; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    /**
     * Engine PR #14 (facet F6 — coercion variant, cardinality guard): the
     * coercion fires only for a SINGLE-cardinality function input. A MULTI input
     * (`xs number (0..*)`) generates an {@code evaluate(List<BigDecimal> xs)}
     * signature (FunctionGenerator gates on {@code generatorModel.isMulti(input)}),
     * so a scalar {@code (integer == null ? null : BigDecimal.valueOf(integer))}
     * argument would be wrong-typed. The renderer must NOT coerce here — it keeps
     * the bare {@code thenArg.get()} form (unchanged, already-waivered). Guards
     * the codex-review cardinality finding.
     */
    @Test
    void generate_bareFunctionThen_multiCardinalityInput_skipsCoercion() throws IOException {
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "func FmtList:",
                "    inputs:",
                "        xs number (0..*)",
                "    output:",
                "        result number (0..*)",
                "    set result:",
                "        xs",
                "",
                "type Ext:",
                "    mults int (0..*)",
                "",
                "reporting rule ExtFmtListRule from Ext:",
                "    mults then FmtList"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "ExtFmtListRule");

            // The MULTI-input gate must suppress the scalar coercion: no hoisted
            // `final Integer integer = thenArg.get();` local.
            assertFalse(code.contains("final Integer integer = thenArg.get();"),
                    "a MULTI-cardinality function input must NOT trigger the scalar "
                    + "coercion hoist; got:\n" + code);
            assertFalse(code.contains("BigDecimal.valueOf(integer)"),
                    "a MULTI-cardinality function input must NOT emit the scalar "
                    + "coercion; got:\n" + code);
            // It keeps the bare unwrapped form (unchanged engine PR #13 behaviour).
            assertTrue(code.contains("evaluate(thenArg.get())"),
                    "the MULTI-input case keeps the bare `evaluate(thenArg.get())` form; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    /**
     * PR #128 (facet {@code bigint_bigdecimal_coerce}). A function-call ARGUMENT
     * inside an {@code extract}/{@code map} lambda whose actual type is
     * {@code BigInteger} (a {@code number} literal exceeding {@code long}, e.g.
     * {@code 9999999999999999999999999} → {@code new BigInteger("…")}) while the
     * callee's parameter expects {@code number} (→ {@code BigDecimal}) must hoist
     * the unwrapped value into a {@code final BigInteger bigInteger = …;} local and
     * pass the null-guarded conversion
     * {@code (bigInteger == null ? null : new BigDecimal(bigInteger))} to
     * {@code evaluate(...)} — lifting the extract lambda to BLOCK form. This reuses
     * the PR #109 {@code ReferenceHandler.tryMetaDerefArg} hoist machinery (the gate
     * is relaxed from meta-wrapper-only to also accept BigInteger→BigDecimal); the
     * conversion expression is produced by the (PR-A) coercion service
     * ({@code ItemToItemCoercer} {@code new BigDecimal(x)}), not hardcoded.
     *
     * <p>Pre-fix the argument stays the flat inline
     * {@code evaluate(item.get(), new BigInteger("…"))} (a non-compiling
     * BigInteger-where-BigDecimal-expected type mismatch). Real-corpus anchor:
     * {@code drr/regulation/esma/emir/refit/trade/reports/NotionalAmountOfLeg1Rule.java}
     * (and the fca/hkma siblings) — {@code then extract
     * cde.quantity.NotionalAmountLeg1(item, 9999999999999999999999999)}.
     */
    @Test
    void generate_bigIntegerArg_coercesToBigDecimalWithNullGuardedHoist() throws IOException {
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Wrapper:",
                "    amount number (1..1)",
                "",
                "func ComputeCapped:",
                "    inputs:",
                "        wrapper Wrapper (1..1)",
                "        cap number (1..1)",
                "    output:",
                "        result number (0..1)",
                "    set result:",
                "        cap",
                "",
                "reporting rule BigIntCoerceRule from Wrapper:",
                "    extract ComputeCapped(item, 9999999999999999999999999)"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "BigIntCoerceRule");

            assertTrue(code.contains(
                    "final BigInteger bigInteger = new BigInteger(\"9999999999999999999999999\");"),
                    "the BigInteger literal must be hoisted into a `final BigInteger "
                    + "bigInteger = new BigInteger(\"…\");` local (block-lambda form); got:\n" + code);
            assertTrue(code.contains(
                    "computeCapped.evaluate(item.get(), (bigInteger == null ? null : new BigDecimal(bigInteger)))"),
                    "the evaluate arg must be the null-guarded `new BigDecimal(bigInteger)` "
                    + "coercion of the hoisted BigInteger local; got:\n" + code);
            assertFalse(code.contains("evaluate(item.get(), new BigInteger("),
                    "the coercion case must NOT pass the raw inline `new BigInteger(\"…\")` "
                    + "as the evaluate argument; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    /**
     * PR #129 (facet {@code bigint_bigdecimal_coerce} — TOP-LEVEL then-extract
     * variant, the PR #128 follow-up). The PR #128 hoist fires only for a
     * function-call arg inside an {@code extract}/{@code map} lambda
     * ({@code ReferenceHandler.metaDerefHoistRoute} requires an enclosing
     * {@link com.regnosys.rosetta.ast.expressions.unary.RExtractExpr}). The
     * corpus also has the coercion in a TOP-LEVEL then-extract output assignment —
     * {@code filter <Pred> then <Func>(item, 9999999999999999999999999)} — where the
     * function call is the body of a {@code then} (parent
     * {@link com.regnosys.rosetta.ast.expressions.binary.RThenExpr}), NOT inside an
     * extract/map lambda. There the call is compiled at STATEMENT level in
     * {@code FunctionExpressionRenderer.renderThenExtractSet}, so the gate did not
     * fire and the BigInteger arg stayed the flat inline
     * {@code evaluate(thenArg.get(), new BigInteger("…"))} (a non-compiling
     * BigInteger-where-BigDecimal-expected type mismatch).
     *
     * <p>The fix extends {@code metaDerefHoistRoute} to also fire for a then-body
     * function call in a reporting RULE, and {@code renderThenExtractSet} LIFTS the
     * resulting {@link com.regnosys.rosetta.generator.java.statement.builder.JavaBlockBuilder}'s
     * hoisted {@code final BigInteger bigInteger = …;} statement to the line before
     * the {@code output =} assignment, keeping the trailing
     * {@code MapperS.of(…).get()} as the RHS. Real-corpus anchor:
     * {@code drr/regulation/asic/rewrite/trade/reports/NotionalAmountLeg1Rule.java}
     * (and the hkma/mas/jfsa + Total* siblings) —
     * {@code filter IsAllowableActionForASIC then cde.quantity.NotionalAmountLeg1(item, 9999999999999999999999999)}.
     */
    @Test
    void generate_bigIntegerArgTopLevelThen_coercesToBigDecimalWithHoistLift() throws IOException {
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Wrapper:",
                "    amount number (1..1)",
                "",
                "func IsAllowable:",
                "    inputs:",
                "        wrapper Wrapper (1..1)",
                "    output:",
                "        result boolean (1..1)",
                "    set result:",
                "        True",
                "",
                "func ComputeCapped:",
                "    inputs:",
                "        wrapper Wrapper (1..1)",
                "        cap number (1..1)",
                "    output:",
                "        result number (0..1)",
                "    set result:",
                "        cap",
                "",
                "reporting rule BigIntThenRule from Wrapper:",
                "    filter IsAllowable",
                "    then ComputeCapped(item, 9999999999999999999999999)"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "BigIntThenRule");

            assertTrue(code.contains(
                    "final BigInteger bigInteger = new BigInteger(\"9999999999999999999999999\");"),
                    "the BigInteger literal must be hoisted to STATEMENT level (a `final "
                    + "BigInteger bigInteger = new BigInteger(\"…\");` line before the output "
                    + "assignment); got:\n" + code);
            assertTrue(code.contains(
                    "computeCapped.evaluate(thenArg.get(), (bigInteger == null ? null : new BigDecimal(bigInteger)))"),
                    "the then-extract evaluate arg must be the null-guarded `new "
                    + "BigDecimal(bigInteger)` coercion of the hoisted BigInteger local; got:\n" + code);
            assertFalse(code.contains("evaluate(thenArg.get(), new BigInteger("),
                    "the top-level then-extract coercion case must NOT pass the raw inline "
                    + "`new BigInteger(\"…\")` as the evaluate argument; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 18 (PR #98 / F6): then-extract / navigating-then core =====

    /**
     * PR #98 (facet F6 — then-local form, <b>then-extract / navigating-then</b>
     * structural core). A {@code then} whose body is an extract / navigation chain
     * (not a bare invocation) must render the upstream {@code caseThenOperation}
     * 2-statement form: declare {@code final MapperS<…> thenArg = <arg>;}, then
     * compute the body with its implicit input <b>re-rooted on {@code thenArg}</b>
     * (upstream {@code createKeySynonym}) — e.g.
     * <pre>
     * final MapperS&lt;Leaf&gt; thenArg = MapperS.of(input).&lt;Leaf&gt;map("getLeaf", mid -&gt; mid.getLeaf());
     * output = thenArg.&lt;String&gt;map("getName", leaf -&gt; leaf.getName());
     * </pre>
     *
     * <p>Pre-fix the extract-bodied then falls through to the generic
     * {@code CollectionHandler.handle(RThenExpr)} path, emitting the broken inline
     * {@code arg.then(item -> item…)} — there is no runtime
     * {@code Mapper.then(Function)} method, so it never byte-matches a golden.
     * Real-corpus shape: {@code … then extract common.party.BrokerID}.
     */
    @Test
    void generate_thenExtract_emitsThenArgReRootedBody() throws IOException {
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Leaf:",
                "    name string (1..1)",
                "",
                "type Mid:",
                "    leaf Leaf (1..1)",
                "",
                "reporting rule ExtractThenRule from Mid:",
                "    leaf then extract name"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "ExtractThenRule");

            assertFalse(code.contains(".then("),
                    "then-extract must NOT emit the broken inline `arg.then(item -> ...)` "
                    + "form (no runtime Mapper.then(Function) method); got:\n" + code);
            assertTrue(code.contains("final MapperS<Leaf> thenArg = "),
                    "then-extract must declare the `final MapperS<Leaf> thenArg = …;` local "
                    + "(upstream caseThenOperation declareAsVariable); got:\n" + code);
            assertTrue(code.contains("output = thenArg"),
                    "the body must be re-rooted on `thenArg` (the implicit input bound to "
                    + "thenArg via createKeySynonym), not a fresh MapperS.of(input); got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    @Test
    void generate_chainedThen_emitsSequentialThenArgs() throws IOException {
        // A chained `a then b then c` (2 thens) — the OUTER then routes to the
        // then-extract renderer, but pre-fix the inner then compiled through the
        // generic CollectionHandler.handle(RThenExpr) path and emitted the broken
        // inline `.then(item -> ...)`. Golden (upstream caseThenOperation, recursive)
        // declares one thenArg per then, 0-based-suffixed because >1 share the desired
        // name "thenArg" (GeneratorScope.computeActualNames). Anchor: the real corpus
        // DTCC_SEFOrDCMIndicatorRule.java (final MapperS<X> thenArg0 = …; thenArg1 =
        // thenArg0…; output = thenArg1…).
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Leaf:",
                "    name string (1..1)",
                "",
                "type Mid:",
                "    leaf Leaf (1..1)",
                "",
                "type Top:",
                "    mid Mid (1..1)",
                "",
                "reporting rule ChainedThenRule from Top:",
                "    mid then extract leaf then extract name"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "ChainedThenRule");

            assertFalse(code.contains(".then("),
                    "chained then must NOT emit the broken inline `.then(item -> ...)` form "
                    + "for the inner then (no runtime Mapper.then(Function) method); got:\n" + code);
            // N=2 thens → 0-based thenArg names: computeActualNames suffixes ALL members
            // of a collision group, so two `thenArg`s become thenArg0/thenArg1 (one then
            // alone stays bare `thenArg` per PR #98).
            assertTrue(code.contains("final MapperS<Mid> thenArg0 = "),
                    "first then-arg (the base argument `mid`) must declare `thenArg0`; got:\n" + code);
            assertTrue(code.contains("final MapperS<Leaf> thenArg1 = thenArg0"),
                    "second then-arg must declare `thenArg1` re-rooted on `thenArg0`; got:\n" + code);
            assertTrue(code.contains("output = thenArg1"),
                    "the final body must be re-rooted on `thenArg1`, not a fresh MapperS.of(input); "
                    + "got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    @Test
    void generate_chainWithBareFunctionTail_routesToChainedRendererWithWrap() throws IOException {
        // PR #276 (facet chainedBareFunctionThen): a chain ending in a bare FUNCTION
        // (`… then DoubleIt`) on the rule path NOW routes to the chained then-extract renderer — the
        // #254-deferred sibling of generate_chainWithBareRuleTail_routesToChainedRenderer below (PR
        // #254 admitted only a bare-RULE tail; this completes the "dual wrap-factory check"). A
        // bare-RULE body compiles to a ready `MapperS.of(rule.evaluate(…))` wrap-factory
        // (renderImplicitRuleInvocation always wraps), but a bare-FUNCTION body compiles BARE
        // (renderImplicitFunctionInvocation emits `doubleIt.evaluate(…)` with no wrap), so
        // renderThenExtractSetImpl WRAPS it MapperS.of(…) at the base/intermediate decl + the
        // terminal — and isInvocationWrapFactory then recognises the wrap and appends `.get()`,
        // emitting `output = MapperS.of(doubleIt.evaluate(thenArg1.get())).get();` (replacing the
        // non-compiling inline `.then(` — no runtime Mapper.then(Function) method). isHoistableThenChain
        // now admits a bare-FUNCTION body on the rule path (CollectionHandler.isBareInvokableBody on
        // the rule path). Mirrors the real-corpus carriers (FixedRateOfLeg1OrCoupon etc.) locked by
        // RuleChainedBareFunctionThenTest.
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Leaf:",
                "    code int (1..1)",
                "",
                "type Mid:",
                "    leaf Leaf (1..1)",
                "",
                "func DoubleIt:",
                "    inputs: n int (1..1)",
                "    output: result int (1..1)",
                "    set result: n",
                "",
                "reporting rule BareTailChainRule from Mid:",
                "    leaf then extract code then DoubleIt"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "BareTailChainRule");
            assertFalse(code.contains(".then("),
                    "no inline runtime `.then(` form (no Mapper.then(Function) method); got:\n" + code);
            assertTrue(code.contains("thenArg0") && code.contains("thenArg1"),
                    "a chain whose tail is a bare FUNCTION NOW routes to the chained "
                    + "thenArg0/thenArg1 renderer (PR #276); got:\n" + code);
            assertTrue(code.contains("MapperS.of(doubleIt.evaluate(thenArg1.get())).get()"),
                    "the bare-FUNCTION tail is WRAPPED MapperS.of(...) + `.get()` (the #276 dual "
                    + "wrap-factory check), re-rooted on the preceding thenArg1; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    @Test
    void generate_chainWithBareRuleTail_routesToChainedRenderer() throws IOException {
        // PR #254 (facet chained_bare_invokable_then): a chain ending in a bare RULE
        // (`… then <rule>`) on the rule path NOW routes to the chained then-extract renderer.
        // The bare-rule then-body compiles (via ReferenceHandler) to a
        // `MapperS.of(leafCodeRule.evaluate(thenArg1.get()))` wrap-factory; isHoistableThenChain
        // admits it (isInvocationWrapFactory's RRule arm keeps the wrap + appends `.get()`), so
        // the inner thens hoist to thenArg0/thenArg1 and the bare-rule tail renders the upstream
        // `output = MapperS.of(<receiver>.evaluate(thenArg1.get())).get();` form — replacing the
        // non-compiling inline `.then(` (no runtime Mapper.then(Function) method). Mirrors the
        // real-corpus carriers (CallCurrencyRule etc.) locked by
        // FunctionChainedBareInvokableThenTest.
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Leaf:",
                "    code int (1..1)",
                "",
                "type Mid:",
                "    leaf Leaf (1..1)",
                "",
                "type Top:",
                "    mid Mid (1..1)",
                "",
                "reporting rule LeafCode from Leaf:",
                "    code",
                "",
                "reporting rule ChainedBareRuleTail from Top:",
                "    mid then extract leaf then LeafCode"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "ChainedBareRuleTail");
            assertFalse(code.contains(".then("),
                    "the inner then must hoist, NOT emit the broken inline `.then(item -> ...)` "
                    + "form (no runtime Mapper.then(Function) method); got:\n" + code);
            assertTrue(code.contains("thenArg0"),
                    "a chained bare-RULE-tail chain MUST route to the chained thenArg0/thenArg1 "
                    + "renderer; got:\n" + code);
            assertTrue(code.contains(
                    "output = MapperS.of(leafCodeRule.evaluate(thenArg1.get())).get();"),
                    "the bare-rule tail must render the upstream wrap-factory output form "
                    + "`MapperS.of(<receiver>.evaluate(thenArg1.get())).get()`, not a stripped bare "
                    + "`output = leafCodeRule.evaluate(...)` (the isInvocationWrapFactory RRule arm); "
                    + "got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 22 (facet F6 residual): bare-rule reference in operand position ===

    /**
     * Facet F6 residual co-blocker — a bare no-arg reference to a RULE used as an
     * <b>operand</b> (here: a function-call argument inside an {@code extract}
     * item-lambda) must render as the injected rule instance invoked on the implicit
     * input — {@code innerRule.evaluate(item.get())} — NOT the bare qualified
     * reference name (non-compiling, e.g. {@code cde.price.StrikePriceNoFormat}).
     *
     * <p>Symmetric to the bare-FUNCTION operand branch
     * ({@code ReferenceHandler.renderImplicitFunctionInvocation}, engine PR #6): a
     * bare-rule body was only handled in the {@code then}-BODY position
     * ({@code FunctionExpressionRenderer.renderBareInvokableThenSet}); operand
     * position fell through to the variable path → {@code MapperS.of(<qualifiedName>)}.
     * The {@code @Inject} field is already registered by
     * {@code FunctionDependencyCollector}'s bare-RRule branch (SAME receiver
     * derivation = SOT), so only the rendering side was missing. Real-corpus anchor:
     * {@code drr/.../asic/.../trade/reports/StrikePriceRule.java}
     * ({@code PriceFormatFromNotation(StrikePriceNoFormat, StrikePriceNotation)} —
     * the bare sub-rules were the file's SOLE residual diff vs golden).
     */
    @Test
    void generate_bareRuleAsFunctionArg_emitsInjectedRuleInvocation() throws IOException {
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Trade:",
                "    id string (1..1)",
                "",
                "func Wrap:",
                "    inputs:",
                "        x string (1..1)",
                "    output:",
                "        result string (1..1)",
                "    set result:",
                "        x",
                "",
                "reporting rule Inner from Trade:",
                "    extract id",
                "",
                "reporting rule Outer from Trade:",
                "    extract Wrap(Inner)"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "Outer");

            assertTrue(code.contains("innerRule.evaluate(item.get())"),
                    "bare rule `Inner` as a function arg must render as the injected "
                    + "`innerRule` instance invoked on the lambda item "
                    + "(`innerRule.evaluate(item.get())`); got:\n" + code);
            assertTrue(code.contains("wrap.evaluate(innerRule.evaluate(item.get()))"),
                    "the bare-rule invocation must nest inside the wrapping function "
                    + "call as its unwrapped evaluate-arg; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    /**
     * Facet F6 residual (this PR) — a bare no-arg reference to a RULE as the entire
     * rule-body expression (a SET output at rule-body TOP LEVEL, no enclosing
     * extract/filter/then lambda) renders as the injected rule instance invoked on
     * the rule's {@code input} parameter — {@code output = innerRule.evaluate(input);}
     * — NOT the non-compiling bare qualified reference name (e.g.
     * {@code output = com.example.reports.Inner;}). This is the deferred top-level
     * sub-facet of PR #100: there the operand argument is the lambda binding
     * ({@code item.get()} / {@code thenArg.get()}), but at rule-body top level the
     * implicit input is the {@code evaluate}/{@code assignOutput} parameter literally
     * named {@code input} (see {@code RFunction.fromRule}). The byte-oracle is uniform
     * across the DRR corpus residual: 202 waivered {@code reports/*Rule.java} whose
     * SOLE diff vs golden was {@code output = RULE.evaluate(input);}. Real-corpus
     * anchor: {@code drr/.../asic/.../trade/reports/AssetClassRule.java}
     * ({@code output = assetClassRule.evaluate(input);}).
     */
    @Test
    void generate_bareRuleTopLevelSetOutput_invokesOnInput() throws IOException {
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Trade:",
                "    id string (1..1)",
                "",
                "reporting rule Inner from Trade:",
                "    extract id",
                "",
                "reporting rule Outer from Trade:",
                "    Inner"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "Outer");

            assertTrue(code.contains("output = innerRule.evaluate(input);"),
                    "a top-level bare-rule SET output must invoke the injected "
                    + "`innerRule` instance on the rule `input` parameter "
                    + "(`output = innerRule.evaluate(input);`); got:\n" + code);
            assertFalse(code.contains("item.get()"),
                    "the top-level form must use the `input` parameter, NOT the "
                    + "in-lambda `item.get()` binding (`item` is undefined outside a "
                    + "lambda); got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    /**
     * Facet F6 residual — top-level bare-rule operand in a CONDITIONAL branch (no
     * enclosing extract/filter/then lambda). Each branch reference invokes the
     * injected instance on the rule {@code input} parameter — never the in-lambda
     * {@code item.get()} binding ({@code item} is undefined outside a lambda). Sister
     * shape to {@link #generate_bareRuleTopLevelSetOutput_invokesOnInput}: confirms
     * the top-level {@code input} form applies to bare-rule operands reached through a
     * conditional, not only the whole-body SET case.
     */
    @Test
    void generate_bareRuleTopLevelConditionalBranch_invokesOnInput() throws IOException {
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Trade:",
                "    id string (1..1)",
                "    flag boolean (1..1)",
                "",
                "reporting rule Inner from Trade:",
                "    extract id",
                "",
                "reporting rule TopCond from Trade:",
                "    if flag then Inner else Inner"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "TopCond");

            assertTrue(code.contains("innerRule.evaluate(input)"),
                    "a top-level bare-rule operand in a conditional branch must invoke "
                    + "the injected `innerRule` instance on the rule `input` parameter "
                    + "(`innerRule.evaluate(input)`); got:\n" + code);
            assertFalse(code.contains("item.get()"),
                    "a top-level bare-rule operand must NOT emit the in-lambda "
                    + "`innerRule.evaluate(item.get())` form — `item` is undefined "
                    + "outside a lambda; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === PR #102 (facet F6 residual): bare RULE reference as a navigation receiver ===

    /**
     * Shared fixture: a bare reporting-rule reference ({@code GetBox}) used as a
     * navigation receiver inside a {@code then} body ({@code then GetBox -> convention}).
     * The rule returns {@code Box}; the navigated feature {@code convention} is an
     * enum. Crucially the rule NAME ({@code GetBox} → {@code getBox}) differs from
     * its OUTPUT TYPE ({@code Box} → {@code box}), so the lambda-var assertion
     * distinguishes output-type derivation from name derivation.
     */
    private static String bareRuleNavReceiverSource() {
        return String.join("\n",
                "namespace com.example.test",
                "",
                "type Trade:",
                "    leg Leg (1..1)",
                "",
                "type Leg:",
                "    box Box (1..1)",
                "",
                "type Box:",
                "    convention DayCountEnum (1..1)",
                "",
                "enum DayCountEnum:",
                "    ACT360",
                "",
                "reporting rule GetBox from Leg:",
                "    extract box",
                "",
                "reporting rule Outer from Trade:",
                "    extract leg",
                "    then GetBox -> convention"
        );
    }

    /**
     * Rendering: {@code then SomeRule -> feature} parses as an {@link REnumValueRef}
     * (grammar's {@code EnumName -> valueName} shape) whose {@code resolvedSymbol} is
     * an {@link RRule}. It must render as the injected rule instance invoked on the
     * then-arg, then navigated —
     * {@code MapperS.of(<ruleField>.evaluate(thenArg.get())).<T>map("getFeature", v -> v.getFeature())}
     * — NOT the non-compiling bare type-name literal {@code MapperS.of(GetBox)}.
     *
     * <p>The lambda var derives from the rule OUTPUT type (the navigated feature's
     * owner — {@code box} for output type {@code Box}), NOT the rule name
     * ({@code getBox}): the real corpus has rules whose name differs from the output
     * type (e.g. {@code UpiPreEnrichmentData} → output {@code AnnaDsbUpiRequestAndType}).
     */
    @Test
    void generate_bareRuleAsNavReceiver_invokesRuleAndNavigates() throws IOException {
        FixtureResult fx = loadFixture(bareRuleNavReceiverSource());
        try {
            String code = generateFor(fx, "Outer");

            assertTrue(code.contains(
                    "MapperS.of(getBoxRule.evaluate(thenArg.get())).<DayCountEnum>map(\"getConvention\", box -> box.getConvention())"),
                    "a bare rule used as a navigation receiver must invoke the injected "
                    + "rule instance on the then-arg and navigate the feature, with the "
                    + "type witness <DayCountEnum> and the lambda var derived from the "
                    + "rule OUTPUT type (`box`), not the rule name (`getBox`); got:\n" + code);
            assertFalse(code.contains("MapperS.of(GetBox)"),
                    "the bare rule reference must NOT render as the bare type-name "
                    + "literal `MapperS.of(GetBox)` (non-compiling); got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    /**
     * Dependency injection: {@code FunctionDependencyCollector} must recognize the
     * {@link REnumValueRef}-disguised rule reference (its {@code resolvedSymbol} is an
     * {@link RRule}) and register the {@code <Name>Rule} {@code @Inject} field, exactly
     * as it does for a bare {@code then <rule>} {@link RSymbolReference}. The field
     * name ({@code getBoxRule}) is the single source of truth shared with the call
     * receiver rendered by the handler.
     */
    @Test
    void generate_bareRuleAsNavReceiver_injectsRuleDependency() throws IOException {
        FixtureResult fx = loadFixture(bareRuleNavReceiverSource());
        try {
            String code = generateFor(fx, "Outer");

            assertTrue(code.contains("GetBoxRule getBoxRule"),
                    "a bare rule used as a navigation receiver must inject the "
                    + "`@Inject GetBoxRule getBoxRule` dependency field (matching the "
                    + "call receiver name); got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === facet F6 residual: explicit function-call reference as a navigation receiver ===

    /**
     * Shared fixture: an explicit function CALL used as a navigation receiver
     * ({@code GetFreq(item) -> period}). The function returns {@code Frequency}; the
     * navigated feature {@code period} is an enum. The function NAME ({@code GetFreq}
     * -> {@code getFreq}) differs from its OUTPUT TYPE ({@code Frequency} ->
     * {@code frequency}), so the lambda-var assertion distinguishes output-type
     * derivation from function-name derivation. Sibling of the bare-RULE nav-receiver
     * fixture {@link #bareRuleNavReceiverSource()}.
     */
    private static String functionCallNavReceiverSource() {
        return String.join("\n",
                "namespace com.example.test",
                "",
                "type Trade:",
                "    box Box (1..1)",
                "",
                "type Box:",
                "    qty int (1..1)",
                "",
                "type Frequency:",
                "    period DayCountEnum (1..1)",
                "",
                "enum DayCountEnum:",
                "    ACT360",
                "",
                "func GetFreq:",
                "    inputs: box Box (1..1)",
                "    output: result Frequency (1..1)",
                "",
                "reporting rule FreqPeriod from Trade:",
                "    extract box",
                "    then extract GetFreq(item) -> period"
        );
    }

    /**
     * Rendering: an explicit function call used as a navigation receiver
     * ({@code GetFreq(item) -> period}) navigates the feature off the wrapped
     * invocation; the {@code map} lambda var must derive from the navigated feature's
     * OWNER type — the function OUTPUT type ({@code frequency} for output
     * {@code Frequency}) — NOT the function name ({@code getFreq}) and NOT the broken
     * bare reference name. Sibling of the bare-RULE nav-receiver case
     * ({@link #generate_bareRuleAsNavReceiver_invokesRuleAndNavigates}); both route
     * through {@code NavigationHandler.resolveLambdaVarName(RFeatureCall)}, whose
     * owner-type branch covers {@code RRule} AND {@code RFunction} receivers.
     */
    @Test
    void generate_functionCallNavReceiver_lambdaVarFromOutputType() throws IOException {
        FixtureResult fx = loadFixture(functionCallNavReceiverSource());
        try {
            String code = generateFor(fx, "FreqPeriod");

            assertTrue(code.contains(
                    ".<DayCountEnum>map(\"getPeriod\", frequency -> frequency.getPeriod())"),
                    "an explicit function call used as a navigation receiver must derive the "
                    + "map lambda var from the function OUTPUT type (`frequency`), not the "
                    + "function name (`getFreq`) or the bare reference name; got:\n" + code);
            assertFalse(code.contains("getFreq -> getFreq."),
                    "the lambda var must NOT be the function name (`getFreq`); got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === PR #136 (facet lambda_var_inherited_feature): rule/function nav receiver navigating an INHERITED feature ===

    /**
     * Shared fixture: a bare reporting-rule reference ({@code GetPrice}) used as a
     * navigation receiver where the navigated feature ({@code value}) is INHERITED —
     * declared on the supertype {@code MeasureBase}, while the rule's concrete OUTPUT
     * type is the subtype {@code PriceSchedule}. Golden names the map lambda var from
     * the receiver's concrete output type ({@code priceSchedule}), NOT the feature's
     * declaring supertype ({@code measureBase}). Mirrors the real corpus shape
     * {@code drr/.../asic/.../trade/reports/SpreadLeg1Rule.java} (rule {@code SpreadLeg1}
     * outputs {@code PriceSchedule}; {@code value} is declared on {@code MeasureBase}).
     */
    private static String inheritedFeatureRuleNavReceiverSource() {
        return String.join("\n",
                "namespace com.example.test",
                "",
                "type Trade:",
                "    leg Leg (1..1)",
                "",
                "type Leg:",
                "    price PriceSchedule (1..1)",
                "",
                "type MeasureBase:",
                "    value number (0..1)",
                "",
                "type PriceSchedule extends MeasureBase:",
                "    other int (0..1)",
                "",
                "reporting rule GetPrice from Leg:",
                "    extract price",
                "",
                "reporting rule Outer from Trade:",
                "    extract leg",
                "    then GetPrice -> value"
        );
    }

    /**
     * A bare RULE nav receiver navigating an INHERITED feature must derive the map
     * lambda var from the rule's concrete OUTPUT type ({@code priceSchedule} for
     * output {@code PriceSchedule}), NOT the navigated feature's declaring SUPERTYPE
     * ({@code measureBase} for owner {@code MeasureBase}).
     *
     * <p>Root cause: {@code NavigationHandler.resolveLambdaVarName(RFeatureCall)}
     * derives the name from {@code resolvedFeature().parent()} — the feature's OWNER
     * type, which for an inherited feature is the declaring supertype. The fix
     * resolves the receiver rule's output type (workspace type inference on the
     * rule's expression — the same mechanism as the RuleGenerator output back-fill)
     * and substitutes it ONLY when it is a STRICT subtype of the owner (the
     * directly-declared case keeps the owner-derived name byte-identically; the
     * sibling tests {@link #generate_bareRuleAsNavReceiver_invokesRuleAndNavigates}
     * and {@link #generate_functionCallNavReceiver_lambdaVarFromOutputType} lock that
     * no-over-fire guarantee).
     */
    @Test
    void generate_bareRuleNavReceiver_inheritedFeature_lambdaVarFromRuleOutputType() throws IOException {
        FixtureResult fx = loadFixture(inheritedFeatureRuleNavReceiverSource());
        try {
            String code = generateFor(fx, "Outer");

            assertTrue(code.contains(
                    ".<BigDecimal>map(\"getValue\", priceSchedule -> priceSchedule.getValue())"),
                    "a bare rule nav receiver navigating an INHERITED feature must derive "
                    + "the lambda var from the rule's concrete OUTPUT type (`priceSchedule`), "
                    + "not the feature's declaring supertype (`measureBase`); got:\n" + code);
            assertFalse(code.contains("measureBase -> measureBase.getValue()"),
                    "the lambda var must NOT be the feature's declaring supertype "
                    + "(`measureBase`); got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    /**
     * Shared fixture: an explicit function CALL used as a navigation receiver where
     * the navigated feature ({@code value}) is INHERITED — declared on the supertype
     * {@code MeasureBase}, while the function's declared OUTPUT type is the subtype
     * {@code PriceSchedule}. Function sibling of
     * {@link #inheritedFeatureRuleNavReceiverSource()} (the function arm reads the
     * DECLARED output type; the rule arm requires workspace type inference).
     */
    private static String inheritedFeatureFunctionNavReceiverSource() {
        return String.join("\n",
                "namespace com.example.test",
                "",
                "type Trade:",
                "    box Box (1..1)",
                "",
                "type Box:",
                "    qty int (1..1)",
                "",
                "type MeasureBase:",
                "    value number (0..1)",
                "",
                "type PriceSchedule extends MeasureBase:",
                "    other int (0..1)",
                "",
                "func GetPrice:",
                "    inputs: box Box (1..1)",
                "    output: result PriceSchedule (1..1)",
                "",
                "reporting rule Outer from Trade:",
                "    extract box",
                "    then extract GetPrice(item) -> value"
        );
    }

    /**
     * An explicit FUNCTION-call nav receiver navigating an INHERITED feature must
     * derive the map lambda var from the function's declared OUTPUT type
     * ({@code priceSchedule}), NOT the feature's declaring supertype
     * ({@code measureBase}). Same strict-subtype gate as the rule arm
     * ({@link #generate_bareRuleNavReceiver_inheritedFeature_lambdaVarFromRuleOutputType});
     * the function arm reads the declared {@code output()} typeCall instead of
     * inferring from a rule expression.
     */
    @Test
    void generate_functionCallNavReceiver_inheritedFeature_lambdaVarFromOutputType() throws IOException {
        FixtureResult fx = loadFixture(inheritedFeatureFunctionNavReceiverSource());
        try {
            String code = generateFor(fx, "Outer");

            assertTrue(code.contains(
                    ".<BigDecimal>map(\"getValue\", priceSchedule -> priceSchedule.getValue())"),
                    "a function-call nav receiver navigating an INHERITED feature must "
                    + "derive the lambda var from the function's declared OUTPUT type "
                    + "(`priceSchedule`), not the feature's declaring supertype "
                    + "(`measureBase`); got:\n" + code);
            assertFalse(code.contains("measureBase -> measureBase.getValue()"),
                    "the lambda var must NOT be the feature's declaring supertype "
                    + "(`measureBase`); got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === facet F1/F8 residual: number-typedef-alias navigation type-witness ====

    /**
     * Shared fixture: a rule navigating to attributes whose declared type is a
     * PascalCase number {@code typeAlias} — {@code Max3Number} (an {@code int}
     * alias) and {@code Max11Number} (a {@code number} alias). These mirror the
     * real DRR corpus aliases declared in {@code standards-iso-type.rosetta}
     * ({@code typeAlias Max3Number: int(digits: 3)} /
     * {@code typeAlias Max11Number: number(digits: 11, fractionalDigits: 10)}),
     * used pervasively for frequency-multiplier / rate report features.
     */
    private static String numberAliasWitnessSource() {
        return String.join("\n",
                "namespace com.example.reports",
                "",
                "typeAlias Max3Number: int(digits: 3)",
                "",
                "typeAlias Max11Number: number(digits: 11, fractionalDigits: 10)",
                "",
                "type PeriodicPayment:",
                "    multiplier Max3Number (0..1)",
                "    rate Max11Number (0..1)",
                "",
                "type Trade:",
                "    payment PeriodicPayment (1..1)",
                "",
                "reporting rule MultiplierRule from Trade:",
                "    extract payment -> multiplier",
                "",
                "reporting rule RateRule from Trade:",
                "    extract payment -> rate"
        );
    }

    /**
     * A navigation {@code <Type>} generic witness whose feature type is a PascalCase
     * parametric type-ALIAS over {@code int} ({@code Max3Number}) must resolve to the
     * Java base simple name {@code Integer}, NOT the raw alias name {@code Max3Number}
     * (which is no Java type — non-compiling, the reason these files are waivered).
     *
     * <p>Root cause: {@code NavigationHandler.resolveJavaSimpleName} resolved only
     * lowercase-leading basic types ({@code int}→{@code Integer}) through the
     * translator and bailed on every PascalCase name — correct for data types / enums
     * / choices (witness == own name) but wrong for PascalCase {@code typeAlias}es,
     * which the legacy plugin resolves to the wrapped base type. The fix resolves a
     * PascalCase {@link com.regnosys.rosetta.types.RAliasType} through the SAME
     * {@code toJavaReferenceType} the getter return type uses; data/enum/choice
     * (non-alias) PascalCase types are unchanged. Real-corpus anchor:
     * {@code drr/.../asic/.../trade/reports/FixedRatePaymentFrequencyPeriodMultiplierLeg1Rule.java}
     * (golden {@code .<Integer>map("getFixedRatePaymentFrequencyPeriodMultiplier", ...)}).
     */
    @Test
    void generate_numberAliasIntWitness_resolvesToInteger() throws IOException {
        FixtureResult fx = loadFixture(numberAliasWitnessSource());
        try {
            String code = generateFor(fx, "MultiplierRule");

            assertTrue(code.contains(".<Integer>map(\"getMultiplier\""),
                    "a navigation witness over a PascalCase int-alias (`Max3Number`) "
                    + "must resolve to the Java base `<Integer>`, not the raw alias "
                    + "name; got:\n" + code);
            assertFalse(code.contains("<Max3Number>"),
                    "the raw alias name `<Max3Number>` must NOT appear as a witness "
                    + "(it is no Java type — non-compiling); got:\n" + code);
            // Regression guard: a PascalCase DATA TYPE witness (`PeriodicPayment`) is
            // NOT an alias, so it must stay its own name — unchanged by the fix.
            assertTrue(code.contains(".<PeriodicPayment>map(\"getPayment\""),
                    "a PascalCase data-type witness (`PeriodicPayment`) must remain "
                    + "its own name — the fix only resolves type-ALIASES; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    /**
     * Sister case to {@link #generate_numberAliasIntWitness_resolvesToInteger}: a
     * PascalCase parametric type-ALIAS over {@code number} ({@code Max11Number}, with
     * {@code fractionalDigits}) must resolve to {@code BigDecimal} (the {@code number}
     * base Java type), exercising the alias→Java mapping for the decimal case (the int
     * alias maps to {@code Integer}). Confirms the fix delegates to the translator's
     * resolution rather than a hardcoded type. Real-corpus anchor:
     * {@code drr/.../cftc/.../valuation/reports/LastFloatingReferenceValueLeg1Rule.java}
     * (golden {@code .<BigDecimal>map("getLastFloatingReferenceValueLeg1", ...)}).
     */
    @Test
    void generate_numberAliasDecimalWitness_resolvesToBigDecimal() throws IOException {
        FixtureResult fx = loadFixture(numberAliasWitnessSource());
        try {
            String code = generateFor(fx, "RateRule");

            assertTrue(code.contains(".<BigDecimal>map(\"getRate\""),
                    "a navigation witness over a PascalCase number-alias (`Max11Number`) "
                    + "must resolve to the Java base `<BigDecimal>`, not the raw alias "
                    + "name; got:\n" + code);
            assertFalse(code.contains("<Max11Number>"),
                    "the raw alias name `<Max11Number>` must NOT appear as a witness; "
                    + "got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === facet number_alias_comparison: int literal vs a number-alias operand ===

    /**
     * Fixture: a function comparing a {@code number}-alias-typed input
     * ({@code Max18D13Number}, mirroring the real DRR
     * {@code standards-iso-type.rosetta} alias used by ExchangeRate /
     * OptionPremiumAmount / NotionalQuantity validation conditions) to the int
     * literal {@code 0}.
     */
    private static String numberAliasComparisonSource() {
        return String.join("\n",
                "namespace com.example.cmp",
                "",
                "typeAlias Max18D13Number: number(digits: 18, fractionalDigits: 13)",
                "",
                "func RateAboveZero:",
                "    inputs:",
                "        rate Max18D13Number (1..1)",
                "    output:",
                "        result boolean (1..1)",
                "    set result:",
                "        rate > 0"
        );
    }

    /**
     * Sister fixture to {@link #numberAliasComparisonSource()}: an
     * {@code int}-alias-typed input ({@code Max3Number}) compared to {@code 0} —
     * the regression-guard case (Integer context, literal stays bare).
     */
    private static String intAliasComparisonSource() {
        return String.join("\n",
                "namespace com.example.cmp",
                "",
                "typeAlias Max3Number: int(digits: 3)",
                "",
                "func CountAboveZero:",
                "    inputs:",
                "        multiplier Max3Number (1..1)",
                "    output:",
                "        result boolean (1..1)",
                "    set result:",
                "        multiplier > 0"
        );
    }

    /**
     * A comparison operand whose declared type is a PascalCase {@code number}
     * type-ALIAS ({@code Max18D13Number}) must drive the BigDecimal comparison
     * context, so the int literal {@code 0} is wrapped
     * {@code MapperS.of(BigDecimal.valueOf(0))} (matching upstream
     * {@code ExpressionGenerator}), NOT the bare {@code MapperS.of(0)}.
     *
     * <p>Root cause: {@code ComparisonHandler.inferNumericType} classified
     * operands only by their declared builtin name via
     * {@code HandlerHelper.inferRuneTypeName} — which returns the ALIAS name
     * (e.g. {@code Max18D13Number}), not {@code number}, so
     * {@code isNumberType} missed it and the comparison type collapsed to
     * {@code null}, leaving the literal unwrapped. The fix resolves an operand
     * the string heuristic cannot name through the inference engine +
     * {@code toJavaReferenceType} (the same alias-transparent translator path
     * {@code NavigationHandler.resolveJavaSimpleName} uses). Real-corpus
     * anchors: {@code drr/.../trade/price/functions/ExchangeRate_Validation.java}
     * + {@code OptionPremiumAmount_Validation} + {@code NotionalQuantityLeg1_Validation}
     * (golden {@code greaterThan(..., MapperS.of(BigDecimal.valueOf(0)), ...)}).
     */
    @Test
    void generate_numberAliasComparison_wrapsIntLiteralInBigDecimal() throws IOException {
        FixtureResult fx = loadFixture(numberAliasComparisonSource());
        try {
            String code = generateFunctionFor(fx, "RateAboveZero");
            assertTrue(code.contains("MapperS.of(BigDecimal.valueOf(0))"),
                    "an int literal compared against a number-ALIAS operand "
                    + "(`Max18D13Number`) must be wrapped MapperS.of(BigDecimal.valueOf(0)) "
                    + "— the alias resolves to BigDecimal; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    /**
     * Regression guard (sister to
     * {@link #generate_numberAliasComparison_wrapsIntLiteralInBigDecimal}): an
     * {@code int}-alias operand ({@code Max3Number}) resolves to {@code Integer},
     * NOT {@code BigDecimal}, so the literal {@code 0} stays the bare
     * {@code MapperS.of(0)} form — confirming the fix delegates to the
     * translator's alias resolution rather than blanket-wrapping every
     * {@code *Number} alias (which would regress int-alias comparisons).
     */
    @Test
    void generate_intAliasComparison_keepsBareIntLiteral() throws IOException {
        FixtureResult fx = loadFixture(intAliasComparisonSource());
        try {
            String code = generateFunctionFor(fx, "CountAboveZero");
            assertTrue(code.contains("MapperS.of(0)"),
                    "an int literal compared against an int-ALIAS operand (`Max3Number`) "
                    + "must stay the bare MapperS.of(0) form (Integer context); got:\n" + code);
            assertFalse(code.contains("BigDecimal.valueOf(0)"),
                    "an int-alias comparison must NOT wrap the literal in BigDecimal.valueOf "
                    + "— the fix resolves Max3Number to Integer, not BigDecimal; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    /**
     * A conditional whose THEN-branch is itself a conditional
     * ({@code if A then if B then X}, no else) must render in assignOutput
     * context as a NESTED if/else block — matching upstream
     * {@code ExpressionGenerator} — NOT a ternary with a
     * {@code MapperC.of().get()} else default.
     *
     * <p>Root cause: {@code FunctionExpressionRenderer.renderConditionalAssignment}
     * recursed for a nested <em>else</em> branch (the {@code else if} chain) but
     * compiled a nested <em>then</em> branch through the generic expression
     * compiler, which flattens a conditional to
     * {@code ControlFlowHandler}'s ternary string
     * ({@code cond ? then : MapperC.of().get()}). The fix recurses
     * {@code renderConditionalAssignment} for a nested-then
     * {@code RConditionalExpr}, nesting one indent level deeper. Real-corpus
     * anchors: the drr {@code _Validation} family (e.g.
     * {@code ExecutionAgentOfCounterparty2_Validation},
     * {@code ExchangeRateBasisCurrency1_Validation}) whose golden nests
     * {@code if (...) { result = ...; } else { result = null; }} inside the
     * outer {@code if}.
     */
    @Test
    void generate_nestedThenConditional_emitsNestedIfBlock() throws IOException {
        String source = String.join("\n",
                "namespace com.example.functions",
                "",
                "func NestedThenConditional:",
                "    inputs:",
                "        outerInput string (1..1)",
                "        innerInput string (1..1)",
                "    output:",
                "        result boolean (1..1)",
                "    set result:",
                "        if outerInput = \"OUTER\"",
                "        then if innerInput = \"INNER\"",
                "             then True"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFunctionFor(fx, "NestedThenConditional");
            // Two `getOrDefault(false)) {` openers — the outer if AND the
            // recursed inner if (the nested then-block), not a flattened ternary.
            int openers = countOccurrences(code, ".getOrDefault(false)) {");
            assertEquals(2, openers,
                    "a nested-then conditional must emit the inner conditional as "
                    + "a nested if-block (2 `getOrDefault(false)) {` openers), not a "
                    + "ternary; got " + openers + ":\n" + code);
            assertFalse(code.contains("MapperC.of().get()"),
                    "a nested-then conditional must NOT flatten the inner "
                    + "conditional to a ternary with a `MapperC.of().get()` else "
                    + "default; it must nest a full if/else block; got:\n" + code);
            assertFalse(code.contains(" ? "),
                    "a nested-then conditional in assignOutput context must render "
                    + "as nested if/else blocks with no ternary operator; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    /**
     * A SET/assignOutput whose RHS navigates to a meta-wrapper value
     * ({@code FieldWithMetaString}, from a {@code [metadata scheme]} attribute)
     * but whose output is the plain value type must hoist the wrapper into a
     * {@code final} local and null-guard the {@code .getValue()} deref (matching
     * upstream {@code ExpressionGenerator}), NOT assign the wrapper directly — a
     * non-compiling {@code FieldWithMetaString} → {@code String} type mismatch.
     * Real-corpus anchors: {@code AdjustableDateResolution} /
     * {@code Extract_HKMAPartyIdentifier} / {@code InflationRateIndex}.
     */
    @Test
    void generate_metaValueOutput_emitsNullGuardedGetValueDeref() throws IOException {
        String source = String.join("\n",
                "namespace com.example.functions",
                "",
                "type Wrapper:",
                "    inner Inner (1..1)",
                "",
                "type Inner:",
                "    code string (1..1)",
                "        [metadata scheme]",
                "",
                "func ExtractCode:",
                "    inputs:",
                "        wrapper Wrapper (1..1)",
                "    output:",
                "        result string (1..1)",
                "    set result:",
                "        wrapper -> inner -> code"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFunctionFor(fx, "ExtractCode");
            assertTrue(code.contains("final FieldWithMetaString fieldWithMetaString ="),
                    "a meta-wrapper (FieldWithMetaString) RHS assigned to a plain "
                    + "String output must hoist the wrapper into a final local; got:\n" + code);
            assertTrue(code.contains("if (fieldWithMetaString == null)"),
                    "the meta-value deref must be null-guarded; got:\n" + code);
            assertTrue(code.contains("result = fieldWithMetaString.getValue();"),
                    "the else branch must assign the unwrapped .getValue(); got:\n" + code);
            assertFalse(code.contains("result = MapperS.of(wrapper)"),
                    "the fork must NOT assign the meta wrapper directly (a non-compiling "
                    + "FieldWithMetaString -> String mismatch); got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === meta_deref_input_hoist (PR #143): top-level direct call ================

    /**
     * PR #143 (facet {@code meta_deref_input_hoist}): a rule body that is a DIRECT
     * top-level function call (no enclosing lambda, no {@code then}) with (a) the
     * literal {@code item} keyword as an argument and (b) a {@code [metadata reference]}
     * navigation argument whose {@code ReferenceWithMeta<T>} type the callee parameter
     * receives as the plain value {@code T}. Two coupled non-compiling defects:
     * <ol>
     *   <li>the top-level literal {@code item} renders as {@code item.get()} (an
     *       undefined identifier — {@code item} is only bound inside a lambda); golden
     *       passes the rule's {@code input} parameter;</li>
     *   <li>the meta-wrapper nav is inlined directly as the arg
     *       ({@code ReferenceWithMetaParty} where the param expects {@code Party});
     *       golden HOISTS {@code final ReferenceWithMetaParty referenceWithMetaParty =
     *       <nav>.get();} and passes the null-guarded deref
     *       {@code (… == null ? null : ….getValue())}.</li>
     * </ol>
     *
     * <p>The meta-deref-hoist machinery ({@code ReferenceHandler.tryMetaDerefArg}, PR
     * #109) is already capable, but {@code metaDerefHoistRoute} only fired for an
     * extract/map lambda body or a {@code then}-body call — NOT a direct top-level
     * call. The fix extends the gate to the direct top-level rule-body call and lifts
     * the resulting {@code JavaBlockBuilder} hoist in
     * {@code FunctionExpressionRenderer.renderOperation}'s SET path (the statement-level
     * analogue of the PR #129 then-extract lift). Real-corpus anchor: drr
     * {@code InitialMarginCollected…PostHaircutRule} across asic/csa/fca/jfsa/mas.
     *
     * <p>Regression-safe by construction: both pre-fix forms ({@code item.get()} +
     * inline {@code ReferenceWithMetaParty}-into-{@code Party}) do not compile, so no
     * currently-green file carries the shape.
     */
    @Test
    void generate_topLevelDirectCall_itemToInput_andMetaDerefArgHoist() throws IOException {
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Party:",
                "    [metadata key]",
                "    name string (1..1)",
                "",
                "type Side:",
                "    party Party (1..1)",
                "        [metadata reference]",
                "",
                "type Instr:",
                "    side Side (1..1)",
                "",
                "func PickName:",
                "    inputs:",
                "        instr Instr (1..1)",
                "        party Party (1..1)",
                "    output:",
                "        result string (0..1)",
                "    set result:",
                "        party -> name",
                "",
                "reporting rule MarginRule from Instr:",
                "    PickName(item, side -> party)"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "MarginRule");

            // (2) the meta-wrapper nav is hoisted into a final local...
            assertTrue(code.contains("final ReferenceWithMetaParty referenceWithMetaParty ="),
                    "the ReferenceWithMetaParty navigation arg must be hoisted into a "
                    + "final local; got:\n" + code);
            // ...and passed null-guard-deref'd to the plain-Party parameter.
            assertTrue(code.contains(
                    "(referenceWithMetaParty == null ? null : referenceWithMetaParty.getValue())"),
                    "the meta arg must be the null-guarded `.getValue()` deref of the "
                    + "hoisted local; got:\n" + code);
            // (1) the top-level literal `item` arg must render as the rule `input`,
            // NOT the undefined `item.get()`.
            assertTrue(code.contains("evaluate(input, (referenceWithMetaParty"),
                    "the top-level literal `item` argument must render as the rule "
                    + "`input` parameter; got:\n" + code);
            assertFalse(code.contains("item.get()"),
                    "the top-level literal `item` must NOT render as the undefined "
                    + "`item.get()`; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === valuation_currency_metabox_deref (PR #144): then-extract meta nav =======

    /**
     * PR #144 (facet {@code valuation_currency_metabox_deref}): a reporting rule
     * whose body navigates to a {@code [metadata scheme]} string field (surfacing
     * {@code FieldWithMetaString}) {@code then} pipes it into a function taking the
     * plain {@code string} value. Two coupled non-compiling defects:
     * <ol>
     *   <li>the then-extract {@code thenArg} declaration types the meta-surfacing
     *       navigation as {@code MapperS<String>} ({@code getInferredType} drops the
     *       meta wrapper) while the compiled RHS is
     *       {@code MapperS<FieldWithMetaString>} — an incompatible-generics
     *       assignment;</li>
     *   <li>the piped meta value is passed raw to the callee
     *       ({@code convert.evaluate(item.get())} — a {@code FieldWithMetaString}
     *       into a {@code String} parameter) instead of the null-guarded
     *       {@code .getValue()} deref the golden hoists.</li>
     * </ol>
     *
     * <p>Fix: (1) {@code FunctionExpressionRenderer.renderThenExtractSet} prefers the
     * COMPILED meta-surfacing element type over the meta-dropping
     * {@code getInferredType} for the {@code thenArg} declaration; (2)
     * {@code ReferenceHandler.renderImplicitFunctionInvocation} meta-derefs the
     * implicit {@code item.get()} arg (reusing {@code tryMetaDerefArg}), returning a
     * {@code JavaBlockBuilder} the {@code mapSingleToItem} block-lambda renderer
     * ({@code CollectionHandler.compileLambda}) wraps — gated through
     * {@code metaDerefHoistRoute}'s extract/then context. Real-corpus anchor: drr
     * {@code ValuationCurrencyRule} asic/cftc/csa/hkma/mas + iosco
     * {@code SettlementCurrencyRule} / {@code OtherPaymentCurrencyRule}. The fixture
     * navigation path is deliberately MINIMISED ({@code extract unit -> currency}) versus
     * the corpus chain ({@code extract valuationDetails -> valuation -> amount -> unit ->
     * currency}); the facet-essential shape (a {@code [metadata scheme]} string leaf
     * then-piped into a plain-{@code string} function) is reproduced exactly.
     *
     * <p>Regression-safe by construction: both pre-fix forms (the
     * {@code MapperS<String> thenArg = <MapperS<FieldWithMetaString>>}
     * incompatible-generics assignment + the raw
     * {@code FieldWithMetaString}-into-{@code String} arg) do not compile, so no
     * currently-green file carries the shape.
     */
    @Test
    void generate_thenExtractMetaNav_typesThenArgMeta_andDerefsImplicitArg() throws IOException {
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Instr:",
                "    unit Unit (1..1)",
                "",
                "type Unit:",
                "    currency string (1..1)",
                "        [metadata scheme]",
                "",
                "func ConvertCurrency:",
                "    inputs:",
                "        ccy string (1..1)",
                "    output:",
                "        result string (1..1)",
                "    set result:",
                "        ccy",
                "",
                "reporting rule CurrencyRule from Instr:",
                "    extract unit -> currency",
                "    then extract ConvertCurrency"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "CurrencyRule");

            // (1) the thenArg declaration preserves the meta-surfacing element type.
            assertTrue(code.contains("MapperS<FieldWithMetaString> thenArg"),
                    "the then-extract thenArg declaration must preserve the "
                    + "meta-surfacing FieldWithMetaString element type, not the "
                    + "meta-dropped String; got:\n" + code);
            assertFalse(code.contains("MapperS<String> thenArg"),
                    "the thenArg must NOT be typed MapperS<String> (incompatible with "
                    + "the MapperS<FieldWithMetaString> RHS); got:\n" + code);
            // (2) the piped meta value is hoisted + null-guard-deref'd.
            assertTrue(code.contains("final FieldWithMetaString fieldWithMetaString ="),
                    "the implicit meta arg must be hoisted into a final local; got:\n"
                    + code);
            assertTrue(code.contains(
                    "(fieldWithMetaString == null ? null : fieldWithMetaString.getValue())"),
                    "the implicit meta arg must be the null-guarded .getValue() deref "
                    + "of the hoisted local; got:\n" + code);
            assertFalse(code.contains("convertCurrency.evaluate(item.get())"),
                    "the fork must NOT pass the raw FieldWithMetaString item.get() to a "
                    + "String parameter; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    /**
     * PR #144 edit (3): the {@code SettlementCurrencyRule} variant — the
     * {@code thenArg} base is an else-less (synthetic-empty-list-else) conditional
     * {@code extract (if <cond> then <meta-field>)} routing through
     * {@code CollectionHandler.compileElselessConditionalBlock}. The block's
     * {@code MapperS.<T>ofNull()} default type {@code T} is derived from the
     * conditional's inferred type, which DROPS the meta wrapper to {@code String},
     * while the {@code if}-branch returns the meta-surfacing
     * {@code MapperS<FieldWithMetaString>} — so the {@code thenArg} declaration
     * (edit 1) and the {@code ofNull} default disagree on the element type
     * (non-compiling). Edit (3) prefers the compiled meta-surfacing element type for
     * the {@code ofNull} default too, matching edits (1)+(2). Real-corpus anchor:
     * drr iosco {@code SettlementCurrencyRule}.
     */
    @Test
    void generate_thenExtractElselessCondMetaNav_typesOfNullMeta() throws IOException {
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "type Terms:",
                "    flag boolean (1..1)",
                "    ccy string (1..1)",
                "        [metadata scheme]",
                "",
                "func ConvertCurrency:",
                "    inputs:",
                "        c string (1..1)",
                "    output:",
                "        result string (1..1)",
                "    set result:",
                "        c",
                "",
                "reporting rule SettlementCurrencyRule from Terms:",
                "    extract",
                "        if flag = True",
                "        then ccy",
                "    then extract ConvertCurrency"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx, "SettlementCurrencyRule");
            // edit (3): the elseless-conditional block default preserves the
            // meta-surfacing element type.
            assertTrue(code.contains("MapperS.<FieldWithMetaString>ofNull()"),
                    "the elseless-conditional block default must type ofNull with the "
                    + "meta-surfacing FieldWithMetaString element, not String; got:\n"
                    + code);
            assertFalse(code.contains("MapperS.<String>ofNull()"),
                    "the ofNull default must NOT drop the meta wrapper to String; got:\n"
                    + code);
            // edits (1)+(2) also apply to this shape.
            assertTrue(code.contains("MapperS<FieldWithMetaString> thenArg"),
                    "the thenArg must preserve the meta element type; got:\n" + code);
            assertTrue(code.contains("final FieldWithMetaString fieldWithMetaString ="),
                    "the implicit meta arg must be hoisted; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    /** Count non-overlapping occurrences of {@code needle} in {@code haystack}. */
    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        for (int i = haystack.indexOf(needle); i >= 0;
                i = haystack.indexOf(needle, i + needle.length())) {
            count++;
        }
        return count;
    }

    // === Helpers ===============================================================

    /** Bundle of per-fixture state. */
    private static final class FixtureResult {
        final RModel model;
        final RLinkingResult linkingResult;
        final GeneratorModel generatorModel;
        final FunctionGenerator functionGenerator;

        FixtureResult(RModel model, RLinkingResult linkingResult,
                      GeneratorModel generatorModel,
                      FunctionGenerator functionGenerator) {
            this.model = model;
            this.linkingResult = linkingResult;
            this.generatorModel = generatorModel;
            this.functionGenerator = functionGenerator;
        }
    }

    /**
     * Construct a {@link RuleGenerator} wired to the fixture's per-call
     * dependencies. Each test calls this fresh — the generator is stateless
     * across calls.
     */
    private RuleGenerator newGenerator(FixtureResult fx) {
        return new RuleGenerator(fx.generatorModel, typeTranslator, fx.functionGenerator);
    }

    /**
     * Parse the synthetic source + load builtins + build the workspace +
     * construct GeneratorModel + FunctionGenerator. Mirrors
     * {@code LabelProviderGeneratorTest.loadFixture} substituting
     * FunctionGenerator (needed for the rBuildClass delegation path) for
     * DeepFeatureCallUtil (T3-specific).
     */
    private FixtureResult loadFixture(String source) throws IOException {
        RModel model = AstBuilder.buildFromString(source, "test.rosetta");
        model.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(model);
        models.addAll(loadBuiltinsOnly());
        RLinkingResult linkingResult = RWorkspace.build(models);
        GeneratorModel gm = new GeneratorModel(linkingResult.workspace());
        FunctionGenerator fg = new FunctionGenerator(gm, typeTranslator, typeUtil);
        return new FixtureResult(model, linkingResult, gm, fg);
    }

    /**
     * Multi-model variant of {@link #loadFixture(String)} for cross-namespace
     * scenarios. Each source becomes its own {@link RModel}; the workspace links
     * them together; the {@link GeneratorModel} is constructed with the supplied
     * {@code emissionFilter} so a test can EXCLUDE a from-type's namespace from
     * emission while keeping it loaded for type resolution (the transitive-
     * dependency shape — see {@link #generate_inputTypeFromExcludedNamespace_resolvesViaOrigin}).
     *
     * <p>{@link FixtureResult#model} is the FIRST source's model — by convention
     * the one carrying the rule(s) under test.
     */
    private FixtureResult loadFixtureMulti(List<String> sources,
                                           Predicate<RModel> emissionFilter) throws IOException {
        List<RModel> userModels = new ArrayList<>();
        int i = 0;
        for (String src : sources) {
            RModel m = AstBuilder.buildFromString(src, "test" + (i++) + ".rosetta");
            m.setVersion("0.0.0.test");
            userModels.add(m);
        }
        List<RModel> models = new ArrayList<>(userModels);
        models.addAll(loadBuiltinsOnly());
        RLinkingResult linkingResult = RWorkspace.build(models);
        GeneratorModel gm = new GeneratorModel(linkingResult.workspace(), emissionFilter);
        FunctionGenerator fg = new FunctionGenerator(gm, typeTranslator, typeUtil);
        return new FixtureResult(userModels.get(0), linkingResult, gm, fg);
    }

    /**
     * Locate the first RFunction streamed by the generator with the given
     * name + run the {@code createTypeRepresentation} + {@code generate}
     * pipeline.
     */
    private String generateFor(FixtureResult fx, String functionName) {
        RuleGenerator gen = newGenerator(fx);
        RFunction f = gen.streamObjects(fx.model)
                .filter(rf -> functionName.equals(rf.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "rule '" + functionName + "' was not streamed; either it "
                        + "isn't an RRule or the name doesn't match. Verify the "
                        + "test source above."));
        return gen.generate(f, gen.createTypeRepresentation(f), "1.0");
    }

    /**
     * Generate the named FUNCTION via the fixture's {@link FunctionGenerator}
     * (the same wired generator a corpus run uses) and return its source. Used
     * by the number-alias comparison tests, which exercise
     * {@code ComparisonHandler} through a function body rather than a rule.
     */
    private String generateFunctionFor(FixtureResult fx, String functionName) {
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> errors = fx.functionGenerator.generateWithErrors(output);
        assertTrue(errors.isEmpty(),
                "fixture function generation produced errors: " + errors);
        return output.entrySet().stream()
                .filter(e -> e.getKey().endsWith(functionName + ".java"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "function '" + functionName + "' was not generated; keys="
                        + output.keySet()));
    }

    /**
     * Mirrors {@code LabelProviderGeneratorTest.loadBuiltinsOnly} — union
     * over {@link #BUILTINS_SEARCH_ROOTS}, dedup by filename, aggregate
     * parse failures into an {@link AssertionError} rather than silently
     * swallowing per the {@code System.err.println} discipline locked at
     * PR #68 R1 F12.
     */
    private List<RModel> loadBuiltinsOnly() throws IOException {
        List<String> failures = new ArrayList<>();
        List<RModel> models = loadBuiltinsOnly(failures);
        if (!failures.isEmpty()) {
            throw new AssertionError(
                    "[RuleGeneratorTest] loadBuiltinsOnly: "
                    + failures.size() + " parse failure(s) — first: "
                    + failures.get(0)
                    + (failures.size() > 1
                        ? " (and " + (failures.size() - 1) + " more — full list: "
                          + String.join("; ", failures.subList(1, failures.size())) + ")"
                        : ""));
        }
        return models;
    }

    private List<RModel> loadBuiltinsOnly(List<String> failures) throws IOException {
        Map<String, Path> resolved = new LinkedHashMap<>();
        for (Path root : BUILTINS_SEARCH_ROOTS) {
            if (!Files.isDirectory(root)) continue;
            try (var stream = Files.walk(root)) {
                stream.filter(p -> p.toString().endsWith(".rosetta"))
                      .forEach(p -> resolved.putIfAbsent(p.getFileName().toString(), p));
            }
        }
        List<RModel> models = new ArrayList<>();
        resolved.values().stream()
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .forEach(p -> {
                    try { models.add(AstBuilder.buildFromFile(p)); }
                    catch (Exception e) {
                        failures.add(p + " — " + e);
                    }
                });
        return models;
    }
}
