package com.regnosys.rosetta.ast.workspace;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.SymbolResolver;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WorkspaceAttachTest {

    /**
     * Subclass-facade leaf: exposes the protected {@code workspace()}
     * accessor + a void {@code requireWorkspace()} for assertThrows usage.
     * Protected access is legal from within a subclass on its own instances.
     */
    private static final class Leaf extends RNode {
        Leaf() { super(); }
        @Override public List<RNode> children() { return List.of(); }
        SymbolResolver workspaceFacade() { return workspace(); }
        void requireWorkspace() { workspace(); }
    }

    @Test
    void unattachedNode_workspace_throwsIllegalStateException() {
        Leaf n = new Leaf();
        IllegalStateException ex = assertThrows(IllegalStateException.class, n::requireWorkspace);
        assertTrue(ex.getMessage().toLowerCase().contains("workspace"));
    }

    @Test
    void attachedNode_workspace_returnsTheAttachedResolver() {
        RWorkspace ws = RWorkspace.build(List.<RModel>of()).workspace();
        Leaf n = new Leaf();
        n.attachToWorkspace(ws);
        assertSame(ws, n.workspaceFacade());
    }

    @Test
    void attachedNode_overwritesPreviousAttachment() {
        RWorkspace ws1 = RWorkspace.build(List.<RModel>of()).workspace();
        RWorkspace ws2 = RWorkspace.build(List.<RModel>of()).workspace();
        Leaf n = new Leaf();
        n.attachToWorkspace(ws1);
        n.attachToWorkspace(ws2);
        assertSame(ws2, n.workspaceFacade());
    }

    @Test
    void attachToWorkspace_nullResolver_throwsNullPointerException() {
        Leaf n = new Leaf();
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> n.attachToWorkspace(null));
        assertTrue(ex.getMessage().contains("resolver"));
    }

    @Test
    void unattachedNode_errorMessageDistinguishesNeverAttached() {
        Leaf n = new Leaf();
        IllegalStateException ex = assertThrows(IllegalStateException.class, n::requireWorkspace);
        assertTrue(ex.getMessage().contains("never attached"),
                "error message should distinguish 'never attached' from 'GC'd': " + ex.getMessage());
    }
}
