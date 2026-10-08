package com.regnosys.rosetta.symbols.diagnostics;

import com.regnosys.rosetta.ast.SourceRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE SEMANTIC LOCK ON {@link Diagnostics#removeMatching} — written for the
 * v3.1 C1 MF-7 fix, whose whole point is that the method gets FASTER without
 * getting DIFFERENT.
 *
 * <p>MF-7 (review the development audit "2026-08-16-c1-resolution-review"):
 * {@code removeMatching} early-exits on a HIT but scans the entire list on a
 * MISS, and the C1 R9 arm fires two guaranteed-miss calls per bound node per
 * fixed-point iteration against a ~180k-entry list. The fix adds an O(1)
 * negative index so a miss never scans; there are 58 call sites in
 * {@code TypeInferenceEngine} alone, and C0's refusal design keeps adding
 * more, so the index has to be exactly equivalent to the scan it replaces.
 *
 * <p>Every assertion here is a property of the ORIGINAL scan, deliberately
 * written to pass before the fix as well as after. A performance fix's test
 * cannot be red-then-green — the wedge is the failing witness, and its
 * receipt is the suite duration. What this class buys instead is that any
 * future drift between the index and the list is caught in 0.01 s rather than
 * as a mysterious diagnostic count somewhere in the 25-cell gate.
 */
class DiagnosticsRemoveMatchingTest {

    private static SourceRange at(int line) {
        return SourceRange.of5Arg("probe.rosetta", line, 1, line, 8);
    }

    private static void error(Diagnostics d, DiagnosticCategory category, SourceRange range) {
        d.error(category, range, "name", "message", List.of());
    }

    @Test
    void removing_a_present_entry_removes_exactly_one_and_reports_it() {
        Diagnostics diagnostics = new Diagnostics();
        error(diagnostics, DiagnosticCategory.SYMBOL_NOT_FOUND, at(1));
        error(diagnostics, DiagnosticCategory.SYMBOL_NOT_FOUND, at(2));

        assertEquals(1, diagnostics.removeMatching(DiagnosticCategory.SYMBOL_NOT_FOUND, at(1)));
        assertEquals(1, diagnostics.size());
        assertEquals(at(2), diagnostics.toList().get(0).range());
    }

    @Test
    void removing_an_absent_entry_reports_zero_and_changes_nothing() {
        Diagnostics diagnostics = new Diagnostics();
        error(diagnostics, DiagnosticCategory.SYMBOL_NOT_FOUND, at(1));

        // The MF-7 path: the overwhelmingly common call, and the one that used
        // to cost a full-list scan every time.
        assertEquals(0, diagnostics.removeMatching(DiagnosticCategory.SYMBOL_NOT_FOUND, at(99)));
        assertEquals(0, diagnostics.removeMatching(DiagnosticCategory.ENUM_NOT_FOUND, at(1)));
        assertEquals(1, diagnostics.size());
    }

    @Test
    void category_and_range_are_both_part_of_the_key() {
        Diagnostics diagnostics = new Diagnostics();
        error(diagnostics, DiagnosticCategory.ENUM_NOT_FOUND, at(1));
        error(diagnostics, DiagnosticCategory.ENUM_VALUE_NOT_FOUND, at(1));

        // The R9 arm clears exactly this pair at one range; they must be
        // independently removable.
        assertEquals(1, diagnostics.removeMatching(DiagnosticCategory.ENUM_NOT_FOUND, at(1)));
        assertEquals(1, diagnostics.size());
        assertEquals(DiagnosticCategory.ENUM_VALUE_NOT_FOUND, diagnostics.toList().get(0).category());
        assertEquals(1, diagnostics.removeMatching(DiagnosticCategory.ENUM_VALUE_NOT_FOUND, at(1)));
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void insertion_order_survives_removal_from_the_middle() {
        Diagnostics diagnostics = new Diagnostics();
        for (int line = 1; line <= 5; line++) {
            error(diagnostics, DiagnosticCategory.SYMBOL_NOT_FOUND, at(line));
        }

        assertEquals(1, diagnostics.removeMatching(DiagnosticCategory.SYMBOL_NOT_FOUND, at(3)));

        // Order is observable — CorpusDiagnosticGateTest's seed lines and every
        // diagnostic report read this list in emission order.
        assertEquals(List.of(1, 2, 4, 5),
                diagnostics.toList().stream().map(d -> d.range().startLine()).toList());
    }

    @Test
    void a_duplicated_key_gives_up_one_entry_per_call() {
        Diagnostics diagnostics = new Diagnostics();
        // The javadoc argues uniqueness per (category, range) holds in practice.
        // The CONTRACT is one-per-call regardless, and the index must not
        // assume the argument is true.
        error(diagnostics, DiagnosticCategory.SYMBOL_NOT_FOUND, at(1));
        error(diagnostics, DiagnosticCategory.SYMBOL_NOT_FOUND, at(1));

        assertEquals(1, diagnostics.removeMatching(DiagnosticCategory.SYMBOL_NOT_FOUND, at(1)));
        assertEquals(1, diagnostics.size());
        assertEquals(1, diagnostics.removeMatching(DiagnosticCategory.SYMBOL_NOT_FOUND, at(1)));
        assertTrue(diagnostics.isEmpty());
        assertEquals(0, diagnostics.removeMatching(DiagnosticCategory.SYMBOL_NOT_FOUND, at(1)));
    }

    @Test
    void a_key_re_added_after_removal_is_removable_again() {
        Diagnostics diagnostics = new Diagnostics();
        error(diagnostics, DiagnosticCategory.SYMBOL_NOT_FOUND, at(1));

        assertEquals(1, diagnostics.removeMatching(DiagnosticCategory.SYMBOL_NOT_FOUND, at(1)));
        assertEquals(0, diagnostics.removeMatching(DiagnosticCategory.SYMBOL_NOT_FOUND, at(1)));

        // A stale negative index would answer 0 here forever. Passes 4/5 and
        // the type engine interleave emission and clearing across fixed-point
        // iterations, so this ordering is live, not hypothetical.
        error(diagnostics, DiagnosticCategory.SYMBOL_NOT_FOUND, at(1));
        assertEquals(1, diagnostics.removeMatching(DiagnosticCategory.SYMBOL_NOT_FOUND, at(1)));
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void warnings_are_indexed_on_the_same_key_as_errors() {
        Diagnostics diagnostics = new Diagnostics();
        // Both emission points feed one list; an index wired to error() alone
        // would silently refuse to remove a warning.
        diagnostics.warning(DiagnosticCategory.SYMBOL_NOT_FOUND, at(1), "n", "m", List.of());

        assertEquals(1, diagnostics.removeMatching(DiagnosticCategory.SYMBOL_NOT_FOUND, at(1)));
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void the_none_sentinel_range_is_a_usable_key() {
        Diagnostics diagnostics = new Diagnostics();
        error(diagnostics, DiagnosticCategory.TYPE_NOT_FOUND, SourceRange.NONE);

        assertEquals(1, diagnostics.removeMatching(DiagnosticCategory.TYPE_NOT_FOUND, SourceRange.NONE));
        assertTrue(diagnostics.isEmpty());
    }
}
