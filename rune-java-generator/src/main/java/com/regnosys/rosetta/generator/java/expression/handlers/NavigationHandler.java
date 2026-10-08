package com.regnosys.rosetta.generator.java.expression.handlers;

import com.regnosys.rosetta.generator.java.SilentDegradation;
import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.enums.ListOp;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConstructorExpr;
import com.regnosys.rosetta.ast.expressions.literals.RListLiteral;
import com.regnosys.rosetta.ast.expressions.references.REmptyLiteral;
import com.regnosys.rosetta.ast.expressions.references.RDeepFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.binary.RDefaultExpr;
import com.regnosys.rosetta.ast.expressions.binary.RJoinExpr;
import com.regnosys.rosetta.ast.expressions.binary.RThenExpr;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCase;
import com.regnosys.rosetta.ast.expressions.unary.RCardinalityCheckExpr;
import com.regnosys.rosetta.ast.expressions.unary.RConversionExpr;
import com.regnosys.rosetta.ast.expressions.unary.RCountExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExistenceExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExtractExpr;
import com.regnosys.rosetta.ast.expressions.unary.RFilterExpr;
import com.regnosys.rosetta.ast.expressions.unary.RListOpExpr;
import com.regnosys.rosetta.ast.expressions.unary.RMaxExpr;
import com.regnosys.rosetta.ast.expressions.unary.RMinExpr;
import com.regnosys.rosetta.ast.expressions.unary.ROnlyExistsExpr;
import com.regnosys.rosetta.ast.expressions.unary.RReduceExpr;
import com.regnosys.rosetta.ast.expressions.unary.RSortExpr;
import com.regnosys.rosetta.ast.expressions.unary.RToStringExpr;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RCardinality;
import com.regnosys.rosetta.ast.supporting.RChoiceOption;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.generator.GeneratedIdentifier;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.generator.java.JavaNamingUtil;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.function.FunctionTemplateModel;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.JavaLocalVariableDeclarationStatement;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.template.ImportCollisionResolver;
import com.regnosys.rosetta.generator.java.types.JavaPojoProperty;
import com.regnosys.rosetta.generator.java.types.RJavaPojoInterface;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RJavaReferenceWithMeta;
import com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RAliasType;
import com.regnosys.rosetta.types.RChoiceTypeRef;
import com.regnosys.rosetta.types.RDataTypeRef;
import com.regnosys.rosetta.types.REnumTypeRef;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.RMissingType;
import com.regnosys.rosetta.types.RRecordType;
import com.regnosys.rosetta.types.RecordKind;
import com.regnosys.rosetta.types.RType;
import com.regnosys.rosetta.types.inference.CardinalityComputer;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaReferenceType;
import com.rosetta.util.types.JavaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import javax.lang.model.SourceVersion;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Handles code generation for navigation (feature call) expressions:
 * {@link RFeatureCall} and {@link RDeepFeatureCall}.
 *
 * <p>Golden output patterns:
 * <pre>
 *   trade -> price             →  receiver.&lt;Price&gt;map("getPrice", _trade -> _trade.getPrice())
 *   businessEvent -> instruction  →  receiver.&lt;Instruction&gt;mapC("getInstruction", _businessEvent -> _businessEvent.getInstruction())
 *   chain: ... -> instruction -> before → ...&lt;Before&gt;map("getBefore", instruction -> instruction.getBefore())
 *   asset ->> identifier       →  receiver.&lt;AssetIdentifier&gt;mapC("chooseIdentifier", asset -> assetDeepPathUtil.chooseIdentifier(asset))
 * </pre>
 *
 * <p>Feature calls compile the receiver then chain a {@code .map()} or {@code .mapC()}
 * call using a lambda whose parameter is derived from the <em>receiver's type</em>:
 * <ul>
 *   <li>When the receiver is a symbol reference (e.g., function input {@code businessEvent}),
 *       the lambda variable is {@code _} + lowerCamelCase(receiverTypeName), e.g.,
 *       {@code _businessEvent}.</li>
 *   <li>When the receiver is another feature call (chained navigation), the lambda variable
 *       is lowerCamelCase of the previous feature call's return type (no underscore prefix),
 *       e.g., {@code instruction} after a step that returns {@code Instruction}.</li>
 * </ul>
 * The getter name is derived from the feature name by capitalising the first letter
 * and prepending {@code get}.
 *
 * <p>The generic type parameter before {@code map}/{@code mapC} is the return type of
 * the accessed feature, resolved from the attribute's {@code typeCall().typeName()}.
 *
 * <p>{@code mapC} is used when the feature has multi-valued cardinality (upper bound
 * greater than 1 or unbounded); {@code map} is used for single-valued features.
 *
 * <p>Deep feature calls ({@code ->>}) use a {@code DeepPathUtil} helper class that is
 * injected into the function. The method name follows the pattern
 * {@code choose<FeatureName>}. The deep path util class and lambda variable name are
 * derived from the receiver's type (e.g., {@code Asset} → {@code assetDeepPathUtil},
 * lambda var {@code asset}).
 *
 * <p>Lambda naming follows CDM golden-file conventions: receiver-type-based names
 * (e.g., {@code _businessEvent}) for the first step from a symbol reference, and
 * return-type-based names (e.g., {@code instruction}) for subsequent chained steps.
 */
public class NavigationHandler {

    private static final Logger LOG = Logger.getLogger(NavigationHandler.class.getName());

    // =========================================================================
    // Feature call (single arrow →)
    // =========================================================================

    /**
     * Compiles a single-arrow feature call into a mapper chain with generic type
     * parameter and correct {@code map}/{@code mapC} selection.
     *
     * <p>Pattern (single-valued): {@code receiver.<Type>map("getFeature", _f -> _f.getFeature())}
     * <p>Pattern (multi-valued):  {@code receiver.<Type>mapC("getFeature", _f -> _f.getFeature())}
     *
     * @param expr     the feature call node
     * @param ctx      the current expression compilation context
     * @param compiler the parent compiler for recursive compilation of the receiver
     * @return a {@link JavaExpression} rendering the mapped feature access
     */
    public JavaStatementBuilder handle(RFeatureCall expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        // PR-A §9.1 C3a.4.f: Mapper library class emitted structurally based
        // on cardinality. Operand refs + staticWildcards flow through from
        // the receiver builder. Domain-type (<Type> generic param)
        // structured-ref bridge is deferred to a follow-up per D3 ι; the
        // pre-C3c.2 GENERIC_TYPE_PARAM regex path has been deleted.
        // facet interior_position_coercion: a receiver CONTINUES the chain, so it
        // compiles interior — an item-level expected leaking into this position
        // (ComparisonHandler.inferNumericType through count/distinct) must not
        // terminal-coerce the wrapper away mid-chain; the meta unwrap is
        // coerceNavigationReceiver's below, in golden inline form.
        JavaStatementBuilder receiverBuilder =
                compiler.compileInterior(expr.receiver(), ctx.expectedType(), ctx.scope());
        // facet meta_coercion_numbering (arm A1): an alias-call receiver renders
        // with a NULL expression type (ReferenceHandler's alias branch carries
        // none), so the meta-strip gate below declined at the alias HEAD even
        // though the alias method's declared signature is meta-typed
        // (`protected abstract MapperS<? extends ReferenceWithMetaTradeState>
        // beforeTradeState(...)` — facet alias_method_signature_typing). Re-type
        // the receiver from the SAME FunctionAliasHelper walk that rendered that
        // signature (single source of truth — signature and coercion cannot
        // disagree), and let the existing gate fire unchanged. Null for every
        // non-alias / non-meta / usesOutput receiver — the byte-flat guarantee.
        // Pre-fix the firing shape rendered NON-COMPILING Java (a value-type
        // getter invoked on the wrapper-typed lambda param), so no green file
        // carries it.
        if (receiverBuilder.getExpressionType() == null) {
            JavaType aliasMapperType = tryAliasReceiverMapperType(expr.receiver(), compiler);
            if (aliasMapperType != null) {
                receiverBuilder = retypeBuilder(receiverBuilder, aliasMapperType);
            }
        }
        // facet mapitem_ctor_wrap (mechanism 3): the inline-function implicit
        // item keeps its META-wrapped element type upstream — implicitVariable
        // types the lambda item via the meta-KEEPING toJavaReferenceType, and
        // the owning list-op's argument compiles against MAPPER_C.wrapExtends
        // (also meta-keeping) — so a navigation step off a meta-element item
        // re-types the NULL-typed receiver (ReferenceHandler's
        // handle(RImplicitVariable) carries no type) as
        // MapperS<FieldWithMetaX>, and the UNCHANGED gate below emits the
        // golden inline guarded `.<Value>map("Type coercion",
        // fieldWithMetaX0 -> fieldWithMetaX0 == null ? null :
        // fieldWithMetaX0.getValue())` with registered 0..n-1 numbering per
        // same-name group inside the per-lambda scope (the #170
        // computeActualNames machinery; golden CompareTradeLot numbers 0/1 and
        // the sibling lambda RESTARTS — each compileLambda body compiles in
        // its own lambdaScope child). Structurally the A1 alias arm's sibling:
        // resolve the type from the SAME implicitItemArgument walk the
        // lambda-var naming reads (the #175 owner walk — the rendered next-step
        // lambda var and this retype cannot disagree). Null for non-meta /
        // unresolvable arguments — the byte-flat guarantee; upstream always
        // emits the unwrap on meta-element items, so no green file carries the
        // fork's unwrap-free form (pre-fix it rendered the value-type getter
        // on the wrapper item — non-compiling).
        if (receiverBuilder.getExpressionType() == null
                && expr.receiver() instanceof RImplicitVariable) {
            JavaType implicitMetaType = implicitItemMetaMapperType(expr, compiler);
            // facet thenArgCollapseMetaPreserve (PR #338), arm (iv) — a last-then EXTRACT
            // item whose chain the AST walkers cannot resolve (ExistingIsin's meta filter is
            // FUSED into the parenthesized chain BASE, where the frozen #272-family walker
            // stops; green ExtractUpi's separate filter-THEN resolves via the #284/#325
            // fallback inside implicitItemMetaMapperType and never reaches this arm): read
            // the then-arg's OWN declared type off the scope binding of the item's OWNING
            // pipe. Resolution is EXACT, not a skip-walk: implicitItemArgument resolves the
            // item's owning argument; only a then-PIPE implicit var qualifies, and the
            // binding is looked up for THAT pipe's nearest enclosing inline function alone —
            // a nested in-lambda then's pipe lands on an UNBOUND lambda fn and declines (the
            // CountryAndProvinceOrTerritoryOfIndividual mis-attribution the #338 over-fire
            // scan caught). The decl is this renderer's own emission (two-halves-agree:
            // after the #338 arm (i) base-filter recovery it is
            // `MapperS<ReferenceWithMetaProductIdentifier>`), so the item retype and the
            // declaration cannot disagree. Guards: MapperS-SINGLE + meta element (a MapperC
            // then-item keeps today's decline — the #180 wrapper-KIND emission hazard), and
            // resolvedFeature PRESENT — a metafield nav off the wrapper (`-> scheme`)
            // resolves NO feature on the meta-blind element type and must NOT deref (the
            // Extract_HKMASchemeName over-fire catch; the #286 metaValueHasNavFeature law at
            // this seat's type-system altitude). Green-safe by construction: the arm fires
            // only where NO existing resolution typed the item, i.e. where the fork
            // navigated a value getter on a wrapper-element item — a non-compiling,
            // already-waivered form (no green file carries it; upstream always derefs a
            // meta-element value nav).
            if (implicitMetaType == null && expr.resolvedFeature().isPresent()
                    && ctx.scope() != null) {
                var tu = compiler.getTypeUtil();
                RExpression ownerArg = implicitItemArgument(expr.receiver());
                if (tu != null && ownerArg instanceof RImplicitVariable) {
                    RNode fnWalk = ownerArg.parent();
                    int fnDepth = 0;
                    while (fnWalk != null && !(fnWalk instanceof RInlineFunction)
                            && fnDepth++ < HandlerHelper.PARENT_WALK_LIMIT) {
                        fnWalk = fnWalk.parent();
                    }
                    if (fnWalk instanceof RInlineFunction pipeOwnerFn) {
                        // facet thenBindingOverlay (v3.1 C2d retirement family 9, PR #616) —
                        // VERDICT-MOVED RETIRE-AFTER-CENSUS -> JUSTIFIED-KEPT (the seat-30 bar;
                        // S32), a LAW-77 seat: MEASURED-INERT, and the proposed widening is
                        // UNMEASURED BY CONSTRUCTION. c9 census, 99,311 arrivals (46,259 / 9,926 /
                        // 43,126) = 67,861 gate + 31,450 read. The probe's own positive control
                        // holds — walkerAnswerNull at 67,861/67,861, so the seat does sit where the
                        // frozen walker gave up — while fired=false and itemIsMeta=false at
                        // 31,450/31,450 with isMapperS=true at 20,582: the first conjunct fires
                        // often, the second never, so the last-resort retype NEVER happens at this
                        // corpus on any route. The WIDENED walker answer was deliberately NOT
                        // cloned (a guessed clone is a confident wrong number), which does not
                        // block a KEEP but would block a retirement. LAW 77: a GATE-POPULATION
                        // asymmetry (31,927 OFF vs 6,914 ON), BENIGN because the decisions agree
                        // PER ROUTE. Producer recovery 30,433/31,450 over SIX NAMED SITES plus
                        // 1,017 nokey.
                        JavaExpression pipeRef = ctx.scope().thenArgRefFor(pipeOwnerFn);
                        JavaType declType = pipeRef == null ? null : pipeRef.getExpressionType();
                        if (declType != null && tu.isMapperS(declType)
                                && tu.getItemType(declType) instanceof RJavaWithMetaValue) {
                            implicitMetaType = declType;
                        }
                    }
                }
            }
            if (implicitMetaType != null) {
                receiverBuilder = retypeBuilder(receiverBuilder, implicitMetaType);
            }
        }
        // facet metaInputParam (PR #342): a bare [metadata …]-annotated single-card
        // FUNCTION-INPUT receiver derefs via upstream's null-guarded meta-PARAM form
        // `(price == null ? MapperS.<PriceSchedule>ofNull() : MapperS.of(price.getValue()))`
        // — the raw-wrapper-Java-local law (the param IS the local), NOT the Mapper-chain
        // `Type coercion` map step coerceNavigationReceiver below would apply to a
        // MapperS<wrapper>-typed receiver. Built VALUE-typed (MapperS<PriceSchedule>) so
        // the coerce no-ops and this step resolves/witnesses on the VALUE type exactly as
        // the linker bound it (`price -> value` resolves on PriceSchedule). Only the 5
        // corpus-closed meta-input carriers can reach this (metaInputParamWrapper's
        // population argument); the pre-facet receiver (`MapperS.of(price)` — a value
        // getter navigated on the wrapper) never compiled.
        // facet fnIoMetaSchemeNavReceiver (PR #433, finding #21): when THIS nav is an
        // unresolved META-FEATURE read (`scheme`/`reference` — the fork has no
        // RosettaMetaType node, so the linker leaves them unresolved), the receiver
        // must KEEP the raw wrapper: upstream navigates the meta feature ON the
        // wrapper (`MapperS.of(myInput).map("getMeta", a->a.getMeta()).map("getScheme",
        // a->a.getScheme())` — the func-meta-scheme-nav/-arith oracle goldens), so the
        // deref below must not fire; metaFeatureShortFormOrNull (placed BEFORE
        // coerceNavigationReceiver) then renders the short form on the wrapper-typed
        // receiver. A RESOLVED feature named scheme/reference (a real model attribute)
        // keeps the deref — the same gate the short form itself applies.
        String fnIoMetaNavFeature = expr.featureName();
        // seat 21 (facet metaFaceShortForm): any face the parser BOUND to a metaType keeps the raw
        // wrapper too — the same admission the short form below applies (one predicate, two seats).
        boolean fnIoMetaFeatureRead = expr.resolvedFeature().isEmpty()
                && ("scheme".equals(fnIoMetaNavFeature)
                        || "reference".equals(fnIoMetaNavFeature)
                        || HandlerHelper.boundMetaType(expr) != null);
        if (expr.receiver() instanceof RSymbolReference metaParamRef && compiler != null
                && !fnIoMetaFeatureRead) {
            RJavaWithMetaValue paramWrapper = metaInputParamWrapper(metaParamRef, compiler);
            if (paramWrapper != null) {
                JavaTypeUtil tu = compiler.getTypeUtil();
                String valueSimple = paramWrapper.getValueType().getSimpleName();
                Set<JavaClass<?>> guardRefs = new HashSet<>(receiverBuilder.getRefs());
                guardRefs.add(HandlerHelper.MAPPER_S);
                if (paramWrapper.getValueType() instanceof JavaClass<?> valueClass) {
                    guardRefs.add(valueClass);
                }
                receiverBuilder = JavaExpression.from(
                        "(" + metaParamRef.name() + " == null ? MapperS.<" + valueSimple
                                + ">ofNull() : MapperS.of(" + metaParamRef.name() + ".getValue()))",
                        tu.wrap(tu.MAPPER_S, paramWrapper.getValueType()),
                        guardRefs);
            }
        }
        // facet closureParamDirectNav (PR #342): an EXPLICIT closure param navigated as a
        // receiver is ALREADY a Mapper-typed Java local — golden navigates it directly
        // (`leg.<RateSpecification>map(…)`, drr InterestRateLeg1/2CrossCurrency;
        // `q1.<BigDecimal>map(…)`, CompareQuantityByUnitOfAmount ×2 cdm cells) where the
        // fork's variable path double-wrapped `MapperS.of(leg)` — a wrap whose value-getter
        // nav never compiled (MapperS carries no POJO getters), so every carrier is an
        // already-waivered mismatch. Admission = the SAME walk + then-decline the #331
        // contains-operand unwrap and the resolveReceiverDataType closure arm use, AND the
        // element must RESOLVE (an untyped bare receiver would re-render today's witness-less
        // bytes only accidentally — decline instead, keeping the wrap). The bare receiver is
        // typed MapperS<Element> so the witness / arity / lambda naming (all reading the same
        // resolution) and the compiled type cannot disagree.
        if (expr.receiver() instanceof RSymbolReference cpRecv && compiler != null
                && cpRecv.args().isEmpty()
                && cpRecv.symbol().filter(RAttribute.class::isInstance).isEmpty()) {
            RInlineFunction cpRecvOwner = ReferenceHandler
                    .enclosingClosureParamOwner(cpRecv, cpRecv.name());
            if (cpRecvOwner != null && !(cpRecvOwner.parent() instanceof RThenExpr)) {
                RDataType cpElem = resolveReceiverDataType(cpRecv, compiler);
                JavaTypeUtil tu = compiler.getTypeUtil();
                if (cpElem != null && tu != null && compiler.getTypeTranslator() != null) {
                    JavaClass<?> elemClass = compiler.getTypeTranslator()
                            .toJavaReferenceType(new RDataTypeRef(cpElem));
                    // A META-wrapped element ([metadata reference/scheme/…] on the owner
                    // argument's terminal attribute) types the bare receiver as the concrete
                    // WRAPPER, so coerceNavigationReceiver below auto-inserts golden's
                    // `Type coercion` deref step before this nav (drr SortIdentifiers'
                    // `identifier.<ProductIdentifier>map("Type coercion", …getValue())
                    // .<ProductIdTypeEnum>map("getSource", …)`); a meta-free element keeps
                    // the bare element type (the CompareQuantity / InterestRateLeg carriers,
                    // coerce a no-op).
                    RExpression cpOwnerArg = cpRecvOwner.parent() instanceof RFilterExpr cpF
                            ? cpF.argument()
                            : cpRecvOwner.parent() instanceof RExtractExpr cpE
                                    ? cpE.argument() : null;
                    RAttribute cpTerminal = cpOwnerArg == null ? null
                            : terminalNavAttr(cpOwnerArg, compiler);
                    // facet listOfListsWrapperKeep (PR #384): a BARE-symbol owner
                    // argument (symbol EMPTY — `price filter p [ p -> priceType <> … ]`
                    // inside a ctor KV, the #384 bareSymbolBodyCardinality class)
                    // resolves its terminal attribute against the implicit item type
                    // via the SAME walk the bare-symbol mapC synthesis renders from
                    // (implicitItemDataTypeOrInferred + findAttributeOnDataType —
                    // render-truth), so a META element stamps the wrapper below and
                    // the coercion emits golden's per-use deref (`p.<PriceSchedule>
                    // map("Type coercion", …).<PriceTypeEnum>map("getPriceType", …)`
                    // — cdm Qualify_OnDemandRateChange). Green-safe by the #342/#373
                    // argument: a value-feature nav on a wrapper element never
                    // compiled, so every carrier is an already-waivered mismatch.
                    if (cpTerminal == null && cpOwnerArg instanceof RSymbolReference bareOwner
                            && bareOwner.symbol().isEmpty() && bareOwner.name() != null) {
                        RDataType bareOwnerItemT =
                                implicitItemDataTypeOrInferred(bareOwner, compiler);
                        cpTerminal = bareOwnerItemT == null ? null
                                : HandlerHelper.findAttributeOnDataType(
                                        bareOwnerItemT, bareOwner.name());
                    }
                    RJavaWithMetaValue cpWrapper = cpTerminal == null ? null
                            : metaWrapperOf(cpTerminal, compiler);
                    // facet explicitParamPipedMeta (PR #373, F-eps): a THEN-PIPED explicit
                    // param (`then extract quantitySchedule [ … ]`) owns an IMPLICIT
                    // argument — the terminal-attr read above sees no nav, so the wrapper
                    // evidence recovers from the OWNING then-chain instead
                    // (thenOwnerArgument → the #264/#270 walker): the #285
                    // implicitItemArgMeta sibling for EXPLICIT params. The piped element IS
                    // the wrapper (the pipe decl types MapperC<FieldWithMeta…> — render
                    // truth), so the receiver types as the wrapper and
                    // coerceNavigationReceiver fires golden's numbered null-guarded deref
                    // hops (drr NotionalQuantityLeg2Rule ×5, method-wide 0..4). Green-safe
                    // by construction: a value-type nav on a wrapper element never compiled
                    // (the witness type mismatches even at the getValue collision), so
                    // every carrier is an already-waivered mismatch.
                    if (cpWrapper == null && cpOwnerArg instanceof RImplicitVariable) {
                        RExpression cpChain = thenOwnerArgument(cpOwnerArg);
                        if (cpChain != null) {
                            cpWrapper = recoverMetaFromExpr(cpChain, compiler, 0, new HashSet<>());
                        }
                    }
                    JavaClass<?> receiverItem = cpWrapper != null ? cpWrapper : elemClass;
                    if (receiverItem != null) {
                        Set<JavaClass<?>> cpRefs = new HashSet<>(receiverBuilder.getRefs());
                        cpRefs.remove(HandlerHelper.MAPPER_S);
                        // v3.2 seat 12 round 1 (cq MF-1, D52 H1): a closure-parameter RECEIVER of a DUPLICATED
                        // reduce pair renders the FIRST parameter's numbered name - `a0.<BigDecimal>map(...)`,
                        // the hold-out group closure-param-duplicate-reads pinned from the released plugin
                        // BEFORE this line (ReduceDupNavRead: 10 / 11 identical before, the receiver the one
                        // raw `a.`); every other owner keeps the raw name through the SAME predicate - the
                        // distinct-name control, and a NON-REDUCE owner by the predicate's own gate (round 2, cq
                        // SF-1: the fork admits `extract a, a [ ... ]`, which upstream refuses - RosettaSimpleValidator
                        // .checkOptionalNamedParameter, "Function must have 1 named parameter."; h1x the control).
                        receiverBuilder = JavaExpression.from(
                                HandlerHelper.reduceParamReadName(cpRecvOwner, cpRecv.name()),
                                tu.wrap(tu.MAPPER_S, receiverItem), cpRefs);
                    }
                }
            }
        }
        // Engine PR #9 (PR-B): meta-strip a meta-typed receiver so a
        // ReferenceWithMetaX / FieldWithMetaX value auto-unwraps via .getValue()
        // before this step navigates further (mirror upstream attributeCall L385).
        // No-op for non-meta / null-typed receivers — the byte-flat guarantee.
        // facet metaPathShortForm (PR #348): a META-FEATURE read (`scheme` / `reference`)
        // renders upstream buildMapFunc's a->a short form DIRECTLY on the wrapper
        // (ExpressionGenerator.xtend:515-527): `reference` → `.map("getReference",
        // a->a.getExternalReference())` — the receiver KEEPS the wrapper, no deref —
        // and `scheme` → `.map("getMeta", a->a.getMeta()).map("getScheme",
        // a->a.getScheme())`. The fork has no RosettaMetaType node, so the linker
        // leaves the meta feature UNRESOLVED (P348-M1 probe: resolved=EMPTY at every
        // corpus seat) and the pre-facet render was the non-compiling wrapper deref +
        // by-name getter. Fires ONLY when the feature does NOT resolve (a REAL model
        // attribute named `scheme`/`reference` keeps its resolved render) AND the
        // receiver PROVABLY rides a single-cardinality meta wrapper (the typed
        // receiver's MapperS item, else the #285 implicitItemArgMeta walk for the
        // type-blind implicit item); reference-kind additionally requires the
        // Reference-kind wrapper (getExternalReference lives only there). Placed
        // BEFORE coerceNavigationReceiver — the deref this facet suppresses. Green-safe:
        // zero gen a->a forms exist pre-facet (grep = 0 per cell) and every
        // probe-verified firing seat's golden carries the short form.
        JavaStatementBuilder metaShortForm =
                metaFeatureShortFormOrNull(expr, receiverBuilder, compiler, ctx.scope());
        if (metaShortForm != null) {
            return metaShortForm;
        }
        // facet iteChainArmThenHoist (PR #375, A4-c): an UNTYPED bare implicit receiver
        // inside a then-piped extract lambda re-stamps from the #350 binding's compiled
        // META element (bareItemThenPipeMetaType — the #362 render-truth channel, never
        // an AST recovery) so the standard coercion below emits golden's guarded deref
        // hop BEFORE this nav (Create_AnnaDsbUpiRequestUnderlyingForRate's consumer
        // `item.<ProductIdentifier>map("Type coercion", fieldWithMetaProductIdentifier
        // -> … .getValue()).<FieldWithMetaString>map("getIdentifier", …)`). MapperS
        // kind — the map-step item is single by construction (the #349-S2 kind law:
        // MapperS → the guarded+registered param). Null-binding / bare-element /
        // already-typed receivers keep today's bytes.
        if (compiler != null && compiler.getTypeUtil() != null && ctx.scope() != null
                && expr.receiver() instanceof RImplicitVariable pipedRecvItem
                && receiverBuilder instanceof JavaExpression pipedRecvJe
                && pipedRecvJe.getExpressionType() == null) {
            RJavaWithMetaValue pipedRecvMeta = HandlerHelper.bareItemThenPipeMetaType(
                    pipedRecvItem, ctx.scope(), compiler);
            if (pipedRecvMeta != null) {
                JavaTypeUtil pipedTu = compiler.getTypeUtil();
                receiverBuilder = JavaExpression.from(pipedRecvJe.renderToString(),
                        pipedTu.wrap(pipedTu.MAPPER_S, pipedRecvMeta),
                        pipedRecvJe.getRefs(), pipedRecvJe.getStaticWildcardImports());
            }
        }
        // facet metaSigKeep (PR #382): the FILTER/SORT-predicate sibling of the A4-c
        // re-stamp above — an UNTYPED bare implicit receiver whose nearest enclosing
        // lambda is a filter/sort predicate over a META-ALIAS receiver chain
        // (element-preserving filter steps looked through, the #329 M5 law) re-stamps
        // from the alias's signature element (tryAliasReceiverMapperType — the SAME
        // walk that renders the `Mapper*<? extends FieldWithMetaX>` alias method, the
        // #178 same-walk invariant), so the standard coercion below emits golden's
        // per-use deref hop (cdm6 StandardizedScheduleFXSwapNotional's predicates:
        // `item.<NonNegativeQuantitySchedule>map("Type coercion",
        // fieldWithMetaNonNegativeQuantitySchedule -> …).<UnitType>map("getUnit", …)`
        // and the sort key's head deref). A non-alias receiver, a non-meta alias, or
        // an already-typed item keeps today's bytes.
        if (compiler != null && compiler.getTypeUtil() != null
                && expr.receiver() instanceof RImplicitVariable aliasPredItem
                && receiverBuilder instanceof JavaExpression aliasPredJe
                && aliasPredJe.getExpressionType() == null) {
            RExpression predOwnerArg = filterOrSortPredicateOwnerArgument(aliasPredItem);
            while (predOwnerArg instanceof RFilterExpr pf) {
                predOwnerArg = pf.argument();
            }
            JavaType aliasPredMapper = predOwnerArg == null ? null
                    : tryAliasReceiverMapperType(predOwnerArg, compiler);
            JavaType aliasPredItemType = aliasPredMapper == null ? null
                    : compiler.getTypeUtil().getItemType(aliasPredMapper);
            if (aliasPredItemType instanceof RJavaWithMetaValue predWrap) {
                JavaTypeUtil predTu = compiler.getTypeUtil();
                receiverBuilder = JavaExpression.from(aliasPredJe.renderToString(),
                        predTu.wrap(predTu.MAPPER_S, predWrap),
                        aliasPredJe.getRefs(), aliasPredJe.getStaticWildcardImports());
            }
        }
        receiverBuilder = compiler.coerceNavigationReceiver(receiverBuilder, ctx.scope());
        String receiver  = HandlerHelper.render(receiverBuilder);
        // Engine PR #147 (facet date_record_feature_nav): a navigation whose RECEIVER
        // is a built-in record value (dateTime / zonedDateTime) accessing the `date`
        // record feature has no POJO getter — emit the upstream RecordJavaUtil lambda
        // (.<Date>map("Date", zdt -> Date.of(zdt.toLocalDate()))) instead of the
        // non-compiling .map("getDate", … -> ….getDate()) getter form. Returns null
        // for the dominant non-record receiver, which flows to the getter path below.
        JavaStatementBuilder recordNav =
                tryRecordFeatureNav(expr, receiver, receiverBuilder, ctx, compiler);
        if (recordNav != null) {
            return recordNav;
        }
        // only-element re-wrap (facet nav_after_get_rewrap): a navigation step whose
        // receiver is a single-cardinality `only-element` (`.get()`) collapse renders the
        // receiver as a raw item (`<chain>.get()`); a further `map`/`mapC` step needs a
        // Mapper receiver, so upstream re-wraps it in `MapperS.of(...)`. Gated to the
        // ONLY_ELEMENT receiver shape — the verified non-compiling form (NO green golden
        // carries `.get().map(` / `.get().mapC(`, corpus-wide = 0), so the wrap only ever
        // touches currently-divergent (waivered) output: blast-radius-0 by construction.
        // The witness + owner-derived lambda var for THIS step are recovered by
        // resolveReceiverDataType's matching ONLY_ELEMENT case (a type-transparent collapse),
        // which also cascades the recovery to any further steps chained after the `.get()`.
        boolean onlyElementReceiver = expr.receiver() instanceof RListOpExpr le
                && le.op() == ListOp.ONLY_ELEMENT;
        // facet collapsedMetaDeref (PR #317): a `<meta feature> only-element -> <bare feature>`
        // navigation — the FEATURE-NAV analogue of #314's ConversionHandler.getCollapsedMetaDeref.
        // If the only-element collapse is a meta wrapper AND this step navigates a feature of the
        // BARE value type, DEREF the wrapper (hoist the `.get()` collapse + null-guard-reconstruct
        // `(w == null ? MapperS.<Value>ofNull() : MapperS.of(w.getValue()))`) instead of the plain
        // `MapperS.of(<X>.get())` re-wrap below (which navigates a value getter on the wrapper —
        // non-compiling). Null → the plain re-wrap (today's still-waivered bytes).
        JavaExpression collapsedMetaDeref = onlyElementReceiver
                ? collapsedMetaDerefRewrapOrNull((RListOpExpr) expr.receiver(), expr,
                        receiverBuilder, receiver, ctx, compiler)
                : null;
        if (collapsedMetaDeref != null) {
            receiver = collapsedMetaDeref.renderToString();
        } else if (onlyElementReceiver) {
            receiver = "MapperS.of(" + receiver + ")";
        }
        String feature   = expr.featureName();
        String getter    = toGetterName(feature);
        // facet accessorTypeEscape (PR #242): the lambda-BODY accessor invocation escapes an
        // inherited-method collision (`getType`/`getClass` → `_getType`/`_getClass`) via the
        // POJO subsystem's shared SOT (JavaPojoProperty.escapeOperationName). The POJO already
        // declares the escaped accessor (getOperationName routes through the same set), so the
        // function nav MUST call the escaped name — golden does (`telephoneNumber._getType()`).
        // The witness/map LABEL keeps the bare `getter` (golden's label is `"getType"`).
        String accessor  = JavaPojoProperty.escapeOperationName(getter);
        String lambdaVar = resolveLambdaVarName(expr, ctx.scope(), compiler);

        // facet fqnWitness (PR #197): resolve the witness attribute up-front so the
        // output-collision check (witnessOutputCollisionFqn) drives BOTH the witness
        // TEXT (FQN-inline on collision, here) and its import (suppressed on collision,
        // below). The THIRD render position of the first-claim-wins import-collision law
        // shipped at PR #195 (input-param) / PR #196 (to-enum target): when the witness's
        // Java simple name collides with the function's OUTPUT type but a DIFFERENT
        // canonical name, the output (registered first) keeps the bare name + import and
        // the witness renders FULLY-QUALIFIED inline with NO import.
        // seat-21 review (LAW 77, one body): the three-rung leaf ladder — resolved, fallback-resolved,
        // switch-case-narrowed (facet switchCaseNarrowedLeafWitness, PR #395: a case-narrowed
        // implicit-item leaf `item -> issuerCountryOfOrigin` resolves off the guard type, consulted
        // ONLY on null so every previously-resolving step keeps its bytes; the recovered attr feeds
        // exactly the witness sentinel + addWitnessTypeRef import below) — lives in navLeafAttrOrNull,
        // the SAME read the IR route's nav-root guard and the call-root predicate consult: a weaker
        // ladder there would let a fallback-resolved override nav render the stamped LEAF on one route
        // while this seat base-witnesses it.
        RAttribute resolvedAttr = navLeafAttrOrNull(expr, compiler);
        // v3.1 flip seat 21 — facet overrideChainWitness (LAW 69): the LAST hop of a call
        // ARGUMENT whose resolved leaf is an OVERRIDE witnesses the override-chain property
        // the callee's DECLARED input type selects — upstream attributeCall consults
        // JavaPojoInterface.findProperty(name, expectedType), which walks leaf → parent and
        // returns the first property whose type the expected (ITEM) type is a subtype of
        // (ExpressionGenerator.xtend:359-380; JavaPojoInterface.java:47-57); the item expected
        // type is installed at the call-argument seat (evaluateCall :296), a Mapper expected
        // type never selects a parent. The wave-D arm (golden drr datarule
        // ASICTransactionReportDTCC_ASIC_BR_1066_01 `<PeriodicPayment>map("getPeriodicPayment",
        // commonLeg -> …)` into `periodicPaymentLeg1 cde.payment.PeriodicPayment (1..1)`) carried
        // the law TYPE-CONDITION-gated — the band it was cut from held only datarule carriers
        // (LAW 75 in reverse) — and re-implemented the selector one level deep on Rune-node
        // identity. It now CONSULTS the fork's own port of upstream's selector
        // (JavaPojoInterface.findProperty, ZERO callers before this seat), un-gated: function
        // and rule paths included (drr 7.x ChangeInNotionalAmountLeg1/2,
        // Create_ValuationDetailsFromReportableEvent, PayoutForQuantityLeg1/2Rule,
        // ReportingTimestampRule — `<ReportableInformationBase>map("getReportableInformation",
        // …)` + the base import). LAW 75 (the seat-21 probe over all 275 rows): un-gated, the
        // arm fires on exactly the 72 already-right periodicPayment datarule sites (byte-
        // identical) + the 60 carrier sites; every other override nav at a call seat stays on
        // its leaf (a Mapper-expected inner hop, or a callee declaring the leaf type). The IR
        // route declines the same predicate (IRExpressionCompiler.tryEmitFromIR — LAW 77).
        if (resolvedAttr != null) {
            RAttribute baseArgAttr = calleeArgBaseAttributeOrNull(expr, resolvedAttr, compiler);
            if (baseArgAttr != null) {
                resolvedAttr = baseArgAttr;
            }
        }
        String witnessCollisionFqn = witnessOutputCollisionFqn(expr, resolvedAttr, compiler);

        // facet fqnWitness (PR #227): when there is no #197 output-collision, emit the witness
        // type as a first-claim-wins sentinel (carrying the canonical name) so a body-internal
        // collision with a different-canonical construction type (builder-ctor / hoisted local)
        // is resolved by render order in FunctionGenerator.buildStandardModel. A non-colliding
        // witness resolves byte-identically to resolveTypeParam's bare <SimpleName>.
        String witnessSentinel = witnessCollisionFqn == null
                ? witnessSentinelTypeParam(resolvedAttr, compiler)
                : null;
        String typeParam = witnessCollisionFqn != null
                ? "<" + witnessCollisionFqn + ">"
                : (witnessSentinel != null ? witnessSentinel : resolveTypeParam(expr, resolvedAttr, compiler));
        String mapMethod = resolveMapMethod(expr, compiler);

        Set<JavaClass<?>> refs = new HashSet<>();
        // The instance .map()/.mapC() methods need NO mapper-class import: the
        // MapperS/MapperC class is imported at the chain's static-factory leaf
        // (MapperS.of(...)/MapperC.of(...)), whose ref flows up via the receiver
        // below. The legacy plugin never imports MapperC for a .mapC() *method*
        // call. (Phase X1 imports knock-on — the prior MAPPER_C/MAPPER_S add here
        // emitted a spurious MapperC import the golden does not carry.)
        refs.addAll(receiverBuilder.getRefs());
        if (onlyElementReceiver) {
            // The outer MapperS.of(...) re-wrap (above) needs MapperS imported. Idempotent
            // with the inner chain's own MapperS.of leaf — refs is a Set.
            refs.add(HandlerHelper.MAPPER_S);
        }
        if (collapsedMetaDeref != null) {
            // facet collapsedMetaDeref (PR #317): the reconstruct expression carries the wrapper +
            // bare value type + MapperS (its `MapperS.<Value>ofNull()` witness / value import).
            refs.addAll(collapsedMetaDeref.getRefs());
        }
        // Register the <Type> generic-witness import: the generic parameter text
        // emitted by resolveTypeParam must be imported for the generated source to
        // compile. The stateless handler has no JavaTypeTranslator, so resolve the
        // witness JavaClass through the compiler's translator + generator model.
        // (D3 ι: witness structured-ref bridge — was deferred at PR-A C3a.4.f.)
        // gm-aware fallback (facet choice_option_nav_witness): a choice-option step resolves its
        // attribute only via the gm-aware fallback, so the witness IMPORT (e.g.
        // `import cdm.product.template.OptionPayout;`) is registered to MATCH the recovered
        // witness TEXT — without it the file carries the `<OptionPayout>` text but misses the
        // import and never byte-matches the golden.
        // facet fqnWitness (PR #197): suppress the witness import when it is rendered
        // FQN-inline on an output-name collision (the output type already imports the
        // simple name; a second import of the differently-packaged witness would be a
        // DUPLICATE same-simple-name import = a Java compile error — exactly the pre-fix
        // bug). Off-collision, register it as before (it appears by simple name).
        if (witnessCollisionFqn == null) {
            addWitnessTypeRef(resolvedAttr, compiler, refs);
        }
        // Engine PR #9 (PR-B): when this step navigates TO a meta attribute, surface a
        // concrete RJavaWithMetaValue as the result item type — and register its
        // <ReferenceWithMetaX>/<FieldWithMetaX> witness import (addWitnessTypeRef skips
        // meta) — so a deeper step can meta-strip THIS receiver. Non-meta steps keep a
        // null type, so the top-level coercion gate stays dormant on them.
        // Facet choice_nav_chain_typing: the wrapper kind is CHAIN-aware, not step-local —
        // once a chain passes through an (uncollapsed) mapC step, every later step renders
        // a MapperC at runtime regardless of its own cardinality, and upstream's meta-deref
        // for a MapperC is the BARE `w -> w.getValue()` (WrappedItemCoercer's MapperC arm),
        // not the null-guarded MapperS form. A step-local flag mis-typed the wrapper as
        // MapperS on `businessEvent -> instruction (mapC) -> before [metadata reference]`,
        // emitting a spurious null-guard the golden does not carry.
        // The meta-kind pre-check is a strict pre-filter of metaNavResultType's own gate
        // (cheap + deterministic): the receiver-chain walk runs ONLY for meta steps — the
        // sole consumers of the wrapper kind — not for the dominant non-meta steps whose
        // result type stays null either way.
        boolean chainMapperC = "mapC".equals(mapMethod)
                || (resolvedAttr != null
                        && MetaFieldGenerator.detectMetaKind(resolvedAttr) != MetaFieldGenerator.MetaKind.NONE
                        && (chainRendersMapperC(expr.receiver(), compiler,
                                ctx == null ? null : ctx.scope())
                                // facet ruleMidChainCondLambdaAdmit (PR #392): render
                                // truth at the wrap-kind read — a step rooted DIRECTLY
                                // on a BOUND then-pipe implicit reads the binding's
                                // compiled Mapper kind (scope.thenArgRefFor — the #362
                                // channel; golden CE1/2 `thenArg3.<FWMRegimeNameEnum>
                                // map(…).<RegimeNameEnum>map("Type coercion",
                                // fieldWithMetaRegimeNameEnum ->
                                // fieldWithMetaRegimeNameEnum.getValue())` — the BARE
                                // non-registering MapperC form, the #310 law). An
                                // unbound/non-then lambda item reads false (its own
                                // channels own the kind).
                                || boundImplicitPipeIsMapperC(expr.receiver(), ctx,
                                        compiler)));
        JavaType resultType = metaNavResultType(resolvedAttr, compiler, refs, chainMapperC);

        return JavaExpression.from(
                receiver + "." + typeParam + mapMethod + "(\"" + getter + "\", "
                        + lambdaVar + " -> " + lambdaVar + "." + accessor + "())",
                resultType,
                refs,
                receiverBuilder.getStaticWildcardImports());
    }

    /**
     * facet collapsedMetaDeref (PR #317): the FEATURE-NAV analogue of #314's
     * {@code ConversionHandler.getCollapsedMetaDeref}. A {@code <meta feature> only-element ->
     * <bare feature>} navigation: the {@code only-element} collapses the MULTI meta chain to a
     * bare wrapper ({@code .get()}), which {@link #handle(RFeatureCall, ExpressionContext,
     * ExpressionCompiler)} would re-wrap {@code MapperS.of(<X>.get())} — but the next step
     * navigates a feature of the BARE value type, so the wrapper must be DEREFERENCED first.
     * Upstream (TypeCoercionService convertNullSafe) hoists the collapsed bare wrapper to a local
     * and null-guard-RECONSTRUCTS a {@code MapperS<Value>}:
     * <pre>
     * final &lt;Wrapper&gt; &lt;name&gt; = &lt;X&gt;.get();
     * … (&lt;name&gt; == null ? MapperS.&lt;Value&gt;ofNull()
     *                       : MapperS.of(&lt;name&gt;.getValue())).&lt;…&gt;map("get…", …)
     * </pre>
     * The hoist drains on the LAMBDA_CHANNEL (inside a {@code mapSingleToItem}/{@code extract}
     * lambda → {@code CollectionHandler.compileLambda} block-converts it, drr
     * {@code BasketConstituentUnitOfMeasureRule}) or the STATEMENT_SINK (a whole-output rule body
     * → {@code FunctionExpressionRenderer.prependStatementHoists} lifts it, drr
     * {@code BasketConstituentNumberOfUnitsRule}). RULE-scoped → FUNCTION-byte-neutral (#232).
     *
     * <p>Green-safe by construction: the fork's {@code MapperS.of(<wrapper>).<Value>map(v ->
     * v.getX())} navigates a value getter on the wrapper-typed lambda param → non-compiling, so
     * no green file carries the pre-fix form (every carrier is an already-waivered NON_COMPILING
     * mismatch; green→red is structurally impossible). Returns {@code null} (→ the plain
     * {@code MapperS.of(<X>.get())} re-wrap, today's still-waivered bytes) when: the
     * {@code only-element} leaf is not a meta wrapper; the navigated feature is not an attribute
     * of the BARE value type (a metafield nav off the wrapper — {@code -> reference}/{@code ->
     * scheme} — must keep the plain re-wrap); the enclosing rule is absent (FUNCTION path); no
     * drain seat is reachable; or the wrapper / derived name is unresolved.
     */
    private static JavaExpression collapsedMetaDerefRewrapOrNull(RListOpExpr onlyElem,
            RFeatureCall expr, JavaStatementBuilder receiverBuilder, String collapsedReceiver,
            ExpressionContext ctx, ExpressionCompiler compiler) {
        if (compiler == null || ctx == null || ctx.scope() == null) {
            return null;
        }
        // Fire only when the collapsed-meta-deref can drain to a reachable STATEMENT_SINK: a RULE
        // body (the #232/#317 original scope), OR any non-rule context whose scope chain reaches a
        // sink (#335). The condition is "rule-scoped OR any reachable statement-hoist sink" — NOT
        // function-only: the #262 unconditional {@code hoistSessionEligible} marks every function
        // SET-body statement scope a sink (opening the FUNCTION path —
        // IsCommodity{Forward,Option,TotalReturnSwap}_SingleIndex), and alias bodies and
        // condition-validator supplier lambda bodies are statement-hoist sinks too, so they also
        // qualify. What does NOT reach a sink here is a reduce/min DATA-TRANSFORM lambda interior:
        // its {@code lambdaScope} boundary stops the sink walk, so {@code findStatementHoistSink()}
        // is null and the gate declines (CommodityBasis A3 deferred — the LAMBDA_CHANNEL below
        // handles drainable map/extract lambdas separately). Computed ONCE and reused as the drain
        // {@code sink} below. The #232 FUNCTION-byte-neutral freeze holds for every function WITHOUT
        // a reachable sink.
        JavaStatementScope sink = ctx.scope().findStatementHoistSink();
        // facet guardedDerefHoist (PR #364): the RESTRICTED arm-deref channel — a
        // pathed-conditional ARM compile reaches this producer through the fallback
        // (the arm scope carries no full sink, deliberately); the decl relocates INTO
        // the owning branch via ControlFlowHandler's arm windows (golden
        // UpdateIndexTransitionPriceAndRateOption's `final FieldWithMetaPriceSchedule
        // fieldWithMetaPriceSchedule0/1/2 = …get();` + the guarded
        // `(x == null ? MapperS.<PriceSchedule>ofNull() : MapperS.of(x.getValue()))`
        // operand re-wraps, numbered as ONE group across the arms).
        if (sink == null) {
            sink = ctx.scope().findArmDerefHoistSink();
        }
        // facet fnNotionalTogetherRestructure (PR #398): the FUNCTION-path LAMBDA
        // channel — a sink-less function seat INSIDE a drainable map/extract lambda
        // proceeds (the #335 sink-reachability read widened; golden NotionalLeg
        // forwardNotional's in-lambda `final ReferenceWithMetaPriceSchedule
        // referenceWithMetaPriceSchedule = item.<…>mapC(…).get();` + the Mapper-guard
        // operand). Green-safe by the #317 construction: the plain re-wrap navigates a
        // value getter on the wrapper — non-compiling, zero green carriers.
        if (HandlerHelper.findEnclosingRule(expr) == null && sink == null
                && !HandlerHelper.isInsideDrainableMapLambda(expr)) {
            return null;
        }
        // The only-element leaf must carry a value-level meta annotation (the wrapper the `->
        // <bare feature>` navigation this step performs strips).
        RAttribute leaf = onlyElementLeafAttribute(onlyElem.argument(), compiler);
        RJavaWithMetaValue wrapper = metaWrapperOf(leaf, compiler);
        if (wrapper == null) {
            return null;
        }
        // The navigated feature must be an attribute of the BARE VALUE type (this step derefs the
        // wrapper to the value) — NOT a metafield off the wrapper (`-> reference`/`-> scheme`),
        // which navigates the wrapper directly and must keep the plain re-wrap. The ONLY_ELEMENT
        // case of resolveReceiverDataType is a type-transparent collapse recovering the bare value
        // RDataType (the same resolution the downstream `getUnit`/`getValue` witness already uses).
        // facet collapsedMetaDerefChoiceOptionFeature (v3.1 flip seat 33, law C.1, rung R2a): a
        // DIRECT choice OPTION of a CHOICE the value type EXTENDS is such a feature too - the
        // attribute walk cannot see it (a `type X extends <choice>` edge lives on
        // choiceSuperType(), not superType()), and reading it as "not a feature of the value
        // type" declined the deref for golden drr 7.x Price's `basketConstituent only-element ->
        // Asset` hop. The question is asked through ONE predicate so rung R2b's statement-seat
        // reader gets the identical answer (LAW 69); see collapsedMetaDerefValueFeature.
        if (!collapsedMetaDerefValueFeature(expr, compiler)) {
            return null;
        }
        // A drain seat must be reachable: the STATEMENT_SINK computed above (rule or any reachable
        // sink), OR a drainable map/extract lambda (LAMBDA_CHANNEL, whose interior stops the sink
        // walk — reached only from a RULE context, since a non-rule with a null sink was gated out
        // above). Else the hoist would orphan → decline.
        boolean lambda = sink == null && HandlerHelper.isInsideDrainableMapLambda(expr);
        // facet blockArmWrapperHopDeref (v3.1 flip seat 33, law F.A rung (a)): the RULE-path
        // BLOCK-LADDER ARM channel. The reader above refuses every node under a conditional
        // (the #312 exclusion, cut because a pending hoist inside a conditional arm makes
        // compileLambda's UNDRAINED conditional-block form decline at its
        // !hasPendingLambdaHoists() gate) - but a rule-path LADDER block DRAINS its arms per
        // rung (CollectionHandler.compileLadderLevelArms' inner-level
        // drainPendingLambdaHoists at :12390/:12430 and compileLadderArmWithDeepThenDrain's
        // own admit list at the top level), and golden puts the decl exactly there: inside
        // `if ((booleanN == null ? false : booleanN)) { ... }`. The window flag is RENDER
        // TRUTH (raised only while that block form is actually being compiled - an
        // inline-ternary ladder never raises it) and is consulted at THIS seat alone.
        // MEASURED, both routes: `verdict=decline:noDrain` is 4 rows over 2 distinct where=
        // (rule:NotionalCurrencyLeg1, rule:NotionalCurrencyOfLeg1) - the ENTIRE population
        // that reaches this conjunct - and the other decline classes cannot join it
        // (`decline:hostGate` is 100% wrap=-, so a drain seat would still decline them at
        // the wrapper conjunct). GREEN BLAST RADIUS ZERO; and the fork's form here
        // navigates a value getter on the wrapper, so it never compiled (javac32 C20:709).
        boolean ruleBlockArm = sink == null && !lambda
                && ctx.scope().isRuleBlockArmDerefSeat()
                && HandlerHelper.findEnclosingRule(expr) != null;
        if (ruleBlockArm) {
            lambda = true;
        }
        if (sink == null && !lambda) {
            return null;
        }
        JavaReferenceType valueType = wrapper.getValueType();
        String baseName = JavaNamingUtil.toFirstLower(wrapper.getSimpleName());
        if (!SourceVersion.isIdentifier(baseName)) {
            return null;
        }
        GeneratedIdentifier id = ctx.scope().createUniqueIdentifier(baseName);
        String nameToken = ctx.scope().registerDeferredCoercionName(id);
        // {@code collapsedReceiver} is the rendered ONLY_ELEMENT collapse — INVARIANT `<chain>.get()`
        // ({@code CollectionHandler.handle(RListOpExpr)} always renders ONLY_ELEMENT as `.get()`), so
        // the hoisted decl value is the whole bare-item collapse the plain re-wrap would have consumed
        // (the #314 arm asserts this through the family-7 arbiter
        // {@code HandlerHelper.bareOnlyElementCollapse} on its convertible source — an
        // `.endsWith(".get()")` test until facet collapseGetSuffix, v3.1 C2d retirement family 7,
        // PR #614; here the {@code onlyElementReceiver} gate guarantees it structurally).
        if (sink != null) {
            // STATEMENT_SINK: register the decl as a sentinel-bearing STRING (the #237 pattern —
            // the decl OBJECT's render() would getActualName()-close the scope mid-compile). The
            // collapsed chain's imports ride the outer receiverBuilder.getRefs() collection.
            sink.registerStatementHoist(
                    "final " + wrapper.getSimpleName() + " " + nameToken + " = "
                            + collapsedReceiver + ";");
        } else {
            // LAMBDA_CHANNEL: register the decl STATEMENT (compileLambda block-converts + drains
            // it, the #314 pattern). JavaLocalVariableDeclarationStatement#getRefs ignores the
            // declared type, so enrich the initializer refs with the wrapper.
            Set<JavaClass<?>> declRefs = new HashSet<>(receiverBuilder.getRefs());
            declRefs.add(wrapper);
            // facet blockArmWrapperHopDeref (v3.1 flip seat 33, law F.A rung (a)): the NEW
            // rule-path block-ladder ARM channel renders the decl through the SAME SENTINEL
            // statement, for the SAME reason (LAW 69, the #346/#398 closed-scope law): this
            // seat mints TWO MORE identifiers after it in the same statement scope (rung
            // (b)'s referenceWithMetaPriceSchedule1/2 at the commodity rungs), and the plain
            // declaration OBJECT's render() calls GeneratedIdentifier.getActualName(), which
            // closes the identifier's scope AND every ancestor - poisoning those later
            // creations. The ladder's per-rung drains treat both statement kinds alike (the
            // inner-level drain at :12430 is kind-blind; the top-level admit list already
            // names ItemGetMetaDerefHoist), so the placement is unchanged and the rendered
            // text is identical (`final <Wrapper> <name> = <chain>;`).
            if (HandlerHelper.findEnclosingRule(expr) == null || ruleBlockArm) {
                // facet fnNotionalTogetherRestructure (PR #398): the FUNCTION-path route
                // renders the decl through the SENTINEL statement (the #346
                // ItemGetMetaDerefHoist class) — the decl OBJECT's render would
                // getActualName()-close the alias scope mid-compile and poison the later
                // same-alias registrations (the fixedPriceNotional closed-scope law).
                // The RULE path keeps the decl-object registration byte-identically.
                ctx.scope().registerPendingLambdaHoist(
                        new ReferenceHandler.ItemGetMetaDerefHoist(wrapper.getSimpleName(),
                                nameToken, collapsedReceiver, declRefs,
                                receiverBuilder.getStaticWildcardImports()));
            } else {
                JavaExpression declValue = JavaExpression.from(collapsedReceiver, wrapper,
                        declRefs, receiverBuilder.getStaticWildcardImports());
                ctx.scope().registerPendingLambdaHoist(
                        new JavaLocalVariableDeclarationStatement(true, wrapper, id, declValue));
            }
        }
        Set<JavaClass<?>> refs = new HashSet<>();
        refs.add(wrapper);
        if (valueType instanceof JavaClass<?> valueClass) {
            refs.add(valueClass);
        }
        refs.add(HandlerHelper.MAPPER_S);
        // The {@code MapperS.<Value>ofNull()} witness emits the bare simple name (with the value-type
        // import added above) — no #197/#227-style import-collision resolution. Green-safe for every
        // current carrier: the arm fires ONLY on the non-compiling firing shape, and no carrier's
        // value type simple-name-collides with a different-canonical imported type. A future colliding
        // carrier would route through the witness-sentinel path (Seat-1 #317 SHOULD-FIX, no carrier).
        return JavaExpression.from(
                "(" + nameToken + " == null ? MapperS.<" + valueType.getSimpleName()
                        + ">ofNull() : MapperS.of(" + nameToken + ".getValue()))",
                null, refs, receiverBuilder.getStaticWildcardImports());
    }

    /**
     * facet collapsedMetaDerefChoiceOptionFeature (v3.1 flip seat 33, law C.1, rung R2a):
     * conjunct 3 of {@link #collapsedMetaDerefRewrapOrNull} - "the navigated feature is a feature
     * of the BARE VALUE type", extracted so that {@link
     * #conditionCarriesCollapsedMetaDerefHop} asks the IDENTICAL question (LAW 69: the call site
     * that shares a law with a declaration CONSULTS it), and WIDENED by one disjunct.
     *
     * <p><b>The widening.</b> {@code HandlerHelper.findAttributeOnDataType} is a plain
     * {@code name.equals(attr.name())} walk up {@code superType()}, and a data type that
     * {@code extends} a CHOICE has no data super-type at all - the parser stores that edge on
     * {@code RDataType.choiceSuperType()} instead. So a {@code -> <Option>} choice-option
     * projection on such a type reads as "not a feature of the value type" and the #317 deref
     * declines, leaving the plain re-wrap that navigates a value getter on the WRAPPER. Golden
     * drr 7.x {@code Price}: {@code final FieldWithMetaBasketConstituent
     * fieldWithMetaBasketConstituent = <chain>.get(); ... (fieldWithMetaBasketConstituent == null
     * ? MapperS.<BasketConstituent>ofNull() : MapperS.of(fieldWithMetaBasketConstituent
     * .getValue())).<Asset>map("getAsset", basketConstituent -> basketConstituent.getAsset())}
     * against the fork's {@code MapperS.of(<chain>.get()).<Asset>map("getAsset", ...)}. The
     * source is {@code standards-iosco-cde-version1-price-func.rosetta:98},
     * {@code ... basketConstituent only-element -> Asset -> Instrument -> ...}, over
     * {@code choice Observable: Asset / Basket / Index} and
     * {@code type BasketConstituent extends Observable}
     * ({@code cdm-6.20.2/.../observable-asset-type.rosetta:203-206, :212}).
     *
     * <p><b>MEASURED (LAW 75, both routes, at {@code fa49da010}).</b>
     * {@code [P33-COLLDEREF] verdict=decline:featNotOnValue} is <b>4 rows / 1 {@code where=}</b>
     * ({@code fn:Price}) - the ENTIRE {@code featNotOnValue} population corpus-wide, identical on
     * the IR route. The widened predicate can only admit rows that read that verdict today, so
     * the GREEN would-fire set is EMPTY by measurement, not by argument.
     *
     * <p><b>LAW 69</b>: the option reachability is {@link ChoiceSwitchSupport#findChoiceOptionPath},
     * the walk seat 31 law 4a promoted for exactly this family (facet
     * {@code choiceOptionNavLadderDeepHop}, the A.1 {@code choiceOptionProjectionTypeId} family),
     * so a path this seat admits is a path the ladder seats can render, hop for hop. Only a
     * DIRECT (one-hop) option qualifies here: the nav performs ONE projection step, and a deeper
     * path is the ladder seats' shape, not this one's.
     */
    private static boolean collapsedMetaDerefValueFeature(RFeatureCall expr,
            ExpressionCompiler compiler) {
        // The ONLY_ELEMENT case of resolveReceiverDataType is a type-transparent collapse
        // recovering the bare value RDataType (the same resolution the downstream witness uses).
        RDataType valueDt = resolveReceiverDataType(expr.receiver(), compiler);
        if (valueDt == null) {
            return false;
        }
        // NOT a metafield off the wrapper (`-> reference` / `-> scheme`), which navigates the
        // wrapper directly and must keep the plain re-wrap: those resolve on neither channel.
        if (HandlerHelper.findAttributeOnDataType(valueDt, expr.featureName()) != null) {
            return true;
        }
        return directChoiceOptionOnDataType(valueDt, expr.featureName(), compiler);
    }

    /**
     * facet collapsedMetaDerefChoiceOptionFeature (v3.1 flip seat 33, law C.1, rung R2a): is
     * {@code feature} the simple name of a DIRECT option of a CHOICE that {@code dt} (or one of
     * its data super-types) extends? {@code RDataType.superType()} is EMPTY for a
     * {@code type X extends <choice>} declaration - the edge lives on
     * {@code choiceSuperType()} - which is precisely why the attribute walk misses the
     * projection. Type names are capitalised and attribute names are not, so this disjunct can
     * never accidentally claim a plain attribute nav.
     */
    private static boolean directChoiceOptionOnDataType(RDataType dt, String feature,
            ExpressionCompiler compiler) {
        if (dt == null || feature == null || compiler == null) {
            return false;
        }
        Set<RDataType> seen = new HashSet<>();
        RDataType cur = dt;
        while (cur != null && seen.add(cur)) {
            RChoice choiceSuper = cur.choiceSuperType().orElse(null);
            if (choiceSuper != null) {
                List<ChoiceSwitchSupport.ChoiceOptionHop> path = new ArrayList<>();
                if (ChoiceSwitchSupport.findChoiceOptionPath(
                                new RChoiceTypeRef(choiceSuper.name(), List.of(), choiceSuper),
                                feature, compiler, path, new HashSet<>())
                        && path.size() == 1) {
                    return true;
                }
            }
            cur = cur.superType().orElse(null);
        }
        return false;
    }

    /**
     * facet nestedElseStatementSeatCapture (v3.1 flip seat 33, law C.1, rung R2b): does this
     * CONDITION carry a #317 collapsed-meta only-element nav-receiver hop that reaches the
     * wrapper gate - i.e. will compiling it register a {@code final <Wrapper> <name> =
     * <chain>.get();} statement hoist? A condition that does cannot flatten into an
     * {@code } else if (...)} header, because a declaration cannot live there; the ite-chain
     * renderer must give it a STATEMENT seat, exactly as it already does for the #179 bare-fn
     * Boolean hoist.
     *
     * <p><b>A pure AST + type read.</b> Never a throwaway compile: this seat shares a
     * {@code StatementHoistSession} with the render that follows, and a discarded compile BURNS
     * names in it (the E.3 {@code explicitClosureParamNameBurn} family; the #372 probe one arm up
     * is the same defect, repaired by law D.12). The three conjuncts below are exactly the three
     * STRUCTURAL conjuncts of {@link #collapsedMetaDerefRewrapOrNull} - the ONLY_ELEMENT receiver
     * shape, the meta wrapper on the collapse leaf, and
     * {@link #collapsedMetaDerefValueFeature} - so the predicate and the producer cannot
     * disagree about which hops hoist (LAW 69). The producer's remaining conjuncts are the
     * {@code isIdentifier(baseName)} name test and the SCOPE-dependent drain-seat reachability,
     * which are the caller's questions, not this one's.
     *
     * <p>The node walk mirrors {@code HandlerHelper.staticOperatorMembersUsed}'s bounded DFS
     * over {@code RNode.children()}; the guard is a structural-anomaly backstop (a condition
     * subtree is orders of magnitude smaller).
     */
    public static boolean conditionCarriesCollapsedMetaDerefHop(RExpression condition,
            ExpressionCompiler compiler) {
        if (condition == null || compiler == null) {
            return false;
        }
        java.util.Deque<RNode> stack = new java.util.ArrayDeque<>();
        stack.push(condition);
        int guard = 0;
        while (!stack.isEmpty() && guard++ < 100_000) {
            RNode cur = stack.pop();
            if (cur instanceof RFeatureCall fc
                    && fc.receiver() instanceof RListOpExpr le
                    && le.op() == ListOp.ONLY_ELEMENT
                    && metaWrapperOf(onlyElementLeafAttribute(le.argument(), compiler), compiler)
                            != null
                    && collapsedMetaDerefValueFeature(fc, compiler)) {
                return true;
            }
            for (RNode child : cur.children()) {
                if (child != null) {
                    stack.push(child);
                }
            }
        }
        return false;
    }

    /**
     * The leaf attribute of an {@code only-element} argument — walk through nested list-ops to the
     * terminal navigation, resolving a plain {@link RFeatureCall} via its {@code resolvedFeature}
     * (gm-aware {@link #fallbackResolveFeature} fallback for an alias-rooted chain) or a disguised
     * 2-name {@link REnumValueRef} via {@link #resolveDisguisedFeature}. {@code null} for any
     * other argument shape.
     */
    // Package-visible: the canonical leaf-through-list-ops resolver, reused by
    // LiteralHandler's #337 listLiteralNavMetaWitness arm (a list-literal element is a
    // `<meta-nav> first` shape — the SAME nav-leaf resolution the #317 collapsed-meta-deref
    // uses). Pure read-only; unchanged behaviour. Public since v3.2 seat 13 (#634, commit 4):
    // FunctionExpressionRenderer's whole-output SET element-wrapper recovery (the R12 / M7a heal,
    // oracle group extract-meta-elem-deref-function) reads it from the function package.
    public static RAttribute onlyElementLeafAttribute(RExpression argument,
            ExpressionCompiler compiler) {
        RExpression cur = argument;
        while (cur instanceof RListOpExpr lop) {
            cur = lop.argument();
        }
        if (cur instanceof RFeatureCall fc) {
            return fc.resolvedFeature().orElseGet(() -> fallbackResolveFeature(fc, compiler));
        }
        if (cur instanceof REnumValueRef evr) {
            return resolveDisguisedFeature(evr, compiler, new HashSet<>());
        }
        // A single-name feature of the rule input / lambda item (`quantity`) parses as an
        // RSymbolReference bound to its RAttribute (the whole-output + map-lambda carriers);
        // fall back to the item-type resolution for an unbound reference.
        if (cur instanceof RSymbolReference sym) {
            return sym.symbol().filter(RAttribute.class::isInstance).map(RAttribute.class::cast)
                    .orElseGet(() -> implicitItemSymbolLeaf(sym, compiler));
        }
        return null;
    }

    /**
     * Built-in record-type feature navigation (Engine PR #147, facet
     * {@code date_record_feature_nav}).
     *
     * <p>A navigation whose RECEIVER's inferred type is a built-in record —
     * {@code dateTime} (Java {@link java.time.LocalDateTime}) or {@code zonedDateTime}
     * (Java {@link java.time.ZonedDateTime}) — accessing the {@code date} record feature
     * has no POJO getter. Upstream (the vendored 9.83.0
     * {@code RecordJavaUtil.recordFeatureToLambda} dispatch for
     * {@code RDateTimeType}/{@code RZonedDateTimeType}, wired by
     * {@code ExpressionGenerator.recordCall}) renders
     * <pre>receiver.&lt;Date&gt;map("Date", «v» -&gt; Date.of(«v».toLocalDate()))</pre>
     * where the lambda var is {@code dt} ({@code dateTime}) / {@code zdt}
     * ({@code zonedDateTime}). The fork's generic getter path below instead emits the
     * non-compiling {@code .map("getDate", zonedDateTime -> zonedDateTime.getDate())}
     * ({@code ZonedDateTime}/{@code LocalDateTime} carry no {@code getDate()}), because
     * a record feature has no {@code RAttribute} ({@code resolvedFeature} is empty and
     * {@code fallbackResolveFeature} returns {@code null} — a record is not an
     * {@code RDataType}).
     *
     * <p>Four record arms: the {@code date} feature on {@code dateTime}/{@code zonedDateTime}
     * (the RecordJavaUtil lambda form above); — facet date_accessor_method_ref,
     * PR #366 — the {@code year}/{@code month}/{@code day} accessors on a DATE-record
     * receiver (the method-ref form {@code .<Integer>map("Year", Date::getYear)},
     * golden carriers CompareDateTo + YearFraction ×2); and — facets
     * recordTimeMemberNav + recordTimezoneMemberNav, PR #434 — the {@code time}
     * method-ref form on both kinds plus the {@code timezone} lambda on
     * {@code zonedDateTime} (the func-record-* oracle goldens; these were the
     * deferred fall-through features until those goldens exercised them). Returns
     * {@code null} for the dominant non-record receiver
     * (→ getter path) and defensively when the receiver's type cannot be resolved
     * (the stateless unit {@code ExpressionCompiler} supplies no
     * {@code GeneratorModel}).
     *
     * @return the record-feature mapper expression, or {@code null} to defer to the
     *         generic getter path
     */
    private JavaStatementBuilder tryRecordFeatureNav(RFeatureCall expr, String receiver,
            JavaStatementBuilder receiverBuilder, ExpressionContext ctx, ExpressionCompiler compiler) {
        GeneratorModel gm = compiler.getGeneratorModel();
        if (gm == null || gm.workspace() == null) {
            return null;
        }
        RType recvType = resolveReceiverRType(expr.receiver(), gm, compiler);
        if (!(recvType instanceof RRecordType rt)) {
            return null;
        }
        // facet date_accessor_method_ref (PR #366): the year/month/day accessors on a
        // DATE-record receiver render the upstream RecordJavaUtil method-ref form
        // (.<Integer>map("Year", Date::getYear)) — the vendored RDateType dispatch emits
        // the bare method ref with the capitalized feature name as the label and registers
        // NO lambda var. Gated to RecordKind.DATE + the three accessor features; the
        // shared isDateRecordFeature gate (the disguised-chain synthesis + alias-leaf
        // consumers) is deliberately untouched — this arm fires only at the direct
        // record-nav seat. A POJO attribute named day/month/year resolves to an
        // RDataType receiver and never reaches here (the LegacyValuationTime datarule
        // class keeps its getter-lambda render).
        if (rt.kind() == RecordKind.DATE && isDateAccessorFeature(expr.featureName())) {
            String cap = Character.toUpperCase(expr.featureName().charAt(0))
                    + expr.featureName().substring(1);
            Set<JavaClass<?>> accessorRefs = new HashSet<>();
            accessorRefs.addAll(receiverBuilder.getRefs());
            accessorRefs.add(compiler.getTypeUtil().DATE);
            return JavaExpression.from(
                    receiver + ".<Integer>map(\"" + cap + "\", Date::get" + cap + ")",
                    null,
                    accessorRefs,
                    receiverBuilder.getStaticWildcardImports());
        }
        // facet recordTimeMemberNav (PR #434, finding #24): the `time` feature on a
        // dateTime/zonedDateTime receiver — upstream RecordJavaUtil's
        // RDateTimeType/RZonedDateTimeType `time` dispatch is the BARE method ref
        // (no lambda var): `.<LocalTime>map("Time", LocalDateTime::toLocalTime)` /
        // `.<LocalTime>map("Time", ZonedDateTime::toLocalTime)` (the
        // func-record-datetime-members / func-record-zoned-members oracle goldens —
        // the seats this method's javadoc deferred until a golden exercised them).
        // The pre-fix getter fall-through (`.map("getTime", dateTime ->
        // dateTime.getTime())`) never compiled — java.time carries no getTime() —
        // so every carrier is waivered (green-safe by construction).
        if ("time".equals(expr.featureName())
                && (rt.kind() == RecordKind.DATE_TIME
                        || rt.kind() == RecordKind.ZONED_DATE_TIME)) {
            boolean zoned = rt.kind() == RecordKind.ZONED_DATE_TIME;
            Set<JavaClass<?>> timeRefs = new HashSet<>();
            timeRefs.addAll(receiverBuilder.getRefs());
            timeRefs.add(compiler.getTypeUtil().LOCAL_TIME);
            timeRefs.add(zoned ? compiler.getTypeUtil().ZONED_DATE_TIME
                    : compiler.getTypeUtil().LOCAL_DATE_TIME);
            return JavaExpression.from(
                    receiver + ".<LocalTime>map(\"Time\", "
                            + (zoned ? "ZonedDateTime" : "LocalDateTime")
                            + "::toLocalTime)",
                    null,
                    timeRefs,
                    receiverBuilder.getStaticWildcardImports());
        }
        // facet recordTimezoneMemberNav (PR #434, finding #24): the `timezone`
        // feature on a zonedDateTime receiver — upstream's lambda form
        // `.<String>map("Timezone", _zdt -> _zdt.getZone().getId())` (the lambda
        // var is the same scope-disambiguated `zdt` base the date-member arm
        // below uses; String is java.lang — no import). Same green-safety class:
        // the getter fall-through (`.getTimezone()`) never compiled.
        if ("timezone".equals(expr.featureName())
                && rt.kind() == RecordKind.ZONED_DATE_TIME) {
            String tzVar = ctx.scope().disambiguate("zdt");
            return JavaExpression.from(
                    receiver + ".<String>map(\"Timezone\", " + tzVar + " -> "
                            + tzVar + ".getZone().getId())",
                    null,
                    new HashSet<>(receiverBuilder.getRefs()),
                    receiverBuilder.getStaticWildcardImports());
        }
        if (!isDateRecordFeature(expr.featureName(), rt)) {
            return null;
        }
        // Lambda var: `zdt` (zonedDateTime) / `dt` (dateTime), disambiguated against the
        // body scope exactly as the getter-nav lambda var is (resolveLambdaVarName) and as
        // upstream's scope.lambdaScope.createUniqueIdentifier does.
        String var = ctx.scope().disambiguate(
                rt.kind() == RecordKind.ZONED_DATE_TIME ? "zdt" : "dt");
        Set<JavaClass<?>> refs = new HashSet<>();
        refs.addAll(receiverBuilder.getRefs());
        // <Date> witness + the Date.of(...) body both resolve to com.rosetta.model.lib.records.Date.
        refs.add(compiler.getTypeUtil().DATE);
        return JavaExpression.from(
                receiver + ".<Date>map(\"Date\", " + var + " -> Date.of(" + var + ".toLocalDate()))",
                null,
                refs,
                receiverBuilder.getStaticWildcardImports());
    }

    /**
     * The {@code date} record-feature gate shared by {@link #tryRecordFeatureNav}
     * and {@code ReferenceHandler}'s disguised record-leaf chain synthesis (facet
     * maxmin_body_coercion arm C) — the synthesis admits a chain exactly when the
     * record nav's {@code date} arm will fire on it, so the two cannot disagree.
     * This gate covers ONLY the {@code date} feature on {@code dateTime}/
     * {@code zonedDateTime}; the {@code year}/{@code month}/{@code day} accessors on
     * a DATE receiver have their own nav-seat-only gate ({@code isDateAccessorFeature},
     * PR #366) that the synthesis/alias consumers deliberately do NOT read;
     * {@code time}/{@code timezone} stay on the getter path (already divergent +
     * waivered) until a golden exercises them.
     */
    static boolean isDateRecordFeature(String featureName, RRecordType rt) {
        return "date".equals(featureName)
                && (rt.kind() == RecordKind.DATE_TIME || rt.kind() == RecordKind.ZONED_DATE_TIME);
    }

    /**
     * The DATE-record accessor gate (facet date_accessor_method_ref, PR #366):
     * {@code year}/{@code month}/{@code day} on a {@link RecordKind#DATE} receiver.
     * Consulted ONLY by {@link #tryRecordFeatureNav}'s method-ref arm — deliberately
     * NOT folded into {@link #isDateRecordFeature}, whose other consumers (the
     * disguised-chain synthesis + the alias-leaf walk) must keep their exact
     * admission set (the #365 positive-seat-gate law).
     */
    private static boolean isDateAccessorFeature(String featureName) {
        return "year".equals(featureName) || "month".equals(featureName)
                || "day".equals(featureName);
    }

    /**
     * Resolve a navigation receiver's M4 {@link RType} via the symbol table
     * ({@link GeneratorModel#resolveTypeCall}) — the SAME mechanism the witness path
     * ({@link #resolveJavaSimpleName}) uses, and reliable in both the unit and D11
     * generation contexts (unlike {@code RWorkspace.getInferredType}, whose
     * inference cache is not populated for a {@code GeneratorModel} built directly
     * from a workspace). Handles the receiver shapes the record-feature nav fires on:
     * a {@link RSymbolReference} to an {@link RAttribute} (the SOLE-flip cases — a
     * function input typed {@code dateTime}/{@code zonedDateTime}) and a chained
     * {@link RFeatureCall} (best-effort, for completeness). A builtin record may
     * resolve through an {@link RAliasType}, so the alias chain is unwrapped.
     *
     * <p>Package-private (was private): facet maxmin_body_coercion arm C's
     * record-leaf chain synthesis reads the SAME resolution on the SAME synthesized
     * head node {@link #tryRecordFeatureNav} will read, so admission and firing
     * align by construction.
     *
     * @return the resolved {@link RType}, or {@code null} when the receiver carries
     *         no resolvable type call
     */
    static RType resolveReceiverRType(RExpression receiver, GeneratorModel gm) {
        return resolveReceiverRType(receiver, gm, null);
    }

    /**
     * gm-aware overload (facet date_record_nav_resolution, PR #203). Resolves two
     * additional receiver shapes the static 2-arg path could not, so the
     * {@code date} record-feature nav fires on a transitive-fpml date chain whose
     * receiver navigates to a {@code zonedDateTime}:
     * <ul>
     *   <li>a chained {@link RFeatureCall} whose feature is not pre-resolved in the
     *       IR resolves via the gm-aware
     *       {@link #fallbackResolveFeature(RFeatureCall, ExpressionCompiler)}
     *       (the static {@link #fallbackResolveFeature(RFeatureCall)} walks only
     *       pre-resolved {@code RDataType} receivers); and</li>
     *   <li>a DISGUISED {@code head -> feature} 2-name chain (parsed as an
     *       {@link REnumValueRef} — e.g. the {@code MapperS.of(fpmlFra)
     *       .<ZonedDateTime>map("getAdjustedTerminationDate", …)} receiver of a
     *       {@code -> date} step), resolved to its leaf attribute via the SAME
     *       {@link #resolveDisguisedFeature(REnumValueRef, ExpressionCompiler, java.util.Set)}
     *       walk the witness / lambda-var path uses.</li>
     * </ul>
     * The fork previously returned {@code null} for both, so {@link #tryRecordFeatureNav}
     * declined and emitted the non-compiling {@code .map("getDate", zonedDateTime ->
     * zonedDateTime.getDate())} ({@code ZonedDateTime} has no {@code getDate()}) instead
     * of the upstream {@code .<Date>map("Date", zdt -> Date.of(zdt.toLocalDate()))} record
     * form. The strict {@link RRecordType} {@code DATE_TIME}/{@code ZONED_DATE_TIME} gate
     * in {@link #tryRecordFeatureNav} ({@code recvType instanceof RRecordType &&
     * isDateRecordFeature}) is unchanged and is what protects green: this overload may now
     * return a NON-null {@code RDataTypeRef} for a disguised data-type receiver it
     * previously left {@code null}, but that gate still declines it, so a data-type
     * receiver with a real {@code getDate()} getter is untouched by construction. A
     * {@code null} {@code compiler}
     * (the 2-arg callers — {@code ControlFlowHandler.thenItemJavaClass}, which never passes
     * an {@link REnumValueRef} here) keeps the static-fallback behaviour exactly.
     */
    public static RType resolveReceiverRType(RExpression receiver, GeneratorModel gm,
            ExpressionCompiler compiler) {
        RTypeCall tc = null;
        // seat 21 H1: an already-resolved RType (a rule's inferred output) that has no
        // typeCall to feed the shared resolve tail; joins the SAME alias-strip exit below.
        RType direct = null;
        if (receiver instanceof RSymbolReference sr) {
            tc = sr.symbol()
                    .filter(RAttribute.class::isInstance)
                    .map(RAttribute.class::cast)
                    .map(RAttribute::typeCall)
                    .orElse(null);
            // facet dispatchVariantParamResolution (PR #369): an UNRESOLVED bare symbol
            // in a dispatch VARIANT body types from the dispatch BASE's declared input —
            // the record-feature nav over a `date`-typed base param fires exactly like a
            // resolved input's (`MapperS.of(startDate).<Integer>map("Year", Date::getYear)`,
            // YearFraction _30_360/_30E_360). Null for every non-variant seat.
            if (tc == null && sr.symbol().isEmpty()) {
                RAttribute baseInput = HandlerHelper.dispatchBaseInput(sr, sr.name());
                if (baseInput != null) {
                    tc = baseInput.typeCall();
                }
            }
            // facet fnCallRecordReceiver (PR #377, M3): a FUNCTION-call receiver
            // (`GetExecutionTimestamp(reportableEvent) -> date`) types as the callee
            // OUTPUT's typeCall — the #359-F-4 bare-invokable-head law at the DIRECT
            // symbol-call seat, so the record-feature nav fires over a
            // zonedDateTime-returning call (golden EMIR/UKEMIR venueMic
            // `.<Date>map("Date", zdt -> Date.of(zdt.toLocalDate()))`; the fork's
            // getter form never compiled). tryRecordFeatureNav's strict RRecordType
            // gate is unchanged — every data-type-returning call keeps the getter
            // path; the Object-gated recovery consumers only ever replace an
            // erased-to-Object read (their existing fallback contracts).
            if (tc == null) {
                RFunction srCallee = sr.symbol()
                        .filter(RFunction.class::isInstance)
                        .map(RFunction.class::cast)
                        .orElse(null);
                if (srCallee != null) {
                    tc = srCallee.output().map(RAttribute::typeCall).orElse(null);
                }
            }
            // v3.1 flip seat 21 — facet ruleReceiverRecordFeature, arm H1: a RULE-call receiver
            // (`ExecutionTimestamp(transaction) -> date`, the bare `extract ValuationTimestamp ->
            // date`) types as the rule's INFERRED output — the #377 fnCallRecordReceiver law at
            // the sibling symbol class. An RRule is NOT an RFunction (disjoint RRootElement
            // subclasses), so the arm above never served it and the walk returned null: the
            // `date` record nav fell to the non-compiling `.map("getDate", x -> x.getDate())`
            // getter (drr 7.x ExecutionISIN/ExecutionVenue + the valuation EventDateRule; golden
            // `.<Date>map("Date", zdt -> Date.of(zdt.toLocalDate()))`). The type is an RType
            // already (no typeCall to resolve) — the shared tail's alias strip applies to it.
            // gm-only (no compiler), so BOTH overloads get it. LAW 75 (the seat-21 probe over all
            // 275 rows): 575 rule-symbol receivers reach this walk, ALL from tryRecordFeatureNav;
            // 12 infer zonedDateTime (= the carriers), the other 563 infer data types the
            // record gate declines — byte-neutral there by construction.
            if (tc == null && direct == null
                    && sr.symbol().orElse(null) instanceof RRule srRule) {
                direct = HandlerHelper.ruleInferredOutputRType(srRule, gm);
            }
        } else if (receiver instanceof RFeatureCall fc) {
            RAttribute attr = fc.resolvedFeature().orElseGet(() -> compiler != null
                    ? fallbackResolveFeature(fc, compiler)
                    : fallbackResolveFeature(fc));
            tc = attr == null ? null : attr.typeCall();
        } else if (receiver instanceof REnumValueRef evr && evr.enumeration().isEmpty()) {
            RAttribute leaf = compiler != null
                    ? resolveDisguisedFeature(evr, compiler, null)
                    : resolveDisguisedFeature(evr);
            // facet dateTimeRecordFeatureNav (PR #359, F-4): a disguised 2-name receiver
            // whose HEAD is a bare-INVOKABLE reference (`PositionForEvent -> openDateTime`
            // — the parser's GlobalResolutionPass binds resolvedSymbol to the RFunction;
            // the #280 render synthesis reads the SAME binding) types as the callee
            // OUTPUT's valueName attribute — so a `-> date` record nav over it fires
            // (EffectiveDateRule iosco/common drr: golden `.<Date>map("Date", dt ->
            // Date.of(dt.toLocalDate()))`; the getter form never compiled). Scoped to
            // THIS read (the record-nav / recovery consumers) — the shared
            // resolveDisguisedFeature keeps its narrower arms (the #224/#225
            // conversion-scope boundary: its witness/naming consumers need their own
            // carriers).
            if (leaf == null && compiler != null && evr.valueName() != null
                    && evr.resolvedSymbol().orElse(null) instanceof RFunction fnHead) {
                RAttribute out = fnHead.output().orElse(null);
                RDataType outDt = out == null ? null : attributeToDataType(out, compiler);
                if (outDt != null) {
                    leaf = HandlerHelper.findAttributeOnDataType(outDt, evr.valueName());
                }
            }
            // v3.1 flip seat 21 — facet ruleReceiverRecordFeature, arm H2: a RULE-BODY disguised
            // 2-name chain (`collateralDetails -> collateralTimestamp -> date`; the receiver of the
            // `-> date` hop is the REnumValueRef `collateralDetails -> collateralTimestamp`) types
            // from the parser's resolvedAttributeChain binding — the SAME binding
            // resolveLambdaVarName already reads to name the lambda var `zonedDateTime` (LAW 69:
            // the namer knew the receiver's type, the admission did not). resolveDisguisedFeature
            // declines every rule-body chain (its walks are function-/condition-scoped), so this
            // arm is monotone: it fires only where the walk returned null. Gated on `compiler !=
            // null` exactly like the #359 F-4 arm above — the 2-arg overload's contract ("a null
            // compiler keeps the static-fallback behaviour exactly") is untouched. LAW 75 (the
            // seat-21 probe): 6,506 bound-chain receivers reach this walk, ALL from
            // tryRecordFeatureNav; only the 4 margin EventDateRule `-> date` hops are record-typed.
            if (leaf == null && compiler != null) {
                leaf = evr.resolvedAttributeChain()
                        .map(REnumValueRef.AttributeChain::feature)
                        .orElse(null);
            }
            tc = leaf == null ? null : leaf.typeCall();
        }
        if (tc == null) {
            return direct == null ? null : HandlerHelper.stripAliases(direct);
        }
        RType rt = gm.resolveTypeCall(tc);
        rt = HandlerHelper.stripAliases(rt);
        return rt;
    }

    /**
     * Recover the concrete ELEMENT type of a hoisted {@code final Mapper*<X> thenArg}
     * declaration whose parser-snapshot inferred type erased to {@code Object} —
     * facet {@code objFallback_witness_recovery} (PR #238). PROMOTED verbatim from
     * {@code FunctionExpressionRenderer.thenArgItemRType} (PR #204) so the deep-then
     * hoist path {@link com.regnosys.rosetta.generator.java.expression.handlers.CollectionHandler}{@code .tryDeepThenHoist}
     * shares the SAME recovery walk as the SET-position {@code renderThenExtractSet}
     * (the deep path previously had ONLY a compiled-value-type fallback, which is
     * {@code null} for a {@code filter} terminal). Unwraps the
     * {@code then}/{@code filter}/{@code extract}/list-op wrappers to the
     * element-producing leaf, then resolves it via the gm-aware
     * {@link #resolveReceiverRType(RExpression, GeneratorModel, ExpressionCompiler)}
     * (resolved/unresolved feature calls, disguised {@code head -> feature}
     * {@code REnumValueRef} chains, alias roots - and NO {@code RImplicitVariable} arm: a
     * then-piped implicit owner is resolved by {@link #lambdaItemReceiverRType}'s own
     * recursion, v3.2 seat 6).
     * A {@code filter} preserves its argument's element type; an
     * {@code extract}/{@code then} body produces the element. The trailing
     * {@code RSymbolReference} fallback re-roots a bare implicit-item feature the
     * linker mis-bound to a same-named global root onto the enclosing item type.
     * Returns {@code null} when no concrete type resolves — the caller keeps the
     * pre-facet {@code Object} (non-compiling-but-already-waivered, so no regression).
     * STRICTLY a fallback for its original consumers: never consulted when the snapshot
     * already resolved a non-Object type. Since v3.2 seat 6 also the PRIMARY resolution of a
     * lambda item's owner argument for {@link #lambdaItemReceiverRType} (the deep-path util
     * injection), which reads it before any snapshot.
     */
    public static RType recoverThenArgItemRType(RExpression e, ExpressionCompiler compiler) {
        GeneratorModel gm = compiler == null ? null : compiler.getGeneratorModel();
        if (e == null || gm == null) {
            return null;
        }
        if (e instanceof RThenExpr t) {
            RExpression bodyExpr = t.body().map(RInlineFunction::body).orElse(null);
            // A then-body that FILTERS the piped item (`thenArg(k-1).filterItemNullSafe(...)`)
            // preserves the then-ARGUMENT's element type; the predicate's filter receiver is
            // the implicit item (unresolvable standalone), so recover from the then argument
            // rather than the predicate body (PR #238 — GetTransactionInformationForRegime
            // thenArg1 = thenArg0.filter(...), declared MapperC<TransactionInformation>).
            if (bodyExpr instanceof RFilterExpr f && f.argument() instanceof RImplicitVariable) {
                return recoverThenArgItemRType(t.argument(), compiler);
            }
            return bodyExpr == null ? null : recoverThenArgItemRType(bodyExpr, compiler);
        }
        if (e instanceof RExtractExpr x) {
            return recoverThenArgItemRType(x.body().body(), compiler);
        }
        if (e instanceof RFilterExpr f) {
            return recoverThenArgItemRType(f.argument(), compiler);
        }
        // facet objFallbackSortThenArg (PR #343): sort is element-PRESERVING (it
        // reorders, keeping the list's element type), so recover through its
        // argument exactly like filter/list-op. Without this arm a
        // `Mapper*<Object> thenArg = <alias>.sort(...)` (FindLatestAssignedIdentifier's
        // `assignedIdentifiersWithVersion sort [item -> version] then last`) stayed
        // Object — the sort node blocked the walk before it reached the alias leaf.
        if (e instanceof RSortExpr s) {
            return recoverThenArgItemRType(s.argument(), compiler);
        }
        if (e instanceof RListOpExpr lo) {
            return recoverThenArgItemRType(lo.argument(), compiler);
        }
        RType direct = resolveReceiverRType(e, gm, compiler);
        if (direct != null) {
            return direct;
        }
        // facet ctorFieldBareInvokableThenArg (PR #385): a disguised 2-name chain whose
        // root is an ENCLOSING extract's EXPLICIT closure param resolves its leaf type
        // through the SAME #360 walk the render's witness reads (render-truth lockstep —
        // the T385D evidence: this walk nulled exactly where the render already emitted
        // the `<BigDecimal>` witness; GetReportableQuantityPeriodLeg1/2's ctor-field
        // thenArg base `quantityPeriod -> value`). Basic leaves resolve too (the decl
        // element `MapperS<BigDecimal>`); the alias-unwrap mirrors the bare-symbol arm
        // below.
        if (e instanceof REnumValueRef evr && evr.enumeration().isEmpty() && compiler != null) {
            RAttribute cpLeaf = closureParamDisguisedLeaf(evr, compiler);
            if (cpLeaf != null && cpLeaf.typeCall() != null) {
                RType rt = gm.resolveTypeCall(cpLeaf.typeCall());
                rt = HandlerHelper.stripAliases(rt);
                return rt;
            }
        }
        if (e instanceof RSymbolReference sr && sr.name() != null) {
            RDataType item = implicitItemDataType(e, compiler);
            if (item != null) {
                RAttribute attr = HandlerHelper.findAttributeOnDataType(item, sr.name());
                if (attr != null && attr.typeCall() != null) {
                    RType rt = gm.resolveTypeCall(attr.typeCall());
                    rt = HandlerHelper.stripAliases(rt);
                    return rt;
                }
            }
        }
        return null;
    }

    /**
     * objFallback_witness_recovery (PR #238) Stage B — alias base: if a hoisted
     * then-arg's element-producing leaf is an ALIAS call (e.g.
     * {@code interestRatePayouts(product)}), return the alias's CONCRETE element Java
     * type. The {@code thenArg} declaration then emits {@code Mapper*<? extends X>}
     * (the alias method signature's upper-bounded return form — golden
     * {@code InterestRateLeg1Basis}: {@code MapperC<? extends InterestRatePayout> thenArg
     * = interestRatePayouts(product)...}); a direct navigation (Stage A) stays the
     * invariant {@code Mapper*<X>}. The element type is recovered by walking the alias's
     * OWN expression body via {@link #recoverThenArgItemRType}. The sibling
     * {@link #tryAliasReceiverMapperType} cannot serve here — it exposes the item type only for
     * META aliases (see the inline note below). That body-walk and the alias method's
     * {@code FunctionAliasHelper}-rendered signature
     * are DISTINCT walks; they agree for the {@code InterestRateLeg1/2Basis} carriers (byte-oracle
     * EXACT). Were they ever to disagree for a future alias, the decl would emit
     * {@code Mapper*<? extends X>} against a signature returning {@code Mapper*<? extends Y>} —
     * still {@code incompatible-types} (non-compiling, hence already waivered): non-regressing but
     * non-flipping, never a GREEN regression. Returns {@code null} when the leaf is not an alias
     * (caller keeps its non-alias recovery / the pre-facet {@code Object}).
     */
    public static JavaClass<?> aliasDerivedThenArgItemType(RExpression e, ExpressionCompiler compiler) {
        if (compiler == null || compiler.getTypeTranslator() == null) {
            return null;
        }
        AliasResolution alias = resolveAliasShortcut(thenArgElementLeaf(e));
        if (alias == null || alias.shortcut().expression() == null) {
            return null;
        }
        // Resolve the alias's element type by walking its OWN expression body — the alias
        // signature renders `Mapper*<? extends X>` from this same body. (tryAliasReceiverMapperType
        // exposes the item type only for META aliases — facet meta_coercion_numbering A1 — so a
        // non-meta alias like `interestRatePayouts: EconomicTermsForProduct(product) -> payout ->
        // interestRatePayout` is recovered here via the shared recovery walk over the body.)
        RExpression body = alias.shortcut().expression();
        RType item = recoverThenArgItemRType(body, compiler);
        // PR #241: a FUNCTION-CALL alias body (e.g. `product: ProductForTrade(TradeForEvent(...))`)
        // parses as an RSymbolReference whose symbol is an RFunction — a shape
        // recoverThenArgItemRType's resolveReceiverRType does NOT resolve (it handles
        // RSymbolReference→RAttribute / RFeatureCall / disguised-REnumValueRef, but not →RFunction;
        // its data-type sibling resolveReceiverDataType DOES carry the RFunction arm). Recover the
        // callee's declared OUTPUT type here so a function-call alias body's element type — and the
        // `Mapper*<? extends X>` alias method signature it agrees with — resolve. SCOPED to the
        // alias-body walk: the SHARED recovery (Stage A, direct then-arguments) is unchanged, so a
        // direct then-argument function call keeps its existing path (a separate, broader facet).
        if (item == null && compiler.getGeneratorModel() != null
                && body instanceof RSymbolReference fnRef
                && fnRef.symbol().orElse(null) instanceof RFunction fn) {
            RAttribute out = fn.output().orElse(null);
            if (out != null && out.typeCall() != null) {
                RType rt = compiler.getGeneratorModel().resolveTypeCall(out.typeCall());
                rt = HandlerHelper.stripAliases(rt);
                item = rt;
            }
        }
        // facet condAliasWildcardThenArg (PR #366): a CONDITIONAL alias body (the
        // if/else-if nav ladder — GetBasketConstituents `alias basketConstituents:
        // if … exists then <nav> else if …`) recovers its element from the FIRST
        // ladder then-arm the shared walk resolves to a MODEL type. The alias
        // SIGNATURE walk (doAnalyze → buildMapperReturnType) types the SAME body
        // `MapperC<? extends X>`, so the thenArg decl must agree (the #178 same-walk
        // law; the caller's equals-gate declines a disagreeing recovery). MODEL-typed
        // arms only — RDataTypeRef/RChoiceTypeRef at the RTYPE level (the #169 law):
        // the signature emits the wildcard ONLY for isRosettaModelType elements (a
        // basic-element alias renders INVARIANT), so a basic-typed arm must not admit.
        // SCOPED to the alias-body walk like the #241 fn-call arm above.
        if (item == null && body instanceof RConditionalExpr condBody) {
            item = condLadderFirstModelArmItem(condBody, compiler);
        }
        JavaClass<?> jc = item == null ? null : compiler.getTypeTranslator().toJavaReferenceType(item);
        return jc != null && !"Object".equals(jc.getSimpleName()) ? jc : null;
    }

    /**
     * facet aliasSigWildcardDecl (seat 28, law D): the SIGNATURE-walk sibling of
     * {@link #aliasDerivedThenArgItemType} - the element an alias-leaf level's decl must
     * declare when the alias method's own emitted signature is
     * {@code Mapper*<? extends T>}, or {@code null} when the leaf is not an alias
     * reference, the alias's element is NOT a Rosetta model type (a basic/enum/record
     * alias renders INVARIANT and must stay invariant), or the type infrastructure is
     * absent (the stateless unit compiler).
     *
     * <p>Reach is IDENTICAL to {@link #aliasDerivedThenArgItemType}'s by construction:
     * the same {@link #thenArgElementLeaf} unwrap and the same {@code resolveAliasShortcut}
     * resolution. The two differ only in HOW they type the alias - that one walks the
     * alias's own BODY, this one asks the alias's SIGNATURE producer
     * ({@code FunctionAliasHelper.aliasSignatureWildcardElementOrNull}, the same single
     * {@code inferExpressionType} walk the emitted signature came from). Where the body
     * walk resolves they agree; the LAW-75 probe measured the disagreement class where it
     * does NOT (drr 7.x {@code lastAvailableSpotPrice}: producer {@code emits=WILDCARD},
     * decl seat {@code wildcard=false}).
     */
    public static JavaClass<?> aliasSignatureWildcardItemType(RExpression e,
            ExpressionCompiler compiler) {
        if (compiler == null) {
            return null;
        }
        AliasResolution alias = resolveAliasShortcut(thenArgElementLeaf(e));
        if (alias == null || alias.shortcut().expression() == null) {
            return null;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        JavaTypeTranslator translator = compiler.getTypeTranslator();
        JavaTypeUtil typeUtil = compiler.getTypeUtil();
        if (gm == null || translator == null || typeUtil == null) {
            return null;
        }
        return new com.regnosys.rosetta.generator.java.function.FunctionAliasHelper(
                gm, translator, typeUtil)
                .aliasSignatureWildcardElementOrNull(alias.shortcut(), alias.enclosing());
    }

    /**
     * facet aliasSigWildcardDecl (seat 28, law D): true when {@link #thenArgElementLeaf}
     * returns {@code e} ITSELF - the value carries no {@code then}/{@code extract}/
     * {@code filter}/{@code sort}/list-op above the leaf, so the rendered initialiser is
     * the leaf's own text. The decl-seat scope gate: only then does the initialiser's
     * declared type govern the local's type outright. A navigation ABOVE an alias call
     * produces the navigated attribute's own invariant element instead, and keeps
     * today's bytes.
     */
    public static boolean thenArgElementLeafIsWholeValue(RExpression e) {
        return e != null && thenArgElementLeaf(e) == e;
    }

    /**
     * facet aliasSigWildcardDecl (seat 28, law D): the LADDER form - true when ANY arm of
     * the conditional chain (a rung's then-branch, a nested conditional's arms, or the
     * terminal else) IS a bare reference to a wildcard-signed alias, so a local assigned
     * from that arm must be declared {@code Mapper*<? extends T>}. Depth-first over
     * then-branches then the else chain - the SAME order
     * {@link #condLadderFirstModelArmItem} walks, so the two ladder walks in this file
     * cannot disagree about what an "arm" is.
     */
    public static boolean ladderBareAliasWildcardArm(RConditionalExpr cond,
            ExpressionCompiler compiler) {
        if (cond == null || compiler == null) {
            return false;
        }
        RConditionalExpr cur = cond;
        while (cur != null) {
            RExpression thenArm = cur.thenBranch();
            if (thenArm instanceof RConditionalExpr nestedThen) {
                if (ladderBareAliasWildcardArm(nestedThen, compiler)) {
                    return true;
                }
            } else if (thenArgElementLeafIsWholeValue(thenArm)
                    && bareModelAliasWildcardArmItem(thenArm, compiler)) {
                return true;
            }
            RExpression els = cur.elseBranch().orElse(null);
            if (els instanceof RConditionalExpr nestedElse) {
                cur = nestedElse;
                continue;
            }
            return els != null
                    && thenArgElementLeafIsWholeValue(els)
                    && bareModelAliasWildcardArmItem(els, compiler);
        }
        return false;
    }

    /**
     * facet aliasSigWildcardDecl (seat 28, law D, E4 amendment - MEASURED by the suite's
     * no-carrier-cell control): the E4 LADDER decl takes the wildcard only for a BARE
     * MODEL element. A META-WRAPPER element keeps the INVARIANT decl even under a
     * wildcarded signature - golden drr 5.61.0 ExtractReferenceEntity declares
     * `final MapperC<FieldWithMetaString> thenArg;` against the alias's own
     * `MapperS<? extends FieldWithMetaString>` signature, while drr 7.x's bare-model
     * twin (EntityIdentifier) wildcards the decl. The SEAT-A arm is not narrowed: its
     * element-agreement gate against the #241 walk already carries the same boundary
     * (the body walk's meta arm resolves there).
     */
    private static boolean bareModelAliasWildcardArmItem(RExpression arm,
            ExpressionCompiler compiler) {
        JavaClass<?> sigItem = aliasSignatureWildcardItemType(arm, compiler);
        return sigItem != null && !(sigItem instanceof RJavaWithMetaValue);
    }

    /**
     * facet fnAnnaDsbTogetherRestructure (PR #399, R5): the #366 conditional-alias-body
     * element walk, RECURSIVE over nested-conditional arms + the terminal else — the
     * AnnaDsb-FRE {@code product} alias nests its model-typed nav arms one conditional
     * deeper ({@code if isSwaption then (if <absent> then <nav> else <nav>) else <nav>}),
     * which the flat #366 rung walk could not reach (the thenArg decl stayed the
     * non-compiling {@code MapperS<Object>}). Depth-first first MODEL-typed arm wins —
     * the same arm order the alias SIGNATURE walk joins, so the two stay agreed (the
     * #178 same-walk law). MODEL-typed arms only (the #169 law), as before.
     */
    private static RType condLadderFirstModelArmItem(RConditionalExpr cond,
            ExpressionCompiler compiler) {
        RConditionalExpr cur = cond;
        while (cur != null) {
            RType armItem;
            if (cur.thenBranch() instanceof RConditionalExpr nestedThen) {
                armItem = condLadderFirstModelArmItem(nestedThen, compiler);
            } else {
                RType t = recoverThenArgItemRType(cur.thenBranch(), compiler);
                armItem = (t instanceof RDataTypeRef || t instanceof RChoiceTypeRef) ? t : null;
            }
            if (armItem != null) {
                return armItem;
            }
            RExpression els = cur.elseBranch().orElse(null);
            if (els instanceof RConditionalExpr nested) {
                cur = nested;
                continue;
            }
            if (els != null) {
                RType t = recoverThenArgItemRType(els, compiler);
                if (t instanceof RDataTypeRef || t instanceof RChoiceTypeRef) {
                    return t;
                }
            }
            return null;
        }
        return null;
    }

    /**
     * Unwrap the {@code then}/{@code filter}/{@code extract}/list-op wrappers to the
     * element-producing leaf EXPRESSION — the expr analogue of the recovery walk in
     * {@link #recoverThenArgItemRType} (a then-body that filters the piped item
     * recurses to the then ARGUMENT, not the predicate). Used to reach the alias-call
     * base a {@code thenArg} hoists.
     */
    private static RExpression thenArgElementLeaf(RExpression e) {
        if (e instanceof RThenExpr t) {
            RExpression bodyExpr = t.body().map(RInlineFunction::body).orElse(null);
            if (bodyExpr instanceof RFilterExpr f && f.argument() instanceof RImplicitVariable) {
                return thenArgElementLeaf(t.argument());
            }
            return bodyExpr == null ? e : thenArgElementLeaf(bodyExpr);
        }
        if (e instanceof RExtractExpr x) {
            return thenArgElementLeaf(x.body().body());
        }
        if (e instanceof RFilterExpr f) {
            return thenArgElementLeaf(f.argument());
        }
        // facet objFallbackSortThenArg (PR #343): sort is element-preserving — the
        // alias/element leaf sits below it (the aliasDerivedThenArgItemType walk must
        // see through the sort to the alias-invocation leaf).
        if (e instanceof RSortExpr s) {
            return thenArgElementLeaf(s.argument());
        }
        if (e instanceof RListOpExpr lo) {
            return thenArgElementLeaf(lo.argument());
        }
        return e;
    }

    // =========================================================================
    // Deep feature call (double arrow →→)
    // =========================================================================

    /**
     * Compiles a deep-arrow feature call into a mapper chain with a
     * {@code DeepPathUtil} delegation.
     *
     * <p>Golden output pattern:
     * <pre>
     *   receiver.&lt;TargetType&gt;mapC("chooseFeatureName", lambdaVar -> deepPathUtil.chooseFeatureName(lambdaVar))
     * </pre>
     *
     * <p>The {@code chooseFeatureName} method name is derived from the feature:
     * {@code "choose" + capitalize(featureName)}. The deep path util field name
     * and lambda variable name are derived from the receiver type.
     *
     * <p>When the receiver type is not available (no resolved feature or no parent
     * type on the feature), a warning is logged and a placeholder is emitted.
     *
     * @param expr     the deep feature call node
     * @param ctx      the current expression compilation context
     * @param compiler the parent compiler for recursive compilation of the receiver
     * @return a {@link JavaExpression} rendering the deep feature call
     */
    public JavaStatementBuilder handle(RDeepFeatureCall expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        // PR-A §9.1 C3a.4.f: structured refs + operand-refs flow matching
        // the RFeatureCall path. DeepPathUtil class domain-import remains
        // on the regex path per D3 ι (deferred to C3a.4.l).
        // facet interior_position_coercion: deep-feature receivers compile
        // interior exactly like the single-arrow path above.
        JavaStatementBuilder receiverBuilder =
                compiler.compileInterior(expr.receiver(), ctx.expectedType(), ctx.scope());
        // Engine PR #9 (PR-B): meta-strip a meta-typed receiver before the deep
        // navigation (same rationale + byte-flat no-op as handle(RFeatureCall)).
        receiverBuilder = compiler.coerceNavigationReceiver(receiverBuilder, ctx.scope());
        // facet ruleMetaLiftResidueSeats (seat 6, the receiver-wrap rung): a bare
        // point-free FUNCTION invocation as the deep-path receiver compiles UNWRAPPED
        // per upstream evaluateCall (renderImplicitFunctionInvocation serves the
        // boolean/filter contexts), but a NAVIGATION receiver consumes Mapper-valued —
        // golden wraps `MapperS.of(rateOption.evaluate(input))` before the deep
        // `.map(…)` step (drr NameOfTheFloatingRateRule). Wrap exactly the bare
        // args-empty RFunction-reference shape; a bare RULE reference is excluded
        // (renderImplicitRuleInvocation always wraps). Green-safe by construction:
        // `.map` on a bare model value never compiled, so no green file carries the
        // unwrapped form at this seat.
        if (expr.receiver() instanceof RSymbolReference recRef
                && recRef.args().isEmpty()
                && recRef.symbol().filter(s -> s instanceof RFunction).isPresent()
                && receiverBuilder instanceof JavaExpression recExpr) {
            Set<JavaClass<?>> recRefs = new HashSet<>(recExpr.getRefs());
            recRefs.add(HandlerHelper.MAPPER_S);
            // The #176 typed-wrap pattern (the indep review's NIT-2): the wrap REPORTS
            // MapperS<bare> when the bare item type is known — render-inert here (the
            // deep step consumes only the string + refs) but type-faithful for any
            // future typed reader. Copilot #576 R1: when the wrap type cannot be built
            // (null compiled type OR no typeUtil) the stamp is NULL — never the bare
            // type, whose text/type mismatch would mislead a typed consumer.
            JavaType recWrapped = recExpr.getExpressionType() != null
                    && compiler.getTypeUtil() != null
                    ? compiler.getTypeUtil().wrap(
                            compiler.getTypeUtil().MAPPER_S, recExpr.getExpressionType())
                    : null;
            receiverBuilder = JavaExpression.from(
                    "MapperS.of(" + recExpr.renderToString() + ")",
                    recWrapped, recRefs,
                    recExpr.getStaticWildcardImports());
        }
        String receiver = HandlerHelper.render(receiverBuilder);
        String feature  = expr.featureName();
        String chooseMethod = toChooseMethodName(feature);

        // facet deep_path_util_resolution (PR #205): the deep feature itself is left
        // UNRESOLVED in the IR for the dominant carrier shape (the parser's
        // findAttributeDeep does not traverse a one-of/choice receiver — gm-aware probe:
        // deepResolved=false for the clean carriers), so the <Type> witness + the
        // map/mapC cardinality were lost. Recover the deep feature gm-aware (the
        // deep-arrow sibling of the single-arrow fallbackResolveFeature) so the witness
        // and cardinality resolve exactly as upstream's caseDeepFeatureCall /
        // CardinalityProvider would.
        RAttribute resolvedAttr = resolveDeepFeature(expr, compiler);
        String typeParam = resolveDeepTypeParam(resolvedAttr, compiler);
        String mapMethod = resolveDeepMapMethod(resolvedAttr);

        Set<JavaClass<?>> refs = new HashSet<>();
        // Instance .map()/.mapC() need no mapper-class import — see the
        // RFeatureCall path. The mapper class flows up from the receiver's
        // static-factory leaf.
        refs.addAll(receiverBuilder.getRefs());
        // Register the <Type> generic-witness import (same rationale as the
        // single-arrow path), now from the gm-aware-recovered deep feature.
        addWitnessTypeRef(resolvedAttr, compiler, refs);
        // Engine PR #9 (PR-B): surface a concrete RJavaWithMetaValue + its import when
        // the deep step navigates TO a meta attribute (see handle(RFeatureCall)).
        // Deep path: the wrapper-kind flag stays STEP-LOCAL by design — the deep-arrow
        // (→→) path is an M7b-4 stub (see resolveReceiverTypeName's NOTE) and the probed
        // population shows no deep-call meta-chain divergence; the chain-aware
        // chainRendersMapperC walk applies to the single-arrow path only.
        JavaType resultType = metaNavResultType(resolvedAttr, compiler, refs, "mapC".equals(mapMethod));

        String receiverTypeName = resolveDeepReceiverTypeName(expr, compiler);

        if (receiverTypeName != null) {
            String bareVar = Character.toLowerCase(receiverTypeName.charAt(0))
                    + receiverTypeName.substring(1);
            // facet deepPathParamEscape (PR #344): the deep-path step's lambda var is
            // scope-disambiguated exactly like every type-derived lambda var (upstream's
            // createUniqueIdentifier ALWAYS escapes) - golden `_instrument ->
            // instrumentDeepPathUtil.chooseInstrumentType(_instrument)` where the desired
            // name collides with the enclosing method's `instrument` param (a bare name
            // there is an illegal Java lambda shadow, so no GREEN file carries a colliding
            // bare form - the 38 bare `product` golden deep-path lambdas stay bare because
            // nothing in their scope takes the name). The DeepPathUtil FIELD keeps the
            // BARE type-derived name: it is the @Inject field reference
            // (FunctionDependencyCollector derives it from the SAME receiver type), never
            // a lambda param, and golden never escapes it.
            String lambdaVar = ctx.scope() != null
                    ? ctx.scope().registerDeferredLambdaParam(bareVar)
                    : bareVar;
            String deepPathUtilField = bareVar + "DeepPathUtil";
            return JavaExpression.from(
                    receiver + "." + typeParam + mapMethod + "(\"" + chooseMethod + "\", "
                            + lambdaVar + " -> " + deepPathUtilField + "." + chooseMethod
                            + "(" + lambdaVar + "))",
                    resultType,
                    refs,
                    receiverBuilder.getStaticWildcardImports());
        }

        // v3.2 seat 12 (D52, COUNTERS FIRST - the LOUD register's deep-path site): the receiver type the resolver
        // could not name used to fall to `<recv>.map("chooseX", _x -> /* TODO(M7b-4): wire DeepPathUtil */
        // chooseX(_x))` with NO @Inject field - a placeholder in emitted Java, non-compiling and SILENT (the seat-4
        // banked register question). REFUSED by name instead: the chaos M7b rows (s29 - a receiver that is itself a
        // deep call, the explicit parameter of a then-extract over a list of lists; the census's 24, re-measured at
        // this seat's D11 on both routes); the heal - the resolver's two missing arms - is seat 13's (D52 decision 3).
        // The collector's @Inject decision reads the SAME resolver (resolveDeepReceiverSymbolId) and declines there
        // without a counter: the render is the one seat that writes bytes, so the render is the one that refuses.
        throw SilentDegradation.refuse(SilentDegradation.Site.DEEP_PATH_UTIL_UNRESOLVED,
                "deep path '->> " + feature + "' whose receiver type the resolver cannot name (receiver "
                        + expr.receiver().getClass().getSimpleName() + ") - no <Type>DeepPathUtil to inject; the"
                        + " pre-seat render was a TODO placeholder",
                expr);
    }

    // =========================================================================
    // Lambda variable naming
    // =========================================================================

    /**
     * Resolves the lambda variable name for a navigation step based on the receiver expression.
     *
     * <p>The lambda parameter represents the element type of the mapper that {@code .map()}
     * is called on. The naming convention follows CDM golden-file patterns:
     *
     * <ul>
     *   <li><strong>Receiver is {@link RImplicitVariable}</strong> (the lambda's implicit
     *       {@code item}, literal or synthetic): inside a from-typed RULE, named from the
     *       resolved item data type ({@link #implicitItemDataType} — the navigated element type
     *       of a hoisted then/filter/extract chain) when a compiler is carried and it resolves
     *       (facet navItemLambdaTyping, PR #255: the implicit item may have been navigated PAST
     *       the rule's from-type, so the item type takes precedence), ELSE from the rule's
     *       from-type (the fallback, and the compiler-less unit-call default). Outside a
     *       from-typed rule (facet filter_predicate_item_typing) named from the owning
     *       filter/extract list-op argument's item data type ({@link #implicitItemDataType}).
     *       Both scope-disambiguated; falls through to the underscore fallback when neither
     *       resolves.</li>
     *   <li><strong>Receiver is {@link RSymbolReference}</strong> (function input, alias, etc.):
     *       the desired name is lowerCamelCase(receiver type name), routed through
     *       {@link JavaStatementScope#disambiguate(String)}. Scope-faithful (Phase X1 Gap #2):
     *       the leading underscore in e.g. {@code _businessEvent} is collision disambiguation,
     *       not a fixed convention — it appears only when the desired name clashes with a name
     *       already in the (seeded) body scope. A function input named like its type
     *       ({@code businessEvent : BusinessEvent}) collides → {@code _businessEvent}; a rule's
     *       implicit input is named {@code input}, so a lambda {@code settlementTerms} does not
     *       collide → {@code settlementTerms} (matching the legacy plugin golden). When no scope
     *       is supplied (defensive / isolated unit calls) the prior always-underscore behaviour
     *       is preserved.</li>
     *   <li><strong>Receiver is {@link RFeatureCall}</strong> (chained navigation):
     *       returns lowerCamelCase of the previous step's return type name (no underscore prefix).
     *       For example, after a step returning {@code Instruction}, the lambda var is
     *       {@code instruction}. Left as-is (not routed through {@code disambiguate}): a chained
     *       receiver's return type rarely collides with a body input, matching upstream in the
     *       common case, and preserving byte-validated chained output.</li>
     *   <li><strong>Fallback</strong>: returns {@code _} + feature name (preserves prior behavior
     *       when type information is unavailable).</li>
     * </ul>
     *
     * @param receiver     the receiver expression of the feature call
     * @param featureName  the feature being accessed (used as fallback)
     * @param scope        the current render scope (seeded with the enclosing function/rule's
     *                     input/output/shortcut names); may be {@code null}
     * @return the lambda variable name
     */
    String resolveLambdaVarName(RExpression receiver, String featureName, JavaStatementScope scope) {
        return resolveLambdaVarName(receiver, featureName, scope, (ExpressionCompiler) null);
    }

    /**
     * Compiler-carrying variant of
     * {@link #resolveLambdaVarName(RExpression, String, JavaStatementScope)} (facet
     * filter_predicate_item_typing). The {@code compiler} — {@code null} for the
     * compiler-less callers, whose every existing branch is byte-unchanged — feeds ONLY
     * the {@link RImplicitVariable} item-type case's gm-aware argument-type resolution;
     * the {@link RFeatureCall} chained branch deliberately stays on the static walk
     * (see the facet choice_nav_chain_typing note inside it).
     */
    private String resolveLambdaVarName(RExpression receiver, String featureName,
            JavaStatementScope scope, ExpressionCompiler compiler) {
        if (receiver instanceof RImplicitVariable) {
            // Engine PR #4 / facet C: a synthesized implicit-input chain head inside
            // a rule extract/filter lambda renders as the lambda's `item` (see
            // ReferenceHandler#buildImplicitInputReceiver). The lambda variable for
            // the first navigation step still derives from the item's type, which
            // equals the enclosing rule's from-type for the cases this fires on.
            // TWO feeders wire a synthetic-IV navigation receiver, each upholding
            // the type this branch names from via its own identity guard:
            //  (a) ReferenceHandler.buildImplicitInputReceiver (rule-side), gated by
            //      `findAttributeOnDataType(fromType, name) == attr` in
            //      synthesizeImplicitInputNavigation/Chain — the head attribute is
            //      provably a feature of the from-type, so item-type == from-type
            //      holds and the rule case below names it correctly;
            //  (b) ReferenceHandler.synthesizeImplicitItemNavigation (facet
            //      filter_predicate_item_typing), gated by the SAME identity check
            //      against the lambda's ITEM type (implicitItemDataType) — named by
            //      the item-type case below. Inside a from-typed RULE's lambda the
            //      rule case wins by order; for feeder (a) the two types coincide by
            //      its guard, and feeder (b) only fires where the pre-facet render
            //      was the non-compiling bare variable (waivered output only).
            // A future caller wiring a synthetic-IV receiver must preserve an
            // equivalent guard. Mirrors the RSymbolReference branch's scope-faithful
            // disambiguation. Falls through to the default when neither the
            // rule/from-type nor the inline-lambda item type is available.
            // facet choiceSwitchLambdaOptionGetter (seat 31, law 4b): this LIVE-bound rung
            // moved ABOVE the rule-from-type rung AND the condition-top-level rung below it -
            // a RULE-body case-bound hop names from
            // the CASE type exactly like the function-body seats (golden drr 7.x CBC
            // `_settlementPayout -> _settlementPayout.getUnderlier()` where the extract
            // item is the un-narrowed Payout); the binding-live gate keeps every unbound
            // rule seat on the from-type rung byte-identically, and the #396 law stands:
            // upstream never names a case-bound hop from the un-narrowed item.
            // The demoted condition-top rung IS reachable at a live-bound seat
            // (ControlFlowHandler.hoistDataRuleSwitchReturnLadderOrNull binds the MAPPER-typed
            // direct form this rung's gate now reads through activeSwitchSubjectBoundText's
            // unwrap-orElse-self, inside a type condition), so the demotion is deliberate: at a
            // case-BOUND hop the #396 law names from the CASE type, and the corpus carries no
            // such seat (law 4b measured ZERO movers - law4-ckpt-accounting.txt, both routes).
            // The BOUND set this gate reads widened to the four MapperS<Case> bindings: the new
            // CH compileChoiceOptionGetterBlockLambda plus the three pre-existing MapperS-direct
            // ladder bindings (CFH conditional-arm, CFH data-rule return, FER SET seat) - all
            // already MapperS-typed, so no type test would exclude any of them (#603 review,
            // SF-6 / L2-04).
            // facet caseNarrowedToStringArm (PR #396): a LIVE-bound case-narrowed
            // implicit names from the CASE type even when the enclosing lambda's item
            // type resolves — the render re-rooted the receiver to the cast local, so
            // the param derives from the RECEIVER's type (render truth; golden cdm6
            // ExtractNotionalAdjustmentByLeg `_returnLeg -> _returnLeg
            // .getNotionalAdjustments()` where the extract item is DirectionalLeg).
            // The binding-live gate keeps every unbound seat on the item-type
            // precedence byte-identically; upstream never names a case-bound hop from
            // the un-narrowed item (the #368 carriers' `_floatingLeg` law), so a green
            // item-derived param at a bound hop can only exist where the two names
            // coincide — a no-op.
            RDataType caseNarrowedBound = (compiler != null && scope != null
                    && activeSwitchSubjectBoundText(receiver, scope) != null)
                    ? caseNarrowedImplicitType(receiver, compiler) : null;
            if (caseNarrowedBound != null && caseNarrowedBound.name() != null) {
                String base = toLowerCamelCase(caseNarrowedBound.name());
                String castLocal = activeSwitchSubjectBoundText(receiver, scope);
                // v3.2 seat 7 (LAW 77): the pre-escape is ONE declaration the IR leaf emitter consults too.
                String desired = preEscapeAgainstCastLocal(base, castLocal);
                return scope.registerDeferredLambdaParam(desired);
            }
            RRule rule = HandlerHelper.findEnclosingRule(receiver);
            if (rule != null && rule.fromType().isPresent()) {
                String typeName = rule.fromType().get().typeName();
                if (typeName != null) {
                    // facet navItemLambdaTyping (PR #255): the implicit item may have
                    // been navigated PAST the rule's from-type — a drr report rule body
                    // is a hoisted then-chain (`input then filter … then extract … then
                    // filter [ … ]`), and a later stage's filter/extract lambda iterates
                    // the navigated ELEMENT type, not the rule's from-type. Golden names
                    // the first navigation step's lambda var from that actual item type
                    // (`interestRatePayout -> interestRatePayout.getRateSpecification()`),
                    // whereas this branch's from-type fallback emitted the report input
                    // name (`transactionReportInstruction -> …`). Prefer the resolved
                    // item data type when a compiler is carried (the D11 rule/function
                    // emission path) and it resolves; keep the from-type otherwise.
                    // GREEN-SAFE BY CONSTRUCTION: when the item IS still the from-type the
                    // resolution returns the SAME data type (identical lowerCamelCase
                    // name) or declines (from-type fallback unchanged) — and a green
                    // from-typed-rule file cannot carry item-type != from-type at this
                    // nav (golden would already name the var from the item type, so the
                    // pre-facet fork from-type name would disagree = not green). The
                    // compiler-less unit-only callers keep the pre-facet from-type naming
                    // (mirrors the #176/#225 compiler-gated contract).
                    // facet rerootItemNav (PR #282): use the INFERRED-type fallback
                    // (implicitItemDataTypeOrInferred) so a THEN-piped item type (which the
                    // structural walk leaves null — the rule-body then-chain stage shape) names
                    // the var from the item element type (`creditDefaultPayout`) not the rule
                    // from-type (`transactionReportInstruction`); same green-safety argument as
                    // #255 (an item != from-type nav whose from-type name was used was already a
                    // waivered mismatch). Monotone: the structural walk still wins first.
                    RDataType navItem = (compiler != null)
                            ? implicitItemDataTypeOrInferred(receiver, compiler) : null;
                    String desired = toLowerCamelCase(
                            (navItem != null && navItem.name() != null)
                                    ? navItem.name() : typeName);
                    // facet deepThenLevelElementPreserve (PR #362): the #358/#360 self-shadow
                    // pre-escape at the IMPLICIT-receiver from-type naming arm — a type-derived
                    // first-step lambda var that EQUALS an ENCLOSING extract's EXPLICIT closure
                    // param shadows a real Java lambda variable (a compile error, so no green
                    // file carries the unescaped form). The collision target is ROSETTA SOURCE
                    // TEXT (paramNames), not a fork-derived name — the #331 uncharacterized-
                    // discriminator class cannot fire here. Golden: `MapperS.of(input)
                    // .<ReportableInformation>map("getReportableInformation",
                    // _collateralReportInstruction -> …)` inside `extract
                    // collateralReportInstruction [ … ]` (drr DTCC_TradeParty1/2
                    // ReportingDestination ×8).
                    if (ReferenceHandler.enclosingClosureParamOwner(receiver, desired) != null) {
                        desired = "_" + desired;
                    }
                    return scope != null ? scope.registerDeferredLambdaParam(desired) : "_" + desired;
                }
            }
            // Coverage wave D (datarule): the CONDITION-TOP implicit (`item ->
            // location` at type-condition top level) names the step's lambda var
            // from the condition's declaring type — the SAME type the
            // condition-top walk arm resolves (lockstep), scope-disambiguated so
            // the seeded instance parameter collides and escapes (golden
            // `_legacyValuationTime -> _legacyValuationTime.getLocation()`).
            if (isConditionTopLevelImplicit(receiver)) {
                RCondition topCondition = HandlerHelper.findEnclosingTypeCondition(receiver);
                if (topCondition != null
                        && topCondition.parent() instanceof RDataType conditionType
                        && conditionType.name() != null) {
                    String desired = toLowerCamelCase(conditionType.name());
                    return scope != null ? scope.registerDeferredLambdaParam(desired) : "_" + desired;
                }
            }
            // facet filter_predicate_item_typing (mechanism 1): with no enclosing
            // rule from-type, the implicit item inside an inline filter/extract
            // lambda is typed by the owning list-op ARGUMENT's item data type, and
            // golden names the first navigation step's lambda var from it
            // (`quantities filter item -> unit = unit` over Quantity items ->
            // `quantity -> quantity.getUnit()`), scope-disambiguated exactly like
            // every other type-derived lambda var. The walk declines (null) outside
            // a filter/extract/then/max/min lambda (facet then_maxmin_item_typing
            // widened the owners) or when the argument's item type does not
            // resolve to a data type, preserving the underscore fallback below —
            // and no in-scope green golden carries an underscore-named
            // implicit-item step (corpus-verified, all 5 cells x all kinds), so a
            // successful resolution only ever touches waivered output.
            // facet filterPredicateOperandNavReRoot (PR #341): the #282 INFERRED-type
            // fallback, un-rule-scoped to this no-from-type arm — a THEN-piped filter's
            // item type (structural walk null) names the first-step lambda var from the
            // inferred element type (`_tradeIdentifier -> _tradeIdentifier
            // .getIdentifierType()`, drr Extract_UTIPropietary) instead of the
            // feature-name underscore fallback (`_identifierType`). Same green-safety
            // law as the arm's own note above (no green golden carries the
            // underscore-fallback step this replaces); the compiler-less unit callers
            // keep the structural walk (the #176/#225 compiler-gated contract).
            RDataType itemType = (compiler != null)
                    ? implicitItemDataTypeOrInferred(receiver, compiler)
                    : implicitItemDataType(receiver, compiler);
            if (itemType != null && itemType.name() != null) {
                String desired = toLowerCamelCase(itemType.name());
                return scope != null ? scope.registerDeferredLambdaParam(desired) : "_" + desired;
            }
            // facet caseNarrowedDisguisedNav (PR #368, F-B): an implicit receiver whose
            // nearest boundary is a TYPE-guard switch CASE names from the NARROWED case
            // type. The cast local (`final <Case> <caseVar> = (<Case>) subject;`)
            // carries the type-derived name at this seat, so the collision is
            // STRUCTURAL — the #358 pre-escape law: explicit `_` prefix
            // (golden MapCap/FloorRateSchedule `_floatingRateCalculation`, MapAsset
            // `_equity`/`_exchangeTradedFund`). Fires only where the rule / item-type
            // resolutions above declined (the `_ + featureName` fallback space — no
            // green golden carries that fallback at a case-narrowed implicit hop).
            // facet caseSwitchAliasSubjectStmtArms (PR #370): the pre-escape keys on the
            // cast local's ACTUAL name read from the LIVE #221 subject binding (render
            // truth) — a method whose ladders repeat a case type numbers the cast locals
            // method-wide (`genericProduct0`/`genericProduct1`, golden
            // MapTransferStateList), freeing the bare type-derived param
            // (`genericProduct -> …`); a singleton cast local carries EXACTLY the
            // type-derived name and keeps the #368 structural escape. Binding-less
            // callers keep the unconditional escape (the pre-#370 bytes).
            RDataType caseNarrowed = (compiler != null)
                    ? caseNarrowedImplicitType(receiver, compiler) : null;
            if (caseNarrowed != null && caseNarrowed.name() != null) {
                String base = toLowerCamelCase(caseNarrowed.name());
                String castLocal = activeSwitchSubjectText(receiver, scope);
                String desired = (castLocal != null && !castLocal.equals(base))
                        ? base
                        : "_" + base;
                return scope != null ? scope.registerDeferredLambdaParam(desired) : desired;
            }
        }

        if (receiver instanceof RSymbolReference symRef) {
            // Scope-faithful lambda naming. The desired name is the lowerCamelCase
            // of the receiver's declared type (or the symbol name when the type is
            // unavailable); disambiguate() re-adds a leading underscore only on a
            // collision with a name already in scope. See method javadoc.
            String typeName = resolveSymbolTypeName(symRef);
            // facet alias_receiver_typing: an ALIAS receiver has no declared type on
            // the symbol — name the lambda var from the alias's resolved VALUE type
            // (the SAME gm-aware resolution the witness path reads, so the witness
            // and the lambda var cannot disagree), scope-disambiguated like every
            // type-derived lambda var (`economicTerms`, or `_varianceReturnTerms`
            // when the type-derived name collides with the alias's own in-scope
            // name). Declines to the symbol-name fallback — the pre-facet
            // `_<aliasName>` — when the alias value is not a data type, or when
            // no compiler is carried (Copilot R1 contract consistency: the
            // compiler-less overload keeps its pre-facet naming rather than
            // half-resolving through the static fast-paths).
            if (typeName == null && compiler != null
                    && resolveAliasReceiver(symRef, symRef.symbol().orElse(null)) != null) {
                RDataType aliasType = resolveReceiverDataType(symRef, compiler);
                // facet aliasCondLadderChoiceJoin (v3.1 flip seat 33, law A.2): the alias
                // naming seat's LAST RESORT. Golden names this hop from the alias's own
                // CHOICE value type (`underlier`), the fork from the alias SYMBOL
                // (`_underliers`), because the walk's conditional arm joins its two branch
                // types by DECLARATION IDENTITY and every choice narrowing mints a FRESH
                // bridge (idCarryingChoiceBridge / RChoiceTypeRef.asRDataType), so a
                // multi-rung ladder over ONE choice joins to null at every rung. Scoped to
                // THIS consumer and to the walk's own DECLINE - measured at 4 rows / 1
                // `where=` over 11,957 alias consults, route-identical, so nothing that
                // resolves today can move. Carrier: drr 7.0-7.3 GetBasketConstituents
                // (`underliers -> Observable`, the `underliers` alias's four-rung
                // `Underlier` ladder).
                if (aliasType == null) {
                    aliasType = aliasCondLadderChoiceJoin(symRef, compiler);
                }
                if (aliasType != null && aliasType.name() != null) {
                    typeName = aliasType.name();
                }
            }
            // facet fnCallReceiverLambdaTyping (PR #391): a FUNCTION-CALL receiver
            // (`UnderlierForProduct(product) -> Product`) names the step's lambda
            // var from the callee OUTPUT's data type via the SAME
            // resolveReceiverDataType walk the witness reads (lockstep — the walk
            // already resolves the call; only this naming consumer fell to the
            // symbol-name fallback, `_underlierForProduct` where golden names
            // `underlier`). Green-safe: a green carrier is impossible — golden
            // always names from the type, so a fn-call receiver whose fn name
            // differs from its output type never byte-matched under the fallback,
            // and where the two names coincide the desired string (and its scope
            // disambiguation) is identical either way. Declines (walk null /
            // non-data output) keep the pre-facet fallback.
            if (typeName == null && compiler != null
                    && symRef.symbol().filter(s -> s instanceof RFunction).isPresent()) {
                RDataType fnOutType = resolveReceiverDataType(symRef, compiler);
                if (fnOutType != null && fnOutType.name() != null) {
                    typeName = fnOutType.name();
                }
            }
            // facet closureParamDirectNav (PR #342): an EXPLICIT closure-param receiver
            // names the step's lambda var from the param's ELEMENT type — the SAME
            // resolveReceiverDataType closure arm the witness/arity read (golden
            // `leg.<RateSpecification>map("getRateSpecification", interestRatePayout ->
            // …)`, drr InterestRateLeg1/2CrossCurrency), instead of the symbol-name
            // fallback whose disambiguation would land the shadow-escape `_leg`.
            // Compiler-gated + then-owner-declined inside the walk, exactly like the
            // alias arm above; declines keep the pre-facet fallback.
            boolean cpElementNamed = false;
            if (typeName == null && compiler != null
                    && ReferenceHandler.enclosingClosureParamOwner(symRef, symRef.name()) != null) {
                RDataType cpType = resolveReceiverDataType(symRef, compiler);
                if (cpType != null && cpType.name() != null) {
                    typeName = cpType.name();
                    cpElementNamed = true;
                }
            }
            // facet dispatchVariantParamResolution (PR #369): an UNRESOLVED receiver
            // naming a dispatch-BASE input derives the lambda var from the input's
            // declared type — the SAME walk resolution the witness reads, so the two
            // cannot disagree (golden `MapperS.of(calcPeriod).<Date>map(
            // "getAdjustedStartDate", calculationPeriodBase -> …)`, ProcessFloatingRate
            // Reset OIS/Modular). Record/basic-typed inputs decline through the walk's
            // null (the symbol-name fallback — the Date heads render via the record
            // method-ref arm with no lambda var at all).
            if (typeName == null && compiler != null && symRef.symbol().isEmpty()
                    && HandlerHelper.dispatchBaseInput(symRef, symRef.name()) != null) {
                RDataType baseType = resolveReceiverDataType(symRef, compiler);
                if (baseType != null && baseType.name() != null) {
                    typeName = baseType.name();
                }
            }
            // facet thenParamBareNavSynthesis (PR #375, B2): an UNRESOLVED receiver that
            // synthesizes to an implicit-item nav (the named-then-step bare-attribute
            // shape — ReferenceHandler.synthesizeImplicitItemBareNav) names the step's
            // lambda var from the SYNTHESIZED head's element type — the SAME walk the
            // witness reads, so the two cannot disagree (golden MessageID
            // `.<MessageInformation>map("getMessageInformation", workflowStep -> …)`:
            // the hop over the synthesized `originatingWorkflowStep` head names from
            // its WorkflowStep element, not the symbol-name fallback). Declines (null
            // walk / non-data element) keep the pre-facet symbol-name naming.
            if (typeName == null && compiler != null && symRef.symbol().isEmpty()) {
                RFeatureCall synthHead =
                        ReferenceHandler.synthesizeImplicitItemBareNav(symRef, compiler);
                if (synthHead != null) {
                    RDataType synthType = resolveReceiverDataType(synthHead, compiler);
                    if (synthType != null && synthType.name() != null) {
                        typeName = synthType.name();
                    }
                }
            }
            String desired = toLowerCamelCase(typeName != null ? typeName : symRef.name());
            // facet thenVarRewrapElision (PR #358): a closure-param receiver whose
            // element-derived lambda var EQUALS the param's own name would shadow the
            // receiver inside the step lambda — upstream's createUniqueIdentifier sees the
            // param registered in scope and underscore-escapes (golden
            // `businessEvent.<Instruction>mapC("getInstruction", _businessEvent -> …)`,
            // ClearingSwapUSIs/UTIs). The fork's explicit lambda params are render text
            // (never scope-registered), so the deferred resolution cannot see the
            // collision — pre-escape exactly the self-shadow case. Scoped to the
            // closure-param element naming (the alias arm's collisions resolve through
            // the registered alias local instead).
            // facet mapItemClosureParamReceiver (PR #360): the enclosing-param shadow at a
            // RESOLVED-attribute receiver — an element-derived lambda var that EQUALS an
            // ENCLOSING extract's explicit closure param would shadow a real Java lambda
            // variable (a compile error, so no green file carries the unescaped form —
            // the #358 self-shadow law at the input-receiver seat; golden
            // `MapperC.<PriceQuantity>of(change).mapC("getPrice", _priceQuantity -> …)`
            // inside `extract priceQuantity [ … ]`, UpdateAmountForEachMatchingQuantity
            // cdm5/cdm6). SCOPED to receivers whose symbol RESOLVES to an RAttribute (a
            // real input/output): a closure-param/unresolved receiver keeps the pre-#360
            // name — on those the fork's enclosing-param names themselves diverge from
            // golden's, and the escape moved heavily-divergent files' tokens AWAY
            // (`_deliveryBlock` 32-vs-golden-0, the cp5a catch — the #331
            // thenArgParentEscape uncharacterized-discriminator class).
            boolean shadowsEnclosingParam =
                    symRef.symbol().filter(RAttribute.class::isInstance).isPresent()
                    && ReferenceHandler.enclosingClosureParamOwner(symRef, desired) != null;
            if ((cpElementNamed && desired.equals(symRef.name())) || shadowsEnclosingParam) {
                desired = "_" + desired;
            }
            return scope != null ? scope.registerDeferredLambdaParam(desired) : "_" + desired;
        }

        if (receiver instanceof RFeatureCall fc) {
            // For chained navigation, use the return type of the previous step (no underscore).
            // When that step's resolvedFeature was not populated during type-directed resolution
            // (a known IR gap), recover the attribute via the best-effort chain walk —
            // the STATIC fallbackResolveFeature — so the lambda var derives from the receiver's
            // resolved TYPE rather than collapsing to the raw feature name. The two coincide when
            // the attribute name equals its type name (the common CDM case, e.g.
            // `rateSpecification : RateSpecification`); they diverge for e.g.
            // `fixedRate : FixedRateSpecification`, where golden names the next step's receiver var
            // `fixedRateSpecification`, not `fixedRate`. NOTE (facet choice_nav_chain_typing):
            // this is the COMPILER-LESS overload, deliberately left on the static walk while the
            // witness (resolveTypeParam) + map-method (resolveMapMethod) paths went gm-aware —
            // see the gm-aware banner below; on a choice-option chain this branch declines to the
            // feature-name fallback (a missed recovery, never a wrong byte — the probed
            // population's lambda vars render correctly via the compiler-carrying overload).
            RAttribute prevFeature = fc.resolvedFeature().orElseGet(() -> fallbackResolveFeature(fc));
            // facet navWalkChoiceDisguise (PR #344) arm 5: when the STATIC walk cannot
            // resolve the receiver hop — a choice-option step (`… -> Product ->
            // TransferableProduct`), a disguised-chain receiver, or a deep-path receiver —
            // recover gm-aware through the SAME fallbackResolveFeature the witness path
            // reads, so the chained lambda var derives from the receiver's resolved type
            // (golden `transferableProduct ->`) instead of the raw feature-name fallback
            // (the capitalized `TransferableProduct ->` — the P344 probe's
            // `lambdaVar=TransferableProduct`). Green-safe: where the feature name
            // lowerCamelCases to the SAME string as the type name (the dominant green
            // coincidence) the rendered bytes are identical; where they differ the
            // pre-facet render disagreed with golden at this step, so the file was
            // already divergent. Compiler-less callers keep the static walk.
            if (prevFeature == null && compiler != null) {
                prevFeature = fallbackResolveFeature(fc, compiler);
            }
            String typeName = (prevFeature != null && prevFeature.typeCall() != null)
                    ? prevFeature.typeCall().typeName()
                    : null;
            if (typeName != null) {
                // facet alias_receiver_typing (mechanism 4): a CHAINED step's
                // type-derived lambda var is scope-disambiguated like every other
                // type-derived lambda var — upstream's
                // `createUniqueIdentifier(type.toFirstLower)` ALWAYS escapes against
                // the enclosing scope, so a var colliding with an in-scope name (the
                // function input `product`) renders golden's `_product`, not the
                // non-compiling shadow `product`. Collision-free names — every
                // previously-green chained var, since a shadowing name does not
                // compile — are returned unchanged.
                String desired = toLowerCamelCase(typeName);
                return scope != null ? scope.registerDeferredLambdaParam(desired) : desired;
            }
            // Fallback: use the feature name of the previous step (no underscore)
            return fc.featureName();
        }

        // facet deepPathLambdaNaming (PR #343): a step navigating FROM a deep-feature
        // call (`Product ->> economicTerms -> payout`) names its lambda var from the
        // deep feature's RETURN type — the type the `->>` navigates TO (golden
        // `_economicTerms`, cdm6 Qualify_{BuySellBack,FullReturn,RepurchaseAgreement}) —
        // instead of the raw feature-name fallback (`_payout`). resolveLambdaVarName has
        // arms for the chained RFeatureCall / list-op / disguised-chain receivers but NONE
        // for RDeepFeatureCall (it extends RExpression, not RFeatureCall), so the step
        // fell through to the `_` + featureName default. Reads the SAME resolveDeepFeature
        // the handle(RDeepFeatureCall) witness/map-method paths render from, so the naming
        // and the witness cannot disagree. Compiler-gated (resolveDeepFeature needs the
        // model); scope-disambiguated like every type-derived lambda var (upstream's
        // createUniqueIdentifier ALWAYS escapes — a collision with the in-scope input
        // `economicTerms` renders golden's `_economicTerms`; a non-colliding name stays
        // bare). Declines to the default fallback when the deep feature / its type does not
        // resolve. Green-safe: no golden carries the `_feature` fallback after a deep-path
        // step (corpus-verified), and the compiler-less callers keep the fallback.
        if (receiver instanceof RDeepFeatureCall deepRecv && compiler != null) {
            RAttribute deepAttr = resolveDeepFeature(deepRecv, compiler);
            if (deepAttr != null && deepAttr.typeCall() != null
                    && deepAttr.typeCall().typeName() != null) {
                String desired = toLowerCamelCase(deepAttr.typeCall().typeName());
                return scope != null ? scope.registerDeferredLambdaParam(desired) : "_" + desired;
            }
        }

        // A step navigating FROM an only-element (`.get()`) derives its lambda var from the
        // element data type the `.get()` produces (the owner type) — e.g.
        // `.<OptionPayout>mapC(...).get()` -> `optionPayout`. Mirrors the chained-RFeatureCall
        // branch's return-type-based naming (no underscore). The same ONLY_ELEMENT receiver is
        // re-wrapped + witness-recovered in handle(RFeatureCall) / resolveReceiverDataType.
        if (receiver instanceof RListOpExpr listOp && listOp.op() == ListOp.ONLY_ELEMENT) {
            RDataType dt = resolveReceiverDataType(receiver);
            if (dt != null && dt.name() != null) {
                return toLowerCamelCase(dt.name());
            }
        }

        // facet first_receiver_lambda_naming: a step navigating FROM a `.first()` /
        // `.last()` collapse derives its lambda var from the receiver's ELEMENT
        // data type — golden `.first().<CommonTransactionInformation>map(
        // "getHkmaTransactionInformation", transactionInformation -> …)`, where the
        // pre-facet fallback rendered the navigated-feature underscore form
        // (`_hkmaTransactionInformation`). The element type reads the SAME
        // first/last-transparent receiver walk the witness path reads (gm-aware,
        // facet then_maxmin_item_typing arm S3), so the witness and the lambda var
        // cannot disagree; scope-disambiguated like every type-derived lambda var
        // (upstream createUniqueIdentifier ALWAYS escapes). Compiler-less callers
        // keep the pre-facet fallback (the static walk is not first/last-
        // transparent — the alias-branch convention above). Carriers: drr
        // Extract_HKMATradeIdentifier, Extract_UnderlyingAssetTradingPlatformIdentifier.
        if (receiver instanceof RListOpExpr listOp
                && (listOp.op() == ListOp.FIRST || listOp.op() == ListOp.LAST)
                && compiler != null) {
            RDataType dt = resolveReceiverDataType(receiver, compiler);
            if (dt != null && dt.name() != null) {
                String desired = toLowerCamelCase(dt.name());
                return scope != null ? scope.registerDeferredLambdaParam(desired) : desired;
            }
        }

        // REnumValueRef used as a disguised feature call ({@code paramName -> featureName})
        // when the name did not resolve to a real enum at link time.
        if (receiver instanceof REnumValueRef evr && evr.enumeration().isEmpty()) {
            // Engine PR #12 (facet F5): a disguised input-navigation chain
            // (`a -> b`, D39 Category 10) binds a resolvedAttributeChain whose
            // TERMINAL feature carries the chain's RESULT type. The rendering path
            // (ReferenceHandler.synthesizeImplicitInputChain) drives the type
            // witness from chain.feature(); the lambda var of the step navigating
            // FROM this disguised chain must derive from the SAME type, not the raw
            // evr.valueName(). This is the dominant F5 trigger inside synthetic-item
            // lambdas (e.g. the ASIC UTI rule's `… else collateralDetails ->
            // uniqueTradeIdentifier -> assignedIdentifier -> identifier`): the chain
            // `collateralDetails -> uniqueTradeIdentifier` parses as an REnumValueRef
            // and its result feeds the next nav step, whose lambda var was emitted as
            // the feature name `uniqueTradeIdentifier` instead of the golden receiver
            // type `tradeIdentifier`. AttributeChain.feature() is always non-null and
            // its typeCall().typeName() needs no workspace (a stored string).
            if (evr.resolvedAttributeChain().isPresent()) {
                // feature() is non-null by AttributeChain's ctor contract (both
                // ctors requireNonNull it); only the typeCall is defensively guarded.
                RAttribute terminal = evr.resolvedAttributeChain().get().feature();
                if (terminal.typeCall() != null && terminal.typeCall().typeName() != null) {
                    // facet then_maxmin_item_typing (arm S4): scope-disambiguated like
                    // every type-derived lambda var — upstream's
                    // createUniqueIdentifier ALWAYS escapes against the enclosing
                    // scope (golden `_product` where the var collides with the
                    // in-scope input `product`); a colliding name is a non-compiling
                    // shadow, so no green disguised-receiver var changes.
                    String desired = toLowerCamelCase(terminal.typeCall().typeName());
                    // facet disguisedChainEnclosingParamEscape (PR #380): a hop
                    // navigating FROM a disguised chain whose type-derived lambda var
                    // EQUALS an ENCLOSING extract's EXPLICIT closure param shadows a
                    // real Java lambda variable — a compile error, so no green file
                    // carries the unescaped form (the #360/#362 pre-escape law at the
                    // disguised-chain-receiver seat; the #358 law: explicit closure
                    // params are render text, never scope-registered, so the deferred
                    // resolution cannot see this collision). The collision target is
                    // ROSETTA SOURCE TEXT (paramNames) and the desired name is the
                    // chain terminal's declared type — golden's own naming law for
                    // this hop — so the #331/#360-cp5a uncharacterized-discriminator
                    // class cannot fire. Golden: `reportableEvent.<WorkflowStep>map(
                    // "getOriginatingWorkflowStep", …).<WorkflowState>map(
                    // "getWorkflowState", _workflowStep -> …)` inside `extract
                    // workflowStep [ … ]` (drr iosco cde v3 EventTypeRule — an
                    // OUTER-param-rooted disguised chain, so the #358/#360
                    // symbol-receiver escapes never see this hop).
                    if (ReferenceHandler.enclosingClosureParamOwner(evr, desired) != null) {
                        desired = "_" + desired;
                    }
                    return scope != null ? scope.registerDeferredLambdaParam(desired) : desired;
                }
            }
            // Try to resolve the feature's type by walking up to the enclosing function.
            // facet alias_receiver_typing: the compiler-carrying variant adds the
            // disguised-ALIAS arm, so a step chained after a disguised alias hop
            // (`optionPayout -> underlier -> security`: this step's receiver is the
            // REnumValueRef `optionPayout -> underlier`) names from the disguised
            // feature's TYPE (golden's `product`) instead of falling through to the
            // raw value name (`underlier`). Compiler-less callers keep the
            // input/output-only resolution (the alias arm gates on an actual
            // compiler) — byte-unchanged.
            RAttribute resolvedFeature = resolveDisguisedFeature(evr, compiler, null);
            if (resolvedFeature != null && resolvedFeature.typeCall() != null) {
                String typeName = resolvedFeature.typeCall().typeName();
                if (typeName != null) {
                    // facet then_maxmin_item_typing (arm S4): same scope escape as the
                    // bound-chain branch above — see its rationale.
                    String desired = toLowerCamelCase(typeName);
                    return scope != null ? scope.registerDeferredLambdaParam(desired) : desired;
                }
            }
            // facet ctorFieldBareInvokableThenArg (PR #385): a step chained after a
            // disguised CLOSURE-PARAM hop names from the #360 leaf's declared TYPE —
            // the SAME walk the hop's witness reads (render-truth lockstep; golden
            // `.<Date>map("getStartDate", dateRange -> …)` where the raw valueName
            // fallback rendered `calculationPeriod` — GetReportableQuantityPeriod-
            // Leg1/2's filter-predicate + ctor navs). Declines keep the fallback.
            if (compiler != null) {
                RAttribute cpLeaf = closureParamDisguisedLeaf(evr, compiler);
                if (cpLeaf != null && cpLeaf.typeCall() != null
                        && cpLeaf.typeCall().typeName() != null) {
                    String desired = toLowerCamelCase(cpLeaf.typeCall().typeName());
                    return scope != null ? scope.registerDeferredLambdaParam(desired) : desired;
                }
            }
            // v3.1 flip seat 13 (facet disguisedOptionHopLambdaVar): a hop chained after
            // a disguised pair whose 2nd name is a CHOICE OPTION (`payout ->
            // CommodityPayout -> delivery` with the root ELIDED — the option hop is the
            // EVR's valueName, not an RFeatureCall) reached this fallback through all
            // three rungs above (the parser's Cat-16 binder does not bind an option
            // chain; the disguised-feature walk resolves function input/output/alias
            // heads only; the closure-param leaf declines) and rendered the RAW
            // PascalCase option name (`CommodityPayout -> CommodityPayout.getDelivery()`)
            // — 30 drr 7.0.0 files / 92 sites, 0 golden files in all 25 cells (the
            // seat-13 two-sided instrument). Upstream (ExpressionGenerator.xtend:359-368)
            // names EVERY nav lambda param toFirstLower of the RECEIVER's own type — a
            // choice receiver narrowed first — never a feature/value name. Consult the
            // SAME gm-aware receiver walk the hop's <Witness> and map-arity consumers
            // read (resolveReceiverDataType's REnumValueRef arm — the seat-1 authority
            // claim / the head+leaf structural walk), which already returns the option's
            // declared type at these hops; mirror the #380 enclosing-closure-param
            // escape and the deferred registration exactly like the two rungs above.
            // Green-safe by construction: this rung fires ONLY where the arm reached the
            // raw fallback, whose verbatim valueName() at a currently-green site already
            // EQUALS toFirstLower(receiverType) (the two-sided instrument: a PascalCase
            // param exists in no golden), so the computed string is unchanged there;
            // the proxy `toLowerCamelCase(evr.valueName())` is deliberately NOT used (a
            // plain-attribute pair whose name differs from its type — `fixedRate :
            // FixedRateSpecification` — would render the wrong name; rung 1 owns that
            // shape). Declines keep the raw fallback.
            if (compiler != null) {
                RDataType optionHopType = resolveReceiverDataType(evr, compiler);
                if (optionHopType != null && optionHopType.name() != null) {
                    String desired = toLowerCamelCase(optionHopType.name());
                    if (ReferenceHandler.enclosingClosureParamOwner(evr, desired) != null) {
                        desired = "_" + desired;
                    }
                    return scope != null ? scope.registerDeferredLambdaParam(desired) : desired;
                }
            }
            // Fallback: use the value name of the disguised feature call (no underscore)
            if (evr.valueName() != null) {
                return evr.valueName();
            }
        }

        // facet lambdaNaming M5 (PR #329): a step navigating FROM a FILTER derives its
        // lambda var from the filter's ELEMENT data type — a filter is element-PRESERVING,
        // so golden names the post-filter nav param from the filtered element exactly like
        // the first/last arm above (`.filterItemNullSafe(…).<AdjustableOrRelativeDate>map(
        // "getTerminationDate", varianceLeg -> …)`, MapVarianceSwapEconomicTerms), where
        // the pre-facet fall-through rendered the navigated-feature underscore form
        // (`_terminationDate`). Same gm-aware receiver walk + compiler-less decline as the
        // FIRST/LAST arm; a failed element resolution keeps the underscore fallback.
        if (receiver instanceof RFilterExpr && compiler != null) {
            RDataType dt = resolveReceiverDataType(receiver, compiler);
            if (dt != null && dt.name() != null) {
                String desired = toLowerCamelCase(dt.name());
                return scope != null ? scope.registerDeferredLambdaParam(desired) : desired;
            }
        }

        // facet navLambdaTypeNaming (PR #333): a first/last/only-element collapse over a
        // DISGUISED 2-name chain (`payout -> commodityPayout first -> underlier`, a rule-body
        // REnumValueRef whose resolvedAttributeChain the parser bound — the #291 leaf) names
        // the next step's lambda var from the chain's TERMINAL feature type, exactly like the
        // direct-receiver S4 arm above (`commodityPayout`, fca BaseProduct/SubProduct/
        // FurtherSubProduct — golden's element naming; the pre-#333 fall-through rendered
        // `_underlier`). The gm-aware walk arms above stay first — this fires only when they
        // all declined (a rule body has no enclosing function for resolveDisguisedFeature's
        // input/output arm, and the walk's REnumValueRef arm does not read the chain).
        // Scope-disambiguated like every type-derived lambda var (upstream always escapes).
        if (receiver instanceof RListOpExpr listOp
                && (listOp.op() == ListOp.FIRST || listOp.op() == ListOp.LAST
                    || listOp.op() == ListOp.ONLY_ELEMENT)
                && listOp.argument() instanceof REnumValueRef chainEvr
                && chainEvr.enumeration().isEmpty()
                && chainEvr.resolvedAttributeChain().isPresent()) {
            RAttribute terminal = chainEvr.resolvedAttributeChain().get().feature();
            if (terminal.typeCall() != null && terminal.typeCall().typeName() != null) {
                String desired = toLowerCamelCase(terminal.typeCall().typeName());
                return scope != null ? scope.registerDeferredLambdaParam(desired) : desired;
            }
        }

        // Default fallback: underscore + feature name
        return "_" + featureName;
    }

    /**
     * Lambda-var overload aware of the full {@link RFeatureCall} (facet F6 residual).
     *
     * <p>For a navigation receiver whose receiver symbol is an {@link RRule} (a bare
     * rule reference {@code SomeRule -> feature}; see
     * {@code ReferenceHandler#synthesizeRuleReceiverNavigation}) OR an {@link RFunction}
     * (an explicit function call {@code SomeFunc(arg) -> feature}), the lambda var is the
     * receiver's OUTPUT type, recovered from the navigated feature's OWNER type. The
     * receiver symbol itself carries no usable type for the lambda var (a rule's output
     * type is inferred at codegen, not stored on the node; a function symbol is the
     * callable, not its output type), and the owner of a directly-declared navigated
     * feature IS that output type. This is NOT the rule/function name: corpus rules and
     * functions exist whose name differs from the output type (e.g.
     * {@code UpiPreEnrichmentData} output {@code AnnaDsbUpiRequestAndType}; {@code GetFreq}
     * output {@code Frequency}). An INHERITED navigated feature's owner is its declaring
     * SUPERTYPE, not the receiver's concrete output subtype — for that case (PR #136)
     * the name derives from the resolved output type instead, gated on a strict-subtype
     * check (see {@link #resolveReceiverOutputType} / {@link #isStrictSubtypeOf}). All
     * other receivers delegate to
     * {@link #resolveLambdaVarName(RExpression, String, JavaStatementScope)}.
     */
    private String resolveLambdaVarName(RFeatureCall expr, JavaStatementScope scope,
                                        ExpressionCompiler compiler) {
        if (expr.receiver() instanceof RSymbolReference symRef
                && symRef.symbol().isPresent()
                && (symRef.symbol().get() instanceof RRule
                    || symRef.symbol().get() instanceof RFunction)) {
            RDataType ownerType = expr.resolvedFeature()
                    .map(RAttribute::parent)
                    .filter(RDataType.class::isInstance)
                    .map(RDataType.class::cast)
                    .orElse(null);
            if (ownerType != null) {
                String typeName = ownerType.name();
                // PR #136 (facet lambda_var_inherited_feature): when the navigated
                // feature is INHERITED, its owner is the declaring SUPERTYPE — but
                // golden names the lambda var from the receiver's CONCRETE output
                // type (e.g. rule `SpreadLeg1` outputs `PriceSchedule`; `value` is
                // declared on `MeasureBase`; golden lambda var is `priceSchedule`,
                // not `measureBase`). Substitute the receiver's output type ONLY
                // when it is a STRICT subtype of the owner: the directly-declared
                // case (owner == output type, every previously-flipped file) never
                // fires the gate and stays byte-identical, and a failed resolution
                // (null GeneratorModel in stateless unit calls, missing inference,
                // non-data-type output) falls back to the owner-derived name.
                RDataType outputType = resolveReceiverOutputType(symRef.symbol().get(), compiler);
                if (outputType != null && isStrictSubtypeOf(outputType, ownerType)) {
                    typeName = outputType.name();
                }
                String desired = toLowerCamelCase(typeName);
                return scope != null ? scope.registerDeferredLambdaParam(desired) : "_" + desired;
            }
        }
        RExpression recv = expr.receiver();
        // Chained navigation (facet choice_option_nav_witness): the lambda var is lowerCamelCase
        // of the previous step's RETURN type. Route the previous step through the gm-aware
        // fallback so a CHOICE-OPTION previous step (`rateSpecification -> FixedRateSpecification`,
        // resolvedFeature unset, AST type an RChoice) recovers its option type instead of
        // collapsing to the raw capitalised feature name (`FixedRateSpecification` ->
        // `fixedRateSpecification`). Strict superset of the static chained-RFeatureCall branch:
        // identical result whenever the AST path already resolved the previous feature.
        if (recv instanceof RFeatureCall prevFc) {
            RAttribute prevFeature = prevFc.resolvedFeature()
                    .orElseGet(() -> fallbackResolveFeature(prevFc, compiler));
            if (prevFeature != null && prevFeature.typeCall() != null
                    && prevFeature.typeCall().typeName() != null) {
                // facet alias_receiver_typing (mechanism 4): scope-disambiguated like
                // every type-derived lambda var — see the 4-arg overload's chained
                // branch for the rationale (upstream always escapes; a colliding
                // name is a non-compiling shadow, so no green chained var changes).
                String desired = toLowerCamelCase(prevFeature.typeCall().typeName());
                return scope != null ? scope.registerDeferredLambdaParam(desired) : desired;
            }
        }
        // Only-element (`.get()`) collapse (PR #152 nav_after_get_rewrap): the lambda var is the
        // owner (element) data type. Route through the gm-aware receiver-type resolution so the
        // owner recovers across a choice option in the collapsed chain (e.g. the `.get()` argument
        // navigates `... -> OptionPayout`, a choice option of `Payout`).
        // facet onlyElemLambdaVarScopeEscape (PR #376, E): register the var DEFERRED like the
        // chained-nav arm above (a fresh per-call lambda child — no cross-hop grouping) instead
        // of returning the literal, so the seeded-scope collision escape emerges (`MapperS.of(
        // MapperC.of(tradeLot).get()).<PriceQuantity>mapC(…, _tradeLot -> …)` inside a method
        // whose param is `tradeLot` — the ETDNotionalOption/Future class; the literal form was
        // an illegal Java shadow, so no green file carries a colliding unescaped var, and a
        // non-colliding seat resolves to the identical bare name).
        if (recv instanceof RListOpExpr lo && lo.op() == ListOp.ONLY_ELEMENT) {
            RDataType dt = resolveReceiverDataType(recv, compiler);
            if (dt != null && dt.name() != null) {
                String desired = toLowerCamelCase(dt.name());
                return scope != null ? scope.registerDeferredLambdaParam(desired) : desired;
            }
        }
        // The compiler flows through so the RImplicitVariable item-type case (facet
        // filter_predicate_item_typing) resolves a choice/unlinked list-op argument
        // type gm-aware; every other delegated branch ignores it (byte-unchanged).
        return resolveLambdaVarName(expr.receiver(), expr.featureName(), scope, compiler);
    }

    /**
     * Resolve a nav-receiver callable's concrete OUTPUT data type (PR #136).
     *
     * <p>A FUNCTION declares its output type directly — read the declared
     * {@code output()} attribute's {@code typeCall().referencedType()} (the same
     * resolved-reference idiom as {@link #resolveDisguisedFeature}). A RULE's
     * output type is inferred-only (not stored on the node) — infer it from the
     * rule's expression via the workspace type-inference engine, the exact
     * mechanism {@code RuleGenerator}'s output back-fill uses; an
     * {@link RDataTypeRef} result carries the canonical AST node.
     *
     * @return the canonical {@link RDataType} AST node, or {@code null} when the
     *         output type cannot be resolved (stateless unit compiler with no
     *         {@link GeneratorModel}, missing inference, or a non-data-type output)
     */
    private static RDataType resolveReceiverOutputType(RNode symbol, ExpressionCompiler compiler) {
        if (symbol instanceof RFunction fn) {
            RAttribute out = fn.output().orElse(null);
            if (out == null || out.typeCall() == null) {
                return null;
            }
            return out.typeCall().referencedType()
                    .filter(RDataType.class::isInstance)
                    .map(RDataType.class::cast)
                    .orElse(null);
        }
        if (symbol instanceof RRule rule) {
            // seat 21: the ONE rule-output read (HandlerHelper.ruleInferredOutputRType); this
            // consumer keeps its own data-type post-filter.
            RType inferred = HandlerHelper.ruleInferredOutputRType(rule, compiler.getGeneratorModel());
            if (inferred instanceof RDataTypeRef ref) {
                return ref.astNode();
            }
        }
        return null;
    }

    /**
     * Walk {@code candidate}'s super-type chain and report whether {@code ancestor}
     * appears STRICTLY above it (PR #136). Identity comparison — both sides are
     * canonical workspace AST nodes ({@code RAttribute.parent()} is the declaring
     * type's AST node; {@link RDataType#superType()} resolves through the
     * workspace). The identity-set visited guard bounds the walk on a (malformed)
     * cyclic hierarchy. Starting at {@code candidate.superType()} makes the check
     * strict on any acyclic hierarchy: a directly-declared feature (owner ==
     * candidate) never matches. (A malformed CYCLIC hierarchy could revisit
     * owner == candidate before the guard trips, but the substitution is then
     * the identity — byte-identical output — and cyclic type extension is
     * rejected upstream by {@code TypeValidator#checkCyclicExtension} anyway.)
     */
    private static boolean isStrictSubtypeOf(RDataType candidate, RDataType ancestor) {
        Set<RDataType> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        RDataType step = candidate;
        while (visited.add(step)) {
            Optional<RDataType> superType = step.superType();
            if (superType.isEmpty()) {
                return false;
            }
            step = superType.get();
            if (step == ancestor) {
                return true;
            }
        }
        return false; // cycle guard tripped — malformed hierarchy, fall back
    }

    /**
     * Resolve a disguised-feature-call {@link REnumValueRef} (where the enum name is
     * actually a function input/output name and the value name is a feature on that
     * input's type) to the feature's {@link RAttribute}.
     *
     * <p>Returns {@code null} when the receiver type cannot be determined or the
     * named feature does not exist on that type.
     */
    private static RAttribute resolveDisguisedFeature(REnumValueRef evr) {
        RAttribute receiverAttr = resolveDisguisedRootAttribute(evr);
        if (receiverAttr == null || receiverAttr.typeCall() == null) return null;

        Optional<RNode> referenced = receiverAttr.typeCall().referencedType();
        if (referenced.isEmpty()) return null;
        if (!(referenced.get() instanceof RDataType dt)) return null;

        return HandlerHelper.findAttributeOnDataType(dt, evr.valueName());
    }

    /**
     * The disguised navigation's ROOT attribute — the enclosing function's
     * input (or output) whose name a disguised {@code REnumValueRef}'s
     * {@code enumName} carries (`interestRatePayouts -> priceQuantity` parses
     * as {@code REnumValueRef(enumName=interestRatePayouts, valueName=priceQuantity)});
     * {@code null} when no input/output matches (e.g. a disguised ALIAS hop).
     * Extracted from {@link #resolveDisguisedFeature(REnumValueRef)} so the
     * chain-wrapper-kind walk ({@code chainRendersMapperC}) can read the
     * root's cardinality, not only the leaf's (facet meta_coercion_numbering
     * guard-kind sub-arm).
     */
    private static RAttribute resolveDisguisedRootAttribute(REnumValueRef evr) {
        if (evr.enumName() == null || evr.valueName() == null) return null;
        RFunction enclosing = HandlerHelper.findEnclosingFunction(evr);
        if (enclosing == null) return null;

        for (RAttribute input : enclosing.inputs()) {
            if (evr.enumName().equals(input.name())) {
                return input;
            }
        }
        if (enclosing.output().isPresent()
                && evr.enumName().equals(enclosing.output().get().name())) {
            return enclosing.output().get();
        }
        // facet dispatchVariantParamResolution (PR #369): a dispatch VARIANT body's
        // disguised head (`resetDates -> resetRelativeTo` parses as
        // REnumValueRef(enumName=resetDates, …) because the variant declares no inputs
        // of its own) resolves against the dispatch BASE's declared inputs — the SAME
        // attributes the generated alias/doEvaluate signatures take via signatureSource.
        // Null for every non-variant enclosing (the pre-facet decline).
        RAttribute baseInput = HandlerHelper.dispatchBaseInput(evr, evr.enumName());
        if (baseInput != null) {
            return baseInput;
        }
        return null;
    }

    /**
     * facet addRestructure (PR #346): TRUE iff {@code expr} is a DISGUISED 2-name navigation
     * — an {@link REnumValueRef} that is NOT a resolved enum constant and whose
     * {@link #resolveDisguisedFeature(REnumValueRef, ExpressionCompiler, Set)} walk (the SAME
     * resolution the navigation render uses — render-truth lockstep) resolves a feature.
     * Public bridge for the {@code function}-package distribution admissibility
     * ({@code FunctionExpressionRenderer.isDisguisedNavArm}); pure read-only.
     */
    public static boolean isDisguisedNavExpr(RExpression expr, ExpressionCompiler compiler) {
        return expr instanceof REnumValueRef evr
                && evr.enumeration().isEmpty()
                && resolveDisguisedFeature(evr, compiler, new HashSet<>()) != null;
    }

    /**
     * Compiler-carrying variant of {@link #resolveDisguisedFeature(REnumValueRef)}
     * (facet alias_receiver_typing): after the input/output arm, also resolves a
     * disguised ALIAS navigation — {@code transferExpression -> scheduledTransfer}
     * parses as {@code REnumValueRef(enumName=alias, valueName=feature)} exactly
     * like the input-based disguise — by matching {@code enumName} against the
     * enclosing function's shortcuts and resolving the feature on the alias's
     * value type through the SAME gm-aware receiver walk (visited-set threaded, so
     * an alias-of-alias disguise keeps the cycle guard). The compiler-less callers
     * (static lambda naming, {@code leafEnumeration}, {@code chainRendersMapperC})
     * stay on the input/output-only variant — each declines to status quo, never a
     * wrong byte. Package-private so sibling handlers reuse the SAME disguise
     * resolution (facet numeric_literal_typing: {@code HandlerHelper.numericOperandKind}
     * types a disguised-navigation operand — e.g. an alias body
     * {@code initialPrice -> value} — by its resolved leaf attribute).
     */
    static RAttribute resolveDisguisedFeature(REnumValueRef evr, ExpressionCompiler compiler,
            Set<RNode> visited) {
        RAttribute attr = resolveDisguisedFeature(evr);
        if (attr != null) {
            return attr;
        }
        // Copilot R1: the alias arm is gated on an ACTUAL compiler — a
        // null-compiler call (reachable via the compiler-less
        // resolveLambdaVarName overload) keeps the input/output-only behaviour
        // above, matching this method's compiler-carrying contract instead of
        // half-resolving through the static fast-paths in unit-only contexts.
        if (compiler == null || evr.enumName() == null || evr.valueName() == null) {
            return null;
        }
        // facet listOfListsCardinality (PR #370, F-D) arm 4: a ROOT that is a bare
        // RULE/FUNCTION invocation the parser's GlobalResolutionPass bound on the
        // disguised head (`TradeStateForEvent -> transferHistory` — evr.resolvedSymbol
        // carries the callable; a converted rule arrives as its fromRule RFunction; the
        // SAME binding ReferenceHandler's synthesizeRule/FunctionReceiverNavigation
        // renders the `<TransferState>mapC` witness from — the #178 same-walk law). The
        // leaf resolves on the callable's OUTPUT type (resolveReceiverOutputType — the
        // #136 naming resolution), so chainProvesMulti reads the leaf's TRUE cardinality
        // and the whole then-chain's decl wrappers / filter-and-map arities follow
        // (golden DTCC_OtherPaymentPayer/ReceiverIDTypeRule `final MapperC<TransferState>
        // thenArg0` + filterItemNullSafe + mapItem + mapItemToList). Sits ABOVE the
        // enclosing-function gate: the resolvedSymbol arm needs no enclosing scope (a
        // rule-body node's parent walk may not reach the fromRule conversion — the #363
        // detachment class). Monotone: fires only where the static arm declined.
        {
            RAttribute leaf = callableRootDisguisedLeaf(evr, compiler);
            if (leaf != null) {
                // META-annotated leaves DECLINE (stay unresolved — today's bytes): the
                // walk's consumers include the whole-output deref machinery, which would
                // read a resolved meta leaf as "the wrapper survives to the output" even
                // where the rendered chain ALREADY derefs elementwise (the green iosco
                // IdentifierOfBasketConstituentsRule `.<String>map("Type coercion", …)`
                // chain — the cp4 catch). The F-D carriers' leaves are meta-free
                // (transferHistory), so the gate costs no current flip.
                if (leaf != null && MetaFieldGenerator.detectMetaKind(leaf)
                        == MetaFieldGenerator.MetaKind.NONE) {
                    return leaf;
                }
            }
        }
        // Coverage wave D (datarule): a disguised chain inside a DATA-TYPE
        // condition — the head resolves on the declaring type's attribute chain,
        // in lockstep with ReferenceHandler.synthesizeFeatureCall's condition arm
        // (the render), so the walk and the render cannot disagree. Fixes the
        // 3-hop cascade: `taxonomy -> value -> classification -> ordinal`'s
        // hop-3 receiver is the raw 2-name EVR — resolving its leaf here gives
        // the chained step its <Integer> witness + the `taxonomyClassification`
        // type-derived lambda var (golden TaxonomyDifferentOrdinals).
        RCondition evrCondition = HandlerHelper.findEnclosingTypeCondition(evr);
        if (evrCondition != null && evrCondition.parent() instanceof RDataType evrDeclaringType) {
            RAttribute head = HandlerHelper.findAttributeOnDataType(evrDeclaringType, evr.enumName());
            RDataType headType = head == null ? null : attributeToDataType(head, compiler);
            RAttribute leaf = headType == null ? null
                    : HandlerHelper.findAttributeOnDataType(headType, evr.valueName());
            if (leaf != null) {
                return leaf;
            }
        }
        RFunction enclosing = HandlerHelper.findEnclosingFunction(evr);
        if (enclosing == null) {
            return null;
        }
        for (RShortcut shortcut : enclosing.shortcuts()) {
            if (evr.enumName().equals(shortcut.name())) {
                // facet aliasSelfShadowDisguisedChain (PR #389): the #372 F-delta-5
                // self-reference law at the WALK seat — a head matching the ENCLOSING
                // shortcut ITSELF is always the shadowed ITEM feature (rune aliases
                // cannot recurse; upstream resolves implicit-item features FIRST), so
                // the alias arm falls THROUGH to the implicit-item arm below — the
                // SAME resolution the exempted B2 synthesis renders (lockstep; golden
                // cdm6 MapPayerReceiverToAccountPartyReference `payerPartyReference ->
                // href` → the PayerModel attribute, leaf String). The pre-#389 cycle
                // guard declined the body revisit and EARLY-returned null, blocking
                // the item-type resolution entirely.
                if (HandlerHelper.findEnclosingShortcut(evr) == shortcut) {
                    break;
                }
                // Cycle protection (facet aliasCallMapWitness PR #253) is handled by
                // resolveReceiverDataType's on-stack wrapper, which guards the alias body node;
                // a self-referential disguised alias revisits that node and is declined there.
                RDataType dt = resolveReceiverDataType(shortcut.expression(), compiler, visited);
                return dt == null ? null : HandlerHelper.findAttributeOnDataType(dt, evr.valueName());
            }
        }
        // facet navWalkChoiceDisguise (PR #344) arm 1: an input/output ROOT whose declared
        // type is a CHOICE — the static path above requires referencedType() to be an
        // RDataType, which an RChoice fails, so `underlier -> Product` (Product = an option
        // of `choice Underlier`) resolved null and every step chained after the disguised
        // hop lost its <Type> witness, map/mapC arity and typed lambda var (the P344 probe:
        // `evr=underlier->Product attr=NULL` while the RENDER half's synthesized feature
        // call resolves the same hop). Narrow the root gm-aware — the SAME
        // resolveTypeCall + asRDataType projection fallbackResolveFeature's receiver walk
        // uses — then look the leaf up directly (choice options are projected as
        // type-named attributes) and through the choice-supertype options (the #207 arm),
        // so the walk and the render read one resolution.
        RAttribute rootAttr = resolveDisguisedRootAttribute(evr);
        if (rootAttr != null) {
            RDataType rootDt = attributeToDataType(rootAttr, compiler);
            if (rootDt != null) {
                RAttribute leaf = HandlerHelper.findAttributeOnDataType(rootDt, evr.valueName());
                if (leaf == null) {
                    leaf = findChoiceSuperOption(rootDt, evr.valueName(), compiler);
                }
                if (leaf != null) {
                    return leaf;
                }
            }
        }
        // facet navWalkChoiceDisguise (PR #344) arm 2: a ROOT that is a feature on the
        // enclosing lambda's implicit ITEM (`asset -> Instrument` inside
        // `transfers filter [...]` — `asset` is an attribute of the item type Transfer,
        // not a function input/alias; the P344 probe: `evr=asset->Instrument attr=NULL`,
        // `evr=quotationCharacteristicsModel->measureType attr=NULL`). Resolve the item
        // type through the SAME implicitItemDataType walk the naming/witness consumers
        // read (visited-set threaded per the #253 cycle guard), then head → leaf with the
        // same gm narrowing + choice-supertype fallback as arm 1. This is the #225
        // implicitItemDisguisedLeaf shape folded into the SHARED resolver: the disguised
        // chain's RENDER (ReferenceHandler.synthesizeImplicitItemChain) already resolves
        // it, so the type walk aligning with the render is the fix, not a widening — a
        // decline here keeps the pre-facet witness-less bytes. Input/alias roots win by
        // order (the arms above), matching upstream name resolution.
        RDataType itemType = implicitItemDataType(evr, compiler, visited);
        if (itemType != null) {
            RAttribute headAttr = HandlerHelper.findAttributeOnDataType(itemType, evr.enumName());
            if (headAttr != null) {
                RDataType headType = attributeToDataType(headAttr, compiler);
                if (headType != null) {
                    RAttribute leaf = HandlerHelper.findAttributeOnDataType(headType, evr.valueName());
                    if (leaf == null) {
                        leaf = findChoiceSuperOption(headType, evr.valueName(), compiler);
                    }
                    if (leaf != null) {
                        return leaf;
                    }
                }
            }
        }
        // facet caseSwitchAliasSubjectStmtArms (PR #370) arm 3: a ROOT that is a feature
        // of the CASE-NARROWED switch subject (`feeLeg -> feeLegSequence` inside
        // `case CreditDefaultSwap` — the same caseNarrowedImplicitType the #368 synthesis
        // renders the re-rooted chain from, so the type walk reads the SAME narrowing).
        // Without it a LEAF hop chained after the disguised pair loses its witness + its
        // mapC arity (golden MapTransferStateList `<SinglePayment>mapC("getSinglePayment",
        // …)`). Same head → leaf resolution + choice-supertype fallback as arms 1/2;
        // fires only where every arm above declined (the witness-less waivered space — a
        // green case-narrowed carrier's ≤2-hop bytes come from the synthesis, never this
        // walk), so a decline keeps today's bytes.
        RDataType caseType = caseNarrowedImplicitType(evr, compiler);
        if (caseType != null) {
            RAttribute headAttr = HandlerHelper.findAttributeOnDataType(caseType, evr.enumName());
            if (headAttr != null) {
                RDataType headType = attributeToDataType(headAttr, compiler);
                if (headType != null) {
                    RAttribute leaf = HandlerHelper.findAttributeOnDataType(headType, evr.valueName());
                    if (leaf == null) {
                        leaf = findChoiceSuperOption(headType, evr.valueName(), compiler);
                    }
                    if (leaf != null) {
                        return leaf;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Attempts to resolve the type name from a symbol reference's resolved symbol.
     *
     * <p>Handles the common cases:
     * <ul>
     *   <li>{@link RAttribute}: function input/output — has {@link RAttribute#typeCall()}</li>
     *   <li>{@link RShortcut}: alias — no declared type, returns {@code null}</li>
     * </ul>
     *
     * @return the type name, or {@code null} if unavailable
     */
    private String resolveSymbolTypeName(RSymbolReference symRef) {
        return symRef.symbol()
                .filter(RAttribute.class::isInstance)
                .map(RAttribute.class::cast)
                .map(RAttribute::typeCall)
                .map(RTypeCall::typeName)
                .orElse(null);
    }

    /**
     * Converts a (possibly namespace-qualified) Rune type name to the lowerCamelCase
     * Java identifier used as a navigation lambda variable.
     *
     * <p>Example: {@code "BusinessEvent"} → {@code "businessEvent"}.
     * Example: {@code "Instruction"} → {@code "instruction"}.
     *
     * <p>A cross-namespace type reference carries a qualified {@code typeCall().typeName()}
     * (e.g. {@code "fpml.Money"}, {@code "cde.payment.PeriodicPayment"}); a same-namespace
     * reference carries the bare simple name. The lambda variable must derive from the
     * type's SIMPLE name — emitting the qualified form verbatim produces a non-compiling
     * Java identifier ({@code fpml.Money -> fpml.Money.getX()}), and upstream never does so
     * (golden always uses the lowercased simple name, e.g. {@code money}). Lowercasing only
     * char 0 left the qualifier in place because the package segment already starts
     * lowercase. Strip any qualifier (substring after the last {@code '.'}) before
     * lowercasing, mirroring the witness path's Java-simple-name resolution
     * ({@link #resolveJavaSimpleName}, PR #88) so the lambda variable and the {@code <Type>}
     * witness stay derived from one source of truth.
     */
    private static String toLowerCamelCase(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return typeName;
        }
        int lastDot = typeName.lastIndexOf('.');
        String simple = (lastDot >= 0 && lastDot < typeName.length() - 1)
                ? typeName.substring(lastDot + 1)
                : typeName;
        return Character.toLowerCase(simple.charAt(0)) + simple.substring(1);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Resolves the generic type parameter prefix for a feature call.
     *
     * <p>If the feature has a resolved attribute with a type call, returns
     * {@code "<TypeName>"} (e.g., {@code "<Instruction>"}). Otherwise attempts
     * a best-effort fallback resolution by walking the receiver chain, which
     * recovers type info for calls whose resolvedFeature was not wired during
     * M4 type inference (typical when the receiver is a disguised
     * {@link REnumValueRef}).
     *
     * <p>If neither path yields a type name, returns an empty string (graceful
     * degradation).
     */
    private String resolveTypeParam(RFeatureCall expr, ExpressionCompiler compiler) {
        // Route the fallback through the gm-aware overload (facet choice_option_nav_witness):
        // a choice-option navigation step (`receiver -> OptionTypeName`) has no resolvedFeature
        // and the receiver's AST-level type is an RChoice, so the static fallback returns null
        // and the witness is lost. The gm-aware fallback narrows the choice to its asRDataType
        // projection (mirroring upstream attributeCall) so the option resolves + the witness
        // recovers. Strict superset of the static path: identical when the AST path resolves.
        return resolveTypeParam(expr,
                expr.resolvedFeature().orElseGet(() -> fallbackResolveFeature(expr, compiler)), compiler);
    }

    /**
     * The witness text for an ALREADY-SELECTED attribute (seat-21 review, the a7 fixture): the nav
     * seat passes the attribute the override-chain selector chose — for a META-annotated witness the
     * sentinel path declines and this text is the only render of the {@code <ReferenceWithMetaX>}
     * wrapper, so re-reading the leaf here had the TEXT name the override while the import
     * ({@code metaNavResultType}) followed the selected base. One attribute feeds text and import.
     */
    private String resolveTypeParam(RFeatureCall expr, RAttribute attr, ExpressionCompiler compiler) {
        if (attr == null || attr.typeCall() == null) return "";
        String baseName = resolveJavaSimpleName(attr, compiler);
        if (baseName == null) return "";
        return "<" + HandlerHelper.applyMetaWrapper(baseName, attr) + ">";
    }

    /**
     * facet fqnWitness (PR #197) — the navigation {@code <Type>} generic WITNESS analogue of PR #196's
     * {@code ConversionHandler.enumTargetCollisionFqn}, the THIRD render position of the upstream
     * {@code ImportingStringConcatenation.internalDoImportIfPossible} first-claim-wins import collision.
     *
     * <p>When the witness's Java simple name collides with the enclosing function's OUTPUT type but a
     * DIFFERENT canonical name (e.g. witness {@code fpml.consolidated.shared.Account} vs output
     * {@code cdm.base.staticdata.party.Account}), the output (registered first) keeps the bare
     * {@code Account} + import, so the witness must render FULLY-QUALIFIED inline
     * ({@code .<fpml.consolidated.shared.Account>mapC(...)}) with its import SUPPRESSED — else the fork
     * emits a DUPLICATE same-simple-name import = a Java compile error (the pre-fix bug, so every carrier
     * was already non-compiling/waivered = green-safe by construction).
     *
     * <p>The MANDATORY FQN-difference gate (the sibling of PR #195/#196): a witness whose canonical name
     * EQUALS the output's (the witness IS the output type — a self-referential navigation) stays bare
     * with one import. Declines for: a null/typeCall-less witness; a META witness (a different render
     * path — {@link #addWitnessTypeRef} already skips meta, and the concrete-wrapper text is not
     * collision-resolved); a navigation outside a function (no enclosing {@link RFunction} — rule bodies
     * are not reparented onto the synthetic function, so a rule-body witness declines, mirroring
     * {@code HandlerHelper.findEnclosingRule}); a function with no output; a META-annotated output
     * (its emitted type is the concrete wrapper {@code ReferenceWithMetaX}/{@code FieldWithMetaX},
     * whose simple name never equals the always-plain witness, so it never collides); and the
     * stateless unit compiler (no translator/model). Returns the witness canonical (dotted) name to render inline, or
     * {@code null} for the bare-simple-name + import path.
     *
     * @param expr     the feature-call navigation step (its AST parent chain reaches the function)
     * @param attr     the resolved witness attribute (the SAME one {@link #resolveTypeParam} reads)
     * @param compiler the parent compiler (carries the translator + generator model)
     * @return the witness FQN to render inline, or {@code null} when there is no collision
     */
    private String witnessOutputCollisionFqn(RFeatureCall expr, RAttribute attr, ExpressionCompiler compiler) {
        if (attr == null || attr.typeCall() == null) return null;
        if (MetaFieldGenerator.detectMetaKind(attr) != MetaFieldGenerator.MetaKind.NONE) return null;
        JavaTypeTranslator tt = compiler.getTypeTranslator();
        GeneratorModel gm = compiler.getGeneratorModel();
        if (tt == null || gm == null) return null;
        RFunction fn = HandlerHelper.findEnclosingFunction(expr);
        if (fn == null) return null;
        RAttribute out = fn.output().orElse(null);
        if (out == null || out.typeCall() == null) return null;
        // A META-annotated output's EMITTED type is the concrete meta wrapper
        // (ReferenceWithMetaX / FieldWithMetaX, per FunctionGenerator.resolveParam's output
        // path / PR #186 metaWit) — its real Java simple name is the wrapper, NOT the bare base
        // X that toJavaReferenceType resolves below. Since the witness is always PLAIN here (the
        // meta-witness early-return above), a meta output can never share a simple name with the
        // witness, so it never collides — decline. (Without this, a future meta-output workspace
        // carrier whose bare base X matched a different-FQN witness X would mis-fire, suppressing
        // a witness import the golden keeps when the real output import is ReferenceWithMetaX.)
        if (MetaFieldGenerator.detectMetaKind(out) != MetaFieldGenerator.MetaKind.NONE) return null;
        RType witnessRt = gm.resolveTypeCall(attr.typeCall());
        RType outputRt = gm.resolveTypeCall(out.typeCall());
        if (witnessRt == null || witnessRt instanceof RMissingType
                || outputRt == null || outputRt instanceof RMissingType) {
            return null;
        }
        JavaClass<?> witness = tt.toJavaReferenceType(witnessRt);
        JavaClass<?> output = tt.toJavaReferenceType(outputRt);
        String witnessFqn = witness.getCanonicalName().withDots();
        if (witness.getSimpleName().equals(output.getSimpleName())
                && !witnessFqn.equals(output.getCanonicalName().withDots())) {
            return witnessFqn;
        }
        return null;
    }

    /**
     * facet fqnWitness (PR #227): the witness type-param as a first-claim-wins
     * {@link ImportCollisionResolver#typeRef sentinel} (carrying the witness canonical name),
     * or {@code null} to fall back to the bare {@link #resolveTypeParam} text. Sentinel-wraps
     * ONLY a non-meta, congruent witness — exactly {@link #addWitnessTypeRef}'s import-registration
     * gate ({@code resolveJavaSimpleName == witness.getSimpleName()}) — so the render-order resolver
     * decides bare-vs-FQN for that class against any colliding construction type. Declines (keeps the
     * bare text) for a meta witness (its emitted text is the concrete wrapper, never collision-
     * resolved), an unresolved/missing witness, the stateless unit compiler, or a builtin/alias whose
     * emitted text diverges from the resolved Java simple name (a non-registered, never-colliding
     * import). The sentinel's bare resolution equals {@code resolveTypeParam}'s output for these
     * non-meta witnesses, so a non-colliding class renders byte-identically.
     */
    private String witnessSentinelTypeParam(RAttribute attr, ExpressionCompiler compiler) {
        if (attr == null || attr.typeCall() == null) return null;
        if (MetaFieldGenerator.detectMetaKind(attr) != MetaFieldGenerator.MetaKind.NONE) return null;
        JavaTypeTranslator tt = compiler.getTypeTranslator();
        GeneratorModel gm = compiler.getGeneratorModel();
        if (tt == null || gm == null) return null;
        RType rt = gm.resolveTypeCall(attr.typeCall());
        if (rt == null || rt instanceof RMissingType) return null;
        JavaClass<?> witness = tt.toJavaReferenceType(rt);
        String witnessText = resolveJavaSimpleName(attr, compiler);
        if (witnessText == null || !witnessText.equals(witness.getSimpleName())) return null;
        return "<" + ImportCollisionResolver.typeRef(witness.getCanonicalName().withDots()) + ">";
    }

    /**
     * v3.1 flip seat 21 — facet overrideChainWitness: the override-chain attribute a
     * call-ARGUMENT nav's LAST hop witnesses INSTEAD of its resolved OVERRIDE leaf — non-null
     * exactly when {@code expr} is an argument of an explicit-args {@link RFunction} call
     * (matched by node identity; by the disguised 2-name {@link REnumValueRef} shape the
     * condition-head synthesis re-enters this handle with; or by the synthesized BARE-symbol
     * nav's resolved attribute — the three shapes the ReferenceHandler synthesizers produce,
     * every one parented at the call but absent from its {@code args()}) AND
     * {@link #overrideChainBaseForDeclaredType} selects a PARENT property for the matching
     * declared input. Every other shape returns {@code null} — the resolved leaf keeps its bytes.
     * The wave-D version of this arm was TYPE-CONDITION-gated, RFunction-identity-only, one
     * supertype level deep and compared Rune nodes for equality; this one is un-gated and
     * consults the ONE selector. The IR route consults the same family
     * ({@code IRExpressionCompiler.tryEmitFromIR} declines the claim — LAW 77) so both routes
     * render the selection through this handler. An {@code RRule} callee (upstream's
     * {@code evaluateCall} serves {@code Function} AND {@code RosettaRule}) is a declared decline:
     * zero corpus carriers, no fixture — banked.
     */
    public static RAttribute calleeArgBaseAttributeOrNull(RFeatureCall expr,
            RAttribute resolvedAttr, ExpressionCompiler compiler) {
        if (!(expr.parent() instanceof RSymbolReference call) || call.args().isEmpty()) {
            return null;
        }
        int argIndex = calleeArgIndexOrMinusOne(call, expr, resolvedAttr);
        if (argIndex < 0) {
            return null;
        }
        RAttribute input = calleeInputOrNull(call, argIndex);
        return input == null ? null
                : overrideChainBaseForDeclaredType(resolvedAttr, input, compiler);
    }

    /**
     * THE ONE leaf-resolution ladder of a navigation (LAW 77 — the legacy nav seat, the IR route's
     * nav-root guard ({@code IRExpressionCompiler.tryEmitFromIR}) and
     * {@link #callCarriesOverrideBaseWitnessArg} all read it, so the two routes resolve the SAME
     * leaf before the override-chain selector runs): the linked feature, else the fallback resolution
     * ({@code fallbackResolveFeature} — the fqnWitness #197 read), else the case-narrowed
     * implicit-item leaf ({@code switchCaseNarrowedImplicitLeafAttr} — #395, consulted ONLY on null
     * so every previously-resolving rung keeps its bytes). {@code null} when no rung resolves.
     * (Seat-21 review SF-1: the IR nav-root guard had read the first rung only — a latent route split
     * with zero corpus carriers, closed by sharing the ladder.)
     */
    public static RAttribute navLeafAttrOrNull(RFeatureCall expr, ExpressionCompiler compiler) {
        RAttribute resolvedAttr = expr.resolvedFeature().orElseGet(() -> fallbackResolveFeature(expr, compiler));
        if (resolvedAttr == null) {
            resolvedAttr = switchCaseNarrowedImplicitLeafAttr(expr, compiler);
        }
        return resolvedAttr;
    }

    /**
     * The position of {@code expr} among {@code call}'s arguments under the three argument
     * shapes (identity · the disguised 2-name REnumValueRef · the synthesized bare-symbol nav),
     * or {@code -1}. Two passes (seat-21 review SF-2): IDENTITY first at every index — the nav IS an
     * argument node — then the two SYNTHESIZED shapes, where exactly ONE argument may match; two
     * same-shaped arguments to one call are AMBIGUOUS (the synthesized nav cannot tell which index it
     * stands for — zero corpus carriers, the b7 fixture) and the seat declines to the leaf rather
     * than guess the declared input. An INNER hop of a synthesized chain never matches (its parent
     * is the call but it is neither the arg node, the disguise's value name nor the bare symbol's
     * attribute) — the seat-21 probe's 16,789 {@code arm=no} call-seat navs.
     */
    private static int calleeArgIndexOrMinusOne(RSymbolReference call, RFeatureCall expr,
            RAttribute resolvedAttr) {
        for (int i = 0; i < call.args().size(); i++) {
            if (call.args().get(i) == expr) {
                return i;
            }
        }
        int shapeIndex = -1;
        for (int i = 0; i < call.args().size(); i++) {
            RExpression argNode = call.args().get(i);
            boolean matches = false;
            // The DISGUISED 2-name arg (`leg1 -> periodicPayment` parses as an
            // REnumValueRef; the condition-head synthesis re-enters this handle
            // with a SYNTHETIC RFeatureCall parented at the call but absent from
            // its args list) — match the synthesis shape by both names.
            if (argNode instanceof REnumValueRef argEvr
                    && argEvr.enumeration().isEmpty()
                    && argEvr.valueName() != null
                    && argEvr.valueName().equals(expr.featureName())
                    && expr.receiver() instanceof RSymbolReference recvRef
                    && argEvr.enumName() != null
                    && argEvr.enumName().equals(recvRef.name())) {
                matches = true;
            }
            // seat 21: the BARE-symbol arg (`extract Fn(reportableInformation)` — drr 7.x
            // ReportingTimestampRule): the ReferenceHandler synthesizers build the
            // `item`/`input -> attr` nav with resolvedFeature = the bare symbol's own RAttribute
            // (synthesizeImplicitItemNavigation's `findAttributeOnDataType(itemType, attr.name())
            // == attr` guard); match on that attribute's IDENTITY — no string leg.
            if (argNode instanceof RSymbolReference bare
                    && bare.args().isEmpty()
                    && expr.receiver() instanceof RImplicitVariable
                    && bare.symbol().orElse(null) == resolvedAttr) {
                matches = true;
            }
            if (matches) {
                if (shapeIndex >= 0) {
                    return -1; // ambiguous — decline (the leaf), never guess the declared input
                }
                shapeIndex = i;
            }
        }
        return shapeIndex;
    }

    /** The callee's declared input at {@code argIndex}, or {@code null} (a non-RFunction callee / out of range). */
    private static RAttribute calleeInputOrNull(RSymbolReference call, int argIndex) {
        RFunction callee = call.symbol().filter(RFunction.class::isInstance)
                .map(RFunction.class::cast).orElse(null);
        if (callee == null || argIndex < 0 || argIndex >= callee.inputs().size()) {
            return null;
        }
        return callee.inputs().get(argIndex);
    }

    /**
     * THE SELECTOR (LAW 69 — one body for the POJO declaration half and the nav-witness half):
     * the override-chain attribute upstream's {@code JavaPojoInterface.findProperty(name,
     * desiredType)} picks for {@code leaf} when the consumer demands {@code declaredInput}'s Java
     * type — evaluated by the fork's own port of that method on the leaf owner's
     * {@link RJavaPojoInterface} (the property chain the POJO generator builds — a parent property
     * exists only for a genuine specialization), with the desired type built by THE property
     * computation itself ({@link RJavaPojoInterface#declaredPropertyJavaType} — the generated
     * {@code FieldWithMeta<X>}/{@code ReferenceWithMeta<X>} class when the input carries
     * {@code [metadata …]}, {@code List<? extends …>}-wrapped when multi; seat-21 review SF-3: the
     * generic runtime {@code FieldWithMeta<T>} of {@code toMetaJavaType} is never a subtype of the
     * generated property class and silently degraded every meta-annotated input to the leaf). The
     * selected property maps back to the attribute its owning type declares. {@code null} when the
     * leaf is no override, the selector keeps the leaf (the desired type is a subtype of the leaf's
     * — the callee declares the override type — or no parent type fits: upstream's
     * {@code // Fallback} return), the owner is not a data type (a choice-owned leaf — declared
     * decline), or the seat cannot type (the stateless unit compiler).
     */
    public static RAttribute overrideChainBaseForDeclaredType(RAttribute leaf,
            RAttribute declaredInput, ExpressionCompiler compiler) {
        if (leaf == null || declaredInput == null || !leaf.isOverride() || compiler == null) {
            return null;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        JavaTypeTranslator tt = compiler.getTypeTranslator();
        JavaTypeUtil tu = compiler.getTypeUtil();
        if (gm == null || tt == null || tu == null) {
            return null;
        }
        RNode owner = leaf.parent();
        int depth = 0;
        while (owner != null && !(owner instanceof RDataType) && depth++ < 8) {
            owner = owner.parent();
        }
        if (!(owner instanceof RDataType ownerType)) {
            return null;
        }
        RType inputType = gm.getType(declaredInput);
        if (inputType == null || inputType instanceof RMissingType) {
            return null;
        }
        JavaType desired = RJavaPojoInterface.declaredPropertyJavaType(declaredInput, inputType, gm, tt, tu);
        RJavaPojoInterface ownerPojo = pojoInterfaceFor(ownerType, gm, tt, tu);
        boolean hasProperty = ownerPojo.getAllProperties().stream()
                .anyMatch(p -> leaf.name().equals(p.getName()));
        if (!hasProperty) {
            return null; // the owner's chain declares no such property — the leaf keeps its bytes (no catch: C0)
        }
        JavaPojoProperty selected = ownerPojo.findProperty(leaf.name(), desired);
        if (selected == null
                || !(selected.getPojo() instanceof RJavaPojoInterface selectedPojo)
                || selectedPojo.getAstNode() == null
                || selectedPojo.getAstNode() == ownerType) {
            return null;
        }
        for (RAttribute declared : selectedPojo.getAstNode().attributes()) {
            if (leaf.name().equals(declared.name())) {
                return declared;
            }
        }
        return null;
    }

    /**
     * seat-21 review (SF-7): the owner POJO model the selector consults, built ONCE per (generator
     * model, data type) — {@link RJavaPojoInterface} lazily materialises the whole property chain
     * (types, javadoc strings) and the selector runs for every override-leaf nav at a call seat
     * (16,917 across the 275 rows); the instances are read-only after construction. Weak on the
     * model so a finished run's cache is collectable; the inner map is concurrent (cells may
     * generate on several threads).
     */
    private static final java.util.Map<GeneratorModel,
            java.util.concurrent.ConcurrentMap<RDataType, RJavaPojoInterface>> OWNER_POJO_CACHE =
            Collections.synchronizedMap(new java.util.WeakHashMap<>());

    private static RJavaPojoInterface pojoInterfaceFor(RDataType ownerType, GeneratorModel gm,
            JavaTypeTranslator tt, JavaTypeUtil tu) {
        return OWNER_POJO_CACHE
                .computeIfAbsent(gm, k -> new java.util.concurrent.ConcurrentHashMap<>())
                .computeIfAbsent(ownerType, k -> new RJavaPojoInterface(k, gm, tt, tu));
    }

    /**
     * The DISGUISED-ARGUMENT form of the same predicate, for the IR route (LAW 77): whether the
     * disguised 2-name {@link REnumValueRef} {@code evr} ({@code <input> -> <feature>}, the L-111
     * shape the IR compiler claims at {@code visitEnumValueRef}) is a call argument whose last hop
     * {@link #overrideChainBaseForDeclaredType} would base-witness. Legacy renders such an arg
     * through {@code ReferenceHandler.synthesizeFeatureCall} (the synthesized nav is parented at the
     * call and carries the resolved feature) — the predicate is evaluated on THAT node, never on a
     * re-derivation, so the two routes cannot disagree on the shape.
     */
    public static boolean disguisedArgIsOverrideBaseWitness(REnumValueRef evr,
            ExpressionCompiler compiler) {
        if (evr == null || compiler == null || !evr.enumeration().isEmpty()
                || compiler.getReferenceHandler() == null
                || !(evr.parent() instanceof RSymbolReference call) || call.args().isEmpty()) {
            return false;
        }
        RFeatureCall synth = compiler.getReferenceHandler().synthesizeFeatureCall(evr, compiler);
        RAttribute leaf = synth == null ? null : synth.resolvedFeature().orElse(null);
        return leaf != null && calleeArgBaseAttributeOrNull(synth, leaf, compiler) != null;
    }

    /**
     * The CALL-root form of the same predicate, for the IR route (LAW 77): whether any argument
     * of {@code call} is a navigation whose last hop {@link #overrideChainBaseForDeclaredType}
     * would base-witness — an explicit {@link RFeatureCall} arg (its {@link #navLeafAttrOrNull}
     * leaf — the legacy nav seat's own three-rung ladder), a disguised 2-name {@link REnumValueRef}
     * arg (through {@link #disguisedArgIsOverrideBaseWitness} — evaluated on the
     * {@code ReferenceHandler.synthesizeFeatureCall} node legacy renders) or a bare attribute symbol
     * (through {@link #bareArgIsOverrideBaseWitness} — evaluated on the
     * {@code ReferenceHandler.synthesizeImplicitItemNavigation} node). An IR-driven apply renders its argument navs through the
     * IR emitter, which names the stamped LEAF type, so such a call declines to legacy whole.
     */
    public static boolean callCarriesOverrideBaseWitnessArg(RSymbolReference call,
            ExpressionCompiler compiler) {
        if (call == null || call.args().isEmpty() || compiler == null) {
            return false;
        }
        for (int i = 0; i < call.args().size(); i++) {
            RAttribute input = calleeInputOrNull(call, i);
            if (input == null) {
                continue;
            }
            RExpression arg = call.args().get(i);
            if (arg instanceof RFeatureCall fc) {
                // the explicit nav arg — the SAME node AND the SAME leaf ladder the legacy nav seat evaluates
                RAttribute leaf = navLeafAttrOrNull(fc, compiler);
                if (leaf != null && calleeArgBaseAttributeOrNull(fc, leaf, compiler) != null) {
                    return true;
                }
            } else if (arg instanceof REnumValueRef evr) {
                // the disguised 2-name arg — ONE body (disguisedArgIsOverrideBaseWitness)
                if (disguisedArgIsOverrideBaseWitness(evr, compiler)) {
                    return true;
                }
            } else if (arg instanceof RSymbolReference bare) {
                // the bare attribute symbol — ONE body (bareArgIsOverrideBaseWitness)
                if (bareArgIsOverrideBaseWitness(bare, compiler)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The BARE-SYMBOL-ARGUMENT form of the same predicate, for the IR route (LAW 77): whether the
     * bare attribute symbol {@code bare} ({@code extract Fn(reportableInformation)} — the rule-body
     * implicit-item feature the IR lowers to a FieldAccess off the item and emits with the stamped
     * LEAF) is a call argument whose nav {@link #overrideChainBaseForDeclaredType} would
     * base-witness. Legacy renders such an arg through
     * {@code ReferenceHandler.synthesizeImplicitItemNavigation} ({@code item -> attr}: receiver = the
     * synthetic implicit item, resolvedFeature = this symbol's attribute, parented at the call) and the
     * third arg shape of {@link #calleeArgBaseAttributeOrNull} decides — the predicate is evaluated on
     * THAT synthesized node, never on a re-derivation (seat-21 review SF-5: the first cut skipped the
     * synthesis and the implicit-receiver leg, strictly broader than legacy). Found by the both-route
     * drr 7.0.0 dump at the seat-21 chain head: the margin
     * {@code ReportingTimestampRule} was the ONE ON-only band file (LAW 77 — "one digest both
     * routes" is a count statement; the file lists told).
     */
    public static boolean bareArgIsOverrideBaseWitness(RSymbolReference bare,
            ExpressionCompiler compiler) {
        if (bare == null || compiler == null || !bare.args().isEmpty()
                || !(bare.parent() instanceof RSymbolReference call) || call.args().isEmpty()) {
            return false;
        }
        RAttribute leaf = bare.symbol().filter(RAttribute.class::isInstance)
                .map(RAttribute.class::cast).orElse(null);
        if (leaf == null) {
            return false;
        }
        RFeatureCall synth = ReferenceHandler.synthesizeImplicitItemNavigation(bare, leaf, compiler);
        return synth != null && calleeArgBaseAttributeOrNull(synth, leaf, compiler) != null;
    }

    /**
     * Resolve the Java simple name of a feature's type for the {@code <Type>}
     * generic witness — the legacy plugin emits the getter's Java return-type
     * simple name, NOT the raw Rune {@code typeCall().typeName()}. For workspace
     * data types and enums the two already agree (both PascalCase), but for
     * builtins they diverge: Rune {@code string}/{@code boolean}/{@code number}/{@code int}
     * → Java {@code String}/{@code Boolean}/{@code BigDecimal}/{@code Integer}. PascalCase
     * parametric type-ALIASES likewise diverge (PR #105): {@code Max3Number}
     * ({@code int} alias) → {@code Integer}, {@code Max11Number} ({@code number} alias) →
     * {@code BigDecimal} — they resolve to the wrapped base type, never the alias name
     * ({@code <Max3Number>} is no Java type). Non-alias PascalCase types (data/enum/choice)
     * keep their own name.
     * The pre-M7b-3 path emitted the raw Rune name, producing non-compiling
     * witnesses like {@code <boolean>} / {@code <FieldWithMetastring>} (both
     * present only in already-waivered files). Resolving through the translator +
     * generator model yields the Java simple name, mirroring exactly what
     * {@link #addWitnessTypeRef} computes for the import and what
     * {@code MetaFieldGenerator} uses to name the concrete wrapper class
     * ({@code FieldWithMeta} + Java simple name).
     *
     * <p>Falls back to the raw Rune {@code typeName} when the translator/model
     * are unavailable (isolated unit calls — the stateless {@code ExpressionCompiler}
     * supplies neither) or the type does not resolve / is {@link RMissingType}.
     * The fallback preserves prior behaviour for the data-type case (Rune name ==
     * Java simple name) and keeps the stateless-handler unit tests green; the
     * real builtin/alias/meta fix is exercised + verified by the D11 byte-diff.
     *
     * @param attr     the resolved feature attribute (non-null, with a typeCall)
     * @param compiler the parent compiler (carries the translator + generator model)
     * @return the Java simple name, or the raw Rune type name as a fallback
     */
    private String resolveJavaSimpleName(RAttribute attr, ExpressionCompiler compiler) {
        String runeName = attr.typeCall().typeName();
        if (runeName == null || runeName.isEmpty()) {
            return runeName;
        }
        // Falls back to the raw Rune typeName when the translator/model are unavailable
        // (isolated unit calls — the stateless ExpressionCompiler supplies neither) or the
        // type does not resolve / is RMissingType. Preserves prior behaviour for the
        // data-type case (Rune name == Java simple name) and keeps the stateless-handler
        // unit tests green; the real builtin/alias/meta fix is exercised by the D11 byte-diff.
        JavaTypeTranslator tt = compiler.getTypeTranslator();
        GeneratorModel gm = compiler.getGeneratorModel();
        if (tt == null || gm == null) {
            return runeName;
        }
        RType rt = gm.resolveTypeCall(attr.typeCall());
        if (rt == null || rt instanceof RMissingType) {
            return runeName;
        }
        // Resolve to the Java simple name for the two witness categories the legacy plugin
        // resolves (it NEVER emits the raw Rune name for either):
        //  (1) lowercase-leading Rune basic types — string→String, int→Integer,
        //      number→BigDecimal, boolean→Boolean, date/time/dateTime/zonedDateTime→
        //      Date/LocalTime/.../ZonedDateTime, plus the basictypes string aliases
        //      calculation / productType / eventType. A grep over the in-tree generated
        //      goldens (2026-05-29) finds ZERO `<calculation>` / `<int>` / `<boolean>`
        //      witnesses across cdm 5.38.0, cdm 6.20.6, drr 6.34.1, rune-fpml 2.0.0
        //      (iso20022 1.38.0 codegen is profile-gated — validated by the CI D11 run).
        //  (2) PascalCase parametric type-ALIASES — e.g. drr `standards-iso-type.rosetta`
        //      `typeAlias Max3Number: int(digits: 3)` → Integer,
        //      `typeAlias Max11Number: number(digits: 11, fractionalDigits: 10)` →
        //      BigDecimal. An RAliasType wraps a basic/number/string type and is transparent
        //      for resolution, so its Java reference type is the wrapped base — the legacy
        //      plugin emits THAT, never the alias name (`<Max3Number>` is no Java type).
        //
        // Data types / enums / choices are neither lowercase-leading NOR RAliasType, so they
        // bail here with the witness already == their own (PascalCase == Java simple) name —
        // exactly the pre-fix behaviour, no regression.
        //
        // Engine PR #7 Copilot R2 (still load-bearing): do NOT add a blanket "defer aliases"
        // branch. `int`, `number`, `date`, etc. are THEMSELVES declared as typeAliases in
        // basictypes.rosetta, so gm.resolveTypeCall returns an RAliasType for them too; an
        // RAliasType-DEFER reverts `<Integer>` → `<int>` and regresses (confirmed:
        // FrequencyPeriodMultiplier.java:32). We RESOLVE aliases through (the opposite of
        // deferring) — for both the lowercase builtins (already) and the PascalCase corpus
        // aliases (this fix) — which is what the golden does and what D11 confirms.
        if (!Character.isLowerCase(runeName.charAt(0)) && !(rt instanceof RAliasType)) {
            return runeName;
        }
        String javaSimpleName = tt.toJavaReferenceType(rt).getSimpleName();
        return javaSimpleName != null ? javaSimpleName : runeName;
    }

    /**
     * Register the import for a feature call's {@code <Type>} generic witness.
     *
     * <p>The witness <em>text</em> ({@link #resolveTypeParam}) is emitted by the
     * stateless handler, but the witness's {@link JavaClass} can only be built
     * with a {@link JavaTypeTranslator}, which the handler does not hold. This
     * method resolves it through the compiler-supplied translator + generator
     * model so the witness text and its import stay derived from the same
     * resolved {@link RAttribute} (no divergence). {@code refs} feeds the
     * {@code FunctionGenerator} structured-import path
     * ({@code collectExpressionImportsFromRefs}); same-package / {@code java.lang}
     * witnesses are filtered downstream by the {@code ImportCollector}.
     *
     * <p>Scope: plain (non-meta) witnesses only. A meta-annotated witness emits a
     * concrete wrapper text (e.g. {@code <FieldWithMetaString>}) that the
     * translator does not yet resolve by concrete name; its base type is already
     * imported via another path in currently-passing classes, so skipping meta
     * here cannot regress a passing class. Defensive on {@code null}
     * translator/model (isolated unit calls) and on {@link RMissingType}.
     *
     * @param attr     the resolved witness attribute (may be {@code null})
     * @param compiler the parent compiler (carries translator + generator model)
     * @param refs     the ref set being assembled for this feature call
     */
    private void addWitnessTypeRef(RAttribute attr, ExpressionCompiler compiler, Set<JavaClass<?>> refs) {
        if (attr == null || attr.typeCall() == null) return;
        if (MetaFieldGenerator.detectMetaKind(attr) != MetaFieldGenerator.MetaKind.NONE) return;
        JavaTypeTranslator tt = compiler.getTypeTranslator();
        GeneratorModel gm = compiler.getGeneratorModel();
        if (tt == null || gm == null) return;
        RType rt = gm.resolveTypeCall(attr.typeCall());
        if (rt == null || rt instanceof RMissingType) return;
        JavaClass<?> witness = tt.toJavaReferenceType(rt);
        // Congruence guard: register the import only when the resolved witness's
        // simple name matches the witness TEXT emitted by resolveTypeParam — which
        // (post-engine-PR-7) is resolveJavaSimpleName(attr, compiler): the Java simple
        // name for lowercase-leading basic types AND PascalCase parametric type-aliases
        // (PR #105), the raw Rune (PascalCase) name for data types / enums / choices. So:
        //  - data type / enum: Rune name == Java simple name → register (unchanged).
        //  - lowercase-leading basic type or string alias (number/date/time/.../
        //    calculation/productType/eventType): the emitted text IS the Java base
        //    simple name (BigDecimal/Date/LocalTime/String/...) → CONGRUENT → register,
        //    so the witness import matches the corrected text (java.lang witnesses like
        //    Boolean/Integer/String are filtered downstream by ImportCollector; the
        //    java.math / java.time / records witnesses ARE emitted). Engine PR #7
        //    Copilot R1: the prior raw-typeName compare skipped these, leaving a
        //    <BigDecimal>/<ZonedDateTime> witness without its import on any file where
        //    that type was not imported via another path.
        //  - PascalCase parametric type-alias (Max3Number/Max11Number, PR #105):
        //    resolveJavaSimpleName now resolves it to the base Java simple name
        //    (Integer/BigDecimal), which EQUALS witness.getSimpleName() → CONGRUENT →
        //    register, so the alias-witness's base import (e.g. java.math.BigDecimal) is
        //    emitted to match the corrected <BigDecimal> text — and the legacy golden DOES
        //    carry it (verified). java.lang bases (Integer/...) are filtered downstream.
        // (Comparing against the resolved-base, not the meta wrapper, is valid because
        // the meta early-return above excludes every non-NONE meta kind.)
        String witnessText = resolveJavaSimpleName(attr, compiler);
        if (witnessText != null && witnessText.equals(witness.getSimpleName())) {
            refs.add(witness);
        }
    }

    /**
     * facet meta_coercion_numbering (arm A1): the typed Mapper result of an
     * ALIAS-call navigation receiver — {@code MapperS/MapperC<ConcreteMetaWrapper>}
     * from {@link FunctionAliasHelper#inferShortcutMapperJavaType} — or
     * {@code null} when the receiver is not an alias reference, the alias is
     * not meta-typed, or the type infrastructure is absent (the stateless unit
     * compiler). A fresh helper instance runs the IDENTICAL deterministic walk
     * the alias method's signature was rendered from, so the stamped type and
     * the declared return type agree by construction. Package-private static
     * since facet evaluate_arg_consumption (arm B3):
     * {@code ReferenceHandler.tryMetaDerefArg} types an ALIAS-call evaluate-arg
     * through the SAME channel (the alias-call rendering carries a null
     * expression type there too).
     */
    /**
     * facet metaSigKeep (PR #382): the receiver ARGUMENT of the filter/sort whose
     * predicate/key lambda directly encloses {@code item} — {@code null} for every
     * other owner (extract/map lambdas keep their own arms; a then-body lambda keeps
     * the #362 binding channel). The walk stops at the NEAREST enclosing
     * {@link RInlineFunction} (nested coercion lambdas are render text, never on the
     * AST parent chain) and at the rule/function root.
     */
    private static RExpression filterOrSortPredicateOwnerArgument(RImplicitVariable item) {
        RNode cur = item.parent();
        int depth = 0;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction inline) {
                if (inline.parent() instanceof RFilterExpr filt) {
                    return filt.argument();
                }
                if (inline.parent() instanceof RSortExpr sortOwner) {
                    return sortOwner.argument();
                }
                return null;
            }
            if (cur instanceof RFunction || cur instanceof RRule) {
                return null;
            }
            cur = cur.parent();
        }
        return null;
    }

    public static JavaType tryAliasReceiverMapperType(RExpression receiver, ExpressionCompiler compiler) {
        AliasResolution resolution = resolveAliasShortcut(receiver);
        if (resolution == null) {
            return null;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        JavaTypeTranslator translator = compiler.getTypeTranslator();
        JavaTypeUtil typeUtil = compiler.getTypeUtil();
        if (gm == null || translator == null || typeUtil == null) {
            return null;
        }
        return new com.regnosys.rosetta.generator.java.function.FunctionAliasHelper(
                gm, translator, typeUtil)
                .inferShortcutMapperJavaType(resolution.shortcut(), resolution.enclosing());
    }

    /**
     * facet caseSwitchAliasSubjectStmtArms (PR #370): the bare MODEL item Java class of
     * an alias-reference expression — the switch-SUBJECT sibling of
     * {@link #tryAliasReceiverMapperType}, which deliberately returns {@code null} for
     * model-typed alias items (the #352 boundary its meta/basic navigation-seat
     * consumers rely on). Runs the IDENTICAL {@code FunctionAliasHelper} walk the alias
     * method's signature was rendered from, so the hoisted {@code final Product
     * switchArgument} declaration and the declared {@code MapperS<? extends Product>}
     * signature agree by construction (golden MapTransferStateList). {@code null} when
     * the receiver is not an alias reference, the alias item is not a bare model type,
     * or the type infrastructure is absent.
     */
    public static JavaClass<?> aliasModelItemJavaClass(RExpression receiver,
            ExpressionCompiler compiler) {
        AliasResolution resolution = resolveAliasShortcut(receiver);
        if (resolution == null) {
            return null;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        JavaTypeTranslator translator = compiler.getTypeTranslator();
        JavaTypeUtil typeUtil = compiler.getTypeUtil();
        if (gm == null || translator == null || typeUtil == null) {
            return null;
        }
        return new com.regnosys.rosetta.generator.java.function.FunctionAliasHelper(
                gm, translator, typeUtil)
                .inferShortcutModelItemJavaClass(resolution.shortcut(), resolution.enclosing());
    }

    /** A resolved alias-head pair: the shortcut and its enclosing function. */
    private record AliasResolution(RShortcut shortcut, RFunction enclosing) { }

    /**
     * facet dateTimeAddWitness (PR #426): the alias-head SHORTCUT of an alias
     * reference (both resolution paths — the linker-bound symbol or the
     * name-matched enclosing-function walk), or {@code null}. Package-visible so
     * {@code ArithmeticHandler}'s date/time operand gates see through aliases
     * with the SAME resolution {@link #tryAliasReceiverMapperType} uses (the
     * alias-TRANSPARENCY law — upstream's type provider types an alias reference
     * by its body).
     */
    static RShortcut aliasShortcutOrNull(RExpression receiver) {
        AliasResolution resolution = resolveAliasShortcut(receiver);
        return resolution == null ? null : resolution.shortcut();
    }

    /**
     * T2 (PR-23): the RENDER-channel SEAM of an alias-reference expression — the typed
     * facts the alias method's own declaration ({@code MapperS<…>} / {@code MapperC<…>} /
     * builder forms) was rendered from, via the IDENTICAL {@code FunctionAliasHelper}
     * walk. The S/C authority the § 6.3 program's flip policy reads (plan § 3), exposed
     * for the value-seam ADD-seat strip guard and the evaluate-arg bridge guard: unlike
     * {@link #tryAliasReceiverMapperType} (which deliberately answers
     * {@code null} for model-typed alias items — the #352 boundary), this
     * channel answers for EVERY alias item kind. {@code null} when the
     * receiver is not an alias reference or the type infrastructure is absent.
     *
     * <p>facet aliasSeamSignature (PR #612): this replaced the seam-STRING sibling —
     * consumers asked for a {@code MapperS<} prefix, which is the seam's own recorded
     * form ({@link FunctionTemplateModel.AliasSeam#isMapperSingle()}); the string had no
     * other reader here (LAW 69 — the producer's decision, not a re-derivation).
     */
    public static FunctionTemplateModel.AliasSeam aliasSeamOrNull(RExpression receiver,
            ExpressionCompiler compiler) {
        AliasResolution resolution = resolveAliasShortcut(receiver);
        if (resolution == null) {
            return null;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        JavaTypeTranslator translator = compiler.getTypeTranslator();
        JavaTypeUtil typeUtil = compiler.getTypeUtil();
        if (gm == null || translator == null || typeUtil == null) {
            return null;
        }
        for (FunctionTemplateModel.AliasModel model
                : new com.regnosys.rosetta.generator.java.function.FunctionAliasHelper(
                        gm, translator, typeUtil).analyze(resolution.enclosing())) {
            if (resolution.shortcut().name().equals(model.getName())) {
                return model.getSeam();
            }
        }
        return null;
    }

    /**
     * Resolve a navigation-chain HEAD to the alias (shortcut) it references, or
     * {@code null} when the head is not an alias reference. Shared by
     * {@link #tryAliasReceiverMapperType} (arm A1's meta re-type) and
     * {@link #chainRendersMapperC}'s alias-head arm (facet
     * interior_position_coercion) so both read the IDENTICAL resolution.
     *
     * <p>Mirror ReferenceHandler.isAliasReference's two-step resolution: a
     * resolved RShortcut symbol when the linker bound one, else a
     * name-match against the enclosing function's shortcuts — the dominant
     * alias-head shape reaches here through a SYNTHESIZED nav step
     * (`beforeTradeState -> trade` parses as a disguised REnumValueRef;
     * ReferenceHandler.synthesizeFeatureCall leaves a shortcut-named
     * receiver's symbol EMPTY because resolveNameInFunction returns null
     * for shortcut matches), and the alias-call rendering this arm types
     * fires on exactly that name-match fallback.
     */
    private static AliasResolution resolveAliasShortcut(RExpression receiver) {
        if (!(receiver instanceof RSymbolReference symRef)) {
            return null;
        }
        RShortcut shortcut = symRef.symbol()
                .filter(RShortcut.class::isInstance)
                .map(RShortcut.class::cast)
                .orElse(null);
        RFunction enclosing;
        if (shortcut != null) {
            enclosing = shortcut.parent() instanceof RFunction fn
                    ? fn
                    : HandlerHelper.findEnclosingFunction(receiver);
        } else {
            // synthesizeFeatureCall parents the synthetic receiver onto the
            // disguised node's parent, so the walk reaches the real function.
            enclosing = HandlerHelper.findEnclosingFunction(receiver);
            if (enclosing == null || symRef.name() == null) {
                return null;
            }
            for (RShortcut sc : enclosing.shortcuts()) {
                if (symRef.name().equals(sc.name())) {
                    shortcut = sc;
                    break;
                }
            }
        }
        if (shortcut == null || enclosing == null) {
            return null;
        }
        return new AliasResolution(shortcut, enclosing);
    }

    /**
     * The resolved alias's wrapper kind from
     * {@link com.regnosys.rosetta.generator.java.function.FunctionAliasHelper#inferShortcutIsMulti}
     * — {@code true}/{@code false} for a MapperC/MapperS-signatured alias,
     * {@code null} when the walk declines or the type infrastructure is absent
     * (the stateless unit compiler). Facet interior_position_coercion; consumed
     * MONOTONically by {@link #chainRendersMapperC}'s two alias-head arms.
     */
    private static Boolean aliasIsMulti(AliasResolution alias, ExpressionCompiler compiler) {
        GeneratorModel gm = compiler.getGeneratorModel();
        JavaTypeTranslator translator = compiler.getTypeTranslator();
        JavaTypeUtil typeUtil = compiler.getTypeUtil();
        if (gm == null || translator == null || typeUtil == null) {
            return null;
        }
        return new com.regnosys.rosetta.generator.java.function.FunctionAliasHelper(
                gm, translator, typeUtil)
                .inferShortcutIsMulti(alias.shortcut(), alias.enclosing());
    }

    /**
     * facet multiDefaultGuardedFold (PR #367): the ONE gate for the ctor-FIELD
     * MULTI-default ternary — shared by SetOperationHandler's render arm and
     * ConstructionHandler's verbatim-splice mirror so the two seats cannot
     * disagree (the #365 textually-identical-gates law, hardened to a single
     * predicate). Upstream binaryExpr {@code case "default"} selects the
     * list-conditional form on LEFT.isMulti ALONE; the #365 both-multi gate
     * under-fired the MULTI-left + SINGLE-alias-right carrier
     * (MapPartyTradeIdentifierToTradeIdentifierList {@code assignedIdentifier} —
     * the alias signatures render {@code MapperC} left / {@code MapperS} right,
     * and {@code MapperS.getMulti()} is the 0/1-element list, so both arms'
     * {@code .getMulti()} stay valid). The RIGHT therefore needs only a PROVEN
     * Mapper render: {@link #chainProvesMulti} (the #365 gate, unchanged) OR a
     * RESOLVED alias wrapper kind — the SAME {@code inferShortcutIsMulti} walk
     * that renders the alias method's signature, where TRUE and FALSE both
     * prove a Mapper call; a declined walk keeps the #365 decline. Green-safe
     * by the #365 argument: {@code getOrDefault} takes the ITEM type, so the
     * pre-fix {@code MapperC.getOrDefault(Mapper)} never compiled — every
     * carrier is waivered.
     */
    public static boolean multiDefaultTernaryOperands(
            com.regnosys.rosetta.ast.expressions.binary.RDefaultExpr expr,
            ExpressionCompiler compiler) {
        if (expr.rawLeft() == null || expr.rawRight() == null) {
            return false;
        }
        if (!chainProvesMulti(expr.rawLeft(), compiler)) {
            return false;
        }
        if (chainProvesMulti(expr.rawRight(), compiler)) {
            return true;
        }
        AliasResolution rightAlias = resolveAliasShortcut(expr.rawRight());
        return rightAlias != null && aliasIsMulti(rightAlias, compiler) != null;
    }

    /**
     * facet extractBodyMultiDefaultTernary (v3.1 flip seat 32, law D2): the ONE gate for the
     * EXTRACT-BODY multi-{@code default} ternary -- the third seat of the
     * {@code thenSeatMultiDefaultTernary} family, shared by {@code SetOperationHandler}'s
     * render arm and {@code ControlFlowHandler}'s Mapper-form ite slot as the NECESSARY
     * condition both halves key on (the #367 single-predicate law, the same reason
     * {@link #multiDefaultTernaryOperands} exists). Each half carries its own further
     * preconditions -- the render arm a resolvable joined element from the LEFT's compiled stamp
     * and its infra guards, the slot the hoist path being reached -- so the invariant is "no
     * ternary and no slot WITHOUT this predicate", not a biconditional; at every measured carrier
     * both fire (the 4 {@code [P32-DEF]} rows).
     *
     * <p>Upstream {@code ExpressionGenerator.xtend} L452-469 {@code case "default"} selects
     * the list-conditional form on {@code left.isMulti} ALONE and compiles the RIGHT at
     * {@code MAPPER.wrapExtends(joined)} in that branch (against the ITEM type in the single
     * branch). The fork already admits that render at two seats -- inside an ALIAS body
     * (#345) and at the then-BASE (seat 5) -- and DECLINES a default that is the BODY of a
     * {@code then extract} inline function, whose parent is the {@link RInlineFunction}
     * ({@code SetOperationHandler.thenBaseDefaultSeat}'s own javadoc says so). This is that
     * third seat, and the family's natural experiment is two drr 7.x rules over one shape:
     * {@code IndicatorOfTheUnderlyingIndex} (no {@code first} on the left, multi, golden
     * ternary -- the carrier) against {@code UnderlyingIndexIndicator} ({@code first} on the
     * left, single, golden {@code getOrDefault} -- byte-frozen by the LEFT-only proof).
     *
     * <p><b>The alias exclusion is LOAD-BEARING, measured.</b> [P32-DEF] over 1,216 rows,
     * route-identical: {@code gateExtractBody=true AND multiLeft=true} holds at exactly TWO
     * {@code where=} values -- the 4 carrier rows and 5 rows of the GREEN
     * {@code fn:MapBasketReferenceInformation} (cdm 6.20.2-6.20.6), which already renders
     * this ternary through the #345 alias arm. Excluding the alias scope keeps that arm, its
     * expected types and its bytes untouched. The wider {@code gateExtractBody=true}
     * population is 111 rows over 20 {@code where=} values, ALL {@code multiLeft=false} --
     * including 9 rows of the green {@code rule:CollateralPortfolioIndicator} -- so the
     * LEFT-only proof holds every one of them out.
     *
     * <p><b>{@link #chainProvesMulti} is a CONSERVATIVE under-approximation of upstream's
     * {@code left.isMulti}, not a mirror -- do not widen it on this law's account.</b>
     * Measured at the GREEN {@code fn:MapBreakdown} (12 golden files, 0 band rows): 24 of its
     * 36 default rows read {@code multiLeft=false multiRight=true} while each of its goldens
     * carries THREE {@code getMulti().isEmpty() ? } ternaries -- at least two golden MULTI
     * ternaries per file sit over a left this proof calls single (an alias-call left). The
     * predicate is correct at both of this law's carriers and can only ever UNDER-fire
     * (keeping {@code getOrDefault}), which is byte-flat; MapBreakdown must stay
     * byte-identical and {@code corpus_control1} + {@code e2} pin that.
     *
     * @param expr     the {@code default} node
     * @param compiler the compiler (the multi proof's resolution channel)
     * @return {@code true} when this default takes the extract-body multi ternary
     */
    public static boolean extractBodyMultiDefaultTernary(RDefaultExpr expr,
            ExpressionCompiler compiler) {
        if (expr.rawLeft() == null || expr.rawRight() == null) {
            return false;
        }
        if (HandlerHelper.findEnclosingShortcut(expr) != null) {
            return false;
        }
        if (!(expr.parent() instanceof RInlineFunction inl)
                || inl.body() != expr
                || !(inl.parent() instanceof RExtractExpr)) {
            return false;
        }
        return chainProvesMulti(expr.rawLeft(), compiler);
    }

    /**
     * facet defaultAliasArgDeref (PR #367 widening): true when {@code node} is an
     * ALIAS (shortcut) reference whose body is a DIRECT function call with a
     * SINGLE, non-meta output — the {@code MapperS<Enum>}-signatured shape
     * {@code tryAliasReceiverMapperType} cannot type (its
     * {@code inferShortcutMapperJavaType} walk resolves meta and basic items
     * only, so an enum/model item declines it). The alias method's signature
     * derives from the SAME fn-output resolution (a single output renders
     * {@code MapperS}), so the {@code .get()} the #234/#352 reduction appends
     * cannot disagree with the rendered wrapper; the non-meta gate keeps the
     * meta-item law (a wrapper item's {@code .get()} yields the wrapper, not
     * the value — those stay declined). Carrier: MapPartyTradeIdentifier
     * {@code identifierTypeForIssuer} (body {@code MapTradeIdToIdentifierType(…)},
     * output {@code TradeIdentifierTypeEnum (0..1)}).
     */
    public static boolean aliasSingleNonMetaFnCallOutput(RExpression node,
            ExpressionCompiler compiler) {
        AliasResolution alias = resolveAliasShortcut(node);
        if (alias == null || compiler == null) {
            return false;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        if (gm == null || !(alias.shortcut().expression() instanceof RSymbolReference call)) {
            return false;
        }
        var calledFn = call.symbol()
                .filter(com.regnosys.rosetta.ast.functions.RFunction.class::isInstance)
                .map(com.regnosys.rosetta.ast.functions.RFunction.class::cast)
                .orElse(null);
        RAttribute out = calledFn == null ? null : calledFn.output().orElse(null);
        return out != null && !gm.isMulti(out)
                && MetaFieldGenerator.detectMetaKind(out) == MetaFieldGenerator.MetaKind.NONE;
    }

    /**
     * facet switchChoiceLambda (PR #226): true when {@code receiver} is an ALIAS (shortcut)
     * reference whose inferred wrapper kind is MULTI (MapperC). The {@code mapItem} selection for
     * an {@code extract} over a multi alias the legacy {@link CardinalityComputer} hard-codes
     * SINGLE (RShortcut) and {@link #chainProvesMulti}'s {@code RAttribute}-only RSymbolReference
     * arm misses ({@code fpmlCommoditySwapLegList: fpmlCommoditySwapDetailsModel -> commoditySwapLeg}).
     * SCOPED to the in-lambda choice-switch render (CollectionHandler) so the broad {@code mapMethod}
     * stays untouched. Conservative: {@code false} for a non-alias receiver or when
     * {@link #aliasIsMulti} (inferShortcutIsMulti) declines — never widens a genuinely-single alias.
     */
    public static boolean aliasReceiverProvesMulti(RExpression receiver, ExpressionCompiler compiler) {
        AliasResolution alias = resolveAliasShortcut(receiver);
        if (alias == null) {
            return false;
        }
        return Boolean.TRUE.equals(aliasIsMulti(alias, compiler));
    }

    /**
     * facet listLiteralAliasMetaWitness (PR #345): the concrete meta wrapper of an
     * ALIAS-invocation expression's signature element — {@code FieldWithMetaString}
     * for an alias whose method renders {@code MapperC<? extends FieldWithMetaString>}
     * — via the SAME {@code FunctionAliasHelper.inferShortcutMapperJavaType} walk
     * that renders that signature into the same file (render-truth lockstep; the
     * meta-arm returns null for every non-meta alias, so this is {@code null} for a
     * non-alias expression, a non-meta alias, or absent type infrastructure).
     * Consumed by {@code LiteralHandler}'s list-literal witness lift.
     */
    public static RJavaWithMetaValue aliasInvocationMetaWrapper(RExpression e, ExpressionCompiler compiler) {
        AliasResolution alias = resolveAliasShortcut(e);
        if (alias == null) {
            return null;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        JavaTypeTranslator translator = compiler.getTypeTranslator();
        JavaTypeUtil typeUtil = compiler.getTypeUtil();
        if (gm == null || translator == null || typeUtil == null) {
            return null;
        }
        JavaType mapper = new com.regnosys.rosetta.generator.java.function.FunctionAliasHelper(
                gm, translator, typeUtil)
                .inferShortcutMapperJavaType(alias.shortcut(), alias.enclosing());
        if (mapper == null) {
            return null;
        }
        return typeUtil.getItemType(mapper) instanceof RJavaWithMetaValue wrapper ? wrapper : null;
    }

    /**
     * The wrapper kind of a disguised navigation's ALIAS root — {@code
     * interestRatePayouts -> rateSpecification} parses as
     * {@code REnumValueRef(enumName=interestRatePayouts, valueName=rateSpecification)}
     * with the root name carried in {@code enumName()}; when that name matches a
     * shortcut of the enclosing function (the "disguised ALIAS hop" that
     * {@link #resolveDisguisedRootAttribute} deliberately declines), the alias's
     * {@link #aliasIsMulti} wrapper kind is the chain's. {@code null} when the
     * root is not an alias or the walk declines.
     */
    private static Boolean disguisedAliasRootIsMulti(REnumValueRef evr, ExpressionCompiler compiler) {
        // v3.1 flip seat 4 (the indep review's hardening): a null compiler
        // declines here — aliasIsMulti dereferences it, and the seat-4 rung is
        // the first consult reachable from a previously fully-null-safe
        // attr==null path (the compiler-less unit contexts decline through the
        // walk's own null-infrastructure gates on every real caller today; this
        // makes the javadoc's decline contract literal at all three consult
        // sites).
        if (compiler == null) {
            return null;
        }
        if (evr.enumName() == null) {
            return null;
        }
        RFunction enclosing = HandlerHelper.findEnclosingFunction(evr);
        if (enclosing == null) {
            return null;
        }
        for (RShortcut sc : enclosing.shortcuts()) {
            if (evr.enumName().equals(sc.name())) {
                return aliasIsMulti(new AliasResolution(sc, enclosing), compiler);
            }
        }
        return null;
    }

    /**
     * Rebuild {@code builder}'s expression with {@code type} stamped on it —
     * rendering, refs and static wildcard imports unchanged (arm A1's alias-call
     * receiver re-type; the alias method declaration owns the wrapper import,
     * so no ref is added here). Package-private since facet
     * arithOperandWrapperCoerce (PR #334): {@code ArithmeticHandler} re-types an
     * ALIAS-call operand through the SAME channel before its wrapper-level
     * operand coercion. Public since v3.2 seat 13 (#634, commit 4):
     * {@code FunctionExpressionRenderer}'s whole-output SET element-wrapper recovery
     * (the R12 / M7a heal) re-types the lifted chain through the same channel from
     * the function package.
     */
    public static JavaStatementBuilder retypeBuilder(JavaStatementBuilder builder, JavaType type) {
        return builder.mapExpression(e -> JavaExpression.from(
                e.renderToString(), type, e.getRefs(), e.getStaticWildcardImports()));
    }

    /**
     * Engine PR #9 (PR-B) — the typed-pipeline meta result type. When a navigation
     * step reads a {@code [metadata reference|address|scheme|id|location]} attribute,
     * its rendered value is a {@code ReferenceWithMetaX}/{@code FieldWithMetaX} wrapper;
     * this surfaces the matching concrete {@link RJavaWithMetaValue} (wrapped in
     * {@code MapperS}/{@code MapperC} by cardinality) as the navigation's expression
     * type, so a deeper step's {@link ExpressionCompiler#coerceNavigationReceiver} can
     * meta-strip this receiver (emitting the golden {@code .getValue()} unwrap). It also
     * registers the concrete wrapper as a ref — {@link #addWitnessTypeRef} early-returns
     * for meta attributes, so the {@code <ReferenceWithMetaX>}/{@code <FieldWithMetaX>}
     * witness import enters {@code refs} here.
     *
     * <p>Returns {@code null} (coercion stays dormant, output byte-flat) for non-meta
     * attributes, or when the translator/model/type-util are unavailable (the stateless
     * unit {@code ExpressionCompiler} supplies none — exercised by the D11 byte-diff) or
     * the attribute type does not resolve. Like {@link #resolveJavaSimpleName}, the meta
     * wrapper's value type is the attribute's resolved Java reference type.
     *
     * @param attr     the resolved feature attribute (may be {@code null})
     * @param compiler the parent compiler (carries translator + generator model + type util)
     * @param refs     the ref set being assembled (the wrapper import is added here)
     * @param multi    whether the chain renders a {@code MapperC} at this step — this
     *                 step is {@code mapC} OR an uncollapsed {@code mapC} lies upstream
     *                 of it ({@link #chainRendersMapperC}; facet choice_nav_chain_typing)
     * @return the {@code MapperS}/{@code MapperC}-wrapped concrete meta type, or {@code null}
     */
    static JavaType metaNavResultType(RAttribute attr, ExpressionCompiler compiler,
                                       Set<JavaClass<?>> refs, boolean multi) {
        RJavaWithMetaValue metaWrapper = metaWrapperOf(attr, compiler);
        if (metaWrapper == null) return null;
        JavaTypeUtil tu = compiler.getTypeUtil();
        refs.add(metaWrapper);
        // Concrete wrapper declaration per branch — a MAPPER_C/MAPPER_S ternary would
        // capture to a wildcard JavaGenericTypeDeclaration and defeat wrap's inference.
        return multi
                ? tu.wrap(tu.MAPPER_C, metaWrapper)
                : tu.wrap(tu.MAPPER_S, metaWrapper);
    }

    /**
     * The value-level meta wrapper ({@link RJavaWithMetaValue}: {@code FieldWithMetaString} /
     * {@code ReferenceWithMetaX}) an attribute carries via its {@code [metadata …]} annotation, or
     * {@code null} for a non-meta / unresolved attribute. Extracted from {@link #metaNavResultType}
     * (facet disguisedOutputMeta, PR #287) so the {@code MapperS<wrapper>} receiver retype and the
     * raw wrapper (the disguised-output deref's precomputed meta, {@link #recoverDisguisedTerminalMetaWrapper})
     * share one resolution. Does NOT register the wrapper on a refs set — the caller adds it.
     * PUBLIC since PR #368 (F-B): the #221 choice-switch ADD-empty element derivation
     * (FunctionExpressionRenderer) needs the same concrete-wrapper resolution.
     */
    public static RJavaWithMetaValue metaWrapperOf(RAttribute attr, ExpressionCompiler compiler) {
        if (attr == null || attr.typeCall() == null) return null;
        MetaFieldGenerator.MetaKind kind = MetaFieldGenerator.detectMetaKind(attr);
        if (kind == MetaFieldGenerator.MetaKind.NONE) return null;
        JavaTypeTranslator tt = compiler.getTypeTranslator();
        GeneratorModel gm = compiler.getGeneratorModel();
        JavaTypeUtil tu = compiler.getTypeUtil();
        if (tt == null || gm == null || tu == null) return null;
        RType rt = gm.resolveTypeCall(attr.typeCall());
        if (rt == null || rt instanceof RMissingType) return null;
        JavaReferenceType valueType = tt.toJavaReferenceType(rt);
        return RJavaWithMetaValue.create(
                kind == MetaFieldGenerator.MetaKind.REFERENCE_WITH_META, valueType, tu);
    }

    /**
     * facet metaInputParam (PR #342): the value-level meta wrapper of a bare
     * {@link RSymbolReference} that resolves to a {@code [metadata …]}-annotated
     * SINGLE-cardinality INPUT parameter of the enclosing function — the body-side twin
     * of {@code FunctionGenerator.resolveParam}'s input wrap (the #186 output law
     * extended to inputs), reading the SAME resolution ({@link #metaWrapperOf}) the
     * signature renders, so the param's declared Java type and every body access agree.
     *
     * <p>Null for everything else: a symbol with args, a non-attribute symbol, an
     * attribute that is not one of the enclosing function's declared inputs (IDENTITY
     * compare — locals, lambda params, rule attributes and mis-bound globals never
     * admit), a MULTI input (no corpus carrier — the meta-input population is
     * corpus-closed at 5 files, all single-card), or a non-meta input. Green-safe by
     * construction: no green file carries a meta-annotated function input (the corpus
     * scan is the population proof), so every consumer arm only ever rewrites
     * already-waivered output.
     */
    static RJavaWithMetaValue metaInputParamWrapper(RSymbolReference expr, ExpressionCompiler compiler) {
        if (expr == null || compiler == null || !expr.args().isEmpty()) return null;
        RAttribute attr = expr.symbol()
                .filter(RAttribute.class::isInstance)
                .map(RAttribute.class::cast)
                .orElse(null);
        if (attr == null) return null;
        com.regnosys.rosetta.ast.functions.RFunction fn = HandlerHelper.findEnclosingFunction(expr);
        if (fn == null) return null;
        boolean isDeclaredInput = false;
        for (RAttribute input : fn.inputs()) {
            if (input == attr) {
                isDeclaredInput = true;
                break;
            }
        }
        if (!isDeclaredInput) return null;
        GeneratorModel gm = compiler.getGeneratorModel();
        if (gm == null || gm.isMulti(attr)) return null;
        return metaWrapperOf(attr, compiler);
    }

    /**
     * facet fnIoMetaValueDeref (PR #433, finding #21): the null-guarded VALUE deref
     * of a BARE single-card meta-input param read at a VALUE-consuming seat —
     * {@code (myInput == null ? MapperS.<V>ofNull() : MapperS.of(myInput.getValue()))},
     * typed {@code MapperS<V>}. Upstream's {@code convertNullSafe} on a raw
     * wrapper-typed Java LOCAL (the param IS the local) — the same template the
     * #342 nav-receiver arm renders inline; consumed by the arithmetic-operand and
     * choice-argument seats (the func-meta-scheme-arith / func-meta-choice-ignore
     * oracle goldens). Returns {@code null} unless the expression is a bare
     * argument-less reference to a [metadata …]-annotated single-card FUNCTION
     * input ({@link #metaInputParamWrapper}'s exact population).
     */
    static JavaExpression metaInputValueDeref(RExpression expr, ExpressionCompiler compiler) {
        if (!(expr instanceof RSymbolReference symRef) || compiler == null) {
            return null;
        }
        RJavaWithMetaValue wrapper = metaInputParamWrapper(symRef, compiler);
        if (wrapper == null) {
            return null;
        }
        JavaTypeUtil tu = compiler.getTypeUtil();
        if (tu == null) {
            return null;
        }
        String valueSimple = wrapper.getValueType().getSimpleName();
        Set<JavaClass<?>> derefRefs = new HashSet<>();
        derefRefs.add(HandlerHelper.MAPPER_S);
        if (wrapper.getValueType() instanceof JavaClass<?> valueClass) {
            derefRefs.add(valueClass);
        }
        return JavaExpression.from(
                "(" + symRef.name() + " == null ? MapperS.<" + valueSimple
                        + ">ofNull() : MapperS.of(" + symRef.name() + ".getValue()))",
                tu.wrap(tu.MAPPER_S, wrapper.getValueType()),
                derefRefs);
    }

    /**
     * v3.2 seat 1 (F2 dropped-coercion): the NUMERIC twin of {@link #metaInputValueDeref} — a bare
     * single-cardinality function INPUT whose Java type is a boxed numeric the arithmetic join
     * must WIDEN (an Integer input in a BigDecimal join — the chaos s08 {@code C8Scale} rows; a
     * {@code number(fractionalDigits: 0)} alias and a plain {@code int} input are the same Java
     * {@code Integer}). Upstream compiles the operand against {@code MAPPER.wrapExtends(join)} and
     * its item→wrapper {@code convertNullSafe} at the variable seat guards OUTSIDE the wrap,
     * because a boxed parameter can be null:
     * {@code (q == null ? MapperS.<BigDecimal>ofNull() : MapperS.of(BigDecimal.valueOf(q)))}. The
     * conversion text comes from the coercion service (one source of truth for the promotion
     * table) and the empty from its typed-empty factory; the parameter name is the one the
     * reference rendered (escapes included), read off the compiled wrap's bare inner.
     *
     * @return the guarded wrapper expression, typed {@code MapperS<join>}; {@code null} unless the
     *         operand is such an input (a meta-annotated input takes the meta twin; a same-typed
     *         or non-numeric input is identity; a multi input never reaches an arithmetic operand)
     */
    static JavaExpression numericInputParamCoerceOrNull(RExpression expr, JavaStatementBuilder compiled,
            JavaType joinItemType, JavaStatementScope scope, ExpressionCompiler compiler) {
        if (!(expr instanceof RSymbolReference symRef) || compiler == null || joinItemType == null
                || !symRef.args().isEmpty() || !(compiled instanceof JavaExpression compiledExpr)) {
            return null;
        }
        RAttribute attr = symRef.symbol()
                .filter(RAttribute.class::isInstance)
                .map(RAttribute.class::cast)
                .orElse(null);
        if (attr == null) {
            return null;
        }
        com.regnosys.rosetta.ast.functions.RFunction fn = HandlerHelper.findEnclosingFunction(expr);
        if (fn == null) {
            return null;
        }
        boolean isDeclaredInput = false;
        for (RAttribute input : fn.inputs()) {
            if (input == attr) {
                isDeclaredInput = true;
                break;
            }
        }
        if (!isDeclaredInput) {
            return null;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        JavaTypeTranslator tt = compiler.getTypeTranslator();
        JavaTypeUtil tu = compiler.getTypeUtil();
        com.regnosys.rosetta.generator.java.expression.TypeCoercionService svc =
                compiler.getCoercionService();
        if (gm == null || tt == null || tu == null || svc == null || gm.isMulti(attr)
                || MetaFieldGenerator.detectMetaKind(attr) != MetaFieldGenerator.MetaKind.NONE) {
            return null;
        }
        RType paramRType = gm.getType(attr);
        JavaClass<?> paramJava = paramRType == null ? null : tt.toJavaReferenceType(paramRType);
        if (paramJava == null || paramJava.equals(joinItemType)
                || !(tu.extendsNumber(paramJava) && tu.extendsNumber(joinItemType))) {
            return null;
        }
        java.util.Optional<JavaStatementBuilder> bare = compiledExpr.unwrapToBuilder();
        if (bare.isEmpty()) {
            return null;
        }
        String name = HandlerHelper.render(bare.get());
        if (!javax.lang.model.SourceVersion.isName(name)) {
            return null;
        }
        JavaExpression paramRef = JavaExpression.from(name, paramJava);
        JavaExpression converted = svc.coerceExpression(paramRef, paramJava, joinItemType, true, scope);
        if (converted == paramRef) {
            // identity - the plain wrap is the join's own form. Decided by REFERENCE, never by
            // rendered text (PR #622, the reviews' SF-2): the service hands back the SAME instance
            // on every no-op path (TypeCoercionServiceTest.noOpCoercion_returnsTheSameInstance).
            return null;
        }
        JavaType wrapped = tu.wrap(tu.MAPPER_S, joinItemType);
        JavaStatementBuilder empty = svc.emptyValueFor(wrapped);
        // the refs and static wildcards of the operand AS COMPILED are carried (the reviews'
        // SF-5): the guarded form re-renders that operand, so whatever import it needed is still
        // needed - then the conversion's, the typed empty's and the wrap's own
        Set<JavaClass<?>> refs = new HashSet<>(compiledExpr.getRefs());
        refs.addAll(converted.getRefs());
        refs.addAll(empty.getRefs());
        refs.add(HandlerHelper.MAPPER_S);
        if (joinItemType instanceof JavaClass<?> joinClass) {
            refs.add(joinClass);
        }
        Set<JavaClass<?>> wildcards = new HashSet<>(compiledExpr.getStaticWildcardImports());
        wildcards.addAll(converted.getStaticWildcardImports());
        return JavaExpression.from(
                "(" + name + " == null ? " + HandlerHelper.render(empty) + " : MapperS.of("
                        + converted.renderToString() + "))",
                wrapped, refs, wildcards);
    }

    /**
     * facet evalArgMetaDerefHoist (PR #237) — the meta {@code MapperS<FieldWithMetaX>}
     * type of an evaluate-arg nav expression whose own {@code getExpressionType()} is
     * {@code null}, used by {@link com.regnosys.rosetta.generator.java.expression.handlers.ReferenceHandler#tryMetaDerefArg}
     * to recover the wrapper item type when the compiled chain erased its meta type
     * (a multi-intermediate {@code mapC} chain collapses through an
     * {@link RListOpExpr} {@code ONLY_ELEMENT}/{@code FIRST}/{@code LAST} or an
     * {@link RExtractExpr} {@code map}, neither of which surfaces the terminal feature's
     * meta wrapper as the result expression type — the same gap
     * {@link #implicitItemMetaMapperType} declines for list-op-wrapped arguments).
     *
     * <p>Resolves the TERMINAL navigation attribute (the outermost feature actually
     * delivered to the callee, descending only single-collapsing list-ops + the map
     * body — element-type-preserving steps) and returns its
     * {@link #metaNavResultType} {@code MapperS} wrap (single — the arg is consumed at
     * a single-cardinality parameter; the caller has already required the param single).
     * Returns {@code null} (caller keeps the flat, still-waivered form) for a non-meta
     * terminal, an unresolvable feature, or a shape that is not a navigation chain.
     */
    public static JavaType tryTerminalMetaMapperType(RExpression arg, ExpressionCompiler compiler) {
        if (arg == null || compiler == null) {
            return null;
        }
        RAttribute terminal = terminalNavAttr(arg, compiler);
        if (terminal == null) {
            return null;
        }
        return metaNavResultType(terminal, compiler, new HashSet<>(), false);
    }

    /**
     * facet ruleMetaListWrap (PR #360): the RECOVERY-LOCAL rule-context disguised-leaf meta
     * read — the case (a3) helper of {@link #recoverMetaFromExpr}. Unwraps an extract /
     * element-preserving list-op shell ({@code then extract <evr> distinct} step bodies) to
     * a bare disguised 2-name {@link REnumValueRef}, requires a RULE context, types the
     * implicit ITEM via {@link #implicitItemDataType} (the #344 arm-2 walk — rule-side
     * capable), resolves head → leaf (with the #207
     * choice-supertype fallback) and returns the leaf's value-level meta wrapper. Returns
     * {@code null} for every non-matching shape, non-rule context, unresolvable item/head/
     * leaf, or meta-free leaf. Deliberately NOT routed through the shared
     * {@code resolveDisguisedFeature} (function-gated): widening that resolver to rules
     * re-rendered 12 green rule files through its naming/witness consumers (the cp2e
     * over-fire catch) — this read is visible ONLY to the meta-recovery consumers.
     * Package-visible since PR #362: the LiteralHandler #337 witness arm consumes the SAME
     * read for a rule-context item-rooted disguised element (the esma/fca UPIRule
     * {@code [contractualProduct -> productIdentifier, security -> productIdentifier]}
     * base literal — {@code onlyElementLeafAttribute}'s EVR arm resolves through the
     * function-gated shared resolver and so declines in rules).
     */
    static RJavaWithMetaValue ruleContextDisguisedLeafMeta(RExpression expr,
            ExpressionCompiler compiler) {
        RExpression e = expr;
        int hops = 0;
        while (e != null && hops++ < 8) {
            if (e instanceof RExtractExpr ext) {
                e = ext.body() == null ? null : ext.body().body();
                continue;
            }
            if (e instanceof RListOpExpr lo
                    && (lo.op() == ListOp.ONLY_ELEMENT || lo.op() == ListOp.FIRST
                        || lo.op() == ListOp.LAST || lo.op() == ListOp.DISTINCT)) {
                e = lo.argument();
                continue;
            }
            break;
        }
        if (!(e instanceof REnumValueRef evr) || evr.enumeration().isPresent()
                || evr.enumName() == null || evr.valueName() == null
                || HandlerHelper.findEnclosingRule(evr) == null) {
            return null;
        }
        Set<RNode> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        RDataType itemType = implicitItemDataType(evr, compiler, visited);
        if (itemType == null) {
            return null;
        }
        RAttribute head = HandlerHelper.findAttributeOnDataType(itemType, evr.enumName());
        RDataType headType;
        if (head == null) {
            // v3.1 flip seat 22, F1 rung R3 (facet wholeOutputMetaDeref): the disguised head that names
            // a CALLABLE - the parser's C1 head binding (REnumValueRef.resolvedHead(), THE AUTHORITY for
            // what the left name means) is an RRule/RFunction (`ExtractReferenceEntity -> identifier`,
            // the drr 7.0-7.3 ReferenceEntityRule carriers), so the chain re-roots on the callable's
            // OUTPUT type - an RFunction through its declared output attribute's resolved type call and
            // an RRule through HandlerHelper.ruleInferredOutputRType (THE ONE rule-output read, seat 21).
            // STRICT fall-through (an attribute head keeps the walk above byte-identically); the green
            // RRule-/RFunction-head look-alikes (FixedRate*/FloatingRate*, BasketConstituent*) resolve a
            // META-FREE leaf here and decline at metaWrapperOf - the whole-cell controls and the
            // IR-route control2 are the receipt.
            headType = disguisedCallableHeadOutputType(evr, compiler);
        } else {
            headType = attributeToDataType(head, compiler);
        }
        if (headType == null) {
            return null;
        }
        RAttribute leaf = HandlerHelper.findAttributeOnDataType(headType, evr.valueName());
        if (leaf == null) {
            leaf = findChoiceSuperOption(headType, evr.valueName(), compiler);
        }
        return leaf == null ? null : metaWrapperOf(leaf, compiler);
    }

    /**
     * v3.1 flip seat 22, F1 rung R3 helper (facet {@code wholeOutputMetaDeref}): the OUTPUT data type of
     * the CALLABLE a disguised head's parser binding names - {@code REnumValueRef.resolvedHead()} (the C1
     * head-binding authority) holding an {@link RFunction} (its declared output attribute's resolved type
     * call) or an {@code RRule} ({@link HandlerHelper#ruleInferredOutputRType} - THE ONE rule-output
     * read). {@code null} for every non-callable head, an unresolved output, or a non-data-type output
     * (an alias-typed rule output declines - zero carriers).
     */
    private static RDataType disguisedCallableHeadOutputType(REnumValueRef evr,
            ExpressionCompiler compiler) {
        GeneratorModel gm = compiler.getGeneratorModel();
        if (gm == null) {
            return null;
        }
        RNode head = evr.resolvedHead().orElse(null);
        RType outType = null;
        if (head instanceof RFunction fn) {
            RAttribute out = fn.output().orElse(null);
            outType = out == null || out.typeCall() == null ? null
                    : gm.resolveTypeCall(out.typeCall());
        } else if (head instanceof com.regnosys.rosetta.ast.functions.RRule rule) {
            outType = HandlerHelper.ruleInferredOutputRType(rule, gm);
        }
        return outType instanceof RDataTypeRef ref ? ref.astNode() : null;
    }

    /**
     * facet ruleMetaLiftResidueSeats (seat 6): the case (a4) helper of
     * {@link #recoverMetaFromExpr} — the rule-context DEEP-PATH ({@code ->>}) leaf meta
     * read. Unwraps the same extract / element-preserving list-op shells as (a3), requires
     * a RULE context, resolves the deep feature through the gm-aware
     * {@link #resolveDeepFeature} (the SAME walk that types the render path's
     * {@code DeepPathUtil} witness — recovery and render cannot disagree) and returns the
     * leaf's value-level meta wrapper. Returns {@code null} for every non-matching shape,
     * non-rule context, unresolvable deep feature, or meta-free leaf (the decline
     * polarity — never a wrong proof).
     */
    static RJavaWithMetaValue ruleContextDeepPathLeafMeta(RExpression expr,
            ExpressionCompiler compiler) {
        RExpression e = expr;
        int hops = 0;
        while (e != null && hops++ < 8) {
            if (e instanceof RExtractExpr ext) {
                e = ext.body() == null ? null : ext.body().body();
                continue;
            }
            if (e instanceof RListOpExpr lo
                    && (lo.op() == ListOp.ONLY_ELEMENT || lo.op() == ListOp.FIRST
                        || lo.op() == ListOp.LAST || lo.op() == ListOp.DISTINCT)) {
                e = lo.argument();
                continue;
            }
            break;
        }
        if (!(e instanceof RDeepFeatureCall deep)
                || HandlerHelper.findEnclosingRule(deep) == null) {
            return null;
        }
        RAttribute deepAttr = resolveDeepFeature(deep, compiler);
        return deepAttr == null ? null : metaWrapperOf(deepAttr, compiler);
    }

    /**
     * facet valueMetaWrapCompletion (PR #328, F2a): {@code true} iff the value's
     * terminal PROVABLY produces the bare (meta-free) item — a nav-chain terminal
     * attribute ({@link #terminalNavAttr}) with {@code detectMetaKind == NONE}, a
     * direct call whose {@link RFunction} output is meta-free, or an ALIAS
     * ({@link RShortcut}) whose own body terminal proves bare (bounded recursion).
     * The polarity is the load-bearing green gate: a terminal that is META
     * (the green ConvertToAdjustableOrRelativeDate {@code FieldWithMetaDate}
     * chain — golden splices the wrapper BARE, wrapper→wrapper identity, no
     * coercion) or UNRESOLVABLE returns {@code false}, declining the caller to
     * today's bytes.
     */
    public static boolean terminalProvablyBare(RExpression e, ExpressionCompiler compiler) {
        return terminalProvablyBare(e, compiler, 0);
    }

    /** Bound on the alias-body ({@link RShortcut}) recursion in {@link #terminalProvablyBare}.
     *  Alias bodies nest shallowly in practice; a chain deeper than this is treated as
     *  UNRESOLVABLE (returns {@code false}, the decline polarity — never a wrong proof). */
    private static final int TERMINAL_BARE_DEPTH_LIMIT = 8;

    private static boolean terminalProvablyBare(RExpression e, ExpressionCompiler compiler, int depth) {
        if (e == null || compiler == null || depth > TERMINAL_BARE_DEPTH_LIMIT) {
            return false;
        }
        RAttribute terminal = terminalNavAttr(e, compiler);
        if (terminal != null) {
            return MetaFieldGenerator.detectMetaKind(terminal) == MetaFieldGenerator.MetaKind.NONE;
        }
        if (e instanceof RSymbolReference sr) {
            var sym = sr.symbol().orElse(null);
            if (sym instanceof RFunction fn) {
                return fn.output()
                        .map(o -> MetaFieldGenerator.detectMetaKind(o) == MetaFieldGenerator.MetaKind.NONE)
                        .orElse(false);
            }
            if (sym instanceof RShortcut sc) {
                return terminalProvablyBare(sc.expression(), compiler, depth + 1);
            }
        }
        return false;
    }

    /**
     * The terminal navigation attribute of {@code e} — the outermost feature whose
     * value the chain delivers — descending the element-type-preserving wrappers a
     * meta nav arg can carry: an {@link RExtractExpr} {@code map} (into its body
     * expression) and an element-type-preserving {@link RListOpExpr}
     * ({@code ONLY_ELEMENT}/{@code FIRST}/{@code LAST} single-collapse, or
     * {@code DISTINCT} dedup — PR #249, into its argument). A
     * {@link RFeatureCall} / disguised {@link REnumValueRef} / attribute
     * {@link RSymbolReference} resolves directly (the SAME resolvers
     * {@link #resolveReceiverRType} uses). Any other shape (literal, function call,
     * conditional, element-changing list-op) returns {@code null}.
     */
    // package-private (was private) since facet minMaxKeyBlockLambdaMetaDeref (PR #342):
    // CollectionHandler's MAXMIN_KEY collapsed-key arm resolves the SAME terminal the
    // #237 evaluate-arg recovery reads — one resolver, both consumers.
    static RAttribute terminalNavAttr(RExpression e, ExpressionCompiler compiler) {
        int depth = 0;
        while (e != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (e instanceof RFeatureCall fc) {
                return fc.resolvedFeature().orElseGet(() -> fallbackResolveFeature(fc, compiler));
            }
            if (e instanceof REnumValueRef evr && evr.enumeration().isEmpty()) {
                return resolveDisguisedFeature(evr, compiler, null);
            }
            if (e instanceof RSymbolReference sr) {
                return sr.symbol().filter(RAttribute.class::isInstance)
                        .map(RAttribute.class::cast).orElse(null);
            }
            if (e instanceof RExtractExpr ext) {
                e = ext.body() == null ? null : ext.body().body();
                continue;
            }
            if (e instanceof RListOpExpr lo
                    && (lo.op() == ListOp.ONLY_ELEMENT || lo.op() == ListOp.FIRST
                        || lo.op() == ListOp.LAST || lo.op() == ListOp.DISTINCT)) {
                e = lo.argument();
                continue;
            }
            return null;
        }
        return null;
    }

    /**
     * facet ruleThenValueMetaWrap (PR #264/#265, transitive recovery #270): recover the
     * value-level meta wrapper ({@link RJavaWithMetaValue} — {@code FieldWithMetaString} etc.)
     * that an inner reporting RULE's rosetta OUTPUT carries ({@code [metadata scheme]} …) but
     * which the fork's rule generator drops from the Java {@code evaluate()} signature
     * (returning the bare value).
     *
     * <p>The meta is invisible to the fork's then-inference ({@code getInferredType} reports the
     * meta-DROPPED plain value type), so it is recovered from the inner rule's OWN body via
     * {@link #recoverMetaFromExpr}. #264/#265 read the terminal navigation attribute of the last
     * navigating then-body directly; #270 generalises that to the TRANSITIVE shapes the iosco-cde
     * report rules use:
     * <ul>
     *   <li><b>bare-rule delegation</b> — a regime rule whose whole body is {@code <baseRule>}
     *       (the iosco-cde {@code v3 -> v2 -> v1} alias chain); recurse into the delegate (a pure
     *       delegate has the SAME rosetta output type, so its meta-ness equals the target's);</li>
     *   <li><b>fused cardinality list-op</b> — a terminal then-body {@code … -> identifier last}
     *       where the meta leaf is INSIDE a {@code last}/{@code only-element} (#264's whole-body
     *       {@link #isElementPreservingTailOp} skip missed it; {@link #terminalNavAttr} descends
     *       the list-op, so trying recovery on the body first recovers it).</li>
     * </ul>
     *
     * <p><b>Green-safe walk:</b> the then-chain is walked OUTERMOST-first (the rule's terminal
     * output is the last then in source). The walk skips ONLY a STANDALONE element-preserving tail
     * op (a {@code then first}/{@code then filter} over the pipe — {@link #terminalNavAttr} returns
     * null) and STOPS at the first body that IS the terminal output but recovered no meta (a bare
     * rosetta output). Continuing past a non-meta terminal would falsely recover an upstream
     * intermediate's meta — a green regression against the ~346 green goldens that carry the exact
     * fork bare form for a non-meta inner rule.
     *
     * <p>Returns the {@link RJavaWithMetaValue} or {@code null} (non-meta inner rule, or a meta
     * wrapper navigated THROUGH whose item type is not itself a wrapper). The CALLER applies the
     * value-type-equality gate (the recovered wrapper's value type must equal the consuming seat's
     * bare type) — kept at the call site so the #264 top-level seat and the #265 lambda seat each
     * verify against their own bare result type.
     */
    public static RJavaWithMetaValue recoverInnerRuleMetaWrapper(RRule innerRule,
            ExpressionCompiler compiler) {
        if (innerRule == null || compiler == null || compiler.getTypeUtil() == null) {
            return null;
        }
        Set<RNode> visited = new HashSet<>();
        visited.add(innerRule);
        return recoverMetaFromExpr(innerRule.expression().orElse(null), compiler, 0, visited);
    }

    /**
     * facet existsMetaSeats (PR #331): the EXPRESSION-level entry to the same walker —
     * used by the #265 conditional-ARM gate to apply the case-(d) all-present-arms-agree
     * JOIN to the WHOLE conditional body ({@code extract [if <cond> then <metaRule>]}):
     * an elseless meta arm keeps its wrapper (csa UnderlyingAssetTradingPlatform-
     * IdentifierLeg1/2), a ladder with a BARE terminal arm joins bare and the wrap
     * DECLINES (hkma UnderlyingAssetTradingPlatformIdentifier — golden returns the bare
     * {@code MapperS.of(<rule>.evaluate(…))} arms there).
     */
    public static RJavaWithMetaValue recoverExprMetaWrapper(RExpression expr,
            ExpressionCompiler compiler) {
        if (expr == null || compiler == null || compiler.getTypeUtil() == null) {
            return null;
        }
        return recoverMetaFromExpr(expr, compiler, 0, new HashSet<>());
    }

    /**
     * facet metaWrapperRecovery (v3.1 C2d retirement family 6, PR #613): THE ONE then-arg decl-seat
     * meta-wrapper read — the composite that replaced the PR #144 scan of the compiled value's
     * emitted-import set ({@code getRefs()}) at both decl seats (LAW 69: one helper, two callers —
     * {@code CollectionHandler.tryDeepThenHoist}'s deep-then decl and
     * {@code FunctionExpressionRenderer.renderThenExtractSet}'s then-arg decl).
     *
     * <p>{@code level} is the LEVEL'S OWN body expression ({@code base} at {@code k == 0}, else the
     * preceding then-body's lambda body) — never the whole {@code RThenExpr}, whose walk answers for
     * earlier levels. {@code value} is the level's compiled value, {@code prevRef} the receiver
     * (the preceding thenArg) and {@code bareValueType} the meta-blind inferred element.
     *
     * <p>The rules, in the order the family's C2c census measured them:
     * <ol>
     *   <li><b>the compiled item</b> — the level's OWN render stamped an {@link RJavaWithMetaValue}
     *       over {@code bareValueType} ({@code metaNavResultType}, a {@code k == 0} fused filter's
     *       receiver type, the #387/seat-30 chain stamps), SKIPPED when that item is the receiver's
     *       CARRIED type (an element-preserving level piping its receiver's wrapper is the #297
     *       {@code prevRef} rung's business, as today);</li>
     *   <li><b>an invocation level declines</b> — at an {@link RSymbolReference} level the render is
     *       the truth: an unwrapped {@code MapperS.of(rule.evaluate(…))} IS bare even where the
     *       rule's rosetta output is meta (the walker's rule-delegation case would over-fire on 4 +
     *       46 corpus arrivals);</li>
     *   <li><b>a standalone {@code default} level joins its faces</b> — the seat-14
     *       {@code defaultJoinMetaRecovery} law at a STANDALONE node: left = the walker on the
     *       explicit left, or the piped value ({@code prevRef}'s wrapper item — an elided left IS
     *       the pipe); right = the walker on the right; two recovering faces must AGREE by
     *       {@link #sameWrapperDenotation}, a SILENT face is neutral, and the survivor must be over
     *       {@code bareValueType} (rule:QuantitySchedule ×16 + fn:MessageID ×9, the homogeneous-meta
     *       defaults whose goldens KEEP the wrapper; the seat-33 law E.2 decline lock — a with-args
     *       fn-call face beside a meta NAV face — is the one-silent-face case);</li>
     *   <li><b>the walker with fused-filter descent</b> — unwrap {@code RFilterExpr} shells to their
     *       argument (a fused {@code <nav> filter […]} keeps its receiver's element, the same law
     *       the walker's then-chain case applies to a standalone {@code then filter}); a BARE
     *       {@code then filter <Fn>} level — a FUNCTION-reference predicate over the piped
     *       implicit — answers prevRef's wrapper item, the retired scan's predicate-witness
     *       class (4a, the s613a e1 catch; a plain-predicate bare filter stays with the #297
     *       rung, whose TRUE flag golden needs at Extract_BondConnect's {@code then first});
     *       a COLLAPSE-op body declines to the #297 prevRef rung, whose seven anchored
     *       disjuncts and seat-33 gates own that class (4b); else
     *       {@link #recoverExprMetaWrapper}, value-matched;</li>
     *   <li><b>the block producer's decision</b> — an {@link RExtractExpr} level whose lambda body
     *       recorded a {@code LambdaJoinFacts} typed-empty element that is a wrapper over
     *       {@code bareValueType} (the identity-arm extract {@code then extract [if … then item]},
     *       drr DTCC_TradeParty1/2ReportingDestination, where the elseless block's #362 pipe-meta
     *       upgrade typed the {@code ofNull}).</li>
     * </ol>
     *
     * <p><b>The census.</b> The composite reproduces the DECL at every arrival of both D11 routes
     * and of the optimised suite: S1 ({@code tryDeepThenHoist}) 10,046 arrivals / 1,333 scan
     * fires; the ARRIVALS classify SAME 9,354 + MASKED 663 + 25 {@code default} levels closed by
     * rule 3 + 4 invocation levels closed by rule 2, RISK 0 (round 2's print — the classification
     * sums to the 10,046). S7 ({@code renderThenExtractSet}) 22,729 arrivals / 1,241 fires, SAME
     * 22,127 + MASKED 602, OVERRIDE 0, RISK 0 (MASKED = the scan said null but a later
     * {@code prevRef} rung set exactly this answer — under the swap the rung finds the element
     * already set; identical decl). Both classifications are the census analyser's own print for
     * the channel {@code ct>w2>pb>pt}. Round 6's post-swap receipt is a DIFFERENT measurement:
     * it reaches 9,352 of the S1 arrivals past the seat's three gates and reads identity there
     * (the seat's code-quality review, SF-9 — the two denominators are never mixed).
     *
     * <p><b>Rule 3 is LOOSER than the form the census measured, deliberately.</b> The censused
     * composite required BOTH faces to recover; this one treats a silent face as neutral. No corpus
     * arrival distinguishes the two (all 25 rule-3 arrivals have both faces recovering), but a
     * REDUCED model does: {@code EnrichReportInstructionWholeSeatTest
     * .e1_e2_aNavFaceDefaultKeepsTheWrapperDecl} — the seat-33 law E.2 decline lock, a with-args
     * fn-call face beside a meta NAV face — needs the wrapper KEPT, and the strict form dropped it.
     * The loose form's corpus evidence is therefore the byte oracle at the swap commit, not the
     * census: the default-route D11 275/0F, the IR-route D11 275/0F and the optimised suite 12/0F
     * at {@code bc4b62553} ({@code target/seat613-instruments/logs/o1.status}, local).
     *
     * <p><b>The #330 determinism law (facet wobbleDeterminism), kept BY CONSTRUCTION.</b> The
     * scan this replaced iterated
     * {@code JavaExpression.getRefs()} — a {@code Set.copyOf(...)} whose iteration order is SALTED
     * per JVM launch — and picked the first match, so a body carrying TWO wrappers over one value
     * type gave a per-run random answer (the #231 wobble: EquityNotionalQuantity /
     * EquityTotalNotionalQuantity / QUoMLeg1 csa+iosco / QUoMLeg2). PR #330 de-noised it by adopting
     * a wrapper only when EXACTLY ONE distinct candidate matched. This helper iterates no set at
     * all: every rule reads a single typed fact (a compiled item, an AST walk, a producer's record),
     * so the answer cannot depend on iteration order.
     *
     * @return the wrapper the decl must keep, or {@code null} (the caller keeps the bare inferred
     *     type)
     */
    public static RJavaWithMetaValue levelMetaWrapper(RExpression level, JavaStatementBuilder value,
            JavaStatementBuilder prevRef, ExpressionCompiler compiler, JavaClass<?> bareValueType) {
        if (compiler == null || bareValueType == null) {
            return null;
        }
        JavaTypeUtil tu = compiler.getTypeUtil();
        // (1) the level's OWN compiled stamp, unless it is the receiver's carried type. The
        //     carried-type comparison is deliberately JavaClass.equals (class + package + simple
        //     name — the retired scan's own comparison), NOT sameWrapperDenotation: the
        //     canonical-name predicate DECLINES on a null canonical name, which a reduced
        //     model's wrapper can carry, so the swap-in the code-quality review's NIT-16
        //     suggested would fire this rule where the retired comparison skipped — the
        //     adjudication keeps equals and says why here instead. The argument is non-null,
        //     so equals cannot NPE (the :4942 note's case).
        if (tu != null && value != null && value.getExpressionType() != null
                && tu.getItemType(value.getExpressionType()) instanceof RJavaWithMetaValue stamped
                && stamped.getValueType().equals(bareValueType)) {
            JavaType prevItem = prevRef == null || prevRef.getExpressionType() == null ? null
                    : tu.getItemType(prevRef.getExpressionType());
            if (prevItem == null || !prevItem.equals(stamped)) {
                return stamped;
            }
        }
        // (2) an invocation level: the render is the truth.
        if (level instanceof RSymbolReference) {
            return null;
        }
        // (3) a STANDALONE `default` level joins its two faces (seat 14's law at the level node).
        if (level instanceof RDefaultExpr defaultLevel) {
            RJavaWithMetaValue leftFace = defaultLevel.left().isPresent()
                    ? recoverExprMetaWrapper(defaultLevel.left().get(), compiler)
                    : (tu != null && prevRef != null && prevRef.getExpressionType() != null
                            && tu.getItemType(prevRef.getExpressionType())
                                    instanceof RJavaWithMetaValue pipedWrapper
                                    ? pipedWrapper : null);
            RJavaWithMetaValue rightFace = defaultLevel.right().isPresent()
                    ? recoverExprMetaWrapper(defaultLevel.right().get(), compiler) : null;
            // A face that recovers NOTHING is NEUTRAL, not a veto. That is the OPPOSITE polarity
            // to this file's own `default`-join law — recoverMetaFromExpr case (a5) collapses the
            // whole join to bare when an arm recovers nothing (upstream's join of a meta and a
            // non-meta type DROPS the meta) — and the divergence is deliberate (the code-quality
            // review, SF-7): a SILENT face here is "the walker declined to type it", not "the
            // face is provably bare" (case (a5)'s structurally-absent arm and case (d)'s no-value
            // else are absence facts; an untypable face is an unknown). The provably-bare case a
            // silent-neutral join could mis-keep — an ELIDED left whose prevRef item is typed
            // PLAIN — falls out before rule 3 fires at S1 (tryDeepThenHoist's
            // defaultFaceProvablyBare gate); at S7 no such gate exists and the loose form is what
            // the byte oracle locks (see the javadoc's rule-3 paragraph: o1, both D11 routes +
            // the optimised suite). Two faces that both recover must AGREE (canonically: the
            // seat-14 sameWrapperDenotation law); one face alone carries the join, because a face
            // the walker cannot type contributes no competing wrapper to the rendered
            // `getOrDefault` — the seat-33 law E.2 decline lock (a with-args fn-call face beside
            // a meta NAV face keeps the wrapper decl); two silent faces are the seat-33 carrier's
            // own all-bare shape and decline.
            RJavaWithMetaValue joined = leftFace != null && rightFace != null
                    ? (sameWrapperDenotation(leftFace, rightFace) ? leftFace : null)
                    : (leftFace != null ? leftFace : rightFace);
            return joined != null && joined.getValueType().equals(bareValueType) ? joined : null;
        }
        // (4) the #331 all-present-arms-agree walk, descending a FUSED filter's shells.
        RExpression walked = level;
        int shells = 0;
        boolean fnPredicateShell = false;
        while (walked instanceof RFilterExpr fused && shells++ < FILTER_SHELL_DESCENT_LIMIT) {
            RExpression pred = fused.body() == null ? null : fused.body().body();
            fnPredicateShell |= pred instanceof RSymbolReference predSym
                    && predSym.symbol().orElse(null) instanceof RFunction;
            walked = fused.argument();
        }
        // (4a) a bare `then filter <Fn>` level (the shells unwrap to the piped implicit and a
        //     shell's predicate is a FUNCTION reference) answers prevRef's wrapper item,
        //     value-matched — the retired scan's own answer there: the fn predicate's
        //     evaluate-arg meta-deref hoist carried the receiver's wrapper into the compiled
        //     refs, so the scan fired and the #297 rung then DECLINED on its equals gate,
        //     leaving `prevRefFilterRecovered` FALSE. The composite answering null here made
        //     the rung fire instead — the same decl, but the flag flipped TRUE and armed the
        //     rung's disjunct-2 admit at the NEXT level, re-leaking the wrapper into a
        //     `then first` collapse decl the seat-33 e1 decline lock keeps bare
        //     (CollapseCarryPrevMetaElementSeatTest.e1, the s613a chain's one failure). A
        //     PLAIN-predicate bare filter (a comparison over navs — no deref witness, so the
        //     scan could NOT fire) stays null here exactly as the scan was null: the rung's
        //     filter disjunct answers it and the TRUE flag it leaves is load-bearing — golden
        //     drr 7.0.0 Extract_BondConnect keeps the wrapper on the `then first` decl AFTER
        //     such a filter through that very admit (the whole-cell shape control's catch when
        //     4a was first cut too wide).
        if (level instanceof RFilterExpr && fnPredicateShell
                && walked instanceof RImplicitVariable
                && tu != null && prevRef != null && prevRef.getExpressionType() != null
                && tu.getItemType(prevRef.getExpressionType())
                        instanceof RJavaWithMetaValue pipedFilterWrap
                && pipedFilterWrap.getValueType().equals(bareValueType)) {
            return pipedFilterWrap;
        }
        // (4b) a COLLAPSE-op level body (first / last / only-element / flatten / distinct /
        //     reverse) is the #297 prevRef rung's domain — its seven anchored disjuncts and the
        //     seat-33 gates decide whether the receiver's element carries over, and the walker
        //     answering here would bypass them (the e1 lock's `then first` is the decline case;
        //     the retired scan could not see the receiver's wrapper at these levels, so
        //     declining is the scan-faithful reading — on corpus the admitted class is the
        //     census's MASKED rows, the rung re-setting the identical element).
        if (walked instanceof RListOpExpr && isElementPreservingTailOp(walked)) {
            return null;
        }
        RJavaWithMetaValue recovered = recoverExprMetaWrapper(walked, compiler);
        if (recovered != null && recovered.getValueType().equals(bareValueType)) {
            return recovered;
        }
        // (5) the block producer's own typed-empty element (the identity-arm extract). The node
        //     is the extract's OWN lambda body — deliberately NOT FunctionExpressionRenderer's
        //     blockProducerNode, which additionally descends a then-PIPE shell: this rule answers
        //     for the level's own element, and a pipe's last step is a different level with its own
        //     decl. The census measured this rule's population as the identity-arm extracts alone.
        if (level instanceof RExtractExpr blockLevel && blockLevel.body() != null) {
            ExpressionCompiler.LambdaJoinFacts facts =
                    compiler.lambdaJoinFactsFor(blockLevel.body().body());
            if (facts != null && facts.typedEmptyElement() instanceof RJavaWithMetaValue typedEmpty
                    && typedEmpty.getValueType().equals(bareValueType)) {
                return typedEmpty;
            }
        }
        return null;
    }

    /** Bound on the transitive recovery recursion (delegate hops + then-body depth). The
     *  {@code visited} rule-set guards a (degenerate) delegation cycle; this is a backstop. */
    private static final int META_RECOVERY_DEPTH_LIMIT = 32;

    /** Backstop on {@link #levelMetaWrapper}'s fused-filter shell descent — a COUNT of syntactic
     *  {@code RFilterExpr} shells unwrapped, a different quantity from the recovery depth above
     *  (the code-quality review, NIT-14). */
    private static final int FILTER_SHELL_DESCENT_LIMIT = 32;

    /**
     * facet defaultJoinMetaRecovery (seat 14, PR #585): do two recovered wrappers denote the
     * SAME type? CANONICAL, never simple-name — a workspace routinely carries several types
     * sharing a simple name across namespaces (the drr 7.0.0 workspace alone declares nine
     * duplicated option simple names, the seat-13 finding), and minting a wrapper the render
     * disagrees with is precisely the failure this join must not have. Null-safe: an
     * unresolvable canonical name on either side declines.
     */
    static boolean sameWrapperDenotation(RJavaWithMetaValue a, RJavaWithMetaValue b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getCanonicalName() == null || b.getCanonicalName() == null) {
            return false;
        }
        return a.getCanonicalName().withDots().equals(b.getCanonicalName().withDots());
    }

    /**
     * facet ruleThenValueMetaWrap transitive recovery (PR #270): the recursive worker behind
     * {@link #recoverInnerRuleMetaWrapper}. See that method's Javadoc for the green-safety
     * argument; this dispatches on the inner-rule body shape.
     */
    private static RJavaWithMetaValue recoverMetaFromExpr(RExpression expr,
            ExpressionCompiler compiler, int depth, Set<RNode> visited) {
        if (expr == null || depth > META_RECOVERY_DEPTH_LIMIT) {
            return null;
        }
        // (a) direct / fused-list-op nav terminal — terminalNavAttr descends extract bodies +
        //     element-preserving cardinality list-ops, so a FUSED `… -> identifier last` recovers
        //     here even though the whole body is element-preserving (the #270 fix vs #264's
        //     whole-body skip). Null for an RThenExpr / RSymbolReference-to-rule (handled below).
        JavaType recovered = tryTerminalMetaMapperType(expr, compiler);
        if (recovered != null) {
            JavaType item = compiler.getTypeUtil().getItemType(recovered);
            if (item instanceof RJavaWithMetaValue meta) {
                return meta;
            }
        }
        // (a3) facet ruleMetaListWrap (PR #360): a RULE-context disguised 2-name leaf.
        //     terminalNavAttr's EVR arm resolves through the SHARED resolveDisguisedFeature,
        //     which is deliberately FUNCTION-gated — widening it to rules moved 12 green rule
        //     files through its naming/witness consumers (the cp2e catch; the #224/#225
        //     conversion-scope boundary). Resolve RECOVERY-LOCALLY instead: unwrap the
        //     extract / element-preserving list-op shell to the bare disguised EVR, type its
        //     implicit ITEM (the same implicitItemDataType walk the #344 arm-2 uses), head →
        //     leaf, and read the leaf's meta wrapper. Only the meta-recovery consumers see
        //     this resolution; the shared resolver and its render consumers are untouched.
        RJavaWithMetaValue ruleLeafMeta = ruleContextDisguisedLeafMeta(expr, compiler);
        if (ruleLeafMeta != null) {
            return ruleLeafMeta;
        }
        // (a4) facet ruleMetaLiftResidueSeats (seat 6): a rule-context DEEP-PATH
        //     (`->>`) terminal — the drr 7.x refactor's dominant callee shape
        //     (`NameOfTheFloatingRate from InterestRatePayout: RateOption ->> name`,
        //     a point-free func head + a deep leaf over its declared-output choice
        //     universe). The leaf resolves through the SAME gm-aware walk the render
        //     path uses (the #205 `resolveDeepFeature` behind the DeepPathUtil
        //     `chooseName` witness — the same-walk law: the render already types this
        //     leaf FieldWithMetaString, so recovery and render cannot disagree).
        //     Decline-polarity: null on an unresolvable deep feature or a meta-free
        //     leaf — never a wrong proof. Shell-unwrap mirrors (a3)'s loop so a
        //     `then extract <recv> ->> name [distinct/only-element]` step body
        //     recovers too.
        RJavaWithMetaValue deepLeafMeta = ruleContextDeepPathLeafMeta(expr, compiler);
        if (deepLeafMeta != null) {
            return deepLeafMeta;
        }
        // (a2) facet ruleMetaListWrap (PR #360): an EXTRACT wrapper whose lambda body is a
        //     THEN-CHAIN descends to that body — the extract's element IS the body's type, so
        //     the descent is type-preserving. Case (a)'s terminalNavAttr descends extract
        //     bodies only for NAV terminals; a then-chain body (`extract x [ <navs> then
        //     filter … then extract <navMeta> distinct then flatten ]` — the drr common
        //     DTCC_TradeParty1/2ReportingDestination inner rules) recovered null and the
        //     whole rule read as non-meta. A non-meta then-chain still bottoms out null
        //     through case (b)'s STOP law, so the ~346-green bare-form argument is unchanged.
        if (expr instanceof RExtractExpr extWrap && extWrap.body() != null
                && extWrap.body().body() instanceof RThenExpr wrappedChain) {
            return recoverMetaFromExpr(wrappedChain, compiler, depth + 1, visited);
        }
        // (b) then-chain — walk the bodies OUTERMOST-first; skip ONLY a standalone element-
        //     preserving tail op; STOP at the first terminal-output body that recovered no meta.
        if (expr instanceof RThenExpr) {
            List<RExpression> bodies = new ArrayList<>();
            RExpression cur = expr;
            while (cur instanceof RThenExpr then) {
                bodies.add(then.body().map(RInlineFunction::body).orElse(null));
                cur = then.argument();
            }
            bodies.add(cur);
            // facet defaultJoinMetaRecovery (seat 14, PR #585): the pending `default` JOIN.
            // Upstream types a `default` from the JOIN of its operands' META-ANNOTATED types
            // (ExpressionGenerator.xtend:452-455 — `leftRMetaType.joinMetaAnnotatedTypes(
            // rightRMetaType)`), so `default` is META-KEEPING exactly when its arms agree. A
            // `then default X` chain reaches this walker as a then-chain whose BODIES are
            // RDefaultExpr nodes with an ELIDED left (the piped value); each such body is one
            // ARM of the join, and the chain's head (the innermost argument) is the first arm.
            // Null while no arm has been seen — then the loop below is byte-identical to the
            // pre-seat walk for every chain that contains no `default`.
            RJavaWithMetaValue joinWrapper = null;
            for (RExpression body : bodies) {
                // (a5) a JOIN ARM: recover the arm's own explicit operand and KEEP WALKING —
                // the remaining bodies (and the chain head) are the other arms. A bare arm
                // collapses the whole join to bare (upstream's join of a meta and a non-meta
                // type drops the meta), and two arms carrying DIFFERENT wrappers likewise.
                // The agreement test is CANONICAL, never simple-name: a same-simple-name
                // wrapper from another namespace is a different denotation and must decline
                // (the seat suite's b5 adversarial pin registers exactly that decoy first).
                // The ELIDED left is a PRECONDITION of the model below, not an observation: with
                // an elided left the piped value IS the left operand, so the chain's head is the
                // join's other arm. An EXPLICIT left (`then <A> default <B>`) joins A with B and
                // the head is NOT an arm — a different shape this arm must decline rather than
                // mis-model. Measured over all 25 cells: the arm fires 200 times and the left is
                // elided in 100% of them, so the guard is hardening, not a behaviour change.
                if (body instanceof RDefaultExpr defArm && defArm.left().isEmpty()) {
                    RExpression armExpr = defArm.right().orElse(null);
                    RJavaWithMetaValue armMeta = armExpr == null ? null
                            : recoverMetaFromExpr(armExpr, compiler, depth + 1, visited);
                    if (armMeta == null) {
                        return null;
                    }
                    if (joinWrapper == null) {
                        joinWrapper = armMeta;
                    } else if (!sameWrapperDenotation(joinWrapper, armMeta)) {
                        return null;
                    }
                    continue;
                }
                RJavaWithMetaValue m = recoverMetaFromExpr(body, compiler, depth + 1, visited);
                if (m != null) {
                    // With a join pending, this body is the chain HEAD — the join's first arm
                    // — so it must agree with the arms already seen rather than win outright.
                    return joinWrapper == null || sameWrapperDenotation(joinWrapper, m)
                            ? m : null;
                }
                // NOTE (seat 14): a body that recovered nothing is NOT decided here even with a
                // join pending. It falls through to the SAME skip/stop logic below, which is
                // already correct for both states — an element-preserving step (`then first`/
                // `then filter`/`then distinct`) must be SKIPPED whether or not a `default` arm
                // has been seen (it does not change the join's element), and any other body that
                // recovered nothing returns null either way: as the rule's bare terminal output
                // when no join is pending, or as a bare JOIN ARM when one is. An earlier draft
                // returned null here on `joinWrapper != null` and so mis-declined
                // `<nav> then filter … then default <meta>` — the bodies arrive OUTERMOST-first,
                // so the preserving step is visited AFTER the default arm.
                // Skip ONLY a STANDALONE element-preserving op (a `then first`/`then filter`/
                // `then flatten` over the pipe — terminalNavAttr null); STOP at any other body
                // (the rule's terminal output) that recovered no meta. The skip's green-safety
                // relies on terminalNavAttr's descend-set (ONLY_ELEMENT/FIRST/LAST/DISTINCT)
                // covering the navigating ops isElementPreservingTailOp admits.
                boolean standalonePreserving = body != null
                        && isElementPreservingTailOp(body)
                        && terminalNavAttr(body, compiler) == null;
                // Defensive hardening (PR #272; Copilot #270 R1 — latent, no corpus carrier): a
                // FUSED `nav flatten`/`nav reverse` terminal body ALSO has a null terminalNavAttr
                // (the descend-set excludes FLATTEN/REVERSE), so the skip above would mis-treat it
                // as standalone-preserving and wrongly continue past the rule's terminal output.
                // Restrict the FLATTEN/REVERSE skip to a TRUE `then flatten`/`then reverse` whose
                // operand is the implicit piped value (the `f.argument() instanceof RImplicitVariable`
                // pattern, L515); a fused nav STOPS. Byte-neutral (regscan 0 new mismatches — no
                // flatten/reverse meta carrier exists); forecloses the latent mis-recovery.
                if (standalonePreserving && body instanceof RListOpExpr lo
                        && (lo.op() == ListOp.FLATTEN || lo.op() == ListOp.REVERSE)
                        && !(lo.argument() instanceof RImplicitVariable)) {
                    standalonePreserving = false;
                }
                // facet ruleMetaListWrap (PR #360): an IDENTITY-arm conditional extract step
                // (`then extract [if <cond> then item]`, elseless — absent or the P358I
                // materialized-empty else, or an explicit `else empty`) is a FILTER idiom:
                // each element maps to ITSELF or drops, so the pipe's element type flows
                // through unchanged and the walk may continue to the real terminal (the drr
                // common DTCC_TradeParty1/2ReportingDestination `supervisoryBody distinct …
                // then extract [if … then item] then distinct` tail). Any arm OTHER than the
                // bare implicit item keeps the STOP law (a value-producing terminal).
                if (!standalonePreserving && body instanceof RExtractExpr identExt
                        && identExt.body() != null
                        && identExt.body().body() instanceof RConditionalExpr identCond
                        && identCond.thenBranch() instanceof RImplicitVariable
                        && (identCond.elseBranch().isEmpty()
                                || identCond.elseBranch().get() instanceof REmptyLiteral
                                || (identCond.elseBranch().get() instanceof RListLiteral el
                                        && el.elements().isEmpty()))) {
                    standalonePreserving = true;
                }
                if (!standalonePreserving) {
                    return null;
                }
            }
            return null;
        }
        // (c) bare rule reference — recurse into the delegated inner rule (the iosco-cde
        //     v3 -> v2 -> v1 alias chain; a regime rule whose whole body is `<baseRule>`).
        // facet wrapperLadderKeepsCollapse (seat 24, law F28): a `[<meta-nav first>, …] only-element`
        // collapse arm resolves to the one wrapper every element navigates to — THE SAME predicate the
        // output-deref rung consults (LiteralHandler.listLiteralNavMetaWrapper, LAW 69), so a ladder
        // whose last arm is that collapse joins to the wrapper (golden mas UnderlyingRule's output deref).
        if (expr instanceof RListOpExpr litCollapse && litCollapse.op() == ListOp.ONLY_ELEMENT
                && litCollapse.argument() instanceof RListLiteral litArg) {
            return LiteralHandler.listLiteralNavMetaWrapper(litArg, compiler);
        }
        // facet ruleCallArmMetaWrap (seat 25, law B): a WITH-ARGS rule call recovers like
        // the bare reference — the args select the callee's INPUT, never its output meta
        // (the NotionalCurrencyOfLeg2 arm calls `CDEInterestRateNotionalCurrency(<argChain>)`;
        // without this the #330 arms-agree JOIN read those arms as bare and the ladder
        // declined). And the {@code visited} guard is STACK-scoped, not walk-scoped: two
        // SIBLING arms of one ladder may call the SAME rule (NotionalCurrencyOfLeg2's arms
        // 0 and 4 both call CDEInterestRateNotionalCurrency — the seat-25 P25F probe read
        // arm 4 as null under the walk-scoped set and the whole join collapsed), so the
        // target leaves the set when its recursion unwinds; a genuine delegation CYCLE is
        // still on the stack and still declines. Green-safety (the review's B-1 correction):
        // monotonicity is a property of the WALKER — this case and its recursive callers
        // (case (b)'s then-chain/default join, case (d)'s all-arms-agree join) can only go
        // null -> non-null — but NOT of every CONSUMER: at least three pre-existing reads
        // have INVERTED polarity and no value-type gate (the ReferenceHandler seat-8
        // with-args MapperC wrap gated on `recoverInnerRuleMetaWrapper(...) == null`;
        // CollectionHandler.isProvablyBareInvokableArm; ConversionHandler's
        // `recoverExprMetaWrapper(argument) == null` read), so a NEW recovery flips them
        // from FIRE to DECLINE. The green-safety evidence is therefore MEASURED, not
        // structural: the seat-25 LAW-80 accounting read ZERO ENTERED files over the full
        // 25-cell matrix on BOTH routes, with the rings EXACT. A non-rule symbol falls
        // through unchanged.
        if (expr instanceof RSymbolReference sr) {
            RRule target = sr.symbol().filter(RRule.class::isInstance)
                    .map(RRule.class::cast).orElse(null);
            if (target != null && visited.add(target)) {
                try {
                    return recoverMetaFromExpr(target.expression().orElse(null),
                            compiler, depth + 1, visited);
                } finally {
                    visited.remove(target);
                }
            }
            // v3.2 seat 5 (PR #626, F6): an ALIAS reference recovers through the alias BODY — the with-meta
            // argument's own meta type is the body's (golden ViaAlias: `alias src: h -> coded` / `src with-meta
            // {…}` hoists the wrapper's builder). Rules above, shortcuts here, each guarded by the SAME visited set
            // (round 1, the code-quality review's NIT-7: a mutual `alias a: b` / `alias b: a` is bounded by the set,
            // not by the depth limit alone). THE GREEN-SAFETY CLAIM IS MEASURED, NOT STRUCTURAL (the review's SF-5):
            // this walker feeds some forty-five consumers, three of them with INVERTED polarity (the seat-25 note
            // below), so "the walker returned null here before" does not by itself prove that no consumer moved; the
            // measurement is EVERY chain of record since the arm landed - the PR body names the current one (round 2,
            // the code-quality review's SF-4: a run id here goes stale with each chain; the invariant does not): the
            // FULL 26-cell matrix IDENTICAL per route, the 25 vendored cells' matrix `d47fa88f` and rings `9062fc14`
            // UNMOVED on both routes - the LAW-80 accounting every fix seat carries (docs/user/testing.md § 7).
            RShortcut alias = sr.symbol().filter(RShortcut.class::isInstance)
                    .map(RShortcut.class::cast).orElse(null);
            if (alias != null && visited.add(alias)) {
                try {
                    return recoverMetaFromExpr(alias.expression(), compiler, depth + 1, visited);
                } finally {
                    visited.remove(alias);
                }
            }
        }
        // v3.2 seat 12 (D52, H2 - M8a the meta wrapper into a rule's plain output): a FUNCTION call - an
        //     RSymbolReference with arguments whose symbol is an RFunction - recovers the CALLEE's declared
        //     OUTPUT attribute's meta wrapper (`extract VenueOf(item)` where VenueOf returns `string [metadata
        //     scheme]`: FieldWithMetaString) through the ONE attribute -> wrapper resolution the navigation
        //     terminal reads (metaWrapperOf, behind metaNavResultType). The SET seat then hoists and unwraps it -
        //     single: renderMetaValueDerefOrNull's deref block; multi: the "Type coercion" map - the released
        //     plugin's form (the hold-out group rule-meta-output-unwrap, pinned before this code). A RULE callee
        //     keeps the with-args rule case above; a callee with a plain output recovers null as before; a
        //     zero-argument call is NOT admitted (no carrier - the shape stays as it was). This walker's consumers
        //     with INVERTED polarity (the seat-25 note above) are MEASURED by the chain's rings and the 26-cell
        //     matrix, never argued green.
        if (expr instanceof RSymbolReference fnCall && !fnCall.args().isEmpty()) {
            RFunction callee = fnCall.symbol().filter(RFunction.class::isInstance)
                    .map(RFunction.class::cast).orElse(null);
            RAttribute calleeOut = callee == null ? null : callee.output().orElse(null);
            RJavaWithMetaValue calleeMeta = calleeOut == null ? null : metaWrapperOf(calleeOut, compiler);
            if (calleeMeta != null) {
                return calleeMeta;
            }
        }
        // (d) facet lcuConditionalArmMeta (PR #292): an if-then-else EXPRESSION body whose
        //     arms navigate to a meta-typed output — recover the wrapper from the first arm that
        //     yields one. The FLIP CARRIER is the hkma SwapLinkID rule, whose delegated inner rule
        //     `common.link.SwapLinkID` body is the ELSELESS conditional `if IsFXSwap(ProductForEvent)
        //     then cde.link.PackageIdentifier` (a meta-typed sub-rule); the #265 lambda-seat wrap
        //     (recoverInnerRuleMetaWrapper) consumed it but recoverMetaFromExpr declined (no
        //     conditional case → null), so golden's `MapperS.of(FieldWithMetaString.builder()…)` wrap
        //     + output deref never fired and the fork assigned the bare String output (NON_COMPILING).
        //     The QuantityUnitOfMeasureLeg2 (csa) producer is the WITH-ELSE sibling — `then extract
        //     [ if cond then <nav -> quantitySchedule> else if … then <nav -> quantitySchedule> ]`
        //     whose arms both navigate to the same `quantitySchedule` ReferenceWithMeta leaf — and is
        //     the DECLINE LOCK: case (d) DOES recover its arm meta, but the consumer's `item.get()`
        //     compiles to a NON-null bare type, so the #285 LAMBDA-channel gate (implicitItemArgMeta,
        //     reached only when the compiled type is null) is never hit → the deref declines and the
        //     carrier stays divergent.
        //     MONOTONE / add-only: the walker previously returned null here, so this only RECOVERS
        //     meta where there was none — never changes an existing recovery. The "arms navigate to
        //     the same meta" assumption is load-bearing (rune joins the arms to ONE output type, so a
        //     meta arm + a bare arm join to the bare base — golden would not deref a mixed conditional);
        //     returning the first arm's meta is correct for the elseless / both-arms-meta carriers and
        //     defended for the (no-current-carrier) mixed shape by the downstream gate
        //     (tryMetaDerefArg's value-type-equality + callee-param-mismatch + real-coercion checks
        //     decline when the recovered wrapper's value type does not match the consumer's). The
        //     nested else-if ladder recurses through elseBranch (an RConditionalExpr).
        //     facet noEmitMetaDelegation (PR #330) TIGHTENING: first-arm-wins became an
        //     over-fire once case (e) below exposed DEEPER conditional arms (the extract-
        //     wrapped ladders): a MIXED conditional (a meta arm + a bare arm) joins to the
        //     BARE base in rune — golden does NOT wrap — so recovery must require every
        //     PRESENT arm to recover the SAME wrapper (the un-gated first cut regressed 4
        //     GREEN files, Counterparty2Rule asic first, caught by the D11 strict gate).
        //     A "no value" else (absent / `else empty` REmptyLiteral / the parser's
        //     SYNTHESIZED empty RListLiteral — the #328 isNoValueElse triple, mirrored
        //     from ConstructionHandler) is NEUTRAL: the join keeps the then-arm's meta
        //     (the #292 SwapLinkID elseless carrier). A nested else-if ladder recurses
        //     through elseBranch, so arm agreement composes transitively.
        if (expr instanceof RConditionalExpr cond) {
            RJavaWithMetaValue thenMeta =
                    recoverMetaFromExpr(cond.thenBranch(), compiler, depth + 1, visited);
            RExpression elseB = cond.elseBranch().orElse(null);
            boolean noValueElse = elseB == null || elseB instanceof REmptyLiteral
                    || (elseB instanceof RListLiteral l && l.elements().isEmpty());
            if (noValueElse) {
                return thenMeta;
            }
            RJavaWithMetaValue elseMeta =
                    recoverMetaFromExpr(elseB, compiler, depth + 1, visited);
            // Both non-null BEFORE equals — JavaClass.equals(null) NPEs (it reads
            // object.getClass() unguarded; the upstream runtime class is vendored).
            if (thenMeta != null && elseMeta != null && thenMeta.equals(elseMeta)) {
                return thenMeta;
            }
            return null;
        }
        // (e) facet noEmitMetaDelegation (PR #330): an EXTRACT wrapper — a then-body
        //     `then extract [ <body> ]` parses as an RExtractExpr whose INLINE body carries
        //     the real shape. Case (a)'s terminalNavAttr descends extract bodies only for
        //     plain NAV terminals and bails (null) on a CONDITIONAL body, so the iosco-cde v1
        //     CustomBasketCode terminal — `then extract [ if … then <nav -> identifier first>
        //     else if … ]` — never reached case (d). Descend to the inline body so the
        //     conditional-arm recovery applies. Carriers: the regime CustomBasketCode ×5 +
        //     DTCC_DeliveryLocation delegators (the #318-blocked recovery, unblocked by the
        //     #330 inference fixes that made these rules emit at all). MONOTONE add-only:
        //     this arm only runs where every prior case returned null, and descending an
        //     extract cannot reach a leaf the extract's own value would not produce (the
        //     extract's element type IS its body's type); the mixed-arm/non-meta shapes
        //     still return null through case (d)'s existing law + the callers' value-type
        //     equality gates.
        if (expr instanceof RExtractExpr ext) {
            return recoverMetaFromExpr(ext.body() == null ? null : ext.body().body(),
                    compiler, depth + 1, visited);
        }
        // (f) facet noEmitMetaDelegation (PR #330): a DISGUISED 2-name chain
        //     (`assignedIdentifier -> identifier`, an REnumValueRef with an empty
        //     enumeration()) as the meta terminal — the recurring #286/#288/#290
        //     blindness at this walker: case (a)'s terminalNavAttr resolves a disguised
        //     chain only against the enclosing FUNCTION's input/output/shortcut names
        //     (null inside a rule body). The parser's Category-10 binding already
        //     carries the chain — feature() IS the leaf attribute — so read its
        //     [metadata …] wrapper directly (the #291 resolvedAttributeChain
        //     consumption pattern). Null for a bare leaf (metaWrapperOf) — the
        //     common DTCC_DeliveryLocation `then extract assignedIdentifier ->
        //     identifier` terminal recovers FieldWithMetaString; a non-meta leaf
        //     keeps the decline.
        if (expr instanceof REnumValueRef evr && evr.enumeration().isEmpty()) {
            RAttribute leaf = evr.resolvedAttributeChain()
                    .map(c -> c.feature()).orElse(null);
            if (leaf != null) {
                return metaWrapperOf(leaf, compiler);
            }
        }
        return null;
    }

    /**
     * facet ruleThenValueMetaWrap (PR #264/#265): a then-body that PRESERVES the piped element
     * type — a cardinality/dedup list-op ({@code first}/{@code last}/{@code only-element}/
     * {@code flatten}/{@code distinct}/{@code reverse}) or a {@code filter}. Such trailing bodies
     * do not change the inner rule's output element type, so the meta wrapper is determined by the
     * body BEFORE them — they are skipped by {@link #recoverInnerRuleMetaWrapper} when walking the
     * inner rule's then-chain to its meta terminal. ({@code sort} is an {@code RSortExpr}, not an
     * {@link RListOpExpr}, and never reaches here; {@code sum} is an element-CHANGING aggregate and
     * is deliberately excluded.)
     */
    public static boolean isElementPreservingTailOp(RExpression body) {
        if (body instanceof RFilterExpr) {
            return true;
        }
        if (body instanceof RListOpExpr lo) {
            return lo.op() == ListOp.FIRST || lo.op() == ListOp.LAST
                    || lo.op() == ListOp.ONLY_ELEMENT || lo.op() == ListOp.FLATTEN
                    || lo.op() == ListOp.DISTINCT || lo.op() == ListOp.REVERSE;
        }
        return false;
    }

    /**
     * Determines whether to use {@code map} or {@code mapC} for a feature call.
     *
     * <p>Multi-valued features (upper bound greater than 1 or unbounded) use
     * {@code mapC}. Single-valued features (upper bound of 0 or 1) use
     * {@code map}. When the feature's resolvedFeature is missing, a best-effort
     * fallback resolution walks the receiver chain — routed through the gm-aware
     * {@link #fallbackResolveFeature(RFeatureCall, ExpressionCompiler)} (facet
     * choice_nav_chain_typing) so a feature DOWNSTREAM of a choice-option step
     * resolves its cardinality from the same {@link RAttribute} the {@code <Type>}
     * witness derives from ({@code economicTerms -> payout -> SettlementPayout ->
     * settlementTerms -> cashSettlementTerms} lost the leaf's {@code mapC} on the
     * choice-blind static path). When cardinality information is unavailable,
     * defaults to {@code map}.
     */
    private String resolveMapMethod(RFeatureCall expr, ExpressionCompiler compiler) {
        if (expr.resolvedFeature().isPresent()) {
            return expr.resolvedFeature()
                    .flatMap(RAttribute::cardinality)
                    .map(NavigationHandler::isMultiValued)
                    .orElse(false)
                    ? "mapC" : "map";
        }
        RAttribute fallback = fallbackResolveFeature(expr, compiler);
        if (fallback != null) {
            return fallback.cardinality()
                    .map(NavigationHandler::isMultiValued)
                    .orElse(false)
                    ? "mapC" : "map";
        }
        return "map";
    }

    /**
     * Whether the RECEIVER chain of a navigation step renders a {@code MapperC} at
     * runtime — i.e. whether any step upstream of the current one is multi-valued
     * and not collapsed back to a single item. Once a chain passes through a
     * {@code mapC} step, every later {@code .map(...)} keeps the {@code MapperC}
     * wrapper (mapping a list item-wise yields a list), so the wrapper kind of a
     * step's RESULT is chain-aware, not step-local (facet choice_nav_chain_typing;
     * consumed by {@link #metaNavResultType}'s multi flag).
     *
     * <p>NOTE (facet then_maxmin_item_typing): {@code resolveReceiverDataType}
     * gained element-type-transparent receiver arms (max/min/filter/then/
     * first/last) that this predicate deliberately does NOT mirror — it stays
     * conservative-single off those receivers (waivered-space only; a future
     * then-family facet may add the matching arms). Otherwise shapes mirror
     * {@link #resolveReceiverDataType(RExpression, ExpressionCompiler)}'s
     * walk:
     * <ul>
     *   <li>{@link RFeatureCall} — multi if THIS step is {@code mapC}
     *       ({@link #resolveMapMethod}, the same gm-aware resolution that emits the
     *       step's text, so the reading and the rendered chain cannot disagree) or
     *       any step upstream of it is;</li>
     *   <li>disguised {@link REnumValueRef} navigation — multi if the disguised
     *       ROOT input/output renders the {@code MapperC.<Item>of(...)} wrap
     *       ({@code ReferenceHandler.mapperCOfWrapWitness}, the same predicate
     *       that renders it — facet meta_coercion_numbering guard-kind sub-arm)
     *       OR the resolved leaf feature's own cardinality is multi;</li>
     *   <li>{@link RSymbolReference} to an {@link RAttribute} — the symbol's declared
     *       cardinality (a bare multi value wraps as {@code MapperC.<Item>of(x)},
     *       PR #156 — NOTE: that wrap is conditional (meta-free + {@code gm.isMulti} +
     *       type resolves), so a META-annotated multi bare receiver still renders the
     *       legacy {@code MapperS.of} and this arm would over-report MapperC for it;
     *       bytes-relevant only when the current step is ALSO meta, a shape with no
     *       byte-diverging corpus instance today (stash-baseline 0, D11 20/20) — align
     *       this predicate with {@code tryMultiValueWrap}'s when the deferred
     *       meta-annotated-multi facet lands);</li>
     *   <li>{@code only-element} — single by construction: the {@code .get()} collapse
     *       re-wraps as {@code MapperS.of(...)}, restarting the chain scalar.</li>
     * </ul>
     * Any other receiver shape reads conservatively single (the step-local behavior
     * this facet replaces), so an unrecognised chain keeps its current bytes.
     */
    /**
     * facet ruleMidChainCondLambdaAdmit (PR #392): true iff {@code receiver} is a BOUND
     * then-pipe implicit whose binding ({@code JavaStatementScope.thenArgRefFor} on the
     * nearest enclosing {@link RInlineFunction} — the #362 compiled-type channel, never
     * an AST walk) carries a {@code MapperC}. Consumed by the wrap-kind read at the
     * feature-nav meta stamp: a MapperC-piped rung condition's meta deref is the BARE
     * non-registering form (the #310/#349-S2 law). A receiver that is not the implicit,
     * an unbound lambda (a filter predicate owns its own item channel), or a MapperS
     * binding reads {@code false} — today's bytes.
     */
    private static boolean boundImplicitPipeIsMapperC(RExpression receiver,
            ExpressionContext ctx, ExpressionCompiler compiler) {
        if (!(receiver instanceof com.regnosys.rosetta.ast.expressions.references
                        .RImplicitVariable)
                || ctx == null || ctx.scope() == null
                || compiler == null || compiler.getTypeUtil() == null) {
            return false;
        }
        RNode cur = receiver.parent();
        int depth = 0;
        while (cur != null && depth++ < 64) {
            if (cur instanceof RInlineFunction inline) {
                // facet thenBindingOverlay (v3.1 C2d retirement family 9, PR #616) — VERDICT-MOVED
                // RETIRE-AFTER-CENSUS -> JUSTIFIED-KEPT (the seat-30 bar; S31): the triage's
                // cardinality channel is refuted in BOTH directions, and the deliberate NON-OVERLAP
                // the javadoc above asserts is now MEASURED. c9 census, 10,519 arrivals (3,531 /
                // 3,531 / 3,457), all phase=read: bound=true at 729 (a 6.93% join rate, the
                // family's lowest), isMapperC=true at 168 (56 per walk). The sibling channel
                // lambdaParamBindsListItem is FALSE at 10,519/10,519 — the two channels NEVER both
                // answer, exactly as asserted, so the rung-R3 route cannot serve this seat
                // (agreement 98.40%, T/F 168). The triage's enclosingThenArgCardinality, sized:
                // SINGLE 7,541 / MULTI 2,573 / noThen 405 — of the 168 MapperC arrivals only 54 are
                // MULTI, so the channel MISSES 67.9% and would OVER-FIRE at 2,519. Locator drift
                // recorded: the triage's L4930 is live at 5103-5112.
                JavaExpression bound = ctx.scope().thenArgRefFor(inline);
                JavaType bt = bound == null ? null : bound.getExpressionType();
                return bt != null && compiler.getTypeUtil().isMapperC(bt);
            }
            if (cur instanceof RFunction
                    || cur instanceof com.regnosys.rosetta.ast.functions.RRule) {
                return false;
            }
            cur = cur.parent();
        }
        return false;
    }

    private boolean chainRendersMapperC(RExpression e, ExpressionCompiler compiler) {
        return chainRendersMapperC(e, compiler, null);
    }

    /**
     * v3.1 flip seat 28 - facet chainMapperCRootRungs. The scope-carrying overload: the
     * {@code scope} feeds rung R3 ONLY (the LIST-consuming lambda's implicit item, whose
     * binding kind lives on the render scope). A {@code null} scope - the 2-arg overload
     * above and every compiler-less caller - keeps the pre-facet behaviour exactly.
     */
    private boolean chainRendersMapperC(RExpression e, ExpressionCompiler compiler,
            JavaStatementScope scope) {
        if (e instanceof RFeatureCall fc) {
            if ("mapC".equals(resolveMapMethod(fc, compiler))) {
                return true;
            }
            return chainRendersMapperC(fc.receiver(), compiler, scope);
        }
        // facet chainMapperCRootRungs, rung R1: a DEEP-arrow (->>) step is an
        // RDeepFeatureCall, a shape this walk had no arm for at all, so a chain that
        // CROSSES a deep hop lost its whole upstream wrapper history and fell to the
        // conservative-single tail. Read the hop's member from the SAME two calls
        // handle(RDeepFeatureCall) makes to RENDER it (resolveDeepFeature +
        // resolveDeepMapMethod - the #348 lockstep law), so the kind read here and the
        // emitted `.mapC(` cannot disagree; otherwise recurse into the deep call's
        // receiver exactly like the RFeatureCall arm. An unresolvable deep feature
        // reads "map" (resolveDeepMapMethod's null contract) and keeps the recursion -
        // monotone, never a widening. Golden drr 7.x GetUnderlierIDForIndex:
        // `.<AssetIdentifier>mapC("chooseIdentifier", ...).<FieldWithMetaString>map(...)
        // .<String>map("Type coercion", fieldWithMetaString -> fieldWithMetaString.getValue())`
        // - the BARE non-registering form (the #310/#349-S2 law). MEASURED population:
        // 4 lines corpus-wide, all 4 band, ZERO green (PROBE28-F14a rootKind=RDeepFeatureCall).
        if (e instanceof RDeepFeatureCall dfc) {
            if ("mapC".equals(resolveDeepMapMethod(resolveDeepFeature(dfc, compiler)))) {
                return true;
            }
            return chainRendersMapperC(dfc.receiver(), compiler, scope);
        }
        // facet chainMapperCRootRungs, rung R3: the implicit item of a LIST-CONSUMING
        // lambda is bound by the runtime as the inner MapperC<T> (filterListNullSafe /
        // mapListToItem / mapListToList all take Function<MapperC<T>, ...>), so every hop
        // off it rides MapperC and its meta coercion is the BARE non-registering form.
        // CollectionHandler.lambdaParamBindsListItem is the SAME method-selection SOT the
        // render halves consult to EMIT `filterListNullSafe` (LAW 69: the two halves
        // cannot disagree). boundImplicitPipeIsMapperC deliberately declines this shape
        // (an unbound filter predicate has no thenArgRefFor binding) and only ever sees
        // the OUTERMOST receiver, so the answer has to arrive inside the recursion -
        // hence the threaded scope. Golden drr 7.x Extract_TradingCapacity:
        // `.filterListNullSafe(item -> areEqual(item.<ReferenceWithMetaParty>map(
        // "getPartyReference", ...).<Party>map("Type coercion", _referenceWithMetaParty ->
        // _referenceWithMetaParty.getValue()), ...))`. Monotone (only ADDs true); dormant
        // for every null-scope caller. The enclosing-lambda walk stops at a FUNCTION or
        // RULE boundary exactly like boundImplicitPipeIsMapperC's.
        if (scope != null
                && e instanceof com.regnosys.rosetta.ast.expressions.references.RImplicitVariable) {
            RNode r3Cur = e.parent();
            int r3Depth = 0;
            while (r3Cur != null && r3Depth++ < 64) {
                if (r3Cur instanceof RInlineFunction r3Owner) {
                    if (CollectionHandler.lambdaParamBindsListItem(r3Owner, compiler, scope)) {
                        return true;
                    }
                    break;
                }
                if (r3Cur instanceof RFunction
                        || r3Cur instanceof com.regnosys.rosetta.ast.functions.RRule) {
                    break;
                }
                r3Cur = r3Cur.parent();
            }
        }
        // facet ruleMidChainCondLambdaAdmit (PR #392): a WITH-ARGS function-call
        // receiver renders `MapperC.<Out>of(<callee>.evaluate(…))` when the callee's
        // declared output is MULTI (the #333 wrap law — the same cardinality the
        // invocation render reads), so the chain rides MapperC and its meta coercions
        // are BARE non-registering (golden UAPSL1's
        // `GetUnderlierProductIdentifierLeg1(…) -> identifier` comparand:
        // `.<String>map("Type coercion", fieldWithMetaString ->
        // fieldWithMetaString.getValue())`, unguarded — the #310/#349-S2 law).
        // MONOTONE: a single-output or unresolvable callee keeps the
        // conservative-single read (today's bytes).
        if (e instanceof RSymbolReference fnCallRef
                && !fnCallRef.args().isEmpty()
                && fnCallRef.symbol().orElse(null) instanceof RFunction fnCallCallee
                && compiler != null && compiler.getGeneratorModel() != null) {
            RAttribute fnCallOut = fnCallCallee.output().orElse(null);
            if (fnCallOut != null && compiler.getGeneratorModel().isMulti(fnCallOut)) {
                return true;
            }
        }
        if (e instanceof REnumValueRef evr && evr.enumeration().isEmpty()) {
            // facet meta_coercion_numbering (guard-kind sub-arm): the disguised
            // chain's ROOT renders `MapperC.<Item>of(<input>)` when the root
            // input/output attribute passes the multi-value wrap's gm-aware
            // predicate (ReferenceHandler.mapperCOfWrapWitness — the SAME
            // ladder tryMultiValueWrap renders the wrap with, so reading and
            // rendering cannot disagree). A plural-input-rooted disguise
            // (`interestRatePayouts -> priceQuantity`, cdm
            // InterestRatePayoutCurrency) therefore carries MapperC down the
            // whole chain, and its meta coercions are BARE — the leaf-only
            // cardinality read below classified it MapperS and emitted a
            // spurious null-guard the golden does not carry (trace-pinned
            // before this arm landed: the a.md hypothesis blamed the
            // bare-SYMBOL arm; the actual carrier is this disguised shape).
            RAttribute root = resolveDisguisedRootAttribute(evr);
            if (root != null
                    && ReferenceHandler.mapperCOfWrapWitness(root, compiler) != null) {
                return true;
            }
            // facet interior_position_coercion: a disguised ALIAS root
            // (`interestRatePayouts -> rateSpecification` where the root name is a
            // shortcut, not an input — resolveDisguisedRootAttribute returns null
            // for exactly this hop) reads its wrapper kind from the SAME
            // FunctionAliasHelper walk that renders the alias method's
            // MapperS/MapperC signature into the same file, so the kind read here
            // and the rendered chain cannot disagree. MONOTONE: only a
            // provably-multi alias widens to MapperC; a single or declined alias
            // falls through to the leaf-cardinality read below (a single alias
            // whose LEAF feature is multi still rides MapperC via that arm).
            if (Boolean.TRUE.equals(disguisedAliasRootIsMulti(evr, compiler))) {
                return true;
            }
            RAttribute attr = resolveDisguisedFeature(evr);
            if (attr != null) {
                return attr.cardinality().map(NavigationHandler::isMultiValued).orElse(false);
            }
            // facet itemRootDisguiseMapperC (PR #344): the FUNCTION-lambda siblings of the
            // #310 rule arm below, now that the #344 navWalkChoiceDisguise arms resolve
            // these roots. (a) When the STATIC resolver above declines (alias / choice /
            // item-feature roots), read the leaf through the COMPILER-carrying resolver —
            // the SAME walk the witness renders from — and take its cardinality: a
            // disguised alias-root chain to a MULTI leaf (`businessEvent -> instruction`)
            // rides MapperC, so its meta coercions are BARE (golden
            // `referenceWithMetaTradeState.getValue()`, drr BeforeTradeForEvent).
            // (b) A disguised chain rooted on a MULTI implicit-ITEM feature
            // (`quantity -> unit -> currency` — `quantity` a 0..* feature of the item
            // type PriceQuantity) renders `item.<FWM…>mapC("getQuantity", …)`, so the
            // chain rides MapperC regardless of its leaf's own cardinality (cdm
            // FindMatchingIndexTransitionInstruction: golden's plain unguarded
            // `fieldWithMetaString -> fieldWithMetaString.getValue()`; the single-headed
            // observable coercions in the SAME carriers keep the guard golden also
            // carries). Green-safe like #310: a guard-vs-bare divergence on a
            // provably-multi chain is already a waivered mismatch — the fork guards,
            // golden bares; a single truth is never widened (exact cardinality reads).
            if (compiler != null && HandlerHelper.findEnclosingFunction(evr) != null) {
                RAttribute gmLeaf = resolveDisguisedFeature(evr, compiler, null);
                if (gmLeaf != null
                        && gmLeaf.cardinality().map(NavigationHandler::isMultiValued).orElse(false)) {
                    return true;
                }
                // Copilot R1 (#344): guard the head name like resolveDisguisedRootAttribute /
                // the compiler-carrying resolver do — findAttributeOnDataType dereferences
                // name.equals(...), so a partially-populated REnumValueRef (enumName unset)
                // must decline to the pre-facet single read, never NPE.
                if (evr.enumName() != null) {
                    RDataType itemType = implicitItemDataType(evr, compiler);
                    if (itemType != null) {
                        RAttribute head = HandlerHelper.findAttributeOnDataType(itemType, evr.enumName());
                        if (head != null
                                && head.cardinality().map(NavigationHandler::isMultiValued).orElse(false)) {
                            return true;
                        }
                    }
                }
            }
            // Coverage wave D (datarule): the TYPE-CONDITION sibling of the #344 (a)
            // arm — the compiler-LESS resolver above is function-scoped and declines
            // every condition-seat disguise, so the wrap-kind read missed the mapC
            // crossing and the chain-end meta deref rendered the GUARDED numbered
            // form. Read the leaf through the COMPILER-carrying resolver (whose
            // wave-D condition arms are the SAME walk the rendered `.mapC(` witness
            // came from — the #348 lockstep law) and take its cardinality: golden
            // cdm5 EconomicTermsFpML_cd_26_28/27 `payout -> cashflow (0..*) -> …`
            // rides MapperC, so the deref is the BARE un-numbered form
            // (`_fieldWithMetaDate -> _fieldWithMetaDate.getValue()` — escaped at
            // finalization exactly when the guarded group's singleton claimed the
            // plain name). Function/rule paths never reach this arm
            // (findEnclosingTypeCondition null by construction).
            if (compiler != null
                    && HandlerHelper.findEnclosingTypeCondition(evr) != null) {
                RAttribute condLeaf = resolveDisguisedFeature(evr, compiler, null);
                if (condLeaf != null
                        && condLeaf.cardinality().map(NavigationHandler::isMultiValued)
                                .orElse(false)) {
                    return true;
                }
            }
            // facet thenVarRewrapElision (PR #358): a disguised chain rooted on an
            // EXPLICIT CLOSURE PARAM (`businessEvent -> instruction` inside the named
            // `then extract businessEvent [ … ]` lambda — ClearingSwapUSIs/UTIs) — the
            // root name is neither a function symbol nor an item feature, so every
            // resolver above declines. The param's element resolves through the SAME
            // closure-param walk the #342/#358 bare-receiver render reads (owner
            // filter/extract ARGUMENT), and the chain rides MapperC when the first hop's
            // attribute is MULTI (`instruction` 0..* → the rendered
            // `businessEvent.<Instruction>mapC(…)` step) — so the meta LEAF's coercion
            // bares exactly like every other mapC-riding chain. Monotone (only ADDs
            // multi); a single first hop falls through to the #310 tail below.
            if (compiler != null && evr.enumName() != null && evr.valueName() != null) {
                RInlineFunction evrCpOwner =
                        ReferenceHandler.enclosingClosureParamOwner(evr, evr.enumName());
                if (evrCpOwner != null && !(evrCpOwner.parent() instanceof RThenExpr)) {
                    RExpression evrOwnerArg = evrCpOwner.parent() instanceof RFilterExpr f2
                            ? f2.argument()
                            : evrCpOwner.parent() instanceof RExtractExpr e2
                                    ? e2.argument() : null;
                    RDataType evrRootElem = evrOwnerArg == null ? null
                            : resolveReceiverDataType(evrOwnerArg, compiler);
                    RAttribute evrHop = evrRootElem == null ? null
                            : HandlerHelper.findAttributeOnDataType(evrRootElem, evr.valueName());
                    if (evrHop != null && evrHop.cardinality()
                            .map(NavigationHandler::isMultiValued).orElse(false)) {
                        return true;
                    }
                }
            }
            // facet chainMapperCRootRungs, rung R2 (seat 28): a disguised 2-name chain
            // rooted on a FUNCTION or RULE CALL (`EconomicTermsForProduct -> payout`,
            // drr 7.x UnderlyingIdentificationType) - the parser's GlobalResolutionPass
            // binds resolvedSymbol to the callable and the #280 synthesis RENDERS
            // `MapperS.of(economicTermsForProduct.evaluate(...)).<Payout>mapC("getPayout", ...)`,
            // so the chain rides MapperC whenever the LEAF is multi. Every arm above
            // resolves an ATTRIBUTE head (the census's finding, confirmed at source: the
            // #310 tail's walk looks the head up with findAttributeOnDataType on the
            // ITEM type, which a function NAME never matches), and relaxing the
            // with-args fn arm's arity would not help - this callee's output is SINGLE
            // and it is the LEAF that is 0..*. Reads the leaf through
            // callableRootDisguisedLeaf - the SAME walk resolveDisguisedFeature's
            // callable-root arm uses (LAW 69, one walk two consumers), whose RRule
            // branch is HandlerHelper.ruleInferredOutputRType, THE ONE rule-output read
            // (seat 21). Keyed on the LEAF's own cardinality, NOT on "callable root":
            // the SAME carrier's `underlier.UnderlierForProduct -> Observable` head is
            // callable-rooted with a SINGLE leaf and golden GUARDS it - a root-keyed
            // rung would break the file it is meant to heal. RULE-scoped (the #232/#310
            // precedent): the function-scoped population is already served by the #344
            // arm above, which routes through the same walk. Monotone (only ADDs multi).
            if (compiler != null && HandlerHelper.findEnclosingRule(evr) != null) {
                RAttribute r2CallableLeaf = callableRootDisguisedLeaf(evr, compiler);
                if (r2CallableLeaf != null && attrIsMulti(r2CallableLeaf)) {
                    return true;
                }
            }
            // facet nullGuardCardinality (PR #310): a disguised chain rooted on the
            // implicit ITEM (`partyInformation -> regimeInformation`, both features off
            // ReportableInformation), not a function input/output/shortcut, is unresolved
            // by the 1-arg resolveDisguisedFeature above, so the chain wrongly reads
            // SINGLE and a meta LEAF renders MapperS — WrappedItemCoercer then emits the
            // null-guarded `w == null ? null : w.getValue()` where golden bares
            // `w.getValue()` (its MapperC arm) for the genuinely-multi receiver
            // (DTCC_LargeNotionalOffFacilitySwapElectionIndicator: the chain renders an
            // uncollapsed `mapC.mapC` at runtime). Re-root via the #282/#288 descent
            // (the same disguisedChainProvesMulti chainProvesMulti's REnumValueRef arm
            // uses). RULE-scoped → FUNCTION-byte-neutral (#232); monotone (only ADDs
            // multi); green-safe (a guard-vs-bare divergence on a provably-multi chain is
            // already a waivered mismatch — the fork guards, golden bares).
            return HandlerHelper.findEnclosingRule(evr) != null
                    && disguisedChainProvesMulti(evr, compiler);
        }
        if (e instanceof RSymbolReference symRef) {
            // facet interior_position_coercion: an ALIAS-call head's wrapper kind
            // reads from the SAME FunctionAliasHelper walk that renders the alias
            // method's MapperS/MapperC signature into the same generated file
            // (inferShortcutIsMulti — the non-meta sibling of arm A1's
            // inferShortcutMapperJavaType), so the kind this predicate reports and
            // the wrapper the chain actually rides cannot disagree: a multi alias
            // head (`interestRatePayouts(product).<RateSpecification>map(...)`)
            // carries MapperC down the whole chain and its meta coercions are BARE
            // un-registered params (the #170 law) — the leaf-only conservative
            // single read below classified it MapperS and emitted a spurious
            // numbered null-guard the golden does not carry (csa
            // InterestRateLeg1/2Basis if-conditions). A declined walk (usesOutput
            // builder-form, unresolvable, stateless compiler) falls through to the
            // pre-facet arms.
            AliasResolution alias = resolveAliasShortcut(symRef);
            if (alias != null
                    && Boolean.TRUE.equals(aliasIsMulti(alias, compiler))) {
                return true;
            }
            return symRef.symbol()
                    .filter(RAttribute.class::isInstance)
                    .map(RAttribute.class::cast)
                    .flatMap(RAttribute::cardinality)
                    .map(NavigationHandler::isMultiValued)
                    .orElse(false);
        }
        return false;
    }

    /**
     * facet lambda_item_body_coercion (arm B1) — gm-aware MONOTONE multi-proof
     * overlay for a functional-operation RECEIVER: returns {@code true} only when
     * a navigation-chain receiver provably carries MULTI cardinality, mirroring
     * upstream {@code CardinalityProvider.caseFeatureCall} (feature multi OR
     * receiver multi — receiver-multi PROPAGATES through single feature steps).
     * The parser-side {@link CardinalityComputer} consults only the leaf
     * feature's own {@code resolvedFeature} cardinality and reads SINGLE when
     * that field was never populated (a known IR gap the gm-aware
     * {@code fallbackResolveFeature} path recovers from), so
     * {@code ...only-element -> priceQuantity (1..*) extract ...} selected
     * {@code mapSingleToItem} where golden carries {@code mapItem}.
     *
     * <p>Arms (everything else returns {@code false} — DECLINE to the legacy
     * computer's answer, which already handles bare symbols, aliases and
     * function-call outputs correctly):
     * <ul>
     *   <li>{@link RFeatureCall} — the step's attribute ({@code resolvedFeature}
     *       or the gm-aware fallback) is multi, OR any upstream step is
     *       (recursion into the receiver — the upstream propagation law);</li>
     *   <li>disguised {@link REnumValueRef} navigation ({@code input -> feature})
     *       — the resolved leaf feature's own cardinality;</li>
     *   <li>{@code RListOpExpr} and every other shape — {@code false}. The
     *       implicit list-op barrier mirrors upstream
     *       {@code caseOnlyElementOperation} returning false; a chain that
     *       RESUMES multi after an only-element collapse proves multi from the
     *       post-collapse feature step instead (the gm-aware
     *       {@code resolveReceiverDataType} walk is only-element-transparent).</li>
     * </ul>
     *
     * <p>The overlay can only ADD multi on top of the legacy answer (consumers
     * OR it in), so every receiver the legacy computer already classifies
     * correctly keeps its bytes; corpus law: ZERO golden {@code .mapSingleToItem(}
     * directly follows a {@code .mapC(...)} step.
     */
    /**
     * facet mapItemClosureParamReceiver (PR #360): resolve the LEAF attribute of a
     * disguised 2-name chain whose root names an ENCLOSING extract's EXPLICIT closure
     * param — the param's element is the owner extract's receiver element (the rich
     * receiver walk), and the leaf resolves on it. {@code null} for a non-closure-param
     * root, a non-extract owner, or an unresolvable element/leaf. The cardinality
     * consumer is monotone (multi-only), so a then-declared param owner needs no
     * decline here: a multi leaf implies a multi chain from EITHER owner kind.
     */
    private static RAttribute closureParamDisguisedLeaf(REnumValueRef evr,
            ExpressionCompiler compiler) {
        if (evr.enumName() == null || evr.valueName() == null || compiler == null) {
            return null;
        }
        RInlineFunction owner =
                ReferenceHandler.enclosingClosureParamOwner(evr, evr.enumName());
        if (owner == null) {
            return null;
        }
        // facet ctorFieldBareInvokableThenArg (PR #385): the owner set widens to the
        // FILTER param (element-preserving, so the param's element is the filter's
        // ARGUMENT element — the SAME #342 closureParamDirectNav owner set;
        // GetReportableQuantityPeriodLeg1/2's `filter customPeriod [ … =
        // customPeriod -> calculationPeriod -> startDate ]` hop-2 witness). A
        // then-declared param owner keeps the decline (the #342 then-owner gate).
        RExpression ownerArg = owner.parent() instanceof RExtractExpr ownerExt
                ? ownerExt.argument()
                : owner.parent() instanceof RFilterExpr ownerFilt
                        ? ownerFilt.argument() : null;
        if (ownerArg == null) {
            return null;
        }
        RDataType elem = resolveReceiverDataType(ownerArg, compiler,
                Collections.newSetFromMap(new IdentityHashMap<>()));
        return elem == null ? null
                : HandlerHelper.findAttributeOnDataType(elem, evr.valueName());
    }

    public static boolean chainProvesMulti(RExpression e, ExpressionCompiler compiler) {
        return chainProvesMulti(e, compiler, null);
    }

    /**
     * seat 30 guard 1 — the recursion-carrying variant. {@code ruleVisited} is the
     * ON-PATH set of {@link RRule}s the {@link #defaultSpineProvesMulti} callee descent
     * is currently inside, threaded through EVERY composing edge of this walk so that
     * the {@link RSymbolReference}/{@link RRule} arm's hand-off back to
     * {@link #ruleOutputProvesMulti} carries it. Before seat 30 that hand-off allocated
     * a FRESH set on every re-entry, so a grammar-legal cyclic rule reference
     * ({@code reporting rule P: extract (Q default …)} against
     * {@code reporting rule Q: extract (P default …)}) recursed unboundedly through the
     * mutual path {@code defaultSpineProvesMulti → chainProvesMulti →
     * ruleOutputProvesMulti → defaultSpineProvesMulti} and overflowed the stack. The
     * shape is corpus-invisible on all 25 cells (no cyclic rule refs exist), so the
     * seat pins it on a fixture and proves the byte-neutrality on the matrix digest.
     *
     * <p><b>ON-PATH, not visited-ever.</b> The set is added to on the way DOWN and
     * removed in a {@code finally} on the way back UP, so it holds exactly the rules on
     * the current descent path. That is what makes the threading byte-neutral BY
     * CONSTRUCTION rather than by measurement: this walk BRANCHES (a {@code default}
     * tests both operands, an {@code extract} tests argument then body), and a
     * visited-ever set would let the FIRST branch's rule visit wrongly decline the
     * SECOND branch's — a diamond false-decline. On an acyclic rule graph no rule can
     * appear twice on one path, so every answer is unchanged.
     *
     * <p>{@code null} means "no rule hop taken yet"; the set is allocated lazily at the
     * first hop ({@link #defaultSpineProvesMulti}'s callee descent), so the dominant
     * indirection-free walks stay allocation-free. This is the ENGINE's own pattern —
     * {@code CardinalityComputer.compute(expr, visited, thenAware)} guards the identical
     * malformed-cycle case the identical way, lazily allocated for the identical reason
     * (LAW 69: the engine half and the overlay half of one question read one design).
     */
    private static boolean chainProvesMulti(RExpression e, ExpressionCompiler compiler,
            java.util.Set<Object> ruleVisited) {
        if (e instanceof RFeatureCall fc) {
            RAttribute attr = fc.resolvedFeature()
                    .orElseGet(() -> fallbackResolveFeature(fc, compiler));
            if (attr != null
                    && attr.cardinality().map(NavigationHandler::isMultiValued).orElse(false)) {
                return true;
            }
            return chainProvesMulti(fc.receiver(), compiler, ruleVisited);
        }
        // facet deepCallChainProvesMulti (v3.1 flip seat 29, law 5): a DEEP-arrow (->>)
        // step is an RDeepFeatureCall - a shape this overlay had NO arm for at all
        // (RDeepFeatureCall extends RExpression, not RFeatureCall) - so a deep-headed
        // chain fell to the tail's conservative `false` = SINGLE and
        // CollectionHandler.wrapSingleArmMapperCOf, reached from renderLadderLevel's
        // mapperC rung, wrapped the arm `MapperC.of(<chain>)` where golden returns the
        // chain BARE. That seat's compiled-type-first read cannot help: a deep call's
        // stamped type is the hop's ELEMENT type (metaNavResultType), never a Mapper, so
        // the AST verdict decides EVERY deep arm there (measured decidedBy=
        // astChainProvesMulti on 16 of 16 rows).
        //
        // The hop's cardinality is read from the SAME two calls
        // handle(RDeepFeatureCall) makes to RENDER it - resolveDeepFeature +
        // resolveDeepMapMethod, the #348 lockstep law - so the arity read here and the
        // emitted `.mapC(` cannot disagree (LAW 69). This is the seat-28
        // chainMapperCRootRungs rung R1 one walk over: same two calls, same monotone
        // shape, the cardinality overlay instead of the wrapper-kind walk.
        //
        // MEASURED (PROBE29-F16w, both routes, 25 cells, route-IDENTICAL row for row):
        // armKind=RDeepFeatureCall at that seat is 16 rows / 16 files, ALL BAND, ZERO
        // green. TWELVE are the carriers - drr 7.0.0/7.1.0/7.2.0/7.3.0 x
        // GetBasketConstituents / GetUnderlierProductIdentifierLeg1 /
        // UnderlierProductIdentifier, deep hop `chooseIdentifier` -> mapC, golden bare.
        // FOUR are the in-band NEGATIVE CONTROL - the same four cells'
        // NameOfTheUnderlyingIndexRule, deep hop `chooseName` -> map - whose wrap is
        // byte-CORRECT in golden (`return MapperC.of(MapperS.of(underlierForProduct
        // .evaluate(item.get()))....<FieldWithMetaString>map("chooseName", index ->
        // indexDeepPathUtil.chooseName(index))....);`). The mapC refinement is therefore
        // LOAD-BEARING: an armKind-only law would unwrap those four - a 4-cell
        // regression. (Their file is in the band for a DIFFERENT arm: an RToStringExpr
        // whose missing wrap is decidedBy=mapperCPrefix.)
        //
        // MONOTONE like every arm of this overlay (add-only true, never true->false): an
        // unresolvable deep feature reads "map" (resolveDeepMapMethod's null contract) and
        // therefore declines - the conservative SINGLE the tail would have returned anyway.
        //
        // ONE TERM, BY MEASUREMENT (v3.1 flip seat 30, the m-law5ii adjudication). Seat 29
        // shipped this arm with a SECOND term - `return chainProvesMulti(dfc.receiver(),
        // compiler)`, the RFeatureCall arm's upstream-propagation law carried to the deep
        // node - and built mutation lane m-law5ii (law5-apply29.py --mut-norecurse) to
        // sever it so the receipts chain could ADJUDICATE it rather than assume it. The
        // chain ran that lane TWICE (run 1 `687c8feb`, run 2 `219fc263`) and MEASURED IT
        // EMPTY both times: `Tests run: 11, Failures: 0, Errors: 0, Skipped: 1` over
        // DeepCallChainMultiSeatTest, in the SAME loop that measured 4F for m-law5 (the
        // whole law) and 4F for m-law5i (the mapC term severed) - so the instrument was
        // live in both directions. All twelve corpus carriers flip on the mapC term alone;
        // the four negative controls decline because their deep receiver is a SINGLE-output
        // rule call, for which the recursion answered `false` anyway. a2/b2 are the minimal
        // pair (identical SINGLE-output-function receiver, only the hop's cardinality
        // differs) and b2 is the DECLINE lock - it pins that NO term of this arm proves
        // MULTI off that receiver, which the one-term form satisfies by construction.
        //
        // Dropping the term NARROWS a monotone overlay: the one-term form can only return
        // false where the two-term form returned true, and the measured size of that set is
        // ZERO. A future deep-headed carrier whose RECEIVER is multi while its own hop is
        // `map` would need the term back; nothing in this corpus is that shape, and the
        // four controls are the standing lock on the class.
        //
        // LAW 80: ZERO whole files on its own (-12 diff lines, one `return` per carrier
        // file). The whole-file value is JOINT with the hunk-mate cardinality law.
        if (e instanceof RDeepFeatureCall dfc) {
            return "mapC".equals(resolveDeepMapMethod(resolveDeepFeature(dfc, compiler)));
        }
        if (e instanceof REnumValueRef evr && evr.enumeration().isEmpty()) {
            // facet blockLambdaCardinalityJoin (PR #373, F-alpha): the CALLABLE-root
            // sibling of the #345 alias-root consult — a disguised 2-name chain rooted
            // at a FUNCTION (`GetUnderlierProductIdentifier -> identifier`, esma/fca
            // UnderlyingIdentification) rides the callable's MULTI output regardless of
            // the leaf's own cardinality: the RFeatureCall arm's receiver recursion at
            // the disguised seat, and exactly the receiver the #280 synthesis renders
            // (render-truth lockstep). Monotone multi-only by the upstream mirror
            // (receiver multi ⇒ chain multi), so a green single-form file cannot carry
            // a truly-multi callable root. A RULE-bound head keeps the legacy walk (the
            // fromRule conversion delivers RFunction here — the P370F probe).
            if (evr.enumName() != null && evr.valueName() != null
                    && evr.resolvedSymbol().filter(RFunction.class::isInstance)
                            .map(RFunction.class::cast)
                            .flatMap(rootFn -> rootFn.output())
                            .flatMap(RAttribute::cardinality)
                            .map(NavigationHandler::isMultiValued).orElse(false)) {
                return true;
            }
            RAttribute attr = resolveDisguisedFeature(evr);
            // facet aliasSigReturnTypeLeak (PR #342): a disguised ALIAS hop
            // (`commodityUnderlier -> productTaxonomy`, enumName = a shortcut) resolves
            // through the compiler-carrying variant — the SAME gm-aware walk the body
            // render's witness path already used to type the `.mapC("getProductTaxonomy",
            // …)` step (render-truth lockstep: the arity probe and the rendered bytes
            // read one resolution). Monotone like every arm of this overlay: a resolved
            // single terminal still answers false, and a green single-form render's
            // golden is single (upstream computes TRUE cardinality), so a newly-proven
            // MULTI can only move an already-waivered mismatch (drr
            // ExtractCommodityClassification's filterItemNullSafe arity).
            if (attr == null && compiler != null) {
                attr = resolveDisguisedFeature(evr, compiler, new HashSet<>());
            }
            if (attr != null) {
                if (attr.cardinality().map(NavigationHandler::isMultiValued).orElse(false)) {
                    return true;
                }
                // facet disguisedInputRootCardinality (PR #389): the MULTI
                // function-INPUT root sibling of the #345 alias-root consult below —
                // a resolved-SINGLE disguised leaf over a MULTI input root rides the
                // input's list (`fpmlPayerReceiverModelList -> payerModel`: the #280
                // synthesis renders `MapperC.<X>of(<input>).<Elem>map(…)` — the head
                // input's MULTI is exactly what makes the render MapperC, the
                // render-truth lockstep), so the filter/extract arity reads
                // filterItemNullSafe/mapItem (golden cdm6
                // MapPayerReceiverToAccountPartyReference ×2 methods). Monotone
                // multi-only like every arm here: upstream computes true cardinality,
                // so a green single-form file cannot carry a truly-MULTI input root.
                RFunction rootOwner = HandlerHelper.findEnclosingFunction(evr);
                if (rootOwner != null && evr.enumName() != null) {
                    for (RAttribute rootIn : rootOwner.inputs()) {
                        if (evr.enumName().equals(rootIn.name())
                                && rootIn.cardinality()
                                        .map(NavigationHandler::isMultiValued)
                                        .orElse(false)) {
                            return true;
                        }
                    }
                }
                // facet disguisedAliasRootCardinality (PR #345): a resolved-SINGLE
                // disguised leaf previously answered false WITHOUT consulting the
                // disguised ROOT — the mirror of the RFeatureCall arm's receiver
                // recursion above. For an ALIAS-rooted disguised chain
                // (`optionPayout -> underlier`, optionPayout a MapperC-signatured
                // shortcut) the chain rides the alias's MapperC regardless of the
                // leaf's own cardinality, so consult the SAME
                // FunctionAliasHelper.inferShortcutIsMulti walk that renders the
                // alias method's signature into the same file (render-truth
                // lockstep — the #325/#342 same-walk law; a green file consuming a
                // MapperC alias chain single-form would be non-compiling, so the
                // add-only TRUE can only move already-waivered mismatches).
                // Qualify_InterestRate_Option_DebtOption cdm6: mapSingleToItem →
                // golden mapItem. Non-alias roots keep the legacy false (monotone).
                // facet disguisedRenderChainCardinality (v3.1 flip seat 30, law 3,
                // rung B): an ITEM-ROOTED disguised head inside a THEN body rides the
                // PIPED LIST. The render already knows this - ReferenceHandler's B2 arm
                // (synthesizeImplicitItemChain) roots the chain at a synthetic
                // RImplicitVariable, which the scope binds to the enclosing `thenArgN`
                // local, and that local is a MapperC whenever the then's ARGUMENT is
                // multi (golden drr 7.x GetRegimeSpecificIdentifiers: `final
                // MapperC<ReportableJurisdictionInformation> thenArg1 = ...; return
                // thenArg1.<TransactionInformation>map("getTransactionInformation", ...)
                // .<TradeIdentifier>map("getTransactionIdentifier", ...)
                // .filterItemNullSafe(...)`). The cardinality half read only the LEAF
                // (single) and the input/alias ROOTS (neither matches an item feature),
                // so it answered SINGLE and the filter seat chose the MapperS-only
                // filterSingleNullSafe over a MapperC receiver - non-compiling (LAW 74;
                // MapperC.java carries no filterSingleNullSafe).
                //
                // The consult is this method's OWN RImplicitVariable arm, verbatim: the
                // then-item's cardinality IS the then ARGUMENT's (upstream
                // CardinalityProvider; see that arm's javadoc below). It is applied HERE
                // rather than reached by recursion because the disguised node is not an
                // RImplicitVariable - and it is EXACTLY equivalent to running the arm on
                // the render's synthetic root: thenOwnerArgument(start) walks from
                // start.parent(), the synthetic root's parent is set to evr.parent(), so
                // thenOwnerArgument(evr) and thenOwnerArgument(syntheticRoot) are the
                // same walk over the same nodes. No synthetic node is minted here.
                //
                // itemRootedDisguisedHead is the ORDER discriminator of the shared
                // resolver: this arm must not claim an INPUT/OUTPUT/dispatch-base root
                // (resolveDisguisedRootAttribute, whose own MULTI rung sits above) nor an
                // ALIAS root (whose rung sits immediately below) - only the item-feature
                // root that resolveDisguisedFeature's arm 2 resolved. Monotone add-only:
                // upstream reads a then-item as its argument's list, so a green
                // single-form consumer of a truly-multi pipe never compiled.
                if (itemRootedDisguisedHead(evr, compiler)) {
                    RExpression pipedOwner = thenOwnerArgument(evr);
                    if (pipedOwner != null && argumentProvesMulti(pipedOwner, compiler, ruleVisited)) {
                        return true;
                    }
                }
                return Boolean.TRUE.equals(disguisedAliasRootIsMulti(evr, compiler));
            }
            // v3.1 flip seat 4 (Rung A): the #345 alias-root consult is
            // leaf-INDEPENDENT (disguisedAliasRootIsMulti name-matches the
            // enclosing function's shortcuts and reads the SAME
            // FunctionAliasHelper.inferShortcutIsMulti walk that renders the alias
            // method's signature), but it sat INSIDE the attr!=null gate — so a
            // disguised alias-head chain whose LEAF the resolver cannot type (a
            // choice-OPTION leaf over an alias root: `underliers -> Observable`,
            // or any root whose ladder body defeats resolveReceiverDataType) read
            // SINGLE and the then-hoist declared MapperS over the MapperC-riding
            // alias call — non-compiling, banded (drr 7.0.0
            // GetBasketConstituents's `final MapperS<Basket> thenArg0 =
            // underliers(trade)…`, the LAW-65 seat-4 dump). Run the consult on the
            // unresolved-leaf path too — chainRendersMapperC's own EVR arm already
            // consults it UNGATED (the interior_position_coercion wrapper-kind
            // arm), so the
            // cardinality walk and the wrapper-kind walk now read the alias root
            // identically. Monotone add-only multi; green-safe by the #325/#345
            // same-walk law (TRUE ⇒ the alias method in the same file is
            // MapperC-signatured ⇒ a MapperS-decl / mapSingleTo* consumer never
            // compiled). Known-shadow caveat: an explicit closure param named
            // identically to an enclosing shortcut would consult the alias — the
            // SAME accepted ordering chainRendersMapperC's own ungated
            // interior_position_coercion consult carries (alias-root before its
            // closure-param arm); the closure/case-narrowed arms below keep
            // serving every non-alias root on the falsy path.
            if (Boolean.TRUE.equals(disguisedAliasRootIsMulti(evr, compiler))) {
                return true;
            }
            // facet mapItemClosureParamReceiver (PR #360): a disguised 2-name chain whose
            // ROOT is an ENCLOSING extract's EXPLICIT closure param (`priceQuantity ->
            // price` inside `priceQuantityList extract priceQuantity [ … ]` —
            // UpdateAmountForEachMatchingQuantity cdm5/cdm6) resolves the leaf on the
            // param's ELEMENT type (the owner extract's receiver element — the same walk
            // the #342 render reads) and proves MULTI from the leaf's cardinality.
            // Monotone multi-only, exactly like the #358 chainRendersMapperC closure arm:
            // a single leaf answers false (a missed proof at worst), and golden's
            // mapItem-over-mapC form never byte-matched the fork's mapSingleToItem.
            if (compiler != null) {
                RAttribute cpLeaf = closureParamDisguisedLeaf(evr, compiler);
                if (cpLeaf != null && cpLeaf.cardinality()
                        .map(NavigationHandler::isMultiValued).orElse(false)) {
                    return true;
                }
            }
            // facet caseNarrowedDisguisedNav (PR #368, F-B): a disguised chain inside a
            // TYPE-guard switch CASE resolves its head on the NARROWED case type — the
            // SAME resolution the #368 synthesis renders (render-truth lockstep) — and
            // proves MULTI from the leaf's cardinality (`floatingRateModel ->
            // capRateSchedule`, StrikeSchedule 0..*: mapSingleToItem → golden mapItem +
            // the ADD-arm `.getMulti()`). Monotone multi-only like every arm here.
            if (compiler != null) {
                RAttribute caseLeaf = caseNarrowedDisguisedLeaf(evr, compiler);
                if (caseLeaf != null && caseLeaf.cardinality()
                        .map(NavigationHandler::isMultiValued).orElse(false)) {
                    return true;
                }
            }
            // facet interiorThenArgCardinality (PR #288): the 1-arg resolveDisguisedFeature
            // resolves a disguised head only against the enclosing function's INPUT/OUTPUT/
            // shortcut NAMES — a head that is the FIRST navigation feature off the implicit item
            // (the rule body `then extract contractDetails -> documentation`) is unresolved and
            // the chain wrongly reads SINGLE. Re-root via the #282 descent, RULE-scoped so the
            // gm-aware overlay stays FUNCTION-byte-neutral.
            if (HandlerHelper.findEnclosingRule(evr) != null
                    && disguisedChainProvesMulti(evr, compiler)) {
                return true;
            }
            // facet disguisedRenderChainCardinality (v3.1 flip seat 30, law 3, rung A):
            // the LAST rung of the cardinality arm consults the LAST arm of the RENDER
            // ladder. ReferenceHandler.handle(REnumValueRef) resolves a RULE-body
            // disguised head against the enclosing rule's FROM-TYPE and synthesizes
            // `input -> head -> leaf` (the seat-28 law-11 by-NAME arm, placed last there
            // for the same reason it is placed last here: it fires only where every
            // earlier arm declined). The cardinality half had NO arm for that head at
            // all - resolveDisguisedFeature roots enumName against the enclosing
            // FUNCTION's inputs/outputs/shortcuts, and a rule has no enclosing RFunction
            // - so `originatingWorkflowStep -> timestamp` read SINGLE while the render
            // emitted `.<EventTimestamp>mapC("getTimestamp", ...)` for the same hop
            // (golden drr 7.x iosco cde v1 ExecutionTimestampRule: `final
            // MapperC<EventTimestamp> thenArg0 = ... .mapC(...).filterItemNullSafe(...)`
            // + `final MapperC<ZonedDateTime> thenArg1 = thenArg0.mapItem(...)`; the fork
            // emitted MapperS + filterSingleNullSafe + mapSingleToItem, which MapperC does
            // not carry - non-compiling, LAW 74).
            //
            // No new cardinality logic: the synthesized chain's hops carry their resolved
            // RAttributes, so the verdict is the SAME leaf/head cardinality read the
            // render typed the `.mapC(` from. That makes the two halves one resolution by
            // CALL, not by copy (LAW 69).
            //
            // The read is bounded to the SYNTHESIZED SPINE and deliberately does NOT
            // recurse through chainProvesMulti. ReferenceHandler.buildImplicitInputReceiver
            // roots the synthesized chain at a fresh RImplicitVariable whose parent it sets
            // to evr.parent() - i.e. wired into the REAL tree - whenever the disguise sits
            // inside a rule LAMBDA. A generic recursion therefore reaches this method's own
            // S2 then-item arm on that synthetic node, reads thenOwnerArgument off the real
            // parent chain, and returns the enclosing PIPE's cardinality: the synthesized
            // `input` root would be mis-read as the then ITEM, claiming MULTI for every
            // rule-body input-rooted disguise inside a then lambda regardless of its own
            // hops. That is precisely the over-fire this rung must not have - the render
            // emits `MapperS.of(input)` there, a SINGLE root - so the spine walk stops at
            // the first non-RFeatureCall receiver and never consults the root at all.
            //
            // Monotone add-only and green-safe by the LAW-74 argument: a proven-MULTI
            // hop makes the render a MapperC, and MapperS-only consumers
            // (filterSingleNullSafe / mapSingleToItem) over a MapperC never compiled, so
            // no byte-correct golden sits in the flip set at the selection seats. A
            // declined synthesis (closure-param head, function-scope head, no enclosing
            // rule, non-data from-type, unresolvable leaf) keeps the legacy false.
            return synthesizedSpineProvesMulti(
                    ReferenceHandler.synthesizeImplicitInputChainByName(evr, compiler));
        }
        // facet then_maxmin_item_typing (arm S2): a then-body's implicit item is the
        // WHOLE piped list (upstream CardinalityProvider: the then-item's cardinality
        // is the then-ARGUMENT's), not a per-element item — the parser-side computer
        // reads SINGLE for every RImplicitVariable. Filter/extract/max/min owners
        // stay false (their items are per-element; see thenOwnerArgument's javadoc
        // for the known decline-direction list-of-lists under-mirror and the two
        // upstream defining-container decline gates). Monotone like every other arm.
        if (e instanceof RImplicitVariable) {
            RExpression ownerArg = thenOwnerArgument(e);
            return ownerArg != null && argumentProvesMulti(ownerArg, compiler, ruleVisited);
        }
        // facet then_maxmin_item_typing (arm S2): cardinality-transparent /
        // cardinality-composing operations, mirroring upstream CardinalityProvider —
        // a filter keeps its argument's cardinality (caseFilterOperation), an
        // extract's value is multi when its receiver OR body is (caseMapOperation),
        // and a then's value is its BODY's value (caseThenOperation). Needed so a
        // CHAINED then (`a filter [...] then filter [...] then extract ...`) proves
        // multi through every hop back to the multi base.
        if (e instanceof RFilterExpr filter) {
            return argumentProvesMulti(filter.argument(), compiler, ruleVisited);
        }
        if (e instanceof RExtractExpr extract) {
            if (argumentProvesMulti(extract.argument(), compiler, ruleVisited)) {
                return true;
            }
            RInlineFunction body = extract.body();
            return body != null && argumentProvesMulti(body.body(), compiler, ruleVisited);
        }
        if (e instanceof RThenExpr then) {
            RInlineFunction body = then.body().orElse(null);
            return body != null && argumentProvesMulti(body.body(), compiler, ruleVisited);
        }
        // facet condBothArmsMultiCardinality (PR #339): a SINGLE-level conditional whose
        // BOTH arms prove MULTI is itself MULTI — upstream CardinalityProvider joins the
        // arm cardinalities, while the fork's global compute hard-codes a conditional
        // SINGLE (the #289 whole-output scoping). RULE-scoped (the FUNCTION tail stays
        // byte-frozen) and BOTH-arms-strict (a mixed single/multi conditional keeps its
        // legacy SINGLE — no carrier evidence for the golden form there); ladder/nested
        // shapes decline (the #289 whole-output conditional stays on
        // getRuleBodyCardinality + appendThenConditionalBlock). Carrier: the csa
        // margin/valuation UniqueTransactionIdentifier extract body — a present-else
        // conditional with a multi-output bare-FUNCTION then-arm and an interior-mapC
        // nav else-arm, whose golden renders mapSingleToList + MapperC<String> (the
        // #289-comment DEFERRED block-lambda-for-multi sub-case).
        // facet listOfListsCardinality (PR #370, F-D): the #339 both-arms law GENERALIZED —
        // PATH-BLIND (the GetBasketConstituentsProductIdentifier FUNCTION-body ladder) and
        // LADDER-AWARE (else-if chains recurse), with EMPTY arms NEUTRAL: upstream's arm
        // join of (multi, empty) is multi — the golden
        // `return MapperC.<ReferenceWithMetaProductIdentifier>ofNull();` ladder terminal is
        // the direct byte evidence (the #144/#269 empty-joins law).
        // facet blockLambdaCardinalityJoin (PR #373, F-alpha): the join is upstream's OR —
        // ANY value arm proving MULTI makes the conditional MULTI (was ALL-arms-strict),
        // and nested-THEN conditionals recurse the same join (were a decline). Monotone
        // add-only by the mirror argument (see conditionalLadderProvesMulti).
        if (e instanceof RConditionalExpr cond) {
            return conditionalLadderProvesMulti(cond, compiler, ruleVisited);
        }
        // facet aliasDefaultJoin (PR #362): a `default` whose BOTH operands prove MULTI is
        // itself MULTI — upstream binaryExpr `case "default"` keeps the joined operand
        // cardinality (the #339 conditional both-arms law at the default node; monotone
        // both-strict, so a mixed or unproven pair keeps the legacy SINGLE read). Carrier:
        // cdm6 MapBasketReferenceInformation's basketIds extract body (`<mapC chain>
        // default <mapC chain>` — the #345 multiDefaultTernary render), whose golden
        // extract is mapSingleToList.
        // facet thenSeatMultiDefaultTernary (v3.1 flip seat 5): the join is upstream's
        // OR — a default is MULTI when EITHER operand proves (upstream
        // CardinalityProvider joins the operand cardinalities; the semantics —
        // left-if-present-else-right — make the result a list when either side is).
        // The #362 both-strict read was a deliberate under-mirror ("no carrier
        // evidence for mixed"); the seat-5 LAW-65 census measured the mixed carrier
        // (drr 7.x UniqueSwapIdentifierForValuation: a multi fn-call LEFT over a
        // single-collapsed then-chain RIGHT, golden `final MapperC<String>` +
        // the list-form ternary) and the widening is the #373
        // blockLambdaCardinalityJoin mirror argument verbatim: every operand proof
        // channel only claims multi where upstream does, so a green single-form
        // default (no truly-multi operand) keeps its false verdict byte-frozen.
        if (e instanceof com.regnosys.rosetta.ast.expressions.binary.RDefaultExpr defaultE
                && defaultE.rawLeft() != null && defaultE.rawRight() != null) {
            return argumentProvesMulti(defaultE.rawLeft(), compiler, ruleVisited)
                    || argumentProvesMulti(defaultE.rawRight(), compiler, ruleVisited);
        }
        // facet thenSeatMultiDefaultTernary (v3.1 flip seat 5): a LIST LITERAL of two
        // or more elements proves MULTI — render truth: an n>=2 literal ALWAYS renders
        // `MapperC.<X>of(a, b, …)` (the seat-5 census carrier: drr 7.x FXLeg1/2's
        // `alias leg1Currency: [chain, chain] default [chain, chain]`, whose #345
        // both-strict proof died here and left the non-compiling
        // `getOrDefault(MapperC…)` legacy form). Monotone add-only: upstream types a
        // multi-element list literal MULTI, so a green single-form consumer of one
        // cannot exist (the #325 mirror argument); the 0/1-element literals keep the
        // legacy fall-through (a 1-element literal renders unwrapped — its
        // cardinality is its element's, deliberately unclaimed).
        if (e instanceof RListLiteral listLit && listLit.elements().size() >= 2) {
            return true;
        }
        // facet functionTailCardinality (PR #325): list-op cardinality, mirroring upstream
        // CardinalityProvider — `flatten` produces a LIST by definition (multi, whatever its
        // operand), and the element-preserving `distinct`/`reverse` keep their operand's
        // cardinality (recurse). The collapsing ops (only-element/first/last/sum) stay on the
        // legacy fall-through (single). Needed so a whole-output `… then flatten then distinct`
        // terminal proves multi back through the pipe (drr ExtractRegimeInformation:
        // `distinct(thenArg1.flattenList()).getMulti()` where the fork's leaf-blind read
        // collapsed to `.get()` — non-compiling against the List output, already waivered).
        // Monotone add-only on both paths (the same mirror argument as the RFeatureCall arm).
        if (e instanceof RListOpExpr lop) {
            if (lop.op() == ListOp.FLATTEN) {
                return true;
            }
            if (lop.op() == ListOp.DISTINCT || lop.op() == ListOp.REVERSE) {
                return chainProvesMulti(lop.argument(), compiler, ruleVisited);
            }
            return false;
        }
        // facet then_maxmin_item_typing (arm S2): a bare symbol-EMPTY name that the
        // renderer re-roots off the rebound item (ReferenceHandler.
        // synthesizeImplicitItemBareNav — the `then resetDate` body shape) proves its
        // cardinality through the SAME synthesized navigation the rendering uses
        // (leaf-multi OR implicit-receiver-multi via the RFeatureCall recursion), so
        // the declared thenArgN wrapper and the rendered RHS cannot disagree. The
        // synthesis declines (null) everywhere the renderer declines — closure
        // params, function-scope names, non-implicit lambdas, unresolved item types —
        // preserving the legacy SINGLE answer.
        if (e instanceof RSymbolReference symRef) {
            // facet functionTailCardinality (PR #325): an ALIAS-call receiver carries the
            // alias walk's own cardinality — the SAME FunctionAliasHelper walk
            // (inferShortcutIsMulti via aliasIsMulti) that renders the alias method's
            // MapperS/MapperC signature into the same generated file, so the cardinality
            // this arm reports and the wrapper the receiver call actually returns cannot
            // disagree (the #178 chainRendersMapperC alias-arm law, now at the
            // cardinality predicate). A multi alias receiver previously fell through the
            // invokable-blind re-root arms below and under-reported SINGLE, selecting
            // `mapSingleToItem` over a MapperC receiver — which has no such method, a
            // non-compiling (already-waivered) mismatch on EITHER path, so this arm is
            // green-safe by construction path-blind (cdm6 MapNaturalPersonRoleList /
            // MapRelatedPartyToPartyRole: `relatedPerson(...)` / `relatedParties(...)`
            // alias receivers, golden `mapItem`). A declined walk keeps the legacy false.
            AliasResolution aliasRecv = resolveAliasShortcut(symRef);
            if (aliasRecv != null && Boolean.TRUE.equals(aliasIsMulti(aliasRecv, compiler))) {
                return true;
            }
            // facet smallScaleBuySideListOfLists (PR #293): a FUNCTION CALL used as a
            // navigation RECEIVER carries its OUTPUT cardinality. `ExtractRegimeInformation(
            // item, party) -> asicPartyInformation` is MULTI (ExtractRegimeInformation returns
            // a List; the navigated leaf asicPartyInformation is single, so the RFeatureCall
            // arm's leaf-multi check above misses it and recurses here into the receiver). The
            // bare-name re-root arms below admit only an empty-symbol / RAttribute reference, so
            // an invokable symbol fell through to `synthesized == null` → false and a multi
            // function-call receiver under-reported SINGLE (SmallScaleBuySideEntityIndicator
            // asic/trade: `mapSingleToItem` + `MapperS` where golden carries `mapSingleToList`
            // + `MapperC`). Originally RULE-scoped (the #288/#291 cardinality-overlay
            // precedent); PR #325 (functionTailCardinality) un-gated it to the function
            // path — `compute` reads the function's DECLARED output cardinality, the exact
            // mirror of upstream's isPreviousOperationMulti read, so a green file cannot
            // diverge (if the fork+golden agreed on the single method, the declared output
            // IS single and this arm still returns false). Monotone (add-only multi),
            // never a false positive.
            if (symRef.symbol().filter(RFunction.class::isInstance).isPresent()) {
                return new CardinalityComputer().compute(symRef) == ExpressionCardinality.MULTI;
            }
            // facet reportOutputRuleRefCardinality (PR #318): a bare REPORTING-RULE reference
            // used as a navigation RECEIVER / extract body (`then extract cdeV3.payment.OtherPayment`,
            // OtherPayment being a MULTI-output rule) carries its OUTPUT cardinality. Unlike a
            // FUNCTION (whose declared output cardinality the frozen `compute` above reads), a rule
            // has no explicit output type — its cardinality comes from the body, which `compute`
            // (thenAware=false) reads SINGLE. This is the SAME rule-aware `computeRuleBody` gap
            // {@code CollectionHandler.isBodyMulti} already closed for the producer METHOD (#274):
            // the extract renders `mapSingleToList(item -> MapperC.<X>of(<rule>.evaluate(...)))`
            // (multi method) but `renderThenExtractSet` declared the thenArg `MapperS<X>` (single
            // DECL) + the downstream then chose `mapSingleToItem` (single receiver) — a type mismatch
            // (`MapperS<X> = <MapperC>` never compiles). Consulting the rule-aware `computeRuleBody`
            // here makes the DECL + downstream cardinality agree with the already-multi METHOD. The
            // RFunction arm's compute-MULTI analogue, but rule-aware. RULE-scoped (findEnclosingRule)
            // → the FUNCTION tail stays byte-frozen (a reporting rule can only be referenced from a
            // rule/report by grammar); off the rule path the legacy false is preserved. Monotone
            // (add-only multi): `computeRuleBody` reads the referenced rule's OWN body cardinality —
            // the SAME golden-faithful function (`RWorkspace.getRuleBodyCardinality` == this) that
            // drives each rule's own `ReportFunction<I, List<X>>` vs `<I, X>` output type, and already
            // relied on at the METHOD seat (`CollectionHandler.isBodyMulti`, #274). Green-safety rests
            // on that faithfulness, NOT an absolute theorem: a hypothetical false-positive rule would
            // render its OWN report file with a mismatched `List<X>` signature (self-waivering) and
            // would have surfaced at #274's method seat; the DECL + METHOD provably read the same
            // signal on the same node (two-halves-agree), so a green rule (method + decl consistent)
            // is unaffected. The full-corpus gensuite (D11 20/20) is the green→red backstop. This only
            // widens the still-waivered injected-sub-rule delegations toward golden.
            if (symRef.symbol().filter(RRule.class::isInstance).isPresent()) {
                // seat 8 (indep review MF-1): the shared ruleOutputProvesMulti
                // predicate — this DECL/receiver half and the #274 METHOD half
                // (CollectionHandler.isBodyMulti, re-pointed in the same seat)
                // provably read the same signal on the same node (the
                // two-halves-agree doc above); a choice-option-overlaid callee
                // must move BOTH halves or the decl and method disagree.
                // Engine-first — byte-neutral wherever the overlay is silent.
                // seat 30 guard 1: THE hand-off. This arm is the ONE edge from this
                // walk back into ruleOutputProvesMulti, so it is the ONE edge a rule
                // cycle can close through. It carries the on-path set; the engine's own
                // computeRuleBody above is already cycle-guarded (its identity set).
                return HandlerHelper.findEnclosingRule(symRef) != null
                        && (new CardinalityComputer().computeRuleBody(symRef)
                                        == ExpressionCardinality.MULTI
                            || ruleOutputProvesMulti(symRef, compiler, ruleVisited));
            }
            RFeatureCall synthesized;
            if (symRef.symbol().isEmpty()) {
                synthesized = ReferenceHandler.synthesizeImplicitItemBareNav(symRef, compiler);
            } else if (symRef.symbol().get() instanceof RAttribute attr) {
                // The Cat-9-BOUND analogue: the linker resolved the bare name to an
                // RAttribute — re-rooted iff it is the item type's OWN feature (the
                // identity guard inside the synthesis declines inputs/outputs/
                // aliases/lambda-locals, all distinct RAttribute objects).
                synthesized = ReferenceHandler.synthesizeImplicitItemNavigation(symRef, attr, compiler);
            } else {
                synthesized = null;
            }
            return synthesized != null && chainProvesMulti(synthesized, compiler, ruleVisited);
        }
        return false;
    }

    /**
     * facet disguisedRenderChainCardinality (v3.1 flip seat 30, law 3, rung B): true iff
     * this disguised head is the ITEM-ROOTED one - the shape
     * {@link #resolveDisguisedFeature(REnumValueRef, ExpressionCompiler, Set)}'s ARM 2
     * resolves (the {@code implicitItemDataType} head/leaf walk, itself the
     * {@code ReferenceHandler.synthesizeImplicitItemChain} render's resolution folded into
     * the shared resolver) and NOT one of the roots the arms ABOVE it own.
     *
     * <p><b>The declines are the RENDER's own, consulted BY CALL, not restated.</b> The
     * first cut of this predicate re-implemented the resolver's arm order locally (an
     * input/output/dispatch-base test plus a hand-rolled shortcut loop) and so could drift
     * from the arm it claims to mirror. It now asks {@code ReferenceHandler} the two
     * questions its B2 synthesis asks itself, on the same node:
     * <ul>
     *   <li>{@code disguisedHeadIsClosureParam} — a closure-param head is a real Java
     *       lambda variable the synthesis refuses to re-root;</li>
     *   <li>{@code disguisedHeadResolvesInFunctionScope} — an INPUT / OUTPUT / SHORTCUT
     *       head (with the #389 self-shadow exemption) renders as
     *       {@code MapperS.of(<headName>)}, a SINGLE root whatever pipe encloses it. This
     *       is the gate that keeps the seat's {@code b3} shape
     *       ({@code header -> msgIdent} inside a then body over a MULTI pipe) out, and it
     *       is a strict SUPERSET of the {@link #resolveDisguisedRootAttribute} test the
     *       first cut used — the seat's in-signature GREEN control
     *       {@code MapTechnicalRecordId} ({@code fpmlRequestMessageHeader -> messageId})
     *       is declined by it too;</li>
     * </ul>
     * plus two conditions with no render-side twin to call:
     * <ul>
     *   <li>a DISPATCH-BASE input root ({@link #resolveDisguisedRootAttribute} non-null) —
     *       a dispatch variant declares no inputs of its own, so the function-scope read
     *       above cannot see it; its own MULTI rung sits immediately above this one;</li>
     *   <li>a head the enclosing lambda's ITEM type does not carry is not item-rooted at
     *       all (the positive half of the resolver's arm 2).</li>
     * </ul>
     * A {@code null} compiler declines (the compiler-less callers keep their behaviour).
     */
    private static boolean itemRootedDisguisedHead(REnumValueRef evr, ExpressionCompiler compiler) {
        if (compiler == null || evr == null
                || evr.enumName() == null || evr.valueName() == null) {
            return false;
        }
        if (ReferenceHandler.disguisedHeadIsClosureParam(evr)
                || ReferenceHandler.disguisedHeadResolvesInFunctionScope(evr)) {
            return false;
        }
        if (resolveDisguisedRootAttribute(evr) != null) {
            return false;
        }
        RDataType itemType = implicitItemDataType(evr, compiler);
        return itemType != null
                && HandlerHelper.findAttributeOnDataType(itemType, evr.enumName()) != null;
    }

    /**
     * facet disguisedRenderChainCardinality (v3.1 flip seat 30, law 3, rung A): the
     * cardinality of a SYNTHESIZED disguise chain, read over its own hops ONLY.
     *
     * <p>Walks the {@link RFeatureCall} spine the render built ({@code input -> head ->
     * leaf}) and answers true iff any hop's resolved feature is MULTI — the same
     * {@link RAttribute} cardinalities {@code resolveMapMethod} types the emitted
     * {@code .map(} / {@code .mapC(} from, so the arity reported here and the rendered
     * bytes cannot disagree (LAW 69).
     *
     * <p><b>Why this is not {@code chainProvesMulti(synthesized, compiler)}.</b> The
     * synthesized ROOT is a fresh node whose {@code parent} is set into the REAL tree
     * ({@code ReferenceHandler.buildImplicitInputReceiver}: an {@link RImplicitVariable}
     * for an in-lambda disguise, an {@code RSymbolReference} otherwise). Handing it to the
     * generic walk lets the S2 then-item arm read {@code thenOwnerArgument} off that real
     * parent chain and return the enclosing PIPE's cardinality — the synthetic root
     * mis-read as the then item, which would claim MULTI for every rule-body input-rooted
     * disguise inside a then lambda whatever its own hops say. Stopping at the first
     * non-{@link RFeatureCall} receiver is what keeps the rung on the shape it charters.
     * Bounded by {@link HandlerHelper#PARENT_WALK_LIMIT} like every other walk here; a
     * {@code null} chain (a declined synthesis) reads false.
     */
    private static boolean synthesizedSpineProvesMulti(RFeatureCall synthesized) {
        RExpression cur = synthesized;
        int depth = 0;
        while (cur instanceof RFeatureCall hop && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            RAttribute hopAttr = hop.resolvedFeature().orElse(null);
            if (hopAttr != null
                    && hopAttr.cardinality().map(NavigationHandler::isMultiValued).orElse(false)) {
                return true;
            }
            cur = hop.receiver();
        }
        return false;
    }

    /**
     * facet then_maxmin_item_typing (arm S2): the {@code chainProvesMulti}
     * recursion step for an owning-operation argument — multi when the legacy
     * parser-side computer already says so, OR when the gm-aware overlay proves
     * it. Shared by every composing arm above.
     */
    private static boolean argumentProvesMulti(RExpression argument, ExpressionCompiler compiler,
            java.util.Set<Object> ruleVisited) {
        if (argument == null) {
            return false;
        }
        return new CardinalityComputer().compute(argument) == ExpressionCardinality.MULTI
                || chainProvesMulti(argument, compiler, ruleVisited);
    }

    /**
     * facet listOfListsCardinality (PR #370, F-D): the generalized #339 conditional
     * cardinality — walks an else-if LADDER, requires every VALUE arm to prove MULTI
     * (≥1 must exist), treats EMPTY arms ({@code empty} / the parser-materialized empty
     * list literal, P358I) as NEUTRAL (upstream's arm join), and declines a nested-THEN
     * (undecidable single-line-arm assumption — the #294 class). See the caller's arm
     * note for the monotonicity argument.
     */
    private static boolean conditionalLadderProvesMulti(RConditionalExpr cond,
            ExpressionCompiler compiler, java.util.Set<Object> ruleVisited) {
        // facet blockLambdaCardinalityJoin (PR #373, F-alpha): upstream CardinalityProvider
        // joins a conditional's arm cardinalities with OR — ANY multi arm makes the whole
        // block MULTI (the mixed Call/PutCurrency iosco ladders: three optionPayout-rooted
        // multi arms + two single fn-call chains → golden mapSingleToList + MapperC decl +
        // MapperC.of-wrapped single arms). The #370 ALL-arms-strict read under-mirrored;
        // nested-THEN conditionals now recurse the same join (the #356 tree shape). Still
        // MONOTONE-under by the mirror argument: every per-arm proof channel
        // (chainProvesMulti et al.) only claims multi where upstream does, so a green
        // single-form ladder (no truly-multi arm) keeps its false verdict byte-frozen.
        RConditionalExpr cur = cond;
        while (true) {
            RExpression thenArm = cur.thenBranch();
            if (thenArm instanceof RConditionalExpr nestedThen) {
                if (conditionalLadderProvesMulti(nestedThen, compiler, ruleVisited)) {
                    return true;
                }
            } else if (!isNeutralEmptyArm(thenArm)
                    && argumentProvesMulti(thenArm, compiler, ruleVisited)) {
                return true;
            }
            RExpression elseArm = cur.elseBranch().orElse(null);
            if (elseArm == null || isNeutralEmptyArm(elseArm)) {
                return false;
            }
            if (elseArm instanceof RConditionalExpr nested) {
                cur = nested;
                continue;
            }
            return argumentProvesMulti(elseArm, compiler, ruleVisited);
        }
    }

    /** An {@code empty}-denoting arm: the empty literal or the materialized empty list (P358I). */
    private static boolean isNeutralEmptyArm(RExpression arm) {
        return arm instanceof REmptyLiteral
                || (arm instanceof RListLiteral ll && ll.elements().isEmpty());
    }

    /**
     * facet then_maxmin_item_typing (arm S2): the THEN-owner restriction of the
     * {@link #implicitItemArgument} walk — the nearest enclosing
     * {@link RInlineFunction}'s owning {@link RThenExpr} ARGUMENT (the piped
     * list the then-body's item IS), or {@code null} for every other owner
     * (filter/extract/max/min items are per-element — their cardinality stays
     * the legacy SINGLE; known under-mirror: upstream additionally reads
     * {@code isOutputListOfLists(argument)} for those owners — a list-of-lists
     * item IS multi upstream — the fork declines that case, decline-direction
     * only). Two extra decline gates mirror upstream
     * {@code ImplicitVariableUtil.findContainerDefiningImplicitVariable}: the
     * inline must actually BIND the implicit item (an explicit-param
     * then-lambda binds its param — upstream only lets a functional operation
     * define {@code item} when {@code parameters.isEmpty()}), and a GUARDED
     * switch-case crossed on the way REDEFINES {@code item} as the SINGLE
     * matched option (upstream's {@code SwitchCaseOrDefault} defining-container
     * case) — both DECLINE rather than walk on, staying strictly under
     * upstream. Same loop shape + {@link HandlerHelper#PARENT_WALK_LIMIT}
     * bound as the shared walk.
     */
    static RExpression thenOwnerArgument(RNode start) {
        RNode cur = start == null ? null : start.parent();
        int depth = 0;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur instanceof RSwitchCase switchCase && switchCase.guard().isPresent()) {
                return null;
            }
            if (cur instanceof RInlineFunction inline) {
                if (!(inline.isImplicit() || inline.paramNames().isEmpty())) {
                    return null;
                }
                if (inline.parent() instanceof RThenExpr then && then.body().orElse(null) == inline) {
                    return then.argument();
                }
                return null;
            }
            if (cur instanceof RRule || cur instanceof RFunction) {
                return null;
            }
            cur = cur.parent();
        }
        return null;
    }

    /**
     * Best-effort resolution of a feature call whose {@code resolvedFeature} field
     * was not set during type-directed resolution. Walks the receiver chain to
     * infer the receiver's type, then looks up the feature name on that type.
     *
     * <p>Supports these receiver shapes:
     * <ul>
     *   <li>{@link RSymbolReference} with a resolved {@link RAttribute} symbol —
     *       use the symbol's declared type.</li>
     *   <li>{@link RFeatureCall} with its own resolved feature or a transitively
     *       resolvable receiver chain — use the feature's type.</li>
     *   <li>{@link REnumValueRef} acting as a disguised feature call — resolve
     *       via the enclosing function's inputs/output.</li>
     * </ul>
     *
     * @return the resolved feature attribute, or {@code null} if resolution failed
     */
    private static RAttribute fallbackResolveFeature(RFeatureCall expr) {
        RDataType receiverType = resolveReceiverDataType(expr.receiver());
        if (receiverType == null) return null;
        return HandlerHelper.findAttributeOnDataType(receiverType, expr.featureName());
    }

    /**
     * Resolve the {@link RDataType} of a receiver expression, walking through
     * chained feature calls and disguised enum-value refs as needed.
     */
    private static RDataType resolveReceiverDataType(RExpression receiver) {
        if (receiver == null) return null;

        if (receiver instanceof RSymbolReference symRef) {
            return symRef.symbol()
                    .filter(RAttribute.class::isInstance)
                    .map(RAttribute.class::cast)
                    .map(NavigationHandler::attributeToDataType)
                    .orElse(null);
        }

        if (receiver instanceof RFeatureCall fc) {
            // Prefer the pre-resolved feature
            if (fc.resolvedFeature().isPresent()) {
                return attributeToDataType(fc.resolvedFeature().get());
            }
            // Fall back to recursive resolution
            RAttribute attr = fallbackResolveFeature(fc);
            return attr == null ? null : attributeToDataType(attr);
        }

        if (receiver instanceof REnumValueRef evr && evr.enumeration().isEmpty()) {
            RAttribute attr = resolveDisguisedFeature(evr);
            return attr == null ? null : attributeToDataType(attr);
        }

        // only-element (`.get()`) is type-transparent: it collapses cardinality to a single
        // item but keeps the element type. A navigation step FROM a `.get()` recovers its
        // receiver data type from the operated-on list expression, so the witness + lambda
        // var resolve for that step AND every step chained after the `.get()` (the recursion
        // walks back through this case). Other list-ops change/lose the element type, so
        // ONLY_ELEMENT alone is transparent here.
        if (receiver instanceof RListOpExpr listOp && listOp.op() == ListOp.ONLY_ELEMENT) {
            return resolveReceiverDataType(listOp.argument());
        }

        return null;
    }

    /**
     * Resolve the implicit ITEM data type for a node inside an inline
     * filter/extract/then/max/min lambda (facet filter_predicate_item_typing; owners widened at facet then_maxmin_item_typing): walk the parent
     * chain to the nearest enclosing {@link RInlineFunction}, take the owning
     * list-op's ARGUMENT (the expression being filtered/mapped), and resolve that
     * argument's item data type through the same receiver-type resolution the
     * witness path uses ({@link #resolveReceiverDataType(RExpression, ExpressionCompiler)}
     * gm-aware when a {@code compiler} is supplied, the static AST walk otherwise).
     *
     * <p>Shared by the implicit-item lambda-var naming (mechanism 1,
     * {@link #resolveLambdaVarName(RExpression, String, JavaStatementScope, ExpressionCompiler)})
     * and {@code ReferenceHandler}'s implicit-item attribute-navigation synthesis
     * (mechanism 2) — the two read the SAME resolution, so the synthesized step's
     * identity guard and its rendered lambda var cannot disagree.
     *
     * <p>Returns {@code null} — every caller then preserves its pre-facet
     * behaviour — when there is no enclosing inline function before the
     * {@link RRule}/{@link RFunction} root, when the nearest inline function is
     * not a filter/extract/then/max/min BODY (sort/reduce lambdas keep their own
     * conventions; then/max/min owners resolve since facet
     * then_maxmin_item_typing), or when the argument's item type does not resolve to a data
     * type. Bounded by {@link HandlerHelper#PARENT_WALK_LIMIT} like every other
     * parent walk.
     */
    public static RDataType implicitItemDataType(RNode start, ExpressionCompiler compiler) {
        RExpression arg = implicitItemArgument(start);
        if (arg == null) {
            return null;
        }
        return compiler != null
                ? resolveReceiverDataType(arg, compiler)
                : resolveReceiverDataType(arg);
    }

    /**
     * v3.2 seat 7 (F11, LAW 77): the #358 STRUCTURAL pre-escape of a nav-hop lambda var against the LIVE-bound cast local
     * it hops off — ONE declaration for both routes. The case local of the in-lambda switch seats is {@code disambiguate}d
     * on the lambda scope, not registered as an identifier, so the deferred registry ({@code registerDeferredLambdaParam})
     * cannot see it at finalization; the default route's bound rung above pre-escapes structurally
     * ({@code _c18OptA -> _c18OptA.getAv()} off the local {@code c18OptA}) and the IR leaf emitter, which named the hop
     * through the registry alone, rendered the shadowing {@code c18OptA -> c18OptA.getAv()} (the seat's fix3 measurement:
     * the twelve chaos rows byte-identical on the default route, one token off on the IR route). A null cast local keeps
     * the rung's pre-seat answer ({@code "_" + base}).
     */
    static String preEscapeAgainstCastLocal(String base, String castLocal) {
        return (castLocal != null && !castLocal.equals(base)) ? base : "_" + base;
    }

    /**
     * v3.2 seat 7 (LAW 77): the IR leaf emitter's consult of {@link #preEscapeAgainstCastLocal} — the emitter holds the
     * EMITTED receiver where the default route's bound rung holds the AST receiver, so the "live binding" gate that rung
     * takes through {@code activeSwitchSubjectBoundText} is taken here through the scope's own registry
     * ({@link JavaStatementScope#boundSwitchSubjectText}): the receiver IS a switch-subject binding live in the scope chain
     * (the option-getter form's Mapper-typed local, or the instanceof form's {@code MapperS.of(<castVar>)} wrap) or the
     * pre-escape does not apply and the registry's answer stands exactly. The s7a chain's catch: the first cut gated on the
     * receiver's TEXT equalling the desired var name, which every input named after its type satisfies — nine plain hops of
     * {@code IRExpressionCompilerTest} rendered {@code __trade} / {@code _party} for the registry's {@code _trade} /
     * {@code party}; the 26-cell matrix was UNMOVED on the ON route because the corpus reaches this leaf for no such hop,
     * so the ir-java unit suite was the only witness (the chain of record's job).
     */
    public static String preEscapeAgainstReceiver(String base, JavaStatementBuilder receiver, JavaStatementScope scope) {
        String castLocal = scope == null ? null : scope.boundSwitchSubjectText(receiver);
        return castLocal == null ? base : preEscapeAgainstCastLocal(base, castLocal);
    }

    /**
     * facet choiceSwitchLambdaOptionGetter (seat 31, law 4b): the LIVE-bound switch-subject
     * TEXT for the hoisted case-narrowed naming rung - like {@link #activeSwitchSubjectText}
     * but reading a MAPPER-TYPED direct binding (the option-getter/FER-ladder form,
     * {@code JavaExpression.from(local, MapperS<Case>)}, which has no unwrap) as the bound
     * expression ITSELF. The #221 wrapped bindings keep their inner text (unwrap-first),
     * so the pre-hoist consumers see identical answers.
     */
    private static String activeSwitchSubjectBoundText(RExpression receiver,
            JavaStatementScope scope) {
        if (scope == null) {
            return null;
        }
        RNode cur = receiver.parent();
        int depth = 0;
        com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr sw = null;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) {
                return null;
            }
            if (cur instanceof com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr s) {
                sw = s;
                break;
            }
            cur = cur.parent();
        }
        if (sw == null) {
            return null;
        }
        JavaExpression bound = scope.switchSubjectRefFor(sw);
        if (bound == null) {
            return null;
        }
        JavaStatementBuilder inner = bound.unwrapToBuilder().orElse(bound);
        return inner instanceof JavaExpression ie ? ie.renderToString() : null;
    }

    /**
     * facet caseSwitchAliasSubjectStmtArms (PR #370): the rendered TEXT of the cast
     * case var bound for the nearest enclosing switch — the #221 subject binding
     * ({@code MapperS.of(<caseVar>)}) unwrapped to its bare inner. Read by the
     * case-narrowed lambda-param naming so the #358 structural pre-escape keys on
     * the cast local's ACTUAL name (bare vs method-wide-numbered — golden
     * MapTransferStateList {@code genericProduct0/1} free the bare param). Walks
     * the SAME case boundary as {@link #caseNarrowedImplicitType} (stops at a
     * closer inline-fn); {@code null} when no live binding is in scope.
     * (This javadoc was orphaned above the seat-31 sibling by that method's insertion;
     * re-seated at the review of #603, L2-08.)
     */
    private static String activeSwitchSubjectText(RExpression receiver, JavaStatementScope scope) {
        if (scope == null) {
            return null;
        }
        RNode cur = receiver.parent();
        int depth = 0;
        com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr sw = null;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) {
                return null;
            }
            if (cur instanceof com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr s) {
                sw = s;
                break;
            }
            cur = cur.parent();
        }
        if (sw == null) {
            return null;
        }
        JavaExpression bound = scope.switchSubjectRefFor(sw);
        if (bound == null) {
            return null;
        }
        JavaStatementBuilder inner = bound.unwrapToBuilder().orElse(null);
        return inner instanceof JavaExpression ie ? ie.renderToString() : null;
    }

    /**
     * facet caseNarrowedDisguisedNav (PR #368, F-B): the NARROWED data type of the
     * TYPE-guard switch CASE hosting {@code start} — the nearest enclosing
     * {@link RSwitchCase} with NO inline-fn boundary between (a closer lambda owns
     * its own {@code item}), whose NAME guard resolves through the SAME resolution
     * the #221/#365 ladder casts use (the nav and the cast cannot disagree):
     * the linker's {@code resolvedGuard} first
     * ({@link ChoiceSwitchSupport#resolvedGuardRType} — PR #460), then the
     * pre-#460 {@code resolveTypeByName} fallback. {@code null} for any other seat.
     */
    static RDataType caseNarrowedImplicitType(RNode start, ExpressionCompiler compiler) {
        if (compiler == null) {
            return null;
        }
        RNode cur = start.parent();
        int depth = 0;
        RSwitchCase scase = null;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) {
                return null;
            }
            if (cur instanceof RSwitchCase c) {
                scase = c;
                break;
            }
            cur = cur.parent();
        }
        if (scase == null) {
            return null;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        if (gm == null) {
            return null;
        }
        com.regnosys.rosetta.ast.expressions.supporting.RSwitchCaseGuard guard =
                scase.guard().orElse(null);
        if (guard == null
                || guard.kind() != com.regnosys.rosetta.ast.enums.SwitchGuardKind.NAME) {
            return null;
        }
        String qn = guard.qualifiedName().orElse(null);
        if (qn == null) {
            return null;
        }
        RType t = ChoiceSwitchSupport.resolvedGuardRType(guard);
        if (t == null) {
            t = gm.resolveTypeByName(qn.substring(qn.lastIndexOf('.') + 1)).orElse(null);
        }
        return t instanceof RDataTypeRef ref ? ref.astNode() : null;
    }

    /**
     * facet caseNarrowedDisguisedNav (PR #368, F-B): the LEAF attribute of a
     * disguised {@code head -> feature} chain whose HEAD is an attribute of the
     * hosting switch case's NARROWED type ({@link #caseNarrowedImplicitType}) —
     * the cardinality source for {@link #chainProvesMulti}'s case arm.
     * {@code null} when the seat, the head, or the leaf does not resolve.
     */
    static RAttribute caseNarrowedDisguisedLeaf(REnumValueRef evr, ExpressionCompiler compiler) {
        String headName = evr.enumName();
        String leafName = evr.valueName();
        if (headName == null || leafName == null) {
            return null;
        }
        RDataType narrowed = caseNarrowedImplicitType(evr, compiler);
        if (narrowed == null) {
            return null;
        }
        RAttribute head = HandlerHelper.findAttributeOnDataType(narrowed, headName);
        if (head == null) {
            return null;
        }
        RDataType headType = attributeToDataType(head, compiler);
        return headType == null ? null
                : HandlerHelper.findAttributeOnDataType(headType, leafName);
    }

    /**
     * facet rerootItemNav (PR #282): {@link #implicitItemDataType} with an INFERRED-type
     * fallback — used by the rule-body reroot facet at TWO coordinated seats:
     * {@code ReferenceHandler.synthesizeImplicitItemChain} (re-root a disguised {@code head ->
     * feature} nav onto the enclosing rule-body map-lambda's implicit ITEM) and the
     * {@code navItemLambdaTyping} (#255) lambda-var-naming branch of {@link #resolveLambdaVarName}
     * (name the re-rooted nav's first lambda param from the item element type, not the rule
     * from-type). Both seats must agree on the item type or the synthesized nav and its lambda
     * param name disagree.
     *
     * <p>The structural walk {@link #resolveReceiverDataType(RExpression, ExpressionCompiler)} does
     * not resolve a THEN-piped / conditional-bodied lambda argument (it returns {@code null} for an
     * {@link com.regnosys.rosetta.ast.expressions.binary.RThenExpr} argument, the dominant rule-body
     * shape — 346 of the disguised navs decline here), so the disguised head falls through to the
     * witness-less, non-compiling bare {@code MapperS.of(<head>)}. But the lambda argument's element
     * type IS known: {@code FunctionExpressionRenderer.renderThenExtractSet} declares the upstream
     * {@code thenArg} from EXACTLY {@code gm.workspace().getInferredType(argExpr)}, so this resolves
     * the implicit item type the SAME way when the structural walk fails. Monotone (the structural
     * result wins first → the 660 already-resolving navs are byte-unchanged); add-only fallback.
     *
     * <p>Green-safe by construction: the consuming arm fires only where the disguised head would
     * otherwise render the non-compiling bare {@code MapperS.of(<head>)} (a NON_COMPILING,
     * already-waivered mismatch — no green file carries it), and the recovered type is the SAME the
     * then-chain renderer already uses, so the re-rooted nav matches golden's witness + lambda-param
     * naming. Returns {@code null} (caller preserves the bare-name decline) when neither route
     * resolves or the inferred type is {@code MISSING}.
     */
    public static RDataType implicitItemDataTypeOrInferred(RNode start, ExpressionCompiler compiler) {
        return implicitItemDataTypeOrInferred(start, compiler, false);
    }

    /**
     * facet choiceOptionProjectionTypeId (v3.1 flip seat 32, law A1): the id-CARRYING
     * variant of {@link #implicitItemDataTypeOrInferred(RNode, ExpressionCompiler)} - the
     * inferred item type's CHOICE arm projects its options through
     * {@link #idCarryingChoiceBridge} (the seat-13 {@code projectedOptionAttribute} id+attach
     * pair) instead of {@link RChoiceTypeRef#asRDataType()}'s id-less floating copies.
     *
     * <p><b>Golden left arrow fork.</b> drr 7.0-7.3 {@code UnderlyingAssetTradingPlatformIdentifierLeg1/2Rule}:
     * golden derefs the {@code [metadata address]} choice option before the next hop -
     * {@code .<ReferenceWithMetaObservable>map("getObservable", underlier -> underlier.getObservable())}
     * {@code .<Observable>map("Type coercion", referenceWithMetaObservable0 -> referenceWithMetaObservable0 == null ? null : referenceWithMetaObservable0.getValue())}
     * {@code .<Asset>map("getAsset", observable -> observable.getAsset())} - and the fork emits
     * the WITNESS but not the {@code Type coercion}: six hops per file, 48 over the eight files.
     *
     * <p><b>The channel.</b> The option's OWN linker-resolved {@code referencedTypeId} - a
     * linker fact, never a re-derivation. Without it {@code GeneratorModel.resolveTypeCall}
     * falls to the by-name workspace lookup, which filters by {@code shouldGenerate}, and in
     * every drr cell the option's cdm namespace is a resolved-but-not-generated dependency:
     * {@code metaWrapperOf} reads back {@code RMissingType} and hard-returns null, the nav's
     * result type is null, and {@code ExpressionCompiler.coerceNavigationReceiver} has no
     * wrapper item to strip at the NEXT hop. With the id, {@code resolveTypeCall} takes its
     * {@code referencedTypeId -> workspace.resolveTypeLike} branch (filter-free AND
     * namespace-exact) and the wrapper resolves.
     *
     * <p><b>The discriminator, as MEASURED ({@code [P32-NAVTYPE]} / {@code [P32-SYNTHATTR]},
     * whole corpus, BOTH routes).</b> A projected choice-option attribute whose
     * {@code typeCall().referencedTypeId()} is EMPTY: 48 NAVTYPE rows (of 444,042) and 96
     * SYNTHATTR rows (of 3,318), ALL of them in the eight carrier files, ZERO green - the
     * minimal pair sits inside ONE file, {@code feature=Observable recvKind=RImplicitVariable
     * hasId=false rt=RMissingType wrapper=null} (24 per Leg) against its explicit twin
     * {@code recvKind=RSymbolReference hasId=true rt=RChoiceTypeRef
     * wrapper=ReferenceWithMetaObservable}. {@code hasId} is the ONLY differing input.
     *
     * <p><b>Why this entry point and not the shared one.</b> The measurement covers exactly
     * one consumer - {@code ReferenceHandler.synthesizeImplicitItemBareNav}. Routing the
     * carry through the public 2-arg form would widen it to seven further consumers the
     * probe never scanned; the #224 conversion-scope lesson (see
     * {@link #implicitItemDisguisedLeaf}) says hold the blast radius at the seat that owns
     * the claim. {@code attributeToDataType(RAttribute, ExpressionCompiler)} already narrows
     * choices through the SAME bridge (seat 13), so this is that seat's pair, not a new one.
     *
     * <p><b>The one projection difference, precedented and byte-neutral.</b>
     * {@link #idCarryingChoiceBridge} OMITS an option whose {@code typeCall} is null where
     * {@code asRDataType()} projects a {@code "?"}-named attribute; no Rune feature is named
     * {@code ?}, and seat 13 shipped exactly this difference at
     * {@code attributeToDataType}. Decline-to-status-quo everywhere else: no workspace (the
     * stateless unit path) or no {@code astNode} keeps today's id-less bridge.
     */
    static RDataType implicitItemChoiceIdCarryingDataType(RNode start, ExpressionCompiler compiler) {
        return implicitItemDataTypeOrInferred(start, compiler, true);
    }

    private static RDataType implicitItemDataTypeOrInferred(RNode start, ExpressionCompiler compiler,
            boolean idCarryingChoiceOptions) {
        RDataType structural = implicitItemDataType(start, compiler);
        if (structural != null) {
            return structural;
        }
        if (compiler == null) {
            return null;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        if (gm == null) {
            return null;
        }
        RExpression arg = implicitItemArgument(start);
        if (arg == null) {
            return null;
        }
        RMetaAnnotatedType inferred = gm.workspace().getInferredType(arg);
        if (inferred == null || inferred.isMissing()) {
            // facet fnNotionalTogetherRestructure (PR #398): the LUB fallback — a pipe
            // whose workspace inference is MISSING because its base is an ELSELESS
            // LUB-joined conditional (the commodityOptionNotional class: the parser
            // leaves `(if … then Measure {…} else if … then if … else <chain>) then
            // only-element then extract …` untyped, so the extract's bare `unit`/`value`
            // reads spliced raw) recovers the element by seeing THROUGH element-
            // preserving collapse steps to the conditional and folding its arms at the
            // nearest common EXTENDS supertype — the SAME per-arm fold the deep-then
            // ladder decl used (`MapperC<? extends MeasureBase>`), so the decl and the
            // re-rooted consumer cannot disagree. Fires only where both existing routes
            // returned null (the bare-name splice never compiled — green-safe by
            // construction).
            RExpression lubProbe = arg;
            if (lubProbe instanceof RImplicitVariable pipedItem) {
                // The extract's receiver is the PIPED implicit — its value is the
                // enclosing then's ARGUMENT chain (the T398I probe: the walk stopped
                // at the RImplicitVariable itself).
                RNode pipeUp = pipedItem.parent();
                for (int g2 = 0; pipeUp != null && g2 < 24; g2++, pipeUp = pipeUp.parent()) {
                    if (pipeUp instanceof com.regnosys.rosetta.ast.expressions.supporting
                                .RInlineFunction pipeFn
                            && pipeFn.parent() instanceof com.regnosys.rosetta.ast.expressions
                                    .binary.RThenExpr pipeThen
                            && pipeThen.body().orElse(null) == pipeFn) {
                        lubProbe = pipeThen.argument();
                        break;
                    }
                    if (pipeUp instanceof com.regnosys.rosetta.ast.functions.RFunction
                            || pipeUp instanceof com.regnosys.rosetta.ast.functions.RRule) {
                        break;
                    }
                }
            }
            for (int guard = 0; guard < 16; guard++) {
                if (lubProbe instanceof com.regnosys.rosetta.ast.expressions.binary
                        .RThenExpr probeThen) {
                    RExpression probeBody = probeThen.body()
                            .map(com.regnosys.rosetta.ast.expressions.supporting
                                    .RInlineFunction::body)
                            .orElse(null);
                    if (probeBody instanceof RListOpExpr probeLop
                            && (probeLop.op() == ListOp.FIRST || probeLop.op() == ListOp.LAST
                                    || probeLop.op() == ListOp.ONLY_ELEMENT
                                    || probeLop.op() == ListOp.DISTINCT)
                            && probeLop.argument() instanceof RImplicitVariable) {
                        lubProbe = probeThen.argument();
                        continue;
                    }
                    return null;
                }
                if (lubProbe instanceof RListOpExpr directLop
                        && (directLop.op() == ListOp.FIRST || directLop.op() == ListOp.LAST
                                || directLop.op() == ListOp.ONLY_ELEMENT
                                || directLop.op() == ListOp.DISTINCT)) {
                    lubProbe = directLop.argument();
                    continue;
                }
                break;
            }
            if (lubProbe instanceof RConditionalExpr lubCond) {
                RType folded = condArmExtendsFold(lubCond, compiler);
                if (folded instanceof RDataTypeRef foldedRef) {
                    return foldedRef.astNode();
                }
            }
            return null;
        }
        RType t = inferred.type();
        if (t instanceof RDataTypeRef dtr) {
            return dtr.astNode();
        }
        if (t instanceof RChoiceTypeRef ctr) {
            // facet choiceOptionProjectionTypeId (v3.1 flip seat 32, law A1): golden's
            // `.<Observable>map("Type coercion", ...)` after a `[metadata address]` choice
            // option left arrow the fork's un-dereferenced hop - the INFERRED item type's choice
            // options project through the seat-13 id+attach bridge, so the option's own
            // linker id (not a shouldGenerate-filtered name search) resolves the wrapper
            // and metaWrapperOf stops reading back RMissingType. Carriers: drr 7.0-7.3
            // UnderlyingAssetTradingPlatformIdentifierLeg1/2Rule (8 files, 6 hops each).
            // Reached ONLY from implicitItemChoiceIdCarryingDataType (the bare-symbol
            // synthesis seat, the measured population); the shared 2-arg entry point keeps
            // the id-less projection, so its seven other consumers cannot move.
            RWorkspace choiceWorkspace = idCarryingChoiceOptions ? workspaceOf(compiler) : null;
            return choiceWorkspace != null && ctr.astNode() != null
                    ? idCarryingChoiceBridge(ctr.astNode(), choiceWorkspace)
                    : ctr.asRDataType();
        }
        return null;
    }

    /**
     * facet fnNotionalTogetherRestructure (PR #398): fold a conditional LADDER's arm
     * types at the nearest common EXTENDS supertype — per-arm workspace inference with
     * the {@link #recoverThenArgItemRType} fallback, descending ONE-LEVEL nested-then
     * arms (the arm-c rung shape). Null when any arm is unresolvable or the fold has
     * no common data-type ancestor.
     */
    static RType condArmExtendsFold(RConditionalExpr cond, ExpressionCompiler compiler) {
        GeneratorModel gm = compiler == null ? null : compiler.getGeneratorModel();
        if (gm == null) {
            return null;
        }
        java.util.List<RExpression> armNodes = new java.util.ArrayList<>();
        RConditionalExpr cur = cond;
        while (true) {
            RExpression thenArm = cur.thenBranch();
            if (thenArm instanceof RConditionalExpr inner
                    && !(inner.thenBranch() instanceof RConditionalExpr)) {
                armNodes.add(inner.thenBranch());
                inner.elseBranch().ifPresent(armNodes::add);
            } else {
                armNodes.add(thenArm);
            }
            RExpression els = cur.elseBranch().orElse(null);
            if (els instanceof RConditionalExpr nested) {
                cur = nested;
                continue;
            }
            // The parser MATERIALIZES an absent else as an empty list literal (the
            // #371 F-E2 P358I read) — it types `nothing` (rune's join identity) and
            // must not poison the fold.
            if (els != null && !(els instanceof REmptyLiteral)
                    && !(els instanceof com.regnosys.rosetta.ast.expressions.literals
                            .RListLiteral elsLit && elsLit.elements().isEmpty())) {
                armNodes.add(els);
            }
            break;
        }
        RType folded = null;
        for (RExpression armNode : armNodes) {
            RMetaAnnotatedType nodeInferred = gm.workspace().getInferredType(armNode);
            RType armT = nodeInferred == null || nodeInferred.isMissing() ? null
                    : nodeInferred.type();
            if (armT == null) {
                armT = recoverThenArgItemRType(armNode, compiler);
            }
            if (armT == null) {
                return null;
            }
            if (folded == null) {
                folded = armT;
            } else if (!folded.equals(armT)
                    && !(folded instanceof RDataTypeRef fr && armT instanceof RDataTypeRef ar
                            && fr.astNode() == ar.astNode())) {
                folded = com.regnosys.rosetta.generator.java.function.FunctionAliasHelper
                        .nearestCommonExtendsSupertype(folded, armT);
                if (folded == null) {
                    return null;
                }
            }
        }
        return folded;
    }

    /**
     * Cycle-guard-threading variant of {@link #implicitItemDataType(RNode, ExpressionCompiler)}
     * (facet aliasCallMapWitness PR #253). Called ONLY from the {@code RImplicitVariable} arm of
     * {@link #resolveReceiverDataType(RExpression, ExpressionCompiler, Set)} so the enclosing
     * lambda-argument resolution shares the in-flight identity set — see the call site. Public
     * 2-arg callers start a fresh walk (a {@code null} set the core lazily allocates).
     */
    private static RDataType implicitItemDataType(RNode start, ExpressionCompiler compiler,
            Set<RNode> visited) {
        RExpression arg = implicitItemArgument(start);
        return arg == null ? null : resolveReceiverDataType(arg, compiler, visited);
    }

    /**
     * The enum analogue of {@link #implicitItemDataType} (facet tostring_enum_source):
     * resolve the implicit ITEM's {@link REnumeration} for a node inside an inline
     * filter/extract/then/max/min lambda (the shared walk's owners, widened at
     * facet then_maxmin_item_typing — so to-string/to-enum enum-source
     * resolution fires inside then/max/min lambdas too) — the same parent walk to the enclosing list-op's
     * ARGUMENT, whose element enum is then recovered through the SAME two routes a
     * directly-written {@code to-string}/{@code to-enum} source uses
     * ({@link #inferredEnumeration} for a directly-typed argument such as an
     * enum-list function input, {@link #leafEnumeration} for a navigation argument).
     * Returns {@code null} — callers preserve their pre-facet behaviour — when there
     * is no enclosing filter/extract/then/max/min lambda or the argument's item type is not an
     * enumeration.
     */
    static REnumeration implicitItemEnumeration(RNode start, ExpressionCompiler compiler) {
        RExpression arg = implicitItemArgument(start);
        if (arg == null) {
            return null;
        }
        REnumeration en = inferredEnumeration(arg, compiler);
        if (en != null) {
            return en;
        }
        return leafEnumeration(arg, compiler);
    }

    /**
     * facet implicitItemEnumSource (PR #225): the conversion-scoped leaf
     * {@link RAttribute} of a disguised {@link REnumValueRef} navigation source
     * ({@code periodicPayment -> fixedRateDayCountConvention} inside an
     * {@code extract}/map lambda) whose ROOT ({@code enumName}) is a feature on the
     * enclosing lambda's implicit ITEM element type — the shape
     * {@link #resolveDisguisedFeature(REnumValueRef, ExpressionCompiler, Set)} does
     * NOT reach (it roots {@code enumName} only against the enclosing function's
     * inputs/outputs/aliases, never the lambda item). Mirrors the head→leaf walk of
     * {@code ReferenceHandler.synthesizeImplicitItemChain} (the item-rooted
     * disguised-chain synthesis) but returns only the resolved leaf attribute; the
     * enum-ness test stays with the conversion-scoped caller
     * ({@code ConversionHandler.conversionLeafEnumeration}, which filters
     * {@link REnumeration}), so a non-enum leaf yields the generic string form
     * (green-safe). Conversion-scoped — deliberately NOT folded into the shared
     * disguise resolver — to hold the blast radius off the lambda-naming / witness /
     * chain-kind consumers (the #224 conversion-scope lesson).
     */
    static RAttribute implicitItemDisguisedLeaf(REnumValueRef evr, ExpressionCompiler compiler) {
        if (compiler == null || evr == null
                || evr.enumName() == null || evr.valueName() == null) {
            return null;
        }
        RDataType itemType = implicitItemDataType(evr, compiler);
        if (itemType == null) {
            return null;
        }
        RAttribute headAttr = HandlerHelper.findAttributeOnDataType(itemType, evr.enumName());
        if (headAttr == null) {
            return null;
        }
        RDataType headType = attributeToDataType(headAttr, compiler);
        if (headType == null) {
            return null;
        }
        return HandlerHelper.findAttributeOnDataType(headType, evr.valueName());
    }

    /**
     * facet implicitItemEnumSource (PR #225): the conversion-scoped leaf
     * {@link RAttribute} of a BARE symbol-reference navigation source
     * ({@code spreadCurrency to-string} / {@code period to-string} inside an
     * {@code extract}/map lambda) — the single-name item feature parses as an unresolved
     * {@link RSymbolReference} (an item feature is not in function scope, so the symbol
     * stays unresolved), the 1-name sibling of the 2-name disguised
     * {@link #implicitItemDisguisedLeaf}. Resolves the feature directly on the enclosing
     * lambda's element type. A genuine function-input enum source resolves earlier (the
     * inferred-type map / {@code siblingComparandEnumeration}) and never reaches this
     * arm; conversion-scoped and enum-ness left to the caller (green-safe — a non-enum
     * leaf keeps the {@code Object::toString} / {@code ::fromDisplayName} form).
     */
    static RAttribute implicitItemSymbolLeaf(RSymbolReference sym, ExpressionCompiler compiler) {
        if (compiler == null || sym == null || sym.name() == null) {
            return null;
        }
        RDataType itemType = implicitItemDataType(sym, compiler);
        if (itemType == null) {
            return null;
        }
        return HandlerHelper.findAttributeOnDataType(itemType, sym.name());
    }

    /**
     * The shared parent walk of {@link #implicitItemDataType} /
     * {@link #implicitItemEnumeration}: the nearest enclosing
     * {@link RInlineFunction}'s owning filter/extract ARGUMENT (the expression being
     * filtered/mapped), or {@code null} when there is no enclosing inline function
     * before the {@link RRule}/{@link RFunction} root or the nearest inline function
     * is not a filter/extract/then/max/min BODY (sort/reduce lambdas keep their
     * own conventions; then/max/min owner arms added at facet
     * then_maxmin_item_typing). Bounded by {@link HandlerHelper#PARENT_WALK_LIMIT} like every
     * other parent walk.
     */
    static RExpression implicitItemArgument(RNode start) {
        RNode cur = start == null ? null : start.parent();
        int depth = 0;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction inline) {
                return lambdaOwnerArgument(inline);
            }
            if (cur instanceof RRule || cur instanceof RFunction) {
                return null;
            }
            cur = cur.parent();
        }
        return null;
    }

    /**
     * v3.2 seat 6 (F5, facet lambdaItemDeepReceiver): the owning ARGUMENT of an inline function — the expression the
     * lambda filters / maps / keys / orders / pipes — or {@code null} for an owner this walk does not admit (a reduce
     * lambda keeps its own conventions). The ONE declaration behind {@link #implicitItemArgument} (the implicit item's
     * owner, read by the lambda-var naming, the enum-source and meta-wrap consumers) and
     * {@link #lambdaItemReceiverRType} (a deep call's implicit-item or explicit-parameter receiver), so the naming, the
     * witness and the deep-path util injection type a lambda's item from the SAME owner argument (LAW 69). The arms are
     * the walk's own, moved verbatim.
     */
    static RExpression lambdaOwnerArgument(RInlineFunction inline) {
        if (inline.parent() instanceof RFilterExpr filter && filter.body() == inline) {
            return filter.argument();
        }
        if (inline.parent() instanceof RExtractExpr extract && extract.body() == inline) {
            return extract.argument();
        }
        // facet then_maxmin_item_typing (arm S1/S3): max/min comparator-key lambdas and
        // then bodies type their implicit item from the SAME owning argument.
        if (inline.parent() instanceof RMaxExpr max && max.body().orElse(null) == inline) {
            return max.argument();
        }
        if (inline.parent() instanceof RMinExpr min && min.body().orElse(null) == inline) {
            return min.argument();
        }
        // facet sortLambdaItemTyping (PR #301): a sort COMPARATOR-key lambda
        // (`.sort(item -> item.<Date>map("getEffectiveDate", X -> …))`) binds its
        // implicit `item` to the sorted list's ELEMENT type exactly like max/min — so
        // type the item from the SAME owning argument. Pre-#301 sort declined here
        // ("sort/reduce keep their own conventions"), so the first navigation step's
        // lambda var was named from the rule from-type (`transactionReportInstruction`)
        // not the element type (`notionalPeriod`); golden names it from the element
        // (the #176/#255 lambda-naming law at the sort-comparator seat). Green-safe
        // by construction across every consumer of this shared walk: the naming branch
        // (resolveLambdaVarName) only ever renamed a still-waivered mismatch (a green
        // from-typed sort lambda would already carry the element name), and the
        // meta-deref / enum-source consumers fire only on their own additional gates
        // (a meta-annotated / enum terminal) — a sort-comparator key navigating to a
        // plain scalar (the NotionalQuantityScheduleLeg1/2 Date) reaches neither.
        if (inline.parent() instanceof RSortExpr sort && sort.body().orElse(null) == inline) {
            return sort.argument();
        }
        if (inline.parent() instanceof RThenExpr then && then.body().orElse(null) == inline) {
            return then.argument();
        }
        return null;
    }

    /**
     * The implicit item's META-wrapped {@code MapperS} type (facet
     * mapitem_ctor_wrap, mechanism 3) — the owning list-op argument's TERMINAL
     * attribute's {@link RJavaWithMetaValue} wrapped in {@code MapperS} via
     * {@link #metaNavResultType} (the map/filter/max/min lambda item is single
     * by construction — THEN owners, whose item is the whole piped list and
     * can be a MapperC, DECLINE below rather than mis-stamp the guard kind) —
     * or {@code null}, every caller keeping the null-typed receiver and the
     * coercion gate dormant, when: the nearest enclosing inline function does
     * not BIND the implicit item (an explicit-param lambda — upstream
     * {@code ImplicitVariableUtil} defines {@code item} only when
     * {@code parameters.isEmpty()}; mirrors {@code thenOwnerArgument}'s and
     * {@code ReferenceHandler.bindsImplicitItem}'s gate) or is a THEN body;
     * there is no enclosing filter/extract/max/min lambda (the SAME
     * {@link #implicitItemArgument} walk the lambda-var naming reads — retype
     * and naming cannot disagree); the argument is neither a feature call nor
     * an attribute-bound symbol reference (deep-feature / list-op-wrapped
     * arguments decline conservatively); or the terminal attribute is not
     * meta-annotated ({@code metaNavResultType}'s own gate). The wrapper ref
     * is discarded (a local throwaway set): the wrapper's NAME never appears
     * in the rendered text — only the coercion lambda param derives from it —
     * and the {@code <Value>} witness import rides the coercion service's own
     * ref channel. Coverage is anchor-only ({@code FunctionMapItemCtorWrapTest}
     * CompareTradeLot cdm5/cdm6 — the stateless unit compiler carries no
     * translator/model, so {@code metaNavResultType} declines there by
     * construction); the downstream gate is unit-locked in
     * {@code MetaReceiverCoercionTest}.
     */
    private JavaType implicitItemMetaMapperType(RFeatureCall expr, ExpressionCompiler compiler) {
        RExpression receiver = expr == null ? null : expr.receiver();
        String navFeature = expr == null ? null : expr.featureName();
        RNode cur = receiver == null ? null : receiver.parent();
        int depth = 0;
        RInlineFunction nearest = null;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction inline) {
                nearest = inline;
                break;
            }
            if (cur instanceof RRule || cur instanceof RFunction) {
                break;
            }
            cur = cur.parent();
        }
        if (nearest == null
                || !(nearest.isImplicit() || nearest.paramNames().isEmpty())
                || nearest.parent() instanceof RThenExpr) {
            return null;
        }
        RExpression arg = implicitItemArgument(receiver);
        if (arg == null) {
            return null;
        }
        RAttribute terminal = null;
        if (arg instanceof RFeatureCall fc) {
            terminal = fc.resolvedFeature().orElseGet(() -> fallbackResolveFeature(fc, compiler));
        } else if (arg instanceof RSymbolReference sr) {
            terminal = sr.symbol().filter(RAttribute.class::isInstance)
                    .map(RAttribute.class::cast).orElse(null);
        }
        if (terminal != null) {
            Set<JavaClass<?>> tmpRefs = new HashSet<>();
            return metaNavResultType(terminal, compiler, tmpRefs, false);
        }
        // facet metaDerefReceiverRule (PR #284): the implicit item's owning argument is
        // not a directly-resolvable feature, so the structural terminal resolution above
        // declined — recover the terminal meta wrapper from the chain instead. The drr
        // rule-body carrier is an EXTRACT off a then-chain: a rule body `<navTerminal> then
        // [item -> … item.<X>mapC("getY", …) …]` (e.g. OtherPaymentPayerFormatRule's
        // payer-role/party-id checks, item = ReferenceWithMetaParty). implicitItemArgument
        // returns the then-PIPE RImplicitVariable (the extract's argument is the piped
        // value), so follow it to the owning then's ARGUMENT via thenOwnerArgument — the
        // then's terminal navigation IS the extract's element type. golden inserts
        // `item.<Value>map("Type coercion", w -> w == null ? null : w.getValue())` before
        // navigating the wrapper; the fork navigated the wrapper-typed receiver directly →
        // NON_COMPILING, so every carrier is already waivered → green-safe by construction.
        // The fork's then-meta inference drops the wrapper (getInferredType reports the bare
        // value, #264), so recover via the #264/#270 then-aware walker recoverMetaFromExpr
        // (terminalNavAttr descends extract bodies + element-preserving list-ops; the
        // then-chain case walks bodies outermost-first, STOPPING at a non-meta terminal —
        // green-safe). The single (MapperS) wrap matches the existing arm: the
        // .mapSingleToItem item is single. Originally RULE-scoped (#284, the #232
        // FUNCTION-byte-neutrality caution at introduction); facet functionTailMetaCoerce
        // (PR #325) widened the fallback PATH-BLIND: every green-safety guard is
        // path-agnostic — a THEN-BODY lambda still declines at the gate above (its item is
        // the whole piped value and can be a MapperC, the #180 guard-kind hazard), the
        // walker still STOPS at a non-meta terminal, and the disguised route still
        // requires the recovered wrapper's VALUE type to carry the navigated feature
        // (metaValueHasNavFeature) — so a recovery only retypes an item whose wrapper the
        // chain PROVABLY carries, where the fork's bare-value nav on the wrapper-typed
        // item was a NON_COMPILING (already-waivered) mismatch on EITHER path (cdm6
        // CompareTradeLotToAmount / drr ExtractUpi: golden inserts
        // `item.<Value>map("Type coercion", w -> …getValue())` before the value-getter nav).
        //
        // PR #325 function-path anchor (the #297 anchoring precedent, keyed on the lambda
        // BODY SHAPE): on the FUNCTION path the recovery proceeds only for a lambda whose
        // body is a navigation / comparison / predicate expression — the shapes golden
        // reliably derefs IN-lambda (drr ExtractUpi's filter areEqual + mapSingleToItem nav
        // chain, FrequencyPeriod's filter exists-ladder, cdm RateOptionObservableCondition's
        // mapItem areEqual). A CONSTRUCTOR or CONDITIONAL body (drr NotionalLeg's
        // `mapSingleToItem(item -> MapperS.of(Measure.builder()…))` + its guarded
        // if/return blocks) sits inside an enclosing structure golden RESTRUCTURES entirely
        // (guarded returns / receiver-level coercion), where a locally-correct per-use deref
        // moves the file AWAY from golden — the regscan catch that drove this gate. The
        // RULE path keeps all body shapes (#284/#286 unchanged).
        if (HandlerHelper.findEnclosingRule(receiver) == null) {
            RExpression lamBody = nearest.body();
            if (lamBody instanceof com.regnosys.rosetta.ast.expressions.constructors.RConstructorExpr
                    || lamBody instanceof RConditionalExpr) {
                return null;
            }
        }
        RExpression chainArg = arg instanceof RImplicitVariable ? thenOwnerArgument(arg) : arg;
        if (chainArg != null) {
            RJavaWithMetaValue chainMeta = recoverMetaFromExpr(chainArg, compiler, 0, new HashSet<>());
            JavaTypeUtil tu = compiler.getTypeUtil();
            if (chainMeta != null && tu != null) {
                return tu.wrap(tu.MAPPER_S, chainMeta);
            }
            // facet disguisedChainMetaRecovery (PR #286): recoverMetaFromExpr declined
            // because the then-argument is a DISGUISED 2-name chain rooted on the FIRST
            // extract's item type (`reportingSide -> reportingCounterparty`), which
            // terminalNavAttr's resolveDisguisedFeature cannot descend — it resolves a
            // disguised head only against the enclosing function's INPUT/OUTPUT/shortcut
            // NAMES, not as a FEATURE of the implicit input. Recover the terminal meta by
            // re-rooting the disguised chain onto its own extract-lambda item type (the
            // #282 synthesizeImplicitItemChain route), gated by the recovered wrapper's
            // VALUE type carrying the feature this receiver navigates next.
            JavaType disguised = recoverDisguisedChainMeta(chainArg, navFeature, compiler);
            if (disguised != null) {
                return disguised;
            }
            // facet metaItemFilterChainRecovery (PR #334): the owning then-chain's argument
            // is a FILTER over a navigation chain (`<nav to meta multi> filter [...] then
            // extract [...]` — CompareTradeLotToAmount / GetCollateralBalancesForMarginType).
            // A filter preserves the ELEMENT type, so the filtered chain's terminal meta IS
            // the lambda item's wrapper — but recoverMetaFromExpr declines (terminalNavAttr
            // does not descend RFilterExpr). Descend the filter layers locally and resolve
            // the terminal exactly like the primary arg-is-FeatureCall branch above, DOUBLY
            // gated: the recovered wrapper's VALUE type must carry the feature this receiver
            // navigates next (metaValueHasNavFeature — a metafield nav off the wrapper, `->
            // reference`/`-> scheme`, declines rather than spuriously unwrapping), and
            // metaNavResultType's own meta gate (a non-meta terminal recovers null,
            // byte-flat). Green-safe by the mechanism-3 argument: pre-fix the fork navigated
            // a value getter on the wrapper-typed item — non-compiling, so no green file
            // carries the unwrap-free form.
            if (chainArg instanceof RFilterExpr) {
                RExpression chainCore = chainArg;
                int filterGuard = 0;
                while (chainCore instanceof RFilterExpr fl && filterGuard++ < 8) {
                    chainCore = fl.argument();
                }
                RAttribute filteredTerminal = null;
                if (chainCore instanceof RFeatureCall fc2) {
                    filteredTerminal = fc2.resolvedFeature()
                            .orElseGet(() -> fallbackResolveFeature(fc2, compiler));
                } else if (chainCore instanceof RSymbolReference sr2) {
                    filteredTerminal = sr2.symbol().filter(RAttribute.class::isInstance)
                            .map(RAttribute.class::cast).orElse(null);
                }
                if (filteredTerminal != null
                        && metaValueHasNavFeature(filteredTerminal, navFeature, compiler)) {
                    JavaType filteredMeta =
                            metaNavResultType(filteredTerminal, compiler, new HashSet<>(), false);
                    if (filteredMeta != null) {
                        return filteredMeta;
                    }
                }
            }
            // facet listLiteralElementMeta (PR #353): the owning list-op's argument is a
            // LIST LITERAL whose elements are navigation chains sharing a META-annotated
            // terminal (`[contractualProduct -> productIdentifier, security ->
            // productIdentifier]` — every element ReferenceWithMetaProductIdentifier;
            // GetIsin's filter item). recoverMetaFromExpr declines (terminalNavAttr does
            // not descend RListLiteral), so resolve EVERY element's terminal exactly like
            // the primary arg-is-FeatureCall branch and require ALL elements to agree on
            // ONE wrapper (canonical-name join — the elements live on DIFFERENT owner
            // types, so identity-compare would spuriously decline). DOUBLY gated like the
            // #334 filter arm: metaValueHasNavFeature (the wrapper's VALUE type carries
            // the feature this receiver navigates next) + metaNavResultType's own meta
            // gate; the item is an ELEMENT of the literal — single by construction
            // (MapperS wrap; the #348 isReceiverMultiInner reads the >1-element literal
            // itself MULTI, which is the RECEIVER's cardinality, not the item's).
            // Green-safe by the mechanism-3 argument: pre-fix the fork navigated a value
            // getter on the wrapper-typed element — NON_COMPILING, so no green file
            // carries the deref-free form.
            if (chainArg instanceof RListLiteral literal && !literal.elements().isEmpty()) {
                JavaType joined = null;
                String joinedFqn = null;
                boolean allAgree = true;
                for (RExpression element : literal.elements()) {
                    RAttribute elementTerminal = null;
                    if (element instanceof RFeatureCall efc) {
                        elementTerminal = efc.resolvedFeature()
                                .orElseGet(() -> fallbackResolveFeature(efc, compiler));
                    } else if (element instanceof RSymbolReference esr) {
                        elementTerminal = esr.symbol().filter(RAttribute.class::isInstance)
                                .map(RAttribute.class::cast).orElse(null);
                    } else if (element instanceof REnumValueRef) {
                        // A DISGUISED 2-name element (`contractualProduct -> productIdentifier`
                        // parses as an REnumValueRef with an empty enumeration — the #286
                        // class; GetIsin/UPIRule's elements): re-root onto the enclosing
                        // extract-lambda's item type via the #286/#287 resolver (a REAL enum
                        // value ref declines inside it — enumeration() non-empty).
                        elementTerminal = resolveDisguisedChainLeafAttr(element, compiler, false);
                    }
                    if (elementTerminal == null
                            || !metaValueHasNavFeature(elementTerminal, navFeature, compiler)) {
                        allAgree = false;
                        break;
                    }
                    RJavaWithMetaValue elementWrapper = metaWrapperOf(elementTerminal, compiler);
                    if (elementWrapper == null) {
                        allAgree = false;
                        break;
                    }
                    String elementFqn = elementWrapper.getCanonicalName().withDots();
                    // The join sentinel is the FQN (always non-null here), NOT the wrapped
                    // type — metaNavResultType re-resolves metaWrapperOf internally and
                    // returns non-null under the same inputs TODAY, but keying first-ness
                    // on `joined` would let a hypothetical null-typed first element skip
                    // the agreement check for later elements (the Seat-1 #353 OBS-1
                    // hardening; the final null guard below still declines the all-null
                    // case either way).
                    if (joinedFqn == null) {
                        joined = metaNavResultType(elementTerminal, compiler,
                                new HashSet<>(), false);
                        joinedFqn = elementFqn;
                    } else if (!elementFqn.equals(joinedFqn)) {
                        allAgree = false;
                        break;
                    }
                }
                if (allAgree && joined != null) {
                    return joined;
                }
            }
        }
        return null;
    }

    /**
     * facet disguisedChainMetaRecovery (PR #286): the implicit item's META {@code MapperS}
     * wrapper when the owning then-chain's argument is a DISGUISED 2-name navigation chain
     * ({@code head -> leaf}, parsed as an {@link REnumValueRef} with an empty
     * {@code enumeration()}) whose HEAD is a FEATURE of the chain-lambda's own implicit ITEM
     * type — the receiver-coercion analogue of {@code ReferenceHandler.synthesizeImplicitItemChain}.
     *
     * <p>{@link #recoverMetaFromExpr} / {@link #terminalNavAttr} decline this shape because
     * {@link #resolveDisguisedFeature(REnumValueRef, ExpressionCompiler, Set)} only resolves a
     * disguised head against the enclosing function's INPUT/OUTPUT/shortcut NAMES, never as the
     * FIRST navigation feature off the implicit input. The drr rule body
     * {@code reportingSide -> reportingCounterparty then extract [item -> … item.<X>mapC(…) …]}
     * (Counterparty2Identifier{Type,TypeIndicator,Source}) renders {@code thenArg} as
     * {@code MapperS<ReferenceWithMetaParty>}, but the {@code .mapSingleToItem(item -> …)} item
     * receiver navigates the wrapper directly (NON_COMPILING) — golden first inserts
     * {@code item.<Value>map("Type coercion", w -> w == null ? null : w.getValue())}.
     *
     * <p>Resolution: descend the then-argument's {@link RExtractExpr} layers to the disguised
     * body, resolve {@code head} as an attribute of the chain-lambda's item type (the FIRST
     * extract's item — the rule input), {@code leaf} as a feature of {@code head}'s type, and
     * return the leaf's {@link RJavaWithMetaValue} wrapped in {@code MapperS}.
     *
     * <p><b>Green-safety</b> ({@link #metaValueHasNavFeature}): the recovered wrapper's VALUE
     * type must carry the feature this receiver navigates next — so a mis-resolution (recovering
     * the wrong stage's meta) DECLINES rather than inserting a spurious deref. The fork form
     * (a value-type getter on the wrapper item) does not compile, so a correct recovery only
     * touches already-waivered NON_COMPILING carriers. RULE-scoped (caller gate). Returns
     * {@code MapperS<wrapper>} or {@code null}.
     */
    private JavaType recoverDisguisedChainMeta(RExpression chainArg,
            String navFeature, ExpressionCompiler compiler) {
        RAttribute leafAttr = resolveDisguisedChainLeafAttr(chainArg, compiler, false);
        if (leafAttr == null || !metaValueHasNavFeature(leafAttr, navFeature, compiler)) {
            return null;
        }
        return metaNavResultType(leafAttr, compiler, new HashSet<>(), false);
    }

    /**
     * facet disguisedChainMetaRecovery (PR #286/#287): the terminal {@link RAttribute} of a DISGUISED
     * 2-name navigation chain ({@code head -> leaf}, an {@link REnumValueRef} with an empty
     * {@code enumeration()}) re-rooted onto the chain-lambda's OWN implicit ITEM type — the shared
     * core of the receiver-coercion ({@link #recoverDisguisedChainMeta}, #286) and whole-output
     * ({@link #recoverDisguisedTerminalMetaWrapper}, #287) recoveries. Descends {@link RExtractExpr}
     * {@code map} layers (always) and, when {@code descendListOps}, the element-preserving
     * {@link RListOpExpr} wrappers ({@code only-element}/{@code first}/{@code last}/{@code distinct} —
     * the whole-output terminal shape {@code partyId -> identifier only-element}, which
     * {@link #terminalNavAttr} also descends) to reach the disguised body; resolves {@code head} as a
     * feature of the item type ({@link #implicitItemDataTypeOrInferred}, the #282 inferred fallback)
     * and {@code leaf} as a feature of {@code head}'s type. Returns the leaf {@link RAttribute} or
     * {@code null} for any non-disguised / unresolvable shape.
     */
    private static RAttribute resolveDisguisedChainLeafAttr(RExpression chainArg,
            ExpressionCompiler compiler, boolean descendListOps) {
        if (chainArg == null || compiler == null || compiler.getTypeUtil() == null) {
            return null;
        }
        // Descend RExtractExpr (map) layers — and, for the whole-output terminal, the
        // element-preserving list-ops — to the disguised-chain body (the descend terminalNavAttr
        // does, restricted here to the then-argument / output-terminal wrappers).
        RExpression body = chainArg;
        int depth = 0;
        boolean sawSingleCollapse = false;
        while (body != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (body instanceof RExtractExpr ext) {
                body = ext.body() == null ? null : ext.body().body();
                continue;
            }
            if (descendListOps && body instanceof RListOpExpr lo) {
                if (lo.op() == ListOp.ONLY_ELEMENT || lo.op() == ListOp.FIRST
                        || lo.op() == ListOp.LAST) {
                    sawSingleCollapse = true;   // collapses the chain to a single
                    body = lo.argument();
                    continue;
                }
                if (lo.op() == ListOp.DISTINCT) {   // dedup — element-preserving, keeps MULTI
                    body = lo.argument();
                    continue;
                }
            }
            break;
        }
        if (!(body instanceof REnumValueRef evr) || !evr.enumeration().isEmpty()) {
            return null;
        }
        String headName = evr.enumName();
        String leafName = evr.valueName();
        if (headName == null || leafName == null) {
            return null;
        }
        // Re-root the disguised chain onto its own extract-lambda item type (rule-scoped
        // inferred fallback, the #282 method).
        RDataType itemType = implicitItemDataTypeOrInferred(evr, compiler);
        if (itemType == null) {
            return null;
        }
        RAttribute headAttr = HandlerHelper.findAttributeOnDataType(itemType, headName);
        RDataType headType;
        if (headAttr == null) {
            // v3.1 flip seat 22, F1 rung R3 (facet wholeOutputMetaDeref): the disguised head that names
            // a CALLABLE - the SAME parser-binding read the (a3) rule-context walk makes
            // (disguisedCallableHeadOutputType: REnumValueRef.resolvedHead() typed through the
            // function's declared output / HandlerHelper.ruleInferredOutputRType). The hkma
            // ReferenceEntityRule carrier reaches THIS walk (the bare `then extract
            // ExtractReferenceEntity -> identifier` body takes the disguisedOutputMeta channel, not the
            // conditional one); the emir variant reaches the (a3) walk. STRICT fall-through - an
            // attribute head keeps the walk byte-identically; the multi gate below still applies on the
            // LEAF (a callable head is a single re-root here).
            headType = disguisedCallableHeadOutputType(evr, compiler);
            if (headType == null) {
                return null;
            }
            headAttr = null;
        } else {
            headType = attributeToDataType(headAttr, compiler);
            if (headType == null) {
                return null;
            }
        }
        RAttribute leafAttr = HandlerHelper.findAttributeOnDataType(headType, leafName);
        // facet disguisedOutputMeta (PR #287) single-output gate: the whole-output single wou deref
        // (`final <W> w = …; output = w.getValue();`) is correct ONLY for a SINGLE output. A
        // disguised output chain with NO single-collapse (only-element/first/last) whose head or
        // leaf is MULTI is a List output golden derefs ELEMENT-WISE
        // (`.<T>map("Type coercion", w -> w.getValue()).getMulti()`), a different mechanism — decline.
        // The fork mis-computes such a rule's cardinality as single (chainProvesMulti's 1-arg
        // resolveDisguisedFeature cannot descend the disguised chain either, so the
        // renderThenExtractSet multiOutput gate is wrongly false — OriginalSwapUTIRule:
        // `… then extract assignedIdentifier -> identifier`, assignedIdentifier MULTI, no collapse).
        // The receiver path (descendListOps=false, #286) is unaffected — it wraps MapperS per its own
        // single item and never reaches this whole-output deref.
        if (descendListOps && !sawSingleCollapse
                && (attrIsMulti(headAttr) || attrIsMulti(leafAttr))) {
            return null;
        }
        return leafAttr;
    }

    /** Whether an attribute is multi-valued ({@code (… ..*)}), null-safe. */
    /**
     * v3.1 flip seat 28 - facet chainMapperCRootRungs (LAW 69: ONE walk, two consumers).
     * The LEAF of a disguised 2-name chain whose ROOT is a bare FUNCTION/RULE invocation
     * the parser bound on {@code resolvedSymbol} - resolved on the callable's OUTPUT type
     * ({@link #resolveReceiverOutputType}, whose {@code RRule} branch is
     * {@code HandlerHelper.ruleInferredOutputRType}, THE ONE rule-output read), with the
     * choice-super-option fallback. Extracted verbatim from
     * {@code resolveDisguisedFeature}'s callable-root arm so that resolver and
     * {@code chainRendersMapperC}'s rung R2 read the SAME leaf.
     *
     * <p>The META-annotated DECLINE stays at the resolver's call site, NOT here: it exists
     * for that method's deref consumers (a resolved meta leaf would read as "the wrapper
     * survives to the output"), while a meta leaf's CARDINALITY is still the right
     * cardinality for a wrapper-kind read. Byte-inert for the existing consumer.
     *
     * @return the leaf attribute, or {@code null} for a non-callable root, an unresolvable
     *         output type, or a leaf that is neither a direct attribute nor a choice option
     */
    private static RAttribute callableRootDisguisedLeaf(REnumValueRef evr,
            ExpressionCompiler compiler) {
        if (evr == null || compiler == null || evr.valueName() == null
                || evr.resolvedSymbol().isEmpty()) {
            return null;
        }
        RNode root = evr.resolvedSymbol().get();
        if (!(root instanceof RRule) && !(root instanceof RFunction)) {
            return null;
        }
        RDataType outType = resolveReceiverOutputType(root, compiler);
        if (outType == null) {
            return null;
        }
        RAttribute leaf = HandlerHelper.findAttributeOnDataType(outType, evr.valueName());
        return leaf != null ? leaf : findChoiceSuperOption(outType, evr.valueName(), compiler);
    }

    private static boolean attrIsMulti(RAttribute attr) {
        return attr != null && attr.cardinality().map(NavigationHandler::isMultiValued).orElse(false);
    }

    /**
     * facet interiorThenArgCardinality (PR #288): whether a DISGUISED 2-name navigation
     * chain ({@code head -> leaf}, an {@link REnumValueRef} with empty {@code enumeration()})
     * re-rooted onto its extract-lambda item type is MULTI — its HEAD or LEAF attribute is
     * multi-valued.
     *
     * <p>The {@link #chainProvesMulti} {@link REnumValueRef} arm's 1-arg
     * {@link #resolveDisguisedFeature(REnumValueRef)} resolves a disguised head only against
     * the enclosing function's INPUT/OUTPUT/shortcut NAMES, so a head that is the FIRST
     * navigation feature off the implicit item (the drr rule body {@code then extract
     * contractDetails -> documentation}, {@code documentation} MULTI; {@code reportableInformation
     * -> partyInformation}) is unresolved and the chain reads SINGLE — the fork renders the
     * thenArg decl {@code MapperS} + {@code mapSingleToItem} + {@code filterSingleNullSafe} where
     * golden carries {@code MapperC} + {@code mapSingleToList} + {@code filterItemNullSafe}
     * (NON_COMPILING). Re-root via the #282 {@link #implicitItemDataTypeOrInferred} descent
     * (shared with {@link #resolveDisguisedChainLeafAttr}) and read head/leaf cardinality.
     *
     * <p>RULE-scoped (caller gate) → the gm-aware {@link #chainProvesMulti} overlay stays
     * FUNCTION-byte-neutral. Monotone: only ADDs multi (a disguised head that the 1-arg
     * resolver already resolved keeps its legacy answer). Returns false for any non-disguised /
     * unresolvable shape.
     */
    /**
     * facet disguisedHeadCardinality (PR #347): the FUNCTION-path bridge to
     * {@link #disguisedChainProvesMulti} for the extract-method seat
     * ({@code CollectionHandler.isBodyMulti}) — a disguised 2-name extract body whose
     * feature HEAD is multi on the implicit item selects the *ToList method (drr
     * PriceOfZeroCouponSwaps `extract tradeLot -> priceQuantity`). The #288 consult
     * inside {@code chainProvesMulti} stays rule-scoped; this bridge scopes the widening
     * to exactly the one seat with a byte-verified carrier.
     *
     * <p>facet listOfListLowering (PR #430): the CLOSURE-PARAM-headed arm — a disguised
     * body whose head is an enclosing extract/filter's EXPLICIT closure param
     * ({@code extract bar [ bar -> foos ]}, {@code foos} 0..*) resolves through the #360
     * {@link #closureParamDisguisedLeaf} walk (the SAME resolution the hop's witness /
     * arity / lambda-naming render reads — render-truth lockstep; the item-rooted
     * {@code disguisedChainProvesMulti} walk above cannot see it: the head is the param
     * ITSELF, not a feature of the item) and proves multi from the LEAF's cardinality,
     * so the named-param list-yielding body lowers into the {@code MapperListOfLists}
     * channel exactly like its implicit-item twin (upstream ListOperationTest
     * ExtractListOfListThenExtractToListOfCounts' own expected text:
     * {@code .mapItemToList(bar -> bar.<Foo>mapC("getFoos", …))}). Monotone add-only by
     * the #325 mirror argument — a leaf this walk proves multi IS multi upstream, so a
     * green single-form file cannot carry it.
     */
    static boolean disguisedBodyProvesMulti(REnumValueRef evr, ExpressionCompiler compiler) {
        if (disguisedChainProvesMulti(evr, compiler)) {
            return true;
        }
        return attrIsMulti(closureParamDisguisedLeaf(evr, compiler));
    }

    /**
     * facet bareSymbolBodyCardinality (PR #384): the 1-name sibling of
     * {@link #disguisedBodyProvesMulti} — a BARE-symbol extract body
     * ({@code extract price}) whose {@link RSymbolReference} never resolved
     * (symbol EMPTY) but whose name is a MULTI attribute of the implicit item
     * type. Resolution mirrors the bare-symbol render's implicit-item feature
     * synthesis (the SAME {@link #implicitItemDataTypeOrInferred} +
     * {@code findAttributeOnDataType} walk — render-truth), so the selected
     * {@code *ToList} method and the rendered {@code mapC} body cannot disagree.
     * Consumed only by {@code CollectionHandler.isBodyMulti} (the extract-method
     * seat); monotone add-only — a resolved symbol keeps the legacy
     * {@code CardinalityComputer} answer, and an unresolvable name returns false.
     */
    static boolean bareSymbolBodyProvesMulti(RSymbolReference sr, ExpressionCompiler compiler) {
        if (sr == null || compiler == null || compiler.getTypeUtil() == null
                || sr.symbol().isPresent()) {
            return false;
        }
        String name = sr.name();
        if (name == null) {
            return false;
        }
        RDataType itemType = implicitItemDataTypeOrInferred(sr, compiler);
        if (itemType == null) {
            return false;
        }
        return attrIsMulti(HandlerHelper.findAttributeOnDataType(itemType, name));
    }

    /**
     * facet choiceOptionNavAssignLadder (PR #394): a BARE unresolved symbol inside a
     * SWITCH-CASE expression resolves as a feature of the case guard's NARROWED type —
     * the narrowing is AST-STATIC (the guard IS the type; no scope binding needed), so
     * this stays consult-seat-scoped exactly like the #384 sibling above (the
     * extract-method receiver seat + the deep-then decl-wrapper seat — golden cdm6
     * CriteriaMatchesAssetType: `allCriteria` is 1..* on AllCriteria, so the arm chain
     * reads MULTI → {@code final MapperC<Boolean> thenArg1 = allCriteria.<…>mapC(…)
     * \n\t.mapItem(…)}; the parser leaves case-narrowed features unresolved, so
     * {@code compute}/{@code chainProvesMulti} read SINGLE and the fork selected
     * {@code MapperS}+{@code mapSingleToItem}, non-compiling). Monotone add-only by
     * the #325 mirror argument (a feature this walk proves multi IS multi upstream,
     * so a green single-form file cannot carry it); a RESOLVED symbol keeps the
     * legacy computer's answer.
     */
    static boolean switchCaseNarrowedBareSymbolProvesMulti(RExpression e,
            ExpressionCompiler compiler) {
        if (!(e instanceof RSymbolReference sr) || compiler == null
                || compiler.getGeneratorModel() == null
                || sr.symbol().isPresent() || sr.name() == null || !sr.args().isEmpty()) {
            return false;
        }
        RNode cur = sr.parent();
        RSwitchCase owningCase = null;
        int depth = 0;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur instanceof RSwitchCase sc) {
                owningCase = sc;
                break;
            }
            if (cur instanceof com.regnosys.rosetta.ast.functions.RRule
                    || cur instanceof RFunction) {
                break;
            }
            cur = cur.parent();
        }
        if (owningCase == null || owningCase.isDefault()) {
            return false;
        }
        com.regnosys.rosetta.ast.expressions.supporting.RSwitchCaseGuard guard =
                owningCase.guard().orElse(null);
        if (guard == null
                || guard.kind() != com.regnosys.rosetta.ast.enums.SwitchGuardKind.NAME) {
            return false;
        }
        String qn = guard.qualifiedName().orElse(null);
        if (qn == null) {
            return false;
        }
        RType caseT = ChoiceSwitchSupport.resolvedGuardRType(guard);
        if (caseT == null) {
            String simple = qn.substring(qn.lastIndexOf('.') + 1);
            caseT = compiler.getGeneratorModel().resolveTypeByName(simple).orElse(null);
        }
        RDataType dt = caseT instanceof RDataTypeRef dtr ? dtr.astNode()
                : caseT instanceof com.regnosys.rosetta.types.RChoiceTypeRef ctr
                        ? ctr.asRDataType() : null;
        if (dt == null) {
            return false;
        }
        return attrIsMulti(HandlerHelper.findAttributeOnDataType(dt, sr.name()));
    }

    /**
     * facet switchCaseNarrowedLeafWitness (PR #395): the WITNESS/type sibling of the #394
     * {@link #switchCaseNarrowedBareSymbolProvesMulti} cardinality read — a feature call whose
     * receiver is the IMPLICIT ITEM inside a SWITCH-CASE expression resolves as a feature of
     * the case guard's NARROWED type (the guard IS the type; AST-static, no scope binding).
     * The parser leaves the case-narrowed {@code item} untyped, so
     * {@code fallbackResolveFeature}'s {@code RImplicitVariable} arm nulls (no enclosing
     * lambda; FUNCTION path — no rule-top fallback) and the step lost its {@code <Type>}
     * witness + import: golden cdm6 CheckCriteria {@code CheckCountryOfOrigin(item ->
     * issuerCountryOfOrigin, query)} renders
     * {@code issuerCountryOfOrigin.<ISOCountryCodeEnum>map("getIssuerCountryOfOrigin",
     * _issuerCountryOfOrigin -> _issuerCountryOfOrigin.getIssuerCountryOfOrigin())} — the
     * same-named disguised-leaf wrapper class (5 sites: ISOCountryCodeEnum ×2 +
     * AgencyRatingCriteria ×3, + their 2 imports; every other token of those lines already
     * byte-matched, so the consult seat is EXACTLY the {@code handle(RFeatureCall)}
     * {@code resolvedAttr} null-fallback — the witness sentinel + {@code addWitnessTypeRef}
     * import recover together, and the meta-gated consumers ({@code chainMapperC} /
     * {@code metaNavResultType}) stay dormant on these plain attrs).
     *
     * <p>Walks the RECEIVER's parent chain to the nearest {@link RSwitchCase} and STOPS at any
     * {@link RInlineFunction} boundary first (the Seat-1 #394 OBS-2 note applied at birth: an
     * implicit inside a lambda nested in a case is the LAMBDA's item — that seat belongs to
     * {@code implicitItemDataType}, which the caller already tried), and at
     * {@code RRule}/{@code RFunction}. Single-hop by population (the direct
     * {@code RImplicitVariable} receiver only — deeper chains keep the pre-facet null/bytes).
     * Monotone add-only by the #325 mirror argument: upstream narrows the case item to the
     * guard type semantically, so any leaf this read resolves IS resolved upstream and golden
     * carries the witness — a green witness-less render over such a seat cannot exist
     * (census395: the 13 corpus-wide witness-less {@code .map("get} files carry no switch
     * renders; CMAT/CheckCriteria golden witness-less count = 0). A resolved feature keeps
     * its legacy answer (consulted only on null).
     */
    static RAttribute switchCaseNarrowedImplicitLeafAttr(RFeatureCall expr,
            ExpressionCompiler compiler) {
        if (expr == null || compiler == null || compiler.getGeneratorModel() == null
                || !(expr.receiver() instanceof RImplicitVariable)
                || expr.featureName() == null) {
            return null;
        }
        RNode cur = expr.receiver().parent();
        RSwitchCase owningCase = null;
        int depth = 0;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) {
                return null;
            }
            if (cur instanceof RSwitchCase sc) {
                owningCase = sc;
                break;
            }
            if (cur instanceof com.regnosys.rosetta.ast.functions.RRule
                    || cur instanceof RFunction) {
                break;
            }
            cur = cur.parent();
        }
        if (owningCase == null || owningCase.isDefault()) {
            return null;
        }
        com.regnosys.rosetta.ast.expressions.supporting.RSwitchCaseGuard guard =
                owningCase.guard().orElse(null);
        if (guard == null
                || guard.kind() != com.regnosys.rosetta.ast.enums.SwitchGuardKind.NAME) {
            return null;
        }
        String qn = guard.qualifiedName().orElse(null);
        if (qn == null) {
            return null;
        }
        RType caseT = ChoiceSwitchSupport.resolvedGuardRType(guard);
        if (caseT == null) {
            String simple = qn.substring(qn.lastIndexOf('.') + 1);
            caseT = compiler.getGeneratorModel().resolveTypeByName(simple).orElse(null);
        }
        RDataType dt = caseT instanceof RDataTypeRef dtr ? dtr.astNode()
                : caseT instanceof com.regnosys.rosetta.types.RChoiceTypeRef ctr
                        ? ctr.asRDataType() : null;
        if (dt == null) {
            return null;
        }
        return HandlerHelper.findAttributeOnDataType(dt, expr.featureName());
    }

    /**
     * facet choiceDeepNavLadderArm (PR #396): the case guard's NARROWED {@link RType} for an
     * IMPLICIT-ITEM receiver inside a switch case — the RType sibling of the #395
     * {@link #switchCaseNarrowedImplicitLeafAttr} read (the SAME AST-static walk: nearest
     * {@link RSwitchCase}, STOP at any {@link RInlineFunction} boundary first and at
     * RRule/RFunction; NAME guards only; the guard's SIMPLE name resolves against the loaded
     * workspace — the Seat-1 #395 OBS-2 cross-namespace caveat carries). Feeds the deep-call
     * receiver resolution ({@link #resolveDeepReceiverJavaClass} — shared by the renderer's
     * {@code <type>DeepPathUtil} field/lambda var and {@code FunctionDependencyCollector}'s
     * {@code @Inject} field — and {@link #resolveDeepFeature}'s receiver walk), consulted ONLY
     * on a null shared-walk answer. Green-safe by the #395 mirror argument: upstream narrows
     * the case item to the guard type semantically, and every deep-call carrier is waivered
     * (zero green deep-call files), so a new resolution can only move waivered bytes.
     */
    static RType switchCaseNarrowedImplicitGuardRType(RExpression receiver,
            ExpressionCompiler compiler) {
        if (!(receiver instanceof RImplicitVariable) || compiler == null
                || compiler.getGeneratorModel() == null) {
            return null;
        }
        RNode cur = receiver.parent();
        RSwitchCase owningCase = null;
        int depth = 0;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) {
                return null;
            }
            if (cur instanceof RSwitchCase sc) {
                owningCase = sc;
                break;
            }
            if (cur instanceof com.regnosys.rosetta.ast.functions.RRule
                    || cur instanceof RFunction) {
                break;
            }
            cur = cur.parent();
        }
        if (owningCase == null || owningCase.isDefault()) {
            return null;
        }
        com.regnosys.rosetta.ast.expressions.supporting.RSwitchCaseGuard guard =
                owningCase.guard().orElse(null);
        if (guard == null
                || guard.kind() != com.regnosys.rosetta.ast.enums.SwitchGuardKind.NAME) {
            return null;
        }
        String qn = guard.qualifiedName().orElse(null);
        if (qn == null) {
            return null;
        }
        // facet caseGuardResolvedBinding (PR #460): the linker's resolvedGuard first —
        // the authoritative import/alias-aware binding; for a choice-subject switch the
        // stored node IS the option's resolved type (the #449/#451 store), so the
        // option-tree walk below agrees by construction when both answer.
        RType resolvedFirst = ChoiceSwitchSupport.resolvedGuardRType(guard);
        if (resolvedFirst != null) {
            return resolvedFirst;
        }
        String simple = qn.substring(qn.lastIndexOf('.') + 1);
        // Resolve the guard's simple name INSIDE the switch subject's choice OPTION TREE
        // first — namespace-correct by construction (the cp1 catch: a workspace
        // simple-name lookup resolved cdm6 QAC's `Index` to fpml.consolidated's Index —
        // the ingest models share the workspace — and injected the WRONG-namespace
        // IndexDeepPathUtil; the Seat-1 #395 OBS-2 collision, live). Non-choice subjects
        // (the EXTENDS/instanceof family) keep the workspace fallback.
        RNode caseParent = owningCase.parent();
        if (caseParent instanceof com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr sw
                && compiler.getGeneratorModel().workspace() != null) {
            RExpression subjExpr = HandlerHelper.orSyntheticImplicit(sw.argument(), sw);
            com.regnosys.rosetta.ast.functions.RShortcut shortcut =
                    subjExpr instanceof RSymbolReference sr
                            && sr.symbol().orElse(null)
                                    instanceof com.regnosys.rosetta.ast.functions.RShortcut sc2
                            ? sc2 : null;
            RType subjT = ControlFlowHandler.choiceSubjectType(subjExpr, shortcut, compiler);
            if (subjT instanceof com.regnosys.rosetta.types.RChoiceTypeRef choiceRef) {
                RType hit = findChoiceOptionBySimpleName(choiceRef, simple, compiler,
                        new java.util.HashSet<>());
                if (hit != null) {
                    return hit;
                }
            }
        }
        // facet guardNamespaceAwareResolve (PR #396, Copilot R1 — the Seat-1 OBS-2
        // seat): the NON-choice-subject fallback prefers a namespace-consistent
        // candidate (the guard's qualifier segments, then the case's own model
        // namespace) over the raw first-wins simple-name lookup; a unique candidate
        // resolves identically to the pre-hardening bytes.
        RType nsAware = ChoiceSwitchSupport.resolveGuardRTypeNamespaceAware(
                qn, owningCase, compiler.getGeneratorModel());
        if (nsAware != null) {
            return nsAware;
        }
        return compiler.getGeneratorModel().resolveTypeByName(simple).orElse(null);
    }

    /**
     * facet choiceDeepNavLadderArm (PR #396): DFS a choice's option tree (declaration
     * order, nested choices recursed) for the option whose alias-stripped type name
     * matches {@code simple} — the namespace-correct case-guard resolution (a case
     * type IS an option of the switch subject's choice, the type-name law).
     */
    static RType findChoiceOptionBySimpleName(com.regnosys.rosetta.types.RChoiceTypeRef choice,
            String simple, ExpressionCompiler compiler, java.util.Set<String> visited) {
        if (!visited.add(choice.name()) || compiler == null
                || compiler.getGeneratorModel() == null) {
            return null;
        }
        // Derive option types from the AST node — a typeCall-resolved choice ref
        // carries an EMPTY options() list (the T396A cp5 read).
        com.regnosys.rosetta.ast.types.RChoice ast = choice.astNode();
        if (ast == null) {
            return null;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        java.util.List<RType> optTypes = new java.util.ArrayList<>();
        for (var astOpt : ast.options()) {
            if (astOpt.typeCall() == null) {
                continue;
            }
            RType optT = gm.resolveTypeCall(astOpt.typeCall());
            optT = HandlerHelper.stripAliases(optT);
            if (optT != null) {
                optTypes.add(optT);
            }
        }
        for (RType optT : optTypes) {
            if (simple.equals(optT.name())) {
                return optT;
            }
        }
        for (RType optT : optTypes) {
            if (optT instanceof com.regnosys.rosetta.types.RChoiceTypeRef nested) {
                RType hit = findChoiceOptionBySimpleName(nested, simple, compiler, visited);
                if (hit != null) {
                    return hit;
                }
            }
        }
        return null;
    }

    private static boolean disguisedChainProvesMulti(REnumValueRef evr, ExpressionCompiler compiler) {
        if (evr == null || compiler == null || compiler.getTypeUtil() == null
                || !evr.enumeration().isEmpty()) {
            return false;
        }
        String headName = evr.enumName();
        String leafName = evr.valueName();
        if (headName == null || leafName == null) {
            return false;
        }
        RDataType itemType = implicitItemDataTypeOrInferred(evr, compiler);
        if (itemType == null) {
            return false;
        }
        RAttribute headAttr = HandlerHelper.findAttributeOnDataType(itemType, headName);
        if (headAttr == null) {
            return false;
        }
        if (attrIsMulti(headAttr)) {
            return true;   // multi head — the chain is multi regardless of the leaf
        }
        RDataType headType = attributeToDataType(headAttr, compiler);
        if (headType == null) {
            return false;
        }
        return attrIsMulti(HandlerHelper.findAttributeOnDataType(headType, leafName));
    }

    /**
     * facet ruleOutputCardFormSeats (seat 8): the rule-output cardinality read with
     * the choice-option overlay — the ONE predicate every rule-output-surface
     * consumer reads (the #367 shared-predicate law: the RuleGenerator signature
     * back-fill, the bare/lambda rule-call MapperC wraps and the then-terminal
     * consults may never disagree, or a List-signatured rule renders a scalar
     * body). Engine-first: a {@code getRuleBodyCardinality == MULTI} answer is
     * final, so every re-pointed consumer is byte-neutral by construction wherever
     * the overlay is silent. The overlay's FIRST channel proves a SINGLE-read body
     * MULTI on the choice-option channel the engine's {@code disguisedChainCardinality}
     * deliberately under-fires (its documented {@code resolvedChoiceOption}
     * conservative-SINGLE fall-through; the binding is channel-exclusive — the
     * engine guards the attribute-chain / input-feature bindings with
     * {@code resolvedChoiceOption().isEmpty()}, so the frozen arms are untouched
     * and the #454-probed attribute-chain boundary cannot be reached), via the
     * SAME {@link #disguisedChainProvesMulti} walk the render's own
     * {@code mapSingleToList}/{@code mapC} selection used (the same-walk law: the
     * drr 7.x {@code then extract payout -> CommodityPayout} spine renders a
     * {@code MapperC<CommodityPayout>} pipeline TODAY while the whole-output
     * surface reads single). Upstream mirror: {@code CardinalityProvider
     * .caseFeatureCall} ORs the head ({@code isFeatureMulti(feature) ||
     * isMulti(receiver)}) with NO separate choice-option channel — a multi-head
     * option chain IS multi upstream, so a green single-form golden cannot carry
     * one (the #325/#347/#384/#394 mirror argument). The engine computer stays
     * FROZEN — this is a generator-side consult-seat overlay, monotone add-only,
     * SINGLE→MULTI on the proven shape only.
     *
     * <p>The SECOND channel is {@link #defaultSpineProvesMulti} (facet
     * {@code ruleOutputDefaultSpineMulti}, seat 29 law 2) — a {@code default} on the
     * body's composition spine whose LEFT operand proves MULTI. Same posture on every
     * axis: engine-first, monotone add-only, decline-by-default, and the engine
     * computer untouched. The two channels are independent disjuncts; neither can
     * demote a row the other or the engine already answers MULTI.
     */
    public static boolean ruleOutputProvesMulti(RExpression expr, ExpressionCompiler compiler) {
        return ruleOutputProvesMulti(expr, compiler, null);
    }

    /**
     * seat 30 guard 1 — the recursion-carrying variant of
     * {@link #ruleOutputProvesMulti(RExpression, ExpressionCompiler)}.
     * {@code defaultSpineVisited} is the ON-PATH {@link RRule} set threaded in from a
     * {@link #chainProvesMulti} hand-off, or {@code null} at a fresh public entry (the
     * set is allocated lazily at the first callee descent).
     *
     * <p><b>Only the DEFAULT channel takes it.</b> {@link #choiceOptionSpineProvesMulti}
     * keeps its own freshly-allocated set exactly as before: the two channels are
     * independent disjuncts, and sharing one set between them would let one channel's
     * descent decline a rule the other has not visited — a behaviour change on ACYCLIC
     * input, which is precisely what this guard must not have.
     */
    private static boolean ruleOutputProvesMulti(RExpression expr, ExpressionCompiler compiler,
            java.util.Set<Object> defaultSpineVisited) {
        if (expr == null || compiler == null || compiler.getGeneratorModel() == null) {
            return false;
        }
        if (compiler.getGeneratorModel().workspace()
                .getRuleBodyCardinality(expr) == ExpressionCardinality.MULTI) {
            return true;
        }
        if (choiceOptionSpineProvesMulti(expr, compiler,
                java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()))) {
            return true;
        }
        return defaultSpineProvesMulti(expr, compiler, defaultSpineVisited);
    }

    /**
     * facet ruleOutputDefaultSpineMulti (seat 29, law 2): the SECOND channel of
     * {@link #ruleOutputProvesMulti} — a reporting rule whose body spine carries a
     * {@code default} whose LEFT operand proves MULTI has a MULTI output. Upstream
     * {@code CardinalityProvider}'s binary {@code case "default"} keeps the joined
     * operand cardinality (the same join {@code chainProvesMulti}'s own
     * {@code RDefaultExpr} arm already mirrors at the #362/seat-5 seat), but the
     * engine's whole-output {@code getRuleBodyCardinality} reads the drr 7.x emir
     * {@code IndicatorOfTheUnderlyingIndex} body SINGLE because the {@code default}
     * sits in the SECOND-TO-LAST extract body, not at the tail. The walk therefore
     * descends the cardinality-COMPOSING edges ({@code RThenExpr} argument+body,
     * {@code RExtractExpr} argument+body, {@code RFilterExpr} argument) and tests the
     * FIRST {@code default} it reaches anywhere on that spine — exactly the
     * counterfactual the probe computed, not a re-derivation of it.
     *
     * <p><b>MEASURED (PROBE29-F10sig, one instrumented D11 round per route over 25
     * cells, 21,341 rows each, route-identical).</b> {@code declaredMulti=false &&
     * cfMulti=true} is <b>4 rows / 1 distinct rule / ALL BAND / ZERO green</b> — the
     * emir producer at drr 7.{0,1,2,3}.0. The reverse polarity is inert by
     * construction: this predicate is monotone SINGLE&#8594;MULTI, so none of the 1,383
     * {@code declaredMulti=true} rows can move.
     *
     * <p><b>The {@code chainProvesMulti(left)} conjunct is LOAD-BEARING, measured.</b>
     * Gate G3 (a {@code default} seen on the spine, no left test) admits <b>95 rows,
     * of which 87 are GREEN across 16 green rules</b> ({@code OptionPremiumCurrency},
     * {@code QuantityUnitOfMeasureLeg1/2}, {@code PriceUnitOfMeasure},
     * {@code CollateralPortfolioIndicator}, {@code SettlementTerms},
     * {@code CustomBasketCodeIdentifier}, {@code QuantitySchedule},
     * {@code CustomSchedule}, {@code DeliveryType}, {@code FixingDate},
     * {@code NotionalQuantity}, …). The left test is the gate, and its in-BAND decline
     * witness is jfsa {@code UnderlyingIndexIndicator} at drr 7.0–7.3
     * ({@code leftKind=RListOpExpr leftProvesMulti=false} — its left is
     * {@code … -> identifier first}, and {@code chainProvesMulti}'s own
     * {@code RListOpExpr} arm declines every collapsing op). That band file stays a
     * mismatch for the meta-join family; this law must never move it.
     *
     * <p><b>The collapse guard, also measured.</b> The bare walk admits a
     * {@code default} even when a COLLAPSING list-op follows it on the spine, and the
     * shape is real: at {@code FER-thenExtractTerminal} the 12 admit candidates split
     * 4 BAND (the emir producer, no {@code RListOpExpr} on its leaf trail) + 8 GREEN
     * (drr 7.x FUNCTION {@code GetUniqueTransactionIdentifier} and
     * {@code UniqueSwapIdentifierForValuation}, both {@code (… default …) then distinct
     * then only-element} — {@code leftProvesMulti=true} on all 8, so the left test does
     * NOT decline them), route-identical. Declining any leaf that is a collapsing
     * list-op removes exactly those 8 and keeps all 4, and it removes zero rows at
     * {@code RG-outputCardBackfill} (the admit set carries no {@code RListOpExpr} leaf
     * on either route). The classification mirrors {@code chainProvesMulti}'s
     * {@code RListOpExpr} arm on the {@code RListOpExpr} kinds — {@code flatten}
     * produces a list, {@code distinct}/{@code reverse} are element-preserving, the
     * other list-ops collapse. <b>Seat 30 guard 2 closed FOUR MORE of that universe —
     * not all of it.</b> {@link #collapsesToSingle} named ten kinds at seat 30 (the
     * {@code RListOpExpr} arm plus four unary node kinds); the collapsing-to-SINGLE
     * expression kinds it did NOT name at seat 30 were {@code RExistenceExpr},
     * {@code RCardinalityCheckExpr}, {@code ROnlyExistsExpr} and {@code RJoinExpr} —
     * every one of which reads SINGLE and would be an admit this leaf test lets
     * through — plus {@code RToStringExpr} and {@code RConversionExpr}, whose
     * cardinality is per-operator and needs a probe before either is classified.
     * <b>Those six were BANKED to S31 and CLOSED there</b> (the seat-31 kind extension:
     * {@link #collapsesToSingle} now names all six, with a decline-lock fixture per kind --
     * {@code Law2GuardsSeatTest} e1-e6 -- and an unchanged matrix digest). The predicate
     * must never be inverted to "anything not
     * element-preserving collapses" — that would decline every spine.
     * The four this seat DID close are the unary collapsers that are their own node
     * kinds — {@code RMinExpr},
     * {@code RMaxExpr}, {@code RReduceExpr}, {@code RCountExpr}, all four read SINGLE by
     * {@code CardinalityComputer} — used to pass this leaf test unrejected, so a
     * {@code multi default X then min} spine ADMITTED and the rule took a
     * {@code List}-signatured {@code ReportFunction} over a scalar body: an
     * ENTERING-class defect, not merely an unclaimed heal. Zero carriers on all 25
     * cells, so the classification now lives in {@link #collapsesToSingle}, pinned by a
     * decline-lock fixture per collapser and by the unchanged matrix digest.
     * {@code RSortExpr} is deliberately NOT in that set — {@code sort} is
     * element-preserving (the {@code distinct}/{@code reverse} case at a different node
     * kind), and the fixture carries an over-widen tripwire that fails if it is added.
     * Without the guard the green safety of those two functions would rest entirely on
     * the rule-scoping of the seven {@code ruleOutputProvesMulti} call sites the probe
     * did not instrument; with it, it rests on the predicate itself.
     *
     * <p><b>The callee descent</b> is the step that carries the consumers. A consumer's
     * own spine holds no {@code default} at all — it delegates
     * ({@code then extract emir.IndicatorOfTheUnderlyingIndex}) — so the walk recurses
     * into an {@code RSymbolReference} bound to an {@code RRule}, ON-PATH-guarded on
     * that recursion. <b>Seat 30 guard 1</b> made the guard total: a
     * {@code default}-bearing leaf hands off to {@code chainProvesMulti}, which now
     * threads this same on-path set through every composing edge of its walk and into
     * its {@code RRule} arm's {@code ruleOutputProvesMulti} hand-off. Before that the
     * hand-off allocated a FRESH set on every re-entry and a grammar-legal cyclic rule
     * ref recursed unboundedly through the mutual path {@code defaultSpineProvesMulti →
     * chainProvesMulti → ruleOutputProvesMulti → defaultSpineProvesMulti}. The shape is
     * corpus-invisible on all 25 cells (no cyclic rule refs exist), so it is pinned on a
     * two-rule-cycle fixture and its byte-neutrality shown on the matrix digest. The
     * on-path discipline (add descending, remove in {@code finally}) is what makes the
     * threading neutral BY CONSTRUCTION — see {@link #chainProvesMulti(RExpression,
     * ExpressionCompiler, java.util.Set)}. Measured at the
     * two seats that hold the callee {@code RRule} directly: {@code cfCalleeMulti=true}
     * is <b>56 rows, all {@code callee=IndicatorOfTheUnderlyingIndex}, all BAND, zero
     * green</b>, and the flip set's transitive closure over the {@code callee} edge is
     * {emir, esma, fca} × drr 7.{0,1,2,3}.0 POJO = 12 rule classes, ALL BAND, closed
     * three ways (the probe's flip-set scan, the sig-seat depth-2 join, and a
     * {@code .rosetta} grep finding exactly 5 referencing sources).
     *
     * <p><b>LAW 69 — one predicate, seven consumer seats.</b>
     * {@code RuleGenerator}'s output back-fill (the {@code ReportFunction<I, List<X>>}
     * signature and every {@code List<X>} that follows it),
     * {@code ReferenceHandler.renderImplicitRuleInvocation}'s {@code MapperC} wrap,
     * {@code CollectionHandler.isBodyMulti}'s rule arm (hence
     * {@code mapSingleToList}), the {@code thenArgN} decl (via
     * {@code chainProvesMulti}'s {@code RRule} arm, which ORs this predicate in), the
     * derived next-step {@code mapItem}, the whole-output {@code .getMulti()} terminal
     * and the bare-rule-then fast path ALL read this one method. They may never
     * disagree — a {@code List}-signatured rule that renders a scalar body does not
     * compile — and one disjunct here keeps them in step by construction.
     *
     * <p><b>LAW 74 — the repair the byte count cannot see.</b> The esma/fca report
     * types declare {@code override indicatorOfTheUnderlyingIndex IndexEnum (0..*)},
     * so the generated {@code ESMAEMIRTransactionReport} builder's ONLY setter overload
     * takes {@code List<IndexEnum>} while today's fork rule returns {@code IndexEnum}:
     * {@code ESMAEMIRTradeReportFunction} and {@code FCAUKEMIRTradeReportFunction} do
     * not compile in any of the four drr 7.x cells. Their call site is
     * {@code set…(rule.evaluate(input))} before and after, so those 8 GREEN files move
     * ZERO bytes and simply start compiling (the seat's javac PRE/POST measures it).
     *
     * <p><b>What this law does NOT heal (LAW 80).</b> The 8 esma/fca consumer cells go
     * WHOLE. The 4 emir PRODUCER cells do NOT: their 31-line delta also carries a
     * meta-join element read, an ite-hoist decl form and a multi-{@code default} render
     * arm owned by three other families, so the file stays a mismatch and contributes
     * zero file heals.
     */
    private static boolean defaultSpineProvesMulti(RExpression expr,
            ExpressionCompiler compiler, java.util.Set<Object> visited) {
        if (expr == null) {
            return false;
        }
        // The probe's bounded worklist verbatim: expand ONLY the composing edges and
        // record the LEAF stages. The IR is a tree (argument/body descend only), so it
        // terminates; the 256 cap is belt-and-braces and matches the instrument.
        java.util.List<RExpression> queue = new java.util.ArrayList<>();
        queue.add(expr);
        java.util.List<RExpression> leaves = new java.util.ArrayList<>();
        for (int i = 0; i < queue.size() && i < 256; i++) {
            RExpression node = queue.get(i);
            if (node == null) {
                continue;
            }
            if (node instanceof RThenExpr then) {
                queue.add(then.argument());
                RInlineFunction thenBody = then.body().orElse(null);
                queue.add(thenBody == null ? null : thenBody.body());
                continue;
            }
            if (node instanceof RExtractExpr extract) {
                queue.add(extract.argument());
                queue.add(extract.body() == null ? null : extract.body().body());
                continue;
            }
            if (node instanceof RFilterExpr filter) {
                queue.add(filter.argument());
                continue;
            }
            leaves.add(node);
        }
        // The collapse guard, evaluated over the WHOLE spine before any admit: a
        // collapsing list-op anywhere downstream makes the output single whatever the
        // default proves. The kind split mirrors chainProvesMulti's RListOpExpr arm.
        for (RExpression leaf : leaves) {
            if (collapsesToSingle(leaf)) {
                return false;
            }
        }
        // Admit on the FIRST default whose LEFT proves multi — the instrument recorded
        // the FIRST RDefaultExpr only, and 16 rows per route carry 4-9 defaults on one
        // spine, so testing every default would widen the gate past what was measured.
        for (RExpression leaf : leaves) {
            if (leaf instanceof RDefaultExpr defaultLeaf) {
                // seat 30 guard 1: the hand-off carries the on-path set, so a cyclic
                // rule ref closes the loop at chainProvesMulti's RRule arm instead of
                // re-entering this predicate with a fresh set forever.
                return defaultLeaf.rawLeft() != null
                        && chainProvesMulti(defaultLeaf.rawLeft(), compiler, visited);
            }
        }
        // No default on this spine: descend into the FIRST delegated rule — the step
        // that carries the esma/fca consumers. `visited` is the ON-PATH rule set: it is
        // allocated lazily here (the first and only hop that adds to it) and removed on
        // the way back up, so it holds exactly the rules this descent is inside. On an
        // acyclic rule graph that is unobservable — this branch returns immediately
        // after the recursion — and on a cyclic one it is the termination proof.
        for (RExpression leaf : leaves) {
            if (leaf instanceof RSymbolReference ref
                    && ref.symbol().orElse(null) instanceof RRule rule
                    && rule.expression().isPresent()) {
                java.util.Set<Object> onPath = visited != null
                        ? visited
                        : java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
                if (!onPath.add(rule)) {
                    return false;
                }
                try {
                    return defaultSpineProvesMulti(rule.expression().get(), compiler, onPath);
                } finally {
                    onPath.remove(rule);
                }
            }
        }
        return false;
    }

    /**
     * facet ruleOutputDefaultSpineMulti (seat 30 guard 2): does this spine LEAF collapse
     * a list to a single value? One definition of "collapsing", read by
     * {@link #defaultSpineProvesMulti}'s guard, split exactly as the engine's own
     * {@code CardinalityComputer} splits it:
     * <ul>
     *   <li>{@link RListOpExpr} — {@code flatten} PRODUCES a list and
     *       {@code distinct}/{@code reverse} are element-preserving (all three keep the
     *       operand's cardinality, which is {@code chainProvesMulti}'s own
     *       {@code RListOpExpr} arm term for term); {@code only-element},
     *       {@code first}, {@code last} and {@code sum} collapse.</li>
     *   <li>The unary collapsers that are their OWN node kinds — {@link RMinExpr},
     *       {@link RMaxExpr}, {@link RReduceExpr}, {@link RCountExpr}. The engine reads
     *       all four SINGLE; they are not {@link RListOpExpr}, so before seat 30 they
     *       passed the guard and a {@code multi default X then min} spine admitted.</li>
     *   <li>{@link RSortExpr} is NOT collapsing — {@code sort} is element-preserving and
     *       the engine reads it MULTI. Adding it here would be an over-widen; the seat's
     *       fixture carries the tripwire that catches exactly that.</li>
     * </ul>
     *
     * <p>Decline-only: every kind named here can only turn an {@code admit} into a
     * {@code decline}, never the reverse, so the guard cannot enter a file. Zero
     * carriers on all 25 cells — the seat's proof is the unchanged matrix digest.
     *
     * <p><b>The seat-30 banked gap is CLOSED here (v3.1 flip seat 31, the kind extension).</b>
     * The six further kinds the seat-30 javadoc named — {@link RExistenceExpr},
     * {@link RCardinalityCheckExpr}, {@link ROnlyExistsExpr}, {@link RJoinExpr},
     * {@link RToStringExpr} and {@link RConversionExpr} — joined the rejection. The
     * classification evidence is the engine's OWN read: {@code CardinalityComputer}'s global
     * arms are UNCONDITIONAL {@code SINGLE} for all six ({@code RJoinExpr} :91,
     * {@code RExistenceExpr} :95, {@code RCardinalityCheckExpr} :96, {@code ROnlyExistsExpr}
     * :97, {@code RToStringExpr} :98, {@code RConversionExpr} :102) — which also answers the
     * banked "to-string/conversion are per-operator" caution: the engine's cardinality read
     * has NO operator dimension for either kind. Each of the six carries its own decline-lock
     * fixture ({@code Law2GuardsSeatTest} e1–e6, all six MEASURED FAILING at the pre-law
     * head — the admit gap was real for every kind, none engine-answered — with e0 the
     * admit-path positive control). Still decline-only, still zero corpus carriers; and the
     * standing warning holds: NEVER invert the predicate to "anything not element-preserving
     * collapses" — the leaf population is dominated by kinds that prove NOTHING about
     * cardinality, and the inversion would decline every spine ({@code d5} remains the
     * over-widen tripwire). The set is still not the whole SINGLE-reading universe:
     * {@code CardinalityComputer} :85–:90 ({@code RComparison}/{@code REquality}/{@code RLogical}/
     * {@code RArithmetic}/{@code RContains}/{@code RDisjoint}) also read unconditional SINGLE and
     * are deliberately out of scope — no pipe/tail form, never banked (#603 review, L2-06).
     */
    private static boolean collapsesToSingle(RExpression leaf) {
        if (leaf instanceof RListOpExpr lop) {
            return lop.op() != ListOp.FLATTEN
                    && lop.op() != ListOp.DISTINCT
                    && lop.op() != ListOp.REVERSE;
        }
        return leaf instanceof RMinExpr
                || leaf instanceof RMaxExpr
                || leaf instanceof RReduceExpr
                || leaf instanceof RCountExpr
                || leaf instanceof RExistenceExpr
                || leaf instanceof RCardinalityCheckExpr
                || leaf instanceof ROnlyExistsExpr
                || leaf instanceof RJoinExpr
                || leaf instanceof RToStringExpr
                || leaf instanceof RConversionExpr;
    }

    /**
     * The NARROW spine walk behind {@link #ruleOutputProvesMulti} (the seat-5
     * Rung-F walk posture — recognize the carrier spine, decline everything
     * else): descending left-nested {@code then} pipes, a stage PROVES the body
     * multi iff its body is a {@code resolvedChoiceOption}-bound ref that
     * {@link #disguisedChainProvesMulti} proves (the head attribute's own
     * declared cardinality, never the receiver-item cardinality); a
     * {@code RConstructorExpr} stage (per-item, non-collapsing) passes the walk
     * through to its argument; a bare {@code then} passes through; a rule ref
     * recurses into the callee's body (the engine's thenAware-RRule recursion
     * mirrored, identity-guarded — the cftc/csa delegation cascades). EVERYTHING
     * else — a collapsing tail ({@code first}/{@code only-element}/aggregates),
     * a plain navigation, a function call — returns false and the engine's
     * answer stands.
     */
    private static boolean choiceOptionSpineProvesMulti(RExpression expr,
            ExpressionCompiler compiler, java.util.Set<Object> visited) {
        if (expr == null) {
            return false;
        }
        if (expr instanceof REnumValueRef evr && evr.resolvedChoiceOption().isPresent()) {
            return disguisedChainProvesMulti(evr, compiler);
        }
        if (expr instanceof RExtractExpr ex) {
            // a `then extract X` stage's body is an RExtractExpr whose OWN inline
            // body carries the expression (the engine's extractCardinality access
            // pattern) — the proof lives on the inner expression; the extract's
            // argument inside a then stage is the piped implicit item, never the
            // upstream spine (the spine recursion happens at the RThenExpr level).
            RExpression inner = ex.body() == null ? null : ex.body().body();
            return choiceOptionSpineProvesMulti(inner, compiler, visited);
        }
        if (expr instanceof RThenExpr then) {
            RExpression body = then.body().map(RInlineFunction::body).orElse(null);
            if (body == null) {
                return choiceOptionSpineProvesMulti(then.argument(), compiler, visited);
            }
            if (choiceOptionSpineProvesMulti(body, compiler, visited)) {
                return true;
            }
            if (body instanceof RConstructorExpr
                    || (body instanceof RExtractExpr bx && bx.body() != null
                        && bx.body().body() instanceof RConstructorExpr)) {
                return choiceOptionSpineProvesMulti(then.argument(), compiler, visited);
            }
            return false;
        }
        if (expr instanceof RSymbolReference ref
                && ref.symbol().orElse(null) instanceof RRule rule
                && rule.expression().isPresent()) {
            if (!visited.add(rule)) {
                return false;
            }
            return choiceOptionSpineProvesMulti(rule.expression().get(), compiler, visited);
        }
        return false;
    }

    /**
     * facet disguisedOutputMeta (PR #287): the value-level meta wrapper ({@link RJavaWithMetaValue})
     * a DISGUISED-chain whole-output terminal produces — the output-deref analogue of #286's
     * {@link #recoverDisguisedChainMeta} (which recovers the RECEIVER-coercion wrapper). A drr rule
     * body {@code <head> -> <feature> then extract [partyId -> identifier only-element]} whose SECOND
     * extract body is a DISGUISED 2-name chain ({@code partyId -> identifier}, an {@link REnumValueRef})
     * collapsed by an {@code only-element}: after #286 inserts the receiver deref, golden ALSO hoists
     * the terminal meta wrapper local and null-guards {@code output = <wrapper>.getValue();}, but the
     * fork assigns the {@code FieldWithMetaString} wrapper bare to the {@code String} output
     * (NON_COMPILING). {@link #tryTerminalMetaMapperType} declines because {@link #terminalNavAttr}'s
     * {@link #resolveDisguisedFeature(REnumValueRef, ExpressionCompiler, java.util.Set)} cannot resolve
     * the disguised head ({@code partyId}) as a feature of the second extract's item — the SAME
     * blindness #286 fixed for the receiver seat, now at the output terminal. Re-roots the disguised
     * chain onto its own item type (descending the {@code only-element} list-op) and returns the leaf's
     * wrapper, or {@code null}.
     *
     * <p><b>Green-safe by construction:</b> the fork's bare {@code output = <wrapper>.get()} (a meta
     * wrapper into a bare-value output) does not compile, so every carrier is an already-waivered
     * NON_COMPILING mismatch; recovering the wrapper only flips a waivered file. The caller is
     * RULE-scoped + single-output-gated (FUNCTION-byte-neutral, #232), and
     * {@code renderMetaValueDerefOrNull}'s {@code outputTypeName.equals(metaSimple)} guard declines
     * when the output IS the wrapper type.
     */
    public static RJavaWithMetaValue recoverDisguisedTerminalMetaWrapper(RExpression outBody,
            ExpressionCompiler compiler) {
        RAttribute leafAttr = resolveDisguisedChainLeafAttr(outBody, compiler, true);
        return leafAttr == null ? null : metaWrapperOf(leafAttr, compiler);
    }

    /**
     * facet collapsedPipeMeta (PR #290): recover the meta wrapper of a then-chain's UPSTREAM
     * PRODUCER body, when the whole-output terminal is a STANDALONE single-collapse list-op
     * ({@code then last}/{@code first}/{@code only-element}) over the rebound pipe — the collapse
     * argument is an {@link RImplicitVariable}, so {@link #terminalNavAttr} descends it and returns
     * null, and the producer's meta is invisible to {@link #recoverInnerRuleMetaWrapper}'s walker
     * (it STOPS at the disguised-chain producer body, whose 1-arg
     * {@link #resolveDisguisedFeature(REnumValueRef)} cannot re-root the chain).
     *
     * <p>The drr rule body {@code … then extract assignedIdentifier -> identifier then last}
     * (OriginalSwapUSI / UniqueSwapIdentifier, cftc rewrite trade) produces a MULTI
     * {@code MapperC<FieldWithMetaString>} thenArg, collapsed to a single {@code FieldWithMetaString}
     * by the downstream {@code last}; golden hoists the wrapper + null-guards
     * {@code output = fieldWithMetaString.getValue()}, the fork assigns the wrapper bare to the
     * {@code String} output (NON_COMPILING). Recover the leaf wrapper from {@code producerBody}: a
     * plain nav leaf via {@link #tryTerminalMetaMapperType}, a DISGUISED 2-name chain
     * ({@code head -> leaf}) via {@link #resolveDisguisedChainLeafAttr} with
     * {@code descendListOps == false} — which SKIPS the #287 multi-head gate (a MULTI head/leaf is a
     * List output deref'd element-wise THERE), correct here because the caller's downstream
     * single-collapse terminal already guarantees a SINGLE output. Returns {@code null} for a
     * non-meta / unresolvable producer.
     */
    public static RJavaWithMetaValue recoverCollapsedProducerMeta(RExpression producerBody,
            ExpressionCompiler compiler) {
        if (producerBody == null || compiler == null || compiler.getTypeUtil() == null) {
            return null;
        }
        JavaType recovered = tryTerminalMetaMapperType(producerBody, compiler);
        if (recovered != null) {
            JavaType item = compiler.getTypeUtil().getItemType(recovered);
            if (item instanceof RJavaWithMetaValue meta) {
                return meta;
            }
        }
        RAttribute leafAttr = resolveDisguisedChainLeafAttr(producerBody, compiler, false);
        return leafAttr == null ? null : metaWrapperOf(leafAttr, compiler);
    }

    /**
     * facet disguisedChainMetaRecovery green-safety gate (PR #286): the recovered meta wrapper's
     * VALUE type ({@code leafAttr}'s declared data type) must carry the feature THIS receiver
     * navigates next. A correct recovery (wrapper = the item's true type) always passes (the
     * wrapper's value type IS the receiver's element type, on which the next feature exists); a
     * mis-resolution that recovered the wrong stage's meta DECLINES, so no spurious deref is
     * inserted on a green carrier. Conservative: returns {@code false} when the next navigation
     * feature cannot be determined (no enclosing nav off the receiver).
     */
    private static boolean metaValueHasNavFeature(RAttribute leafAttr, String navFeature,
            ExpressionCompiler compiler) {
        if (navFeature == null) {
            return false;
        }
        RDataType valueType = attributeToDataType(leafAttr, compiler);
        if (valueType == null) {
            return false;
        }
        return HandlerHelper.findAttributeOnDataType(valueType, navFeature) != null;
    }

    /**
     * facet metaCoercionRule LAMBDA-channel (PR #285): the META-wrapper {@code MapperS} type of an
     * implicit-item EVALUATE-ARG — the {@code item.get()} passed to a function call NESTED inside a
     * rule-body {@code .mapSingleToItem(item -> MapperS.of(<fn>.evaluate(item.get(), …)))} lambda
     * where the callee wants the bare value (golden block-hoists {@code final <Wrapper> w =
     * item.get();} + derefs {@code (w == null ? null : w.getValue())}). The OtherPaymentPayer/Receiver
     * (#267-deferred LAMBDA_CHANNEL) family. The implicit item's compiled type erases to null, so
     * {@code ReferenceHandler.tryMetaDerefArg}'s compiled-type + alias/terminal fallbacks decline;
     * this recovers the wrapper from the owning then-chain's terminal navigation the SAME way
     * {@link #implicitItemMetaMapperType} does (the receiver-coercion path) — {@code implicitItemArgument}
     * to the owning extract/then ARGUMENT, then {@code thenOwnerArgument} through the then-pipe, then
     * the #264/#270 walker {@link #recoverMetaFromExpr}. The caller (tryMetaDerefArg)
     * applies it ONLY when the compiled type is null AND gates the deref on the callee param being
     * the bare value type (mismatch) + a real coercion — so a non-meta item recovers null and a
     * matching param declines. Returns {@code MapperS<wrapper>} or {@code null}.
     *
     * <p>facet functionImplicitItemArgMeta (PR #340): the #285 RULE-scope gate DROPPED — the
     * walk machinery is path-agnostic and the green-safety argument is path-INDEPENDENT (the
     * fork's bare wrapper into a bare-value callee param never compiled on EITHER path, so
     * every carrier is an already-waivered NON_COMPILING mismatch — the #325 precedent for
     * dropping a findEnclosingRule gate). FUNCTION carrier: BarrierFromTriggerEvent drr
     * (`.mapSingleToItem(item -> MapperS.of(convertNonISOToISOCurrency.evaluate(item.get())))`
     * where the piped element is FieldWithMetaString and the callee wants String — golden
     * block-hoists the wrapper + derefs, exactly the rule-path form).
     */
    public static JavaType implicitItemArgMeta(RExpression arg, ExpressionCompiler compiler) {
        if (!(arg instanceof RImplicitVariable) || compiler == null || compiler.getTypeUtil() == null) {
            return null;
        }
        RExpression owningArg = implicitItemArgument(arg);
        if (owningArg == null) {
            return null;
        }
        RExpression chainArg = owningArg instanceof RImplicitVariable
                ? thenOwnerArgument(owningArg) : owningArg;
        if (chainArg == null) {
            return null;
        }
        RJavaWithMetaValue chainMeta = recoverMetaFromExpr(chainArg, compiler, 0, new HashSet<>());
        JavaTypeUtil tu = compiler.getTypeUtil();
        return (chainMeta != null) ? tu.wrap(tu.MAPPER_S, chainMeta) : null;
    }

    /**
     * facet metaPathShortForm (PR #348) → seat 21's metaFaceShortForm arm — upstream
     * {@code buildMapFunc}'s a->a meta-feature short form ({@code ExpressionGenerator.xtend:515-527}),
     * or {@code null} to decline (the getter path). AS SHIPPED at seat 21 — the #348 block this
     * replaced admitted scheme/reference only, MapperS receivers only and typed MapperS<String>;
     * every one of those statements is false now: (1) ADMISSION — G2 first: a resolved REAL attribute
     * named like a face declines (the shadowing law); then the PARSER's metaType binding
     * ({@code HandlerHelper.boundMetaType} — the name the linker bound to an exported
     * {@code metaType} root; the corpus declares SIX roots: id, key, reference, scheme, location,
     * address — no {@code template} root in any cell), OR, for scheme/reference ONLY, the #348/#433
     * wrapper proof (the compiled receiver's own MapperS item, else the {@code implicitItemArgMeta}
     * walk) — a parser-bound face needs NO wrapper proof ({@code !wrapperProven && !metaBound} is
     * the decline). (2) RESULT TYPE — upstream {@code featureCall}'s rule
     * ({@code mapperReceiverCode.expressionType.isMapperS && !isMulti}): a MapperS receiver yields
     * MapperS<String>, a MapperC receiver MapperC<String> (the emitted text is the same Mapper.map
     * either way); a null-typed receiver (a lambda's implicit item, a bare input) follows the receiver
     * EXPRESSION's inferred cardinality ({@code receiverExpressionIsMulti}: MULTI → MapperC), NOT a
     * fixed MapperS fallback. (3) RENDER — {@code .map("getMeta", a->a.getMeta()).map("get<Face>",
     * a->a.get<PojoProperty>())} through the ONE name table ({@code HandlerHelper.metaPojoPropertyName});
     * reference keeps its one-hop {@code getExternalReference} form. No new refs: the short form
     * carries no witness and no new imports (the receiver's refs flow through).
     */
    private static JavaStatementBuilder metaFeatureShortFormOrNull(RFeatureCall expr,
            JavaStatementBuilder receiverBuilder, ExpressionCompiler compiler,
            JavaStatementScope scope) {
        String feature = expr.featureName();
        // G2 FIRST — the shadowing law: a REAL attribute named like a face wins (upstream's scope
        // lists the type's attributes before the meta descriptions: cdm
        // LegacyValuationTimeDayAndTime's `location`, drr DTCC_DeliveryLocationRule's `location`).
        if (expr.resolvedFeature().isPresent()) {
            return null;
        }
        // v3.1 flip seat 21 — facet metaFaceShortForm (LAW 69/67): the admission is the PARSER's
        // binding of this read to the exported metaType (HandlerHelper.boundMetaType — carried onto
        // a synthesized nav by ReferenceHandler.synthesizeFeatureCall / the bare-name meta synth;
        // upstream `caseFeatureCall`: `feature instanceof RosettaMetaType` → metaCall, ExpressionGenerator
        // .xtend:776, 345-350, 515-527), or — for `scheme`/`reference` only — the #348/#433 wrapper
        // proof this arm has always carried (the class the parser's two-hop binder does not reach:
        // the `partyReference -> reference` family, 84 cdm sites). Every other name declines to the
        // getter path (the 1,368 matrix-wide attribute-form `Id`/`Address`/`Location` getters all
        // resolve a real attribute and never reach here — the seat-21 probe, G2 above). No name
        // list admits anything on its own.
        com.regnosys.rosetta.ast.types.RMetaType bound = HandlerHelper.boundMetaType(expr);
        boolean metaBound = bound != null && feature != null && feature.equals(bound.name());
        boolean schemeKind = "scheme".equals(feature);
        boolean referenceKind = "reference".equals(feature);
        if (!metaBound && !schemeKind && !referenceKind) {
            return null;
        }
        JavaTypeUtil tu = compiler.getTypeUtil();
        if (tu == null) {
            return null;
        }
        JavaType recvType = receiverBuilder.getExpressionType();
        JavaType item = null;
        boolean mapperC = false;
        if (recvType != null) {
            // upstream featureCall (:389-399): a MapperS receiver yields MapperS<String>, a MapperC
            // receiver MapperC<String> — the emitted TEXT is identical (Mapper.map on both); the
            // pre-seat MapperS-only gate (G3) is gone. [PROBE21M] over the 275 rows: 16 carriers are
            // MapperC-TYPED receivers (`filter quantity -> location` — FXLeg1/2, FXSwapLeg1/2), the
            // population the gate declined; the 8 `commodityPayouts -> key` carriers are NULL-typed
            // (a min-lambda item) and take the cardinality fallback below; of the 208 already-right
            // scheme/reference sites 153 are MapperS-typed and 55 null-typed (the implicit-item walk)
            // — all 208 byte-unchanged: the ring is the receipt, not the receiver type.
            if (!tu.isMapperS(recvType) && !tu.isMapperC(recvType)) {
                return null;
            }
            mapperC = tu.isMapperC(recvType);
            item = tu.getItemType(recvType);
        }
        if (!(item instanceof RJavaWithMetaValue)) {
            JavaType walked = implicitItemArgMeta(expr.receiver(), compiler);
            item = walked == null ? null : tu.getItemType(walked);
        }
        boolean wrapperProven = item instanceof RJavaWithMetaValue;
        if (!wrapperProven && !metaBound) {
            return null;
        }
        if (referenceKind && !(item instanceof RJavaReferenceWithMeta)) {
            return null;
        }
        if (recvType == null) {
            // a null-typed receiver (the implicit item of a min/max/filter lambda, a bare function
            // input): the result wrapper follows the receiver EXPRESSION's inferred cardinality
            // (a MULTI receiver → MapperC), single otherwise — the text is the same either way
            mapperC = receiverExpressionIsMulti(expr.receiver(), compiler);
        }
        String recv = HandlerHelper.render(receiverBuilder);
        // Leg-C #16 typing-channel heal (2026-07-18): upstream's buildMapFunc mints
        // each lambda param via lambdaScope.createUniqueIdentifier("a") — desired
        // `a`, ESCAPED (`_a`) while taken in the enclosing chain (GeneratorScope
        // computeActualNames). The pre-heal literal `a` shadowed an in-scope `a`
        // (the deep-path witness: a function input named `a` — javac
        // already-defined). Each map's param registers in its OWN fresh lambda
        // scope, exactly upstream's two independent lambdaScopes; at every corpus
        // seat nothing named `a` is in scope, so the deferred resolution mints the
        // same `a` bytes (the P348-M1 census). A scope-less caller keeps the
        // literal (the file's null-scope convention).
        String p1 = scope != null ? scope.registerDeferredLambdaParam("a") : "a";
        String text;
        if (referenceKind) {
            text = recv + ".map(\"getReference\", " + p1 + "->" + p1
                    + ".getExternalReference())";
        } else {
            // upstream buildMapFunc (:521-526): `.map("getMeta", a->a.getMeta()).map("get<Name>",
            // a->a.get<PojoProperty>())` — the scheme branch of old, generalised by ONE
            // substitution through the shared name table (scheme → getScheme byte-identical;
            // key/id → getExternalKey, location → getScopedKey, address → getReference,
            // template → getTemplate — the last two corpus-unexercised, emitted for fidelity).
            String p2 = scope != null ? scope.registerDeferredLambdaParam("a") : "a";
            String cap = Character.toUpperCase(feature.charAt(0)) + feature.substring(1);
            String prop = HandlerHelper.metaPojoPropertyName(feature);
            String propCap = Character.toUpperCase(prop.charAt(0)) + prop.substring(1);
            text = recv + ".map(\"getMeta\", " + p1 + "->" + p1
                    + ".getMeta()).map(\"get" + cap + "\", " + p2 + "->" + p2 + ".get" + propCap + "())";
        }
        JavaType metaResultType = mapperC ? tu.wrap(tu.MAPPER_C, tu.STRING) : tu.wrap(tu.MAPPER_S, tu.STRING);
        return JavaExpression.from(text, metaResultType,
                receiverBuilder.getRefs(), receiverBuilder.getStaticWildcardImports());
    }

    /**
     * seat 21 (facet metaFaceShortForm): the receiver EXPRESSION's inferred cardinality — the
     * result-wrapper fallback for a null-typed receiver of the meta short form. {@code false}
     * (single) when the workspace cannot type it.
     */
    private static boolean receiverExpressionIsMulti(RExpression receiver, ExpressionCompiler compiler) {
        GeneratorModel gm = compiler == null ? null : compiler.getGeneratorModel();
        if (gm == null || gm.workspace() == null || receiver == null) {
            return false;
        }
        ExpressionCardinality card = gm.workspace().getCardinality(receiver);
        return card == ExpressionCardinality.MULTI;
    }

    /**
     * The {@link REnumeration} a workspace-inferred type resolves to, or {@code null}
     * for an absent/MISSING/non-enum inference, a {@code null} argument, or the
     * stateless zero-arg compiler. The inferred-type read is the same one
     * {@code ConversionHandler.isEnumSource}'s first step performs (it types a
     * directly-declared symbol reference); this helper additionally surfaces the
     * resolved enum NODE so the {@code to-string} rendering can name it and register
     * its import ref.
     */
    static REnumeration inferredEnumeration(RExpression argument, ExpressionCompiler compiler) {
        if (argument == null || compiler == null || compiler.getGeneratorModel() == null) {
            return null;
        }
        RMetaAnnotatedType inferred =
                compiler.getGeneratorModel().workspace().getInferredType(argument);
        if (inferred != null && !inferred.isMissing()
                && inferred.type() instanceof REnumTypeRef enumRef) {
            return enumRef.astNode();
        }
        return null;
    }

    /**
     * Extract the {@link RDataType} referenced by an attribute's type call.
     * Returns {@code null} if the type call references a non-data-type (enum,
     * basic, record, alias) or is unresolved.
     */
    private static RDataType attributeToDataType(RAttribute attr) {
        if (attr.typeCall() == null) return null;
        Optional<RNode> referenced = attr.typeCall().referencedType();
        if (referenced.isEmpty()) return null;
        if (referenced.get() instanceof RDataType dt) return dt;
        return null;
    }

    // =========================================================================
    // gm-aware receiver-type resolution (facet choice_option_nav_witness)
    // =========================================================================
    //
    // The static resolveReceiverDataType / attributeToDataType chain above resolves a
    // receiver's data type via the AST referencedType() slot only. That slot is an RChoice
    // (not an RDataType) for a choice-typed receiver — so the static chain returns null for a
    // navigation through a CHOICE OPTION (`receiver -> OptionTypeName`; the feature is the
    // option's type name, its resolvedFeature is unset because a choice option is not an
    // RAttribute), losing the <Type> witness + the chained/owner lambda var for that step and
    // every step chained after it. Upstream (ExpressionGenerator.attributeCall) narrows a
    // choice receiver to its data-type projection
    // (`receiverRType instanceof RChoiceType ? asRDataType : (RDataType) receiverRType`).
    //
    // These gm-aware overloads mirror the static ones but, when the AST path yields no
    // RDataType, fall back to GeneratorModel.resolveTypeCall (by-name workspace resolution) and
    // narrow an RChoiceTypeRef via asRDataType() so the choice's options become findable
    // attributes — cascading witness + lambda recovery through the whole chain (including the
    // PR #152 `.get()` only-element collapse). STRICTLY ADDITIVE: the static AST fast-path is
    // tried first, so any receiver that resolved before resolves identically; only
    // previously-null (choice-option) cases now resolve. resolveMapMethod / navLeafFeatureMulti
    // also route through the gm-aware fallback (facet choice_nav_chain_typing — the original
    // "cardinality selection needs no choice narrowing" assumption was falsified by the cdm6
    // Qualify_* family: a multi feature DOWNSTREAM of a choice-option step lost its mapC, the
    // witness/cardinality split this banner's witness-only scoping created). The static
    // overloads remain in use by THREE sites: the compiler-less resolveLambdaVarName
    // RFeatureCall branch, its ONLY_ELEMENT owner-type walk, and resolveReceiverRType's
    // record-receiver typing (tryRecordFeatureNav) — each a decline-to-status-quo path
    // (missed recovery, never a wrong byte) whose output the probed population shows
    // resolving correctly.

    /**
     * gm-aware counterpart of {@link #fallbackResolveFeature(RFeatureCall)} — resolves a
     * choice-option navigation the static AST path cannot. Package-private so sibling
     * handlers reuse the SAME leaf walk (facet numeric_literal_typing:
     * {@code HandlerHelper.numericOperandKind} resolves a resolution-blind nav-chain
     * operand's leaf attribute through it).
     */
    static RAttribute fallbackResolveFeature(RFeatureCall expr, ExpressionCompiler compiler) {
        return fallbackResolveFeature(expr, compiler, null);
    }

    private static RAttribute fallbackResolveFeature(RFeatureCall expr, ExpressionCompiler compiler,
            Set<RNode> visited) {
        // v3.1 flip seat 1 (LADDER RETIREMENT) — authority-first: when the C1
        // authority slot bound this hop to a CHOICE OPTION, project THAT node
        // instead of re-deriving structurally. This is the choke point of the
        // COMPILER-CARRYING resolution path — every witness / import / map-arity /
        // lambda-naming consumer on it routes through here, so the one read flips
        // them all (the compiler-less static overload keeps its three documented
        // decline-to-status-quo users, per the gm-aware banner below). Every
        // non-option authority (empty, RAttribute — which agrees with
        // resolvedFeature() and is claimed before this fallback runs — meta types,
        // record features) falls through to the structural walk unchanged: the
        // flip's authority-first-with-legacy-fallback contract.
        RAttribute authorityOption =
                authorityChoiceOptionAttr(expr.resolvedFeatureNode().orElse(null), compiler);
        if (authorityOption != null) {
            return authorityOption;
        }
        RDataType receiverType = resolveReceiverDataType(expr.receiver(), compiler, visited);
        if (receiverType == null) return null;
        RAttribute direct = HandlerHelper.findAttributeOnDataType(receiverType, expr.featureName());
        if (direct != null) return direct;
        // facet witnessDrop (PR #207): a navigate-by-type choice-option step whose choice is the
        // receiver type's CHOICE SUPERTYPE — `type TransferableProduct extends Asset` where Asset is
        // a `choice`. RDataType.superType() is EMPTY for an extends-choice relationship (the resolved
        // choice node is held by RDataType.choiceSuperType(), a SEPARATE accessor), so the data-supertype
        // walk in HandlerHelper.findAttributeOnDataType NEVER reaches the option, and the witness drops
        // (`.map("getCommodity", ...)` not `.<Commodity>map(...)`). Recover the option by matching the
        // (type-named, capitalized) feature against the choice supertype's options BY TYPE NAME — the
        // option identity per RChoiceTypeRef.asRDataType (a choice option has no name field; its typeCall
        // IS its identity, projected to an attribute named by the option's type). Green-safe by
        // construction: the empty-witness form never compiled, so every carrier is already waivered.
        return findChoiceSuperOption(receiverType, expr.featureName(), compiler);
    }

    /**
     * facet choiceSuperOptionIdCarry (v3.1 flip seat 29, law 6): the COMPILER-carrying
     * overload - what all eight reachable call sites take (this class at the disguised-root,
     * implicit-item, case-narrowed, callable-head and {@code fallbackResolveFeature} rungs;
     * {@link ReferenceHandler} at the type-condition and bare-item re-root seats). It exists
     * for the same reason {@link #authorityChoiceOptionAttr} has one: the workspace must be
     * THREADED in - {@code RNode.workspace()} is {@code protected final}, so nothing outside
     * the AST package can recover a workspace from the {@link RDataType} or the option node
     * itself. A null compiler (or a compiler with no generator model) yields a null
     * workspace and therefore today's id-less copy - decline-to-status-quo, never a wrong
     * byte.
     */
    static RAttribute findChoiceSuperOption(RDataType type, String featureName,
            ExpressionCompiler compiler) {
        return findChoiceSuperOption(type, featureName, workspaceOf(compiler));
    }

    /**
     * The worker. With a workspace the option projects through
     * {@link #projectedOptionAttribute} (id + attach + phantom-parent + annotation refs);
     * without one it keeps the legacy id-less, detached copy below.
     */
    static RAttribute findChoiceSuperOption(RDataType type, String featureName,
            RWorkspace workspace) {
        Set<RDataType> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (RDataType t = type; t != null && visited.add(t); t = t.superType().orElse(null)) {
            RChoice choice = t.choiceSuperType().orElse(null);
            if (choice == null) {
                continue;
            }
            for (RChoiceOption opt : choice.options()) {
                RTypeCall tc = opt.typeCall();
                if (tc != null && featureName.equals(tc.typeName())) {
                    // facet choiceSuperOptionIdCarry (v3.1 flip seat 29, law 6): the ONE
                    // projection, shared with seats 1 and 13 - name, deep-copied typeCall,
                    // annotation refs, PLUS the option typeCall's linker-resolved
                    // referencedTypeId carried onto the copy and the copy attached to the
                    // workspace. The carried id IS the heal: it resolves through
                    // workspace.resolveTypeLike regardless of the shouldGenerate emission
                    // filter, so witnessSentinelTypeParam and addWitnessTypeRef stop
                    // hard-returning and the `<Asset>` witness registers its import.
                    // MEASURED domain (PROBE29-F29w): 24 rows in 4 band files (drr
                    // 7.0.0/7.1.0/7.2.0/7.3.0 GetBasketConstituents, feature=Asset, 6 rows
                    // each), ZERO green - and a LAW-74 repair (`cannot find symbol: class
                    // Asset` x4 cells). LAW 80: ZERO whole files - GetBasketConstituents
                    // stays multi-family and the import is 1 of its 6 hunks, so this law's
                    // corpus receipt is the import plus javac, never a band move.
                    // Green-safety is the seat-1 SAME_DENOTATION argument: wherever the
                    // by-name resolution already succeeded, the linker id denotes the SAME
                    // declaration, so the render cannot move.
                    if (workspace != null) {
                        return projectedOptionAttribute(opt, workspace);
                    }
                    RAttribute attr = new RAttribute();
                    attr.setName(tc.typeName());
                    attr.setTypeCall(RTypeCall.deepCopy(tc));
                    // Carry the option's annotation refs (name + qualifier only) onto the projected
                    // attribute — EXACTLY as RChoiceTypeRef.asRDataType does — so a meta-annotated choice
                    // option (`choice Underlier: Observable [metadata address ...]`) keeps its meta signal
                    // for the downstream MetaFieldGenerator.detectMetaKind consumers (the navigation
                    // witness / meta result type). FRESH nodes (not the option's own subtree) for the
                    // single-parent-contract reason the typeCall is deep-copied. (Byte-neutral for the
                    // shipped non-meta carriers; correctness future-proofing for a meta choice-super option.)
                    for (RAnnotationRef srcRef : opt.annotationRefs()) {
                        RAnnotationRef refCopy = new RAnnotationRef();
                        refCopy.setAnnotationName(srcRef.annotationName());
                        srcRef.qualifierName().ifPresent(refCopy::setQualifierName);
                        attr.annotationRefs().add(refCopy);
                    }
                    return attr;
                }
            }
        }
        return null;
    }

    /**
     * v3.1 flip seat 1 (the LADDER RETIREMENT): the C1 AUTHORITY slot's choice-option
     * binding ({@code resolvedFeatureNode}), projected onto the render's attribute
     * currency — name = the option's type name, a deep-copied typeCall, the option's
     * annotation refs — exactly {@link #findChoiceSuperOption}'s projection PLUS the
     * option typeCall's linker-resolved {@code referencedTypeId} carried onto the
     * copy. The carried id IS the heal: the structural projections
     * ({@code RChoiceTypeRef.asRDataType}, {@link #findChoiceSuperOption}) resolve
     * their id-less floating copies through {@code GeneratorModel.resolveFromWorkspace},
     * which filters by {@code shouldGenerate} — so an option type living in a
     * NON-generated dependency namespace (every drr cell navigating vendored CDM
     * types) never resolves: the witness import never registers
     * ({@code addWitnessTypeRef} declines — the E1 import-only mismatch class) and
     * every hop chained after the option loses typing (witness drop + raw-name
     * lambda params, the #570 census's drr 7.x in-band mass). The authority option
     * is a PARENTED, linker-resolved node, so the carried id resolves through
     * {@code workspace.resolveTypeLike} regardless of the generation filter — and in
     * same-workspace cells it resolves to the SAME declaration the name-search found
     * (the census's SAME_DENOTATION predicate), so green renders cannot move.
     * Returns {@code null} unless {@code authority} is an {@link RChoiceOption} with
     * a typeCall AND a workspace is reachable through the compiler — every other
     * shape stays on the legacy paths.
     *
     * <p><b>The copy is id-carried AND workspace-attached, as a pair, serving two
     * DISJOINT reader styles.</b> The attach serves the lazy direct readers: an
     * id-bearing DETACHED copy is a live grenade — {@code RTypeCall.referencedType()}
     * resolves through the node's attached workspace and THROWS for a detached
     * node, where the old id-less copies returned a harmless {@code Optional.empty()},
     * so carrying the id alone flips every direct reader (e.g. the static
     * {@code attributeToDataType}) from decline to refusal (measured on cdm 6.20.6
     * {@code Qualify_Commodity_Option_Cash}: the C0 boundary catch refused the whole
     * expression). The id serves {@code GeneratorModel.resolveTypeCall}
     * independently — it reads {@code referencedTypeId()} through the GENERATOR's
     * own workspace (the Phase X1 Gap #1 detached-copy design) and never consults
     * the node's attachment. ({@code attachToWorkspace} is public and pre-freeze
     * re-invocable by design.) No workspace → no claim.
     *
     * <p>Package-private since flip seat 2: {@code ReferenceHandler}'s bare-name
     * synthesis ({@code synthesizeImplicitItemBareNav}) claims the SAME projection
     * for the SYM-side authority slot ({@code RSymbolReference.resolvedFeatureNode})
     * — the a2 bare-option class is this mechanism one arm over.
     */
    static RAttribute authorityChoiceOptionAttr(RNode authority,
            ExpressionCompiler compiler) {
        return authorityChoiceOptionAttr(authority, workspaceOf(compiler));
    }

    /**
     * v3.1 flip seat 3: the workspace-direct claim overload —
     * {@code FunctionAliasHelper}'s typing walk (a different package, and the
     * analyzer path carries no {@code ExpressionCompiler}) claims the SAME
     * projection through its {@code GeneratorModel}'s workspace, so the
     * id+attach pair (LAW 62) stays implemented in exactly one place. No
     * workspace → no claim (the stateless unit path keeps today's walks).
     */
    public static RAttribute authorityChoiceOptionAttr(RNode authority,
            RWorkspace workspace) {
        if (!(authority instanceof RChoiceOption option)) {
            return null;
        }
        if (workspace == null) {
            return null; // stateless unit path — no resolver to attach; today's walks stand
        }
        return projectedOptionAttribute(option, workspace);
    }

    /** The generator's workspace, or {@code null} on the stateless unit path. */
    private static RWorkspace workspaceOf(
            ExpressionCompiler compiler) {
        return compiler == null || compiler.getGeneratorModel() == null ? null
                : compiler.getGeneratorModel().workspace();
    }

    /**
     * The seat's single projection: one choice option onto one render attribute,
     * with the id+attach pair applied. Shared by {@link #authorityChoiceOptionAttr}
     * (the hop's own claim) and {@link #authorityOptionDeclaredType}'s
     * nested-choice bridge (Copilot #571 R1: the {@code asRDataType()} projection
     * would have reintroduced id-less floating copies one level down — a
     * nested-choice hop resolving through the RECEIVER projection instead of its
     * own authority claim would have kept the cross-namespace failure).
     */
    private static RAttribute projectedOptionAttribute(RChoiceOption option,
            RWorkspace workspace) {
        RTypeCall sourceTypeCall = option.typeCall();
        if (sourceTypeCall == null) {
            return null;
        }
        RAttribute attr = new RAttribute();
        attr.setName(sourceTypeCall.typeName());
        RTypeCall copy = RTypeCall.deepCopy(sourceTypeCall);
        sourceTypeCall.referencedTypeId().ifPresent(copy::setReferencedTypeId);
        copy.attachToWorkspace(workspace);
        // Copilot #571 R2 (suppressed): phantom-parent the copy onto the OPTION so
        // requester-aware resolution keeps the source typeCall's model context — a
        // parentless requester ranks duplicate FQNs by registration order instead
        // of the PR #445 same-file/same-cell preference. The pointer is a field on
        // the fresh copy only (the option's child list is untouched — the same
        // phantom-parent convention synthesizeFeatureCall's synthetic nodes use);
        // setTypeCall below does not reparent.
        copy.setParent(option);
        attr.setTypeCall(copy);
        for (RAnnotationRef srcRef : option.annotationRefs()) {
            RAnnotationRef refCopy = new RAnnotationRef();
            refCopy.setAnnotationName(srcRef.annotationName());
            srcRef.qualifierName().ifPresent(refCopy::setQualifierName);
            attr.annotationRefs().add(refCopy);
        }
        return attr;
    }

    /**
     * v3.1 flip seat 1: the DECLARED type of the choice option the C1 authority
     * slot bound for an option-nav receiver ({@code underlier -> Observable}
     * feeding a chained hop) — an {@link RDataType} directly, or a nested-choice
     * option projected onto a bridge whose option attributes carry the SAME
     * id+attach pair as the hop-level claim ({@link #projectedOptionAttribute} —
     * Copilot #571 R1; {@code RChoiceTypeRef.asRDataType()}'s id-less floating
     * copies stay confined to the legacy structural paths). {@code null} for
     * every non-option authority — the receiver walk's existing arms run
     * unchanged (the fallback contract).
     */
    private static RDataType authorityOptionDeclaredType(REnumValueRef evr,
            ExpressionCompiler compiler) {
        if (!(evr.resolvedFeatureNode().orElse(null) instanceof RChoiceOption option)
                || option.typeCall() == null) {
            return null;
        }
        RNode declared = option.typeCall().referencedType().orElse(null);
        if (declared instanceof RDataType dt) {
            return dt;
        }
        if (declared instanceof RChoice ch) {
            RWorkspace workspace = workspaceOf(compiler);
            if (workspace == null) {
                return null; // stateless unit path — no resolver to attach
            }
            return idCarryingChoiceBridge(ch, workspace);
        }
        return null;
    }

    /**
     * v3.1 flip seat 13 (facet optionProjectionIdAttach): the choice-as-data-type
     * bridge whose option attributes carry the seat-1 id+attach PAIR (LAW 62 —
     * {@link #projectedOptionAttribute}: the option's linker-resolved id copied
     * onto the fresh typeCall, the copy attached to the workspace and
     * phantom-parented onto the option, the annotation refs carried), in place of
     * {@link RChoiceTypeRef#asRDataType()}'s id-less floating copies. Extracted
     * from {@link #authorityOptionDeclaredType}'s nested-choice arm (Copilot #571
     * R1) so the gm-aware choice narrowing {@link #attributeToDataType(RAttribute,
     * ExpressionCompiler)} projects options through the SAME implementation (LAW
     * 69: one projection, every consumer). Why the id matters: a projected option
     * typeCall without it resolves through {@code GeneratorModel.resolveTypeCall}'s
     * name lookup, which filters by {@code shouldGenerate} — and in every drr cell
     * the option's cdm namespace is a resolved-but-not-generated dependency, so the
     * lookup returns {@code RMissingType}: {@code addWitnessTypeRef} registered NO
     * import for a witness whose TEXT already rendered (a non-compiling
     * `.<OptionPayout>map(…)` with no `import cdm.product.template.OptionPayout;`),
     * and {@code metaWrapperOf} returned null for a meta-annotated option (its next
     * hop lost the `Type coercion` deref). With the id, {@code resolveTypeCall}
     * takes its {@code referencedTypeId → workspace.resolveTypeLike} branch —
     * filter-free AND namespace-exact (a simple-name lookup is not: the drr 7.0.0
     * workspace loads rune-fpml transitively and declares `Product`, `Asset`,
     * `Basket`, `Commodity`, `FloatingRateIndex`, `Index`, `Cash`, `Loan`,
     * `Security` in TWO namespaces). Ring-safe: cdm 5.38.0 / drr 6.34.1 /
     * iso20022 1.38.0 / rune-fpml 2.0.0 declare no choice at all; cdm 6.20.6
     * declares 10 choices / 51 option names and its cell loads rune-fpml 1.5.3,
     * which re-declares nine of them (Asset, Basket, Cash, Commodity,
     * FloatingRateIndex, Index, Loan, Product, Security) — what makes that cell
     * safe is the FILTER: its rosetta-config generates {@code cdm.*} only, so
     * {@code resolveFromWorkspace}'s shouldGenerate filter removes every fpml
     * homonym and within {@code cdm.*} the 51 names are unique (0 duplicates), so
     * the filtered first-match and the linker id denote the SAME declaration
     * there — the projection's consumers are byte-unchanged in every
     * self-contained cell and recover only where they declined (the seat-13
     * indep review's MF-1 restated the predicate; the 275-row PRE→POST matrix
     * showed 0 rows dropping {@code identical}). Contract: {@code ch} must be a
     * root element of {@code workspace} (the id copied onto each option's typeCall
     * is resolved through that workspace by {@code referencedType()}); every
     * caller passes the RChoice reached by resolving a typeCall against the
     * generator's own workspace.
     */
    static RDataType idCarryingChoiceBridge(RChoice ch, RWorkspace workspace) {
        RDataType bridge = new RDataType();
        bridge.setName(ch.name());
        for (RChoiceOption opt : ch.options()) {
            RAttribute attr = projectedOptionAttribute(opt, workspace);
            if (attr != null) {
                bridge.attributes().add(attr);
            }
        }
        return bridge;
    }

    /**
     * v3.1 flip seat 13: the id+attach bridge of a CALLABLE's CHOICE output — a
     * FUNCTION's declared output typeCall resolved through {@code gm.resolveTypeCall}
     * (the read the symbol-receiver arm's {@link #attributeToDataType(RAttribute,
     * ExpressionCompiler)} performs), or a RULE's INFERRED body type (the read
     * {@link #resolveReceiverOutputType} performs) — {@code null} for a non-choice
     * output, an unresolved output, or the stateless unit path (no workspace).
     * The corpus carriers are all FUNCTION heads ({@code RateOption},
     * {@code UnderlierForProduct}); the RULE arm mirrors the rule branch of
     * {@code resolveReceiverOutputType} for the walk's completeness and has ZERO
     * corpus carriers today — a rule-head option hop still renders witness-less
     * (the symbol-receiver arm has no RRule branch; recorded in the seat-13
     * charter as a follow-on).
     */
    private static RDataType callableChoiceOutputBridge(RNode callable,
            ExpressionCompiler compiler) {
        RWorkspace workspace = workspaceOf(compiler);
        GeneratorModel gm = compiler == null ? null : compiler.getGeneratorModel();
        if (workspace == null || gm == null) {
            return null;
        }
        RType outType = null;
        if (callable instanceof RFunction fn) {
            RAttribute out = fn.output().orElse(null);
            outType = out == null || out.typeCall() == null ? null
                    : gm.resolveTypeCall(out.typeCall());
        } else if (callable instanceof RRule rule) {
            // seat 21: the ONE rule-output read (HandlerHelper.ruleInferredOutputRType); this bridge
            // keeps its own choice post-filter below
            outType = HandlerHelper.ruleInferredOutputRType(rule, gm);
        }
        return outType instanceof RChoiceTypeRef ctr && ctr.astNode() != null
                ? idCarryingChoiceBridge(ctr.astNode(), workspace)
                : null;
    }

    /**
     * gm-aware counterpart of {@link #resolveReceiverDataType(RExpression)}.
     * Package-private (was private): facet lambda_item_body_coercion arm C's
     * onlyExists parent-type resolution falls back to the SAME walk when
     * {@code HandlerHelper.resolveValueDataType} declines (alias roots —
     * RShortcut recursion incl. only-element alias bodies — and alias-rooted
     * chains via {@code fallbackResolveFeature}).
     */
    static RDataType resolveReceiverDataType(RExpression receiver, ExpressionCompiler compiler) {
        return resolveReceiverDataType(receiver, compiler, null);
    }

    /**
     * Recursion-carrying variant of the gm-aware walk + ON-STACK cycle guard. {@code onStack} is
     * an identity set of every {@link RNode} currently on the resolution path (receiver expressions
     * AND the alias {@link RShortcut} body the worker recurses into) — added on entry, popped on
     * return (the {@code try/finally} below). A recursive structure — a self-/mutually-referential
     * alias, or (since facet aliasCallMapWitness PR #253) an item-piped then/distinct chain that
     * resolves its own ancestor via {@link #implicitItemDataType} — revisits a node already on the
     * path and resolves {@code null} instead of overflowing the stack. Remove-on-exit (NOT
     * add-only): a node reached twice via SIBLING branches (a diamond — e.g. a
     * {@code min [ alias -> a, alias -> b ]} list-literal whose branches share one alias body node)
     * resolves on BOTH visits, since the first pops its path before the second descends, so only a
     * node that is its OWN ancestor (a true cycle) is declined. The set is allocated once per
     * top-level walk (when the wrapper is entered with a {@code null} set); recursive hops thread
     * the same set. The receiver-shape arms live in the worker {@link #resolveReceiverDataType0}.
     */
    private static RDataType resolveReceiverDataType(RExpression receiver, ExpressionCompiler compiler,
            Set<RNode> onStack) {
        if (receiver == null) return null;

        // Cycle guard (facet aliasCallMapWitness PR #253): the element-transparent arms in the
        // worker (then/extract/filter/max/min/distinct/only-element) plus the RImplicitVariable
        // arm — which resolves the enclosing lambda's ARGUMENT (an ancestor of the item) via
        // implicitItemDataType — can revisit a node already on the current resolution path on a
        // recursive structure (e.g. a then-body that pipes the item back through a distinct).
        // Before this facet the distinct/conditional shapes were dead-ends that naturally
        // terminated the walk; making them traversable exposed the latent cycle. A single
        // ON-STACK identity set (added on entry, popped on return), threaded through every
        // recursive hop (incl. the alias body, fallbackResolveFeature, resolveDisguisedFeature,
        // and implicitItemDataType), breaks it. Remove-on-exit (NOT add-only) is required so a
        // legitimate DIAMOND — the SAME node reached twice via sibling branches, e.g. the
        // `min [ alias -> ccy1, alias -> ccy2 ]` list-literal comparator key of drr FXSwapLeg1/2
        // whose two branches share one alias body node — succeeds on both visits: the first pops
        // its path before the second descends, so only a node that is its OWN ancestor (a true
        // cycle) is declined to null (= the pre-facet witness-less bytes; never a green
        // regression). Lazily allocated so the dominant single-step walks stay allocation-free.
        if (onStack == null) {
            onStack = Collections.newSetFromMap(new IdentityHashMap<>());
        }
        if (!onStack.add(receiver)) {
            return null;
        }
        try {
            return resolveReceiverDataType0(receiver, compiler, onStack);
        } finally {
            onStack.remove(receiver);
        }
    }

    /**
     * Worker for {@link #resolveReceiverDataType(RExpression, ExpressionCompiler, Set)} carrying the
     * receiver-shape arms. The wrapper has already pushed {@code receiver} onto {@code onStack} (and
     * pops it when this returns), so every recursive hop goes back through the wrapper — and the
     * alias-shortcut indirection needs no separate guard: a self-referential alias revisits its own
     * body expression node and is declined by the wrapper there.
     */
    private static RDataType resolveReceiverDataType0(RExpression receiver, ExpressionCompiler compiler,
            Set<RNode> visited) {
        // facet aliasSwitchExtendsJoin (PR #396): a receiver that IS a bare-item-case
        // SWITCH (an alias BODY — both the symbol-ref alias arm and the disguised-head
        // alias arm funnel shortcut.expression() into this walk) types as the JOINED
        // case-guard type — the SAME switchItemCaseJoinedGuardType walk the signature +
        // the RETURN ladder read (the #178/#353 lockstep), so the nav
        // witness/cardinality/lambda-var cascade off the LUB element (golden cdm6
        // ExtractNotionalAdjustmentByLeg `<DirectionalLeg>mapC("getReturnSwapLeg",
        // returnSwapBase -> …)` over the fpmlProduct alias).
        if (receiver instanceof com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr recvSw
                && compiler != null && compiler.getGeneratorModel() != null
                && compiler.getTypeTranslator() != null && compiler.getTypeUtil() != null) {
            RType joined = new com.regnosys.rosetta.generator.java.function.FunctionAliasHelper(
                    compiler.getGeneratorModel(), compiler.getTypeTranslator(),
                    compiler.getTypeUtil())
                    .switchItemCaseJoinedGuardTypeOrNull(recvSw);
            if (joined instanceof RDataTypeRef jdtr) {
                return jdtr.astNode();
            }
        }
        if (receiver instanceof RSymbolReference symRef) {
            RNode sym = symRef.symbol().orElse(null);
            if (sym instanceof RAttribute a) {
                return attributeToDataType(a, compiler);
            }
            // facet alias_receiver_typing: an explicit function-call value's type is
            // the callee's declared OUTPUT type. Reached both directly and — the
            // facet's driving shape — as the terminal hop of an alias body that
            // wraps a function call (`alias openEconomicTerms:
            // ExtractOpenEconomicTerms(businessEvent)` → output `EconomicTerms`).
            if (sym instanceof RFunction fn) {
                RAttribute out = fn.output().orElse(null);
                return out == null ? null : attributeToDataType(out, compiler);
            }
            // facet alias_receiver_typing: a navigation rooted at an ALIAS call
            // (`openEconomicTerms -> terminationDate`) resolves the alias's value
            // type by recursing into the alias's own expression through this SAME
            // walk — the alias body is itself a navigation/list-op chain ending at
            // a data-typed leaf. Cascades the <Type> witness, the map/mapC
            // cardinality selection, the witness imports, and (via
            // resolveLambdaVarName's symbol-ref branch) the type-derived lambda
            // var through the whole alias-rooted chain. Strictly additive: this
            // receiver shape previously always resolved null (the underscore
            // `_<aliasName>` fallback + witness-less `.map(`).
            // The alias indirection rides the SAME wrapper cycle guard: recursing into the alias
            // body pushes that body node onto the on-stack set, so an `alias A: A -> x`
            // self-reference revisits the body node and is declined — no separate shortcut guard.
            RShortcut shortcut = resolveAliasReceiver(symRef, sym);
            if (shortcut != null) {
                return resolveReceiverDataType(shortcut.expression(), compiler, visited);
            }
            // facet closureParamDirectNav (PR #342): an EXPLICIT closure param
            // (`filter leg [ … ]` / `extract q1 [ … ]`) is a Mapper of its owner
            // list-op ARGUMENT's element — resolve that element through this SAME
            // walk, so the <T> witness, the map/mapC arity and the type-derived
            // lambda var all cascade in lockstep with the bare-param receiver
            // render (handle(RFeatureCall)'s #342 arm reads this resolution). A
            // then-declared param DECLINES (the whole-pipe MapperC shape — the
            // #180/#331 gate); filter/extract owners only AT #342 — sort/min/max
            // joined at #419 (facet sortMinMaxClosureParam, below); every other
            // owner keeps the null → today's bytes.
            RInlineFunction cpOwner = ReferenceHandler
                    .enclosingClosureParamOwner(symRef, symRef.name());
            if (cpOwner != null && !(cpOwner.parent() instanceof RThenExpr)) {
                // facet sortMinMaxClosureParam (PR #419): sort/min/max comparator-key
                // owners JOIN the #342 owner set — oracle golden expr-sort-min-max
                // (`items sort x [x -> price]` → `.sort(x -> x.<BigDecimal>map(
                // "getPrice", item -> item.getPrice()))`): the closure param is the
                // Mapper of the sorted list's element exactly like filter/extract
                // (the #301 sortLambdaItemTyping law already types the IMPLICIT item
                // from these same owners; the EXPLICIT closure arm had never joined,
                // so the resolution returned null and the whole lockstep cascade —
                // witness / arity / lambda naming / the L285 wrap-elision — degraded
                // to the wrapped, witness-less, shadow-named form, which never
                // compiled). Corpus-neutral by ABSENCE: zero explicit-param
                // sort/min/max carriers in the five cells (grep-verified); the
                // then-decline stays.
                // Leg-C #16 typing-channel heal (2026-07-18): REDUCE owners JOIN
                // the set — both reduce params are Mappers of the reduced list's
                // element (`foos reduce foo1, foo2 [ foo1 -> attr … ]`), the SAME
                // element-of-argument law as filter/extract, so the whole lockstep
                // cascade (witness / map-mapC arity / lambda naming / the L285
                // wrap-elision) heals for reduce-param navs. Corpus-neutral by
                // ABSENCE — the #420 census: the reduce operator occurs in ZERO
                // corpus sources and `.reduce(` in ZERO goldens. The then-decline
                // stays (the whole-pipe MapperC shape).
                RExpression cpOwnerArg = cpOwner.parent() instanceof RFilterExpr f
                        ? f.argument()
                        : cpOwner.parent() instanceof RExtractExpr e ? e.argument()
                        : cpOwner.parent() instanceof RReduceExpr red
                                && red.body() == cpOwner ? red.argument()
                        : cpOwner.parent() instanceof RSortExpr srt
                                && srt.body().orElse(null) == cpOwner ? srt.argument()
                        : cpOwner.parent() instanceof RMinExpr mn
                                && mn.body().orElse(null) == cpOwner ? mn.argument()
                        : cpOwner.parent() instanceof RMaxExpr mx
                                && mx.body().orElse(null) == cpOwner ? mx.argument()
                        : null;
                if (cpOwnerArg != null) {
                    // facet thenVarRewrapElision (PR #358): a NAMED top-level extract over
                    // the BARE rule input (`extract reportInstruction [ … ]` — TraderLocation,
                    // Beneficiary1/2, NotionalCurrencyLeg1/2) resolves through the recursion's
                    // RImplicitVariable arm, whose rule-top fallback types the bare rule input
                    // as the rule's from-type (the single seat for that fallback).
                    return resolveReceiverDataType(cpOwnerArg, compiler, visited);
                }
            }
            // facet choiceOptionTrailingWitness (PR #348): the receiver is the BARE
            // (mis-bound or unresolved) name the RENDER re-roots through
            // synthesizeImplicitItemBareNav — the #347-F8 choice-super-option class
            // (`Asset`/`Index` on a BasketConstituent item) whose TRAILING step then
            // lost its <Type> witness + import (`.map("getCommodity", …)` where golden
            // has `.<Commodity>map(…)`) because THIS walk had no re-root arm. Resolve
            // the SAME synthesis the render uses (its function-scope/closure-param
            // declines keep every green bare ref off this path) and read the resolved
            // attribute; the (M) meta short-form synthesis carries NO resolvedFeature
            // and stays null here. The 4 green #344-F1 carriers' receivers are
            // RAttribute-BOUND → the attribute arm above wins first, so the #347 cp11
            // naive-carry over-fire class is structurally unreachable from this arm.
            RFeatureCall optionReroot =
                    ReferenceHandler.synthesizeImplicitItemBareNav(symRef, compiler);
            if (optionReroot != null && optionReroot.resolvedFeature().isPresent()) {
                return attributeToDataType(optionReroot.resolvedFeature().get(), compiler);
            }
            // facet dispatchVariantParamResolution (PR #369): an UNRESOLVED bare symbol
            // inside a dispatch VARIANT body names a declared input of the dispatch BASE
            // (`MapperS.of(resetDates)` — the variant's own inputs are the
            // `__synthesized_input__` placeholder, so the parser leaves the reference
            // unbound). Resolving the base attribute here cascades the <T> witness, the
            // map/mapC arity and the type-derived lambda naming through the SAME walk
            // every consumer reads. Null for every non-variant seat (pre-facet bytes).
            if (sym == null) {
                RAttribute baseInput = HandlerHelper.dispatchBaseInput(symRef, symRef.name());
                if (baseInput != null) {
                    return attributeToDataType(baseInput, compiler);
                }
            }
            // Coverage wave D (datarule): an unbound bare name inside a DATA-TYPE
            // condition types as the declaring type's attribute — resolved through
            // the RENDER's own synthesis (the #348 lockstep convention: the walk
            // and the render cannot disagree; its closure-param + attr-bound
            // declines keep every other seat off this arm). Cascades the <Type>
            // witness, the map/mapC arity, and the type-derived lambda var through
            // chained condition hops (`period -> period` → `.<PeriodEnum>map(
            // "getPeriod", period -> period.getPeriod())`, golden
            // CalculationFrequencyDomTerm).
            RFeatureCall conditionReroot =
                    ReferenceHandler.synthesizeConditionInstanceBareNav(symRef, compiler);
            if (conditionReroot != null && conditionReroot.resolvedFeature().isPresent()) {
                return attributeToDataType(conditionReroot.resolvedFeature().get(), compiler);
            }
            return null;
        }

        if (receiver instanceof RFeatureCall fc) {
            if (fc.resolvedFeature().isPresent()) {
                return attributeToDataType(fc.resolvedFeature().get(), compiler);
            }
            RAttribute attr = fallbackResolveFeature(fc, compiler, visited);
            return attr == null ? null : attributeToDataType(attr, compiler);
        }

        if (receiver instanceof REnumValueRef evr && evr.enumeration().isEmpty()) {
            // v3.1 flip seat 1 (LADDER RETIREMENT) — authority-first: an option-nav
            // receiver (`underlier -> Observable` feeding a chained hop) types as
            // the option's DECLARED type read off the C1 authority slot, so the
            // chained hop's lambda var / witness / arity cascade recovers where the
            // disguise ladder below has no option arm. Non-option authorities fall
            // through to every existing arm unchanged.
            RDataType authorityType = authorityOptionDeclaredType(evr, compiler);
            if (authorityType != null) {
                return authorityType;
            }
            // facet alias_receiver_typing: the compiler-carrying variant adds the
            // disguised-ALIAS arm (`transferExpression -> scheduledTransfer`), so a
            // chained step whose receiver is a disguised alias hop recovers its
            // witness + cardinality exactly like the symbol-ref alias root.
            RAttribute attr = resolveDisguisedFeature(evr, compiler, visited);
            // facet thenVarRewrapElision (PR #358): a disguised 2-name nav ROOTED ON THE
            // LAMBDA ITEM (`originatingWorkflowStep -> businessEvent` inside a rule
            // then-extract — the ClearingSwapUSIs/UTIs conditional pipe) resolves its head
            // on the enclosing lambda's item element, the #225 implicitItemDisguisedLeaf
            // route inlined here WITH the on-stack set threaded (the #253 cycle law —
            // implicitItemDataType resolves an ANCESTOR argument). RULE-gated
            // (findEnclosingRule != null): the #224/#225 conversion-scope lesson keeps this
            // fallback away from cdm FUNCTION lambdas (the MapIntent-class junk-echo risk);
            // a rule-lambda item type is walk-proven, and the two findAttributeOnDataType
            // hops fail null for any name that is not a real feature (no junk propagation).
            if (attr == null && evr.enumName() != null && evr.valueName() != null
                    && HandlerHelper.findEnclosingRule(evr) != null) {
                RDataType itemType = implicitItemDataType(evr, compiler, visited);
                RAttribute headAttr = itemType == null ? null
                        : HandlerHelper.findAttributeOnDataType(itemType, evr.enumName());
                RDataType headType = headAttr == null ? null
                        : attributeToDataType(headAttr, compiler);
                RAttribute leaf = headType == null ? null
                        : HandlerHelper.findAttributeOnDataType(headType, evr.valueName());
                if (leaf != null) {
                    return attributeToDataType(leaf, compiler);
                }
            }
            // facet ctorFieldBareInvokableThenArg (PR #385): a disguised 2-name receiver
            // whose root is an ENCLOSING extract's EXPLICIT closure param resolves through
            // the SAME #360 leaf walk the cardinality overlay reads — the chained step's
            // <T> witness, map/mapC arity and type-derived lambda var cascade in lockstep
            // with the render (`customPeriod -> calculationPeriod -> startDate`: the hop-2
            // `<Date>` witness + the `dateRange` param — GetReportableQuantityPeriodLeg1/2's
            // filter predicate + ctor navs, where gen previously lost the witness).
            if (attr == null) {
                attr = closureParamDisguisedLeaf(evr, compiler);
            }
            // v3.1 flip seat 13 (facet disguisedOptionHopLambdaVar, the CALLABLE-head form):
            // a disguised pair whose head the parser's GlobalResolutionPass bound to a
            // FUNCTION/RULE (`RateOption -> FloatingRateIndex`, `underlier.UnderlierForProduct
            // -> Product` — the drr callables whose DECLARED output is a CHOICE) and whose
            // 2nd name is one of that choice's OPTIONS. resolveDisguisedFeature's callable
            // arm (#370 F-D arm 4) resolves the leaf on the callable's output only when
            // that output is a DATA type (resolveReceiverOutputType filters RDataTypeRef)
            // and declines META leaves by design (the cp4 whole-output-deref catch), so a
            // hop chained after the option had no receiver type: its lambda var fell to
            // the raw PascalCase valueName (`FloatingRateIndex -> FloatingRateIndex.
            // getIndexTenor()`; golden `floatingRateIndex`). Narrow the callable's CHOICE
            // output through the SAME id+attach bridge the symbol-receiver arm now reads
            // for the option hop's own witness/import (idCarryingChoiceBridge) and type the
            // receiver as the named option's declared type — the walk's answer for the
            // chained hop, exactly what ReferenceHandler's synthesizeFunction/Rule-
            // ReceiverNavigation renders that hop from. Fires ONLY for a callable head
            // with a CHOICE output naming one of its options (a data-type output stays on
            // arm 4; a non-option 2nd name finds nothing on the bridge and declines).
            if (attr == null && evr.valueName() != null && evr.resolvedSymbol().isPresent()) {
                RDataType choiceOutput =
                        callableChoiceOutputBridge(evr.resolvedSymbol().get(), compiler);
                RAttribute option = choiceOutput == null ? null
                        : HandlerHelper.findAttributeOnDataType(choiceOutput, evr.valueName());
                if (option != null) {
                    return attributeToDataType(option, compiler);
                }
            }
            return attr == null ? null : attributeToDataType(attr, compiler);
        }

        if (receiver instanceof RListOpExpr listOp && listOp.op() == ListOp.ONLY_ELEMENT) {
            return resolveReceiverDataType(listOp.argument(), compiler, visited);
        }

        // facet navWalkChoiceDisguise (PR #344) arm 3: a receiver that is a DEEP feature
        // call (`… -> product ->> economicTerms` feeding a chained `-> payout` step) —
        // the walk previously had NO RDeepFeatureCall arm (it extends RExpression, not
        // RFeatureCall; the P344 probe: `RRDT0 NO-ARM recvClass=RDeepFeatureCall`), so
        // every step chained after a deep call lost its <Type> witness, its map/mapC
        // arity (golden `.<Payout>mapC(…)` rendered `.map(…)`) and its typed lambda var.
        // Resolves through the SAME resolveDeepFeature the handle(RDeepFeatureCall)
        // witness/map-method render reads — the #343 deepPathLambdaNaming law, one walk
        // deeper — so the walk and the deep-call render cannot disagree. Compiler-gated
        // (resolveDeepFeature returns null without one); a failed resolution keeps the
        // pre-facet null → today's bytes.
        if (receiver instanceof RDeepFeatureCall dfc) {
            RAttribute deepAttr = resolveDeepFeature(dfc, compiler);
            return deepAttr == null ? null : attributeToDataType(deepAttr, compiler);
        }

        // facet thenArgTypeFromCompiled (PR #204): an extract body whose own RESULT is
        // navigated PRODUCES the element type — a then-chain pipes its mapped element
        // type through the extract wrapper, so the implicit-item resolution
        // (implicitItemDataType → here, via the then-pipe RImplicitVariable →
        // then.argument()) must see through it to recover the leaf data type the parser
        // snapshot erased to Object. STRICTLY ADDITIVE: RExtractExpr previously had NO
        // arm here and always returned null (the underscore lambda-var fallback +
        // witness-less bare render); the filter/then shapes are ALREADY resolved by the
        // facet then_maxmin_item_typing S3 arms below, so this PR adds ONLY the extract
        // arm (no duplicate filter/then arm).
        if (receiver instanceof RExtractExpr extract) {
            return resolveReceiverDataType(extract.body().body(), compiler, visited);
        }

        // facet onlyelement_item_typing: the lambda's implicit `item` as a chain root
        // (`item -> trade -> tradeLot`) — resolve the ITEM data type through the SAME
        // walk the PR #161 lambda-var-naming/attribute-synthesis mechanisms read
        // ({@link #implicitItemDataType}: nearest enclosing filter/extract inline
        // lambda's ARGUMENT item type; sort/reduce owners decline inside the walk —
        // then/max/min owners resolve since facet then_maxmin_item_typing). This was the deliberately-deferred RImplicitVariable base case:
        // without it the whole chain off `item` lost its <Type> witnesses AND its
        // map/mapC cardinality selection. Strictly additive — this receiver shape
        // previously always returned null.
        if (receiver instanceof RImplicitVariable) {
            // Thread the cycle-guard set (facet aliasCallMapWitness PR #253): implicitItemDataType
            // resolves the enclosing lambda's ARGUMENT — an ANCESTOR of this item — so without
            // sharing the set this re-entry would restart a fresh walk and a recursive item-piped
            // structure (then/distinct) would not see the node already on the path.
            RDataType viaItem = implicitItemDataType(receiver, compiler, visited);
            // facet thenVarRewrapElision (PR #358): the RULE-TOP implicit (the bare rule
            // input heading the rule body's chain — filter/extract argument with NO
            // enclosing lambda) types as the rule's from-type. A then-step named extract's
            // element recursion bottoms here (`filter … then filter … then extract
            // reportInstruction [ … ]` — ClearingExceptionsAndExemptionsCounterparty1/2;
            // the conditional-piped ClearingSwapUSIs/UTIs route additionally crosses the
            // elseless-conditional arm below). FUNCTION cells are structurally unaffected
            // (findEnclosingRule == null → null), and the NAMING side of the same seats has
            // read rule.fromType() since PR #255 (resolveLambdaVarName's rule branch) — this
            // aligns the data-type walk with it. Green-safe: upstream types every rule seat
            // from the same static from-type, so a green witness-less render over a seat
            // this newly types cannot exist (golden would carry the witness).
            if (viaItem == null && isRuleTopLevelImplicit(receiver)) {
                viaItem = ruleFromDataType(receiver);
            }
            // Coverage wave D (datarule): the CONDITION-TOP implicit — an explicit
            // `item -> attr` at type-condition top level (golden
            // LegacyValuationTimeDayAndTime `item -> location`) — types as the
            // condition's declaring type, aligning the walk with
            // handle(RImplicitVariable)'s condition-instance render and
            // resolveLambdaVarName's condition arm (lockstep: the witness, the
            // map/mapC arity and the lambda var read the SAME type).
            if (viaItem == null && isConditionTopLevelImplicit(receiver)) {
                RCondition topCondition = HandlerHelper.findEnclosingTypeCondition(receiver);
                if (topCondition != null && topCondition.parent() instanceof RDataType conditionType) {
                    viaItem = conditionType;
                }
            }
            return viaItem;
        }

        // facet then_maxmin_item_typing (arm S3): element-type-transparent receivers.
        // max/min collapse cardinality but keep the element type; filter keeps both;
        // first/last collapse like only-element; a then's value is its body's value.
        if (receiver instanceof RMaxExpr max) {
            return resolveReceiverDataType(max.argument(), compiler, visited);
        }
        if (receiver instanceof RMinExpr min) {
            return resolveReceiverDataType(min.argument(), compiler, visited);
        }
        if (receiver instanceof RFilterExpr filter) {
            return resolveReceiverDataType(filter.argument(), compiler, visited);
        }
        // v3.2 seat 2 (Law 1, the chaos C20Spread rows): a SORT pipes its receiver through exactly
        // as a filter does (this walk had the filter half only). The sort alias `ordered` was the
        // receiver behind `backFirst -> v`: it resolved null here, so the navigation named its
        // lambda parameter by the `_backFirst` escape and dropped the `<BigDecimal>` witness —
        // the ONE token pair by which the default route trailed the IR route on those twelve
        // files (the seat-2 census § 1a). Golden: `c20Leaf -> c20Leaf.getV()`.
        if (receiver instanceof RSortExpr sortPipe) {
            return resolveReceiverDataType(sortPipe.argument(), compiler, visited);
        }
        if (receiver instanceof RThenExpr then) {
            RInlineFunction body = then.body().orElse(null);
            return body == null ? null : resolveReceiverDataType(body.body(), compiler, visited);
        }
        if (receiver instanceof RListOpExpr listOp
                && (listOp.op() == ListOp.FIRST || listOp.op() == ListOp.LAST)) {
            return resolveReceiverDataType(listOp.argument(), compiler, visited);
        }

        // facet aliasCallMapWitness (PR #253): distinct (and its reorder sibling reverse) is
        // element-type TRANSPARENT — it removes duplicates / reverses order but keeps the list's
        // element type. A `<chain> -> reason distinct only-element -> value` navigation
        // (MapExecutionAdviceToWorkflowStep) reaches this walk via the ONLY_ELEMENT arm above
        // recursing into the DISTINCT argument; without this arm the element type dropped, so the
        // trailing `-> value` step lost its `<String>` witness AND its type-named lambda var (it
        // fell back to the disambiguated `_value` feature-name form). Strictly additive: this
        // receiver shape previously always returned null. REVERSE is the same element-transparent
        // family (no corpus carrier today — included so a reverse-rooted chain resolves identically
        // should one appear; it can only ADD a correct witness where one was missing).
        // facet flattenElementTransparent (PR #383): FLATTEN joins the element-transparent
        // family — it collapses a list-of-lists' NESTING but keeps the leaf element data
        // type (`… then extract quantity then flatten then filter frequency exists then
        // extract quantitySchedule [ … ]` — the drr common NotionalQuantityLeg1Rule pipe,
        // whose named-param element previously resolved null EXACTLY because the chain
        // crossed the flatten step; its flatten-free Leg2 sibling already resolved). The
        // walk tracks the ELEMENT type, not the nesting, so the recursion is the same as
        // distinct/reverse. Strictly additive (#253 law): this receiver shape previously
        // always returned null, and upstream types the same seats, so a green witness-less
        // render over a newly-typed flatten pipe cannot exist (golden would carry the
        // witness).
        if (receiver instanceof RListOpExpr listOp
                && (listOp.op() == ListOp.DISTINCT || listOp.op() == ListOp.REVERSE
                        || listOp.op() == ListOp.FLATTEN)) {
            return resolveReceiverDataType(listOp.argument(), compiler, visited);
        }

        // facet aliasCallMapWitness (PR #253): an alias whose VALUE is an if-then-else
        // (`alias wtPeriod: if (lookback exists) then adjustedCalculationPeriod else
        // observationPeriod`) types as the JOIN of its branch types. A navigation off such an
        // alias call (`wtPeriod -> adjustedEndDate`; `calculationResults -> calculatedRate`)
        // recurses here through the RShortcut alias arm above; without this arm the branch type
        // dropped, so the step lost its `<Date>`/`<BigDecimal>` witness AND its type-named lambda
        // var (the `_wtPeriod`/`_calculationResults` symbol-name fallback). IDENTICAL-OR-DECLINE,
        // mirroring the RListLiteral arm below: both branches must resolve to the SAME data-type
        // declaration node (identity-comparable — attributeToDataType returns the canonical AST
        // node; EvaluateCalculatedRate's branches are two functions that both output
        // CalculatedRateDetails). A heterogeneous conditional (branches of different types, or a
        // branch that does not resolve to a data type) declines to null = the pre-facet
        // witness-less bytes, never a wrong type. Strictly additive: this receiver shape
        // previously always returned null.
        // facet thenVarRewrapElision (PR #358): a SINGLE-BRANCH conditional joins trivially to
        // its then-branch — upstream's type system joins the then-type with empty (bottom), so
        // the piped value of `if C then <nav>` carries <nav>'s element type (the
        // ClearingSwapUSIs/UTIs `then extract` pipe: `if IsCleared(…) … then
        // originatingWorkflowStep -> businessEvent`). The parser materializes the implicit else
        // as an EMPTY RListLiteral (the P358I probe), so "single-branch" here means the else is
        // ABSENT or that materialized empty literal — a REAL `else []` is semantically the same
        // empty join. A seat this newly types was previously witness-less ONLY because the walk
        // declined; upstream types the same seat, so a green witness-less render over a typeable
        // single-branch conditional cannot exist (golden would carry the witness — the #253
        // green-safety argument, single-branch case).
        if (receiver instanceof RConditionalExpr cond) {
            RDataType thenType = resolveReceiverDataType(cond.thenBranch(), compiler, visited);
            boolean elseEmpty = cond.elseBranch().isEmpty()
                    || (cond.elseBranch().get() instanceof RListLiteral el && el.elements().isEmpty());
            if (elseEmpty) {
                return thenType;
            }
            RDataType elseType = resolveReceiverDataType(cond.elseBranch().get(), compiler, visited);
            // facet aliasCallElementType (PR #358, F-B): upstream joins heterogeneous branch
            // types to their LUB when one branch's type is an ANCESTOR of the other on the
            // extends chain (typeSystem.join — golden names the securityQuantity alias's nav
            // lambda `quantitySchedule` from the Quantity/QuantitySchedule join, cdm
            // SecurityFinanceCashSettlementAmount; NonNegativeQuantity/Quantity joins to
            // Quantity, Create_AssetTransfer). IDENTITY stays the fast path; a genuinely
            // unrelated pair (no ancestor relation) still declines to null — never a wrong
            // type, the #253 law with the subtype case added.
            return ancestorJoin(thenType, elseType);
        }

        // facet aliasDefaultJoin (PR #362): a `default` receiver types as the ancestorJoin
        // of its operands — upstream binaryExpr `case "default"` types the result at the
        // joined operand type (the SAME typeSystem.join the conditional arm above mirrors).
        // Carrier: cdm6 MapEarlyTerminationProvisionToAncillaryParty's
        // `mandatoryEarlyTermination` alias body (`<nav to MandatoryEarlyTermination>
        // default <nav to MandatoryEarlyTermination>` — the identical-type fast path); the
        // resolution cascades the <CalculationAgent> witness, the mapC arity, the
        // type-derived lambda vars and both imports through the EXISTING alias-receiver
        // consumers, zero per-consumer changes. A genuinely unrelated pair still declines
        // to null — never a wrong type (the #253/#358 law).
        if (receiver instanceof com.regnosys.rosetta.ast.expressions.binary.RDefaultExpr defaultRecv) {
            // facet thenWrappedDefaultRewrap (seat 28, law 4, rung B - OFF route only,
            // LAW 77 in reverse: the IR route already renders this witness, so the ON
            // dump carries ONE hunk for QuantityFrequency where the OFF dump carries
            // TWO). The grammar's DefaultWithoutLeftExpr alternative
            // (`| DEFAULT expression`) leaves rawLeft() NULL by parse
            // (AstBuilder.visitDefaultWithoutLeftExpr sets only `right`; `default` is
            // not among the elided-operand synthesis slots - the #513 IR-adapter law),
            // so ancestorJoin(null, X) declined and the whole alias typed null: the
            // `<PeriodExtendedEnum>` / `<Integer>` witnesses and their imports vanished
            // off a `then default ...` alias receiver. The elided left IS the enclosing
            // then-pipe's argument - resolve it exactly as the RImplicitVariable arm
            // does, through the SHARED implicitItemDataType walk, whose RThenExpr owner
            // arm returns then.argument(). Strictly additive: this branch returned null
            // for EVERY left-null default before, so a newly-resolving site can only
            // ADD a witness where one was missing (the #253/#358/#362 green-safety law).
            RDataType leftType = defaultRecv.rawLeft() != null
                    ? resolveReceiverDataType(defaultRecv.rawLeft(), compiler, visited)
                    : implicitItemDataType(defaultRecv, compiler, visited);
            RDataType rightType = resolveReceiverDataType(defaultRecv.rawRight(), compiler, visited);
            return ancestorJoin(leftType, rightType);
        }

        // facet aliasCallElementType (PR #358, F-B): a CONSTRUCTOR receiver types as its
        // constructed data type (`NonNegativeQuantity { value: …, unit: …, … }` — the
        // Create_AssetTransfer securityQuantity alias's else branch). Strictly additive:
        // this receiver shape previously always returned null.
        if (receiver instanceof RConstructorExpr ctor && ctor.typeCall() != null) {
            return ctor.typeCall().referencedType()
                    .filter(RDataType.class::isInstance)
                    .map(RDataType.class::cast)
                    .orElse(null);
        }

        // facet maxmin_body_coercion (arm B): a LIST-LITERAL receiver types as the
        // JOIN of its element types — upstream RosettaTypeProvider.caseListLiteral
        // joins the elements (typeSystem.joinMetaAnnotatedTypes), so a min/max over
        // `[chain -> exchangedCurrency1, chain -> exchangedCurrency2]` types its
        // implicit item as the elements' shared data type (the PR #171
        // void_witness list-literal law, here at the RDataType level). The join is
        // IDENTICAL-OR-DECLINE on the declaration node (identity-comparable for
        // data-type declarations — the AST-node and RDataTypeRef.astNode() paths
        // both return the canonical declaration; a CHOICE-typed element resolves
        // through asRDataType()'s per-call fresh projection and so always
        // declines — acceptable, no corpus carrier. A genuinely heterogeneous
        // literal would need the upstream subtype join — no corpus golden
        // exercises one as a lambda owner, so it declines to the pre-facet
        // null = witness-less bytes, never a wrong type). Cascades — through the
        // EXISTING implicitItemDataType consumers, zero per-consumer changes — the
        // <T>map witnesses, the type-derived lambda vars, and the interior
        // receiver-side meta-unwraps of the whole comparator-key chain (drr
        // FXLeg1/2, FXSwapLeg1/2).
        if (receiver instanceof RListLiteral list && !list.elements().isEmpty()) {
            RDataType joined = null;
            for (RExpression el : list.elements()) {
                RDataType t = resolveReceiverDataType(el, compiler, visited);
                if (t == null || (joined != null && t != joined)) {
                    return null;
                }
                joined = t;
            }
            return joined;
        }

        return null;
    }

    /**
     * facet thenVarRewrapElision (PR #358): true when {@code node}'s parent chain reaches
     * the enclosing {@link RRule} WITHOUT crossing an {@link RInlineFunction} — the
     * rule-body top-level position (an {@link RImplicitVariable} here IS the bare rule
     * input). A node inside any lambda returns false (its item types via the enclosing
     * lambda's argument instead). Bounded like every other parent walk.
     */
    private static boolean isRuleTopLevelImplicit(RNode node) {
        RNode cur = node == null ? null : node.parent();
        int depth = 0;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) {
                return false;
            }
            if (cur instanceof RRule) {
                return true;
            }
            if (cur instanceof RFunction) {
                return false;
            }
            cur = cur.parent();
        }
        return false;
    }

    /**
     * Coverage wave D (datarule): the {@link #isRuleTopLevelImplicit} twin for a
     * DATA-TYPE condition — true when {@code node}'s parent chain reaches a
     * type-parented {@link RCondition} WITHOUT crossing an
     * {@link RInlineFunction} (an implicit here IS the condition instance). A
     * node inside any lambda returns false (its item types via the enclosing
     * lambda's argument instead).
     */
    private static boolean isConditionTopLevelImplicit(RNode node) {
        RNode cur = node == null ? null : node.parent();
        int depth = 0;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) {
                return false;
            }
            if (cur instanceof RCondition condition) {
                return condition.parent() instanceof RDataType;
            }
            if (cur instanceof RRule || cur instanceof RFunction) {
                return false;
            }
            cur = cur.parent();
        }
        return false;
    }

    /**
     * facet aliasCallElementType (PR #358, F-B): the LUB of two branch data types on the
     * {@code extends} chain — {@code a} when identical, else whichever of the two is an
     * ANCESTOR of the other (upstream {@code typeSystem.join} for the data-type/data-type
     * case), else {@code null} (unrelated types decline — never a wrong type). Identity
     * compares on declaration nodes like every walk join; the super walk is bounded like
     * every other parent walk.
     */
    private static RDataType ancestorJoin(RDataType a, RDataType b) {
        if (a == null || b == null) {
            return a == b ? a : null;
        }
        if (a == b) {
            return a;
        }
        RDataType cur = a;
        int depth = 0;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur == b) {
                return b;
            }
            cur = cur.superType().orElse(null);
        }
        cur = b;
        depth = 0;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur == a) {
                return a;
            }
            cur = cur.superType().orElse(null);
        }
        return null;
    }

    /**
     * facet thenVarRewrapElision (PR #358): the enclosing rule's from-type as an
     * {@link RDataType}, or {@code null} (no enclosing rule / no from-type / the
     * from-type call does not reference a data type). Mirrors
     * {@code ReferenceHandler.synthesizeImplicitInputNavigation}'s resolution.
     */
    private static RDataType ruleFromDataType(RNode node) {
        RRule rule = HandlerHelper.findEnclosingRule(node);
        if (rule == null || rule.fromType().isEmpty()) {
            return null;
        }
        RTypeCall fromTypeCall = rule.fromType().get();
        return fromTypeCall.referencedType()
                .filter(RDataType.class::isInstance)
                .map(RDataType.class::cast)
                .orElse(null);
    }

    /**
     * Resolves the alias ({@link RShortcut}) a symbol reference names, or {@code null}
     * when the reference is not an alias (facet alias_receiver_typing). Primary path:
     * the linker-resolved {@code symbol()}. Belt-and-braces fallback: when
     * {@code symbol()} is EMPTY — the dominant corpus shape for an alias used as a
     * navigation RECEIVER — match the name against the enclosing function's shortcut
     * list, the SAME dual path {@code ReferenceHandler.isAliasReference} uses to emit
     * the {@code aliasName(args)} invocation text, so the receiver's typing and its
     * rendered call cannot disagree. A reference RESOLVED to a non-alias symbol never
     * reaches the fallback (it is that symbol, not an alias). Package-private so
     * sibling handlers reuse the SAME dual path (facet numeric_literal_typing:
     * {@code HandlerHelper.numericOperandKind} types an alias operand by it).
     */
    static RShortcut resolveAliasReceiver(RSymbolReference symRef, RNode resolvedSym) {
        if (resolvedSym instanceof RShortcut shortcut) {
            return shortcut;
        }
        if (resolvedSym != null || symRef.name() == null) {
            return null;
        }
        RFunction enclosing = HandlerHelper.findEnclosingFunction(symRef);
        if (enclosing == null) {
            return null;
        }
        for (RShortcut shortcut : enclosing.shortcuts()) {
            if (symRef.name().equals(shortcut.name())) {
                return shortcut;
            }
        }
        return null;
    }

    /**
     * facet aliasCondLadderChoiceJoin (v3.1 flip seat 33, law A.2): the CHOICE-ladder fold
     * for the ALIAS-NAMING seat - consulted ONLY from {@link #resolveLambdaVarName}'s alias
     * arm, and ONLY where {@link #resolveReceiverDataType(RExpression, ExpressionCompiler)}
     * has already DECLINED.
     *
     * <p>An alias whose value is a multi-rung conditional LADDER over one {@code choice}
     * resolves null today. The walk's {@link RConditionalExpr} arm joins its branch types
     * with {@code ancestorJoin}, which compares DECLARATION IDENTITY, and every choice
     * narrowing mints a FRESH bridge ({@link #idCarryingChoiceBridge}, and the stateless
     * {@code RChoiceTypeRef.asRDataType()}) - so two projections of ONE choice are never
     * the same object, the deepest rung joins two fresh {@code Underlier} bridges to null,
     * and the outer rungs then join something against that null. The lambda var falls back
     * to the alias SYMBOL name: golden {@code underlier -> underlier.getObservable()},
     * fork {@code _underliers -> _underliers.getObservable()} (drr 7.0-7.3
     * {@code GetBasketConstituents}, the only sig left in that file).
     *
     * <p><b>Why the seat and not the join.</b> {@code ancestorJoin}'s null contract - an
     * UNRELATED pair never becomes a join - is untouched, and the fold is deliberately NOT
     * consulted at the shared conditional arm: {@code [P33-CONDJOIN] arm=join join=-
     * fold!=-} is 3,065 rows over 15 {@code where=}, FOURTEEN of them byte-GREEN
     * ({@code UnderlyingAssetTradingPlatformIdentifierLeg1/2}, {@code FXLeg1/2},
     * {@code Notional}, {@code NotionalLeg}, {@code GetUnderlierProductIdentifierLeg1},
     * {@code SpreadCurrency*}, {@code BasketConstituentIdentifier*},
     * {@code MasterAgreementType}, {@code OtherMasterAgreementType},
     * {@code DTCC_Leg1CommodityInstrumentID}), and NO printed field separates the carrier
     * from {@code UnderlyingAssetTradingPlatformIdentifierLeg1} - narrowing to
     * {@code sameName=true} still leaves three of them. The discriminator is therefore the
     * SEAT, not a field of the join: those fourteen reach the walk through the witness /
     * arity / import consumers and through the other naming arms, never through this
     * alias-naming decline. {@code [P32-ALIASWALK]} measured that decline at 4 rows / 1
     * {@code where=} over 11,957 alias consults (OFF) and 4 / 1 over 2,023 (ON) - the
     * carrier's, and only the carrier's.
     *
     * <p>The value comes from {@link #condArmExtendsFold} - the SAME per-arm workspace
     * inference the #398 LUB fallback already reads (LAW 69: one fold, every consumer) -
     * narrowed through the SAME id+attach bridge that
     * {@link #attributeToDataType(RAttribute, ExpressionCompiler)} narrows a choice-typed
     * attribute with, so the alias's item type and a directly-navigated choice attribute
     * cannot denote different things. Only the {@link RChoiceTypeRef} shape is answered: a
     * fold to a plain {@code RDataTypeRef} is unreachable at this seat by construction (two
     * arms resolving to one data type return the SAME canonical AST node, so
     * {@code ancestorJoin} already succeeded and this method was never called), and an
     * unwitnessed branch is not shipped. Every other shape - a non-alias receiver, a
     * non-ladder alias value, an unresolvable arm, the stateless unit path - DECLINES to
     * {@code null}, which is today's bytes exactly.
     */
    private static RDataType aliasCondLadderChoiceJoin(RSymbolReference symRef,
            ExpressionCompiler compiler) {
        if (compiler == null) {
            return null;
        }
        RShortcut alias = resolveAliasReceiver(symRef, symRef.symbol().orElse(null));
        if (alias == null) {
            return null;
        }
        // Peel the element-transparent hops the worker's own arms peel (then / filter /
        // first / last / only-element / distinct / reverse / flatten) to reach the alias's
        // VALUE shape - the carrier's body is `<call> -> payout then <ladder>`. Bounded
        // like every other walk in this class.
        RExpression value = alias.expression();
        int depth = 0;
        while (value != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (value instanceof RThenExpr then) {
                RInlineFunction thenBody = then.body().orElse(null);
                value = thenBody == null ? null : thenBody.body();
            } else if (value instanceof RFilterExpr filter) {
                value = filter.argument();
            } else if (value instanceof RListOpExpr listOp
                    && (listOp.op() == ListOp.FIRST || listOp.op() == ListOp.LAST
                            || listOp.op() == ListOp.ONLY_ELEMENT
                            || listOp.op() == ListOp.DISTINCT
                            || listOp.op() == ListOp.REVERSE
                            || listOp.op() == ListOp.FLATTEN)) {
                value = listOp.argument();
            } else {
                break;
            }
        }
        // A LADDER only. A single-rung conditional already resolves through the arm's
        // else-empty fast path, and a two-arm conditional over ONE data type through the
        // identity join - neither reaches this seat - so requiring a nested else keeps the
        // consult on exactly the shape the mechanism describes.
        if (!(value instanceof RConditionalExpr ladder)
                || !(ladder.elseBranch().orElse(null) instanceof RConditionalExpr)) {
            return null;
        }
        RType folded = condArmExtendsFold(ladder, compiler);
        if (folded instanceof RChoiceTypeRef fctr && fctr.astNode() != null) {
            RWorkspace workspace = workspaceOf(compiler);
            return workspace != null
                    ? idCarryingChoiceBridge(fctr.astNode(), workspace)
                    : fctr.asRDataType();
        }
        return null;
    }

    /**
     * gm-aware counterpart of {@link #attributeToDataType(RAttribute)}. Tries the static AST
     * fast-path first (unchanged semantics); when that yields no {@link RDataType}, resolves
     * the attribute's type via {@link GeneratorModel#resolveTypeCall} — the linker id when the
     * typeCall carries one, else the by-name workspace resolution (filtered by
     * {@code shouldGenerate}) — and narrows an {@link RChoiceTypeRef} to a choice-as-data-type
     * bridge so the choice's options are findable: since v3.1 flip seat 13 the
     * id+attach bridge {@link #idCarryingChoiceBridge} whenever a workspace is reachable
     * (each projected option attribute carries the option's linker id and is attached to the
     * workspace, so every consumer resolves it filter-free and namespace-exactly), and the
     * id-less {@link RChoiceTypeRef#asRDataType()} projection only on the stateless unit path.
     * Returns {@code null} when no compiler / generator model is available (isolated unit
     * calls) or the type does not resolve.
     */
    // Package-private (was private): facet lambda_item_body_coercion arm B2's
    // synthesizeImplicitItemChain resolves the chain HEAD attribute's data type
    // through the SAME gm-aware walk (choice-typed heads narrow to the options
    // projection) so the guard and the rendering cannot disagree.
    static RDataType attributeToDataType(RAttribute attr, ExpressionCompiler compiler) {
        if (attr == null || attr.typeCall() == null) return null;
        RDataType ast = attributeToDataType(attr);
        if (ast != null) return ast;
        if (compiler == null) return null;
        GeneratorModel gm = compiler.getGeneratorModel();
        if (gm == null) return null;
        RType rt = gm.resolveTypeCall(attr.typeCall());
        if (rt instanceof RDataTypeRef dtr) return dtr.astNode();
        if (rt instanceof RChoiceTypeRef ctr) {
            // v3.1 flip seat 13 (facet optionProjectionIdAttach): narrow through the
            // id+attach bridge (see idCarryingChoiceBridge) whenever a workspace is
            // reachable; the stateless unit path keeps the id-less projection.
            RWorkspace workspace = workspaceOf(compiler);
            return workspace != null && ctr.astNode() != null
                    ? idCarryingChoiceBridge(ctr.astNode(), workspace)
                    : ctr.asRDataType();
        }
        return null;
    }

    /**
     * Best-effort resolution of the LEAF {@link REnumeration} of a comparison sibling, for the
     * bare-enum comparison-operand facet ({@code ComparisonHandler.tryBareEnumComparand}).
     *
     * <p>A bare enum value used as a comparison operand ({@code <nav> = Clearing}) parses as an
     * UNRESOLVED single-identifier {@link RSymbolReference} (enum VALUES are not in function
     * scope), and the {@code TypeInferenceEngine} only type-directs it to its
     * {@link com.regnosys.rosetta.ast.supporting.REnumValue} when a SIBLING already types as the
     * enum — but the comparison sibling's enum type is MISSING on the expression node (the
     * {@code resolvedFeature} IR gap). The enum it belongs to is nonetheless pinned by the OTHER
     * operand's leaf feature. This recovers that enum using the SAME best-effort receiver-chain
     * resolution the {@code <Type>} witness path already uses — {@link #fallbackResolveFeature}
     * for a chained {@link RFeatureCall}, and {@link #resolveDisguisedFeature} for a disguised
     * {@link REnumValueRef} navigation ({@code spreadLeg1 -> spreadNotation}, the grammar's
     * {@code EnumName -> valueName} ambiguity) — so the comparison's qualified constant and the
     * sibling's {@code <Type>} witness stay derived from one resolved {@link RAttribute}.
     *
     * <p>Returns {@code null} when the sibling is not a recognised navigation, its leaf attribute
     * does not resolve, or the leaf type is not an enum (the dominant non-enum comparison — the
     * bare-enum facet then declines and the operand renders unchanged).
     */
    static REnumeration leafEnumeration(RExpression sibling, ExpressionCompiler compiler) {
        RAttribute leaf = null;
        if (sibling instanceof RFeatureCall fc) {
            leaf = fc.resolvedFeature().orElseGet(() -> fallbackResolveFeature(fc, compiler));
        } else if (sibling instanceof REnumValueRef evr && evr.enumeration().isEmpty()) {
            leaf = resolveDisguisedFeature(evr);
        }
        if (leaf == null || leaf.typeCall() == null) {
            return null;
        }
        return leaf.typeCall().referencedType()
                .filter(REnumeration.class::isInstance)
                .map(REnumeration.class::cast)
                .orElse(null);
    }

    /**
     * The symbol-reference sibling analogue of {@link #leafEnumeration} (facet
     * {@code enumQualify}, PR #206): recover the enum of a bare-enum comparison operand's
     * SIBLING when that sibling is a bare reference to an ALIAS or a FUNCTION whose result
     * is enum-typed — e.g. {@code if actionType = NEWT} where {@code alias actionType:
     * drrReport -> actionType} renders {@code MapperS<ActionTypeEnum> actionType(...)}.
     * {@link #leafEnumeration} only handles a direct {@link RFeatureCall} navigation or a
     * disguised {@link REnumValueRef} chain, so a bare alias/function sibling returned
     * {@code null} and the bare enum stayed unqualified ({@code MapperS.of(NEWT)} — a
     * non-compiling undefined symbol; every such carrier is already waivered, so the
     * recovery is regression-safe by construction).
     *
     * <ul>
     *   <li><b>Alias ({@link RShortcut}) sibling</b> — resolve the alias's EXPRESSION
     *       (the navigation {@code drrReport -> actionType}) via the SAME leaf walk
     *       {@link #leafEnumeration} uses for a direct navigation.</li>
     *   <li><b>Function ({@link RFunction}) sibling</b> — recover the enum from the
     *       callee's OUTPUT type.</li>
     * </ul>
     *
     * <p>Deliberately a SEPARATE method scoped to the comparison path
     * ({@code ComparisonHandler.tryBareEnumComparand}) rather than a new arm in
     * {@link #leafEnumeration}: the latter is also consumed by
     * {@link #implicitItemEnumeration} (to-string / to-enum source detection inside
     * filter/extract/then/max/min lambdas), and widening it there would change that
     * green path. Keeping it scoped holds the blast radius to the bare-enum-comparand
     * facet.
     *
     * @return the sibling alias/function's result {@link REnumeration}, or {@code null}
     *         when the sibling is not such a reference or its result is not enum-typed.
     */
    static REnumeration siblingComparandEnumeration(RExpression sibling, ExpressionCompiler compiler) {
        if (!(sibling instanceof RSymbolReference sr)) {
            return null;
        }
        RNode sym = sr.symbol().orElse(null);
        if (sym instanceof RShortcut shortcut && shortcut.expression() != null) {
            return leafEnumeration(shortcut.expression(), compiler);
        }
        if (sym instanceof RFunction fn) {
            RAttribute out = fn.output().orElse(null);
            if (out != null && out.typeCall() != null) {
                return out.typeCall().referencedType()
                        .filter(REnumeration.class::isInstance)
                        .map(REnumeration.class::cast)
                        .orElse(null);
            }
        }
        return null;
    }

    /**
     * The disguised-navigation analogue of {@link #leafEnumeration} for a bare-enum comparison
     * operand's SIBLING (facet bare_enum_comparison_operand, PR #239): recover the enum of a sibling
     * that is a disguised {@link REnumValueRef} navigation whose RECEIVER is an ALIAS — e.g.
     * {@code quotedCurrencyPair(...) -> quoteBasis} parses as
     * {@code REnumValueRef(enumName=quotedCurrencyPair, valueName=quoteBasis)} where
     * {@code quotedCurrencyPair} is a function alias, so the leaf {@code quoteBasis} resolves only
     * through the alias's value type. {@link #leafEnumeration} resolves the disguised receiver with the
     * COMPILER-LESS {@link #resolveDisguisedFeature(REnumValueRef)} (input/output roots only) and so
     * returns {@code null} for an alias-headed chain; this method uses the COMPILER-CARRYING
     * {@link #resolveDisguisedFeature(REnumValueRef, ExpressionCompiler, java.util.Set)} alias arm to
     * recover the leaf and reads its enum type.
     *
     * <p>Deliberately a SEPARATE method scoped to {@code ComparisonHandler.tryBareEnumComparand}
     * rather than widening {@link #leafEnumeration} (which {@link #implicitItemEnumeration} also
     * consumes for the green to-string / to-enum conversion-source detection, where resolving more
     * alias-headed enums would change that byte-stable path). The recovered enum still qualifies the
     * bare operand only when it declares the value name, so a non-enum alias leaf declines.
     *
     * @return the disguised alias-headed sibling's leaf {@link REnumeration}, or {@code null} when the
     *         sibling is not a disguised {@link REnumValueRef}, its leaf does not resolve, or the leaf
     *         type is not an enum.
     */
    static REnumeration disguisedSiblingEnumeration(RExpression sibling, ExpressionCompiler compiler) {
        if (!(sibling instanceof REnumValueRef evr) || !evr.enumeration().isEmpty()) {
            return null;
        }
        RAttribute leaf = resolveDisguisedFeature(evr, compiler, null);
        if (leaf == null) {
            // facet bareEnumQualifyRBodyShadow (PR #299): a disguised receiver nav
            // ({@code regimeInformation -> supervisoryBody}) that the inference stage
            // already resolved (its {@code resolvedAttributeChain} is present) but whose
            // receiver is neither an input/output root nor an alias, so the input-/alias-
            // rooted {@link #resolveDisguisedFeature} cannot re-resolve it. Read the LEAF
            // feature off the already-resolved chain directly. Comparison-path-only (this
            // method is consumed solely by {@code ComparisonHandler.tryBareEnumComparand}),
            // so the green to-string / to-enum source detection is untouched.
            leaf = evr.resolvedAttributeChain()
                    .map(REnumValueRef.AttributeChain::feature)
                    .orElse(null);
        }
        if (leaf == null || leaf.typeCall() == null) {
            return null;
        }
        return leaf.typeCall().referencedType()
                .filter(REnumeration.class::isInstance)
                .map(REnumeration.class::cast)
                .orElse(null);
    }

    /**
     * The bare-attribute-reference analogue of {@link #siblingComparandEnumeration}
     * (facet bareEnumQualifyRBodyShadow, PR #299): recover the enum of a bare-enum
     * comparison operand's SIBLING when that sibling is a bare {@link RSymbolReference}
     * resolving directly to an enum-typed {@link RAttribute} — e.g. a filter-predicate
     * attribute reference {@code supervisoryBody} / {@code source} / {@code regimeName}
     * / {@code identifierType} whose type is {@code SupervisoryBodyEnum} /
     * {@code AssetIdTypeEnum} / {@code RegimeNameEnum} / {@code PartyIdentifierTypeEnum}
     * (the leaf may carry a {@code [metadata scheme]} annotation; the annotation is on
     * the ATTRIBUTE, not the type, so {@code typeCall().referencedType()} still resolves
     * the bare enum).
     *
     * <p>Deliberately a SEPARATE method scoped to {@code ComparisonHandler
     * .tryBareEnumComparand}: {@link #siblingComparandEnumeration} (which handles the
     * {@link RShortcut}/{@link RFunction} symbol siblings) is ALSO consumed by
     * {@code ConversionHandler}'s green to-string / to-enum source detection, where
     * admitting a bare enum-attribute source would change that byte-stable path. The
     * recovered enum still qualifies the bare operand only when it declares the value
     * name, so a non-enum attribute sibling declines.
     *
     * @return the sibling attribute's leaf {@link REnumeration}, or {@code null} when
     *         the sibling is not a bare {@link RSymbolReference} to an enum-typed
     *         {@link RAttribute}.
     */
    static REnumeration siblingAttributeEnumeration(RExpression sibling, ExpressionCompiler compiler) {
        if (!(sibling instanceof RSymbolReference sr)) {
            return null;
        }
        RAttribute attr = sr.symbol()
                .filter(RAttribute.class::isInstance)
                .map(RAttribute.class::cast)
                .orElse(null);
        if (attr == null || attr.typeCall() == null) {
            return null;
        }
        return attr.typeCall().referencedType()
                .filter(REnumeration.class::isInstance)
                .map(REnumeration.class::cast)
                .orElse(null);
    }

    /**
     * facet annotationShadowedSiblingEnum (PR #354): the sibling comparand is a bare
     * implicit-item attribute whose name the linker MIS-BOUND to a same-named root
     * element (the #204 non-value mis-binding class — {@code filter qualification =
     * confirmationDateTime} binds the {@code qualification} ANNOTATION, so the #299
     * {@link #siblingAttributeEnumeration} rung's {@link RAttribute} filter declines;
     * the P354M2 probe: {@code sibSym=RAnnotation}). The RENDER side re-roots the nav
     * through {@code ReferenceHandler.synthesizeImplicitItemBareNav} (the
     * #204/#282/#341/#346 admit + its green-safety decline ladder) — recover the enum
     * through the SAME synthesis (same-walk invariant: the recovered enum is the enum
     * the rendered nav actually types), with the #299 typeCall tail. The caller's
     * value-name match stays the load-bearing gate; a non-enum re-rooted attribute
     * declines here and the operand keeps its bare render.
     */
    static REnumeration misBoundSiblingAttributeEnumeration(RExpression sibling,
            ExpressionCompiler compiler) {
        if (!(sibling instanceof RSymbolReference sr) || !sr.args().isEmpty()) {
            return null;
        }
        boolean nonValueMisBind = sr.symbol().isEmpty()
                || sr.symbol().get() instanceof RDataType
                || sr.symbol().get() instanceof RChoice
                || sr.symbol().get() instanceof com.regnosys.rosetta.ast.types.RRecordType
                || sr.symbol().get() instanceof com.regnosys.rosetta.ast.types.REnumeration
                || sr.symbol().get() instanceof com.regnosys.rosetta.ast.types.RBasicType
                || sr.symbol().get() instanceof com.regnosys.rosetta.ast.regulatory.RSegmentDef
                || sr.symbol().get() instanceof com.regnosys.rosetta.ast.annotations.RAnnotation;
        if (!nonValueMisBind) {
            return null;
        }
        RFeatureCall nav = ReferenceHandler.synthesizeImplicitItemBareNav(sr, compiler);
        if (nav == null) {
            return null;
        }
        RAttribute attr = nav.resolvedFeature().orElse(null);
        if (attr == null || attr.typeCall() == null) {
            return null;
        }
        return attr.typeCall().referencedType()
                .filter(REnumeration.class::isInstance)
                .map(REnumeration.class::cast)
                .orElse(null);
    }

    /**
     * facet enumRequalifyComparison (PR #348): the sibling comparand is a DEEP
     * feature call ({@code observable -> Index ->> assetClass} — rendered through
     * the {@code <Type>DeepPathUtil} choose form) whose deep-resolved attribute
     * types an enumeration. The SAME {@link #resolveDeepFeature} walk the deep-call
     * render itself uses (render-truth lockstep), with
     * {@link #siblingAttributeEnumeration}'s typeCall→enumeration tail. Null for
     * every non-deep sibling or a non-enum deep leaf (the caller's decline).
     */
    static REnumeration deepSiblingEnumeration(RExpression sibling, ExpressionCompiler compiler) {
        if (!(sibling instanceof RDeepFeatureCall dfc)) {
            return null;
        }
        RAttribute attr = resolveDeepFeature(dfc, compiler);
        if (attr == null || attr.typeCall() == null) {
            return null;
        }
        return attr.typeCall().referencedType()
                .filter(REnumeration.class::isInstance)
                .map(REnumeration.class::cast)
                .orElse(null);
    }

    /**
     * facet bareItemPipeSiblingEnum (PR #391): the sibling comparand is the BARE
     * implicit item of a then-step lambda ({@code … then first then extract [if item
     * = LEI then …]} — the {@code mapSingleToItem(item -> areEqual(item,
     * MapperS.of(LEI), …))} seat). The item's element enum is the PIPE's — walk the
     * owning then's ARGUMENT past element-preserving steps (bare-item collapse /
     * distinct / filter / sort — the {@link #recoverThenArgItemRType} unwrap set) to
     * the element-PRODUCING body, then resolve THAT through the same sibling-rung
     * ladder the direct seats use ({@link #leafEnumeration} →
     * {@link #misBoundSiblingAttributeEnumeration} — the producer here is the
     * mis-bound bare {@code identifierType} extract body, the exact shape the #354
     * rung already resolves at the filter-predicate seat; golden drr
     * Extract_ReferenceEntityFormat). Null for a non-implicit sibling, an unowned
     * item, or an unresolvable producer — the caller's decline, today's bytes.
     */
    static REnumeration bareItemPipeSiblingEnumeration(RExpression sibling,
            ExpressionCompiler compiler) {
        if (!(sibling instanceof com.regnosys.rosetta.ast.expressions.references.RImplicitVariable)) {
            return null;
        }
        RNode n = sibling.parent();
        RInlineFunction binder = null;
        while (n != null) {
            if (n instanceof RInlineFunction f) {
                if (!(f.isImplicit() || f.paramNames().isEmpty())) {
                    return null; // an explicit-param lambda re-binds the item
                }
                binder = f;
                break;
            }
            n = n.parent();
        }
        if (binder == null) {
            return null;
        }
        // The then-step's body may WRAP the binder in an item-rooted extract
        // (`then extract [if item = LEI …]` parses as then(body=fn(extract(item,
        // fn(conditional)))) — the conditional's item binds to the EXTRACT's fn,
        // whose element IS the piped element). Ascend through such wraps only.
        RNode ownerNode = binder.parent();
        while (ownerNode instanceof RExtractExpr wrapExt
                && wrapExt.argument()
                        instanceof com.regnosys.rosetta.ast.expressions.references.RImplicitVariable
                && wrapExt.parent() instanceof RInlineFunction outerFn
                && (outerFn.isImplicit() || outerFn.paramNames().isEmpty())) {
            ownerNode = outerFn.parent();
        }
        if (!(ownerNode instanceof RThenExpr owner)) {
            return null;
        }
        RExpression producer = pipeElementProducer(owner.argument());
        if (producer == null || producer == sibling) {
            return null;
        }
        REnumeration en = leafEnumeration(producer, compiler);
        if (en == null) {
            en = siblingComparandEnumeration(producer, compiler);
        }
        if (en == null) {
            en = disguisedSiblingEnumeration(producer, compiler);
        }
        if (en == null) {
            en = siblingAttributeEnumeration(producer, compiler);
        }
        if (en == null) {
            en = deepSiblingEnumeration(producer, compiler);
        }
        if (en == null) {
            en = misBoundSiblingAttributeEnumeration(producer, compiler);
        }
        return en;
    }

    /**
     * facet bareItemPipeSiblingEnum (PR #391): walk a then-pipe past
     * element-PRESERVING steps (a then whose body is a bare-item list-op or a
     * bare-item filter; a standalone filter / sort / list-op node) to the
     * element-PRODUCING expression — a producing then-BODY (the {@code extract
     * identifierType} nav) or the base chain itself. Mirrors
     * {@link #recoverThenArgItemRType}'s unwrap set.
     */
    private static RExpression pipeElementProducer(RExpression pipe) {
        while (pipe != null) {
            if (pipe instanceof RThenExpr t) {
                RExpression body = t.body().map(RInlineFunction::body).orElse(null);
                boolean preserving =
                        (body instanceof RListOpExpr lo
                                && lo.argument()
                                        instanceof com.regnosys.rosetta.ast.expressions.references.RImplicitVariable)
                        || (body instanceof RFilterExpr f
                                && f.argument()
                                        instanceof com.regnosys.rosetta.ast.expressions.references.RImplicitVariable);
                if (preserving) {
                    pipe = t.argument();
                    continue;
                }
                return body;
            }
            if (pipe instanceof RFilterExpr f) {
                pipe = f.argument();
                continue;
            }
            if (pipe instanceof RSortExpr s) {
                pipe = s.argument();
                continue;
            }
            if (pipe instanceof RListOpExpr lo) {
                pipe = lo.argument();
                continue;
            }
            // facet bareEnumAliasShadowComparand (seat 28, law 10, half B): the pipe BASE
            // may itself be an element-PRODUCING extract (`extract ExtractReferenceEntity
            // -> identifierType then [if item = ...]`), whose BODY produces the piped
            // element - the same relationship the RThenExpr arm above already encodes for
            // a producing then-body. Without this the walk returned the extract NODE and
            // every sibling rung declined on it (the measured pipeRung=(null)
            // 17,539/17,539 - the #391 rung was born dead). An element-PRESERVING extract
            // (bare implicit body) keeps unwrapping through its argument.
            if (pipe instanceof RExtractExpr ex) {
                RExpression body = ex.body() == null ? null : ex.body().body();
                if (body != null) {
                    boolean preserving = body
                            instanceof com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
                    if (preserving) {
                        pipe = ex.argument();
                        continue;
                    }
                    return body;
                }
            }
            return pipe;
        }
        return null;
    }

    /**
     * Checks whether a cardinality represents a multi-valued attribute.
     *
     * <p>An attribute is multi-valued if its upper bound is unbounded ({@code *})
     * or explicitly greater than 1.
     */
    private static boolean isMultiValued(RCardinality card) {
        if (card.isUnbounded()) {
            return true;
        }
        return card.sup() != null && card.sup().intValue() > 1;
    }

    /**
     * facet caseNarrowedBareNavCardinality (seat 18): the BARE half of
     * {@code caseNarrowedDisguisedNav} (PR #368, F-B). Inside a choice-type switch arm the
     * subject is NARROWED to the case type, so a one-name navigation off it parses as a bare
     * {@link RSymbolReference} the linker resolves straight to an {@link RAttribute} — there is
     * no receiver node to recurse through, which is why {@link #chainProvesMulti} answers false
     * for it: its {@code RSymbolReference} arm re-roots only through
     * {@code synthesizeImplicitItem*Navigation}, and a case-narrowed subject is not an item.
     *
     * <p><b>LAW 69 — and the identity guard is what makes the two halves agree, not the shared
     * {@link #isMultiValued}.</b> The RENDER of that same bare name re-roots it on the narrowed
     * subject and picks {@code .mapC} vs {@code .map} from the cardinality of the attribute
     * {@code ReferenceHandler.synthesizeCaseNarrowedBareNav} resolves — a FRESH by-name lookup on
     * {@link #caseNarrowedImplicitType}'s result, which never consults
     * {@link RSymbolReference#symbol()}. Reading the linker's binding through the same
     * {@code isMultiValued} function is therefore NOT enough: they are two independently resolved
     * {@link RAttribute} objects, and this corpus really does declare the carrier name twice with
     * different cardinality (cdm 6.21.0 {@code product-asset-type.rosetta}:
     * {@code FloatingRateBase.capRateSchedule (0..1)} and
     * {@code StubFloatingRate.capRateSchedule (0..*)}). A disagreement there would drain a
     * {@code MapperS}-headed chain — non-compiling Java.
     *
     * <p>So this predicate performs the render's OWN resolution and requires the two to be the
     * SAME object — the identity guard {@code ReferenceHandler.synthesizeImplicitItemNavigation}
     * already applies for the item-rooted arm {@link #chainProvesMulti} delegates to. Only then
     * does it read the cardinality. {@link #caseNarrowedImplicitType} is the very function the
     * render's {@code activeCaseNarrowedType} delegates to, including its lambda boundary decline
     * (a closer inline function owns its own {@code item}), so the seat and the render walk one
     * resolution.
     *
     * <p>Deliberately NOT folded into {@link #chainProvesMulti}: that predicate is consulted from
     * many arms whose blast radius this seat did not measure. The caller keeps the narrow
     * consultation, and mirrors the render's remaining precondition (an ACTIVE switch-subject
     * binding) at its own site, where the scope is in hand.
     *
     * <p>Monotone add-only in BOTH directions the review named: an attribute resolving SINGLE
     * answers false, and an attribute the render would resolve to a DIFFERENT declaration answers
     * false rather than guessing.
     *
     * @return {@code true} only when the linker's binding for a bare symbol reference IS the
     *     attribute the render resolves on the case-narrowed type, and its cardinality is
     *     multi-valued
     */
    public static boolean caseNarrowedBareNavProvesMulti(RExpression e, ExpressionCompiler compiler) {
        if (!(e instanceof RSymbolReference symRef)) {
            return false;
        }
        RAttribute linked = symRef.symbol().filter(RAttribute.class::isInstance)
                .map(RAttribute.class::cast).orElse(null);
        if (linked == null) {
            return false;
        }
        RDataType narrowed = caseNarrowedImplicitType(symRef, compiler);
        if (narrowed == null
                || HandlerHelper.findAttributeOnDataType(narrowed, linked.name()) != linked) {
            return false;
        }
        return linked.cardinality().map(NavigationHandler::isMultiValued).orElse(false);
    }

    /**
     * Whether the LEAF navigated feature of {@code expr} is multi-valued, using the
     * SAME best-effort resolution {@link #resolveMapMethod} uses to choose
     * {@code mapC} over {@code map}.
     *
     * <p>Used by {@link ReferenceHandler#evaluateArgIsMulti} to recover the
     * cardinality of a function-call argument when the AST-level
     * {@code CardinalityComputer} is resolution-blind — it returns {@code SINGLE}
     * for a top-level function-body {@link RFeatureCall} whose {@code resolvedFeature}
     * was never set, and for a disguised {@link REnumValueRef} navigation (it has no
     * {@code REnumValueRef} case). The navigation side, by contrast, recovers the
     * leaf feature via the receiver-chain fallback and still emits {@code .mapC(...)}
     * for these, so the compiled argument IS a multi mapper; mirroring that
     * resolution here lets a multi nav-arg into a multi parameter unwrap with the
     * {@code .getMulti()} the golden uses, not the non-compiling scalar {@code .get()}.
     *
     * <p>By construction the multi-reading and the emitted mapper tail derive from
     * the same resolved {@link RAttribute}, so they cannot disagree (leaf-multi ⟺
     * {@code mapC} ⟺ golden {@code getMulti}). Reading the leaf can only
     * UNDER-count a multi-in-the-middle / scalar-leaf nav (which then keeps
     * {@code .get()} — a missed flip, never a wrong byte). Returns {@code false}
     * for any non-navigation expression or when resolution fails.
     */
    static boolean navLeafFeatureMulti(RExpression expr, ExpressionCompiler compiler) {
        if (expr instanceof RFeatureCall fc) {
            RAttribute attr = fc.resolvedFeature().orElseGet(() -> fallbackResolveFeature(fc, compiler));
            return attr != null
                    && attr.cardinality().map(NavigationHandler::isMultiValued).orElse(false);
        }
        if (expr instanceof REnumValueRef evr && evr.enumeration().isEmpty()) {
            RAttribute attr = resolveDisguisedFeature(evr);
            return attr != null
                    && attr.cardinality().map(NavigationHandler::isMultiValued).orElse(false);
        }
        return false;
    }

    /**
     * Converts a Rune feature name to the corresponding Java getter name.
     *
     * <p>Example: {@code "quantity"} → {@code "getQuantity"}.
     */
    private String toGetterName(String featureName) {
        if (featureName == null || featureName.isEmpty()) {
            return "get";
        }
        return "get" + JavaNamingUtil.toFirstUpper(featureName);
    }

    // =========================================================================
    // Deep feature call helpers
    // =========================================================================

    /**
     * Converts a Rune feature name to the corresponding deep path {@code choose}
     * method name.
     *
     * <p>Example: {@code "identifier"} → {@code "chooseIdentifier"}.
     * Example: {@code "economicTerms"} → {@code "chooseEconomicTerms"}.
     */
    private String toChooseMethodName(String featureName) {
        if (featureName == null || featureName.isEmpty()) {
            return "choose";
        }
        return "choose" + JavaNamingUtil.toFirstUpper(featureName);
    }

    /**
     * Resolves the generic type parameter prefix for a deep feature call, from the
     * gm-aware-recovered deep feature ({@link #resolveDeepFeature}).
     *
     * <p>Uses the deep feature's type call, same as {@link #resolveTypeParam(RFeatureCall)}.
     * When the attribute carries a {@code [metadata ...]} annotation, the type is wrapped via
     * {@link HandlerHelper#applyMetaWrapper(String, RAttribute)}.
     */
    private String resolveDeepTypeParam(RAttribute attr, ExpressionCompiler compiler) {
        if (attr == null || attr.typeCall() == null) return "";
        String baseName = resolveJavaSimpleName(attr, compiler);
        if (baseName == null) return "";
        return "<" + HandlerHelper.applyMetaWrapper(baseName, attr) + ">";
    }

    /**
     * Determines whether to use {@code map} or {@code mapC} for a deep feature call, from the
     * gm-aware-recovered deep feature's cardinality (upstream
     * {@code CardinalityProvider.caseDeepFeatureCall}: a multi deep feature → {@code mapC}).
     */
    // Static (was instance) since facet deepCallChainProvesMulti (v3.1 flip seat 29,
    // law 5): the STATIC cardinality overlay chainProvesMulti reads the render's OWN
    // map-method chooser, so the arity verdict and the emitted `.mapC(` cannot disagree
    // (LAW 69 - the #348 lockstep law at the overlay). Stateless (it reads only the
    // attribute's cardinality), so the change is modifier-only - exactly the PR #344
    // precedent recorded above resolveDeepFeature; both existing callers
    // (handle(RDeepFeatureCall) and chainRendersMapperC's rung R1) are instance methods
    // of this class and bind to the static form unchanged.
    private static String resolveDeepMapMethod(RAttribute attr) {
        return attr != null && attr.cardinality()
                .map(NavigationHandler::isMultiValued)
                .orElse(false)
                ? "mapC" : "map";
    }

    /**
     * gm-aware deep-feature resolution (facet deep_path_util_resolution, PR #205) — the
     * deep-arrow ({@code ->>}) sibling of
     * {@link #fallbackResolveFeature(RFeatureCall, ExpressionCompiler)}.
     *
     * <p>The parser leaves {@link RDeepFeatureCall#resolvedFeature()} empty for the dominant
     * carrier shape: a deep call whose receiver navigates through a {@code one-of}/choice type
     * (the parser's {@code findAttributeDeep} does not traverse choice-projected attributes —
     * gm-aware probe: {@code deepResolved=false} for the clean carriers), so the {@code <Type>}
     * witness and the map/mapC cardinality were lost and
     * {@link #handle(RDeepFeatureCall, ExpressionContext, ExpressionCompiler)} emitted the
     * {@code TODO(M7b-4)} stub. This recovers the deep feature attribute through the SAME gm-aware
     * receiver-type walk the witness path uses
     * ({@link #resolveReceiverDataType(RExpression, ExpressionCompiler)}), then a choice-aware deep
     * search ({@link #findDeepFeatureAttr}) mirroring upstream
     * {@code DeepFeatureCallUtil.findDeepFeaturePaths}. Prefers the pre-resolved feature when
     * present, so the already-resolved carriers stay byte-identical.
     *
     * @return the deep feature attribute, or {@code null} when the receiver type or the feature
     *         cannot be resolved (the handler then keeps the stub — byte-unchanged, still waivered)
     */
    // Static (was instance) since facet navWalkChoiceDisguise (PR #344): the static
    // receiver-type walk's RDeepFeatureCall arm reads the SAME resolution the deep-call
    // render reads. Stateless — the change is modifier-only.
    /**
     * v3.2 seat 13 (D53, the R12 heal's second mechanism): the leaf attribute a DEEP feature call ({@code o ->> codes})
     * resolves to - the SAME resolver the deep-path render and its injection consult, exposed for the whole-output
     * SET's element-wrapper recovery over a then-chain rooted at a deep path (the chaos s29 {@code C29InLambda} body).
     */
    public static RAttribute deepFeatureLeafAttribute(RDeepFeatureCall expr, ExpressionCompiler compiler) {
        return resolveDeepFeature(expr, compiler);
    }

    private static RAttribute resolveDeepFeature(RDeepFeatureCall expr, ExpressionCompiler compiler) {
        RAttribute pre = expr.resolvedFeature().orElse(null);
        if (pre != null) {
            return pre;
        }
        if (compiler == null) {
            return null;
        }
        RDataType receiverDt = resolveReceiverDataType(expr.receiver(), compiler);
        // facet choiceDeepNavLadderArm (PR #396): a deep call whose receiver is the
        // IMPLICIT ITEM inside a switch case reads the case guard's NARROWED type —
        // the deep-arrow sibling of the #395 switchCaseNarrowedImplicitLeafAttr
        // consult (null-fallback only; a resolved receiver keeps its answer).
        if (receiverDt == null) {
            RType narrowed = switchCaseNarrowedImplicitGuardRType(expr.receiver(), compiler);
            receiverDt = narrowed instanceof RDataTypeRef dtr ? dtr.astNode()
                    : narrowed instanceof com.regnosys.rosetta.types.RChoiceTypeRef ctr
                            ? ctr.asRDataType() : null;
        }
        if (receiverDt == null) {
            return null;
        }
        return findDeepFeatureAttr(receiverDt, expr.featureName(), compiler,
                Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    /**
     * Choice-aware deep-attribute search (the resolution worker of {@link #resolveDeepFeature}):
     * find an attribute named {@code featureName} reachable from {@code type}, recursing into each
     * attribute's data type (an {@link RChoiceTypeRef} projected to its data form by
     * {@link #attributeToDataType(RAttribute, ExpressionCompiler)}'s {@code asRDataType}). Mirrors
     * upstream {@code DeepFeatureCallUtil.findDeepFeaturePaths}. The {@code visited} identity set
     * bounds the walk to each data type once (cycle + revisit guard). Every deep-call receiver in
     * the corpus is, by construction, an upstream-eligible one-of type (its source compiled against
     * the generated {@code <Type>DeepPathUtil}), so no separate eligibility gate is needed: a
     * receiver whose {@code <Type>DeepPathUtil} lacks the {@code choose} method simply yields
     * output that stays divergent (still waivered) — never a regression.
     *
     * <p><b>Weakened vs upstream</b> (deliberate, resolution-equivalent for the 9.83.0 corpus): this
     * walk (a) recurses into EVERY nested attribute's data type — not only {@code choice} options —
     * and (b) matches by NAME only, where upstream matches the pre-resolved deep feature by
     * type+cardinality. For a future deep-call carrier with two same-named, differently-typed
     * features reachable through nested attributes, the first DFS name-match could pick a wrong-typed
     * attribute (a {@code <Type>}/map-vs-mapC skew). That is byte-verified correct for every shipped
     * carrier (Product→EconomicTerms single-card map; Index→AssetClassEnum map), and is regression-safe
     * by the same argument as above (a wrong pick can only affect a deep-call carrier, all waivered);
     * tighten to a type+cardinality match here before tackling broader deep-call families.
     */
    private static RAttribute findDeepFeatureAttr(RDataType type, String featureName,
            ExpressionCompiler compiler, Set<RDataType> visited) {
        if (type == null || !visited.add(type)) {
            return null;
        }
        RAttribute direct = HandlerHelper.findAttributeOnDataType(type, featureName);
        if (direct != null) {
            return direct;
        }
        for (RAttribute attr : collectAttributesWithSupertypes(type)) {
            RDataType nested = attributeToDataType(attr, compiler);
            if (nested != null) {
                RAttribute found = findDeepFeatureAttr(nested, featureName, compiler, visited);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** All attributes of a data type, including those inherited from its supertype chain. */
    private static List<RAttribute> collectAttributesWithSupertypes(RDataType type) {
        List<RAttribute> result = new ArrayList<>();
        Set<RDataType> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (RDataType t = type; t != null && seen.add(t); t = t.superType().orElse(null)) {
            result.addAll(t.attributes());
        }
        return result;
    }

    /**
     * Resolves the receiver type's simple name for a deep feature call — gm-aware
     * (facet deep_path_util_resolution, PR #205). Shared by
     * {@link #handle(RDeepFeatureCall, ExpressionContext, ExpressionCompiler)} (the lambda var +
     * the {@code <type>DeepPathUtil} field of the rendered body) and
     * {@code FunctionDependencyCollector} (the {@code @Inject <Type>DeepPathUtil} field), so the
     * injected field name and the lambda body reference agree by construction.
     *
     * @return the receiver type's Java simple name, or {@code null} when it cannot be resolved (the
     *         caller then REFUSES at {@code DEEP_PATH_UTIL_UNRESOLVED} - v3.2 seat 12, D52 R2; it kept
     *         the {@code TODO(M7b-4)} placeholder until then)
     */
    public static String resolveDeepReceiverTypeName(RDeepFeatureCall expr, ExpressionCompiler compiler) {
        JavaClass<?> jc = resolveDeepReceiverJavaClass(expr, compiler);
        if (jc != null) {
            return jc.getSimpleName();
        }
        // Fallback when the JavaClass path cannot resolve — the receiver feature call's resolved
        // type name (the pre-PR-#205 resolvedFeature()-only construct). Reached by stateless unit
        // calls (null compiler) and by isolated unit setups whose synthetic types are detached
        // (toJavaReferenceType degrades to Object). Every PRODUCTION flip carrier takes the JavaClass
        // path above (its receiver resolves to a concrete model type — byte-verified), so this
        // fallback does NOT widen the flip set. The collector's @Inject field is gated on
        // {@link #resolveDeepReceiverSymbolId} (the SAME JavaClass path), so when the JavaClass path
        // succeeds the renderer name and the collector field agree by construction. In the residual
        // window (compiler present but the receiver type genuinely unresolvable via the JavaClass
        // path, yet its feature call carries a resolvedFeature) this fallback gives the renderer a
        // name while the collector falls to its findDataTypeByName BC path — the two MAY then disagree
        // on whether the @Inject field is emitted (or its namespace). That is harmless: such a file is
        // a co-occupied deep-call carrier that stays divergent/waivered regardless (there are zero
        // green deep-call files), so it can never regress a green file — see findDeepFeatureAttr.
        RExpression receiver = expr.receiver();
        if (receiver instanceof RFeatureCall fc) {
            return fc.resolvedFeature().map(RAttribute::typeCall).map(tc -> tc.typeName()).orElse(null);
        }
        return null;
    }

    /**
     * gm-aware resolution of a deep feature call's RECEIVER type's {@link ModelSymbolId}
     * (facet deep_path_util_resolution, PR #205). {@code FunctionDependencyCollector} uses this to
     * derive the correct {@code @Inject <Type>DeepPathUtil} import — the symbol id carries the
     * NAMESPACE, so it is unambiguous (unlike a name-only lookup, which cannot tell
     * {@code cdm.product.template.Product} from {@code fpml.consolidated.shared.Product}). Derived
     * from the SAME {@link #resolveDeepReceiverJavaClass} as {@link #resolveDeepReceiverTypeName}
     * (the renderer's lambda var + field name), so the field name and its type agree by construction.
     *
     * @return the receiver type's symbol id, or {@code null} for a {@code null} compiler/gm or an
     *         unresolvable receiver
     */
    public static ModelSymbolId resolveDeepReceiverSymbolId(RDeepFeatureCall expr, ExpressionCompiler compiler) {
        JavaClass<?> jc = resolveDeepReceiverJavaClass(expr, compiler);
        return jc == null ? null : new ModelSymbolId(jc.getPackageName(), jc.getSimpleName());
    }

    /**
     * Resolves a deep feature call's RECEIVER type to its generated {@link JavaClass} (carrying
     * BOTH the package — for the {@code <Type>DeepPathUtil} namespace — and the simple name — for the
     * lambda var / field). Routes the receiver through the gm-aware
     * {@link #resolveReceiverRType(RExpression, GeneratorModel, ExpressionCompiler)} (the SAME walk
     * the witness path uses) then {@link JavaTypeTranslator#toJavaReferenceType(RType)}.
     *
     * <p>This is the reliable namespace source: the AST {@code referencedType()} slot's SymbolId is
     * often UNSET for a deep-call receiver typeCall (gm-aware probe: {@code id=null}), so a
     * workspace-SymbolId resolve fails; and {@code gm.resolveTypeCall(..).astNode()} / a choice's
     * {@code asRDataType()} projection is DETACHED (no parent {@code RModel} → {@code symbolId}
     * throws). {@code toJavaReferenceType} resolves the rooted package directly (probe:
     * {@code jrt=cdm.product.template.Product} for every carrier). Declines (returns {@code null})
     * when the receiver type cannot be resolved or degrades to {@code Object} (no DeepPathUtil
     * exists for a non-model type) — those carriers REFUSE at {@code DEEP_PATH_UTIL_UNRESOLVED} since
     * v3.2 seat 12 (D52 R2; they kept the {@code TODO(M7b-4)} stub, waivered, until then).
     */
    private static JavaClass<?> resolveDeepReceiverJavaClass(RDeepFeatureCall expr, ExpressionCompiler compiler) {
        if (compiler == null) {
            return null;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        JavaTypeTranslator tt = compiler.getTypeTranslator();
        if (gm == null || tt == null) {
            return null;
        }
        RType rt = resolveReceiverRType(expr.receiver(), gm, compiler);
        // facet choiceDeepNavLadderArm (PR #396): the case-narrowed implicit receiver —
        // the SAME null-fallback consult as resolveDeepFeature, so the renderer's
        // <type>DeepPathUtil field/lambda var AND FunctionDependencyCollector's @Inject
        // (via resolveDeepReceiverSymbolId, routed through this method) agree on the
        // guard type by construction.
        if (rt == null) {
            rt = switchCaseNarrowedImplicitGuardRType(expr.receiver(), compiler);
        }
        // v3.1 flip seat 28 - facet deepReceiverBoundItemFallback: a deep call whose
        // receiver is a BARE, parser-UNBOUND name on the enclosing lambda's implicit item
        // (`Index ->> identifier` inside `then extract [...]`, the #347-F8
        // choice-super-option class - UNBOUND like UnderlierProductIdentifier's, or
        // MIS-BOUND to the type symbol like GetBasketConstituents': the rung keys on the
        // ARMS ABOVE having produced nothing, not on symbol emptiness, because the
        // mis-bound case leaves symbol() present but non-attribute so tc stayed null)
        // resolves the SAME #348 synthesizeImplicitItemBareNav
        // synthesis that RENDERED the receiver's `<Index>map("getIndex", ...)` text (so
        // the field name and the emitted chain cannot disagree), then the resolved
        // attribute's typeCall through the SAME gm.resolveTypeCall + stripAliases tail
        // resolveReceiverRType's own arms use - the by-name workspace resolution this
        // method's javadoc names as the reliable namespace source (LAW 69: the render
        // half knew the receiver's type, this admission half did not). NOT the detached
        // attributeToDataType(...).astNode() projection: wrapping that node throws the
        // C0 unreachable-declaring-model refusal at toJavaReferenceType (measured on the
        // first cut of this seat - GetBasketConstituents refused whole-file). The rung
        // is scoped to THIS resolver (not the shared resolveReceiverRType walk) so its
        // reach is exactly the deep-call receiver seat. resolveReceiverRType's
        // RSymbolReference arms resolve an ATTRIBUTE / dispatch-base / FUNCTION / RULE
        // symbol and have no arm for an unbound bare name, so the receiver type was lost
        // and handle(RDeepFeatureCall) emitted the `/* TODO(M7b-4): wire DeepPathUtil */`
        // stub while FunctionDependencyCollector (gated on the SAME resolver through
        // resolveDeepReceiverSymbolId) emitted neither the @Inject <Type>DeepPathUtil
        // field nor its import. Note the receiver's type is NOT the bound item type: the
        // bound item is the ROOT the bare name is re-rooted onto (BasketConstituent), and
        // the receiver is one hop further on (Index) - the synthesis hop takes it. Sits
        // LAST, so it fires only where both existing rungs returned null - monotone by
        // construction. MEASURED: `fellBack=true` occurs 20 times corpus-wide, in exactly
        // 12 band files (GetBasketConstituents, UnderlierProductIdentifier,
        // GetUnderlierProductIdentifierLeg1 x drr 7.0-7.3) and NEVER in a green file
        // (PROBE28-F16, 756 lines, both routes), cross-checked 1:1 against the 20
        // emitted TODO(M7b-4) placeholders.
        if (rt == null && expr.receiver() instanceof RSymbolReference bareRecvSr) {
            RFeatureCall bareRecvReroot =
                    ReferenceHandler.synthesizeImplicitItemBareNav(bareRecvSr, compiler);
            RAttribute bareRecvAttr = bareRecvReroot == null ? null
                    : bareRecvReroot.resolvedFeature().orElse(null);
            if (bareRecvAttr != null && bareRecvAttr.typeCall() != null) {
                rt = HandlerHelper.stripAliases(gm.resolveTypeCall(bareRecvAttr.typeCall()));
            }
        }
        // v3.2 seat 6 (F5, facet lambdaItemDeepReceiver): a deep call whose receiver is a list-op LAMBDA'S ITEM -
        // the implicit `item` of an extract / filter / max / min / sort / then body (the chaos s03 C3Commons shape,
        // `outers extract item ->> text`) or the lambda's EXPLICIT parameter (`extract o [ o ->> text ]`). The WITNESS
        // half already typed it: resolveDeepFeature reads resolveReceiverDataType, whose RImplicitVariable arm walks
        // implicitItemDataType, so the render carried the right `<String>` witness and `map` hop; THIS half - the
        // render's `<type>DeepPathUtil` field / lambda var AND FunctionDependencyCollector's @Inject (through
        // resolveDeepReceiverSymbolId) - walked resolveReceiverRType, which has no lambda-item arm, so both halves
        // declined together: the `TODO(M7b-4)` placeholder and NO injection, a non-compiling emission the D11 byte
        // compare cannot tell from any other (LAW 69: the witness knew the receiver's type, the admission did not).
        // The item's ATTACHED RType comes from the owning argument (lambdaOwnerArgument - the SAME owner walk the
        // naming reads) through recoverThenArgItemRType (filter / extract / then / sort / list-op wrappers unwrapped to
        // the element leaf, then resolveReceiverRType -> gm.resolveTypeCall): a choice resolves to its RChoiceTypeRef,
        // which toJavaReferenceType maps to the choice's own class - the seat-28 precedent, never the detached
        // asRDataType() projection (the C0 unreachable-declaring-model refusal). Sits LAST: it fires only where every
        // rung above returned null - monotone by construction. MEASURED reach (the seat's census): ZERO vendored
        // carriers on 25 cells (0 of 27,521 function goldens carry a map lambda whose body starts with a deep hop on
        // the lambda var); the oracle groups deep-path-util-injection / -edge (the released 9.83.0 plugin) and the 11
        // chaos C3Commons rows are the witnesses.
        if (rt == null) {
            rt = lambdaItemReceiverRType(expr.receiver(), gm, compiler);
        }
        if (rt == null) {
            return null;
        }
        JavaClass<?> jc;
        try {
            jc = tt.toJavaReferenceType(rt);
        } catch (RuntimeException ex) {
            // v3.1 C0: a REFUSAL is never recovery — it must reach the per-element
            // boundary (JavaClassGenerator), which attaches the target path and reports
            // it as a generation error. Locked by scripts/ci/refusal-propagation-lint.py.
            if (ex instanceof SilentDegradation.Refusal __refusal) throw __refusal;
            return null;
        }
        if (jc == null || "java.lang.Object".equals(jc.getCanonicalName().withDots())) {
            return null;
        }
        return jc;
    }

    /**
     * v3.2 seat 6 (F5, facet lambdaItemDeepReceiver): the ATTACHED {@link RType} of a deep call's LAMBDA-ITEM receiver -
     * an {@link RImplicitVariable} inside an extract / filter / max / min / sort / then body, or the body's EXPLICIT
     * closure parameter (an args-free {@link RSymbolReference} whose name an enclosing inline function declares -
     * {@code ReferenceHandler.enclosingClosureParamOwner}, the SAME Java-shadowing owner walk the naming and the
     * closure-param disguised-leaf reads use). The item is the owner argument's ELEMENT
     * ({@link #lambdaOwnerArgument}), recovered by {@link #recoverThenArgItemRType}; {@code null} for every other
     * receiver shape, an owner the walk does not admit or an unresolvable argument - the caller keeps its decline.
     * Read by {@link #resolveDeepReceiverJavaClass} alone (the ONE resolver the render and the collector share).
     *
     * <p>The explicit-parameter arm and a THEN owner (the round-1 code-quality review's SF-5): {@code lambdaOwnerArgument}
     * has an {@code RThenExpr} arm, so a then-DECLARED parameter ({@code X then p [ p ->> y ]}) would be admitted here
     * where the sibling closure-param consults (the #180/#331 gate in this class, {@code ComparisonHandler},
     * {@code SetOperationHandler}) decline it. No oracle-valid model can write that shape: the released 9.83.0 grammar's
     * {@code then} takes a bare implicit body ({@code ImplicitInlineFunction: body=OrOperation}, Rosetta.xtext), and the
     * plugin REFUSED the probe {@code outers then p [ p ->> text ]} at the grammar - "no viable alternative at input
     * 'p'" (target/v32-seat6-instruments/scratch/oracle-s6c-thenparam-refusal-run1.log, local). The shape reaches the
     * fork only through its own grammar's over-acceptance ({@code thenSuffix: THEN (inlineFunction |
     * implicitInlineFunction)?}), a parser-level divergence BANKED in the seat plan with that receipt; this arm's
     * {@code RThenExpr} owner is the IMPLICIT then item's recursion (cut 2: a4, b7), not the explicit parameter's.
     */
    static RType lambdaItemReceiverRType(RExpression receiver, GeneratorModel gm, ExpressionCompiler compiler) {
        return lambdaItemReceiverRType(receiver, gm, compiler, 0);
    }

    private static RType lambdaItemReceiverRType(RExpression receiver, GeneratorModel gm, ExpressionCompiler compiler,
            int depth) {
        if (compiler == null || gm == null || depth >= HandlerHelper.PARENT_WALK_LIMIT) {
            return null;
        }
        RExpression ownerArg = null;
        if (receiver instanceof RImplicitVariable) {
            ownerArg = implicitItemArgument(receiver);
        } else if (receiver instanceof RSymbolReference sr && sr.args().isEmpty() && sr.name() != null) {
            RInlineFunction owner = ReferenceHandler.enclosingClosureParamOwner(sr, sr.name());
            ownerArg = owner == null ? null : lambdaOwnerArgument(owner);
        }
        if (ownerArg == null) {
            return null;
        }
        // A THEN-PIPED owner (`X then extract item ->> y`, `then filter [...]`, the rule's `extract picks then extract
        // item ->> common`): the inner lambda's owner argument is the then body's OWN implicit item, a shape
        // recoverThenArgItemRType has no arm for - resolve it as a lambda item in its own right (the then's implicit ->
        // the then's argument, unwrapped by the same walk). Measured at the fix head: ThenChain's extract step and the
        // reporting rule kept the placeholder while their filter / first steps healed (scratch/fix-dump/, cut 1).
        if (ownerArg instanceof RImplicitVariable) {
            return lambdaItemReceiverRType(ownerArg, gm, compiler, depth + 1);
        }
        return recoverThenArgItemRType(ownerArg, compiler);
    }

    /**
     * v3.2 seat 6 (F5, facet deepBodyMapMethod): the cardinality of a deep feature call AS THE RENDER READS IT, at the
     * extract chooser's body read ({@code CollectionHandler.isBodyMulti}) - CONSULTS the deep arm of
     * {@link #chainProvesMulti} (ONE declaration, LAW 69 - the round-1 code-quality review's SF-1: the first cut
     * restated that arm's two calls, {@link #resolveDeepMapMethod} over {@link #resolveDeepFeature}, character for
     * character), where the parser computer's GLOBAL {@code RDeepFeatureCall} arm answers a blanket MULTI (the #443
     * split; {@code CardinalityComputer:155}, DO-NOT-TOUCH). An unresolvable deep feature reads "map" (the null
     * contract) and the body is single. THE RECEIVER TERM IS NOT HERE, BY MEASUREMENT (the review's MF-1): the first
     * cut carried {@code || chainProvesMulti(dfc.receiver(), compiler)} as upstream's {@code isMulti(receiver)} for a
     * list-of-lists lambda item - but {@code chainProvesMulti}'s implicit-variable arm is THEN-only
     * ({@link #thenOwnerArgument}'s recorded under-mirror: an extract / filter / max / min / sort item is per-element,
     * the list-of-lists case declined), so for the shape it was offered the term could never fire; lane B2 deleted it
     * and the seat suite stayed GREEN. It is the SAME term v3.1 seat 30 severed from {@code chainProvesMulti}'s own deep
     * arm after measuring it empty twice ("ONE TERM, BY MEASUREMENT" at that arm). BANKED with its carrier named in
     * the seat plan (the oracle group {@code deep-path-lol-item}); it re-enters with a fixture, never on the table alone.
     */
    public static boolean deepBodyProvesMulti(RDeepFeatureCall dfc, ExpressionCompiler compiler) {
        return chainProvesMulti(dfc, compiler);
    }

}
