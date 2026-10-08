package com.regnosys.rosetta.utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RCardinality;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.types.RChoiceTypeRef;
import com.regnosys.rosetta.types.RDataTypeRef;
import com.regnosys.rosetta.types.RMissingType;
import com.regnosys.rosetta.types.RType;

/**
 * Phase X T2 — verbatim Java port of upstream
 * {@code com.regnosys.rosetta.utils.DeepFeatureCallUtil} (rune-dsl 9.75.3,
 * 173 LOC). Pure graph-traversal utility for navigating attribute chains
 * from a root data type to a target attribute through nested data types
 * and choice types.
 *
 * <p>Two API shape differences vs upstream, driven by fork divergence:
 *
 * <ol>
 *   <li><b>Public entry point takes {@link RType}, not {@link RDataType}.</b>
 *       The fork's spec § 3.4 broadens the signature to handle root types
 *       that arrive as {@link RDataTypeRef} or {@link RChoiceTypeRef} from
 *       callers. Non-data-type/non-choice roots return an empty path list.</li>
 *   <li><b>{@link AttributeTypeResolver} parameterisation.</b> Upstream's
 *       {@code RAttribute.getRMetaAnnotatedType().getRType()} accessor does
 *       not exist on the fork's {@link RAttribute} (the fork's resolved
 *       attribute-type lives on {@link RTypeCall} via {@code SymbolId} +
 *       workspace lookup, or on the rune-java-generator-side
 *       {@code GeneratorModel}). The util takes an {@code AttributeTypeResolver}
 *       so unit tests + production wiring can supply the resolution
 *       strategy without dragging GeneratorModel into rune-parser.</li>
 * </ol>
 *
 * <p>The algorithmic shape is preserved verbatim from upstream — see method-level
 * cross-references for the upstream-line anchors.
 */
public class DeepFeatureCallUtil {

    /**
     * Functional interface used by {@link DeepFeatureCallUtil} to resolve an
     * attribute's {@link RType}. Production wiring will route this through
     * {@code com.regnosys.rosetta.generator.java.GeneratorModel#getType(RAttribute)}
     * (which performs the workspace lookup + builtin fallback); unit tests
     * can supply a synthetic {@code Map<RAttribute, RType>::get}.
     *
     * <p>Returning {@code null} (or {@link RMissingType#INSTANCE}) is the
     * idiomatic "no resolved type" outcome — callers that do not have
     * resolution context (e.g., post-parse but pre-link) can short-circuit
     * traversal cleanly.
     */
    @FunctionalInterface
    public interface AttributeTypeResolver {
        RType getType(RAttribute attribute);
    }

    private final AttributeTypeResolver resolver;

    /**
     * Constructs a utility with the default AST-direct resolver: reads the
     * attribute's {@link RTypeCall} and dereferences it via
     * {@link RTypeCall#referencedType()} (which requires the workspace to
     * be attached to the parent attribute). Returns {@link RMissingType#INSTANCE}
     * when the type is not resolved (so the traversal short-circuits cleanly).
     */
    public DeepFeatureCallUtil() {
        this(DeepFeatureCallUtil::defaultResolveType);
    }

    /**
     * Constructs a utility with an explicit {@link AttributeTypeResolver}.
     * Used in unit tests + by production wiring (LabelProviderGenerator at
     * T3) that needs to delegate to GeneratorModel's resolution machinery.
     */
    public DeepFeatureCallUtil(AttributeTypeResolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
    }

    // === Public API (verbatim port — upstream lines 21-25) ===================

    /**
     * Find all deep paths from {@code rootType} to a feature that matches
     * {@code deepFeature} by type/cardinality shape. Each returned path is
     * a list of {@link RAttribute}s representing one route through the type
     * graph; the last element is the matching attribute.
     *
     * <p>If {@code rootType} is neither {@link RDataTypeRef} nor
     * {@link RChoiceTypeRef}, returns an empty list (cannot recurse into a
     * basic/enum/record/alias/missing type).
     *
     * <p>Upstream port of {@code findDeepFeaturePaths(RDataType, RAttribute)}
     * (lines 21-25). Entry-point widened to {@link RType} per fork spec § 3.4.
     */
    public List<List<RAttribute>> findDeepFeaturePaths(RType rootType, RAttribute deepFeature) {
        RDataType startType = unwrapToDataType(rootType);
        if (startType == null) {
            return Collections.emptyList();
        }
        List<List<RAttribute>> result = new ArrayList<>();
        findDeepFeaturePaths(startType, new ArrayList<>(), deepFeature, result);
        return result;
    }

    // === Recursive walker (verbatim port — upstream lines 26-48) =============

