package com.regnosys.rosetta.types.inference;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.expressions.binary.RThenExpr;
import com.regnosys.rosetta.ast.expressions.references.*;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCase;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCaseGuard;
import com.regnosys.rosetta.ast.expressions.unary.RConversionExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExtractExpr;
import com.regnosys.rosetta.ast.expressions.unary.RFilterExpr;
import com.regnosys.rosetta.ast.expressions.unary.RListOpExpr;
import com.regnosys.rosetta.ast.expressions.unary.RCountExpr;
import com.regnosys.rosetta.ast.expressions.unary.RMaxExpr;
import com.regnosys.rosetta.ast.expressions.unary.RMinExpr;
import com.regnosys.rosetta.ast.expressions.unary.RReduceExpr;
import com.regnosys.rosetta.ast.expressions.unary.RSortExpr;
import com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr;
import com.regnosys.rosetta.ast.expressions.unary.RToStringExpr;
import com.regnosys.rosetta.ast.functions.ROperation;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.functions.RSegment;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.diagnostics.Diagnostics;
import com.regnosys.rosetta.symbols.index.ReferenceIndex;
import com.regnosys.rosetta.types.*;

import java.util.*;

/**
 * Fixed-point type inference engine. Iterates over all expressions in
 * the workspace until types stabilize or max iterations reached (D2).
 * Interleaves type-directed resolution with inference.
 *
 * <p>Spec: sections 3.4 + 3.9 in {@code docs/specs/2026-04-08-m4-type-system-design.md}.
 */
public final class TypeInferenceEngine {

    // PR #330: raised 10 -> 40. The loop exits at STABILITY (no type change + no
    // Phase A change), so the budget is a runaway backstop, not a cost: a converged
    // workspace pays only its natural pass count. The drr-6.34.1 workspace (the
    // deepest cross-namespace rule-delegation web in the corpus) converges at 15
    // passes — the old budget of 10 cut it off mid-convergence, leaving every
    // consumer positioned BEFORE its target in phaseBOrder (reverse file order:
    // e.g. csa reads the common rule one pass staler than cftc does) with a
    // permanently-MISSING type. That budget shortfall was the sole cause of 24 of
    // the 25 drr no-emit Rule POJOs (RuleGenerator's RMissingType fail-fast);
    // the 25th is the ReportingTimestamp self-cycle (see ExpressionTypeComputer.
    // inferTypeOfNode). 40 = ~2.6x the deepest observed need.
    public static final int MAX_ITERATIONS = 40;

    private final ExpressionTypeComputer typeComputer;
    private TypeDirectedResolver resolver;
    private ReferenceIndex referenceIndex;
    private final Map<RExpression, RMetaAnnotatedType> inferredTypes = new IdentityHashMap<>();
    private int iterationCount;

    public TypeInferenceEngine(ExpressionTypeComputer typeComputer) {
        this.typeComputer = Objects.requireNonNull(typeComputer);
    }

    /**
     * Sets the type-directed resolver for interleaved resolution. Both
     * {@link #setResolver} and {@link #setReferenceIndex} keep the resolver's
     * listener in sync with the current pair of dependencies — wired when both
     * are non-null, cleared when either is null. Configuration order is
     * irrelevant (Copilot PR #76 R12 F1); subsequent unset of {@code referenceIndex}
     * also clears the listener (Copilot PR #76 R14 F1).
     *
     * <p>When the resolver is being replaced or unset, the previously-stored
     * resolver's listener is detached BEFORE the field is overwritten so the
     * old resolver cannot continue calling {@code referenceIndex::registerReference}
     * via callers that still hold a direct reference to it (Copilot PR #76 R16
     * F1 — earlier R14 fix relied on {@link #rewireResolutionListener()} alone,
     * which short-circuits on a null new resolver and so left the OLD resolver's
     * listener intact).
     *
     * <p>Re-setting with the same resolver instance skips the explicit detach
     * (identity guard) but still calls {@link #rewireResolutionListener()}; the
     * net effect is idempotent — the resolver's listener ends in the state
     * determined by the current {@code (resolver, referenceIndex)} pair, which
     * for an identical pair is the same listener it already had (Copilot PR #76
     * R17 F3 — earlier wording loosely called this a "no-op", which understates
     * that the rewire still runs).
     */
    public void setResolver(TypeDirectedResolver resolver) {
        TypeDirectedResolver previous = this.resolver;
        this.resolver = resolver;
        // R16 F1: detach the previously-wired listener from the OLD resolver
        // before rewiring. Without this, swapping or unsetting the resolver
        // leaves the old resolver's listener pointing at the engine's
        // referenceIndex, allowing late resolveX() calls on the orphaned
        // resolver to keep registering into the index.
        if (previous != null && previous != resolver) {
            previous.setResolutionListener(null);
        }
        rewireResolutionListener();
    }

    /**
     * Wires the workspace's {@link ReferenceIndex} so type-directed resolution
     * (Cat 9 + Cat 10 on this engine; Cat 1 / 2 / 4 / 7 + external attributes /
     * enum values / annotation segments / switch guards via the resolver's
     * {@link TypeDirectedResolver.ResolutionListener}) can register
     * cross-references it discovers — keeping
     * {@code RWorkspace.findReferences(target)} consistent with the resolutions
     * stamped onto AST nodes via {@code setResolvedSymbol} /
     * {@code setResolvedAttributeChain} / {@code setResolvedFeature} /
     * {@code setResolvedAttribute} / {@code setResolvedEnumValue} /
     * {@code setResolvedGuard}.
     *
     * <p>Fix for Copilot PR #76 R11 F1 — without this wiring, late-resolved
     * symbol references set in pass 6 were invisible to the reverse index built
     * by passes 4 + 5 (GlobalResolutionPass + LexicalResolutionPass both pair
     * every {@code setResolvedSymbol} with a {@code referenceIndex.registerReference}
     * call; Cat 9 / Cat 10 broke that contract). Class-of-issue sweep extended
     * the fix to every resolution-stamp setter in {@link TypeDirectedResolver}
     * via its {@code ResolutionListener} callback. LSP find-references + any
     * invariant built atop the index now sees these resolutions too.
     *
     * <p>Optional — if null (e.g. test paths that don't construct a workspace),
     * Cat 9 + Cat 10 + resolver paths still resolve correctly; only the
     * reverse-index registration is skipped. Mirrors the {@link #setResolver}
     * optional-dependency pattern. Setting {@code referenceIndex} to null
     * after a previous non-null set clears any previously-wired listener via
     * {@link #rewireResolutionListener()} so callers can opt out of registration
     * at any time (Copilot PR #76 R14 F1).
     */
    public void setReferenceIndex(ReferenceIndex referenceIndex) {
        this.referenceIndex = referenceIndex;
        rewireResolutionListener();
    }

    /**
     * R10 of {@code docs/specs/2026-08-15-upstream-resolution-spec.md} — finds
     * the exported {@code metaType} ROOT ELEMENT a meta name refers to, in the
     * scope of the file the reference sits in.
     *
     * <p>Upstream appends a type's meta DESCRIPTIONS to the very same feature
     * scope its attributes come from, and those descriptions are resolved by
     * {@code RosettaConfigExtension.findMetaTypes} against exported
     * {@code metaType} declarations — <b>not</b> against the {@code metadata}
     * annotation's own attributes. cdm declares them in {@code base-desc.rosetta};
     * a self-contained model must declare its own or the name does not resolve.
     */
    @FunctionalInterface
    public interface MetaTypeLookup {
        /** The {@code metaType} declaration named {@code name} visible from {@code context}, if any. */
        java.util.Optional<com.regnosys.rosetta.ast.types.RMetaType> find(RNode context, String name);
    }

    private MetaTypeLookup metaTypeLookup;

    /**
     * The {@link RAttribute} a receiver expression resolved to, or {@code null}
     * — the node carrying any {@code [metadata ...]} annotations the receiver
     * has.
     *
     * <p>All three receiver shapes are covered because the grammar decides
     * which one you get, not the author: {@code holder -> tagged -> key} puts
     * an {@link REnumValueRef} under the meta call (the fork parses every
     * {@code <name> -> <name>} that way, § 7 of the spec), while a receiver
     * that is itself a longer navigation arrives as an {@link RFeatureCall}
     * and a bare one as an {@link RSymbolReference}. Missing the REnumValueRef
     * arm silently disables R10 on the corpus's own shape.
     */
    private static RAttribute attributeBehind(RExpression receiver) {
        if (receiver instanceof RFeatureCall fc) {
            return fc.resolvedFeature().orElse(null);
        }
        if (receiver instanceof REnumValueRef enr) {
            // Authority first (v3.1 C1), then the legacy channels that carry
            // the same navigation for nodes the rebuild has not reached.
            RAttribute authority = enr.resolvedFeatureNode()
                    .filter(RAttribute.class::isInstance)
                    .map(RAttribute.class::cast)
                    .orElse(null);
            if (authority != null) {
                return authority;
            }
            return enr.resolvedAttributeChain()
                    .map(chain -> chain.feature())
                    .or(enr::resolvedInputFeature)
                    .orElse(null);
        }
        if (receiver instanceof RSymbolReference sr) {
            RNode bound = sr.symbol().orElse(null);
            if (bound instanceof RAttribute attribute) {
                return attribute;
            }
            return sr.resolvedFeatureNode()
                    .filter(RAttribute.class::isInstance)
                    .map(RAttribute.class::cast)
                    .orElse(null);
        }
        return null;
    }

    /**
     * Wires the R10 meta-type channel. Optional, like {@link #setResolver} and
     * {@link #setReferenceIndex}: without it a meta feature simply stays
     * unresolved, which is the pre-C1 behaviour, so test paths that build no
     * workspace are unaffected.
     */
    public void setMetaTypeLookup(MetaTypeLookup metaTypeLookup) {
        this.metaTypeLookup = metaTypeLookup;
    }

    /**
     * Updates the resolver's listener to reflect the current pair of
     * dependencies. Called by both {@link #setResolver} and
     * {@link #setReferenceIndex} so the listener state is a pure function of
     * the latest {@code (resolver, referenceIndex)} pair:
     * <ul>
     *   <li>{@code resolver == null}: no-op (nowhere to set a listener);</li>
     *   <li>{@code resolver != null && referenceIndex == null}: clear the
     *       resolver's listener so any previously-wired
     *       {@code registerReference} closure is detached (Copilot PR #76
     *       R14 F1 — javadoc on {@link #setReferenceIndex} promises the
     *       optional-dependency semantic; without explicit clearing the
     *       previous closure leaked beyond a null re-set);</li>
     *   <li>{@code resolver != null && referenceIndex != null}: wire the
     *       resolver's listener to {@code referenceIndex::registerReference}.</li>
     * </ul>
     */
    private void rewireResolutionListener() {
        if (resolver == null) return;
        resolver.setResolutionListener(referenceIndex != null ? referenceIndex::registerReference : null);
    }

    /**
     * Runs type inference on all files. Populates inferredTypes map,
     * runs type-directed resolution, and emits diagnostics.
     */
    public void run(List<RModel> files, Diagnostics collector) {
        // Step 1: Collect all expressions and initialize to MISSING
        List<RExpression> allExprs = new ArrayList<>();
        for (RModel file : files) {
            List<RExpression> exprs = AstWalker.findAll(file, RExpression.class);
            allExprs.addAll(exprs);
            for (RExpression expr : exprs) {
                inferredTypes.put(expr, RMetaAnnotatedType.MISSING);
            }
        }

        if (allExprs.isEmpty()) {
            iterationCount = 0;
            return;
        }

        // Phase X1 closure (CLOSURE T9) — post-order (children-first) Phase B
        // re-compute order. `AstWalker.findAll` yields pre-order (each parent
        // before its descendants); reversing it gives post-order, so every
        // expression's children are re-typed BEFORE the expression itself
        // within a single pass. The fixed point's result is order-independent,
        // but post-order makes deep dependency chains converge bottom-up in one
        // pass instead of one-level-per-iteration. The Phase X1 grammar fix
        // (CLOSURE T8) restored deeply-nested then/conditional chains (e.g. the
        // ~15-deep DRR DayCountConvention `if/else if` enum-mapping ladder, plus
        // cross-namespace rule-to-rule cascades like cdeV3 -> cdeV2 -> cdeV1);
        // in pre-order those needed more than MAX_ITERATIONS passes to converge,
        // so the outer node was left computed against still-MISSING children and
        // never recomputed. Post-order converges DESCENDANT chains in one pass —
        // but a CROSS-RULE read (inferTypeOfNode's RRule memo read) is positional,
        // not structural: a consumer whose file sorts AFTER its target file in the
        // (sorted-path) load order sits BEFORE the target in this reversed list and
        // reads a one-pass-stale memo. PR #330 measured the drr workspace's true
        // convergence at 15 passes and raised MAX_ITERATIONS to cover it (the
        // earlier note here claiming a 5x budget bump changed nothing was measured
        // BEFORE the Phase X1 PR #76 RRule branch made cross-rule chains this deep,
        // and was falsified at #330: budget 40 types every delegator + the
        // CustomBasketCode root that budget 10 left MISSING). Phase A still walks
        // pre-order (`allExprs`); resolution interleaving converges regardless.
        List<RExpression> phaseBOrder = new ArrayList<>(allExprs);
        java.util.Collections.reverse(phaseBOrder);

        // Step 2: Fixed-point iteration with interleaved resolution
        for (iterationCount = 1; iterationCount <= MAX_ITERATIONS; iterationCount++) {
            boolean changed = false;

            // Phase A: Type-directed resolution using current inferred types.
            // The mutable `collector` is threaded as a method parameter (not
            // stored on the engine) so it doesn't get retained inside the
            // workspace alongside the already-copied immutable snapshot.
            //
            // Phase A may mutate `inferredTypes` directly via Cat 8 (writes
            // `inferredTypes.put(iv, itemType)` for RImplicitVariable). That
            // mutation is invisible to Phase B's change-detector below
            // (compute(iv) reads back the same value just stored). Without
            // signalling via the returned `phaseAChanged` flag, parents that
            // need to re-fire Cat 1 against the freshly-typed receiver could
            // miss their second iteration. Returning the Cat 8 mutation flag
            // ensures the outer loop iterates again when Phase A made any
            // direct map mutation.
            boolean phaseAChanged = false;
            if (resolver != null) {
                phaseAChanged = runTypeDirectedResolution(allExprs, collector);
            }

            // Phase B: Re-compute types with updated resolution
            for (RExpression expr : phaseBOrder) {
                RMetaAnnotatedType computed = typeComputer.compute(expr, this);
                RMetaAnnotatedType previous = inferredTypes.get(expr);
                if (!computed.equals(previous)) {
                    inferredTypes.put(expr, computed);
                    changed = true;
                }
            }
            if (!changed && !phaseAChanged) break; // Stable — all types converged
        }

        // Step 3: Diagnostics — only for expressions that are "locally inferable"
        // (suppress cascade from upstream MISSING dependencies)
        // Diagnostic emission deferred to M6 validation phase where
        // context-aware type error reporting is implemented properly.
        //
        // v3.1 CLOSE-OUT (the parser's standing conformance red, list (a)): the ONE
        // linking diagnostic the feature-call seat never emitted, at the one receiver
        // kind where silence is provably wrong — see the method.
        reportUnresolvedFeaturesOnEnumReceivers(allExprs, collector);
    }

    /**
     * v3.1 CLOSE-OUT — the fork has no emitter for {@code FEATURE_NOT_FOUND}: a
     * feature call whose lookup fails is left silently unbound (the category exists,
     * the test-side {@code ResolutionAudit} reads it, production never wrote it).
     * Upstream reports an unresolved-feature ERROR for every such call (its bytes:
     * {@code Couldn't resolve reference to RosettaFeature 'V1'.}). This method ports
     * that diagnostic's PRESENCE — at the fork's linking-message house style, like
     * {@code Symbol 'x' not found} — for the ONE receiver kind where the fork's own
     * lookup is exhaustive: an ENUMERATION-typed receiver whose attribute the fork
     * can NAME. An enumeration has no attributes, no choice options and no deep
     * features; the only things that resolve off it are its META features
     * ({@code -> scheme} on a {@code [metadata scheme]} attribute), which the R10 meta
     * arm above binds into {@code resolvedFeatureNode} inside the fixed point — but
     * that arm can only look where {@link #attributeBehind} names the receiver's
     * attribute (a feature call, a disguised nav, a bare attribute reference). A
     * receiver it cannot name — a function call, an alias, a list operation, a
     * conditional — is one the meta channel never consulted, so silence there is
     * the #437 decline, not a verdict (the recorded R10 follow-on: meta names
     * flowing through alias and chain types). Within that scope, a call still
     * unbound AFTER convergence over an {@link REnumTypeRef} receiver
     * (alias-unwrapped) has nothing left to resolve to: the enum's values are not
     * features either — spec R12's negative half, the enum-values scope belongs to a
     * literal reference to the enumeration ({@code Beta9 -> V1}), never to an
     * expression that merely TYPES as the enum ({@code h -> b -> V1}), which is
     * exactly p9-enum-typed-expr-not-r12:26, the witness. Any other receiver kind
     * stays silent here: the fork's lookup over data types still declines shapes
     * upstream resolves, so a general emitter would manufacture errors upstream
     * cannot emit. Runs once, after convergence, at the {@code "feature"} token range
     * the {@code @CrossRefField} names; the twenty-cell diagnostic gate measures the
     * corpus at ZERO new rows (no valid model writes a value off an enum-typed
     * expression — upstream refuses it).
     */
    private void reportUnresolvedFeaturesOnEnumReceivers(List<RExpression> allExprs,
            Diagnostics collector) {
        for (RExpression expr : allExprs) {
            if (!(expr instanceof RFeatureCall fc)
                    || fc.resolvedFeatureNode().isPresent()
                    || fc.resolvedFeature().isPresent()
                    || fc.featureName() == null) {
                continue;
            }
            if (attributeBehind(fc.left().orElse(null)) == null) {
                // The R10 meta channel could not even be consulted for this receiver
                // shape (the #606 review's MF-1: a function-call or alias receiver
                // over an enum-typed [metadata scheme] attribute resolves upstream)
                // — decline, never diagnose.
                continue;
            }
            RMetaAnnotatedType receiverType = fc.left()
                    .map(this::getInferredType)
                    .orElse(RMetaAnnotatedType.MISSING);
            if (receiverType.isMissing()) {
                continue;
            }
            RType t = receiverType.type();
            int depth = 0;
            while (t instanceof RAliasType alias && depth++ < 100) {
                t = alias.refersTo();
            }
            if (!(t instanceof REnumTypeRef enumRef)) {
                continue;
            }
            com.regnosys.rosetta.ast.SourceRange range =
                    fc.tokenRanges().getOrDefault("feature", fc.sourceRange());
            // Idempotent across re-runs of the engine on the same tree.
            collector.removeMatching(DiagnosticCategory.FEATURE_NOT_FOUND, range);
            collector.error(DiagnosticCategory.FEATURE_NOT_FOUND, range, fc.featureName(),
                    "Feature '" + fc.featureName() + "' not found on enumeration '" + enumRef.name()
                            + "' (an enumeration has no features; its values are written `"
                            + enumRef.name() + " -> " + fc.featureName()
                            + "` on the enumeration itself)",
                    List.of());
        }
    }

