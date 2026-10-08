package com.regnosys.rosetta.generator.java.expression.handlers;

import com.regnosys.rosetta.ast.enums.EqOp;
import com.regnosys.rosetta.ast.enums.LogOp;
import com.regnosys.rosetta.ast.enums.ExistenceOp;
import com.regnosys.rosetta.ast.enums.ListOp;
import com.regnosys.rosetta.ast.expressions.binary.RLogicalExpr;
import com.regnosys.rosetta.ast.expressions.binary.REqualityExpr;
import com.regnosys.rosetta.ast.expressions.binary.RThenExpr;
import com.regnosys.rosetta.ast.expressions.literals.RBooleanLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RListLiteral;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.unary.RCountExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExistenceExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExtractExpr;
import com.regnosys.rosetta.ast.expressions.unary.RFilterExpr;
import com.regnosys.rosetta.ast.expressions.unary.RListOpExpr;
import com.regnosys.rosetta.ast.expressions.unary.RMaxExpr;
import com.regnosys.rosetta.ast.expressions.unary.RMinExpr;
import com.regnosys.rosetta.ast.expressions.unary.RReduceExpr;
import com.regnosys.rosetta.ast.expressions.unary.RSortExpr;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompilerTest;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CollectionHandlerTest {

    private CollectionHandler handler;
    private ExpressionCompiler compiler;
    private ExpressionContext ctx;

    @BeforeEach
    void setUp() {
        handler  = new CollectionHandler();
        compiler = new ExpressionCompiler();
        ctx      = ExpressionContext.of(null, ExpressionCompilerTest.createTestScope());
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private String render(JavaStatementBuilder result) {
        assertInstanceOf(JavaExpression.class, result);
        // seat 32 law E.3 (explicitClosureParamNameBurn): compileLambda now registers an EXPLICIT
        // closure param's name as a burn on its body scope and embeds the deferred sentinel
        // (`__COERCION_PARAM_<n>__`) in the rendered param, exactly as the coercion sentinels the
        // pipeline has carried since #339; the real generator resolves them at file assembly. This
        // harness renders a bare handler result, so it resolves them here - the NavigationHandlerTest
        // precedent ("resolve here so the pins keep asserting the actual names"). A group-of-one burn
        // resolves to the param's own name, so every pinned string in this class is unchanged.
        return ctx.scope().resolveDeferredCoercionNames(
                ((JavaExpression) result).renderToString());
    }

    private RSymbolReference symbolRef(String name) {
        var ref = new RSymbolReference();
        ref.setName(name);
        return ref;
    }

    /**
     * Symbol reference resolving to an unbounded (multi) attribute, so
     * {@link com.regnosys.rosetta.types.inference.CardinalityComputer} reports
     * the receiver as MULTI. Used to exercise the {@code mapItem} branch of the
     * extract selector (a bare {@link #symbolRef} has no resolved symbol and
     * defaults to SINGLE -> {@code mapSingleToItem}).
     */
    private RSymbolReference multiSymbolRef(String name) {
        var card = new com.regnosys.rosetta.ast.supporting.RCardinality();
        card.setInf(0);
        card.setUnbounded(true);
        var tc = new com.regnosys.rosetta.ast.supporting.RTypeCall();
        tc.setTypeName("int");
        var attr = new com.regnosys.rosetta.ast.supporting.RAttribute();
        attr.setName(name);
        attr.setTypeCall(tc);
        attr.setCardinality(card);
        var ref = new RSymbolReference();
        ref.setName(name);
        ref.setResolvedSymbol(attr);
        return ref;
    }

    /**
     * Symbol reference resolving to an {@link RFunction}, so the engine PR #6
     * facet (a) branch in {@link ReferenceHandler} renders it as an invocation
     * of the injected instance rather than a bare variable.
     *
     * <p>The fixture callee declares ONE real input (the corpus shape every
     * in-lambda predicate invocation has — {@code IsAllowableActionForASIC}
     * takes the piped item): facet tobuilder_output_assignment (mechanism 5)
     * renders a callee with NO real inputs as the argless {@code evaluate()}
     * (see {@link #funcRefNoInputs(String)} and the zero-input contract test),
     * so the implicit-item-arg shape these tests pin requires a real input on
     * the fixture.
     */
    private RSymbolReference funcRef(String name) {
        var func = new com.regnosys.rosetta.ast.functions.RFunction();
        func.setName(name);
        var tc = new com.regnosys.rosetta.ast.supporting.RTypeCall();
        tc.setTypeName("string");
        var input = new com.regnosys.rosetta.ast.supporting.RAttribute();
        input.setName("subject");
        input.setTypeCall(tc);
        func.inputs().add(input);
        var ref = new RSymbolReference();
        ref.setName(name);
        ref.setResolvedSymbol(func);
        return ref;
    }

    /**
     * Symbol reference resolving to an {@link RFunction} with NO real inputs —
     * the facet tobuilder_output_assignment (mechanism 5) zero-real-input shape,
     * which renders as the argless {@code evaluate()} (upstream builds the
     * argument list from EXPLICIT arguments only, so a zero-input callee never
     * receives a synthesized implicit item).
     */
    private RSymbolReference funcRefNoInputs(String name) {
        var func = new com.regnosys.rosetta.ast.functions.RFunction();
        func.setName(name);
        var ref = new RSymbolReference();
        ref.setName(name);
        ref.setResolvedSymbol(func);
        return ref;
    }

    private RIntLiteral intLit(int value) {
        var lit = new RIntLiteral();
        lit.setValue(value);
        return lit;
    }

    private RBooleanLiteral boolLit(boolean value) {
        var lit = new RBooleanLiteral();
        lit.setValue(value);
        return lit;
    }

    private RInlineFunction implicitFunc(com.regnosys.rosetta.ast.RExpression body) {
        var func = new RInlineFunction();
        func.setImplicit(true);
        func.setBody(body);
        return func;
    }

    private RInlineFunction explicitFunc(String paramName, com.regnosys.rosetta.ast.RExpression body) {
        var func = new RInlineFunction();
        func.setImplicit(false);
        func.paramNames().add(paramName);
        func.setBody(body);
        return func;
    }

    private RInlineFunction reduceFunc(String param1, String param2, com.regnosys.rosetta.ast.RExpression body) {
        var func = new RInlineFunction();
        func.setImplicit(false);
        func.paramNames().add(param1);
        func.paramNames().add(param2);
        func.setBody(body);
        return func;
    }

    // =========================================================================
    // 1. Filter
    // =========================================================================

    @Test
    void filter_over_single_receiver_generates_filterSingleNullSafe() {
        // Engine PR #6 facet (d): a SCALAR receiver (a bare symbol ref has no
        // resolved symbol -> CardinalityComputer SINGLE) selects
        // filterSingleNullSafe, mirroring upstream caseFilterOperation's
        // !isPreviousOperationMulti branch. The fork previously hardcoded
        // filterItemNullSafe.
        var expr = new RFilterExpr();
        expr.setArgument(symbolRef("items"));
        expr.setBody(implicitFunc(symbolRef("predicate")));

        assertEquals(
            "MapperS.of(items)\n\t.filterSingleNullSafe(item -> MapperS.of(predicate))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void filter_over_multi_receiver_generates_filterItemNullSafe() {
        // Engine PR #6 facet (d): a MULTI receiver (resolved to an unbounded
        // attribute) selects filterItemNullSafe, mirroring upstream
        // caseFilterOperation's isPreviousOperationMulti branch.
        var expr = new RFilterExpr();
        expr.setArgument(multiSymbolRef("trades"));
        expr.setBody(implicitFunc(symbolRef("predicate")));

        assertEquals(
            "MapperS.of(trades)\n\t.filterItemNullSafe(item -> MapperS.of(predicate))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void filter_with_explicit_lambda_uses_param_name() {
        var expr = new RFilterExpr();
        expr.setArgument(symbolRef("trades"));
        expr.setBody(explicitFunc("t", symbolRef("isValid")));

        // SCALAR receiver -> filterSingleNullSafe (facet (d)).
        assertEquals(
            "MapperS.of(trades)\n\t.filterSingleNullSafe(t -> MapperS.of(isValid))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void filter_with_function_predicate_invokes_injected_instance() {
        // Engine PR #6 facet (a): a bare function reference used as the filter
        // predicate (`filter IsAllowableActionForASIC`) resolves to an RFunction
        // with empty args(). It must invoke the injected instance with the
        // enclosing implicit variable — isAllowableActionForASIC.evaluate(item.get())
        // — NOT render the function type name as a literal (MapperS.of(...)).
        // Receiver = lowerCamelCase(name) matches the @Inject field
        // FunctionDependencyCollector registers.
        var funcRef = funcRef("IsAllowableActionForASIC");
        var lambda = implicitFunc(funcRef);
        funcRef.setParent(lambda);
        var expr = new RFilterExpr();
        expr.setArgument(symbolRef("items"));
        expr.setBody(lambda);

        assertEquals(
            "MapperS.of(items)\n\t.filterSingleNullSafe(item -> isAllowableActionForASIC.evaluate(item.get()))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void filter_with_zero_input_function_predicate_renders_argless_evaluate() {
        // facet tobuilder_output_assignment (mechanism 5): a bare reference to a
        // callee with NO real inputs renders the ARGLESS evaluate() — upstream
        // builds the argument list from explicit arguments only, so a zero-input
        // function never receives a synthesized implicit item (the pre-facet
        // `evaluate(item.get())` passed an argument a no-param signature cannot
        // accept). The 1-real-input shape keeps the implicit item arg (the test
        // above).
        var funcRef = funcRefNoInputs("IsAllowableActionForASIC");
        var lambda = implicitFunc(funcRef);
        funcRef.setParent(lambda);
        var expr = new RFilterExpr();
        expr.setArgument(symbolRef("items"));
        expr.setBody(lambda);

        assertEquals(
            "MapperS.of(items)\n\t.filterSingleNullSafe(item -> isAllowableActionForASIC.evaluate())",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void filter_with_explicit_arg_function_predicate_strips_spurious_mapperS_wrap() {
        // over_wrap_predicate facet (engine, drr rule-family): a filter predicate
        // whose body is a function invocation WITH explicit args
        // (`filter [item -> IsFXOption(trade)]`) compiles the call through the
        // wrappedInMapperSOf factory (ReferenceHandler's explicit-args path —
        // function(a) → MapperS.of(fn.evaluate(a))). The filterSingleNullSafe
        // predicate signature is Function<MapperS<T>, Boolean>, so a MapperS<Boolean>
        // body does NOT compile; golden strips the spurious wrap, leaving the bare
        // Boolean-valued evaluate(...). INVERSE of the MapperS-EXPECTING extract body
        // (extract_with_function_body_wraps_invocation_in_mapperS), which KEEPS the
        // wrap. Contrast filter_with_function_predicate_invokes_injected_instance
        // (a bare no-args FUNCTION) which renderImplicitFunctionInvocation already
        // emits UNWRAPPED. Corpus witness: drr CallAmount/PutAmount
        // (isFXOption(productForEvent(item))).
        var funcRef = funcRef("IsFXOption");
        funcRef.args().add(symbolRef("trade"));
        var lambda = implicitFunc(funcRef);
        funcRef.setParent(lambda);
        var expr = new RFilterExpr();
        expr.setArgument(symbolRef("items"));
        expr.setBody(lambda);

        assertEquals(
            "MapperS.of(items)\n\t.filterSingleNullSafe(item -> isFXOption.evaluate(trade))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void filter_with_boolean_literal_predicate_strips_spurious_mapperS_wrap() {
        // over_wrap_predicate facet: a filter predicate whose body is a boolean
        // literal (`filter [item -> True]`) compiles via
        // LiteralHandler.handle(RBooleanLiteral), which wraps it MapperS.of(true).
        // The predicate position wants a bare Boolean, so the wrap is stripped —
        // golden emits `item -> true`. Same structural unwrap as the invocation
        // bodies above (all three wrap-producing shapes carry an unwrapToBuilder).
        // Corpus witness: drr IsReportableEventRule (cftc trade).
        var expr = new RFilterExpr();
        expr.setArgument(symbolRef("items"));
        expr.setBody(implicitFunc(boolLit(true)));

        assertEquals(
            "MapperS.of(items)\n\t.filterSingleNullSafe(item -> true)",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void filter_with_comparison_predicate_wraps_bareFn_operand_and_appends_get() {
        // areEqual_operand_wrap facet (engine, drr rule-family): a filter predicate
        // whose body is a COMPARISON between a bare implicit-FUNCTION invocation and a
        // boolean literal (`filter [item -> IsMax32UpperCaseAlphanumericText = True]`)
        // sits in a Boolean-EXPECTING position. The bare fork output
        //   areEqual(isMax32...Text.evaluate(item.get()), MapperS.of(true), All)
        // has TWO coupled defects, both of which make it non-compiling:
        //   (1) the bare-fn LEFT operand renders UNWRAPPED (renderImplicitFunctionInvocation
        //       is UNWRAPPED by design), but areEqual needs a Mapper operand — so
        //       ComparisonHandler wraps it MapperS.of(...);
        //   (2) areEqual is a ComparisonResult (a Mapper<Boolean>, NOT a Boolean), but
        //       filterSingleNullSafe's predicate is Function<MapperS<T>, Boolean> — so
        //       CollectionHandler appends .get() (the inverse of the MapperS-expecting
        //       map body's .asMapper(), see extract_with_existence_body_appends_asMapper).
        // Golden:
        //   filterSingleNullSafe(item -> areEqual(MapperS.of(isMax32...Text.evaluate(item.get())), MapperS.of(true), CardinalityOperator.All).get())
        // Corpus witness: drr UtiRule (= True) / UtiProprietaryRule (= False)
        // (esma trade: EsmaUti then filter IsMax32UpperCaseAlphanumericText = True).
        var funcRef = funcRef("IsMax32UpperCaseAlphanumericText");
        var cmp = new REqualityExpr();
        cmp.setOp(EqOp.EQ);
        cmp.setLeft(funcRef);
        cmp.setRight(boolLit(true));
        var lambda = implicitFunc(cmp);
        funcRef.setParent(cmp);
        cmp.setParent(lambda);
        var expr = new RFilterExpr();
        expr.setArgument(symbolRef("items"));
        expr.setBody(lambda);

        assertEquals(
            "MapperS.of(items)\n\t.filterSingleNullSafe(item -> areEqual(MapperS.of(isMax32UpperCaseAlphanumericText.evaluate(item.get())), MapperS.of(true), CardinalityOperator.All).get())",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void filter_with_logical_or_predicate_wraps_bareFn_operands_and_appends_get() {
        // or_chain_filter_predicate facet (engine, drr rule-family): a filter predicate
        // whose body is a logical OR/AND chain of bare implicit-FUNCTION invocations
        // (`filter [item -> IsActionTypeNEWT or IsActionTypeVALU]`) sits in a
        // Boolean-EXPECTING position. The bare fork output
        //   isActionTypeNEWT.evaluate(item.get()).orNullSafe(isActionTypeVALU.evaluate(item.get()))
        // has TWO coupled defects, both non-compiling:
        //   (1) orNullSafe/andNullSafe are ComparisonResult instance methods, but each
        //       bare-fn operand renders as a raw Boolean — so LogicalHandler wraps each
        //       operand ComparisonResult.ofNullSafe(MapperS.of(...));
        //   (2) the OR chain is a ComparisonResult (a Mapper<Boolean>, NOT a Boolean),
        //       but filterSingleNullSafe's predicate is Function<MapperS<T>, Boolean> —
        //       so CollectionHandler appends .get() (the RLogicalExpr sibling of the
        //       single-comparison appendGet, see
        //       filter_with_comparison_predicate_wraps_bareFn_operand_and_appends_get).
        // Golden:
        //   filterSingleNullSafe(item -> ComparisonResult.ofNullSafe(MapperS.of(isActionTypeNEWT.evaluate(item.get()))).orNullSafe(ComparisonResult.ofNullSafe(MapperS.of(isActionTypeVALU.evaluate(item.get())))).get())
        // Corpus witness: drr DeltaRule / ExecutionTimestampRule (asic/esma trade:
        // filter IsActionType... or IsActionType...).
        var left  = funcRef("IsActionTypeNEWT");
        var right = funcRef("IsActionTypeVALU");
        var or = new RLogicalExpr();
        or.setOp(LogOp.OR);
        or.setLeft(left);
        or.setRight(right);
        var lambda = implicitFunc(or);
        left.setParent(or);
        right.setParent(or);
        or.setParent(lambda);
        var expr = new RFilterExpr();
        expr.setArgument(symbolRef("items"));
        expr.setBody(lambda);

        assertEquals(
            "MapperS.of(items)\n\t.filterSingleNullSafe(item -> ComparisonResult.ofNullSafe(MapperS.of(isActionTypeNEWT.evaluate(item.get()))).orNullSafe(ComparisonResult.ofNullSafe(MapperS.of(isActionTypeVALU.evaluate(item.get())))).get())",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // 2. Extract (map)
    // =========================================================================

    @Test
    void extract_over_single_receiver_generates_mapSingleToItem() {
        // Engine PR #3 facet B: a SCALAR receiver (a bare symbol ref has no
        // resolved symbol -> CardinalityComputer SINGLE) with a SCALAR body
        // selects mapSingleToItem, mirroring upstream caseMapOperation's
        // !isPreviousOperationMulti && !isBodyMulti branch. The chain link is
        // emitted on a new line with one RELATIVE tab (CHAIN_LINK) — the base
        // indent is applied later by FunctionExpressionRenderer.
        var expr = new RExtractExpr();
        expr.setArgument(symbolRef("parties"));
        expr.setBody(implicitFunc(symbolRef("name")));

        assertEquals(
            "MapperS.of(parties)\n\t.mapSingleToItem(item -> MapperS.of(name))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void extract_with_function_body_wraps_invocation_in_mapperS() {
        // wrap_placement facet (engine, drr rule-family): a bare FUNCTION
        // reference as a SINGLE-body extract (`extract GetDeliveryTypeDSBRecord`)
        // is a MapperS-EXPECTING position — the mapSingleToItem lambda's body must
        // return a MapperS. The injected-instance invocation
        // (renderImplicitFunctionInvocation, which is UNWRAPPED by default for the
        // boolean filter-predicate position) must therefore be wrapped in
        // MapperS.of here. Golden:
        //   mapSingleToItem(item -> MapperS.of(getDeliveryTypeDSBRecord.evaluate(item.get())))
        // Contrast filter_with_function_predicate_invokes_injected_instance above,
        // which stays UNWRAPPED (boolean position). The wrap is keyed on POSITION
        // (map body) not on function-vs-rule: a bare RULE body already wraps via
        // renderImplicitRuleInvocation, so this branch is FUNCTION-only to avoid
        // double-wrapping.
        var funcRef = funcRef("GetDeliveryTypeDSBRecord");
        var lambda = implicitFunc(funcRef);
        funcRef.setParent(lambda);
        var expr = new RExtractExpr();
        expr.setArgument(symbolRef("items"));
        expr.setBody(lambda);

        assertEquals(
            "MapperS.of(items)\n\t.mapSingleToItem(item -> MapperS.of(getDeliveryTypeDSBRecord.evaluate(item.get())))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void extract_with_existence_body_appends_asMapper() {
        // existence_body_asmapper facet (engine, drr rule-family): a SINGLE-body
        // extract whose body is an existence check (`extract <nav> exists`) sits in
        // a MapperS-EXPECTING position — the mapSingleToItem lambda must return a
        // MapperS, but `exists(...)` compiles to a ComparisonResult (a
        // Mapper<Boolean>, NOT a MapperS). Golden converts it via `.asMapper()`:
        //   mapSingleToItem(item -> exists(MapperS.of(portfolioIdentifier)).asMapper())
        // Mirrors extract_with_function_body_wraps_invocation_in_mapperS (bare-fn
        // body -> MapperS.of wrap); both convert a non-MapperS SINGLE map body to a
        // MapperS, keyed on POSITION (map body) not on the body kind. Contrast the
        // filter-predicate position, where a bare ComparisonResult is correct.
        // Corpus witness: drr CollateralPortfolioIndicatorRule (asic/esma/fca margin).
        var existsExpr = new RExistenceExpr();
        existsExpr.setArgument(symbolRef("portfolioIdentifier"));
        existsExpr.setOp(ExistenceOp.EXISTS);
        var expr = new RExtractExpr();
        expr.setArgument(symbolRef("items"));
        expr.setBody(implicitFunc(existsExpr));

        assertEquals(
            "MapperS.of(items)\n\t.mapSingleToItem(item -> exists(MapperS.of(portfolioIdentifier)).asMapper())",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void extract_with_ctor_body_wraps_builder_in_mapperS() {
        // facet mapitem_ctor_wrap (mechanism 1): a constructor body compiles to
        // a BARE item (the POJO builder block, never a Mapper), so the
        // MapperS-EXPECTING map-body seat wraps it MapperS.of(…) — the same
        // wrap factory (and the same positional law) as the bare-fn-invocation
        // arm pinned by extract_with_function_body_wraps_invocation_in_mapperS
        // above. Golden witness: cdm CompareTradeLot / drr esma GetNtnlQty
        // `.mapItem(item -> MapperS.of(X.builder()…build()))`.
        var typeCall = new com.regnosys.rosetta.ast.supporting.RTypeCall();
        typeCall.setTypeName("Party");
        var ctor = new com.regnosys.rosetta.ast.expressions.constructors.RConstructorExpr();
        ctor.setTypeCall(typeCall);
        var pair = new com.regnosys.rosetta.ast.expressions.supporting.RKeyValuePair();
        pair.setKey("name");
        pair.setValue(symbolRef("partyName"));
        ctor.pairs().add(pair);
        var expr = new RExtractExpr();
        expr.setArgument(symbolRef("items"));
        expr.setBody(implicitFunc(ctor));

        assertEquals(
            "MapperS.of(items)\n\t.mapSingleToItem(item -> MapperS.of(Party.builder().setName(MapperS.of(partyName)).build()))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void extract_multiline_expression_body_reindents_per_nesting_level() {
        // facet mapitem_ctor_wrap (mechanism 2): a MULTI-LINE expression body
        // re-indents +1 tab per lambda nesting level — upstream's Xtend
        // StringConcatenation prepends the chain-template placeholder line's
        // tab to every continuation line of the embedded lambda code,
        // compounding per nesting (golden CompareTradeLot: outer .mapItem at
        // 4 tabs, inner at 5, ctor setters at 6 over statement base 3). The
        // inner chain's own CHAIN_LINK "\n\t." gains exactly one extra tab
        // inside the outer lambda; single-line bodies (every other pin in this
        // class) have no newline and are byte-unchanged.
        var inner = new RExtractExpr();
        inner.setArgument(symbolRef("xs"));
        inner.setBody(implicitFunc(symbolRef("y")));
        var expr = new RExtractExpr();
        expr.setArgument(symbolRef("items"));
        expr.setBody(implicitFunc(inner));

        assertEquals(
            "MapperS.of(items)\n\t.mapSingleToItem(item -> MapperS.of(xs)\n\t\t.mapSingleToItem(item -> MapperS.of(y)))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void extract_over_multi_receiver_generates_mapItem() {
        // Engine PR #3 facet B: a MULTI receiver with a SCALAR body selects
        // mapItem (upstream caseMapOperation isPreviousOperationMulti &&
        // !isBodyMulti branch).
        var expr = new RExtractExpr();
        expr.setArgument(multiSymbolRef("parties"));
        expr.setBody(implicitFunc(symbolRef("name")));

        assertEquals(
            "MapperS.of(parties)\n\t.mapItem(item -> MapperS.of(name))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void extract_over_single_receiver_with_multi_body_generates_mapSingleToList() {
        // Engine PR #3 facet B: a SCALAR receiver with a MULTI body selects
        // mapSingleToList (upstream caseMapOperation !isPreviousOperationMulti &&
        // isBodyMulti branch). The multi body is an unbounded symbol ref.
        var expr = new RExtractExpr();
        expr.setArgument(symbolRef("party"));
        expr.setBody(implicitFunc(multiSymbolRef("names")));

        assertEquals(
            "MapperS.of(party)\n\t.mapSingleToList(item -> MapperS.of(names))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void extract_over_multi_receiver_with_multi_body_generates_mapItemToList() {
        // Engine PR #3 facet B: a MULTI receiver with a MULTI body selects
        // mapItemToList (upstream caseMapOperation isPreviousOperationMulti &&
        // isBodyMulti branch).
        var expr = new RExtractExpr();
        expr.setArgument(multiSymbolRef("parties"));
        expr.setBody(implicitFunc(multiSymbolRef("names")));

        assertEquals(
            "MapperS.of(parties)\n\t.mapItemToList(item -> MapperS.of(names))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void extract_lambda_body_refs_propagate_to_outer_expression() {
        // Engine PR #2 Bucket D — lock the lambda-ref propagation invariant.
        // Pre-fix: compileLambda discarded the body's refs via
        // HandlerHelper.render(...).toString, so any import collected inside
        // the lambda body (e.g. <ReportableInformation> generic witness on a
        // chain inside a rule body) was dropped — the generated source
        // referenced the type without importing it, breaking compilation.
        //
        // Test setup: extract's argument is a symbol ref (produces MAPPER_S
        // ref via MapperS.of); body is a RListLiteral (produces a distinct
        // MAPPER_C ref via wrappedInMapperCOf). Both refs must appear in the
        // outer JavaExpression's refs set when propagation works correctly.
        var listLit = new RListLiteral();
        listLit.elements().add(intLit(1));
        listLit.elements().add(intLit(2));

        var expr = new RExtractExpr();
        expr.setArgument(symbolRef("items"));
        expr.setBody(implicitFunc(listLit));

        JavaStatementBuilder result = handler.handle(expr, ctx, compiler);
        var refSimpleNames = result.getRefs().stream()
                .map(c -> c.getSimpleName())
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(refSimpleNames.contains("MapperS"),
                "outer refs should contain MapperS from the receiver; got " + refSimpleNames);
        assertTrue(refSimpleNames.contains("MapperC"),
                "outer refs should contain MapperC from the lambda body's RListLiteral "
                + "(invariant: lambda-body refs propagate via compileLambda's LambdaCompiled "
                + "record); got " + refSimpleNames);
    }

    // =========================================================================
    // 3. Sort with key
    // =========================================================================

    @Test
    void sort_with_key_generates_sort_lambda() {
        var expr = new RSortExpr();
        expr.setArgument(symbolRef("prices"));
        expr.setBody(explicitFunc("p", symbolRef("amount")));

        assertEquals(
            "MapperS.of(prices)\n\t.sort(p -> MapperS.of(amount))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void sort_without_key_generates_natural_sort() {
        var expr = new RSortExpr();
        expr.setArgument(symbolRef("numbers"));
        // no body

        assertEquals(
            "MapperS.of(numbers)\n\t.sort()",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // 4. Reduce
    // =========================================================================

    @Test
    void reduce_generates_two_param_lambda() {
        var expr = new RReduceExpr();
        expr.setArgument(symbolRef("values"));
        expr.setBody(reduceFunc("acc", "item", symbolRef("sum")));

        assertEquals(
            "MapperS.of(values)\n\t.reduce((acc, item) -> MapperS.of(sum))",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // 5. First (via ListOp)
    // =========================================================================

    @Test
    void list_op_first_generates_first() {
        var expr = new RListOpExpr();
        expr.setArgument(symbolRef("items"));
        expr.setOp(ListOp.FIRST);

        assertEquals(
            "MapperS.of(items)\n\t.first()",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // 6. Last (via ListOp)
    // =========================================================================

    @Test
    void list_op_last_generates_last() {
        var expr = new RListOpExpr();
        expr.setArgument(symbolRef("items"));
        expr.setOp(ListOp.LAST);

        assertEquals(
            "MapperS.of(items)\n\t.last()",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // 7. Flatten (via ListOp)
    // =========================================================================

    @Test
    void list_op_flatten_generates_flattenList() {
        var expr = new RListOpExpr();
        expr.setArgument(symbolRef("nested"));
        expr.setOp(ListOp.FLATTEN);

        assertEquals(
            "MapperS.of(nested)\n\t.flattenList()",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // 8. Distinct (via ListOp)
    // =========================================================================

    @Test
    void list_op_distinct_generates_distinct() {
        var expr = new RListOpExpr();
        expr.setArgument(symbolRef("items"));
        expr.setOp(ListOp.DISTINCT);

        // facet distinct_prefix_rendering: DISTINCT renders the upstream PREFIX
        // runtime fn (ExpressionOperatorsNullSafe.distinct), NOT a member call —
        // the Mapper classes have no `distinct` member. The static wildcard
        // import rides the structured channel exactly like the comparison fns.
        var result = handler.handle(expr, ctx, compiler);
        assertEquals("distinct(MapperS.of(items))", render(result));
        assertTrue(result.getStaticWildcardImports()
                        .contains(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE),
                "prefix distinct must carry the ExpressionOperatorsNullSafe wildcard");
    }

    // =========================================================================
    // 9. Count
    // =========================================================================

    @Test
    void count_generates_resultCount() {
        var expr = new RCountExpr();
        expr.setArgument(symbolRef("trades"));

        assertEquals(
            "MapperS.of(trades).resultCount()",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // 10. Then (piping)
    // =========================================================================

    @Test
    void then_with_body_generates_then_lambda() {
        var expr = new RThenExpr();
        expr.setArgument(symbolRef("input"));
        expr.setBody(explicitFunc("x", symbolRef("transform")));

        assertEquals(
            "MapperS.of(input).then(x -> MapperS.of(transform))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void then_without_body_returns_argument() {
        var expr = new RThenExpr();
        expr.setArgument(symbolRef("passthrough"));
        // no body

        assertEquals(
            "MapperS.of(passthrough)",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // Additional ListOp coverage
    // =========================================================================

    @Test
    void list_op_reverse_generates_reverse() {
        var expr = new RListOpExpr();
        expr.setArgument(symbolRef("list"));
        expr.setOp(ListOp.REVERSE);

        assertEquals(
            "MapperS.of(list)\n\t.reverse()",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void list_op_sum_generates_sum() {
        var expr = new RListOpExpr();
        expr.setArgument(symbolRef("amounts"));
        expr.setOp(ListOp.SUM);

        assertEquals(
            "MapperS.of(amounts).sum()",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void list_op_only_element_generates_get() {
        var expr = new RListOpExpr();
        expr.setArgument(symbolRef("singleton"));
        expr.setOp(ListOp.ONLY_ELEMENT);

        assertEquals(
            "MapperS.of(singleton).get()",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // Max / Min
    // =========================================================================

    @Test
    void max_without_key_generates_parameterless_max() {
        var expr = new RMaxExpr();
        expr.setArgument(symbolRef("values"));
        // no body

        assertEquals(
            "MapperS.of(values)\n\t.max()",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void max_with_key_generates_max_lambda() {
        var expr = new RMaxExpr();
        expr.setArgument(symbolRef("items"));
        expr.setBody(explicitFunc("x", symbolRef("price")));

        assertEquals(
            "MapperS.of(items)\n\t.max(x -> MapperS.of(price))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void min_without_key_generates_parameterless_min() {
        var expr = new RMinExpr();
        expr.setArgument(symbolRef("values"));
        // no body

        assertEquals(
            "MapperS.of(values)\n\t.min()",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void min_with_key_generates_min_lambda() {
        var expr = new RMinExpr();
        expr.setArgument(symbolRef("items"));
        expr.setBody(explicitFunc("x", symbolRef("amount")));

        assertEquals(
            "MapperS.of(items)\n\t.min(x -> MapperS.of(amount))",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // Dispatch via ExpressionCompiler
    // =========================================================================

    @Test
    void compiler_dispatches_filter() {
        var expr = new RFilterExpr();
        expr.setArgument(symbolRef("data"));
        expr.setBody(implicitFunc(symbolRef("check")));

        var scope  = ExpressionCompilerTest.createTestScope();
        var result = compiler.compile(expr, null, scope);

        assertInstanceOf(JavaExpression.class, result);
        assertEquals(
            "MapperS.of(data)\n\t.filterSingleNullSafe(item -> MapperS.of(check))",
            ((JavaExpression) result).renderToString());
    }

    @Test
    void compiler_dispatches_count() {
        var expr = new RCountExpr();
        expr.setArgument(symbolRef("list"));

        var scope  = ExpressionCompilerTest.createTestScope();
        var result = compiler.compile(expr, null, scope);

        assertInstanceOf(JavaExpression.class, result);
        assertEquals(
            "MapperS.of(list).resultCount()",
            ((JavaExpression) result).renderToString());
    }

    @Test
    void compiler_dispatches_then() {
        var expr = new RThenExpr();
        expr.setArgument(symbolRef("input"));
        expr.setBody(implicitFunc(symbolRef("output")));

        var scope  = ExpressionCompilerTest.createTestScope();
        var result = compiler.compile(expr, null, scope);

        assertInstanceOf(JavaExpression.class, result);
        assertEquals(
            "MapperS.of(input).then(item -> MapperS.of(output))",
            ((JavaExpression) result).renderToString());
    }

    @Test
    void compiler_dispatches_list_op() {
        var expr = new RListOpExpr();
        expr.setArgument(symbolRef("items"));
        expr.setOp(ListOp.DISTINCT);

        var scope  = ExpressionCompilerTest.createTestScope();
        var result = compiler.compile(expr, null, scope);

        assertInstanceOf(JavaExpression.class, result);
        // facet distinct_prefix_rendering — the dispatcher takes the same
        // prefix runtime-fn form as the direct handler call.
        assertEquals(
            "distinct(MapperS.of(items))",
            ((JavaExpression) result).renderToString());
    }
}
