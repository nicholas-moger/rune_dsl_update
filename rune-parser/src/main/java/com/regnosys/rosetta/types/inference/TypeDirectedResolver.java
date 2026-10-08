package com.regnosys.rosetta.types.inference;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.annotations.RAnnotationPathSegment;
import com.regnosys.rosetta.ast.enums.SwitchGuardKind;
import com.regnosys.rosetta.ast.expressions.references.RDeepFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCaseGuard;
import com.regnosys.rosetta.ast.external.RExternalEnumValue;
import com.regnosys.rosetta.ast.external.RExternalRegularAttribute;
import com.regnosys.rosetta.ast.functions.RSegment;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RChoiceOption;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.types.*;
import com.regnosys.rosetta.types.builtin.BuiltinTypeRegistry;

import java.util.*;

/**
 * Type-directed resolution: resolves cross-references that depend on
 * knowing the receiver's type (the 7 categories deferred by M3 D1).
 *
 * <p>Spec: section 3.6 in {@code docs/specs/2026-04-08-m4-type-system-design.md}.
 */
public final class TypeDirectedResolver {

    private final BuiltinTypeRegistry builtins;
    private ResolutionListener resolutionListener;

    /**
     * Alias-unwrap depth cap — the {@code TypeAliasSolver.evaluateForward}
     * idiom (same value). A cyclic alias chain, constructible on malformed
     * input, would otherwise hang the unwrap loops / overflow the
     * {@code findAttribute} recursion (Copilot R1 on PR #446; the class swept
     * across all three alias-unwrap sites in this resolver).
     */
    private static final int MAX_ALIAS_DEPTH = 100;

    public TypeDirectedResolver(BuiltinTypeRegistry builtins) {
        this.builtins = Objects.requireNonNull(builtins);
    }

    /**
     * Bounded alias unwrap: returns the first non-alias type in the chain, or
     * the alias reached at the {@link #MAX_ALIAS_DEPTH} cap (cyclic chains) —
     * callers' {@code instanceof} arms then simply decline. Behaviour is
     * identical to the previous unbounded walks for every non-cyclic chain.
     */
    private static RType unwrapAliases(RType type) {
        RType t = type;
        int depth = 0;
        while (t instanceof RAliasType alias && depth++ < MAX_ALIAS_DEPTH) {
            t = alias.refersTo();
        }
        return t;
    }

    /**
     * Callback fired every time a resolution-stamp setter (setResolvedFeature /
     * setResolvedAttribute / setResolvedEnumValue / setResolvedGuard) completes
     * successfully inside this resolver. Used by the type engine to mirror the
     * passes 4 + 5 contract — every resolution stamp pairs with a
     * {@code ReferenceIndex.registerReference(source, target)} call so
     * {@code RWorkspace.findReferences} stays consistent with the resolutions
     * stamped onto AST nodes during pass 6.
     *
     * <p>Class-of-issue sweep on top of Copilot PR #76 R11 F1 — that finding
     * flagged Cat 9 / Cat 10 specifically; the same contract gap applied to
     * every {@code attr.ifPresent(target::setResolvedX)} call site in this
     * resolver (resolveFeatureCall / resolveDeepFeatureCall / resolveSegment /
     * resolveExternalAttribute / resolveExternalEnumValue /
     * resolveAnnotationPathSegment / resolveSwitchGuard). All 7 sites now
     * notify via {@link #notifyResolved} when they succeed.
     */
    @FunctionalInterface
    public interface ResolutionListener {
        void onResolved(RNode source, RNode target);
    }

    /**
     * Wires a {@link ResolutionListener}. Optional — if not set, the success-path
     * notifications are no-ops; resolution stamping still happens. Mirrors the
     * optional-dependency pattern used by {@code TypeInferenceEngine.setReferenceIndex}.
     */
    public void setResolutionListener(ResolutionListener listener) {
        this.resolutionListener = listener;
    }

    private void notifyResolved(RNode source, RNode target) {
        if (resolutionListener != null) resolutionListener.onResolved(source, target);
    }

    /**
     * Resolves a feature call by looking up the featureName on the receiver's
     * inferred type.
     *
     * <p><b>{@code resolvedFeatureNode} is the AUTHORITATIVE slot and is set for every
     * kind of feature this binds.</b> The legacy {@code resolvedFeature} slot is typed
     * to {@link RAttribute} and so is set ONLY when the feature is one — it structurally
     * cannot hold a choice option or a meta type.
     *
     * <p>Callers must test {@code resolvedFeatureNode} to ask "is this bound?".
     * This javadoc previously said only "sets the resolvedFeature field", and a caller
     * took it at its word: {@code TypeInferenceEngine}'s Category-1 arm gated on
     * {@code resolvedFeature().isEmpty()}, so for option and meta bindings the gate
     * never closed and every fixed-point iteration re-resolved and re-registered the
     * reference (Copilot R8 then R18, PR #566).
     */
    public void resolveFeatureCall(RFeatureCall fc, RMetaAnnotatedType receiverType) {
        if (receiverType.isMissing()) return;
        lookupFeature(receiverType.type(), fc.featureName()).ifPresent(feature -> {
            fc.setResolvedFeatureNode(feature);
            if (feature instanceof RAttribute a) {
                fc.setResolvedFeature(a);
            } else {
                // The two slots must agree. A non-attribute binding has no attribute, so
                // anything left here from an earlier resolution would contradict the
                // authoritative slot — and ~80 call sites still read this one. Unreachable
                // through the engine today (the Category-1 arm is gated on
                // resolvedFeatureNode being empty, so a bound call is never re-resolved),
                // but this method is public and does not get to assume that gate
                // (Copilot R19, PR #566).
                fc.setResolvedFeature(null);
            }
            notifyResolved(fc, feature);
        });
    }

