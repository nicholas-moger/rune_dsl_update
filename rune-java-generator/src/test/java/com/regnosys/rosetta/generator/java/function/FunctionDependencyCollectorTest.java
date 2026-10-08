package com.regnosys.rosetta.generator.java.function;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.expressions.literals.RBooleanLiteral;
import com.regnosys.rosetta.ast.expressions.references.RDeepFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.ROperation;
import com.regnosys.rosetta.ast.functions.RPostCondition;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link FunctionDependencyCollector}.
 *
 * <p>Strategy: build minimal in-memory AST trees (no parser, no file I/O),
 * wire parent relationships manually, and use real {@link GeneratorModel} /
 * {@link JavaTypeTranslator} instances. This validates the full pipeline from
 * expression-walking through type resolution to {@link FunctionTemplateModel.DependencyModel}
 * construction.
 */
class FunctionDependencyCollectorTest {

    // -------------------------------------------------------------------------
    // Shared infrastructure
    // -------------------------------------------------------------------------

    private GeneratorModel generatorModel;
    private JavaTypeTranslator typeTranslator;
    private FunctionDependencyCollector collector;

    @BeforeEach
    void setUp() {
        // Build an empty workspace — sufficient because GeneratorModel.symbolId()
        // only requires that the function's parent is an RModel.
        RWorkspace ws = RWorkspace.build(List.of()).workspace();
        generatorModel = new GeneratorModel(ws);
        typeTranslator = new JavaTypeTranslator(new JavaTypeUtil());
        collector = new FunctionDependencyCollector(generatorModel, typeTranslator);
    }

    // -------------------------------------------------------------------------
    // Helper factories
    // -------------------------------------------------------------------------

    /**
     * Build an {@link RFunction} whose parent is a fresh {@link RModel} with
     * the given namespace. The function carries no body — tests add children.
     */
    private static RFunction functionInNamespace(String namespace, String name) {
        RModel model = new RModel();
        model.setNamespace(namespace);

        RFunction func = new RFunction();
        func.setName(name);
        func.setParent(model);
        return func;
    }

    /**
     * Build an {@link RSymbolReference} that resolves to {@code callee}.
     */
    private static RSymbolReference symRefTo(RFunction callee) {
        RSymbolReference ref = new RSymbolReference();
        ref.setName(callee.name());
        ref.setResolvedSymbol(callee);
        return ref;
    }

    /**
     * Wrap an expression in an {@link ROperation} and attach it to the function.
     */
    private static void addOperation(RFunction func, RExpression expr) {
        ROperation op = new ROperation();
        op.setExpression(expr);
        func.operations().add(op);
    }

    /**
     * Wrap an expression in an {@link RShortcut} and attach it to the function.
     */
    private static void addShortcut(RFunction func, RExpression expr) {
        RShortcut shortcut = new RShortcut();
        shortcut.setName("alias_" + func.shortcuts().size());
        shortcut.setExpression(expr);
        func.shortcuts().add(shortcut);
    }

    /**
     * Wrap an expression in an {@link RCondition} and attach it to the function.
     */
    private static void addCondition(RFunction func, RExpression expr) {
        RCondition cond = new RCondition();
        cond.setExpression(expr);
        func.conditions().add(cond);
    }

    /**
     * Wrap an expression in an {@link RPostCondition} and attach it to the function.
     */
    private static void addPostCondition(RFunction func, RExpression expr) {
        RPostCondition pc = new RPostCondition();
        pc.setExpression(expr);
        func.postConditions().add(pc);
    }

    // =========================================================================
    // Empty function
    // =========================================================================

    @Test
    void empty_function_returns_empty_list() {
        RFunction func = functionInNamespace("com.example", "MyFunc");
        List<FunctionTemplateModel.DependencyModel> deps = collector.collect(func);
        assertTrue(deps.isEmpty(), "No deps expected for a function with no body");
    }

    // =========================================================================
    // Single dependency from an operation
    // =========================================================================

    @Test
    void single_function_call_in_operation_is_collected() {
        RFunction callee = functionInNamespace("com.example", "CalculatePrice");
        RFunction caller = functionInNamespace("com.example", "ProcessTrade");

        addOperation(caller, symRefTo(callee));

        List<FunctionTemplateModel.DependencyModel> deps = collector.collect(caller);

        assertEquals(1, deps.size());
        FunctionTemplateModel.DependencyModel dep = deps.get(0);
        assertEquals("calculatePrice", dep.getFieldName());
        assertEquals("CalculatePrice", dep.getTypeName());
        assertEquals("com.example.functions.CalculatePrice", dep.getTypeFqn());
        assertTrue(dep.getIsFunction());
    }

