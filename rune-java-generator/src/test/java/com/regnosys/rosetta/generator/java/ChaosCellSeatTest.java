package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.testutil.ChaosCell;
import com.regnosys.rosetta.testutil.ChaosDeclaredRows;
import com.regnosys.rosetta.testutil.CorpusCells;

/**
 * v3.2 SEAT 10 (PR #631, D49 — the generation-2 chaos cell) — THE SEAT SUITE, generator side: the
 * witnesses of the one-read helper {@link ChaosCell} the {@code chaos-1.0.0 → chaos-1.1.0} swap
 * introduced (seven seat suites had carried the cell path as a literal each).
 *
 * <ul>
 *   <li><b>b1–b3 the one-row law</b> over synthetic catalogues ({@code rowOf}): zero or two chaos rows
 *       refuse loudly, one is returned with its derived paths.</li>
 *   <li><b>b4 the committed catalogue</b>: the live row's label is the key prefix of EVERY row of the
 *       declared-divergence baseline ({@code chaos/<version>/<KIND>:<path>}) — the declared sets are
 *       keyed by the SOT's version, so a version cut that forgets the baseline fails here, not in
 *       the D11's silence.</li>
 * </ul>
 *
 * <p>The lane record (target/v32-seat10-instruments/lanes-s10.py, local): the one-row law
 * short-circuited → b1, b2 alone.
 */
class ChaosCellSeatTest {

    private static CorpusCells.Row row(String corpus, String version) {
        return new CorpusCells.Row(corpus, version, true, true, Map.of());
    }

    @Test
    void b1_rowOf_refusesZeroChaosRows() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ChaosCell.rowOf(List.of(row("cdm", "6.20.6"), row("drr", "7.3.0"))));
        assertTrue(e.getMessage().contains("found 0"), e.getMessage());
    }

    @Test
    void b2_rowOf_refusesTwoChaosRows() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ChaosCell.rowOf(List.of(row("chaos", "1.0.0"), row("cdm", "6.20.6"), row("chaos", "1.1.0"))));
        assertTrue(e.getMessage().contains("found 2"), e.getMessage());
    }

    @Test
    void b3_rowOf_returnsTheOneChaosRow() {
        CorpusCells.Row r = ChaosCell.rowOf(List.of(row("cdm", "6.20.6"), row("chaos", "1.1.0"), row("drr", "7.3.0")));
        assertEquals("chaos-1.1.0", r.dirName());
        assertEquals("chaos/chaos-1.1.0", r.relPath());
    }

    @Test
    void b4_theCommittedRow_keysEveryDeclaredDivergenceRow() {
        CorpusCells.Row r = ChaosCell.row();
        assertEquals("chaos", r.corpus());
        assertEquals("chaos/" + r.version(), ChaosCell.label());
        assertEquals(Path.of("..", "test-corpus").resolve("chaos").resolve("chaos-" + r.version()), ChaosCell.root());
        assertEquals(ChaosCell.root().resolve("rosetta-source").resolve("src").resolve("generated").resolve("java"),
                ChaosCell.goldens());
        assertEquals(ChaosCell.root().resolve("rosetta-source").resolve("src").resolve("main").resolve("rosetta"),
                ChaosCell.sources());
        // The declared-divergence baseline (the D11's chaos waiver file) is keyed <label>/<KIND>:<path>:
        // every row must carry the LIVE label, or the D11 tolerates nothing and refuses loudly (read through
        // the seat suites' ONE reader, ChaosDeclaredRows — round 1).
        List<String> rows = ChaosDeclaredRows.rows();
        for (String line : rows) {
            assertTrue(line.startsWith(ChaosCell.label() + "/"),
                    "a baseline row keyed by a version that is not the catalogue's: " + line);
        }
        assertTrue(!rows.isEmpty(), "the baseline holds rows (the gen-2 census's declared set)");
        assertEquals(rows.size(), ChaosDeclaredRows.declaredPaths().size(),
                "every declared row names a distinct golden-relative path");
    }
}
