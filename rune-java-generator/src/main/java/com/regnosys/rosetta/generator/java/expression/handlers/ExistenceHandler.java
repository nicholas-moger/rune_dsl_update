package com.regnosys.rosetta.generator.java.expression.handlers;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.enums.CardCheckOp;
import com.regnosys.rosetta.ast.enums.ExistenceOp;
import com.regnosys.rosetta.ast.enums.ExistsModifier;
import com.regnosys.rosetta.ast.enums.Necessity;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.ROnlyExistsElement;
import com.regnosys.rosetta.ast.expressions.unary.RCardinalityCheckExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExistenceExpr;
import com.regnosys.rosetta.ast.expressions.unary.ROnlyExistsExpr;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.statement.builder.JavaConditionalExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Handles code generation for existence and cardinality check expressions:
 * {@link RExistenceExpr}, {@link ROnlyExistsExpr}, and {@link RCardinalityCheckExpr}.
 *
 * <p>Golden output patterns (verified against CDM golden files):
 * <pre>
 *   expression exists           →  exists(argument)
 *   expression is absent        →  notExists(argument)
 *   single expression exists    →  singleExists(argument)
 *   multiple expression exists  →  multipleExists(argument)
 *   (e1, e2) only exists        →  onlyExists(root, Arrays.asList("all fields"), Arrays.asList("selected"))
 *   expression one-of           →  choice(argument, Arrays.asList("attr1","attr2"), ChoiceRuleValidationMethod.REQUIRED)
 *   expression optional choice  →  choice(argument, Arrays.asList("attr1","attr2"), ChoiceRuleValidationMethod.OPTIONAL)
 * </pre>
 *
 * <p>Static calls are from
 * {@code com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe} via
 * {@code import static ExpressionOperatorsNullSafe.*;}.
 */
public class ExistenceHandler {

    // =========================================================================
    // Existence expressions (exists / absent)
    // =========================================================================

