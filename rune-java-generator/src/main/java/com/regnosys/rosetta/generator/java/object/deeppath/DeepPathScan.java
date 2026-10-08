package com.regnosys.rosetta.generator.java.object.deeppath;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.enums.CardCheckOp;
import com.regnosys.rosetta.ast.expressions.unary.RCardinalityCheckExpr;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RCardinality;
import com.regnosys.rosetta.ast.supporting.RChoiceOption;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.types.RChoiceTypeRef;
import com.regnosys.rosetta.types.RDataTypeRef;
import com.regnosys.rosetta.types.RType;

/**
 * Family-owned deep-feature discovery for the deep-path util generator
 * (coverage burn-down wave C) — a faithful port of upstream
 * {@code com.regnosys.rosetta.utils.DeepFeatureCallUtil}'s
 * {@code isEligibleForDeepFeatureCall} + {@code findDeepFeatureMap} pair,
 * consuming the workspace AST through {@link GeneratorModel} (the wave-B
 * precedent: build family-side what the shared path lacks — the shared
 * {@code rune-parser} port's eligibility gate is a documented stub that
 * returns {@code false}, and un-stubbing it would touch machinery consumed
 * by byte-proven callers).
 *
 * <p><b>THE ORDER LAW.</b> Upstream renders one {@code choose<Feature>} method per
 * entry of {@code findDeepFeatureMap(type).values()} — a plain
 * {@code java.util.HashMap<String, RAttribute>} — so the golden method order is
 * the JDK HashMap iteration order produced by upstream's exact put/remove
 * sequence over attribute-name keys. This port therefore mirrors the upstream
 * algorithm OPERATION BY OPERATION on a real {@code HashMap<String, ScanAttr>}:
 * same map type, same key strings, same insertion/removal sequence — which
 * reproduces the iteration order byte-for-byte. Do NOT "clean this up" into an
 * ordered map, do NOT memoize the returned map (a copied HashMap can size its
 * table differently and change iteration order), and do NOT reorder the loop
 * bodies.
 *
 * <p>Eligibility mirrors upstream: a {@code Data} type with an own (never
 * inherited) {@code one-of} condition, at least one attribute and every
 * attribute single-optional {@code (0..1)}; a {@code choice} type is always
 * eligible (upstream's {@code ChoiceImpl.getConditions()} synthesizes the
 * {@code Choice} one-of condition, and options are single-optional by
 * construction). Choice options project as attributes named with the option's
 * type name AS WRITTEN (upstream {@code ChoiceOptionImpl.getName()} returns the
 * typeCall token text — capitalized), which keeps the HashMap key strings
 * identical on both sides.
 */
public final class DeepPathScan {

    private final GeneratorModel generatorModel;

    /**
     * Per-element attribute views are memoized so the SAME {@link ScanAttr}
     * instances flow through every recursion — upstream's
     * {@code intersectButRetainAttribute} retain check compares attribute
     * IDENTITY ({@code attr.equals(attributeToRetain)} on the same RAttribute
     * instance), and stable instances reproduce it. The deep-feature MAPS are
     * deliberately NOT memoized (see the order law above).
     */
    private final Map<Object, List<ScanAttr>> attributeCache = new IdentityHashMap<>();

    public DeepPathScan(GeneratorModel generatorModel) {
        this.generatorModel = generatorModel;
    }

    /**
     * One attribute-like member of a type: a declared {@code Data} attribute or a
     * choice option projected as an attribute. Identity semantics (no
     * equals/hashCode override) — instances are memoized per element so the
     * upstream retain-check identity comparison holds.
     */
    public static final class ScanAttr {
        private final String name;
        private final RRootElement owner;
        private final RType resolvedType;
        private final boolean isMulti;
        private final boolean hasMeta;

        ScanAttr(String name, RRootElement owner, RType resolvedType, boolean isMulti, boolean hasMeta) {
            this.name = name;
            this.owner = owner;
            this.resolvedType = resolvedType;
            this.isMulti = isMulti;
            this.hasMeta = hasMeta;
        }

        public String name() {
            return name;
        }

        /** The element that declares this attribute (the pojo to resolve getters against). */
        public RRootElement owner() {
            return owner;
        }

        public RType resolvedType() {
            return resolvedType;
        }

        public boolean isMulti() {
            return isMulti;
        }

        /** Value-wrapping meta ({@code FieldWithMetaX}/{@code ReferenceWithMetaX} getter surface). */
        public boolean hasMeta() {
            return hasMeta;
        }
    }

    /** Whether the deep-path family emits a util class for this root element. */
    public boolean isEligible(RRootElement element) {
        if (element instanceof RChoice choice) {
            // Upstream ChoiceImpl.getConditions() synthesizes the one-of; options
            // are single-optional by construction — eligibility reduces to >= 1 option.
            return !choice.options().isEmpty();
        }
        if (element instanceof RDataType dataType) {
            boolean hasOneOf = dataType.conditions().stream()
                    .anyMatch(c -> c.expression() instanceof RCardinalityCheckExpr check
                            && check.op() == CardCheckOp.ONE_OF);
            if (!hasOneOf) {
                return false;
            }
            Collection<RAttribute> allAttributes = generatorModel.allAttributes(dataType);
            if (allAttributes.isEmpty()) {
                return false;
            }
            return allAttributes.stream().allMatch(a -> isSingularOptional(a.cardinality().orElse(null)));
        }
        return false;
    }

    /**
     * The element's attribute view in declaration order (data: the supertype-chain
     * {@code allAttributes} walk; choice: options in declaration order, named with
     * the option type name as written).
     */
    public List<ScanAttr> attributesOf(RRootElement element) {
        return attributeCache.computeIfAbsent(element, e -> buildAttributes(element));
    }

