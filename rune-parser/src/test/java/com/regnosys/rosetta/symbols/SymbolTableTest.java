package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.types.RDataType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SymbolTableTest extends BaseSymbolsTest {

    @Test
    void single_file_with_one_type_registers_into_namespace() {
        RLinkingResult result = parseAndLink("""
                namespace test
                type Foo:
                    field string (1..1)
                """);
        RWorkspace ws = result.workspace();

        Optional<RNamespaceScope> ns = ws.namespace("test");
        assertTrue(ns.isPresent(), "namespace 'test' must be registered");
        assertEquals("test", ns.get().qualifiedName());

        Optional<RRootElement> foo = ns.get().lookup("Foo");
        assertTrue(foo.isPresent(), "type 'Foo' must be in scope 'test'");
        assertInstanceOf(RDataType.class, foo.get());
    }

    @Test
    void multiple_top_level_declarations_all_register() {
        RLinkingResult result = parseAndLink("""
                namespace test
                type Foo:
                    a string (1..1)
                type Bar:
                    b int (1..1)
                enum Baz:
                    A
                    B
                """);
        RNamespaceScope ns = result.workspace().namespace("test").orElseThrow();
        assertTrue(ns.lookup("Foo").isPresent());
        assertTrue(ns.lookup("Bar").isPresent());
        assertTrue(ns.lookup("Baz").isPresent());
    }

    @Test
    void duplicate_declarations_are_preserved_in_multimap() {
        RLinkingResult result = parseAndLink("""
                namespace test
                type Foo:
                    a string (1..1)
                type Foo:
                    b int (1..1)
                """);
        RNamespaceScope ns = result.workspace().namespace("test").orElseThrow();

        Optional<RRootElement> first = ns.lookup("Foo");
        assertTrue(first.isPresent());

        List<RRootElement> all = ns.allMatching("Foo");
        assertEquals(2, all.size(),
            "duplicate Foo must be preserved for M6 names-are-unique check");
    }

    @Test
    void multiple_files_in_same_namespace_merge_into_one_scope() {
        RLinkingResult result = parseAndLink(
            "namespace test\ntype Foo:\n    a string (1..1)\n",
            "namespace test\ntype Bar:\n    b int (1..1)\n");
        RNamespaceScope ns = result.workspace().namespace("test").orElseThrow();
        assertTrue(ns.lookup("Foo").isPresent());
        assertTrue(ns.lookup("Bar").isPresent());
    }

    @Test
    void unknown_namespace_returns_empty_optional() {
        RLinkingResult result = parseAndLink("namespace test\ntype Foo:\n");
        assertTrue(result.workspace().namespace("nonexistent").isEmpty());
        assertTrue(result.workspace().namespace("test").get().lookup("Bar").isEmpty());
    }
}
