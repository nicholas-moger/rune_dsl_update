package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.symbols.linker.ResolutionAudit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResolutionCompletenessTest extends BaseSymbolsTest {

    @Test
    void clean_input_passes_audit() {
        RLinkingResult result = parseAndLink("""
                namespace test
                type Animal:
                    name string (1..1)
                type Dog extends Animal:
                    breed string (1..1)
                """);
        ResolutionAudit.assertFullyHandled(result.workspace());
    }

    @Test
    void unresolved_input_passes_audit_via_diagnostic() {
        RLinkingResult result = parseAndLink("""
                namespace test
                type Dog extends MissingAnimal:
                """);
        assertFalse(result.linkingDiagnostics().isEmpty(),
            "should have an unresolved super type diagnostic");
        // Audit must STILL pass — the unresolved field is covered by the diagnostic
        ResolutionAudit.assertFullyHandled(result.workspace());
    }
}
