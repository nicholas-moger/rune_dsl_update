package com.regnosys.rosetta.symbols.carryover;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.external.RExternalSynonymSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M3-owned carryover test that verifies the per-entry source ranges
 * stored by {@code AstBuilder.populateSourceNames}. Lives in the M3
 * test tree rather than in the M2 {@code ExternalSourceNodeTest}
 * because D3 forbids M3 from modifying any M2 test source file.
 *
 * <p>Spec: D3 test discipline + D5/E2 M2 carryover audit in
 * {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
class AstBuilderSourceRangesTest {

    @Test
    void external_synonym_source_extends_captures_per_entry_token_ranges() {
        String source = """
                namespace test
                synonym source FpML extends DefaultSource, FallbackSource {}
                """;
        RModel model = AstBuilder.buildFromString(source, "AstBuilderSourceRangesTest.rosetta");
        RExternalSynonymSource ess = (RExternalSynonymSource) model.rootElements().get(0);

        assertEquals(2, ess.superSourceNames().size());
        assertEquals("DefaultSource", ess.superSourceNames().get(0));
        assertEquals("FallbackSource", ess.superSourceNames().get(1));

        // The new contract: per-entry token ranges keyed as superSource_0, superSource_1
        SourceRange range0 = ess.tokenRanges().get("superSource_0");
        SourceRange range1 = ess.tokenRanges().get("superSource_1");

        assertNotNull(range0, "superSource_0 token range must be set");
        assertNotNull(range1, "superSource_1 token range must be set");
        assertNotEquals(SourceRange.NONE, range0);
        assertNotEquals(SourceRange.NONE, range1);

        // Range 0 should cover "DefaultSource"; range 1 should cover "FallbackSource"
        assertTrue(range0.startCol() < range1.startCol(),
            "second source name should appear after the first");
    }
}
