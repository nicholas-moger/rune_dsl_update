package com.regnosys.rosetta.generator.java.expression;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.expressions.binary.*;
import com.regnosys.rosetta.ast.expressions.constructors.*;
import com.regnosys.rosetta.ast.expressions.literals.*;
import com.regnosys.rosetta.ast.expressions.references.*;
import com.regnosys.rosetta.ast.expressions.unary.*;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.expression.handlers.ArithmeticHandler;
import com.regnosys.rosetta.generator.java.expression.handlers.CollectionHandler;
import com.regnosys.rosetta.generator.java.expression.handlers.ComparisonHandler;
import com.regnosys.rosetta.generator.java.expression.handlers.ConstructionHandler;
import com.regnosys.rosetta.generator.java.expression.handlers.ControlFlowHandler;
import com.regnosys.rosetta.generator.java.expression.handlers.ConversionHandler;
import com.regnosys.rosetta.generator.java.expression.handlers.ExistenceHandler;
import com.regnosys.rosetta.generator.java.expression.handlers.LiteralHandler;
import com.regnosys.rosetta.generator.java.expression.handlers.LogicalHandler;
import com.regnosys.rosetta.generator.java.expression.handlers.NavigationHandler;
import com.regnosys.rosetta.generator.java.expression.handlers.ReferenceHandler;
import com.regnosys.rosetta.generator.java.expression.handlers.SetOperationHandler;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;

/**
 * Compiles Rune DSL expression AST nodes into {@link JavaStatementBuilder} fragments.
 *
 * <p>Implements the exhaustive {@link RExpressionVisitor} interface. All visit
 * methods are delegated to typed handler classes. All 12 handlers are wired:
 * Literal, Arithmetic, Comparison, Logical, SetOperation, Reference, Existence,
 * Conversion, Navigation, ControlFlow, Construction, and Collection.
 */
public class ExpressionCompiler implements RExpressionVisitor<JavaStatementBuilder, ExpressionContext> {

    // Type infrastructure (null in zero-arg / legacy mode)
    private final GeneratorModel generatorModel;
    private final JavaTypeTranslator typeTranslator;
    private final JavaTypeUtil typeUtil;
    private final TypeCoercionService coercionService;

    private final LiteralHandler literalHandler;
    private final ArithmeticHandler arithmeticHandler;
    private final ComparisonHandler comparisonHandler;
    private final LogicalHandler logicalHandler;
    private final SetOperationHandler setOperationHandler;
    private final ReferenceHandler referenceHandler;
    private final ExistenceHandler existenceHandler;
    private final ConversionHandler conversionHandler;
    private final NavigationHandler navigationHandler;
    private final ControlFlowHandler controlFlowHandler;
    private final ConstructionHandler constructionHandler;
    private final CollectionHandler collectionHandler;

    /**
     * Typed constructor — enables type coercion.
     *
     * @param gm the generator model (may be {@code null})
     * @param tt the Java type translator (may be {@code null})
     * @param tu the Java type utility; when non-null, coercion is active
     */
    public ExpressionCompiler(GeneratorModel gm, JavaTypeTranslator tt, JavaTypeUtil tu) {
        this.generatorModel = gm;
        this.typeTranslator = tt;
        this.typeUtil = tu;
        this.coercionService = (tu != null) ? new TypeCoercionService(tu) : null;
        this.literalHandler = createLiteralHandler();
        this.arithmeticHandler = new ArithmeticHandler();
        this.comparisonHandler = new ComparisonHandler();
        this.logicalHandler = new LogicalHandler();
        this.setOperationHandler = new SetOperationHandler();
        this.referenceHandler = new ReferenceHandler();
        this.existenceHandler = new ExistenceHandler();
        this.conversionHandler = new ConversionHandler();
        this.navigationHandler = new NavigationHandler();
        this.controlFlowHandler = createControlFlowHandler();
        this.constructionHandler = new ConstructionHandler();
        this.collectionHandler = createCollectionHandler();
    }

