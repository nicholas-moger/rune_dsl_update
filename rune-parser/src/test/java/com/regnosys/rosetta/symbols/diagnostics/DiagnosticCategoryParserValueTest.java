package com.regnosys.rosetta.symbols.diagnostics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DiagnosticCategoryParserValueTest {

    @Test
    void parserValuePresent() {
        assertEquals("PARSER", DiagnosticCategory.PARSER.name());
    }

    @Test
    void parserIsLastInEnumOrder() {
        DiagnosticCategory[] values = DiagnosticCategory.values();
        assertEquals(DiagnosticCategory.PARSER, values[values.length - 1],
            "PARSER must be appended at the end to preserve ordinals of pre-existing values "
            + "(M3/M4 enum entries have stable ordinals consumed by serializer/snapshots).");
    }
}
