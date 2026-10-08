package com.regnosys.rosetta.generator.java.function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.regnosys.rosetta.ast.annotations.RAnnotationPathExpression;
import com.regnosys.rosetta.ast.annotations.RAnnotationPathSegment;
import com.regnosys.rosetta.ast.annotations.RLabelAnnotation;
import com.regnosys.rosetta.ast.external.RExternalRuleSource;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.regulatory.RReport;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.GeneratedIdentifier;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.JavaClassGenerator;
import com.regnosys.rosetta.generator.java.scoping.JavaClassScope;
import com.regnosys.rosetta.generator.java.scoping.JavaMethodScope;
import com.regnosys.rosetta.generator.java.template.JavaStringUtil;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.RGeneratedJavaClass;
import com.regnosys.rosetta.types.RChoiceTypeRef;
import com.regnosys.rosetta.types.RDataTypeRef;
import com.regnosys.rosetta.types.RType;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.model.lib.functions.LabelProvider;
import com.rosetta.util.DottedPath;

/**
 * Phase X T3 — port of upstream {@code LabelProviderGenerator.xtend}
 * ({@code rune-dsl-9.83.0-line upstream/rune-lang/.../generator/java/function/LabelProviderGenerator.xtend};
 * 251 LOC). Emits a Java class per {@link RFunction} (transform-annotated) or
 * {@link RReport} that extends
 * {@link com.regnosys.rosetta.lib.labelprovider.GraphBasedLabelProvider} and
 * wires a {@link com.regnosys.rosetta.lib.labelprovider.LabelNode} graph at
 * construction time, with one node per {@link RDataType} reachable from the
 * function output and one edge per attribute traversal.
 *
 * <p><b>Algorithm (verbatim from upstream {@code LabelProviderGenerator.generate}):</b>
 * <ol>
 *   <li>For {@link RReport}-origin functions, build an
 *       {@code RAttribute → RRule} map from the origin report's rule-reference
 *       annotations (legacy {@code as} branch only).</li>
 *   <li>Walk the output type recursively, registering one
 *       {@link LabelNode}-equivalent per {@link RDataType} encountered plus
 *       any label / legacy-rule labels per attribute.</li>
 *   <li>Prune nodes that cannot reach any label via the
 *       {@code nodesWithReachableLabels} fixed-point iteration.</li>
 *   <li>Emit a constructor that instantiates each retained node, wires the
 *       outgoing edges, and registers each label via {@code addLabel}.</li>
 * </ol>
 *
 * <p><b>Fork divergences vs upstream</b> — captured here once instead of at
 * every consumer site:
 * <ul>
 *   <li><b>No {@code RObjectFactory}.</b> Upstream's {@code streamObjects}
 *       converts each {@code RosettaReport} via
 *       {@code rObjectFactory.buildRFunction(report)}; the fork uses
 *       {@link RFunction#fromReport(RReport)} (added at Phase X T2).</li>
 *   <li><b>{@code RChoiceTypeRef} (fork) replaces upstream {@code RChoiceType}.</b>
 *       Unwrapped via {@link RChoiceTypeRef#asRDataType()} (added at T2;
 *       returns {@code null} when the underlying choice ref lacks an AST
 *       anchor — that branch short-circuits cleanly here).</li>
 *   <li><b>No {@code RAttribute.RMetaAnnotatedType().getRType()}.</b> Resolved
 *       attribute type goes through
 *       {@link GeneratorModel#getType(RAttribute)} which handles workspace
 *       lookup + builtin fallback + the Phase X T3 workspace-search recovery
 *       paths.</li>
 *   <li><b>No {@code RuleReferenceService.traverse(...)} with the
 *       {@code (origin, ruleSource, ...) → RAttribute→RRule} fold.</b>
 *       The fork's rule-reference data lives directly on
 *       {@link RAttribute#ruleReferenceAnnotations()}; the legacy-{@code as}
 *       branch reads each attribute's first rule-reference whose path is
 *       absent (matching upstream's {@code origin.path === null} guard in
 *       {@code LabelProviderGenerator.computeRuleReferences}). This is
 *       sufficient for the legacy {@code as}-label
 *       branch upstream populates the map for.</li>
 *   <li><b>No {@code AnnotationPathExpressionUtil.fold}.</b> The fork's
 *       {@link RAnnotationPathExpression} carries the root + a flat list of
 *       segments (shallow {@code ->} vs deep {@code ->>}); the leaf / deep
 *       branches in {@link #evaluateAnnotationPathExpression} are inlined.</li>
 *   <li><b>Java string-builder emission (Option A per the Phase X plan
 *       default).</b> Upstream's {@code StringConcatenationClient} ({@code '''...'''})
 *       is translated to plain Java string-builder; the
 *       {@code GraphBasedLabelProvider} / {@code LabelNode} runtime classes
 *       are referenced by simple name (imports declared once at the top of
 *       the emitted file).</li>
 *   <li><b>{@code StringEscapeUtils.escapeJava} (Apache commons-text)
 *       replaced by</b> {@link JavaStringUtil#escapeJava(String)} — the fork's
 *       parity-equivalent helper (handles {@code "}, {@code \\}, {@code \\n},
 *       {@code \\r}, {@code \\t}).</li>
 *   <li><b>No upstream {@code @Inject} DI.</b> Constructor-injected by every
 *       wiring site (see {@code ChoiceObjectGenerator} for the same pattern).</li>
 * </ul>
 *
 * <p>Wired into {@code JavaCodeGenerator}'s per-model generator list at Phase X
 * T7 (deferred until Rule + Report generators land at T5/T6); this class is
 * complete on its own.
 */
