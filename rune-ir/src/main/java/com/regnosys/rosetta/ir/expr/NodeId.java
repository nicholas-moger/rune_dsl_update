package com.regnosys.rosetta.ir.expr;

import java.util.ArrayList;
import java.util.List;

/**
 * The deterministic <strong>structural identity</strong> of an {@link IRExpr} within
 * a single seam tree: the root-to-node path of child-slot indices, assigned in a
 * stable pre-order walk over {@link IRExpr#children()}. The seam root has the empty
 * path ({@link #ROOT}); its {@code i}-th child has path {@code [i]}; that child's
 * {@code j}-th child has {@code [i, j]}; and so on.
 *
 * <p><strong>Why a structural path, not a source range.</strong> The AST's
 * {@link com.regnosys.rosetta.ast.SourceRange} cannot serve as identity:
 * {@code SourceRange.NONE} is a single shared singleton stamped on every synthetic
 * node (injected default-else branches, the synthesized implicit {@code item}), so two
 * synthetic siblings would be range-identical — colliding temp names and silently
 * merging the Decorations sidecar. A child-index path over the frozen, stable
 * {@code children()} order is unique and reproducible run-to-run. This is the
 * ratified resolution of Phase-2 design gate&nbsp;Q2 (2026-06-19); see
 * {@code notes/expr-ir-design.md} §5 and the owed D-entry (NodeId&nbsp;≠&nbsp;sourceRange).
 *
 * <p><strong>Identity, not the temp number.</strong> {@code NodeId} is the stable
 * KEY used to map a temporary back to its node and to key Decorations. It is
 * <em>not</em> the temp's numeric suffix: the generator numbers temps by per-scope
 * render-walk registration order (design §5, gate Q3), which deliberately reorders
 * versus source pre-order. Do not derive temp numbers from this path.
 *
 * <p>Lambda/closure binders are not {@code children()} nodes; the wave that
 * introduces lambdas will extend identity to address binder slots as
 * {@code (lambdaNodeId, paramIndex)} (design §5, V3). Wave-0 leaves are all
 * addressable by the plain child-index path.
 */
public record NodeId(List<Integer> path) {

    /** The identity of a seam root: the empty child-index path. */
    public static final NodeId ROOT = new NodeId(List.of());

    /** Canonicalises to an unmodifiable, defensively-copied path. */
    public NodeId {
        path = List.copyOf(path);
    }

    /**
     * The identity of this node's {@code index}-th child (by {@link IRExpr#children()}
     * order). {@code index} must be a non-negative child slot.
     */
    public NodeId child(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("child index must be non-negative: " + index);
        }
        List<Integer> next = new ArrayList<>(path);
        next.add(index);
        return new NodeId(next);
    }

    /** A stable, path-like rendering for diagnostics, e.g. {@code "/"}, {@code "/0/2"}. */
    @Override
    public String toString() {
        if (path.isEmpty()) {
            return "/";
        }
        StringBuilder sb = new StringBuilder();
        for (Integer i : path) {
            sb.append('/').append(i);
        }
        return sb.toString();
    }
}
