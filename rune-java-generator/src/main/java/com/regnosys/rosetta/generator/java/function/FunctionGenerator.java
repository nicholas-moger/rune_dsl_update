package com.regnosys.rosetta.generator.java.function;

import com.regnosys.rosetta.generator.java.SilentDegradation;
import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.enums.ListOp;
import com.regnosys.rosetta.ast.expressions.unary.RCountExpr;
import com.regnosys.rosetta.ast.expressions.unary.RListOpExpr;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.functions.RDispatch;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.ROperation;
import com.regnosys.rosetta.ast.functions.RPostCondition;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.enums.EnumHelper;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.generator.java.template.ImportCollector;
import com.regnosys.rosetta.generator.java.template.ImportCollisionResolver;
import com.regnosys.rosetta.generator.java.template.TemplateRenderer;
import com.regnosys.rosetta.generator.java.scoping.JavaClassScope;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RGeneratedJavaClass;
import com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue;
import com.regnosys.rosetta.types.RType;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.util.DottedPath;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaParameterizedType;
import com.rosetta.util.types.JavaReferenceType;
import com.rosetta.util.types.JavaWildcardTypeArgument;
import com.rosetta.util.types.JavaType;
import com.rosetta.util.types.JavaTypeArgument;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Orchestrator for generating Java function classes from Rune DSL function
 * declarations. Iterates all {@link RFunction} nodes from the workspace,
 * groups dispatch variants together, builds {@link FunctionTemplateModel}
 * for each function, and renders via ST4 templates.
 *
 * <p>Not a {@code JavaClassGenerator} — functions are workspace-wide (like
 * metafield wrappers), not per-model. Called once by {@code JavaCodeGenerator}
 * after per-model generators and metafield generation.
 *
 * <p>Spec: M7b-3 Tasks 7-8 in the development plan "2026-04-14-m7b-expression-compiler-plan"
 */
public class FunctionGenerator {

    private static final String FUNCTION_TEMPLATE_GROUP = "templates/java-function.stg";
    private static final String DISPATCH_TEMPLATE_GROUP = "templates/java-function-dispatch.stg";

    // protected (not private) so the IR-routed subclass (the D43 seam) can read them from its
    // createExpressionCompiler() override. The constructor invokes that factory (see below);
    // these three are assigned before the invocation, so the override observes them fully set.
    protected final GeneratorModel generatorModel;
    protected final JavaTypeTranslator typeTranslator;
    protected final JavaTypeUtil typeUtil;
    private final FunctionDependencyCollector dependencyCollector;
    private final FunctionAliasHelper aliasHelper;
    private final FunctionExpressionRenderer expressionRenderer;
    private final TemplateRenderer templateRenderer;
    // facet labelProviderImplicit (PR #211): the upstream gate that decides whether a
    // transform function ([ingest]/[enrich]/[projection]) gets an auto-generated
    // <Func>LabelProvider + an @RuneLabelProvider class annotation (mirrors upstream
    // FunctionGenerator.generateClass:122-125, which @Injects labelProviderUtil).
    private final LabelProviderGeneratorUtil labelProviderUtil;
    /**
     * v3.2 seat 3 (F12), round 2 (cq SF-3): the function files THIS generator wrote in the current
     * run, by path — the writer seam's own record. Cleared by {@link #generateWithErrors} ALONE
     * (round-3 cq N-6): the package-private writers {@code generateStandardFunction} /
     * {@code generateDispatchFunction} are test seams that share it, so two direct calls at one path
     * across two logical runs would refuse the second naming the first run's writer — every caller
     * outside this class goes through {@code generateWithErrors}, on both routes.
     */
    private final Map<String, RFunction> emittedThisRun = new LinkedHashMap<>();

    public FunctionGenerator(GeneratorModel generatorModel,
                             JavaTypeTranslator typeTranslator,
                             JavaTypeUtil typeUtil) {
        this.generatorModel = generatorModel;
        this.typeTranslator = typeTranslator;
        this.typeUtil = typeUtil;
        // facet deep_path_util_resolution (PR #205): give the dependency collector an
        // ExpressionCompiler (built from the same generatorModel/typeTranslator/typeUtil as the
        // renderer's) so the DeepPathUtil @Inject field resolves its receiver type gm-aware via
        // the SAME NavigationHandler.resolveDeepReceiverTypeName the renderer uses.
        this.dependencyCollector = new FunctionDependencyCollector(generatorModel, typeTranslator,
                createExpressionCompiler());
        this.aliasHelper = new FunctionAliasHelper(generatorModel, typeTranslator, typeUtil);
        this.expressionRenderer = createFunctionExpressionRenderer(
                createExpressionCompiler());
        this.templateRenderer = new TemplateRenderer();
        // facet labelProviderImplicit (PR #211): stateless transform-annotation gate —
        // constructed here (not @Injected) consistent with the other helpers above.
        this.labelProviderUtil = new LabelProviderGeneratorUtil();
        templateRenderer.loadGroupFromClasspath(FUNCTION_TEMPLATE_GROUP);
        templateRenderer.loadGroupFromClasspath(DISPATCH_TEMPLATE_GROUP);
    }

    /**
     * Factory for the {@link ExpressionCompiler} that compiles function-body expressions to
     * Java. Extracted as an overridable seam (the D43 IR seam; the lab's L-001/L-002) so the
     * IR-routed subclass can substitute an IR-aware compiler with strangler fallback, leaving
     * Path-1 (this base) byte-identical.
     *
     * <p><b>Construction-time invariant.</b> The constructor calls this once for the
     * dependency collector and once for the expression renderer. An override therefore runs
     * during super-construction — it MUST depend only on {@link #generatorModel},
     * {@link #typeTranslator} and {@link #typeUtil} (all assigned before the call) and on no
     * subclass state, since subclass fields are not yet initialised.
     *
     * @return a fresh expression compiler wired to this generator's type infrastructure
     */
    protected ExpressionCompiler createExpressionCompiler() {
        return new ExpressionCompiler(generatorModel, typeTranslator, typeUtil);
    }

    /**
     * Factory for the {@link FunctionExpressionRenderer} that renders function-body operations
     * (SET/ADD) — including the statement-position hoists (the
     * {@code ifThenElseResult}/{@code thenArg}/{@code boolean} locals) it intercepts
     * <em>upstream</em> of the {@link #createExpressionCompiler() expression compiler}.
     * Extracted as an overridable seam — symmetric to {@link #createExpressionCompiler()} — so
     * the IR-routed subclass can substitute an IR-driven renderer that drives those
     * SET-position hoists from the neutral ANF substrate (the lab's Wave-6 Phase C; the SET
     * hoists are renderer-intercepted, so the renderer is the only seam that can reach them —
     * the lab's L-053/L-055/L-056). The base returns the standard renderer unchanged, so
     * Path-1 is byte-identical.
     *
     * <p><b>Construction-time invariant</b> (as {@link #createExpressionCompiler()}): invoked
     * once from the constructor, so an override MUST depend only on its {@code compiler}
     * argument and the already-assigned
     * {@link #generatorModel}/{@link #typeTranslator}/{@link #typeUtil}, not on subclass state.
     *
     * @param compiler the expression compiler the renderer drives body expressions through
     *                 (this generator's {@link #createExpressionCompiler() compiler})
     * @return a fresh function-expression renderer wired to {@code compiler}
     */
    protected FunctionExpressionRenderer createFunctionExpressionRenderer(ExpressionCompiler compiler) {
        return new FunctionExpressionRenderer(compiler);
    }

    /**
     * The expression compiler that renders function-BODY expressions — i.e. the
     * {@code expressionRenderer}'s, NOT the dependency collector's (the constructor builds two;
     * only the renderer's output is the gated Java). Exposed (the D43 seam) so the IR-routed
     * subclass can surface its compiler's progress counters to the ON-ring gate. Additive;
     * Path-1 unaffected.
     */
    protected ExpressionCompiler renderingExpressionCompiler() {
        return expressionRenderer.expressionCompiler();
    }

    /**
     * The {@link FunctionExpressionRenderer} that renders this generator's function-body
     * operations (the one built by {@link #createFunctionExpressionRenderer}). Exposed (the D43
     * seam) so the IR-routed subclass can surface its renderer's hoist-driving counters to the
     * ON-ring gate — the firing-breadth measurement (the lab's anti-L-042 discipline).
     * Additive; Path-1 unaffected.
     */
    protected FunctionExpressionRenderer functionExpressionRenderer() {
        return expressionRenderer;
    }

    /**
     * Seam invoked at the START of every {@link #buildStandardModel} run — BEFORE any
     * operation/alias/condition body of the function renders. The optimised subclass
     * (U019 — the member-injection surface) resets its per-file helper-member
     * collection here, so members collected by an earlier build (including one
     * aborted by a per-function generation error) can never leak into this file.
     * Default: no-op — the reference pipeline is unaffected.
     */
    protected void onStandardModelBuildStart() {
    }

    /**
     * Seam invoked at the END of every {@link #buildStandardModel} run — AFTER every
     * body of the function has rendered — supplying the pre-rendered private members
     * appended inside the generated {@code <Name>Default} class body (the template's
     * {@code helperMethodsBlock} slot, immediately before the closing brace; both the
     * standard and the dispatch-variant templates carry the slot). The optimised
     * subclass (U019) drains its per-file helper registry here; the base returns
     * {@code ""}, so the slot renders ZERO bytes and the reference emission is
     * byte-identical.
     *
     * @param indentLevel the body-statement indent of this build
     *        ({@link FunctionExpressionRenderer#DEFAULT_INDENT_LEVEL} standard /
     *        4 dispatch-variant); injected members sit one level shallower
     */
    protected String buildHelperMethodsBlock(int indentLevel) {
        return "";
    }

    /**
     * Binary-compatibility-preserving overload of {@link #generateWithErrors(Map)}.
     * Calls {@link #generateWithErrors} and discards the returned per-function failure
     * list. Preserves the pre-R9 {@code public void generate(Map)} ABI for downstream
     * consumers compiled against earlier rune-java-generator artifacts (Copilot R10 F1
     * 2026-05-04 — restoring the void overload after R9's signature change had been
     * flagged as binary-incompatible).
     *
     * <p>New code should call {@link #generateWithErrors(Map)} directly to capture
     * per-function failures (the {@code JavaCodeGenerator} production caller and
     * {@code D11CorpusRegressionTest.function_comparison} both do).
     *
     * @apiNote Subclasses that wish to customize generation called from
     *          {@link com.regnosys.rosetta.generator.java.JavaCodeGenerator#generate()}
     *          must override {@link #generateWithErrors(Map)} (canonical path) or,
     *          for same-package subclasses only, the package-private helpers
     *          {@link #generateStandardFunction(RFunction, Map)} and
     *          {@link #generateDispatchFunction(String, List, RFunction, Map)} that
     *          {@code generateWithErrors} dispatches to (test seam — see
     *          method-level Javadoc on those helpers). Overriding this deprecated
     *          void overload does NOT influence production code generation —
     *          it calls {@code generateWithErrors} and discards the return,
     *          and only affects callers that explicitly invoke {@code generate(Map)}.
     *          The negative-case contract is locked by
     *          {@code JavaCodeGeneratorTest.production_calls_generateWithErrors_not_deprecated_generate_overload}
     *          (PR-1.5 Copilot R5 F3 regression test 2026-05-04;
     *          Copilot R11 F2 2026-05-04; helper-override path acknowledged at
     *          PR-1.5 Copilot R1 F2 2026-05-04).
     *
     * @param output mutable map of file paths to generated Java source
     * @deprecated since P1.7-PR1-R10 in favour of {@link #generateWithErrors(Map)};
     *             retained for ABI compatibility with downstream consumers.
     *             Not slated for removal — callers that genuinely want silent-discard
     *             semantics may keep using this wrapper.
     */
    @Deprecated(since = "P1.7-PR1-R10", forRemoval = false)
    public void generate(Map<String, String> output) {
        generateWithErrors(output);
    }

    /**
     * Generate all function classes into the output map.
     *
     * <p>Phase 1: Walk all models and group dispatch variants together, tracking
     * both dispatch variants and their base (non-dispatch) function declarations.
     * Phase 2: Generate standard (non-dispatch, non-dispatch-base) functions.
     * Phase 3: Generate dispatch functions (base + variants together).
     *
     * <p>Per-function failures are collected into the returned list rather than
     * thrown, mirroring {@link com.regnosys.rosetta.generator.java.JavaClassGenerator
     * JavaClassGenerator#generateClasses}. Callers must capture the return — silently
     * dropping it lets per-function failures vanish (Copilot R9 F1 architectural fix
     * 2026-05-04). The previous behaviour ({@code System.err.println}) treated all
     * exceptions as warnings and let the test path continue with partial output.
     *
     * <p>Method renamed from {@code generate(Map)} to {@code generateWithErrors(Map)}
     * at R10 to preserve ABI compatibility — the void {@link #generate(Map)} overload
     * is retained as a discarding wrapper for downstream consumers compiled against
     * pre-R9 artifacts (Copilot R10 F1 2026-05-04).
     *
     * <p>Phase 2 + Phase 3 catch blocks preserve {@link GenerationException} identity
     * for already-typed exceptions <em>only when</em> the inner exception carries
     * diagnostic payload — i.e. {@link GenerationException#getResourceUri()} or
     * {@link GenerationException#getContext()} is non-null. When BOTH payload fields
     * are null (e.g. bare {@code GenerationException}s from
     * {@code TemplateRenderer} carrying messages like {@code "ST4 runtime error..."}),
     * the catch block wraps the inner GE in a fresh {@code GenerationException}
     * to add this generator's per-function attribution prefix while preserving the
     * inner GE as cause. Generic (non-{@code GenerationException}) exceptions are
     * always wrapped with cause. Diverges from
     * {@code JavaClassGenerator#generateClasses}, which preserves identity for
     * all already-typed {@code GenerationException}s unconditionally (including
     * bare GEs without payload — both paths receive bare GEs from
     * {@code TemplateRenderer}, but only this path adds the payload-aware
     * attribution-preserving wrap; the per-model path's behaviour for bare GEs
     * is a latent attribution-loss case not in this PR's scope) (Copilot R11
     * F1+F3 2026-05-04; payload-aware refinement at PR-1.5 Copilot R1 F1+F3
     * 2026-05-04; divergence framing corrected at PR-1.5 Copilot R3 F1
     * 2026-05-04).
     *
     * @apiNote This is the canonical override point for subclasses that customize
     *          function-generation behaviour invoked from
     *          {@link com.regnosys.rosetta.generator.java.JavaCodeGenerator#generate()}.
     *          Same-package subclasses may alternatively override the package-private
     *          helpers {@link #generateStandardFunction(RFunction, Map)} and
     *          {@link #generateDispatchFunction(String, List, RFunction, Map)} that
     *          this method dispatches to — that alternative path is the test seam
     *          (see method-level Javadoc on those helpers) and equally affects
     *          production codegen because the dispatch is polymorphic.
     *          The deprecated void {@link #generate(Map)} overload calls this method
     *          and discards the return — overriding {@code generate(Map)} does NOT
     *          influence production code generation. The negative-case contract is
     *          locked by
     *          {@code JavaCodeGeneratorTest.production_calls_generateWithErrors_not_deprecated_generate_overload}
     *          (PR-1.5 Copilot R5 F5 regression test 2026-05-04;
     *          Copilot R11 F2 2026-05-04;
     *          helper-override path acknowledged at PR-1.5 Copilot R1 F4 2026-05-04).
     *
     * @param output mutable map of file paths to generated Java source
     * @return list of per-function generation failures (empty on full success)
     */
    public List<GenerationException> generateWithErrors(Map<String, String> output) {
        List<GenerationException> errors = new ArrayList<>();
        emittedThisRun.clear();

        // Phase 1: Group dispatch variants and find base functions — PER FILE.
        // v3.2 seat 3 (census family F12, the fix): a dispatch group is the set of same-named
        // variants declared in the SAME model file, and its base is that file's same-named
        // function without a dispatch clause — upstream RosettaFunctionExtensions
        // .getDispatchingFunctions / getMainFunction (EcoreUtil2.getSiblingsOfType: siblings of
        // one resource; a variant cannot see a base declared in another file at 9.83.0 — the
        // released plugin refuses the split, seat-3 census finding 4). The pre-seat map was keyed
        // by BARE NAME across every generated model, so same-named groups in different namespaces
        // folded into one (the chaos C4Speed ×13: one file at the last base's path carrying every
        // namespace's members, twelve functions silently gone) and a plain function that merely
        // shared a group's name in another namespace was misfiled as its "base" and dropped.
        Map<DispatchKey, List<RFunction>> dispatchGroups = new LinkedHashMap<>();
        Map<DispatchKey, RFunction> dispatchBases = new LinkedHashMap<>();
        List<RFunction> standardFunctions = new ArrayList<>();
        Map<RFunction, DispatchKey> keyOf = new LinkedHashMap<>();

        for (RModel model : generatorModel.files()) {
            if (!generatorModel.shouldGenerate(model)) continue;
            for (var element : model.rootElements()) {
                if (element instanceof RFunction func) {
                    DispatchKey key = new DispatchKey(model, func.name());
                    keyOf.put(func, key);
                    if (func.dispatch().isPresent()) {
                        dispatchGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(func);
                    } else {
                        standardFunctions.add(func);
                    }
                }
            }
        }

        // Separate dispatch base functions from truly standard functions.
        // A function with the same name as a dispatch group IN ITS OWN FILE but no
        // dispatch() is the base declaration (it declares inputs/output/annotations).
        List<RFunction> trueStandard = new ArrayList<>();
        for (RFunction func : standardFunctions) {
            DispatchKey key = keyOf.get(func);
            if (dispatchGroups.containsKey(key)) {
                dispatchBases.put(key, func);
            } else {
                trueStandard.add(func);
            }
        }

        // Phase 2: Generate standard functions. The catch is a BOUNDARY, not recovery
        // (the refusal-propagation lint's distinction): a Refusal is KEPT — collected
        // per function so the rest of the model still generates. Identity is preserved
        // for a GenerationException carrying diagnostic payload (resourceUri/context —
        // Refusal always does); bare GEs are wrapped for func-name attribution
        // (Copilot R11 F3 + PR-1.5 R1 F1/R3 F5, 2026-05-04). The capture records the
        // output path this function WOULD have emitted (waiver-key format, mirroring
        // JavaClassGenerator) so the waiver-aware D11 gate can tell a DECLARED refusal
        // (the v3.2 chaos rows) from a new one.
        for (RFunction func : trueStandard) {
            try {
                generateStandardFunction(func, output);
            } catch (Exception e) {
                GenerationException captured =
                        (e instanceof GenerationException ge
                                && (ge.getResourceUri() != null || ge.getContext() != null))
                        ? ge
                        : new GenerationException("FunctionGenerator: error generating "
                                + func.name() + " — " + e.getMessage(), null, null, e);
                captured.setTargetPath(computeFilePathSafe(func));
                errors.add(captured);
            }
        }

        // Phase 3: Generate dispatch functions (one per per-file group)
        for (var entry : dispatchGroups.entrySet()) {
            try {
                RFunction base = dispatchBases.get(entry.getKey());
                generateDispatchFunction(entry.getKey().name(), entry.getValue(), base, output);
            } catch (Exception e) {
                // Same payload-guarded preservation as Phase 2 (class-of-issue
                // sweep; Copilot R11 F1 2026-05-04 + PR-1.5 Copilot R1 F3 2026-05-04).
                GenerationException captured;
                if (e instanceof GenerationException ge
                        && (ge.getResourceUri() != null || ge.getContext() != null)) {
                    captured = ge;
                } else {
                    captured = new GenerationException(
                            "FunctionGenerator: error generating dispatch "
                                    + entry.getKey().name() + " — " + e.getMessage(),
                            null, null, e);
                }
                // attribution: the base's path, or the variants' (= the same class path)
                // for a baseless group — the DISPATCH_BASE_MISSING refusal's (round-1 cq SF-1)
                RFunction base = dispatchBases.get(entry.getKey());
                captured.setTargetPath(computeFilePathSafe(base != null ? base : entry.getValue().get(0)));
                errors.add(captured);
            }
        }

        // Phase 4 (v3.2 seat 3, census family F12 "silent-whole-function-no-emit" — the
        // COUNTER, landed before its fix): the accounting pass. Every function root element
        // of a generated model that is not a dispatch variant must now be accounted for by a
        // file in {@code output} or by an error in {@code errors}; one that is neither is the
        // silent class L4 exists to forbid, and it REFUSES here with its own target path so the
        // D11 gate reports a missing output with its reason instead of a bare hole. Measured
        // carrier: the chaos cell's C4Speed — thirteen same-named dispatch groups in thirteen
        // namespaces keyed by bare name above, one file emitted, twelve functions vanished
        // with every LOUD counter at zero (the seat-3 census, both routes).
        accountForEveryFunction(output, errors);

        return errors;
    }

