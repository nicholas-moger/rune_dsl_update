package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A let-binding {@code let <binder> = <value> in <in>} (design §3 / §5 {@code then → Let}) — the desugared
 * form of a {@code then}-pipe. It is a neutral <em>fact</em>: it records "bind {@code value} to a binder
 * named {@code binder}, then evaluate {@code in} (which references {@code binder})", never a target form. It
 * is a first-class binding across targets — Java (a hoisted {@code thenArg} local), Python ({@code let}/the
 * walrus {@code :=}), Rust ({@code let}), Morphir ({@code Let}). The {@code then}-desugar produces, for a
 * chain {@code e0 then f1 then … then fN}, a flattened chain of {@code Let}s whose binders are the
 * intermediate pipe values; the Java target lowers a SET-position chain into the {@code thenArg0..n−1}
 * temporaries (the {@link com.regnosys.rosetta.ir.expr.anf ANF tier}).
 *
 * <h2>Three distinct identities — keep them apart (the L-029 split)</h2>
 * <ul>
 *   <li><strong>{@code binder} — the neutral binder NAME.</strong> A fresh name keyed by the {@code then}
 *       node (design §5:360, {@code fresh(thenNodeId)}); it is what the C-stage capture-avoiding substitution
 *       stamps as a {@link IRVariable} of kind {@code LET_BINDER} (which references its binder <em>by name</em>),
 *       and what a Python/Rust/Morphir emitter prints as the bound name. It is NOT the Java temp lexeme.</li>
 *   <li><strong>{@link #nodeId()} — the structural identity / Java temp-numbering KEY.</strong> A {@code then}
 *       {@code Let} has exactly ONE binder, so the node's own {@code NodeId} keys its temporary (V3's
 *       {@code (lambdaNodeId, paramIndex)} addressing is only for multi-binder lambdas such as {@code reduce}
 *       acc,item — out of scope here, an additive extension). The Java render <em>number</em> comes from the
 *       per-scope render-walk registration order, never from this key (see {@link com.regnosys.rosetta.ir.expr.anf.TempName}).</li>
 *   <li><strong>The Java render lexeme {@code thenArg}/{@code thenArgN} is NOT on this node.</strong> It is the
 *       {@code AnfSkeletonRenderer}'s decision — the {@code TempName} base {@code "thenArg"} is assigned at the
 *       Java-target {@code normalize} ({@code normalize : IRExpr → ANFExpr} is the Java-target lowering), so the
 *       neutral node stays target-agnostic.</li>
 * </ul>
 *
 * <h2>Children are load-bearing</h2>
 * {@code children()} are {@code [value, in]} (the binder is a name, not a child node). {@code type()} is the
 * {@code Let}'s inferred type — for a {@code then}-{@code Let} it is the type of {@code in} (the pipe's result
 * after this step).
 *
 * <h2>Live driving is the open obligation ("C"); offline validation goes through facts</h2>
 * This node is defined for the live SET-position driving at <strong>C</strong> (where the {@code RThenExpr}→
 * {@code Let} desugar with the V4 capture-avoiding substitution runs and the bodies lower). At the vendored
 * #219 pin the corpus then-chain bodies are {@code item}/alias-receiver constructs that do not lower, so the
 * Phase-A offline harness validates the {@code thenArg} hoist <em>skeleton</em> (count + numbering + render
 * form) from the chain's {@link com.regnosys.rosetta.ir.expr.anf.ThenChainHoist} facts, not from a fully-lowered
 * {@code Let} (see {@code notes/wave6-anf-slice3-thenchain.md}). The {@code normalize(Let)} flatten is exercised
 * offline only on a synthetic, lowerable chain.
 *
 * <p>Lab-authored Phase-2; not present upstream.
 *
 * @param binder the neutral binder name (referenced by a {@code LET_BINDER} {@link IRVariable}); NOT the Java
 *               {@code thenArg} render lexeme
 * @param value  the bound value subtree (non-null) — what the binder is bound to
 * @param in     the continuation subtree (non-null) — evaluated with the binder in scope; references it
 */
public record Let(
        String binder,
        IRExpr value,
        IRExpr in,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.LET;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of(value, in);
    }
}