    private void findDeepFeaturePaths(RDataType currentType,
                                      List<RAttribute> currentPath,
                                      RAttribute deepFeature,
                                      List<List<RAttribute>> paths) {
        Collection<RAttribute> allAttrs = getAllAttributes(currentType);
        Optional<RAttribute> matchingAttribute = allAttrs.stream()
                .filter(a -> match(a, deepFeature))
                .findAny();
        matchingAttribute.ifPresentOrElse(
                a -> {
                    // Mirror the descend branch's defensive copy: never alias
                    // a caller-owned list into the returned `paths`. Without
                    // the copy the leaf branch silently mutates the parent's
                    // currentPath via aliasing — a foot-gun for any later
                    // change that recurses through the same frame.
                    List<RAttribute> leafPath = new ArrayList<>(currentPath);
                    leafPath.add(a);
                    paths.add(leafPath);
                },
                () -> {
                    allAttrs.forEach(a -> {
                        RType attrType = resolver.getType(a);
                        if (attrType instanceof RChoiceTypeRef) {
                            attrType = wrapAsDataTypeRef(asRDataType((RChoiceTypeRef) attrType));
                        }
                        if (attrType instanceof RDataTypeRef) {
                            List<RAttribute> newPath = new ArrayList<>(currentPath);
                            newPath.add(a);
                            findDeepFeaturePaths(((RDataTypeRef) attrType).astNode(),
                                    newPath, deepFeature, paths);
                        }
                    });
                });
    }

    // === DeepFeatureMap derivation (verbatim port — upstream lines 50-92) ===

    /**
     * Returns the deep-feature collection for {@code type} — every attribute
     * reachable via the one-of decomposition rules upstream's
     * {@code findDeepFeatureMap} encodes. Verbatim port of upstream lines
     * 50-52.
     */
    public Collection<RAttribute> findDeepFeatures(RDataType type) {
        return findDeepFeatureMap(type).values();
    }

    /**
     * Verbatim port of upstream {@code findDeepFeatureMap(RDataType)} lines
     * 54-92. Computes the deep-feature map by intersecting per-attribute
     * deep-feature views with the type's own attribute set, preserving
     * upstream's metadata-collapse rules.
     */
    public Map<String, RAttribute> findDeepFeatureMap(RDataType type) {
        if (!isEligibleForDeepFeatureCall(type)) {
            return new HashMap<>();
        }

        Map<String, RAttribute> deepIntersection = null;
        Map<String, RAttribute> result = new HashMap<>();
        Collection<RAttribute> allAttributes = getAllAttributes(type);
        for (RAttribute attr : allAttributes) {
            result.put(attr.name(), attr);
        }
        for (RAttribute attr : allAttributes) {

            RType attrType = resolver.getType(attr);
            if (attrType instanceof RChoiceTypeRef) {
                attrType = wrapAsDataTypeRef(asRDataType((RChoiceTypeRef) attrType));
            }
            Map<String, RAttribute> attrDeepFeatureMap;
            if (attrType instanceof RDataTypeRef) {
                RDataType attrDataType = ((RDataTypeRef) attrType).astNode();
                attrDeepFeatureMap = findDeepFeatureMap(attrDataType);
                for (RAttribute attrFeature : getAllAttributes(attrDataType)) {
                    attrDeepFeatureMap.put(attrFeature.name(), attrFeature);
                }
            } else {
                attrDeepFeatureMap = new HashMap<>();
            }
            if (deepIntersection == null) {
                deepIntersection = attrDeepFeatureMap;
            } else {
                intersect(deepIntersection, attrDeepFeatureMap);
            }
            intersectButRetainAttribute(result, attrDeepFeatureMap, attr);
        }
        if (deepIntersection != null) {
            merge(result, deepIntersection);
        }
        return result;
    }

    // === Intersection / merge helpers (verbatim port — upstream lines 93-135) ===

    private void intersect(Map<String, RAttribute> featuresMapToModify,
                           Map<String, RAttribute> otherFeatureMap) {
        intersectButRetainAttribute(featuresMapToModify, otherFeatureMap, null);
    }

    private void intersectButRetainAttribute(Map<String, RAttribute> featuresMapToModify,
                                             Map<String, RAttribute> otherFeatureMap,
                                             RAttribute attributeToRetain) {
        featuresMapToModify.entrySet().removeIf(entry -> {
            String attrName = entry.getKey();
            RAttribute attr = entry.getValue();
            if (attr.equals(attributeToRetain)) {
                return false;
            }
            RAttribute otherAttr = otherFeatureMap.get(attrName);
            if (otherAttr != null) {
                if (match(attr, otherAttr)) {
                    return false;
                }
            }
            return true;
        });
        // Upstream comment retained verbatim: "Make sure we don't give back
        // an attribute with metadata if not all of them have it."
        // The fork's RAttribute does not yet carry an RMetaAnnotatedType
        // analogue with hasAttributeMeta(); the metadata-collapse branch is
        // an architecturally-equivalent no-op until that surface lands.
        // See spec § 3.4 dependency table (RMetaAnnotatedType.hasAttributeMeta).
    }