    /**
     * v3.2 seat 3 (F12): the identity of a dispatch group — the declaring FILE (model identity,
     * never its namespace: a namespace may span files, and upstream groups by resource siblings)
     * and the function name.
     */
    private record DispatchKey(RModel model, String name) {
    }

    /**
     * v3.2 seat 3 (F12): the L4 accounting invariant at the function generator's boundary —
     * every non-variant function root element of a generated model has a disposition after
     * the run: a file at its own path in {@code output}, or an error attributed to that path
     * in {@code errors}. A function with neither is recorded at
     * {@link SilentDegradation.Site#FUNCTION_NOT_EMITTED} and refused with that path attached
     * (an unattributable function — its symbol cannot resolve — refuses with a {@code null}
     * path, which the gate treats as unwaivered by contract). Dispatch variants are folded
     * into their group's file by design and are not counted; the whole-group disposition is
     * carried by the base declaration, which IS counted.
     *
     * <p>What it asserts, exactly (round-1 cq SF-1 / N-6): path PRESENCE. The expected path is
     * recomputed by the same {@code computeFilePath} that wrote it, so for a function that
     * reached a writer the check is a tautology, and two functions resolving to one path would
     * both pass it. The pass is therefore mechanism-blind for the class it names — a function
     * DROPPED by any mechanism, which is what the chaos carriers measured (it reads only the
     * model and the two result collections, never the grouping above) — and blind by
     * construction to an OVERWRITE, which the one writer seam {@link #emit} refuses instead
     * ({@link SilentDegradation.Site#FUNCTION_PATH_COLLISION}). The mutation lane that reverts
     * the per-file dispatch grouping is its positive control on the chaos carriers.
     */
    private void accountForEveryFunction(Map<String, String> output, List<GenerationException> errors) {
        Set<String> attributed = new HashSet<>();
        for (GenerationException e : errors) {
            if (e.getTargetPath() != null) {
                attributed.add(e.getTargetPath());
            }
        }
        for (RModel model : generatorModel.files()) {
            if (!generatorModel.shouldGenerate(model)) continue;
            for (var element : model.rootElements()) {
                if (!(element instanceof RFunction func) || func.dispatch().isPresent()) continue;
                String path = computeFilePathSafe(func);
                if (path != null && (output.containsKey(path) || attributed.contains(path))) {
                    continue;
                }
                String qualified = (model.namespace() == null ? "" : model.namespace() + ".") + func.name();
                GenerationException refusal = SilentDegradation.refuse(
                        SilentDegradation.Site.FUNCTION_NOT_EMITTED,
                        "function " + qualified + " left with no file and no error after generation",
                        func);
                refusal.setTargetPath(path);
                errors.add(refusal);
            }
        }
    }

    /**
     * v3.2 seat 3 (F12), the round-1 code-quality review (SF-1): the ONE writer seam of this
     * generator — every function file lands through it (the committed lint
     * {@code scripts/ci/one-writer-seam-lint.py}, run by the CI hygiene workflow beside its
     * {@code .test}, fails the build on a second {@code output.put} in this class or on any in its
     * DISCOVERED subclasses — every {@code src/main/java} class that {@code extends FunctionGenerator},
     * two today), and a second writer at a path already written REFUSES at
     * {@link SilentDegradation.Site#FUNCTION_PATH_COLLISION} instead of overwriting the first
     * file silently (the accounting pass checks path presence and cannot see an overwrite).
     * Two shapes, one site (round-2 cq SF-3 — the message asserts only what the seam checks):
     * <ul>
     *   <li>a same-named FUNCTION earlier in this run — the seam's own record names the first
     *       writer (seat fixture control8, lane S). Upstream's per-resource writer is
     *       last-writer-wins on this shape, but its validator forbids it before generation (a
     *       duplicate function name is a model error): a belt on invalid input;</li>
     *   <li>a file of ANOTHER kind already at the path — the shared output map is filled by every
     *       class generator before this one runs, so a {@code type} of the function's simple
     *       name declared in its {@code .functions} sub-namespace lands first (valid Rune on both
     *       sides; the fork used to clobber it silently). Witnessed AT THE SEAM by a seeded output
     *       map (seat fixture control9, lane AC — the lane set's run 3 at commit 9 had found this
     *       arm green once the record above answered control8 first); a whole-pipeline carrier —
     *       the seat harness renders no POJOs — is a gen-2 seed shape.</li>
     * </ul>
     */
    private void emit(Map<String, String> output, String filePath, String code, RFunction func) {
        RFunction first = emittedThisRun.get(filePath);
        if (first != null) {
            throw SilentDegradation.refuse(SilentDegradation.Site.FUNCTION_PATH_COLLISION,
                    "function " + generatorModel.symbolId(func) + " would overwrite " + filePath
                            + ", written earlier in this run by function " + generatorModel.symbolId(first), func);
        }
        if (output.containsKey(filePath)) {
            throw SilentDegradation.refuse(SilentDegradation.Site.FUNCTION_PATH_COLLISION,
                    "function " + generatorModel.symbolId(func) + " would overwrite the file already at " + filePath
                            + ", emitted earlier in this run by another generator (a type of the function's"
                            + " simple name declared in its .functions sub-namespace)", func);
        }
        emittedThisRun.put(filePath, func);
        output.put(filePath, code);
    }

    // =========================================================================
    // Standard function generation
    // =========================================================================

    /**
     * Render a standard (non-dispatch) function and write its Java source into
     * {@code output}.
     *
     * <p>Package-private so a same-package test subclass can override it to inject
     * a controlled exception, locking the Phase 2 catch-block identity-preservation
     * contract introduced at Copilot R11 F3 2026-05-04. Not part of the published
     * API surface — prefer {@link #generateWithErrors(Map)} for orchestration.
     */
    void generateStandardFunction(RFunction func, Map<String, String> output) {
        FunctionTemplateModel model = buildStandardModel(func);
        String code = templateRenderer.renderNoIndent(FUNCTION_TEMPLATE_GROUP, "functionFile", "m", model);
        String filePath = computeFilePath(func);
        emit(output, filePath, code, func);
    }

    /**
     * Phase X T4 — public reusable helper for emitting a function-shaped class
     * with a custom base interface (e.g. {@code ReportFunction<I,O>}) plus
     * class-level annotations. Used by {@code RuleGenerator} (T5) and
     * {@code ReportGenerator} (T6) to emit report-function classes that share
     * the standard function template machinery but with non-default
     * class-decl decoration.
     *
     * <p>The existing {@link #generateStandardFunction(RFunction, Map)} path
     * is unchanged — this method is additive. When called with empty
     * {@code baseInterfaces} + {@code annotations} + {@code
     * renderAsReportFunction=false}, output is byte-equivalent to the
     * standard path (BC guard locked by
     * {@code FunctionGeneratorTest#buildClassWithBaseInterface_unchangedWhenBaseAndAnnotationsEmpty}).
     *
     * <p><b>Byte-parity invariant (Phase X T4.0.5 C1):</b> when
     * {@code renderAsReportFunction=true}, the emitted {@code implements}
     * clause MUST render the base interface raw type AND every type-argument
     * as simple names, with corresponding canonical names added to the
     * imports list. This matches upstream's legacy plugin (see
     * {@code test-corpus/drr/drr-6.34.1/.../CollateralEnrichmentDataRule.java}
     * lines 6 + 15) so D11 byte-parity holds at T7/T8.
     *
     * @param func the function to emit
     * @param clazz the pre-resolved generated Java class identity (FQN/scope
     *        anchor). LOAD-BEARING: the implementation overrides the standard
     *        rendering pipeline's func-derived FQN with
     *        {@code clazz.getPackageName()} + {@code clazz.getSimpleName()}
     *        so RULE-origin generators emit under {@code <ns>.reports/<Name>Rule}
     *        and REPORT-origin generators emit under
     *        {@code <ns>.reports/<body+corpus>ReportFunction} — the
     *        principled origin-dispatched routing locked at T6.0.5 (Copilot
     *        PR #72 R7 F20: javadoc updated to reflect the actual T6 +
     *        T6.0.5 behavior; was previously marked "currently unused"). NOT
     *        NULL (NPE-checked).
     * @param isAbstract reserved for future use by T5/T6 (rules + reports
     *        emit non-abstract classes); currently ignored since the standard
     *        template always emits {@code public abstract class} per the
     *        {@code JavaClassGenerator.createTypeRepresentation} +
     *        {@code generate} overrides contract at spec § 3.3 lines 117-128.
     *        Boolean — no null check.
     * @param baseInterfaces additional interfaces to emit in the class
     *        declaration {@code implements} clause when
     *        {@code renderAsReportFunction} is true (REPLACES the standard
     *        {@code implements RosettaFunction...} clause). Empty / null list
     *        = no replacement, BC-equivalent emission.
     * @param annotations class-level annotations to emit above the
     *        {@code @ImplementedBy} line. Map keys are annotation types (for
     *        ordering/diagnostics); values are pre-rendered annotation source
     *        fragments without the leading {@code @}.
     * @param renderAsReportFunction when {@code true} AND {@code baseInterfaces}
     *        is non-empty, replaces the standard implements clause. When
     *        {@code false} (default), the standard clause is preserved.
     * @param scope the JavaClassScope for identifier registration — required
     *        by the {@code JavaClassGenerator} contract per spec § 3.3 lines
     *        117-128. Forward-compat parameter — currently unused by this
     *        method since the template derives identifiers from {@code func}'s
     *        symbolId; T5/T6 callers pass it so the workspace-wide scope tree
     *        stays consistent. NOT NULL (NPE-checked).
     * @return rendered Java source for the class
     */
    public String buildClassWithBaseInterface(RFunction func,
                                              RGeneratedJavaClass<?> clazz,
                                              boolean isAbstract,
                                              List<JavaParameterizedType> baseInterfaces,
                                              Map<Class<?>, String> annotations,
                                              boolean renderAsReportFunction,
                                              JavaClassScope scope) {
        return buildClassWithBaseInterface(func, clazz, isAbstract,
                baseInterfaces, annotations, renderAsReportFunction, scope,
                List.of() /* supportingImports — back-compat overload */);
    }

    /**
     * Phase X T6 — 8-arg overload extending the 7-arg
     * {@link #buildClassWithBaseInterface(RFunction, RGeneratedJavaClass, boolean,
     *   List, Map, boolean, JavaClassScope)}
     * with a {@code supportingImports} list. The list carries canonical names
     * (e.g. {@code "drr.regulation.asic.rewrite.margin.labels.ASICMarginLabelProvider"})
     * that the emitted source references inline by SIMPLE name — e.g. the
     * {@code @RuneLabelProvider(labelProvider=ASICMarginLabelProvider.class)}
     * argument needs the canonical name in imports so the simple-name
     * {@code .class} literal compiles.
     *
     * <p>The 7-arg overload above delegates here with an empty supporting list
     * so the existing T4/T5 call sites preserve their current behaviour
     * byte-for-byte.
     */
    public String buildClassWithBaseInterface(RFunction func,
                                              RGeneratedJavaClass<?> clazz,
                                              boolean isAbstract,
                                              List<JavaParameterizedType> baseInterfaces,
                                              Map<Class<?>, String> annotations,
                                              boolean renderAsReportFunction,
                                              JavaClassScope scope,
                                              List<String> supportingImports) {
        Objects.requireNonNull(clazz, "clazz");
        Objects.requireNonNull(scope, "scope");

        // Engine PR #6 facet (b): thread the ACTUAL generated host class identity
        // (simple name e.g. <Name>Rule WITH suffix + package e.g. <ns>.reports)
        // into buildStandardModel so (1) the dependency collector can detect a
        // rule-target self-collision (golden injects the colliding rule FQN-inline,
        // no import) and (2) the ImportCollector filters same-package imports
        // against the emitted package, not the function-derived <ns>.functions.
        FunctionTemplateModel base = buildStandardModel(func, clazz.getSimpleName(),
                clazz.getPackageName().withDots());
        List<JavaParameterizedType> safeBaseInterfaces =
                baseInterfaces != null ? baseInterfaces : List.of();
        Map<Class<?>, String> safeAnnotations =
                annotations != null ? annotations : Map.of();
        List<String> safeSupportingImports =
                supportingImports != null ? supportingImports : List.of();

        // Copilot PR #72 R5 F13 — defensive precondition: when emitting the
        // non-default {@code implements <baseInterfaceFragments>} clause via
        // {@code renderAsReportFunction=true}, the base-interfaces list MUST
        // be non-empty. Otherwise the ST template at templates/java-function.stg renders
        // {@code implements } with nothing on the right-hand side, producing
        // invalid Java. The template can't easily express the combined
        // condition without diverging from upstream's structure, so enforce
        // the invariant at the API surface instead.
        if (renderAsReportFunction && safeBaseInterfaces.isEmpty()) {
            throw new IllegalArgumentException(
                    "buildClassWithBaseInterface: renderAsReportFunction=true requires "
                            + "a non-empty baseInterfaces list (otherwise the emitted "
                            + "`implements ` clause is empty + invalid Java). function="
                            + func.name());
        }

        // Phase X T4.0.5 C1 — when emitting a non-default implements clause,
        // merge the base-interface package(s) + every type-argument package
        // into the imports list so the template can render simple names
        // byte-equivalent to upstream's legacy plugin output.
        //
        // Phase X T6 — class-level annotations (passed in via {@code annotations})
        // ALSO need their declaring-package added to imports so the emitter can
        // render {@code @<SimpleName>(...)} not {@code @<FQN>(...)}. The
        // annotation Class<?> keys carry the canonical names directly via
        // reflection; their declaring package is the import target. Same
        // for {@code supportingImports} which are canonical names of classes
        // referenced inline by simple name in the annotation argument
        // fragments (e.g. {@code <X>LabelProvider.class}).
        // Phase X T6 — override the package + class name from the caller-
        // supplied clazz so report/rule generators can emit under the
        // upstream-aligned <ns>.reports package with <body><corpus...>ReportFunction
        // simple names. The default buildStandardModel path derives these
        // from generatorModel.symbolId(func) + toFunctionJavaClass which
        // produces the FUNCTION-origin layout (<ns>.functions); the
        // RObjectJavaClassGenerator contract specifies clazz as the source
        // of truth.
        String overridePackage = clazz.getPackageName().withDots();
        String overrideClassName = clazz.getSimpleName();

        boolean needsMerge = (renderAsReportFunction && !safeBaseInterfaces.isEmpty())
                || !safeAnnotations.isEmpty()
                || !safeSupportingImports.isEmpty();
        List<String> mergedImports = needsMerge
                ? mergeBaseInterfaceImports(base.getImports(), overridePackage,
                        safeBaseInterfaces, safeAnnotations.keySet(),
                        safeSupportingImports, renderAsReportFunction)
                : base.getImports();

        FunctionTemplateModel extended = base.withBaseInterfacesAndAnnotations(
                overridePackage, overrideClassName, mergedImports,
                safeBaseInterfaces, safeAnnotations, renderAsReportFunction);
        return templateRenderer.renderNoIndent(
                FUNCTION_TEMPLATE_GROUP, "functionFile", "m", extended);
    }

    /**
     * Phase X T4.0.5 C1 — collect canonical names of base-interface raw types
     * AND all type-argument types, merge them into the existing imports list,
     * and return the sorted union. Same-package and {@code java.lang} imports
     * are skipped (matches {@link ImportCollector} convention).
     *
     * <p>Phase X T6 — also collects canonical names from the
     * {@code annotationKeys} set so class-level annotations passed via
     * {@code FunctionGenerator.buildClassWithBaseInterface}'s {@code annotations}
     * map get the declaring package added to imports, allowing the template to
     * emit {@code @<SimpleName>(...)} not {@code @<FQN>(...)}. The
     * pre-rendered annotation argument fragment (the {@code Map} value) MUST
     * use the matching simple name, e.g.
     * {@code annotations.put(RosettaReport.class, "RosettaReport(namespace=...)")}.
     */
    private static List<String> mergeBaseInterfaceImports(
            List<String> existingImports,
            String emittedClassPackageName,
            List<JavaParameterizedType> baseInterfaces,
            Set<Class<?>> annotationKeys,
            List<String> supportingImports,
            boolean renderAsReportFunction) {
        Set<String> merged = new TreeSet<>(existingImports);
        // Phase X1 — when renderAsReportFunction is true the standard
        // `implements RosettaFunction...` clause is REPLACED by the base
        // interface(s) (e.g. ReportFunction<I,O>), so the unconditional
        // RosettaFunction import added by collectStandardImports is spurious.
        // The golden ReportFunction classes never import RosettaFunction
        // (ReportFunction extends it transitively — no direct import needed).
        if (renderAsReportFunction) {
            merged.remove("com.rosetta.model.lib.functions.RosettaFunction");
        }
        for (JavaParameterizedType<?> base : baseInterfaces) {
            addCanonicalIfImportable(merged, base, emittedClassPackageName);
            for (JavaTypeArgument arg : base.getArguments()) {
                collectImportsFromTypeArgument(arg, merged, emittedClassPackageName);
            }
        }
        // Phase X T6 — annotation classes contribute their declaring package
        // to imports. The Class<?> key carries canonical name + package via
        // standard reflection (no JavaClass wrapper involved).
        for (Class<?> annotationType : annotationKeys) {
            addReflectiveCanonicalIfImportable(merged, annotationType,
                    emittedClassPackageName);
        }
        // Phase X T6 — supporting canonical names (e.g. for classes
        // referenced inline by simple name in annotation argument fragments,
        // such as {@code <X>LabelProvider.class}).
        for (String canonical : supportingImports) {
            addStringCanonicalIfImportable(merged, canonical, emittedClassPackageName);
        }
        return List.copyOf(merged);
    }

