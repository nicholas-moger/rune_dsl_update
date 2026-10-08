package com.regnosys.rosetta.generator.java.function;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.enums.ListOp;
import com.regnosys.rosetta.ast.expressions.literals.RBooleanLiteral;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RCardinality;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.types.RType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FunctionAliasHelper}.
 *
 * <p>All tests use synthetic, in-memory AST nodes — no parser, no file I/O.
 *
 * <p>Test matrix:
 * <ul>
 *   <li>Function with no aliases → empty list</li>
 *   <li>Alias not referencing output → {@code usesOutput=false}, only inputs in params</li>
 *   <li>Alias referencing output → {@code usesOutput=true}, output builder is first param</li>
 *   <li>Alias with multi-valued output → {@code MapperC} return type</li>
 *   <li>Multiple aliases → structural ordering preserved</li>
 *   <li>Void function (no output) → graceful fallback</li>
 *   <li>usesOutput helper: null expression → false</li>
 *   <li>usesOutput helper: nested reference → found</li>
 *   <li>compiledBody is always empty string (filled by renderer later)</li>
 * </ul>
 */
class FunctionAliasHelperTest {

    private FunctionAliasHelper helper;

    @BeforeEach
    void setUp() {
        helper = new FunctionAliasHelper();
    }

    // =========================================================================
    // Factory helpers
    // =========================================================================

    /** Build an {@link RAttribute} with the given name and type name. */
    private static RAttribute attr(String name, String typeName) {
        return attr(name, typeName, false);
    }

    /** Build an {@link RAttribute} with explicit multi-valued flag. */
    private static RAttribute attr(String name, String typeName, boolean multi) {
        RTypeCall tc = new RTypeCall();
        tc.setTypeName(typeName);

        RCardinality card = new RCardinality();
        if (multi) {
            card.setUnbounded(true);
        } else {
            card.setInf(0);
            card.setSup(1);
        }

        RAttribute a = new RAttribute();
        a.setName(name);
        a.setTypeCall(tc);
        a.setCardinality(card);
        return a;
    }

    /** Build a minimal {@link RFunction} with a given output attribute and inputs. */
    private static RFunction func(RAttribute output, List<RAttribute> inputs) {
        RFunction f = new RFunction();
        f.setName("TestFunc");
        f.setOutput(output);
        f.inputs().addAll(inputs);
        return f;
    }

    /** Build a void (no output) {@link RFunction} with given inputs. */
    private static RFunction voidFunc(List<RAttribute> inputs) {
        RFunction f = new RFunction();
        f.setName("VoidFunc");
        f.inputs().addAll(inputs);
        return f;
    }

    /**
     * Build an {@link RShortcut} with the given name and expression, and add
     * it to the function.
     */
    private static RShortcut shortcut(String name, RExpression expr, RFunction func) {
        RShortcut sc = new RShortcut();
        sc.setName(name);
        sc.setExpression(expr);
        func.shortcuts().add(sc);
        return sc;
    }

    /** A symbol reference that names the given identifier (not resolved). */
    private static RSymbolReference unresolved(String name) {
        RSymbolReference ref = new RSymbolReference();
        ref.setName(name);
        return ref;
    }

    /** A leaf expression (RBooleanLiteral) that does not reference any symbol. */
    private static RBooleanLiteral boolLiteral(boolean value) {
        RBooleanLiteral lit = new RBooleanLiteral();
        lit.setValue(value);
        return lit;
    }

    // =========================================================================
    // No shortcuts
    // =========================================================================

    @Test
    void function_with_no_shortcuts_returns_empty_list() {
        RFunction f = func(attr("result", "Trade"), List.of());
        List<FunctionTemplateModel.AliasModel> models = helper.analyze(f);
        assertTrue(models.isEmpty());
    }

    // =========================================================================
    // Alias not referencing output (usesOutput = false)
    // =========================================================================

    @Test
    void alias_not_referencing_output_uses_output_false() {
        RAttribute output = attr("result", "Trade");
        RFunction f = func(output, List.of());

        // Expression references a different name
        shortcut("myAlias", unresolved("someInput"), f);

        List<FunctionTemplateModel.AliasModel> models = helper.analyze(f);

        assertEquals(1, models.size());
        assertFalse(models.get(0).getUsesOutput());
    }

