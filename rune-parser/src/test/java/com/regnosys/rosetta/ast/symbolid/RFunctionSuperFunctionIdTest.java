package com.regnosys.rosetta.ast.symbolid;

import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.SymbolId;
import com.regnosys.rosetta.symbols.symbolid.TestSymbolResolver;
import java.lang.ref.Reference;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the migrated cross-ref accessor for {@code RFunction.superFunctionId}
 * (P1.4.1b Task 4.3 — site 3/6 SymbolId migration).
 *
 * <ul>
 *   <li>{@code superFunction()} keeps its existing {@code Optional<RFunction>} signature.</li>
 *   <li>{@code superFunctionId()} is a new accessor returning {@code Optional<SymbolId>}.</li>
 *   <li>{@code setSuperFunctionId(SymbolId)} replaces the removed
 *       {@code setResolvedSuperFunction(RFunction)} setter.</li>
 * </ul>
 *
 * <p>Uses the dedicated minimal-corpus fixture under
 * {@code rune-parser/src/test/resources/symbolid-fixtures/super-function/}
 * so the test does not depend on the full builtin corpus.
 *
 * <p>Per plan step 4.3 — mirrors {@link RDataTypeChoiceSuperTypeIdTest} shape.
 */
class RFunctionSuperFunctionIdTest {

    /**
     * After RWorkspace.build(), the child function's superFunctionId holds the correct
     * SymbolId, and superFunction() resolves lazily to the parent node.
     */
    @Test
    void superFunction_resolvesViaWorkspace_postBuild() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("super-function/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        RFunction child = ws.namespace("com.test")
                .flatMap(n -> n.lookup("ChildFn"))
                .filter(RFunction.class::isInstance)
                .map(RFunction.class::cast)
                .orElseThrow(() -> new AssertionError("RFunction com.test.ChildFn not found in workspace"));

        RFunction parent = ws.namespace("com.test")
                .flatMap(n -> n.lookup("ParentFn"))
                .filter(RFunction.class::isInstance)
                .map(RFunction.class::cast)
                .orElseThrow(() -> new AssertionError("RFunction com.test.ParentFn not found in workspace"));

        // superFunctionId() must be present with correct coordinates
        Optional<SymbolId> childSuperFnId = child.superFunctionId();
        assertTrue(childSuperFnId.isPresent(),
                "ChildFn.superFunctionId() should be present after build");
        assertEquals("com.test", childSuperFnId.get().namespace(),
                "SymbolId namespace should be 'com.test'");
        assertEquals("ParentFn", childSuperFnId.get().localName(),
                "SymbolId localName should be 'ParentFn'");
        assertEquals(ws.generation(), childSuperFnId.get().generation(),
                "SymbolId generation should match workspace generation");

        // superFunction() lazy-resolves to the same node instance as parent
        assertSame(parent, child.superFunction().orElseThrow(),
                "superFunction() should resolve to the same RFunction instance as parent");
    }

    /**
     * A function without an 'extends' clause should have an empty superFunctionId
     * and an empty superFunction().
     */
    @Test
    void noSuperFunction_idIsEmpty() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("super-function/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        RFunction parent = ws.namespace("com.test")
                .flatMap(n -> n.lookup("ParentFn"))
                .filter(RFunction.class::isInstance)
                .map(RFunction.class::cast)
                .orElseThrow(() -> new AssertionError("RFunction com.test.ParentFn not found in workspace"));

        assertTrue(parent.superFunctionId().isEmpty(),
                "ParentFn.superFunctionId() should be empty (no extends clause)");
        assertTrue(parent.superFunction().isEmpty(),
                "ParentFn.superFunction() should be empty (no extends clause)");
    }

    /**
     * Direct-construction test: wire a child function to a parent function via
     * TestSymbolResolver.wireSuperType and the new setSuperFunctionId setter.
     */
    @Test
    void directConstruction_wireSuperType_functionTarget() {
        TestSymbolResolver resolver = new TestSymbolResolver();

        RFunction parent = new RFunction();
        parent.setName("MyParentFn");

        RFunction child = new RFunction();
        child.setName("MyChildFn");
        child.setSuperFunctionName("MyParentFn");

        // Wire via the new setter (mirrors wireSuperType pattern)
        resolver.wireSuperType(child, "test", "MyParentFn", parent, child::setSuperFunctionId);

        // superFunctionId() is present
        Optional<SymbolId> id = child.superFunctionId();
        assertTrue(id.isPresent(), "superFunctionId() should be present after wiring");
        assertEquals("test", id.get().namespace());
        assertEquals("MyParentFn", id.get().localName());

        // superFunction() lazy-resolves to the same RFunction instance
        assertSame(parent, child.superFunction().orElseThrow(),
                "superFunction() should resolve lazily to parent");
        Reference.reachabilityFence(resolver);
    }
}
