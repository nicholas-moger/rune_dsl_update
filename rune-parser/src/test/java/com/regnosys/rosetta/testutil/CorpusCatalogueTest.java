package com.regnosys.rosetta.testutil;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Guards {@code test-corpus/corpus-cells.tsv} — the corpus cell source of truth —
 * and keeps the surfaces that restate its contents from drifting away from it.
 *
 * <p>The cell list previously existed as seven independent hand-maintained copies
 * with nothing keeping them in step. That was survivable only while the corpus was
 * frozen; it is not survivable now the corpus changes. These assertions are the
 * mechanical replacement for "remember to update the other six places".
 */
class CorpusCatalogueTest {

    @Test
    void sotParsesAndIsInternallyConsistent() {
        List<CorpusCatalogue.Cell> all = CorpusCatalogue.all();
        assertFalse(all.isEmpty(), "no cells parsed from " + CorpusCatalogue.tsvPath());

        // No duplicate (corpus, version) rows — a duplicate would make "the" cell
        // for a version ambiguous and silently pick whichever parsed first.
        Set<String> seen = new LinkedHashSet<>();
        List<String> dupes = new ArrayList<>();
        for (CorpusCatalogue.Cell c : all) {
            if (!seen.add(c.dirName())) {
                dupes.add(c.dirName());
            }
        }
        assertTrue(dupes.isEmpty(), "duplicate cell rows in the SOT: " + dupes);

        // Every catalogue cell is in the 9.83.0 band by definition of this corpus.
        // Resolution-only cells are exempt: their own pin is irrelevant because the
        // DEPENDENT cell's 9.83.0 toolchain compiles their sources.
        List<String> offBand = all.stream()
                .filter(CorpusCatalogue.Cell::catalogue)
                .filter(c -> !"9.83.0".equals(c.dsl()))
                .map(c -> c.dirName() + "@" + c.dsl())
                .toList();
        assertTrue(offBand.isEmpty(),
                "catalogue cells must pin rune-dsl 9.83.0; off-band rows: " + offBand);

        // Every dependency must itself be a row in the SOT — otherwise a closure
        // points at a version nothing knows how to materialise.
        List<String> unknown = new ArrayList<>();
        for (CorpusCatalogue.Cell c : all) {
            for (CorpusCatalogue.Dep d : c.resolved()) {
                if (CorpusCatalogue.find(d.corpus(), d.version()).isEmpty()) {
                    unknown.add(c.dirName() + " -> " + d.dirName());
                }
            }
        }
        assertTrue(unknown.isEmpty(),
                "dependency pins with no matching SOT row: " + unknown);
    }

    /**
     * An ACTIVE cell claims to be materialised, so it and its whole resolved
     * closure must actually be on disk. A cell loaded without its dependencies
     * produces thousands of unresolved-reference diagnostics that read as engine
     * defects — the exact failure the 2026-08-14 expansion hit.
     */
    @Test
    void activeCellsAndTheirClosuresArePresent() {
        assumeTrue(CorpusWalker.corpusExists(),
                "test-corpus/ absent (cloned-on-demand) — SOT presence check skipped");

        List<String> missingCells = new ArrayList<>();
        List<String> missingDeps = new ArrayList<>();
        for (CorpusCatalogue.Cell c : CorpusCatalogue.all()) {
            if (!c.active()) {
                continue;
            }
            if (!Files.isDirectory(CorpusCatalogue.cellDir(c))) {
                missingCells.add(c.relPath());
                continue;
            }
            // Check the TRANSITIVE closure via loadRoots, not just the direct pins:
            // drr 7.3.0 pins cdm 6.21.0, which itself pins rune-fpml 2.1.1, and a
            // one-level check would call that closure complete when it is not.
            for (Path root : CorpusCatalogue.loadRoots(c)) {
                if (!Files.isDirectory(root)) {
                    missingDeps.add(c.dirName() + " closure root absent: " + root);
                }
            }
        }
        assertTrue(missingCells.isEmpty(),
                "cells marked 'active' but absent on disk (materialise them, or mark them "
                        + "'planned'): " + missingCells);
        assertTrue(missingDeps.isEmpty(),
                "active cells whose resolved dependency closure is NOT on disk: " + missingDeps);
    }

    /**
     * {@code CorpusWalker.TRANSITIVE_DEP_CELL_DIRS} restates the active
     * resolution-only cells — the cells excluded from the catalogue census.
     */
    @Test
    void transitiveDepDirsMatchSot() {
        assertEquals(new TreeSet<>(CorpusCatalogue.activeResolutionDirNames()),
                new TreeSet<>(CorpusWalker.TRANSITIVE_DEP_CELL_DIRS),
                "CorpusWalker.TRANSITIVE_DEP_CELL_DIRS has drifted from "
                        + CorpusCatalogue.TSV_REL_PATH);
    }

    /**
     * Load roots must arrive in the D11 cell-loader order — builtins, then the
     * resolved closure, then the cell's own tree LAST. Callers depend on the last
     * entry being the cell's own root (it is what the corpus-absent assumption
     * checks), so the ordering is a contract, not a convenience.
     */
    @Test
    void loadRootsPutTheCellsOwnTreeLast() {
        for (CorpusCatalogue.Cell c : CorpusCatalogue.activeCatalogue()) {
            List<Path> roots = CorpusCatalogue.loadRoots(c);
            assertTrue(roots.size() >= 2 + c.resolved().size(),
                    "closure for " + c.dirName() + " is smaller than its direct pins");
            assertEquals(CorpusCatalogue.builtinsRoot(), roots.get(0),
                    "builtins must load first for " + c.dirName());
            assertEquals(CorpusCatalogue.cellDir(c), roots.get(roots.size() - 1),
                    "the cell's own tree must load last for " + c.dirName());
            // Every direct pin must appear in the closure.
            for (CorpusCatalogue.Dep d : c.resolved()) {
                assertTrue(roots.contains(CorpusCatalogue.depSourceRoot(d)),
                        c.dirName() + " closure is missing its direct pin " + d.dirName());
            }
        }
    }

    /**
     * A dependency must be loaded BEFORE anything that depends on it, and the
     * closure must reach dependencies-of-dependencies. drr 7.3.0 is the witness:
     * it pins cdm 6.21.0, which pins rune-fpml 2.1.1, so fpml must be present and
     * must precede cdm. A one-level closure left this cell with 3,913 linking errors.
     */
    @Test
    void closureIsTransitiveAndOrderedDepsFirst() {
        CorpusCatalogue.Cell drr = CorpusCatalogue.find("drr", "7.3.0").orElse(null);
        assumeTrue(drr != null && drr.active(), "drr 7.3.0 not an active cell");

        List<Path> roots = CorpusCatalogue.loadRoots(drr);
        Path fpml = CorpusCatalogue.depSourceRoot(new CorpusCatalogue.Dep("rune-fpml", "2.1.1"));
        Path cdm = CorpusCatalogue.depSourceRoot(new CorpusCatalogue.Dep("cdm", "6.21.0"));

        assertTrue(roots.contains(fpml),
                "drr 7.3.0's closure must reach rune-fpml 2.1.1 transitively via cdm 6.21.0");
        assertTrue(roots.indexOf(fpml) < roots.indexOf(cdm),
                "rune-fpml 2.1.1 must load before cdm 6.21.0, which depends on it");
    }
}
