package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.expressions.binary.RThenExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.function.FunctionExpressionRenderer;
import com.regnosys.rosetta.generator.java.function.StatementHoistSession;
import com.regnosys.rosetta.ir.expr.IRExpr;
import com.regnosys.rosetta.ir.expr.NodeId;
import com.regnosys.rosetta.ir.expr.adapter.ExpressionToIRAdapter;
import com.regnosys.rosetta.ir.expr.anf.Bind;
import com.regnosys.rosetta.ir.expr.anf.Block;
import com.regnosys.rosetta.ir.expr.anf.BooleanConditionHoist;
import com.regnosys.rosetta.ir.expr.anf.ConditionalHoist;
import com.regnosys.rosetta.ir.expr.anf.JoinPoint;
import com.regnosys.rosetta.ir.expr.anf.Normalize;
import com.regnosys.rosetta.ir.expr.anf.ThenChainHoist;
import com.regnosys.rosetta.symbols.RWorkspace;

import java.util.List;
import java.util.Optional;

/**
 * Path-2 (IR-routed) variant of {@link FunctionExpressionRenderer} — the Wave-6 Phase C seam by which the neutral
 * ANF substrate drives the SET-position statement hoists. Those hoists are intercepted in
 * {@code FunctionExpressionRenderer.renderOperationInner} <em>before</em> the expression compiler, so the renderer
 * is the ONLY seam that can reach them live (the lab does not otherwise subclass it — decision-log L-053/L-055; the
 * open obligation "C", L-056). {@code IRFunctionGenerator} supplies this subclass through the
 * {@code FunctionGenerator.createFunctionExpressionRenderer} factory.
 *
 * <h2>Slice 1 (L-061): drive the pathed-SET conditional {@code ifThenElseResult} hoist NAME</h2>
 * {@link #ifThenElseResultBaseName(RConditionalExpr)} is overridden to SOURCE the hoist's base lexeme from the
 * neutral ANF substrate — it runs {@link ExpressionToIRAdapter#adaptConditionalFacts} +
 * {@link Normalize#conditionalToBind} (the same lowering Phase A validated offline against the Q3 dump) and returns
 * the {@code ifThenElseResult} {@link Bind}'s {@code TempName} base. By the L-029 split, the IR drives only the
 * NEUTRAL fact (the hoist name); the surrounding Java rendering — the decl type (from the segment leaf attribute),
 * the declare-then-assign block ({@code ControlFlowHandler.buildIfThenElseHoistBlock}), the setter consumer — stays
 * in the legacy oracle, byte-identical. The ANF base equals the legacy constant by construction, so Path-2 stays
 * byte-for-byte identical to Path-1 (corpus-byte-validated, not a new naming behaviour); what slice 1 proves is that
 * the neutral substrate genuinely runs LIVE and reproduces the legacy hoist naming across the whole corpus — the
 * foundation the later slices (the §6 {@code boolean}-before-{@code ifThenElseResult} reorder, the then-chain
 * re-rooting) build on, where the ANF's ORDERING is load-bearing.
 *
 * <h2>Safety</h2>
 * The override is defensive (R3): on any missing oracle (no {@code GeneratorModel}/workspace), an {@code else}-
 * bearing conditional (a later slice), or an un-computable fact, it falls back to {@code super} (the legacy
 * constant) — never crashing a render, always byte-identical. It is also self-guarding (R5): if the ANF ever
 * produced a base that did NOT equal the legacy constant it would FAIL LOUDLY (a silent byte-identity divergence),
 * so a future ANF change cannot quietly break parity.
 *
 * <p>Lab-authored Phase-2; not present upstream.
 */
public class IRFunctionExpressionRenderer extends FunctionExpressionRenderer {

    /** Stateless AST→IR adapter — supplies the {@link ConditionalHoist} facts the ANF lowering consumes. */
    private final ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();

    /**
     * The number of pathed-SET conditional {@code ifThenElseResult} hoists this renderer DROVE from the ANF — the
     * firing-breadth measurement (the anti-L-042 discipline: confirm the seam fires on real corpus rows, not a
     * 0-row vacuous pass). Accumulated across every function rendered by this generator instance; read by the
     * FUNCTION byte gate.
     */
    private int ifThenElseResultDrivenCount;

    /**
     * The number of SET-position then-chains this renderer DROVE from the ANF (slice 2, L-062) — the firing-breadth
     * measurement (anti-L-042). Accumulated across every function rendered by this generator instance; read by the
     * FUNCTION byte gate.
     */
    private int thenArgDrivenCount;