    /**
     * Runs type-directed resolution on expressions and their nested nodes,
     * using the current inferred types as context. Covers all 7 deferred
     * categories from M3 D1.
     *
     * @param collector the M3 mutable diagnostics collector. Threaded as a
     *     param (not field-stored) so the workspace doesn't retain the
     *     mutable backing list alongside the already-copied immutable
     *     snapshot returned by {@code collector.toList()}. Cat 9 uses it to
     *     clear stale {@code SYMBOL_NOT_FOUND} diagnostics when late
     *     resolution succeeds — see {@code Diagnostics.removeMatching}.
     * @return {@code true} iff Cat 8 directly mutated the {@code inferredTypes}
     *     map this pass. Cat 1/2/9/10 mutations propagate through Phase B
     *     change detection naturally (they set AST-node fields that the
     *     {@code typeComputer.compute} path reads), but Cat 8's
     *     {@code inferredTypes.put(iv, ...)} writes a value that Phase B
     *     reads back as-is — invisible to its diff check. The caller ORs
     *     this flag with Phase B's {@code changed} to decide whether to
     *     iterate again, so parents needing a fresh Cat 1 pass against the
     *     just-typed receiver don't miss it.
     */
    private boolean runTypeDirectedResolution(List<RExpression> allExprs, Diagnostics collector) {
        boolean cat8Changed = false;
        for (RExpression expr : allExprs) {
            // Category 1: Feature calls
            //
            // GATED ON THE AUTHORITY SLOT, NOT THE LEGACY ONE. `resolvedFeature`
            // is typed to RAttribute and structurally cannot hold a choice
            // option (see TypeDirectedResolver#lookupFeature), so gating on it
            // left the arm OPEN forever for every call that bound an option or
            // a meta type: each fixed-point iteration re-ran the lookup and
            // re-fired notifyResolved, which registered the same reference
            // again — measured at up to 3x on the conformance suite, against
            // zero for attribute seats, whose gate did close. Gating on
            // `resolvedFeatureNode` gives option and meta seats exactly the
            // once-bound-then-done behaviour attribute seats already had, and
            // matches the R10 meta arm below, which was already written this
            // way (Copilot R8, PR #566).
            if (expr instanceof RFeatureCall fc && fc.resolvedFeatureNode().isEmpty()) {
                var receiverType = fc.left()
                    .map(this::getInferredType)
                    .orElse(RMetaAnnotatedType.MISSING);
                resolver.resolveFeatureCall(fc, receiverType);
            }
            // v3.1 C1 (spec R10) — A META FEATURE IS NOT A SPECIAL CASE
            // UPSTREAM, AND MUST NOT BE ONE HERE EITHER.
            //
            // `allFeaturesExcludingEnumValues` returns the type's attributes
            // CONCATENATED WITH its meta descriptions, so `-> key` is found by
            // the same lookup that finds `-> identifier`. Two consequences the
            // ordering below preserves: attributes come FIRST (a type with an
            // attribute literally named `key` shadows the meta description —
            // pinned by probe P7), and a meta name resolves to the exported
            // `metaType` ROOT ELEMENT, never to the `metadata` annotation's own
            // attributes.
            //
            // The fork's RMetaAnnotatedType does not carry the receiver's meta
            // attributes (inferAttributeType never populates them), so the
            // admission is seated where the fork DOES have the fact: the
            // receiving ATTRIBUTE's own `[metadata <name>]` annotation. That is
            // the corpus-witnessed shape (`commodityPayouts -> key`,
            // `holder -> tagged -> key`). Flowing meta names through alias and
            // chain types the way upstream's RMetaAnnotatedType does is the
            // recorded faithful follow-on.
            if (expr instanceof RFeatureCall metaFc
                    && metaFc.resolvedFeatureNode().isEmpty()
                    && metaTypeLookup != null) {
                RAttribute receiverAttribute = attributeBehind(metaFc.left().orElse(null));
                if (receiverAttribute != null
                        && resolver.isMetaFaceOnAttribute(receiverAttribute, metaFc.featureName())) {
                    metaTypeLookup.find(metaFc, metaFc.featureName()).ifPresent(metaType -> {
                        metaFc.setResolvedFeatureNode(metaType);
                        if (referenceIndex != null) {
                            referenceIndex.registerReference(metaFc, metaType);
                        }
                    });
                }
            }
            // Category 2: Deep feature calls
            if (expr instanceof RDeepFeatureCall dfc && dfc.resolvedFeature().isEmpty()) {
                var receiverType = dfc.left()
                    .map(this::getInferredType)
                    .orElse(RMetaAnnotatedType.MISSING);
                resolver.resolveDeepFeatureCall(dfc, receiverType);
            }
            // Category 4: Switch case guards
            if (expr instanceof RSwitchExpr sw) {
                // PR #451 — implicit-subject switch: `extract (switch
                // fpml.ReturnLeg then ...)` parses with a NULL argument; the
                // subject IS the enclosing implicit item (upstream's grammar
                // fills the implicit variable there), so guard resolution
                // reads the enclosing item type when no argument exists.
                var argType = sw.left()
                    .map(this::getInferredType)
                    .orElseGet(() -> getEnclosingItemType(sw));
                for (RSwitchCase sc : sw.cases()) {
                    sc.guard().ifPresent(g -> resolver.resolveSwitchGuard(g, argType));
                }
                // Category 15 (map_enum_function): switch-case RESULT bare-enum
                // resolution. A case result written as a bare enum value (e.g.
                // `"AggregateClient" then AggregateClient`) parses as an
                // RSymbolReference with no resolved symbol — the grammar cannot
                // know it names a value of the function OUTPUT enum without an
                // expected type, and no existing category resolves switch RESULTS
                // (Cat 4 above resolves only GUARDS; Cat 13 conditional branches;
                // Cat 14 comparison operands). When the switch is the DIRECT body
                // of an output assignment whose output is an enum E, bind each
                // bare case-result symbol matching a value of E to that REnumValue,
                // reusing the shared {@link #bindBareEnumValue} worker (idempotent
                // + name-match-gated, so it never overrides a real resolution).
                // The generator's resolved-REnumValue rendering (ReferenceHandler)
                // then emits `E.VALUE` in place of the broken bare-symbol form.
                // HARD-gated to the immediate-parent-ROperation / grandparent-
                // RFunction / output-name-match shape (see
                // enclosingFunctionOutputEnumForDirectSwitch): a switch in a
                // shortcut / condition / lambda (all also RFunction children) must
                // NOT be bound against the function output enum.
                com.regnosys.rosetta.ast.types.REnumeration switchOutputEnum =
                    enclosingFunctionOutputEnumForDirectSwitch(sw);
                if (switchOutputEnum != null) {
                    for (RSwitchCase sc : sw.cases()) {
                        // allowTypeShadowOverride=true: a case result whose bare name
                        // collides with an in-scope TYPE (e.g. `then Commodity`) was
                        // bound to that type by GlobalResolutionPass; rebind it to the
                        // output enum value (a type is never a valid case result).
                        bindBareEnumValue(sc.expression(), switchOutputEnum, collector, true);
                    }
                }
            }
            // Category 8: Implicit variables (literal 'item' keyword AND the
            // synthetic elided-operand form emitted by buildListOpExpr /
            // buildConversionExpr / visitExtractWithoutLeftExpr /
            // visitFilterWithoutLeftExpr / visitToStringWithoutLeftExpr). For
            // the literal form the type is the enclosing inline-body item type;
            // for the elided form it is the enclosing rule's from-type — both
            // branches live in {@link #computeImplicitItemType}.
            if (expr instanceof RImplicitVariable iv) {
                RMetaAnnotatedType itemType = computeImplicitItemType(iv);
                if (!itemType.isMissing()) {
                    RMetaAnnotatedType prev = inferredTypes.get(iv);
                    if (prev == null || !itemType.equals(prev)) {
                        inferredTypes.put(iv, itemType);
                        cat8Changed = true;
                    }
                }
            }
            // Category 9: Bare RSymbolReference inside ImplicitInlineFunction bodies.
            // Primary path for shapes like `extract id` (no literal 'item' keyword).
            // Idempotency: only fires when ref.symbol().isEmpty().
            //
            // Guard: ref.args().isEmpty() — RSymbolReference covers both bare
            // identifier shape (args empty, e.g. `extract id`) AND function-call
            // shape (args populated, e.g. `extract foo(x)`). The implicit-item
            // attribute lookup only applies to the bare shape; binding a
            // function-call to an attribute and clearing its SYMBOL_NOT_FOUND
            // would mis-classify a legitimately-unresolved function call.
            //
            // On successful resolution, clear the stale M3 SYMBOL_NOT_FOUND
            // diagnostic emitted at pass 5 (LexicalResolutionPass) — the M3
            // scope chain didn't include the implicit-item attribute lookup,
            // so the diagnostic is now misleading. The
            // LinkerInvariants.noNodeIsBothResolvedAndDiagnosed gate
            // requires either resolution OR diagnostic, never both.
            //
            // CLOSURE T10 — builtin-type shadow. The guard ALSO fires when the
            // bare reference resolved to a builtin TYPE (RRecordType /
            // RBasicType) and the implicit-item type has an attribute of that
            // name. In an extract / filter / then body a bare name is always a
            // feature navigation, so an implicit-item attribute shadows a
            // same-named builtin type. The global resolver binds e.g. the bare
            // `dateTime` in `... then only-element then extract dateTime` to the
            // builtin `dateTime` record type before type inference runs (corpus
            // EventTimestamp.dateTime; verified: itemType=EventTimestamp,
            // findAttribute=dateTime — the only blocker was the non-empty
            // RRecordType symbol). Re-binding to the attribute here mirrors the
            // normal Cat 9 path. Restricted to builtin types so it never
            // overrides a user type/function/variable resolution.
            // v3.1 C1 (spec R7.1) — THE HEAD OF `a -> b` WHERE `a` NAMES A
            // FEATURE OF THE IMPLICIT ITEM. LexicalResolutionPass gives the head
            // the parent chain (R4/R5), but the implicit item's features are the
            // NEXT bucket down and are only knowable once types are, so they can
            // only be reached here. `Asset -> Instrument` inside an extract over
            // an Observable is the canonical shape: `Asset` is an option of the
            // item, not anything the lexical chain can see.
            if (expr instanceof com.regnosys.rosetta.ast.expressions.references.REnumValueRef headRef
                    && headRef.resolvedHead().isEmpty()
                    && headRef.enumName() != null) {
                RMetaAnnotatedType headItemType = getEnclosingItemType(headRef);
                if (!headItemType.isMissing()) {
                    resolver.lookupFeature(headItemType.type(), headRef.enumName())
                            .ifPresent(feature -> {
                                headRef.setResolvedHead(feature);
                                if (referenceIndex != null) {
                                    referenceIndex.registerReference(headRef, feature);
                                }
                            });
                }
            }
            // v3.1 C1 (spec R9) — THE RIGHT-HAND NAME, LOOKED UP IN THE FEATURE
            // SCOPE OF THE HEAD'S TYPE. With the head now bound (R4/R5/R7.1),
            // the RHS needs no channel of its own: it is one feature lookup on
            // one type, which is all upstream ever does. The ten Cat-8 style
            // arms that guess a channel per LHS species stay in place filling
            // the legacy slots; this fills the authority, and where the two
            // disagree the authority is the one that matches upstream.
            //
            // IDEMPOTENT, like every sibling arm (v3.1 C1 MF-7): the head arm
            // above gates on resolvedHead().isEmpty() and Cat 1 on
            // resolvedFeature().isEmpty(); this arm had no such gate, so every
            // already-bound node re-ran the lookup, re-stamped, re-registered
            // and re-fired both clears on EVERY fixed-point iteration (up to 40,
            // ~15 observed on DRR-class corpora). The answer cannot change
            // between iterations — typeOfHead reads the LINKER's typeCalls and
            // the node's own structure, never inferredTypes — so first-wins here
            // is the same binding, arrived at once.
            if (expr instanceof com.regnosys.rosetta.ast.expressions.references.REnumValueRef rhsRef
                    && rhsRef.valueName() != null
                    && rhsRef.resolvedFeatureNode().isEmpty()
                    && rhsRef.resolvedHead().isPresent()) {
                RType headType = resolver.typeOfHead(rhsRef.resolvedHead().get());
                if (headType != null) {
                    java.util.Optional<RNode> feature =
                        resolver.lookupFeature(headType, rhsRef.valueName());
                    if (feature.isPresent()) {
                        rhsRef.setResolvedFeatureNode(feature.get());
                        if (referenceIndex != null) {
                            referenceIndex.registerReference(rhsRef, feature.get());
                        }
                        // BOTH halves of `a -> b` are now bound, so the
                        // phase-A "Enum 'a' not found" is a false
                        // positive: `a` was never an enum name, and the
                        // construct resolves completely. Cleared only on
                        // the both-bound path — a head that binds while
                        // its feature does not is still a genuine miss
                        // and keeps its diagnostic.
                        collector.removeMatching(
                            DiagnosticCategory.ENUM_NOT_FOUND, rhsRef.sourceRange());
                        collector.removeMatching(
                            DiagnosticCategory.ENUM_VALUE_NOT_FOUND, rhsRef.sourceRange());
                    } else if (metaTypeLookup != null
                            && rhsRef.resolvedHead().get() instanceof RAttribute headAttribute
                            && resolver.isMetaFaceOnAttribute(headAttribute, rhsRef.valueName())) {
                        // v3.1 C1 part 2 (R-2, spec R10) — the meta leg the R9
                        // arm was missing. 17/n wired R10 into the RFeatureCall
                        // arm and closed the THREE-hop `holder -> tagged ->
                        // key`; the corpus's TWO-hop `commodityPayouts -> key`
                        // parses as a bare REnumValueRef and resolves through
                        // THIS arm — the commit's own lesson ("a rule written
                        // against upstream's two receiver shapes silently does
                        // nothing on the fork's third") repeated one arm over.
                        // Ordered AFTER lookupFeature declines, so a real
                        // attribute named `key` shadows the meta description
                        // (the P7 order, same as the RFeatureCall pair), and
                        // gated inside headType != null because upstream's
                        // NOTHING_WITH_ANY_META special case withholds meta
                        // descriptions from an unresolved receiver too.
                        metaTypeLookup.find(rhsRef, rhsRef.valueName()).ifPresent(metaType -> {
                            rhsRef.setResolvedFeatureNode(metaType);
                            if (referenceIndex != null) {
                                referenceIndex.registerReference(rhsRef, metaType);
                            }
                            // Both halves bound (head lexically, leaf to the
                            // exported metaType root), so the phase-A tried-enum
                            // diagnostics are false positives exactly as on the
                            // feature path above.
                            collector.removeMatching(
                                DiagnosticCategory.ENUM_NOT_FOUND, rhsRef.sourceRange());
                            collector.removeMatching(
                                DiagnosticCategory.ENUM_VALUE_NOT_FOUND, rhsRef.sourceRange());
                        });
                    }
                }
            }
            // v3.1 C1 (spec R6 + R7.1) — THE OPTION BINDING, INDEPENDENT OF THE
            // LEGACY SLOT. Upstream's symbol scope is filtered to RosettaSymbol,
            // and a `Data` is not one — so a top-level type NEVER competes with
            // an implicit-item feature of the same name, and a bare option name
            // binds the ChoiceOption even when a same-named type is in scope
            // (pinned: a1 case (d), where `choice Asset` is both). The fork's
            // global scope carries no such filter, so pass 4 binds the TYPE and
            // the arm below is gated symbol-EMPTY and never fires for these.
            //
            // Rather than un-bind the type — which would flip the #451 render
            // arms keyed on that bind state, on cells that are byte-EXACT today —
            // the correct answer is recorded in the ADDITIVE authoritative slot
            // and the legacy one is left as it is. Consumers migrate in the
            // follow-on that retires the ladder; until then the authority and the
            // legacy view disagree here BY RECORD rather than by accident.
            //
            // GATED ON THE SYMBOL SLOT TOO (v3.1 C1 part 2, MF-1): the arm
            // exists to fire OVER a type-like legacy binding — that is its
            // purpose — but never over a GENUINE upstream symbol. Upstream's
            // parent chain (R3/R4) outranks implicit-item features, so a
            // function input named like an option binds the INPUT upstream
            // (probe P2, the committed pin: upstream Attribute
            // P2InputShadowsOption.Credit, not ChoiceOption Idx.Credit).
            // Ungated, the arm stamped the option into the authority slot
            // whenever it was empty, which is exactly the wrong-binding the
            // differential then reported.
            if (expr instanceof RSymbolReference optionRef
                    && optionRef.args().isEmpty()
                    && optionRef.resolvedFeatureNode().isEmpty()
                    && (optionRef.symbol().isEmpty()
                            || !com.regnosys.rosetta.symbols.UpstreamSymbolKinds
                                    .isUpstreamSymbol(optionRef.symbol().get()))) {
                RMetaAnnotatedType optionItemType = getEnclosingItemType(optionRef);
                if (!optionItemType.isMissing()) {
                    com.regnosys.rosetta.ast.supporting.RChoiceOption declaredOption =
                        resolver.choiceOptionDeclarationOnType(
                            optionItemType.type(), optionRef.name());
                    if (declaredOption != null) {
                        optionRef.setResolvedFeatureNode(declaredOption);
                        if (referenceIndex != null) {
                            referenceIndex.registerReference(optionRef, declaredOption);
                        }
                    }
                }
            }
            // THE EXCEPTION IS EVERY NON-SYMBOL, NOT BUILTINS ALONE (v3.1 C1
            // part 2, MF-2). Upstream's R6 filter removes every non-RosettaSymbol
            // from the symbol scope — a global `type`/`choice` never competes,
            // and a metaType is filtered at the chain's root — so an
            // implicit-item ATTRIBUTE named like either binds the attribute
            // upstream (probes P3 and P7(a), the committed pins: Attribute
            // Position.Cash over `type Cash`; Attribute Tag7.key over
            // `metaType key`). The builtin-only exception (CLOSURE T10) was the
            // same rule discovered one kind at a time; the whitelist states it
            // whole. ⚠️ This widening changes what the LEGACY symbol slot ends
            // up holding on collision inputs (the re-bind overwrites the pass-5
            // type/metaType bind, exactly as the T10 builtin shadow always did)
            // — measured over the band: zero carriers moved.
            if (expr instanceof RSymbolReference ref
                    && ref.args().isEmpty()
                    && (ref.symbol().isEmpty()
                            || !com.regnosys.rosetta.symbols.UpstreamSymbolKinds
                                    .isUpstreamSymbol(ref.symbol().get()))) {
                java.util.Optional<RAttribute> resolvedAttr =
                    resolver.resolveImplicitItemFeatureCall(ref, this::getEnclosingItemType);
                if (resolvedAttr.isPresent()) {
                    ref.setResolvedSymbol(resolvedAttr.get());
                    // Mirror passes 4 + 5: every setResolvedSymbol must pair with
                    // a ReferenceIndex.registerReference, otherwise findReferences()
                    // misses the cross-ref (Copilot PR #76 R11 F1).
                    if (referenceIndex != null) {
                        referenceIndex.registerReference(ref, resolvedAttr.get());
                    }
                    collector.removeMatching(
                        DiagnosticCategory.SYMBOL_NOT_FOUND, ref.sourceRange());
                } else if (ref.symbol().isEmpty()) {
                    // PR #451 — bare CHOICE-OPTION name on the implicit item
                    // (`payouts extract InterestRatePayout exists` — the item
                    // is the Payout choice and the bare name is one of its
                    // options). Upstream's implicit-variable feature set on a
                    // choice receiver IS its options-as-attributes (the
                    // asRDataType view — the #446 law), and options inherited
                    // through a `type X extends ChoiceC` parent arrive by the
                    // same walk (getAllAttributes; the fork's widened choice
                    // VIEW), so the nav resolves upstream and emits NOTHING.
                    // The fork has no RAttribute to bind for an option-as-
                    // feature at a bare RSymbolReference (setResolvedSymbol
                    // carries the Cat-9 attribute contract), so the arm is
                    // CLEARING-ONLY: the stale SYMBOL_NOT_FOUND is removed,
                    // nothing binds. Same-name references that pass 5 already
                    // bound to a global TYPE are untouched (symbol-empty
                    // gate) — that resolved-type render class generates
                    // byte-identically today and stays as-is.
                    RMetaAnnotatedType bareItemType = getEnclosingItemType(ref);
                    if (!bareItemType.isMissing()
                            && resolver.isChoiceOptionOnType(
                                bareItemType.type(), ref.name())) {
                        // v3.1 C1: the arm is no longer clearing-only. There IS
                        // something to bind — the option DECLARATION, which is
                        // what upstream binds here (ChoiceOption <Choice>.<Option>).
                        // It goes to the additive authoritative slot, so symbol()
                        // and every render arm keyed on its bind state are
                        // untouched and the byte surface is unchanged by
                        // construction.
                        com.regnosys.rosetta.ast.supporting.RChoiceOption bareOption =
                            resolver.choiceOptionDeclarationOnType(
                                bareItemType.type(), ref.name());
                        if (bareOption != null) {
                            ref.setResolvedFeatureNode(bareOption);
                            if (referenceIndex != null) {
                                referenceIndex.registerReference(ref, bareOption);
                            }
                        }
                        collector.removeMatching(
                            DiagnosticCategory.SYMBOL_NOT_FOUND, ref.sourceRange());
                    }
                    // PR #458 — the two upstream scope channels the lexical
                    // chain cannot see (vendored RosettaScopeProvider's
                    // symbol-reference scope, :202-208: `findFeaturesOfImplicit-
                    // Variable` + the expected-type enum-value contribution),
                    // measured LIVE at the mojo seam as the drr corpus's ONLY
                    // two linking over-fires (the #448-recorded eq rows —
                    // hkma `scheme` + datetime-rule `confirmationDateTime`).
                    // Both arms are CLEARING-ONLY (the #451 choice-option
                    // precedent): upstream resolves these silently; the fork's
                    // render already recovers both shapes through its own
                    // decline rungs (implicitItemArgMeta + misBoundSibling-
                    // AttributeEnumeration), so binding here would flip
                    // bind-state-keyed render arms — the diagnostic clears,
                    // nothing binds, the byte surface is untouched by
                    // construction (proven by the live cp + swap re-proof).
                    else if (implicitItemMetaFeature(ref)) {
                        // Arm A: the bare name is a META feature of the
                        // implicit item — the item's producing attribute
                        // carries `[metadata <name>]` (upstream's meta-
                        // annotated implicit-variable features include the
                        // wrapper's meta fields; the drr carrier filters a
                        // `[metadata scheme]` string stream on `scheme`).
                        collector.removeMatching(
                            DiagnosticCategory.SYMBOL_NOT_FOUND, ref.sourceRange());
                    } else if (equalitySiblingEnumValue(ref)) {
                        // Arm B: the bare name is a VALUE of the enumeration
                        // the equality SIBLING types to (upstream's expected-
                        // type channel adds the expected enum's values to the
                        // scope; the drr carrier filters on `qualification =
                        // confirmationDateTime` where `qualification` is the
                        // item's enum-typed attribute).
                        collector.removeMatching(
                            DiagnosticCategory.SYMBOL_NOT_FOUND, ref.sourceRange());
                    }
                }
            }
            // Category 13 (Phase X1 T0n / Gap E; DIRECTIONAL since v3.1 C1
            // part 2): implicit (bare) enum-value resolution by expected type
            // inside a conditional. A branch written as a bare enum value
            // (e.g. `then PHYS`) parses as an RSymbolReference with no
            // resolved symbol — the grammar cannot know it names an enum value
            // without an expected type. Upstream's rule
            // (ExpectedTypeProvider.caseConditionalExpression) is DIRECTIONAL:
            // a CONTAINER expectation feeds both arms, and where there is
            // none, ONLY the else-arm falls back to typeof(then-arm) — a bare
            // THEN arm with no container expectation does not resolve upstream
            // at all (probe P4, the committed refusal pin; the pre-C1
            // then-first-else-descend sibling scan bound exactly that shape
            // and was the wrong-accept the pin caught). Binding uses the
            // idempotent name-matched bindBareEnumValue worker throughout
            // (symbol-empty references only, super-enum-chain aware), so a
            // real variable/attribute resolution always wins. Mirrors the DRR
            // shape `if .. then DeliveryTypeEnum -> CASH else if .. then PHYS`
            // — the qualified THEN feeding the bare ELSE, which is the one
            // direction upstream admits.
            if (expr instanceof com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr cond) {
                // Category 13b (enum_set_result_qualify): the assignment-target
                // OUTPUT enum — but ONLY when the conditional is the DIRECT
                // body of that output's `set` (the same hard gate as Cat 15's
                // enclosingFunctionOutputEnumForDirectSwitch), so a conditional
                // in a shortcut / condition / lambda is never bound against the
                // function output (DRR ClearingExceptionsAndExemptions
                // `set result: if .. then ENDU else if .. then AFFL ..`).
                com.regnosys.rosetta.ast.types.REnumeration containerEnum =
                    enclosingFunctionOutputEnumForDirectBody(cond);
                // Category 13d (facet ctorSetterEnum, PR #202): a conditional
                // consumed as a CONSTRUCTOR-SETTER pair value (`Price {
                // priceType: if .. then InterestRate }`) — the pair attribute's
                // enum type is the container expectation there. Once bound, the
                // generator's REnumValue branch (ReferenceHandler) renders
                // `Enum.VALUE` AND ControlFlowHandler's ctor_setter_ite_hoist
                // (thenItemJavaClass REnumValue arm) can type + hoist the
                // no-else conditional.
                if (containerEnum == null) {
                    containerEnum = enclosingCtorAttributeEnumForDirectValue(cond);
                }
                if (containerEnum != null) {
                    // container expectation: upstream flows it to BOTH arms
                    // (IFTHEN = container; ELSETHEN = container-first).
                    resolveBareEnumBranches(cond, containerEnum, collector);
                } else {
                    // no container: only the else-arm's typeof(then) fallback
                    // applies, per level down the nested chain.
                    seedElseArmsFromThen(cond, collector);
                }
            }
            // Category 13c (facet void_witness_bare_enum / wf169 mechanism (d)):
            // implicit (bare) enum-value resolution for LIST-LITERAL elements by
            // the operation target's enum type. Upstream flows the expected type
            // THROUGH a list literal to its elements (ExpectedTypeProvider
            // caseListLiteral → getExpectedTypeFromContainer) and scopes bare
            // names against the expected enum's values (RosettaScopeProvider), so
            // `add supervisoryBodyCSA: [CA_AB_ASC, ...]` binds each bare element
            // to a value of the output's SupervisoryBodyEnum. Same hard gate as
            // Cat 13b (the literal must be the DIRECT body of an operation
            // targeting the function output) and the same idempotent name-matched
            // bindBareEnumValue worker (symbol-empty references only — a real
            // variable/attribute resolution always wins; super-enum-chain aware).
            // Binding restores the EXISTING inference join (computeListLiteral
            // types the element via its parent enumeration, so the literal's
            // witness joins to the enum instead of NOTHING→Void) and the EXISTING
            // generator render (ReferenceHandler's Cat-13 branch emits the
            // qualified constant; LiteralHandler's element wrap adds MapperS.of).
            if (expr instanceof com.regnosys.rosetta.ast.expressions.literals.RListLiteral lit) {
                com.regnosys.rosetta.ast.types.REnumeration expectedEnum =
                    enclosingFunctionOutputEnumForDirectBody(lit);
                if (expectedEnum != null) {
                    for (RExpression elem : lit.elements()) {
                        bindBareEnumValue(elem, expectedEnum, collector);
                    }
                }
            }
            // Category 14 (Phase X1 T0n / Gap E): implicit (bare) enum-value
            // resolution by expected type across a comparison. The DRR shape
            // `settlementType = Cash` parses the RHS `Cash` as an unresolved
            // RSymbolReference (no expected type at parse time). When the sibling
            // operand already types as an enum E and `Cash` names a value of E,
            // bind it to that REnumValue. Unlike Category 13 this does NOT change
            // the comparison's own type (= / <> / ordering comparisons are
            // BOOLEAN unconditionally — ExpressionTypeComputer:75-77), so it does
            // not move the fail-fast count; its purpose is byte-identity — the
            // resolved operand renders as the Java enum constant `E.CASH` instead
            // of the broken `MapperS.of(Cash)` variable form a bare unresolved
            // symbol would otherwise emit. RIGHT-only, upstream's direction
            // (SF-2/P5 — see the arm below). bindBareEnumValue only fires on an unresolved
            // (symbol-empty, arg-empty) reference whose name is a value of the
            // sibling's enum, so it never overrides a genuine variable/attribute
            // resolution and is idempotent. Runs after Cat 9 so a real
            // variable/attribute binding always wins. RComparisonExpr is included
            // for symmetry; enumOfBranch returns null for its (numeric) operands,
            // making it a guarded no-op there.
            // DIRECTIONAL since v3.1 C1 part 2 (SF-2): upstream flows the
            // expected type into the RIGHT operand only (caseEqualsOperation /
            // caseNotEqualsOperation handle the RIGHT containment), so a bare
            // enum value on the LEFT has no expected type and does not resolve
            // — probe P5, the committed refusal pin, which the symmetric form
            // wrongly bound (measured: fork RosettaEnumValue
            // conformance.p5.Beta5.Other5 vs upstream UNRESOLVED). The RIGHT
            // half stays: bare RHS from typed LHS is upstream's rule and the
            // corpus's `settlementType = Cash` shape.
            if (expr instanceof com.regnosys.rosetta.ast.expressions.binary.REqualityExpr eq) {
                resolveBareEnumOperand(eq.right().orElse(null), eq.left().orElse(null), collector);
            }
            if (expr instanceof com.regnosys.rosetta.ast.expressions.binary.RComparisonExpr cmp) {
                resolveBareEnumOperand(cmp.right().orElse(null), cmp.left().orElse(null), collector);
            }
            // Category 16 (PR #448): implicit (bare) enum-value admission by the
            // POSITION's expected type — the recursion upstream implements in
            // ExpectedTypeProvider.getExpectedTypeFromContainer and consumes in
            // RosettaScopeProvider (a bare RosettaSymbolReference's scope gains
            // the expected enum's values as implicit features when the expected
            // type is an enum). Precedence honors upstream's ReversedSimpleScope
            // exactly: the parent (lexical) scope wins — this arm fires only on
            // a symbol-EMPTY reference (pass 5 found no lexical symbol; never
            // overrides) and declines names any enclosing closure declares as an
            // explicit param — and the implicit item's features outrank the
            // expected enum's values within the local block, so the arm DEFERS
            // while an enclosing implicit item is still untyped (Cat 9 has not
            // conclusively declined). The position walker
            // (expectedEnumViaPosition) prunes upstream's table to the
            // enum-producing paths; positions whose upstream expected type is a
            // builtin (boolean guards, filter bodies, to-* arguments,
            // arithmetic operands) return null by omission.
            //
            // BIND (PR #452 — the recorded typed follow-on of the #448
            // clearing-only arm, landed WITH its generator seats aligned per
            // the #447 lands-with-its-seats precedent): the bare value
            // resolves to the position enum's REnumValue (own or `extends`
            // super-enum chain — bindBareEnumValue's match walk, byte-identical
            // to the retired clearBareEnumValue's admission), registers the
            // cross-ref and clears the stale SYMBOL_NOT_FOUND. The #448
            // bind-mode probe had measured the bind moving 6 cdm-6.20.6
            // FUNCTION goldens through TWO generator seats (dump banked at
            // target/448-bindmode-dump/); the #452 wave seats both: the
            // getOrDefault-arg hoist gate + the ctor-value / default-RHS
            // qualification rungs admit the BIND-STAMPED value under the #215
            // same-instance descend-only law (facet cat16BindEnumSeats), so a
            // super-enum-declared value keeps qualifying by the expected CHILD
            // enum (the #211/#358 flatten law) and the generation surface
            // stays byte-equal to the #437 SOT (probe-measured).
            if (expr instanceof RSymbolReference bare
                    && bare.args().isEmpty()
                    && bare.symbol().isEmpty()
                    && !closureDeclaresParamName(bare)
                    && !untypedImplicitItemContext(bare)) {
                com.regnosys.rosetta.ast.types.REnumeration positionEnum =
                    expectedEnumViaPosition(bare, 0);
                if (positionEnum != null) {
                    bindBareEnumValue(bare, positionEnum, collector);
                }
            }
            // Category 10: REnumValueRef fallback to attribute-feature chain.
            // The grammar parses `a -> b` as REnumValueRef('a','b') because the
            // {EnumName -> ValueName} production has precedence. When (a) enum
            // resolution failed AND (b) the node sits inside an implicit inline
            // body AND (c) the implicit-item type has an attribute named 'a',
            // reinterpret as a 2-segment attribute chain: `a` on the
            // implicit-item type, then `b` as a feature on `a`'s element type.
            // Idempotency: only fires when both enum + chain are unresolved.
            //
            // On successful AttributeChain bind, clear the stale M3 enum
            // diagnostics that GlobalResolutionPass.resolveEnumValueRef
            // emitted at the same SourceRange (ENUM_NOT_FOUND + the related
            // ENUM_VALUE_NOT_FOUND). Symmetric to Cat 9's SYMBOL_NOT_FOUND
            // cleanup. Without this, ws.diagnostics() still reports the enum
            // miss even though the node has been successfully reinterpreted
            // as an attribute chain — misleading consumers + violating the
            // LinkerInvariants.noNodeIsBothResolvedAndDiagnosed gate.
            if (expr instanceof REnumValueRef enr
                    && enr.enumeration().isEmpty()
                    && enr.enumValue().isEmpty()
                    && enr.resolvedAttributeChain().isEmpty()) {
                RMetaAnnotatedType itemType = getEnclosingItemType(enr);
                // Phase X1 closure T10 lever 11 (extends D42 LOCK 2026-05-23) —
                // closure-param-as-leading-name; the DECLARING closure may be an
                // OUTER one. The DRR corpus uses `extract X [ ... (extract X -> field) ]`
                // where the leading X is the EXPLICIT closure parameter of an OUTER
                // closure but is referenced inside a NESTED inner closure; the grammar
                // parses `X -> field` as REnumValueRef(enumName=X, valueName=field).
                // Walk the FULL ancestor chain (not just the nearest inline function)
                // to find the closure that DECLARES X as a param, then resolve `field`
                // as a feature on THAT closure's item element type — the param is bound
                // to its declaring closure's items, whose type may be resolved even when
                // the nearest (inner) item type is MISSING. Hoisted ABOVE the
                // `!itemType.isMissing()` guard precisely because the nearest item type
                // is MISSING for the nested-reference case. The original in-guard D42
                // simple case (`extract X [X -> field]`, declaring closure == nearest
                // inline function) resolves here too, since computeArgumentElementType
                // of the declaring closure's parent op equals the nearest item type.
                if (enr.resolvedAttributeChain().isEmpty()
                        && enr.resolvedChoiceOption().isEmpty()
                        && enr.resolvedTypeRestriction().isEmpty()) {
                    RInlineFunction declaringClosure = null;
                    for (RNode anc = enr.parent(); anc != null; anc = anc.parent()) {
                        if (anc instanceof RInlineFunction inlineAnc
                                && inlineAnc.paramNames().contains(enr.enumName())) {
                            declaringClosure = inlineAnc;
                            break;
                        }
                    }
                    if (declaringClosure != null) {
                        RMetaAnnotatedType paramType =
                            computeArgumentElementType(declaringClosure.parent());
                        if (!paramType.isMissing()) {
                            java.util.Optional<RAttribute> feature =
                                resolver.findAttributeOnType(paramType.type(), enr.valueName());
                            if (feature.isPresent()) {
                                enr.setResolvedAttributeChain(
                                    new REnumValueRef.AttributeChain(feature.get()));
                                if (referenceIndex != null) {
                                    referenceIndex.registerReference(enr, feature.get());
                                }
                                collector.removeMatching(
                                    DiagnosticCategory.ENUM_NOT_FOUND, enr.sourceRange());
                                collector.removeMatching(
                                    DiagnosticCategory.ENUM_VALUE_NOT_FOUND, enr.sourceRange());
                                continue;
                            }
                        }
                    }
                }
                if (!itemType.isMissing()) {
                    java.util.Optional<RAttribute> first =
                        resolver.findAttributeOnType(itemType.type(), enr.enumName());
                    if (first.isPresent()) {
                        RMetaAnnotatedType firstType = resolver.inferAttributeRefType(first.get());
                        if (!firstType.isMissing()) {
                            java.util.Optional<RAttribute> second =
                                resolver.findAttributeOnType(firstType.type(), enr.valueName());
                            if (second.isPresent()) {
                                enr.setResolvedAttributeChain(
                                    new REnumValueRef.AttributeChain(first.get(), second.get()));
                                // Mirror passes 4 + 5: every resolution-stamp must pair
                                // with ReferenceIndex.registerReference, otherwise
                                // findReferences() misses the cross-ref. Register both
                                // attributes in the chain — the enum-value-ref node
                                // references both (the first attribute on the receiver
                                // type, the second attribute on the first's element
                                // type). Copilot PR #76 R11 F1.
                                if (referenceIndex != null) {
                                    referenceIndex.registerReference(enr, first.get());
                                    referenceIndex.registerReference(enr, second.get());
                                }
                                collector.removeMatching(
                                    DiagnosticCategory.ENUM_NOT_FOUND, enr.sourceRange());
                                collector.removeMatching(
                                    DiagnosticCategory.ENUM_VALUE_NOT_FOUND, enr.sourceRange());
                            } else if (resolver.isRecordFeatureOnType(
                                    firstType.type(), enr.valueName())) {
                                // PR #451 record-member leaf at the ITEM-headed
                                // 2-step — the same #444 contract the input /
                                // output / condition channels already carry:
                                // the first attribute's type is a builtin
                                // RECORD and the leaf is one of its features
                                // (`stepDate -> date` on a zonedDateTime item
                                // feature — the fpml ingest extract lambdas'
                                // dominant shape). Record features are not
                                // RAttributes, so nothing binds; the
                                // resolution is proven (upstream types record
                                // features via RRecordFeature and emits
                                // nothing — the V0 oracle streams are
                                // error-free over every carrier) and the
                                // stale tried-enum diagnostics are removed.
                                // No cat8Changed — collector-only.
                                collector.removeMatching(
                                    DiagnosticCategory.ENUM_NOT_FOUND, enr.sourceRange());
                                collector.removeMatching(
                                    DiagnosticCategory.ENUM_VALUE_NOT_FOUND, enr.sourceRange());
                            } else if (resolver.isMetaFaceOnAttribute(
                                    first.get(), enr.valueName())) {
                                // PR #451 metadata-face leaf: the first
                                // attribute declares [metadata <leaf>]
                                // (`partyReference -> reference` on a
                                // [metadata reference] attribute inside the
                                // Counterparty filter lambdas; `identifier ->
                                // scheme` on [metadata scheme] — the drr
                                // productIdentifier filters). Upstream admits
                                // the face as an ordinary feature (the
                                // receiver's meta-annotated type names it and
                                // allFeatures appends getMetaDescriptions) and
                                // emits nothing. No fork AST node backs a meta
                                // face, so the arm is CLEARING-ONLY, exactly
                                // like the record rung above.
                                collector.removeMatching(
                                    DiagnosticCategory.ENUM_NOT_FOUND, enr.sourceRange());
                                collector.removeMatching(
                                    DiagnosticCategory.ENUM_VALUE_NOT_FOUND, enr.sourceRange());
                            }
                        }
                    }
                    // Phase X1 closure T10 — choice-option navigation.
                    // `<choiceAttr> -> <OptionName>` where <OptionName> is the
                    // NAME of one of the choice's declared options (e.g.
                    // `payout -> CommodityPayout` on a `payout : Payout`
                    // attribute, Payout being a choice). The standard 2-step
                    // above bails because <OptionName> is a TYPE, not an
                    // attribute on the choice. Unlike Gap B (below), this does
                    // NOT depend on GlobalResolutionPass importing the option
                    // type into the rule file's scope — the choice's own option
                    // list IS in scope. Membership in that list IS the
                    // downcast's validity (no SubtypeRelation check needed); the
                    // option may itself be a data type OR a nested choice.
                    if (enr.resolvedAttributeChain().isEmpty()
                            && enr.resolvedChoiceOption().isEmpty()
                            && enr.resolvedTypeRestriction().isEmpty()) {
                        java.util.Optional<RAttribute> choiceAttr =
                            resolver.findAttributeOnType(itemType.type(), enr.enumName());
                        if (choiceAttr.isPresent()) {
                            RMetaAnnotatedType choiceAttrType =
                                resolver.inferAttributeRefType(choiceAttr.get());
                            if (!choiceAttrType.isMissing()
                                    && choiceAttrType.type()
                                        instanceof com.regnosys.rosetta.types.RChoiceTypeRef choiceType) {
                                com.regnosys.rosetta.ast.RRootElement optionNode =
                                    matchChoiceOption(choiceType, enr.valueName());
                                if (optionNode != null) {
                                    enr.setResolvedChoiceOption(optionNode);
                                    enr.setChoiceOptionHeadIsDirectChoice(true);
                                    if (referenceIndex != null) {
                                        referenceIndex.registerReference(enr, optionNode);
                                    }
                                    collector.removeMatching(
                                        DiagnosticCategory.ENUM_NOT_FOUND, enr.sourceRange());
                                    collector.removeMatching(
                                        DiagnosticCategory.ENUM_VALUE_NOT_FOUND, enr.sourceRange());
                                }
                            }
                        }
                    }
                    // Phase X1 closure T10 lever 8 — choice-TYPE-NAME-LHS option
                    // navigation. `<ChoiceType> -> <OptionName>` where the
                    // implicit ITEM's type IS the choice and the LHS names that
                    // choice type (capitalised), not a lowercase attribute. The
                    // DRR cdeV3 underlier rules navigate `Observable -> Asset` on
                    // an item already typed as the Observable choice (choice
                    // Observable: Asset|Basket|Index). The standard 2-step and the
                    // attribute-LHS choice-option block above both bail because the
                    // LHS is the choice type name, not an attribute on the item.
                    // Narrow to the named option from the item choice's own option
                    // list (membership IS the downcast's validity). Exact name
                    // match against the item choice keeps this from firing when the
                    // LHS names a different symbol.
                    if (enr.resolvedAttributeChain().isEmpty()
                            && enr.resolvedChoiceOption().isEmpty()
                            && enr.resolvedTypeRestriction().isEmpty()
                            && itemType.type()
                                instanceof com.regnosys.rosetta.types.RChoiceTypeRef itemChoice
                            && itemChoice.name().equals(enr.enumName())) {
                        com.regnosys.rosetta.ast.RRootElement optionNode =
                            matchChoiceOption(itemChoice, enr.valueName());
                        if (optionNode != null) {
                            enr.setResolvedChoiceOption(optionNode);
                            // headIsDirectChoice deliberately NOT set: the LHS
                            // names the choice TYPE, not a choice-typed value —
                            // upstream's parse carries no choice-typed receiver
                            // here (the drr cdeV3 carriers are bank-silent on
                            // the path-operator deprecation).
                            if (referenceIndex != null) {
                                referenceIndex.registerReference(enr, optionNode);
                            }
                            collector.removeMatching(
                                DiagnosticCategory.ENUM_NOT_FOUND, enr.sourceRange());
                            collector.removeMatching(
                                DiagnosticCategory.ENUM_VALUE_NOT_FOUND, enr.sourceRange());
                        }
                    }
                    // Phase X1 closure T10 lever 10 — option-of-option choice
                    // navigation. `<OuterOption> -> <InnerOption>` where the
                    // implicit item is a choice, enumName names one of its options
                    // that is ITSELF a choice, and valueName names an option of
                    // that inner choice. The DRR cdeV3 underlier rules navigate
                    // `Observable -> Asset` on an item typed
                    // `choice Underlier: Observable | Product` (Observable is an
                    // option of Underlier AND is itself
                    // `choice Observable: Asset | Basket | Index`). Narrow
                    // Underlier -> Observable -> Asset; the resolved type is the
                    // inner option (Asset). The L8 choice-TYPE-NAME block above
                    // bails (item choice name "Underlier" != enumName "Observable")
                    // and the attribute-LHS choice-option block bails (Observable
                    // is an option TYPE, not an attribute on Underlier).
                    //
                    // PR #451 — the ENTRY widens from `itemType instanceof
                    // RChoiceTypeRef` to the item's CHOICE VIEW
                    // (choiceViewOfType): a `type X extends ChoiceC` item
                    // admits C's options as inherited features upstream
                    // (getAllAttributes walks the choice parent — the
                    // BasketConstituent extract lambdas' `Asset -> Commodity`
                    // witness), and the view is the IDENTITY for a direct
                    // choice item, so every pre-#451 admission is preserved
                    // exactly. A leaf that is an ATTRIBUTE or RECORD feature
                    // of the outer option's type (rather than an option of a
                    // nested choice — `Index -> ForeignExchangeRateIndex`
                    // when Index is a data type) is admitted upstream by the
                    // same uniform feature walk; the fork's AttributeChain
                    // cannot carry an option-headed first segment, so that
                    // shape is CLEARING-ONLY (the bind is the recorded typed
                    // follow-on).
                    if (enr.resolvedAttributeChain().isEmpty()
                            && enr.resolvedChoiceOption().isEmpty()
                            && enr.resolvedTypeRestriction().isEmpty()
                            && resolver.choiceViewOfType(itemType.type())
                                instanceof com.regnosys.rosetta.types.RChoiceTypeRef outerItemChoice) {
                        com.regnosys.rosetta.types.RType outerOption =
                            matchChoiceOptionType(outerItemChoice, enr.enumName());
                        if (outerOption
                                instanceof com.regnosys.rosetta.types.RChoiceTypeRef innerChoice) {
                            com.regnosys.rosetta.ast.RRootElement innerOption =
                                matchChoiceOption(innerChoice, enr.valueName());
                            if (innerOption != null) {
                                enr.setResolvedChoiceOption(innerOption);
                                // The fire-relevant receiver is the OUTER
                                // option (enumName), whose type is the inner
                                // choice by this arm's own gate.
                                enr.setChoiceOptionHeadIsDirectChoice(true);
                                if (referenceIndex != null) {
                                    // The enr references both the outer option
                                    // (the inner choice on the item choice) and
                                    // the inner option (the resolved type).
                                    referenceIndex.registerReference(enr, innerChoice.astNode());
                                    referenceIndex.registerReference(enr, innerOption);
                                }
                                collector.removeMatching(
                                    DiagnosticCategory.ENUM_NOT_FOUND, enr.sourceRange());
                                collector.removeMatching(
                                    DiagnosticCategory.ENUM_VALUE_NOT_FOUND, enr.sourceRange());
                            }
                        } else if (outerOption != null
                                && (resolver.findAttributeOnType(
                                        outerOption, enr.valueName()).isPresent()
                                    || resolver.isRecordFeatureOnType(
                                        outerOption, enr.valueName()))) {
                            // Option-headed nav whose leaf is an attribute /
                            // record feature of the option's own type —
                            // resolution proven (the uniform upstream feature
                            // walk), nothing bindable: CLEARING-ONLY.
                            collector.removeMatching(
                                DiagnosticCategory.ENUM_NOT_FOUND, enr.sourceRange());
                            collector.removeMatching(
                                DiagnosticCategory.ENUM_VALUE_NOT_FOUND, enr.sourceRange());
                        }
                    }
                }
                // Phase X1 closure T0h (Gap A) — func/rule call result + feature
                // access. GlobalResolutionPass.resolveEnumValueRef binds
                // resolvedSymbol when the LHS resolves to an RFunction (or
                // RRule); here we complete the chain by looking up valueName
                // as a feature on the symbol's output type. Ordering vs the
                // standard 2-step: this branch's `resolvedAttributeChain().isEmpty()`
                // guard means the standard 2-step wins on the rare collision
                // case where a source name resolves to BOTH an attribute on
                // itemType AND a workspace func/rule (the attribute-on-itemType
                // path runs first and binds the chain). For the dominant Gap A
                // corpus shape — where the LHS name is NOT also an attribute
                // on itemType (e.g. `EconomicTermsForProduct` is a func, not
                // a field of any chain receiver) — the standard 2-step bails
                // at step 1 and this branch completes the chain.
                if (enr.resolvedAttributeChain().isEmpty()
                        && enr.resolvedSymbol().isPresent()) {
                    com.regnosys.rosetta.ast.RRootElement symbol = enr.resolvedSymbol().get();
                    java.util.Optional<RAttribute> symbolOutput =
                        symbolOutputAttribute(symbol);
                    // Output type of the resolved symbol: an RFunction's output
                    // ATTRIBUTE, or — for an RRule (no output attribute;
                    // symbolOutputAttribute is RFunction-only) — the inferred type
                    // of the rule's expression body (engine fixed-point), so
                    // `<RuleRef> -> feature` resolves (e.g.
                    // UnderlierProductIdentifierOther -> identifier).
                    RMetaAnnotatedType outputType;
                    if (symbolOutput.isPresent()) {
                        outputType = resolver.inferAttributeRefType(symbolOutput.get());
                    } else if (symbol instanceof com.regnosys.rosetta.ast.functions.RRule outputRule
                            && outputRule.expression().isPresent()) {
                        outputType = getInferredType(outputRule.expression().get());
                    } else {
                        outputType = RMetaAnnotatedType.MISSING;
                    }
                    if (!outputType.isMissing()) {
                            java.util.Optional<RAttribute> feature =
                                resolver.findAttributeOnType(
                                    outputType.type(), enr.valueName());
                            if (feature.isPresent()) {
                                // One-segment AttributeChain shape — the LHS
                                // resolution is captured separately via
                                // resolvedSymbol; only the feature attribute
                                // backs the chain's typing.
                                enr.setResolvedAttributeChain(
                                    new REnumValueRef.AttributeChain(feature.get()));
                                if (referenceIndex != null) {
                                    referenceIndex.registerReference(enr, feature.get());
                                }
                                collector.removeMatching(
                                    DiagnosticCategory.ENUM_NOT_FOUND, enr.sourceRange());
                                collector.removeMatching(
                                    DiagnosticCategory.ENUM_VALUE_NOT_FOUND, enr.sourceRange());
                            } else if (enr.resolvedChoiceOption().isEmpty()
                                    && outputType.type()
                                        instanceof com.regnosys.rosetta.types.RChoiceTypeRef outputChoice) {
                                // Phase X1 closure T10 — Gap A choice-option
                                // navigation. The func/rule LHS output is a
                                // CHOICE and <valueName> names one of its options
                                // rather than a feature (e.g.
                                // `underlier.UnderlierForProduct(..) -> Product`
                                // where UnderlierForProduct returns the Underlier
                                // choice and Product is one of its options). The
                                // feature lookup above bails (Product is a TYPE,
                                // not an attribute); narrow to the option here.
                                // Mirrors the simple-attribute-LHS choice-option
                                // path; shares matchChoiceOption.
                                com.regnosys.rosetta.ast.RRootElement optionNode =
                                    matchChoiceOption(outputChoice, enr.valueName());
                                if (optionNode != null) {
                                    enr.setResolvedChoiceOption(optionNode);
                                    enr.setChoiceOptionHeadIsDirectChoice(true);
                                    if (referenceIndex != null) {
                                        referenceIndex.registerReference(enr, optionNode);
                                    }
                                    collector.removeMatching(
                                        DiagnosticCategory.ENUM_NOT_FOUND, enr.sourceRange());
                                    collector.removeMatching(
                                        DiagnosticCategory.ENUM_VALUE_NOT_FOUND, enr.sourceRange());
                                }
                            }
                        }
                    }
                // Phase X1 closure T0i (Gap B) — attribute → type-restriction
                // (downcast). GlobalResolutionPass.resolveEnumValueRef binds
                // {@code resolvedRestrictionType} when the RHS valueName
                // resolves to a workspace RDataType. Here we verify by looking
                // up the LHS enumName as an attribute on item-type + checking
                // that the restriction target is a subtype of the LHS
                // attribute's element type via SubtypeRelation. On
                // verification success, bind the full
                // {@link REnumValueRef.TypeRestriction} record so
                // ExpressionTypeComputer can compute the narrowed type, +
                // clear stale enum diagnostics. On verification failure
                // (LHS not an attribute, or RHS not a subtype) leave the
                // speculative bind in place; downstream type computation
                // reads ONLY the verified record so MISSING propagates as
                // before.
                if (enr.resolvedAttributeChain().isEmpty()
                        && enr.resolvedChoiceOption().isEmpty()
                        && enr.resolvedTypeRestriction().isEmpty()
                        && enr.resolvedRestrictionType().isPresent()) {
                    RMetaAnnotatedType itemTypeForRestriction =
                        getEnclosingItemType(enr);
                    if (!itemTypeForRestriction.isMissing()) {
                        java.util.Optional<RAttribute> lhsAttr =
                            resolver.findAttributeOnType(
                                itemTypeForRestriction.type(), enr.enumName());
                        if (lhsAttr.isPresent()) {
                            RMetaAnnotatedType lhsType =
                                resolver.inferAttributeRefType(lhsAttr.get());
                            if (!lhsType.isMissing()) {
                                com.regnosys.rosetta.ast.types.RDataType restrictionAst =
                                    enr.resolvedRestrictionType().get();
                                com.regnosys.rosetta.types.RType restrictionRType =
                                    new com.regnosys.rosetta.types.RDataTypeRef(restrictionAst);
                                if (typeComputer.subtypeRelation()
                                        .isSubtypeOf(restrictionRType, lhsType.type())) {
                                    enr.setResolvedTypeRestriction(
                                        new REnumValueRef.TypeRestriction(
                                            lhsAttr.get(), restrictionAst));
                                    if (referenceIndex != null) {
                                        // Register BOTH cross-refs at verified
                                        // bind — Phase A deliberately skips
                                        // registerReference on the speculative
                                        // resolvedRestrictionType bind to avoid
                                        // leaking an orphan REnumValueRef →
                                        // RDataType ref into the reverse index
                                        // when Phase B verification fails.
                                        referenceIndex.registerReference(enr, lhsAttr.get());
                                        referenceIndex.registerReference(enr, restrictionAst);
                                    }
                                    collector.removeMatching(
                                        DiagnosticCategory.ENUM_NOT_FOUND, enr.sourceRange());
                                    collector.removeMatching(
                                        DiagnosticCategory.ENUM_VALUE_NOT_FOUND, enr.sourceRange());
                                }
                            }
                        }
                    }
                }
                // IR-Lab cascade fix (2026-06-27) — function-input-reference nav.
                // `<name> -> <feature>` parses as REnumValueRef(enumName=name,
                // valueName=feature). When <name> is the enclosing FUNCTION's input
                // (by param name OR the implicit lowercase-input-type-name
                // convention — both via {@link #enclosingFunctionInput}), the nav
                // reads <feature> on that input's type. The engine already resolves
                // the analogous shape inside RULE bodies (getEnclosingItemType ->
                // rule.fromType) and inside lambdas (the implicit-item path), but a
                // TOP-LEVEL nav in a FUNCTION body has no enclosing rule and no
                // enclosing inline function, so getEnclosingItemType returns MISSING
                // and none of the branches above fire — the dominant MISSING cascade
                // the IR-Lab head->feature census measured (~92% of the MISSING
                // bases; ~77% with head = the function input). Resolves from the
                // enclosing RFunction's inputs (static AST only — no GeneratorModel)
                // into a SEPARATE, TYPING-ONLY {@code resolvedInputFeature} field — kept
                // distinct from resolvedAttributeChain (which the generator reads to
                // RENDER navs), so it exposes the type via getInferredType without the
                // generator binding it as a nav. The Path-1 generator DOES still consult
                // getInferredType, so the improved type is byte-POSITIVE (+6 FUNCTION
                // flips); the field separation is a separation-of-concerns boundary, NOT
                // a byte-neutrality claim.
                //
                // Gated LAST (all of enum / item-attribute / Gap-A func-rule / Gap-B
                // downcast / choice-option already declined) so a genuine binding
                // always wins; idempotent via the resolvedInputFeature().isEmpty()
                // guard. The stale ENUM_NOT_FOUND diagnostic is left in place (a
                // benign pre-existing false-positive) — clearing it is a further
                // resolution improvement intentionally out of scope for this typing fix.
                // (enr.enumeration()/enumValue() are already empty here — the block
                // entry guards them; re-check only the resolution fields that inner
                // branches above may have bound, so a genuine binding always wins.)
                if (enr.resolvedAttributeChain().isEmpty()
                        && enr.resolvedSymbol().isEmpty()
                        && enr.resolvedChoiceOption().isEmpty()
                        && enr.resolvedTypeRestriction().isEmpty()
                        && enr.resolvedInputFeature().isEmpty()) {
                    // The #279 BYTE-SAFETY GATE (alias/RShortcut bodies excluded)
                    // was LIFTED here 2026-07-18 (the leg-C #11/#16 typing-channel
                    // heal). The gate existed because typing an alias body
                    // PROPAGATES to its consumers, and at #279 two within-waiver
                    // corpus consumers carried unhealed generator facets (the
                    // #221/#226 type-switch re-root; a witness-import) that the
                    // better type would have exposed. Both facets have since
                    // healed (the corpus is at byte-identity, 34,685/34,686 with
                    // the ONE upstream-defect PERMANENT), and the alias-body navs
                    // were the gate's own documented follow-on: an alias body that
                    // stays MISSING renders compile-breaking output at list-literal
                    // seats (`MapperC.<Void>of(…)` — finding #11's witness). The
                    // D11 population run is the regression oracle for the lift.
                    RAttribute input = enclosingFunctionInput(enr);
                    // valueName() is nullable; findAttributeOnType does name.equals(attr.name())
                    // and would NPE on a null/empty feature name in an incomplete tree.
                    // (Copilot R1 on PR #279.)
                    if (input != null && enr.valueName() != null && !enr.valueName().isEmpty()) {
                        RMetaAnnotatedType inputType = resolver.inferAttributeRefType(input);
                        if (!inputType.isMissing()) {
                            java.util.Optional<RAttribute> feature =
                                resolver.findAttributeOnType(inputType.type(), enr.valueName());
                            if (feature.isPresent()) {
                                enr.setResolvedInputFeature(feature.get());
                                cat8Changed = true;
                                // PR #443 #4-class-(a): the successful bind now ALSO
                                // clears the stale M3 enum diagnostics, exactly like
                                // every sibling Cat-10 channel above — the #279 note
                                // reserved this as the follow-on resolution
                                // improvement. Upstream emits NOTHING for a resolved
                                // input nav; leaving the diagnostic reported a
                                // resolution failure the node no longer has (the
                                // dominant half of the ENUM_NOT_FOUND census mass:
                                // 3,598 cdm + 5,418 drr rows at the #443 count).
                                collector.removeMatching(
                                    DiagnosticCategory.ENUM_NOT_FOUND, enr.sourceRange());
                                collector.removeMatching(
                                    DiagnosticCategory.ENUM_VALUE_NOT_FOUND, enr.sourceRange());
                            } else if (resolver.isRecordFeatureOnType(
                                    inputType.type(), enr.valueName())) {
                                // PR #444 record-member leaf, CLEARING-ONLY. The
                                // input's type is a builtin RECORD (date /
                                // dateTime / zonedDateTime) and the leaf is one
                                // of its features (`endDate -> year` on a `date`
                                // input — the daycount dispatch variants'
                                // dominant shape). Record features are not
                                // RAttributes, so there is NOTHING to bind into
                                // resolvedInputFeature; the resolution is
                                // nonetheless proven (upstream types record
                                // features via RRecordFeature and emits nothing
                                // for these navs — the V0 oracle streams are
                                // error-free over every carrier), so the stale
                                // tried-enum diagnostics are removed. No
                                // cat8Changed — a collector-only action feeds no
                                // typing consumer (the #443 alias-arm
                                // convention); removeMatching is idempotent
                                // across fixed-point iterations.
                                collector.removeMatching(
                                    DiagnosticCategory.ENUM_NOT_FOUND, enr.sourceRange());
                                collector.removeMatching(
                                    DiagnosticCategory.ENUM_VALUE_NOT_FOUND, enr.sourceRange());
                            } else if (resolver.isChoiceOptionOnType(
                                    inputType.type(), enr.valueName())) {
                                // PR #446 choice-option leaf; PR #449 BIND-MODE.
                                // The input's type is a CHOICE and the leaf names
                                // one of its OPTIONS (`observable -> Asset` on an
                                // input typed Observable). Upstream's feature
                                // scope on a choice receiver admits its options
                                // as (0..1) attributes (the asRDataType view —
                                // see isChoiceOptionOnType), so the nav is plain
                                // attribute navigation upstream and it emits
                                // NOTHING (the V0 oracle streams are error-free
                                // over every carrier). The engine's Phase X1 T10
                                // arms already BIND resolvedChoiceOption for the
                                // same shape in ITEM-headed contexts; #446 landed
                                // this channel clearing-only under the
                                // TYPING-EXPOSURE law, and #449 lands the bind:
                                // the nav types as the matched option
                                // (astNodeToRType — data type or nested choice),
                                // so alias bodies heading these navs type and the
                                // census's alias:MISSING cascade heals. The bind
                                // is a SUBSET of the boolean admission
                                // (choiceOptionNodeOnType returns null for an
                                // unresolvable option target — such rows keep
                                // the #446 clearing-only behaviour).
                                com.regnosys.rosetta.ast.RRootElement optionNode =
                                    resolver.choiceOptionNodeOnType(
                                        inputType.type(), enr.valueName());
                                if (optionNode != null) {
                                    enr.setResolvedChoiceOption(optionNode);
                                    // v3.1 C1 (spec R9): record the option DECLARATION alongside the
                                    // legacy type-node bind. Upstream binds ChoiceOption <Choice>.<Option>;
                                    // the type alone loses which choice the option came through.
                                    enr.setResolvedFeatureNode(
                                            resolver.choiceOptionDeclarationOnType(inputType.type(), enr.valueName()));
                                    enr.setChoiceOptionHeadIsDirectChoice(
                                        inputType.type()
                                            instanceof com.regnosys.rosetta.types.RChoiceTypeRef);
                                    if (referenceIndex != null) {
                                        referenceIndex.registerReference(enr, optionNode);
                                    }
                                    cat8Changed = true;
                                }
                                collector.removeMatching(
                                    DiagnosticCategory.ENUM_NOT_FOUND, enr.sourceRange());
                                collector.removeMatching(
                                    DiagnosticCategory.ENUM_VALUE_NOT_FOUND, enr.sourceRange());
                            }
                        }
                    }
                }
                // PR #443 #4-class-(a) — ALIAS-headed nav; PR #447 BIND-MODE.
                // `<alias> -> <feature>` parses as REnumValueRef(enumName=alias,
                // valueName=feature) like every other disguised 2-segment chain;
                // when the leading name is one of the enclosing FUNCTION's
                // shortcuts and <feature> resolves on the alias EXPRESSION's
                // inferred type, the linker's tried-enum diagnostic is a proven
                // false positive (upstream scopes ShortcutDeclarations as plain
                // symbols — its parse never takes the enum path and it emits
                // nothing) and is REMOVED. The attribute case now ALSO BINDS
                // resolvedInputFeature (the #447 typed-alias-nav wave): the nav
                // types as the leaf attribute's meta-annotated type, exactly
                // like the input channel above, so CHAINED aliases type in the
                // fixed-point iteration (an alias whose expression heads another
                // alias — the census's headTypeMissing block, 101 cdm + 84 drr
                // rows, could never reach this arm before because the head
                // alias's own expression stayed MISSING). At #443 this bind was
                // measured moving 2 drr FUNCTION goldens (ExtractReferenceEntity
                // + hkma Extract_ReferenceEntityFormat — the generator's
                // meta-coercion placement shifts when a `<alias> -> <meta leaf>`
                // conditional arm's type becomes known) and was deferred
                // clearing-only under the TYPING-EXPOSURE law; the #447 wave
                // lands the bind WITH that generator seat aligned (the
                // conditional-arm ladder keeps upstream's raw-meta arms +
                // whole-output deref placement — see FunctionExpressionRenderer)
                // and the cp gate re-derived byte-equal. The record case stays
                // CLEARING-ONLY (record features are not RAttributes — nothing
                // bindable); the choice case BINDS since PR #449 (the bind-mode
                // probe measured it byte-neutral over the full 55-param
                // population — see the input channel's choice arm). Runs every
                // fixed-point iteration (an alias body may type late);
                // removeMatching is idempotent and the binds set cat8Changed so
                // dependent alias bodies re-type.
                if (enr.resolvedAttributeChain().isEmpty()
                        && enr.resolvedSymbol().isEmpty()
                        && enr.resolvedChoiceOption().isEmpty()
                        && enr.resolvedTypeRestriction().isEmpty()
                        && enr.resolvedInputFeature().isEmpty()
                        && enr.valueName() != null && !enr.valueName().isEmpty()) {
                    com.regnosys.rosetta.ast.functions.RShortcut aliasHead =
                        enclosingFunctionShortcut(enr);
                    if (aliasHead != null && aliasHead.expression() != null) {
                        RMetaAnnotatedType aliasType =
                            getInferredType(aliasHead.expression());
                        if (!aliasType.isMissing()) {
                            java.util.Optional<RAttribute> aliasFeature =
                                resolver.findAttributeOnType(aliasType.type(), enr.valueName());
                            if (aliasFeature.isPresent()) {
                                enr.setResolvedInputFeature(aliasFeature.get());
                                cat8Changed = true;
                                collector.removeMatching(
                                    DiagnosticCategory.ENUM_NOT_FOUND, enr.sourceRange());
                                collector.removeMatching(
                                    DiagnosticCategory.ENUM_VALUE_NOT_FOUND, enr.sourceRange());
                            } else if (
                                // PR #444: an alias whose expression types as a
                                // builtin RECORD admits its record features the
                                // same way — the same clearing-only contract,
                                // nothing bindable either side.
                                resolver.isRecordFeatureOnType(
                                        aliasType.type(), enr.valueName())) {
                                collector.removeMatching(
                                    DiagnosticCategory.ENUM_NOT_FOUND, enr.sourceRange());
                                collector.removeMatching(
                                    DiagnosticCategory.ENUM_VALUE_NOT_FOUND, enr.sourceRange());
                            } else if (resolver.isChoiceOptionOnType(
                                        aliasType.type(), enr.valueName())) {
                                // PR #446: an alias whose expression types as a
                                // CHOICE admits its options the same way
                                // (`underlier -> Product` on an alias typed
                                // Underlier — upstream's asRDataType feature
                                // scope; see the input channel's choice arm).
                                // PR #449 BIND-MODE: same contract as the input
                                // channel's choice arm — bind the matched option
                                // (subset-of-admission; null keeps clearing-only).
                                com.regnosys.rosetta.ast.RRootElement aliasOptionNode =
                                    resolver.choiceOptionNodeOnType(
                                        aliasType.type(), enr.valueName());
                                if (aliasOptionNode != null) {
                                    enr.setResolvedChoiceOption(aliasOptionNode);
                                    // v3.1 C1 (spec R9): record the option DECLARATION alongside the
                                    // legacy type-node bind. Upstream binds ChoiceOption <Choice>.<Option>;
                                    // the type alone loses which choice the option came through.
                                    enr.setResolvedFeatureNode(
                                            resolver.choiceOptionDeclarationOnType(aliasType.type(), enr.valueName()));
                                    enr.setChoiceOptionHeadIsDirectChoice(
                                        aliasType.type()
                                            instanceof com.regnosys.rosetta.types.RChoiceTypeRef);
                                    if (referenceIndex != null) {
                                        referenceIndex.registerReference(enr, aliasOptionNode);
                                    }
                                    cat8Changed = true;
                                }
                                collector.removeMatching(
                                    DiagnosticCategory.ENUM_NOT_FOUND, enr.sourceRange());
                                collector.removeMatching(
                                    DiagnosticCategory.ENUM_VALUE_NOT_FOUND, enr.sourceRange());
                            }
                        }
                    }
                }
                // PR #444 — OUTPUT-headed nav; PR #449 BIND-MODE. `<output> ->
                // <feature>` inside a function body parses as the same disguised
                // REnumValueRef; the OUTPUT symbol is in upstream's function
                // body scope exactly like the inputs (the base-aware getOutput —
                // vendored RosettaScopeProvider.getSymbolParentScope:394-400),
                // so when the leaf resolves on the output's type (attribute OR
                // record feature) the tried-enum diagnostic is a proven false
                // positive and is REMOVED (the #444 census: 23 cdm + 15 drr
                // rows, every one leaf-resolving). #444 landed clearing-only
                // under the TYPING-EXPOSURE law; #449 lands the bind exactly
                // like the input channel above (attribute → resolvedInputFeature,
                // choice option → resolvedChoiceOption, record feature →
                // clearing-only, nothing bindable), so an alias whose body is an
                // output-headed nav chain types and the census's alias:MISSING
                // cascade heals (`alias payout: product -> contractualProduct ->
                // economicTerms -> payout` — the inner disguised node types,
                // the outer segments follow, the alias types, its consumers
                // bind via the #447 alias arm).
                // The head lookup follows upstream's REPLACE semantics (the
                // Seat-1 #444 MF-3 law, getOutput's contract): the dispatch
                // BASE's output whenever a base exists, the function's own
                // output otherwise. PRE-conditions are
                // EXCLUDED: upstream filters the output symbol out of a
                // non-post condition's scope (vendored
                // RosettaScopeProvider.getSymbolParentScope:405-406 — only
                // isPostCondition() sees FUNCTION__OUTPUT descriptors), so an
                // output-headed nav inside a function pre-condition is
                // upstream-DIAGNOSED and must keep the fork's diagnostic too;
                // the guard declines on any RCondition ancestor
                // (RPostCondition is a separate class and passes). Every
                // corpus carrier sits in an operation body — the V0 oracles
                // are error-free, so none can be a pre-condition case.
                if (enr.resolvedAttributeChain().isEmpty()
                        && enr.resolvedSymbol().isEmpty()
                        && enr.resolvedChoiceOption().isEmpty()
                        && enr.resolvedTypeRestriction().isEmpty()
                        && enr.resolvedInputFeature().isEmpty()
                        && enr.enumName() != null && !enr.enumName().isEmpty()
                        && enr.valueName() != null && !enr.valueName().isEmpty()
                        && AstWalker.findAncestor(
                            enr, com.regnosys.rosetta.ast.functions.RCondition.class).isEmpty()) {
                    com.regnosys.rosetta.ast.functions.RFunction outFn =
                        AstWalker.findAncestor(
                            enr, com.regnosys.rosetta.ast.functions.RFunction.class).orElse(null);
                    if (outFn != null) {
                        java.util.Optional<RAttribute> output =
                            outFn.dispatchBase()
                                .map(com.regnosys.rosetta.ast.functions.RFunction::output)
                                .orElseGet(outFn::output);
                        if (output.isPresent()
                                && enr.enumName().equals(output.get().name())) {
                            RMetaAnnotatedType outType =
                                resolver.inferAttributeRefType(output.get());
                            if (!outType.isMissing()) {
                                java.util.Optional<RAttribute> outFeature =
                                    resolver.findAttributeOnType(
                                        outType.type(), enr.valueName());
                                if (outFeature.isPresent()) {
                                    // PR #449: bind exactly like the input
                                    // channel — the nav types as the leaf
                                    // attribute and dependent alias bodies
                                    // re-type in the fixed point.
                                    enr.setResolvedInputFeature(outFeature.get());
                                    cat8Changed = true;
                                    collector.removeMatching(
                                        DiagnosticCategory.ENUM_NOT_FOUND, enr.sourceRange());
                                    collector.removeMatching(
                                        DiagnosticCategory.ENUM_VALUE_NOT_FOUND,
                                        enr.sourceRange());
                                } else if (resolver.isRecordFeatureOnType(
                                        outType.type(), enr.valueName())) {
                                    // Record features stay CLEARING-ONLY —
                                    // nothing bindable (the #444 contract).
                                    collector.removeMatching(
                                        DiagnosticCategory.ENUM_NOT_FOUND, enr.sourceRange());
                                    collector.removeMatching(
                                        DiagnosticCategory.ENUM_VALUE_NOT_FOUND,
                                        enr.sourceRange());
                                } else if (resolver.isChoiceOptionOnType(
                                        outType.type(), enr.valueName())) {
                                    // PR #446: a choice-typed output admits its
                                    // options the same way (zero corpus carriers
                                    // at the #446 census — the arm is the same
                                    // upstream feature-scope table applied at
                                    // the remaining head channel; see the input
                                    // channel's choice arm). PR #449 BIND-MODE
                                    // (subset-of-admission; null keeps
                                    // clearing-only).
                                    com.regnosys.rosetta.ast.RRootElement outOptionNode =
                                        resolver.choiceOptionNodeOnType(
                                            outType.type(), enr.valueName());
                                    if (outOptionNode != null) {
                                        enr.setResolvedChoiceOption(outOptionNode);
                                        // v3.1 C1 (spec R9): record the option DECLARATION alongside the
                                        // legacy type-node bind. Upstream binds ChoiceOption <Choice>.<Option>;
                                        // the type alone loses which choice the option came through.
                                        enr.setResolvedFeatureNode(
                                                resolver.choiceOptionDeclarationOnType(outType.type(), enr.valueName()));
                                        enr.setChoiceOptionHeadIsDirectChoice(
                                            outType.type()
                                                instanceof com.regnosys.rosetta.types.RChoiceTypeRef);
                                        if (referenceIndex != null) {
                                            referenceIndex.registerReference(
                                                enr, outOptionNode);
                                        }
                                        cat8Changed = true;
                                    }
                                    collector.removeMatching(
                                        DiagnosticCategory.ENUM_NOT_FOUND, enr.sourceRange());
                                    collector.removeMatching(
                                        DiagnosticCategory.ENUM_VALUE_NOT_FOUND,
                                        enr.sourceRange());
                                }
                            }
                        }
                    }
                }
                // PR #443 #4-class-(a) — DATA-TYPE-CONDITION nav. Inside a data
                // type's condition body, `<attr> -> <feature>` navigates from an
                // attribute of the DECLARING type (the condition's implicit item
                // — the scope LexicalResolutionPass's buildTypeConditionScope
                // already gives bare RSymbolReferences): `leg1 -> direction2` on
                // the asic TransactionReport reads direction2 on leg1's type.
                // The Cat-10 family never fired here because getEnclosingItemType
                // has no condition arm (rule/lambda contexts only) — the census's
                // second-largest NONE bucket (1,082 cdm + 4,127 drr at the #443
                // count). Head lookup walks the declaring type's supertype chain
                // (findAttributeOnType via RDataTypeRef — the same leaf-first
                // walk the condition scope registers). TYPING-ONLY bind +
                // clearing, exactly like the alias arm above; deliberately NOT
                // wired through getEnclosingItemType, whose other consumers
                // (Cat 8/9, the chain-binding standard 2-step the generator
                // renders from) would move generator-visible state.
                if (enr.resolvedAttributeChain().isEmpty()
                        && enr.resolvedSymbol().isEmpty()
                        && enr.resolvedChoiceOption().isEmpty()
                        && enr.resolvedTypeRestriction().isEmpty()
                        && enr.resolvedInputFeature().isEmpty()
                        && enr.enumName() != null && !enr.enumName().isEmpty()
                        && enr.valueName() != null && !enr.valueName().isEmpty()) {
                    com.regnosys.rosetta.ast.types.RDataType conditionType =
                        enclosingConditionDataType(enr);
                    if (conditionType != null) {
                        java.util.Optional<RAttribute> head =
                            resolver.findAttributeOnType(
                                new com.regnosys.rosetta.types.RDataTypeRef(conditionType),
                                enr.enumName());
                        if (head.isPresent()) {
                            RMetaAnnotatedType headType =
                                resolver.inferAttributeRefType(head.get());
                            if (!headType.isMissing()) {
                                java.util.Optional<RAttribute> feature =
                                    resolver.findAttributeOnType(
                                        headType.type(), enr.valueName());
                                if (feature.isPresent()) {
                                    enr.setResolvedInputFeature(feature.get());
                                    cat8Changed = true;
                                    collector.removeMatching(
                                        DiagnosticCategory.ENUM_NOT_FOUND, enr.sourceRange());
                                    collector.removeMatching(
                                        DiagnosticCategory.ENUM_VALUE_NOT_FOUND, enr.sourceRange());
                                } else if (resolver.isRecordFeatureOnType(
                                        headType.type(), enr.valueName())) {
                                    // PR #444 record-member leaf, CLEARING-ONLY
                                    // (`reportingTimestamp -> date` on a
                                    // zonedDateTime attribute — the DRR report
                                    // types' dominant condition shape). Same
                                    // contract as the input channel's record
                                    // branch: nothing bindable, resolution
                                    // proven, diagnostics removed, no
                                    // cat8Changed.
                                    collector.removeMatching(
                                        DiagnosticCategory.ENUM_NOT_FOUND, enr.sourceRange());
                                    collector.removeMatching(
                                        DiagnosticCategory.ENUM_VALUE_NOT_FOUND, enr.sourceRange());
                                } else if (resolver.isChoiceOptionOnType(
                                        headType.type(), enr.valueName())) {
                                    // PR #446 choice-option leaf
                                    // (`payout -> InterestRatePayout` inside an
                                    // EconomicTerms condition — the family's
                                    // dominant condition shape, 66 of the #446
                                    // census's 140). Same contract as the input
                                    // channel's choice arm; PR #449 BIND-MODE
                                    // (subset-of-admission; null keeps
                                    // clearing-only).
                                    com.regnosys.rosetta.ast.RRootElement condOptionNode =
                                        resolver.choiceOptionNodeOnType(
                                            headType.type(), enr.valueName());
                                    if (condOptionNode != null) {
                                        enr.setResolvedChoiceOption(condOptionNode);
                                        // v3.1 C1 (spec R9): record the option DECLARATION alongside the
                                        // legacy type-node bind. Upstream binds ChoiceOption <Choice>.<Option>;
                                        // the type alone loses which choice the option came through.
                                        enr.setResolvedFeatureNode(
                                                resolver.choiceOptionDeclarationOnType(headType.type(), enr.valueName()));
                                        enr.setChoiceOptionHeadIsDirectChoice(
                                            headType.type()
                                                instanceof com.regnosys.rosetta.types.RChoiceTypeRef);
                                        if (referenceIndex != null) {
                                            referenceIndex.registerReference(
                                                enr, condOptionNode);
                                        }
                                        cat8Changed = true;
                                    }
                                    collector.removeMatching(
                                        DiagnosticCategory.ENUM_NOT_FOUND, enr.sourceRange());
                                    collector.removeMatching(
                                        DiagnosticCategory.ENUM_VALUE_NOT_FOUND, enr.sourceRange());
                                } else if (resolver.isMetaFaceOnAttribute(
                                        head.get(), enr.valueName())) {
                                    // PR #451 metadata-face leaf at the
                                    // condition channel (`quantityReference ->
                                    // reference exists` in the PayoutBase /
                                    // ResolvablePriceQuantity conditions — the
                                    // head attribute declares [metadata
                                    // reference]). Same CLEARING-ONLY contract
                                    // as the 2-step's meta rung: upstream
                                    // admits the face as a feature of the
                                    // receiver's meta-annotated type and emits
                                    // nothing; nothing bindable.
                                    collector.removeMatching(
                                        DiagnosticCategory.ENUM_NOT_FOUND, enr.sourceRange());
                                    collector.removeMatching(
                                        DiagnosticCategory.ENUM_VALUE_NOT_FOUND, enr.sourceRange());
                                }
                            }
                        }
                    }
                }
            }
        }
        // Categories 3, 5-7 (operation paths, annotations, externals, with-meta)
        // are resolved via AstWalker over non-expression nodes. These require
        // the containing type context which comes from M3's resolved parent type,
        // not from expression inference. They are resolved once during pass 6
        // (not iteratively) since they don't depend on expression types.
        return cat8Changed;
    }

