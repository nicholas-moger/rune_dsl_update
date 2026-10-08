package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.RRootElement;

import java.util.*;
import java.util.function.Predicate;

/**
 * Per-file visible scope. Wraps the workspace symbol table with the file's
 * own namespace + imported namespaces + the implicit built-in namespace.
 * Lookup is a chain walk: own → imports → builtins → empty.
 *
 * <p>Spec: D8 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
public final class RFileScope {

    private final RModel model;
    private final RNamespaceScope ownNamespace;
    private final List<ImportEntry> imports;
    private final RNamespaceScope builtinNamespace;
    private final CellPreference cellPreference;

    public RFileScope(
            RModel model,
            RNamespaceScope ownNamespace,
            List<ImportEntry> imports,
            RNamespaceScope builtinNamespace) {
        this(model, ownNamespace, imports, builtinNamespace, CellPreference.NONE);
    }

    /** PR #445 — the linker's constructor, carrying the workspace's cell preference. */
    public RFileScope(
            RModel model,
            RNamespaceScope ownNamespace,
            List<ImportEntry> imports,
            RNamespaceScope builtinNamespace,
            CellPreference cellPreference) {
        this.model = Objects.requireNonNull(model);
        this.ownNamespace = Objects.requireNonNull(ownNamespace);
        this.imports = List.copyOf(imports);
        this.builtinNamespace = builtinNamespace; // may be null in test scenarios
        this.cellPreference = Objects.requireNonNull(cellPreference);
    }

    public RModel model() { return model; }
    public RNamespaceScope ownNamespace() { return ownNamespace; }
    public List<ImportEntry> imports() { return imports; }
    public RNamespaceScope builtinNamespace() { return builtinNamespace; }
    /** PR #445 — the workspace's cell preference (NONE outside cell-rooted builds). */
    public CellPreference cellPreference() { return cellPreference; }

    /**
     * Looks up a name in the file's visible scope. Walks own namespace
     * first, then imported namespaces in declaration order, then built-ins.
     *
     * <p>PR #445 — TWO-PASS walk. Pass 1 admits only candidates the
     * {@link CellPreference} prefers for THIS file (declared in this file, or
     * in this file's cell) across the whole own+imports chain; pass 2 is the
     * historical first-match walk. The two-pass shape (not a per-level pick)
     * is load-bearing: the merged multi-closure population carries
     * NAMESPACE-LAYOUT drift between cells (CDM 6 moved types such as
     * {@code PriceQuantity} to different namespaces), so a c5 file's
     * reference can match a c6-ONLY namespace through an EARLIER import while
     * the c5 declaration sits behind a LATER import — upstream's per-closure
     * build never sees the foreign candidate at all. With no cell roots pass
     * 1 degenerates to the same-file test (Xtext's
     * local-scope-shadows-global), and pass 2 keeps every historical
     * resolution — a name found only in a foreign cell still resolves
     * (lenient: the preference never creates a new failure).
     */
    public Optional<RRootElement> lookup(String name) {
        Optional<RRootElement> preferred = lookupPreferred(name);
        if (preferred.isPresent()) return preferred;
        Optional<RRootElement> hit = ownNamespace.lookup(name);
        if (hit.isPresent()) return hit;
        for (ImportEntry imp : imports) {
            hit = imp.lookup(name);
            if (hit.isPresent()) return hit;
        }
        if (builtinNamespace != null) {
            return builtinNamespace.lookup(name);
        }
        return Optional.empty();
    }

    /**
     * Pass 1 of {@link #lookup}: the first same-FILE candidate across own +
     * imports (tier 0 — upstream's local scope), then the first same-CELL
     * candidate (tier 1 — the per-closure emulation).
     */
    private Optional<RRootElement> lookupPreferred(String name) {
        Optional<RRootElement> sameCell = Optional.empty();
        for (RRootElement c : ownNamespace.allMatching(name)) {
            if (CellPreference.modelOf(c) == model) return Optional.of(c);
            if (sameCell.isEmpty() && cellPreference.prefers(c, model)) sameCell = Optional.of(c);
        }
        for (ImportEntry imp : imports) {
            for (RRootElement c : imp.allMatching(name)) {
                if (CellPreference.modelOf(c) == model) return Optional.of(c);
                if (sameCell.isEmpty() && cellPreference.prefers(c, model)) sameCell = Optional.of(c);
            }
        }
        return sameCell;
    }

    /**
     * Kind-filtered sibling of {@link #lookup(String)} — the SAME two-pass
     * walk with every candidate additionally gated by {@code kind}, so a
     * kind-mismatched earlier registrant can no longer shadow a later
     * kind-matched declaration at any level (PR #450; upstream's scoping is
     * ECLASS-typed per cross-reference, i.e. kind-filtered by construction —
     * a {@code [RosettaRule|QualifiedName]} ref never sees a same-named type
     * as a candidate). {@link #lookup(String)} is byte-untouched; this path
     * runs only where a seat supplies its position's upstream ECLASS filter.
     */
    public Optional<RRootElement> lookupOfKind(String name, Predicate<RRootElement> kind) {
        // Pass 1 — the lookupPreferred walk, kind-gated.
        Optional<RRootElement> sameCell = Optional.empty();
        for (RRootElement c : ownNamespace.allMatching(name)) {
            if (!kind.test(c)) continue;
            if (CellPreference.modelOf(c) == model) return Optional.of(c);
            if (sameCell.isEmpty() && cellPreference.prefers(c, model)) sameCell = Optional.of(c);
        }
        for (ImportEntry imp : imports) {
            for (RRootElement c : imp.allMatching(name)) {
                if (!kind.test(c)) continue;
                if (CellPreference.modelOf(c) == model) return Optional.of(c);
                if (sameCell.isEmpty() && cellPreference.prefers(c, model)) sameCell = Optional.of(c);
            }
        }
        if (sameCell.isPresent()) return sameCell;
        // Pass 2 — the historical first-match walk in lookup's level order
        // (own → imports in declaration order → builtins), kind-gated.
        for (RRootElement c : ownNamespace.allMatching(name)) {
            if (kind.test(c)) return Optional.of(c);
        }
        for (ImportEntry imp : imports) {
            for (RRootElement c : imp.allMatching(name)) {
                if (kind.test(c)) return Optional.of(c);
            }
        }
        if (builtinNamespace != null) {
            for (RRootElement c : builtinNamespace.allMatching(name)) {
                if (kind.test(c)) return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns every declaration visible in this file (own + imports + builtins).
     * Used by M6 for the names-are-unique check.
     */
    public List<RRootElement> lookupAll() {
        List<RRootElement> all = new ArrayList<>(ownNamespace.allDeclarations());
        for (ImportEntry imp : imports) {
            all.addAll(imp.target().allDeclarations());
        }
        if (builtinNamespace != null) {
            all.addAll(builtinNamespace.allDeclarations());
        }
        return List.copyOf(all);
    }
}
