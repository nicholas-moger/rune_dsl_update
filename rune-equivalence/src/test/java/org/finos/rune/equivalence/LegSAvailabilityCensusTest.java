package org.finos.rune.equivalence;

import org.finos.rune.benchmarks.corpus.CorpusClasses;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Leg S's availability census over all five cells (corpus-gated; skips with a
 * recorded reason on corpus-less checkouts). The printed map is the equiv
 * receipt's availability surface; the frozen-corpus anchor counts verified at
 * the census (research/p3-rosetta-common-census.md § 7) are pinned exactly —
 * the corpus is manifest-frozen, so drift here is loud by design.
 */
class LegSAvailabilityCensusTest {

    private static final List<String> CELLS = List.of(
            "cdm/cdm-5.38.0", "cdm/cdm-6.20.6", "drr/drr-6.34.1",
            "iso20022/iso20022-1.38.0", "rune-fpml/rune-fpml-2.0.0");

    @Test
    void availabilityMapPrintsEveryCellAndRoot() {
        assumeTrue(CorpusClasses.corpusPresent(), "test-corpus/ absent — local-only census");
        for (String cell : CELLS) {
            List<AvailabilityMap.RootCensus> census = AvailabilityMap.census(cell);
            assertEquals(AvailabilityMap.SAMPLE_ROOTS.size(), census.size(),
                    cell + ": every known root must be censused (absent ⇒ recorded)");
            AvailabilityMap.print(cell, System.out);
        }
    }

    @Test
    void frozenCorpusAnchorCountsHold() {
        assumeTrue(CorpusClasses.corpusPresent(), "test-corpus/ absent — local-only census");

        assertEquals(885, jsonCount("cdm/cdm-5.38.0", "result-json-files"),
                "cdm5 result-json-files: the census-verified CDM-instance JSON anchor");
        assertEquals(974, jsonCount("cdm/cdm-6.20.6", "result-json-files"),
                "cdm6 result-json-files: the census-verified CDM-instance JSON anchor");

        AvailabilityMap.RootCensus drrSamples = root("drr/drr-6.34.1", "cdm-sample-files");
        assertEquals(205, drrSamples.countsByExtension().getOrDefault("xml", 0),
                "drr record-keeping FpML XML inputs — loadable only once an XML ingestion"
                        + " route is wired (RECORDED absence for XML leg-S loading)");
        assertEquals(15, drrSamples.countsByExtension().getOrDefault("json", 0),
                "drr expectations.json files (per-set expectation records, not instances)");
        // The hand census initially read the DRR cell as JSON-sample-free; the map's
        // first instrument run corrected it — the cell DOES carry result-json-files
        // (231 instance JSONs). The instrument is the SOT; the pin keeps it loud.
        assertEquals(231, jsonCount("drr/drr-6.34.1", "result-json-files"),
                "drr result-json-files: the map-verified instance-JSON anchor");
    }

    private static AvailabilityMap.RootCensus root(String cell, String rootLeaf) {
        return AvailabilityMap.census(cell).stream()
                .filter(c -> c.root().endsWith(rootLeaf))
                .findFirst().orElseThrow();
    }

    private static int jsonCount(String cell, String rootLeaf) {
        Map<String, Integer> counts = root(cell, rootLeaf).countsByExtension();
        return counts == null ? -1 : counts.getOrDefault("json", 0);
    }
}
