package com.regnosys.rosetta.ast.symbolid;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.SymbolId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the migrated cross-ref accessor for {@code RDataType.superTypeId}
 * (P1.4.1b Task 4.1 — site 1/6 SymbolId migration).
 *
 * <ul>
 *   <li>{@code superType()} keeps its existing {@code Optional<RDataType>} signature.</li>
 *   <li>{@code superTypeId()} is a new accessor returning {@code Optional<SymbolId>}.</li>
 *   <li>{@code setSuperTypeId(SymbolId)} replaces the removed
 *       {@code setResolvedSuperType(RDataType)}.</li>
 * </ul>
 *
 * <p>Uses the dedicated minimal-corpus fixture under
 * {@code rune-parser/src/test/resources/symbolid-fixtures/super-type/}
 * so the test does not depend on the full builtin corpus.
 *
 * <p>Per plan step 4.1.5 and MF1 fix (use AstBuilder.buildFromString / parseAndLink pattern).
 */
class RDataTypeSuperTypeIdTest {

    /**
     * After RWorkspace.build(), the child type's superTypeId holds the correct
     * SymbolId, and superType() resolves lazily to the parent node.
     */
    @Test
    void superType_resolvesViaWorkspace_postBuild() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("super-type/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        RDataType child = SymbolIdTestFixtures.findDataType(ws, "com.test.Child");
        RDataType parent = SymbolIdTestFixtures.findDataType(ws, "com.test.Parent");

        // superTypeId() must be present with correct coordinates
        Optional<SymbolId> childSuperId = child.superTypeId();
        assertTrue(childSuperId.isPresent(),
                "Child.superTypeId() should be present after build");
        assertEquals("com.test", childSuperId.get().namespace(),
                "SymbolId namespace should be 'com.test'");
        assertEquals("Parent", childSuperId.get().localName(),
                "SymbolId localName should be 'Parent'");
        assertEquals(ws.generation(), childSuperId.get().generation(),
                "SymbolId generation should match workspace generation");

        // superType() lazy-resolves to the same node instance as parent
        assertSame(parent, child.superType().orElseThrow(),
                "superType() should resolve to the same RDataType instance as parent");
    }

    /**
     * A type without an 'extends' clause should have an empty superTypeId
     * and an empty superType().
     */
    @Test
    void noSuperType_idIsEmpty() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("super-type/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        RDataType parent = SymbolIdTestFixtures.findDataType(ws, "com.test.Parent");

        assertTrue(parent.superTypeId().isEmpty(),
                "Parent.superTypeId() should be empty (no extends clause)");
        assertTrue(parent.superType().isEmpty(),
                "Parent.superType() should be empty (no extends clause)");
    }
}