    /**
     * The type of a resolved HEAD binding — spec R9's {@code typeOf(receiver)},
     * for the node kinds a head can legitimately be.
     *
     * <p>A choice OPTION types as the type it names: upstream reaches the same
     * answer for free, because an option there IS an attribute and an
     * attribute's type is its type call. Returns {@code null} where the head's
     * type is not computable from the node alone (an alias, whose type is its
     * body's), leaving the caller's other channels in play.
     */
    public RType typeOfHead(RNode head) {
        if (head instanceof RAttribute attribute) {
            RMetaAnnotatedType t = inferAttributeRefType(attribute);
            return t.isMissing() ? null : t.type();
        }
        if (head instanceof RChoiceOption option) {
            var typeCall = option.typeCall();
            if (typeCall == null) {
                return null;
            }
            return typeCall.referencedType()
                    .filter(com.regnosys.rosetta.ast.RRootElement.class::isInstance)
                    .map(com.regnosys.rosetta.ast.RRootElement.class::cast)
                    .map(this::typeOfResolvedNode)
                    .orElse(null);
        }
        if (head instanceof com.regnosys.rosetta.ast.RRootElement root) {
            return typeOfResolvedNode(root);
        }
        return null;
    }

    /**
     * R9 of {@code docs/specs/2026-08-15-upstream-resolution-spec.md} — THE
     * FEATURE NAMESPACE of a receiver type, as one lookup.
     *
     * <p>Upstream needs no such method because it has no such question: it
     * declares {@code Choice extends Data} with the options AS the attributes,
     * so {@code allFeaturesExcludingEnumValues} finds an option and an attribute
     * by the same walk, and {@code a -> b} has exactly one rule regardless of
     * what {@code a} is. The fork models {@link com.regnosys.rosetta.ast.types.RChoice}
     * separately from {@link RDataType} with its own option list, so the two
     * namespaces have to be rejoined here — and until they were, every hop after
     * the first in {@code Asset -> Instrument -> Security} came back unresolved,
     * because the legacy {@code resolvedFeature} slot is typed to
     * {@link RAttribute} and structurally cannot hold an option.
     *
     * <p>ORDER, and why it is not arbitrary: upstream's {@code getAllAttributes}
     * walks the supertype chain own-first, and a {@code type X extends ChoiceC}
     * puts the choice's options-as-attributes on that chain BELOW X's own — so
     * declared attributes precede inherited options.
     *
     * <p><b>A receiver that IS a choice gets its OPTIONS AND NOTHING ELSE.</b>
     * Upstream's feature scope there is {@code asRDataType()}, whose attributes
     * ARE the options; the shared attributes of every option are the DEEP path's
     * business ({@code ->>}, R11), not the plain arrow's. This method used to run
     * {@code findAttribute} first regardless — and on a choice that applies
     * {@link #findCommonAttribute}, so {@code c -> shared} bound a common
     * attribute upstream REFUSES (probe P1: upstream answers
     * {@code Couldn't resolve reference to RosettaFeature 'shared'}). A silent
     * wrong-accept, invisible on an upstream-valid corpus, and the earlier text
     * here asserted the opposite ("findAttribute finds nothing for a choice") —
     * which was the tell. Found twice independently: Fable review SF-1 and
     * Copilot R3 on PR #566.
     *
     * <p>Meta descriptions are NOT part of this lookup. Upstream concatenates
     * them onto the same feature scope, but the fork's {@code RMetaAnnotatedType}
     * does not carry the receiver's meta names, so R10 is served by a separate
     * per-file {@code metaType} channel in {@code TypeInferenceEngine} — ordered
     * AFTER this call, which preserves upstream's attributes-before-meta
     * precedence (probe P7).
     *
     * @return the option DECLARATION for a choice option — not the type it
     *         names. Upstream binds {@code ChoiceOption Instrument.Security};
     *         binding the bare type loses which choice the option came through,
     *         and that is the fact the missing-import half of E1 needs.
     */
    public Optional<RNode> lookupFeature(RType receiverType, String name) {
        if (!(unwrapAliases(receiverType) instanceof RChoiceTypeRef)) {
            Optional<RAttribute> attribute = findAttribute(receiverType, name);
            if (attribute.isPresent()) {
                return attribute.map(RNode.class::cast);
            }
        }
        RChoiceOption option = choiceOptionDeclarationOnType(receiverType, name);
        if (option != null) {
            return Optional.of(option);
        }
        return Optional.empty();
    }