    /**
     * The number of renderer-direct {@code boolean} condition hoists this renderer DROVE from the ANF (slice 3,
     * L-063) — the firing-breadth measurement (anti-L-042) for the 3rd hoist family. Accumulated across every
     * function rendered by this generator instance; read by the FUNCTION byte gate. Counts INVOCATIONS (a function
     * with several bare-fn-condition hoists — nested-else / multi-rung — increments more than once), so it may
     * exceed the distinct-function carrier count.
     */
    private int booleanHoistDrivenCount;

    /**
     * The number of trivial whole-output SET assignment statements this renderer confirmed as a no-hoist ANF
     * {@code Block} from a LIVE {@code normalize} (slice-0, the foundation of the SET-position structure drive).
     * A <strong>§4.2-NEUTRAL firing WITNESS</strong> (statement-level — it is NOT an IR-driven-share contribution;
     * the slice drives no bytes, it only confirms the no-hoist single-assignment shape): it proves the neutral
     * {@code Normalize} runs correctly on real corpus SET expressions. The global driven&gt;0 gate cannot pin THIS
     * seam, so the FUNCTION byte gate asserts it fires on every cell (anti-L-042). Accumulated across every
     * function rendered by this generator instance.
     */
    private int setAssignmentDrivenCount;

    /**
     * Construct the IR-routed renderer over the IR-aware compiler.
     *
     * @param compiler the (IR-aware) expression compiler this renderer drives body expressions through
     */
    public IRFunctionExpressionRenderer(ExpressionCompiler compiler) {
        super(compiler);
    }

    /**
     * Slice 1: source the pathed-SET {@code ifThenElseResult} hoist's base lexeme from the neutral ANF substrate
     * (rather than the legacy constant) — the IR genuinely driving the SET-position hoist NAME. See the class docs
     * for the L-029 layering (the IR drives the neutral name; the oracle renders the Java block) and the
     * defensive/self-guarding contract. Byte-identical by construction (the ANF base == the legacy constant).
     */
    @Override
    protected String ifThenElseResultBaseName(RConditionalExpr conditional) {
        final String legacyBase = super.ifThenElseResultBaseName(conditional);
        GeneratorModel gm = expressionCompiler().getGeneratorModel();
        RWorkspace ws = gm == null ? null : gm.workspace();
        if (ws == null) {
            return legacyBase; // R3: no workspace oracle — byte-identical fallback
        }
        ConditionalHoist facts;
        try {
            facts = adapter.adaptConditionalFacts(conditional, NodeId.ROOT, ws);
        } catch (RuntimeException e) {
            return legacyBase; // R3: facts not computable — byte-identical fallback (never crash a render)
        }
        if (facts.hasElse()) {
            return legacyBase; // slice-1 scope: else-less only (the else-bearing join lowering is a later slice)
        }
        Bind resultBind = null;
        for (Bind bind : Normalize.conditionalToBind(facts)) {
            // the ifThenElseResult hoist is the JoinPoint-valued Bind (a boolean-hoist, if any, is a Block-valued
            // Bind registered first) — identify it structurally, then guard its base.
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
            // R5: a silent byte-identity divergence — the ANF naming must reproduce the legacy constant. Fail loudly
            // so a future ANF change cannot quietly break parity (the byte gate would otherwise catch it, but late).
            throw new IllegalStateException("Phase-C slice-1: ANF ifThenElseResult base '" + anfBase
                    + "' != legacy '" + legacyBase + "' — a byte-identity divergence the IR must not introduce");
        }
        ifThenElseResultDrivenCount++;
        return anfBase;
    }

