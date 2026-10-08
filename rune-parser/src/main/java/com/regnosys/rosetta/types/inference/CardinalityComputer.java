package com.regnosys.rosetta.types.inference;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.expressions.binary.*;
import com.regnosys.rosetta.ast.expressions.constructors.*;
import com.regnosys.rosetta.ast.expressions.literals.*;
import com.regnosys.rosetta.ast.expressions.references.*;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.unary.*;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RCardinality;
import com.regnosys.rosetta.types.ExpressionCardinality;

import java.math.BigInteger;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import static com.regnosys.rosetta.types.ExpressionCardinality.*;

/**
 * Computes whether an expression is single-valued or multi-valued (list).
 * Runs in parallel with type inference (D6).
 *
 * <p>Spec: section 3.6 in {@code docs/specs/2026-04-08-m4-type-system-design.md}.
 */
public final class CardinalityComputer {

    /**
     * Computes the cardinality of an expression.
     */
    public ExpressionCardinality compute(RExpression expr) {
        return compute(expr, null, false);
    }

    /**
     * Rule-output cardinality: identical to {@link #compute(RExpression)}
     * except a {@code then} pipe ({@link RThenExpr}) is computed as upstream's
     * {@code CardinalityProvider} does — the pipe's result follows the
     * argument's cardinality through the inline-function body — instead of the
     * conservative {@code SINGLE} the global {@link #compute(RExpression)} path
     * returns. Its emitting consumers are the rule-emission path only (the
     * {@code RuleGenerator} synthetic-output cardinality back-fill + the rule
     * whole-output SET terminal coercion), so the function tail's
     * {@code then}-rendering stays byte-frozen (the global {@code compute} is
     * unchanged); since PR #607 the optimised route's
     * {@code NavigationChainClassifier} also READS it, on function bodies, as
     * the then-body root law's channel — a read-only third consumer that can
     * only decline a ladder (STREAM), never change a byte of either byte ring;
     * and the default route's function-path deep-then hoist reads it on FUNCTION
     * bodies too — including the conditional consumer's multi verdict and, since v3.2
     * seat 2, the literal-switch consumer's SINGLE-pipe admission ({@code CollectionHandler})
     * — admission reads that decline a restructure, never rendering reads. A reporting
     * rule whose body is a multi {@code filter … then extract …} chain
     * (e.g. {@code ClearingThresholdOfCounterparty1}) is multi-valued, so its
     * {@code ReportFunction<I,O>} output type-arg is {@code List<X>} and the
     * body terminal collapses with {@code .getMulti()} — matching golden.
     */
    public ExpressionCardinality computeRuleBody(RExpression expr) {
        return compute(expr, null, true);
    }