public class LabelProviderGenerator extends JavaClassGenerator<RFunction, RGeneratedJavaClass<? extends LabelProvider>> {

    private final GeneratorModel generatorModel;
    private final JavaTypeTranslator typeTranslator;
    private final DeepFeatureCallUtil deepPathUtil;
    private final LabelProviderGeneratorUtil util;

    /**
     * Construct with the four dependencies upstream injects via Guice.
     *
     * @param generatorModel the per-workspace generator-model bridge (replaces
     *        upstream {@code RObjectFactory} + {@code RosettaTypeProvider} —
     *        used for attribute-type resolution + symbol-id lookup for
     *        {@link JavaTypeTranslator})
     * @param typeTranslator the Java-class translator (used to derive the
     *        label-provider class name via
     *        {@link JavaTypeTranslator#toLabelProviderJavaClass} — added at T2.5)
     * @param deepPathUtil the deep-feature-call util (port from upstream
     *        added at T2; used to expand deep paths in
     *        {@code AnnotationPathExpression})
     * @param util the small {@link LabelProviderGeneratorUtil} helper that
     *        decides which functions are eligible for label-provider emission
     */
    public LabelProviderGenerator(GeneratorModel generatorModel,
                                  JavaTypeTranslator typeTranslator,
                                  DeepFeatureCallUtil deepPathUtil,
                                  LabelProviderGeneratorUtil util) {
        this.generatorModel = Objects.requireNonNull(generatorModel, "generatorModel");
        this.typeTranslator = Objects.requireNonNull(typeTranslator, "typeTranslator");
        this.deepPathUtil = Objects.requireNonNull(deepPathUtil, "deepPathUtil");
        this.util = Objects.requireNonNull(util, "util");
    }

    /**
     * Stream {@link RFunction} candidates from the model. Mirrors upstream
     * {@code LabelProviderGenerator.streamObjects}: for each root element, emit when (a) it is a
     * {@link RFunction} with {@link LabelProviderGeneratorUtil#shouldGenerateLabelProvider}
     * true, or (b) it is an {@link RReport} (always emitted; bridged via
     * {@link RFunction#fromReport(RReport)}). RRules ({@link RRule}) are NOT
     * streamed here per upstream (they are RuleGenerator's responsibility).
     */
    @Override
    protected Stream<? extends RFunction> streamObjects(RModel model) {
        return model.rootElements().stream().map(e -> {
            if (e instanceof RFunction f) {
                return util.shouldGenerateLabelProvider(f) ? f : null;
            }
            if (e instanceof RReport r) {
                return RFunction.fromReport(r);
            }
            return null;
        }).filter(Objects::nonNull);
    }

    /**
     * Compute the generated {@code <FuncName>LabelProvider} class via
     * {@link JavaTypeTranslator#toLabelProviderJavaClass(ModelSymbolId)}
     * (added at T2.5 — the fork's accessor takes the resolved
     * {@link ModelSymbolId} rather than the {@link RFunction} directly per
     * the fork's JavaTypeTranslator convention).
     *
     * <p><b>Synthetic-bridge handling (post-T5):</b>
     * {@link GeneratorModel#symbolId(RFunction)} carries the canonical
     * synthetic-bridge recovery — when {@code function} is a synthetic
     * produced by {@link RFunction#fromReport(RReport)} or
     * {@link RFunction#fromRule(com.regnosys.rosetta.ast.functions.RRule)},
     * it has no {@link RModel} parent, and {@code symbolId} reads the
     * namespace off the {@linkplain RFunction#originReport() originReport} /
     * {@linkplain RFunction#originRule() originRule} back-pointer. The
     * T3 path through this method previously carried an inline
     * recovery; T5 promoted the recovery to {@link GeneratorModel} so all
     * downstream callers share the same canonical path, and this method
     * delegates directly.
     *
     * <p><b>Fail-fast on missing namespace:</b> when the synthetic carries
     * neither an origin back-pointer nor an attached parent,
     * {@code generatorModel.symbolId(function)} falls through to
     * {@link GeneratorModel#namespace(com.regnosys.rosetta.ast.RRootElement)}
     * which throws {@link IllegalStateException} naming the failure surface
     * — an upstream RReport without an attached RModel parent (and without
     * the {@link RFunction#fromReport(RReport)} back-pointer) is a
     * workspace-construction bug rather than a recoverable runtime state.
     */
    @Override
    protected RGeneratedJavaClass<? extends LabelProvider> createTypeRepresentation(RFunction function) {
        return typeTranslator.toLabelProviderJavaClass(generatorModel.symbolId(function));
    }

