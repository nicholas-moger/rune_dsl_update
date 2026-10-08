package com.regnosys.rosetta.generator.java.scoping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.expressions.binary.RThenExpr;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.generator.GeneratedIdentifier;
import com.regnosys.rosetta.generator.GeneratorScope;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;

/**
 * Statement-level scope. Supports lambda sub-scopes for variable
 * isolation in functional expressions.
 */
public class JavaStatementScope extends AbstractJavaScope<AbstractJavaScope<?>> {

    // PR #98 (facet F6 then-extract) + PR #99 (chained thens): the implicit-input rebinding
    // for `then` bodies. When a then-extract is rendered (FunctionExpressionRenderer.
    // renderThenExtractSet), each then-function's implicit input must resolve to the hoisted
    // `thenArg`/`thenArgN` local for ITS level rather than a fresh `MapperS.of(input)`/`item`.
    // Carried on the SCOPE (not ExpressionContext) so it survives the 3-arg
    // ExpressionCompiler.compile(expr, type, scope) calls that handlers use for sub-expressions
    // (those rebuild the context but thread the scope). Keyed (by identity) on the boundary
    // then-function so (a) a chained `a then b then c` can bind each body to its own preceding
    // thenArg, and (b) a nested lambda inside a body — which matches a different inline function
    // — keeps its own `item`. Fork analogue of upstream createKeySynonym(function.
    // implicitVarInContext, thenArgCode), of which a chain emits one per then.
    private final Map<RInlineFunction, JavaExpression> thenArgBindings = new IdentityHashMap<>();

    public JavaStatementScope(String description, AbstractJavaScope<?> parent) {
        super(description, parent);
    }

    /** Create a child scope for a lambda body. */
    public JavaStatementScope lambdaScope() {
        JavaStatementScope child = childScope("Lambda[]");
        child.lambdaBoundary = true;
        return child;
    }

    /**
     * Create the statement-body child scope (facet meta_coercion_numbering) —
     * the fork analogue of upstream {@code assignOutputScope.bodyScope}.
     * Renderer-seeded names (inputs / output / aliases) live on the parent
     * scope; guarded coercion params register HERE, so a param sharing a
     * seed's name ESCAPES against the parent (group-of-one + parent-taken →
     * {@code _name}) instead of numbering with it — seeds and params are
     * separate {@code computeActualNames} groups exactly as upstream's
     * two-level method/body scope split resolves them.
     */
    public JavaStatementScope bodyScope() {
        return childScope("Body[]");
    }

    private JavaStatementScope childScope(String description) {
        return new JavaStatementScope(description, this);
    }

    /**
     * Bind a {@code then} body's implicit input (the {@code boundary} inline function's
     * implicit parameter) to the hoisted {@code thenArg}/{@code thenArgN} reference for that
     * level. May be called once per then in a chain (each {@code boundary} is distinct); a
     * later binding for the same boundary overwrites the earlier. See the field comment.
     */
    public void bindThenArg(RInlineFunction boundary, JavaExpression ref) {
        this.thenArgBindings.put(boundary, ref);
    }

    // facet lambdaItemReceiverType (v3.1 flip seat 32, law A.2): the RECEIVER-RENDER typing
    // channel. A block lambda's implicit item has exactly one typed channel today - the
    // thenArg binding above, which REPLACES the render with the hoisted local. A lambda whose
    // receiver is an INLINE chain gets neither the local nor the type, so its item compiles
    // TYPE-LESS and every compiled-type-gated consumer downstream (the #295 deref pass, the
    // meta coercion at the nav seat) is blind - the F1 blind spot the seat-30 probe measured
    // ([PROBE30-NAVRECV] nullTyped=true) and the GUPIL1-vs-UPI minimal pair isolated: same
    // feature, same wrapper, same next hop, the ONLY difference whether the receiver was
    // bound through a typed local. This channel carries the RECEIVER's compiled ITEM TYPE
    // (bound at CollectionHandler's extract seat, where the receiver compile is in scope)
    // keyed by the lambda boundary, so the item render keeps its NAME and gains its TYPE.
    // Seat 32 binds a META WRAPPER item ONLY (the narrowing at the bind site): a bare item
    // type tells the nav seat nothing it did not already assume and merely closes
    // NavigationHandler's null-typed recovery gate - the measured seat-31 regression on the
    // mas FixedFloatRateLeg1/2Rule + InterestRatePriceRule trio.
    private final Map<RInlineFunction, com.rosetta.util.types.JavaType>
            lambdaItemTypes = new IdentityHashMap<>();

    /** Bind the enclosing extract's compiled receiver ITEM type for {@code boundary}'s item. */
    public void bindLambdaItemType(RInlineFunction boundary,
            com.rosetta.util.types.JavaType itemType) {
        this.lambdaItemTypes.put(boundary, itemType);
    }

    /**
     * The receiver item type bound for {@code boundary}, walking this scope and its
     * ancestors exactly like {@link #thenArgRefFor}; {@code null} if none.
     */
    public com.rosetta.util.types.JavaType lambdaItemTypeFor(
            RInlineFunction boundary) {
        GeneratorScope<?> s = this;
        while (s != null) {
            if (s instanceof JavaStatementScope js) {
                com.rosetta.util.types.JavaType t =
                        js.lambdaItemTypes.get(boundary);
                if (t != null) {
                    return t;
                }
            }
            s = s.getParent();
        }
        return null;
    }

    // facet switchChoiceHoist (PR #221): the switch-subject rebinding for a CHOICE/TYPE-keyed
    // switch CASE body rendered as the upstream `instanceof` block-hoist. The switch subject
    // (the implicit `item`, narrowed to the case type) must resolve to the cast case var
    // (`final <CaseType> <caseVar> = (<CaseType>) switchArgument;`) inside that case's body,
    // NOT a fresh `item`. Keyed (by identity) on the boundary RSwitchExpr so a nested switch or
    // a nested lambda inside a case body — whose nearest binding boundary differs — keeps its
    // own resolution. The value is rebound per case (each case narrows to a different type), so
    // the renderer compiles each case body in a child scope carrying that case's binding. The
    // direct analogue of {@link #bindThenArg} for the switch subject.
    private final Map<com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr, JavaExpression>
            switchSubjectBindings = new IdentityHashMap<>();

    /**
     * Bind a choice-type switch CASE body's implicit subject (the {@code boundary} switch's
     * narrowed {@code item}) to the cast case-var reference for the case being rendered. See
     * the field comment; the analogue of {@link #bindThenArg}.
     */
    public void bindSwitchSubject(
            com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr boundary, JavaExpression ref) {
        this.switchSubjectBindings.put(boundary, ref);
    }

    /**
     * Resolve the cast case-var reference bound for {@code boundary}, walking this scope and its
     * ancestors; {@code null} if none. Used by {@code ReferenceHandler.handle(RImplicitVariable)}
     * to re-root a choice-type switch case body's implicit subject onto the cast case var — only
     * when the switch IS the nearest binding boundary (a closer lambda owns its own {@code item}).
     */
    public JavaExpression switchSubjectRefFor(
            com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr boundary) {
        GeneratorScope<?> s = this;
        while (s != null) {
            if (s instanceof JavaStatementScope js) {
                JavaExpression ref = js.switchSubjectBindings.get(boundary);
                if (ref != null) {
                    return ref;
                }
            }
            s = s.getParent();
        }
        return null;
    }

    /**
     * v3.2 seat 7 (F11, LAW 77): the bare text of the switch-subject binding {@code receiver} IS — the IR leaf emitter's
     * structural read of "this hop is off the bound case local", the twin of the default route's
     * {@code NavigationHandler.activeSwitchSubjectBoundText} (which walks the AST to the binding boundary; the emitter
     * holds the emitted receiver instead). IDENTITY alone: {@code ReferenceHandler.handle(RImplicitVariable)} returns
     * the bound ref object itself on both routes (the IR route reaches a bound implicit item through the legacy
     * renderer's {@code switchSubjectRefFor} arm), so the receiver either IS one of the LIVE switch-subject bindings of
     * this scope chain — every {@code bindSwitchSubject} site's, the in-lambda seats' and the SET seats' alike — or the
     * read answers {@code null} and the emitter's registry answer stands. Round 1 (the code-quality review's SF-3): the
     * first cut kept a SECOND leg, a rendered-string equality of the receiver's bare text against each live binding's —
     * UNWITNESSED (the identity leg fires on every carrier: control2b and the twelve chaos rows on the IR route; lane G1,
     * the whole consult removed, is red on control2b alone) and a text compare this javadoc itself denied — DELETED,
     * measured by the seat suites ON, the chaos D11 on both routes and the chain at the round-1 head. The s7a chain's
     * catch is why there is no text gate at all: a receiver-text equality fired on every input named after its type
     * (nine plain hops rendered {@code __trade} / {@code _party}).
     *
     * @return the binding's bare local (the #221 {@code MapperS.of(<caseVar>)} wrap unwrapped through the factory marker,
     *         or the option-getter form's Mapper-typed local as is), or {@code null} when {@code receiver} is not a live
     *         switch-subject binding of this scope chain
     */
    public String boundSwitchSubjectText(
            com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder receiver) {
        if (!(receiver instanceof JavaExpression recv)) {
            return null;
        }
        GeneratorScope<?> s = this;
        while (s != null) {
            if (s instanceof JavaStatementScope js) {
                for (JavaExpression bound : js.switchSubjectBindings.values()) {
                    if (bound == recv) {
                        return bareText(bound);
                    }
                }
            }
            s = s.getParent();
        }
        return null;
    }

    private static String bareText(JavaExpression e) {
        com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder inner =
                e.unwrapToBuilder().orElse(e);
        return inner instanceof JavaExpression ie ? ie.renderToString() : null;
    }

    // facet evaluate_arg_consumption (arm B3): hoisted meta-deref local
    // declarations registered by a NESTED-operand function call inside an
    // extract/map lambda — too deep for the JavaBlockBuilder return route
    // (intermediate operand consumers render eagerly and accept only
    // JavaExpression). CollectionHandler.compileLambda drains this scope's
    // pendings after compiling the body and prepends them as the block-lambda
    // leading statements — the fork analogue of upstream's JavaStatementBuilder
    // composition floating declarations up to the enclosing lambda block.
    // Per-SCOPE (not root): each lambda drains exactly the hoists its own body
    // compile registered; a discarded compile attempt's registrations stay on
    // the attempt's sibling scope, never rendered.
    private List<com.regnosys.rosetta.generator.java.statement.JavaStatement> pendingLambdaHoists;

    /** Register a hoisted declaration for the lambda whose body compiles against this scope. */
    public void registerPendingLambdaHoist(
            com.regnosys.rosetta.generator.java.statement.JavaStatement decl) {
        if (pendingLambdaHoists == null) {
            pendingLambdaHoists = new ArrayList<>();
        }
        pendingLambdaHoists.add(decl);
    }