    /**
     * Recursion-carrying variant. {@code visited} guards the indirection hops
     * that can revisit a node — an alias ({@link RShortcut}) expression, and (on
     * the rule-output path) a bare reference to another reporting {@link RRule}
     * whose body is itself walked — against a malformed self-/mutually-referential
     * cycle; it is allocated lazily on the first such hop so the dominant
     * indirection-free walks stay allocation-free, and {@code null} until then.
     *
     * <p>{@code thenAware} is {@code false} on the global path (a {@code then}
     * pipe reads {@code SINGLE} — the historical conservative default) and
     * {@code true} only when reached via {@link #computeRuleBody(RExpression)};
     * it is threaded through every recursive hop so a nested {@code then}-chain
     * (the left-associative {@code A then B then C} shape) is computed
     * correctly end-to-end.
     */
    private ExpressionCardinality compute(RExpression expr, Set<RNode> visited, boolean thenAware) {
        return switch (expr) {
            // === Literals — always single (except list literal) ===============
            case RIntLiteral l -> SINGLE;
            case RNumberLiteral l -> SINGLE;
            case RStringLiteral l -> SINGLE;
            case RBooleanLiteral l -> SINGLE;
            case REmptyLiteral l -> SINGLE;
            case RListLiteral l -> MULTI;

            // === Operators that always return single ==========================
            case RComparisonExpr c -> SINGLE;
            case REqualityExpr e -> SINGLE;
            case RLogicalExpr l -> SINGLE;
            case RArithmeticExpr a -> SINGLE;
            case RContainsExpr c -> SINGLE;
            case RDisjointExpr d -> SINGLE;
            case RJoinExpr j -> SINGLE;

            // === Unary that always returns single ============================
            case RCountExpr c -> SINGLE;
            case RExistenceExpr e -> SINGLE;
            case RCardinalityCheckExpr c -> SINGLE;
            case ROnlyExistsExpr o -> SINGLE;
            case RToStringExpr t -> SINGLE;
            case RReduceExpr r -> SINGLE;
            case RMinExpr m -> SINGLE;
            case RMaxExpr m -> SINGLE;
            case RConversionExpr c -> SINGLE;

            // === Unary list ops — cardinality depends on argument / body =====
            // Mirrors upstream `CardinalityProvider`:
            //   - extract (MapOperation): multi iff body is multi OR argument is multi
            //   - filter (FilterOperation): multi iff argument is multi
            //   - sort (SortOperation): always multi
            // The original "always multi" emission was wrong for the rule-body
            // case (e.g. `reporting rule R from Foo: extract bar -> baz` over a
            // SCALAR receiver with SCALAR navigation should yield SCALAR — the
            // golden uses `mapSingleToItem` not `mapItem`, and the
            // ReportFunction<I,O> output type-arg is bare T not List<? extends T>).
            case RExtractExpr e -> extractCardinality(e, visited, thenAware);
            case RFilterExpr f -> filterCardinality(f, visited, thenAware);
            case RSortExpr s -> MULTI;

            // === ListOp — depends on operation ===============================
            case RListOpExpr lop -> switch (lop.op()) {
                case FIRST, LAST, ONLY_ELEMENT, SUM -> SINGLE;
                case FLATTEN, DISTINCT, REVERSE -> MULTI;
            };

            // === References — depends on resolved symbol's cardinality =======
            case RSymbolReference ref -> computeSymbolRefCardinality(ref, visited, thenAware);
            // facet reportOutputNavReceiverCardinality (PR #397): the rule-output path
            // mirrors upstream CardinalityProvider.caseFeatureCall — isFeatureMulti(leaf)
            // || isMulti(receiver) — so a multi-HEAD chain with a single leaf
            // (`forwardPayout -> underlier -> basket`: head 0..*, leaf 0..1) is MULTI
            // end-to-end (the BasketConstituentUnitOfMeasure basket-arm shape). The
            // global path keeps the leaf-only read (function-tail rendering depends on
            // it — FUNCTION-byte-neutral by construction, the #289/#291 pattern).
            case RFeatureCall fc -> thenAware
                    ? receiverAwareFeatureCallCardinality(fc, visited)
                    : computeFeatureCallCardinality(fc);
            // PR #445: the faithful (thenAware/validator) path takes upstream's
            // deep-feature rule — CardinalityProvider.caseDeepFeatureCall is
            // isFeatureMulti(feature) || isMulti(receiver), the EXACT
            // feature-call table (vendored :361-366). The carrier: c6
            // `instrument ->> instrumentType` over `instrument Instrument
            // (1..1)` — single ->> single = SINGLE upstream, while the fork's
            // blanket MULTI armed the #437 all-any check on the four
            // `instrumentType = <enum>` comparisons once the same-cell
            // preference resolved Instrument to the c6 choice. The GLOBAL
            // path keeps the historical blanket MULTI (generator-visible —
            // byte-frozen, the #443 split).
            case RDeepFeatureCall dfc -> thenAware
                    ? receiverAwareDeepFeatureCallCardinality(dfc, visited)
                    : MULTI;
            // facet reportOutputThenItemCardinality (PR #397): on the rule-output path a
            // bare implicit `item` reads its NEAREST-DEFINER cardinality — upstream
            // CardinalityProvider.safeIsClosureParameterMulti's ThenOperation arm: an
            // implicit defined by a THEN's inline function IS the whole piped value
            // (isMulti(then.argument)), while one defined by an extract/map/filter/sort
            // inline function is a per-element view — SINGLE unless the operation's
            // input is a LIST OF LISTS, in which case the item is itself a sub-list
            // (upstream's non-then arm is isOutputListOfLists(op.argument) — modeled
            // since PR #457 by {@link #outputIsListOfLists}). Pays the `then if item
            // exists then item else <literal>` DSL-1064 workaround shape
            // (JurisdictionOfCounterparty1/2) and the `then extract
            // <aggregate>` receiver read (the extract's ARGUMENT implicit sits outside
            // the extract's own inline fn, so the walk reaches the THEN — upstream
            // caseMapOperation's isMulti(argument) OR). The global path keeps SINGLE.
            case RImplicitVariable iv -> thenAware
                    ? implicitThenItemCardinality(iv, visited)
                    : SINGLE;
            // facet dtccProductGradeCardinality (PR #291): a DISGUISED 2-name navigation
            // chain (`head -> feature`, parsed as an REnumValueRef when `head` did not
            // resolve to a real enum) reads conservative SINGLE on the global path (an
            // REnumValueRef had no case here → the {@code default} below) and its true
            // chain cardinality on the rule-output path ({@link #computeRuleBody}) via
            // {@link #disguisedChainCardinality}. FUNCTION-byte-neutral by construction
            // (the global {@code compute} is unchanged).
            case REnumValueRef evr -> thenAware ? disguisedChainCardinality(evr, visited) : SINGLE;

            // === Structural ===================================================
            case RConstructorExpr c -> SINGLE;
            // facet reportOutputConditionalCardinality (PR #289): an if-then-else
            // EXPRESSION reads conservative SINGLE on the global path (the function
            // tail's MapperS ite-hoist rendering depends on it); the rule-output path
            // ({@link #computeRuleBody}) computes it faithfully via
            // {@link #conditionalCardinality}.
            case RConditionalExpr c -> thenAware ? conditionalCardinality(c, visited) : SINGLE;
            case RSwitchExpr s -> SINGLE;
            case RWithMetaExpr w -> SINGLE;

            // === Conservative single (argument may be multi, but these operations
            //     produce a single result in most contexts) ====================
            case RDefaultExpr d -> SINGLE;
            // A `then` pipe reads conservative SINGLE on the global path (the
            // function tail's then-rendering depends on this historical
            // default); the rule-output path ({@link #computeRuleBody}) computes
            // it faithfully via {@link #thenCardinality}.
            case RThenExpr t -> thenAware ? thenCardinality(t, visited) : SINGLE;

            default -> SINGLE; // conservative default
        };
    }

    private ExpressionCardinality computeSymbolRefCardinality(RSymbolReference ref,
            Set<RNode> visited, boolean thenAware) {
        var resolved = ref.symbol();
        if (resolved.isEmpty()) return SINGLE;
        var node = resolved.get();
        if (node instanceof RAttribute attr) {
            ExpressionCardinality declared = attributeCardinality(attr);
            // PR #454 (facet annexAReleasedSeverities' cardinality decode): a bare
            // reference to a TYPE-member attribute is an implicit-item feature
            // (the Cat-9 bind), and upstream's CardinalityProvider reads it as the
            // feature-call it semantically is — isFeatureMulti(feature) ||
            // isMulti(implicit receiver). A single-declared feature of a MULTI
            // then-item therefore reads MULTI (`then if regimeName any = …` over a
            // flattened regimeInformation pipe — the csa/cftc V0-bank-silent
            // sites where the missing OR-in over-fired the modifier warnings).
            // Faithful path only; function inputs/outputs/params (parent = an
            // RFunction) keep the declared read on both paths.
            if (thenAware && declared == SINGLE
                    && (attr.parent() instanceof com.regnosys.rosetta.ast.types.RDataType
                            || attr.parent() instanceof com.regnosys.rosetta.ast.types.RChoice)) {
                return implicitItemCardinalityFrom(ref, visited);
            }
            return declared;
        }
        if (node instanceof RFunction fn) {
            return fn.output().map(this::attributeCardinality).orElse(SINGLE);
        }
        // facet reportOutputCardinality follow-on (PR #273): a bare reference to
        // ANOTHER reporting RULE (the injected sub-rule shape — `filter … then
        // cde.quantity.NotionalAmountScheduleLeg1`, or a whole-body delegation
        // `cdeV1.basket.BasketConstituentNumberOfUnits`) carries that rule's OWN
        // body cardinality. The referenced rule's synthetic output is hard-coded
        // 0..1 by RFunction.fromRule (so the RFunction branch above, were it ever
        // reached, would read SINGLE), and the symbol here resolves to the RRule
        // directly (mirroring ReferenceHandler's bare-RRule injection arm) — so
        // recurse into the sub-rule's body to recover the true cardinality. This
        // hops transitively (asic → cdeV2 → cdeV1) until a directly-multi extract
        // is reached; the identity-set guard makes a malformed self-/mutually-
        // referential rule chain read SINGLE instead of overflowing the stack.
        // Rule-output path only ({@code thenAware}) — the global compute (function
        // tail) is unchanged, FUNCTION-byte-neutral by construction.
        if (thenAware && node instanceof RRule rule && rule.expression().isPresent()) {
            Set<RNode> v = visited != null
                    ? visited
                    : Collections.newSetFromMap(new IdentityHashMap<>());
            if (!v.add(rule)) {
                return SINGLE;
            }
            return compute(rule.expression().get(), v, true);
        }
        // Facet alias_receiver_typing: an alias (shortcut) reference carries its
        // EXPRESSION's cardinality — mirrors upstream CardinalityProvider's
        // ShortcutDeclaration case (`isMulti(shortcut.expression)`). Without this,
        // every multi alias read SINGLE and a multi alias-call argument into a
        // List-typed function parameter unwrapped with the non-compiling scalar
        // `.get()` instead of golden's `.getMulti()`. Alias-of-alias chains recurse
        // through this case; the identity-set guard makes a malformed
        // self-/mutually-referential alias cycle read SINGLE instead of
        // overflowing the stack.
        if (node instanceof RShortcut shortcut && shortcut.expression() != null) {
            Set<RNode> v = visited != null
                    ? visited
                    : Collections.newSetFromMap(new IdentityHashMap<>());
            if (!v.add(shortcut)) {
                return SINGLE;
            }
            return compute(shortcut.expression(), v, thenAware);
        }
        return SINGLE;
    }

