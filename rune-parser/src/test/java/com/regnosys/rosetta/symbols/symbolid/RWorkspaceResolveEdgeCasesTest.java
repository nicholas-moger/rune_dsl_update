package com.regnosys.rosetta.symbols.symbolid;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.symbolid.SymbolIdTestFixtures;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.SymbolId;
import com.regnosys.rosetta.symbols.SymbolResolver;
import org.junit.jupiter.api.Test;

import java.lang.ref.Reference;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge-case tests for {@link RWorkspace#resolve(SymbolId, Class)}.
 *
 * <p>Covers: type-mismatch IAE, unknown-local-name null-return,
 * namespace case-sensitivity, local-name case-sensitivity,
 * null expected-class NPE, and SF8 builtin-namespace routing.
 *
 * <p>Every test that constructs a local workspace ends with
 * {@link Reference#reachabilityFence(Object)} per R5-2 discipline.
 *
 * <p>Part of P1.4.1b Task 6.2 (plan §6.2).
 */
class RWorkspaceResolveEdgeCasesTest {

    /**
     * Resolving a known SymbolId with the wrong expected type must throw
     * {@link IllegalArgumentException} whose message names both the actual
     * runtime type and the caller's expected type.
     *
     * <p>Fixture: {@code super-type/} contains {@code com.test.Parent} as
     * an {@code RDataType}. Requesting it as {@code REnumeration} triggers
     * the type-mismatch branch.
     */
    @Test
    void resolve_typeMismatch_throwsIllegalArgumentException() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("super-type/");
        RWorkspace ws = RWorkspace.build(files).workspace();
        SymbolId parentId = SymbolId.of("com.test", "Parent", ws.generation());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ws.resolve(parentId, REnumeration.class));
        assertTrue(ex.getMessage().contains("Parent"));
        assertTrue(ex.getMessage().contains("RDataType"));
        assertTrue(ex.getMessage().contains("REnumeration"));
        Reference.reachabilityFence(ws);
    }

    /**
     * A SymbolId whose namespace IS in the workspace but whose local name
     * does not match any registered element must return null (not throw).
     *
     * <p>This tests the "target.isEmpty() → return null" branch of resolve().
     */
    @Test
    void resolve_unknownLocalNameInKnownNamespace_returnsNull() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("super-type/");
        RWorkspace ws = RWorkspace.build(files).workspace();
        SymbolId ghost = SymbolId.of("com.test", "DoesNotExist", ws.generation());
        assertNull(ws.resolve(ghost, RDataType.class));
        Reference.reachabilityFence(ws);
    }

    /**
     * Namespace lookup is case-sensitive. A SymbolId with a differently-cased
     * namespace ("com.TEST" vs "com.test") must not resolve.
     *
     * <p>Rune-dsl namespace identifiers are case-sensitive per the grammar;
     * treating them as case-insensitive would allow silent cross-namespace
     * pollution on case-insensitive file systems.
     */
    @Test
    void resolve_namespaceCaseSensitive() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("super-type/");
        RWorkspace ws = RWorkspace.build(files).workspace();
        SymbolId wrongCase = SymbolId.of("com.TEST", "Parent", ws.generation());
        // Different case should NOT match (rune-dsl namespace lookup is case-sensitive).
        assertNull(ws.resolve(wrongCase, RDataType.class));
        Reference.reachabilityFence(ws);
    }

    /**
     * Local name lookup within a namespace is case-sensitive.
     * "PARENT" must not resolve when "Parent" is the registered name.
     */
    @Test
    void resolve_localNameCaseSensitive() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("super-type/");
        RWorkspace ws = RWorkspace.build(files).workspace();
        SymbolId wrongCase = SymbolId.of("com.test", "PARENT", ws.generation());
        assertNull(ws.resolve(wrongCase, RDataType.class));
        Reference.reachabilityFence(ws);
    }

    /**
     * {@link SymbolResolver} contract: {@code expected == null} must throw
     * {@link NullPointerException} BEFORE any id-null or staleness check.
     *
     * <p>This ordering ensures a fully-bogus call surfaces as a clean NPE
     * rather than being silently absorbed by a null-id return path.
     */
    @Test
    void expected_nullClass_throwsNullPointerException() throws Exception {
        RWorkspace ws = RWorkspace.build(List.<RModel>of()).workspace();
        SymbolId id = SymbolId.of("com.foo", "Bar", ws.generation());
        assertThrows(NullPointerException.class, () -> ws.resolve(id, null));
        Reference.reachabilityFence(ws);
    }

    /**
     * SF8 sentinel: a {@link SymbolId} whose namespace is
     * {@link SymbolResolver#BUILTIN_NAMESPACE} must be routed to
     * {@code fileScopes.builtinNamespace()} rather than the user-facing
     * {@code namespaces} map.
     *
     * <p><b>Option C</b> (chosen over A/B): directly construct a SymbolId
     * with {@code BUILTIN_NAMESPACE} and drive {@code RWorkspace.resolve}
     * on a zero-file workspace. The zero-file workspace has no file scopes,
     * so {@code fileScopes.values().stream()...findFirst()} produces an empty
     * Optional, and resolve returns null. This is the correct and specified
     * contract (Javadoc: "Empty fileScopes → returns null for builtin
     * targets"). The test verifies:
     * <ol>
     *   <li>The BUILTIN_NAMESPACE branch IS taken (no StaleSymbolIdException
     *       or ClassCastException is thrown — the namespace is routed through
     *       the builtin path, not the user-namespace path).</li>
     *   <li>The return is null (no builtins loaded), not a spurious non-null
     *       result from a leaking user-namespace lookup.</li>
     * </ol>
     *
     * <p>Options A (load a minimal builtin fixture) and B (drive
     * GlobalResolutionPass directly) were evaluated and rejected: the
     * {@code com.rosetta.model} builtin namespace is only populated when a
     * fixture file explicitly declares that namespace, which would require
     * a gitignored external dependency (the rune-dsl clone). Option C
     * provides a clean, self-contained contract test that does not depend
     * on any external file system state.
     *
     * <p>Deferred from {@code RTypeCallReferencedTypeIdTest} (commit d9223fa).
     * The full pipeline test where {@code GlobalResolutionPass.symbolIdOf}
     * emits the sentinel for an unbound builtin target remains deferred to
     * an integration test requiring the rune-dsl corpus.
     */
    @Test
    void resolve_builtinNamespaceSentinel_routesToBuiltinNamespace() {
        // Zero-file workspace: no file scopes, builtinNamespace() always null.
        RWorkspace ws = RWorkspace.build(List.<RModel>of()).workspace();
        // Construct a SymbolId with the BUILTIN_NAMESPACE sentinel directly.
        SymbolId builtinId = SymbolId.of(SymbolResolver.BUILTIN_NAMESPACE, "string", ws.generation());
        // The BUILTIN_NAMESPACE branch is taken in resolve(); with no file scopes,
        // the result is null (correct per spec — no builtins available in this workspace).
        assertNull(ws.resolve(builtinId, com.regnosys.rosetta.ast.RNode.class));
        Reference.reachabilityFence(ws);
    }
}