    /** True when {@link #registerPendingLambdaHoist} registrations are awaiting a drain. */
    public boolean hasPendingLambdaHoists() {
        return pendingLambdaHoists != null && !pendingLambdaHoists.isEmpty();
    }

    /** Return and clear this scope's pending lambda hoists (empty list when none). */
    public List<com.regnosys.rosetta.generator.java.statement.JavaStatement> drainPendingLambdaHoists() {
        if (pendingLambdaHoists == null) {
            return List.of();
        }
        List<com.regnosys.rosetta.generator.java.statement.JavaStatement> drained = pendingLambdaHoists;
        pendingLambdaHoists = null;
        return drained;
    }

    /**
     * facet inLambdaArgSeatIteHoist (PR #355): the nearest ancestor-or-self scope a
     * {@code compileLambda} drain reads — the {@code lambdaScope()} child marked
     * {@code lambdaBoundary}. A producer registering from a DESCENDANT compile scope
     * must register HERE or the drain misses the hoist (the #312 producer registered
     * on the seat scope directly, correct for its direct-body seats; the arg-seat
     * ite-hoist's seats sit arbitrarily deep). Returns {@code null} outside any
     * lambda (the producer then declines).
     */
    public JavaStatementScope findPendingLambdaHoistBoundary() {
        GeneratorScope<?> s = this;
        while (s != null) {
            if (s instanceof JavaStatementScope js && js.lambdaBoundary) {
                return js;
            }
            s = s.getParent();
        }
        return null;
    }

    /**
     * facet inLambdaArgSeatIteHoist (PR #355): the current pending-lambda-hoist
     * registration count — pair with {@link #drainPendingLambdaHoistsSince(int)} so an
     * outer hoist's arm compile drains exactly the interior hoists it registered
     * (the arm-interior relocation window, the sink-channel
     * {@code statementHoistMark}/{@code drainStatementHoistsSince} law on this channel).
     */
    public int pendingLambdaHoistMark() {
        return pendingLambdaHoists == null ? 0 : pendingLambdaHoists.size();
    }

    /**
     * facet inLambdaArgSeatIteHoist (PR #355): return and remove the pending lambda
     * hoists registered at or after {@code mark} (empty list when none) — the
     * arm-interior window drain. Registrations before {@code mark} stay pending for
     * the lambda-top drain.
     */
    public List<com.regnosys.rosetta.generator.java.statement.JavaStatement>
            drainPendingLambdaHoistsSince(int mark) {
        if (pendingLambdaHoists == null || pendingLambdaHoists.size() <= mark) {
            return List.of();
        }
        List<com.regnosys.rosetta.generator.java.statement.JavaStatement> tail =
                new ArrayList<>(pendingLambdaHoists.subList(mark, pendingLambdaHoists.size()));
        pendingLambdaHoists.subList(mark, pendingLambdaHoists.size()).clear();
        return tail;
    }

    /**
     * facet ctorSetterNumericNarrowChain (PR #361): insert pending lambda hoists at
     * {@code index} — the pending-channel mirror of {@link #insertStatementHoists}
     * (the #327 hoist-reorder law on this channel: a ctor pair's coercion-phase
     * hoists relocate to the pair's CONSUMPTION position, interleaving each numeric
     * value local right after its own pair's compile-phase thenArg statements —
     * golden iosco PeriodicPaymentRule `…thenArg4; bigDecimal0; …thenArg5;
     * bigDecimal1;`). An out-of-range index clamps to append (defensive — marks are
     * always recorded on this same list).
     */
    public void insertPendingLambdaHoists(int index,
            List<com.regnosys.rosetta.generator.java.statement.JavaStatement> stmts) {
        if (stmts == null || stmts.isEmpty()) {
            return;
        }
        if (pendingLambdaHoists == null) {
            pendingLambdaHoists = new ArrayList<>();
        }
        int at = Math.max(0, Math.min(index, pendingLambdaHoists.size()));
        pendingLambdaHoists.addAll(at, stmts);
    }

    /**
     * facet inLambdaArgSeatIteHoist (PR #355): the nearest enclosing statement-hoist
     * SESSION, walking through lambda boundaries — the in-lambda naming channel.
     * {@link #findStatementHoistSink()} deliberately stops at a lambda boundary
     * (hoist STATEMENTS must not escape the lambda); the NAMING session is
     * method-spanning by construction (upstream resolves every hoist local through
     * ONE method scope), so the in-lambda ite-hoist reads it across boundaries for
     * its per-lambda sub-groups ({@code StatementHoistSession.registerLambdaScoped}).
     * {@code null} on session-less paths (POJO-condition / alias-body emission
     * without a session) — the producer then declines.
     */
    public com.regnosys.rosetta.generator.java.function.StatementHoistSession
            findStatementHoistSessionAnyDepth() {
        GeneratorScope<?> s = this;
        while (s != null) {
            if (s instanceof JavaStatementScope js && js.statementHoistSession != null) {
                return js.statementHoistSession;
            }
            s = s.getParent();
        }
        return null;
    }

    /**
     * facet inline_then_hoist (PR #219): suppression depth for the deep-position thenArg
     * hoist ({@code CollectionHandler.tryDeepThenHoist}). Raised while compiling a
     * {@code then}'s ARGUMENT (a chain link) so an INNER then of a chain whose OUTERMOST then
     * is non-hoistable (the whole chain renders inline, matching {@code renderThenExtractSet}'s
     * all-or-nothing decline) does not independently hoist an implicit SUB-chain.
     */
    private int thenHoistSuppression;

    /** Suppress the deep thenArg hoist for the next argument compile (chain-link). */
    public void pushThenHoistSuppression() {
        thenHoistSuppression++;
    }

    /** Lift one level of deep-thenArg-hoist suppression. */
    public void popThenHoistSuppression() {
        if (thenHoistSuppression <= 0) {
            // fail-fast on push/pop imbalance: an underflow would silently disable
            // suppression and let the deep thenArg hoist fire on a chain-link (Copilot R1).
            throw new IllegalStateException(
                    "popThenHoistSuppression() without a matching pushThenHoistSuppression()");
        }
        thenHoistSuppression--;
    }

    /** True when a deep thenArg hoist is suppressed on this scope or an ancestor. */
    public boolean isThenHoistSuppressed() {
        GeneratorScope<?> s = this;
        while (s != null) {
            if (s instanceof JavaStatementScope js && js.thenHoistSuppression > 0) {
                return true;
            }
            s = s.getParent();
        }
        return false;
    }

    /**
     * facet deep_value_then_hoist (PR #250): suppression depth for the deep-position
     * VALUE-then hoist ({@code CollectionHandler.tryDeepThenHoist}'s value-output arm).
     * Raised by {@code ControlFlowHandler} while compiling an INLINE ternary
     * ({@code condition.getOrDefault(false) ? thenExpr : elseExpr}) or inline chained-ternary
     * switch ({@code Objects.equals(g, arg) ? r : …} - a form never written since v3.2 seat 12:
     * the suppression is pushed BEFORE the switch's case loop, which runs under it for the per-case
     * refusal's precedence, then the seat refuses at {@code SWITCH_TERNARY_STUB}) — the cascade-UNSAFE
     * seats where
     * hoisting a {@code final Mapper*<X> thenArg = …;} decl out of an inline arm would force
     * the conditional into an if/else block and renumber co-resident hoist groups (the #219
     * regression of 59 conditional-restructure lines). Distinct from
     * {@link #thenHoistSuppression} (which suppresses ALL deep then-hoists on a chain-link
     * argument): this suppresses ONLY the value-output arm of the deep hoist, leaving the
     * ComparisonResult-output (boolean-context) hoist active, since that one renders inside
     * the conditional's OWN already-block paths and is independently green-safe.
     */
    private int thenValueHoistSuppression;

    /** Suppress the deep VALUE-then hoist while compiling an inline ternary (the conditional's) or a switch arm
     * (the residual path's case loop, whose product the seat discards before its {@code SWITCH_TERNARY_STUB}
     * refusal since v3.2 seat 12). */
    public void pushThenValueHoistSuppression() {
        thenValueHoistSuppression++;
    }

    /** Lift one level of deep VALUE-then-hoist suppression. */
    public void popThenValueHoistSuppression() {
        if (thenValueHoistSuppression <= 0) {
            // fail-fast on push/pop imbalance: an underflow would silently re-enable the
            // value-then hoist inside an inline ternary arm or the residual switch path's case loop
            // (the cascade seat; the switch's inline form a SWITCH_TERNARY_STUB refusal since v3.2 seat 12).
            throw new IllegalStateException(
                    "popThenValueHoistSuppression() without a matching pushThenValueHoistSuppression()");
        }
        thenValueHoistSuppression--;
    }

    /** True when the deep VALUE-then hoist is suppressed on this scope or an ancestor. */
    public boolean isThenValueHoistSuppressed() {
        GeneratorScope<?> s = this;
        while (s != null) {
            if (s instanceof JavaStatementScope js && js.thenValueHoistSuppression > 0) {
                return true;
            }
            s = s.getParent();
        }
        return false;
    }

    private int existsMetaWrapSuppression;

    /**
     * facet existsOperandMetaWrap (PR #315): suppress the exists-operand meta-wrap
     * ({@code ReferenceHandler.renderImplicitRuleInvocation}'s #315 arm) while re-compiling a
     * conditional map-lambda body whose BLOCK form DECLINED (a co-occupied carrier — a multi-line
     * ctor arm etc.). Golden always block-renders a #315 meta-wrap exists (the value hoist needs a
     * block), so an inline ternary can NEVER byte-match golden with the wrap — suppressing it keeps
     * the fall-through inline ternary at its clean pre-#315 form (no within-waiver regression).
     */
    public void pushExistsMetaWrapSuppression() {
        existsMetaWrapSuppression++;
    }

    /** Lift one level of exists-operand meta-wrap suppression. */
    public void popExistsMetaWrapSuppression() {
        if (existsMetaWrapSuppression <= 0) {
            throw new IllegalStateException(
                    "popExistsMetaWrapSuppression() without a matching pushExistsMetaWrapSuppression()");
        }
        existsMetaWrapSuppression--;
    }

    /** True when the exists-operand meta-wrap is suppressed on this scope or an ancestor. */
    public boolean isExistsMetaWrapSuppressed() {
        GeneratorScope<?> s = this;
        while (s != null) {
            if (s instanceof JavaStatementScope js && js.existsMetaWrapSuppression > 0) {
                return true;
            }
            s = s.getParent();
        }
        return false;
    }

    private int metaLeafBaresymAllowed;