    private ExpressionCardinality computeFeatureCallCardinality(RFeatureCall fc) {
        var resolved = fc.resolvedFeature();
        if (resolved.isEmpty()) return SINGLE;
        return attributeCardinality(resolved.get());
    }

    /**
     * facet reportOutputNavReceiverCardinality (PR #397): rule-output navigation
     * cardinality with upstream's receiver OR ({@code CardinalityProvider
     * .caseFeatureCall}: {@code isFeatureMulti(feature) || isMulti(receiver)}). The
     * receiver recursion threads {@code thenAware} so a chain rooted on a then-body
     * implicit ({@link #implicitThenItemCardinality}) or a nested multi hop reads
     * MULTI end-to-end. An unresolved leaf/receiver stays the conservative SINGLE
     * (under-fire keeps today's bytes — monotone add-only).
     */
    private ExpressionCardinality receiverAwareFeatureCallCardinality(RFeatureCall fc,
            Set<RNode> visited) {
        if (computeFeatureCallCardinality(fc) == MULTI) {
            return MULTI;
        }
        RExpression receiver = fc.receiver();
        if (receiver != null && compute(receiver, visited, true) == MULTI) {
            return MULTI;
        }
        return SINGLE;
    }

    /**
     * PR #445: upstream's deep-feature cardinality on the faithful path —
     * {@code CardinalityProvider.caseDeepFeatureCall} (vendored :361-366) is
     * {@code isFeatureMulti(feature) || isMulti(receiver)}, the exact
     * {@link #receiverAwareFeatureCallCardinality} table. An unresolved deep
     * feature reads SINGLE (under-fire — a multi deep feature the fork cannot
     * resolve would miss an upstream ERROR, and upstream-ERROR shapes cannot
     * exist in valid corpora: their builds would have failed).
     */
    private ExpressionCardinality receiverAwareDeepFeatureCallCardinality(RDeepFeatureCall dfc,
            Set<RNode> visited) {
        var resolved = dfc.resolvedFeature();
        if (resolved.isPresent() && attributeCardinality(resolved.get()) == MULTI) {
            return MULTI;
        }
        RExpression receiver = dfc.receiver();
        if (receiver != null && compute(receiver, visited, true) == MULTI) {
            return MULTI;
        }
        return SINGLE;
    }

    /**
     * facet reportOutputThenItemCardinality (PR #397): the nearest-definer walk for a
     * bare implicit on the rule-output path — mirrors upstream {@code
     * ImplicitVariableUtil.findContainerDefiningImplicitVariable} + {@code
     * safeIsClosureParameterMulti}. The first enclosing {@link RInlineFunction} owns
     * the implicit: a THEN's function binds it to the WHOLE piped value (the then
     * argument's cardinality — upstream's {@code isMulti(then.argument)}), any other
     * functional operation's function binds a per-element view (SINGLE unless the
     * operation's input is a LIST OF LISTS — upstream's non-then arm, modeled since
     * PR #457 by {@link #outputIsListOfLists} through
     * {@link #implicitItemCardinalityFrom}'s non-then arm). No
     * enclosing inline function (the rule-input implicit at the chain head, upstream's
     * {@code RosettaRule} definer) is SINGLE. A missing parent link ends the walk at
     * SINGLE — under-fire keeps today's bytes. Depth-bounded 4096 (the #326 walk
     * convention).
     */
    private ExpressionCardinality implicitThenItemCardinality(RImplicitVariable iv,
            Set<RNode> visited) {
        return implicitItemCardinalityFrom(iv, visited);
    }

    /**
     * The {@link #implicitThenItemCardinality} walk generalized to ANY start
     * node (PR #454): the nearest enclosing {@link RInlineFunction} owns the
     * implicit — a THEN's function binds the WHOLE piped value (its
     * cardinality), any other functional operation binds a per-element view
     * (SINGLE — unless the operation's input is a LIST OF LISTS, PR #457: the
     * per-element view of a nested list is a SUB-LIST, i.e. MULTI; upstream
     * {@code safeIsClosureParameterMulti}'s non-then arm is
     * {@code isOutputListOfLists(op.getArgument())}, vendored :194-203).
     * Shared by the bare-implicit read above and the
     * {@code computeSymbolRefCardinality} item-feature OR-in.
     */
    private ExpressionCardinality implicitItemCardinalityFrom(RNode start, Set<RNode> visited) {
        RNode cur = start.parent();
        int depth = 0;
        while (cur != null && depth++ < 4096) {
            if (cur instanceof RInlineFunction fn) {
                if (fn.parent() instanceof RThenExpr then && then.argument() != null) {
                    return compute(then.argument(), visited, true);
                }
                RExpression opArg = fn.parent() == null
                        ? null
                        : functionalOpArgument(fn.parent());
                if (opArg != null && outputIsListOfLists(opArg, visited)) {
                    return MULTI;
                }
                return SINGLE;
            }
            cur = cur.parent();
        }
        return SINGLE;
    }