    /**
     * Compute the implicit-item type for an {@link RImplicitVariable} — both
     * forms the parser now emits:
     * <ul>
     *   <li><b>Literal {@code item} keyword</b> ({@code iv.isSynthetic()==false}) —
     *       the keyword is shadowed by any explicit-parameter inline function
     *       ancestor, so the literal branch gates on
     *       {@code RInlineFunction.isImplicit()}: only an IMPLICIT body provides
     *       a valid item type. If the keyword sits inside an explicit-parameter
     *       closure (a semantic error in source), this branch returns MISSING.</li>
     *   <li><b>Synthetic elided operand</b> ({@code iv.isSynthetic()==true}) —
     *       emitted by {@code AstBuilder.syntheticImplicitInput} for
     *       without-left list/extract/filter/conversion/toString forms (engine
     *       PR #1). Resolved through a dedicated branch via
     *       {@link #isElidedOperandImplicitVariable}: either the rule's from-type
     *       (top-level) or the outer chain step's element type (nested).</li>
     * </ul>
     *
     * <p>Cat 9 (bare RSymbolReference) and Cat 10 (REnumValueRef chain) use
     * {@link #getEnclosingItemType} directly — that method handles both
     * implicit AND explicit bodies because the explicit-parameter type IS the
     * same as the implicit-item type (both equal the chain op's argument
     * element type).
     */
    private RMetaAnnotatedType computeImplicitItemType(RImplicitVariable iv) {
        if (isElidedOperandImplicitVariable(iv)) {
            // Synthetic elided-operand: the implicit input is what pipes INTO
            // the parent op. Two cases — mirrors
            // {@link #computeArgumentElementType}'s null-arg fallback below:
            //   (a) NESTED — the parent op sits inside an outer chain's inline
            //       function body (e.g. `... then extract dateTime` where the
            //       inner extract's elided receiver is the outer chain's piped
            //       item, NOT the rule's input). Walk to the outer chain op
            //       via the wrapping inline function.
            //   (b) TOP-LEVEL — the parent op sits directly at the rule body
            //       root. The elided receiver is the rule's from-type.
            RNode parentOp = iv.parent();
            RInlineFunction wrappingInline = parentOp == null
                    ? null
                    : AstWalker.findAncestor(parentOp, RInlineFunction.class).orElse(null);
            if (wrappingInline != null) {
                return computeArgumentElementType(wrappingInline.parent());
            }
            RRule enclosingRule = parentOp == null
                    ? null
                    : AstWalker.findAncestor(parentOp, RRule.class).orElse(null);
            if (enclosingRule != null) {
                return typeComputer.inferRuleFromType(enclosingRule);
            }
            return RMetaAnnotatedType.MISSING;
        }
        // PR #451 — switch-case item narrowing for the literal `item` keyword:
        // a case result written as `then item` (the tradestate fpmlProduct
        // alias's switch-of-items shape) IS the narrowed guard type — upstream's
        // implicit-variable walk stops at the narrowing case before any lambda.
        // Consulted first; null falls through to the pre-#451 literal branch
        // (including the D41-LOCK explicit-parameter shadowing) unchanged.
        RMetaAnnotatedType narrowedCase = narrowedSwitchCaseItemType(iv);
        if (narrowedCase != null) {
            return narrowedCase;
        }
        // The literal `item` keyword binds to the chain item UNLESS an explicit
        // closure parameter shadows it. Two body shapes carry the keyword:
        //   - an implicitInlineFunction body (isImplicit==true) — extract / filter
        //     / reduce / then written without brackets;
        //   - a NO-closure-parameter inlineFunction body (isImplicit==false,
        //     paramNames().isEmpty()) — sort / max / min ALWAYS take this form
        //     (grammar: `expression (SORT|MIN|MAX) inlineFunction?`, no
        //     implicitInlineFunction alternative), and extract/filter/reduce may
        //     when written `[ item -> … ]`. The `item` keyword there is
        //     unambiguous (no param to shadow it), so it types as the chain
        //     item exactly like the implicit form.
        // IR-Lab PART A (2026-06-25): widening the filter from isImplicit() to
        // also accept no-param bodies is the second half of typing the sort/max/min
        // lambda item — without it the switch cases above only reach the bare-attr
        // (Cat 9, getEnclosingItemType — no isImplicit filter) + enum-chain (Cat 10)
        // paths, not the dominant literal-`item` form (`sort [ item -> nav ]`).
        // A body WITH a closure parameter keeps the conservative MISSING (the
        // D41-LOCK shadowing case), unchanged.
        java.util.Optional<RInlineFunction> anyInline =
                AstWalker.findAncestor(iv, RInlineFunction.class);
        RInlineFunction implicitBody = anyInline
                .filter(fn -> fn.isImplicit() || fn.paramNames().isEmpty())
                .orElse(null);
        if (implicitBody == null) {
            // facet ruleRecursionTyping (PR #437, finding #32): the literal `item`
            // keyword DIRECTLY in a rule body — no inline-function ancestor at all
            // (`reporting rule Fac from int: if item = 1 then 1 else item * Fac(item
            // - 1)`) — types as the rule's from-type: upstream
            // safeTypeOfImplicitVariable's RosettaRule arm (getRuleInputType), the
            // same fallback the SYNTHETIC branch above and Cat 9/10's
            // getEnclosingItemType already carry. The D41-LOCK shadowing law is
            // untouched: a PRESENT-but-filtered explicit-parameter closure ancestor
            // (anyInline non-empty) keeps the conservative MISSING.
            if (anyInline.isEmpty()) {
                RRule enclosingRule = AstWalker.findAncestor(iv, RRule.class).orElse(null);
                if (enclosingRule != null) {
                    return typeComputer.inferRuleFromType(enclosingRule);
                }
            }
            return RMetaAnnotatedType.MISSING;
        }
        return computeArgumentElementType(implicitBody.parent());
    }

