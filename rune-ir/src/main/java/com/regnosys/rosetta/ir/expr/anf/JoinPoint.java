package com.regnosys.rosetta.ir.expr.anf;

/**
 * The normal-form of a branching construct (conditional / switch) whose value is bound to a temporary
 * (design §5, {@code JoinPoint{ TempName result, ANFExpr branching }}). Java has no expression-form
 * {@code if}/{@code switch}, so a branching value cannot be a {@code final X t = <branch>;} initializer;
 * instead {@code normalize} captures it as a {@link Bind} whose value is this {@code JoinPoint} (the
 * declare-then-assign form), and — in operand position — replaces the branching node's position with a
 * reference to {@code result}.
 *
 * <h2>Two declare-then-assign lowerings at the Java target</h2>
 * Confirmed by the Q3 hoist-trace dump (§1.1, §3):
 * <ul>
 *   <li><strong>Statement (SET) position</strong> — the {@code ifThenElseResult}/switch-result hoist:
 *       {@code <Type> result = null;} (NON-final, {@code null}-initialized) followed by the {@code branching}
 *       construct with each present branch assigning into {@code result}; an absent {@code else} leaves the
 *       {@code null} (e.g. {@code Date ifThenElseResult0 = null; if(<cond>){ ifThenElseResult0 = <then>; }}).</li>
 *   <li><strong>Operand position</strong> — the same shape but the result is consumed by an enclosing
 *       expression, which is replaced by a reference to {@code result}.</li>
 * </ul>
 * A Python target lowers the same node to a ternary, a Rust target to a {@code let result = if …} — the IR
 * records only "this branch produces a joined value into {@code result}", never the target form.
 * {@code result}'s {@link TempName} key is the branching node's structural
 * {@link com.regnosys.rosetta.ir.expr.NodeId}; its numeric suffix is assigned by the render-walk replay.
 *
 * <p><strong>Note (correcting the earlier model sketch):</strong> a top-level statement-position
 * conditional/switch IS a {@code JoinPoint} (the declare-then-assign / {@code = null} form above), NOT a
 * {@link Bind} of a {@link Block} initializer — the dump emits {@code <Type> t = null; if(…){…}}, which a
 * {@code Block}-value {@link Bind} ({@code final X = value;}) cannot express.
 *
 * <p>Lab-authored Phase-2; not present upstream.
 *
 * @param result    the temporary the branches join into
 * @param branching the normalized branching construct (the conditional/switch body), or {@code null} in
 *                  the Phase-A offline skeleton, where the branch bodies are masked (rendered at the live
 *                  "C" stage) and only the hoist skeleton — temp name/number/order, declared type,
 *                  declare-then-assign shape — is validated
 */
public record JoinPoint(TempName result, ANFExpr branching) implements ANFExpr {
}