    /**
     * The receiver ARGUMENT of a per-element functional operation (the
     * operations that own an {@link RInlineFunction} lambda and bind its
     * implicit item to one INPUT ELEMENT at a time) — the operand upstream's
     * {@code safeIsClosureParameterMulti} non-then arm feeds to
     * {@code isOutputListOfLists}. {@code null} for any other parent (a THEN
     * is handled by its own arm; a non-functional parent defines no
     * per-element item).
     */
    private static RExpression functionalOpArgument(RNode op) {
        return switch (op) {
            case RExtractExpr e -> e.argument();
            case RFilterExpr f -> f.argument();
            case RSortExpr s -> s.argument();
            case RReduceExpr r -> r.argument();
            case RMinExpr m -> m.argument();
            case RMaxExpr m -> m.argument();
            default -> null;
        };
    }

    /**
     * PR #457 (facet ioscoLoLItemCardinality): the fork mirror of upstream
     * {@code CardinalityProvider.safeIsOutputListOfLists} (vendored :218-254)
     * — whether an expression produces a LIST OF LISTS. Upstream does not
     * model the {@code then extract} sugar as implicitly flattening: an
     * {@code extract} whose per-element body is itself a list, applied over a
     * multi stream, yields a NESTED list, and only the
     * {@code CanHandleListOfLists} operations may consume it. A downstream
     * {@code extract}'s item over such an input is a SUB-LIST — multi — which
     * is exactly how the released 9.83.0 artifact reads the iosco cde-v1
     * basket-rule call arguments (`CapacityUnitToISO20022UnitOfMeasure(
     * capacityUnit)` over the `portfolioBasketConstituent -> quantity -> unit`
     * pipe: the V0 drr bank's three bare `Expecting single cardinality` lines
     * at :59/:61/:63 — the #454/#455-recorded under-fire this walk retires).
     *
     * <p>The upstream decision table, arm for arm:
     * <ul>
     *   <li>{@code FlattenOperation} → {@code false} (:219-220) — flatten
     *       un-nests; it implements {@code CanHandleListOfLists} but is
     *       short-circuited before the residual arm. Every other fork
     *       {@link RListOpExpr} kind is not an implementor → the default arm.</li>
     *   <li>{@code MapOperation} (:221-229) → body-multi if the item is
     *       already a sub-list (input LoL), else body-multi AND input multi
     *       (a list-producing body mapped over a plain list nests).</li>
     *   <li>{@code ThenOperation} (:230-235) → its inline body's shape.</li>
     *   <li>closure-param reference (:236-245) → a named param of a THEN's
     *       lambda IS the whole piped value: LoL iff the then's input is (the
     *       fork binds closure params to their declaring
     *       {@link RInlineFunction} — LexicalResolutionPass; any other symbol
     *       — alias, rule, function — is {@code false} upstream: a LoL cannot
     *       be assigned to an alias or output).</li>
     *   <li>implicit variable (:246-248) → definer-is-THEN, same rule.</li>
     *   <li>the residual {@code CanHandleListOfLists} pass-through (:249-252):
     *       the xcore implementor set is EXACTLY {Flatten, Filter, Map, Then}
     *       (RosettaExpression.xcore :314-489), so with the first three
     *       handled above the arm catches {@code FilterOperation} ONLY —
     *       filter narrows the outer list, preserving nesting.</li>
     *   <li>everything else → {@code false} (:253).</li>
     * </ul>
     *
     * <p>Reached only via {@link #computeRuleBody} paths ({@code thenAware}),
     * through {@link #implicitItemCardinalityFrom}'s non-then arm. The walk is
     * structural over the expression TREE (the only symbol hop — a closure
     * param — jumps to the enclosing then's ARGUMENT subtree, strictly
     * outward, so it terminates without a visited set); the nested
     * {@code compute} calls thread {@code visited} for their own rule/alias
     * hops.
     */
    private boolean outputIsListOfLists(RExpression expr, Set<RNode> visited) {
        return switch (expr) {
            case RExtractExpr e -> {
                RInlineFunction fn = e.body();
                if (fn == null || fn.body() == null) {
                    yield false;
                }
                boolean bodyMulti = compute(fn.body(), visited, true) == MULTI;
                RExpression arg = e.argument();
                if (arg != null && outputIsListOfLists(arg, visited)) {
                    yield bodyMulti;
                }
                yield bodyMulti && arg != null && compute(arg, visited, true) == MULTI;
            }
            case RThenExpr t -> {
                var fnOpt = t.body();
                yield fnOpt.isPresent() && fnOpt.get().body() != null
                        && outputIsListOfLists(fnOpt.get().body(), visited);
            }
            case RSymbolReference ref -> ref.symbol()
                    // the closure-param binding in either shape (v3.2 seat 8: the
                    // RClosureParameter node, or the lambda for a name-only param); the
                    // witness of this leg is lane E1 of the seat's lane record ->
                    // LoLItemCardinalityWaveTest.named_then_param_receiver_carries_nesting
                    // (EXACT: a wave fixture types and sizes a declared then-parameter)
                    .flatMap(s -> RInlineFunction.declaringLambdaOf(s, ref.name()))
                    .map(pf -> pf.parent() instanceof RThenExpr then
                            && then.argument() != null
                            && outputIsListOfLists(then.argument(), visited))
                    .orElse(false);
            case RImplicitVariable iv -> {
                RNode cur = iv.parent();
                int depth = 0;
                while (cur != null && depth++ < 4096) {
                    if (cur instanceof RInlineFunction fn) {
                        yield fn.parent() instanceof RThenExpr then
                                && then.argument() != null
                                && outputIsListOfLists(then.argument(), visited);
                    }
                    cur = cur.parent();
                }
                yield false;
            }
            case RFilterExpr f -> f.argument() != null
                    && outputIsListOfLists(f.argument(), visited);
            default -> false;
        };
    }

    /**
     * v3.1 CLOSE-OUT: the public entry to {@link #outputIsListOfLists(RExpression, Set)}
     * (the decision table above) for the {@code flatten} validator — upstream
     * {@code checkFlattenOperation} reads {@code isOutputListOfLists(o.getArgument())}
     * through the same provider. A fresh visited set per question, exactly like the
     * rule-body entry points above.
     */
    public boolean outputIsListOfLists(RExpression expr) {
        return expr != null && outputIsListOfLists(expr, new java.util.HashSet<>());
    }