    /**
     * facet conditionalMetaJoin (PR #295): allow a meta-leaf bare-FUNCTION navigation
     * (a disguised {@code func -> metaFeature}, normally DECLINED by the #280 meta-leaf
     * gate) to render its meta WRAPPER while compiling the arms of a MIXED-baresym
     * conditional ladder — where {@code CollectionHandler.compileLadderConditionalBlock}
     * has already proven a sibling baresym arm navigates the BARE (non-meta) leaf, so the
     * meta-join is the bare value and the wrapper arm will be deref'd by the caller.
     * Outside such a ladder the #280 decline stands (the wrapper would leak / regress a
     * getOrDefault operand).
     */
    public void pushMetaLeafBaresymAllowed() {
        metaLeafBaresymAllowed++;
    }

    /** Lift one level of meta-leaf bare-FUNCTION-nav allowance. */
    public void popMetaLeafBaresymAllowed() {
        if (metaLeafBaresymAllowed <= 0) {
            throw new IllegalStateException(
                    "popMetaLeafBaresymAllowed() without a matching pushMetaLeafBaresymAllowed()");
        }
        metaLeafBaresymAllowed--;
    }

    /** True when a meta-leaf bare-FUNCTION nav may render its wrapper (this scope or an ancestor). */
    public boolean isMetaLeafBaresymAllowed() {
        GeneratorScope<?> s = this;
        while (s != null) {
            if (s instanceof JavaStatementScope js && js.metaLeafBaresymAllowed > 0) {
                return true;
            }
            s = s.getParent();
        }
        return false;
    }

    private int ladderJoinBareArmReplay;

    /**
     * facet drainConsumerChainTypeStamp (PR #387): raised while
     * {@code CollectionHandler.compileLadderConditionalBlock}'s join-bare REPLAY pass
     * recompiles the ladder's arms (the #375-C2 text-order route) — the ONLY window in
     * which the extract assembly may stamp a thenArg-bound {@code mapItem} consumer
     * chain's type from the #237 terminal walker, so the #295 compiled-type-gated leaf
     * deref reaches the then-chain arms golden derefs BARE. Outside the replay the
     * assembly keeps the null chain type: the pre-stamp null is LOAD-BEARING at
     * ordinary seats (the cp2-387 catch — a GREEN whole-output consumer,
     * FilterInvalidFloatingRateIndexTradeDate, re-rendered its meta-deref hoist and
     * cascaded a numbering shift when the stamp fired unscoped).
     */
    public void pushLadderJoinBareArmReplay() {
        ladderJoinBareArmReplay++;
    }

    /** Lower one level of the join-bare arm-replay stamp window. */
    public void popLadderJoinBareArmReplay() {
        if (ladderJoinBareArmReplay <= 0) {
            throw new IllegalStateException(
                    "popLadderJoinBareArmReplay() without a matching pushLadderJoinBareArmReplay()");
        }
        ladderJoinBareArmReplay--;
    }

    /** True inside a join-bare ladder arm REPLAY pass (this scope or an ancestor). */
    public boolean isLadderJoinBareArmReplay() {
        GeneratorScope<?> s = this;
        while (s != null) {
            if (s instanceof JavaStatementScope js && js.ladderJoinBareArmReplay > 0) {
                return true;
            }
            s = s.getParent();
        }
        return false;
    }

    private int ruleBlockArmDerefSeat;

    /**
     * facet blockArmWrapperHopDeref (v3.1 flip seat 33, law F.A rung (a)): raised while
     * {@code CollectionHandler.compileLadderArmWithDeepThenDrain} compiles ONE RULE-path
     * block-ladder rung arm / terminal else -- a real braced statement seat whose per-rung
     * drains place a pending lambda hoist INSIDE the owning rung ({@code
     * compileLadderLevelArms}' inner-level {@code drainPendingLambdaHoists} at the arm and
     * the terminal, and the arm method's own {@code instanceof}-admit list at the top
     * level), which is exactly where golden puts an arm's wrapper-deref decl (drr 5.61.0
     * jfsa NotionalCurrencyOfLeg1Rule / cftc NotionalCurrencyLeg1Rule: {@code final
     * ReferenceWithMetaPriceSchedule referenceWithMetaPriceSchedule0 = ....get();} inside
     * the equity-forward rung, before its return).
     *
     * <p>The ONE consumer is {@code NavigationHandler.collapsedMetaDerefRewrapOrNull}'s
     * drain-seat conjunct, whose blanket AST reader
     * ({@code HandlerHelper.isInsideDrainableMapLambda}) returns false for ANY node under a
     * conditional -- the #312 exclusion, whose stated reason (a pending hoist inside a
     * conditional arm makes {@code compileLambda}'s conditional-block form DECLINE at its
     * {@code !hasPendingLambdaHoists()} gate) does not hold for a LADDER block, which
     * drains its arms per rung. RENDER TRUTH, never AST shape: the flag is raised only
     * while the block form is actually being compiled, so an inline-ternary ladder (the
     * P352A suppress class) never sees it. Deliberately NOT
     * {@link #markArmDerefHoistSink()}: that flag has SIX consumers
     * ({@code ControlFlowHandler} x3, {@code NavigationHandler}, {@code ReferenceHandler},
     * {@code SetOperationHandler}) and opening it here would carry a radius the seat-33
     * probe does not cover.
     *
     * <p>Counter + ancestor walk with NO lambda-boundary stop (the
     * {@link #isLadderJoinBareArmReplay()} pattern): the admitted node sits inside the arm's
     * own subtree by construction. Push/pop, never mark: the arm compiles on the ladder's
     * OWN scope, so a sticky mark would leak into the next rung's condition compile - and
     * the rung-CONDITION seat staying OUTSIDE the window is a decline this law locks
     * ({@code BlockArmWrapperHopDerefSeatTest.e2}).
     */
    public void pushRuleBlockArmDerefSeat() {
        ruleBlockArmDerefSeat++;
    }

    /** Lower one level of the rule-path block-ladder ARM window. */
    public void popRuleBlockArmDerefSeat() {
        if (ruleBlockArmDerefSeat <= 0) {
            throw new IllegalStateException(
                    "popRuleBlockArmDerefSeat() without a matching pushRuleBlockArmDerefSeat()");
        }
        ruleBlockArmDerefSeat--;
    }

    /**
     * facet blockArmWrapperHopDeref (v3.1 flip seat 33, law F.A rung (a)): true inside a
     * RULE-path block-ladder ARM compile (this scope or an ancestor).
     */
    public boolean isRuleBlockArmDerefSeat() {
        GeneratorScope<?> s = this;
        while (s != null) {
            if (s instanceof JavaStatementScope js && js.ruleBlockArmDerefSeat > 0) {
                return true;
            }
            s = s.getParent();
        }
        return false;
    }

    /**
     * facet deepBareInvokableThenHoist (PR #339): non-null while an enclosing
     * conditional-block renderer ({@code CollectionHandler.compileLadderConditionalBlock})
     * compiles the ONE arm it will drain {@code DeepThenArgHoist} pendings for — so
     * {@code tryDeepThenHoist} may exempt EXACTLY that arm node (identity-compared, the
     * #327 CondListCoerce single-slot handshake pattern) from the blanket
     * {@code thenValueHoistSuppression} the renderer pushes around arm compiles (golden
     * hoists the chain's {@code thenArgN} decls inside the owning rung). A NESTED
     * conditional/chain inside the arm is a different node and stays suppressed — the
     * #219/#250 cascade guard; inline-ternary seats never push this at all.
     */
    private RExpression deepThenHoistDrainableArm;

    /**
     * Push the drainable-arm handshake for one arm compile; returns the PREVIOUS slot
     * value so the caller's {@code finally} restores it (nested renderers must not clear
     * an outer handshake — the CondListCoerce save/restore law).
     */
    public RExpression pushDeepThenHoistDrainableArm(RExpression arm) {
        RExpression previous = this.deepThenHoistDrainableArm;
        this.deepThenHoistDrainableArm = arm;
        return previous;
    }

    /** Restore the slot to the value {@link #pushDeepThenHoistDrainableArm} returned. */
    public void popDeepThenHoistDrainableArm(RExpression previous) {
        this.deepThenHoistDrainableArm = previous;
    }

    /**
     * The nearest enclosing scope's drainable-arm handshake, or {@code null}. Stops at a
     * lambda boundary like {@link #findCondListCoerce()} — a chain inside a FURTHER nested
     * lambda drains through that lambda's own channel, not the renderer's.
     */
    public RExpression findDeepThenHoistDrainableArm() {
        GeneratorScope<?> s = this;
        while (s != null) {
            if (s instanceof JavaStatementScope js) {
                if (js.deepThenHoistDrainableArm != null) {
                    return js.deepThenHoistDrainableArm;
                }
                if (js.lambdaBoundary) {
                    return null;
                }
            }
            s = s.getParent();
        }
        return null;
    }

    /**
     * facet deepThenCtlRestructure (PR #351): the deep-seat then-LEVEL conditional
     * handshake — non-null while {@code CollectionHandler.tryDeepThenHoist} compiles a
     * then-body whose ROOT is a conditional it wants hoisted as golden's Mapper-typed
     * {@code final Mapper*<X> ifThenElseResult; if/else} block
     * ({@code ControlFlowHandler.hoistAsDeepThenMapperLocalOrNull} fires ONLY for the
     * identity-matched node — the #327/#339/#340 single-slot handshake pattern). Carries
     * the level's cardinality so the arm declares MapperS vs MapperC without re-deriving
     * the deep seat's computation. A NESTED conditional inside an arm is a different node
     * and stays on the ternary/suppression path — the #219/#250 cascade guard.
     */
    private DeepThenIteHoist deepThenIteHoistCond;

    /**
     * The #351 deep-then conditional-level handshake payload: node identity + cardinality
     * in; {@code fired} back-channel out (the arm marks it so the deep seat can
     * distinguish the hoisted sentinel from a declined inline ternary — both are
     * {@code JavaExpression}s, and shape-sniffing rendered text is off-limits).
     */
    public static final class DeepThenIteHoist {
        private final RExpression cond;
        private final boolean multi;
        private final String forcedName;
        private final boolean lambdaRoute;
        private boolean fired;

        public DeepThenIteHoist(RExpression cond, boolean multi) {
            this(cond, multi, null);
        }

        /**
         * facet deepThenCtlRestructure (PR #351, i3): {@code forcedName} names the hoist
         * local exactly (the base-thenArg mode — the #316 forcedName pattern: a
         * conditional chain BASE hoists as {@code final Mapper*<X> thenArgN;} sharing the
         * per-method thenArg sequence, NOT a separate ifThenElseResult).
         */
        public DeepThenIteHoist(RExpression cond, boolean multi, String forcedName) {
            this(cond, multi, forcedName, false);
        }

        /**
         * facet lambdaCondBaseThenArg (PR #374): {@code lambdaRoute} routes the hoisted
         * if/else block through the LAMBDA channel ({@code registerPendingLambdaHoist})
         * instead of the statement-hoist sink — the in-lambda k==0 conditional-base
         * sibling of the #351-i3 arm ({@code forcedName} is the pre-created deferred
         * lambda token, so no session registration happens on this route).
         */
        public DeepThenIteHoist(RExpression cond, boolean multi, String forcedName,
                boolean lambdaRoute) {
            this.cond = cond;
            this.multi = multi;
            this.forcedName = forcedName;
            this.lambdaRoute = lambdaRoute;
        }