    /**
     * Compiles an existence check expression into a static method call.
     *
     * <p>The output method depends on the operation and optional modifier:
     * <ul>
     *   <li>{@code EXISTS} with no modifier → {@code exists(arg)}</li>
     *   <li>{@code EXISTS} with {@code SINGLE} → {@code singleExists(arg)}</li>
     *   <li>{@code EXISTS} with {@code MULTIPLE} → {@code multipleExists(arg)}</li>
     *   <li>{@code ABSENT} → {@code notExists(arg)}</li>
     * </ul>
     *
     * @param expr     the existence expression node
     * @param ctx      the current expression compilation context
     * @param compiler the parent compiler for recursive compilation of the argument
     * @return a {@link JavaExpression} rendering the existence check call
     */
    public JavaStatementBuilder handle(RExistenceExpr expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        // PR-A §9.1 C3a.4.d: exists / notExists / singleExists / multipleExists
        // are all static methods on ExpressionOperatorsNullSafe → carry the
        // wildcard structurally. No library class refs (argument refs flow
        // through via the operand builder union).
        // facet implicit_operand_synthesis (PR #218): a `then exists` / `filter exists`
        // bare existence elides its argument — the piped value / lambda item — so
        // argument() is null; substitute the synthetic implicit (→ thenArg / item).
        RExpression operand = HandlerHelper.orSyntheticImplicit(expr.argument(), expr);

        // v3.2 seat 9 (PR #630, F8 / D47 - the `nothing` render law at the mapper seat): upstream compiles the
        // operand and then COERCES it to `Mapper<? extends X>` (caseExistsOperation -> javaCode(expected) ->
        // addCoercions), whose early exit turns a Void-item operand into the expected type's EMPTY -
        // `exists(MapperS.<Void>ofNull())` - DISCARDING the compiled render (the getter chain, the bare input, the
        // MapperC list): the oracle groups void-mapping-basic-record-edge (ConditionCarrierTokPresent) and
        // void-mapping-render-edge (InputExists / InputAbsent / InputSingleExists, CarrierTokAbsent /
        // CarrierToksExist). The fork's compiled operand carries no Java type here, so the Void verdict is read
        // from the front end through the translator and the coercer's OWN predicate (voidOperandEmptyOrNull) BEFORE
        // any compile, and a Void operand is NEVER compiled - round 2 (cq MF-1 / spec MF-2), decided by the oracle group
        // void-mapping-render-hoist-second's ExistsThenConditional: upstream's discarded builder consumes NO name (the
        // kept conditional after it is the bare `ifThenElseResult`), where round 1's compile-then-drain removed the
        // discarded hoist's LOCAL but left its NAME registration on the per-method session, one number too many for a
        // later hoist. That group's ExistsThenConditional carries the shape but is a DECLARED byte pin the bar tolerates either way;
        // the law's WITNESS is round 3's void-mapping-exists-then-clean (the Void exists operand SET beside a pathed string
        // conditional - both sides the plain `String ifThenElseResult` at index 0, a compiling, byte-matching golden), which
        // lane L14 (round 1's single compile re-introduced before the verdict) numbers `ifThenElseResult1` and turns red ALONE.
        // The law is about a BARE conditional, coerced upstream before any collapse - a value that collapses INSIDE the
        // discarded builder consumes its name on both sides (FunctionExpressionRenderer's general path, round 3). The
        // IR route's scalar-parameter existence claim DECLINES a Void operand to this seat (LAW 77, ONE site) -
        // for the EXPLICIT-argument form alone: the elided form's synthetic implicit operand has no inference
        // (MISSING at the workspace), so the Void law never reaches `then exists` / a bare `filter … exists` on
        // either route (round 1, cq SF-5 - BANKED with the gen-2 seeds).
        JavaStatementBuilder voidOperand = voidOperandEmptyOrNull(operand, compiler);
        if (voidOperand != null) {
            Set<JavaClass<?>> voidRefs = new HashSet<>(voidOperand.getRefs());
            Set<JavaClass<?>> voidWildcards = new HashSet<>();
            voidWildcards.add(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE);
            return JavaExpression.from(
                    existenceMethod(expr) + "(" + HandlerHelper.render(voidOperand) + ")",
                    null,
                    voidRefs,
                    voidWildcards);
        }
        JavaStatementBuilder argBuilder = compiler.compile(operand, ctx.expectedType(), ctx.scope());

        // facet existsOperandMetaWrap (PR #315): if the operand compiled to the meta-wrap null-safe
        // conditional (ReferenceHandler.renderImplicitRuleInvocation produced a
        // JavaConditionalExpression whose item type is a meta wrapper, for a meta-typed inner-rule
        // exists/notExists operand), distribute exists/notExists into BOTH ternary branches via
        // mapExpression — so the downstream getOrDefault/asMapper coercers (which apply via
        // mapExpression) distribute too — matching golden's convertNullSafe form
        // `(x == null ? exists(MapperS.<W>ofNull()) : exists(MapperS.of(W.builder().setValue(x).build())))`.
        // Gated on the conditional's item type being a meta wrapper (the #315 marker), so a plain
        // conditional operand keeps the string-wrap path below. Green-safe by construction: the
        // fork's flat `exists(MapperS.of(<call>))` never byte-matched golden (the distributed
        // wrap form is a semantic round-trip a green file cannot carry).
        if (argBuilder instanceof JavaConditionalExpression metaCond
                && isMetaWrapperConditional(metaCond, compiler)) {
            String existMethod = existenceMethod(expr);
            return metaCond.mapExpression(e -> {
                Set<JavaClass<?>> w = new HashSet<>(e.getStaticWildcardImports());
                w.add(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE);
                return JavaExpression.from(
                        existMethod + "(" + e.renderToString() + ")",
                        HandlerHelper.COMPARISON_RESULT,
                        e.getRefs(),
                        w);
            });
        }

        String arg = HandlerHelper.render(argBuilder);

        // navGetWrap facet (PR #243): an exists/notExists argument that rendered as a
        // transparent item-collapse (a selfUnwrapping JavaExpression whose text ends in
        // `.get()`) must be re-wrapped MapperS.of(...) so the existence check sees a
        // Mapper argument — green-safe by the same corpus law (no golden leaves a
        // `.get()`-collapsed existence argument bare). refs are copied to a mutable set
        // so the wrap can register MAPPER_S.
        Set<JavaClass<?>> refs = new HashSet<>(argBuilder.getRefs());
        arg = HandlerHelper.wrapSelfUnwrappingGetOperand(arg, argBuilder, refs);

        // facet bareValueMapperSWrap (PR #256): a bare no-args FUNCTION-invocation argument
        // (e.g. `<rule/report> exists` where the rule navigates a `func` — TradeForEvent /
        // GetPackageInformation) renders UNWRAPPED via
        // ReferenceHandler.renderImplicitFunctionInvocation, so the existence check sees a bare
        // item where golden wraps it MapperS.of(...). Re-wrap it exactly as ComparisonHandler
        // does at the areEqual/ordering operand seats (the shared HandlerHelper SOT). Disjoint
        // from the .get()-collapse wrap above by AST shape (a bare function ref renders ending
        // in ")" with no unwrapToBuilder). Use orSyntheticImplicit so the same node compiled
        // above is inspected. Green-safe by construction (a bare function exists argument never
        // byte-matched golden — the unwrapped form does not compile).
        // facet existsOperandMapperCWrap (PR #301): pass the compiler so a MULTI-output bare
        // function (`exists(getUnderlierProductIdentifier)` over a `ProductIdentifier (0..*)` —
        // UnderlyingIdentificationType asic) wraps `MapperC.<X>of(...)` not `MapperS.of(...)`,
        // the cardinality-aware #298 law at the exists-operand seat (a single-output function
        // keeps the #256 `MapperS.of`). Green-safe by construction (MapperS.of over a multi-list
        // is non-compiling — already waivered).
        arg = HandlerHelper.wrapBareInvocationOperand(arg,
                HandlerHelper.orSyntheticImplicit(expr.argument(), expr), refs, compiler);

        String method = existenceMethod(expr);

        Set<JavaClass<?>> wildcards = new HashSet<>();
        wildcards.add(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE);
        wildcards.addAll(argBuilder.getStaticWildcardImports());

        return JavaExpression.from(
                method + "(" + arg + ")",
                null,
                refs,
                wildcards);
    }

