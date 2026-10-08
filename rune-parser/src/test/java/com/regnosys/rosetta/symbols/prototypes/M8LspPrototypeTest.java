package com.regnosys.rosetta.symbols.prototypes;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.symbols.BaseSymbolsTest;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Forward-compat prototype for M8 (LSP).
 */
class M8LspPrototypeTest extends BaseSymbolsTest {

    @Test
    void m8_go_to_definition() {
        var result = parseAndLink("""
                namespace test
                type Foo:
                type Bar extends Foo:
                """);
        RDataType bar = findType(result.workspace().files().get(0), "Bar");
        assertTrue(bar.superType().isPresent());
        assertNotEquals(SourceRange.NONE, bar.superType().get().sourceRange());
    }

    @Test
    void m8_find_references() {
        RModel m1 = parseModel("namespace test\ntype Foo:\n");
        RModel m2 = parseModel("namespace test\ntype Bar extends Foo:\ntype Baz extends Foo:\n");
        RWorkspace ws = RWorkspace.build(List.of(m1, m2)).workspace();

        RDataType foo = findType(m1, "Foo");
        List<RNode> refs = ws.findReferences(foo);
        assertEquals(2, refs.size(), "M8 find-references must see Bar and Baz");
    }

    @Test
    void m8_file_dependencies() {
        RModel cdm = parseModel("namespace cdm\ntype Foo:\n");
        RModel drr = parseModel("namespace drr\nimport cdm.*\ntype Report:\n    foo Foo (1..1)");
        RWorkspace ws = RWorkspace.build(List.of(cdm, drr)).workspace();

        assertTrue(ws.getDependencies(drr).contains(cdm));
    }
}
