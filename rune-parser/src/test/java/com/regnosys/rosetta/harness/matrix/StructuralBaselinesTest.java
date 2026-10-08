package com.regnosys.rosetta.harness.matrix;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class StructuralBaselinesTest {

    @Test
    void default_resource_loads_every_catalogue_cell() {
        StructuralBaselines b = StructuralBaselines.fromDefaultResource();
        // PR #85 rebaselined onto 5 cells (2 CDM + 1 DRR + 1 ISO-20022 + 1 rune-fpml).
        // v3.1 phase A widened the corpus to the whole 9.83.0 band and REGENERATED this
        // resource for all 25 catalogue cells; the assertion stayed at 5 and had been red
        // since. Fixed in C0 with the count derived from the corpus SOT rather than
        // restated, so the next cell to join the band cannot leave it stale again.
        assertEquals(com.regnosys.rosetta.testutil.CorpusCatalogue.activeCatalogue().size(),
                b.cellCount(),
                "structural-baselines.properties must cover every ACTIVE catalogue cell in "
                        + "test-corpus/corpus-cells.tsv — regenerate with "
                        + "-Dtest=StructuralBaselineDumper -Dstructural.dump=true");
    }

    @Test
    void cdm_6_20_6_anchor_baseline() {
        // CDM 6.20.6 is the latest-minor 6.x cell at 9.83.0 (PR #85). Numbers
        // pinned from the regenerated dumper run; a silent regeneration that
        // drifts any of these would surface here rather than hiding in the
        // per-cell fingerprint table.
        StructuralBaselines b = StructuralBaselines.fromDefaultResource();
        Map<String, Integer> cdm = b.forCell(Corpus.CDM, Version.parse("6.20.6"));
        assertEquals(719, cdm.get("type"));
        assertEquals(262, cdm.get("enum"));
        assertEquals(1311, cdm.get("func"));
        // Negative-regression gate: these should remain zero.
        assertEquals(0, cdm.get("basicType"));
        assertEquals(0, cdm.get("recordType"));
        assertEquals(0, cdm.get("libraryFunction"));
    }

    @Test
    void drr_6_34_1_anchor_baseline() {
        // DRR 6.34.1 is the latest 6.x cell at 9.83.0 (PR #85). Numbers pinned
        // from the regenerated dumper run — DRR pulls its transitive CDM
        // .rosetta via target/parent-dependency/ which the parser loader
        // walks alongside the cell's own sources.
        StructuralBaselines b = StructuralBaselines.fromDefaultResource();
        Map<String, Integer> drr = b.forCell(Corpus.DRR, Version.parse("6.34.1"));
        assertEquals(159, drr.get("type"));
        assertEquals(102, drr.get("enum"));
        assertEquals(1244, drr.get("func"));
        assertEquals(2317, drr.get("rule"));
        assertEquals(23, drr.get("report"));
        // DRR-signature kind: externalRuleSource is non-zero only on DRR cells
        // (CDM/ISO/rune-fpml all have externalRuleSource=0). Anchor the count
        // so a regression in external-rule-source parsing fails loud.
        assertEquals(6, drr.get("externalRuleSource"));
    }

    @Test
    void unknown_cell_returns_empty_map() {
        StructuralBaselines b = StructuralBaselines.fromDefaultResource();
        // Version doesn't exist in the CATALOGUE, so no baseline is defined.
        Map<String, Integer> missing = b.forCell(Corpus.CDM, Version.parse("99.99.99"));
        assertTrue(missing.isEmpty(),
                "unknown (corpus, version) must return an empty map — "
                        + "StructuralComparisonTest treats this as a hard failure, not a skip");
    }

    @Test
    void all_5_cells_present_by_name() {
        StructuralBaselines b = StructuralBaselines.fromDefaultResource();
        // Spot-check each kept cell at the 9.83.0 rebaseline.
        assertFalse(b.forCell(Corpus.CDM, Version.parse("5.38.0")).isEmpty());
        assertFalse(b.forCell(Corpus.CDM, Version.parse("6.20.6")).isEmpty());
        assertFalse(b.forCell(Corpus.DRR, Version.parse("6.34.1")).isEmpty());
        assertFalse(b.forCell(Corpus.ISO20022, Version.parse("1.38.0")).isEmpty());
        assertFalse(b.forCell(Corpus.RUNE_FPML, Version.parse("2.0.0")).isEmpty());
    }

    @Test
    void malformed_keys_are_silently_ignored() {
        Properties p = new Properties();
        p.setProperty("cdm-6.16.0.type", "719");      // valid
        p.setProperty("no-dot-in-key", "99");          // malformed: no dot
        p.setProperty("cdm-6.16.0.", "10");            // malformed: trailing dot
        // `lastIndexOf('.')` splits this into cell=`cdm-6.16.0.type` and
        // kind=`nested`. It parses cleanly, but it lands in a *different*
        // cell, not cdm-6.16.0. For the documented `<cell>.<kind>` format
        // this key is malformed-for-our-purposes — the assertions below
        // prove it does not leak into cdm-6.16.0's entries.
        p.setProperty("cdm-6.16.0.type.nested", "5"); // malformed for documented <cell>.<kind> format
        StructuralBaselines b = StructuralBaselines.fromProperties(p);
        Map<String, Integer> cell = b.forCell(Corpus.CDM, Version.parse("6.16.0"));
        assertEquals(719, cell.get("type"));
        // No entries leaked from malformed keys into the cdm-6.16.0 cell.
        assertEquals(1, cell.size(),
                "cdm-6.16.0 must contain only the one valid `type` entry");
        assertNull(cell.get(""));
        assertNull(cell.get("nested"));
    }

    @Test
    void non_integer_values_are_silently_ignored() {
        Properties p = new Properties();
        p.setProperty("cdm-6.16.0.type", "not-a-number");
        p.setProperty("cdm-6.16.0.enum", "262");
        StructuralBaselines b = StructuralBaselines.fromProperties(p);
        Map<String, Integer> cell = b.forCell(Corpus.CDM, Version.parse("6.16.0"));
        assertNull(cell.get("type"),
                "non-integer values must be dropped, not stored");
        assertEquals(262, cell.get("enum"));
    }
}