    /**
     * Slice 2: source the SET-position then-chain {@code thenArg} hoist's base lexeme from the neutral ANF
     * substrate (rather than the legacy constant) — the IR genuinely driving the {@code thenArg} hoist NAME. The
     * legacy oracle renders the chain length, the {@code +k} numbering, the re-rooting, the decl types and the
     * bodies (the L-029 split). Byte-identical by construction (the ANF base == the legacy constant); same
     * defensive (R3) + self-guarding (R5) contract as {@link #ifThenElseResultBaseName}.
     */
    @Override
    protected String thenArgBaseName(RThenExpr then) {
        final String legacyBase = super.thenArgBaseName(then);
        GeneratorModel gm = expressionCompiler().getGeneratorModel();
        RWorkspace ws = gm == null ? null : gm.workspace();
        if (ws == null) {
            return legacyBase; // R3: no workspace oracle — byte-identical fallback
        }
        ThenChainHoist facts;
        try {
            facts = adapter.adaptThenChainFacts(then, NodeId.ROOT, ws);
        } catch (RuntimeException e) {
            return legacyBase; // R3: facts not computable — byte-identical fallback (never crash a render)
        }
        List<Bind> binds = Normalize.thenChainToBinds(facts);
        if (binds.isEmpty()) {
            return legacyBase; // R3: the ANF produced no thenArg hoist (unexpected) — byte-identical fallback
        }
        String anfBase = binds.get(0).name().base();
        if (!StatementHoistSession.THEN_ARG.equals(anfBase)) {
            // R5: a silent byte-identity divergence — the ANF naming must reproduce the legacy constant. Fail loudly.
            throw new IllegalStateException("Phase-C slice-2: ANF thenArg base '" + anfBase + "' != legacy '"
                    + legacyBase + "' — a byte-identity divergence the IR must not introduce");
        }
        thenArgDrivenCount++;
        return anfBase;
    }

    /**
     * Slice 3 (L-063): source the renderer-direct {@code boolean} condition hoist's base lexeme from the neutral
     * ANF substrate (rather than the legacy constant) — the IR genuinely driving the 3rd hoist family's NAME. Called
     * at the two renderer-direct seats ({@code appendReturnLadder}, {@code renderConditionalAssignment}) where a
     * bare-function-call condition hoists a {@code final Boolean} guard before an INLINE {@code if/else} (no
     * {@code ifThenElseResult}). The oracle renders the {@code final Boolean} decl, the {@code (id == null ? false :
     * id)} guard, the {@code _boolean} keyword escape and the per-group numbering (the L-029 split). Byte-identical
     * by construction (the ANF base == the legacy constant).
     *
     * <p>Unlike {@link #ifThenElseResultBaseName} this is <strong>else-agnostic</strong>: the {@code boolean} hoist
     * depends only on the condition shape (it fires on else-bearing SET-conditionals too), and
     * {@link Normalize#booleanConditionToBind} is throw-free (no {@code else} gate), so the override needs no
     * {@code hasElse} guard. Same defensive (R3 fallback to {@code super}) + self-guarding (R5: ANF base ≠ the
     * constant fails loudly) contract as the slice-1/2 overrides.
     */
    @Override
    protected String booleanHoistBaseName(RConditionalExpr conditional) {
        final String legacyBase = super.booleanHoistBaseName(conditional);
        GeneratorModel gm = expressionCompiler().getGeneratorModel();
        RWorkspace ws = gm == null ? null : gm.workspace();
        if (ws == null) {
            return legacyBase; // R3: no workspace oracle — byte-identical fallback
        }
        BooleanConditionHoist facts;
        try {
            facts = adapter.adaptBooleanConditionFacts(conditional, NodeId.ROOT, ws);
        } catch (RuntimeException e) {
            return legacyBase; // R3: facts not computable (e.g. a type-inference throw) — never crash a render
        }
        List<Bind> binds = Normalize.booleanConditionToBind(facts);
        if (binds.isEmpty()) {
            return legacyBase; // R3: the ANF found no boolean hoist (unexpected at this seat) — byte-identical fallback
        }
        String anfBase = binds.get(0).name().base();
        if (!StatementHoistSession.BOOLEAN.equals(anfBase)) {
            // R5: a silent byte-identity divergence — the ANF naming must reproduce the legacy constant. Fail loudly.
            throw new IllegalStateException("Phase-C slice-3: ANF boolean base '" + anfBase + "' != legacy '"
                    + legacyBase + "' — a byte-identity divergence the IR must not introduce");
        }
        booleanHoistDrivenCount++;
        return anfBase;
    }

