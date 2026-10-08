package com.regnosys.rosetta.ir.expr.anf;

import com.regnosys.rosetta.ir.expr.NodeId;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * The neutral <em>facts</em> of a {@code then}-chain's {@code thenArg} hoists — exactly what {@code normalize}
 * needs to produce the {@code thenArg} {@link Bind} sequence, with no dependence on the chain's body interiors
 * being lowered. It is a fact bundle, deliberately <strong>not</strong> an
 * {@link com.regnosys.rosetta.ir.expr.IRExpr} and not a member of any emitter-dispatch enum (mirrors
 * {@link ConditionalHoist}).
 *
 * <p><strong>One entry per {@code thenArg}.</strong> A chain {@code e0 then f1 then … then fN} of length
 * {@code N} (= {@code N} {@code RThenExpr} nodes) hoists {@code N} {@code thenArg}s: {@code thenArg0} = the
 * chain base (the innermost {@code then}'s argument, hoisted even when a trivial atom — confirmed against the
 * live generator {@code FunctionExpressionRenderer.renderThenExtractSet}), {@code thenArg_k} = the k-th
 * {@code then}'s body re-rooted onto {@code thenArg_{k−1}} (k=1..N−1); the <em>outermost</em> {@code then}'s
 * body is the residual consumer, NOT a hoist. {@code binderKeys.size()} = {@code N}.
 *
 * <p><strong>Why a facts bundle and not the full {@link com.regnosys.rosetta.ir.expr.Let} chain.</strong> At the
 * vendored #219 pin the corpus then-chain bodies are {@code item}/alias-receiver constructs whose type is
 * {@code MISSING} (L-032) and which do not lower to IR, so a fully-childed {@code Let} chain is unbuildable
 * offline. The offline harness validates the hoist <em>skeleton</em> (the {@code thenArg} count, the
 * single→bare vs multi→{@code 0..N−1} numbering, the {@code final <T> thenArgK = <value>;} initializer form),
 * which needs only these keys. The live "C" path builds a real {@code Let} chain (bodies lowered, the V4
 * substitution applied) and {@code normalize} reads the binder keys off it via {@link Normalize#factsOf}, so
 * the {@link Normalize#thenChainToBinds} lowering is shared.
 *
 * <p>Lab-authored Phase-2; not present upstream.
 *
 * @param binderKeys  the {@link NodeId} of each hoisted {@code thenArg}, in render-walk (registration) order
 *                    ({@code thenArg0} first). The numeric suffix comes from the per-scope render-walk replay
 *                    (list position), NOT from these keys — they are the temp→node map (a Decorations / "C"
 *                    concern), so any distinct, ordered keys reproduce the offline numbering.
 * @param binderTypes the declared local type of each {@code thenArg}, positionally aligned with
 *                    {@code binderKeys}. All {@link RMetaAnnotatedType#MISSING} offline at the #219 pin (the
 *                    {@code MapperC<…>}/{@code MapperS<…>} decl types are render decisions the live generator
 *                    derives via its own oracles — a "C" cross-check, L-057/L-058); carried for the shared "C"
 *                    lowering.
 */
public record ThenChainHoist(List<NodeId> binderKeys, List<RMetaAnnotatedType> binderTypes) {

    /** Canonicalises to unmodifiable, defensively-copied, length-matched lists. */
    public ThenChainHoist {
        if (binderKeys.size() != binderTypes.size()) {
            throw new IllegalArgumentException(
                    "binderKeys/binderTypes length mismatch: " + binderKeys.size() + " vs " + binderTypes.size());
        }
        binderKeys = List.copyOf(binderKeys);
        binderTypes = List.copyOf(binderTypes);
    }
}