        public boolean lambdaRoute() {
            return lambdaRoute;
        }

        public RExpression cond() {
            return cond;
        }

        public boolean multi() {
            return multi;
        }

        public String forcedName() {
            return forcedName;
        }

        public void markFired() {
            this.fired = true;
        }

        public boolean fired() {
            return fired;
        }
    }

    /**
     * Push the deep-then conditional handshake for one level compile; returns the PREVIOUS
     * slot value so the caller's {@code finally} restores it (nested renderers must not
     * clear an outer handshake — the CondListCoerce save/restore law).
     */
    public DeepThenIteHoist pushDeepThenIteHoistCond(DeepThenIteHoist hoist) {
        DeepThenIteHoist previous = this.deepThenIteHoistCond;
        this.deepThenIteHoistCond = hoist;
        return previous;
    }

    /** Restore the slot to the value {@link #pushDeepThenIteHoistCond} returned. */
    public void popDeepThenIteHoistCond(DeepThenIteHoist previous) {
        this.deepThenIteHoistCond = previous;
    }

    /**
     * The nearest enclosing scope's deep-then conditional handshake, or {@code null}.
     * Stops at a lambda boundary like {@link #findDeepThenHoistDrainableArm()} — a
     * conditional inside a FURTHER nested lambda compiles through that lambda's own
     * channel.
     */
    public DeepThenIteHoist findDeepThenIteHoistCond() {
        GeneratorScope<?> s = this;
        while (s != null) {
            if (s instanceof JavaStatementScope js) {
                if (js.deepThenIteHoistCond != null) {
                    return js.deepThenIteHoistCond;
                }
                if (js.lambdaBoundary) {
                    return null;
                }
            }
            s = s.getParent();
        }
        return null;
    }

    /**
     * facet enumSingletonListCondArm (PR #340): non-null while
     * {@code CollectionHandler.compileEffectiveElseConditionalBlock} compiles the conditional
     * whose per-arm drains it will place {@code ReferenceHandler.EnumConstArgHoist} pendings
     * into — so {@code tryEnumSingletonListArg} may admit EXACTLY the args this conditional
     * directly owns (identity-compared, the #327/#339 single-slot handshake pattern). A
     * NESTED conditional / lambda inside an arm is a different node and stays declined —
     * the #219/#250 cascade guard; inline-ternary seats never push this at all.
     */
    private RExpression enumConstArgDrainableCond;

    /**
     * Push the blessed-conditional handshake for one effective-else block attempt; returns
     * the PREVIOUS slot value so the caller's {@code finally} restores it (nested renderers
     * must not clear an outer handshake — the CondListCoerce save/restore law).
     */
    public RExpression pushEnumConstArgDrainableCond(RExpression cond) {
        RExpression previous = this.enumConstArgDrainableCond;
        this.enumConstArgDrainableCond = cond;
        return previous;
    }

    /** Restore the slot to the value {@link #pushEnumConstArgDrainableCond} returned. */
    public void popEnumConstArgDrainableCond(RExpression previous) {
        this.enumConstArgDrainableCond = previous;
    }

    /**
     * The nearest enclosing scope's blessed-conditional handshake, or {@code null}. Stops
     * at a lambda boundary like {@link #findDeepThenHoistDrainableArm()} — an enum arg
     * inside a FURTHER nested lambda drains through that lambda's own channel.
     */
    public RExpression findEnumConstArgDrainableCond() {
        GeneratorScope<?> s = this;
        while (s != null) {
            if (s instanceof JavaStatementScope js) {
                if (js.enumConstArgDrainableCond != null) {
                    return js.enumConstArgDrainableCond;
                }
                if (js.lambdaBoundary) {
                    return null;
                }
            }
            s = s.getParent();
        }
        return null;
    }

    /**
     * facet blockArmInteriorLadder (PR #354): the conditional nodes whose BLOCK-form render
     * is compiling an ARM right now ({@code CollectionHandler.compileLadderConditionalBlock}
     * registers its whole rung chain around each rung-arm / terminal-else compile). An
     * interior ladder whose {@code isCleanLadderContext} walk reaches a REGISTERED
     * conditional ancestor sits in a statement seat — the outer's arm renders
     * {@code return <arm>;} inside an if/return block, where golden block-converts the
     * interior ladder too (the OptionStyle class). An UNREGISTERED conditional ancestor is
     * an inline-ternary owner ({@code ControlFlowHandler}) — the P352A suppress class where
     * golden restructures the WHOLE outer — and keeps the decline. Unlike the
     * drainable-arm / ite-hoist handshakes the reader does NOT stop at lambda boundaries
     * (mirroring {@link #isThenValueHoistSuppressed()}): the admitted interior sits INSIDE
     * the arm's extract lambda by construction. Save/restore push with UNION semantics — a
     * nested block render inside an arm re-pushes previous + its own nodes, so a deeper
     * interior sees every enclosing block level registered.
     */
    private List<RExpression> blockArmSeatConditionals;

    /**
     * Register {@code nodes} (a block renderer's conditional chain) for one arm compile;
     * returns the PREVIOUS slot value so the caller's {@code finally} restores it. The new
     * slot is the UNION of the previous registration and {@code nodes}.
     */
    public List<RExpression> pushBlockArmSeatConditionals(List<? extends RExpression> nodes) {
        List<RExpression> previous = this.blockArmSeatConditionals;
        List<RExpression> merged = new ArrayList<>();
        if (previous != null) {
            merged.addAll(previous);
        }
        merged.addAll(nodes);
        this.blockArmSeatConditionals = merged;
        return previous;
    }

    /** Restore the slot to the value {@link #pushBlockArmSeatConditionals} returned. */
    public void popBlockArmSeatConditionals(List<RExpression> previous) {
        this.blockArmSeatConditionals = previous;
    }

    /**
     * True iff {@code cond} is registered as a block-arm-seat owner on this scope or ANY
     * ancestor (identity compare; no lambda-boundary stop — see the field javadoc).
     */
    public boolean isBlockArmSeatConditional(RExpression cond) {
        GeneratorScope<?> s = this;
        while (s != null) {
            if (s instanceof JavaStatementScope js && js.blockArmSeatConditionals != null) {
                for (RExpression c : js.blockArmSeatConditionals) {
                    if (c == cond) {
                        return true;
                    }
                }
            }
            s = s.getParent();
        }
        return false;
    }

    /**
     * facet nestedLadderRung (PR #356): the ROOT {@link RThenExpr} of the then-chain whose
     * ARGUMENT-side step value the SET-seat restructure
     * ({@code FunctionExpressionRenderer.renderThenExtractSetImpl}) is compiling right now.
     * A ladder inside the chain's ARGUMENT reaches its then ancestor BEFORE any step body is
     * bound (the k==0 base compiles first), so the #350 {@code thenArgRefFor} read cannot see
     * the restructure — and the #349-D base-scan is self-poisoned (the argument subtree
     * contains the ladder itself). This node-identity flag is the render-truth signal for
     * exactly that window ({@code isCleanLadderContext} ascends past the flagged chain — the
     * ExtractCall/PutAmount class). A producer decline AFTER the base compile discards the
     * attempt and the fallback re-compile runs flag-free, so unrestructured chains keep the
     * #349-D decline. The reader walks ancestors with NO lambda-boundary stop (the
     * {@link #isBlockArmSeatConditional} pattern): the admitted ladder sits inside the
     * chain's extract lambda by construction. Single-slot save/restore push — the
     * CondListCoerce law.
     */
    private RThenExpr deepThenRestructureChainTop;

    /**
     * Flag {@code chainTop} as the actively-restructuring chain for one argument-side value
     * compile; returns the PREVIOUS slot value so the caller's {@code finally} restores it.
     */
    public RThenExpr pushDeepThenRestructureChainTop(RThenExpr chainTop) {
        RThenExpr previous = this.deepThenRestructureChainTop;
        this.deepThenRestructureChainTop = chainTop;
        return previous;
    }

    /** Restore the slot to the value {@link #pushDeepThenRestructureChainTop} returned. */
    public void popDeepThenRestructureChainTop(RThenExpr previous) {
        this.deepThenRestructureChainTop = previous;
    }

    /**
     * The nearest enclosing scope's actively-restructuring chain top, or {@code null}
     * (identity compare by the caller; no lambda-boundary stop — see the field javadoc).
     */
    public RThenExpr findDeepThenRestructureChainTop() {
        GeneratorScope<?> s = this;
        while (s != null) {
            if (s instanceof JavaStatementScope js && js.deepThenRestructureChainTop != null) {
                return js.deepThenRestructureChainTop;
            }
            s = s.getParent();
        }
        return null;
    }

    /**
     * Resolve the {@code thenArg} reference bound for {@code boundary}, walking this scope
     * and its ancestors; {@code null} if none. Used by
     * {@code ReferenceHandler.handle(RImplicitVariable)} to re-root a then-body's implicit
     * input on its {@code thenArg}/{@code thenArgN} — only when the implicit variable's nearest
     * enclosing inline function IS the bound boundary, so nested lambdas inside the body (and
     * the OTHER thens' bodies in a chain) are unaffected.
     */
    public JavaExpression thenArgRefFor(RInlineFunction boundary) {
        GeneratorScope<?> s = this;
        while (s != null) {
            if (s instanceof JavaStatementScope js) {
                JavaExpression ref = js.thenArgBindings.get(boundary);
                if (ref != null) {
                    return ref;
                }
            }
            s = s.getParent();
        }
        return null;
    }

    // =========================================================================
    // Deferred coercion-param naming (facet meta_coercion_numbering)
    // =========================================================================

    /**
     * A null-GUARDED Type-coercion lambda param registered for deferred naming:
     * the {@code sentinel} token was emitted in place of the name; {@code site}
     * is the scope the param registered into (the statement body scope for
     * chain-level coercions, a {@link #lambdaScope()} child for in-lambda
     * coercions); {@code id} resolves to the actual name at finalization.
     * {@code burnOnly} entries (facet n2DoubleUnderscore, PR #363 — see
     * {@link #registerNameBurn}) resolve to burn their escaped name into the
     * site's taken set but substitute to the EMPTY string: the sentinel marks
     * op-keptness, never a rendered name. {@code textOrder} entries (facet
     * ctorSetterHoistTextOrder, seat 33 law A.1 — see
     * {@link #registerDeferredCoercionNameTextOrdered}, and its second producer, any entry
     * registered while a law-D.3 text-order window is open, see
     * {@link #textOrderCoercionWindowDepth}) make their unified replay group number by
     * first-occurrence text position.
     */
    private record DeferredCoercionName(
            String sentinel, JavaStatementScope site, GeneratedIdentifier id,
            boolean burnOnly, boolean textOrder) {}

