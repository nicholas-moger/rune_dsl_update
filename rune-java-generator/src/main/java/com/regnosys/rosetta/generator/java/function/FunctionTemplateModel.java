package com.regnosys.rosetta.generator.java.function;

import com.regnosys.rosetta.types.RType;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaParameterizedType;
import com.rosetta.util.types.JavaReferenceType;
import com.rosetta.util.types.JavaTypeArgument;
import com.rosetta.util.types.JavaWildcardTypeArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Template model for function ST4 templates.
 * Provides all values needed by the ST4 templates to generate function classes.
 *
 * <p>All getters follow JavaBeans convention (getXxx) so that ST4 can resolve
 * {@code <m.fieldName>} expressions via reflection.</p>
 */
public class FunctionTemplateModel {

    // ---- Identity ----
    private final String packageName;
    private final String className;
    private final List<String> imports;
    private final List<String> staticImports;

    // ---- Pre-computed type strings (avoids ST4 delimiter escaping issues) ----
    /** e.g. "List<Date>" or "Date" — return type for public evaluate() method. */
    private final String evaluateReturnType;
    /** e.g. "List<Date>" or "Date.DateBuilder" — return type for doEvaluate/assignOutput. */
    private final String doEvaluateReturnType;
    /** Pre-computed @Inject fields block (conditions, objectValidator, deps) with trailing blank line. */
    private final String injectBlock;
    /** Pre-computed null-check block for multi-valued inputs in doEvaluate. */
    private final String multiInputNullChecks;
    /** Pre-computed alias override methods block. */
    private final String aliasOverridesBlock;
    /**
     * Pre-rendered private helper members appended INSIDE the generated
     * {@code <Name>Default} class body, immediately before its closing brace —
     * {@code ""} for none (the template slot renders ZERO bytes). Stamped
     * post-construction by {@link FunctionGenerator#buildStandardModel} from the
     * {@code buildHelperMethodsBlock} seam; the reference pipeline always leaves
     * it empty (U019 — the optimised generator's member-injection surface).
     */
    private String helperMethodsBlock = "";

    // ---- Category flags ----
    /** True when function implements IQualifyFunctionExtension. */
    private final boolean isQualify;
    /** True when function is a dispatch function. */
    private final boolean isDispatch;
    /** True for Qualify_ functions that @Override evaluate. */
    private final boolean overridesEvaluate;
    /** Type parameter for IQualifyFunctionExtension&lt;T&gt;, null if not a Qualify_ function. */
    private final String qualifyGenericType;
    /** Simple name of the @RuneLabelProvider class, null if none. */
    private final String labelProviderClassName;
    /** Javadoc @version value for dispatch functions, null if not dispatch. */
    private final String version;
    /** True when deep operations trigger two-arg toBuilder usage. */
    private final boolean hasDeepOperations;

    // ---- Parameters ----
    private final List<ParamModel> inputs;
    /** Output parameter, null for void functions. */
    private final ParamModel output;
    private final boolean outputIsMulti;
    private final boolean outputNeedsBuilder;

    // ---- Body ----
    private final List<AliasModel> aliases;
    private final List<OperationModel> operations;
    private final List<ConditionModel> preConditions;
    private final List<ConditionModel> postConditions;

    // ---- Dependencies ----
    /** True when a condition validator field is needed. */
    private final boolean hasConditions;
    /** True when an object validator field is needed. */
    private final boolean hasObjectValidator;
    private final List<DependencyModel> dependencies;

    // ---- Super function ----
    /** Simple name of the superclass, null if none. */
    private final String superClassName;
    /** Fully-qualified name of the superclass, null if none. */
    private final String superClassFqn;

    // ---- Documentation ----
    /** Rune definition / description text. */
    private final String definition;
    /** @param lines for Javadoc. */
    private final List<String> javadocParams;

    // ---- Dispatch ----
    private final List<DispatchVariantModel> dispatchVariants;
    /** Name of the dispatch parameter (the enum-typed argument). */
    private final String dispatchParamName;
    /** Simple type name of the dispatch enum. */
    private final String dispatchEnumType;

    // ---- Phase X T4: extension surface for Rule/Report generators ----
    /**
     * Phase X T4 — additional interfaces emitted in the class declaration
     * {@code implements} clause. Empty by default (standard function emission
     * unchanged). When non-empty AND {@link #renderAsReportFunction} is true,
     * REPLACES the standard {@code implements RosettaFunction[, IQualifyFunctionExtension<T>]}
     * clause with {@code implements <baseInterfaces>}. Used by
     * {@code RuleGenerator} (T5) and {@code ReportGenerator} (T6) to emit
     * {@code implements ReportFunction<I, O>}.
     */
    private final List<JavaParameterizedType> baseInterfaces;
    /**
     * Phase X T4 — class-level annotations emitted on the line immediately
     * above the class declaration. Map keys are the annotation type (used by
     * callers for ordering / diagnostics); values are pre-rendered annotation
     * source fragments (e.g. {@code "RosettaReport(namespace=\"...\", body=\"...\")"})
     * with the leading {@code @} prepended by the template. Empty by default
     * (standard function emission unchanged).
     */
    private final Map<Class<?>, String> annotations;
    /**
     * Phase X T4 — when {@code true} AND {@link #baseInterfaces} is non-empty,
     * the ST template replaces the standard {@code implements RosettaFunction...}
     * clause with {@code implements <baseInterfaces>}. When {@code false}
     * (default), {@code baseInterfaces} has no effect on the class declaration.
     */
    private final boolean renderAsReportFunction;

    /**
     * Backward-compatible ctor — existing call sites pre-Phase X. Delegates to
     * the full ctor with empty {@code baseInterfaces} + {@code annotations} +
     * {@code renderAsReportFunction=false}. Produces byte-equivalent output to
     * the pre-T4 emission path (locked by
     * {@code FunctionGeneratorTest#buildClassWithBaseInterface_unchangedWhenBaseAndAnnotationsEmpty}).
     */
    public FunctionTemplateModel(
            String packageName,
            String className,
            List<String> imports,
            List<String> staticImports,
            boolean isQualify,
            boolean isDispatch,
            boolean overridesEvaluate,
            String qualifyGenericType,
            String labelProviderClassName,
            String version,
            boolean hasDeepOperations,
            List<ParamModel> inputs,
            ParamModel output,
            boolean outputIsMulti,
            boolean outputNeedsBuilder,
            List<AliasModel> aliases,
            List<OperationModel> operations,
            List<ConditionModel> preConditions,
            List<ConditionModel> postConditions,
            boolean hasConditions,
            boolean hasObjectValidator,
            List<DependencyModel> dependencies,
            String superClassName,
            String superClassFqn,
            String definition,
            List<String> javadocParams,
            List<DispatchVariantModel> dispatchVariants,
            String dispatchParamName,
            String dispatchEnumType) {
        this(packageName, className, imports, staticImports,
                isQualify, isDispatch, overridesEvaluate,
                qualifyGenericType, labelProviderClassName, version,
                hasDeepOperations, inputs, output, outputIsMulti, outputNeedsBuilder,
                aliases, operations, preConditions, postConditions,
                hasConditions, hasObjectValidator, dependencies,
                superClassName, superClassFqn, definition, javadocParams,
                dispatchVariants, dispatchParamName, dispatchEnumType,
                List.of(), Map.of(), false);
    }

