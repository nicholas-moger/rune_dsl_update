package com.regnosys.rosetta.ast.symbolid;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.SymbolId;
import com.regnosys.rosetta.symbols.SymbolResolver;
import com.regnosys.rosetta.symbols.symbolid.TestSymbolResolver;
import org.junit.jupiter.api.Test;

import java.lang.ref.Reference;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the migrated cross-ref accessor for {@code RTypeCall.referencedTypeId}
 * (P1.4.1b Task 4.5 — site 5/6 SymbolId migration, workhorse).
 *
 * <ul>
 *   <li>{@code referencedType()} keeps its existing {@code Optional<RNode>} signature
 *       (broadest type — callers use instanceof for the specific kind).</li>
 *   <li>{@code referencedTypeId()} is a new accessor returning {@code Optional<SymbolId>}.</li>
 *   <li>{@code setReferencedTypeId(SymbolId)} replaces the removed
 *       {@code setResolvedType(RNode)} setter.</li>
 * </ul>
 *
 * <p>Covers all 4 target kinds + SF8 BUILTIN_NAMESPACE sentinel round-trip:
 * <ol>
 *   <li>RDataType target ({@code data-target.rosetta})</li>
 *   <li>REnumeration target ({@code enum-target.rosetta})</li>
 *   <li>RChoice target ({@code choice-target.rosetta})</li>
 *   <li>RTypeAlias target ({@code alias-target.rosetta})</li>
 *   <li>BUILTIN_NAMESPACE sentinel round-trip (direct construction — no
 *       fixture dependency on basictypes.rosetta which is in the gitignored
 *       {@code rune-dsl/} dependency clone, not present on CI)</li>
 *   <li>Direct-construction test via {@code TestSymbolResolver}</li>
 * </ol>
 *
 * <p>Per plan step 4.5 — mirrors {@link REnumerationSuperTypeIdTest} shape.
 */
class RTypeCallReferencedTypeIdTest {