    /**
     * facet dtccProductGradeCardinality (PR #291): the rule-output cardinality of a
     * DISGUISED 2-name navigation chain ({@code head -> feature}, parsed as an
     * {@link REnumValueRef} when {@code head} did not resolve to a real enum). The
     * parser's type inference already bound the chain's leaf attribute(s) during
     * resolution, so the cardinality reads DIRECTLY off the bound attributes — no
     * {@code gm}/workspace re-rooting is needed (the generator's
     * {@code NavigationHandler.disguisedChainProvesMulti} of PR #288 must re-root
     * because it runs without the parser's resolution context; here we have it).
     *
     * <p>Resolution order mirrors {@code ExpressionTypeComputer.computeEnumValueRef}:
     * the Category-10 implicit-item two-segment {@code resolvedAttributeChain}
     * ({@code payout -> commodityPayout}: {@code attribute()} is the head,
     * {@code feature()} the leaf — null head for the one-segment closure-param shape),
     * then the lexical-head nav {@code resolvedInputFeature} (the leaf feature plus —
     * PR #443 — the re-derived head via {@link #lexicalHeadCardinality}). The
     * chain is MULTI iff the HEAD is multi (the whole navigated list is a list) OR the
     * LEAF is multi — mirroring {@code disguisedChainProvesMulti}'s head-then-leaf check.
     * A real enum constant, a choice-option narrowing ({@code resolvedChoiceOption}), a
     * downcast ({@code resolvedTypeRestriction}) and a record-field nav
     * ({@code resolvedSymbol}) stay conservative SINGLE — no carrier needs them and
     * leaving them SINGLE preserves the prior bytes (monotone, under-fire is safe).
     *
     * <p>Reached only via {@link #computeRuleBody(RExpression)} ({@code thenAware});
     * the common reporting rule {@code DTCC_ProductGrade} body
     * {@code … then extract payout -> commodityPayout then extract ProductGradeReport{…}}
     * is multi-valued ({@code commodityPayout} is {@code 0..*}), so its
     * {@code ReportFunction<I,O>} output type-arg becomes {@code List<? extends
     * ProductGradeReport>} and the whole cascade renders MULTI — matching golden; the
     * cftc/csa {@code extract commondtcc.DTCC_ProductGrade} delegations recurse into the
     * common rule's body via the {@link #computeSymbolRefCardinality} {@link RRule} case
     * (PR #273).
     */
    private ExpressionCardinality disguisedChainCardinality(REnumValueRef evr, Set<RNode> visited) {
        // facet reportOutputFunctionChainCardinality (PR #307): a FUNCTION-headed disguised
        // chain (`<fn> -> feature`, resolvedSymbol = RFunction) is MULTI when the function's
        // OUTPUT is multi — the function is invoked PER ITEM and produces a list per item
        // (GetBasketConstituentsProductIdentifier: `trade (1..1)` -> `productIdentifiers (0..*)`),
        // and the per-element leaf navigation (`-> source`) preserves the list cardinality. The
        // resolvedAttributeChain / resolvedInputFeature checks below resolve only an ATTRIBUTE
        // head (head as a feature of the item type), so a function head reads SINGLE — the
        // BasketConstituent reporting-rule family (SourceOfTheIdentifier, BasketConstituentIdentifier*,
        // BasketConstituentUnitOfMeasure, IdentifierOfBasketConstituents) whose
        // `then extract [GetBasketConstituents… -> X]` whole-output body wrongly read SINGLE
        // (`ReportFunction<I, T>` + `mapSingleToItem(...).get()` where golden carries
        // `ReportFunction<I, List<T>>` + `mapSingleToList(...).getMulti()`).
        //
        // Gated on the function OUTPUT being multi: a function whose INPUT is the list and OUTPUT
        // is SINGLE (GetProductIdentifierFilteringISIN: `productIdentifiers (0..*)` ->
        // `productIdentifier (0..1)`) CONSUMES + COLLAPSES the list, so its `<fn> -> feature` chain
        // stays SINGLE — the distinct PR #291 collapse case (isElementWiseThenBody's RFunction
        // exclusion handles it at the then-body seat). Green-safe by construction: a
        // `<multi-output-fn> -> feature` chain IS a list (golden always renders it multi), so
        // moving the fork to MULTI only moves toward golden; the function-OUTPUT-single case
        // (collapse) and every attribute-headed chain are untouched. Reached only via
        // computeRuleBody (thenAware) → FUNCTION-byte-neutral by construction (the global compute
        // reads REnumValueRef as SINGLE).
        var sym = evr.resolvedSymbol();
        if (sym.isPresent() && sym.get() instanceof RFunction fn
                && fn.output().map(this::attributeCardinality).orElse(SINGLE) == MULTI) {
            return MULTI;
        }
        var chain = evr.resolvedAttributeChain();
        if (chain.isPresent()) {
            RAttribute head = chain.get().attribute(); // null for the one-segment closure-param shape
            if (head != null && attributeCardinality(head) == MULTI) {
                return MULTI;
            }
            // NB (PR #454): upstream's receiver OR would ALSO reach the implicit
            // item here (a single-declared Cat-10 chain over a MULTI then-item is
            // a list upstream), but adding that OR at THIS seat moved 6 drr
            // BasketConstituent POJO goldens at the live cp probe (the rule
            // whole-output cardinality back-fill consumes this read, and the
            // `then extract <chain>` sugar puts the chain in the THEN's own
            // inline function, so the OR flips those rule outputs to List where
            // golden is single) — the #449 probed-byte-risk law. The validator's
            // only-element seat supplements the read locally instead
            // (ExpressionValidator.thenItemContextMulti); the core read stays
            // byte-frozen.
            return attributeCardinality(chain.get().feature());
        }
        var inputFeature = evr.resolvedInputFeature();
        if (inputFeature.isPresent()) {
            // PR #443: the lexical-head nav arm is HEAD-AWARE — upstream's
            // CardinalityProvider.caseFeatureCall ORs the receiver
            // (isFeatureMulti(feature) || isMulti(receiver)), and the head IS
            // the receiver here. The leaf-only read was the #443 population
            // re-seed's measured false-positive mechanism: `[…enum list…] <>
            // otherPayment -> currency` fired the all-any cardinality ERROR
            // because `currency (0..1)` read SINGLE while the head input
            // `otherPayment (1..*)` — which upstream ORs in — was invisible,
            // where upstream is corpus-silent (the V0-DRR oracle). The head is
            // re-derived lexically (input / alias / condition-type attribute —
            // three of the four shapes the engine binds into
            // resolvedInputFeature; the FOURTH, the #449 function-OUTPUT head,
            // has no arm in lexicalHeadCardinality and falls to its documented
            // unmatched-head SINGLE under-fire — the recorded follow-on).
            if (attributeCardinality(inputFeature.get()) == MULTI) {
                return MULTI;
            }
            return lexicalHeadCardinality(evr, visited);
        }
        return SINGLE;
    }

