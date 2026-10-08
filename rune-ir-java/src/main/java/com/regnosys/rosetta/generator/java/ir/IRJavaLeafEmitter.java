package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.generator.java.JavaNamingUtil;
import com.regnosys.rosetta.generator.java.enums.EnumHelper;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper;
import com.regnosys.rosetta.generator.java.expression.handlers.LiteralHandler;
import com.regnosys.rosetta.generator.java.template.ImportCollisionResolver;
import com.regnosys.rosetta.generator.java.template.JavaStringUtil;
import com.regnosys.rosetta.generator.java.expression.handlers.ReferenceHandler;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaLiteral;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaPojoProperty;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.ir.expr.BinaryOp;
import com.regnosys.rosetta.ir.expr.Existence;
import com.regnosys.rosetta.ir.expr.FieldAccess;
import com.regnosys.rosetta.ir.expr.IRApply;
import com.regnosys.rosetta.ir.expr.IREmptyLiteral;
import com.regnosys.rosetta.ir.expr.IRConditional;
import com.regnosys.rosetta.ir.expr.IRConstruct;
import com.regnosys.rosetta.ir.expr.IRLambdaOp;
import com.regnosys.rosetta.ir.expr.IRExpr;
import com.regnosys.rosetta.ir.expr.IRListOp;
import com.regnosys.rosetta.ir.expr.IRMetaAccess;
import com.regnosys.rosetta.ir.expr.IRPointFreeApply;
import com.regnosys.rosetta.ir.expr.IRToString;
import com.regnosys.rosetta.ir.expr.IRLiteral;
import com.regnosys.rosetta.ir.expr.IRReference;
import com.regnosys.rosetta.ir.expr.IRVariable;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RType;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * The Java lowering of neutral {@link IRExpr} leaf nodes — the first slice of the
 * expression IR's {@code ir → java} emitter. It reads the IR node's facts (kind + value;
 * the expected target type from the {@link ExpressionContext}; and, for an enum reference,
 * the node's {@code type()} mapped to a Java import via the {@link JavaTypeTranslator}) and
 * produces the Java {@code Mapper}/expression form. No <em>AST</em> node is consulted — only
 * neutral IR facts — which is what keeps the same IR reusable by future Python/Rust
 * emitters (each maps {@code type()} with its own translator).
 *
 * <p><strong>Coverage.</strong> String, boolean and decimal ({@code number}) literals
 * (context-free), the {@code empty} leaf (bare {@code null}), integer literals up to
 * {@code long} range (both int and {@code BigDecimal}-target contexts), the bare function
 * parameter (scalar {@code MapperS.of(name)} / multi {@code MapperC.<Item>of(name)}), the
 * {@code super} / bare enum-value
 * references ({@code super.doEvaluate()} / {@code EnumName.CONSTANT}), and the
 * <em>structural</em> nodes — a {@code BinaryOp} (an ordered comparison, an equality (static
 * {@code {method}(left, right, CardinalityOperator.{All|Any})}, the {@code Any} default being
 * {@code <>}'s) or a logical {@code and}/{@code or} ({@code left.{andNullSafe|orNullSafe}(right)}))
 * and a unary {@code Existence} check ({@code exists}/{@code notExists}/{@code singleExists}/
 * {@code multipleExists}{@code (arg)}), plus a single-hop {@code FieldAccess} navigation
 * ({@code receiver.<Witness>map("getX", v -> v.getX())}), whose operands recurse through
 * {@link #emit}. {@link #emit}
 * returns {@link Optional#empty()} for everything else — including <em>beyond-long</em> integer
 * literals (whose {@code BigDecimal}-context form hoists), multi-cardinality variables, the
 * {@code FUNCTION} reference kind, and a comparison whose operand declines — so the caller
 * falls back to the legacy handler and the corpus stays byte-green.
 *
 * <p>Byte-parity contract: each form reproduces, factory-call for factory-call and ref
 * for ref, the output of the corresponding {@code LiteralHandler.handle(...)} arm — the
 * same {@code JavaExpression.wrappedInMapperSOf(...)} shape, the same escaping, the same
 * {@code BigDecimal.valueOf}/long-suffix branch, the same {@link HandlerHelper} ref
 * constants — so the emitted bytes (and imports) are identical by construction. The
 * {@code Path2ByteIdentityTest} FUNCTION rows are the oracle.
 *
 * <p>Lab-authored Phase-2 (Wave 0); not present upstream.
 */
public final class IRJavaLeafEmitter {

    /**
     * Count of witness/output simple-name collisions this emitter rendered FQN-inline (the L-029
     * cross-namespace case: a multi witness whose Java simple name collides with the enclosing function
     * output's, but in a different package → the witness renders fully-qualified with its import
     * suppressed). The caller ({@link IRExpressionCompiler}) reads the delta around each top-level
     * {@link #emit} call to count, for the {@code Path2ByteIdentityTest} §4.2 progress instrumentation,
     * how many collision renders the IR path actually produced (vs the legacy fallback). A count of 0
     * over the corpus means no IR-<em>driven</em> emission hit a real cross-namespace collision — i.e. the
     * collision renders present in the goldens were produced by the legacy fallback, not this emitter.
     */
    private int collisionFqnRenders = 0;

    /** The number of FQN-inline witness/output collision renders this emitter has produced so far. */
    int collisionFqnRenderCount() {
        return collisionFqnRenders;
    }

    /**
     * Reset the collision-render counter to a prior snapshot — used by {@link IRExpressionCompiler} to
     * DISCARD the speculative collision renders of a subtree whose top-level emission ultimately declined
     * (so a node that fell back to legacy does not inflate the IR-driven collision count).
     */
    void setCollisionFqnRenderCount(int value) {
        this.collisionFqnRenders = value;
    }

    /**
     * Resolver for the Java-emission facts an {@code ALIAS} operand needs (render inputs + dependency-collision
     * name + numeric witness), set by {@link IRExpressionCompiler} before each top-level emission and {@code null}
     * between emissions. A nested alias reaches the generic {@link #emit} recursion as an arithmetic operand,
     * a call argument, or — since the #491 teach — an equality/existence operand (the remaining contexts defer
     * an alias); a {@code null} resolver makes the alias decline to legacy. Instance state, managed exactly like {@link #collisionFqnRenders} (single-threaded,
     * one value for the whole recursion of a top-level node).
     */
    private AliasOperandResolver aliasResolver;

    /** Install the alias resolver for the next top-level emission (or {@code null} to clear). */
    void setAliasResolver(AliasOperandResolver aliasResolver) {
        this.aliasResolver = aliasResolver;
    }

    /**
     * Resolver for the {@code @Inject} receiver field name a function CALL ({@link IRApply}) needs to render
     * {@code <receiver>.evaluate(...)} — set by {@link IRExpressionCompiler} before each top-level emission and
     * {@code null} between emissions. The neutral {@link IRApply} carries only the callee's simple name; the receiver
     * (its lowerCamelCase + the dependency/shortcut-collision suffix) is a Java-emission decision the resolver
     * supplies. A {@code null} resolver, or a {@code null} return, makes the call decline to legacy. Instance state,
     * managed exactly like {@link #aliasResolver}.
     */
    private CallReceiverResolver callReceiverResolver;

    /** Install the call-receiver resolver for the next top-level emission (or {@code null} to clear). */
    void setCallReceiverResolver(CallReceiverResolver callReceiverResolver) {
        this.callReceiverResolver = callReceiverResolver;
    }

    /**
     * Resolver for the {@code @Inject} {@code <Name>Rule} receiver field name a bare RULE delegation
     * ({@link IRApply} with an {@code IRReference{RULE}} callee) needs to render
     * {@code <receiver>.evaluate(input)} — the twin of {@link #callReceiverResolver} for the rule case. The
     * derivation differs (legacy {@code ReferenceHandler.ruleInvocationReceiver} bridges the rule via
     * {@code RFunction.fromRule} + {@code toFunctionJavaClass}, with no collision suffix), so the compiler
     * supplies a distinct instance. {@code null} resolver / {@code null} return → decline to legacy. Managed
     * exactly like {@link #callReceiverResolver}.
     */
    private CallReceiverResolver ruleReceiverResolver;

    /** Install the rule-receiver resolver for the next top-level emission (or {@code null} to clear). */
    void setRuleReceiverResolver(CallReceiverResolver ruleReceiverResolver) {
        this.ruleReceiverResolver = ruleReceiverResolver;
    }

    /**
     * Renderer for an <em>in-lambda</em> bare RULE delegation ({@link IRApply} with an
     * {@code IRReference{RULE}} callee reached inside an extract/filter/then lambda body) — supplies the
     * complete {@code MapperS.of(<Name>Rule.evaluate(<binding>))} render, where the polymorphic
     * implicit-input {@code <binding>} ({@code item.get()}/{@code thenArg.get()}) is derived by the
     * compiler reusing legacy {@code ReferenceHandler.renderImplicitRuleInvocation} verbatim (L-049).
     * Set by {@link IRExpressionCompiler} before each top-level emission, {@code null} between emissions
     * and {@code null} for a rule-body <em>top-level</em> delegation — there the {@link #ruleReceiverResolver}
     * path renders the {@code input} form instead (the L-045 render, undisturbed). A {@code null} renderer
     * makes {@link #emitRuleDelegation} fall to that top-level path. Managed exactly like
     * {@link #ruleReceiverResolver}.
     */
    private RuleDelegationRenderer ruleDelegationRenderer;

    /** Install the in-lambda rule-delegation renderer for the next top-level emission (or {@code null} to clear). */
    void setRuleDelegationRenderer(RuleDelegationRenderer ruleDelegationRenderer) {
        this.ruleDelegationRenderer = ruleDelegationRenderer;
    }

    /**
     * Resolver for whether the callee parameter at a given argument position is declared MULTI — the one fact the
     * function-CALL arg unwrap needs to choose {@code .getMulti()} (a {@code Mapper}-chain arg into a {@code List}
     * parameter) over the scalar {@code .get()} (legacy facet {@code tailMulti}). Set by {@link IRExpressionCompiler}
     * before each top-level emission (reusing legacy {@code ReferenceHandler.evaluateParamIsMulti} as the oracle) and
     * {@code null} between emissions. A {@code null} resolver makes every arg unwrap with the scalar accessor (the
     * historical single-parameter behaviour), so single-parameter calls are unaffected. Managed exactly like
     * {@link #callReceiverResolver}.
     */
    private CallParamMultiResolver callParamMultiResolver;

    /** Install the call-param-cardinality resolver for the next top-level emission (or {@code null} to clear). */
    void setCallParamMultiResolver(CallParamMultiResolver callParamMultiResolver) {
        this.callParamMultiResolver = callParamMultiResolver;
    }

    /**
     * Renderer for a flat postfix list/collection op ({@link IRListOp}: distinct/flatten/first/last/reverse/count,
     * + only-element since the #498 teach) —
     * supplies the complete {@code distinct(<arg>)} / {@code <arg>.first()} / {@code <arg>.resultCount()} /
     * {@code <arg>.get()} render by
     * reusing legacy {@code CollectionHandler.handle} verbatim (L-050; the method/link/import are Java-emission
     * decisions kept off the neutral {@link IRListOp}). Set by {@link IRExpressionCompiler} before each top-level
     * emission, {@code null} between emissions and {@code null} for a non-list-op expression. A {@code null} renderer
     * makes the op decline to legacy. Managed exactly like {@link #ruleDelegationRenderer}.
     */
    private CollectionOpRenderer collectionOpRenderer;

    /** Install the collection-op renderer for the next top-level emission (or {@code null} to clear). */
    void setCollectionOpRenderer(CollectionOpRenderer collectionOpRenderer) {
        this.collectionOpRenderer = collectionOpRenderer;
    }

    /**
     * The {@code to-string} render delegate for the current top-level emission — reusing legacy
     * {@code ConversionHandler.handle(RToStringExpr)} verbatim (L-070; the {@code "to-string"} lexeme, the source-enum
     * {@code ::toDisplayString} selection and the meta-unwrap are Java-emission decisions kept off the neutral
     * {@link IRToString}). Set by {@link IRExpressionCompiler} before each top-level emission, {@code null} between
     * emissions and {@code null} for a non-to-string expression. A {@code null} renderer makes the conversion decline
     * to legacy. Managed exactly like {@link #collectionOpRenderer}.
     */
    private ToStringRenderer toStringRenderer;

    /** Install the to-string renderer for the next top-level emission (or {@code null} to clear). */
    void setToStringRenderer(ToStringRenderer toStringRenderer) {
        this.toStringRenderer = toStringRenderer;
    }

    /**
     * Renderer for a lambda-bound implicit {@code item} ({@link IRVariable.VariableKind#USER_ITEM}) — the
     * #469 itemCallArg teach: the compiler supplies the LIVE scope binding (a named extract parameter, a
     * collision-escaped {@code _item}, or the plain {@code item}) by reusing legacy
     * {@code ReferenceHandler.handle(RImplicitVariable)} verbatim on the source-range-correlated AST node.
     * A {@code null} renderer keeps the historical bare {@code variable.name()} render (standalone-emitter
     * contexts); an installed renderer returning {@code null} declines the claim to legacy. Managed exactly
     * like {@link #toStringRenderer}.
     */
    private ImplicitItemRenderer implicitItemRenderer;

    /** Install the implicit-item renderer for the next top-level emission (or {@code null} to clear). */
    void setImplicitItemRenderer(ImplicitItemRenderer implicitItemRenderer) {
        this.implicitItemRenderer = implicitItemRenderer;
    }

    /**
     * Renderer for the guarded item-rooted navigation families (the #469 multiHopItemNav + metaItemReceiver
     * teach): the compiler serves the source-range-indexed chained / meta-receiver item navs by reusing the
     * exact legacy fallback render ({@code super.visitFeatureCall} — deferred-sentinel lambda naming and the
     * Type-coercion deref are the scope machinery's own). A {@code null} renderer or a {@code null} return
     * (an un-indexed node) keeps this emitter's own hop-by-hop {@code FieldAccess} render. Managed exactly
     * like {@link #implicitItemRenderer}.
     */
    private ItemNavRenderer itemNavRenderer;

    /** Install the item-nav renderer for the next top-level emission (or {@code null} to clear). */
    void setItemNavRenderer(ItemNavRenderer itemNavRenderer) {
        this.itemNavRenderer = itemNavRenderer;
    }

    /**
     * Renderer for a SYNTHETIC elided-operand implicit item
     * ({@link IRVariable.VariableKind#SYNTHETIC_ITEM}) — the #479 filter/extract synthetic-item
     * receiver teach: the compiler supplies the LIVE binding legacy resolves (the lambda's own
     * {@code item}/escaped/named binding) by reusing legacy
     * {@code ReferenceHandler.handle(RImplicitVariable)} verbatim on the source-range-correlated
     * synthetic AST node — the same oracle-reuse closure as {@link #implicitItemRenderer}, indexed
     * over the SYNTHETIC sub-family with range-collision poisoning (a synthesized node can share a
     * range with its synthesizing construct, so an ambiguous range declines rather than guesses).
     * Unlike the USER-item renderer there is NO bare fallback: a synthetic item's render is
     * context-dependent by definition, so a {@code null} renderer OR a {@code null} return declines
     * the claim to legacy — never guess a binding. Managed exactly like {@link #itemNavRenderer}.
     */
    private ImplicitItemRenderer syntheticItemRenderer;

    /** Install the synthetic-item renderer for the next top-level emission (or {@code null} to clear). */
    void setSyntheticItemRenderer(ImplicitItemRenderer syntheticItemRenderer) {
        this.syntheticItemRenderer = syntheticItemRenderer;
    }

    /**
     * Renderer for a POINT-FREE function application ({@link com.regnosys.rosetta.ir.expr.IRPointFreeApply})
     * — the #492 L-109 teach: the compiler serves the source-range-correlated raw bare-function
     * reference by reusing legacy {@code ReferenceHandler.renderImplicitFunctionInvocation}
     * verbatim (the SAME public oracle the top-level point-free claim reuses — the implicit-argument
     * derivation, the nav-receiver {@code MapperS.of} wrap and the zero-real-input {@code .evaluate()}
     * form are all the oracle's own, byte-identical by the strongest argument). A {@code null}
     * renderer or a {@code null} return (an uncorrelated node) declines the claim to legacy — never
     * guess an implicit binding. Managed exactly like {@link #itemNavRenderer}.
     */
    private PointFreeRenderer pointFreeRenderer;

    /** Install the point-free renderer for the next top-level emission (or {@code null} to clear). */
    void setPointFreeRenderer(PointFreeRenderer pointFreeRenderer) {
        this.pointFreeRenderer = pointFreeRenderer;
    }

    /**
     * Renderer for a type construction ({@link com.regnosys.rosetta.ir.expr.IRConstruct}) — the
     * #494 teach: the compiler serves the source-range-correlated raw constructor site by calling
     * {@code super.visitConstructor(site, ctx)} — the LITERAL legacy fallback (one line:
     * {@code constructionHandler.handle(expr, ctx, this)}), so the typed-builder-block facet family
     * and the placeholder decline path are all the oracle's own, byte-identical by the strongest
     * argument. A {@code null} renderer or a {@code null} return (an uncorrelated node) declines
     * the claim to legacy — never render a site the raw walk cannot prove. Managed exactly like
     * {@link #pointFreeRenderer}.
     */
    private ConstructRenderer constructRenderer;

    /** Install the construct renderer for the next top-level emission (or {@code null} to clear). */
    void setConstructRenderer(ConstructRenderer constructRenderer) {
        this.constructRenderer = constructRenderer;
    }

    /**
     * Renderer for a conditional ({@link com.regnosys.rosetta.ir.expr.IRConditional}) — the #495
     * teach: the compiler serves the source-range-correlated raw conditional site by calling
     * {@code super.visitConditional(site, ctx)} — the LITERAL legacy fallback (routing to
     * {@code controlFlowHandler.handle(expr, ctx, this)}), so the ANF/hoist statement family and
     * every branch compile are the oracle's own, byte-identical by the strongest argument. A
     * {@code null} renderer or a {@code null} return (an uncorrelated node) declines the claim to
     * legacy — never render a site the raw walk cannot prove. Managed exactly like
     * {@link #constructRenderer}.
     */
    private ConditionalRenderer conditionalRenderer;

    /** Install the conditional renderer for the next top-level emission (or {@code null} to clear). */
    void setConditionalRenderer(ConditionalRenderer conditionalRenderer) {
        this.conditionalRenderer = conditionalRenderer;
    }

    /**
     * Renderer for a lambda-bodied collection operation
     * ({@link com.regnosys.rosetta.ir.expr.IRLambdaOp}) — the #496 teach: the compiler serves the
     * source-range-correlated raw extract/filter site by calling
     * {@code super.visitExtract(site, ctx)} / {@code super.visitFilter(site, ctx)} — the LITERAL
     * legacy fallbacks (routing to {@code collectionHandler.handle(expr, ctx, this)}), so the
     * lambda-form facet family and every receiver/body compile are the oracle's own,
     * byte-identical by the strongest argument. A {@code null} renderer or a {@code null} return
     * (an uncorrelated node) declines the claim to legacy — never render a site the raw walk
     * cannot prove. Managed exactly like {@link #conditionalRenderer}.
     */
    private LambdaOpRenderer lambdaOpRenderer;

    /**
     * The #498 args-present rule-invocation renderer — the whole-reference render supplied by the
     * compiler on the root-site fast path (the literal {@code referenceHandler.handle} fallback
     * call — see {@link RuleApplyRenderer}), byte-identical by the strongest argument. A
     * {@code null} renderer or a {@code null} return (an uncorrelated node) declines the claim to
     * legacy — never render a site the correlation cannot prove. Managed exactly like
     * {@link #conditionalRenderer}.
     */
    private RuleApplyRenderer ruleApplyRenderer;

    /** Install the rule-apply renderer for the next top-level emission (or {@code null} to clear). */
    void setRuleApplyRenderer(RuleApplyRenderer ruleApplyRenderer) {
        this.ruleApplyRenderer = ruleApplyRenderer;
    }

    private MetaNavRenderer metaNavRenderer;

    /** Install the meta-nav renderer for the next top-level emission (or {@code null} to clear). */
    void setMetaNavRenderer(MetaNavRenderer metaNavRenderer) {
        this.metaNavRenderer = metaNavRenderer;
    }

    /**
     * The #500 generic root-site oracle renderer — the ONE slot serving the arm-B family roots
     * (conversion / pipe / only-exists / root list-literal) and the arm-A meta-bearing roots
     * (nav / binary / existence / non-rule apply with an interior meta hop), each rendered by
     * the literal {@code super.visitX} legacy line (see {@link OracleRootRenderer}). Consulted
     * ONCE at the emit entry: a correlated serve renders the WHOLE claim; a null slot or an
     * uncorrelated node falls through to the kind dispatch (where the #500 shapes decline to
     * legacy at the chain's end — never a native compose). Managed exactly like
     * {@link #conditionalRenderer}.
     */
    private OracleRootRenderer oracleRootRenderer;

    /** Install the oracle-root renderer for the next top-level emission (or {@code null} to clear). */
    void setOracleRootRenderer(OracleRootRenderer oracleRootRenderer) {
        this.oracleRootRenderer = oracleRootRenderer;
    }

    /** Install the lambda-op renderer for the next top-level emission (or {@code null} to clear). */
    void setLambdaOpRenderer(LambdaOpRenderer lambdaOpRenderer) {
        this.lambdaOpRenderer = lambdaOpRenderer;
    }

    /**
     * The enclosing function's INPUT-name escape for the current top-level emission —
     * facet aliasCallInputEscape (v3.1 flip seat 32, law E.2): a bare {@code PARAM} read renders
     * the EMITTED name, which is {@code "_"}-escaped when the raw input name is already taken on the
     * class scope — golden {@code MapperS.of(_partyLei)} against our {@code MapperS.of(partyLei)}
     * (drr 7.0.0-7.3.0 {@code CounterpartyRoleFromLEI}, whose {@code @Inject protected PartyLei
     * partyLei} takes the raw name; legacy already serves this half at {@code ReferenceHandler}'s
     * variable path). This emitter holds no
     * {@link com.regnosys.rosetta.ast.functions.RFunction}, so the escape cannot be computed here:
     * {@link IRExpressionCompiler} — which has the enclosing function — installs the table it
     * built from {@code FunctionGenerator.escapedFunctionInputName}, the one table legacy's two
     * seats read (LAW 69: one escape table, three consumers).
     *
     * <p>{@code null} for every function with no escaping input (the overwhelming majority) and the
     * identity outside the escape population, so a null hook and an installed one render the same
     * bytes everywhere the law does not apply. Managed exactly like {@link #oracleRootRenderer}.
     */
    private UnaryOperator<String> inputEscape;

    /** Install the function-input escape for the next top-level emission (or {@code null} to clear). */
    void setInputEscape(UnaryOperator<String> inputEscape) {
        this.inputEscape = inputEscape;
    }

    /**
     * The complete per-emission resolver/renderer frame as ONE value — the unit {@link #swapResolvers}
     * installs and restores. Introduced with the #469 re-entrancy fix; the component names mirror the
     * individual setters ({@code toStringRenderer} keeps its full name — a {@code toString} component
     * would clash with {@link Object#toString()}).
     */
    record Resolvers(AliasOperandResolver alias,
                     CallReceiverResolver callReceiver,
                     CallReceiverResolver ruleReceiver,
                     RuleDelegationRenderer ruleDelegation,
                     CallParamMultiResolver callParamMulti,
                     CollectionOpRenderer collectionOp,
                     ToStringRenderer toStringRenderer,
                     ImplicitItemRenderer implicitItem,
                     ItemNavRenderer itemNav,
                     ImplicitItemRenderer syntheticItem,
                     PointFreeRenderer pointFree,
                     ConstructRenderer construct,
                     ConditionalRenderer conditional,
                     LambdaOpRenderer lambdaOp,
                     RuleApplyRenderer ruleApply,
                     MetaNavRenderer metaNav,
                     OracleRootRenderer oracleRoot,
                     UnaryOperator<String> inputEscape) {}

    /**
     * Install a complete resolver frame and return the PREVIOUS one — the #469 re-entrancy fix. An oracle
     * render can recurse back through the compiler (a NORMAL recursion — the {@link CollectionOpRenderer}
     * contract), and the nested claim's own install/teardown must RESTORE the outer emission's frame
     * rather than null it: a nulled {@link ImplicitItemRenderer} would silently downgrade a later
     * {@code USER_ITEM} render in the outer emission to the bare form, and a nulled resolver would turn
     * later calls into spurious declines. LATENT today, not live (the #469 Seat-1 OBS-2 precision): every
     * shipped oracle seat returns a WHOLE-claim render, so no post-teardown consult window existed in
     * shipped code — the swap hardens the implicit invariant before any future mid-emit consult gains a
     * recursion. The compiler wraps every {@code emit} in {@code swap(next) … swap(previous)}.
     */
    Resolvers swapResolvers(Resolvers next) {
        Resolvers previous = new Resolvers(aliasResolver, callReceiverResolver, ruleReceiverResolver,
                ruleDelegationRenderer, callParamMultiResolver, collectionOpRenderer, toStringRenderer,
                implicitItemRenderer, itemNavRenderer, syntheticItemRenderer, pointFreeRenderer,
                constructRenderer, conditionalRenderer, lambdaOpRenderer, ruleApplyRenderer,
                metaNavRenderer, oracleRootRenderer, inputEscape);
        this.aliasResolver = next.alias();
        this.callReceiverResolver = next.callReceiver();
        this.ruleReceiverResolver = next.ruleReceiver();
        this.ruleDelegationRenderer = next.ruleDelegation();
        this.callParamMultiResolver = next.callParamMulti();
        this.collectionOpRenderer = next.collectionOp();
        this.toStringRenderer = next.toStringRenderer();
        this.implicitItemRenderer = next.implicitItem();
        this.itemNavRenderer = next.itemNav();
        this.syntheticItemRenderer = next.syntheticItem();
        this.pointFreeRenderer = next.pointFree();
        this.constructRenderer = next.construct();
        this.conditionalRenderer = next.conditional();
        this.lambdaOpRenderer = next.lambdaOp();
        this.ruleApplyRenderer = next.ruleApply();
        this.metaNavRenderer = next.metaNav();
        this.oracleRootRenderer = next.oracleRoot();
        this.inputEscape = next.inputEscape();
        return previous;
    }

    /**
     * Lower a leaf IR node to its Java {@code Mapper} expression, or {@link Optional#empty()}
     * if this emitter does not (yet) handle the node — signalling the caller to fall back.
     *
     * @param node       the neutral IR node to lower
     * @param ctx        the expression context (its {@code expectedType} drives the integer
     *                   {@code BigDecimal} branch; unused by the context-free leaves)
     * @param typeUtil   interprets {@code ctx.expectedType()} (is it BigDecimal?); may be null
     * @param translator maps an IR node's {@code type()} to a Java type — used only for the enum
     *                   import of an {@code ENUM_VALUE} reference; may be null (then enum refs decline)
     */
    public Optional<JavaStatementBuilder> emit(IRExpr node, ExpressionContext ctx, JavaTypeUtil typeUtil,
                                               JavaTypeTranslator translator) {
        return emit(node, ctx, typeUtil, translator, null);
    }

    /**
     * Overload carrying the enclosing function's OUTPUT Java type — the one extra fact a witnessed
     * leaf ({@code MapperC.<Item>of} / {@code .<Witness>map}) needs to reproduce legacy's
     * <em>witness/output simple-name collision</em> render: when the witness's Java simple name collides
     * with the output's but they are different types, the witness renders FULLY-QUALIFIED inline and its
     * import is suppressed (mirroring {@code ReferenceHandler.mapperCWitnessOutputCollisionFqn} /
     * {@code NavigationHandler.witnessOutputCollisionFqn}). This is a <em>Java-emission</em> decision —
     * it lives here, not in the language-neutral adapter (L-029), and is computed by the caller
     * ({@link IRExpressionCompiler}) which has the AST node + translator. {@code outputType} is
     * {@code null} when there is no enclosing function output, the output is meta-annotated, or the
     * caller does not supply it — in which case every witness renders bare (no collision).
     *
     * @param outputType the enclosing function output's Java type for the collision check, or {@code null}
     */
    public Optional<JavaStatementBuilder> emit(IRExpr node, ExpressionContext ctx, JavaTypeUtil typeUtil,
                                               JavaTypeTranslator translator, JavaClass<?> outputType) {
        // #500: the generic oracle-root consult — the ONE entry-point serve for every claim root
        // whose render is the literal legacy fallback line (the arm-B family roots + the arm-A
        // meta-bearing roots). The slot installs only at served roots (the compiler's frame kind
        // gate) and range-correlates inside the render, so an interior node under any other root
        // falls through to the kind dispatch below (where the #500 shapes decline to legacy at
        // the chain's end — never a native compose).
        if (oracleRootRenderer != null) {
            JavaStatementBuilder oracleRendered = oracleRootRenderer.render(node);
            if (oracleRendered != null) {
                return Optional.of(oracleRendered);
            }
        }
        if (node instanceof IRLiteral literal) {
            return switch (literal.literalKind()) {
                case STRING -> Optional.of(emitString((String) literal.value()));
                case BOOLEAN -> Optional.of(emitBoolean((Boolean) literal.value()));
                case NUMBER -> Optional.of(emitNumber((BigDecimal) literal.value()));
                case INT -> emitInt((BigInteger) literal.value(), ctx, typeUtil);
            };
        }
        if (node instanceof IREmptyLiteral) {
            // Bare null — mirrors LiteralHandler.handle(REmptyLiteral).
            return Optional.of(JavaLiteral.NULL);
        }
        if (node instanceof IRVariable variable) {
            return emitVariable(variable, translator, outputType);
        }
        if (node instanceof IRReference reference) {
            return emitReference(reference, translator);
        }
        if (node instanceof BinaryOp binary) {
            return emitBinaryOp(binary, ctx, typeUtil, translator, outputType);
        }
        if (node instanceof Existence existence) {
            return emitExistence(existence, ctx, typeUtil, translator, outputType);
        }
        if (node instanceof FieldAccess fieldAccess) {
            return emitFieldAccess(fieldAccess, ctx, typeUtil, translator, outputType);
        }
        if (node instanceof IRMetaAccess metaAccess) {
            return emitMetaAccess(metaAccess);
        }
        if (node instanceof IRApply apply) {
            return emitApply(apply, ctx, typeUtil, translator, outputType);
        }
        if (node instanceof IRListOp listOp) {
            return emitListOp(listOp, ctx, typeUtil, translator, outputType);
        }
        if (node instanceof IRToString toString) {
            return emitToString(toString);
        }
        if (node instanceof IRPointFreeApply pointFree) {
            return emitPointFree(pointFree);
        }
        if (node instanceof IRConstruct construct) {
            return emitConstruct(construct);
        }
        if (node instanceof IRConditional conditional) {
            return emitConditional(conditional);
        }
        if (node instanceof IRLambdaOp lambdaOp) {
            return emitLambdaOp(lambdaOp);
        }
        return Optional.empty();
    }

    /**
     * Renders a POINT-FREE function application ({@link IRPointFreeApply}) by delegating to the
     * compiler-installed {@link #pointFreeRenderer}, which reuses legacy
     * {@code ReferenceHandler.renderImplicitFunctionInvocation} verbatim on the
     * source-range-correlated raw node (#492 — the SAME public D43-seam oracle the top-level
     * point-free claim reuses): the implicit-argument derivation (rule input / lambda item /
     * condition instance), the nav-receiver {@code MapperS.of} wrap (the oracle reads the RAW
     * node's own parent) and the zero-real-input {@code .evaluate()} form are all the oracle's
     * own — byte-identical to the decline path by the strongest argument. A {@code null} renderer
     * or an uncorrelated node declines to legacy (never guess an implicit binding).
     */
    private Optional<JavaStatementBuilder> emitPointFree(IRPointFreeApply pointFree) {
        if (pointFreeRenderer != null) {
            JavaStatementBuilder rendered = pointFreeRenderer.render(pointFree);
            if (rendered != null) {
                return Optional.of(rendered);
            }
        }
        return Optional.empty();
    }

    /**
     * Renders a type construction ({@link IRConstruct}) by delegating to the compiler-installed
     * range-correlated {@link #constructRenderer} — the literal {@code super.visitConstructor}
     * fallback call, byte-identical by the strongest argument (see {@link ConstructRenderer}). The
     * {@code type().isMissing()} gate is the #491 typeMissing convention: a node the fixed point
     * never typed carries no honest constructed-type fact, so it declines to legacy rather than
     * claim (the #494 probe read 2 of 3,501 — both the {@code otherType.typeMissing} face). A
     * {@code null} renderer (no constructor site indexed in the claimed subtree) or a {@code null}
     * return (uncorrelated / range-poisoned) declines identically.
     */
    private Optional<JavaStatementBuilder> emitConstruct(IRConstruct construct) {
        var type = construct.type();
        if (type == null || type.isMissing()) {
            return Optional.empty();
        }
        if (constructRenderer != null) {
            JavaStatementBuilder rendered = constructRenderer.render(construct);
            if (rendered != null) {
                return Optional.of(rendered);
            }
        }
        return Optional.empty();
    }

    /**
     * Renders a conditional ({@link IRConditional}) by delegating to the compiler-installed
     * range-correlated {@link #conditionalRenderer} — the literal {@code super.visitConditional}
     * fallback call, byte-identical by the strongest argument (see {@link ConditionalRenderer}).
     * The {@code type().isMissing()} gate is the #491 typeMissing convention: a node the fixed
     * point never typed carries no honest joined-branch-type fact, so it declines to legacy
     * rather than claim (the #495 probe read exactly 1 of 1,133, already inside a blocked face —
     * the gate expects zero live declines). A {@code null} renderer (a non-conditional claim
     * root — the frame-slot kind gate) or a {@code null} return (uncorrelated: a CHILD-position
     * conditional inside another claim's lowered tree) declines identically — the pre-#495
     * bytes.
     */
    private Optional<JavaStatementBuilder> emitConditional(IRConditional conditional) {
        var type = conditional.type();
        if (type == null || type.isMissing()) {
            return Optional.empty();
        }
        if (conditionalRenderer != null) {
            JavaStatementBuilder rendered = conditionalRenderer.render(conditional);
            if (rendered != null) {
                return Optional.of(rendered);
            }
        }
        return Optional.empty();
    }

    /**
     * Renders a lambda-bodied collection operation ({@link IRLambdaOp}) by delegating to the
     * compiler-installed range-correlated {@link #lambdaOpRenderer} — the literal
     * {@code super.visitExtract}/{@code super.visitFilter} fallback call, byte-identical by the
     * strongest argument (see {@link LambdaOpRenderer}). The {@code type().isMissing()} gate is
     * the #491 typeMissing convention: a node the fixed point never typed carries no honest
     * element-type fact, so it declines to legacy rather than claim (the #496 probe read ZERO
     * typeMissing events on both families — the gate is the robustness belt). A {@code null}
     * renderer (a non-lambda claim root — the frame-slot kind gate) or a {@code null} return
     * (uncorrelated: a CHILD-position lambda inside another claim's lowered tree) declines
     * identically — the pre-#496 bytes.
     */
    private Optional<JavaStatementBuilder> emitLambdaOp(IRLambdaOp lambdaOp) {
        var type = lambdaOp.type();
        if (type == null || type.isMissing()) {
            return Optional.empty();
        }
        if (lambdaOpRenderer != null) {
            JavaStatementBuilder rendered = lambdaOpRenderer.render(lambdaOp);
            if (rendered != null) {
                return Optional.of(rendered);
            }
        }
        return Optional.empty();
    }

    /**
     * Renders a {@code to-string} conversion ({@link IRToString}) by delegating to the compiler-installed
     * {@link #toStringRenderer}, which reuses {@code ConversionHandler.handle(RToStringExpr)} verbatim (L-070) —
     * {@code <arg>.map("to-string", Object::toString)} / {@code <SourceEnum>::toDisplayString}, byte-identical to
     * Path-1 by construction (the source-enum detection, the meta-unwrap and the import are the oracle's, kept off
     * the neutral node — the L-029/L-050 split). A TOP-LEVEL driven {@link IRToString} always has the renderer
     * installed; since the #503 arm-A2 the kind ALSO admits as an equality operand ({@code isEqualityOperand} +
     * {@code containsOracleLeaf}), where the containing equality root oracle-serves the WHOLE claim and this leaf
     * arm is never consulted — so a {@code null} renderer here (a nested to-string reaching the leaf arm outside
     * that oracle-served route) still declines to legacy: the native-compose choke point.
     */
    private Optional<JavaStatementBuilder> emitToString(IRToString toString) {
        if (toStringRenderer != null) {
            return toStringRenderer.render(); // the verbatim ConversionHandler oracle (L-070)
        }
        return Optional.empty(); // nested outside the oracle-served equality route → declines to legacy
    }

    /**
     * Renders a flat postfix list/collection op ({@link IRListOp}: distinct/flatten/first/last/reverse/count,
     * + only-element since the #498 teach) —
     * {@code distinct(<arg>)} / {@code <arg>.first()} / {@code <arg>.resultCount()} / {@code <arg>.get()} etc.
     *
     * <p><strong>Top-level (a list-op IS the emitted expression).</strong> The flat wrap (the op→method mapping, the
     * chain link, the {@code distinct} static-wildcard import) is a Java-emission decision the emitter cannot reproduce
     * without re-deriving the legacy {@code CollectionHandler} facts (the L-029 trap), so it is supplied COMPLETE by the
     * compiler's {@link #collectionOpRenderer}, which reuses {@code CollectionHandler.handle} verbatim (L-050) —
     * byte-identical to Path-1 by construction. The {@link IRListOp} node's {@code op}/{@code child} state the neutral
     * structure for a future Python/Rust emitter; this Java emitter delegates the render.
     *
     * <p><strong>Nested {@code count} operand (no renderer).</strong> The collection-op renderer is installed only for a
     * TOP-LEVEL list-op expression, so a {@code count} reached as a comparison / equality OPERAND ({@code xs count = 1},
     * L-051) has none. Only {@code count} is admitted as an operand (the adapter's {@code isCountOperand} gate), and its
     * render is a flat, witness-free, import-free member call — so the emitter composes it GENUINELY from the node:
     * {@code <emit(child)>.resultCount()}, with the child's refs/imports carried through (modelled on
     * {@link #emitFieldAccess}, and byte-identical to legacy {@code CollectionHandler.handle(RCountExpr)}, which renders
     * the bare {@code <arg>.resultCount()} with a {@code null} type — the {@code MapperS.of} wrap is then applied at the
     * comparison by {@link #wrapCountValueOperand}). The child is the lowered receiver subtree, so {@code emit(child)}
     * reproduces {@code compileInterior(argument)} exactly (both render the same lowered, non-meta subtree).
     *
     * <p><strong>Nested {@code only-element}/{@code first} receiver (#499).</strong> The
     * {@code isCollapseNavBase} branch below composes the census-proven collapse class genuinely
     * from the node — see the branch comment for the render identities and its TWO consumers
     * (the {@code emitFieldAccess} kind-keyed re-wrap and the {@code emitNestedCount} recursion).
     *
     * <p>A {@code null} renderer for any other nested list-op declines to legacy (no consumer
     * gate admits one).
     */
    private Optional<JavaStatementBuilder> emitListOp(IRListOp listOp, ExpressionContext ctx, JavaTypeUtil typeUtil,
                                                      JavaTypeTranslator translator, JavaClass<?> outputType) {
        if (collectionOpRenderer != null) {
            return collectionOpRenderer.render(); // top-level list-op — the verbatim CollectionHandler oracle (L-050)
        }
        if (listOp.op() == IRListOp.Kind.COUNT) {
            return emitNestedCount(listOp, ctx, typeUtil, translator, outputType); // a nested count operand (L-051)
        }
        if (isCollapseNavBase(listOp)) {
            // #499: a nested ONLY_ELEMENT / FIRST collapse over an isCollapseNavBase child —
            // the navigation-RECEIVER position the adapter's isNavigableReceiver admission
            // serves (the #498-exposed receiver:IRListOp frontier; the emitNestedCount
            // composition model). Byte-identical to legacy CollectionHandler's own two forms:
            // ONLY_ELEMENT renders the flat, import-free, witness-free bare-item collapse
            // `<child>.get()` (link "." — the inline golden form; the selfUnwrapping
            // consumer-marker legacy stamps on it serves the value-consuming STRIP sites only,
            // and BOTH consumers of this nested render take marker-free decisions — the Seat-1
            // #499 MF-5 consumer census: emitFieldAccess's re-wrap is keyed on the IR node's
            // own kind exactly as legacy's nav_after_get_rewrap keys on the AST receiver
            // shape, and emitNestedCount appends `.resultCount()` to the rendered text exactly
            // as legacy handle(RCountExpr) appends to the same compileInterior text with no
            // marker consultation — so the marker is structurally unnecessary on this route);
            // FIRST renders the Mapper-valued member call `<child>\n\t.first()` (legacy
            // CHAIN_LINK — upstream's buildListOperationNoBody wrap; the #399G
            // inlineCollapseMetaElementStamp cannot fire on FIRST's admitted population —
            // param/nav children only, where the adapter's meta gates close BOTH stamp
            // channels: the compiled argument's element and the AST terminal-meta walk). The
            // child gate (isCollapseNavBase — the single native-vs-oracle boundary since the
            // #511 kind-wide receiver admission dissolved the adapter's copy) keeps every
            // child-shape-conditional legacy facet structurally unreachable (#334 requires a
            // BARE multi-fn RSymbolReference argument — the admitted call child is
            // args-present; the item round-trips need item children; the #361 hoist is
            // LAST-only), so emit(child) reproduces legacy compileInterior(argument) exactly
            // (the L-051 equivalence; the shared-factory wrap identity covers the call/alias
            // children — see the strip below). Declines (→ legacy) if the child subtree cannot
            // emit. Every other kind/child shape stays deferred below — the adapter admits no
            // other nested collapse, and a drift arrival declines rather than rendering an
            // unproven form.
            Optional<JavaStatementBuilder> child = emit(listOp.child(), ctx, typeUtil, translator, outputType);
            if (child.isEmpty()) {
                return Optional.empty();
            }
            JavaStatementBuilder childBuilder = child.get();
            // The tobuilder_output_assignment mechanism-3 strip, verbatim (legacy
            // CollectionHandler.handle(RListOpExpr) — the same class detection, the same
            // witnesslessForm() call): an only-element collapse consumes its multi argument in
            // upstream's WITNESSLESS List-to-item unwrap shape (`MapperC.of(name).get()` — the
            // consumption site selects the wrap from the expected type; only STAY-multi
            // consumers keep the `MapperC.<Item>of` witness, so a FIRST child keeps it here
            // too). The emitter's multi-param render returns the SAME MapperCOfSingleWrap the
            // legacy bare-variable path builds, so this strip is byte-identical BY THE SAME
            // LINE — the witnessless form's refs drop the witness class exactly as legacy's
            // import set does (the first #499 ring's one divergence class:
            // `MapperC.<Reset>of(resets)` where golden reads `MapperC.of(resets)`).
            if (listOp.op() == IRListOp.Kind.ONLY_ELEMENT
                    && childBuilder instanceof JavaExpression.MapperCOfSingleWrap witnessedWrap) {
                childBuilder = witnessedWrap.witnesslessForm();
            }
            String link = listOp.op() == IRListOp.Kind.ONLY_ELEMENT ? "." : "\n\t.";
            String method = listOp.op() == IRListOp.Kind.ONLY_ELEMENT ? "get" : "first";
            return Optional.of(JavaExpression.from(
                    HandlerHelper.render(childBuilder) + link + method + "()", null,
                    childBuilder.getRefs(), childBuilder.getStaticWildcardImports()));
        }
        return Optional.empty(); // a nested non-count list-op outside the taught class — defer to legacy
    }

    /**
     * The #499 collapse-receiver NATIVE-render boundary, kind × child census-narrow: a
     * {@code FIRST} collapse serves a PARAM variable or {@link FieldAccess} child (the L-051
     * byte-equivalence class); an {@code ONLY_ELEMENT} collapse additionally serves an
     * args-present FUNCTION call child (the {@code emitApply} render — single-output
     * {@code MapperS.of(…)} / multi-output the SAME {@code MapperCOfSingleWrap} legacy builds,
     * so the mechanism-3 strip above fires identically) and an alias child (the ring-proven
     * {@code emitAlias} bare-Mapper render via the frame's alias resolver). Until #511 this was
     * the emitter twin of the adapter's like-named admission gate; the #511 kind-wide receiver
     * admission dissolved the adapter copy, so THIS predicate is now the single boundary —
     * package-private for the compiler's nav-over-list-op SHAPE leg
     * ({@code IRExpressionCompiler.containsOracleLeaf}), which routes every receiver OUTSIDE
     * this class to the oracle-root serve off the SAME predicate (twin-exact BY IDENTITY: an
     * in-class receiver always renders natively here, an excluded one always oracle-serves —
     * never a wrong render, and no spurious-decline drift surface remains).
     */
    static boolean isCollapseNavBase(IRListOp listOp) {
        boolean paramOrNav = listOp.child() instanceof IRVariable v
                && v.variableKind() == IRVariable.VariableKind.PARAM
                || listOp.child() instanceof FieldAccess;
        if (listOp.op() == IRListOp.Kind.FIRST) {
            return paramOrNav;
        }
        if (listOp.op() != IRListOp.Kind.ONLY_ELEMENT) {
            return false;
        }
        return paramOrNav
                || (listOp.child() instanceof IRApply apply && !apply.args().isEmpty()
                        && apply.callee() instanceof IRReference callee
                        && callee.referenceKind() == IRReference.ReferenceKind.FUNCTION)
                || (listOp.child() instanceof IRReference ref
                        && ref.referenceKind() == IRReference.ReferenceKind.ALIAS);
    }

    /**
     * Composes a NESTED {@code count} operand ({@link IRListOp.Kind#COUNT} reached as a comparison / equality operand,
     * not a top-level expression) — {@code <emit(child)>.resultCount()}, byte-identical to legacy
     * {@code CollectionHandler.handle(RCountExpr)}. That handler renders {@code compileInterior(argument).resultCount()}
     * with a {@code null} type, carrying the argument builder's refs + static-wildcard imports; the emitter mirrors it
     * by rendering the lowered {@code child} subtree (which equals the legacy {@code compileInterior(argument)} for the
     * non-meta receivers the adapter admits) and appending {@code .resultCount()}. The result is a bare {@code int}
     * chain with no {@code Mapper} wrap — the {@code MapperS.of(...)} a comparison operand needs is applied by
     * {@link #wrapCountValueOperand} at {@link #emitBinaryOp}, exactly as legacy {@code ComparisonHandler.wrapCountOperand}
     * wraps it at the comparison (never in the count handler). Declines (→ legacy) if the child subtree cannot emit.
     *
     * <p><strong>Expected-type invariant (byte-safety precondition).</strong> The child is emitted against the OUTER
     * comparison {@code ctx} (not a count-specific expected type), whereas legacy threads the comparison's inferred
     * {@code Integer} type into {@code compileInterior(argument, ctx.expectedType(), …)}. This is byte-inert ONLY because
     * the adapter's {@code adaptCount} admits a count child solely when it lowers to an <em>expected-type-insensitive</em>
     * receiver — a parameter ({@code MapperS.of}/{@code MapperC.<Item>of}) or a {@link FieldAccess} navigation — both of
     * which ignore the threaded type; a numeric literal (the only expected-type-sensitive leaf) can never be a count
     * receiver ({@code N count} is not valid Rune). If a future increment widens the count-child admission set to an
     * expected-type-sensitive shape, re-verify this against legacy's {@code Integer}-threaded {@code compileInterior}.
     */
    private Optional<JavaStatementBuilder> emitNestedCount(IRListOp count, ExpressionContext ctx, JavaTypeUtil typeUtil,
                                                           JavaTypeTranslator translator, JavaClass<?> outputType) {
        Optional<JavaStatementBuilder> child = emit(count.child(), ctx, typeUtil, translator, outputType);
        if (child.isEmpty()) {
            return Optional.empty();
        }
        JavaStatementBuilder argBuilder = child.get();
        String arg = HandlerHelper.render(argBuilder);
        return Optional.of(JavaExpression.from(arg + ".resultCount()", null,
                argBuilder.getRefs(), argBuilder.getStaticWildcardImports()));
    }

    /**
     * A function CALL — byte-identical to the function-call branch of {@code ReferenceHandler.handle(RSymbolReference)}
     * (lines 620-642): {@code MapperS.of(<receiver>.evaluate(<unwrapped arg1>, <unwrapped arg2>, …))}.
     * <ul>
     *   <li>{@code <receiver>} = the {@code @Inject} field name from the compiler-supplied {@link #callReceiverResolver}
     *       ({@code lowerCamelCase(callee.name())} + the shortcut-collision {@code "0"} suffix — a Java-emission
     *       decision kept off the neutral {@link IRApply}, the L-029 split). No resolver / unresolved receiver →
     *       decline.</li>
     *   <li>each {@code <unwrapped argN>} = the argument emitted against a NEUTRALIZED context —
     *       {@code ExpressionContext.of(null, ctx.scope())}, the EXACT construction legacy's per-arg
     *       {@code compiler.compile(arg, null, ctx.scope())} builds (the #360 {@code evaluateArgExpectedTypeReset}
     *       facet: a call boundary RESETS the argument expectation — the callee-param-driven channels below are the
     *       one arg-expectation law). Threading the OUTER {@code ctx} here was a latent divergence seat closed at
     *       #489: the int-literal render is expected-type-sensitive ({@code isBigDecimalContext}), so an
     *       int-literal arg under a BigDecimal-expected outer context would have rendered
     *       {@code BigDecimal.valueOf(N)} where legacy's neutral arg compile renders the bare {@code N}
     *       (population-inert — the ring proves no corpus shape hits it — but latent). The emitted arg is then
     *       UNWRAPPED to the bare {@code evaluate} slot by delegating to the legacy oracle
     *       {@link ReferenceHandler#unwrapForEvaluateArg(JavaStatementBuilder, boolean)} — the SAME helper the
     *       Path-1 function-call branch calls, so byte-identical by construction. A scalar-param / literal arg
     *       carries a {@code MapperS.of(...)} wrap and strips STRUCTURALLY (the oracle's structural branch,
     *       {@code MAPPER_S} dropped atomically — identical to the slice-1 {@code unwrapToBuilder} path); an
     *       alias/shortcut arg (slice 2) is the bare {@code Mapper} {@code aliasName(inputs)} (L-031
     *       {@code emitAlias}) and a NAVIGATION arg (the #489 L-042 revival) is the bare {@code Mapper} chain the
     *       standing nav render produces — both take the oracle's chained-{@code Mapper} fall-through accessor
     *       ({@code .get()} into a SINGLE callee param, {@code .getMulti()} into a MULTI one — the
     *       {@code tailMulti} accessor law, the per-index {@code asMulti} supplied by
     *       {@link CallParamMultiResolver} reusing legacy {@code evaluateParamIsMulti}; golden witness
     *       {@code resolveAdjustableDates.evaluate(MapperS.of(valuationDates).<...>map(...).get())}, cdm6
     *       {@code AdjustedValuationDates}), preserving refs and static wildcards verbatim. A
     *       non-{@link JavaExpression} arg declines (byte-safe); the adapter's call-argument admission — the
     *       {@code isSimpleCallArg} / {@code isNavCallArg} / {@code isFilterExtractBoundItemOperand}
     *       or-composition (#489) — is the sole admission control.</li>
     *   <li>the {@code evaluate(...)} result is wrapped once in {@code MapperS.of(...)} (the scalar-output case; the
     *       adapter defers a multi/meta output), via {@link JavaExpression#wrappedInMapperSOf} which adds
     *       {@code MAPPER_S}. Refs / static-wildcard imports union in from each unwrapped arg, exactly as legacy
     *       unions the arg builders.</li>
     * </ul>
     */
    private Optional<JavaStatementBuilder> emitApply(IRApply apply, ExpressionContext ctx, JavaTypeUtil typeUtil,
                                                     JavaTypeTranslator translator, JavaClass<?> outputType) {
        if (!(apply.callee() instanceof IRReference callee)) {
            return Optional.empty();
        }
        if (callee.referenceKind() == IRReference.ReferenceKind.RULE) {
            if (!apply.args().isEmpty()) {
                return emitRuleApply(apply); // #498: the args-present rule invocation — its own render leg
            }
            return emitRuleDelegation(callee); // the bare-rule-delegation render is distinct (input arg, no arg loop)
        }
        if (callReceiverResolver == null || callee.referenceKind() != IRReference.ReferenceKind.FUNCTION) {
            return Optional.empty();
        }
        String receiver = callReceiverResolver.receiverFor(callee);
        if (receiver == null) {
            return Optional.empty(); // the enclosing function / receiver name could not be resolved — defer to legacy
        }
        List<IRExpr> args = apply.args();
        List<String> argStrings = new ArrayList<>(args.size());
        Set<JavaClass<?>> refs = new HashSet<>();
        Set<JavaClass<?>> staticWildcards = new HashSet<>();
        // The NEUTRALIZED per-arg context — legacy's own construction (compile(arg, null, ctx.scope()) builds
        // ExpressionContext.of(null, scope): the #360 evaluateArgExpectedTypeReset facet). The outer ctx's
        // expectedType must NOT thread into an arg: the int-literal render is expected-type-sensitive, and a
        // BigDecimal-expected outer context would coerce an int-literal arg legacy renders bare (#489). A null
        // outer ctx stays null (already expectation-free; the leaf emits null-check ctx before reading it).
        ExpressionContext argCtx = ctx == null ? null : ExpressionContext.of(null, ctx.scope());
        for (int i = 0; i < args.size(); i++) {
            Optional<JavaStatementBuilder> argEmitted = emit(args.get(i), argCtx, typeUtil, translator, outputType);
            if (argEmitted.isEmpty()) {
                return Optional.empty(); // an arg declined — the whole call falls back
            }
            if (!(argEmitted.get() instanceof JavaExpression argExpr)) {
                return Optional.empty(); // the oracle requires a JavaExpression — defer to legacy (byte-safe)
            }
            // Unwrap the arg to the bare evaluate slot via the legacy oracle ReferenceHandler.unwrapForEvaluateArg —
            // the SAME helper Path-1's function-call branch calls, so byte-identical by construction. A scalar param /
            // literal arg carries a MapperS.of(...) wrap and strips STRUCTURALLY (the oracle's structural branch,
            // identical to the slice-1 unwrapToBuilder() path) regardless of asMulti; an alias arg is the bare Mapper
            // aliasName(inputs) (L-031 emitAlias, no structural wrap) and takes the oracle's fall-through accessor —
            // .get() into a SINGLE param, .getMulti() into a MULTI param (the multi-PARAM lever, legacy facet
            // tailMulti) — preserving refs + static wildcards verbatim. The asMulti flag follows the CALLEE PARAMETER's
            // cardinality (not the arg's), supplied per index by the compiler's CallParamMultiResolver reusing legacy
            // evaluateParamIsMulti; a null resolver yields the scalar accessor (the single-parameter default).
            boolean asMulti = callParamMultiResolver != null && callParamMultiResolver.paramAcceptsMulti(callee, i);
            // #492: a POINT-FREE arg passes RAW — the mirror of legacy's argIsBareFnInvocation
            // branch in ReferenceHandler's own call-arg loop (`unwrapped = compiled`, no unwrap):
            // the oracle-rendered `f.evaluate(...)` result is a bare VALUE, not a Mapper, so the
            // unwrap oracle's fall-through `.get()` must not fire (the #492 leg-2 ring's ONE
            // mismatch class — 151 carriers, all this spurious accessor).
            JavaStatementBuilder unwrapped = args.get(i) instanceof IRPointFreeApply
                    ? argExpr
                    : ReferenceHandler.unwrapForEvaluateArg(argExpr, asMulti);
            argStrings.add(HandlerHelper.render(unwrapped));
            refs.addAll(unwrapped.getRefs());
            staticWildcards.addAll(unwrapped.getStaticWildcardImports());
        }
        JavaExpression innerCall = JavaExpression.from(
                receiver + ".evaluate(" + String.join(", ", argStrings) + ")", null, refs, staticWildcards);
        if (apply.cardinality() != ExpressionCardinality.MULTI) {
            return Optional.of(JavaExpression.wrappedInMapperSOf(innerCall)); // SINGLE output — the L-041 form
        }
        // MULTI output → MapperC.<Item>of(recv.evaluate(args)), byte-identical to legacy tryMultiValueWrap. The
        // <Item> witness is the call-result item type (apply.type()), with the L-029 witness/output simple-name
        // FQN-collision — reusing the EXACT collisionFqn + wrappedInMapperCOfSingle the multi-param leaf emitVariable
        // uses (the arg-unwrap above is output-cardinality-independent; only the result wrap differs). Declines to
        // legacy when the witness cannot be named — mirroring emitVariable's multi-param guards (legacy's
        // mapperCOfWrapWitness also yields no witness there).
        if (translator == null || apply.type() == null || apply.type().isMissing()) {
            return Optional.empty();
        }
        RType itemType = apply.type().type();
        if (itemType == null) {
            return Optional.empty();
        }
        JavaClass<?> witness = translator.toJavaReferenceType(itemType);
        if (witness == null) {
            return Optional.empty();
        }
        String fqn = collisionFqn(witness, outputType);
        if (fqn != null) {
            collisionFqnRenders++; // §4.2 instrumentation: an IR-produced cross-namespace FQN-inline render
        }
        return Optional.of(JavaExpression.wrappedInMapperCOfSingle(innerCall, null, witness, fqn));
    }

    /**
     * Renders a bare RULE delegation — {@code MapperS.of(<Name>Rule.evaluate(<binding>))}, byte-identical to legacy
     * {@code ReferenceHandler.renderImplicitRuleInvocation}. Two render paths by position, both selected
     * compiler-side (the emitter is AST-blind):
     * <ul>
     *   <li><b>In-lambda</b> ({@code … then SomeRule} inside an extract/filter/then lambda) — the
     *       {@link #ruleDelegationRenderer}, when installed, supplies the COMPLETE render (the polymorphic
     *       {@code item.get()}/{@code thenArg.get()} binding derived by reusing the legacy oracle verbatim, L-049).</li>
     *   <li><b>Rule-body top level</b> ({@code output = SomeRule}) — no renderer is installed, so this falls to the
     *       L-045 form: the receiver is the {@code @Inject} {@code <Name>Rule} field (the compiler's
     *       {@link #ruleReceiverResolver}, reusing legacy {@code ruleInvocationReceiver}) and the implicit-input
     *       argument is the enclosing rule's synthetic {@code input} parameter, rendered as the bare identifier
     *       (legacy's {@code MapperS.of(input)} compile strips structurally through {@code unwrapForEvaluateArg}).
     *       The {@code input} binding is correct unconditionally here because the compiler installs the renderer
     *       for EVERY in-lambda delegation, so a renderer-less rule delegation is necessarily top-level.</li>
     * </ul>
     * The {@code input} identifier and the receiver carry no import of their own (the {@code @Inject} field + its
     * import are declared by the shared rule-class generator, identically on both paths); {@code MapperS} comes from
     * the wrap. A {@code null} {@link #ruleReceiverResolver} (with no renderer) is a non-rule-family emission and
     * declines to legacy.
     */
    private Optional<JavaStatementBuilder> emitRuleDelegation(IRReference callee) {
        if (ruleDelegationRenderer != null) {
            return ruleDelegationRenderer.render(); // in-lambda: the legacy oracle's full render (item.get()/thenArg.get())
        }
        if (ruleReceiverResolver == null) {
            return Optional.empty(); // not a rule-family emission (no resolver) — defer to legacy
        }
        String receiver = ruleReceiverResolver.receiverFor(callee);
        if (receiver == null) {
            return Optional.empty(); // the <Name>Rule receiver could not be resolved — defer to legacy
        }
        JavaExpression innerCall = JavaExpression.from(receiver + ".evaluate(input)", null, Set.of());
        return Optional.of(JavaExpression.wrappedInMapperSOf(innerCall));
    }

    /**
     * Renders an args-present rule invocation ({@code <Rule>(<arg>)} — the #498 teach) by delegating
     * to the compiler-installed root-site-correlated {@link #ruleApplyRenderer} — the literal
     * {@code referenceHandler.handle(expr, ctx, this)} fallback call (the one line legacy
     * {@code ExpressionCompiler.visitSymbolReference} is), byte-identical by the strongest argument
     * (see {@link RuleApplyRenderer}). Deliberately NO {@code type().isMissing()} gate — the
     * RULE-delegation family's established posture ({@link #emitRuleDelegation} renders type-blind
     * on both its paths, never consulting the fact), and the #498 census read the class's dominant
     * face AS typeMissing (the {@code fromRule}/{@code fromReport} wrapper factories' synthesized
     * calls sit outside the type fixed point's walk): the render never reads the fact, and gating
     * here would forfeit the exact population the arm exists to claim. A {@code null} renderer
     * (a non-rule-apply claim root — the frame-slot kind gate) or a {@code null} return
     * (uncorrelated: a CHILD-position rule call inside another claim's lowered tree) declines
     * identically — the pre-#498 bytes.
     */
    private Optional<JavaStatementBuilder> emitRuleApply(IRApply apply) {
        if (ruleApplyRenderer != null) {
            JavaStatementBuilder rendered = ruleApplyRenderer.render(apply);
            if (rendered != null) {
                return Optional.of(rendered);
            }
        }
        return Optional.empty();
    }

    /**
     * Renders a meta-feature navigation ({@link IRMetaAccess} — the #499 metaNav conversion) by
     * delegating to the compiler-installed root-site-correlated {@link #metaNavRenderer} — the
     * literal {@code super.visitFeatureCall(site, ctx)} relabel-belt line, byte-identical to the
     * L-109d delegation it converts by the strongest argument (see {@link MetaNavRenderer}).
     * Deliberately NO {@code type().isMissing()} gate — the #497 census read the seat's typing
     * heavily fragmented, and the oracle render never consults the fact (the delegation family's
     * type-blind posture, the {@link #emitRuleApply} precedent): gating here would forfeit exactly
     * the population the arm exists to claim. A {@code null} renderer (a non-meta-access claim
     * root — the frame-slot kind gate: an INTERIOR meta hop inside a natively-composed claim) or a
     * {@code null} return (uncorrelated) declines identically — the pre-#499 bytes.
     */
    private Optional<JavaStatementBuilder> emitMetaAccess(IRMetaAccess metaAccess) {
        if (metaNavRenderer != null) {
            JavaStatementBuilder rendered = metaNavRenderer.render(metaAccess);
            if (rendered != null) {
                return Optional.of(rendered);
            }
        }
        return Optional.empty();
    }

    /**
     * A single-hop feature navigation — byte-identical to the dominant getter arm of
     * {@code NavigationHandler.handle(RFeatureCall)}:
     * {@code <receiver>.<Witness>{map|mapC}("getFeature", v -> v.getFeature())}. The receiver
     * subtree recurses through {@link #emit}; the node-local step composes:
     * <ul>
     *   <li>the {@code <Witness>} generic — {@code translator.toJavaReferenceType(node.type())}
     *       rendered as legacy's first-claim-wins {@code ImportCollisionResolver.typeRef} SENTINEL
     *       (the fqnWitness facet, #478 — resolved at file assembly; uncontested it reads the bare
     *       simple name, mirroring {@code NavigationHandler.witnessSentinelTypeParam}), with the
     *       witness class added to refs (java.lang witnesses are filtered downstream by the import
     *       collector, exactly as for the legacy {@code addWitnessTypeRef}) — UNLESS the witness's
     *       Java simple name collides with the enclosing function output's (a different type): then
     *       it renders FULLY-QUALIFIED inline and its import is SUPPRESSED
     *       ({@link #collisionFqn}, mirroring {@code NavigationHandler.witnessOutputCollisionFqn});</li>
     *   <li>the getter {@code getFeature} = {@code "get" + toFirstUpper(feature)};</li>
     *   <li>the map method — {@code map}/{@code mapC} by the hop's OWN feature cardinality
     *       ({@code featureCardinality}, per-hop and receiver-agnostic, mirroring {@code resolveMapMethod});</li>
     *   <li>the lambda variable — {@code scope.registerDeferredLambdaParam(toLowerCamelCase(
     *       receiverTypeName))} (the parity linchpin, #478; was {@code disambiguate}): legacy names
     *       every nav-hop var through the DEFERRED registry, whose escape decision resolves at
     *       finalization against later-declared hoist locals — the same {@code ctx.scope()} channel,
     *       so the resolution is identical by construction; with no scope it falls back to
     *       {@code "_" + name} exactly as {@code resolveLambdaVarName} does.</li>
     * </ul>
     * Declines (→ legacy) when the receiver declines, or the witness / receiver type name cannot be
     * named (no translator, missing type). The receiver may be single or multi (a chain through a multi
     * hop, or a multi-parameter base), each rendering its own {@code map}/{@code mapC} per hop.
     */
    private Optional<JavaStatementBuilder> emitFieldAccess(FieldAccess fieldAccess, ExpressionContext ctx,
                                                           JavaTypeUtil typeUtil, JavaTypeTranslator translator,
                                                           JavaClass<?> outputType) {
        // The #469 multiHopItemNav/metaItemReceiver teach: a guarded item-rooted nav (chained hops, whose
        // deferred-sentinel lambda naming a node-local render cannot reproduce, or a meta-typed item
        // receiver needing the Type-coercion deref) is served COMPLETE by the compiler's renderer — the
        // exact legacy fallback render. A null renderer / un-indexed node keeps the hop-by-hop render below.
        if (itemNavRenderer != null) {
            JavaStatementBuilder delegated = itemNavRenderer.render(fieldAccess);
            if (delegated != null) {
                return Optional.of(delegated);
            }
        }
        Optional<JavaStatementBuilder> receiverEmitted = emit(fieldAccess.receiver(), ctx, typeUtil, translator, outputType);
        if (receiverEmitted.isEmpty()) {
            return Optional.empty(); // the receiver declined — the whole navigation falls back
        }
        // #499 — the only-element re-wrap (legacy NavigationHandler's nav_after_get_rewrap facet
        // verbatim): an ONLY_ELEMENT collapse receiver renders the bare item `<chain>.get()`, and
        // a further map/mapC hop needs a Mapper receiver, so the navigation re-wraps it
        // `MapperS.of(<chain>.get())` and adds the MapperS ref (legacy adds HandlerHelper.MAPPER_S
        // at the same seat; idempotent with the inner chain's own MapperS leaf — refs is a Set).
        // Keyed on the IR receiver's own kind exactly as legacy keys on the AST receiver shape
        // (`expr.receiver() instanceof RListOpExpr le && le.op() == ONLY_ELEMENT`). The sibling
        // collapsedMetaDeref arm (#317 — a META-wrapper collapse hoist+deref) takes the plain
        // re-wrap on BOTH routes by the two-leg argument (the Seat-1 #499 MF-6 recut): param/nav
        // children are non-meta at every hop by the adapter's gates, and the admitted call/alias
        // children fail legacy onlyElementLeafAttribute's RAttribute filter (a non-attribute
        // symbol resolves no leaf → metaWrapperOf(null) null → the deref arm never fires; the
        // name-collision implicitItemSymbolLeaf fallback is the one named drift face, zero live
        // carriers). A FIRST collapse receiver is Mapper-valued and chains directly — no wrap,
        // same as legacy.
        if (fieldAccess.receiver() instanceof IRListOp recvCollapse
                && recvCollapse.op() == IRListOp.Kind.ONLY_ELEMENT) {
            JavaStatementBuilder bare = receiverEmitted.get();
            Set<JavaClass<?>> wrapRefs = new HashSet<>(bare.getRefs());
            wrapRefs.add(HandlerHelper.MAPPER_S);
            receiverEmitted = Optional.of(JavaExpression.from(
                    "MapperS.of(" + HandlerHelper.render(bare) + ")", null,
                    wrapRefs, bare.getStaticWildcardImports()));
        }
        if (translator == null || fieldAccess.type() == null || fieldAccess.type().isMissing()) {
            return Optional.empty(); // cannot name the <Witness> — defer
        }
        RType resultType = fieldAccess.type().type();
        if (resultType == null) {
            return Optional.empty();
        }
        JavaClass<?> witness = translator.toJavaReferenceType(resultType);
        String receiverTypeName = receiverTypeName(fieldAccess.receiver());
        if (witness == null || receiverTypeName == null) {
            return Optional.empty(); // cannot name the witness / lambda var — defer
        }
        JavaStatementBuilder receiver = receiverEmitted.get();
        String getter = "get" + JavaNamingUtil.toFirstUpper(fieldAccess.feature());
        // facet accessorTypeEscape: the lambda-BODY accessor escapes an inherited-method collision
        // (getType/getClass → _getType/_getClass) via the POJO subsystem's shared SOT, while the map LABEL
        // keeps the bare getter — byte-identical to legacy NavigationHandler:225-232. Identity for every
        // non-reserved feature, so existing driven navs are byte-unchanged; only a feature named type/class
        // (newly reachable as an item-nav body) differs.
        String accessor = JavaPojoProperty.escapeOperationName(getter);
        // map/mapC by THIS hop's own declared feature cardinality (per-hop, receiver-agnostic) —
        // mirrors legacy NavigationHandler.resolveMapMethod; the MapperS/MapperC runtime is polymorphic,
        // so a single feature off a MULTI receiver correctly renders `.map` off the MapperC.
        String mapMethod = fieldAccess.featureCardinality() == ExpressionCardinality.MULTI ? "mapC" : "map";
        // v3.2 seat 7 (F11, LAW 77): the #358 structural pre-escape against the LIVE-bound case local the hop
        // reads off - the SAME declaration the default route's bound rung consults
        // (NavigationHandler.preEscapeAgainstCastLocal, through its receiver-builder overload): the in-lambda switch
        // seats `disambiguate` their case local, which the deferred registry below cannot see, so the emitter rendered
        // the shadowing `c18OptA -> c18OptA.getAv()` where the default route and the golden write `_c18OptA` (the
        // seat's fix3 measurement, the twelve chaos C18ToKind rows on the IR route). A receiver that is not a switch-subject
        // binding LIVE in the scope chain keeps the registry's answer exactly - the gate is the scope's registry, never the
        // receiver's text (the s7a chain's catch: a text gate fired on every input named after its type - `__trade`).
        String desired = com.regnosys.rosetta.generator.java.expression.handlers.NavigationHandler
                .preEscapeAgainstReceiver(toLowerCamelCase(receiverTypeName), receiver,
                        ctx != null ? ctx.scope() : null);
        // registerDeferredLambdaParam (#478; was disambiguate): legacy names EVERY nav-hop lambda
        // var through the deferred registry (NavigationHandler's resolveLambdaVarName arms), whose
        // escape decision resolves at finalization when the parent chain is complete — a hoist
        // local declared LATER in the statement (the #198 ctor-hoist class) is visible as taken.
        // The immediate disambiguate read the scope too early: a nav inside a hoisted constructor
        // rendered the bare var where legacy's deferred resolution escapes it (`_passThroughItem`
        // vs `passThroughItem`, MapEquityOptionPayout — the first #478 ring's last mismatch). The
        // deferred channel is legacy's own, so the two routes resolve identically BY CONSTRUCTION;
        // on the pre-#478 proven population the two channels agreed everywhere (a divergence would
        // have been a byte mismatch), so the switch is inert there.
        String lambdaVar = (ctx != null && ctx.scope() != null)
                ? ctx.scope().registerDeferredLambdaParam(desired)
                : "_" + desired;
        // Witness/output collision: render FQN-inline + suppress the import on collision, else the
        // first-claim-wins ImportCollisionResolver SENTINEL + import — byte-identical to legacy
        // NavigationHandler (typeParam = witnessCollisionFqn != null ? "<fqn>" :
        // witnessSentinelTypeParam(...); addWitnessTypeRef only when fqn == null). The sentinel (#478,
        // the fqnWitness facet mirrored): a raw simple name here BYPASSES the render-order resolver, so
        // a construction type sharing the simple name in the same file would win a bare import it never
        // gets under legacy (the first #478 ring's duplicate-import class — the fpml witness vs the cdm
        // ctor type in MapExerciseProcedure). The emitter's witness is always non-meta with text ==
        // getSimpleName (the adapter's gates), exactly legacy's sentinel congruence gate, and an
        // uncontested sentinel resolves to the bare simple name — byte-identical off-collision.
        String fqn = collisionFqn(witness, outputType);
        if (fqn != null) {
            collisionFqnRenders++; // §4.2 instrumentation: an IR-produced cross-namespace FQN-inline render
        }
        Set<JavaClass<?>> refs = new HashSet<>(receiver.getRefs());
        if (fqn == null) {
            refs.add(witness);
        }
        String witnessRender = fqn != null
                ? fqn
                : ImportCollisionResolver.typeRef(witness.getCanonicalName().withDots());
        String code = HandlerHelper.render(receiver) + ".<" + witnessRender + ">" + mapMethod
                + "(\"" + getter + "\", " + lambdaVar + " -> " + lambdaVar + "." + accessor + "())";
        return Optional.of(JavaExpression.from(code, null, refs, receiver.getStaticWildcardImports()));
    }

    /**
     * The witness's fully-qualified (dotted) name when its Java simple name collides with the enclosing
     * function OUTPUT type's simple name but they are DIFFERENT types — the FQN-inline render that dodges
     * a duplicate same-simple-name import. Byte-identical to legacy
     * {@code ReferenceHandler.mapperCWitnessOutputCollisionFqn} / {@code NavigationHandler.witnessOutputCollisionFqn}:
     * a collision requires equal simple names AND different canonical names. Returns {@code null} (→ bare
     * witness + its import) when there is no output, the simple names differ, or the witness IS the output
     * type (same canonical — the {@code merge(xs T (0..*)) -> result T} self-referential case). The output's
     * meta-annotation and resolvability are pre-handled by the caller (a meta/unresolved output is passed
     * as {@code null}).
     *
     * <p>Unlike legacy (which early-returns for a META-annotated witness attribute), this method needs no
     * meta-witness guard: a meta feature never reaches the witnessed-render seats (pre-#499 the adapter
     * declined it; since #499 it lowers to the distinct {@code IRMetaAccess} kind, served whole by the
     * meta-aware oracle renderer — either way only NON-meta witnesses reach here), and for a non-meta
     * witness legacy's {@code detectMetaKind} check is a no-op, so the decisions
     * agree. (The output's meta-annotation IS handled — the caller passes {@code null} for a meta output.)
     *
     * <p>Package-private (not private) so the unit test can pin this legacy-mirrored decision directly:
     * a translator-produced witness that collides with the output is hard to construct end-to-end, so the
     * decision is verified here while the bare/no-collision render is covered through {@link #emit}.
     */
    static String collisionFqn(JavaClass<?> witness, JavaClass<?> outputType) {
        if (witness == null || outputType == null) {
            return null;
        }
        String witnessFqn = witness.getCanonicalName().withDots();
        if (witness.getSimpleName().equals(outputType.getSimpleName())
                && !witnessFqn.equals(outputType.getCanonicalName().withDots())) {
            return witnessFqn;
        }
        return null;
    }

    /** The receiver's declared type simple name — the source of the navigation lambda variable. */
    private static String receiverTypeName(IRExpr receiver) {
        if (receiver.type() == null || receiver.type().isMissing() || receiver.type().type() == null) {
            return null;
        }
        return receiver.type().type().name();
    }

    /**
     * The lowerCamelCase Java identifier for a (possibly namespace-qualified) Rune type name —
     * byte-identical to {@code NavigationHandler.toLowerCamelCase}: strip any qualifier (the
     * substring after the last {@code '.'}), then lowercase char 0. A cross-namespace type carries a
     * qualified name (e.g. {@code fpml.Money}); the lambda variable derives from the SIMPLE name.
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

    /**
     * The unary STRUCTURAL emission for an existence / absence check, rendered by recursing into the
     * single operand subtree and composing the fixed {@code ExpressionOperatorsNullSafe} static call
     * {@code {method}(arg)} — byte-identical to {@code ExistenceHandler.handle(RExistenceExpr)}. The
     * {@code method} folds the operator and optional modifier: {@code is absent} → {@code notExists};
     * {@code exists} → {@code exists}, or {@code singleExists}/{@code multipleExists} under a
     * {@code single}/{@code multiple} qualifier. If the operand is not (yet) emitter-expressible the
     * whole node declines (strangler-safe). The existence call contributes only the
     * {@code EXPRESSION_OPERATORS_NULL_SAFE} static wildcard (no library ref of its own); the
     * operand's refs/wildcards are unioned in, exactly as the legacy handler unions {@code argBuilder}.
     */
    private Optional<JavaStatementBuilder> emitExistence(Existence existence, ExpressionContext ctx,
                                                         JavaTypeUtil typeUtil, JavaTypeTranslator translator,
                                                         JavaClass<?> outputType) {
        Optional<JavaStatementBuilder> argEmitted = emit(existence.arg(), ctx, typeUtil, translator, outputType);
        if (argEmitted.isEmpty()) {
            return Optional.empty(); // the operand declined — the whole check falls back
        }
        JavaStatementBuilder arg = argEmitted.get();
        String method = switch (existence.op()) {
            case ABSENT -> "notExists";
            case EXISTS -> {
                Existence.ExistMod modifier = existence.modifier();
                if (modifier == null) {
                    yield "exists";
                }
                yield switch (modifier) {
                    case SINGLE -> "singleExists";
                    case MULTIPLE -> "multipleExists";
                };
            }
        };
        Set<JavaClass<?>> refs = new HashSet<>(arg.getRefs());
        Set<JavaClass<?>> staticWildcards = new HashSet<>();
        staticWildcards.add(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE);
        staticWildcards.addAll(arg.getStaticWildcardImports());
        String code = method + "(" + HandlerHelper.render(arg) + ")";
        return Optional.of(JavaExpression.from(code, null, refs, staticWildcards));
    }

    /**
     * The STRUCTURAL emission for a {@link BinaryOp} — an ordered comparison, an equality, or a
     * logical {@code and}/{@code or} — rendered by recursing into both operand subtrees through
     * this emitter and composing the operator's Java idiom. If either operand is not (yet)
     * emitter-expressible the whole node declines (strangler-safe).
     *
     * <p>Two idiom families, dispatched on {@link BinaryOp#op()}:
     * <ul>
     *   <li><strong>comparison / equality</strong> ({@code < > <= >=}, {@code = <>}) → a static
     *       {@code ExpressionOperatorsNullSafe} call {@code {method}(left, right,
     *       CardinalityOperator.{All|Any})} — {@link #emitComparisonLike};</li>
     *   <li><strong>logical</strong> ({@code and}/{@code or}) → a {@code ComparisonResult}
     *       instance-method chain {@code left.{andNullSafe|orNullSafe}(right)} —
     *       {@link #emitLogical}.</li>
     * </ul>
     * Either way the operand refs and static-wildcard imports are <em>unioned</em> in (dropping a
     * child's import would diverge), exactly as the legacy handlers union
     * {@code leftBuilder}/{@code rightBuilder}. Byte-identical to {@code ComparisonHandler} /
     * {@code LogicalHandler}.
     */
    private Optional<JavaStatementBuilder> emitBinaryOp(BinaryOp binary, ExpressionContext ctx,
                                                        JavaTypeUtil typeUtil, JavaTypeTranslator translator,
                                                        JavaClass<?> outputType) {
        // Arithmetic threads a NUMERIC expected type (the int/number JOIN) into its operands so a literal
        // renders against it (an int literal in a BigDecimal join → MapperS.of(BigDecimal.valueOf(N))), so it
        // emits its OWN operands. Comparison/equality likewise thread the join into a NUMERIC-LITERAL operand
        // below (the count >= 1 / arg < 0 shapes); a logical and/or and a non-numeric equality stay
        // expected-type-agnostic and render against the outer ctx.
        switch (binary.op()) {
            case ADD, SUB, MUL, DIV -> {
                return emitArithmetic(binary, ctx, typeUtil, translator, outputType);
            }
            default -> { /* comparison / logical — fall through */ }
        }
        // An INHERITED enum operand is requalified by legacy to the sibling's (child) enum — decline when this
        // emitter's enum class (the node's type — the expected enum for a bare value, the author's enum for an
        // explicit `Enum -> Value`) differs from the sibling's (see enumOperandRequalifies). Only EQ/NEQ ever carry
        // an enum operand (adaptComparison/adaptLogical do not admit one), so this is a no-op for the other operators.
        if (enumOperandRequalifies(binary.left(), binary.right(), translator)
                || enumOperandRequalifies(binary.right(), binary.left(), translator)) {
            return Optional.empty();
        }
        // The NUMERIC comparison/equality operand guard — the int/number JOIN + decline rule, mirroring
        // emitArithmetic. A numeric-LITERAL operand re-renders against the sibling's resolved numeric type
        // (the expected-type threading legacy applies via ComparisonHandler.inferNumericType: an int literal
        // against a number sibling → MapperS.of(BigDecimal.valueOf(N)); against an int sibling → bare
        // MapperS.of(N)); a RESOLVED operand whose own kind differs from the join would be COERCED by legacy
        // (the gm-aware `Type coercion` map the IR does not re-render) → the pair declines. Since #478 the
        // guard classifies EVERY numeric operand pair, not only literal-present ones: the nav arm made
        // mixed RESOLVED/RESOLVED pairs reachable (int-nav vs number-nav — CheckMaturity's
        // `periodMultiplier >= query -> maturity`, whose golden carries the Integer→BigDecimal coercion
        // map; the first #478 ring's mismatch class), where the pre-#478 reachable population had no such
        // pair (the literal-present gate was vacuously complete — the standing SOT proves it). Non-numeric
        // paths (boolean/string/enum equality, logical and/or — both witnesses null) are untouched.
        ExpressionContext leftCtx = ctx;
        ExpressionContext rightCtx = ctx;
        if (typeUtil != null && translator != null) {
            boolean literalPresent =
                    isNumericLiteralOperand(binary.left()) || isNumericLiteralOperand(binary.right());
            String leftWitness = numericWitness(binary.left(), translator);
            String rightWitness = numericWitness(binary.right(), translator);
            if (literalPresent && (leftWitness == null || rightWitness == null)) {
                return Optional.empty(); // a numeric literal with an unresolved/non-numeric sibling — defer
            }
            if (leftWitness != null && rightWitness != null) {
                // The int/number JOIN: number (BigDecimal) if EITHER operand is number, else int (Integer).
                boolean joinIsNumber = "BigDecimal".equals(leftWitness) || "BigDecimal".equals(rightWitness);
                String operandType = joinIsNumber ? "BigDecimal" : "Integer";
                // A RESOLVED operand whose own kind differs from the join would be coerced by legacy (and the
                // IR does not re-render a non-literal operand) → decline; a literal re-renders against the
                // join so is safe.
                if (!(binary.left() instanceof IRLiteral) && !operandType.equals(leftWitness)) {
                    return Optional.empty();
                }
                if (!(binary.right() instanceof IRLiteral) && !operandType.equals(rightWitness)) {
                    return Optional.empty();
                }
                JavaType operandExpectedType = joinIsNumber ? typeUtil.BIG_DECIMAL : typeUtil.INTEGER;
                ExpressionContext numericCtx =
                        ExpressionContext.of(operandExpectedType, ctx == null ? null : ctx.scope());
                if (binary.left() instanceof IRLiteral) {
                    leftCtx = numericCtx;
                }
                if (binary.right() instanceof IRLiteral) {
                    rightCtx = numericCtx;
                }
            }
        }
        Optional<JavaStatementBuilder> leftEmitted = emit(binary.left(), leftCtx, typeUtil, translator, outputType);
        Optional<JavaStatementBuilder> rightEmitted = emit(binary.right(), rightCtx, typeUtil, translator, outputType);
        if (leftEmitted.isEmpty() || rightEmitted.isEmpty()) {
            return Optional.empty(); // an operand declined — the whole binary op falls back
        }
        // A bare enum-value operand renders the unwrapped dotted constant; wrap it MapperS.of(EnumName.VALUE)
        // for the comparison/equality idiom (legacy HandlerHelper.wrapEnumOperand). No-op for everything else.
        JavaStatementBuilder left = wrapEnumValueOperand(binary.left(), leftEmitted.get());
        JavaStatementBuilder right = wrapEnumValueOperand(binary.right(), rightEmitted.get());
        // A count operand renders the bare int chain <chain>.resultCount(); wrap it MapperS.of(...) for the
        // comparison/equality idiom (legacy ComparisonHandler.wrapCountOperand). No-op for everything else,
        // and mutually exclusive with the enum wrap above (a count is never an enum value).
        left = wrapCountValueOperand(binary.left(), left);
        right = wrapCountValueOperand(binary.right(), right);
        return switch (binary.op()) {
            case AND, OR -> emitLogical(binary.op(), left, right);
            case LT, GT, LTE, GTE, EQ, NEQ -> emitComparisonLike(binary.op(), left, right);
            case ADD, SUB, MUL, DIV -> throw new IllegalStateException("arithmetic handled above");
        };
    }

    /**
     * The numeric ARITHMETIC emission for a {@link BinaryOp} ({@code + - * /}) — a static
     * {@code MapperMaths.<R, O, O>method(left, right)} call, byte-identical to {@code ArithmeticHandler}'s
     * typed-JOIN path. Each operand's numeric kind is classified by {@link #numericWitness} ({@code Integer}
     * for Rune {@code int}, {@code BigDecimal} for {@code number} incl. type-aliases; a literal carries its
     * OWN kind, a resolved operand its declared type). The operand witness {@code O} is the int/number JOIN —
     * {@code BigDecimal} if EITHER operand is {@code number}, else {@code Integer} (legacy
     * {@code joinIsNumber = leftKind==NUMBER || rightKind==NUMBER}); the result witness {@code R} equals
     * {@code O} EXCEPT for {@code divide}, whose result is always {@code BigDecimal} (upstream
     * {@code caseDivideOperation = UNCONSTRAINED_NUMBER}).
     *
     * <p><strong>Byte-safety of the operand cut.</strong> Each operand is re-emitted against the JOIN numeric
     * type ({@code ExpressionContext.of(operandExpectedType, scope)}) — the "expected-type threading" legacy
     * applies via {@code compiler.compile(operand, operandExpectedType, scope)}:
     * <ul>
     *   <li>a <em>literal</em> operand re-renders against the join ({@code MapperS.of(N)} in an int join,
     *       {@code MapperS.of(BigDecimal.valueOf(N))} in a number join) with NO coercion wrapper (its
     *       expression type is null), so it is always byte-safe regardless of its own kind vs the join;</li>
     *   <li>a <em>resolved</em> operand (a parameter) renders {@code MapperS.of(name)} ignoring the expected
     *       type, so it is byte-safe ONLY when its own kind EQUALS the join — otherwise legacy's
     *       {@code compile} inserts a numeric coercion the IR does not reproduce, so such a pair DECLINES.</li>
     * </ul>
     * A resolved operand can differ from the join only when the OTHER operand raises it to {@code number}
     * (e.g. {@code intParam + numberLiteral} → join {@code BigDecimal}, the int param would be coerced →
     * decline); two same-kind resolved operands give a join equal to their shared kind, so both are safe (the
     * L-033 param/param case). A non-numeric operand ({@code null} witness) declines (e.g. {@code string + string}).
     *
     * <p>{@code MAPPER_MATHS} is always contributed; {@code BIG_DECIMAL} when {@code O}/{@code R} is
     * {@code BigDecimal}; operand refs/wildcards union in (the operands' {@code MapperS}/{@code BigDecimal}
     * imports arrive through them, exactly as the legacy handler unions {@code leftBuilder}/{@code rightBuilder}).
     */
    private Optional<JavaStatementBuilder> emitArithmetic(BinaryOp binary, ExpressionContext ctx,
                                                          JavaTypeUtil typeUtil, JavaTypeTranslator translator,
                                                          JavaClass<?> outputType) {
        if (translator == null || typeUtil == null) {
            return Optional.empty();
        }
        String leftWitness = operandWitness(binary.left(), translator);
        String rightWitness = operandWitness(binary.right(), translator);
        if (leftWitness == null || rightWitness == null) {
            return Optional.empty(); // a non-numeric / unclassifiable operand (e.g. string concat) — defer
        }
        // The int/number JOIN: number (BigDecimal) if EITHER operand is number, else int (Integer) —
        // byte-identical to legacy ArithmeticHandler's joinIsNumber.
        boolean joinIsNumber = "BigDecimal".equals(leftWitness) || "BigDecimal".equals(rightWitness);
        String operandType = joinIsNumber ? "BigDecimal" : "Integer";
        // A RESOLVED operand whose own kind differs from the join would be coerced by legacy → decline; a
        // literal operand re-renders against the join (no coercion) so is always safe (see method javadoc).
        if (!(binary.left() instanceof IRLiteral) && !operandType.equals(leftWitness)) {
            return Optional.empty();
        }
        if (!(binary.right() instanceof IRLiteral) && !operandType.equals(rightWitness)) {
            return Optional.empty();
        }
        // Re-emit each operand against the JOIN numeric type — the expected-type threading a literal needs.
        JavaType operandExpectedType = joinIsNumber ? typeUtil.BIG_DECIMAL : typeUtil.INTEGER;
        ExpressionContext operandCtx = ExpressionContext.of(operandExpectedType, ctx == null ? null : ctx.scope());
        Optional<JavaStatementBuilder> leftEmitted = emit(binary.left(), operandCtx, typeUtil, translator, outputType);
        Optional<JavaStatementBuilder> rightEmitted = emit(binary.right(), operandCtx, typeUtil, translator, outputType);
        if (leftEmitted.isEmpty() || rightEmitted.isEmpty()) {
            return Optional.empty(); // an operand declined — the whole arithmetic op falls back
        }
        JavaStatementBuilder left = leftEmitted.get();
        JavaStatementBuilder right = rightEmitted.get();
        String method = switch (binary.op()) {
            case ADD -> "add";
            case SUB -> "subtract";
            case MUL -> "multiply";
            case DIV -> "divide";
            case LT, GT, LTE, GTE, EQ, NEQ, AND, OR ->
                    throw new IllegalArgumentException("not an arithmetic op: " + binary.op());
        };
        // divide's result is ALWAYS BigDecimal (UNCONSTRAINED_NUMBER); otherwise result == operand witness.
        String resultType = binary.op() == BinaryOp.BinOp.DIV ? "BigDecimal" : operandType;
        Set<JavaClass<?>> refs = new HashSet<>();
        refs.add(HandlerHelper.MAPPER_MATHS);
        if ("BigDecimal".equals(operandType) || "BigDecimal".equals(resultType)) {
            refs.add(HandlerHelper.BIG_DECIMAL);
        }
        refs.addAll(left.getRefs());
        refs.addAll(right.getRefs());
        Set<JavaClass<?>> staticWildcards = new HashSet<>();
        staticWildcards.addAll(left.getStaticWildcardImports());
        staticWildcards.addAll(right.getStaticWildcardImports());
        String code = "MapperMaths.<" + resultType + ", " + operandType + ", " + operandType + ">"
                + method + "(" + HandlerHelper.render(left) + ", " + HandlerHelper.render(right) + ")";
        return Optional.of(JavaExpression.from(code, null, refs, staticWildcards));
    }

    /**
     * The arithmetic operand's numeric witness, dispatching the one operand kind this seat deliberately does NOT
     * classify from the neutral IR: an {@code ALIAS} reference. Since the #503 arm-A3 the alias node carries its
     * shortcut BODY's cached type, but this seat keeps classifying through the compiler-supplied
     * {@link #aliasResolver} (which recurses the shortcut body through the legacy classifier — the STRONGER
     * oracle, byte-contract-keeping); without a resolver the alias is unclassifiable → {@code null} → the
     * arithmetic declines. Every other operand (param, literal, navigation) is classified by
     * {@link #numericWitness} from its own kind / {@code type()}.
     */
    private String operandWitness(IRExpr operand, JavaTypeTranslator translator) {
        if (operand instanceof IRReference ref && ref.referenceKind() == IRReference.ReferenceKind.ALIAS) {
            if (aliasResolver == null) {
                return null;
            }
            AliasOperandResolver.Facts facts = aliasResolver.factsFor(ref);
            return facts == null ? null : facts.numericWitness();
        }
        if (operand instanceof BinaryOp nested) {
            return switch (nested.op()) {
                case ADD, SUB, MUL, DIV -> nestedArithmeticWitness(nested, translator);
                case LT, GT, LTE, GTE, EQ, NEQ, AND, OR -> null; // a ComparisonResult is never a numeric operand
            };
        }
        return numericWitness(operand, translator);
    }

    /**
     * The numeric witness of a NESTED arithmetic {@link BinaryOp} operand — mirroring legacy
     * {@code HandlerHelper.numericOperandKind}'s THREE-ARM structure EXACTLY (nested-divide arm first, then the
     * engine arm, structural recursion only on MISSING). Matching the ordering is load-bearing for byte-parity,
     * not cosmetic.
     *
     * <p><strong>Nested-divide arm (FIRST — post-pin legacy, taught at #470).</strong> A nested divide is
     * {@code number} unconditionally (upstream {@code caseDivideOperation = UNCONSTRAINED_NUMBER}), checked
     * BEFORE the engine arm — legacy's own #369 arm ({@code numericOperandKind}'s {@code earlyDiv}): the fork
     * engine's int/int JOIN types a divide {@code int} and would mis-join the enclosing operation
     * ({@code <Integer, Integer, Integer>add(<BigDecimal, …>divide…)} — never a golden shape). The pin-era
     * order here (engine arm first) mirrored PIN-ERA legacy, which had no divide arm; post-pin legacy checks
     * divide first, and an engine-RESOLVED {@code int / int} divide is exactly where the two orders disagree —
     * the {@code <Integer,…>}-where-legacy-renders-{@code <BigDecimal,…>} face the #466 A/B decoded in the
     * dispatch-variant carriers.
     *
     * <p><strong>Engine arm.</strong> When the nested node's {@code type()} is resolved (all its operands'
     * types are present), use it — exactly as legacy's engine arm ({@code numericOperandKind} reading
     * {@code getInferredType}) returns {@code INT}/{@code NUMBER}, or {@code UNKNOWN} (→ decline) for any other
     * resolved type, WITHOUT falling through. A resolved non-{@code Integer}/{@code BigDecimal} numeric (e.g. a
     * constrained {@code long}) yields {@code null} → decline, matching legacy's {@code UNKNOWN}.
     *
     * <p><strong>Structural arm (only when {@code type()} is MISSING).</strong> The lab engine returns MISSING for
     * any arithmetic with a MISSING operand type — notably an ALIAS operand (the ENGINE's shortcut type inference
     * is unimplemented, L-032; distinct from the alias IR node's #503 body-sourced {@code type()}, an adapter-side
     * attribute the engine read never sees) — so the compound day-count nodes
     * ({@code (daysInNonLeapPeriod / 365) + …}) have a MISSING {@code type()}. Legacy sidesteps this identically: its engine arm returns MISSING and it falls to its explicit
     * {@code RArithmeticExpr} arm — the recursive int/number JOIN of the operands ({@code BigDecimal} if EITHER is
     * {@code BigDecimal}, else {@code Integer}), declining ({@code null}) if either operand is unclassifiable
     * (the divide case never reaches this arm — it returned at the first arm). We mirror that arm here, recursing
     * through {@link #operandWitness} so a nested alias classifies via the resolver and deeper nesting composes.
     *
     * <p>The outer JOIN + same-kind coercion gate ({@link #emitArithmetic}) then treat the nested operand as
     * RESOLVED (its witness must equal the outer join), protecting byte-parity exactly as for a parameter / alias
     * operand. The nested node's own {@code MapperMaths.<R,O,O>method(...)} render — computed from ITS own operands,
     * with a null Java type → no coercion in operand position — is produced by the recursive {@link #emitArithmetic},
     * never read from {@code type()}.
     */
    private String nestedArithmeticWitness(BinaryOp nested, JavaTypeTranslator translator) {
        // Nested-divide arm FIRST (post-pin legacy #369, taught at #470): number unconditionally — the engine's
        // int/int join would mis-type it Integer and mis-join the enclosing operation. Legacy's earlyDiv arm
        // additionally guards `rawLeft() != null` (its unary-minus defense — a unary AST node reuses the
        // RArithmeticExpr shape with a null left); no unary op adapts to a BinaryOp, so the IR arm needs no
        // mirror of that guard (Seat-1 #470 OBS-4 — equivalent by construction, asymmetry annotated).
        if (nested.op() == BinaryOp.BinOp.DIV) {
            return "BigDecimal";
        }
        // Engine arm: a RESOLVED type is definitive (Integer/BigDecimal, else decline) — never fall through.
        if (nested.type() != null && !nested.type().isMissing() && nested.type().type() != null) {
            return numericWitness(nested, translator);
        }
        // Structural arm (type() MISSING): the recursive int/number JOIN of the operands.
        String left = operandWitness(nested.left(), translator);
        if (left == null) {
            return null;
        }
        String right = operandWitness(nested.right(), translator);
        if (right == null) {
            return null;
        }
        return "BigDecimal".equals(left) || "BigDecimal".equals(right) ? "BigDecimal" : "Integer";
    }

    /**
     * The arithmetic operand's Java numeric witness — {@code "Integer"} for Rune {@code int},
     * {@code "BigDecimal"} for Rune {@code number} (incl. number type-aliases, which the translator resolves),
     * or {@code null} when the operand has no resolved type or is non-numeric (string / date / …). Mirrors
     * legacy {@code HandlerHelper.numericOperandKind}'s int/number classification: a LITERAL carries its OWN
     * kind ({@code int} literal → {@code Integer}, {@code number} literal → {@code BigDecimal}; a
     * string/boolean literal is non-numeric → {@code null}), independent of context — the JOIN + expected-type
     * threading then renders it against the join type. A resolved operand is classified via its {@code type()}
     * through the translator's {@code int → Integer} / {@code number → BigDecimal} mapping.
     */
    /**
     * Whether {@code operand} is a numeric ({@code int}/{@code number}) literal — the comparison/equality
     * operand kind that re-renders against the sibling's numeric type (a boolean/string literal is non-numeric
     * and renders context-free). Mirrors the adapter's {@code isNumericLiteral} so the two gates agree.
     */
    private static boolean isNumericLiteralOperand(IRExpr operand) {
        return operand instanceof IRLiteral lit
                && (lit.literalKind() == IRLiteral.LiteralKind.INT
                        || lit.literalKind() == IRLiteral.LiteralKind.NUMBER);
    }

    private static String numericWitness(IRExpr operand, JavaTypeTranslator translator) {
        if (operand instanceof IRLiteral lit) {
            return switch (lit.literalKind()) {
                case INT -> "Integer";
                case NUMBER -> "BigDecimal";
                case STRING, BOOLEAN -> null; // a non-numeric literal — decline to legacy
            };
        }
        if (operand instanceof IRListOp listOp && listOp.op() == IRListOp.Kind.COUNT) {
            return "Integer"; // a count renders <chain>.resultCount() — a Java int → Integer (L-051), always
        }
        if (operand.type() == null || operand.type().isMissing() || operand.type().type() == null) {
            return null;
        }
        JavaClass<?> witness = translator.toJavaReferenceType(operand.type().type());
        if (witness == null) {
            return null;
        }
        String canonical = witness.getCanonicalName().withDots();
        if ("java.lang.Integer".equals(canonical)) {
            return "Integer";
        }
        if ("java.math.BigDecimal".equals(canonical)) {
            return "BigDecimal";
        }
        return null; // non-numeric operand — decline to legacy
    }

    /**
     * Wraps a bare enum-value operand in {@code MapperS.of(...)} for a comparison/equality — mirroring legacy
     * {@code HandlerHelper.wrapEnumOperand}, which wraps a witnessed enum constant ({@code EnumName.VALUE} — the
     * {@code REnumValue} render is UNWRAPPED by design, its consumers self-wrap) and passes everything already a
     * {@code Mapper} through unchanged. The IR knows the operand is an enum value STRUCTURALLY
     * ({@link IRReference.ReferenceKind#ENUM_VALUE}), and the emitted render carries its producer's witness
     * ({@link #emitEnumValue} builds every ENUM_VALUE render through {@code JavaExpression.enumConstant}), so the
     * wrap keys on the node kind AND that witness — {@code HandlerHelper.isBareEnumConstant}, the SAME predicate
     * every legacy consumer reads since PR #611 (LAW 77: one predicate at every claim seat on both routes); the
     * wrap factory adds {@code MAPPER_S} and preserves the enum import. A scalar / navigation / boolean-literal
     * operand (already a {@code MapperS.of(...)} render) is returned unchanged — the no-op arm. Only reached for
     * EQUALITY operands (the only gate admitting an enum value); logical operands are {@code ComparisonResult}s,
     * never enum values, so AND/OR never wrap. The {@code ENUM_VALUE} kind conjunct is kept beside the witness as
     * a DELIBERATE narrowing with no carrier: only {@link #emitEnumValue} mints the witness on this route, so the
     * two agree everywhere today; an IR emitter minting it under another kind would have to widen this seat to
     * legacy's witness-only wrap (LAW 77).
     */
    private static JavaStatementBuilder wrapEnumValueOperand(IRExpr node, JavaStatementBuilder emitted) {
        if (node instanceof IRReference ref
                && ref.referenceKind() == IRReference.ReferenceKind.ENUM_VALUE
                && emitted instanceof JavaExpression enumExpr
                && HandlerHelper.isBareEnumConstant(enumExpr)) {
            // Before PR #611 this seat re-read legacy's dotted-TEXT gate on the render, with a stale note that a
            // digit-leading value (PeriodEnum._15_MINUTES) failed it and so stayed unwrapped: PR #267 (A3) had
            // already admitted the `_<digit>` escape on both routes. The witness makes the spelling moot.
            return JavaExpression.wrappedInMapperSOf(enumExpr);
        }
        return emitted;
    }

    /**
     * Wraps a {@code count} comparison / equality operand ({@link IRListOp.Kind#COUNT}) in {@code MapperS.of(...)} —
     * mirroring legacy {@code ComparisonHandler.wrapCountOperand}. A count renders as the bare {@code int} chain
     * {@code <chain>.resultCount()} (a {@code null}-typed {@link JavaExpression} from {@link #emitNestedCount}), but a
     * comparison needs a {@code Mapper} operand; {@code MapperS.of(<chain>.resultCount())} is the golden form, and
     * {@link JavaExpression#wrappedInMapperSOf} adds the {@code MAPPER_S} ref + preserves the chain's refs/imports
     * exactly as legacy does. Reached only for an {@code EQ}/{@code NEQ}/{@code LT}/{@code GT}/{@code LTE}/{@code GTE}
     * operand (the only gates the adapter admits a count into — never a logical {@code and}/{@code or}); a non-count
     * operand (already a {@code MapperS.of(...)} render) returns unchanged. Mutually exclusive with
     * {@link #wrapEnumValueOperand} — a count is an {@link IRListOp}, never an enum reference — so the two never
     * double-wrap.
     */
    private static JavaStatementBuilder wrapCountValueOperand(IRExpr node, JavaStatementBuilder emitted) {
        if (node instanceof IRListOp listOp
                && listOp.op() == IRListOp.Kind.COUNT
                && emitted instanceof JavaExpression countExpr) {
            return JavaExpression.wrappedInMapperSOf(countExpr);
        }
        return emitted;
    }

    /**
     * Whether {@code enumNode} is an enum-value operand that legacy would REQUALIFY to a different enum class than
     * the IR renders — the inherited-enum case. An enum value declared on a SUPER-enum of the sibling operand's type
     * is rendered by legacy qualified with the sibling's (child) enum, because the generated Java flattens inherited
     * values under the child name (legacy {@code tryInheritedEnumRequalify}). The IR's enum leaf qualifies by the
     * node's {@code type()} ({@code emitEnumValue}): for a BARE value that is the parser's EXPECTED enum at the seat
     * — the same answer legacy's root arm gives since v3.1 flip seat 12 whenever its #215 same-instance gate passes
     * (before seat 12 legacy's root qualified by the value's DECLARING enum and only its sibling-rung ladders
     * recovered the child) — while for an EXPLICIT
     * {@code Enum -> Value} operand it is the author's enum, which legacy's #215 arm requalifies by the sibling, so
     * the two can still diverge there in both the constant qualifier and its import. Detect it by comparing the enum
     * operand's Java enum class to the sibling's; when they differ (or either is unresolvable) the equality DECLINES
     * to legacy. The common same-enum case ({@code op = SomeEnum.VALUE} over a {@code SomeEnum}-typed operand)
     * matches and drives. This is a Java-name decision (which enum class qualifies the constant), kept in the
     * emitter per the L-029 split.
     */
    private static boolean enumOperandRequalifies(IRExpr enumNode, IRExpr sibling, JavaTypeTranslator translator) {
        if (!(enumNode instanceof IRReference ref)
                || ref.referenceKind() != IRReference.ReferenceKind.ENUM_VALUE) {
            return false;
        }
        if (translator == null) {
            return true; // cannot compare the enum classes → decline (byte-safe)
        }
        JavaClass<?> enumClass = enumClassOf(enumNode, translator);
        JavaClass<?> siblingClass = enumClassOf(sibling, translator);
        return enumClass == null || siblingClass == null || !enumClass.equals(siblingClass);
    }

    /** The operand's Java enum/reference class via the translator, or {@code null} when its type is unresolved. */
    private static JavaClass<?> enumClassOf(IRExpr expr, JavaTypeTranslator translator) {
        if (expr.type() == null || expr.type().isMissing() || expr.type().type() == null) {
            return null;
        }
        return translator.toJavaReferenceType(expr.type().type());
    }

    /**
     * Comparison ({@code < > <= >=}) and equality ({@code = <>}): a static
     * {@code ExpressionOperatorsNullSafe} call {@code {method}(left, right,
     * CardinalityOperator.{default})}, with the operator-dependent cardinality default — {@code Any}
     * for {@code <>} ({@code NEQ}), {@code All} for every other operator (upstream
     * {@code defaultModifier = operator == "<>" ? ANY : ALL}). An explicit {@code all}/{@code any}
     * source modifier lowers to the DISTINCT {@code IRAllAnyCompare} kind since the #503 arm-A1
     * (oracle-served at its equality/comparison root, never routed here), so only the default
     * reaches this arm.
     * Contributes {@code CARDINALITY_OPERATOR} + the {@code EXPRESSION_OPERATORS_NULL_SAFE} static
     * wildcard, unioned with the operands'. No type witness, no translator call — the idiom is fixed.
     */
    private static Optional<JavaStatementBuilder> emitComparisonLike(BinaryOp.BinOp op,
                                                                     JavaStatementBuilder left,
                                                                     JavaStatementBuilder right) {
        String method = switch (op) {
            case LT -> "lessThan";
            case GT -> "greaterThan";
            case LTE -> "lessThanEquals";
            case GTE -> "greaterThanEquals";
            case EQ -> "areEqual";
            case NEQ -> "notEqual";
            case AND, OR, ADD, SUB, MUL, DIV ->
                    throw new IllegalArgumentException("not a comparison/equality op: " + op);
        };
        String cardinality = op == BinaryOp.BinOp.NEQ ? "Any" : "All";
        Set<JavaClass<?>> refs = new HashSet<>();
        refs.add(HandlerHelper.CARDINALITY_OPERATOR);
        refs.addAll(left.getRefs());
        refs.addAll(right.getRefs());
        Set<JavaClass<?>> staticWildcards = new HashSet<>();
        staticWildcards.add(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE);
        staticWildcards.addAll(left.getStaticWildcardImports());
        staticWildcards.addAll(right.getStaticWildcardImports());
        String code = method + "(" + HandlerHelper.render(left) + ", " + HandlerHelper.render(right)
                + ", CardinalityOperator." + cardinality + ")";
        return Optional.of(JavaExpression.from(code, null, refs, staticWildcards));
    }

    /**
     * Logical ({@code and}/{@code or}): a {@code ComparisonResult} instance-method chain
     * {@code left.{andNullSafe|orNullSafe}(right)} — byte-identical to
     * {@code LogicalHandler.handle(RLogicalExpr)}. Only a logical whose operands render to a
     * {@code ComparisonResult} natively (a comparison, equality or nested logical) REACHES this
     * arm — the #501–#504 operand admissions (the oracle-leaf kinds, the coerced boolean calls,
     * the point-free references) route their containing roots to the ORACLE serve before the
     * leaf walk ever runs, so no coercion is composed here, and the logical
     * itself imports nothing — the result unions ONLY the operands' refs/wildcards (the
     * {@code CardinalityOperator}/{@code ExpressionOperatorsNullSafe} arrive through them).
     */
    private static Optional<JavaStatementBuilder> emitLogical(BinaryOp.BinOp op,
                                                              JavaStatementBuilder left,
                                                              JavaStatementBuilder right) {
        String method = switch (op) {
            case AND -> "andNullSafe";
            case OR -> "orNullSafe";
            case LT, GT, LTE, GTE, EQ, NEQ, ADD, SUB, MUL, DIV ->
                    throw new IllegalArgumentException("not a logical op: " + op);
        };
        Set<JavaClass<?>> refs = new HashSet<>();
        refs.addAll(left.getRefs());
        refs.addAll(right.getRefs());
        Set<JavaClass<?>> staticWildcards = new HashSet<>();
        staticWildcards.addAll(left.getStaticWildcardImports());
        staticWildcards.addAll(right.getStaticWildcardImports());
        String code = HandlerHelper.render(left) + "." + method + "(" + HandlerHelper.render(right) + ")";
        return Optional.of(JavaExpression.from(code, null, refs, staticWildcards));
    }

    /**
     * Lowers an {@link IRReference}: {@code super.doEvaluate()} for {@code SUPER} (context-free),
     * {@code EnumName.CONSTANT} for {@code ENUM_VALUE}, and {@code aliasName(inputs)} for an {@code ALIAS}
     * operand via the {@link #aliasResolver} (set by the compiler). The {@code FUNCTION} kind returns empty
     * (legacy) until its increment.
     *
     * <p>An {@code ALIAS} reaches here as an arithmetic operand (L-035), a function-CALL argument (slice 2 —
     * {@code emitApply} recurses each arg through {@link #emit}, then the oracle appends {@code .get()}), an
     * EQUALITY/EXISTENCE operand (the #491 alias-operand teach — {@code emitBinaryOp}/{@code emitExistence}
     * recurse it here), or — since the #492 alias-nav teach — a NAVIGATION RECEIVER
     * ({@code emitFieldAccess} recurses the body-retyped {@code IRReference{ALIAS}} base, composing
     * {@code aliasName(inputs).<Leaf>map("getLeaf", bodyTypeVar -> bodyTypeVar.getLeaf())} — legacy's own
     * interior form; the retyped body type names the lambda var exactly as legacy's
     * {@code resolveReceiverDataType} walk does); a top-level alias is rendered by the compiler's dedicated
     * path before the generic emit, and the comparison gate still rejects an alias. Without a resolver — or
     * for an alias the enclosing function does not declare — it declines to legacy.
     */
    private Optional<JavaStatementBuilder> emitReference(IRReference reference, JavaTypeTranslator translator) {
        return switch (reference.referenceKind()) {
            case SUPER -> Optional.of(JavaExpression.from("super.doEvaluate()", null));
            case ENUM_VALUE -> emitEnumValue(reference, translator);
            case ALIAS -> emitAliasOperand(reference);
            // FUNCTION and RULE references render only as an IRApply callee (via emitApply / emitRuleDelegation),
            // never standalone, so a bare reference of either kind declines to legacy here.
            case FUNCTION, RULE -> Optional.empty();
        };
    }

    /**
     * Renders an {@code ALIAS} operand — {@code aliasName(input1, …)} via {@link #emitAlias} — using the
     * compiler-supplied {@link #aliasResolver} for the enclosing function's inputs + the dependency-collision
     * name. Declines (→ legacy) when no resolver is installed or the enclosing function does not declare the
     * alias. Byte-identical to the top-level alias path (the same {@link #emitAlias}), reused here for the
     * arithmetic-operand position (L-031's alias render carries a null expression type, so legacy inserts no
     * coercion in operand position — the render is context-free).
     */
    private Optional<JavaStatementBuilder> emitAliasOperand(IRReference alias) {
        if (aliasResolver == null) {
            return Optional.empty();
        }
        AliasOperandResolver.Facts facts = aliasResolver.factsFor(alias);
        if (facts == null) {
            return Optional.empty();
        }
        return Optional.of(emitAlias(alias.target(), facts.collidesWithDependency(), facts.enclosingInputNames()));
    }

    /**
     * An alias/shortcut invocation — byte-identical to the alias arm of
     * {@code ReferenceHandler.handle(RSymbolReference)}: {@code aliasName(input1, input2, …)}, the alias
     * helper-method call threading the enclosing function's inputs in declaration order. <strong>No
     * {@code MapperS.of} wrap and no refs</strong> — the alias method already returns a {@code Mapper}, and
     * the imports its declaration implies are contributed where that declaration is emitted (legacy
     * {@code AliasModel}), not at the invocation. The {@code aliasName} is the IR's alias name with a
     * {@code "1"} suffix iff it collides with a function dependency (legacy
     * {@code disambiguateAliasInvocation}); both that decision and the input list are JAVA-emission facts the
     * caller ({@link IRExpressionCompiler}) computes from the enclosing function and passes in — keeping the
     * input-threading out of the neutral IR (a functional target inlines the alias instead).
     *
     * @param aliasName              the neutral alias name (from the IR node)
     * @param collidesWithDependency whether the name collides with a function dependency → {@code "1"} suffix
     * @param enclosingInputNames    the enclosing function's input names, in declaration order
     */
    JavaStatementBuilder emitAlias(String aliasName, boolean collidesWithDependency,
                                   List<String> enclosingInputNames) {
        String invoked = collidesWithDependency ? aliasName + "1" : aliasName;
        String code = invoked + "(" + String.join(", ", enclosingInputNames) + ")";
        return JavaExpression.from(code, null, Set.of());
    }

    /**
     * The #381 W-facet alias invocation (the #531 teach — the LAST decline taught): an
     * OUTPUT-ROOTED disguised-chain alias threads the output builder as the FIRST argument and
     * wraps the Builder-returning call back into the Mapper world —
     * {@code MapperS.of(aliasName(outputName.toBuilder(), input1, …).build())} — byte-identical
     * to the aliasOutputBuilderNav arm of {@code ReferenceHandler.handle(RSymbolReference)}
     * (golden cdm5 {@code NewEquitySwapProduct} ×6 call sites, the census face
     * {@code wAlias.w.lowered.out:product.collide:0.inputs:2} — {@code declined} pre-teach,
     * the fate segment being the exit verdict). The {@code MAPPER_S} ref rides the
     * render exactly as legacy's arm attaches it ({@code Set.of(HandlerHelper.MAPPER_S)}); the
     * expression type stays null (legacy's own W-arm form — the enclosing composition reads
     * only the string + refs). The collision suffix is the SAME {@code "1"} law as
     * {@link #emitAlias}; the static-operator escape never reaches this render (the compiler
     * declines it FIRST at both channels — the #470 OBS-1 belt precedes the W-gate, so a
     * hypothetical escape+W alias falls whole to legacy, which renders the escape at every
     * seat).
     *
     * @param aliasName              the neutral alias name (from the IR node)
     * @param collidesWithDependency whether the name collides with a function dependency → {@code "1"} suffix
     * @param outputName             the enclosing function's declared output name (the builder receiver)
     * @param enclosingInputNames    the enclosing function's input names, in declaration order
     */
    JavaStatementBuilder emitAliasOutputBuilderNav(String aliasName, boolean collidesWithDependency,
                                                   String outputName, List<String> enclosingInputNames) {
        String invoked = collidesWithDependency ? aliasName + "1" : aliasName;
        String inputs = String.join(", ", enclosingInputNames);
        String code = "MapperS.of(" + invoked + "(" + outputName + ".toBuilder()"
                + (inputs.isEmpty() ? "" : ", " + inputs) + ").build())";
        return JavaExpression.from(code, null, Set.of(HandlerHelper.MAPPER_S));
    }

    /**
     * {@code EnumName.CONSTANT} for a bare enum-value reference — byte-identical to the bare-enum
     * arm of {@code ReferenceHandler.handle(RSymbolReference)} since v3.1 flip seat 12 whenever
     * legacy's #215 same-instance gate passes: BOTH then qualify by the node's INFERRED type, the
     * parser's EXPECTED enum at the seat (this emitter reads it off the IR node's {@code type()}
     * unconditionally; legacy reads {@code gm.workspace().getInferredType(expr)} and requires the
     * expected enum's hierarchy to flatten the EXACT bound value — where that gate declines [MISSING
     * inference, a non-enum type, a shadowed same-name value] legacy keeps the DECLARING enum while
     * this emitter still renders the node's type; before seat 12 legacy qualified by the value's
     * DECLARING enum and the two diverged wherever an inherited value sat at a child-typed seat, the
     * drr 7.x DTCC_OptionType / UnderlierIDOtherSourceLeg1 / MAS_BR_0052 carriers). The enum simple name is
     * the IR node's enum {@code type().name()} (an {@code REnumTypeRef}, whose {@code name()} is the
     * enumeration name); the constant is {@code formatEnumName(stripEscape(target))} on the raw
     * value name the adapter carried in {@link IRReference#target()}; the import is the translator's
     * mapping of that enum type (mirroring {@code enumImportRefs}'s
     * {@code toJavaReferenceType(REnumTypeRef)}). Declines (→ legacy) without a translator or enum
     * type.
     */
    private static Optional<JavaStatementBuilder> emitEnumValue(IRReference reference,
                                                                JavaTypeTranslator translator) {
        if (translator == null || reference.type() == null) {
            return Optional.empty();
        }
        RType enumType = reference.type().type();
        if (enumType == null) {
            return Optional.empty();
        }
        JavaClass<?> enumClass = translator.toJavaReferenceType(enumType);
        String constant = EnumHelper.formatEnumName(EnumHelper.stripEscape(reference.target()));
        return Optional.of(JavaExpression.enumConstant(enumType.name() + "." + constant, null,
                Set.of(enumClass), Set.of()));
    }

    /**
     * A bare function-parameter reference, byte-identical to the bare-variable arm of
     * {@code ReferenceHandler.handle(RSymbolReference)}:
     * <ul>
     *   <li><strong>scalar</strong> ({@code SINGLE}) → {@code MapperS.of(name)} via
     *       {@code wrappedInMapperSOf(from(name, null, Set.of()))} — no refs (the parameter's type
     *       import is contributed at the method signature, not the reference);</li>
     *   <li><strong>multi</strong> ({@code MULTI}, {@code (0..*)}/{@code (1..*)}) → the witnessed
     *       {@code MapperC.<Item>of(name)} via {@code wrappedInMapperCOfSingle(from(name), …, witness)},
     *       where the {@code <Item>} witness is the node's item {@code type()} mapped through the
     *       {@link JavaTypeTranslator} — mirroring {@code ReferenceHandler.mapperCOfWrapWitness}'s
     *       {@code toJavaReferenceType(itemType)}. {@code MapperC} + the witness class enter refs via
     *       the wrap factory.</li>
     * </ul>
     * A {@code MULTI} param whose witness cannot be named (no translator, or a missing item type)
     * declines to legacy; the rare witness/output simple-name collision renders the witness FQN-inline
     * with its import suppressed ({@link #collisionFqn} against {@code outputType}, mirroring legacy
     * {@code ReferenceHandler.mapperCWitnessOutputCollisionFqn}). Only
     * {@link IRVariable.VariableKind#PARAM} is emitted — every other binder kind (the implicit
     * {@code item}, aliases, closure/let binders) returns empty so it stays on the legacy handler.
     */
    private Optional<JavaStatementBuilder> emitVariable(IRVariable variable, JavaTypeTranslator translator,
                                                        JavaClass<?> outputType) {
        // The implicit `item` of a filter/extract lambda (the percolation slice): a typed, non-synthetic
        // USER_ITEM renders as the lambda-bound parameter, mirroring legacy
        // ReferenceHandler.handle(RImplicitVariable). Reached wherever the adapter admits a USER_ITEM — an
        // item-nav FieldAccess receiver (isFilterExtractBoundItemReceiver), a bare-item check OPERAND, a
        // call ARG (isFilterExtractBoundItemOperand), or — since the #491 teach — the DIRECT visit-seat
        // claim ROOT (visitImplicitVariable's tryEmitFromIR: the adapter's unconditional lowering, ANY
        // binding context — the renderer serves the binding decision, so the filter/extract framing below
        // no longer bounds this arm's reach); then/switch/sort/reduce-bound items decline at the COMPOSED
        // adaptation seats as before (the #469 Seat-1 OBS-4 comment heal — the former "receiver-only"
        // claim predated the operand/arg admission slices). The non-MISSING-type guard declines the
        // untyped population honestly (at the #491 visit seat: the render-time-minted implicits the fixed
        // point never typed — the probe's typeMissing residue).
        if (variable.variableKind() == IRVariable.VariableKind.USER_ITEM) {
            if (variable.type() == null || variable.type().isMissing()) {
                return Optional.empty();
            }
            // The #469 itemCallArg teach: an installed renderer supplies the LIVE scope binding (named
            // extract param / collision-escaped `_item` / plain `item`) via legacy handle(RImplicitVariable)
            // verbatim — byte-identical to the fallback by construction. A null RENDERER keeps the
            // historical bare render (standalone-emitter contexts); a null RESULT declines (an
            // uncorrelated variable — never guess a binding).
            if (implicitItemRenderer != null) {
                return Optional.ofNullable(implicitItemRenderer.render(variable));
            }
            return Optional.of(JavaExpression.from(variable.name(), null));
        }
        // The #479 filter/extract synthetic-item receiver teach: a SYNTHETIC elided-operand item
        // reaches here through the adapter's boundary RETYPE (a typed, non-meta, filter/extract-bound
        // synthetic item) or — since the #491 teach — as the DIRECT visit-seat claim ROOT (the plain
        // unconditional lowering, cache-typed with no retype: the type gate below is then the ONLY
        // typing filter, declining the render-time mints the fixed point never typed), and renders
        // ONLY via the oracle renderer (legacy handle(RImplicitVariable) verbatim on the
        // range-correlated node — the live binding is context-dependent by definition, so there is
        // NO bare fallback: a null renderer or an uncorrelated/ambiguous range declines the claim).
        if (variable.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM) {
            if (variable.type() == null || variable.type().isMissing()) {
                return Optional.empty(); // defence in depth: only the adapter's retyped item renders
            }
            if (syntheticItemRenderer != null) {
                return Optional.ofNullable(syntheticItemRenderer.render(variable));
            }
            return Optional.empty();
        }
        if (variable.variableKind() != IRVariable.VariableKind.PARAM) {
            return Optional.empty();
        }
        // facet aliasCallInputEscape (v3.1 flip seat 32, law E.2): a bare PARAM read renders the
        // EMITTED input name. A null hook (no escaping input) and the identity arm both hand back
        // the raw name — today's bytes for every non-escaping function.
        String renderName = inputEscape == null ? variable.name() : inputEscape.apply(variable.name());
        JavaExpression innerName = JavaExpression.from(renderName, null, Set.of());
        if (variable.cardinality() == ExpressionCardinality.MULTI) {
            if (translator == null || variable.type() == null || variable.type().isMissing()) {
                return Optional.empty(); // cannot name the MapperC witness — defer
            }
            RType itemType = variable.type().type();
            if (itemType == null) {
                return Optional.empty();
            }
            JavaClass<?> witness = translator.toJavaReferenceType(itemType);
            if (witness == null) {
                return Optional.empty();
            }
            // Witness/output collision → FQN-inline witness + suppressed import (the 4-arg wrap handles
            // both: fqn != null renders MapperC.<a.b.C>of and drops the witness import; null → bare +
            // import). Byte-identical to legacy tryMultiValueWrap's mapperCWitnessOutputCollisionFqn arm.
            String fqn = collisionFqn(witness, outputType);
            if (fqn != null) {
                collisionFqnRenders++; // §4.2 instrumentation: an IR-produced cross-namespace FQN-inline render
            }
            return Optional.of(JavaExpression.wrappedInMapperCOfSingle(innerName, null, witness, fqn));
        }
        return Optional.of(JavaExpression.wrappedInMapperSOf(innerName));
    }

    /** {@code MapperS.of("<escaped>")} — mirrors {@code LiteralHandler.handle(RStringLiteral)}. */
    private static JavaStatementBuilder emitString(String value) {
        String escaped = escapeJavaString(value);
        return JavaExpression.wrappedInMapperSOf(JavaExpression.from("\"" + escaped + "\"", null));
    }

    /** {@code MapperS.of(true|false)} — mirrors {@code LiteralHandler.handle(RBooleanLiteral)}. */
    private static JavaStatementBuilder emitBoolean(boolean value) {
        return JavaExpression.wrappedInMapperSOf(JavaExpression.from(value ? "true" : "false", null));
    }

    /** {@code MapperS.of(new BigDecimal("<plain>"))} — mirrors {@code LiteralHandler.handle(RNumberLiteral)}. */
    private static JavaStatementBuilder emitNumber(BigDecimal value) {
        return JavaExpression.wrappedInMapperSOf(JavaExpression.from(
                "new BigDecimal(\"" + value.toPlainString() + "\")", null, Set.of(HandlerHelper.BIG_DECIMAL)));
    }

    /**
     * The in-{@code long}-range arms of {@code LiteralHandler.handle(RIntLiteral)}, rendered by LiteralHandler
     * ITSELF - ONE method per arm for both routes (LAW 77 by identity): {@link LiteralHandler#renderIntValueForBigDecimal}
     * in a {@code BigDecimal} target context ({@code BigDecimal.valueOf(N)} / {@code BigDecimal.valueOf(Nl)}, lowercase
     * {@code l} on the long band) and {@link LiteralHandler#intLiteralCode} for the bare int form. v3.2 seat 13: the
     * mirrored copy of the int arm here carried an uppercase {@code L} on the long band, and the seat's M5c heal of
     * the default route's suffix left this route 13 rows behind until the copy was replaced by the call (commit 4);
     * #634 round 1 replaced the BigDecimal arm's mirror the same way (the code-quality seat's SF-2). Returns empty for
     * beyond-long values (bit length &gt; 63): the {@code BigDecimal}-context form hoists a {@code BigInteger} local
     * and the int-context form uses a {@code BigInteger} constructor - both deferred to the legacy handler.
     */
    private static Optional<JavaStatementBuilder> emitInt(BigInteger value, ExpressionContext ctx,
                                                          JavaTypeUtil typeUtil) {
        if (value.bitLength() > 63) {
            return Optional.empty();
        }
        if (isBigDecimalContext(ctx, typeUtil)) {
            String valueCode = LiteralHandler.renderIntValueForBigDecimal(value);
            Set<JavaClass<?>> innerRefs = new HashSet<>();
            innerRefs.add(HandlerHelper.BIG_DECIMAL);
            return Optional.of(JavaExpression.wrappedInMapperSOf(JavaExpression.from(valueCode, null, innerRefs)));
        }
        String valueCode = LiteralHandler.intLiteralCode(value);
        return Optional.of(JavaExpression.wrappedInMapperSOf(JavaExpression.from(valueCode, null)));
    }

    /**
     * Whether the expected Java target type is {@code BigDecimal} (Rune {@code number}) —
     * directly or as the item type of a {@code MapperS}/{@code MapperC} wrapper. Byte-identical
     * to {@code LiteralHandler.isBigDecimalContext}. Declines (false) without type info.
     */
    private static boolean isBigDecimalContext(ExpressionContext ctx, JavaTypeUtil typeUtil) {
        if (typeUtil == null || ctx == null) {
            return false;
        }
        JavaType expectedType = ctx.expectedType();
        if (expectedType == null) {
            return false;
        }
        if (typeUtil.isBigDecimal(expectedType)) {
            return true;
        }
        if (typeUtil.isWrapper(expectedType)) {
            JavaType itemType = typeUtil.getItemType(expectedType);
            return itemType != null && typeUtil.isBigDecimal(itemType);
        }
        return false;
    }

    /**
     * Java string-literal escaping — v3.1 flip seat 22 (facet {@code labelUnicodeEscape}, LAW 69 /
     * LAW 77): THE ONE escape, {@link JavaStringUtil#escapeJava(String)} (commons-text 1.12.0
     * {@code StringEscapeUtils.escapeJava} byte-for-byte — the control-char table and the
     * {@code \\uXXXX} leg for every unit outside {@code 0x20..0x7f}), the same method the legacy
     * {@code LiteralHandler.handle(RStringLiteral)} and the template emitters consult, so the two
     * routes cannot disagree on a literal. The five-case private mirror that lived here is retired.
     */
    private static String escapeJavaString(String raw) {
        return JavaStringUtil.escapeJava(raw);
    }
}