    /**
     * Walks up the AST to find the enclosing RInlineFunction parent (implicit
     * OR explicit-parameter), then up one more level to the chain op
     * (RExtractExpr / RFilterExpr / RThenExpr / RReduceExpr). Returns the op's
     * argument-element type. If no enclosing inline function is found (the
     * node sits directly in the rule body), falls back to the enclosing
     * {@code RRule}'s {@code fromType}.
     *
     * <p>Phase X1 closure (D41 LOCK 2026-05-23) — the {@code isImplicit}
     * filter was previously applied here, which broke Cat 9/10 inside
     * explicit-parameter closures (e.g. {@code extract reportableCollateral
     * [reportableCollateral -> reportableInformation ...]} fragments in the
     * DRR corpus). The explicit parameter's type IS the chain op argument
     * element type, so the answer for explicit + implicit cases is identical.
     * The filter remains in {@link #computeImplicitItemType} (Cat 8 only)
     * because the literal {@code item} keyword is shadowed by an explicit
     * parameter.
     *
     * <p>Uses RNode.parent() (existing public accessor at RNode.java:160)
     * for the inline → op step. AstWalker has no parent() helper.
     */
    /**
     * Phase X1 closure T0h (Gap A): returns the declarative output RAttribute
     * of a callable symbol for use in REnumValueRef chain resolution. Only
     * RFunction has a declared output attribute in the AST; RRule's output is
     * the inferred type of its expression, which depends on the fixed-point
     * and isn't representable as a single RAttribute, so RRule case returns
     * empty here. The RRule case can be addressed in a follow-up sub-cause
     * that threads inferred-expression-type back into the chain typing.
     */
    private static java.util.Optional<RAttribute> symbolOutputAttribute(
            com.regnosys.rosetta.ast.RRootElement symbol) {
        if (symbol instanceof com.regnosys.rosetta.ast.functions.RFunction fn) {
            return fn.output();
        }
        return java.util.Optional.empty();
    }