    /**
     * PR #443: the cardinality of a lexical-head nav's HEAD — the enclosing
     * function's input or shortcut matching the leading name, or the attribute
     * of the data type declaring the enclosing condition (three of the FOUR
     * {@code resolvedInputFeature} binding shapes, re-derived by the same
     * lexical walks the engine's binding channels use; the fourth — the #449
     * function-OUTPUT head — has NO arm here and deliberately falls to the
     * unmatched-head SINGLE under-fire below, the recorded follow-on: a
     * head-aware output arm needs its own probe). A shortcut head carries
     * its EXPRESSION's cardinality (the {@link #computeSymbolRefCardinality}
     * RShortcut law), guarded against self-referential alias cycles via the
     * shared {@code visited} identity set. An unmatched head stays SINGLE —
     * under-fire keeps today's bytes.
     */
    private ExpressionCardinality lexicalHeadCardinality(REnumValueRef evr, Set<RNode> visited) {
        String head = evr.enumName();
        if (head == null || head.isEmpty()) {
            return SINGLE;
        }
        // Walk note: the RCondition arm below claims the walk ONLY for a
        // DATA-TYPE condition (a declaring RDataType ancestor exists); a
        // FUNCTION pre/post condition falls through to the RFunction arm so
        // input-headed navs inside function conditions stay head-aware
        // (Copilot #443 R1 — an early return there would silently drop the
        // head and re-arm the list-vs-single fabrication class this arm
        // exists to kill; locked by the function-condition carrier in
        // SmokeTypeInferenceTest.lexical_head_cardinality_is_head_aware).
        // The shortcuts arm went LIVE at the #447 typed-alias-nav wave (the
        // engine's alias channel now binds resolvedInputFeature), and the
        // binding order is aligned with this walk's inputs-first order: the
        // engine consults the input channel before the alias arm, exactly as
        // this walk reads inputs before shortcuts.
        for (RNode anc = evr.parent(); anc != null; anc = anc.parent()) {
            if (anc instanceof RFunction fn) {
                for (RAttribute in : fn.inputs()) {
                    if (head.equals(in.name())) {
                        return attributeCardinality(in);
                    }
                }
                for (RShortcut sc : fn.shortcuts()) {
                    if (head.equals(sc.name()) && sc.expression() != null) {
                        Set<RNode> v = visited != null
                                ? visited
                                : Collections.newSetFromMap(new IdentityHashMap<>());
                        if (!v.add(sc)) {
                            return SINGLE;
                        }
                        return compute(sc.expression(), v, true);
                    }
                }
                // The implicit lowercase-input-type-name convention (the
                // engine's enclosingFunctionInput second loop): a unique input
                // whose type simple-name lower-cases to the head.
                RAttribute implicitMatch = null;
                for (RAttribute in : fn.inputs()) {
                    var tc = in.typeCall();
                    if (tc == null || tc.typeName() == null) continue;
                    String simple = tc.typeName();
                    int dot = simple.lastIndexOf('.');
                    if (dot >= 0) simple = simple.substring(dot + 1);
                    if (!simple.isEmpty()
                            && (Character.toLowerCase(simple.charAt(0)) + simple.substring(1))
                                .equals(head)) {
                        if (implicitMatch != null) {
                            return SINGLE;
                        }
                        implicitMatch = in;
                    }
                }
                return implicitMatch != null ? attributeCardinality(implicitMatch) : SINGLE;
            }
            if (anc instanceof com.regnosys.rosetta.ast.functions.RCondition condition) {
                var declaring = com.regnosys.rosetta.ast.util.AstWalker
                        .findAncestor(condition, com.regnosys.rosetta.ast.types.RDataType.class)
                        .orElse(null);
                if (declaring == null) {
                    // A FUNCTION pre/post condition — not this arm's context;
                    // keep walking so the RFunction arm resolves the head.
                    continue;
                }
                Set<RNode> seenTypes = Collections.newSetFromMap(new IdentityHashMap<>());
                for (com.regnosys.rosetta.ast.types.RDataType t = declaring;
                        t != null && seenTypes.add(t);
                        t = t.superType().orElse(null)) {
                    for (RAttribute attr : t.attributes()) {
                        if (head.equals(attr.name())) {
                            return attributeCardinality(attr);
                        }
                    }
                }
                return SINGLE;
            }
        }
        return SINGLE;
    }

    /**
     * Mirrors upstream {@code CardinalityProvider#caseMapOperation}: an
     * {@code extract} is multi iff its inline-function body is multi OR its
     * receiver argument is multi.
     */
    private ExpressionCardinality extractCardinality(RExtractExpr e,
            Set<RNode> visited, boolean thenAware) {
        var fn = e.body();
        if (fn != null && fn.body() != null && compute(fn.body(), visited, thenAware) == MULTI) {
            return MULTI;
        }
        var arg = e.argument();
        if (arg != null && compute(arg, visited, thenAware) == MULTI) {
            return MULTI;
        }
        return SINGLE;
    }

    /**
     * Mirrors upstream {@code CardinalityProvider#caseFilterOperation}: a
     * {@code filter} is multi iff its argument is multi (filter narrows a list
     * but does not change cardinality category).
     */
    private ExpressionCardinality filterCardinality(RFilterExpr f,
            Set<RNode> visited, boolean thenAware) {
        var arg = f.argument();
        if (arg != null && compute(arg, visited, thenAware) == MULTI) {
            return MULTI;
        }
        return SINGLE;
    }