    @Test
    void alias_not_referencing_output_returns_mapper_s_for_single() {
        RAttribute output = attr("result", "Trade");
        RFunction f = func(output, List.of());

        shortcut("myAlias", unresolved("someInput"), f);

        var model = helper.analyze(f).get(0);
        assertEquals("MapperS<? extends Trade>", model.getReturnType());
    }

    @Test
    void alias_not_referencing_output_params_are_only_inputs() {
        RAttribute output = attr("result", "Trade");
        RAttribute input1 = attr("trade", "Trade");
        RAttribute input2 = attr("rate", "Rate");
        RFunction f = func(output, List.of(input1, input2));

        shortcut("myAlias", unresolved("trade"), f);

        var model = helper.analyze(f).get(0);
        assertEquals(List.of("Trade trade", "Rate rate"), model.getParams());
    }

    // =========================================================================
    // Alias referencing output (usesOutput = true)
    // =========================================================================

    @Test
    void alias_referencing_output_uses_output_true() {
        RAttribute output = attr("result", "Trade");
        RFunction f = func(output, List.of());

        // Expression directly names the output parameter
        shortcut("outputAlias", unresolved("result"), f);

        var model = helper.analyze(f).get(0);
        assertTrue(model.getUsesOutput());
    }

    @Test
    void alias_referencing_output_return_type_is_builder_type() {
        RAttribute output = attr("result", "Trade");
        RFunction f = func(output, List.of());

        shortcut("outputAlias", unresolved("result"), f);

        var model = helper.analyze(f).get(0);
        assertEquals("Trade.TradeBuilder", model.getReturnType());
    }

    @Test
    void alias_referencing_output_first_param_is_output_builder() {
        RAttribute output = attr("result", "Trade");
        RAttribute input1 = attr("trade", "Trade");
        RFunction f = func(output, List.of(input1));

        shortcut("outputAlias", unresolved("result"), f);

        var model = helper.analyze(f).get(0);
        assertEquals(2, model.getParams().size());
        assertEquals("Trade.TradeBuilder result", model.getParams().get(0));
        assertEquals("Trade trade", model.getParams().get(1));
    }

    @Test
    void alias_referencing_output_with_no_inputs_has_only_output_param() {
        RAttribute output = attr("result", "Money");
        RFunction f = func(output, List.of());

        shortcut("moneyAlias", unresolved("result"), f);

        var model = helper.analyze(f).get(0);
        assertEquals(List.of("Money.MoneyBuilder result"), model.getParams());
    }

    // =========================================================================
    // Multi-valued output → MapperC
    // =========================================================================

    @Test
    void alias_with_multi_valued_output_not_using_output_returns_mapper_c() {
        RAttribute output = attr("result", "Trade", /*multi=*/true);
        RFunction f = func(output, List.of());

        shortcut("multiAlias", unresolved("someInput"), f);

        var model = helper.analyze(f).get(0);
        assertFalse(model.getUsesOutput());
        assertEquals("MapperC<? extends Trade>", model.getReturnType());
    }

    // =========================================================================
    // Void function (no output)
    // =========================================================================

    @Test
    void void_function_alias_not_using_output_returns_mapper_s_wildcard() {
        RFunction f = voidFunc(List.of(attr("x", "Integer")));
        shortcut("voidAlias", boolLiteral(true), f);

        var model = helper.analyze(f).get(0);
        assertFalse(model.getUsesOutput());
        assertEquals("MapperS<?>", model.getReturnType());
    }

    @Test
    void void_function_alias_params_are_only_inputs() {
        RFunction f = voidFunc(List.of(attr("x", "Integer")));
        shortcut("voidAlias", boolLiteral(true), f);

        var model = helper.analyze(f).get(0);
        assertEquals(List.of("Integer x"), model.getParams());
    }

    // =========================================================================
    // Multiple aliases
    // =========================================================================