    /**
     * Copilot PR #72 R3 F5 — recursively collect imports from a type
     * argument, walking into nested {@link JavaParameterizedType} arguments
     * so that types like {@code FieldWithMeta<Foo>} contribute both
     * {@code FieldWithMeta} AND {@code Foo} to the imports list.
     *
     * <p>Class-of-issue pair with R2 F3: the rendering side
     * ({@link FunctionTemplateModel#renderTypeArgumentSimpleName})
     * recursively renders nested parameterized types as
     * {@code FieldWithMeta<Foo>}; without this recursive import collection,
     * the emitted source would reference {@code Foo} by simple name without
     * a matching import. Branch order matches the rendering side
     * ({@link JavaParameterizedType} extends {@link JavaClass}, so the
     * parameterized check must precede).
     */
    private static void collectImportsFromTypeArgument(JavaTypeArgument arg,
            Set<String> out, String emittedClassPackageName) {
        if (arg instanceof JavaParameterizedType<?> jpt) {
            addCanonicalIfImportable(out, jpt, emittedClassPackageName);
            for (JavaTypeArgument nested : jpt.getArguments()) {
                collectImportsFromTypeArgument(nested, out, emittedClassPackageName);
            }
            return;
        }
        if (arg instanceof JavaClass<?> jc) {
            addCanonicalIfImportable(out, jc, emittedClassPackageName);
            return;
        }
        // facet reportOutputCardinality (PR #272): a bounded wildcard
        // (`? extends X` / `? super X`) DOES carry an importable bound — the
        // rendering side prints the bound by simple name (e.g.
        // ReportFunction<I, List<? extends PricePeriod>>), so without collecting
        // PricePeriod's import the emitter falls back to an FQN-inline reference.
        // (An UNbounded `?` has no bound and contributes nothing.)
        if (arg instanceof JavaWildcardTypeArgument wild) {
            wild.getBound().ifPresent(b -> {
                if (b instanceof JavaTypeArgument bArg) {
                    collectImportsFromTypeArgument(bArg, out, emittedClassPackageName);
                } else {
                    addCanonicalIfImportable(out, b, emittedClassPackageName);
                }
            });
            return;
        }
        if (arg instanceof JavaReferenceType jrt) {
            addCanonicalIfImportable(out, jrt, emittedClassPackageName);
        }
        // Type variables: no FQN to import (rendered as `T`, etc.) — fall through.
    }

    /**
     * Phase X T6 — add a canonical name string to the imports set if
     * importable (not same-package, not {@code java.lang}, has at least one
     * dot — primitive / bare names are silently skipped). Mirrors
     * {@link #addReflectiveCanonicalIfImportable} but for strings.
     */
    private static void addStringCanonicalIfImportable(Set<String> out,
            String canonical, String emittedClassPackageName) {
        if (canonical == null || canonical.isEmpty()) {
            return;
        }
        int lastDot = canonical.lastIndexOf('.');
        if (lastDot < 0) {
            return; // no package — not importable
        }
        String pkg = canonical.substring(0, lastDot);
        if (!pkg.equals(emittedClassPackageName) && !pkg.equals("java.lang")) {
            out.add(canonical);
        }
    }

    /**
     * Phase X T6 — add a {@link Class}'s canonical name to the imports set if
     * importable (not same-package, not {@code java.lang}, has a canonical
     * name — array / anonymous types are silently skipped). Mirrors
     * {@link #addCanonicalIfImportable} but for reflective {@link Class}
     * inputs rather than {@link JavaClass} / {@link JavaReferenceType}
     * wrappers.
     */
    private static void addReflectiveCanonicalIfImportable(Set<String> out,
            Class<?> type, String emittedClassPackageName) {
        String canonical = type.getCanonicalName();
        if (canonical == null) {
            return; // local / anonymous — not importable
        }
        Package pkg = type.getPackage();
        String pkgName = pkg != null ? pkg.getName() : "";
        if (!pkgName.isEmpty()
                && !pkgName.equals(emittedClassPackageName)
                && !pkgName.equals("java.lang")) {
            out.add(canonical);
        }
    }

    /**
     * Phase X T4.0.5 C1 — add a JavaClass / JavaReferenceType canonical name
     * to the imports set if it is importable (not same-package, not
     * {@code java.lang}). Best-effort: types without a derivable canonical name
     * (e.g. {@code JavaReferenceType.NULL_TYPE}) are silently skipped.
     */
    private static void addCanonicalIfImportable(Set<String> out, Object type,
            String emittedClassPackageName) {
        try {
            String canonical;
            String pkg;
            if (type instanceof JavaClass<?> jc) {
                canonical = jc.getCanonicalName().withDots();
                pkg = jc.getPackageName().withDots();
            } else {
                // Fallback path for non-JavaClass reference types — rare
                // (wildcards / type-vars already filtered upstream). Use
                // toString() which renders FQN for JavaClass-derived types.
                canonical = type.toString();
                int lastDot = canonical.lastIndexOf('.');
                if (lastDot < 0) return;
                pkg = canonical.substring(0, lastDot);
            }
            if (!pkg.isEmpty()
                    && !pkg.equals(emittedClassPackageName)
                    && !pkg.equals("java.lang")) {
                out.add(canonical);
            }
        } catch (UnsupportedOperationException ignored) {
            // JavaReferenceType.NULL_TYPE.getSimpleName / similar — no import
            // to add. Defensive: don't fail the whole codegen for one bad arg.
        }
    }

    /**
     * Build a {@link FunctionTemplateModel} for a standard (non-dispatch) function.
     * Uses the default indentation level (3 tabs) for operation rendering.
     */
    FunctionTemplateModel buildStandardModel(RFunction func) {
        return buildStandardModel(func, null, FunctionExpressionRenderer.DEFAULT_INDENT_LEVEL);
    }

    /**
     * Build a {@link FunctionTemplateModel} for a standard (non-dispatch) function,
     * overriding the host class simple name + package used for rule-target
     * dependency self-collision detection AND same-package import filtering
     * (engine PR #6 facet (b)).
     *
     * <p>Used by the rule/report emission path
     * ({@link #buildClassWithBaseInterface}) which knows the ACTUAL generated
     * class identity (e.g. {@code <ns>.reports/<Name>Rule}) via {@code clazz}.
     * The default path derives the FUNCTION-origin class identity from
     * {@code generatorModel.symbolId(func)} ({@code <ns>.functions/<Name>}),
     * which (a) would never match a {@code <Name>Rule}-suffixed rule-target
     * dependency simple name, and (b) builds the {@link ImportCollector} with
     * the WRONG package ({@code <ns>.functions} instead of the emitted
     * {@code <ns>.reports}), so a dependency that happens to live in the
     * {@code <ns>.functions} sibling package (e.g. an ASIC filter function
     * referenced by an ASIC rule) would be wrongly skipped as same-package and
     * its import dropped (golden imports it).
     *
     * @param func the function to build the model for
     * @param hostClassSimpleName the generated host class's simple name, passed
     *        RAW to the dependency collector to gate rule-target {@code RRule}
     *        injection + collision detection (e.g. {@code clazz.getSimpleName()} on
     *        the rule/report path). {@code null} on the standard function path
     *        DISABLES the {@code RRule} branch — it is NOT a fallback to the
     *        function-derived simple name (a className fallback here would re-enable
     *        the branch for every function: the gate bug fixed at Copilot R1).
     * @param hostPackageName the generated host class's package to use for
     *        same-package import filtering (e.g. {@code clazz.getPackageName().withDots()}),
     *        or {@code null} to use the function-derived package
     */
    FunctionTemplateModel buildStandardModel(RFunction func, String hostClassSimpleName,
                                             String hostPackageName) {
        return buildStandardModel(func, null, FunctionExpressionRenderer.DEFAULT_INDENT_LEVEL,
                hostClassSimpleName, hostPackageName);
    }

    /**
     * Build a {@link FunctionTemplateModel} for a function.
     *
     * @param func the function to build the model for
     * @param signatureSource optional base function that provides inputs/output
     *        declarations (for dispatch variants that inherit from a base).
     *        If null, the function itself provides inputs/output.
     * @param indentLevel the base indentation level for multi-line operation statements
     */
    FunctionTemplateModel buildStandardModel(RFunction func, RFunction signatureSource, int indentLevel) {
        return buildStandardModel(func, signatureSource, indentLevel, null, null);
    }