    /**
     * Emit the label-provider class. See class-level Javadoc for algorithm
     * overview.
     */
    @Override
    protected String generate(RFunction function,
                              RGeneratedJavaClass<? extends LabelProvider> labelClass,
                              String version) {
        // v3.2 seat 4 (PR #625, F7): a report-origin function whose `with type` did not resolve is
        // REFUSED here as at the report seat (RuleReferenceTraversal.requireResolvedReportType, the ONE
        // declaration - LAW 69); before the seat the start node came back null and an EMPTY provider
        // was emitted in silence
        function.originReport().ifPresent(report -> RuleReferenceTraversal.requireResolvedReportType(function, report));
        // 1. RReport-origin functions populate an RAttribute → RRule map for
        //    the legacy `as`-label branch. Plain functions yield an empty map.
        Map<RAttribute, RRule> attributeToRuleMap = buildAttributeToRuleMap(function);

        // 2. Build graph + prune. The graph rooted at the output type may be
        //    empty when the output type does not unwrap to a data type
        //    (basic/enum/missing/etc.) — empty graph is a legal outcome (the
        //    emitted class still extends GraphBasedLabelProvider; no addLabel /
        //    addOutgoingEdge calls).
        Map<RDataType, Map<DottedPath, String>> labelsPerNode = new LinkedHashMap<>();
        Map<RDataType, Map<String, RDataType>> edgesPerNode = new LinkedHashMap<>();
        RDataType startNode = resolveStartNode(function);
        if (startNode != null) {
            buildLabelGraph(startNode, labelsPerNode, edgesPerNode, attributeToRuleMap);
            pruneLabelGraph(labelsPerNode, edgesPerNode);
        }

        // 3. Per-node identifier registration via the JavaClassScope hierarchy.
        //    Upstream registers the start node as "startNode" and every other
        //    node as "<typeName-first-lower>Node"; collisions (two attributes
        //    yielding the same typeName-based identifier) are resolved by
        //    GeneratorScope's escapeName ("_" prefix) — see
        //    JavaClassScope#escapeName (the fork's identifier-collision strategy
        //    inherited from AbstractJavaScope / GeneratorScope).
        JavaClassScope scope = JavaClassScope.createAndRegisterIdentifier(labelClass);
        JavaMethodScope constructorScope = scope.createMethodScope("constructor");
        Map<RDataType, GeneratedIdentifier> nodeVar = new LinkedHashMap<>();
        if (startNode != null && labelsPerNode.containsKey(startNode)) {
            nodeVar.put(startNode, constructorScope.createIdentifier(startNode, "startNode"));
        }
        for (RDataType node : labelsPerNode.keySet()) {
            if (node.equals(startNode)) continue;
            String desired = lowerFirst(node.name()) + "Node";
            nodeVar.put(node, constructorScope.createIdentifier(node, desired));
        }

        // 4. Emit class — Option A inline Java string-builder (per plan default).
        return emitClass(labelClass, labelsPerNode, edgesPerNode, startNode, nodeVar);
    }

    // === Emission (verbatim translation of upstream LabelProviderGenerator.generate emission body) ===========

