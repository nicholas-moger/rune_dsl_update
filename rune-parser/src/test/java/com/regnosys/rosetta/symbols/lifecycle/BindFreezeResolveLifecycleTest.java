package com.regnosys.rosetta.symbols.lifecycle;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.symbolid.SymbolIdTestFixtures;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.StaleSymbolIdException;
import com.regnosys.rosetta.symbols.SymbolId;
import org.junit.jupiter.api.Test;

import java.lang.ref.Reference;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spec §5.5 / §12 risk #1 sentinel: linker freeze MUST come AFTER pass 4
 * (GlobalResolutionPass) and AFTER passes 5–7.
 *
 * <p>The ordering guarded here is:
 * <pre>
 *   Pass 1-3: symbol registration, import resolution, derived state
 *   Pass 4:   GlobalResolutionPass — emits SymbolIds onto nodes
 *   Interim attach: nodes get a SymbolResolver so passes 5-7 can lazy-resolve
 *   Pass 5-7: lexical resolution, type inference, validation
 *   Re-attach: nodes overwritten with the full workspace reference
 *   FREEZE:   RNode.freeze() propagated; checkMutable() now blocks all setters
 * </pre>
 *
 * <p>If freeze were moved earlier (between passes 4 and 5), the SymbolId
 * emission in pass 4 itself would throw {@code IllegalStateException} from
 * {@code checkMutable()}, causing test #1 to fail.
 *
 * <p>If freeze were moved before pass 4, no SymbolId would be emitted, so
 * {@link #pass4EmittedSymbolId_presentAndResolvable_postBuild()} would fail
 * on the {@code assertTrue(child.superTypeId().isPresent())} assertion.
 *
 * <p>Every test that creates a local workspace or resolver ends with
 * {@link Reference#reachabilityFence(Object)} per R5-2 discipline, ensuring
 * the JIT cannot GC the resolver before the test's assertions complete.
 */
class BindFreezeResolveLifecycleTest {

    // -----------------------------------------------------------------------
    // Coverage area 1 + 4: Build → attach → freeze → resolve happy path,
    //                       and the freeze-ordering sentinel.
    // -----------------------------------------------------------------------

    /**
     * After {@code RWorkspace.build()}, the child type's superTypeId is set
     * (confirming pass 4 ran BEFORE freeze), and superType() still resolves
     * through the post-freeze workspace (confirming re-attach ran BEFORE freeze
     * and that read-only lazy access is not guarded by checkMutable()).
     *
     * <p>Freeze-ordering sentinel: if freeze() moved to before pass 4, the
     * GlobalResolutionPass {@code setSuperTypeId()} call would throw
     * {@code IllegalStateException}. If freeze() moved between passes 4 and 5,
     * this test still passes — but test
     * {@link #setterPostFreeze_throwsIllegalStateException_confirmsFreezeIsActive}
     * confirms freeze actually fired, ruling out a "freeze silently no-oped"
     * false positive.
     */
    @Test
    void pass4EmittedSymbolId_presentAndResolvable_postBuild() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("super-type/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        RDataType child = SymbolIdTestFixtures.findDataType(ws, "com.test.Child");
        RDataType parent = SymbolIdTestFixtures.findDataType(ws, "com.test.Parent");

        // Invariant A: pass 4 emitted the SymbolId (proof that freeze ran AFTER pass 4).
        assertTrue(child.superTypeId().isPresent(),
                "child.superTypeId() must be present — pass 4 emission ran before freeze");

        // Invariant B: the SymbolId carries the workspace's generation token.
        assertEquals(ws.generation(), child.superTypeId().get().generation(),
                "SymbolId generation must match workspace generation");

        // Invariant C: lazy resolve returns the correct node.
        assertSame(parent, child.superType().orElseThrow(),
                "child.superType() must resolve to the parent node via workspace");

        // Invariant D: read-only lazy access works post-freeze (checkMutable is
        // not triggered by the getter path).
        assertSame(parent, child.superType().orElseThrow(),
                "child.superType() must still resolve after first call (idempotent)");

        // Invariant E: nodes are frozen — confirms freeze actually fired (not skipped).
        assertTrue(child.isFrozen(), "child must be frozen post-build");
        assertTrue(parent.isFrozen(), "parent must be frozen post-build");

        Reference.reachabilityFence(ws);
    }

    /**
     * Confirms that nodes without an {@code extends} clause have no superTypeId
     * and no superType, post-build.
     */
    @Test
    void noSuperType_idAndAccessorBothEmpty_postBuild() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("super-type/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        RDataType parent = SymbolIdTestFixtures.findDataType(ws, "com.test.Parent");

        assertTrue(parent.superTypeId().isEmpty(),
                "parent.superTypeId() must be empty — no extends clause");
        assertTrue(parent.superType().isEmpty(),
                "parent.superType() must be empty — no extends clause");

        Reference.reachabilityFence(ws);
    }

    // -----------------------------------------------------------------------
    // Coverage area 3: Post-freeze immutability — multiple setter sites.
    // -----------------------------------------------------------------------

    /**
     * After build+freeze, {@code setSuperTypeId()} on a frozen node must throw
     * {@link IllegalStateException}. This is the primary confirmation that
     * freeze fired and is active — it rules out a "freeze no-oped" false
     * positive for the ordering assertions above.
     */
    @Test
    void setterPostFreeze_throwsIllegalStateException_confirmsFreezeIsActive() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("super-type/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        RDataType child = SymbolIdTestFixtures.findDataType(ws, "com.test.Child");

        // setSuperTypeId is a setter → guarded by checkMutable() → must throw.
        SymbolId existingId = child.superTypeId().orElseThrow();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> child.setSuperTypeId(existingId));
        assertTrue(ex.getMessage().contains("RDataType"),
                "checkMutable error message should name the class: " + ex.getMessage());

        Reference.reachabilityFence(ws);
    }

    /**
     * {@code setName()} on a frozen node must throw {@link IllegalStateException}.
     * Tests a different setter path from {@code setSuperTypeId()} to confirm the
     * freeze guard is not class-specific.
     */
    @Test
    void setNamePostFreeze_throwsIllegalStateException() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("super-type/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        RDataType parent = SymbolIdTestFixtures.findDataType(ws, "com.test.Parent");

        assertThrows(IllegalStateException.class,
                () -> parent.setName("MutatedName"),
                "setName() on a frozen RDataType must throw IllegalStateException");

        Reference.reachabilityFence(ws);
    }

    // -----------------------------------------------------------------------
    // Coverage area 2: Stale generation crossover.
    // -----------------------------------------------------------------------

    /**
     * A SymbolId from workspace W1 must NOT resolve against workspace W2, and
     * vice versa. {@link StaleSymbolIdException} must be thrown in both
     * directions. This guards the generation-tagging invariant (H7 / U005).
     */
    @Test
    void staleGeneration_crossWorkspaceLookup_throwsInBothDirections() throws Exception {
        List<RModel> files1 = SymbolIdTestFixtures.parse("super-type/");
        List<RModel> files2 = SymbolIdTestFixtures.parse("super-type/");

        RWorkspace ws1 = RWorkspace.build(files1).workspace();
        RWorkspace ws2 = RWorkspace.build(files2).workspace();

        // Sanity: the two workspaces have distinct generation tokens.
        assertNotEquals(ws1.generation(), ws2.generation(),
                "each build() call must produce a distinct generation token");

        RDataType child1 = SymbolIdTestFixtures.findDataType(ws1, "com.test.Child");
        RDataType child2 = SymbolIdTestFixtures.findDataType(ws2, "com.test.Child");

        SymbolId id1 = child1.superTypeId().orElseThrow();
        SymbolId id2 = child2.superTypeId().orElseThrow();

        // W1's SymbolId must not resolve in W2.
        StaleSymbolIdException ex1 = assertThrows(StaleSymbolIdException.class,
                () -> ws2.resolve(id1, RDataType.class),
                "W1 SymbolId must not resolve in W2 (generation mismatch)");
        assertEquals(ws2.generation(), ex1.expectedGeneration(),
                "exception must report W2's generation as the expected one");

        // W2's SymbolId must not resolve in W1.
        StaleSymbolIdException ex2 = assertThrows(StaleSymbolIdException.class,
                () -> ws1.resolve(id2, RDataType.class),
                "W2 SymbolId must not resolve in W1 (generation mismatch)");
        assertEquals(ws1.generation(), ex2.expectedGeneration(),
                "exception must report W1's generation as the expected one");

        Reference.reachabilityFence(ws1);
        Reference.reachabilityFence(ws2);
    }

    /**
     * A manually-constructed stale SymbolId (generation = ws.generation() - 1)
     * must trigger {@link StaleSymbolIdException} when resolved against a
     * post-build workspace.
     */
    @Test
    void staleGeneration_manuallyConstructedId_throwsStaleSymbolIdException() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("super-type/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        SymbolId staleId = SymbolId.of("com.test", "Parent", ws.generation() - 1);
        StaleSymbolIdException ex = assertThrows(StaleSymbolIdException.class,
                () -> ws.resolve(staleId, RDataType.class));
        assertEquals(ws.generation(), ex.expectedGeneration());

        Reference.reachabilityFence(ws);
    }

    // -----------------------------------------------------------------------
    // Coverage area 4 (direct): attach-after-freeze is intentionally NOT
    // guarded by checkMutable — verify the documented deliberate gap.
    // -----------------------------------------------------------------------

    /**
     * {@code attachToWorkspace()} is intentionally not guarded by
     * {@code checkMutable()} — documented in {@link com.regnosys.rosetta.ast.RNode}
     * javadoc. After build+freeze, re-attaching a node to a new (or the same)
     * workspace must NOT throw — confirming the deliberate gap exists and
     * setters remain independently guarded.
     *
     * <p>This is the freeze-ordering "deliberate gap" sentinel: if a future
     * refactor accidentally adds a {@code checkMutable()} call inside
     * {@code attachToWorkspace()}, the post-freeze re-attach scenario needed by
     * incremental-compilation (P1.4.4) would break, and this test would catch it.
     */
    @Test
    void attachToWorkspace_afterFreeze_doesNotThrow() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("super-type/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        RDataType child = SymbolIdTestFixtures.findDataType(ws, "com.test.Child");

        // Confirm node is frozen first (so the test is meaningful).
        assertTrue(child.isFrozen(), "child must be frozen before testing attach");

        // Re-attaching a frozen node must not throw.
        assertDoesNotThrow(() -> child.attachToWorkspace(ws),
                "attachToWorkspace() must not throw on a frozen node — intentional gap, see RNode javadoc");

        Reference.reachabilityFence(ws);
    }

    // -----------------------------------------------------------------------
    // Coverage area 1 (complementary): generation token stability.
    // -----------------------------------------------------------------------

    /**
     * The generation token must be stable for the entire lifetime of one
     * workspace — multiple reads must return the same value.
     */
    @Test
    void generationToken_stableForLifetimeOfOneWorkspace() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("super-type/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        long g1 = ws.generation();
        long g2 = ws.generation();
        long g3 = ws.generation();

        assertEquals(g1, g2, "generation must be stable across reads");
        assertEquals(g1, g3, "generation must be stable across reads");

        Reference.reachabilityFence(ws);
    }
}
