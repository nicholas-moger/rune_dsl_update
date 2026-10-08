package com.regnosys.rosetta.generator.java.reports;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.ROperation;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RCardinality;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.JavaClassGenerator;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.handlers.NavigationHandler;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.scoping.JavaClassScope;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RGeneratedJavaClass;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.SymbolId;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RChoiceTypeRef;
import com.regnosys.rosetta.types.RDataTypeRef;
import com.regnosys.rosetta.types.REnumTypeRef;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.RType;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.reports.ReportFunction;
import com.rosetta.util.types.JavaGenericTypeDeclaration;
import com.rosetta.util.types.JavaParameterizedType;
import com.rosetta.util.types.JavaType;
import com.rosetta.util.types.JavaTypeArgument;

/**
 * Phase X T5 — port of upstream {@code RuleGenerator.xtend}
 * ({@code rune-dsl-9.83.0-line upstream/rune-lang/.../generator/java/reports/RuleGenerator.xtend};
 * 32 LOC). Mirrors upstream's {@code reports} package placement per
 * {@code rune-dsl 9.83.0-line upstream RuleGenerator.xtend} — the fork keeps the
 * upstream-aligned location so future japicmp Layer 3 comparisons remain
 * structurally aligned. ({@code LabelProviderGenerator} at T3 sits under
 * {@code function.*} because upstream {@code LabelProviderGenerator.xtend}
 * lives under {@code function/}; the divergence between the two generators
 * mirrors the upstream layout.)
 *
 * <p>Thin shim that filters {@link RRule} root elements from each
 * {@link RModel}, bridges each to a synthetic {@link RFunction} via
 * {@link RFunction#fromRule(RRule)} (T2), and delegates the emission to
 * {@link FunctionGenerator#buildClassWithBaseInterface} (T4) with
 * {@code ReportFunction<I,O>} as the base interface (replacing the standard
 * {@code RosettaFunction} clause).
 *
 * <p><b>Fork divergences vs upstream</b> — captured here once instead of at
 * every consumer site:
 * <ul>
 *   <li><b>No {@code RObjectFactory}.</b> Upstream's {@code streamObjects}
 *       converts each {@code RosettaRule} via
 *       {@code rObjectFactory.buildRFunction(rule)}; the fork uses
 *       {@link RFunction#fromRule(RRule)} (added at Phase X T2).</li>
 *   <li><b>{@code toMetaJavaType} 2-arg signature.</b> Upstream's accessor is
 *       1-arg ({@code rAttr.toMetaJavaType}) — the fork's
 *       {@link JavaTypeTranslator#toMetaJavaType(RAttribute, com.regnosys.rosetta.types.RType)}
 *       is 2-arg because {@link RAttribute} lacks the upstream
 *       {@code getRMetaAnnotatedType().getRType()} accessor (T2.5). The
 *       second argument is the pre-resolved {@code RType}, fetched here via
 *       {@link GeneratorModel#getType(RAttribute)}.</li>
 *   <li><b>No {@code RFunction.symbolId()} instance accessor.</b> The fork's
 *       {@link GeneratorModel#symbolId(RFunction)} requires the
 *       {@code RFunction} to be {@code RModel}-attached for namespace
 *       resolution. The synthetic {@code RFunction} produced by
 *       {@link RFunction#fromRule(RRule)} has no parent attachment, so we
 *       recover by reading namespace + name off the source
 *       {@linkplain RFunction#originRule() origin rule} (which IS
 *       {@code RModel}-attached). Mirrors {@code LabelProviderGenerator}'s
 *       {@code originReport()} bridge at T3 verbatim.</li>
 *   <li><b>{@code JavaParameterizedType.from} 2-arg overload.</b> Upstream's
 *       Xtend uses {@code TypeReference<ReportFunction<?,?>>}; the fork's
 *       {@link JavaParameterizedType#from(JavaGenericTypeDeclaration, JavaTypeArgument...)}
 *       requires a {@link JavaGenericTypeDeclaration} as the raw-type marker.
 *       {@code JavaClass.from(ReportFunction.class)} would NOT compile
 *       against that overload (a {@link com.rosetta.util.types.JavaClass}
 *       does not extend {@link JavaGenericTypeDeclaration}). We use
 *       {@link JavaGenericTypeDeclaration#from(Class)} instead. Same
 *       discipline as {@code FunctionGeneratorTest}'s 2-arg parameterized
 *       base interface construction at T4.</li>
 *   <li><b>No upstream {@code @Inject} DI.</b> Constructor-injected by every
 *       wiring site, same pattern as {@code LabelProviderGenerator} (T3)
 *       and {@code ChoiceObjectGenerator} (P2.1.3c).</li>
 * </ul>
 *
 * <p>NOT yet wired into {@code JavaCodeGenerator}'s per-model generator list
 * — that's Phase X T7 (deferred until {@code ReportGenerator} also lands at
 * T6). This class is complete on its own and exercised directly by
 * {@code RuleGeneratorTest}.
 */