    private String emitClass(RGeneratedJavaClass<? extends LabelProvider> labelClass,
                             Map<RDataType, Map<DottedPath, String>> labelsPerNode,
                             Map<RDataType, Map<String, RDataType>> edgesPerNode,
                             RDataType startNode,
                             Map<RDataType, GeneratedIdentifier> nodeVar) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(labelClass.getPackageName().withDots()).append(";\n\n");
        // Import block mirrors upstream's Xtext import-manager output
        // (LabelProviderGenerator.xtend:95-126): the three candidate types are
        // emitted in FQN-sorted order (com.regnosys… before java.util…) and ONLY
        // when actually referenced. java.util.Arrays is referenced solely by
        // representAsList(), i.e. only when at least one node contributes a label;
        // a label-free provider (e.g. a projection-only LabelProvider with an
        // empty graph) therefore omits both the Arrays.asList(...) call and its
        // import. The fork previously emitted Arrays unconditionally and first,
        // diverging from every golden — byte-matching no LabelProvider.
        boolean usesArrays = labelsPerNode.values().stream()
                .anyMatch(labels -> labels != null && !labels.isEmpty());
        sb.append("import com.regnosys.rosetta.lib.labelprovider.GraphBasedLabelProvider;\n");
        sb.append("import com.regnosys.rosetta.lib.labelprovider.LabelNode;\n");
        if (usesArrays) {
            sb.append("import java.util.Arrays;\n");
        }
        sb.append("\n\n");
        sb.append("public class ").append(labelClass.getSimpleName())
          .append(" extends GraphBasedLabelProvider {\n");
        sb.append("\tpublic ").append(labelClass.getSimpleName()).append("() {\n");
        // The super() call passes a fresh `new LabelNode()` to the
        // GraphBasedLabelProvider base class as the start-node. Upstream emits
        // exactly the same form: the start-node is anonymous in the super()
        // call because it precedes any local variable declarations in the
        // constructor body, so there is no scope-registered identifier that
        // could collide with it. Per-attribute LabelNode locals declared
        // below DO go through scope.createIdentifier(...) for collision-safe
        // naming; the start-node sits outside that namespace by construction.
        sb.append("\t\tsuper(new LabelNode());\n");
        // Upstream emits an unconditional blank line (carrying only the template's
        // two-tab indentation) between the super(...) call and the per-node
        // label/edge blocks (LabelProviderGenerator.xtend:98). Reproduce it for
        // byte parity — the fork previously went straight to the node loop.
        sb.append("\t\t\n");

        // Per-node LabelNode declarations + addLabel calls (upstream
        // LabelProviderGenerator.generate node-decl emission body).
        for (RDataType node : labelsPerNode.keySet()) {
            GeneratedIdentifier id = nodeVar.get(node);
            if (id == null) continue; // defensive — pruning should keep map consistent
            String var = id.getActualName();
            Map<DottedPath, String> labels = labelsPerNode.get(node);
            if (!node.equals(startNode)) {
                // The separator blank line carries the template's two-tab
                // indentation (upstream's Xtend blank line inside the node FOR
                // loop — same law as the post-super() blank above). The fork
                // previously emitted a bare empty line, which diverged on every
                // multi-node provider (PR #321; all multi-node providers are
                // waivered drr files, every green provider is single-block).
                sb.append("\t\t\n\t\tLabelNode ").append(var).append(" = new LabelNode();\n");
            }
            for (Map.Entry<DottedPath, String> entry : labels.entrySet()) {
                sb.append("\t\t").append(var).append(".addLabel(")
                  .append(representAsList(entry.getKey()))
                  .append(", \"").append(JavaStringUtil.escapeJava(entry.getValue()))
                  .append("\");\n");
            }
        }

        // Per-node addOutgoingEdge calls (upstream
        // LabelProviderGenerator.generate edge-wiring emission body).
        for (RDataType node : edgesPerNode.keySet()) {
            GeneratedIdentifier srcId = nodeVar.get(node);
            if (srcId == null) continue;
            Map<String, RDataType> edges = edgesPerNode.get(node);
            if (edges.isEmpty()) continue;
            String srcVar = srcId.getActualName();
            // Two-tab-indented separator blank — same template law as the
            // node-declaration separator above (upstream's blank line inside
            // the `IF !edges.empty` template block).
            sb.append("\t\t\n");
            for (Map.Entry<String, RDataType> edge : edges.entrySet()) {
                GeneratedIdentifier tgtId = nodeVar.get(edge.getValue());
                if (tgtId == null) continue;
                sb.append("\t\t").append(srcVar).append(".addOutgoingEdge(\"")
                  .append(JavaStringUtil.escapeJava(edge.getKey()))
                  .append("\", ").append(tgtId.getActualName()).append(");\n");
            }
        }

        sb.append("\t}\n");
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Render a {@link DottedPath} as {@code Arrays.asList("seg1", "seg2", ...)}.
     * Verbatim port of upstream {@code LabelProviderGenerator.representAsList}
     * (the {@code StringConcatenationClient}).
     */
    // package-private for test access (LabelProviderGeneratorTest.representAsList_*)
    String representAsList(DottedPath path) {
        StringBuilder sb = new StringBuilder("Arrays.asList(");
        boolean first = true;
        for (String segment : (Iterable<String>) path.stream()::iterator) {
            if (!first) sb.append(", ");
            sb.append('"').append(JavaStringUtil.escapeJava(segment)).append('"');
            first = false;
        }
        return sb.append(')').toString();
    }

    // === Graph build (verbatim port of upstream LabelProviderGenerator.buildLabelGraph) ===============