    // =========================================================================
    // Single dependency from each expression source
    // =========================================================================

    @Test
    void function_call_in_shortcut_is_collected() {
        RFunction callee = functionInNamespace("com.example", "Evaluate");
        RFunction caller = functionInNamespace("com.example", "Caller");

        addShortcut(caller, symRefTo(callee));

        List<FunctionTemplateModel.DependencyModel> deps = collector.collect(caller);
        assertEquals(1, deps.size());
        assertEquals("evaluate", deps.get(0).getFieldName());
    }

    @Test
    void function_call_in_condition_is_collected() {
        RFunction callee = functionInNamespace("com.example", "Validate");
        RFunction caller = functionInNamespace("com.example", "Caller");

        addCondition(caller, symRefTo(callee));

        List<FunctionTemplateModel.DependencyModel> deps = collector.collect(caller);
        assertEquals(1, deps.size());
        assertEquals("validate", deps.get(0).getFieldName());
    }

    @Test
    void function_call_in_post_condition_is_collected() {
        RFunction callee = functionInNamespace("com.example", "PostCheck");
        RFunction caller = functionInNamespace("com.example", "Caller");

        addPostCondition(caller, symRefTo(callee));

        List<FunctionTemplateModel.DependencyModel> deps = collector.collect(caller);
        assertEquals(1, deps.size());
        assertEquals("postCheck", deps.get(0).getFieldName());
    }

    // =========================================================================
    // Deduplication
    // =========================================================================

    @Test
    void duplicate_calls_to_same_function_are_deduplicated() {
        RFunction callee = functionInNamespace("com.example", "CalculatePrice");
        RFunction caller = functionInNamespace("com.example", "Caller");

        // Reference the same function twice (e.g. called in two operations)
        addOperation(caller, symRefTo(callee));
        addOperation(caller, symRefTo(callee));

        List<FunctionTemplateModel.DependencyModel> deps = collector.collect(caller);
        assertEquals(1, deps.size(), "Same callee referenced twice should appear once");
        assertEquals("calculatePrice", deps.get(0).getFieldName());
    }

    @Test
    void same_callee_in_different_expression_sources_is_deduplicated() {
        RFunction callee = functionInNamespace("com.example", "Convert");
        RFunction caller = functionInNamespace("com.example", "Caller");

        addShortcut(caller, symRefTo(callee));
        addOperation(caller, symRefTo(callee));
        addCondition(caller, symRefTo(callee));
        addPostCondition(caller, symRefTo(callee));

        List<FunctionTemplateModel.DependencyModel> deps = collector.collect(caller);
        assertEquals(1, deps.size(), "Callee referenced in all 4 sources should appear once");
    }

    // =========================================================================
    // Sorting
    // =========================================================================

    @Test
    void multiple_dependencies_are_sorted_by_field_name() {
        RFunction zeta = functionInNamespace("com.example", "Zeta");
        RFunction alpha = functionInNamespace("com.example", "Alpha");
        RFunction mu = functionInNamespace("com.example", "Mu");
        RFunction caller = functionInNamespace("com.example", "Caller");

        // Add in reverse alphabetical order
        addOperation(caller, symRefTo(zeta));
        addOperation(caller, symRefTo(alpha));
        addOperation(caller, symRefTo(mu));

        List<FunctionTemplateModel.DependencyModel> deps = collector.collect(caller);
        assertEquals(3, deps.size());
        assertEquals("alpha", deps.get(0).getFieldName());
        assertEquals("mu", deps.get(1).getFieldName());
        assertEquals("zeta", deps.get(2).getFieldName());
    }

    // =========================================================================
    // Type information
    // =========================================================================

    @Test
    void fqn_reflects_callee_namespace_and_function_package() {
        RFunction callee = functionInNamespace("org.isda.common", "Qualify_Foo");
        RFunction caller = functionInNamespace("org.isda.common", "Caller");

        addOperation(caller, symRefTo(callee));

        List<FunctionTemplateModel.DependencyModel> deps = collector.collect(caller);
        assertEquals(1, deps.size());
        // Function classes live in a .functions sub-package
        assertEquals("org.isda.common.functions.Qualify_Foo", deps.get(0).getTypeFqn());
        assertEquals("Qualify_Foo", deps.get(0).getTypeName());
    }