    /**
     * Factory for the {@link CollectionHandler} — handles list ops, {@code count}, and the
     * operand-position {@code then}-chains that reach {@code visitThen}. Extracted as an
     * overridable factory (the D43 IR seam; the lab's Wave-6 Phase-C "Step 0", decision-log
     * L-060/L-065) so the IR-routed compiler ({@code rune-ir-java}'s {@code IRExpressionCompiler})
     * can supply an IR-aware {@link CollectionHandler} subclass that sources the operand-position
     * {@code thenArg} hoist NAME from the neutral ANF substrate (the L-029 split — the IR drives
     * the neutral name; the handler renders the Java). The base returns the standard handler, so
     * Path-1 is byte-identical. Invoked ONCE during construction; the handler is stateless and
     * depends on NO subclass state (all type infra + per-call context are passed to
     * {@code handle}), so a subclass override is init-order-safe — the standard
     * base-ctor-calls-overridable-factory pattern, mirroring
     * {@code FunctionGenerator.createFunctionExpressionRenderer}.
     *
     * @return the {@link CollectionHandler} this compiler dispatches {@code then}/list-op/{@code count} to
     */
    protected CollectionHandler createCollectionHandler() {
        return new CollectionHandler();
    }

    /**
     * v3.1 flip seat 24 (law F5): the literal handler this compiler dispatches to — the {@code default}
     * seat consults its BigInteger-hoist producer ({@code registerBigIntegerLiteralHoistOrNull}) so the
     * IR-routed subclass's base-name seam is honoured there too.
     */
    public LiteralHandler getLiteralHandler() {
        return literalHandler;
    }

    /**
     * Factory for the {@link LiteralHandler} — handles the six literal kinds, including the
     * beyond-{@code long} {@code bigInteger} literal-hoist (facet {@code biginteger_literal_hoist}).
     * Extracted as an overridable factory (the D43 IR seam; the lab's Wave-6 Phase-C slice-5,
     * decision-log L-068) so the IR-routed compiler can supply an IR-aware {@link LiteralHandler}
     * subclass that sources the {@code bigInteger} hoist NAME from the neutral ANF substrate (the
     * L-029 split). The base returns the standard handler, so Path-1 is byte-identical. Same
     * init-order-safety rationale as {@link #createCollectionHandler()}.
     *
     * @return the {@link LiteralHandler} this compiler dispatches the literal kinds to
     */
    protected LiteralHandler createLiteralHandler() {
        return new LiteralHandler();
    }

    /**
     * Factory for the {@link ControlFlowHandler} — handles conditional ({@code if/then/else})
     * lowering, including the statement-hoist {@code ifThenElseResult} local (the ComparisonResult
     * arm + the ctor item-local arm). Extracted as an overridable factory (the D43 IR seam; the
     * lab's Wave-6 Phase-C slice-6) so the IR-routed compiler can supply an IR-aware
     * {@link ControlFlowHandler} subclass that sources the {@code ifThenElseResult} hoist NAME from
     * the neutral ANF substrate (the L-029 split). The base returns the standard handler, so Path-1
     * is byte-identical. Same init-order-safety rationale as {@link #createCollectionHandler()}.
     *
     * @return the {@link ControlFlowHandler} this compiler dispatches conditionals to
     */
    protected ControlFlowHandler createControlFlowHandler() {
        return new ControlFlowHandler();
    }

    /**
     * Zero-arg constructor — preserved for backward compatibility.
     * Delegates to the typed constructor with a default {@link JavaTypeUtil}.
     * Coercion is active but M7b-1 handlers produce null expression types,
     * so coercion remains dormant until M7b-3 wires real types through.
     */
    public ExpressionCompiler() {
        this(null, null, new JavaTypeUtil());
    }

    // =========================================================================
    // Type infrastructure accessors (handlers may use these in M7b-3+)
    // =========================================================================

    /** Returns the {@link GeneratorModel} supplied at construction, or {@code null}. */
    public GeneratorModel getGeneratorModel() { return generatorModel; }

    /** Returns the {@link JavaTypeTranslator} supplied at construction, or {@code null}. */
    public JavaTypeTranslator getTypeTranslator() { return typeTranslator; }

    /** Returns the {@link JavaTypeUtil} supplied at construction, or {@code null}. */
    public JavaTypeUtil getTypeUtil() { return typeUtil; }

    /**
     * Returns this compiler's {@link ReferenceHandler} — the same instance the
     * {@code visitSymbolReference} fallback delegates to. Exposed (the D43 IR seam) so the
     * IR-routed subclass can reuse a handler oracle (e.g.
     * {@link ReferenceHandler#renderImplicitRuleInvocation}) VERBATIM rather than re-deriving a
     * parity-risky render IR-side, calling it on the exact instance the legacy path would.
     */
    public ReferenceHandler getReferenceHandler() { return referenceHandler; }