    /** Root-only (see {@link #rootStatementScope()}): deferred entries. */
    private List<DeferredCoercionName> deferredCoercionNames;

    /**
     * facet ctorSetterHoistTextOrder (v3.1 flip seat 33, law D.3 rung 5, extending law A.1
     * rung 3): root-only depth counter - while a text-order coercion WINDOW is open, EVERY
     * deferred entry registered under this root marks {@code textOrder}, so its whole replay
     * group re-orders to first-occurrence text position in
     * {@link #resolveUnifiedDeferredNames}. Pushed around exactly ONE compile: the deep-seat
     * conditional-BASE compile of a chain {@code deepSeatCondBaseAdmitNewlyAdmits} newly
     * admits (CollectionHandler, law D.3) - the handshake's arm coercions register DURING
     * the base compile while the inner-ladder consumers register AFTER, and golden numbers
     * the whole {@code "Type coercion"} param group in emission (= text) order (drr 7.x
     * TotalNotionalQuantity {@code referenceWithMetaNonNegativeQuantitySchedule0..7}). A
     * window over a green render is NOT a no-op by construction - the A.1 measurement
     * {@code [P33-A1ORDER]} (below) found green groups whose counter order differs from their
     * text order - so the window's green-safety rests on its footprint (pushed around exactly
     * ONE compile, the D.3 base compile of a newly-admitted chain) and on the whole-matrix
     * measurement at the seat's head (band 0 on both routes), not on a stable-sort argument.
     */
    private int textOrderCoercionWindowDepth;

    /** See {@link #textOrderCoercionWindowDepth}. */
    public void pushTextOrderCoercionWindow() {
        rootStatementScope().textOrderCoercionWindowDepth++;
    }

    /** See {@link #textOrderCoercionWindowDepth}. */
    public void popTextOrderCoercionWindow() {
        rootStatementScope().textOrderCoercionWindowDepth--;
    }

    /**
     * Globally unique sentinel counter — NOT per-root: if two registering roots'
     * rendered strings were ever concatenated into one finalized statement
     * (no renderer route does this today, but renderConditionalAssignment's
     * scope-sharing javadoc names switch/then-extract as candidates if a
     * numbering carrier surfaces there), per-root counters would both emit
     * {@code __COERCION_PARAM_0__} and the first resolution's
     * {@code String.replace} would silently consume both occurrences.
     */
    private static final java.util.concurrent.atomic.AtomicInteger COERCION_SENTINEL_COUNTER =
            new java.util.concurrent.atomic.AtomicInteger();

    /**
     * Register a null-GUARDED coercion lambda param for deferred naming and
     * return the unique sentinel token to embed where the name belongs.
     *
     * <p>Mirrors upstream {@code TypeCoercionService.convertNullSafe} →
     * {@code declareAsVariable} → {@code createSynonym}: the param registers a
     * {@link GeneratedIdentifier} into THIS scope (the scope threaded at the
     * coercion site), so {@code computeActualNames} resolves the group law —
     * bare when the desired name is unique in its scope, {@code 0..n-1} when
     * ≥2 identifiers share it, underscore-escaped when a child-scope param hits
     * a parent-taken name. Resolution happens once the statement's rendering is
     * final, via {@link #resolveDeferredCoercionNames(String)} on the root
     * scope (the {@code FunctionExpressionRenderer} finalization hook) —
     * the fork's eager string rendering cannot know the group size until every
     * same-scope registration has happened, exactly why upstream defers through
     * {@code GeneratedIdentifier} lazy resolution.
     *
     * <p>The sentinel substitution is a literal {@link String#replace} of an
     * opaque generated token — not structural analysis of the generated Java.
     */
    public String registerDeferredCoercionParam(String desiredName) {
        return registerDeferredCoercionName(createUniqueIdentifier(desiredName));
    }

    /**
     * facet lambdaNaming M3/M4 (PR #329): register a NAV/deref LAMBDA PARAM for
     * deferred naming and return the sentinel token to embed as the param name.
     * The identifier registers into a FRESH {@link #lambdaScope()} child of this
     * scope — every nav-step lambda has exactly ONE param in its own textual
     * lambda, so the group is always a singleton (never numbered, exactly the
     * prior non-mutating {@code disambiguate} semantics) — but the ESCAPE
     * decision now resolves at finalization, when the parent chain is complete:
     * a hoist local or registered param created LATER in the statement (or, under
     * the M1 method-wide group, later in the METHOD) is visible as taken, exactly
     * as upstream's lazily-materialised method scope resolves it (golden
     * {@code _dateTimeList} against the #198 ctor-hoist local declared around the
     * lambda; golden {@code _fieldWithMetaString} against the #290 whole-output
     * deref hoist declared statements later).
     */
    public String registerDeferredLambdaParam(String desiredName) {
        JavaStatementScope lambda = lambdaScope();
        return lambda.registerDeferredCoercionName(lambda.createUniqueIdentifier(desiredName));
    }

    /**
     * Identifier-taking variant of {@link #registerDeferredCoercionParam}: defer
     * the naming of an ALREADY-created {@link GeneratedIdentifier} and return the
     * sentinel token to embed where the name belongs in eagerly-rendered strings.
     * Lets a caller that ALSO needs the identifier itself — facet
     * evaluate_arg_consumption's hoisted meta-deref local, whose
     * {@code JavaLocalVariableDeclarationStatement} resolves the SAME identifier
     * at render time — share one registration, so the declaration and the
     * sentinel-rendered references agree on the finalized name (numbered with
     * same-desired-name guarded coercion params per the
     * {@code computeActualNames} group law).
     */
    public String registerDeferredCoercionName(GeneratedIdentifier id) {
        return registerDeferredEntry(id, false);
    }

    /**
     * facet n2DoubleUnderscore (PR #363): register a NAME BURN — the fork-side
     * mirror of upstream {@code JavaVariable.declareAsVariable}'s synonym route
     * ({@code TypeCoercionService.convertNullSafe} / the as-key single branch on
     * an ALREADY-variable expression): upstream re-registers the variable's
     * {@link GeneratedIdentifier} into the statement scope via
     * {@code createSynonym}, so that scope's {@code computeActualNames} processes
     * the desired name like a local — escaping it against the ancestor chain
     * (fn input {@code observation} → body-scope actual {@code _observation})
     * and BURNING the escaped name into the scope's taken set even though the
     * re-registration renders nowhere (the variable renders through its OWN
     * method-scope actual). Every child-scope lambda param desiring the same
     * name then escalates ({@code __observation} — golden cdm5/cdm6
     * Resolve{InterestRate,Performance}Reset, cdm5 InterestCashSettlementAmount,
     * Create_PartyChange ×2; the CLOSED 10-token corpus census).
     *
     * <p>The returned sentinel MUST be embedded in the op's rendered text (it
     * substitutes to the EMPTY string): the unified-replay survival filter
     * ({@link #resolveUnifiedDeferredNames} step 2) keeps entries whose sentinel
     * survives into a KEPT text, which is exactly the burn's keep condition — a
     * discarded render pass (the #257 cascade fallback) must not burn.
     *
     * <p>The identifier registers into THIS scope directly (the statement body —
     * NOT a {@link #lambdaScope()} child), so its unified counterpart is the ONE
     * method-wide body: a burn in op 4 escalates a lambda param in op 1, exactly
     * as upstream's close-time resolution is registration-order-blind. A burn
     * sharing its desired name with a registered guarded param NUMBERS with it
     * (upstream's {@code referenceWithMetaParty0/1/2}, Create_PartyChange).
     */
    public String registerNameBurn(String desiredName) {
        return registerDeferredEntry(createUniqueIdentifier(desiredName), true);
    }

    private String registerDeferredEntry(GeneratedIdentifier id, boolean burnOnly) {
        return registerDeferredEntry(id, burnOnly, false);
    }

    /**
     * facet ctorSetterHoistTextOrder (v3.1 flip seat 33, law A.1, rung 3): the MARKED variant
     * of {@link #registerDeferredCoercionName} for a COERCION-PHASE ctor-setter meta-deref
     * hoist ({@code ConstructionHandler.hoistMetaDerefCtorNavInLambdaOrNull} and its
     * {@code default}-valued sibling — the only callers). {@code ConstructionHandler} compiles
     * every ctor pair's value FIRST and runs coercion in a SECOND loop, so such a hoist
     * REGISTERS after its later siblings' compile-phase evaluate-arg derefs while the #361
     * lambda-hoist window renders its declaration FIRST; the mark lets
     * {@link #resolveUnifiedDeferredNames} re-order exactly the replay group containing the
     * entry to first-occurrence text position — golden's emission-order numbering
     * ({@code fieldWithMetaString0/1/2}, drr 7.0–7.3 GetBasketConstituents, against the
     * counter order's {@code 2/0/1}). Single-root resolution
     * ({@link #resolveDeferredCoercionNames}) ignores the mark — the RULE path renders
     * golden's numbering from plain registration order today, measured green.
     */
    public String registerDeferredCoercionNameTextOrdered(GeneratedIdentifier id) {
        return registerDeferredEntry(id, false, true);
    }

    private String registerDeferredEntry(GeneratedIdentifier id, boolean burnOnly,
            boolean textOrder) {
        JavaStatementScope root = rootStatementScope();
        if (root.deferredCoercionNames == null) {
            root.deferredCoercionNames = new ArrayList<>();
        }
        String sentinel = "__COERCION_PARAM_" + COERCION_SENTINEL_COUNTER.getAndIncrement() + "__";
        root.deferredCoercionNames.add(new DeferredCoercionName(sentinel, this, id, burnOnly,
                textOrder || root.textOrderCoercionWindowDepth > 0));
        return sentinel;
    }

    /**
     * Substitute every deferred coercion-param sentinel registered under this
     * scope tree with its resolved actual name. Must be called on the scope a
     * renderer compile site created (any scope in the tree resolves to the same
     * root), and only after ALL compilation against the tree is complete —
     * {@code getActualName} closes scopes to materialise names. No-op (returns
     * {@code src} unchanged, scopes left open) when nothing registered. The
     * registry is CLEARED once applied (Copilot PR #170): a repeat call is a
     * no-op instead of redundant {@code getActualName} walks, and the
     * {@link GeneratedIdentifier}/scope references are released.
     */
    public String resolveDeferredCoercionNames(String src) {
        JavaStatementScope root = rootStatementScope();
        if (root.deferredCoercionNames == null) {
            return src;
        }
        String out = src;
        for (DeferredCoercionName d : root.deferredCoercionNames) {
            String actual = d.site().getActualName(d.id())
                    .orElseThrow(() -> new NoSuchElementException(
                            "Deferred coercion param did not resolve: " + d.sentinel()));
            // A burn resolves (its escaped name lands in the site's taken set,
            // visible to every sibling-lambda resolution) but renders nothing.
            out = out.replace(d.sentinel(), d.burnOnly() ? "" : actual);
        }
        root.deferredCoercionNames = null;
        return out;
    }