    /**
     * Build a {@link FunctionTemplateModel} for a function.
     *
     * @param func the function to build the model for
     * @param signatureSource optional base function that provides inputs/output
     *        declarations (for dispatch variants that inherit from a base).
     *        If null, the function itself provides inputs/output.
     * @param indentLevel the base indentation level for multi-line operation statements
     * @param hostClassSimpleNameOverride the generated host class's simple name,
     *        passed RAW to {@code dependencyCollector.collect(...)} (engine PR #6
     *        facet (b)). Non-null ONLY on the rule/report path (enables {@code RRule}
     *        injection + collision detection); {@code null} on the standard function
     *        path DISABLES the {@code RRule} branch so plain functions cannot gain an
     *        {@code @Inject <Name>Rule} field. NOT a fallback to the function-derived
     *        className — that fallback was the gate bug fixed at Copilot R1.
     * @param hostPackageNameOverride the generated host class's package for
     *        same-package import filtering (engine PR #6 facet (b)), or
     *        {@code null} to use the function-derived package.
     */
    FunctionTemplateModel buildStandardModel(RFunction func, RFunction signatureSource, int indentLevel,
                                             String hostClassSimpleNameOverride,
                                             String hostPackageNameOverride) {
        // U019 member-injection seam: reset BEFORE any body of this function renders
        // (the leak belt), stamp the drained block on the model at the end.
        onStandardModelBuildStart();
        // For inputs/output, use the signatureSource if provided (dispatch variants
        // inherit inputs/output from the non-dispatch base function declaration).
        RFunction sig = signatureSource != null ? signatureSource : func;

        ModelSymbolId symbolId = generatorModel.symbolId(func);
        var javaClass = typeTranslator.toFunctionJavaClass(symbolId);
        String packageName = javaClass.getPackageName().withDots();
        String className = javaClass.getSimpleName();
        // Engine PR #6 facet (b) — Copilot R1 #1 fix: the RRule dependency
        // branch in FunctionDependencyCollector is gated on a NON-NULL host
        // name, which must mean "rule/report path ONLY". The host name is
        // supplied solely by buildClassWithBaseInterface (the rule/report
        // path, WITH the <Name>Rule suffix); the standard buildStandardModel(func)
        // path leaves the override null. Pass the RAW override (not a
        // className-derived fallback) to collect() so a plain generated function
        // that happens to reference a rule does NOT gain an @Inject <Name>Rule
        // field. A className fallback here made the gate ineffective — non-null
        // for every function — so the rule-family restriction never engaged.
        // The package the ImportCollector filters
        // same-package imports against MUST be the package the class is actually
        // emitted into. For the rule/report path that's clazz's package
        // (<ns>.reports), threaded in; otherwise the function-derived package.
        String importPackageName = hostPackageNameOverride != null
                ? hostPackageNameOverride
                : packageName;

        // Detect isQualify via [qualification ...] annotation
        boolean isQualify = hasQualificationAnnotation(func);
        String qualifyGenericType = null;
        boolean overridesEvaluate = false;
        if (isQualify) {
            // Qualify_ functions type-parameterize on their first input type
            qualifyGenericType = resolveQualifyGenericType(sig);
            // ALL Qualify_ functions need @Override on evaluate() —
            // IQualifyFunctionExtension<T> declares Boolean evaluate(T)
            overridesEvaluate = true;
        }

        // Resolve output from signature source
        Optional<RAttribute> outputOpt = sig.output();
        ParamInfo outputInfo = outputOpt.map(out -> resolveParam(out, packageName, true))
                .orElse(null);
        boolean outputIsMulti = outputOpt.map(generatorModel::isMulti).orElse(false);
        boolean outputNeedsBuilder = outputInfo != null && outputInfo.isRosettaModelType;

        // Dependencies — pass the RAW host override (engine PR #6 facet (b),
        // Copilot R1 #1): non-null ONLY on the rule/report path, where a
        // rule-target dependency whose generated <Name>Rule class collides with
        // the host is FQN-qualified inline + its import suppressed. Null on the
        // standard function path so the RRule injection branch stays disabled
        // for plain functions.
        //
        // ALSO thread the ACTUAL generated host class identity (simple name +
        // emitted package) so an injected FUNCTION dependency whose generated
        // class simple name collides with the host but lives in a DIFFERENT
        // package (e.g. an iosco CDE version2/version3 function injecting the
        // identically-named version1/version2 function) is FQN-qualified inline
        // with NO import — matching the golden. The collision-host simple name is
        // the rule/report override when present (the <Name>Rule-suffixed class)
        // else the function's own generated class name; the package is the
        // actually-emitted package (importPackageName). This is decoupled from the
        // RRule gate above: the simple name is ALWAYS supplied (collision
        // detection) while the gate override stays null on the function path.
        //
        // Collected HERE (moved up from after the operation loop) so the
        // usRename output-name escape below is known before the output ParamModel,
        // the operation rendering, and the @return javadoc consume it.
        String collisionHostSimpleName = hostClassSimpleNameOverride != null
                ? hostClassSimpleNameOverride
                : className;
        List<FunctionTemplateModel.DependencyModel> dependencies =
                dependencyCollector.collect(func, hostClassSimpleNameOverride,
                        collisionHostSimpleName, importPackageName);

        // facet member_name_disambiguation: a dependency whose field name collides
        // with a shortcut/alias name is numbered name0 (the alias decl/impl/call sites
        // take name1 via compileAliases + ReferenceHandler). Upstream
        // GeneratorScope.computeActualNames numbers the whole same-desired-name group
        // in registration order (dependencies first); see FunctionDependencyCollector
        // .collidingDependencyAliasNames. The fork emitted the bare colliding name for
        // both members (a field + same-named method DO compile, but byte-mismatch the
        // numbered golden), so every collision file was already a waivered mismatch —
        // green-safe by construction. Applied BEFORE the usRename check so an output
        // colliding with a now-numbered dependency no longer matches (upstream numbers
        // the dependency rather than escaping the output in that case). List position
        // is preserved: a numeric suffix never reorders the collector's name sort
        // (nothing sorts between `foo` and `foo0`), matching the golden field order.
        Set<String> collidingMemberNames =
                FunctionDependencyCollector.collidingDependencyAliasNames(func);
        if (!collidingMemberNames.isEmpty()) {
            List<FunctionTemplateModel.DependencyModel> numberedDeps = new ArrayList<>();
            for (FunctionTemplateModel.DependencyModel dep : dependencies) {
                if (collidingMemberNames.contains(dep.getFieldName())) {
                    numberedDeps.add(new FunctionTemplateModel.DependencyModel(
                            dep.getFieldName() + "0", dep.getTypeName(),
                            dep.getTypeFqn(), dep.getIsFunction()));
                } else {
                    numberedDeps.add(dep);
                }
            }
            dependencies = numberedDeps;
        }

        // Resolve inputs from signature source (skip synthesized parameters).
        // The dependency collection above was MOVED ahead of this loop (PR #436,
        // facet fnInputDepCollisionEscape) so the input-name escape below can
        // consult the enclosing function's dependency surface — mirroring the
        // usRename ordering note ("collected HERE so the escape is known before
        // the ParamModel consumes it").
        List<FunctionTemplateModel.ParamModel> inputs = new ArrayList<>();
        for (RAttribute input : sig.inputs()) {
            if (isSynthesizedInput(input)) continue;
            // facet fpmlInputFqn (PR #195): FQN-inline + suppress import for an input whose simple
            // type name collides with the OUTPUT type but a DIFFERENT canonical name.
            ParamInfo info = resolveInputParamForOutputCollision(
                    resolveParam(input, packageName), outputInfo);
            // facet fnInputDepCollisionEscape (W42 finding #27, PR #436): the INPUT-side
            // sibling of the #194 usRename law — upstream registers @Inject dependency
            // fields on the CLASS scope and the class's own simple name on the FILE
            // scope, then allocates method params on a CHILD scope, so an input whose
            // name is already "taken" (a dependency field / the class name) or is not a
            // valid Java identifier (a keyword) escapes with a leading "_"
            // (GeneratorScope.computeActualNames; golden func-call-single-to-list C:
            // `evaluate(Integer _a)` against `@Inject protected A a`). The SHARED
            // static helper is the single source of truth for BOTH halves — this
            // template seat and ReferenceHandler's expression-side reference render —
            // so the signature and every body mention agree on the escaped name.
            inputs.add(new FunctionTemplateModel.ParamModel(
                    escapedFunctionInputName(func, input.name(), collisionHostSimpleName),
                    info.typeName, info.typeFqn,
                    generatorModel.isMulti(input), hasMeta(input), info.isRosettaModelType));
        }

        // facet fqnSelfCollision (PR #366): a model OUTPUT type whose simple name equals
        // the ACTUALLY-EMITTED class's own name renders FULLY-QUALIFIED at every template
        // site with its import suppressed (typeName=FQN, typeFqn=null — the #195/#6
        // convention; a same-simple import against the declaring class is a Java compile
        // error, so no green file can carry the bare form). Upstream's class declaration
        // claims the simple name first in ImportingStringConcatenation, FQN-ing every
        // mention of the same-simple model type — golden drr TechnicalRecordId (the
        // function's name IS the model type's simple name: evaluate return, Builder
        // locals, validator class, doEvaluate signatures, builder ctors all FQN).
        // Gated on collisionHostSimpleName — the EMITTED class identity: a rule named X
        // producing model type X emits class XRule (no collision; the cp5 11-rule NPE
        // regression when gated on the raw className). Placed AFTER the inputs loop
        // (resolveInputParamForOutputCollision compares the ORIGINAL output identity)
        // and AFTER collisionHostSimpleName derives.
        // facet javaLangSelfFqn (W42 finding #30, PR #436): the isRosettaModelType gate
        // DROPPED — a BASIC-typed output whose Java simple name equals the emitted
        // class's own name takes the same FQN swap (golden func-java-lang-self: `func
        // Boolean` returning `boolean` renders `java.lang.Boolean` at every type seat —
        // upstream's file scope claims `Boolean` for the class itself, so the java.lang
        // type can never render bare inside it). For java.lang types the suppressed
        // import is vacuous (they were never imported), so the swap is text-only.
        if (outputInfo != null
                && outputInfo.typeName.equals(collisionHostSimpleName)) {
            outputInfo = new ParamInfo(outputInfo.typeFqn, null, outputInfo.isRosettaModelType);
        }

        // facet usRename (PR #194): a function OUTPUT whose name EXACTLY equals an
        // injected dependency field name is escaped with a single leading "_",
        // mirroring upstream GeneratorScope.escapeName — the output local is
        // allocated on a child method/body scope whose parent class scope already
        // "took" the dependency name, so the same-named output collides and is
        // prefixed. The dependency INVOCATION (e.g. interestRateLeg1.evaluate(...))
        // resolves by a different key and keeps the BARE name; only the output
        // local/param/return + the @return javadoc + the renderer SET left-hand
        // side are escaped. EXACT .equals only (a near-miss like output
        // interestRateLeg1 vs the longer dep interestRateLeg1Basis does NOT
        // collide). The fork emitted the raw name in two uncoordinated pipelines
        // (the ST4 ParamModel.name + the renderer's ROperation.targetName), so the
        // local shadowed the @Inject field → non-compiling → every collision file
        // was already a waivered mismatch (green-safe by construction).
        //
        // SCOPE: the collision set is the collected `dependencies` — which matches
        // every corpus carrier across all 4 corpora. Upstream's GeneratorScope also
        // reserves the template-hardcoded @Inject `objectValidator`/`conditionValidator`
        // fields and the doEvaluate `<name>Builder` local; an output named exactly one
        // of those (or one whose `<name>Builder` equals a dependency field) would
        // escape upstream but not here. There are ZERO such carriers in the corpus, so
        // the narrower set is byte-equivalent today; widen it (and add an anchor) only
        // if such a carrier appears.
        String rawOutputName = outputOpt.map(RAttribute::name).orElse(null);
        // facet fnOutputNameEscape (W42 findings #30/#31, PR #436): the keyword and
        // class-name arms join the #194 dependency arm — upstream's
        // GeneratorScope.computeActualNames escapes an identifier that is NOT a
        // valid Java identifier (SourceVersion.isName false — `static`, golden
        // func-java-keyword-attr `Integer _static`) or whose name is already taken
        // by an ANCESTOR scope (the class's own simple name on the file scope —
        // golden func-java-lang-self `java.lang.Boolean _Boolean`), with the same
        // single leading "_" the dependency collision takes. One escape pass
        // suffices for every witness (upstream loops, but a still-colliding
        // escaped name has no corpus or holdout carrier).
        String escapedOutputName = (rawOutputName != null
                && (dependencies.stream().anyMatch(d -> d.getFieldName().equals(rawOutputName))
                    || !javax.lang.model.SourceVersion.isName(rawOutputName)
                    || rawOutputName.equals(collisionHostSimpleName)))
                ? "_" + rawOutputName
                : rawOutputName;

        // Output param model. facet usRename: the output VARIABLE name carries the
        // escape (escapedOutputName), but the doEvaluate-result BUILDER local uses
        // the RAW name + "Builder" (the golden's `interestRateLeg1Builder` stays bare
        // while `_interestRateLeg1` is escaped) — they do not collide, so upstream
        // escapes only the former.
        FunctionTemplateModel.ParamModel outputModel = null;
        if (outputInfo != null) {
            RAttribute out = outputOpt.get();
            outputModel = new FunctionTemplateModel.ParamModel(
                    escapedOutputName, outputInfo.typeName, outputInfo.typeFqn,
                    outputIsMulti, hasMeta(out), outputInfo.isRosettaModelType,
                    rawOutputName + "Builder");
        }

        // Aliases (from the variant, not the base). The indentLevel is the
        // template splice depth (3 standard / 4 dispatch) the returnIte ladder
        // anchors its continuation lines to.
        List<FunctionTemplateModel.AliasModel> aliases = compileAliases(func, indentLevel);

        // Operations — pass outputNeedsBuilder for toBuilder() wrapping in ADD operations.
        // facet tobuilder_output_assignment (mechanism 2): a function with at least one
        // deep (path-tailed) operation wraps its whole-output SET with the supplier
        // second arg — toBuilder(<value>, () -> <Output>.builder()) — mirroring upstream
        // FunctionGenerator.xtend's functionHasDeepOperations
        // (operations.filter[pathTail.size > 0].size > 0; the fork's ROperation.segment()
        // is the pathTail equivalent). The supplier keeps the builder non-null for the
        // subsequent .getOrCreate… chains. The supplier's type name is the output's
        // resolved Java simple name (corpus-verified equal to the compiled expression's
        // type in every golden supplier instance), whose import is already registered
        // unconditionally for the doEvaluate builder init.
        boolean functionHasDeepOperations = func.operations().stream()
                .anyMatch(o -> o.segment().isPresent());
        List<FunctionTemplateModel.OperationModel> operations = compileOperations(
                func, indentLevel, outputNeedsBuilder, functionHasDeepOperations,
                outputInfo != null ? outputInfo.typeName : null,
                // facet ruleBodyHoistSession (PR #262): the StatementHoistSession now
                // opens on EVERY emission path — function, dispatch-variant AND the
                // rule/report path (hostClassSimpleNameOverride != null), was the
                // function-only `hostClassSimpleNameOverride == null`. Upstream resolves
                // a rule's hoisted locals through the same assignOutputBodyScope a
                // function uses, so golden rule bodies carry the hoisted forms the fork
                // could not emit while the session stayed closed: the convertNullSafe
                // meta-deref CSE (`final ReferenceWithMetaParty rwm = <chain>.get();` +
                // `(rwm == null ? null : rwm.getValue())`), the function-spanning
                // `thenArg0..N-1` numbering (PR #251) and the ifThenElseResult ladder
                // (#173). The #172/#173 "rule bytes frozen" gate was a CONSERVATIVE
                // freeze (the inline-ternary form never byte-matched golden, so every
                // would-be-hoisted rule was already a waivered mismatch — opening the
                // session can only move toward golden, never regress a green rule). The
                // freeze was load-bearing for ONE reason: the renderThenExtractSet
                // cascade fallback (#257) double-renders, leaking the discarded render's
                // thenArg/ifThenElseResult registrations into the field-scoped session —
                // harmless while the session stayed null on the rule path. That leak is
                // now closed by the snapshot/restore around the discarded render
                // (StatementHoistSession.snapshot/restore), so opening the session for
                // rules is byte-clean (full all-kinds gensuite: 0 green regression).
                true,
                // facet usRename (PR #194): thread the raw + escaped output name
                // so the renderer's SET left-hand side (ROperation.targetName)
                // uses the SAME "_"-escaped name as the ParamModel-driven
                // signature/locals when the output collides with a dependency.
                rawOutputName, escapedOutputName,
                // facet depFieldNamingSeeds (PR #334): the injected @Inject
                // dependency field names join the method naming seeds (below) —
                // upstream registers dependency fields on the CLASS scope the
                // method scope inherits, so a type-derived lambda param equal to
                // a dep field escapes (golden `_technicalRecordId` against the
                // injected `TechnicalRecordId technicalRecordId`).
                dependencies);

        // Conditions. facet conditionValidatorBlockLambda (PR #222): the validate()
        // splice depth — 2 tabs for a standard function (java-function.stg), 3 for a
        // dispatch variant (java-function-dispatch.stg) — is threaded so a flat-conditional
        // condition can render the upstream block-lambda body at the correct indent.
        int conditionIndent = func.dispatch().isPresent() ? 3 : 2;
        List<FunctionTemplateModel.ConditionModel> preConditions = compileConditions(func.conditions(), conditionIndent);
        List<FunctionTemplateModel.ConditionModel> postConditions = compilePostConditions(func.postConditions(), conditionIndent);

        boolean hasConditions = !preConditions.isEmpty() || !postConditions.isEmpty();
        boolean hasObjectValidator = outputNeedsBuilder;

        // Super function
        String superClassName = null;
        String superClassFqn = null;
        if (func.superFunction().isPresent()) {
            RFunction superFunc = func.superFunction().get();
            ModelSymbolId superId = generatorModel.symbolId(superFunc);
            var superJavaClass = typeTranslator.toFunctionJavaClass(superId);
            superClassName = superJavaClass.getSimpleName();
            superClassFqn = superJavaClass.getCanonicalName().withDots();
        }

        // facet fqnWitness (PR #227): resolve render-order, first-claim-wins type-name import
        // collisions across the rendered bodies. The colliding seats (builder-ctor, navigation
        // witness, singletonList local-var / emptyList witness) emit ImportCollisionResolver
        // sentinels carrying the canonical name; here they resolve in render order — operations
        // (assignOutput), then aliases, then conditions — the FIRST claim of a simple name keeping
        // the bare name + import and a later different-canonical claim rendering FQN-inline with its
        // import SUPPRESSED. Seeded with the signature output/input/superclass types (emitted ahead
        // of the bodies). A class with NO sentinels (the overwhelming majority) is skipped entirely,
        // so non-colliding output is byte-identical. Green-safe by construction: a same-simple-name
        // collision is a duplicate import that does not compile, so every carrier is already waivered.
        Set<String> suppressedImportCanonicals = java.util.Set.of();
        // Fast path: stream over the existing models and short-circuit on the FIRST sentinel
        // (hasSentinel is a cheap indexOf). Only the rare colliding function allocates + populates
        // the ordered body list below — the overwhelming majority of (sentinel-free) functions skip
        // it entirely, so this stays off the hot-path allocation budget (Copilot R1).
        boolean hasAnySentinel =
                operations.stream().anyMatch(op -> ImportCollisionResolver.hasSentinel(op.getCompiledStatement()))
                || aliases.stream().anyMatch(al -> ImportCollisionResolver.hasSentinel(al.getCompiledBody()))
                // facet importCollisionFqn return-type (PR #247): a model-type alias return whose
                // body carries no element witness (a plain chain, no `ofNull`) has its sole sentinel
                // in the return-type STRING — include it so resolution runs and the sentinel never
                // renders raw into the abstract/impl method declaration. Bounded to model-type alias
                // returns (buildMapperReturnType's `Mapper*<? extends ...>` form); a usesOutput /
                // primitive return carries none, so those functions stay off the resolution path.
                || aliases.stream().anyMatch(al -> ImportCollisionResolver.hasSentinel(al.getReturnType()))
                || preConditions.stream().anyMatch(c -> ImportCollisionResolver.hasSentinel(c.getCompiledExpression()))
                || postConditions.stream().anyMatch(c -> ImportCollisionResolver.hasSentinel(c.getCompiledExpression()));
        if (hasAnySentinel) {
            List<String> collisionBodyOrder = new ArrayList<>();
            for (FunctionTemplateModel.OperationModel op : operations) collisionBodyOrder.add(op.getCompiledStatement());
            for (FunctionTemplateModel.AliasModel al : aliases) collisionBodyOrder.add(al.getCompiledBody());
            for (FunctionTemplateModel.ConditionModel c : preConditions) collisionBodyOrder.add(c.getCompiledExpression());
            for (FunctionTemplateModel.ConditionModel c : postConditions) collisionBodyOrder.add(c.getCompiledExpression());
            // facet importCollisionFqn return-type (PR #247): resolve the alias return-type STRINGS
            // in the SAME pass so the abstract + impl method declarations FQN the loser element
            // consistently with the body's `MapperS.<Item>ofNull()` and SUPPRESS its import in ONE
            // everBare/everFqn accounting (a split across two resolve calls could disagree). Appended
            // after the bodies — resolution is per-body so order is immaterial; a sentinel-free
            // return type passes through unchanged. returnTypeStart lets the alias rebuild pull the
            // resolved return type back by absolute index.
            int returnTypeStart = collisionBodyOrder.size();
            for (FunctionTemplateModel.AliasModel al : aliases) {
                collisionBodyOrder.add(al.getReturnType() == null ? "" : al.getReturnType());
            }
            List<String> seedCanonicals = new ArrayList<>();
            // facet fqnSelfCollision (PR #366): the generated class's OWN identity claims
            // its simple name AHEAD of every other seed (the class declaration precedes
            // all bodies textually) — a body sentinel carrying a same-simple model-type
            // canonical then renders FQN-inline with its import suppressed, agreeing with
            // the template-site FQN flip above. Off-collision this seed claims a simple
            // name no different-canonical sentinel carries (a same-simple import against
            // the declaring class never compiles), so resolution is unchanged.
            seedCanonicals.add(importPackageName + "." + collisionHostSimpleName);
            // The self-collision flip nulls the output typeFqn — the class seed above
            // already owns the simple name, so skip the null.
            if (outputInfo != null && outputInfo.typeFqn != null) {
                seedCanonicals.add(outputInfo.typeFqn);
            }
            for (FunctionTemplateModel.ParamModel in : inputs) seedCanonicals.add(in.getTypeFqn());
            if (superClassFqn != null) seedCanonicals.add(superClassFqn);
            // facet importCollisionFqn (PR #245): the alias signature return-type elements
            // are emitted ahead of the bodies (the abstract + impl method declarations
            // precede the assignOutput/alias bodies textually), so seed them — AFTER
            // output/input/super so seedFromCanonicals' putIfAbsent lets the evaluate()
            // signature win a shared simple name (the correct first-claim render order).
            // A body witness/ctor whose canonical differs from a seeded alias-return
            // element then renders FQN-inline (e.g. MapFraPayoutList's fpml `AdjustableDate`
            // witness loses to the cdm alias return type; MapFxPerformanceSwap...'s cdm ctor
            // loses to the fpml alias return type). Off-collision the seed changes nothing.
            for (FunctionTemplateModel.AliasModel al : aliases) {
                if (al.getReturnTypeElementFqn() != null) {
                    seedCanonicals.add(al.getReturnTypeElementFqn());
                }
            }
            // facet depFieldTypeCollisionSeed (v3.1 flip seat 32, law E.1): the @Inject
            // dependency FIELDS are emitted textually AHEAD of every body, so each dependency
            // TYPE claims its simple name FIRST - a later body sentinel carrying a DIFFERENT
            // canonical of that simple name then renders FQN-inline with its import suppressed.
            // GOLDEN <- FORK, drr 7.0-7.3 Create_AnnaDsbUpiRequestFromReportableEventAndUnderlying
            // (the ONLY carrier measured, one file per cell): golden line 193 `@Inject protected
            // FloatingRateIndex floatingRateIndex;` is the SAME-PACKAGE generated function class
            // drr.enrichment.upi.functions.FloatingRateIndex, so all NINETEEN nav witnesses read
            // `.<cdm.observable.asset.FloatingRateIndex>map("getFloatingRateIndex", ...)` and NO
            // `import cdm.observable.asset.FloatingRateIndex;` is emitted.  Unseeded, the fork let
            // the first WITNESS claim the simple name and imported the cdm type - an import that
            // shadows the same-package dependency class for the whole compilation unit (JLS 6.4.1),
            // so `floatingRateIndex.evaluate(...)` did not resolve.  This is the CLAIM half of the
            // walk whose IMPORT half already ships in collectStandardImports (the
            // `dep.getTypeFqn() != null` dependency loop): exactly the canonicals that are IMPORTED
            // are the ones that claim a simple name, so both loops read the same field with the
            // same test.  An FQN-inline dependency carries typeFqn == null (the #330/#364
            // convention - FunctionDependencyCollector.addFunctionDependency / injectRuleDependency:
            // a host-class collision or a group loser renders its field type fully qualified and
            // emits no import) and so claims nothing.  LAW 69, two siblings already walk this list:
            // facet depFieldNamingSeeds (PR #334) seeds the dependency field NAMES into the method
            // naming scope (which is why the carrier's `_floatingRateIndex` lambda name is already
            // golden), and the DATARULE kind's own seed build already seeds the dependency TYPES
            // with the contrapositive test (`!dep.getTypeName().contains(".")`, DataRuleGenerator)
            // - this is the FUNCTION kind catching up with its own rule sibling.  Appended LAST:
            // seedFromCanonicals is putIfAbsent, so no existing winner (class self / output /
            // inputs / superclass / alias-return elements) moves and an off-collision seed is a
            // no-op.  Green-safe by construction: a file whose BARE dependency simple name is also
            // a different-canonical body witness carried either that shadowing import or a
            // duplicate same-simple-name import, and never compiled - the flip population measured
            // over the 15 censused cells is ONE file per drr 7.x cell (the carrier) and ZERO
            // everywhere else.  No null guard on `dependencies`: it is assigned at :912 and only
            // ever reassigned to a fresh list at :942, exactly as the import half assumes.
            for (FunctionTemplateModel.DependencyModel dep : dependencies) {
                if (dep.getTypeFqn() != null) {
                    seedCanonicals.add(dep.getTypeFqn());
                }
            }
            ImportCollisionResolver.Result collisionRes = ImportCollisionResolver.resolve(
                    collisionBodyOrder, ImportCollisionResolver.seedFromCanonicals(seedCanonicals));
            suppressedImportCanonicals = collisionRes.suppressedCanonicals();
            List<String> resolvedBodies = collisionRes.bodies();
            int rIdx = 0;
            List<FunctionTemplateModel.OperationModel> resolvedOps = new ArrayList<>(operations.size());
            for (FunctionTemplateModel.OperationModel op : operations) {
                resolvedOps.add(new FunctionTemplateModel.OperationModel(op.getOperator(),
                        resolvedBodies.get(rIdx++), op.getTargetPath(), op.getIsAsKey(),
                        op.getRefs(), op.getStaticWildcardImports()));
            }
            operations = resolvedOps;
            List<FunctionTemplateModel.AliasModel> resolvedAliases = new ArrayList<>(aliases.size());
            int aliasIdx = 0;
            for (FunctionTemplateModel.AliasModel al : aliases) {
                // facet importCollisionFqn return-type (PR #247): pull the resolved return-type
                // string (the abstract + impl method decls render the loser element FQN-inline, the
                // winner / off-collision element bare); a null original return type stays null.
                String resolvedReturnType = al.getReturnType() == null
                        ? null
                        : resolvedBodies.get(returnTypeStart + aliasIdx);
                resolvedAliases.add(new FunctionTemplateModel.AliasModel(al.getName(), resolvedReturnType,
                        al.getParams(), resolvedBodies.get(rIdx++), al.getUsesOutput(),
                        al.getRefs(), al.getInferredRefs(), al.getStaticWildcardImports())
                        .withReturnTypeElementFqn(al.getReturnTypeElementFqn())
                        // facet aliasSeamSignature (PR #612): the typed seam rides the resolved
                        // string UNCHANGED — only the STRING is resolved here (the loser element
                        // renders FQN-inline), so the seam's ELEMENT keeps the pre-resolution
                        // (#247 sentinel) spelling. No consumer reads the element after this
                        // point: the ladders' ofNull terminals were rendered before it and
                        // resolve with the bodies, and the template reads the string. Measured
                        // by the C2c census (8 REGCONFLICT arrivals per D11 walk).
                        .withSeam(al.getSeam()));
                aliasIdx++;
            }
            aliases = resolvedAliases;
            List<FunctionTemplateModel.ConditionModel> resolvedPre = new ArrayList<>(preConditions.size());
            for (FunctionTemplateModel.ConditionModel c : preConditions) {
                resolvedPre.add(new FunctionTemplateModel.ConditionModel(c.getName(), c.getDefinition(),
                        resolvedBodies.get(rIdx++), c.getRefs(), c.getStaticWildcardImports()));
            }
            preConditions = resolvedPre;
            List<FunctionTemplateModel.ConditionModel> resolvedPost = new ArrayList<>(postConditions.size());
            for (FunctionTemplateModel.ConditionModel c : postConditions) {
                resolvedPost.add(new FunctionTemplateModel.ConditionModel(c.getName(), c.getDefinition(),
                        resolvedBodies.get(rIdx++), c.getRefs(), c.getStaticWildcardImports()));
            }
            postConditions = resolvedPost;
        }

        // Label provider — explicit [labelProvider X] source annotation OR, mirroring
        // upstream FunctionGenerator.generateClass:122-125, the IMPLICIT transform case:
        // a function carrying [ingest]/[enrich]/[projection] gets an auto-generated
        // <Func>LabelProvider (emitted by LabelProviderGenerator on the same gate) and a
        // class-level @RuneLabelProvider(labelProvider=<Func>LabelProvider.class). The
        // template (java-function.stg:26-27) emits the annotation whenever
        // labelProviderClassName is set; the two supporting imports are added below.
        String labelProviderClassName = detectLabelProvider(func);
        RGeneratedJavaClass<?> implicitLabelProvider = null;
        if (labelProviderClassName == null
                && labelProviderUtil.shouldGenerateLabelProvider(func)) {
            implicitLabelProvider = typeTranslator.toLabelProviderJavaClass(
                    generatorModel.symbolId(func));
            labelProviderClassName = implicitLabelProvider.getSimpleName();
        }

        // Definition / Javadoc (use sig for inputs/output-based javadoc params;
        // the @return name carries the usRename escape — see escapedOutputName).
        String definition = func.definition().orElse(null);
        List<String> javadocParams = buildJavadocParams(sig, func, collisionHostSimpleName,
                escapedOutputName);

        // Version
        String version = resolveVersion(func);

        // Imports. Engine PR #6 facet (b): filter same-package imports against
        // the ACTUAL emitted package (importPackageName), not the function-derived
        // package — otherwise a dependency in the <ns>.functions sibling of a
        // <ns>.reports rule class is wrongly dropped as same-package.
        ImportCollector imports = new ImportCollector(importPackageName);
        // facet labelProviderImplicit (PR #211): the implicit transform-function label
        // provider needs the @RuneLabelProvider annotation import AND the generated
        // <Func>LabelProvider class import, so the template renders both by simple name
        // (byte-parity with upstream — the annotation references <Func>LabelProvider.class
        // inline). The ImportCollector sorts, so add-order is irrelevant.
        if (implicitLabelProvider != null) {
            imports.addImport("com.rosetta.model.lib.annotations.RuneLabelProvider");
            imports.addImport(implicitLabelProvider.getCanonicalName().withDots());
        }
        collectStandardImports(imports, outputNeedsBuilder, outputIsMulti,
                isQualify, hasConditions, hasObjectValidator,
                inputs, outputModel, dependencies, superClassFqn);
        // PR-A §9.1 C3c.2: structured refs + staticWildcards path is now the
        // sole feeder for library-class and domain-type expression imports.
        // Under option F, builder.refs ⊇ rendered-source's library/domain
        // class references by construction; same for staticWildcards.
        Set<JavaClass<?>> refs = unionExpressionRefs(
                operations, aliases, preConditions, postConditions);
        // facet fqnWitness (PR #227): drop the FQN-ed (loser) canonicals so the first-claim
        // winner keeps the SOLE import (the duplicate same-simple-name import was the pre-facet
        // compile-error bug). No-op when no collision was resolved.
        if (!suppressedImportCanonicals.isEmpty()) {
            final Set<String> suppressed = suppressedImportCanonicals;
            refs = new HashSet<>(refs);
            refs.removeIf(jc -> suppressed.contains(jc.getCanonicalName().withDots()));
        }
        // facet aliasParamImportCollisionFqn (PR #341): a #195 collision-nulled INPUT param
        // (typeFqn == null, typeName = the dotted FQN — resolveInputParamForOutputCollision's
        // convention) renders FULLY-QUALIFIED at every seat, but the alias-walk refs channel
        // (AliasModel.inferredRefs / body refs) still carried its JavaClass into the imports —
        // the duplicate same-simple-name import = the pre-facet compile error (cdm6
        // MapProtectionTerms: the fpml.consolidated.cd.ProtectionTerms input vs the
        // cdm.product.asset.ProtectionTerms output; golden carries NO fpml import). Drop it from
        // the refs channel exactly like the sentinel-resolved losers above. Green-safe by
        // construction: the duplicate import never compiled, so every carrier is an
        // already-waivered mismatch — and a ref matching the nulled input's canonical never
        // renders bare (signatures FQN by #195; body witnesses lose the #227/#245 first-claim
        // to the seeded output type and FQN too), so its import is never needed.
        Set<String> collisionNulledInputCanonicals = inputs.stream()
                .filter(in -> in.getTypeFqn() == null && in.getTypeName().indexOf('.') >= 0)
                .map(FunctionTemplateModel.ParamModel::getTypeName)
                .collect(java.util.stream.Collectors.toSet());
        if (!collisionNulledInputCanonicals.isEmpty()) {
            refs = new HashSet<>(refs);
            refs.removeIf(jc -> collisionNulledInputCanonicals.contains(jc.getCanonicalName().withDots()));
        }
        collectExpressionImportsFromRefs(imports, refs);
        Set<JavaClass<?>> wildcards = unionExpressionStaticWildcardImports(
                operations, aliases, preConditions, postConditions);
        collectExpressionStaticImportsFromWildcards(imports, wildcards);

        FunctionTemplateModel model = new FunctionTemplateModel(
                packageName, className,
                imports.getImports(), imports.getStaticImports(),
                isQualify, false /* isDispatch */, overridesEvaluate,
                qualifyGenericType, labelProviderClassName, version,
                functionHasDeepOperations,
                inputs, outputModel, outputIsMulti, outputNeedsBuilder,
                aliases, operations, preConditions, postConditions,
                hasConditions, hasObjectValidator, dependencies,
                superClassName, superClassFqn,
                definition, javadocParams,
                List.of() /* dispatchVariants */, null, null);
        // U019: every body of this function has rendered — stamp the drained
        // helper-members block ("" on the reference pipeline; the slot is byte-inert).
        model.setHelperMethodsBlock(buildHelperMethodsBlock(indentLevel));
        return model;
    }

