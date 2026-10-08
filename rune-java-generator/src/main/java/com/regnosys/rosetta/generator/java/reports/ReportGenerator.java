package com.regnosys.rosetta.generator.java.reports;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.regnosys.rosetta.ast.enums.OperationOp;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.external.RExternalRuleSource;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.ROperation;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.functions.RSegment;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.regulatory.RRegulatoryDocumentReference;
import com.regnosys.rosetta.ast.regulatory.RReport;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.JavaClassGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.RuleReferenceTraversal;
import com.regnosys.rosetta.generator.java.scoping.JavaClassScope;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.RGeneratedJavaClass;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.model.lib.annotations.RosettaReport;
import com.rosetta.model.lib.annotations.RuneLabelProvider;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.reports.ReportFunction;
import com.rosetta.util.types.JavaGenericTypeDeclaration;
import com.rosetta.util.types.JavaParameterizedType;
import com.rosetta.util.types.JavaType;
import com.rosetta.util.types.JavaTypeArgument;

/**
 * Phase X T6 — port of upstream {@code ReportGenerator.xtend}
 * ({@code rune-dsl-9.83.0-line upstream/rune-lang/.../generator/java/reports/ReportGenerator.xtend};
 * 45 LOC). Sibling to {@link RuleGenerator} (T5) under the same
 * upstream-aligned {@code reports} package.
 *
 * <p>Thin shim that filters {@link RReport} root elements from each
 * {@link RModel}, bridges each to a synthetic {@link RFunction} via
 * {@link RFunction#fromReport(RReport)} (T2), and delegates the emission to
 * {@link FunctionGenerator#buildClassWithBaseInterface} (T4) with
 * {@code ReportFunction<I,O>} as the base interface plus TWO class-level
 * annotations:
 * <ul>
 *   <li>{@code @RosettaReport(namespace="<ns>", body="<body>", corpusList={...})}
 *       — the regulatory metadata used by downstream consumers for
 *       report-result classification.</li>
 *   <li>{@code @RuneLabelProvider(labelProvider=<X>LabelProvider.class)} —
 *       points at the per-report label provider class emitted by
 *       {@code LabelProviderGenerator} (T3).</li>
 * </ul>
 *
 * <p><b>Fork divergences vs upstream</b> — captured here once instead of at
 * every consumer site:
 * <ul>
 *   <li><b>No {@code RObjectFactory}.</b> Upstream's {@code streamObjects}
 *       converts each {@code RosettaReport} via
 *       {@code rObjectFactory.buildRFunction(report)}; the fork uses
 *       {@link RFunction#fromReport(RReport)} (added at Phase X T2).</li>
 *   <li><b>{@code toMetaJavaType} 2-arg signature.</b> Upstream's accessor is
 *       1-arg ({@code rAttr.toMetaJavaType}) — the fork's
 *       {@link JavaTypeTranslator#toMetaJavaType(RAttribute, com.regnosys.rosetta.types.RType)}
 *       is 2-arg because {@link RAttribute} lacks the upstream
 *       {@code getRMetaAnnotatedType().getRType()} accessor (T2.5). The
 *       second argument is the pre-resolved {@code RType}, fetched here via
 *       {@link GeneratorModel#getType(RAttribute)}.</li>
 *   <li><b>{@code toFunctionJavaClass} dispatch on origin (T6.0.5).</b>
 *       Upstream's 1-arg {@code toFunctionJavaClass(RFunction)} switches on
 *       {@code RFunction.getOrigin()} (REPORT vs FUNCTION vs RULE). The fork
 *       mirrors this at T6.0.5 via
 *       {@link JavaTypeTranslator#toFunctionJavaClass(RFunction, ModelSymbolId)}
 *       — {@code RFunction.origin()} drives the 3-way switch into private
 *       routers ({@code toJavaFunctionClass} / {@code toJavaReportClass} /
 *       {@code toJavaRuleClass}). The caller passes the pre-resolved
 *       {@link ModelSymbolId} (composed via
 *       {@code generatorModel.symbolId(rFunction)}) because the fork's
 *       {@link JavaTypeTranslator} has no {@code GeneratorModel} dependency
 *       (used by 20+ call sites that don't have one).</li>
 *   <li><b>{@code JavaParameterizedType.from} 2-arg overload.</b> Upstream's
 *       Xtend uses {@code TypeReference<ReportFunction<?,?>>}; the fork's
 *       {@link JavaParameterizedType#from(JavaGenericTypeDeclaration, JavaTypeArgument...)}
 *       requires a {@link JavaGenericTypeDeclaration} as the raw-type marker.
 *       {@code JavaClass.from(ReportFunction.class)} would NOT compile
 *       against that overload. We use
 *       {@link JavaGenericTypeDeclaration#from(Class)} instead (same
 *       discipline as {@link RuleGenerator} at T5).</li>
 *   <li><b>No upstream {@code @Inject} DI.</b> Constructor-injected by every
 *       wiring site, same pattern as {@link RuleGenerator} (T5) and
 *       {@code LabelProviderGenerator} (T3).</li>
 *   <li><b>Annotation argument fragment is fully-rendered.</b> Upstream's
 *       Xtend value is just the inner args (e.g.
 *       {@code namespace="...", body="..."}) — the framework wraps it as
 *       {@code @<SimpleName>(<value>)}. The fork's
 *       {@code FunctionGenerator.buildClassWithBaseInterface} contract
 *       expects the FULL invocation (e.g.
 *       {@code RosettaReport(namespace="...", body="...")}) as the
 *       {@code Map} value, with the {@code @} prepended by the template at
 *       emission. See {@code FunctionTemplateModel.getAnnotationFragments}
 *       javadoc for the contract.</li>
 * </ul>
 *
 * <p>NOT yet wired into {@code JavaCodeGenerator}'s per-model generator list
 * — that's Phase X T7 (deferred until all three new generators have landed
 * + the wiring change can be made + verified against the full D11 matrix in
 * one focused step). This class is complete on its own and exercised
 * directly by {@code ReportGeneratorTest}.
 */
