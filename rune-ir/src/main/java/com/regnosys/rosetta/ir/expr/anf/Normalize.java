package com.regnosys.rosetta.ir.expr.anf;

import com.regnosys.rosetta.ir.expr.IRApply;
import com.regnosys.rosetta.ir.expr.IRConditional;
import com.regnosys.rosetta.ir.expr.IRExpr;
import com.regnosys.rosetta.ir.expr.Let;
import com.regnosys.rosetta.ir.expr.NodeId;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code normalize : IRExpr(core) → ANFExpr} (design §5) — the <strong>Java-target</strong> lowering of
 * the neutral core IR into A-Normal Form, where every hoisted sub-expression is named by a let-binding.
 * It is one total nanopass (the Northeastern {@code (answer, context)} contract): a {@link Block} of the
 * bindings that must precede the trivial result atom. A Python/Rust target would define its own
 * {@code normalize} over the same core IR.
 *
 * <h2>Phase-A scope (slices 1–3)</h2>
 * The clauses land additively, one make-or-break hoist family per slice:
 * <ul>
 *   <li><strong>leaf / trivial</strong> → {@code Block{[], self}} (no hoist);</li>
 *   <li><strong>statement-position {@link IRConditional}, {@code else}-less</strong> (slice 1/2) → an
 *       {@code ifThenElseResult} hoist (with a {@code boolean} hoist before it for a bare-fn-call condition,
 *       dump §6): {@code Block{conditionalToBind(facts), <residual>}};</li>
 *   <li><strong>a {@link Let} chain</strong> (slice 3, the desugared {@code then}-pipe) → the {@code thenArg}
 *       hoists, one per {@code Let} in the flattened chain (the chain base is {@code thenArg0}, the outermost
 *       {@code in} is the residual): {@code Block{thenChainToBinds(facts), <residual>}}.</li>
 * </ul>
 * Each hoist lowering is factored through a shared {@code *ToBind(s)} so the offline harness (which has only the
 * structural facts — the L-032/under-resolution branch interiors are un-lowerable at the #219 pin) and the live
 * "C" path (which has the fully-childed node) share one lowering: {@link #conditionalToBind(ConditionalHoist)}
 * for a conditional, {@link #thenChainToBinds(ThenChainHoist)} for a then-chain. An {@code else}-bearing
 * conditional, an operand-position conditional/switch (a {@link JoinPoint}), {@code switch}, and the {@code then}
 * V4 capture-avoiding substitution (which shapes the masked bodies, a "C" concern) are NOT handled here — they
 * fall through to the leaf clause or are the subject of later slices / "C".
 *
 * <p><strong>This tier is OFFLINE-VALIDATED ONLY — the live wiring ("C") is the mandatory open
 * obligation</strong> (decision-log L-056; see the {@code ir.expr.anf} package docs).
 *
 * <p>Lab-authored Phase-2; not present upstream.
 */
public final class Normalize {

    private Normalize() {
    }

    /**
     * Normalize a core {@link IRExpr} to its {@link ANFExpr} form (the Java-target lowering). See the
     * class docs for the Phase-A slice-1 clause set.
     */
    public static ANFExpr normalize(IRExpr expr) {
        if (expr instanceof IRConditional conditional && conditional.elseBranch() == null) {
            List<Bind> hoists = conditionalToBind(factsOf(conditional));
            // The hoisted conditional reduces to a reference to its temporary; slice 1 carries the
            // pre-hoist then-branch as the residual result placeholder (proper temp-reference
            // result-threading — an ANF temp-reference atom — lands with the consumer render at the
            // live "C" stage, where the SET statement that reads the temp is emitted).
            return new Block(hoists, conditional.thenBranch());
        }
        if (expr instanceof Let let) {
            // A `then`-chain desugars to a (right-nested) chain of Lets; flatten it into the thenArg hoists,
            // one per Let, in render-walk order (thenArg0 = the chain base). The residual is the innermost
            // `in` — the chain's final value, which the SET statement reads (slice 3 carries it as the
            // residual placeholder; the thenArg-value bytes are masked, a "C" obligation, exactly as the
            // slice-1 conditional masks its JoinPoint branching).
            return new Block(thenChainToBinds(factsOf(let)), residualOf(let));
        }
        // Leaf / not-yet-handled-here: a trivial expression normalizes to itself with no hoist.
        return new Block(List.of(), expr);
    }

    /**
     * The three {@link ConditionalHoist} facts of a fully-childed {@link IRConditional} — the bridge that
     * lets {@link #normalize} and the offline harness share {@link #conditionalToBind}. {@code resultType}
     * is the conditional's inferred type, which for an {@code else}-less conditional is the then-branch's
     * type (the {@code ifThenElseResult} local's declared type).
     */
    public static ConditionalHoist factsOf(IRConditional conditional) {
        // The condition hoists a `final Boolean` temp iff it is a bare function call (an IRApply on the lowered
        // IR — the analogue of the adapter's AST-side RSymbolReference→RFunction test, which is authoritative
        // for the offline harness); a comparison/equality/existence condition stays inline.
        IRExpr condition = conditional.condition();
        boolean conditionHoists = condition instanceof IRApply;
        return new ConditionalHoist(
                conditional.nodeId(),
                conditional.type(),
                conditional.elseBranch() != null,
                condition.nodeId(),
                conditionHoists,
                condition.type());
    }

    /**
     * Lower a statement-position conditional's {@link ConditionalHoist} facts to its {@code ifThenElseResult}
     * {@link Bind} — a {@link JoinPoint} (the declare-then-assign form: {@code <Type> t = null;
     * if(<cond>){t = <then>;}}, since Java has no expression-form {@code if}). The {@link TempName} carries
     * the base lexeme {@code "ifThenElseResult"} and the conditional's {@link com.regnosys.rosetta.ir.expr.NodeId}
     * key; its numeric suffix is assigned by the per-scope render-walk replay (the ANF→Java renderer), not
     * here. The {@code JoinPoint}'s {@code branching} is {@code null} in the offline skeleton (the branch
     * bodies are masked, rendered at "C"); the declared type travels out-of-band as a
     * {@link ConditionalHoist#resultType()} fact.
     *
     * <p>Returns one or two bindings in <strong>render-walk order</strong>: when the condition is a bare
     * function call ({@link ConditionalHoist#hoistedCondition()} present) it hoists a {@code boolean} temp
     * FIRST — {@code Bind{ "boolean", Block{[], <call>} }}, the {@code final Boolean t = <call>;} initializer
     * form — then the {@code ifThenElseResult} {@link JoinPoint}; otherwise just the {@code ifThenElseResult}
     * (the condition renders inline). The {@code boolean}-before-{@code ifThenElseResult} order is the §5 / dump
     * §6 render-walk reorder (a hoisted condition decl renders before the result it guards).
     *
     * @throws IllegalArgumentException if the conditional has an {@code else} branch (not handled in
     *                                  Phase-A slice 1/2 — the {@code else}-bearing lowering is a later slice)
     */
    public static List<Bind> conditionalToBind(ConditionalHoist facts) {
        if (facts.hasElse()) {
            throw new IllegalArgumentException(
                    "else-bearing conditional hoist is not handled in Phase-A slice 1/2: " + facts.key());
        }
        List<Bind> hoists = new ArrayList<>(2);
        if (facts.conditionHoists()) {
            hoists.add(booleanHoistBind(facts.conditionKey()));
        }
        TempName resultTemp = new TempName("ifThenElseResult", facts.key());
        hoists.add(new Bind(resultTemp, new JoinPoint(resultTemp, null)));
        return hoists;
    }

    /**
     * The {@code boolean} condition-hoist {@link Bind} — a fluent-initializer {@code final Boolean t = <call>;}
     * modelled as a Block-value {@code Bind} with a masked ({@code null}) value in the offline skeleton (the decl
     * {@code Boolean} type travels out-of-band). The {@link TempName} base lexeme is {@code "boolean"}; the
     * keyword escape ({@code _boolean}) and per-group numbering are the render-walk replay's decision, not here.
     * Shared by {@link #conditionalToBind} (the {@code boolean}-before-{@code ifThenElseResult} reorder) and
     * {@link #booleanConditionToBind} (the renderer-direct seat, no {@code ifThenElseResult}) so the two paths
     * cannot disagree on the base lexeme.
     */
    private static Bind booleanHoistBind(NodeId conditionKey) {
        return new Bind(new TempName("boolean", conditionKey), new Block(List.of(), null));
    }

    /**
     * Lower a renderer-direct {@link BooleanConditionHoist}'s facts to JUST its {@code boolean} condition hoist
     * {@link Bind} — the {@code final Boolean t = <call>;} initializer the renderer emits BEFORE an inline-rendered
     * conditional whose condition is a bare function call (the whole-output SET-conditional and alias return-ladder
     * seats, where the conditional does NOT hoist an {@code ifThenElseResult} — it renders as an {@code if/else}
     * assignment guarded by the hoisted boolean). Returns an empty list when the condition does not hoist (a
     * comparison/equality/existence condition renders inline).
     *
     * <p>Distinct from {@link #conditionalToBind} (which ALSO emits the {@code ifThenElseResult} {@link JoinPoint}):
     * this seat has NO {@code ifThenElseResult}, so fabricating one would be dishonest. <strong>THROW-FREE</strong>
     * (returns {@code List.of()} when {@code !conditionHoists}, never throws on {@code else}-presence) so the live
     * "C" renderer override can call it outside its {@code try} and preserve the never-crash-a-render fallback.
     *
     * <p>Shared by the live Wave-6 Phase-C renderer ({@code IRFunctionExpressionRenderer.booleanHoistBaseName},
     * which sources the renderer-direct {@code boolean} hoist NAME from the resulting Bind); the {@code boolean}
     * base lexeme comes from the shared {@link #booleanHoistBind}.
     */
    public static List<Bind> booleanConditionToBind(BooleanConditionHoist facts) {
        return facts.conditionHoists() ? List.of(booleanHoistBind(facts.conditionKey())) : List.of();
    }

    /**
     * The {@link ThenChainHoist} facts of a desugared {@code then}-chain — the ordered binder keys and declared
     * types of the {@code thenArg} hoists — read off a flattened {@link Let} chain. It is the node→facts bridge
     * that lets {@link #normalize} and the offline harness share {@link #thenChainToBinds} (mirrors
     * {@link #factsOf(IRConditional)}).
     *
     * <p>Descends the {@code in}-spine ({@code let.in()} while it is another {@code Let}) in render-walk order:
     * the outermost {@code Let} is {@code thenArg0} (the chain base), then {@code thenArg1, …}; the first
     * non-{@code Let} {@code in} is the residual (NOT a binder — see {@link #residualOf(Let)}). Each binder's key
     * is its {@code Let}'s {@link com.regnosys.rosetta.ir.expr.NodeId} — a {@code then}-{@code Let} has exactly
     * one binder, so the node's id keys its temporary (V3's {@code (lambdaNodeId, paramIndex)} is only for
     * multi-binder lambdas). The declared types are the {@code Let}s' inferred types.
     */
    public static ThenChainHoist factsOf(Let let) {
        List<NodeId> binderKeys = new ArrayList<>();
        List<RMetaAnnotatedType> binderTypes = new ArrayList<>();
        IRExpr cur = let;
        while (cur instanceof Let l) {
            binderKeys.add(l.nodeId());
            binderTypes.add(l.type());
            cur = l.in();
        }
        return new ThenChainHoist(binderKeys, binderTypes);
    }

    /**
     * The residual result of a flattened {@link Let} chain — the innermost {@code in} (the chain's final value,
     * which the consumer reads). Descends {@code let.in()} while it is another {@code Let}; the first
     * non-{@code Let} {@code in} is the residual. (For a {@code then}-chain this is the outermost {@code then}'s
     * body re-rooted onto the last {@code thenArg}; the residual bytes are a "C" concern, this returns the
     * un-numbered atom placeholder.)
     */
    private static IRExpr residualOf(Let let) {
        IRExpr cur = let;
        while (cur instanceof Let l) {
            cur = l.in();
        }
        return cur;
    }

    /**
     * Lower a {@code then}-chain's {@link ThenChainHoist} facts to its {@code thenArg} {@link Bind} sequence —
     * one {@link Bind} per binder, in render-walk (list) order, each a fluent initializer
     * ({@code final <Type> thenArgN = <value>;}). The {@link TempName} carries the base lexeme {@code "thenArg"}
     * and the binder's {@link com.regnosys.rosetta.ir.expr.NodeId} key; its numeric suffix (bare for a single
     * member, {@code 0..n−1} for a group) is assigned by the per-scope render-walk replay (the ANF→Java
     * renderer), not here. The {@code Bind}'s value is a {@link Block} (the initializer form, distinct from a
     * conditional's declare-then-assign {@link JoinPoint}) whose result is {@code null} in the offline skeleton —
     * the {@code thenArg} value bytes (the pre-{@code then}/re-rooted chain) are render-scope-dependent, a "C"
     * obligation; the decl type travels out-of-band as a {@link ThenChainHoist#binderTypes()} fact.
     *
     * <p>Shared by {@link #normalize(IRExpr)} (via {@link #factsOf(Let)}, on a real {@code Let} chain) and the
     * offline harness (via {@code ExpressionToIRAdapter.adaptThenChainForHarness}, on the structural AST walk) —
     * one lowering impl, so the two desugar paths cannot disagree on the {@code thenArg} sequence.
     */
    public static List<Bind> thenChainToBinds(ThenChainHoist facts) {
        List<Bind> hoists = new ArrayList<>(facts.binderKeys().size());
        for (NodeId key : facts.binderKeys()) {
            // a fluent-initializer `final <Type> thenArgN = <value>;` hoist — a Block-value Bind with a masked
            // (null) value in the offline skeleton; the decl type travels out-of-band (binderTypes).
            hoists.add(new Bind(new TempName("thenArg", key), new Block(List.of(), null)));
        }
        return hoists;
    }

    /**
     * Lower a {@link BigIntegerHoist}'s facts to its single {@code bigInteger} {@link Bind} — a fluent initializer
     * ({@code final BigInteger bigInteger = new BigInteger("…");}) modelled as a {@link Block}-value {@link Bind}
     * with a masked ({@code null}) value (the literal-value bytes + the consuming null-guarded ternary are the
     * legacy oracle's, the L-029 name-driving split). The {@link TempName} base lexeme is {@code "bigInteger"}: the
     * <em>value-independent</em> constant — so this lowering, alone among the {@code *ToBind(s)} family, branches on
     * NOTHING (the {@link BigIntegerHoist} record is identity-only; see its docs). The bare-vs-{@code 0..n-1}
     * numbering is the per-scope render-walk replay's decision, not here.
     *
     * <p>Sourced by the live Wave-6 Phase-C handler ({@code IRLiteralHandler.bigIntegerBaseName}, which reads this
     * {@code Bind}'s base, slice-5, L-068); the {@code "bigInteger"} lexeme equals the generator-side
     * {@code StatementHoistSession.BIG_INTEGER} by construction, which the handler's R5 guard asserts.
     */
    public static List<Bind> bigIntegerToBind(BigIntegerHoist facts) {
        return List.of(new Bind(new TempName("bigInteger", facts.key()), new Block(List.of(), null)));
    }
}