    private JavaStatementScope rootStatementScope() {
        JavaStatementScope s = this;
        while (s.getParent() instanceof JavaStatementScope parent) {
            s = parent;
        }
        return s;
    }

    // =========================================================================
    // Unified method-wide deferred naming (facet lambdaNaming M1, PR #329)
    // =========================================================================

    /**
     * Resolve every deferred coercion-param sentinel registered under the given
     * per-statement scope trees against ONE unified virtual method scope, so
     * same-desired-name REGISTERED identifiers group and number {@code 0..n-1}
     * METHOD-wide — upstream keeps a single {@code assignOutput} body scope for
     * the whole method, so a guarded Type-coercion param in statement 3 numbers
     * {@code referenceWithMetaPayout3} after statement 1's {@code 0..2} (golden
     * CalculateTransfer), and a #237 hoist local groups with a LATER statement's
     * guarded param ({@code referenceWithMetaNonNegativeQuantitySchedule0/1},
     * golden GetNotionalAmount). The fork renders statements eagerly against
     * per-statement scope trees (fresh root per {@code createScope} call — the
     * #170 discarded-attempt isolation depends on this), so instead of sharing a
     * scope, the FINAL naming is computed here by REPLAY:
     *
     * <ol>
     *   <li><b>Collect</b> every {@link DeferredCoercionName} from every root
     *       reachable from {@code bodyScopes} (in list order), then CLEAR the
     *       originals (they are never individually resolved).</li>
     *   <li><b>Filter</b> to entries whose sentinel occurs in a KEPT text — a
     *       discarded render pass (the #257 cascade fallback's first pass, the
     *       #316 base-conditional probe, a handler compile-then-discard) leaves
     *       registrations on orphaned scope trees whose sentinels appear in no
     *       surviving statement, so they must not inflate the unified groups.</li>
     *   <li><b>Order</b> by the global sentinel counter — registration order
     *       across roots (list order alone can interleave when a sub-render
     *       creates a root mid-statement).</li>
     *   <li><b>Replay</b> each entry into a counterpart of its original site in
     *       a fresh unified tree (one seeded root + ONE body child standing in
     *       for every per-statement body; deeper descendants recreated
     *       per-original-object, preserving lambda boundaries so in-lambda
     *       groups stay per-lambda), and let {@code computeActualNames} resolve
     *       the group law exactly as it does for a single statement.</li>
     * </ol>
     *
     * @param bodyScopes the per-statement body scopes the renderer tracked
     *     (one per {@code createScope} call), in creation order
     * @param seedNames the method-scope seed names (inputs / output / aliases —
     *     identical to what every per-statement root was seeded with)
     * @param texts the kept per-operation rendered statements
     * @return {@code texts} with every surviving sentinel substituted
     */
    public static List<String> resolveUnifiedDeferredNames(List<JavaStatementScope> bodyScopes,
            List<String> seedNames, List<String> texts) {
        return resolveUnifiedDeferredNames(bodyScopes, seedNames, texts, Map.of());
    }

    /**
     * facet condDerefTextOrderUnify (PR #378): {@code sessionGroups} overload — the
     * {@code StatementHoistSession} method-level groups (base name → registration-ordered
     * sentinels) whose base name is ALSO the desired name of a surviving deferred entry
     * join the replay as ONE group ordered by first-occurrence TEXT position, exactly as
     * upstream's single method scope numbers guarded coercion params and whole-output
     * deref hoist locals together in build (= text) order (golden CallQuantity /
     * PutQuantity: cond-param 0, branch hoist 1, cond-param 2, branch hoist 3 — the
     * #329 documented-limitation carrier). A session sentinel absent from every kept
     * text (a stranded tail registration, numbering-safe under the session law) orders
     * AFTER every surviving member, preserving the rendered prefix's numbers. Entries
     * whose desired name is NOT a session base keep the global-counter order.
     */
    public static List<String> resolveUnifiedDeferredNames(List<JavaStatementScope> bodyScopes,
            List<String> seedNames, List<String> texts, Map<String, List<String>> sessionGroups) {
        // 1. Collect + clear.
        List<DeferredCoercionName> all = new ArrayList<>();
        Map<JavaStatementScope, Boolean> seenRoots = new IdentityHashMap<>();
        for (JavaStatementScope body : bodyScopes) {
            JavaStatementScope root = body.rootStatementScope();
            if (seenRoots.put(root, Boolean.TRUE) != null) {
                continue;
            }
            if (root.deferredCoercionNames != null) {
                all.addAll(root.deferredCoercionNames);
                root.deferredCoercionNames = null;
            }
        }
        if (all.isEmpty() && sessionGroups.isEmpty()) {
            return texts;
        }
        // 2. Filter to sentinels that survived into a kept text. One scan of the
        //    kept texts collects the opaque tokens (globally unique, so set
        //    membership is exact); avoids the O(entries x texts) contains scan.
        Set<String> surviving = new HashSet<>();
        for (String t : texts) {
            if (t == null) {
                continue;
            }
            int from = 0;
            while ((from = t.indexOf("__COERCION_PARAM_", from)) >= 0) {
                int close = t.indexOf("__", from + "__COERCION_PARAM_".length());
                if (close < 0) {
                    break;
                }
                surviving.add(t.substring(from, close + 2));
                from = close + 2;
            }
        }
        List<DeferredCoercionName> kept = new ArrayList<>();
        for (DeferredCoercionName d : all) {
            if (surviving.contains(d.sentinel())) {
                kept.add(d);
            }
        }
        if (kept.isEmpty() && sessionGroups.isEmpty()) {
            return texts;
        }
        // 3. Global registration order (the sentinel counter is monotonic).
        kept.sort(java.util.Comparator.comparingLong(
                JavaStatementScope::sentinelSequence));
        // facet condDerefTextOrderUnify (PR #378): partition — entries sharing a base
        // name with a session group merge with that group's sentinels and re-order by
        // first-occurrence text position (each kind's sentinel embeds at its declaration
        // site, so text position IS upstream's build/registration order); everything
        // else keeps the counter order. Cross-group replay order is immaterial (the
        // group law numbers within one desired name; escapes read final scope state).
        record ReplayItem(String sentinel, JavaStatementScope site, String desiredName,
                boolean burnOnly, boolean textOrder) {}
        List<ReplayItem> items = new ArrayList<>(kept.size());
        List<ReplayItem> sharedItems = new ArrayList<>();
        for (DeferredCoercionName d : kept) {
            ReplayItem item = new ReplayItem(d.sentinel(), d.site(),
                    d.id().getDesiredName(), d.burnOnly(), d.textOrder());
            if (sessionGroups.containsKey(item.desiredName())) {
                sharedItems.add(item);
            } else {
                items.add(item);
            }
        }
        // facet ctorSetterHoistTextOrder (v3.1 flip seat 33, law A.1, rung 3): a replay group
        // that CONTAINS a marked coercion-phase ctor-setter hoist re-orders by first-occurrence
        // TEXT position - the #378 condDerefTextOrderUnify ordering extended to exactly the
        // marked groups, NEVER generalised: [P33-A1ORDER] probe v2 measured three OTHER
        // same-site multi-member groups per cell (referenceWithMetaParty n=3 / n=4,
        // ifThenElseResult n=2) whose counter order differs from text order inside files GREEN
        // today, so the general text-order rule is REFUTED at this corpus. What forces the
        // scoped rule: ConstructionHandler compiles every ctor pair's value FIRST and runs
        // coercion in a SECOND loop, so a coercion-phase hoist (the two
        // registerDeferredCoercionNameTextOrdered callers) registers AFTER its later siblings'
        // compile-phase evaluate-arg derefs while the #361 lambda-hoist window renders its
        // declaration FIRST - golden numbers the RENDERED order (fieldWithMetaString0/1/2,
        // drr 7.0-7.3 GetBasketConstituents), the counter order reads 2/0/1. The permutation
        // writes the sorted members back into the SAME index slots, so cross-group
        // interleaving and every unmarked group keep the counter order byte-for-byte; within
        // a marked group whose counter order already equals its text order (rung 1's RULE-path
        // carriers, green today) the stable sort moves nothing. An absent sentinel (a stranded
        // registration) keeps its counter order at the tail (firstOccurrence = Long.MAX_VALUE),
        // exactly as the session rule below treats it. A marked entry whose desired name is a
        // session base joins sharedItems above and is already text-ordered by the #378 rule.
        Map<JavaStatementScope, Map<String, List<Integer>>> textOrderGroups =
                new IdentityHashMap<>();
        for (int i = 0; i < items.size(); i++) {
            ReplayItem it = items.get(i);
            textOrderGroups.computeIfAbsent(it.site(), k -> new java.util.LinkedHashMap<>())
                    .computeIfAbsent(it.desiredName(), k -> new ArrayList<>()).add(i);
        }
        for (Map<String, List<Integer>> byName : textOrderGroups.values()) {
            for (List<Integer> slots : byName.values()) {
                if (slots.size() < 2) {
                    continue;
                }
                boolean marked = false;
                for (int slot : slots) {
                    if (items.get(slot).textOrder()) {
                        marked = true;
                        break;
                    }
                }
                if (!marked) {
                    continue;
                }
                List<ReplayItem> members = new ArrayList<>(slots.size());
                for (int slot : slots) {
                    members.add(items.get(slot));
                }
                members.sort(java.util.Comparator.comparingLong(
                        it -> firstOccurrence(it.sentinel(), texts)));
                for (int j = 0; j < slots.size(); j++) {
                    items.set(slots.get(j), members.get(j));
                }
            }
        }
        if (!sessionGroups.isEmpty()) {
            for (Map.Entry<String, List<String>> g : sessionGroups.entrySet()) {
                for (String sessionSentinel : g.getValue()) {
                    // Session hoist locals are method-level declarations — they replay
                    // at the unified BODY (site == null marks it; resolved below).
                    sharedItems.add(new ReplayItem(sessionSentinel, null, g.getKey(), false,
                            false));
                }
            }
            sharedItems.sort(java.util.Comparator.comparingLong(
                    it -> firstOccurrence(it.sentinel(), texts)));
            items.addAll(sharedItems);
        }
        // 4. Replay into the unified tree.
        JavaStatementScope unifiedRoot = new JavaStatementScope("function", null);
        for (String seedName : seedNames) {
            if (seedName != null && !seedName.isEmpty()) {
                unifiedRoot.createUniqueIdentifier(seedName);
            }
        }
        JavaStatementScope unifiedBody = unifiedRoot.bodyScope();
        Map<JavaStatementScope, JavaStatementScope> counterparts = new IdentityHashMap<>();
        record Replayed(String sentinel, JavaStatementScope site, GeneratedIdentifier id,
                boolean burnOnly) {}
        List<Replayed> replayed = new ArrayList<>(items.size());
        for (ReplayItem it : items) {
            JavaStatementScope site = it.site() == null
                    ? unifiedBody
                    : counterpartOf(it.site(), unifiedRoot, unifiedBody, counterparts);
            replayed.add(new Replayed(it.sentinel(), site,
                    site.createUniqueIdentifier(it.desiredName()), it.burnOnly()));
        }
        // 5. Resolve + substitute (literal replace of opaque generated tokens).
        // A burn entry resolves like any other (its escaped name lands in the
        // unified body's taken set — the whole point) but substitutes to "".
        List<String> out = new ArrayList<>(texts);
        for (Replayed r : replayed) {
            String actual = r.site().getActualName(r.id())
                    .orElseThrow(() -> new NoSuchElementException(
                            "Unified deferred coercion param did not resolve: " + r.sentinel()));
            String replacement = r.burnOnly() ? "" : actual;
            for (int i = 0; i < out.size(); i++) {
                if (out.get(i) != null) {
                    out.set(i, out.get(i).replace(r.sentinel(), replacement));
                }
            }
        }
        return out;
    }