    @Test
    void multiple_aliases_are_returned_in_declaration_order() {
        RAttribute output = attr("result", "Trade");
        RAttribute input = attr("t", "Trade");
        RFunction f = func(output, List.of(input));

        shortcut("aliasA", boolLiteral(false), f);
        shortcut("aliasB", unresolved("result"), f);  // references output
        shortcut("aliasC", unresolved("t"), f);

        List<FunctionTemplateModel.AliasModel> models = helper.analyze(f);

        assertEquals(3, models.size());
        assertEquals("aliasA", models.get(0).getName());
        assertEquals("aliasB", models.get(1).getName());
        assertEquals("aliasC", models.get(2).getName());
    }

    @Test
    void multiple_aliases_output_usage_flags_set_independently() {
        RAttribute output = attr("result", "Trade");
        RFunction f = func(output, List.of());

        shortcut("noOutput", boolLiteral(true), f);
        shortcut("withOutput", unresolved("result"), f);

        List<FunctionTemplateModel.AliasModel> models = helper.analyze(f);

        assertFalse(models.get(0).getUsesOutput(), "aliasA should not use output");
        assertTrue(models.get(1).getUsesOutput(), "aliasB should use output");
    }

    // =========================================================================
    // compiledBody is always empty string
    // =========================================================================

    @Test
    void compiled_body_is_empty_string() {
        RAttribute output = attr("result", "Trade");
        RFunction f = func(output, List.of());

        shortcut("anyAlias", unresolved("result"), f);

        var model = helper.analyze(f).get(0);
        assertEquals("", model.getCompiledBody());
    }

    // =========================================================================
    // name is preserved
    // =========================================================================

    @Test
    void alias_name_is_preserved() {
        RAttribute output = attr("result", "Trade");
        RFunction f = func(output, List.of());

        shortcut("mySpecialAlias", unresolved("result"), f);

        var model = helper.analyze(f).get(0);
        assertEquals("mySpecialAlias", model.getName());
    }

    // =========================================================================
    // usesOutput helper: edge cases
    // =========================================================================

    @Test
    void uses_output_null_expression_returns_false() {
        RAttribute output = attr("result", "Trade");
        RFunction f = func(output, List.of());

        // A shortcut with null expression should not throw and should yield usesOutput=false
        RShortcut sc = new RShortcut();
        sc.setName("nullExprAlias");
        sc.setExpression(null);
        f.shortcuts().add(sc);

        List<FunctionTemplateModel.AliasModel> models = helper.analyze(f);

        assertEquals(1, models.size());
        assertFalse(models.get(0).getUsesOutput());
    }

    @Test
    void uses_output_via_arg_in_symbol_reference() {
        // A symbol reference whose name is NOT the output, but one of its *args*
        // is another RSymbolReference that IS the output — the walk must descend.
        RAttribute output = attr("result", "Trade");
        RFunction f = func(output, List.of());

        // Build: someFunc(result)  — outer ref name = "someFunc", inner ref name = "result"
        RSymbolReference inner = unresolved("result");
        RSymbolReference outer = new RSymbolReference();
        outer.setName("someFunc");
        outer.args().add(inner);

        shortcut("deepAlias", outer, f);

        var model = helper.analyze(f).get(0);
        assertTrue(model.getUsesOutput(),
                "Output reference nested in call args should be detected");
    }

    @Test
    void uses_output_helper_direct_match() {
        RSymbolReference ref = unresolved("output");
        assertTrue(helper.usesOutput(ref, "output"));
    }

    @Test
    void uses_output_helper_no_match() {
        RSymbolReference ref = unresolved("something");
        assertFalse(helper.usesOutput(ref, "output"));
    }

    @Test
    void uses_output_helper_leaf_with_no_children() {
        RBooleanLiteral lit = boolLiteral(true);
        assertFalse(helper.usesOutput(lit, "output"));
    }

    // =========================================================================
    // Transitive alias detection (BLOCKER-1)
    // =========================================================================