    /**
     * Slice-0 (the live ANF→Java SET-statement seam, the foundation of the SET-position structure drive): for a
     * fully-IR-lowerable, no-embedded-hoist whole-output SET, run a LIVE {@code adapt}+{@code normalize} on the
     * expression's core IR and confirm it is a no-hoist ANF {@link Block} — the IR sourcing the
     * "no-hoist ⇒ single bare assignment" structural fact from the neutral substrate.
     *
     * <p><strong>Drives NO bytes:</strong> it returns the byte-identical legacy assignment on every path; its sole
     * effect is the {@link #setAssignmentDrivenCount} firing witness, which proves the neutral {@code Normalize}
     * runs correctly on real corpus SET expressions (the Java {@code setValue} bytes stay emitter-side — the
     * L-029 split; the genuine structural drive, emitting decls from a multi-{@code Bind} Block, is the then-chain
     * slice that extends this pattern). <strong>Fail-soft throughout:</strong> any missing oracle, non-lowerable
     * expression, nested statement hoist, or hoisting ANF declines to legacy, byte-identical, never crashing a
     * render. (The hoisting-ANF decline was BORN provably-unreachable — the pre-step-12 shared dispatch produced
     * neither a root {@code IRConditional} nor {@code Let} — and is LIVE since step-12 wired
     * {@code adaptConditional} into the dispatch: a root-conditional SET adapts, normalizes to a hoisting ANF and
     * takes this decline; the #495 conditional wave's stale-comment catch, and the ctor-slot admission widens the
     * adapting population further.) No R5 throw: every output here is already byte-identical to legacy, so a
     * hoisting surprise is byte-safe and must decline, not crash.
     */
    @Override
    protected String renderSetAssignmentStatement(String targetName, String setValue, RExpression expression,
            boolean noEmbeddedHoists) {
        final String legacy = super.renderSetAssignmentStatement(targetName, setValue, expression, noEmbeddedHoists);
        if (!noEmbeddedHoists) {
            // a nested conditional/bigInteger registered a statement hoist (prepended separately by
            // prependStatementHoists) — the ANF models only top-level hoists, so it must not claim this
            // multi-statement SET as a trivial single assignment
            return legacy;
        }
        GeneratorModel gm = expressionCompiler().getGeneratorModel();
        RWorkspace ws = gm == null ? null : gm.workspace();
        if (ws == null) {
            return legacy; // R3: no workspace oracle — byte-identical fallback
        }
        Optional<IRExpr> ir;
        try {
            ir = adapter.adapt(expression, ws);
        } catch (RuntimeException e) {
            return legacy; // R3: adapt threw — never crash a render
        }
        if (ir.isEmpty()) {
            return legacy; // the expression is not fully IR-lowerable → the IR cannot own this statement
        }
        if (!(Normalize.normalize(ir.get()) instanceof Block block) || !block.lets().isEmpty()) {
            // A no-hoist Block is the only shape the DOMINANT fully-lowered SET expression normalizes to at
            // this seam. The lets-bearing branch is LIVE, not provably-unreachable (the method javadoc's
            // step-12/#495 note): a root-conditional SET that escapes the earlier renderOperationInner
            // interception adapts through the shared dispatch since step-12 — and the #495 ctor-slot
            // admission widens that population — normalizing to a hoisting ANF that lands here. Decline
            // softly rather than throw: the output is already byte-identical to legacy on every path.
            return legacy;
        }
        setAssignmentDrivenCount++;
        return legacy; // byte-identical; the live ANF confirmed the no-hoist single-assignment shape
    }

    /**
     * The count of pathed-SET {@code ifThenElseResult} hoists this renderer drove from the ANF (the firing
     * breadth). Read by the FUNCTION byte gate to assert the seam fired on real corpus rows (anti-L-042).
     */
    public int ifThenElseResultDrivenCount() {
        return ifThenElseResultDrivenCount;
    }

    /**
     * The count of SET-position then-chains this renderer drove from the ANF (the slice-2 firing breadth). Read by
     * the FUNCTION byte gate to assert the seam fired on real corpus rows (anti-L-042).
     */
    public int thenArgDrivenCount() {
        return thenArgDrivenCount;
    }

    /**
     * The count of renderer-direct {@code boolean} condition hoists this renderer drove from the ANF (the slice-3
     * firing breadth, INVOCATION-based). Read by the FUNCTION byte gate to assert the seam fired on real corpus
     * rows (anti-L-042).
     */
    public int booleanHoistDrivenCount() {
        return booleanHoistDrivenCount;
    }

    /**
     * The count of trivial whole-output SET assignments this renderer confirmed as a no-hoist ANF Block from a
     * live {@code normalize} (slice-0). A §4.2-NEUTRAL firing witness (the slice drives no bytes — see the
     * {@link #renderSetAssignmentStatement} docs). Read by the FUNCTION byte gate to assert the live
     * {@code Normalize} seam ran on real corpus SET rows (anti-L-042).
     */
    public int setAssignmentDrivenCount() {
        return setAssignmentDrivenCount;
    }
}