    // =========================================================================
    // Dispatch function generation
    // =========================================================================

    /**
     * Render a dispatch function (base + variants) and write its Java source into
     * {@code output}.
     *
     * <p>Package-private so a same-package test subclass can override it to inject
     * a controlled exception, locking the Phase 3 catch-block identity-preservation
     * contract introduced at Copilot R11 F1 2026-05-04. Not part of the published
     * API surface — prefer {@link #generateWithErrors(Map)} for orchestration.
     */
    void generateDispatchFunction(String baseName, List<RFunction> variants,
                                          RFunction base, Map<String, String> output) {
        // v3.2 seat 3, round-1 cq SF-1: a group whose base is not in its own file has NO
        // signature to render from (a variant declares no inputs or output) and would have
        // written a dispatch class from the first variant AT THE BASE'S PATH — refused at the
        // register instead (upstream's linker refuses the cross-file split; oracle group
        // func-dispatch-crossfile, seat fixture control7).
        if (base == null) {
            throw SilentDegradation.refuse(SilentDegradation.Site.DISPATCH_BASE_MISSING,
                    "dispatch group " + baseName + " has " + variants.size() + " variant(s) but no base"
                            + " declaration in its own file (a variant cannot see a base declared in"
                            + " another file at 9.83.0; the released plugin refuses the split)",
                    variants.get(0));
        }
        // The base function derives package/class info and the signature
        RFunction signatureSource = base;
        ModelSymbolId symbolId = generatorModel.symbolId(signatureSource);
        var javaClass = typeTranslator.toFunctionJavaClass(symbolId);
        String packageName = javaClass.getPackageName().withDots();
        String className = javaClass.getSimpleName();

        // Resolve inputs/output from the base function declaration
        // (dispatch variants inherit inputs/output from the base)
        Optional<RAttribute> outputOpt = signatureSource.output();
        ParamInfo outputInfo = outputOpt.map(out -> resolveParam(out, packageName, true)).orElse(null);
        boolean outputIsMulti = outputOpt.map(generatorModel::isMulti).orElse(false);
        boolean outputNeedsBuilder = outputInfo != null && outputInfo.isRosettaModelType;

        List<FunctionTemplateModel.ParamModel> inputs = new ArrayList<>();
        for (RAttribute input : signatureSource.inputs()) {
            if (isSynthesizedInput(input)) continue;
            // facet fpmlInputFqn (PR #195): apply the same input-vs-output collision FQN-inline to
            // the dispatch path for class-of-issue consistency (no dispatch carrier in the corpus,
            // so this is byte-neutral today; gated identically by the FQN-difference check).
            ParamInfo info = resolveInputParamForOutputCollision(
                    resolveParam(input, packageName), outputInfo);
            inputs.add(new FunctionTemplateModel.ParamModel(
                    input.name(), info.typeName, info.typeFqn,
                    generatorModel.isMulti(input), hasMeta(input), info.isRosettaModelType));
        }

        FunctionTemplateModel.ParamModel outputModel = null;
        if (outputInfo != null) {
            RAttribute out = outputOpt.get();
            outputModel = new FunctionTemplateModel.ParamModel(
                    out.name(), outputInfo.typeName, outputInfo.typeFqn,
                    outputIsMulti, hasMeta(out), outputInfo.isRosettaModelType);
        }

        // Dispatch parameter info from the first variant
        RFunction firstVariant = variants.get(0);
        String dispatchParamName = null;
        String dispatchEnumType = null;
        if (firstVariant.dispatch().isPresent()) {
            RDispatch dispatch = firstVariant.dispatch().get();
            dispatchParamName = dispatch.paramName();
            dispatchEnumType = dispatch.enumRef();
        }

        // Build variant models
        List<FunctionTemplateModel.DispatchVariantModel> dispatchVariants = new ArrayList<>();
        for (RFunction variant : variants) {
            RDispatch dispatch = variant.dispatch().orElse(null);
            if (dispatch == null) continue;

            // facet dispatchWrapperRendering (PR #229): the variant CLASS name and the
            // routing `case` label use the Java enum CONSTANT name (golden
            // `ProcessFloatingRateResetSCREEN` / `case SCREEN:`), NOT the raw Rosetta
            // value name (`Screen`). Mirror EnumGenerator/EnumHelper.convertValue exactly
            // (strip the `^` keyword-escape, then formatEnumName) so the dispatch case
            // labels match the enum constants the EnumGenerator emits. The @Inject FIELD
            // name, however, keeps the raw value name (golden `processFloatingRateResetScreen`),
            // so it is derived from `className + valueName` (unchanged from the prior behaviour
            // — for an already-constant value like `ACT_360` formatEnumName is a no-op, so
            // DayCountBasis is byte-unaffected by this casing change).
            String valueName = dispatch.valueName();
            // v3.2 seat 9 round 1 (PR #630, D48 - the spec review's SF-1): THE SAME LAW AT THE DISPATCH SEAT. A dispatch
            // value the linker reported unresolved (ENUM_VALUE_NOT_FOUND at the dispatch's own range) used to render
            // its raw name into the routing `case` label and the variant class name - a non-compiling class with no
            // counter, the F13 echo one seat over (no chaos carrier; the seat suite's a6 is the witness). REFUSED at
            // ENUM_VALUE_NAME_ECHO on the linker's category-filtered verdict; a value absent AND unreported keeps the
            // fallback and COUNTS at the same site, as the reference seat does. Round 2 (cq SF-6): the linker leaves the
            // value unbound by TWO paths - the value not found in a resolved enum (ENUM_VALUE_NOT_FOUND on the value
            // name) and the dispatch PARAMETER itself not found (DISPATCH_PARAM_NOT_FOUND on the parameter name, the
            // value never looked up) - both anchored at the dispatch's own range; the gate reads both (a7 the second
            // path's witness). The generator model is a final constructor argument, never null here (cq NIT-1).
            if (dispatch.dispatchValue().isEmpty()) {
                String witness = dispatch.enumRef() + " -> " + valueName;
                if (generatorModel.isReportedUnresolved(dispatch, valueName,
                        com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory.ENUM_VALUE_NOT_FOUND)) {
                    throw SilentDegradation.refuse(SilentDegradation.Site.ENUM_VALUE_NAME_ECHO,
                            "dispatch value `" + witness + "` whose value the linker reported unresolved"
                                    + " (ENUM_VALUE_NOT_FOUND) - the raw name would render a case label and a"
                                    + " variant class the enum does not declare",
                            dispatch);
                }
                if (dispatch.paramName() != null && generatorModel.isReportedUnresolved(dispatch, dispatch.paramName(),
                        com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory.DISPATCH_PARAM_NOT_FOUND)) {
                    throw SilentDegradation.refuse(SilentDegradation.Site.ENUM_VALUE_NAME_ECHO,
                            "dispatch value `" + witness + "` whose parameter `" + dispatch.paramName()
                                    + "` the linker reported unresolved (DISPATCH_PARAM_NOT_FOUND) - the value was"
                                    + " never looked up, and its raw name would render a case label and a variant"
                                    + " class no enum declares",
                            dispatch);
                }
                SilentDegradation.record(SilentDegradation.Site.ENUM_VALUE_NAME_ECHO, witness);
            }
            String enumConstant = EnumHelper.formatEnumName(EnumHelper.stripEscape(valueName));
            String variantClassName = className + enumConstant;
            String fieldName = lowerCamelCase(className + valueName);

            // Build variant as a standard function model (used inside variantClass template).
            // Pass signatureSource so the variant inherits inputs/output from the base.
            // Dispatch variants render at 4-tab indentation (one level deeper than standard).
            FunctionTemplateModel variantModel = buildStandardModel(variant, signatureSource, 4);

            dispatchVariants.add(new FunctionTemplateModel.DispatchVariantModel(
                    enumConstant, variantClassName, fieldName, variantModel));
        }

        // Definition / version from the base function (which has the docstring)
        String definition = signatureSource.definition().orElse(null);
        String version = resolveVersion(signatureSource);
        // The dispatch ROUTING class has no injected dependencies (it delegates to
        // variant classes), so its output name cannot collide — pass the RAW name
        // (no usRename escape). The dispatch VARIANTS go through buildStandardModel
        // (above) and get the escape there if they collide.
        // The dispatch wrapper's own signature keeps RAW input names (it injects no
        // dependencies and its inputs resolve in the wrapper builder, not the
        // buildStandardModel loop) — pass the base function + a null host name so
        // only the universal keyword arm of the input escape could ever fire.
        List<String> javadocParams = buildJavadocParams(signatureSource, signatureSource, null,
                signatureSource.output().map(RAttribute::name).orElse(null));

        // Imports for dispatch wrapper
        ImportCollector imports = new ImportCollector(packageName);
        collectDispatchImports(imports, inputs, outputModel, dispatchVariants,
                dispatchEnumType, outputNeedsBuilder, outputIsMulti);
        // Scan variant expression bodies for imports (MapperMaths, CardinalityOperator, etc.).
        for (var v : dispatchVariants) {
            var vm = v.getVariantModel();
            // PR-A §9.1 C3c.2: structured refs + staticWildcards sole feeder
            // (mirrors buildStandardModel wiring).
            // facet fqnWitness (PR #227) KNOWN GAP: the variant bodies ARE sentinel-resolved (each
            // variant is built via buildStandardModel), but this wrapper-file import re-collection
            // unions the reconstructed variant models' per-model refs, which still carry the FQN-ed
            // LOSER canonical (the import suppression dropped it only from the variant's own
            // function-level union INSIDE buildStandardModel, not from the per-model refs). So a
            // dispatch VARIANT carrying a same-simple-name collision would re-emit the duplicate
            // same-simple-name import in this wrapper file. Byte-NEUTRAL today (no dispatch carrier
            // collides in the corpus — verified by D11 20/20); if one ever appears it is a duplicate
            // import = a compile error = already waivered (green-safe). Closing this fully needs
            // buildStandardModel to surface its suppressed-canonical set so the same removeIf can run
            // here — deferred until a real dispatch carrier exists.
            Set<JavaClass<?>> refs = unionExpressionRefs(
                    vm.getOperations(), vm.getAliases(),
                    vm.getPreConditions(), vm.getPostConditions());
            collectExpressionImportsFromRefs(imports, refs);
            Set<JavaClass<?>> wildcards = unionExpressionStaticWildcardImports(
                    vm.getOperations(), vm.getAliases(),
                    vm.getPreConditions(), vm.getPostConditions());
            collectExpressionStaticImportsFromWildcards(imports, wildcards);
        }

        FunctionTemplateModel model = new FunctionTemplateModel(
                packageName, className,
                imports.getImports(), imports.getStaticImports(),
                false /* isQualify */, true /* isDispatch */, false,
                null, null, version,
                false /* hasDeepOperations */,
                inputs, outputModel, outputIsMulti, outputNeedsBuilder,
                List.of(), List.of(), List.of(), List.of(),
                false, false, List.of(),
                null, null,
                definition, javadocParams,
                dispatchVariants, dispatchParamName, dispatchEnumType);

        String code = templateRenderer.renderNoIndent(DISPATCH_TEMPLATE_GROUP, "dispatchFile", "m", model);
        String filePath = computeFilePath(signatureSource);
        emit(output, filePath, code, signatureSource);

        // Also generate each variant as a standalone class (for the inner static classes,
        // they are rendered inline by the dispatch template — no separate files needed)
    }

    // =========================================================================
    // Param resolution
    // =========================================================================

    private record ParamInfo(String typeName, String typeFqn, boolean isRosettaModelType) {}

    private ParamInfo resolveParam(RAttribute attr, String contextPackage) {
        return resolveParam(attr, contextPackage, false);
    }