    /**
     * Alias A has an expression that is a symbol reference resolved to alias B.
     * Alias B's expression directly references the output.
     * helper.usesOutput on alias A's expression must return true (transitive detection).
     */
    @Test
    void transitive_alias_to_output_detected() {
        // Build: aliasB expression = unresolved("result")
        RShortcut aliasB = new RShortcut();
        aliasB.setName("aliasB");
        aliasB.setExpression(unresolved("result"));

        // Build: aliasA expression = ref to "aliasB", resolved to aliasB shortcut
        RSymbolReference refToB = new RSymbolReference();
        refToB.setName("aliasB");
        refToB.setResolvedSymbol(aliasB);

        // usesOutput on aliasA's expression should be true (transitively via B → result)
        assertTrue(helper.usesOutput(refToB, "result"),
                "Transitive: aliasA → aliasB → output should be detected");
    }

    /**
     * Alias A → alias B, but alias B does NOT reference the output.
     * helper.usesOutput on alias A's expression must return false.
     */
    @Test
    void transitive_alias_not_to_output_returns_false() {
        RShortcut aliasB = new RShortcut();
        aliasB.setName("aliasB");
        aliasB.setExpression(unresolved("someInput"));  // not the output

        RSymbolReference refToB = new RSymbolReference();
        refToB.setName("aliasB");
        refToB.setResolvedSymbol(aliasB);

        assertFalse(helper.usesOutput(refToB, "result"),
                "Transitive: aliasA → aliasB → non-output should return false");
    }

    /**
     * Verify that a cycle in alias references does not cause infinite recursion.
     * Alias A resolves to alias B, alias B resolves back to alias A — no output ref.
     */
    @Test
    void transitive_alias_cycle_does_not_loop_infinitely() {
        // aliasA and aliasB each reference the other — circular, but no output ref
        RShortcut aliasA = new RShortcut();
        aliasA.setName("aliasA");
        RShortcut aliasB = new RShortcut();
        aliasB.setName("aliasB");

        RSymbolReference refToA = new RSymbolReference();
        refToA.setName("aliasA");
        refToA.setResolvedSymbol(aliasA);

        RSymbolReference refToB = new RSymbolReference();
        refToB.setName("aliasB");
        refToB.setResolvedSymbol(aliasB);

        aliasA.setExpression(refToB);
        aliasB.setExpression(refToA);

        // Should terminate without StackOverflow and return false (no output ref)
        assertFalse(helper.usesOutput(refToB, "result"),
                "Cyclic alias graph should terminate without StackOverflow");
    }

    // =========================================================================
    // Multi-valued output with usesOutput=true (BLOCKER-2)
    // =========================================================================

    @Test
    void alias_with_multi_valued_output_using_output_returns_list_builder_type() {
        RAttribute output = attr("result", "Trade", /*multi=*/true);
        RFunction f = func(output, List.of());

        // Expression directly references the output
        shortcut("multiOutputAlias", unresolved("result"), f);

        var model = helper.analyze(f).get(0);
        assertTrue(model.getUsesOutput(), "alias should use output");
        assertEquals("List<Trade.TradeBuilder>", model.getReturnType(),
                "Multi-valued output with usesOutput=true should return List<BuilderType>");
    }

    @Test
    void alias_with_single_valued_output_using_output_returns_bare_builder_type() {
        RAttribute output = attr("result", "Trade", /*multi=*/false);
        RFunction f = func(output, List.of());

        shortcut("singleOutputAlias", unresolved("result"), f);

        var model = helper.analyze(f).get(0);
        assertTrue(model.getUsesOutput());
        assertEquals("Trade.TradeBuilder", model.getReturnType(),
                "Single-valued output with usesOutput=true should not be List-wrapped");
    }

