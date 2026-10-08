package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.types.RDataType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TraceabilityApiTest extends BaseSymbolsTest {

    @Test
    void getSubTypes_returns_direct_subtypes_only() {
        RLinkingResult result = parseAndLink("""
                namespace test
                type Animal:
                type Dog extends Animal:
                type Cat extends Animal:
                type Puppy extends Dog:
                """);
        RDataType animal = findType(result.workspace().files().get(0), "Animal");
        assertEquals(2, result.workspace().getSubTypes(animal).size(),
            "getSubTypes returns DIRECT subtypes only (Dog, Cat) — not Puppy");
    }

    @Test
    void getTypeHierarchy_walks_full_super_chain() {
        RLinkingResult result = parseAndLink("""
                namespace test
                type Animal:
                type Dog extends Animal:
                type Puppy extends Dog:
                """);
        RDataType puppy = findType(result.workspace().files().get(0), "Puppy");
        List<RDataType> chain = result.workspace().getTypeHierarchy(puppy);
        assertEquals(2, chain.size());
        assertEquals("Dog", chain.get(0).name());
        assertEquals("Animal", chain.get(1).name());
    }

    @Test
    void findReferences_across_files() {
        RLinkingResult result = parseAndLink(
            "namespace cdm.foo\ntype Foo:\n",
            "namespace test1\nimport cdm.foo.*\ntype A extends Foo:\n",
            "namespace test2\nimport cdm.foo.*\ntype B extends Foo:\n");

        RModel cdmFoo = result.workspace().files().stream()
            .filter(m -> "cdm.foo".equals(m.namespace())).findFirst().orElseThrow();
        RDataType foo = findType(cdmFoo, "Foo");
        assertEquals(2, result.workspace().findReferences(foo).size());
    }

    @Test
    void getDependencies_returns_imported_files() {
        RLinkingResult result = parseAndLink(
            "namespace cdm.foo\ntype Foo:\n",
            "namespace test\nimport cdm.foo.*\ntype Bar extends Foo:\n");

        RModel testFile = result.workspace().files().stream()
            .filter(m -> "test".equals(m.namespace())).findFirst().orElseThrow();
        assertEquals(1, result.workspace().getDependencies(testFile).size());
    }

    @Test
    void getDependents_returns_importing_files() {
        RLinkingResult result = parseAndLink(
            "namespace cdm.foo\ntype Foo:\n",
            "namespace test1\nimport cdm.foo.*\ntype A:\n",
            "namespace test2\nimport cdm.foo.*\ntype B:\n");

        RModel cdmFoo = result.workspace().files().stream()
            .filter(m -> "cdm.foo".equals(m.namespace())).findFirst().orElseThrow();
        assertEquals(2, result.workspace().getDependents(cdmFoo).size());
    }

    @Test
    void findByName_exact_match() {
        RLinkingResult result = parseAndLink("""
                namespace cdm.product
                type SwapProduct:
                type FxProduct:
                """);
        List<RNode> hits = result.workspace().findByName("SwapProduct");
        assertEquals(1, hits.size());
        assertInstanceOf(RDataType.class, hits.get(0));
    }

    @Test
    void findByName_fuzzy_match() {
        RLinkingResult result = parseAndLink("""
                namespace cdm.product
                type SwapProduct:
                type FxProduct:
                type EquityProduct:
                """);
        List<RNode> hits = result.workspace().findByName("SwapProdct"); // typo
        assertFalse(hits.isEmpty(), "fuzzy search should find SwapProduct");
    }
}