    /**
     * Resolve the Java type identity (simple name, FQN, model-ness) of a function
     * parameter for the template model.
     *
     * <p>facet {@code metaWit} (PR #186): when {@code isOutput} and the attribute
     * carries a value-level meta annotation — {@code [metadata reference|address]}
     * ({@code → ReferenceWithMetaX}) or {@code [metadata scheme|id|location]}
     * ({@code → FieldWithMetaX}) — the OUTPUT type surfaces the CONCRETE per-namespace
     * meta wrapper, exactly as upstream rune-dsl does, rather than the bare value type
     * {@code X} the fork previously erased it to. Every output-type site
     * ({@code evaluate} return, the {@code …Builder} locals, {@code ::build},
     * {@code objectValidator.validate(…class)}, the abstract + Default {@code doEvaluate},
     * the {@code assignOutput} signature, and the output import) derives from this
     * {@code typeName}/{@code typeFqn}, so the single substitution here flips them all.
     *
     * <p>facet {@code metaInputParam} (PR #342): the same wrap now applies to INPUT
     * parameters — the "distinct, separately-tracked facet" the #186 note deferred. A
     * {@code [metadata …]}-annotated input renders the concrete wrapper at every
     * signature seat ({@code evaluate}, the abstract + Default {@code doEvaluate},
     * {@code assignOutput}, the alias methods — all derive from this ParamInfo), exactly
     * as upstream. Population corpus-closed (all 34,686 goldens): exactly 5 carriers
     * (Create_Cashflow cdm5 {@code currency string [metadata scheme]} +
     * Update{Price,Quantity}AmountForEachMatchingQuantity ×2 cdm cells
     * {@code [metadata location]}), every one a waivered divergence — no green file
     * carries a meta-annotated input, so the input wrap is blast-radius-0 by
     * construction. The wrapper
     * is built via {@link RJavaWithMetaValue#create} (the concrete
     * {@code metafields.ReferenceWithMetaX}/{@code FieldWithMetaX}), NOT
     * {@link JavaTypeTranslator#toMetaJavaType}, which yields the generic
     * {@code FieldWithMeta<T>}. The meta wrapper is itself a {@code RosettaModelObject}
     * (it owns a builder), so {@code isRosettaModelType} — which drives
     * {@code outputNeedsBuilder} — is computed against the effective (wrapped) type.
     *
     * <p>The {@code isOutput} parameter is deliberately RETAINED though the wrap no
     * longer branches on it (both directions wrap identically since #342): the two
     * call-site families (output vs input resolution) stay self-documenting, and a
     * future output-only law re-gains its seat without re-threading the flag.
     */
    private ParamInfo resolveParam(RAttribute attr, String contextPackage, boolean isOutput) {
        RType rType = generatorModel.getType(attr);
        JavaType javaType = typeTranslator.toJavaType(rType);
        JavaClass<?> refType = typeTranslator.toJavaReferenceType(rType);
        JavaClass<?> effectiveType = refType;
        boolean wrapped = false;
        MetaFieldGenerator.MetaKind metaKind = MetaFieldGenerator.detectMetaKind(attr);
        if (metaKind != MetaFieldGenerator.MetaKind.NONE) {
            effectiveType = RJavaWithMetaValue.create(
                    metaKind == MetaFieldGenerator.MetaKind.REFERENCE_WITH_META,
                    refType, typeUtil);
            wrapped = true;
        }
        String typeName = effectiveType.getSimpleName();
        String typeFqn = effectiveType.getCanonicalName().withDots();
        // The meta wrapper is itself a RosettaModelObject (it owns a builder), so probe
        // the wrapper when we wrapped — this flips outputNeedsBuilder true for a
        // builtin-valued meta output (e.g. FieldWithMetaString, whose String value is NOT
        // a model object), restoring the golden builder form. When we did not wrap, probe
        // the original javaType to preserve the exact pre-fix primitive/unboxed behavior.
        boolean isRosettaModel = typeUtil.isRosettaModelObject(wrapped ? effectiveType : javaType);
        // facet javaLangParamFqn (W42 finding #13, PR #426): the FUNCTION seat of the
        // #304–#306 java.lang-collision law — a param/output whose MODEL type's simple
        // name collides with an implicitly-imported java.lang type renders FQN-inline
        // with its import suppressed (typeName=FQN, typeFqn=null — the established
        // #195/#366 convention, consumed at every import seat; upstream's
        // ImportingStringConcatenation refuses the import and fully qualifies every
        // reference — golden func-dispatch-collision MathFunc ×10 sites,
        // `test.dispatchcollision.Math` with NO import). A java.lang type itself
        // (String/Integer/…) stays bare — the canonical-prefix gate, exactly
        // ModelObjectGenerator.valueSiteTypeName's law at the POJO value seats.
        if (!typeFqn.startsWith("java.lang.")
                && com.regnosys.rosetta.generator.java.object.ModelObjectGenerator
                        .collidesWithJavaLang(typeName)) {
            return new ParamInfo(typeFqn, null, isRosettaModel);
        }
        return new ParamInfo(typeName, typeFqn, isRosettaModel);
    }

    /**
     * facet fpmlInputFqn (PR #195): an INPUT parameter whose Java simple type name collides with
     * the function OUTPUT type's simple name but resolves to a DIFFERENT canonical name is rendered
     * FULLY-QUALIFIED inline ({@code typeName} = the dotted FQN) with its import SUPPRESSED
     * ({@code typeFqn} = null). This mirrors upstream {@code ImportingStringConcatenation}'s
     * first-claim-wins import-collision fallback (the OUTPUT type, registered first, keeps the bare
     * simple name; the later same-simple collider renders FQN-inline with no import) and reuses the
     * EXACT {@code typeName=FQN, typeFqn=null} convention the dependency/host collision path already
     * uses (engine PR #6; PR #194 {@code usRename}). Carriers: the cdm6 ingest-fpml {@code Map*}
     * functions (output {@code cdm...Money}, input {@code fpml.consolidated.shared.Money}) which the
     * fork otherwise emits with a DUPLICATE same-simple-name import = a Java compile error (so every
     * carrier is already a waivered mismatch — green-safe by construction).
     *
     * <p>The FQN-DIFFERENCE gate ({@code !info.typeFqn.equals(outputInfo.typeFqn)}) is MANDATORY: a
     * same-type self-mapping input ({@code SetCashCurrency}'s {@code Cash cash} → {@code Cash},
     * identical FQN) is NOT a collision and stays bare with one import. A null {@code outputInfo}
     * (no output) cannot collide. Returns the input {@code info} unchanged when no collision.
     */
    private ParamInfo resolveInputParamForOutputCollision(ParamInfo info, ParamInfo outputInfo) {
        if (outputInfo != null
                && info.typeName.equals(outputInfo.typeName)
                && !info.typeFqn.equals(outputInfo.typeFqn)) {
            return new ParamInfo(info.typeFqn, null, info.isRosettaModelType);
        }
        return info;
    }

    // =========================================================================
    // Expression compilation
    // =========================================================================

    private List<FunctionTemplateModel.OperationModel> compileOperations(RFunction func, int indentLevel,
                                                                          boolean outputNeedsBuilder,
                                                                          boolean functionHasDeepOperations,
                                                                          String outputTypeName,
                                                                          boolean hoistSessionEligible,
                                                                          String rawOutputName,
                                                                          String escapedOutputName,
                                                                          List<FunctionTemplateModel.DependencyModel> dependencies) {
        List<FunctionTemplateModel.OperationModel> result = new ArrayList<>();
        // facet ifthenelse_result_hoisting: hoisted ifThenElseResult locals
        // number per same-name group over the WHOLE assignOutput body (one
        // upstream assignOutputBodyScope spans all operations), so the naming
        // session brackets the operation loop and resolves the group-law names
        // over every rendered statement once the group size is final — see
        // StatementHoistSession. The bracket opens on the standard
        // function/dispatch path only (hoistSessionEligible — the rule/report
        // path passes false and keeps the inline ternary); alias compilation
        // runs OUTSIDE the bracket either way.
        StatementHoistSession hoistSession = hoistSessionEligible
                ? expressionRenderer.beginStatementHoistSession()
                : null;
        // facet lambdaNaming M1 (PR #329): open the method-wide NAMING group — the
        // per-statement deferred-coercion resolution defers to endMethodNamingGroup
        // below, so registered identifiers (guarded Type-coercion params, #237
        // hoist locals) number 0..n-1 over the WHOLE assignOutput body exactly as
        // the hoistSession's thenArg/ifThenElseResult groups already do (upstream
        // keeps ONE assignOutput body scope spanning all operations). Opens on
        // EVERY path (functions AND rules — the naming law is path-independent);
        // the finally backstop closes-and-discards if the loop throws outside the
        // per-op catch so the renderer never stays in deferred mode.
        expressionRenderer.beginMethodNamingGroup();
        boolean namingClosed = false;
        try {
            try {
                for (ROperation op : func.operations()) {
                    String compiled = "";
                    Set<JavaClass<?>> refs = Set.of();   // PR-A C2
                    Set<JavaClass<?>> wildcards = Set.of();  // PR-A C3c.1
                    try {
                        // PR-A C2: renderOperation returns RenderedStatement — drain
                        // both source + refs so FunctionGenerator can union refs later
                        // for the structured import path.
                        RenderedStatement rs = expressionRenderer.renderOperation(
                                op, indentLevel, outputNeedsBuilder, functionHasDeepOperations, outputTypeName,
                                rawOutputName, escapedOutputName);
                        compiled = rs.source();
                        refs = rs.refs();
                        wildcards = rs.staticWildcardImports();
                    } catch (Exception e) {
                        // v3.1 C0: a REFUSAL is never recovery — it must reach the per-element
                        // boundary (JavaClassGenerator), which attaches the target path and reports
                        // it as a generation error. Locked by scripts/ci/refusal-propagation-lint.py.
                        if (e instanceof SilentDegradation.Refusal __refusal) throw __refusal;
                        // v3.2 seat 4 (PR #625): lane C measured this recovery swallowing a detached synthetic
                        // node's IllegalStateException into a silent TODO comment (no error, no counter) - a
                        // register question WIDER than that seat, BANKED (the seat plan, section 6)
                        compiled = "/* TODO: expression compilation error: " + e.getMessage() + " */";
                    }
                    result.add(new FunctionTemplateModel.OperationModel(
                            op.operator() != null ? op.operator().name().toLowerCase() : "set",
                            compiled,
                            List.of(), // target path — M7b-4
                            op.isAsKey(),
                            refs,
                            wildcards));
                }
            } finally {
                if (hoistSession != null) {
                    expressionRenderer.endStatementHoistSession();
                }
            }
            // facet addHoistLocalParamEscape (PR #347): the method seeds (inputs/output/
            // shortcuts + dep fields) compute BEFORE the session replay so the group-law
            // names can escape-iff-taken against them (upstream computeActualNames sees the
            // method-scope params when materialising a hoist local; golden cdm6
            // MapAveragingObservations `_averagingObservationList`). Pure reordering of the
            // #329/#334 seed computation — the same list feeds endMethodNamingGroup below.
            List<String> seeds = methodSeedNames(func);
            // facet depFieldNamingSeeds (PR #334): the injected @Inject dependency
            // field names seed the unified method naming scope — upstream registers
            // dependency fields on the CLASS scope the method scope inherits, so a
            // type-derived deferred lambda param colliding with a dep field escapes
            // (`_technicalRecordId`). Deduped against the existing seeds so an
            // input/output/alias that shares a dep name keeps today's single-take
            // behaviour (the usRename/member-numbering facets own those collisions).
            if (dependencies != null) {
                for (FunctionTemplateModel.DependencyModel dep : dependencies) {
                    if (dep.getFieldName() != null && !seeds.contains(dep.getFieldName())) {
                        seeds.add(dep.getFieldName());
                    }
                }
            }
            // facet condDerefTextOrderUnify (PR #378): the #329 documented limitation's
            // fix, its carrier now surfaced (CallQuantity/PutQuantity) — a session
            // method-level group sharing its base name with a SURVIVING deferred
            // coercion entry resolves as ONE text-ordered group in the unified replay
            // (upstream's single method scope numbers the guarded cond params and the
            // whole-output deref hoists 0..n-1 in build order: cond-param 0, hoist 1,
            // cond-param 2, hoist 3). The session resolve SKIPS those groups (their
            // sentinels ride into the replay) and their would-be names stay OUT of the
            // escape seeds. Structural bases (thenArg/ifThenElseResult/boolean/
            // bigInteger — placeholder+substitute kinds whose registration order is
            // deliberately consumption order, the #327 law) never unify; a coercion
            // param never desires those names (they are type-derived lowerCamel names).
            java.util.Set<String> sharedHoistBases = java.util.Set.of();
            if (hoistSession != null && !hoistSession.isEmpty()) {
                List<String> preTexts = new ArrayList<>(result.size());
                for (FunctionTemplateModel.OperationModel om : result) {
                    preTexts.add(om.getCompiledStatement());
                }
                java.util.Set<String> candidates = hoistSession.methodGroupBaseNames();
                candidates.removeAll(java.util.Set.of("thenArg",
                        StatementHoistSession.IF_THEN_ELSE_RESULT,
                        StatementHoistSession.BOOLEAN,
                        StatementHoistSession.BIG_INTEGER));
                sharedHoistBases =
                        expressionRenderer.survivingSharedDeferredBases(candidates, preTexts);
            }
            if (hoistSession != null && !hoistSession.isEmpty()) {
                List<FunctionTemplateModel.OperationModel> resolved = new ArrayList<>(result.size());
                for (FunctionTemplateModel.OperationModel om : result) {
                    resolved.add(new FunctionTemplateModel.OperationModel(
                            om.getOperator(),
                            hoistSession.resolve(om.getCompiledStatement(), seeds, sharedHoistBases),
                            om.getTargetPath(),
                            om.getIsAsKey(),
                            om.getRefs(),
                            om.getStaticWildcardImports()));
                }
                result = resolved;
            }
            // facet lambdaNaming M1 (PR #329): resolve the surviving deferred
            // coercion sentinels against the unified method scope (after the
            // session resolve above, so hoist-block-embedded sentinels are
            // already inside the statement texts). The session's RESOLVED hoist
            // locals join the seed set (M3/M4): a deferred lambda param escapes
            // against a method-level hoist local regardless of registration
            // order, exactly as upstream's lazily-materialised method scope
            // resolves it (golden `_dateTimeList` against the #198 ctor-hoist
            // local declared around the lambda). Unified (skipped) groups
            // contribute no seed — they number INSIDE the replay instead
            // (facet condDerefTextOrderUnify, PR #378).
            List<String> texts = new ArrayList<>(result.size());
            for (FunctionTemplateModel.OperationModel om : result) {
                texts.add(om.getCompiledStatement());
            }
            if (hoistSession != null) {
                List<String> sessionNames = hoistSession.resolvedNames(seeds, sharedHoistBases);
                seeds.addAll(sessionNames);
            }
            List<String> named = expressionRenderer.endMethodNamingGroup(seeds, texts,
                    hoistSession == null ? Map.of() : hoistSession.methodGroupsFor(sharedHoistBases));
            namingClosed = true;
            if (!named.equals(texts)) {
                List<FunctionTemplateModel.OperationModel> renamed = new ArrayList<>(result.size());
                for (int i = 0; i < result.size(); i++) {
                    FunctionTemplateModel.OperationModel om = result.get(i);
                    renamed.add(new FunctionTemplateModel.OperationModel(
                            om.getOperator(),
                            named.get(i),
                            om.getTargetPath(),
                            om.getIsAsKey(),
                            om.getRefs(),
                            om.getStaticWildcardImports()));
                }
                result = renamed;
            }
            return result;
        } finally {
            if (!namingClosed) {
                expressionRenderer.endMethodNamingGroup(List.of(), List.of());
            }
        }
    }

    /**
     * facet lambdaNaming M1 (PR #329): the seed-name set of the generated
     * method's naming scope — inputs, output, aliases — identical to what
     * {@code FunctionExpressionRenderer.createScope} seeds every per-statement
     * root with on the operations path (a rule-derived {@link RFunction}'s
     * synthetic input/output are literally named {@code input}/{@code output},
     * matching the rule branch there).
     */
    private static List<String> methodSeedNames(RFunction func) {
        List<String> seeds = new ArrayList<>();
        for (RAttribute in : func.inputs()) {
            seeds.add(in.name());
        }
        // facet dispatchVariantParamResolution (PR #369): a dispatch VARIANT's own
        // inputs are the `__synthesized_input__` placeholder — its generated methods
        // take the BASE's declared inputs (signatureSource), so those names must join
        // the naming seeds or a type-derived lambda var equal to a parameter name
        // renders unescaped (golden `_resetDates -> …`; the unescaped form shadows the
        // method parameter and does not compile — never green). Base OUTPUT deliberately
        // NOT seeded: no corpus carrier, and the alias-scope usesOutput gate (M2) would
        // need its own base-output derivation.
        RFunction dispatchBase = com.regnosys.rosetta.generator.java.expression.handlers
                .HandlerHelper.dispatchBaseOf(func);
        if (dispatchBase != null) {
            for (RAttribute in : dispatchBase.inputs()) {
                seeds.add(in.name());
            }
        }
        func.output().ifPresent(out -> seeds.add(out.name()));
        for (RShortcut sc : func.shortcuts()) {
            seeds.add(sc.name());
        }
        return seeds;
    }

    /**
     * A VALUE-TYPED alias emission (the § 6.3 alias re-typing program, T1):
     * the re-typed seam's return-type string — the Mapper wrapper stripped
     * with the seam's own recorded element preserved VERBATIM, so
     * {@code MapperS<? extends T>}/{@code MapperS<T>} → {@code T} and
     * {@code MapperC<? extends T>} → {@code List<? extends T>} /
     * {@code MapperC<T>} (primitive/enum elements) → {@code List<T>} — the
     * full method-body source (its own {@code return}s, the ladder/hoist
     * full-body contract), and the body's refs/wildcards. Produced ONLY by the
     * optimised route's {@link #aliasValueFormOrNull} override; the reference
     * route never constructs one. Protected (subclass-facing, not public API);
     * the canonical constructor is widened to public because JLS 6.6.2.2 bars
     * a cross-package subclass from invoking a protected constructor via
     * {@code new} — the protected TYPE still gates who can name it.
     */
    protected record AliasValueForm(String returnType, String body,
            Set<JavaClass<?>> refs, Set<JavaClass<?>> wildcards) {
        public AliasValueForm {
        }
    }

    /**
     * THE § 6.3 VALUE-SEAM EMISSION HOOK (the T1 flip's compileAliases seat):
     * non-null exactly when {@code shortcut} is emitted with a VALUE-TYPED seam
     * — the alias's {@code AliasModel} then carries the returned form's
     * return type + body VERBATIM (the facet body chain is skipped; the
     * name-disambiguation, signature-ref-union and ctor tail run unchanged).
     *
     * <p>THE REFERENCE ROUTE ALWAYS ANSWERS NULL — the byte-inert seam (the
     * PR-7 member-injection precedent; the rings prove reference bytes
     * untouched). {@code OptimisedFunctionGenerator} overrides with the
     * tranche policy.
     */
    protected AliasValueForm aliasValueFormOrNull(RFunction func, RShortcut shortcut,
            FunctionTemplateModel.AliasModel base, int indentLevel) {
        return null;
    }