    /**
     * Recursively walk the data-type graph from {@code currentNode}, registering
     * one entry per visited node in {@code labelsPerNode} + {@code edgesPerNode}.
     * Circular references terminate via the
     * {@code labelsPerNode.containsKey(currentNode)} short-circuit at the top.
     */
    /**
     * <b>Choice-typed attribute traversal note (T3.0.5 B1 verification):</b>
     * {@link #unwrapToDataType(RType)} for an {@link RChoiceTypeRef} delegates
     * to {@link RChoiceTypeRef#asRDataType()}, which synthesises a fresh
     * {@link RDataType} whose attributes carry the choice option's typeCall
     * <em>reference</em> (not a copy). The recursion below then calls
     * {@code generatorModel.getType(syntheticAttr)} →
     * {@code generatorModel.resolveTypeCall(syntheticAttr.typeCall())} on
     * those synthetic attributes; because the typeCall reference is shared
     * with the original (post-link workspace-resolved) typeCall, the option
     * types resolve through the standard workspace lookup and the recursion
     * proceeds into each option's data type with its labels intact. The
     * {@code choiceTypedAttribute_propagatesOptionLabels} test in
     * {@code LabelProviderGeneratorTest} guards this invariant.
     */
    private void buildLabelGraph(RDataType currentNode,
                                 Map<RDataType, Map<DottedPath, String>> labelsPerNode,
                                 Map<RDataType, Map<String, RDataType>> edgesPerNode,
                                 Map<RAttribute, RRule> attributeToRuleMap) {
        if (labelsPerNode.containsKey(currentNode)) {
            // Circular reference: this node already processed.
            return;
        }
        Map<DottedPath, String> labels = new LinkedHashMap<>();
        labelsPerNode.put(currentNode, labels);
        Map<String, RDataType> edges = new LinkedHashMap<>();
        edgesPerNode.put(currentNode, edges);

        for (RAttribute attr : generatorModel.allAttributes(currentNode)) {
            DottedPath attrPath = DottedPath.of(attr.name());

            // 1. Register labels on the type of this attribute (upstream
            //    LabelProviderGenerator.buildLabelGraph attribute-type branch).
            RDataType t = RuleReferenceTraversal.unwrapToDataType(generatorModel.getType(attr));
            if (t != null) {
                edges.put(attr.name(), t);
                buildLabelGraph(t, labelsPerNode, edgesPerNode, attributeToRuleMap);
            }

            // 2. Register legacy `as` annotations from rule references (upstream
            //    LabelProviderGenerator.buildLabelGraph rule-reference branch).
            RRule ruleRef = attributeToRuleMap.get(attr);
            if (ruleRef != null) {
                registerLegacyRuleAsLabel(ruleRef, attrPath, labels);
            }

            // 3. Register label annotations (upstream
            //    LabelProviderGenerator.buildLabelGraph label-annotation branch;
            //    upstream reads attr.allLabelAnnotations — own annotations PLUS
            //    those inherited from the overridden parent attribute, parent's
            //    FIRST — see allLabelAnnotations below).
            for (RLabelAnnotation ann : allLabelAnnotations(attr)) {
                registerLabelAnnotation(ann, attr, attrPath, labels);
            }
        }
    }

    // === Prune (verbatim port of upstream LabelProviderGenerator.pruneLabelGraph) =====================

    /**
     * Remove nodes that cannot reach any label. A node is retained iff (a) it
     * has a non-empty {@code labels} map of its own, OR (b) one of its
     * outgoing edges leads (transitively) to a retained node. Fixed-point
     * iteration until no further additions to {@code nodesWithReachableLabels}.
     */
    private void pruneLabelGraph(Map<RDataType, Map<DottedPath, String>> labelsPerNode,
                                 Map<RDataType, Map<String, RDataType>> edgesPerNode) {
        Set<RDataType> nodes = new HashSet<>(labelsPerNode.keySet());

        Set<RDataType> nodesWithReachableLabels = new LinkedHashSet<>();
        for (RDataType n : nodes) {
            if (!labelsPerNode.get(n).isEmpty()) {
                nodesWithReachableLabels.add(n);
            }
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (RDataType node : nodes) {
                if (nodesWithReachableLabels.contains(node)) continue;
                boolean reaches = false;
                for (RDataType target : edgesPerNode.get(node).values()) {
                    if (nodesWithReachableLabels.contains(target)) {
                        reaches = true;
                        break;
                    }
                }
                if (reaches) {
                    nodesWithReachableLabels.add(node);
                    changed = true;
                }
            }
        }

        // Prune unreachable nodes + any edges pointing at them (upstream
        // LabelProviderGenerator.pruneLabelGraph unreachable-removal body).
        for (RDataType node : nodes) {
            if (nodesWithReachableLabels.contains(node)) continue;
            labelsPerNode.remove(node);
            edgesPerNode.remove(node);
            for (Map<String, RDataType> edges : edgesPerNode.values()) {
                edges.entrySet().removeIf(e -> Objects.equals(e.getValue(), node));
            }
        }
    }

    // === Label registration (verbatim port of upstream LabelProviderGenerator.registerLabel) ========