    /**
     * v3.2 seat 9 (PR #630, F8 / D47): the EMPTY mapper an existence operand of Void item type coerces to -
     * {@code MapperS.<Void>ofNull()} for every cardinality (upstream's expected type is {@code Mapper<? extends X>},
     * whose empty is the MapperS form: the oracle group void-mapping-render-edge's InputSingleExists over a LIST
     * input renders {@code singleExists(MapperS.<Void>ofNull())}) - or {@code null} when the operand is not a
     * Void item (today's render stands). The verdict is the coercer's own predicate over the front-end type
     * ({@code TypeCoercionService.isNullOrVoidItem} of {@code HandlerHelper.inferredJavaType}); the empty is the
     * coercer's own {@code emptyValueFor}. SHARED by {@code handle(RExistenceExpr)} and the IR route's
     * {@code IRExpressionCompiler.visitExistence}, which declines a Void operand to the handler (LAW 77).
     */
    public static JavaStatementBuilder voidOperandEmptyOrNull(RExpression operand, ExpressionCompiler compiler) {
        com.regnosys.rosetta.generator.java.expression.TypeCoercionService svc = compiler.getCoercionService();
        com.regnosys.rosetta.generator.java.types.JavaTypeUtil tu = compiler.getTypeUtil();
        if (svc == null || tu == null) {
            return null;
        }
        JavaType operandJava = HandlerHelper.inferredJavaType(operand, compiler);
        if (operandJava == null || !svc.isNullOrVoidItem(operandJava)) {
            return null;
        }
        return svc.emptyValueFor(tu.wrap(tu.MAPPER_S, operandJava));
    }

