package com.regnosys.rosetta.generator.java.function;

import com.regnosys.rosetta.ast.annotations.RAnnotationPathExpression;
import com.regnosys.rosetta.ast.annotations.RAnnotationPathSegment;
import com.regnosys.rosetta.ast.annotations.RRuleReferenceAnnotation;
import com.regnosys.rosetta.ast.external.RExternalClass;
import com.regnosys.rosetta.ast.external.RExternalRegularAttribute;
import com.regnosys.rosetta.ast.external.RExternalRuleSource;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.regulatory.RReport;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.SilentDegradation;
import com.regnosys.rosetta.types.RChoiceTypeRef;
import com.regnosys.rosetta.types.RDataTypeRef;
import com.regnosys.rosetta.types.RType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Shared, faithful port of upstream {@code RuleReferenceService.traverse} +
 * {@code computeRulePathMapInContext} + {@code RulePathMap} + {@code RuleResult}
 * (built at PR #321 inside {@code LabelProviderGenerator}; PROMOTED here at
 * PR #322 with upstream's path tracking + a general fold so BOTH upstream
 * consumers exist on the fork):
 *
 * <ul>
 *   <li><b>{@code LabelProviderGenerator.buildAttributeToRuleMap}</b> — the
 *       as-label fold (upstream {@code LabelProviderGenerator}'s
 *       {@code ruleService.traverse} fold body; PR #321 M3), and</li>
 *   <li><b>{@code ReportGenerator} operation synthesis</b> — the
 *       {@code (path, rule)} fold of upstream
 *       {@code RObjectFactory.generateOperations} (PR #322): every
 *       non-explicitly-empty rule association becomes an
 *       {@code ROperation(SET, output, assignPath, ruleInvocation)}.</li>
 * </ul>
 *
 * <p>The traversal semantics matter for byte parity (PR #321 — the previous
 * label walk visited EVERY attribute of every reachable type and read inline
 * annotations only, which (a) ignored the report's {@code with source}
 * external rule source entirely, and (b) registered as-labels on attributes
 * upstream's traversal never reaches):
 * <ul>
 *   <li>An attribute with an ASSOCIATED rule reference (path-less inline
 *       or rule-source annotation, an explicit {@code empty}, a rule-source
 *       {@code -} minus, or a nested-context hit from an ancestor's pathed
 *       annotation) contributes its result and is NOT descended into.</li>
 *   <li>A MULTI-cardinality attribute is never descended into.</li>
 *   <li>Rule references WITH a {@code for} path descend as "nested rule
 *       context" and associate at the pointed-to attribute (excluded from
 *       the as-label map by the pathless-origin guard, but they still
 *       terminate descent there — upstream's
 *       {@code LabelProviderGenerator} fold guard
 *       {@code origin.path === null}; they DO join the operations fold, which
 *       has no pathless guard).</li>
 *   <li>Cycle detection is per-path ({@code visited} add/remove around the
 *       recursion), not global.</li>
 * </ul>
 *
 * <p>The {@code nonReportTypeCache} perf memo is deliberately not ported —
 * results are identical without it.
 */
public final class RuleReferenceTraversal {

    private final GeneratorModel generatorModel;

    public RuleReferenceTraversal(GeneratorModel generatorModel) {
        this.generatorModel = Objects.requireNonNull(generatorModel, "generatorModel");
    }

    /**
     * The traverse fold — upstream's {@code BiFunction<T, RuleReferenceContext, T>}
     * flattened to a consumer over {@code (path, result)}: {@code path} is the
     * attribute path from the start node (upstream
     * {@code RuleReferenceContext.getPath()}, last element = the associated
     * attribute) and {@code result} the associated {@link RuleResult}
     * (possibly explicitly empty — folds filter, exactly like upstream's).
     */
    public interface RuleFold {
        void apply(List<RAttribute> path, RuleResult result);
    }

    /**
     * Port of upstream {@code rules.RuleResult}: a rule associated with an
     * attribute path — {@code rule == null} means explicitly empty (an
     * {@code [ruleReference empty]} or a rule-source minus);
     * {@code originPathless} is upstream's {@code origin.path === null}
     * discriminator (true only for a {@code RuleReferenceAnnotation} without
     * a {@code for} path — the only origins eligible for the legacy as-label).
     */
    public record RuleResult(RRule rule, boolean originPathless) {}

    // === Entry points ========================================================

    /**
     * Run the traversal from {@code start} under {@code source}, invoking
     * {@code fold} for every rule association in upstream's traversal order.
     * Mirrors upstream {@code RuleReferenceService.traverse(source, type,
     * initialState, updateState)} (the public overload seeds the empty
     * nested-rule context, path and visited set).
     */
    public void traverse(RExternalRuleSource source, RDataType start, RuleFold fold) {
        traverseRuleReferences(source, start, new HashMap<>(), new ArrayList<>(), new HashSet<>(), fold);
    }

    /**
     * v3.2 seat 4 (PR #625, F7): the ONE declaration both report seats consult — the report function
     * ({@code ReportGenerator}) and its label provider ({@code LabelProviderGenerator}), LAW 69: the
     * synthetic output of a report-backed function must carry the linker's id ({@code RReport.withTypeId},
     * copied by {@code RFunction.fromReport}); one without it is REFUSED at
     * {@link SilentDegradation.Site#REPORT_REFERENCE_UNRESOLVED}, never resolved by a workspace-wide
     * search on the simple name — the search that typed the chaos s07 report functions with the FIRST
     * namespace's report type and rules, and that left the label provider of an unresolvable
     * {@code with type} EMPTY in silence (its start node null, no label, no error).
     */
    public static void requireResolvedReportType(RFunction rFunction, RReport report) {
        RAttribute output = rFunction.output().orElse(null);
        if (output == null || output.typeCall() == null || output.typeCall().referencedTypeId().isEmpty()) {
            throw SilentDegradation.refuse(SilentDegradation.Site.REPORT_REFERENCE_UNRESOLVED,
                    "report " + rFunction.name() + ": its `with type " + report.withType()
                            + "` did not resolve in the linker (see its TYPE_NOT_FOUND diagnostic);"
                            + " the generator does not guess a report type by name", report);
        }
    }

    /**
     * The report's {@code with source} rule source — the LINKER's resolved reference
     * ({@code RReport.withSourceId}, scoped like every other name), read through the generator's
     * workspace; {@code null} only when the report declares no {@code with source} (upstream passes a
     * null source in exactly that case — inline annotations only). A {@code with source} that did not
     * resolve is REFUSED here at {@link SilentDegradation.Site#REPORT_REFERENCE_UNRESOLVED}, never
     * searched for by simple name across the workspace: the own-model-first / segment-boundary walk this
     * replaced at v3.2 seat 4 (PR #625) was the report-type leak's twin, corpus-inert only because every
     * chaos rule source sits beside its report. Both report seats consult this read (LAW 69); the
     * refusal names the report function exactly as the with-type seat's does (round 1).
     */
    public RExternalRuleSource resolveRuleSource(RFunction rFunction, RReport report) {
        String name = report.withSource().orElse(null);
        if (name == null || name.isEmpty()) return null;
        // v3.2 seat 4 (PR #625, F7): the rule source is the linker's resolved reference
        // (RReport.withSourceId — scoped like every other name); a `with source` that did not resolve
        // is REFUSED here, never searched for by simple name across the workspace (the own-model-first
        // walk retired at this seat was the report-type leak's twin, corpus-inert only because every
        // chaos rule source sits beside its report)
        RExternalRuleSource resolved = report.withSourceId()
                .map(id -> generatorModel.workspace().resolve(id, RExternalRuleSource.class, report))
                .orElse(null);
        if (resolved == null) {
            throw SilentDegradation.refuse(SilentDegradation.Site.REPORT_REFERENCE_UNRESOLVED,
                    "report " + rFunction.name() + ": its `with source " + name + "` did not resolve in the linker (see its"
                            + " EXTERNAL_SOURCE_NOT_FOUND diagnostic); the generator does not guess a rule"
                            + " source by name", report);
        }
        return resolved;
    }

    // === The traversal core (PR #321, verbatim modulo path tracking) ========

    /**
     * Port of upstream {@code RuleReferenceService.traverse} (fold
     * parameterised; upstream's {@code List<RAttribute> path} tracking kept —
     * PR #321 dropped it because the as-label fold only needs the attribute,
     * PR #322's operations fold needs the full assign path).
     */
    private void traverseRuleReferences(RExternalRuleSource source, RDataType type,
                                        Map<List<String>, RuleResult> nestedRuleContext,
                                        List<RAttribute> path,
                                        Set<RDataType> visited,
                                        RuleFold fold) {
        boolean isCycle = !visited.add(type);
        if (isCycle && nestedRuleContext.isEmpty()) {
            return;
        }
        for (RAttribute attr : generatorModel.allAttributes(type)) {
            List<RAttribute> attrPath = new ArrayList<>(path);
            attrPath.add(attr);
            RuleResult ruleResult = nestedRuleContext.get(List.of(attr.name()));
            if (ruleResult != null) {
                fold.apply(attrPath, ruleResult);
            } else {
                RulePathMap pathMap = computeRulePathMapInContext(source, type, attr);
                ruleResult = pathMap.get(List.of());
                if (ruleResult != null) {
                    fold.apply(attrPath, ruleResult);
                } else if (!generatorModel.isMulti(attr)) {
                    RDataType attrType = unwrapToDataType(generatorModel.getType(attr));
                    if (attrType != null) {
                        Map<List<String>, RuleResult> subcontext =
                                subcontextForAttribute(attr, nestedRuleContext);
                        if (!isCycle) {
                            pathMap.addRulesToMapIfNotPresent(subcontext);
                        }
                        traverseRuleReferences(source, attrType, subcontext, attrPath, visited, fold);
                    }
                }
            }
        }
        // Deliberately UNCONDITIONAL (the #321 Copilot R1 finding, verified +
        // declined): upstream RuleReferenceService.traverse (9.83.0-line upstream L120 — the
        // porting target whose behaviour the goldens carry) removes without an
        // isCycle guard, so an isCycle frame (reachable only with a non-empty
        // nestedRuleContext) removes the ANCESTOR frame's entry. Termination is
        // still bounded — the nested context strictly shrinks on descent — and
        // any redundant re-traversal this permits is exactly upstream's, which
        // byte parity requires. Guarding on !isCycle would deviate.
        visited.remove(type);
    }

    /** Port of upstream {@code RuleReferenceService.getSubcontextForAttribute}. */
    private Map<List<String>, RuleResult> subcontextForAttribute(RAttribute attr,
                                                                 Map<List<String>, RuleResult> context) {
        String attributeName = attr.name();
        Map<List<String>, RuleResult> sub = new HashMap<>();
        for (Map.Entry<List<String>, RuleResult> e : context.entrySet()) {
            if (!e.getKey().isEmpty() && e.getKey().get(0).equals(attributeName)) {
                sub.put(e.getKey().subList(1, e.getKey().size()), e.getValue());
            }
        }
        return sub;
    }

    /**
     * Port of upstream {@code RuleReferenceService.computeRulePathMapInContext}
     * — the layered per-attribute rule map. Inside a rule source: parents are
     * the super type within the SAME source (highest priority), then the super
     * sources (or the no-source inline layer when the source has none).
     * Outside a source: the parent is the overridden parent attribute's map.
     * Own annotations shadow parents per-path; a rule-source {@code -} (minus)
     * explicit-empties every inherited path (or the attribute itself when
     * nothing is inherited); a {@code +} (plus) adds the external annotations.
     */
    private RulePathMap computeRulePathMapInContext(RExternalRuleSource source,
                                                    RDataType type, RAttribute attr) {
        RulePathMap parentInSameContext = null;
        List<RulePathMap> parentsInDescendingPriority = new ArrayList<>();
        if (source != null) {
            RDataType superType = type.superType().orElse(null);
            if (superType != null) {
                // Due to attribute overrides, the attribute in the super type
                // might be a different instance with the same name.
                RAttribute attrInSuperType = attributeByName(superType, attr.name());
                if (attrInSuperType != null) {
                    parentInSameContext = computeRulePathMapInContext(source, superType, attrInSuperType);
                }
            }
            if (source.superSources().isEmpty()) {
                parentsInDescendingPriority.add(computeRulePathMapInContext(null, type, attr));
            } else {
                for (RExternalRuleSource superSource : source.superSources()) {
                    parentsInDescendingPriority.add(computeRulePathMapInContext(superSource, type, attr));
                }
            }
        } else {
            RAttribute parentAttribute = parentAttributeOf(attr);
            if (parentAttribute != null && parentAttribute.parent() instanceof RDataType parentEnclosing) {
                parentInSameContext = computeRulePathMapInContext(null, parentEnclosing, parentAttribute);
            }
        }
        RulePathMap pathMap = new RulePathMap(parentInSameContext, parentsInDescendingPriority);
        if (source == null) {
            addRuleAnnotationsToMap(attr.ruleReferenceAnnotations(), pathMap);
        } else {
            for (RExternalRegularAttribute extAttr : findAttributesInSource(source, type, attr)) {
                if (!extAttr.isAddition()) {
                    // Minus: explicit-empty every inherited path (upstream
                    // RuleResult.explicitlyEmptyFromMinusInRuleSource).
                    RuleResult minusResult = new RuleResult(null, false);
                    Map<List<String>, RuleResult> existingRules = pathMap.getAsMap();
                    if (!existingRules.isEmpty()) {
                        for (List<String> path : existingRules.keySet()) {
                            pathMap.add(path, minusResult);
                        }
                    } else {
                        pathMap.add(List.of(), minusResult);
                    }
                } else {
                    addRuleAnnotationsToMap(extAttr.ruleRefs(), pathMap);
                }
            }
        }
        return pathMap;
    }

    /**
     * Port of upstream {@code RuleReferenceService.addRuleReferenceAnnotationsToMap}.
     * An UNRESOLVED named rule reference is skipped (upstream's
     * {@code eIsProxy} guard); an explicit {@code [ruleReference empty]}
     * (no rule name) registers an explicitly-empty result; a {@code for}
     * path containing a deep ({@code ->>}) segment is invalid for rule
     * references and skips the annotation.
     */
    private void addRuleAnnotationsToMap(List<RRuleReferenceAnnotation> annotations,
                                         RulePathMap pathMap) {
        for (RRuleReferenceAnnotation ann : annotations) {
            if (ann == null) continue;
            boolean explicitlyEmpty = ann.ruleName().isEmpty();
            RRule rule = ann.rule().orElse(null);
            if (!explicitlyEmpty && rule == null) {
                continue; // unresolved rule reference
            }
            List<String> path = toPathList(ann.forPath().orElse(null));
            if (path == null) {
                continue; // deep or unresolved path — invalid for rule refs
            }
            pathMap.add(path, new RuleResult(rule, ann.forPath().isEmpty()));
        }
    }

    /**
     * Port of upstream {@code RuleReferenceService.toList}: a rule-reference
     * {@code for} path as a list of attribute names — the named root is the
     * FIRST step (an ITEM root contributes nothing); a deep segment or an
     * unresolved name makes the whole path invalid ({@code null}).
     */
    private List<String> toPathList(RAnnotationPathExpression expr) {
        if (expr == null) {
            return List.of();
        }
        List<String> acc = new ArrayList<>();
        if (!expr.isRootItem()) {
            if (expr.root() == null) return null;
            acc.add(expr.root());
        }
        for (RAnnotationPathSegment segment : expr.segments()) {
            if (segment.isDeep() || segment.name() == null) return null;
            acc.add(segment.name());
        }
        return acc;
    }

    /**
     * Port of upstream {@code RuleReferenceService.findAttributesInSource}:
     * the external regular attributes attached to {@code attr} of
     * {@code type} inside {@code source}. Class match prefers the linker's
     * resolved type (identity), falling back to the simple-name match;
     * attribute match prefers the resolved attribute (identity), falling
     * back to the name match within the already-matched class.
     *
     * <p><b>Canonicality assumption (the PR #321 Seat-1 should-fix note):</b>
     * the identity comparisons assume the fork's linker resolves
     * {@code RExternalClass.referencedType()} / {@code resolvedAttribute()}
     * to the CANONICAL AST instances — the same objects the label graph
     * walks (upstream compares EObject identity the same way). If a resolved
     * reference is PRESENT but non-canonical, the identity test yields false
     * with no fallback (the name fallback only fires when the Optional is
     * EMPTY) — a silent under-match; and the name fallbacks themselves could
     * over-match same-simple-name types across namespaces within one source.
     * Both are corpus-inert today (the 6 with-source providers byte-flip,
     * proving exactness) and reachable only when {@code source != null}
     * (every with-source provider is a flipped ex-waiver, never green).
     */
    private List<RExternalRegularAttribute> findAttributesInSource(RExternalRuleSource source,
                                                                   RDataType type, RAttribute attr) {
        for (RExternalClass extClass : source.classes()) {
            boolean classMatches = extClass.referencedType()
                    .map(t -> t == type)
                    .orElseGet(() -> {
                        String tn = extClass.typeName();
                        if (tn == null) return false;
                        return tn.substring(tn.lastIndexOf('.') + 1).equals(type.name());
                    });
            if (!classMatches) continue;
            List<RExternalRegularAttribute> result = new ArrayList<>();
            for (RExternalRegularAttribute extAttr : extClass.attributes()) {
                boolean attrMatches = extAttr.resolvedAttribute()
                        .map(a -> a == attr)
                        .orElseGet(() -> attr.name().equals(extAttr.name()));
                if (attrMatches) {
                    result.add(extAttr);
                }
            }
            return result; // upstream findAny — at most one class entry per type
        }
        return List.of();
    }

    /**
     * The attribute named {@code name} on {@code type} including inherited
     * attributes (child override wins) — mirrors upstream
     * {@code RDataType.getAttributeByName} as used by the super-type layer of
     * {@code computeRulePathMapInContext}.
     */
    private RAttribute attributeByName(RDataType type, String name) {
        for (RAttribute candidate : generatorModel.allAttributes(type)) {
            if (name.equals(candidate.name())) {
                return candidate;
            }
        }
        return null;
    }

    // === Shared static helpers (other LabelProviderGenerator seats delegate) =

    /**
     * The overridden parent attribute: for an {@code override} attribute, the
     * first same-named own attribute found walking the enclosing type's
     * super-type chain. Mirrors upstream {@code RAttribute.getParentAttribute}
     * (gated on {@code isOverride}; returns {@code null} for a non-override
     * attribute, a detached attribute, or when no super type declares the
     * name). Static + shared: {@code LabelProviderGenerator}'s M2b
     * label-inheritance seat (PR #321) and {@code RJavaPojoInterface}'s
     * doc-reference-inheritance seat (PR #330 — public for the cross-package
     * caller) both resolve override parents through this one walk.
     */
    public static RAttribute parentAttributeOf(RAttribute attr) {
        if (!attr.isOverride()) {
            return null;
        }
        if (!(attr.parent() instanceof RDataType enclosing)) {
            return null;
        }
        RDataType current = enclosing.superType().orElse(null);
        while (current != null) {
            for (RAttribute candidate : current.attributes()) {
                if (attr.name().equals(candidate.name())) {
                    return candidate;
                }
            }
            current = current.superType().orElse(null);
        }
        return null;
    }

    /**
     * Unwrap a resolved {@link RType} to a {@link RDataType} via the fork's
     * {@link RDataTypeRef} / {@link RChoiceTypeRef} ref shape (mirrors upstream
     * {@code RChoiceType.asRDataType} + the implicit
     * {@code RDataType extends RDataType} identity). Returns {@code null} for
     * non-data types or when an {@code RChoiceTypeRef} lacks an AST anchor.
     */
    public static RDataType unwrapToDataType(RType type) {
        if (type instanceof RDataTypeRef ref) {
            return ref.astNode();
        }
        if (type instanceof RChoiceTypeRef ref) {
            return ref.asRDataType(); // may return null when astNode absent
        }
        return null;
    }

    // === RulePathMap (PR #321, verbatim) =====================================

    /** Port of upstream {@code rules.RulePathMap} (the layered path map). */
    private static final class RulePathMap {
        private final RulePathMap parentInContext;
        private final List<RulePathMap> parentsOutsideContext;
        private final Map<List<String>, RuleResult> map = new LinkedHashMap<>();

        RulePathMap(RulePathMap parentInContext, List<RulePathMap> parentsOutsideContextInDescendingPriority) {
            this.parentInContext = parentInContext;
            this.parentsOutsideContext = parentsOutsideContextInDescendingPriority;
        }

        void add(List<String> path, RuleResult ruleResult) {
            map.put(path, ruleResult);
        }

        RuleResult get(List<String> path) {
            RuleResult result = getInContext(path);
            if (result != null) {
                return result;
            }
            for (RulePathMap parent : parentsOutsideContext) {
                result = parent.get(path);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }

        private RuleResult getInContext(List<String> path) {
            RuleResult result = map.get(path);
            if (result != null) {
                return result;
            }
            return parentInContext != null ? parentInContext.getInContext(path) : null;
        }

        Map<List<String>, RuleResult> getAsMap() {
            Map<List<String>, RuleResult> result = new LinkedHashMap<>();
            addRulesToMapIfNotPresent(result);
            return result;
        }

        void addRulesToMapIfNotPresent(Map<List<String>, RuleResult> mapToAddTo) {
            addRulesInContextToMapIfNotPresent(mapToAddTo);
            for (RulePathMap parent : parentsOutsideContext) {
                parent.addRulesToMapIfNotPresent(mapToAddTo);
            }
        }

        private void addRulesInContextToMapIfNotPresent(Map<List<String>, RuleResult> mapToAddTo) {
            map.forEach(mapToAddTo::putIfAbsent);
            if (parentInContext != null) {
                parentInContext.addRulesInContextToMapIfNotPresent(mapToAddTo);
            }
        }
    }
}