    private List<ScanAttr> buildAttributes(RRootElement element) {
        List<ScanAttr> result = new ArrayList<>();
        if (element instanceof RChoice choice) {
            for (RChoiceOption option : choice.options()) {
                String name = option.typeCall() == null ? "?" : option.typeCall().typeName();
                RType resolved = option.typeCall() == null
                        ? null
                        : generatorModel.resolveTypeCall(option.typeCall());
                boolean hasMeta = MetaFieldGenerator.detectMetaKind(option.annotationRefs())
                        != MetaFieldGenerator.MetaKind.NONE;
                result.add(new ScanAttr(name, element, resolved, false, hasMeta));
            }
            return result;
        }
        RDataType dataType = (RDataType) element;
        for (RAttribute attr : generatorModel.allAttributes(dataType)) {
            boolean hasMeta = MetaFieldGenerator.detectMetaKind(attr) != MetaFieldGenerator.MetaKind.NONE;
            result.add(new ScanAttr(attr.name(), element, generatorModel.getType(attr),
                    generatorModel.isMulti(attr), hasMeta));
        }
        return result;
    }

    /** The element an attribute descends into ({@code Data} or {@code choice}), or null. */
    public RRootElement descendTarget(ScanAttr attr) {
        RType type = attr.resolvedType();
        if (type instanceof RDataTypeRef dataRef) {
            return dataRef.astNode();
        }
        if (type instanceof RChoiceTypeRef choiceRef) {
            return choiceRef.astNode();
        }
        return null;
    }

    /**
     * The upstream {@code findDeepFeatureMap} port — see the class javadoc's order
     * law. Returns a FRESH {@code HashMap} whose {@code values()} iteration order
     * IS the golden {@code choose*} method order.
     */
    public Map<String, ScanAttr> findDeepFeatureMap(RRootElement element) {
        if (!isEligible(element)) {
            return new HashMap<>();
        }
        Map<String, ScanAttr> deepIntersection = null;
        Map<String, ScanAttr> result = new HashMap<>();
        List<ScanAttr> allAttributes = attributesOf(element);
        for (ScanAttr attr : allAttributes) {
            result.put(attr.name(), attr);
        }
        for (ScanAttr attr : allAttributes) {
            RRootElement attrTarget = descendTarget(attr);
            Map<String, ScanAttr> attrDeepFeatureMap;
            if (attrTarget != null) {
                attrDeepFeatureMap = findDeepFeatureMap(attrTarget);
                for (ScanAttr attrFeature : attributesOf(attrTarget)) {
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

    private void intersect(Map<String, ScanAttr> featuresMapToModify, Map<String, ScanAttr> otherFeatureMap) {
        intersectButRetainAttribute(featuresMapToModify, otherFeatureMap, null);
    }

    private void intersectButRetainAttribute(Map<String, ScanAttr> featuresMapToModify,
                                             Map<String, ScanAttr> otherFeatureMap,
                                             ScanAttr attributeToRetain) {
        featuresMapToModify.entrySet().removeIf(entry -> {
            String attrName = entry.getKey();
            ScanAttr attr = entry.getValue();
            if (attr.equals(attributeToRetain)) {
                return false;
            }
            ScanAttr otherAttr = otherFeatureMap.get(attrName);
            if (otherAttr != null) {
                if (match(attr, otherAttr)) {
                    return false;
                }
            }
            return true;
        });
        // Upstream comment retained: "Make sure we don't give back an attribute
        // with metadata if not all of them have it." Swaps the VALUE only — key
        // membership (and therefore iteration order) is untouched.
        for (Map.Entry<String, ScanAttr> e : featuresMapToModify.entrySet()) {
            ScanAttr currFeature = e.getValue();
            ScanAttr otherFeature = otherFeatureMap.get(e.getKey());
            if (otherFeature != null && currFeature.hasMeta() && !otherFeature.hasMeta()) {
                e.setValue(otherFeature);
            }
        }
    }

    private void merge(Map<String, ScanAttr> featuresMapToModify, Map<String, ScanAttr> otherFeatureMap) {
        otherFeatureMap.forEach((name, attr) -> {
            ScanAttr candidate = featuresMapToModify.get(name);
            if (candidate != null) {
                if (!match(candidate, attr)) {
                    featuresMapToModify.remove(name);
                } else if (candidate.hasMeta() && !attr.hasMeta()) {
                    // The metadata-collapse value swap (upstream merge lines 127-130).
                    featuresMapToModify.put(name, attr);
                }
            } else {
                featuresMapToModify.put(name, attr);
            }
        });
    }

    /**
     * Upstream {@code match}: same resolved bare {@link RType} (meta ignored — the
     * refs compare by referenced AST node identity) and same multiplicity. The
     * null-safe branch mirrors the rune-parser port (upstream would NPE on an
     * unresolved type; the corpus never exercises it).
     */
    public boolean match(ScanAttr a, ScanAttr b) {
        RType ta = a.resolvedType();
        RType tb = b.resolvedType();
        if (ta == null || tb == null) {
            return ta == tb;
        }
        if (!ta.equals(tb)) {
            return false;
        }
        return a.isMulti() == b.isMulti();
    }

    /** Upstream {@code RCardinality.OPTIONAL} equality: {@code (0..1)} exactly. */
    private static boolean isSingularOptional(RCardinality card) {
        if (card == null) {
            // Choice-projected attributes and defaulted cardinalities are
            // single-optional (the rune-parser port's convention).
            return true;
        }
        return !card.isUnbounded()
                && card.inf() != null
                && card.sup() != null
                && card.inf().signum() == 0
                && card.sup().equals(BigInteger.ONE);
    }

}
