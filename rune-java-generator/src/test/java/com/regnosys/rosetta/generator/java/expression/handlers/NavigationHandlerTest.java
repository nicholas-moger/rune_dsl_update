package com.regnosys.rosetta.generator.java.expression.handlers;

import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.expressions.references.RDeepFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RCardinality;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.generator.java.SilentDegradation;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompilerTest;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NavigationHandlerTest {

    private NavigationHandler handler;
    private ExpressionCompiler compiler;
    private ExpressionContext ctx;

    @BeforeEach
    void setUp() {
        handler  = new NavigationHandler();
        compiler = new ExpressionCompiler();
        ctx      = ExpressionContext.of(null, seededScope());
    }

    /**
     * A render scope seeded with the names a first-step navigation lambda
     * variable collides against, reproducing the legacy plugin's collision-based
     * {@code _} prefix (Phase X1 scope-faithful lambda naming). In a real
     * function body these are the input parameter names — each named like its
     * type per the CDM convention — seeded on the render scope's ROOT before
     * the expression is rendered (see
     * {@code FunctionExpressionRenderer.createScope(RExpression)}; since facet
     * meta_coercion_numbering compilation threads the seed root's BODY child,
     * with reads walking ancestors — behaviorally identical here). The unit
     * tests use synthetic receivers without a seeded enclosing function, so the
     * equivalent names are seeded here directly. Since facet
     * alias_receiver_typing, CHAINED-step lambda names are ALSO routed through
     * {@code disambiguate} (upstream's {@code createUniqueIdentifier} always
     * escapes; a colliding chained var is a non-compiling Java shadow), so the
     * seed must mirror a REAL function scope: the corpus functions behind the
     * chained golden patterns asserted below (e.g.
     * {@code businessEvent -> instruction -> primitiveInstruction}) declare no
     * input named like a mid-chain step type, so such names are NOT seeded;
     * {@link #chained_lambda_var_escapes_on_scope_collision} covers the
     * colliding shape ({@code product} — the drr IsFXForward corpus form).
     */
    private static JavaStatementScope seededScope() {
        JavaStatementScope scope = ExpressionCompilerTest.createTestScope();
        for (String name : new String[]{
                "trade", "businessEvent", "contract", "product", "event",
                "container", "payout",
                "adjustableOrAdjustedOrRelativeDate"}) {
            scope.createUniqueIdentifier(name);
        }
        return scope;
    }

    // =========================================================================
    // Helper
    // =========================================================================

    /**
     * facet lambdaNaming M3/M4 (PR #329): resolveLambdaVarName now returns a deferred
     * sentinel (registerDeferredLambdaParam) — resolve it so the pins keep asserting
     * the actual name the finalization produces.
     */
    private String resolvedLambdaVar(
            com.regnosys.rosetta.ast.RExpression receiver, String featureName) {
        return ctx.scope().resolveDeferredCoercionNames(
                handler.resolveLambdaVarName(receiver, featureName, ctx.scope()));
    }

    private String renderRaw(JavaStatementBuilder result) {
        assertInstanceOf(JavaExpression.class, result);
        return ((JavaExpression) result).renderToString();
    }

    private String render(JavaStatementBuilder result) {
        assertInstanceOf(JavaExpression.class, result);
        // facet lambdaNaming M3/M4 (PR #329): nav lambda params render as deferred
        // sentinels (registerDeferredLambdaParam) and resolve at finalization —
        // resolve here so the pins keep asserting the actual names (the naming law
        // itself is unchanged for these shapes: group-of-one, escaped iff taken).
        return ctx.scope().resolveDeferredCoercionNames(
                ((JavaExpression) result).renderToString());
    }

    private RSymbolReference symbolRef(String name) {
        var ref = new RSymbolReference();
        ref.setName(name);
        return ref;
    }

    /**
     * Creates an {@link RAttribute} with a type call and cardinality.
     *
     * @param typeName   the type name for the attribute's type call
     * @param inf        lower cardinality bound
     * @param sup        upper cardinality bound (-1 for unbounded)
     */
    private RAttribute attr(String typeName, int inf, int sup) {
        var tc = new RTypeCall();
        tc.setTypeName(typeName);

        var card = new RCardinality();
        card.setInf(inf);
        if (sup < 0) {
            card.setUnbounded(true);
        } else {
            card.setSup(sup);
        }

        var attribute = new RAttribute();
        attribute.setTypeCall(tc);
        attribute.setCardinality(card);
        return attribute;
    }

    /**
     * Add a {@code [metadata <qualifier>]} annotation to an attribute.
     */
    private RAttribute withMetadata(RAttribute attribute, String qualifier) {
        var ref = new RAnnotationRef();
        ref.setAnnotationName("metadata");
        ref.setQualifierName(qualifier);
        attribute.annotationRefs().add(ref);
        return attribute;
    }

    // =========================================================================
    // Simple feature call (single arrow)
    // =========================================================================

    @Test
    void feature_call_generates_map_with_getter_and_lambda() {
        var expr = new RFeatureCall();
        expr.setReceiver(symbolRef("trade"));
        expr.setFeatureName("price");

        // Lambda var is _trade (receiver symbol name), not _price (feature name)
        assertEquals(
            "MapperS.of(trade).map(\"getPrice\", _trade -> _trade.getPrice())",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void feature_call_capitalises_first_letter_of_feature_name() {
        var expr = new RFeatureCall();
        expr.setReceiver(symbolRef("businessEvent"));
        expr.setFeatureName("trade");

        // Lambda var is _businessEvent (receiver symbol name), not _trade (feature name)
        assertEquals(
            "MapperS.of(businessEvent).map(\"getTrade\", _businessEvent -> _businessEvent.getTrade())",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // Chained feature calls
    // =========================================================================

    @Test
    void chained_feature_calls_nest_correctly() {
        // trade -> contract -> quantity
        // Inner lambda uses _trade (receiver symbol name); outer uses "contract"
        // (inner feature name, no underscore) because no resolved type on inner
        var inner = new RFeatureCall();
        inner.setReceiver(symbolRef("trade"));
        inner.setFeatureName("contract");

        var outer = new RFeatureCall();
        outer.setReceiver(inner);
        outer.setFeatureName("quantity");

        assertEquals(
            "MapperS.of(trade).map(\"getContract\", _trade -> _trade.getContract())"
            + ".map(\"getQuantity\", contract -> contract.getQuantity())",
            render(handler.handle(outer, ctx, compiler)));
    }

    // =========================================================================
    // Feature call with resolved type — generic type param
    // =========================================================================

    @Test
    void feature_call_with_resolved_type_emits_generic_type_param() {
        var expr = new RFeatureCall();
        expr.setReceiver(symbolRef("trade"));
        expr.setFeatureName("price");
        expr.setResolvedFeature(attr("Price", 1, 1));

        // Lambda var is _trade (receiver symbol name), not _price
        assertEquals(
            "MapperS.of(trade).<Price>map(\"getPrice\", _trade -> _trade.getPrice())",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void feature_call_multi_valued_uses_mapC() {
        var expr = new RFeatureCall();
        expr.setReceiver(symbolRef("businessEvent"));
        expr.setFeatureName("instruction");
        expr.setResolvedFeature(attr("Instruction", 0, -1)); // (0..*)

        // Lambda var is _businessEvent (receiver symbol name), matching golden output
        assertEquals(
            "MapperS.of(businessEvent).<Instruction>mapC(\"getInstruction\", _businessEvent -> _businessEvent.getInstruction())",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void feature_call_bounded_multi_uses_mapC() {
        var expr = new RFeatureCall();
        expr.setReceiver(symbolRef("contract"));
        expr.setFeatureName("party");
        expr.setResolvedFeature(attr("Party", 0, 5)); // (0..5) — multi-valued

        assertEquals(
            "MapperS.of(contract).<Party>mapC(\"getParty\", _contract -> _contract.getParty())",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void feature_call_single_valued_uses_map() {
        var expr = new RFeatureCall();
        expr.setReceiver(symbolRef("product"));
        expr.setFeatureName("economicTerms");
        expr.setResolvedFeature(attr("EconomicTerms", 1, 1)); // (1..1)

        assertEquals(
            "MapperS.of(product).<EconomicTerms>map(\"getEconomicTerms\", _product -> _product.getEconomicTerms())",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void feature_call_optional_single_uses_map() {
        var expr = new RFeatureCall();
        expr.setReceiver(symbolRef("event"));
        expr.setFeatureName("meta");
        expr.setResolvedFeature(attr("MetaFields", 0, 1)); // (0..1)

        assertEquals(
            "MapperS.of(event).<MetaFields>map(\"getMeta\", _event -> _event.getMeta())",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void chained_feature_calls_with_resolved_types() {
        // product -> economicTerms -> payout (multi)
        // Inner: _product (receiver symbol name)
        // Outer: economicTerms (return type of inner, no underscore)
        var inner = new RFeatureCall();
        inner.setReceiver(symbolRef("product"));
        inner.setFeatureName("economicTerms");
        inner.setResolvedFeature(attr("EconomicTerms", 1, 1));

        var outer = new RFeatureCall();
        outer.setReceiver(inner);
        outer.setFeatureName("payout");
        outer.setResolvedFeature(attr("Payout", 0, -1));

        assertEquals(
            "MapperS.of(product).<EconomicTerms>map(\"getEconomicTerms\", _product -> _product.getEconomicTerms())"
            + ".<Payout>mapC(\"getPayout\", economicTerms -> economicTerms.getPayout())",
            render(handler.handle(outer, ctx, compiler)));
    }

    // =========================================================================
    // Deep feature call (double arrow)
    // =========================================================================

    @Test
    void deep_feature_call_without_receiver_type_emits_choose_placeholder() {
        // When the receiver is a plain symbol reference (not a feature call with a resolved type) the handler used
        // to emit a placeholder with the choose method name - `.map("choosePrice", _price -> /* TODO(M7b-4): wire
        // DeepPathUtil */ choosePrice(_price))`, a comment wearing a compile error. v3.2 seat 12 (D52, COUNTERS FIRST -
        // R2): the seat REFUSES by name instead (DEEP_PATH_UTIL_UNRESOLVED, the chaos M7b class; the heal is seat 13's).
        // The test keeps its name as the record of the placeholder it pinned.
        var expr = new RDeepFeatureCall();
        expr.setReceiver(symbolRef("trade"));
        expr.setFeatureName("price");

        var refusal = assertThrows(SilentDegradation.Refusal.class, () -> handler.handle(expr, ctx, compiler));
        assertEquals(SilentDegradation.Site.DEEP_PATH_UTIL_UNRESOLVED, refusal.site());
        assertTrue(refusal.getMessage().contains("deep path '->> price'"), refusal.getMessage());
        assertTrue(refusal.getMessage().contains("(receiver RSymbolReference)"), refusal.getMessage());
    }

    @Test
    void deep_feature_call_with_resolved_receiver_type_emits_deep_path_util() {
        // When the receiver is a feature call with a resolved type, the handler
        // can determine the DeepPathUtil field name and lambda variable.
        // Inner: _payout (receiver symbol name)
        var receiverFeature = new RFeatureCall();
        receiverFeature.setReceiver(symbolRef("payout"));
        receiverFeature.setFeatureName("underlier");
        receiverFeature.setResolvedFeature(attr("Asset", 0, 1));

        var expr = new RDeepFeatureCall();
        expr.setReceiver(receiverFeature);
        expr.setFeatureName("identifier");
        expr.setResolvedFeature(attr("AssetIdentifier", 0, -1));

        assertEquals(
            "MapperS.of(payout).<Asset>map(\"getUnderlier\", _payout -> _payout.getUnderlier())"
            + ".<AssetIdentifier>mapC(\"chooseIdentifier\", asset -> assetDeepPathUtil.chooseIdentifier(asset))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void deep_feature_call_with_resolved_single_valued_uses_map() {
        // Inner: _container (receiver symbol name)
        var receiverFeature = new RFeatureCall();
        receiverFeature.setReceiver(symbolRef("container"));
        receiverFeature.setFeatureName("product");
        receiverFeature.setResolvedFeature(attr("Product", 1, 1));

        var expr = new RDeepFeatureCall();
        expr.setReceiver(receiverFeature);
        expr.setFeatureName("economicTerms");
        expr.setResolvedFeature(attr("EconomicTerms", 1, 1));

        // facet deepPathParamEscape (PR #344): the deep-path lambda var scope-disambiguates
        // like every type-derived lambda var — the seeded `product` collision escapes the
        // PARAM (`_product`, golden AuxiliarEffectiveDate's form under a real collision)
        // while the DeepPathUtil FIELD keeps the bare type-derived name
        // (`productDeepPathUtil`, never `_productDeepPathUtil`). The non-colliding
        // deep-path pins above/below (`asset -> assetDeepPathUtil...`) stay bare.
        assertEquals(
            "MapperS.of(container).<Product>map(\"getProduct\", _container -> _container.getProduct())"
            + ".<EconomicTerms>map(\"chooseEconomicTerms\", _product -> productDeepPathUtil.chooseEconomicTerms(_product))",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // Lambda naming — receiver-type resolution
    // =========================================================================

    @Test
    void lambda_var_uses_resolved_symbol_type_when_available() {
        // When the symbol reference has a resolved symbol (RAttribute with type),
        // the lambda var should use the type name, not the symbol name.
        var businessEvent = symbolRef("businessEvent");
        var inputAttr = attr("BusinessEvent", 1, 1);
        inputAttr.setName("businessEvent");
        businessEvent.setResolvedSymbol(inputAttr);

        var expr = new RFeatureCall();
        expr.setReceiver(businessEvent);
        expr.setFeatureName("instruction");
        expr.setResolvedFeature(attr("Instruction", 0, -1));

        // Golden: MapperS.of(businessEvent).<Instruction>mapC("getInstruction", _businessEvent -> ...)
        assertEquals(
            "MapperS.of(businessEvent).<Instruction>mapC(\"getInstruction\", _businessEvent -> _businessEvent.getInstruction())",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void triple_chain_golden_pattern() {
        // Matches golden: businessEvent -> instruction -> primitiveInstruction -> transfer
        // Step 1: _businessEvent (from symbol ref)
        // Step 2: instruction (from Instruction return type, no underscore)
        // Step 3: primitiveInstruction (from PrimitiveInstruction return type, no underscore)
        var step1 = new RFeatureCall();
        step1.setReceiver(symbolRef("businessEvent"));
        step1.setFeatureName("instruction");
        step1.setResolvedFeature(attr("Instruction", 0, -1));

        var step2 = new RFeatureCall();
        step2.setReceiver(step1);
        step2.setFeatureName("primitiveInstruction");
        step2.setResolvedFeature(attr("PrimitiveInstruction", 1, 1));

        var step3 = new RFeatureCall();
        step3.setReceiver(step2);
        step3.setFeatureName("transfer");
        step3.setResolvedFeature(attr("TransferInstruction", 1, 1));

        assertEquals(
            "MapperS.of(businessEvent).<Instruction>mapC(\"getInstruction\", _businessEvent -> _businessEvent.getInstruction())"
            + ".<PrimitiveInstruction>map(\"getPrimitiveInstruction\", instruction -> instruction.getPrimitiveInstruction())"
            + ".<TransferInstruction>map(\"getTransfer\", primitiveInstruction -> primitiveInstruction.getTransfer())",
            render(handler.handle(step3, ctx, compiler)));
    }

    @Test
    void resolveLambdaVarName_returns_underscore_prefix_for_symbol_ref() {
        var ref = symbolRef("trade");
        assertEquals("_trade", resolvedLambdaVar(ref, "anything"));
    }

    @Test
    void resolveLambdaVarName_uses_resolved_type_for_symbol_ref() {
        var ref = symbolRef("myVar");
        var inputAttr = attr("BusinessEvent", 1, 1);
        inputAttr.setName("myVar");
        ref.setResolvedSymbol(inputAttr);

        // Uses the type name (BusinessEvent → _businessEvent), not the variable name
        assertEquals("_businessEvent", resolvedLambdaVar(ref, "anything"));
    }

    @Test
    void resolveLambdaVarName_chained_feature_call_collision_free_keeps_bare_name() {
        var fc = new RFeatureCall();
        fc.setFeatureName("instruction");
        fc.setResolvedFeature(attr("Instruction", 0, -1));

        assertEquals("instruction", resolvedLambdaVar(fc, "anything"));
    }

    @Test
    void chained_lambda_var_escapes_on_scope_collision() {
        // facet alias_receiver_typing (mechanism 5) — the drr IsFXForward corpus
        // form: a chained step's type-derived lambda var ("product", from the
        // previous step's Product return type) collides with the seeded input
        // name "product"; upstream's createUniqueIdentifier ALWAYS escapes (a
        // shadowing lambda param does not compile), so the var renders "_product".
        var fc = new RFeatureCall();
        fc.setFeatureName("underlier");
        fc.setResolvedFeature(attr("Product", 1, 1));

        assertEquals("_product", resolvedLambdaVar(fc, "anything"));
    }

    @Test
    void resolveLambdaVarName_strips_qualifier_for_cross_namespace_symbol_type() {
        // A cross-namespace input type carries a QUALIFIED typeCall().typeName()
        // (e.g. "cde.payment.PeriodicPayment"); the lambda variable must derive
        // from the SIMPLE name. Emitting the qualifier verbatim
        // ("cde.payment.PeriodicPayment -> cde.payment.PeriodicPayment.getX()") is a
        // non-compiling Java identifier, and upstream never does so. Without the
        // qualifier strip the var would be the whole FQN (the package segment already
        // starts lowercase, so the char-0 lowercase left it unchanged). No collision
        // with the seeded scope → no leading underscore (matches golden, e.g.
        // DayCountConvLeg1Fixed_Validation's `periodicPayment`).
        var ref = symbolRef("periodicPaymentLeg1");
        var inputAttr = attr("cde.payment.PeriodicPayment", 1, 1);
        inputAttr.setName("periodicPaymentLeg1");
        ref.setResolvedSymbol(inputAttr);

        assertEquals("periodicPayment",
                resolvedLambdaVar(ref, "anything"));
    }

    @Test
    void resolveLambdaVarName_strips_qualifier_for_chained_feature_call() {
        // Same qualifier-strip on the chained (RFeatureCall) branch — the previous
        // step's resolved type is the FpML-ingest qualified name "fpml.Money"; the
        // next step's lambda var must be the simple `money`, not `fpml.Money`.
        var fc = new RFeatureCall();
        fc.setFeatureName("money");
        fc.setResolvedFeature(attr("fpml.Money", 0, -1));

        assertEquals("money", resolvedLambdaVar(fc, "anything"));
    }

    @Test
    void resolveLambdaVarName_falls_back_to_feature_name_for_unknown_receiver() {
        // RImplicitVariable with NO enclosing rule (no parent) → fallback. The
        // engine-PR-4 facet-C branch only fires when an enclosing rule with a
        // from-type is reachable; without one it falls through to the default.
        var implicit = new com.regnosys.rosetta.ast.expressions.references.RImplicitVariable();
        assertEquals("_myFeature", resolvedLambdaVar(implicit, "myFeature"));
    }

    @Test
    void resolveLambdaVarName_uses_rule_from_type_for_implicit_variable() {
        // Engine PR #4 / facet C: a synthesized implicit-input chain head inside a
        // rule lambda is an RImplicitVariable; the first navigation step's lambda
        // var derives from the enclosing rule's from-type (item-type == from-type
        // for the cases this fires on). No collision with the seeded scope, so no
        // leading underscore — matches golden (e.g. EnrichmentDataRule's
        // `transactionReportInstruction`).
        var fromTypeCall = new RTypeCall();
        fromTypeCall.setTypeName("TransactionReportInstruction");
        var rule = new com.regnosys.rosetta.ast.functions.RRule();
        rule.setFromType(fromTypeCall);

        var implicit = new com.regnosys.rosetta.ast.expressions.references.RImplicitVariable();
        implicit.setParent(rule);

        assertEquals("transactionReportInstruction",
                resolvedLambdaVar(implicit, "anything"));
    }

    // =========================================================================
    // Dispatch via ExpressionCompiler
    // =========================================================================

    @Test
    void compiler_dispatches_feature_call() {
        var expr = new RFeatureCall();
        expr.setReceiver(symbolRef("event"));
        expr.setFeatureName("timestamp");

        var scope  = seededScope();
        var result = compiler.compile(expr, null, scope);

        assertInstanceOf(JavaExpression.class, result);
        // Lambda var is _event (receiver symbol name), not _timestamp — resolved
        // through the LOCAL scope (facet lambdaNaming M3/M4 deferred sentinels).
        assertEquals(
            "MapperS.of(event).map(\"getTimestamp\", _event -> _event.getTimestamp())",
            scope.resolveDeferredCoercionNames(((JavaExpression) result).renderToString()));
    }

    @Test
    void compiler_dispatches_deep_feature_call() {
        var expr = new RDeepFeatureCall();
        expr.setReceiver(symbolRef("contract"));
        expr.setFeatureName("amount");

        var scope  = seededScope();
        // the compiler dispatches to the handler, and the handler's R2 refusal (v3.2 seat 12) surfaces through the
        // dispatch unchanged - the pre-seat pin was the `TODO(M7b-4)` placeholder render
        var refusal = assertThrows(SilentDegradation.Refusal.class, () -> compiler.compile(expr, null, scope));
        assertEquals(SilentDegradation.Site.DEEP_PATH_UTIL_UNRESOLVED, refusal.site());
        assertTrue(refusal.getMessage().contains("deep path '->> amount'"), refusal.getMessage());
    }

    // =========================================================================
    // Metafield wrapper generic type param
    // =========================================================================

    @Test
    void feature_call_with_metadata_scheme_wraps_type_as_FieldWithMeta() {
        // [metadata scheme] → generic param is FieldWithMeta<TypeName>
        var expr = new RFeatureCall();
        expr.setReceiver(symbolRef("trade"));
        expr.setFeatureName("currency");
        expr.setResolvedFeature(withMetadata(attr("String", 0, 1), "scheme"));

        assertEquals(
            "MapperS.of(trade).<FieldWithMetaString>map(\"getCurrency\", _trade -> _trade.getCurrency())",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void feature_call_with_metadata_id_wraps_type_as_FieldWithMeta() {
        // [metadata id] → generic param is FieldWithMeta<TypeName>
        var expr = new RFeatureCall();
        expr.setReceiver(symbolRef("adjustableOrAdjustedOrRelativeDate"));
        expr.setFeatureName("adjustedDate");
        expr.setResolvedFeature(withMetadata(attr("Date", 0, 1), "id"));

        assertEquals(
            "MapperS.of(adjustableOrAdjustedOrRelativeDate).<FieldWithMetaDate>map("
                + "\"getAdjustedDate\", _adjustableOrAdjustedOrRelativeDate -> "
                + "_adjustableOrAdjustedOrRelativeDate.getAdjustedDate())",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void feature_call_with_metadata_reference_wraps_type_as_ReferenceWithMeta() {
        // [metadata reference] → generic param is ReferenceWithMeta<TypeName>.
        // Local scope seeding "instruction" (a same-named input, the real
        // collision shape) — the shared fixture seed deliberately omits it so the
        // chained golden patterns stay collision-free (see seededScope()).
        var expr = new RFeatureCall();
        expr.setReceiver(symbolRef("instruction"));
        expr.setFeatureName("businessDayAdjustments");
        expr.setResolvedFeature(withMetadata(attr("BusinessDayAdjustments", 0, 1), "reference"));

        JavaStatementScope collide = ExpressionCompilerTest.createTestScope();
        collide.createUniqueIdentifier("instruction");
        var collideCtx = ExpressionContext.of(null, collide);
        assertEquals(
            "MapperS.of(instruction).<ReferenceWithMetaBusinessDayAdjustments>map("
                + "\"getBusinessDayAdjustments\", _instruction -> "
                + "_instruction.getBusinessDayAdjustments())",
            collide.resolveDeferredCoercionNames(renderRaw(handler.handle(expr, collideCtx, compiler))));
    }

    @Test
    void feature_call_with_metadata_address_wraps_type_as_ReferenceWithMeta() {
        // [metadata address] → generic param is ReferenceWithMeta<TypeName>
        var expr = new RFeatureCall();
        expr.setReceiver(symbolRef("container"));
        expr.setFeatureName("target");
        expr.setResolvedFeature(withMetadata(attr("Asset", 0, 1), "address"));

        assertEquals(
            "MapperS.of(container).<ReferenceWithMetaAsset>map("
                + "\"getTarget\", _container -> _container.getTarget())",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void feature_call_with_metadata_location_wraps_type_as_FieldWithMeta() {
        // [metadata location] → generic param is FieldWithMeta<TypeName>
        var expr = new RFeatureCall();
        expr.setReceiver(symbolRef("trade"));
        expr.setFeatureName("price");
        expr.setResolvedFeature(withMetadata(attr("Price", 1, 1), "location"));

        assertEquals(
            "MapperS.of(trade).<FieldWithMetaPrice>map(\"getPrice\", _trade -> _trade.getPrice())",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void feature_call_with_metadata_key_leaves_type_unwrapped() {
        // [metadata key] is a type-level annotation — does not affect navigation wrapping.
        var expr = new RFeatureCall();
        expr.setReceiver(symbolRef("trade"));
        expr.setFeatureName("price");
        expr.setResolvedFeature(withMetadata(attr("Price", 1, 1), "key"));

        assertEquals(
            "MapperS.of(trade).<Price>map(\"getPrice\", _trade -> _trade.getPrice())",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void feature_call_with_metadata_template_leaves_type_unwrapped() {
        // [metadata template] is a type-level annotation — does not affect navigation wrapping.
        var expr = new RFeatureCall();
        expr.setReceiver(symbolRef("trade"));
        expr.setFeatureName("price");
        expr.setResolvedFeature(withMetadata(attr("Price", 1, 1), "template"));

        assertEquals(
            "MapperS.of(trade).<Price>map(\"getPrice\", _trade -> _trade.getPrice())",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void feature_call_without_metadata_remains_unwrapped_regression() {
        // Regression guard: attributes with no annotation emit the bare type name.
        var expr = new RFeatureCall();
        expr.setReceiver(symbolRef("trade"));
        expr.setFeatureName("price");
        expr.setResolvedFeature(attr("Price", 1, 1));

        assertEquals(
            "MapperS.of(trade).<Price>map(\"getPrice\", _trade -> _trade.getPrice())",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void feature_call_with_dual_metadata_prefers_reference_wrapper() {
        // When an attribute carries BOTH reference-family and field-family annotations,
        // ReferenceWithMeta wins. This mirrors MetaFieldGenerator.detectMetaKind precedence
        // and must be stable so generated code matches the POJO getter's declared return type.
        var attrWithBoth = withMetadata(withMetadata(attr("Date", 0, 1), "scheme"), "reference");

        var expr = new RFeatureCall();
        expr.setReceiver(symbolRef("trade"));
        expr.setFeatureName("effectiveDate");
        expr.setResolvedFeature(attrWithBoth);

        assertEquals(
            "MapperS.of(trade).<ReferenceWithMetaDate>map("
                + "\"getEffectiveDate\", _trade -> _trade.getEffectiveDate())",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void deep_feature_call_with_metadata_scheme_wraps_type_as_FieldWithMeta() {
        // Deep feature calls must also apply the metafield wrapper.
        var receiverFeature = new RFeatureCall();
        receiverFeature.setReceiver(symbolRef("payout"));
        receiverFeature.setFeatureName("underlier");
        receiverFeature.setResolvedFeature(attr("Asset", 0, 1));

        var expr = new RDeepFeatureCall();
        expr.setReceiver(receiverFeature);
        expr.setFeatureName("identifier");
        expr.setResolvedFeature(withMetadata(attr("String", 0, -1), "scheme"));

        assertEquals(
            "MapperS.of(payout).<Asset>map(\"getUnderlier\", _payout -> _payout.getUnderlier())"
            + ".<FieldWithMetaString>mapC(\"chooseIdentifier\", asset -> assetDeepPathUtil.chooseIdentifier(asset))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void deep_feature_call_with_metadata_id_wraps_type_as_FieldWithMeta() {
        var receiverFeature = new RFeatureCall();
        receiverFeature.setReceiver(symbolRef("payout"));
        receiverFeature.setFeatureName("underlier");
        receiverFeature.setResolvedFeature(attr("Asset", 0, 1));

        var expr = new RDeepFeatureCall();
        expr.setReceiver(receiverFeature);
        expr.setFeatureName("adjustedDate");
        expr.setResolvedFeature(withMetadata(attr("Date", 0, 1), "id"));

        assertEquals(
            "MapperS.of(payout).<Asset>map(\"getUnderlier\", _payout -> _payout.getUnderlier())"
            + ".<FieldWithMetaDate>map(\"chooseAdjustedDate\", asset -> assetDeepPathUtil.chooseAdjustedDate(asset))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void deep_feature_call_with_metadata_location_wraps_type_as_FieldWithMeta() {
        var receiverFeature = new RFeatureCall();
        receiverFeature.setReceiver(symbolRef("payout"));
        receiverFeature.setFeatureName("underlier");
        receiverFeature.setResolvedFeature(attr("Asset", 0, 1));

        var expr = new RDeepFeatureCall();
        expr.setReceiver(receiverFeature);
        expr.setFeatureName("market");
        expr.setResolvedFeature(withMetadata(attr("String", 0, 1), "location"));

        assertEquals(
            "MapperS.of(payout).<Asset>map(\"getUnderlier\", _payout -> _payout.getUnderlier())"
            + ".<FieldWithMetaString>map(\"chooseMarket\", asset -> assetDeepPathUtil.chooseMarket(asset))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void deep_feature_call_with_metadata_reference_wraps_type_as_ReferenceWithMeta() {
        var receiverFeature = new RFeatureCall();
        receiverFeature.setReceiver(symbolRef("payout"));
        receiverFeature.setFeatureName("underlier");
        receiverFeature.setResolvedFeature(attr("Asset", 0, 1));

        var expr = new RDeepFeatureCall();
        expr.setReceiver(receiverFeature);
        expr.setFeatureName("businessDayAdjustments");
        expr.setResolvedFeature(withMetadata(attr("BusinessDayAdjustments", 0, 1), "reference"));

        assertEquals(
            "MapperS.of(payout).<Asset>map(\"getUnderlier\", _payout -> _payout.getUnderlier())"
            + ".<ReferenceWithMetaBusinessDayAdjustments>map(\"chooseBusinessDayAdjustments\", "
            + "asset -> assetDeepPathUtil.chooseBusinessDayAdjustments(asset))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void deep_feature_call_with_metadata_address_wraps_type_as_ReferenceWithMeta() {
        var receiverFeature = new RFeatureCall();
        receiverFeature.setReceiver(symbolRef("payout"));
        receiverFeature.setFeatureName("underlier");
        receiverFeature.setResolvedFeature(attr("Asset", 0, 1));

        var expr = new RDeepFeatureCall();
        expr.setReceiver(receiverFeature);
        expr.setFeatureName("target");
        expr.setResolvedFeature(withMetadata(attr("Instrument", 0, 1), "address"));

        assertEquals(
            "MapperS.of(payout).<Asset>map(\"getUnderlier\", _payout -> _payout.getUnderlier())"
            + ".<ReferenceWithMetaInstrument>map(\"chooseTarget\", asset -> assetDeepPathUtil.chooseTarget(asset))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void deep_feature_call_with_metadata_key_leaves_type_unwrapped() {
        // [metadata key] on the deep feature must NOT be wrapped — it's type-level only.
        var receiverFeature = new RFeatureCall();
        receiverFeature.setReceiver(symbolRef("payout"));
        receiverFeature.setFeatureName("underlier");
        receiverFeature.setResolvedFeature(attr("Asset", 0, 1));

        var expr = new RDeepFeatureCall();
        expr.setReceiver(receiverFeature);
        expr.setFeatureName("instrument");
        expr.setResolvedFeature(withMetadata(attr("Instrument", 0, 1), "key"));

        assertEquals(
            "MapperS.of(payout).<Asset>map(\"getUnderlier\", _payout -> _payout.getUnderlier())"
            + ".<Instrument>map(\"chooseInstrument\", asset -> assetDeepPathUtil.chooseInstrument(asset))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void deep_feature_call_without_metadata_remains_unwrapped_regression() {
        // Regression guard for the deep path: no annotation → bare type name.
        var receiverFeature = new RFeatureCall();
        receiverFeature.setReceiver(symbolRef("payout"));
        receiverFeature.setFeatureName("underlier");
        receiverFeature.setResolvedFeature(attr("Asset", 0, 1));

        var expr = new RDeepFeatureCall();
        expr.setReceiver(receiverFeature);
        expr.setFeatureName("instrument");
        expr.setResolvedFeature(attr("Instrument", 0, 1));

        assertEquals(
            "MapperS.of(payout).<Asset>map(\"getUnderlier\", _payout -> _payout.getUnderlier())"
            + ".<Instrument>map(\"chooseInstrument\", asset -> assetDeepPathUtil.chooseInstrument(asset))",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // PR-A §9.1 C3a.4.f — refs-carrying invariant tests
    // =========================================================================

    @Test
    void single_feature_call_single_valued_carries_mapper_s_ref() {
        var expr = new RFeatureCall();
        expr.setReceiver(symbolRef("trade"));
        expr.setFeatureName("price");
        expr.setResolvedFeature(attr("Price", 0, 1));

        var result = handler.handle(expr, ctx, compiler);
        Set<?> refs = result.getRefs();
        // MAPPER_S flows up from the receiver's MapperS.of(trade) static-factory
        // leaf — NOT added by the handler (the .map() method needs no import).
        assertTrue(refs.contains(HandlerHelper.MAPPER_S),
                "Receiver MapperS.of(trade) carries MAPPER_S");
        assertFalse(refs.contains(HandlerHelper.MAPPER_C),
                "Single-valued feature call must NOT carry MAPPER_C ref");
    }

    @Test
    void single_feature_call_multi_valued_does_not_import_mapper_c() {
        var expr = new RFeatureCall();
        expr.setReceiver(symbolRef("businessEvent"));
        expr.setFeatureName("instruction");
        expr.setResolvedFeature(attr("Instruction", 0, -1));  // unbounded = multi

        var result = handler.handle(expr, ctx, compiler);
        Set<?> refs = result.getRefs();
        // The `.mapC(...)` is an INSTANCE method on the receiver mapper — it needs
        // no MapperC class import. The legacy plugin never imports MapperC for a
        // .mapC() method call. The only mapper import is MAPPER_S, flowing up from
        // the receiver's MapperS.of(businessEvent) leaf. (Phase X1 imports knock-on.)
        assertFalse(refs.contains(HandlerHelper.MAPPER_C),
                "Instance .mapC() must NOT import MapperC — golden never does");
        assertTrue(refs.contains(HandlerHelper.MAPPER_S),
                "Receiver MapperS.of(businessEvent) carries MAPPER_S");
    }

    @Test
    void deep_feature_call_single_valued_carries_mapper_s_ref() {
        var receiverFeature = new RFeatureCall();
        receiverFeature.setReceiver(symbolRef("trade"));
        receiverFeature.setFeatureName("underlier");
        receiverFeature.setResolvedFeature(attr("Asset", 0, 1));

        var expr = new RDeepFeatureCall();
        expr.setReceiver(receiverFeature);
        expr.setFeatureName("instrument");
        expr.setResolvedFeature(attr("Instrument", 0, 1));

        var result = handler.handle(expr, ctx, compiler);
        Set<?> refs = result.getRefs();
        // MAPPER_S flows up from the chain's MapperS.of(trade) leaf.
        assertTrue(refs.contains(HandlerHelper.MAPPER_S),
                "Receiver chain MapperS.of(trade)... carries MAPPER_S");
    }

    @Test
    void deep_feature_call_multi_valued_does_not_import_mapper_c() {
        var receiverFeature = new RFeatureCall();
        receiverFeature.setReceiver(symbolRef("trade"));
        receiverFeature.setFeatureName("identifiers");
        receiverFeature.setResolvedFeature(attr("Asset", 0, -1));

        var expr = new RDeepFeatureCall();
        expr.setReceiver(receiverFeature);
        expr.setFeatureName("identifier");
        expr.setResolvedFeature(attr("Identifier", 0, -1));

        var result = handler.handle(expr, ctx, compiler);
        Set<?> refs = result.getRefs();
        // Same as the single-arrow path: instance .mapC() imports no MapperC.
        assertFalse(refs.contains(HandlerHelper.MAPPER_C),
                "Instance .mapC() on a deep call must NOT import MapperC");
        assertTrue(refs.contains(HandlerHelper.MAPPER_S),
                "Receiver chain MapperS.of(trade)... carries MAPPER_S");
    }

    @Test
    void feature_call_with_null_translator_skips_witness_ref_no_crash() {
        // Defensive: the witness-import bridge needs a JavaTypeTranslator +
        // GeneratorModel. The lightweight unit ExpressionCompiler supplies neither,
        // so addWitnessTypeRef must early-return without registering a ref or
        // throwing. (Witness registration itself is verified via the D11 byte-diff,
        // which exercises the real generator infrastructure.)
        var expr = new RFeatureCall();
        expr.setReceiver(symbolRef("trade"));
        expr.setFeatureName("price");
        expr.setResolvedFeature(attr("Price", 0, 1));

        var result = handler.handle(expr, ctx, compiler);
        Set<?> refs = result.getRefs();
        // Only the receiver-carried MAPPER_S; no domain witness ref (Price) since
        // the translator is null in this harness.
        assertTrue(refs.contains(HandlerHelper.MAPPER_S));
        assertEquals(1, refs.size(),
                "With a null translator, only the receiver MAPPER_S ref is present");
    }

    // =========================================================================
    // M7b-3 typed pipeline — builtin witness Java-simple-name resolution
    // =========================================================================

    @Test
    void feature_call_builtin_witness_falls_back_to_rune_name_without_translator() {
        // The M7b-3 witness fix (engine PR #7) resolves a lowercase Rune builtin
        // type (string / boolean / number / zonedDateTime / ...) to its Java simple
        // name (String / Boolean / BigDecimal / ZonedDateTime) for the <Type> generic
        // witness — the legacy plugin emits the getter's Java return type, not the raw
        // Rune name. That resolution needs the compiler's JavaTypeTranslator +
        // GeneratorModel; the stateless unit ExpressionCompiler supplies neither, so
        // resolveJavaSimpleName falls back to the raw Rune name WITHOUT crashing.
        // The real Rune->Java mapping (e.g. <zonedDateTime> -> <ZonedDateTime>) is
        // exercised + verified by the D11 byte-diff — engine PR #7 flipped 38 files
        // (22 drr POJO + 16 FUNCTION; the 16 = 14 drr projection Get* + 2 cdm
        // EquityNotionalAmount across both cdm cells) on exactly this fix. This test pins the
        // null-translator fallback so the lowercase gate cannot regress to an NPE.
        var expr = new RFeatureCall();
        expr.setReceiver(symbolRef("trade"));
        expr.setFeatureName("timestamp");
        expr.setResolvedFeature(attr("zonedDateTime", 0, 1));  // lowercase Rune builtin

        assertEquals(
            "MapperS.of(trade).<zonedDateTime>map(\"getTimestamp\", _trade -> _trade.getTimestamp())",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void feature_call_pascalcase_type_witness_unchanged_by_gate() {
        // Regression guard for the lowercase gate: a PascalCase type (data type /
        // enum / type-alias) is NOT a direct builtin, so resolveJavaSimpleName
        // returns the Rune name verbatim — the witness is byte-identical to the
        // pre-M7b-3 output regardless of translator availability. This is why the
        // change is byte-flat on the green cdm cells (their navigation witnesses are
        // all PascalCase data types / enums).
        var expr = new RFeatureCall();
        expr.setReceiver(symbolRef("trade"));
        expr.setFeatureName("price");
        expr.setResolvedFeature(attr("Price", 1, 1));  // PascalCase data type

        assertEquals(
            "MapperS.of(trade).<Price>map(\"getPrice\", _trade -> _trade.getPrice())",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // PR #117 — chained-navigation lambda var from receiver TYPE, not feature name
    // =========================================================================

    @Test
    void chained_feature_call_outer_lambda_var_from_resolved_type_not_feature_name() {
        // Facet (PR #117): in a `.map` chain the NEXT step's receiver lambda var is
        // the lowerCamel of the PREVIOUS step's resolved TYPE, not the feature name.
        // The two coincide when attribute name == type name (most CDM maps), but
        // diverge for `fixedRate : FixedRateSpecification` — golden names the outer
        // lambda var `fixedRateSpecification`, NOT `fixedRate`. "product" is seeded,
        // so the inner symbol-ref lambda var collides → `_product`.
        var inner = new RFeatureCall();
        inner.setReceiver(symbolRef("product"));
        inner.setFeatureName("fixedRate");
        inner.setResolvedFeature(attr("FixedRateSpecification", 1, 1));

        var outer = new RFeatureCall();
        outer.setReceiver(inner);
        outer.setFeatureName("rateSchedule");
        outer.setResolvedFeature(attr("RateSchedule", 1, 1));

        assertEquals(
            "MapperS.of(product).<FixedRateSpecification>map(\"getFixedRate\", _product -> _product.getFixedRate())"
            + ".<RateSchedule>map(\"getRateSchedule\", fixedRateSpecification -> fixedRateSpecification.getRateSchedule())",
            render(handler.handle(outer, ctx, compiler)));
    }

    @Test
    void resolveLambdaVarName_chained_uses_resolved_type_not_feature_name() {
        // Direct check: the lambda var for the step navigating FROM this feature call
        // is the previous feature's TYPE (FixedRateSpecification → fixedRateSpecification),
        // not its feature name (fixedRate).
        var fc = new RFeatureCall();
        fc.setFeatureName("fixedRate");
        fc.setResolvedFeature(attr("FixedRateSpecification", 1, 1));

        assertEquals("fixedRateSpecification",
                resolvedLambdaVar(fc, "rateSchedule"));
    }

    @Test
    void resolveLambdaVarName_chained_falls_back_to_feature_name_when_type_unresolvable() {
        // The PR #117 fix routes the empty-resolvedFeature case through the same
        // fallbackResolveFeature the witness/map-method paths use — a type-scoped chain
        // walk that needs the generator workspace to resolve a receiver's RDataType.
        // The stateless unit ExpressionCompiler supplies no workspace, so the walk
        // cannot resolve and the branch degrades gracefully to the feature name (no
        // crash, no behavioural change vs the pre-fix `return fc.featureName()`). The
        // REAL resolution — e.g. `fixedRate : FixedRateSpecification` →
        // `fixedRateSpecification` — is exercised + verified by the D11 byte-diff (this
        // PR flipped 11 FUNCTION on exactly that fix). This test pins the null-workspace
        // degradation so the new orElseGet branch cannot regress to an NPE.
        var inner = new RFeatureCall();
        inner.setReceiver(symbolRef("rateSpecification")); // no resolved symbol → unresolvable here
        inner.setFeatureName("fixedRate");                 // NO resolvedFeature

        assertEquals("fixedRate",
                resolvedLambdaVar(inner, "rateSchedule"));
    }
}