    private void merge(Map<String, RAttribute> featuresMapToModify,
                       Map<String, RAttribute> otherFeatureMap) {
        otherFeatureMap.forEach((name, attr) -> {
            RAttribute candidate = featuresMapToModify.get(name);
            if (candidate != null) {
                if (!match(candidate, attr)) {
                    featuresMapToModify.remove(name);
                }
                // Upstream's metadata-collapse branch (lines 127-130) is a
                // no-op for the same reason noted in intersectButRetainAttribute.
            } else {
                featuresMapToModify.put(name, attr);
            }
        });
    }

    // === Match + eligibility (verbatim port — upstream lines 136-172) ========

    /**
     * Two attributes match when they share resolved {@link RType} and
     * cardinality multiplicity. Upstream port (lines 136-144) — the
     * metadata-equality branch is intentionally absent until the fork
     * gets the {@code RMetaAnnotatedType.hasAttributeMeta} analogue
     * (see spec § 3.4 dependency table).
     */
    public boolean match(RAttribute a, RAttribute b) {
        RType ta = resolver.getType(a);
        RType tb = resolver.getType(b);
        if (ta == null || tb == null) {
            return ta == tb;
        }
        if (!ta.equals(tb)) {
            return false;
        }
        if (isMulti(a) != isMulti(b)) {
            return false;
        }
        return true;
    }

    /**
     * Whether the type has a {@code one-of} condition where every attribute
     * is single-optional and at least one attribute is present. Upstream
     * port (lines 146-161) — the fork's {@code one-of} eligibility check
     * walks the data type's RConditions for a {@code OneOf}-shaped expression.
     *
     * <p><b>NOTE:</b> the fork does not yet ship an {@code one-of} AST shape
     * recognisable at this layer (the condition body lives at the expression
     * tree but is not a dedicated {@code OneOfOperation} node). This stub
     * returns {@code false} until the AST shape is locked at a future task.
     * The path-traversal entry point ({@link #findDeepFeaturePaths}) does
     * NOT depend on this method — it is only consulted by
     * {@link #findDeepFeatureMap} per upstream invariant.
     *
     * <p>Package-private until the fork's {@code OneOfOperation} AST node
     * lands — exposing a stub on a public utility surface is a footgun for
     * downstream callers expecting upstream-parity semantics (they would
     * always receive {@code false}, silently). Promote to {@code public}
     * once the eligibility logic is wired through at D22 P2.3+.
     */
    boolean isEligibleForDeepFeatureCall(RDataType type) {
        // Eligibility requires (a) at least one attribute, (b) every
        // attribute (0..1), AND (c) a one-of condition. The fork's
        // condition AST does not yet expose the one-of shape distinctly;
        // until it does, the function returns false (the safe default —
        // upstream returns false when the eligibility chain fails).
        Collection<RAttribute> all = getAllAttributes(type);
        if (all.isEmpty()) return false;
        if (!all.stream().allMatch(a -> isSingularOptional(a.cardinality().orElse(null)))) {
            return false;
        }
        return hasOneOfCondition(type);
    }

    private boolean hasOneOfCondition(RDataType type) {
        // PLACEHOLDER: the fork's expression tree does not yet surface a
        // one-of-operation node at this layer. The deeper semantic check
        // is folded into the grammar-attached-validator track (D22 P2.3+).
        // Until then, return false (safe — findDeepFeatureMap returns an
        // empty map, matching upstream's behaviour for non-eligible types).
        Objects.requireNonNull(type, "type must not be null");
        return false;
    }

    /**
     * Whether the attribute is multi-valued. Upstream:
     * {@code RAttribute.isMulti()}; here uses the {@link RCardinality}
     * setter pair directly (cardinality is multi if {@code sup > 1} or
     * unbounded).
     *
     * <p><b>BigInteger comparison</b> (Copilot PR #72 R1+R2 F1):
     * {@code INT_LITERAL} grammar permits arbitrary-precision upper bounds, so
     * narrowing via {@code intValue()} would silently overflow for cardinalities
     * larger than {@link Integer#MAX_VALUE}. The null guard is also required
     * because {@link RCardinality#sup()} may be {@code null} when the bound is
     * not yet set (only the {@link RCardinality#isUnbounded() unbounded} sentinel
     * is universally non-null per the invariant at {@code RCardinality.java}
     * class-level javadoc).
     */
    public boolean isMulti(RAttribute attr) {
        return attr.cardinality()
                .map(c -> c.isUnbounded()
                        || (c.sup() != null && c.sup().compareTo(BigInteger.ONE) > 0))
                .orElse(false);
    }

    // === Helpers ============================================================

