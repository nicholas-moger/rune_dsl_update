package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.expression.handlers.LiteralHandler;
import com.regnosys.rosetta.generator.java.function.StatementHoistSession;
import com.regnosys.rosetta.ir.expr.NodeId;
import com.regnosys.rosetta.ir.expr.adapter.ExpressionToIRAdapter;
import com.regnosys.rosetta.ir.expr.anf.BigIntegerHoist;
import com.regnosys.rosetta.ir.expr.anf.Bind;
import com.regnosys.rosetta.ir.expr.anf.Normalize;
import com.regnosys.rosetta.symbols.RWorkspace;

import java.util.List;

/**
 * Path-2 (IR-routed) variant of {@link LiteralHandler} — the Wave-6 Phase-C <em>expression-compiler</em> seam by
 * which the neutral ANF substrate drives the {@code bigInteger} literal-hoist NAME (the 4th + LAST hoist family,
 * decision-log L-068). That hoist is minted in {@code LiteralHandler.hoistBigIntegerLiteralOrNull} for a
 * beyond-{@code long} integer literal ({@code value.bitLength() > 63}) consumed in a {@code BigDecimal} context with
 * a statement-hoist sink. The lab's {@code IRExpressionCompiler} supplies this subclass through the
 * {@code ExpressionCompiler.createLiteralHandler()} factory (the second expression-compiler "Step 0", after L-064's
 * {@code createCollectionHandler}).
 *
 * <h2>Slice 5 (L-068): drive the {@code bigInteger} hoist NAME</h2>
 * {@link #bigIntegerBaseName(RIntLiteral, GeneratorModel)} is overridden to SOURCE the hoist's base lexeme from the
 * neutral ANF — it runs {@link ExpressionToIRAdapter#adaptBigIntegerFacts} + {@link Normalize#bigIntegerToBind} and
 * returns the {@link Bind}'s {@code TempName} base. By the L-029 split, the IR drives only the NEUTRAL fact (the
 * hoist name); the surrounding Java rendering — the {@code new BigInteger("…")} decl value, the {@code 0..n-1}
 * numbering, the consuming null-guarded ternary — stays in the legacy {@code hoistBigIntegerLiteralOrNull} oracle,
 * byte-identical. The ANF base equals the legacy constant by construction, so Path-2 stays byte-for-byte identical
 * to Path-1.
 *
 * <p><strong>This is the thinnest of the four family drives.</strong> The {@code bigInteger} base is
 * value-independent (always {@code "bigInteger"}), so {@link BigIntegerHoist} is an identity-only fact bundle whose
 * lowering branches on nothing (unlike the boolean/conditional/then records). The drive is still non-vacuous: the
 * override genuinely sources the lexeme through the ANF and self-guards it equals the legacy constant, and
 * {@code adaptBigIntegerFacts} validates the family's beyond-{@code long} precondition. See {@link BigIntegerHoist}.
 *
 * <h2>Safety</h2>
 * The override is defensive (R3): on any missing oracle (no {@code GeneratorModel}/workspace) or an un-computable
 * fact (e.g. the precondition check throws) it falls back to {@code super} (the legacy constant) — never crashing a
 * render, always byte-identical. It is self-guarding (R5): a base that did NOT equal the legacy constant FAILS
 * LOUDLY, so a future ANF change cannot silently break parity. The R5 guard is the SOLE oracle that the parser-side
 * {@code "bigInteger"} lexeme agrees with the generator-side {@link StatementHoistSession#BIG_INTEGER} — the Q3
 * hoist-trace dump is silent on {@code bigInteger}, so there is no offline cross-check. Same contract as the
 * slice-1/2/3/4 overrides.
 *
 * <p>Lab-authored Phase-2; not present upstream.
 */
public class IRLiteralHandler extends LiteralHandler {

    /** Stateless AST→IR adapter — supplies the {@link BigIntegerHoist} facts the ANF lowering consumes. */
    private final ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();

    /**
     * The number of {@code bigInteger} literal hoists this handler DROVE from the ANF (slice 5, L-068) — the
     * firing-breadth measurement (anti-L-042). Incremented at the seam, AFTER the R5 guard passes, only on the
     * statement-hoist (function) path (rules have no sink, so {@code hoistBigIntegerLiteralOrNull} returns before
     * the seam — FUNCTION-only). Read by the FUNCTION byte gate, asserted {@code > 0} for the drr carrier cell
     * (never an exact equality).
     */
    private int bigIntegerDrivenCount;

    /**
     * Slice 5: source the {@code bigInteger} literal-hoist's base lexeme from the neutral ANF substrate (rather than
     * the legacy constant). See the class docs for the L-029 layering and the defensive (R3) + self-guarding (R5)
     * contract; byte-identical by construction (the ANF base == the legacy constant). Reached only from inside
     * {@code hoistBigIntegerLiteralOrNull}, AFTER its sink null-checks, so a sink-less (rule) carrier never
     * increments the count.
     */
    @Override
    protected String bigIntegerBaseName(RIntLiteral expr, GeneratorModel gm) {
        final String legacyBase = super.bigIntegerBaseName(expr, gm); // == StatementHoistSession.BIG_INTEGER
        RWorkspace ws = gm == null ? null : gm.workspace();
        if (ws == null) {
            return legacyBase; // R3: no workspace oracle — byte-identical fallback
        }
        BigIntegerHoist facts;
        try {
            facts = adapter.adaptBigIntegerFacts(expr, NodeId.ROOT, ws);
        } catch (RuntimeException e) {
            return legacyBase; // R3: facts not computable (e.g. not beyond-long) — never crash a render
        }
        List<Bind> binds = Normalize.bigIntegerToBind(facts);
        if (binds.isEmpty()) {
            return legacyBase; // R3: the ANF produced no bigInteger hoist (unexpected) — byte-identical fallback
        }
        String anfBase = binds.get(0).name().base();
        if (!StatementHoistSession.BIG_INTEGER.equals(anfBase)) {
            // R5: a silent byte-identity divergence — the ANF naming must reproduce the legacy constant. Fail loudly.
            throw new IllegalStateException("Phase-C slice-5: ANF bigInteger base '" + anfBase
                    + "' != legacy '" + legacyBase + "' — a byte-identity divergence the IR must not introduce");
        }
        bigIntegerDrivenCount++;
        return anfBase;
    }

    /**
     * The count of {@code bigInteger} literal hoists this handler drove from the ANF (the slice-5 firing breadth).
     * Read by the FUNCTION byte gate to assert the seam fired on real corpus rows (anti-L-042) — and, decisively,
     * that the {@code createLiteralHandler} factory actually installed this subclass (else the count is 0 while the
     * gate stays green via the legacy fallback).
     */
    public int bigIntegerDrivenCount() {
        return bigIntegerDrivenCount;
    }
}