    /**
     * IR-Lab cascade fix — the enclosing {@link com.regnosys.rosetta.ast.functions.RFunction}
     * input that a disguised {@code <name> -> <feature>} nav references, or
     * {@code null}. Two match shapes (both unambiguous function-input references,
     * the function analogue of the rule from-type implicit scope):
     * <ol>
     *   <li>an input whose PARAM NAME equals the nav's {@code enumName}
     *       ({@code before -> trade});</li>
     *   <li>the implicit-input convention — an input whose TYPE's simple name,
     *       lower-cased, equals it ({@code tradeState -> trade} on an input typed
     *       {@code TradeState}). Most corpus carriers also satisfy (1) because the
     *       param is named the lowercase type; (2) covers the residual.</li>
     * </ol>
     */
    private static RAttribute enclosingFunctionInput(REnumValueRef enr) {
        com.regnosys.rosetta.ast.functions.RFunction fn =
            AstWalker.findAncestor(enr, com.regnosys.rosetta.ast.functions.RFunction.class).orElse(null);
        if (fn == null) return null;
        String head = enr.enumName();
        if (head == null || head.isEmpty()) return null;
        // PR #444 dispatch-base inheritance: a DISPATCH VARIANT's body reads the
        // BASE declaration's inputs (upstream's function symbol scope is built
        // from the base-aware RosettaFunctionExtensions.getInputs — vendored
        // RosettaScopeProvider.getSymbolParentScope:394-400; the base link =
        // RFunction.dispatchBase(), same-file first-in-document-order
        // operations-empty). Upstream's getInputs REPLACES: when a base
        // exists, the variant's own input declarations are IGNORED, not
        // merged (the Seat-1 #444 MF-3 semantics; zero corpus carriers
        // declare variant-own inputs). Both matching modes (param NAME + the
        // unique implicit lowercase-type-name convention) apply against
        // whichever list upstream would scope.
        java.util.Optional<com.regnosys.rosetta.ast.functions.RFunction> base =
            fn.dispatchBase();
        if (base.isPresent()) return inputByHead(base.get(), head);
        return inputByHead(fn, head);
    }