    /**
     * Register a label annotation. Upstream {@code LabelProviderGenerator.registerLabel}:
     * evaluates the annotation's path expression to a list of {@link DottedPath}s and stores
     * the label under each. The fork's {@link RLabelAnnotation} carries
     * separate {@code forPath} (the path attached to the annotation header)
     * + {@code asPath} (the {@code as}-clause path); upstream's
     * {@code ann.path} is the legacy {@code forPath}.
     */
    private void registerLabelAnnotation(RLabelAnnotation ann,
                                         RAttribute hostAttribute,
                                         DottedPath attrPath,
                                         Map<DottedPath, String> labels) {
        Optional<RAnnotationPathExpression> forPath = ann.forPath();
        List<DottedPath> paths = evaluateAnnotationPathExpression(
                hostAttribute, attrPath, forPath.orElse(null));
        for (DottedPath p : paths) {
            labels.put(p, ann.label());
        }
    }

    /**
     * Register a legacy {@code as}-style rule reference: the rule's
     * {@code as "..."} clause text becomes the label for the attribute's own
     * path. Verbatim port of upstream
     * {@code LabelProviderGenerator.registerLegacyRuleAsLabel} (9.83.0/9.83.0-line upstream:
     * {@code if (rule.identifier !== null) labels.put(attrPath, rule.identifier)}
     * — upstream's {@code RosettaRule.identifier} IS the {@code as} clause
     * string; the fork's {@link RRule} carries it as {@link RRule#alias()}).
     * A rule WITHOUT an {@code as} clause contributes no label (PR #321 —
     * the fork previously emitted {@code rule.name()} for every mapped rule,
     * which never matched any golden: golden labels are the human-readable
     * {@code as} texts, e.g. {@code "10 Unique Transaction Identifier (UTI)"},
     * and golden emits nothing when the clause is absent, e.g. the ESMA/FCA
     * margin {@code MICCollateral} rule).
     */
    private void registerLegacyRuleAsLabel(RRule rule, DottedPath attrPath,
                                           Map<DottedPath, String> labels) {
        rule.alias().ifPresent(alias -> labels.put(attrPath, alias));
    }

    // === Annotation path evaluation (port of upstream LabelProviderGenerator.evalAnnotationPathExpression) =========

    /**
     * Evaluate an annotation-path expression to the list of dotted paths it
     * resolves to. Three branches:
     * <ul>
     *   <li>{@code expr} is {@code null} → the root path only
     *       (upstream {@code evalAnnotationPathExpression} null-branch).</li>
     *   <li>Shallow segment ({@code ->} arrow) → append the segment name to
     *       the current path(s) (upstream
     *       {@code AnnotationPathExpressionUtil.fold} shallow-folder).</li>
     *   <li>Deep segment ({@code ->>} arrow) → expand via
     *       {@link DeepFeatureCallUtil#findDeepFeaturePaths} to all deep paths
     *       that resolve to the deep segment's attribute (upstream
     *       {@code evalAnnotationPathExpression} deep-arrow branch).</li>
     * </ul>
     *
     * <p>The fork's {@link RAnnotationPathExpression} is a flat
     * {@code root + List<RAnnotationPathSegment>} structure (each segment
     * carries its own deep/shallow flag), unlike upstream's recursive fold
     * AST. We evaluate left-to-right.
     */
    private List<DottedPath> evaluateAnnotationPathExpression(RAttribute hostAttribute,
                                                              DottedPath root,
                                                              RAnnotationPathExpression expr) {
        if (expr == null) {
            return List.of(root);
        }
        // The fork's RAnnotationPathExpression encodes the root separately
        // from the segments (validID OR ITEM keyword). A NAMED root is a
        // FIRST PATH STEP relative to the annotation host's type — upstream's
        // fold seeds a named attribute-reference root with
        // `root.child(a.name)` (LabelProviderGenerator.xtend
        // evaluateAnnotationPathExpression, first fold lambda) while an
        // ITEM root seeds the bare `root` (second lambda). E.g.
        // `[label for periodicPayment -> fixedRateDayCountConvention "..."]`
        // on the `leg1` attribute registers at
        // leg1.periodicPayment.fixedRateDayCountConvention — `periodicPayment`
        // names a CHILD attribute of leg1's type, NOT the host. (PR #321 —
        // the fork previously dropped the named root, emitting
        // leg1.fixedRateDayCountConvention, which never matched any golden;
        // green files carry no named-root labels, so the seed change fires
        // only on already-divergent files.)
        List<DottedPath> current = new ArrayList<>();
        if (!expr.isRootItem() && expr.root() != null) {
            current.add(root.child(expr.root()));
        } else {
            current.add(root);
        }

        for (RAnnotationPathSegment segment : expr.segments()) {
            String segName = segment.name();
            if (segName == null) continue;
            if (!segment.isDeep()) {
                // Shallow: append segment name to every current path.
                List<DottedPath> next = new ArrayList<>(current.size());
                for (DottedPath p : current) {
                    next.add(p.child(segName));
                }
                current = next;
            } else {
                // Deep: resolve via DeepFeatureCallUtil. The receiver type is
                // the type of the attribute identified by the previous
                // segment (or the root attribute if this is the first
                // segment). Use the segment's resolved attribute to drive
                // the deep-path expansion.
                RAttribute deepAttr = segment.resolvedAttribute().orElse(null);
                if (deepAttr == null) {
                    // No resolved attribute → cannot expand deep path.
                    // Mirrors upstream's behaviour when the deep path's
                    // attribute fails to resolve (returns no paths).
                    return List.of();
                }
                RType receiverType = receiverTypeForDeepSegment(
                        hostAttribute, expr, segment, deepAttr);
                if (receiverType == null) {
                    return List.of();
                }
                List<List<RAttribute>> deepPaths =
                        deepPathUtil.findDeepFeaturePaths(receiverType, deepAttr);
                if (deepPaths.isEmpty()) {
                    return List.of();
                }
                List<DottedPath> next = new ArrayList<>();
                for (DottedPath p : current) {
                    for (List<RAttribute> deepPath : deepPaths) {
                        DottedPath acc = p;
                        for (RAttribute step : deepPath) {
                            acc = acc.child(step.name());
                        }
                        next.add(acc);
                    }
                }
                current = next;
            }
        }
        return current;
    }

