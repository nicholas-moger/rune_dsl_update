package com.regnosys.rosetta.ir.expr.anf;

import com.regnosys.rosetta.ir.expr.IRExpr;

import java.util.List;

/**
 * The normal-form of an expression (design §5, {@code ANFExpr = Block{ List<Binding> lets, Atom result }}):
 * a sequence of hoisted {@link Bind} statements followed by a trivial result {@code Atom}. It is the
 * Northeastern ANF {@code (answer, context)} pair — {@code result} is the answer, {@code lets} the context
 * that must precede it.
 *
 * <h2>The {@code result} is an Atom</h2>
 * {@code result} is an {@link IRExpr} that is <em>trivial</em> — a leaf
 * ({@code Literal}/{@code Variable}/{@code Reference}/{@code EmptyLiteral}) or a compound all of whose
 * operands are themselves atoms (i.e. no nested operand needed hoisting). {@code normalize} guarantees
 * this invariant: a non-atomic sub-expression that is reused OR sits under a hoist barrier is lifted into
 * a {@link Bind} and replaced by a {@code Variable} reference, so the residual {@code result} is always
 * an atom. (The type is {@code IRExpr} rather than a distinct {@code Atom} wrapper to avoid re-wrapping the
 * whole core hierarchy; the triviality is a {@code normalize} post-condition, asserted by its tests.)
 *
 * <h2>Barriers are encoded by nesting, not a flag</h2>
 * Bindings must not float across a hoist barrier (a conditional/switch branch, a lambda body, a
 * short-circuit/optionality boundary — design §5). {@code normalize} enforces this structurally: a
 * barrier's sub-expression normalizes to its own nested {@code Block}, so its {@code lets} stay inside it
 * and cannot be lifted into the enclosing block. No explicit barrier marker is needed on the model.
 *
 * <p>Lab-authored Phase-2; not present upstream.
 *
 * @param lets   the hoisted statements, in render-walk (registration) order — preserved exactly, as it
 *               drives the per-scope temp numbering
 * @param result the trivial atom this block evaluates to (a {@link #lets()}-may-be-empty leaf for a
 *               leaf input: {@code leaf → Block{[], leaf}}). May be {@code null} when this {@code Block} is
 *               the value of a hoisted fluent-initializer binding whose right-hand side is masked in the
 *               Phase-A offline skeleton (e.g. a {@code final Boolean t = <call>;} {@code boolean} hoist —
 *               the call bytes are render-scope-dependent, a "C" obligation; the decl type travels out-of-band)
 */
public record Block(List<Bind> lets, IRExpr result) implements ANFExpr {

    /** Canonicalises to an unmodifiable, defensively-copied binding list. */
    public Block {
        lets = List.copyOf(lets);
    }
}