    /**
     * Unwrap the root {@link RType} to an {@link RDataType} for traversal.
     * Returns {@code null} if the type cannot host attributes (basic/enum/
     * record/alias/missing types).
     *
     * <p>{@link RChoiceTypeRef} is bridged via {@link #asRDataType(RChoiceTypeRef)}
     * so the caller can begin traversal at a choice type without an extra
     * unwrap call site.
     */
    private RDataType unwrapToDataType(RType type) {
        if (type instanceof RDataTypeRef) {
            return ((RDataTypeRef) type).astNode();
        }
        if (type instanceof RChoiceTypeRef) {
            return asRDataType((RChoiceTypeRef) type);
        }
        return null;
    }

    /**
     * Wrap an {@link RDataType} as an {@link RDataTypeRef}. Bridge for the
     * recursion site where {@code asRDataType()} returns an unwrapped
     * {@link RDataType}.
     */
    private RDataTypeRef wrapAsDataTypeRef(RDataType dataType) {
        return dataType == null ? null : new RDataTypeRef(dataType);
    }

    /**
     * Bridge from {@link RChoiceTypeRef} to an {@link RDataType}, mirroring
     * upstream's {@code RChoiceType.asRDataType()}. The fork's
     * {@link RChoiceTypeRef} ships its own
     * {@link RChoiceTypeRef#asRDataType()} (added at T2); this helper exists
     * so tests can override the bridge without subclassing the {@code final}
     * {@link RChoiceTypeRef}.
     */
    protected RDataType asRDataType(RChoiceTypeRef ref) {
        return ref.asRDataType();
    }

    /**
     * Returns ALL attributes of {@code type}, including inherited ones from
     * the super-type chain when the chain can be walked (workspace-attached
     * nodes). When workspace is not attached, the inherited walk is skipped
     * and only the type's own direct attributes are returned.
     *
     * <p>Upstream parity: matches {@code RDataType.getAllAttributes()}'s
     * inheritance-aware semantics on workspace-attached corpora; degrades
     * to direct-only for hand-built unit-test fixtures (per
     * {@link RNode#workspace()} contract).
     */
    private Collection<RAttribute> getAllAttributes(RDataType type) {
        if (type == null) return Collections.emptyList();
        Map<String, RAttribute> result = new LinkedHashMap<>();
        List<RDataType> chain = new ArrayList<>();
        RDataType current = type;
        // Walk the super-type chain when workspace is attached; bail out
        // cleanly if not (unit-test fixtures).
        while (current != null) {
            chain.add(current);
            current = trySuperType(current);
        }
        // Reverse: root first, so child overrides parent.
        for (int i = chain.size() - 1; i >= 0; i--) {
            for (RAttribute attr : chain.get(i).attributes()) {
                result.put(attr.name(), attr);
            }
        }
        return result.values();
    }

    private RDataType trySuperType(RDataType type) {
        try {
            return type.superType().orElse(null);
        } catch (IllegalStateException workspaceUnattached) {
            // Hand-built fixtures (no SymbolResolver attached) — graceful
            // degrade to direct-attrs-only. Production paths always have
            // workspace attached so this branch is not exercised there.
            return null;
        }
    }

    private boolean isSingularOptional(RCardinality card) {
        if (card == null) {
            // No explicit cardinality — upstream treats the default as
            // single-optional for the eligibility check.
            return true;
        }
        // Copilot PR #72 R1+R2 F1 class-of-issue sweep: avoid intValueExact()
        // which throws on overflow for cardinalities larger than Integer.MAX_VALUE.
        // Use BigInteger.equals(ONE) instead.
        return !card.isUnbounded()
                && card.inf() != null
                && card.sup() != null
                && card.inf().signum() == 0
                && card.sup().equals(BigInteger.ONE);
    }

    /**
     * Default {@link AttributeTypeResolver}: reads the attribute's
     * {@link RTypeCall#referencedType()} (workspace-backed) and maps the
     * resolved AST node back to its M4 {@link RType}. Returns
     * {@link RMissingType#INSTANCE} when the typecall has no resolution.
     */
    private static RType defaultResolveType(RAttribute attribute) {
        if (attribute == null) return RMissingType.INSTANCE;
        RTypeCall tc = attribute.typeCall();
        if (tc == null) return RMissingType.INSTANCE;
        Optional<RNode> resolved;
        try {
            resolved = tc.referencedType();
        } catch (IllegalStateException workspaceUnattached) {
            return RMissingType.INSTANCE;
        }
        if (resolved.isEmpty()) return RMissingType.INSTANCE;
        RNode node = resolved.get();
        if (node instanceof RDataType) {
            return new RDataTypeRef((RDataType) node);
        }
        // Other resolved kinds (enum / choice / alias) are not in scope for
        // this default resolver — production wiring delegates to
        // GeneratorModel.resolveTypeCall which has full coverage.
        return RMissingType.INSTANCE;
    }
}