    /**
     * facet reportOutputConditionalCardinality (PR #289): the rule-output cardinality
     * of an if-then-else EXPRESSION is the MAX of its arms' cardinalities — mirroring
     * upstream {@code CardinalityProvider}'s {@code caseConditionalExpression}
     * ({@code isMulti(ifthen) || isMulti(elsethen)}). The global {@link #compute} path
     * keeps the conservative {@code SINGLE} (the function tail's {@code MapperS}
     * ite-hoist rendering — {@code appendIteHoistChainCore} — depends on it); the
     * rule-output path ({@code thenAware}) computes it faithfully, so a reporting rule
     * {@code filter … then if cond then <multi sub-rule> else <multi sub-rule>}
     * (NotionalQuantityScheduleLeg1/2, NotionalAmountScheduleLeg1/2) is multi-valued —
     * its {@code ReportFunction<I,O>} output type-arg becomes {@code List<? extends X>},
     * the conditional ite-hoist declares {@code MapperC} and the body terminal collapses
     * with {@code .getMulti()}. Each arm recurses with {@code thenAware} so a sub-rule
     * arm carries its OWN body cardinality (the #273 {@link RRule} recursion) and a
     * nested {@code else if} ladder (an {@link RConditionalExpr} else-branch) is computed
     * end-to-end. A single-literal-armed aggregate conditional ({@code … then if exists(…)
     * then True else False}, Counterparty1FederalEntityIndicator) stays SINGLE (both arms
     * single). The {@code visited} set is threaded through both arms; sharing it can only
     * UNDER-report a cross-arm self-referential rule chain (SINGLE) — never over-report —
     * and the then-branch MULTI short-circuit avoids it for the carriers (distinct arms).
     */
    private ExpressionCardinality conditionalCardinality(RConditionalExpr c, Set<RNode> visited) {
        if (compute(c.thenBranch(), visited, true) == MULTI) {
            return MULTI;
        }
        // An ELSELESS conditional (`if cond then X`, no else) injects a SYNTHETIC empty-list
        // else (the DefaultElseRule) — an empty {@link RListLiteral}, which reads MULTI above.
        // That would spuriously make EVERY elseless conditional multi (the green enrichment /
        // margin rules `… then if <cond> then <single fn-call>` whose golden renders the single
        // empty-else default `MapperS.<T>ofNull()` — UpiPreEnrichmentData, ActionType). An
        // elseless else carries no value, so the conditional's cardinality is the then-branch's
        // (SINGLE here, since the MULTI then short-circuited above). Exclude both an ABSENT else
        // and the synthetic empty-list else; a REAL else (`else <multi sub-rule>`,
        // NotionalQuantityScheduleLeg1/2) is computed normally.
        java.util.Optional<RExpression> elseBranch = c.elseBranch();
        if (elseBranch.isEmpty() || isEmptyListLiteral(elseBranch.get())) {
            return SINGLE;
        }
        return compute(elseBranch.get(), visited, true);
    }

    private static boolean isEmptyListLiteral(RExpression expr) {
        return expr instanceof RListLiteral list && list.elements().isEmpty();
    }

    /**
     * Cardinality of a {@code then} pipe ({@code argument then [body]}) on the
     * rule-output path. Mirrors upstream {@code CardinalityProvider}'s
     * inline-function handling: the body maps over the piped argument, so the
     * result is multi iff the argument is multi (and the body does not collapse
     * it) OR the per-item body is itself multi. A collapsing list-op body
     * ({@code first}/{@code last}/{@code only-element}/{@code sum}) reduces the
     * piped list to a single value regardless of the argument's cardinality.
     *
     * <p>Reached only via {@link #computeRuleBody(RExpression)} ({@code thenAware
     * == true}); the recursive {@code compute} calls thread {@code thenAware} so
     * a nested {@code then}-chain (the left-associative {@code A then B then C})
     * propagates a multi step from an interior pipe through to the outer result
     * (the {@code ClearingThresholdOfCounterparty1} shape, where the multi enters
     * at the {@code then extract ExtractRegimeInformation(…)} function call).
     */
    private ExpressionCardinality thenCardinality(RThenExpr e, Set<RNode> visited) {
        ExpressionCardinality argCard = e.argument() == null
                ? SINGLE
                : compute(e.argument(), visited, true);
        var bodyOpt = e.body();
        if (bodyOpt.isEmpty() || bodyOpt.get().body() == null) {
            // A bare `then` with no inline body passes the argument through.
            return argCard;
        }
        RExpression bodyExpr = bodyOpt.get().body();
        ExpressionCardinality bodyCard = compute(bodyExpr, visited, true);
        // The body itself produces a multi value per item (a multi function call,
        // a flatten, a multi nav) → multi regardless of the argument.
        if (bodyCard == MULTI) {
            return MULTI;
        }
        // Element-wise body (a navigation / extract / filter / sort over the piped
        // item) MAPS over the argument, so a multi argument yields a multi result.
        // An AGGREGATE body (a conditional / comparison / exists / count / reduce /
        // collapsing list-op) reduces the whole piped list to a single value, so the
        // result is single regardless of the argument's cardinality (the
        // Counterparty1FederalEntityIndicator shape: `… then if exists(…) then True
        // else False` is single even though the piped `thenArg1` is multi). The
        // whitelist of element-wise shapes UNDER-fires safely — an unlisted body just
        // stays single (no over-fire regression) rather than wrongly going multi.
        return (argCard == MULTI && isElementWiseThenBody(bodyExpr)) ? MULTI : SINGLE;
    }