    /**
     * The two input-matching modes of {@link #enclosingFunctionInput}, against
     * one function's declared inputs: exact param-NAME match first, then the
     * implicit lowercase-input-type-name convention. The implicit match must be
     * UNIQUE: if two inputs' type simple-names lower-case to the same head an
     * arbitrary first-match could silently mis-type the nav, so return null and
     * leave the reference unresolved (a safe no-op — the param-NAME loop above
     * is already unambiguous). (Copilot R1 on PR #279.)
     */
    private static RAttribute inputByHead(
            com.regnosys.rosetta.ast.functions.RFunction fn, String head) {
        for (RAttribute in : fn.inputs()) {
            if (head.equals(in.name())) return in;
        }
        RAttribute implicitMatch = null;
        for (RAttribute in : fn.inputs()) {
            com.regnosys.rosetta.ast.supporting.RTypeCall tc = in.typeCall();
            if (tc == null || tc.typeName() == null) continue;
            String simple = tc.typeName();
            int dot = simple.lastIndexOf('.');
            if (dot >= 0) simple = simple.substring(dot + 1);
            if (!simple.isEmpty()
                    && (Character.toLowerCase(simple.charAt(0)) + simple.substring(1)).equals(head)) {
                if (implicitMatch != null) return null; // ambiguous — leave unresolved
                implicitMatch = in;
            }
        }
        return implicitMatch;
    }

    /**
     * PR #443 #4-class-(a): the enclosing FUNCTION's shortcut (alias) whose name
     * matches the disguised chain's leading name — the alias analogue of
     * {@link #enclosingFunctionInput}. Null when the node is not inside a
     * function or no shortcut matches (upstream enforces function-scope name
     * uniqueness, so at most one can).
     */
    private static com.regnosys.rosetta.ast.functions.RShortcut enclosingFunctionShortcut(
            REnumValueRef enr) {
        com.regnosys.rosetta.ast.functions.RFunction fn =
            AstWalker.findAncestor(enr, com.regnosys.rosetta.ast.functions.RFunction.class).orElse(null);
        if (fn == null) return null;
        String head = enr.enumName();
        if (head == null || head.isEmpty()) return null;
        for (com.regnosys.rosetta.ast.functions.RShortcut sc : fn.shortcuts()) {
            if (head.equals(sc.name())) return sc;
        }
        return null;
    }

    /**
     * PR #443 #4-class-(a): the DATA TYPE declaring the condition this node sits
     * in — the condition body's implicit-item context (the same declaring-type
     * scope {@code LexicalResolutionPass.buildTypeConditionScope} gives bare
     * symbol references). Null when the node is not inside a data-type
     * condition (a FUNCTION's pre/post condition has no RDataType ancestor, so
     * it correctly falls through to the input/alias channels).
     */
    private static com.regnosys.rosetta.ast.types.RDataType enclosingConditionDataType(
            REnumValueRef enr) {
        com.regnosys.rosetta.ast.functions.RCondition condition =
            AstWalker.findAncestor(enr, com.regnosys.rosetta.ast.functions.RCondition.class).orElse(null);
        if (condition == null) return null;
        return AstWalker.findAncestor(condition, com.regnosys.rosetta.ast.types.RDataType.class)
            .orElse(null);
    }

    /**
     * The {@link com.regnosys.rosetta.ast.types.REnumeration} that an
     * {@link RSwitchExpr}'s case results should resolve against — the enclosing
     * function's OUTPUT enum — but ONLY when the switch is the DIRECT body of
     * that output's assignment; else {@code null} (Category 15 helper).
     *
     * <p>HARD gate (soundness): the switch's immediate parent must be an
     * {@link ROperation} whose grandparent is an
     * {@link com.regnosys.rosetta.ast.functions.RFunction} and whose
     * {@code targetName} equals the function output attribute name. This excludes
     * a switch nested in a shortcut / condition / chained-then / lambda — all of
     * which are also reachable from the same RFunction by a generic parent walk
     * but do NOT produce the function output, so binding their bare case results
     * against the output enum would be unsound. Returns {@code null} (a safe
     * no-op) for non-enum outputs, rule/report-origin functions whose output
     * typeCall is back-filled at codegen ({@code inferAttributeRefType} MISSING),
     * and any non-direct-output switch. Mirrors {@link #enumOfBranch}'s
     * REnumTypeRef extraction + the L617-627 function-output idiom.
     */
    private com.regnosys.rosetta.ast.types.REnumeration enclosingFunctionOutputEnumForDirectSwitch(
            RSwitchExpr sw) {
        if (!(sw.parent() instanceof ROperation op)) return null;
        if (!(op.parent() instanceof com.regnosys.rosetta.ast.functions.RFunction fn)) return null;
        java.util.Optional<RAttribute> output = fn.output();
        if (output.isEmpty()) return null;
        if (!output.get().name().equals(op.targetName())) return null;
        RMetaAnnotatedType outputType = resolver.inferAttributeRefType(output.get());
        if (outputType.isMissing()) return null;
        if (outputType.type() instanceof com.regnosys.rosetta.types.REnumTypeRef enumRef) {
            return enumRef.astNode();
        }
        return null;
    }

    /**
     * The {@link com.regnosys.rosetta.ast.types.REnumeration} that a function-body
     * expression's bare enum-value names should resolve against — the enclosing
     * function's OUTPUT enum — but ONLY when the expression is the DIRECT body of
     * that output's assignment; else {@code null}. Shared by Category 13b (the
     * {@code RConditionalExpr} result-branch shape) and Category 13c (the
     * {@code RListLiteral} element shape — facet void_witness_bare_enum).
     *
     * <p>The exact sibling of {@link #enclosingFunctionOutputEnumForDirectSwitch}:
     * {@link #seedElseArmsFromThen} can only seed an else-arm from a then-arm
     * that ALREADY types as an enum, so an all-bare conditional result chain
     * (DRR {@code ClearingExceptionsAndExemptions}
     * {@code set result: if .. then ENDU else if .. then AFFL ..}) self-seeds
     * nothing — and a list literal of all-bare names (DRR
     * {@code SupervisoryBodyForCSA} {@code add supervisoryBodyCSA: [CA_AB_ASC, ..]})
     * has NO branch to seed from at all. This supplies the expected type from the
     * output declaration instead, mirroring upstream's expected-type scoping
     * ({@code ExpectedTypeProvider} flows the container's type through a list
     * literal to its elements; {@code RosettaScopeProvider} adds the expected
     * enum's values as implicit features).
     *
     * <p>HARD gate (soundness, mirrors the switch helper): the expression's
     * immediate parent must be an {@link ROperation} whose grandparent is an
     * {@link com.regnosys.rosetta.ast.functions.RFunction} and whose
     * {@code targetName} equals the function output attribute name. This excludes an
     * expression nested in a shortcut / condition / lambda — all also reachable from
     * the same RFunction by a generic parent walk but NOT the function output, so
     * binding their bare names against the output enum would be unsound. Returns
     * {@code null} (a safe no-op) for non-enum outputs and rule/report-origin outputs
     * whose typeCall is back-filled at codegen ({@code inferAttributeRefType}
     * MISSING).
     */
    private com.regnosys.rosetta.ast.types.REnumeration enclosingFunctionOutputEnumForDirectBody(
            RExpression body) {
        if (!(body.parent() instanceof ROperation op)) return null;
        if (!(op.parent() instanceof com.regnosys.rosetta.ast.functions.RFunction fn)) return null;
        java.util.Optional<RAttribute> output = fn.output();
        if (output.isEmpty()) return null;
        if (!output.get().name().equals(op.targetName())) return null;
        RMetaAnnotatedType outputType = resolver.inferAttributeRefType(output.get());
        if (outputType.isMissing()) return null;
        if (outputType.type() instanceof com.regnosys.rosetta.types.REnumTypeRef enumRef) {
            return enumRef.astNode();
        }
        return null;
    }

    /**
     * Category 13d helper (facet {@code ctorSetterEnum}, PR #202): the
     * {@link com.regnosys.rosetta.ast.types.REnumeration} a bare conditional
     * value should bind to when it is the DIRECT value of a constructor-setter
     * pair ({@code <Type> { <attr>: if .. then BARE_ENUM }}). The expression's
     * immediate parent must be an
     * {@link com.regnosys.rosetta.ast.expressions.supporting.RKeyValuePair} whose
     * parent is an {@link com.regnosys.rosetta.ast.expressions.constructors.RConstructorExpr};
     * the constructor's (iteratively-cached) type supplies the data type, the pair
     * key the attribute, and the attribute its enum type — mirroring the hard
     * direct-body gate of {@link #enclosingFunctionOutputEnumForDirectBody} (so a
     * bare name nested deeper is never bound). Returns {@code null} (safe no-op)
     * for a non-enum attribute or any resolution miss.
     */
    private com.regnosys.rosetta.ast.types.REnumeration enclosingCtorAttributeEnumForDirectValue(
            RExpression body) {
        if (!(body.parent() instanceof com.regnosys.rosetta.ast.expressions.supporting.RKeyValuePair pair)) {
            return null;
        }
        if (!(pair.parent() instanceof com.regnosys.rosetta.ast.expressions.constructors.RConstructorExpr ctor)) {
            return null;
        }
        if (pair.key() == null) {
            return null;
        }
        RMetaAnnotatedType ctorType = getInferredType(ctor);
        if (ctorType.isMissing()) {
            return null;
        }
        java.util.Optional<RAttribute> attr = resolver.findAttributeOnType(ctorType.type(), pair.key());
        if (attr.isEmpty()) {
            return null;
        }
        RMetaAnnotatedType attrType = resolver.inferAttributeRefType(attr.get());
        if (!attrType.isMissing()
                && attrType.type() instanceof com.regnosys.rosetta.types.REnumTypeRef enumRef) {
            return enumRef.astNode();
        }
        return null;
    }

    // === Category 16 (PR #448) helpers =======================================

    /**
     * Depth budget for {@link #expectedEnumViaPosition}'s container recursion —
     * a runaway backstop (the deepest corpus chain is ~8 hops: nested
     * conditionals through extract/then lambda bodies to the assign root);
     * mirrors the {@code MAX_ITERATIONS} idiom.
     */
    private static final int MAX_EXPECTED_TYPE_DEPTH = 40;

    /**
     * The {@link com.regnosys.rosetta.ast.types.REnumeration} the POSITION of
     * {@code expr} expects, or {@code null} — the fork's port of upstream's
     * {@code ExpectedTypeProvider.getExpectedTypeFromContainer} recursion,
     * pruned to the enum-producing paths (a position whose upstream expected
     * type is a builtin — a boolean guard, a filter body, a to-* argument, an
     * arithmetic operand — returns null by omission, and the {@code =}/{@code
     * <>} operand seat stays Cat 14's). Consumed by the Cat 16 arm through the
     * {@link #bindBareEnumValue} worker (BIND since PR #452 — the #448
     * clearing-only mode's recorded typed follow-on).
     */
    /**
     * The enum expected at this expression's position, or {@code null} when the
     * position does not fix one. Exposed for {@link ExpressionTypeComputer},
     * which needs it to type a BARE enum value the way upstream does — by the
     * expected type at the seat rather than by the enum that happens to declare
     * the value.
     */
    com.regnosys.rosetta.ast.types.REnumeration expectedEnumAt(RExpression expr) {
        return expectedEnumViaPosition(expr, 0);
    }

    private com.regnosys.rosetta.ast.types.REnumeration expectedEnumViaPosition(
            RExpression expr, int depth) {
        if (depth > MAX_EXPECTED_TYPE_DEPTH) {
            return null;
        }
        RNode parent = expr.parent();
        if (parent == null) {
            return null;
        }
        // constructor pair value — `T { attr: <expr> }` (upstream
        // CONSTRUCTOR_KEY_VALUE_PAIR__VALUE → the key attribute's type; the
        // 13d seat, generalized beyond conditional values)
        if (parent instanceof com.regnosys.rosetta.ast.expressions.supporting.RKeyValuePair) {
            return enclosingCtorAttributeEnumForDirectValue(expr);
        }
        // function-call argument — `Foo(.., <expr>, ..)` (upstream
        // caseCallableReference → the callee's declared input at the
        // argument's index; a dispatch variant's empty input list falls back
        // to the #444 dispatch base)
        if (parent instanceof RSymbolReference call) {
            int idx = call.args().indexOf(expr);
            if (idx < 0 || call.symbol().isEmpty()
                    || !(call.symbol().get()
                            instanceof com.regnosys.rosetta.ast.functions.RFunction fn)) {
                return null;
            }
            List<RAttribute> inputs = fn.inputs().isEmpty()
                    ? fn.dispatchBase()
                        .map(com.regnosys.rosetta.ast.functions.RFunction::inputs)
                        .orElse(fn.inputs())
                    : fn.inputs();
            return idx < inputs.size() ? enumOfAttribute(inputs.get(idx)) : null;
        }
        // conditional arms — DIRECTIONAL, upstream's exact rule
        // (ExpectedTypeProvider.caseConditionalExpression, verified identical
        // to the 9.83.0 tag): IFTHEN gets the container's expected type ONLY;
        // ELSETHEN gets container ?: typeof(ifthen). The fallback chains per
        // level through nested else-ifs — a nested conditional in an else
        // seat inherits typeof(the outer then-arm) as ITS container, feeding
        // BOTH its arms — and it is one-way: a bare THEN arm with no
        // container expectation does not resolve upstream (probe P4, the
        // committed refusal pin; P6 pins the positive). The IF guard is
        // boolean — pruned. This fallback is the corpus-11 mechanism: the
        // hkma no-output rules' bare arms typed by their declaring enum
        // instead produced the paired no-common-supertype TYPE_ERRORs
        // (v3.1 C1 part 2, the cascade-breaker).
        if (parent instanceof com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr cond) {
            if (cond.thenBranch() == expr) {
                return expectedEnumViaPosition(cond, depth + 1);
            }
            if (cond.elseBranch().filter(b -> b == expr).isPresent()) {
                com.regnosys.rosetta.ast.types.REnumeration container =
                    expectedEnumViaPosition(cond, depth + 1);
                if (container != null) {
                    return container;
                }
                return enumOfBranch(cond.thenBranch());
            }
            return null;
        }
        // the binary RIGHT-operand family — expected(RHS) = typeof(LHS), the
        // same rule as the `default` arm below (upstream caseEqualsOperation /
        // caseNotEqualsOperation / caseContainsOperation /
        // caseDisjointOperation, all RIGHT-only). The bare-LHS mirror is a
        // recorded deliberate divergence (SF-2), NOT implemented here. The
        // elided-subject form (`then if = Other ...`) has no LHS node in the
        // fork's AST where upstream's grammar fills the implicit variable, so
        // typeof(LHS) is read off the enclosing item type instead — the same
        // fact by the fork's channel for it.
        if (parent instanceof com.regnosys.rosetta.ast.expressions.binary.REqualityExpr eq
                && eq.rawRight() == expr) {
            return enumOfLeftOrImplicitSubject(eq.rawLeft(), eq);
        }
        if (parent instanceof com.regnosys.rosetta.ast.expressions.binary.RContainsExpr ct
                && ct.rawRight() == expr) {
            return enumOfLeftOrImplicitSubject(ct.rawLeft(), ct);
        }
        if (parent instanceof com.regnosys.rosetta.ast.expressions.binary.RDisjointExpr dj
                && dj.rawRight() == expr) {
            return enumOfLeftOrImplicitSubject(dj.rawLeft(), dj);
        }
        // operation body — `set <root>[ -> path]: <expr>` (upstream
        // OPERATION__EXPRESSION → the assign root's type or the LAST path
        // segment's feature; the 13b seat, generalized to paths and aliases)
        if (parent instanceof ROperation op && op.expression() == expr) {
            return assignTargetEnum(op);
        }
        // list-literal elements flow the literal's expected type (upstream
        // caseListLiteral; the 13c seat, generalized past direct bodies)
        if (parent instanceof com.regnosys.rosetta.ast.expressions.literals.RListLiteral) {
            return expectedEnumViaPosition((RExpression) parent, depth + 1);
        }
        // switch-case results flow the switch's expected type (upstream
        // SwitchCaseOrDefault; the Cat 15 seat, generalized past direct
        // bodies — case GUARDS stay Cat 4's argument-typed seat)
        if (parent instanceof RSwitchCase sc && sc.expression() == expr
                && sc.parent() instanceof RSwitchExpr sw) {
            return expectedEnumViaPosition(sw, depth + 1);
        }
        // `default` RHS — the LEFT operand's ACTUAL type (upstream
        // caseDefaultOperation), reading the ENCLOSING ITEM TYPE where the
        // subject is elided (`Conv(x) then default ADHO` — the drr residue's
        // third mechanism, same helper as the equality/contains arms below;
        // v3.1 C1 part 2, pinned by DrrResidueResolutionTest).
        if (parent instanceof com.regnosys.rosetta.ast.expressions.binary.RDefaultExpr df
                && df.rawRight() == expr) {
            return enumOfLeftOrImplicitSubject(df.rawLeft(), df);
        }
        // item-type-transparent unaries flow their container's expected type
        // (upstream's leavesItemTypeUnchanged family — SUM changes the type
        // and is pruned by the explicit list)
        if (parent instanceof RListOpExpr lop && lop.argument() == expr) {
            com.regnosys.rosetta.ast.enums.ListOp o = lop.op();
            boolean transparent = o == com.regnosys.rosetta.ast.enums.ListOp.ONLY_ELEMENT
                    || o == com.regnosys.rosetta.ast.enums.ListOp.FLATTEN
                    || o == com.regnosys.rosetta.ast.enums.ListOp.DISTINCT
                    || o == com.regnosys.rosetta.ast.enums.ListOp.REVERSE
                    || o == com.regnosys.rosetta.ast.enums.ListOp.FIRST
                    || o == com.regnosys.rosetta.ast.enums.ListOp.LAST;
            return transparent ? expectedEnumViaPosition(lop, depth + 1) : null;
        }
        if (parent instanceof RFilterExpr fil && fil.argument() == expr) {
            return expectedEnumViaPosition(fil, depth + 1);
        }
        if (parent instanceof RSortExpr sort && sort.argument() == expr) {
            return expectedEnumViaPosition(sort, depth + 1);
        }
        if (parent instanceof RMaxExpr max && max.argument() == expr) {
            return expectedEnumViaPosition(max, depth + 1);
        }
        if (parent instanceof RMinExpr min && min.argument() == expr) {
            return expectedEnumViaPosition(min, depth + 1);
        }
        // map / then / reduce lambda BODIES flow the operation's expected type
        // (upstream INLINE_FUNCTION__BODY; filter / comparing bodies are
        // boolean — pruned)
        if (parent instanceof RInlineFunction inline && inline.body() == expr) {
            RNode op = inline.parent();
            if (op instanceof RExtractExpr || op instanceof RThenExpr
                    || op instanceof RReduceExpr) {
                return expectedEnumViaPosition((RExpression) op, depth + 1);
            }
            return null;
        }
        return null;
    }