    // =========================================================================
    // Non-function symbol references are ignored
    // =========================================================================

    @Test
    void non_function_symbol_reference_is_ignored() {
        RFunction caller = functionInNamespace("com.example", "Caller");

        // A symbol reference that resolves to a non-function AST node (e.g. a bool literal)
        RSymbolReference ref = new RSymbolReference();
        ref.setName("someParam");
        ref.setResolvedSymbol(new RBooleanLiteral()); // not an RFunction

        addOperation(caller, ref);

        List<FunctionTemplateModel.DependencyModel> deps = collector.collect(caller);
        assertTrue(deps.isEmpty(), "Non-function symbol should not be treated as a dependency");
    }

    @Test
    void unresolved_symbol_reference_is_ignored() {
        RFunction caller = functionInNamespace("com.example", "Caller");

        // Symbol reference with no resolved symbol
        RSymbolReference ref = new RSymbolReference();
        ref.setName("unknown");
        // resolvedSymbol stays null → symbol() returns empty Optional

        addOperation(caller, ref);

        List<FunctionTemplateModel.DependencyModel> deps = collector.collect(caller);
        assertTrue(deps.isEmpty(), "Unresolved symbol should not be treated as a dependency");
    }

    // =========================================================================
    // All four expression sources in one function
    // =========================================================================

    @Test
    void collects_deps_across_all_four_expression_sources() {
        RFunction alpha = functionInNamespace("com.example", "Alpha");
        RFunction beta = functionInNamespace("com.example", "Beta");
        RFunction gamma = functionInNamespace("com.example", "Gamma");
        RFunction delta = functionInNamespace("com.example", "Delta");
        RFunction caller = functionInNamespace("com.example", "BigCaller");

        addShortcut(caller, symRefTo(alpha));
        addOperation(caller, symRefTo(beta));
        addCondition(caller, symRefTo(gamma));
        addPostCondition(caller, symRefTo(delta));

        List<FunctionTemplateModel.DependencyModel> deps = collector.collect(caller);
        assertEquals(4, deps.size());
        // Sorted by field name: alpha, beta, delta, gamma
        assertEquals("alpha", deps.get(0).getFieldName());
        assertEquals("beta", deps.get(1).getFieldName());
        assertEquals("delta", deps.get(2).getFieldName());
        assertEquals("gamma", deps.get(3).getFieldName());
    }

    // =========================================================================
    // Deep feature call — DeepPathUtil dependency detection
    // =========================================================================

    /**
     * Build a data type in a namespace, returning both the model and data type.
     */
    private static RModel modelWithDataType(String namespace, String typeName) {
        RModel model = new RModel();
        model.setNamespace(namespace);

        RDataType dt = new RDataType();
        dt.setName(typeName);
        dt.setParent(model);
        model.rootElements().add(dt);
        return model;
    }

    /**
     * Build an {@link RAttribute} with a type call pointing to the given type name.
     */
    private static RAttribute attrWithType(String typeName) {
        RTypeCall tc = new RTypeCall();
        tc.setTypeName(typeName);
        RAttribute attr = new RAttribute();
        attr.setTypeCall(tc);
        return attr;
    }

    /**
     * Build a deep feature call where the receiver is a feature call with a resolved type.
     */
    private static RDeepFeatureCall deepCallWithReceiverType(String receiverTypeName, String featureName) {
        RFeatureCall receiverFc = new RFeatureCall();
        receiverFc.setReceiver(new RSymbolReference()); // placeholder receiver
        receiverFc.setFeatureName("field");
        receiverFc.setResolvedFeature(attrWithType(receiverTypeName));

        RDeepFeatureCall deepCall = new RDeepFeatureCall();
        deepCall.setReceiver(receiverFc);
        deepCall.setFeatureName(featureName);
        return deepCall;
    }

