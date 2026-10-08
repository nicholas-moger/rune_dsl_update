package com.regnosys.rosetta.symbols.symbolid;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.StaleSymbolIdException;
import com.regnosys.rosetta.symbols.SymbolId;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RWorkspaceResolveTest {

    @Test
    void generation_isMonotonicallyIncreasingAcrossBuilds() {
        RWorkspace ws1 = RWorkspace.build(List.<RModel>of()).workspace();
        RWorkspace ws2 = RWorkspace.build(List.<RModel>of()).workspace();
        assertTrue(ws2.generation() > ws1.generation(),
                "generation should increase: ws1=" + ws1.generation() + " ws2=" + ws2.generation());
    }

    @Test
    void generation_isStableForOneWorkspace() {
        RWorkspace ws = RWorkspace.build(List.<RModel>of()).workspace();
        long g1 = ws.generation();
        long g2 = ws.generation();
        assertEquals(g1, g2);
    }

    @Test
    void resolve_nullSymbolId_returnsNull() {
        RWorkspace ws = RWorkspace.build(List.<RModel>of()).workspace();
        assertNull(ws.resolve(null, com.regnosys.rosetta.ast.RNode.class));
    }

    @Test
    void resolve_staleGeneration_throwsStaleSymbolIdException() {
        RWorkspace ws = RWorkspace.build(List.<RModel>of()).workspace();
        SymbolId stale = SymbolId.of("com.foo", "Bar", ws.generation() - 1);
        StaleSymbolIdException ex = assertThrows(StaleSymbolIdException.class,
                () -> ws.resolve(stale, com.regnosys.rosetta.ast.RNode.class));
        assertEquals(ws.generation(), ex.expectedGeneration());
    }

    @Test
    void resolve_unknownNamespace_returnsNull() {
        RWorkspace ws = RWorkspace.build(List.<RModel>of()).workspace();
        SymbolId id = SymbolId.of("com.does.not.exist", "Bar", ws.generation());
        assertNull(ws.resolve(id, com.regnosys.rosetta.ast.RNode.class));
    }

    @Test
    void resolve_nullExpectedClass_throwsNullPointerException() {
        // SymbolResolver contract: expected-null check wins over id-null
        // so a fully-bogus call surfaces as NPE rather than silent null-return.
        // Keep this here as a regression guard for the null expected-class contract.
        RWorkspace ws = RWorkspace.build(List.<RModel>of()).workspace();
        SymbolId id = SymbolId.of("com.foo", "Bar", ws.generation());
        assertThrows(NullPointerException.class, () -> ws.resolve(id, null));
    }
}