    /**
     * The enum the assignment TARGET of {@code op} expects: with a path — the
     * LAST segment's resolved attribute; without — the assign root, which is
     * the enclosing function's output attribute (own, or the #444 dispatch
     * base's) or one of its aliases (upstream OPERATION__EXPRESSION →
     * {@code getRTypeOfSymbol(assignRoot)} / the last Segment's feature).
     */
    private com.regnosys.rosetta.ast.types.REnumeration assignTargetEnum(ROperation op) {
        if (op.segment().isPresent()) {
            RSegment last = op.segment().get();
            while (last.next().isPresent()) {
                last = last.next().get();
            }
            return last.resolvedAttribute().map(this::enumOfAttribute).orElse(null);
        }
        if (!(op.parent() instanceof com.regnosys.rosetta.ast.functions.RFunction fn)) {
            return null;
        }
        java.util.Optional<RAttribute> output = fn.output()
                .or(() -> fn.dispatchBase()
                        .flatMap(com.regnosys.rosetta.ast.functions.RFunction::output));
        if (output.isPresent() && output.get().name().equals(op.targetName())) {
            return enumOfAttribute(output.get());
        }
        for (com.regnosys.rosetta.ast.functions.RShortcut sc : fn.shortcuts()) {
            if (sc.name().equals(op.targetName())) {
                return enumOfBranch(sc.expression());
            }
        }
        return null;
    }

    /** The REnumeration {@code att}'s declared type resolves to, or {@code null}. */
    private com.regnosys.rosetta.ast.types.REnumeration enumOfAttribute(RAttribute att) {
        RMetaAnnotatedType t = resolver.inferAttributeRefType(att);
        if (!t.isMissing()
                && t.type() instanceof com.regnosys.rosetta.types.REnumTypeRef enumRef) {
            return enumRef.astNode();
        }
        return null;
    }