    /**
     * Returns this compiler's {@link CollectionHandler} — the same instance the
     * {@code visitListOp}/{@code visitCount} fallback delegates to. Exposed (the D43 IR seam) so
     * the IR-routed subclass can reuse the flat list-op render oracle
     * ({@code CollectionHandler.handle}) VERBATIM for an IR list-op, calling it on the exact
     * instance the legacy path would (the lab's L-049 reuse discipline, applied to collections).
     */
    public CollectionHandler getCollectionHandler() { return collectionHandler; }

    /**
     * The shared stateless {@link ConversionHandler}, so the IR-routed compiler can reuse its
     * {@code handle(RToStringExpr)} oracle VERBATIM for an IR to-string (the lab's L-050 reuse,
     * applied to {@code to-string}). The D43 IR seam.
     */
    public ConversionHandler getConversionHandler() { return conversionHandler; }

    /**
     * Returns the {@link TypeCoercionService}, or {@code null} only when the
     * typed constructor was given a {@code null} {@link JavaTypeUtil}. The
     * zero-arg constructor supplies a default {@code JavaTypeUtil}, so the
     * service is NON-null there (matching {@link #getTypeUtil()}). Renderers
     * that need to coerce a single item expression directly — e.g.
     * {@code FunctionExpressionRenderer.renderBareInvokableThenSet} coercing a
     * piped value to a then-target's input type — go through this rather than
     * re-deriving the conversion form, so the conversion stays a single source
     * of truth with {@link #compile} and {@link #coerceNavigationReceiver}.
     */
    public TypeCoercionService getCoercionService() { return coercionService; }

    /**
     * facet metaWrapperRecovery (v3.1 C2d retirement family 6, PR #613): what a block-lambda /
     * {@code default} producer DECIDED about its element, recorded beside the text it rendered
     * (LAW 69) so the then-arg decl seats read the decision instead of scanning the rendered block
     * for {@code return MapperS.<X>ofNull();} / {@code .<X>map("Type coercion", …).getValue());} /
     * {@code .getValue()).getOrDefault(} markers or the emitted-imports set for a wrapper class.
     * Housed on this compiler — its registry's owner (the seat's code-quality review) — and keyed
     * by the AST node the producer compiled, NOT by a scope: the producers record from deep inside
     * lambda sub-scopes whose statement-scope root is not always the method scope the decl seats
     * read from (the family's census measured 19 of 702 block-marker fires unreadable through the
     * scope root).
     *
     * @param typedEmptyElement the element the block's typed-empty terminal RENDERS, or
     *     {@code null} when the block renders no typed empty
     * @param typedEmptyMulti whether that typed empty is the {@code MapperC} form
     * @param joinBare whether the producer joined its arms BARE (an arm deref pass fired)
     * @param derefdLeftValue the LEFT operand's bare value type when a {@code default} seat's
     *     LEFT deref arm fired, else {@code null}
     */
    public record LambdaJoinFacts(com.rosetta.util.types.JavaClass<?> typedEmptyElement,
            boolean typedEmptyMulti, boolean joinBare,
            com.rosetta.util.types.JavaClass<?> derefdLeftValue) {}

    /**
     * facet metaWrapperRecovery (v3.1 C2d retirement family 6, PR #613): the
     * {@link LambdaJoinFacts} registry — one identity map per compilation, keyed by the AST node
     * the producer compiled. Held here, beside the coercion service's deref ledger, because both
     * the block-lambda / {@code default} producers and the then-arg decl seats hold this compiler,
     * whereas a scope root is not always shared between a lambda sub-scope and the method scope.
     */
    private final java.util.Map<RExpression, LambdaJoinFacts> lambdaJoinFacts =
            new java.util.IdentityHashMap<>();

    /**
     * facet metaWrapperRecovery (v3.1 C2d retirement family 6, PR #613): record
     * {@code producerNode}'s join facts, keyed by AST-node identity. A later record for the same
     * node overwrites the earlier (each producer records once, at the point its verdict is final).
     */
    public void recordLambdaJoinFacts(RExpression producerNode, LambdaJoinFacts facts) {
        if (producerNode != null && facts != null) {
            lambdaJoinFacts.put(producerNode, facts);
        }
    }

    /**
     * facet metaWrapperRecovery (v3.1 C2d retirement family 6, PR #613): the facts recorded for
     * {@code producerNode}, or {@code null} if none. The ONE point every read goes through.
     */
    public LambdaJoinFacts lambdaJoinFactsFor(RExpression producerNode) {
        return producerNode == null ? null : lambdaJoinFacts.get(producerNode);
    }

