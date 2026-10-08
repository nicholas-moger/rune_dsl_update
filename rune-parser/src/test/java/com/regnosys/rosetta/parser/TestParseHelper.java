package com.regnosys.rosetta.parser;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;

/**
 * Shared parse + AstBuilder wiring for P1.4.2 (and later) E2E tests. Package-private —
 * not part of the test-time public surface.
 *
 * <p>Extracted to reduce duplication across the H2/H3/H4/H11 attach-site tests
 * (each previously carried its own copy of the same helper). Delegates to
 * {@link AstBuilder#buildFromString(String, String)} — the non-deprecated entry
 * that wires byte-offset tracking via {@link com.regnosys.rosetta.ast.CharToByteOffsets}.
 *
 * <p>Note: {@link AstBuilder#buildFromString(String, String)} throws
 * {@link com.regnosys.rosetta.ast.builder.AstBuildException} on parse errors.
 * Tests asserting parse failure should call
 * {@link RosettaParserFacade#parseString(String)} directly instead.
 */
final class TestParseHelper {

    private TestParseHelper() {
        // utility class — no instances
    }

    /**
     * Parses a Rune DSL source snippet and runs the AstBuilder on the resulting
     * parse tree, returning the typed {@link RModel}.
     */
    static RModel parseModel(String src) {
        return AstBuilder.buildFromString(src, "inline-test");
    }
}
