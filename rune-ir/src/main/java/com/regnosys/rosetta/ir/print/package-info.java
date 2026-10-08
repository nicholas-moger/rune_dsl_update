/**
 * Deterministic, print-only MLIR-style textual renderer for the neutral IR.
 *
 * <p>This package gives the hand-rolled IR a stable-to-read textual form used
 * as (a) a developer debug surface, (b) the golden-IR regression sentinel for
 * the declaration adapter, and (c) the conceptual foundation for the step-2
 * JSON serialization. It is <strong>read-only</strong> over the IR, so it
 * cannot affect the Java byte-parity emission path.
 *
 * <h2>Two entry points over two disjoint hierarchies</h2>
 * The IR has two unrelated roots with two closed discriminator enums:
 * {@link com.regnosys.rosetta.ir.core.IRNode} (declarations, keyed by
 * {@link com.regnosys.rosetta.ir.core.IRKind}) and
 * {@link com.regnosys.rosetta.ir.expr.IRExpr} (expressions, keyed by
 * {@link com.regnosys.rosetta.ir.expr.IRExprKind}). {@link IRPrinter} therefore
 * exposes {@code print(IRNode)} / {@code printAll(List)} and a separate
 * {@code print(IRExpr)}; each dispatches with a closed-enum {@code switch} over
 * an open node type (no GoF visitor).
 *
 * <h2>Guarantees</h2>
 * <ul>
 *   <li><b>Deterministic:</b> declared/source order is preserved for fields,
 *       enum values and operands; only genuinely-unordered collections (meta
 *       attributes) are sorted. No hashes, addresses, timestamps or locale.</li>
 *   <li><b>Lossless by construction:</b> every record component is rendered
 *       except an explicit allowlist ({@code sourceRange}, {@code metadata},
 *       {@code nodeId}); the default {@code switch} arm never declines.</li>
 *   <li><b>Non-stable text:</b> the textual form is free to churn; durability
 *       and versioning live on the step-2 JSON wire form.</li>
 * </ul>
 *
 * <h2>No EMF / no codegen coupling</h2>
 * Nothing here may reference Xtext, EMF/Ecore, or the {@code rune-java-generator}
 * module; the IR is the neutral hub.
 */
package com.regnosys.rosetta.ir.print;