    @Test
    void deep_feature_call_with_known_receiver_type_collects_deep_path_util() {
        // Set up a workspace with the Asset data type
        RModel assetModel = modelWithDataType("cdm.base.staticdata.asset.common", "Asset");
        RWorkspace ws = RWorkspace.build(List.of(assetModel)).workspace();
        GeneratorModel gm = new GeneratorModel(ws);
        JavaTypeTranslator tt = new JavaTypeTranslator(new JavaTypeUtil());
        FunctionDependencyCollector coll = new FunctionDependencyCollector(gm, tt);

        RFunction caller = functionInNamespace("cdm.event.common", "Create_AssetTransfer");
        addOperation(caller, deepCallWithReceiverType("Asset", "identifier"));

        List<FunctionTemplateModel.DependencyModel> deps = coll.collect(caller);
        assertEquals(1, deps.size());
        FunctionTemplateModel.DependencyModel dep = deps.get(0);
        assertEquals("assetDeepPathUtil", dep.getFieldName());
        assertEquals("AssetDeepPathUtil", dep.getTypeName());
        assertEquals("cdm.base.staticdata.asset.common.util.AssetDeepPathUtil", dep.getTypeFqn());
        assertFalse(dep.getIsFunction(), "DeepPathUtil is not a function dependency");
    }

    @Test
    void deep_feature_call_without_receiver_type_emits_no_dependency() {
        // Deep call where receiver is a plain symbol ref (no resolved type)
        RDeepFeatureCall deepCall = new RDeepFeatureCall();
        deepCall.setReceiver(new RSymbolReference());
        deepCall.setFeatureName("price");

        RFunction caller = functionInNamespace("com.example", "Caller");
        addOperation(caller, deepCall);

        List<FunctionTemplateModel.DependencyModel> deps = collector.collect(caller);
        assertTrue(deps.isEmpty(), "No dependency should be emitted when receiver type is unknown");
    }

    @Test
    void deep_feature_call_deduplicates_same_deep_path_util() {
        // Two deep calls to the same receiver type should produce one dependency
        RModel assetModel = modelWithDataType("cdm.base.staticdata.asset.common", "Asset");
        RWorkspace ws = RWorkspace.build(List.of(assetModel)).workspace();
        GeneratorModel gm = new GeneratorModel(ws);
        JavaTypeTranslator tt = new JavaTypeTranslator(new JavaTypeUtil());
        FunctionDependencyCollector coll = new FunctionDependencyCollector(gm, tt);

        RFunction caller = functionInNamespace("cdm.event.common", "Caller");
        addOperation(caller, deepCallWithReceiverType("Asset", "identifier"));
        addOperation(caller, deepCallWithReceiverType("Asset", "taxonomy"));

        List<FunctionTemplateModel.DependencyModel> deps = coll.collect(caller);
        assertEquals(1, deps.size(), "Same receiver type should produce single DeepPathUtil dependency");
        assertEquals("assetDeepPathUtil", deps.get(0).getFieldName());
    }

    // =========================================================================
    // lowerCamelCase utility
    // =========================================================================

    @Test
    void lower_camel_case_already_lower() {
        assertEquals("foo", FunctionDependencyCollector.lowerCamelCase("foo"));
    }

    @Test
    void lower_camel_case_single_upper_char() {
        assertEquals("a", FunctionDependencyCollector.lowerCamelCase("A"));
    }

    @Test
    void lower_camel_case_pascal_case() {
        assertEquals("calculatePrice", FunctionDependencyCollector.lowerCamelCase("CalculatePrice"));
    }

    @Test
    void lower_camel_case_qualify_prefix() {
        assertEquals("qualify_Trade", FunctionDependencyCollector.lowerCamelCase("Qualify_Trade"));
    }

    @Test
    void lower_camel_case_empty_string() {
        assertEquals("", FunctionDependencyCollector.lowerCamelCase(""));
    }

    @Test
    void lower_camel_case_null_returns_null() {
        assertNull(FunctionDependencyCollector.lowerCamelCase(null));
    }

    // =========================================================================
    // Engine PR #6 facet (b) — bare-rule-then RRule dependency injection
    // =========================================================================

    /**
     * Build an {@link RRule} whose parent is a fresh {@link RModel} with the
     * given namespace (so {@code symbolId(fromRule(rule))} recovers the
     * namespace via the originRule back-pointer).
     */
    private static RRule ruleInNamespace(String namespace, String name) {
        RModel model = new RModel();
        model.setNamespace(namespace);

        RRule rule = new RRule();
        rule.setName(name);
        rule.setParent(model);
        model.rootElements().add(rule);
        return rule;
    }

