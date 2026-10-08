package com.regnosys.rosetta.ir.expr.anf;

/**
 * A normalized (A-Normal-Form) expression — the result of {@code normalize : IRExpr(core) → ANFExpr}
 * (design §5). In the common case this is a {@link Block} (a sequence of let-bindings followed by a
 * trivial atom); when a branching construct (conditional / switch) is bound in operand position it is
 * a {@link JoinPoint} (which lowers to a {@code final X t; if(…){t=…}else{…}} assign rather than a
 * {@code final X t = …;} initializer, since Java has no expression-form {@code if}).
 *
 * <p>The hierarchy is <strong>sealed</strong> (unlike the open {@link com.regnosys.rosetta.ir.expr.IRExpr}):
 * ANF is the lab's own Java-target lowering, so the set of forms is closed and the ANF→Java replay
 * dispatches exhaustively over it. A Python/Rust target would define its own normal form, not extend
 * this one.
 *
 * <p>Lab-authored Phase-2; not present upstream.
 */
public sealed interface ANFExpr permits Block, JoinPoint {
}
