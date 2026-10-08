package com.regnosys.rosetta.ast.workspace;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Test;
import java.lang.ref.WeakReference;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class WorkspaceGCResolveTest {

    private static final class Leaf extends RNode {
        Leaf() { super(); }
        @Override public List<RNode> children() { return List.of(); }
        void requireWorkspace() { workspace(); }
    }

    @Test
    void detachedWorkspace_eligibleForGC_subsequentAccessThrows() throws Exception {
        Leaf n = new Leaf();
        WeakReference<RWorkspace> tracker;
        {
            RWorkspace ws = RWorkspace.build(List.<RModel>of()).workspace();
            n.attachToWorkspace(ws);
            tracker = new WeakReference<>(ws);
            // ws goes out of scope here
        }
        // Best-effort GC nudge. Use assumeTrue so test-runner output explicitly
        // shows "skipped" when GC didn't run, rather than a silent green pass
        // that conveys nothing about whether the GC path was actually exercised.
        // The unattached-throws path is independently covered by
        // WorkspaceAttachTest#unattachedNode_workspace_throwsIllegalStateException
        // so coverage doesn't lapse when this test is skipped.
        for (int i = 0; i < 20 && tracker.get() != null; i++) {
            System.gc();
            Thread.sleep(50);
        }
        assumeTrue(tracker.get() == null,
                "GC nudge failed in this JVM; detached-resolver assertion skipped");
        IllegalStateException ex = assertThrows(IllegalStateException.class, n::requireWorkspace);
        assertTrue(ex.getMessage().contains("Leaf"));
        assertTrue(ex.getMessage().contains("garbage-collected"),
                "error message should distinguish 'GC'd' from 'never attached': " + ex.getMessage());
    }
}