    /** The numeric sequence embedded in a {@code __COERCION_PARAM_<n>__} sentinel. */
    private static long sentinelSequence(DeferredCoercionName d) {
        String s = d.sentinel();
        return Long.parseLong(s.substring("__COERCION_PARAM_".length(), s.length() - 2));
    }

    /**
     * facet condDerefTextOrderUnify (PR #378): first-occurrence position of a sentinel
     * across the kept texts — {@code (textIndex << 32) | charOffset}, or
     * {@link Long#MAX_VALUE} when absent (a stranded registration orders after every
     * surviving member; the merge sort is stable, so absent entries keep their
     * session registration order at the tail).
     */
    private static long firstOccurrence(String token, List<String> texts) {
        for (int i = 0; i < texts.size(); i++) {
            String t = texts.get(i);
            int at = t == null ? -1 : t.indexOf(token);
            if (at >= 0) {
                return ((long) i << 32) | at;
            }
        }
        return Long.MAX_VALUE;
    }

    /**
     * facet condDerefTextOrderUnify (PR #378): read-only peek for the unify gate — the
     * subset of {@code candidates} that names a deferred entry (registered under the
     * given scopes' roots) whose sentinel survives into a kept text. Clears and
     * resolves NOTHING — the actual resolution stays with
     * {@link #resolveUnifiedDeferredNames}.
     */
    public static java.util.Set<String> survivingDeferredDesiredNames(
            List<JavaStatementScope> bodyScopes, java.util.Collection<String> candidates,
            List<String> texts) {
        if (candidates.isEmpty()) {
            return java.util.Set.of();
        }
        java.util.Set<String> out = new java.util.LinkedHashSet<>();
        Map<JavaStatementScope, Boolean> seenRoots = new IdentityHashMap<>();
        for (JavaStatementScope body : bodyScopes) {
            JavaStatementScope root = body.rootStatementScope();
            if (seenRoots.put(root, Boolean.TRUE) != null) {
                continue;
            }
            if (root.deferredCoercionNames == null) {
                continue;
            }
            for (DeferredCoercionName d : root.deferredCoercionNames) {
                String desired = d.id().getDesiredName();
                if (!candidates.contains(desired) || out.contains(desired)) {
                    continue;
                }
                for (String t : texts) {
                    if (t != null && t.contains(d.sentinel())) {
                        out.add(desired);
                        break;
                    }
                }
            }
        }
        return out;
    }

    /**
     * The unified-tree counterpart of an original per-statement scope: every
     * original ROOT maps to the one unified root, every root's direct body child
     * to the one unified body (this is the method-wide unification), and deeper
     * descendants map through their parent's counterpart — preserving
     * {@link #lambdaBoundary} so in-lambda registrations keep their per-lambda
     * grouping (golden numbers guarded params within one lambda independently of
     * the method group).
     *
     * <p>facet methodGroupBodyTransparency (PR #399, F): a NON-lambda deeper
     * descendant is naming-TRANSPARENT — it maps to its parent's counterpart
     * directly instead of a fresh {@code bodyScope()} child, so a decl minted on
     * an arm/chain child scope (the #354/#375/#397 relocation seats, whose decls
     * RENDER at method-statement depth) joins the ONE method-wide group exactly
     * as upstream's single {@code assignOutput} body scope numbers them (golden
     * AnnaDsb-FRE fieldWithMetaString0..14 in text order across the FX/commodity
     * arms; the pre-#399 fresh-child mapping isolated each arm into a
     * {@code _fieldWithMetaString0/1} per-arm group). Lambda boundaries keep the
     * fresh {@code lambdaScope()} child — in-lambda groups stay per-lambda and
     * escape against the (now larger) unified ancestor set, exactly upstream's
     * two-level law.
     */
    private static JavaStatementScope counterpartOf(JavaStatementScope original,
            JavaStatementScope unifiedRoot, JavaStatementScope unifiedBody,
            Map<JavaStatementScope, JavaStatementScope> counterparts) {
        JavaStatementScope hit = counterparts.get(original);
        if (hit != null) {
            return hit;
        }
        JavaStatementScope result;
        if (!(original.getParent() instanceof JavaStatementScope parent)) {
            result = unifiedRoot;
        } else if (!(parent.getParent() instanceof JavaStatementScope)) {
            // A root's direct child — the trackBodyScope statement body.
            result = unifiedBody;
        } else {
            JavaStatementScope parentCp =
                    counterpartOf(parent, unifiedRoot, unifiedBody, counterparts);
            result = original.lambdaBoundary ? parentCp.lambdaScope() : parentCp;
        }
        counterparts.put(original, result);
        return result;
    }

    // =========================================================================
    // Statement-hoist sink (facets ifthenelse_result_hoisting + biginteger_literal_hoist)
    // =========================================================================

    /**
     * True for {@link #lambdaScope()} children. A statement-hoist walk
     * ({@link #findStatementHoistSink()}) STOPS at a lambda boundary: a
     * conditional inside a lambda body must not hoist its local OUTSIDE the
     * lambda (upstream renders lambda-interior conditionals as if/return
     * block lambdas, a different — out-of-scope — form; a lambda-interior
     * beyond-long BigInteger literal likewise declines —
     * {@code LiteralHandler.hoistBigIntegerLiteralOrNull} keeps the inline
     * constructor form, upstream's in-lambda conversion being a different
     * shape too), and a discarded
     * compile attempt's scope (a lambda-scope sibling, see
     * {@code CollectionHandler.compileLambda}) must never leak a hoist into
     * the live statement.
     */
    private boolean lambdaBoundary;

    /**
     * Non-null marks this scope as a statement-hoist SINK: hoisted
     * statement-local blocks ({@code ifThenElseResult} conditionals and
     * {@code bigInteger} beyond-long literals) registered during compilation
     * against this scope (or a non-lambda descendant) are drained and
     * prepended by the renderer that created it. Set only by
     * {@code FunctionExpressionRenderer} while its function-body hoist
     * session is active — and the session opens on the standard
     * function/dispatch path only ({@code FunctionGenerator.compileOperations}
     * passes {@code hoistSessionEligible=false} on the rule/report path), so
     * rule, alias and POJO-condition compilation never sees a sink:
     * {@code ControlFlowHandler} keeps the inline ternary and
     * {@code LiteralHandler} the inline constructor form there (today's
     * bytes).
     */
    private com.regnosys.rosetta.generator.java.function.StatementHoistSession statementHoistSession;

    /** Sink-only: hoist blocks awaiting the renderer's drain, in registration order. */
    private List<String> pendingStatementHoists;

    /** Mark this scope as a statement-hoist sink bound to {@code session}. */
    public void markStatementHoistSink(
            com.regnosys.rosetta.generator.java.function.StatementHoistSession session) {
        this.statementHoistSession = session;
    }

    /**
     * The nearest enclosing statement-hoist sink, or {@code null} when none is
     * reachable — including when a lambda boundary lies between this scope and
     * the sink (the conditional then declines to the inline ternary).
     */
    public JavaStatementScope findStatementHoistSink() {
        GeneratorScope<?> s = this;
        while (s != null) {
            if (s instanceof JavaStatementScope js) {
                if (js.statementHoistSession != null) {
                    return js;
                }
                if (js.lambdaBoundary) {
                    return null;
                }
            }
            s = s.getParent();
        }
        return null;
    }

    /** Sink-only: the bound naming session ({@code null} on non-sink scopes). */
    public com.regnosys.rosetta.generator.java.function.StatementHoistSession statementHoistSession() {
        return statementHoistSession;
    }

    /**
     * facet guardedDerefHoist (PR #364): a RESTRICTED statement-hoist sink flag the
     * META-DEREF producers ALONE consult ({@code ReferenceHandler.tryMetaDerefArg}'s
     * #237 STATEMENT_SINK route + {@code NavigationHandler.collapsedMetaDerefRewrapOrNull}'s
     * #317 arm) — marked on the pathed-conditional ARM scope
     * ({@code FunctionExpressionRenderer.renderPathedConditionalSetOrNull}) so a
     * guarded wrapper-deref hoist can register + drain INTO the owning branch
     * (golden AnnaDsbUpiRequestUnderlyingForCredit's per-arm
     * {@code final FieldWithMetaString fieldWithMetaStringN = …;} /
     * UpdateIndexTransitionPriceAndRateOption's operand-hoist pair). Deliberately NOT
     * {@link #markStatementHoistSink}: a full sink would also open the
     * conditional-hoist producers (ite/ctor/bool) on arm-VALUE-nested conditionals,
     * which golden renders as ternaries — the very decline the arm scope's unmarked
     * root enforces. Registration/drain share the plain
     * {@link #registerStatementHoist}/{@link #statementHoistMark}/
     * {@link #drainStatementHoistsSince} mechanics (session-free).
     */
    private boolean armDerefHoistSink;

    /** facet guardedDerefHoist (PR #364): mark this scope as the arm-deref sink. */
    public void markArmDerefHoistSink() {
        this.armDerefHoistSink = true;
    }

    /**
     * facet guardedDerefHoist (PR #364): the nearest enclosing ARM-DEREF sink, or
     * {@code null} — the walk mirrors {@link #findStatementHoistSink()}'s lambda-boundary
     * stop (an in-lambda deref rides the lambda channel, never this one).
     */
    public JavaStatementScope findArmDerefHoistSink() {
        GeneratorScope<?> s = this;
        while (s != null) {
            if (s instanceof JavaStatementScope js) {
                if (js.armDerefHoistSink) {
                    return js;
                }
                if (js.lambdaBoundary) {
                    return null;
                }
            }
            s = s.getParent();
        }
        return null;
    }

