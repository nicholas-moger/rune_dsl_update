package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ast.model.RModel;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PR #445 — same-file / same-cell candidate preference for name resolution.
 *
 * <p><b>The problem:</b> the fork's corpus-diagnostic population deliberately
 * loads MULTIPLE upstream build closures into one workspace (the merged
 * two-cell cdm space: cdm-5.38.0 + cdm-6.20.6 share every namespace, so
 * {@code cdm.product.template.Product} is declared once per cell).
 * {@link RNamespaceScope#lookup} is first-registered-wins ("Xtext's index
 * first-wins lookup"), so every cdm-6.20.6 reference to a shared-FQN name
 * resolved into the 5.38.0 cell — the #445 census measured the fallout as the
 * dominant residual linking-error mechanism ({@code product -> economicTerms}
 * ×92, {@code Payout} choice-option navs, {@code Trade.tradeLot}, the
 * EligibleCollateralCriteria extends carrier behind the cdm
 * MISSING_ATTRIBUTE validation row). Upstream NEVER faces the collision: each
 * mojo build loads exactly one closure.
 *
 * <p><b>The rule (rank order within one name's candidate list):</b>
 * <ol>
 *   <li><b>Same file</b> — a candidate declared in the requesting model
 *       itself. Upstream-faithful: Xtext's
 *       {@code ImportedNamespaceAwareLocalScopeProvider} puts a resource's own
 *       declarations in a local scope that shadows the global index.</li>
 *   <li><b>Same cell</b> — a candidate declared under the same load root
 *       (cell) as the requesting model, when cell roots were supplied to
 *       {@link RWorkspace#build(List, List)}. This emulates upstream's
 *       per-closure isolation inside the fork's merged space: a cdm-6.20.6
 *       file sees the 6.20.6 declaration exactly as upstream's cdm6 build
 *       would. No upstream analogue is needed — upstream cannot collide
 *       across closures.</li>
 *   <li><b>Registration order</b> — the status-quo first-wins pick. Also the
 *       cross-cell FALLBACK: a name declared only in another cell still
 *       resolves (lenient — never fabricates a new failure).</li>
 * </ol>
 *
 * <p>With no cell roots ({@link #NONE} / the plain {@code build(files)}
 * overload) only the same-file rank is active; in single-closure populations
 * (every D11 generator cell) namespaces carry no duplicate names, so every
 * rank degenerates to the status-quo pick — the preference is
 * generation-neutral by construction there.
 */
public final class CellPreference {

    /** No cell roots: same-file preference only (rank 2 never fires). */
    public static final CellPreference NONE = new CellPreference(new IdentityHashMap<>());

    /** Model → cell index (identity keys); models with no matching root are absent (-1). */
    private final IdentityHashMap<RModel, Integer> modelCells;

    private CellPreference(IdentityHashMap<RModel, Integer> modelCells) {
        this.modelCells = modelCells;
    }

    /**
     * Assigns each model the LONGEST-prefix matching cell root (normalized to
     * forward slashes; a root matches a model whose file path equals it or
     * continues under it with a separator). Models matching no root get no
     * cell (same-file rank only). Empty/null roots → {@link #NONE}.
     */
    public static CellPreference fromRoots(List<RModel> files, List<Path> cellRoots) {
        if (cellRoots == null || cellRoots.isEmpty()) {
            return NONE;
        }
        List<String> rootPrefixes = new ArrayList<>(cellRoots.size());
        for (Path root : cellRoots) {
            rootPrefixes.add(normalize(root.toString()));
        }
        IdentityHashMap<RModel, Integer> cells = new IdentityHashMap<>();
        for (RModel model : files) {
            String file = normalize(fileOf(model));
            int best = -1;
            int bestLen = -1;
            for (int i = 0; i < rootPrefixes.size(); i++) {
                String prefix = rootPrefixes.get(i);
                if (prefix.length() > bestLen
                        && (file.equals(prefix) || file.startsWith(prefix + "/"))) {
                    best = i;
                    bestLen = prefix.length();
                }
            }
            if (best >= 0) {
                cells.put(model, best);
            }
        }
        return new CellPreference(cells);
    }

    /**
     * Picks the preferred candidate for the requesting model: first by rank
     * (same file, same cell, rest), ties by list order (registration order).
     * Zero-allocation single scan; {@code requester == null} or a size-≤1 list
     * short-circuits to the status-quo pick.
     */
    public Optional<RRootElement> pick(List<RRootElement> candidates, RModel requester) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        if (candidates.size() == 1 || requester == null) {
            return Optional.of(candidates.get(0));
        }
        RRootElement best = null;
        int bestRank = Integer.MAX_VALUE;
        for (RRootElement c : candidates) {
            int rank = rank(c, requester);
            if (rank < bestRank) {
                bestRank = rank;
                best = c;
                if (rank == 0) {
                    break;
                }
            }
        }
        return Optional.of(best);
    }

    /**
     * Preference-ordered view of the candidate list (stable: rank, then
     * registration order). For callers that iterate candidates with their own
     * kind filter (type-position walks).
     */
    public List<RRootElement> order(List<RRootElement> candidates, RModel requester) {
        if (candidates.size() <= 1 || requester == null) {
            return candidates;
        }
        List<RRootElement> sorted = new ArrayList<>(candidates);
        // stable sort preserves registration order within a rank
        sorted.sort((a, b) -> Integer.compare(rank(a, requester), rank(b, requester)));
        return sorted;
    }

    /**
     * True when the candidate is declared in the requesting model itself or in
     * the requester's cell (rank ≤ 1) — the pass-1 filter of the two-pass
     * scope walk: a c5 file's reference must not bind a c6-only namespace's
     * declaration through an earlier import when a later import (or the same
     * namespace's duplicate list) holds the c5 declaration, because
     * upstream's per-closure build would never see the foreign candidate at
     * all (the #445 namespace-layout drift: CDM 6 moved types like
     * {@code PriceQuantity} to different namespaces, so the collision spans
     * IMPORTS, not just one namespace's duplicate list). With no cell roots
     * this degenerates to the same-file test — exactly Xtext's
     * local-scope-shadows-global.
     */
    public boolean prefers(RRootElement candidate, RModel requester) {
        return requester != null && rank(candidate, requester) <= 1;
    }

    /** 0 = same file, 1 = same cell (both known), 2 = neither. */
    private int rank(RRootElement candidate, RModel requester) {
        RModel declModel = modelOf(candidate);
        if (declModel == null) {
            return 2;
        }
        if (declModel == requester) {
            return 0;
        }
        int declCell = modelCells.getOrDefault(declModel, -1);
        if (declCell >= 0 && declCell == modelCells.getOrDefault(requester, -1)) {
            return 1;
        }
        return 2;
    }

    /** The declaring {@link RModel} of a node (parent-chain walk), or null. */
    public static RModel modelOf(RNode node) {
        RNode cur = node;
        while (cur != null && !(cur instanceof RModel)) {
            cur = cur.parent();
        }
        return (RModel) cur;
    }

    /**
     * A model's source file string — its own range's file, falling back to the
     * first root element's (defensive: loaders name models via
     * {@code AstBuilder.buildFromString(source, path)}, which stamps ranges).
     */
    private static String fileOf(RModel model) {
        String file = model.sourceRange() == null ? null : model.sourceRange().file();
        if (file == null || file.isEmpty() || "<unknown>".equals(file)) {
            for (RNode child : model.rootElements()) {
                SourceRange r = child.sourceRange();
                if (r != null && r.file() != null && !"<unknown>".equals(r.file())) {
                    return r.file();
                }
            }
            return "";
        }
        return file;
    }

    private static String normalize(String path) {
        return path == null ? "" : path.replace('\\', '/');
    }
}
