package com.regnosys.rosetta.symbols;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LinkerInvariantsTest extends BaseSymbolsTest {

    @Test
    void invariants_pass_on_clean_input() {
        RLinkingResult result = parseAndLink("""
                namespace test
                type Animal:
                    name string (1..1)
                type Dog extends Animal:
                    breed string (1..1)
                type Cat extends Animal:
                    indoor boolean (1..1)
                """);
        List<String> violations = LinkerInvariants.checkAll(result.workspace());
        assertTrue(violations.isEmpty(),
            "linker invariants must pass on clean input. Violations: " + violations);
    }

    @Test
    void invariants_pass_on_input_with_diagnostics() {
        RLinkingResult result = parseAndLink("""
                namespace test
                type Dog extends MissingAnimal:
                """);
        List<String> violations = LinkerInvariants.checkAll(result.workspace());
        assertTrue(violations.isEmpty(),
            "invariants must hold even with diagnostics. Violations: " + violations);
    }
}
