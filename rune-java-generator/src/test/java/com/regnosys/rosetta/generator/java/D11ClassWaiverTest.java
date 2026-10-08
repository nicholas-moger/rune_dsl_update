package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.generator.java.D11CorpusRegressionTest.CellSpec;
import com.regnosys.rosetta.generator.java.D11CorpusRegressionTest.ElementKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * N2 (v3.1 phase C, C0 item 5) — the CLASS-KEYED waiver, and the shape of the waiver
 * population it leaves behind.
 *
 * <p>{@code ReferenceWithMetaVoid} is an upstream CDM defect: the generated class wraps
 * {@code java.lang.Void} (rune-dsl's mapping for an unresolvable type), no other
 * generated file references it, and no model type stands behind it. The fork resolves
 * the same model closure cleanly and correctly emits nothing — so the golden has a file
 * we do not produce, in EVERY cell whose CDM line carries it.
 *
 * <p>The 5-cell corpus made that look like a property of {@code cdm/6.20.6}; the 25-cell
 * band showed it in twelve cells (eight CDM plus the four DRR 7.x that inherit CDM 6),
 * exactly one per cell. Waiving by class states the real claim once, instead of minting
 * a line per cell — and, more importantly, instead of leaving eleven reds standing for a
 * cause already characterised on record.
 *
 * <p>What this test holds: the waiver reaches every cell, it does NOT leak across kinds,
 * and the waiver file still carries exactly ONE entry — the property CLAUDE.md's
 * number-grep law tracks, now class-keyed rather than cell-keyed.
 */
class D11ClassWaiverTest {

    private static final String VOID_WRAPPER =
            "com/rosetta/model/metafields/ReferenceWithMetaVoid.java";

    /** Cells drawn from both affected corpora, including one that was never waived before. */
    private static final List<CellSpec> WITNESSES = List.of(
            new CellSpec("cdm", "6.20.2", null),
            new CellSpec("cdm", "6.20.6", null),   // the originally-keyed cell
            new CellSpec("cdm", "6.23.0", null),
            new CellSpec("drr", "7.0.0", null),
            new CellSpec("drr", "7.3.0", null));

    @Test
    void theVoidWrapperIsWaivedInEveryAffectedCell() {
        for (CellSpec cell : WITNESSES) {
            Set<String> waivers = D11CorpusRegressionTest.waiversFor(cell, ElementKind.METAFIELD);
            assertTrue(waivers.contains(VOID_WRAPPER),
                    cell + " METAFIELD: the class-keyed ReferenceWithMetaVoid waiver must be in "
                            + "force. Its cause is upstream's and cell-independent; a per-cell key "
                            + "left eleven reds standing across the band.");
        }
    }

    @Test
    void theClassWaiverDoesNotLeakAcrossKinds() {
        for (CellSpec cell : WITNESSES) {
            for (ElementKind kind : ElementKind.values()) {
                if (kind == ElementKind.METAFIELD) {
                    continue;
                }
                assertTrue(D11CorpusRegressionTest.waiversFor(cell, kind).isEmpty(),
                        cell + " " + kind + ": expected no waivers. A class key wildcards the "
                                + "corpus and version only — never the kind, and never the path.");
            }
        }
    }

    /** The waiver file is the SOT for the published "waiver N entries" figure. */
    @Test
    void theWaiverPopulationIsExactlyOneEntry() {
        Set<String> metafield = D11CorpusRegressionTest.waiversFor(
                new CellSpec("cdm", "6.20.6", null), ElementKind.METAFIELD);
        assumeTrue(!metafield.isEmpty(), "waiver resource not loaded");
        assertEquals(Set.of(VOID_WRAPPER), metafield,
                "the band's entire divergence waiver population is this one upstream defect; "
                        + "any addition needs its own written root cause in the resource file.");
    }
}