    // =========================================================================
    // PR-A §9.1 A3-D2-02: 3-arg inferExpressionType overload + analyze wiring
    // =========================================================================
    //
    // Harness constraint: the legacy zero-arg FunctionAliasHelper used by this
    // test file constructs with null generatorModel/typeTranslator/typeUtil.
    // The inference walker (inferExpressionType and its helpers) gates every
    // RType-resolving path on those fields being non-null, so synthetic
    // RExpressions cannot drive the walker to emit RTypes here. A test that
    // asserts `refs.contains(<named RType>)` after calling the walker would
    // require either (a) a parse-from-string harness that builds a real
    // GeneratorModel, or (b) Mockito-style stubs (not on the classpath). The
    // D11 corpus regression is the effective coverage for populate-refs
    // behaviour until such a harness exists.
    //
    // The two tests below verify what IS achievable without that harness:
    //   1. the 3-arg overload is present and callable (signature pin);
    //   2. analyze() returns AliasModels whose inferredRefs set is the
    //      collector's output — for the zero-arg helper that set is empty,
    //      but the wiring is exercised.

    /**
     * Step 1.1b: verify the 3-arg overload exists with the expected signature.
     * Before Step 1.3a this method did not exist and reflective lookup would
     * throw NoSuchMethodException; the passing state demonstrates that the
     * refactor exposed the new collector-aware entry point.
     */
    @Test
    void inferExpressionType_3arg_overload_is_present_and_callable() throws Exception {
        Method m = FunctionAliasHelper.class.getDeclaredMethod(
                "inferExpressionType",
                RExpression.class,
                Set.class,
                Set.class);
        assertNotNull(m, "3-arg inferExpressionType must exist (PR-A §9.1 A3-D2-02)");

        // Calling the method with a leaf literal on a zero-arg helper exercises
        // the early-return path and leaves the refs set untouched — proving
        // the method signature accepts the collector.
        RBooleanLiteral leaf = boolLiteral(true);
        Set<RType> refs = new HashSet<>();
        m.setAccessible(true);
        Object result = m.invoke(helper, leaf, new HashSet<RNode>(), refs);
        assertNotNull(result, "Boolean literal inference returns a non-null ExpressionTypeInfo");
        assertTrue(refs.isEmpty(),
                "Leaf literal has no resolved RType emission points — refs must remain empty");
    }

    /**
     * Step 1.5: verify analyze() populates {@code inferredRefs} through the
     * 7-arg AliasModel constructor. With the zero-arg helper the walker's
     * inference path is disabled (null generatorModel), so the set is empty;
     * the test pins the wiring rather than the content.
     *
     * <p>Coverage for the non-empty case lives in D11CorpusRegressionTest,
     * which drives the full GeneratorModel via the parser + workspace.
     */
    @Test
    void analyze_populates_aliasModel_inferredRefs_via_7arg_constructor() {
        RAttribute output = attr("result", "Trade");
        RAttribute input = attr("trade", "Trade");
        RFunction f = func(output, List.of(input));

        shortcut("aliasA", unresolved("trade"), f);

        var model = helper.analyze(f).get(0);
        assertNotNull(model.getInferredRefs(),
                "inferredRefs must be a non-null Set (populated by analyze via 7-arg constructor)");
        assertTrue(model.getInferredRefs().isEmpty(),
                "Zero-arg helper has no GeneratorModel, so inferredRefs is empty — "
                + "non-empty case is covered by D11CorpusRegressionTest.");
    }

    /**
     * v3.2 seat 2, PR #623 (round-1 cq review, N-9): the relation between the alias SIGNATURE walk's
     * element-preserving op set and the two mirrors' (the RType twin and the render-side hoist gate) is
     * PINNED - SIGNATURE = MIRROR + {REVERSE}, and the mirrors are the pre-seat five - so an edit to one of
     * the three sites fails here rather than drifting silently.
     */
    @Test
    void elementPreservingOpSets_signatureIsTheMirrorsPlusReverse() {
        var mirror = EnumSet.copyOf(FunctionAliasHelper.MIRROR_ELEMENT_PRESERVING_OPS);
        assertEquals(EnumSet.of(ListOp.FIRST, ListOp.LAST, ListOp.ONLY_ELEMENT, ListOp.FLATTEN, ListOp.DISTINCT),
                mirror);
        var expectedSignature = EnumSet.copyOf(mirror);
        expectedSignature.add(ListOp.REVERSE);
        assertEquals(expectedSignature, EnumSet.copyOf(FunctionAliasHelper.SIGNATURE_ELEMENT_PRESERVING_OPS));
    }
}