    /**
     * The static ExpressionOperatorsNullSafe method name for an existence expression.
     * Package-visible since facet aliasStaticImportEscape (PR #420):
     * {@link HandlerHelper#staticOperatorMembersUsed} consults the SAME selector the
     * render emits with, so the escape census and the emitted member name cannot
     * disagree (the #178 same-walk law). (Orphaned by the seat-9 insertion above it, restored at round 1.)
     */
    static String existenceMethod(RExistenceExpr expr) {
        if (expr.op() == ExistenceOp.ABSENT) {
            return "notExists";
        }
        ExistsModifier modifier = expr.modifier().orElse(null);
        if (modifier == ExistsModifier.SINGLE) {
            return "singleExists";
        } else if (modifier == ExistsModifier.MULTIPLE) {
            return "multipleExists";
        }
        return "exists";
    }

    /**
     * facet existsOperandMetaWrap (PR #315): a JavaConditionalExpression whose branch item type is
     * a value-level meta wrapper ({@link RJavaWithMetaValue}) is the #315 marker — the meta-wrap
     * null-safe ternary ReferenceHandler produced for a meta-typed inner-rule exists operand. A
     * plain conditional exists operand (non-meta item type) is NOT a #315 wrap and keeps the
     * string-wrap path (so this discriminator scopes the exists distribution to exactly the meta
     * case, leaving every other conditional operand byte-identical).
     */
    private static boolean isMetaWrapperConditional(JavaConditionalExpression cond,
            ExpressionCompiler compiler) {
        if (compiler.getTypeUtil() == null) {
            return false;
        }
        JavaType item = compiler.getTypeUtil().getItemType(cond.getExpressionType());
        return item instanceof RJavaWithMetaValue;
    }

    // =========================================================================
    // Only-exists expressions
    // =========================================================================

