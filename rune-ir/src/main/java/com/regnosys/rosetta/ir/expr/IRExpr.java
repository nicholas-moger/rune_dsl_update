package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * Root of the Phase-2 <strong>expression IR</strong> — a language-neutral semantic
 * node. {@code IRExpr} is deliberately <em>not</em> a subtype of
 * {@code com.regnosys.rosetta.ir.core.IRNode}: the declaration IR's {@code IRKind} is
 * a frozen, serialization-pinned enum with no expression members, so this hierarchy
 * carries its own closed {@link IRExprKind} and is bridged into the declaration graph
 * only at the expression seam roots (Phase-2 design gate Q1; see the package docs).
 *
 * <p>The interface is intentionally <strong>open</strong> (not sealed): external
 * targets (e.g. a cross-DSL importer) may supply their own implementations, and emitters
 * discriminate via {@link #kind()} rather than {@code instanceof} on a closed set.
 *
 * <h2>Every node carries four computed facts</h2>
 * Beyond its kind and operand {@link #children()}, every node carries facts the
 * <em>adapter</em> computes at build time — never re-derived per emitter (design §3/§6):
 * <ul>
 *   <li>{@link #nodeId()} — its deterministic structural identity within the seam tree;</li>
 *   <li>{@link #type()} — the inferred {@link RMetaAnnotatedType} (the meta-annotated
 *       envelope, not a bare {@code RType} — WithMeta/meta-parity needs the envelope);</li>
 *   <li>{@link #cardinality()} — SINGLE vs MULTI ({@link ExpressionCardinality});</li>
 *   <li>{@link #optionality()} — PRESENT vs OPTIONAL, the orthogonal absence axis.</li>
 * </ul>
 * These are <em>facts</em>, not Java forms: an emitter reads them to choose a lowering
 * (e.g. {@code mapItem} vs {@code mapSingleToList}, {@code a} vs {@code List a}), but no
 * node ever names a {@code Mapper} type, a temp variable, or a hoisting strategy.
 *
 * <h2>{@link #sourceRange()} is diagnostics-only</h2>
 * It is the originating AST node's range (possibly {@code SourceRange.NONE} for
 * synthetics) and must never be used as identity — that is {@link #nodeId()}'s job
 * (see {@link NodeId} for why).
 */
public interface IRExpr {

    /** The closed kind discriminator; the basis for exhaustive emitter dispatch. */
    IRExprKind kind();

    /** This node's deterministic structural identity within its seam tree. */
    NodeId nodeId();

    /**
     * The inferred meta-annotated type of this expression, as computed by the type
     * engine. May be {@link RMetaAnnotatedType#MISSING} when inference did not bind a
     * type (e.g. an unresolved reference); callers must tolerate the sentinel.
     */
    RMetaAnnotatedType type();

    /** Whether this expression yields a single value or a list. */
    ExpressionCardinality cardinality();

    /** Whether this expression may yield an absent value. */
    Optionality optionality();

    /**
     * The originating AST source range, for diagnostics only. May be
     * {@code SourceRange.NONE}. NOT an identity — use {@link #nodeId()}.
     */
    SourceRange sourceRange();

    /**
     * The direct operand sub-expressions, in a stable order. Empty for leaves. This
     * order is the basis of child {@link NodeId}s, so it must be deterministic.
     */
    List<? extends IRExpr> children();
}