    /**
     * Analyse and compile all aliases in a function.
     *
     * <p>Delegates structural analysis (return type, params, usesOutput) to
     * {@link FunctionAliasHelper#analyze}, then compiles each alias expression
     * via {@link FunctionExpressionRenderer#renderAlias} and wraps it as
     * {@code return compiledExpr;}.
     *
     * @param func the function whose aliases should be compiled
     * @return ordered list of alias models with compiled bodies
     */
    private List<FunctionTemplateModel.AliasModel> compileAliases(RFunction func, int indentLevel) {
        // Get structural analysis (return type, params, usesOutput) — compiledBody is empty.
        // ORDERING INVARIANT: aliasHelper.analyze() iterates func.shortcuts() internally,
        // so the returned list is in the same order. We correlate by index below.
        List<FunctionTemplateModel.AliasModel> structural = aliasHelper.analyze(func);
        List<RShortcut> shortcuts = func.shortcuts();

        // facet member_name_disambiguation: an alias whose name collides with a
        // dependency field name is numbered name1 on its method decl + impl (the
        // dependency took name0 in buildStandardModel; the call sites take the same
        // numbers via ReferenceHandler). Empty (a no-op) on every collision-free
        // function. See FunctionDependencyCollector.collidingDependencyAliasNames.
        Set<String> collidingMemberNames =
                FunctionDependencyCollector.collidingDependencyAliasNames(func);

        // Safety check: structural list and shortcuts list should be the same size
        if (structural.size() != shortcuts.size()) {
            return structural; // fall back to empty bodies
        }

        List<FunctionTemplateModel.AliasModel> result = new ArrayList<>();
        for (int i = 0; i < structural.size(); i++) {
            FunctionTemplateModel.AliasModel base = structural.get(i);
            RShortcut shortcut = shortcuts.get(i);

            String compiledBody = "";
            Set<JavaClass<?>> refs = Set.of();   // PR-A C2
            Set<JavaClass<?>> wildcards = Set.of();  // PR-A C3c.1
            // THE § 6.3 VALUE-SEAM FLIP (T1): the optimised route's hook may
            // re-type this alias — the returned form REPLACES the return type
            // and the facet-chain body wholesale; the reference route's hook
            // is a constant null (byte-inert). The name-disambiguation,
            // signature-ref union and AliasModel construction below run
            // UNCHANGED for both shapes. The hook is invoked INSIDE the
            // try (Copilot #558 R3): a throwing override degrades to the
            // same "/* TODO: alias compilation error */" fallback as every
            // other alias-compilation failure — the seam then keeps the
            // Mapper-typed return (returnType's initial value), matching the
            // failure shape of the reference paths.
            String returnType = base.getReturnType();
            // facet aliasSeamSignature (PR #612): the typed seam of the emitted method — the analyzed
            // Mapper/builder facts, or their VALUE re-seam when the optimised hook replaced the string.
            FunctionTemplateModel.AliasSeam seam = base.getSeam();
            try {
                AliasValueForm valueForm = aliasValueFormOrNull(func, shortcut, base, indentLevel);
                if (valueForm != null) {
                    returnType = valueForm.returnType();
                    seam = seam.asValue();
                    compiledBody = valueForm.body();
                    refs = valueForm.refs();
                    wildcards = valueForm.wildcards();
                } else if (shortcut.expression() != null) {
                    // facet returnIte — a conditional alias body renders as the
                    // flat `if (cond) { return then; } return else;` ladder
                    // (upstream JavaIfThenElseBuilder.completeAsReturn), not the
                    // fork's inline ternary. Declines (null) to the ternary path
                    // below on a non-conditional body or an un-typable empty else
                    // (see FunctionExpressionRenderer#renderAliasReturnLadderOrNull).
                    RenderedStatement ladder = expressionRenderer
                            .renderAliasReturnLadderOrNull(shortcut, base.getSeam(), indentLevel);
                    // facet aliasSwitchValueLadder (PR #365, F-A): a CHOICE/TYPE-keyed
                    // switch body with VALUE-class case results (enum value / boolean
                    // literal) renders the upstream instanceof RETURN ladder — the
                    // switch sibling of the conditional ladder above, riding the same
                    // full-body contract slot. The element type is the SAME
                    // FunctionAliasHelper case-result join that computed
                    // base.getReturnType() (MapperS<FinancialUnitEnum> /
                    // MapperS<Boolean>), so signature and ladder agree by construction.
                    // Declines (null → the routes below, which end at the residual seat's
                    // refusal - TYPE_SWITCH_TERNARY_STUB per resolvable case since v3.1 C0; R1, and R3 at
                    // the alias SIGNATURE for a builtin output, since v3.2 seat 12; today's bytes before
                    // them) for every other switch shape.
                    if (ladder == null && !base.getUsesOutput()) {
                        // facet aliasSwitchNavLadder (PR #369, cluster A): the FULL join
                        // (element + cardinality) threads through so the ladder's
                        // terminals select MapperS vs MapperC in lockstep with the
                        // signature (golden `MapperC.<SpreadSchedule>ofNull()`, Spread).
                        FunctionAliasHelper.ExpressionTypeInfo switchJoin = aliasHelper
                                .switchValueCaseJoinInfoOrNull(shortcut, func);
                        if (switchJoin != null) {
                            // facet ladderElementCollisionQualify (PR #389): the ladder
                            // element lifts VERBATIM from the FACTS the signature's producer
                            // recorded beside it (base.getSeam(), facet aliasSeamSignature
                            // PR #612 — no longer re-parsed out of the rendered string) so a
                            // collision-QUALIFIED
                            // element (the #247 sentinel — cdm6 MapBasketConstituent
                            // WithLocation `MapperS.<cdm.observable.asset.
                            // BasketConstituent>ofNull()`) rides into the ofNull
                            // terminals, the same ride-along law the conditional
                            // ladder's typedEmptyElseOrNull documents. Off-collision
                            // the signature element IS the join's simple name
                            // (byte-identical); a non-Mapper signature falls back to
                            // the join name (today's bytes).
                            String ladderElement = FunctionExpressionRenderer
                                    .aliasReturnElementOrNull(base.getSeam());
                            ladder = expressionRenderer.renderAliasChoiceSwitchLadderOrNull(
                                    shortcut,
                                    ladderElement != null ? ladderElement
                                            : switchJoin.javaTypeName,
                                    switchJoin.isMulti,
                                    indentLevel);
                        }
                    }
                    if (ladder != null) {
                        // The ladder source IS the full method body (its own
                        // `return`s, no wrapping); fall through to the shared
                        // signature-witness + wrapper ref union below.
                        compiledBody = ladder.source();
                        refs = ladder.refs();
                        wildcards = ladder.staticWildcardImports();
                    } else {
                        // facet convertNullSafe (slice 2) — an alias body that is a
                        // direct function call hoisting a meta-deref arg lifts the
                        // hoist to a statement before the `return`
                        // (renderAliasLiftedReturnOrNull, the SET-path analogue of the
                        // PR #143 lift, beside the PR #183 returnIte ladder above).
                        // Like the ladder, the returned source IS the full method body
                        // (its own `return`, no wrapping). Declines (null) for a
                        // non-call body or a call with no meta-deref arg → the plain
                        // renderAlias path below (today's bytes). Gated to the
                        // !usesOutput aliases (the builder-form usesOutput body assigns
                        // the output, never a `return MapperS.of(...)` — strictly
                        // conservative; no usesOutput meta-call carrier exists today).
                        RenderedStatement lifted = base.getUsesOutput() ? null
                                : expressionRenderer.renderAliasLiftedReturnOrNull(shortcut, indentLevel);
                        if (lifted != null) {
                            compiledBody = lifted.source();
                            refs = lifted.refs();
                            wildcards = lifted.staticWildcardImports();
                        } else {
                          // facet inline_then_hoist (PR #219) — a then-chain alias body
                          // hoists its thenArg decl(s) to statement level before a single
                          // return (the alias-seat analogue of renderThenExtractSet).
                          // Declines (null) to the plain renderAlias path below for a
                          // non-then / non-hoistable body. Like the ladder/lifted forms the
                          // returned source IS the full method body. Gated !usesOutput
                          // (a usesOutput body assigns the output, never `return …`).
                          RenderedStatement thenHoist = base.getUsesOutput() ? null
                                  : expressionRenderer.renderAliasThenHoistOrNull(shortcut, indentLevel);
                          // facet aliasReturnCoerce (PR #220): a plain (non-conditional,
                          // non-then, non-call-hoist) alias body coerces to its declared
                          // MapperS<…> signature — a bare ctor → `return MapperS.of(<ctor>);`,
                          // a ComparisonResult → `return <body>.asMapper();`. Null → the plain
                          // renderAlias path below (today's bytes). Gated !usesOutput (a
                          // usesOutput body assigns the output, never `return …`); computed
                          // only once thenHoist has declined.
                          RenderedStatement coerce = (thenHoist == null && !base.getUsesOutput())
                                  ? expressionRenderer.renderAliasReturnCoerceOrNull(
                                          shortcut, base.getSeam(), indentLevel)
                                  : null;
                          // facet aliasBodySinkHoists (PR #349, S1+S3): the alias body
                          // compiles inside a statement-hoist SINK so the #237
                          // evaluate-arg meta-deref route and the #335 collapsed-meta
                          // rewrap fire inside alias methods exactly as in operation
                          // bodies; the drained decls lift ahead of a single return
                          // (full-body contract, like the siblings above). Declines
                          // (null) when no producer fires — the plain path below keeps
                          // today's bytes.
                          RenderedStatement sinkHoists =
                                  (thenHoist == null && coerce == null && !base.getUsesOutput())
                                  ? expressionRenderer.renderAliasSinkHoistsOrNull(
                                          shortcut, base.getSeam(), indentLevel)
                                  : null;
                          if (thenHoist != null) {
                            compiledBody = thenHoist.source();
                            refs = thenHoist.refs();
                            wildcards = thenHoist.staticWildcardImports();
                          } else if (coerce != null) {
                            compiledBody = coerce.source();
                            refs = coerce.refs();
                            wildcards = coerce.staticWildcardImports();
                          } else if (sinkHoists != null) {
                            compiledBody = sinkHoists.source();
                            refs = sinkHoists.refs();
                            wildcards = sinkHoists.staticWildcardImports();
                          } else {
                            // PR-A C2: renderAlias returns RenderedStatement with refs.
                            // facet aliasReturnChainContinuationIndent (PR #232): pass
                            // indentLevel so a wrapped list-op chain continuation re-anchors
                            // to the alias-body statement depth (the plain fall-through is the
                            // only alias path that previously skipped reindentContinuation).
                            RenderedStatement rs = expressionRenderer.renderAlias(shortcut, indentLevel);
                            compiledBody = "return " + rs.source() + ";";
                            // facet aliasOutputBuilderNav (PR #381, W): the OUTPUT-ROOTED
                            // disguised-chain alias wraps `return toBuilder(<nav>.get());`
                            // against its `<Element>.<Element>Builder` signature — the
                            // toBuilder wrap the arm-A comment below anticipated for the
                            // usesOutput class (golden cdm5 NewEquitySwapProduct;
                            // `toBuilder` is inherited, no import). Same-walk gated
                            // (FunctionAliasHelper.isOutputBuilderNavAlias) so the body
                            // wrap and the signature cannot disagree.
                            boolean outputBuilderNav = base.getUsesOutput()
                                    && func.output().map(out -> FunctionAliasHelper
                                            .isOutputBuilderNavAlias(
                                                    shortcut.expression(), out.name()))
                                    .orElse(false);
                            if (outputBuilderNav) {
                                compiledBody = "return toBuilder(" + rs.source() + ".get());";
                            }
                            // facet lambda_item_body_coercion (arm A) — upstream compiles
                            // the alias body with expected = the declared signature
                            // (FunctionGenerator.xtend:319-321), so an ITEM-typed body
                            // takes TypeCoercionService's null-safe item->MapperS wrap
                            // (getItemToMapperSConversionExpression). The fork's two
                            // bare-item top-level producers are the ONLY_ELEMENT collapse
                            // (`<chain>.get()` / `MapperC.of(<call>).get()`) and count
                            // (`<chain>.resultCount()`). Corpus law: ZERO golden
                            // MapperS-signature alias bodies end bare `.get();`; all 332
                            // MapperC-signature bodies are raw chains — so the wrap is
                            // gated on the MapperS<-prefixed signature and declines for
                            // usesOutput (upstream wraps those with toBuilder instead),
                            // first/last (already MapperS-valued) and every other top
                            // node. The MapperS import is already registered for every
                            // MapperS<-signature alias by the wrapper-ref arm below.
                            RExpression aliasExpr = shortcut.expression();
                            boolean itemTypedTop =
                                    (aliasExpr instanceof RListOpExpr listOp
                                            && listOp.op() == ListOp.ONLY_ELEMENT)
                                    || aliasExpr instanceof RCountExpr;
                            // facet aliasSeamSignature (PR #612): the ANALYZED seam's own FORM
                            // (a usesOutput builder-form seam is never MAPPER_SINGLE, and it is
                            // tested separately at this site anyway) — not the prefix of the
                            // rendered signature string.
                            if (itemTypedTop && !base.getUsesOutput()
                                    && base.getSeam().isMapperSingle()) {
                                compiledBody = "return MapperS.of(" + rs.source() + ");";
                            }
                            refs = rs.refs();
                            wildcards = rs.staticWildcardImports();
                          }
                        }
                    }
                }
            } catch (Exception e) {
                // v3.1 C0: a REFUSAL is never recovery — it must reach the per-element
                // boundary (JavaClassGenerator), which attaches the target path and reports
                // it as a generation error. Locked by scripts/ci/refusal-propagation-lint.py.
                if (e instanceof SilentDegradation.Refusal __refusal) throw __refusal;
                compiledBody = "/* TODO: alias compilation error: " + e.getMessage() + " */";
            }

            // facet alias_method_signature_typing — union the structural walk's
            // signature-witness Java refs (the concrete FieldWithMetaX/
            // ReferenceWithMetaX wrapper FunctionAliasHelper surfaces for a
            // meta-annotated leaf) into the rendered body's refs. Idempotent
            // where the body already registered the wrapper.
            if (!base.getRefs().isEmpty()) {
                Set<JavaClass<?>> withSignatureRefs = new HashSet<>(refs);
                withSignatureRefs.addAll(base.getRefs());
                refs = withSignatureRefs;
            }

            // facet alias_receiver_typing: the alias METHOD SIGNATURE's Mapper
            // wrapper needs its own import — a navigation-chain body references
            // only the MapperS.of leaf (instance .map()/.mapC() steps import
            // nothing), so a MULTI alias's `MapperC<? extends T>` signature
            // compiled without golden's `import …mapper.MapperC`. Keyed on the
            // EMITTED seam's own FORM (facet aliasSeamSignature, PR #612): the local
            // `seam` above is the analyzed seam, or its VALUE re-seam when the
            // optimised hook replaced the string — so a flipped alias imports
            // neither wrapper, exactly as keying off the replaced string did, and a
            // usesOutput builder form (never a Mapper seam) stays untouched.
            // Idempotent where the body already referenced the class; a green file's
            // signature matches golden's, whose import the golden then also carries.
            JavaClass<?> wrapperRef = null;
            if (seam.isMapperMulti()) {
                wrapperRef = HandlerHelper.MAPPER_C;
            } else if (seam.isMapperSingle()) {
                wrapperRef = HandlerHelper.MAPPER_S;
            }
            if (wrapperRef != null && !refs.contains(wrapperRef)) {
                Set<JavaClass<?>> withWrapper = new HashSet<>(refs);
                withWrapper.add(wrapperRef);
                refs = withWrapper;
            }

            // Phase 2 of two-phase AliasModel construction (plan §2.11a):
            // analyze() set inferredRefs from the FunctionAliasHelper walk;
            // compileAliases populates refs from the rendered body. Carry
            // base.getInferredRefs() forward unchanged.
            String aliasName = collidingMemberNames.contains(base.getName())
                    ? base.getName() + "1"
                    : base.getName();
            // facet aliasStaticImportEscape (PR #420): an alias whose name collides
            // with a static-imported ExpressionOperatorsNullSafe member the
            // function's own expressions reference escapes `_<name>` on its decl +
            // override (upstream: the file-scope member identifier wins at
            // GeneratorScope.computeActualNames, the class-member alias escapes via
            // JavaClassScope.escapeName — golden expr-misc-primaries
            // `protected abstract MapperS<Boolean> _notEqual(…)` under the
            // function's own `a <> b` / `a <> 0` notEqual references, while the
            // non-colliding `multiExists` alias stays bare). The invocation seat
            // (ReferenceHandler.disambiguateAliasInvocation) consults the SAME
            // memoized census — decl and call cannot disagree. Applied AFTER the
            // dep-collision numbering; a numbered name (`notEqual1`) never matches
            // a member simple name, so the two mechanisms never compose in
            // practice. Corpus-neutral by ABSENCE: zero corpus aliases carry an
            // operator-member name (grep census; zero `_`-escaped alias methods in
            // the 34,686 goldens).
            if (com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper
                    .staticOperatorMembersUsed(func).contains(aliasName)) {
                aliasName = "_" + aliasName;
            }
            result.add(new FunctionTemplateModel.AliasModel(
                    aliasName,
                    returnType,
                    base.getParams(),
                    compiledBody,
                    base.getUsesOutput(),
                    refs,
                    base.getInferredRefs(),
                    wildcards)
                    // facet importCollisionFqn (PR #245): carry the signature return-type
                    // element canonical from phase 1 (analyze) into phase 2 for the seed.
                    .withReturnTypeElementFqn(base.getReturnTypeElementFqn())
                    .withSeam(seam));
        }
        return result;
    }

    private List<FunctionTemplateModel.ConditionModel> compileConditions(List<RCondition> conditions, int conditionIndent) {
        List<FunctionTemplateModel.ConditionModel> result = new ArrayList<>();
        for (RCondition cond : conditions) {
            String compiled = "";
            Set<JavaClass<?>> refs = Set.of();   // PR-A C2
            Set<JavaClass<?>> wildcards = Set.of();  // PR-A C3c.1
            try {
                if (cond.expression() != null) {
                    RenderedStatement rs = expressionRenderer.renderCondition(cond.expression(), conditionIndent);
                    compiled = rs.source();
                    refs = rs.refs();
                    wildcards = rs.staticWildcardImports();
                }
            } catch (Exception e) {
                // v3.1 C0: a REFUSAL is never recovery — it must reach the per-element
                // boundary (JavaClassGenerator), which attaches the target path and reports
                // it as a generation error. Locked by scripts/ci/refusal-propagation-lint.py.
                if (e instanceof SilentDegradation.Refusal __refusal) throw __refusal;
                compiled = "/* TODO: condition compilation error: " + e.getMessage() + " */";
            }
            result.add(new FunctionTemplateModel.ConditionModel(
                    cond.name().orElse(null),
                    cond.definition().orElse(""),
                    compiled,
                    refs,
                    wildcards));
        }
        return result;
    }

    private List<FunctionTemplateModel.ConditionModel> compilePostConditions(
            List<RPostCondition> postConditions, int conditionIndent) {
        List<FunctionTemplateModel.ConditionModel> result = new ArrayList<>();
        for (RPostCondition pc : postConditions) {
            String compiled = "";
            Set<JavaClass<?>> refs = Set.of();   // PR-A C2
            Set<JavaClass<?>> wildcards = Set.of();  // PR-A C3c.1
            try {
                if (pc.expression() != null) {
                    RenderedStatement rs = expressionRenderer.renderCondition(pc.expression(), conditionIndent);
                    compiled = rs.source();
                    refs = rs.refs();
                    wildcards = rs.staticWildcardImports();
                }
            } catch (Exception e) {
                // v3.1 C0: a REFUSAL is never recovery — it must reach the per-element
                // boundary (JavaClassGenerator), which attaches the target path and reports
                // it as a generation error. Locked by scripts/ci/refusal-propagation-lint.py.
                if (e instanceof SilentDegradation.Refusal __refusal) throw __refusal;
                compiled = "/* TODO: post-condition compilation error: " + e.getMessage() + " */";
            }
            result.add(new FunctionTemplateModel.ConditionModel(
                    pc.name().orElse(null),
                    pc.definition().orElse(""),
                    compiled,
                    refs,
                    wildcards));
        }
        return result;
    }

    // =========================================================================
    // Annotation / metadata detection
    // =========================================================================

