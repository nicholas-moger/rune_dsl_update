package com.regnosys.rosetta.ir.expr.anf;

/**
 * A single hoisted statement — a let-binding {@code name = value} that must be emitted before the
 * {@link Block#result()} (and before any later {@link Bind}) that reads it (design §5,
 * {@code Binding = Bind{ TempName name, ANFExpr value }}). At the Java target a {@code Bind} becomes a
 * real local only at a hoist sink:
 * <ul>
 *   <li>a {@link Block} value → {@code final X name = <value>;} (the fluent-chain initializer form, used
 *       e.g. for a {@code thenArg} hoist);</li>
 *   <li>a {@link JoinPoint} value → the declare-then-assign form: a statement-position conditional/switch
 *       hoist {@code <X> name = null; if(…){name=…}…} (NON-final, {@code null}-init; an absent {@code else}
 *       leaves the {@code null}). See {@link JoinPoint} for the two declare-then-assign lowerings.</li>
 * </ul>
 *
 * <p>{@code name}'s numeric suffix is resolved by the render-walk replay (see {@link TempName}); the
 * order of {@code Bind}s within a {@link Block#lets()} IS the registration/render order, so it must be
 * preserved exactly — it is what the per-scope numbering replays.
 *
 * <p>Lab-authored Phase-2; not present upstream.
 *
 * @param name  the temporary this statement binds
 * @param value the normalized right-hand side — a {@link Block} (initializer) or {@link JoinPoint}
 *              (branch-assign)
 */
public record Bind(TempName name, ANFExpr value) {
}