    /**
     * facet metaWrapperRecovery (v3.1 C2d retirement family 6, PR #613): a rollback point over the
     * two producer channels this compiler owns — the {@link LambdaJoinFacts} registry and the
     * coercion service's deref ledger. Taken before an ATTEMPT whose render may be discarded (a
     * block-lambda dispatch attempt, the ladder's text-order rewalk, the conditional-arm probe);
     * {@link #restoreProducerChannels} rolls both channels back so a record made beside DISCARDED
     * text cannot outlive it (LAW 69: the fact stands only while the text it was recorded beside
     * is the kept render — the seat's code-quality review, MF-1).
     */
    public record ProducerChannelMark(java.util.Map<RExpression, LambdaJoinFacts> facts,
            int derefMark) {}

    /** Capture both producer channels. See {@link ProducerChannelMark}. */
    public ProducerChannelMark markProducerChannels() {
        return new ProducerChannelMark(new java.util.IdentityHashMap<>(lambdaJoinFacts),
                coercionService == null ? 0 : coercionService.derefMark());
    }

    /**
     * Roll both producer channels back to {@code mark} — called when the compile the mark bracketed
     * was DISCARDED (its render is not the kept text). See {@link ProducerChannelMark}.
     */
    public void restoreProducerChannels(ProducerChannelMark mark) {
        if (mark == null) {
            return;
        }
        lambdaJoinFacts.clear();
        lambdaJoinFacts.putAll(mark.facts());
        if (coercionService != null) {
            coercionService.truncateDerefLedger(mark.derefMark());
        }
    }

    /**
     * Entry point: compile an expression with an optional expected type and scope.
     *
     * <p>If a {@link TypeCoercionService} is available and both {@code expectedType}
     * and the result's expression type are non-null, coercion is applied.
     * M7b-1 handlers pass null expression types, so coercion is dormant until M7b-3.
     *
     * @param expr         the expression to compile
     * @param expectedType the target Java type (may be {@code null} for M7b-1)
     * @param scope        the current variable scope
     * @return the compiled statement builder
     */
    public JavaStatementBuilder compile(RExpression expr, JavaType expectedType, JavaStatementScope scope) {
        var context = ExpressionContext.of(expectedType, scope);
        var result = expr.accept(this, context);
        // Coercion activates when handlers produce typed expressions.
        // M7b-1 handlers pass null types, so coercion is dormant until M7b-3.
        if (coercionService != null && expectedType != null && result.getExpressionType() != null) {
            result = coercionService.coerce(result, result.getExpressionType(), expectedType, scope);
        }
        return result;
    }

    /**
     * Interior-position entry point: compile a sub-expression that continues a
     * mapper CHAIN — a navigation receiver, a count operand, a list-op argument —
     * WITHOUT the terminal entry coercion {@link #compile} applies (facet
     * interior_position_coercion).
     *
     * <p>Upstream compiles interior chain positions against WRAPPER-level
     * expecteds: a feature-call receiver against
     * {@code MAPPER.wrapExtendsWithoutMeta(receiver)} ({@code ExpressionGenerator}
     * L774/785), the compiled receiver re-coerced wrapper-level at
     * {@code attributeCall} (L355/385), and the seven non-collapsing list-op
     * arguments + {@code count} likewise (L729/739/829/832/1075). The lone
     * item-level interior expected — {@code caseOnlyElementOperation} L998-1001 —
     * is upstream's collapse leaf itself, and the fork's only-element handler
     * performs that {@code .get()} collapse string-level in
     * {@code CollectionHandler}, so suppressing the entry coercion there is
     * equally byte-correct; bare-item coercion otherwise happens only at genuine
     * value-consumption leaves. The fork threads ONE
     * {@code expectedType} down the whole recursion (so deeper consumers — numeric
     * literal typing, bare-enum binding, ITE result hoisting — can read it), which
     * let {@link #compile}'s blanket terminal coercion fire MID-CHAIN whenever an
     * item-level expected leaked into a chain position whose sub-result carries a
     * type (= meta nav steps, {@code metaNavResultType}): the wrapper collapsed to
     * the non-compiling {@code .get().getValue().<T>map(...)} /
     * {@code .getValue().resultCount()} deref instead of the golden inline
     * {@code map("Type coercion", ...)} step that
     * {@link #coerceNavigationReceiver} emits at the NEXT step once the wrapper
     * type survives. Zero goldens carry the deref form (plain-grep over the frozen
     * corpus-baseline-9.83, all 34,686 goldens), so suppressing it cannot move a
     * green file.
     *
     * <p>The context still carries {@code expectedType} — only the entry-level
     * terminal coercion is skipped; every interior consumer of the threaded
     * expected type behaves identically to {@link #compile}.
     *
     * @param expr         the interior expression to compile
     * @param expectedType the threaded expected type (read by interior consumers,
     *                     never applied as a terminal coercion here)
     * @param scope        the current variable scope
     * @return the compiled statement builder, uncoerced at this level
     */
    public JavaStatementBuilder compileInterior(RExpression expr, JavaType expectedType, JavaStatementScope scope) {
        var context = ExpressionContext.of(expectedType, scope);
        return expr.accept(this, context);
    }

