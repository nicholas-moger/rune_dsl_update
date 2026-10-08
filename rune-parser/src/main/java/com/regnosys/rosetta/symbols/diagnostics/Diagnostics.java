package com.regnosys.rosetta.symbols.diagnostics;

import com.regnosys.rosetta.ast.SourceRange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mutable diagnostic collector used by the M3 linker passes. The
 * {@link com.regnosys.rosetta.symbols.RWorkspace} exposes only the
 * immutable list (via {@link #toList()}); this class itself is internal
 * to the M3 linker, never escapes to consumers.
 *
 * <p>Spec: D10 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
public final class Diagnostics {

    private final List<LinkingDiagnostic> entries = new ArrayList<>();

    /**
     * How many live entries carry each {@code (category, range)} pair — the
     * NEGATIVE INDEX that lets {@link #removeMatching} answer a miss without
     * touching {@link #entries} (v3.1 C1 MF-7).
     *
     * <p>It is deliberately a COUNT rather than a position: {@code entries} is
     * insertion-ordered and consumers read that order, so removals cannot be
     * allowed to shift stored indices. A count is enough for the property that
     * matters — "is there anything to find?" — and leaves the hit path, and
     * the emission order, exactly as they were.
     *
     * <p>Both emission points maintain it, and {@link #removeMatching}
     * decrements on a successful removal, so a key emitted → removed → emitted
     * again is findable again. Keys are safe: {@link DiagnosticCategory} is an
     * enum and {@link SourceRange} a record.
     */
    private final Map<Key, Integer> liveByKey = new HashMap<>();

    private record Key(DiagnosticCategory category, SourceRange range) {}

    public void error(
            DiagnosticCategory category,
            SourceRange range,
            String unresolvedName,
            String message,
            List<String> candidates) {
        entries.add(new LinkingDiagnostic(
            Severity.ERROR, category, range, unresolvedName, message, candidates));
        index(category, range);
    }

    public void warning(
            DiagnosticCategory category,
            SourceRange range,
            String unresolvedName,
            String message,
            List<String> candidates) {
        entries.add(new LinkingDiagnostic(
            Severity.WARNING, category, range, unresolvedName, message, candidates));
        index(category, range);
    }

    private void index(DiagnosticCategory category, SourceRange range) {
        liveByKey.merge(new Key(category, range), 1, Integer::sum);
    }

    public boolean isEmpty() { return entries.isEmpty(); }

    public int size() { return entries.size(); }

    /** Returns an immutable snapshot. The collector itself stays mutable. */
    public List<LinkingDiagnostic> toList() {
        return List.copyOf(entries);
    }

    /**
     * Removes diagnostics matching the given category + range. Used by the M4
     * type-directed resolver (Phase X1 Categories 8/9/10) to clear stale M3
     * cross-ref diagnostics when a late-resolution path successfully binds the
     * reference. Without this, the
     * {@code LinkerInvariants.noNodeIsBothResolvedAndDiagnosed} invariant
     * fires when Cat 9 sets {@code RSymbolReference.resolvedSymbol} on a
     * reference that M3 had previously diagnosed as SYMBOL_NOT_FOUND.
     *
     * <p>Implemented as an O(1) index probe, then iterator + break on first
     * match. The single M3 emission point (the
     * {@code collector.error(SYMBOL_NOT_FOUND, range, ...)} call in
     * {@code LexicalResolutionPass} around line 114) writes at most one
     * diagnostic per (category, sourceRange) pair — every unresolved ref has a
     * distinct source range, so uniqueness holds in practice and we can stop
     * after the first removal. The contract is one-per-call regardless of
     * whether that argument holds; {@code DiagnosticsRemoveMatchingTest} locks
     * it either way.
     *
     * <p><b>Why the index exists (v3.1 C1 MF-7).</b> Iterator+break is O(K) on
     * a HIT, where K is the matched entry's index — the case the original
     * design reasoned about. On a MISS it was O(N): a full scan of a list the
     * corpus builds to ~180k entries. Misses are not the rare case. There are
     * ~58 call sites in {@code TypeInferenceEngine} alone, C0's refusal design
     * keeps adding more, and the C1 R9 arm fires two clears per bound node
     * whose diagnostics, after the first pass, are already gone — so the
     * steady state is miss, miss, miss against the whole list, once per node
     * per fixed-point iteration. That wedged the full parser suite for 45+
     * minutes inside {@code SymbolTableSnapshotTest} (surefire thread dumps
     * caught it here, in this method). The count map answers those calls
     * without touching the list; a genuine hit still costs O(K), which is what
     * the original analysis budgeted for.
     *
     * @return the number of diagnostics removed (0 or 1 by uniqueness).
     */
    public int removeMatching(DiagnosticCategory category, SourceRange range) {
        Key key = new Key(category, range);
        Integer live = liveByKey.get(key);
        if (live == null) {
            return 0;
        }
        java.util.Iterator<LinkingDiagnostic> it = entries.iterator();
        while (it.hasNext()) {
            LinkingDiagnostic d = it.next();
            if (d.category() == category && d.range().equals(range)) {
                it.remove();
                if (live == 1) {
                    liveByKey.remove(key);
                } else {
                    liveByKey.put(key, live - 1);
                }
                return 1;
            }
        }
        // The index promised an entry the list does not have — the two are out
        // of step, which is a bug in this class rather than a caller's problem.
        // Drop the stale key so the miss stays O(1) from here on.
        liveByKey.remove(key);
        return 0;
    }
}