    /**
     * The declaration-returning sibling of {@link #choiceOptionNodeOnType}:
     * walks the SAME admission (the receiver's choice view via
     * {@link #choiceViewOfType}, matching on the option's written type-reference
     * text) and returns the matched {@link RChoiceOption} itself rather than the
     * type it names, so the binding records WHICH choice the option belongs to.
     *
     * <p>Only the AST-backed path can answer: the IR-only fallback in its
     * sibling carries resolved option TYPES with no declaration nodes behind
     * them, so there is nothing to return there and the caller falls back.
     */
    public RChoiceOption choiceOptionDeclarationOnType(RType type, String name) {
        RType t = choiceViewOfType(type);
        if (t instanceof RChoiceTypeRef choiceRef && choiceRef.astNode() != null) {
            for (RChoiceOption option : choiceRef.astNode().options()) {
                var typeCall = option.typeCall();
                if (typeCall != null && name.equals(typeCall.typeName())) {
                    return option;
                }
            }
        }
        return null;
    }

    /**
     * Resolves a deep feature call by searching transitively through the
     * receiver type's attribute tree. Sets the resolvedFeature field.
     */
    public void resolveDeepFeatureCall(RDeepFeatureCall dfc, RMetaAnnotatedType receiverType) {
        if (receiverType.isMissing()) return;
        var attr = findAttributeDeep(receiverType.type(), dfc.featureName(), new HashSet<>());
        attr.ifPresent(a -> { dfc.setResolvedFeature(a); notifyResolved(dfc, a); });
    }

    /**
     * Resolve a bare symbol reference inside the inline function body of a
     * HasGeneratedInput chain operation (extract / filter / then / reduce) as
     * a feature call on the implicit item. Returns Optional.empty() if:
     *   - the reference isn't inside such a body;
     *   - the implicit-item type can't be computed yet (caller's fixed-point
     *     iteration will retry); or
     *   - the symbol name doesn't match an attribute on the implicit-item type.
     *
     * <p>Spec: docs/superpowers/specs/2026-05-21-phase-x1-m3-implicit-input-extension.md (local) § 4.3.
     */
    public Optional<RAttribute> resolveImplicitItemFeatureCall(
            RSymbolReference ref,
            ImplicitItemTypeProvider typeProvider) {
        RMetaAnnotatedType itemType = typeProvider.typeOf(ref);
        if (itemType.isMissing()) return Optional.empty();
        return findAttribute(itemType.type(), ref.name());
    }

    /**
     * Callback that walks the AST to find the implicit-item type for a node
     * inside an implicit inline function body. Returns RMetaAnnotatedType.MISSING
     * when the type can't be computed yet (the caller's fixed-point iteration
     * retries on a later pass).
     */
    @FunctionalInterface
    public interface ImplicitItemTypeProvider {
        RMetaAnnotatedType typeOf(RNode node);
    }

    /**
     * Resolves a segment in an operation path by looking up the name on
     * the current type context. Sets the resolvedAttribute field.
     */
    public void resolveSegment(RSegment seg, RMetaAnnotatedType contextType) {
        if (contextType.isMissing()) return;
        var attr = findAttribute(contextType.type(), seg.name());
        attr.ifPresent(a -> { seg.setResolvedAttribute(a); notifyResolved(seg, a); });
    }

    // === T17: Operation path chain resolution =================================

    /**
     * Resolves a full operation path chain (e.g., foo -> bar -> baz).
     * Walks each segment, resolving against the type of the previous segment.
     *
     * @param firstSegment the first segment in the chain
     * @param contextType the type context for the first segment
     */
    public void resolveOperationPath(RSegment firstSegment, RMetaAnnotatedType contextType) {
        if (contextType.isMissing()) return;

        RSegment current = firstSegment;
        RMetaAnnotatedType currentType = contextType;
        int safety = 0;
        while (current != null && safety++ < 100) {
            resolveSegment(current, currentType);
            if (current.resolvedAttribute().isEmpty()) break; // can't continue

            // Advance type context to the resolved attribute's type
            currentType = inferAttributeType(current.resolvedAttribute().get());
            current = current.next().orElse(null);
        }
    }

    /**
     * Looks up an attribute by name on the given type (public reuse of the
     * private {@link #findAttribute(RType, String)} helper, surfaced for
     * Category 10's REnumValueRef → attribute-chain fallback path in
     * {@link TypeInferenceEngine#runTypeDirectedResolution}).
     */
    public java.util.Optional<RAttribute> findAttributeOnType(RType type, String name) {
        return findAttribute(type, name);
    }

    /**
     * Surfaces {@link #inferAttributeType(RAttribute)} publicly for the
     * Category 10 attribute-chain fallback in
     * {@link TypeInferenceEngine#runTypeDirectedResolution}.
     */
    public RMetaAnnotatedType inferAttributeRefType(RAttribute attr) {
        return inferAttributeType(attr);
    }