    /**
     * Sink-only: register a hoist block (relative indent — interior lines
     * carry only their own relative tab depth, 0 at the block's base and one
     * deeper per nested fold level; the draining renderer re-anchors every
     * line to its statement indent).
     */
    public void registerStatementHoist(String block) {
        if (pendingStatementHoists == null) {
            pendingStatementHoists = new ArrayList<>();
        }
        pendingStatementHoists.add(block);
    }

    /**
     * Sink-only: the current registration count — pair with
     * {@link #drainStatementHoistsSince(int)} so nested render pieces (e.g.
     * each arm of a conditional assignment) drain exactly the hoists their own
     * compile registered.
     */
    public int statementHoistMark() {
        return pendingStatementHoists == null ? 0 : pendingStatementHoists.size();
    }

    /**
     * Sink-only: insert hoist blocks at {@code index} (facet hoistReorder, PR #327) —
     * a ctor pair's COERCION-phase hoist (#198 singletonList / #236 meta-deref value
     * locals) emits at the pair's CONSUMPTION position, i.e. before any LATER pair's
     * compile-phase {@code ifThenElseResult} block, matching upstream's
     * consumption-order {@code declareAsVariable}. Appends when {@code index} is at or
     * past the end (the no-later-hoist case — byte-neutral with the old append).
     */
    public void insertStatementHoists(int index, List<String> blocks) {
        if (blocks.isEmpty()) {
            return;
        }
        if (pendingStatementHoists == null) {
            pendingStatementHoists = new ArrayList<>();
        }
        int at = Math.min(Math.max(index, 0), pendingStatementHoists.size());
        pendingStatementHoists.addAll(at, blocks);
    }

    /**
     * Coverage wave D (datarule): substitute a deferred-A2 placeholder in every
     * pending hoist block on this sink — the logical-combine deferral resolves
     * the {@code ifThenElseResult} sentinel AFTER the right operand compiled
     * (upstream's collapse-at-composition order), so the block was registered
     * carrying a unique placeholder. No-op when nothing is pending.
     */
    public void replaceInStatementHoists(String placeholder, String actual) {
        if (pendingStatementHoists == null) {
            return;
        }
        // ci-allowlist: regex-on-structured-content (java.util.List#replaceAll(UnaryOperator)
        // over a literal String#replace of the generator's own placeholder token — not the
        // String#replaceAll regex overload; no pattern is compiled or matched)
        pendingStatementHoists.replaceAll(b -> b.replace(placeholder, actual));
    }

    /**
     * Coverage wave D (datarule): the A2 logical-combine deferral frame — pushed
     * by {@code LogicalHandler} around ONE operand compile inside a type
     * condition; {@code ControlFlowHandler}'s A2 hoist defers its sentinel
     * registration into the frame when the hoisted conditional IS the frame's
     * operand root (node identity — a nested conditional inside the operand's
     * arms keeps the after-arms registration). The combine resolves left-frame
     * entries then right-frame entries, reproducing upstream's
     * collapse-at-composition numbering (golden drr
     * CFTCPart43TransactionReportFloatingRateResetFrequencyPeriodCond:
     * {@code A and (B and C)} numbers B=0, C=1, A=2 with source-order emission).
     */
    public static final class A2DeferralFrame {
        public final com.regnosys.rosetta.ast.RExpression operandRoot;
        public final List<String> placeholders = new ArrayList<>();

        public A2DeferralFrame(com.regnosys.rosetta.ast.RExpression operandRoot) {
            this.operandRoot = operandRoot;
        }
    }

    /** Single-slot A2 deferral frame (caller-side save/restore — the #327 pattern). */
    private A2DeferralFrame a2DeferralFrame;

    /** Push the frame for one operand compile; returns the PREVIOUS slot value. */
    public A2DeferralFrame pushA2DeferralFrame(A2DeferralFrame frame) {
        A2DeferralFrame previous = this.a2DeferralFrame;
        this.a2DeferralFrame = frame;
        return previous;
    }

    /** Restore the slot to the value {@link #pushA2DeferralFrame} returned. */
    public void popA2DeferralFrame(A2DeferralFrame previous) {
        this.a2DeferralFrame = previous;
    }

    /**
     * The nearest enclosing scope's A2 deferral frame, or {@code null}. Stops at
     * a lambda boundary like {@link #findStatementHoistSink()} — an in-lambda
     * conditional has no reachable statement sink, so the deferral must not
     * fire there either.
     */
    public A2DeferralFrame findA2DeferralFrame() {
        GeneratorScope<?> s = this;
        while (s != null) {
            if (s instanceof JavaStatementScope js) {
                if (js.a2DeferralFrame != null) {
                    return js.a2DeferralFrame;
                }
                if (js.lambdaBoundary) {
                    return null;
                }
            }
            s = s.getParent();
        }
        return null;
    }

    /** Sink-only: return and remove the hoist blocks registered since {@code mark}. */
    public List<String> drainStatementHoistsSince(int mark) {
        if (pendingStatementHoists == null || pendingStatementHoists.size() <= mark) {
            return List.of();
        }
        List<String> tail = pendingStatementHoists.subList(mark, pendingStatementHoists.size());
        List<String> drained = new ArrayList<>(tail);
        tail.clear();
        return drained;
    }

    // =========================================================================
    // condListCoerce handshake (facet condListCoerce, PR #327 arm A1)
    // =========================================================================

    /**
     * Non-null while a consumer seat compiles a conditional value it wants
     * LIST-coerced — see {@link CondListCoerce}. Single-slot with caller-side
     * save/restore (the consumer pushes around ONE value compile and pops in a
     * {@code finally}); node-identity keying in the hoist arm makes a stale or
     * nested read a no-op.
     */
    private CondListCoerce condListCoerce;

    /**
     * Push the handshake for one value compile; returns the PREVIOUS slot value so the
     * caller's {@code finally} can {@link #popCondListCoerce(CondListCoerce) restore} it —
     * a nested consumer (a ctor value inside a ctor) must not clear an outer handshake
     * that has not yet been consumed (PR #327 Seat-1 SF-3 hardening; node-identity keying
     * in the hoist arm makes a stale read a no-op either way).
     */
    public CondListCoerce pushCondListCoerce(CondListCoerce coerce) {
        CondListCoerce previous = this.condListCoerce;
        this.condListCoerce = coerce;
        return previous;
    }

    /** Restore the slot to the value {@link #pushCondListCoerce} returned. */
    public void popCondListCoerce(CondListCoerce previous) {
        this.condListCoerce = previous;
    }

    /**
     * The nearest enclosing scope's handshake, or {@code null}. The walk stops at a
     * lambda boundary like {@link #findStatementHoistSink()} — a conditional inside
     * a lambda has no reachable statement sink, so the List-form (which registers a
     * statement hoist) must not fire there either.
     */
    public CondListCoerce findCondListCoerce() {
        GeneratorScope<?> s = this;
        while (s != null) {
            if (s instanceof JavaStatementScope js) {
                if (js.condListCoerce != null) {
                    return js.condListCoerce;
                }
                if (js.lambdaBoundary) {
                    return null;
                }
            }
            s = s.getParent();
        }
        return null;
    }

    // =========================================================================
    // condSingleCoerce handshake (facet ctorCondSingleCoerce, seat 28 law A)
    // =========================================================================

    /**
     * Non-null while a ctor key-value seat compiles a conditional value it wants coerced at the
     * ATTRIBUTE's item type - see {@link CondSingleCoerce}. Single-slot with caller-side
     * save/restore, node-identity keying in the hoist arm, exactly like
     * {@link #condListCoerce}. The two slots are DISJOINT by construction: the pair loop
     * pushes the List slot on a MULTI attribute and this one on a SINGLE attribute.
     */
    private CondSingleCoerce condSingleCoerce;

    /**
     * Push the handshake for one value compile; returns the PREVIOUS slot value so the caller's
     * {@code finally} can {@link #popCondSingleCoerce(CondSingleCoerce) restore} it - the #327
     * Seat-1 SF-3 nested-consumer law (a ctor value inside a ctor must not clear an outer
     * handshake that has not yet been consumed).
     */
    public CondSingleCoerce pushCondSingleCoerce(CondSingleCoerce coerce) {
        CondSingleCoerce previous = this.condSingleCoerce;
        this.condSingleCoerce = coerce;
        return previous;
    }

    /** Restore the slot to the value {@link #pushCondSingleCoerce} returned. */
    public void popCondSingleCoerce(CondSingleCoerce previous) {
        this.condSingleCoerce = previous;
    }

    /**
     * The nearest enclosing scope's handshake, or {@code null}. The walk stops at a lambda
     * boundary exactly like {@link #findCondListCoerce()}: a boundary crossing means the
     * conditional sits one lambda DEEPER than the ctor pair that pushed, an unwitnessed shape
     * (both corpus carriers push and consume inside the SAME lambda - GetPackg's ctor is itself
     * inside the {@code mapSingleToItem} body), and declining there keeps today's bytes.
     */
    public CondSingleCoerce findCondSingleCoerce() {
        GeneratorScope<?> s = this;
        while (s != null) {
            if (s instanceof JavaStatementScope js) {
                if (js.condSingleCoerce != null) {
                    return js.condSingleCoerce;
                }
                if (js.lambdaBoundary) {
                    return null;
                }
            }
            s = s.getParent();
        }
        return null;
    }

    /**
     * Sink-only: capture the current pending-hoist list (content AND order) so a
     * SPECULATIVE render that both DRAINS ({@link #drainStatementHoistsSince(int)})
     * and re-registers ({@link #registerStatementHoist(String)}) hoists on this sink,
     * then DECLINES, can roll the sink back to its exact pre-render state — the
     * statement-hoist analogue of {@link com.regnosys.rosetta.generator.java.function.StatementHoistSession#snapshot()}
     * (PR #277 defensive hardening, Copilot R1 on PR #276 appendIteHoistChainCore's
     * decline-path drain). Returns an independent copy ({@code null} when no hoist
     * has been registered, i.e. the pre-render state is "empty").
     */
    public List<String> snapshotStatementHoists() {
        return pendingStatementHoists == null ? null : new ArrayList<>(pendingStatementHoists);
    }

    /** Sink-only: roll the pending-hoist list back to a {@link #snapshotStatementHoists()} capture. */
    public void restoreStatementHoists(List<String> snapshot) {
        if (snapshot == null) {
            // Restore the EXACT pre-render state: a null snapshot means "no hoist had been
            // registered" (pendingStatementHoists == null), so null it rather than leaving an
            // empty list — keeps snapshot/restore symmetric (a subsequent snapshot returns null,
            // not []). Functionally equivalent for the sink operations (mark/drain treat null and
            // [] identically), so byte-neutral; this is contract-purity (PR #277 Copilot R1).
            pendingStatementHoists = null;
            return;
        }
        if (pendingStatementHoists == null) {
            pendingStatementHoists = new ArrayList<>(snapshot);
        } else {
            pendingStatementHoists.clear();
            pendingStatementHoists.addAll(snapshot);
        }
    }
}