    /**
     * Whether a {@code then}-body MAPS over the piped argument element-wise
     * (preserving its cardinality) rather than AGGREGATING the whole list to a
     * single value. Used by {@link #thenCardinality} to decide whether a multi
     * argument propagates through the pipe. Conservative (whitelist): only the
     * clearly element-wise navigation/map/filter/sort shapes return {@code true};
     * everything else (conditional, comparison, boolean predicate, count/exists,
     * reduce/min/max, collapsing list-op, literal) is treated as single-producing.
     */
    private static boolean isElementWiseThenBody(RExpression body) {
        return switch (body) {
            // PR #454 (facet annexAReleasedSeverities' cardinality decode): a nav
            // chain is element-wise ONLY when its DEEP HEAD maps per-item. A chain
            // headed by a COLLAPSING list-op (`then only-element -> a -> b`, the
            // enrichment-upi Compute_IndexTermValue argument shape) evaluates the
            // collapse ONCE on the whole piped list and navigates the single
            // result — upstream reads the pipe SINGLE (the V0 banks are silent at
            // all six upi arg sites); before this walk the blanket `true` here
            // propagated the argument's MULTI through the collapsed chain.
            case RFeatureCall fc -> !navHeadCollapses(fc);
            case RDeepFeatureCall dfc -> !navHeadCollapses(dfc);
            // A disguised 2-name `head -> feature` navigation parses as an
            // REnumValueRef (the recurring fork shape); a NAV chain is element-wise
            // like any nav. An actual enum CONSTANT (`then SomeEnum.VALUE`) has its
            // enumeration resolved and is single-producing — exclude it. A
            // FUNCTION-headed chain (`<fn> -> <feature>`, resolvedSymbol = RFunction)
            // is NOT element-wise: the function is applied to the WHOLE piped list
            // (it consumes the list and returns its own output cardinality), so a
            // multi argument does NOT propagate through it — exclude it too (PR #291:
            // the BasketConstituentIdentifierSource shape `<multi conditional> then
            // GetProductIdentifierFilteringISIN -> source`, where the function
            // collapses the multi ProductIdentifier list to a single ProductIdTypeEnum;
            // before dtccProductGradeCardinality made the disguised-chain arms multi
            // this body was never reached with a multi argument, so the gap was latent).
            // NB: resolvedSymbol can ALSO be an RRule (GlobalResolutionPass's T0h Gap A
            // "callable symbol" arm binds RFunction OR RRule), but the exclusion is
            // DELIBERATELY RFunction-only — a callable head does NOT "always collapse".
            // A function's input is the list (`GetProductIdentifierFilteringISIN` takes
            // `ProductIdentifier (0..*)`) so it consumes the whole piped list. A reporting
            // RULE in a then-chain is invoked PER-ITEM (`mapSingleToItem`) — its input is
            // a single object — so a rule-headed body over a multi argument IS element-
            // wise (the multi propagates), exactly like a plain nav. Excluding RRule too
            // would mis-collapse a per-item rule-headed chain (a latent over-narrowing).
            // The symmetric "exclude RRule" tweak was byte-oracle-clean over today's
            // corpus (no `<multi arg> then <rule> -> <feature>` carrier exists) but is
            // semantically wrong; a rule-headed chain whose rule genuinely collapses is a
            // separate deferred cardinality facet, not this exclusion.
            case REnumValueRef evr -> evr.enumeration().isEmpty()
                    && !evr.resolvedSymbol().map(s -> s instanceof RFunction).orElse(false);
            // PR #455 (facet warningFamilyWaves' cardinality decode): a
            // functional op is element-wise ONLY when its receiver chain's
            // deep head maps per-item — `then first extract item -> id` (the
            // enrichment-upi UnderlierID shapes) collapses the piped list at
            // `first` BEFORE the extract, so the body is single-producing
            // (upstream reads the pipe SINGLE; the V0 banks are silent at all
            // four upi sites). The shared walk steps through the functional
            // receivers exactly like the nav arms' heads.
            case RExtractExpr e -> !navHeadCollapses(e);
            case RFilterExpr f -> !navHeadCollapses(f);
            case RSortExpr s -> !navHeadCollapses(s);
            case RImplicitVariable iv -> true;     // passes the piped list through
            // NOTE: a `then <list-literal>` is NOT listed — a list literal does not "map over"
            // the piped argument; {@link #compute} reads an {@link RListLiteral} as MULTI directly,
            // so {@link #thenCardinality}'s {@code bodyCard == MULTI} fast-path already returns MULTI
            // before this whitelist is consulted (the case here was dead, and contradicted this
            // method's own "literal → single-producing" contract). Removed per PR #272 Copilot R1.
            // NOTE: a `then <constructor>` / `then <with-meta>` / `then <default>` is NOT
            // listed — upstream CardinalityProvider.caseConstructorExpression /
            // caseWithMetaOperation return single UNCONDITIONALLY (the body is evaluated
            // ONCE on the whole piped value, not per-item), and caseDefaultOperation is
            // operand-driven (isMulti(left)||isMulti(right)); marking them element-wise
            // would OVER-fire (the wrong direction). They fall to the default-single arm
            // (conservative under-fire) until a real multi carrier needs the operand-aware
            // default — none exists in the corpus today (byte-oracle / regscan confirm).
            case RListOpExpr lop -> switch (lop.op()) {
                case FLATTEN, DISTINCT, REVERSE -> true;          // cardinality-preserving
                case FIRST, LAST, ONLY_ELEMENT, SUM -> false;     // collapsing
            };
            default -> false;
        };
    }

    /**
     * Whether a then-body navigation chain's DEEP HEAD collapses the piped
     * list — a COLLAPSING list-op head ({@code first}/{@code last}/{@code
     * only-element}/{@code sum}, the same set {@link #isElementWiseThenBody}'s
     * own list-op arm treats as collapsing) evaluated over the implicit item
     * reduces the whole list before the navigation, so the chain is
     * single-producing. Any other head (implicit / symbol / disguised nav /
     * parenthesized sub-chain) keeps the pre-#454 element-wise read.
     */
    private static boolean navHeadCollapses(RExpression nav) {
        RExpression head = nav;
        while (true) {
            if (head instanceof RFeatureCall fc && fc.receiver() != null) {
                head = fc.receiver();
            } else if (head instanceof RDeepFeatureCall dfc && dfc.receiver() != null) {
                head = dfc.receiver();
            // PR #455: the walk steps through functional receivers too — the
            // deep head of `first extract item -> id` is the collapsing
            // `first` (see isElementWiseThenBody's functional arms). A bare
            // body-position functional (implicit receiver, argument null)
            // breaks out unchanged.
            } else if (head instanceof RExtractExpr ex && ex.argument() != null) {
                head = ex.argument();
            } else if (head instanceof RFilterExpr fl && fl.argument() != null) {
                head = fl.argument();
            } else if (head instanceof RSortExpr so && so.argument() != null) {
                head = so.argument();
            } else {
                break;
            }
        }
        return head instanceof RListOpExpr lop
                && switch (lop.op()) {
                    case FIRST, LAST, ONLY_ELEMENT, SUM -> true;
                    case FLATTEN, DISTINCT, REVERSE -> false;
                };
    }

    private ExpressionCardinality attributeCardinality(RAttribute attr) {
        var cardOpt = attr.cardinality();
        if (cardOpt.isEmpty()) return SINGLE;
        RCardinality card = cardOpt.get();
        if (card.isUnbounded()) return MULTI;
        if (card.sup() != null && card.sup().compareTo(BigInteger.ONE) > 0) return MULTI;
        return SINGLE;
    }
}
