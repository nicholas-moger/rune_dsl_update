package com.regnosys.rosetta.harness.matrix;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CorpusTest {

    @Test
    void dir_names_match_test_corpus_layout() {
        // Confirms the enum's dirName matches the on-disk test-corpus/ layout.
        // If the repository's corpus layout ever changes, this test fails with
        // a pointer to what needs updating — rather than matrix tests silently
        // seeing the wrong corpus directory.
        assertEquals("cdm", Corpus.CDM.dirName());
        assertEquals("drr", Corpus.DRR.dirName());
        assertEquals("iso20022", Corpus.ISO20022.dirName());
        assertEquals("rune-fpml", Corpus.RUNE_FPML.dirName());
        assertEquals("chaos", Corpus.CHAOS.dirName());
    }

    @Test
    void from_dir_name_resolves_known_names() {
        assertEquals(Corpus.CDM, Corpus.fromDirName("cdm"));
        assertEquals(Corpus.DRR, Corpus.fromDirName("drr"));
        assertEquals(Corpus.ISO20022, Corpus.fromDirName("iso20022"));
        assertEquals(Corpus.RUNE_FPML, Corpus.fromDirName("rune-fpml"));
        assertEquals(Corpus.CHAOS, Corpus.fromDirName("chaos"));
    }

    @Test
    void from_dir_name_returns_null_for_unknown() {
        assertNull(Corpus.fromDirName("unknown"));
        assertNull(Corpus.fromDirName(""));
        assertNull(Corpus.fromDirName("CDM")); // case-sensitive
    }
}