    /** A bare no-arg {@link RSymbolReference} resolving to the given rule. */
    private static RSymbolReference symRefTo(RRule rule) {
        RSymbolReference ref = new RSymbolReference();
        ref.setName(rule.name());
        ref.setResolvedSymbol(rule);
        return ref;
    }

    @Test
    void bare_rule_reference_in_operation_emits_rule_dependency() {
        RRule rule = ruleInNamespace("drr.regulation.common.trade.datetime", "MaturityDateOfTheUnderlier");
        RFunction caller = functionInNamespace("drr.regulation.asic.rewrite.trade", "MaturityDateOfTheUnderlier");

        addOperation(caller, symRefTo(rule));

        // Non-colliding host name → simple name + import.
        List<FunctionTemplateModel.DependencyModel> deps =
                collector.collect(caller, "SomeUnrelatedHost");
        assertEquals(1, deps.size());
        FunctionTemplateModel.DependencyModel dep = deps.get(0);
        // Field name = lowerCamelCase of the rule name, WITH the generated
        // <Name>Rule suffix on the class (rule-origin routing).
        assertEquals("maturityDateOfTheUnderlierRule", dep.getFieldName());
        assertEquals("MaturityDateOfTheUnderlierRule", dep.getTypeName());
        assertEquals("drr.regulation.common.trade.datetime.reports.MaturityDateOfTheUnderlierRule",
                dep.getTypeFqn());
        assertTrue(dep.getIsFunction());
    }

    @Test
    void colliding_rule_dependency_renders_fqn_inline_with_null_typeFqn() {
        // Anchor case: the rule-target's generated class simple name collides
        // with the HOST class simple name (both <Name>Rule). The golden injects
        // the dependency FULLY QUALIFIED inline and emits NO import.
        RRule rule = ruleInNamespace("drr.regulation.common.trade.datetime", "MaturityDateOfTheUnderlier");
        RFunction caller = functionInNamespace("drr.regulation.asic.rewrite.trade", "MaturityDateOfTheUnderlier");

        addOperation(caller, symRefTo(rule));

        // Host simple name = the WITH-SUFFIX generated rule class name. PR #330: the
        // rule-dep collision check gained the function-path PACKAGE guard (a SELF /
        // same-package dependency keeps the simple name — the ReportingTimestamp
        // self-@Inject), so the pin threads the emitted host package like production
        // (FunctionGenerator passes importPackageName): CROSS-package + same simple
        // name still FQN-inlines.
        List<FunctionTemplateModel.DependencyModel> deps =
                collector.collect(caller, "MaturityDateOfTheUnderlierRule",
                        "MaturityDateOfTheUnderlierRule",
                        "drr.regulation.asic.rewrite.trade.reports");
        assertEquals(1, deps.size());
        FunctionTemplateModel.DependencyModel dep = deps.get(0);
        assertEquals("maturityDateOfTheUnderlierRule", dep.getFieldName());
        // typeName = FQN inline on collision.
        assertEquals("drr.regulation.common.trade.datetime.reports.MaturityDateOfTheUnderlierRule",
                dep.getTypeName());
        // typeFqn = null → import suppressed.
        assertNull(dep.getTypeFqn());
        assertTrue(dep.getIsFunction());
    }

    @Test
    void explicit_args_rule_reference_emits_rule_dependency() {
        // facet injectedRuleRef (PR #263): a rule reference carrying arguments (an
        // EXPLICIT-args cross-namespace rule invocation, e.g.
        // `then extract cde.valuation.ValuationMethod(GetValuation)`) is now injected
        // exactly like the no-arg bare-rule-then — the `args().isEmpty()` gate on the
        // RRule branch was dropped so the @Inject <Name>Rule field is registered for the
        // with-args shape too (ReferenceHandler's explicit-args receiver derives the same
        // field name). Still gated on the rule-family path (host gate != null).
        RRule rule = ruleInNamespace("drr.standards.iosco.cde.version3.valuation", "ValuationMethod");
        RFunction caller = functionInNamespace("drr.regulation.asic.rewrite.trade", "Caller");

        RSymbolReference ref = symRefTo(rule);
        ref.args().add(new RBooleanLiteral());
        addOperation(caller, ref);

        // Non-colliding host name → simple name + import.
        List<FunctionTemplateModel.DependencyModel> deps = collector.collect(caller, "SomeUnrelatedHost");
        assertEquals(1, deps.size(), "Explicit-args rule reference should inject the rule dependency");
        FunctionTemplateModel.DependencyModel dep = deps.get(0);
        assertEquals("valuationMethodRule", dep.getFieldName());
        assertEquals("ValuationMethodRule", dep.getTypeName());
        assertTrue(dep.getIsFunction());
    }