    /**
     * True when {@code name} is a feature of the RECORD type {@code type}
     * (aliases unwrapped): {@code date}'s day/month/year, {@code dateTime}'s
     * date/time, {@code zonedDateTime}'s date/time/timezone — per
     * {@code basictypes.rosetta}, the same feature lists
     * {@code ExpressionTypeComputer.recordFieldType} gates on. Record features
     * are NOT {@link RAttribute}s, so {@link #findAttributeOnType} can never
     * see them; the PR #444 record-member arms in
     * {@link TypeInferenceEngine#runTypeDirectedResolution} use this membership
     * test to CLEAR a disguised-nav's stale tried-enum diagnostics without
     * binding anything (upstream resolves record features via
     * {@code RRecordFeature} and emits nothing for these navs).
     */
    public boolean isRecordFeatureOnType(RType type, String name) {
        RType t = unwrapAliases(type);
        if (t instanceof com.regnosys.rosetta.types.RRecordType rec) {
            for (var f : rec.features()) {
                if (f.name().equals(name)) return true;
            }
        }
        return false;
    }

    /**
     * True when {@code name} is an OPTION of the receiver's CHOICE VIEW
     * ({@link #choiceViewOfType} — PR #451: the view is the receiver itself
     * for a direct choice, aliases unwrapped, and the inherited parent
     * choice for a {@code type X extends ChoiceC} data type, so
     * extends-choice receivers admit their inherited options exactly like
     * upstream's {@code getAllAttributes} supertype walk). Upstream's
     * feature scope on a choice receiver is
     * the choice viewed as a data type whose attributes ARE its options
     * ({@code RosettaEcoreUtil.allFeaturesExcludingEnumValues} delegates to
     * {@code RChoiceType.asRDataType()}; {@code ChoiceOption extends Attribute}
     * with name = the WRITTEN token text of its type reference and hardcoded
     * cardinality (0..1) — upstream {@code RosettaSimple.xcore}), so
     * {@code payout -> InterestRatePayout} is plain attribute navigation
     * upstream. The fork's {@link #findAttributeOnType} applies the
     * common-attribute rule on choices (the {@code ->>} deep-feature
     * semantics) and can never admit an option; the PR #446 choice-option
     * arms in {@link TypeInferenceEngine#runTypeDirectedResolution} use this
     * membership test to admit a disguised-nav's option leaf, and since PR
     * #449 those channels BIND the matched option via
     * {@link #choiceOptionNodeOnType} (#446 landed them clearing-only under
     * the TYPING-EXPOSURE law; the #449 bind-mode probe measured the bind
     * byte-neutral over the full 55-param population — the engine's Phase X1
     * T10 arms had always bound the same shape in ITEM-headed contexts).
     *
     * <p>Matches the option's WRITTEN type-reference text — upstream's name
     * rule is resolution-independent {@code NodeModelUtils} token text, so a
     * qualified-written option could only match a qualified leaf (which a nav
     * leaf never is; the corpus writes every choice option unqualified).
     * Falls back to the resolved option {@code RType} names when the choice
     * ref carries no AST node (IR-only fixtures — the
     * {@code matchChoiceOption} convention).
     */
    public boolean isChoiceOptionOnType(RType type, String name) {
        RType t = choiceViewOfType(type);
        if (t instanceof RChoiceTypeRef choiceRef) {
            if (choiceRef.astNode() != null) {
                for (var opt : choiceRef.astNode().options()) {
                    var tc = opt.typeCall();
                    if (tc != null && name.equals(tc.typeName())) return true;
                }
                return false;
            }
            for (RType opt : choiceRef.options()) {
                if (name.equals(opt.name())) return true;
            }
        }
        return false;
    }

