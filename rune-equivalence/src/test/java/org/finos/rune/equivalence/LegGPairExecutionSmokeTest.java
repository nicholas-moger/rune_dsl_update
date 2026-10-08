package org.finos.rune.equivalence;

import org.finos.rune.benchmarks.corpus.CorpusClasses;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The leg-G pair-execution smoke over ALL FIVE cells (corpus-gated): each
 * cell's tree is compiled once (the CorpusClasses marker fast-path shares the
 * PR-2 compile cache), loaded in TWO separate classloaders over the SAME fork
 * runtime, and every XMeta-usable type's bounded seat lattice is built on
 * both sides and compared — O1 structural, O2 validation outcomes (incl.
 * failure-message text + path), O3 thrown envelopes, O5 the API-delta
 * report, plus the resolved/unresolved reference arms.
 *
 * <p>At PR-3 both sides load the SAME compiled goldens, so ZERO divergence is
 * asserted on every counter — the run proves the pair machinery and prints
 * the census (the receipt). From PR-4 the optimised side's dir diverges and
 * the same zero-divergence assertion becomes the differential gate.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LegGPairExecutionSmokeTest {

    private static final int FLIP_WIDTH_CAP = 4;
    private static final int POPULATE_DEPTH = 2;
    private static final int DETAIL_CAP = 25;

    @Test
    @Order(1)
    void cdm5PairExecutesWithZeroDivergence() throws Exception {
        assumeTrue(CorpusClasses.corpusPresent(), "test-corpus/ absent — local-only smoke");
        runCell("cdm/cdm-5.38.0", List.of(), null);
    }

    @Test
    @Order(2)
    void cdm6PairExecutesWithZeroDivergence() throws Exception {
        assumeTrue(CorpusClasses.corpusPresent(), "test-corpus/ absent — local-only smoke");
        // WHOLE since PR #607 — against rune-fpml 1.5.3's own-toolchain goldens (the
        // cell's SOT closure); the cdm/ingest/ exclusion of PR-2..#606 (the
        // fpml-artifact closure gap, survey § 6) is retired.
        runCell("cdm/cdm-6.20.6", CorpusClasses.ensureClosureCompiled("cdm/cdm-6.20.6"), null);
    }

    @Test
    @Order(3)
    void drrCompositePairExecutesWithZeroDivergence() throws Exception {
        assumeTrue(CorpusClasses.corpusPresent(), "test-corpus/ absent — local-only smoke");
        // The SOT closure = cdm 5.38.0 + iso20022 1.38.0 (the one recorded substitution).
        runCell("drr/drr-6.34.1", CorpusClasses.ensureClosureCompiled("drr/drr-6.34.1"), null);
    }

    @Test
    @Order(4)
    void iso20022PairExecutesWithZeroDivergence() throws Exception {
        assumeTrue(CorpusClasses.corpusPresent(), "test-corpus/ absent — local-only smoke");
        runCell("iso20022/iso20022-1.38.0", List.of(), null);
    }

    @Test
    @Order(5)
    void runeFpmlPairExecutesWithZeroDivergence() throws Exception {
        assumeTrue(CorpusClasses.corpusPresent(), "test-corpus/ absent — local-only smoke");
        runCell("rune-fpml/rune-fpml-2.0.0", List.of(), null);
    }

    private static void runCell(String cell, List<Path> upstream, String exclude)
            throws Exception {
        Path out = CorpusClasses.ensureCompiled(cell, upstream, exclude);
        List<Path> dirs = new java.util.ArrayList<>();
        dirs.add(out);
        dirs.addAll(upstream);
        PairHarness.Side side = new PairHarness.Side(dirs);
        PairHarness.Summary s = PairHarness.run(
                new PairHarness.Config(cell, side, side, FLIP_WIDTH_CAP, POPULATE_DEPTH,
                        DETAIL_CAP),
                System.out);
        assertTrue(s.usableTypes > 0, cell + ": zero usable types — census above");
        assertEquals(0, s.totalDivergences(),
                cell + ": ZERO divergence expected (ref≡ref at PR-3) — detail lines above");
        assertEquals(0, s.oneSidedBuilds, cell + ": no one-sided builds");
        // A MACHINERY check, not a gate: O5 is report-only (revised requirement 1),
        // but with both sides loading the SAME tree a nonzero delta can only mean
        // the report machinery itself is broken. Family PRs publish the report
        // instead of asserting zero.
        assertEquals(0, s.apiDelta.classesWithDelta(),
                cell + ": the O5 report must read zero at PR-3 (same compiled tree — a"
                        + " machinery self-check, not the gate)");
        // The § 6.3 protected channel (the program plan § 7): ref≡ref must read
        // zero on it too, ALWAYS — same tree, so any delta is broken machinery.
        // The ref-vs-opt zero-proof is a published receipt, never an assert.
        assertEquals(0, s.apiDeltaProtected.classesWithDelta(),
                cell + ": the O5p protected channel must read zero ref≡ref (same"
                        + " compiled tree — the channel's own machinery self-check)");
    }
}