    @Test
    void explicit_args_rule_reference_injects_off_the_rule_path_too() {
        // facet injectRuleDepInFunctions (PR #332): the rule-family gate was DROPPED —
        // a null host gate (the plain FUNCTION emission path / 1-arg test callers) now
        // injects the rule dependency for a rule reference exactly like the rule path,
        // matching upstream JavaDependencyProvider (which adds every RosettaRule symbol
        // reference unconditionally). This pin CONVERTED from the pre-#332
        // explicit_args_rule_reference_is_ignored_off_the_rule_path decline lock.
        RRule rule = ruleInNamespace("drr.standards.iosco.cde.version3.valuation", "ValuationMethod");
        RFunction caller = functionInNamespace("drr.regulation.asic.rewrite.trade", "Caller");

        RSymbolReference ref = symRefTo(rule);
        ref.args().add(new RBooleanLiteral());
        addOperation(caller, ref);

        List<FunctionTemplateModel.DependencyModel> deps = collector.collect(caller);
        assertEquals(1, deps.size(), "Off the rule path the rule dep is now injected (PR #332)");
        assertEquals("valuationMethodRule", deps.get(0).getFieldName());
        assertEquals("ValuationMethodRule", deps.get(0).getTypeName());
    }

    @Test
    void duplicate_bare_rule_references_are_deduplicated() {
        RRule rule = ruleInNamespace("drr.regulation.common", "DefaultPercentageToDecimal");
        RFunction caller = functionInNamespace("drr.regulation.asic", "Caller");

        addOperation(caller, symRefTo(rule));
        addOperation(caller, symRefTo(rule));

        List<FunctionTemplateModel.DependencyModel> deps = collector.collect(caller, "Caller");
        assertEquals(1, deps.size(), "Same rule referenced twice should appear once");
        assertEquals("defaultPercentageToDecimalRule", deps.get(0).getFieldName());
    }

    @Test
    void mixed_function_and_rule_dependencies_are_sorted_together() {
        // Anchor shape: one RFunction dep (isAllowableActionForASIC) + one RRule
        // dep (maturityDateOfTheUnderlierRule); sorted by field name.
        RFunction fn = functionInNamespace("drr.regulation.asic.rewrite.trade", "IsAllowableActionForASIC");
        RRule rule = ruleInNamespace("drr.regulation.common.trade.datetime", "MaturityDateOfTheUnderlier");
        RFunction caller = functionInNamespace("drr.regulation.asic.rewrite.trade", "MaturityDateOfTheUnderlier");

        addOperation(caller, symRefTo(fn));
        addOperation(caller, symRefTo(rule));

        // PR #330: thread the emitted host package (see the colliding-rule pin above).
        List<FunctionTemplateModel.DependencyModel> deps =
                collector.collect(caller, "MaturityDateOfTheUnderlierRule",
                        "MaturityDateOfTheUnderlierRule",
                        "drr.regulation.asic.rewrite.trade.reports");
        assertEquals(2, deps.size());
        // "isAllowableActionForASIC" < "maturityDateOfTheUnderlierRule"
        assertEquals("isAllowableActionForASIC", deps.get(0).getFieldName());
        assertEquals("maturityDateOfTheUnderlierRule", deps.get(1).getFieldName());
        // The function dep keeps simple name + import; the colliding rule dep is FQN inline + null typeFqn.
        assertEquals("IsAllowableActionForASIC", deps.get(0).getTypeName());
        assertNotNull(deps.get(0).getTypeFqn());
        assertNull(deps.get(1).getTypeFqn());
    }