    /**
     * Compiles an only-exists expression to match upstream 9.83.0
     * ({@code ExpressionGenerator.xtend} {@code caseOnlyExists}).
     *
     * <p>The first element's synthesized parent-navigation receiver (built by
     * {@code AstBuilder.buildOnlyExistsElement} as {@code root + featureChain[:-1]}) is the
     * shared parent: it compiles to the {@code Mapper} navigation, its resolved data type
     * supplies the FULL attribute set ({@code allFields}, declaration order, super-first), and
     * each element's leaf feature name supplies the {@code selectedFields} (argument order).
     *
     * <p>Output pattern:
     * <pre>
     *   onlyExists(MapperS.of(root).&lt;T&gt;map("getX", _root -&gt; _root.getX()),
     *              Arrays.asList(&lt;all parent attrs&gt;), Arrays.asList(&lt;leaf names&gt;))
     * </pre>
     *
     * <p>Declines to the {@link #handleLegacyPlaceholder legacy placeholder} (keeping the
     * pre-fix, still-waivered rendering — hence zero regression) for the residual edge
     * shapes whose parent type STILL does not resolve after the facet
     * lambda_item_body_coercion arm-C fallbacks and the facet onlyexists_implicit_item
     * arm-3 synthesis below: a null receiver that is a bare {@code item} root with no
     * chain (a chained {@code item -> x} parses over a real {@code RImplicitVariable} since
     * v3.2 seat 12 — D52 H3, M6 — and resolves through arm 1's gm-aware walk exactly as
     * cdm's {@code item -> location} condition does: the lambda item type, the rule
     * from-type or the condition's declaring type; the receiver compiles to {@code item}
     * inside a lambda and to {@code MapperS.of(<instance>)} at a condition top — the
     * hold-out group {@code only-exists-item-root}), a CHAIN-LESS bare symbol only when arm 3's
     * arm-B2 decline ladder fires (closure param / function-scope name / non-implicit
     * lambda / unresolved item type / root not an attribute of the item type), a
     * symbol-EMPTY root that is not an attribute of a resolvable implicit-item type (or
     * sits outside an implicit lambda / matches a closure param or function-scope name),
     * and any navigation neither the legacy {@code resolveValueDataType} nor the gm-aware
     * {@code resolveReceiverDataType} walk resolves. Alias roots, alias-rooted chains,
     * implicit-item attribute roots (arm C) and ladder-passing chain-less bare elements
     * (arm 3 — the implicit variable as shared parent) now resolve instead (see the
     * arm-C / arm-3 comments in the method body).
     *
     * @param expr     the only-exists expression node
     * @param ctx      the current expression compilation context (supplies the render scope)
     * @param compiler the parent compiler (compiles the synthesized receiver navigation)
     * @return a {@link JavaExpression} rendering the onlyExists call
     */
    public JavaStatementBuilder handle(ROnlyExistsExpr expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        List<ROnlyExistsElement> elements = expr.elements();

        // Upstream uses the FIRST arg's receiver as the shared parent. Resolve the parent type
        // from its synthesized receiver expression; decline to the legacy placeholder when it
        // is absent or unresolved (the edge shapes above) so currently-waivered files stay
        // byte-identical.
        ROnlyExistsElement first = elements.isEmpty() ? null : elements.get(0);
        RExpression receiver = first == null ? null : first.receiverExpression();
        RDataType parentType = receiver == null ? null : HandlerHelper.resolveValueDataType(receiver);
        // facet lambda_item_body_coercion (arm C): two previously-declining
        // receiver shapes now resolve instead of falling to the dotted legacy
        // placeholder (corpus law: ZERO of 219 golden onlyExists calls carry a
        // dotted path — lists are always parent-relative simple names).
        // (1) ALIAS roots and alias-rooted chains — resolveValueDataType only
        //     accepts a resolved RAttribute symbol / resolvedFeature, so an
        //     v3.2 seat 12 (D52, H3 - M6): an `item ->` root arrives here too - the parser
        //     builds it over a real RImplicitVariable now (AstBuilder.buildOnlyExistsPath),
        //     and this walk's implicit arm types it from its context (the lambda item, the
        //     rule input, the condition's declaring type) while a hop over it resolves by
        //     name (fallbackResolveFeature) where the type engine left it unbound (a
        //     type-condition top level). The chaos s25 rows and the hold-out group
        //     only-exists-item-root are the witnesses; the pre-seat render was the legacy
        //     dotted placeholder.
        //     RShortcut root (`primitiveInstruction only exists`, the alias body
        //     even ending only-element) or an alias-rooted chain returned null;
        //     the gm-aware walk recurses through the alias expression (cycle
        //     guarded) and fallbackResolveFeature. The receiver COMPILE already
        //     renders the alias method call (`primitiveInstruction(businessEvent)`)
        //     through ReferenceHandler — only the parent-type gate was missing.
        if (parentType == null && receiver != null) {
            parentType = NavigationHandler.resolveReceiverDataType(receiver, compiler);
        }
        // (2) IMPLICIT-ITEM roots — inside a map/filter lambda the synthesized
        //     receiver root stays symbol-EMPTY (parse-time Cat 9 cannot type the
        //     lambda's item when the list-op argument is itself a disguised
        //     ref); when the bare root names an attribute of the gm-aware item
        //     type, substitute the item-rooted navigation (the arm-B2 synthesis,
        //     sharing ALL its decline gates: closure params, function-scope
        //     names, non-implicit lambdas) — golden
        //     `onlyExists(item.<PrimitiveInstruction>map(...), ...)`.
        if (parentType == null && receiver instanceof RSymbolReference symRef
                && symRef.symbol().isEmpty()) {
            RFeatureCall itemNav = ReferenceHandler.synthesizeImplicitItemBareNav(symRef, compiler);
            if (itemNav != null && itemNav.resolvedFeature().isPresent()) {
                RDataType itemParent = NavigationHandler.attributeToDataType(
                        itemNav.resolvedFeature().get(), compiler);
                if (itemParent != null) {
                    receiver = itemNav;
                    parentType = itemParent;
                }
            }
        }
        // (3) facet onlyexists_implicit_item — CHAIN-LESS bare elements
        //     (`foreignExchange only exists` inside an extract lambda) parse
        //     with a NULL receiverExpression BY DESIGN
        //     (AstBuilder.buildOnlyExistsPath returns null for a root with no
        //     chain - a bare symbol or a bare `item`; a chained item root
        //     synthesizes since v3.2 seat 12); upstream's caseOnlyExists ELSE
        //     branch renders the IMPLICIT VARIABLE as the shared parent with
        //     the item type's full attribute set — golden `onlyExists(item,
        //     Arrays.asList(<all 8 Product attrs>), Arrays.asList(
        //     "foreignExchange"))`. The gate excludes item-ROOTED shapes
        //     (isRootItem covers `item -> x` chains, which also parse
        //     receiver-less) and elements WITH a chain (never receiver-less
        //     unless item-rooted); the synthesis shares the arm-B2 decline
        //     ladder. leafName() already returns the bare root for the
        //     selectedFields list. Zero goldens carry the legacy chain-less
        //     fingerprint `onlyExists(x, asList("x"), asList("x"))` — the
        //     decline path stays byte-frozen for every other receiver-less
        //     shape.
        if (parentType == null && receiver == null && first != null
                && !first.isRootItem() && first.featureChain().isEmpty()) {
            ReferenceHandler.ImplicitItemOnlyExistsReceiver itemReceiver =
                    ReferenceHandler.synthesizeImplicitItemOnlyExistsReceiver(
                            expr, first.root(), compiler);
            if (itemReceiver != null) {
                receiver = itemReceiver.receiver();
                parentType = itemReceiver.itemType();
            }
        }
        // (4) Coverage wave D (datarule): a CHAIN-LESS bare element inside a
        //     DATA-TYPE condition (`priceReturnTerms only exists`, golden
        //     ReturnTermsReturnTermsExists) — upstream's caseOnlyExists renders
        //     the condition's implicit INSTANCE as the shared parent with the
        //     declaring type's FULL attribute set: onlyExists(
        //     MapperS.of(returnTerms), Arrays.asList(<all 5 ReturnTerms attrs>),
        //     Arrays.asList("priceReturnTerms")). Gated on the root naming an
        //     attribute of the declaring type (the same by-name gate as every
        //     condition-instance arm); fires only after the item arms above
        //     decline (no lambda in a condition top level), null for every
        //     function/rule-path expression by construction.
        if (parentType == null && receiver == null && first != null
                && !first.isRootItem() && first.featureChain().isEmpty()) {
            com.regnosys.rosetta.ast.functions.RCondition condition =
                    HandlerHelper.findEnclosingTypeCondition(expr);
            if (condition != null
                    && condition.parent() instanceof com.regnosys.rosetta.ast.types.RDataType declaringType
                    && HandlerHelper.findAttributeOnDataType(declaringType, first.root()) != null) {
                receiver = ReferenceHandler.syntheticConditionInstanceRef(expr, declaringType.name());
                parentType = declaringType;
            }
        }
        if (parentType == null) {
            return handleLegacyPlaceholder(elements);
        }

        // Receiver: compile the synthesized parent navigation. Passing a null expected type
        // yields its natural Mapper rendering (a bare variable wraps as MapperS.of(name); a
        // feature-call chain appends .<T>map(...)); coercion stays dormant on the null
        // expression type these produce.
        JavaStatementBuilder receiverBuilder = compiler.compile(receiver, null, ctx.scope());
        String receiverStr = HandlerHelper.render(receiverBuilder);

        // allFields: the parent type's complete attribute set, in upstream declaration order.
        StringJoiner allFields = new StringJoiner(", ");
        for (RAttribute attr : HandlerHelper.allAttributesInUpstreamOrder(parentType)) {
            allFields.add("\"" + attr.name() + "\"");
        }

        // selectedFields: each element's leaf feature name, preserving argument order.
        StringJoiner selectedFields = new StringJoiner(", ");
        for (ROnlyExistsElement element : elements) {
            selectedFields.add("\"" + leafName(element) + "\"");
        }

        Set<JavaClass<?>> refs = new HashSet<>();
        refs.add(HandlerHelper.ARRAYS);
        refs.addAll(receiverBuilder.getRefs());
        Set<JavaClass<?>> wildcards = new HashSet<>();
        wildcards.add(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE);
        wildcards.addAll(receiverBuilder.getStaticWildcardImports());

        return JavaExpression.from(
                "onlyExists(" + receiverStr + ", Arrays.asList(" + allFields + "), Arrays.asList(" + selectedFields + "))",
                null,
                refs,
                wildcards);
    }

