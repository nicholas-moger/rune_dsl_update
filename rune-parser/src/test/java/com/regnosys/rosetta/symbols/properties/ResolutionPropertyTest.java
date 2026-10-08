package com.regnosys.rosetta.symbols.properties;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import net.jqwik.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResolutionPropertyTest {

    @Property(tries = 100)
    void any_super_type_within_same_namespace_resolves(
            @ForAll("typeNames") String parentName,
            @ForAll("typeNames") String childName) {
        Assume.that(!parentName.equals(childName));

        String source = "namespace test\n"
            + "type " + parentName + ":\n    a string (1..1)\n"
            + "type " + childName + " extends " + parentName + ":\n    b string (1..1)\n";

        RModel model = AstBuilder.buildFromString(source, "property-test.rosetta");
        RLinkingResult result = RWorkspace.build(List.of(model));

        RDataType child = AstWalker.findAll(model, RDataType.class).stream()
            .filter(dt -> childName.equals(dt.name()))
            .findFirst().orElseThrow();
        assertTrue(child.superType().isPresent(),
            "any in-namespace super type should resolve");
        assertEquals(parentName, child.superType().get().name());
    }

    @Property(tries = 100)
    void resolved_super_type_appears_in_subtype_index(
            @ForAll("typeNames") String parentName,
            @ForAll("typeNames") String childName) {
        Assume.that(!parentName.equals(childName));

        String source = "namespace test\n"
            + "type " + parentName + ":\n"
            + "type " + childName + " extends " + parentName + ":\n";

        RModel model = AstBuilder.buildFromString(source, "property-test.rosetta");
        RLinkingResult result = RWorkspace.build(List.of(model));

        RDataType parent = AstWalker.findAll(model, RDataType.class).stream()
            .filter(dt -> parentName.equals(dt.name()))
            .findFirst().orElseThrow();
        RDataType child = AstWalker.findAll(model, RDataType.class).stream()
            .filter(dt -> childName.equals(dt.name()))
            .findFirst().orElseThrow();

        assertTrue(result.workspace().getSubTypes(parent).contains(child),
            "child must appear in parent's subtype index");
    }

    @Property(tries = 100)
    void resolved_super_type_appears_in_reference_index(
            @ForAll("typeNames") String parentName,
            @ForAll("typeNames") String childName) {
        Assume.that(!parentName.equals(childName));

        String source = "namespace test\n"
            + "type " + parentName + ":\n"
            + "type " + childName + " extends " + parentName + ":\n";

        RModel model = AstBuilder.buildFromString(source, "property-test.rosetta");
        RLinkingResult result = RWorkspace.build(List.of(model));

        RDataType parent = AstWalker.findAll(model, RDataType.class).stream()
            .filter(dt -> parentName.equals(dt.name()))
            .findFirst().orElseThrow();

        assertFalse(result.workspace().findReferences(parent).isEmpty(),
            "parent must have at least one reference (the child's extends)");
    }

    @Provide
    Arbitrary<String> typeNames() {
        return Arbitraries.strings()
            .withCharRange('A', 'Z')
            .ofLength(1)
            .flatMap(first -> Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(2)
                .ofMaxLength(8)
                .map(rest -> first + rest));
    }
}
