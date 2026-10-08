package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.expression.handlers.ControlFlowHandler;
import com.regnosys.rosetta.generator.java.function.StatementHoistSession;
import com.regnosys.rosetta.ir.expr.NodeId;
import com.regnosys.rosetta.ir.expr.adapter.ExpressionToIRAdapter;
import com.regnosys.rosetta.ir.expr.anf.Bind;
import com.regnosys.rosetta.ir.expr.anf.ConditionalHoist;
import com.regnosys.rosetta.ir.expr.anf.JoinPoint;
import com.regnosys.rosetta.ir.expr.anf.Normalize;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * Path-2 (IR-routed) variant of {@link ControlFlowHandler} — the Wave-6 Phase-C <em>expression-compiler</em> seam
 * by which the neutral ANF substrate drives the {@code ifThenElseResult} hoist NAME at the two
 * expression-compiler-reached mint sites: the {@code ComparisonResult} arm
 * ({@code ControlFlowHandler.hoistAsComparisonResultOrNull}) and the ctor item-local arm
 * ({@code ControlFlowHandler.hoistAsItemLocalOrNull} — the §6 cross-group reorder carriers, e.g. the dump §6
 * {@code Create_MarginCorrectionData}). These two seats are reached via the EXPRESSION COMPILER
 * ({@code ControlFlowHandler.handle}), distinct from the RENDERER pathed-SET seat
 * ({@code FunctionExpressionRenderer.ifThenElseResultBaseName}) the {@code IRFunctionExpressionRenderer} subclass
 * drives (L-061). The lab's {@code IRExpressionCompiler} supplies this subclass through the
 * {@code ExpressionCompiler.createControlFlowHandler()} factory (the expression-compiler "Step 0", L-064/L-068).
 *
 * <h2>Slice 6: drive the expression-compiler {@code ifThenElseResult} hoist NAME</h2>
 * {@link #ifThenElseResultBaseName(RConditionalExpr, GeneratorModel)} is overridden to SOURCE the hoist's base
 * lexeme from the neutral ANF substrate — it runs {@link ExpressionToIRAdapter#adaptConditionalFacts} +
 * {@link Normalize#conditionalToBind} (the SAME lowering L-061 uses for the renderer pathed-SET seat) and returns
 * the {@code JoinPoint}-valued {@link Bind}'s {@code TempName} base. When the condition is a bare function call,
 * {@code conditionalToBind} returns {@code [boolean (Block-valued), ifThenElseResult (JoinPoint-valued)]} — the §6
 * boolean-before-{@code ifThenElseResult} order; the scan selects the {@code JoinPoint} (the ite), never the
 * Block (the boolean). By the L-029 split, the IR drives ONLY the neutral name; the surrounding Java — the
 * {@code 0..n-1} numbering + the {@code _boolean} keyword escape, the decl type, the arm bodies, the boolean guard
 * and the boolean-before-ite render-walk ORDER — stays in the legacy {@code ControlFlowHandler} oracle,
 * byte-identical. The ANF base equals the legacy constant by construction, so Path-2 stays byte-for-byte identical
 * to Path-1. The §6 reorder ORDER is byte-PROVEN by the gate; it is the oracle's render-walk, NOT IR-driven.
 *
 * <p><strong>Independent seam, not an override pair.</strong> This mirrors the LOGIC of
 * {@code IRFunctionExpressionRenderer.ifThenElseResultBaseName(RConditionalExpr)} (R3 fallbacks, the JoinPoint
 * scan, the R5 guard) but takes {@code gm} as a method PARAMETER because {@link ControlFlowHandler} is a stateless
 * handler with no {@code expressionCompiler()} accessor — the seam shape mirrors
 * {@code CollectionHandler.thenArgBaseName(RThenExpr, GeneratorModel)}.
 *
 * <h2>Safety</h2>
 * Defensive (R3): on any missing oracle (no {@code GeneratorModel}/workspace), an un-computable fact, an
 * else-bearing conditional ({@code conditionalToBind} throws on an else — the slice-1 else-less scope), or no
 * {@code JoinPoint} Bind, it falls back to {@code super} (the legacy constant) — never crashing a render, always
 * byte-identical. Self-guarding (R5): a base that did NOT equal the legacy constant FAILS LOUDLY, so a future ANF
 * change cannot silently break parity. Same contract as the slice-1/4/5 overrides.
 *
 * <p>Lab-authored Phase-2; not present upstream.
 */
public class IRControlFlowHandler extends ControlFlowHandler {

    /** Stateless AST→IR adapter — supplies the {@link ConditionalHoist} facts the ANF lowering consumes. */
    private final ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();

    /**
     * The number of expression-compiler-reached {@code ifThenElseResult} hoists this handler DROVE from the ANF
     * (slice 6) — the firing-breadth measurement (anti-L-042). Counts the two mint seats (ComparisonResult arm +
     * ctor item-local arm); RULE bodies have no hoist sink, so this was FUNCTION-only until v3.3 seat 3 (D54): a
     * DATA_RULE condition body installs its own hoist session, so the item-local arm - seat-agnostic - can fire
     * there too (a no-else conditional as a call argument, a comparison operand or a constructor field value),
     * byte-identical by the same contract. Read by the FUNCTION byte
     * gate, asserted {@code > 0} for the carrier cell(s) (never an exact equality). Distinct from
     * {@code IRFunctionExpressionRenderer.ifThenElseResultDrivenCount} (the renderer pathed-SET seat, L-061) — a
     * different class/seat, never summed.
     */
    private int ifThenElseResultDrivenCount;

    /**
     * Slice 6: source the expression-compiler-reached {@code ifThenElseResult} hoist's base lexeme from the neutral
     * ANF substrate (rather than the legacy constant). See the class docs for the L-029 layering and the defensive
     * (R3) + self-guarding (R5) contract; byte-identical by construction (the ANF base == the legacy constant).
     * <strong>else-less scope</strong> — an else-bearing conditional falls back to {@code super} (the else-bearing
     * join lowering is a later slice; {@code conditionalToBind} throws on an else).
     */
    @Override
    protected String ifThenElseResultBaseName(RConditionalExpr expr, GeneratorModel gm) {
        final String legacyBase = super.ifThenElseResultBaseName(expr, gm); // == StatementHoistSession.IF_THEN_ELSE_RESULT
        RWorkspace ws = gm == null ? null : gm.workspace();
        if (ws == null) {
            return legacyBase; // R3: no workspace oracle — byte-identical fallback
        }
        ConditionalHoist facts;
        try {
            facts = adapter.adaptConditionalFacts(expr, NodeId.ROOT, ws);
        } catch (RuntimeException e) {
            return legacyBase; // R3: facts not computable — never crash a render
        }
        if (facts.hasElse()) {
            return legacyBase; // slice-1 scope: else-less only (else-bearing lowering is a later slice; conditionalToBind throws on an else)
        }
        Bind resultBind = null;
        for (Bind bind : Normalize.conditionalToBind(facts)) {
            // the ifThenElseResult hoist is the JoinPoint-valued Bind (a boolean-hoist, if any, is a Block-valued
            // Bind registered first — the §6 boolean-before-ifThenElseResult order); identify it structurally.
            if (bind.value() instanceof JoinPoint) {
                resultBind = bind;
                break;
            }
        }
        if (resultBind == null) {
            return legacyBase; // R3: the ANF produced no ifThenElseResult hoist (unexpected) — byte-identical fallback
        }
        String anfBase = resultBind.name().base();
        if (!StatementHoistSession.IF_THEN_ELSE_RESULT.equals(anfBase)) {
            // R5: a silent byte-identity divergence — the ANF naming must reproduce the legacy constant. Fail loudly.
            throw new IllegalStateException("Phase-C slice-6: ANF ifThenElseResult base '" + anfBase
                    + "' != legacy '" + legacyBase + "' — a byte-identity divergence the IR must not introduce");
        }
        ifThenElseResultDrivenCount++;
        return anfBase;
    }

    /**
     * The count of expression-compiler-reached {@code ifThenElseResult} hoists this handler drove from the ANF (the
     * slice-6 firing breadth). Read by the FUNCTION byte gate to assert the seam fired on real corpus rows
     * (anti-L-042).
     */
    public int ifThenElseResultDrivenCount() {
        return ifThenElseResultDrivenCount;
    }
}
