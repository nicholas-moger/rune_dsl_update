package com.regnosys.rosetta.symbols.index;

import com.regnosys.rosetta.ast.RNode;

import java.util.*;

/**
 * Reverse index: target node → all nodes that reference it. Powers
 * {@code RWorkspace.findReferences}. Built by GlobalResolutionPass and
 * LexicalResolutionPass as cross-references are resolved.
 *
 * <p>Spec: D6 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
public final class ReferenceIndex {

    private final Map<RNode, List<RNode>> referers = new IdentityHashMap<>();

    /**
     * Membership mirror of {@link #referers}, so a repeat registration is
     * rejected in O(1) rather than by scanning the referer list — a linear scan
     * would make a heavily-referenced target quadratic, and this index is built
     * inside the linker's hot path.
     */
    private final Map<RNode, Set<RNode>> registered = new IdentityHashMap<>();

    /**
     * Record that {@code source} references {@code target}. Idempotent by
     * IDENTITY — the semantics the {@code IdentityHashMap} keying already
     * implies — so a caller that registers the same pair on more than one pass
     * does not inflate {@link #findReferences}.
     *
     * <p>It used to append unconditionally. A feature call binding a choice
     * OPTION or a META type never filled {@code TypeInferenceEngine}'s legacy
     * {@code resolvedFeature} slot (typed to {@code RAttribute}, so it
     * structurally cannot hold an option), its Category-1 gate therefore never
     * closed, and each fixed-point iteration re-registered the same reference —
     * measured at up to 3x on the conformance suite while attribute seats,
     * whose gate does close, stayed clean. The engine gate is fixed too; this
     * keeps the invariant true regardless of caller (v3.1 C1, Copilot R8 on
     * PR #566).
     */
    public void registerReference(RNode source, RNode target) {
        Set<RNode> seen = registered.computeIfAbsent(
                target, k -> Collections.newSetFromMap(new IdentityHashMap<>()));
        if (seen.add(source)) {
            referers.computeIfAbsent(target, k -> new ArrayList<>()).add(source);
        }
    }

    public List<RNode> findReferences(RNode target) {
        return List.copyOf(referers.getOrDefault(target, List.of()));
    }
}
