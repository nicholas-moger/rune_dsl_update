package com.regnosys.rosetta.ast.symbolid;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.SymbolId;
import com.regnosys.rosetta.symbols.symbolid.TestSymbolResolver;
import org.junit.jupiter.api.Test;

import java.lang.ref.Reference;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the migrated cross-ref accessor for {@code REnumeration.superTypeId}
 * (P1.4.1b Task 4.4 — site 4/6 SymbolId migration).
 *
 * <ul>
 *   <li>{@code superType()} keeps its existing {@code Optional<REnumeration>} signature.</li>
 *   <li>{@code superTypeId()} is a new accessor returning {@code Optional<SymbolId>}.</li>
 *   <li>{@code setSuperTypeId(SymbolId)} replaces the removed
 *       {@code setResolvedSuperType(REnumeration)} setter.</li>
 * </ul>
 *
 * <p>Uses the dedicated minimal-corpus fixture under
 * {@code rune-parser/src/test/resources/symbolid-fixtures/super-enum/}
 * so the test does not depend on the full builtin corpus.
 *
 * <p>Per plan step 4.4 — mirrors {@link RFunctionSuperFunctionIdTest} shape.
 */
class REnumerationSuperTypeIdTest {

    /**
     * After RWorkspace.build(), the child enum's superTypeId holds the correct
     * SymbolId, and superType() resolves lazily to the parent node.
     */
    @Test
    void superType_resolvesViaWorkspace_postBuild() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("super-enum/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        REnumeration child = ws.namespace("com.test")
                .flatMap(n -> n.lookup("ChildEnum"))
                .filter(REnumeration.class::isInstance)
                .map(REnumeration.class::cast)
                .orElseThrow(() -> new AssertionError("REnumeration com.test.ChildEnum not found in workspace"));

        REnumeration parent = ws.namespace("com.test")
                .flatMap(n -> n.lookup("ParentEnum"))
                .filter(REnumeration.class::isInstance)
                .map(REnumeration.class::cast)
                .orElseThrow(() -> new AssertionError("REnumeration com.test.ParentEnum not found in workspace"));

        // superTypeId() must be present with correct coordinates
        Optional<SymbolId> childSuperTypeId = child.superTypeId();
        assertTrue(childSuperTypeId.isPresent(),
                "ChildEnum.superTypeId() should be present after build");
        assertEquals("com.test", childSuperTypeId.get().namespace(),
                "SymbolId namespace should be 'com.test'");
        assertEquals("ParentEnum", childSuperTypeId.get().localName(),
                "SymbolId localName should be 'ParentEnum'");
        assertEquals(ws.generation(), childSuperTypeId.get().generation(),
                "SymbolId generation should match workspace generation");

        // superType() lazy-resolves to the same node instance as parent
        assertSame(parent, child.superType().orElseThrow(),
                "superType() should resolve to the same REnumeration instance as parent");
    }

    /**
     * An enum without an 'extends' clause should have an empty superTypeId
     * and an empty superType().
     */
    @Test
    void noSuperType_idIsEmpty() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("super-enum/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        REnumeration parent = ws.namespace("com.test")
                .flatMap(n -> n.lookup("ParentEnum"))
                .filter(REnumeration.class::isInstance)
                .map(REnumeration.class::cast)
                .orElseThrow(() -> new AssertionError("REnumeration com.test.ParentEnum not found in workspace"));

        assertTrue(parent.superTypeId().isEmpty(),
                "ParentEnum.superTypeId() should be empty (no extends clause)");
        assertTrue(parent.superType().isEmpty(),
                "ParentEnum.superType() should be empty (no extends clause)");
    }

    /**
     * Direct-construction test: wire a child enum to a parent enum via
     * TestSymbolResolver.wireSuperType and the new setSuperTypeId setter.
     */
    @Test
    void directConstruction_wireSuperType_enumTarget() {
        TestSymbolResolver resolver = new TestSymbolResolver();

        REnumeration parent = new REnumeration();
        parent.setName("MyParentEnum");

        REnumeration child = new REnumeration();
        child.setName("MyChildEnum");
        child.setSuperTypeName("MyParentEnum");

        // Wire via the new setter (mirrors wireSuperType pattern)
        resolver.wireSuperType(child, "test", "MyParentEnum", parent, child::setSuperTypeId);

        // superTypeId() is present
        Optional<SymbolId> id = child.superTypeId();
        assertTrue(id.isPresent(), "superTypeId() should be present after wiring");
        assertEquals("test", id.get().namespace());
        assertEquals("MyParentEnum", id.get().localName());

        // superType() lazy-resolves to the same REnumeration instance
        assertSame(parent, child.superType().orElseThrow(),
                "superType() should resolve lazily to parent");

        Reference.reachabilityFence(resolver);
    }
}