    /**
     * Compute the receiver type for a deep-arrow segment — the type of the
     * attribute the deep segment expands within. For the first segment, this
     * is the type of the attribute the annotation is attached to (read by
     * looking up the deep segment's resolved attribute's parent type via
     * the workspace); for subsequent segments, it's the type of the previous
     * shallow/deep step's resolved attribute.
     *
     * <p>The minimal correct approach: use the deep segment's own resolved
     * attribute as the deep-feature target, and the type of the preceding
     * step (or the annotation host's attribute type) as the receiver. The
     * fork's {@link RAnnotationPathSegment} carries a back-pointer to its
     * {@code resolvedAttribute()}; the parent type can be inferred from the
     * preceding segment's {@code resolvedAttribute()}'s declared type.
     *
     * <p><b>Segment identity:</b> segments are walked by inspecting
     * {@code expr.segments()} and stopping at the supplied {@code segment}
     * marker. The fork's {@link RAnnotationPathExpression#segments()} returns
     * the backing list per the {@link com.regnosys.rosetta.ast.RNode} contract
     * (no defensive copies — segments are reference-stable RNode children),
     * which makes {@code ==} safe on this path. We nevertheless use
     * {@link Objects#equals(Object, Object)} below to be defensive against any
     * future refactor that introduces defensive copying of the segments list.
     *
     * <p><b>Host-attribute fallback (Phase X T3.0.5):</b> when the deep
     * segment is the first segment in the expression, the receiver is the
     * type of the host attribute (the attribute the annotation is attached
     * to). This matches upstream's behaviour for the {@code item ->> deep}
     * idiom — {@code item} refers to the annotation host, so the deep arrow
     * walks within the host's resolved type. We accept {@code hostAttribute}
     * by parameter rather than synthesise it from the expression because
     * the {@link RAnnotationPathExpression} does not carry a back-pointer to
     * its owning attribute on the fork's AST.
     */
    private RType receiverTypeForDeepSegment(RAttribute hostAttribute,
                                             RAnnotationPathExpression expr,
                                             RAnnotationPathSegment segment,
                                             RAttribute deepAttr) {
        // Find the segment immediately before {@code segment}; its resolved
        // attribute's type is the receiver. When the deep segment is the
        // first segment, fall back to the HOST attribute's type (the type the
        // annotation is on top of). This matches upstream's behaviour where
        // the receiver of {@code item ->> deep} is the type of the attribute
        // {@code item} refers to (the annotation host).
        // LATENT (corpus-inert, PR #321): for a NAMED root followed by a
        // first-segment deep arrow ({@code for someAttr ->> deep}), upstream's
        // receiver would be the ROOT attribute's type, not the host's — but
        // ZERO label annotations in the corpus combine a named root with a
        // deep arrow (plain-grep over every cell's rosetta sources), so the
        // host-type fallback is unreachable for that shape today. Revisit if
        // a deep-rooted label ever appears.
        RAttribute previous = null;
        for (RAnnotationPathSegment s : expr.segments()) {
            if (Objects.equals(s, segment)) break;
            previous = s.resolvedAttribute().orElse(previous);
        }
        if (previous != null) {
            return generatorModel.getType(previous);
        }
        // No prior segment — receiver is the host attribute's resolved type.
        // When that type is non-data (e.g. an annotation on a primitive-typed
        // attribute with a deep arrow), findDeepFeaturePaths returns an
        // empty list (mirrors upstream behaviour — no deep paths to expand).
        return generatorModel.getType(hostAttribute);
    }

