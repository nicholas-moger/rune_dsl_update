/**
 * The AST → expression-IR builder. {@link com.regnosys.rosetta.ir.expr.adapter.ExpressionToIRAdapter}
 * lowers a Rune {@code RExpression} subtree into the neutral
 * {@link com.regnosys.rosetta.ir.expr.IRExpr} hierarchy, computing each node's
 * {@code type}/{@code cardinality} facts from the workspace and assigning structural
 * {@link com.regnosys.rosetta.ir.expr.NodeId}s as it descends.
 *
 * <p>Deliberately a <em>sibling</em> of {@code com.regnosys.rosetta.ir.adapter} (the
 * Phase-1 declaration adapter) rather than a member of it: that package's exact file
 * set is pinned by {@code IRAdapterShellTest}, and the expression IR is its own
 * hierarchy (Phase-2 design gate Q1). Keeping the expression builder here leaves the
 * declaration-adapter contract untouched.
 *
 * <p>Lab-authored Phase-2 (Wave 0); not present upstream.
 */
package com.regnosys.rosetta.ir.expr.adapter;
