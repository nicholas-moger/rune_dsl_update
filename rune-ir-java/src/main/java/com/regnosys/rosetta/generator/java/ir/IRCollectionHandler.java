package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.ast.expressions.binary.RThenExpr;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.expression.handlers.CollectionHandler;
import com.regnosys.rosetta.generator.java.function.StatementHoistSession;
import com.regnosys.rosetta.ir.expr.NodeId;
import com.regnosys.rosetta.ir.expr.adapter.ExpressionToIRAdapter;
import com.regnosys.rosetta.ir.expr.anf.Bind;
import com.regnosys.rosetta.ir.expr.anf.Normalize;
import com.regnosys.rosetta.ir.expr.anf.ThenChainHoist;
import com.regnosys.rosetta.symbols.RWorkspace;

import java.util.List;

/**
 * Path-2 (IR-routed) variant of {@link CollectionHandler} — the Wave-6 Phase-C <em>expression-compiler</em> seam
 * by which the neutral ANF substrate drives the OPERAND-position then-chain {@code thenArg} hoist NAME. That hoist
 * is minted in {@code CollectionHandler.tryDeepThenHoist} (the "SEAM 1" {@code then} that reaches
 * {@code ExpressionCompiler.visitThen} → {@code CollectionHandler.handle}, as opposed to the SET-position
 * {@code renderThenExtractSet} the renderer subclass {@code IRFunctionExpressionRenderer} drives, L-062). The lab's
 * {@code IRExpressionCompiler} supplies this subclass through the {@code ExpressionCompiler.createCollectionHandler()}
 * factory (the expression-compiler "Step 0", L-064).
 *
 * <h2>Slice 4 (L-065): drive the operand-position {@code thenArg} hoist NAME</h2>
 * {@link #thenArgBaseName(RThenExpr, GeneratorModel)} is overridden to SOURCE the hoist's base lexeme from the
 * neutral ANF substrate — it runs {@link ExpressionToIRAdapter#adaptThenChainFacts} +
 * {@link Normalize#thenChainToBinds} (the SAME lowering slice-2 uses for the SET-position {@code thenArg}, reused
 * verbatim — the operand chain has the identical left-associative {@code argument()} spine) and returns the first
 * {@link Bind}'s {@code TempName} base. By the L-029 split, the IR drives only the NEUTRAL fact (the hoist name);
 * the surrounding Java rendering — the chain length, the {@code 0..n-1} numbering, the re-rooting, the decl types
 * and the bodies — stays in the legacy {@code tryDeepThenHoist} oracle, byte-identical. The ANF base equals the
 * legacy constant by construction, so Path-2 stays byte-for-byte identical to Path-1.
 *
 * <h2>Safety</h2>
 * The override is defensive (R3): on any missing oracle (no {@code GeneratorModel}/workspace) or an un-computable
 * fact, it falls back to {@code super} (the legacy constant) — never crashing a render, always byte-identical. It
 * is self-guarding (R5): a base that did NOT equal the legacy constant FAILS LOUDLY, so a future ANF change cannot
 * silently break parity. Same contract as the slice-1/2/3 overrides.
 *
 * <p>Lab-authored Phase-2; not present upstream.
 */
public class IRCollectionHandler extends CollectionHandler {

    /** Stateless AST→IR adapter — supplies the {@link ThenChainHoist} facts the ANF lowering consumes. */
    private final ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();

    /**
     * The number of operand-position then-chains this handler DROVE from the ANF (slice 4, L-065) — the
     * firing-breadth measurement (anti-L-042). Counts chains that REACH the drive point (where the base is computed,
     * once per chain): a SUPERSET of the chains that ultimately mint a {@code thenArg}, since
     * {@code tryDeepThenHoist} has mid-loop multi-statement-block decline paths AFTER the base is taken (so a few
     * chains increment this then fall back inline — byte-inert, the base is unused). Read by the FUNCTION byte gate,
     * asserted {@code > 0} for the carrier cell (never an exact equality). RULE bodies have no hoist sink, so this was
     * FUNCTION-only until v3.3 seat 3 (D54): a DATA_RULE condition body's ladder compiles its then-chain rungs through the
     * compiler with a hoist sink installed, so the drive point is reached there too, byte-identical by the same contract.
     */
    private int operandThenArgDrivenCount;

    /**
     * Slice 4: source the operand-position then-chain {@code thenArg} hoist's base lexeme from the neutral ANF
     * substrate (rather than the legacy constant). See the class docs for the L-029 layering and the defensive
     * (R3) + self-guarding (R5) contract; byte-identical by construction (the ANF base == the legacy constant).
     * <strong>else-/seat-agnostic</strong> and reuses slice-2's {@code thenArg} ANF lowering verbatim.
     */
    @Override
    protected String thenArgBaseName(RThenExpr expr, GeneratorModel gm) {
        final String legacyBase = super.thenArgBaseName(expr, gm); // == StatementHoistSession.THEN_ARG
        RWorkspace ws = gm == null ? null : gm.workspace();
        if (ws == null) {
            return legacyBase; // R3: no workspace oracle — byte-identical fallback
        }
        ThenChainHoist facts;
        try {
            facts = adapter.adaptThenChainFacts(expr, NodeId.ROOT, ws);
        } catch (RuntimeException e) {
            return legacyBase; // R3: facts not computable — never crash a render
        }
        List<Bind> binds = Normalize.thenChainToBinds(facts);
        if (binds.isEmpty()) {
            return legacyBase; // R3: the ANF produced no thenArg hoist (unexpected) — byte-identical fallback
        }
        String anfBase = binds.get(0).name().base();
        if (!StatementHoistSession.THEN_ARG.equals(anfBase)) {
            // R5: a silent byte-identity divergence — the ANF naming must reproduce the legacy constant. Fail loudly.
            throw new IllegalStateException("Phase-C slice-4: ANF operand-then thenArg base '" + anfBase
                    + "' != legacy '" + legacyBase + "' — a byte-identity divergence the IR must not introduce");
        }
        operandThenArgDrivenCount++;
        return anfBase;
    }

    /**
     * The count of operand-position then-chains this handler drove from the ANF (the slice-4 firing breadth; counts
     * chains reaching the drive point, a superset of those that mint — see the field docs). Read by the FUNCTION
     * byte gate to assert the seam fired on real corpus rows (anti-L-042).
     */
    public int operandThenArgDrivenCount() {
        return operandThenArgDrivenCount;
    }
}
