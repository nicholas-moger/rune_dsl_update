package com.regnosys.rosetta.maven;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the upstream 9.83.0 {@code generators.namespaces} semantics
 * (vendored {@code com.regnosys.rosetta.config.file.NamespaceFilter}):
 * generic {@code x.*} patterns match segment-wise prefixes (including the bare
 * parent), specific patterns match exactly, and an empty list matches all.
 */
class NamespaceFilterTest {

    @Test
    void genericPatternMatchesSegmentwisePrefix() {
        NamespaceFilter filter = new NamespaceFilter(List.of("cdm.*"));
        assertTrue(filter.test("cdm"), "the bare parent matches (DottedPath.startsWith self)");
        assertTrue(filter.test("cdm.base"));
        assertTrue(filter.test("cdm.base.staticdata.asset.common"));
        assertFalse(filter.test("cdmx"), "raw string prefix must NOT match — segments only");
        assertFalse(filter.test("cdmx.foo"));
        assertFalse(filter.test("drr.regulation"));
    }

    @Test
    void specificPatternMatchesExactlyOnly() {
        NamespaceFilter filter = new NamespaceFilter(List.of("com.rosetta.model"));
        assertTrue(filter.test("com.rosetta.model"));
        assertFalse(filter.test("com.rosetta.model.metafields"),
                "a specific pattern is exact equality, not a prefix");
        assertFalse(filter.test("com.rosetta"));
    }

    @Test
    void emptyPatternListMatchesEverything() {
        NamespaceFilter filter = new NamespaceFilter(List.of());
        assertTrue(filter.test("anything.at.all"));
        assertTrue(filter.test("cdm"));
    }

    @Test
    void nullNamespaceMatchesNothingUnderNonEmptyPatterns() {
        assertFalse(new NamespaceFilter(List.of("cdm.*")).test(null));
        assertFalse(new NamespaceFilter(List.of("com.rosetta.model")).test(null));
        assertTrue(new NamespaceFilter(List.of()).test(null),
                "the empty list short-circuits true before the namespace is read, as upstream does");
    }

    @Test
    void theMeasuredConsumerLists() {
        // CDM 6.20.6 rosetta-config.yml: [cdm.*, com.rosetta.model]
        NamespaceFilter cdm = new NamespaceFilter(List.of("cdm.*", "com.rosetta.model"));
        assertTrue(cdm.test("cdm.event.common"));
        assertTrue(cdm.test("com.rosetta.model"));
        assertFalse(cdm.test("fpml.consolidated.recordkeeping"),
                "the fpml dependency models must not emit in the cdm cell");
        // DRR 6.34.1 rosetta-config.yml: [drr.*, com.rosetta.model]
        NamespaceFilter drr = new NamespaceFilter(List.of("drr.*", "com.rosetta.model"));
        assertTrue(drr.test("drr.regulation.common"));
        assertFalse(drr.test("cdm.event.common"),
                "the cdm dependency models must not emit in the drr cell");
        assertFalse(drr.test("iso20022.auth108.esma"));
    }

    @Test
    void blankPatternRejected() {
        assertThrows(IllegalArgumentException.class, () -> new NamespaceFilter(List.of("")));
    }
}
