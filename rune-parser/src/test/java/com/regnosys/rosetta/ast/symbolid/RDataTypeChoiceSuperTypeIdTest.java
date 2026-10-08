package com.regnosys.rosetta.ast.symbolid;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.SymbolId;
import com.regnosys.rosetta.symbols.symbolid.TestSymbolResolver;
import java.lang.ref.Reference;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the migrated cross-ref accessor for {@code RDataType.choiceSuperTypeId}
 * (P1.4.1b Task 4.2 — site 2/6 SymbolId migration).
 *
 * <ul>
 *   <li>{@code choiceSuperType()} keeps its existing {@code Optional<RChoice>} signature.</li>
 *   <li>{@code choiceSuperTypeId()} is a new accessor returning {@code Optional<SymbolId>}.</li>
 *   <li>{@code setChoiceSuperTypeId(SymbolId)} replaces the removed
 *       {@code setResolvedChoiceSuperType(RChoice)} setter.</li>
 * </ul>
 *
 * <p>Uses the dedicated minimal-corpus fixture under
 * {@code rune-parser/src/test/resources/symbolid-fixtures/choice-super-type/}
 * so the test does not depend on the full builtin corpus.
 *
 * <p>Per plan step 4.2.1 — mirrors {@link RDataTypeSuperTypeIdTest} shape.
 */
class RDataTypeChoiceSuperTypeIdTest {

    /**
     * After RWorkspace.build(), the child type's choiceSuperTypeId holds the correct
     * SymbolId, and choiceSuperType() resolves lazily to the ChoiceParent node.
     */
    @Test
    void choiceSuperType_resolvesViaWorkspace_postBuild() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("choice-super-type/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        RDataType child = SymbolIdTestFixtures.findDataType(ws, "com.test.ChildOfChoice");
        RChoice parent = ws.namespace("com.test")
                .flatMap(n -> n.lookup("ChoiceParent"))
                .filter(RChoice.class::isInstance)
                .map(RChoice.class::cast)
                .orElseThrow(() -> new AssertionError("RChoice com.test.ChoiceParent not found in workspace"));

        // choiceSuperTypeId() must be present with correct coordinates
        Optional<SymbolId> childChoiceSuperId = child.choiceSuperTypeId();
        assertTrue(childChoiceSuperId.isPresent(),
                "ChildOfChoice.choiceSuperTypeId() should be present after build");
        assertEquals("com.test", childChoiceSuperId.get().namespace(),
                "SymbolId namespace should be 'com.test'");
        assertEquals("ChoiceParent", childChoiceSuperId.get().localName(),
                "SymbolId localName should be 'ChoiceParent'");
        assertEquals(ws.generation(), childChoiceSuperId.get().generation(),
                "SymbolId generation should match workspace generation");

        // choiceSuperType() lazy-resolves to the same node instance as parent
        assertSame(parent, child.choiceSuperType().orElseThrow(),
                "choiceSuperType() should resolve to the same RChoice instance as parent");

        // superTypeId() must be empty — ChoiceParent is not an RDataType
        assertTrue(child.superTypeId().isEmpty(),
                "ChildOfChoice.superTypeId() should be empty (super is a choice, not a data type)");

        // isSuperTypeResolved() must return true via the choice branch
        assertTrue(child.isSuperTypeResolved(),
                "isSuperTypeResolved() should return true when choiceSuperTypeId is set");
    }

    /**
     * A type extending a choice has {@code choiceSuperTypeId} present and
     * {@code superTypeId} empty — the two ID slots are mutually exclusive
     * for a given super-type, and the linker routes to the correct one based
     * on the resolved target's kind ({@code RChoice} vs {@code RDataType}).
     */
    @Test
    void choiceSuperTypeId_isPresent_superTypeId_isEmpty_whenExtendsChoice() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("choice-super-type/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        RDataType child = SymbolIdTestFixtures.findDataType(ws, "com.test.ChildOfChoice");

        // superTypeId must be absent (the super is a choice, not a data type)
        assertTrue(child.superTypeId().isEmpty(),
                "superTypeId() should be empty for a type extending a choice");

        // choiceSuperTypeId must be present
        assertTrue(child.choiceSuperTypeId().isPresent(),
                "choiceSuperTypeId() should be present for a type extending a choice");
    }

    /**
     * Direct-construction test: wire a child data type to a choice super type via
     * TestSymbolResolver.wireSuperType and the new setChoiceSuperTypeId setter.
     */
    @Test
    void directConstruction_wireSuperType_choiceTarget() {
        TestSymbolResolver resolver = new TestSymbolResolver();

        RChoice parent = new RChoice();
        parent.setName("MyChoice");

        RDataType child = new RDataType();
        child.setName("ChildType");
        child.setSuperTypeName("MyChoice");

        // Wire via the new setter (mirrors wireSuperType pattern)
        resolver.wireSuperType(child, "test", "MyChoice", parent, child::setChoiceSuperTypeId);

        // choiceSuperTypeId() is present
        Optional<SymbolId> id = child.choiceSuperTypeId();
        assertTrue(id.isPresent(), "choiceSuperTypeId() should be present after wiring");
        assertEquals("test", id.get().namespace());
        assertEquals("MyChoice", id.get().localName());

        // choiceSuperType() lazy-resolves to the same RChoice instance
        assertSame(parent, child.choiceSuperType().orElseThrow(),
                "choiceSuperType() should resolve lazily to parent");

        // superTypeId() is absent
        assertTrue(child.superTypeId().isEmpty(),
                "superTypeId() should be empty when only choiceSuperTypeId is set");

        // isSuperTypeResolved() is true via choiceSuperTypeId branch
        assertTrue(child.isSuperTypeResolved(),
                "isSuperTypeResolved() should be true when choiceSuperTypeId is set");
        Reference.reachabilityFence(resolver);
    }
}
