package com.regnosys.rosetta.generator.java.expression.handlers;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.references.RSuperCall;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.unary.RExtractExpr;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompilerTest;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.statement.JavaStatementList;
import com.regnosys.rosetta.generator.java.statement.builder.JavaBlockBuilder;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.rosetta.util.types.JavaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ReferenceHandlerTest {

    private ReferenceHandler handler;
    private ExpressionCompiler compiler;
    private ExpressionContext ctx;

    @BeforeEach
    void setUp() {
        handler  = new ReferenceHandler();
        compiler = new ExpressionCompiler();
        // Seed the disguised-feature receiver name so the navigation lambda
        // variable collides and emits the legacy `_`-prefixed form, mirroring a
        // real function body where the input parameter (`trade`) is registered in
        // the body scope (Phase X1 scope-faithful lambda naming; see
        // FunctionExpressionRenderer.createScope(RExpression)).
        var scope = ExpressionCompilerTest.createTestScope();
        scope.createUniqueIdentifier("trade");
        ctx      = ExpressionContext.of(null, scope);
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private String render(JavaStatementBuilder result) {
        assertInstanceOf(JavaExpression.class, result);
        // facet lambdaNaming M3/M4 (PR #329): nav lambda params render as deferred
        // sentinels (registerDeferredLambdaParam) resolved at finalization — resolve
        // here so the pins keep asserting the actual names.
        return ctx.scope().resolveDeferredCoercionNames(
                ((JavaExpression) result).renderToString());
    }

    private RIntLiteral intLiteral(int value) {
        var lit = new RIntLiteral();
        lit.setValue(value);
        return lit;
    }

    /**
     * Create a fully-resolved enum-value reference. In production, the global
     * resolution pass sets {@code resolvedEnum} only when the name is a real
     * enumeration — when {@code resolvedEnum} is empty, the node is treated as
     * a disguised feature call (grammar ambiguity). Tests that want to
     * exercise the "real enum reference" path must therefore wire the resolved
     * enum explicitly.
     */
    private REnumValueRef resolvedEnumRef(String enumName, String valueName) {
        var ref = new REnumValueRef();
        ref.setEnumName(enumName);
        ref.setValueName(valueName);
        var mockEnum = new REnumeration();
        mockEnum.setName(enumName);
        ref.setResolvedEnum(mockEnum);
        return ref;
    }

    /**
     * Build an {@link RFunction} with the given input names (no types — tests
     * only need the declaration order of input names for rendering).
     */
    private RFunction functionWithInputs(String... inputNames) {
        var func = new RFunction();
        for (String n : inputNames) {
            var attr = new RAttribute();
            attr.setName(n);
            func.inputs().add(attr);
        }
        return func;
    }

    /**
     * Build an alias-resolved {@link RSymbolReference}: the reference's
     * {@code resolvedSymbol} is an {@link RShortcut} and its parent is wired to
     * the supplied enclosing function so {@code HandlerHelper.findEnclosingFunction}
     * can walk up and find the inputs.
     */
    private RSymbolReference aliasRef(String aliasName, RFunction enclosing) {
        var shortcut = new RShortcut();
        shortcut.setName(aliasName);
        var ref = new RSymbolReference();
        ref.setName(aliasName);
        ref.setResolvedSymbol(shortcut);
        if (enclosing != null) {
            ref.setParent(enclosing);
        }
        return ref;
    }

    // =========================================================================
    // Symbol reference — simple variable (no args)
    // =========================================================================

    @Test
    void symbol_reference_no_args_wraps_in_MapperS_of() {
        var expr = new RSymbolReference();
        expr.setName("trade");

        assertEquals("MapperS.of(trade)", render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // Symbol reference — alias (RShortcut) invocation
    // =========================================================================

    @Test
    void alias_reference_emits_method_call_with_single_input() {
        // Golden: relativeDate(adjustableOrAdjustedOrRelativeDate)
        var enclosing = functionWithInputs("adjustableOrAdjustedOrRelativeDate");
        var expr = aliasRef("relativeDate", enclosing);

        assertEquals(
            "relativeDate(adjustableOrAdjustedOrRelativeDate)",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void alias_reference_emits_method_call_with_multiple_inputs_in_declaration_order() {
        // Use names that would REORDER alphabetically so the test actually pins
        // declaration order (not accidental alphabetical coincidence).
        var enclosing = functionWithInputs("zebra", "apple", "mango");
        var expr = aliasRef("myAlias", enclosing);

        assertEquals(
            "myAlias(zebra, apple, mango)",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void alias_reference_emits_method_call_with_no_inputs() {
        var enclosing = functionWithInputs();
        var expr = aliasRef("noArgAlias", enclosing);

        assertEquals(
            "noArgAlias()",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void alias_reference_without_enclosing_function_degrades() {
        // No parent wired — findEnclosingFunction returns null, and renderEnclosingInputs
        // returns an empty string rather than crashing. Partial-resolution scenarios
        // must not NPE during codegen.
        var expr = aliasRef("looseAlias", null);

        assertEquals(
            "looseAlias()",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void variable_reference_still_wraps_in_MapperS_of() {
        // Regression guard: bare RSymbolReference with no resolved symbol still
        // emits MapperS.of(trade) — the alias path must only trigger when the
        // symbol resolves to an RShortcut.
        var expr = new RSymbolReference();
        expr.setName("trade");

        assertEquals("MapperS.of(trade)", render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void function_call_path_still_uses_evaluate() {
        // Regression guard: symbol ref with args still emits the function-call
        // pattern MapperS.of(funcName.evaluate(arg)). Alias dispatch short-circuits
        // only when the resolved symbol is an RShortcut.
        var expr = new RSymbolReference();
        expr.setName("myFunction");
        expr.args().add(intLiteral(42));

        assertEquals(
            "MapperS.of(myFunction.evaluate(42))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void alias_reference_with_args_throws_illegal_state() {
        // Aliases are name-only references to RShortcut — a non-empty args list
        // alongside RShortcut resolution indicates a malformed AST. Fail loud
        // so the bug surfaces at codegen time, not downstream when invalid Java
        // fails to compile.
        var enclosing = functionWithInputs("trade");
        var expr = aliasRef("someAlias", enclosing);
        expr.args().add(intLiteral(1));

        var thrown = assertThrows(IllegalStateException.class,
                () -> handler.handle(expr, ctx, compiler));
        assertTrue(thrown.getMessage().contains("someAlias"),
                "exception message must name the offending alias; got: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains("1 argument"),
                "exception message must report argument count; got: " + thrown.getMessage());
    }

    @Test
    void alias_reference_falls_back_to_shortcut_list_when_symbol_unresolved() {
        // Belt-and-braces path: when cross-reference resolution fails to wire
        // `resolvedSymbol` but the name matches an RShortcut in the enclosing
        // function, the handler must still emit the alias method-call pattern
        // (not the broken MapperS.of(aliasName) variable fallback).
        var enclosing = functionWithInputs("trade");
        var shortcut = new RShortcut();
        shortcut.setName("myAlias");
        enclosing.shortcuts().add(shortcut);

        var expr = new RSymbolReference();
        expr.setName("myAlias");
        // Deliberately NOT calling setResolvedSymbol — simulates a partial/
        // disabled cross-ref pass. The name lookup via enclosing.shortcuts()
        // is the last-resort identification.
        expr.setParent(enclosing);

        assertEquals(
            "myAlias(trade)",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // Symbol reference — function call with args
    // =========================================================================

    @Test
    void symbol_reference_with_args_generates_evaluate_call() {
        var expr = new RSymbolReference();
        expr.setName("myFunction");
        expr.args().add(intLiteral(1));
        expr.args().add(intLiteral(2));

        // Arguments are unwrapped: evaluate() takes raw Java types, not Mapper-wrapped values
        assertEquals(
            "MapperS.of(myFunction.evaluate(1, 2))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void symbol_reference_with_variable_arg_unwraps_MapperS() {
        // Golden: filterQuantityByCurrencyExists.evaluate(quantity)
        var arg = new RSymbolReference();
        arg.setName("quantity");

        var expr = new RSymbolReference();
        expr.setName("filterQuantityByCurrencyExists");
        expr.args().add(arg);

        // Variable "quantity" compiles to MapperS.of(quantity), which is unwrapped to "quantity"
        assertEquals(
            "MapperS.of(filterQuantityByCurrencyExists.evaluate(quantity))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void symbol_reference_with_closure_param_arg_appends_get() {
        // facet mapitem_ctor_wrap (mechanism 4): a DECLARED closure param is a
        // Mapper-typed Java variable at runtime (the map*/filter* lambda
        // signatures bind MapperS<T> params), so an evaluate arg that is the
        // BARE param name collapses wrapperToItem with `.get()` — golden
        // `compareQuantityByUnitOfAmount.evaluate(…, unitOfAmount.get())`
        // (CompareTradeLot).
        // Gated by isEnclosingClosureParam (the #168 arm-B2 walk): a raw
        // function-input arg (the pin above) keeps the bare structural unwrap.
        var arg = new RSymbolReference();
        arg.setName("unitOfAmount");

        var expr = new RSymbolReference();
        expr.setName("myFunction");
        expr.args().add(arg);
        arg.setParent(expr);

        // the call sits inside an inline function DECLARING the param
        var lambda = new RInlineFunction();
        lambda.paramNames().add("unitOfAmount");
        lambda.setBody(expr);
        expr.setParent(lambda);

        assertEquals(
            "MapperS.of(myFunction.evaluate(unitOfAmount.get()))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void symbol_reference_with_enum_arg_unwraps_directly() {
        // Golden: filterQuantityByFinancialUnit.evaluate(quantity, FinancialUnitEnum.SHARE)
        var arg1 = new RSymbolReference();
        arg1.setName("quantity");

        // Resolved enum — production linker sets resolvedEnum when the name
        // genuinely refers to an enumeration, unlocking dotted-constant form.
        var arg2 = resolvedEnumRef("FinancialUnitEnum", "SHARE");

        var expr = new RSymbolReference();
        expr.setName("filterQuantityByFinancialUnit");
        expr.args().add(arg1);
        expr.args().add(arg2);

        // Variable unwraps from MapperS.of(quantity) → quantity.
        // Enum constant "FinancialUnitEnum.SHARE" is detected as a dotted enum
        // constant and passes through unchanged (no .get() appended — enum
        // constants have no .get() method).
        assertEquals(
            "MapperS.of(filterQuantityByFinancialUnit.evaluate(quantity, FinancialUnitEnum.SHARE))",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // Symbol reference — explicit function-call receiver casing
    // (engine PR #11 / facet F3, completeness-ledger Tier-3 #20)
    // =========================================================================

    @Test
    void explicit_call_receiver_is_lowerCamel_of_resolved_function_name() {
        // F3: the fork rendered the explicit-args call receiver as the raw
        // UpperCamel reference name; the golden uses the lowerCamel injected
        // instance — matching the @Inject field FunctionDependencyCollector
        // registers for the same function. The receiver must derive from the
        // RESOLVED RFunction's simple name. Grounded in drr/6.34.1 facet-dump
        // (golden `extractPartyResponsibleForReportingIdentifier.evaluate(...)`
        // vs fork `ExtractPartyResponsibleForReportingIdentifier.evaluate(...)`).
        var callee = new RFunction();
        callee.setName("ExtractPartyResponsibleForReportingIdentifier");
        var expr = new RSymbolReference();
        expr.setName("ExtractPartyResponsibleForReportingIdentifier");
        expr.setResolvedSymbol(callee);
        expr.args().add(intLiteral(1));

        assertEquals(
            "MapperS.of(extractPartyResponsibleForReportingIdentifier.evaluate(1))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void explicit_call_receiver_uses_simple_name_not_qualified_reference_name() {
        // The crux of why Tier-3 #20 was deferred: for a cross-namespace
        // function the reference name is namespace-QUALIFIED, so a naive
        // lowerCamelCase(expr.name()) would leave
        // "cde.collateral.InitialMargin..." untouched (its first char is the
        // already-lowercase 'c') — a reference to an undeclared identifier. The
        // golden uses the lowerCamel of the resolved RFunction's SIMPLE name.
        // Grounded in drr/6.34.1 facet-dump line 519/522 (fork
        // `cde.collateral.InitialMarginPostedByReportingCounterpartyPreHaircut`
        // vs golden `initialMarginPostedByReportingCounterpartyPreHaircut`).
        var callee = new RFunction();
        callee.setName("InitialMarginPostedByReportingCounterpartyPreHaircut");
        var expr = new RSymbolReference();
        expr.setName("cde.collateral.InitialMarginPostedByReportingCounterpartyPreHaircut");
        expr.setResolvedSymbol(callee);
        expr.args().add(intLiteral(0));

        assertEquals(
            "MapperS.of(initialMarginPostedByReportingCounterpartyPreHaircut.evaluate(0))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void explicit_call_receiver_lowercases_only_first_char() {
        // lowerCamelCase lowercases ONLY the first character, so an
        // acronym-leading name keeps its remaining caps. Golden
        // `aPI_AnnaDsbRetrieveUpi` (drr/6.34.1 facet-dump line 26) <- fork
        // `API_AnnaDsbRetrieveUpi`. Pins lowerCamelCase semantics on the call
        // receiver so a future "smart" casing change cannot silently regress it.
        var callee = new RFunction();
        callee.setName("API_AnnaDsbRetrieveUpi");
        var expr = new RSymbolReference();
        expr.setName("API_AnnaDsbRetrieveUpi");
        expr.setResolvedSymbol(callee);
        expr.args().add(intLiteral(7));

        assertEquals(
            "MapperS.of(aPI_AnnaDsbRetrieveUpi.evaluate(7))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void explicit_call_receiver_falls_back_to_raw_name_when_symbol_unresolved() {
        // Regression guard: when the symbol does NOT resolve to an RFunction
        // (partial/disabled resolution), the receiver falls back to the raw
        // reference name — preserving the pre-fix behaviour the other with-args
        // tests rely on (they wire no resolvedSymbol). A name-only ref with no
        // resolved function cannot be re-cased safely.
        var expr = new RSymbolReference();
        expr.setName("alreadyLowerFunc");
        expr.args().add(intLiteral(3));
        // Deliberately NOT calling setResolvedSymbol.

        assertEquals(
            "MapperS.of(alreadyLowerFunc.evaluate(3))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void symbol_reference_with_args_propagates_staticWildcardImports_into_inner_call() {
        // Copilot round-7 finding: the function-call path at
        // handle(RSymbolReference) L155-158 was dropping staticWildcardImports
        // from argument builders by synthesising innerCall via the 3-arg
        // JavaExpression.from factory. Once handlers populate
        // staticWildcardImports (C3a.4.b/d), those imports must propagate
        // through the evaluate(...) emission and survive the outer
        // MapperS.of wrap (which already preserves them per
        // JavaExpression.wrappedInMapperSOf L143).
        //
        // To test the CONSTRUCTION site (not the unwrap helper — already
        // covered by unwrapForEvaluateArg_preserves_staticWildcardImports_*),
        // we inject an argument whose compiled result carries a
        // staticWildcardImport via a test-scoped compiler override.
        ExpressionCompiler wildcardCompiler = new ExpressionCompiler() {
            @Override
            public JavaStatementBuilder compile(RExpression expr, JavaType expectedType,
                                                com.regnosys.rosetta.generator.java.scoping.JavaStatementScope scope) {
                // Return a bare expression carrying a static wildcard — mirrors
                // what ComparisonHandler will emit in C3a.4.b for a
                // comparison-returning argument (e.g. areEqual(...)).
                return JavaExpression.from(
                        "areEqual(left, right, CardinalityOperator.All)",
                        null,
                        Set.of(HandlerHelper.CARDINALITY_OPERATOR),
                        Set.of(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE));
            }
        };

        var arg = new RSymbolReference();
        arg.setName("someComparison");
        var expr = new RSymbolReference();
        expr.setName("consumer");
        expr.args().add(arg);

        JavaStatementBuilder result = handler.handle(expr, ctx, wildcardCompiler);
        // The outer MapperS.of wrap preserves staticWildcardImports from inner
        // (verified by JavaExpressionStaticImportsTest). Assertion: the
        // wildcard set on the final builder contains what the arg contributed.
        assertTrue(result.getStaticWildcardImports()
                        .contains(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE),
                "handle(RSymbolReference) function-call path must union "
                        + "staticWildcardImports from argument builders into innerCall "
                        + "so they survive wrappedInMapperSOf — otherwise "
                        + "EXPRESSION_OPERATORS_NULL_SAFE is silently dropped for "
                        + "any function call whose args include "
                        + "comparison/existence emissions.");
    }

    // =========================================================================
    // unwrapForEvaluateArg — unit tests
    // =========================================================================
    //
    // v6.2 C3a.2: helper signature changed from (String) to
    // (JavaStatementBuilder) to carry refs structurally. The five legacy
    // string tests below were rewritten against the new signature. Each
    // test pins a specific branch of the dispatch ladder:
    //   1. structural wrap-unwrap (wrappedInMapperSOf → unwrapToBuilder)
    //   2. fall-through .get() append with refs preservation
    //   3. null passthrough
    //   4. enum-constant witness passthrough (PR #611)
    // The legacy string-scan strip that was branch 2 is GONE at facet mapperWrapPrefix
    // (v3.1 C2d retirement family 8, PR #615) — dead at 0 of 320,024 census arrivals. The two
    // tests that pinned it are RETARGETED to the branch the same shapes now take, so the
    // deletion has a unit-level decline lock rather than a silent gap.

    @Test
    void unwrapForEvaluateArg_strips_MapperS_of_wrapper_structural() {
        // Structural path: inner with empty refs → wrappedInMapperSOf adds
        // MAPPER_S → unwrap drops MAPPER_S atomically.
        JavaExpression inner =
                JavaExpression.from("quantity", null, Set.of());
        JavaExpression wrapped = JavaExpression.wrappedInMapperSOf(inner);
        JavaStatementBuilder unwrapped =
                ReferenceHandler.unwrapForEvaluateArg(wrapped);
        assertEquals("quantity", ((JavaExpression) unwrapped).renderToString());
        assertEquals(Set.of(), unwrapped.getRefs(),
                "structural unwrap must drop MAPPER_S atomically with source strip");
    }

    @Test
    void unwrapForEvaluateArg_raw_MapperS_of_text_falls_through_after_the_strip_deletion() {
        // facet mapperWrapPrefix (v3.1 C2d retirement family 8, PR #615): the legacy string-scan
        // strip is DELETED (dead at 0 of 320,024 census arrivals — the branch was reachable only
        // with an EMPTY marker, and marker-less traffic here is bare or chained, never a balanced
        // whole wrap). This test is RETARGETED, not removed: it now pins the post-deletion
        // contract for the shape the branch used to claim — a RAW `MapperS.of(...)` text with no
        // marker takes the Mapper fall-through and keeps its refs verbatim. It is the unit-level
        // decline lock for the deletion; the corpus-level one is the census zero.
        JavaExpression wrapped = JavaExpression.from(
                "MapperS.of(trade.getPrice())", null,
                Set.of(HandlerHelper.MAPPER_S));
        JavaStatementBuilder unwrapped =
                ReferenceHandler.unwrapForEvaluateArg(wrapped);
        assertEquals("MapperS.of(trade.getPrice()).get()",
                ((JavaExpression) unwrapped).renderToString(),
                "an unmarked MapperS.of text must take the .get() fall-through, not a text strip");
        assertEquals(Set.of(HandlerHelper.MAPPER_S), unwrapped.getRefs(),
                "the fall-through preserves refs verbatim — the source still names MapperS");
    }

    @Test
    void unwrapForEvaluateArg_adds_get_for_non_mapper() {
        // Fall-through: non-wrap, non-dotted-enum expression → ".get()" suffix
        // with refs preserved verbatim (MAPPER_S stays because the final
        // source still contains MapperS.of via chain).
        JavaExpression bare = JavaExpression.from("someExpr", null,
                Set.of(HandlerHelper.MAPPER_S));
        JavaStatementBuilder unwrapped =
                ReferenceHandler.unwrapForEvaluateArg(bare);
        assertEquals("someExpr.get()",
                ((JavaExpression) unwrapped).renderToString());
        assertEquals(Set.of(HandlerHelper.MAPPER_S), unwrapped.getRefs(),
                "fall-through .get() branch must preserve inbound refs");
    }

    @Test
    void unwrapForEvaluateArg_null_passes_through() {
        // null builder → synthetic "null" expression with empty refs.
        JavaStatementBuilder fromNull = ReferenceHandler.unwrapForEvaluateArg(null);
        assertEquals("null", ((JavaExpression) fromNull).renderToString());
        assertEquals(Set.of(), fromNull.getRefs());

        // "null"-rendering expression → passthrough (returns same builder).
        JavaExpression nullExpr = JavaExpression.from("null", null, Set.of());
        JavaStatementBuilder fromNullExpr =
                ReferenceHandler.unwrapForEvaluateArg(nullExpr);
        assertSame(nullExpr, fromNullExpr,
                "'null'-rendering expression must passthrough unchanged");
    }

    @Test
    void unwrapForEvaluateArg_integer_literal_unwraps_through_the_factory_marker() {
        // facet mapperWrapPrefix (v3.1 C2d retirement family 8, PR #615): LiteralHandler is
        // MIGRATED — every scalar-literal compile goes through wrappedInMapperSOf — so the
        // integer literal this test names arrives with the MARKER and unwraps STRUCTURALLY,
        // dropping MAPPER_S atomically. That is the live path, and it is what this test now pins.
        JavaExpression literalWrap =
                JavaExpression.wrappedInMapperSOf(JavaExpression.from("42", null, Set.of()));
        JavaStatementBuilder unwrapped =
                ReferenceHandler.unwrapForEvaluateArg(literalWrap);
        assertEquals("42", ((JavaExpression) unwrapped).renderToString());
        assertEquals(Set.of(), unwrapped.getRefs(),
                "the structural unwrap drops MAPPER_S atomically with the source strip");

        // The negative twin: the SAME text built WITHOUT the marker (the shape the deleted
        // string-scan branch used to claim) takes the fall-through instead — 0 of 320,024 census
        // arrivals reached the strip, so nothing in the corpus relies on the old answer.
        JavaExpression rawText = JavaExpression.from(
                "MapperS.of(42)", null, Set.of(HandlerHelper.MAPPER_S));
        assertEquals("MapperS.of(42).get()",
                ((JavaExpression) ReferenceHandler.unwrapForEvaluateArg(rawText)).renderToString(),
                "an unmarked MapperS.of text is no longer text-stripped");
    }

    @Test
    void unwrapForEvaluateArg_enum_constant_witness_passthrough() {
        // An enum constant BY ITS PRODUCER'S WITNESS → passthrough (enums have no .get()).
        // PR #611 (C2d family 4): the branch reads JavaExpression.enumConstant's witness,
        // never the dotted spelling.
        JavaExpression enumExpr = JavaExpression.enumConstant(
                "FinancialUnitEnum.SHARE", null, Set.of(), Set.of());
        JavaStatementBuilder unwrapped =
                ReferenceHandler.unwrapForEvaluateArg(enumExpr);
        assertSame(enumExpr, unwrapped,
                "a witnessed enum constant must passthrough unchanged");
    }

    @Test
    void unwrapForEvaluateArg_unwitnessed_dotted_spelling_is_not_an_enum_constant() {
        // The negative twin (PR #611): the SAME spelling built WITHOUT the producer's witness
        // is an ordinary expression and takes the Mapper fall-through — the text no longer
        // decides. (Before #611 this rendered bare, by the dotted-shape test.)
        JavaExpression plain = JavaExpression.from("FinancialUnitEnum.SHARE", null, Set.of());
        JavaStatementBuilder unwrapped = ReferenceHandler.unwrapForEvaluateArg(plain);
        assertEquals("FinancialUnitEnum.SHARE.get()", ((JavaExpression) unwrapped).renderToString(),
                "an unwitnessed dotted spelling must NOT be treated as an enum constant");
    }

    @Test
    void unwrapForEvaluateArg_preserves_staticWildcardImports_for_an_unmarked_whole_wrap() {
        // Pins the Copilot round-6 finding: static wildcards (e.g.
        // EXPRESSION_OPERATORS_NULL_SAFE from ComparisonHandler/ExistenceHandler
        // emissions in C3a.4.b/d) travel with the builder through unwrap.
        // facet mapperWrapPrefix (PR #615): the branch this arrival used to take (the legacy
        // MapperS.of string-scan strip) is DELETED, so it now travels the .get() fall-through —
        // the invariant is the same one and it must hold on whichever branch carries the shape.
        // NAMED FOR THE SHAPE, NOT THE DEAD BRANCH (the spec-compliance review's NIT-2): the test
        // was `..._across_legacy_string_scan`, which named a branch that no longer exists. The
        // shape is what it pins — an UNMARKED whole `MapperS.of(...)` wrap (built through
        // JavaExpression.from, so unwrapToBuilder() is EMPTY), which is exactly the population the
        // deleted strip used to claim and which the census measured at 0 whole-wrap matches of
        // 320,024. Distinct from its `..._across_get_fallthrough` sibling, whose input is not a
        // wrap at all: both now leave by the same branch, and that is the point.
        JavaExpression wrapped = JavaExpression.from(
                "MapperS.of(areEqual(trade.getPrice(), zero, CardinalityOperator.All))",
                null, Set.of(HandlerHelper.MAPPER_S),
                Set.of(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE));
        JavaStatementBuilder unwrapped =
                ReferenceHandler.unwrapForEvaluateArg(wrapped);
        assertEquals(Set.of(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE),
                unwrapped.getStaticWildcardImports(),
                "an unmarked whole MapperS.of(...) wrap must preserve staticWildcardImports"
                + " through the fall-through the deleted strip used to precede");
    }

    @Test
    void unwrapForEvaluateArg_preserves_staticWildcardImports_across_get_fallthrough() {
        // Pins the Copilot round-6 finding: the .get() fall-through branch must
        // preserve staticWildcardImports. Same invariant as above but for the
        // non-wrap fall-through path.
        JavaExpression bare = JavaExpression.from(
                "areEqual(trade.getPrice(), zero, CardinalityOperator.All)",
                null, Set.of(HandlerHelper.CARDINALITY_OPERATOR),
                Set.of(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE));
        JavaStatementBuilder unwrapped =
                ReferenceHandler.unwrapForEvaluateArg(bare);
        assertEquals(Set.of(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE),
                unwrapped.getStaticWildcardImports(),
                "fall-through .get() branch must preserve staticWildcardImports");
    }

    @Test
    void unwrapForEvaluateArg_throws_on_non_javaexpression_builder() {
        // Defensive guard: the dispatch ladder ends with an IllegalStateException
        // for any non-null, non-JavaExpression builder. This path is
        // structurally unreachable from current call sites (compile() returns
        // JavaStatementBuilder; ReferenceHandler.handle filters via
        // `instanceof JavaExpression` before calling unwrapForEvaluateArg),
        // but the guard exists to surface future drift loudly rather than
        // silently losing refs. JavaBlockBuilder is a convenient non-
        // JavaExpression JavaStatementBuilder for this probe.
        JavaExpression endingExpr = JavaExpression.from("x", null, Set.of());
        JavaBlockBuilder block =
                new JavaBlockBuilder(JavaStatementList.of(), endingExpr);
        assertThrows(IllegalStateException.class,
                () -> ReferenceHandler.unwrapForEvaluateArg(block),
                "non-JavaExpression builder must throw IllegalStateException");
    }

    // =========================================================================
    // Enum value reference
    // =========================================================================

    @Test
    void enum_value_ref_generates_dotted_constant() {
        // Resolved enum — production linker sets resolvedEnum when the name
        // refers to a real enumeration. Without resolution, the node is treated
        // as a disguised feature call (grammar ambiguity).
        var expr = resolvedEnumRef("CurrencyCodeEnum", "EUR");

        assertEquals("CurrencyCodeEnum.EUR", render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void enum_value_ref_converts_camelcase_value_to_java_constant() {
        // A real enum value reference renders the Java enum CONSTANT, which is the
        // converted value name (EnumHelper.convertValue / formatEnumName: camelCase →
        // UPPER_SNAKE), NOT the raw rosetta source value name. The golden CompareOp
        // enum declares `GREATER_THAN("GreaterThan", ...)`, so a reference to source
        // value `GreaterThan` must render `CompareOp.GREATER_THAN` — emitting the raw
        // `CompareOp.GreaterThan` would not compile.
        var expr = resolvedEnumRef("CompareOp", "GreaterThan");

        assertEquals("CompareOp.GREATER_THAN", render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void enum_value_ref_uses_resolved_simple_name_not_qualified_source_text() {
        // enum_arg_fqn_get facet (PR #141): a namespace-qualified enum ref in the rosetta source
        // — e.g. `staticdata.asset.common.TaxonomySourceEnum -> CFTC` — stores the raw qualified
        // text in enumName(), but the RESOLVED enumeration's name() is always the simple
        // identifier. The Java constant must use the SIMPLE name `TaxonomySourceEnum.CFTC` (+ the
        // import, derived from the resolved enumeration via enumImportRefs), NOT the partial/invalid
        // FQN `staticdata.asset.common.TaxonomySourceEnum.CFTC` (missing the real `cdm.base.` package
        // root → non-compiling, and — in an evaluate-arg slot — failing the single-dot probe so a
        // spurious `.get()` is appended). No-op for already-simple refs (name() == enumName()).
        var ref = new REnumValueRef();
        ref.setEnumName("staticdata.asset.common.TaxonomySourceEnum");
        ref.setValueName("CFTC");
        var mockEnum = new REnumeration();
        mockEnum.setName("TaxonomySourceEnum");
        ref.setResolvedEnum(mockEnum);

        assertEquals("TaxonomySourceEnum.CFTC", render(handler.handle(ref, ctx, compiler)));
    }

    @Test
    void unresolved_enum_value_ref_is_disguised_feature_call() {
        // Grammar ambiguity: `paramName -> featureName` parses as REnumValueRef.
        // When global resolution cannot find a real enum, the node represents a
        // feature call on a parameter and must emit the mapper chain pattern.
        var expr = new REnumValueRef();
        expr.setEnumName("trade");
        expr.setValueName("price");
        // No setResolvedEnum — this is the disguised-feature-call path.

        assertEquals(
            "MapperS.of(trade).map(\"getPrice\", _trade -> _trade.getPrice())",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // Implicit variable
    // =========================================================================

    @Test
    void implicit_variable_generates_bare_item() {
        var expr = new RImplicitVariable();

        assertEquals("item", render(handler.handle(expr, ctx, compiler)));
    }

    // -- Elided-operand context-aware rendering (engine-phase PR #1) ---------
    //
    // The synthesised RImplicitVariable emitted by AstBuilder for without-left
    // extract / filter / list-op / conversion forms is the same node type as
    // the literal `item` keyword. ReferenceHandler distinguishes them via the
    // parent-chain walk in `isElidedOperandTopLevel`. Three context flavours
    // need to lock at the unit-test layer (Copilot R1 F1 + indep F4):
    //   (a) parent op at rule-body top level → `MapperS.of(input)`
    //   (b) parent op in plain function body → falls through to `item`
    //       (F1 lock — the rule-input naming guarantee only holds for RRule)
    //   (c) parent op nested in an outer chain's inline function → `item`
    //       (the outer lambda's `item` binding flows through)

    @Test
    void implicit_variable_elided_operand_at_rule_root_renders_mapperS_of_input() {
        // <RRule> ─body─> <RExtractExpr argument=iv> ─argument─> <iv (synthetic)>
        var iv = new RImplicitVariable();
        iv.setSynthetic(true);
        var extract = new RExtractExpr();
        extract.setArgument(iv);
        iv.setParent(extract);
        var rule = new RRule();
        rule.setName("R");
        extract.setParent(rule);

        assertEquals("MapperS.of(input)", render(handler.handle(iv, ctx, compiler)));
    }

    @Test
    void implicit_variable_elided_operand_in_function_body_renders_item() {
        // <RFunction> ─body─> <RExtractExpr argument=iv> ─argument─> <iv (synthetic)>
        // F1 lock — plain user-written function bodies do NOT guarantee an
        // input parameter named "input"; emitting `MapperS.of(input)` here
        // would generate uncompilable Java.
        var iv = new RImplicitVariable();
        iv.setSynthetic(true);
        var extract = new RExtractExpr();
        extract.setArgument(iv);
        iv.setParent(extract);
        var func = functionWithInputs("trade");
        extract.setParent(func);

        assertEquals("item", render(handler.handle(iv, ctx, compiler)));
    }

    @Test
    void implicit_variable_elided_operand_in_explicit_param_lambda_renders_param_name() {
        // Copilot R2 F1 — when the enclosing inline function has an explicit
        // parameter name (e.g. `t`), the synthetic elided operand of an inner
        // op must reference `t`, not the implicit-form default `item`.
        var iv = new RImplicitVariable();
        iv.setSynthetic(true);
        var inner = new RExtractExpr();
        inner.setArgument(iv);
        iv.setParent(inner);
        var lambda = new RInlineFunction();
        lambda.paramNames().add("t");
        lambda.setBody(inner);
        inner.setParent(lambda);
        var outer = new RExtractExpr();
        outer.setBody(lambda);
        lambda.setParent(outer);
        var rule = new RRule();
        rule.setName("R");
        outer.setParent(rule);

        assertEquals("t", render(handler.handle(iv, ctx, compiler)));
    }

    @Test
    void implicit_variable_elided_operand_in_nested_chain_renders_item() {
        // Outer extract's lambda body wraps the inner elided extract; the
        // inner iv piggy-backs on the outer chain's `item` binding rather than
        // referencing the rule input directly.
        var iv = new RImplicitVariable();
        iv.setSynthetic(true);
        var inner = new RExtractExpr();
        inner.setArgument(iv);
        iv.setParent(inner);
        var lambda = new RInlineFunction();
        lambda.setBody(inner);
        inner.setParent(lambda);
        var outer = new RExtractExpr();
        outer.setBody(lambda);
        lambda.setParent(outer);
        var rule = new RRule();
        rule.setName("R");
        outer.setParent(rule);

        assertEquals("item", render(handler.handle(iv, ctx, compiler)));
    }

    @Test
    void literal_item_keyword_in_argument_slot_renders_item_not_mapperS_of_input() {
        // Copilot R4 F1+F2+F3 — the user-written literal `item` keyword used as
        // an explicit receiver (e.g. `item only-element`) occupies the same
        // structural slot as a synthetic elided operand. Predicate tightening
        // via iv.isSynthetic() must keep it routed through the bare-item
        // codegen path, not the `MapperS.of(input)` top-level path.
        var iv = new RImplicitVariable();
        // NOT synthetic — this is the literal `item` keyword
        var extract = new RExtractExpr();
        extract.setArgument(iv);
        iv.setParent(extract);
        var rule = new RRule();
        rule.setName("R");
        extract.setParent(rule);

        assertEquals("item", render(handler.handle(iv, ctx, compiler)));
    }

    // =========================================================================
    // Super call
    // =========================================================================

    @Test
    void super_call_generates_placeholder() {
        var expr = new RSuperCall();

        assertEquals("super.doEvaluate()", render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // Dispatch via ExpressionCompiler
    // =========================================================================

    @Test
    void compiler_dispatches_symbol_reference() {
        var expr = new RSymbolReference();
        expr.setName("myParam");

        var scope  = ExpressionCompilerTest.createTestScope();
        var result = compiler.compile(expr, null, scope);

        assertInstanceOf(JavaExpression.class, result);
        assertEquals("MapperS.of(myParam)", ((JavaExpression) result).renderToString());
    }

    @Test
    void compiler_dispatches_enum_value_ref() {
        // Resolved enum reference — dispatches through ExpressionCompiler and
        // emits the dotted-constant form with the value name CONVERTED to the Java
        // enum constant (camelCase `NewTrade` → `NEW_TRADE`), matching the
        // EnumGenerator-emitted declaration; the raw source name would not compile.
        var expr = resolvedEnumRef("TradeTypeEnum", "NewTrade");

        var scope  = ExpressionCompilerTest.createTestScope();
        var result = compiler.compile(expr, null, scope);

        assertInstanceOf(JavaExpression.class, result);
        assertEquals("TradeTypeEnum.NEW_TRADE", ((JavaExpression) result).renderToString());
    }

    @Test
    void compiler_dispatches_implicit_variable() {
        var expr = new RImplicitVariable();

        var scope  = ExpressionCompilerTest.createTestScope();
        var result = compiler.compile(expr, null, scope);

        assertInstanceOf(JavaExpression.class, result);
        assertEquals("item", ((JavaExpression) result).renderToString());
    }

    @Test
    void compiler_dispatches_super_call() {
        var expr = new RSuperCall();

        var scope  = ExpressionCompilerTest.createTestScope();
        var result = compiler.compile(expr, null, scope);

        assertInstanceOf(JavaExpression.class, result);
        assertEquals("super.doEvaluate()", ((JavaExpression) result).renderToString());
    }
}