    /**
     * The legacy (pre-upstream-alignment) placeholder rendering: the raw root as the receiver
     * and each element's fully-qualified dotted path for BOTH field lists. Retained verbatim as
     * the decline path so files whose parent type does not resolve stay byte-identical to the
     * prior output (no regression).
     */
    private JavaStatementBuilder handleLegacyPlaceholder(List<ROnlyExistsElement> elements) {
        String root;
        if (elements.isEmpty()) {
            root = "null";
        } else {
            ROnlyExistsElement first = elements.get(0);
            root = first.isRootItem() ? "item" : first.root();
        }

        StringJoiner allFields = new StringJoiner(", ");
        StringJoiner selectedFields = new StringJoiner(", ");
        for (ROnlyExistsElement element : elements) {
            String path = buildElementPath(element);
            allFields.add("\"" + path + "\"");
            selectedFields.add("\"" + path + "\"");
        }

        return JavaExpression.from(
                "onlyExists(" + root + ", Arrays.asList(" + allFields + "), Arrays.asList(" + selectedFields + "))",
                null,
                Set.of(HandlerHelper.ARRAYS),
                Set.of(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE));
    }

    /** The element's selected-field name: the last feature in its chain, or the root for a chain-less element. */
    private String leafName(ROnlyExistsElement element) {
        List<String> chain = element.featureChain();
        if (!chain.isEmpty()) {
            return chain.get(chain.size() - 1);
        }
        return element.isRootItem() ? "item" : (element.root() != null ? element.root() : "");
    }

