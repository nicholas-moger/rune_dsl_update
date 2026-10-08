/**
 * The Phase-2 <strong>expression IR</strong> — a separate, language-neutral node
 * hierarchy rooted at {@link com.regnosys.rosetta.ir.expr.IRExpr}, sibling to (but
 * deliberately <em>not</em> part of) the wire-pinned {@code com.regnosys.rosetta.ir.core}
 * declaration IR.
 *
 * <h2>Why a separate hierarchy</h2>
 * The declaration IR's {@link com.regnosys.rosetta.ir.core.IRKind} is a closed,
 * serialization-pinned enum with zero expression members (frozen for the M9 Python
 * target and guarded by {@code IRKindCoverageTest}). Making {@code IRExpr} a subtype
 * of {@code IRNode} would force expression members into that frozen enum. So the
 * expression IR is its own root with its own closed {@link com.regnosys.rosetta.ir.expr.IRExprKind},
 * bridged into the declaration graph only at the expression <em>seam roots</em>
 * (function value / condition / alias body; rule expression; data-type condition)
 * via {@code Optional<IRExpr>} accessors — never by widening {@code IRKind}. This is
 * the ratified outcome of Phase-2 design gate&nbsp;Q1 (2026-06-19); see
 * {@code notes/expr-ir-design.md} §2.6 and decision&nbsp;L-010.
 *
 * <h2>Neutrality contract</h2>
 * Every node names a <em>what</em> (a semantic operator plus operand subtrees and
 * resolved type / cardinality / optionality facts). Every Java <em>how</em> — type
 * lowering, {@code Mapper} wrapping, null-safety, statement hoisting, temp naming —
 * lives in the emitter, never in these nodes. The same neutral IR is intended to
 * feed future Python / Rust emitters, so a leaked Java-ism (a {@code Mapper}
 * type, a temp-var name) is a design defect, not a detail.
 *
 * <h2>Scope</h2>
 * The package grows one emission wave at a time. <strong>Wave 0</strong> (leaf
 * primitives) introduces the literal and reference/var/apply leaves; later waves add
 * the binary, navigation, collection, control-flow, existence, conversion and
 * construction families catalogued in {@code notes/expr-ir-design.md} §3–§4.
 *
 * <p>This package is fork-owned lab work (decision L-009/L-010); it is not present
 * upstream.
 */
package com.regnosys.rosetta.ir.expr;