    /**
     * Meta-strip a navigation receiver so a {@code FieldWithMetaX}/{@code ReferenceWithMetaX}
     * value auto-unwraps via {@code .getValue()} before the chain navigates further.
     *
     * <p>Mirrors upstream {@code ExpressionGenerator.attributeCall}'s receiver coercion
     * ({@code addCoercions(receiverCode, MAPPER.wrapExtendsWithoutMeta(receiverCode.expressionType.itemType), scope)}):
     * when the receiver's item type is an {@link RJavaWithMetaValue}, it is coerced to
     * {@code Mapper<? extends valueType>}, which dispatches the (PR-A) dormant
     * {@code WrappedItemCoercer}/{@code ItemToItemCoercer} meta-unwrap and appends the
     * golden {@code .<Value>map("Type coercion", x -> x == null ? null : x.getValue())}.
     * The base {@code Mapper<? extends T>} expected (not {@code MapperS}/{@code MapperC})
     * lets the wrapper-kind cascade collapse to an identity render, so the only byte
     * change is the appended unwrap.
     *
     * <p>No-op (returns the receiver unchanged) when coercion is unavailable, the receiver
     * carries no expression type (M7b-1 handlers), or the receiver item type is not meta —
     * the byte-flat guarantee for every currently-passing navigation. This is the engine
     * PR #9 / PR-B activation lever; {@code NavigationHandler} sets the concrete-meta
     * receiver type that triggers it — for in-chain meta steps via
     * {@code metaNavResultType}, for alias-call receivers via the
     * {@code FunctionAliasHelper} signature walk (facet meta_coercion_numbering
     * arm A1), and for terminal ADD values via
     * {@code FunctionExpressionRenderer.coerceAddValueMetaItem} (arm A4).
     *
     * @param receiver the compiled navigation receiver
     * @param scope    the current variable scope (for lambda-param disambiguation)
     * @return the receiver, meta-unwrapped if its item type is an {@link RJavaWithMetaValue}
     */
    public JavaStatementBuilder coerceNavigationReceiver(JavaStatementBuilder receiver,
                                                         JavaStatementScope scope) {
        return coerceNavigationReceiver(receiver, scope, null);
    }

    /**
     * facet coercionWitnessFollowsCalleeParam (PR #342): the evaluate-arg variant —
     * upstream compiles every argument against the CALLEE parameter's expected item
     * type, so a meta-wrapped arg coerces with the PARAMETER's element as the
     * {@code Type coercion} witness, not the wrapper's own value type (golden
     * Create_StockSplit: {@code .<QuantitySchedule>map("Type coercion", …)} where the
     * wrapper value is {@code NonNegativeQuantitySchedule} — the callee param element
     * is the model supertype; rune already validated the call, so the value conforms).
     * A {@code null}/equal {@code expectedElement} keeps the receiver-strip target
     * byte-identical to the two-arg overload.
     */
    public JavaStatementBuilder coerceNavigationReceiver(JavaStatementBuilder receiver,
                                                         JavaStatementScope scope,
                                                         JavaClass<?> expectedElement) {
        if (coercionService == null || typeUtil == null) {
            return receiver;
        }
        JavaType actualType = receiver.getExpressionType();
        if (actualType == null) {
            return receiver;
        }
        JavaType itemType = typeUtil.getItemType(actualType);
        if (!(itemType instanceof RJavaWithMetaValue meta)) {
            return receiver;
        }
        JavaType targetElement = expectedElement != null ? expectedElement : meta.getValueType();
        JavaType metaStripped = typeUtil.wrapExtends(typeUtil.MAPPER, targetElement);
        return coercionService.coerce(receiver, actualType, metaStripped, scope);
    }