    /**
     * Phase X T4 full ctor — accepts the 3 new fields ({@code baseInterfaces},
     * {@code annotations}, {@code renderAsReportFunction}) used by Rule/Report
     * generator emission. The backward-compat ctor above delegates to this
     * with empty defaults to preserve byte-equivalent standard function
     * emission.
     */
    public FunctionTemplateModel(
            String packageName,
            String className,
            List<String> imports,
            List<String> staticImports,
            boolean isQualify,
            boolean isDispatch,
            boolean overridesEvaluate,
            String qualifyGenericType,
            String labelProviderClassName,
            String version,
            boolean hasDeepOperations,
            List<ParamModel> inputs,
            ParamModel output,
            boolean outputIsMulti,
            boolean outputNeedsBuilder,
            List<AliasModel> aliases,
            List<OperationModel> operations,
            List<ConditionModel> preConditions,
            List<ConditionModel> postConditions,
            boolean hasConditions,
            boolean hasObjectValidator,
            List<DependencyModel> dependencies,
            String superClassName,
            String superClassFqn,
            String definition,
            List<String> javadocParams,
            List<DispatchVariantModel> dispatchVariants,
            String dispatchParamName,
            String dispatchEnumType,
            List<JavaParameterizedType> baseInterfaces,
            Map<Class<?>, String> annotations,
            boolean renderAsReportFunction) {

        this.packageName = packageName;
        this.className = className;
        this.imports = List.copyOf(imports);
        this.staticImports = staticImports != null ? List.copyOf(staticImports) : List.of();

        // Compute type strings to avoid ST4 <%...%> escape issues
        this.evaluateReturnType = computeEvaluateReturnType(output, outputIsMulti, outputNeedsBuilder);
        this.doEvaluateReturnType = computeDoEvaluateReturnType(output, outputIsMulti, outputNeedsBuilder);
        this.injectBlock = computeInjectBlock(hasConditions, hasObjectValidator, dependencies);
        this.multiInputNullChecks = computeMultiInputNullChecks(inputs);
        // facet aliasOutputBuilderNav (PR #381, W): stamp each alias's rendered
        // parameter list — a usesOutput alias joins its own analyzed params (the
        // output builder threads FIRST); every other alias carries the
        // function-input list, byte-identical to the pre-#381 template expansion
        // (usesOutput fires on ZERO aliases corpus-wide outside the W class — the
        // 381-W tracer census).
        String aliasInputParams = computeInputParamList(inputs);
        List<AliasModel> stampedAliases = new ArrayList<>(aliases.size());
        for (AliasModel al : aliases) {
            stampedAliases.add(al.withParamsDecl(al.getUsesOutput()
                    ? String.join(", ", al.getParams())
                    : aliasInputParams));
        }
        aliases = stampedAliases;
        this.aliasOverridesBlock = computeAliasOverridesBlock(aliases, inputs);
        this.isQualify = isQualify;
        this.isDispatch = isDispatch;
        this.overridesEvaluate = overridesEvaluate;
        this.qualifyGenericType = qualifyGenericType;
        this.labelProviderClassName = labelProviderClassName;
        this.version = version;
        this.hasDeepOperations = hasDeepOperations;
        this.inputs = List.copyOf(inputs);
        this.output = output;
        this.outputIsMulti = outputIsMulti;
        this.outputNeedsBuilder = outputNeedsBuilder;
        this.aliases = List.copyOf(aliases);
        this.operations = List.copyOf(operations);
        this.preConditions = List.copyOf(preConditions);
        this.postConditions = List.copyOf(postConditions);
        this.hasConditions = hasConditions;
        this.hasObjectValidator = hasObjectValidator;
        this.dependencies = List.copyOf(dependencies);
        this.superClassName = superClassName;
        this.superClassFqn = superClassFqn;
        this.definition = definition;
        this.javadocParams = List.copyOf(javadocParams);
        this.dispatchVariants = List.copyOf(dispatchVariants);
        this.dispatchParamName = dispatchParamName;
        this.dispatchEnumType = dispatchEnumType;

        // Phase X T4 — defensive copies; null inputs degrade to empty defaults
        // so back-compat ctor delegation cannot NPE on legacy call sites.
        // The annotations map is wrapped as unmodifiable-LinkedHashMap rather
        // than Map.copyOf because Map.copyOf produces an iteration order based
        // on internal hash buckets (System.identityHashCode of the Class keys),
        // which is non-deterministic across JVM lifetimes — that defeats the
        // ST template's stable per-call ordering contract and caused a flaky
        // FunctionGeneratorTest.buildClassWithBaseInterface_emitsAnnotationsOnClass
        // failure (T5.0.5 Indep sweep finding). LinkedHashMap preserves
        // caller-side insertion order, matching the @<frag1>\n@<frag2>\n
        // @ImplementedBy adjacency the template asserts.
        //
        // Copilot PR #72 R5 F14 — the local LinkedHashMap copy only preserves
        // ordering if the SOURCE map already has a stable iteration order.
        // A caller passing a plain HashMap would seed the copy with
        // hash-bucket order. Enforce the contract: when annotations.size() > 1
        // (single-entry maps have only one order), the source must implement
        // SequencedMap (Java 21+ marker for insertion/access-ordered maps;
        // implemented by LinkedHashMap). Size-0/1 maps are accepted as-is.
        this.baseInterfaces = baseInterfaces != null
                ? List.copyOf(baseInterfaces) : List.of();
        if (annotations != null && annotations.size() > 1
                && !(annotations instanceof java.util.SequencedMap)) {
            throw new IllegalArgumentException(
                    "FunctionTemplateModel.annotations: multi-entry annotation map must implement "
                            + "java.util.SequencedMap (e.g. LinkedHashMap) so the template emits "
                            + "annotations in a deterministic order. Got: "
                            + annotations.getClass().getName());
        }
        this.annotations = annotations != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(annotations))
                : Map.of();
        this.renderAsReportFunction = renderAsReportFunction;
    }

    // ---- Identity getters ----
    public String getPackageName() { return packageName; }
    public String getClassName() { return className; }
    public List<String> getImports() { return imports; }
    public List<String> getStaticImports() { return staticImports; }

    // ---- Computed type string getters ----
    public String getEvaluateReturnType() { return evaluateReturnType; }
    public String getDoEvaluateReturnType() { return doEvaluateReturnType; }
    public String getInjectBlock() { return injectBlock; }
    public String getMultiInputNullChecks() { return multiInputNullChecks; }
    public String getAliasOverridesBlock() { return aliasOverridesBlock; }
    public String getHelperMethodsBlock() { return helperMethodsBlock; }

    /**
     * Stamps the {@code Default}-class helper-members slot (U019 seam; see the
     * field javadoc). Package-private — called only by
     * {@link FunctionGenerator#buildStandardModel}; {@code null} degrades to
     * {@code ""} so the template slot stays byte-inert.
     */
    void setHelperMethodsBlock(String helperMethodsBlock) {
        this.helperMethodsBlock = helperMethodsBlock != null ? helperMethodsBlock : "";
    }

    // ---- Category flag getters ----
    public boolean getIsQualify() { return isQualify; }
    public boolean getIsDispatch() { return isDispatch; }
    public boolean getOverridesEvaluate() { return overridesEvaluate; }
    public String getQualifyGenericType() { return qualifyGenericType; }
    public String getLabelProviderClassName() { return labelProviderClassName; }
    public String getVersion() { return version; }
    public boolean getHasDeepOperations() { return hasDeepOperations; }

    // ---- Parameter getters ----
    public List<ParamModel> getInputs() { return inputs; }
    public ParamModel getOutput() { return output; }
    public boolean getOutputIsMulti() { return outputIsMulti; }
    public boolean getOutputNeedsBuilder() { return outputNeedsBuilder; }

    // ---- Body getters ----
    public List<AliasModel> getAliases() { return aliases; }
    public List<OperationModel> getOperations() { return operations; }
    public List<ConditionModel> getPreConditions() { return preConditions; }
    public List<ConditionModel> getPostConditions() { return postConditions; }

    // ---- Dependency getters ----
    public boolean getHasConditions() { return hasConditions; }
    public boolean getHasObjectValidator() { return hasObjectValidator; }
    public List<DependencyModel> getDependencies() { return dependencies; }

    // ---- Super function getters ----
    public String getSuperClassName() { return superClassName; }
    public String getSuperClassFqn() { return superClassFqn; }

    // ---- Documentation getters ----
    public String getDefinition() { return definition; }
    public List<String> getJavadocParams() { return javadocParams; }

    // ---- Dispatch getters ----
    public List<DispatchVariantModel> getDispatchVariants() { return dispatchVariants; }
    public String getDispatchParamName() { return dispatchParamName; }
    public String getDispatchEnumType() { return dispatchEnumType; }

    // ---- Phase X T4 getters ----
    public List<JavaParameterizedType> getBaseInterfaces() { return baseInterfaces; }
    public Map<Class<?>, String> getAnnotations() { return annotations; }
    /**
     * Phase X T4 — JavaBeans {@code is*} naming for boolean accessor (matches
     * existing {@code FunctionTemplateModel} convention {@code isQualify()},
     * {@code isDispatch()}). ST4 resolves {@code <m.renderAsReportFunction>}
     * via either {@code is*} or {@code get*} reflection, so the template
     * surface is unchanged.
     */
    public boolean isRenderAsReportFunction() { return renderAsReportFunction; }
    /**
     * ST template convenience: pre-rendered annotation source fragments
     * (just the values, in insertion order). Avoids ST4 reflection-on-Map
     * complexity inside the template — the template emits {@code @<frag>}
     * lines from this list.
     */
    public List<String> getAnnotationFragments() {
        return List.copyOf(annotations.values());
    }
    /**
     * ST template convenience: pre-rendered base interface declaration
     * fragments (e.g. {@code "ReportFunction<CollateralReportInstruction, EnrichmentData>"}).
     * Both the raw type and ALL type arguments are rendered using their simple
     * names; the callers of {@code FunctionGenerator.buildClassWithBaseInterface}
     * are responsible for adding the corresponding canonical names to the
     * {@code imports} list so the emitted source compiles. Avoids exposing the
     * heavyweight {@code JavaParameterizedType} type to ST4 reflection — the
     * template just emits comma-separated strings.
     *
     * <p>Byte-parity requirement (Phase X T4.0.5 C1): upstream's legacy plugin
     * imports + simple-names both the base interface raw type AND its type
     * arguments — see {@code test-corpus/drr/drr-6.34.1/.../CollateralEnrichmentDataRule.java}
     * lines 6 + 7-9 + 15 for a worked example. The previous
     * {@code JavaParameterizedType::toString} rendering leaked FQN forms on
     * type arguments (e.g. {@code ReportFunction<java.lang.String, java.lang.Integer>})
     * which would have broken D11 byte-parity at T7/T8.
     */
    public List<String> getBaseInterfaceFragments() {
        return baseInterfaces.stream()
                .map(FunctionTemplateModel::renderBaseInterfaceSimpleNames)
                .toList();
    }

    /**
     * Phase X T4.0.5 C1 — render a {@link JavaParameterizedType} with the raw
     * type AND every type-argument expressed as a simple name (no FQN leak).
     * Mirrors the format produced by upstream's legacy plugin for the
     * {@code implements} clause: {@code ReportFunction<I, O>}.
     */
    private static String renderBaseInterfaceSimpleNames(JavaParameterizedType<?> base) {
        String args = base.getArguments().stream()
                .map(FunctionTemplateModel::renderTypeArgumentSimpleName)
                .collect(Collectors.joining(", "));
        return base.getSimpleName() + "<" + args + ">";
    }

    /**
     * Phase X T4.0.5 C1 — render a {@link JavaTypeArgument} using its simple
     * name when possible. {@link JavaClass} renders as canonical (FQN) by
     * default; we override to {@code getSimpleName()} for byte-parity with
     * legacy. Wildcards / type variables fall back to their existing
     * {@code toString()} (already simple-named for the wildcard syntax {@code ?}).
     *
     * <p><b>Nested generics</b> (Copilot PR #72 R2 F3): when a type argument
     * is itself a parameterized type (e.g. {@code FieldWithMeta<String>} from
     * {@link com.regnosys.rosetta.generator.java.types.JavaTypeTranslator#toMetaJavaType}),
     * collapsing it to a bare simple name would erase the inner generic and
     * emit a raw type in the {@code implements} clause. Recurse via
     * {@link #renderBaseInterfaceSimpleNames} so the inner type arguments are
     * preserved using the same simple-name rules.
     *
     * <p><b>Branch order matters:</b> {@link JavaParameterizedType} extends
     * {@link JavaClass}, so the parameterized check must precede the
     * {@link JavaClass} branch — otherwise {@code instanceof JavaClass}
     * matches first for parameterized types and returns the bare raw simple
     * name, never reaching the parameterized branch (indep pre-push review
     * BLOCK 2026-05-19).
     */
    private static String renderTypeArgumentSimpleName(JavaTypeArgument arg) {
        if (arg instanceof JavaParameterizedType<?> jpt) {
            return renderBaseInterfaceSimpleNames(jpt);
        }
        if (arg instanceof JavaClass<?> jc) {
            return jc.getSimpleName();
        }
        // facet reportOutputCardinality (PR #272): a bounded wildcard
        // (`? extends X` / `? super X`) — e.g. the ReportFunction<I, List<? extends
        // PricePeriod>> base interface of a multi-valued model-typed reporting rule.
        // Render the bound by SIMPLE name (recurse for a parameterized bound)
        // instead of the default arg.toString() which prints the bound's FQN inline.
        if (arg instanceof JavaWildcardTypeArgument wild) {
            if (wild.isUnbounded()) {
                return "?";
            }
            String keyword = wild.hasExtendsBound() ? "? extends " : "? super ";
            JavaReferenceType bound = wild.getBound().orElseThrow();
            return keyword + (bound instanceof JavaTypeArgument boundArg
                    ? renderTypeArgumentSimpleName(boundArg)
                    : bound.getSimpleName());
        }
        if (arg instanceof JavaReferenceType jrt) {
            return jrt.getSimpleName();
        }
        return arg.toString();
    }

    /**
     * Phase X T4 — immutable-copy helper: returns a new
     * {@code FunctionTemplateModel} identical to {@code this} except for the
     * imports list (extended with base-interface FQNs) and the 3 Phase X
     * extension fields ({@code baseInterfaces}, {@code annotations},
     * {@code renderAsReportFunction}). Used by
     * {@code FunctionGenerator.buildClassWithBaseInterface(...)} to extend a
     * standard model without re-running the heavyweight builder pipeline.
     *
     * <p>The 4-arg form (with explicit {@code newImports}) is required by
     * Phase X T4.0.5 C1 — base interface imports must be merged in so the
     * {@code implements} clause renders as simple names (byte-parity vs
     * upstream legacy plugin).
     */
    public FunctionTemplateModel withBaseInterfacesAndAnnotations(
            List<String> newImports,
            List<JavaParameterizedType> newBaseInterfaces,
            Map<Class<?>, String> newAnnotations,
            boolean newRenderAsReportFunction) {
        return withBaseInterfacesAndAnnotations(packageName, className, newImports,
                newBaseInterfaces, newAnnotations, newRenderAsReportFunction);
    }

    /**
     * Phase X T6 — 6-arg overload that ALSO overrides the package + class
     * name. Required by report-class emission where the generated class
     * identity is computed via the principled origin-dispatched
     * {@code JavaTypeTranslator.toFunctionJavaClass(RFunction, ModelSymbolId)}
     * (T6.0.5) — for REPORT origin, package {@code <ns>.reports}, simple
     * name {@code <body><corpus...>ReportFunction} — rather than the
     * FUNCTION-origin path ({@code <ns>.functions}). Mirrors the upstream
     * {@code RObjectJavaClassGenerator} contract that uses the
     * {@code createTypeRepresentation}-supplied clazz as the source of truth.
     */
    public FunctionTemplateModel withBaseInterfacesAndAnnotations(
            String newPackageName, String newClassName,
            List<String> newImports,
            List<JavaParameterizedType> newBaseInterfaces,
            Map<Class<?>, String> newAnnotations,
            boolean newRenderAsReportFunction) {
        FunctionTemplateModel derived = new FunctionTemplateModel(
                newPackageName, newClassName, newImports, staticImports,
                // Phase X1 (Gap #1 residual) — the report path implements
                // ReportFunction<I,O>, whose `O evaluate(I)` the emitted abstract
                // `evaluate` overrides, so it must carry @Override (matching the
                // legacy plugin golden). Mirrors the isQualify override of
                // IQualifyFunctionExtension<T>.evaluate. OR-in rather than
                // overwrite so a future qualify+report combination keeps @Override.
                isQualify, isDispatch, overridesEvaluate || newRenderAsReportFunction,
                qualifyGenericType, labelProviderClassName, version,
                hasDeepOperations, inputs, output, outputIsMulti, outputNeedsBuilder,
                aliases, operations, preConditions, postConditions,
                hasConditions, hasObjectValidator, dependencies,
                superClassName, superClassFqn, definition, javadocParams,
                dispatchVariants, dispatchParamName, dispatchEnumType,
                newBaseInterfaces, newAnnotations, newRenderAsReportFunction);
        // U019 (PR #542 Seat-1 SF1): the rule/report derive runs AFTER
        // buildStandardModel stamped the base model, so the seam's stamp must
        // carry forward — a member-emitting family on the rule face would
        // otherwise lose its members silently. Inert while the block is ""
        // (every path today).
        derived.setHelperMethodsBlock(helperMethodsBlock);
        return derived;
    }

    /**
     * Phase X T4 — 3-arg back-compat overload preserves the existing imports
     * list verbatim. Test-only convenience for the BC-pass-through guard test
     * which asserts byte-equivalent output when all 3 Phase X fields are at
     * defaults; production callers MUST use the 4-arg form above so base
     * interface imports are merged in.
     */
    public FunctionTemplateModel withBaseInterfacesAndAnnotations(
            List<JavaParameterizedType> newBaseInterfaces,
            Map<Class<?>, String> newAnnotations,
            boolean newRenderAsReportFunction) {
        return withBaseInterfacesAndAnnotations(imports, newBaseInterfaces,
                newAnnotations, newRenderAsReportFunction);
    }

    // =========================================================================
    // Sub-model POJOs
    // =========================================================================

    // ---- Static helpers for computing type strings ----

    /**
     * Compute the block of @Inject fields that appears between the class opening brace
     * and the Javadoc comment. Returns the text including leading whitespace and
     * trailing blank line (so the template just needs {@code <m.injectBlock>\t/**}).
     */
    private static String computeInjectBlock(boolean hasConditions, boolean hasObjectValidator,
                                              List<DependencyModel> dependencies) {
        StringBuilder sb = new StringBuilder();
        boolean hasAnyField = false;

        if (hasConditions) {
            sb.append("\t\n");
            sb.append("\t@Inject protected ConditionValidator conditionValidator;\n");
            hasAnyField = true;
            if (hasObjectValidator) {
                sb.append("\t\n");
                sb.append("\t@Inject protected ModelObjectValidator objectValidator;\n");
            }
            if (dependencies != null && !dependencies.isEmpty()) {
                sb.append("\t\n");
                sb.append("\t// RosettaFunction dependencies\n");
                sb.append("\t//\n");
                for (DependencyModel dep : dependencies) {
                    sb.append("\t@Inject protected ").append(dep.getTypeName())
                      .append(" ").append(dep.getFieldName()).append(";\n");
                }
            }
        } else if (hasObjectValidator) {
            sb.append("\t\n");
            sb.append("\t@Inject protected ModelObjectValidator objectValidator;\n");
            hasAnyField = true;
            if (dependencies != null && !dependencies.isEmpty()) {
                sb.append("\t\n");
                sb.append("\t// RosettaFunction dependencies\n");
                sb.append("\t//\n");
                for (DependencyModel dep : dependencies) {
                    sb.append("\t@Inject protected ").append(dep.getTypeName())
                      .append(" ").append(dep.getFieldName()).append(";\n");
                }
            }
        } else if (dependencies != null && !dependencies.isEmpty()) {
            sb.append("\t\n");
            sb.append("\t// RosettaFunction dependencies\n");
            sb.append("\t//\n");
            for (DependencyModel dep : dependencies) {
                sb.append("\t@Inject protected ").append(dep.getTypeName())
                  .append(" ").append(dep.getFieldName()).append(";\n");
            }
            hasAnyField = true;
        }

        // The injectBlock is inserted directly before <\t>/** on the same
        // template line, so it must include ALL whitespace between { and /**.
        // Always end with \n (the blank line before /**).
        sb.append("\n");

        return sb.toString();
    }

    private static String computeAliasOverridesBlock(List<AliasModel> aliases, List<ParamModel> inputs) {
        if (aliases == null || aliases.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        String inputParams = computeInputParamList(inputs);
        for (AliasModel alias : aliases) {
            // facet aliasOutputBuilderNav (PR #381, W): the stamped per-alias
            // paramsDecl when present (== inputParams for every non-usesOutput
            // alias by the constructor's stamping law).
            String params = alias.getParamsDecl() != null ? alias.getParamsDecl() : inputParams;
            sb.append("\t\t\n");
            sb.append("\t\t@Override\n");
            sb.append("\t\tprotected ").append(alias.getReturnType()).append(" ")
              .append(alias.getName()).append("(").append(params).append(") {\n");
            sb.append("\t\t\t").append(alias.getCompiledBody()).append("\n");
            sb.append("\t\t}\n");
        }
        return sb.toString();
    }

    private static String computeInputParamList(List<ParamModel> inputs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < inputs.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(inputs.get(i).getInputParamDecl());
        }
        return sb.toString();
    }

    private static String computeMultiInputNullChecks(List<ParamModel> inputs) {
        StringBuilder sb = new StringBuilder();
        for (ParamModel inp : inputs) {
            if (inp.getIsMulti()) {
                sb.append("\t\t\tif (").append(inp.getName()).append(" == null) {\n");
                sb.append("\t\t\t\t").append(inp.getName()).append(" = Collections.emptyList();\n");
                sb.append("\t\t\t}\n");
            }
        }
        // Strip the single trailing newline. In java-function.stg the
        // {@code <m.multiInputNullChecks>} reference sits on its own line, so the
        // template already contributes a line terminator after it; a trailing
        // newline in this value would double into a spurious blank line between the
        // null-normalization block and the first body statement — a blank the
        // golden never emits (multiInputNullChecks parity facet). Regression-free by
        // construction: with no multi inputs this renders an empty string (ST4 trims
        // the whole line, so no blank either way); with multi inputs the fork always
        // emitted that blank, so no green file depends on it — removing it can only
        // flip or stay waivered, never regress a green file.
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private static String computeEvaluateReturnType(ParamModel output, boolean isMulti, boolean needsBuilder) {
        if (output == null) return "void";
        if (isMulti) {
            return needsBuilder
                    ? "List<? extends " + output.getTypeName() + ">"
                    : "List<" + output.getTypeName() + ">";
        }
        return output.getTypeName();
    }

    private static String computeDoEvaluateReturnType(ParamModel output, boolean isMulti, boolean needsBuilder) {
        if (output == null) return "void";
        if (isMulti) {
            return needsBuilder
                    ? "List<" + output.getTypeName() + "." + builderSimpleName(output) + ">"
                    : "List<" + output.getTypeName() + ">";
        }
        return needsBuilder
                ? output.getTypeName() + "." + builderSimpleName(output)
                : output.getTypeName();
    }

    /**
     * facet fqnSelfCollision (PR #366): the nested {@code <Type>Builder} segment is
     * always the SIMPLE name — a self-colliding output carries the dotted FQN in
     * {@code typeName} (typeFqn null), and the builder segment must not repeat it
     * (golden {@code drr.regulation.common.TechnicalRecordId.TechnicalRecordIdBuilder}).
     * Delegates to {@link ParamModel#getBuilderTypeName()} — the ONE derivation the
     * ST templates also read (Copilot #366 R1: keep a single source of truth).
     */
    private static String builderSimpleName(ParamModel output) {
        return output.getBuilderTypeName();
    }

    /** Represents a single function parameter (input or output). */
    public static class ParamModel {
        private final String name;
        private final String typeName;
        private final String typeFqn;
        private final boolean isMulti;
        private final boolean isMeta;
        /** True for Rosetta model types (data/choice) that use {@code ? extends T} in generics and have builders. */
        private final boolean isRosettaModelType;
        /** Pre-computed parameter declaration string, e.g. "List<? extends Date> dates". */
        private final String inputParamDecl;
        /**
         * The doEvaluate-result BUILDER local variable name (e.g. {@code
         * interestRateLeg1Builder}). facet usRename (PR #194): this derives from the
         * RAW output attribute name + {@code "Builder"} and is NOT escaped, even when
         * {@link #name} (the output variable) IS {@code "_"}-escaped for a
         * dependency-name collision — the builder local's desired name
         * ({@code <raw>Builder}) does not collide with the dependency ({@code <raw>}),
         * so upstream leaves it bare. Defaults to {@code name + "Builder"} (byte-
         * identical to the prior {@code <m.output.name>Builder} for non-colliding
         * outputs, where {@code name} is the raw name).
         */
        private final String builderName;

        public ParamModel(String name, String typeName, String typeFqn,
                          boolean isMulti, boolean isMeta, boolean isRosettaModelType) {
            this(name, typeName, typeFqn, isMulti, isMeta, isRosettaModelType, name + "Builder");
        }

        public ParamModel(String name, String typeName, String typeFqn,
                          boolean isMulti, boolean isMeta, boolean isRosettaModelType,
                          String builderName) {
            this.name = name;
            this.typeName = typeName;
            this.typeFqn = typeFqn;
            this.isMulti = isMulti;
            this.isMeta = isMeta;
            this.isRosettaModelType = isRosettaModelType;
            this.builderName = builderName;
            // Pre-compute to avoid ST4 <%...%> escape issues
            if (isMulti) {
                this.inputParamDecl = isRosettaModelType
                        ? "List<? extends " + typeName + "> " + name
                        : "List<" + typeName + "> " + name;
            } else {
                this.inputParamDecl = typeName + " " + name;
            }
        }

        public String getName() { return name; }
        public String getTypeName() { return typeName; }
        public String getTypeFqn() { return typeFqn; }
        public boolean getIsMulti() { return isMulti; }
        public boolean getIsMeta() { return isMeta; }
        public boolean getIsRosettaModelType() { return isRosettaModelType; }
        public String getInputParamDecl() { return inputParamDecl; }
        public String getBuilderName() { return builderName; }

        /**
         * facet fqnSelfCollision (PR #366): the nested {@code <Type>Builder} SEGMENT for
         * the ST-template {@code <typeName>.<builderTypeName>} compositions — always the
         * SIMPLE name + {@code "Builder"} (a self-colliding output carries the dotted FQN
         * in {@code typeName}; golden nests the bare {@code TechnicalRecordIdBuilder}
         * under the FQN outer). A bare typeName has no dot and composes unchanged.
         */
        public String getBuilderTypeName() {
            return typeName.substring(typeName.lastIndexOf('.') + 1) + "Builder";
        }
    }

    /**
     * facet aliasSeamSignature (v3.1 C2d retirement family 5 {@code alias-seam-signature}, PR #612): the TYPED
     * facts an alias method's declared return type — the seam string {@link AliasModel#getReturnType()} — is
     * RENDERED from, recorded by the producer beside the string ({@code FunctionAliasHelper}'s
     * {@code buildMapperReturnType} / {@code computeReturnType} / {@code buildOutputNavBuilderReturnType}, one
     * {@code SeamRender} per branch, so the text and the facts cannot disagree — LAW 69) and carried by every
     * {@link AliasModel} copy. A consumer that must know the seam's FORM (a {@code MapperS} / {@code MapperC}
     * Mapper seam, a usesOutput builder form, the optimised route's value-typed re-seam), its WILDCARD (a Rosetta
     * model element renders {@code Mapper*<? extends T>}) or its ELEMENT spelling (the bare simple name, or the
     * PR #247 {@code ImportCollisionResolver} sentinel when the element collides with a signature type; absent for
     * the legacy {@code MapperS<?>} fallback) asks these facts instead of scanning the text for a {@code MapperS<}
     * prefix, a {@code >} suffix or a {@code ? extends } infix.
     *
     * @param form       the seam's form
     * @param wildcarded whether a Mapper seam renders {@code ? extends} (a Rosetta model element)
     * @param element    a Mapper seam's element spelling exactly as rendered — a bare simple name or the
     *                   #247 sentinel, never a generic or a wildcard (the walk records simple names; witnessed
     *                   corpus-wide by {@code AliasSeamSignatureSeatTest.corpus_c2}); null when absent
     */
    public record AliasSeam(Form form, boolean wildcarded, String element) {
        /** The seam's form; {@link #UNKNOWN} is the legacy-constructor default and never a Mapper seam. */
        public enum Form { MAPPER_SINGLE, MAPPER_MULTI, BUILDER, BUILDER_LIST, VALUE_SINGLE, VALUE_MULTI, UNKNOWN }

        /** The legacy-constructor default: no producer recorded the facts (unit fixtures built by hand). */
        public static final AliasSeam UNKNOWN = new AliasSeam(Form.UNKNOWN, false, null);

        /** A Mapper seam — {@code MapperC} when {@code multi}, {@code ? extends} when {@code wildcarded}. */
        public static AliasSeam mapper(boolean multi, boolean wildcarded, String element) {
            return new AliasSeam(multi ? Form.MAPPER_MULTI : Form.MAPPER_SINGLE, wildcarded, element);
        }

        /** A usesOutput builder-form seam ({@code <Out>.<Out>Builder}, {@code List<…>} when {@code multi}). */
        public static AliasSeam builder(boolean multi) {
            return new AliasSeam(multi ? Form.BUILDER_LIST : Form.BUILDER, false, null);
        }

        /** True for a {@code MapperS} / {@code MapperC} seam (never a builder, value or unknown form). */
        public boolean isMapper() {
            return form == Form.MAPPER_SINGLE || form == Form.MAPPER_MULTI;
        }

        public boolean isMapperSingle() {
            return form == Form.MAPPER_SINGLE;
        }

        public boolean isMapperMulti() {
            return form == Form.MAPPER_MULTI;
        }

        /** A Mapper seam's element spelling (bare or sentinel); null for an absent element or a non-Mapper seam. */
        public String mapperElementOrNull() {
            return isMapper() ? element : null;
        }

        /**
         * The optimised route's VALUE-typed re-seam of a Mapper seam (the § 6.3 flip: {@code MapperS<…>} →
         * the element, {@code MapperC<…>} → {@code List<…>}); the wildcard and element carry over. A non-Mapper
         * seam is returned unchanged (the flip policy never re-seams one).
         */
        public AliasSeam asValue() {
            if (form == Form.MAPPER_SINGLE) {
                return new AliasSeam(Form.VALUE_SINGLE, wildcarded, element);
            }
            if (form == Form.MAPPER_MULTI) {
                return new AliasSeam(Form.VALUE_MULTI, wildcarded, element);
            }
            return this;
        }
    }

    /** Represents a named alias (shorthand expression) defined inside a function. */
    public static class AliasModel {
        private final String name;
        private final String returnType;
        private final List<String> params;
        private final String compiledBody;
        private final boolean usesOutput;
        /**
         * PR-A §9.1 C2: Java classes referenced by the rendered alias body
         * (populated by FunctionExpressionRenderer via
         * RenderedStatement.refs()). On the STRUCTURAL phase-1 model
         * (FunctionAliasHelper.analyze) this instead carries the signature
         * walk's witness Java refs — today the concrete RJavaWithMetaValue
         * meta wrapper, which has no RType for the inferredRefs channel —
         * which FunctionGenerator.compileAliases unions into the rendered
         * body's refs (facet alias_method_signature_typing). Not exposed to
         * ST4 — consumed by FunctionGenerator for import collection.
         */
        private final Set<JavaClass<?>> refs;
        /**
         * PR-A §9.1 A3-D2-02: RTypes encountered while FunctionAliasHelper
         * walks the alias expression tree to infer its return type.
         * Translated to JavaClass&lt;?&gt; via
         * {@code JavaTypeTranslator.toJavaReferenceType} in FunctionGenerator
         * before feeding the import collector.
         */
        private final Set<RType> inferredRefs;
        /**
         * PR-A §9.1 C3c.1: static-wildcard imports referenced by the alias
         * body (populated from RenderedStatement.staticWildcardImports()).
         * Consumed by FunctionGenerator to feed imports.addStaticImport —
         * replaces the FunctionGenerator L844-849 substring-match path
         * deleted in C3c.2.
         */
        private final Set<JavaClass<?>> staticWildcardImports;
        /**
         * facet importCollisionFqn (PR #245): the dotted CANONICAL name of this alias's
         * signature return-type ELEMENT (a Rosetta model type / meta wrapper), or
         * {@code null} for primitive / usesOutput-builder / unresolved aliases. Carried
         * from the FunctionAliasHelper walk through both build phases so
         * {@code FunctionGenerator.buildStandardModel} can SEED the import-collision
         * resolver with it — the alias signatures are emitted ahead of the bodies, so a
         * same-simple-name body reference to a DIFFERENT canonical loses the first-claim
         * and renders FQN-inline. Not exposed to ST4.
         */
        private final String returnTypeElementFqn;
        /**
         * facet aliasSeamSignature (PR #612): the typed facts {@link #getReturnType()} was rendered from — see
         * {@link AliasSeam}. {@link AliasSeam#UNKNOWN} on the legacy constructors. Not exposed to ST4.
         */
        private final AliasSeam seam;

        /** 5-arg legacy constructor — defaults refs, inferredRefs, staticWildcardImports to empty. */
        public AliasModel(String name, String returnType, List<String> params,
                          String compiledBody, boolean usesOutput) {
            this(name, returnType, params, compiledBody, usesOutput, Set.of(), Set.of(), Set.of());
        }

        /** 7-arg legacy constructor — defaults staticWildcardImports to empty. */
        public AliasModel(String name, String returnType, List<String> params,
                          String compiledBody, boolean usesOutput,
                          Set<JavaClass<?>> refs, Set<RType> inferredRefs) {
            this(name, returnType, params, compiledBody, usesOutput, refs, inferredRefs, Set.of());
        }

        /** 8-arg constructor — PR-A §9.1 C3c.1; returnTypeElementFqn defaults null. */
        public AliasModel(String name, String returnType, List<String> params,
                          String compiledBody, boolean usesOutput,
                          Set<JavaClass<?>> refs, Set<RType> inferredRefs,
                          Set<JavaClass<?>> staticWildcardImports) {
            this(name, returnType, params, compiledBody, usesOutput, refs, inferredRefs,
                    staticWildcardImports, null);
        }

        /** 9-arg constructor — facet importCollisionFqn (PR #245): adds returnTypeElementFqn; seam defaults UNKNOWN. */
        public AliasModel(String name, String returnType, List<String> params,
                          String compiledBody, boolean usesOutput,
                          Set<JavaClass<?>> refs, Set<RType> inferredRefs,
                          Set<JavaClass<?>> staticWildcardImports, String returnTypeElementFqn) {
            this(name, returnType, params, compiledBody, usesOutput, refs, inferredRefs,
                    staticWildcardImports, returnTypeElementFqn, AliasSeam.UNKNOWN);
        }

        /** 10-arg canonical constructor — facet aliasSeamSignature (PR #612): adds the typed seam. */
        public AliasModel(String name, String returnType, List<String> params,
                          String compiledBody, boolean usesOutput,
                          Set<JavaClass<?>> refs, Set<RType> inferredRefs,
                          Set<JavaClass<?>> staticWildcardImports, String returnTypeElementFqn,
                          AliasSeam seam) {
            this.name = name;
            this.returnType = returnType;
            this.params = List.copyOf(params);
            this.compiledBody = compiledBody;
            this.usesOutput = usesOutput;
            this.refs = Set.copyOf(Objects.requireNonNull(refs, "refs"));
            this.inferredRefs = Set.copyOf(
                    Objects.requireNonNull(inferredRefs, "inferredRefs"));
            this.staticWildcardImports = Set.copyOf(
                    Objects.requireNonNull(staticWildcardImports, "staticWildcardImports"));
            this.returnTypeElementFqn = returnTypeElementFqn;
            this.seam = Objects.requireNonNull(seam, "seam");
        }

        public String getName() { return name; }
        public String getReturnType() { return returnType; }
        public List<String> getParams() { return params; }
        public String getCompiledBody() { return compiledBody; }
        public boolean getUsesOutput() { return usesOutput; }
        public Set<JavaClass<?>> getRefs() { return refs; }
        public Set<RType> getInferredRefs() { return inferredRefs; }
        public Set<JavaClass<?>> getStaticWildcardImports() { return staticWildcardImports; }
        public String getReturnTypeElementFqn() { return returnTypeElementFqn; }
        /** facet aliasSeamSignature (PR #612): the typed facts {@link #getReturnType()} was rendered from. */
        public AliasSeam getSeam() { return seam; }

        /**
         * facet aliasOutputBuilderNav (PR #381, W): the rendered parameter list of
         * BOTH alias method declarations (abstract + impl override) — stamped by the
         * {@link FunctionTemplateModel} constructor: a usesOutput alias joins its own
         * analyzed params (the output builder threads FIRST — golden cdm5
         * NewEquitySwapProduct), every other alias carries the function-input list
         * byte-identical to the pre-#381 template expansion
         * ({@code computeInputParamList}). Read by ST4 as {@code <a.paramsDecl>}.
         */
        public String getParamsDecl() { return paramsDecl; }

        private String paramsDecl;

        /** Return a copy with {@code paramsDecl} stamped (facet aliasOutputBuilderNav). */
        public AliasModel withParamsDecl(String decl) {
            AliasModel copy = new AliasModel(name, returnType, params, compiledBody, usesOutput,
                    refs, inferredRefs, staticWildcardImports, returnTypeElementFqn, seam);
            copy.paramsDecl = decl;
            return copy;
        }

        /** Return a copy with {@code returnTypeElementFqn} set (facet importCollisionFqn). */
        public AliasModel withReturnTypeElementFqn(String fqn) {
            AliasModel copy = new AliasModel(name, returnType, params, compiledBody, usesOutput,
                    refs, inferredRefs, staticWildcardImports, fqn, seam);
            copy.paramsDecl = this.paramsDecl;
            return copy;
        }

        /** Return a copy with the typed {@code seam} set (facet aliasSeamSignature, PR #612). */
        public AliasModel withSeam(AliasSeam typedSeam) {
            AliasModel copy = new AliasModel(name, returnType, params, compiledBody, usesOutput,
                    refs, inferredRefs, staticWildcardImports, returnTypeElementFqn, typedSeam);
            copy.paramsDecl = this.paramsDecl;
            return copy;
        }
    }

    /** Represents a single set/add operation statement inside a function body. */
    public static class OperationModel {
        /** "set" or "add" */
        private final String operator;
        /** Full rendered assignment statement. */
        private final String compiledStatement;
        /** Path segments describing the target (e.g. output.field.subField). */
        private final List<PathSegmentModel> targetPath;
        /** True when the operation uses the "as key" modifier. */
        private final boolean isAsKey;
        /**
         * PR-A §9.1 C2: Java classes referenced by the rendered statement.
         * Not exposed to ST4.
         */
        private final Set<JavaClass<?>> refs;
        /** PR-A §9.1 C3c.1: static-wildcard imports referenced by the rendered statement. */
        private final Set<JavaClass<?>> staticWildcardImports;

        /** 4-arg legacy constructor. */
        public OperationModel(String operator, String compiledStatement,
                              List<PathSegmentModel> targetPath, boolean isAsKey) {
            this(operator, compiledStatement, targetPath, isAsKey, Set.of(), Set.of());
        }

        /** 5-arg legacy constructor — defaults staticWildcardImports to empty. */
        public OperationModel(String operator, String compiledStatement,
                              List<PathSegmentModel> targetPath, boolean isAsKey,
                              Set<JavaClass<?>> refs) {
            this(operator, compiledStatement, targetPath, isAsKey, refs, Set.of());
        }

        /** 6-arg constructor — PR-A §9.1 C3c.1. */
        public OperationModel(String operator, String compiledStatement,
                              List<PathSegmentModel> targetPath, boolean isAsKey,
                              Set<JavaClass<?>> refs,
                              Set<JavaClass<?>> staticWildcardImports) {
            this.operator = operator;
            this.compiledStatement = compiledStatement;
            this.targetPath = List.copyOf(targetPath);
            this.isAsKey = isAsKey;
            this.refs = Set.copyOf(Objects.requireNonNull(refs, "refs"));
            this.staticWildcardImports = Set.copyOf(
                    Objects.requireNonNull(staticWildcardImports, "staticWildcardImports"));
        }

        public String getOperator() { return operator; }
        public String getCompiledStatement() { return compiledStatement; }
        public List<PathSegmentModel> getTargetPath() { return targetPath; }
        public boolean getIsAsKey() { return isAsKey; }
        public Set<JavaClass<?>> getRefs() { return refs; }
        public Set<JavaClass<?>> getStaticWildcardImports() { return staticWildcardImports; }
    }

    /** Represents a single segment in a target path expression (e.g. one step of output.field.sub). */
    public static class PathSegmentModel {
        private final String name;
        private final String typeName;
        private final boolean isMulti;
        private final boolean isMeta;

        public PathSegmentModel(String name, String typeName, boolean isMulti, boolean isMeta) {
            this.name = name;
            this.typeName = typeName;
            this.isMulti = isMulti;
            this.isMeta = isMeta;
        }

        public String getName() { return name; }
        public String getTypeName() { return typeName; }
        public boolean getIsMulti() { return isMulti; }
        public boolean getIsMeta() { return isMeta; }
    }

    /** Represents a pre- or post-condition on a function. */
    public static class ConditionModel {
        private final String name;
        private final String definition;
        private final String compiledExpression;
        private final Set<JavaClass<?>> refs;
        /** PR-A §9.1 C3c.1: static-wildcard imports referenced by the condition. */
        private final Set<JavaClass<?>> staticWildcardImports;

        /** 3-arg legacy constructor. */
        public ConditionModel(String name, String definition, String compiledExpression) {
            this(name, definition, compiledExpression, Set.of(), Set.of());
        }

        /** 4-arg legacy constructor — defaults staticWildcardImports to empty. */
        public ConditionModel(String name, String definition, String compiledExpression,
                              Set<JavaClass<?>> refs) {
            this(name, definition, compiledExpression, refs, Set.of());
        }

        /** 5-arg constructor — PR-A §9.1 C3c.1. */
        public ConditionModel(String name, String definition, String compiledExpression,
                              Set<JavaClass<?>> refs,
                              Set<JavaClass<?>> staticWildcardImports) {
            this.name = name;
            this.definition = definition;
            this.compiledExpression = compiledExpression;
            this.refs = Set.copyOf(Objects.requireNonNull(refs, "refs"));
            this.staticWildcardImports = Set.copyOf(
                    Objects.requireNonNull(staticWildcardImports, "staticWildcardImports"));
        }

        public String getName() { return name; }
        public String getDefinition() { return definition; }
        public String getCompiledExpression() { return compiledExpression; }
        public Set<JavaClass<?>> getRefs() { return refs; }
        public Set<JavaClass<?>> getStaticWildcardImports() { return staticWildcardImports; }
    }

    /** Represents an injected dependency (e.g., a called function or external service). */
    public static class DependencyModel {
        private final String fieldName;
        private final String typeName;
        private final String typeFqn;
        /** True when the dependency is a Rune function (as opposed to a service/validator). */
        private final boolean isFunction;

        public DependencyModel(String fieldName, String typeName, String typeFqn,
                               boolean isFunction) {
            this.fieldName = fieldName;
            this.typeName = typeName;
            this.typeFqn = typeFqn;
            this.isFunction = isFunction;
        }

        public String getFieldName() { return fieldName; }
        public String getTypeName() { return typeName; }
        public String getTypeFqn() { return typeFqn; }
        public boolean getIsFunction() { return isFunction; }
    }

    /** Represents a single case/variant in a dispatch function. */
    public static class DispatchVariantModel {
        private final String enumValue;
        private final String variantClassName;
        /** The field name for the @Inject declaration (variant class name with first letter lowercase). */
        private final String fieldName;
        private final FunctionTemplateModel variantModel;

        public DispatchVariantModel(String enumValue, String variantClassName,
                                    String fieldName, FunctionTemplateModel variantModel) {
            this.enumValue = enumValue;
            this.variantClassName = variantClassName;
            this.fieldName = fieldName;
            this.variantModel = variantModel;
        }

        public String getEnumValue() { return enumValue; }
        public String getVariantClassName() { return variantClassName; }
        public String getFieldName() { return fieldName; }
        public FunctionTemplateModel getVariantModel() { return variantModel; }
    }
}
