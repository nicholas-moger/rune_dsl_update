/**
 * The <strong>ANF / hoisting tier</strong> of the Phase-2 expression IR — the
 * <em>Java-target</em> lowering of the neutral {@link com.regnosys.rosetta.ir.expr core IR}
 * into A-Normal Form, where every hoisted sub-expression is named by a let-binding and a
 * {@link com.regnosys.rosetta.ir.expr.anf.Bind} becomes a real Java local <em>only at the
 * hoist sinks</em> (conditional / then-chain / switch / the beyond-{@code long} bigInteger
 * literal), dissolving into the fluent {@code Mapper} chain everywhere else (the
 * {@code R-OPTIONBIND-LOCALS} local-count contract — the make-or-break byte-parity surface of
 * the hoisting tier; see {@code notes/expr-ir-design.md} §5 / §2.6 Q3).
 *
 * <p><strong>ANF is a per-TARGET lowering, not a mandatory shared pass.</strong> The neutral
 * {@code core} IR is never ANF'd; this {@code normalize} is the Java lowering, and a Python/Rust
 * emitter would apply its own (idiomatic let-bindings) — different lowerings of the <em>same</em>
 * core IR, so Java's hoist shape stays out of the neutral layer. The model mirrors the
 * Northeastern {@code (answer, context)} ANF contract: {@code normalize : IRExpr(core) → ANFExpr}
 * returns a {@link com.regnosys.rosetta.ir.expr.anf.Block} — an atom plus the bindings that must
 * precede it. Temporary <em>names</em> are NOT carried here as final numbers: a
 * {@link com.regnosys.rosetta.ir.expr.anf.TempName} carries a base lexeme + the
 * {@link com.regnosys.rosetta.ir.expr.NodeId} key, and the per-scope render-walk replay assigns
 * the numeric suffix (design §5 — per-method/per-lambda reset, render-walk order).
 *
 * <h2>⚠ OPEN OBLIGATION — this tier is OFFLINE-VALIDATED ONLY; the live wiring ("C") is MANDATORY and NOT yet done</h2>
 * This package is built + validated in <strong>Phase A</strong>: {@code normalize} and the ANF→Java
 * replay are proven against the maintainer's Q3 hoist-trace dump
 * ({@code handover/q3-hoist-trace-dump.md}) — high-confidence on those worked examples, but
 * <strong>NOT corpus-proven and NOT wired into the live generator.</strong> Hoisting is NOT
 * complete until <strong>Phase&nbsp;B / "C"</strong>: subclassing {@code FunctionExpressionRenderer}
 * so the IR genuinely drives the SET-position hoists (the only way to reach them — all SET-position
 * hoist constructs are renderer-intercepted, decision-log L-053/L-055), the FULL corpus byte gate
 * (2219 FUNCTION + 1489 RULE), and a re-pin to ≥#226 for switch + the operand-position conditional.
 * A future maintainer must NOT mistake "Phase&nbsp;A done" for "hoisting done" (Nick's standing
 * no-hacky-shortcut condition, decision-log L-056). See {@code notes/wave6-anf-scoping.md} v2 +
 * {@code notes/decision-log.md} L-054/L-055/L-056.
 *
 * <p>Lab-authored Phase-2; not present upstream.
 */
package com.regnosys.rosetta.ir.expr.anf;