    // === Label-annotation inheritance (port of upstream RAttribute.getAllLabelAnnotations) ===================

    /**
     * All label annotations of an attribute — its own plus those inherited
     * from the parent attribute it overrides (transitively), parent's
     * annotations FIRST. Verbatim port of upstream
     * {@code RAttribute.getAllLabelAnnotations} /
     * {@code inheritAnnotationsFromParent} (PR #321 — the fork previously
     * read own annotations only, dropping e.g. the CFTC
     * {@code override nonReportable} labels inherited from the common
     * {@code CommonTransactionReport.nonReportable} attribute).
     */
    private List<RLabelAnnotation> allLabelAnnotations(RAttribute attr) {
        List<RLabelAnnotation> own = attr.labelAnnotations();
        RAttribute parent = RuleReferenceTraversal.parentAttributeOf(attr);
        if (parent == null) {
            return own;
        }
        List<RLabelAnnotation> parentAll = allLabelAnnotations(parent);
        if (parentAll.isEmpty()) {
            return own;
        }
        List<RLabelAnnotation> all = new ArrayList<>(parentAll.size() + own.size());
        all.addAll(parentAll);
        all.addAll(own);
        return all;
    }

    // === Attribute → rule map (the as-label fold over the shared traversal, PR #321/#322) ===

    /**
     * Build the {@code RAttribute → RRule} map populated upstream by
     * {@code ruleService.traverse} for {@link RReport}-origin functions. The
     * traversal itself (a faithful port of upstream
     * {@code RuleReferenceService.traverse} + {@code computeRulePathMapInContext}
     * + {@code RulePathMap} + {@code RuleResult}, built HERE at PR #321) was
     * PROMOTED to the shared {@link RuleReferenceTraversal} at PR #322 — the
     * report-operations synthesis in {@code ReportGenerator} folds over the
     * SAME traversal (upstream {@code RObjectFactory.generateOperations} uses
     * the same {@code ruleService.traverse}); this method contributes only
     * upstream {@code LabelProviderGenerator}'s as-label fold body
     * ({@link #recordAsLabelRule}) — the last path element is the associated
     * attribute, exactly the attribute the PR #321 in-place fold recorded.
     */
    private Map<RAttribute, RRule> buildAttributeToRuleMap(RFunction function) {
        Map<RAttribute, RRule> result = new HashMap<>();
        Optional<RReport> originReport = function.originReport();
        if (originReport.isEmpty()) {
            return result;
        }
        RDataType start = resolveStartNode(function);
        if (start == null) return result;
        RuleReferenceTraversal traversal = new RuleReferenceTraversal(generatorModel);
        RExternalRuleSource source = traversal.resolveRuleSource(function, originReport.get());
        traversal.traverse(source, start,
                (path, ruleResult) -> recordAsLabelRule(result, path.get(path.size() - 1), ruleResult));
        return result;
    }

    /**
     * The upstream {@code LabelProviderGenerator} fold body: record the rule
     * for the as-label branch iff the result carries a non-empty rule reached
     * through a {@code RuleReferenceAnnotation} WITHOUT a {@code for} path
     * (upstream {@code origin.path === null}) whose rule has an {@code as}
     * identifier ({@link RRule#alias()} on the fork).
     */
    private void recordAsLabelRule(Map<RAttribute, RRule> out, RAttribute attr,
                                   RuleReferenceTraversal.RuleResult result) {
        if (result.rule() != null && result.originPathless() && result.rule().alias().isPresent()) {
            out.put(attr, result.rule());
        }
    }

    // === Helpers =============================================================

    /**
     * Resolve the function's output type to a {@link RDataType} — unwrapping
     * {@link RChoiceTypeRef} per upstream {@code LabelProviderGenerator.resolveStartNode}. Returns
     * {@code null} when the output is missing, missing-typed, or unwraps to a
     * non-data type (basic/enum/etc.) — empty label graph is the correct
     * outcome in that case.
     */
    // package-private for test access (LabelProviderGeneratorTest can drive
    // resolveStartNode without going through the full generate() pipeline)
    RDataType resolveStartNode(RFunction function) {
        RAttribute output = function.output().orElse(null);
        if (output == null) return null;
        return RuleReferenceTraversal.unwrapToDataType(generatorModel.getType(output));
    }

    /**
     * Lowercase the first character of a non-empty string. Used to derive
     * node-variable names (upstream {@code LabelProviderGenerator.generate}
     * node-variable derivation: {@code node.name.toFirstLower + "Node"}).
     */
    private static String lowerFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        // Use Character.toLowerCase to avoid the Turkish-locale dotted/dotless-i
        // pitfall that String.toLowerCase() would introduce; Character version is
        // locale-independent, matching upstream's Xtend toFirstLower semantics.
        if (s.length() == 1) return String.valueOf(Character.toLowerCase(s.charAt(0)));
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
