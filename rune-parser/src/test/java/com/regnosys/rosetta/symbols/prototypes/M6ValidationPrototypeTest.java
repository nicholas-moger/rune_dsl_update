package com.regnosys.rosetta.symbols.prototypes;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.symbols.BaseSymbolsTest;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RNamespaceScope;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Forward-compat prototype for M6 (validation).
 */
class M6ValidationPrototypeTest extends BaseSymbolsTest {

    @Test
    void m6_can_iterate_namespace_for_names_are_unique() {
        RLinkingResult result = parseAndLink("""
                namespace test
                type Foo:
                type Bar:
                type Foo:
                """);
        RNamespaceScope ns = result.workspace().namespace("test").orElseThrow();
        List<RRootElement> all = ns.allMatching("Foo");
        assertEquals(2, all.size(), "M6 must see both Foo declarations");
    }

    @Test
    void m6_can_walk_super_function_chain() {
        RLinkingResult result = parseAndLink("""
                namespace test
                func Base:
                    output: x int (1..1)
                func Override extends Base:
                    output: x int (1..1)
                """);
        RFunction override = findFunction(result.workspace().files().get(0), "Override");
        assertTrue(override.superFunction().isPresent());
        assertEquals("Base", override.superFunction().get().name());
    }

    @Test
    void m6_sees_circular_inheritance_from_m3() {
        RLinkingResult result = parseAndLink("""
                namespace test
                type A extends B:
                type B extends A:
                """);
        assertTrue(result.linkingDiagnostics().stream()
            .anyMatch(d -> d.category() == DiagnosticCategory.CIRCULAR_INHERITANCE));
    }
}