    private static boolean hasQualificationAnnotation(RFunction func) {
        for (RAnnotationRef ref : func.annotationRefs()) {
            if ("qualification".equals(ref.annotationName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasMeta(RAttribute attr) {
        for (RAnnotationRef ref : attr.annotationRefs()) {
            if ("metadata".equals(ref.annotationName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolve the type parameter for IQualifyFunctionExtension&lt;T&gt;.
     * Uses the first input's type (matching upstream).
     */
    private String resolveQualifyGenericType(RFunction func) {
        if (!func.inputs().isEmpty()) {
            RAttribute firstInput = func.inputs().get(0);
            RType rType = generatorModel.getType(firstInput);
            JavaClass<?> refType = typeTranslator.toJavaReferenceType(rType);
            // LATENT GAP (Seat-1 #426 OBS-1, no witness anywhere): a
            // `[qualification]` func whose FIRST input type collides with
            // java.lang would render `IQualifyFunctionExtension<Math>` BARE while
            // facet javaLangParamFqn (resolveParam) now suppresses that type's
            // import — binding java.lang.Math, non-compiling. Zero corpus
            // carriers (qualification lives in the CDM cells, no colliders — the
            // #426 cp identical-lineset ×2) and no oracle fixture yet; the
            // measured heal is the valueSiteTypeName-class FQN mirror once a
            // qualify+collider fixture exists (the #306 latent-gap convention).
            return refType.getSimpleName();
        }
        return "Object";
    }

    /**
     * Detect label provider annotation: [labelProvider ClassName].
     */
    private static String detectLabelProvider(RFunction func) {
        for (RAnnotationRef ref : func.annotationRefs()) {
            if ("labelProvider".equals(ref.annotationName())) {
                return ref.qualifierName().orElse(null);
            }
        }
        return null;
    }

    // =========================================================================
    // Import collection
    // =========================================================================

    private void collectStandardImports(ImportCollector imports,
                                        boolean outputNeedsBuilder, boolean outputIsMulti,
                                        boolean isQualify, boolean hasConditions,
                                        boolean hasObjectValidator,
                                        List<FunctionTemplateModel.ParamModel> inputs,
                                        FunctionTemplateModel.ParamModel outputModel,
                                        List<FunctionTemplateModel.DependencyModel> dependencies,
                                        String superClassFqn) {
        // Core function imports
        imports.addImport("com.google.inject.ImplementedBy");
        imports.addImport("com.rosetta.model.lib.functions.RosettaFunction");

        // Dependency injection
        if (!dependencies.isEmpty() || hasConditions || hasObjectValidator) {
            imports.addImport("javax.inject.Inject");
        }

        // Conditions
        if (hasConditions) {
            imports.addImport("com.rosetta.model.lib.functions.ConditionValidator");
        }

        // Object validator
        if (hasObjectValidator) {
            imports.addImport("com.rosetta.model.lib.functions.ModelObjectValidator");
        }

        // Qualify functions
        if (isQualify) {
            imports.addImport("com.rosetta.model.lib.functions.IQualifyFunctionExtension");
        }

        // Multi-valued params need List and Collections
        boolean needsList = outputIsMulti;
        boolean needsCollections = false;
        boolean needsCollectors = false;
        boolean needsArrayList = false;
        boolean needsOptional = false;

        for (FunctionTemplateModel.ParamModel inp : inputs) {
            if (inp.getIsMulti()) {
                needsList = true;
                needsCollections = true;
            }
        }

        if (outputIsMulti) {
            needsList = true;
            needsArrayList = true;
            if (outputNeedsBuilder) {
                needsOptional = true;
                needsCollectors = true;
            }
        } else if (outputNeedsBuilder) {
            needsOptional = true;
        }

        if (needsList) imports.addImport("java.util.List");
        if (needsCollections) imports.addImport("java.util.Collections");
        if (needsCollectors) imports.addImport("java.util.stream.Collectors");
        if (needsArrayList) imports.addImport("java.util.ArrayList");
        if (needsOptional) imports.addImport("java.util.Optional");

        // Import param types. facet fpmlInputFqn (PR #195): an input whose simple type name
        // collides with the OUTPUT type carries a NULL typeFqn (rendered FULLY QUALIFIED inline in
        // the signatures instead) — skip those, exactly as the dependency-collision loop below.
        for (FunctionTemplateModel.ParamModel inp : inputs) {
            if (inp.getTypeFqn() != null) {
                imports.addImport(inp.getTypeFqn());
            }
        }
        // facet fqnSelfCollision (PR #366): a self-colliding output carries a NULL
        // typeFqn (FQN-inline, import suppressed) — skip it like the dep/input cases.
        if (outputModel != null && outputModel.getTypeFqn() != null) {
            imports.addImport(outputModel.getTypeFqn());
        }

        // Import dependencies. Engine PR #6 facet (b): a rule-target dependency
        // whose generated class simple name collides with the host class carries
        // a NULL typeFqn (its type is rendered FULLY QUALIFIED inline in the
        // @Inject field instead). Skip those — importing the colliding FQN would
        // be wrong (and the golden emits no import for it).
        for (FunctionTemplateModel.DependencyModel dep : dependencies) {
            if (dep.getTypeFqn() != null) {
                imports.addImport(dep.getTypeFqn());
            }
        }

        // Super class
        if (superClassFqn != null) {
            imports.addImport(superClassFqn);
        }
    }

    private void collectDispatchImports(ImportCollector imports,
                                        List<FunctionTemplateModel.ParamModel> inputs,
                                        FunctionTemplateModel.ParamModel outputModel,
                                        List<FunctionTemplateModel.DispatchVariantModel> variants,
                                        String dispatchEnumType,
                                        boolean outputNeedsBuilder, boolean outputIsMulti) {
        // Core
        imports.addImport("com.google.inject.ImplementedBy");
        imports.addImport("com.rosetta.model.lib.functions.RosettaFunction");
        imports.addImport("javax.inject.Inject");

        // Variant-level imports: conditions, validators, dependencies
        boolean anyVariantHasConditions = false;
        boolean anyVariantHasObjectValidator = false;
        boolean anyVariantNeedsList = false;
        boolean anyVariantNeedsCollections = false;
        boolean anyVariantNeedsCollectors = false;
        boolean anyVariantNeedsArrayList = false;
        boolean anyVariantNeedsOptional = false;

        for (var v : variants) {
            var vm = v.getVariantModel();
            if (vm.getHasConditions()) anyVariantHasConditions = true;
            if (vm.getHasObjectValidator()) anyVariantHasObjectValidator = true;
            if (vm.getOutputIsMulti()) {
                anyVariantNeedsList = true;
                anyVariantNeedsArrayList = true;
                if (vm.getOutputNeedsBuilder()) {
                    anyVariantNeedsOptional = true;
                    anyVariantNeedsCollectors = true;
                }
            } else if (vm.getOutputNeedsBuilder()) {
                anyVariantNeedsOptional = true;
            }
            for (var inp : vm.getInputs()) {
                if (inp.getIsMulti()) {
                    anyVariantNeedsList = true;
                    anyVariantNeedsCollections = true;
                }
            }
            for (var dep : vm.getDependencies()) {
                // Engine PR #6 facet (b): skip null-FQN (collision) deps — same
                // class-of-issue guard as collectStandardImports.
                if (dep.getTypeFqn() != null) {
                    imports.addImport(dep.getTypeFqn());
                }
            }
        }

        if (anyVariantHasConditions) {
            imports.addImport("com.rosetta.model.lib.functions.ConditionValidator");
        }
        if (anyVariantHasObjectValidator) {
            imports.addImport("com.rosetta.model.lib.functions.ModelObjectValidator");
        }

        // Multi-valued params at dispatch level
        boolean needsList = outputIsMulti || anyVariantNeedsList;
        for (FunctionTemplateModel.ParamModel inp : inputs) {
            if (inp.getIsMulti()) needsList = true;
        }

        if (needsList) imports.addImport("java.util.List");
        if (anyVariantNeedsCollections) imports.addImport("java.util.Collections");
        if (anyVariantNeedsCollectors) imports.addImport("java.util.stream.Collectors");
        if (anyVariantNeedsArrayList) imports.addImport("java.util.ArrayList");
        if (anyVariantNeedsOptional) imports.addImport("java.util.Optional");

        // Import param types. facet fpmlInputFqn (PR #195): skip an input whose typeFqn was nulled
        // by the input-vs-output collision (rendered FQN-inline) — same convention as the standard
        // path + the dependency-collision loop.
        for (FunctionTemplateModel.ParamModel inp : inputs) {
            if (inp.getTypeFqn() != null) {
                imports.addImport(inp.getTypeFqn());
            }
        }
        // facet fqnSelfCollision (PR #366): null typeFqn = FQN-inline, no import.
        if (outputModel != null && outputModel.getTypeFqn() != null) {
            imports.addImport(outputModel.getTypeFqn());
        }
    }

    /**
     * PR-A §9.1 C2: union all per-model refs sets into a single
     * {@code Set<JavaClass<?>>} for the whole function. For {@link
     * FunctionTemplateModel.AliasModel} this also translates A3-D2-02
     * {@code inferredRefs} ({@link RType}) to {@link JavaClass} via
     * {@link JavaTypeTranslator#toJavaReferenceType}; null translations
     * are skipped.
     */
    private Set<JavaClass<?>> unionExpressionRefs(
            List<FunctionTemplateModel.OperationModel> operations,
            List<FunctionTemplateModel.AliasModel> aliases,
            List<FunctionTemplateModel.ConditionModel> preConditions,
            List<FunctionTemplateModel.ConditionModel> postConditions) {
        Set<JavaClass<?>> refs = new HashSet<>();
        for (var op : operations) refs.addAll(op.getRefs());
        for (var alias : aliases) {
            refs.addAll(alias.getRefs());
            if (typeTranslator != null) {
                for (RType rt : alias.getInferredRefs()) {
                    JavaClass<?> jc = typeTranslator.toJavaReferenceType(rt);
                    if (jc != null) refs.add(jc);
                }
            }
        }
        for (var cond : preConditions) refs.addAll(cond.getRefs());
        for (var cond : postConditions) refs.addAll(cond.getRefs());
        return refs;
    }

    /**
     * PR-A §9.1 C3c.1: union staticWildcardImports across all per-model sets.
     * Parallel to {@link #unionExpressionRefs} but for the static-wildcard
     * channel (e.g. {@code import static ExpressionOperatorsNullSafe.*;}).
     * Feeds {@link ImportCollector#addStaticImport} — replaces the
     * FunctionGenerator L843-849 substring ladder when C3c.2 deletes it.
     */
    private Set<JavaClass<?>> unionExpressionStaticWildcardImports(
            List<FunctionTemplateModel.OperationModel> operations,
            List<FunctionTemplateModel.AliasModel> aliases,
            List<FunctionTemplateModel.ConditionModel> preConditions,
            List<FunctionTemplateModel.ConditionModel> postConditions) {
        Set<JavaClass<?>> wildcards = new HashSet<>();
        for (var op : operations) wildcards.addAll(op.getStaticWildcardImports());
        for (var alias : aliases) wildcards.addAll(alias.getStaticWildcardImports());
        for (var cond : preConditions) wildcards.addAll(cond.getStaticWildcardImports());
        for (var cond : postConditions) wildcards.addAll(cond.getStaticWildcardImports());
        return wildcards;
    }

    /**
     * PR-A §9.1 C3c.2: populate imports from the structured
     * {@code Set<JavaClass<?>>} carried by each {@link RenderedStatement}
     * and by {@link FunctionTemplateModel.AliasModel#getInferredRefs} via
     * A3-D2-02. Sole feeder for library-class and domain-type expression
     * imports post-C3c.2 (replaces the deleted substring ladder +
     * {@code resolveExpressionTypeImports} regex path).
     *
     * <p>{@link JavaClass#getCanonicalName()} returns a {@link DottedPath};
     * {@code .withDots()} converts it to the dot-separated FQN string
     * expected by {@link ImportCollector#addImport(String)}.
     */
    private void collectExpressionImportsFromRefs(ImportCollector imports,
                                                  Set<JavaClass<?>> refs) {
        for (JavaClass<?> cls : refs) {
            imports.addImport(cls.getCanonicalName().withDots());
        }
    }

    /**
     * PR-A §9.1 C3c.2: feed static-wildcard imports into {@link ImportCollector}.
     * Each class is converted to {@code <FQN>.*} and passed to
     * {@link ImportCollector#addStaticImport}. Sole feeder for static-wildcard
     * imports post-C3c.2 (replaces the deleted ExpressionOperatorsNullSafe
     * substring trigger ladder). Populated by handlers that emit calls into
     * statically-imported classes (ComparisonHandler, ExistenceHandler).
     */
    private void collectExpressionStaticImportsFromWildcards(ImportCollector imports,
                                                             Set<JavaClass<?>> wildcards) {
        for (JavaClass<?> cls : wildcards) {
            imports.addStaticImport(cls.getCanonicalName().withDots() + ".*");
        }
    }

    // =========================================================================
    // Javadoc
    // =========================================================================

    /**
     * facet fnInputDepCollisionEscape (W42 finding #27, PR #436): the Java render
     * name of a function INPUT — the raw attribute name, {@code "_"}-escaped when
     * upstream's scope allocation would find the desired name unavailable:
     * <ul>
     *   <li>the name is already taken by an {@code @Inject} dependency field on the
     *       parent class scope ({@link FunctionDependencyCollector#hasFunctionDependencyNamed}
     *       — EXCLUDING names the member-numbering facet vacates to {@code name0},
     *       per {@link FunctionDependencyCollector#collidingDependencyAliasNames});</li>
     *   <li>the name is not a valid Java identifier (a keyword —
     *       {@code javax.lang.model.SourceVersion.isName});</li>
     *   <li>the name equals the emitted class's own simple name (registered on the
     *       file scope by upstream {@code JavaClassScope.createAndRegisterIdentifier}).</li>
     * </ul>
     * The SINGLE source of truth for both halves: the template seats (ParamModel /
     * javadoc, via {@code buildStandardModel}) and the expression-side reference
     * render ({@code ReferenceHandler}'s variable path) — so the signature and every
     * body mention agree. SCOPE (the #194 usRename convention): the dependency arm
     * reads the FUNCTION-callee surface only — rule-injection and DeepPathUtil
     * fields are deliberately not mirrored (zero carriers; widen with an anchor if
     * one appears). One escape pass, as the output-name sibling.
     *
     * <p>A {@code null} {@code hostClassSimpleName} is tolerated (the
     * dispatch-wrapper javadoc path passes it deliberately): {@code rawName} is a
     * parsed attribute name and never null, and {@code String.equals(null)} is
     * {@code false} per the JLS — so a null host simply disables the class-name
     * arm, leaving the dependency and keyword arms live. No NPE path exists.
     */
    public static String escapedFunctionInputName(RFunction func, String rawName,
                                                  String hostClassSimpleName) {
        boolean depCollision = FunctionDependencyCollector.hasFunctionDependencyNamed(func, rawName)
                && !FunctionDependencyCollector.collidingDependencyAliasNames(func).contains(rawName);
        if (depCollision
                || !javax.lang.model.SourceVersion.isName(rawName)
                || rawName.equals(hostClassSimpleName)) {
            return "_" + rawName;
        }
        return rawName;
    }

    private List<String> buildJavadocParams(RFunction func, RFunction depFunc,
                                            String hostClassSimpleName, String escapedOutputName) {
        List<String> params = new ArrayList<>();
        for (RAttribute input : func.inputs()) {
            if (isSynthesizedInput(input)) continue;
            // facet fnInputDepCollisionEscape (PR #436): the @param tag names the
            // input VARIABLE, so it carries the same "_"-escape as the parameter
            // (golden `@param _a` — the #194 @return precedent at the input seat).
            String paramName = escapedFunctionInputName(depFunc, input.name(), hostClassSimpleName);
            String def = input.definition().orElse(null);
            if (def != null && !def.isEmpty()) {
                params.add("@param " + paramName + " " + escapeJavadoc(def));
            } else {
                params.add("@param " + paramName + " ");
            }
        }
        if (func.output().isPresent()) {
            RAttribute out = func.output().get();
            // facet usRename (PR #194): the @return tag names the output VARIABLE,
            // so it carries the same "_"-escape as the output local when the output
            // collides with a dependency (golden `@return _interestRateLeg1`).
            String returnName = escapedOutputName != null ? escapedOutputName : out.name();
            String def = out.definition().orElse(null);
            if (def != null && !def.isEmpty()) {
                params.add("@return " + returnName + " " + escapeJavadoc(def));
            } else {
                params.add("@return " + returnName + " ");
            }
        }
        return params;
    }

    /**
     * Escape special characters for Javadoc HTML. Matches upstream encoding:
     * {@code &} -> {@code &amp;}, {@code '} -> {@code &#39;}, {@code >} -> {@code &gt;}.
     */
    private static String escapeJavadoc(String text) {
        // & must be escaped first to avoid double-escaping
        return text.replace("&", "&amp;")
                   .replace("'", "&#39;")
                   .replace(">", "&gt;");
    }

    // =========================================================================
    // Version resolution
    // =========================================================================

    private String resolveVersion(RFunction func) {
        if (func.parent() instanceof RModel model) {
            return generatorModel.version(model);
        }
        return null;
    }

    // =========================================================================
    // File path computation
    // =========================================================================

    private String computeFilePath(RFunction func) {
        ModelSymbolId symbolId = generatorModel.symbolId(func);
        var javaClass = typeTranslator.toFunctionJavaClass(symbolId);
        String packagePath = javaClass.getPackageName().withForwardSlashes();
        return packagePath + "/" + javaClass.getSimpleName() + ".java";
    }

    /**
     * {@link #computeFilePath(RFunction)} for the failure-capture path: attribution
     * must never turn a per-object failure into a thrown secondary (a function whose
     * symbol cannot resolve would otherwise mask its own refusal), so any exception
     * here degrades to {@code null} — the unattributed-failure contract
     * ({@code GenerationException.getTargetPath()} javadoc: null = treated unwaivered).
     *
     * <p>That INCLUDES a {@code Refusal} (the cq review's catch): this helper runs
     * INSIDE the phase-2/phase-3 BOUNDARY catches, mid-attribution of an already
     * captured primary failure — rethrowing a secondary Refusal from here would
     * escape the boundary and abort the whole model, the exact inversion the
     * refusal-propagation lint's own comments warn about. A Refusal is-a
     * {@code GenerationException}, so the first clause claims it (and the lint's
     * prior-GE-clause rule recognises the broad clause below as refusal-terminal).
     */
    private String computeFilePathSafe(RFunction func) {
        try {
            return computeFilePath(func);
        } catch (GenerationException ge) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // =========================================================================
    // Utilities
    // =========================================================================

    /**
     * Check if an input attribute is a synthesized parameter added by
     * {@code GeneratedInputRule} for functions with no explicit inputs.
     * These should be excluded from code generation.
     *
     * <p>Package-visible so {@link FunctionExpressionRenderer}'s no-input-function
     * rule-body branch (no_arg_evaluate facet) can detect a function with no REAL
     * inputs (every input is this synthesized placeholder) and emit an argless
     * {@code <fn>.evaluate()} call.
     */
    public static boolean isSynthesizedInput(RAttribute attr) {
        return "__synthesized_input__".equals(attr.name());
    }

    private static String lowerCamelCase(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}