    private String buildElementPath(ROnlyExistsElement element) {
        String root = element.isRootItem() ? "item" : (element.root() != null ? element.root() : "");
        List<String> chain = element.featureChain();
        if (chain.isEmpty()) {
            return root;
        }
        return root + "." + String.join(".", chain);
    }

    // =========================================================================
    // Cardinality check expressions (one-of / choice)
    // =========================================================================

    /**
     * Compiles a cardinality check expression into a {@code choice()} call.
     *
     * <p>The validation method depends on the operation and necessity:
     * <ul>
     *   <li>{@code ONE_OF} → {@code ChoiceRuleValidationMethod.REQUIRED}</li>
     *   <li>{@code CHOICE} with {@code REQUIRED} necessity → {@code ChoiceRuleValidationMethod.REQUIRED}</li>
     *   <li>{@code CHOICE} with {@code OPTIONAL} necessity → {@code ChoiceRuleValidationMethod.OPTIONAL}</li>
     * </ul>
     *
     * @param expr     the cardinality check expression node
     * @param ctx      the current expression compilation context
     * @param compiler the parent compiler for recursive compilation of the argument
     * @return a {@link JavaExpression} rendering the choice call
     */
    public JavaStatementBuilder handle(RCardinalityCheckExpr expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        // PR-A §9.1 C3a.4.d (D6 ε + D9 λ):
        //   - ARRAYS for Arrays.asList(...)
        //   - CHOICE_RULE_VALIDATION_METHOD for REQUIRED/OPTIONAL enum values (D6 ε)
        //   - EXPRESSION_OPERATORS_NULL_SAFE wildcard for static `choice(...)` (D9 λ Case a —
        //     confirmed 2026-04-21: choice() is public static on
        //     ExpressionOperatorsNullSafe L477/L480; this fixes the latent bug
        //     where FunctionGenerator L844-849 trigger list does NOT include
        //     `choice(` so pure-choice functions currently fail to import the
        //     wildcard)
        RExpression argument = expr.argument();
        List<String> attributes = expr.attributes();
        // Coverage wave D (datarule): the WITHOUT-LEFT cardinality form inside a
        // DATA-TYPE condition carries a NULL argument (`optional choice a, b` /
        // bare `one-of` at ANY operand depth — golden TradeNovationContentChoice2's
        // nested `and optional choice …` operands; drr BR_3001_01's nested
        // `required choice`): the implicit subject is the condition INSTANCE (the
        // executeDataRule parameter), and a bare one-of takes the declaring type's
        // ALL-attribute list (upstream caseOneOfOperation → allAttributes). The
        // datarule generator's root-level twin synthesis covers whole-body forms
        // identically; this arm is the nested-seat law. Null for every
        // function/rule-path expression by construction.
        if (argument == null) {
            var condition = HandlerHelper.findEnclosingTypeCondition(expr);
            if (condition != null && condition.parent() instanceof RDataType declaringType) {
                argument = ReferenceHandler.syntheticConditionInstanceRef(expr, declaringType.name());
                if (attributes.isEmpty() && expr.op() == CardCheckOp.ONE_OF
                        && compiler.getGeneratorModel() != null) {
                    attributes = compiler.getGeneratorModel().allAttributes(declaringType).stream()
                            .map(RAttribute::name).toList();
                }
            }
        } else if (attributes.isEmpty() && expr.op() == CardCheckOp.ONE_OF
                && compiler.getGeneratorModel() != null) {
            // facet oneOfStaticAttrList (PR #437, finding #36): the FUNCTION/RULE-path
            // `<arg> one-of` enumerates the ARGUMENT's STATIC data type's attribute
            // names (upstream caseOneOfOperation → t.allAttributes over the argument's
            // RType — supertypes walked; golden func-one-of-static
            // `choice(MapperS.of(a), Arrays.asList("a1", "a2", "a3"), REQUIRED)`).
            // The pre-#437 empty list COMPILED and answered FALSE for every instance
            // (the #428 RUNTIME face). The datarule condition seat above already
            // enumerates identically (one SOT: GeneratorModel.allAttributes); a
            // non-data-typed argument (choice-ref, unresolved) keeps today's empty
            // list — no witness.
            var gm = compiler.getGeneratorModel();
            var inferredArgType = gm.workspace().getInferredType(argument);
            if (inferredArgType != null && !inferredArgType.isMissing()
                    && inferredArgType.type()
                            instanceof com.regnosys.rosetta.types.RDataTypeRef dtRef) {
                attributes = gm.allAttributes(dtRef.astNode()).stream()
                        .map(RAttribute::name).toList();
            }
        }
        // facet fnIoMetaChoiceValueDeref (PR #433, finding #21 — the RUNTIME face):
        // a required/optional-choice over a BARE meta-input param fires on the
        // VALUE, not the wrapper — upstream derefs with the null-guarded PARAM form
        // `choice((foo == null ? MapperS.<Foo>ofNull() : MapperS.of(foo.getValue())),
        // …)` (the func-meta-choice-ignore oracle golden; the pre-facet wrapper read
        // COMPILED and mis-fired reflection on FieldWithMetaFoo at run — the
        // invoke-battery-only divergence class).
        JavaStatementBuilder argBuilder =
                NavigationHandler.metaInputValueDeref(argument, compiler);
        if (argBuilder == null) {
            argBuilder = compiler.compile(argument, ctx.expectedType(), ctx.scope());
        }
        String arg = HandlerHelper.render(argBuilder);

        StringJoiner attrsJoiner = new StringJoiner(", ");
        for (String attr : attributes) {
            attrsJoiner.add("\"" + attr + "\"");
        }
        String attrsList = "Arrays.asList(" + attrsJoiner + ")";

        String validationMethod;
        if (expr.op() == CardCheckOp.ONE_OF) {
            validationMethod = "ChoiceRuleValidationMethod.REQUIRED";
        } else {
            Necessity necessity = expr.necessity().orElse(Necessity.REQUIRED);
            validationMethod = necessity == Necessity.OPTIONAL
                    ? "ChoiceRuleValidationMethod.OPTIONAL"
                    : "ChoiceRuleValidationMethod.REQUIRED";
        }

        Set<JavaClass<?>> refs = new HashSet<>();
        refs.add(HandlerHelper.ARRAYS);
        refs.add(HandlerHelper.CHOICE_RULE_VALIDATION_METHOD);
        refs.addAll(argBuilder.getRefs());

        Set<JavaClass<?>> wildcards = new HashSet<>();
        wildcards.add(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE);
        wildcards.addAll(argBuilder.getStaticWildcardImports());

        return JavaExpression.from(
                "choice(" + arg + ", " + attrsList + ", " + validationMethod + ")",
                null,
                refs,
                wildcards);
    }

}