    @Test
    void function_path_null_host_injects_rule_dependency_too() {
        // facet injectRuleDepInFunctions (PR #332): the rule-family gate
        // (hostSimpleName != null) was DROPPED — a plain function (the 1-arg
        // collect(func) path, null host gate) that references a rule now gains the
        // @Inject <Name>Rule field, matching upstream JavaDependencyProvider (every
        // RosettaRule symbol reference injects, no host-kind gate). A resolution
        // failure still degrades to skipping via injectRuleDependency's catch, so the
        // old iso20022/rune-fpml throw concern cannot fire. This pin CONVERTED from
        // the pre-#332 function_path_null_host_does_not_inject_rule_dependency
        // decline lock (Engine PR #6 review-workflow finding, superseded).
        RRule rule = ruleInNamespace("drr.regulation.common.trade.datetime", "MaturityDateOfTheUnderlier");
        RFunction caller = functionInNamespace("drr.regulation.asic.rewrite.trade", "MaturityDateOfTheUnderlier");

        addOperation(caller, symRefTo(rule));

        List<FunctionTemplateModel.DependencyModel> deps = collector.collect(caller);
        assertEquals(1, deps.size(),
                "A plain function referencing a rule now injects the rule dependency (PR #332)");
        assertEquals("maturityDateOfTheUnderlierRule", deps.get(0).getFieldName());
        assertEquals("MaturityDateOfTheUnderlierRule", deps.get(0).getTypeName());
    }

    // =========================================================================
    // FUNCTION-dependency name collision (function-path facet)
    // =========================================================================

    @Test
    void colliding_function_dependency_renders_fqn_inline_with_null_typeFqn() {
        // Anchor case: an iosco CDE version2 function injects the identically-named
        // version1 function (different package, same simple name). The simple name
        // would shadow the enclosing class, so the golden FQN-qualifies the @Inject
        // field type inline and emits NO import.
        RFunction callee = functionInNamespace("drr.standards.iosco.cde.version1.party", "Direction2");
        RFunction caller = functionInNamespace("drr.standards.iosco.cde.version2.party", "Direction2");
        addOperation(caller, symRefTo(callee));

        // Function-path call: ruleGate=null (no rule injection), but the actual host
        // class identity (simple name + emitted package) IS supplied for collision.
        List<FunctionTemplateModel.DependencyModel> deps = collector.collect(
                caller, null, "Direction2", "drr.standards.iosco.cde.version2.party.functions");
        assertEquals(1, deps.size());
        FunctionTemplateModel.DependencyModel dep = deps.get(0);
        assertEquals("direction2", dep.getFieldName());
        // typeName = FQN inline on collision.
        assertEquals("drr.standards.iosco.cde.version1.party.functions.Direction2", dep.getTypeName());
        // typeFqn = null → import suppressed.
        assertNull(dep.getTypeFqn());
        assertTrue(dep.getIsFunction());
    }

    @Test
    void same_package_same_name_function_dependency_keeps_simple_name() {
        // Package guard: a dependency that shares the host's simple name but lives in
        // the SAME package is NOT a collision (no import is needed — same package — so
        // the plain simple name is correct and its same-package import is suppressed
        // downstream by ImportCollector). Must stay simple-name + non-null typeFqn.
        RFunction callee = functionInNamespace("com.example.same", "Direction2");
        RFunction caller = functionInNamespace("com.example.same", "Direction2");
        addOperation(caller, symRefTo(callee));

        List<FunctionTemplateModel.DependencyModel> deps = collector.collect(
                caller, null, "Direction2", "com.example.same.functions");
        assertEquals(1, deps.size());
        FunctionTemplateModel.DependencyModel dep = deps.get(0);
        assertEquals("Direction2", dep.getTypeName());
        assertEquals("com.example.same.functions.Direction2", dep.getTypeFqn());
    }

    @Test
    void function_dependency_collision_check_disabled_without_host_package() {
        // BC guard: the 2-arg overload (and any null-package caller) disables the
        // function-dependency collision check, preserving the pre-facet behaviour
        // (simple name + import) even when the simple names happen to match.
        RFunction callee = functionInNamespace("drr.standards.iosco.cde.version1.party", "Direction2");
        RFunction caller = functionInNamespace("drr.standards.iosco.cde.version2.party", "Direction2");
        addOperation(caller, symRefTo(callee));

        List<FunctionTemplateModel.DependencyModel> deps = collector.collect(caller, "Direction2");
        assertEquals(1, deps.size());
        FunctionTemplateModel.DependencyModel dep = deps.get(0);
        assertEquals("Direction2", dep.getTypeName());
        assertEquals("drr.standards.iosco.cde.version1.party.functions.Direction2", dep.getTypeFqn());
    }
}