    /**
     * True when an enclosing closure declares {@code ref}'s name as an explicit
     * param — upstream's parent (lexical) scope contains closure params, and
     * ReversedSimpleScope's parent-wins rule keeps them ahead of the expected
     * enum's values, so Cat 16 must never capture such a name.
     */
    private boolean closureDeclaresParamName(RSymbolReference ref) {
        for (RNode anc = ref.parent(); anc != null; anc = anc.parent()) {
            if (anc instanceof RInlineFunction inline
                    && inline.paramNames().contains(ref.name())) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when {@code ref} sits inside an implicit inline body whose item type
     * is still MISSING — the implicit-item feature lookup (Cat 9; upstream's
     * implicit features outrank the expected enum's values inside the local
     * scope block) has not conclusively declined yet, so Cat 16 defers to a
     * later fixed-point iteration.
     */
    private boolean untypedImplicitItemContext(RSymbolReference ref) {
        for (RNode anc = ref.parent(); anc != null; anc = anc.parent()) {
            if (anc instanceof RInlineFunction) {
                return getEnclosingItemType(ref).isMissing();
            }
        }
        return false;
    }

    // === Category 13 (Phase X1 T0n / Gap E) helpers ==========================

    /**
     * PR #458 Arm A — is the bare unresolved name a META feature of the
     * implicit item? Upstream's {@code findFeaturesOfImplicitVariable}
     * returns the item's features INCLUDING the meta-annotated wrapper's
     * meta fields, so a bare {@code scheme} over a {@code [metadata scheme]}
     * stream resolves silently. The fork's item TYPE does not carry meta
     * (the {@code RMetaAnnotatedType} meta list is seeded only by joins), so
     * the arm mirrors the truth condition on the syntax the fork has: the
     * element-PRODUCING attribute of the lambda's source chain carries a
     * {@code [metadata <name>]} annotation. The qualifier name IS the feature
     * name for the corpus channel (scheme/reference); wider upstream wrapper
     * aliases (externalReference etc.) are corpus-empty at this seat and
     * recorded as a Seat-1 OBS.
     */
    private boolean implicitItemMetaFeature(RSymbolReference ref) {
        RInlineFunction lambda = com.regnosys.rosetta.ast.util.AstWalker
                .findAncestor(ref, RInlineFunction.class).orElse(null);
        if (lambda == null || !lambda.isImplicit()) {
            return false;
        }
        RAttribute leaf = elementProducingAttribute(chainSourceOf(lambda.parent()));
        return leaf != null && carriesMetadataFeature(leaf, ref.name());
    }

    /** The stream a chain operation's per-element lambda iterates. */
    private static RExpression chainSourceOf(RNode op) {
        return switch (op) {
            case com.regnosys.rosetta.ast.expressions.unary.RFilterExpr f -> f.argument();
            case com.regnosys.rosetta.ast.expressions.unary.RExtractExpr e -> e.argument();
            case com.regnosys.rosetta.ast.expressions.binary.RThenExpr t -> t.argument();
            case null, default -> null;
        };
    }

    /**
     * The attribute whose values the chain's elements ARE: navigation leaves
     * resolve to it directly; per-element mapping ops ({@code then} /
     * {@code extract}) delegate to their BODY; element-preserving ops
     * ({@code filter}, {@code distinct}/{@code first}/{@code last}/
     * {@code reverse}/{@code flatten}/{@code only-element}, {@code sort}/
     * {@code min}/{@code max}) delegate to their receiver. Scalar-producing
     * ops ({@code sum}, {@code count}, ...) end the walk — their result drops
     * the wrapper's meta upstream too.
     */
    private RAttribute elementProducingAttribute(RExpression expr) {
        return switch (expr) {
            case null -> null;
            case com.regnosys.rosetta.ast.expressions.references.RFeatureCall fc ->
                    fc.resolvedFeature().orElse(null);
            // The disguised-nav form: `party -> identifier` parses as
            // REnumValueRef(enumName, valueName) — the #446–#452 grammar
            // ambiguity. Cat 10 binds the nav as an AttributeChain whose
            // FEATURE is the leaf attribute; the lexical-head typing bind
            // (resolvedInputFeature) carries the same leaf for the
            // function-input head shape.
            case com.regnosys.rosetta.ast.expressions.references.REnumValueRef enr ->
                    enr.resolvedAttributeChain()
                            .map(com.regnosys.rosetta.ast.expressions.references.REnumValueRef.AttributeChain::feature)
                            .or(enr::resolvedInputFeature)
                            .orElse(null);
            // The elided-operand form: `then filter P` parses as
            // THEN(source, lambda{ FILTER(<synthetic implicit item>, P) }) —
            // the synthetic implicit variable stands for the ENCLOSING
            // lambda's element, so delegate to that lambda's own chain source.
            case com.regnosys.rosetta.ast.expressions.references.RImplicitVariable iv ->
                    elementProducingAttribute(chainSourceOf(
                            com.regnosys.rosetta.ast.util.AstWalker
                                    .findAncestor(iv, RInlineFunction.class)
                                    .map(RNode::parent).orElse(null)));
            case com.regnosys.rosetta.ast.expressions.binary.RThenExpr then ->
                    elementProducingAttribute(then.body().map(RInlineFunction::body).orElse(null));
            case com.regnosys.rosetta.ast.expressions.unary.RExtractExpr ex ->
                    elementProducingAttribute(ex.body() != null ? ex.body().body() : null);
            case com.regnosys.rosetta.ast.expressions.unary.RFilterExpr f ->
                    elementProducingAttribute(f.argument());
            case com.regnosys.rosetta.ast.expressions.unary.RListOpExpr lo ->
                    switch (lo.op()) {
                        case DISTINCT, FIRST, LAST, REVERSE, FLATTEN, ONLY_ELEMENT ->
                                elementProducingAttribute(lo.argument());
                        default -> null;
                    };
            case com.regnosys.rosetta.ast.expressions.unary.RSortExpr s ->
                    elementProducingAttribute(s.argument());
            case com.regnosys.rosetta.ast.expressions.unary.RMinExpr mn ->
                    elementProducingAttribute(mn.argument());
            case com.regnosys.rosetta.ast.expressions.unary.RMaxExpr mx ->
                    elementProducingAttribute(mx.argument());
            case RSymbolReference sr ->
                    sr.symbol().orElse(null) instanceof RAttribute a ? a : null;
            default -> null;
        };
    }

    /** {@code [metadata <name>]} on the attribute — the wrapper meta feature. */
    private static boolean carriesMetadataFeature(RAttribute attr, String name) {
        for (var annotationRef : attr.annotationRefs()) {
            if ("metadata".equals(annotationRef.annotationName())
                    && name.equals(annotationRef.qualifierName().orElse(null))) {
                return true;
            }
        }
        return false;
    }

    /**
     * PR #458 Arm B — is the bare unresolved name a VALUE of the enumeration
     * its equality SIBLING types to? Upstream's expected-type channel
     * (vendored RosettaScopeProvider :204-208) concatenates the expected
     * enum's {@code getAllEnumValues} into every symbol-reference scope; for
     * an equality operand the expected type is the OTHER side's — and it flows
     * into the RIGHT operand ONLY ({@code caseEqualsOperation} handles the
     * RIGHT containment), so this clearing arm is DIRECTIONAL too (v3.1 C1
     * part 2, SF-2): a bare value on the LEFT keeps its diagnostic, exactly as
     * upstream refuses it (probe P5 — the symmetric form cleared the
     * diagnostic without binding, leaving an unresolved node with no report;
     * the drr carrier {@code qualification = confirmationDateTime} has the
     * bare value on the RIGHT and is unaffected). The sibling's
     * enum is read from its inferred type when available, else through the
     * implicit-item attribute walk (the drr carrier's sibling is itself a
     * bare item feature that pass 5 mis-bound to a same-named annotation —
     * the #204 class — so its inferred type is not the enum; the item walk
     * recovers the REAL attribute exactly as the render's re-root synthesis
     * does). Value names include the super-enum chain (upstream
     * {@code getAllEnumValues}), cycle-guarded.
     */
    private boolean equalitySiblingEnumValue(RSymbolReference ref) {
        if (!(ref.parent() instanceof com.regnosys.rosetta.ast.expressions.binary.REqualityExpr eq)) {
            return false;
        }
        if (ref != eq.right().orElse(null)) {
            return false;
        }
        RExpression sibling = eq.left().orElse(null);
        if (sibling == null) {
            return false;
        }
        com.regnosys.rosetta.ast.types.REnumeration en = siblingEnum(sibling);
        return en != null && allEnumValueNames(en).contains(ref.name());
    }

    private com.regnosys.rosetta.ast.types.REnumeration siblingEnum(RExpression sibling) {
        RMetaAnnotatedType t = getInferredType(sibling);
        if (!t.isMissing() && t.type() instanceof com.regnosys.rosetta.types.REnumTypeRef enumRef) {
            return enumRef.astNode();
        }
        RAttribute attr = null;
        if (sibling instanceof RSymbolReference ss && ss.args().isEmpty()) {
            if (ss.symbol().orElse(null) instanceof RAttribute bound) {
                attr = bound;
            } else {
                attr = resolver.resolveImplicitItemFeatureCall(ss, this::getEnclosingItemType)
                        .orElse(null);
            }
        } else if (sibling instanceof com.regnosys.rosetta.ast.expressions.references.RFeatureCall fc) {
            attr = fc.resolvedFeature().orElse(null);
        } else if (sibling instanceof com.regnosys.rosetta.ast.expressions.references.REnumValueRef enr) {
            // The disguised-nav sibling (`a -> b = BARE`): the Cat-10 chain's
            // leaf / the lexical-head typing bind carries the attribute.
            attr = enr.resolvedAttributeChain()
                    .map(com.regnosys.rosetta.ast.expressions.references.REnumValueRef.AttributeChain::feature)
                    .or(enr::resolvedInputFeature)
                    .orElse(null);
        }
        if (attr != null && attr.typeCall() != null
                && attr.typeCall().referencedType().orElse(null)
                        instanceof com.regnosys.rosetta.ast.types.REnumeration en) {
            return en;
        }
        return null;
    }

    /** Own + inherited value names (upstream {@code getAllEnumValues}), cycle-guarded. */
    private static java.util.Set<String> allEnumValueNames(
            com.regnosys.rosetta.ast.types.REnumeration en) {
        java.util.Set<String> names = new java.util.HashSet<>();
        java.util.Set<com.regnosys.rosetta.ast.types.REnumeration> seen = new java.util.HashSet<>();
        com.regnosys.rosetta.ast.types.REnumeration current = en;
        while (current != null && seen.add(current)) {
            for (var v : current.values()) {
                names.add(v.name());
            }
            current = current.superType().orElse(null);
        }
        return names;
    }

    /**
     * The DIRECTIONAL half of upstream's conditional expected-type rule, for a
     * chain with NO container expectation: each level's else-arm takes
     * typeof(that level's then-arm) as its expected enum — and where the
     * else-arm is itself a nested conditional, that enum becomes the nested
     * chain's inherited expectation, feeding BOTH its arms (upstream: expected
     * of a nested conditional = its container's expected, and ELSETHEN is
     * container-first). A level whose then-arm is untyped seeds nothing at that
     * level and recurses — its nested else-chain may still self-seed one level
     * down. The then-arm itself NEVER takes a type from its else sibling
     * (probe P4, the committed refusal pin). Replaced the pre-C1 symmetric
     * then-first-else-descend scan, whose any-leaf seeding bound exactly the
     * shape upstream refuses.
     */
    private void seedElseArmsFromThen(
            com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr cond,
            Diagnostics collector) {
        if (cond.elseBranch().isEmpty()) {
            return;
        }
        RExpression elseB = cond.elseBranch().get();
        com.regnosys.rosetta.ast.types.REnumeration thenType = enumOfBranch(cond.thenBranch());
        if (thenType != null) {
            // inherited expectation: flows into the whole else-side, both arms
            // of any nested chain (resolveEnumBranch recurses + descends the
            // structure-preserving extract wrapper).
            resolveEnumBranch(elseB, thenType, collector);
        } else if (elseB instanceof com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr nested) {
            seedElseArmsFromThen(nested, collector);
        }
    }

    /**
     * The enum a binary operation's LEFT operand types as — read off the
     * operand where one exists, and off the ENCLOSING ITEM TYPE where the
     * subject is elided ({@code carrier -> beta then if = Other ...}): the
     * fork's builder leaves the elided left {@code null} where upstream's
     * grammar fills the implicit variable, and upstream types that variable as
     * the enclosing item — the same fact, read through the fork's channel for
     * it. Serves the {@link #expectedEnumViaPosition} RIGHT-operand family
     * (equals / not-equals / contains / disjoint).
     */
    private com.regnosys.rosetta.ast.types.REnumeration enumOfLeftOrImplicitSubject(
            RExpression left, RExpression binaryNode) {
        if (left != null) {
            return enumOfBranch(left);
        }
        RMetaAnnotatedType item = getEnclosingItemType(binaryNode);
        if (!item.isMissing()
                && item.type() instanceof com.regnosys.rosetta.types.REnumTypeRef enumRef) {
            return enumRef.astNode();
        }
        return null;
    }

    /** The REnumeration {@code branch} is typed as, or {@code null} if MISSING / not an enum. */
    private com.regnosys.rosetta.ast.types.REnumeration enumOfBranch(RExpression branch) {
        if (branch == null) return null;
        RMetaAnnotatedType t = getInferredType(branch);
        if (!t.isMissing()
                && t.type() instanceof com.regnosys.rosetta.types.REnumTypeRef enumRef) {
            return enumRef.astNode();
        }
        return null;
    }

    /**
     * Bind each bare {@link RSymbolReference} leaf-branch of the chain to a value
     * of {@code enumeration} when its name matches. Recurses into nested
     * else-conditionals so the expected type flows down the whole chain
     * ({@code if .. then E->A else if .. then BARE_B else BARE_C}).
     */
    private void resolveBareEnumBranches(
            com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr cond,
            com.regnosys.rosetta.ast.types.REnumeration enumeration,
            Diagnostics collector) {
        resolveEnumBranch(cond.thenBranch(), enumeration, collector);
        cond.elseBranch().ifPresent(elseB -> resolveEnumBranch(elseB, enumeration, collector));
    }

    /**
     * Push the expected {@code enumeration} into one conditional branch,
     * descending through structure-preserving wrappers so the type reaches the
     * bare {@link RSymbolReference} leaves (CLOSURE T10 lever 7):
     * <ul>
     *   <li>a nested {@code if/then/else} chain — recurse on both branches;</li>
     *   <li>an {@code extract} map wrapper — descend into its inline-function
     *       body, whose element result type equals the wrapper's result type, so
     *       the expected enum flows in unchanged. Mirrors DRR {@code Confirmed}'s
     *       {@code else extract (if .. then ECNF ..)} and {@code OptionStyle}'s
     *       {@code (style extract if item = American then AMER ..)} — the bare
     *       leaves live inside an extract-wrapped nested conditional whose own
     *       branches are all bare, so the expected type cannot be self-seeded and
     *       must propagate from the OUTER conditional through the wrapper;</li>
     *   <li>otherwise — attempt a direct bare-enum bind (no-op unless the branch
     *       is an unresolved bare {@link RSymbolReference} naming a value of the
     *       enum or one of its {@code extends} super-enums).</li>
     * </ul>
     */
    private void resolveEnumBranch(
            RExpression branch,
            com.regnosys.rosetta.ast.types.REnumeration enumeration,
            Diagnostics collector) {
        if (branch == null) return;
        if (branch instanceof com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr nested) {
            resolveBareEnumBranches(nested, enumeration, collector);
            return;
        }
        if (branch instanceof RExtractExpr extract
                && extract.body() != null && extract.body().body() != null) {
            resolveEnumBranch(extract.body().body(), enumeration, collector);
            return;
        }
        bindBareEnumValue(branch, enumeration, collector);
    }

    /**
     * Category 14 worker: if {@code typedSibling} types as an enum E and
     * {@code bareCandidate} is an unresolved bare enum value of E, bind it.
     * Either argument may be {@code null} (operand absent) — a no-op then.
     * Delegates the match + stamp to {@link #bindBareEnumValue}, which guards
     * the unresolved-and-matching precondition, so this is safe to call for
     * both operand orderings of a comparison.
     */
    private void resolveBareEnumOperand(
            RExpression bareCandidate,
            RExpression typedSibling,
            Diagnostics collector) {
        if (bareCandidate == null || typedSibling == null) return;
        com.regnosys.rosetta.ast.types.REnumeration enumeration = enumOfBranch(typedSibling);
        if (enumeration != null) {
            bindBareEnumValue(bareCandidate, enumeration, collector);
        }
    }

    /**
     * If {@code ref} is an unresolved bare {@link RSymbolReference} whose name
     * matches a value of {@code enumeration}, resolve it to that
     * {@link com.regnosys.rosetta.ast.supporting.REnumValue} + register the
     * cross-ref + clear the stale SYMBOL_NOT_FOUND diagnostic (mirrors the
     * pass-4/5 resolution-stamp contract). Shared by Category 13 (conditional
     * branches) and Category 14 (comparison operands); named for the value it
     * binds rather than the syntactic position it was first used in.
     */
    private void bindBareEnumValue(
            RExpression branch,
            com.regnosys.rosetta.ast.types.REnumeration enumeration,
            Diagnostics collector) {
        bindBareEnumValue(branch, enumeration, collector, false);
    }

    /**
     * As {@link #bindBareEnumValue(RExpression, com.regnosys.rosetta.ast.types.REnumeration, Diagnostics)},
     * but when {@code allowTypeShadowOverride} is true the bind ALSO fires when the
     * reference already resolved to a TYPE declaration (a user data type / enum /
     * record / basic type — see {@link #isTypeSymbol}), rebinding it to the enum
     * value. This is needed for switch-case RESULTS (Category 15): a bare result
     * whose name collides with an in-scope symbol that is NOT the output enum's
     * value is mis-bound by GlobalResolutionPass, so the strict
     * {@code symbol().isEmpty()} guard would skip it and the generator would emit
     * the wrong form. Two collision shapes occur in the corpus:
     * <ul>
     *   <li>a TYPE shadow — {@code then Commodity} where cdm has a {@code Commodity}
     *       product type (bound to that type → broken {@code MapperS.of(Commodity)}
     *       variable form); and</li>
     *   <li>a WRONG-ENUM shadow — {@code then Call} where {@code Call} is also a
     *       value of {@code PutCallEnum} (bound to {@code PutCallEnum.CALL} instead
     *       of the output {@code OptionTypeEnum.CALL}).</li>
     * </ul>
     * A type / wrong-enum value can never be a switch-case RESULT of an enum-output
     * function, so when the result names a value of the output enum E that value
     * always wins — mirroring upstream's expected-type-directed scoping and the
     * Category 9 builtin-type-shadow precedent. A result resolved to a genuine
     * VALUE reference (RAttribute / RFunction / local variable) or already to E's
     * own value is left untouched (the latter via the idempotent identity check
     * in the bind loop).
     */
    private void bindBareEnumValue(
            RExpression branch,
            com.regnosys.rosetta.ast.types.REnumeration enumeration,
            Diagnostics collector,
            boolean allowTypeShadowOverride) {
        if (!(branch instanceof RSymbolReference ref)) return;
        if (!ref.args().isEmpty()) return;
        if (ref.symbol().isPresent()) {
            RNode sym = ref.symbol().get();
            boolean overridable = allowTypeShadowOverride
                    && (isTypeSymbol(sym)
                        || sym instanceof com.regnosys.rosetta.ast.supporting.REnumValue);
            if (!overridable) {
                return;
            }
        }
        // Walk the `extends` super-enum chain: a bare value may be declared on a
        // parent enum (CLOSURE T10 lever 7 — DRR PartyIdentifierFormat2Enum
        // extends LeiIdentifierFormatEnum, where the bare `Lei` is a value of the
        // PARENT). The direct enum is the first hop (preserving prior behaviour).
        // Cycle-guarded against a malformed self-referential `extends` graph.
        Set<com.regnosys.rosetta.ast.types.REnumeration> visited = new HashSet<>();
        for (com.regnosys.rosetta.ast.types.REnumeration current = enumeration;
                current != null && visited.add(current);
                current = current.superType().orElse(null)) {
            for (com.regnosys.rosetta.ast.supporting.REnumValue val : current.values()) {
                if (val.name().equals(ref.name())) {
                    // Idempotent: already bound to E's own value (the override path
                    // re-enters here for an REnumValue symbol) — nothing to do.
                    if (ref.symbol().isPresent() && ref.symbol().get() == val) {
                        return;
                    }
                    ref.setResolvedSymbol(val);
                    if (referenceIndex != null) {
                        referenceIndex.registerReference(ref, val);
                    }
                    collector.removeMatching(
                        DiagnosticCategory.SYMBOL_NOT_FOUND, ref.sourceRange());
                    return;
                }
            }
        }
    }

    /**
     * True when {@code sym} is a TYPE declaration node — a user data type, enum,
     * builtin record, or builtin basic type. A bare reference resolved to one of
     * these sits in TYPE position; in a switch-case RESULT (Category 15) that is
     * never a valid value, so the enum-value rebind overrides it (see
     * {@link #bindBareEnumValue(RExpression, com.regnosys.rosetta.ast.types.REnumeration, Diagnostics, boolean)}).
     * A VALUE symbol (RAttribute / RFunction / local variable / REnumValue) is
     * NOT a type symbol and is never overridden.
     */
    private static boolean isTypeSymbol(RNode sym) {
        return sym instanceof com.regnosys.rosetta.ast.types.RDataType
            || sym instanceof com.regnosys.rosetta.ast.types.REnumeration
            || sym instanceof com.regnosys.rosetta.ast.types.RChoice
            || sym instanceof com.regnosys.rosetta.ast.types.RRecordType
            || sym instanceof com.regnosys.rosetta.ast.types.RBasicType;
    }

    /**
     * True when {@code iv} is the synthetic implicit-input materialised as the
     * elided operand of a without-left list/extract/filter/conversion/toString
     * operation — i.e. {@code iv} is the {@code argument} field of its parent. Such
     * implicit variables refer to the enclosing rule's implicit input (top-level)
     * or the outer chain step's element type (nested chain), NOT the
     * enclosing inline body's {@code item}. {@linkplain #computeImplicitItemType
     * Category 8} still applies — it just has a dedicated elided-operand branch
     * that routes to {@code rule.fromType} / outer chain's argument-element
     * type instead of the standard "enclosing implicit-body argument" path.
     */
    private static boolean isElidedOperandImplicitVariable(RImplicitVariable iv) {
        // Tighten the structural check by requiring iv.isSynthetic() — a
        // user-written literal `item` keyword used as an explicit receiver
        // (e.g. `item only-element` inside a lambda body) also occupies its
        // parent op's argument slot, so structural detection alone would
        // misroute the literal through this elided-branch (Copilot R4
        // — see RImplicitVariable.isSynthetic javadoc).
        if (!iv.isSynthetic()) {
            return false;
        }
        RNode p = iv.parent();
        if (p instanceof RListOpExpr listOp) {
            return listOp.argument() == iv;
        }
        if (p instanceof RConversionExpr conv) {
            return conv.argument() == iv;
        }
        if (p instanceof RToStringExpr ts) {
            return ts.argument() == iv;
        }
        if (p instanceof RExtractExpr ext) {
            return ext.argument() == iv;
        }
        if (p instanceof RFilterExpr filt) {
            return filt.argument() == iv;
        }
        // Engine PR #2 Bucket A — extend to the 5 residual without-left visitors
        // (count / sort / min / max / reduce). AstBuilder now synthesises the
        // implicit-input operand for these forms; the predicate must recognise
        // them too so the Cat 8 elided-branch types the synthetic correctly.
        if (p instanceof RCountExpr count) {
            return count.argument() == iv;
        }
        if (p instanceof RSortExpr sort) {
            return sort.argument() == iv;
        }
        if (p instanceof RMinExpr min) {
            return min.argument() == iv;
        }
        if (p instanceof RMaxExpr max) {
            return max.argument() == iv;
        }
        if (p instanceof RReduceExpr reduce) {
            return reduce.argument() == iv;
        }
        return false;
    }

    /**
     * Returns the AST node of the choice option whose type name equals
     * {@code optionName}, or {@code null} if none match. Used by the CLOSURE
     * T10 choice-option navigation path: a {@code <choiceAttr> -> <OptionName>}
     * REnumValueRef narrows the choice to the named option. The option may be a
     * data type ({@link com.regnosys.rosetta.ast.types.RDataType}) or a nested
     * choice ({@link com.regnosys.rosetta.ast.types.RChoice}); both are
     * {@link com.regnosys.rosetta.ast.RRootElement}, so the resolved node is
     * stored as that common supertype and converted back to an {@code RType} by
     * {@code ExpressionTypeComputer.computeEnumValueRef} via {@code astNodeToRType}.
     */
    private static com.regnosys.rosetta.ast.RRootElement matchChoiceOption(
            com.regnosys.rosetta.types.RChoiceTypeRef choiceType, String optionName) {
        if (optionName == null) {
            return null;
        }
        for (com.regnosys.rosetta.types.RType opt : choiceType.options()) {
            if (!optionName.equals(opt.name())) {
                continue;
            }
            if (opt instanceof com.regnosys.rosetta.types.RDataTypeRef d) {
                return d.astNode();
            }
            if (opt instanceof com.regnosys.rosetta.types.RChoiceTypeRef c) {
                return c.astNode();
            }
        }
        return null;
    }

    /**
     * Returns the {@link com.regnosys.rosetta.types.RType} of the choice option
     * whose name equals {@code optionName}, or {@code null} if none match. Unlike
     * {@link #matchChoiceOption}, this preserves the option's RType (rather than
     * collapsing to the bare AST node) so the caller can re-narrow when the
     * matched option is itself a choice. Used by the CLOSURE T10 lever-10
     * option-of-option navigation (e.g. {@code Observable -> Asset} on a
     * {@code choice Underlier: Observable | Product} item, where the Observable
     * option is itself {@code choice Observable: Asset | Basket | Index}).
     */
    private static com.regnosys.rosetta.types.RType matchChoiceOptionType(
            com.regnosys.rosetta.types.RChoiceTypeRef choiceType, String optionName) {
        if (optionName == null) {
            return null;
        }
        for (com.regnosys.rosetta.types.RType opt : choiceType.options()) {
            if (optionName.equals(opt.name())) {
                return opt;
            }
        }
        return null;
    }

    private RMetaAnnotatedType getEnclosingItemType(RNode node) {
        // PR #451 — switch-case item narrowing. Upstream's implicit-variable
        // walk (ImplicitVariableUtil.findContainerDefiningImplicitVariable)
        // stops at a SwitchCaseOrDefault ancestor when the case narrows
        // (non-default + choice-option/data guard) BEFORE reaching any
        // enclosing lambda, so a bare ref / disguised nav inside
        // `fpmlProduct switch fpml.CreditDefaultSwap then ...` resolves
        // against the GUARD's type, not the outer chain item. Consulted
        // first; null falls through to the pre-#451 derivation unchanged.
        RMetaAnnotatedType narrowed = narrowedSwitchCaseItemType(node);
        if (narrowed != null) {
            return narrowed;
        }
        RInlineFunction enclosingBody = AstWalker.findAncestor(node, RInlineFunction.class).orElse(null);
        if (enclosingBody == null) {
            // No enclosing chain-op inline function — the expression sits
            // directly inside a rule body (e.g. an RConditionalExpr / bare
            // reference attached as the RRule's expression). Fall back to
            // the enclosing RRule's fromType so the rule body itself acts
            // as the implicit-input scope. Mirrors
            // computeArgumentElementType's analogous fallback at the start
            // of a rule body when the chain op's argument is null.
            RRule enclosingRule = AstWalker.findAncestor(node, RRule.class).orElse(null);
            if (enclosingRule != null) {
                return typeComputer.inferRuleFromType(enclosingRule);
            }
            return RMetaAnnotatedType.MISSING;
        }
        RNode op = enclosingBody.parent();
        return computeArgumentElementType(op);
    }

    /**
     * PR #451 — the SWITCH-CASE ITEM NARROWING walk: the fork mirror of
     * upstream {@code ImplicitVariableUtil.findContainerDefiningImplicitVariable}'s
     * {@code SwitchCaseOrDefault} arm + {@code RosettaTypeProvider
     * .safeTypeOfImplicitVariable}'s guard-type derivation. Walks the
     * ancestors of {@code node} outward and returns the NARROWED implicit
     * item type when the nearest defining container is a narrowing switch
     * case, or {@code null} when the pre-#451 derivation should decide:
     *
     * <ul>
     *   <li>an {@link RSwitchCase} ancestor entered from its result
     *       EXPRESSION (never from the guard), non-default, whose NAME
     *       guard resolved to a TYPE-LIKE node ({@link
     *       com.regnosys.rosetta.ast.types.RDataType} — a data guard, the
     *       linker's speculative store — or {@link
     *       com.regnosys.rosetta.ast.types.RChoice}; a choice-OPTION guard
     *       stores the option's resolved type node, so both guard kinds
     *       land here uniformly) → the guard's type IS the case's implicit
     *       item (upstream: the implicit variable is DEFINED by the case);</li>
     *   <li>a case whose guard resolved to an {@code REnumValue} (an enum
     *       switch), a default case, a literal guard or a still-unresolved
     *       NAME guard defines NO implicit variable — the walk CONTINUES
     *       outward, exactly like upstream's;</li>
     *   <li>an {@link RInlineFunction} or root-element boundary reached
     *       first → {@code null} (the existing lambda/rule/condition
     *       derivations own the answer).</li>
     * </ul>
     *
     * <p>Soundness gate: a data/choice guard only exists under a DATA
     * subject upstream (the guard scope is subject-kind-typed), so the
     * narrowing declines when the subject expression's inferred type is an
     * ENUM — the linker's speculative store could otherwise mis-narrow an
     * enum-switch case whose value name collides with a global type name.
     * A MISSING subject does NOT decline: the narrowing is deliberately
     * subject-blind there (the guard node alone defines the case item
     * upstream), which lets case bodies type while the subject chain is
     * itself still cascading (the tradestate implicit-subject witness).
     * Enum-typed OPTION guards store nothing (choiceOptionNodeOnType is
     * data/choice-only), so such cases keep the pre-#451 outer item — the
     * recorded faithful follow-on (upstream narrows to the option's enum).
     */
    private RMetaAnnotatedType narrowedSwitchCaseItemType(RNode node) {
        RNode prev = node;
        for (RNode a = node.parent(); a != null; a = a.parent()) {
            if (a instanceof RInlineFunction
                    || a instanceof com.regnosys.rosetta.ast.RRootElement) {
                return null;
            }
            if (a instanceof RSwitchCase sc
                    && !sc.isDefault()
                    && sc.expression() == prev
                    && sc.guard().isPresent()
                    && sc.guard().get().kind()
                        == com.regnosys.rosetta.ast.enums.SwitchGuardKind.NAME) {
                RNode resolved = sc.guard().get().resolvedGuard().orElse(null);
                if ((resolved instanceof com.regnosys.rosetta.ast.types.RDataType
                            || resolved instanceof com.regnosys.rosetta.ast.types.RChoice)
                        && !switchSubjectIsEnum(sc)) {
                    com.regnosys.rosetta.types.RType t =
                        resolver.typeOfResolvedNode(
                            (com.regnosys.rosetta.ast.RRootElement) resolved);
                    if (t != null) {
                        return RMetaAnnotatedType.withNoMeta(t);
                    }
                }
                // Non-narrowing case: walk on (upstream behaviour).
            }
            prev = a;
        }
        return null;
    }

    /** True when the case's switch SUBJECT infers to an enum (aliases unwrapped). */
    private boolean switchSubjectIsEnum(RSwitchCase sc) {
        if (!(sc.parent() instanceof RSwitchExpr sw)) {
            return false;
        }
        RMetaAnnotatedType subjectType = sw.left()
            .map(this::getInferredType)
            .orElse(RMetaAnnotatedType.MISSING);
        if (subjectType.isMissing()) {
            return false;
        }
        com.regnosys.rosetta.types.RType t = subjectType.type();
        while (t instanceof com.regnosys.rosetta.types.RAliasType alias) {
            t = alias.refersTo();
        }
        return t instanceof com.regnosys.rosetta.types.REnumTypeRef;
    }

    /**
     * Reads the argument-element type for a chain operation. Handles the
     * 7 ops that carry an {@code argument()}: extract / filter / then / reduce
     * (the implicit-supporting forms) plus sort / max / min (added IR-Lab
     * PART A 2026-06-25 — each carries the source list as {@code argument()}
     * exactly like extract/filter, so the key-lambda item's element type is
     * the argument's element type). Plus the rule-body special case where the
     * outermost op's argument is null (walk to enclosing RRule + read
     * rule.fromType().referencedType()).
     *
     * <p>Package-private (2026-07-18, the leg-C #11/#16 typing-channel heal):
     * {@link ExpressionTypeComputer#inferTypeOfNode} reads this to type a bare
     * closure-parameter reference (resolved to its RClosureParameter node since
     * v3.2 seat 8, or to its declaring RInlineFunction for a name-only parameter)
     * as the closure's argument element type — the same derivation Cat 10's
     * closure-param-as-leading-name branch uses for `param -> feature` navs.
     */
    RMetaAnnotatedType computeArgumentElementType(RNode op) {
        if (op == null) return RMetaAnnotatedType.MISSING;

        RExpression arg = null;
        if (op instanceof RExtractExpr ext)   arg = ext.argument();
        else if (op instanceof RFilterExpr flt) arg = flt.argument();
        else if (op instanceof RThenExpr then)  arg = then.argument();
        else if (op instanceof RReduceExpr red) arg = red.argument();
        // IR-Lab PART A (2026-06-25) — sort / max / min carry an argument()
        // (the source list) exactly like extract/filter, so their key-lambda
        // item type IS the argument's element type. Without these arms the
        // implicit `item` (Cat 8) and bare attribute refs (Cat 9) inside a
        // sort/max/min key body type MISSING, which the neutral IR adapter
        // reads to decline the node. This types the DIRECT
        // `<typed-list> sort/max/min [...]` case; the `xs then sort [...]`
        // form whose argument is the elided then-pipe still bottoms out via
        // the elided-IV null fallback below (the then-pipe element type is a
        // separate, still-MISSING gap). Byte-neutral for Path-1 (the
        // generator resolves these at emit time via the gm-aware nav walk).
        else if (op instanceof RSortExpr sort) arg = sort.argument();
        else if (op instanceof RMaxExpr max)   arg = max.argument();
        else if (op instanceof RMinExpr min)   arg = min.argument();
        else return RMetaAnnotatedType.MISSING;

        // Elided-operand implicit-input materialised by the AstBuilder
        // syntheticImplicitInput synthesis is semantically equivalent to a
        // null argument: the receiver IS the enclosing rule's implicit input.
        // Only the without-left chain-op visitors (visitExtractWithoutLeftExpr
        // / visitFilterWithoutLeftExpr) produce elided IVs that reach THIS
        // method's switch — see L1120-1124 above, which only accepts RExtract
        // / RFilter / RThen / RReduce. The other elided forms (without-left
        // list-op / conversion / toString visitors) produce RListOpExpr /
        // RConversionExpr / RToStringExpr which aren't chain ops handled by
        // computeArgumentElementType, so they don't reach this fallback.
        // Category 8 does also enter a type
        // for the synthetic via {@link #computeImplicitItemType}'s elided-
        // branch, but reading that here would short-circuit through this
        // method recursively (the elided branch ends up delegating back into
        // {@link #computeArgumentElementType} for nested-chain cases — circular
        // if Cat 8 hasn't converged on a stored value for this iv yet). Fall
        // through to the rule/wrapping-inline fallback below so the element
        // type is sourced directly from the structural context that the
        // synthetic is documenting (Copilot R1 F2 — clarified after the
        // mid-PR refactor moved synthetic typing from a Cat 8 skip to a
        // dedicated Cat 8 branch).
        if (arg instanceof RImplicitVariable iv && isElidedOperandImplicitVariable(iv)) {
            arg = null;
        }

        if (arg != null) {
            // Argument expression exists — read its inferred type. The
            // list-vs-singleton distinction is encoded by CardinalityComputer
            // separately; the type itself is the element type for chain ops
            // (the chain op's "argument" is the receiver that produces items).
            return getInferredType(arg);
        }

        // Argument is null. Two cases — ORDER MATTERS (Phase X1 closure T0i,
        // latent-bug fix). Previously the (b) RRule-fallback ran FIRST + always
        // shadowed (a), which made (a) effectively dead code for any chain op
        // inside a rule body — the walker returned rule.fromType for ALL
        // null-arg ops, masking the correct upstream chain element type for
        // nested chains. The reorder below restores nested-chain semantics +
        // is required for Cat 10 / Cat 11 / Cat 12 to fire inside
        // `then extract <body>` shapes that dominate the DRR corpus.
        //
        // (a) NESTED chain — the op sits inside another chain op's
        //     inline-function body (e.g. `trades extract [ extract id ]` or
        //     `trades then extract id` where the inner `extract id` has no
        //     direct left argument because it's a "without-left" prefix form
        //     inside the outer chain's body). The chain element type is
        //     determined by the OUTER chain op's argument, NOT the rule's
        //     fromType. Detect by finding an enclosing RInlineFunction that
        //     wraps the op itself (not the op's own inline body).
        // (b) OUTERMOST chain — the op sits directly at the top of a rule
        //     body (no wrapping inline function). Delegate to
        //     ExpressionTypeComputer.inferRuleFromType which covers
        //     RDataType + REnumeration via astNodeToRType plus a
        //     typeName-keyed BuiltinTypeRegistry fallback for builtin types
        //     (string/int/boolean/etc.) — widens what was previously a
        //     hardcoded `instanceof RDataType` filter (Copilot PR #76 R10 F1).
        RInlineFunction wrappingInline = AstWalker.findAncestor(op, RInlineFunction.class).orElse(null);
        if (wrappingInline != null) {
            return computeArgumentElementType(wrappingInline.parent());
        }
        RRule enclosingRule = AstWalker.findAncestor(op, RRule.class).orElse(null);
        if (enclosingRule != null) {
            return typeComputer.inferRuleFromType(enclosingRule);
        }
        return RMetaAnnotatedType.MISSING;
    }

    /** Returns the inferred type for an expression, or MISSING if not yet inferred. */
    public RMetaAnnotatedType getInferredType(RExpression expr) {
        return inferredTypes.getOrDefault(expr, RMetaAnnotatedType.MISSING);
    }

    /**
     * Resolves an attribute's declared {@code typeCall} to its
     * {@link RMetaAnnotatedType} (MISSING when unresolvable) — the same
     * channel feature-access typing reads. PR #442: exposed for the
     * attribute-override validator seat (upstream
     * {@code AttributeValidator.checkAttributeOverride} compares the
     * override's and the parent attribute's RMetaAnnotatedTypes).
     */
    public RMetaAnnotatedType getInferredAttributeType(RAttribute attr) {
        return typeComputer.inferTypeOfAttribute(attr);
    }

    /**
     * Resolves a library-function parameter's declared {@code typeCall} to its
     * {@link RMetaAnnotatedType} (MISSING when unresolvable) — the same
     * resolution channel as {@link #getInferredAttributeType}. PR #454:
     * exposed for the call-site validator seat (upstream
     * {@code ExpressionValidator.checkCallableReference}'s
     * {@code RosettaExternalFunction} arm reads {@code getRTypeOfSymbol} on
     * each {@code RosettaParameter}).
     */
    public RMetaAnnotatedType getInferredParameterType(
            com.regnosys.rosetta.ast.supporting.RParameter param) {
        return typeComputer.inferTypeOfTypeCall(param.typeCall());
    }

    /**
     * A rule's declared FROM type (MISSING when unresolvable/absent). PR #454:
     * exposed for the call-site validator seat (upstream's rule arm checks the
     * single argument against {@code getRuleInputType}; a rule with no
     * declared input yields NOTHING there — the fork's MISSING decline at the
     * validator is strictly more conservative on that corpus-empty corner).
     */
    public RMetaAnnotatedType getInferredRuleFromType(
            com.regnosys.rosetta.ast.functions.RRule rule) {
        return typeComputer.inferRuleFromType(rule);
    }

    /**
     * The implicit item's type at {@code node} — the lambda/rule/switch-case
     * walk ({@link #getEnclosingItemType}; MISSING when no context supplies
     * one). PR #454: exposed for the only-exists validator seat (upstream
     * {@code typeOfImplicitVariable} at the parentless-element arm); the
     * validator's own condition-context walk supplies the declaring-type arm
     * this walk does not cover.
     */
    public RMetaAnnotatedType getImplicitItemType(com.regnosys.rosetta.ast.RNode node) {
        return getEnclosingItemType(node);
    }

    /** Number of iterations the last run took. */
    public int iterationCount() { return iterationCount; }
}