    // =========================================================================
    // Literals — delegated to LiteralHandler
    // =========================================================================

    @Override
    public JavaStatementBuilder visitIntLiteral(RIntLiteral expr, ExpressionContext ctx) {
        return literalHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitNumberLiteral(RNumberLiteral expr, ExpressionContext ctx) {
        return literalHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitStringLiteral(RStringLiteral expr, ExpressionContext ctx) {
        return literalHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitBooleanLiteral(RBooleanLiteral expr, ExpressionContext ctx) {
        return literalHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitListLiteral(RListLiteral expr, ExpressionContext ctx) {
        return literalHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitEmptyLiteral(REmptyLiteral expr, ExpressionContext ctx) {
        return literalHandler.handle(expr, ctx, this);
    }

    // =========================================================================
    // Binary operations (8)    // =========================================================================

    @Override
    public JavaStatementBuilder visitArithmetic(RArithmeticExpr expr, ExpressionContext ctx) {
        return arithmeticHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitLogical(RLogicalExpr expr, ExpressionContext ctx) {
        return logicalHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitEquality(REqualityExpr expr, ExpressionContext ctx) {
        return comparisonHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitComparison(RComparisonExpr expr, ExpressionContext ctx) {
        return comparisonHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitDefault(RDefaultExpr expr, ExpressionContext ctx) {
        return setOperationHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitContains(RContainsExpr expr, ExpressionContext ctx) {
        return setOperationHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitDisjoint(RDisjointExpr expr, ExpressionContext ctx) {
        return setOperationHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitJoin(RJoinExpr expr, ExpressionContext ctx) {
        return setOperationHandler.handle(expr, ctx, this);
    }

    // =========================================================================
    // Navigation (2)    // =========================================================================

    @Override
    public JavaStatementBuilder visitFeatureCall(RFeatureCall expr, ExpressionContext ctx) {
        return navigationHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitDeepFeatureCall(RDeepFeatureCall expr, ExpressionContext ctx) {
        return navigationHandler.handle(expr, ctx, this);
    }

    // =========================================================================
    // References (4)    // =========================================================================

    /**
     * A VALUE-TYPED alias seam's facts at an invocation seat (the § 6.3 alias
     * re-typing program, T1): {@code isMulti} = the render-channel S/C bit —
     * the seam returns a {@code List} form when true ({@code List<? extends T>}
     * for model-typed seams, {@code List<T>} for primitive/enum seams — the
     * Mapper seam string's inner text preserved verbatim), bare {@code T} when
     * false; {@code multiWitness} = the element class for the multi bridge's
     * {@code MapperC.<X>of(...)} witness (null for single seams — the
     * {@code MapperS.of(...)} wrap needs none); {@code witnessRender} = the
     * witness's render text when the seam's element is an import-collision
     * sentinel ({@code ImportCollisionResolver.typeRef} — the seam string spelt
     * it so because the element's simple name collides with another import of
     * the file), null otherwise. A non-null render goes through the bridge's
     * FQN-witness channel ({@code JavaExpression.wrappedInMapperCOfSingle}'s
     * {@code fqnWitnessRender}): the sentinel resolves bare-vs-FQN in the file's
     * render-order resolver exactly like the seam declaration's own element, and
     * the bridge adds no second import of the colliding simple name (PR #607 —
     * cdm 6.20.2–6.20.5's {@code MapCreditEventsReferenceWithReference}).
     */
    public record AliasValueSeam(boolean isMulti, JavaClass<?> multiWitness, String witnessRender) {
    }

    /**
     * THE § 6.3 VALUE-SEAM QUERY (the T1 flip's single leaf authority): non-null
     * exactly when {@code alias} of {@code enclosingFunc} is emitted with a
     * VALUE-TYPED seam in the class currently being rendered — the alias arm of
     * {@link ReferenceHandler} then wraps the invocation in the structural
     * {@code MapperS.of(...)} / {@code MapperC.<X>of(...)} bridge (whose
     * {@code unwrapToBuilder} channel restores the bare value at evaluate-arg /
     * assignment strip seats), instead of emitting the bare Mapper-returning
     * call.
     *
     * <p>THE REFERENCE ROUTE ALWAYS ANSWERS NULL — this default is the byte-inert
     * seam (the PR-7 member-injection precedent): reference emission is
     * unreachable-different by construction, and the rings prove it. The
     * optimised route ({@code OptimisedExpressionCompiler}) overrides with the
     * tranche policy, window-gated so the dependency collector's compiler
     * instance (never windowed) keeps reading reference shapes.
     */
    public AliasValueSeam aliasValueSeamOrNull(RFunction enclosingFunc, RShortcut alias) {
        return null;
    }

    /**
     * THE NON-COUNTING SEAM-CARDINALITY QUERY (T3, PR-24): the S/C bit of a
     * flipped alias's value seam, or {@code null} when the alias keeps its
     * Mapper seam. A pure PREDICATE twin of {@link #aliasValueSeamOrNull} for
     * guard seats that need the seam's cardinality authority WITHOUT counting
     * as a leaf bridge-wrap emission (the {@code ConstructionHandler}
     * bridge-cardinality guard's read — the emission counters must tally
     * emissions only). The reference route's constant null is the same
     * byte-inert seam as the query above.
     */
    public Boolean aliasValueSeamIsMultiOrNull(RFunction enclosingFunc, RShortcut alias) {
        return null;
    }

    @Override
    public JavaStatementBuilder visitSymbolReference(RSymbolReference expr, ExpressionContext ctx) {
        return referenceHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitEnumValueRef(REnumValueRef expr, ExpressionContext ctx) {
        return referenceHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitImplicitVariable(RImplicitVariable expr, ExpressionContext ctx) {
        return referenceHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitSuperCall(RSuperCall expr, ExpressionContext ctx) {
        return referenceHandler.handle(expr, ctx, this);
    }

    // =========================================================================
    // Collections (8)    // =========================================================================

    @Override
    public JavaStatementBuilder visitFilter(RFilterExpr expr, ExpressionContext ctx) {
        return collectionHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitExtract(RExtractExpr expr, ExpressionContext ctx) {
        return collectionHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitSort(RSortExpr expr, ExpressionContext ctx) {
        return collectionHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitReduce(RReduceExpr expr, ExpressionContext ctx) {
        return collectionHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitMax(RMaxExpr expr, ExpressionContext ctx) {
        return collectionHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitMin(RMinExpr expr, ExpressionContext ctx) {
        return collectionHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitListOp(RListOpExpr expr, ExpressionContext ctx) {
        return collectionHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitCount(RCountExpr expr, ExpressionContext ctx) {
        return collectionHandler.handle(expr, ctx, this);
    }

    // =========================================================================
    // Piping (1)
    // =========================================================================

    @Override
    public JavaStatementBuilder visitThen(RThenExpr expr, ExpressionContext ctx) {
        return collectionHandler.handle(expr, ctx, this);
    }

    // =========================================================================
    // Control flow (2)    // =========================================================================

    @Override
    public JavaStatementBuilder visitConditional(RConditionalExpr expr, ExpressionContext ctx) {
        return controlFlowHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitSwitch(RSwitchExpr expr, ExpressionContext ctx) {
        return controlFlowHandler.handle(expr, ctx, this);
    }

    // =========================================================================
    // Existence & cardinality (3)    // =========================================================================

    @Override
    public JavaStatementBuilder visitExistence(RExistenceExpr expr, ExpressionContext ctx) {
        return existenceHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitOnlyExists(ROnlyExistsExpr expr, ExpressionContext ctx) {
        return existenceHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitCardinalityCheck(RCardinalityCheckExpr expr, ExpressionContext ctx) {
        return existenceHandler.handle(expr, ctx, this);
    }

    // =========================================================================
    // Construction & metadata (2)    // =========================================================================

    @Override
    public JavaStatementBuilder visitConstructor(RConstructorExpr expr, ExpressionContext ctx) {
        return constructionHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitWithMeta(RWithMetaExpr expr, ExpressionContext ctx) {
        return constructionHandler.handle(expr, ctx, this);
    }

    // =========================================================================
    // Conversion (2)    // =========================================================================

    @Override
    public JavaStatementBuilder visitConversion(RConversionExpr expr, ExpressionContext ctx) {
        return conversionHandler.handle(expr, ctx, this);
    }

    @Override
    public JavaStatementBuilder visitToString(RToStringExpr expr, ExpressionContext ctx) {
        return conversionHandler.handle(expr, ctx, this);
    }
}