    /**
     * The node-returning sibling of {@link #isChoiceOptionOnType}: walks the
     * SAME admission (the receiver's CHOICE VIEW via
     * {@link #choiceViewOfType} — a direct choice alias-unwrapped OR the
     * inherited parent choice of a {@code type X extends ChoiceC} data type
     * (PR #451); the option's WRITTEN type-reference text on the AST path,
     * the resolved option names on the
     * IR-only fallback) and returns the matched option's resolved AST node
     * (an {@code RDataType} or nested {@code RChoice} — the two forms
     * {@code ExpressionTypeComputer.astNodeToRType} converts), or {@code null}
     * when no option matches OR the matched option's target does not resolve
     * to a bindable node. Callers bind on non-null and fall back to the
     * clearing-only contract on null, so the bind is provably a SUBSET of the
     * boolean admission — {@code isChoiceOptionOnType} itself is untouched.
     */
    public com.regnosys.rosetta.ast.RRootElement choiceOptionNodeOnType(RType type, String name) {
        RType t = choiceViewOfType(type);
        if (t instanceof RChoiceTypeRef choiceRef) {
            if (choiceRef.astNode() != null) {
                for (var opt : choiceRef.astNode().options()) {
                    var tc = opt.typeCall();
                    if (tc != null && name.equals(tc.typeName())) {
                        var resolved = tc.referencedType();
                        if (resolved.isPresent()) {
                            if (resolved.get() instanceof com.regnosys.rosetta.ast.types.RDataType dt) {
                                return dt;
                            }
                            if (resolved.get() instanceof com.regnosys.rosetta.ast.types.RChoice ch) {
                                return ch;
                            }
                        }
                        return null;
                    }
                }
                return null;
            }
            for (RType opt : choiceRef.options()) {
                if (name.equals(opt.name())) {
                    if (opt instanceof RDataTypeRef d) {
                        return d.astNode();
                    }
                    if (opt instanceof RChoiceTypeRef c) {
                        return c.astNode();
                    }
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * PR #451 — the CHOICE VIEW of a type (aliases unwrapped): the
     * {@link RChoiceTypeRef} through which a receiver admits choice options
     * as features, or {@code null} when the type has no choice view.
     *
     * <p>Upstream's feature set on a receiver comes from
     * {@code RDataType.getAllAttributes}, which walks the FULL supertype
     * chain — and a {@code type X extends ChoiceC} declaration puts the
     * choice's options-as-attributes ({@code RChoiceType.asRDataType()}) on
     * that chain, so {@code basketConstituent extract [ Asset -> Commodity ]}
     * resolves {@code Asset} as an INHERITED option-attribute upstream
     * (cdm-6.20.6 {@code type BasketConstituent extends Observable}). The
     * fork models that parent link as {@link RDataType#choiceSuperType()};
     * this helper surfaces it for the choice-membership tests:
     * <ul>
     *   <li>an {@link RChoiceTypeRef} IS its own view (identity — every
     *       pre-#451 admission through {@link #isChoiceOptionOnType} /
     *       {@link #choiceOptionNodeOnType} is preserved byte-for-byte);</li>
     *   <li>an {@link RDataTypeRef} walks {@code superType()} /
     *       {@code choiceSuperType()} to the first choice ancestor and
     *       returns its populated view ({@link #choiceToRType});</li>
     *   <li>anything else has no view.</li>
     * </ul>
     */
    public RType choiceViewOfType(RType type) {
        RType t = unwrapAliases(type);
        if (t instanceof RChoiceTypeRef) {
            return t;
        }
        if (t instanceof RDataTypeRef dtRef) {
            java.util.Set<RDataType> visited = new HashSet<>();
            RDataType current = dtRef.astNode();
            while (current != null && visited.add(current)) {
                var choiceSuper = current.choiceSuperType();
                if (choiceSuper.isPresent()) {
                    return choiceToRType(choiceSuper.get(), new HashSet<>());
                }
                current = current.superType().orElse(null);
            }
        }
        return null;
    }

    /**
     * PR #451 — the inference {@link RType} for a resolved TYPE-LIKE node:
     * the same three node-branch mapping {@link #inferAttributeType} applies
     * to an attribute's resolved typeCall (data type → {@link RDataTypeRef},
     * enumeration → {@link REnumTypeRef}, choice → the fully-populated
     * choice ref), surfaced for the engine's switch-case narrowing — the
     * narrowed implicit item's type is the GUARD's resolved node
     * (upstream {@code RosettaTypeProvider.safeTypeOfImplicitVariable}:
     * choice-option guard → the option symbol's type, data guard → the
     * guard {@code Data}'s type). Returns {@code null} for any other node
     * kind.
     */
    public RType typeOfResolvedNode(com.regnosys.rosetta.ast.RRootElement node) {
        if (node instanceof RDataType dt) {
            return new RDataTypeRef(dt);
        }
        if (node instanceof REnumeration en) {
            return new REnumTypeRef(en);
        }
        if (node instanceof com.regnosys.rosetta.ast.types.RChoice ch) {
            return choiceToRType(ch, new HashSet<>());
        }
        return null;
    }

    /**
     * PR #451 — true when {@code leaf} names a METADATA FACE of the receiver
     * attribute: the attribute declares {@code [metadata <leaf>]} (e.g.
     * {@code partyReference -> reference} on a {@code [metadata reference]}
     * attribute, {@code identifier -> scheme} on {@code [metadata scheme]}).
     *
     * <p>Upstream admits these as ordinary features: the receiver's
     * {@code RMetaAnnotatedType} carries the attribute's meta attribute names
     * and {@code RosettaEcoreUtil.allFeaturesExcludingEnumValues} appends
     * {@code getMetaDescriptions(t, context)} — one feature per meta name,
     * resolved through {@code RosettaConfigExtension.findMetaTypes} against
     * EXPORTED {@code metaType} ROOT ELEMENTS (cdm declares them in
     * {@code base-desc.rosetta}), <b>not</b> against the {@code metadata}
     * annotation's own attribute declarations — a model that writes
     * {@code -> key} with no {@code metaType key} in scope does not resolve it
     * (spec R10; the sentence this replaces asserted the opposite, and the
     * committed oracle dump's {@code RosettaMetaType key} row is the receipt).
     * So the nav parses, resolves and emits NOTHING (the V0
     * oracle streams are error-free over every carrier). The fork's
     * {@link #inferAttributeType} does not populate
     * {@code RMetaAnnotatedType.metaAttributes}, so the admission is seated
     * where upstream's {@code RosettaTypeProvider.getRMetaAttributesOfSymbol}
     * reads the fact off the AST: the attribute's own {@code [metadata ...]}
     * annotation refs, UNIONED with the declared type's own annotations
     * walked up the full supertype chain ({@code getRMetaAttributesOfType} —
     * {@code while (current != null) annotations += current.getAnnotations();
     * current = superType}). The TYPE-level face is the corpus-witnessed
     * shape ({@code commodityPayouts -> key}, where {@code type
     * CommodityPayout ... [metadata key]} carries the annotation and the
     * input attribute carries nothing) and the only form upstream's validator
     * accepts — "[metadata key] annotation only allowed on a type" — while
     * the attribute-level face still resolves upstream despite the complaint
     * (a2 case (d) pins that; the oracle records the same {@code
     * RosettaMetaType} binding either way). NOT walked through type aliases:
     * upstream's gate is {@code a.getTypeCall().getType() instanceof Data},
     * so an alias-typed attribute contributes no type-level face there
     * either. The general upstream form (meta names flowing through alias
     * and chain types via {@code RMetaAnnotatedType}) is the recorded
     * faithful follow-on.
     */
    public boolean isMetaFaceOnAttribute(RAttribute recv, String leaf) {
        if (recv == null || leaf == null) {
            return false;
        }
        if (hasMetadataAnnotation(recv.annotationRefs(), leaf)) {
            return true;
        }
        var tc = recv.typeCall();
        RNode declared = tc == null ? null : tc.referencedType().orElse(null);
        if (declared instanceof RDataType dt) {
            Set<RDataType> visited = new HashSet<>();
            RDataType current = dt;
            while (current != null && visited.add(current)) {
                if (hasMetadataAnnotation(current.annotationRefs(), leaf)) {
                    return true;
                }
                // Upstream models a choice as Data, so a choice parent sits on
                // the same getSuperType() chain and its annotations count; the
                // fork splits the link into choiceSuperType(). A choice
                // declaration has no extends clause of its own, so the hop is
                // terminal.
                var choiceSuper = current.choiceSuperType();
                if (choiceSuper.isPresent()
                        && hasMetadataAnnotation(choiceSuper.get().annotationRefs(), leaf)) {
                    return true;
                }
                current = current.superType().orElse(null);
            }
        }
        return false;
    }

    private static boolean hasMetadataAnnotation(
            List<com.regnosys.rosetta.ast.annotations.RAnnotationRef> refs, String leaf) {
        for (var ref : refs) {
            if ("metadata".equals(ref.annotationName())
                    && leaf.equals(ref.qualifierName().orElse(null))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Infers the type of an attribute for path walking. Reads the attribute's
     * typeCall to determine the next type context.
     */
    private RMetaAnnotatedType inferAttributeType(RAttribute attr) {
        var tc = attr.typeCall();
        if (tc == null) return RMetaAnnotatedType.MISSING;
        var resolved = tc.referencedType();
        if (resolved.isPresent()) {
            if (resolved.get() instanceof RDataType dt) {
                return RMetaAnnotatedType.withNoMeta(new RDataTypeRef(dt));
            }
            if (resolved.get() instanceof REnumeration en) {
                return RMetaAnnotatedType.withNoMeta(new REnumTypeRef(en));
            }
            // Phase X1 closure T0p (Gap I): choice-typed attribute. Build a
            // fully-populated RChoiceTypeRef so the attribute no longer infers
            // MISSING — required by Gap-B Cat 12, which reads the LHS
            // attribute's type via this method (e.g. `payout : Payout` where
            // Payout is a choice) and then checks the restriction subtype via
            // SubtypeRelation Rule 8 (which iterates options()).
            if (resolved.get() instanceof com.regnosys.rosetta.ast.types.RChoice ch) {
                return RMetaAnnotatedType.withNoMeta(
                    choiceToRType(ch, new java.util.HashSet<>()));
            }
        }
        // Fallback to builtin
        if (tc.typeName() != null) {
            var builtin = builtins.lookup(tc.typeName());
            if (builtin.isPresent()) return RMetaAnnotatedType.withNoMeta(builtin.get());
        }
        return RMetaAnnotatedType.MISSING;
    }

    /**
     * Builds a fully-populated {@link RChoiceTypeRef} from an {@link
     * com.regnosys.rosetta.ast.types.RChoice} — each option's resolved type
     * becomes an element of {@code options()} so SubtypeRelation Rule 8 can
     * match. Recurses for nested choices; {@code visited} guards cycles.
     * Mirrors {@code ExpressionTypeComputer.choiceToRType} (the two inference
     * entry points must return the same shape for choice-typed attributes).
     */
    private RType choiceToRType(com.regnosys.rosetta.ast.types.RChoice ch,
                                java.util.Set<com.regnosys.rosetta.ast.types.RChoice> visited) {
        if (!visited.add(ch)) {
            return new RChoiceTypeRef(ch.name(), java.util.List.of(), ch);
        }
        java.util.List<RType> options = new java.util.ArrayList<>();
        for (var option : ch.options()) {
            var tc = option.typeCall();
            if (tc == null) continue;
            var resolved = tc.referencedType();
            if (resolved.isPresent()) {
                var n = resolved.get();
                if (n instanceof RDataType dt) options.add(new RDataTypeRef(dt));
                else if (n instanceof REnumeration en) options.add(new REnumTypeRef(en));
                else if (n instanceof com.regnosys.rosetta.ast.types.RChoice inner)
                    options.add(choiceToRType(inner, visited));
            } else if (tc.typeName() != null) {
                builtins.lookup(tc.typeName()).ifPresent(options::add);
            }
        }
        return new RChoiceTypeRef(ch.name(), options, ch);
    }

    // === T16: External attributes, enum values, annotations, switch guards ===

    /** Resolves an external regular attribute against its parent type. */
    public void resolveExternalAttribute(RExternalRegularAttribute era, RMetaAnnotatedType contextType) {
        if (contextType.isMissing()) return;
        var attr = findAttribute(contextType.type(), era.name());
        attr.ifPresent(a -> { era.setResolvedAttribute(a); notifyResolved(era, a); });
    }

    /** Resolves an external enum value against its parent enumeration. */
    public void resolveExternalEnumValue(RExternalEnumValue eev, RMetaAnnotatedType contextType) {
        if (contextType.isMissing()) return;
        if (contextType.type() instanceof REnumTypeRef enumRef) {
            findEnumValue(enumRef.astNode(), eev.name())
                    .ifPresent(v -> { eev.setResolvedEnumValue(v); notifyResolved(eev, v); });
        }
    }

    /** Resolves an annotation path segment against the current type context. */
    public void resolveAnnotationPathSegment(RAnnotationPathSegment seg, RMetaAnnotatedType contextType) {
        if (contextType.isMissing()) return;
        var attr = findAttribute(contextType.type(), seg.name());
        attr.ifPresent(a -> { seg.setResolvedAttribute(a); notifyResolved(seg, a); });
    }

    /** Resolves a switch case guard against the switch argument type. */
    public void resolveSwitchGuard(RSwitchCaseGuard guard, RMetaAnnotatedType switchType) {
        if (switchType.isMissing()) return;
        if (guard.kind() != SwitchGuardKind.NAME) return; // literals don't need resolution

        String name = guard.qualifiedName().orElse(null);
        if (name == null) return;

        // For enum switch types, resolve to enum value
        if (switchType.type() instanceof REnumTypeRef enumRef) {
            var val = findEnumValue(enumRef.astNode(), name);
            val.ifPresent(v -> { guard.setResolvedGuard(v); notifyResolved(guard, v); });
        }
        // PR #451 — CHOICE-subject NAME guard: upstream scopes the guard
        // cross-ref to the subject choice's OPTIONS
        // (RosettaScopeProvider SWITCH_CASE_GUARD__REFERENCE_GUARD →
        // RChoiceType.getAllOptions), so `inputCriteria switch AllCriteria
        // then ...` binds the AllCriteria option. Resolve the written name
        // against the subject's choice view and store the matched option's
        // resolved TYPE node (RDataType / nested RChoice — the same node
        // choiceOptionNodeOnType returns; enum-typed options stay EMPTY,
        // exactly like the pre-#451 state). The engine's switch-case
        // narrowing reads this node; both generator resolvedGuard consumers
        // treat any non-REnumValue node exactly like EMPTY (guardEnumValue /
        // renderEnumGuardConstant fall through on the instanceof check), so
        // the store is render-neutral by construction. NOT gated on a prior
        // resolution: the linker's speculative type store
        // (resolveSwitchGuardType) picks the bare name globally, and when an
        // option name collides with a same-named type in ANOTHER cell the
        // option's own target — the subject choice's cell — must win (the
        // #445 same-cell law at this seat); in the common case both paths
        // produce the SAME node and the re-store is a no-op. Idempotent:
        // the subject-kind arms are mutually exclusive (a type is never
        // both enum and choice). Gated on the subject being genuinely
        // CHOICE-typed (upstream's guard scope for an RDataType subject is
        // the global DATA filter, never a supertype choice's options — so
        // the widened choice VIEW deliberately does not apply here).
        else if (unwrapAliases(switchType.type()) instanceof RChoiceTypeRef subjectChoice) {
            com.regnosys.rosetta.ast.RRootElement optionNode =
                choiceOptionNodeOnType(subjectChoice, name);
            if (optionNode != null) {
                guard.setResolvedGuard(optionNode);
                notifyResolved(guard, optionNode);
            }
        }
        // DATA-subject NAME guards (`fpml.CreditDefaultSwap then ...`) are
        // global type references — upstream scopes them to the DATA eClass
        // over the default (import-aware) scope. The fork resolves them at
        // the LINKER (GlobalResolutionPass.resolveSwitchGuardType — the
        // import/alias-aware qualified ladder, kind-gated to type-like
        // nodes), which runs before this pass; the resolvedGuard node is
        // already in place here.
    }

    /** Finds an enum value by name (including inherited values). */
    private Optional<REnumValue> findEnumValue(REnumeration en, String name) {
        for (REnumValue val : en.values()) {
            if (name.equals(val.name())) return Optional.of(val);
        }
        // Search parent enum
        if (en.superType().isPresent()) {
            return findEnumValue(en.superType().get(), name);
        }
        return Optional.empty();
    }

    // === Attribute lookup ====================================================

    /** Finds an attribute by name on a type (including inherited attributes). */
    private Optional<RAttribute> findAttribute(RType type, String name) {
        if (type instanceof RDataTypeRef dtRef) {
            return findAttributeOnDataType(dtRef.astNode(), name, new HashSet<>());
        }
        if (type instanceof RAliasType) {
            RType t = unwrapAliases(type);
            // Still an alias after the cap = a cyclic chain — decline.
            return t instanceof RAliasType ? Optional.empty() : findAttribute(t, name);
        }
        if (type instanceof RChoiceTypeRef) {
            // v3.1 C1 part 2 (SF-1, the root): a CHOICE receiver's PLAIN-arrow
            // feature scope is its OPTIONS AND NOTHING ELSE — upstream's scope
            // there is asRDataType(), whose attributes ARE the options, and
            // `c -> shared` is refused (probe P1, the committed refusal pin:
            // "Couldn't resolve reference to RosettaFeature 'shared'"). The
            // shared-attribute reach-through is the DEEP path's semantics
            // (`->>`, R11) and lives in findAttributeDeep's own choice arm.
            // lookupFeature's call-site guard had closed ONE door; every legacy
            // rung reaching here through findAttributeOnType kept the
            // wrong-accept until the arm itself refused.
            return Optional.empty();
        }
        return Optional.empty();
    }

    /** Searches a data type and its supertypes for an attribute by name. */
    private Optional<RAttribute> findAttributeOnDataType(
            RDataType dt, String name, Set<RDataType> visited) {
        if (!visited.add(dt)) return Optional.empty(); // cycle guard
        for (RAttribute attr : dt.attributes()) {
            if (name.equals(attr.name())) return Optional.of(attr);
        }
        // Search supertype
        if (dt.superType().isPresent()) {
            return findAttributeOnDataType(dt.superType().get(), name, visited);
        }
        return Optional.empty();
    }

    /** Deep search: looks for an attribute transitively through nested types. */
    private Optional<RAttribute> findAttributeDeep(
            RType type, String name, Set<RDataType> visited) {
        // Direct lookup first
        var direct = findAttribute(type, name);
        if (direct.isPresent()) return direct;

        // The DEEP path is where a choice's common attributes ARE reachable
        // (`->>`, R11) — the arm findAttribute deliberately refuses for the
        // plain arrow (SF-1; probe P1's `c ->> shared` control resolves).
        if (type instanceof RChoiceTypeRef choice) {
            var common = findCommonAttribute(choice.options(), name);
            if (common.isPresent()) return common;
        }

        // Recurse into data type attributes
        if (type instanceof RDataTypeRef dtRef) {
            if (!visited.add(dtRef.astNode())) return Optional.empty();
            for (RAttribute attr : dtRef.astNode().attributes()) {
                var tc = attr.typeCall();
                if (tc != null && tc.referencedType().isPresent()) {
                    var attrType = tc.referencedType().get();
                    if (attrType instanceof RDataType nested) {
                        var found = findAttributeDeep(
                            new RDataTypeRef(nested), name, visited);
                        if (found.isPresent()) return found;
                    }
                }
            }
        }
        return Optional.empty();
    }

    /** Finds an attribute common to all options in a choice type. */
    private Optional<RAttribute> findCommonAttribute(List<RType> options, String name) {
        return findCommonAttribute(options, name, new HashSet<>());
    }

    /**
     * A NESTED-choice option contributes its own common set, recursively — the
     * deep path's semantics compose (cdm 6.21+ nests `choice Index` inside
     * `choice Observable`, and `observable ->> identifier` reaches the
     * attribute through BOTH hops). This recursion used to happen by accident
     * through {@code findAttribute}'s choice arm; when SF-1 made that arm
     * refuse (the PLAIN arrow's rule), the nested hop silently died with it —
     * caught by the 25-cell seed the same session (SYMBOL_NOT_FOUND +21 per
     * drr 7.x cell), and now explicit here where the DEEP semantics live.
     * Cycle-guarded on the choice node: a malformed self-referential option
     * graph declines instead of overflowing.
     */
    private Optional<RAttribute> findCommonAttribute(
            List<RType> options, String name, Set<com.regnosys.rosetta.ast.types.RChoice> visited) {
        RAttribute found = null;
        for (RType option : options) {
            Optional<RAttribute> attr;
            if (unwrapAliases(option) instanceof RChoiceTypeRef nested) {
                if (nested.astNode() != null && !visited.add(nested.astNode())) {
                    return Optional.empty(); // cyclic option graph — decline
                }
                attr = findCommonAttribute(nested.options(), name, visited);
            } else {
                attr = findAttribute(option, name);
            }
            if (attr.isEmpty()) return Optional.empty(); // must exist on all
            if (found == null) found = attr.get();
        }
        return Optional.ofNullable(found);
    }
}
