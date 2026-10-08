package com.regnosys.rosetta.generator.java.optimised;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.enums.ListOp;
import com.regnosys.rosetta.ast.expressions.constructors.RConstructorExpr;
import com.regnosys.rosetta.ast.expressions.unary.RCountExpr;
import com.regnosys.rosetta.ast.expressions.unary.RListOpExpr;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper;
import com.regnosys.rosetta.generator.java.function.FunctionExpressionRenderer;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionTemplateModel;
import com.regnosys.rosetta.generator.java.function.RenderedStatement;
import com.regnosys.rosetta.generator.java.scoping.JavaClassScope;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RGeneratedJavaClass;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaParameterizedType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The v3 OPTIMISED variant of {@link FunctionGenerator} — the FIRST emission-family
 * generator (PR-4: navigation chains, tranche 1). It reuses the entire reference
 * function pipeline unchanged (the {@code IRFunctionGenerator}-precedent seam shape —
 * that reference-backend twin lives in rune-ir-java, not on this module's classpath),
 * swapping in the {@link OptimisedExpressionCompiler} via the
 * {@link FunctionGenerator#createExpressionCompiler()} factory seam and the
 * {@link OptimisedFunctionExpressionRenderer} via the renderer seam.
 *
 * <p><b>The optimised window.</b> The compiler converts sites ONLY while
 * {@link #generateWithErrors(Map)} (the FUNCTION kind) or the
 * {@link #buildClassWithBaseInterface} override (family 3, census § 12 — the
 * rule/report renders: this generator instance is ALSO the body-renderer for the DRR
 * rule/report generators, constructed around the seam-supplied funcGen and delegating
 * to that single canonical entry) is on the stack. The rule-path bracket ALSO stamps
 * the § 12b.2 OWNER BRIDGE — the synthetic {@code fromRule}/{@code fromReport}
 * function being rendered — because rule bodies have no ancestry owner
 * ({@code ROperation.setExpression} never re-parents). Label providers never pass
 * through either entry and stay reference-shaped.
 *
 * <p>The dependency collector's compiler instance (the second
 * {@code createExpressionCompiler()} product) never has its window opened either —
 * dependency discovery reads the reference shapes.
 */
public class OptimisedFunctionGenerator extends FunctionGenerator {

    public OptimisedFunctionGenerator(GeneratorModel generatorModel,
            JavaTypeTranslator typeTranslator, JavaTypeUtil typeUtil) {
        super(generatorModel, typeTranslator, typeUtil);
    }

    /**
     * Substitutes the optimised compiler. Invoked from the superclass constructor, so
     * it reads only the inherited {@code generatorModel}/{@code typeTranslator}/
     * {@code typeUtil} (assigned before the call) and no field of this subclass — the
     * {@link FunctionGenerator#createExpressionCompiler()} construction-time invariant.
     */
    @Override
    protected ExpressionCompiler createExpressionCompiler() {
        return new OptimisedExpressionCompiler(generatorModel, typeTranslator, typeUtil);
    }

    /**
     * Substitutes the operation-context-marking renderer. Invoked from the superclass
     * constructor; uses only its {@code compiler} argument (the
     * {@link FunctionGenerator#createFunctionExpressionRenderer} invariant).
     */
    @Override
    protected FunctionExpressionRenderer createFunctionExpressionRenderer(ExpressionCompiler compiler) {
        return new OptimisedFunctionExpressionRenderer((OptimisedExpressionCompiler) compiler);
    }

    /**
     * The FUNCTION-kind emission entry — brackets the run in the optimised window so
     * eligible operation-body sites convert; every render outside this call (rule and
     * report bodies through the shared instance, the dependency collector's walks)
     * stays reference-shaped.
     */
    @Override
    public List<GenerationException> generateWithErrors(Map<String, String> output) {
        OptimisedExpressionCompiler compiler = optimisedExpressionCompiler();
        compiler.enterOptimisedWindow();
        try {
            return super.generateWithErrors(output);
        } finally {
            compiler.exitOptimisedWindow();
        }
    }

    /**
     * THE FAMILY-3 WINDOW BRACKET (census § 12b.1–2): the rule/report emission
     * entry — the single canonical overload BOTH {@code RuleGenerator} and
     * {@code ReportGenerator} delegate to — bracketed in the optimised window
     * with the OWNER BRIDGE stamped (the synthetic {@code fromRule}/{@code
     * fromReport} function — rule bodies have no ancestry owner). Save/restore
     * keeps nested renders correct; the report face converts ZERO sites by
     * construction (its operations are synthesized rule-invocation refs — the
     * emission gate asserts the zero empirically).
     */
    @Override
    @SuppressWarnings("rawtypes") // List<JavaParameterizedType> — the superclass
                                  // signature's own erasure shape (the
                                  // ReportFunction<I,O> raw-type marker idiom at
                                  // the T4/T5/T6 call sites).
    public String buildClassWithBaseInterface(RFunction func,
            RGeneratedJavaClass<?> clazz,
            boolean isAbstract,
            List<JavaParameterizedType> baseInterfaces,
            Map<Class<?>, String> annotations,
            boolean renderAsReportFunction,
            JavaClassScope scope,
            List<String> supportingImports) {
        OptimisedExpressionCompiler compiler = optimisedExpressionCompiler();
        RFunction savedOwner = compiler.bridgeOwner();
        compiler.setBridgeOwner(func);
        compiler.enterOptimisedWindow();
        try {
            return super.buildClassWithBaseInterface(func, clazz, isAbstract,
                    baseInterfaces, annotations, renderAsReportFunction, scope,
                    supportingImports);
        } finally {
            compiler.exitOptimisedWindow();
            compiler.setBridgeOwner(savedOwner);
        }
    }

    /** The rendering compiler, typed — the conversion counters' read surface. */
    public OptimisedExpressionCompiler optimisedExpressionCompiler() {
        return (OptimisedExpressionCompiler) renderingExpressionCompiler();
    }

    /**
     * THE § 6.3 T1 VALUE-SEAM EMISSION (the {@code compileAliases} flip seat):
     * a policy-flipped alias re-types to the value seam — the return type is
     * the seam's own Mapper wrapper form stripped ({@code T} /
     * {@code List<? extends T>}; the render channel is the authority, sentinel
     * elements ride verbatim), and the body emits per the plan's shape:
     *
     * <pre>
     *   SINGLE          return (p == null ? null : p.getA() == null ? null : p.getA().getB());
     *   TERMINAL_MULTI  return p == null ? Collections.&lt;X&gt;emptyList() : … : p.getA().getBs();
     *                   (the § 2 empty contract: a multi seam returns the EMPTY
     *                   LIST for absent — the terminal getter's own null coalesced
     *                   too, matching {@code getMulti()}'s observable exactly)
     *   STREAM          return (&lt;the reference Mapper chain&gt;).getMulti();
     *                   (the § 4 disclosed fallback — no landed stream ladder;
     *                   Mapper-bodied behind the value seam, counted per cell)
     * </pre>
     *
     * <p>Window-gated (belt — {@code compileAliases} only runs inside
     * {@link #generateWithErrors}'s bracket) and answered by the SAME policy
     * instance the invocation-seat leaf and the chain-root admission read, so
     * the seam and every consumer seat agree by construction. The reference
     * route's hook stays the constant null — byte-inert, ring-proven.
     */
    @Override
    protected AliasValueForm aliasValueFormOrNull(RFunction func, RShortcut shortcut,
            FunctionTemplateModel.AliasModel base, int indentLevel) {
        OptimisedExpressionCompiler compiler = optimisedExpressionCompiler();
        if (!compiler.optimisedWindowOpen() || shortcut.expression() == null) {
            return null;
        }
        AliasValueSeamPolicy.FlipFacts facts =
                compiler.aliasValueSeamPolicy().flipFactsOrNull(func, shortcut);
        if (facts == null) {
            return null;
        }
        NavigationChainClassifier.LadderPlan plan = facts.plan();
        if (plan == null) {
            // T2 (PR-23) ∪ T3 (PR-24): a VALUE-BODY member — chain/APPLY-headed
            // or the CONDITIONAL + tail kinds.
            return valueBodyForm(func, shortcut, facts, base, indentLevel, compiler);
        }
        String body;
        Set<JavaClass<?>> refs;
        Set<JavaClass<?>> wildcards = Set.of();
        switch (plan.shape()) {
            case SINGLE -> {
                body = "return (" + plan.ladderCode() + ");";
                refs = Set.of();
            }
            case TERMINAL_MULTI -> {
                body = terminalMultiValueBody(plan, facts.multiElementText());
                refs = Set.of(HandlerHelper.LIST, HandlerHelper.COLLECTIONS);
            }
            case STREAM -> {
                // The disclosed Mapper-bodied fallback: the reference chain
                // rendered by the SAME renderer the plain path uses, value-
                // unwrapped ONCE at the seam boundary (getMulti() = today's
                // consumer observable: the item values, [] when the chain
                // nulls out — the § 2 empty contract for free).
                RenderedStatement rs = functionExpressionRenderer()
                        .renderAlias(shortcut, indentLevel);
                body = "return (" + rs.source() + ").getMulti();";
                Set<JavaClass<?>> withList = new LinkedHashSet<>(rs.refs());
                withList.add(HandlerHelper.LIST);
                refs = withList;
                wildcards = rs.staticWildcardImports();
            }
            default -> {
                return null;
            }
        }
        compiler.aliasValueSeamPolicy().recordSeamEmitted(facts);
        return new AliasValueForm(facts.valueReturnType(), body, refs, wildcards);
    }

    /**
     * THE § 6.3 T2∪T3 VALUE-BODY EMISSION (T2 PR-23, the plan § 4 chain/APPLY
     * rows; T3 PR-24, the CONDITIONAL + tail rows): probes the SAME facet
     * paths {@code compileAliases} probes and in the SAME order — the
     * conditional RETURN ladder, the choice-switch ladder (both through the
     * {@link FunctionExpressionRenderer.AliasValueLadderForm} rung
     * decoration), then lifted-return, then-hoist, coerce, sink-hoists (the
     * decl-led paths, each through the
     * {@link FunctionExpressionRenderer.AliasValueBoundary} decorator so the
     * hoisted decls render verbatim and ONLY the trailing return value
     * re-shapes), then the item-typed-top direct arm, then the structural
     * plain path {@code renderAliasValueOrNull}:
     *
     * <pre>
     *   LADDER          if (cond.getOrDefault(false)) { return &lt;value&gt;; } return &lt;value&gt;;
     *                   (the § 4 if-over-re-shaped-arms form — rungs invert
     *                   their Mapper lifts, unwrap structurally, or take the
     *                   paren boundary; empty terminals are null /
     *                   Collections.&lt;X&gt;emptyList(); the conditions keep
     *                   today's Mapper composition, a disclosed non-goal)
     *   LADDER:switch   the instanceof ladder with value rungs + value empties
     *   DIRECT          return dayCountBasis.evaluate(date);   // and bare
     *                   ctors, item-typed only-element/count collapses
     *   BOUNDARY        return (&lt;the reference Mapper chain&gt;).get();   // .getMulti() for C
     *   *:hoist         final X x = …;  +  the re-shaped trailing return
     * </pre>
     *
     * <p>The last-resort arm (a non-expression plain compile) boundary-wraps
     * the plain Mapper render — any genuinely statement-shaped body is
     * compile-loud in the differential gate, never silently mis-shaped. The
     * then-hoist arm's tally floor: a raw already-value collapse counts
     * BOUNDARY:hoist (the boundary is never invoked there — the honest-count
     * conservative direction; the sink's item-top verbatim arm is labelled
     * DIRECT by the caller's own AST gate below instead). Multi seams add
     * {@code List} to the signature refs (the T1 precedent).
     */
    private AliasValueForm valueBodyForm(RFunction func, RShortcut shortcut,
            AliasValueSeamPolicy.FlipFacts facts, FunctionTemplateModel.AliasModel base,
            int indentLevel, OptimisedExpressionCompiler compiler) {
        FunctionExpressionRenderer renderer = functionExpressionRenderer();
        boolean multi = facts.isMulti();
        boolean[] direct = new boolean[1];
        FunctionExpressionRenderer.AliasValueBoundary boundary =
                renderer.valueBoundary(multi, d -> direct[0] = d);
        FunctionExpressionRenderer.AliasValueLadderForm ladderForm =
                new FunctionExpressionRenderer.AliasValueLadderForm(
                        multi, facts.multiElementText());
        RExpression expr = shortcut.expression();
        boolean itemTop = (expr instanceof RListOpExpr topListOp
                && topListOp.op() == ListOp.ONLY_ELEMENT)
                || expr instanceof RCountExpr;
        String form;
        String body;
        Set<JavaClass<?>> refs;
        Set<JavaClass<?>> wildcards;
        // T3: the ladder paths FIRST — compileAliases' own probe order (they
        // fire before the decl-led paths there, and a conditional/switch body
        // reaches the decl-led probes only after both ladders declined).
        RenderedStatement ladder = renderer.renderAliasReturnLadderOrNull(
                shortcut, base.getSeam(), indentLevel, ladderForm);
        String ladderLabel = "LADDER";
        if (ladder == null) {
            ladder = renderer.renderAliasChoiceSwitchLadderViaJoinOrNull(
                    shortcut, func, base.getSeam(), indentLevel, ladderForm);
            ladderLabel = "LADDER:switch";
        }
        if (ladder != null) {
            form = ladderLabel;
            body = ladder.source();
            refs = new LinkedHashSet<>(ladder.refs());
            wildcards = ladder.staticWildcardImports();
        } else {
            // The decl-led probes in compileAliases' own order (lifted → then →
            // coerce → sink); the coerce probe runs exactly where compileAliases
            // probes it — after then-hoist declined, before the sink.
            RenderedStatement coerce = null;
            RenderedStatement hoisted =
                    renderer.renderAliasLiftedReturnOrNull(shortcut, indentLevel, boundary);
            if (hoisted == null) {
                hoisted = renderer.renderAliasThenHoistOrNull(shortcut, indentLevel, boundary);
            }
            if (hoisted == null) {
                coerce = renderer.renderAliasReturnCoerceOrNull(
                        shortcut, base.getSeam(), indentLevel, true);
                if (coerce == null) {
                    hoisted = renderer.renderAliasSinkHoistsOrNull(shortcut,
                            base.getSeam(), indentLevel, boundary);
                }
            }
            if (coerce != null) {
                // The coerce arms' value forms: a ctor body IS the value
                // (DIRECT); a ComparisonResult body takes (…).get() (BOUNDARY)
                // — the arm split mirrors the render's own ctorWrap gate.
                form = expr instanceof RConstructorExpr ? "DIRECT" : "BOUNDARY";
                body = coerce.source();
                refs = new LinkedHashSet<>(coerce.refs());
                wildcards = coerce.staticWildcardImports();
            } else if (hoisted != null) {
                form = ((direct[0] || itemTop) ? "DIRECT" : "BOUNDARY") + ":hoist";
                body = hoisted.source();
                refs = new LinkedHashSet<>(hoisted.refs());
                wildcards = hoisted.staticWildcardImports();
            } else if (itemTop && !multi) {
                // The item-typed-top direct arm (T3): an only-element/count body
                // compiles to the BARE item value (compileAliases lifts it into
                // MapperS.of at the plain arm; the value seam takes it verbatim).
                RenderedStatement rs = renderer.renderAlias(shortcut, indentLevel);
                form = "DIRECT";
                body = "return " + rs.source() + ";";
                refs = new LinkedHashSet<>(rs.refs());
                wildcards = rs.staticWildcardImports();
            } else {
                FunctionExpressionRenderer.AliasValueRender value =
                        renderer.renderAliasValueOrNull(shortcut, indentLevel);
                if (value != null) {
                    form = value.direct() ? "DIRECT" : "BOUNDARY";
                    body = value.direct()
                            ? "return " + value.rendered().source() + ";"
                            : "return (" + value.rendered().source()
                                    + (multi ? ").getMulti();" : ").get();");
                    refs = new LinkedHashSet<>(value.rendered().refs());
                    wildcards = value.rendered().staticWildcardImports();
                } else {
                    // The corpus-zero last resort: the plain Mapper render behind
                    // the disclosed boundary (compile-loud if ever statement-shaped).
                    RenderedStatement rs = renderer.renderAlias(shortcut, indentLevel);
                    form = "BOUNDARY:block";
                    body = "return (" + rs.source()
                            + (multi ? ").getMulti();" : ").get();");
                    refs = new LinkedHashSet<>(rs.refs());
                    wildcards = rs.staticWildcardImports();
                }
            }
        }
        if (multi) {
            refs.add(HandlerHelper.LIST);
        }
        compiler.aliasValueSeamPolicy().recordValueSeamEmitted(facts, form);
        return new AliasValueForm(facts.valueReturnType(), body, refs, wildcards);
    }

    /**
     * The TERMINAL_MULTI value body — the § 9a guard structure with the § 2
     * empty contract's {@code Collections.<X>emptyList()} in every else seat
     * (mid-chain nulls AND the terminal getter's own null): getter runs repeat
     * per guard level exactly as the landed ladder repeats them (the JIT-CSE
     * law); ternaries right-associate, no interior parens.
     */
    private static String terminalMultiValueBody(NavigationChainClassifier.LadderPlan plan,
            String elementText) {
        String empty = "Collections.<" + elementText + ">emptyList()";
        StringBuilder sb = new StringBuilder("return ");
        String prefix = plan.rootName();
        for (String accessor : plan.accessors()) {
            sb.append(prefix).append(" == null ? ").append(empty).append(" : ");
            prefix = prefix + "." + accessor + "()";
        }
        sb.append(prefix).append(" == null ? ").append(empty).append(" : ").append(prefix);
        return sb.append(';').toString();
    }
}