    /**
     * After RWorkspace.build(), the RTypeCall in FooWithDataType.parent
     * has a referencedTypeId pointing to the Bar RDataType.
     */
    @Test
    void dataType_target_resolvesViaWorkspace_postBuild() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("typecall/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        RDataType fooType = findLocalDataType(ws, "FooWithDataType");
        RDataType barType = findLocalDataType(ws, "Bar");

        RAttribute parentAttr = fooType.attributes().stream()
                .filter(a -> "parent".equals(a.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("FooWithDataType.parent attribute not found"));

        RTypeCall tc = parentAttr.typeCall();
        assertNotNull(tc, "typeCall should not be null");

        Optional<SymbolId> id = tc.referencedTypeId();
        assertTrue(id.isPresent(), "referencedTypeId() should be present after build");
        assertEquals("com.test", id.get().namespace(),
                "SymbolId namespace should be 'com.test'");
        assertEquals("Bar", id.get().localName(),
                "SymbolId localName should be 'Bar'");
        assertEquals(ws.generation(), id.get().generation(),
                "SymbolId generation should match workspace generation");

        // referencedType() lazy-resolves to the Bar RDataType node
        Optional<RNode> resolved = tc.referencedType();
        assertTrue(resolved.isPresent(), "referencedType() should be present");
        assertInstanceOf(RDataType.class, resolved.get(),
                "referencedType() should be an RDataType");
        assertSame(barType, resolved.get(),
                "referencedType() should resolve to the same Bar instance");
    }

    /**
     * After RWorkspace.build(), the RTypeCall in FooWithEnum.c
     * has a referencedTypeId pointing to the Color REnumeration.
     */
    @Test
    void enumeration_target_resolvesViaWorkspace_postBuild() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("typecall/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        RDataType fooType = findLocalDataType(ws, "FooWithEnum");
        REnumeration colorType = ws.namespace("com.test")
                .flatMap(n -> n.lookup("Color"))
                .filter(REnumeration.class::isInstance)
                .map(REnumeration.class::cast)
                .orElseThrow(() -> new AssertionError("com.test.Color REnumeration not found"));

        RAttribute cAttr = fooType.attributes().stream()
                .filter(a -> "c".equals(a.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("FooWithEnum.c attribute not found"));

        RTypeCall tc = cAttr.typeCall();
        assertNotNull(tc, "typeCall should not be null");

        Optional<SymbolId> id = tc.referencedTypeId();
        assertTrue(id.isPresent(), "referencedTypeId() should be present after build");
        assertEquals("com.test", id.get().namespace());
        assertEquals("Color", id.get().localName());

        Optional<RNode> resolved = tc.referencedType();
        assertTrue(resolved.isPresent(), "referencedType() should be present");
        assertInstanceOf(REnumeration.class, resolved.get(),
                "referencedType() should be an REnumeration");
        assertSame(colorType, resolved.get(),
                "referencedType() should resolve to the same Color instance");
    }

    /**
     * After RWorkspace.build(), the RTypeCall in FooWithChoice.m
     * has a referencedTypeId pointing to the Method RChoice.
     */
    @Test
    void choice_target_resolvesViaWorkspace_postBuild() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("typecall/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        RDataType fooType = findLocalDataType(ws, "FooWithChoice");
        RChoice methodType = ws.namespace("com.test")
                .flatMap(n -> n.lookup("Method"))
                .filter(RChoice.class::isInstance)
                .map(RChoice.class::cast)
                .orElseThrow(() -> new AssertionError("com.test.Method RChoice not found"));

        RAttribute mAttr = fooType.attributes().stream()
                .filter(a -> "m".equals(a.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("FooWithChoice.m attribute not found"));

        RTypeCall tc = mAttr.typeCall();
        assertNotNull(tc, "typeCall should not be null");

        Optional<SymbolId> id = tc.referencedTypeId();
        assertTrue(id.isPresent(), "referencedTypeId() should be present after build");
        assertEquals("com.test", id.get().namespace());
        assertEquals("Method", id.get().localName());

        Optional<RNode> resolved = tc.referencedType();
        assertTrue(resolved.isPresent(), "referencedType() should be present");
        assertInstanceOf(RChoice.class, resolved.get(),
                "referencedType() should be an RChoice");
        assertSame(methodType, resolved.get(),
                "referencedType() should resolve to the same Method instance");
    }

    /**
     * After RWorkspace.build(), the RTypeCall in FooWithAlias.a
     * has a referencedTypeId pointing to the SomeAlias RTypeAlias.
     */
    @Test
    void typeAlias_target_resolvesViaWorkspace_postBuild() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("typecall/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        RDataType fooType = findLocalDataType(ws, "FooWithAlias");
        RTypeAlias aliasType = ws.namespace("com.test")
                .flatMap(n -> n.lookup("SomeAlias"))
                .filter(RTypeAlias.class::isInstance)
                .map(RTypeAlias.class::cast)
                .orElseThrow(() -> new AssertionError("com.test.SomeAlias RTypeAlias not found"));

        RAttribute aAttr = fooType.attributes().stream()
                .filter(a -> "a".equals(a.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("FooWithAlias.a attribute not found"));

        RTypeCall tc = aAttr.typeCall();
        assertNotNull(tc, "typeCall should not be null");

        Optional<SymbolId> id = tc.referencedTypeId();
        assertTrue(id.isPresent(), "referencedTypeId() should be present after build");
        assertEquals("com.test", id.get().namespace());
        assertEquals("SomeAlias", id.get().localName());

        Optional<RNode> resolved = tc.referencedType();
        assertTrue(resolved.isPresent(), "referencedType() should be present");
        assertInstanceOf(RTypeAlias.class, resolved.get(),
                "referencedType() should be an RTypeAlias");
        assertSame(aliasType, resolved.get(),
                "referencedType() should resolve to the same SomeAlias instance");
    }

    /**
     * SF8 round-trip: a SymbolId carrying the {@code BUILTIN_NAMESPACE}
     * sentinel can be set via {@code setReferencedTypeId} and read back via
     * {@code referencedTypeId()} unchanged.
     *
     * <p>Direct-construction test — does NOT depend on basictypes.rosetta
     * being present on the filesystem (it lives in the gitignored
     * {@code rune-dsl/} dependency clone, which is not checked out on CI).
     * The full pipeline test where {@code GlobalResolutionPass.symbolIdOf}
     * emits the sentinel for an unbound builtin target is deferred to
     * Task 6 edge-case tests with a self-contained minimal builtin fixture.
     */
    @Test
    void builtin_namespace_sentinel_roundTripsViaSetter() {
        TestSymbolResolver resolver = new TestSymbolResolver();
        RTypeCall tc = new RTypeCall();
        tc.setTypeName("string");
        tc.attachToWorkspace(resolver);

        SymbolId builtinSid = SymbolId.of(SymbolResolver.BUILTIN_NAMESPACE,
                "string", resolver.generation());
        tc.setReferencedTypeId(builtinSid);

        Optional<SymbolId> roundTrip = tc.referencedTypeId();
        assertTrue(roundTrip.isPresent(),
                "referencedTypeId() should be present after setReferencedTypeId");
        assertEquals(SymbolResolver.BUILTIN_NAMESPACE, roundTrip.get().namespace(),
                "SymbolId namespace should be the BUILTIN_NAMESPACE sentinel");
        assertEquals("string", roundTrip.get().localName(),
                "SymbolId localName should be 'string'");
        assertEquals(resolver.generation(), roundTrip.get().generation(),
                "SymbolId generation should match resolver generation");

        Reference.reachabilityFence(resolver);
    }

    /**
     * Direct-construction test: wire an RTypeCall to an RDataType target via
     * TestSymbolResolver and the new setReferencedTypeId setter.
     */
    @Test
    void directConstruction_wireTypeCall_dataTypeTarget() {
        TestSymbolResolver resolver = new TestSymbolResolver();

        RDataType targetType = new RDataType();
        targetType.setName("MyType");
        targetType.attachToWorkspace(resolver);

        RTypeCall tc = new RTypeCall();
        tc.setTypeName("MyType");
        tc.attachToWorkspace(resolver);

        resolver.bind("test.ns", "MyType", targetType);
        tc.setReferencedTypeId(resolver.idFor("test.ns", "MyType"));

        // referencedTypeId() is present
        Optional<SymbolId> id = tc.referencedTypeId();
        assertTrue(id.isPresent(), "referencedTypeId() should be present after wiring");
        assertEquals("test.ns", id.get().namespace());
        assertEquals("MyType", id.get().localName());

        // referencedType() lazy-resolves to the same RDataType instance
        Optional<RNode> resolved = tc.referencedType();
        assertTrue(resolved.isPresent(), "referencedType() should resolve lazily");
        assertInstanceOf(RDataType.class, resolved.get());
        assertSame(targetType, resolved.get(),
                "referencedType() should resolve to the same RDataType instance");

        Reference.reachabilityFence(resolver);
    }

    /**
     * R7-1 regression test: when a {@code RTypeCall} uses a qualified type
     * name like {@code com.test.foo.Bar}, the resulting {@code SymbolId} must
     * carry the LOCAL segment (just {@code Bar}) plus the producer namespace
     * ({@code com.test.foo}) — NOT the full qualified form as the local name
     * with a {@code BUILTIN_NAMESPACE} fallback (which was the pre-fix bug).
     *
     * <p>Pre-fix behaviour (broken): {@code symbolIdOf} did
     * {@code namespace.lookup("com.test.foo.Bar")}, which never matches any
     * registered local name, so it fell through to
     * {@code SymbolId.of(BUILTIN_NAMESPACE, "com.test.foo.Bar", gen)} —
     * lazy {@code referencedType()} then returned null because the builtin
     * namespace contains no such local name. None of the 4 same-namespace
     * fixture tests caught this because they used unqualified names.
     */
    @Test
    void qualified_typeName_extractsLocalSegment_inSymbolId() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("typecall-qualified/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        // Container is in com.test.bar (different namespace from local helper default).
        RDataType container = SymbolIdTestFixtures.findDataType(ws, "com.test.bar.Container");
        RAttribute bAttr = container.attributes().stream()
                .filter(a -> "b".equals(a.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Container.b attribute not found"));

        RTypeCall tc = bAttr.typeCall();
        assertNotNull(tc, "typeCall should not be null");

        Optional<SymbolId> id = tc.referencedTypeId();
        assertTrue(id.isPresent(),
                "referencedTypeId() should be present for a qualified type call");
        assertEquals("Bar", id.get().localName(),
                "SymbolId localName should be the LOCAL segment 'Bar', not the qualified form");
        assertEquals("com.test.foo", id.get().namespace(),
                "SymbolId namespace should be the producer namespace 'com.test.foo'");

        // referencedType() lazily resolves through the cross-namespace lookup
        Optional<RNode> resolved = tc.referencedType();
        assertTrue(resolved.isPresent(),
                "referencedType() must resolve cross-namespace — pre-fix this returned null");
        assertInstanceOf(RDataType.class, resolved.get());
        assertEquals("Bar", ((RDataType) resolved.get()).name());
    }

    /**
     * R11-3 regression test: when a {@code RTypeCall} resolves to a target via a
     * single-type aliased import (e.g. {@code import com.foo.Bar as MyType} then
     * {@code MyType}), the resulting {@code SymbolId} must carry the target's
     * ACTUAL declared name ({@code Bar}) — NOT the source-text alias
     * ({@code MyType}). The alias only exists in the source file's import scope;
     * the registered name in {@code namespaces.lookup} is always the original.
     *
     * <p>Pre-fix behaviour (broken): R7-1's lastIndexOf('.') heuristic would
     * extract {@code MyType} as the localName (since it isn't dotted), but
     * {@code symbolIdOf} would call {@code namespace.lookup("MyType")} which
     * returns empty (only {@code Bar} is registered). Falls through to
     * {@code BUILTIN_NAMESPACE} with localName {@code MyType} — broken.
     *
     * <p>Post-fix: {@code typeNodeName} dispatches on the resolved target's
     * concrete subtype and returns {@code RDataType#name()} = {@code Bar}, so
     * {@code symbolIdOf} finds it in {@code com.test.foo} correctly.
     */
    @Test
    void aliasedImport_singleType_useTargetActualName() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("typecall-aliased/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        RDataType container = SymbolIdTestFixtures.findDataType(ws, "com.test.bar.Container");
        RAttribute cAttr = container.attributes().stream()
                .filter(a -> "c".equals(a.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Container.c attribute not found"));

        RTypeCall tc = cAttr.typeCall();
        assertNotNull(tc, "typeCall should not be null");

        Optional<SymbolId> id = tc.referencedTypeId();
        assertTrue(id.isPresent(),
                "referencedTypeId() should be present for an aliased type call");
        assertEquals("Bar", id.get().localName(),
                "SymbolId localName should be target's ACTUAL name 'Bar', not the alias 'MyType'");
        assertEquals("com.test.foo", id.get().namespace(),
                "SymbolId namespace should be the producer namespace, not BUILTIN_NAMESPACE");

        Optional<RNode> resolved = tc.referencedType();
        assertTrue(resolved.isPresent(),
                "referencedType() must resolve through alias — pre-fix this returned null");
        assertInstanceOf(RDataType.class, resolved.get());
        assertEquals("Bar", ((RDataType) resolved.get()).name());
    }

    /**
     * R12-1 regression test: when a function and a type share the same local
     * name in the same namespace (e.g. {@code CreditSupportAmount}),
     * {@code resolveTypeCall}'s type-filter at lines 134-138 of
     * {@code GlobalResolutionPass} picks the type even though
     * {@code RNamespaceScope.lookup} (first-wins) would return the function.
     * After the filter, {@code target} is the TYPE, but it's not the first
     * declaration registered for that local name.
     *
     * <p>Pre-fix behaviour (broken): {@code symbolIdOf} called
     * {@code namespace.lookup(localName)} which returned the function. The
     * {@code t -> t == target} check failed (function != type), so
     * {@code symbolIdOf} fell through to {@code BUILTIN_NAMESPACE} with the
     * wrong namespace, producing a {@code SymbolId} that downstream
     * {@code referencedType()} resolution could not satisfy.
     *
     * <p>Post-fix: {@code symbolIdOf} uses
     * {@code namespace.allMatching(localName)} which returns BOTH the function
     * and the type. The {@code anyMatch(t -> t == target)} check finds the type
     * correctly, returning the right user-namespace {@code SymbolId}.
     *
     * <p>NB: this test asserts the {@code SymbolId} fields (R12-1's exact
     * scope). The orthogonal concern of which declaration
     * {@code referencedType()} picks at resolve-time when name shadowing
     * occurs across kinds (type vs function with same name) is a deeper
     * rune-dsl architectural concern — {@code SymbolId(namespace, localName)}
     * is intentionally kind-agnostic, and {@code RWorkspace.resolve} uses
     * {@code lookup} (first-wins). Adding a kind discriminator to
     * {@code SymbolId} or filtering at resolve-time is out of P1.4.1b scope.
     */
    @Test
    void typeFunctionShadowing_symbolIdLocalNameAndNamespaceCorrect() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("typecall-shadowed/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        RDataType container = SymbolIdTestFixtures.findDataType(ws, "com.test.Container");
        RAttribute cAttr = container.attributes().stream()
                .filter(a -> "c".equals(a.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Container.c attribute not found"));

        RTypeCall tc = cAttr.typeCall();
        assertNotNull(tc, "typeCall should not be null");

        Optional<SymbolId> id = tc.referencedTypeId();
        assertTrue(id.isPresent(),
                "referencedTypeId() should be present despite name shadowing");
        assertEquals("CreditSupportAmount", id.get().localName(),
                "SymbolId localName should be 'CreditSupportAmount'");
        assertEquals("com.test", id.get().namespace(),
                "SymbolId namespace MUST be 'com.test' (NOT BUILTIN_NAMESPACE) — "
                        + "this is the R12-1 fix in action: allMatching finds the "
                        + "type even though it's not lookup's first-wins result.");

        // referencedType() resolves to *some* declaration in com.test —
        // either the type or the function, depending on registration order.
        // The R12-1 fix's correctness is in the SymbolId fields above; the
        // resolve-time disambiguation is intentionally not asserted here.
        Optional<RNode> resolved = tc.referencedType();
        assertTrue(resolved.isPresent(),
                "referencedType() must resolve through the user namespace — "
                        + "pre-fix this returned null because the SymbolId pointed "
                        + "at BUILTIN_NAMESPACE.");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Local helper: resolves a simple name within the {@code com.test} namespace.
     * Distinct from {@link SymbolIdTestFixtures#findDataType(RWorkspace, String)},
     * which takes an FQN and works across arbitrary namespaces. Use the
     * SymbolIdTestFixtures variant when fixtures span multiple namespaces.
     */
    private static RDataType findLocalDataType(RWorkspace ws, String localName) {
        return ws.namespace("com.test")
                .flatMap(n -> n.lookup(localName))
                .filter(RDataType.class::isInstance)
                .map(RDataType.class::cast)
                .orElseThrow(() -> new AssertionError("RDataType com.test." + localName + " not found in workspace"));
    }
}