public class ReportGenerator
        extends JavaClassGenerator<RFunction, RGeneratedJavaClass<? extends RosettaFunction>> {

    private final GeneratorModel generatorModel;
    private final JavaTypeTranslator typeTranslator;
    private final FunctionGenerator functionGenerator;

    /**
     * Construct with the three dependencies upstream injects via Guice.
     *
     * @param generatorModel the per-workspace generator-model bridge (used
     *        for namespace resolution off the source RReport and for
     *        attribute-type resolution on the synthetic RFunction inputs +
     *        output)
     * @param typeTranslator the Java-class translator (used to derive the
     *        generated report-function class FQN via the principled
     *        origin-dispatched
     *        {@link JavaTypeTranslator#toFunctionJavaClass(RFunction, ModelSymbolId)}
     *        (T6.0.5) and the label-provider class FQN via
     *        {@link JavaTypeTranslator#toLabelProviderJavaClass} +
     *        meta-typed argument resolution for the
     *        {@code ReportFunction<I,O>} type parameters via
     *        {@link JavaTypeTranslator#toMetaJavaType(RAttribute, com.regnosys.rosetta.types.RType)})
     * @param functionGenerator the function generator (delegated to for the
     *        actual emission via
     *        {@link FunctionGenerator#buildClassWithBaseInterface} — T4)
     */
    public ReportGenerator(GeneratorModel generatorModel,
                           JavaTypeTranslator typeTranslator,
                           FunctionGenerator functionGenerator) {
        this.generatorModel = Objects.requireNonNull(generatorModel, "generatorModel");
        this.typeTranslator = Objects.requireNonNull(typeTranslator, "typeTranslator");
        this.functionGenerator = Objects.requireNonNull(functionGenerator, "functionGenerator");
    }

    /**
     * Filter {@link RReport} root elements + bridge each to a synthetic
     * {@link RFunction} via {@link RFunction#fromReport(RReport)}. Mirrors
     * upstream {@code ReportGenerator.streamObjects} verbatim.
     */
    @Override
    protected Stream<? extends RFunction> streamObjects(RModel model) {
        return model.rootElements().stream()
                .filter(e -> e instanceof RReport)
                .map(e -> RFunction.fromReport((RReport) e));
    }

    /**
     * Compute the generated {@code <body><corpus...>ReportFunction} class via
     * the principled origin-dispatched
     * {@link JavaTypeTranslator#toFunctionJavaClass(RFunction, ModelSymbolId)}
     * entry point (T6.0.5). Mirrors upstream
     * {@code ReportGenerator.createTypeRepresentation} which dispatches via
     * {@code RFunction.getOrigin() == REPORT → toJavaReportClass} (rune-dsl
     * 9.83.0-line upstream lines 122-126).
     *
     * <p><b>REPORT-origin routing</b> places the class under
     * {@code <namespace>.reports/<body+corpus>ReportFunction}. The
     * {@code ModelSymbolId.getNamespace()} resolved by
     * {@link GeneratorModel#symbolId(RFunction)} provides the namespace; the
     * simple name is composed from {@link RFunction#originReport()}'s
     * body+corpus references inside the private {@code toJavaReportClass}
     * router (mirrors upstream's {@code ModelReportId.joinRegulatoryReference()}).
     *
     * <p><b>Synthetic-RFunction recovery:</b>
     * {@link RFunction#fromReport(RReport)} produces an in-memory-only
     * synthetic with no {@code RModel} parent (NOT linker-attached). The
     * source {@link RReport} IS {@code RModel}-attached and is carried via
     * the {@code originReport()} back-pointer;
     * {@link GeneratorModel#symbolId(RFunction)} reads namespace off it for
     * the synthetic case (see GeneratorModel T5 javadoc for the recovery
     * semantics).
     */
    @Override
    protected RGeneratedJavaClass<? extends RosettaFunction> createTypeRepresentation(
            RFunction rFunction) {
        // Principled origin-dispatched routing per T6.0.5 — RFunction.origin()
        // == REPORT routes to <namespace>.reports/<body+corpus>ReportFunction
        // via the private toJavaReportClass router inside JavaTypeTranslator.
        return typeTranslator.toFunctionJavaClass(
                rFunction, generatorModel.symbolId(rFunction));
    }

    /**
     * Emit the report class. Construct the {@code ReportFunction<I,O>} base
     * interface from the synthetic function's first input + output
     * meta-typed attributes, build the two class-level annotation fragments
     * ({@code @RosettaReport} + {@code @RuneLabelProvider}), then delegate
     * to {@link FunctionGenerator#buildClassWithBaseInterface} with
     * {@code renderAsReportFunction=true}. Mirrors upstream
     * {@code ReportGenerator.generateClass}.
     */
    @Override
    @SuppressWarnings("rawtypes") // JavaParameterizedType<ReportFunction> — same
                                  // erasure idiom as RuleGenerator at T5; the
                                  // parameterization comes from the input /
                                  // output meta JavaType args passed to
                                  // JavaParameterizedType.from, NOT from the
                                  // raw-type marker.
    protected String generate(RFunction rFunction,
                              RGeneratedJavaClass<? extends RosettaFunction> clazz,
                              String version) {
        if (rFunction.inputs().isEmpty()) {
            throw new IllegalStateException(
                    "ReportGenerator: synthetic RFunction has no input attribute — "
                    + "RFunction.fromReport(RReport) should always set one. function name="
                    + rFunction.name());
        }
        RAttribute input = rFunction.inputs().get(0);
        RAttribute output = rFunction.output().orElseThrow(() -> new IllegalStateException(
                "ReportGenerator: synthetic RFunction has no output attribute — "
                + "RFunction.fromReport(RReport) should always set one. function name="
                + rFunction.name()));
        RReport report = rFunction.originReport().orElseThrow(() -> new IllegalStateException(
                "ReportGenerator: synthetic RFunction has no originReport back-pointer — "
                + "RFunction.fromReport(RReport) should always set one. function name="
                + rFunction.name()));

        // Phase X1 (Gap #1) — back-fill the synthetic input's resolved type id
        // from the origin report's fromType (RTypeCall.deepCopy drops the id;
        // see RuleGenerator + GeneratorModel#resolveTypeCall). Without this the
        // input collapses to Object for from-types in emission-excluded
        // namespaces (CDM transitive deps).
        if (input.typeCall() != null && report.fromType() != null) {
            report.fromType().referencedTypeId()
                    .ifPresent(input.typeCall()::setReferencedTypeId);
            // round 1 (cq SF-5): the synthetic type call is ATTACHED to the generator's workspace whether or
            // not the from-type's id was present (an attached id-less node is harmless; an id on a DETACHED
            // node is not - its first referencedType() read would throw and be rendered as a TODO comment,
            // the seat's one surprise, RFunction.fromReport's twin; round 2, cq NIT-8: the comment made exact)
            input.typeCall().attachToWorkspace(generatorModel.workspace());
        }
        // v3.2 seat 4 (PR #625, F7): the synthetic output is typed BY the linker's id
        // (RFunction.fromReport copies report.withTypeId()); an output reaching this seat without one
        // is REFUSED, never resolved by a workspace-wide search on the simple name — the search that
        // typed the chaos s07 report functions with the FIRST namespace's report type and rules. The
        // ONE declaration the label provider consults too (LAW 69).
        RuleReferenceTraversal.requireResolvedReportType(rFunction, report);

        // PR #322 (reportOperations) — synthesize the per-attribute rule-invocation
        // operations upstream RObjectFactory.generateOperations builds at
        // buildRFunction(RosettaReport) time. Done HERE (not in RFunction.fromReport)
        // because the rule-reference traversal needs the generator-model context
        // (linker-resolved types, the workspace-wide rule-source resolution).
        synthesizeReportOperations(rFunction, report, input, output);

        JavaType inputMetaType = typeTranslator.toMetaJavaType(input,
                generatorModel.getType(input));
        JavaType outputMetaType = typeTranslator.toMetaJavaType(output,
                generatorModel.getType(output));

        // ReportFunction<I,O> raw-type marker. JavaClass.from(ReportFunction.class)
        // would NOT match JavaParameterizedType.from's
        // (JavaGenericTypeDeclaration<? super T>, JavaTypeArgument...) overload
        // — JavaClass does not extend JavaGenericTypeDeclaration. Use the
        // genericDecl helper instead (same idiom as RuleGenerator at T5).
        JavaGenericTypeDeclaration<ReportFunction> rfDecl =
                JavaGenericTypeDeclaration.from(ReportFunction.class);
        JavaParameterizedType<ReportFunction> baseInterface = JavaParameterizedType.from(
                rfDecl, (JavaTypeArgument) inputMetaType, (JavaTypeArgument) outputMetaType);

        // Build the two class-level annotation fragments. Per FunctionGenerator
        // contract, the Map<Class<?>,String> values are FULLY-rendered
        // annotation invocations (e.g. "RosettaReport(namespace=...)") without
        // the leading '@' — the template prepends the '@'. The Class<?> keys
        // drive import collection via the T6 extension to
        // mergeBaseInterfaceImports so each annotation's declaring package is
        // pulled into the imports list (allowing simple-name rendering in
        // emission).
        Map<Class<?>, String> annotations = new LinkedHashMap<>();
        annotations.put(RosettaReport.class, buildRosettaReportFragment(report, clazz));
        // The @RuneLabelProvider fragment references <X>LabelProvider.class
        // inline by SIMPLE name (byte-parity with upstream legacy plugin) —
        // we must therefore pass the corresponding canonical name as a
        // supporting import so the emitted source compiles.
        ModelSymbolId symbolId = generatorModel.symbolId(rFunction);
        RGeneratedJavaClass<?> labelProviderClass =
                typeTranslator.toLabelProviderJavaClass(symbolId);
        String labelProviderFqn = labelProviderClass.getCanonicalName().withDots();
        annotations.put(RuneLabelProvider.class,
                "RuneLabelProvider(labelProvider=" + labelProviderClass.getSimpleName() + ".class)");

        JavaClassScope scope = JavaClassScope.createAndRegisterIdentifier(clazz);
        return functionGenerator.buildClassWithBaseInterface(
                rFunction, clazz,
                /* isAbstract — reserved per spec § 3.3 forward-compat; same
                 * future-state slot as RuleGenerator at T5. */ false,
                List.<JavaParameterizedType>of(baseInterface),
                annotations,
                /* renderAsReportFunction */ true, scope,
                /* supportingImports */ List.of(labelProviderFqn));
    }

    /**
     * PR #322 (reportOperations) — the fork analogue of upstream
     * {@code RObjectFactory.generateOperations} (rune-dsl 9.83.0-line upstream lines
     * 162-189): fold the shared rule-reference traversal
     * ({@link RuleReferenceTraversal} — the PR #321 faithful port of upstream
     * {@code RuleReferenceService.traverse}, promoted at PR #322) into one
     * {@code ROperation(SET, output, assignPath, <rule>(input))} per
     * NON-explicitly-empty rule association, in traversal order (upstream's
     * {@code LinkedHashMap} fold — a later association on the SAME path
     * replaces the value but keeps the original position).
     *
     * <p>Each operation carries:
     * <ul>
     *   <li>{@code targetName = "output"} + an {@link RSegment} chain naming
     *       the assign path, with {@code resolvedAttribute} set DIRECTLY from
     *       the traversal's path attributes (the renderer's
     *       {@code renderSetBuilderChain} reads it for the
     *       {@code getOrCreateX()} multi-indexing + the leaf setter
     *       discrimination — synthetic segments never pass through
     *       {@code ValidationPass.resolveOperationPaths});</li>
     *   <li>a synthetic explicit-args {@link RSymbolReference} to the rule
     *       with a single {@code input} argument resolving to the function's
     *       input attribute — upstream
     *       {@code generateOperationForRuleReference}'s
     *       {@code symbolRef(rule, args=[inputAttribute])}. Parent links are
     *       wired ({@code arg → ruleRef → op → func}) so the expression
     *       handlers' enclosing-context walks
     *       ({@code HandlerHelper.findEnclosingFunction}) resolve to the
     *       REPORT-origin synthetic function.</li>
     * </ul>
     *
     * <p>Idempotent (skips when operations already exist) — {@code generate}
     * is invoked once per streamed synthetic, but the guard keeps a re-entry
     * harmless.
     */
    private void synthesizeReportOperations(RFunction func, RReport report,
                                            RAttribute input, RAttribute output) {
        if (!func.operations().isEmpty()) {
            return;
        }
        RDataType reportType = RuleReferenceTraversal.unwrapToDataType(
                generatorModel.getType(output));
        if (reportType == null) {
            return; // non-data output type — no attributes to traverse
        }
        RuleReferenceTraversal traversal = new RuleReferenceTraversal(generatorModel);
        RExternalRuleSource source = traversal.resolveRuleSource(func, report);
        // Upstream's fold: if (!context.isExplicitlyEmpty()) acc.put(path, rule)
        Map<List<RAttribute>, RRule> assignments = new LinkedHashMap<>();
        traversal.traverse(source, reportType, (path, result) -> {
            if (result.rule() != null) {
                assignments.put(List.copyOf(path), result.rule());
            }
        });
        for (Map.Entry<List<RAttribute>, RRule> e : assignments.entrySet()) {
            func.operations().add(buildRuleOperation(func, input, e.getKey(), e.getValue()));
        }
    }

    /**
     * Build one synthetic {@code set output -> <path>: <rule>(input)}
     * operation — upstream {@code generateOperationForRuleReference}.
     */
    private ROperation buildRuleOperation(RFunction func, RAttribute input,
                                          List<RAttribute> path, RRule rule) {
        ROperation op = new ROperation();
        op.setOperator(OperationOp.SET);
        op.setTargetName("output");

        RSegment head = null;
        RSegment tail = null;
        for (RAttribute attr : path) {
            RSegment seg = new RSegment();
            seg.setName(attr.name());
            seg.setResolvedAttribute(attr);
            if (tail == null) {
                head = seg;
                seg.setParent(op);
            } else {
                tail.setNext(seg);
                seg.setParent(tail);
            }
            tail = seg;
        }
        op.setSegment(head);

        RSymbolReference inputRef = new RSymbolReference();
        inputRef.setName(input.name());
        inputRef.setResolvedSymbol(input);
        RSymbolReference ruleRef = new RSymbolReference();
        ruleRef.setName(rule.name());
        ruleRef.setResolvedSymbol(rule);
        ruleRef.args().add(inputRef);
        inputRef.setParent(ruleRef);
        ruleRef.setParent(op);
        op.setExpression(ruleRef);
        op.setParent(func);
        return op;
    }

    /**
     * Phase X T6 — render the {@code @RosettaReport} annotation argument
     * fragment as
     * {@code RosettaReport(namespace="<ns>", body="<body>", corpusList={"X", "Y"})}.
     *
     * <p>The simple name {@code RosettaReport} is intentionally embedded in
     * the fragment so the template can emit {@code @<fragment>} verbatim
     * (per the contract documented on
     * {@code FunctionGenerator.buildClassWithBaseInterface}). The
     * corresponding canonical import (
     * {@code com.rosetta.model.lib.annotations.RosettaReport}) is added by
     * {@code mergeBaseInterfaceImports} from the {@code Map} key driven by
     * the T6 extension.
     *
     * <p><b>Empty corpus list</b> renders as {@code corpusList={}} — matches
     * upstream's {@code FOR corpus SEPARATOR ", "} which emits an empty body
     * inside the braces when the corpus list is empty (the
     * {@code RosettaReport} annotation's {@code corpusList} default value
     * tolerates an empty array).
     */
    private static String buildRosettaReportFragment(RReport report,
            RGeneratedJavaClass<? extends RosettaFunction> clazz) {
        // The emitted class's package IS the report's namespace's `.reports`
        // child, NOT the bare namespace — the @RosettaReport.namespace value
        // refers to the report's source namespace, so we drop the trailing
        // ".reports" segment to recover it.
        String classPackage = clazz.getPackageName().withDots();
        String namespace;
        if (classPackage.endsWith(".reports")) {
            namespace = classPackage.substring(0,
                    classPackage.length() - ".reports".length());
        } else {
            // Defensive fallback: caller's package layout diverged from the
            // toJavaReportClass convention (T6.0.5 — REPORT-origin private
            // router in JavaTypeTranslator). Use the classPackage
            // verbatim so the generator doesn't crash; the divergence will
            // surface at byte-parity verification.
            namespace = classPackage;
        }
        RRegulatoryDocumentReference ref = report.regulatoryDocRef();
        String body = ref.bodyRef();
        StringBuilder corpusList = new StringBuilder();
        boolean first = true;
        for (String corpus : ref.corpusRefs()) {
            if (corpus == null) continue;
            if (!first) {
                corpusList.append(", ");
            }
            corpusList.append('"').append(corpus).append('"');
            first = false;
        }
        return "RosettaReport(namespace=\"" + namespace
                + "\", body=\"" + body
                + "\", corpusList={" + corpusList + "})";
    }

}