public class RuleGenerator
        extends JavaClassGenerator<RFunction, RGeneratedJavaClass<? extends RosettaFunction>> {

    private final GeneratorModel generatorModel;
    private final JavaTypeTranslator typeTranslator;
    private final FunctionGenerator functionGenerator;

    /**
     * Construct with the three dependencies upstream injects via Guice.
     *
     * @param generatorModel the per-workspace generator-model bridge (used for
     *        attribute-type resolution + symbol-id lookup via the
     *        {@link RFunction#originRule() originRule} bridge)
     * @param typeTranslator the Java-class translator (used to derive the
     *        generated class FQN via
     *        {@link JavaTypeTranslator#toFunctionJavaClass} +
     *        meta-typed argument resolution for the
     *        {@code ReportFunction<I,O>} type parameters via
     *        {@link JavaTypeTranslator#toMetaJavaType(RAttribute, com.regnosys.rosetta.types.RType)})
     * @param functionGenerator the function generator (delegated to for the
     *        actual emission via
     *        {@link FunctionGenerator#buildClassWithBaseInterface} — T4)
     */
    public RuleGenerator(GeneratorModel generatorModel,
                         JavaTypeTranslator typeTranslator,
                         FunctionGenerator functionGenerator) {
        this.generatorModel = Objects.requireNonNull(generatorModel, "generatorModel");
        this.typeTranslator = Objects.requireNonNull(typeTranslator, "typeTranslator");
        this.functionGenerator = Objects.requireNonNull(functionGenerator, "functionGenerator");
    }

    /**
     * seat 8 (facet ruleOutputCardFormSeats): the compiler the shared
     * {@link NavigationHandler#ruleOutputProvesMulti} predicate walks with —
     * built lazily from the SAME (generatorModel, typeTranslator) pair this
     * generator holds (the {@code FunctionGenerator.createExpressionCompiler()}
     * factory pattern), so the signature back-fill below and every re-pointed
     * render consult read ONE predicate (the #367 shared-predicate law: the
     * {@code ReportFunction<I,O>} type-arg and the body's MapperC wraps and
     * terminal derefs may never disagree).
     */
    private ExpressionCompiler overlayCompiler;

    private ExpressionCompiler overlayCompiler() {
        if (overlayCompiler == null) {
            overlayCompiler = new ExpressionCompiler(
                    generatorModel, typeTranslator, new JavaTypeUtil());
        }
        return overlayCompiler;
    }

    /**
     * Filter {@link RRule} root elements + bridge each to a synthetic
     * {@link RFunction} via {@link RFunction#fromRule(RRule)}. Mirrors
     * upstream {@code RuleGenerator.streamObjects} verbatim.
     */
    @Override
    protected Stream<? extends RFunction> streamObjects(RModel model) {
        return model.rootElements().stream()
                .filter(e -> e instanceof RRule)
                .map(e -> RFunction.fromRule((RRule) e));
    }

    /**
     * Compute the generated {@code <RuleName>Rule} class via the principled
     * origin-dispatched
     * {@link JavaTypeTranslator#toFunctionJavaClass(RFunction, ModelSymbolId)}
     * entry point (T6.0.5). Mirrors upstream
     * {@code RuleGenerator.createTypeRepresentation} which dispatches via
     * {@code RFunction.getOrigin() == RULE → toJavaRuleClass} (rune-dsl 9.83.0-line upstream
     * lines 127-131).
     *
     * <p><b>RULE-origin routing</b> places the class under
     * {@code <namespace>.reports/<Name>Rule} (note: {@code .reports}, not
     * {@code .functions} — corrected at T6.0.5; the pre-T6.0.5 T5 RuleGenerator
     * routed to {@code .functions} which was wrong).
     *
     * <p><b>Synthetic-RFunction namespace recovery:</b>
     * {@link RFunction#fromRule(RRule)} produces an in-memory-only synthetic
     * with no {@code RModel} parent (NOT linker-attached). The standard
     * {@code generatorModel.symbolId(function)} path handles the recovery by
     * reading the namespace off the {@linkplain RFunction#originRule() origin rule}
     * (which IS {@code RModel}-attached) + composing the rule's name into a
     * fresh {@link ModelSymbolId} — see {@link GeneratorModel#symbolId(RFunction)}
     * Phase X T5 javadoc for the recovery semantics. The principled dispatcher
     * uses the resolved {@link ModelSymbolId} to compose the
     * {@code <namespace>.reports/<Name>Rule} class identity.
     */
    @Override
    protected RGeneratedJavaClass<? extends RosettaFunction> createTypeRepresentation(
            RFunction rFunction) {
        // Principled origin-dispatched routing per T6.0.5 — RFunction.origin()
        // == RULE routes to <namespace>.reports/<Name>Rule via the private
        // toJavaRuleClass router inside JavaTypeTranslator.
        return typeTranslator.toFunctionJavaClass(
                rFunction, generatorModel.symbolId(rFunction));
    }

    /**
     * Emit the rule class. Construct the {@code ReportFunction<I,O>} base
     * interface from the synthetic function's first input + output
     * meta-typed attributes, then delegate to
     * {@link FunctionGenerator#buildClassWithBaseInterface} with
     * {@code renderAsReportFunction=true}. Mirrors upstream
     * {@code RuleGenerator.generateClass}.
     */
    @Override
    @SuppressWarnings("rawtypes") // JavaParameterizedType<ReportFunction> — the
                                  // ReportFunction<I,O> type-parameter shape
                                  // is intentionally erased here because the
                                  // parameterization comes from the inputMeta /
                                  // outputMeta JavaType arguments passed to
                                  // JavaParameterizedType.from, NOT from the
                                  // raw-type marker. Same idiom as the
                                  // FunctionGeneratorTest stand-in at
                                  // buildClassWithBaseInterface_emitsBaseInterfaceInClassDecl.
    protected String generate(RFunction rFunction,
                              RGeneratedJavaClass<? extends RosettaFunction> clazz,
                              String version) {
        if (rFunction.inputs().isEmpty()) {
            throw new IllegalStateException(
                    "RuleGenerator: synthetic RFunction has no input attribute — "
                    + "RFunction.fromRule(RRule) should always set one. function name="
                    + rFunction.name());
        }
        RAttribute input = rFunction.inputs().get(0);
        RAttribute output = rFunction.output().orElseThrow(() -> new IllegalStateException(
                "RuleGenerator: synthetic RFunction has no output attribute — "
                + "RFunction.fromRule(RRule) should always set one. function name="
                + rFunction.name()));

        // Phase X1 § 4.3 — back-fill output.typeCall + cardinality from rule
        // expression's inferred type. The synthetic RFunction's output attribute
        // is created in RFunction.fromRule with no typeCall (the upstream
        // RObjectFactory equivalent has the same shape); without back-fill,
        // generatorModel.getType(output) returns RMissingType.INSTANCE which
        // maps to Object in the emission. The M3 implicit-item resolution at
        // T2.0 (Categories 8+9+10) ensures the workspace's type-inference
        // engine resolves bare-attribute references inside implicit inline
        // bodies before this back-fill reads them.
        RType inferredOutputType = null;
        if (!rFunction.operations().isEmpty()) {
            ROperation soleOp = rFunction.operations().get(0);
            RExpression expr = soleOp.expression();
            // R17 F1 — ROperation.expression() returns a raw nullable RExpression
            // (not an Optional). The back-fill path immediately dereferences it
            // for type inference + diagnostic detail, so an op without an
            // expression would NPE here and hide the intended fail-fast. Surface
            // it explicitly with rule + op metadata so the fix is obvious.
            if (expr == null) {
                throw new IllegalStateException(
                        "Phase X1 back-fill: rule-origin function '" + rFunction.name()
                                + "' carries an operation with no expression "
                                + "(operator=" + soleOp.operator()
                                + ", segment=" + soleOp.segment().map(s -> s.getClass().getSimpleName()).orElse("EMPTY")
                                + "). RuleGenerator back-fill requires soleOp.expression() to be set; "
                                + "check RFunction.fromRule / fromReport synthesis.");
            }
            RWorkspace ws = generatorModel.workspace();
            RMetaAnnotatedType inferred = ws.getInferredType(expr);
            if (inferred.isMissing()) {
                String extra = "";
                if (expr instanceof com.regnosys.rosetta.ast.expressions.references.RSymbolReference ref) {
                    extra = "; symbol(): " + ref.symbol()
                            .map(s -> s.getClass().getSimpleName()
                                    + (s instanceof com.regnosys.rosetta.ast.functions.RRule r ? "(" + r.name() + ")" : ""))
                            .orElse("EMPTY")
                            + "; name: " + ref.name()
                            + "; argsCount: " + ref.args().size();
                } else if (expr instanceof com.regnosys.rosetta.ast.expressions.unary.RExtractExpr ext) {
                    var body = ext.body();
                    if (body != null && body.body() != null) {
                        var inner = body.body();
                        extra = "; ext.body.body class: " + inner.getClass().getSimpleName();
                        if (inner instanceof com.regnosys.rosetta.ast.expressions.references.REnumValueRef enr) {
                            extra += "; enr.enumName: " + enr.enumName()
                                    + "; enr.valueName: " + enr.valueName()
                                    + "; enr.enumeration: " + enr.enumeration().map(en -> en.name()).orElse("EMPTY")
                                    + "; enr.enumValue: " + enr.enumValue().map(v -> v.name()).orElse("EMPTY")
                                    + "; enr.resolvedAttributeChain: " + enr.resolvedAttributeChain().map(c -> c.getClass().getSimpleName() + "(feature=" + (c.feature() == null ? "null" : c.feature().name()) + ")").orElse("EMPTY");
                        }
                        if (inner instanceof com.regnosys.rosetta.ast.expressions.references.RFeatureCall fc) {
                            extra += "; fc.featureName: " + fc.featureName()
                                    + "; fc.resolvedFeature: " + fc.resolvedFeature().map(a -> a.getClass().getSimpleName() + "(" + a.name() + ")").orElse("EMPTY")
                                    + "; fc.left class: " + (fc.left().isPresent() ? fc.left().get().getClass().getSimpleName() : "EMPTY");
                            if (fc.left().isPresent() && fc.left().get() instanceof com.regnosys.rosetta.ast.expressions.references.RFeatureCall innerFc) {
                                extra += "; inner.featureName: " + innerFc.featureName()
                                        + "; inner.resolved: " + innerFc.resolvedFeature().map(a -> a.getClass().getSimpleName() + "(" + a.name() + ")").orElse("EMPTY");
                                if (innerFc.left().isPresent()) {
                                    extra += "; inner.left class: " + innerFc.left().get().getClass().getSimpleName();
                                }
                            }
                            if (fc.resolvedFeature().isPresent()) {
                                var attr = fc.resolvedFeature().get();
                                var tc = attr.typeCall();
                                extra += "; outer.attr.typeCall: " + (tc == null ? "NULL" : ("typeName=" + tc.typeName() + ", referencedType=" + tc.referencedType().map(n -> n.getClass().getSimpleName()).orElse("EMPTY")));
                            }
                        }
                    } else {
                        extra = "; ext.body or ext.body.body is NULL";
                    }
                }
                throw new IllegalStateException(
                        "RuleGenerator: rule '" + rFunction.name()
                        + "' expression has unresolved type — workspace type "
                        + "inference returned RMissingType. Cannot back-fill "
                        + "output.typeCall; emission would default to Object. "
                        + "expression AST class: " + expr.getClass().getSimpleName()
                        + "; sourceRange: " + expr.sourceRange()
                        + extra
                        + ". (M3 Categories 8/9/10 should resolve implicit-input "
                        + "feature-calls; investigate the unresolved expression "
                        + "shape if this fires.)");
            }
            // Back-fill output.typeCall from the inferred type. Two consumers
            // read it, and BOTH are now satisfied:
            //   1. The rule-emission base interface (ReportFunction<I,O>) uses
            //      `inferredOutputType` directly below (skipping the
            //      workspace-search round-trip), so the name-only resolveTypeCall
            //      ambiguity flagged by Copilot PR #76 R11 F2 is bypassed there.
            //   2. The emitted method signatures (evaluate/doEvaluate/assignOutput
            //      return + the `output` local) flow through
            //      buildStandardModel → resolveParam → getType(output) →
            //      resolveTypeCall, whose name-only branch is the
            //      shouldGenerate-filtered workspace search — which MISSES (→
            //      RMissingType → Object) for output types in an emission-excluded
            //      (transitive-CDM) namespace. So we set typeName AND attach a
            //      resolved referencedTypeId (Engine PR #10 / facet F1) so
            //      resolveTypeCall resolves via workspace.resolveTypeLike before
            //      the filtered name-search. Output-side mirror of the input
            //      fromType referencedTypeId re-attach below (Gap #1).
            RTypeCall outTc = new RTypeCall();
            outTc.setTypeName(inferred.type().name());
            attachOutputReferencedTypeId(outTc, inferred.type(), ws);
            // round 1 (cq SF-5): the synthetic output type call is ATTACHED to the workspace its id names
            // (RFunction.fromReport's twin) - an id on a detached node would throw at its first
            // referencedType() read and be rendered as a TODO comment; no consumer reads it that way today
            outTc.attachToWorkspace(ws);
            output.setTypeCall(outTc);
            inferredOutputType = inferred.type();

            // facet reportOutputCardinality (PR #272) — use the rule-output
            // (then-aware) cardinality, not the global getCardinality. A
            // reporting rule whose body is a multi `filter … then extract …`
            // chain is multi-valued; the global getCardinality reads a `then`
            // pipe as conservative SINGLE (CardinalityComputer's historical
            // default the function tail depends on), so the synthetic output
            // stayed single and the ReportFunction<I,O> emitted the bare `X`
            // type-arg + a `.get()` body terminal where golden emits `List<X>`
            // + `.getMulti()`. getRuleBodyCardinality threads the `then` pipe
            // faithfully (rule-scoped — the function tail's then-rendering is
            // untouched, FUNCTION-byte-neutral by construction).
            // seat 8: re-pointed to the shared ruleOutputProvesMulti predicate
            // (engine-first, so the #272 behavior is byte-identical wherever the
            // choice-option overlay is silent; the drr 7.x
            // `then extract payout -> CommodityPayout` spine — engine-SINGLE via
            // the deliberate resolvedChoiceOption under-fire — proves MULTI here
            // and the whole evaluate surface goes List, matching golden).
            if (NavigationHandler.ruleOutputProvesMulti(expr, overlayCompiler())) {
                RCardinality multi = new RCardinality();
                multi.setInf(0);
                multi.setUnbounded(true);
                output.setCardinality(multi);
            }
        }

        // Phase X1 (Gap #1) — back-fill the synthetic input's resolved type id
        // from the origin rule's fromType. RFunction.fromRule deep-copies the
        // rule's `from` typeCall but RTypeCall.deepCopy drops the resolved
        // referencedTypeId, so generatorModel.getType(input) would fall into the
        // shouldGenerate-filtered workspace-search-by-name and emit Object for
        // from-types in emission-excluded namespaces (CDM transitive deps like
        // SettlementTerms). Re-attaching the origin's referencedTypeId lets
        // resolveTypeCall resolve it via the workspace (bypassing the filter).
        // Symmetric with the output.typeCall back-fill above.
        if (input.typeCall() != null) {
            rFunction.originRule()
                    .flatMap(RRule::fromType)
                    .flatMap(RTypeCall::referencedTypeId)
                    .ifPresent(input.typeCall()::setReferencedTypeId);
        }

        JavaType inputMetaType = typeTranslator.toMetaJavaType(input,
                generatorModel.getType(input));
        // R11 F2: pass the inferred RType directly when we computed it from
        // the rule's expression. Falling back to `generatorModel.getType(output)`
        // would re-resolve through `output.typeCall()` — which, for the
        // back-filled typeCall above, drops into the workspace-search fallback
        // (name-only, non-deterministic when multiple namespaces share a
        // simple type name). Using the already-known `inferredOutputType`
        // here avoids that ambiguity entirely on the rule emission path.
        RType outputRType = inferredOutputType != null
                ? inferredOutputType
                : generatorModel.getType(output);
        JavaType outputMetaType = typeTranslator.toMetaJavaType(output, outputRType);
        // facet reportOutputCardinality (PR #272) — toMetaJavaType returns the
        // SINGULAR item type (its cardinality List<> wrapping is deferred to
        // this T5 caller). For a multi-valued reporting rule (output back-filled
        // to MULTI above via the then-aware getRuleBodyCardinality) the
        // ReportFunction<I,O> output type-arg must be List<O> to match golden +
        // the emitted method signatures (which already List<>-wrap a multi
        // output through FunctionGenerator's isMulti(output) standard-model
        // path). Without this the base interface stayed ReportFunction<I, O>
        // while the signatures rendered List<O> — an internal inconsistency that
        // never matched golden's uniformly-List<> shape.
        if (generatorModel.isMulti(output)) {
            // Polymorphic `List<? extends O>` for a MODEL-typed element (a data /
            // choice type, e.g. PricePeriod / NotionalPeriod — the Schedule rules);
            // invariant `List<O>` for a primitive / enum / string element (Boolean /
            // String — the Counterparty rules). Tested at the RTYPE level per the
            // #169 lesson (RGeneratedJavaClass is not a JavaPojoInterface).
            boolean modelTyped = outputRType instanceof RDataTypeRef
                    || outputRType instanceof RChoiceTypeRef;
            outputMetaType = typeTranslator.listWrap(outputMetaType, modelTyped);
        }

        // ReportFunction<I,O> raw-type marker. JavaClass.from(ReportFunction.class)
        // would NOT match JavaParameterizedType.from's
        // (JavaGenericTypeDeclaration<? super T>, JavaTypeArgument...) overload
        // — JavaClass does not extend JavaGenericTypeDeclaration. Use the
        // genericDecl helper instead (same idiom as
        // FunctionGeneratorTest#buildClassWithBaseInterface_emitsBaseInterfaceInClassDecl).
        JavaGenericTypeDeclaration<ReportFunction> rfDecl =
                JavaGenericTypeDeclaration.from(ReportFunction.class);
        JavaParameterizedType<ReportFunction> baseInterface = JavaParameterizedType.from(
                rfDecl, (JavaTypeArgument) inputMetaType, (JavaTypeArgument) outputMetaType);

        JavaClassScope scope = JavaClassScope.createAndRegisterIdentifier(clazz);
        return functionGenerator.buildClassWithBaseInterface(
                rFunction, clazz,
                /* isAbstract — reserved per spec § 3.3 forward-compat;
                 * FunctionGenerator currently emits 'public abstract class'
                 * unconditionally per its buildClassWithBaseInterface javadoc
                 * (@param isAbstract: "reserved for future use by T5/T6");
                 * this slot reserves the future-state argument shape so the
                 * non-abstract rule + report classes can be emitted without
                 * a downstream signature change. */ false,
                List.<JavaParameterizedType>of(baseInterface),
                Map.<Class<?>, String>of(),
                /* renderAsReportFunction */ true, scope);
    }

    /**
     * Engine PR #10 (facet F1) — mint a resolved {@link SymbolId} for the
     * back-filled output {@link RTypeCall} from the rule expression's inferred
     * type. Output-side mirror of the input {@code fromType} {@code referencedTypeId}
     * re-attach in {@link #generate} (Gap #1).
     *
     * <p><b>Why.</b> The output {@code typeCall} is back-filled with the
     * {@code typeName} only. The base interface {@code ReportFunction<I,O>} is
     * correct because {@link #generate} feeds it {@code inferredOutputType}
     * directly — but the emitted method signatures
     * ({@code evaluate}/{@code doEvaluate}/{@code assignOutput} return + the
     * {@code output} local) flow through
     * {@code FunctionGenerator.buildStandardModel} → {@code resolveParam(output)}
     * → {@code GeneratorModel.getType(output)} →
     * {@code GeneratorModel.resolveTypeCall}, whose name-only branch is the
     * {@code shouldGenerate}-filtered workspace search. For output types in an
     * emission-EXCLUDED namespace (the transitive-CDM shape — e.g. drr
     * currency-leg rules whose output enum {@code ISOCurrencyCodeEnum} lives in
     * {@code cdm.base.staticdata.asset.common}) that search misses and the type
     * collapses to {@code RMissingType} → {@code Object}. Attaching a resolvable
     * {@code referencedTypeId} routes {@code resolveTypeCall} through
     * {@code workspace.resolveTypeLike} BEFORE the filtered name-search.
     *
     * <p><b>How.</b> Only the declared-type variants (data / enum / choice) carry
     * a declaration node and need this; basic / number / string / record types —
     * and aliases resolvable via {@code BUILTINS} / the known string-alias map —
     * resolve by name already. (A <i>user-defined</i> alias whose declaration sits
     * in an emission-excluded namespace is the same failure class but is not
     * reached today — the D11 oracle reports zero such rules in the corpus — so it
     * is left to {@code default -> null}; revisit if a flipping file needs it.)
     * The {@link SymbolId} is minted with the
     * declaration model's own namespace (the {@code namespaces}-map key the
     * linker uses), the type's simple name, and the current workspace
     * {@code generation()} — matching {@code GlobalResolutionPass.symbolIdOf} so
     * {@code RWorkspace.resolveTypeLike} resolves it.
     */
    private static void attachOutputReferencedTypeId(RTypeCall outTc, RType outputType,
                                                     RWorkspace ws) {
        RRootElement decl = switch (outputType) {
            case RDataTypeRef d -> d.astNode();
            case REnumTypeRef e -> e.astNode();
            case RChoiceTypeRef c -> c.astNode();
            default -> null;
        };
        if (decl != null && decl.parent() instanceof RModel m) {
            outTc.setReferencedTypeId(SymbolId.of(m.namespace(), outputType.name(), ws.generation()));
        }
    }
}
